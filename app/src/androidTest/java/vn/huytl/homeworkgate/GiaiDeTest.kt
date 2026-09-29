package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LichKiemTra
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.data.LoaiLoi
import vn.huytl.homeworkgate.kho.TraLoi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Giai de va duong sach bai tap: moc hoc toi theo phan, lam them trong bai da hoc, ra de
 * tuan, doc lich kiem tra, cham trac nghiem tren may. Xem [GiaiDe], [PhanHoc], [LichKiemTra].
 *
 * CAN THAN: bo test nay ghi moc hoc toi, vo dan do va de vao kho that tren may, roi tra
 * lai moc va vo cu, xoa de va cac lan lam no tao ra. Khong goi mang, khong goi AI.
 */
@RunWith(AndroidJUnit4::class)
class GiaiDeTest {

    private lateinit var context: Context
    private lateinit var kho: KhoBai

    private val cacPhan = listOf("toan8ct", "khtn8hoa", "khtn8li", "khtn8sinh")
    private val mocCu = mutableMapOf<String, String?>()
    private var voCu: VoDanDo.DanDo? = null
    private var anhCu: java.io.File? = null
    private val deDaTao = mutableListOf<String>()
    private val cauDaGhi = mutableListOf<String>()

    /** Sang thu Bay 3/10/2026, 8 gio: sau gio mo de tuan. */
    private val sangThuBay = ms(LocalDateTime.of(2026, 10, 3, 8, 0))

    private fun ms(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        BoThe.napNeuCan(context)
        kho = KhoBai.get(context)
        cacPhan.forEach { mocCu[it] = HocToi.baiCua(context, it) }
        voCu = VoDanDo.doc(context)
        // VoDanDo.xoa xoa ca tam anh vo: chep ra truoc de tra lai, khong thi test xong vo
        // tren may mat anh (doc lai, gui ba deu hong).
        anhCu = voCu?.anh?.let { java.io.File(it) }?.takeIf { it.exists() }?.let { f ->
            java.io.File(context.cacheDir, "giai_de_test_vo.jpg").also { f.copyTo(it, overwrite = true) }
        }
        // Ban vo that tren may co the dang bao sap kiem tra: bo di cho de tuan khong lan.
        VoDanDo.xoa(context)
    }

    @After
    fun tearDown() {
        deDaTao.forEach { kho.xoaDe(it) }
        cauDaGhi.forEach { kho.writableDatabase.delete("tra_loi", "cau_id = ?", arrayOf(it)) }
        cacPhan.forEach { ma ->
            HocToi.xoa(context, ma)
            mocCu[ma]?.let { HocToi.ghiBai(context, ma, it) }
        }
        val cu = voCu
        if (cu != null) VoDanDo.luu(context, cu) else VoDanDo.xoa(context)
        anhCu?.let { tam ->
            cu?.anh?.let { tam.copyTo(java.io.File(it), overwrite = true) }
            tam.delete()
        }
    }

    private fun datMoc(ma: String, soBai: Int?) {
        HocToi.xoa(context, ma)
        if (soBai == null) return
        val phan = PhanHoc.theoMa(ma)!!
        val ten = if (soBai == 0) HocToi.CHUA_HOC_BAI_NAO
        else PhanHoc.cacBai(context, phan).first { PhanHoc.soBai(it) == soBai }
        HocToi.ghiBai(context, ma, ten)
    }

    private fun taoDe(bayGio: Long): List<DeGiai> =
        GiaiDe.taoNeuCan(context, bayGio).also { moi -> deDaTao += moi.map { it.id } }

    // ------------------------------------------------------------- moc hoc toi

    @Test
    fun hop_chon_khtn_co_du_bai_ca_bai_khong_co_the() {
        val hoa = PhanHoc.cacBai(context, PhanHoc.theoMa("khtn8hoa")!!)
        assertEquals((1..12).toList(), hoa.map { PhanHoc.soBai(it) })
        assertEquals((30..47).toList(), PhanHoc.cacBai(context, PhanHoc.theoMa("khtn8sinh")!!).map { PhanHoc.soBai(it) })
        assertEquals(39, PhanHoc.cacBai(context, PhanHoc.theoMa("toan8ct")!!).size)
    }

