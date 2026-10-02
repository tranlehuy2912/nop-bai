package vn.huytl.homeworkgate.ui

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.LuatGhep

/**
 * Hang sao cua mot cau lam tren may, dung chung cho man lam bai ([LamBaiActivity]) va the tung
 * cau o man Giai de.
 *
 * BA DANG SAO (Ba Huy chot 30/9/2026): rong la con co the duoc, xam la da mat vi sai, vang la
 * da duoc. Vao cau thi ca hang rong; sai thi mot o rong chuyen xam ngay (cau nhieu o sai hai o
 * thi xam hai o); lam xong dung thi cac o rong con lai chuyen vang, kem mot nhip sang ([nhip]).
 *
 * Truoc ngay do vao cau la sao da day, sai thi tat dan, mot mau cam cho ca hang: con khong phan
 * biet duoc sao "dang giu" voi sao "da duoc", va lam lai thi khong biet lan truoc duoc may sao.
 * Dang xam van can: chi co rong va vang thi luc dang lam con khong thay lan sai vua roi lam mat
 * gi, ma thiet ke 29/9/2026 dua vao dung tin hieu do de con tu hieu luat, khong can chu.
 *
 * Dong "Lần trước" ([chuLanTruoc]) la sao TOT NHAT cua vong dang lam, xem
 * [vn.huytl.homeworkgate.data.LamTrenMay.saoLanTruoc].
 */
object HangSao {

    enum class O { CON, MAT, DUOC }

    /**
     * Cac o sao cua luot, theo thu tu hien: o con (hay da duoc) truoc, o mat sau. Het sao, dang
     * thu them, hay xong ma sai thi ca hang xam; xong dung thi o con lai la o duoc.
     */
    fun cacO(luot: LuatGhep.Luot): List<O> {
        val toiDa = luot.saoToiDa.coerceAtLeast(0)
        val con = if (luot.xong && !luot.dung) 0 else luot.sao.coerceIn(0, toiDa)
        val kieu = if (luot.xong && luot.dung) O.DUOC else O.CON
        return List(con) { kieu } + List(toiDa - con) { O.MAT }
    }

    /** O sao cua dong "Lần trước": [dat] o vang, phan con lai xam. */
    fun cacOLanTruoc(dat: Int, toiDa: Int): List<O> {
        val d = dat.coerceIn(0, toiDa.coerceAtLeast(0))
        return List(d) { O.DUOC } + List(toiDa.coerceAtLeast(0) - d) { O.MAT }
    }

    /** Chu cua hang sao, moi sao mot mau. Sao rong la ☆, sao xam va vang la ★. */
    fun chu(ct: Context, cac: List<O>): CharSequence {
        val sb = SpannableStringBuilder()
        cac.forEach { o ->
            val dau = sb.length
            sb.append(if (o == O.CON) "☆" else "★")
            sb.setSpan(ForegroundColorSpan(mau(ct, o)), dau, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    fun chu(ct: Context, luot: LuatGhep.Luot): CharSequence = chu(ct, cacO(luot))

    /** "Lần trước ★★☆" cho lan lam lai, [dat] la sao tot nhat cua vong. */
    fun chuLanTruoc(ct: Context, dat: Int, toiDa: Int): CharSequence =
        SpannableStringBuilder("Lần trước ").append(chu(ct, cacOLanTruoc(dat, toiDa)))

    /**
     * Mau chu "+N ★" hien khi mot cau vua xong (man lam bai, Giai de, hai man Kiem tra): co sao
     * thi vang, dung mau o sao da duoc ([O.DUOC]); "+0 ★" thi xam. Truoc 2/10/2026 chu nay xanh
     * la (mau "ok"), lech voi hang sao vua chuyen vang ngay tren do; Ba Huy muon "+N ★" la sao
     * vang nhu sao luc lam dung. Cac man goi ham nay thay vi tu chon mau, de doi mau sao o [mau]
     * thi "+N ★" doi theo.
     */
    fun mauCong(ct: Context, n: Int): Int =
        if (n > 0) mau(ct, O.DUOC) else ContextCompat.getColor(ct, R.color.ink_soft)

    private fun mau(ct: Context, o: O): Int = ContextCompat.getColor(
        ct,
        when (o) {
            O.CON -> R.color.ink_soft
            O.MAT -> R.color.sao_mat
            O.DUOC -> R.color.wait
        }
    )

    /**
     * Sang len mot nhip khi sao vua chuyen vang: phong to roi thu lai, khong rung (Ba Huy chot
     * khong rung ngay 29/9/2026). Phong tu mep phai vi hang sao nam sat le phai man hinh, phong
     * tu giua thi dau hang chom ra ngoai le.
     */
    fun nhip(v: View) {
        v.animate().cancel()
        v.pivotX = v.width.toFloat()
        v.pivotY = v.height / 2f
        v.scaleX = 1f
        v.scaleY = 1f
        v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(160).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
        }.start()
    }
}
