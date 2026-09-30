package vn.huytl.homeworkgate

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.ui.HangSao
import vn.huytl.homeworkgate.ui.HangSao.O.CON
import vn.huytl.homeworkgate.ui.HangSao.O.DUOC
import vn.huytl.homeworkgate.ui.HangSao.O.MAT

/**
 * Ba dang sao (Ba Huy chot 30/9/2026): rong la con co the duoc, xam la mat vi sai, vang la da
 * duoc. Ham thuan, khong doc kho; chi [HangSao.chu] can Context de lay mau.
 */
@RunWith(AndroidJUnit4::class)
class HangSaoTest {

    private fun sai(l: LuatGhep.Luot, soSai: Int = 1) = LuatGhep.kiem(l, soSai)
    private fun dung(l: LuatGhep.Luot) = LuatGhep.kiem(l, 0)

    @Test
    fun vao_cau_rong_het_sai_thi_xam_dung_thi_vang() {
        val dau = LuatGhep.Luot(3)
        assertEquals(listOf(CON, CON, CON), HangSao.cacO(dau))
        val sai1 = sai(dau)
        assertEquals(listOf(CON, CON, MAT), HangSao.cacO(sai1))
        assertEquals(listOf(DUOC, DUOC, MAT), HangSao.cacO(dung(sai1)))
        assertEquals(listOf(DUOC, DUOC, DUOC), HangSao.cacO(dung(dau)))
    }

    @Test
    fun cau_nhieu_o_sai_hai_o_thi_xam_hai_o() {
        assertEquals(listOf(CON, CON, MAT, MAT), HangSao.cacO(sai(LuatGhep.Luot(4), soSai = 2)))
    }

    @Test
    fun het_sao_thu_them_va_xong_sai_thi_xam_het() {
        val het = sai(LuatGhep.Luot(1))
        assertEquals(true, het.thuThem)
        assertEquals(listOf(MAT), HangSao.cacO(het))
        // Thu them van sai: hien loi giai, ca hang xam.
        assertEquals(listOf(MAT), HangSao.cacO(sai(het)))
        // Thu them dung: dung nhung 0 sao, khong o nao vang.
        assertEquals(listOf(MAT), HangSao.cacO(dung(het)))
    }

    @Test
    fun dong_lan_truoc_vang_bang_sao_da_dat_con_lai_xam() {
        assertEquals(listOf(DUOC, DUOC, MAT), HangSao.cacOLanTruoc(2, 3))
        assertEquals(listOf(MAT, MAT), HangSao.cacOLanTruoc(0, 2))
        assertEquals(listOf(DUOC, DUOC), HangSao.cacOLanTruoc(5, 2))
    }

    @Test
    fun chu_moi_sao_mot_mau_sao_rong_la_ngoi_sao_rong() {
        val ct = InstrumentationRegistry.getInstrumentation().targetContext
        val chu = HangSao.chu(ct, listOf(DUOC, CON, MAT))
        assertEquals("★☆★", chu.toString())
        val mau = (chu as Spanned).getSpans(0, chu.length, ForegroundColorSpan::class.java)
        assertEquals(3, mau.size)
        assertEquals(3, mau.map { it.foregroundColor }.toSet().size)
        assertEquals("Lần trước ★★★", HangSao.chuLanTruoc(ct, 2, 3).toString())
    }
}
