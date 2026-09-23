package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang

/**
 * Hai quyen Ngu van 8 nap tu assets co dung duoc khong.
 *
 * TACH RIENG KHOI [NganHangTest] vi bo do xoa so cai va cau hinh truoc moi test.
 * Bo nay khong xoa gi: chi nap sach - dung viec app van lam luc khoi dong - roi doc
 * lai. Chay tren may ao dang dung do cung khong mat token hay bai da cham.
 *
 * Ma cau la ma tu dat nhu KHTN ("B3.C7"), nen cung phai ghim vai cau lam coc: file
 * bi xep lai hay chen cau vao giua thi ma troi di, so cai nhan nham cau.
 */
@RunWith(AndroidJUnit4::class)
class NganHangVanTest {

    private lateinit var context: Context
    private lateinit var kho: KhoBai

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        kho = KhoBai.get(context)
    }

    private fun tatCaCau(nguon: String): List<CauHoi> =
        NganHang.cacBai(context, nguon).flatMap { NganHang.cacCau(context, nguon, it.bai) }

    @Test
    fun hai_tap_la_hai_quyen_cua_mon_ngu_van() {
        assertEquals(
            listOf("van8t1", "van8t2"),
            NganHang.sachCua("Ngữ văn").map { it.nguon }
        )
    }

    @Test
    fun ca_hai_tap_nap_du_va_ma_cau_dung_dang() {
        for ((nguon, soTrang) in listOf("van8t1" to 134, "van8t2" to 142)) {
            val soCau = kho.soCauCua(nguon)
            assertTrue("$nguon nap thieu cau: $soCau", soCau > 180)

            // cacCau loc theo ten bai thoi, khong theo chuong. Hai muc trung ten (hai
            // "Thực hành tiếng Việt" chang han) thi moi muc tra ve cau cua ca hai, va
            // tong o day vuot so cau trong kho.
            val tatCa = tatCaCau(nguon)
            assertEquals("$nguon: co hai muc trung ten", soCau, tatCa.size)

            assertTrue("co cau thieu de", tatCa.all { it.de.isNotBlank() })
            assertTrue("co cau thieu trang", tatCa.all { it.trang in 1..soTrang })
            assertEquals("ma cau bi trung", tatCa.size, tatCa.map { it.ma }.toSet().size)
            assertTrue("ma cau sai dang", tatCa.all { Regex("""^B\d+\.C\d+$""").matches(it.ma) })
            assertTrue(tatCa.all { it.mon == "Ngữ văn" })
        }
    }

    /**
     * AI cham chi thay ma va de bai. Cau hoi sau van ban ma khong ghi ten van ban
     * thi AI khong biet dang hoi van ban nao; bai tieng Viet khong ghi ten muc thi
     * khong biet dang hoi hien tuong gi.
     */
    @Test
    fun de_bai_mang_du_ngu_canh_cho_AI_cham() {
        val tatCa = tatCaCau("van8t1") + tatCaCau("van8t2")

        val sauVanBan = tatCa.filter { it.nhom == "Trả lời câu hỏi" }
        assertTrue(sauVanBan.size > 100)
        sauVanBan.forEach {
            assertTrue("${it.id} thieu ten van ban", it.de.contains("(văn bản ") && it.de.endsWith(")"))
        }

        val tiengViet = tatCa.filter { it.nhom == "Thực hành tiếng Việt" }
        assertTrue(tiengViet.size > 50)
        tiengViet.forEach {
            assertTrue("${it.id} thieu ten muc", it.de.contains("(Thực hành tiếng Việt: "))
        }
    }

    @Test
    fun ma_cau_khong_troi() {
        val c = kho.cauTheoId("van8t1:B1.C3")
        // Ma tu dat khong dua ra cho con: con thay ten muc va so trang.
        assertEquals("Trả lời câu hỏi (tr.15)", c?.nhan())
        assertTrue(c?.de.orEmpty().startsWith("Câu 1. Tóm tắt nội dung của văn bản"))
        assertTrue(c?.de.orEmpty().endsWith("(văn bản Lá cờ thêu sáu chữ vàng của Nguyễn Huy Tưởng)"))

        assertTrue(kho.cauTheoId("van8t1:B1.C14")?.de.orEmpty()
            .contains("(in đậm: làm xe; chim mòng, nhà đi săn, viên đạn)"))
        assertTrue(kho.cauTheoId("van8t1:B4.C28")?.de.orEmpty()
            .contains("Đế vương lấy hiếu trị thiên hạ"))
        assertTrue(kho.cauTheoId("van8t2:B9.C11")?.de.orEmpty()
            .contains("A-lớt-xtơ Phơ-dơ-gheo"))
        assertTrue(kho.cauTheoId("van8t2:B10.C17")?.de.orEmpty()
            .startsWith("Viết bài thuyết minh giới thiệu cuốn sách yêu thích"))

        // On tap hoc ki mang so bai 0 o ca hai tap. Hai quyen la hai nguon rieng nen
        // "B0.C1" cua tap mot va cua tap hai la hai cau khac nhau.
        val otMot = kho.cauTheoId("van8t1:B0.C1")
        val otHai = kho.cauTheoId("van8t2:B0.C1")
        assertTrue(otMot?.de.orEmpty().contains("học kì I,"))
        assertNotEquals(otMot?.de, otHai?.de)
    }

    /** Dang quyet dinh so phut, va dang cua sach thang dang AI doan. */
    @Test
    fun dang_bai_theo_sach() {
        assertEquals("VIET_DAI", kho.cauTheoId("van8t1:B1.C11")?.dang)
        assertEquals("VIET_DAI", kho.cauTheoId("van8t1:B1.C35")?.dang)
        // Bai tieng Viet ma bat viet doan van thi tinh nhu bai viet.
        assertEquals("VIET_DAI", kho.cauTheoId("van8t2:B6.C13")?.dang)
        assertEquals("TRAC_NGHIEM", kho.cauTheoId("van8t2:B0.C11")?.dang)
        assertTrue(kho.cauTheoId("van8t2:B0.C11")?.de.orEmpty().contains("(in đậm: Tâu bệ hạ)"))
        assertEquals("CAU_NHO", kho.cauTheoId("van8t2:B7.C3")?.dang)
    }
}
