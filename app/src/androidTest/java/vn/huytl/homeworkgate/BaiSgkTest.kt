package vn.huytl.homeworkgate

import android.content.ContentValues
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CongSang
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Bai tap SGK lam tren may ([GiaiDe.LOAI_SGK], Ba Huy chot 2/10/2026): con chon cau o "Làm bài tập
 * trong SGK", cau co ban phim ghep lam tren may, cau phai viet chup sau khi nop.
 *
 * Luc viet test SGK chua co cau nao soan ban phim ghep, nen phan lam tren may muon cau SBT (co
 * ghep) dat vao de mang nguon SGK; cach dung de khong xet cau thuoc quyen nao. Cau phai viet la
 * cau SGK that (chua co ghep). Tu 6/10/2026 moi cau SGK Toan tap mot deu co ghep (anh Huy chot
 * tranh viet tay nhieu nhat co the, cau ve thanh cau chon hinh), nen het cau phai viet that thi
 * test tu tam xoa ghep cua mot cau trong kho, tearDown tra lai (xem [cauPhaiViet]).
 *
 * Don dep nhu [GiaiDeTest]: xoa de da tao va dong so cai cua cac cau da ghi, khong xoa ca so cai.
 * Tra lai moc bai da hoc, quy gio; dong phieu gio va bo phut giu toi sang ([CongSang]) vi nop de
 * cong phut that.
 */
@RunWith(AndroidJUnit4::class)
class BaiSgkTest {

    private lateinit var context: Context
    private lateinit var kho: KhoBai
    private var mocCu: Map<String, Set<Int>?> = emptyMap()
    private var quyCu = 0
    private val deDaTao = mutableListOf<String>()
    private val cauDaGhi = mutableListOf<String>()
    /** Cau ma test tam xoa ghep de lam cau phai viet: id -> ghep cu, tearDown ghi lai. */
    private val ghepCu = mutableMapOf<String, String>()

