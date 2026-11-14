package vn.huytl.homeworkgate.pin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.guard.GuardAccessibilityService
import vn.huytl.homeworkgate.guard.KhoaJokers
import vn.huytl.homeworkgate.guard.TruyCapTuXa

private const val TAG = "TietKiemPin"

/**
 * Tu dong tiet kiem pin khi man hinh tat (anh Huy chot 05/11/2026). Chep cach lam cua app Lich
 * (pin/TietKiemPin.kt ban 5.73 cua kho lich), bo phan Tailscale va dong bo tai khoan.
 *
 * - Man hinh tat du [Prefs.pinPhut] phut (0 la ngay) thi bat che do Tiet kiem pin cua Android. Du
 *   [Prefs.pinPhutDung] phut thi tam dung cac app trong [Prefs.tamDungPackages] qua Jokers
 *   Permissions: `pm suspend` (app xam, khong mo duoc, an thong bao) roi `am kill` (tat tien trinh,
 *   cat ket noi mang de khoi danh thuc may; `pm suspend` mot minh khong tat tien trinh). Mo man
 *   hinh thi hoan lai ngay, Le Hoa dung may nhu thuong. Luc moi cai: 10 phut va 5 phut, nhu app
 *   Lich (anh Huy chot "như đề xuất").
 * - Dang cam sac thi khong lam gi, va dang tiet kiem ma cam sac thi hoan lai.
 * - Chi hoan lai cai app da doi: Tiet kiem pin do nguoi dung tu bat thi de nguyen.
 * - Tailscale khong bao gio bi tam dung (anh Huy chot): anh Huy can vao tablet tu xa bat cu luc
 *   nao, va Tailscale da it ton pin (do 04/11/2026: 6,5 mAh trong 15 gio).
 * - Khong dung `am force-stop`: app bi khoa o trang thai dung, mo man hinh roi van khong nhan thong
 *   bao toi khi mo app (app Lich gap 03/11/2026).
 *
 * Do pin tablet that ngay 04/11/2026 (adb dumpsys batterystats): tat man hinh chi hao khoang
 * 0,6%/gio, bat man hinh khoang 7%/gio. Phan nay bot duoc phan nho luc tat man hinh; anh Huy biet
 * so do va van muon lam.
 *
 * Bat Tiet kiem pin can WRITE_SECURE_SETTINGS (da cap qua adb ngay 04/11/2026 cho lenh /goloi);
 * thieu thi bo qua. Tam dung app can Jokers ([Jokers]); thieu thi bo qua. Tinh trang hai thu
 * nay ghi vao hop/caidat (DongBo.banCaiDat) de Bang dieu khien bao.
 *
 * Cau hinh nam o kho chinh ([Prefs]); trang thai (luc tat man hinh, da bat Tiet kiem pin, app nao
 * dang tam dung) nam o file prefs thuong rieng [TEP]: no doi moi lan tat man hinh, ma DongBo nghe
 * kho chinh, de o do thi moi lan tat man hinh keo theo mot luot ghi Firestore.
 */
object TietKiemPin {
    private const val TEP = "tiet_kiem_pin"
    private const val DA_BAT_TKP = "da_bat_tkp"
    private const val TAT_LUC = "tat_luc"
    private const val DA_DUNG = "da_dung"

    const val TAILSCALE = "com.tailscale.ipn"

    /** Settings.Global.LOW_POWER_MODE, bi an trong SDK. */
    private const val LOW_POWER = "low_power"

    /** Hen tre hon moc vai giay, de luc bao thuc chay man hinh da tat du so phut. */
    private const val TRE_MS = 5_000L

    /**
     * Goi mot lan luc app chay (App.onCreate): nghe man hinh bat tat va cam rut sac (chi nghe duoc
     * khi tien trinh dang chay, ma tien trinh nay song suot nho dich vu tro nang va ApprovalService),
     * nghe Jokers, roi dua may ve dung trang thai.
     */
    fun batDau(context: Context) {
        val app = context.applicationContext
        val loc = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(app, ManHinhReceiver(), loc, ContextCompat.RECEIVER_NOT_EXPORTED)
        // Jokers vua chay lai (khoi dong may, bat lai go loi) thi mo lai app con tam dung tu truoc,
        // hay tam dung neu man hinh dang tat; va ghi lai tinh trang cho Bang dieu khien.
        Jokers.batDau {
            apDung(app)
            DongBo.dayCaiDatNeuDoi(app)
        }
        apDung(app)
    }

    /** Luc nay nen tiet kiem khong: man hinh da tat du [phut] phut ([daTatMs] null la dang sang). */
    fun nenTietKiem(bat: Boolean, phut: Int, daTatMs: Long?): Boolean =
        bat && daTatMs != null && daTatMs >= phut * 60_000L

