package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.GioiHanApp

/**
 * Khong phai test that. Hai cai cong tay de thu han gio rieng tren may ao ma khong
 * phai ngoi xem Netflix chin muoi phut.
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualGioiHan#datHan \
 *     -e goi com.android.chrome -e phut 15 \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *
 *   ...#dungHet -e goi com.android.chrome
 */
@RunWith(AndroidJUnit4::class)
class ManualGioiHan {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val args by lazy { InstrumentationRegistry.getArguments() }
    private val goi by lazy { args.getString("goi") ?: "com.android.chrome" }

    @Test
    fun datHan() {
        val phut = args.getString("phut")?.toIntOrNull() ?: 15
        GioiHanApp.datHan(context, goi, phut)
        println("MANUAL_HAN: $goi = ${GioiHanApp.han(context, goi)} phut moi ngay")
    }

    /** Danh dau la da xem het han hom nay, de thu canh bi khoa. */
    @Test
    fun dungHet() {
        val phut = GioiHanApp.han(context, goi)
        GioiHanApp.congThem(context, goi, phut * 60_000L)
        // congThem ghi bang apply(), tien trinh test chet ngay sau do la mat ban ghi.
        // Goi them mot ham ghi dong bo de day het hang doi xuong dia.
        GioiHanApp.datHan(context, goi, phut)
        println("MANUAL_HAN: $goi da dung ${GioiHanApp.daDungMs(context, goi) / 60_000} phut, " +
            "het gio=${GioiHanApp.hetGio(context, goi)}")
    }

    @Test
    fun xoaDaDung() {
        GioiHanApp.xoaDaDung(context, goi)
        println("MANUAL_HAN: da xoa so dem cua $goi")
    }
}
