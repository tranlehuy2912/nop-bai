package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.NhatKyAi

/**
 * Viet cau con go thanh mot dong cua so hoi AI, giu lai cho xuong dong.
 *
 * Chi goi [NhatKyAi.motDong], khong dung toi prefs: chay tren may nao cung khong xoa
 * hay ghi them gi vao so that.
 */
@RunWith(AndroidJUnit4::class)
class NhatKyAiTest {

    @Test
    fun cau_mot_dong_chi_bo_khoang_trang_hai_dau() {
        assertEquals("giải bài 2.26 sgk toán 8", NhatKyAi.motDong("  giải bài 2.26 sgk toán 8 \n"))
    }

    /** Dung kieu de bai con dan vao ngay 28/9/2026: moi y mot dong, co ca CRLF. */
    @Test
    fun moi_lan_xuong_dong_thanh_mot_dau() {
        assertEquals(
            "Cho tam giác ABC vuông tại A, đường cao AH. ↵ a) Chứng minh AH² = HB.HC. ↵ b) Tính AH.",
            NhatKyAi.motDong(
                "Cho tam giác ABC vuông tại A, đường cao AH.\na) Chứng minh AH² = HB.HC.\r\nb) Tính AH."
            )
        )
    }

    @Test
    fun dong_trong_va_khoang_trang_quanh_cho_xuong_dong_gop_lai() {
        assertEquals("Bài 1 ↵ Bài 2", NhatKyAi.motDong("\n Bài 1  \n\n \t\n   Bài 2\n\n"))
    }

    @Test
    fun xuong_dong_kieu_unicode_cung_tinh() {
        assertEquals("a ↵ b ↵ c ↵ d", NhatKyAi.motDong("a\u2028b\u2029c\u0085d"))
    }

    /**
     * Ket qua phai la dung mot dong cua so, va khong con ky tu nao dau cham trong khuon
     * doc cua Bang dieu khien khong khop (khong thi ca dong hien nguyen ban ben do).
     */
    @Test
    fun ket_qua_luon_la_mot_dong() {
        val dong = NhatKyAi.motDong("x\ny\r\nz\rw\u2028v\u2029u\u0085t")
        assertEquals(listOf(dong), dong.lines())
        assertTrue(Regex(".*").matches(dong))
    }
}
