package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LoaiLoi
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Cho hay vap, va ba cau de luyen lai cho do.
 *
 * Bang bay nhan loi von chi mot minh Ba Huy doc, qua lenh /loi. Duong nay dua no ve
 * man hinh cua con - nhung chi mot nhan, khong kem con so, va luon di cung ba cau
 * lam duoc. Xem [KhoBai.nhanHayVap] va [KhoBai.cacCauLuyenTheoLoi].
 *
 * CAN THAN: bo test nay nap hai quyen gia vao kho that tren may. Don o [tearDown].
 */
@RunWith(AndroidJUnit4::class)
class LuyenTheoLoiTest {

    private lateinit var context: Context
    private val now = System.currentTimeMillis()
    private val ngay = 24 * 60 * 60_000L
    private val thang = 30 * ngay

    /**
     * Hai bai trong mot quyen gia.
     *
     * Bai 6 la cho con vap; bai 7 khong lien quan, de kiem rang danh sach luyen
     * khong vo bua ca quyen ra.
     */
    private val bai6 = (1..5).map { cauHoi("6.$it", "Bài 6. Hiệu hai bình phương", it) }
    private val bai7 = (1..3).map { cauHoi("7.$it", "Bài 7. Phương trình", 10 + it) }

    private fun cauHoi(ma: String, bai: String, thuTu: Int, dang: String = "CAU_NHO") = CauHoi(
        id = "thu:$ma",
        mon = "Toán",
        nguon = "thu",
        chuong = "Chương II",
        bai = bai,
        nhom = "Bài tập",
        ma = ma,
        de = "Đề của câu $ma",
        trang = 36,
        dang = dang,
        thuTu = thuTu
    )

    /** Mot lan cham, chi dien nhung truong bang nay thuc su doc. */
    private fun sai(ma: String, nhan: String, luc: Long = now - ngay) = TraLoi(
        cauId = "thu:$ma",
        mon = "Toán",
        ma = ma,
        de = "Đề của câu $ma",
        ketQua = "",
        dung = false,
        phut = 0,
        nhanXet = "",
        luc = luc,
        loaiLoi = nhan
    )

    private fun dung(ma: String, luc: Long = now - ngay) = TraLoi(
        cauId = "thu:$ma",
        mon = "Toán",
        ma = ma,
        de = "Đề của câu $ma",
        ketQua = "",
        dung = true,
        phut = 2,
        nhanXet = "",
        luc = luc
    )

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        donKho()
        KhoBai.get(context).napNguon("thu", bai6 + bai7)
    }

    @After
    fun tearDown() = donKho()

    private fun donKho() {
        val kho = KhoBai.get(context)
        runCatching { kho.napNguon("thu", emptyList()) }
        runCatching { kho.writableDatabase.delete("tra_loi", "cau_id LIKE ?", arrayOf("thu:%")) }
    }

    // ------------------------------------------------------------------ cho vap

    @Test
    fun vap_hai_lan_thi_chua_noi_gi_voi_con() {
        val kho = KhoBai.get(context)
        kho.ghiTraLoi(sai("6.1", LoaiLoi.SAI_DAU))
        kho.ghiTraLoi(sai("6.2", LoaiLoi.SAI_DAU))

        // Hai lan co the chi la hai hom mat tap trung. Dong chu "con hay sai kieu
        // nay" thi dua tre mang theo ca thang, nen no phai dat gia hon hai lan.
        assertNull(kho.nhanHayVap(now - thang))
    }

    @Test
    fun vap_ba_lan_thi_goi_ten_dung_cho_do() {
        val kho = KhoBai.get(context)
        repeat(2) { kho.ghiTraLoi(sai("6.1", LoaiLoi.SAI_DAU)) }
        kho.ghiTraLoi(sai("6.2", LoaiLoi.SAI_DAU))
        kho.ghiTraLoi(sai("7.1", LoaiLoi.TINH_NHAM))

        assertEquals(LoaiLoi.SAI_DAU to 3, kho.nhanHayVap(now - thang))
    }

    @Test
    fun kieu_khac_thi_khong_bao_gio_duoc_goi_ten() {
        val kho = KhoBai.get(context)
        repeat(5) { kho.ghiTraLoi(sai("6.1", LoaiLoi.KHAC)) }
        repeat(3) { kho.ghiTraLoi(sai("6.2", LoaiLoi.NHAM_CONG_THUC)) }

        // KHAC la thung rac cua sau nhan kia: noi voi con rang no hay sai "kieu
        // khac" thi vua dung vua khong lam duoc gi. Nhan dung thu hai len thay cho.
        assertEquals(LoaiLoi.NHAM_CONG_THUC to 3, kho.nhanHayVap(now - thang))
    }

    // ---------------------------------------------------------------- cau luyen

    @Test
    fun cau_luyen_lay_trong_dung_bai_con_hay_vap() {
        val kho = KhoBai.get(context)
        repeat(3) { kho.ghiTraLoi(sai("6.1", LoaiLoi.SAI_DAU)) }

        val cau = kho.cacCauLuyenTheoLoi(LoaiLoi.SAI_DAU, now - thang)
        assertEquals(3, cau.size)
        // Cung bai voi cho vap, khong phai bai 7 vo can.
        assertTrue(cau.all { it.bai == bai6.first().bai })
        // Va khong tra lai chinh cau con vua lam.
        assertTrue(cau.none { it.id == "thu:6.1" })
    }

    @Test
    fun cau_da_dung_den_roi_thi_khong_dua_ra_luyen_nua() {
        val kho = KhoBai.get(context)
        repeat(3) { kho.ghiTraLoi(sai("6.1", LoaiLoi.SAI_DAU)) }
        kho.ghiTraLoi(dung("6.2"))
        kho.ghiTraLoi(sai("6.3", LoaiLoi.THIEU))

        val cau = kho.cacCauLuyenTheoLoi(LoaiLoi.SAI_DAU, now - thang)
        // 6.2 da lam dung, 6.3 dang cho sua o duong khac ngoai man chinh. Con lai
        // 6.4 va 6.5 la hai cau chua dung den bao gio.
        assertEquals(listOf("thu:6.4", "thu:6.5"), cau.map { it.id })
    }

    @Test
    fun trac_nghiem_khong_dung_de_luyen_cho_vap() {
        val kho = KhoBai.get(context)
        // Nap lai quyen: bai 6 gio toan cau trac nghiem.
        kho.napNguon(
            "thu",
            (1..5).map { cauHoi("6.$it", "Bài 6. Hiệu hai bình phương", it, dang = "TRAC_NGHIEM") }
        )
        repeat(3) { kho.ghiTraLoi(sai("6.1", LoaiLoi.SAI_DAU)) }

        // Mot cau khoanh A B C D khong luyen duoc cai dau hay cai buoc bien doi, ma
        // do la ca ly do con duoc dua den day.
        assertTrue(kho.cacCauLuyenTheoLoi(LoaiLoi.SAI_DAU, now - thang).isEmpty())
    }

    @Test
    fun chua_vap_o_bai_nao_co_trong_sach_thi_khong_co_gi_de_luyen() {
        val kho = KhoBai.get(context)
        assertTrue(kho.cacCauLuyenTheoLoi(LoaiLoi.SAI_DAU, now - thang).isEmpty())
    }
}
