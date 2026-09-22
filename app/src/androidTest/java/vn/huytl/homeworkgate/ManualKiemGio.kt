package vn.huytl.homeworkgate

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TraLoi
import vn.huytl.homeworkgate.kho.TraThe
import vn.huytl.homeworkgate.ui.CachKiemGioActivity

/**
 * Khong phai test that. Cai cong tay de nhin man "Cach kiem gio choi" tren may ao o
 * cac the ngay khac nhau, ma khong phai ngoi chup mot xap bai that.
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualKiemGio#mo \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *
 *   ...#daCong   ca ba phan deu da cong gio hom nay
 *   ...#coGoi    chi tinh tron goi, hai phan kia van trong
 *   ...#xoa      don lai de ve ngay trang
 *
 * VI SAO PHAI CO MAY THE NGAY NAY. Man do tra loi hai cau hoi ma chi nhin duoc khi
 * co so lieu that: hom nay phan nao da cong, phan nao chua. Va tran lam them 90 phut
 * chi song vao hom da tinh goi (xem bien conTran trong [LuatCongGio.tinh]), nen dong
 * ghi chu duoi the bai tap noi hai cau khac han nhau tuy hom. Ngoi cho du lieu that
 * de kiem hai nhanh do thi mat ca ngay.
 */
@RunWith(AndroidJUnit4::class)
class ManualKiemGio {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Test
    fun mo() {
        context.startActivity(
            Intent(context, CachKiemGioActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        Thread.sleep(2_000)
    }

    /** Ca ba phan deu da cong gio hom nay: de nhin ba huy hieu o the xanh. */
    @Test
    fun daCong() {
        val kho = KhoBai.get(context)
        SoCaiBai.ghiGoi(context, LuatCongGio.PHUT_TRON_GOI_DAN_DO)
        kho.ghiTraLoi(
            TraLoi(
                cauId = MA_BAI, mon = "Toán", ma = "Bài 1", de = "thử",
                ketQua = "", dung = true, phut = 12,
                nhanXet = "", luc = System.currentTimeMillis()
            )
        )
        kho.ghiTraThe(TraThe(theId = MA_THE, go = "thử", dung = true, phut = 6))
        ViecNha.congPhutHomNay(context, 20)
        println("MANUAL_KIEMGIO: goi 45 + lam them 12 + kiem tra bai 6 + viec nha 20")
    }

    /** Chi danh dau goi, de nhin the "da tinh goi" ma cac phan kia van trong. */
    @Test
    fun coGoi() {
        SoCaiBai.ghiGoi(context, LuatCongGio.PHUT_TRON_GOI_DAN_DO)
        println("MANUAL_KIEMGIO: da danh dau goi hom nay")
    }

    /**
     * Don dung nhung dong [daCong] vua nap, khong dung toi so cai that.
     *
     * Xoa thang bang SQL chu khong qua [KhoBai.xoaHetTraLoi]: ham do xoa sach ca nam
     * hoc, ma may ao thi con dang giu bai cua nhung lan thu khac. Mot cai cong tay
     * khong duoc phep don nha rong hon pham vi no bay ra.
     *
     * KHONG DON so phut viec nha. Muon don thi phai them mot ham dat-ve-khong trong
     * [ViecNha], ma mot ham chi ton tai de phuc vu cong tay nay thi khong dang; con
     * so do tu het hieu luc sang hom sau.
     */
    @Test
    fun xoa() {
        val db = KhoBai.get(context).writableDatabase
        db.delete("tra_loi", "cau_id IN (?, ?)", arrayOf(KhoBai.CAU_GOI, MA_BAI))
        db.delete("tra_the", "the_id = ?", arrayOf(MA_THE))
        println("MANUAL_KIEMGIO: da don cac dong vua nap (tru so phut viec nha)")
    }

    private companion object {
        const val MA_BAI = "manual:kiemgio"
        const val MA_THE = "manual:kiemgio-the"
    }
}
