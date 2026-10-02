package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.LuatTuVung

/**
 * Luat quy bai chup anh ra so phut choi.
 *
 * Tu 29/9/2026 duong chup anh chi con bai co giao, co tran rieng [LuatCongGio.TRAN_ANH]
 * 45 phut: mot dong mot phut, trac nghiem mot phut mot cau, khong san khong tran tung cau.
 * Tu 30/9/2026 khong con tron goi vo dan do: moi cau tinh le. Bai lam them, on lai, Giai de
 * da sang lam tren may, luat sao cua chung o [LuatGhepTest] va [LamTrenMayTest]. Han cua
 * vo dan do (nay de nhac bai) o [NhacBaiTest].
 *
 * Khong dung den may hay mang, nhung de o day cho cung cho voi cac bo test kia.
 * Bo nay KHONG xoa prefs.
 */
@RunWith(AndroidJUnit4::class)
class LuatCongGioTest {

    private fun cauNho(
        ma: String,
        dung: Boolean = true,
        soDong: Int = 4
    ) = CauCham(ma = ma, de = "de $ma", dung = dung, dang = DangBai.CAU_NHO, soDong = soDong)

    // --- khong con tron goi ---

    /**
     * Lam het bai co giao cung chi tinh le (Ba Huy bo tron goi ngay 30/9/2026).
     *
     * Truoc do bon cau bon dong nay, neu la het bai trong vo dan do hom nay, duoc tron goi
     * 45 phut chu khong phai 16.
     */
    @Test
    fun lam_het_bai_co_giao_cung_chi_tinh_le_tung_cau() {
        val ket = KetQuaCham(cac = listOf(cauNho("2.26a"), cauNho("2.26b"), cauNho("2.26c"), cauNho("2.26d")))
        val b = LuatCongGio.tinh(ket)
        assertEquals(16, b.phut)
        assertEquals(mapOf("2.26a" to 4, "2.26b" to 4, "2.26c" to 4, "2.26d" to 4), b.phutCua)
        assertTrue(b.dong.none { it.contains("trọn gói") || it.contains("Làm hết bài cô giao") })
    }

    @Test
    fun lam_chua_het_bai_co_giao_thi_tinh_le_tung_cau() {
        val ket = KetQuaCham(cac = listOf(cauNho("a"), cauNho("b"), cauNho("c", dung = false)))
        val b = LuatCongGio.tinh(ket)
        assertEquals(8, b.phut)
        assertTrue(b.dong.any { it.contains("Chưa tính: c") })
    }

    // --- tinh theo so dong lam bai ---

    @Test
    fun cau_ngan_khong_con_san_bon_phut() {
        // Truoc 29/9/2026 moi cau it nhat 4 phut. Ba Huy bo san: mot dong mot phut, dung
        // bang so dong con viet.
        assertEquals(1, LuatCongGio.phutChoCau(cauNho("a", soDong = 1)))
        assertEquals(3, LuatCongGio.phutChoCau(cauNho("a", soDong = 3)))
        assertEquals(4, LuatCongGio.phutChoCau(cauNho("a", soDong = 4)))
        assertEquals(5, LuatCongGio.phutChoCau(cauNho("a", soDong = 5)))
    }

    @Test
    fun mot_dong_mot_phut() {
        assertEquals(10, LuatCongGio.phutChoCau(cauNho("bài 1", soDong = 10)))
        assertEquals(15, LuatCongGio.phutChoCau(cauNho("bài 2", soDong = 15)))
    }

    @Test
    fun mot_cau_khong_con_tran_hai_muoi_phut_chi_con_tran_anh() {
        // Truoc 29/9/2026 mot cau toi da 20 phut. Bo tran tung cau, chi con tran 45 cua ca
        // duong chup anh trong ngay.
        assertEquals(40, LuatCongGio.phutChoCau(cauNho("dài", soDong = 40)))
        assertEquals(200, LuatCongGio.phutChoCau(cauNho("dài", soDong = 200)))
        val b = LuatCongGio.tinh(KetQuaCham(cac = listOf(cauNho("dài", soDong = 200))))
        assertEquals(LuatCongGio.TRAN_ANH, b.phut)
        assertEquals(mapOf("dài" to 45), b.phutCua)
    }

