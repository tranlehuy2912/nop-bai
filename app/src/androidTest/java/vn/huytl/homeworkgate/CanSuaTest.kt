package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.BoSua
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.NganHang

/**
 * Danh sach "câu cần sửa" cua con (Ba Huy chot 30/9/2026): lan sai qua mot tuan thi roi,
 * cau lam duoc tren may roi sau 24 gio, cau Ba Huy bo bang lenh BOSUA thi roi. So cai van
 * giu dong sai, va [SoCaiBai.dangChoSua] (cho SUACHAM dung) khong doi.
 *
 * CAN THAN: xoa sach prefs va so cai nhu [SoCaiBaiTest]. Dung chay tren tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class CanSuaTest {

    private lateinit var context: Context
    private val now = System.currentTimeMillis()
    private val gio = 60 * 60_000L
    private val ngay = 24 * gio

    private fun ngoaiSach(ma: String, de: String) =
        CauCham(ma = ma, de = de, dung = false, soDong = 3)

    private fun trongSach(c: CauHoi) =
        CauCham(ma = c.ma, de = c.de, dung = false, soDong = 3, cauId = c.id, mon = c.mon)

    /** Mot cau Toan lam duoc tren may, lay dung cach duong Lam bai tren may lay. */
    private fun cauTrenMay(): CauHoi {
        MocToanThu.dat(context, 3)
        return LamTrenMay.cauLamThem(context, "Toán").first().cau
    }

    private fun maCanSua(luc: Long = now) = SoCaiBai.canSua(context, luc).map { it.ma }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(context).raw().edit().clear().commit()
        SoCaiBai.xoaHet(context)
        BoSua.xoaHet(context)
        NganHang.napNeuCan(context)
    }

    @Test
    fun lan_sai_qua_mot_tuan_thi_roi_danh_sach_nhung_so_cai_van_giu() {
        SoCaiBai.ghi(context, listOf(ngoaiSach("cũ", "Bài vở tuần trước")), emptyMap(), now - 8 * ngay)
        SoCaiBai.ghi(context, listOf(ngoaiSach("mới", "Bài vở hôm kia")), emptyMap(), now - 6 * ngay)

        assertEquals(listOf("mới"), maCanSua())
        // SUACHAM van tim thay ca hai.
        assertEquals(setOf("cũ", "mới"), SoCaiBai.dangChoSua(context, now).map { it.ma }.toSet())
    }

    @Test
    fun cau_lam_duoc_tren_may_roi_danh_sach_sau_24_gio_cau_ngoai_sach_thi_khong() {
        val c = cauTrenMay()
        SoCaiBai.ghi(context, listOf(trongSach(c), ngoaiSach("ngoài", "Bài cô cho thêm")), emptyMap(), now)

        assertEquals(setOf(c.ma, "ngoài"), maCanSua(now + 2 * gio).toSet())
        assertEquals(listOf("ngoài"), maCanSua(now + 25 * gio))
        // Luc do cau nam o duong Lam bai tren may.
        assertTrue(LamTrenMay.cauLamThem(context, "Toán", bayGio = now + 25 * gio).any { it.cau.id == c.id })
    }

    @Test
    fun ba_huy_bo_cau_thi_roi_va_sai_lai_lan_sau_thi_hien_lai() {
        SoCaiBai.ghi(
            context,
            listOf(ngoaiSach("2.1", "Câu thứ nhất"), ngoaiSach("2.2", "Câu thứ hai")),
            emptyMap(), now - gio
        )

        val kq = BoSua.boTheoMa(context, listOf("2.1" to "Câu thứ nhất", "9.9" to ""))
        assertEquals(listOf("2.1"), kq.daBo)
        assertEquals(listOf("9.9"), kq.khongThay)
        assertEquals(listOf("2.2"), maCanSua())

        // Con nop lai 2.1 va lai sai: day la loi moi, hien lai.
        SoCaiBai.ghi(context, listOf(ngoaiSach("2.1", "Câu thứ nhất")), emptyMap(), now + gio)
        assertEquals(setOf("2.1", "2.2"), maCanSua(now + 2 * gio).toSet())
    }

    @Test
    fun lenh_bo_sua_tra_loi_cau_nao_da_bo_va_khong_cong_phut() {
        SoCaiBai.ghi(context, listOf(ngoaiSach("3.4", "Tính nhanh")), emptyMap(), now - gio)

        val tra = ThiHanhLenh.boSua(context, listOf(mapOf("ma" to "3.4", "de" to "Tính nhanh")))
        assertTrue(tra, tra.startsWith("Đã bỏ câu 3.4"))
        assertTrue(maCanSua().isEmpty())
        assertEquals(0, SoCaiBai.phutDaCongHomNay(context))
        assertTrue(ThiHanhLenh.boSua(context, null).startsWith("Lệnh thiếu"))
    }
}
