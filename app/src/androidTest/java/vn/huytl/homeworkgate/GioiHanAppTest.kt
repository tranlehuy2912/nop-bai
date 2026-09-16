package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.Prefs

/**
 * Han gio rieng tung app: Netflix mot ngay mot tieng ruoi, du co gio choi hay khong.
 *
 * CAN THAN: giong [GateStoreTest], bo test nay xoa sach prefs. Dung chay tren tablet
 * cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class GioiHanAppTest {

    private lateinit var context: Context
    private val netflix = "com.netflix.mediaclient"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(context).raw().edit().clear().commit()
    }

    @Test
    fun chua_dat_han_thi_khong_gioi_han_gi() {
        assertEquals(0, GioiHanApp.han(context, netflix))
        assertFalse(GioiHanApp.hetGio(context, netflix))
        assertEquals(Long.MAX_VALUE, GioiHanApp.conLaiMs(context, netflix))
    }

    @Test
    fun dat_han_roi_xem_thi_so_con_lai_giam_theo() {
        GioiHanApp.datHan(context, netflix, 90)
        assertEquals(90, GioiHanApp.han(context, netflix))

        GioiHanApp.congThem(context, netflix, 20 * 60_000L)
        assertEquals(20 * 60_000L, GioiHanApp.daDungMs(context, netflix))
        assertEquals(70 * 60_000L, GioiHanApp.conLaiMs(context, netflix))
        assertFalse(GioiHanApp.hetGio(context, netflix))
    }

    @Test
    fun xem_du_so_phut_thi_het_gio() {
        GioiHanApp.datHan(context, netflix, 90)
        GioiHanApp.congThem(context, netflix, 90 * 60_000L)

        assertTrue(GioiHanApp.hetGio(context, netflix))
        assertEquals(0L, GioiHanApp.conLaiMs(context, netflix))
    }

    @Test
    fun han_cua_app_nay_khong_dinh_gi_den_app_khac() {
        GioiHanApp.datHan(context, netflix, 30)
        GioiHanApp.congThem(context, netflix, 30 * 60_000L)

        val youtube = "com.google.android.youtube"
        assertFalse(GioiHanApp.hetGio(context, youtube))
        assertEquals(0L, GioiHanApp.daDungMs(context, youtube))
    }

    @Test
    fun ba_xoa_gio_da_xem_thi_dem_lai_tu_dau() {
        GioiHanApp.datHan(context, netflix, 30)
        GioiHanApp.congThem(context, netflix, 30 * 60_000L)
        assertTrue(GioiHanApp.hetGio(context, netflix))

        GioiHanApp.xoaDaDung(context, netflix)
        assertEquals(0L, GioiHanApp.daDungMs(context, netflix))
        assertFalse(GioiHanApp.hetGio(context, netflix))
        // Bo dem xoa nhung han van con: mai van la ba muoi phut.
        assertEquals(30, GioiHanApp.han(context, netflix))
    }

    @Test
    fun bo_han_thi_so_da_xem_khong_con_y_nghia() {
        GioiHanApp.datHan(context, netflix, 30)
        GioiHanApp.congThem(context, netflix, 40 * 60_000L)
        assertTrue(GioiHanApp.hetGio(context, netflix))

        GioiHanApp.datHan(context, netflix, 0)
        assertEquals(0, GioiHanApp.han(context, netflix))
        assertFalse(GioiHanApp.hetGio(context, netflix))
    }

    @Test
    fun dat_han_cho_nhieu_app_thi_moi_cai_mot_so_rieng() {
        val youtube = "com.google.android.youtube"
        GioiHanApp.datHan(context, netflix, 90)
        GioiHanApp.datHan(context, youtube, 30)

        assertEquals(mapOf(netflix to 90, youtube to 30), GioiHanApp.tatCa(context))
    }
}