    /**
     * App can tam dung luc nay: app Ba Huy chon, tru app da tam dung va [khongDung] (xem
     * [khongDuocDung]).
     */
    fun canTamDung(apps: Set<String>, daDung: Set<String>, khongDung: Set<String>): List<String> =
        apps.filter { it !in daDung && it !in khongDung }.sorted()

    /**
     * App khong bao gio tam dung du co trong danh sach: chinh app nay va Jokers (tam dung thi khong
     * tu mo lai duoc), Tailscale (anh Huy chot), man hinh chinh va ban phim (tam dung ma Jokers chet
     * truoc khi mo lai thi tablet thanh cuc gach), va cac app he thong guard mien tru hay canh (Cai
     * dat, trinh cai app, goi dien). Man chon app da an cac app nay; loc them o day phong danh sach
     * cu hay Bang dieu khien gui len.
     */
    fun khongDuocDung(context: Context): Set<String> = buildSet {
        add(context.packageName)
        add(Jokers.GOI)
        add(TAILSCALE)
        addAll(GuardAccessibilityService.ALWAYS_ALLOWED)
        addAll(GuardAccessibilityService.GUARDED_SYSTEM_PACKAGES)
        val pm = context.packageManager
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        runCatching {
            pm.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.activityInfo?.packageName }
        }.getOrDefault(emptyList()).forEach { add(it) }
        runCatching {
            context.getSystemService(InputMethodManager::class.java)
                .enabledInputMethodList.mapNotNull { it.packageName }
        }.getOrDefault(emptyList()).forEach { add(it) }
    }

    /**
     * Dua tablet ve dung trang thai luc nay roi hen lan sau. Goi luc app chay, luc man hinh bat tat,
     * luc cam rut sac, luc bao thuc toi, luc Jokers doi tinh trang va khi Ba Huy doi cai dat. Goi
     * nhieu lan cung khong sao.
     */
    fun apDung(context: Context) {
        val app = context.applicationContext
        val prefs = Prefs.get(app)
        val t = tep(app)
        val bayGio = System.currentTimeMillis()

        // Luc man hinh tat: tien trinh chay lai khi man hinh dang tat thi tinh tu bay gio.
        val manHinhSang = app.getSystemService(PowerManager::class.java)?.isInteractive != false
        var tatLuc = t.getLong(TAT_LUC, 0L).takeIf { it > 0 }
        if (manHinhSang) {
            if (tatLuc != null) t.edit().remove(TAT_LUC).apply()
            tatLuc = null
        } else if (tatLuc == null) {
            tatLuc = bayGio
            t.edit().putLong(TAT_LUC, bayGio).apply()
        }

        val bat = prefs.pinBat
        val phut = prefs.pinPhut
        val phutDung = prefs.pinPhutDung
        val daTat = tatLuc?.let { bayGio - it }
        val sac = dangSac(app)
        if (nenTietKiem(bat, phut, daTat) && !sac) batTietKiem(app) else hoanTietKiem(app)
        if (nenTietKiem(bat, phutDung, daTat) && !sac) {
            val apps = prefs.tamDungPackages
            // Viec can lenh shell lam tren luong cua Jokers, lan luot.
            Jokers.lam { tamDungApp(app, apps) }
        } else {
            Jokers.lam { moLaiApp(app) }
        }
        hen(app, bat, setOf(phut, phutDung), tatLuc)
    }

    /**
     * Quen so app dang tam dung, sau khi KhoaJokers da mo lai het luc may sap tat. Luc bat lai,
     * man hinh tat du lau thi phan nay tam dung lai tu dau.
     */
    fun quenDaDung(context: Context) {
        tep(context.applicationContext).edit().remove(DA_DUNG).commit()
    }

    /** App dang bi tam dung vi tiet kiem pin, cho man Tiet kiem pin tren tablet. */
    fun dangTamDung(context: Context): Set<String> =
        tep(context.applicationContext).getStringSet(DA_DUNG, null).orEmpty().toSet()

    private fun batTietKiem(context: Context) {
        val t = tep(context)
        if (t.getBoolean(DA_BAT_TKP, false) || dangTietKiem(context) || !TruyCapTuXa.coQuyen(context)) return
        try {
            Settings.Global.putInt(context.contentResolver, LOW_POWER, 1)
            t.edit().putBoolean(DA_BAT_TKP, true).apply()
            Log.i(TAG, "bat Tiet kiem pin")
        } catch (e: Exception) {
            Log.w(TAG, "Khong bat duoc Tiet kiem pin", e)
        }
    }

    private fun hoanTietKiem(context: Context) {
        val t = tep(context)
        if (!t.getBoolean(DA_BAT_TKP, false)) return
        t.edit().remove(DA_BAT_TKP).apply()
        if (!dangTietKiem(context) || !TruyCapTuXa.coQuyen(context)) return
        try {
            Settings.Global.putInt(context.contentResolver, LOW_POWER, 0)
            Log.i(TAG, "tat Tiet kiem pin")
        } catch (e: Exception) {
            Log.w(TAG, "Khong tat duoc Tiet kiem pin", e)
        }
    }

    /** Goi tren luong cua Jokers. */
    private fun tamDungApp(context: Context, apps: Set<String>) {
        if (!Jokers.sanSang()) return
        val t = tep(context)
        val daDung = t.getStringSet(DA_DUNG, null).orEmpty().toMutableSet()
        val can = canTamDung(apps, daDung, khongDuocDung(context)).filter { coCai(context, it) }
        if (can.isEmpty()) return
        // App lop khoa (KhoaJokers, 14/11/2026) dang tam dung thi chi tat tien trinh: goi lai pm
        // suspend khong kem cau bao la xoa mat cau "Hết giờ rồi..." cua lop khoa. Van ghi vao so cua
        // phan nay, de lop khoa nha ra trong luc man hinh con tat thi app van nam yen.
        val khoa = KhoaJokers.dangGiu(context)
        for (goi in can) {
            val lenh = if (goi in khoa) "am kill $goi" else "pm suspend $goi && am kill $goi"
            if (Jokers.chay(lenh)) daDung += goi
        }
        t.edit().putStringSet(DA_DUNG, daDung).apply()
        Log.i(TAG, "tam dung: $daDung")
    }

    /**
     * Mo lai moi app da tam dung, ke ca app Ba Huy vua bo khoi danh sach. Jokers chua chay thi de
     * lan sau: app con xam toi khi Jokers chay lai (no tu goi [apDung]) hay toi khi khoi dong lai
     * may (Android tu bo tam dung, app Lich thu tren may ao). Goi tren luong cua Jokers.
     */
    private fun moLaiApp(context: Context) {
        val t = tep(context)
        val daDung = t.getStringSet(DA_DUNG, null).orEmpty().toMutableSet()
        if (daDung.isEmpty() || !Jokers.sanSang()) return
        // App lop khoa dang giu (het gio choi, gio ngu...) thi chi bo khoi so cua phan nay, khong mo:
        // lop khoa tu mo khi het luat chan. Xem KhoaJokers.
        val khoa = KhoaJokers.dangGiu(context)
        for (goi in daDung.toList()) {
            if (goi in khoa || Jokers.chay("pm unsuspend $goi") || !coCai(context, goi)) daDung -= goi
        }
        t.edit().putStringSet(DA_DUNG, daDung).apply()
        Log.i(TAG, "mo lai app, con tam dung: $daDung")
    }

    /**
     * Hen bao thuc danh thuc may dung luc man hinh tat du so phut. Bao thuc khong chinh xac bi
     * Android cho tre toi 1 gio luc may ngu sau (app Lich thu tren may ao Android 13 ngay
     * 02/11/2026), nen dung bao thuc chinh xac: app nay khai USE_EXACT_ALARM (Android 13 tro len tu
     * cap). Khong co quyen thi dung bao thuc thuong, nhu [vn.huytl.homeworkgate.guard.MocGio].
     */
    private fun hen(context: Context, bat: Boolean, cacPhut: Set<Int>, tatLuc: Long?) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val y = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, Receiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val bayGio = System.currentTimeMillis()
        val luc = if (bat && tatLuc != null) {
            cacPhut.map { tatLuc + it * 60_000L + TRE_MS }.filter { it > bayGio }.minOrNull()
        } else {
            null
        }
        if (luc == null) {
            am.cancel(y)
            return
        }
        runCatching {
            val chinhXac = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (chinhXac) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, luc, y)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, luc, y)
            }
        }.onFailure { Log.w(TAG, "Khong hen duoc bao thuc", it) }
    }

    private fun tep(context: Context) = context.getSharedPreferences(TEP, Context.MODE_PRIVATE)

    private fun dangTietKiem(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

    private fun dangSac(context: Context): Boolean {
        val pin = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        return pin.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }

    private fun coCai(context: Context, goi: String): Boolean = try {
        context.packageManager.getApplicationInfo(goi, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Man hinh bat tat, cam rut sac: ghi luc tat (man hinh sang thi apDung tu xoa) roi xet lai. */
    private class ManHinhReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                tep(context).edit().putLong(TAT_LUC, System.currentTimeMillis()).apply()
            }
            apDung(context)
        }
    }

    /** Bao thuc luc man hinh da tat du so phut. */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = apDung(context)
    }
}
