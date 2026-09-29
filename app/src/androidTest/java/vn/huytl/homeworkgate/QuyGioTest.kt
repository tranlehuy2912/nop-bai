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
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.QuyGio
import java.util.Calendar

/**
 * "Quỹ giờ chơi" ([QuyGio], tu 29/9/2026): phut lam tren may bi tran cat vao quy, Ba Huy cap
 * tu quy bang lenh CAPQUY hay /quy. Cap khong tinh vao so dem phut doi bang hoc trong ngay,
 * va cap hong (gio ngu) thi quy giu nguyen.
 *
 * Gio ngu dat quanh gio that cua may nhu SuaChamLenhTest. Don dep: tra lai quy, gio ngu,
 * dong phieu gio vua cap. Khong xoa prefs.
 */
@RunWith(AndroidJUnit4::class)
class QuyGioTest {

    private lateinit var context: Context
    private var quyCu = 0
    private var nguCu = 0
    private var dayCu = 0

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val p = Prefs.get(context)
        quyCu = p.quyGio
        nguCu = p.hardStopMinuteOfDay
        dayCu = p.gioDayMinuteOfDay
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        datGioNgu(dangNgu = false)
        p.quyGio = 0
    }

    @After
    fun tearDown() {
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        val p = Prefs.get(context)
        p.quyGio = quyCu
        p.hardStopMinuteOfDay = nguCu
        p.gioDayMinuteOfDay = dayCu
    }

    /** Dat gio ngu quanh gio that: dang ngu thi con mot tieng nua, khong thi ba tieng nua moi ngu. */
    private fun datGioNgu(dangNgu: Boolean) {
        val c = Calendar.getInstance()
        val bayGio = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val ngay = 24 * 60
        Prefs.get(context).hardStopMinuteOfDay = (bayGio + if (dangNgu) ngay - 60 else 180) % ngay
        Prefs.get(context).gioDayMinuteOfDay = (bayGio + if (dangNgu) 60 else 240) % ngay
    }

    @Test
    fun them_cong_vao_quy_so_0_hay_am_thi_bo_qua() {
        QuyGio.them(context, 7, "Làm bài trên máy")
        QuyGio.them(context, 0, "Làm bài trên máy")
        QuyGio.them(context, -3, "Làm bài trên máy")

        assertEquals(7, QuyGio.so(context))
        assertTrue(DayLog.today(context).contains("Vào quỹ giờ chơi 7 phút (Làm bài trên máy vượt trần hôm nay)"))
    }

    @Test
    fun quy_trong_thi_khong_cap() {
        val truoc = GateStore(context).state

        val kq = QuyGio.cap(context, null)

        assertEquals(0, kq.cap)
        assertEquals("Quỹ giờ chơi đang trống.", kq.loi)
        assertEquals(truoc, GateStore(context).state)
        assertEquals(0, GateStore(context).grantedMinutes)
    }

    @Test
    fun cap_het_quy_thanh_phieu_khong_tinh_vao_so_dem_ngay() {
        Prefs.get(context).quyGio = 12
        val daDuyet = GateStore(context).phutDaDuyetHomNay()

        val kq = QuyGio.cap(context, null)

        assertEquals(12, kq.cap)
        assertEquals(0, kq.conLai)
        assertNull(kq.loi)
        assertEquals(0, QuyGio.so(context))
        val gate = GateStore(context)
        assertEquals(GateState.GRANTED, gate.state)
        assertEquals(12, gate.grantedMinutes)
        // Ba Huy chu dong cho, y nhu lenh CHO: khong an vao so phut doi bang hoc.
        assertEquals(daDuyet, gate.phutDaDuyetHomNay())
        assertTrue(DayLog.today(context).contains("Ba Huy cấp 12 phút từ quỹ giờ chơi, quỹ còn 0 phút"))
    }

    @Test
    fun cap_mot_phan_thi_quy_con_lai_xin_qua_quy_thi_cap_phan_con() {
        Prefs.get(context).quyGio = 30
        val lan1 = QuyGio.cap(context, 10)
        assertEquals(10, lan1.cap)
        assertEquals(20, lan1.conLai)
        assertEquals(20, QuyGio.so(context))

        val lan2 = QuyGio.cap(context, 50)
        assertEquals(20, lan2.cap)
        assertEquals(0, lan2.conLai)
        assertEquals(0, QuyGio.so(context))
        // Hai lan cap cong don vao cung mot phieu.
        assertEquals(30, GateStore(context).grantedMinutes)
    }

    @Test
    fun dang_choi_thi_cong_vao_phien() {
        val gate = GateStore(context)
        assertNotNull(gate.approve(wantedMinutes = 10, useQuota = false))
        assertNotNull(gate.start())
        val truoc = gate.remainingMs()
        Prefs.get(context).quyGio = 6

        val kq = QuyGio.cap(context, 6)

        assertEquals(6, kq.cap)
        assertEquals(GateState.ACTIVE, GateStore(context).state)
        val them = GateStore(context).remainingMs() - truoc
        assertTrue("cong $them ms", them in (5 * 60_000L)..(6 * 60_000L + 1_000L))
    }

    @Test
    fun gio_ngu_thi_khong_cap_va_quy_giu_nguyen() {
        datGioNgu(dangNgu = true)
        Prefs.get(context).quyGio = 9

        val kq = QuyGio.cap(context, null)

        assertEquals(0, kq.cap)
        assertEquals(9, kq.conLai)
        assertTrue(kq.loi.orEmpty(), kq.loi.orEmpty().startsWith("Chưa cấp được"))
        assertEquals(9, QuyGio.so(context))
    }
}
