package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.guard.CachXet
import vn.huytl.homeworkgate.guard.LuatManHinh
import vn.huytl.homeworkgate.guard.LuatManHinh.TieuDiem

/**
 * Doc danh sach cua so ra cach xet.
 *
 * Duong di dan den bo test nay: Le Hoa keo thanh thong bao xuong mot chut roi giu
 * yen ngon tay. Guard tuong thanh dang mo nen thoi xet, con YouTube ben duoi van
 * hien gan tron. Thu tren may ao Android 13: trong luc giu, danh sach cua so chi con
 * moi cai thanh do, YouTube khong co trong danh sach.
 */
@RunWith(AndroidJUnit4::class)
class LuatManHinhTest {

    private val youtube = "com.google.android.youtube"
    private val nhaMay = "com.google.android.apps.nexuslauncher"

    /** Thanh thong bao keo xuong: cua so cua he thong, phu kin man hinh. */
    private val thanhThongBao = TieuDiem(cuaMinh = false, phuKin = true)

    @Test
    fun tieu_diem_o_app_thi_xet_binh_thuong() {
        val cach = LuatManHinh.cachXet(null, manKhoa = false, docDuocApp = true)
        assertEquals(CachXet.XET, cach)
        assertEquals(setOf(youtube), LuatManHinh.appDangHien(cach, setOf(youtube), setOf(nhaMay)))
    }

    @Test
    fun giu_thanh_thong_bao_sat_dau_thi_van_xet_app_ben_duoi() {
        val cach = LuatManHinh.cachXet(thanhThongBao, manKhoa = false, docDuocApp = false)
        assertEquals(CachXet.DUOI_THONG_BAO, cach)
        assertEquals(setOf(youtube), LuatManHinh.appDangHien(cach, emptySet(), setOf(youtube)))
    }

    /** Keo mot thong bao ra cua so noi: app mo len ngay duoi thanh, danh sach khong thay. */
    @Test
    fun app_vua_mo_len_duoi_thanh_thong_bao_thi_them_vao() {
        val cach = LuatManHinh.cachXet(thanhThongBao, manKhoa = false, docDuocApp = false)
        assertEquals(
            setOf(nhaMay, youtube),
            LuatManHinh.appDangHien(cach, emptySet(), setOf(nhaMay), goiSuKienMoi = youtube)
        )
    }

    /** Thanh da dong thi danh sach doc duoc la dung nhat, khong them gi. */
    @Test
    fun khong_phu_thi_khong_them_app_cua_su_kien() {
        assertEquals(
            setOf(nhaMay),
            LuatManHinh.appDangHien(CachXet.XET, setOf(nhaMay), emptySet(), goiSuKienMoi = youtube)
        )
    }

    /** May khac co the van bao app nam duoi thanh. Doc duoc thi tin cai doc duoc. */
    @Test
    fun thanh_thong_bao_ma_van_doc_duoc_app_thi_lay_cai_doc_duoc() {
        val cach = LuatManHinh.cachXet(thanhThongBao, manKhoa = false, docDuocApp = true)
        assertEquals(CachXet.DUOI_THONG_BAO, cach)
        assertEquals(setOf(youtube), LuatManHinh.appDangHien(cach, setOf(youtube), setOf(nhaMay)))
    }

    /** Man chan gio hoc cua chinh app nay giu tieu diem de nuot nut Back. */
    @Test
    fun man_chan_cua_minh_thi_bo_qua_nhu_cu() {
        val manChan = TieuDiem(cuaMinh = true, phuKin = true)
        assertEquals(CachXet.BO_QUA, LuatManHinh.cachXet(manChan, manKhoa = false, docDuocApp = false))
    }

    /** Man khoa ve trong cua so cua thanh thong bao, nhung khong co app nao xem duoc ben duoi. */
    @Test
    fun man_hinh_khoa_thi_bo_qua_nhu_cu() {
        assertEquals(CachXet.BO_QUA, LuatManHinh.cachXet(thanhThongBao, manKhoa = true, docDuocApp = false))
    }

    /** Thanh keo tren dau cua so noi: cham vao no khong duoc lam guard thoi chan. */
    @Test
    fun cua_so_he_thong_nho_ma_van_thay_app_thi_xet_binh_thuong() {
        val thanhNho = TieuDiem(cuaMinh = false, phuKin = false)
        assertEquals(CachXet.XET, LuatManHinh.cachXet(thanhNho, manKhoa = false, docDuocApp = true))
    }

    /** Hep ma van giau het app thi van dang phu, du no rong bao nhieu. */
    @Test
    fun cua_so_he_thong_nho_ma_giau_het_app_thi_coi_nhu_dang_phu() {
        val thanhNho = TieuDiem(cuaMinh = false, phuKin = false)
        val cach = LuatManHinh.cachXet(thanhNho, manKhoa = false, docDuocApp = false)
        assertEquals(CachXet.DUOI_THONG_BAO, cach)
        assertEquals(setOf(youtube), LuatManHinh.appDangHien(cach, emptySet(), setOf(youtube)))
    }

    /**
     * Khong co thanh nao phu ma van khong doc duoc app, nhu luc he thong khong cho doc:
     * khong lay cai nho lai. Ham xet cua GuardAccessibilityService da co duong rieng
     * cho luc mu.
     */
    @Test
    fun khong_phu_ma_khong_doc_duoc_thi_khong_lay_cai_nho_lai() {
        val cach = LuatManHinh.cachXet(null, manKhoa = false, docDuocApp = false)
        assertEquals(CachXet.XET, cach)
        assertEquals(emptySet<String>(), LuatManHinh.appDangHien(cach, emptySet(), setOf(youtube)))
    }
}
