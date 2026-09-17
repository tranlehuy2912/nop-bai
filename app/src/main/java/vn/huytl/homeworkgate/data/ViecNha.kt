package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import vn.huytl.homeworkgate.dongbo.Duong

/**
 * Viec nha ba noi giao, va cai khoa di kem.
 *
 * CACH CHAY: ba bam mot hay nhieu viec tren dien thoai cua ba. Tablet khoa toan bo
 * man hinh cho den khi ba bam "Da xong" cho tung viec. Xong het thi may mo ra va
 * cong so phut cua cac viec do vao gio choi.
 *
 * VI SAO KHOA CA MAY chu khong chi khoa game: khoa game thi dua tre van ngoi xem
 * YouTube trong danh sach trang, va cai viec nha kia nam do mai. Man chan thi noi
 * thang ra con gi chua lam, va cham vao la mo dung app Nop bai de doc lai - khong
 * co duong nao khac.
 *
 * AI XAC NHAN: ba, va chi ba. Con khong tu bao xong duoc, khong thi ca co che nay
 * chi la mot cai nut "toi xong roi" tu bam.
 *
 * KHONG CO TRAN NGAY: gio doi bang viec nha khong tru vao han muc bai tap, va Ba
 * Huy chon khong dat tran. Viec nha thi ba noi giao theo viec that trong nha, khong
 * phai thu dua tre tu sinh ra duoc.
 *
 * TRANG THAI NAM TRONG MOT DOCUMENT tren Firestore (hop/viecnha), va do la trang
 * thai DAY DU chu khong phai su kien roi le. Ba bam ba lan lien ma tablet chi doc
 * duoc lan cuoi thi lan cuoi do van dung: no cho biet ca danh sach va viec nao da
 * xong.
 */
object ViecNha {

    /** Mot viec trong phien. */
    data class Viec(val ten: String, val phut: Int, val xong: Boolean)

    /**
     * Mot lan ba giao viec.
     *
     * @param id ma ngan cua phien, do may ba dat. Doi id nghia la phien moi.
     */
    data class Phien(val id: String, val cac: List<Viec>, val nhanLuc: Long) {
        val chuaXong: List<Viec> get() = cac.filter { !it.xong }
        val xongHet: Boolean get() = cac.isNotEmpty() && chuaXong.isEmpty()
        val tongPhut: Int get() = cac.sumOf { it.phut }
    }

    /** Chuyen gi da xay ra sau khi ap ban moi tu hop thu vao. */
    enum class Doi {
        /** Ba vua giao viec, tablet bat dau khoa. */
        MOI,

        /** Xong mot phan, van con viec chua lam. */
        BOT_MOT_VIEC,

        /** Xong het: mo khoa va cong gio. */
        XONG_HET,

        /** Ba bo het viec, khong cong gi ca. */
        BO_HET,

        /** Ban doc duoc giong het cai dang giu, khong co gi de lam. */
        KHONG_DOI
    }

    /** Phien dang treo, null la khong co viec gi. */
    fun dangTreo(context: Context): Phien? {
        val chu = sp(context).getString(K_PHIEN, null) ?: return null
        return doc(chu)
    }

    /** Dang co viec chua lam xong khong. Man chan hoi cau nay moi nhip. */
    fun dangKhoa(context: Context): Boolean = dangTreo(context)?.chuaXong?.isNotEmpty() == true

