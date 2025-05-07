package com.example.foodnexus.Fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnexus.R
import java.util.*

class WaiterCartAdapter(
    private val items: List<WaiterCartStructure>,
    private val onQuantityChanged: () -> Unit,
    private val onIncrease: (WaiterCartStructure) -> Unit,
    private val onDecrease: (WaiterCartStructure) -> Unit
) : RecyclerView.Adapter<WaiterCartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTv: TextView = itemView.findViewById(R.id.cartItemName)
        private val priceTv: TextView = itemView.findViewById(R.id.cartItemPrice)
        private val qtyTv: TextView = itemView.findViewById(R.id.cartItemQuantity)
        private val incBtn: ImageButton = itemView.findViewById(R.id.btnIncrease)
        private val decBtn: ImageButton = itemView.findViewById(R.id.btnDecrease)
        private val recipeTv: TextView = itemView.findViewById(R.id.CustomizeRecipe)

        fun bind(item: WaiterCartStructure) {
            nameTv.text = item.itemName
            priceTv.text = String.format(Locale.getDefault(), "%.2f", item.totalPrice)
            qtyTv.text = item.quantity.toString()
            recipeTv.text = item.customizeRecipe

            incBtn.setOnClickListener {
                onIncrease(item)
                onQuantityChanged()
            }
            decBtn.setOnClickListener {
                if (item.quantity > 1) {
                    onDecrease(item)
                    onQuantityChanged()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.waiter_cart_recycler_view, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
