package vn.huytl.homeworkgate.guard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import vn.huytl.homeworkgate.data.Prefs
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cho Ba Huy (va Claude Code tren Mac mini) vao tablet tu xa: bat tat Tailscale, go loi
 * khong day, go loi USB bang lenh Telegram /tailscale, /goiloi, /goiloiusb, xem bang /tuxa.
 *
 * VI SAO CO. Toi 4/11/2026 Firestore cua tablet treo ca ngay (Wi-Fi nha co IPv6 hong, xem
 * dongbo/UuTienIPv4) ma khong ai o nha, khong co cach nao doc logcat. Nguoi nha cai
 * Tailscale, bat go loi khong day, Claude Code noi adb qua Tailscale moi tim ra loi. Ngay
 * sau khi cai ban sua, Le Hoa cam may, che do Ba Huy tat, app cat mang lai va VPN cat mang
 * day Tailscale ra: mat duong vao. Anh Huy muon tu bat lai duoc tu Telegram (chot 4/11/2026,
 * Bang dieu khien lam sau).
 *
 * Luat anh Huy chot cung ngay:
 *  - Tailscale dang bat theo lenh thi thoi cat mang (Android chi chay mot VPN mot luc, xem
 *    [CatMangVpn]). App trong danh sach cat mang van bi chan mo nhu thuong. Tat Tailscale thi
 *    cat mang lai.
 *  - Bat roi thi giu toi khi Ba Huy tat, khong tu tat.
 *  - Khoi dong lai may thi khong tu bat lai: co Tailscale quen luon (giong [ParentMode]),
 *    con go loi khong day thi Android tu tat.
 *  - Go loi USB la muc "Go loi USB" thuong. Muc "(Cai dat bao mat)" rieng cua Xiaomi app
 *    khong doi duoc, de nguyen.
 *
 * QUYEN. Go loi nam trong Settings.Global, ghi phai co WRITE_SECURE_SETTINGS, quyen nay chi
 * cap duoc qua adb (`pm grant`, xem [LENH_CAP_QUYEN]). Tailscale thi khong can quyen gi: app
 * Tailscale nhan tin CONNECT_VPN, DISCONNECT_VPN tu app khac (IPNReceiver, kiem tren tablet
 * ngay 4/11/2026, Tailscale 1.104.4).
 *
 * HOP HOI CUA ANDROID. Bat go loi khong day tren mot mang Wi-Fi chua tung "Luon cho phep"
 * thi Android hien hop "Cho phep go loi khong day tren mang nay?" va de cai dat ve 0 cho toi
 * khi co nguoi bam (thu tren may ao ngay 4/11/2026). App bao lai cho Ba Huy chu khong tu bam.
 */
object TruyCapTuXa {
    private const val TAG = "HomeworkGate"

    const val GOI_TAILSCALE = "com.tailscale.ipn"
    private const val NHAN_TAILSCALE = "com.tailscale.ipn.IPNReceiver"
    private const val NOI_TAILSCALE = "com.tailscale.ipn.CONNECT_VPN"
    private const val NGAT_TAILSCALE = "com.tailscale.ipn.DISCONNECT_VPN"

    /** Ten trong Settings.Global. ADB_WIFI_ENABLED khong co trong SDK nen viet tay. */
    private const val ADB_USB = Settings.Global.ADB_ENABLED
    private const val ADB_WIFI = "adb_wifi_enabled"

    /** Dich vu mDNS ma adbd rao khi bat go loi khong day, mang so cong de noi. */
    private const val DICH_VU_ADB = "_adb-tls-connect._tcp"

    /** Co: Ba Huy dang bat Tailscale bang lenh. Nam trong prefs chinh de CatMangVpn nghe thay. */
    private const val K_TAILSCALE = "tuxa_tailscale"

    /** elapsedRealtime luc bat co. Nho hon gio hien tai la may da khoi dong lai. */
    private const val K_TAILSCALE_LUC = "tuxa_tailscale_luc"

    const val LENH_CAP_QUYEN =
        "adb shell pm grant vn.huytl.homeworkgate android.permission.WRITE_SECURE_SETTINGS"

    /** Cho Android doi cai dat go loi khong day roi moi doc lai. Hop hoi hien trong 1-2 giay. */
    private const val CHO_DOC_LAI_MS = 3_000L

