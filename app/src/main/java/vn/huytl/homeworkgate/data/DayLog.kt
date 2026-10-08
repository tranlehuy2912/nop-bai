package vn.huytl.homeworkgate.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Nhat ky trong ngay, de lenh /nhatky tra loi duoc cau "hom nay no choi may lan".
 *
 * Chi giu mot ngay, qua nua dem la so moi. Tu 8/10/2026 khong con gioi han so dong (truoc
 * do 40 dong, ngay nhieu viec thi dong som nhat bi day ra): anh Huy muon xem du ca ngay
 * va xem lai bay ngay tren Bang dieu khien. Ban sao tung ngay tren Firestore
 * (nhatky/{ngay}, xem [vn.huytl.homeworkgate.dongbo.DongBo]) giu [NGAY_GIU_TREN_MANG]
 * ngay roi tablet tu xoa: nho giup mot tuan chu khong phai ghi so ca nam.
 */
object DayLog {

    /** internal cho test chup roi tra lai nhat ky hom nay, khoi de dong gia tren may ao. */
    internal const val K_DAY = "log_day"
    internal const val K_TEXT = "log_text"

    /**
     * So ngay nhat ky (va so hoi AI) giu tren Firestore, tinh ca hom nay, bang so ngay cua
     * "Thời gian dùng app" ([NhatKySuDung.GIU_NGAY]) vi anh Huy muon xem giong trang do.
     */
    const val NGAY_GIU_TREN_MANG = NhatKySuDung.GIU_NGAY

    private val clock = SimpleDateFormat("HH:mm", Locale("vi", "VN"))

    fun add(context: Context, text: String) {
        val sp = Prefs.get(context).raw()
        val today = dayKey()
        val old = if (sp.getInt(K_DAY, 0) == today) sp.getString(K_TEXT, "").orEmpty() else ""
        val line = "${clock.format(Date())}  $text"
        val lines = old.lines().filter { it.isNotBlank() } + line
        sp.edit()
            .putInt(K_DAY, today)
            .putString(K_TEXT, lines.joinToString("\n"))
            .apply()
    }

    /**
     * Than tin /nhatky: [tieuDe] roi cac dong cua [so]. Dai qua [toiDa] chu thi bo dong cu
     * nhat truoc, va dong thu hai noi bo bao nhieu dong.
     *
     * Tu 8/10/2026 so khong gioi han so dong, ma mot tin Telegram chi chua 4096 chu va
     * TelegramClient.sendMessage cat duoi tin dai, tuc la mat dung cac dong moi nhat, la cac
     * dong Ba Huy dang muon doc. Cung cach voi [NhatKyAi.tinHoi]; dong moi nhat luon con.
     */
    fun tinNhatKy(tieuDe: String, so: String, toiDa: Int): String {
        val dong = so.lines().filter { it.isNotBlank() }
        val ca = tieuDe + "\n" + dong.joinToString("\n")
        if (ca.length <= toiDa || dong.size < 2) return ca

        // So dong bo luon it hon dong.size nen dong bao that khong dai hon cho da chua.
        var con = toiDa - tieuDe.length - baoBot(dong.size).length - 2 - dong.last().length
        var tu = dong.lastIndex
        while (tu > 0 && dong[tu - 1].length + 1 <= con) {
            tu--
            con -= dong[tu].length + 1
        }
        return tieuDe + "\n" + baoBot(tu) + "\n" + dong.drop(tu).joinToString("\n")
    }

    private fun baoBot(n: Int) = "(Tin dài quá nên không hiện $n dòng đầu ngày.)"

    /** Cac dong cua hom nay, hoac chuoi rong neu chua co gi. */
    fun today(context: Context): String {
        val sp = Prefs.get(context).raw()
        if (sp.getInt(K_DAY, 0) != dayKey()) return ""
        return sp.getString(K_TEXT, "").orEmpty()
    }

    private fun dayKey(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 10_000 +
            (cal.get(Calendar.MONTH) + 1) * 100 +
            cal.get(Calendar.DAY_OF_MONTH)
    }
}
