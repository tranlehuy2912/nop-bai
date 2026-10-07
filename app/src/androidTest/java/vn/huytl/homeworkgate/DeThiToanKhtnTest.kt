package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LichKiemTra
import vn.huytl.homeworkgate.kho.DeThi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Bo de thi Toan, KHTN that trong assets (buoc 4.4, 1/10/2026): du ma de, pham vi tung phan doc
 * ra duoc tu bai_sgk, dong ho 90 va 60 phut, moi cau lam tren may co trong kho, moi anh co that.
 * Chi doc, khong ghi gi vao may. Luat mo de thu o [DeThiPhanTest].
 */
@RunWith(AndroidJUnit4::class)
class DeThiToanKhtnTest {

    private lateinit var context: Context
    private lateinit var kho: KhoBai

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        kho = KhoBai.get(context)
    }

    private fun cua(mon: String): List<DeThi.De> = DeThi.tatCa(context).filter { it.mon == mon }

    /**
     * Giua ki 1 (buoc 4.4, 1/10/2026) roi cuoi ki 1 (buoc 4.5, 2/10/2026), du ca hai luot. Hoc ki 2
     * (buoc 4.9, tu 7/10/2026) gop dan tung lo, xep sau hoc ki 1: danh sach lon dan theo tung lo.
     */
    @Test
    fun du_de_hoc_ki_1_hai_mon_theo_thu_tu() {
        assertEquals((1..10).map { "KGK1-$it" } + (1..10).map { "KCK1-$it" }, cua(LichKiemTra.KHTN).map { it.ma })
        assertEquals(
            (1..8).map { "TGK1-$it" } + (1..8).map { "TCK1-$it" } + (1..4).map { "TGK2-$it" },
            cua(LichKiemTra.TOAN).map { it.ma }
        )
        assertTrue(NganHang.boDeThi().map { it.nguon }.containsAll(listOf("dethitoan8", "dethikhtn8")))
        assertTrue("de thi khong hien o man chon sach", NganHang.sachCua(LichKiemTra.TOAN).none { it.deThi })
    }

    @Test
    fun moi_de_co_pham_vi_dong_ho_va_cau_lam_duoc_tren_may() {
        listOf(LichKiemTra.TOAN, LichKiemTra.KHTN).forEach { mon ->
            cua(mon).forEach { de ->
                assertTrue("${de.ma}: co pham vi", de.denBai.isNotEmpty())
                de.denBai.forEach { (ma, bai) ->
                    val phan = PhanHoc.theoMa(ma)
                    assertEquals("${de.ma}: phan $ma thuoc mon", mon, phan?.mon)
                    assertTrue("${de.ma}: Bai $bai thuoc phan $ma", phan!!.chua(bai))
                }
                assertEquals("${de.ma}: de khong in gio", DeThi.phutMacDinh(mon), de.phut)
                assertTrue("${de.ma}: it nhat 3 cau lam tren may", de.cauIds.size >= 3)
                assertTrue("${de.ma}: moi cau co nhan", de.cacMuc.all { it.nhan.isNotBlank() })
                val trongKho = kho.cacCauTheoId(de.cauIds).associateBy { it.id }
                val thieu = de.cauIds.filter { it !in trongKho }
                assertTrue("${de.ma} thieu cau trong kho: $thieu", thieu.isEmpty())
                assertTrue("${de.ma}: cau lam duoc tren may", trongKho.values.all { it.lamTrenMay })
            }
        }
    }

    /** Loigiaihay soan de giua ki 1 KHTN theo thu tu sach: chi co cau Hoa (kiem 1/10/2026). */
    @Test
    fun de_giua_ki_1_khtn_chi_xet_phan_hoa() {
        cua(LichKiemTra.KHTN).filter { it.ma.startsWith("KGK1-") }.forEach {
            assertEquals(it.ma, setOf("khtn8hoa"), it.denBai.keys)
        }
    }

    /**
     * De cuoi ki 1 KHTN co ca phan Hoa lan phan Li (Bai 13-19); KCK1-10 co them mot cau Sinh (Bai
     * 37) nen chi doi co du Hoa va Li. De cuoi ki 1 Toan co hai phan.
     */
    @Test
    fun de_cuoi_ki_1_co_du_cac_phan_cua_hoc_ki_1() {
        cua(LichKiemTra.KHTN).filter { it.ma.startsWith("KCK1-") }.forEach {
            assertTrue(it.ma, it.denBai.keys.containsAll(setOf("khtn8hoa", "khtn8li")))
        }
        cua(LichKiemTra.TOAN).filter { it.ma.startsWith("TCK1-") }.forEach {
            assertEquals(it.ma, setOf("toan8ds", "toan8hh"), it.denBai.keys)
        }
    }

    @Test
    fun moi_anh_cua_de_co_that_trong_assets() {
        fun co(duong: String) = runCatching { context.assets.open("hinh/$duong").use { it.read() >= 0 } }.getOrDefault(false)
        DeThi.tatCa(context).filter { !it.laTiengAnh }.forEach { de ->
            de.cacMuc.forEach { m ->
                (m.hinhGoiY + m.hinhDayDu).forEach { assertTrue("${de.ma} ${m.nhan}: thieu anh $it", co(it)) }
                assertTrue("${de.ma} ${m.nhan}: co hinh day du thi phai co hinh goi y", m.hinhDayDu.isEmpty() || m.hinhGoiY.isNotEmpty())
            }
            kho.cacCauTheoId(de.cauIds).forEach { c ->
                c.hinh.forEach { assertTrue("${c.id}: thieu anh $it", co(it)) }
            }
        }
    }
}
