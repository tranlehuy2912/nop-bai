package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauBoQua
import vn.huytl.homeworkgate.data.CongSang
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.QuyGio
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.kho.TraLoi
import java.util.Calendar

/**
 * Bai may giao lam tren may ([LamTrenMay], tu 29/9/2026): chon cau trong phan Le Hoa da hoc,
 * lam lai sau 24 gio, on lai theo hen, cong phut theo tran rieng, phan vuot tran vao quy,
 * gio ngu thi giu toi sang.
 *
 * Dung du lieu that trong ngan hang: luc viet test (29/9/2026) co ghep cho SBT Toan tap mot
 * chuong I va SBT Tieng Anh Unit 1, 2.
 *
 * Don dep: xoa so cai (nhu SuaChamLenhTest), tra lai moc hoc, quy, gio ngu, dong phieu gio.
 */
@RunWith(AndroidJUnit4::class)
class LamTrenMayTest {

    private lateinit var context: Context
    private lateinit var kho: KhoBai
    private var mocCu: Map<String, Set<Int>?> = emptyMap()
    private var quyCu = 0
    private var nguCu = 0
    private var dayCu = 0

    private val gio = 60 * 60_000L
    private val ngay = 24 * gio

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        kho = KhoBai.get(context)
        SoCaiBai.xoaHet(context)
        CongSang.xoaHet(context)
        CauBoQua.xoaHet(context)
        mocCu = MocThu.luu(context)
        val p = Prefs.get(context)
        quyCu = p.quyGio
        nguCu = p.hardStopMinuteOfDay
        dayCu = p.gioDayMinuteOfDay
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        datGioNgu(dangNgu = false)
    }

    @After
    fun tearDown() {
        SoCaiBai.xoaHet(context)
        CongSang.xoaHet(context)
        CauBoQua.xoaHet(context)
        MocThu.tra(context, mocCu)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        val p = Prefs.get(context)
        p.quyGio = quyCu
        p.hardStopMinuteOfDay = nguCu
        p.gioDayMinuteOfDay = dayCu
    }

    /** Dat gio ngu quanh gio that: dang ngu thi con mot tieng nua, khong thi ba tieng nua moi ngu. */
    private fun datGioNgu(dangNgu: Boolean) {
        val c = Calendar.getInstance()
        val bayGio = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val phutNgay = 24 * 60
        Prefs.get(context).hardStopMinuteOfDay = (bayGio + if (dangNgu) phutNgay - 60 else 180) % phutNgay
        Prefs.get(context).gioDayMinuteOfDay = (bayGio + if (dangNgu) 60 else 240) % phutNgay
    }

    private fun datMocToan(soBai: Int) {
        MocThu.datToan(context, soBai)
    }

    private fun muc(id: String): LamTrenMay.Muc = LamTrenMay.muc(context, kho.cauTheoId(id)!!)!!

    /** Luot sai [sai] lan roi dung. */
    private fun luot(saoToiDa: Int, sai: Int = 0): LuatGhep.Luot {
        var l = LuatGhep.Luot(saoToiDa)
        repeat(sai) { l = LuatGhep.kiem(l, soSai = 1) }
        return LuatGhep.kiem(l, soSai = 0)
    }

    // ------------------------------------------------------------------ chon cau

    /**
     * Ba Huy chot 2/10/2026: mot luot la HET cau lam duoc cua cac bai da hoc (truoc do toi da 10
     * cau, moi bai 3 cau), xep tu bai cu toi bai moi (truoc do Bai 3, bai lop vua hoc, len dau).
     */
    @Test
    fun cau_moi_la_het_cau_cua_bai_da_hoc_bai_cu_truoc() {
        datMocToan(3)

        val ds = LamTrenMay.cauLamThem(context, "Toán")

        assertTrue("cau khong lam tren may", ds.all { it.cau.lamTrenMay })
        val soBai = ds.map { PhanHoc.soBai(it.cau.bai) }
        assertTrue("bai ngoai moc: $soBai", soBai.all { it != null && it in 1..3 })
        val so = soBai.filterNotNull()
        assertEquals(1, so.first())
        assertEquals("bai cu truoc: $so", so.sorted(), so)
        // Bai 1 toi Bai 3 cua SBT tap mot co hai muoi lam cau lam tren may: hon mot luot 10 cau cu.
        assertTrue("chi co ${ds.size} cau", ds.size > 10)
        val trongDe = kho.cauTrongDeConHan()
        val canCo = NganHang.cauSbtDaHoc(context, "Toán")
            .filter { it.lamTrenMay && it.id !in trongDe && LamTrenMay.muc(context, it) != null }
            .sortedBy { PhanHoc.soBai(it.bai) }
            .map { it.id }
        assertEquals("het cau lam duoc, theo thu tu in trong tung bai", canCo, ds.map { it.cau.id })
    }

    /**
     * Ly do Ba Huy chon bai cu truoc (2/10/2026): cau lam xong thi khong ra lai, nen con lam het
     * bai cu thi luot sau bat dau tu bai ke tiep, roi chi con bai moi.
     */
    @Test
    fun lam_het_bai_cu_thi_luot_sau_bat_dau_tu_bai_ke_tiep() {
        datMocToan(3)
        LamTrenMay.cauLamThem(context, "Toán")
            .filter { PhanHoc.soBai(it.cau.bai) == 1 }
            .forEach { LamTrenMay.ghi(context, it, luot(it.ghep.sao), "x", LamTrenMay.Loai.LAM_THEM, congNgay = false) }

        val sau = LamTrenMay.cauLamThem(context, "Toán")

        assertTrue(sau.isNotEmpty())
        assertTrue("con cau Bai 1: ${sau.map { it.cau.ma }}", sau.none { PhanHoc.soBai(it.cau.bai) == 1 })
        assertEquals(2, PhanHoc.soBai(sau.first().cau.bai))
    }

    /**
     * Bai vua sai (Ba Huy chot 2/10/2026): chi toi da ba cau chua lam cua bai do len dau, cac cau
     * con lai cua bai nam dung cho theo thu tu bai cu toi bai moi.
     */
    @Test
    fun bai_vua_sai_chi_dua_ba_cau_len_dau() {
        datMocToan(6)
        val t0 = System.currentTimeMillis()
        val sai = LamTrenMay.cauLamThem(context, "Toán", bayGio = t0).first { PhanHoc.soBai(it.cau.bai) == 6 }
        var l = LuatGhep.Luot(sai.ghep.sao)
        repeat(sai.ghep.sao + 1) { l = LuatGhep.kiem(l, soSai = 1) }
        assertTrue(l.xong && !l.dung)
        LamTrenMay.ghi(context, sai, l, "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0, congNgay = false)

        val so = LamTrenMay.cauLamThem(context, "Toán", bayGio = t0 + gio).map { PhanHoc.soBai(it.cau.bai)!! }

        val ba = LamTrenMay.SO_CAU_BAI_VUA_SAI
        assertEquals("ba cau Bai 6 len dau: $so", List(ba) { 6 }, so.take(ba))
        assertEquals("roi toi Bai 1: $so", 1, so[ba])
        val conLai = so.drop(ba)
        assertEquals("bai cu truoc: $conLai", conLai.sorted(), conLai)
        assertTrue("cau Bai 6 con lai nam o cuoi: $conLai", conLai.last() == 6)
    }

    /** Trang Luyen tap moi phan mot dong tu 2/10/2026: luot cua mot phan chi co cau phan do. */
    @Test
    fun luyen_tap_theo_phan_chi_lay_cau_cua_phan_do() {
        MocThu.datToan(context, 14)
        val hh = PhanHoc.theoMa("toan8hh")!!
        val ds = PhanHoc.theoMa("toan8ds")!!
        val cauHh = LamTrenMay.cauLamThem(context, "Toán", phan = hh)
        assertTrue(cauHh.isNotEmpty())
        assertTrue(cauHh.all { PhanHoc.phanCuaCau(it.cau) == hh })
        val cauDs = LamTrenMay.cauLamThem(context, "Toán", phan = ds)
        assertTrue(cauDs.isNotEmpty())
        assertTrue(cauDs.all { PhanHoc.phanCuaCau(it.cau) == ds })
    }

    /** So "đã làm / tổng" cua dong Luyen tap tinh tren ca phan, khong nhay khi danh dau them bai. */
    @Test
    fun dem_theo_phan_tong_dung_yen_khi_danh_dau_them_bai() {
        MocThu.datToan(context, 3)
        val truoc = LamTrenMay.demTheoPhan(context, "Toán")
        MocThu.datToan(context, 20)
        assertEquals(truoc, LamTrenMay.demTheoPhan(context, "Toán"))
        assertEquals(setOf("toan8ds", "toan8hh"), truoc.keys)
        assertTrue(truoc.values.all { (da, tong) -> da in 0..tong && tong > 0 })
        assertEquals("Tieng Anh khong chia phan", setOf(""), LamTrenMay.demTheoPhan(context, PhanHoc.TIENG_ANH).keys)
    }

    @Test
    fun chua_chon_moc_thi_khong_co_cau_moi() {
        MocThu.datToan(context, null)
        assertTrue(LamTrenMay.cauLamThem(context, "Toán").isEmpty())
    }

    @Test
    fun tieng_anh_theo_unit_da_hoc() {
        MocThu.datUnit(context, 1)
        val unit1 = LamTrenMay.cauLamThem(context, PhanHoc.TIENG_ANH)
        assertTrue(unit1.isNotEmpty())
        assertTrue(unit1.all { it.cau.bai.startsWith("Unit 1.") })

        MocThu.datUnit(context, 2)
        val unit2 = LamTrenMay.cauLamThem(context, PhanHoc.TIENG_ANH)
        // Unit cu truoc (Ba Huy chot 2/10/2026), Unit moi hoc van co trong luot.
        assertTrue(unit2.first().cau.bai.startsWith("Unit 1."))
        assertTrue(unit2.any { it.cau.bai.startsWith("Unit 2.") })
        assertTrue(unit2.all { PhanHoc.soBai(it.cau.bai) in 1..2 })
    }

    // ------------------------------------------------------------ lam lai, on lai

    @Test
    fun lam_lai_mo_sau_24_gio_va_chi_cong_phan_hon_lan_tot_nhat() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first()
        val v = m.ghep.sao
        val t0 = System.currentTimeMillis()

        // Lan dau sai mot lan: mat mot sao.
        val lan1 = LamTrenMay.ghi(context, m, luot(v, sai = 1), "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0)
        assertEquals(v - 1, lan1.sao)
        assertEquals(v - 1, lan1.phutCap)

        // Chua du 24 gio: khong co lai, cung khong tinh la cau moi.
        assertTrue(LamTrenMay.cauLamThem(context, "Toán", bayGio = t0 + gio).none { it.cau.id == m.cau.id })

        // Qua 24 gio: cau do dung dau danh sach.
        val sau = LamTrenMay.cauLamThem(context, "Toán", bayGio = t0 + 25 * gio)
        assertEquals(m.cau.id, sau.first().cau.id)

        // Lam lai du sao: chi cong phan hon lan truoc.
        val lan2 = LamTrenMay.ghi(
            context, sau.first(), luot(v), "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0 + 25 * gio
        )
        assertEquals(v, lan2.sao)
        assertEquals(1, lan2.phutCap)

        // Du sao roi thi thoi.
        assertTrue(LamTrenMay.cauLamThem(context, "Toán", bayGio = t0 + 60 * gio).none { it.cau.id == m.cau.id })
    }

    @Test
    fun cau_tung_mat_sao_den_hen_on_la_vong_sao_moi() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first()
        val v = m.ghep.sao
        val t0 = System.currentTimeMillis()
        LamTrenMay.ghi(context, m, luot(v, sai = 1), "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0, congNgay = false)

        // Hen dau tien la ba ngay sau lan lam dung.
        assertTrue(LamTrenMay.cauOn(context, bayGio = t0 + 2 * ngay).none { it.cau.id == m.cau.id })
        val hen = t0 + 3 * ngay + gio
        val on = LamTrenMay.cauOn(context, bayGio = hen)
        assertTrue(on.any { it.cau.id == m.cau.id })

        // On la vong moi: sao tinh lai tu dau, phut di theo tran on lai.
        val ghi = LamTrenMay.ghi(
            context, on.first { it.cau.id == m.cau.id }, luot(v), "x", LamTrenMay.Loai.ON, bayGio = hen
        )
        assertEquals(1, ghi.dong.vong)
        assertTrue(ghi.dong.onTap)
        assertEquals(v, ghi.sao)
        assertEquals(v, ghi.phutCap)
        assertEquals(v, SoCaiBai.phutOnHomNay(context, hen))

        // Da on xong hen dau: hen sau la 10 ngay.
        assertTrue(LamTrenMay.cauOn(context, bayGio = hen + gio).none { it.cau.id == m.cau.id })
    }

    /**
     * Dong "Lần trước" o man lam bai (Ba Huy chot 30/9/2026): sao TOT NHAT cua vong dang lam,
     * khong hien khi chua lam lan nao va khi on lai den hen (vong moi, duoc tron so sao).
     */
    @Test
    fun lan_truoc_la_sao_tot_nhat_cua_vong_va_khong_hien_khi_mo_vong_moi() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first { it.ghep.sao >= 3 }
        val v = m.ghep.sao
        val t0 = System.currentTimeMillis()
        assertEquals(null, LamTrenMay.saoLanTruoc(context, m, LamTrenMay.Loai.LAM_THEM, t0))

        // Luot dau mat mot sao, luot sau (24 gio sau) mat hai sao: moc van la luot dau.
        LamTrenMay.ghi(context, m, luot(v, sai = 1), "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0, congNgay = false)
        val lai = muc(m.cau.id)
        assertEquals(v - 1, LamTrenMay.saoLanTruoc(context, lai, LamTrenMay.Loai.LAM_THEM, t0 + 25 * gio))
        LamTrenMay.ghi(
            context, lai, luot(v, sai = 2), "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0 + 25 * gio, congNgay = false
        )
        assertEquals(v - 1, LamTrenMay.saoLanTruoc(context, muc(m.cau.id), LamTrenMay.Loai.LAM_THEM, t0 + 50 * gio))

        // Den hen on (3 ngay sau luot dung gan nhat): vong moi, khong hien moc cu.
        val hen = t0 + 25 * gio + 3 * ngay + gio
        assertTrue(LamTrenMay.cauOn(context, bayGio = hen).any { it.cau.id == m.cau.id })
        assertEquals(null, LamTrenMay.saoLanTruoc(context, muc(m.cau.id), LamTrenMay.Loai.ON, hen))

        // On mat mot sao, lam lai trong vong on sau 24 gio: hien moc cua vong on.
        LamTrenMay.ghi(context, muc(m.cau.id), luot(v, sai = 1), "x", LamTrenMay.Loai.ON, bayGio = hen, congNgay = false)
        assertEquals(v - 1, LamTrenMay.saoLanTruoc(context, muc(m.cau.id), LamTrenMay.Loai.ON, hen + 25 * gio))
    }

    @Test
    fun dung_ngay_tu_dau_thi_khong_vao_lich_on() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first()
        val t0 = System.currentTimeMillis()
        LamTrenMay.ghi(context, m, luot(m.ghep.sao), "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0, congNgay = false)
        assertTrue(LamTrenMay.cauOn(context, bayGio = t0 + 4 * ngay).none { it.cau.id == m.cau.id })
    }

    /** On lai hien het cau den hen (Ba Huy chot 2/10/2026), truoc do moi luot toi da 10 cau. */
    @Test
    fun on_lai_hien_het_cau_den_hen() {
        datMocToan(3)
        val cac = LamTrenMay.cauLamThem(context, "Toán").take(12)
        assertEquals("can du 12 cau de thu", 12, cac.size)
        val t0 = System.currentTimeMillis()
        // Moi cau sai mot lan roi dung: tung sai, ba ngay sau den hen.
        cac.forEach { LamTrenMay.ghi(context, it, luot(it.ghep.sao, sai = 1), "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0, congNgay = false) }

        val on = LamTrenMay.cauOn(context, bayGio = t0 + 3 * ngay + gio)

        assertTrue("thieu cau den hen", on.map { it.cau.id }.containsAll(cac.map { it.cau.id }))
    }

    /**
     * Cau on lai lam sai het (Ba Huy chot 2/10/2026): 24 gio sau moi gap lai, nhu Luyen tap. Truoc
     * do cau van den hen nen bam On lai lan nua la hien ngay voi du sao, con chep loi giai vua xem.
     */
    @Test
    fun on_lai_sai_het_thi_24_gio_sau_moi_gap_lai() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first()
        val v = m.ghep.sao
        val t0 = System.currentTimeMillis()
        LamTrenMay.ghi(context, m, luot(v, sai = 1), "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0, congNgay = false)
        val hen = t0 + 3 * ngay + gio
        val on = LamTrenMay.cauOn(context, bayGio = hen).first { it.cau.id == m.cau.id }
        var l = LuatGhep.Luot(v)
        repeat(v + 1) { l = LuatGhep.kiem(l, soSai = 1) }
        assertTrue(l.xong && l.hienLoiGiai)
        LamTrenMay.ghi(context, on, l, "sai", LamTrenMay.Loai.ON, bayGio = hen, congNgay = false)

        assertTrue("vua sai het da hien lai", LamTrenMay.cauOn(context, bayGio = hen + gio).none { it.cau.id == m.cau.id })
        assertEquals("dem cau cho on cung bo cau do", 0, LamTrenMay.soCauOn(context, hen + gio, "Toán"))
        val homSau = LamTrenMay.cauOn(context, bayGio = hen + 25 * gio).firstOrNull { it.cau.id == m.cau.id }
        assertTrue("qua 24 gio phai gap lai", homSau != null)
        val lai = LamTrenMay.ghi(context, homSau!!, luot(v), "x", LamTrenMay.Loai.ON, bayGio = hen + 25 * gio, congNgay = false)
        assertEquals("gap lai la vong sao moi, du sao", v, lai.sao)
    }

    // ------------------------------------------------------------------ phut va quy

    @Test
    fun vuot_tran_tren_may_thi_phan_vuot_vao_quy() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first { it.ghep.sao >= 2 }
        val v = m.ghep.sao
        val bayGio = System.currentTimeMillis()
        // Hom nay da cap gan du tran lam tren may: chi con mot phut.
        kho.ghiTraLoi(
            TraLoi(
                cauId = "thu:da-lam", mon = "Toán", ma = "da-lam", de = "", ketQua = "", dung = true,
                phut = LuatCongGio.TRAN_TREN_MAY - 1, nhanXet = "", luc = bayGio, trenMay = true,
                sao = 1, saoToiDa = 1
            )
        )
        val quyTruoc = QuyGio.so(context)

        val ghi = LamTrenMay.ghi(context, m, luot(v), "x", LamTrenMay.Loai.LAM_THEM, bayGio = bayGio)

        assertEquals(1, ghi.phutCap)
        assertEquals(v - 1, ghi.phutQuy)
        assertEquals(quyTruoc + v - 1, QuyGio.so(context))
        assertEquals(LuatCongGio.TRAN_TREN_MAY, SoCaiBai.phutTrenMayHomNay(context, bayGio))
    }

    @Test
    fun gio_ngu_thi_ghi_so_va_giu_phut_toi_sang() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first()
        datGioNgu(dangNgu = true)

        val ghi = LamTrenMay.ghi(context, m, luot(m.ghep.sao), "x", LamTrenMay.Loai.LAM_THEM)

        assertEquals(m.ghep.sao, ghi.phutCap)
        assertEquals(ghi.phutCap, ghi.phutGiu)
        assertEquals(listOf(ghi.phutCap), CongSang.cacMuc(context).map { it.phut })
        // So ghi ngay: cau da xong, khong con la cau moi.
        assertFalse(LamTrenMay.cauLamThem(context, "Toán").any { it.cau.id == m.cau.id })
    }

    @Test
    fun het_sao_thi_ghi_0_phut_va_mo_lai_sau_24_gio() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first()
        var l = LuatGhep.Luot(m.ghep.sao)
        repeat(m.ghep.sao) { l = LuatGhep.kiem(l, soSai = 1) }
        l = LuatGhep.kiem(l, soSai = 1)
        assertTrue(l.xong && l.hienLoiGiai)
        val t0 = System.currentTimeMillis()

        val ghi = LamTrenMay.ghi(context, m, l, "x", LamTrenMay.Loai.LAM_THEM, bayGio = t0)

        assertEquals(0, ghi.sao)
        assertEquals(0, ghi.phutCap)
        assertFalse(ghi.dong.dung)
        assertEquals(m.cau.id, LamTrenMay.cauLamThem(context, "Toán", bayGio = t0 + 25 * gio).first().cau.id)
    }

    @Test
    fun nhat_ky_mot_dong_cho_ca_luot() {
        datMocToan(3)
        val ds = LamTrenMay.cauLamThem(context, "Toán").take(2)
        val cac = ds.map { LamTrenMay.ghi(context, it, luot(it.ghep.sao), "x", LamTrenMay.Loai.LAM_THEM, congNgay = false) }
        val sao = ds.sumOf { it.ghep.sao }

        LamTrenMay.ghiNhatKy(context, "Toán", LamTrenMay.Loai.LAM_THEM, cac)

        assertTrue(DayLog.today(context).contains("Luyện tập Toán: 2 câu, $sao/$sao sao, +0 phút"))
    }

    // ----------------------------- bo qua (nut "Bài này làm sau", truoc 2/10/2026 ten "Câu tiếp")

    @Test
    fun cau_bo_qua_coi_nhu_chua_lam_lui_ra_sau_mot_ngay_roi_ve_cho_cu() {
        datMocToan(3)
        val t0 = System.currentTimeMillis()
        val dau = LamTrenMay.cauLamThem(context, "Toán", bayGio = t0)
        assertTrue("can it nhat hai cau de thu", dau.size >= 2)
        val kho1 = dau.first().cau.id

        CauBoQua.ghi(context, kho1, luc = t0)

        val sau1Gio = LamTrenMay.cauLamThem(context, "Toán", bayGio = t0 + gio)
        assertEquals("cau ke tiep len dau", dau[1].cau.id, sau1Gio.first().cau.id)
        val vt = sau1Gio.indexOfFirst { it.cau.id == kho1 }
        assertTrue("cau bo qua xep cuoi hay ra khoi luot, dang o $vt", vt == -1 || vt == sau1Gio.lastIndex)
        assertTrue("khong ghi so cai", kho.cacLuotTrenMay(kho1).isEmpty())
        assertTrue("van la cau chua lam", LamTrenMay.muc(context, dau.first().cau)!!.tinhTrang.vong < 0)

        val homSau = LamTrenMay.cauLamThem(context, "Toán", bayGio = t0 + 25 * gio)
        assertEquals("qua mot ngay ve cho cu", kho1, homSau.first().cau.id)
    }

    @Test
    fun luot_it_cau_van_giu_cau_bo_qua_o_cuoi() {
        datMocToan(3)
        val t0 = System.currentTimeMillis()
        val hai = LamTrenMay.cauLamThem(context, "Toán", gioiHan = 2, bayGio = t0)
        CauBoQua.ghi(context, hai.first().cau.id, luc = t0)
        val tatCa = LamTrenMay.cauLamThem(context, "Toán", gioiHan = 500, bayGio = t0 + gio)
        assertEquals(hai.first().cau.id, tatCa.last().cau.id)
    }

    @Test
    fun lam_xong_cau_da_bo_qua_thi_thoi_lui() {
        datMocToan(3)
        val m = LamTrenMay.cauLamThem(context, "Toán").first()
        CauBoQua.ghi(context, m.cau.id)
        assertTrue(m.cau.id in CauBoQua.vuaBoQua(context))
        LamTrenMay.ghi(context, m, luot(m.ghep.sao), "x", LamTrenMay.Loai.LAM_THEM, congNgay = false)
        assertFalse(m.cau.id in CauBoQua.vuaBoQua(context))
    }

    @Test
    fun nhat_ky_ke_cau_bo_qua() {
        datMocToan(3)
        val ds = LamTrenMay.cauLamThem(context, "Toán").take(3)
        val cac = listOf(LamTrenMay.ghi(context, ds[0], luot(ds[0].ghep.sao), "x", LamTrenMay.Loai.LAM_THEM, congNgay = false))

        LamTrenMay.ghiNhatKy(context, "Toán", LamTrenMay.Loai.LAM_THEM, cac, listOf(ds[1].cau, ds[2].cau))
        assertTrue(DayLog.today(context).contains("bỏ qua 2 câu: ${ds[1].cau.ma}, ${ds[2].cau.ma}"))

        LamTrenMay.ghiNhatKy(context, "Toán", LamTrenMay.Loai.LAM_THEM, emptyList(), listOf(ds[1].cau))
        assertTrue("chi bo qua cung ghi", DayLog.today(context).contains("Luyện tập Toán: bỏ qua 1 câu: ${ds[1].cau.ma}"))
    }

    /**
     * Man xem lai cau da lam dung (2/10/2026): moi cau mot dong voi luot dung gan nhat, cau het sao
     * ma van sai thi khong vao.
     */
    @Test
    fun cau_da_lam_dung_moi_cau_mot_dong_luot_gan_nhat() {
        datMocToan(3)
        val cac = LamTrenMay.cauLamThem(context, "Toán")
        val m = cac[0]
        val sai = cac[1]
        val t0 = System.currentTimeMillis()
        LamTrenMay.ghi(context, m, luot(m.ghep.sao, sai = 1), "lần một", LamTrenMay.Loai.LAM_THEM, bayGio = t0, congNgay = false)
        LamTrenMay.ghi(context, m, luot(m.ghep.sao), "lần hai", LamTrenMay.Loai.LAM_THEM, bayGio = t0 + 25 * gio, congNgay = false)
        var l = LuatGhep.Luot(sai.ghep.sao)
        repeat(sai.ghep.sao + 1) { l = LuatGhep.kiem(l, soSai = 1) }
        assertTrue(l.xong && l.hienLoiGiai)
        LamTrenMay.ghi(context, sai, l, "sai", LamTrenMay.Loai.LAM_THEM, bayGio = t0, congNgay = false)

        val dung = LamTrenMay.cauDaLamDung(context, "Toán")
        assertEquals(listOf(m.cau.id), dung.map { it.cau.id })
        assertEquals("lần hai", dung.single().luot.ketQua)
        assertEquals(0, dung.single().luot.lanSai)
        assertEquals(1, LamTrenMay.soCauDaLamDung(context, "Toán"))
        assertTrue("mon khac khong lan vao", LamTrenMay.cauDaLamDung(context, "Khoa học tự nhiên").none { it.cau.id == m.cau.id })
    }
}
