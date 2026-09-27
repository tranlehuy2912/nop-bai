package vn.huytl.homeworkgate

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.guard.CatMangVpn
import vn.huytl.homeworkgate.guard.ParentMode
import java.util.Calendar

/**
 * Tap app ma VPN phai cat ngay luc nay. Luat gio va viec bat tat nam o LuatCatMangTest;
 * o day kiem phan ghep luat voi prefs that: danh sach, cong, che do Ba Huy, app nao con
 * cai va chay UID nao.
 *
 * Dung mot app thuong co tren may lam app thu, khong dung Cai dat: lang nghe prefs that
 * chay chung tien trinh voi test, may ao nao da cho phep VPN thi test bat VPN that trong
 * chot lat, ma Cai dat chung UID 1000 voi ca dong tien trinh he thong.
 *
 * CAN THAN: [setUp] xoa sach prefs nhu GateStoreTest. Chay tren may ao, dung chay
 * tren tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class CatMangVpnTest {

    private lateinit var ct: Context
    private lateinit var prefs: Prefs

    /** Mot app co icon tren man hinh chinh, chay UID rieng, khong phai app nay. */
    private lateinit var appThu: String

    @Before
    fun setUp() {
        ct = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = Prefs.get(ct)
        prefs.raw().edit().clear().commit()
        ParentMode.disable(ct)
        // Gio ngu dat xa gio chay test, va tat man chan gio hoc: ca hai deu theo dong ho
        // that, de nguyen thi test chay luc 22:00 hay sang thu hai ra ket qua khac.
        val bayGio = Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
        prefs.hardStopMinuteOfDay = (bayGio + 120) % (24 * 60)
        prefs.gioDayMinuteOfDay = (bayGio + 180) % (24 * 60)
        prefs.batManChan = false
        prefs.grantMinutes = 60
        prefs.tranPhutMoiNgay = 480

        val thuong = appThuongTrenMay()
        assumeTrue("may nay khong co app thuong nao de thu", thuong != null)
        appThu = thuong!!
    }

    @After
    fun tearDown() {
        ParentMode.disable(ct)
        prefs.catMangPackages = emptySet()
    }

    private fun appThuongTrenMay(): String? {
        val pm = ct.packageManager
        val y = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(y, 0)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != ct.packageName && it.uid >= Process.FIRST_APPLICATION_UID }
            .map { it.packageName }
            .sorted()
            .firstOrNull()
    }

    @Test
    fun danh_sach_rong_thi_khong_cat() {
        assertEquals(emptySet<String>(), CatMangVpn.canCat(ct))
    }

    @Test
    fun het_gio_thi_cat_app_con_cai() {
        prefs.catMangPackages = setOf(appThu, "vn.khong.co.that")
        assertEquals(setOf(appThu), CatMangVpn.canCat(ct))
    }

    @Test
    fun khong_bao_gio_cat_chinh_app_nay() {
        prefs.catMangPackages = setOf(ct.packageName, appThu)
        assertEquals(setOf(appThu), CatMangVpn.canCat(ct))
    }

    @Test
    fun khong_cat_app_dung_moi_luc() {
        prefs.catMangPackages = setOf(appThu)
        prefs.moiLucPackages = setOf(appThu)
        assertEquals(emptySet<String>(), CatMangVpn.canCat(ct))
    }

    /** Cai dat chay UID 1000 tren AOSP va HyperOS; cat no la cat ca he thong. */
    @Test
    fun khong_cat_app_chay_uid_he_thong() {
        val caiDat = "com.android.settings"
        val uid = runCatching { ct.packageManager.getApplicationInfo(caiDat, 0).uid }.getOrNull()
        assumeTrue("may nay Cai dat khong chay UID he thong", uid != null && uid < Process.FIRST_APPLICATION_UID)
        assertFalse(CatMangVpn.laAppThuong(ct.packageManager, caiDat))
        assertTrue(CatMangVpn.laAppThuong(ct.packageManager, appThu))

        prefs.catMangPackages = setOf(caiDat, appThu)
        assertEquals(setOf(appThu), CatMangVpn.canCat(ct))
    }

    @Test
    fun dang_choi_thi_khong_cat() {
        prefs.catMangPackages = setOf(appThu)
        val gate = GateStore(ct)
        assertNotNull(gate.approve())
        assertNotNull(gate.start())
        assertTrue(gate.isOpen())
        assertEquals(emptySet<String>(), CatMangVpn.canCat(ct))
    }

    @Test
    fun toi_gio_ngu_thi_cat_ca_khi_phien_con_chay() {
        prefs.catMangPackages = setOf(appThu)
        val gate = GateStore(ct)
        gate.approve()
        gate.start()
        // Doi gio ngu phu lay luc nay. Phien van con, vi moc dung da tinh luc bam Bat dau.
        val bayGio = Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
        prefs.hardStopMinuteOfDay = (bayGio + 24 * 60 - 1) % (24 * 60)
        prefs.gioDayMinuteOfDay = (bayGio + 60) % (24 * 60)
        assertTrue(gate.isOpen())
        assertEquals(setOf(appThu), CatMangVpn.canCat(ct))
    }

    @Test
    fun ba_huy_mo_toan_bo_may_thi_khong_cat() {
        prefs.catMangPackages = setOf(appThu)
        ParentMode.enable(ct)
        assertEquals(emptySet<String>(), CatMangVpn.canCat(ct))
    }
}
