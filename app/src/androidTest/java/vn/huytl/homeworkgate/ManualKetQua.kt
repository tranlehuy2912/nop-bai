package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.dongbo.Duong
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Khong phai test. Dung de NHIN man Bai da cham voi mot ban cham that.
 *
 * Man [vn.huytl.homeworkgate.ui.KetQuaActivity] doc tu Firestore, nen trong nha phai
 * co bai da cham thi moi co gi de nhin. O day ghi mot bai mau, lay nguyen ban cham
 * cua bai nop 10:23 ngay 23/9/2026: 12 cau, trong do may chep sai chu cua cau 2.32b
 * va cham nham cau 2.33a.
 *
 * Ban go loi noi vao du an Firebase THU, nen bai mau khong vao nha that.
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualKetQua#napThu \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *   adb shell am start -n vn.huytl.homeworkgate/.ui.KetQuaActivity
 *
 *   # xoa bai mau
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualKetQua#xoaThu \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *
 * Muon nhin nut "Nộp lại N câu sai" (tu 28/9/2026) thi chay [napBaiSai]: bai do co
 * phan khai theo sach va ghi ca so cai, nen man ket qua biet cau nao con phai sua.
 */
@RunWith(AndroidJUnit4::class)
class ManualKetQua {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Ghi bai mau bang dung hai ham ApprovalService dung khi con nop that. */
    @Test
    fun napThu() {
        DongBo.dayBaiMoi(context, ID_MAU, 0L, emptyList())
        DongBo.dayChamBai(
            context, ID_MAU,
            mapOf(
                "mon" to "Toán",
                "tomTat" to "Bai mau cho ManualKetQua, khong phai bai that.",
                "phutDeNghi" to 3,
                "lamHetDanDo" to false,
                "cac" to CAC.map { (ma, de, ketQua, dung, nhanXet) ->
                    mapOf(
                        "ma" to ma,
                        "de" to de,
                        "ketQua" to ketQua,
                        "dung" to dung,
                        "docRo" to true,
                        "soDong" to 1,
                        "nhanXet" to nhanXet
                    )
                }
            )
        )
        choGhiXong("napThu")
    }

    /**
     * Gan them ket qua Claude cham lai vao bai mau, y nhu app Bang dieu khien ghi.
     *
     * Claude doc dung chu cau 2.32b va cham lai 2.33a la dung. Chay sau [napThu].
     *
     *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualKetQua#napClaude \
     *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
     */
    @Test
    fun napClaude() {
        val maNha = DongBo.maNhaHienTai(context)
        if (maNha.isEmpty()) {
            println("MANUAL_KETQUA: may nay chua lap nha")
            return
        }
        val doc = mapOf(
            "2.32b" to Triple(true, "1000000000", ""),
            "2.33a" to Triple(true, "8x^2 + 20xy", ""),
            "2.34b" to Triple(
                false, "(8x - 3y)(16x^2 + 24xy + 9y^2)",
                "Xem lại: 64x^3 là lập phương của số nào, rồi kiểm lại hạng tử giữa."
            )
        )
        val cac = CAC.map { m ->
            val c = doc[m.ma]
            mapOf(
                "ma" to m.ma,
                "dung" to (c?.first ?: m.dung),
                "chac" to true,
                "conViet" to (c?.second ?: m.ketQua),
                "goiY" to (c?.third ?: "")
            )
        }
        FirebaseFirestore.getInstance().collection(Duong.NHA).document(maNha)
            .collection(Duong.BAI).document(ID_MAU)
            .update(Duong.F_CHAM_CLAUDE, mapOf("luc" to System.currentTimeMillis(), "cac" to cac))
        choGhiXong("napClaude")
    }

