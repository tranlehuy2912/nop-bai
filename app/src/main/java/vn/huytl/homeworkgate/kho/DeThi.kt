package vn.huytl.homeworkgate.kho

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * Bo de thi in san: moi "bai" trong file la mot de nguyen ven, giu dung thu tu va ten cac
 * phan nhu de in. Cau hoi nam trong kho nhu moi quyen sach ([NganHang] nap); file nay chi
 * doc them khung cua tung de, thu kho khong giu: de nao, cau nao so may, thuoc phan nao,
 * de toi Unit nao, lam trong bao lau.
 *
 * VI SAO CO (30/9/2026). Ba Huy muon Le Hoa giai de giua ki, cuoi ki tren may cho quen dang
 * de truoc gio kiem tra that. Giai de cu tu nhat cau sach bai tap roi don trac nghiem len
 * truoc; de thi thi phai di dung nhu to de: phan A roi phan B, cau 5 la cau 5.
 *
 * CAU TRUNG GIUA HAI DE. Cung mot cau in o hai de (cau 1 den 4 cua de giua ki 1 so 1 va so
 * 2 giong het nhau) thi de sau ghi "trung" tro ve cau cua de truoc, va hai de dung chung
 * mot id. So cai tra gio theo id, nen cau do chi sinh gio mot lan, lam lai o de sau chi
 * cong phan hon lan tot nhat nhu moi cau lam lai. Dinh dang o tools/ghep/DINH_DANG.md.
 *
 * Doc tu assets mot lan moi tien trinh roi giu trong bo nho: file khong doi trong luc app
 * chay, va man Giai de hoi khung de moi lan ve.
 */
object DeThi {

    /** Mot cau in trong de, theo dung thu tu in. */
    data class Muc(
        /** So cau in tren de: cau 36 la 36 ke ca khi phan nghe truoc do bi bo. */
        val so: Int,
        /** Id cau trong kho. Cau "trung" thi la id cua cau goc o de khac. */
        val cauId: String,
        /** Phan lon in tren de: "A. LANGUAGE FOCUS". */
        val phan: String,
        /** Loi dan cua bai trong phan: "Exercise 1. Circle the word whose ...". */
        val nhom: String,
        /** Ly do khong lam tren may ("nghe"), rong la lam duoc. */
        val boMay: String
    )

    data class De(
        /** Ma ngan, di vao khoa cua [DeGiai] va lenh cua Bang dieu khien: "GK1-1". */
        val ma: String,
        val nguon: String,
        val mon: String,
        /** "Đề giữa kì 1 số 1". */
        val ten: String,
        /** Unit cuoi cung ma cau trong de cham toi, tinh tren cac cau lam tren may. */
        val denUnit: Int,
        /** Gio lam bai, phut. */
        val phut: Int,
        val cacMuc: List<Muc>
    ) {
        /** Cac cau lam tren may, theo thu tu in, moi id mot lan. */
        val cauIds: List<String> get() = cacMuc.filter { it.boMay.isBlank() }.map { it.cauId }.distinct()
    }

    @Volatile
    private var nho: List<De>? = null

    /** Moi de cua moi bo de thi, theo thu tu trong file. */
    fun tatCa(context: Context): List<De> =
        nho ?: synchronized(this) { nho ?: doc(context).also { nho = it } }

    fun theoMa(context: Context, ma: String): De? = tatCa(context).firstOrNull { it.ma == ma }

    /** "GK1-1.36" ra 36. Ma khong ket bang so thi 0. */
    fun soIn(ma: String): Int = ma.substringAfterLast('.').takeWhile { it.isDigit() }.toIntOrNull() ?: 0

    private fun doc(context: Context): List<De> = NganHang.boDeThi().flatMap { sach ->
        runCatching { docQuyen(context, sach) }.getOrElse { e ->
            Log.w(TAG, "khong doc duoc ${sach.file}: ${e.message}")
            emptyList()
        }
    }

    private fun docQuyen(context: Context, sach: NganHang.Sach): List<De> {
        val o = JSONObject(context.assets.open(sach.file).bufferedReader().use { it.readText() })
        val cacBai = o.optJSONArray("cac_bai") ?: return emptyList()
        return (0 until cacBai.length()).mapNotNull { i ->
            val b = cacBai.optJSONObject(i) ?: return@mapNotNull null
            val meta = b.optJSONObject("de_thi") ?: return@mapNotNull null
            val ma = meta.optString("ma").trim()
            val cacCau = b.optJSONArray("cac_cau")
            if (ma.isEmpty() || cacCau == null) return@mapNotNull null
            val cacMuc = (0 until cacCau.length()).mapNotNull { j ->
                val c = cacCau.optJSONObject(j) ?: return@mapNotNull null
                val maCau = c.optString("ma").trim()
                val goc = c.optString("trung").trim().ifEmpty { maCau }
                if (goc.isEmpty()) return@mapNotNull null
                Muc(
                    so = soIn(maCau),
                    cauId = "${sach.nguon}:$goc",
                    phan = c.optString("phan").trim(),
                    nhom = c.optString("nhom").trim(),
                    boMay = c.optString("bo_may").trim()
                )
            }
            De(
                ma = ma,
                nguon = sach.nguon,
                mon = sach.mon,
                ten = b.optString("bai").trim(),
                denUnit = meta.optInt("den_unit", DEN_UNIT_CA_NAM),
                phut = meta.optInt("phut", PHUT_MAC_DINH),
                cacMuc = cacMuc
            )
        }
    }

    /** De khong ghi pham vi thi coi nhu thi ca nam: chi mo khi lop hoc het sach. */
    private const val DEN_UNIT_CA_NAM = 12

    private const val PHUT_MAC_DINH = 45

    private const val TAG = "DeThi"
}
