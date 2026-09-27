package vn.huytl.homeworkgate.guard

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.Log
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.LuatCatMang
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.telegram.Notifier
import java.util.Calendar

/**
 * Cat mang cac app Ba Huy chon, dung luc chung dang bi khoa. Luat nam o [LuatCatMang],
 * danh sach o [Prefs.catMangPackages].
 *
 * CACH LAM: mot VPN "ho den". Chi dua vao VPN dung cac app trong danh sach
 * (addAllowedApplication), route ca IPv4 lan IPv6 vao tun, roi khong ai doc tun. Goi
 * tin cua cac app do vao tun roi nam lai trong hang doi cua kernel, day thi bi bo.
 * App ngoai danh sach khong di qua VPN nen khong biet gi, ke ca chinh app nay.
 *
 * Thu tren may ao Android 13 ngay 27/9/2026: ten mien van phan giai duoc, vi VPN nay
 * khong khai DNS nen he thong hoi qua mang thuong. Goi du lieu thi vao tun het, nen app
 * cho het han roi moi bao mat mang, chu khong bao ngay.
 *
 * VI SAO KHONG DOC TUN. Doc thi phai co mot luong nam cho read(). Dong tun trong luc
 * luong do dang read() thi kernel chua nha tun ra cho toi khi read() tra ve, tuc la
 * cac app kia khong gui gi thi VPN nam do mai. Khong doc thi dong la tat ngay, va ca
 * ngay khong ton mot lan danh thuc CPU nao.
 *
 * KHONG BAO GIO ESTABLISH KHI CHUA THEM DUOC APP NAO: Builder khong co app nao trong
 * addAllowedApplication la MOI app di vao VPN, ke ca app nay. Tablet mat mang hoan
 * toan, Telegram va Bang dieu khien cung chet theo. Nen [apDung] dem so app da them.
 *
 * Khong dung "Chan ket noi khong qua VPN" (lockdown): Android chan luon moi app nam
 * ngoai VPN, tuc la chan ca danh sach trang.
 *
 * Day la service thuong, khong phai foreground. Establish xong thi he thong tu bind
 * vao service nay voi co BIND_FOREGROUND_SERVICE, tien trinh duoc giu nhu dang co dich
 * vu foreground, nen khong phai treo them mot thong bao thuong truc canh thong bao cua
 * [vn.huytl.homeworkgate.telegram.ApprovalService]. startService tu nen van duoc, vi
 * ApprovalService luon chay foreground va app da nam ngoai tiet kiem pin.
 *
 * VPN chung tien trinh voi guard. Tien trinh chet thi VPN mat theo; tien trinh len
 * lai thi App.onCreate goi [theoDoi], va no xet lai ngay.
 */
class CatMangVpn : VpnService() {

