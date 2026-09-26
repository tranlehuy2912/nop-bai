package vn.huytl.homeworkgate.data

import android.app.NotificationManager
import android.content.Context
import java.io.File

/**
 * Xoa phan con lai cua khung chat trong app, bo ngay 27/9/2026.
 *
 * Le Hoa nhan tin voi ba bang Telegram that, khung chat cu da go khoi app. Ba Huy bao
 * xoa luon tin cu, ca trong may lan tren Firestore: de lai thi la mot dong chuyen rieng
 * cua tre con nam do mai ma khong con man nao doc duoc. Phan tren Firestore o
 * [vn.huytl.homeworkgate.dongbo.DongBo.xoaChatCu].
 *
 * Trong may co ba thu: cac cau chat trong prefs (da ma hoa), may tam anh hai cha con
 * gui nhau trong [THU_MUC_ANH], va kenh thong bao "Tin nhan cua Ba Huy". Goi luc app
 * khoi dong, bao nhieu lan cung duoc: khong con gi thi khong lam gi.
 */
object ChatCu {

    /** Cac khoa prefs cua ChatBox cu. */
    private val KHOA = listOf("chat_lines", "chat_read_at", "chat_last_sent", "chat_wait_until")

    /** Thu muc anh cua khung chat cu, trong filesDir. */
    const val THU_MUC_ANH = "chat_anh"

    /** Kenh thong bao ChuongTin tung dung cho tin cua ba. */
    private const val KENH = "chat_tu_ba"

    fun xoaTrongMay(context: Context) {
        val sp = Prefs.get(context).raw()
        val con = KHOA.filter { sp.contains(it) }
        if (con.isNotEmpty()) {
            sp.edit().apply { con.forEach { remove(it) } }.commit()
        }
        runCatching { File(context.filesDir, THU_MUC_ANH).deleteRecursively() }
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                ?.deleteNotificationChannel(KENH)
        }
    }
}
