package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.PhimKiemTra
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.BuoiDo
import vn.huytl.homeworkgate.kho.Chieu
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TraTu
import vn.huytl.homeworkgate.kho.TuVung
import kotlin.random.Random

/**
 * Luat cua duong tu vung.
 *
 * Phan dang test ky nhat la [KhoBai.tinhTrangTu], vi tinh trang quyet dinh trong so,
 * ma trong so quyet dinh tu nao duoc hoi lai. Sai o day thi may van chay, van hoi,
 * chi la hoi nham tu - va khong co cach nao nhin ra bang mat.
 *
 * Bo test nay KHONG xoa prefs, nhung co xoa bang tu vung cua bo "thu".
 */
@RunWith(AndroidJUnit4::class)
class TuVungTest {

    private lateinit var kho: KhoBai
    private val now = System.currentTimeMillis()

    private fun tu(ma: String, unit: Int = 1, loai: String = "n") = TuVung(
        id = "thu:$ma", bo = "thu", mon = "Tiếng Anh", unit = unit,
        tu = ma, loai = loai, am = "/$ma/", nghia = "nghĩa của $ma"
    )

    private fun ghi(ma: String, phien: String, dung: Boolean, lan: Int = 1, luc: Long = now) =
        kho.ghiTraTu(
            TraTu(
                tuId = "thu:$ma", phien = phien, buoi = BuoiDo.HANG_NGAY,
                chieu = Chieu.VIET_ANH, lan = lan, go = ma, dung = dung,
                goiY = 0, chiu = false, giay = 0, luc = luc
            )
        )

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        kho = KhoBai.get(context)
        // Nap bo rong de xoa sach tu cu, roi nap bo that. Khong xoa thi lan chay thu
        // hai doc phai lich su cua lan truoc, ma loi kieu do chi hien o lan hai.
        kho.napBoTu("thu", emptyList())
        kho.writableDatabase.delete("tra_tu", "tu_id LIKE ?", arrayOf("thu:%"))
    }

    // --- nap bo that tu assets ---

    /**
     * Bo tu vung that co nap duoc khong, va co dung so tu khong.
     *
     * Kiem cho nay vi duong nap la cho de hong im lang nhat: thieu file thi
     * [vn.huytl.homeworkgate.kho.BoTuVung.napNeuCan] chi ghi mot dong log roi thoi,
     * app van chay binh thuong, chi la khong co tu nao de hoi. Khong co test thi
     * khong ai biet cho den luc mo man do ra thay trong.
     */
    @Test
    fun nap_duoc_bo_tu_vung_that() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BoTuVung.napNeuCan(context)
        val cac = kho.cacTuCua("anh8")

        // SO DONG TRONG FILE PHAI BANG SO DONG TRONG BANG.
        //
        // Day moi la phep kiem that su, chu khong phai mot con so go cung. Ma cua
        // mot tu dat tu chinh tu do, nen hai muc khac nhau trong sach co the ra cung
        // mot ma, va luc do dong nap sau de len dong truoc - mat mot tu ma khong ai
        // bao gi. Da xay ra that: sach Unit 11 co "face-to-face" va "face to face",
        // file ghi ca hai, bang chi nhan mot. Phep so nay bat duoc moi lan va nhu
        // vay, khong phai chi lan do.
        val trongFile = org.json.JSONObject(
            context.assets.open("tuvung/anh8.json").bufferedReader().use { it.readText() }
        ).getJSONArray("cac_tu").length()
        assertEquals("file $trongFile tu ma bang chi nhan ${cac.size}", trongFile, cac.size)

        // Du muoi hai Unit, khong Unit nao rong - thieu mot trang la thay ngay.
        assertEquals((1..12).toSet(), cac.map { it.unit }.toSet())
    }

    @Test
    fun tu_nap_ra_du_bon_cot() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BoTuVung.napNeuCan(context)
        val t = kho.cacTuCua("anh8", unit = 8).first { it.tu == "bargain" }
        assertEquals("v", t.loai)
        assertEquals("/ˈbɑːɡən/", t.am)
        assertEquals("mặc cả", t.nghia)
        assertEquals("anh8:bargain", t.id)
    }

    // --- so giay va so phut ---

    @Test
    fun hai_muoi_tu_dung_la_hai_muoi_phut() {
        assertEquals(1200, LuatTuVung.SO_TU_HANG_NGAY * LuatTuVung.GIAY_MOI_TU)
        assertEquals(20, LuatTuVung.phutTu(1200))
    }

    @Test
    fun cau_ngu_phap_gap_doi_mot_tu() {
        assertEquals(2 * LuatTuVung.GIAY_MOI_TU, LuatTuVung.GIAY_MOI_CAU_NGU_PHAP)
    }

    @Test
    fun tran_ngay_bang_dung_mot_buoi() {
        assertEquals(LuatTuVung.GIAY_TOI_DA_HANG_NGAY, LuatTuVung.GIAY_TRAN_MOI_NGAY)
        // Hai muoi tu mot phut cong nam cau ngu phap hai phut.
        assertEquals(30, LuatTuVung.phutTrongNgay(LuatTuVung.GIAY_TRAN_MOI_NGAY))
        // Qua tran bao nhieu cung chi ra dung tran.
        assertEquals(30, LuatTuVung.phutTrongNgay(99_999))
    }

    @Test
    fun le_duoi_mot_phut_nam_lai_trong_ngay_chu_khong_mat() {
        // Tu 23/9/2026 moi tu tron mot phut, phan le chi con tu nhung luot ghi truoc
        // do (nua phut mot tu). Ham van phai giu le trong ngay: hai luot 90 giay,
        // chia rieng thi moi luot mot phut, tong hai; tinh don ca ngay la ba phut.
        assertEquals(1, LuatTuVung.phutThem(0, 90))
        assertEquals(2, LuatTuVung.phutThem(90, 90))
    }

    @Test
    fun het_tran_ngay_thi_do_them_khong_ra_phut_nao() {
        assertEquals(
            0,
            LuatTuVung.phutThem(LuatTuVung.GIAY_TRAN_MOI_NGAY, 20 * LuatTuVung.GIAY_MOI_TU)
        )
        // Con mot phut trong tran thi chi duoc mot phut, khong am.
        assertEquals(
            1,
            LuatTuVung.phutThem(
                LuatTuVung.GIAY_TRAN_MOI_NGAY - 60,
                20 * LuatTuVung.GIAY_MOI_TU
            )
        )
    }

    @Test
    fun le_nua_phut_thi_bo_di_chu_khong_lam_tron_len() {
        assertEquals(9, LuatTuVung.phutTu(570))
        assertEquals(0, LuatTuVung.phutTu(30))
        assertEquals(0, LuatTuVung.phutTu(-10))
    }

    // --- dang hoc unit nao ---

    /**
     * Con chon da hoc toi Unit nao thi chi hoi tu Unit 1 toi het Unit do.
     *
     * Truoc 25/9/2026 cho nay la may doan Unit theo lich, va test o day kiem nhip doan.
     * Ba Huy bo han cach doan, cho con tu chon - xem [vn.huytl.homeworkgate.kho.HocToi].
     */
    @Test
    fun chi_hoi_tu_unit_mot_toi_het_unit_da_chon() {
        assertTrue(LuatTuVung.daHoc(1, 3))
        assertTrue(LuatTuVung.daHoc(3, 3))
        assertFalse(LuatTuVung.daHoc(4, 3))
        assertFalse(LuatTuVung.daHoc(12, 3))
    }

    @Test
    fun chon_chua_hoc_unit_nao_thi_khong_tu_nao_lot_qua() {
        assertFalse(LuatTuVung.daHoc(1, HocToi.CHUA_HOC_UNIT_NAO))
        // Ke ca tu chep hong unit 0: con vua noi la chua hoc gi.
        assertFalse(LuatTuVung.daHoc(0, HocToi.CHUA_HOC_UNIT_NAO))
    }

    @Test
    fun tu_chep_hong_unit_0_van_duoc_hoi_khi_da_hoc() {
        // Mot cho hong trong file khong duoc lam mat han mot tu khoi duong hoc.
        assertTrue(LuatTuVung.daHoc(0, 1))
    }

    /**
     * Chua chon la null, khac voi chon "chua hoc Unit nao". Hai cai lan nhau thi hoac
     * may khong bao gio hoi con chon, hoac con da noi chua hoc ma may van bat chon lai.
     */
    @Test
    fun chua_chon_unit_khac_voi_chon_chua_hoc() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Ghi bang HocToi.ghiUnit chu khong bang datUnit, cung ly do ben HocThuocTest:
        // datUnit ghi them nhat ky, va nhat ky sang Bang dieu khien.
        HocToi.xoa(context, "thu")
        try {
            assertNull(HocToi.unitCua(context, "thu"))
            HocToi.ghiUnit(context, "thu", HocToi.CHUA_HOC_UNIT_NAO)
            assertEquals(HocToi.CHUA_HOC_UNIT_NAO, HocToi.unitCua(context, "thu"))
            HocToi.ghiUnit(context, "thu", 3)
            assertEquals(3, HocToi.unitCua(context, "thu"))
        } finally {
            HocToi.xoa(context, "thu")
        }
    }

    // --- cham ---

    @Test
    fun cham_bo_qua_chu_hoa_va_dau_cach_thua() {
        assertTrue(LuatTuVung.dung("A Book", "a book"))
        assertTrue(LuatTuVung.dung("  abook ", "a book"))
    }

    @Test
    fun sai_chinh_ta_van_la_sai() {
        assertFalse(LuatTuVung.dung("libary", "library"))
        assertFalse(LuatTuVung.dung("", "library"))
    }

    // --- phim ghep va goi y (2/10/2026) ---

    @Test
    fun tu_mot_chu_moi_o_du_26_phim_khong_hien_chu_dau() {
        val k = PhimKiemTra.ghepTu("bracelet")
        val g = k.ghep as Ghep.Chu
        assertTrue(g.duPhim)
        assertEquals("", g.truoc)
        assertEquals(8, g.soO)
        // 7 chu khac nhau (b r a c e l t), 19 phim con lai la nhieu.
        assertEquals(19, g.nhieu.size)
        assertTrue(g.nhieu.none { it in "bracelt" })
        assertEquals("bracelet".map { it.toString() }, k.phan)
    }

    @Test
    fun goi_y_mo_phim_roi_dien_san_nua_dau_roi_hien_het() {
        val k = PhimKiemTra.ghepTu("bracelet")
        val dung = { go: String -> LuatTuVung.dung(go, "bracelet") }
        assertEquals(listOf(0, 1, 2, 2, 2), (0..4).map { PhimKiemTra.mucMo(it) })
        assertTrue(k.dienSan(2).isEmpty())
        assertEquals("brac", k.dienSan(3).joinToString(""))
        assertEquals("brac", k.dienSan(4).joinToString(""))
        // Mo phim va dien san nua dau van chua lo tu: lam dung van duoc phut.
        (0..3).forEach { assertFalse("sai $it", k.loHet(it, dung)) }
        // Sai lan thu tu thi hien ca tu, lam dung cung khong duoc phut.
        assertTrue(k.loHet(PhimKiemTra.BAC_HIEN_HET, dung))
    }

    @Test
    fun dau_cach_gach_noi_va_phan_trong_ngoac_in_san() {
        val log = PhimKiemTra.ghepTu("log (on to)")
        assertEquals(listOf(null, null, null, " ", "(on to)"), (log.ghep as Ghep.Chu).khuon)
        assertEquals(3, (log.ghep as Ghep.Chu).soO)
        assertTrue(LuatTuVung.dung((log.ghep as Ghep.Chu).dien("log"), "log (on to)"))

        val diy = PhimKiemTra.ghepTu("DIY (do-it-yourself)").ghep as Ghep.Chu
        assertEquals(3, diy.soO)
        // Phim chu thuong, sach in chu hoa: van cham dung.
        assertTrue(LuatTuVung.dung(diy.dien("diy"), "DIY (do-it-yourself)"))

        val ghepNoi = PhimKiemTra.ghepTu("well-trained").ghep as Ghep.Chu
        assertEquals(11, ghepNoi.soO)
        assertEquals("well-trained", ghepNoi.dien("welltrained"))

        val nhay = PhimKiemTra.ghepTu("farmers' market").ghep as Ghep.Chu
        assertEquals("farmers' market", nhay.dien("farmersmarket"))
        assertTrue(LuatTuVung.dung(nhay.dien("farmersmarket"), "farmers' market"))

        val cum = PhimKiemTra.ghepTu("keep in touch").ghep as Ghep.Chu
        assertEquals(11, cum.soO)
        assertTrue(LuatTuVung.dung(cum.dien("keepintouch"), "keep in touch"))
        // Thieu chu thi khong lot: o trong de trong.
        assertFalse(LuatTuVung.dung(cum.dien("keepintouc"), "keep in touch"))
    }

    /** Moi tu that trong bo: dien du chu vao o thi ra dung tu, va khong co chu nao ngoai 26 phim. */
    @Test
    fun moi_tu_that_go_duoc_bang_26_phim() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BoTuVung.napNeuCan(context)
        val kho = KhoBai.get(context)
        var dem = 0
        BoTuVung.BO.forEach { bo ->
            kho.cacTuCua(bo.bo).forEach { tu ->
                val k = PhimKiemTra.ghepTu(tu.tu)
                val g = k.ghep as Ghep.Chu
                val go = k.phan.joinToString("").lowercase()
                assertTrue("${tu.tu}: '$go' co chu ngoai 26 phim", go.all { it in 'a'..'z' })
                assertEquals(tu.tu, g.soO, k.phan.size)
                assertTrue("${tu.tu}: dien du o ma cham sai", LuatTuVung.dung(g.dien(go), tu.tu))
                dem++
            }
        }
        assertTrue(dem > 200)
    }

    // --- chieu hoi ---

    @Test
    fun tu_chua_dung_lan_nao_thi_cho_nhan_mat_truoc() {
        assertEquals(Chieu.ANH_VIET, LuatTuVung.chieuCho(0))
        assertEquals(Chieu.VIET_ANH, LuatTuVung.chieuCho(1))
    }

    // --- boc theo trong so ---

    @Test
    fun boc_khong_lay_trung_va_khong_doi_bao_nhieu_cung_ra() {
        val cac = (1..30).map { "t$it" to 1.0 }
        val ra = LuatTuVung.bocTheoTrongSo(cac, 20, Random(1))
        assertEquals(20, ra.size)
        assertEquals(20, ra.toSet().size)
    }

    @Test
    fun xin_nhieu_hon_so_co_thi_tra_ve_het_chu_khong_treo() {
        val ra = LuatTuVung.bocTheoTrongSo(listOf("a" to 1.0, "b" to 1.0), 20, Random(2))
        assertEquals(2, ra.size)
    }

    @Test
    fun trong_so_khong_thi_khong_bao_gio_duoc_boc() {
        val cac = listOf("co" to 1.0, "khong" to 0.0)
        repeat(20) { assertFalse("khong" in LuatTuVung.bocTheoTrongSo(cac, 1, Random(it))) }
    }

    @Test
    fun tu_vua_sai_duoc_boc_nhieu_hon_han_tu_da_thuoc() {
        // Mot tu vua sai va mot tram tu da thuoc. Trong so 8 so voi 0,25 nghia la tu
        // vua sai chiem khoang mot phan tu tong, nen boc mot nghin lan phai thay no
        // vai tram lan - khac han ty le mot tren mot tram neu trong so bang nhau.
        val cac = listOf("sai" to LuatTuVung.TRONG_SO_VUA_SAI) +
            (1..100).map { "thuoc$it" to LuatTuVung.TRONG_SO_DA_THUOC }
        val rnd = Random(7)
        val demSai = (1..1000).count { "sai" in LuatTuVung.bocTheoTrongSo(cac, 1, rnd) }
        assertTrue("boc duoc $demSai/1000 lan", demSai in 150..400)
    }

    // --- moi nhu trac nghiem ---

    @Test
    fun moi_nhu_uu_tien_cung_unit_cung_loai() {
        val ra = LuatTuVung.chonMoiNhu(
            dung = "dung",
            cungUnitCungLoai = listOf("a", "b", "c", "d"),
            cungLoai = listOf("x"),
            cungUnit = listOf("y"),
            rnd = Random(3)
        )
        assertEquals(LuatTuVung.SO_LUA_CHON, ra.size)
        assertTrue("dung" in ra)
        assertFalse("x" in ra)
        assertFalse("y" in ra)
    }

    @Test
    fun moi_nhu_khong_duoc_gan_nghia_voi_dap_an() {
        // Ba tu that cua Unit 1: keen, fond, crazy. De chung mot cau thi con khong
        // tra loi duoc bang kien thuc, chi doan.
        assertFalse(LuatTuVung.moiNhuDuoc("say mê, ham thích", "mến, thích"))
        assertFalse(LuatTuVung.moiNhuDuoc("say mê, ham thích", "rất thích, quá say mê"))
        // Nghia khac han thi van lam moi nhu duoc.
        assertTrue(LuatTuVung.moiNhuDuoc("say mê, ham thích", "độc ác"))
        assertTrue(LuatTuVung.moiNhuDuoc("say mê, ham thích", "căm ghét"))
    }

    @Test
    fun thieu_moi_nhu_thi_ra_it_lua_chon_chu_khong_hong() {
        val ra = LuatTuVung.chonMoiNhu("dung", listOf("a"), emptyList(), emptyList(), Random(4))
        assertEquals(2, ra.size)
        assertTrue("dung" in ra)
    }

    // --- tinh trang tu, doc tu SQLite ---

    private fun tinhTrang(ma: String): LuatTuVung.TinhTrang =
        kho.tinhTrangTu("thu", LuatTuVung.LAN_DUNG_DE_TINH)
            .first { it.first.id == "thu:$ma" }.second

    @Test
    fun tu_chua_gap_bao_gio_la_chua_gap() {
        kho.napBoTu("thu", listOf(tu("moi")))
        assertEquals(LuatTuVung.TinhTrang.CHUA_GAP, tinhTrang("moi"))
    }

    @Test
    fun sai_o_lan_dau_cua_phien_gan_nhat_la_vua_sai() {
        kho.napBoTu("thu", listOf(tu("sai")))
        // Mot phien cu da xong, roi phien moi mo dau bang mot lan sai.
        ghi("sai", "p1", dung = true, luc = now - 2000)
        ghi("sai", "p1", dung = true, lan = 2, luc = now - 1900)
        ghi("sai", "p2", dung = false, luc = now - 100)
        ghi("sai", "p2", dung = true, lan = 2, luc = now - 50)
        assertEquals(LuatTuVung.TinhTrang.VUA_SAI, tinhTrang("sai"))
    }

    @Test
    fun dung_ngay_lan_dau_thi_dem_theo_so_phien_da_xong() {
        kho.napBoTu("thu", listOf(tu("a"), tu("b"), tu("c")))
        // a: xong mot phien.
        ghi("a", "p1", dung = true, luc = now - 300)
        ghi("a", "p1", dung = true, lan = 2, luc = now - 290)
        // b: xong hai phien.
        listOf("p1", "p2").forEachIndexed { i, p ->
            ghi("b", p, dung = true, luc = now - 300 + i * 100)
            ghi("b", p, dung = true, lan = 2, luc = now - 290 + i * 100)
        }
        // c: xong ba phien.
        listOf("p1", "p2", "p3").forEachIndexed { i, p ->
            ghi("c", p, dung = true, luc = now - 300 + i * 100)
            ghi("c", p, dung = true, lan = 2, luc = now - 290 + i * 100)
        }
        assertEquals(LuatTuVung.TinhTrang.DUNG_1, tinhTrang("a"))
        assertEquals(LuatTuVung.TinhTrang.DUNG_2, tinhTrang("b"))
        assertEquals(LuatTuVung.TinhTrang.DA_THUOC, tinhTrang("c"))
    }

    @Test
    fun dung_mot_lan_trong_phien_la_xong_phien() {
        kho.napBoTu("thu", listOf(tu("le")))
        // Tu 27/9/2026 dung mot lan la xong tu do trong buoi, khong hoi lai lan hai.
        ghi("le", "p1", dung = true, luc = now - 100)
        assertEquals(LuatTuVung.TinhTrang.DUNG_1, tinhTrang("le"))
    }

    @Test
    fun so_phien_da_xong_di_kem_tinh_trang() {
        kho.napBoTu("thu", listOf(tu("m"), tu("n")))
        // m: xong hai phien roi phien thu ba mo dau bang mot lan sai. Tinh trang che
        // mat con so, nhung con so van phai dung - chieu hoi doc tu no.
        listOf("p1", "p2").forEachIndexed { i, p ->
            ghi("m", p, dung = true, luc = now - 500 + i * 100)
            ghi("m", p, dung = true, lan = 2, luc = now - 490 + i * 100)
        }
        ghi("m", "p3", dung = false, luc = now - 10)

        val cac = kho.tinhTrangTu("thu", LuatTuVung.LAN_DUNG_DE_TINH)
        val m = cac.first { it.first.id == "thu:m" }
        assertEquals(LuatTuVung.TinhTrang.VUA_SAI, m.second)
        assertEquals(2, m.third)
        // Chua gap bao gio thi khong phien nao, va do la tu duoc cho nhan mat truoc.
        val n = cac.first { it.first.id == "thu:n" }
        assertEquals(0, n.third)
        assertEquals(Chieu.ANH_VIET, LuatTuVung.chieuCho(n.third))
        assertEquals(Chieu.VIET_ANH, LuatTuVung.chieuCho(m.third))
    }

    @Test
    fun ty_le_dung_lan_dau_chi_dem_lan_thu_dau_cua_moi_phien() {
        kho.napBoTu("thu", listOf(tu("x"), tu("y")))
        val moc = now - 10_000
        // x: phien p1 mo dau sai, sau do dung hai lan.
        ghi("x", "p1", dung = false, luc = moc + 1)
        ghi("x", "p1", dung = true, lan = 2, luc = moc + 2)
        ghi("x", "p1", dung = true, lan = 3, luc = moc + 3)
        // y: phien p1 dung ngay tu dau.
        ghi("y", "p1", dung = true, luc = moc + 4)
        ghi("y", "p1", dung = true, lan = 2, luc = moc + 5)

        val (dung, tong) = kho.tyLeDungLanDau(moc)
        assertEquals(2, tong)
        assertEquals(1, dung)
    }
}
