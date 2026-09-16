package vn.huytl.homeworkgate.guard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import vn.huytl.homeworkgate.R

/**
 * Lop phu bao "het gio" khi con mo app bi chan.
 *
 * Dung cua so overlay chu khong mo Activity, vi tu Android 10 app chay nen bi
 * cam mo Activity, con overlay thi khong dinh han che do. Doi lai phai xin quyen
 * "Hien thi tren ung dung khac" mot lan luc cai.
 */
class BlockOverlay(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var view: View? = null
    private val hideRunnable = Runnable { hide() }

    fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    @SuppressLint("InflateParams")
    /**
     * @param autoHideMs 0 la giu nguyen cho den khi co nguoi goi [hide]. Dung cho
     *   truong hop app bi chan khong chiu di: luc do lop phu nay chinh la thu duy
     *   nhat che duoc no lai.
     * @param khiBam bam vao lop phu thi lam gi. Phai co mot duong ra, khong thi lop
     *   phu giu nguyen va an cham se khoa cung ca may.
     */
    fun show(
        title: String,
        detail: String,
        autoHideMs: Long = 4_000L,
        khiBam: (() -> Unit)? = null,
    ) {
        if (!canDraw()) return
        handler.post { showOnMain(title, detail, autoHideMs, khiBam) }
    }

    private fun showOnMain(
        title: String,
        detail: String,
        autoHideMs: Long,
        khiBam: (() -> Unit)?,
    ) {
        handler.removeCallbacks(hideRunnable)

        if (view == null) {
            val fresh = LayoutInflater.from(context)
                .inflate(R.layout.overlay_blocked, null)
            val added = runCatching {
                windowManager.addView(fresh, buildParams())
            }.isSuccess
            if (!added) return
            view = fresh
        }

        val v = view ?: return
        v.findViewById<TextView>(R.id.overlay_title).text = title
        v.findViewById<TextView>(R.id.overlay_detail).text = detail
        v.visibility = View.VISIBLE
        if (khiBam == null) {
            v.setOnClickListener(null)
            v.isClickable = false
        } else {
            v.setOnClickListener { khiBam() }
        }

        if (autoHideMs > 0) handler.postDelayed(hideRunnable, autoHideMs)
    }

    fun hide() {
        handler.post {
            handler.removeCallbacks(hideRunnable)
            val v = view ?: return@post
            runCatching { windowManager.removeView(v) }
            view = null
        }
    }

    private fun buildParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Khong dat FLAG_NOT_TOUCHABLE: lop phu phai an cham, neu khong con
            // bam xuyen qua xuong game ben duoi trong luc no con hien.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
}
