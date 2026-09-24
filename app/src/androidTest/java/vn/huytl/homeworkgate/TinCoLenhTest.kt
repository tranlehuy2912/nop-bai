package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh

/**
 * Lenh TINCO tu Bang dieu khien, va kho tin cua co ma lenh do ghi vao.
 *
 * Giu lai kho tin dang co tren may roi tra ve nguyen ven sau khi chay: may ao co the
 * dang giu tin thu, xoa sach thi mat.
 */
@RunWith(AndroidJUnit4::class)
class TinCoLenhTest {

    private lateinit var context: Context
    private var khoCu: String? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        khoCu = Prefs.get(context).raw().getString(K_KHO, null)
        KhoTinCuaCo(context).xoaHet()
    }

    @After
    fun tearDown() {
        val sua = Prefs.get(context).raw().edit()
        if (khoCu == null) sua.remove(K_KHO) else sua.putString(K_KHO, khoCu)
        sua.commit()
    }

    @Test
    fun lenh_tin_co_ghi_gio_gui_va_tra_loi_nhu_telegram() {
        val toiQua = System.currentTimeMillis() - 12 * GIO

        val tra = ThiHanhLenh.tinCo(context, "  Mai lớp kiểm tra 15 phút môn Toán.  ", toiQua)

        assertEquals("Đã đưa lên tablet rồi.", tra)
        val tin = KhoTinCuaCo(context).danhSach()
        assertEquals(1, tin.size)
        assertEquals("Mai lớp kiểm tra 15 phút môn Toán.", tin[0].noiDung)
        assertEquals(toiQua, tin[0].luc)
        assertFalse(tin[0].daDoc)
    }

    @Test
    fun tin_rong_khong_ghi_gi() {
        assertEquals("Tin rỗng, không đưa lên.", ThiHanhLenh.tinCo(context, "   ", 0L))
        assertEquals(0, KhoTinCuaCo(context).danhSach().size)
    }

    @Test
    fun tin_den_muon_xep_theo_gio_gui_va_khong_ghi_trung() {
        val kho = KhoTinCuaCo(context)
        val bayGio = System.currentTimeMillis()
        kho.them("Tin sáng nay", bayGio)
        // Tablet tat ca dem: tin gui toi qua den sau tin sang nay.
        kho.them("Tin tối qua", bayGio - 10 * GIO)
        // Cung lenh do doc lai lan hai.
        kho.them("Tin tối qua", bayGio - 10 * GIO)

        assertEquals(listOf("Tin sáng nay", "Tin tối qua"), kho.danhSach().map { it.noiDung })
    }

    @Test
    fun giu_bon_muoi_tin_moi_nhat() {
        val kho = KhoTinCuaCo(context)
        val goc = System.currentTimeMillis()
        repeat(45) { kho.them("Tin $it", goc + it * 1_000L) }

        val tin = kho.danhSach()
        assertEquals(40, tin.size)
        assertEquals("Tin 44", tin.first().noiDung)
        assertEquals("Tin 5", tin.last().noiDung)
    }

    private companion object {
        const val GIO = 3_600_000L

        /** Khoa cua kho trong Prefs, chep tu KhoTinCuaCo. Chi dung de tra kho cu ve. */
        const val K_KHO = "tin_cua_co"
    }
}