    @Test
    fun chon_bai_khong_co_the_thi_bo_the_dung_o_bai_truoc() {
        // Bo the Hoa khong co Bai 7: chon Bai 7 thi bo the dung o the cuoi cua Bai 6.
        datMoc("khtn8hoa", 7)
        val bai6 = kho.cacBaiTrongBoThe("khtn8hoa").first { PhanHoc.soBai(it) == 6 }
        assertEquals(kho.thuTuCuoiCua("khtn8hoa", bai6), BoThe.denThuTu(context, "khtn8hoa"))
        // Lop chua toi the dau tien cua bo (Bai 3) thi khong the nao.
        datMoc("khtn8hoa", 2)
        assertEquals(-1, BoThe.denThuTu(context, "khtn8hoa"))
    }

    @Test
    fun bai_da_hoc_gop_cac_phan() {
        datMoc("khtn8hoa", 6)
        datMoc("khtn8li", 0)
        datMoc("khtn8sinh", null)
        // Con mot phan chua chon thi lam them phai hoi truoc.
        assertNull(PhanHoc.baiDaHoc(context, "Khoa học tự nhiên"))
        // De tu mo thi bo qua phan chua chon.
        assertEquals((1..6).toSet(), PhanHoc.baiDaHoc(context, "Khoa học tự nhiên", chiPhanDaChon = true))
        datMoc("khtn8sinh", 31)
        assertEquals((1..6).toSet() + setOf(30, 31), PhanHoc.baiDaHoc(context, "Khoa học tự nhiên"))
    }

    // --------------------------------------------------------------- lam them

    @Test
    fun lam_them_chi_lay_sbt_trong_bai_da_hoc_bai_gan_moc_truoc() {
        datMoc("toan8ct", 3)
        val cau = NganHang.cauNenLamThemCuaMon(context, "Toán", gioiHan = 12)
        assertTrue(cau.isNotEmpty())
        assertTrue("co cau SGK", cau.all { it.nguon == "sbttoan8t1" })
        assertTrue("co bai chua hoc", cau.all { (PhanHoc.soBai(it.bai) ?: 99) <= 3 })
        assertTrue("co muc on tap chuong", cau.none { PhanHoc.soBai(it.bai) == null })
        // Bai gan moc nhat truoc: Bai 3, roi Bai 2.
        assertEquals(3, PhanHoc.soBai(cau.first().bai))
        assertTrue(cau.count { PhanHoc.soBai(it.bai) == 3 } <= NganHang.MOI_BAI_LAM_THEM)
    }

    @Test
    fun lam_them_chua_chon_moc_thi_rong() {
        datMoc("toan8ct", null)
        assertTrue(NganHang.cauNenLamThemCuaMon(context, "Toán").isEmpty())
    }

    @Test
    fun luyen_cho_hay_vap_rut_cau_sbt_cung_ten_bai() {
        datMoc("toan8ct", 9)
        val bai6 = NganHang.cacBai(context, "toan8t1").first { PhanHoc.soBai(it.bai) == 6 }.bai
        val sgk = NganHang.cacCau(context, "toan8t1", bai6).first()
        cauDaGhi += sgk.id
        repeat(3) {
            kho.ghiTraLoi(
                TraLoi(
                    cauId = sgk.id, mon = "Toán", ma = sgk.ma, de = sgk.de, ketQua = "",
                    dung = false, phut = 0, nhanXet = "", luc = System.currentTimeMillis() - 60_000L,
                    loaiLoi = LoaiLoi.SAI_DAU
                )
            )
        }
        val cau = kho.cacCauLuyenTheoLoi(LoaiLoi.SAI_DAU)
        assertTrue(cau.isNotEmpty())
        assertTrue(cau.all { it.nguon == "sbttoan8t1" && it.bai == bai6 })
        assertTrue(cau.none { it.dang == "TRAC_NGHIEM" })
    }

    // ------------------------------------------------------------------ ra de

