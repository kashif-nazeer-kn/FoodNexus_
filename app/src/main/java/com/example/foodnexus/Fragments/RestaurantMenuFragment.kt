package com.example.foodnexus.Fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnexus.Adapters.OwnerMenuAdapter
import com.example.foodnexus.Structures.OwnerMenuStructure
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentRestaurantMenuBinding
import com.example.foodnexus.databinding.AddResturantMenuDialogBinding
import com.google.firebase.firestore.FirebaseFirestore

class RestaurantMenuFragment : Fragment() {
    private var _binding: FragmentRestaurantMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var arrayList: ArrayList<OwnerMenuStructure>
    private lateinit var adapter: OwnerMenuAdapter
    private lateinit var loadingDialog: Dialog
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurantMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        loadMenu()

        binding.RestaurantMenuImgBtnAdd.setOnClickListener {
            openAddItemDialog()
        }
    }

    private fun init() {
        firestore = FirebaseFirestore.getInstance()
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)
//        userId = preferences.getString("userId", "") ?: ""
            userId="1234"
        arrayList = ArrayList()
        adapter = OwnerMenuAdapter(arrayList, this@RestaurantMenuFragment, userId)

        binding.RestaurantMenuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.RestaurantMenuRecyclerView.adapter = adapter

        // Setup loading dialog
        loadingDialog = Dialog(requireContext()).apply {
            setContentView(com.example.foodnexus.R.layout.progress_bar)
            setCancelable(false)
        }
    }

    private fun openAddItemDialog() {
        val dialogBinding = AddResturantMenuDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
        }

        dialogBinding.DialogBtnAdd.setOnClickListener {
            val name = dialogBinding.DialogEtItemName.text.toString().trim()
            val recipe = dialogBinding.DialogEtItemRecipe.text.toString().trim()

            if (name.isEmpty() || recipe.isEmpty()) {
                Utils.showToast(requireContext(), "Please fill out all fields.")
            } else {
                dialog.dismiss()
                addItem(name, recipe)
            }
        }

        dialog.show()
    }

    private fun addItem(name: String, recipe: String) {
        try {
            Utils.showProgress(loadingDialog)

            val itemRef = firestore.collection("Restaurants")
                .document(userId)
                .collection("Menu")
                .document()

            val itemData = hashMapOf(
                "Item Name" to name,
                "Item Recipe" to recipe,
                "Time Stamp" to System.currentTimeMillis()
            )

            itemRef.set(itemData)
                .addOnSuccessListener {
                    arrayList.add(OwnerMenuStructure(itemRef.id, name, recipe))
                    adapter.notifyItemInserted(arrayList.lastIndex)
                    Utils.showToast(requireContext(), "Item added successfully.")
                    updateEmptyState()
                }
                .addOnFailureListener { e ->
                    Utils.showToast(requireContext(), "Failed to add item: ${e.localizedMessage}")
                }
                .addOnCompleteListener {
                    Utils.hideProgress(loadingDialog)
                }

        } catch (e: Exception) {
            Utils.showToast(requireContext(), "Error: ${e.localizedMessage}")
            Utils.hideProgress(loadingDialog)
        }
    }

    private fun loadMenu() {
        Utils.showProgress(loadingDialog)

        firestore.collection("Restaurants")
            .document(userId)
            .collection("Menu")
            .get()
            .addOnSuccessListener { documents ->
                arrayList.clear()
                for (doc in documents) {
                    val itemId = doc.id
                    val itemName = doc.getString("Item Name") ?: "Unknown"
                    val itemRecipe = doc.getString("Item Recipe") ?: "Unknown"
                    arrayList.add(OwnerMenuStructure(itemId, itemName, itemRecipe))
                }
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener { e ->
                Utils.showToast(requireContext(), "Failed to load menu: ${e.localizedMessage}")
            }
            .addOnCompleteListener {
                Utils.hideProgress(loadingDialog)
            }
    }

    private fun updateEmptyState() {
        binding.RestaurantMenuTvAddClasses.text =
            if (arrayList.isEmpty()) "No items available" else ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
