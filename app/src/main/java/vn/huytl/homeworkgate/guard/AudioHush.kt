package vn.huytl.homeworkgate.guard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log

/**
 * Bat cac app khac im tieng khi het gio.
 *
 * Day ve man hinh chinh moi chi cat phan nhin. App bi day xuong nen van phat
 * tieng duoc: nhac, podcast, YouTube ban tra tien. Con nam nghe tiep trong khi
 * man hinh da khoa, va nguoi lon tuong la da xong.
 *
 * Khong co cach nao tat han app khac neu khong phai Device Owner. Nhung gianh
 * quyen phat tieng thi lam duoc, va he qua gan nhu the: xin [AudioManager.AUDIOFOCUS_GAIN]
 * la moi app dang phat nhan duoc AUDIOFOCUS_LOSS, tuc la "mat han", nen chung dung
 * lai va khong tu phat tiep. Xin xong nha ra ngay, vi minh khong co gi de phat.
 *
 * Nha ra ma chung khong tu chay lai la nho dung GAIN chu khong phai GAIN_TRANSIENT:
 * TRANSIENT co nghia la "muon mot lat", va app nghe loi se phat tiep khi minh nha.
 */
object AudioHush {

    fun hush(context: Context) {
        val manager = context.getSystemService(AudioManager::class.java) ?: return

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        val result = runCatching { manager.requestAudioFocus(request) }.getOrNull()
        Log.i(TAG, "xin quyen phat tieng de bat app khac im: ket qua=$result")
        runCatching { manager.abandonAudioFocusRequest(request) }
    }

    private const val TAG = "HomeworkGate"
}
