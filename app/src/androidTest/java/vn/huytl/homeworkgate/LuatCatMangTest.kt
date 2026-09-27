package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LuatCatMang

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
}
