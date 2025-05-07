package com.example.foodnexus.Fragments

data class WaiterCartStructure(
    val itemId: String = "",
    val itemName: String = "",
    val unitPrice: Double = 0.0,
    var quantity: Int = 1,
    val customizeRecipe: String = ""
) {
    val totalPrice: Double
        get() = unitPrice * quantity
}
