package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.DeThi
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * De thi in san (30/9/2026): khung de doc tu assets, cau trung dung chung id, de tu mo khi lop
 * hoc toi pham vi, Le Hoa va Ba Huy mo bang tay. Xem [DeThi], [GiaiDe.taoDeThi], [GiaiDe.moDeThi].
 *
 * CAN THAN: bo test nay ghi moc Unit Tieng Anh va de vao kho that tren may, roi tra lai moc cu
 * va xoa moi de no tao. Nop de thi gia bang cach ghi thang luc nop vao de, khong di qua
 * [GiaiDe.nop], nen khong ghi so cai, khong cap phut nao. Mo de bang tay ghi dong nhat ky
 * ("Lê Hòa tự mở ..."): test chup nhat ky hom nay truoc va tra lai sau.
 *
 * Tu 1/10/2026 co ca bo de Toan, KHTN (buoc 4.4), nen moi phep so o day chi xet de Tieng Anh
 * ([anh], [tuMo]); luat mo de cua hai mon kia o [DeThiPhanTest].
 */
@RunWith(AndroidJUnit4::class)
class DeThiTest {

    private lateinit var context: Context
    private lateinit var kho: KhoBai
    private var unitCu: Set<Int>? = null
    private val deDaTao = mutableListOf<String>()
    private var nhatKyCu: Pair<Int, String?> = 0 to null

    /** Toi thu Ba 20/10/2026, sau khi lop hoc xong Unit 3: gan ngay kiem tra giua ki 1. */
    private val toi = ms(LocalDateTime.of(2026, 10, 20, 19, 0))
    private val motNgay = 24 * 60 * 60_000L

    private fun ms(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        kho = KhoBai.get(context)
        unitCu = HocToi.daHoc(context, PhanHoc.TIENG_ANH)
        HocToi.xoa(context, PhanHoc.TIENG_ANH)
        val sp = Prefs.get(context).raw()
        nhatKyCu = sp.getInt(DayLog.K_DAY, 0) to sp.getString(DayLog.K_TEXT, null)
    }

    @After
    fun tearDown() {
        deDaTao.forEach { kho.xoaDe(it) }
        MocThu.dat(context, PhanHoc.TIENG_ANH, unitCu)
        Prefs.get(context).raw().edit()
            .putInt(DayLog.K_DAY, nhatKyCu.first)
            .putString(DayLog.K_TEXT, nhatKyCu.second)
            .commit()
    }

    /** Cac de Tieng Anh, theo thu tu trong file. */
    private fun anh(): List<DeThi.De> = DeThi.tatCa(context).filter { it.laTiengAnh }

    /** Lop hoc toi Unit [unit]: danh dau Unit 1 toi [unit]. */
    private fun moc(unit: Int) = MocThu.datUnit(context, unit)

    /**
     * Tu 1/10/2026 [GiaiDe.taoDeThi] mo moi mon mot de va tra ca danh sach. O day chi lay de
     * Tieng Anh; de mon khac mo ra (khi may co moc Toan, KHTN) van vao [deDaTao] de xoa.
     */
    private fun tuMo(bayGio: Long): DeGiai? =
        GiaiDe.taoDeThi(context, bayGio).also { ds -> deDaTao += ds.map { it.id } }
            .firstOrNull { it.mon == PhanHoc.TIENG_ANH }

    private fun moTay(ma: String, choBa: Boolean, bayGio: Long): GiaiDe.MoDeThi =
        GiaiDe.moDeThi(context, ma, choBa, bayGio).also { kq -> kq.de?.let { if (it.id !in deDaTao) deDaTao += it.id } }

    /** Con nop de luc [luc], 0 sao: ghi thang vao de, khong ghi so, khong cap phut. */
    private fun nopGia(de: DeGiai, luc: Long) =
        kho.luuDe(de.copy(batDau = luc - 30 * 60_000L, nopLuc = luc, saoDat = 0))

    // ------------------------------------------------------------ khung de

    @Test
    fun ba_de_giua_ki_1_du_khung_so_cau_in_va_phan_nghe_bo() {
        val cac = anh()
        assertEquals(listOf("GK1-1", "GK1-2", "GK1-3"), cac.map { it.ma }.take(3))
        cac.take(3).forEach { de ->
            assertEquals("${de.ma}: pham vi", 3, de.denUnit)
            assertEquals("${de.ma}: gio lam", 45, de.phut)
            assertEquals("${de.ma}: so cau in", (1..40).toList(), de.cacMuc.map { it.so })
            assertEquals("${de.ma}: cau nghe", (31..35).toList(), de.cacMuc.filter { it.boMay == "nghe" }.map { it.so })
            assertEquals("${de.ma}: cau lam tren may", 35, de.cauIds.size)
            assertTrue("${de.ma}: moi cau co loi dan", de.cacMuc.all { it.nhom.isNotBlank() && it.phan.isNotBlank() })
        }
        assertEquals("Đề giữa kì 1 số 1", cac.first().ten)
    }

