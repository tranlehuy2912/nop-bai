package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.NhacBai
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.telegram.DanDoSender
import java.io.File
import java.time.LocalDate

/**
 * Don ban vo cua app cu, dau tich "da chup hom nay", va tin gui Ba Huy khi con tu go.
 *
 * Tu 30/9/2026 trang da soat nam o [NhacBai] (xem NhacBaiTest), va may doc khong duoc thi
 * con tu go. Phan dang test ky nhat la [VoDanDo.donDep]: no la duong duy nhat xoa tam anh
 * trang vo cua ban cu khoi may. Hong o day thi khong ai thay - app van chay, chi la anh mot
 * trang vo cua tre con nam lai trong may qua het tuan.
 */
@RunWith(AndroidJUnit4::class)
class VoDanDoTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun don() {
        VoDanDo.donDep(context)
        NhacBai.xoaHet(context)
    }

    private fun anhGia(): File =
        File(context.cacheDir, "vo_thu_${System.nanoTime()}.jpg").apply { writeText("x") }

    private fun ban(ngay: LocalDate, anh: File?) = VoDanDo.DanDo(
        ngay = ngay.toString(),
        cacDong = listOf(
            VoDanDo.Dong("Toán: làm bài 2 trang 36", laBaiTap = true, mayTich = false),
            VoDanDo.Dong("KHTN: mang sách vở đầy đủ", laBaiTap = false, mayTich = false)
        ),
        anh = anh?.absolutePath
    )

    @Test
    fun banGhiRaJsonDocLaiDuNguyen() {
        val d = ban(LocalDate.now(), anhGia()).copy(nguon = VoDanDo.NGUON_TU_GO)
        val doc = VoDanDo.tuJson(VoDanDo.sangJson(d))
        assertEquals(d.ngay, doc.ngay)
        assertEquals(listOf("Toán: làm bài 2 trang 36"), doc.cacBai)
        assertEquals(listOf("KHTN: mang sách vở đầy đủ"), doc.dongKhac)
        assertEquals(d.anh, doc.anh)
        assertEquals(VoDanDo.NGUON_TU_GO, doc.nguon)
        // Dong con tich khac may thi phai giu lai dau vet, tin cho Ba Huy dua vao do.
        assertTrue(doc.cacDong[0].conSua)
        assertFalse(doc.cacDong[1].conSua)
        File(d.anh!!).delete()
    }

    /**
     * Ban luu bang app truoc 30/9/2026 la "vo dang dung" co chu. Lan don dep dau tien chep
     * no sang [NhacBai] (neu ngay do chua co trang) roi bo khoi cho cu, ca anh.
     */
    @Test
    fun banCuCoChuThiChepSangNhacBaiRoiBo() {
        val homNay = LocalDate.now()
        val f = anhGia()
        VoDanDo.ghiBanCu(context, ban(homNay, f))
        VoDanDo.donDep(context)
        assertNull(VoDanDo.docBanCu(context))
        assertFalse(f.exists())
        assertEquals(
            listOf("Toán: làm bài 2 trang 36", "KHTN: mang sách vở đầy đủ"),
            NhacBai.docTrang(context).single().cacDong.map { it.chu }
        )

        // Ngay do da co trang (con da luu bang ban moi) thi trang do thang, ban cu chi bi bo.
        NhacBai.xoaHet(context)
        NhacBai.ghi(context, homNay, listOf(VoDanDo.Dong("Toán: làm bài 5", true)))
        VoDanDo.ghiBanCu(context, ban(homNay, anhGia()))
        VoDanDo.donDep(context)
        assertEquals(listOf("Toán: làm bài 5"), NhacBai.docTrang(context).single().cacDong.map { it.chu })
        assertNull(VoDanDo.docBanCu(context))
    }

    /**
     * Tam anh cu dang cho Ba Huy nho Claude doc: khong con ai doc no nua, bo ca anh va khong
     * dong vao [NhacBai].
     */
    @Test
    fun tamChoDocCuBiBoCaAnh() {
        val f = anhGia()
        VoDanDo.ghiBanCu(
            context,
            VoDanDo.DanDo(ngay = LocalDate.now().toString(), cacDong = emptyList(), anh = f.absolutePath, chuaDoc = true)
        )
        VoDanDo.donDep(context)
        assertNull(VoDanDo.docBanCu(context))
        assertFalse(f.exists())
        assertTrue(NhacBai.docTrang(context).isEmpty())
    }

    /** File anh mo coi trong filesDir/dando: xoa khi cu hon mot ngay. */
    @Test
    fun donDepXoaAnhMoCoiCuHonMotNgay() {
        val kho = File(context.filesDir, VoDanDo.THU_MUC_ANH).apply { mkdirs() }
        val bayGio = System.currentTimeMillis()
        val cu = File(kho, "vo-thucu.jpg").apply { writeText("x"); setLastModified(bayGio - 2 * 86_400_000L) }
        val moi = File(kho, "vo-thumoi.jpg").apply { writeText("x") }
        // File khong do man vo dan do dat ten (anh day tay cho ManualDanDo) thi khong dong vao.
        val tay = File(kho, "trang1.jpg").apply { writeText("x"); setLastModified(bayGio - 2 * 86_400_000L) }
        VoDanDo.donDep(context, bayGioMs = bayGio)
        assertFalse("file mo coi cu phai bi xoa", cu.exists())
        assertTrue("file vua ghi co the dang gui, phai o lai", moi.exists())
        assertTrue(tay.exists())
        moi.delete()
        tay.delete()
    }

    @Test
    fun dauTichTinhTheoNgayBamLuu() {
        val homNay = LocalDate.of(2026, 9, 30)
        VoDanDo.ghiDaLuu(context, homNay)
        assertTrue(VoDanDo.daLuuHomNay(context, homNay))
        assertFalse(VoDanDo.daLuuHomNay(context, homNay.plusDays(1)))
        VoDanDo.ghiDaLuu(context)
    }

    /**
     * Trang con tu go: tin gui Ba Huy noi ro may khong doc duoc, va khong ke tung dong la
     * "tự thêm dòng" vi dong nao cung la con go.
     */
    @Test
    fun tinTrangTuGoNoiRoVaKhongKeTuThem() {
        val d = VoDanDo.DanDo(
            ngay = "2026-09-16",
            cacDong = listOf(
                VoDanDo.Dong("TOÁN: Làm luyện tập 3 trang 59", true),
                VoDanDo.Dong("LS-ĐL: Sinh hoạt ngoài trời", false)
            ),
            nguon = VoDanDo.NGUON_TU_GO
        )
        val chu = DanDoSender.chuThich(d)
        assertTrue(chu, chu.contains("Lê Hòa tự gõ vở dặn dò ngày 16/9"))
        assertTrue(chu, chu.contains("Máy không đọc được ảnh"))
        assertTrue(chu, chu.contains("• TOÁN: Làm luyện tập 3 trang 59"))
        assertFalse(chu, chu.contains("tự thêm dòng"))

        // Trang may doc thi dong con them van duoc ke ra.
        val soat = DanDoSender.chuThich(d.copy(nguon = VoDanDo.NGUON_CON))
        assertTrue(soat, soat.contains("soát xong vở dặn dò"))
        assertTrue(soat, soat.contains("+ tự thêm dòng: TOÁN: Làm luyện tập 3 trang 59"))
    }
}
