package vn.huytl.homeworkgate.kho

import org.json.JSONArray
import org.json.JSONObject

/**
 * Mot cau hoi trong ngan hang, nap tu sach chu khong phai tu anh chup.
 *
 * [id] la thu quan trong nhat trong ca file nay: no la danh tinh co dinh cua mot
 * cau, dang "nguon:ma" - vi du "toan8t1:2.26a". Sach in ra sao thi ma the, nam nay
 * hay sang nam cung the. So cai bam vao chuoi nay de biet cau da tra gio chua, nen
 * no PHAI khong doi giua hai lan nop. Doi cach dat [id] la ca so cai cu tro thanh
 * vo nghia - luc do phai xoa so va lam lai tu dau.
 */
data class CauHoi(
    val id: String,
    val mon: String,
    val nguon: String,
    val chuong: String,
    val bai: String,
    /** "Bài tập", "Luyện tập 1", "Vận dụng"... de con nhan ra cho can tim. */
    val nhom: String,
    val ma: String,
    val de: String,
    val trang: Int,
    val dang: String,
    val thuTu: Int
) {
    /** Dong hien cho con chon: "2.26a — Phân tích đa thức..." */
    fun dongChon(): String = "$ma  ·  ${de.take(70)}${if (de.length > 70) "…" else ""}"
}

/** Mot bai trong sach, gom nhieu cau. Dung de con chon truoc khi chup. */
data class TenBai(
    val bai: String,
    val chuong: String,
    val trang: Int,
    val soCau: Int
)

/**
 * Mot trang sach co bai tap, de con chon theo trang.
 *
 * @param bai ten bai dau tien co cau o trang nay - chi de con nhin cho do lac, mot
 *   trang co the vat sang bai ke tiep.
 */
data class TrangSach(
    val trang: Int,
    val soCau: Int,
    val bai: String
)

/** Mot lan cham mot cau, da ghi xuong kho. */
data class TraLoi(
    val cauId: String,
    val mon: String,
    val ma: String,
    val de: String,
    val ketQua: String,
    /** Tung dong con viet, chep tu anh ra. Xem [vn.huytl.homeworkgate.data.CauCham.baiLam]. */
    val baiLam: List<String> = emptyList(),
    /** Dong dau tien sai trong [baiLam], dem tu 1; 0 la dung het hoac khong ro. */
    val dongSai: Int = 0,
    val dung: Boolean,
    val phut: Int,
    val nhanXet: String,
    val luc: Long
)

/**
 * Pham vi con khai truoc khi chup: dang lam bai nao, nhung cau nao.
 *
 * DAY LA CHO SUA THAT SU CUA CA BAN NAY. Truoc day con bam "Nop bai" roi chup, va
 * may phai tu doan tam anh nay la bai gi - doan bang chinh cau lenh gui cho AI. Bay
 * gio con noi truoc, may chi viec cham. Mot dua tre khai "bai 2.26" roi chup bai
 * 2.27 thi AI van thay ngay khi doi chieu de bai trong danh sach voi de bai trong
 * anh; con khai dung ma chup lai bai hom qua thi ma cau da nam trong so cai.
 *
 * [nguon] rong nghia la bai khong co trong ngan hang (vo bai tap, phieu photo, mon
 * chua nap sach). Luc do chay duong cu: AI tu tach cau, so cai lay de bai da chuan
 * hoa lam khoa. Kem chac hon, nhung van la duong duy nhat cho nhung mon chua co
 * sach trong may.
 */
data class PhamVi(
    val mon: String,
    val nguon: String = "",
    val tenNguon: String = "",
    val bai: String = "",
    val cauIds: List<String> = emptyList()
) {
    val theoSach: Boolean get() = nguon.isNotBlank() && cauIds.isNotEmpty()

    fun sangJson(): String = JSONObject()
        .put("mon", mon)
        .put("nguon", nguon)
        .put("ten_nguon", tenNguon)
        .put("bai", bai)
        .put("cau_ids", JSONArray(cauIds))
        .toString()

    companion object {
        fun tuJson(chu: String?): PhamVi? {
            if (chu.isNullOrBlank()) return null
            val o = runCatching { JSONObject(chu) }.getOrNull() ?: return null
            val a = o.optJSONArray("cau_ids")
            return PhamVi(
                mon = o.optString("mon"),
                nguon = o.optString("nguon"),
                tenNguon = o.optString("ten_nguon"),
                bai = o.optString("bai"),
                cauIds = (0 until (a?.length() ?: 0)).mapNotNull { a?.optString(it) }
                    .filter { it.isNotBlank() }
            )
        }
    }
}
