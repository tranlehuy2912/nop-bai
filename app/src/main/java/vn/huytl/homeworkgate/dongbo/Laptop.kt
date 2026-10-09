package vn.huytl.homeworkgate.dongbo

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs

/**
 * Doi phut choi tablet sang phut xem Netflix tren laptop (anh Huy chot 7/10/2026).
 *
 * Laptop chay Linux Mint, tai khoan cua Le Hoa chi mo duoc Firefox vao Netflix, va
 * Timekpr-nExt dem gio cua tai khoan do: moi ngay 0 phut, chi co phut khi duoc cap. Con
 * bam nut "Xem phim" tren man chinh (truoc 9/10/2026 la nut "Netflix" mo mot chuoi hop
 * thoai), chon so phut choi o man [vn.huytl.homeworkgate.ui.XemPhimActivity]; tablet lay
 * so phut do ra khoi phieu hom nay ([GateStore.doiPhut]), nhan voi [Prefs.tiLeNetflix], roi
 * ghi mot phieu vao laptop/{maNha}/cap. Chuong trinh chay ngam tren laptop nghe cho do (tu
 * 9/10/2026; truoc do hoi moi phut), cong gio qua Timekpr-nExt roi xoa phieu. Ba Huy cung
 * cap duoc tu Bang dieu khien, cung cho do.
 *
 * Tu 9/10/2026 laptop xem duoc ca Netflix lan YouTube, chung mot quy phut, nen chu tren
 * tablet la "phut xem phim". Ten trong code (tiLeNetflix, Duong.LAPTOP...) giu nguyen vi da
 * nam trong Firestore va Bang dieu khien.
 *
 * Phut Netflix chi dung trong ngay: Timekpr-nExt tinh gio theo ngay, nua dem la ve 0, va
 * laptop bo phieu tao tu hom truoc. Giong phieu gio choi tablet, nen con khong doi tich
 * tru duoc. Cau hoi doi phut noi ro dieu nay.
 *
 * Doi roi khong doi lai duoc: phut da sang laptop thi tablet khong biet con xem toi dau.
 */
object Laptop {

    private const val TAG = "Laptop"
    private const val FILE = "laptop"
    private const val K_CO = "co"
    private const val K_KET_THUC = "ket_thuc_luc"
    private const val K_CON_LAI = "con_lai_ms"

    /**
     * Cac nut so phut o man Xem phim, them hai nut "Đổi hết" va "Khác" (go so). Anh Huy chon
     * 9/10/2026; truoc do la 10, 15, 30 trong hop thoai. Nut nao cung bam duoc, ke ca khi
     * lon hon so phut con dang co: luc do man bao con chi co bao nhieu (anh Huy chot "cho mo
     * nut", thay vi an nhu hop cu).
     */
    val CAC_NUT = listOf(15, 30, 45, 60)

    /**
     * Laptop da noi vao nha chua, theo ban document laptop/{maNha} vua nghe duoc.
     *
     * Chua noi thi khong hien nut: phieu ghi vao do khong ai nhan, con mat phut ma khong
     * duoc gi.
     */
    fun daNoi(context: Context): Boolean = sp(context).getBoolean(K_CO, false)

    /** Laptop con bao nhieu ms Netflix luc [bayGio], theo ban vua nghe duoc. */
    fun conLaiMs(context: Context, bayGio: Long = System.currentTimeMillis()): Long {
        val s = sp(context)
        val ketThuc = s.getLong(K_KET_THUC, 0L)
        return if (ketThuc > 0L) (ketThuc - bayGio).coerceAtLeast(0L) else s.getLong(K_CON_LAI, 0L)
    }

    /** Goi tu listener trong [DongBo]. Chi ghi file rieng, khong dung toi prefs chinh. */
    internal fun nhan(context: Context, snap: DocumentSnapshot?) {
        val co = snap != null && snap.exists() && !snap.getString(Duong.F_UID_LAPTOP).isNullOrEmpty()
        sp(context).edit()
            .putBoolean(K_CO, co)
            .putLong(K_KET_THUC, snap?.getLong(Duong.F_KET_THUC_LUC) ?: 0L)
            .putLong(K_CON_LAI, snap?.getLong(Duong.F_CON_LAI_MS) ?: 0L)
            .apply()
    }

    /**
     * Ket qua mot lan doi. [loi] khac null la khong doi gi. [phieu] la phieu vua ghi vao
     * laptop/{maNha}/cap: laptop cong gio xong thi xoa no, nen man Xem phim nghe no de biet
     * laptop da nhan chua.
     */
    data class KetQua(
        val phutChoi: Int,
        val phutNetflix: Int,
        val loi: String?,
        val phieu: DocumentReference? = null
    )

    /**
     * Doi [phut] phut choi (het la doi het). Lay phut ra khoi phieu TRUOC roi moi ghi phieu
     * cap: Firestore giu lan ghi trong may khi mat mang va day len khi co mang lai, nen
     * nguoc lai moi la cho de mat: ghi phieu xong ma tien trinh chet truoc khi tru phut la
     * con co phut Netflix ma phieu choi van nguyen.
     */
    fun doi(context: Context, phut: Int, het: Boolean = false): KetQua {
        if (!daNoi(context)) return KetQua(0, 0, "Laptop chưa nối với tablet.")
        val noi = DongBo.laptop(context) ?: return KetQua(0, 0, "Tablet chưa nối mạng của nhà.")
        val gate = GateStore(context)
        val lay = gate.doiPhut(phut, het)
        if (lay <= 0) return KetQua(0, 0, "Không còn phút chơi nào để đổi.")
        val tiLe = Prefs.get(context).tiLeNetflix
        val netflix = lay * tiLe
        val phieu = noi.collection(Duong.CAP).document()
        phieu.set(
            mapOf(
                Duong.F_PHUT to netflix,
                Duong.F_PHUT_CHOI to lay,
                Duong.F_AI to Nguoi.LE_HOA,
                Duong.F_TAO_LUC to System.currentTimeMillis()
            )
        ).addOnFailureListener { Log.w(TAG, "ghi phieu laptop hong: ${it.message}") }
        DayLog.add(context, "Đổi $lay phút chơi lấy $netflix phút Netflix trên laptop")
        DongBo.dayNgay()
        return KetQua(lay, netflix, null, phieu)
    }

    private fun sp(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
