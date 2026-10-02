package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LichKiemTra
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.DeThi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * De thi Toan, KHTN (Ba Huy chot 1/10/2026): pham vi tinh tu bai_sgk cua cau, moi mon mot de
 * dang mo, dong ho 90 va 60 phut khi de khong in gio. Tu 2/10/2026 de tu mo khi MOI bai de hoi
 * toi deu da danh dau la da hoc, ke ca khi con danh dau cach quang. Xem [DeThi], [GiaiDe.taoDeThi],
 * [GiaiDe.thieuPhamVi].
 *
 * Luc viet test nay repo chua co file de Toan, KHTN that (buoc 4.4 moi soan), nen phan doc file
 * dung mot chuoi JSON gia ([DeThi.docTuChuoi]), con phan mo de dung de gia lap ([DeThi.thayChoTest])
 * lay cau sach bai tap that trong kho de dung duoc [DeGiai].
 *
 * CAN THAN: bo test nay ghi bai da hoc cua moi mon, de va dong nhat ky vao may that, roi tra
 * lai bai da hoc cu, nhat ky hom nay va xoa moi de no tao. Nop de gia bang cach ghi thang luc nop vao de, khong di
 * qua [GiaiDe.nop], nen khong ghi so cai, khong cap phut nao. Chay tren may ao, khong chay tren
 * tablet that.
 */
@RunWith(AndroidJUnit4::class)
class DeThiPhanTest {

    private lateinit var context: Context
    private lateinit var kho: KhoBai
    private var mocCu: Map<String, Set<Int>?> = emptyMap()
    private val deDaTao = mutableListOf<String>()
    private var nhatKyCu: Pair<Int, String?> = 0 to null

    /**
     * 19 gio, 40 ngay sau hom nay. De thi mo ra giu 30 ngay, nen luc do moi de dang mo san tren
     * may (nguoi thu tay de lai) da het han, va luat "moi mon mot de" khong chan de cua test.
     * Lan dau test nay dung ngay co dinh 20/10/2026 thi hong khi tren may ao dang mo mot de Toan
     * mau (thu ngay 1/10/2026). Gio co dinh 19 gio de cac buoc "nop xong 40 phut sau" van cung ngay.
     */
    private val toi = ms(LocalDate.now().plusDays(40).atTime(19, 0))
    private val motNgay = 24 * 60 * 60_000L

    private fun ms(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        kho = KhoBai.get(context)
        mocCu = MocThu.luu(context)
        MocThu.xoaHet(context)
        val sp = Prefs.get(context).raw()
        nhatKyCu = sp.getInt(DayLog.K_DAY, 0) to sp.getString(DayLog.K_TEXT, null)
    }

    @After
    fun tearDown() {
        DeThi.thayChoTest = null
        deDaTao.forEach { kho.xoaDe(it) }
        MocThu.tra(context, mocCu)
        // Mo de bang tay, xem hinh deu ghi nhat ky: tra lai nhat ky hom nay nhu truoc test.
        Prefs.get(context).raw().edit()
            .putInt(DayLog.K_DAY, nhatKyCu.first)
            .putString(DayLog.K_TEXT, nhatKyCu.second)
            .commit()
    }

    // ------------------------------------------------------------ cong cu

    /** Phan [ma] hoc toi bai so [so] (0 la chua hoc bai nao), ghi thang khong qua nhat ky. */
    private fun moc(ma: String, so: Int) = MocThu.datPhan(context, ma, so)

    /** Cau sach bai tap that lam duoc tren may, de de gia dung duoc [DeGiai]. */
    private fun cauSbt(nguon: String, bo: Int, soLuong: Int = 6): List<String> =
        kho.cacCauCuaNguon(nguon).filter { it.lamTrenMay && it.dang != "KHONG_TINH" }
            .drop(bo).take(soLuong).map { it.id }

    /**
     * De gia: pham vi la moi bai tu bai dau toi [denBai] cua tung phan, dung kieu de giua ki, cuoi
     * ki that soan theo thu tu sach.
     */
    private fun deGia(ma: String, mon: String, nguon: String, denBai: Map<String, Int>, cauIds: List<String>) =
        DeThi.De(
            ma = ma, nguon = nguon, mon = mon, ten = "Đề thử $ma", denUnit = 0, denBai = denBai,
            phut = DeThi.phutMacDinh(mon),
            cacMuc = cauIds.mapIndexed { i, id -> DeThi.Muc(0, id, "Phần trắc nghiệm", "", "", "Câu ${i + 1}") },
            cacBai = denBai.flatMap { (ma, den) -> PhanHoc.theoMa(ma)!!.cacSoToi(den) }.toSet()
        )

