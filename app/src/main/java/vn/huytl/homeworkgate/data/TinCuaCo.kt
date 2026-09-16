package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TinCuaCo(
    val luc: Long,
    val noiDung: String,
    val daDoc: Boolean
) {
    fun gioGui(): String = DINH_DANG.format(Date(luc))

    private companion object {
        val DINH_DANG = SimpleDateFormat("HH:mm 'ngày' dd/MM", Locale("vi", "VN"))
    }
}

/**
 * Kho tin co giao nhan tin vao nhom lop.
 *
 * Ba Huy chia se tin tu Zalo sang bot Telegram, app nhan duoc thi cat vao day roi
 * hien tren man chinh. Ba noi cam tablet len la doc duoc, khong phai dung Zalo.
 *
 * Giu toi da [TOI_DA] tin. Moi tin vai tram byte nen ca kho chi vai chuc KB,
 * khong dang de day ra file rieng.
 */
class KhoTinCuaCo(context: Context) {

    private val sp = Prefs.get(context).raw()

    fun them(noiDung: String) {
        val sach = noiDung.trim()
        if (sach.isEmpty()) return

        val moi = JSONArray()
        moi.put(
            JSONObject().apply {
                put(K_LUC, System.currentTimeMillis())
                put(K_NOI_DUNG, sach)
                put(K_DA_DOC, false)
            }
        )
        // Tin moi nhat len dau, cat bot duoi neu qua day
        val cu = doc()
        for (i in 0 until minOf(cu.length(), TOI_DA - 1)) {
            moi.put(cu.getJSONObject(i))
        }
        ghi(moi)
    }

    fun danhSach(): List<TinCuaCo> {
        val arr = doc()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TinCuaCo(
                luc = o.optLong(K_LUC),
                noiDung = o.optString(K_NOI_DUNG),
                daDoc = o.optBoolean(K_DA_DOC, false)
            )
        }
    }

    fun soTinChuaDoc(): Int = danhSach().count { !it.daDoc }

    fun danhDauDaDocHet() {
        val arr = doc()
        for (i in 0 until arr.length()) {
            arr.getJSONObject(i).put(K_DA_DOC, true)
        }
        ghi(arr)
    }

    fun xoaHet() = ghi(JSONArray())

    private fun doc(): JSONArray =
        runCatching { JSONArray(sp.getString(K_KHO, "[]")) }.getOrDefault(JSONArray())

    private fun ghi(arr: JSONArray) {
        sp.edit().putString(K_KHO, arr.toString()).commit()
    }

    private companion object {
        const val TOI_DA = 40
        const val K_KHO = "tin_cua_co"
        const val K_LUC = "luc"
        const val K_NOI_DUNG = "noi_dung"
        const val K_DA_DOC = "da_doc"
    }
}
