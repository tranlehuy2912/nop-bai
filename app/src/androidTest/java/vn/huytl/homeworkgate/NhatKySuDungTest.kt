package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.Prefs
import java.util.Calendar

/**
 * So ghi con dung app nao, tu may gio den may gio.
 *
 * CAN THAN: giong [GateStoreTest], bo test nay xoa sach prefs. Dung chay tren tablet
 * cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class NhatKySuDungTest {

    private lateinit var context: Context
    private val youtube = "com.google.android.youtube"
    private val netflix = "com.netflix.mediaclient"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(context).raw().edit().clear().commit()
    }

    /** Moc gio trong ngay, [lui] ngay truoc hom nay. */
    private fun luc(gio: Int, phut: Int = 0, lui: Int = 0): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -lui)
            set(Calendar.HOUR_OF_DAY, gio)
            set(Calendar.MINUTE, phut)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun chua_ghi_gi_thi_ngay_trong() {
        assertTrue(NhatKySuDung.theoApp(context).isEmpty())
        assertEquals(0L, NhatKySuDung.tongMs(context))
        assertEquals(null, NhatKySuDung.tuDenTrongNgay(context))
    }

    @Test
    fun mot_khoang_ghi_xong_doc_lai_dung_gio() {
        NhatKySuDung.ghi(context, youtube, luc(14, 3), luc(14, 25))

        val cac = NhatKySuDung.theoApp(context)
        assertEquals(1, cac.size)
        assertEquals(youtube, cac[0].goi)
        assertEquals(22 * 60_000L, cac[0].tongMs)
        assertEquals(1, cac[0].cacDoan.size)
        assertEquals(luc(14, 3) to luc(14, 25), cac[0].cacDoan[0].tu to cac[0].cacDoan[0].den)
    }

    /**
     * Dich vu canh app ghi lai ca khoang dang mo sau moi nhip, nen cung mot khoang
     * duoc ghi nhieu lan. Ket qua phai la MOT dong dai ra, khong phai nhieu dong.
     */
    @Test
    fun ghi_de_khoang_dang_mo_thi_chi_noi_dai_them() {
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 2))
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 4))
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 30))

        val cac = NhatKySuDung.theoApp(context)
        assertEquals(1, cac.size)
        assertEquals(1, cac[0].cacDoan.size)
        assertEquals(30 * 60_000L, cac[0].tongMs)
    }

    /** Doi ngang sang man hinh chinh vai giay roi quay lai van la mot lan choi. */
    @Test
    fun roi_ra_vai_giay_roi_quay_lai_van_la_mot_khoang() {
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 10))
        NhatKySuDung.ghi(context, youtube, luc(14, 10) + 20_000L, luc(14, 20))

        val cac = NhatKySuDung.theoApp(context)
        assertEquals(1, cac[0].cacDoan.size)
        assertEquals(luc(14, 0), cac[0].cacDoan[0].tu)
        assertEquals(luc(14, 20), cac[0].cacDoan[0].den)
    }

    /** Hai lan choi cach nhau ca tieng thi phai la hai dong. */
    @Test
    fun hai_lan_cach_xa_nhau_thi_la_hai_khoang() {
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 20))
        NhatKySuDung.ghi(context, youtube, luc(19, 0), luc(19, 40))

        val cac = NhatKySuDung.theoApp(context)
        assertEquals(1, cac.size)
        assertEquals(2, cac[0].cacDoan.size)
        assertEquals(60 * 60_000L, cac[0].tongMs)
        assertEquals("14:00–14:20", NhatKySuDung.moTaDoan(cac[0].cacDoan[0]))
    }

    @Test
    fun app_dung_lau_nhat_dung_truoc() {
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 20))
        NhatKySuDung.ghi(context, netflix, luc(15, 0), luc(16, 0))

        val cac = NhatKySuDung.theoApp(context)
        assertEquals(listOf(netflix, youtube), cac.map { it.goi })
    }

    /**
     * Chia doi man hinh thi hai app cung nam truoc mat. Tong cua ngay phai dem mot
     * lan, khong thi nua tieng dung may ra thanh mot tieng.
     */
    @Test
    fun hai_app_chong_gio_nhau_chi_tinh_mot_lan_vao_tong() {
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 30))
        NhatKySuDung.ghi(context, netflix, luc(14, 10), luc(14, 40))

        assertEquals(30 * 60_000L, NhatKySuDung.theoApp(context)[0].tongMs)
        assertEquals(40 * 60_000L, NhatKySuDung.tongMs(context))
    }

    @Test
    fun ngay_hom_qua_khong_lan_sang_hom_nay() {
        NhatKySuDung.ghi(context, youtube, luc(20, 0, lui = 1), luc(20, 30, lui = 1))
        NhatKySuDung.ghi(context, netflix, luc(9, 0), luc(9, 15))

        assertEquals(listOf(netflix), NhatKySuDung.theoApp(context, 0).map { it.goi })
        assertEquals(listOf(youtube), NhatKySuDung.theoApp(context, 1).map { it.goi })
        assertEquals(15 * 60_000L, NhatKySuDung.tongMs(context, 0))
        assertEquals(30 * 60_000L, NhatKySuDung.tongMs(context, 1))
    }

    /** Khoang vat qua nua dem bi cat doi, moi ngay nhan phan cua minh. */
    @Test
    fun khoang_vat_qua_nua_dem_bi_cat_o_moc_giao_ngay() {
        NhatKySuDung.ghi(context, youtube, luc(23, 40, lui = 1), luc(0, 20))

        assertEquals(20 * 60_000L, NhatKySuDung.tongMs(context, 0))
        assertEquals(20 * 60_000L, NhatKySuDung.tongMs(context, 1))
    }

    /** Dong ho bi day toi, hoac tien trinh dong bang nua ngay: khong tin, bo luon. */
    @Test
    fun khoang_dai_vo_ly_thi_khong_ghi() {
        NhatKySuDung.ghi(context, youtube, luc(2, 0), luc(20, 0))
        assertTrue(NhatKySuDung.theoApp(context).isEmpty())
    }

    @Test
    fun khoang_am_hoac_rong_thi_khong_ghi() {
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 0))
        NhatKySuDung.ghi(context, youtube, luc(14, 30), luc(14, 0))
        assertTrue(NhatKySuDung.theoApp(context).isEmpty())
    }

    @Test
    fun tu_den_trong_ngay_lay_moc_dau_va_moc_cuoi() {
        NhatKySuDung.ghi(context, youtube, luc(19, 0), luc(19, 40))
        NhatKySuDung.ghi(context, netflix, luc(7, 10), luc(7, 20))

        assertEquals(luc(7, 10) to luc(19, 40), NhatKySuDung.tuDenTrongNgay(context))
    }

    @Test
    fun do_dai_doc_ra_tieng_viet() {
        assertEquals("dưới 1 phút", NhatKySuDung.moTaDoDai(30_000L))
        assertEquals("45 phút", NhatKySuDung.moTaDoDai(45 * 60_000L))
        assertEquals("1 tiếng", NhatKySuDung.moTaDoDai(60 * 60_000L))
        assertEquals("2 tiếng 15 phút", NhatKySuDung.moTaDoDai(135 * 60_000L))
    }

    @Test
    fun xoa_het_thi_so_trong_lai() {
        NhatKySuDung.ghi(context, youtube, luc(14, 0), luc(14, 20))
        NhatKySuDung.xoaHet(context)
        assertTrue(NhatKySuDung.theoApp(context).isEmpty())
    }
}
