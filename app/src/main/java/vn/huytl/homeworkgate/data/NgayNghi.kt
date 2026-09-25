package vn.huytl.homeworkgate.data

import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Ngay nghi le trong nam hoc 2026-2027.
 *
 * Chi la le quoc gia. Truong Le Hoa nghi giua ky, nghi he, nghi ngay nha giao, nghi
 * dot xuat vi thoi tiet, nhung cai do khong co trong luat nao nen app khong biet.
 *
 * Lich nghi Tet 2027 tinh den luc viet van chua co quyet dinh chinh thuc, Bo Noi
 * vu moi dang trinh phuong an. So o day lay theo phuong an hoc sinh nghi 10 ngay
 * dang duoc de xuat, co quyet dinh that thi sua lai.
 */
object NgayNghi {

    private data class Khoang(val tu: Int, val den: Int, val ten: String)

    /** Ngay dang so nguyen yyyyMMdd cho de so sanh. */
    private fun ngay(nam: Int, thang: Int, ngay: Int) = nam * 10_000 + thang * 100 + ngay

    private val CAC_KHOANG = listOf(
        Khoang(ngay(2027, 1, 1), ngay(2027, 1, 1), "Tết Dương lịch"),
        Khoang(ngay(2027, 2, 1), ngay(2027, 2, 10), "nghỉ Tết Nguyên đán"),
        Khoang(ngay(2027, 4, 16), ngay(2027, 4, 16), "Giỗ tổ Hùng Vương"),
        Khoang(ngay(2027, 4, 30), ngay(2027, 5, 1), "lễ 30 tháng 4 và 1 tháng 5")
    )

    /** Ngay hoc cuoi cung cua nam hoc. Sau ngay nay app thoi nhac. */
    private val HET_NAM_HOC = ngay(2027, 5, 31)

    /** Tra ve ten ky nghi, hoac null neu hom do van hoc binh thuong. */
    fun tenKyNghi(cal: Calendar): String? {
        val n = ngay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        if (n > HET_NAM_HOC) return "nghỉ hè"
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return "chủ nhật"
        return CAC_KHOANG.firstOrNull { n >= it.tu && n <= it.den }?.ten
    }

    fun laNgayNghi(cal: Calendar): Boolean = tenKyNghi(cal) != null

    /** Dung cho test: dung mot Calendar o mua gio may. */
    fun calendarCua(nam: Int, thang: Int, ngay: Int, gio: Int = 0, phut: Int = 0): Calendar =
        GregorianCalendar(nam, thang - 1, ngay, gio, phut, 0)
}
