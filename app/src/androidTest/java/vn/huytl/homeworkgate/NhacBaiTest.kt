package vn.huytl.homeworkgate

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Buoi
import vn.huytl.homeworkgate.data.LichKiemTra
import vn.huytl.homeworkgate.data.NhacBai
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.dongbo.Duong
import vn.huytl.homeworkgate.telegram.DanDoSender
import vn.huytl.homeworkgate.ui.NhacBaiActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar

/**
 * Nhac bai theo vo dan do: moi dong han toi tiet sau cua dung mon do, nhac tu dau ngay
 * truoc buoi han (Ba Huy chon 30/9/2026).
 *
 * Ngay co dinh trong tuan 28/9/2026 (thu Hai) theo thoi khoa bieu lop 8A15: Tieng Anh hoc
 * thu Hai va thu Bay, KHTN thu Hai, thu Nam, thu Sau, Toan thu Tu, thu Nam, thu Bay, Tin
 * sang thu Sau, the duc sang thu Hai. Doi thoi khoa bieu thi sua cac ngay o day theo.
 */
@RunWith(AndroidJUnit4::class)
class NhacBaiTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun don() = NhacBai.xoaHet(context)

    private val thuHai = LocalDate.of(2026, 9, 28)

    private fun trang(ngay: LocalDate, vararg dong: Pair<String, Boolean>) =
        NhacBai.Trang(ngay, dong.map { (chu, bai) -> VoDanDo.Dong(chu, bai) })

    // ------------------------------------------------------------------ ten mon

    @Test
    fun doc_ten_mon_viet_tat_trong_vo_that() {
        assertEquals("Toán", NhacBai.monCua("TOÁN: làm luyện tập 3 trang 59"))
        assertEquals(LichKiemTra.KHTN, NhacBai.monCua("KHTN: làm bài 4 trang 27"))
        assertEquals("Ngữ văn", NhacBai.monCua("NV: soạn bài Hôm nay và ngày mai"))
        assertEquals("Ngữ văn", NhacBai.monCua("Ngữ Văn: Ôn bài tuần sau KT"))
        assertEquals("Lịch sử - Địa lý", NhacBai.monCua("LS-ĐL: Sinh hoạt ngoài trời"))
        assertEquals("Âm nhạc", NhacBai.monCua("ÂNhạc: Mang sách vở đầy đủ"))
        assertEquals("STEM", NhacBai.monCua("STEAM: Mang sách vở đầy đủ"))
        assertEquals("Trí tuệ nhân tạo", NhacBai.monCua("TTNT: Làm bài cô giao"))
        assertEquals("Tiếng Anh", NhacBai.monCua("Tiếng Anh: tiết sau kiểm tra từ vựng"))
        assertEquals("Công nghệ", NhacBai.monCua("CN: làm bài trang 19"))
        assertEquals("Mỹ thuật", NhacBai.monCua("Mỹ thuật: Vẽ ký họa dáng người trên giấy A4"))
        assertEquals("Giáo dục công dân", NhacBai.monCua("GDCD - học thuộc bài 2"))
        // Ten co giao sau ten mon.
        assertEquals("Toán", NhacBai.monCua("Toán (cô Lan): bài 5 trang 40"))
        // Khong co dau hai cham, nhung ten mon khong the nham.
        assertEquals("Toán", NhacBai.monCua("Toán làm bài 2 trang 36"))
        assertEquals(LichKiemTra.KHTN, NhacBai.monCua("KHTN làm bài 3.2"))
        // Dong co so thu tu hay gach dau dong.
        assertEquals("Toán", NhacBai.monCua("1. Toán: làm bài 5 trang 40"))
        assertEquals("Ngữ văn", NhacBai.monCua("- NV: soạn bài"))
    }

    @Test
    fun khong_co_ten_mon_thi_khong_doan() {
        // "Sinh" dau dong ma khong co dau hai cham: la "sinh hoạt", khong phai mon Sinh.
        assertNull(NhacBai.monCua("Sinh hoạt ngoài trời"))
        assertNull(NhacBai.monCua("Mang sách vở đầy đủ"))
        assertNull(NhacBai.monCua("Anh trai đưa đi học"))
        assertNull(NhacBai.monCua("Làm đúng nội quy nhà trường"))
        // Truoc dau hai cham khong phai ten mon nao.
        assertNull(NhacBai.monCua("Lưu ý: mang áo mưa"))
    }

    // ------------------------------------------------------------------ han

    @Test
    fun han_la_tiet_sau_cua_dung_mon() {
        fun han(mon: String?, ngay: LocalDate = thuHai) = NhacBai.hanCua(mon, ngay)!!.let { it.first to it.second.buoi }

        assertEquals(LocalDate.of(2026, 9, 30) to Buoi.CHIEU, han("Toán"))
        assertEquals(LocalDate.of(2026, 10, 1) to Buoi.CHIEU, han(LichKiemTra.KHTN))
        assertEquals(LocalDate.of(2026, 10, 3) to Buoi.CHIEU, han("Tiếng Anh"))
        assertEquals(LocalDate.of(2026, 10, 2) to Buoi.SANG, han("Tin học"))
        // Mon mot tuan mot tiet: tuan sau.
        assertEquals(LocalDate.of(2026, 10, 5) to Buoi.CHIEU, han("Kỹ năng"))
        assertEquals(LocalDate.of(2026, 10, 5) to Buoi.SANG, han(ThoiKhoaBieu.MON_THE_DUC))
        // Khong doc ra mon: buoi hoc ke tiep.
        assertEquals(LocalDate.of(2026, 9, 29) to Buoi.CHIEU, han(null))
        // Vo thu Bay: buoi ke tiep la sang thu Hai, bo chu nhat.
        assertEquals(LocalDate.of(2026, 10, 5) to Buoi.SANG, han(null, LocalDate.of(2026, 10, 3)))
        // Vo truoc Tet (nghi 1/2 toi 10/2/2027): tiet Toan dau tien sau Tet la thu Nam 11/2.
        assertEquals(LocalDate.of(2027, 2, 11) to Buoi.CHIEU, han("Toán", LocalDate.of(2027, 1, 30)))
    }

    @Test
    fun nhac_tu_dau_ngay_truoc_buoi_han_toi_luc_vao_hoc() {
        val vo = listOf(
            trang(
                thuHai,
                "Toán: làm bài 2 trang 36" to true,
                "Tiếng Anh: làm bài tập Unit 2" to true,
                "KHTN: tiết sau kiểm tra bài 2, bài 3" to false,
                "Mang sách vở đầy đủ" to false
            )
        )
        fun nhac(luc: LocalDateTime) = NhacBai.canNhac(vo, luc).map { n -> n.ngay to n.cac.map { it.chu } }

        // Toi thu Hai: chi dong khong co mon, han chieu thu Ba.
        assertEquals(
            listOf(LocalDate.of(2026, 9, 29) to listOf("Mang sách vở đầy đủ")),
            nhac(LocalDateTime.of(2026, 9, 28, 20, 0))
        )
        // Toi thu Ba: bai Toan cho chieu thu Tu. Dong cua chieu thu Ba da het.
        assertEquals(
            listOf(LocalDate.of(2026, 9, 30) to listOf("Toán: làm bài 2 trang 36")),
            nhac(LocalDateTime.of(2026, 9, 29, 20, 0))
        )
        // Toi thu Sau: bai Tieng Anh cho chieu thu Bay.
        assertEquals(
            listOf(LocalDate.of(2026, 10, 3) to listOf("Tiếng Anh: làm bài tập Unit 2")),
            nhac(LocalDateTime.of(2026, 10, 2, 19, 0))
        )
        // Vao hoc chieu thu Bay thi thoi nhac.
        assertTrue(nhac(LocalDateTime.of(2026, 10, 3, 12, 45)).isEmpty())

        // Sap toi: ca bon buoi, buoi som truoc.
        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)
            ),
            NhacBai.sapToi(vo, LocalDateTime.of(2026, 9, 28, 20, 0)).map { it.ngay }
        )
    }

    @Test
    fun cung_buoi_thi_gom_mot_nhom_bai_tap_truoc_va_dong_trung_chi_giu_mot() {
        val vo = listOf(
            trang(thuHai, "Toán: ôn hằng đẳng thức" to false, "Toán: làm bài 2 trang 36" to true),
            // Thu Ba con chep lai bai Toan chua xong, them mot bai moi.
            trang(
                LocalDate.of(2026, 9, 29),
                "Toán: làm bài 2 trang 36" to true,
                "CN: làm bài trang 19" to true
            )
        )
        val thuTu = NhacBai.canNhac(vo, LocalDateTime.of(2026, 9, 29, 21, 0)).single()
        assertEquals(LocalDate.of(2026, 9, 30), thuTu.ngay)
        assertEquals(
            listOf("Toán: làm bài 2 trang 36", "Toán: ôn hằng đẳng thức"),
            thuTu.cac.map { it.chu }
        )
        // Dong trung giu ban cua trang moi hon.
        assertEquals(LocalDate.of(2026, 9, 29), thuTu.cacBai.single().ngayVo)
        assertEquals("20260930-CHIEU", thuTu.ma)
        assertEquals("chiều thứ tư 30/9", thuTu.ten())

        // Bai Cong nghe giao thu Ba: tiet Cong nghe sau la thu Ba tuan sau.
        val cn = NhacBai.sapToi(vo, LocalDateTime.of(2026, 9, 29, 21, 0)).last()
        assertEquals(LocalDate.of(2026, 10, 6), cn.ngay)

        val chieuThuTu = ThoiKhoaBieu.buoiHocCua(Calendar.WEDNESDAY).single()
        assertEquals(2, NhacBai.choBuoi(vo, LocalDate.of(2026, 9, 30), chieuThuTu).size)
    }

    /**
     * Tin vo dan do gui Ba Huy ghi kem buoi se nhac cho tung dong: may doc nham ten mon thi
     * nhac sai buoi, va Ba Huy thay ngay tu tin nay. Trang vo that ngay 16/9/2026 (thu Tu).
     */
    @Test
    fun tin_vo_dan_do_ghi_buoi_se_nhac_cho_tung_dong() {
        val chu = DanDoSender.chuThich(
            VoDanDo.DanDo(
                ngay = "2026-09-16",
                cacDong = listOf(
                    VoDanDo.Dong("Mỹ thuật: Vẽ ký họa dáng người trên giấy A4", true, true),
                    VoDanDo.Dong("TOÁN: Làm luyện tập 3 trang 59", true, true),
                    VoDanDo.Dong("LS-ĐL: Sinh hoạt ngoài trời", false, false)
                )
            )
        )
        assertTrue(chu, chu.contains("• Mỹ thuật: Vẽ ký họa dáng người trên giấy A4 → chiều thứ tư 23/9"))
        assertTrue(chu, chu.contains("• TOÁN: Làm luyện tập 3 trang 59 → chiều thứ năm 17/9"))
        assertTrue(chu, chu.contains("• LS-ĐL: Sinh hoạt ngoài trời → chiều thứ năm 17/9"))
        assertTrue(chu, !chu.contains("trọn gói"))
    }

    /** Ban hop/nhacbai cho Bang dieu khien. Xem [Duong.D_NHAC_BAI]. */
    @Test
    fun ban_gui_dien_thoai_gom_theo_buoi() {
        val vo = listOf(trang(thuHai, "Toán: làm bài 2 trang 36" to true, "Mang sách vở đầy đủ" to false))
        val ban = DongBo.banNhacBai(NhacBai.sapToi(vo, LocalDateTime.of(2026, 9, 28, 20, 0)))!!
        val cacBuoi = ban[Duong.F_CAC_BUOI] as List<*>
        val (thuBa, thuTu) = cacBuoi.map { it as Map<*, *> }
        assertEquals("20260929-CHIEU", thuBa["ma"])
        assertEquals("2026-09-29", thuBa["ngay"])
        assertEquals("chiều thứ ba 29/9", thuBa["ten"])
        val dong = (thuTu["cac"] as List<*>).single() as Map<*, *>
        assertEquals("Toán: làm bài 2 trang 36", dong["chu"])
        assertEquals(true, dong["bai"])
        assertEquals("Toán", dong["mon"])
        assertEquals("2026-09-28", dong["ngayVo"])
        // Khong con dong nao thi xoa document.
        assertNull(DongBo.banNhacBai(emptyList()))
    }

    /**
     * Man Bài dặn dò sắp tới ke moi buoi con dong chua toi han, chu y nhu the tren Bang dieu
     * khien (Ba Huy chon 30/9/2026): ten buoi viet hoa chu dau, moi dong mot dau "•", bai tap
     * truoc, cuoi dong la ngay tren vo.
     */
    @Test
    fun man_bai_dan_do_sap_toi_viet_nhu_the_tren_dien_thoai() {
        val vo = listOf(
            trang(
                thuHai,
                "Toán: ôn hằng đẳng thức" to false,
                "Toán: làm bài 2 trang 36" to true,
                "Mang sách vở đầy đủ" to false
            )
        )
        val cac = NhacBai.sapToi(vo, LocalDateTime.of(2026, 9, 28, 20, 0))
        assertEquals(
            listOf("Chiều thứ ba 29/9", "Chiều thứ tư 30/9"),
            cac.map { NhacBaiActivity.tenBuoi(it) }
        )
        assertEquals("• Mang sách vở đầy đủ (vở 28/9)", NhacBaiActivity.cacDong(cac[0]))
        assertEquals(
            "• Toán: làm bài 2 trang 36 (vở 28/9)\n• Toán: ôn hằng đẳng thức (vở 28/9)",
            NhacBaiActivity.cacDong(cac[1])
        )

        // Mo that man hinh voi trang vo hom nay: moi buoi mot the, buoi som truoc.
        NhacBai.ghi(
            context, LocalDate.now(),
            listOf(VoDanDo.Dong("Toán: làm bài 2 trang 36", true), VoDanDo.Dong("Mang sách vở đầy đủ", false))
        )
        val mong = NhacBai.sapToi(context)
        assertTrue(mong.isNotEmpty())
        ActivityScenario.launch(NhacBaiActivity::class.java).use { sc ->
            sc.onActivity { a ->
                val ds = a.findViewById<LinearLayout>(R.id.danh_sach)
                assertEquals(mong.size, ds.childCount)
                mong.forEachIndexed { i, n ->
                    val the = ds.getChildAt(i)
                    assertEquals(NhacBaiActivity.tenBuoi(n), the.findViewById<TextView>(R.id.ten_buoi).text.toString())
                    assertEquals(NhacBaiActivity.cacDong(n), the.findViewById<TextView>(R.id.cac_dong).text.toString())
                }
                assertEquals(View.GONE, a.findViewById<View>(R.id.trong).visibility)
            }
        }
    }

    // ------------------------------------------------------------------ luu trong may

    private fun ghi(ngay: String, vararg dong: String, boNgay: String? = null, bayGio: LocalDateTime) =
        NhacBai.ghi(
            context, LocalDate.parse(ngay), dong.map { VoDanDo.Dong(it, true) },
            boNgay = boNgay?.let { LocalDate.parse(it) }, bayGio = bayGio
        )

    /**
     * Moi ngay ghi tren vo mot trang, trung ngay thi trang sau thay trang truoc (Ba Huy chon
     * 30/9/2026). Truoc do hai trang chung mot tam anh cung thay nhau, nen mot tam anh chep
     * lien hai buoi luu buoi thu hai la mat buoi thu nhat.
     */
    @Test
    fun luu_thay_trang_cung_ngay_giu_trang_khac_ngay_va_bo_trang_het_han() {
        val toiThuHai = LocalDateTime.of(2026, 9, 28, 20, 0)
        ghi("2026-09-28", "Toán: làm bài 2", bayGio = toiThuHai)
        // Chup lai vo cung ngay: thay, khong them.
        ghi("2026-09-28", "Toán: làm bài 2", "Tiếng Anh: Unit 2", bayGio = toiThuHai)
        assertEquals(2, NhacBai.docTrang(context).single().cacDong.size)

        // Vo hom kia va vo hom qua, chup chung mot luc: giu ca hai.
        ghi("2026-09-27", "Toán: làm bài 2", bayGio = toiThuHai)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 28)),
            NhacBai.docTrang(context).map { it.ngay }
        )

        // Con mo trang 27/9 ra sua ngay thanh 26/9: trang 27/9 phai di, trang 28/9 o lai.
        ghi("2026-09-26", "Toán: làm bài 2", boNgay = "2026-09-27", bayGio = toiThuHai)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 28)),
            NhacBai.docTrang(context).map { it.ngay }
        )
        assertNull(NhacBai.trangNgay(context, LocalDate.of(2026, 9, 27)))

        // Trang 29/9: KHTN han chieu thu Nam 1/10.
        ghi("2026-09-29", "KHTN: làm bài 3.2", bayGio = toiThuHai)
        assertEquals(3, NhacBai.docTrang(context).size)

        // Toan cua trang 26/9 va 28/9 han chieu thu Tu 30/9, Tieng Anh cua trang 28/9 han
        // thu Bay 3/10. Toi thu Tu thi trang 26/9 het dong, trang 28/9 con dong Tieng Anh.
        NhacBai.donDep(context, LocalDateTime.of(2026, 9, 30, 20, 0))
        assertEquals(
            listOf(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29)),
            NhacBai.docTrang(context).map { it.ngay }
        )
        NhacBai.donDep(context, LocalDateTime.of(2026, 10, 3, 20, 0))
        assertTrue(NhacBai.docTrang(context).isEmpty())
    }

}
