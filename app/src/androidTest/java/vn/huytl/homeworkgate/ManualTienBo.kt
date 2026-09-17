package vn.huytl.homeworkgate

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.TraLoi
import vn.huytl.homeworkgate.ui.TienBoActivity

/**
 * Khong phai test that. Cai cong tay de nhin man "Con da lam duoc gi" tren may ao
 * ma khong phai ngoi lam bai tap ca tuan cho no co so lieu.
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualTienBo#napThu \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *
 *   ...#mo    mo thang man hinh do ra de chup anh
 *   ...#xoa   xoa sach so cai
 */
@RunWith(AndroidJUnit4::class)
class ManualTienBo {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    private fun ngayTruoc(lui: Int): Long =
        System.currentTimeMillis() - lui * 24L * 60 * 60_000L

    @Test
    fun napThu() {
        NganHang.napNeuCan(context)
        val kho = KhoBai.get(context)
        val sach = NganHang.SACH.first()
        val cac = kho.cacBaiCua(sach.nguon).take(4)
            .flatMap { kho.cacCauCua(sach.nguon, it.bai) }
            .take(14)
        if (cac.isEmpty()) {
            println("MANUAL_TIENBO: chua nap duoc ngan hang cau hoi")
            return
        }

        // Muoi cau dung ngay lan dau, rai ra bon ngay khac nhau.
        cac.take(10).forEachIndexed { i, c ->
            kho.ghiTraLoi(
                TraLoi(
                    cauId = c.id, mon = c.mon, ma = c.ma, de = c.de,
                    ketQua = "x = $i", dung = true, phut = 2,
                    nhanXet = "", luc = ngayTruoc(i % 4)
                )
            )
        }

        // Ba cau sai truoc, sua dung sau: day la "cau kho da go".
        cac.drop(10).take(3).forEach { c ->
            kho.ghiTraLoi(
                TraLoi(
                    cauId = c.id, mon = c.mon, ma = c.ma, de = c.de,
                    ketQua = "sai", dung = false, phut = 0,
                    nhanXet = "con nhầm dấu ở dòng cuối", luc = ngayTruoc(3)
                )
            )
            kho.ghiTraLoi(
                TraLoi(
                    cauId = c.id, mon = c.mon, ma = c.ma, de = c.de,
                    ketQua = "đúng rồi", dung = true, phut = 2,
                    nhanXet = "", luc = ngayTruoc(2)
                )
            )
        }

        // Mot cau dang con no, de thay dong chan.
        cac.last().let { c ->
            kho.ghiTraLoi(
                TraLoi(
                    cauId = c.id + "-no", mon = "KHTN", ma = c.ma, de = c.de,
                    ketQua = "sai", dung = false, phut = 0,
                    nhanXet = "xem lại bước hai", luc = ngayTruoc(0)
                )
            )
        }
        println("MANUAL_TIENBO: da nap ${cac.size} cau")
    }

    @Test
    fun mo() {
        context.startActivity(
            Intent(context, TienBoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        Thread.sleep(2_000)
    }

    @Test
    fun xoa() {
        KhoBai.get(context).xoaHetTraLoi()
        println("MANUAL_TIENBO: da xoa so cai")
    }
}
