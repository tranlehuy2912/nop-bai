package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.kho.TheHoc

/**
 * Luat cham cua duong hoc thuoc.
 *
 * Khong dung den may hay mang, nhung de o day cho cung cho voi cac bo test kia.
 * Bo nay KHONG xoa prefs.
 *
 * Phan dang test ky nhat la [HocThuoc.chuanHoa], vi no la cho quyet dinh con bi
 * bao sai oan hay khong. Moi cho no CHO DI deu co mot test, va moi cho no KHONG
 * cho di cung vay - cai thu hai moi quan trong: bo nham dau tieng Viet thi ca bo
 * the tu vung tro thanh vo nghia ma khong ai bao loi.
 */
@RunWith(AndroidJUnit4::class)
class HocThuocTest {

    private fun the(dap: String, vararg khac: String) = TheHoc(
        id = "thu:1", mon = "Toán", bo = "thu", bai = "bài thử",
        hoi = "hỏi", dap = dap, dapKhac = khac.toList()
    )

    // --- nhung cho chuan hoa CHO DI ---

    @Test
    fun chu_hoa_chu_thuong_la_mot() {
        assertTrue(HocThuoc.dung("A Book", the("a book")))
    }

    @Test
    fun thua_thieu_dau_cach_van_dung() {
        assertTrue(HocThuoc.dung("a²+2ab+b²", the("a² + 2ab + b²")))
        assertTrue(HocThuoc.dung("  a² + 2ab + b²  ", the("a² + 2ab + b²")))
    }

    @Test
    fun go_mu_kieu_ban_phim_thuong_van_dung() {
        assertTrue(HocThuoc.dung("a^2 + 2ab + b^2", the("a² + 2ab + b²")))
        assertTrue(HocThuoc.dung("a^3 - b^3", the("a³ − b³")))
    }

    @Test
    fun ba_loai_gach_ngang_la_mot() {
        // Dau tru toan hoc, gach ngang ngan, gach ngang dai, va dau tru ban phim.
        assertTrue(HocThuoc.dung("a - b", the("a − b")))
        assertTrue(HocThuoc.dung("a – b", the("a − b")))
        assertTrue(HocThuoc.dung("a — b", the("a − b")))
    }

    @Test
    fun dau_cham_cuoi_cau_khong_tinh() {
        assertTrue(HocThuoc.dung("a book.", the("a book")))
    }

    // --- nhung cho chuan hoa KHONG cho di ---

    @Test
    fun thieu_dau_tieng_viet_la_sai() {
        assertFalse(HocThuoc.dung("ma", the("mà")))
    }

    @Test
    fun sai_chinh_ta_tieng_anh_la_sai() {
        assertFalse(HocThuoc.dung("libary", the("library")))
    }

    @Test
    fun thieu_mot_hang_tu_la_sai() {
        assertFalse(HocThuoc.dung("a² + b²", the("a² + 2ab + b²")))
    }

    @Test
    fun de_trong_la_sai() {
        assertFalse(HocThuoc.dung("", the("a book")))
        assertFalse(HocThuoc.dung("   ", the("a book")))
    }

    // --- dap an khac ---

    @Test
    fun ban_viet_khac_ke_trong_file_cung_tinh_dung() {
        val t = the("(a − b)(a + b)", "(a + b)(a − b)")
        assertTrue(HocThuoc.dung("(a + b)(a − b)", t))
        assertTrue(HocThuoc.dung("(a-b)(a+b)", t))
    }

    @Test
    fun ban_viet_khong_ke_trong_file_thi_khong_tinh() {
        // Doi cho hai hang tu la dung ve toan, nhung the nay khong khai ra nen may
        // khong tu doan. Muon tinh thi them vao dap_khac trong file.
        assertFalse(HocThuoc.dung("b² + 2ab + a²", the("a² + 2ab + b²")))
    }

    // --- so phut ---

    @Test
    fun mot_the_dung_duoc_mot_phut() {
        assertEquals(120, HocThuoc.giayCho(2))
        assertEquals(720, HocThuoc.giayCho(12))
        // Moi the tron mot phut, khong con nua phut de danh nhu truoc 23/9/2026.
        assertEquals(1, HocThuoc.phutThem(0, HocThuoc.giayCho(1)))
        assertEquals(3, HocThuoc.phutThem(0, HocThuoc.giayCho(3)))
        assertEquals(12, HocThuoc.phutThem(0, HocThuoc.giayCho(12)))
    }

    @Test
    fun khong_dung_the_nao_thi_khong_co_phut() {
        assertEquals(0, HocThuoc.giayCho(0))
        assertEquals(0, HocThuoc.giayCho(-1))
        assertEquals(0, HocThuoc.phutThem(0, 0))
    }

    @Test
    fun le_nua_phut_nam_lai_trong_ngay_chu_khong_mat() {
        // Tu 23/9/2026 moi the tron mot phut, phan le chi con tu nhung luot ghi theo
        // gia cu. Ham van phai giu le trong ngay: ba luot 90 giay, chia rieng thi moi
        // luot mot phut, tong ba; tinh don ca ngay la bon phut ruoi, duoc bon.
        var giay = 0
        var tong = 0
        repeat(3) {
            tong += HocThuoc.phutThem(giay, 90)
            giay += 90
        }
        assertEquals(4, tong)
    }

    @Test
    fun tran_ngay_cat_bot_chu_khong_tu_choi_ca_luot() {
        // Hom nay da lam ra 18 phut, tran la 20: luot nay dang duoc 12 phut, chi con 2.
        assertEquals(2, HocThuoc.phutThem(18 * 60, HocThuoc.giayCho(12)))
        // Het tran thi khong con gi.
        assertEquals(0, HocThuoc.phutThem(HocThuoc.GIAY_TRAN_MOI_NGAY, HocThuoc.giayCho(12)))
        // Ghi nham lon hon tran cung khong duoc ra so am.
        assertEquals(0, HocThuoc.phutThem(999 * 60, HocThuoc.giayCho(12)))
    }

    @Test
    fun mot_the_va_mot_tu_vung_cung_gia() {
        assertEquals(LuatTuVung.GIAY_MOI_TU, HocThuoc.GIAY_MOI_THE)
    }

    @Test
    fun gia_tron_phut_vi_man_hinh_ghi_bang_phut() {
        // Cau bao cuoi luot o man hoc thuoc va man do tu ghi gia bang "X phút", lay so
        // giay chia 60. Gia le giay thi hai cau do im lang noi sai.
        assertEquals(0, HocThuoc.GIAY_MOI_THE % 60)
        assertEquals(0, LuatTuVung.GIAY_MOI_TU % 60)
    }
}