    /**
     * Ap ban vua doc duoc tu Firestore.
     *
     * Tra ve cai da doi, de ben goi biet phai lam gi: mo khoa, cong gio, hay im
     * lang. Ham nay khong tu cong gio va khong tu mo khoa - no chi giu so.
     */
    fun apDung(context: Context, moi: Phien): Doi {
        val cu = dangTreo(context)

        // Phien khac han: ba giao dot moi. Dot cu con do dang thi bo, vi chinh ba
        // la nguoi vua quyet dinh nhu vay.
        if (cu == null || cu.id != moi.id) {
            if (moi.cac.isEmpty()) return Doi.KHONG_DOI
            luu(context, moi)
            return if (moi.xongHet) {
                xoa(context)
                Doi.XONG_HET
            } else {
                Doi.MOI
            }
        }

        if (moi.cac.isEmpty()) {
            xoa(context)
            return Doi.BO_HET
        }
        if (moi.cac == cu.cac) return Doi.KHONG_DOI

        if (moi.xongHet) {
            // Giu lai de ben goi con doc duoc tong so phut, roi no tu goi [xoa].
            luu(context, moi)
            return Doi.XONG_HET
        }
        luu(context, moi)
        return Doi.BOT_MOT_VIEC
    }

    /**
     * Ma dot viec vua khep lai.
     *
     * Khep xong thi tablet xoa document tren Firestore, nhung lenh xoa do co the
     * khong di duoc - mat mang dung nhip ay. Luc do document nam lai voi ca dot da
     * xong het, va lan doc sau tablet khong con gi de so nen coi no la dot moi vua
     * giao: cong gio them mot lan nua. Nho mot ma o day la du de nhan ra va xoa lai.
     */
    fun daKhep(context: Context): String = sp(context).getString(K_DA_KHEP, "").orEmpty()

    fun ghiDaKhep(context: Context, ma: String) {
        sp(context).edit().putString(K_DA_KHEP, ma).commit()
    }

    fun xoa(context: Context) {
        sp(context).edit().remove(K_PHIEN).commit()
    }

    /** Mot dong ke cac viec chua xong, de hien len man chan va man hinh chinh. */
    fun keChuaXong(context: Context): String =
        dangTreo(context)?.chuaXong.orEmpty().joinToString(", ") { it.ten }

    // ---------------------------------------------------------------- doc ghi

    private fun luu(context: Context, p: Phien) {
        val cac = JSONArray()
        p.cac.forEach {
            cac.put(
                JSONObject().put("ten", it.ten).put("phut", it.phut).put("xong", it.xong)
            )
        }
        sp(context).edit().putString(
            K_PHIEN,
            JSONObject().put("id", p.id).put("luc", p.nhanLuc).put("cac", cac).toString()
        ).commit()
    }

    private fun doc(chu: String): Phien? = runCatching {
        val o = JSONObject(chu)
        val a = o.optJSONArray("cac") ?: JSONArray()
        Phien(
            id = o.optString("id"),
            nhanLuc = o.optLong("luc"),
            cac = (0 until a.length()).map { i ->
                val v = a.getJSONObject(i)
                Viec(v.optString("ten"), v.optInt("phut"), v.optBoolean("xong"))
            }
        )
    }.getOrNull()

    /**
     * Dung mot ban tu document hop/viecnha ben Firestore.
     *
     * Nhan kieu Kotlin tran chu khong nhan DocumentSnapshot: goi nay nam trong lop
     * du lieu, va keo Firebase vao day thi moi bai test don gian nhat cung phai co
     * mot du an Firebase that de chay.
     *
     * Ten viec do may ba gui kem chu khong phai bang ma cung o hai dau: Ba Huy sua
     * danh sach viec trong app cua ba, ma sua xong thi tablet phai goi dung ten moi
     * ngay - khong the doi cai lai ca hai may.
     */
    fun tuBan(maPhien: String, cac: List<Map<*, *>>, nhanLuc: Long): Phien? {
        if (maPhien.isBlank()) return null
        val viec = cac.mapNotNull { o ->
            val ten = (o[Duong.F_TEN] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val phut = ((o[Duong.F_PHUT] as? Number)?.toInt() ?: 0).coerceIn(0, 240)
            Viec(ten, phut, o[Duong.F_XONG] == true)
        }
        return Phien(maPhien, viec, nhanLuc)
    }

    private const val K_PHIEN = "viec_nha_phien"
    private const val K_DA_KHEP = "viec_nha_da_khep"

    private fun sp(context: Context) = Prefs.get(context).raw()
}
