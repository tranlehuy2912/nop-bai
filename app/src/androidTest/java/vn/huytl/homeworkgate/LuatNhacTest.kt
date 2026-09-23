package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.guard.LuatNhac
import vn.huytl.homeworkgate.guard.XuLyNhac

/**
 * Luat cho tieng phat ra khi con khong ngoi trong app.
 *
 * Duong di dan den bo test nay: Le Hoa mo Spotify trong gio choi, bam tam dung
 * phien de giu lai so phut, roi bam play tu the nhac trong thanh thong bao. Khong
 * co cua so nao de chan, dong ho phien dang dung, ma nhac van chay.
 */
@RunWith(AndroidJUnit4::class)
class LuatNhacTest {

    private fun xet(
        laAppNhac: Boolean = false,
        duocKhiHetGio: Boolean = false,
        hetHanNgay: Boolean = false,
        trongGioNgu: Boolean = false,
        trongGioHoc: Boolean = false,
        congMo: Boolean = false,
    ) = LuatNhac.xet(
        laAppNhac = laAppNhac,
        duocKhiHetGio = duocKhiHetGio,
        hetHanNgay = hetHanNgay,
        trongGioNgu = trongGioNgu,
        trongGioHoc = trongGioHoc,
        congMo = congMo,
    )

    @Test
    fun app_nhac_van_keu_khi_da_het_gio_choi() {
        assertEquals(XuLyNhac.CHO_PHAT, xet(laAppNhac = true))
    }

    @Test
    fun app_nhac_het_so_phut_trong_ngay_thi_im() {
        assertEquals(XuLyNhac.DUNG, xet(laAppNhac = true, hetHanNgay = true))
    }

    @Test
    fun app_nhac_im_qua_gio_di_ngu_du_con_phut() {
        assertEquals(XuLyNhac.DUNG, xet(laAppNhac = true, trongGioNgu = true))
    }

    @Test
    fun app_nhac_im_trong_gio_di_hoc() {
        assertEquals(XuLyNhac.DUNG, xet(laAppNhac = true, trongGioHoc = true))
    }

    /** Gio di hoc xet truoc ca gio choi, giong het phan chan app. */
    @Test
    fun gio_di_hoc_thi_im_ke_ca_khi_dang_co_gio_choi() {
        assertEquals(XuLyNhac.DUNG, xet(trongGioHoc = true, congMo = true))
        assertEquals(
            XuLyNhac.DUNG,
            xet(duocKhiHetGio = true, trongGioHoc = true, congMo = true)
        )
    }

    @Test
    fun game_keu_o_nen_sau_khi_tam_dung_phien_thi_im() {
        assertEquals(XuLyNhac.DUNG, xet())
    }

    @Test
    fun con_gio_choi_thi_phat_gi_cung_duoc() {
        assertEquals(XuLyNhac.CHO_PHAT, xet(congMo = true))
    }

    /** App hoc tieng Anh khong doc duoc thanh tieng thi coi nhu hong. */
    @Test
    fun app_trong_danh_sach_trang_van_doc_duoc_thanh_tieng() {
        assertEquals(XuLyNhac.CHO_PHAT, xet(duocKhiHetGio = true))
    }

    /** Han rieng xet truoc gio choi: mot ngay bao nhieu phut la bay nhieu. */
    @Test
    fun het_han_ngay_thi_im_ke_ca_khi_dang_co_gio_choi() {
        assertEquals(XuLyNhac.DUNG, xet(hetHanNgay = true, congMo = true))
        assertEquals(
            XuLyNhac.DUNG,
            xet(laAppNhac = true, hetHanNgay = true, congMo = true)
        )
    }

    /**
     * Gio ngu khoa ca danh sach trang, giong luat chan app.
     *
     * Truoc day gio ngu chi khoa app nhac, con tu dien va app hoc thi keu ca dem.
     * Ba Huy doi lai khi thay app AI cung nam trong danh sach trang.
     */
    @Test
    fun gio_ngu_dung_tieng_cua_app_trong_danh_sach_trang() {
        assertEquals(XuLyNhac.DUNG, xet(duocKhiHetGio = true, trongGioNgu = true))
    }

    /** Gio choi con thi van phat, ke ca trong khung gio ngu. */
    @Test
    fun gio_ngu_ma_con_gio_choi_thi_van_phat() {
        assertEquals(XuLyNhac.CHO_PHAT, xet(duocKhiHetGio = true, trongGioNgu = true, congMo = true))
        assertEquals(XuLyNhac.CHO_PHAT, xet(trongGioNgu = true, congMo = true))
    }
}
