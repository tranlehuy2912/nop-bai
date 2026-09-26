package vn.huytl.homeworkgate.telegram

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Lop goi Bot API. Khong dung thu vien Telegram nao, vi app chi can bon method
 * va viec tu goi giup kiem soat timeout cho long polling.
 *
 * Moi ham deu chan luong goi, nen phai goi tu Dispatchers.IO.
 */
class TelegramClient(private val token: String) {

    /** Client cho lenh thuong. */
    private val shortClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Client rieng cho getUpdates. readTimeout phai lon hon timeout gui cho
     * Telegram, neu khong OkHttp cat ket noi truoc khi Telegram tra loi va moi
     * lan poll deu thanh mot lan that bai.
     */
    private val pollClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(POLL_TIMEOUT_SEC + 15L, TimeUnit.SECONDS)
        .build()

    class ApiException(val description: String, val errorCode: Int) :
        IOException("Telegram tra loi loi $errorCode: $description")

    /**
     * Ket qua mot lan gui anh.
     *
     * Ngoai message_id con tra ve ca file_id cua tung tam. App Bang dieu khien ben
     * dien thoai Ba Huy cam file_id la tai lai duoc chinh tam anh do bang getFile -
     * nho the anh bai tap khong phai luu them mot ban nao tren Firebase.
     */
    data class AnhDaGui(val messageId: Long, val fileIds: List<String>)

    /**
     * Gui anh kem ban phim duyet. Tra ve message_id de sau nay sua lai ban phim.
     * Caption toi da 1024 ky tu theo dac ta, nen cat bot cho an toan.
     */
    fun sendPhoto(
        chatId: Long,
        photo: File,
        caption: String,
        replyMarkup: JSONObject?
    ): AnhDaGui {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())
            .addFormDataPart("caption", caption.take(1024))
            .addFormDataPart(
                "photo",
                photo.name,
                photo.asRequestBody("image/jpeg".toMediaType())
            )
            .apply {
                if (replyMarkup != null) {
                    addFormDataPart("reply_markup", replyMarkup.toString())
                }
            }
            .build()