    @Test
    fun sang_thu_bay_ra_de_tuan_moi_mon_mot_lan() {
        datMoc("toan8ct", 6)
        datMoc("khtn8hoa", 6)
        datMoc("khtn8li", 0)
        datMoc("khtn8sinh", null)

        val moi = taoDe(sangThuBay).filter { it.loai == GiaiDe.LOAI_TUAN }
        assertEquals(setOf("Toán", "Khoa học tự nhiên"), moi.map { it.mon }.toSet())
        // Chay lai thi khong sinh them de nao.
        assertTrue(taoDe(sangThuBay + 60_000L).none { it.loai == GiaiDe.LOAI_TUAN })

        val hetHan = ms(LocalDateTime.of(2026, 10, 10, GiaiDe.GIO_MO_DE_TUAN, 0))
        moi.forEach { de ->
            assertEquals(hetHan, de.hetHan)
            val cac = GiaiDe.cacCau(context, de)
            assertEquals(de.cauIds.size, cac.size)
            assertEquals("mot de mot quyen", 1, cac.map { it.nguon }.toSet().size)
            assertTrue("thieu tu luan", cac.any { !it.bamTrenMay })
            assertTrue("cau khong co dap an", cac.all { it.dapAn.isNotBlank() })
            assertTrue("trac nghiem khac kieu", cac.none { it.dang == "TRAC_NGHIEM" && !it.bamTrenMay })
            assertTrue(de.phutGoiY in 10..45)
        }

        // Toan: lop vua hoc xong chuong I (Bai 1-5) nen de tuan la muc on tap chuong I.
        val toan = moi.first { it.mon == "Toán" }
        assertEquals("Ôn tập chương I", toan.ten)
        assertTrue(GiaiDe.cacCau(context, toan).count { it.bamTrenMay } in 1..4)

        // KHTN: chi phan Hoa da hoc, cac bai gan moc (4, 5, 6).
        val khtn = moi.first { it.mon == "Khoa học tự nhiên" }
        val soBai = GiaiDe.cacCau(context, khtn).map { PhanHoc.soBai(it.bai) }.toSet()
        assertTrue("bai ngoai moc: $soBai", soBai.all { it != null && it in 4..6 })
        assertTrue(GiaiDe.cacCau(context, khtn).count { it.bamTrenMay } in 1..8)

        // Hai de khong chung cau nao.
        assertTrue(toan.cauIds.intersect(khtn.cauIds.toSet()).isEmpty())
    }

    @Test
    fun chua_chon_moc_thi_khong_co_de() {
        cacPhan.forEach { datMoc(it, null) }
        assertTrue(taoDe(sangThuBay).none { it.loai == GiaiDe.LOAI_TUAN })
    }

    @Test
    fun moc_tuan_la_sang_thu_bay() {
        val thuSau = ms(LocalDateTime.of(2026, 10, 2, 20, 0))
        assertEquals(LocalDateTime.of(2026, 9, 26, 6, 0), GiaiDe.mocTuan(thuSau))
        assertEquals(LocalDateTime.of(2026, 10, 3, 6, 0), GiaiDe.mocTuan(sangThuBay))
        // Thu Bay truoc sau gio van la tuan truoc.
        assertEquals(LocalDateTime.of(2026, 9, 26, 6, 0), GiaiDe.mocTuan(ms(LocalDateTime.of(2026, 10, 3, 5, 0))))
    }

    @Test
    fun vo_dan_do_bao_kiem_tra_thi_mo_de_on_dung_bai() {
        datMoc("toan8ct", 9)
        val homNay = LocalDate.now()
        VoDanDo.luu(
            context,
            VoDanDo.DanDo(
                ngay = homNay.toString(),
                cacDong = listOf(
                    VoDanDo.Dong("Toán: làm bài 2.28 trang 47", laBaiTap = true),
                    VoDanDo.Dong("Toán: tiết sau kiểm tra 15 phút bài 7, 8", laBaiTap = false)
                )
            )
        )
        val kt = taoDe(System.currentTimeMillis()).filter { it.loai == GiaiDe.LOAI_KIEM_TRA }
        assertEquals(1, kt.size)
        val de = kt.first()
        assertEquals("Toán", de.mon)
        assertTrue(de.ghiChu.contains("kiểm tra 15 phút"))
        val soBai = GiaiDe.cacCau(context, de).map { PhanHoc.soBai(it.bai) }.toSet()
        assertTrue("bai ngoai dong dan: $soBai", soBai.all { it == 7 || it == 8 })
        // Ngay kiem tra la buoi Toan ke tiep, de het han cuoi ngay do.
        val ngay = LocalDate.parse(de.ngayKiemTra)
        assertTrue(ngay.isAfter(homNay))
        assertEquals(ms(ngay.plusDays(1).atStartOfDay()), de.hetHan)
        assertTrue(GiaiDe.tenDe(de).startsWith("Ôn kiểm tra"))
    }

