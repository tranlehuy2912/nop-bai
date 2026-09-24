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
 * Ba Huy chep tin trong nhom Zalo roi gui sang, bang lenh /tinco ben Telegram hay
 * nut Tin cua co giao trong app Bang dieu khien. App nhan duoc thi cat vao day roi
 * hien tren man chinh. Ba noi cam tablet len la doc duoc, khong phai dung Zalo.
 *
 * Giu toi da [TOI_DA] tin. Moi tin vai tram byte nen ca kho chi vai chuc KB,
 * khong dang de day ra file rieng.
 */
class KhoTinCuaCo(context: Context) {

    private val sp = Prefs.get(context).raw()

    /**
     * Them mot tin.
     *
     * [luc] la luc Ba Huy gui chu khong phai luc tablet nhan: tablet tat ca dem thi
     * sang hom sau moi nhan, ma man Tin cua co phai ghi gio toi qua.
     *
     * Xep theo gio gui chu khong chen len dau, vi tin den muon co the cu hon tin vua
     * nhan. Cung mot tin cung gio gui thi bo qua: mot lenh doc lai hai lan khong duoc
     * thanh hai tin.
     */
    fun them(noiDung: String, luc: Long = System.currentTimeMillis()) {
        val sach = noiDung.trim()
        if (sach.isEmpty()) return
        val gio = if (luc > 0L) luc else System.currentTimeMillis()

        val cu = doc()
        val cac = (0 until cu.length()).map { cu.getJSONObject(it) }
        if (cac.any { it.optLong(K_LUC) == gio && it.optString(K_NOI_DUNG) == sach }) return

        val moi = JSONObject().apply {
            put(K_LUC, gio)
            put(K_NOI_DUNG, sach)
            put(K_DA_DOC, false)
        }
        // Tin moi nhat len dau, cat bot duoi neu qua day
        ghi(JSONArray((cac + moi).sortedByDescending { it.optLong(K_LUC) }.take(TOI_DA)))
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
