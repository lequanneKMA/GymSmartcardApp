package app.model

import java.time.LocalDate

data class Member(
    val memberId: String,
    var fullName: String,
    var birthDate: LocalDate? = null,           // Ngày tháng năm sinh
    var cccdNumber: String? = null,             // Số CCCD/CMND
    var photoPath: String? = null,              // Đường dẫn ảnh (legacy - để tương thích UI)
    var photoData: ByteArray? = null,           // Dữ liệu ảnh (lưu trên thẻ - encrypted)
    var startDate: LocalDate,
    var expireDate: LocalDate,
    var packageType: String,
    var balance: Long,
    var pinRetry: Int = 3,
    var locked: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Member

        if (memberId != other.memberId) return false
        if (photoData != null) {
            if (other.photoData == null) return false
            if (!photoData.contentEquals(other.photoData)) return false
        } else if (other.photoData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = memberId.hashCode()
        result = 31 * result + (photoData?.contentHashCode() ?: 0)
        return result
    }
}
