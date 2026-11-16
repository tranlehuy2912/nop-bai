package vn.huytl.homeworkgate.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Chu trang co vien mau day va bong lech xuong, nhu so gio va ten trang thai trong mau H
 * "Game bong bay" (anh Huy chon 16/11/2026, hoi "mau dep lam ma, sao lam ko giong?").
 *
 * TextView cua Android khong ve duoc vien chu, nen ve ba lan: bong (vien lech xuong
 * [lechBong]), vien, roi moi den ruot chu. Doi mau chu giua chung bang setTextColor, ma
 * ham do goi invalidate(), nen chan invalidate trong luc dang ve, khong thi ve mai khong
 * dung.
 *
 * Vien tran ra ngoai net chu nua [doDayVien]: dat padding cho TextView it nhat bang chung
 * do, khong thi vien bi cat o mep.
 */
class ChuVien @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    /** Mau vien va bong. Thuong la mau dam cua trang thai, xem [HinhBong.dam]. */
    var mauVien: Int = Color.parseColor("#1F5FCC")
        set(v) {
            field = v
            invalidate()
        }

    /** Do day vien, tinh bang px. 0 la chu thuong. */
    var doDayVien: Float = 8f * resources.displayMetrics.density

    /** Bong lech xuong bao nhieu px. */
    var lechBong: Float = 5f * resources.displayMetrics.density

    private var dangVe = false

    override fun onDraw(canvas: Canvas) {
        if (doDayVien <= 0f) {
            super.onDraw(canvas)
            return
        }
        val mauChu = currentTextColor
        dangVe = true
        val p = paint
        p.strokeJoin = Paint.Join.ROUND
        p.strokeMiter = 10f
        p.style = Paint.Style.STROKE
        p.strokeWidth = doDayVien
        setTextColor(mauVien)
        if (lechBong > 0f) {
            canvas.save()
            canvas.translate(0f, lechBong)
            super.onDraw(canvas)
            canvas.restore()
        }
        super.onDraw(canvas)
        p.style = Paint.Style.FILL
        p.strokeWidth = 0f
        setTextColor(mauChu)
        super.onDraw(canvas)
        dangVe = false
    }

    override fun invalidate() {
        if (!dangVe) super.invalidate()
    }
}
