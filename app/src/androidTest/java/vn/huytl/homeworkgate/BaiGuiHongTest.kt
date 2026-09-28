package vn.huytl.homeworkgate

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
import vn.huytl.homeworkgate.data.BaiGuiHong
import vn.huytl.homeworkgate.data.CaptureStage
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.telegram.HomeworkSender
import vn.huytl.homeworkgate.telegram.TelegramClient
import java.io.File
import java.net.SocketTimeoutException

/**
 * Cac lan nop gui khong xong: giu anh de gui lai, dem so lan hong, bo khi qua ngay hay khi
 * da nop lai cung bai, khong de hai lan gui chong nhau, va dong nhat ky noi anh nao da len
 * Telegram. Xem [BaiGuiHong].
 */
@RunWith(AndroidJUnit4::class)
class BaiGuiHongTest {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val gio = 60 * 60_000L

    @Before
    fun sach() = donHet()

    @After
    fun don() = donHet()

    /** Xoa thang khoa trong prefs va anh thu: goi cacLan o day se ghi nhat ky "hôm trước". */
    private fun donHet() {
        Prefs.get(context).raw().edit().remove("bai_gui_hong").commit()
        context.cacheDir.listFiles { f -> f.name.startsWith("thu_gui_hong_") }?.forEach { it.delete() }
    }

    private fun anh(ten: String): File =
        File(context.cacheDir, "thu_gui_hong_$ten.jpg").apply { writeText("anh $ten") }

    private fun giai(vararg ten: String) = mapOf(CaptureStage.BAI_GIAI to ten.map { anh(it) })

