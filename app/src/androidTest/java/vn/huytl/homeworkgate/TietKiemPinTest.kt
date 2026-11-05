package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.CaiDatPin
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh
import vn.huytl.homeworkgate.pin.Jokers
import vn.huytl.homeworkgate.pin.TietKiemPin

/**
 * Tiet kiem pin khi man hinh tat (5/11/2026): luat luc nao tiet kiem, app nao khong bao gio tam
 * dung, lenh CAIDAT tu Bang dieu khien va ban sao hop/caidat.
 *
 * Viec that (bat Tiet kiem pin, pm suspend qua Jokers) da thu tay tren pad5 ngay 5/11/2026, khong
 * nam o day. Giu cau hinh pin tren may roi tra ve nguyen ven sau khi chay, nhu [CaiDatLenhTest].
 */
@RunWith(AndroidJUnit4::class)
class TietKiemPinTest {

    private lateinit var context: Context
    private var batCu = false
    private var phutCu = 0
    private var phutDungCu = 0
    private var appsCu: Set<String> = emptySet()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = Prefs.get(context)
        batCu = prefs.pinBat
        phutCu = prefs.pinPhut
        phutDungCu = prefs.pinPhutDung
        appsCu = prefs.tamDungPackages
    }

    @After
    fun tearDown() {
        val prefs = Prefs.get(context)
        prefs.pinBat = batCu
        prefs.pinPhut = phutCu
        prefs.pinPhutDung = phutDungCu
        prefs.tamDungPackages = appsCu
        TietKiemPin.apDung(context)
    }

    @Test
    fun man_hinh_sang_hay_chua_bat_thi_khong_tiet_kiem() {
        assertFalse(TietKiemPin.nenTietKiem(bat = true, phut = 0, daTatMs = null))
        assertFalse(TietKiemPin.nenTietKiem(bat = false, phut = 0, daTatMs = 3_600_000L))
    }

    @Test
    fun tat_man_hinh_du_so_phut_moi_tiet_kiem() {
        assertTrue(TietKiemPin.nenTietKiem(bat = true, phut = 0, daTatMs = 0L))
        assertFalse(TietKiemPin.nenTietKiem(bat = true, phut = 5, daTatMs = 4 * 60_000L + 59_000L))
        assertTrue(TietKiemPin.nenTietKiem(bat = true, phut = 5, daTatMs = 5 * 60_000L))
    }

    @Test
    fun can_tam_dung_bo_app_da_dung_va_app_cam() {
        val can = TietKiemPin.canTamDung(
            apps = setOf("b.youtube", "a.chrome", "c.game", TietKiemPin.TAILSCALE),
            daDung = setOf("c.game"),
            khongDung = setOf(TietKiemPin.TAILSCALE)
        )
        assertEquals(listOf("a.chrome", "b.youtube"), can)
    }

    /** Tam dung man hinh chinh hay ban phim ma Jokers chet truoc khi mo lai la tablet thanh cuc gach. */
    @Test
    fun khong_bao_gio_tam_dung_app_nay_jokers_tailscale_man_hinh_chinh_ban_phim() {
        val cam = TietKiemPin.khongDuocDung(context)
        assertTrue(context.packageName in cam)
        assertTrue(Jokers.GOI in cam)
        assertTrue(TietKiemPin.TAILSCALE in cam)
        assertTrue("com.android.settings" in cam)
        assertTrue("com.android.systemui" in cam)
        val manHinhChinh = context.packageManager.resolveActivity(
            android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_HOME),
            0
        )?.activityInfo?.packageName
        if (manHinhChinh != null) assertTrue(manHinhChinh in cam)
    }

    @Test
    fun lenh_cai_dat_doi_bon_muc() {
        assertEquals("Đã bật tự động tiết kiệm pin.", ThiHanhLenh.doiCaiDat(context, CaiDatPin.BAT, true))
        // Firestore tra so ve dang Long.
        assertEquals("Tắt màn hình là bật Tiết kiệm pin ngay.", ThiHanhLenh.doiCaiDat(context, CaiDatPin.PHUT, 0L))
        assertEquals("Tắt màn hình 2 phút thì tạm dừng app.", ThiHanhLenh.doiCaiDat(context, CaiDatPin.PHUT_DUNG, 2L))
        assertEquals(
            "Danh sách app tạm dừng khi tắt màn hình: 2 app.",
            ThiHanhLenh.doiCaiDat(context, CaiDatPin.APPS, listOf(YOUTUBE, CHROME))
        )
        val prefs = Prefs.get(context)
        assertTrue(prefs.pinBat)
        assertEquals(0, prefs.pinPhut)
        assertEquals(2, prefs.pinPhutDung)
        assertEquals(setOf(YOUTUBE, CHROME), prefs.tamDungPackages)
    }

    /** Hai hang nut cua Bang dieu khien chi co cac muc nay; so la thi khong doi gi. */
    @Test
    fun so_phut_ngoai_danh_sach_thi_tu_choi() {
        Prefs.get(context).pinPhutDung = 5
        assertEquals("Số phút không có trong danh sách.", ThiHanhLenh.doiCaiDat(context, CaiDatPin.PHUT_DUNG, 0L))
        assertEquals("Số phút không có trong danh sách.", ThiHanhLenh.doiCaiDat(context, CaiDatPin.PHUT, 7L))
        assertEquals(5, Prefs.get(context).pinPhutDung)
    }

    /** Bang dieu khien doc cac truong nay de ve nhom "Tiết kiệm pin" va dong bao loi. */
    @Test
    fun ban_sao_cau_hinh_co_cac_truong_pin() {
        val prefs = Prefs.get(context)
        prefs.pinBat = true
        prefs.pinPhut = 10
        prefs.pinPhutDung = 5
        prefs.tamDungPackages = setOf(YOUTUBE, CHROME)

        val ban = DongBo.banCaiDat(context)

        assertEquals(true, ban[CaiDatPin.BAT])
        assertEquals(10, ban[CaiDatPin.PHUT])
        assertEquals(5, ban[CaiDatPin.PHUT_DUNG])
        assertEquals(listOf(CHROME, YOUTUBE), ban[CaiDatPin.APPS])
        assertTrue(ban[CaiDatPin.JOKERS] in setOf(
            CaiDatPin.JOKERS_CHUA_CAI, CaiDatPin.JOKERS_CHUA_CHAY,
            CaiDatPin.JOKERS_CHUA_CHO_PHEP, CaiDatPin.JOKERS_SAN_SANG
        ))
        assertTrue(ban[CaiDatPin.CO_QUYEN] is Boolean)
    }

    private companion object {
        const val YOUTUBE = "com.google.android.youtube"
        const val CHROME = "com.android.chrome"
    }
}
