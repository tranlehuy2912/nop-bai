package vn.huytl.homeworkgate

import android.content.Context
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Dat moc Toan cho test va cong cu tay quay, theo so bai cua ca sach.
 *
 * Tu 30/9/2026 Toan hai phan Dai so va Hinh hoc, bai xen nhau trong sach ([PhanHoc]). Test cu
 * viet "lop hoc toi Bai 9" cho ca Toan; o day doi ra hai moc: moi phan lay bai lon nhat cua no
 * khong qua so do, phan chua co bai nao thi "chua hoc". Ghi thang bang [HocToi.ghiBai], khong
 * de lai dong nhat ky.
 */
object MocToanThu {

    private fun cacPhan() = PhanHoc.cuaMon("Toán")

    /** [so] null la xoa ca hai phan (chua chon), 0 la chua hoc bai nao. */
    fun dat(context: Context, so: Int?) {
        cacPhan().forEach { HocToi.xoa(context, it.ma) }
        if (so == null) return
        cacPhan().forEach { p ->
            val den = p.cacSoToi(so).lastOrNull()
            val ten = if (den == null) HocToi.CHUA_HOC_BAI_NAO
            else PhanHoc.cacBai(context, p).first { PhanHoc.soBai(it) == den }
            HocToi.ghiBai(context, p.ma, ten)
        }
    }

    fun luu(context: Context): Map<String, String?> =
        cacPhan().associate { it.ma to HocToi.baiCua(context, it.ma) }

    fun tra(context: Context, cu: Map<String, String?>) {
        cu.forEach { (ma, bai) ->
            HocToi.xoa(context, ma)
            bai?.let { HocToi.ghiBai(context, ma, it) }
        }
    }
}