    /**
     * Dot hai (30/9/2026): giua ki 1 so 4 toi 6, cuoi ki 1 so 1 toi 5 va 7, 8. Ba Huy chot: de
     * dang nam 2024 co cau cua Unit 7 toi 9 thi doi lop hoc toi Unit 9 moi mo; cuoi ki 1 so 6 in
     * lai y het giua ki 1 so 4 nen bo.
     */
    @Test
    fun du_muoi_ba_de_moi_de_du_so_cau_in_va_pham_vi_unit() {
        val cac = anh().filter { it.ma.startsWith("GK1-") || it.ma.startsWith("CK1-") }
        val denUnit = linkedMapOf(
            "GK1-1" to 3, "GK1-2" to 3, "GK1-3" to 3, "GK1-4" to 9, "GK1-5" to 9, "GK1-6" to 9,
            "CK1-1" to 5, "CK1-2" to 6, "CK1-3" to 6, "CK1-4" to 6, "CK1-5" to 6, "CK1-7" to 9, "CK1-8" to 9
        )
        assertEquals(denUnit.keys.toList(), cac.map { it.ma })
        cac.forEach { de ->
            assertEquals("${de.ma}: pham vi", denUnit[de.ma], de.denUnit)
            assertEquals("${de.ma}: gio lam", 45, de.phut)
            assertEquals("${de.ma}: so cau in", (1..40).toList(), de.cacMuc.map { it.so })
            val nghe = de.cacMuc.filter { it.boMay.isNotBlank() }.map { it.so }
            assertTrue("${de.ma}: phan nghe lien mot khuc: $nghe", nghe.isNotEmpty() && nghe == (nghe.first()..nghe.last()).toList())
            assertTrue("${de.ma}: chi bo phan nghe", de.cacMuc.all { it.boMay.isBlank() || it.boMay == "nghe" })
            assertEquals("${de.ma}: cau lam tren may", 40 - nghe.size, de.cauIds.size)
            assertTrue("${de.ma}: moi cau co loi dan", de.cacMuc.all { it.nhom.isNotBlank() })
        }
        assertEquals("Đề cuối kì 1 số 1", cac.first { it.ma == "CK1-1" }.ten)
        assertEquals((1..10).toList(), cac.first { it.ma == "CK1-1" }.cacMuc.filter { it.boMay == "nghe" }.map { it.so })
    }

    /**
     * Hoc ki 2 (buoc 4.9, tu 7/10/2026): giua ki 2 so 1-7, cuoi ki 2 so 1-7, gop dan tung lo nen
     * bang duoi lon dan theo tung lo. Moi de 40 cau in, cau 1-10 la phan nghe (bo), con lai lam
     * tren may, ke ca cau "trung" (tro ve cau goc o de khac, xem gop_de.py TRUNG_TAY). De sau cua
     * mot ky dung sau de truoc trong file (gop_de.py xep GK1, CK1, GK2, CK2).
     */
    @Test
    fun de_hoc_ki_2_du_so_cau_in_va_pham_vi_unit() {
        val cac = anh().filter { it.ma.startsWith("GK2-") || it.ma.startsWith("CK2-") }
        // ma de -> (den_unit, phut)
        val khung = linkedMapOf(
            "GK2-1" to (9 to 45), "GK2-2" to (9 to 45), "GK2-3" to (9 to 45), "GK2-4" to (9 to 45)
        )
        assertEquals(khung.keys.toList(), cac.map { it.ma })
        cac.forEach { de ->
            assertEquals("${de.ma}: pham vi", khung.getValue(de.ma).first, de.denUnit)
            assertEquals("${de.ma}: gio lam", khung.getValue(de.ma).second, de.phut)
            assertEquals("${de.ma}: so cau in", (1..40).toList(), de.cacMuc.map { it.so })
            assertEquals("${de.ma}: cau nghe", (1..10).toList(), de.cacMuc.filter { it.boMay == "nghe" }.map { it.so })
            assertTrue("${de.ma}: chi bo phan nghe", de.cacMuc.all { it.boMay.isBlank() || it.boMay == "nghe" })
            assertEquals("${de.ma}: cau lam tren may", 30, de.cauIds.size)
            assertTrue("${de.ma}: moi cau co loi dan", de.cacMuc.all { it.nhom.isNotBlank() })
        }
        assertEquals("Đề giữa kì 2 số 1", cac.first().ten)
        // Cau gan trung khac loi dan dung chung id cau goc, ke ca cau goc o de hoc ki 1.
        assertTrue(cac.first { it.ma == "GK2-2" }.cauIds.contains("dethianh8:GK1-4.9"))
    }

