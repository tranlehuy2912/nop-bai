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
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Toan hai phan Dai so va Hinh hoc (Ba Huy chot 30/9/2026), bai xen nhau trong sach. Tu 2/10/2026
 * bai da hoc la mot tap bai cua ca mon con tu danh dau ([HocToi]); moc cu "hoc toi Bai N" cua
 * tung phan tu chuyen thanh tap bai. Bo the Toan van mot bo, cat theo dung cac bai da danh dau.
 *
 * Tra lai bai da hoc that tren may sau moi test.
 */
@RunWith(AndroidJUnit4::class)
class PhanToanTest {

    private lateinit var context: Context
    private var mocCu: Map<String, Set<Int>?> = emptyMap()

    private fun ten(ma: String, so: Int) =
        PhanHoc.cacBai(context, PhanHoc.theoMa(ma)!!).first { PhanHoc.soBai(it) == so }

    /** Ghi thang khoa cu (truoc 2/10/2026), nhu may dang chay ban cu de lai. */
    private fun khoaCu(khoa: String, gia: Any?) {
        val ed = Prefs.get(context).raw().edit()
        when (gia) {
            null -> ed.remove(khoa)
            is Int -> ed.putInt(khoa, gia)
            else -> ed.putString(khoa, gia.toString())
        }
        ed.commit()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        BoThe.napNeuCan(context)
        mocCu = MocThu.luu(context)
    }

    @After
    fun tearDown() {
        khoaCu("hoc_toi_the_toan8ct", null)
        MocThu.tra(context, mocCu)
    }

    @Test
    fun hai_phan_chia_theo_chuong_thong_ke_xac_suat_theo_dai_so() {
        val ds = PhanHoc.theoMa("toan8ds")!!
        val hh = PhanHoc.theoMa("toan8hh")!!
        assertEquals(ds, PhanHoc.cuaBai("Toán", 19))
        assertEquals(ds, PhanHoc.cuaBai("Toán", 31))
        assertEquals(hh, PhanHoc.cuaBai("Toán", 12))
        assertEquals(hh, PhanHoc.cuaBai("Toán", 35))
        assertEquals(listOf(ds, hh), PhanHoc.cuaBoThe("toan8ct"))
    }

    /** Moc cu cua tung phan tu chuyen: danh dau san moi bai tu bai dau toi bai cu (Ba Huy chon). */
    @Test
    fun moc_cu_tu_chuyen_thanh_tap_bai() {
        HocToi.xoa(context, PhanHoc.TOAN)
        khoaCu("hoc_toi_the_toan8ds", ten("toan8ds", 9))
        khoaCu("hoc_toi_the_toan8hh", ten("toan8hh", 12))
        assertEquals(((1..9) + (10..12)).toSet(), HocToi.daHoc(context, PhanHoc.TOAN))
        // Doc mot lan la da ghi khoa moi: bo khoa cu di van con nguyen.
        khoaCu("hoc_toi_the_toan8ds", null)
        khoaCu("hoc_toi_the_toan8hh", null)
        assertEquals(((1..9) + (10..12)).toSet(), HocToi.daHoc(context, PhanHoc.TOAN))

        HocToi.xoa(context, PhanHoc.TIENG_ANH)
        khoaCu("hoc_toi_tu_anh8", 3)
        assertEquals(setOf(1, 2, 3), HocToi.daHoc(context, PhanHoc.TIENG_ANH))
        HocToi.xoa(context, PhanHoc.TIENG_ANH)
        khoaCu("hoc_toi_tu_anh8", 0)
        assertEquals("chua hoc Unit nao la tap rong", emptySet<Int>(), HocToi.daHoc(context, PhanHoc.TIENG_ANH))
    }

    /** Moc gop "toan8ct" bo tu 30/9/2026 khong duoc doc lai: con phai chon. */
    @Test
    fun moc_gop_cu_khong_con_duoc_doc() {
        HocToi.xoa(context, PhanHoc.TOAN)
        khoaCu("hoc_toi_the_toan8ct", "Bài 9. Phân tích đa thức thành nhân tử")
        assertTrue(PhanHoc.chuaChon(context, PhanHoc.TOAN))
        assertNull(PhanHoc.baiDaHoc(context, PhanHoc.TOAN))
        assertNull(BoThe.mocCua(context, "toan8ct"))
    }

    @Test
    fun hai_phan_di_song_song_trong_mot_tap_bai() {
        MocThu.datPhan(context, "toan8ds", 9)
        MocThu.datPhan(context, "toan8hh", 12)
        assertEquals(((1..9) + (10..12)).toSet(), PhanHoc.baiDaHoc(context, PhanHoc.TOAN))
        assertEquals((10..12).toSet(), PhanHoc.baiDaHocCuaPhan(context, PhanHoc.theoMa("toan8hh")!!))
        assertEquals(12, PhanHoc.mocCuaPhan(context, PhanHoc.theoMa("toan8hh")!!))

        MocThu.datPhan(context, "toan8ds", 22)
        assertEquals(((1..9) + (18..22) + (10..12)).toSet(), PhanHoc.baiDaHoc(context, PhanHoc.TOAN))
        assertEquals(22, PhanHoc.mocCuaPhan(context, PhanHoc.theoMa("toan8ds")!!))
    }

    @Test
    fun bo_the_toan_cat_theo_bai_da_danh_dau() {
        MocThu.datPhan(context, "toan8ds", 6)
        MocThu.datPhan(context, "toan8hh", 11)
        val moc = BoThe.mocCua(context, "toan8ct")!!
        val so = moc.chiBai!!.mapNotNull { PhanHoc.soBai(it) }.toSet()
        assertTrue("bai ngoai phan da hoc: $so", so.all { it in 1..6 || it in 10..11 })
        assertTrue("thieu bai hinh: $so", so.any { it in 10..11 })

        val kho = KhoBai.get(context)
        val the = kho.cacTheDenLuot("toan8ct", 999, bayGio = Long.MAX_VALUE / 2, chiBai = moc.chiBai)
        assertTrue(the.isNotEmpty())
        assertTrue(the.all { it.bai in moc.chiBai!! })

        // Chua hoc bai nao: bo rong, khong the nao lot qua.
        MocThu.datToan(context, 0)
        assertTrue(BoThe.mocCua(context, "toan8ct")!!.rong)
        assertTrue(!kho.conTheDenLuot("toan8ct", chiBai = BoThe.mocCua(context, "toan8ct")!!.chiBai))
    }

    /** Cau muc on tap chuong khong co so bai: roi vao phan theo so chuong. */
    @Test
    fun cau_on_tap_chuong_theo_phan_cua_chuong() {
        val kho = KhoBai.get(context)
        val onTap = kho.cacCauCuaNguon("sbttoan8t1").filter { PhanHoc.soBai(it.bai) == null }
        assertTrue(onTap.isNotEmpty())
        onTap.forEach { c ->
            val ch = PhanHoc.soChuong(c.chuong)
            val p = PhanHoc.phanCuaCau(c)
            assertTrue("${c.id} chuong ${c.chuong}", p != null && ch in p.cacChuong)
        }
        assertEquals(3, PhanHoc.soChuong("Chương III. Tứ giác"))
        assertEquals(9, PhanHoc.soChuong("Chương IX. Tam giác đồng dạng"))
        assertEquals(8, PhanHoc.soChuong("Chương VIII. Mở đầu về tính xác suất của biến cố"))
    }
}
