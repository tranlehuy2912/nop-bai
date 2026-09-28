package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.NhatKyAi
import vn.huytl.homeworkgate.telegram.TelegramClient

/**
 * Viet cau con go thanh mot dong cua so hoi AI, giu lai cho xuong dong, va dung tin /hoi
 * tu so.
 *
 * Chi goi [NhatKyAi.motDong], [NhatKyAi.traXuongDong], [NhatKyAi.tinHoi], khong dung toi
 * prefs: chay tren may nao cung khong xoa hay ghi them gi vao so that.
 */
@RunWith(AndroidJUnit4::class)
class NhatKyAiTest {

    @Test
    fun cau_mot_dong_chi_bo_khoang_trang_hai_dau() {
        assertEquals("giải bài 2.26 sgk toán 8", NhatKyAi.motDong("  giải bài 2.26 sgk toán 8 \n"))
    }

    /** Dung kieu de bai con dan vao ngay 28/9/2026: moi y mot dong, co ca CRLF. */
    @Test
    fun moi_lan_xuong_dong_thanh_mot_dau() {
        assertEquals(
            "Cho tam giác ABC vuông tại A, đường cao AH. ↵ a) Chứng minh AH² = HB.HC. ↵ b) Tính AH.",
            NhatKyAi.motDong(
                "Cho tam giác ABC vuông tại A, đường cao AH.\na) Chứng minh AH² = HB.HC.\r\nb) Tính AH."
            )
        )
    }

    @Test
    fun dong_trong_va_khoang_trang_quanh_cho_xuong_dong_gop_lai() {
        assertEquals("Bài 1 ↵ Bài 2", NhatKyAi.motDong("\n Bài 1  \n\n \t\n   Bài 2\n\n"))
    }

    @Test
    fun xuong_dong_kieu_unicode_cung_tinh() {
        assertEquals("a ↵ b ↵ c ↵ d", NhatKyAi.motDong("a\u2028b\u2029c\u0085d"))
    }

    /**
     * Ket qua phai la dung mot dong cua so, va khong con ky tu nao dau cham trong khuon
     * doc cua Bang dieu khien khong khop (khong thi ca dong hien nguyen ban ben do).
     */
    @Test
    fun ket_qua_luon_la_mot_dong() {
        val dong = NhatKyAi.motDong("x\ny\r\nz\rw\u2028v\u2029u\u0085t")
        assertEquals(listOf(dong), dong.lines())
        assertTrue(Regex(".*").matches(dong))
    }

    /** Tin /hoi: moi cau van mo dau bang ngay gio, cho con xuong dong thanh dong moi. */
    @Test
    fun tin_hoi_tra_lai_cho_xuong_dong() {
        val so = listOf(
            "28/09 12:56  [Gemini]  " + NhatKyAi.motDong("Cho tam giác ABC.\na) Tính AH."),
            "28/09 12:57  [Dola]  past simple của go là gì"
        ).joinToString("\n")
        assertEquals(
            "28/09 12:56  [Gemini]  Cho tam giác ABC.\na) Tính AH.\n" +
                "28/09 12:57  [Dola]  past simple của go là gì",
            NhatKyAi.traXuongDong(so)
        )
    }

    /** Dau ↵ con tu go ma khong co hai dau cach hai ben thi khong phai cho xuong dong. */
    @Test
    fun dau_con_tu_go_giu_nguyen() {
        val dong = "28/09 12:58  [ChatGPT]  phím↵ trên bàn phím là phím gì"
        assertEquals(dong, NhatKyAi.traXuongDong(dong))
    }

    /** Vua dung mot tin thi tin /hoi y nhu truoc, khong co dong bao bo cau. */
    @Test
    fun tin_hoi_vua_mot_tin_thi_giu_nguyen() {
        val so = listOf(
            "28/09 12:56  [Gemini]  " + NhatKyAi.motDong("Cho tam giác ABC.\na) Tính AH."),
            "28/09 12:57  [Dola]  past simple của go là gì"
        ).joinToString("\n")
        val tin = "Hôm nay hỏi AI:\n" + NhatKyAi.traXuongDong(so)
        assertEquals(tin, NhatKyAi.tinHoi("Hôm nay hỏi AI:", so, tin.length))
    }

