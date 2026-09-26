package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.ChatCu
import vn.huytl.homeworkgate.data.Prefs
import java.io.File

/**
 * Xoa phan con lai cua khung chat cu trong may: cac cau trong prefs va thu muc anh.
 * Nhung khoa khac trong prefs phai con nguyen.
 */
@RunWith(AndroidJUnit4::class)
class ChatCuTest {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Test
    fun xoa_tin_va_anh_cu_ma_khong_dong_vao_khoa_khac() {
        val sp = Prefs.get(context).raw()
        sp.edit()
            .putString("chat_lines", "[{\"f\":\"CON\",\"t\":\"thu\",\"a\":1}]")
            .putLong("chat_read_at", 1L)
            .putLong("chat_last_sent", 2L)
            .putLong("chat_wait_until", 3L)
            .putString("thu_chat_cu_giu_lai", "con")
            .commit()
        val thuMuc = File(context.filesDir, ChatCu.THU_MUC_ANH).apply { mkdirs() }
        File(thuMuc, "con_1.jpg").writeText("anh thu")

        ChatCu.xoaTrongMay(context)

        listOf("chat_lines", "chat_read_at", "chat_last_sent", "chat_wait_until").forEach {
            assertFalse("con khoa $it", sp.contains(it))
        }
        assertFalse(thuMuc.exists())
        assertEquals("con", sp.getString("thu_chat_cu_giu_lai", null))
        sp.edit().remove("thu_chat_cu_giu_lai").commit()
    }

    /** App khoi dong nhieu lan trong ngay: goi lai khi khong con gi thi khong hong gi. */
    @Test
    fun goi_lai_khi_da_sach_khong_hong() {
        ChatCu.xoaTrongMay(context)
        ChatCu.xoaTrongMay(context)
        assertFalse(File(context.filesDir, ChatCu.THU_MUC_ANH).exists())
    }
}
