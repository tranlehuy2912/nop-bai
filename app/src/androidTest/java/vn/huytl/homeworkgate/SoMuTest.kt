package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.ui.SoMu

/**
 * Doi so mu luc hien de cho con doc. Xem [SoMu].
 *
 * Ham chi xu ly chu, khong can may that, nhung repo chi co bo test chay tren may nen
 * dat o day.
 */
@RunWith(AndroidJUnit4::class)
class SoMuTest {

    @Test
    fun so_mu_la_chu_so() {
        assertEquals("x²y³ − 6x² + 9", SoMu.hien("x^2y^3 − 6x^2 + 9"))
        assertEquals("1,2044·10²² phân tử", SoMu.hien("1,2044·10^22 phân tử"))
        assertEquals("(x + 1)³/(x² − 1)", SoMu.hien("(x + 1)^3/(x^2 − 1)"))
    }

    @Test
    fun dau_cua_ion() {
        assertEquals("ion Ca²⁺ và Mg²⁺.", SoMu.hien("ion Ca^2+ và Mg^2+."))
        assertEquals("H⁺, OH⁻, SO4²⁻", SoMu.hien("H^+, OH^−, SO4^2−"))
    }

    @Test
    fun dau_tru_lien_sau_so_mu_van_la_phep_tru() {
        // Viet thieu dau cach thi "−1" dung sau so mu van la phep tru, khong phai dien tich.
        assertEquals("x²−1", SoMu.hien("x^2−1"))
        assertEquals("x²+y", SoMu.hien("x^2+y"))
    }

    @Test
    fun kieu_khac_thi_de_nguyen() {
        assertEquals("x^(n+1)", SoMu.hien("x^(n+1)"))
        assertEquals("Không có số mũ", SoMu.hien("Không có số mũ"))
    }

    @Test
    fun khtn_doi_chi_so_cong_thuc_hoa_hoc() {
        val khtn = "Khoa học tự nhiên"
        assertEquals(
            "Hỗn hợp CO và C₂H₆ có tỉ lệ 1 : 2 về số mol.",
            SoMu.hienDe("Hỗn hợp CO và C2H6 có tỉ lệ 1 : 2 về số mol.", khtn)
        )
        assertEquals("Ca(OH)₂, Fe₂(SO₄)₃, CuSO₄.5H₂O", SoMu.hienDe("Ca(OH)2, Fe2(SO4)3, CuSO4.5H2O", khtn))
        assertEquals("2H₂ + O₂ ⟶ 2H₂O", SoMu.hienDe("2H2 + O2 ⟶ 2H2O", khtn))
        // So mu va dien tich ion doi truoc, roi moi toi chi so.
        assertEquals("SO₄²⁻ và 6,02·10²³", SoMu.hienDe("SO4^2− và 6,02·10^23", khtn))
        // Ky hieu dai luong chu hoa cung thanh chi so duoi; chu thuong giu nguyen.
        assertEquals("M₁·n1 + M₂·n2", SoMu.hienDe("M1·n1 + M2·n2", khtn))
        assertEquals(
            "Hình 3.1, Bài 5, 0,3 M, phản ứng (1)",
            SoMu.hienDe("Hình 3.1, Bài 5, 0,3 M, phản ứng (1)", khtn)
        )
    }

    @Test
    fun mon_khac_khong_doi_chi_so() {
        assertEquals("Điểm A1, B1 và x²", SoMu.hienDe("Điểm A1, B1 và x^2", "Toán"))
        assertEquals("Unit 2, A1", SoMu.hienDe("Unit 2, A1", "Tiếng Anh"))
    }
}
