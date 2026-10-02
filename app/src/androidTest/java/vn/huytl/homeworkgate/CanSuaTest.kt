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
import vn.huytl.homeworkgate.data.CauChuaRo
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.NganHang

/**
 * Danh sach "câu cần sửa" cua con (Ba Huy chot 30/9/2026): lan sai qua mot tuan thi roi,
 * cau lam duoc tren may roi sau 24 gio, cau Ba Huy bo bang lenh BOSUA thi roi, cau Claude doc
 * chua chac thi an toi khi Ba Huy xu. So cai van giu dong sai, va [SoCaiBai.dangChoSua] (cho
 * SUACHAM dung) khong doi.
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
        MocThu.datToan(context, 3)
        return LamTrenMay.cauLamThem(context, "Toán").first().cau
    }

    private fun maCanSua(luc: Long = now) = SoCaiBai.canSua(context, luc).map { it.ma }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(context).raw().edit().clear().commit()
        SoCaiBai.xoaHet(context)
        BoSua.xoaHet(context)
        CauChuaRo.xoaHet(context)
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

    /**
     * Cau Claude doc chua chac ma cham chua dung (30/9/2026): so ghi sai nhung an khoi danh sach,
     * vi the bai ghi "Ba Huy sẽ xem lại" va khong co nut nop lai. Ba Huy cham Sai thi lan ghi
     * moi doc ro, cau hien ra.
     */
    @Test
    fun cau_claude_doc_chua_chac_ma_sai_thi_an_toi_khi_ba_huy_xu() {
        val cham = listOf(
            ngoaiSach("B5-TL3g", "Cân bằng Fe + Cl2 → FeCl3").copy(docRo = false),
            ngoaiSach("B5-TL1a", "Lập phương trình Fe + O2 → Fe3O4")
        )
        CauChuaRo.danhDau(context, cham, SoCaiBai.ghi(context, cham, emptyMap(), now - 2 * gio))
        assertEquals(listOf("B5-TL1a"), maCanSua())
        // So cai van ghi sai: SUACHAM van tim thay.
        assertEquals(setOf("B5-TL3g", "B5-TL1a"), SoCaiBai.dangChoSua(context, now).map { it.ma }.toSet())

        // Ba Huy cham Sai o the "Câu cần Ba Huy xem": tablet cham lai, lan nay doc ro.
        val lai = listOf(ngoaiSach("B5-TL3g", "Cân bằng Fe + Cl2 → FeCl3"))
        CauChuaRo.danhDau(context, lai, SoCaiBai.ghi(context, lai, emptyMap(), now - gio))
        assertEquals(setOf("B5-TL3g", "B5-TL1a"), maCanSua().toSet())
    }

    /** Ba Huy bam Chup lai: cau hien trong danh sach, con nop lai duoc tren the bai. */
    @Test
    fun ba_huy_nho_chup_lai_thi_cau_hien_ra() {
        val gate = GateStore(context)
        val id = "thu-chup-lai"
        val de = "Cân bằng Al + O2 → Al2O3"
        try {
            gate.markPending(id, 0L)
            val cham = listOf(ngoaiSach("B5-TL3i", de).copy(docRo = false))
            CauChuaRo.danhDau(context, cham, SoCaiBai.ghi(context, cham, emptyMap(), now - gio))
            assertTrue(maCanSua().isEmpty())

            val giaTri = mapOf(
                "cac" to listOf(
                    mapOf("ma" to "B5-TL3i", "dung" to false, "chac" to true, "chupLai" to true, "de" to de)
                )
            )
            // Bai khong con cho thi lenh khong duoc cham, cau van an.
            ThiHanhLenh.hienCauChupLai(context, gate, "bai-khac", giaTri, listOf("B5-TL3i"))
            assertTrue(maCanSua().isEmpty())
            ThiHanhLenh.hienCauChupLai(context, gate, id, giaTri, listOf("B5-TL3i"))
            assertEquals(listOf("B5-TL3i"), maCanSua())
        } finally {
            gate.boBaiCho(id)
            gate.endSession(EndReason.PARENT_REVOKED)
        }
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
