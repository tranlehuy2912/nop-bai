package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.guard.CatMangVpn
import vn.huytl.homeworkgate.guard.ParentMode
import java.util.Calendar

/**
 * Tap app ma VPN phai cat ngay luc nay. Luat gio nam o LuatCatMangTest; o day kiem
 * phan ghep luat voi prefs that: danh sach, cong, che do Ba Huy, app nao con cai.
 *
 * CAN THAN: [setUp] xoa sach prefs nhu GateStoreTest. Chay tren may ao, dung chay
 * tren tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class CatMangVpnTest {

    private lateinit var ct: Context
    private lateinit var prefs: Prefs

    /** App co icon tren man hinh chinh, may ao nao cung co. */
    private val caiDat = "com.android.settings"

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
    }

    @After
    fun tearDown() {
        ParentMode.disable(ct)
        prefs.catMangPackages = emptySet()
    }

    @Test
    fun danh_sach_rong_thi_khong_cat() {
        assertEquals(emptySet<String>(), CatMangVpn.canCat(ct))
    }

    @Test
    fun het_gio_thi_cat_app_con_cai() {
        prefs.catMangPackages = setOf(caiDat, "vn.khong.co.that")
        assertEquals(setOf(caiDat), CatMangVpn.canCat(ct))
    }

    @Test
    fun khong_bao_gio_cat_chinh_app_nay() {
        prefs.catMangPackages = setOf(ct.packageName, caiDat)
        assertEquals(setOf(caiDat), CatMangVpn.canCat(ct))
    }

    @Test
    fun khong_cat_app_dung_moi_luc() {
        prefs.catMangPackages = setOf(caiDat)
        prefs.moiLucPackages = setOf(caiDat)
        assertEquals(emptySet<String>(), CatMangVpn.canCat(ct))
    }

    @Test
    fun dang_choi_thi_khong_cat() {
        prefs.catMangPackages = setOf(caiDat)
        val gate = GateStore(ct)
        assertNotNull(gate.approve())
        assertNotNull(gate.start())
        assertTrue(gate.isOpen())
        assertEquals(emptySet<String>(), CatMangVpn.canCat(ct))
    }

    @Test
    fun toi_gio_ngu_thi_cat_ca_khi_phien_con_chay() {
        prefs.catMangPackages = setOf(caiDat)
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
        assertEquals(setOf(caiDat), CatMangVpn.canCat(ct))
    }

    @Test
    fun ba_huy_mo_toan_bo_may_thi_khong_cat() {
        prefs.catMangPackages = setOf(caiDat)
        ParentMode.enable(ct)
        assertEquals(emptySet<String>(), CatMangVpn.canCat(ct))
    }
}
