package vn.huytl.homeworkgate.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.graphics.ColorUtils

/**
 * Cac hinh nen cua mau H "Game bong bay" (anh Huy chon 16/11/2026) ma mau doi theo trang thai
 * hay theo tung viec, nen khong viet san thanh file XML duoc: hinh tron bong, the trang vien
 * day, dai ruy bang.
 *
 * Kieu chung cua mau: than co mau, mep day dam hon lech xuong vai dp nhu mot phim bam noi,
 * vien trang. Mau dam lay tu [dam] khi ben goi khong dua san.
 */
object HinhBong {

    private fun Context.px(dp: Float): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()

    /** Mau dam hon cho mep day va vien chu. */
    fun dam(mau: Int): Int = ColorUtils.blendARGB(mau, Color.BLACK, 0.22f)

    /** Mau sang hon cho phan tren cua than. */
    fun sang(mau: Int): Int = ColorUtils.blendARGB(mau, Color.WHITE, 0.45f)

    /**
     * Hinh tron bong: tam sang lech len goc trai, ra ngoai dam dan ve [mau], vien trang,
     * mep day [mauDam] lech xuong 4dp. [coDp] la co hinh, de tinh ban kinh vung sang.
     */
    fun tron(ct: Context, mau: Int, coDp: Float, mauDam: Int = dam(mau), vienDp: Float = 4f): Drawable {
        val day = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(mauDam)
        }
        val than = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = intArrayOf(Color.WHITE, sang(mau), mau, mau)
            setGradientCenter(0.35f, 0.3f)
            gradientRadius = ct.px(coDp * 0.85f).toFloat()
            if (vienDp > 0f) setStroke(ct.px(vienDp), Color.WHITE)
        }
        return LayerDrawable(arrayOf(day, than)).apply {
            setLayerInset(0, 0, ct.px(4f), 0, 0)
            setLayerInset(1, 0, 0, 0, ct.px(4f))
        }
    }

    /**
     * The trang vien day [mau], mep day [mauDam] cao [dayDp]: the trang thai o man chinh.
     * Ben goi phai dat padding duoi lon hon tren mot khoang [dayDp], vi phan mep day nam
     * trong khung cua the.
     */
    fun the(ct: Context, mau: Int, mauDam: Int, gocDp: Float = 32f, vienDp: Float = 7f, dayDp: Float = 12f): Drawable {
        val day = GradientDrawable().apply {
            cornerRadius = ct.px(gocDp).toFloat()
            setColor(mauDam)
        }
        val than = GradientDrawable().apply {
            cornerRadius = ct.px(gocDp).toFloat()
            setColor(Color.WHITE)
            setStroke(ct.px(vienDp), mau)
        }
        return LayerDrawable(arrayOf(day, than)).apply {
            setLayerInset(1, 0, 0, 0, ct.px(dayDp))
        }
    }

    /** Dai ruy bang: than [mau] vien trang, mep day [mauDam] 5dp. Nhan trang thai tren the. */
    fun ruyBang(ct: Context, mau: Int, mauDam: Int): Drawable {
        val day = GradientDrawable().apply {
            cornerRadius = ct.px(18f).toFloat()
            setColor(mauDam)
        }
        val than = GradientDrawable().apply {
            cornerRadius = ct.px(18f).toFloat()
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(sang(mau), mau)
            setStroke(ct.px(3f), Color.WHITE)
        }
        return LayerDrawable(arrayOf(day, than)).apply {
            setLayerInset(1, 0, 0, 0, ct.px(5f))
        }
    }

    /**
     * O mau o trang Luyen tap (16/11/2026): than chuyen tu sang xuong [mau], vien trang 5dp, mep
     * day dam 7dp. O dat padding duoi lon hon tren 7dp cho phan mep.
     */
    fun o(ct: Context, mau: Int): Drawable {
        val day = GradientDrawable().apply {
            cornerRadius = ct.px(28f).toFloat()
            setColor(dam(mau))
        }
        val than = GradientDrawable().apply {
            cornerRadius = ct.px(28f).toFloat()
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(sang(mau), mau)
            setStroke(ct.px(5f), Color.WHITE)
        }
        return LayerDrawable(arrayOf(day, than)).apply {
            setLayerInset(1, 0, 0, 0, ct.px(7f))
        }
    }

    /**
     * Nut bong mau [mau] ve trong code, cho nut ma mau doi theo du lieu (nut mon o trang Luyen
     * tap). Cung kieu voi drawable/nut_bong_*.xml: mep day dam 5dp, than chuyen mau, vet sang.
     */
    fun nut(ct: Context, mau: Int): Drawable {
        val day = GradientDrawable().apply {
            cornerRadius = ct.px(20f).toFloat()
            setColor(dam(mau))
        }
        val than = GradientDrawable().apply {
            cornerRadius = ct.px(20f).toFloat()
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(sang(mau), mau)
        }
        val sangTren = GradientDrawable().apply {
            cornerRadius = ct.px(6f).toFloat()
            setColor(Color.argb(0x33, 255, 255, 255))
        }
        return LayerDrawable(arrayOf(day, than, sangTren)).apply {
            setLayerInset(1, 0, 0, 0, ct.px(5f))
            setLayerInset(2, ct.px(16f), ct.px(4f), ct.px(16f), 0)
            setLayerGravity(2, android.view.Gravity.TOP or android.view.Gravity.FILL_HORIZONTAL)
            setLayerHeight(2, ct.px(9f))
        }
    }
}
