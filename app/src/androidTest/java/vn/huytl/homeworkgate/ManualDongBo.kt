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

    /**
     * Thu duong PING: go mot lenh hoi nhu app Bang dieu khien van go, roi xem
     * tablet co day ban trang thai moi khong.
     *
     * Dong vai dien thoai bang chinh may nay - khong kiem duoc phan Firestore chuyen
     * lenh di, nhung kiem duoc cai de hong that: tablet nhan lenh, va no day DU
     * KHONG CO GI DOI so voi lan day truoc. Cho do la cho co the lang le khong lam
     * gi, vi [DongBo.dayThat] von bo qua nhung ban giong het ban cu.
     */
    @Test
    fun pingThu() {
        DongBo.batDau(context)
        val maNha = DongBo.maNhaHienTai(context)
        if (maNha.isEmpty()) {
            println("MANUAL_DONGBO: may nay chua lap nha, khong thu duoc")
            return
        }
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val trangThai = db.collection("nha").document(maNha).collection("hop")
            .document("trangthai")

        fun doc(truong: String): Long {
            val cho = CountDownLatch(1)
            var luc = -1L
            trangThai.get().addOnCompleteListener {
                luc = when (truong) {
                    "capNhatLuc" -> it.result?.getLong("capNhatLuc") ?: 0L
                    // O tra loi la mot map {chu, luc, ai}. PING khong duoc dong vao
                    // day: mot dong trong moc len giua man hinh cua Ba Huy.
                    else -> (it.result?.get("traLoi") as? Map<*, *>)?.get("luc") as? Long ?: 0L
                }
                cho.countDown()
            }
            cho.await(20, TimeUnit.SECONDS)
            return luc
        }

        fun capNhatLuc(): Long = doc("capNhatLuc")

        val truoc = capNhatLuc()
        val traLoiTruoc = doc("traLoi")
        // Day ngay mot lan roi doc lai, de chac chan ban tren Firestore la ban
        // "moi nhat khong co gi doi" - dung canh ma PING phai vuot qua.
        DongBo.dayNgay()
        Thread.sleep(3_000)
        val nen = capNhatLuc()

        db.collection("nha").document(maNha).collection("lenh").add(
            mapOf(
                "kieu" to "PING",
                "ai" to "bahuy",
                "tao" to System.currentTimeMillis()
            )
        )
        Thread.sleep(6_000)
        val sau = capNhatLuc()

        val traLoiSau = doc("traLoi")
        println(
            "MANUAL_DONGBO: ping truoc=$truoc nen=$nen sau=$sau " +
                (if (sau > nen) "-> TABLET DA DAP" else "-> KHONG DAP") +
                ", o traLoi " + (if (traLoiSau == traLoiTruoc) "khong bi dong vao" else "BI GHI DE")
        )
    }
}