    @Test
    fun claude_khong_ghi_so_dong_thi_cau_do_0_phut() {
        // Truoc 29/9/2026 luc do tra muc du phong 4 phut, bai rieng 10 phut. Bo san roi thi
        // muc du phong thanh nguoc doi (cau quen dem duoc nhieu hon cau mot dong that), nen
        // cau do ra 0 va cho Ba Huy chon so dong.
        val nho = cauNho("a", soDong = 0)
        assertEquals(0, LuatCongGio.phutChoCau(nho))
        assertTrue(LuatCongGio.thieuSoDong(nho))
        val rieng = CauCham("bài 1", "de", dung = true, dang = DangBai.BAI_RIENG)
        assertEquals(0, LuatCongGio.phutChoCau(rieng))
        assertTrue(LuatCongGio.thieuSoDong(rieng))
    }

    @Test
    fun thieu_so_dong_thi_cho_ba_huy_xem() {
        val ket = KetQuaCham(cac = listOf(cauNho("a", soDong = 4), cauNho("b", soDong = 0)))
        val b = LuatCongGio.tinh(ket)
        // Cau co so dong van tinh; cau thieu so dong 0 phut, va ca bai dung tu duyet.
        assertEquals(4, b.phut)
        assertEquals(mapOf("a" to 4), b.phutCua)
        assertEquals(listOf("b"), b.thieuDong.map { it.ma })
        assertTrue(b.canBaHuyXem)
        assertTrue(b.dong.any { it.contains("Claude chưa ghi số dòng: b") })
    }

    @Test
    fun thieu_so_dong_chi_tinh_cho_cau_dung_tu_luan_co_de() {
        // Trac nghiem khong can so dong, hoc thuoc khong tinh phut, cau sai chua co gi de
        // tra, cau khong co de thi khong ai cham duoc: ca bon khong phai hoi Ba Huy.
        assertFalse(LuatCongGio.thieuSoDong(
            CauCham("tn", "de", dung = true, dang = DangBai.TRAC_NGHIEM)
        ))
        assertFalse(LuatCongGio.thieuSoDong(
            CauCham("ht", "de", dung = true, dang = DangBai.KHONG_TINH)
        ))
        assertFalse(LuatCongGio.thieuSoDong(cauNho("sai", dung = false, soDong = 0)))
        assertFalse(LuatCongGio.thieuSoDong(cauNho("khong de", soDong = 0).copy(coDe = false)))

        val ket = KetQuaCham(
            cac = listOf(
                CauCham("tn", "de tn", dung = true, dang = DangBai.TRAC_NGHIEM),
                cauNho("sai", dung = false, soDong = 0)
            )
        )
        val b = LuatCongGio.tinh(ket)
        assertTrue(b.thieuDong.isEmpty())
        assertFalse(b.canBaHuyXem)
    }

    @Test
    fun bai_viet_dai_cung_mot_dong_mot_phut() {
        // Truoc 29/9/2026 bai viet dai tinh ba dong bon phut, san 10, tran 60. Nay chung
        // luat voi moi cau: mot dong mot phut, chi con tran 45 cua ca phan.
        val motTrang = CauCham("đoạn văn", "tả con mèo", dung = true,
            dang = DangBai.VIET_DAI, soDong = 22)
        assertEquals(22, LuatCongGio.phutChoCau(motTrang))
        assertEquals(22, LuatCongGio.tinh(KetQuaCham(cac = listOf(motTrang))).phut)

        val vaiDong = motTrang.copy(soDong = 3)
        assertEquals(3, LuatCongGio.phutChoCau(vaiDong))
        val batTrang = motTrang.copy(soDong = 200)
        assertEquals(LuatCongGio.TRAN_ANH, LuatCongGio.tinh(KetQuaCham(cac = listOf(batTrang))).phut)
    }

    @Test
    fun hoc_thuoc_luyen_chu_thi_khong_tinh_may() {
        val hocThuoc = CauCham("học thuộc", "bài thơ", dung = true,
            dang = DangBai.KHONG_TINH, soDong = 30)
        assertEquals(0, LuatCongGio.phutChoCau(hocThuoc))
        assertEquals(0, LuatCongGio.tinh(KetQuaCham(cac = listOf(hocThuoc))).phut)
    }

