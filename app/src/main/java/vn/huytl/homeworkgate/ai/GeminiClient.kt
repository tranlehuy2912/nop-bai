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
 * Goi Gemini de cham bai. Mot ham duy nhat: gui chu + may tam anh, nhan chu ve.
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
        // Cham mot bai ba anh mat 3-5 giay, nhung luc Google ban thi lau hon nhieu.
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Ket qua tho: [maLoi] 0 la goi duoc, khac 0 la HTTP tra ve loi do. */
    data class Tra(val maLoi: Int, val than: String, val chu: String?)

    fun hoi(cauLenh: String, anh: List<File>, model: String = PromptCham.MODEL): Tra {
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

    private companion object {
        const val GOC = "https://generativelanguage.googleapis.com/v1beta"
    }
}
