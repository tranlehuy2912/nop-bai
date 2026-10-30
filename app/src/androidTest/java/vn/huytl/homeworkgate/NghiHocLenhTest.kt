package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh

/**
 * Lenh NGHI_HOC tu Bang dieu khien (30/10/2026): cho nghi hoc mot ngay ngoai lich, hay bo ngay
 * nghi do. Xem [ThiHanhLenh.nghiHoc] va [NgayNghi.ngayThem].
 *
 * Tinh theo lich mau va mot "bay gio" dat tay (thu nam 29/10/2026), nen khong tuy ngay chay.
 * Giu lai danh sach ngay nghi dang co tren may roi tra ve sau khi chay.
 */
@RunWith(AndroidJUnit4::class)
class NghiHocLenhTest {

    @get:Rule
    val lich = LichMau.Rule()

    private lateinit var context: Context
    private lateinit var prefs: Prefs
    private var cu: Set<Int> = emptySet()

    /** Thu nam 29/10/2026, 20 gio. */
    private val bayGio = NgayNghi.calendarCua(2026, 10, 29, 20, 0).timeInMillis

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = Prefs.get(context)
        cu = prefs.ngayNghiThem
        prefs.ngayNghiThem = emptySet()
        // App.onCreate da gan dung cai nay; gan lai cho chac, phong khi test chay truoc App.
        NgayNghi.ngayThem = { Prefs.get(context).ngayNghiThem }
    }

    @After
    fun tearDown() {
        prefs.ngayNghiThem = cu
    }

    /** Sang thu sau 30/10 hoc Tin, 9 gio dang trong gio chan. Cho nghi thi het chan. */
    @Test
    fun cho_nghi_thi_khong_chan_gio_hoc_bo_nghi_thi_chan_lai() {
        val sangThuSau = NgayNghi.calendarCua(2026, 10, 30, 9, 0)
        assertNotNull(TinhLoiNhac.buoiDangTrongGioChan(sangThuSau, 9 * 60))

        val tra = ThiHanhLenh.nghiHoc(context, "20261030", nghi = true, now = bayGio)
        assertTrue(tra, tra.startsWith("Đã cho"))
        assertTrue(tra, tra.contains("thứ sáu 30/10"))
        assertEquals(setOf(20261030), prefs.ngayNghiThem)
        assertEquals(NgayNghi.TEN_NGHI_THEM, NgayNghi.tenKyNghi(sangThuSau))
        assertNull(TinhLoiNhac.buoiDangTrongGioChan(sangThuSau, 9 * 60))

        assertEquals("Ngày thứ sáu 30/10 đã cho nghỉ rồi.",
            ThiHanhLenh.nghiHoc(context, "20261030", nghi = true, now = bayGio))

        val bo = ThiHanhLenh.nghiHoc(context, "20261030", nghi = false, now = bayGio)
        assertTrue(bo, bo.startsWith("Đã bỏ ngày nghỉ"))
        assertTrue(prefs.ngayNghiThem.isEmpty())
        assertNotNull(TinhLoiNhac.buoiDangTrongGioChan(sangThuSau, 9 * 60))
    }

    /** Moi lan ghi thi bo ngay da qua, danh sach chi giu tu hom nay tro di. */
    @Test
    fun ghi_thi_bo_ngay_da_qua() {
        prefs.ngayNghiThem = setOf(20261020, 20261029)
        ThiHanhLenh.nghiHoc(context, "20261030", nghi = true, now = bayGio)
        assertEquals(setOf(20261029, 20261030), prefs.ngayNghiThem)
    }

    @Test
    fun ngay_da_qua_ngay_nghi_san_va_ngay_sai_thi_khong_doi_gi() {
        assertTrue(ThiHanhLenh.nghiHoc(context, "20261028", nghi = true, now = bayGio).contains("đã qua"))
        // Chu nhat 1/11 da nghi san.
        val chuNhat = ThiHanhLenh.nghiHoc(context, "20261101", nghi = true, now = bayGio)
        assertTrue(chuNhat, chuNhat.contains("nghỉ sẵn theo lịch (chủ nhật)"))
        assertEquals("Lệnh thiếu ngày, máy không đổi gì.",
            ThiHanhLenh.nghiHoc(context, "20261131", nghi = true, now = bayGio))
        assertEquals("Lệnh thiếu ngày, máy không đổi gì.",
            ThiHanhLenh.nghiHoc(context, "", nghi = true, now = bayGio))
        assertTrue(prefs.ngayNghiThem.isEmpty())
    }
}
