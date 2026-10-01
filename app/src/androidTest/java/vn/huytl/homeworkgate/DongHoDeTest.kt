package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.DongHoDe
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.KhoBai

/**
 * Dong ho Giai de chi chay luc con ngoi o man de (Ba Huy chon ngay 1/10/2026). Xem [DongHoDe].
 *
 * Truoc ngay do man chinh ghi "Đang làm, quá 1078 phút" cho mot de bam Bat dau roi bo do qua
 * dem, vi moi cho deu lay gio that tru luc bat dau.
 *
 * Gio trong test la gio gia truyen vao, khong doi gio may. Don dep: xoa muc dong ho va de thu.
 * Khong xoa prefs, khong xoa so cai.
 */
@RunWith(AndroidJUnit4::class)
class DongHoDeTest {

    private lateinit var context: Context
    private val phut = 60_000L
    private val t0 = System.currentTimeMillis() - 24 * 60 * phut
    private val deDaTao = mutableListOf<String>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @After
    fun tearDown() {
        deDaTao.forEach {
            DongHoDe.xoa(context, it)
            KhoBai.get(context).xoaDe(it)
        }
    }

    /** Mot de gia, chi can ma de va cac moc gio. [trenMay] la de lam bang ban phim ghep. */
    private fun de(ten: String, batDau: Long = 0L, nopLuc: Long = 0L, trenMay: Boolean = false): DeGiai {
        val id = "dongho-thu-$ten-$t0"
        deDaTao += id
        return DeGiai(
            id = id, mon = "Toán", nguon = "thu", loai = GiaiDe.LOAI_TUAN, khoa = "dongho-thu:$id",
            ten = "Bài thử", cauIds = emptyList(), phutGoiY = 20, taoLuc = t0,
            hetHan = t0 + 7 * 24 * 60 * phut, batDau = batDau, nopLuc = nopLuc,
            saoToiDa = if (trenMay) 1 else 0
        )
    }

    @Test
    fun thoat_ra_thi_dong_ho_dung_vao_lai_thi_chay_tiep() {
        val d = de("thoat", batDau = t0)
        DongHoDe.batDau(context, d.id, t0)
        assertEquals(5 * phut, DongHoDe.daLamMs(context, d, t0 + 5 * phut, dangOMan = true))

        // Ra ngoai mot tieng: dong ho dung o 5 phut, ca o man chinh lan o man de.
        DongHoDe.roi(context, d, t0 + 5 * phut)
        assertEquals(5 * phut, DongHoDe.daLamMs(context, d, t0 + 65 * phut))
        assertEquals(5 * phut, DongHoDe.daLamMs(context, d, t0 + 65 * phut, dangOMan = true))

        // Vao lai ba phut: man de dem tiep, man chinh van doc so da cong.
        DongHoDe.vao(context, d, t0 + 65 * phut)
        assertEquals(8 * phut, DongHoDe.daLamMs(context, d, t0 + 68 * phut, dangOMan = true))
        assertEquals(5 * phut, DongHoDe.daLamMs(context, d, t0 + 68 * phut))
        DongHoDe.roi(context, d, t0 + 68 * phut)
        assertEquals(8 * phut, DongHoDe.daLamMs(context, d, t0 + 600 * phut))
    }

    @Test
    fun de_bat_dau_truoc_ban_nay_dang_lam_thi_dem_lai_tu_0_da_nop_thi_giu_cach_cu() {
        // Dang lam do, chua co muc: chua vao lai thi la 0, vao roi thi dem tu luc vao.
        val dangLam = de("cu-dang-lam", batDau = t0)
        assertEquals(0L, DongHoDe.daLamMs(context, dangLam, t0 + 1078 * phut))
        DongHoDe.vao(context, dangLam, t0 + 1078 * phut)
        assertEquals(2 * phut, DongHoDe.daLamMs(context, dangLam, t0 + 1080 * phut, dangOMan = true))

        // Da nop truoc ban nay: gio that tu luc bat dau toi luc nop, nhu dong tom tat cu.
        val daNop = de("cu-da-nop", batDau = t0, nopLuc = t0 + 30 * phut)
        assertEquals(30 * phut, DongHoDe.daLamMs(context, daNop, t0 + 600 * phut))
    }

    @Test
    fun may_sap_luc_dang_o_man_de_thi_bo_doan_do() {
        val d = de("sap", batDau = t0)
        DongHoDe.batDau(context, d.id, t0)
        // Khong ai goi roi(): man chinh khong cong doan dang chay vao.
        assertEquals(0L, DongHoDe.daLamMs(context, d, t0 + 600 * phut))
        // Vao lai: bo doan tu t0, dem tu luc vao.
        DongHoDe.vao(context, d, t0 + 600 * phut)
        assertEquals(2 * phut, DongHoDe.daLamMs(context, d, t0 + 602 * phut, dangOMan = true))
    }

    @Test
    fun nop_bai_chot_dong_ho_va_dong_tom_tat_ghi_luc_ngoi_o_man_de() {
        val kho = KhoBai.get(context)
        var d = de("nop", trenMay = true)
        assertTrue(kho.themDe(d))
        d = GiaiDe.batDau(context, d, t0)
        DongHoDe.roi(context, d, t0 + 5 * phut)
        DongHoDe.vao(context, d, t0 + 65 * phut)

        val kq = GiaiDe.nop(context, d, t0 + 67 * phut)
        val sau = GiaiDe.theoId(context, d.id)
        assertNotNull(sau)
        assertEquals(7 * phut, DongHoDe.daLamMs(context, sau!!, t0 + 600 * phut))
        val tomTat = GiaiDe.tomTat(context, kq.de)
        assertTrue(tomTat, tomTat.contains("làm 7 phút (gợi ý 20)"))
        // Nop roi thi ra vao man de khong doi gi nua.
        DongHoDe.vao(context, sau, t0 + 700 * phut)
        DongHoDe.roi(context, sau, t0 + 800 * phut)
        assertEquals(7 * phut, DongHoDe.daLamMs(context, sau, t0 + 900 * phut))
    }
}
