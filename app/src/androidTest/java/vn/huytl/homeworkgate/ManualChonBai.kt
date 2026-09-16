package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.kho.NganHang

/**
 * Khong phai test. Dung de NHIN man khai bai o cac trang thai khac nhau.
 *
 * Man [vn.huytl.homeworkgate.ui.ChonBaiActivity] doi mat theo so cai: cau da tra
 * gio thi mo di va khong tich duoc, cau dang sai thi tich san. Hai trang thai do
 * chi hien ra sau khi da nop bai that vai lan - ma cho den luc do thi khong ai
 * nhin thay chung co dung khong. O day nap thang vao so roi mo man ra xem.
 *
 *   # danh dau 1.1, 1.2a da xong va 1.3a dang sai
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualChonBai#napThu \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *   adb shell am start -n vn.huytl.homeworkgate/.ui.ChonBaiActivity
 *
 *   # tra so ve rong
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualChonBai#xoaSo \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ManualChonBai {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun napThu() {
        NganHang.napNeuCan(context)
        val now = System.currentTimeMillis()

        fun cau(ma: String, dung: Boolean, nhanXet: String = "") = CauCham(
            ma = ma,
            de = "",
            cauId = "toan8t1:$ma",
            mon = "Toán",
            dung = dung,
            soDong = 4,
            nhanXet = nhanXet
        )

        SoCaiBai.ghi(
            context,
            listOf(cau("1.1", true), cau("1.2a", true)),
            mapOf("1.1" to 2, "1.2a" to 2),
            now
        )
        SoCaiBai.ghi(
            context,
            listOf(cau("1.3a", false, "Dòng 2 con nhân thiếu một thừa số")),
            emptyMap(),
            now
        )
        println("ManualChonBai: 1.1 va 1.2a da tra gio, 1.3a dang cho sua")
        println("ManualChonBai: phut hom nay = ${SoCaiBai.phutDaCongHomNay(context, now)}")
        SoCaiBai.dangChoSua(context, now).forEach { println("  cho sua: ${it.khoa} - ${it.nhanXet}") }
    }

    @Test
    fun xoaSo() {
        SoCaiBai.xoaHet(context)
        println("ManualChonBai: da xoa so cai")
    }
}