    private val toanGk by lazy {
        deGia("TTHU-GK", LichKiemTra.TOAN, "sbttoan8t1", linkedMapOf("toan8ds" to 6, "toan8hh" to 12), cauSbt("sbttoan8t1", 0))
    }
    private val toanCk by lazy {
        deGia("TTHU-CK", LichKiemTra.TOAN, "sbttoan8t1", linkedMapOf("toan8ds" to 20, "toan8hh" to 17), cauSbt("sbttoan8t1", 40))
    }
    private val khtnGk by lazy {
        deGia("KTHU-GK", LichKiemTra.KHTN, "sbtkhtn8", linkedMapOf("khtn8hoa" to 5), cauSbt("sbtkhtn8", 0))
    }

    private fun tuMo(bayGio: Long): List<DeGiai> =
        GiaiDe.taoDeThi(context, bayGio).also { ds -> deDaTao += ds.map { it.id } }
            .filter { it.mon != PhanHoc.TIENG_ANH }

    private fun maCua(ds: List<DeGiai>) = ds.mapNotNull { GiaiDe.maDeThi(it) }.toSet()

    private fun nopGia(de: DeGiai, luc: Long) =
        kho.luuDe(de.copy(batDau = luc - 30 * 60_000L, nopLuc = luc, saoDat = 0))

    // ------------------------------------------------------------ doc file de

    private val SACH_TOAN = NganHang.Sach(
        nguon = "dethitoan8", mon = LichKiemTra.TOAN, ten = "Đề thi Toán 8", file = "", deThi = true
    )
    private val SACH_KHTN = NganHang.Sach(
        nguon = "dethikhtn8", mon = LichKiemTra.KHTN, ten = "Đề thi KHTN 8", file = "", deThi = true
    )

    private val FILE_TOAN = """
        {"nguon": "dethitoan8", "mon": "Toán", "ban": 1, "cac_bai": [
         {"chuong": "Giữa học kì 1", "bai": "Đề giữa kì 1 số 2", "de_thi": {"ma": "TGK1-2"}, "cac_cau": [
          {"ma": "TGK1-2.C1", "nhan": "Câu 1", "phan": "Phần trắc nghiệm", "nhom": "", "bai_sgk": 3},
          {"ma": "TGK1-2.C2", "nhan": "Câu 2", "phan": "Phần trắc nghiệm", "nhom": "", "bai_sgk": 6},
          {"ma": "TGK1-2.C3", "nhan": "Câu 3", "phan": "Phần trắc nghiệm", "nhom": "", "bai_sgk": 10},
          {"ma": "TGK1-2.B3a", "nhan": "Bài 3a", "phan": "Phần tự luận", "nhom": "Bài 3. (2,5 điểm) Cho hình bình hành ABCD.",
           "bai_sgk": 12, "hinh_goi_y": ["dethitoan8/TGK1-2.B3.goi-y.webp"], "hinh_day_du": ["dethitoan8/TGK1-2.B3.day-du.webp"]},
          {"ma": "TGK1-2.B3b", "nhan": "Bài 3b", "phan": "Phần tự luận", "nhom": "Bài 3. (2,5 điểm) Cho hình bình hành ABCD.",
           "bai_sgk": 14, "hinh_goi_y": ["dethitoan8/TGK1-2.B3.goi-y.webp"], "hinh_day_du": ["dethitoan8/TGK1-2.B3.day-du.webp"]},
          {"ma": "TGK1-2.B4", "nhan": "Bài 4", "phan": "Phần tự luận", "nhom": "Bài 4.", "bai_sgk": 17, "bo_may": "vẽ hình"}
         ]},
         {"chuong": "Cuối học kì 1", "bai": "Đề cuối kì 1 số 1", "de_thi": {"ma": "TCK1-1", "phut": 45}, "cac_cau": [
          {"ma": "TCK1-1.C1", "nhan": "Câu 1", "phan": "Phần trắc nghiệm", "nhom": "", "trung": "TGK1-2.B3a"},
          {"ma": "TCK1-1.C2", "nhan": "Câu 2", "phan": "Phần trắc nghiệm", "nhom": "", "bai_sgk": 20}
         ]},
         {"chuong": "Cuối học kì 1", "bai": "Đề cuối kì 1 số 2", "de_thi": {"ma": "TCK1-2"}, "cac_cau": [
          {"ma": "TCK1-2.C1", "nhan": "Câu 1", "phan": "Phần trắc nghiệm", "nhom": "", "bai_sgk": 2},
          {"ma": "TCK1-2.C2", "nhan": "Câu 2", "phan": "Phần trắc nghiệm", "nhom": ""}
         ]}
        ]}
    """.trimIndent()

