package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.VoDanDo
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Vong doi cua ban vo dan do, nhat la luc no bien mat.
 *
 * Phan dang test ky nhat la [VoDanDo.donDep]: no la duong duy nhat xoa tam anh
 * trang vo khoi may. Hong o day thi khong ai thay - app van chay, chi la anh mot
 * trang vo cua tre con nam lai trong may qua het tuan.
 */
@RunWith(AndroidJUnit4::class)
class VoDanDoTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun don() = VoDanDo.xoa(context)

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
    fun luuXongDocLaiDuNguyen() {
        val f = anhGia()
        VoDanDo.luu(context, ban(LocalDate.now(), f))

        val doc = VoDanDo.doc(context)
        assertNotNull(doc)
        assertEquals(LocalDate.now().toString(), doc!!.ngay)
        assertEquals(listOf("Toán: làm bài 2 trang 36"), doc.cacBai)
        assertEquals(listOf("KHTN: mang sách vở đầy đủ"), doc.dongKhac)
        assertEquals(f.absolutePath, doc.anh)
        // Dong con tich khac may thi phai giu lai dau vet, tin cho Ba Huy dua vao do.
        assertTrue(doc.cacDong[0].conSua)
        assertFalse(doc.cacDong[1].conSua)
    }

    @Test
    fun donDepGiuBanConHanVaXoaBanHetHan() {
        val fCon = anhGia()
        VoDanDo.luu(context, ban(LocalDate.now(), fCon))
        VoDanDo.donDep(context)
        assertNotNull("ban cua hom nay phai o lai", VoDanDo.doc(context))
        assertTrue("anh cua ban con han phai o lai", fCon.exists())

        val fCu = anhGia()
        VoDanDo.luu(context, ban(LocalDate.now().minusDays(9), fCu))
        VoDanDo.donDep(context)
        assertNull("ban qua han phai bi xoa", VoDanDo.doc(context))
        assertFalse("anh cua ban qua han phai bi xoa theo", fCu.exists())
    }

    /**
     * Vo dan do hom qua van dung duoc vao buoi sang, va don dep phai theo dung luat
     * do chu khong tu dat mot moc khac - xem [vn.huytl.homeworkgate.data.LuatCongGio].
     */
    @Test
    fun banHomQuaConDungDuocVaoBuoiSang() {
        val f = anhGia()
        val homQua = LocalDate.now().minusDays(1)
        VoDanDo.luu(context, ban(homQua, f))

        val sang = LocalDateTime.of(LocalDate.now(), java.time.LocalTime.of(8, 0))
        assertNotNull(VoDanDo.conHieuLuc(context, sang))
        VoDanDo.donDep(context, sang)
        assertNotNull("sang hom sau van con dung duoc", VoDanDo.doc(context))
        assertTrue(f.exists())
    }

    /**
     * Qua ngay hoc moi thi ban cu KHONG duoc dung lai.
     *
     * Day la cho de sai nhat cua ca duong nay: neu ban thu Hai con song sang chieu
     * thu Ba thi bai co giao thu Ba bi cham theo danh sach cua thu Hai, va con hoac
     * mat oan tron goi hoac duoc no cho mot hom khac han.
     */
    @Test
    fun quaNgayHocMoiThiBanCuHetHan() {
        val f = anhGia()
        val homQua = LocalDate.now().minusDays(1)
        VoDanDo.luu(context, ban(homQua, f))

        // Chieu hom sau, sau moc LuatCongGio.GIO_HET_HAN_SANG.
        val chieu = LocalDateTime.of(LocalDate.now(), java.time.LocalTime.of(17, 0))
        assertNull("chieu hom sau ban cu khong con dung duoc", VoDanDo.conHieuLuc(context, chieu))

        VoDanDo.donDep(context, chieu)
        assertNull("va phai bi don di", VoDanDo.doc(context))
        assertFalse(f.exists())
    }

    @Test
    fun xoaThiAnhDiTheo() {
        val f = anhGia()
        VoDanDo.luu(context, ban(LocalDate.now(), f))
        VoDanDo.xoa(context)
        assertNull(VoDanDo.doc(context))
        assertFalse(f.exists())
    }
}
