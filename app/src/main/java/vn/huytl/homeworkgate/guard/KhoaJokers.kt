package vn.huytl.homeworkgate.guard

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.webkit.WebView
import androidx.core.content.ContextCompat
import org.json.JSONObject
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.pin.Jokers
import vn.huytl.homeworkgate.pin.TietKiemPin
import java.util.Calendar

private const val TAG = "KhoaJokers"

/**
 * Lop chan chinh bang Jokers (anh Huy chot 14/11/2026): luc app nao khong duoc dung thi tam dung
 * no (`pm suspend`), luc duoc dung lai thi bo tam dung. App bi tam dung hien xam o man hinh chinh,
 * bam vao chi ra hop bao cua Android voi cau bao lay tu [LuatChanApp], khong chay nen, khong hien
 * thong bao, khong phat tieng.
 *
 * Tro nang (GuardAccessibilityService) van chay nhu cu va xet cung bo luat, lam lop du phong: Jokers
 * khong chay (khoi dong lai may luc khong co Wi-Fi, tat go loi USB roi bat lai ma Jokers khong tu
 * len, Ba Huy go Jokers) thi tro nang van day app ve man hinh chinh. Anh Huy chon giu ca hai lop
 * vi ly do do.
 *
 * Luat nao di qua Jokers (anh Huy chot "nhu de xuat"): het gio choi thi tam dung moi app tru danh
 * sach "Dung khi het gio choi" va "Dung moi luc"; gio ngu, gio di hoc thi tam dung them danh sach
 * dau; "App chan han" luc nao cung tam dung; "Gio rieng tung app" het phut thi tam dung toi nua dem.
 * Cai dat, cho ung dung, man hinh chinh, ban phim khong bao gio tam dung (xem [khongBaoGioDung]):
 * tam dung chung ma Jokers chet truoc khi mo lai thi tablet thanh cuc gach. Cac app do van do tro
 * nang chan nhu cu.
 *
 * Che do Ba Huy (ParentMode) thi khong tam dung app nao, mo lai het.
 *
 * Chi dung app co bieu tuong o man hinh chinh. App nen (dich vu Google, WebView, kho danh ba...)
 * khong bao gio den truoc mat nen tro nang chua tung chan, ma tam dung chung thi app khac hong theo.
 *
 * Ghi lai app nao do chinh lop nay tam dung, kem cau bao, o file prefs thuong rieng [TEP] (khong o
 * kho chinh: DongBo nghe kho chinh, de o do thi moi lan doi keo theo mot luot ghi Firestore). Chi
 * mo lai app co trong so nay: app do Tiet kiem pin tam dung ([TietKiemPin]) hay ai khac tam dung
 * thi de nguyen. Hai lop cung tam dung mot app thi lop nao nha ra sau moi bo tam dung that.
 *
 * Moi lan xet doc trang thai that cua tung app (co FLAG_SUSPENDED), khong tin so ghi: khoi dong lai
 * may thi Android tu bo tam dung (app Lich thu tren may ao), so ghi van con, lan xet sau tam dung lai.
 */
object KhoaJokers {
    private const val TEP = "khoa_jokers"
    private const val DA_DUNG = "da_dung"

    /** Gop cac lan ghi prefs lien nhau (mot lan duyet ghi nhieu khoa) thanh mot lan xet. */
    private const val GOP_MS = 700L

    /** Nhip [dongBoThua] thua nhat. */
    private const val NHIP_THUA_MS = 30_000L

    private val tay = Handler(Looper.getMainLooper())

    @Volatile
    private var ct: Context? = null

    @Volatile
    private var lanThua = 0L

    /** May dang tat: thoi xet, de khong tam dung lai app vua mo truoc khi tat ([moHetTruocKhiTat]). */
    @Volatile
    private var dangTat = false

    /**
     * App Android tu choi tam dung (man hinh chinh, trinh goi dien, quan tri thiet bi... xem
     * SuspendPackageHelper.canSuspendPackageForUser). Nho trong bo nho de khong goi lenh lai moi lan
     * xet; app doi (cai lai, cap nhat) thi xoa.
     */
    private val khongDungDuoc = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val xet = Runnable {
        val c = ct ?: return@Runnable
        if (dangTat) return@Runnable
        Jokers.lam { runCatching { xetThat(c) }.onFailure { Log.w(TAG, "xet loi", it) } }
    }

