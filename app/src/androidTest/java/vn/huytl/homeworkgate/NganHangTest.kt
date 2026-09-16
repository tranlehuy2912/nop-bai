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
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi

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
        // Hai muoi cau trac nghiem, may khai dung het - nhung khong co de. Luat cu
        // se tra 20/4 = 5 phut cho mot trang khong ai cham duoc.
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
        assertEquals(5, LuatCongGio.tinh(ket).phut)
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

    /** Boc may cau JSON vao mot cau tra loi day du nhu cua Gemini. */
    private fun traLoiAi(vararg cau: String): String =
        """{"mon":"Toán","ngay_dan_do":null,"bai_duoc_giao":[],"lam_het_dan_do":false,
           "cac_cau":[${cau.joinToString(",")}],"tom_tat":"thử"}"""
}
