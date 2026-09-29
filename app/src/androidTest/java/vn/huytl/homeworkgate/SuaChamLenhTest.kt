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
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.CongSang
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh
import java.util.Calendar

/**
 * Lenh SUACHAM tu Bang dieu khien, voi dung kieu du lieu Firestore dua vao "giaTri".
 *
 * Khac [SuaChamTest] o cho di qua ca phan cap gio cua [GateStore]. Gio ngu dat quanh gio
 * that cua may, y nhu CatMangVpnTest: ngoai gio ngu thi cong ngay, trong gio ngu thi ghi so
 * va giu phut toi luc het gio ngu (tu 27/9/2026, xem CongSang).
 *
 * Tu 29/9/2026 khong con tran ngay chung: so phut cua cau sua chi chiu tran 45 cua duong
 * chup anh, tinh trong [vn.huytl.homeworkgate.data.SuaCham]. Bo test cu nang tam tran ngay
 * len cho du cho; nay khong can nua.
 *
 * Don dep sau khi chay: xoa so, va dong phieu gio vua cap. Khong xoa prefs.
 */
@RunWith(AndroidJUnit4::class)
class SuaChamLenhTest {

    private lateinit var context: Context
    private val de = "Rút gọn biểu thức (2x − 5y)(2x + 5y) + (2x + 5y)^2."
    private var nguCu = 0
    private var dayCu = 0

