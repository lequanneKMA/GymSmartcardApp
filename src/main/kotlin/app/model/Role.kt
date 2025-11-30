package app.model

enum class Role {
    ADMIN,      // Quản lý tất cả: tạo/sửa/xóa thành viên, tạo PIN, xem báo cáo
    STAFF,      // Nhân viên: quét thẻ, checkin/checkout, gia hạn gói, xem lịch sử
    CUSTOMER    // Khách hàng: nạp tiền, mua đồ, xem thông tin cá nhân
}
