package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/**
 * Chum khoa API cua AI, va luat nghi khi mot khoa bi tu choi.
 *
 * "Het han muc" khong phai mot thu duy nhat. Google chan theo nhieu tang cung luc:
 * so lan goi moi phut, so lan goi moi ngay, so token moi phut. Het han muc phut la
 * mot phut sau goi lai duoc; het han muc ngay thi phai doi sang hom sau. Danh dau
 * chung mot kieu "hong het hom nay" la sai o ca hai dau: het phut ma nghi ca ngay
 * thi phi khoa, het ngay ma mot phut sau lai goi thi lan nao cung an 429.
 *
 * Nen o day moi khoa mang mot moc "nghi den luc nao" chu khong phai mot dau tich.
 * Het phut thi moc do la mot phut sau (theo dung so giay chinh Google bao trong
 * retryDelay), het ngay thi la nua dem, khoa sai thi la mot ngay - du lau de khong
 * goi vo ich, va van tu song lai neu Ba Huy quen khong sua.
 *
 * Danh sach khoa nam trong cung cho da ma hoa voi token bot.
 */
object KhoaAi {

    /** Vi sao mot khoa dang nghi. Chi de hien ra man hinh cho de doc. */
    enum class Vi { PHUT, NGAY, HONG, KHAC }

    data class Nghi(val denLuc: Long, val vi: Vi)

    /** Toan bo khoa, theo thu tu uu tien. */
    fun tatCa(context: Context): List<String> = Prefs.get(context).aiKeys

    /**
     * Khoa nen dung bay gio: khoa dau tien khong con dang nghi.
     * Tra ve null khi ca chum dang nghi.
     */
    fun hienTai(context: Context, now: Long = System.currentTimeMillis()): String? {
        val nghi = dangNghi(context, now)
        return tatCa(context).firstOrNull { it !in nghi }
    }

    /**
     * Cho [khoa] nghi den [denLuc], roi tra ve khoa ke tiep con dung duoc.
     *
     * Tra ve null nghia la ca chum dang nghi - luc do ben goi bao Ba Huy chu dung
     * thu lai vong quanh.
     */
    fun danhDauNghi(
        context: Context,
        khoa: String,
        denLuc: Long,
        vi: Vi = Vi.KHAC,
        now: Long = System.currentTimeMillis()
    ): String? {
        val map = docBang(context)
        map.put(khoa, JSONObject().put("den", denLuc).put("vi", vi.name))
        Prefs.get(context).raw().edit().putString(KEY_NGHI, map.toString()).commit()
        return hienTai(context, now)
    }

    /**
     * Doc cau tra loi loi cua Google roi cho khoa nghi dung kieu.
     *
     * Tra ve khoa nen dung cho lan goi sau: co the la chinh [khoa] nay (loi cua may
     * chu chu khong phai cua khoa), khoa ke tiep, hay null neu het.
     */
    fun nghiTheoLoi(
        context: Context,
        khoa: String,
        maLoi: Int,
        than: String,
        now: Long = System.currentTimeMillis()
    ): String? {
        val nghi = tinhNghi(maLoi, than, now) ?: return khoa
        return danhDauNghi(context, khoa, nghi.denLuc, nghi.vi, now)
    }

    /**
     * Tu ma loi va than tin cua Google, suy ra nen nghi den bao gio.
     *
     * Tra ve null nghia la dung trach khoa nay: hoac khong phai loi han muc (may chu
     * Google qua tai, mang dut), hoac goi thanh cong. Luc do thu lai chinh khoa do.
     *
     * Ham thuan, khong dung den may - de bo test chay duoc voi tung dang than tin
     * that ma Google tra ve.
     */
    fun tinhNghi(maLoi: Int, than: String, now: Long = System.currentTimeMillis()): Nghi? {
        // Khoa sai, khoa bi thu hoi, khoa chua bat API. Nghi mot ngay: du lau de
        // khong goi vo ich, va van tu song lai neu khong ai sua.
        val khoaHong = than.contains("API_KEY_INVALID") ||
            than.contains("API key not valid") ||
            than.contains("PERMISSION_DENIED") ||
            than.contains("SERVICE_DISABLED")
        if (maLoi == 400 || maLoi == 401 || maLoi == 403) {
            return if (khoaHong) Nghi(now + 24 * 60 * 60_000L, Vi.HONG) else null
        }

        if (maLoi != 429) return null

        // "limit: 0" la model nay khong co suat mien phi nao ca, khong phai khoa het
        // luot. Doi khoa cung the: ca chum deu limit 0. Bat nguyen chum nghi vi mot
        // cai ten model chon sai la mat luon duong duyet bai.
        if (Regex("limit:\\s*0\\b").containsMatchIn(than)) return null

        // Han muc ngay: phai doi sang ngay moi that, retryDelay luc nay chi la con
        // so tuong doi cua Google chu khong phai luc han muc dat lai.
        if (than.contains("PerDay", ignoreCase = true) ||
            than.contains("per day", ignoreCase = true)
        ) {
            return Nghi(nuaDemSau(now), Vi.NGAY)
        }

        // Han muc phut (hoac token moi phut): nghi dung so giay Google bao, cong mot
        // giay cho chac. Khong thay so nao thi coi nhu mot phut.
        //
        // Google noi so giay o hai cho khac nhau tuy loi: trong khoi RetryInfo, hoac
        // chi la mot cau tieng Anh cuoi message ("Please retry in 57.31s"). Doc ca
        // hai, vi ban that ma toi thu ngay 13/9 chi co cau tieng Anh.
        val giay = Regex("\"retryDelay\"\\s*:\\s*\"(\\d+)(?:\\.\\d+)?s\"")
            .find(than)?.groupValues?.get(1)?.toLongOrNull()
            ?: Regex("retry in (\\d+)(?:\\.\\d+)?s").find(than)?.groupValues?.get(1)?.toLongOrNull()
        return Nghi(now + ((giay ?: 59L) + 1L) * 1000L, Vi.PHUT)
    }

