package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.NhatKySuDung.Doan

/**
 * Ban so dung app day sang Bang dieu khien: khoang dang mo noi vao dung cho, va chi
 * con [NhatKySuDung.GIU_NGAY] ngay.
 *
 * Chi goi may ham khong doc kho, nen KHONG xoa prefs nhu [NhatKySuDungTest]: chay tren
 * may ao dung chung van giu nguyen so va cai dat cua app.
 */
@RunWith(AndroidJUnit4::class)
class BanDaySuDungTest {

    private val phut = 60_000L
    private val moc = 1_790_000_000_000L
    private fun luc(p: Int) = moc + p * phut

    /**
     * Kho chi ghi khoang dang mo nam phut mot lan. Ba Huy mo app luc 14:24 thi ban day
     * phai keo khoang YouTube den 14:24, khong dung o lan ghi 14:20.
     */
    @Test
    fun khoang_dang_mo_keo_dai_khoang_da_ghi() {
        val kho = listOf(Doan("yt", luc(0), luc(20)))
        val ra = NhatKySuDung.noiHet(kho, listOf(Doan("yt", luc(0), luc(24))))
        assertEquals(listOf(Doan("yt", luc(0), luc(24))), ra)
    }

    /** App vua mo, kho chua co dong nao cua no: them mot khoang moi. */
    @Test
    fun khoang_dang_mo_chua_co_trong_kho_thi_them_moi() {
        val kho = listOf(Doan("yt", luc(0), luc(20)))
        val ra = NhatKySuDung.noiHet(kho, listOf(Doan("zalo", luc(22), luc(25))))
        assertEquals(listOf("yt", "zalo"), ra.map { it.goi })
    }

    /** Roi app hon mot phut ruoi roi quay lai la hai lan choi, nhu luc ghi vao kho. */
    @Test
    fun khoang_dang_mo_cach_xa_thi_khong_noi() {
        val kho = listOf(Doan("yt", luc(0), luc(20)))
        val ra = NhatKySuDung.noiHet(kho, listOf(Doan("yt", luc(40), luc(45))))
        assertEquals(2, ra.size)
    }

    /** Cung luat voi ghi: khong co ten goi, dai 0, hay dai hon sau tieng thi bo. */
    @Test
    fun khoang_dang_mo_hong_thi_bo() {
        val kho = listOf(Doan("yt", luc(0), luc(20)))
        val ra = NhatKySuDung.noiHet(
            kho,
            listOf(
                Doan("", luc(21), luc(22)),
                Doan("zalo", luc(30), luc(30)),
                Doan("zalo", luc(0), luc(7 * 60))
            )
        )
        assertEquals(kho, ra)
    }

    /**
     * Ban day chi con cac khoang cham vao han tro ve sau. Khoang vat qua moc han van
     * giu, vi phan sau moc la cua ngay con trong so.
     */
    @Test
    fun ban_day_chi_con_trong_han_va_xep_theo_gio() {
        val han = luc(100)
        val ra = NhatKySuDung.trongHan(
            listOf(
                Doan("moi", luc(200), luc(210)),
                Doan("cu", luc(10), luc(20)),
                Doan("vat", luc(90), luc(110))
            ),
            han
        )
        assertEquals(listOf("vat", "moi"), ra.map { it.goi })
    }

    /** Tach phep noi ra dung chung khong duoc lam doi cach ghi cu. */
    @Test
    fun noi_vao_giu_nguyen_luat_cu() {
        val kho = listOf(Doan("yt", luc(0), luc(10)))
        // Quay lai sau 20 giay: van mot khoang.
        val lien = NhatKySuDung.noiVao(kho, Doan("yt", luc(10) + 20_000L, luc(20)))
        assertEquals(listOf(Doan("yt", luc(0), luc(20))), lien)
        // App khac cung luc: khong noi sang app nay.
        assertEquals(2, NhatKySuDung.noiVao(kho, Doan("zalo", luc(5), luc(8))).size)
    }
}
