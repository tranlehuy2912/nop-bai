package vn.huytl.homeworkgate.data

import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Ngay nghi trong nam hoc va ngay hoc cuoi cung.
 *
 * Tu 4/10/2026 danh sach nam trong ban lich ([LichDangDung.ban], xem [BanLich]) chu khong
 * viet o day: truong bao nghi dot xuat, hay lich nghi Tet co quyet dinh chinh thuc, thi
 * Claude Code ghi ban lich moi len Firestore, khong cai lai app.
 *
 * Ban mang san trong app ([LichMacDinh]) chi co le quoc gia. Lich nghi Tet 2027 luc viet
 * van chua co quyet dinh chinh thuc, so trong do lay theo phuong an hoc sinh nghi 10 ngay
 * dang duoc de xuat.
 *
 * Bang dieu khien giu mot ban chep y het file nay, chi khac dong package, de ve tab
 * Lich hoc (tu 29/9/2026). Sua o day thi chep sang ban do va chay tools/kiem-duong.sh.
 */
object NgayNghi {

    /** Ngay dang so nguyen yyyyMMdd cho de so sanh. */
    private fun ngay(nam: Int, thang: Int, ngay: Int) = nam * 10_000 + thang * 100 + ngay

    /** Tra ve ten ky nghi, hoac null neu hom do van hoc binh thuong. */
    fun tenKyNghi(cal: Calendar): String? {
        val n = ngay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val ban = LichDangDung.ban
        if (n > ban.hetNamHoc) return "nghỉ hè"
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) return "chủ nhật"
        return ban.cacKyNghi.firstOrNull { n >= it.tu && n <= it.den }?.ten
    }

    fun laNgayNghi(cal: Calendar): Boolean = tenKyNghi(cal) != null

    /** Dung cho test: dung mot Calendar o mua gio may. */
    fun calendarCua(nam: Int, thang: Int, ngay: Int, gio: Int = 0, phut: Int = 0): Calendar =
        GregorianCalendar(nam, thang - 1, ngay, gio, phut, 0)
}
