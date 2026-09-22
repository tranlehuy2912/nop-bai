package vn.huytl.homeworkgate.telegram

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.guard.Permissions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gui tin bao len Telegram roi quen di. Khong cho ket qua, khong thu lai.
 *
 * Ly do khong lam co ban: cac tin nay deu la tin phu. Tin quan trong duy nhat
 * la anh bai tap, va anh do gui tu man hinh chup, co bao loi truoc mat con.
 */
object Notifier {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val timeFormat = SimpleDateFormat("HH:mm 'ngày' dd/MM", Locale("vi", "VN"))

    /**
     * Vang lau hon khoang nay moi bao.
     *
     * 45 phut: du dai de tablet sap nguon vai phut hay may khoi dong lai binh thuong
     * khong lam phien, du ngan de mot buoi choi len khong lot qua.
     */
    const val AWAY_SLACK_MS = 45 * 60_000L

    fun sessionEnded(context: Context, reason: EndReason) {
        DayLog.add(
            context,
            when (reason) {
                EndReason.RAN_OUT -> "Hết giờ, máy khoá lại"
                EndReason.HARD_STOP -> "Tới giờ nghỉ, máy khoá lại"
                EndReason.REBOOT -> "Máy khởi động lại giữa phiên, cắt phiên"
                EndReason.CLOCK_TAMPER -> "Đồng hồ bị đẩy lùi, cắt phiên"
                EndReason.PARENT_REVOKED -> "Phiên bị thu hồi"
                EndReason.NEVER_STARTED -> "Phiếu duyệt hết hạn, Lê Hòa không bấm Bắt đầu"
            }
        )
        val text = when (reason) {
            EndReason.RAN_OUT -> "Hết giờ chơi, tablet khoá lại."
            EndReason.HARD_STOP -> "Tới giờ nghỉ, tablet khoá lại."
            EndReason.REBOOT -> "Tablet khởi động lại giữa phiên nên cắt phiên."
            EndReason.CLOCK_TAMPER -> "⚠️ Đồng hồ tablet bị đẩy lùi giữa phiên, đã cắt phiên."
            EndReason.PARENT_REVOKED -> "Đã dừng phiên chơi."
            EndReason.NEVER_STARTED -> "Lê Hòa không bấm Bắt đầu, phiếu hết hạn. Số phút đó trả lại vào hạn mức hôm nay."
        }
        send(context, text)
    }

    fun adminDisableRequested(context: Context) {
        send(
            context,
            "⚠️ Có người mở màn hình tắt quản trị thiết bị trên tablet."
        )
    }

    fun adminDisabled(context: Context) {
        send(
            context,
            "⚠️ Quản trị thiết bị đã tắt. Từ giờ app gỡ được."
        )
    }

    /**
     * App vua vang mat mot khoang dai.
     *
     * Bat duoc ba chuyen khac nhau ma deu dan den "may khong chan gi": tablet tat
     * nguon, app bi giet lau, va khoi dong vao che do an toan. Tin nhan noi dung
     * ba kha nang do chu khong doan, vi ca ba deu de lai dau vet giong het nhau:
     * mot khoang thoi gian khong co nhip nao.
     */
    fun appWasAway(context: Context, tuWall: Long, denWall: Long) {
        val phut = (denWall - tuWall) / 60_000
        val gio = phut / 60
        val doDai = if (gio > 0) "$gio tiếng ${phut % 60} phút" else "$phut phút"
        send(
            context,
            "⚠️ App không chạy ${timeFormat.format(Date(tuWall))} → " +
                "${timeFormat.format(Date(denWall))} ($doDai), tablet không chặn gì.\n" +
                "Máy tắt nguồn, hoặc vào chế độ an toàn."
        )
    }

    /**
     * Co nguoi vua nhap dung ma PIN tren tablet.
     *
     * Chi ghi nhat ky, khong nhan tin.
     *
     * Truoc day co nhan tin moi lan. Nhung nguoi go dung PIN gan nhu luon la chinh
     * Ba Huy, va lan nao cam may lên cung nhan mot tin giong het nhau thi cuoi cung
     * la khong ai doc nua - luc co tin that cung troi qua theo. Nhap PIN cung chua
     * mo khoa gi ca: may van chan binh thuong. Luc may that su mo la luc gat cong
     * tac, va cho do van bao bang moToanBo() ben duoi.
     *
     * Van con hai duong de biet: /nhatky xem lai duoc ai vao luc may gio, va go sai
     * PIN ba lan lien tiep thi bao ngay.
     */
    fun parentModeEntered(context: Context) {
        DayLog.add(context, "Có người nhập PIN vào trang cấu hình")
    }

    /**
     * Le Hoa vua bam Bat dau, dong ho bat dau chay tu day.
     *
     * Duyet xong chua phai la dang choi: phieu duyet nam do cho den khi con bam.
     * Khong co tin nay thi Ba Huy duyet luc 4 gio ma khong biet con ngoi vao may
     * luc 4 gio hay 7 gio toi.
     */
    fun conBatDau(context: Context, phut: Int) {
        DayLog.add(context, "Lê Hòa bắt đầu chơi, $phut phút")
        send(context, "▶️ Lê Hòa bắt đầu chơi, $phut phút.")
    }

    /** Con tu bam tam dung, gio con lai duoc giu lai cho lan sau. */
    fun conTamDung(context: Context, phut: Int) {
        DayLog.add(context, "Lê Hòa tạm dừng, giữ $phut phút")
        send(context, "⏸️ Lê Hòa tạm dừng, giữ lại $phut phút.")
    }

