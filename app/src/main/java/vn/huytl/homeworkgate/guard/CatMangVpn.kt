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
 * Cung vi VPN chia theo UID, [canCat] bo app chay UID he thong: Cai dat, Bao mat cua
 * Xiaomi chung UID 1000 voi ca dong tien trinh he thong.
 *
 * Khong dung "Chan ket noi khong qua VPN" (lockdown): Android chan luon moi app nam
 * ngoai VPN, tuc la chan ca danh sach trang. Manifest khai SUPPORTS_ALWAYS_ON = false
 * de Cai dat khong cho bat che do do voi VPN nay.
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
        //
        // Bi da thi coi nhu khong cat, ke ca lan he thong goi lai service sau khi tien
        // trinh chet, hay mot lan startService xep hang tu truoc luc onRevoke chay. Thieu
        // dong nay thi prepare trong apDung gianh lai VPN cua app kia.
        val goi = if (biDa(this)) emptySet() else canCat(this)
        val loi = runCatching { apDung(goi) }.getOrElse { "lỗi ${it.javaClass.simpleName}" }
        if (loi != null) {
            lanHong = SystemClock.elapsedRealtime()
            if (loi != CHUA_CHO_PHEP) ghiHong(this, loi)
            // Dang co tun thi giu: doi danh sach giua luc khoa ma hong thi van cat theo
            // danh sach cu, con hon bo trong hoan toan.
            if (tun == null) tat()
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
     * @return null la xong; con lai la ly do phai cat ma khong bat duoc VPN,
     *         [CHUA_CHO_PHEP] la mat quyen VPN.
     */
    private fun apDung(goi: Set<String>): String? {
        if (goi.isEmpty()) {
            tat()
            return null
        }
        if (goi == dangCat && tun != null) return null

        // Goi truoc moi lan establish: khoi dong lai may thi he thong quen app nao dang
        // giu VPN, va establish cua app chua prepare tra ve null. Da duoc cho phep thi
        // ham nay tra null ngay, va gianh cho neu mot app VPN khac dang giu.
        if (VpnService.prepare(this) != null) {
            // Van con quyen ma prepare doi hop thoai: mot app VPN khac dang dat luon bat,
            // he thong khong cho ai gianh cho cua no.
            if (theoAppOps(this) == true) return "có app VPN khác đang đặt chế độ luôn bật"
            boChoPhep(this)
            return CHUA_CHO_PHEP
        }
        ghiChoPhep(this)

        val b = Builder()
            .setSession("Cắt mạng khi bị khoá")
            .addAddress(DIA_CHI_4, 32)
            .addAddress(DIA_CHI_6, 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
        val daThem = goi.filter { runCatching { b.addAllowedApplication(it) }.isSuccess }
        if (daThem.isEmpty()) return "không thêm được app nào vào VPN"

        val moi = b.establish() ?: return "hệ thống không dựng được VPN"
        val cu = tun
        tun = moi
        dangCat = goi
        lanHong = 0L
        xoaHong(this)
        runCatching { cu?.close() }
        Log.i(TAG, "cat mang ${daThem.joinToString(",")}")
        return null
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
            // Ba Huy bam "Quen VPN" trong Cai dat thi mat quyen luon, bang canh bao phai noi
            // "chua cho phep" chu khong phai "vua bi tat".
            if (theoAppOps(this) == false) boChoPhep(this)
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
                    "dòng cảnh báo VPN trong Cài đặt của app Nộp bài rồi bấm Xong."
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

        /** Ly do lan bat VPN hong gan nhat trong buoi khoa nay. Xem [ghiHong]. */
        private const val K_HONG = "cat_mang_hong"

        /** App nay tu ghi: Ba Huy da bam OK o hop thoai VPN. Xem [daChoPhep]. */
        private const val K_CHO_PHEP = "cat_mang_cho_phep"

        /** Ly do [apDung] tra ve khi mat quyen VPN. Khong phai hong: bang canh bao da bao. */
        private const val CHUA_CHO_PHEP = "chưa được cho phép VPN"

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

        /** So lan hong lien tiep tu lan bat duoc gan nhat. Xem [ghiHong]. */
        @Volatile
        private var soLanHong = 0

        /** Lan cuoi [dongBoThua] cho qua, theo elapsedRealtime. */
        @Volatile
        private var lanThua = 0L

        private val xet = Runnable {
            val c = ct ?: return@Runnable
            runCatching { xetThat(c) }.onFailure {
                // Thuong la startService bi tu choi luc app o nen.
                lanHong = SystemClock.elapsedRealtime()
                ghiHong(c, "không bật được dịch vụ VPN (${it.javaClass.simpleName})")
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
            val viec = LuatCatMang.viecVpn(
                can = canCat(c),
                dangCat = dangCat,
                biDa = biDa(c),
                choPhep = daChoPhep(c),
                dangNghi = lanHong > 0L &&
                    SystemClock.elapsedRealtime() - lanHong < NGHI_SAU_HONG_MS,
            )
            when (viec) {
                LuatCatMang.ViecVpn.TAT -> {
                    // Het buoi khoa: quen ca dau bi da lan dau hong, lan khoa sau thu lai.
                    lanHong = 0L
                    xoaHong(c)
                    val sp = Prefs.get(c).raw()
                    if (sp.getBoolean(K_BI_DA, false)) sp.edit().remove(K_BI_DA).apply()
                    dangSong?.tat()
                }
                LuatCatMang.ViecVpn.GIU -> Unit
                LuatCatMang.ViecVpn.BAT -> c.startService(Intent(c, CatMangVpn::class.java))
            }
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
            return ds.filter { laAppThuong(pm, it) }.toSet()
        }

        /**
         * App con cai va chay UID rieng cua no. App chay UID he thong (duoi
         * [Process.FIRST_APPLICATION_UID]) thi khong cat: VPN chia theo UID, cat Cai dat
         * la cat ca dong tien trinh he thong chung UID 1000.
         */
        fun laAppThuong(pm: PackageManager, goi: String): Boolean = runCatching {
            pm.getApplicationInfo(goi, 0).uid >= Process.FIRST_APPLICATION_UID
        }.getOrDefault(false)

        /**
         * Ba Huy da bam OK o hop thoai VPN cua he thong chua.
         *
         * Khong goi [VpnService.prepare]: prepare cua mot app da duoc cho phep se gianh
         * VPN cua app khac ngay luc goi, ma ham nay chay moi lan ve bang canh bao va thong
         * bao trang thai, ke ca giua gio choi.
         *
         * Hoi hai cho: AppOps, va dau chinh app nay ghi luc Ba Huy bam OK. Android 9 tro
         * xuong chua co ten op nay, hoi AppOps la nem loi; con ROM nao lam khac AOSP thi
         * AppOps co the khong doi sau hop thoai. Dau cu ma quyen da mat thi lan bat that
         * dau tien biet ngay ([apDung] xoa dau).
         */
        fun daChoPhep(c: Context): Boolean =
            theoAppOps(c) == true || Prefs.get(c).raw().getBoolean(K_CHO_PHEP, false)

        /** AppOps noi gi ve quyen VPN cua app nay. null la khong hoi duoc. */
        private fun theoAppOps(c: Context): Boolean? = runCatching {
            val ao = c.getSystemService(AppOpsManager::class.java)
            val che = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ao.unsafeCheckOpNoThrow(OP_VPN, Process.myUid(), c.packageName)
            } else {
                @Suppress("DEPRECATION")
                ao.checkOpNoThrow(OP_VPN, Process.myUid(), c.packageName)
            }
            che == AppOpsManager.MODE_ALLOWED
        }.getOrNull()

        /** Ba Huy vua bam OK o hop thoai VPN, hay prepare vua tra null. */
        fun ghiChoPhep(c: Context) {
            val sp = Prefs.get(c).raw()
            if (!sp.getBoolean(K_CHO_PHEP, false)) sp.edit().putBoolean(K_CHO_PHEP, true).apply()
        }

        private fun boChoPhep(c: Context) {
            val sp = Prefs.get(c).raw()
            if (sp.contains(K_CHO_PHEP)) sp.edit().remove(K_CHO_PHEP).apply()
        }

        /** VPN vua bi thu lai va chua bat lai. Xem [onRevoke]. */
        fun biDa(c: Context): Boolean = Prefs.get(c).raw().getBoolean(K_BI_DA, false)

        /** Ba Huy vua dat lai danh sach, hay vua cho phep lai: thu bat lai ngay. */
        fun boCoBiDa(c: Context) {
            lanHong = 0L
            Prefs.get(c).raw().edit().remove(K_BI_DA).apply()
        }

        /** Ly do lan bat VPN hong gan nhat trong buoi khoa nay, null la khong hong. */
        fun hong(c: Context): String? = Prefs.get(c).raw().getString(K_HONG, null)

        /**
         * Phai cat ma bat VPN khong duoc.
         *
         * Bang canh bao hien ngay tu lan dau. Tin Telegram chi gui o lan hong thu hai lien
         * tiep, tuc la da thu lai sau khoang nghi van hong: mot lan hong thoang qua luc may
         * vua khoi dong thi khong nen thanh tin bao dong. Moi buoi khoa gui nhieu nhat mot
         * tin, vi [xoaHong] chi chay khi bat duoc hay het khoa.
         */
        private fun ghiHong(c: Context, lyDo: String) {
            soLanHong += 1
            Log.w(TAG, "cat mang hong lan $soLanHong: $lyDo")
            val sp = Prefs.get(c).raw()
            if (sp.getString(K_HONG, null) != lyDo) sp.edit().putString(K_HONG, lyDo).apply()
            if (soLanHong != 2) return
            DayLog.add(c, "Không bật được VPN cắt mạng: $lyDo")
            Notifier.send(
                c,
                "⚠️ Tablet không bật được VPN cắt mạng ($lyDo), nên app trong danh sách " +
                    "cắt mạng vẫn lên mạng được dù đang bị khoá. Máy tự thử lại hai phút " +
                    "một lần. Nếu có app VPN khác đang đặt chế độ luôn bật thì tắt chế độ " +
                    "đó trong Cài đặt của tablet."
            )
        }

        private fun xoaHong(c: Context) {
            soLanHong = 0
            val sp = Prefs.get(c).raw()
            if (sp.contains(K_HONG)) sp.edit().remove(K_HONG).apply()
        }

        private fun tenApp(c: Context, goi: String): String = runCatching {
            val pm = c.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(goi, 0)).toString()
        }.getOrDefault(goi)
    }
}
