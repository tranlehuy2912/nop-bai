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
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Toan hai phan Dai so va Hinh hoc (Ba Huy chot 30/9/2026): hai moc rieng, bai xen nhau trong
 * sach, bo the Toan van mot bo va cat theo ca hai moc.
 *
 * Tra lai moc that tren may sau moi test.
 */
@RunWith(AndroidJUnit4::class)
class PhanToanTest {

    private lateinit var context: Context
    private var mocCu: Map<String, String?> = emptyMap()
    private var mocGopCu: String? = null

    private fun ten(ma: String, so: Int) =
        PhanHoc.cacBai(context, PhanHoc.theoMa(ma)!!).first { PhanHoc.soBai(it) == so }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        BoThe.napNeuCan(context)
        mocCu = MocToanThu.luu(context)
        mocGopCu = HocToi.baiCua(context, "toan8ct")
    }

    @After
    fun tearDown() {
        MocToanThu.tra(context, mocCu)
        HocToi.xoa(context, "toan8ct")
        mocGopCu?.let { HocToi.ghiBai(context, "toan8ct", it) }
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

    @Test
    fun moc_gop_cu_khong_con_duoc_doc_con_phai_chon_lai_ca_hai() {
        MocToanThu.dat(context, null)
        HocToi.ghiBai(context, "toan8ct", "Bài 9. Phân tích đa thức thành nhân tử")
        assertEquals(2, PhanHoc.chuaChon(context, "Toán").size)
        assertNull(PhanHoc.baiDaHoc(context, "Toán"))
        assertNull(BoThe.mocCua(context, "toan8ct"))
    }

    @Test
    fun hai_moc_di_song_song_gop_thanh_bai_da_hoc_cua_mon() {
        HocToi.ghiBai(context, "toan8ds", ten("toan8ds", 9))
        HocToi.ghiBai(context, "toan8hh", ten("toan8hh", 12))
        assertEquals(((1..9) + (10..12)).toSet(), PhanHoc.baiDaHoc(context, "Toán"))

        HocToi.ghiBai(context, "toan8ds", ten("toan8ds", 22))
        assertEquals(((1..9) + (18..22) + (10..12)).toSet(), PhanHoc.baiDaHoc(context, "Toán"))
    }

    @Test
    fun bo_the_toan_cat_theo_ca_hai_moc() {
        HocToi.ghiBai(context, "toan8ds", ten("toan8ds", 6))
        HocToi.ghiBai(context, "toan8hh", ten("toan8hh", 11))
        val moc = BoThe.mocCua(context, "toan8ct")!!
        val so = moc.chiBai!!.mapNotNull { PhanHoc.soBai(it) }.toSet()
        assertTrue("bai ngoai moc: $so", so.all { it in 1..6 || it in 10..11 })
        assertTrue("thieu bai hinh: $so", so.any { it in 10..11 })

        val kho = KhoBai.get(context)
        val the = kho.cacTheDenLuot("toan8ct", 999, bayGio = Long.MAX_VALUE / 2, chiBai = moc.chiBai)
        assertTrue(the.isNotEmpty())
        assertTrue(the.all { it.bai in moc.chiBai!! })

        // Ca hai chua hoc bai nao: bo rong, khong the nao lot qua.
        MocToanThu.dat(context, 0)
        assertTrue(BoThe.mocCua(context, "toan8ct")!!.rong)
        assertTrue(!kho.conTheDenLuot("toan8ct", chiBai = BoThe.mocCua(context, "toan8ct")!!.chiBai))
    }
}
