package vn.huytl.homeworkgate.guard

import android.content.Context
import android.os.SystemClock
import vn.huytl.homeworkgate.data.Prefs

/**
 * Che do Ba Huy: tam ngung moi viec chan de Ba Huy dung may.
 *
 * Mac dinh khong co han. Truoc day cho nay tu tat sau 30 phut, voi ly do "quen tat
 * mot lan la cong mo toang ca tuan". Ly do do that, nhung cai gia phai tra nang hon:
 * dang lam viec tren may ma cu nua tieng lai phai nhap PIN mo lai thi phien den muc
 * khong ai dung. Anh Huy chon tu nho tat, va day la may cua anh.
 *
 * Van con ba duong tu dong dong lai, deu khong dua vao tri nho:
 *  - khoi dong lai may;
 *  - bam o "Le Hoa" o man chon nguoi dung, tuc la dua may lai cho con;
 *  - go /khoa trong Telegram.
 *
 * Van dat han duoc khi muon, bang "/bo 30".
 */
object ParentMode {

    private const val K_ON = "parent_on"
    private const val K_AT_ELAPSED = "parent_at_elapsed"
    private const val K_UNTIL_ELAPSED = "parent_until_elapsed"
    private const val K_UNTIL_WALL = "parent_until_wall"

    /**
     * Bat che do Ba Huy. [minutes] bang null la khong dat han.
     */
    fun enable(context: Context, minutes: Int? = null) {
        val sp = Prefs.get(context).raw()
        val batDauElapsed = SystemClock.elapsedRealtime()
        val ms = minutes?.coerceIn(1, 12 * 60)?.times(60_000L)

        sp.edit()
            .putBoolean(K_ON, true)
            .putLong(K_AT_ELAPSED, batDauElapsed)
            .putLong(K_UNTIL_ELAPSED, if (ms == null) 0L else batDauElapsed + ms)
            .putLong(K_UNTIL_WALL, if (ms == null) 0L else System.currentTimeMillis() + ms)
            .apply()
    }

    fun disable(context: Context) {
        Prefs.get(context).raw().edit()
            .putBoolean(K_ON, false)
            .remove(K_AT_ELAPSED)
            .remove(K_UNTIL_ELAPSED)
            .remove(K_UNTIL_WALL)
            .apply()
    }

    fun isActive(context: Context): Boolean {
        val sp = Prefs.get(context).raw()
        if (!sp.getBoolean(K_ON, false)) return false

        // Khoi dong lai may thi dong ho tuong doi bat dau lai tu gan 0, nen moc luu
        // truoc do lon hon hien tai. Do la cach biet may vua reboot ma khong can
        // tin vao dong ho he thong.
        val batDau = sp.getLong(K_AT_ELAPSED, 0L)
        if (SystemClock.elapsedRealtime() < batDau) {
            disable(context)
            return false
        }

        if (sp.getLong(K_UNTIL_ELAPSED, 0L) == 0L) return true
        return remainingMs(context) > 0
    }

    /** Co dat han hay khong. Khong dat han thi [remainingMs] vo nghia. */
    fun coHan(context: Context): Boolean =
        Prefs.get(context).raw().getLong(K_UNTIL_ELAPSED, 0L) != 0L

    /**
     * So milli giay con lai khi co dat han. Tra 0 khi khong dat han hoac da tat,
     * nen dung [isActive] de hoi "dang bat khong", dung ham nay chi de hien so phut.
     */
    fun remainingMs(context: Context): Long {
        val sp = Prefs.get(context).raw()
        val untilElapsed = sp.getLong(K_UNTIL_ELAPSED, 0L)
        if (untilElapsed == 0L) return 0L

        val nowElapsed = SystemClock.elapsedRealtime()
        if (nowElapsed > untilElapsed) return 0L

        // Cung dung hai dong ho nhu phien choi cua con: het theo cai nao den truoc.
        val byElapsed = untilElapsed - nowElapsed
        val byWall = sp.getLong(K_UNTIL_WALL, 0L) - System.currentTimeMillis()
        return minOf(byElapsed, byWall).coerceAtLeast(0L)
    }

    /** Mot cau ngan de hien len man hinh va gui len Telegram. */
    fun moTa(context: Context): String = when {
        !isActive(context) -> "đã tắt"
        coHan(context) -> "còn ${remainingMs(context) / 60_000 + 1} phút"
        else -> "không đặt hạn, tự tắt khi khởi động lại máy"
    }
}
