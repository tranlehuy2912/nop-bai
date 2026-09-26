package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.guard.ThongBaoTelegram

/**
 * Dem tin Ba Huy gui qua Telegram theo thong bao cua Telegram: nhan ra thong bao cua
 * khung chat voi ba bang ma "ndid_" + chat id, va lay so tin Telegram ghi san.
 *
 * Tinh bang so, khong can Telegram that.
 */
@RunWith(AndroidJUnit4::class)
class TelegramThatTest {

    @Test
    fun nhan_ra_thong_bao_cua_khung_chat_voi_ba() {
        assertTrue(ThongBaoTelegram.laCuaBa("ndid_123", 123L))
    }

    /** Tin cua ban khac, cua nhom, hay thong bao gop cua Telegram thi khong phai cua ba. */
    @Test
    fun tin_cua_nguoi_khac_khong_phai_cua_ba() {
        assertFalse(ThongBaoTelegram.laCuaBa("ndid_456", 123L))
        assertFalse(ThongBaoTelegram.laCuaBa("ndid_-100123", 123L))
        assertFalse(ThongBaoTelegram.laCuaBa("ndid_1234", 123L))
        assertFalse(ThongBaoTelegram.laCuaBa(null, 123L))
    }

    /** Chat id am la cua mot nhom, chua cai xong la 0: khong khop voi tin nao ca. */
    @Test
    fun chat_id_khong_phai_cua_mot_nguoi_thi_khong_khop() {
        assertFalse(ThongBaoTelegram.laCuaBa("ndid_0", 0L))
        assertFalse(ThongBaoTelegram.laCuaBa("ndid_-5", -5L))
    }

    @Test
    fun dem_so_tin_chua_doc_cua_ba() {
        assertEquals(3, ThongBaoTelegram.soTinCuaBa(11, "ndid_123", 3, 123L))
        assertEquals(1, ThongBaoTelegram.soTinCuaBa(11, "ndid_123", 0, 123L))
        assertEquals(0, ThongBaoTelegram.soTinCuaBa(11, "ndid_456", 3, 123L))
        assertEquals(0, ThongBaoTelegram.soTinCuaBa(11, null, 3, 123L))
    }

    /**
     * Ba dang story thi Telegram cung hien mot thong bao mang ma khung chat cua ba, so cua
     * no la so story. Khong duoc tinh la tin nhan.
     */
    @Test
    fun story_cua_ba_khong_tinh_la_tin() {
        assertEquals(0, ThongBaoTelegram.soTinCuaBa(ThongBaoTelegram.MA_STORY, "ndid_123", 2, 123L))
    }
}
