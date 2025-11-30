package app.model

data class Transaction(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val memberId: String
)

enum class TransactionType {
    TOP_UP,         // Nạp tiền
    EXTEND_PACKAGE, // Gia hạn gói
    PURCHASE,       // Thanh toán đồ dùng
    CHANGE_PIN      // Đổi PIN
}
