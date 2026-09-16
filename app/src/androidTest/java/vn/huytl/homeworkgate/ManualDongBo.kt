package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.DongBo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Khong phai test that. May cai cong tay de thu duong sang app Bang dieu khien.
 *
 * Co cai nay vi thu ghep doi bang tay rat phien: phai vao man Ba Huy, cuon xuong,
 * bam nut, doc hai dong ma tren anh chup man hinh, roi go lai sang may kia. Chay
 * mot lenh ra ngay hai dong do thi nhanh hon nhieu lan.
 */
@RunWith(AndroidJUnit4::class)
class ManualDongBo {

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    /**
     * Lay ma ghep doi va in ra. Tuong duong bam "Nối điện thoại Ba Huy".
     *
     * Chan luong cho den khi Firestore tra loi, vi chay xong la tien trinh test tat
     * - khong cho thi in ra mot chuoi rong roi thoat.
     */
    @Test
    fun layMaGhep() {
        val cho = CountDownLatch(1)
        var ketQua = "khong lay duoc"
        DongBo.maGhepMoi(context) { maNha, maGhep, loi ->
            ketQua = if (maNha.isEmpty()) "HONG: $loi" else "nha=$maNha ghep=$maGhep"
            cho.countDown()
        }
        cho.await(30, TimeUnit.SECONDS)
        println("MANUAL_DONGBO: $ketQua")
    }

    /**
     * Cho mot app vao danh sach duoc choi.
     *
     * Dung khi thu ca hai app tren cung mot may ao: app Bang dieu khien la app la
     * doi voi guard, nen dang mo no ra la bi day di ngay. Tren may that khong can -
     * app do nam ben dien thoai Ba Huy.
     *
     *   ...#choPhepApp -e goi vn.huytl.bangdieukhien
     */
    @Test
    fun choPhepApp() {
        val goi = InstrumentationRegistry.getArguments().getString("goi")
        if (goi.isNullOrBlank()) {
            println("MANUAL_DONGBO: thieu -e goi <ten.goi>")
            return
        }
        val prefs = Prefs.get(context)
        prefs.allowedPackages = prefs.allowedPackages + goi
        println("MANUAL_DONGBO: da cho phep $goi, danh sach=${prefs.allowedPackages}")
    }
}
