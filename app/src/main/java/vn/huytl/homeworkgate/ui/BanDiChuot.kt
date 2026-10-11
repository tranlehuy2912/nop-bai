package vn.huytl.homeworkgate.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot

/**
 * Ban di chuot cua man Remote (11/10/2026, mau B anh Huy chon): mot ngon vuot la chuot tren tivi
 * chay theo, cham nhanh la bam, hai ngon vuot len xuong la cuon. Chi doi cu dong ngon tay thanh
 * so diem anh tivi; gui di la viec cua [RemoteActivity].
 *
 * Vuot cham thi chuot di cham de nham trung nut nho, vuot nhanh thi chuot di xa de qua het man
 * tivi 1920 diem anh trong mot lan vuot, nhu ban di chuot laptop.
 */
class BanDiChuot @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var khiDi: (Int, Int) -> Unit = { _, _ -> }
    var khiBam: () -> Unit = {}
    /** So nac cuon: duong la cuon xuong (ngon tay day len), nhu cuon tren dien thoai. */
    var khiCuon: (Int) -> Unit = {}

    private val dp = resources.displayMetrics.density
    private val chamToiDa = ViewConfiguration.get(context).scaledTouchSlop

    private var xTruoc = 0f
    private var yTruoc = 0f
    private var lucTruoc = 0L
    private var xDau = 0f
    private var yDau = 0f
    private var lucDau = 0L
    private var daDi = false
    private var haiNgon = false
    private var yCuon = 0f
    private var duX = 0f
    private var duY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                xDau = e.x; yDau = e.y; lucDau = e.eventTime
                xTruoc = e.x; yTruoc = e.y; lucTruoc = e.eventTime
                daDi = false
                haiNgon = false
                duX = 0f; duY = 0f
            }
            MotionEvent.ACTION_POINTER_DOWN -> if (e.pointerCount == 2) {
                haiNgon = true
                yCuon = giuaY(e)
            }
            MotionEvent.ACTION_MOVE -> if (haiNgon) {
                if (e.pointerCount >= 2) cuon(e)
            } else {
                di(e)
            }
            MotionEvent.ACTION_UP -> {
                if (!haiNgon && !daDi && e.eventTime - lucDau < CHAM_MS) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    khiBam()
                }
            }
        }
        return true
    }

    private fun di(e: MotionEvent) {
        if (!daDi && hypot(e.x - xDau, e.y - yDau) > chamToiDa) daDi = true
        val dx = e.x - xTruoc
        val dy = e.y - yTruoc
        val ms = (e.eventTime - lucTruoc).coerceAtLeast(1L)
        xTruoc = e.x; yTruoc = e.y; lucTruoc = e.eventTime
        if (!daDi) return
        // Toc do tinh bang dp moi ms; he so la so diem anh tivi cho moi dp ngon tay di.
        val tocDo = hypot(dx, dy) / dp / ms
        val heSo = (HE_SO_CHAM + HE_SO_TANG * tocDo).coerceAtMost(HE_SO_TOI_DA)
        duX += dx / dp * heSo
        duY += dy / dp * heSo
        val x = duX.toInt()
        val y = duY.toInt()
        duX -= x; duY -= y
        if (x != 0 || y != 0) khiDi(x, y)
    }

    private fun cuon(e: MotionEvent) {
        val y = giuaY(e)
        val buoc = BUOC_CUON_DP * dp
        val n = ((yCuon - y) / buoc).toInt()
        if (n != 0) {
            yCuon -= n * buoc
            khiCuon(n)
        }
    }

    private fun giuaY(e: MotionEvent) = (e.getY(0) + e.getY(1)) / 2f

    private companion object {
        const val CHAM_MS = 250L
        const val HE_SO_CHAM = 1.5f
        const val HE_SO_TANG = 4f
        const val HE_SO_TOI_DA = 9f
        const val BUOC_CUON_DP = 28f
    }
}
