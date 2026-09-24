package vn.huytl.homeworkgate.guard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.ui.ChatActivity
import vn.huytl.homeworkgate.ui.TinActivity

/**
 * Keu len khi Ba Huy nhan mot cau cho con, hay chuyen mot tin cua co giao sang.
 *
 * Tach ra khoi ApprovalService vi ca hai loai tin deu co hai duong cung dan toi day:
 * go trong Telegram, va go trong app Bang dieu khien. De nguyen trong service thi
 * duong thu hai phai chep lai ca doan tao kenh - ma chep ten kenh sai mot chu la
 * tin nhan im lang, khong ai biet vi sao.
 *
 * Tieng to (IMPORTANCE_HIGH) la co chu y: con co the dang o app khac hoac da de may
 * xuong ban, ma tin cua ba thuong la tra loi cho mot viec dang gap.
 */
object ChuongTin {

    fun keu(context: Context, chu: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(KENH) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    KENH,
                    "Tin nhắn của ${context.getString(R.string.parent_name)}",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val mo = PendingIntent.getActivity(
            context,
            1,
            Intent(context, ChatActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            SO,
            NotificationCompat.Builder(context, KENH)
                .setSmallIcon(R.drawable.ic_stat_gate)
                .setContentTitle("${context.getString(R.string.parent_name_cap)} nhắn")
                .setContentText(chu)
                .setStyle(NotificationCompat.BigTextStyle().bigText(chu))
                .setContentIntent(mo)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()
        )
    }

    /**
     * Bao co tin cua co giao bang thong bao Android thuong.
     *
     * Khong dung the noi nhu loi nhac soan vo: tin cua co can doc ky va can xem
     * lai, ma the noi thi troi qua roi mat. Thong bao thi nam trong khay cho toi
     * khi co nguoi bam vao.
     *
     * Ma kenh giu nguyen nhu hoi doan nay con nam trong ApprovalService: doi ma la
     * may tao mot kenh moi, va cai dat am thanh cua kenh cu mat theo.
     */
    fun baoTinCuaCo(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(KENH_TIN_CO) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    KENH_TIN_CO,
                    "Tin của cô giáo",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Báo khi ba Huy chuyển tin của cô từ nhóm lớp sang"
                }
            )
        }
        val chuaDoc = KhoTinCuaCo(context).soTinChuaDoc()
        val mo = PendingIntent.getActivity(
            context,
            2,
            Intent(context, TinActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            SO_TIN_CO,
            NotificationCompat.Builder(context, KENH_TIN_CO)
                .setSmallIcon(R.drawable.st_ic_cap_sach)
                .setContentTitle("Cô giáo nhắn tin")
                .setContentText(if (chuaDoc > 1) "$chuaDoc tin chưa đọc" else "Bấm để đọc")
                .setContentIntent(mo)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()
        )
    }

    private const val KENH = "chat_tu_ba"
    private const val SO = 1002
    private const val KENH_TIN_CO = "tin_cua_co"
    private const val SO_TIN_CO = 1003
}
