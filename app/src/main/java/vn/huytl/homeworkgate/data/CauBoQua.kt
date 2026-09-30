package vn.huytl.homeworkgate.data

import android.content.Context

/**
 * Cau Le Hoa bam "Câu tiếp" khi chua lam xong, o man Luyen tap ([vn.huytl.homeworkgate.ui.LamBaiActivity]).
 *
 * VI SAO CO (Ba Huy chot 30/9/2026). Truoc day man lam bai chi co nut Kiem tra: gap cau khong
 * biet lam thi con dung o do hoai, thoat ra vao lai thi may lai dua dung cau do len dau. Nay
 * con bam Câu tiếp la sang cau khac, va cau bo qua coi nhu chua lam: khong ghi so cai, khong
 * cap phut, van nam trong phan chua lam.
 *
 * LUI RA SAU TRONG MOT NGAY. Cau bo qua trong [LUI] vua qua thi luot sau xep ra sau moi cau
 * khac ([sapSau]), de con vao lai khong gap ngay cau do. Qua mot ngay thi ve cho cu.
 *
 * SAO DA MAT VAN MAT. Cau dang lam do ma bo qua thi luot do ([LuotDangLam]) giu nguyen, nhu
 * khi thoat man giua chung: bo qua khong thanh cach xoa lan sai.
 *
 * Nam trong kho prefs rieng nhu [LuotDangLam], nen khong keo theo luot ghi Firestore.
 */
object CauBoQua {

    private const val KHO = "cau_bo_qua"

    /** Cau bo qua lui ra sau trong bay lau. */
    const val LUI = 24L * 60 * 60_000L

    fun ghi(context: Context, cauId: String, luc: Long = System.currentTimeMillis()) {
        runCatching { Prefs.khoRieng(context, KHO).edit().putLong(cauId, luc).commit() }
    }

    /** Cau vua lam xong thi thoi lui. */
    fun xoa(context: Context, cauId: String) {
        runCatching { Prefs.khoRieng(context, KHO).edit().remove(cauId).commit() }
    }

    /** Cho bo test. */
    fun xoaHet(context: Context) {
        runCatching { Prefs.khoRieng(context, KHO).edit().clear().commit() }
    }

    /** Cau bo qua trong [LUI] tinh toi [bayGio]. Don luon dong da qua han, kho khong phinh ra. */
    fun vuaBoQua(context: Context, bayGio: Long = System.currentTimeMillis()): Set<String> {
        val kho = runCatching { Prefs.khoRieng(context, KHO) }.getOrNull() ?: return emptySet()
        val tatCa = runCatching { kho.all }.getOrDefault(emptyMap())
        val ra = mutableSetOf<String>()
        val cu = mutableListOf<String>()
        tatCa.forEach { (id, v) ->
            val luc = v as? Long
            if (luc != null && bayGio - luc < LUI) ra += id else cu += id
        }
        if (cu.isNotEmpty()) runCatching { kho.edit().apply { cu.forEach { remove(it) } }.apply() }
        return ra
    }

    /** Giu thu tu, dua cac cau trong [boQua] ra sau cung. */
    fun <T> sapSau(cac: List<T>, boQua: Set<String>, id: (T) -> String): List<T> {
        if (boQua.isEmpty()) return cac
        val (sau, truoc) = cac.partition { id(it) in boQua }
        return truoc + sau
    }
}