    // ------------------------------------------------------------ cham de

    /** Mot de dung tay, khong qua luat ra de, de biet chac cau nao trac nghiem. */
    private fun deThu(): DeGiai {
        val sbt = kho.cacCauCuaNguon("sbtkhtn8")
        val tn = sbt.filter { it.bamTrenMay && PhanHoc.soBai(it.bai) == 6 }.take(4)
        val tl = sbt.filter { !it.bamTrenMay && it.dang != "TRAC_NGHIEM" && PhanHoc.soBai(it.bai) == 6 }.take(2)
        val bayGio = System.currentTimeMillis()
        val de = DeGiai(
            id = "de-thu-" + bayGio, mon = "Khoa học tự nhiên", nguon = "sbtkhtn8",
            loai = GiaiDe.LOAI_TUAN, khoa = "thu:$bayGio", ten = "Bài 6",
            cauIds = (tn + tl).map { it.id }, phutGoiY = 20, taoLuc = bayGio,
            hetHan = bayGio + 3 * 24 * 60 * 60_000L
        )
        assertTrue(kho.themDe(de))
        deDaTao += de.id
        return de
    }

    @Test
    fun nop_trac_nghiem_cham_bang_dap_an_ghi_so_cap_gio() {
        var de = deThu()
        val tn = GiaiDe.cacCau(context, de).filter { it.bamTrenMay }
        de = GiaiDe.batDau(context, de, System.currentTimeMillis() - 10 * 60_000L)
        // Hai cau chon dung, mot cau chon sai, mot cau bo trong.
        de = GiaiDe.chon(context, de, tn[0].id, tn[0].dapAn)
        de = GiaiDe.chon(context, de, tn[1].id, tn[1].dapAn)
        val sai = listOf("A", "B", "C", "D").first { it != tn[2].dapAn }
        de = GiaiDe.chon(context, de, tn[2].id, sai)
        assertTrue(GiaiDe.dangMo(context).any { it.id == de.id })

        var capDuoc = 0
        val kq = GiaiDe.nopTracNghiem(context, de) { phut -> capDuoc = phut; true }
        assertEquals(2, kq.dung)
        assertEquals(4, kq.tong)
        assertEquals(2 * LuatCongGio.PHUT_MOI_CAU_TRAC_NGHIEM, kq.phut)
        assertEquals(kq.phut, capDuoc)
        assertFalse(kq.khongCapDuoc)

        val dong = tn.map { c -> kho.lichSuCua(c.id).firstOrNull { it.deId == de.id } }
        assertTrue("thieu dong so", dong.all { it != null })
        assertEquals(listOf(true, true, false, false), dong.map { it!!.dung })
        assertEquals(kq.phut, dong.sumOf { it!!.phut })
        assertEquals("Chưa chọn", dong[3]!!.baiLam.single())

        // Nop roi thi con phan tu luan: de van mo cho toi khi gui.
        assertEquals(2, kq.de.tnDung)
        assertTrue(GiaiDe.dangMo(context).any { it.id == de.id })
        GiaiDe.daGuiTuLuan(context, de.id)
        assertTrue(GiaiDe.dangMo(context).none { it.id == de.id })
        assertFalse(GiaiDe.daCoDiem(context, GiaiDe.theoId(context, de.id)!!))
    }

