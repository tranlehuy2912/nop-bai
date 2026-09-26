package vn.huytl.homeworkgate.kho

import android.content.Context

/**
 * Cac phan hoc cua tung mon, moi phan mot moc "lop da hoc toi bai nao".
 *
 * VI SAO CO. Moc hoc toi co tu 25/9/2026, nhung chi cho bo the hoc thuoc: moi bo mot
 * moc, va hop chon chi liet ke nhung bai CO THE trong bo. Tu 27/9/2026 cung moc do con
 * cat kho cau sach bai tap: lam them, luyen cho hay vap, Giai de (xem [NganHang] va
 * [vn.huytl.homeworkgate.data.GiaiDe]). Kho SBT thi co du moi bai, con bo the Hoa bo
 * Bai 5 va Bai 7, bo the Li dung o Bai 26, va phan Sinh khong co bo nao. Chon theo danh
 * sach cua bo the thi con khong noi duoc "lop dang o Bai 7", ma SBT Bai 7 cung khong
 * bao gio duoc ra.
 *
 * Nen moc tinh theo PHAN: Toan mot phan (Bai 1-39), KHTN ba phan Hoa, Li, Sinh, vi
 * truong day ba phan song song voi ba giao vien. Hop chon liet ke du cac bai cua phan,
 * lay ten tu SGK. Ma phan trung ma bo the cung phan, nen khoa trong prefs khong doi va
 * lua chon cu cua con giu nguyen - xem [HocToi]. Bo the cat o bai con chon theo SO bai,
 * xem [BoThe.denThuTu].
 */
object PhanHoc {

    data class Phan(
        /** Trung ma bo the cung phan, de dung chung khoa cua [HocToi]. Phan Sinh khong co bo. */
        val ma: String,
        val mon: String,
        val ten: String,
        /** Ten ngan cho dong mo ta gop ca mon: "Hoá", "Lí", "Sinh". */
        val tenNgan: String,
        /** Bai dau va bai cuoi cua phan, theo so in trong SGK. */
        val tu: Int,
        val den: Int,
        /** Quyen SGK de lay ten cac bai cho hop chon. */
        val sgk: List<String>
    )

    /**
     * KHTN 8 Ket noi tri thuc: Chuong I, II la Hoa (Bai 1-12, ke ca bai mo dau ve hoa
     * chat va thiet bi thi nghiem), Chuong III toi VI la Li (Bai 13-29), Chuong VII,
     * VIII la Sinh (Bai 30-47).
     */
    val TAT_CA = listOf(
        Phan("toan8ct", "Toán", "Toán 8", "Toán", 1, 39, listOf("toan8t1", "toan8t2")),
        Phan("khtn8hoa", "Khoa học tự nhiên", "KHTN 8 phần Hoá học", "Hoá", 1, 12, listOf("khtn8")),
        Phan("khtn8li", "Khoa học tự nhiên", "KHTN 8 phần Vật lí", "Lí", 13, 29, listOf("khtn8")),
        Phan("khtn8sinh", "Khoa học tự nhiên", "KHTN 8 phần Sinh học", "Sinh", 30, 47, listOf("khtn8"))
    )

    fun cuaMon(mon: String): List<Phan> = TAT_CA.filter { it.mon == mon }

    fun theoMa(ma: String): Phan? = TAT_CA.firstOrNull { it.ma == ma }

    /** Phan chua bai so [so] cua mon [mon]. */
    fun cuaBai(mon: String, so: Int): Phan? = cuaMon(mon).firstOrNull { so in it.tu..it.den }

    /**
     * "Bài 12. Muối" ra 12. Muc khong phai mot bai ("Ôn tập chương I", "Luyện tập chung
     * (trang 17)") ra null.
     */
    fun soBai(ten: String): Int? = SO_BAI.find(ten)?.groupValues?.get(1)?.toIntOrNull()

    private val SO_BAI = Regex("""^\s*Bài\s+(\d+)\.""")

    /** Ten cac bai cua phan, theo thu tu sach, lay tu SGK trong kho. */
    fun cacBai(context: Context, phan: Phan): List<String> =
        phan.sgk.flatMap { NganHang.cacBai(context, it) }
            .map { it.bai }
            .filter { ten -> soBai(ten)?.let { it in phan.tu..phan.den } == true }
            .distinct()

    /**
     * Lop da hoc toi bai so may trong phan nay. 0 la chua hoc bai nao, null la con chua
     * chon (hay bai con chon khong doc ra so - coi nhu chua chon, hoi lai).
     */
    fun hocToi(context: Context, phan: Phan): Int? {
        val bai = HocToi.baiCua(context, phan.ma) ?: return null
        if (bai == HocToi.CHUA_HOC_BAI_NAO) return 0
        return soBai(bai)
    }

    /**
     * Cac so bai lop da hoc cua mot mon, gop moi phan. null khi con mot phan chua chon:
     * luc do ben goi hoi con truoc, khong doan.
     *
     * @param chiPhanDaChon bo qua phan chua chon thay vi tra null; null chi khi chua phan
     *   nao duoc chon. Cho de Giai de tu mo: con chua chon phan Sinh thi van co de Hoa, Li,
     *   con lam them thi van hoi du ca ba phan.
     */
    fun baiDaHoc(context: Context, mon: String, chiPhanDaChon: Boolean = false): Set<Int>? {
        val cac = cuaMon(mon)
        if (cac.isEmpty()) return null
        val ra = mutableSetOf<Int>()
        var coChon = false
        for (p in cac) {
            val den = hocToi(context, p)
            if (den == null) {
                if (chiPhanDaChon) continue else return null
            }
            coChon = true
            if (den >= p.tu) ra.addAll(p.tu..minOf(den, p.den))
        }
        return if (coChon) ra else null
    }

    /** Cac phan cua mon con chua chon moc, theo thu tu sach. */
    fun chuaChon(context: Context, mon: String): List<Phan> =
        cuaMon(mon).filter { hocToi(context, it) == null }

    /**
     * Mot dong noi moc hien tai cua mon, cho dong nho duoi tieu de: "Lớp đã học tới
     * Bài 5" hay "Hoá tới Bài 9, Lí tới Bài 15, Sinh chưa học".
     */
    fun moTa(context: Context, mon: String): String {
        val cac = cuaMon(mon)
        if (cac.size == 1) {
            val so = hocToi(context, cac.first())
            return when (so) {
                null -> "Chưa chọn lớp đã học tới bài nào"
                0 -> "Lớp chưa học tới bài nào"
                else -> "Lớp đã học tới Bài $so"
            }
        }
        return cac.joinToString(", ") { p ->
            when (val so = hocToi(context, p)) {
                null -> "${p.tenNgan} chưa chọn"
                0 -> "${p.tenNgan} chưa học"
                else -> "${p.tenNgan} tới Bài $so"
            }
        }
    }

    /** Mot dong cho mot phan, de chon phan can doi: "Hoá: đã học tới Bài 9". */
    fun moTaPhan(context: Context, phan: Phan): String =
        "${phan.tenNgan}: " + when (val so = hocToi(context, phan)) {
            null -> "chưa chọn"
            0 -> "chưa học bài nào"
            else -> "đã học tới Bài $so"
        }
}