    /** Ngat VPN cat mang truoc, roi moi bao Tailscale noi, de Tailscale khong da VPN dang song. */
    private const val CHO_NGAT_VPN_MS = 1_500L

    /** Cho Tailscale noi xong roi moi doc dia chi 100.x cua no. */
    private const val CHO_TAILSCALE_MS = 15_000L

    /** Tim cong go loi qua mDNS lau nhat bao lau. */
    private const val CHO_TIM_CONG_MS = 10_000L

    private val tay = Handler(Looper.getMainLooper())

    // ------------------------------------------------------------------ Tailscale

    /** Ba Huy dang bat Tailscale bang lenh, va may chua khoi dong lai tu luc do. */
    fun tailscaleDangBat(c: Context): Boolean {
        val sp = Prefs.get(c).raw()
        if (!sp.getBoolean(K_TAILSCALE, false)) return false
        if (SystemClock.elapsedRealtime() < sp.getLong(K_TAILSCALE_LUC, 0L)) {
            datCo(c, false)
            return false
        }
        return true
    }

    private fun datCo(c: Context, bat: Boolean) {
        val e = Prefs.get(c).raw().edit()
        if (bat) {
            e.putBoolean(K_TAILSCALE, true).putLong(K_TAILSCALE_LUC, SystemClock.elapsedRealtime())
        } else {
            e.remove(K_TAILSCALE).remove(K_TAILSCALE_LUC)
        }
        e.commit()
    }

    fun coTailscale(c: Context): Boolean = runCatching {
        c.packageManager.getPackageInfo(GOI_TAILSCALE, 0)
        true
    }.getOrDefault(false)

    /**
     * Bat Tailscale: dung cat mang, roi bao app Tailscale noi. [xong] nhan tin thu hai, sau
     * khoang 15 giay, noi Tailscale da co dia chi chua.
     */
    fun batTailscale(c: Context, xong: (String) -> Unit): String {
        if (!coTailscale(c)) return "Tablet chưa cài Tailscale."
        val ung = c.applicationContext
        val coDanhSach = Prefs.get(ung).catMangPackages.isNotEmpty()
        datCo(ung, true)
        CatMangVpn.dongBo(ung)
        tay.postDelayed({ guiTailscale(ung, NOI_TAILSCALE) }, CHO_NGAT_VPN_MS)
        tay.postDelayed({
            val ip = ipTailscale(ung)
            xong(
                if (ip != null) "Tailscale đã nối, địa chỉ $ip."
                else "Chưa thấy Tailscale nối sau ${CHO_TAILSCALE_MS / 1000} giây. Có thể " +
                    "Tailscale bị đăng xuất hoặc tablet mất mạng. Gõ /tuxa để xem lại."
            )
        }, CHO_TAILSCALE_MS)
        return "Đang bật Tailscale…" +
            if (coDanhSach) "\nTrong lúc Tailscale bật, tablet không cắt mạng các app trong " +
                "danh sách cắt mạng (vẫn chặn mở như thường)." else ""
    }

    /** Tat Tailscale va cat mang lai theo luat cu. */
    fun tatTailscale(c: Context): String {
        val ung = c.applicationContext
        datCo(ung, false)
        if (coTailscale(ung)) guiTailscale(ung, NGAT_TAILSCALE)
        CatMangVpn.dongBo(ung)
        return "Đã tắt Tailscale. Tablet cắt mạng lại theo giờ như cũ."
    }

    private fun guiTailscale(c: Context, viec: String) {
        runCatching {
            c.sendBroadcast(Intent(viec).setClassName(GOI_TAILSCALE, NHAN_TAILSCALE))
        }.onFailure { Log.w(TAG, "khong gui duoc $viec cho Tailscale", it) }
    }

