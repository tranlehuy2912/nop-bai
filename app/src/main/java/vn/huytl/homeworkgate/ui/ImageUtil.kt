package vn.huytl.homeworkgate.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Thu nho va xoay anh truoc khi gui.
 *
 * Telegram cho toi 10MB nhung anh goc tu camera tablet thuong 3-6MB, gui qua
 * wifi nha mat hang chuc giay va con phai dung do cho.
 *
 * HAI CO, VI HAI TAM ANH LAM HAI VIEC KHAC NHAU:
 *
 *  - [CANH_THUONG] cho vo dan do va trang de bai in san: chi can doc ra chu gi,
 *    ma chu in thi to va deu, 1600px du thoai mai.
 *  - [CANH_BAI_GIAI] cho bai lam viet tay. O day may phai doc NET BUT cua mot dua
 *    tre viet voi, va phan biet 1 voi l, 0 voi 6, dau tru voi net gach xoa. Mot
 *    trang A4 qua 1600px chi con chung 190 cham moi inch - vua du voi chu nan not,
 *    khong du voi chu au. Tra them vai tram KB de bot mot lan doc nham la re, vi
 *    moi lan doc nham la mot lan goi AI nua, hoac mot lan Ba Huy phai mo anh ra.
 *
 * Doi mot trong hai con so nay thi do lai tren anh that cua Le Hoa, dung sua bang
 * cam tinh: dem so cau AI khai "doc_ro": false truoc va sau khi doi.
 */
object ImageUtil {

    private const val CANH_THUONG = 1600
    private const val CANH_BAI_GIAI = 2200
    private const val JPEG_QUALITY = 82

    /**
     * Thu anh ve co thuong.
     *
     * Giu ten ham cu de moi cho dang goi khong phai sua; anh bai giai thi goi
     * [shrinkBaiGiai].
     */
    fun shrinkInPlace(source: File): File = shrink(source, CANH_THUONG)

    /** Thu anh bai lam viet tay - giu to hon de may doc duoc net but. */
    fun shrinkBaiGiai(source: File): File = shrink(source, CANH_BAI_GIAI)

    private fun shrink(source: File, canhToiDa: Int): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return source

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, canhToiDa)
        }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, options) ?: return source

        // Camera ghi huong anh vao EXIF chu khong xoay pixel. Giai ma xong la mat
        // thong tin do, nen phai tu xoay, neu khong bai tap se nam ngang.
        val rotated = applyExifRotation(decoded, source)
        val scaled = scaleDown(rotated, canhToiDa)

        val out = File(source.parentFile, "send_${source.nameWithoutExtension}.jpg")
        FileOutputStream(out).use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        }
        if (scaled !== decoded) scaled.recycle()
        if (rotated !== decoded && rotated !== scaled) rotated.recycle()
        decoded.recycle()
        return out
    }

    private fun sampleSizeFor(width: Int, height: Int, canhToiDa: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= canhToiDa && h / 2 >= canhToiDa) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyExifRotation(bitmap: Bitmap, file: File): Bitmap {
        val degrees = runCatching {
            when (
                ExifInterface(file.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap, canhToiDa: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= canhToiDa) return bitmap
        val ratio = canhToiDa.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    }
}
