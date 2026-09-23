package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.ChamTheoClaude
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.KhaiChoCham
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import java.time.LocalDateTime

/**
 * Ket qua Claude cham, do Ba Huy dan ve, thanh ban cham cua tablet.
 *
 * Cau trong sach phai lay de, ma sach va dang bai tu ngan hang chu khong tu Claude:
 * dang bai quyet gia moi cau, va ma sach la khoa chan viec tra gio hai lan.
 */
@RunWith(AndroidJUnit4::class)
class ChamTheoClaudeTest {

    private lateinit var context: Context

    private val pham = PhamVi(
        mon = "Toán",
        nguon = "toan8t1",
        tenNguon = "SGK Toán 8 — tập một",
        bai = "trang 47",
        cauIds = listOf("toan8t1:2.28", "toan8t1:2.33a")
    )

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
    }

    @Test
    fun cau_trong_sach_lay_de_ma_va_dang_tu_ngan_hang() {
        val giaTri = listOf(
            mapOf("ma" to "2.28", "dung" to true, "chac" to true, "conViet" to "B", "soDong" to 1),
            mapOf(
                "ma" to "2.33a", "dung" to false, "chac" to true, "conViet" to "8x^2",
                "goiY" to "Xem lại phép khai triển", "soDong" to 3
            )
        )
        val ket = ChamTheoClaude.banCham(context, giaTri, pham)!!
        assertEquals("Toán", ket.mon)

        val tn = ket.cac.single { it.ma == "2.28" }
        assertEquals("toan8t1:2.28", tn.cauId)
        assertEquals(DangBai.TRAC_NGHIEM, tn.dang)
        assertTrue(tn.de.startsWith("Đa thức x^2 − 9x + 8"))
        assertTrue(tn.dung)
        assertEquals("", tn.nhanXet)

        val nho = ket.cac.single { it.ma == "2.33a" }
        assertEquals("toan8t1:2.33a", nho.cauId)
        assertEquals(DangBai.CAU_NHO, nho.dang)
        assertFalse(nho.dung)
        assertEquals("Xem lại phép khai triển", nho.nhanXet)
        assertEquals(3, nho.soDong)
        assertTrue(nho.coDe)
    }

    @Test
    fun cau_dung_duoc_tinh_phut_nhu_luc_may_tu_cham() {
        val ket = ChamTheoClaude.banCham(
            context,
            listOf(mapOf("ma" to "2.33a", "dung" to true, "soDong" to 4)),
            pham
        )!!
        val bang = LuatCongGio.tinh(ket)
        // Bon dong lam bai cua mot cau nho: hai dong mot phut, it nhat hai phut.
        assertEquals(2, bang.phut)
    }

    @Test
    fun cau_ngoai_sach_lay_de_claude_chep_con_khong_co_de_thi_khong_tra_gio() {
        val ket = ChamTheoClaude.banCham(
            context,
            listOf(
                mapOf("ma" to "1", "dung" to true, "de" to "Tính 2 + 3.", "soDong" to 1),
                mapOf("ma" to "2", "dung" to true)
            ),
            null
        )!!
        val coDe = ket.cac.single { it.ma == "1" }
        assertNull(coDe.cauId)
        assertEquals("Tính 2 + 3.", coDe.de)
        assertTrue(coDe.coDe)
        assertFalse(ket.cac.single { it.ma == "2" }.coDe)
    }

    @Test
    fun cau_ngoai_sach_lay_dang_claude_xep_con_cau_trong_sach_giu_dang_ngan_hang() {
        val ket = ChamTheoClaude.banCham(
            context,
            listOf(
                // Dang bai quyet gia, nen cau trong sach khong de Claude doi dang.
                mapOf("ma" to "2.28", "dung" to true, "dang" to "VIET_DAI"),
                mapOf("ma" to "5", "dung" to true, "de" to "Học thuộc bảng tuần hoàn.", "dang" to "KHONG_TINH"),
                mapOf("ma" to "6", "dung" to true, "de" to "Tính 1 + 1.", "dang" to "linh tinh")
            ),
            pham
        )!!
        assertEquals(DangBai.TRAC_NGHIEM, ket.cac.single { it.ma == "2.28" }.dang)
        assertEquals(DangBai.KHONG_TINH, ket.cac.single { it.ma == "5" }.dang)
        assertEquals(DangBai.CAU_NHO, ket.cac.single { it.ma == "6" }.dang)
        assertEquals(0, LuatCongGio.phutChoCau(ket.cac.single { it.ma == "5" }))
    }

    @Test
    fun muc_khong_co_ket_luan_that_thi_bo_va_khong_chac_thi_doc_khong_ro() {
        assertNull(ChamTheoClaude.banCham(context, null, pham))
        assertNull(
            ChamTheoClaude.banCham(
                context,
                listOf(mapOf("ma" to "2.28"), mapOf("ma" to "2.33a", "dung" to "true")),
                pham
            )
        )
        val ket = ChamTheoClaude.banCham(
            context, listOf(mapOf("ma" to "2.28", "dung" to true, "chac" to false)), pham
        )!!
        assertFalse(ket.cac.single().docRo)
    }

    @Test
    fun claude_doc_vo_dan_do_thi_tron_goi_tinh_nhu_may_cham() {
        val giaTri = mapOf(
            "cac" to listOf(
                mapOf("ma" to "2.28", "dung" to true, "soDong" to 1, "trongDanDo" to true),
                mapOf("ma" to "2.33a", "dung" to true, "soDong" to 4, "trongDanDo" to false)
            ),
            "ngayDanDo" to "2026-09-23",
            "baiDuocGiao" to listOf("Bài 2.28", " "),
            "lamHetDanDo" to true,
            "coAnhDanDo" to true
        )
        val ket = ChamTheoClaude.banCham(context, giaTri, pham)!!
        assertEquals("2026-09-23", ket.ngayDanDo)
        assertEquals(listOf("Bài 2.28"), ket.baiDuocGiao)
        assertTrue(ket.lamHetDanDo)
        assertTrue(ket.cac.single { it.ma == "2.28" }.trongDanDo)
        assertFalse(ket.cac.single { it.ma == "2.33a" }.trongDanDo)
        assertTrue(ChamTheoClaude.coAnhDanDo(giaTri))

        // Toi hom do: goi 45 phut cho bai co giao, cau lam them 2.33a tinh le.
        val lamThem = LuatCongGio.phutChoCau(ket.cac.single { it.ma == "2.33a" })
        val bang = LuatCongGio.tinh(ket, bayGio = LocalDateTime.of(2026, 9, 23, 20, 0))
        assertEquals(LuatCongGio.PHUT_TRON_GOI_DAN_DO + lamThem, bang.phut)
        // Qua trua hom sau thi vo hom qua het han. Vi vay duong Claude tinh theo luc con
        // nop chu khong theo luc Ba Huy dan ket qua.
        val tre = LuatCongGio.tinh(ket, bayGio = LocalDateTime.of(2026, 9, 24, 13, 0))
        assertTrue(tre.phut < LuatCongGio.PHUT_TRON_GOI_DAN_DO)
    }

    @Test
    fun lan_nop_khong_co_vo_dan_do_thi_moi_cau_la_bai_co_giao() {
        val ds = listOf(mapOf("ma" to "2.33a", "dung" to true, "soDong" to 4, "trongDanDo" to false))
        // Kieu cu, va kieu moi ma khong co trang vo: quy tac 17 cua may cham.
        assertTrue(ChamTheoClaude.banCham(context, ds, pham)!!.cac.single().trongDanDo)
        assertTrue(
            ChamTheoClaude.banCham(context, mapOf("cac" to ds, "coAnhDanDo" to false), pham)!!
                .cac.single().trongDanDo
        )
        // Co trang vo ma Claude khong noi cau nay thuoc bai giao: coi la lam them, nhu may.
        assertFalse(
            ChamTheoClaude.banCham(
                context,
                mapOf("cac" to listOf(mapOf("ma" to "2.33a", "dung" to true)), "coAnhDanDo" to true),
                pham
            )!!.cac.single().trongDanDo
        )
        // Hom nay da co goi: nop lai bai da sua khong duoc tinh le them lan nua.
        val sua = ChamTheoClaude.banCham(context, ds, pham)!!
        assertEquals(0, LuatCongGio.tinh(sua, goiDaCoHomNay = true).phut)
        // "lamHetDanDo" viet thanh chu thi khong co goi.
        assertFalse(
            ChamTheoClaude.banCham(context, mapOf("cac" to ds, "lamHetDanDo" to "true"), pham)!!
                .lamHetDanDo
        )
    }

    @Test
    fun pham_vi_luu_lai_duoc_theo_ma_bai() {
        KhaiChoCham.luu(context, "thu-khai", pham)
        val lai = KhaiChoCham.lay(context, "thu-khai")!!
        assertEquals(pham.cauIds, lai.cauIds)
        assertEquals("trang 47", lai.bai)
        assertNull(KhaiChoCham.lay(context, "khong-co-bai-nay"))
    }
}
