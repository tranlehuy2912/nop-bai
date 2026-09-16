package vn.huytl.homeworkgate.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Nhat ky cau con hoi app AI trong ngay.
 *
 * Tach khoi [DayLog] vi hai thu khac muc dich va khac vong doi. DayLog la su kien
 * cong, giu mot ngay, tra loi "hom nay choi may lan". Cai nay la noi dung con go
 * vao AI, de Ba Huy doc lai xem con nho AI giai ho hay chi nho chi cho sai.
 *
 * Van chi giu vai ngay, khong giu mai: dich la doc trong tuan roi thoi, khong phai
 * lap ho so. Con chu de ghi bang [MAX_DONG] chu khong phai luu tru vinh vien.
 */
object NhatKyAi {

    private const val K_TEXT = "nhatky_ai_text"
    private const val MAX_DONG = 200

    private val dongHo = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("vi-VN"))

    /** Ghi mot cau con vua hoi [tenApp]. Tra ve dong da ghi, de con gui di Telegram. */
    fun ghi(context: Context, tenApp: String, cau: String): String {
        val sp = Prefs.get(context).raw()
        val dong = "${dongHo.format(Date())}  [$tenApp]  $cau"
        val cu = sp.getString(K_TEXT, "").orEmpty()
        val moi = (cu.lines().filter { it.isNotBlank() } + dong).takeLast(MAX_DONG)
        sp.edit().putString(K_TEXT, moi.joinToString("\n")).commit()
        return dong
    }

    /** Toan bo nhat ky con giu. */
    fun tatCa(context: Context): String =
        Prefs.get(context).raw().getString(K_TEXT, "").orEmpty()

    /** Chi cac dong cua hom nay. */
    fun homNay(context: Context): String {
        val hom = SimpleDateFormat("dd/MM", Locale.forLanguageTag("vi-VN")).format(Date())
        return tatCa(context).lines().filter { it.startsWith(hom) }.joinToString("\n")
    }
}
