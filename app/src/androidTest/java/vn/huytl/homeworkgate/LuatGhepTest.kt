package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Luat sao cua bai lam tren may ([LuatGhep], Ba Huy chot ngay 29/9/2026): sai mot lan mat
 * mot sao, het sao thu them mot lan roi hien loi giai, lam lai mo sau 24 gio va chi cong
 * phan hon lan tot nhat, phan vuot tran vao quy gio choi.
 *
 * Ham thuan, khong doc kho hay prefs, nen test khong can don dep gi.
 */
@RunWith(AndroidJUnit4::class)
class LuatGhepTest {

    private val gio = 60 * 60_000L

    private fun luot(
        vong: Int = 0,
        dung: Boolean = true,
        sao: Int = 3,
        lanSai: Int = 0,
        luc: Long = 0L,
        trenMay: Boolean = true
    ) = TraLoi(
        cauId = "thu:1", mon = "Toán", ma = "1", de = "", ketQua = "", dung = dung, phut = 0,
        nhanXet = "", luc = luc, trenMay = trenMay, sao = sao, saoToiDa = 3, vong = vong, lanSai = lanSai
    )

    // ------------------------------------------------------------------ mot luot

    @Test
    fun dung_ngay_lan_dau_thi_du_sao() {
        val l = LuatGhep.kiem(LuatGhep.Luot(3), soSai = 0)
        assertTrue(l.xong && l.dung)
        assertFalse(l.botNhieu)
        assertEquals(3, LuatGhep.saoKhiXong(l))
    }

    @Test
    fun sai_mot_lan_mat_mot_sao_va_bot_phim_nhieu() {
        var l = LuatGhep.kiem(LuatGhep.Luot(3), soSai = 1)
        assertFalse(l.xong)
        assertEquals(2, l.sao)
        assertEquals(1, l.lanSai)
        assertTrue(l.botNhieu)
        assertFalse(l.thuThem)

        l = LuatGhep.kiem(l, soSai = 0)
        assertTrue(l.dung)
        assertEquals(2, LuatGhep.saoKhiXong(l))
    }

    @Test
    fun cau_nhieu_o_moi_o_sai_mat_mot_sao() {
        assertEquals(3, LuatGhep.kiem(LuatGhep.Luot(5), soSai = 2).sao)
        // Sai nhieu o hon so sao con lai thi ve 0, khong am, va con duoc thu them.
        val het = LuatGhep.kiem(LuatGhep.Luot(2), soSai = 5)
        assertEquals(0, het.sao)
        assertTrue(het.thuThem)
    }

    @Test
    fun het_sao_duoc_thu_them_mot_lan_roi_hien_loi_giai() {
        val het = LuatGhep.kiem(LuatGhep.Luot(1), soSai = 1)
        assertTrue(het.thuThem)
        assertFalse(het.xong)
        assertEquals(0, het.sao)

        // Lan thu them dung: xong, nhung 0 phut.
        val dungLanThem = LuatGhep.kiem(het, soSai = 0)
        assertTrue(dungLanThem.xong && dungLanThem.dung)
        assertEquals(0, LuatGhep.saoKhiXong(dungLanThem))

        // Lan thu them van sai: xong, hien loi giai.
        val saiLanThem = LuatGhep.kiem(het, soSai = 1)
        assertTrue(saiLanThem.xong)
        assertFalse(saiLanThem.dung)
        assertTrue(saiLanThem.hienLoiGiai)
        assertEquals(2, saiLanThem.lanSai)
        assertEquals(0, LuatGhep.saoKhiXong(saiLanThem))
    }

    @Test
    fun luot_da_xong_thi_bam_them_khong_doi_gi() {
        val xong = LuatGhep.kiem(LuatGhep.Luot(3), soSai = 0)
        assertEquals(xong, LuatGhep.kiem(xong, soSai = 1))
    }

    // ------------------------------------------------------------ vong va lam lai

    @Test
    fun tinh_trang_doc_vong_gan_nhat_va_lan_tot_nhat_cua_vong_do() {
        assertEquals(-1, LuatGhep.tinhTrang(emptyList()).vong)

        val t = LuatGhep.tinhTrang(
            listOf(
                luot(vong = 0, sao = 1, lanSai = 2, luc = 1 * gio),
                luot(vong = 0, sao = 2, lanSai = 1, luc = 30 * gio),
                luot(vong = 1, sao = 2, lanSai = 1, luc = 100 * gio),
                // Lan chup anh khong phai luot tren may: khong tinh.
                luot(vong = 5, sao = 3, luc = 200 * gio, trenMay = false)
            )
        )
        assertEquals(1, t.vong)
        assertEquals(2, t.totNhat)
        assertEquals(100 * gio, t.lanCuoi)
        assertTrue(t.tungSai)

        // Dung ngay tu dau thi chua tung sai: khong vao lich on.
        assertFalse(LuatGhep.tinhTrang(listOf(luot(sao = 3))).tungSai)
    }

    @Test
    fun lam_lai_mo_sau_24_gio_toi_khi_du_sao() {
        val thieu = LuatGhep.tinhTrang(listOf(luot(sao = 1, lanSai = 2, luc = 0L)))
        assertFalse(LuatGhep.moLamLai(thieu, 3, 23 * gio))
        assertEquals(gio, LuatGhep.conChoMs(thieu, 3, 23 * gio))
        assertTrue(LuatGhep.moLamLai(thieu, 3, 24 * gio))
        assertEquals(0L, LuatGhep.conChoMs(thieu, 3, 24 * gio))

        // Du sao roi thi khong mo nua.
        val du = LuatGhep.tinhTrang(listOf(luot(sao = 3, luc = 0L)))
        assertFalse(LuatGhep.moLamLai(du, 3, 1000 * gio))
        assertEquals(0L, LuatGhep.conChoMs(du, 3, 1 * gio))

        // Sai het ca lan thu them: lan tot nhat la 0 sao, 24 gio sau van mo lai.
        val sai = LuatGhep.tinhTrang(listOf(luot(dung = false, sao = 0, lanSai = 2, luc = 0L)))
        assertEquals(0, sai.totNhat)
        assertTrue(LuatGhep.moLamLai(sai, 3, 24 * gio))

        // Chua lam lan nao thi khong phai lam lai.
        assertFalse(LuatGhep.moLamLai(LuatGhep.tinhTrang(emptyList()), 3, 1000 * gio))
    }

    @Test
    fun lam_lai_chi_cong_phan_hon_lan_tot_nhat() {
        assertEquals(3, LuatGhep.phutCong(saoVuaDat = 3, totNhatCu = 0))
        assertEquals(2, LuatGhep.phutCong(saoVuaDat = 3, totNhatCu = 1))
        assertEquals(0, LuatGhep.phutCong(saoVuaDat = 1, totNhatCu = 2))
    }

    // ------------------------------------------------------------------ tran va quy

    @Test
    fun chia_tran_phan_vuot_vao_quy() {
        assertEquals(5 to 0, LuatGhep.chiaTran(lamRa = 5, daDung = 10, tran = 90))
        assertEquals(2 to 3, LuatGhep.chiaTran(lamRa = 5, daDung = 88, tran = 90))
        assertEquals(0 to 5, LuatGhep.chiaTran(lamRa = 5, daDung = 95, tran = 90))
        assertEquals(0 to 0, LuatGhep.chiaTran(lamRa = 0, daDung = 10, tran = 90))
    }
}
