package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Ai nhan cau do. */
enum class ChatFrom { CON, BA }

/**
 * Mot cau trong khung chat.
 *
 * @param anh duong dan tam anh di kem, hay null neu chi co chu. Anh nam trong
 *   [ChatBox.thuMucAnh] chu khong phai cache: mot tam anh Ba Huy gui ma may tu don
 *   mat sau vai ngay thi con mo chat ra chi con cai khung trong.
 */
data class ChatLine(
    val from: ChatFrom,
    val text: String,
    val at: Long,
    val anh: String? = null
)

/**
 * Khung chat giua Le Hoa va Ba Huy, di nho duong Telegram.
 *
 * Chi giu [MAX_LINES] cau gan nhat: cau thu 51 day cau cu nhat ra khoi may han,
 * khong phai giau di. Day khong phai app nhan tin, no chi de con bao mot viec gap:
 * quen vo o lop, may sap het pin, bai kho qua. Giu ca lich su lau dai thi thanh
 * mot thu khac han, va lai nam trong may ma con dang cam.
 *
 * Cat trong cung cho da ma hoa voi phan cau hinh, vi tin nhan cua tre con cung la
 * chuyen rieng, khong nen de ai cam may len la doc duoc.
 */
object ChatBox {

    private const val K_LINES = "chat_lines"
    private const val K_READ_AT = "chat_read_at"
    private const val K_LAST_SENT = "chat_last_sent"
    private const val K_WAIT_UNTIL = "chat_wait_until"

    /**
     * Toi da bay nhieu cau. Vuot qua thi cau cu nhat bi day han ra khoi may.
     *
     * Khong dat them han theo ngay: mot cau ba nhan tuan truoc van la cau ba nhan,
     * khong co ly do gi de may tu xoa no trong khi hop con thua cho. Chi khi day len
     * hon mot tram thi cau cu nhat moi phai nhuong cho.
     *
     * Len mot tram tu ban co anh trong chat: mot lan con hoi bai co the la ba bon
     * tam anh lien nhau, nen nam muoi dong het nhanh hon han hoi con toan chu.
     */
    private const val MAX_LINES = 100

    /** Con nhan xong thi giu duong day mo bay nhieu lau de cho ba tra loi. */
    const val WAIT_WINDOW_MS = 20 * 60_000L

    /**
     * Chi de chan mot cu bam doi thanh hai tin giong het nhau. Khong phai han muc.
     *
     * Truoc day cho nay la 20 giay va 30 tin mot ngay. Bo di, vi khong co ly do nao
     * dung duoc: nguoi nhan chi co mot la Ba Huy, con nhan nhieu qua thi do la
     * chuyen giua hai cha con, khong phai viec cua may moc. Doi lai, cai gia khi
     * dat sai la mot dua tre can bao gap ma bi phan mem chan lai.
     */
    const val COOLDOWN_MS = 3_000L

    fun add(
        context: Context,
        from: ChatFrom,
        text: String,
        now: Long = System.currentTimeMillis(),
        anh: String? = null
    ) {
        val lines = read(context).toMutableList()
        lines.add(ChatLine(from, text.take(500), now, anh))
        ghi(context, lines.takeLast(MAX_LINES))
    }

    /**
     * Cho de anh di kem tin nhan.
     *
     * Trong filesDir chu khong phai cacheDir: Android don cache bat cu luc nao no
     * thay chat o dia, ma mot tam anh bai tap Ba Huy gui thi con co the mo lai xem
     * ca tuan sau.
     */
    fun thuMucAnh(context: Context): java.io.File =
        java.io.File(context.filesDir, "chat_anh").apply { mkdirs() }

    fun read(context: Context): List<ChatLine> = docThoDay(context).takeLast(MAX_LINES)

    /**
     * Xoa han phan vuot qua [MAX_LINES] khoi may.
     *
     * [read] da cat san nen man hinh khong bao gio hien qua nam muoi cau, nhung cat
     * chi la khong hien - chu van con nam trong o dia neu ban app cu tung ghi nhieu
     * hon. Ham nay moi that su xoa, va chi ghi lai khi co gi de xoa.
     */
    fun donDep(context: Context, now: Long = System.currentTimeMillis()) {
        val tatCa = docThoDay(context)
        if (tatCa.size <= MAX_LINES) return
        ghi(context, tatCa.takeLast(MAX_LINES))
    }

