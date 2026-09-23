package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh

/**
 * Lenh CHAMBAI tu Bang dieu khien: cac cho chan truoc khi giao cho ApprovalService.
 *
 * KHONG chay nhanh cham that. Nhanh do bat service, va neu may thu co dat bot thi
 * no nhan tin Telegram that cho Ba Huy. Phan tinh phut cua nhanh do da nam o
 * [ChamTheoClaudeTest] va o cac kiem thu cua LuatCongGio.
 *
 * Don dep sau khi chay: bo bai thu khoi hang cho, va dong phieu gio.
 */
@RunWith(AndroidJUnit4::class)
class ChamBaiLenhTest {

    private lateinit var context: Context
    private lateinit var gate: GateStore
    private val id = "thu-cham-bai"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        gate = GateStore(context)
        gate.boBaiCho(id)
    }

    @After
    fun tearDown() {
        gate.boBaiCho(id)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
    }

    @Test
    fun chi_cham_bai_dang_cho_va_danh_sach_doc_duoc() {
        val giaTri: List<Map<String, Any>> =
            listOf(mapOf("ma" to "1", "dung" to true, "de" to "Tính 2 + 3.", "soDong" to 1))

        assertEquals(
            "Lệnh thiếu mã bài, máy không chấm.",
            ThiHanhLenh.chamTheoClaude(context, gate, null, giaTri)
        )
        // Bai da duyet tay hay tu choi: cham nua la cong gio hai lan cho mot bai.
        assertEquals(
            "Bài này không còn chờ duyệt nên máy không chấm nữa.",
            ThiHanhLenh.chamTheoClaude(context, gate, id, giaTri)
        )

        gate.markPending(id, 0L)
        // "dung" viet thanh chu thi khong co ket luan nao, khong duoc thanh mot lan cham.
        assertEquals(
            "Lệnh thiếu danh sách câu, máy không chấm.",
            ThiHanhLenh.chamTheoClaude(context, gate, id, listOf(mapOf("ma" to "1", "dung" to "true")))
        )
        assertEquals(
            "Lệnh thiếu danh sách câu, máy không chấm.",
            ThiHanhLenh.chamTheoClaude(context, gate, id, null)
        )
    }
}