    /**
     * Mot bai 5 cau SBT, 3 dung 2 sai, da duyet 12 phut: dung canh Ba Huy ke ngay
     * 28/9/2026. Ghi ca phan khai (ma sach tung cau) va so cai (3 cau da tra gio, 2 cau
     * sai), y nhu ApprovalService ghi sau khi cham theo Claude. Mo man ket qua la thay
     * nut "Nộp lại 2 câu sai" tren the bai nay.
     *
     *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualKetQua#napBaiSai \
     *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
     *   adb shell am start -n vn.huytl.homeworkgate/.ui.KetQuaActivity
     *
     * Ghi so cai that cua may ao. Dung chay tren tablet cua Le Hoa.
     */
    @Test
    fun napBaiSai() {
        NganHang.napNeuCan(context)
        val cau = KhoBai.get(context).cacCauTheoId(ID_SBT)
        check(cau.size == ID_SBT.size) { "ngan hang SBT chua nap du" }
        val pham = PhamVi(
            mon = "Toán", nguon = "sbttoan8t1", tenNguon = "SBT Toán 8 tập một",
            bai = "Ôn tập chương II", cauIds = ID_SBT
        )
        DongBo.dayBaiMoi(context, ID_SAI, 0L, emptyList(), DongBo.banKhai(context, pham))
        val cham = cau.map { c ->
            val sai = c.ma in MA_SAI
            CauCham(
                ma = c.ma, de = c.de, cauId = c.id, mon = "Toán", dung = !sai, soDong = 1,
                ketQua = if (sai) "999000" else "đúng",
                nhanXet = if (sai) {
                    "Dòng 1 viết (x - 3)^3 rồi thay x = 103 được 100^3 là đúng, nhưng dòng 2 " +
                        "tính 100^3 ra 999000. Tính lại 100 nhân 100 nhân 100 ở dòng 2."
                } else {
                    ""
                }
            )
        }
        DongBo.dayChamBai(
            context, ID_SAI,
            mapOf(
                "mon" to "Toán",
                "tomTat" to "Bai mau cho ManualKetQua.napBaiSai, khong phai bai that.",
                "phutDeNghi" to 12,
                "lamHetDanDo" to false,
                "cac" to cham.map { c ->
                    mapOf(
                        "ma" to c.ma, "de" to c.de, "ketQua" to c.ketQua, "dung" to c.dung,
                        "docRo" to true, "soDong" to c.soDong, "nhanXet" to c.nhanXet
                    )
                }
            )
        )
        DongBo.datTrangThaiBai(context, ID_SAI, "DUYET", 12)
        SoCaiBai.ghi(context, cham, cham.filter { it.dung }.associate { it.ma to 4 })
        choGhiXong("napBaiSai")
    }

    @Test
    fun xoaThu() {
        val maNha = DongBo.maNhaHienTai(context)
        if (maNha.isEmpty()) {
            println("MANUAL_KETQUA: may nay chua lap nha")
            return
        }
        listOf(ID_MAU, ID_SAI).forEach {
            FirebaseFirestore.getInstance().collection(Duong.NHA).document(maNha)
                .collection(Duong.BAI).document(it).delete()
        }
        choGhiXong("xoaThu")
    }

    /**
     * Cho may chu nhan xong moi thoat.
     *
     * Chay xong la tien trinh test tat. Khong cho thi lan ghi con nam trong hang doi
     * cua Firestore trong may, va man ket qua mo ra van thay rong.
     */
    private fun choGhiXong(ten: String) {
        val cho = CountDownLatch(1)
        var ket = "het gio cho"
        FirebaseFirestore.getInstance().waitForPendingWrites()
            .addOnSuccessListener { ket = "xong" }
            .addOnFailureListener { ket = "hong: ${it.message}" }
            .addOnCompleteListener { cho.countDown() }
        cho.await(30, TimeUnit.SECONDS)
        println("MANUAL_KETQUA: $ten $ket, nha=${DongBo.maNhaHienTai(context)}")
    }

    private data class Mau(
        val ma: String,
        val de: String,
        val ketQua: String,
        val dung: Boolean,
        val nhanXet: String = ""
    )

