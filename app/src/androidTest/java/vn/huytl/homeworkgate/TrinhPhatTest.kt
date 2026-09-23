package vn.huytl.homeworkgate

import android.content.Context
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.guard.TrinhPhat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Duong day tu dich vu doc thong bao den trinh phat.
 *
 * Bo test nay kiem mat xich de sai nhat va khong nhin thay duoc bang mat: khai bao
 * dich vu trong manifest co dung khong, va tu do co hoi duoc danh sach trinh phat
 * dang chay khong. Sai mot ky tu trong ten thanh phan la moi thu van bien dich,
 * van chay, chi co dieu khong bao gio thay app nao dang phat nhac.
 *
 * Can bat quyen truoc khi chay, khong thi test tu bo qua:
 *   adb shell cmd notification allow_listener \
 *     vn.huytl.homeworkgate/vn.huytl.homeworkgate.guard.TaiThongBao
 */
@RunWith(AndroidJUnit4::class)
class TrinhPhatTest {

    private lateinit var context: Context
    private var session: MediaSession? = null

    /** Dat len khi trinh phat gia nhan duoc lenh dung. */
    private val daBamDung = CountDownLatch(1)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Chua bat quyen doc thong bao cho app", TrinhPhat.coQuyen(context))

        // MediaSession phai dung tren luong co Looper.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            session = MediaSession(context, "test-trinh-phat").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPause() = daBamDung.countDown()
                })
                setPlaybackState(
                    PlaybackState.Builder()
                        .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                        .setActions(PlaybackState.ACTION_PAUSE)
                        .build()
                )
                isActive = true
            }
        }
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            session?.release()
            session = null
        }
    }

    @Test
    fun thay_duoc_goi_dang_phat() {
        assertTrue(
            "Khong thay trinh phat nao dang chay",
            context.packageName in TrinhPhat.dangPhat(context)
        )
    }

    @Test
    fun bam_dung_duoc_trinh_phat() {
        assertTrue("Khong bam duoc nut dung", TrinhPhat.dungLai(context, context.packageName))
        assertTrue(
            "Trinh phat khong nhan duoc lenh dung",
            daBamDung.await(5, TimeUnit.SECONDS)
        )
    }
}
