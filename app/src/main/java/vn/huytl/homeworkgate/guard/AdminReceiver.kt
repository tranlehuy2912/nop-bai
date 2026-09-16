package vn.huytl.homeworkgate.guard

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import vn.huytl.homeworkgate.telegram.Notifier

/**
 * Dang ky lam quan tri thiet bi. Khong phai Device Owner, chi can bo bam dong y
 * mot lan luc cai, khong phai go tai khoan Google hay reset may.
 *
 * Duoc hai viec: khi dang bat thi Android tu choi go app, va khi ai do dinh tat
 * no thi [onDisableRequested] chay truoc, du de bao mot tin len Telegram.
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Notifier.adminDisableRequested(context)
        return "Tắt cái này là Ba Huy biết ngay. Tablet sẽ nhắn tin báo."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Notifier.adminDisabled(context)
    }
}
