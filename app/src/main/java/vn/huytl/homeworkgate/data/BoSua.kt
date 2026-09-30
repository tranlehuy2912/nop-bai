package vn.huytl.homeworkgate.data

import android.content.Context

/**
 * Cau Ba Huy da bo khoi danh sach "câu cần sửa" (lenh BOSUA, 30/9/2026). Khong cong phut.
 *
 * VI SAO CO. Dong "Có N câu cần sửa" o man chinh truoc day chi mat khi con sua dung, ma so
 * cai nho mot nam. Ngay 30/9/2026 no dang ghi 19 cau, co cau ngoai sach tu may hom truoc
 * con khong con giu de sua. Ba Huy muon co cach bo di ma khong phai go "đúng" cho cau con
 * chua lam dung, vi go dung la cong phut.
 *
 * NHO LUC BO, KHONG CHI MA CAU. Bo mot cau la bo lan sai hien tai cua no. Con nop lai cau
 * do va lai sai (dong sai moi ghi sau luc bo) thi cau hien lai, vi day la mot loi moi.
 *
 * So cai khong doi gi: dong sai van nam do, nen cau van chi tra gio mot lan, va lich on,
 * Tien bo van thay no.
 *
 * Nam trong kho prefs rieng nhu [LuotDangLam]: file prefs chinh thi moi lan ghi keo theo mot
 * luot ghi Firestore. Mat kho nay (cai lai app) thi cac cau da bo hien lai, Ba Huy bo lai.
 */
object BoSua {

    private const val KHO = "bo_sua"

    /** Luc Ba Huy bo cau [khoa], 0 khi chua bo lan nao. */
    fun lucBo(context: Context, khoa: String): Long =
        runCatching { Prefs.khoRieng(context, KHO).getLong(khoa, 0L) }.getOrDefault(0L)

    /** Cau co lan sai luc [lucSai] da bi Ba Huy bo chua. */
    fun daBo(context: Context, khoa: String, lucSai: Long): Boolean =
        lucBo(context, khoa).let { it > 0L && it >= lucSai }

    fun bo(context: Context, khoa: String, now: Long = System.currentTimeMillis()) {
        runCatching { Prefs.khoRieng(context, KHO).edit().putLong(khoa, now).commit() }
    }

    /** Ket qua mot lenh BOSUA: ma cau da bo, va ma cau khong con cho sua nen khong co gi de bo. */
    data class KetQua(val daBo: List<String>, val khongThay: List<String>)

    /**
     * Bo cac cau Ba Huy chon, moi cau la mot cap (ma, de) lay tu ban cham cua bai.
     *
     * Ghep voi cau dang cho sua dung cach cua [SuaCham.chuanBi]: theo ma, va hai cau trung
     * ma (hai quyen sach) thi so them de bai. Ghep voi [SoCaiBai.dangChoSua] chu khong voi
     * [SoCaiBai.canSua]: cau da roi danh sach vi qua han ma Ba Huy van bo thi cung khong sao,
     * va no se khong hien lai khi han doi.
     */
    fun boTheoMa(
        context: Context,
        danhSach: List<Pair<String, String>>,
        now: Long = System.currentTimeMillis()
    ): KetQua {
        val cho = SoCaiBai.dangChoSua(context, now)
        val daBo = mutableListOf<String>()
        val khongThay = mutableListOf<String>()
        val daChon = mutableSetOf<String>()
        danhSach.forEach { (ma, de) ->
            val cungMa = cho.filter { it.ma.trim() == ma.trim() }
            val so = if (cungMa.size <= 1) cungMa.firstOrNull()
            else cungMa.firstOrNull { SoCaiBai.chuanHoa(it.de) == SoCaiBai.chuanHoa(de) }
            if (so == null || !daChon.add(so.khoa)) {
                khongThay += ma
            } else {
                bo(context, so.khoa, now)
                daBo += so.ma
            }
        }
        return KetQua(daBo, khongThay)
    }

    /** Chi test goi: xoa het, de moi test bat dau tu kho trong. */
    internal fun xoaHet(context: Context) {
        runCatching { Prefs.khoRieng(context, KHO).edit().clear().commit() }
    }
}
