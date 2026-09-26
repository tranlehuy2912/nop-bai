package vn.huytl.homeworkgate.guard

import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import vn.huytl.homeworkgate.data.Prefs

/**
 * Dich vu doc thong bao.
 *
 * Ly do ton tai dau tien: Android chi cho doc danh sach trinh phat dang chay
 * (MediaSessionManager) neu app co mot dich vu kieu nay va nguoi dung da bat no
 * trong Settings. Do la cai gia phai tra de hoi duoc cau "app nao dang phat tieng".
 *
 * Tu 27/9/2026 no nhin them thong bao cua Telegram, va chi cua Telegram, de dem so tin
 * Ba Huy gui ma Le Hoa chua doc: nut "Nhan cho ba Huy" o man chinh ghi "Ba Huy nhan 3
 * tin moi" nhu thoi man chat cu. Xem [TinCuaBa].
 *
 * Chi doc ten goi, ma khung chat va so tin, khong doc tieu de hay noi dung. Quyen nay
 * doc duoc MOI thong bao tren may, rong hon nhieu so voi phan dang dung, nen them viec
 * gi o day thi phai can nhac lai.
 */
class TaiThongBao : NotificationListenerService() {

    /**
     * Vua noi lai: tien trinh vua song lai, hay may vua khoi dong. Dem lai tu nhung thong
     * bao dang nam tren may, vi so cu trong prefs co the da sai: con doc tin trong luc
     * dich vu nay khong chay thi khong ai bao cho no biet.
     */
    override fun onListenerConnected() {
        runCatching {
            val chatIdBa = Prefs.get(this).parentChatId
            val so = activeNotifications.orEmpty()
                .filter { it.packageName in TelegramThat.GOI }
                .sumOf {
                    ThongBaoTelegram.soTinCuaBa(it.notification.shortcutId, it.notification.number, chatIdBa)
                }
            TinCuaBa.dat(this, so)
        }.onFailure { Log.w(TAG, "khong dem lai duoc tin Telegram: ${it.message}") }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val tb = sbn ?: return
        if (tb.packageName !in TelegramThat.GOI) return
        runCatching {
            val chatIdBa = Prefs.get(this).parentChatId
            val ma = tb.notification.shortcutId
            if (!ThongBaoTelegram.laCuaBa(ma, chatIdBa)) return
            TinCuaBa.dat(this, ThongBaoTelegram.soTinCuaBa(ma, tb.notification.number, chatIdBa))
        }.onFailure { Log.w(TAG, "khong doc duoc thong bao Telegram: ${it.message}") }
    }

    /**
     * Thong bao tin cua Ba Huy mat di: Telegram tu go khi con da mo khung chat ra doc,
     * hoac con vuot bo no. Truong hop sau thi tin van chua doc ben trong Telegram, nhung
     * tu ngoai nhin vao khong con cach nao biet nua.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val tb = sbn ?: return
        if (tb.packageName !in TelegramThat.GOI) return
        runCatching {
            if (ThongBaoTelegram.laCuaBa(tb.notification.shortcutId, Prefs.get(this).parentChatId)) {
                TinCuaBa.dat(this, 0)
            }
        }
    }

    private companion object {
        const val TAG = "HomeworkGate"
    }
}

/** Doc mot thong bao cua Telegram: co phai tin cua Ba Huy khong, may tin. Tach rieng de test. */
object ThongBaoTelegram {

    /**
     * Thong bao cua khung chat rieng voi Ba Huy.
     *
     * Telegram gan cho thong bao cua moi khung chat mot ma "ndid_" + ma khung chat
     * (NotificationsController, ham createNotificationShortcut), ma khung chat rieng voi
     * mot nguoi chinh la id cua nguoi do. Telegram chi gan ma nay khi bat "bong bong
     * chat", mac dinh bat tu Android 11. Tat di thi nut o man chinh khong dem duoc tin,
     * con Telegram van mo binh thuong.
     */
    fun laCuaBa(maKhungChat: String?, chatIdBa: Long): Boolean =
        chatIdBa > 0L && maKhungChat == "ndid_$chatIdBa"