    private companion object {
        const val ID_MAU = "thu-ket-qua"

        /** Bai cua [napBaiSai]. */
        const val ID_SAI = "thu-nop-lai"
        val ID_SBT = listOf("2.19a", "2.19b", "2.20a", "2.20b", "2.20c").map { "sbttoan8t1:$it" }
        val MA_SAI = setOf("2.19b", "2.20b")

        val CAC = listOf(
            Mau(
                "2.28",
                "Đa thức x^2 − 9x + 8 được phân tích thành tích của hai đa thức: A. x − 1 và x + 8. " +
                    "B. x − 1 và x − 8. C. x − 2 và x − 4. D. x − 2 và x + 4.",
                "B", true
            ),
            Mau(
                "2.29",
                "Khẳng định nào sau đây là đúng? A. (A − B)(A + B) = A^2 + 2AB + B^2. " +
                    "B. (A + B)(A − B) = A^2 − 2AB + B^2. C. (A + B)(A − B) = A^2 + B^2. " +
                    "D. (A + B)(A − B) = A^2 − B^2.",
                "D", true
            ),
            Mau(
                "2.30",
                "Biểu thức 25x^2 + 20xy + 4y^2 viết dưới dạng bình phương của một tổng là: " +
                    "A. [5x + (−2y)]^2. B. [2x + (−5y)]^2. C. (2x + 5y)^2. D. (5x + 2y)^2.",
                "D", true
            ),
            Mau(
                "2.31",
                "Rút gọn biểu thức A = (2x + 1)^3 − 6x(2x + 1) ta được: A. x^3 + 8. B. x^3 + 1. " +
                    "C. 8x^3 + 1. D. 8x^3 − 1.",
                "C", true
            ),
            Mau("2.32a", "Tính nhanh giá trị của biểu thức x^2 − 4x + 4 tại x = 102.", "10000", true),
            Mau(
                "2.32b", "Tính nhanh giá trị của biểu thức x^3 + 3x^2 + 3x + 1 tại x = 999.",
                "11000000000", false,
                "Kết quả \"11000000000\" chưa đúng khi con thay x = 999 vào biểu thức."
            ),
            Mau(
                "2.33a", "Rút gọn biểu thức (2x − 5y)(2x + 5y) + (2x + 5y)^2.",
                "8x^2 + 20xy", false,
                "Biểu thức \"8x^2 + 20xy\" còn thiếu hạng tử chứa y^2 sau khi khai triển."
            ),
            Mau(
                "2.33b",
                "Rút gọn biểu thức (x + 2y)(x^2 − 2xy + 4y^2) + (2x − y)(4x^2 + 2xy + y^2).",
                "9x^3 - 7y^3", false,
                "Kết quả \"9x^3 - 7y^3\" sai ở hệ số của y^3 sau khi thu gọn."
            ),
            Mau(
                "2.34a", "Phân tích đa thức 6x^2 − 24y^2 thành nhân tử.",
                "6(x^2 - 2y^2)(x^2 + 2y^2)", false,
                "Đoạn \"6(x^2 - 2y^2)(x^2 + 2y^2)\" sai vì 2y^2 chưa đưa hết được về bình phương."
            ),
            Mau(
                "2.34b", "Phân tích đa thức 64x^3 − 27y^3 thành nhân tử.",
                "(8x - 3y)(16x^2 + 24xy + 9y^2)", false,
                "Đoạn \"(8x - 3y)(16x^2 + 24xy + 9y^2)\" sai ở bình phương số đầu tiên trong ngoặc."
            ),
            Mau(
                "2.34c", "Phân tích đa thức x^4 − 2x^3 + x^2 thành nhân tử.",
                "x^2(x^2 - 2x + 1)", false,
                "Đoạn \"x^2(x^2 - 2x + 1)\" chưa phân tích tiếp đa thức trong ngoặc thành nhân tử."
            ),
            Mau(
                "2.34d", "Phân tích đa thức (x − y)^3 + 8y^3 thành nhân tử.",
                "(x - 7y)(x - y^2 + (x - y).2y + 4y^2)", false,
                "Đoạn \"(x - 7y)(x - y^2 + ...)\" sai công thức tổng hai lập phương ở nhân tử thứ hai."
            )
        )
    }
}