    @Test
    fun khong_cap_duoc_gio_thi_cau_dung_chua_ghi_so() {
        var de = deThu()
        val tn = GiaiDe.cacCau(context, de).filter { it.bamTrenMay }
        de = GiaiDe.batDau(context, de)
        de = GiaiDe.chon(context, de, tn[0].id, tn[0].dapAn)
        val kq = GiaiDe.nopTracNghiem(context, de) { false }
        assertEquals(1, kq.dung)
        assertEquals(0, kq.phut)
        assertTrue(kq.khongCapDuoc)
        // Cau dung chua vao so, ngay khac lam lai van duoc tinh; cau sai thi co dong de sua.
        assertNull(kho.lichSuCua(tn[0].id).firstOrNull { it.deId == de.id })
        assertNotNull(kho.lichSuCua(tn[1].id).firstOrNull { it.deId == de.id })
        // Diem van ghi.
        assertEquals(1, GiaiDe.theoId(context, de.id)!!.tnDung)
    }

    @Test
    fun cham_xong_tu_luan_thi_de_co_diem() {
        var de = deThu()
        val cac = GiaiDe.cacCau(context, de)
        val tn = cac.filter { it.bamTrenMay }
        val tl = cac.filterNot { it.bamTrenMay }
        de = GiaiDe.batDau(context, de, System.currentTimeMillis() - 25 * 60_000L)
        tn.forEach { de = GiaiDe.chon(context, de, it.id, it.dapAn) }
        GiaiDe.nopTracNghiem(context, de) { true }

        val daCham = tl.mapIndexed { i, c ->
            CauCham(ma = c.ma, de = c.de, dung = i == 0, dang = DangBai.CAU_NHO, cauId = c.id)
        }
        val tomTat = GiaiDe.nhanTuLuan(context, de.id, daCham)
        val sau = GiaiDe.theoId(context, de.id)!!
        assertEquals(1, sau.tlDung)
        assertEquals(mapOf(tl[0].id to true, tl[1].id to false), sau.ketTuLuan)
        assertTrue(GiaiDe.daCoDiem(context, sau))
        assertEquals(tn.size + 1 to cac.size, GiaiDe.diem(context, sau))
        assertTrue(tomTat.orEmpty(), tomTat.orEmpty().contains("đúng ${tn.size + 1}/${cac.size} câu"))
        assertTrue(tomTat.orEmpty(), tomTat.orEmpty().contains("quá"))
        assertTrue(GiaiDe.xongHomNay(context).any { it.id == de.id })
    }

    // -------------------------------------------------------- lich kiem tra

    /** Thu Hai 28/9/2026: buoi Toan ke tiep la thu Tu, buoi KHTN ke tiep la thu Nam. */
    private val thuHai = LocalDate.of(2026, 9, 28)

    private fun doc(chu: String) = LichKiemTra.doc(chu, thuHai)

    @Test
    fun doc_dong_dan_do_kiem_tra() {
        val kt = doc("Toán: tiết sau kiểm tra 15 phút bài 2, 3").single()
        assertEquals(LichKiemTra.TOAN, kt.mon)
        assertEquals(listOf(2, 3), kt.cacBai)
        assertEquals(LocalDate.of(2026, 9, 30), kt.ngay)

        val hoa = doc("KHTN: tiết sau kiểm tra bài 8 đến bài 10").single()
        assertEquals(LichKiemTra.KHTN, hoa.mon)
        assertEquals(listOf(8, 9, 10), hoa.cacBai)
        assertEquals(LocalDate.of(2026, 10, 1), hoa.ngay)

        assertEquals(listOf(12), doc("Hoá - ktra 15p bài 12").single().cacBai)
        assertEquals(2, doc("Toán: kiểm tra chương II vào thứ 4").single().chuong)
        assertEquals(LocalDate.of(2026, 9, 30), doc("Toán: kiểm tra chương II vào thứ 4").single().ngay)
    }

    @Test
    fun doc_tin_cua_co() {
        val kt = doc("Mai lớp kiểm tra 15 phút môn Toán.").single()
        assertEquals(LichKiemTra.TOAN, kt.mon)
        assertEquals(thuHai.plusDays(1), kt.ngay)
        assertTrue(kt.cacBai.isEmpty())

        val hai = doc("Ngày 5/10 kiểm tra Toán và KHTN")
        assertEquals(setOf(LichKiemTra.TOAN, LichKiemTra.KHTN), hai.map { it.mon }.toSet())
        assertTrue(hai.all { it.ngay == LocalDate.of(2026, 10, 5) })
    }

