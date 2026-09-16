package vn.huytl.homeworkgate.guard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Dong ho dem nguoc nho, noi tren moi app trong luc dang choi.
 *
 * Dat o day chu khong phai trong app, vi luc choi thi dua tre nhin game chu khong
 * mo app Nop bai ra xem. Het gio ma bat ngo bi cat la cam giac bi giat do choi;
 * thay so dem xuong thi biet duong ma tu ket thuc.
 *
 * Cua so nay khong nhan cham: [WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE] cho
 * moi cu cham di xuyen qua xuong game ben duoi, nen no khong the che mat nut nao.
 */
class CountdownOverlay(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var view: TextView? = null

    /** Den luc nay moi cho so dem ghi de len cau nhac. */
    private var giuDen = 0L

    fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    /** Hien hoac cap nhat so. Goi lai moi giay cung duoc, khong tao lai cua so. */
    fun show(text: String) {
        if (!canDraw()) return
        if (SystemClock.elapsedRealtime() < giuDen) return
        handler.post {
            val v = view ?: taoView()?.also { view = it } ?: return@post
            v.text = text
        }
    }

    /**
     * Nhac mot cau ngay trong chinh cai dong ho nay, doi sang mau cam [ms] mili giay.
     *
     * Truoc day cau "con 5 phut, chuan bi dung" hien bang lop phu to bang ca man
     * hinh, va lop do an cham. Dang danh nhau trong game ma man hinh toi di ba giay,
     * bam gi cung khong an, thi khong phai la loi nhac - la bi giat do choi som ba
     * giay. Cai dong ho nay thi khong an cham va dang nam san tren man hinh roi.
     */
    fun nhac(text: String, ms: Long) {
        if (!canDraw()) return
        handler.post {
            val v = view ?: taoView()?.also { view = it } ?: return@post
            v.text = text
            doiNen(v, MAU_NHAC)
            giuDen = SystemClock.elapsedRealtime() + ms
            handler.postDelayed({ view?.let { doiNen(it, MAU_THUONG) } }, ms)
        }
    }

    private fun doiNen(v: TextView, mau: String) {
        (v.background as? GradientDrawable)?.setColor(Color.parseColor(mau))
    }

    fun hide() {
        handler.post {
            view?.let { runCatching { windowManager.removeView(it) } }
            view = null
        }
    }

    @SuppressLint("InflateParams")
    private fun taoView(): TextView? {
        val dp = context.resources.displayMetrics.density
        val tv = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding((14 * dp).toInt(), (6 * dp).toInt(), (14 * dp).toInt(), (6 * dp).toInt())
            background = GradientDrawable().apply {
                // Den mo chu khong dac: de nhin thay so ma van thay game ben duoi.
                setColor(Color.parseColor(MAU_THUONG))
                cornerRadius = 100 * dp
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Goc tren ben phai: it cham vao noi dung game nhat.
            //
            // Khong dat FLAG_LAYOUT_NO_LIMITS: co do cho cua so ve tran ca len thanh
            // trang thai, va no che mat bieu tuong pin voi wifi. Bo di thi he thong
            // tu xep no xuong duoi thanh do.
            gravity = Gravity.TOP or Gravity.END
            x = (12 * dp).toInt()
            // 34dp: du de xuong duoi thanh trang thai cua moi may. Chua 8dp thi
            // tren may ao no van liem vao bieu tuong pin.
            y = (34 * dp).toInt()
        }

        return if (runCatching { windowManager.addView(tv, params) }.isSuccess) tv else null
    }

    companion object {
        private const val MAU_THUONG = "#CC101216"

        /** Cam dac hon mot chut, de liec mot cai la thay no vua doi. */
        private const val MAU_NHAC = "#E6C2410C"

        /** "23:07" - phut va giay con lai. */
        fun dinhDang(conLaiMs: Long): String {
            val giay = (conLaiMs / 1000).coerceAtLeast(0)
            return "%d:%02d".format(giay / 60, giay % 60)
        }
    }
}
