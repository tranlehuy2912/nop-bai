package vn.huytl.homeworkgate.guard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.telegram.Notifier
import java.util.Calendar

/**
 * Danh thuc dung hai moc trong ngay: gio di ngu, va nua dem.
 *
 * VI SAO CAN: hai thu het han theo dong ho chu khong theo viec ai bam gi. Phieu
 * duyet chua dung chi song trong ngay va khong qua gio ngu; bai dang cho duyet
 * cung vay. Ma o hai trang thai do thi khong co vong nao dang chay ca - vong dem
 * nguoc ben [GuardAccessibilityService] chi song trong phien choi that.
 *
 * Truoc day cho nay duoc lam bang cach man hinh chinh goi tick() moi muoi giay
 * trong luc no dang mo. Vua khong chac (dong man hinh lai la khong ai canh nua),
 * vua ton: moi lan tick la mot lan ghi xuong dia, keo theo mot luot day len
 * Firestore, ca ngay hang tram lan de phat hien hai moc.
 *
 * Bao thuc dung [AlarmManager.setAndAllowWhileIdle] chu khong phai bao thuc chinh
 * xac: tre vai phut khong hong gi. Con bam Bat dau luc 22:05 thi [GateStore.start]
 * van tu choi vi no hoi lai gio ngu ngay tai do; bao thuc nay chi de trang thai va
 * tin bao ve dung luc, khong phai de chan.
 */
object MocGio {

    fun datLai(context: Context) {
        val prefs = Prefs.get(context)
        if (!prefs.isConfigured) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val moc = mocKeTiep(prefs.hardStopMinuteOfDay)
        runCatching {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, moc, pendingIntent(context))
        }.onFailure { Log.w(TAG, "khong dat duoc moc gio: ${it.message}") }
    }

    fun huy(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    /**
     * Moc gan nhat trong hai moc: gio di ngu, va nua dem.
     *
     * Nua dem la moc doi ngay - phieu duyet cua hom qua het han o day. Gio di ngu
     * thuong den truoc, nhung khong phai luon: Ba Huy doi gio ngu sang 23:30 thi
     * hai moc doi cho nhau.
     */
    private fun mocKeTiep(phutNgu: Int, bayGio: Long = System.currentTimeMillis()): Long {
        val ngu = mocTrongNgay(bayGio, phutNgu / 60, phutNgu % 60)
        val nuaDem = mocTrongNgay(bayGio, 0, 0)
        return minOf(ngu, nuaDem)
    }

    /** Lan ke tiep dong ho cham gio phut nay. Qua roi thi la ngay mai. */
    private fun mocTrongNgay(bayGio: Long, gio: Int, phut: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = bayGio
            set(Calendar.HOUR_OF_DAY, gio)
            set(Calendar.MINUTE, phut)
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= bayGio) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, Receiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private const val TAG = "HomeworkGate"
    private const val REQUEST_CODE = 7302
    const val ACTION = "vn.huytl.homeworkgate.MOC_GIO"

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val gate = GateStore(context)
            val ly = gate.tick()
            Log.i(TAG, "toi moc gio: state=${gate.state} ly=$ly")
            if (ly != null) runCatching { Notifier.sessionEnded(context, ly) }
            runCatching { DongBo.dayNgay() }
            // Dat moc ke tiep ngay tai day: bao thuc nay khong lap lai.
            datLai(context)
        }
    }
}
