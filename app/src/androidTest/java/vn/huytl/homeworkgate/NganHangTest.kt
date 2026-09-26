package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.ai.ChamBaiJson
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.KhoBai
import org.junit.After
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Ngan hang cau hoi: cai sinh ra de chua dung cho hong ma Ba Huy thay khi chay thu.
 *
 * Trieu chung hoi do: "luc phan biet duoc bai da lam roi, luc khong". Nguyen nhan
 * la so cai lay DE BAI DO AI CHEP LAI lam khoa nhan dang, ma AI chep moi lan mot
 * khac. Bo test nay dung lai dung hai canh do - cung mot cau chep khac di, va hai
 * cau khac nhau chep ra giong nhau - roi chung minh ma cau trong sach di qua ca hai.
 *
 * CAN THAN: bo test nay xoa so cai. Dung chay tren tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class NganHangTest {

    private lateinit var context: Context
    private val now = System.currentTimeMillis()
    private val ngay = 24 * 60 * 60_000L

    /** Mon khong co that, cho quyen gia cua phan "cau nop truoc khi co sach". */
    private val MON_RIENG = "Môn thử"

    /** Mot bai gia, du de doi chieu ma khong phu thuoc vao file sach that. */
    private val bai = listOf(
        cauHoi("2.26a", "Phân tích đa thức x^2 - 6x + 9 - y^2 thành nhân tử", 0),
        cauHoi("2.26b", "Phân tích đa thức 4x^2 - y^2 + 4y - 4 thành nhân tử", 1),
        cauHoi("2.27a", "Rút gọn biểu thức", 2),
        cauHoi("2.27b", "Rút gọn biểu thức", 3),
        cauHoi("2.26d", "Phân tích đa thức x^2 - 6x thành nhân tử", 4)
    )

    private fun cauHoi(ma: String, de: String, thuTu: Int) = CauHoi(
        id = "thu:$ma",
        mon = "Toán",
        nguon = "thu",
        chuong = "Chương II. Hằng đẳng thức đáng nhớ",
        bai = "Bài 6. Hiệu hai bình phương",
        nhom = "Bài tập",
        ma = ma,
        de = de,
        trang = 36,
        dang = "CAU_NHO",
        thuTu = thuTu
    )

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(context).raw().edit().clear().commit()
        SoCaiBai.xoaHet(context)
        KhoBai.get(context).napNguon("thu", bai)
    }

    /**
     * Don quyen sach gia di sau khi chay xong.
     *
     * VI SAO CAN. Bo test nay nap mot quyen ten "thu" vao dung CAI KHO THAT cua app
     * tren may, chu khong phai mot kho rieng. Khong don thi quyen do nam lai vinh
     * vien, va no HIEN RA TREN MAN HINH: man on tap nhom cac cau theo quyen, nen o
     * do moc len mot muc ten "THU" khong ai hieu la gi. Da thay that tren may ao.
     *
     * Xoa ca cau tra loi cua quyen do, nhung CHI cua quyen do - loc theo "thu:%" chu
     * khong goi SoCaiBai.xoaHet, vi ham do quet sach so cai cua ca app.
     */
    @After
    fun tearDown() {
        val kho = KhoBai.get(context)
        runCatching { kho.napNguon("thu", emptyList()) }
        runCatching { kho.writableDatabase.delete("tra_loi", "cau_id LIKE ?", arrayOf("thu:%")) }
    }

    // ------------------------------------------------------------------ cai chinh

    @Test
    fun cung_mot_cau_ma_AI_chep_de_khac_di_van_bi_nhan_ra_la_da_lam() {
        // Hom qua: AI chep de day du.
        val homQua = ChamBaiJson.doc(
            traLoiAi("""{"ma":"2.26a","co_lam":true,"dung":true,"doc_ro":true,"so_dong":4}"""),
            bai
        )!!
        SoCaiBai.ghi(context, homQua.cac, mapOf("2.26a" to 2), now - ngay)
        assertEquals(2, SoCaiBai.phutDaCongHomNay(context, now - ngay))

        // Hom nay con chup lai chinh trang do. Lan nay AI chep de cut di mot nua -
        // ngay o duong cu la mot khoa khac, va con duoc cong gio lan hai.
        val homNay = ChamBaiJson.doc(
            traLoiAi("""{"ma":"2.26 a","co_lam":true,"dung":true,"doc_ro":true,"so_dong":4}"""),
            bai
        )!!
        assertTrue(SoCaiBai.daTraGioCua(context, homNay.cac.first(), now))
        assertEquals(0, SoCaiBai.phutDaCongHomNay(context, now))
    }

    @Test
    fun hai_cau_khac_nhau_co_de_giong_het_nhau_van_la_hai_cau() {
        // 2.27a va 2.27b cung mang de "Rút gọn biểu thức" - chuan hoa ra y het nhau.
        // Duong cu se coi cau thu hai la da lam roi va khong tra gio cho no.
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"2.27a","co_lam":true,"dung":true,"doc_ro":true,"so_dong":4}""",
                """{"ma":"2.27b","co_lam":true,"dung":true,"doc_ro":true,"so_dong":4}"""
            ),
            bai
        )!!

        SoCaiBai.ghi(context, listOf(ket.cac[0]), mapOf("2.27a" to 2), now)
        assertTrue(SoCaiBai.daTraGioCua(context, ket.cac[0], now))
        assertFalse(SoCaiBai.daTraGioCua(context, ket.cac[1], now))

        SoCaiBai.ghi(context, listOf(ket.cac[1]), mapOf("2.27b" to 2), now)
        assertEquals(4, SoCaiBai.phutDaCongHomNay(context, now))
    }

    // ----------------------------------------------------------------- doc ket qua

    @Test
    fun de_bai_lay_tu_sach_chu_khong_phai_tu_chu_AI_doc_duoc() {
        val ket = ChamBaiJson.doc(
            traLoiAi("""{"ma":"2.26a","co_lam":true,"dung":true,"doc_ro":true,"de":"chép sai"}"""),
            bai
        )!!
        assertEquals("Phân tích đa thức x^2 - 6x + 9 - y^2 thành nhân tử", ket.cac.first().de)
        assertEquals("thu:2.26a", ket.cac.first().cauId)
    }

    @Test
    fun con_khai_lam_ma_anh_khong_thay_thi_khong_vao_so() {
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"2.26a","co_lam":true,"dung":true,"doc_ro":true,"so_dong":4}""",
                """{"ma":"2.26b","co_lam":false,"dung":false,"doc_ro":true}"""
            ),
            bai
        )!!

        // Chi con cau that su co bai lam. Cau khai suong khong duoc coi la sai - ghi
        // vao thi man hinh cua con day nhung cau no chua dinh vao bao gio.
        assertEquals(listOf("2.26a"), ket.cac.map { it.ma })
        SoCaiBai.ghi(context, ket.cac, mapOf("2.26a" to 2), now)
        assertTrue(SoCaiBai.dangChoSua(context, now).isEmpty())
    }

    @Test
    fun cau_ngoai_danh_sach_van_duoc_cham_nhung_di_duong_de_bai() {
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"2.99","ngoai_danh_sach":true,"de":"Bài con tự làm thêm",
                   "co_lam":true,"dung":true,"doc_ro":true,"so_dong":6}"""
            ),
            bai
        )!!
        val cau = ket.cac.first()
        assertEquals(null, cau.cauId)
        assertEquals("Bài con tự làm thêm", cau.de)

        SoCaiBai.ghi(context, listOf(cau), mapOf("2.99" to 3), now)
        assertTrue(SoCaiBai.daTraGio(context, "Bài con tự làm thêm", now))
    }

    @Test
    fun dang_bai_lay_theo_sach_chu_khong_theo_cai_may_doan() {
        // Sach ghi 2.26a la CAU_NHO. May bao TRAC_NGHIEM thi khong nghe: dang quyet
        // dinh so phut, ma cung mot cau ba lan chay may co the ra ba dang khac nhau.
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"2.26a","co_lam":true,"dung":true,"doc_ro":true,
                   "dang":"TRAC_NGHIEM","so_dong":4}"""
            ),
            bai
        )!!
        assertEquals(vn.huytl.homeworkgate.data.DangBai.CAU_NHO, ket.cac.first().dang)
    }

    // --------------------------------------------------- may chep lai bai lam

    @Test
    fun may_chep_lai_tung_dong_con_viet() {
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"2.26a","co_lam":true,"dung":false,"doc_ro":true,"dong_sai":2,
                   "bai_lam":["x^2 - 6x + 9 - y^2","= (x-3)^2 + y^2","= (x-3-y)(x-3+y)"],
                   "so_dong":3}"""
            ),
            bai
        )!!
        val cau = ket.cac.first()
        assertEquals(3, cau.baiLam.size)
        assertEquals("= (x-3)^2 + y^2", cau.baiLam[1])
        assertEquals(2, cau.dongSai)
    }

    @Test
    fun so_dong_lay_theo_so_dong_may_chep_ra_chu_khong_theo_con_so_may_khai() {
        // So dong quy ra phut. May khai 99 dong ma chi chep ra 3 dong thi tin 3:
        // danh sach dong thi Ba Huy doc duoc, con con so thi khong ai soat.
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"2.26a","co_lam":true,"dung":true,"doc_ro":true,"so_dong":99,
                   "bai_lam":["dòng 1","dòng 2","dòng 3"]}"""
            ),
            bai
        )!!
        assertEquals(3, ket.cac.first().soDong)
    }

    @Test
    fun ban_cu_khong_co_bai_lam_thi_van_doc_duoc() {
        // Model thinh thoang tra ve thieu truong. Thieu "bai_lam" thi quay ve con so
        // "so_dong" nhu truoc, khong duoc lam hong ca lan cham.
        val ket = ChamBaiJson.doc(
            traLoiAi("""{"ma":"2.26a","co_lam":true,"dung":true,"doc_ro":true,"so_dong":4}"""),
            bai
        )!!
        assertEquals(4, ket.cac.first().soDong)
        assertTrue(ket.cac.first().baiLam.isEmpty())
        assertEquals(0, ket.cac.first().dongSai)
    }

    @Test
    fun dong_sai_khong_vuot_qua_so_dong_chep_ra() {
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"2.26a","co_lam":true,"dung":false,"doc_ro":true,"dong_sai":9,
                   "bai_lam":["dòng 1","dòng 2"]}"""
            ),
            bai
        )!!
        assertEquals(2, ket.cac.first().dongSai)
    }

    @Test
    fun bai_lam_di_xuong_so_va_doc_lai_duoc() {
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"2.26d","co_lam":true,"dung":false,"doc_ro":true,"dong_sai":1,
                   "bai_lam":["= x(x-6)","= x^2-6x"],"nhan_xet":"Dòng 1 sai"}"""
            ),
            bai
        )!!
        SoCaiBai.ghi(context, ket.cac, emptyMap(), now)

        val dong = SoCaiBai.lichSuCua(context, "thu:2.26d").first()
        assertEquals(listOf("= x(x-6)", "= x^2-6x"), dong.baiLam)
        assertEquals(1, dong.dongSai)
    }

    // --------------------------------------------------- anh chi co dap an

    @Test
    fun cau_trong_danh_sach_con_khai_thi_luon_co_de() {
        // De lay tu sach nen khong can hoi may. May co quen tra "co_de" cung khong sao.
        val ket = ChamBaiJson.doc(
            traLoiAi("""{"ma":"2.26a","co_lam":true,"dung":true,"doc_ro":true,"so_dong":3}"""),
            bai
        )!!
        assertTrue(ket.cac.first().coDe)
    }

    @Test
    fun cau_tu_do_ma_may_khong_thay_de_thi_khong_tra_gio() {
        // Canh ngay 16/9/2026: vo KHTN chi ghi "cau 1 B", khong mot dong de nao.
        val ket = ChamBaiJson.doc(
            traLoiAi(
                """{"ma":"câu 1","ngoai_danh_sach":true,"de":"","co_de":false,
                   "dung":true,"doc_ro":true,"dang":"TRAC_NGHIEM","ket_qua":"B"}"""
            ),
            bai
        )!!
        val cau = ket.cac.first()
        assertFalse("may khai khong co de ma van coi la co", cau.coDe)
        assertEquals(0, LuatCongGio.phutChoCau(cau))
    }

    @Test
    fun ca_trang_chi_co_dap_an_thi_khong_duoc_phut_nao() {
        // Hai muoi cau trac nghiem, may khai dung het - nhung khong co de. Khong chan
        // o co_de thi trang nay van ra phut (20 cau, tran 15) du khong ai cham duoc.
        val cac = (1..20).joinToString(",") { i ->
            """{"ma":"câu $i","ngoai_danh_sach":true,"de":"","co_de":false,"dung":true,
               "doc_ro":true,"dang":"TRAC_NGHIEM","ket_qua":"B"}"""
        }
        val ket = ChamBaiJson.doc(traLoiAi(cac), bai)!!
        assertEquals(20, ket.cac.size)
        assertEquals(0, LuatCongGio.tinh(ket).phut)
    }

    @Test
    fun may_khai_co_de_thi_van_tra_gio_binh_thuong() {
        // Doi chung voi test tren: cung 20 cau trac nghiem, lan nay co de.
        val cac = (1..20).joinToString(",") { i ->
            """{"ma":"câu $i","ngoai_danh_sach":true,"de":"Câu $i hỏi gì đó","co_de":true,
               "dung":true,"doc_ro":true,"dang":"TRAC_NGHIEM","ket_qua":"B"}"""
        }
        val ket = ChamBaiJson.doc(traLoiAi(cac), bai)!!
        // Mot cau mot phut, hai muoi cau cham tran moi lan nop.
        assertEquals(LuatCongGio.TRAN_TRAC_NGHIEM, LuatCongGio.tinh(ket).phut)
    }

    // ---------------------------------------------------------------- on tap

    /** Ghi mot cau: sai truoc, roi sua dung - thanh cau co the den hen on. */
    private fun lamSaiRoiSuaDung(ma: String, luiNgay: Int = 1) {
        val luc = now - luiNgay * ngay
        val sai = CauCham(ma = ma, de = "", cauId = "thu:$ma", dung = false, soDong = 4)
        SoCaiBai.ghi(context, listOf(sai), emptyMap(), luc)
        SoCaiBai.ghi(context, listOf(sai.copy(dung = true)), mapOf(ma to 2), luc)
    }

    @Test
    fun cau_vua_sua_dung_thi_chua_den_hen_on() {
        // Sua xong hom qua. On ngay hom sau thi khong phai nho lai, ma la chep lai.
        lamSaiRoiSuaDung("2.26d", luiNgay = 1)
        assertTrue(SoCaiBai.cacCauDangOn(context, now).isEmpty())
    }

    @Test
    fun cau_sua_dung_qua_ba_ngay_thi_den_hen_on() {
        lamSaiRoiSuaDung("2.26d", luiNgay = 4)
        assertEquals(listOf("thu:2.26d"), SoCaiBai.cacCauDangOn(context, now))
    }

    @Test
    fun cau_dung_ngay_tu_dau_thi_khong_can_on() {
        // Con lam dung ngay lan dau thi no da biet lam. Bat on lai chi ton thi gio.
        SoCaiBai.ghi(
            context,
            listOf(CauCham(ma = "2.26a", de = "", cauId = "thu:2.26a", dung = true, soDong = 4)),
            mapOf("2.26a" to 2),
            now - ngay
        )
        assertTrue(SoCaiBai.cacCauDangOn(context, now).isEmpty())
    }

    @Test
    fun on_tap_tra_nua_so_phut() {
        val cau = CauCham(ma = "2.26d", de = "", cauId = "thu:2.26d", dung = true, soDong = 4)
        val ket = KetQuaCham(cac = listOf(cau))
        // Bai moi: 4 dong -> 2 phut. On lai cung cau do -> 1 phut.
        assertEquals(2, LuatCongGio.tinh(ket).phut)
        assertEquals(1, LuatCongGio.tinh(ket, onTap = true).phut)
    }

    @Test
    fun on_tap_lam_tron_len_chu_khong_ve_khong() {
        // Cau toi thieu 2 phut, nua la 1 - khong duoc ra 0, khong thi con on lai de
        // lay mot con so khong, va lan sau no khong on nua.
        val cau = CauCham(ma = "2.26d", de = "", cauId = "thu:2.26d", dung = true, soDong = 3)
        assertEquals(1, LuatCongGio.tinh(KetQuaCham(cac = listOf(cau)), onTap = true).phut)
    }

    @Test
    fun on_tap_tinh_gio_moi_lan_den_hen_chu_khong_phai_moi_lan_on() {
        lamSaiRoiSuaDung("2.26d", luiNgay = 4)
        val cau = CauCham(ma = "2.26d", de = "", cauId = "thu:2.26d", dung = true, soDong = 4)

        // Lan on dau: da den hen (ba ngay), duoc nua so phut.
        SoCaiBai.ghi(context, listOf(cau), mapOf("2.26d" to 1), now, onTap = true)
        assertTrue(SoCaiBai.daOnTap(context, "thu:2.26d", now))
        assertEquals(1, SoCaiBai.phutDaCongHomNay(context, now))

        // Hom sau on lai chinh cau do: hen ke tiep la muoi ngay, chua toi, khong
        // duoc gi. Day la cho chan viec chep lai cau cu moi toi de kiem gio.
        SoCaiBai.ghi(context, listOf(cau), mapOf("2.26d" to 1), now + ngay, onTap = true)
        assertEquals(0, SoCaiBai.phutDaCongHomNay(context, now + ngay))
        assertTrue(SoCaiBai.cacCauDangOn(context, now + ngay).isEmpty())

        // Qua muoi ngay thi den hen lan hai, va lai duoc tinh.
        assertEquals(listOf("thu:2.26d"), SoCaiBai.cacCauDangOn(context, now + 11 * ngay))
    }

    // --------------------------------------------- chup lai trang cu

    private fun cauOn(ma: String, dong: List<String>) = CauCham(
        ma = ma, de = "", cauId = "thu:$ma", dung = true, soDong = dong.size, baiLam = dong
    )

    @Test
    fun bai_on_giong_het_lan_truoc_tung_dong_thi_bi_danh_dau() {
        val dong = listOf("= x(x - 3)", "= x² - 3x")
        SoCaiBai.ghi(context, listOf(cauOn("2.26d", dong)), mapOf("2.26d" to 2), now - 4 * ngay)
        // Chup lai dung trang cu: AI doc ra y het tung dong cua lan truoc.
        assertTrue(SoCaiBai.giongHetLanTruoc(context, cauOn("2.26d", dong)))
    }

    @Test
    fun lam_lai_that_thi_khong_bi_danh_dau() {
        SoCaiBai.ghi(
            context,
            listOf(cauOn("2.26d", listOf("= x(x - 3)", "= x² - 3x"))),
            mapOf("2.26d" to 2), now - 4 * ngay
        )
        // Lam lai lan nua thi chu viet khac di, may ngat dong khac di, doc ra khac.
        assertFalse(
            SoCaiBai.giongHetLanTruoc(context, cauOn("2.26d", listOf("= x(x-3)", "= x²-3x")))
        )
    }

    @Test
    fun chua_co_lan_dung_nao_thi_khong_co_gi_de_so() {
        assertFalse(SoCaiBai.giongHetLanTruoc(context, cauOn("2.26d", listOf("= x(x - 3)"))))
    }

    @Test
    fun may_khong_doc_duoc_dong_nao_thi_khong_ket_luan_gi() {
        // Khong co dong nao de so thi khong duoc suy ra la chup lai: im lang o day
        // nghia la bai van tu duyet nhu thuong.
        SoCaiBai.ghi(
            context, listOf(cauOn("2.26d", listOf("= x(x - 3)"))),
            mapOf("2.26d" to 2), now - 4 * ngay
        )
        assertFalse(SoCaiBai.giongHetLanTruoc(context, cauOn("2.26d", emptyList())))
    }

    // ------------------------------------------------------ keo so cu ve

    @Test
    fun keo_so_cu_ve_thi_thay_han_ban_trong_may() {
        // Canh cai lai app roi noi lai voi nha cu: trong may co the da co vai dong
        // tu nhung lan nop sau khi cai lai. Nap la THAY, khong gop - gop thi so phut
        // da cong trong ngay nhan doi.
        SoCaiBai.ghi(
            context,
            listOf(CauCham(ma = "moi", de = "", cauId = "thu:2.26a", dung = true, soDong = 4)),
            mapOf("moi" to 2),
            now
        )
        assertEquals(2, SoCaiBai.phutDaCongHomNay(context, now))

        val cu = listOf(
            TraLoi("thu:2.26b", "Toán", "2.26b", "đề cũ", "", listOf("dòng 1"), 0, false, true, 5, "", now),
            TraLoi("thu:2.26c", "Toán", "2.26c", "đề cũ", "", emptyList(), 0, false, false, 0, "sai dấu", now)
        )
        KhoBai.get(context).napSoCai(cu)

        // Dong vua ghi truoc do bien mat, chi con hai dong keo ve.
        assertEquals(5, SoCaiBai.phutDaCongHomNay(context, now))
        assertEquals(listOf("2.26c"), SoCaiBai.dangChoSua(context, now).map { it.ma })
        assertTrue(SoCaiBai.daTraGioTheoKhoa(context, "thu:2.26b", now))
        assertFalse(SoCaiBai.daTraGioTheoKhoa(context, "thu:2.26a", now))
    }

    @Test
    fun ghi_tra_ve_dung_nhung_dong_that_su_ghi_xuong() {
        // Ben goi day chinh danh sach nay len Firestore, nen no phai khop voi so
        // trong may: cau da xong bi bo qua thi khong duoc co mat o day.
        val cau = CauCham(ma = "2.26a", de = "", cauId = "thu:2.26a", dung = true, soDong = 4)
        assertEquals(1, SoCaiBai.ghi(context, listOf(cau), mapOf("2.26a" to 2), now).size)
        // Lan hai: cau da xong roi, khong ghi nua.
        assertEquals(0, SoCaiBai.ghi(context, listOf(cau), mapOf("2.26a" to 2), now).size)
    }

    // ------------------------------------------------- cau nop truoc khi co sach

    /**
     * Mot quyen gia mang mon rieng, de sach that trong may (Toan, KHTN, Van cung nam
     * trong kho nay) khong chen vao phep so de.
     */
    private fun napSachRieng(vararg cau: Pair<String, String>) {
        KhoBai.get(context).napNguon(
            "thu",
            cau.mapIndexed { i, (ma, de) -> cauHoi(ma, de, i).copy(mon = MON_RIENG) }
        )
    }

    /** Mot lan nop qua "Bai khac": khong co ma sach, khoa lay tu de AI chep. */
    private fun nopDuongCu(de: String, mon: String = MON_RIENG) {
        val cau = CauCham(ma = "câu 3", de = de, mon = mon, dung = true, soDong = 4)
        SoCaiBai.ghi(context, listOf(cau), mapOf("câu 3" to 2), now - ngay)
    }

    @Test
    fun cau_nop_qua_bai_khac_truoc_khi_co_sach_duoc_noi_sang_ma_sach() {
        // Nop tu hoi mon nay chua co sach. AI chep de khong kem ten van ban.
        nopDuongCu("Câu 3. Trần Quốc Toản có hành động gì khác thường khi bị quân Thánh Dực ngăn cản?")
        napSachRieng(
            "B1.C5" to "Câu 3. Trần Quốc Toản có hành động gì khác thường khi bị quân " +
                "Thánh Dực ngăn cản? (văn bản Lá cờ thêu sáu chữ vàng của Nguyễn Huy Tưởng)",
            "B1.C6" to "Câu 4. Vua Thiệu Bảo có thái độ và cách xử lí như thế nào? " +
                "(văn bản Lá cờ thêu sáu chữ vàng của Nguyễn Huy Tưởng)"
        )

        assertEquals(1, KhoBai.get(context).noiCauDuongCu())
        // Cau sach gio la da tra gio: nop lai theo sach thi khong duoc them phut nao.
        assertTrue(SoCaiBai.daTraGioTheoKhoa(context, "thu:B1.C5", now))
        assertFalse(SoCaiBai.daTraGioTheoKhoa(context, "thu:B1.C6", now))
        // Doi khoa chu khong them dong: so phut hom do van la 2.
        assertEquals(2, SoCaiBai.phutDaCongHomNay(context, now - ngay))
        // Chay lai khong con gi de doi.
        assertEquals(0, KhoBai.get(context).noiCauDuongCu())
    }

    @Test
    fun de_cu_dinh_toi_hai_cau_sach_thi_khong_noi() {
        // Hai cau nho chung mot phan dan. AI chi chep phan dan thi khong biet la cau nao.
        val dan = "Cho hai đa thức A = 2x^2y + 3xyz − 2x + 5 và B = 3xyz − 2x^2y + x − 4."
        nopDuongCu(dan)
        napSachRieng("1.17a" to "$dan Tính A + B.", "1.17b" to "$dan Tính A − B.")

        assertEquals(0, KhoBai.get(context).noiCauDuongCu())
        assertFalse(SoCaiBai.daTraGioTheoKhoa(context, "thu:1.17a", now))
        assertFalse(SoCaiBai.daTraGioTheoKhoa(context, "thu:1.17b", now))
        // Duong cu van nhan ra cau do nhu truoc.
        assertTrue(SoCaiBai.daTraGio(context, dan, now))
    }

    @Test
    fun khac_mon_thi_khong_noi() {
        val de = "Hãy khái quát chủ đề của văn bản và cho biết căn cứ vào đâu em khái quát như vậy."
        nopDuongCu(de, mon = "Mỹ thuật")
        napSachRieng("B1.C10" to "Câu 8. $de (văn bản Lá cờ thêu sáu chữ vàng của Nguyễn Huy Tưởng)")
        assertEquals(0, KhoBai.get(context).noiCauDuongCu())
    }

    @Test
    fun keo_so_cu_ve_thi_noi_lai_ngay() {
        // Firestore van giu khoa cu. Keo ve xong ma doi den lan mo app sau moi noi thi
        // trong khoang do con nop lai cau cu duoc.
        val de = "Hãy khái quát chủ đề của văn bản và cho biết căn cứ vào đâu em khái quát như vậy."
        napSachRieng("B1.C10" to "Câu 8. $de (văn bản Lá cờ thêu sáu chữ vàng của Nguyễn Huy Tưởng)")
        val khoaCu = SoCaiBai.DAU_NGOAI_SACH + SoCaiBai.chuanHoa(de)
        KhoBai.get(context).napSoCai(
            listOf(TraLoi(khoaCu, MON_RIENG, "câu 8", de, "", emptyList(), 0, false, true, 2, "", now))
        )
        assertTrue(SoCaiBai.daTraGioTheoKhoa(context, "thu:B1.C10", now))
    }

    // ----------------------------------------------------------------- kho va pham vi

    @Test
    fun kho_tra_ve_cac_cau_theo_dung_thu_tu_in_trong_sach() {
        val kho = KhoBai.get(context)
        // Hoi nguoc thu tu, van phai ra dung thu tu sach.
        val ra = kho.cacCauTheoId(listOf("thu:2.27b", "thu:2.26a"))
        assertEquals(listOf("2.26a", "2.27b"), ra.map { it.ma })
    }

    @Test
    fun cac_bai_gom_dung_so_cau() {
        val cac = KhoBai.get(context).cacBaiCua("thu")
        assertEquals(1, cac.size)
        assertEquals(5, cac.first().soCau)
        assertEquals(36, cac.first().trang)
    }

    @Test
    fun pham_vi_di_qua_json_ma_khong_mat_gi() {
        val goc = PhamVi(
            mon = "Toán",
            nguon = "thu",
            tenNguon = "Sách thử",
            bai = "Bài 6 (trang 36)",
            cauIds = listOf("thu:2.26a", "thu:2.26b")
        )
        val ve = PhamVi.tuJson(goc.sangJson())
        assertEquals(goc, ve)
        assertTrue(ve!!.theoSach)
        // Khong co cau nao thi khong con la lan nop theo sach - phai quay ve duong cu.
        assertFalse(goc.copy(cauIds = emptyList()).theoSach)
    }

    // -------------------------------------------------------------- sach that

    /**
     * Quyen Toan 8 tap mot nap tu assets co that su dung duoc khong.
     *
     * Kiem ba thu de bat ba kieu hong khac nhau cua file ngan hang: file loi hay
     * thieu thi so cau tut han xuong; mot cau thieu de bai thi cau lenh gui cho AI
     * co mot dong rong, va may khong biet duong nao ma cham; ma cau trung nhau la
     * hai bai khac nhau dung chung mot khoa - dung cai lo ma ca ban nay sinh ra de
     * bit lai.
     */
    @Test
    fun sach_toan_8_tap_mot_nap_duoc_va_dung_duoc() {
        NganHang.napNeuCan(context)
        val kho = KhoBai.get(context)
        assertTrue("nap thieu cau", kho.soCauCua("toan8t1") > 300)

        val cacBai = NganHang.cacBai(context, "toan8t1")
        assertTrue("nap thieu bai", cacBai.size > 30)
        // Bai dau tien phai la bai dau sach: thu tu trong ngan hang la thu tu in.
        assertEquals("Bài 1. Đơn thức", cacBai.first().bai)

        val tatCa = cacBai.flatMap { NganHang.cacCau(context, "toan8t1", it.bai) }
        assertTrue("co cau thieu de", tatCa.all { it.de.isNotBlank() })
        assertTrue("co cau thieu trang", tatCa.all { it.trang in 1..130 })
        assertEquals("ma cau bi trung", tatCa.size, tatCa.map { it.ma }.toSet().size)
        assertEquals("ma cau khong khop id", tatCa.size, tatCa.map { it.id }.toSet().size)

        // Mot cau co that, de chac la noi dung di den noi chu khong chi dung so luong.
        val c = kho.cauTheoId("toan8t1:2.26a")
        assertEquals("2.26a", c?.ma)
        assertTrue(c?.de.orEmpty().contains("x^2 − 6x + 9 − y^2"))
    }

    /** Toan 8 tap hai: cung mot mon voi tap mot, nen phai la hai quyen tach han. */
    @Test
    fun sach_toan_8_tap_hai_nap_duoc_va_dung_duoc() {
        NganHang.napNeuCan(context)
        val kho = KhoBai.get(context)
        assertTrue("nap thieu cau", kho.soCauCua("toan8t2") > 250)

        val cacBai = NganHang.cacBai(context, "toan8t2")
        assertTrue("nap thieu bai", cacBai.size > 25)
        assertEquals("Bài 21. Phân thức đại số", cacBai.first().bai)

        val tatCa = cacBai.flatMap { NganHang.cacCau(context, "toan8t2", it.bai) }
        assertTrue("co cau thieu de", tatCa.all { it.de.isNotBlank() })
        assertTrue("co cau thieu trang", tatCa.all { it.trang in 1..140 })
        assertEquals("ma cau bi trung", tatCa.size, tatCa.map { it.ma }.toSet().size)

        val c = kho.cauTheoId("toan8t2:7.2")
        assertEquals("7.2", c?.ma)
        assertTrue(c?.de.orEmpty().contains("5x − 4 = 0"))

        // Hai tap la hai quyen rieng: ma "7.2" cua tap hai khong duoc lan sang id cua
        // tap mot, nguoc lai cung vay. Lan la so cai khong con phan biet duoc bai nao.
        assertEquals("Toán", c?.mon)
        assertEquals("toan8t2", c?.nguon)
        assertEquals(
            listOf("toan8t1", "toan8t2", "sbttoan8t1", "sbttoan8t2"),
            NganHang.sachCua("Toán").map { it.nguon }
        )
    }

    /**
     * KHTN 8: ma cau la ma TU DAT ("B12.C3") chu khong phai ma in trong sach.
     *
     * Vi tu dat nen khong co gi ben ngoai kiem ho: neu mot ngay nao do file bi xep
     * lai hay chen them cau vao giua bai, ma cac cau sau se troi di mot nac va so
     * cai coi do la nhung cau khac han - cau da tra gio bong tro thanh cau moi. Test
     * nay ghim ba cau o ba cho khac nhau trong sach lam cai coc.
     */
    @Test
    fun sach_khtn_8_nap_duoc_va_ma_cau_khong_troi() {
        NganHang.napNeuCan(context)
        val kho = KhoBai.get(context)
        assertTrue("nap thieu cau", kho.soCauCua("khtn8") > 200)

        val cacBai = NganHang.cacBai(context, "khtn8")
        assertTrue("nap thieu bai", cacBai.size > 40)

        val tatCa = cacBai.flatMap { NganHang.cacCau(context, "khtn8", it.bai) }
        assertTrue("co cau thieu de", tatCa.all { it.de.isNotBlank() })
        assertTrue("co cau thieu trang", tatCa.all { it.trang in 1..200 })
        assertEquals("ma cau bi trung", tatCa.size, tatCa.map { it.ma }.toSet().size)
        assertTrue("ma cau sai dang", tatCa.all { Regex("""^B\d+\.C\d+$""").matches(it.ma) })

        assertEquals("Khoa học tự nhiên", tatCa.first().mon)

        // Ma tu dat thi khong duoc dua ra cho con nhin: gio sach KHTN khong co cho
        // nao ghi "B2.C1". Con thay ten o va so trang, dung thu in tren giay.
        val c = kho.cauTheoId("khtn8:B2.C1")
        assertEquals("Câu hỏi (tr.11)", c?.nhan())
        assertTrue(c?.dongChon().orEmpty().startsWith("Câu hỏi (tr.11)"))
        // Sach Toan van giu nguyen so in trong sach.
        assertEquals("2.26a", kho.cauTheoId("toan8t1:2.26a")?.nhan())

        assertTrue(kho.cauTheoId("khtn8:B3.C2")?.de.orEmpty().contains("0,25 mol"))
        assertTrue(kho.cauTheoId("khtn8:B15.C2")?.de.orEmpty().contains("350 000 N"))
        assertTrue(kho.cauTheoId("khtn8:B42.C4")?.de.orEmpty().contains("mật độ cá thể"))
    }

    /** Boc may cau JSON vao mot cau tra loi day du nhu cua Gemini. */
    private fun traLoiAi(vararg cau: String): String =
        """{"mon":"Toán","ngay_dan_do":null,"bai_duoc_giao":[],"lam_het_dan_do":false,
           "cac_cau":[${cau.joinToString(",")}],"tom_tat":"thử"}"""
}
