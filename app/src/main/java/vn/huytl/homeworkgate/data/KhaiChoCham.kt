package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONObject
import vn.huytl.homeworkgate.kho.PhamVi

/**
 * Pham vi bai con khai cua tung lan nop, giu lai cho den luc Ba Huy cham bang Claude.
 *
 * VI SAO CAN. Binh thuong pham vi chi song vai giay: man soat dua no sang service,
 * service cham xong la het viec. Khi tablet tat cham AI thi lan cham xay ra sau do
 * co khi vai tieng, luc Ba Huy dan ket qua Claude ve. Luc do van can dung pham vi cu:
 * cau nao thuoc sach nao, co phai lan on tap khong, con khai cau nao chua chac. Thieu
 * no thi ket qua cua Claude khong khop duoc voi ma cau trong so, va cau nao cung bi
 * tinh la bai ngoai sach.
 *
 * Mot file prefs rieng, moi bai mot khoa. Ban nao qua bay ngay thi don di.
 */
object KhaiChoCham {

    private const val FILE = "khai_cho_cham"
    private const val GIU_MS = 7 * 24 * 60 * 60_000L

    fun luu(
        context: Context,
        baiId: String,
        pham: PhamVi,
        now: Long = System.currentTimeMillis()
    ) {
        val sp = sp(context)
        val sua = sp.edit()
        sp.all.forEach { (khoa, giaTri) ->
            val luc = runCatching { JSONObject(giaTri as String).optLong("luc") }.getOrDefault(0L)
            if (now - luc > GIU_MS) sua.remove(khoa)
        }
        sua.putString(baiId, JSONObject().put("luc", now).put("pham", pham.sangJson()).toString())
        sua.apply()
    }

    fun lay(context: Context, baiId: String): PhamVi? {
        val chu = sp(context).getString(baiId, null) ?: return null
        return runCatching { PhamVi.tuJson(JSONObject(chu).optString("pham")) }.getOrNull()
    }

    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