    /**
     * May sap tat hay khoi dong lai: mo lai moi app dang tam dung, ca app cua Tiet kiem pin.
     *
     * Ly do (thay tren may ao dt_bahuy, Android 16, ngay 14/11/2026): app bi `pm suspend` van bi
     * tam dung sau khi khoi dong lai, trong khi Jokers chi tu chay lai khi luc khoi dong co Wi-Fi.
     * Khong mo truoc khi tat thi Jokers khong len la app xam mai, ke ca luc Le Hoa co gio choi, va
     * khong ai tren tablet mo duoc. Mo roi thi sau khi khoi dong lai tro nang chan nhu cu, toi khi
     * Jokers chay lai thi lop nay tam dung lai.
     *
     * Chi do duoc khi tat may dang hoang (Android gui ACTION_SHUTDOWN va cho toi 10 giay). Het pin,
     * treo may thi khong kip: lan do van phai cho Jokers chay lai.
     */
    private val tatMay = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            dangTat = true
            tay.removeCallbacks(xet)
            val xong = goAsync()
            val c = context.applicationContext
            Jokers.lam {
                try {
                    moHetTruocKhiTat(c)
                } finally {
                    xong.finish()
                }
            }
        }
    }

    private val ngheDoi = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        ct?.let { dongBo(it) }
    }

    private val nhanTin = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.data?.schemeSpecificPart?.let { khongDungDuoc.remove(it) }
            dongBo(context)
        }
    }

    /**
     * Goi mot lan trong App.onCreate. Nghe moi lan kho chinh doi (cong mo dong, che do Ba Huy, danh
     * sach app di qua nhieu khoa, loc thieu mot khoa la het gio ma app van mo; giong
     * CatMangVpn.theoDoi), app moi cai hay vua go, man hinh bat len, va Jokers vua chay lai.
     *
     * Nhung gi doi theo dong ho ma khong ai ghi prefs (het phien, het che do Ba Huy, toi gio di hoc,
     * nua dem) thi cac cho dang goi CatMangVpn.dongBo goi them [dongBo] hay [dongBoThua].
     */
    fun theoDoi(context: Context) {
        val ung = context.applicationContext
        ct = ung
        Prefs.get(ung).raw().registerOnSharedPreferenceChangeListener(ngheDoi)
        val goi = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(ung, nhanTin, goi, ContextCompat.RECEIVER_NOT_EXPORTED)
        val manHinh = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(ung, nhanTin, manHinh, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(
            ung, tatMay, IntentFilter(Intent.ACTION_SHUTDOWN), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Jokers.batDau { dongBo(ung) }
        dongBo(ung)
    }

    /** Xet lai ngay (sau mot nhip gop ngan). Goi tu luong nao cung duoc. */
    fun dongBo(context: Context) {
        ct = context.applicationContext
        tay.removeCallbacks(xet)
        tay.postDelayed(xet, GOP_MS)
    }

    /** [dongBo] cho vong lap chay deu, nhieu nhat nua phut mot lan. */
    fun dongBoThua(context: Context) {
        val bayGio = SystemClock.elapsedRealtime()
        if (lanThua > 0L && bayGio - lanThua < NHIP_THUA_MS) return
        lanThua = bayGio
        dongBo(context)
    }

    /**
     * Gio choi coi nhu con mo voi Jokers. Ngoai luc con gio that, tinh ca luc phien tu dung vi man
     * hinh tat qua 60 giay (GateStore.dungViTatManHinh): bat man hinh len la phien chay tiep, ma
     * tam dung roi bo tam dung moi lan tat mo man hinh thi Le Hoa mo khoa xong bam app co the gap
     * hop "khong dung duoc" trong luc Jokers chua kip mo lai. Man hinh dang tat thi khong ai dung app.
     */
    fun moChoJokers(gate: GateStore): Boolean =
        gate.isOpen() || (gate.state == GateState.PAUSED && gate.dungViTatManHinh())

    /** App lop nay dang tam dung theo so ghi, cho [TietKiemPin] biet app nao khong duoc mo lai. */
    fun dangGiu(context: Context): Set<String> = docSo(context).keys

    /**
     * Cac app phai tam dung luc nay, kem cau bao trong hop cua Android. Rong khi dang che do Ba Huy.
     */
    fun canTamDung(context: Context): Map<String, String> {
        val c = context.applicationContext
        if (ParentMode.isActive(c)) return emptyMap()
        val prefs = Prefs.get(c)
        val gate = GateStore(c)
        val mo = moChoJokers(gate)
        val hoc = TinhLoiNhac.khoangDangChan(Calendar.getInstance(), prefs.batManChan, prefs.buoiDuocMoSom)
        val chua = khongBaoGioDung(c)
        val pm = c.packageManager
        val ra = LinkedHashMap<String, String>()
        for (goi in cacAppCoBieuTuong(c)) {
            if (goi in chua) continue
            val ly = LuatChanApp.lyDo(c, goi, prefs, gate, mo, hoc) { tenApp(pm, it) } ?: continue
            ra[goi] = "${ly.first}. ${ly.second}"
        }
        return ra
    }

    /**
     * App khong bao gio tam dung du luat noi gi: cac app Tiet kiem pin cung khong dung (chinh app
     * nay, Jokers, Tailscale, man hinh chinh, ban phim, cac app he thong tro nang mien tru hay canh),
     * them cho ung dung (tro nang van chan), dich vu Google va WebView (app khac dung chung, tam dung
     * la app duoc phep cung hong), va app dang hen bao thuc gan nhat (app bi tam dung thi khong duoc
     * reo: tam dung app Dong ho trong gio ngu la sang mai khong co bao thuc).
     */
    fun khongBaoGioDung(context: Context): Set<String> = buildSet {
        addAll(TietKiemPin.khongDuocDung(context))
        addAll(GuardAccessibilityService.STORE_PACKAGES)
        add("com.google.android.gms")
        add("com.google.android.gsf")
        runCatching { WebView.getCurrentWebViewPackage()?.packageName }.getOrNull()?.let { add(it) }
        runCatching {
            context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.showIntent?.creatorPackage
        }.getOrNull()?.let { add(it) }
    }

    /** Goi tren luong cua Jokers, xem [tatMay]. */
    private fun moHetTruocKhiTat(c: Context) {
        val tat = (docSo(c).keys + TietKiemPin.dangTamDung(c))
            .filter { coCai(c, it) && dangTamDungThat(c, it) }
        if (tat.isNotEmpty()) Jokers.chayLayThang("pm", "unsuspend", *tat.toTypedArray())
        ghiSo(c, emptyMap())
        TietKiemPin.quenDaDung(c)
        Log.i(TAG, "tat may: da mo lai ${tat.size} app")
    }

    /** Goi tren luong cua Jokers. */
    private fun xetThat(c: Context) {
        if (!Jokers.sanSang()) return
        val muon = canTamDung(c)
        val so = docSo(c).toMutableMap()
        val truoc = so.toMap()

        // Tam dung app moi phai dung, hay doi cau bao (vd tu "Het gio" sang "Toi gio ngu"). Goi lai
        // pm suspend voi app dang tam dung thi Android chi thay cau bao.
        val canDung = muon.filter { (goi, cau) ->
            goi !in khongDungDuoc && (so[goi] != cau || !dangTamDungThat(c, goi))
        }
        for ((cau, ds) in canDung.entries.groupBy({ it.value }, { it.key })) {
            Jokers.chayLayThang("pm", "suspend", "--dialogMessage", cau, *ds.toTypedArray())
            for (goi in ds) {
                if (dangTamDungThat(c, goi)) {
                    so[goi] = cau
                } else if (coCai(c, goi)) {
                    khongDungDuoc += goi
                    Log.w(TAG, "Android khong cho tam dung $goi")
                }
            }
        }

        // Mo lai app khong con phai dung. App Tiet kiem pin dang giu thi de no tu mo luc bat man hinh.
        val bo = so.keys - muon.keys
        if (bo.isNotEmpty()) {
            val pin = TietKiemPin.dangTamDung(c)
            val canMo = bo.filter { it !in pin && coCai(c, it) && dangTamDungThat(c, it) }
            if (canMo.isNotEmpty()) Jokers.chayLayThang("pm", "unsuspend", *canMo.toTypedArray())
            for (goi in bo) {
                if (goi in pin || !coCai(c, goi) || !dangTamDungThat(c, goi)) so.remove(goi)
            }
        }

        if (so != truoc) {
            ghiSo(c, so)
            Log.i(TAG, "dang tam dung ${so.size} app: ${so.keys.sorted().joinToString(",")}")
        }
    }

    private fun cacAppCoBieuTuong(c: Context): Set<String> {
        val chinh = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching {
            c.packageManager.queryIntentActivities(chinh, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
        }.getOrDefault(emptySet())
    }

    private fun dangTamDungThat(c: Context, goi: String): Boolean = try {
        c.packageManager.getApplicationInfo(goi, 0).flags and ApplicationInfo.FLAG_SUSPENDED != 0
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun coCai(c: Context, goi: String): Boolean = try {
        c.packageManager.getApplicationInfo(goi, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun tenApp(pm: PackageManager, goi: String): String = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(goi, 0)).toString()
    }.getOrDefault(goi)

    private fun tep(c: Context) = c.applicationContext.getSharedPreferences(TEP, Context.MODE_PRIVATE)

    private fun docSo(c: Context): Map<String, String> {
        val chu = tep(c).getString(DA_DUNG, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(chu)
            o.keys().asSequence().associateWith { o.getString(it) }
        }.getOrDefault(emptyMap())
    }

    private fun ghiSo(c: Context, so: Map<String, String>) {
        val o = JSONObject()
        so.forEach { (k, v) -> o.put(k, v) }
        // commit: tien trinh co the bi giet ngay sau, mat so la lan sau khong biet app nao do minh
        // tam dung, app nam xam mai.
        tep(c).edit().putString(DA_DUNG, o.toString()).commit()
    }
}
