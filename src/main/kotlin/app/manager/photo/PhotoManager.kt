package app.manager.photo

import java.awt.image.BufferedImage
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO

/**
 * Photo Manager
 * Quản lý lưu trữ và load ảnh thành viên
 */
object PhotoManager {
    
    private const val PHOTOS_DIR = "photos"
    
    init {
        // Tạo thư mục photos nếu chưa tồn tại
        val photosPath = Paths.get(PHOTOS_DIR)
        if (!Files.exists(photosPath)) {
            Files.createDirectories(photosPath)
        }
    }
    
    /**
     * Lưu ảnh từ file picker
     * @param sourceFile File ảnh gốc
     * @param memberId ID thành viên
     * @return Đường dẫn tương đối của ảnh đã lưu, hoặc null nếu lỗi
     */
    fun savePhoto(sourceFile: File, memberId: String): String? {
        return try {
            val extension = sourceFile.extension.lowercase()
            if (extension !in listOf("jpg", "jpeg", "png", "bmp")) {
                return null
            }
            
            val fileName = "${memberId}_${System.currentTimeMillis()}.$extension"
            val destPath = Paths.get(PHOTOS_DIR, fileName)
            
            Files.copy(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING)
            
            "$PHOTOS_DIR/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Lưu ảnh từ BufferedImage (webcam hoặc screenshot)
     * @param image BufferedImage
     * @param memberId ID thành viên
     * @return Đường dẫn tương đối của ảnh đã lưu, hoặc null nếu lỗi
     */
    fun savePhoto(image: BufferedImage, memberId: String): String? {
        return try {
            val fileName = "${memberId}_${System.currentTimeMillis()}.png"
            val destPath = Paths.get(PHOTOS_DIR, fileName).toFile()
            
            ImageIO.write(image, "png", destPath)
            
            "$PHOTOS_DIR/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Load ảnh từ đường dẫn
     * @param photoPath Đường dẫn tương đối hoặc tuyệt đối
     * @return BufferedImage hoặc null nếu không tìm thấy
     */
    fun loadPhoto(photoPath: String?): BufferedImage? {
        if (photoPath == null) return null
        
        return try {
            val file = File(photoPath)
            if (file.exists()) {
                ImageIO.read(file)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Xóa ảnh
     * @param photoPath Đường dẫn ảnh
     * @return true nếu xóa thành công
     */
    fun deletePhoto(photoPath: String?): Boolean {
        if (photoPath == null) return false
        
        return try {
            val file = File(photoPath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Kiểm tra ảnh có tồn tại không
     */
    fun photoExists(photoPath: String?): Boolean {
        if (photoPath == null) return false
        return File(photoPath).exists()
    }
    
    /**
     * Convert BufferedImage to ByteArray (để lưu encrypted trên thẻ)
     * @param image BufferedImage
     * @param format Format (png, jpg)
     * @return ByteArray hoặc null nếu lỗi
     */
    fun imageToByteArray(image: BufferedImage, format: String = "png"): ByteArray? {
        return try {
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, format, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Convert File to ByteArray (để lưu encrypted trên thẻ)
     * @param file File ảnh
     * @return ByteArray hoặc null nếu lỗi
     */
    fun fileToByteArray(file: File): ByteArray? {
        return try {
            val image = ImageIO.read(file) ?: return null
            imageToByteArray(image)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Convert ByteArray to BufferedImage (để hiển thị ảnh từ thẻ)
     * @param bytes ByteArray
     * @return BufferedImage hoặc null nếu lỗi
     */
    fun byteArrayToImage(bytes: ByteArray?): BufferedImage? {
        if (bytes == null) return null
        
        return try {
            val bais = ByteArrayInputStream(bytes)
            ImageIO.read(bais)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
