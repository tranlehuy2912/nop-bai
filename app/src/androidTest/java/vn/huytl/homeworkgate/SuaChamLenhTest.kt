package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh

/**
 * Lenh SUACHAM tu Bang dieu khien, voi dung kieu du lieu Firestore dua vao "giaTri".
 *
 * Khac [SuaChamTest] o cho di qua ca phan cap gio cua [GateStore]. Nen ket qua tuy
 * gio: trong gio ngu thi may tu choi cap gio, va luc do so phai giu nguyen cau cho sua
 * de Ba Huy gui lai sau. Ca hai nhanh deu duoc kiem.
 *
 * Don dep sau khi chay: xoa so, va dong phieu gio vua cap. Khong xoa prefs.
 */
@RunWith(AndroidJUnit4::class)
class SuaChamLenhTest {

    private lateinit var context: Context
    private val de = "Rút gọn biểu thức (2x − 5y)(2x + 5y) + (2x + 5y)^2."

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        SoCaiBai.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
    }

    @After
    fun tearDown() {
        SoCaiBai.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
    }

    @Test
    fun lenh_sua_cham_bo_cau_khoi_danh_sach_sua_hoac_giu_nguyen_khi_khong_cap_duoc() {
        SoCaiBai.ghi(
            context,
            listOf(
                CauCham(
                    ma = "2.33a", de = de, ketQua = "8x^2 + 20xy", dung = false, soDong = 1,
                    baiLam = listOf("a) 8x^2 + 20xy"), cauId = "sachthu:2.33a", mon = "Toán",
                    nhanXet = "máy bảo thiếu hạng tử y^2"
                )
            ),
            emptyMap()
        )
        // Dung kieu Firestore tra ve cho mot mang cac map.
        val giaTri: List<Map<String, Any>> = listOf(mapOf("ma" to "2.33a", "de" to de))

        val tra = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)

        println("SUACHAM_LENH: $tra")
        if (tra.startsWith("Không cấp được")) {
            // Gio ngu: khong ghi gi, cau van cho sua.
            assertEquals(listOf("2.33a"), SoCaiBai.dangChoSua(context).map { it.ma })
            return
        }
        // Mot dong lam bai, cau nho toi thieu 4 phut tu 27/9/2026 (truoc do 2).
        assertTrue(tra, tra.startsWith("Đã sửa câu 2.33a thành đúng, cộng 4 phút"))
        assertTrue(SoCaiBai.dangChoSua(context).isEmpty())
        assertEquals(4, SoCaiBai.phutDaCongHomNay(context))
        assertTrue(SoCaiBai.loiNhan(context).orEmpty().contains("câu 2.33a Lê Hòa làm đúng rồi"))

        // Gui lai dung lenh do: khong cong them phut nao.
        val lan2 = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)
        assertTrue(lan2, lan2.startsWith("Không còn câu nào"))
        assertEquals(4, SoCaiBai.phutDaCongHomNay(context))
    }

    @Test
    fun lenh_thieu_danh_sach_thi_khong_lam_gi() {
        assertTrue(ThiHanhLenh.suaCham(context, GateStore(context), null).startsWith("Lệnh thiếu"))
        assertTrue(
            ThiHanhLenh.suaCham(context, GateStore(context), listOf(mapOf("de" to "không có mã")))
                .startsWith("Lệnh thiếu")
        )
    }
}
