package vn.huytl.homeworkgate

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

    private fun trang(ngay: LocalDate, vararg dong: Pair<String, Boolean>, chupLuc: Long = 0L) =
        NhacBai.Trang(ngay, dong.map { (chu, bai) -> VoDanDo.Dong(chu, bai) }, chupLuc)

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
        assertTrue(chu, chu.contains("· LS-ĐL: Sinh hoạt ngoài trời → chiều thứ năm 17/9"))
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

    // ------------------------------------------------------------------ luu trong may

    private fun danDo(ngay: String, chupLuc: Long, vararg dong: String, chuaDoc: Boolean = false) =
        VoDanDo.DanDo(
            ngay = ngay,
            cacDong = dong.map { VoDanDo.Dong(it, true) },
            chupLuc = chupLuc,
            chuaDoc = chuaDoc
        )

    @Test
    fun luu_thay_trang_cung_ngay_hay_cung_tam_anh_va_bo_trang_het_han() {
        val toiThuHai = LocalDateTime.of(2026, 9, 28, 20, 0)
        NhacBai.ghi(context, danDo("2026-09-28", 1L, "Toán: làm bài 2"), toiThuHai)
        // Soat lai trang do: thay, khong them.
        NhacBai.ghi(context, danDo("2026-09-28", 1L, "Toán: làm bài 2", "Tiếng Anh: Unit 2"), toiThuHai)
        assertEquals(2, NhacBai.docTrang(context).single().cacDong.size)

        // Sua ngay cua chinh tam anh do: ban ngay cu phai di.
        NhacBai.ghi(context, danDo("2026-09-27", 1L, "Toán: làm bài 2"), toiThuHai)
        assertEquals(listOf(LocalDate.of(2026, 9, 27)), NhacBai.docTrang(context).map { it.ngay })

        // Trang khac cua ngay khac: giu ca hai.
        NhacBai.ghi(context, danDo("2026-09-29", 2L, "KHTN: làm bài 3.2"), toiThuHai)
        assertEquals(2, NhacBai.docTrang(context).size)

        // Ban chi co anh: chua co gi de nhac, khong dong vao so.
        NhacBai.ghi(context, danDo("2026-09-30", 3L, chuaDoc = true), toiThuHai)
        assertEquals(2, NhacBai.docTrang(context).size)

        // Trang 27/9 chi co bai Toan, han chieu thu Tu 30/9: toi thu Tu la qua han, bo trang.
        // Trang 29/9: KHTN han chieu thu Nam 1/10, con giu.
        NhacBai.donDep(context, LocalDateTime.of(2026, 9, 30, 20, 0))
        assertEquals(listOf(LocalDate.of(2026, 9, 29)), NhacBai.docTrang(context).map { it.ngay })
        NhacBai.donDep(context, LocalDateTime.of(2026, 10, 1, 13, 0))
        assertTrue(NhacBai.docTrang(context).isEmpty())
    }
}
