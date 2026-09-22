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
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.BuoiDo
import vn.huytl.homeworkgate.kho.Chieu
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
    fun hai_muoi_tu_dung_la_muoi_phut() {
        assertEquals(600, LuatTuVung.SO_TU_HANG_NGAY * LuatTuVung.GIAY_MOI_TU)
        assertEquals(10, LuatTuVung.phutTu(600))
    }

    @Test
    fun cau_ngu_phap_gap_doi_mot_tu() {
        assertEquals(2 * LuatTuVung.GIAY_MOI_TU, LuatTuVung.GIAY_MOI_CAU_NGU_PHAP)
    }

    @Test
    fun le_nua_phut_thi_bo_di_chu_khong_lam_tron_len() {
        assertEquals(9, LuatTuVung.phutTu(570))
        assertEquals(0, LuatTuVung.phutTu(30))
        assertEquals(0, LuatTuVung.phutTu(-10))
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

    // --- goi y ---

    @Test
    fun goi_y_ho_dan_chu_khong_hien_thang_dap_an() {
        assertEquals("", LuatTuVung.goiY("library", 0))
        assertEquals("7 chữ cái", LuatTuVung.goiY("library", 1))
        assertEquals("l…", LuatTuVung.goiY("library", 2))
        assertEquals("libr…", LuatTuVung.goiY("library", 3))
        // Bac cao nhat van khong duoc ra ca tu.
        assertFalse(LuatTuVung.goiY("library", LuatTuVung.BAC_GOI_Y_TOI_DA) == "library")
    }

    @Test
    fun dem_chu_cai_thi_khong_dem_dau_cach() {
        assertEquals("5 chữ cái", LuatTuVung.goiY("a book", 1))
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

    // --- chen lai trong mot buoi ---

    @Test
    fun tu_vua_tra_loi_khong_duoc_hoi_lai_ngay() {
        assertEquals(15, LuatTuVung.chenLai(5, 20))
        // Cuoi buoi, khong con du muoi tu de chen thi day xuong cuoi.
        assertEquals(8, LuatTuVung.chenLai(5, 3))
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
    fun thieu_moi_nhu_thi_ra_it_lua_chon_chu_khong_hong() {
        val ra = LuatTuVung.chonMoiNhu("dung", listOf("a"), emptyList(), emptyList(), Random(4))
        assertEquals(2, ra.size)
        assertTrue("dung" in ra)
    }

    // --- buoi nao tra giay ---

    @Test
    fun buoi_toi_cua_vo_dan_do_nam_trong_tron_goi_nen_khong_tra_rieng() {
        assertFalse(LuatTuVung.coTraGiay(BuoiDo.DAN_DO_TOI))
        assertTrue(LuatTuVung.coTraGiay(BuoiDo.DAN_DO_SANG))
        assertTrue(LuatTuVung.coTraGiay(BuoiDo.HANG_NGAY))
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
    fun mot_lan_dung_le_chua_du_de_tinh_la_xong_phien() {
        kho.napBoTu("thu", listOf(tu("le")))
        // Dung mot lan roi bo ngang: chua du hai lan nen phien do khong tinh la xong.
        ghi("le", "p1", dung = true, luc = now - 100)
        assertEquals(LuatTuVung.TinhTrang.CHUA_GAP, tinhTrang("le"))
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