    /**
     * Toi gio di hoc nen may tu giu phien choi lai.
     *
     * Tin rieng chu khong dung lai [conTamDung]: cau do noi la Le Hoa tu bam, ma
     * day la may tu cat. Ba Huy doc nham mot cai la tuong con biet dieu, trong khi
     * that ra no dang bi man chan day ra khoi may.
     */
    fun catVaoGioHoc(context: Context, phut: Int, moTaBuoi: String) {
        DayLog.add(context, "Tới giờ đi học, giữ lại $phut phút")
        send(
            context,
            "⏸️ Tới giờ đi học ($moTaBuoi) nên tablet giữ phiên chơi lại. " +
                "Còn $phut phút, tan học chơi tiếp được."
        )
    }

    /** Con choi tiep phan gio da giu. */
    fun conChoiTiep(context: Context, phut: Int) {
        DayLog.add(context, "Lê Hòa chơi tiếp, còn $phut phút")
        send(context, "▶️ Lê Hòa chơi tiếp, còn $phut phút.")
    }

    /**
     * Cong tac "Mo toan bo may" tren tablet vua duoc gat.
     *
     * Day moi la luc may thuc su khong chan gi nua, nen phai bao. Lenh /bo trong
     * Telegram thi khong goi ham nay, vi luc do chinh Ba Huy vua go no.
     */
    fun moToanBo(context: Context, bat: Boolean) {
        DayLog.add(context, if (bat) "Gạt mở toàn bộ máy" else "Gạt khoá lại")
        send(
            context,
            if (bat) "🔓 Tablet vừa mở toàn bộ, không chặn gì nữa."
            else "🔒 Tablet đã khoá lại, chặn bình thường."
        )
    }

    /** Ai do vua go sai ma PIN nhieu lan lien tiep tren tablet. */
    fun wrongPinAttempts(context: Context, soLan: Int) {
        DayLog.add(context, "Gõ sai PIN $soLan lần liên tiếp")
        send(
            context,
            "🔐 Gõ sai PIN $soLan lần liên tiếp trên tablet."
        )
    }

    fun guardOffline(context: Context) {
        send(
            context,
            "⚠️ Tablet khởi động lại mà dịch vụ canh app đang TẮT, máy không chặn gì.\n" +
                "Bật lại: app Nộp bài → ổ khoá góc trên phải → Cài đặt."
        )
    }

    /**
     * Bao con song. Chi gui mot tin duy nhat roi cac lan sau sua lai chinh tin do,
     * de chat khong day tin rac. Anh Huy ghim tin nay len dau chat, nhin thay gio
     * trong do dung im qua lau la biet tablet co chuyen.
     */
    /**
     * Sua lai tin ghim duy nhat trong chat, tao moi neu chua co.
     *
     * Ca nhip tim lan dong ho dem nguoc deu ghi vao dung mot tin nay. Nho vay chat
     * khong bi day tin rac: Ba Huy ghim no len dau, liec mot cai la biet tablet dang
     * the nao, khong phai go /trangthai.
     */
    fun ghimTin(context: Context, client: TelegramClient, text: String) {
        val prefs = Prefs.get(context)
        val cu = prefs.heartbeatMessageId
        val suaDuoc = cu != 0L && client.editMessageText(prefs.parentChatId, cu, text)
        if (!suaDuoc) {
            val id = client.sendMessage(prefs.parentChatId, text)
            if (id != 0L) prefs.heartbeatMessageId = id
        }
        prefs.lastHeartbeatWall = System.currentTimeMillis()
    }

    fun heartbeat(context: Context) {
        val prefs = Prefs.get(context)
        if (!prefs.isConfigured) return
        val chatId = prefs.parentChatId
        val client = TelegramClient(prefs.botToken)

        // Nhip tim phai noi ca hai viec. Truoc day no chi noi "app Lê Hòa chay", ma
        // app con chay khong co nghia la con dang bi quan ly: tat dich vu tro nang
        // di thi app van chay, van bao con song, trong khi khong chan gi nua.
        // Nhip truoc do cach day qua lau nghia la app da khong chay trong khoang do.
        val nhipTruoc = prefs.lastHeartbeatWall
        val bayGio = System.currentTimeMillis()
        val khoangCho = prefs.heartbeatHours * 60L * 60L * 1000L + AWAY_SLACK_MS
        if (nhipTruoc > 0 && bayGio - nhipTruoc > khoangCho) {
            appWasAway(context, nhipTruoc, bayGio)
        }

        // Thieu viec nang moi goi la ho. Thieu quan tri thiet bi hay thong bao thi
        // may van chan duoc, van ke ra nhung khong dat tieu de bao dong - de dat
        // moi ngay thi ba nhin mai thanh quen, den luc ho that cung troi qua.
        val thieu = Permissions.missing(context)
        val nang = thieu.count { it.nang }
        val danhSach = thieu.joinToString("\n") { (if (it.nang) "⛔ " else "⚠️ ") + it.ten }
        val text = when {
            thieu.isEmpty() -> "Tablet còn sống, đang canh. ${now()}"
            nang > 0 -> "⚠️ TABLET ĐANG HỞ — ${now()}\n$danhSach"
            else -> "Tablet còn sống, đang canh. ${now()}\n$danhSach"
        }

        scope.launch { runCatching { ghimTin(context, client, text) } }
    }

    fun send(context: Context, text: String) {
        val prefs = Prefs.get(context)
        if (!prefs.isConfigured) return
        val chatId = prefs.parentChatId
        val client = TelegramClient(prefs.botToken)
        scope.launch { runCatching { client.sendMessage(chatId, text) } }
    }

    private fun now(): String = timeFormat.format(Date())
}
