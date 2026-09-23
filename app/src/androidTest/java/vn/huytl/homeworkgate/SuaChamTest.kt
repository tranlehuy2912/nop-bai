package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.SuaCham

/**
 * Sua ban cham theo Claude: chi sua cau dang cho sua, tinh phut dung luat, va khong
 * bao gio tra gio hai lan cho mot cau.
 *
 * Chi xoa so cai, khong xoa prefs: bo test nay khong dung toi cau hinh nao. Van dung
 * chay tren tablet cua Le Hoa, vi so cai la so that cua con.
 */
@RunWith(AndroidJUnit4::class)
class SuaChamTest {

    private lateinit var context: Context
    private val now = System.currentTimeMillis()

    private fun cauSai(ma: String, de: String, cauId: String, dong: Int = 1) = CauCham(
        ma = ma,
        de = de,
        ketQua = "kết quả của con",
        dung = false,
        soDong = dong,
        baiLam = List(dong) { "dòng ${it + 1}" },
        cauId = cauId,
        mon = "Toán",
        nhanXet = "máy bảo sai"
    )

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        SoCaiBai.xoaHet(context)
    }

    @After
    fun tearDown() {
        SoCaiBai.xoaHet(context)
    }

    @Test
    fun cau_may_bao_sai_duoc_sua_thanh_dung_va_tra_gio_theo_luat() {
        val de = "Rút gọn biểu thức (2x − 5y)(2x + 5y) + (2x + 5y)^2."
        SoCaiBai.ghi(context, listOf(cauSai("2.33a", de, "sachthu:2.33a")), emptyMap(), now)
        assertEquals(1, SoCaiBai.dangChoSua(context, now + 1).size)

        val chuanBi = SuaCham.chuanBi(context, listOf(SuaCham.Cau("2.33a", de)), now + 1000)
        assertEquals(1, chuanBi.cac.size)
        assertTrue(chuanBi.cac.single().dung)
        // Mot dong lam bai thi duoc muc toi thieu cua bai tap, y nhu luc may tu cham.
        assertEquals(LuatCongGio.TOI_THIEU_BAI_TAP, chuanBi.phut)

        val daGhi = SuaCham.ghi(context, chuanBi, now + 1000)
        assertEquals(1, daGhi.size)
        assertTrue(SoCaiBai.dangChoSua(context, now + 2000).isEmpty())
        assertEquals(LuatCongGio.TOI_THIEU_BAI_TAP, SoCaiBai.phutDaCongHomNay(context, now + 2000))
    }

    @Test
    fun gui_lai_lenh_thi_khong_tra_gio_lan_hai() {
        val de = "Tính nhanh giá trị của biểu thức x^3 + 3x^2 + 3x + 1 tại x = 999."
        SoCaiBai.ghi(context, listOf(cauSai("2.32b", de, "sachthu:2.32b")), emptyMap(), now)
        val lan1 = SuaCham.chuanBi(context, listOf(SuaCham.Cau("2.32b", de)), now + 1000)
        SuaCham.ghi(context, lan1, now + 1000)

        val lan2 = SuaCham.chuanBi(context, listOf(SuaCham.Cau("2.32b", de)), now + 2000)
        assertTrue(lan2.cac.isEmpty())
        assertEquals(listOf("2.32b"), lan2.boQua)
        assertTrue(SuaCham.ghi(context, lan2, now + 2000).isEmpty())
        assertEquals(lan1.phut, SoCaiBai.phutDaCongHomNay(context, now + 3000))
    }

    @Test
    fun cau_khong_dang_cho_sua_thi_bo_qua() {
        val chuanBi = SuaCham.chuanBi(context, listOf(SuaCham.Cau("9.99", "không có câu này")), now)
        assertTrue(chuanBi.cac.isEmpty())
        assertEquals(listOf("9.99"), chuanBi.boQua)
        assertEquals(0, chuanBi.phut)
    }

    @Test
    fun hai_sach_trung_ma_thi_phan_biet_bang_de() {
        SoCaiBai.ghi(
            context,
            listOf(
                cauSai("1.1", "Đề của sách A.", "sachA:1.1"),
                cauSai("1.1", "Đề của sách B.", "sachB:1.1")
            ),
            emptyMap(), now
        )
        val chuanBi = SuaCham.chuanBi(context, listOf(SuaCham.Cau("1.1", "Đề của sách B.")), now + 1000)
        assertEquals(listOf("sachB:1.1"), chuanBi.cac.map { it.cauId })
        SuaCham.ghi(context, chuanBi, now + 1000)

        val conLai = SoCaiBai.dangChoSua(context, now + 2000)
        assertEquals(listOf("sachA:1.1"), conLai.map { it.khoa })
    }
}
