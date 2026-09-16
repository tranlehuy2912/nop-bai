package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.HopThuBaNoi
import vn.huytl.homeworkgate.data.Prefs

/**
 * Kiem tra cua nhan lenh tu may ba noi.
 *
 * Cho nay dang mot cong vao may: cai gi lot qua duoc [HopThuBaNoi.lenhMoi] la cap
 * gio that. Nen test o day khong chi lo "lenh dung thi chay", ma lo ca "cai gi
 * khong phai lenh thi phai bi tu choi", va "mot lenh khong duoc an hai lan".
 *
 * CAN THAN: giong GateStoreTest, bo test nay xoa sach prefs. Chi chay tren may ao.
 */
@RunWith(AndroidJUnit4::class)
class HopThuBaNoiTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    private fun moTa(s: String) = JSONObject().put("description", s)

    private fun tinGhim(s: String) =
        JSONObject().put("pinned_message", JSONObject().put("text", s))

    private fun lenhDung(phut: Int = 30, ma: String = "a3f9") =
        "${HopThuBaNoi.TU_KHOA}|$phut|${System.currentTimeMillis() / 1000}|$ma"

    @Before
    fun setUp() {
        Prefs.get(ctx).raw().edit().clear().commit()
    }

    @Test
    fun docDuocLenhTrongMoTaNhom() {
        val lenh = HopThuBaNoi.lenhMoi(ctx, moTa(lenhDung(30)))
        assertNotNull(lenh)
        assertEquals(30, lenh!!.phut)
    }

    /** Quyen doi thong tin nhom bi tat thi tin ghim phai gach duoc viec. */
    @Test
    fun docDuocLenhTrongTinGhim() {
        val lenh = HopThuBaNoi.lenhMoi(ctx, tinGhim(lenhDung(45)))
        assertNotNull(lenh)
        assertEquals(45, lenh!!.phut)
    }

    /** App ben may ba gui ca dong loi phia sau, phan may doc nam o dau. */
    @Test
    fun boQuaPhanChuNguoiDocPhiaSau() {
        val lenh = HopThuBaNoi.lenhMoi(ctx, moTa("${lenhDung(20)} Bà nội cho chơi 20 phút."))
        assertEquals(20, lenh?.phut)
    }

    @Test
    fun tinGhimBinhThuongKhongPhaiLenh() {
        assertNull(HopThuBaNoi.lenhMoi(ctx, tinGhim("Cả nhà nhớ ăn cơm")))
        assertNull(HopThuBaNoi.lenhMoi(ctx, moTa("Nhóm gia đình")))
        assertNull(HopThuBaNoi.lenhMoi(ctx, JSONObject()))
    }

    /** Khuon sai mot chut cung khong duoc tinh la lenh. */
    @Test
    fun khuonSaiThiTuChoi() {
        assertNull(HopThuBaNoi.lenhMoi(ctx, moTa("CHOGIO|30|123")))
        assertNull(HopThuBaNoi.lenhMoi(ctx, moTa("CHOGIO|ba muoi|123|a3f9")))
        assertNull(HopThuBaNoi.lenhMoi(ctx, moTa("CHOGIA|30|123|a3f9")))
        assertNull(HopThuBaNoi.lenhMoi(ctx, moTa("|30|123|a3f9")))
    }

    /**
     * So phut ngoai khoang thi bo.
     *
     * Neu khong, mot chuoi hong trong mo ta nhom co the thanh "cho choi 9999 phut".
     */
    @Test
    fun soPhutNgoaiKhoangThiTuChoi() {
        assertNull(HopThuBaNoi.lenhMoi(ctx, moTa(lenhDung(0))))
        assertNull(HopThuBaNoi.lenhMoi(ctx, moTa(lenhDung(-30))))
        assertNull(HopThuBaNoi.lenhMoi(ctx, moTa(lenhDung(9999))))
    }

    /**
     * Day la ca quan trong nhat.
     *
     * Mo ta nhom nam nguyen do sau khi doc - khong ai xoa no di. Neu khong nho la
     * da xu ly roi thi cu mot phut tablet lai coi do la lenh moi mot lan.
     */
    @Test
    fun lenhDaDocThiKhongDocLaiLanHai() {
        val chat = moTa(lenhDung(30))
        val lan1 = HopThuBaNoi.lenhMoi(ctx, chat)
        assertNotNull(lan1)
        HopThuBaNoi.danhDauDaDoc(ctx, lan1!!)
        assertNull(HopThuBaNoi.lenhMoi(ctx, chat))
    }

    /** Ma ngau nhien lam hai lan bam cung so phut van ra hai lenh khac nhau. */
    @Test
    fun bamLanNuaCungSoPhutVanLaLenhMoi() {
        val lan1 = HopThuBaNoi.lenhMoi(ctx, moTa(lenhDung(30, "a3f9")))!!
        HopThuBaNoi.danhDauDaDoc(ctx, lan1)
        assertNotNull(HopThuBaNoi.lenhMoi(ctx, moTa(lenhDung(30, "b7c1"))))
    }

    @Test
    fun chuaNoiNhomThiKhongHoiHan() {
        assertFalse(HopThuBaNoi.nenNgo(ctx))
    }

    /** Ba cho xong mot lan la thoi hoi cho den sang mai - do la cho tiet kiem pin. */
    @Test
    fun choXongRoiThiThoiHoi() {
        HopThuBaNoi.datNhom(ctx, -1001234567890L)
        assertFalse(HopThuBaNoi.daChoHomNay(ctx))
        HopThuBaNoi.ghiNhanDaCho(ctx)
        assertTrue(HopThuBaNoi.daChoHomNay(ctx))
        assertFalse(HopThuBaNoi.nenNgo(ctx))
    }
}
