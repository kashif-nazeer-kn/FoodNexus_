package com.example.foodnexus.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.R
import com.example.foodnexus.databinding.FragmentWaiterCartBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.*

class WaiterCartFragment : Fragment() {
    private var _binding: FragmentWaiterCartBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var prefs: SharedPreferences
    private lateinit var ownerId: String
    private lateinit var userId: String

    private val cartItems = mutableListOf<WaiterCartStructure>()
    private lateinit var adapter: WaiterCartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentWaiterCartBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefs = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
        userId  = prefs.getString("userId", "") ?: ""
        ownerId = prefs.getString("ownerId", "") ?: ""

        setupRecycler()
        setupMenu()
        listenToCartRealtime()
    }

    private fun setupRecycler() {
        adapter = WaiterCartAdapter(
            items = cartItems,
            onQuantityChanged = { updateProceedButton() },
            onIncrease = { changeQuantity(it, +1) },
            onDecrease = { changeQuantity(it, -1) }
        )
        binding.WaiterCartRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WaiterCartFragment.adapter
        }
        binding.WaiterCartPlaceOrderButton.setOnClickListener { placeOrder() }
    }

    private fun setupMenu() {
        binding.WaiterCartMenu.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                menuInflater.inflate(R.menu.waiter_cart_menu, menu)
                setOnMenuItemClickListener {
                    if (it.itemId == R.id.clearCart) {
                        clearCart()
                        true
                    } else false
                }
                show()
            }
        }
    }

    private fun listenToCartRealtime() {
        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("Staff")
            .document(userId)
            .collection("Carts")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                cartItems.clear()
                for (doc in snap.documents) {
                    val item = doc.toObject(WaiterCartStructure::class.java)
                    item?.let { cartItems.add(it) }
                }
                adapter.notifyDataSetChanged()
                updateProceedButton()
            }
    }

    private fun changeQuantity(item: WaiterCartStructure, delta: Int) {
        val newQty = (item.quantity + delta).coerceAtLeast(1)
        item.quantity = newQty
        val data = mapOf(
            "quantity" to newQty
        )
        lifecycleScope.launch {
            try {
                firestore.collection("Restaurants")
                    .document(ownerId)
                    .collection("Staff")
                    .document(userId)
                    .collection("Carts")
                    .document(item.itemId)
                    .update(data)
                    .await()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not update item", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProceedButton() {
        val total = cartItems.sumOf { it.totalPrice }
        binding.WaiterCartPlaceOrderButton.text =
            String.format(Locale.getDefault(), "Proceed with: %.2f", total)
    }

    private fun placeOrder() {
        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val total = cartItems.sumOf { it.totalPrice }
        val order = mapOf(
            "waiterId"   to userId,
            "items"      to cartItems.map { mapOf(
                "itemId"   to it.itemId,
                "itemName" to it.itemName,
                "quantity" to it.quantity,
                "unitPrice" to it.unitPrice,
                "customizeRecipe" to it.customizeRecipe
            )},
            "totalPrice" to String.format(Locale.getDefault(), "%.2f", total),
            "status"     to "pending",
            "timestamp"  to Date()
        )

        lifecycleScope.launch {
            try {
                val ref = firestore.collection("Restaurants")
                    .document(ownerId)
                    .collection("PendingOrders")   // <-- fixed typo
                    .add(order)
                    .await()
                Toast.makeText(requireContext(),
                    "Order sent to chef, please wait…", Toast.LENGTH_SHORT).show()
                listenOrderStatus(ref.id)
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Failed to place order", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listenOrderStatus(orderId: String) {
        firestore.collection("Restaurants")
            .document(ownerId)
            .collection("PendingOrders")
            .document(orderId)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                when (snap.getString("status")) {
                    "accepted" -> {
                        // update to preparing
                        snap.reference.update("status", "preparing")
                        clearCart()
                        Toast.makeText(requireContext(),
                            "Order accepted — preparing now", Toast.LENGTH_SHORT).show()
                    }
                    "declined" -> {
                        Toast.makeText(requireContext(),
                            "Order declined — you can modify and resend", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun clearCart() {
        lifecycleScope.launch {
            try {
                val col = firestore.collection("Restaurants")
                    .document(ownerId)
                    .collection("Staff")
                    .document(userId)
                    .collection("Carts")
                    .get()
                    .await()
                for (doc in col.documents) {
                    doc.reference.delete()
                }
                Toast.makeText(requireContext(), "Cart cleared", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Failed to clear cart", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
