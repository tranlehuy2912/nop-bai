package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.BanLich
import vn.huytl.homeworkgate.data.LichDangDung
import vn.huytl.homeworkgate.data.LichMacDinh
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import java.util.Calendar

/**
 * Lich nhan tu Firestore (4/10/2026): ban nao thang, luu xuong may, ban hong thi giu ban cu.
 *
 * Kiem tung luat cua [BanLich.doc] nam o BanLichTest ben Bang dieu khien (chay tren JVM, cung
 * mot file). O day chi kiem phan can may that: file prefs va luc mo lai app.
 */
@RunWith(AndroidJUnit4::class)
class LichDangDungTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = context.getSharedPreferences("lich_hoc", Context.MODE_PRIVATE)

    private var fileCu: Map<String, *> = emptyMap<String, Any>()
    private var banCu: BanLich? = null

    private val macDinh get() = BanLich.doc(LichMacDinh.JSON)

    @Before
    fun luu() {
        fileCu = file.all.toMap()
        banCu = LichDangDung.ban
        file.edit().clear().commit()
        LichDangDung.dungTam(macDinh)
    }

    @After
    fun traLai() {
        file.edit().clear().apply { fileCu.forEach { (k, v) -> putString(k, v as String) } }.commit()
        banCu?.let(LichDangDung::dungTam)
    }

    /** Ban mac dinh doi phien ban, sua them theo [sua]. */
    private fun ban(phienBan: Int, sua: (JSONObject) -> Unit = {}): String =
        JSONObject(LichMacDinh.JSON).apply {
            put("phienBan", phienBan)
            sua(this)
        }.toString()

    @Test
    fun ban_moi_thi_doi_ngay_va_mo_lai_app_van_con() {
        val so = macDinh.phienBan + 1
        val kq = LichDangDung.nhan(context, ban(so) {
            it.getJSONObject("chieu").getJSONObject("2").put("2", "Ngữ văn")
            it.getJSONArray("ngayNghi").put(JSONObject("""{"tu":"2026-11-20","den":"2026-11-20","ten":"Ngày Nhà giáo"}"""))
        })
        assertTrue("$kq", kq is LichDangDung.KetQua.Doi)
        assertEquals("Ngữ văn", ThoiKhoaBieu.buoiHocCua(Calendar.MONDAY).last().monTheoTiet[2])
        assertEquals("Ngày Nhà giáo", NgayNghi.tenKyNghi(NgayNghi.calendarCua(2026, 11, 20, 9)))

        // Mo lai app: ban luu xuong may thang ban mang san trong app.
        LichDangDung.dungTam(macDinh)
        LichDangDung.napTuMay(context)
        assertEquals(so, LichDangDung.ban.phienBan)
    }

    @Test
    fun ban_cu_hon_ban_dang_dung_thi_bo() {
        LichDangDung.dungTam(BanLich.doc(ban(5)))
        val kq = LichDangDung.nhan(context, ban(4) { it.getJSONObject("chieu").remove("7") })
        assertTrue("$kq", kq is LichDangDung.KetQua.CuHon)
        assertEquals(5, LichDangDung.ban.phienBan)
        assertEquals(1, ThoiKhoaBieu.buoiHocCua(Calendar.SATURDAY).size)
    }

    @Test
    fun ban_hong_thi_giu_ban_dang_dung_va_noi_cho_hong() {
        val dang = LichDangDung.ban
        val kq = LichDangDung.nhan(context, ban(9) {
            it.getJSONObject("chieu").getJSONObject("3").put("6", "Toán")
        })
        assertTrue("$kq", kq is LichDangDung.KetQua.Hong)
        assertTrue((kq as LichDangDung.KetQua.Hong).loi, kq.loi.startsWith("chieu.3.6:"))
        assertTrue(LichDangDung.nhan(context, "{ chưa xong") is LichDangDung.KetQua.Hong)
        assertEquals(dang, LichDangDung.ban)
        assertTrue(file.all.isEmpty())
    }

    @Test
    fun giong_het_ban_dang_dung_thi_khong_ghi_gi() {
        assertEquals(LichDangDung.KetQua.GiuNguyen, LichDangDung.nhan(context, LichMacDinh.JSON))
        assertTrue(file.all.isEmpty())
    }

    @Test
    fun ban_mang_san_moi_hon_ban_luu_thi_dung_ban_mang_san() {
        // Cai APK moi mang san ban 7, trong may con ban 6 nhan tu truoc.
        LichDangDung.nhan(context, ban(6))
        LichDangDung.dungTam(BanLich.doc(ban(7)))
        LichDangDung.napTuMay(context)
        assertEquals(7, LichDangDung.ban.phienBan)
    }

    @Test
    fun lich_mau_cua_ban_thu_dung_yen_cho_toi_khi_bo() {
        LichDangDung.epTam(context, ban(3) { it.put("ghiChu", "mẫu") })
        assertEquals("mẫu", LichDangDung.ban.ghiChu)
        // Firestore gui ban moi hon: may ao dang thu van giu ban mau.
        assertEquals(LichDangDung.KetQua.GiuNguyen, LichDangDung.nhan(context, ban(50)))
        LichDangDung.dungTam(macDinh)
        LichDangDung.napTuMay(context)
        assertEquals("mẫu", LichDangDung.ban.ghiChu)

        LichDangDung.epTam(context, null)
        assertEquals(macDinh, LichDangDung.ban)
    }
}
