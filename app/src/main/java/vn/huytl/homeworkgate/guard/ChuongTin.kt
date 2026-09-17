package vn.huytl.homeworkgate.guard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.ui.ChatActivity

/**
 * Keu len khi Ba Huy nhan mot cau cho con.
 *
 * Tach ra khoi ApprovalService vi bay gio co hai duong cung dan toi day: tin go
 * trong Telegram, va tin go trong app Bang dieu khien. De nguyen trong service thi
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

    private const val KENH = "chat_tu_ba"
    private const val SO = 1002
}