    /** So khoa dung duoc ngay bay gio. */
    fun soKhoaConDung(context: Context, now: Long = System.currentTimeMillis()): Int {
        val nghi = dangNghi(context, now)
        return tatCa(context).count { it !in nghi }
    }

    /** Khoa dang dung la khoa thu may trong danh sach, dem tu 1. 0 la khong con. */
    fun thuTuDangDung(context: Context, now: Long = System.currentTimeMillis()): Int {
        val dang = hienTai(context, now) ?: return 0
        return tatCa(context).indexOf(dang) + 1
    }

    /** Mot dong mo ta cho man Cai dat. */
    fun moTa(context: Context, now: Long = System.currentTimeMillis()): String {
        val so = tatCa(context).size
        if (so == 0) return "Chưa có khoá nào. Chưa có khoá thì phần duyệt bằng AI không chạy."

        val dang = hienTai(context, now)
        if (dang != null) {
            val nghi = so - soKhoaConDung(context, now)
            val duoi = if (nghi > 0) " ($nghi khoá đang nghỉ)" else ""
            return "$so khoá · đang dùng khoá thứ ${thuTuDangDung(context, now)} " +
                "(${rutGon(dang)})$duoi. Hết lượt là tự nhảy sang khoá kế tiếp."
        }

        // Ca chum dang nghi: noi ro bao lau nua co khoa dung lai duoc, khong thi
        // nhin vao chi thay "het" ma khong biet la het mot phut hay het ca ngay.
        val som = tatCa(context).mapNotNull { bangNghi(context)[it] }.minOrNull() ?: now
        val conMs = (som - now).coerceAtLeast(0L)
        val con = if (conMs < 60 * 60_000L) {
            "${conMs / 60_000 + 1} phút nữa"
        } else {
            "${conMs / (60 * 60_000L)} tiếng nữa"
        }
        return "$so khoá, cả $so đang nghỉ. Dùng lại được sau $con."
    }

    /** Bo het danh dau, dung khi Ba Huy muon thu lai ngay. */
    fun xoaDanhDau(context: Context) {
        Prefs.get(context).raw().edit().remove(KEY_NGHI).commit()
    }

    /**
     * Dang ngan de ghi ra man hinh hay log ma khong lam lo ca khoa.
     *
     * Khoa API ma in nguyen ra log la ai doc log cung dung duoc, ke ca cac ban ghi
     * loi gui di noi khac.
     */
    fun rutGon(khoa: String): String =
        if (khoa.length <= 12) "…" else "${khoa.take(6)}…${khoa.takeLast(4)}"

    /** Cac khoa dang nghi tai thoi diem [now]. */
    private fun dangNghi(context: Context, now: Long): Set<String> =
        bangNghi(context).filterValues { it > now }.keys

    /** Khoa -> moc nghi den. */
    private fun bangNghi(context: Context): Map<String, Long> {
        val json = docBang(context)
        return json.keys().asSequence().mapNotNull { khoa ->
            val den = json.optJSONObject(khoa)?.optLong("den") ?: return@mapNotNull null
            khoa to den
        }.toMap()
    }

    private fun docBang(context: Context): JSONObject {
        val raw = Prefs.get(context).raw().getString(KEY_NGHI, null) ?: return JSONObject()
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun nuaDemSau(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private const val KEY_NGHI = "ai_khoa_nghi"
}