    @Test
    fun doc_bai_sgk_ra_pham_vi_tung_phan_dong_ho_va_hinh_an() {
        val (gk, ck1, ck2) = DeThi.docTuChuoi(FILE_TOAN, SACH_TOAN)

        assertEquals("cau ve bo may khong tinh vao pham vi", linkedMapOf("toan8ds" to 6, "toan8hh" to 14), gk.denBai)
        assertEquals("pham vi la dung cac bai cau cham toi", setOf(3, 6, 10, 12, 14), gk.cacBai)
        assertEquals(listOf("toan8ds", "toan8hh"), gk.denBai.keys.toList())
        assertEquals("de khong in gio thi Toan 90 phut", 90, gk.phut)
        assertEquals(0, gk.denUnit)
        assertEquals("cau bo may khong vao de", 5, gk.cauIds.size)
        val b3a = gk.cacMuc.first { it.nhan == "Bài 3a" }
        assertTrue(b3a.coHinhAn)
        assertEquals(listOf("dethitoan8/TGK1-2.B3.goi-y.webp"), b3a.hinhGoiY)
        assertEquals("Bài 3", DeThi.tenBai(b3a))
        assertEquals("Câu 2", DeThi.tenBai(gk.cacMuc[1]))
        assertEquals("doRong: Dai so Bai 1-6 la 6 bai, Hinh hoc Bai 10-14 la 5 bai", 6 + 5, gk.doRong)

        assertEquals("de in gio thi theo gio in", 45, ck1.phut)
        assertEquals("cau trung dung id cau goc", "dethitoan8:TGK1-2.B3a", ck1.cacMuc[0].cauId)
        assertEquals("cau trung lay bai_sgk cua cau goc", linkedMapOf("toan8ds" to 20, "toan8hh" to 12), ck1.denBai)
        assertEquals(setOf(12, 20), ck1.cacBai)
        assertEquals("cau trung lay hinh an cua cau goc", b3a.hinhDayDu, ck1.cacMuc[0].hinhDayDu)
        assertEquals("cau trung giu nhan rieng", "Câu 1", ck1.cacMuc[0].nhan)

        assertEquals("thieu bai_sgk thi coi nhu thi ca nam", linkedMapOf("toan8ds" to 32, "toan8hh" to 39), ck2.denBai)
        assertEquals("thieu bai_sgk thi pham vi la ca sach", (1..39).toSet(), ck2.cacBai)
    }

    @Test
    fun de_khtn_khong_in_gio_thi_60_phut_va_chi_xet_phan_co_cau() {
        val chu = """
            {"nguon": "dethikhtn8", "mon": "Khoa học tự nhiên", "ban": 1, "cac_bai": [
             {"bai": "Đề giữa kì 1 số 1", "de_thi": {"ma": "KGK1-1"}, "cac_cau": [
              {"ma": "KGK1-1.C3", "nhan": "Câu 3", "phan": "I. Trắc nghiệm", "nhom": "", "bai_sgk": 2},
              {"ma": "KGK1-1.C8", "nhan": "Câu 8", "phan": "I. Trắc nghiệm", "nhom": "", "bai_sgk": 8}
             ]}]}
        """.trimIndent()
        val de = DeThi.docTuChuoi(chu, SACH_KHTN).single()
        assertEquals(60, de.phut)
        assertEquals(mapOf("khtn8hoa" to 8), de.denBai)
        assertEquals("Bài 2, 8", GiaiDe.moTaPhamVi(de))
        val moc = GiaiDe.MocHoc(mapOf(LichKiemTra.KHTN to (1..9).toSet()))
        assertTrue("chua danh dau bai Li, Sinh khong chan de chi co cau Hoa", GiaiDe.thieuPhamVi(de, moc).isEmpty())
    }

    // ------------------------------------------------------------ pham vi va chu mo ta

    private fun mocToan(cac: Set<Int>?) = GiaiDe.MocHoc(mapOf(LichKiemTra.TOAN to cac))

