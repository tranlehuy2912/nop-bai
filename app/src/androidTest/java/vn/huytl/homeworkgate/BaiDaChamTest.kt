package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.BaiDaCham
import vn.huytl.homeworkgate.dongbo.Duong

/**
 * Trang Bai da cham doc tung bai tu Firestore, xem [BaiDaCham].
 *
 * Kiem canh chi co ban Claude: may khong cham, vi du bai da duyet tay truoc luc Ba Huy
 * dan ket qua. Luc do de phai lay tu ban Claude hoac tu truong khai, khong duoc trong.
 * So thi de kieu Long, dung kieu Firestore tra ve.
 */
@RunWith(AndroidJUnit4::class)
class BaiDaChamTest {

    private val khai = mapOf(
        "tenNguon" to "SGK Toán 8 — tập một",
        "bai" to "trang 47",
        "mon" to "Toán",
        "onTap" to false,
        "cac" to listOf(
            mapOf(
                "ma" to "2.33a", "cauId" to "toan8t1:2.33a",
                "de" to "Rút gọn (2x + 5y)^2 − (2x − 5y)^2", "dang" to "CAU_NHO"
            )
        )
    )

    @Test
    fun chi_co_ban_claude_thi_de_lay_tu_khai_va_tu_ban_claude() {
        val bai = BaiDaCham.tuDuLieu(
            "b1",
            mapOf(
                Duong.F_LUC to 1_790_133_813_356L,
                Duong.F_KHAI to khai,
                Duong.F_CHAM_CLAUDE to mapOf(
                    "luc" to 1_790_134_000_000L,
                    "chinh" to true,
                    "cac" to listOf(
                        mapOf(
                            "ma" to "2.33a", "dung" to false, "chac" to true,
                            "conViet" to "8x^2", "goiY" to "Xem lại hằng đẳng thức"
                        ),
                        mapOf(
                            "ma" to "5", "dung" to true, "chac" to true,
                            "conViet" to "5", "goiY" to "", "de" to "Tính 2 + 3."
                        )
                    )
                )
            )
        )
        val cac = bai.cac!!
        // Cau con khai: Claude khong chep de, lay tu truong khai.
        assertEquals("Rút gọn (2x + 5y)^2 − (2x − 5y)^2", cac.single { it.ma == "2.33a" }.de)
        // Cau ngoai sach: lay de Claude chep.
        assertEquals("Tính 2 + 3.", cac.single { it.ma == "5" }.de)
        assertEquals("Toán", bai.mon)
        assertEquals(1_790_133_813_356L, bai.luc)
        assertEquals(1_790_134_000_000L, bai.claudeLuc)
    }

    @Test
    fun co_ban_cham_cua_may_thi_giu_de_cua_may() {
        val bai = BaiDaCham.tuDuLieu(
            "b2",
            mapOf(
                Duong.F_KHAI to khai,
                Duong.F_CHAM to mapOf(
                    "mon" to "Toán",
                    "cac" to listOf(mapOf("ma" to "2.33a", "de" to "Đề máy chép", "dung" to true))
                )
            )
        )
        assertEquals("Đề máy chép", bai.cac!!.single().de)
    }

    @Test
    fun chua_co_ban_cham_nao_thi_chua_co_cau() {
        assertNull(BaiDaCham.tuDuLieu("b3", mapOf(Duong.F_KHAI to khai)).cac)
    }
}