    /** Giua ngay hom nay, de lui toi vai tieng van cung ngay. */
    private fun giuaNgay(): Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 12)
        set(java.util.Calendar.MINUTE, 0)
    }.timeInMillis

    @Test
    fun giu_roi_lay_lai_du_anh_theo_thu_tu_buoc() {
        val now = giuaNgay()
        val de = anh("de1")
        val giai = listOf(anh("giai1"), anh("giai2"))
        BaiGuiHong.giu(
            context, mapOf(CaptureStage.BAI_GIAI to giai, CaptureStage.DE_BAI to listOf(de)),
            "{\"mon\":\"Toán\"}", lucNop = now - gio, now = now
        )

        val ds = BaiGuiHong.cacLan(context, now)
        assertEquals(1, ds.size)
        val lan = ds[0]
        assertEquals(listOf(CaptureStage.DE_BAI, CaptureStage.BAI_GIAI), lan.anh.keys.toList())
        assertEquals(giai.map { it.absolutePath }, lan.anh.getValue(CaptureStage.BAI_GIAI).map { it.absolutePath })
        assertEquals("{\"mon\":\"Toán\"}", lan.pham)
        assertEquals(now - gio, lan.lucNop)
        assertEquals(1, lan.soLan)
        assertFalse(BaiGuiHong.dangGui(lan, now))
    }

    @Test
    fun gui_lai_van_hong_thi_cong_mot_lan_giu_ma_va_luc_nop_dau() {
        val now = giuaNgay()
        val bo = giai("g1")
        val dau = BaiGuiHong.giu(context, bo, null, lucNop = now - gio, now = now)
        BaiGuiHong.danhDauBamGui(context, dau.ma, now + 1_000)

        BaiGuiHong.giu(context, bo, null, lucNop = now + 5_000, now = now + 5_000)

        val ds = BaiGuiHong.cacLan(context, now + 6_000)
        assertEquals(1, ds.size)
        assertEquals(dau.ma, ds[0].ma)
        assertEquals(2, ds[0].soLan)
        assertEquals(now - gio, ds[0].lucNop)
        // Hong xong thi thoi "dang gui", khong doi het khoang cho service.
        assertFalse(BaiGuiHong.dangGui(ds[0], now + 6_000))
    }

    /** Hai lan nop khac nhau cung hong: giu ca hai, khong xoa anh cua lan truoc. */
    @Test
    fun hai_lan_nop_khac_nhau_hong_thi_giu_ca_hai() {
        val now = giuaNgay()
        val a = giai("a")
        val b = giai("b")
        BaiGuiHong.giu(context, a, null, now - gio, now - gio)
        BaiGuiHong.giu(context, b, null, now, now)

        val ds = BaiGuiHong.cacLan(context, now)
        assertEquals(2, ds.size)
        assertTrue(a.values.flatten().all { it.exists() })
        assertTrue(b.values.flatten().all { it.exists() })
    }

    @Test
    fun giu_toi_da_nam_lan_bo_lan_cu_nhat_va_ghi_nhat_ky() {
        val now = giuaNgay()
        val cuNhat = giai("x0")
        BaiGuiHong.giu(context, cuNhat, null, now - 6 * 60_000, now - 6 * 60_000)
        (1..5).forEach { BaiGuiHong.giu(context, giai("x$it"), null, now - (6 - it) * 60_000L, now) }

        assertEquals(5, BaiGuiHong.cacLan(context, now).size)
        assertFalse(cuNhat.values.flatten().any { it.exists() })
        assertTrue(DayLog.today(context).contains("giữ tối đa 5 lần"))
    }

    @Test
    fun gui_lai_xong_thi_go_lan_do_nhung_de_anh_cho_service_xoa() {
        val now = giuaNgay()
        val bo = giai("a", "b")
        val khac = giai("khac")
        BaiGuiHong.giu(context, bo, null, now, now)
        BaiGuiHong.giu(context, khac, null, now, now)

        val xong = BaiGuiHong.xong(context, bo.values.flatten().reversed(), null, now)

        assertNotNull(xong.guiLai)
        assertEquals(listOf(khac.values.flatten().map { it.absolutePath }),
            BaiGuiHong.cacLan(context, now).map { l -> l.cacTep().map { it.absolutePath } })
        assertTrue(bo.values.flatten().all { it.exists() })
    }

    /**
     * Chup lai dung bai do va gui duoc: lan hong cu khong con gi de gui, bo di. Nhu 21:13 va
     * 21:15 ngay 28/9/2026 neu 21:15 la chup lai bai 21:13 (that ra khong phai: 21:13 la nop
     * lai cau sai, 21:15 la bai moi - xem test duoi).
     */
    @Test
    fun nop_lai_cung_bai_thanh_cong_thi_bo_lan_hong() {
        val now = giuaNgay()
        val cu = giai("cu")
        val pham = PhamVi(mon = "Toán", nguon = "sgk", cauIds = listOf("c1", "c2"))
        BaiGuiHong.giu(context, cu, pham.sangJson(), now - gio, now - gio)

        val xong = BaiGuiHong.xong(context, listOf(anh("moi")), pham.copy(cauIds = listOf("c2", "c1")), now)

        assertNull(xong.guiLai)
        assertEquals(1, xong.boDi.size)
        assertTrue(BaiGuiHong.cacLan(context, now).isEmpty())
        assertFalse(cu.values.flatten().any { it.exists() })
        assertTrue(DayLog.today(context).contains("đã nộp lại lúc"))
    }

    /** 21:13 ngay 28/9/2026: nop lai cau sai hong, 21:15 nop bai moi. Bai 21:13 phai con. */
    @Test
    fun bai_moi_khac_bai_thi_van_giu_lan_nop_lai_cau_sai() {
        val now = giuaNgay()
        val sua = giai("sua")
        val phamSua = PhamVi(mon = "KHTN", nguon = "sgk", cauIds = listOf("k1"), suaBai = "baiCu")
        BaiGuiHong.giu(context, sua, phamSua.sangJson(), now - 2 * 60_000, now - 2 * 60_000)

        val phamMoi = PhamVi(mon = "KHTN", nguon = "sgk", cauIds = listOf("k1", "k2"))
        val xong = BaiGuiHong.xong(context, listOf(anh("moi")), phamMoi, now)

        assertTrue(xong.boDi.isEmpty())
        assertEquals(1, BaiGuiHong.cacLan(context, now).size)
        assertTrue(sua.values.flatten().all { it.exists() })
    }

    @Test
    fun cung_bai_theo_ma_bai_sua_de_hay_danh_sach_cau() {
        val toan = PhamVi(mon = "Toán", cauIds = listOf("a", "b"))
        assertTrue(BaiGuiHong.cungBai(toan, toan.copy(cauIds = listOf("b", "a"))))
        assertFalse(BaiGuiHong.cungBai(toan, toan.copy(cauIds = listOf("a"))))
        assertFalse(BaiGuiHong.cungBai(toan, toan.copy(onTap = true)))
        assertFalse(BaiGuiHong.cungBai(toan, null))
        assertFalse(BaiGuiHong.cungBai(PhamVi(mon = "Toán"), PhamVi(mon = "Toán")))
        assertTrue(BaiGuiHong.cungBai(toan.copy(suaBai = "x"), PhamVi(mon = "Toán", suaBai = "x")))
        assertFalse(BaiGuiHong.cungBai(toan.copy(suaBai = "x"), toan))
        assertTrue(BaiGuiHong.cungBai(PhamVi(mon = "Toán", giaiDe = "d1"), PhamVi(mon = "Toán", giaiDe = "d1")))
        assertFalse(BaiGuiHong.cungBai(PhamVi(mon = "Toán", giaiDe = "d1"), toan))
    }

    @Test
    fun qua_ngay_thi_bo_ca_anh_va_ghi_nhat_ky() {
        val now = giuaNgay()
        val homQua = giai("hom_qua")
        BaiGuiHong.giu(context, homQua, null, now - 24 * gio, now - 24 * gio)

        assertTrue(BaiGuiHong.cacLan(context, now).isEmpty())
        assertFalse(homQua.values.flatten().any { it.exists() })
        assertTrue(DayLog.today(context).contains("hôm trước"))
    }

    /** Dang trong tay service thi la dang gui; vua bam Gui lai thi cho service 30 giay. */
    @Test
    fun dang_gui_theo_service_va_theo_luc_bam() {
        val now = giuaNgay()
        val bo = giai("dg")
        val lan = BaiGuiHong.giu(context, bo, null, now, now)
        val tep = bo.values.flatten()

        BaiGuiHong.nhanGui(tep)
        assertTrue(BaiGuiHong.dangGui(BaiGuiHong.lay(context, lan.ma, now)!!, now + 60 * 60_000))
        BaiGuiHong.traGui(context, tep)
        assertFalse(BaiGuiHong.dangGui(BaiGuiHong.lay(context, lan.ma, now)!!, now))

        BaiGuiHong.danhDauBamGui(context, lan.ma, now)
        val daBam = BaiGuiHong.lay(context, lan.ma, now)!!
        assertTrue(BaiGuiHong.dangGui(daBam, now + 29_000))
        assertFalse(BaiGuiHong.dangGui(daBam, now + 31_000))
        assertEquals(10_000L, BaiGuiHong.conChoService(daBam, now + 20_000))
    }

    @Test
    fun lam_gon_bo_anh_de_bai_da_mat_mat_bai_giai_thi_khong_gui_duoc() {
        val now = giuaNgay()
        val de = anh("de_mat")
        val giai = anh("giai_con")
        val lan = BaiGuiHong.giu(
            context, mapOf(CaptureStage.DE_BAI to listOf(de), CaptureStage.BAI_GIAI to listOf(giai)),
            null, now, now
        )
        de.delete()

        val gon = BaiGuiHong.lamGon(context, lan.ma)
        assertNotNull(gon)
        assertEquals(listOf(CaptureStage.BAI_GIAI), gon!!.anh.keys.toList())
        // Da luu ban gon: gui lai xong thi khop dung bo anh con lai.
        assertEquals(gon.anh, BaiGuiHong.lay(context, lan.ma, now)!!.anh)

        giai.delete()
        assertNull(BaiGuiHong.lamGon(context, lan.ma))
    }

    @Test
    fun ke_anh_nhu_dong_nop_bai() {
        val ke = BaiGuiHong.keAnh(
            context,
            mapOf(CaptureStage.DE_BAI to listOf(anh("d1")), CaptureStage.BAI_GIAI to listOf(anh("g1"), anh("g2")))
        )
        assertEquals("đề bài 1, bài giải 2", ke)
    }

    /** Dong nhat ky noi ro anh nao da len Telegram, hong o dau, loi goc cua may. */
    @Test
    fun mo_ta_hong_ke_anh_da_len_va_cho_hong() {
        val tinNut = HomeworkSender.GuiHong(
            listOf(CaptureStage.BAI_GIAI), "tin có nút duyệt", SocketTimeoutException("timeout")
        )
        assertEquals(
            "ảnh bài giải đã lên Telegram, hỏng ở tin có nút duyệt: " +
                "Telegram không trả lời kịp (SocketTimeoutException)",
            HomeworkSender.moTaHong(context, tinNut)
        )

        val anhDau = HomeworkSender.GuiHong(
            emptyList(), "ảnh đề bài", TelegramClient.ApiException("Too Many Requests: retry after 7", 429)
        )
        assertEquals(
            "chưa lên ảnh nào, hỏng ở ảnh đề bài: Telegram báo lỗi 429: Too Many Requests: retry after 7",
            HomeworkSender.moTaHong(context, anhDau)
        )
    }
}
