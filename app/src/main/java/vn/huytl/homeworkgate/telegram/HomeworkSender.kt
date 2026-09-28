package vn.huytl.homeworkgate.telegram

import android.content.Context
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.CaptureStage
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.ui.ImageUtil
import java.io.File
import java.io.IOException
import java.security.SecureRandom

/**
 * Gui mot lan nop bai len Telegram.
 *
 * Tach khoi man hinh chup vi hai ly do: chay duoc trong bai test ma khong can
 * camera that, va vi day la doan de sai nhat trong ca app - sai thi ba khong thay
 * anh, hoac thay anh ma khong co nut bam.
 */
object HomeworkSender {

    /**
     * Ket qua mot lan gui, de ben goi ghi vao [vn.huytl.homeworkgate.data.GateStore].
     *
     * [anh] la ma cac tam vua gui len Telegram. App Bang dieu khien cam ma nay la
     * tai lai duoc chinh nhung tam do, nen anh bai tap khong phai luu them mot ban
     * nao o cho khac.
     */
    data class Sent(
        val requestId: String,
        val messageId: Long,
        val anh: List<DongBo.Anh> = emptyList()
    )

    /**
     * Gui hong giua chung: [daLen] la cac buoc anh da len Telegram truoc luc hong, [cho] la
     * cho hong ("ảnh bài giải", "tin có nút duyệt").
     *
     * Can biet de ghi vao nhat ky. Lan 21:13 ngay 28/9/2026 album bai giai da len Telegram
     * ma tin co nut duyet khong di, tablet bo ca lan nop, Ba Huy thay anh ma Bang dieu
     * khien khong co bai, va khong ai biet hong o dau.
     */
    class GuiHong(val daLen: List<CaptureStage>, val cho: String, cause: Throwable) :
        IOException("$cho: ${cause.message}", cause)

    /**
     * Moi nhom gui rieng, co nhan hieu o dong dau, de ba luot mot cai la biet dau
     * la de bai dau la bai lam. Sau ba nhom moi gui mot tin mang hai nut duyet,
     * vi Telegram khong cho gan ban phim vao album anh.
     *
     * [shrink] de test tat di duoc: anh test ve san co san kich thuoc vua roi.
     * Ham nay chay dong bo, nguoi goi tu dua sang luong nen.
     */
    fun send(
        context: Context,
        groups: Map<CaptureStage, List<File>>,
        shrink: Boolean = true
    ): Sent {
        require(groups[CaptureStage.BAI_GIAI]?.isNotEmpty() == true) {
            "phai co it nhat mot tam bai giai"
        }

        val prefs = Prefs.get(context)
        val client = TelegramClient(prefs.botToken)
        val requestId = newRequestId()

        val maAnh = mutableListOf<DongBo.Anh>()
        val daLen = mutableListOf<CaptureStage>()

        // Duyet theo thu tu enum chu khong theo thu tu map truyen vao, de tin nhan
        // luon la dan do truoc, de bai giua, bai giai cuoi.
        CaptureStage.entries.forEach { stage ->
            val files = groups[stage].orEmpty()
            if (files.isEmpty()) return@forEach

            val ready = mutableListOf<File>()
            val guiXong = try {
                // Bai giai giu to hon: do la tam Ba Huy phai soi khi may doc nham chu.
                files.mapTo(ready) {
                    when {
                        !shrink -> it
                        stage == CaptureStage.BAI_GIAI -> ImageUtil.shrinkBaiGiai(it)
                        else -> ImageUtil.shrinkInPlace(it)
                    }
                }
                val caption = "${context.getString(stage.labelRes)} · ${ready.size} trang"
                if (ready.size == 1) {
                    client.sendPhoto(prefs.parentChatId, ready[0], caption, null)
                } else {
                    client.sendPhotoAlbum(prefs.parentChatId, ready, caption)
                }
            } catch (e: Exception) {
                throw GuiHong(daLen.toList(), "ảnh " + context.getString(stage.labelRes).lowercase(), e)
            } finally {
                // Chi xoa ban da thu nho. shrinkInPlace tra ve chinh file goc khi khong
                // giai ma duoc anh - xoa luc do la mat anh that, va phan cham bai sau do
                // khong con gi de doc. Gui hong cung xoa: anh goc con giu de gui lai.
                if (shrink) ready.forEachIndexed { i, f -> if (f != files[i]) f.delete() }
            }
            guiXong.fileIds.forEach { maAnh += DongBo.Anh(it, stage.name) }
            daLen += stage
        }

        val summary = CaptureStage.entries
            .filter { groups[it].orEmpty().isNotEmpty() }
            .joinToString(", ") {
                "${context.getString(it.labelRes).lowercase()} ${groups.getValue(it).size} trang"
            }

        val messageId = try {
            client.sendMessage(
                chatId = prefs.parentChatId,
                text = "${context.getString(R.string.child_name)} nộp bài: $summary.\n" +
                    dongGio(context),
                replyMarkup = TelegramClient.approvalKeyboard(requestId)
            )
        } catch (e: Exception) {
            throw GuiHong(daLen.toList(), "tin có nút duyệt", e)
        }
        return Sent(requestId, messageId, maAnh)
    }

