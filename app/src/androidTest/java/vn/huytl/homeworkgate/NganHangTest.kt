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
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.ChamTheoClaude
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.KhaiChoCham
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.CauNgoai
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

    /**
     * Mot cau ngoai sach: phieu Tieng Anh nop qua "Bai khac". Khong co ma sach nen
     * khoa lam tu de bai, dang "tu:..." - xem [SoCaiBai.khoaCua].
     */
    private val cauNgoaiSach = CauCham(
        ma = "câu 3", de = "She ___ (go) to school every day.", mon = "Tiếng Anh", soDong = 1
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
     * khong goi SoCaiBai.xoaHet, vi ham do quet sach so cai cua ca app. Cau ngoai
     * sach gia [cauNgoaiSach] cung don theo dung khoa cua no. Don hep nhu vay khong
     * giu duoc so cai that: setUp da xoa sach no truoc moi test, xem canh bao o dau lop.
     */
    @After
    fun tearDown() {
        val kho = KhoBai.get(context)
        runCatching { kho.napNguon("thu", emptyList()) }
        runCatching { kho.writableDatabase.delete("tra_loi", "cau_id LIKE ?", arrayOf("thu:%")) }
        runCatching {
            kho.writableDatabase.delete(
                "tra_loi", "cau_id = ?", arrayOf(SoCaiBai.khoaCua(cauNgoaiSach))
            )
        }
    }

    // ------------------------------------------------------------------ cai chinh

    @Test
    fun cung_mot_cau_ma_chep_ma_khac_di_van_bi_nhan_ra_la_da_lam() {
        // Hom qua: Claude chep dung ma sach.
        val homQua = chamClaude(cau("2.26a"))
        SoCaiBai.ghi(context, homQua.cac, mapOf("2.26a" to 2), now - ngay)
        assertEquals(2, SoCaiBai.phutDaCongHomNay(context, now - ngay))

        // Hom nay con chup lai chinh trang do, lan nay ma viet kieu khac. Khoa theo de
        // bai la mot khoa khac han, va con duoc cong gio lan hai; khoa theo ma sach thi
        // van la mot cau.
        val homNay = chamClaude(cau("2.26 a"))
        assertTrue(SoCaiBai.daTraGioCua(context, homNay.cac.first(), now))
        assertEquals(0, SoCaiBai.phutDaCongHomNay(context, now))
    }

    @Test
    fun hai_cau_khac_nhau_co_de_giong_het_nhau_van_la_hai_cau() {
        // 2.27a va 2.27b cung mang de "Rút gọn biểu thức" - chuan hoa ra y het nhau.
        // Duong cu se coi cau thu hai la da lam roi va khong tra gio cho no.
        val ket = chamClaude(cau("2.27a"), cau("2.27b"))

        SoCaiBai.ghi(context, listOf(ket.cac[0]), mapOf("2.27a" to 2), now)
        assertTrue(SoCaiBai.daTraGioCua(context, ket.cac[0], now))
        assertFalse(SoCaiBai.daTraGioCua(context, ket.cac[1], now))

        SoCaiBai.ghi(context, listOf(ket.cac[1]), mapOf("2.27b" to 2), now)
        assertEquals(4, SoCaiBai.phutDaCongHomNay(context, now))
    }

    // ----------------------------------------------------------------- doc ket qua

    @Test
    fun de_bai_lay_tu_sach_chu_khong_phai_chu_Claude_chep() {
        val ket = chamClaude(cau("2.26a", de = "chép sai"))
        assertEquals("Phân tích đa thức x^2 - 6x + 9 - y^2 thành nhân tử", ket.cac.first().de)
        assertEquals("thu:2.26a", ket.cac.first().cauId)
    }

    @Test
    fun cau_ngoai_danh_sach_van_duoc_cham_nhung_di_duong_de_bai() {
        val ket = chamClaude(cau("2.99", de = "Bài con tự làm thêm", soDong = 6))
        val c = ket.cac.first()
        assertEquals(null, c.cauId)
        assertEquals("Bài con tự làm thêm", c.de)

        SoCaiBai.ghi(context, listOf(c), mapOf("2.99" to 3), now)
        assertTrue(SoCaiBai.daTraGio(context, "Bài con tự làm thêm", now))
    }

    @Test
    fun dang_bai_lay_theo_sach_chu_khong_theo_Claude() {
        // Sach ghi 2.26a la CAU_NHO. Claude bao TRAC_NGHIEM thi khong nghe: dang quyet
        // dinh so phut, ma cung mot cau hai lan cham co the ra hai dang khac nhau.
        val ket = chamClaude(cau("2.26a", dang = "TRAC_NGHIEM"))
        assertEquals(vn.huytl.homeworkgate.data.DangBai.CAU_NHO, ket.cac.first().dang)
    }

    @Test
    fun bai_lam_di_xuong_so_va_doc_lai_duoc() {
        // So cai van giu tung dong bai lam va dong sai, du tu 28/9/2026 khong con may
        // cham nao chep dong ra: ban cham cu trong so van doc lai duoc.
        val c = CauCham(
            ma = "2.26d", de = bai[4].de, cauId = "thu:2.26d", dung = false, dongSai = 1,
            baiLam = listOf("= x(x-6)", "= x^2-6x"), nhanXet = "Dòng 1 sai"
        )
        SoCaiBai.ghi(context, listOf(c), emptyMap(), now)

        val dong = SoCaiBai.lichSuCua(context, "thu:2.26d").first()
        assertEquals(listOf("= x(x-6)", "= x^2-6x"), dong.baiLam)
        assertEquals(1, dong.dongSai)
    }

    // --------------------------------------------------- anh chi co dap an

    @Test
    fun cau_trong_danh_sach_con_khai_thi_luon_co_de() {
        // De lay tu sach nen Claude khong phai chep de cho cau con da khai.
        val ket = chamClaude(cau("2.26a", soDong = 3))
        assertTrue(ket.cac.first().coDe)
    }

    @Test
    fun cau_ngoai_danh_sach_khong_co_de_thi_khong_tra_gio() {
        // Canh ngay 16/9/2026: vo KHTN chi ghi "cau 1 B", khong mot dong de nao.
        val ket = chamClaude(cau("câu 1", de = "", dang = "TRAC_NGHIEM", conViet = "B"))
        val c = ket.cac.first()
        assertFalse("khong co de ma van coi la co", c.coDe)
        assertEquals(0, LuatCongGio.phutChoCau(c))
    }

    @Test
    fun ca_trang_chi_co_dap_an_thi_khong_duoc_phut_nao() {
        // Hai muoi cau trac nghiem, Claude bao dung het - nhung khong co de. Khong chan
        // o co de thi trang nay van ra phut du khong ai cham duoc.
        val cac = (1..20).map { i -> cau("câu $i", de = "", dang = "TRAC_NGHIEM", conViet = "B") }
        val ket = chamClaude(*cac.toTypedArray())
        assertEquals(20, ket.cac.size)
        assertEquals(0, LuatCongGio.tinh(ket).phut)
    }

    @Test
    fun cau_ngoai_danh_sach_co_de_thi_tra_gio_binh_thuong() {
        // Doi chung voi test tren: cung 20 cau trac nghiem, lan nay co de.
        val cac = (1..20).map { i -> cau("câu $i", de = "Câu $i hỏi gì đó", dang = "TRAC_NGHIEM") }
        val ket = chamClaude(*cac.toTypedArray())
        // Mot phut mot cau dung, khong con tran trac nghiem moi lan nop (29/9/2026): hai
        // muoi cau van duoi tran 45 cua duong chup anh.
        assertEquals(20 * LuatCongGio.PHUT_MOI_CAU_TRAC_NGHIEM, LuatCongGio.tinh(ket).phut)
    }

    // ------------------------------------------------------------ nop lai cau sai

    /**
     * Lan nop lai cac cau sai cua mot bai: cau ngoai danh sach khai bi bo.
     *
     * Canh Ba Huy tim ra ngay 28/9/2026: con sua de len chinh trang vo cu, anh co ca cau
     * da dung tu lan truoc, va Claude duoc dan chep them moi cau thay trong anh. Cau do
     * khong nam trong danh sach nen khoa so cai theo de bai, so cai khong nhan ra la da
     * tra gio, va con duoc tra gio lan hai.
     */
    @Test
    fun lan_nop_lai_bo_cau_ngoai_danh_sach() {
        val sua = PhamVi(
            mon = "Toán", nguon = "thu", tenNguon = "Sách thử", bai = "sửa bài lúc 11:18",
            cauIds = listOf("thu:2.26b"), suaBai = "bai-cu"
        )
        val ket = chamClaude(
            cau("2.26a", de = "Phân tích đa thức x^2 - 6x + 9 - y^2 thành nhân tử"),
            cau("2.26b"),
            pham = sua
        )
        assertEquals(listOf("2.26b"), ket.cac.map { it.ma })
        assertEquals("thu:2.26b", ket.cac.first().cauId)
    }

    /**
     * Cau ngoai sach cua lan nop lai mang de chep tu ban cham cu, khong lay de Claude chep
     * lan nay: so cai khoa cau ngoai sach theo de, lay de moi thi lan sai cu va lan sua
     * dung nay thanh hai cau khac nhau.
     */
    @Test
    fun lan_nop_lai_cau_ngoai_sach_lay_de_cua_ban_cham_cu() {
        SoCaiBai.ghi(context, listOf(cauNgoaiSach.copy(dung = false)), emptyMap(), now - ngay)
        val sua = PhamVi(
            mon = "Tiếng Anh", bai = "sửa bài lúc 11:18",
            cauNgoai = listOf(CauNgoai("câu 3", cauNgoaiSach.de)), suaBai = "bai-cu"
        )
        val ket = chamClaude(
            cau("Câu 3", de = "She ____ (go) to school every day"),
            cau("câu 4", de = "Câu đã đúng từ lần trước"),
            pham = sua
        )
        val c = ket.cac.single()
        assertEquals(cauNgoaiSach.de, c.de)
        assertEquals(SoCaiBai.khoaCua(cauNgoaiSach), SoCaiBai.khoaCua(c))
        assertEquals(1, SoCaiBai.soLanSai(context, c, now))
        assertFalse(SoCaiBai.daTraGioCua(context, c, now))
    }

    /**
     * Cau dang nam trong mot bai cho chua cham thi bi khoa o man chon cau va man ket qua,
     * xem [KhaiChoCham.cauChoCham]. Cham xong (co dong ghi sau luc nop) thi het khoa.
     */
    @Test
    fun cau_trong_bai_cho_chua_cham_thi_dang_cho_cham() {
        val gate = GateStore(context)
        val luc = System.currentTimeMillis()
        gate.markPending("bai-cho", 0L, luc)
        KhaiChoCham.luu(
            context, "bai-cho",
            PhamVi(mon = "Toán", nguon = "thu", cauIds = listOf("thu:2.26a", "thu:2.26b"))
        )
        try {
            assertEquals(setOf("thu:2.26a", "thu:2.26b"), KhaiChoCham.cauChoCham(context))
            // Claude cham: 2.26b sai, co dong sai ghi sau luc nop - het "cho cham".
            SoCaiBai.ghi(
                context, listOf(CauCham(ma = "2.26b", de = bai[1].de, cauId = "thu:2.26b", dung = false)),
                emptyMap(), luc + 1_000
            )
            assertEquals(setOf("thu:2.26a"), KhaiChoCham.cauChoCham(context))
        } finally {
            gate.boBaiCho("bai-cho")
        }
    }

    // ---------------------------------------------------------------- on tap

    /** Ghi mot cau: sai truoc, roi sua dung - thanh cau co the den hen on. */
    private fun lamSaiRoiSuaDung(ma: String, luiNgay: Int = 1) =
        lamSaiRoiSuaDung(CauCham(ma = ma, de = "", cauId = "thu:$ma", soDong = 4), luiNgay)

    private fun lamSaiRoiSuaDung(cau: CauCham, luiNgay: Int = 1) {
        val luc = now - luiNgay * ngay
        SoCaiBai.ghi(context, listOf(cau.copy(dung = false)), emptyMap(), luc)
        SoCaiBai.ghi(context, listOf(cau.copy(dung = true)), mapOf(cau.ma to 2), luc)
        // Kiem ngay o day: buoc dung du lieu ma khong ghi duoc gi thi cac test "khong
        // den hen" phia sau van qua, ma khong chung minh duoc dieu gi.
        assertEquals(1, SoCaiBai.soLanSai(context, cau, now))
        assertTrue(SoCaiBai.daTraGioCua(context, cau, now))
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
    fun on_tap_tinh_gio_moi_lan_den_hen_chu_khong_phai_moi_lan_on() {
        lamSaiRoiSuaDung("2.26d", luiNgay = 4)
        val cau = CauCham(ma = "2.26d", de = "", cauId = "thu:2.26d", dung = true, soDong = 4)

        // Lan on dau: da den hen (ba ngay), duoc tinh gio.
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

    @Test
    fun cau_ngoai_sach_sua_dung_roi_cung_khong_den_hen_on() {
        // Truoc ban nay cau nay den hen sau ba ngay: man chinh bao "Ôn lại 1 câu đến
        // hẹn", ma man on chi ve duoc cau trong sach nen bam vao khong thay cau nao.
        lamSaiRoiSuaDung(cauNgoaiSach, luiNgay = 4)
        assertTrue(SoCaiBai.cacCauDangOn(context, now).isEmpty())
        assertFalse(SoCaiBai.denHenOn(context, cauNgoaiSach, now))
    }

    @Test
    fun cau_ngoai_sach_lot_vao_bai_on_thi_khong_duoc_tinh_gio_on() {
        // Lan nop bai on, may tach them mot cau ngoai danh sach va chep de trung khoa
        // cu. Cau do chua bao gio nam tren man on, nen khong duoc ghi la mot lan on
        // va khong an nua so phut nao.
        lamSaiRoiSuaDung(cauNgoaiSach, luiNgay = 4)
        val daGhi = SoCaiBai.ghi(
            context, listOf(cauNgoaiSach.copy(dung = true)), mapOf(cauNgoaiSach.ma to 1), now,
            onTap = true
        )
        assertTrue(daGhi.isEmpty())
        assertFalse(SoCaiBai.daOnTap(context, SoCaiBai.khoaCua(cauNgoaiSach), now))
        // Tu 29/9/2026 phutOnHomNay chi dem luot on tren may, nen o day hoi tong phut hom
        // nay: hai dong cua lamSaiRoiSuaDung ghi tu bon ngay truoc.
        assertEquals(0, SoCaiBai.phutDaCongHomNay(context, now))
    }

    @Test
    fun cau_khong_hien_tren_man_on_thi_lan_on_khong_duoc_tinh() {
        // Hai dieu kien ban nay them cho duong cham tung cau, ngoai khoa "tu:": cau
        // phai tung sai, va phai con trong ngan hang. Thieu mot trong hai thi cau do
        // khong nam tren man on, nen lan nop on khong ghi so va khong co phut.
        val dungNgay = CauCham(ma = "2.26a", de = "", cauId = "thu:2.26a", dung = true, soDong = 4)
        SoCaiBai.ghi(context, listOf(dungNgay), mapOf("2.26a" to 2), now - 4 * ngay)
        assertTrue(SoCaiBai.daTraGioCua(context, dungNgay, now))
        // 2.99z khong co trong [bai]: tung co trong sach roi bi bo khoi file.
        val boKhoiSach = CauCham(ma = "2.99z", de = "", cauId = "thu:2.99z", soDong = 4)
        lamSaiRoiSuaDung(boKhoiSach, luiNgay = 4)

        listOf(dungNgay, boKhoiSach).forEach { cau ->
            assertFalse(cau.ma, SoCaiBai.denHenOn(context, cau, now))
            val daGhi = SoCaiBai.ghi(
                context, listOf(cau.copy(dung = true)), mapOf(cau.ma to 1), now, onTap = true
            )
            assertTrue(cau.ma, daGhi.isEmpty())
        }
        // Xem cau_ngoai_sach_lot_vao_bai_on_thi_khong_duoc_tinh_gio_on: phutOnHomNay chi
        // con dem luot on tren may.
        assertEquals(0, SoCaiBai.phutDaCongHomNay(context, now))
    }

    @Test
    fun man_on_ve_du_so_cau_ma_dong_on_lai_dem() {
        // Ba cau cung den hen, nhung chi mot cau con trong sach. Hai cau kia: cau ngoai
        // sach, va cau tung co trong sach roi bi bo khoi file (2.99z khong co trong
        // [bai]). Dong "Ôn lại N câu" dem bao nhieu thi man on phai ve ra bay nhieu.
        lamSaiRoiSuaDung("2.26d", luiNgay = 4)
        lamSaiRoiSuaDung("2.99z", luiNgay = 4)
        lamSaiRoiSuaDung(cauNgoaiSach, luiNgay = 4)

        val dem = SoCaiBai.cacCauDangOn(context, now)
        val ve = KhoBai.get(context).cacCauTheoId(dem)
        assertEquals(listOf("thu:2.26d"), dem)
        assertEquals(dem.size, ve.size)
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
            cauIds = listOf("thu:2.26a", "thu:2.26b"),
            suaBai = "bai-cu",
            cauNgoai = listOf(CauNgoai("câu 3", "She ___ (go) to school every day.")),
            huyBaiCu = true
        )
        val ve = PhamVi.tuJson(goc.sangJson())
        assertEquals(goc, ve)
        assertTrue(ve!!.laSua)
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
    /** Pham vi con khai ca bai gia: moi cau trong [bai] la cau trong sach. */
    private val phamThu = PhamVi(
        mon = "Toán", nguon = "thu", tenNguon = "Sách thử", bai = "Bài 6 (trang 36)",
        cauIds = bai.map { it.id }
    )

    /** Mot muc trong ket qua Claude, dung kieu lenh CHAMBAI tu Bang dieu khien gui ve. */
    private fun cau(
        ma: String,
        dung: Boolean = true,
        chac: Boolean = true,
        soDong: Int = 4,
        de: String = "",
        dang: String = "",
        conViet: String = ""
    ): Map<String, Any> = mapOf(
        "ma" to ma, "dung" to dung, "chac" to chac, "soDong" to soDong,
        "de" to de, "dang" to dang, "conViet" to conViet, "goiY" to ""
    )

    /** Doi ket qua Claude thanh ban cham cua tablet, y nhu lenh CHAMBAI. */
    private fun chamClaude(vararg cac: Map<String, Any>, pham: PhamVi = phamThu): KetQuaCham =
        ChamTheoClaude.banCham(context, mapOf("cac" to cac.toList()), pham)!!
}
