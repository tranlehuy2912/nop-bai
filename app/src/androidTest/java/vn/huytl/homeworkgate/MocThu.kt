package vn.huytl.homeworkgate

import android.content.Context
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Dat bai da hoc cho test va cong cu tay quay, ghi thang bang [HocToi.ghiDaHoc] (khong de lai
 * dong nhat ky).
 *
 * Tu 2/10/2026 bai da hoc la mot tap bai moi mon ([HocToi]), con danh dau tung bai. Test cu viet
 * "lop hoc toi Bai 9" (ca mon Toan, hay tung phan); o day doi ra tap moi bai tu bai dau toi so
 * do, dung nhu cach moc cu tu chuyen. Truoc ngay do file nay la MocToanThu, chi lo hai phan Toan.
 */
object MocThu {

    val MON = listOf(PhanHoc.TOAN, PhanHoc.KHTN, PhanHoc.TIENG_ANH)

    /** Bai da hoc cua ca ba mon, de [tra] lai sau khi test xong. */
    fun luu(context: Context): Map<String, Set<Int>?> = MON.associateWith { HocToi.daHoc(context, it) }

    fun tra(context: Context, cu: Map<String, Set<Int>?>) = cu.forEach { (mon, cac) -> dat(context, mon, cac) }

    /** Ca ba mon ve "chua chon". */
    fun xoaHet(context: Context) = MON.forEach { HocToi.xoa(context, it) }

    /** Dat thang tap bai cua [mon]; null la chua chon. */
    fun dat(context: Context, mon: String, cac: Set<Int>?) {
        HocToi.xoa(context, mon)
        cac?.let { HocToi.ghiDaHoc(context, mon, it) }
    }

    /** Toan hoc toi Bai [so] cua ca sach: moi bai tu 1 toi so, ca hai phan. null la chua chon. */
    fun datToan(context: Context, so: Int?) = dat(context, PhanHoc.TOAN, so?.let { (1..it).toSet() })

    /**
     * Phan [ma] hoc toi bai [so] (0 la chua hoc bai nao cua phan), giu nguyen bai cua cac phan
     * khac trong mon. Mon chua chon thi thanh da chon voi dung cac bai nay.
     */
    fun datPhan(context: Context, ma: String, so: Int) {
        val p = PhanHoc.theoMa(ma)!!
        val khac = HocToi.daHoc(context, p.mon).orEmpty().filterNot { p.chua(it) }
        HocToi.ghiDaHoc(context, p.mon, (khac + p.cacSoToi(so)).toSet())
    }

    /** Tieng Anh hoc toi Unit [n]: Unit 1 toi n. 0 la chua hoc Unit nao, null la chua chon. */
    fun datUnit(context: Context, n: Int?) =
        dat(context, PhanHoc.TIENG_ANH, n?.let { if (it <= 0) emptySet() else (1..it).toSet() })
}
