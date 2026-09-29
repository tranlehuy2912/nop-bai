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
import vn.huytl.homeworkgate.data.LoaiLoi
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi

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
        // Bon dong lam bai cua mot cau nho: mot dong mot phut (khong con san tu 29/9/2026).
        assertEquals(4, bang.phut)
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

    /**
     * Kieu sai Claude xep (tu 28/9/2026) di vao ban cham, ep ve bay nhan cua LoaiLoi: nhan
     * la thanh KHAC, cau dung thi khong co nhan nao du Claude co ghi. Man Tien bo va "Luyện
     * chỗ hay vấp" cong don theo nhan nay, ma tu khi bo may cham chi con Claude dat nhan.
     */
    @Test
    fun kieu_sai_claude_xep_ep_ve_bay_nhan() {
        val ket = ChamTheoClaude.banCham(
            context,
            listOf(
                mapOf("ma" to "2.28", "dung" to true, "loaiLoi" to "SAI_DAU"),
                mapOf("ma" to "2.33a", "dung" to false, "loaiLoi" to "sai_buoc"),
                mapOf("ma" to "7", "dung" to false, "de" to "Tính 3 - 5.", "loaiLoi" to "SAI_LUNG_TUNG"),
                mapOf("ma" to "8", "dung" to false, "de" to "Tính 4 - 9.")
            ),
            pham
        )!!
        assertEquals("", ket.cac.single { it.ma == "2.28" }.loaiLoi)
        assertEquals(LoaiLoi.SAI_BUOC, ket.cac.single { it.ma == "2.33a" }.loaiLoi)
        assertEquals(LoaiLoi.KHAC, ket.cac.single { it.ma == "7" }.loaiLoi)
        assertEquals("", ket.cac.single { it.ma == "8" }.loaiLoi)
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

    /**
     * Bang dieu khien ban cu con gui phan vo dan do (ngayDanDo, baiDuocGiao, lamHetDanDo,
     * coAnhDanDo, trongDanDo tung cau). Tu 30/9/2026 khong con tron goi: tablet bo qua cac
     * truong do, moi cau tinh le.
     */
    @Test
    fun ban_cham_con_mang_phan_vo_dan_do_thi_bo_qua_va_tinh_le() {
        val giaTri = mapOf(
            "cac" to listOf(
                mapOf("ma" to "2.28", "dung" to true, "soDong" to 1, "trongDanDo" to true),
                mapOf("ma" to "2.33a", "dung" to true, "soDong" to 4, "trongDanDo" to false)
            ),
            "ngayDanDo" to "2026-09-23",
            "baiDuocGiao" to listOf("Bài 2.28"),
            "lamHetDanDo" to true,
            "coAnhDanDo" to true
        )
        val ket = ChamTheoClaude.banCham(context, giaTri, pham)!!
        assertEquals(2, ket.cac.size)
        val bang = LuatCongGio.tinh(ket)
        // Trac nghiem 2.28 mot phut, cau nho 2.33a bon dong bon phut: 5, khong phai 45.
        assertEquals(5, bang.phut)
        assertEquals(setOf("2.28", "2.33a"), bang.phutCua.keys)
    }

    @Test
    fun ma_lech_cach_viet_van_khop_ngan_hang() {
        val ket = ChamTheoClaude.banCham(
            context,
            listOf(
                mapOf("ma" to "Câu 2.33A", "dung" to true, "soDong" to 4),
                mapOf("ma" to " 2.28) ", "dung" to true, "soDong" to 1)
            ),
            pham
        )!!
        // Truoc day so y het: "Câu 2.33A" khong khop cau nao, mat de va tra 0 phut.
        val nho = ket.cac.single { it.cauId == "toan8t1:2.33a" }
        assertEquals("2.33a", nho.ma)
        assertTrue(nho.coDe)
        assertEquals(4, LuatCongGio.phutChoCau(nho))
        assertEquals("toan8t1:2.28", ket.cac.single { it.ma == "2.28" }.cauId)

        assertEquals("2.33a", ChamTheoClaude.chuanMa("2.33 a:"))
        assertEquals("b3.c7", ChamTheoClaude.chuanMa("Câu B3.C7."))
    }

    /**
     * Ma nguyen van cua Claude giu o [vn.huytl.homeworkgate.data.CauCham.maGoc] (29/9/2026),
     * ke ca khi khop sach doi [vn.huytl.homeworkgate.data.CauCham.ma] sang ma cua sach. Bang
     * dieu khien dung ma nay de tim lai cau trong ban Claude no giu, luc Ba Huy tu xu cau
     * chua chac (lenh XUCAU).
     */
    @Test
    fun ma_goc_giu_ma_claude_khi_ma_sach_khac() {
        val ket = ChamTheoClaude.banCham(
            context,
            listOf(
                mapOf("ma" to "Câu 2.33A", "dung" to true, "soDong" to 4),
                mapOf("ma" to " 2.28) ", "dung" to true, "soDong" to 1),
                mapOf("ma" to "câu 5", "dung" to true, "de" to "Tính 1 + 1.", "soDong" to 1)
            ),
            pham
        )!!
        val nho = ket.cac.single { it.cauId == "toan8t1:2.33a" }
        assertEquals("2.33a", nho.ma)
        assertEquals("Câu 2.33A", nho.maGoc)
        // Khoang trang hai dau da cat, con lai nguyen van.
        assertEquals("2.28)", ket.cac.single { it.cauId == "toan8t1:2.28" }.maGoc)
        // Cau ngoai sach: ma va ma goc la mot.
        val ngoai = ket.cac.single { it.cauId == null }
        assertEquals("câu 5", ngoai.ma)
        assertEquals("câu 5", ngoai.maGoc)
    }

    /**
     * Ba Huy xem anh roi bam "Chụp lại" cho mot cau (lenh XUCAU, 29/9/2026): coi nhu may khong
     * nhin thay de, ke ca cau trong sach von luon co de. Khong co de thi khong cham, khong
     * cong phut, va luat cong gio coi la chua dung.
     */
    @Test
    fun chup_lai_thi_cau_do_coi_nhu_khong_co_de() {
        val ket = ChamTheoClaude.banCham(
            context,
            listOf(
                mapOf("ma" to "2.33a", "dung" to true, "soDong" to 4, "chupLai" to true),
                mapOf("ma" to "2.28", "dung" to true, "soDong" to 1, "chupLai" to false),
                mapOf("ma" to "7", "dung" to true, "de" to "Tính 3 - 5.", "soDong" to 2, "chupLai" to true),
                // Viet sai kieu thi khong phai lenh chup lai: van cham binh thuong.
                mapOf("ma" to "8", "dung" to true, "de" to "Tính 4 - 9.", "soDong" to 2, "chupLai" to "true")
            ),
            pham
        )!!
        val chup = ket.cac.single { it.ma == "2.33a" }
        assertFalse(chup.coDe)
        // De sach van giu, chi la khong cham.
        assertTrue(chup.de.isNotBlank())
        assertFalse(ket.cac.single { it.ma == "7" }.coDe)
        assertTrue(ket.cac.single { it.ma == "2.28" }.coDe)
        assertTrue(ket.cac.single { it.ma == "8" }.coDe)

        val bang = LuatCongGio.tinh(ket)
        // Chi 2.28 (trac nghiem, 1 phut) va 8 (2 dong) duoc tinh.
        assertEquals(3, bang.phut)
        assertEquals(setOf("2.28", "8"), bang.phutCua.keys)
        // Cau chup lai khong phai cau thieu so dong: khong hoi Ba Huy lan nua.
        assertTrue(bang.thieuDong.isEmpty())
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
