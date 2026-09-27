package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LuatCatMang
import vn.huytl.homeworkgate.data.LuatCatMang.ViecVpn

/**
 * Luat cat mang cac app Ba Huy chon vao danh sach cat mang.
 *
 * App trong danh sach mat mang dung luc no dang bi khoa: het gio choi, gio di hoc,
 * gio ngu. Trong gio choi that thi van co mang.
 */
@RunWith(AndroidJUnit4::class)
class LuatCatMangTest {

    private fun cat(
        gateMo: Boolean = false,
        trongGioNgu: Boolean = false,
        trongGioHoc: Boolean = false,
        moToanBo: Boolean = false,
    ) = LuatCatMang.catMang(
        gateMo = gateMo,
        trongGioNgu = trongGioNgu,
        trongGioHoc = trongGioHoc,
        moToanBo = moToanBo,
    )

    @Test
    fun het_gio_choi_thi_cat() {
        assertTrue(cat(gateMo = false))
    }

    @Test
    fun dang_co_gio_choi_thi_co_mang() {
        assertFalse(cat(gateMo = true))
    }

    @Test
    fun gio_di_hoc_thi_cat_ke_ca_khi_dang_co_gio_choi() {
        assertTrue(cat(gateMo = true, trongGioHoc = true))
    }

    @Test
    fun gio_ngu_thi_cat_ke_ca_khi_con_giu_phieu() {
        assertTrue(cat(gateMo = true, trongGioNgu = true))
    }

    @Test
    fun ba_huy_mo_toan_bo_may_thi_khong_cat() {
        assertFalse(cat(gateMo = false, trongGioHoc = true, moToanBo = true))
        assertFalse(cat(gateMo = false, trongGioNgu = true, moToanBo = true))
    }

    // ------------------------------------------------ viec phai lam voi VPN

    private val yt = setOf("com.google.android.youtube")
    private val ytVaGame = yt + "com.mojang.minecraftpe"

    private fun viec(
        can: Set<String> = yt,
        dangCat: Set<String> = emptySet(),
        biDa: Boolean = false,
        choPhep: Boolean = true,
        dangNghi: Boolean = false,
    ) = LuatCatMang.viecVpn(can, dangCat, biDa, choPhep, dangNghi)

    @Test
    fun khoa_ma_chua_cat_thi_bat() {
        assertEquals(ViecVpn.BAT, viec())
    }

    @Test
    fun het_phai_cat_thi_tat_ke_ca_dang_bi_da() {
        assertEquals(ViecVpn.TAT, viec(can = emptySet(), dangCat = yt))
        assertEquals(ViecVpn.TAT, viec(can = emptySet(), biDa = true))
    }

    @Test
    fun dang_cat_dung_danh_sach_thi_giu() {
        assertEquals(ViecVpn.GIU, viec(dangCat = yt))
    }

    @Test
    fun doi_danh_sach_giua_luc_khoa_thi_dung_lai() {
        assertEquals(ViecVpn.BAT, viec(can = ytVaGame, dangCat = yt))
    }

    @Test
    fun bi_app_khac_chiem_thi_khong_gianh_lai() {
        assertEquals(ViecVpn.GIU, viec(biDa = true))
    }

    @Test
    fun chua_cho_phep_thi_khong_bat() {
        assertEquals(ViecVpn.GIU, viec(choPhep = false))
    }

    @Test
    fun vua_hong_thi_cho_het_khoang_nghi() {
        assertEquals(ViecVpn.GIU, viec(dangNghi = true))
    }
}
