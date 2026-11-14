package vn.huytl.homeworkgate.data

import org.json.JSONObject
import vn.huytl.homeworkgate.dongbo.Duong
import java.util.Calendar

/** Khoang khoa mot thu trong tuan: [tu] toi [toi], phut tinh tu 0 gio, trong ngay ([tu] < [toi]). */
data class NgayKhoa(val bat: Boolean, val tu: Int, val toi: Int)

/**
 * Gio di hoc theo tung thu, Ba Huy dat tren Bang dieu khien (anh Huy chot 14/11/2026: "lam phan
 * cau hinh de toi tu thay doi, thoi khoa bieu co doi toi cung tu doi").
 *
 * Truoc ngay do tablet chan tu moc buong may trong ban lich ("buongMay": chieu 11:30, sang thu hai
 * va thu sau 8:45) toi luc tan buoi, nen doi gio chan phai nho Claude Code ghi ban lich moi. Gio
 * moi thu mot khoang, bat tat duoc tung thu. Lich hoc chi con de biet buoi do hoc mon gi (man chan
 * ke mon can mang, nhac soan tap) va ngay nao nghi.
 *
 * Ngay nghi khong khoa: cac ky nghi trong ban lich, he (sau [BanLich.hetNamHoc]), va ngay Ba Huy
 * bam "Cho nghỉ học" ([NgayNghi.ngayThem]). Chu nhat thi KHONG tu coi la nghi nhu [NgayNghi.laNgayNghi]:
 * Ba Huy bat khoa chu nhat thi chu nhat khoa.
 *
 * Laptop co mot bo rieng cung dang nay (anh Huy chot: moi may mot bo), Bang dieu khien ghi thang
 * vao laptop/{maNha}; tablet chi ghi cho laptop cac ngay nghi, xem dongbo/Laptop.
 *
 * Thu la so cua Calendar.DAY_OF_WEEK: 1 chu nhat, 2 thu hai ... 7 thu bay.
 */
object KhoaHoc {

    /**
     * Gia tri luc moi cai (anh Huy chot 14/11/2026 "ok"): thu hai va thu sau 8:45 toi 17:00 (sang
     * hai ngay do co the duc, Tin hoc; khoa lien de khoi mo 15 phut giua hai buoi), thu ba, tu,
     * nam, bay 11:00 toi 17:00, chu nhat khong khoa.
     */
    val MAC_DINH: Map<Int, NgayKhoa> = mapOf(
        Calendar.SUNDAY to NgayKhoa(false, 11 * 60, 17 * 60),
        Calendar.MONDAY to NgayKhoa(true, 8 * 60 + 45, 17 * 60),
        Calendar.TUESDAY to NgayKhoa(true, 11 * 60, 17 * 60),
        Calendar.WEDNESDAY to NgayKhoa(true, 11 * 60, 17 * 60),
        Calendar.THURSDAY to NgayKhoa(true, 11 * 60, 17 * 60),
        Calendar.FRIDAY to NgayKhoa(true, 8 * 60 + 45, 17 * 60),
        Calendar.SATURDAY to NgayKhoa(true, 11 * 60, 17 * 60),
    )

    /** Ten truong cua tung thu trong JSON va trong Firestore, xem [Duong.F_KHOA_HOC]. */
    private const val F_BAT = Duong.F_BAT
    private const val F_TU = Duong.F_TU
    private const val F_TOI = Duong.F_TOI

    /**
     * Cau hinh dang dung. Tablet tro vao Prefs (App.onCreate), test gan ban rieng. Mac dinh la
     * [MAC_DINH], nen cho chua gan van chan dung gio luc moi cai.
     */
    @Volatile
    var cauHinh: () -> Map<Int, NgayKhoa> = { MAC_DINH }

    /** Khoang khoa cua ngay [cal], null neu ngay do khong khoa (tat, hay ngay nghi). */
    fun ngayKhoa(cal: Calendar): NgayKhoa? {
        val k = cauHinh()[cal.get(Calendar.DAY_OF_WEEK)] ?: return null
        if (!k.bat || k.tu >= k.toi) return null
        if (nghi(cal)) return null
        return k
    }

    /** Ngay nghi khong khoa gio di hoc, xem dau file. */
    fun nghi(cal: Calendar): Boolean = nghiTheoLich(cal) || NgayNghi.soNgay(cal) in NgayNghi.ngayThem()