    /**
     * Dai qua thi bo cau cu nhat, bo nguyen ca cau, va dong thu hai noi so cau khong hien.
     * Thieu mot chu la bo them mot cau: giu duoc bao nhieu cau moi thi giu bay nhieu.
     */
    @Test
    fun tin_hoi_dai_qua_thi_bo_cau_cu_nhat() {
        val cau = (1..5).map {
            "28/09 10:0$it  [ChatGPT]  " + NhatKyAi.motDong("câu số $it\ncó hai dòng")
        }
        val so = cau.joinToString("\n")

        val haiCau = "Hôm nay hỏi AI:\n(Tin dài quá nên không hiện 3 câu cũ nhất.)\n" +
            NhatKyAi.traXuongDong(cau[3] + "\n" + cau[4])
        assertEquals(haiCau, NhatKyAi.tinHoi("Hôm nay hỏi AI:", so, haiCau.length))

        val motCau = "Hôm nay hỏi AI:\n(Tin dài quá nên không hiện 4 câu cũ nhất.)\n" +
            NhatKyAi.traXuongDong(cau[4])
        assertEquals(motCau, NhatKyAi.tinHoi("Hôm nay hỏi AI:", so, haiCau.length - 1))
    }

    /** Cau moi nhat luon con, ke ca khi mot minh no da dai qua mot tin. */
    @Test
    fun cau_moi_nhat_luon_con() {
        val dai = "28/09 11:00  [Gemini]  " + "x".repeat(200)
        assertEquals(
            "Hôm nay hỏi AI:\n(Tin dài quá nên không hiện 1 câu cũ nhất.)\n$dai",
            NhatKyAi.tinHoi("Hôm nay hỏi AI:", "28/09 10:59  [Dola]  câu cũ\n$dai", 100)
        )
        // Chi co mot cau thi khong co gi de bo, cung khong co dong bao.
        assertEquals("Hôm nay hỏi AI:\n$dai", NhatKyAi.tinHoi("Hôm nay hỏi AI:", dai, 100))
    }

    /**
     * Cau ghi tu 16/9 den 23/9/2026 co the nam tren nhieu dong cua so, dong sau khong co
     * ngay gio. Bo cau do thi bo ca dong sau cua no, va chi dem la mot cau.
     */
    @Test
    fun cau_cu_nhieu_dong_bo_ca_cau() {
        val cu = "20/09 20:00  [Gemini]  Cho tam giác ABC.\na) Tính AH."
        val moi = "28/09 10:00  [ChatGPT]  câu mới"
        val tin = "Câu hỏi AI đã ghi:\n(Tin dài quá nên không hiện 1 câu cũ nhất.)\n$moi"
        assertEquals(tin, NhatKyAi.tinHoi("Câu hỏi AI đã ghi:", "$cu\n$moi", tin.length))
    }

    /**
     * So day 200 cau (NhatKyAi giu toi 200 dong) voi gioi han that cua Telegram: vua mot
     * tin, giu dung doan cau moi nhat, va khong bo thua.
     */
    @Test
    fun so_day_vua_mot_tin_telegram() {
        val cau = (0 until 200).map {
            "27/09 10:00  [ChatGPT]  " +
                NhatKyAi.motDong("câu số $it\n" + "giải giúp em bài này ".repeat(it % 7))
        }
        val tin = NhatKyAi.tinHoi("Câu hỏi AI đã ghi:", cau.joinToString("\n"), TelegramClient.MAX_TIN)
        assertTrue(tin.length <= TelegramClient.MAX_TIN)

        val boDi = Regex("""không hiện (\d+) câu""").find(tin)!!.groupValues[1].toInt()
        assertEquals(
            "Câu hỏi AI đã ghi:\n(Tin dài quá nên không hiện $boDi câu cũ nhất.)\n" +
                cau.drop(boDi).joinToString("\n") { NhatKyAi.traXuongDong(it) },
            tin
        )
        // Moi cau o day ngan hon 200 chu, nen con trong hon 200 chu la da bo thua cau.
        assertTrue(tin.length > TelegramClient.MAX_TIN - 200)
    }
}
