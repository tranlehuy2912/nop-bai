package vn.huytl.homeworkgate.guard

import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/**
 * Dich vu doc thong bao, rong ruot va se mai rong.
 *
 * No khong doc mot thong bao nao. Ly do ton tai: Android chi cho doc danh sach
 * trinh phat dang chay (MediaSessionManager) neu app co mot dich vu kieu nay va
 * nguoi dung da bat no trong Settings. Khai bao class rong la cai gia phai tra de
 * hoi duoc cau "app nao dang phat tieng".
 *
 * Khong nhan onNotificationPosted, khong doc noi dung tin nhan cua ai. Neu sau nay
 * co them viec gi o day thi phai can nhac lai: quyen nay doc duoc MOI thong bao
 * tren may, rong hon nhieu so voi phan dang dung.
 */
class TaiThongBao : NotificationListenerService()

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
