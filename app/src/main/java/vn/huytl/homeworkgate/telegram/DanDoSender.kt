package vn.huytl.homeworkgate.telegram

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.ui.ImageUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gui trang vo dan do Le Hoa vua soat cho Ba Huy.
 *
 * TACH KHOI [HomeworkSender] chu khong them mot nhanh vao trong do, y het ly do cua
 * [CapSachSender]: nop bai thi sinh ra mot yeu cau cho duyet gio choi, con day chi
 * la mot trang vo de ba doi chieu. Cho chung mot ham thi som muon cung co ngay mot
 * trang vo dan do di lac vao duong duyet gio.
 *
 * VI SAO PHAI GUI. Truoc day trang vo di kem moi lan nop bai nen ba van thay no.
 * Tach thanh mot buoc rieng la ba mat duong nhin, ma o tich tren man do lai la thu
 * quyet dinh tron goi 45 phut. Gui anh kem danh sach thi ba liec mot cai la doi
 * chieu duoc chu con tich voi chu tren giay.
 *
 * NGAY KHONG CO BAI TAP THI GAN THEM NUT. Ngay do khong co tron goi tu dong - luat
 * doi phai co bai tap duoc giao, xem [LuatCongGio]. Nhung con van phai on bai hay
 * hoc thuoc, nen quyen cho 45 phut do chuyen sang tay Ba Huy, bam ngay duoi tin nay.
 */
object DanDoSender {

    /** Tien to callback_data: duyet tron goi cho ngay do. */
    const val MA_DUYET = "dd:"

    /** Tien to callback_data: khong duyet. */
    const val MA_TU_CHOI = "ddk:"

    /**
     * Gui o luong nen, khong cho ket qua. Chep [Notifier]: scope rieng cua object
     * chu khong lifecycleScope, vi man soat dong ngay sau khi con bam Luu - buoc
     * vao lifecycleScope la tin bi huy giua chung.
     */
    fun guiNen(context: Context, d: VoDanDo.DanDo) {
        val ct = context.applicationContext
        if (!Prefs.get(ct).isConfigured) return
        scope.launch { runCatching { send(ct, d) } }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * [shrink] de test tat di duoc. Ham nay chan luong goi - nguoi goi tu dua sang
     * luong nen.
     */
    fun send(context: Context, d: VoDanDo.DanDo, shrink: Boolean = true) {
        val prefs = Prefs.get(context)
        val client = TelegramClient(prefs.botToken)
        val chuThich = chuThich(d, context.getString(R.string.child_name))
        // Ban chi co anh thi cacBai rong vi chua ai doc, khong phai co khong giao bai.
        val banPhim = if (!d.chuaDoc && d.cacBai.isEmpty()) banPhimDuyet(d.ngay) else null

        val goc = d.anh?.let { File(it) }?.takeIf { it.exists() }
        if (goc == null) {
            client.sendMessage(prefs.parentChatId, chuThich, banPhim)
            return
        }
        val guiDi = if (shrink) ImageUtil.shrinkInPlace(goc) else goc
        val daGui = client.sendPhoto(prefs.parentChatId, guiDi, chuThich, banPhim)
        // Giu ma anh de moi bai nop sau do mang theo dung tam nay, cho Claude doi chieu
        // danh sach con tich voi chu tren giay. Xem [VoDanDo.DanDo.fileId].
        daGui.fileIds.firstOrNull()?.let { VoDanDo.ghiMaAnh(context, d.luc, it) }
        // shrinkInPlace tra ve chinh file goc khi khong giai ma duoc anh; xoa luc do
        // la mat ban duy nhat trong may.
        if (guiDi != goc) guiDi.delete()
    }

    /**
     * Chu di kem anh.
     *
     * KE RIENG CHO CON SUA KHAC MAY. Do khong phai de bat loi con; do la cho duy
     * nhat trong ca duong nay ma mot nguoi co the doi so phut bang cach go tay, nen
     * no phai hien ra chu khong lang le troi qua.
     */
    fun chuThich(d: VoDanDo.DanDo, con: String = "Lê Hòa"): String = buildString {
        val ngay = d.ngayDoc()
        val gio = SimpleDateFormat("HH:mm 'ngày' dd/MM", Locale("vi", "VN")).format(Date())
        if (d.chuaDoc) {
            // May doc khong duoc, con gui anh sang. Noi ro hai duong doc de Ba Huy biet
            // khong can lam gi ngay: lan Nho Claude cham dau tien cung doc duoc.
            append("📒 ").append(con).append(" chụp vở dặn dò lúc ").append(gio).append('.')
            append("\n\nMáy chưa đọc được trang này. Ba Huy mở Bảng điều khiển, chạm thẻ vở ")
            append("dặn dò ở tab Bảng để nhờ Claude đọc. Không đọc riêng thì lần Nhờ Claude ")
            append("chấm đầu tiên sẽ đọc luôn.")
            return@buildString
        }
        append("📒 ")
        if (d.nguon == VoDanDo.NGUON_CLAUDE) append("Claude đọc vở dặn dò")
        else append(con).append(" soát xong vở dặn dò")
        if (ngay != null) append(" ngày ${ngay.dayOfMonth}/${ngay.monthValue}")
        append(".\nLúc ").append(gio).append('.')

        if (d.cacBai.isEmpty()) {
            append("\n\nHôm đó cô KHÔNG giao bài tập nào.")
        } else {
            append("\n\nBài phải làm rồi nộp:")
            d.cacBai.forEach { append("\n• ").append(it) }
        }

        val khac = d.dongKhac
        if (khac.isNotEmpty()) {
            append("\n\nDặn dò khác:")
            khac.forEach { append("\n· ").append(it) }
        }

        val sua = d.cacDong.filter { it.conSua }
        val tuThem = d.cacDong.filter { it.mayTich == null }
        if (sua.isNotEmpty() || tuThem.isNotEmpty()) {
            append("\n\n").append(con).append(" sửa khác máy đọc:")
            sua.forEach {
                append(if (it.laBaiTap) "\n+ tính là bài tập: " else "\n− bỏ khỏi bài tập: ")
                append(it.chu)
            }
            tuThem.forEach { append("\n+ tự thêm dòng: ").append(it.chu) }
        }

        if (d.cacBai.isEmpty()) {
            append("\n\nHôm đó không có bài tập nên máy không tự tính trọn gói ")
            append(LuatCongGio.PHUT_TRON_GOI_DAN_DO).append(" phút. Ba Huy duyệt thì bấm nút dưới.")
        }
    }

    private fun banPhimDuyet(ngay: String): JSONObject {
        val rows = JSONArray()
        rows.put(
            JSONArray().put(
                JSONObject()
                    .put("text", "Duyệt ${LuatCongGio.PHUT_TRON_GOI_DAN_DO} phút")
                    .put("callback_data", "$MA_DUYET$ngay")
            )
        )
        rows.put(
            JSONArray().put(
                JSONObject()
                    .put("text", "Không duyệt")
                    .put("callback_data", "$MA_TU_CHOI$ngay")
            )
        )
        return JSONObject().put("inline_keyboard", rows)
    }
}
