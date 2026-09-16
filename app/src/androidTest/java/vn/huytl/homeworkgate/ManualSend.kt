package vn.huytl.homeworkgate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CaptureStage
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.telegram.HomeworkSender
import java.io.File

/**
 * Khong phai test that. Day la cach gui thu mot lan nop bai len Telegram ma khong
 * can camera: may ao arm64 mo duoc camera nhung khong tra ve khung hinh nao, nen
 * bam nut chup tren may ao se treo mai.
 *
 * Trang giay o day ve bang tay, co ghi ro no thuoc nhom nao, de nhin trong Telegram
 * la biet ngay ba nhom co vao dung cho khong.
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualSend \
 *     -e dando 1 -e debai 2 -e baigiai 2 \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *
 * De mot nhom bang 0 la thu truong hop con bo qua nhom do.
 */
@RunWith(AndroidJUnit4::class)
class ManualSend {

    @Test
    fun guiThuMotLanNopBai() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val args = InstrumentationRegistry.getArguments()

        fun soTrang(key: String, macDinh: Int) =
            args.getString(key)?.toIntOrNull() ?: macDinh

        val ke = mapOf(
            CaptureStage.DAN_DO to soTrang("dando", 1),
            CaptureStage.DE_BAI to soTrang("debai", 2),
            CaptureStage.BAI_GIAI to soTrang("baigiai", 2)
        )

        val groups = ke.mapValues { (stage, count) ->
            (1..count).map { veTrang(context, context.getString(stage.labelRes), it, count) }
        }.filterValues { it.isNotEmpty() }

        val sent = HomeworkSender.send(context, groups, shrink = false)

        // Ghi lai trang thai cho giong het luc con bam gui that, de bam nut Duyet
        // trong Telegram la mo cong duoc chu khong bao "yeu cau nay cu roi".
        GateStore(context).markPending(sent.requestId, sent.messageId)

        println("MANUAL_SEND: da gui ${groups.values.sumOf { it.size }} trang, " +
            "requestId=${sent.requestId} messageId=${sent.messageId}")
    }

    /** Ve mot trang giay tra, co ke dong, de nhin trong Telegram ra hinh trang vo. */
    private fun veTrang(context: Context, nhan: String, so: Int, tong: Int): File {
        val bitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val ke = Paint().apply {
            color = Color.parseColor("#D8E2F0")
            strokeWidth = 2f
        }
        var y = 320f
        while (y < 1520f) {
            canvas.drawLine(80f, y, 1120f, y, ke)
            y += 64f
        }

        val tieuDe = Paint().apply {
            color = Color.parseColor("#1A1C20")
            textSize = 76f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(nhan, 80f, 170f, tieuDe)

        val phu = Paint().apply {
            color = Color.parseColor("#5F6472")
            textSize = 48f
            isAntiAlias = true
        }
        canvas.drawText("Trang $so / $tong — ảnh dựng để thử", 80f, 250f, phu)

        val file = File(context.cacheDir, "thu_${nhan.hashCode()}_$so.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        return file
    }
}
