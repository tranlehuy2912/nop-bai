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
import vn.huytl.homeworkgate.data.NhacBai
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
    fun don() {
        VoDanDo.xoa(context)
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
     * do chu khong tu dat mot moc khac - xem [VoDanDo.conDung].
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
     * Qua luc vao buoi hoc sau thi vo can chup la vo cua buoi moi: man chinh phai hien
     * lai "Chụp vở dặn dò hôm nay", khong bay ra vo cua hom truoc.
     */
    @Test
    fun quaNgayHocMoiThiBanCuHetHan() {
        val f = anhGia()
        // Thu Hai va chieu thu Ba co dinh. Lay "hom qua" theo hom nay thi test nay hong
        // moi chu nhat: vo thu Bay dung duoc ca ngay chu nhat, xem VoDanDo.conDung.
        val homQua = LocalDate.of(2026, 9, 21)
        VoDanDo.luu(context, ban(homQua, f))

        // Chieu hom sau, sau luc vao hoc buoi chieu thu Ba (12:45).
        val chieu = LocalDateTime.of(2026, 9, 22, 17, 0)
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

    /**
     * Vo song toi luc vao buoi hoc ke tiep, theo thoi khoa bieu (30/9/2026). Truoc do vo
     * chi song toi 12 gio trua hom sau, rieng vo thu Bay toi het chu nhat: luat do coi
     * bai trong vo la bai cho ngay mai.
     */
    @Test
    fun voSongToiLucVaoBuoiHocKeTiep() {
        val thuHai = LocalDate.of(2026, 9, 28)
        // Chieu thu Ba vao hoc 12:45.
        assertTrue(VoDanDo.conDung(thuHai, LocalDateTime.of(2026, 9, 28, 20, 0)))
        assertTrue(VoDanDo.conDung(thuHai, LocalDateTime.of(2026, 9, 29, 12, 30)))
        assertFalse(VoDanDo.conDung(thuHai, LocalDateTime.of(2026, 9, 29, 12, 45)))

        // Vo thu Nam: sang thu Sau co tiet Tin 9:15, nen het han luc do.
        val thuNam = LocalDate.of(2026, 10, 1)
        assertTrue(VoDanDo.conDung(thuNam, LocalDateTime.of(2026, 10, 2, 9, 0)))
        assertFalse(VoDanDo.conDung(thuNam, LocalDateTime.of(2026, 10, 2, 9, 15)))

        // Vo thu Bay: dung het chu nhat, toi 9:15 sang thu Hai (tiet the duc).
        val thuBay = LocalDate.of(2026, 10, 3)
        assertTrue(VoDanDo.conDung(thuBay, LocalDateTime.of(2026, 10, 4, 21, 0)))
        assertTrue(VoDanDo.conDung(thuBay, LocalDateTime.of(2026, 10, 5, 8, 0)))
        assertFalse(VoDanDo.conDung(thuBay, LocalDateTime.of(2026, 10, 5, 9, 30)))

        // Truoc Tet (nghi 1/2 toi 10/2/2027): vo thu Bay 30/1 dung toi chieu thu Nam 11/2.
        val truocTet = LocalDate.of(2027, 1, 30)
        assertTrue(VoDanDo.conDung(truocTet, LocalDateTime.of(2027, 2, 8, 20, 0)))
        assertFalse(VoDanDo.conDung(truocTet, LocalDateTime.of(2027, 2, 11, 13, 0)))

        // Vo ghi ngay mai thi khong: vo ghi ngay cua buoi vua hoc.
        assertFalse(VoDanDo.conDung(LocalDate.of(2026, 9, 29), LocalDateTime.of(2026, 9, 28, 20, 0)))
        assertFalse(VoDanDo.conDung(null, LocalDateTime.of(2026, 9, 28, 20, 0)))
    }

    /**
     * Luu vo la chep chu sang so nhac bai. Vo het han roi thi chu van o [NhacBai] toi tiet
     * sau cua tung mon, xem NhacBaiTest.
     */
    @Test
    fun luuVoThiChepChuSangNhacBai() {
        // Vo hom nay: dong nao cung han sau hom nay, nen chac chan con trong so.
        val homNay = LocalDate.now()
        VoDanDo.luu(context, ban(homNay, anhGia()))
        val trang = NhacBai.docTrang(context).single()
        assertEquals(homNay, trang.ngay)
        assertEquals(
            listOf("Toán: làm bài 2 trang 36", "KHTN: mang sách vở đầy đủ"),
            trang.cacDong.map { it.chu }
        )

        // Vo luu bang app cu (truoc khi co NhacBai): lan don dep dau tien chep chu sang.
        NhacBai.xoaHet(context)
        VoDanDo.donDep(context)
        assertEquals(homNay, NhacBai.docTrang(context).single().ngay)

        // Ban chi co anh thi chua co gi de nhac.
        NhacBai.xoaHet(context)
        VoDanDo.luu(context, ban(homNay, null).copy(cacDong = emptyList(), chuaDoc = true))
        assertTrue(NhacBai.docTrang(context).isEmpty())
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
