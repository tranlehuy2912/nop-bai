package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.KhoaAi
import vn.huytl.homeworkgate.data.Prefs

/**
 * Chum khoa AI: khoa nay bi tu choi thi nhay sang khoa ke tiep, va nghi dung lau.
 *
 * Cho de sai nhat la lan lon giua het han muc theo PHUT va theo NGAY. Het phut ma
 * nghi ca ngay la phi khoa; het ngay ma mot phut sau lai goi la lan nao cung an
 * 429. Nen phan lon bo test nay la ve cau tra loi loi cua Google.
 *
 * CAN THAN: giong [GateStoreTest], bo test nay xoa sach prefs. Dung chay tren
 * tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class KhoaAiTest {

    private lateinit var context: Context
    private lateinit var prefs: Prefs
    private val phut = 60_000L
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = Prefs.get(context)
        prefs.raw().edit().clear().commit()
        prefs.aiKeys = listOf("khoa-mot", "khoa-hai", "khoa-ba")
    }

    // --- thu tu va viec nhay khoa ---

    @Test
    fun chua_dung_gi_thi_lay_khoa_dau_tien() {
        assertEquals("khoa-mot", KhoaAi.hienTai(context, now))
        assertEquals(1, KhoaAi.thuTuDangDung(context, now))
        assertEquals(3, KhoaAi.soKhoaConDung(context, now))
    }

    @Test
    fun khoa_dang_nghi_thi_nhay_sang_khoa_sau() {
        KhoaAi.danhDauNghi(context, "khoa-mot", now + phut, KhoaAi.Vi.PHUT, now)
        assertEquals("khoa-hai", KhoaAi.hienTai(context, now))
        assertEquals(2, KhoaAi.soKhoaConDung(context, now))
    }

    @Test
    fun het_gio_nghi_thi_khoa_dau_tu_song_lai() {
        KhoaAi.danhDauNghi(context, "khoa-mot", now + phut, KhoaAi.Vi.PHUT, now)
        assertEquals("khoa-hai", KhoaAi.hienTai(context, now))

        // Mot phut sau, khong ai phai lam gi ca.
        assertEquals("khoa-mot", KhoaAi.hienTai(context, now + phut + 1))
        assertEquals(3, KhoaAi.soKhoaConDung(context, now + phut + 1))
    }

    @Test
    fun ca_chum_dang_nghi_thi_tra_ve_null_chu_khong_quay_vong() {
        KhoaAi.danhDauNghi(context, "khoa-mot", now + phut, now = now)
        KhoaAi.danhDauNghi(context, "khoa-hai", now + phut, now = now)
        assertNull(KhoaAi.danhDauNghi(context, "khoa-ba", now + phut, now = now))
        assertNull(KhoaAi.hienTai(context, now))
        assertEquals(0, KhoaAi.soKhoaConDung(context, now))
    }

    @Test
    fun them_khoa_moi_thi_dung_duoc_ngay() {
        listOf("khoa-mot", "khoa-hai", "khoa-ba").forEach {
            KhoaAi.danhDauNghi(context, it, now + 10 * phut, now = now)
        }
        assertNull(KhoaAi.hienTai(context, now))

        prefs.aiKeys = prefs.aiKeys + "khoa-bon"
        assertEquals("khoa-bon", KhoaAi.hienTai(context, now))
    }

    // --- doc cau tra loi loi cua Google ---

    @Test
    fun het_han_muc_theo_phut_thi_nghi_dung_so_giay_google_bao() {
        val than = """
            {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
            {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
            {"quotaMetric":"generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId":"GenerateRequestsPerMinutePerProjectPerModel-FreeTier"}]},
            {"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"27s"}]}}
        """.trimIndent()

        val nghi = KhoaAi.tinhNghi(429, than, now)
        assertNotNull(nghi)
        assertEquals(KhoaAi.Vi.PHUT, nghi!!.vi)
        assertEquals(now + 28_000L, nghi.denLuc)
    }

    @Test
    fun het_han_muc_theo_ngay_thi_nghi_den_nua_dem() {
        val than = """
            {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
            {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
            {"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier","quotaValue":"50"}]},
            {"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"32s"}]}}
        """.trimIndent()

        val nghi = KhoaAi.tinhNghi(429, than, now)
        assertNotNull(nghi)
        assertEquals(KhoaAi.Vi.NGAY, nghi!!.vi)
        // Het ngay thi khong duoc nghe theo retryDelay 32 giay: phai dung nua dem.
        // Tinh moc nua dem ngay trong test, chu khong doan kieu "hon mot tieng nua" -
        // chay bo test luc 23:00 la cau do sai trong khi ma van dung.
        val nuaDem = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(nuaDem, nghi.denLuc)
    }

    @Test
    fun bi_429_ma_khong_ro_kieu_thi_nghi_mot_phut() {
        val nghi = KhoaAi.tinhNghi(429, """{"error":{"code":429}}""", now)
        assertEquals(KhoaAi.Vi.PHUT, nghi!!.vi)
        assertEquals(now + 60_000L, nghi.denLuc)
    }

    @Test
    fun khoa_sai_thi_nghi_han_mot_ngay() {
        val than = """
            {"error":{"code":400,"message":"API key not valid. Please pass a valid API key.",
            "status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}
        """.trimIndent()

        val nghi = KhoaAi.tinhNghi(400, than, now)
        assertEquals(KhoaAi.Vi.HONG, nghi!!.vi)
        assertEquals(now + 24 * 60 * 60_000L, nghi.denLuc)
    }

    /**
     * Than tin that Google tra ve ngay 13/9 khi goi gemini-3.1-pro bang khoa mien
     * phi. "limit: 0" nghia la model do khong co suat mien phi nao ca - doi sang
     * khoa khac cung the. Trach khoa o day la mat ca chum vi mot cai ten model.
     */
    @Test
    fun model_khong_co_suat_mien_phi_thi_khong_trach_khoa() {
        val than = """
            {"error":{"code":429,"message":"You exceeded your current quota...
            * Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 0, model: gemini-3.1-pro
            Please retry in 57.315865302s.","status":"RESOURCE_EXHAUSTED","details":[
            {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
            {"quotaId":"GenerateContentInputTokensPerModelPerDay-FreeTier"}]}]}}
        """.trimIndent()

        assertNull(KhoaAi.tinhNghi(429, than, now))
        assertEquals("khoa-mot", KhoaAi.nghiTheoLoi(context, "khoa-mot", 429, than, now))
    }

    @Test
    fun doc_duoc_so_giay_nam_trong_cau_tieng_anh() {
        // Co loi 429 khong kem khoi RetryInfo, so giay chi nam trong cau chu.
        val than = """{"error":{"code":429,"message":"Quota exceeded for metric: ...requests, limit: 15, model: gemini-3.5-flash-lite. Please retry in 41.5s."}}"""
        val nghi = KhoaAi.tinhNghi(429, than, now)
        assertEquals(KhoaAi.Vi.PHUT, nghi!!.vi)
        assertEquals(now + 42_000L, nghi.denLuc)
    }

    @Test
    fun may_chu_google_qua_tai_thi_khong_trach_khoa() {
        // 503 la loi ben Google, doi khoa khong giup gi. Tra ve null de ben goi thu
        // lai chinh khoa do, thay vi dot sach ca chum vi mot luc may chu ban.
        assertNull(KhoaAi.tinhNghi(503, """{"error":{"code":503,"message":"overloaded"}}""", now))
        assertNull(KhoaAi.tinhNghi(500, """{"error":{"code":500}}""", now))
    }

    @Test
    fun loi_503_thi_van_dung_khoa_cu() {
        val tiep = KhoaAi.nghiTheoLoi(context, "khoa-mot", 503, """{"code":503}""", now)
        assertEquals("khoa-mot", tiep)
        assertEquals(3, KhoaAi.soKhoaConDung(context, now))
    }

    @Test
    fun het_han_muc_ngay_o_khoa_dau_thi_lan_goi_sau_dung_khoa_hai() {
        val than = """{"error":{"code":429,"details":[{"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier"}]}}"""
        val tiep = KhoaAi.nghiTheoLoi(context, "khoa-mot", 429, than, now)
        assertEquals("khoa-hai", tiep)
        // Sang hom sau khoa dau lai dung duoc.
        assertEquals("khoa-mot", KhoaAi.hienTai(context, now + 25 * 60 * 60_000L))
    }

    // --- linh tinh ---

    @Test
    fun danh_sach_khoa_giu_dung_thu_tu_da_nhap() {
        prefs.aiKeys = listOf("  khoa-z  ", "", "khoa-a")
        assertEquals(listOf("khoa-z", "khoa-a"), prefs.aiKeys)
    }

    @Test
    fun rut_gon_khong_lam_lo_ca_khoa() {
        // Khoa gia, dung dang that de phep rut gon van cat dung cho.
        val ngan = KhoaAi.rutGon("AIzaSyKHOA-GIA-CHI-DUNG-TRONG-TEST-0000")
        assertEquals("AIzaSy…0000", ngan)
        assertTrue(ngan.length < 16)
    }

    @Test
    fun mo_ta_noi_ro_con_bao_lau_khi_ca_chum_dang_nghi() {
        listOf("khoa-mot", "khoa-hai", "khoa-ba").forEach {
            KhoaAi.danhDauNghi(context, it, now + 2 * phut, KhoaAi.Vi.PHUT, now)
        }
        val dong = KhoaAi.moTa(context, now)
        assertTrue(dong, dong.contains("phút nữa"))
    }

    @Test
    fun xoa_danh_dau_thi_dung_lai_tu_khoa_dau() {
        KhoaAi.danhDauNghi(context, "khoa-mot", now + 10 * phut, now = now)
        KhoaAi.xoaDanhDau(context)
        assertEquals("khoa-mot", KhoaAi.hienTai(context, now))
    }
}
