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
import vn.huytl.homeworkgate.data.BaiKhongDuyet
import java.util.Calendar

/**
 * The bai Ba Huy khong duyet (30/9/2026): Le Hoa xoa the thi the an khoi man Bai da cham.
 *
 * Canh that: the bai 21:48 ngay 30/9/2026 Ba Huy khong duyet chi ghi "chưa có kết quả chấm từng
 * câu", khong co ly do va khong xoa duoc.
 */
@RunWith(AndroidJUnit4::class)
class BaiKhongDuyetTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        BaiKhongDuyet.xoaHet(context)
    }

    @After
    fun tearDown() {
        BaiKhongDuyet.xoaHet(context)
    }

    @Test
    fun xoa_the_thi_an_khoi_man_bai_da_cham() {
        BaiKhongDuyet.an(context, "b1")
        BaiKhongDuyet.an(context, "b2")
        BaiKhongDuyet.an(context, "b1")
        assertEquals(setOf("b1", "b2"), BaiKhongDuyet.daAn(context))
    }

    @Test
    fun chi_giu_nam_muoi_the_moi_xoa() {
        (1..55).forEach { BaiKhongDuyet.an(context, "b$it") }
        val an = BaiKhongDuyet.daAn(context)
        assertEquals(50, an.size)
        assertTrue("b55" in an && "b5" !in an)
    }

    @Test
    fun luc_nop_ghi_ngay_khi_khac_hom_nay() {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 30, 21, 48, 0)
        }
        val homNay = c.timeInMillis + 60 * 60_000L
        assertEquals("lúc 21:48", BaiKhongDuyet.lucNop(c.timeInMillis, homNay))
        val homSau = c.timeInMillis + 24 * 60 * 60_000L
        assertEquals("ngày 30/09 lúc 21:48", BaiKhongDuyet.lucNop(c.timeInMillis, homSau))
        assertTrue(BaiKhongDuyet.lucNop(System.currentTimeMillis()).startsWith("lúc "))
    }
}
