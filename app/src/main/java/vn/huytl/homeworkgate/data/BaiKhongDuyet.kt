package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The bai Ba Huy khong duyet ma Le Hoa da xoa khoi man Bai da cham (30/9/2026).
 *
 * VI SAO CO. Truoc day Ba Huy bam Khong duyet thi the bai 21:48 ngay 30/9/2026 van ghi "chưa có
 * kết quả chấm từng câu", nhu bai dang cho. Tu ban nay the ghi "Không được duyệt" kem ly do, va
 * co nut xoa o goc. Xoa chi an the tren tablet nay: Bang dieu khien van giu bai, document tren
 * Firestore khong doi.
 *
 * Man chinh khong co dong bao rieng cho bai khong duoc duyet: Ba Huy chon chi hien o the bai.
 */
object BaiKhongDuyet {

    private const val KHOA_AN = "bai_khong_duyet_an"

    /** Man Bai da cham chi hien 10 bai moi nhat, nen giu 50 ma bai da an la thua. */
    private const val AN_TOI_DA = 50

    /** Le Hoa bam Xoa o the bai [id]: an the khoi man Bai da cham. */
    fun an(context: Context, id: String) {
        if (id.isBlank()) return
        val ds = (docAn(context).filter { it != id } + id).takeLast(AN_TOI_DA)
        val mang = JSONArray().apply { ds.forEach { put(it) } }
        Prefs.get(context).raw().edit().putString(KHOA_AN, mang.toString()).commit()
    }

    /** Ma cac bai Le Hoa da xoa khoi man Bai da cham. */
    fun daAn(context: Context): Set<String> = docAn(context).toSet()

    /** "lúc 21:48" cho bai nop hom nay, "ngày 28/09 lúc 21:15" cho bai hom khac. */
    fun lucNop(luc: Long, now: Long = System.currentTimeMillis()): String {
        val vn = Locale.forLanguageTag("vi-VN")
        val gio = SimpleDateFormat("HH:mm", vn).format(Date(luc))
        val a = Calendar.getInstance().apply { timeInMillis = luc }
        val b = Calendar.getInstance().apply { timeInMillis = now }
        val cungNgay = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        return if (cungNgay) "lúc $gio" else "ngày ${SimpleDateFormat("dd/MM", vn).format(Date(luc))} lúc $gio"
    }

    /** Chi test goi: xoa het, de moi test bat dau tu kho trong. */
    internal fun xoaHet(context: Context) {
        Prefs.get(context).raw().edit().remove(KHOA_AN).commit()
    }

    private fun docAn(context: Context): List<String> = runCatching {
        val mang = JSONArray(Prefs.get(context).raw().getString(KHOA_AN, null) ?: return emptyList())
        (0 until mang.length()).map { mang.getString(it) }
    }.getOrDefault(emptyList())
}
