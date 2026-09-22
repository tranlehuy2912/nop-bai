package vn.huytl.homeworkgate.ui

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat

/**
 * To mau mot doan chu trong cau.
 *
 * VI SAO CO. Luat but do la luat DE QUEN NHAT trong ca app, va quen no thi con mat
 * cong that: lam lai ca trang bai on bang but thuong, chup gui, roi may khong tinh
 * gio - dung cai cong suc that su da bo ra. Cau nhac thi co, nhung no nam lan trong
 * mot dong chu den nhu moi dong khac.
 *
 * To do chinh hai chu "but do" thi mat bat duoc no truoc khi doc het cau. Va mau
 * chon la [vn.huytl.homeworkgate.R.color.alert], dung mau app dang dung cho nhung
 * cho "coi chung", chu khong dat mot mau do rieng.
 */
object ToChu {

    /**
     * Tra ve [chu] voi moi lan [doan] xuat hien duoc to mau [mau].
     *
     * Khong phan biet hoa thuong, vi cung mot cau nhac co cho viet "bút đỏ" co cho
     * viet "BÚT ĐỎ" - de hai ban thi kieu gi cung co lan sua mot ben quen ben kia.
     */
    fun to(context: Context, chu: String, doan: String, mau: Int): CharSequence {
        val s = SpannableString(chu)
        var tu = chu.indexOf(doan, ignoreCase = true)
        while (tu >= 0) {
            s.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, mau)),
                tu, tu + doan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tu = chu.indexOf(doan, tu + doan.length, ignoreCase = true)
        }
        return s
    }

    /** Cau nhac co nhac but do thi to hai chu do len. Khong co thi tra lai nguyen cau. */
    fun toButDo(context: Context, chu: String): CharSequence =
        to(context, chu, "bút đỏ", vn.huytl.homeworkgate.R.color.alert)
}
