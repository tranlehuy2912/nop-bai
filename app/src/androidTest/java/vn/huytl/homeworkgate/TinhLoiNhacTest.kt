package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Buoi
import vn.huytl.homeworkgate.data.KhoaHoc
import vn.huytl.homeworkgate.data.NgayKhoa
import vn.huytl.homeworkgate.data.LoaiNhac
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.TinhLoiNhac
import java.util.Calendar

/**
 * Kiem tra phan tinh loi nhac. Day la cho de sai nhat vi no phu thuoc vao thu
 * trong tuan, gio trong ngay, va viec Le Hoa hoc buoi chieu nen "ngay mai" khong
 * co nghia gi ca.
 *
 * Cac moc lay theo lich that: nam 2026, thang 9. 14/9/2026 la thu hai.
 *
 * Tu 14/11/2026 gio chan va loi nhac sap di hoc theo gio Ba Huy dat cho tung thu (KhoaHoc), o
 * day la gia tri luc moi cai: thu hai, thu sau 8:45-17:00; thu ba, tu, nam, bay 11:00-17:00; chu
 * nhat khong khoa. Truoc do theo moc buong may trong lich (chieu 11:30, sang thu hai, thu sau
 * 8:45 toi 10:45).
 */
@RunWith(AndroidJUnit4::class)
class TinhLoiNhacTest {

    /** Tinh theo lich: chay tren ban lich dung yen, xem [LichMau]. */
    @get:Rule
    val lich = LichMau.Rule()

    private fun luc(ngay: Int, gio: Int, phut: Int): Calendar =
        NgayNghi.calendarCua(2026, 9, ngay, gio, phut)