    private val gio = 60 * 60_000L

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        kho = KhoBai.get(context)
        mocCu = MocThu.luu(context)
        quyCu = Prefs.get(context).quyGio
        CongSang.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        MocThu.datToan(context, 3)
    }

    @After
    fun tearDown() {
        deDaTao.forEach { kho.xoaDe(it) }
        ghepCu.forEach { (id, g) -> datGhep(id, g) }
        cauDaGhi.forEach { kho.writableDatabase.delete("tra_loi", "cau_id = ?", arrayOf(it)) }
        MocThu.tra(context, mocCu)
        Prefs.get(context).quyGio = quyCu
        CongSang.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
    }

    private val sgk: NganHang.Sach get() = NganHang.sachGiaoKhoaCua("Toán").first()

    /** Hai cau lam duoc tren may, chua lam lan nao (cau moi cua Luyen tap). */
    private fun haiCauTrenMay(): List<LamTrenMay.Muc> = LamTrenMay.cauLamThem(context, "Toán").take(2)

    /**
     * Mot cau SGK chua co ban phim ghep: cau phai viet. Con cau that thi dung cau that; het roi
     * (tu 6/10/2026 SGK Toan tap mot khong con cau nao) thi tam xoa ghep cua cau cuoi quyen, xa
     * cac bai dau ma [haiCauTrenMay] lay.
     */
    private fun cauPhaiViet(): CauHoi {
        val cac = kho.cacCauCuaNguon(sgk.nguon)
        cac.firstOrNull { LamTrenMay.muc(context, it) == null }?.let { return it }
        val c = cac.last()
        ghepCu[c.id] = c.ghep
        datGhep(c.id, "")
        return kho.cauTheoId(c.id) ?: error("khong doc lai duoc cau ${c.id}")
    }

    private fun datGhep(id: String, ghep: String) {
        kho.writableDatabase.update("cau_hoi", ContentValues().apply { put("ghep", ghep) }, "id = ?", arrayOf(id))
    }

    private fun tao(ids: List<String>) =
        GiaiDe.taoBaiSgk(context, sgk, listOf("Bài 1. Đơn thức"), ids)?.also { deDaTao += it.id }

    @Test
    fun luyen_tap_lay_ca_sgk_dung_truoc_sach_bai_tap() {
        val nguon = NganHang.sachLuyenTapCua("Toán").map { it.nguon }
        assertEquals(listOf("toan8t1", "toan8t2", "sbttoan8t1", "sbttoan8t2"), nguon)
        assertEquals(listOf("toan8t1", "toan8t2"), NganHang.sachGiaoKhoaCua("Toán").map { it.nguon })
    }

    @Test
    fun tieng_anh_co_sgk_dung_truoc_sach_bai_tap() {
        assertEquals(listOf("anh8"), NganHang.sachGiaoKhoaCua(PhanHoc.TIENG_ANH).map { it.nguon })
        assertEquals(listOf("anh8", "sbtanh8"), NganHang.sachLuyenTapCua(PhanHoc.TIENG_ANH).map { it.nguon })
    }

    @Test
    fun bai_sgk_gom_cau_lam_tren_may_va_cau_phai_viet() {
        val may = haiCauTrenMay()
        assertEquals("can hai cau lam tren may", 2, may.size)
        val viet = cauPhaiViet()

        val de = tao(may.map { it.cau.id } + viet.id)

        assertNotNull(de)
        de!!
        assertEquals(GiaiDe.LOAI_SGK, de.loai)
        assertEquals("Làm bài tập trong SGK · Toán", GiaiDe.tenDe(de))
        assertTrue("bat dau ngay, khong co the huong dan", de.daBatDau)
        assertEquals(0, de.phutGoiY)
        assertEquals("sao chi tinh cau lam tren may", may.sumOf { it.ghep.sao }, de.saoToiDa)
        assertEquals(listOf(viet.id), GiaiDe.cauPhaiViet(context, de).map { it.id })
        // Chi o "Làm bài tập trong SGK", khong len man chinh hay cac dong de cua trang Luyen tap.
        assertTrue(GiaiDe.baiSgkDangLam(context, "Toán").any { it.id == de.id })
        assertTrue(GiaiDe.dangMo(context).none { it.id == de.id })
        // Cau nam trong bai dang lam thi ra khoi Luyen tap.
        val trongLuyen = LamTrenMay.cauLamThem(context, "Toán").map { it.cau.id }
        assertTrue(may.none { it.cau.id in trongLuyen })
    }

    @Test
    fun chi_co_cau_phai_viet_thi_khong_tao_bai() {
        assertNull(tao(listOf(cauPhaiViet().id)))
    }

    @Test
    fun nop_cong_phut_theo_sao_het_han_ngay_va_con_cau_phai_viet_de_chup() {
        val may = haiCauTrenMay()
        val viet = cauPhaiViet()
        var de = tao(may.map { it.cau.id } + viet.id)!!
        cauDaGhi += may.map { it.cau.id }
        val dau = may[0]
        de = GiaiDe.luuLuot(context, de, dau.cau.id, LuatGhep.kiem(LuatGhep.Luot(dau.ghep.sao), 0), "x")

        val kq = GiaiDe.nop(context, de)

        assertTrue(kq.de.daNop)
        assertEquals("cau thu hai chua lam tinh 0 sao", dau.ghep.sao, kq.saoDat)
        assertEquals(dau.ghep.sao, kq.phutCap + kq.phutQuy)
        assertEquals("bai SGK het han luc nop", kq.de.nopLuc, kq.de.hetHan)
        assertTrue(GiaiDe.baiSgkDangLam(context, "Toán").none { it.id == de.id })
        assertEquals(listOf(viet.id), GiaiDe.cauPhaiVietChuaGui(context, kq.de).map { it.id })
        val nhatKy = DayLog.today(context)
        assertTrue(nhatKy, nhatKy.contains("Làm bài tập trong SGK · Toán ("))
        assertTrue(nhatKy, nhatKy.contains("còn 1 câu phải viết chụp ảnh"))
        // Cau chua lam luc nop: 24 gio sau ra o Luyen tap de lam lai, nhu cau cua de khac.
        val sau = LamTrenMay.cauLamThem(context, "Toán", bayGio = System.currentTimeMillis() + 25 * gio)
        assertTrue(sau.any { it.cau.id == may[1].cau.id })
    }
}