    // Dung kieu Firestore tra ve cho mot mang cac map.
    private val giaTri: List<Map<String, Any>> = listOf(mapOf("ma" to "2.33a", "de" to de))

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        SoCaiBai.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        nguCu = Prefs.get(context).hardStopMinuteOfDay
        dayCu = Prefs.get(context).gioDayMinuteOfDay
        datGioNgu(dangNgu = false)
        CongSang.xoaHet(context)
    }

    @After
    fun tearDown() {
        SoCaiBai.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        Prefs.get(context).hardStopMinuteOfDay = nguCu
        Prefs.get(context).gioDayMinuteOfDay = dayCu
        CongSang.xoaHet(context)
    }

    /** Dat gio ngu quanh gio that: dang ngu thi con mot tieng nua, khong thi ba tieng nua moi ngu. */
    private fun datGioNgu(dangNgu: Boolean) {
        val c = Calendar.getInstance()
        val bayGio = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val ngay = 24 * 60
        Prefs.get(context).hardStopMinuteOfDay = (bayGio + if (dangNgu) ngay - 60 else 180) % ngay
        Prefs.get(context).gioDayMinuteOfDay = (bayGio + if (dangNgu) 60 else 240) % ngay
    }

    /** Cau 2.33a bi cham sai, dang cho sua. [dong] la cac dong bai lam ghi trong so. */
    private fun ghiCauSai(dong: List<String> = listOf("a) 8x^2 + 20xy")) {
        SoCaiBai.ghi(
            context,
            listOf(
                CauCham(
                    ma = "2.33a", de = de, ketQua = "8x^2 + 20xy", dung = false, soDong = 1,
                    baiLam = dong, cauId = "sachthu:2.33a", mon = "Toán",
                    nhanXet = "máy bảo thiếu hạng tử y^2"
                )
            ),
            emptyMap()
        )
    }

    @Test
    fun lenh_sua_cham_bo_cau_khoi_danh_sach_sua_hoac_giu_nguyen_khi_khong_cap_duoc() {
        ghiCauSai()
        val tra = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)

        println("SUACHAM_LENH: $tra")
        // Mot dong lam bai, mot phut (29/9/2026 bo san 4 phut mot cau).
        assertTrue(tra, tra.startsWith("Đã sửa câu 2.33a thành đúng, cộng 1 phút"))
        assertTrue(SoCaiBai.dangChoSua(context).isEmpty())
        assertEquals(1, SoCaiBai.phutDaCongHomNay(context))
        assertTrue(SoCaiBai.loiNhan(context).orEmpty().contains("câu 2.33a Lê Hòa làm đúng rồi"))

        // Gui lai dung lenh do: khong cong them phut nao.
        val lan2 = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)
        assertTrue(lan2, lan2.startsWith("Không còn câu nào"))
        assertEquals(1, SoCaiBai.phutDaCongHomNay(context))
    }

    /**
     * Tran 45 cua duong chup anh cat bot cau sua, va cau tra loi, nhat ky, so cai deu noi
     * dung so phut con nhan that. Thay cho test tran ngay cu (tran chung bo ngay 29/9/2026).
     */
    @Test
    fun tran_anh_cat_bot_thi_tra_loi_va_so_noi_so_phut_that() {
        ghiCauSai(dong = listOf("dòng 1", "dòng 2", "dòng 3", "dòng 4"))
        // Hom nay da cong 43 phut chup anh: cau bon dong chi con 2 phut.
        SoCaiBai.ghi(
            context, listOf(CauCham(ma = "da xong", de = "bài đã chấm", dung = true, soDong = 43)),
            mapOf("da xong" to 43)
        )

        val tra = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)

        println("SUACHAM_TRAN: $tra")
        assertTrue(tra, tra.startsWith("Đã sửa câu 2.33a thành đúng, cộng 2 phút."))
        assertFalse(tra, tra.contains("hạn mức"))
        assertEquals(LuatCongGio.TRAN_ANH, SoCaiBai.phutAnhHomNay(context))
        assertTrue(SoCaiBai.loiNhan(context).orEmpty().contains("Được thêm 2 phút."))
    }

    @Test
    fun trong_gio_ngu_thi_ghi_so_ngay_va_giu_phut_toi_luc_het_gio_ngu() {
        ghiCauSai()
        datGioNgu(dangNgu = true)

        val tra = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)

        println("SUACHAM_NGU: $tra")
        assertTrue(tra, tra.startsWith("Đã sửa câu 2.33a thành đúng. Đang giờ ngủ nên giữ 1 phút"))
        // So ghi ngay: cau het cho sua, gui lai dung lenh do khong giu them.
        assertTrue(SoCaiBai.dangChoSua(context).isEmpty())
        assertEquals(1, SoCaiBai.phutDaCongHomNay(context))
        assertEquals(listOf(1), CongSang.cacMuc(context).map { it.phut })
        assertTrue(SoCaiBai.loiNhan(context).orEmpty().contains("thì được thêm 1 phút"))
        assertTrue(ThiHanhLenh.suaCham(context, GateStore(context), giaTri).startsWith("Không còn câu nào"))
        assertEquals(1, CongSang.cacMuc(context).size)
    }

    /**
     * Cau Claude cham lan dau (lenh CHAMBAI) khong co dong bai lam nao trong so: Claude chi
     * noi so dong, [vn.huytl.homeworkgate.data.ChamTheoClaude] khong chep dong ra, va so cai
     * khong luu so dong. SuaCham lay so dong bang so dong bai lam trong so, nen ra 0.
     *
     * Theo luat 29/9/2026 cau dung thieu so dong thi 0 phut va CHO BA HUY chon so dong. Duong
     * SUACHAM thi khong cho: no ghi cau la xong voi 0 phut, va cau do khong bao gio con duoc
     * tra gio nua. Test nay giu dieu toi thieu: hoac duoc cong phut, hoac cau van con cho.
     *
     * DO VI CODE CHINH (ghi ngay 29/9/2026), xem bao cao: ThiHanhLenh.suaCham va SuaCham.ghi
     * khong xet ChuanBi.bang.thieuDong.
     */
    @Test
    fun cau_claude_cham_khong_co_dong_bai_lam_thi_khong_bi_ghi_xong_voi_0_phut() {
        ghiCauSai(dong = emptyList())

        val tra = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)

        println("SUACHAM_THIEU_DONG: $tra")
        assertTrue(
            "cau bi ghi xong voi 0 phut: $tra",
            SoCaiBai.phutDaCongHomNay(context) > 0 || SoCaiBai.dangChoSua(context).isNotEmpty()
        )
    }

    @Test
    fun lenh_thieu_danh_sach_thi_khong_lam_gi() {
        assertTrue(ThiHanhLenh.suaCham(context, GateStore(context), null).startsWith("Lệnh thiếu"))
        assertTrue(
            ThiHanhLenh.suaCham(context, GateStore(context), listOf(mapOf("de" to "không có mã")))
                .startsWith("Lệnh thiếu")
        )
    }
}
