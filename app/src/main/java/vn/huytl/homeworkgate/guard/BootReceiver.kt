package vn.huytl.homeworkgate.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier

/**
 * Sau khi may khoi dong lai.
 *
 * Goi [GateStore.tick] o day de phien dang do bi cat dung luc, chu khong doi den
 * khi con mo game roi moi phat hien. Neu khong, khoang thoi gian tu luc bat may
 * den luc co su kien cua so dau tien la mot khe ho choi mien phi.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val gate = GateStore(context)
        gate.tick()
        ParentMode.disable(context)
        Heartbeat.schedule(context)
        MocGio.datLai(context)

        // Khoi dong lai la luc dau tien app chay lai sau che do an toan, nen cung la
        // luc duy nhat co the bao ve khoang thoi gian vua roi. Kiem truoc ca viec
        // tro nang con bat hay khong, vi vao safe mode choi roi ra ma khong dung gi
        // thi tro nang van nguyen, va khong co dong nao duoi day chay.
        val prefs = Prefs.get(context)
        val nhipTruoc = prefs.lastHeartbeatWall
        val bayGio = System.currentTimeMillis()
        if (prefs.isConfigured && nhipTruoc > 0 &&
            bayGio - nhipTruoc > prefs.heartbeatHours * 60L * 60L * 1000L + Notifier.AWAY_SLACK_MS
        ) {
            Notifier.appWasAway(context, nhipTruoc, bayGio)
            prefs.lastHeartbeatWall = bayGio
        }

        // Bat duong safe mode. Trong safe mode app nay khong chay nen khong bao
        // duoc gi, nhung con muon choi thi cuoi cung van phai khoi dong lai vao
        // che do thuong - va day la luc dau tien app chay lai. Thay tro nang da
        // tat tu bao gio thi bao ngay, thay vi doi nhip tim ba tieng nua.
        if (prefs.isConfigured && !Permissions.hasAccessibility(context)) {
            Notifier.guardOffline(context)
        }

        // Bat lai duong day Telegram du may dang khoa. Khoa la trang thai gan nhu ca
        // ngay, va do cung la luc Ba Huy hay go lenh nhat.
        ApprovalService.ensureRunning(context)
    }
}
