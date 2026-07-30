package ru.school.healthmonitor.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Генерирует QR-код как Bitmap. Возвращает null при ошибке. */
fun generateQr(content: String, sizePx: Int = 512): Bitmap? = try {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val w = matrix.width
    val h = matrix.height
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    for (x in 0 until w) {
        for (y in 0 until h) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    bmp
} catch (_: Throwable) { null }

@Composable
fun rememberQr(content: String, sizePx: Int = 512): Bitmap? =
    remember(content, sizePx) { generateQr(content, sizePx) }