        val result = call("sendPhoto", body)
        return AnhDaGui(result.optLong("message_id", 0L), listOfNotNull(fileIdTo(result)))
    }

    fun sendMessage(
        chatId: Long,
        text: String,
        replyMarkup: JSONObject? = null,
        replyToMessageId: Long = 0L
    ): Long {
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("text", text.take(4096))
            if (replyMarkup != null) put("reply_markup", replyMarkup)
            if (replyToMessageId != 0L) {
                put("reply_parameters", JSONObject().put("message_id", replyToMessageId))
            }
        }
        return call("sendMessage", payload.toJsonBody()).optLong("message_id", 0L)
    }

    /**
     * Gui nhieu anh thanh mot album. Telegram doi 2-10 anh va khong cho gan ban
     * phim vao album, nen nguoi goi phai gui them mot tin rieng mang hai nut va
     * tra loi vao album nay. Tra ve message_id cua tam anh dau.
     */
    fun sendPhotoAlbum(chatId: Long, photos: List<File>, caption: String): AnhDaGui {
        require(photos.size in 2..10) { "album phai co 2 den 10 anh" }

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())

        val media = JSONArray()
        photos.forEachIndexed { index, file ->
            val partName = "anh$index"
            media.put(JSONObject().apply {
                put("type", "photo")
                put("media", "attach://$partName")
                // Chu thich chi dat o tam dau, Telegram hien no cho ca album.
                if (index == 0) put("caption", caption.take(1024))
            })
            builder.addFormDataPart(
                partName,
                file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
        }
        builder.addFormDataPart("media", media.toString())

        val sent = callArray("sendMediaGroup", builder.build())
        val tin = (0 until sent.length()).map { sent.getJSONObject(it) }
        return AnhDaGui(
            messageId = tin.firstOrNull()?.optLong("message_id", 0L) ?: 0L,
            fileIds = tin.mapNotNull { fileIdTo(it) }
        )
    }

    /**
     * Lay file_id cua ban to nhat trong mot tin anh.
     *
     * Telegram tra ve nhieu co cho cung mot tam - ban thu nho de xem truoc, ban day
     * du o cuoi mang. Lay tam cuoi, vi bai tap but chi ma tai ve ban thu nho thi doc
     * khong ra chu.
     */
    private fun fileIdTo(tin: JSONObject): String? {
        val co = tin.optJSONArray("photo") ?: return null
        if (co.length() == 0) return null
        return co.optJSONObject(co.length() - 1)?.optString("file_id")?.takeIf { it.isNotBlank() }
    }

    /**
     * Long polling. Tra ve danh sach Update tho.
     * Telegram coi mot update la da nhan khi lan goi sau dung offset lon hon
     * update_id cua no, nen nguoi goi phai tu cap nhat offset.
     */
    /**
     * @param timeoutSec bao lau thi Telegram tra loi rong neu chua co gi. Dat 0 la
     *   hoi mot cai roi ve ngay, dung cho khung gio ngu: ghe hoi chu khong nam cho.
     */
    fun getUpdates(
        offset: Long,
        allowed: List<String>,
        timeoutSec: Int = POLL_TIMEOUT_SEC,
    ): List<JSONObject> {
        val payload = JSONObject().apply {
            if (offset > 0) put("offset", offset)
            put("timeout", timeoutSec)
            put("limit", 20)
            put("allowed_updates", JSONArray(allowed))
        }
        val req = Request.Builder()
            .url("$BASE$token/getUpdates")
            .post(payload.toJsonBody())
            .build()

        pollClient.newCall(req).execute().use { resp ->
            val raw = resp.body.string()
            val json = JSONObject(raw)
            if (!json.optBoolean("ok", false)) {
                throw ApiException(
                    json.optString("description", "khong ro"),
                    json.optInt("error_code", resp.code)
                )
            }
            val arr = json.optJSONArray("result") ?: JSONArray()
            return (0 until arr.length()).map { arr.getJSONObject(it) }
        }
    }

    /**
     * Bat buoc goi sau khi nhan callback_query, neu khong Telegram giu vong xoay
     * tren may cua bo cho den khi het thoi gian.
     */
    fun answerCallbackQuery(callbackQueryId: String, text: String? = null) {
        val payload = JSONObject().apply {
            put("callback_query_id", callbackQueryId)
            if (text != null) put("text", text.take(200))
        }
        runCatching { call("answerCallbackQuery", payload.toJsonBody()) }
    }

    /** Bo ban phim sau khi da bam, tranh bo bam lai lan hai vao anh cu. */
    fun clearReplyMarkup(chatId: Long, messageId: Long) {
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("message_id", messageId)
        }
        runCatching { call("editMessageReplyMarkup", payload.toJsonBody()) }
    }

    /**
     * Doi ban phim cua mot tin da gui.
     *
     * Dung sau khi AI cham xong: luc gui anh thi chua ai biet bai nay dang may phut,
     * nen nut to chi mang duoc con so mac dinh. Cham xong roi thi biet, va luc do
     * sua lai nut cho khop con so do.
     */
    fun datBanPhim(chatId: Long, messageId: Long, markup: JSONObject) {
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("message_id", messageId)
            put("reply_markup", markup)
        }
        runCatching { call("editMessageReplyMarkup", payload.toJsonBody()) }
    }

    /**
     * Tai mot tam anh Ba Huy gui ve may, de con xem duoc trong khung chat.
     *
     * Hai lan goi: getFile lay duong dan, roi tai thang file do ve. Telegram de file
     * o mot ten mien khac (api.telegram.org/file/bot...), khong phai cho goi lenh.
     *
     * Nuot moi loi: anh cua ba khong ve duoc thi con van con dong chu di kem, va
     * duong nhan tin khong duoc phep ket vi mot lan tai hong.
     */
    fun taiAnh(fileId: String, dich: File): Boolean {
        val duong = runCatching {
            call("getFile", JSONObject().put("file_id", fileId).toJsonBody())
                .optJSONObject("result")?.optString("file_path")
        }.getOrNull()
        if (duong.isNullOrBlank()) return false

        val req = Request.Builder().url("$FILE_GOC$token/$duong").build()
        return runCatching {
            shortClient.newCall(req).execute().use { res ->
                val than = res.body ?: return@use false
                if (!res.isSuccessful) return@use false
                dich.outputStream().use { ra -> than.byteStream().copyTo(ra) }
                true
            }
        }.getOrDefault(false)
    }

    /** Lay file_id cua ban to nhat trong mot tin anh Ba Huy gui. */
    fun fileIdToCuaTin(tin: JSONObject): String? = fileIdTo(tin)

    /** Sua lai noi dung mot tin da gui. Dung cho nhip tim, de khong rac chat. */
    fun editMessageText(chatId: Long, messageId: Long, text: String): Boolean {
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("message_id", messageId)
            put("text", text.take(4096))
        }
        return runCatching { call("editMessageText", payload.toJsonBody()) }.isSuccess
    }

    fun editCaption(chatId: Long, messageId: Long, caption: String) {
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("message_id", messageId)
            put("caption", caption.take(1024))
        }
        runCatching { call("editMessageCaption", payload.toJsonBody()) }
    }

    /**
     * Danh sach lenh hien trong menu dau / cua Telegram.
     *
     * Chi nhung lenh dung hang ngay. May lenh cai dat - so phut moi lan, tran ngay,
     * gio nghi, hai danh sach app - van go duoc nhung khong nam trong menu: nhung
     * thu do sua trong app tien hon, va de ca hai muoi ba dong trong menu thi cuon
     * mai khong thay cai can tim.
     *
     * /them khong co trong menu vi no la mot phan cua /cho: dang choi ma go /cho 15
     * thi may cong 15 phut vao phien, y het /them 15.
     */
    private fun danhSachLenh(): JSONArray = JSONArray().apply {
        put(cmd("duyet", "Duyệt bài chờ cũ nhất, ví dụ /duyet 45"))
        put(cmd("tuchoi", "Không duyệt bài cũ nhất, kèm lý do nếu muốn"))
        put(cmd("cho", "Cho chơi luôn, đang chơi thì cộng thêm: /cho 30"))
        put(cmd("bot", "Bớt phút của phiên đang chạy"))
        put(cmd("dung", "Tạm dừng, giữ nguyên số phút còn lại"))
        put(cmd("tiep", "Cho chơi tiếp sau khi tạm dừng"))
        put(cmd("khoa", "Khoá tablet ngay, mất số phút còn lại"))
        put(cmd("trangthai", "Còn bao nhiêu phút chơi, hôm nay đã duyệt bao nhiêu"))
        put(cmd("nhatky", "Hôm nay đã xảy ra những gì"))
        put(cmd("hoi", "Hôm nay Lê Hòa hỏi app AI những gì"))
        put(cmd("loi", "Lê Hòa hay sai kiểu gì, ví dụ /loi 7"))
        put(cmd("lichmai", "Buổi học kế tiếp có môn gì, cần mang vở nào"))
        put(cmd("soanlai", "Bắt soạn tập lại vì soạn thiếu"))
        put(cmd("mo", "Mở màn chặn cho hết buổi học đang chặn"))
        put(cmd("chan", "Chặn lại ngay"))
        put(cmd("tinco", "Đưa tin của cô lên tablet: /tinco Mai kiểm tra Toán"))
        put(cmd("bo", "Mở toàn bộ máy, kèm số phút nếu muốn: /bo 45"))
        put(cmd("choxoa", "Tắt quản trị thiết bị để gỡ app"))
        put(cmd("xoapin", "Xoá mã PIN để đặt lại trên tablet"))
        put(cmd("trogiup", "Xem lại toàn bộ lệnh"))
    }

    /**
     * Dau cua danh sach lenh, de biet co phai khai bao lai khong.
     *
     * Truoc day cho nay lay so ban cua app. Nhung so ban nam trong build.gradle va
     * gan nhu khong ai nho tang, nen sua danh sach lenh xong thi menu ben Telegram
     * van la danh sach cu mai mai. Lay dau tu chinh noi dung thi sua la doi.
     */
    fun dauLenh(): Int = danhSachLenh().toString().hashCode()

    /**
     * Khai bao menu lenh voi Telegram. Sau lenh nay, bo go dau / trong chat la
     * Telegram tu hien danh sach kem mo ta, khoi phai nho.
     *
     * Nem loi ra ngoai chu khong nuot: ben goi can biet that bai de con thu lai
     * lan sau, khong thi mat mang mot lan la menu khong bao gio duoc khai.
     */
    fun setMyCommands() {
        val payload = JSONObject().put("commands", danhSachLenh())
        call("setMyCommands", payload.toJsonBody())
    }

    private fun cmd(command: String, description: String) =
        JSONObject().put("command", command).put("description", description)

    /** Kiem tra token va lay thong tin bot, dung o man cai dat. */
    fun getMe(): JSONObject = call("getMe", JSONObject().toJsonBody())

    private fun call(method: String, body: okhttp3.RequestBody): JSONObject {
        val req = Request.Builder()
            .url("$BASE$token/$method")
            .post(body)
            .build()
        shortClient.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body.string())
            if (!json.optBoolean("ok", false)) {
                throw ApiException(
                    json.optString("description", "khong ro"),
                    json.optInt("error_code", resp.code)
                )
            }
            return json.optJSONObject("result") ?: JSONObject()
        }
    }

    /** Ban tra ve mang, dung cho sendMediaGroup. */
    private fun callArray(method: String, body: okhttp3.RequestBody): JSONArray {
        val req = Request.Builder()
            .url("$BASE$token/$method")
            .post(body)
            .build()
        shortClient.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body.string())
            if (!json.optBoolean("ok", false)) {
                throw ApiException(
                    json.optString("description", "khong ro"),
                    json.optInt("error_code", resp.code)
                )
            }
            return json.optJSONArray("result") ?: JSONArray()
        }
    }

    private fun JSONObject.toJsonBody() =
        toString().toRequestBody("application/json; charset=utf-8".toMediaType())

    companion object {
        private const val BASE = "https://api.telegram.org/bot"

        /** Telegram de file o ten mien khac cho goi lenh. */
        private const val FILE_GOC = "https://api.telegram.org/file/bot"

        /** Telegram giu ket noi toi da chung nay giay neu chua co update. */
        /**
         * Hoi Telegram xong thi nam cho bao lau truoc khi no tra ve rong.
         *
         * Lenh Ba Huy go van toi tuc thi: co tin la Telegram tra loi ngay, khong
         * doi het khoang nay. So nay chi quyet dinh bao lau phai bat tay lai mot
         * lan khi khong co gi - 25 giay la 144 lan moi gio, 45 giay con 80 lan.
         */
        const val POLL_TIMEOUT_SEC = 45

        /**
         * Ban phim duoi anh bai tap. callback_data chi duoc 1-64 byte theo dac ta,
         * nen ma yeu cau phai ngan.
         */
        /**
         * Ban phim duoi anh bai tap.
         *
         * Hang tren la so phut mac dinh, la cai ba bam trong chin lan muoi. Hang
         * duoi cho chon nhanh so khac, de khong phai go /duyet 45 bang mot tay
         * trong luc dang lam viec khac. So phut di kem trong callback_data.
         */
        /**
         * Ban phim thay the sau khi AI cham xong, khi may KHONG tu duyet.
         *
         * Nut to mang dung con so AI vua tinh. Ban phim cu chi co con so mac dinh -
         * thuong la 60 phut - vi luc gan no vao tin nop bai thi chua ai cham gi. Ba
         * Huy doc thay "AI tinh 3 phut" roi nhin xuong thay nut "Duyet 60 phut" thi
         * hoac phai go tay /duyet 3, hoac tac lua cho qua tay hai muoi lan.
         */
        fun banPhimSauCham(requestId: String, phutAi: Int): JSONObject {
            val rows = JSONArray()
            rows.put(JSONArray().put(JSONObject().apply {
                put("text", "Duyệt $phutAi phút (máy tính)")
                put("callback_data", "a:$requestId:$phutAi")
            }))
            val khac = listOf(15, 30, 45, 60, 90).filter { it != phutAi }.take(4)
            rows.put(JSONArray().apply {
                khac.forEach { phut ->
                    put(JSONObject().apply {
                        put("text", "$phut'")
                        put("callback_data", "a:$requestId:$phut")
                    })
                }
            })
            rows.put(JSONArray().put(JSONObject().apply {
                put("text", "Không duyệt")
                put("callback_data", "r:$requestId")
            }))
            return JSONObject().put("inline_keyboard", rows)
        }

        fun approvalKeyboard(requestId: String, minutes: Int): JSONObject {
            val rows = JSONArray()

            rows.put(JSONArray().put(JSONObject().apply {
                put("text", "Duyệt $minutes phút")
                put("callback_data", "a:$requestId")
            }))

            // Bo di lua chon trung voi so mac dinh, khong de hai nut giong nhau.
            val khac = listOf(15, 30, 45, 60, 90).filter { it != minutes }.take(4)
            rows.put(JSONArray().apply {
                khac.forEach { phut ->
                    put(JSONObject().apply {
                        put("text", "$phut'")
                        put("callback_data", "a:$requestId:$phut")
                    })
                }
            })

            rows.put(JSONArray().put(JSONObject().apply {
                put("text", "Không duyệt")
                put("callback_data", "r:$requestId")
            }))

            return JSONObject().put("inline_keyboard", rows)
        }
    }
}