    @Test
    fun ngay_14_9_2026_dung_la_thu_hai() {
        assertEquals(Calendar.MONDAY, luc(14, 8, 0).get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun toi_thu_hai_thi_nhac_soan_cho_chieu_thu_ba() {
        val nhac = TinhLoiNhac.tinh(luc(14, 20, 0), emptySet())
        assertNotNull(nhac)
        assertEquals(LoaiNhac.SOAN_VO, nhac!!.loai)
        assertEquals(Calendar.TUESDAY, nhac.buoi!!.thu)
        assertEquals(Buoi.CHIEU, nhac.buoi!!.buoi)
    }

    @Test
    fun sang_thu_ba_van_nhac_soan_cho_chieu_thu_ba() {
        // Le Hoa hoc chieu nen sang hom do van con kip soan, khong duoc im lang.
        val nhac = TinhLoiNhac.tinh(luc(15, 8, 0), emptySet())
        assertEquals(LoaiNhac.SOAN_VO, nhac!!.loai)
        assertEquals(Calendar.TUESDAY, nhac.buoi!!.thu)
    }

    @Test
    fun da_soan_roi_thi_khong_nhac_nua() {
        val nhac = TinhLoiNhac.tinh(luc(15, 8, 0), emptySet())!!
        val imLang = TinhLoiNhac.tinh(luc(15, 8, 0), setOf(nhac.maBuoi!!))
        assertNull(imLang)
    }

    @Test
    fun mot_gio_truoc_gio_khoa_thi_nhac_di_hoc() {
        // Thu ba khoa luc 11h00, nen 10h30 phai bao con 30 phut, kem gio vao hoc chieu.
        val nhac = TinhLoiNhac.tinh(luc(15, 10, 30), emptySet())
        assertEquals(LoaiNhac.SAP_DI_HOC, nhac!!.loai)
        assertTrue(nhac.tieuDe.contains("30 phút"))
        assertTrue(!nhac.gap)
        assertEquals("Vào học lúc 12:45, chiều thứ ba", nhac.chiTiet)
    }

    @Test
    fun con_muoi_phut_thi_thanh_gap() {
        val nhac = TinhLoiNhac.tinh(luc(15, 10, 50), emptySet())
        assertEquals(LoaiNhac.SAP_DI_HOC, nhac!!.loai)
        assertTrue(nhac.gap)
    }

    @Test
    fun vao_nguong_gap_thi_tieu_de_hien_phut_giay() {
        // 10h50 con dung 10 phut, phai hien 10:00 chu khong phai "10 phut".
        val nhac = TinhLoiNhac.tinh(luc(15, 10, 50), emptySet())!!
        assertTrue(nhac.tieuDe.contains("10:00"))
    }

    @Test
    fun dinh_dang_phut_giay_dung() {
        assertEquals("8:32", TinhLoiNhac.dinhDangPhutGiay(512))
        assertEquals("0:07", TinhLoiNhac.dinhDangPhutGiay(7))
        assertEquals("14:00", TinhLoiNhac.dinhDangPhutGiay(840))
    }

    @Test
    fun qua_gio_khoa_thi_thoi_bao_sap_di_hoc() {
        // Truoc 14/11/2026 qua moc buong may ma chua vao hoc van bao "Tới giờ chuẩn bị đi học".
        // Gio qua gio khoa la man chan lo; tat man chan thi khong bao sap di hoc nua.
        val nhac = TinhLoiNhac.tinh(luc(15, 12, 30), emptySet())
        assertTrue(nhac == null || nhac.loai != LoaiNhac.SAP_DI_HOC)
    }

    @Test
    fun dang_trong_tiet_thi_bao_dang_gio_hoc() {
        val nhac = TinhLoiNhac.tinh(luc(15, 13, 0), emptySet())
        assertEquals(LoaiNhac.DANG_GIO_HOC, nhac!!.loai)
    }

    @Test
    fun thu_hai_sang_va_chieu_la_hai_buoi_rieng() {
        // 8h sang thu hai: con 45 phut toi gio khoa 8h45, buoi trong khoang la buoi sang.
        val sang = TinhLoiNhac.tinh(luc(14, 8, 0), emptySet())!!
        assertEquals(LoaiNhac.SAP_DI_HOC, sang.loai)
        assertEquals(Buoi.SANG, sang.buoi!!.buoi)

        // 11h sang thu hai, sang da hoc xong luc 10h45: dang chan thi man chan ke buoi chieu.
        val chieu = TinhLoiNhac.tinh(luc(14, 11, 0), emptySet(), batManChan = true)!!
        assertEquals(LoaiNhac.CHAN, chieu.loai)
        assertEquals(Buoi.CHIEU, chieu.buoi!!.buoi)
    }

    @Test
    fun toi_thu_bay_thi_nhay_qua_chu_nhat_toi_thu_hai() {
        // 19/9/2026 la thu bay. Buoi ke tiep la sang thu hai, con soan tap thi cho chieu thu hai.
        val ke = TinhLoiNhac.buoiKeTiep(luc(19, 20, 0))!!
        assertEquals(Calendar.MONDAY, ke.second.thu)
        assertEquals(Buoi.SANG, ke.second.buoi)
        val nhac = TinhLoiNhac.tinh(luc(19, 20, 0), emptySet())!!
        assertEquals(LoaiNhac.SOAN_VO, nhac.loai)
        assertEquals(Calendar.MONDAY, nhac.buoi!!.thu)
        assertEquals(Buoi.CHIEU, nhac.buoi!!.buoi)
    }

    @Test
    fun sang_thu_hai_khong_co_mon_nao_can_soan_vo() {
        // Sang thu hai chi co the duc, khong mang vo nhung phai mac do.
        val sang = TinhLoiNhac.buoiKeTiep(luc(14, 6, 0))!!.second
        assertEquals(Buoi.SANG, sang.buoi)
        assertEquals(0, sang.monCanSoan.size)
        assertTrue(sang.coTheDuc)
    }

    @Test
    fun chieu_thu_hai_bo_chao_co_ra_khoi_danh_sach_soan() {
        val nhac = TinhLoiNhac.tinh(luc(14, 11, 0), emptySet())!!
        val mon = nhac.buoi!!.monCanSoan
        assertTrue(mon.none { it == "Chào cờ" })
        assertTrue(mon.contains("Tiếng Anh"))
    }

    @Test
    fun sang_thu_sau_chi_con_tin_hoc_tu_tiet_3() {
        // Tu 27/9/2026 sang thu sau khong con hai tiet AVNN o tiet 1 va 2. Toi thu nam
        // 17/9/2026 thi buoi ke tiep la sang thu sau, vao hoc luc 9h15.
        val sang = TinhLoiNhac.buoiKeTiep(luc(17, 20, 0))!!.second
        assertEquals(Calendar.FRIDAY, sang.thu)
        assertEquals(Buoi.SANG, sang.buoi)
        assertEquals(listOf("Tin học"), sang.monCanSoan)
        assertEquals(9 * 60 + 15, sang.phutVaoHoc)
    }

    /**
     * Buoi sang khong nhac soan tap (Ba Huy chot 2/10/2026): sang thu hai chi the duc, sang thu
     * sau chi Tin hoc. Loi nhac soan, man Soan tap va dong o man chinh deu nham buoi chieu.
     */
    @Test
    fun buoi_sang_khong_nhac_soan_tap_ma_nhac_cho_buoi_chieu() {
        // Toi thu nam: buoi ke tiep la sang thu sau (Tin hoc), soan tap cho chieu thu sau.
        val thuSau = TinhLoiNhac.tinh(luc(17, 20, 0), emptySet())!!
        assertEquals(LoaiNhac.SOAN_VO, thuSau.loai)
        assertEquals("Soạn tập cho chiều thứ sáu", thuSau.tieuDe)
        assertEquals(Buoi.CHIEU, thuSau.buoi!!.buoi)
        assertTrue(thuSau.maBuoi!!.endsWith("-CHIEU"))
        val ca = TinhLoiNhac.buoiCanSoan(luc(17, 20, 0))!!
        assertEquals(Calendar.FRIDAY, ca.second.thu)
        assertEquals(Buoi.CHIEU, ca.second.buoi)

        // Soan roi cho chieu thu sau thi im lang, du sang thu sau chua hoc.
        assertNull(TinhLoiNhac.tinh(luc(17, 20, 0), setOf(thuSau.maBuoi!!)))

        // Dang hoc chieu thu hai (14h): buoi can soan la chieu thu ba.
        assertEquals(Calendar.TUESDAY, TinhLoiNhac.buoiCanSoan(luc(14, 14, 0))!!.second.thu)
    }

    @Test
    fun ngay_le_thi_khong_tinh_la_ngay_hoc() {
        // 1/1/2027 la Tet Duong lich.
        val tet = NgayNghi.calendarCua(2027, 1, 1, 10, 0)
        assertEquals("Tết Dương lịch", NgayNghi.tenKyNghi(tet))
    }

    @Test
    fun trong_ky_nghi_tet_thi_buoi_ke_tiep_nhay_qua_het_ky_nghi() {
        // 3/2/2027 nam trong ky nghi Tet.
        val trongTet = NgayNghi.calendarCua(2027, 2, 3, 10, 0)
        val ke = TinhLoiNhac.buoiKeTiep(trongTet)
        assertNotNull(ke)
        assertTrue(NgayNghi.tenKyNghi(ke!!.first) == null)
    }

    @Test
    fun sang_thu_hai_the_duc_khong_bao_chuan_bi_do_ma_nhac_soan_chieu() {
        // Truoc 2/10/2026 6h sang thu hai bao "Sáng thứ hai có tiết học thể dục / Chuẩn bị đồ
        // thể dục". Nay buoi sang khong nhac soan: 6h sang thu hai nhac soan cho chieu thu hai.
        val nhac = TinhLoiNhac.tinh(luc(14, 6, 0), emptySet())!!
        assertEquals(LoaiNhac.SOAN_VO, nhac.loai)
        assertEquals("Soạn tập cho chiều thứ hai", nhac.tieuDe)
        assertTrue(!nhac.chiTiet.contains("thể dục"))
    }

    @Test
    fun buoi_co_ca_mon_va_the_duc_thi_nhac_ca_hai() {
        // Chieu thu tu co 4 mon can vo, khong co the duc. Kiem de chac rang phan "va
        // nho do the duc" chi hien khi dung co the duc.
        //
        // Truoc day kiem chieu thu hai, vi sang cung ngay co the duc. Tu khi buoi
        // chieu buong may luc 11h30 thi chieu thu hai khong con luc nao nhac soan vo:
        // sang hoc toi 10h45, ma 10h30 da vao nguong nhac di hoc. Nen kiem toi thu
        // ba, luc buoi ke tiep la chieu thu tu.
        val nhac = TinhLoiNhac.tinh(luc(15, 20, 0), emptySet())!!
        assertEquals(LoaiNhac.SOAN_VO, nhac.loai)
        assertTrue(nhac.chiTiet.contains("4 môn"))
        assertTrue(!nhac.chiTiet.contains("thể dục"))
    }

    /**
     * Loi nhac soan tap ke kem so bai trong vo dan do han dung buoi do (30/9/2026), xem
     * NhacBai. Khong co bai thi giu nguyen cau cu.
     */
    @Test
    fun soan_tap_ke_kem_so_bai_phai_lam_cho_buoi_do() {
        val nhac = TinhLoiNhac.tinh(luc(15, 20, 0), emptySet(), soBaiCho = { cal, buoi ->
            if (cal.get(Calendar.DAY_OF_MONTH) == 16 && buoi.buoi == Buoi.CHIEU) 2 else 0
        })!!
        assertEquals("4 môn cần mang, 2 bài phải làm", nhac.chiTiet)
        assertEquals("4 môn cần mang", TinhLoiNhac.tinh(luc(15, 20, 0), emptySet())!!.chiTiet)
    }

    // ---- man chan ----

    private fun chan(cal: Calendar, moSom: String = "") =
        TinhLoiNhac.tinh(cal, emptySet(), batManChan = true, buoiDuocMoSom = moSom)

    @Test
    fun trong_gio_hoc_chieu_thi_chan_man_hinh() {
        // Chieu thu hai chan tu 11h30 toi 17h00.
        val nhac = chan(luc(14, 12, 30))!!
        assertEquals(LoaiNhac.CHAN, nhac.loai)
        assertEquals(17 * 60, nhac.phutHetChan)
    }

    @Test
    fun truoc_gio_khoa_thi_chua_chan() {
        // Thu ba khoa tu 11h00.
        assertTrue(chan(luc(15, 10, 55))!!.loai != LoaiNhac.CHAN)
        assertEquals(LoaiNhac.CHAN, chan(luc(15, 11, 0))!!.loai)
    }

    @Test
    fun tan_hoc_roi_thi_thoi_chan() {
        assertTrue(chan(luc(14, 17, 30))?.loai != LoaiNhac.CHAN)
    }

    @Test
    fun sang_thu_hai_khoa_lien_toi_17h() {
        // Thu hai khoa mot mach 8h45-17h00 (anh Huy chot 14/11/2026), man chan ke buoi sang.
        val nhac = chan(luc(14, 9, 30))!!
        assertEquals(LoaiNhac.CHAN, nhac.loai)
        assertEquals(17 * 60, nhac.phutHetChan)
        assertEquals(Buoi.SANG, nhac.buoi!!.buoi)
    }

    @Test
    fun giua_hai_buoi_thu_hai_van_chan() {
        // Truoc 14/11/2026 11h00 thu hai mo 45 phut giua hai buoi. Gio khoa lien.
        val nhac = chan(luc(14, 11, 0))!!
        assertEquals(LoaiNhac.CHAN, nhac.loai)
        assertEquals(Buoi.CHIEU, nhac.buoi!!.buoi)
    }

    @Test
    fun sang_thu_sau_chan_tu_8h45_toi_17h() {
        // 18/9/2026 la thu sau. Luc 7h15, truoc day la gio vao tiet AVNN, nay chua chan.
        assertTrue(chan(luc(18, 7, 15))?.loai != LoaiNhac.CHAN)
        assertTrue(chan(luc(18, 8, 40))?.loai != LoaiNhac.CHAN)
        val nhac = chan(luc(18, 8, 45))!!
        assertEquals(LoaiNhac.CHAN, nhac.loai)
        assertEquals(17 * 60, nhac.phutHetChan)
    }

    @Test
    fun tat_cong_tac_thi_khong_chan_nua() {
        val nhac = TinhLoiNhac.tinh(luc(14, 12, 30), emptySet(), batManChan = false)
        assertTrue(nhac == null || nhac.loai != LoaiNhac.CHAN)
    }

    @Test
    fun ba_huy_mo_som_thi_buoi_do_thoi_chan() {
        val dangChan = chan(luc(14, 12, 30))!!
        val sauKhiMo = chan(luc(14, 12, 30), moSom = dangChan.maBuoi!!)
        assertTrue(sauKhiMo == null || sauKhiMo.loai != LoaiNhac.CHAN)
    }

    @Test
    fun mo_som_buoi_nay_khong_lam_buoi_sau_het_chan() {
        // Mo som cho chieu thu hai, roi kiem chieu thu ba: phai chan lai binh thuong.
        val maChieuThuHai = chan(luc(14, 12, 30))!!.maBuoi!!
        val chieuThuBa = chan(luc(15, 12, 30), moSom = maChieuThuHai)!!
        assertEquals(LoaiNhac.CHAN, chieuThuBa.loai)
    }

    @Test
    fun ngay_nghi_thi_khong_chan() {
        val tetDuong = NgayNghi.calendarCua(2027, 1, 1, 13, 0)
        val nhac = TinhLoiNhac.tinh(tetDuong, emptySet(), batManChan = true)
        assertTrue(nhac == null || nhac.loai != LoaiNhac.CHAN)
    }

    @Test
    fun vao_nguong_gap_thi_dem_tung_giay() {
        // Thu ba 10h50 con 10 phut toi gio khoa 11h00, tuc 600 giay.
        val nhac = TinhLoiNhac.tinh(luc(15, 10, 50), emptySet())!!
        assertEquals(LoaiNhac.SAP_DI_HOC, nhac.loai)
        assertEquals(600, nhac.giayConLai)
    }

    @Test
    fun chua_vao_nguong_gap_thi_khong_dem_giay() {
        assertEquals(-1, TinhLoiNhac.tinh(luc(15, 10, 30), emptySet())!!.giayConLai)
    }

    // ---- gio di hoc Ba Huy dat (14/11/2026) ----

    private fun voiKhoa(thu: Int, k: NgayKhoa, viec: () -> Unit) {
        val cu = KhoaHoc.cauHinh
        KhoaHoc.cauHinh = { KhoaHoc.MAC_DINH + (thu to k) }
        try { viec() } finally { KhoaHoc.cauHinh = cu }
    }

    @Test
    fun tat_khoa_thu_ba_thi_khong_chan_khong_bao_sap_di_hoc() {
        voiKhoa(Calendar.TUESDAY, NgayKhoa(false, 11 * 60, 17 * 60)) {
            assertTrue(chan(luc(15, 12, 30))?.loai != LoaiNhac.CHAN)
            assertTrue(TinhLoiNhac.tinh(luc(15, 10, 50), emptySet())?.loai != LoaiNhac.SAP_DI_HOC)
        }
    }

    @Test
    fun doi_gio_khoa_thi_chan_theo_gio_moi() {
        voiKhoa(Calendar.TUESDAY, NgayKhoa(true, 12 * 60, 16 * 60)) {
            assertTrue(chan(luc(15, 11, 30))?.loai != LoaiNhac.CHAN)
            val nhac = chan(luc(15, 12, 0))!!
            assertEquals(LoaiNhac.CHAN, nhac.loai)
            assertEquals(16 * 60, nhac.phutHetChan)
            assertTrue(chan(luc(15, 16, 0))?.loai != LoaiNhac.CHAN)
        }
    }

    @Test
    fun bat_khoa_chu_nhat_thi_chu_nhat_cung_chan() {
        // 20/9/2026 la chu nhat: lich khong co buoi nao, man chan chi noi gio mo lai.
        voiKhoa(Calendar.SUNDAY, NgayKhoa(true, 9 * 60, 11 * 60)) {
            val nhac = chan(luc(20, 10, 0))!!
            assertEquals(LoaiNhac.CHAN, nhac.loai)
            assertNull(nhac.buoi)
            assertEquals("Máy khoá tới 11:00", nhac.chiTiet)
            assertEquals(11 * 60, nhac.phutHetChan)
        }
        assertTrue(chan(luc(20, 10, 0))?.loai != LoaiNhac.CHAN)
    }

    @Test
    fun mo_som_la_mo_ca_ngay_khoa() {
        val ma = chan(luc(14, 9, 0))!!.maBuoi!!
        assertEquals(TinhLoiNhac.maKhoa(luc(14, 9, 0)), ma)
        assertTrue(chan(luc(14, 15, 0), moSom = ma)?.loai != LoaiNhac.CHAN)
    }

    @Test
    fun lenh_khoa_hoc_doc_va_chan_gio_sai() {
        val (moi, loi) = KhoaHoc.tuLenh(
            mapOf("3" to mapOf("bat" to true, "tu" to 600L, "toi" to 900L), "1" to mapOf("bat" to true)),
            KhoaHoc.MAC_DINH
        )
        assertNull(loi)
        assertEquals(NgayKhoa(true, 600, 900), moi!![Calendar.TUESDAY])
        assertEquals(NgayKhoa(true, 11 * 60, 17 * 60), moi[Calendar.SUNDAY])
        assertEquals(KhoaHoc.MAC_DINH[Calendar.MONDAY], moi[Calendar.MONDAY])

        val (sai, lyDo) = KhoaHoc.tuLenh(
            mapOf("2" to mapOf("bat" to true, "tu" to 900L, "toi" to 600L)), KhoaHoc.MAC_DINH
        )
        assertNull(sai)
        assertEquals("Thứ hai: giờ mở lại phải sau giờ khoá.", lyDo)

        assertEquals(KhoaHoc.MAC_DINH, KhoaHoc.docJson(KhoaHoc.thanhJson(KhoaHoc.MAC_DINH)))
        assertEquals(KhoaHoc.MAC_DINH, KhoaHoc.docJson("hong"))
    }
}
