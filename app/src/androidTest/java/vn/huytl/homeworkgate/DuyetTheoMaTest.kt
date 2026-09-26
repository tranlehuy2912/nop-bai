package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh

/**
 * Lenh DUYET va TUCHOI tu Bang dieu khien luon kem ma bai. Bai do khong con trong hang
 * cho thi khong duoc dung sang bai khac.
 *
 * Canh that: bai nop 20:29 ngay 24/9/2026, sang ngay 25 tablet tu bo khoi hang cho ma
 * Bang dieu khien van ghi dang cho. Ban cu gap ma bai khong co trong hang thi lay bai
 * cu nhat dang cho, tuc la bam Duyet bai hom qua la duyet nham bai hom nay.
 *
 * Khong chay nhanh duyet duoc: nhanh do bat ApprovalService. Don dep sau khi chay: bo
 * bai thu khoi hang cho, va dong phieu gio.
 */
@RunWith(AndroidJUnit4::class)
class DuyetTheoMaTest {

    private lateinit var context: Context
    private lateinit var gate: GateStore
    private val conCho = "thu-con-cho"
    private val daBo = "thu-da-bo"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        gate = GateStore(context)
        gate.boBaiCho(conCho)
        gate.boBaiCho(daBo)
    }

    @After
    fun tearDown() {
        gate.boBaiCho(conCho)
        gate.boBaiCho(daBo)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
    }

    @Test
    fun khong_duyet_bai_da_bo_thi_hang_cho_giu_nguyen() {
        gate.markPending(conCho, 0L)
        val truoc = gate.baiDangCho()

        assertEquals(
            "Bài đó không còn trong hàng chờ của tablet. Đã gỡ khỏi danh sách chờ duyệt.",
            ThiHanhLenh.tuChoi(context, gate, daBo, "Làm ẩu, làm lại đi")
        )
        assertEquals(truoc, gate.baiDangCho())
    }

    @Test
    fun duyet_bai_da_bo_thi_khong_cap_gio_cho_bai_khac() {
        gate.markPending(conCho, 0L)
        val truoc = gate.baiDangCho()
        val congTruoc = gate.state
        val phutTruoc = gate.grantedMinutes

        assertEquals(
            "Bài đó không còn trong hàng chờ nữa. Muốn cho giờ thì bấm Cho chơi ngay.",
            ThiHanhLenh.duyet(context, gate, daBo, 30)
        )
        assertEquals(truoc, gate.baiDangCho())
        assertEquals(congTruoc, gate.state)
        assertEquals(phutTruoc, gate.grantedMinutes)
    }

    @Test
    fun khong_duyet_dung_bai_dang_cho() {
        gate.markPending(daBo, 0L)
        gate.markPending(conCho, 0L)

        assertEquals("Đã từ chối bài đó.", ThiHanhLenh.tuChoi(context, gate, conCho, ""))
        assertFalse(gate.baiDangCho().any { it.id == conCho })
        assertTrue(gate.baiDangCho().any { it.id == daBo })
    }

    @Test
    fun khong_co_ma_bai_thi_lay_bai_cu_nhat_nhu_truoc() {
        // Hang cho con bai that thi bai cu nhat la bai do, ma test khong duoc dung vao.
        assumeTrue(gate.baiDangCho().isEmpty())
        gate.markPending(daBo, 0L)
        gate.markPending(conCho, 0L)

        assertEquals("Đã từ chối bài đó.", ThiHanhLenh.tuChoi(context, gate, null, ""))
        assertEquals(listOf(conCho), gate.baiDangCho().map { it.id })
    }
}
