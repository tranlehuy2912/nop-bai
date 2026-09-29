package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.LuatTuVung
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Luat quy bai chup anh ra so phut choi.
 *
 * Tu 29/9/2026 duong chup anh chi con bai vo dan do, co tran rieng [LuatCongGio.TRAN_ANH]
 * 45 phut: tron goi va tinh le chung tran nay, tinh le mot dong mot phut, trac nghiem mot
 * phut mot cau, khong san khong tran tung cau. Bai lam them, on lai, Giai de da sang lam
 * tren may, luat sao cua chung o [LuatGhepTest] va [LamTrenMayTest].
 *
 * Khong dung den may hay mang, nhung de o day cho cung cho voi cac bo test kia.
 * Bo nay KHONG xoa prefs.
 */
@RunWith(AndroidJUnit4::class)
class LuatCongGioTest {

    // Thu Hai 14/9/2026, 19:00.
    private val toiThuHai = LocalDateTime.of(2026, 9, 14, 19, 0)
    private val thuHai = LocalDate.of(2026, 9, 14)

    private fun cauNho(
        ma: String,
        dung: Boolean = true,
        trongDanDo: Boolean = true,
        soDong: Int = 4
    ) = CauCham(
        ma = ma, de = "de $ma", dung = dung, dang = DangBai.CAU_NHO,
        trongDanDo = trongDanDo, soDong = soDong
    )

    /** Mot lan nop co chup vo dan do hom nay, co bai giao, va con khai da lam het. */
    private fun coGoi(cac: List<CauCham>) = KetQuaCham(
        cac = cac,
        ngayDanDo = "2026-09-14",
        baiDuocGiao = listOf("bài 2"),
        lamHetDanDo = true
    )

    // --- ngay trong vo dan do ---

    @Test
    fun vo_ghi_ngay_hom_nay_thi_duoc() {
        assertTrue(LuatCongGio.ngayDanDoHopLe(thuHai, toiThuHai))
    }

    @Test
    fun vo_ghi_hom_qua_ma_chup_sang_hom_sau_thi_van_duoc() {
        // Con hoc chieu 12:30, nen bai cua hom qua thuong lam sang hom sau.
        val sangThuBa = LocalDateTime.of(2026, 9, 15, 9, 30)
        assertTrue(LuatCongGio.ngayDanDoHopLe(thuHai, sangThuBa))
    }

    @Test
    fun vo_ghi_hom_qua_ma_da_qua_trua_thi_thoi() {
        val chieuThuBa = LocalDateTime.of(2026, 9, 15, 13, 0)
        assertFalse(LuatCongGio.ngayDanDoHopLe(thuHai, chieuThuBa))
    }

    @Test
    fun vo_ghi_thu_bay_thi_ca_ngay_chu_nhat_deu_duoc() {
        val thuBay = LocalDate.of(2026, 9, 12)
        val toiChuNhat = LocalDateTime.of(2026, 9, 13, 20, 0)
        assertTrue(LuatCongGio.ngayDanDoHopLe(thuBay, toiChuNhat))

        // Nhung sang thu Hai thi het han.
        val sangThuHai = LocalDateTime.of(2026, 9, 14, 9, 0)
        assertFalse(LuatCongGio.ngayDanDoHopLe(thuBay, sangThuHai))
    }

    @Test
    fun vo_ghi_ngay_mai_thi_khong_tinh() {
        assertFalse(LuatCongGio.ngayDanDoHopLe(LocalDate.of(2026, 9, 15), toiThuHai))
    }

