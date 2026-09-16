package vn.huytl.homeworkgate.ai

import org.json.JSONObject

/**
 * Doc cau tra loi cua lan goi "nhin lai anh" - xem [PromptCham.CAU_LENH_DOC_LAI].
 *
 * Tach rieng khoi [ChamBaiJson] vi hai lan goi tra ve hai thu khac nhau: ben kia la
 * ca mot lan nop, ben nay chi la may cau dang tranh chap, kem mot truong ma ben kia
 * khong co - may co cong nhan anh dung nhu con noi khong.
 *
 * Nguyen tac doc y het ben kia: thieu gi thi lay mac dinh AN TOAN. Thieu "dung" coi
 * nhu chua dung; thieu "doc_ro" hay thieu "dung_nhu_hoc_sinh_noi" coi nhu khong -
 * tuc la khong tu duyet, de Ba Huy nhin. Mot cau tra loi thieu truong khong duoc
 * phep bien thanh mot lan cong gio.
 */
object DocLaiJson {

    data class Lai(
        val baiLam: List<String>,
        val dongSai: Int,
        val ketQua: String,
        val dung: Boolean,
        val docRo: Boolean,
        val dungNhuConNoi: Boolean,
        val nhanXet: String
    )

    /** Tra ve theo ma cau da chuan hoa, de ben goi doi chieu khong so lech dau cach. */
    fun doc(chu: String?): Map<String, Lai> {
        if (chu.isNullOrBlank()) return emptyMap()
        val dau = chu.indexOf('{')
        val cuoi = chu.lastIndexOf('}')
        if (dau < 0 || cuoi <= dau) return emptyMap()
        val o = runCatching { JSONObject(chu.substring(dau, cuoi + 1)) }.getOrNull()
            ?: return emptyMap()
        val mang = o.optJSONArray("cac_cau") ?: return emptyMap()

        return (0 until mang.length()).mapNotNull { i ->
            val c = mang.optJSONObject(i) ?: return@mapNotNull null
            val ma = c.optString("ma").ifBlank { return@mapNotNull null }
            val baiLam = c.optJSONArray("bai_lam")
                ?.let { a -> (0 until a.length()).map { a.optString(it) } }
                .orEmpty()
            ChamBaiJson.chuanHoaMa(ma) to Lai(
                baiLam = baiLam,
                dongSai = c.optInt("dong_sai", 0).coerceIn(0, baiLam.size),
                ketQua = c.optString("ket_qua"),
                dung = c.optBoolean("dung", false),
                docRo = c.optBoolean("doc_ro", false),
                dungNhuConNoi = c.optBoolean("dung_nhu_hoc_sinh_noi", false),
                nhanXet = c.optString("nhan_xet")
            )
        }.toMap()
    }
}
