package vn.huytl.homeworkgate.guard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.telegram.Notifier

/**
 * Bao con song dinh ky.
 *
 * Dung setInexactRepeating chu khong phai bao thuc chinh xac: he thong duoc phep
 * xe dich lan bao nay de gom chung voi cac bao thuc khac dang cho, nen gan nhu
 * khong tao them lan danh thuc CPU nao. Tin bao khong can dung gio, chi can den.
 */
object Heartbeat {

    fun schedule(context: Context) {
        val prefs = Prefs.get(context)
        if (!prefs.isConfigured) return

        val intervalMs = prefs.heartbeatHours * 60L * 60L * 1000L
        val am = context.getSystemService(AlarmManager::class.java)
        am.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + intervalMs,
            intervalMs,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, Receiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private const val REQUEST_CODE = 7301
    const val ACTION = "vn.huytl.homeworkgate.HEARTBEAT"

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Notifier.heartbeat(context)
        }
    }
}