    @Test
    fun thieu_pham_vi_noi_dung_bai_con_thieu() {
        val gk = DeThi.docTuChuoi(FILE_TOAN, SACH_TOAN).first()
        assertEquals("Bài 3, 6, 10, 12, 14", GiaiDe.moTaPhamVi(gk))

        val thieu = GiaiDe.thieuPhamVi(gk, mocToan(((1..9) + (10..12)).toSet()))
        assertEquals(listOf(GiaiDe.Thieu(listOf(14), laUnit = false)), thieu)
        assertEquals("thiếu Bài 14", GiaiDe.moTaThieu(thieu))

        assertTrue(
            "chi can dung cac bai de hoi, khong can bai dau sach",
            GiaiDe.thieuPhamVi(gk, mocToan(setOf(3, 6, 10, 12, 14))).isEmpty()
        )
        // Luat 2/10/2026: danh dau cach quang thi bai bo trong van thieu, du bai cao nhat da qua.
        assertEquals(
            "thiếu Bài 6",
            GiaiDe.moTaThieu(GiaiDe.thieuPhamVi(gk, mocToan((1..14).toSet() - 6)))
        )
        assertEquals("chưa chọn bài đã học", GiaiDe.moTaThieu(GiaiDe.thieuPhamVi(gk, mocToan(null))))
        assertEquals("thiếu Bài 3, 6, 10, 12, 14", GiaiDe.moTaThieu(GiaiDe.thieuPhamVi(gk, mocToan(emptySet()))))

        // Dai so danh so ngat quang (Bai 1-9 roi 18-32): Bai 20 nam sau Hinh hoc trong sach.
        val ck = DeThi.docTuChuoi(FILE_TOAN, SACH_TOAN)[1]
        assertTrue(GiaiDe.thieuPhamVi(ck, mocToan(((1..9) + (18..21) + (10..12)).toSet())).isEmpty())
        assertEquals(1, GiaiDe.thieuPhamVi(ck, mocToan((1..12).toSet())).size)

        val anh = DeThi.tatCa(context).first { it.ma == "GK1-1" }
        fun mocAnh(cac: Set<Int>?) = GiaiDe.MocHoc(mapOf(PhanHoc.TIENG_ANH to cac))
        assertEquals("Unit 1–3", GiaiDe.moTaPhamVi(anh))
        assertEquals("thiếu Unit 3", GiaiDe.moTaThieu(GiaiDe.thieuPhamVi(anh, mocAnh(setOf(1, 2)))))
        assertTrue(GiaiDe.thieuPhamVi(anh, mocAnh(setOf(1, 2, 3))).isEmpty())
        assertEquals("thiếu Unit 1", GiaiDe.moTaThieu(GiaiDe.thieuPhamVi(anh, mocAnh(setOf(2, 3)))))
    }

    // ------------------------------------------------------------ tu mo, moi mon mot de

    @Test
    fun moi_mon_mot_de_mo_khi_du_moi_bai_de_hoi() {
        assertTrue("cau sbt du dung de gia", toanGk.cauIds.size >= 3 && toanCk.cauIds.size >= 3 && khtnGk.cauIds.size >= 3)
        DeThi.thayChoTest = listOf(toanGk, toanCk, khtnGk)

        assertTrue("chua chon bai da hoc thi khong mo", tuMo(toi).isEmpty())

        moc("toan8ds", 9)
        moc("toan8hh", 12)
        moc("khtn8hoa", 5)
        val lan1 = tuMo(toi)
        assertEquals("Toan giua ki va KHTN cung mo, Toan cuoi ki chua toi", setOf("TTHU-GK", "KTHU-GK"), maCua(lan1))
        val toan = lan1.first { GiaiDe.maDeThi(it) == "TTHU-GK" }
        assertEquals(90, toan.phutGoiY)
        assertEquals(60, lan1.first { GiaiDe.maDeThi(it) == "KTHU-GK" }.phutGoiY)
        assertEquals(GiaiDe.LOAI_DE_THI, toan.loai)
        assertEquals(LichKiemTra.TOAN, toan.mon)

        assertTrue("moi mon dang co mot de thi khong mo them", tuMo(toi + 60_000L).isEmpty())

        nopGia(toan, toi + 40 * 60_000L)
        assertTrue("nop trong ngay thi hom nay Toan khong mo de tiep", tuMo(toi + 50 * 60_000L).isEmpty())
        assertTrue("hom sau: de cuoi ki chua toi pham vi, de giua ki da nop", tuMo(toi + motNgay).isEmpty())

        moc("toan8ds", 20)
        moc("toan8hh", 17)
        val lan2 = tuMo(toi + motNgay + 60_000L)
        assertEquals("KHTN con de dang mo nen chi Toan mo them", setOf("TTHU-CK"), maCua(lan2))
    }

