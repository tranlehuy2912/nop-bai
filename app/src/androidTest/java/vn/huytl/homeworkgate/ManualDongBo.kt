package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinCuaCo
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

    /**
     * Thu lenh TUCHOI cho bai khong con trong hang cho, qua dung duong Firestore.
     *
     * Dat hai bai thu khong nam trong hang cho: mot bai hom qua van ghi CHO, y nhu bai
     * 20:29 ngay 24/9/2026, va mot bai da duyet. Go lenh TUCHOI cho ca hai nhu app Bang
     * dieu khien van go. Bai CHO phai thanh TUCHOI, con bai da duyet phai giu nguyen.
     * Chay xong xoa hai bai thu.
     */
    @Test
    fun tuChoiBaiDaBoThu() {
        DongBo.batDau(context)
        val maNha = DongBo.maNhaHienTai(context)
        if (maNha.isEmpty()) {
            println("MANUAL_DONGBO: may nay chua lap nha, khong thu duoc")
            return
        }
        val nha = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("nha").document(maNha)
        val batDau = System.currentTimeMillis()
        val quaNgay = nha.collection("bai").document("thu-qua-ngay-$batDau")
        val daDuyet = nha.collection("bai").document("thu-da-duyet-$batDau")

        fun cho(viec: com.google.android.gms.tasks.Task<*>) {
            val xong = CountDownLatch(1)
            viec.addOnCompleteListener { xong.countDown() }
            xong.await(20, TimeUnit.SECONDS)
        }

        fun trangThai(d: com.google.firebase.firestore.DocumentReference): String {
            val xong = CountDownLatch(1)
            var tt = "khong doc duoc"
            d.get(com.google.firebase.firestore.Source.SERVER).addOnCompleteListener {
                tt = it.result?.getString("trangThai") ?: "khong co"
                xong.countDown()
            }
            xong.await(20, TimeUnit.SECONDS)
            return tt
        }

        val homQua = batDau - 24 * 3_600_000L
        cho(quaNgay.set(mapOf("luc" to homQua, "trangThai" to "CHO", "soPhut" to 0)))
        cho(daDuyet.set(mapOf("luc" to batDau, "trangThai" to "DUYET", "soPhut" to 30)))

        listOf(quaNgay.id, daDuyet.id).forEach { id ->
            cho(
                nha.collection("lenh").add(
                    mapOf("kieu" to "TUCHOI", "ai" to "bahuy", "baiId" to id, "tao" to batDau)
                )
            )
        }

        var ttQuaNgay = trangThai(quaNgay)
        while (ttQuaNgay == "CHO" && System.currentTimeMillis() - batDau < 30_000L) {
            Thread.sleep(1_000)
            ttQuaNgay = trangThai(quaNgay)
        }
        // Lenh thu hai den sau lenh dau, cho them mot nhip cho chac.
        Thread.sleep(3_000)
        val ttDaDuyet = trangThai(daDuyet)

        cho(quaNgay.delete())
        cho(daDuyet.delete())

        println(
            "MANUAL_DONGBO: tuchoi bai qua ngay -> $ttQuaNgay" +
                (if (ttQuaNgay == "TUCHOI") " (dung)" else " (SAI)") +
                ", bai da duyet -> $ttDaDuyet" +
                (if (ttDaDuyet == "DUYET") " (dung)" else " (SAI)")
        )
    }

    /**
     * Thu duong TINCO qua Firestore: go mot lenh tin cua co nhu app Bang dieu khien van
     * go, roi xem tablet co cat tin vao kho va tra loi khong.
     *
     * Luc tao ghi tu toi qua la co y. Lenh khac cu nhu vay thi tablet bo, con tin cua co
     * thi phai qua, va phai giu dung gio gui do. Chay xong tra kho tin ve nhu cu.
     */
    @Test
    fun tinCoThu() {
        DongBo.batDau(context)
        val maNha = DongBo.maNhaHienTai(context)
        if (maNha.isEmpty()) {
            println("MANUAL_DONGBO: may nay chua lap nha, khong thu duoc")
            return
        }
        val nha = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("nha").document(maNha)
        val sp = Prefs.get(context).raw()
        val khoCu = sp.getString("tin_cua_co", null)

        // Tien trinh test vua mo nen lan day trang thai dau tien luon ghi de ca document,
        // xoa luon o traLoi. Cho lan day do xong da, y nhu pingThu.
        DongBo.dayNgay()
        Thread.sleep(3_000)

        val batDau = System.currentTimeMillis()
        val toiQua = batDau - 14 * 3_600_000L
        val chu = "Tin thử đường Firestore lúc $batDau"
        nha.collection("lenh").add(
            mapOf("kieu" to "TINCO", "ai" to "bahuy", "chu" to chu, "tao" to toiQua)
        )

        var tin: TinCuaCo? = null
        while (tin == null && System.currentTimeMillis() - batDau < 30_000L) {
            Thread.sleep(500)
            tin = KhoTinCuaCo(context).danhSach().firstOrNull { it.noiDung == chu }
        }
        // Cau tra loi ghi ngay sau khi lam xong lenh. Doi them mot chut roi moi doc.
        Thread.sleep(3_000)
        val cho = CountDownLatch(1)
        var traLoi = "khong doc duoc"
        nha.collection("hop").document("trangthai").get().addOnCompleteListener {
            val o = it.result?.get("traLoi") as? Map<*, *>
            val luc = o?.get("luc") as? Long ?: 0L
            traLoi = if (luc >= batDau) "\"${o?.get("chu")}\"" else "chua co"
            cho.countDown()
        }
        cho.await(20, TimeUnit.SECONDS)

        val sua = sp.edit()
        if (khoCu == null) sua.remove("tin_cua_co") else sua.putString("tin_cua_co", khoCu)
        sua.commit()

        println(
            "MANUAL_DONGBO: tinco " + when {
                tin == null -> "KHONG VAO KHO"
                tin.luc == toiQua -> "vao kho, dung gio gui"
                else -> "vao kho nhung SAI GIO: ${tin.luc}, gui luc $toiQua"
            } + ", traLoi=$traLoi"
        )
    }

    /**
     * Thu xoa chat/ cu tren du an thu, y nhu ban tablet moi lam mot lan luc noi.
     *
     * Dat ba tin gia vao chat/, bo dau "da xoa", goi [DongBo.xoaChatCu], roi dem lai:
     * phai con 0. Chi chay voi ban go loi, tuc la du an thu.
     *
     * Khong goi DongBo.batDau nhu cac lenh khac: ham do tu xoa chat/ luc noi, se xoa
     * ngay trong luc dang dat tin gia va so "truoc" ra 0. Dang nhap Firebase van con tu
     * lan app chay truoc, nen doc ghi van qua luat.
     */
    @Test
    fun xoaChatCuThu() {
        val maNha = DongBo.maNhaHienTai(context)
        if (maNha.isEmpty()) {
            println("MANUAL_DONGBO: may nay chua lap nha, khong thu duoc")
            return
        }
        val chat = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("nha").document(maNha).collection("chat")

        fun cho(viec: com.google.android.gms.tasks.Task<*>) {
            val xong = CountDownLatch(1)
            viec.addOnCompleteListener { xong.countDown() }
            xong.await(20, TimeUnit.SECONDS)
        }
        fun dem(): Int {
            var so = -1
            val xong = CountDownLatch(1)
            chat.get().addOnCompleteListener { so = it.result?.size() ?: -1; xong.countDown() }
            xong.await(20, TimeUnit.SECONDS)
            return so
        }

        repeat(3) { i ->
            cho(chat.add(mapOf("tu" to "CON", "chu" to "tin thu $i", "luc" to System.currentTimeMillis())))
        }
        val truoc = dem()
        Prefs.get(context).raw().edit().remove("dongbo_da_xoa_chat_cu").commit()

        val xong = CountDownLatch(1)
        var rong = false
        DongBo.xoaChatCu(context) { rong = it; xong.countDown() }
        xong.await(60, TimeUnit.SECONDS)
        val sau = dem()
        println("MANUAL_DONGBO: chat cu truoc=$truoc sau=$sau rong=$rong")
    }

    /**
     * Thu duong so dung app: go PING nhu app Bang dieu khien van go, roi doc hop/sudung.
     *
     * Them vao kho hai khoang thu: mot khoang hom nay, va mot khoang tu tam ngay truoc,
     * tuc la qua han bay ngay. Ban tren Firestore phai co khoang hom nay kem ten app, va
     * khong co khoang qua han. Chay xong tra kho ve nhu cu roi day lai mot lan, de ban
     * tren du an thu khong con hai khoang do.
     */
    @Test
    fun suDungThu() {
        DongBo.batDau(context)
        val maNha = DongBo.maNhaHienTai(context)
        if (maNha.isEmpty()) {
            println("MANUAL_DONGBO: may nay chua lap nha, khong thu duoc")
            return
        }
        val nha = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("nha").document(maNha)
        val so = nha.collection("hop").document("sudung")
        val kho = Prefs.khoRieng(context, "nhat_ky_su_dung")
        val khoCu = kho.getString("su_dung_doan", null)

        fun docSo(): Map<String, Any>? {
            val cho = CountDownLatch(1)
            var ra: Map<String, Any>? = null
            so.get(com.google.firebase.firestore.Source.SERVER).addOnCompleteListener {
                ra = it.result?.data
                cho.countDown()
            }
            cho.await(20, TimeUnit.SECONDS)
            return ra
        }

        val bayGio = System.currentTimeMillis()
        val ngay = 24 * 3_600_000L
        val homNay = "thu.sudung.homnay|${bayGio - 20 * 60_000L}|${bayGio - 10 * 60_000L}"
        val quaHan = "thu.sudung.quahan|${bayGio - 8 * ngay}|${bayGio - 8 * ngay + 30 * 60_000L}"
        kho.edit().putString(
            "su_dung_doan",
            listOfNotNull(khoCu?.takeIf { it.isNotBlank() }, quaHan, homNay).joinToString("\n")
        ).commit()

        var ban: Map<String, Any>? = null
        var truoc = 0L
        try {
            truoc = docSo()?.get("capNhatLuc") as? Long ?: 0L
            nha.collection("lenh").add(
                mapOf("kieu" to "PING", "ai" to "bahuy", "tao" to System.currentTimeMillis())
            )
            val batDau = System.currentTimeMillis()
            do {
                Thread.sleep(1_000)
                ban = docSo()
            } while ((ban?.get("capNhatLuc") as? Long ?: 0L) == truoc &&
                System.currentTimeMillis() - batDau < 30_000L)
        } finally {
            val sua = kho.edit()
            if (khoCu == null) sua.remove("su_dung_doan") else sua.putString("su_dung_doan", khoCu)
            sua.commit()
            DongBo.daySuDung(context)
            Thread.sleep(3_000)
        }

        val sau = ban?.get("capNhatLuc") as? Long ?: 0L
        val doan = (ban?.get("doan") as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
        val goi = doan.map { it["goi"] }
        val han = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_MONTH, -6)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val ngoaiHan = doan.count { (it["den"] as? Long ?: 0L) < han }
        val ten = (ban?.get("app") as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
            .firstOrNull { it["goi"] == "thu.sudung.homnay" }?.get("ten")
        val sach = docSo()?.get("doan").toString().contains("thu.sudung")

        println(
            "MANUAL_DONGBO: sudung truoc=$truoc sau=$sau " +
                (if (sau > truoc) "-> TABLET DA GHI" else "-> KHONG GHI") +
                ", doan=${doan.size}" +
                ", khoang hom nay " + (if ("thu.sudung.homnay" in goi) "co (dung)" else "KHONG CO (SAI)") +
                ", khoang qua han " + (if ("thu.sudung.quahan" in goi) "CON (SAI)" else "da bo (dung)") +
                ", ngoai han=$ngoaiHan" + (if (ngoaiHan == 0) " (dung)" else " (SAI)") +
                ", ten=$ten, giuNgay=${ban?.get("giuNgay")}, dangGhi=${ban?.get("dangGhi")}" +
                ", don lai " + (if (sach) "CON KHOANG THU (SAI)" else "sach (dung)")
        )
    }
}
