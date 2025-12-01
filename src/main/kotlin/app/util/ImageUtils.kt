package app.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Extension function to convert BufferedImage to Compose ImageBitmap
 */
fun BufferedImage.toImageBitmap(): ImageBitmap {
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(this, "png", outputStream)
    val bytes = outputStream.toByteArray()
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