    @Test
    fun hai_de_cung_toi_pham_vi_thi_de_rong_hon_mo_truoc() {
        DeThi.thayChoTest = listOf(toanGk, toanCk)
        moc("toan8ds", 21)
        moc("toan8hh", 17)
        assertEquals(setOf("TTHU-CK"), maCua(tuMo(toi)))
    }

    @Test
    fun le_hoa_mo_tay_thi_biet_phan_con_thieu_ba_huy_mo_duoc() {
        DeThi.thayChoTest = listOf(toanGk, toanCk)
        moc("toan8ds", 9)
        moc("toan8hh", 12)
        val con = GiaiDe.moDeThi(context, "TTHU-CK", choBa = false, bayGio = toi)
        assertNull(con.de)
        val loi = con.loi.orEmpty()
        assertTrue(loi, loi.contains("mở khi đã học Bài 1–20"))
        assertTrue(loi, loi.contains("Còn thiếu Bài 13–20"))

        val ba = GiaiDe.moDeThi(context, "TTHU-CK", choBa = true, bayGio = toi)
        assertNotNull(ba.de)
        deDaTao += ba.de!!.id
        assertEquals(90, ba.de!!.phutGoiY)
    }

    @Test
    fun tinh_trang_ghi_phan_con_thieu_cua_de_khoa() {
        DeThi.thayChoTest = listOf(toanGk, toanCk, khtnGk)
        moc("toan8ds", 9)
        moc("toan8hh", 12)
        val tt = GiaiDe.tinhTrangDeThi(context, toi).associateBy { it.de.ma }
        assertEquals(GiaiDe.TT_SAN, tt["TTHU-GK"]?.trangThai)
        assertEquals(GiaiDe.TT_KHOA, tt["TTHU-CK"]?.trangThai)
        assertEquals("thiếu Bài 13–20", GiaiDe.moTaThieu(tt["TTHU-CK"]!!.thieu))
        assertEquals("KHTN chua chon bai da hoc", GiaiDe.TT_KHOA, tt["KTHU-GK"]?.trangThai)
        assertEquals("chưa chọn bài đã học", GiaiDe.moTaThieu(tt["KTHU-GK"]!!.thieu))
    }

    @Test
    fun hinh_an_tim_duoc_theo_id_cau_cho_man_on_lai() {
        val gk = DeThi.docTuChuoi(FILE_TOAN, SACH_TOAN).first()
        DeThi.thayChoTest = listOf(gk)
        val ha = DeThi.hinhAnCua(context, "dethitoan8:TGK1-2.B3b")
        assertNotNull(ha)
        assertEquals("TGK1-2", ha!!.de.ma)
        assertEquals(listOf("dethitoan8/TGK1-2.B3.day-du.webp"), ha.muc.hinhDayDu)
        assertNull("cau khong co hinh an", DeThi.hinhAnCua(context, "dethitoan8:TGK1-2.C1"))
    }

    /** Ghi hai dong vao nhat ky hom nay; [tearDown] tra lai nhat ky nhu truoc test. */
    @Test
    fun xem_hinh_ghi_nhat_ky_moi_ngay_mot_lan_moi_muc() {
        val gk = DeThi.docTuChuoi(FILE_TOAN, SACH_TOAN).first()
        val b3a = gk.cacMuc.first { it.nhan == "Bài 3a" }
        val con = context.getString(R.string.child_name)
        val goiY = "$con xem hình gợi ý Bài 3, Đề thi thử Toán (Đề giữa kì 1 số 2)"
        val dayDu = "$con xem hình đầy đủ Bài 3, Đề thi thử Toán (Đề giữa kì 1 số 2)"
        fun dem(dong: String) = DayLog.today(context).lines().count { it.endsWith(dong) }
        val goiYTruoc = dem(goiY)
        GiaiDe.ghiXemHinh(context, gk, b3a, dayDu = false)
        GiaiDe.ghiXemHinh(context, gk, b3a, dayDu = false)
        assertEquals("bam lai trong ngay khong ghi them", 1, dem(goiY))
        assertTrue(goiYTruoc <= 1)
        GiaiDe.ghiXemHinh(context, gk, b3a, dayDu = true)
        assertEquals(1, dem(dayDu))
    }
}