    @Test
    fun cau_sai_thi_khong_duoc_phut_nao_va_duoc_nhac_sua() {
        val ket = KetQuaCham(
            cac = listOf(cauNho("a"), cauNho("b", dung = false))
        )
        val b = LuatCongGio.tinh(ket)
        assertEquals(4, b.phut)
        assertTrue(b.dong.any { it.contains("sửa lại rồi nộp tiếp") })
    }

    // --- tran rieng cua duong chup anh ---

    /**
     * Tinh le chiu tran 45 cua ca ngay (29/9/2026). Truoc 30/9/2026 day cung la gia tron goi,
     * nen lam mot nua ma nhieu dong hon cung khong duoc hon lam het.
     */
    @Test
    fun tinh_le_bi_cat_o_45() {
        val ket = KetQuaCham(cac = (1..60).map { cauNho("c$it") })
        val b = LuatCongGio.tinh(ket)
        // 60 cau x 4 dong = 240 phut, cat con 45.
        assertEquals(LuatCongGio.TRAN_ANH, b.phut)
        assertEquals(45, b.phutCua.values.sum())
        assertTrue(b.dong.any { it.contains("Bài chụp ảnh mỗi ngày tối đa 45 phút") })
    }

    @Test
    fun tran_anh_tru_ca_phan_da_cong_luc_truoc_trong_ngay() {
        val ket = KetQuaCham(cac = (1..10).map { cauNho("them$it") })
        // Chieu nay da cong 40 phut chup anh roi, chi con 5.
        assertEquals(5, LuatCongGio.tinh(ket, 40).phut)
        // Het tran thi khong con phut nao, ke ca cau dung.
        assertEquals(0, LuatCongGio.tinh(ket, 45).phut)
    }

    @Test
    fun tran_ngay_la_tong_tran_rieng_245() {
        // Chi de hien ra man hinh ("hom nay kiem duoc 60/245 phut"), khong chan gi. Tran lam tren
        // may 120 tu 2/10/2026 (Ba Huy chot khi bai SGK chuyen len may), truoc do 90 va tong 215.
        assertEquals(45, LuatCongGio.TRAN_ANH)
        assertEquals(120, LuatCongGio.TRAN_TREN_MAY)
        assertEquals(30, LuatCongGio.TRAN_ON_MOI_NGAY)
        assertEquals(
            LuatCongGio.TRAN_ANH + LuatCongGio.TRAN_TREN_MAY + LuatCongGio.TRAN_ON_MOI_NGAY +
                HocThuoc.TRAN_PHUT_MOI_NGAY + LuatTuVung.phutTrongNgay(LuatTuVung.GIAY_TRAN_MOI_NGAY),
            LuatCongGio.TRAN_NGAY
        )
        assertEquals(245, LuatCongGio.TRAN_NGAY)
    }

    // --- cho Ba Huy xem ---

    @Test
    fun cho_nao_ai_doc_khong_ro_thi_khong_tu_duyet() {
        val ket = KetQuaCham(
            cac = listOf(
                cauNho("a"),
                cauNho("d").copy(docRo = false)
            )
        )
        val b = LuatCongGio.tinh(ket)
        assertTrue(b.canBaHuyXem)
        assertTrue(b.dong.any { it.contains("đọc không rõ") })
    }

    @Test
    fun doc_ro_het_thi_khong_can_lam_phien_ba() {
        val ket = KetQuaCham(cac = listOf(cauNho("a")))
        assertFalse(LuatCongGio.tinh(ket).canBaHuyXem)
    }

    @Test
    fun bi_tran_cat_thi_so_ghi_dung_phan_con_lai() {
        val ket = KetQuaCham(cac = (1..10).map { cauNho("them$it") })
        // Con 5 phut tran: cau dau an bon, cau thu hai an mot, cac cau sau khong duoc gi.
        val b = LuatCongGio.tinh(ket, 40)
        assertEquals(5, b.phut)
        assertEquals(5, b.phutCua.values.sum())
        assertEquals(listOf("them1", "them2"), b.phutCua.keys.toList())
        assertEquals(1, b.phutCua["them2"])
    }

    @Test
    fun cau_mot_dong_duoc_mot_phut() {
        // Hai cau mot dong: hai phut, khong con san bon phut mot cau (29/9/2026).
        val ket = KetQuaCham(cac = listOf(cauNho("a", soDong = 1), cauNho("b", soDong = 1)))
        assertEquals(2, LuatCongGio.tinh(ket).phut)
    }

