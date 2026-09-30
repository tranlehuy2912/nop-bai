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
     * Hong thi thu lai hai lan (sau 20 giay, roi mot phut), vi hong thuong la dung luc mat
     * mang. Van hong thi thoi: trang da nam trong [NhacBai] va may van nhac, chi la Ba Huy
     * khong nhan duoc tin nay. Xong thi xoa anh, gui duoc hay khong cung vay: [NhacBai]
     * khong giu anh, va khong co nut gui lai.
     */
    fun guiNen(context: Context, d: VoDanDo.DanDo, sua: Boolean = false) {
        val ct = context.applicationContext
        if (!Prefs.get(ct).isConfigured) {
            boAnh(d)
            return
        }
        scope.launch {
            for (cho in listOf(0L, 20_000L, 60_000L)) {
                if (cho > 0L) delay(cho)
                if (runCatching { send(ct, d, sua = sua) }.isSuccess) break
            }
            boAnh(d)
        }
    }

    private fun boAnh(d: VoDanDo.DanDo) {
        d.anh?.let { runCatching { File(it).delete() } }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * [shrink] de test tat di duoc. Ham nay chan luong goi - nguoi goi tu dua sang
     * luong nen.
     */
    fun send(context: Context, d: VoDanDo.DanDo, shrink: Boolean = true, sua: Boolean = false) {
        val prefs = Prefs.get(context)
        val client = TelegramClient(prefs.botToken)
        val chuThich = chuThich(d, context.getString(R.string.child_name), sua)

        val goc = d.anh?.let { File(it) }?.takeIf { it.exists() }
        if (goc == null) {
            client.sendMessage(prefs.parentChatId, chuThich)
            return
        }
        val guiDi = if (shrink) ImageUtil.shrinkInPlace(goc) else goc
        client.sendPhoto(prefs.parentChatId, guiDi, chuThich, null)
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
     *
     * [sua] la con mo trang da luu ra sua, khong chup anh moi. Luc do tin khong co anh
     * (anh goc nam o tin lan dau), va [VoDanDo.Dong.mayTich] cua moi dong la o tich cua
     * lan luu truoc, nen phan ke cho sua la so voi lan luu truoc chu khong so voi may.
     *
     * Trang con tu go ([VoDanDo.NGUON_TU_GO]) thi khong co ban may doc de so: dong nao
     * cung la con go, nen noi mot cau o dau tin thay cho danh sach "tự thêm dòng".
     */
    fun chuThich(d: VoDanDo.DanDo, con: String = "Lê Hòa", sua: Boolean = false): String = buildString {
        val ngay = d.ngayDoc()
        val gio = SimpleDateFormat("HH:mm 'ngày' dd/MM", Locale("vi", "VN")).format(Date())
        val tuGo = d.nguon == VoDanDo.NGUON_TU_GO
        append("📒 ").append(con)
        append(
            when {
                sua -> " sửa lại vở dặn dò"
                tuGo -> " tự gõ vở dặn dò"
                else -> " soát xong vở dặn dò"
            }
        )
        if (ngay != null) append(" ngày ${ngay.dayOfMonth}/${ngay.monthValue}")
        append(".\nLúc ").append(gio).append('.')
        if (tuGo && !sua) append("\nMáy không đọc được ảnh, ").append(con).append(" nhìn vở gõ lại từng dòng.")

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
            khac.forEach { append("\n• ").append(kem(it)) }
        }
        if (hanCua.isNotEmpty()) {
            append("\n\nMáy nhắc ").append(con).append(" từ hôm trước buổi đó.")
        }

        val doiTich = d.cacDong.filter { it.conSua }
        val tuThem = if (tuGo && !sua) emptyList() else d.cacDong.filter { it.mayTich == null }
        if (doiTich.isNotEmpty() || tuThem.isNotEmpty()) {
            append("\n\n").append(con).append(if (sua) " sửa so với lần lưu trước:" else " sửa khác máy đọc:")
            doiTich.forEach {
                append(if (it.laBaiTap) "\n+ tính là bài tập: " else "\n− bỏ khỏi bài tập: ")
                append(it.chu)
            }
            tuThem.forEach { append("\n+ tự thêm dòng: ").append(it.chu) }
        }
    }
}
