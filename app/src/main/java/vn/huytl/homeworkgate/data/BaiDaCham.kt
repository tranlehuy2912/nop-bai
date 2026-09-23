package vn.huytl.homeworkgate.data

import com.google.firebase.firestore.DocumentSnapshot
import vn.huytl.homeworkgate.dongbo.Duong

/**
 * Mot lan nop bai kem ban cham tung cau, doc lai tu Firestore cho man ket qua cua con.
 *
 * VI SAO DOC TU FIRESTORE CHU KHONG TU KHO TRONG MAY. So cai trong may chi ghi cau
 * dung khi gio da vao tay con, xem [SoCaiBai.ghi]. Lan nao may chua cap duoc gio thi
 * so chi co cau sai, va man ket qua se mat het cac cau dung. Ban cham tren Firestore
 * thi du tung cau, va cung chinh la ban app Bang dieu khien dang hien: con va Ba Huy
 * nhin cung mot thu.
 *
 * Firestore giu san mot ban sao trong may, nen mat mang con van xem duoc nhung bai
 * tablet tu ghi len.
 */
data class BaiDaCham(
    val id: String,
    val luc: Long,
    val mon: String,
    /** null la bai chua co ban cham tung cau: may dang cham, hoac cham khong duoc. */
    val cac: List<Cau>?,
    /** Luc Ba Huy dan ket qua Claude cham lai. 0 la chua cham lai lan nao. */
    val claudeLuc: Long = 0L
) {
    data class Cau(
        val ma: String,
        val de: String,
        val ketQua: String,
        val dung: Boolean,
        val docRo: Boolean,
        val nhanXet: String,
        /** Ket luan cua Claude cho cau nay, null la Claude chua cham lai. */
        val claude: Claude? = null
    ) {
        /**
         * Ket luan cuoi cung: cua Claude neu Claude doc chac, khong thi cua may.
         *
         * Claude khong doc chac thi van nghe theo may. Mot cau "khong chac" ma lat
         * nguoc ket luan cua may thi chi doi mot phong doan lay mot phong doan khac.
         */
        val dungCuoi: Boolean
            get() = if (claude != null && claude.chac) claude.dung else docRo && dung

        /** Chua noi duoc dung hay sai: may doc khong ro, va Claude cung chua doc chac. */
        val chuaRo: Boolean
            get() = !docRo && (claude == null || !claude.chac)
    }

    data class Claude(
        val dung: Boolean,
        val chac: Boolean,
        val conViet: String,
        val goiY: String
    )

    companion object {
        /**
         * Doc mot document trong nha/{id}/bai.
         *
         * Ten truong trong ban cham phai y het cho ApprovalService goi
         * [vn.huytl.homeworkgate.dongbo.DongBo.dayChamBai], va trong ban Claude phai y
         * het cho app Bang dieu khien ghi [Duong.F_CHAM_CLAUDE]. Cac ten do khong nam
         * trong [Duong] vi moi ban chi mot app ghi.
         */
        fun doc(d: DocumentSnapshot): BaiDaCham {
            val cham = d.get(Duong.F_CHAM) as? Map<*, *>
            val claudeMap = d.get(Duong.F_CHAM_CLAUDE) as? Map<*, *>
            val claude = (claudeMap?.get("cac") as? List<*>).orEmpty().mapNotNull { c ->
                val o = c as? Map<*, *> ?: return@mapNotNull null
                val ma = (o["ma"] as? String)?.trim().orEmpty()
                if (ma.isEmpty()) return@mapNotNull null
                ma to Claude(
                    dung = o["dung"] as? Boolean ?: false,
                    chac = o["chac"] as? Boolean ?: true,
                    conViet = o["conViet"] as? String ?: "",
                    goiY = o["goiY"] as? String ?: ""
                )
            }.toMap()

            val cacMay = (cham?.get("cac") as? List<*>)?.mapNotNull { c ->
                val o = c as? Map<*, *> ?: return@mapNotNull null
                val ma = o["ma"] as? String ?: ""
                Cau(
                    ma = ma,
                    de = o["de"] as? String ?: "",
                    ketQua = o["ketQua"] as? String ?: "",
                    dung = o["dung"] as? Boolean ?: false,
                    docRo = o["docRo"] as? Boolean ?: true,
                    nhanXet = o["nhanXet"] as? String ?: "",
                    claude = claude[ma.trim()]
                )
            }?.takeIf { it.isNotEmpty() }

            // May khong cham duoc ma Claude co cham: hien theo Claude, khong co de.
            val cac = cacMay ?: claude.takeIf { it.isNotEmpty() }?.map { (ma, cl) ->
                Cau(
                    ma = ma, de = "", ketQua = cl.conViet, dung = cl.dung,
                    docRo = cl.chac, nhanXet = cl.goiY, claude = cl
                )
            }

            return BaiDaCham(
                id = d.id,
                luc = d.getLong(Duong.F_LUC) ?: 0L,
                mon = cham?.get("mon") as? String ?: "",
                cac = cac,
                claudeLuc = (claudeMap?.get("luc") as? Number)?.toLong() ?: 0L
            )
        }
    }
}
