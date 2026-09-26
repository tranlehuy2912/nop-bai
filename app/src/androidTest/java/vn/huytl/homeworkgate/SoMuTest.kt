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
}
