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
 * Ba quyen sach bai tap (Toan 8 hai tap, KHTN 8) nap tu assets co dung duoc khong.
 *
 * Giong [NganHangVanTest]: khong xoa gi, chi nap sach roi doc lai, nen chay tren may
 * ao dang dung do cung duoc.
 *
 * Ma cau SBT la so in trong sach, nhung ba kieu ma do nha minh dat them: y tach rieng
 * ("1.3a"), trac nghiem kem trang ("Trắc nghiệm 1 (tr.17)"), on cuoi nam ("Ôn cuối năm
 * 5b"). Ghim vai cau lam coc de file bi sua nham thi test bao ngay.
 */
@RunWith(AndroidJUnit4::class)
class NganHangSbtTest {

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

    private val MA_SBT = Regex("""^(\d+\.\d+[a-z]?|Trắc nghiệm \d+ \(tr\.\d+\)|Ôn cuối năm \d+[a-z]?)$""")

    @Test
    fun mon_toan_co_bon_quyen_sgk_truoc_sbt() {
        assertEquals(
            listOf("toan8t1", "toan8t2", "sbttoan8t1", "sbttoan8t2"),
            NganHang.sachCua("Toán").map { it.nguon }
        )
    }

    @Test
    fun ca_hai_tap_nap_du_va_ma_cau_dung_dang() {
        for ((nguon, trangCuoi) in listOf("sbttoan8t1" to 77, "sbttoan8t2" to 83)) {
            val soCau = kho.soCauCua(nguon)
            assertTrue("$nguon nap thieu cau: $soCau", soCau > 250)

            // Moi muc "Ôn tập chương" la mot ten bai rieng. Hai muc trung ten thi cacCau
            // tra ve cau cua ca hai, va tong o day vuot so cau trong kho.
            val tatCa = tatCaCau(nguon)
            assertEquals("$nguon: co hai muc trung ten", soCau, tatCa.size)

            assertTrue("co cau thieu de", tatCa.all { it.de.isNotBlank() })
            assertTrue("co cau thieu trang", tatCa.all { it.trang in 1..trangCuoi })
            assertEquals("ma cau bi trung", tatCa.size, tatCa.map { it.ma }.toSet().size)
            tatCa.forEach { assertTrue("${it.id} sai dang ma", MA_SBT.matches(it.ma)) }
            assertTrue(tatCa.all { it.mon == "Toán" })

            // Trac nghiem thi dang TRAC_NGHIEM, va trang ghi trong ma phai la trang that.
            tatCa.filter { it.nhom == "Trắc nghiệm" }.forEach {
                assertEquals(it.id, "TRAC_NGHIEM", it.dang)
                assertTrue(it.id, it.ma.endsWith("(tr.${it.trang})"))
            }
        }
    }

    /**
     * Cac y cua mot bai nam cung mot trang. Co giao giao "bai 1.3 trang 7"; y b in tran
     * sang trang 8 ma ghi trang 8 thi con chon trang 7 se thieu y do.
     */
    @Test
    fun moi_y_cua_mot_bai_cung_trang_in_so_bai() {
        for (nguon in listOf("sbttoan8t1", "sbttoan8t2", "sbtkhtn8")) {
            tatCaCau(nguon)
                .filter { it.ma.last().isLetter() }
                .groupBy { it.ma.dropLast(1) }
                .forEach { (bai, cac) ->
                    assertEquals("$nguon $bai: cac y khac trang", 1, cac.map { it.trang }.toSet().size)
                }
        }
    }

    @Test
    fun cung_so_bai_voi_sgk_nhung_la_hai_cau_khac_nhau() {
        val sgk = kho.cauTheoId("toan8t1:1.3a")
        val sbt = kho.cauTheoId("sbttoan8t1:1.3a")
        assertTrue(sgk?.de.orEmpty().contains("A = (−2)x^2y(1/2)xy"))
        assertTrue(sbt?.de.orEmpty().contains("M = (1/2)x^2y(−4)y"))
        assertNotEquals(sgk?.id, sbt?.id)
        // Ma SBT la ma in trong sach, nen con thay dung so do tren man hinh.
        assertEquals("1.3a", sbt?.nhan())
    }

    @Test
    fun ma_cau_khong_troi() {
        val tn = kho.cauTheoId("sbttoan8t1:Trắc nghiệm 1 (tr.17)")
        assertEquals("TRAC_NGHIEM", tn?.dang)
        assertEquals(17, tn?.trang)
        assertTrue(tn?.de.orEmpty().contains("3xy^5(−(2/3)x^3y^2z)"))
        assertEquals("Trắc nghiệm 1 (tr.17)", tn?.nhan())

        assertTrue(kho.cauTheoId("sbttoan8t1:1.32b")?.de.orEmpty().contains(": (5/6)xy^2"))
        assertEquals(10, kho.cauTheoId("sbttoan8t2:6.20b")?.trang)
        assertTrue(kho.cauTheoId("sbttoan8t2:6.20b")?.de.orEmpty().contains("tại x = 103"))
        assertEquals("Bài tập ôn tập cuối năm", kho.cauTheoId("sbttoan8t2:Ôn cuối năm 1a")?.bai)
    }

    @Test
    fun mon_khtn_co_hai_quyen_sgk_truoc_sbt() {
        assertEquals(
            listOf("khtn8", "sbtkhtn8"),
            NganHang.sachCua("Khoa học tự nhiên").map { it.nguon }
        )
    }

    /**
     * SBT KHTN dung so in cua sach lam ma, khac SGK KHTN phai tu dat ma ("B2.C1").
     *
     * Trang lay tu muc luc tung cau chu khong tu file Word, nen ghim mot cho doi trang
     * giua bai: 37.5 con o trang 94, 37.6 da sang 95.
     */
    @Test
    fun sbt_khtn_nap_du_va_ma_la_so_in() {
        val soCau = kho.soCauCua("sbtkhtn8")
        assertTrue("sbtkhtn8 nap thieu cau: $soCau", soCau > 600)
        val tatCa = tatCaCau("sbtkhtn8")
        assertEquals("sbtkhtn8: co hai muc trung ten", soCau, tatCa.size)
        assertEquals("ma cau bi trung", tatCa.size, tatCa.map { it.ma }.toSet().size)
        assertTrue("co cau thieu de", tatCa.all { it.de.isNotBlank() })
        assertTrue("co cau thieu trang", tatCa.all { it.trang in 5..114 })
        assertTrue(tatCa.all { it.mon == "Khoa học tự nhiên" })
        // So dau cua ma la so bai: cau 11.17 phai nam trong Bai 11.
        val ma = Regex("""^(\d+)\.\d+[a-z]?$""")
        tatCa.forEach {
            val so = ma.find(it.ma)?.groupValues?.get(1)
            assertTrue("${it.id} sai dang ma", so != null)
            assertTrue("${it.id} lac bai", it.bai.startsWith("Bài $so."))
        }

        val c = kho.cauTheoId("sbtkhtn8:2.1")
        assertEquals("2.1", c?.nhan())
        assertEquals(5, c?.trang)
        assertEquals("TRAC_NGHIEM", c?.dang)
        assertEquals(94, kho.cauTheoId("sbtkhtn8:37.5")?.trang)
        assertEquals(95, kho.cauTheoId("sbtkhtn8:37.6")?.trang)
        assertTrue(kho.cauTheoId("sbtkhtn8:11.17")?.de.orEmpty().contains("5,6 g Fe"))
        // Ban Word nhan dang sai phuong an C; sua theo dap an C cua sach.
        assertTrue(kho.cauTheoId("sbtkhtn8:38.6")?.de.orEmpty().contains("C. Insulin và Glucagon."))
    }

    /** Cau sach in nham mang ngoac ghi cach hieu dung, xem [NganHang.SACH]. */
    @Test
    fun cau_in_nham_co_ghi_chu_cho_AI() {
        assertTrue(kho.cauTheoId("sbttoan8t2:9.14")?.de.orEmpty().contains("ΔABC ᔕ ΔDEF"))
        assertTrue(kho.cauTheoId("sbttoan8t2:Trắc nghiệm 2 (tr.47)")?.de.orEmpty()
            .contains("trong số học sinh nữ"))
        assertTrue(kho.cauTheoId("sbttoan8t2:Ôn cuối năm 5b")?.de.orEmpty()
            .contains("x^2 − 3x − 4"))
    }
}
