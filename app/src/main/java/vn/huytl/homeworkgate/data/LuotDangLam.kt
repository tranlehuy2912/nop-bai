package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Luot dang lam do cua mot cau tren may: sao con lai, so lan sai (phim nhieu mo toi dau, xem
 * [LuatGhep.mucMo]), cac phuong an da chon sai. Ghi moi lan con bam Kiem tra ma sai, xoa khi
 * luot xong.
 *
 * VI SAO CO. Ban dau (29/9/2026) roi man giua mot cau da sai la ghi luot do hong, 0 sao, de
 * thoat ra vao lai khong thanh cach xoa sai. Nhung man hinh tu tat luc con dang nghi cung
 * la roi man (onStop): con bat man hinh len thay cau van do, ma bam Kiem tra khong an gi,
 * vi luot da bi ghi xong. Giu luot do lai thi ca hai deu on: vao lai la dung so sao da mat,
 * khong phat them, cung khong xoa duoc lan sai nao.
 *
 * Nam trong kho prefs rieng: ghi vao file prefs chinh thi moi lan bam sai keo theo mot luot
 * ghi Firestore (xem DongBo.KHOA_BO_QUA).
 */
object LuotDangLam {

    private const val KHO = "luot_dang_lam"

    fun lay(context: Context, cauId: String, saoToiDa: Int): LuatGhep.Luot? {
        val chu = runCatching { Prefs.khoRieng(context, KHO).getString(cauId, null) }.getOrNull()
            ?: return null
        val o = runCatching { JSONObject(chu) }.getOrNull() ?: return null
        // Cau da soan lai (so sao khac) thi luot cu khong con nghia.
        if (o.optInt("saoToiDa") != saoToiDa) return null
        return LuatGhep.Luot(
            saoToiDa = saoToiDa,
            sao = o.optInt("sao", saoToiDa).coerceIn(0, saoToiDa),
            lanSai = o.optInt("lanSai"),
            thuThem = o.optBoolean("thuThem"),
            saiDaChon = docDanhSach(o.optJSONArray("saiDaChon"))
        )
    }

    fun ghi(context: Context, cauId: String, luot: LuatGhep.Luot) {
        val o = JSONObject()
            .put("saoToiDa", luot.saoToiDa)
            .put("sao", luot.sao)
            .put("lanSai", luot.lanSai)
            .put("thuThem", luot.thuThem)
            .put("saiDaChon", JSONArray(luot.saiDaChon))
            .put("luc", System.currentTimeMillis())
        runCatching { Prefs.khoRieng(context, KHO).edit().putString(cauId, o.toString()).commit() }
    }

    /** Mot mang chuoi JSON ra danh sach, thieu hay hong la danh sach rong. Dung chung voi [GiaiDe]. */
    fun docDanhSach(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }

    fun xoa(context: Context, cauId: String) {
        runCatching { Prefs.khoRieng(context, KHO).edit().remove(cauId).commit() }
    }
}
