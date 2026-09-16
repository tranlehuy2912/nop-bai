package vn.huytl.homeworkgate

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.NhatKySuDung
import java.util.Calendar

/**
 * Khong phai test that. Cai cong tay de nhin trang thong ke tren may ao ma khong
 * phai ngoi mo app suot ba tieng cho no ghi du so lieu.
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualThongKe#napThu \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *
 *   ...#in      in ra dung doan chu ma /thongke gui di Telegram
 *   ...#xoa     xoa sach so
 *
 * App dung de nap lay ngay tren may - app nao co san thi dung app do, de trang
 * thong ke co bieu tuong va ten that chu khong phai mot dong ten goi.
 */
@RunWith(AndroidJUnit4::class)
class ManualThongKe {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    /** Vai app mo duoc tu man hinh chinh, sap xep co dinh de lan nao nap cung giong nhau. */
    private fun vaiApp(so: Int): List<String> {
        val pm = context.packageManager
        val y = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(y, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .filter { it != context.packageName }
            .distinct()
            .sorted()
            .take(so)
    }

    private fun luc(gio: Int, phut: Int, lui: Int = 0): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -lui)
            set(Calendar.HOUR_OF_DAY, gio)
            set(Calendar.MINUTE, phut)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun napThu() {
        val app = vaiApp(4)
        if (app.isEmpty()) {
            println("MANUAL_THONGKE: may nay khong co app nao mo duoc tu man hinh chinh")
            return
        }
        fun goi(i: Int) = app[i % app.size]

        // Mot buoi chieu that: choi mot lat, bo ra an com, quay lai, toi mo them app khac.
        NhatKySuDung.ghi(context, goi(0), luc(14, 3), luc(14, 26))
        NhatKySuDung.ghi(context, goi(0), luc(15, 10), luc(15, 42))
        NhatKySuDung.ghi(context, goi(1), luc(16, 5), luc(16, 20))
        NhatKySuDung.ghi(context, goi(0), luc(19, 2), luc(19, 41))
        NhatKySuDung.ghi(context, goi(2), luc(19, 45), luc(20, 3))
        NhatKySuDung.ghi(context, goi(3), luc(20, 5), luc(20, 12))

        // Hom qua, de thu ca hang chon ngay.
        NhatKySuDung.ghi(context, goi(1), luc(17, 0, lui = 1), luc(18, 12, lui = 1))
        NhatKySuDung.ghi(context, goi(0), luc(20, 30, lui = 1), luc(20, 50, lui = 1))

        // ghi() ghi bang apply(), tien trinh test chet ngay sau do la mat ban ghi.
        // Doc lai mot lan de cho hang doi kip day xuong dia.
        println("MANUAL_THONGKE: da nap ${NhatKySuDung.theoApp(context).size} app cho hom nay")
        println(NhatKySuDung.tomTat(context, 0, "Lê Hòa"))
    }

    @Test
    fun inRa() {
        println(NhatKySuDung.tomTat(context, 0, "Lê Hòa"))
    }

    @Test
    fun xoa() {
        NhatKySuDung.xoaHet(context)
        println("MANUAL_THONGKE: da xoa so su dung")
    }
}