    @Test
    fun doc_duoc_ngay_viet_kieu_viet_nam() {
        val ngay = LocalDate.of(2026, 9, 14)
        assertEquals(ngay, LuatCongGio.docNgay("2026-09-14"))
        assertEquals(ngay, LuatCongGio.docNgay("14/9/2026"))
        assertEquals(ngay, LuatCongGio.docNgay("14-09-2026"))
        assertEquals(ngay, LuatCongGio.docNgay("Thứ hai, ngày 14-09-2026"))
        assertEquals(ngay, LuatCongGio.docNgay("Thứ hai, ngày 14 tháng 9 năm 2026"))
        // Cai bay: "Thứ 2" cung la mot con so, va no dung TRUOC ngay.
        assertEquals(ngay, LuatCongGio.docNgay("Thứ 2, ngày 14 tháng 9 năm 2026"))
        assertEquals(ngay, LuatCongGio.docNgay("Thu 2 ngay 14 thang 9 nam 2026"))
        assertEquals(ngay, LuatCongGio.docNgay("Thứ 2 - 14/9/2026"))
        // Vo khong ghi nam: lay nam cho ra ngay gan hom nay nhat.
        assertEquals(ngay, LuatCongGio.docNgay("Thứ hai, ngày 14 tháng 9", thuHai))
        assertEquals(ngay, LuatCongGio.docNgay("ngày 14 tháng 9", LocalDate.of(2026, 9, 20)))
        // Cuoi nam: vo ghi 31/12, doc vao mung 1 thang 1 nam sau -> van la nam cu.
        assertEquals(
            LocalDate.of(2026, 12, 31),
            LuatCongGio.docNgay("ngày 31 tháng 12", LocalDate.of(2027, 1, 1))
        )
        assertNull(LuatCongGio.docNgay(null))
        assertNull(LuatCongGio.docNgay("không rõ"))
        assertNull(LuatCongGio.docNgay("ngày 32 tháng 13 năm 2026"))
    }

    // --- tron goi vo dan do ---

    @Test
    fun lam_het_bai_co_giao_thi_duoc_tron_goi() {
        val ket = coGoi(listOf(cauNho("2.26a"), cauNho("2.26b"), cauNho("2.26c"), cauNho("2.26d")))
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        // Bon cau nam trong bai co giao nen khong cong le nua: 45 chu khong phai 61.
        assertEquals(45, b.phut)
        assertEquals(45, b.phutGoi)
        assertTrue(b.dong.first().contains("Làm hết bài cô giao"))
    }

