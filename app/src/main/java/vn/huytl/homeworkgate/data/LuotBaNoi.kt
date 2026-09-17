package vn.huytl.homeworkgate.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context

/**
 * Luat mot luot moi ngay cua ba noi, giu o phia tablet.
 *
 * Ben may ba cung dem mot lan nua, va do la chu y: may ba go cai dat hay cai lai
 * app la so dem ben do ve khong, luc do chi con cho nay chan. Nguoc lai thi khong
 * - ben do dem de tat nut di cho ba khoi bam vao khoang khong, con cho nay moi la
 * cho that su tu choi.
 *
 * VI SAO BA NOI BI GIOI HAN MA BA HUY THI KHONG: ba cho gio vi thuong chau, va
 * mot ngay mot lan la con so hai nguoi lon da thong nhat. Ba Huy thi dang cam cai
 * app quan ly, ong ay tu chiu trach nhiem ve con so cua minh.
 *
 * VIEC NHA KHONG TINH VAO DAY. Viec nha la viec that trong nha, ba giao bao nhieu
 * lan cung duoc, va so phut di kem la cong lam ra chu khong phai mot suat uu dai.
 *
 * Giu nguyen ten khoa cu cua hop thu Telegram ("hopthu_ngay_da_cho"): may dang cai
 * ban cu, ba da cho hom nay roi, ma doi ten khoa la sang ban moi ba bam duoc lan
 * nua trong cung mot ngay.
 */
object LuotBaNoi {

    /** Hom nay ba da cho lan nao chua. */
    fun daChoHomNay(context: Context): Boolean =
        Prefs.get(context).raw().getString(K_NGAY, "") == homNay()

    fun ghiNhanDaCho(context: Context) {
        Prefs.get(context).raw().edit().putString(K_NGAY, homNay()).commit()
    }

    private fun homNay(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private const val K_NGAY = "hopthu_ngay_da_cho"
}
