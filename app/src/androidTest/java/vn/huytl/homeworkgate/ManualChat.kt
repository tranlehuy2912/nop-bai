package vn.huytl.homeworkgate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.ChatFrom
import java.io.File

/**
 * Khong phai test that. Nap san mot cuoc noi chuyen vao may ao de NHIN man chat.
 *
 * Khung chat rong thi khong danh gia duoc gi ve no, ma go tay qua adb thi vua lac
 * phim vua gui that sang Telegram cua Ba Huy. Cho nay ghi thang vao [ChatBox], nen
 * khong tin nao ra khoi may.
 *
 * CAN THAN, y het [ManualSeed]: lop nay chay trong tien trinh cua bai test chu
 * khong phai cua app. Nap xong phai "adb shell am force-stop vn.huytl.homeworkgate"
 * roi mo lai, khong thi app van doc ban cu trong bo nho.
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualChat \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ManualChat {

    @Test
    fun napCuocNoiChuyenDeNhin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ChatBox.xoaHet(context)

        val phut = 60_000L
        val gio = 60 * phut
        val bayGio = System.currentTimeMillis()

        // Mot doan hom qua va mot doan hom nay: du de nhin ra hai ngay lien nhau co
        // phan biet duoc khong.
        ChatBox.add(context, ChatFrom.CON, "Ba ơi con để quên vở Ngữ văn ở lớp rồi", bayGio - 26 * gio)
        ChatBox.add(context, ChatFrom.BA, "Không sao, mai con lấy lại. Tối nay làm môn khác trước nhé", bayGio - 25 * gio)
        ChatBox.add(context, ChatFrom.CON, "Dạ 🙂", bayGio - 25 * gio + 2 * phut)

        ChatBox.add(context, ChatFrom.CON, "Ba ơi bài này con không hiểu", bayGio - 3 * gio, anhThu(context, "Bài 4 trang 27"))
        ChatBox.add(context, ChatFrom.CON, "", bayGio - 3 * gio + phut, anhThu(context, "phần b"))
        ChatBox.add(
            context, ChatFrom.BA,
            "Con đọc lại hằng đẳng thức số 3 rồi thử phân tích vế trái xem. Tí nữa ba về ba chỉ thêm.",
            bayGio - 2 * gio
        )
        ChatBox.add(context, ChatFrom.CON, "Dạ con làm được rồi ạ 🎉", bayGio - 40 * phut)
    }

    /** Mot tam anh gia, du de nhin ra bong bong co anh trong nhu the nao. */
    private fun anhThu(context: android.content.Context, nhan: String): String {
        val bm = Bitmap.createBitmap(900, 640, Bitmap.Config.ARGB_8888)
        Canvas(bm).apply {
            drawColor(Color.parseColor("#FFFDF6E8"))
            val but = Paint().apply {
                color = Color.parseColor("#FF3A3F4A")
                textSize = 56f
                isAntiAlias = true
            }
            drawText(nhan, 60f, 180f, but)
            but.color = Color.parseColor("#FFB8C0CC")
            for (y in 260..600 step 70) drawRect(60f, y.toFloat(), 840f, y + 3f, but)
        }
        val f = File(ChatBox.thuMucAnh(context), "thu_${System.nanoTime()}.jpg")
        f.outputStream().use { bm.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return f.absolutePath
    }
}
