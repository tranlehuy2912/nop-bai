package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CongSang
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import java.util.Calendar

/**
 * Phut cham xong trong gio ngu, giu lai toi luc het gio ngu. Xem [CongSang].
 *
 * Gio ngu dat quanh gio that cua may, y nhu CatMangVpnTest, de ket qua khong tuy luc chay.
 * KHONG gui gi len Telegram: [CongSang.congNeuDenLuc] chi tra cau bao, ben goi moi gui.
 *
 * Service co the dang chay tu bo test truoc, va nhip xet cua no cung goi congNeuDenLuc. Nen
 * test xet ket qua tren cong, khong doi chinh lan goi o day phai la lan cong.
 *
 * Don dep: xoa phan giu, dong phieu gio, tra gio ngu va tran ngay nhu cu.
 */
@RunWith(AndroidJUnit4::class)
class CongSangTest {

    private lateinit var context: Context
    private lateinit var prefs: Prefs
    private var nguCu = 0
    private var dayCu = 0
    private var tranCu = 0

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = Prefs.get(context)
        nguCu = prefs.hardStopMinuteOfDay
        dayCu = prefs.gioDayMinuteOfDay
        tranCu = prefs.tranPhutMoiNgay
        CongSang.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
    }

    @After
    fun tearDown() {
        CongSang.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        prefs.hardStopMinuteOfDay = nguCu
        prefs.gioDayMinuteOfDay = dayCu
        prefs.tranPhutMoiNgay = tranCu
        SoCaiBai.xoaLoiNhan(context)
    }

    private fun phutBayGio(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /** Dang trong gio ngu, con mot tieng nua moi het. */
    private fun datDangNgu() {
        val bayGio = phutBayGio()
        prefs.hardStopMinuteOfDay = (bayGio + 24 * 60 - 60) % (24 * 60)
        prefs.gioDayMinuteOfDay = (bayGio + 60) % (24 * 60)
    }

    /** Vua het gio ngu. */
    private fun datHetNgu() {
        val bayGio = phutBayGio()
        prefs.hardStopMinuteOfDay = (bayGio + 180) % (24 * 60)
        prefs.gioDayMinuteOfDay = (bayGio + 24 * 60 - 1) % (24 * 60)
    }

    @Test
    fun trong_gio_ngu_thi_giu_het_gio_ngu_thi_cong() {
        prefs.tranPhutMoiNgay = GateStore(context).phutDaDuyetHomNay() + 100
        datDangNgu()
        CongSang.them(context, 12, "thu-cong-sang")
        assertEquals(listOf(12), CongSang.cacMuc(context).map { it.phut })
        // Luc cong la luc het gio ngu, trong vong mot tieng toi.
        val con = CongSang.lucCong(context) - System.currentTimeMillis()
        assertTrue("$con", con in 1..61 * 60_000L)
        assertEquals(CongSang.lucCong(context), CongSang.mocBaoThuc(context))
        assertNull(CongSang.congNeuDenLuc(context))
        assertEquals(1, CongSang.cacMuc(context).size)

        datHetNgu()
        val bao = CongSang.congNeuDenLuc(context)
        if (bao != null) assertTrue(bao, bao.contains("đã cộng 12 phút"))
        val gate = GateStore(context)
        assertEquals(GateState.GRANTED, gate.state)
        assertEquals(12, gate.grantedMinutes)
        assertTrue(CongSang.cacMuc(context).isEmpty())
        assertNull(CongSang.mocBaoThuc(context))
        assertTrue(SoCaiBai.loiNhan(context).orEmpty().contains("Được thêm 12 phút"))
    }

    @Test
    fun cong_luc_het_gio_ngu_van_an_vao_tran_ngay() {
        datDangNgu()
        CongSang.them(context, 10, "thu-cong-sang-1")
        CongSang.them(context, 8, "thu-cong-sang-2")
        datHetNgu()
        // Hom nay chi con 5 phut trong tran: cong 5, bai giu truoc lay truoc.
        prefs.tranPhutMoiNgay = maxOf(15, GateStore(context).phutDaDuyetHomNay() + 5)
        val conTran = GateStore(context).phutConLaiHomNay()
        val bao = CongSang.congNeuDenLuc(context)
        if (bao != null && conTran < 18) {
            assertTrue(bao, bao.contains("đã cộng $conTran phút"))
            assertTrue(bao, bao.contains("hôm nay chỉ còn $conTran phút trong hạn mức"))
        }
        assertEquals(minOf(18, conTran), GateStore(context).grantedMinutes)
        assertTrue(CongSang.cacMuc(context).isEmpty())
    }

    @Test
    fun khong_giu_gi_thi_khong_lam_gi() {
        datHetNgu()
        assertNull(CongSang.congNeuDenLuc(context))
        assertNull(CongSang.mocBaoThuc(context))
        CongSang.them(context, 0)
        assertTrue(CongSang.cacMuc(context).isEmpty())
    }
}