    @Test
    fun de_cuoi_ki_1_mo_khi_lop_hoc_toi_pham_vi_de_co_cau_hoc_ki_2_doi_toi_unit_9() {
        moc(5)
        assertEquals("Unit 5: de cuoi ki 1 so 1 cham toi Unit 5, mo truoc de giua ki", "CK1-1", tuMo(toi)?.let { GiaiDe.maDeThi(it) })
        deDaTao.forEach { kho.xoaDe(it) }
        deDaTao.clear()

        moc(6)
        assertEquals("Unit 6: de pham vi cao nhat, theo thu tu trong file", "CK1-2", tuMo(toi)?.let { GiaiDe.maDeThi(it) })
        val con = moTay("GK1-4", choBa = false, bayGio = toi)
        assertNull("de co cau Unit 9 chua mo cho con o Unit 6", con.de)
        // Tu 2/10/2026 cau noi ca pham vi va phan con thieu: "mở khi đã học Unit 1–9. Còn thiếu Unit 7–9."
        assertTrue(con.loi.orEmpty(), con.loi.orEmpty().contains("Unit 1–9"))
        assertTrue(con.loi.orEmpty(), con.loi.orEmpty().contains("thiếu Unit 7–9"))
        deDaTao.forEach { kho.xoaDe(it) }
        deDaTao.clear()

        moc(9)
        assertEquals("Unit 9: de giua ki 1 so 4 dung dau cac de pham vi 9", "GK1-4", tuMo(toi)?.let { GiaiDe.maDeThi(it) })
    }

    @Test
    fun cau_trung_giua_hai_de_dung_chung_id_cau_goc() {
        val (d1, d2, d3) = anh().take(3)
        fun id(de: DeThi.De, so: Int) = de.cacMuc.first { it.so == so }.cauId
        (1..4).forEach { assertEquals("de 2 cau $it", id(d1, it), id(d2, it)) }
        assertEquals(id(d1, 5), id(d3, 5))
        assertEquals(id(d1, 7), id(d3, 9))
        assertEquals(id(d1, 20), id(d3, 15))
        assertEquals(id(d1, 19), id(d3, 19))
        assertEquals("dethianh8:GK1-1.1", id(d2, 1))
    }

    @Test
    fun moi_cau_cua_de_co_trong_kho_va_lam_duoc_tren_may() {
        DeThi.tatCa(context).forEach { de ->
            val trongKho = kho.cacCauTheoId(de.cauIds).associateBy { it.id }
            val thieu = de.cauIds.filter { it !in trongKho }
            assertTrue("${de.ma} thieu cau: $thieu", thieu.isEmpty())
            assertTrue("${de.ma} co cau khong lam tren may", trongKho.values.all { it.lamTrenMay })
        }
    }

    @Test
    fun de_thi_khong_hien_o_man_chon_sach_va_khong_vao_lam_them() {
        assertFalse(NganHang.sachCua(PhanHoc.TIENG_ANH).any { it.deThi })
        assertFalse(NganHang.sachBaiTapCua(PhanHoc.TIENG_ANH).any { it.deThi })
        assertTrue(NganHang.boDeThi().any { it.nguon == "dethianh8" })
        assertTrue(NganHang.boDeThi().all { it.deThi })
        moc(4)
        // Cau moi cua Luyen tap chi rut tu sach bai tap. Cau de thi con da lam trong Giai de
        // thi dung luat van quay lai o buoc lam lai cua Luyen tap, nen chi xet cau chua lam
        // luot nao. Truoc 2/10/2026 test nay hoi ham chon cau cua buoc lam them chup anh.
        val lamThem = LamTrenMay.cauLamThem(context, PhanHoc.TIENG_ANH).filter { it.soLuot == 0 }
        assertTrue("lam them rut cau de thi", lamThem.none { it.cau.nguon == "dethianh8" })
    }

    // ------------------------------------------------------------ tu mo theo moc