    /**
     * So tin chua doc ma thong bao nay mang, neu la cua Ba Huy.
     *
     * Telegram ghi vao so cua thong bao (Notification.number) dung so tin chua doc cua
     * khung chat do, lay tu danh sach tin dang cho bao. Thong bao con do ma so bang 0
     * thi van tinh la mot tin.
     */
    fun soTinCuaBa(maKhungChat: String?, so: Int, chatIdBa: Long): Int =
        if (laCuaBa(maKhungChat, chatIdBa)) so.coerceAtLeast(1) else 0
}

/**
 * Hoi xem app nao dang giu trinh phat, va bam dung ho.
 *
 * VI SAO KHONG DUNG [AudioHush]. Gianh quyen phat tieng thi bat im duoc ca may,
 * nhung no la mot cu dam mu: khong biet vua bat ai im, va bat nham ca tieng cua app
 * dang duoc phep. Trinh phat thi noi ro ten goi, nen dem duoc phut cua dung mot app
 * va dung dung app do.
 *
 * Thieu quyen thi moi ham o day tra ve rong hoac false, va ben goi quay lai cach cu.
 */
object TrinhPhat {

    /** Mot trinh phat dang co tren may. */
    data class Phien(val goi: String, val dangPhat: Boolean)

    fun coQuyen(context: Context): Boolean = runCatching {
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    }.getOrDefault(false)

    /**
     * Cac trinh phat dang co, ke ca cai dang tam dung.
     *
     * Tra ve rong khi chua bat quyen. Khong log moi lan: ham nay chay hai muoi giay
     * mot lan trong suot luc co nhac, va Ba Huy da co mot dong trong bang canh bao
     * neu quyen chua bat.
     */
    fun dangCo(context: Context): List<Phien> {
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return emptyList()
        val listener = ComponentName(context, TaiThongBao::class.java)
        val controllers = runCatching { manager.getActiveSessions(listener) }
            .getOrElse { return emptyList() }

        return controllers.mapNotNull { dieuKhien ->
            val goi = dieuKhien.packageName ?: return@mapNotNull null
            if (!laTiengGiaiTri(dieuKhien)) return@mapNotNull null
            Phien(goi, dangPhat(dieuKhien))
        }
    }

    /** Cac goi dang phat tieng ngay luc nay. */
    fun dangPhat(context: Context): Set<String> =
        dangCo(context).filter { it.dangPhat }.map { it.goi }.toSet()

    /**
     * Bam nut dung ho [goi]. Tra ve true neu bam duoc.
     *
     * Bam dung chu khong phai tat: con mo lai la nhac chay tiep tu cho cu. Muc dich
     * la het gio thi thoi, khong phai lam con mat cho dang nghe.
     */
    fun dungLai(context: Context, goi: String): Boolean {
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return false
        val listener = ComponentName(context, TaiThongBao::class.java)
        val controllers = runCatching { manager.getActiveSessions(listener) }
            .getOrElse { return false }

        var xong = false
        controllers.filter { it.packageName == goi }.forEach {
            runCatching { it.transportControls.pause() }
                .onSuccess { xong = true }
                .onFailure { loi -> Log.w(TAG, "khong bam dung duoc $goi: ${loi.message}") }
        }
        return xong
    }

    /**
     * Dang phat that hay chi dang mo san.
     *
     * Tinh ca BUFFERING: dung giua luc dang tai mot bai la no chay tiep ngay sau do,
     * va dem thieu mot nhip hai muoi giay moi lan mang cham thi so phut sai dan.
     */
    private fun dangPhat(dieuKhien: MediaController): Boolean {
        val trangThai = dieuKhien.playbackState?.state ?: return false
        return trangThai == PlaybackState.STATE_PLAYING ||
            trangThai == PlaybackState.STATE_BUFFERING
    }

    /**
     * Tieng nay co phai loai duoc phep dung khong.
     *
     * Bao thuc, chuong bao, tieng cuoc goi deu di qua duong media session tren mot
     * so may. Bam dung mot cai bao thuc luc 6 gio sang thi con ngu quen va di hoc
     * muon - hong nang hon nhieu so voi viec de lot mot bai hat.
     *
     * Khong khai bao gi thi coi la nhac: rat nhieu app de usage la UNKNOWN, loai ca
     * nhom do ra thi phan nay coi nhu khong chay.
     */
    private fun laTiengGiaiTri(dieuKhien: MediaController): Boolean {
        val usage = dieuKhien.playbackInfo?.audioAttributes?.usage
            ?: AudioAttributes.USAGE_UNKNOWN
        return usage == AudioAttributes.USAGE_UNKNOWN ||
            usage == AudioAttributes.USAGE_MEDIA ||
            usage == AudioAttributes.USAGE_GAME
    }

    private const val TAG = "HomeworkGate"
}
