package vn.huytl.homeworkgate.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Nhat ky trong ngay, de lenh /nhatky tra loi duoc cau "hom nay no choi may lan".
 *
 * Chi giu mot ngay va toi da [MAX_LINES] dong. Giu dai hon thi thanh ho so theo
 * doi mot dua tre, ma viec o day chi la nho giup mot buoi toi chu khong phai
 * ghi so ca nam.
 */
object DayLog {

    private const val K_DAY = "log_day"
    private const val K_TEXT = "log_text"
    private const val MAX_LINES = 40

    private val clock = SimpleDateFormat("HH:mm", Locale("vi", "VN"))

    fun add(context: Context, text: String) {
        val sp = Prefs.get(context).raw()
        val today = dayKey()
        val old = if (sp.getInt(K_DAY, 0) == today) sp.getString(K_TEXT, "").orEmpty() else ""
        val line = "${clock.format(Date())}  $text"
        val lines = (old.lines().filter { it.isNotBlank() } + line).takeLast(MAX_LINES)
        sp.edit()
            .putInt(K_DAY, today)
            .putString(K_TEXT, lines.joinToString("\n"))
            .apply()
    }

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