    /** Xoa sach khung chat. Cho luc Ba Huy muon don han. */
    fun xoaHet(context: Context) {
        Prefs.get(context).raw().edit().remove(K_LINES).putLong(K_READ_AT, 0L).commit()
    }

    /** Doc nguyen xi nhung gi dang nam trong may, ke ca cau da qua han. */
    private fun docThoDay(context: Context): List<ChatLine> {
        val raw = Prefs.get(context).raw().getString(K_LINES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                ChatLine(
                    from = ChatFrom.valueOf(o.getString("f")),
                    text = o.getString("t"),
                    at = o.getLong("a"),
                    anh = o.optString("i").takeIf { it.isNotBlank() }
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun ghi(context: Context, lines: List<ChatLine>) {
        val array = JSONArray()
        lines.forEach {
            array.put(
                JSONObject()
                    .put("f", it.from.name)
                    .put("t", it.text)
                    .put("a", it.at)
                    .put("i", it.anh)
            )
        }
        Prefs.get(context).raw().edit().putString(K_LINES, array.toString()).commit()
        donAnhMoCoi(context, lines)
    }

    /**
     * Xoa nhung tam anh khong con cau nao tro toi.
     *
     * Cau thu 51 day cau cu nhat ra khoi danh sach, nhung tam anh cua no thi van nam
     * trong o dia mai mai neu khong co cho nay. Quet theo thu muc chu khong theo cau
     * vua bi day ra: cach do don duoc ca nhung tam sot lai tu ban cu hay tu mot lan
     * ghi hong giua chung.
     */
    private fun donAnhMoCoi(context: Context, lines: List<ChatLine>) {
        val conDung = lines.mapNotNull { it.anh }.toSet()
        runCatching {
            thuMucAnh(context).listFiles()?.forEach { f ->
                if (f.absolutePath !in conDung) f.delete()
            }
        }
    }

    /** So cau cua ba ma con chua mo ra xem. */
    fun unread(context: Context): Int {
        val readAt = Prefs.get(context).raw().getLong(K_READ_AT, 0L)
        return read(context).count { it.from == ChatFrom.BA && it.at > readAt }
    }

    fun markRead(context: Context) {
        Prefs.get(context).raw().edit()
            .putLong(K_READ_AT, System.currentTimeMillis())
            .commit()
    }

    /**
     * Con co duoc nhan ngay bay gio khong. Tra ve null neu duoc, hoac cau giai
     * thich de hien thang len man hinh.
     */
    fun blockReason(context: Context, now: Long = System.currentTimeMillis()): String? {
        val sp = Prefs.get(context).raw()
        val last = sp.getLong(K_LAST_SENT, 0L)
        if (now - last < COOLDOWN_MS) return "Vừa gửi rồi, chờ một chút nhé"
        return null
    }

    /** Ghi nhan con vua nhan xong, va mo duong day cho ba tra loi. */
    fun noteSent(context: Context, now: Long = System.currentTimeMillis()) {
        val sp = Prefs.get(context).raw()
        sp.edit()
            .putLong(K_LAST_SENT, now)
            .putLong(K_WAIT_UNTIL, now + WAIT_WINDOW_MS)
            .commit()
    }

    /**
     * Dang cho ba tra loi hay khong.
     *
     * [vn.huytl.homeworkgate.telegram.ApprovalService] binh thuong tu tat khi cong
     * khoa, de khong giu ket noi mang suot ngay. Nhung con vua nhan xong ma service
     * tat thi cau tra loi cua ba khong bao gio toi noi, nen phai giu no song them
     * mot khoang.
     */
    fun isWaiting(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        now < Prefs.get(context).raw().getLong(K_WAIT_UNTIL, 0L)

    /** Ba tra loi roi thi khong can giu duong day nua. */
    fun stopWaiting(context: Context) {
        Prefs.get(context).raw().edit().remove(K_WAIT_UNTIL).commit()
    }

    private fun dayKeyOf(now: Long): Int {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        return cal.get(java.util.Calendar.YEAR) * 10_000 +
            (cal.get(java.util.Calendar.MONTH) + 1) * 100 +
            cal.get(java.util.Calendar.DAY_OF_MONTH)
    }
}
