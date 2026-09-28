package vn.huytl.homeworkgate.ai

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Goi Gemini. Mot ham duy nhat: gui chu + may tam anh, nhan chu ve.
 *
 * Tu 28/9/2026 chi con mot viec dung toi: doc trang vo dan do, xem [DocDanDo]. Truoc
 * do Gemini con cham bai tap tren tablet; Ba Huy bo han phan cham, bai nao cung do
 * Claude cham tren Bang dieu khien.
 *
 * Khong dung thu vien cua Google: no keo theo ca dong phu thuoc de lam nhung viec
 * app nay khong can (stream, chat nhieu luot, function calling). O day chi la mot
 * POST kem anh ma thoi.
 *
 * Ham nay chan luong goi - phai goi tu Dispatchers.IO.
 */
class GeminiClient(private val khoa: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        // Doc mot trang vo mat vai giay, nhung luc Google ban thi lau hon nhieu.
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Ket qua tho: [maLoi] 0 la goi duoc, khac 0 la HTTP tra ve loi do. */
    data class Tra(val maLoi: Int, val than: String, val chu: String?)

    fun hoi(cauLenh: String, anh: List<File>, model: String = MODEL): Tra {
        val parts = JSONArray().put(JSONObject().put("text", cauLenh))
        anh.forEach { f ->
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                )
            )
        }

        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put(
                "generationConfig",
                JSONObject().put("responseMimeType", "application/json")
            )
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("$GOC/models/$model:generateContent?key=$khoa")
            .post(body)
            .build()

        client.newCall(req).execute().use { res ->
            val than = res.body?.string().orEmpty()
            if (!res.isSuccessful) return Tra(res.code, than, null)
            return Tra(0, than, docChu(than))
        }
    }

    /** Lay phan chu trong cau tra loi cua Gemini. */
    private fun docChu(than: String): String? = runCatching {
        val parts = JSONObject(than)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
        (0 until parts.length()).joinToString("") { parts.getJSONObject(it).optString("text") }
    }.getOrNull()

    companion object {
        private const val GOC = "https://generativelanguage.googleapis.com/v1beta"

        /**
         * Model Gemini cua app.
         *
         * Chon gemini-3.5-flash-lite ngay 13/9/2026, luc Gemini con cham bai, sau khi do
         * sau model tren cung bo anh bai tap:
         *
         *  | model                 | giay | token nghi | cau 2.26d (sai that) |
         *  | gemini-3.5-flash-lite |  3-4 |          0 | Sai 3/3 lan          |
         *  | gemini-3.5-flash      |   21 |      4.222 | Sai                  |
         *  | gemini-3.1-flash-lite |  3-5 |          0 | Dung 3/3 - doc nham  |
         *  | gemini-3-flash-preview|   18 |      3.391 | Dung - doc nham      |
         *
         * Ban lite vua nhanh nhat, re nhat (khong dot token "suy nghi"), vua it doc chu mo
         * thanh cai no doan nhat. Ly do sau cung la ly do giu no cho viec doc vo dan do.
         *
         * gemini-2.5-* tra 404 voi khoa moi tao ("no longer available to new users"),
         * gemini-3.1-pro tra 429 "limit: 0" - khong co suat mien phi.
         */
        const val MODEL = "gemini-3.5-flash-lite"
    }
}