    @Test
    fun tron_goi_bao_ca_cau_ngoai_vo_khong_tinh_le_them() {
        // Truoc 29/9/2026 cau khong thuoc bai co giao la bai lam them, tinh le them ngoai
        // goi. Tu hom do moi bai chup anh la bai dan do (Ba Huy chot): co goi thi moi cau
        // dung nam trong goi, ke ca cau Claude ghi la ngoai vo.
        val ket = coGoi(
            listOf(
                cauNho("2.26a"), cauNho("2.26b"),
                cauNho("2.27a", trongDanDo = false), cauNho("2.27b", trongDanDo = false)
            )
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(45, b.phut)
        assertTrue(b.phutCua.isEmpty())
        assertEquals(listOf("2.26a", "2.26b", "2.27a", "2.27b"), b.trongGoi.map { it.ma })
    }

    @Test
    fun vo_dan_do_qua_han_thi_khong_co_tron_goi_nhung_van_tinh_le() {
        val ket = KetQuaCham(
            cac = listOf(cauNho("a"), cauNho("b"), cauNho("c")),
            ngayDanDo = "2026-09-10",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(12, b.phut)
        assertTrue(b.dong.any { it.contains("không tính trọn gói") })
        assertTrue(b.dong.any { it.contains("(4 dòng)") })
    }

    @Test
    fun bang_tinh_noi_ro_lan_nay_co_an_tron_goi_khong() {
        // Ben goi dua vao co nay de danh dau "hom nay da tinh tron goi". Truoc day
        // no phai doan bang cach do chuoi trong dong giai thich - doi mot chu trong
        // cau la co im lang, va con duoc 45 phut moi lan nop.
        val coGoi = coGoi(listOf(cauNho("a")))
        assertTrue(LuatCongGio.tinh(coGoi, bayGio = toiThuHai).daTinhGoi)

        // Da tinh tron goi hom nay roi thi lan nay khong tinh nua.
        assertFalse(LuatCongGio.tinh(coGoi, bayGio = toiThuHai, goiDaCoHomNay = true).daTinhGoi)
        // Khong chup vo dan do thi cung khong co goi.
        assertFalse(
            LuatCongGio.tinh(KetQuaCham(cac = listOf(cauNho("a"))), bayGio = toiThuHai).daTinhGoi
        )
    }

    @Test
    fun lam_chua_het_bai_co_giao_thi_tinh_le_tung_cau() {
        val ket = KetQuaCham(
            cac = listOf(cauNho("a"), cauNho("b"), cauNho("c", dung = false)),
            ngayDanDo = "2026-09-14",
            lamHetDanDo = false
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(8, b.phut)
        assertTrue(b.dong.any { it.contains("Chưa tính: c") })
    }

    /**
     * Da tinh le truoc do bao nhieu thi luc du goi chi cong them cho tron 45 (29/9/2026).
     *
     * Canh that: chieu con nop ba cau truoc, duoc tinh le; toi lam het vo dan do roi nop
     * not. Goi ma cong nguyen 45 phut thi phan tinh le chieu nay thanh tra hai lan.
     */
    @Test
    fun goi_bu_cho_tron_45_khi_da_tinh_le() {
        val ket = coGoi(listOf(cauNho("a"), cauNho("b")))
        val b = LuatCongGio.tinh(ket, daCongAnhHomNay = 30, bayGio = toiThuHai)
        assertTrue(b.daTinhGoi)
        assertEquals(15, b.phutGoi)
        assertEquals(15, b.phut)
        assertTrue(b.phutCua.isEmpty())
        assertTrue(
            b.dong.first(),
            b.dong.first().contains("+15 phút (cộng cho tròn 45, đã tính lẻ 30 phút)")
        )

        // Tinh le da du 45 thi goi van danh dau la da co (de lan sau khong tinh le nua),
        // nhung khong con phut nao.
        val du = LuatCongGio.tinh(ket, daCongAnhHomNay = 45, bayGio = toiThuHai)
        assertTrue(du.daTinhGoi)
        assertEquals(0, du.phutGoi)
        assertEquals(0, du.phut)
        assertEquals(0, LuatCongGio.tinh(ket, daCongAnhHomNay = 60, bayGio = toiThuHai).phut)
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
        val b = LuatCongGio.tinh(KetQuaCham(cac = listOf(cauNho("dài", soDong = 200))), bayGio = toiThuHai)
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
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
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
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertTrue(b.thieuDong.isEmpty())
        assertFalse(b.canBaHuyXem)
    }

    @Test
    fun hom_co_goi_thi_cau_thieu_so_dong_khong_lam_phien_ba_huy() {
        // Cau nam trong goi khong duoc phut rieng, nen so dong cua no khong doi gi.
        val ket = coGoi(listOf(cauNho("a", soDong = 0), cauNho("b")))
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(45, b.phut)
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
        assertEquals(22, LuatCongGio.tinh(KetQuaCham(cac = listOf(motTrang)), bayGio = toiThuHai).phut)

        val vaiDong = motTrang.copy(soDong = 3)
        assertEquals(3, LuatCongGio.phutChoCau(vaiDong))
        val batTrang = motTrang.copy(soDong = 200)
        assertEquals(LuatCongGio.TRAN_ANH, LuatCongGio.tinh(KetQuaCham(cac = listOf(batTrang)), bayGio = toiThuHai).phut)
    }

    @Test
    fun hoc_thuoc_luyen_chu_thi_khong_tinh_may() {
        val hocThuoc = CauCham("học thuộc", "bài thơ", dung = true,
            dang = DangBai.KHONG_TINH, soDong = 30)
        assertEquals(0, LuatCongGio.phutChoCau(hocThuoc))
        assertEquals(0, LuatCongGio.tinh(KetQuaCham(cac = listOf(hocThuoc)), bayGio = toiThuHai).phut)
    }

    @Test
    fun cau_sai_thi_khong_duoc_phut_nao_va_duoc_nhac_sua() {
        val ket = KetQuaCham(
            cac = listOf(cauNho("a", trongDanDo = false), cauNho("b", dung = false, trongDanDo = false))
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(4, b.phut)
        assertTrue(b.dong.any { it.contains("sửa lại rồi nộp tiếp") })
    }

    // --- tran rieng cua duong chup anh ---

    /**
     * Chua co goi thi tinh le, va tinh le cung chiu tran 45 (29/9/2026).
     *
     * Truoc do hom khong co goi thi khong tran nao cat phan tinh le, vi phan do chinh la
     * bai co giao. Bay gio bai co giao lam het chi duoc 45, nen lam mot nua ma nhieu dong
     * hon cung khong duoc hon lam het.
     */
    @Test
    fun tinh_le_bi_cat_o_45() {
        val ket = KetQuaCham(cac = (1..60).map { cauNho("c$it", trongDanDo = false) })
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        // 60 cau x 4 dong = 240 phut, cat con 45.
        assertEquals(LuatCongGio.TRAN_ANH, b.phut)
        assertFalse(b.daTinhGoi)
        assertEquals(45, b.phutCua.values.sum())
        assertTrue(b.dong.any { it.contains("Bài dặn dò mỗi ngày tối đa 45 phút") })
    }

    @Test
    fun tran_anh_tru_ca_phan_da_cong_luc_truoc_trong_ngay() {
        val ket = KetQuaCham(cac = (1..10).map { cauNho("them$it", trongDanDo = false) })
        // Chieu nay da cong 40 phut chup anh roi, chi con 5.
        assertEquals(5, LuatCongGio.tinh(ket, 40, toiThuHai).phut)
        // Het tran thi khong con phut nao, ke ca cau dung.
        assertEquals(0, LuatCongGio.tinh(ket, 45, toiThuHai).phut)
    }

    @Test
    fun tran_ngay_la_tong_tran_rieng_215() {
        // Chi de hien ra man hinh ("hom nay kiem duoc 60/215 phut"), khong chan gi.
        assertEquals(45, LuatCongGio.TRAN_ANH)
        assertEquals(90, LuatCongGio.TRAN_TREN_MAY)
        assertEquals(30, LuatCongGio.TRAN_ON_MOI_NGAY)
        assertEquals(
            LuatCongGio.TRAN_ANH + LuatCongGio.TRAN_TREN_MAY + LuatCongGio.TRAN_ON_MOI_NGAY +
                HocThuoc.TRAN_PHUT_MOI_NGAY + LuatTuVung.phutTrongNgay(LuatTuVung.GIAY_TRAN_MOI_NGAY),
            LuatCongGio.TRAN_NGAY
        )
        assertEquals(215, LuatCongGio.TRAN_NGAY)
    }

    // --- cho Ba Huy xem ---

    @Test
    fun cho_nao_ai_doc_khong_ro_thi_khong_tu_duyet() {
        val ket = KetQuaCham(
            cac = listOf(
                cauNho("a", trongDanDo = false),
                cauNho("d", trongDanDo = false).copy(docRo = false)
            )
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertTrue(b.canBaHuyXem)
        assertTrue(b.dong.any { it.contains("đọc không rõ") })
    }

    @Test
    fun doc_ro_het_thi_khong_can_lam_phien_ba() {
        val ket = KetQuaCham(cac = listOf(cauNho("a", trongDanDo = false)))
        assertFalse(LuatCongGio.tinh(ket, bayGio = toiThuHai).canBaHuyXem)
    }

    // --- nop lai bai da sua (loi ngay 14/9/2026) ---

    @Test
    fun nop_lai_bai_da_sua_khong_duoc_tinh_le_lai_phan_da_nam_trong_goi() {
        // Chuyen that ngay 14/9/2026. Lan mot: chup ca vo dan do, duoc tron goi 45.
        // Lan hai con sua may cau sai roi nop lai - man hinh sua bai chi cho chup
        // BAI GIAI, khong co trang vo dan do, nen ngay_dan_do ve null.
        //
        // Truoc khi sua: ngay null -> "khong co goi" -> tinh le tung cau -> 20 cau
        // dung x 2 phut = 40 phut nua cho dung xap bai da tra 45 phut. Tong 85.
        val nopLai = KetQuaCham(
            cac = (1..20).map { cauNho("cau$it", soDong = 1) },
            ngayDanDo = null,
            lamHetDanDo = true
        )
        val b = LuatCongGio.tinh(nopLai, bayGio = toiThuHai, goiDaCoHomNay = true)
        assertEquals(0, b.phut)
        assertEquals(20, b.trongGoi.size)
        assertTrue(b.dong.any { it.contains("nằm trong trọn gói") })
    }

    /**
     * Hom da co goi thi bai nop them khong cong phut nao (29/9/2026).
     *
     * Truoc do cau ngoai vo dan do la bai lam them, van tinh le sau khi da co goi. Tu 29/9
     * moi bai chup anh la bai dan do, bai lam them chi con tren may: nop them anh sau goi
     * thi moi cau dung, ca trac nghiem, deu nam trong goi.
     */
    @Test
    fun hom_da_co_goi_thi_bai_nop_them_0_phut() {
        val ket = KetQuaCham(
            cac = listOf(
                cauNho("trong 1"), cauNho("ngoài", trongDanDo = false, soDong = 10),
                CauCham("tn", "de tn", dung = true, dang = DangBai.TRAC_NGHIEM),
                cauNho("sai", dung = false)
            )
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai, goiDaCoHomNay = true)
        assertEquals(0, b.phut)
        assertFalse(b.daTinhGoi)
        assertTrue(b.phutCua.isEmpty())
        assertEquals(listOf("trong 1", "ngoài", "tn"), b.trongGoi.map { it.ma })
        assertTrue(b.dong.any { it.contains("Hôm nay đã tính trọn gói bài dặn dò rồi") })
        assertTrue(b.dong.any { it.contains("nằm trong trọn gói bài dặn dò hôm nay") })
        assertTrue(b.dong.any { it.contains("Chưa tính: sai") })
    }

    @Test
    fun phut_ghi_vao_so_dung_bang_phut_that_su_duoc_tra() {
        // So cai phai ghi con so DA TRA, khong phai gia niem yet cua cau. Ngay co tron
        // goi, cau nam trong goi duoc 0: ghi gia niem yet thi so cong them phut cho tung
        // cau, va tran 45 cua hom do tu nhien hut di bay nhieu.
        val ket = coGoi(listOf(cauNho("trong"), cauNho("ngoài", trongDanDo = false)))
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(45, b.phut)
        assertTrue(b.phutCua.isEmpty())
        assertEquals(listOf("trong", "ngoài"), b.trongGoi.map { it.ma })
    }

    @Test
    fun bi_tran_cat_thi_so_ghi_dung_phan_con_lai() {
        val ket = KetQuaCham(cac = (1..10).map { cauNho("them$it", trongDanDo = false) })
        // Con 5 phut tran: cau dau an bon, cau thu hai an mot, cac cau sau khong duoc gi.
        val b = LuatCongGio.tinh(ket, 40, toiThuHai)
        assertEquals(5, b.phut)
        assertEquals(5, b.phutCua.values.sum())
        assertEquals(listOf("them1", "them2"), b.phutCua.keys.toList())
        assertEquals(1, b.phutCua["them2"])
    }

    // --- vo dan do khong giao bai tap nao ---

    @Test
    fun vo_dan_do_chi_dan_viec_chu_khong_giao_bai_thi_khong_co_tron_goi() {
        // Vo cua Le Hoa ngay 14/9/2026: "tiet sau kiem tra tu vung", "tiet sau kiem
        // tra bai 2 bai 3", "mang sach vo day du", "lam dung noi quy nha truong".
        // Khong mot bai tap nao, ma may van tra lam_het_dan_do = true -> 45 phut.
        val ket = KetQuaCham(
            cac = listOf(cauNho("a", soDong = 1), cauNho("b", soDong = 1)),
            ngayDanDo = "2026-09-14",
            baiDuocGiao = emptyList(),
            lamHetDanDo = true
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        // Hai cau mot dong: hai phut, khong con san bon phut mot cau (29/9/2026).
        assertEquals(2, b.phut)
        assertFalse(b.daTinhGoi)
        assertTrue(b.dong.any { it.contains("không giao bài tập nào") })
    }

    @Test
    fun tron_goi_noi_ro_co_giao_giao_bai_nao() {
        val ket = KetQuaCham(
            cac = listOf(cauNho("a")),
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2", "bài 3"),
            lamHetDanDo = true
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(45, b.phut)
        assertTrue(b.dong.first().contains("bài 2, bài 3"))
    }

    // --- trac nghiem tinh theo cum ---

    private fun tracNghiem(ma: String, dung: Boolean = true) = CauCham(
        ma = ma, de = "de $ma", dung = dung, dang = DangBai.TRAC_NGHIEM,
        trongDanDo = false, soDong = 1
    )

    @Test
    fun trac_nghiem_moi_cau_dung_duoc_mot_phut() {
        val ket = KetQuaCham(cac = (1..12).map { tracNghiem("c$it") })
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
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
            LuatCongGio.tinh(KetQuaCham(cac = (1..n).map { tracNghiem("c$it") }), bayGio = toiThuHai).phut
        assertEquals(40, phut(40))
        assertEquals(LuatCongGio.TRAN_ANH, phut(200))
    }

    @Test
    fun cau_trac_nghiem_sai_thi_khong_tinh_vao_cum() {
        val ket = KetQuaCham(
            cac = (1..8).map { tracNghiem("c$it") } + (1..8).map { tracNghiem("s$it", dung = false) }
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(8, b.phut)
        assertTrue(b.dong.any { it.contains("Chưa tính") })
    }

    @Test
    fun trang_vua_co_trac_nghiem_vua_co_tu_luan() {
        val ket = KetQuaCham(
            cac = (1..8).map { tracNghiem("tn$it") } +
                listOf(cauNho("tự luận 1", trongDanDo = false, soDong = 6))
        )
        // 8 cau khoanh ra 8 phut, bai tu luan 6 dong ra 6 phut.
        assertEquals(14, LuatCongGio.tinh(ket, bayGio = toiThuHai).phut)
    }

    @Test
    fun so_ghi_dung_tong_phut_cua_cum_trac_nghiem() {
        val ket = KetQuaCham(cac = (1..60).map { tracNghiem("c$it") })
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
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
        val b = LuatCongGio.tinh(ket, 42, toiThuHai)
        assertEquals(3, b.phut)
        assertEquals(3, b.phutCua.values.sum())
    }

    // --- cap khong du so phut da tinh ---

    @Test
    fun cap_du_thi_so_ghi_dung_bang_tinh() {
        val b = LuatCongGio.tinh(
            KetQuaCham(cac = listOf(cauNho("1", soDong = 6), cauNho("2", soDong = 6))),
            bayGio = toiThuHai
        )
        assertEquals(12, b.phut)
        assertEquals(0 to b.phutCua, LuatCongGio.chiaPhutDaCap(b, 12))
    }

    @Test
    fun cap_khong_du_thi_cau_truoc_lay_truoc_cau_sau_con_0() {
        val b = LuatCongGio.tinh(
            KetQuaCham(cac = listOf(cauNho("1", soDong = 6), cauNho("2", soDong = 6))),
            bayGio = toiThuHai
        )
        // Tablet chi cap duoc 3 phut (phieu cat o gio chot): so ghi 3 phut da tra, khong
        // phai 12.
        assertEquals(0 to mapOf("1" to 3, "2" to 0), LuatCongGio.chiaPhutDaCap(b, 3))
    }

    @Test
    fun cap_khong_du_thi_goi_lay_truoc() {
        val ket = coGoi(listOf(cauNho("1"), cauNho("9", trongDanDo = false, soDong = 6)))
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(45, b.phut)
        assertEquals(45 to emptyMap<String, Int>(), LuatCongGio.chiaPhutDaCap(b, 45))
        assertEquals(30 to emptyMap<String, Int>(), LuatCongGio.chiaPhutDaCap(b, 30))

        // Goi bu (da tinh le 30): cap 10 thi goi an 10, khong phai 15.
        val bu = LuatCongGio.tinh(ket, daCongAnhHomNay = 30, bayGio = toiThuHai)
        assertEquals(10 to emptyMap<String, Int>(), LuatCongGio.chiaPhutDaCap(bu, 10))
        assertEquals(15 to emptyMap<String, Int>(), LuatCongGio.chiaPhutDaCap(bu, 15))
    }
}