    // --- trac nghiem tinh theo cum ---

    private fun tracNghiem(ma: String, dung: Boolean = true) = CauCham(
        ma = ma, de = "de $ma", dung = dung, dang = DangBai.TRAC_NGHIEM, soDong = 1
    )

    @Test
    fun trac_nghiem_moi_cau_dung_duoc_mot_phut() {
        val ket = KetQuaCham(cac = (1..12).map { tracNghiem("c$it") })
        val b = LuatCongGio.tinh(ket)
        // Mot phut mot cau tu 29/9/2026. Truoc do hai phut (27/9), mot phut (23/9), va
        // truoc nua bon cau mot phut.
        assertEquals(12, b.phut)
        assertTrue(b.dong.any { it.contains("12 câu trắc nghiệm đúng: +12 phút") })
    }

    @Test
    fun cum_trac_nghiem_khong_con_tran_rieng_chi_chung_tran_anh() {
        // Truoc 29/9/2026 moi lan nop toi da 30 phut trac nghiem. Nay chi con tran 45 cua
        // ca duong chup anh.
        fun phut(n: Int) =
            LuatCongGio.tinh(KetQuaCham(cac = (1..n).map { tracNghiem("c$it") })).phut
        assertEquals(40, phut(40))
        assertEquals(LuatCongGio.TRAN_ANH, phut(200))
    }

    @Test
    fun cau_trac_nghiem_sai_thi_khong_tinh_vao_cum() {
        val ket = KetQuaCham(
            cac = (1..8).map { tracNghiem("c$it") } + (1..8).map { tracNghiem("s$it", dung = false) }
        )
        val b = LuatCongGio.tinh(ket)
        assertEquals(8, b.phut)
        assertTrue(b.dong.any { it.contains("Chưa tính") })
    }

    @Test
    fun trang_vua_co_trac_nghiem_vua_co_tu_luan() {
        val ket = KetQuaCham(
            cac = (1..8).map { tracNghiem("tn$it") } +
                listOf(cauNho("tự luận 1", soDong = 6))
        )
        // 8 cau khoanh ra 8 phut, bai tu luan 6 dong ra 6 phut.
        assertEquals(14, LuatCongGio.tinh(ket).phut)
    }

    @Test
    fun so_ghi_dung_tong_phut_cua_cum_trac_nghiem() {
        val ket = KetQuaCham(cac = (1..60).map { tracNghiem("c$it") })
        val b = LuatCongGio.tinh(ket)
        // Sau muoi cau bi tran 45 cat. So phai ghi du ca sau muoi cau (de lan sau khong
        // tinh lai) ma cong lai van dung 45 phut, khong phinh thanh 60.
        assertEquals(45, b.phut)
        assertEquals(60, b.phutCua.size)
        assertEquals(45, b.phutCua.values.sum())
    }

    @Test
    fun cum_trac_nghiem_cung_bi_tran_anh_chan() {
        val ket = KetQuaCham(cac = (1..40).map { tracNghiem("c$it") })
        // Con 3 phut trong tran 45: cum 40 phut bi cat con 3.
        val b = LuatCongGio.tinh(ket, 42)
        assertEquals(3, b.phut)
        assertEquals(3, b.phutCua.values.sum())
    }

    // --- cap khong du so phut da tinh ---

    @Test
    fun cap_du_thi_so_ghi_dung_bang_tinh() {
        val b = LuatCongGio.tinh(
            KetQuaCham(cac = listOf(cauNho("1", soDong = 6), cauNho("2", soDong = 6)))
        )
        assertEquals(12, b.phut)
        assertEquals(b.phutCua, LuatCongGio.chiaPhutDaCap(b, 12))
    }

    @Test
    fun cap_khong_du_thi_cau_truoc_lay_truoc_cau_sau_con_0() {
        val b = LuatCongGio.tinh(
            KetQuaCham(cac = listOf(cauNho("1", soDong = 6), cauNho("2", soDong = 6)))
        )
        // Tablet chi cap duoc 3 phut (phieu cat o gio chot): so ghi 3 phut da tra, khong
        // phai 12.
        assertEquals(mapOf("1" to 3, "2" to 0), LuatCongGio.chiaPhutDaCap(b, 3))
    }
}
