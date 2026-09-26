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

    /**
     * Ma anh Telegram chi vao dung ban vua gui.
     *
     * Tin vo dan do gui o luong nen. Con bam Luu lan nua trong luc tin cu dang gui thi
     * ma anh cua ban cu khong duoc de len ban moi: bai nop sau do se gui nham tam anh
     * cho Claude doi chieu.
     */
    @Test
    fun maAnhChiGhiVaoDungBanVuaGui() {
        val d = ban(LocalDate.now(), anhGia())
        VoDanDo.luu(context, d)
        VoDanDo.ghiMaAnh(context, d.luc, "ma-anh-1")
        assertEquals("ma-anh-1", VoDanDo.doc(context)!!.fileId)

        val moi = d.copy(luc = d.luc + 1, fileId = null)
        VoDanDo.luu(context, moi)
        VoDanDo.ghiMaAnh(context, d.luc, "ma-anh-cu")
        assertNull(VoDanDo.doc(context)!!.fileId)
    }

    // ------------------------------------------------------ may doc khong duoc

    private fun chiCoAnh(chup: Long = 1_000L) = VoDanDo.DanDo(
        ngay = LocalDate.now().toString(),
        cacDong = emptyList(),
        luc = chup,
        anh = anhGia().absolutePath,
        chuaDoc = true
    )

    @Test
    fun banChiCoAnhLuuXongDocLaiVanChiCoAnh() {
        val d = chiCoAnh()
        VoDanDo.luu(context, d)
        val doc = VoDanDo.doc(context)!!
        assertTrue(doc.chuaDoc)
        assertEquals(1_000L, doc.chupLuc)
        assertEquals(VoDanDo.NGUON_CON, doc.nguon)
        // Chua doc thi khong duoc hien la "khong co bai tap".
        assertTrue(doc.moTa().endsWith("chờ ba Huy đọc"))
        // Ban luu truoc khi co moc chup: moc chup la luc luu.
        val cu = VoDanDo.tuJson(org.json.JSONObject().put("ngay", "2026-09-23").put("luc", 77L))
        assertEquals(77L, cu.chupLuc)
        assertFalse(cu.chuaDoc)
    }

    private fun ketQuaClaude(chup: Long, ngay: Any? = "2026-09-23") = mapOf(
        "chupLuc" to chup,
        "ngay" to ngay,
        "cacDong" to listOf(
            mapOf("chu" to "Toán: làm bài 2 trang 36", "bai" to true),
            mapOf("chu" to "KHTN: mang sách vở", "bai" to false),
            mapOf("chu" to " ", "bai" to true)
        )
    )

    @Test
    fun ketQuaClaudeChiGhiVaoDungTamChuaDoc() {
        val cu = chiCoAnh(chup = 5_000L)
        val kq = VoDanDo.tuClaude(cu, ketQuaClaude(5_000L), bayGio = 9_000L)
        val ban = kq.ban!!
        assertFalse(ban.chuaDoc)
        assertEquals(VoDanDo.NGUON_CLAUDE, ban.nguon)
        assertEquals("2026-09-23", ban.ngay)
        assertEquals(listOf("Toán: làm bài 2 trang 36"), ban.cacBai)
        assertEquals(listOf("KHTN: mang sách vở"), ban.dongKhac)
        // Cung tam anh: giu moc chup va anh, chi doi luc luu.
        assertEquals(5_000L, ban.chupLuc)
        assertEquals(cu.anh, ban.anh)
        assertEquals(9_000L, ban.luc)
        // O tich cua Claude di vao ca mayTich, de con sua thi Ba Huy thay cho sua.
        assertTrue(ban.cacDong.none { it.conSua })

        // Tam khac, ban da co chu, ngay hong, khong co dong nao: deu khong ghi.
        assertNull(VoDanDo.tuClaude(cu, ketQuaClaude(4_000L)).ban)
        assertNull(VoDanDo.tuClaude(ban, ketQuaClaude(5_000L)).ban)
        assertNull(VoDanDo.tuClaude(cu, ketQuaClaude(5_000L, ngay = "23/9")).ban)
        assertNull(VoDanDo.tuClaude(cu, ketQuaClaude(5_000L, ngay = null)).ban)
        assertNull(
            VoDanDo.tuClaude(cu, mapOf("chupLuc" to 5_000L, "ngay" to "2026-09-23", "cacDong" to emptyList<Any>())).ban
        )
        assertNull(VoDanDo.tuClaude(null, ketQuaClaude(5_000L)).ban)
        assertTrue(VoDanDo.tuClaude(cu, ketQuaClaude(4_000L)).loi.isNotBlank())
    }

    @Test
    fun lanChamDauDocDuocVoThiGiuLaiDanhSach() {
        val cu = chiCoAnh(chup = 5_000L)
        val moi = VoDanDo.tuLanCham(cu, 5_000L, "Thứ ba, ngày 23 tháng 9 năm 2026", listOf("Bài 2.28"))!!
        assertFalse(moi.chuaDoc)
        assertEquals(VoDanDo.NGUON_LUC_CHAM, moi.nguon)
        assertEquals("2026-09-23", moi.ngay)
        assertEquals(listOf("Bài 2.28"), moi.cacBai)
        assertEquals(5_000L, moi.chupLuc)
        // Doc ra la hom do khong giao bai: van giu, va la "khong co bai tap" that.
        assertTrue(VoDanDo.tuLanCham(cu, 5_000L, "2026-09-23", emptyList())!!.cacBai.isEmpty())

        // Khong doc ra ngay, tam anh khac, hay ban da co chu: khong giu.
        assertNull(VoDanDo.tuLanCham(cu, 5_000L, null, listOf("Bài 2.28")))
        assertNull(VoDanDo.tuLanCham(cu, 4_000L, "2026-09-23", listOf("Bài 2.28")))
        assertNull(VoDanDo.tuLanCham(moi, 5_000L, "2026-09-23", listOf("Bài 2.28")))
        assertNull(VoDanDo.tuLanCham(null, 5_000L, "2026-09-23", listOf("Bài 2.28")))
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