    /**
     * Dia chi Tailscale (dai 100.64.0.0/10) cua tablet, null la Tailscale chua noi. Doc tu
     * mang VPN dang chay: VPN cua Tailscale ap cho moi app nen app nay thay no.
     */
    fun ipTailscale(c: Context): String? = runCatching {
        val cm = c.getSystemService(ConnectivityManager::class.java) ?: return null
        @Suppress("DEPRECATION")
        for (mang in cm.allNetworks) {
            val nc = cm.getNetworkCapabilities(mang) ?: continue
            if (!nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            val lp = cm.getLinkProperties(mang) ?: continue
            val dc = lp.linkAddresses.map { it.address }.firstOrNull { laDaiTailscale(it) }
            if (dc != null) return dc.hostAddress
        }
        null
    }.getOrNull()

    private fun laDaiTailscale(dc: InetAddress): Boolean {
        if (dc !is Inet4Address) return false
        val b = dc.address
        return (b[0].toInt() and 0xff) == 100 && (b[1].toInt() and 0xc0) == 64
    }

    // ------------------------------------------------------------------ go loi

    fun coQuyen(c: Context): Boolean =
        c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    private fun doc(c: Context, ten: String): Int =
        runCatching { Settings.Global.getInt(c.contentResolver, ten, 0) }.getOrDefault(0)

    /** Ghi mot muc go loi. Tra ve null la xong, khac null la ly do khong ghi duoc. */
    private fun ghi(c: Context, ten: String, bat: Boolean): String? {
        if (!coQuyen(c)) return chuaCoQuyen()
        return runCatching {
            Settings.Global.putInt(c.contentResolver, ten, if (bat) 1 else 0)
            null
        }.getOrElse { "Không đổi được cài đặt (${it.javaClass.simpleName})." }
    }

    private fun chuaCoQuyen() =
        "App chưa có quyền đổi cài đặt gỡ lỗi. Cần cấp một lần qua adb:\n$LENH_CAP_QUYEN"

    fun goLoiUsbDangBat(c: Context) = doc(c, ADB_USB) == 1
    fun goLoiKhongDayDangBat(c: Context) = doc(c, ADB_WIFI) == 1

    /** Bat tat muc "Go loi USB". Tat muc nay thi moi duong adb dong lai, ke ca qua Tailscale. */
    fun goLoiUsb(c: Context, bat: Boolean): String {
        ghi(c, ADB_USB, bat)?.let { return it }
        return if (bat) "Đã bật gỡ lỗi USB." else "Đã tắt gỡ lỗi USB."
    }

    /**
     * Bat go loi khong day. [xong] nhan tin thu hai: dia chi de noi, hay loi hop hoi dang
     * cho nguoi bam tren tablet.
     */
    fun batGoLoiKhongDay(c: Context, xong: (String) -> Unit): String {
        ghi(c, ADB_WIFI, true)?.let { return it }
        val ung = c.applicationContext
        tay.postDelayed({
            if (!goLoiKhongDayDangBat(ung)) {
                xong(
                    "Android chưa bật gỡ lỗi không dây. Thường là tablet đang hiện hộp " +
                        "\"Cho phép gỡ lỗi không dây trên mạng này?\": cần người bấm Cho phép " +
                        "(tick \"Luôn cho phép trên mạng này\" để lần sau khỏi hỏi). Hoặc " +
                        "tablet đang không nối Wi-Fi."
                )
            } else {
                timCong(ung) { cong -> xong(moTaKhongDay(ung, cong)) }
            }
        }, CHO_DOC_LAI_MS)
        return "Đang bật gỡ lỗi không dây…"
    }

    fun tatGoLoiKhongDay(c: Context): String {
        ghi(c, ADB_WIFI, false)?.let { return it }
        return "Đã tắt gỡ lỗi không dây."
    }

    private fun moTaKhongDay(c: Context, cong: Int?): String {
        if (cong == null) {
            // Thu tren may ao 4/11/2026: tat roi bat lai cach nhau nua giay thi adbd van mo
            // cong nhung mDNS khong rao nua, cho bao lau cung khong thay, toi lan tat bat sau.
            return "Gỡ lỗi không dây đang bật nhưng chưa tìm được số cổng. Thử /goiloi tat, " +
                "chờ vài giây rồi /goiloi bat."
        }
        val ts = ipTailscale(c)
        return "Gỡ lỗi không dây đang bật, cổng $cong.\n" +
            (if (ts != null) "adb connect $ts:$cong" else "Tailscale chưa nối (/tailscale bat).")
    }

    // ------------------------------------------------------------------ tinh trang

    /** Bang /tuxa. Co tim cong go loi nen tra qua [xong], cham nhat khoang 10 giay. */
    fun tinhTrang(c: Context, xong: (String) -> Unit) {
        val ung = c.applicationContext
        val ts = ipTailscale(ung)
        val dong = buildList {
            add(
                "• Tailscale: " + when {
                    ts != null && tailscaleDangBat(ung) -> "đang bật theo lệnh, địa chỉ $ts"
                    ts != null -> "đang nối, địa chỉ $ts"
                    tailscaleDangBat(ung) -> "đã bật theo lệnh nhưng chưa nối"
                    !coTailscale(ung) -> "chưa cài"
                    else -> "tắt"
                }
            )
            add("• Gỡ lỗi USB: " + if (goLoiUsbDangBat(ung)) "bật" else "tắt")
            add("• Quyền đổi cài đặt gỡ lỗi: " + if (coQuyen(ung)) "có" else "chưa có")
        }
        val dau = "TRUY CẬP TỪ XA\n" + dong.joinToString("\n")
        if (!goLoiKhongDayDangBat(ung)) {
            xong("$dau\n• Gỡ lỗi không dây: tắt")
            return
        }
        timCong(ung) { cong ->
            xong("$dau\n• Gỡ lỗi không dây: bật" +
                (cong?.let { ", cổng $it" } ?: ", chưa tìm được cổng (thử /goiloi tat rồi bat)") +
                if (cong != null && ts != null) "\nadb connect $ts:$cong" else "")
        }
    }

    // ------------------------------------------------------------------ tim cong

    /**
     * Tim so cong go loi khong day cua chinh tablet. Android doi cong moi lan bat, va mDNS
     * khong di qua Tailscale, nen ben ngoai khong biet cong; adbd rao no tren mDNS
     * ([DICH_VU_ADB]). Chi lay dich vu co dia chi la cua chinh may nay: o nha con co may khac
     * dang bat go loi (dien thoai cua Ba Huy).
     *
     * [xong] chay dung mot lan, tren luong chinh; null la khong tim duoc trong [CHO_TIM_CONG_MS].
     */
    fun timCong(c: Context, xong: (Int?) -> Unit) {
        val nsd = c.getSystemService(NsdManager::class.java)
        if (nsd == null) {
            xong(null)
            return
        }
        val cuaMay = diaChiCuaMay()
        val daXong = AtomicBoolean(false)
        val hang = ArrayDeque<NsdServiceInfo>()
        var dangGiai = false
        lateinit var nghe: NsdManager.DiscoveryListener

        fun ket(cong: Int?) {
            if (!daXong.compareAndSet(false, true)) return
            runCatching { nsd.stopServiceDiscovery(nghe) }
            tay.post { xong(cong) }
        }

        // resolveService cua Android 13 chi giai mot dich vu mot luc, nen giai lan luot.
        fun giaiTiep() {
            synchronized(hang) {
                if (dangGiai || daXong.get()) return
                val dv = hang.removeFirstOrNull() ?: return
                dangGiai = true
                @Suppress("DEPRECATION")
                nsd.resolveService(dv, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, loi: Int) {
                        synchronized(hang) { dangGiai = false }
                        giaiTiep()
                    }

                    override fun onServiceResolved(info: NsdServiceInfo) {
                        synchronized(hang) { dangGiai = false }
                        @Suppress("DEPRECATION")
                        val host = info.host
                        if (host != null && host in cuaMay) ket(info.port) else giaiTiep()
                    }
                })
            }
        }

        nghe = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(loai: String) = Unit
            override fun onDiscoveryStopped(loai: String) = Unit
            override fun onStartDiscoveryFailed(loai: String, loi: Int) = ket(null)
            override fun onStopDiscoveryFailed(loai: String, loi: Int) = Unit
            override fun onServiceLost(info: NsdServiceInfo) = Unit
            override fun onServiceFound(info: NsdServiceInfo) {
                synchronized(hang) { hang.addLast(info) }
                giaiTiep()
            }
        }
        val batDau = runCatching {
            nsd.discoverServices(DICH_VU_ADB, NsdManager.PROTOCOL_DNS_SD, nghe)
        }
        if (batDau.isFailure) {
            xong(null)
            return
        }
        tay.postDelayed({ ket(null) }, CHO_TIM_CONG_MS)
    }

    private fun diaChiCuaMay(): Set<InetAddress> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .toSet()
    }.getOrDefault(emptySet())
}
