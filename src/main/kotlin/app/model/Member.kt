package app.model

import java.time.LocalDate

data class Member(
    val memberId: String,
    var fullName: String,
    var birthDate: LocalDate? = null,           // Ngày tháng năm sinh
    var cccdNumber: String? = null,             // Số CCCD/CMND
    var photoPath: String? = null,              // Đường dẫn ảnh khách hàng
    var startDate: LocalDate,
    var expireDate: LocalDate,
    var packageType: String,
    var balance: Long,
    var pinRetry: Int = 3,
    var locked: Boolean = false
)