    @Test
    fun tu_mo_khi_lop_hoc_toi_pham_vi_moi_luc_mot_de_de_sau_mo_tu_hom_sau() {
        assertNull("chua chon moc thi khong mo", tuMo(toi))
        moc(2)
        assertNull("Unit 2 chua toi pham vi de giua ki 1", tuMo(toi))

        moc(3)
        val de1 = tuMo(toi)
        assertNotNull(de1)
        de1!!
        assertEquals(GiaiDe.LOAI_DE_THI, de1.loai)
        assertEquals("GK1-1", GiaiDe.maDeThi(de1))
        assertEquals("Đề giữa kì 1 số 1", de1.ten)
        assertEquals(45, de1.phutGoiY)
        assertEquals("giu dung thu tu in", DeThi.theoMa(context, "GK1-1")!!.cauIds, de1.cauIds)
        assertEquals("sao: 16 cau chon, 5 doc hieu, 10 go chu, 5 dien doan, 5 xep the", 55, de1.saoToiDa)
        assertNull("dang mo mot de thi thi khong mo them", tuMo(toi + 60_000L))

        nopGia(de1, toi + 45 * 60_000L)
        assertNull("nop trong ngay thi hom nay khong mo de tiep", tuMo(toi + 50 * 60_000L))
        val de2 = tuMo(toi + motNgay)
        assertEquals("hom sau mo de tiep theo trong file", "GK1-2", de2?.let { GiaiDe.maDeThi(it) })
        assertEquals(50, de2!!.saoToiDa)
    }

    @Test
    fun de_da_nop_khong_tu_mo_lai() {
        moc(3)
        listOf("GK1-1", "GK1-2", "GK1-3").forEachIndexed { i, ma ->
            val de = tuMo(toi + i * motNgay)
            assertEquals(ma, de?.let { GiaiDe.maDeThi(it) })
            nopGia(de!!, toi + i * motNgay + 40 * 60_000L)
        }
        assertNull("ba de deu nop roi", tuMo(toi + 5 * motNgay))
    }

    // ------------------------------------------------------------ mo bang tay

    @Test
    fun le_hoa_chi_mo_de_da_toi_pham_vi_ba_huy_mo_duoc_moi_de() {
        moc(1)
        val con = moTay("GK1-2", choBa = false, bayGio = toi)
        assertNull(con.de)
        assertTrue(con.loi.orEmpty(), con.loi.orEmpty().contains("Unit 1–3"))
        assertTrue(con.loi.orEmpty(), con.loi.orEmpty().contains("thiếu Unit 2–3"))

        val ba = moTay("GK1-2", choBa = true, bayGio = toi)
        assertNotNull(ba.de)
        val lai = moTay("GK1-2", choBa = true, bayGio = toi + 60_000L)
        assertEquals("de dang mo thi tra lai chinh no", ba.de!!.id, lai.de?.id)

        assertNull(moTay("GK9-9", choBa = true, bayGio = toi).de)
    }

    @Test
    fun de_da_nop_lam_lai_duoc_tu_hom_sau() {
        moc(3)
        val lan1 = moTay("GK1-3", choBa = false, bayGio = toi).de!!
        nopGia(lan1, toi + 40 * 60_000L)
        val cungNgay = moTay("GK1-3", choBa = false, bayGio = toi + 60 * 60_000L)
        assertNull(cungNgay.de)
        assertTrue(cungNgay.loi.orEmpty(), cungNgay.loi.orEmpty().contains("hôm nay"))
        val lan2 = moTay("GK1-3", choBa = false, bayGio = toi + motNgay).de
        assertNotNull(lan2)
        assertNotEquals("lam lai la mot de moi", lan1.id, lan2!!.id)
        assertEquals(lan1.cauIds, lan2.cauIds)
    }

    @Test
    fun tinh_trang_tung_de_cho_luyen_tap_va_dien_thoai() {
        fun tt(bayGio: Long) = GiaiDe.tinhTrangDeThi(context, bayGio).associate { it.de.ma to it.trangThai }
        assertEquals(GiaiDe.TT_KHOA, tt(toi)["GK1-1"])
        moc(3)
        assertEquals(GiaiDe.TT_SAN, tt(toi)["GK1-1"])
        val de = tuMo(toi)!!
        assertEquals(GiaiDe.TT_MO, tt(toi)["GK1-1"])
        kho.luuDe(de.copy(batDau = toi + 60_000L))
        assertEquals(GiaiDe.TT_DANG, tt(toi + 120_000L)["GK1-1"])
        nopGia(de, toi + 40 * 60_000L)
        val sau = GiaiDe.tinhTrangDeThi(context, toi + 50 * 60_000L).first { it.de.ma == "GK1-1" }
        assertEquals(GiaiDe.TT_XONG, sau.trangThai)
        assertEquals(de.id, sau.lanCuoi?.id)
        assertEquals(GiaiDe.TT_SAN, tt(toi + 50 * 60_000L)["GK1-2"])
    }
}
