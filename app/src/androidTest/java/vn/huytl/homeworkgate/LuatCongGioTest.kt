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
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.LuatCongGio
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Luat quy bai lam ra so phut choi.
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
        val ket = KetQuaCham(
            cac = listOf(cauNho("2.26a"), cauNho("2.26b"), cauNho("2.26c"), cauNho("2.26d")),
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        // Bon cau nam trong bai co giao nen khong cong le nua: 45 chu khong phai 53.
        assertEquals(45, b.phut)
        assertTrue(b.dong.first().contains("Làm hết bài cô giao"))
    }

    @Test
    fun tron_goi_cong_them_bai_lam_them_ngoai_vo() {
        val ket = KetQuaCham(
            cac = listOf(
                cauNho("2.26a"), cauNho("2.26b"),
                cauNho("2.27a", trongDanDo = false), cauNho("2.27b", trongDanDo = false)
            ),
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        assertEquals(45 + 4, LuatCongGio.tinh(ket, bayGio = toiThuHai).phut)
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
        assertEquals(6, b.phut)
        assertTrue(b.dong.any { it.contains("không tính trọn gói") })
        assertTrue(b.dong.any { it.contains("(4 dòng)") })
    }

    @Test
    fun bang_tinh_noi_ro_lan_nay_co_an_tron_goi_khong() {
        // Ben goi dua vao co nay de danh dau "hom nay da tinh tron goi". Truoc day
        // no phai doan bang cach do chuoi trong dong giai thich - doi mot chu trong
        // cau la co im lang, va con duoc 45 phut moi lan nop.
        val coGoi = KetQuaCham(
            cac = listOf(cauNho("a")), ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"), lamHetDanDo = true
        )
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
        assertEquals(4, b.phut)
        assertTrue(b.dong.any { it.contains("Chưa tính: c") })
    }

    // --- tinh le ---

    // --- tinh theo so dong lam bai ---

    @Test
    fun cau_ngan_bon_dong_van_ra_hai_phut_nhu_luat_cu() {
        // Bon cau trong anh that cua Le Hoa: moi cau 3-5 dong.
        assertEquals(2, LuatCongGio.phutChoCau(cauNho("a", soDong = 3)))
        assertEquals(2, LuatCongGio.phutChoCau(cauNho("a", soDong = 4)))
        assertEquals(2, LuatCongGio.phutChoCau(cauNho("a", soDong = 5)))
    }

    @Test
    fun bai_dai_muoi_dong_ra_nam_phut_nhu_luat_cu() {
        assertEquals(5, LuatCongGio.phutChoCau(cauNho("bài 1", soDong = 10)))
        assertEquals(7, LuatCongGio.phutChoCau(cauNho("bài 2", soDong = 15)))
    }

    @Test
    fun mot_cau_toi_da_muoi_phut_du_viet_bao_nhieu_dong() {
        // Viet dai dong de kiem gio thi khong an thua.
        assertEquals(10, LuatCongGio.phutChoCau(cauNho("dài", soDong = 40)))
        assertEquals(10, LuatCongGio.phutChoCau(cauNho("dài", soDong = 200)))
    }

    @Test
    fun khong_dem_duoc_dong_thi_quay_ve_luat_phang_cu() {
        assertEquals(2, LuatCongGio.phutChoCau(cauNho("a", soDong = 0)))
        assertEquals(
            5,
            LuatCongGio.phutChoCau(CauCham("bài 1", "de", dung = true, dang = DangBai.BAI_RIENG))
        )
    }

    @Test
    fun bai_viet_dai_mot_trang_ra_khoang_muoi_bon_phut() {
        val motTrang = CauCham("đoạn văn", "tả con mèo", dung = true,
            dang = DangBai.VIET_DAI, soDong = 22)
        assertEquals(14, LuatCongGio.phutChoCau(motTrang))
        assertEquals(14, LuatCongGio.tinh(KetQuaCham(cac = listOf(motTrang)), bayGio = toiThuHai).phut)
    }

    @Test
    fun bai_viet_dai_co_san_va_co_tran() {
        val vaiDong = CauCham("đoạn ngắn", "de", dung = true,
            dang = DangBai.VIET_DAI, soDong = 3)
        assertEquals(5, LuatCongGio.phutChoCau(vaiDong))

        val batTrang = CauCham("bài văn", "de", dung = true,
            dang = DangBai.VIET_DAI, soDong = 200)
        assertEquals(30, LuatCongGio.phutChoCau(batTrang))
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
        assertEquals(2, b.phut)
        assertTrue(b.dong.any { it.contains("sửa lại rồi nộp tiếp") })
    }

    // --- tran bai lam them ---

    @Test
    fun bai_lam_them_co_tran_moi_ngay() {
        val ket = KetQuaCham(
            cac = (1..60).map { cauNho("them$it", trongDanDo = false) },
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        // 60 cau x 2 phut = 120, nhung tran lam them la 90.
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(45 + 90, b.phut)
        assertEquals(LuatCongGio.TOI_DA_MOI_NGAY, b.phut)
        assertTrue(b.dong.any { it.contains("tối đa 90 phút") })
    }

    @Test
    fun tran_lam_them_tru_ca_phan_da_cong_luc_truoc_trong_ngay() {
        val ket = KetQuaCham(
            cac = (1..10).map { cauNho("them$it", trongDanDo = false) },
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        // Chieu nay da cong 85 phut lam them roi, chi con 5.
        assertEquals(45 + 5, LuatCongGio.tinh(ket, 85, toiThuHai).phut)
    }

    @Test
    fun tron_goi_khong_bi_tran_lam_them_chan() {
        // Tran 90 chi ap cho phan le. Da dung het tran ma van lam het bai co giao
        // thi van duoc tron goi.
        val ket = KetQuaCham(
            cac = listOf(cauNho("a"), cauNho("b")),
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        assertEquals(45, LuatCongGio.tinh(ket, 90, toiThuHai).phut)
    }

    @Test
    fun khong_co_tron_goi_thi_khong_ap_tran() {
        // Chua chup vo dan do ma lam ca dong bai: day chinh la bai co giao, chan lai
        // la phat con vi lam nhieu.
        val ket = KetQuaCham(cac = (1..60).map { cauNho("c$it", trongDanDo = false) })
        assertEquals(120, LuatCongGio.tinh(ket, bayGio = toiThuHai).phut)
    }

    // --- tran duong on ---

    @Test
    fun on_lai_co_tran_rieng_moi_ngay() {
        // Hai muoi cau muoi dong: lan dau moi cau 5 phut, on lai duoc nua, tuc 3.
        // Sau muoi phut, nhung tran cua duong on la ba muoi.
        val ket = KetQuaCham(cac = (1..20).map { cauNho("on$it", soDong = 10) })
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai, onTap = true)
        assertEquals(LuatCongGio.TRAN_ON_MOI_NGAY, b.phut)
        assertTrue(b.dong.any { it.contains("Ôn lại hôm nay tối đa 30 phút") })
    }

    @Test
    fun tran_on_tru_ca_phan_da_on_luc_truoc_trong_ngay() {
        val ket = KetQuaCham(cac = (1..10).map { cauNho("on$it", soDong = 10) })
        // Chieu nay da on duoc 25 phut roi, chi con 5.
        assertEquals(5, LuatCongGio.tinh(ket, bayGio = toiThuHai, onTap = true, daCongOnHomNay = 25).phut)
    }

    @Test
    fun tran_on_ap_ca_vao_hom_khong_co_tron_goi() {
        // Khac han tran lam them: khong co goi thi phan tinh le chinh la bai co
        // giao nen khong chan: xem [khong_co_tron_goi_thi_khong_ap_tran]. Nhung on
        // lai thi khong bao gio la bai co giao - co giao khong giao lam lai bai cu.
        val ket = KetQuaCham(cac = (1..20).map { cauNho("on$it", soDong = 10) })
        // Hai muoi cau muoi dong: bai moi 5 phut mot cau, khong tran nao cat.
        assertEquals(100, LuatCongGio.tinh(ket, bayGio = toiThuHai).phut)
        // Cung xap do nop nhu bai on: 3 phut mot cau, roi tran cat con 30.
        assertEquals(30, LuatCongGio.tinh(ket, bayGio = toiThuHai, onTap = true).phut)
    }

    @Test
    fun bai_moi_khong_bi_tran_on_chan() {
        // On het ba muoi phut roi van lam bai moi binh thuong: hai duong, hai tran.
        val ket = KetQuaCham(cac = (1..10).map { cauNho("moi$it", soDong = 10) })
        assertEquals(50, LuatCongGio.tinh(ket, bayGio = toiThuHai, daCongOnHomNay = 30).phut)
    }

    @Test
    fun on_lai_van_bi_tran_lam_them_chan_khi_no_chat_hon() {
        val ket = KetQuaCham(
            cac = (1..20).map { cauNho("on$it", trongDanDo = false, soDong = 10) },
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        // Hom nay da lam them 88 phut: tran lam them chi con 2, chat hon tran on.
        // Cau bao cat phai goi ten dung cai tran vua cat.
        val b = LuatCongGio.tinh(ket, 88, toiThuHai, onTap = true)
        assertEquals(45 + 2, b.phut)
        assertTrue(b.dong.any { it.contains("Bài làm thêm hôm nay tối đa 90 phút") })
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

    @Test
    fun nop_lai_ma_co_bai_ngoai_vo_dan_do_thi_van_duoc_tinh_phan_do() {
        val ket = KetQuaCham(
            cac = listOf(cauNho("trong 1"), cauNho("trong 2"), cauNho("ngoài", trongDanDo = false)),
            lamHetDanDo = true
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai, goiDaCoHomNay = true)
        assertEquals(2, b.phut)
        assertEquals(listOf("ngoài"), b.phutCua.keys.toList())
    }

    @Test
    fun tran_lam_them_van_ap_khi_goi_da_tinh_tu_lan_nop_truoc() {
        // Tran 90 truoc day chi ap khi tron goi roi vao DUNG lan nop nay, nen lan
        // nop sau trong cung ngay khong con tran nao ca.
        val ket = KetQuaCham(cac = (1..60).map { cauNho("them$it", trongDanDo = false) })
        assertEquals(90, LuatCongGio.tinh(ket, bayGio = toiThuHai, goiDaCoHomNay = true).phut)
    }

    @Test
    fun phut_ghi_vao_so_dung_bang_phut_that_su_duoc_tra() {
        // So cai phai ghi con so DA TRA. Ghi gia niem yet cua cau thi ngay co tron
        // goi, so cong them 2 phut cho tung cau da nam trong goi, va tran lam them
        // cua hom do tu nhien hut di bay nhieu.
        val ket = KetQuaCham(
            cac = listOf(cauNho("trong"), cauNho("ngoài", trongDanDo = false)),
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        assertEquals(45 + 2, b.phut)
        assertEquals(mapOf("ngoài" to 2), b.phutCua)
        assertEquals(listOf("trong"), b.trongGoi.map { it.ma })
    }

    @Test
    fun bi_tran_cat_thi_so_ghi_dung_phan_con_lai() {
        val ket = KetQuaCham(
            cac = (1..10).map { cauNho("them$it", trongDanDo = false) },
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        // Con 5 phut tran: hai cau dau an het, cac cau sau khong duoc gi.
        val b = LuatCongGio.tinh(ket, 85, toiThuHai)
        assertEquals(45 + 5, b.phut)
        assertEquals(5, b.phutCua.values.sum())
        assertEquals(listOf("them1", "them2", "them3"), b.phutCua.keys.toList())
        assertEquals(1, b.phutCua["them3"])
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
        assertEquals(4, b.phut)
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
        // Duoi tran: so phut dung bang so cau. Truoc 23/9/2026 la bon cau mot phut.
        assertEquals(12, b.phut)
        assertTrue(b.dong.any { it.contains("12 câu trắc nghiệm đúng: +12 phút") })
    }

    @Test
    fun may_dem_qua_tay_thi_tran_moi_lan_nop_chan_lai() {
        // Ba lan chay tren dung nam tam anh KHTN ngay 14/9/2026 ra 36, 4, roi 22 cau.
        // Mot cau mot phut thi duoi tran so phut di theo so cau may dem; cai chan
        // phia tren la tran moi lan nop, khong con la ti le nua.
        fun phut(n: Int) =
            LuatCongGio.tinh(KetQuaCham(cac = (1..n).map { tracNghiem("c$it") }), bayGio = toiThuHai).phut
        assertEquals(LuatCongGio.TRAN_TRAC_NGHIEM, phut(36))
        assertEquals(4, phut(4))
        assertEquals(LuatCongGio.TRAN_TRAC_NGHIEM, phut(22))
    }

    @Test
    fun cum_trac_nghiem_co_tran_rieng_moi_lan_nop() {
        val ket = KetQuaCham(cac = (1..200).map { tracNghiem("c$it") })
        assertEquals(LuatCongGio.TRAN_TRAC_NGHIEM, LuatCongGio.tinh(ket, bayGio = toiThuHai).phut)
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
        // 8 cau khoanh ra 8 phut, bai tu luan 6 dong ra 3 phut.
        assertEquals(11, LuatCongGio.tinh(ket, bayGio = toiThuHai).phut)
    }

    @Test
    fun so_ghi_dung_tong_phut_cua_cum_trac_nghiem() {
        val ket = KetQuaCham(cac = (1..20).map { tracNghiem("c$it") })
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)
        // Hai muoi cau bi tran cat con 15 phut. So phai ghi du ca hai muoi cau (de
        // lan sau khong tinh lai) ma cong lai van dung 15 phut, khong phinh thanh 20.
        assertEquals(15, b.phut)
        assertEquals(20, b.phutCua.size)
        assertEquals(15, b.phutCua.values.sum())
    }

    @Test
    fun cum_trac_nghiem_cung_bi_tran_lam_them_chan() {
        val ket = KetQuaCham(
            cac = (1..40).map { tracNghiem("c$it") },
            ngayDanDo = "2026-09-14",
            baiDuocGiao = listOf("bài 2"),
            lamHetDanDo = true
        )
        // Con 3 phut tran lam them: cum 15 phut bi cat con 3.
        val b = LuatCongGio.tinh(ket, 87, toiThuHai)
        assertEquals(45 + 3, b.phut)
        assertEquals(3, b.phutCua.values.sum())
    }
}