    @Test
    fun khong_nham_dong_khong_phai_kiem_tra_toan_khtn() {
        // Truoc 29/9/2026 dong nay ra rong vi chua co sach bai tap Tieng Anh. Tu hom do
        // Tieng Anh la mon thu ba: xem doc_kiem_tra_tieng_anh_theo_unit.
        assertTrue(doc("Toán: làm bài 2.26 trang 36").isEmpty())
        assertTrue(doc("NV: học thuộc bài thơ thì mới làm được").isEmpty())
        // "học sinh", "sinh hoạt" khong phai mon Sinh.
        assertTrue(doc("Học sinh mặc áo trắng, sinh hoạt lớp, kiểm tra vở").isEmpty())
        // "thì" bo dau thanh "thi" nhung khong phai thi cu.
        assertTrue(doc("Toán: làm xong bài thì chụp gửi cô").isEmpty())
        // So cau bai tap SGK "2.26" khong phai so bai.
        assertEquals(emptyList<Int>(), doc("Toán: kiểm tra bài 2.26 đầu giờ").single().cacBai)
    }

    @Test
    fun doc_so_bai_hai_chu_so_va_ngay_tuan_sau() {
        // So cau bai tap hai chu so ("10.5", "25.1") khong phai so bai.
        assertEquals(emptyList<Int>(), doc("Toán: tiết sau kiểm tra, ôn bài 10.5 và 10.6").single().cacBai)
        assertEquals(emptyList<Int>(), doc("KHTN: tiết sau kiểm tra, ôn bài 25.1 đến 25.4").single().cacBai)
        // "thứ 5 tuần sau" la thu Nam cua tuan sau, khong phai thu Nam tuan nay.
        assertEquals(LocalDate.of(2026, 10, 8), doc("Toán: thứ 5 tuần sau kiểm tra 1 tiết").single().ngay)
        // Ten co giao khong phai "mai".
        assertEquals(thuHai.plusDays(7), doc("Toán: cô Mai dặn tuần sau kiểm tra chương II").single().ngay)
        // Ngay viet bang chu.
        assertEquals(
            LocalDate.of(2026, 10, 5),
            doc("Ngày 5 tháng 10 lớp kiểm tra 1 tiết Toán chương II.").single().ngay
        )
        // Ngay cua tin khong phai ngay kiem tra.
        assertEquals(
            LocalDate.of(2026, 9, 30),
            doc("Thông báo ngày 28/9: thứ 4 kiểm tra 15 phút môn Toán.").single().ngay
        )
        // Moi mon mot ngay.
        val hai = doc("Kiểm tra: Toán ngày 5/11, KHTN ngày 7/11").associateBy { it.mon }
        assertEquals(LocalDate.of(2026, 11, 5), hai.getValue(LichKiemTra.TOAN).ngay)
        assertEquals(LocalDate.of(2026, 11, 7), hai.getValue(LichKiemTra.KHTN).ngay)
    }

    // ---------------------------------------------- lich kiem tra Tieng Anh (29/9/2026)