    /** Nghi theo ban lich: ky nghi, he. Khong tinh chu nhat, khong tinh ngay Ba Huy cho nghi them. */
    fun nghiTheoLich(cal: Calendar): Boolean {
        val n = NgayNghi.soNgay(cal)
        val ban = LichDangDung.ban
        return n > ban.hetNamHoc || ban.cacKyNghi.any { n >= it.tu && n <= it.den }
    }

    /** Doc ban luu trong Prefs; hong hay thieu thu nao thi thu do lay [MAC_DINH]. */
    fun docJson(chu: String?): Map<Int, NgayKhoa> {
        if (chu.isNullOrBlank()) return MAC_DINH
        val o = runCatching { JSONObject(chu) }.getOrNull() ?: return MAC_DINH
        return MAC_DINH.mapValues { (thu, md) ->
            val n = o.optJSONObject(thu.toString()) ?: return@mapValues md
            hopLe(n.optBoolean(F_BAT, md.bat), n.optInt(F_TU, md.tu), n.optInt(F_TOI, md.toi)) ?: md
        }
    }

    fun thanhJson(ds: Map<Int, NgayKhoa>): String {
        val o = JSONObject()
        ds.toSortedMap().forEach { (thu, k) ->
            o.put(thu.toString(), JSONObject().put(F_BAT, k.bat).put(F_TU, k.tu).put(F_TOI, k.toi))
        }
        return o.toString()
    }

    /** Dang ghi vao hop/caidat: { "1": { bat, tu, toi }, ... "7": {...} }. */
    fun thanhMap(ds: Map<Int, NgayKhoa>): Map<String, Any> =
        ds.toSortedMap().entries.associate { (thu, k) ->
            thu.toString() to mapOf(F_BAT to k.bat, F_TU to k.tu, F_TOI to k.toi)
        }

    /**
     * Doc gia tri lenh CAIDAT khoaHoc tu Bang dieu khien (cung dang [thanhMap]). Thu nao thieu thi
     * giu [cu]. Tra ve null kem ly do neu co thu sai (gio mo khong sau gio khoa, so ngoai 0..1439).
     */
    fun tuLenh(giaTri: Any?, cu: Map<Int, NgayKhoa>): Pair<Map<Int, NgayKhoa>?, String?> {
        val m = giaTri as? Map<*, *> ?: return null to "Giá trị không đúng dạng."
        val ra = cu.toMutableMap()
        for ((khoa, v) in m) {
            val thu = khoa?.toString()?.toIntOrNull()?.takeIf { it in 1..7 }
                ?: return null to "Không có thứ $khoa."
            val n = v as? Map<*, *> ?: return null to "Giá trị không đúng dạng."
            val cuThu = cu[thu] ?: MAC_DINH.getValue(thu)
            val bat = n[F_BAT] as? Boolean ?: cuThu.bat
            val tu = (n[F_TU] as? Number)?.toInt() ?: cuThu.tu
            val toi = (n[F_TOI] as? Number)?.toInt() ?: cuThu.toi
            ra[thu] = hopLe(bat, tu, toi)
                ?: return null to "${tenThu(thu).replaceFirstChar { it.uppercase() }}: giờ mở lại phải sau giờ khoá."
        }
        return ra to null
    }

    private fun hopLe(bat: Boolean, tu: Int, toi: Int): NgayKhoa? {
        if (tu !in 0 until 24 * 60 || toi !in 1..24 * 60) return null
        if (tu >= toi) return null
        return NgayKhoa(bat, tu, toi)
    }

    /** "thứ hai 08:45–17:00, ..., chủ nhật không khoá", cho cau tra loi va nhat ky. */
    fun moTa(ds: Map<Int, NgayKhoa>): String =
        THU_TU.joinToString(", ") { thu ->
            val k = ds[thu]
            val gio = if (k == null || !k.bat) "không khoá" else "${gio(k.tu)}–${gio(k.toi)}"
            "${tenThu(thu)} $gio"
        }

    /** Thu hai truoc, chu nhat sau cung, nhu tren lich. */
    val THU_TU = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
    )

    private fun tenThu(thu: Int) = ThoiKhoaBieu.tenThu(thu)

    private fun gio(phut: Int) = "%02d:%02d".format(phut / 60, phut % 60)
}
