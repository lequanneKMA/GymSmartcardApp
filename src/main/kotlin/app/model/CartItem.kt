package app.model

data class CartItem(
    val name: String,
    val price: Double,
    val quantity: Int = 1
)
