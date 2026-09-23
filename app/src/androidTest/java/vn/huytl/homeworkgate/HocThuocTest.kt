package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TheHoc

/**
 * Luat cham cua duong hoc thuoc.
 *
 * Khong dung den may hay mang, nhung de o day cho cung cho voi cac bo test kia.
 * Bo nay KHONG xoa prefs. No co nap cac bo the that vao kho, y het luc app khoi dong.
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

    // --- ki hieu cua bo KHTN ---

    @Test
    fun chi_so_duoi_go_bang_so_thuong_van_dung() {
        assertTrue(HocThuoc.dung("H2SO4", the("H₂SO₄"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("(NH2)2CO", the("(NH₂)₂CO"), phanBietHoa = true))
    }

    @Test
    fun mu_nhieu_chu_so_doi_ca_cum() {
        // Doi tung chu thi "10²³" ra "10^2^3", ma con go "10^23".
        assertEquals("10^23", HocThuoc.chuanHoa("10²³"))
        assertTrue(HocThuoc.dung("6,022.10^23", the("6,022·10²³")))
        assertFalse(HocThuoc.dung("6,022.10^2^3", the("6,022·10²³")))
    }

    @Test
    fun cac_kieu_dau_nhan_dau_chia_la_mot() {
        val t = the("FA = d.V")
        assertTrue(HocThuoc.dung("FA = d·V", t, phanBietHoa = true))
        assertTrue(HocThuoc.dung("FA = d×V", t, phanBietHoa = true))
        assertTrue(HocThuoc.dung("FA=d*V", t, phanBietHoa = true))
        assertTrue(HocThuoc.dung("D = m : V", the("D = m/V"), phanBietHoa = true))
    }

    @Test
    fun dau_phay_tren_ban_phim_uon_van_la_mot() {
        assertTrue(HocThuoc.dung("H = m’/m·100%", the("H = m'/m·100%"), phanBietHoa = true))
    }

    @Test
    fun ion_go_bang_dau_cong_tru_thuong() {
        assertTrue(HocThuoc.dung("H+", the("H⁺"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("OH-", the("OH⁻"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("OH−", the("OH⁻"), phanBietHoa = true))
    }

    @Test
    fun hai_kieu_ma_cua_chu_co_dau_la_mot() {
        // "a" kem dau huyen dung rieng, nhin y het "à" go lien.
        assertTrue(HocThuoc.dung("màu đỏ", the("màu đỏ")))
    }

    @Test
    fun bo_phan_biet_hoa_thi_CO_khac_Co() {
        val t = the("CO")
        assertTrue(HocThuoc.dung("CO", t, phanBietHoa = true))
        assertFalse(HocThuoc.dung("Co", t, phanBietHoa = true))
        assertFalse(HocThuoc.dung("co", t, phanBietHoa = true))
        // Bo Toan va tu vung khong bat co nay, van bo qua hoa thuong nhu cu.
        assertTrue(HocThuoc.dung("co", t))
    }

    @Test
    fun chu_dau_viet_hoa_thay_thuong_van_duoc_chieu_nguoc_lai_thi_khong() {
        // Ban phim tu viet hoa chu dau: "p" thanh "P". Con khong can duoc.
        assertTrue(HocThuoc.dung("P = F/S", the("p = F/S"), phanBietHoa = true))
        // Nhung chu hoa o giua van phai dung.
        assertFalse(HocThuoc.dung("p = f/s", the("p = F/S"), phanBietHoa = true))
        // Ban phim khong bao gio tu ha chu dau xuong: go "d" cho "D" la sai that,
        // D la khoi luong rieng con d la trong luong rieng.
        assertFalse(HocThuoc.dung("d = m/V", the("D = m/V"), phanBietHoa = true))
    }

    @Test
    fun dap_an_toan_chu_thuong_thi_hoa_thuong_gi_cung_duoc() {
        assertTrue(HocThuoc.dung("Đỏ", the("đỏ"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("KG/M³", the("kg/m³"), phanBietHoa = true))
        // Dap an phu viet thuong thi cung vay, du dap an chinh co chu hoa.
        assertTrue(HocThuoc.dung("Ampe", the("ampe (A)", "ampe", "A"), phanBietHoa = true))
        assertFalse(HocThuoc.dung("a", the("ampe (A)", "ampe", "A"), phanBietHoa = true))
    }

    // --- cac bo the that trong assets ---

    /**
     * Moi bo that nap du so the trong file, va moi dap an trong file tu cham dung.
     *
     * SO THE TRONG FILE PHAI BANG SO THE TRONG BANG, cung ly do voi bo tu vung:
     * the thieu ma, thieu hoi hay thieu dap thi [BoThe] bo qua im lang, con hai the
     * trung ma thi the sau de len the truoc. Ca hai deu chi hien ra o day.
     *
     * TU CHAM DUNG CHINH NO: dap an va tung dap an phu phai qua duoc [HocThuoc.dung]
     * voi dung co cua bo do. Khong qua thi the do khong ai tra loi dung duoc, ke ca
     * khi go y het dap an in trong sach.
     */
    @Test
    fun bo_the_that_nap_du_va_tu_cham_dung_chinh_no() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BoThe.napNeuCan(context)
        val kho = KhoBai.get(context)
        BoThe.BO.forEach { bo ->
            val o = JSONObject(context.assets.open(bo.file).bufferedReader().use { it.readText() })
            val cacBai = o.getJSONArray("cac_bai")
            var soThe = 0
            for (i in 0 until cacBai.length()) {
                val cacThe = cacBai.getJSONObject(i).getJSONArray("cac_the")
                for (j in 0 until cacThe.length()) {
                    soThe++
                    val t = cacThe.getJSONObject(j)
                    val khac = t.optJSONArray("dap_khac")
                    val cacDap = listOf(t.getString("dap")) +
                        (0 until (khac?.length() ?: 0)).map { khac!!.getString(it) }
                    val theHoc = the(cacDap.first(), *cacDap.drop(1).toTypedArray())
                    cacDap.forEach { dap ->
                        assertTrue(
                            "${bo.bo}:${t.getString("ma")} khong nhan \"$dap\"",
                            HocThuoc.dung(dap, theHoc, bo.phanBietHoa)
                        )
                    }
                }
            }
            assertEquals("${bo.bo}: file $soThe the ma bang co", soThe, kho.soTheCua(bo.bo))
        }
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
