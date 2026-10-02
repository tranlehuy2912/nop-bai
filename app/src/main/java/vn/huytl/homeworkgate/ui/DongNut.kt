package vn.huytl.homeworkgate.ui

import android.content.Context
import android.view.ViewGroup

/**
 * Mot dong nut tu xuong hang khi het be ngang, thay cho cuon ngang.
 *
 * Viet cho dai nut ky hieu cua man Kiem tra bai ngay 24/9/2026; dai do bo ngay 2/10/2026 khi
 * man Kiem tra bai sang phim ghep, con lop nay o lai cho [KhungGhep].
 *
 * Tu viet chu khong keo them FlexboxLayout: mot phu thuoc moi chi de xep vai chuc cai
 * nut. Flow cua ConstraintLayout cung lam duoc, nhung phai dat id cho tung nut.
 */
internal class DongNut(
    context: Context,
    private val khoangNgang: Int,
    private val khoangDoc: Int
) : ViewGroup(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val coTran = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED
        val rong = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var caoDong = 0
        var rongNhat = 0
        for (i in 0 until childCount) {
            val con = getChildAt(i)
            if (con.visibility == GONE) continue
            measureChild(con, widthMeasureSpec, heightMeasureSpec)
            if (coTran && x > 0 && x + con.measuredWidth > rong) {
                x = 0
                y += caoDong + khoangDoc
                caoDong = 0
            }
            rongNhat = maxOf(rongNhat, x + con.measuredWidth)
            x += con.measuredWidth + khoangNgang
            caoDong = maxOf(caoDong, con.measuredHeight)
        }
        // Giu chieu cao toi thieu ben goi dat (minimumHeight): o dat the cua cau ghep cau dat
        // 60dp de con thay cho tha the vao, ma truoc 2/10/2026 lop nay bo qua no va o trong chi
        // con mot vach mong (thay tren may ao pad5 o man Kiem tra bai).
        setMeasuredDimension(
            resolveSize(rongNhat + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(maxOf(y + caoDong + paddingTop + paddingBottom, suggestedMinimumHeight), heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val rong = r - l - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var caoDong = 0
        for (i in 0 until childCount) {
            val con = getChildAt(i)
            if (con.visibility == GONE) continue
            if (x > 0 && x + con.measuredWidth > rong) {
                x = 0
                y += caoDong + khoangDoc
                caoDong = 0
            }
            con.layout(
                paddingLeft + x, paddingTop + y,
                paddingLeft + x + con.measuredWidth, paddingTop + y + con.measuredHeight
            )
            x += con.measuredWidth + khoangNgang
            caoDong = maxOf(caoDong, con.measuredHeight)
        }
    }
}
