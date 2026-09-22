package vn.huytl.homeworkgate

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.ai.DocDanDo
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.telegram.DanDoSender
import java.io.File

/**
 * Khong phai test that. Thu cau lenh doc vo dan do tren anh that, khong can camera.
 *
 * Camera ao cua emulator khong chup duoc (Camera3-Stream: timestamp is not
 * increasing), nen khong di het duong that tren may ao duoc. Cho nay day thang anh
 * vao [DocDanDo], de xem no tach dung tung buoi khong va tich dung dong nao la bai
 * tap khong.
 *
 * DUNG ANH THAT, DUNG VE ANH GIA. Ban dau cho nay ve mot trang vo bang Canvas va
 * moi thu deu chay dung; den luc dua bon trang vo that cua Le Hoa vao thi lo ra ba
 * cho hong ma anh gia khong bao gio de ra: mot trang co hai ba buoi chep noi nhau,
 * ngay viet ca bang tieng Anh, va chu so viet tay doc nham thuong xuyen.
 *
 * GOI AI THAT, MOI TAM ANH TON MOT LAN CHAM vao han muc khoa.
 */
@RunWith(AndroidJUnit4::class)
class ManualDanDo {

    /**
     * Doc anh THAT do Le Hoa chup, day san vao /sdcard/dando/.
     *
     *   adb push trang1.jpg /data/data/vn.huytl.homeworkgate/files/dando/  (can adb root)
     *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualDanDo#docAnhThat \
     *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
     */
    @Test
    fun docAnhThat() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val thuMuc = File(context.filesDir, "dando")
        val cac = thuMuc.listFiles()?.sortedBy { it.name }.orEmpty()
        if (cac.isEmpty()) {
            Log.w(TAG, "khong co anh nao trong ${thuMuc.absolutePath}")
            return
        }
        cac.forEach { f ->
            val ket = DocDanDo.doc(context, listOf(f))
            Log.i(TAG, "===== ${f.name}  loi='${ket.loi}'  ${ket.cacNgay.size} ngay")
            ket.cacNgay.forEach { d ->
                Log.i(TAG, "  ngay ${d.ngay}")
                d.cacDong.forEach {
                    Log.i(TAG, "    ${if (it.laBaiTap) "[x]" else "[ ]"} ${it.chu}")
                }
            }
        }
    }

    /**
     * Nap san mot ban dan do de xem man soat ma khong ton mot lan goi AI.
     *
     *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualDanDo#luuThuDeXemMan \
     *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
     *   adb shell am force-stop vn.huytl.homeworkgate
     */
    @Test
    fun luuThuDeXemMan() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        VoDanDo.luu(
            context,
            VoDanDo.DanDo(
                ngay = java.time.LocalDate.now().toString(),
                cacDong = listOf(
                    VoDanDo.Dong("Ngữ Văn: Ôn bài tuần sau KT", false),
                    VoDanDo.Dong("KHTN: Ôn bài tuần sau KT", false),
                    VoDanDo.Dong("TTNT: Làm bài cô giao", true),
                    VoDanDo.Dong("ÂNhạc: Mang sách vở đầy đủ", false)
                )
            )
        )
        Log.i(TAG, "da luu: ${VoDanDo.doc(context)}")
    }

    /**
     * In ra dong chu se gui cho Ba Huy, khong goi mang.
     *
     * Kiem bang mat la du: cai dang test la cach dien dat, ma cho do khong co ham
     * nao khang dinh thay nguoi doc duoc.
     */
    @Test
    fun inChuGuiChoBa() {
        val coBai = VoDanDo.DanDo(
            ngay = "2026-09-16",
            cacDong = listOf(
                VoDanDo.Dong("Mỹ thuật: Vẽ ký họa dáng người trên giấy A4", true, true),
                VoDanDo.Dong("STEM: Mang tập vở đầy đủ", false, false),
                VoDanDo.Dong("TOÁN: Làm luyện tập 3 trang 59", true, false),
                VoDanDo.Dong("LS-ĐL: Sinh hoạt ngoài trời", false, true),
                VoDanDo.Dong("KHTN: làm bài 4 trang 27", true, null)
            )
        )
        val khongBai = VoDanDo.DanDo(
            ngay = "2026-09-14",
            cacDong = listOf(
                VoDanDo.Dong("Tiếng Anh: Tiết sau kiểm tra từ vựng", false, false),
                VoDanDo.Dong("KHTN: Tiết sau kiểm tra bài 2 bài 3", false, false),
                VoDanDo.Dong("STEAM: Mang sách vở đầy đủ", false, false)
            )
        )
        Log.i(TAG, "===== NGAY CO BAI TAP\n" + DanDoSender.chuThich(coBai))
        Log.i(TAG, "===== NGAY KHONG CO BAI TAP\n" + DanDoSender.chuThich(khongBai))
    }

    private companion object {
        const val TAG = "ManualDanDo"
    }
}
