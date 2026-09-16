package vn.huytl.homeworkgate.data

import android.content.Context
import java.util.Calendar

/**
 * Han gio rieng cua tung app, dem doc lap voi gio choi.
 *
 * Co nhung app khong hop voi luat "duyet bai xong duoc choi bao nhieu do": Netflix
 * mot ngay mot tap la du, du hom do Le Hoa co dua gio choi hay khong. Mo trong gio
 * choi cung tinh, mo ngoai gio choi ma app nam trong danh sach trang cung tinh - het
 * han la thoi, mai xem tiep.
 *
 * Dem theo ngay, sang hom sau tu ve khong. Khong cong don qua ngay: "hom qua chua
 * xem het nen hom nay duoc gap doi" khong phai y dinh cua ai ca.
 */
object GioiHanApp {

    /** So phut moi ngay cho [pkg]. 0 la khong dat han. */
    fun han(context: Context, pkg: String): Int = doc(context, KEY_HAN)[pkg]?.toInt() ?: 0

    /** Toan bo danh sach app co dat han, goi -> so phut moi ngay. */
    fun tatCa(context: Context): Map<String, Int> =
        doc(context, KEY_HAN).mapValues { it.value.toInt() }

    /** Dat han [phut] moi ngay. Dat 0 la bo han. */
    fun datHan(context: Context, pkg: String, phut: Int) {
        val map = doc(context, KEY_HAN).toMutableMap()
        if (phut <= 0) map.remove(pkg) else map[pkg] = phut.coerceIn(1, 24 * 60).toLong()
        ghi(context, KEY_HAN, map)
    }

    /** So milli giay da dung hom nay. */
    fun daDungMs(context: Context, pkg: String): Long = docHomNay(context)[pkg] ?: 0L

    /** Con lai bao nhieu milli giay hom nay. Khong dat han thi tra ve Long.MAX_VALUE. */
    fun conLaiMs(context: Context, pkg: String): Long {
        val phut = han(context, pkg)
        if (phut <= 0) return Long.MAX_VALUE
        return (phut * 60_000L - daDungMs(context, pkg)).coerceAtLeast(0L)
    }

    fun hetGio(context: Context, pkg: String): Boolean {
        val phut = han(context, pkg)
        return phut > 0 && daDungMs(context, pkg) >= phut * 60_000L
    }

    /**
     * Cong them [themMs] vao so da dung hom nay cua [pkg].
     *
     * Ghi bang apply() chu khong commit(): ham nay chay moi hai muoi giay trong suot
     * luc con dung app, ma mat mot nhip cuoi cung khi tien trinh bi giet thi cung
     * chi sai vai chuc giay.
     */
    fun congThem(context: Context, pkg: String, themMs: Long) {
        if (themMs <= 0L) return
        val map = docHomNay(context).toMutableMap()
        map[pkg] = (map[pkg] ?: 0L) + themMs
        val sp = Prefs.get(context).raw()
        sp.edit()
            .putInt(KEY_NGAY, ngayHomNay())
            .putStringSet(KEY_DA_DUNG, map.map { "${it.key}=${it.value}" }.toSet())
            .apply()
    }

    /** Xoa so da dung hom nay, dung khi Ba Huy muon cho xem them. */
    fun xoaDaDung(context: Context, pkg: String) {
        val map = docHomNay(context).toMutableMap()
        map.remove(pkg)
        val sp = Prefs.get(context).raw()
        sp.edit()
            .putInt(KEY_NGAY, ngayHomNay())
            .putStringSet(KEY_DA_DUNG, map.map { "${it.key}=${it.value}" }.toSet())
            .commit()
    }

    /** So da dung, tu bo qua neu ban ghi la cua ngay hom truoc. */
    private fun docHomNay(context: Context): Map<String, Long> {
        val sp = Prefs.get(context).raw()
        if (sp.getInt(KEY_NGAY, 0) != ngayHomNay()) return emptyMap()
        return doc(context, KEY_DA_DUNG)
    }

    private fun doc(context: Context, key: String): Map<String, Long> =
        Prefs.get(context).raw().getStringSet(key, emptySet()).orEmpty()
            .mapNotNull { dong ->
                val cho = dong.lastIndexOf('=')
                if (cho <= 0) return@mapNotNull null
                val so = dong.substring(cho + 1).toLongOrNull() ?: return@mapNotNull null
                dong.substring(0, cho) to so
            }
            .toMap()

    private fun ghi(context: Context, key: String, map: Map<String, Long>) {
        Prefs.get(context).raw().edit()
            .putStringSet(key, map.map { "${it.key}=${it.value}" }.toSet())
            .commit()
    }

    private fun ngayHomNay(now: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return cal.get(Calendar.YEAR) * 10_000 +
            (cal.get(Calendar.MONTH) + 1) * 100 +
            cal.get(Calendar.DAY_OF_MONTH)
    }

    private const val KEY_HAN = "gioi_han_app"
    private const val KEY_DA_DUNG = "gioi_han_da_dung"
    private const val KEY_NGAY = "gioi_han_ngay"
}