    /**
     * Mot dong cho nhat ky khi gui hong: anh nao da len Telegram, hong o dau, vi sao.
     *
     * Vi du "ảnh bài giải đã lên Telegram, hỏng ở tin có nút duyệt: Telegram không trả
     * lời kịp (SocketTimeoutException)". Loi goc cua may giu nguyen tieng Anh trong ngoac.
     */
    fun moTaHong(context: Context, e: Throwable): String {
        val hong = e as? GuiHong ?: return lyDo(e)
        val len = hong.daLen.joinToString(", ") { context.getString(it.labelRes).lowercase() }
        val truoc = if (len.isEmpty()) "chưa lên ảnh nào" else "ảnh $len đã lên Telegram"
        return "$truoc, hỏng ở ${hong.cho}: ${lyDo(hong.cause ?: hong)}"
    }

    private fun lyDo(e: Throwable): String {
        val ten = e.javaClass.simpleName
        return when (e) {
            is TelegramClient.ApiException -> "Telegram báo lỗi ${e.errorCode}: ${e.description}"
            is java.net.SocketTimeoutException -> "Telegram không trả lời kịp ($ten)"
            is java.net.UnknownHostException -> "không tìm ra máy chủ Telegram, có thể mất mạng ($ten)"
            is java.net.ConnectException -> "không nối được tới Telegram ($ten)"
            is IOException -> "mạng đứt giữa chừng ($ten: ${e.message})"
            else -> "$ten: ${e.message}"
        }
    }

    /**
     * Dong noi ve gio o cuoi tin nop bai.
     *
     * Con dang giu mot phieu chua dung ma nop them bai thi duyet la cong don. Ghi ro
     * so dang giu, de Ba Huy biet minh dang duyet cai gi truoc khi bam.
     */
    private fun dongGio(context: Context): String {
        val gate = GateStore(context)
        val giu = gate.grantedMinutes
        val dong = if (giu > 0) {
            "Đang giữ $giu phút, duyệt bao nhiêu phút thì cộng dồn vào đó."
        } else {
            "Duyệt thì bấm số phút."
        }
        // Ham nay chay truoc khi bai moi duoc xep vao hang, nen cong mot cho chinh no.
        // Ba can biet minh dang nhin bai thu may, vi moi bai mot nut duyet rieng va
        // duyet bai nao thi cong gio bai do.
        val thu = gate.soBaiDangCho() + 1
        return if (thu > 1) "$dong Đây là bài thứ $thu đang chờ duyệt." else dong
    }

    /** Ma ngan de khop voi callback_data, vi Telegram chi cho 64 byte. */
    private fun newRequestId(): String {
        val bytes = ByteArray(4).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
