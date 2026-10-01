package vn.huytl.homeworkgate.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore

/**
 * Thanh han muc gio choi trong ngay, ve chung cho man chinh va man Cach kiem gio.
 *
 * Ba khuc tren mot truc:
 *   - xam: phut da choi that trong ngay, dem bang dong ho ([GateStore.msDaChoiHomNay]);
 *   - mau cua trang thai: phut dang giu, chua choi ([GateStore.msDangGiu]);
 *   - trang: phut hom nay con kiem them duoc bang bai tap ([GateStore.phutConLaiHomNay]).
 * Hai khuc dau nam sat nhau, khong khe ho: Ba Huy chon vay ngay 1/10/2026, sau khi xem ban
 * ve thu co khe trang 2dp giua hai khuc.
 *
 * "Duoc choi" la hai khuc dau cong lai, nen gio nguoi lon cho (Ba Huy cho, ba noi cho,
 * thuong viec nha, quy gio choi) tu nam trong do: cho 30 phut thi so duoc choi va ca thanh
 * cung dai them 30, con phan trang con kiem duoc giu nguyen. Ba Huy chot 1/10/2026: kiem
 * 45 phut, ba cho 30 thi dong chu ghi "75/245". Truoc do chu ghi "kiem duoc" va chi dem
 * phut doi bang bai tap, nen gio cho khong co cho nao tren thanh.
 *
 * NHUNG LOI CUA BAN TRUOC 1/10/2026, de khoi lap lai:
 *   - Khung khong dat weightSum, nen hai khuc co trong so chia nhau CA chieu dai thanh:
 *     kiem duoc 45/215 ma thanh lap kin, luc toan xanh duong, luc toan xanh la. Ba Huy
 *     thay "luc mau nay luc mau no" ma khong doan ra quy luat.
 *   - Khuc da choi xanh la, trung mau vong dem nguoc ngay tren - ma o vong, xanh la la
 *     phan CON LAI.
 *   - Phan rong la den trong suot nen nga theo mau the.
 *   - Khong cho nao noi mau nao la gi.
 *   - "Da choi" suy bang phep tru, ba cho them gio giua phien la khuc da choi tut ve 0.
 */
object ThanhNgay {

    /** So phut cua ba khuc. [duoc] la hai khuc dau, [tong] la ca thanh. */
    data class So(val daChoi: Int, val con: Int, val conKiem: Int) {
        val duoc: Int get() = daChoi + con
        val tong: Int get() = duoc + conKiem
    }

    fun so(
        gate: GateStore,
        nowWall: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): So {
        // Da choi lam tron xuong, dang giu lam tron len: hai so van cong lai dung bang so
        // phut cua phieu (choi 29 phut ruoi, con 15 phut ruoi thi la 29 + 16 = 45), va
        // "con 1" chi mat khi het giay cuoi cung.
        val daChoi = (gate.msDaChoiHomNay(nowWall, nowElapsed) / 60_000L).toInt()
        val con = ((gate.msDangGiu(nowWall, nowElapsed) + 59_999L) / 60_000L).toInt()
        return So(daChoi, con, gate.phutConLaiHomNay(nowWall))
    }

    /**
     * Mau khuc dang giu: dung mau cua vong dem nguoc hay cua so to tren the luc do. Dang
     * choi xanh la, tam dung vang, phieu chua bam choi xanh duong. Nhin la noi duoc khuc
     * mau tren thanh voi con so o tren.
     */
    @ColorRes
    fun mauCon(gate: GateStore): Int = when (gate.state) {
        GateState.ACTIVE -> R.color.ok
        GateState.PAUSED -> R.color.wait
        else -> R.color.brand
    }

    fun ve(khung: LinearLayout, phanDaChoi: View, phanCon: View, so: So, @ColorRes mauCon: Int) {
        val ct = khung.context
        // Thuoc tinh clipToOutline trong XML chi co tu Android 12 (minSdk o day la 26); dat
        // trong code thi may cu hon cung bo tron hai dau khuc mau theo nen.
        khung.clipToOutline = true
        phanDaChoi.setBackgroundColor(ContextCompat.getColor(ct, R.color.da_choi))
        phanCon.setBackgroundColor(ContextCompat.getColor(ct, mauCon))
        // Phan con kiem duoc khong can View nao: no chinh la nen trang cua khung, mien la
        // tong trong so bang ca thanh. Thieu dong nay la loi dau tien trong danh sach tren.
        khung.weightSum = so.tong.coerceAtLeast(1).toFloat()
        (phanDaChoi.layoutParams as LinearLayout.LayoutParams).weight = so.daChoi.toFloat()
        (phanCon.layoutParams as LinearLayout.LayoutParams).weight = so.con.toFloat()
        khung.requestLayout()
    }

    /**
     * Dong chu thich duoi thanh, "● đã chơi 30     ● còn 15", moi cham cung mau khuc cua no.
     *
     * Hai so nay la hai phan cua so "được chơi" dung ngay tren, cong lai dung bang no, va
     * xep theo thu tu trai sang phai cua thanh. Khuc nao bang 0 thi khong noi; ca hai bang
     * 0 thi tra chuoi rong, de ben goi khoi xuong dong.
     */
    fun chuThich(ct: Context, so: So, @ColorRes mauCon: Int): CharSequence {
        val sb = SpannableStringBuilder()
        fun muc(@ColorRes mau: Int, chu: String) {
            if (sb.isNotEmpty()) sb.append("     ")
            val dau = sb.length
            sb.append(' ')
            sb.setSpan(
                ChamTron(ContextCompat.getColor(ct, mau)),
                dau, dau + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            sb.append(chu)
        }
        if (so.daChoi > 0) muc(R.color.da_choi, "đã chơi ${so.daChoi}")
        if (so.con > 0) muc(mauCon, "còn ${so.con}")
        return sb
    }

    /**
     * Cham tron ve thang len dong chu, thay cho ky tu "●": ky tu do ve theo bo font cua
     * may, to nho cao thap moi may mot kieu (cung ly do hinh o the trang thai la vector chu
     * khong phai emoji). Co cham theo co chu cua dong.
     */
    private class ChamTron(private val mau: Int) : ReplacementSpan() {

        override fun getSize(
            paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?
        ): Int {
            // Khong dien thi Android co the dua vao do cao dong mot bo so rong.
            if (fm != null) paint.getFontMetricsInt(fm)
            return (paint.textSize * (DUONG_KINH + CACH_SAU)).roundToInt()
        }

        override fun draw(
            canvas: Canvas, text: CharSequence?, start: Int, end: Int,
            x: Float, top: Int, y: Int, bottom: Int, paint: Paint
        ) {
            val cu = paint.color
            paint.color = mau
            val r = paint.textSize * DUONG_KINH / 2f
            val fm = paint.fontMetrics
            // Tam cham o giua chieu cao cua font, tinh tu duong chan chu.
            canvas.drawCircle(x + r, y + (fm.ascent + fm.descent) / 2f, r, paint)
            paint.color = cu
        }
    }

    /** Duong kinh cham va khoang trong sau no, tinh theo co chu. */
    private const val DUONG_KINH = 0.62f
    private const val CACH_SAU = 0.35f
}
