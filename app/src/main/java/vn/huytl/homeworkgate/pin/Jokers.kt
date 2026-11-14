package vn.huytl.homeworkgate.pin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import vn.huytl.homeworkgate.dongbo.CaiDatPin
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "Jokers"

/**
 * Noi voi Jokers Permissions (ban build rieng cua Shizuku o ~/Documents/jokers-permissions, anh Huy
 * chot 03/11/2026 cho dien thoai, 05/11/2026 cho tablet) de chay lenh shell voi quyen ngang may tinh
 * noi qua go loi: tam dung app khac, tat tien trinh app khac. Thu vien noi o module jokers-api.
 *
 * Chep tu pin/Jokers.kt cua app Lich ngay 05/11/2026, bo phan trang thai Compose (app nay dung
 * View) va doi [batDau] thanh goi lai moi lan tinh trang doi, de tablet ghi lai hop/caidat cho Bang
 * dieu khien biet Jokers vua chet hay vua chay lai.
 *
 * App Nop bai phai duoc cho phep dung Jokers. Hai cach: bam "Cho phép" trong hop hoi cua Jokers
 * ([xinPhep], nut tren man Tiet kiem pin cua tablet), hoac cap qua adb, khong can ai bam tren
 * tablet (man hinh khoa co mat khau thi Claude khong bam ho duoc):
 *   adb shell pm grant vn.huytl.homeworkgate vn.huytl.jokers.permission.API_V23
 * Quyen API_V23 la quyen "dangerous" do Jokers khai, va Jokers coi app da duoc cap quyen nay la
 * da cho phep (ShizukuService.getFlagsForUid). Vi quyen do Jokers khai nen phai cai Jokers truoc.
 *
 * Jokers khong chay (vua khoi dong may luc khong co Wi-Fi, hay tat ca go loi USB lan go loi khong
 * day: Android dung adbd va giet ca nhom tien trinh cua no, gom Jokers) hay chua cho phep thi
 * [chay] tra false, viec bo qua.
 */
object Jokers {
    const val GOI = "vn.huytl.jokers"
    const val QUYEN = "vn.huytl.jokers.permission.API_V23"
    private const val MA_XIN = 7

    private val luong = Executors.newSingleThreadExecutor()
    private val khiDoi = CopyOnWriteArrayList<() -> Unit>()

    @Volatile
    private var daNghe = false

    /**
     * Goi mot lan luc app chay. [khiDoiTinhTrang] chay tren luong chinh moi lan Jokers vua chay,
     * vua chet, hay vua tra loi hop hoi. Listener "sticky" goi ngay neu Jokers dang chay san.
     */
    fun batDau(khiDoiTinhTrang: () -> Unit) {
        khiDoi += khiDoiTinhTrang
        if (daNghe) return
        daNghe = true
        runCatching {
            Shizuku.addBinderReceivedListenerSticky { doi() }
            Shizuku.addBinderDeadListener { doi() }
            Shizuku.addRequestPermissionResultListener { _, _ -> doi() }
        }.onFailure { Log.w(TAG, "Khong nghe duoc Jokers", it) }
    }

    /** Man hinh dang mo nghe them de ve lai tinh trang; nho [boNghe] khi dong man. */
    fun nghe(f: () -> Unit) {
        khiDoi += f
    }

    fun boNghe(f: () -> Unit) {
        khiDoi -= f
    }

    private fun doi() {
        Log.i(TAG, "tinh trang doi: con chay=${conChay()}, duoc phep=${duocPhep()}")
        khiDoi.forEach { runCatching { it() } }
    }

    /** Mot trong cac chuoi CaiDatPin.JOKERS_..., ghi vao hop/caidat cho Bang dieu khien. */
    fun tinhTrang(context: Context): String = when {
        !daCai(context) -> CaiDatPin.JOKERS_CHUA_CAI
        !conChay() -> CaiDatPin.JOKERS_CHUA_CHAY
        !duocPhep() -> CaiDatPin.JOKERS_CHUA_CHO_PHEP
        else -> CaiDatPin.JOKERS_SAN_SANG
    }

    fun sanSang(): Boolean = conChay() && duocPhep()

    /** Hien hop hoi cua Jokers Permissions tren tablet: cho app Nop bai dung khong. */
    fun xinPhep() {
        try {
            Shizuku.requestPermission(MA_XIN)
        } catch (e: Exception) {
            Log.w(TAG, "Khong xin duoc quyen", e)
        }
    }

    /** Mo app Jokers Permissions de bam "Bat dau" khi no chua chay. */
    fun moApp(context: Context) {
        context.packageManager.getLaunchIntentForPackage(GOI)?.let {
            context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /**
     * Chay lenh shell (sh -c), cho xong toi da 10 giay. Tra ve chu lenh in ra, null neu Jokers chua
     * san sang hay lenh loi. Chan luong dang goi: goi tu [lam], dung goi tu luong giao dien.
     */
    fun chayLay(lenh: String): String? = chayLayThang("sh", "-c", lenh)

    /**
     * Nhu [chayLay] nhung chay thang chuong trinh [lenh] voi cac doi so, khong qua sh. Dung khi doi
     * so la chu nguoi doc (cau bao co dau cach, dau cham, dau nhay cua `pm suspend --dialogMessage`,
     * xem [vn.huytl.homeworkgate.guard.KhoaJokers]): qua sh thi phai tu boc dau nhay, sot mot dau
     * la lenh hong.
     */
    fun chayLayThang(vararg lenh: String): String? {
        if (!sanSang()) return null
        return try {
            val p = Shizuku.newProcess(arrayOf(*lenh), null, null)
            val ra = p.inputStream.bufferedReader().readText()
            // Khong dung waitFor(10, SECONDS) cua Process: no hoi exitValue(), lenh chua xong thi
            // Jokers nem IllegalArgumentException qua binder, nen lenh da chay van bi coi la loi (app
            // Lich gap 03/11/2026 khi force-stop Google Maps). waitForTimeout cho ngay ben Jokers.
            if (!p.waitForTimeout(10, TimeUnit.SECONDS)) {
                p.destroy()
                return null
            }
            if (p.exitValue() == 0) ra else null
        } catch (e: Exception) {
            Log.w(TAG, "Lenh loi: ${lenh.joinToString(" ")}", e)
            null
        }
    }

    /** Nhu [chayLay], chi can biet lenh chay duoc khong (ma thoat 0). */
    fun chay(lenh: String): Boolean = chayLay(lenh) != null

    /** Lam viec tren luong nen rieng cua Jokers, lan luot dung thu tu goi. */
    fun lam(viec: () -> Unit) {
        luong.execute {
            try {
                viec()
            } catch (e: Throwable) {
                Log.w(TAG, "Viec loi", e)
            }
        }
    }

    fun daCai(context: Context): Boolean = try {
        context.packageManager.getApplicationInfo(GOI, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun conChay(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    private fun duocPhep(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }
}
