package vn.huytl.homeworkgate.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Nen cua mot o trong luoi thoi khoa bieu: net dut o canh duoi va canh trai.
 *
 * Cac o cung mot hang noi net duoi lai thanh duong ke ngang, cac o cung mot cot
 * noi net trai lai thanh duong ke doc - khong phai chen them View phan cach nao
 * giua cac o.
 *
 * Tu ve tung doan gach bang drawRect chu khong dung DashPathEffect: path effect
 * khong duoc canvas tang toc phan cung ho tro day du o moi ban Android, ma mot
 * duong ke khong ve ra thi khong ai thay de bao loi. Truoc do da thu bang
 * layer-list shape co dashWidth: tren may that no khong ve gi ca, vi vung 1dp
 * kep mat net stroke ve giua.
 */
class KeDut(
    private val mau: Int,
    private val doDay: Float,
    private val veDuoi: Boolean,
    private val veTrai: Boolean,
    private val nen: Int = Color.TRANSPARENT
) : Drawable() {

    private val but = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    /** Doan gach va khoang ho deu bang ba lan do day: nhin ra net dut ro rang. */
    private val gach = doDay * 3f
    private val ho = doDay * 3f

    override fun draw(canvas: Canvas) {
        val v = bounds
        if (nen != Color.TRANSPARENT) {
            but.color = nen
            canvas.drawRect(v.left.toFloat(), v.top.toFloat(), v.right.toFloat(), v.bottom.toFloat(), but)
        }
        but.color = mau

        if (veDuoi) {
            var x = v.left.toFloat()
            val y = v.bottom - doDay
            while (x < v.right) {
                canvas.drawRect(x, y, minOf(x + gach, v.right.toFloat()), y + doDay, but)
                x += gach + ho
            }
        }
        if (veTrai) {
            var y = v.top.toFloat()
            val x = v.left.toFloat()
            while (y < v.bottom) {
                canvas.drawRect(x, y, x + doDay, minOf(y + gach, v.bottom.toFloat()), but)
                y += gach + ho
            }
        }
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Drawable.getOpacity da bi bo, nhung lop cha van bat cai dat")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
