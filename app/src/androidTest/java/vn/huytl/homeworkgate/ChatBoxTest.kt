package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.ChatFrom
import vn.huytl.homeworkgate.data.Prefs

/**
 * Khung chat phai tu gon lai: giu nam muoi cau gan nhat, cau thu nam muoi mot day
 * cau cu nhat ra khoi may han chu khong chi giau di.
 *
 * CAN THAN: giong [GateStoreTest], bo test nay xoa sach prefs. Dung chay tren
 * tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class ChatBoxTest {

    private lateinit var context: Context
    private val ngay = 24 * 60 * 60 * 1000L

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(context).raw().edit().clear().commit()
    }

    @Test
    fun giu_toi_da_nam_muoi_cau() {
        val moc = System.currentTimeMillis()
        repeat(60) { ChatBox.add(context, ChatFrom.CON, "cau $it", moc + it * 1_000L) }

        val cac = ChatBox.read(context)
        assertEquals(50, cac.size)
        // Giu cac cau MOI nhat: cau 0..9 la nhung cau bi day ra.
        assertEquals("cau 10", cac.first().text)
        assertEquals("cau 59", cac.last().text)
    }

    @Test
    fun cau_cu_bao_nhieu_ngay_van_giu_neu_chua_qua_nam_muoi() {
        val bayGio = System.currentTimeMillis()
        ChatBox.add(context, ChatFrom.BA, "chuyen thang truoc", bayGio - 40 * ngay)
        ChatBox.add(context, ChatFrom.BA, "chuyen hom nay", bayGio)

        // Khong co luat xoa theo ngay: hop moi co hai cau thi khong xoa gi ca.
        val cac = ChatBox.read(context)
        assertEquals(2, cac.size)
        assertEquals("chuyen thang truoc", cac.first().text)
    }

    @Test
    fun cau_thu_nam_muoi_mot_day_cau_cu_nhat_ra_khoi_may() {
        val moc = System.currentTimeMillis()
        repeat(51) { ChatBox.add(context, ChatFrom.CON, "cau $it", moc + it * 1_000L) }

        val cac = ChatBox.read(context)
        assertEquals(50, cac.size)
        assertTrue(cac.none { it.text == "cau 0" })
        assertEquals("cau 1", cac.first().text)
        assertEquals("cau 50", cac.last().text)
    }

    @Test
    fun xoa_het_thi_khung_chat_trong() {
        val bayGio = System.currentTimeMillis()
        ChatBox.add(context, ChatFrom.CON, "mot cau", bayGio)
        ChatBox.xoaHet(context)

        assertEquals(0, ChatBox.read(context).size)
        assertEquals(0, ChatBox.unread(context))
    }
}
