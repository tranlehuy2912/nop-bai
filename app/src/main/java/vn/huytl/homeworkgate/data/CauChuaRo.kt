package vn.huytl.homeworkgate.data

import android.content.Context
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Cau Claude doc chua chac ma cham chua dung, dang cho Ba Huy xem (30/9/2026). An khoi danh
 * sach "câu cần sửa" cua Le Hoa.
 *
 * VI SAO CO. So cai ghi moi cau cham ra chua dung, ke ca cau Claude doc chua chac, de lan nop
 * sau biet do la sua bai cu. Nhung the bai o man Bai da cham ghi cau do "Ba Huy sẽ xem lại" va
 * khong co nut nop lai, trong khi dong "Có N câu cần sửa" van dem no: con thay cau phai sua ma
 * khong lam gi duoc. Bai KHTN 21:15 ngay 28/9/2026 co 5 cau nhu vay. Ba Huy chon an cau do
 * khoi danh sach cua con.
 *
 * Ba Huy xu o the "Câu cần Ba Huy xem" tren dien thoai (lenh XU_CAU), trong luc bai con cho:
 * Dung thi so ghi xong; Sai thi so ghi mot dong sai moi, sau luc danh dau, nen cau hien ra;
 * Chup lai thi bo danh dau o day ([bo]), cau hien ra kem nut nop lai.
 *
 * NHO LUC CUA DONG SAI, KHONG CHI MA CAU, nhu [BoSua]: lan sau cau do bi cham sai ma doc ro
 * thi dong moi ghi sau luc danh dau, va cau hien lai, vi do la loi con phai sua.
 *
 * So cai khong doi gi. Nam trong kho prefs rieng nhu [BoSua]. Mat kho nay (cai lai app) thi
 * cac cau do hien lai trong danh sach, Ba Huy bo bang nut "Không bắt sửa câu sai".
 */
object CauChuaRo {

    private const val KHO = "cau_chua_ro"

    /** Lan sai luc [lucSai] cua cau [khoa] la lan Claude doc chua chac, chua ai xu. */
    fun dangAn(context: Context, khoa: String, lucSai: Long): Boolean =
        runCatching { Prefs.khoRieng(context, KHO).getLong(khoa, 0L) }.getOrDefault(0L)
            .let { it > 0L && it >= lucSai }

    /**
     * Danh dau sau mot lan ghi so: cau nao trong [cham] Claude doc chua chac ma cham chua dung,
     * va vua ghi thanh dong sai trong [daGhi], thi an theo luc cua dong do.
     */
    fun danhDau(context: Context, cham: List<CauCham>, daGhi: List<TraLoi>) {
        val chuaRo = cham.filter { !it.docRo && !it.dung }.map { SoCaiBai.khoaCua(it) }.toSet()
        val cac = daGhi.filter { !it.dung && it.cauId in chuaRo }
        if (cac.isEmpty()) return
        runCatching {
            val sua = Prefs.khoRieng(context, KHO).edit()
            cac.forEach { sua.putLong(it.cauId, it.luc) }
            sua.commit()
        }
    }

    /** Ba Huy bam Chup lai o cau [khoa]: thoi an, cau hien trong danh sach cua con. */
    fun bo(context: Context, khoa: String) {
        if (khoa.isEmpty()) return
        runCatching { Prefs.khoRieng(context, KHO).edit().remove(khoa).commit() }
    }

    /** Chi test goi: xoa het, de moi test bat dau tu kho trong. */
    internal fun xoaHet(context: Context) {
        runCatching { Prefs.khoRieng(context, KHO).edit().clear().commit() }
    }
}
