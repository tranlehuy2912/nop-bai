package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.CongSang
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
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
 * Don dep sau khi chay: xoa so, va dong phieu gio vua cap. Khong xoa prefs. Tran phut
 * ngay nang tam len cho du cho, vi may thu chay bo test nhieu lan mot ngay thi tran con
 * it, ma tu 27/9/2026 cau tra loi noi dung so phut con lai trong tran; xong thi tra lai.
 */
@RunWith(AndroidJUnit4::class)
class SuaChamLenhTest {

    private lateinit var context: Context
    private val de = "Rút gọn biểu thức (2x − 5y)(2x + 5y) + (2x + 5y)^2."
    private var tranCu = 0
    private var nguCu = 0
    private var dayCu = 0

    // Dung kieu Firestore tra ve cho mot mang cac map.
    private val giaTri: List<Map<String, Any>> = listOf(mapOf("ma" to "2.33a", "de" to de))

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        SoCaiBai.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        tranCu = Prefs.get(context).tranPhutMoiNgay
        Prefs.get(context).tranPhutMoiNgay = GateStore(context).phutDaDuyetHomNay() + 100
        nguCu = Prefs.get(context).hardStopMinuteOfDay
        dayCu = Prefs.get(context).gioDayMinuteOfDay
        datGioNgu(dangNgu = false)
        CongSang.xoaHet(context)
    }

    @After
    fun tearDown() {
        SoCaiBai.xoaHet(context)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        Prefs.get(context).tranPhutMoiNgay = tranCu
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

    /** Cau 2.33a may bao sai, dang cho sua. */
    private fun ghiCauSai() {
        SoCaiBai.ghi(
            context,
            listOf(
                CauCham(
                    ma = "2.33a", de = de, ketQua = "8x^2 + 20xy", dung = false, soDong = 1,
                    baiLam = listOf("a) 8x^2 + 20xy"), cauId = "sachthu:2.33a", mon = "Toán",
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
        // Mot dong lam bai, cau nho toi thieu 4 phut tu 27/9/2026 (truoc do 2).
        assertTrue(tra, tra.startsWith("Đã sửa câu 2.33a thành đúng, cộng 4 phút"))
        assertTrue(SoCaiBai.dangChoSua(context).isEmpty())
        assertEquals(4, SoCaiBai.phutDaCongHomNay(context))
        assertTrue(SoCaiBai.loiNhan(context).orEmpty().contains("câu 2.33a Lê Hòa làm đúng rồi"))

        // Gui lai dung lenh do: khong cong them phut nao.
        val lan2 = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)
        assertTrue(lan2, lan2.startsWith("Không còn câu nào"))
        assertEquals(4, SoCaiBai.phutDaCongHomNay(context))
    }

    @Test
    fun tran_ngay_cat_bot_thi_tra_loi_nhat_ky_va_so_noi_so_phut_that() {
        ghiCauSai()
        // Cau nay gia 4 phut, ma hom nay chi con 2 phut trong tran. Tran thap nhat la 15
        // phut, nen dung truoc cho du 13 phut roi moi dat tran.
        val gate = GateStore(context)
        if (gate.phutDaDuyetHomNay() < 13) {
            gate.approve(wantedMinutes = 13 - gate.phutDaDuyetHomNay(), useQuota = true)
            gate.endSession(EndReason.PARENT_REVOKED)
        }
        Prefs.get(context).tranPhutMoiNgay = gate.phutDaDuyetHomNay() + 2

        val tra = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)

        println("SUACHAM_TRAN: $tra")
        assertTrue(tra, tra.startsWith("Đã sửa câu 2.33a thành đúng, cộng 2 phút. Hôm nay chỉ còn 2 phút"))
        assertEquals(2, SoCaiBai.phutDaCongHomNay(context))
        assertTrue(SoCaiBai.loiNhan(context).orEmpty().contains("Được thêm 2 phút."))
        assertEquals(0, GateStore(context).phutConLaiHomNay())
    }

    @Test
    fun trong_gio_ngu_thi_ghi_so_ngay_va_giu_phut_toi_luc_het_gio_ngu() {
        ghiCauSai()
        datGioNgu(dangNgu = true)

        val tra = ThiHanhLenh.suaCham(context, GateStore(context), giaTri)

        println("SUACHAM_NGU: $tra")
        assertTrue(tra, tra.startsWith("Đã sửa câu 2.33a thành đúng. Đang giờ ngủ nên giữ 4 phút"))
        // So ghi ngay: cau het cho sua, gui lai dung lenh do khong giu them.
        assertTrue(SoCaiBai.dangChoSua(context).isEmpty())
        assertEquals(4, SoCaiBai.phutDaCongHomNay(context))
        assertEquals(listOf(4), CongSang.cacMuc(context).map { it.phut })
        assertTrue(SoCaiBai.loiNhan(context).orEmpty().contains("thì được thêm 4 phút"))
        assertTrue(ThiHanhLenh.suaCham(context, GateStore(context), giaTri).startsWith("Không còn câu nào"))
        assertEquals(1, CongSang.cacMuc(context).size)
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
