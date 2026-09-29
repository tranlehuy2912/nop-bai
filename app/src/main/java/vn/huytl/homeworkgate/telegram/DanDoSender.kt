package vn.huytl.homeworkgate.telegram

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.NhacBai
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinhLoiNhac
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
 * Tach thanh mot buoc rieng la ba mat duong nhin. Gui anh kem danh sach thi ba liec mot
 * cai la doi chieu duoc chu con tich voi chu tren giay.
 *
 * MOI DONG GHI KEM BUOI SE NHAC (tu 30/9/2026). Vo dan do dung de nhac bai theo tiet sau
 * cua tung mon, xem [NhacBai]. May doc nham ten mon thi nhac sai buoi, va dong nay la cho
 * Ba Huy thay ngay. Truoc do ngay khong co bai tap thi tin nay gan nut Duyet 45 phut; bo
 * tron goi thi bo luon nut.
 */
object DanDoSender {

    /**
     * Gui o luong nen, khong cho ket qua. Chep [Notifier]: scope rieng cua object
     * chu khong lifecycleScope, vi man soat dong ngay sau khi con bam Luu - buoc
     * vao lifecycleScope la tin bi huy giua chung.
     *
     * Hong thi thu lai hai lan (sau 20 giay, roi mot phut), vi may doc vo hong thuong
     * la dung luc mat mang. Van hong thi ghi lai [guiHong], de man vo dan do bao con
     * va cho gui lai. Truoc day loi bi nuot: ban vo chi co anh mat ma anh ca ngay, ba
     * khong nhan duoc gi ma man van ghi "Ảnh đã gửi ba Huy".
     */
    fun guiNen(context: Context, d: VoDanDo.DanDo) {
        val ct = context.applicationContext
        if (!Prefs.get(ct).isConfigured) return
        sp(ct).edit().remove(K_HONG_LUC).apply()
        dangGuiLuc = d.luc
        scope.launch {
            var xong = false
            for (cho in listOf(0L, 20_000L, 60_000L)) {
                if (cho > 0L) delay(cho)
                xong = runCatching { send(ct, d) }.isSuccess
                if (xong) break
            }
            if (dangGuiLuc == d.luc) dangGuiLuc = 0L
            if (!xong) sp(ct).edit().putLong(K_HONG_LUC, d.luc).apply()
        }
    }

    /** Dang gui ban vo chup luc [luc] (con trong lan thu). */
    fun dangGui(luc: Long): Boolean = luc != 0L && dangGuiLuc == luc

    /** Ban vo chup luc [luc] da thu het ma van khong gui duoc. */
    fun guiHong(context: Context, luc: Long): Boolean =
        luc != 0L && sp(context).getLong(K_HONG_LUC, 0L) == luc

    @Volatile
    private var dangGuiLuc = 0L

    private const val K_HONG_LUC = "gui_hong_luc"

    private fun sp(context: Context) =
        context.applicationContext.getSharedPreferences("dando_sender", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * [shrink] de test tat di duoc. Ham nay chan luong goi - nguoi goi tu dua sang
     * luong nen.
     */
    fun send(context: Context, d: VoDanDo.DanDo, shrink: Boolean = true) {
        val prefs = Prefs.get(context)
        val client = TelegramClient(prefs.botToken)
        val chuThich = chuThich(d, context.getString(R.string.child_name))

        val goc = d.anh?.let { File(it) }?.takeIf { it.exists() }
        if (goc == null) {
            client.sendMessage(prefs.parentChatId, chuThich)
            return
        }
        val guiDi = if (shrink) ImageUtil.shrinkInPlace(goc) else goc
        val daGui = client.sendPhoto(prefs.parentChatId, guiDi, chuThich, null)
        // Giu ma anh de Bang dieu khien tai lai dung tam nay khi Ba Huy nho Claude doc.
        // Xem [VoDanDo.DanDo.fileId].
        daGui.fileIds.firstOrNull()?.let { VoDanDo.ghiMaAnh(context, d.luc, it) }
        // shrinkInPlace tra ve chinh file goc khi khong giai ma duoc anh; xoa luc do
        // la mat ban duy nhat trong may.
        if (guiDi != goc) guiDi.delete()
    }

    /**
     * Chu di kem anh.
     *
     * KE RIENG CHO CON SUA KHAC MAY. Do khong phai de bat loi con; do la cho duy
     * nhat trong ca duong nay ma mot nguoi co the doi loi nhac bang cach go tay, nen
     * no phai hien ra chu khong lang le troi qua.
     */
    fun chuThich(d: VoDanDo.DanDo, con: String = "Lê Hòa"): String = buildString {
        val ngay = d.ngayDoc()
        val gio = SimpleDateFormat("HH:mm 'ngày' dd/MM", Locale("vi", "VN")).format(Date())
        if (d.chuaDoc) {
            // May doc khong duoc, con gui anh sang. Chua ai doc thi chua co gi de nhac.
            append("📒 ").append(con).append(" chụp vở dặn dò lúc ").append(gio).append('.')
            append("\n\nMáy chưa đọc được trang này. Ba Huy mở Bảng điều khiển, chạm thẻ vở ")
            append("dặn dò ở tab Bảng để nhờ Claude đọc. Đọc xong thì máy nhắc bài theo trang này.")
            return@buildString
        }
        append("📒 ")
        if (d.nguon == VoDanDo.NGUON_CLAUDE) append("Claude đọc vở dặn dò")
        else append(con).append(" soát xong vở dặn dò")
        if (ngay != null) append(" ngày ${ngay.dayOfMonth}/${ngay.monthValue}")
        append(".\nLúc ").append(gio).append('.')

        // Buoi se nhac cua tung dong, tinh y nhu [NhacBai] tinh.
        val hanCua = ngay?.let { n ->
            NhacBai.cacMuc(listOf(NhacBai.Trang(n, d.cacDong))).associateBy { it.chu }
        }.orEmpty()
        fun kem(chu: String): String = hanCua[chu]?.let {
            "$chu → ${TinhLoiNhac.moTaBuoi(it.buoi)} ${it.ngayHan.dayOfMonth}/${it.ngayHan.monthValue}"
        } ?: chu

        if (d.cacBai.isEmpty()) {
            append("\n\nHôm đó cô KHÔNG giao bài tập nào.")
        } else {
            append("\n\nBài phải làm rồi nộp:")
            d.cacBai.forEach { append("\n• ").append(kem(it)) }
        }

        val khac = d.dongKhac
        if (khac.isNotEmpty()) {
            append("\n\nDặn dò khác:")
            khac.forEach { append("\n· ").append(kem(it)) }
        }
        if (hanCua.isNotEmpty()) {
            append("\n\nMáy nhắc ").append(con).append(" từ hôm trước buổi đó.")
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
    }
}
