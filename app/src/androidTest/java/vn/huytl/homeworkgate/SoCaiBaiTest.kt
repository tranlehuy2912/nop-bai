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
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * So cai cac cau da nop: moi cau chi tra gio mot lan, va nho cau nao dang cho sua.
 *
 * Day la thu duy nhat chan viec chup lai bai hom qua de lay gio lan nua - AI khong
 * nho gi giua hai lan goi, no se duyet that.
 *
 * CAN THAN: giong [GateStoreTest], bo test nay xoa sach prefs. Dung chay tren
 * tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class SoCaiBaiTest {

    private lateinit var context: Context
    private val now = System.currentTimeMillis()
    private val ngay = 24 * 60 * 60_000L

    private fun cau(ma: String, de: String, dung: Boolean, nhanXet: String = "") =
        CauCham(ma = ma, de = de, dung = dung, soDong = 4, nhanXet = nhanXet)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(context).raw().edit().clear().commit()
        // Tu ban co ngan hang cau hoi, so cai nam trong SQLite chu khong trong prefs
        // nua. Xoa prefs khong con don duoc no, va bo test se doc phai so cua lan
        // chay truoc - nhung loi kieu do chi hien ra o lan chay thu hai.
        SoCaiBai.xoaHet(context)
    }

    @Test
    fun cau_dung_thi_ghi_la_da_tra_gio() {
        val c = cau("2.26a", "x^2 - 6x + 9 - y^2", dung = true)
        SoCaiBai.ghi(context, listOf(c), mapOf("2.26a" to 2), now)

        assertTrue(SoCaiBai.daTraGio(context, "x^2 - 6x + 9 - y^2", now))
        assertEquals(2, SoCaiBai.phutDaCongHomNay(context, now))
        assertTrue(SoCaiBai.dangChoSua(context, now).isEmpty())
    }

    @Test
    fun dem_lan_sai_truoc_mot_moc_bo_lan_sai_cua_chinh_lan_nop_dang_cham_lai() {
        val sai = cau("2.33a", "Rút gọn (2x + 5y)^2 − (2x − 5y)^2", dung = false)
        SoCaiBai.ghi(context, listOf(sai), emptyMap(), now - ngay)
        SoCaiBai.ghi(context, listOf(sai), emptyMap(), now)

        assertEquals(2, SoCaiBai.soLanSai(context, sai, now))
        // Claude cham lai lan nop luc [now]: lan sai may ghi luc do khong tinh.
        assertEquals(1, SoCaiBai.soLanSai(context, sai, now, truocLuc = now))
        assertEquals(0, SoCaiBai.soLanSai(context, sai, now, truocLuc = now - ngay))
    }

    @Test
    fun chep_lai_dung_trang_hom_qua_thi_khong_duoc_tinh_lan_hai() {
        val c = cau("2.26a", "x^2 - 6x + 9 - y^2", dung = true)
        SoCaiBai.ghi(context, listOf(c), mapOf("2.26a" to 2), now - ngay)

        // Hom sau nop lai y het. Cach viet de hoi khac mot ti vi AI chep lai moi lan
        // mot khac - van phai nhan ra la mot bai.
        assertTrue(SoCaiBai.daTraGio(context, "x^2-6x+9-y^2", now))
        assertTrue(SoCaiBai.daTraGio(context, "X^2 - 6X + 9 - Y^2", now))
    }

    @Test
    fun cau_sai_thi_nam_cho_sua_va_chua_tra_gio() {
        val c = cau("2.26d", "x^2 - 4xy + 4y^2 + xz - 2yz", dung = false, nhanXet = "Dòng 3 sai dấu")
        SoCaiBai.ghi(context, listOf(c), emptyMap(), now)

        assertFalse(SoCaiBai.daTraGio(context, "x^2 - 4xy + 4y^2 + xz - 2yz", now))
        val cho = SoCaiBai.dangChoSua(context, now)
        assertEquals(1, cho.size)
        assertEquals("2.26d", cho.first().ma)
        assertEquals("Dòng 3 sai dấu", cho.first().nhanXet)
        assertEquals(0, SoCaiBai.phutDaCongHomNay(context, now))
    }

    @Test
    fun sua_dung_roi_nop_lai_thi_het_cho_sua_va_duoc_tra_gio() {
        val de = "x^2 - 4xy + 4y^2 + xz - 2yz"
        SoCaiBai.ghi(context, listOf(cau("2.26d", de, dung = false)), emptyMap(), now)
        assertEquals(1, SoCaiBai.dangChoSua(context, now).size)

        SoCaiBai.ghi(context, listOf(cau("2.26d", de, dung = true)), mapOf("2.26d" to 2), now)
        assertTrue(SoCaiBai.dangChoSua(context, now).isEmpty())
        assertTrue(SoCaiBai.daTraGio(context, de, now))
        assertEquals(2, SoCaiBai.phutDaCongHomNay(context, now))
    }

    @Test
    fun cau_da_tra_gio_thi_nop_lai_khong_cong_them_lan_nua() {
        val de = "xy + z^2 + xz + yz"
        SoCaiBai.ghi(context, listOf(cau("2.26c", de, dung = true)), mapOf("2.26c" to 2), now)
        // Nop lai chinh cau do, lan nay AI cham 10 phut. Van phai la 2.
        SoCaiBai.ghi(context, listOf(cau("2.26c", de, dung = true)), mapOf("2.26c" to 10), now)

        assertEquals(2, SoCaiBai.phutDaCongHomNay(context, now))
    }

    @Test
    fun cung_mot_trang_hom_qua_lam_bai_tren_hom_nay_lam_bai_duoi() {
        // Hom qua con lam hai bai dau cua trang.
        SoCaiBai.ghi(
            context,
            listOf(cau("2.26a", "x^2 - 6x + 9 - y^2", true), cau("2.26b", "4x^2 - y^2 + 4y - 4", true)),
            mapOf("2.26a" to 2, "2.26b" to 2),
            now - ngay
        )

        // Hom nay con lam not hai bai duoi, chup CA TRANG (co ca bai hom qua).
        val chupLai = listOf(
            cau("2.26a", "x^2 - 6x + 9 - y^2", true),
            cau("2.26b", "4x^2 - y^2 + 4y - 4", true),
            cau("2.26c", "xy + z^2 + xz + yz", true),
            cau("2.26d", "x^2 - 4xy + 4y^2 + xz - 2yz", true)
        )
        val moi = chupLai.filterNot { SoCaiBai.daTraGio(context, it.de, now) }

        // Chi hai bai duoi la moi. Nhan dang theo TUNG CAU chu khong theo tam anh,
        // nen chup ca trang cung khong duoc tinh lai phan da tinh hom qua.
        assertEquals(listOf("2.26c", "2.26d"), moi.map { it.ma })
        SoCaiBai.ghi(context, moi, mapOf("2.26c" to 2, "2.26d" to 2), now)
        assertEquals(4, SoCaiBai.phutDaCongHomNay(context, now))
    }

    @Test
    fun khong_cap_duoc_gio_thi_chi_ghi_cau_sai_va_khong_tru_tran() {
        // Canh AI doc khong ro, hoac qua gio nghi: khong cap gio nao ca. Luc do chi
        // ghi cau SAI de con biet ma sua; cau dung khong duoc danh dau la da tra gio,
        // de mai nop lai van duoc tinh.
        val dung = cau("2.26a", "x^2 - 6x + 9 - y^2", dung = true)
        val sai = cau("2.26d", "x^2 - 4xy", dung = false, nhanXet = "sai dấu")
        SoCaiBai.ghi(context, listOf(sai), emptyMap(), now)

        assertEquals(0, SoCaiBai.phutDaCongHomNay(context, now))
        assertFalse(SoCaiBai.daTraGio(context, dung.de, now))
        assertEquals(listOf("2.26d"), SoCaiBai.dangChoSua(context, now).map { it.ma })
    }

    @Test
    fun phut_cua_hom_qua_khong_tinh_vao_tran_hom_nay() {
        SoCaiBai.ghi(
            context,
            listOf(cau("hôm qua", "de cu", dung = true)),
            mapOf("hôm qua" to 20),
            now - ngay
        )
        assertEquals(0, SoCaiBai.phutDaCongHomNay(context, now))
    }

    /**
     * Du tran anh trong ngay thi man chinh doi nut Nop bai sang lam bai tren may. Thay cho
     * "hom nay da tinh tron goi" tu 30/9/2026, khi Ba Huy bo tron goi.
     */
    @Test
    fun du_tran_anh_trong_ngay_thi_het_tran_anh() {
        assertFalse(SoCaiBai.hetTranAnhHomNay(context, now))
        SoCaiBai.ghi(context, listOf(cau("a", "de a", dung = true)), mapOf("a" to 40), now)
        assertFalse(SoCaiBai.hetTranAnhHomNay(context, now))
        SoCaiBai.ghi(context, listOf(cau("b", "de b", dung = true)), mapOf("b" to 5), now)
        assertTrue(SoCaiBai.hetTranAnhHomNay(context, now))
        // Sang hom sau lai tu dau.
        assertFalse(SoCaiBai.hetTranAnhHomNay(context, now + ngay))
    }

    @Test
    fun bai_cham_thang_truoc_nop_lai_van_bi_nhan_ra() {
        SoCaiBai.ghi(
            context,
            listOf(cau("2.26a", "x^2 - 6x + 9 - y^2", true)),
            mapOf("2.26a" to 2),
            now - 40 * ngay
        )
        // Bon muoi ngay sau con chup lai chinh trang do: van la bai da tra gio.
        assertTrue(SoCaiBai.daTraGio(context, "x^2 - 6x + 9 - y^2", now))
    }

    @Test
    fun so_cu_hon_mot_nam_thi_tu_bien_mat() {
        SoCaiBai.ghi(
            context,
            listOf(cau("cũ", "bài từ đời nào", dung = true)),
            mapOf("cũ" to 2),
            now - 400 * ngay
        )
        assertTrue(SoCaiBai.tatCa(context, now).isEmpty())
        assertFalse(SoCaiBai.daTraGio(context, "bài từ đời nào", now))
    }

    @Test
    fun cau_da_xong_thi_khong_giu_lai_loi_che() {
        SoCaiBai.ghi(
            context,
            listOf(cau("2.26d", "de", dung = false, nhanXet = "Dòng 3 sai dấu")),
            emptyMap(), now
        )
        assertEquals("Dòng 3 sai dấu", SoCaiBai.dangChoSua(context, now).first().nhanXet)

        SoCaiBai.ghi(context, listOf(cau("2.26d", "de", dung = true)), mapOf("2.26d" to 2), now)
        assertEquals("", SoCaiBai.tatCa(context, now).first { it.ma == "2.26d" }.nhanXet)
    }

    @Test
    fun loi_nhan_cho_con_het_han_sau_muoi_hai_tieng() {
        SoCaiBai.datLoiNhan(context, "Được thêm 7 phút.", now)
        assertEquals("Được thêm 7 phút.", SoCaiBai.loiNhan(context, now))
        assertEquals("Được thêm 7 phút.", SoCaiBai.loiNhan(context, now + 11 * 60 * 60_000L))
        // Sang hom sau thi thoi, khong de loi nhan cua toi qua nam lai tren man hinh.
        assertNull(SoCaiBai.loiNhan(context, now + 13 * 60 * 60_000L))
    }

    /**
     * Co tin vui (1/10/2026) di theo lan dat loi nhan gan nhat: loi nhan sau khong mang co cua
     * loi nhan truoc, khong khai thi khong phai tin vui, xoa loi nhan thi mat ca co.
     */
    @Test
    fun co_tin_vui_di_theo_lan_dat_loi_nhan_gan_nhat() {
        assertFalse(SoCaiBai.loiNhanLaTinVui(context))

        SoCaiBai.datLoiNhan(context, "Bài tốt! Được thêm 7 phút.", now, tinVui = true)
        assertTrue(SoCaiBai.loiNhanLaTinVui(context))

        SoCaiBai.datLoiNhan(context, "Có 1 câu không thấy đề bài. Lê Hòa chụp thêm trang đề nhé.", now)
        assertFalse(SoCaiBai.loiNhanLaTinVui(context))

        SoCaiBai.datLoiNhan(context, "Bài tốt! Được thêm 7 phút.", now, tinVui = true)
        SoCaiBai.xoaLoiNhan(context)
        assertNull(SoCaiBai.loiNhan(context, now))
        assertFalse(SoCaiBai.loiNhanLaTinVui(context))
    }
    /**
     * Ba phan ba tran rieng (29/9/2026): chup anh 45, lam tren may 90, on lai tren may 30.
     * Moi phan dem rieng, khong phan nao an cua phan kia.
     *
     * Dong tron goi cu (truoc 30/9/2026) van la phut chup anh cua ngay do: ghi thang vao kho
     * nhu mot dong cu con nam trong so.
     */
    @Test
    fun phut_anh_gom_ca_goi_cu_con_tren_may_va_on_lai_dem_rieng() {
        KhoBai.get(context).ghiTraLoi(dongGoiCu(phut = 40))
        SoCaiBai.ghi(context, listOf(cau("anh", "bài ảnh", dung = true)), mapOf("anh" to 5), now)
        val kho = KhoBai.get(context)
        kho.ghiTraLoi(dongTrenMay("tren may", phut = 7, onTap = false))
        kho.ghiTraLoi(dongTrenMay("on lai", phut = 3, onTap = true))
        // Cau sai chup anh: dung = 0 nen khong vao phan chup anh.
        SoCaiBai.ghi(context, listOf(cau("sai", "bài sai", dung = false)), emptyMap(), now)

        assertEquals(45, SoCaiBai.phutAnhHomNay(context, now))
        assertEquals(7, SoCaiBai.phutTrenMayHomNay(context, now))
        assertEquals(3, SoCaiBai.phutOnHomNay(context, now))
        assertEquals(55, SoCaiBai.phutDaCongHomNay(context, now))
        // Hom sau ca ba ve 0.
        assertEquals(0, SoCaiBai.phutAnhHomNay(context, now + ngay))
        assertEquals(0, SoCaiBai.phutTrenMayHomNay(context, now + ngay))
        assertEquals(0, SoCaiBai.phutOnHomNay(context, now + ngay))
    }

    /**
     * Cau sai vao mot ngay cu da tinh tron goi thi khong hien cho sua (Ba Huy chot 29/9/2026):
     * sua xong cung khong ra phut, vi bai do da nam trong goi. Cau sai hom khong co goi thi
     * van hien. Tu 30/9/2026 khong con goi moi, nen chi con ngay cu bi bo.
     */
    @Test
    fun cau_sai_vao_ngay_cu_da_co_goi_thi_khong_hien_cho_sua() {
        SoCaiBai.ghi(context, listOf(cau("hôm qua", "bài hôm qua", dung = false)), emptyMap(), now - ngay)
        KhoBai.get(context).ghiTraLoi(dongGoiCu(phut = 45))
        SoCaiBai.ghi(context, listOf(cau("hôm nay", "bài hôm nay", dung = false)), emptyMap(), now)

        assertEquals(listOf("hôm qua"), SoCaiBai.dangChoSua(context, now).map { it.ma })
    }

    /** Dong tron goi vo dan do nhu ban cu ghi, luc [now]. */
    private fun dongGoiCu(phut: Int) = TraLoi(
        cauId = KhoBai.CAU_GOI, mon = "", ma = "trọn gói", de = "làm hết bài cô giao",
        ketQua = "", dung = true, phut = phut, nhanXet = "", luc = now
    )

    /** Luot sai tren may mat sao va 24 gio sau lam lai tren may, khong phai chup vo nop lai. */
    @Test
    fun luot_sai_tren_may_khong_nam_trong_danh_sach_cho_sua() {
        KhoBai.get(context).ghiTraLoi(dongTrenMay("tren may sai", phut = 0, onTap = false, dung = false))
        SoCaiBai.ghi(context, listOf(cau("anh sai", "bài ảnh sai", dung = false)), emptyMap(), now)

        assertEquals(listOf("anh sai"), SoCaiBai.dangChoSua(context, now).map { it.ma })
    }

    /** Mot luot lam tren may, chi dien nhung truong phep dem phut doc. */
    private fun dongTrenMay(ma: String, phut: Int, onTap: Boolean, dung: Boolean = true) = TraLoi(
        cauId = "somay:$ma",
        mon = "Toán",
        ma = ma,
        de = "đề $ma",
        ketQua = "",
        onTap = onTap,
        dung = dung,
        phut = phut,
        nhanXet = "",
        luc = now,
        trenMay = true,
        sao = if (dung) phut else 0,
        saoToiDa = 5,
        vong = if (onTap) 1 else 0
    )
}
