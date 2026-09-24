package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Buoi
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
 */
@RunWith(AndroidJUnit4::class)
class TinhLoiNhacTest {

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
    fun mot_gio_truoc_moc_buong_may_thi_nhac_di_hoc() {
        // Chieu thu ba buong may luc 11h30, nen 11h00 phai bao con 30 phut.
        val nhac = TinhLoiNhac.tinh(luc(15, 11, 0), emptySet())
        assertEquals(LoaiNhac.SAP_DI_HOC, nhac!!.loai)
        assertTrue(nhac.tieuDe.contains("30 phút"))
        assertTrue(!nhac.gap)
    }

    @Test
    fun con_muoi_phut_thi_thanh_gap() {
        val nhac = TinhLoiNhac.tinh(luc(15, 11, 20), emptySet())
        assertEquals(LoaiNhac.SAP_DI_HOC, nhac!!.loai)
        assertTrue(nhac.gap)
    }

    @Test
    fun vao_nguong_gap_thi_tieu_de_hien_phut_giay() {
        // 11h20 con dung 10 phut, phai hien 10:00 chu khong phai "10 phut".
        val nhac = TinhLoiNhac.tinh(luc(15, 11, 20), emptySet())!!
        assertTrue(nhac.tieuDe.contains("10:00"))
    }

    @Test
    fun dinh_dang_phut_giay_dung() {
        assertEquals("8:32", TinhLoiNhac.dinhDangPhutGiay(512))
        assertEquals("0:07", TinhLoiNhac.dinhDangPhutGiay(7))
        assertEquals("14:00", TinhLoiNhac.dinhDangPhutGiay(840))
    }

    @Test
    fun qua_moc_buong_may_van_con_nhac_va_van_gap() {
        val nhac = TinhLoiNhac.tinh(luc(15, 12, 30), emptySet())
        assertEquals(LoaiNhac.SAP_DI_HOC, nhac!!.loai)
        assertTrue(nhac.gap)
        assertTrue(nhac.tieuDe.contains("Tới giờ"))
    }

    @Test
    fun dang_trong_tiet_thi_bao_dang_gio_hoc() {
        val nhac = TinhLoiNhac.tinh(luc(15, 13, 0), emptySet())
        assertEquals(LoaiNhac.DANG_GIO_HOC, nhac!!.loai)
    }

    @Test
    fun thu_hai_sang_va_chieu_la_hai_buoi_rieng() {
        // 7h sang thu hai: buoi ke tiep la sang thu hai, buong may luc 8h45.
        val sang = TinhLoiNhac.tinh(luc(14, 8, 0), emptySet())!!
        assertEquals(Buoi.SANG, sang.buoi!!.buoi)

        // 11h sang thu hai, sang da hoc xong luc 10h45, ke tiep la chieu.
        val chieu = TinhLoiNhac.tinh(luc(14, 11, 0), emptySet())!!
        assertEquals(Buoi.CHIEU, chieu.buoi!!.buoi)
    }

    @Test
    fun toi_thu_bay_thi_nhay_qua_chu_nhat_toi_sang_thu_hai() {
        // 19/9/2026 la thu bay.
        val nhac = TinhLoiNhac.tinh(luc(19, 20, 0), emptySet())!!
        assertEquals(Calendar.MONDAY, nhac.buoi!!.thu)
        assertEquals(Buoi.SANG, nhac.buoi!!.buoi)
    }

    @Test
    fun sang_thu_hai_khong_co_mon_nao_can_soan_vo() {
        // Sang thu hai chi co the duc, khong mang vo nhung phai mac do.
        val nhac = TinhLoiNhac.tinh(luc(14, 6, 0), emptySet())!!
        assertEquals(0, nhac.buoi!!.monCanSoan.size)
        assertTrue(nhac.buoi!!.coTheDuc)
    }

    @Test
    fun chieu_thu_hai_bo_chao_co_ra_khoi_danh_sach_soan() {
        val nhac = TinhLoiNhac.tinh(luc(14, 11, 0), emptySet())!!
        val mon = nhac.buoi!!.monCanSoan
        assertTrue(mon.none { it == "Chào cờ" })
        assertTrue(mon.contains("Tiếng Anh"))
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
    fun buoi_chi_co_the_duc_thi_khong_bao_soan_vo() {
        // Sang thu hai chi co the duc. Bao "soan tap vo" roi them "khong phai
        // mang vo" thi doc nhu app hong, nen phai noi thang la chuan bi do.
        val nhac = TinhLoiNhac.tinh(luc(14, 6, 0), emptySet())!!
        assertEquals(LoaiNhac.SOAN_VO, nhac.loai)
        assertTrue(nhac.tieuDe.contains("tiết học thể dục"))
        assertTrue(!nhac.tieuDe.contains("Soạn tập"))
        assertEquals("Chuẩn bị đồ thể dục", nhac.chiTiet)
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
    fun truoc_moc_buong_may_thi_chua_chan() {
        assertTrue(chan(luc(14, 11, 25))!!.loai != LoaiNhac.CHAN)
    }

    @Test
    fun tan_hoc_roi_thi_thoi_chan() {
        assertTrue(chan(luc(14, 17, 30))?.loai != LoaiNhac.CHAN)
    }

    @Test
    fun sang_thu_hai_cung_chan_rieng_mot_khoang() {
        // Sang thu hai chan tu 8h45 toi 10h45, khong dinh gi toi buoi chieu.
        val nhac = chan(luc(14, 9, 30))!!
        assertEquals(LoaiNhac.CHAN, nhac.loai)
        assertEquals(10 * 60 + 45, nhac.phutHetChan)
    }

    @Test
    fun giua_hai_buoi_thu_hai_thi_khong_chan() {
        // 11h00: sang da tan luc 10h45, chieu chua toi moc 11h30. Con ve nha an trua.
        assertTrue(chan(luc(14, 11, 0))!!.loai != LoaiNhac.CHAN)
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
        // 11h20 con 10 phut toi moc 11h30, tuc 600 giay.
        val nhac = TinhLoiNhac.tinh(luc(14, 11, 20), emptySet())!!
        assertEquals(LoaiNhac.SAP_DI_HOC, nhac.loai)
        assertEquals(600, nhac.giayConLai)
    }

    @Test
    fun chua_vao_nguong_gap_thi_khong_dem_giay() {
        assertEquals(-1, TinhLoiNhac.tinh(luc(14, 11, 0), emptySet())!!.giayConLai)
    }
}
