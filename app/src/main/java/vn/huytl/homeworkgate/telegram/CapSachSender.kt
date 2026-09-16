package vn.huytl.homeworkgate.telegram

import android.content.Context
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.ui.ImageUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gui anh cap sach da soan cho Ba Huy.
 *
 * Tach khoi [HomeworkSender] chu khong them mot nhanh vao trong do: hai viec nay
 * giong nhau o moi cho tru cho quan trong nhat. Nop bai thi sinh ra mot yeu cau
 * cho duyet, co nut bam, co AI cham, co cong gio. Chup cap thi khong: no chi la
 * mot tam anh de Ba Huy liec qua xem co thieu quyen vo nao khong. Cho chung mot
 * ham thi som muon cung co ngay mot tam anh cap sach di lac vao duong duyet gio
 * choi, va Le Hoa duoc mot phieu gio khong ai duyet.
 */
object CapSachSender {

    /**
     * [shrink] de test tat di duoc: anh test ve san co san kich thuoc vua roi.
     * Ham nay chay dong bo, nguoi goi tu dua sang luong nen.
     */
    fun send(
        context: Context,
        anh: List<File>,
        moTaBuoi: String,
        monDaTich: List<String>,
        shrink: Boolean = true
    ) {
        require(anh.isNotEmpty()) { "phai co it nhat mot tam anh cap sach" }

        val prefs = Prefs.get(context)
        val client = TelegramClient(prefs.botToken)
        val gio = SimpleDateFormat("HH:mm 'ngày' dd/MM", Locale("vi", "VN")).format(Date())

        val chuThich = buildString {
            append("Lê Hòa báo đã soạn xong cho ").append(moTaBuoi).append('.')
            append("\nLúc ").append(gio).append('.')
            if (monDaTich.isEmpty()) {
                append("\nBuổi này không phải mang vở.")
            } else {
                append("\nĐã tích ").append(monDaTich.size).append(" môn: ")
                append(monDaTich.joinToString(", "))
            }
            append("\n\nThiếu gì thì gõ /soanlai để bắt soạn lại.")
        }

        val guiDi = if (shrink) anh.map { ImageUtil.shrinkInPlace(it) } else anh
        if (guiDi.size == 1) {
            client.sendPhoto(prefs.parentChatId, guiDi[0], chuThich, null)
        } else {
            client.sendPhotoAlbum(prefs.parentChatId, guiDi, chuThich)
        }
        // Chi xoa ban da thu nho. shrinkInPlace tra ve chinh file goc khi khong giai
        // ma duoc anh, xoa luc do la mat anh that.
        if (shrink) guiDi.forEachIndexed { i, f -> if (f != anh[i]) f.delete() }
    }
}