    /**
     * Lop kiem tra Tieng Anh theo Unit. So Unit vao [LichKiemTra.KiemTra.cacBai] y nhu so
     * bai cua Toan, de Giai de lay dung cac Unit do. Tieng Anh khong co chuong.
     */
    @Test
    fun doc_kiem_tra_tieng_anh_theo_unit() {
        // Khong ghi mon, chi co "Unit": van la Tieng Anh.
        val kt = doc("kiểm tra 15 phút Unit 2").single()
        assertEquals(LichKiemTra.TIENG_ANH, kt.mon)
        assertEquals(listOf(2), kt.cacBai)
        assertNull(kt.chuong)
        assertNull(kt.ngay)

        // "Tiếng Anh:" dau dong, Unit viet kieu khoang. Tiet sau la buoi Tieng Anh ke tiep
        // theo thoi khoa bieu: tu thu Hai 28/9 la thu Bay 3/10.
        val khoang = doc("Tiếng Anh: tiết sau kiểm tra unit 1-3").single()
        assertEquals(LichKiemTra.TIENG_ANH, khoang.mon)
        assertEquals(listOf(1, 2, 3), khoang.cacBai)
        assertEquals(LocalDate.of(2026, 10, 3), khoang.ngay)

        // "Anh văn" dau dong va giua cau.
        assertEquals(listOf(4), doc("Anh văn: kiểm tra 15 phút unit 4").single().cacBai)
        val giua = doc("Mai kiểm tra Anh văn unit 3").single()
        assertEquals(LichKiemTra.TIENG_ANH, giua.mon)
        assertEquals(thuHai.plusDays(1), giua.ngay)

        // "English", ngay theo thu trong tuan.
        val eng = doc("Kiểm tra English unit 5 vào thứ 6").single()
        assertEquals(LichKiemTra.TIENG_ANH, eng.mon)
        assertEquals(listOf(5), eng.cacBai)
        assertEquals(LocalDate.of(2026, 10, 2), eng.ngay)

        // Kiem tra tu vung khong noi Unit: van la mot lan kiem tra Tieng Anh, khong Unit nao.
        val tuVung = doc("Tiếng Anh: tiết sau kiểm tra từ vựng").single()
        assertEquals(LichKiemTra.TIENG_ANH, tuVung.mon)
        assertTrue(tuVung.cacBai.isEmpty())

        // Co giao ghi "bài" cho Tieng Anh thi van hieu la Unit.
        assertEquals(listOf(6), doc("Tiếng Anh: kiểm tra bài 6").single().cacBai)
    }

    @Test
    fun anh_trai_khong_phai_mon_tieng_anh() {
        // "anh" dung mot minh la anh trai, khong phai mon.
        assertTrue(doc("Hôm nay anh trai kiểm tra bài cho em").isEmpty())
        val toan = doc("Toán: anh trai kiểm tra giúp bài 3").single()
        assertEquals(LichKiemTra.TOAN, toan.mon)
        assertEquals(listOf(3), toan.cacBai)
        // Nhac ca Toan va Tieng Anh thi ra hai lan.
        assertEquals(
            setOf(LichKiemTra.TOAN, LichKiemTra.TIENG_ANH),
            doc("Ngày 5/10 kiểm tra Toán và tiếng Anh").map { it.mon }.toSet()
        )
    }

    @Test
    fun doc_so_unit_theo_nhieu_cach_viet() {
        // Ham nhan chu da bo dau, viet thuong, nhu [LichKiemTra.doc] dua vao.
        assertEquals(listOf(2, 3), LichKiemTra.cacUnit("kiem tra unit 2, 3"))
        assertEquals(listOf(1, 2, 3), LichKiemTra.cacUnit("unit 1 den unit 3"))
        assertEquals(listOf(1, 2, 3), LichKiemTra.cacUnit("unit 1-3"))
        assertEquals(listOf(2, 5), LichKiemTra.cacUnit("unit 2 va unit 5"))
        // Sach chi co 12 Unit.
        assertTrue(LichKiemTra.cacUnit("unit 13").isEmpty())
        // "unit" nam trong mot tu khac thi khong phai.
        assertTrue(LichKiemTra.cacUnit("community 3").isEmpty())
    }

    @Test
    fun tach_phuong_an_khi_trong_phuong_an_co_chu_cai() {
        // Cau 18.2 SBT KHTN: phuong an B la "Vị trí C.".
        assertEquals(
            "Hỏi?\n\nA. Vị trí O.\nB. Vị trí C.\nC. Vị trí A.\nD. Vị trí B.",
            vn.huytl.homeworkgate.ui.GiaiDeActivity.tachPhuongAn("Hỏi? A. Vị trí O. B. Vị trí C. C. Vị trí A. D. Vị trí B.")
        )
        assertEquals(
            "Khối lượng là\n\nA. 10 g.\nB. 3 g.\nC. 0,9 g.\nD. 0,1 g.",
            vn.huytl.homeworkgate.ui.GiaiDeActivity.tachPhuongAn("Khối lượng là A. 10 g. B. 3 g. C. 0,9 g. D. 0,1 g.")
        )
    }
}
