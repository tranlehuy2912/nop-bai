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

    /**
     * Dau ghi thay cho moi lan con xuong dong trong cau, xem [motDong].
     *
     * Bang dieu khien doi dau nay lai thanh xuong dong that khi ve the "Hoi AI"
     * (DongHoiAi ben bang-dieu-khien). Cho nao chua doi lai - tin /hoi, logcat, ban
     * Bang dieu khien cu - van doc ra la sang dong moi, nho mui ten va hai dau cach.
     */
    const val XUONG_DONG = " ↵ "

    /** Moi kieu xuong dong con co the go hay dan vao, ke ca ba kieu cua Unicode. */
    private val CHO_XUONG_DONG = Regex("""\r\n|[\n\r\u0085\u2028\u2029]""")

    private val dongHo = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("vi-VN"))

    /** Ghi mot cau con vua hoi [tenApp]. Tra ve dong da ghi, de con gui di Telegram. */
    fun ghi(context: Context, tenApp: String, cau: String): String {
        val sp = Prefs.get(context).raw()
        val dong = "${dongHo.format(Date())}  [$tenApp]  ${motDong(cau)}"
        val cu = sp.getString(K_TEXT, "").orEmpty()
        val moi = (cu.lines().filter { it.isNotBlank() } + dong).takeLast(MAX_DONG)
        sp.edit().putString(K_TEXT, moi.joinToString("\n")).commit()
        return dong
    }

    /**
     * Cau con go, viet lai thanh mot dong de cat vao so.
     *
     * Mot cau mot dong. So nay noi cac cau bang dau xuong dong roi doc lai bang
     * lines(), va [homNay] chi giu dong bat dau bang ngay - nen de nguyen cau hai dong
     * thi dong thu hai roi mat, ca o /hoi lan the "Hoi AI" tren Bang dieu khien. Truoc
     * 23/9/2026 no mat that.
     *
     * Tu 23/9 den 28/9/2026 cho xuong dong bi thay bang dau cach. Ngay 28/9 Ba Huy bao
     * the Hoi AI "chua xuong hang": de bai con dan vao bon dong ("... duong cao AH." /
     * "a) ..." / "b) ..." / "c) ...") hien thanh mot doan lien, chu "b)" treo cuoi dong.
     * Nen gio moi cho xuong dong ghi thanh [XUONG_DONG] de Bang dieu khien tra lai duoc.
     * Cau ghi trong khoang do van la mot doan lien, khong khoi phuc duoc.
     *
     * Dong trong va khoang trang quanh cho xuong dong gop lai thanh mot lan xuong
     * dong: the Hoi AI dung dong trong de ngan hai cau, de dong trong ben trong mot cau
     * thi nhin nhu sang cau moi. U+2028, U+2029, U+0085 cung tinh la xuong dong.
     * lines() khong cat o chung nen so khong hong, nhung dau cham trong khuon doc ben
     * Bang dieu khien khong khop chung, va ca dong se hien nguyen ban.
     *
     * Tin Telegram van gui cau goc, xem noi goi [ghi].
     */
    fun motDong(cau: String): String =
        cau.split(CHO_XUONG_DONG)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(XUONG_DONG)

    /** Toan bo nhat ky con giu. */
    fun tatCa(context: Context): String =
        Prefs.get(context).raw().getString(K_TEXT, "").orEmpty()

    /** Chi cac dong cua hom nay. */
    fun homNay(context: Context): String {
        val hom = SimpleDateFormat("dd/MM", Locale.forLanguageTag("vi-VN")).format(Date())
        return tatCa(context).lines().filter { it.startsWith(hom) }.joinToString("\n")
    }
}