    private var tun: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        dangSong = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Tinh lai ngay luc nay chu khong mang danh sach theo intent: tu luc goi toi luc
        // service chay, cong co the da doi.
        val duoc = runCatching { apDung(canCat(this)) }.getOrElse {
            Log.w(TAG, "cat mang hong: ${it.message}")
            false
        }
        if (!duoc) {
            lanHong = SystemClock.elapsedRealtime()
            tat()
        }
        // Tien trinh bi giet giua luc dang cat thi he thong goi lai service, va lan goi
        // do tu tinh xem con can cat khong.
        return START_STICKY
    }

    /**
     * Dua VPN ve dung tap [goi]: rong thi tat, khac tap dang cat thi dung tun moi.
     *
     * Establish lan hai thi he thong thay tun cu bang tun moi roi moi go tun cu, nen doi
     * danh sach giua luc dang cat khong ho nhip nao. Dong tun cu sau cung.
     *
     * @return false la phai cat ma khong bat duoc VPN.
     */
    private fun apDung(goi: Set<String>): Boolean {
        if (goi.isEmpty()) {
            tat()
            return true
        }
        if (goi == dangCat && tun != null) return true

        // Goi truoc moi lan establish: khoi dong lai may thi he thong quen app nao dang
        // giu VPN, va establish cua app chua prepare tra ve null. Da duoc cho phep thi
        // ham nay tra null ngay, va gianh cho neu mot app VPN khac dang giu.
        if (VpnService.prepare(this) != null) {
            Log.i(TAG, "cat mang: chua duoc cho phep VPN")
            return false
        }

        val b = Builder()
            .setSession("Cắt mạng khi bị khoá")
            .addAddress(DIA_CHI_4, 32)
            .addAddress(DIA_CHI_6, 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
        val daThem = goi.filter { runCatching { b.addAllowedApplication(it) }.isSuccess }
        if (daThem.isEmpty()) return false

        val moi = b.establish()
        if (moi == null) {
            Log.w(TAG, "cat mang: establish tra ve null")
            return false
        }
        val cu = tun
        tun = moi
        dangCat = goi
        lanHong = 0L
        runCatching { cu?.close() }
        Log.i(TAG, "cat mang ${daThem.joinToString(",")}")
        return true
    }

    /** Go VPN va dung service. Chay tren luong chinh. */
    fun tat() {
        val cu = tun
        tun = null
        dangCat = emptySet()
        if (cu != null) {
            runCatching { cu.close() }
            Log.i(TAG, "thoi cat mang")
        }
        stopSelf()
    }

    /**
     * He thong thu VPN lai: mot app VPN khac vua bat, hay VPN bi ngat trong Cai dat.
     *
     * Chi bao Ba Huy va ghi nhat ky, khong gianh lai (Ba Huy chon ngay 27/9/2026). Gianh
     * lai ngay thi hai VPN da nhau lien tuc, lan nao cung mot tin. Nen danh dau bi da:
     * tu gio toi luc het khoa, [dongBo] khong bat lai. Toi luc khong con phai cat (gio
     * choi, hay Ba Huy mo toan bo may) thi quen dau do, lan khoa sau thu lai tu dau.
     *
     * He thong goi ham nay tren luong binder, khong phai luong chinh.
     */
    override fun onRevoke() {
        tay.post {
            val daCat = dangCat
            val dangCo = tun != null
            tat()
            if (!dangCo) return@post
            Prefs.get(this).raw().edit().putBoolean(K_BI_DA, true).apply()
            val ten = daCat.map { tenApp(this, it) }.sorted().joinToString(", ")
            Log.w(TAG, "VPN cat mang bi thu lai")
            DayLog.add(this, "VPN cắt mạng bị tắt, $ten lên mạng lại được")
            Notifier.send(
                this,
                "⚠️ Tablet vừa mất VPN cắt mạng, nên $ten lên mạng lại được dù đang " +
                    "bị khoá. Có thể Lê Hòa vừa bật một app VPN khác, hoặc ngắt VPN " +
                    "trong Cài đặt.\n" +
                    "Máy không tự giành lại. Hết giờ chơi lần sau máy bật lại. Muốn bật " +
                    "ngay thì gửi lại danh sách cắt mạng từ Bảng điều khiển, hoặc bấm " +
                    "dòng cảnh báo VPN trong Cài đặt của app Nộp bài."
            )
        }
    }

    override fun onDestroy() {
        runCatching { tun?.close() }
        tun = null
        dangCat = emptySet()
        if (dangSong === this) dangSong = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "HomeworkGate"

        /** Dia chi cua tun. Khong goi tin nao ra khoi day, nen chon dai nao cung duoc. */
        private const val DIA_CHI_4 = "10.111.222.1"
        private const val DIA_CHI_6 = "fd00:111:222::1"

        /** VPN vua bi thu lai giua luc dang cat. Xem [onRevoke]. */
        private const val K_BI_DA = "cat_mang_bi_da"

        /** Ten appop cua quyen VPN. Hang AppOpsManager.OPSTR_ACTIVATE_VPN khong co trong SDK. */
        private const val OP_VPN = "android:activate_vpn"

        /** Bat VPN hong thi nghi bay lau roi moi thu lai. Xem [xetThat]. */
        private const val NGHI_SAU_HONG_MS = 2 * 60_000L

        /** [dongBoThua] cho qua nhieu nhat mot lan trong khoang nay. */
        private const val NHIP_THUA_MS = 30_000L

        private val tay = Handler(Looper.getMainLooper())

        @Volatile
        private var ct: Context? = null

        @Volatile
        private var dangSong: CatMangVpn? = null

        /** Nhung app dang bi cat mang. Rong la VPN dang tat. */
        @Volatile
        var dangCat: Set<String> = emptySet()
            private set

        /** Lan gan nhat bat VPN khong duoc, theo elapsedRealtime. 0 la chua hong. */
        @Volatile
        private var lanHong = 0L

        /** Lan cuoi [dongBoThua] cho qua, theo elapsedRealtime. */
        @Volatile
        private var lanThua = 0L

        private val xet = Runnable {
            val c = ct ?: return@Runnable
            runCatching { xetThat(c) }.onFailure {
                // Thuong la startService bi tu choi luc app o nen.
                lanHong = SystemClock.elapsedRealtime()
                Log.w(TAG, "cat mang: ${it.message}")
            }
        }

        private val ngheDoi = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            ct?.let { dongBo(it) }
        }

        /**
         * Nghe moi lan prefs doi, tu luc tien trinh len toi luc no chet. Goi mot lan
         * trong App.onCreate.
         *
         * Khong loc khoa. Cong mo hay dong di qua nhieu khoa cua [GateStore], [ParentMode]
         * va [Prefs], va ca chuc cho trong app doi chung: lenh Telegram, lenh tu Bang
         * dieu khien, nut tren man hinh, bao thuc. Loc thieu mot khoa la het gio ma
         * YouTube van co mang, khong ai biet. Con xet lai thi re: doc vai gia tri roi so
         * voi tap dang cat, ca chum ghi lien nhau gop thanh mot lan tren luong chinh.
         *
         * Nhung gi doi theo dong ho ma khong ai ghi prefs, nhu het han che do Ba Huy hay
         * toi gio di hoc, thi nhip cua guard va vong xet cua ApprovalService goi [dongBo].
         */
        fun theoDoi(context: Context) {
            val ung = context.applicationContext
            ct = ung
            Prefs.get(ung).raw().registerOnSharedPreferenceChangeListener(ngheDoi)
            dongBo(ung)
        }

        /**
         * Xet lai xem co phai cat mang khong, roi bat hay tat VPN cho khop.
         *
         * Goi tu luong nao cung duoc: viec that chay tren luong chinh, va nhieu lan goi
         * sat nhau chi chay mot lan.
         */
        fun dongBo(context: Context) {
            ct = context.applicationContext
            tay.removeCallbacks(xet)
            tay.post(xet)
        }

        /**
         * [dongBo] cho vong lap chay deu, nhieu nhat nua phut mot lan. Vong xet cua
         * ApprovalService chay moi giay suot luc man chan gio hoc dang hien, ma o do chi
         * can mot tam luoi do cho nhung thay doi khong qua prefs.
         */
        fun dongBoThua(context: Context) {
            val bayGio = SystemClock.elapsedRealtime()
            if (lanThua > 0L && bayGio - lanThua < NHIP_THUA_MS) return
            lanThua = bayGio
            dongBo(context)
        }

        private fun xetThat(c: Context) {
            val can = canCat(c)
            val sp = Prefs.get(c).raw()
            if (can.isEmpty()) {
                lanHong = 0L
                if (sp.getBoolean(K_BI_DA, false)) sp.edit().remove(K_BI_DA).apply()
                dangSong?.tat()
                return
            }
            if (can == dangCat) return
            if (sp.getBoolean(K_BI_DA, false)) return
            // Chua cho phep thi thoi, bang canh bao da bao Ba Huy. Hoi truoc o day de khoi
            // bat service chi de no tu tat.
            if (!daChoPhep(c)) return
            // Vua bat hong thi nghi mot luc. Khong nghi thi moi lan prefs doi, vai lan mot
            // phut, lai bat service len roi tat.
            if (lanHong > 0L && SystemClock.elapsedRealtime() - lanHong < NGHI_SAU_HONG_MS) return
            c.startService(Intent(c, CatMangVpn::class.java))
        }

        /**
         * Nhung app phai mat mang ngay luc nay. Rong la khong cat gi, VPN phai tat.
         *
         * Chi lay app con cai tren may, va khong bao gio lay chinh app nay. Man chon app
         * cua tablet khong liet ke Nop bai, nhung danh sach ben Bang dieu khien thi co.
         * Tich nham no la tablet mat duong Telegram va Bang dieu khien dung luc bi khoa.
         *
         * Bo ca app dung moi luc: app do khong bao gio bi khoa, nen cung khong co luc nao
         * phai mat mang. Tich trung Telegram vao day thi Le Hoa van nhan duoc cho ba.
         */
        fun canCat(c: Context): Set<String> {
            val prefs = Prefs.get(c)
            val ds = prefs.catMangPackages - prefs.moiLucPackages - c.packageName
            if (ds.isEmpty()) return emptySet()
            val gate = GateStore(c)
            val cat = LuatCatMang.catMang(
                gateMo = gate.isOpen(),
                trongGioNgu = gate.trongGioNgu(),
                trongGioHoc = TinhLoiNhac.buoiDangChan(
                    Calendar.getInstance(), prefs.batManChan, prefs.buoiDuocMoSom
                ) != null,
                moToanBo = ParentMode.isActive(c),
            )
            if (!cat) return emptySet()
            val pm = c.packageManager
            return ds.filter { daCai(pm, it) }.toSet()
        }

        /**
         * Ba Huy da bam OK o hop thoai VPN cua he thong chua.
         *
         * Hoi AppOps chu khong goi [VpnService.prepare]: prepare cua mot app da duoc cho
         * phep se gianh VPN cua app khac ngay luc goi. Ham nay chay moi lan ve bang canh
         * bao va thong bao trang thai, ke ca giua gio choi. Hoi AppOps hong thi moi phai
         * dung prepare.
         */
        fun daChoPhep(c: Context): Boolean = runCatching {
            val ao = c.getSystemService(AppOpsManager::class.java)
            val che = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ao.unsafeCheckOpNoThrow(OP_VPN, Process.myUid(), c.packageName)
            } else {
                @Suppress("DEPRECATION")
                ao.checkOpNoThrow(OP_VPN, Process.myUid(), c.packageName)
            }
            che == AppOpsManager.MODE_ALLOWED
        }.getOrElse { VpnService.prepare(c) == null }

        /** VPN vua bi thu lai va chua bat lai. Xem [onRevoke]. */
        fun biDa(c: Context): Boolean = Prefs.get(c).raw().getBoolean(K_BI_DA, false)

        /** Ba Huy vua dat lai danh sach, hay vua cho phep lai: thu bat lai ngay. */
        fun boCoBiDa(c: Context) {
            lanHong = 0L
            Prefs.get(c).raw().edit().remove(K_BI_DA).apply()
        }

        private fun daCai(pm: PackageManager, goi: String): Boolean =
            runCatching { pm.getApplicationInfo(goi, 0) }.isSuccess

        private fun tenApp(c: Context, goi: String): String = runCatching {
            val pm = c.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(goi, 0)).toString()
        }.getOrDefault(goi)
    }
}
