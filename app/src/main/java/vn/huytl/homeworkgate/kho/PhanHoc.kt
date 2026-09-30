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
 * Nen moc tinh theo PHAN: KHTN ba phan Hoa, Li, Sinh, vi truong day ba phan song song voi
 * ba giao vien. Hop chon liet ke du cac bai cua phan, lay ten tu SGK. Ma phan KHTN trung ma
 * bo the cung phan, nen khoa trong prefs khong doi va lua chon cu cua con giu nguyen - xem
 * [HocToi]. Bo the cat o bai con chon theo SO bai, xem [BoThe.denThuTu].
 *
 * TOAN HAI PHAN (Ba Huy chot 30/9/2026): Dai so va Hinh hoc, cung day song song. Khac KHTN,
 * bai cua moi phan khong lien nhau trong sach: Dai so la Chuong I, II (Bai 1-9) va VI, VII
 * (Bai 21-29), Hinh hoc la Chuong III, IV (Bai 10-17) va IX, X (Bai 33-39). Hai chuong
 * Thong ke (Bai 18-20) va Xac suat (Bai 30-32) tinh theo Dai so. Nen moi phan la mot danh
 * sach khoang [Phan.cacKhoang], khong phai mot khoang. Truoc ngay do Toan la mot phan ma
 * "toan8ct" (Bai 1-39); lua chon cu do bi bo, con chon lai hai phan moi (Ba Huy chon hoi
 * lai thay vi tu suy tu moc cu). Bo the Toan van mot bo, cat theo ca hai moc, xem
 * [BoThe.mocCua].
 */
object PhanHoc {

    data class Phan(
        /** Trung ma bo the cung phan, de dung chung khoa cua [HocToi]. Phan Sinh khong co bo. */
        val ma: String,
        val mon: String,
        val ten: String,
        /** Ten ngan cho dong mo ta gop ca mon: "Hoá", "Lí", "Sinh". */
        val tenNgan: String,
        /** Cac khoang so bai cua phan, theo so in trong SGK. */
        val cacKhoang: List<IntRange>,
        /** Quyen SGK de lay ten cac bai cho hop chon. */
        val sgk: List<String>,
        /** Bo the hoc thuoc cua phan, null khi phan khong co bo nao (Sinh). */
        val boThe: String? = null
    ) {
        fun chua(so: Int): Boolean = cacKhoang.any { so in it }

        /** Cac so bai cua phan tu bai dau toi [den], theo thu tu sach. */
        fun cacSoToi(den: Int): List<Int> = cacKhoang.flatMap { it }.filter { it <= den }
    }

    /**
     * KHTN 8 Ket noi tri thuc: Chuong I, II la Hoa (Bai 1-12, ke ca bai mo dau ve hoa
     * chat va thiet bi thi nghiem), Chuong III toi VI la Li (Bai 13-29), Chuong VII,
     * VIII la Sinh (Bai 30-47).
     */
    val TAT_CA = listOf(
        Phan(
            "toan8ds", "Toán", "Toán 8 Đại số", "Đại số", listOf(1..9, 18..32),
            listOf("toan8t1", "toan8t2"), boThe = "toan8ct"
        ),
        Phan(
            "toan8hh", "Toán", "Toán 8 Hình học", "Hình học", listOf(10..17, 33..39),
            listOf("toan8t1", "toan8t2"), boThe = "toan8ct"
        ),
        Phan("khtn8hoa", "Khoa học tự nhiên", "KHTN 8 phần Hoá học", "Hoá", listOf(1..12), listOf("khtn8"), "khtn8hoa"),
        Phan("khtn8li", "Khoa học tự nhiên", "KHTN 8 phần Vật lí", "Lí", listOf(13..29), listOf("khtn8"), "khtn8li"),
        Phan("khtn8sinh", "Khoa học tự nhiên", "KHTN 8 phần Sinh học", "Sinh", listOf(30..47), listOf("khtn8"))
    )

    fun cuaMon(mon: String): List<Phan> = TAT_CA.filter { it.mon == mon }

    fun theoMa(ma: String): Phan? = TAT_CA.firstOrNull { it.ma == ma }

    /** Phan chua bai so [so] cua mon [mon]. */
    fun cuaBai(mon: String, so: Int): Phan? = cuaMon(mon).firstOrNull { it.chua(so) }

    /** Cac phan dung bo the [bo]: KHTN mot phan, bo Toan hai phan. */
    fun cuaBoThe(bo: String): List<Phan> = TAT_CA.filter { it.boThe == bo }

    /**
     * "Bài 12. Muối" ra 12. Muc khong phai mot bai ("Ôn tập chương I", "Luyện tập chung
     * (trang 17)") ra null.
     */
    fun soBai(ten: String): Int? {
        SO_BAI.find(ten)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        // Sach Tieng Anh danh so theo Unit. Bai Test Yourself k kiem tra ba Unit truoc no,
        // nen tinh la Unit 3k: lop hoc xong Unit 3 moi co Test Yourself 1.
        SO_UNIT.find(ten)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return SO_TEST.find(ten)?.groupValues?.get(1)?.toIntOrNull()?.let { it * 3 }
    }

    private val SO_BAI = Regex("""^\s*Bài\s+(\d+)\.""")
    private val SO_UNIT = Regex("""^\s*Unit\s+(\d+)\b""")
    private val SO_TEST = Regex("""^\s*Test Yourself\s+(\d+)\b""")

    /** Bo tu vung giu moc "lop da hoc toi Unit nao" cua mon Tieng Anh, xem [HocToi.unitCua]. */
    const val BO_TIENG_ANH = "anh8"
    const val TIENG_ANH = "Tiếng Anh"

    /** Ten cac bai cua phan, theo thu tu sach, lay tu SGK trong kho. */
    fun cacBai(context: Context, phan: Phan): List<String> =
        phan.sgk.flatMap { NganHang.cacBai(context, it) }
            .map { it.bai }
            .filter { ten -> soBai(ten)?.let { phan.chua(it) } == true }
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
        /*
         * Tieng Anh dung chung moc Unit con chon o man Do tu vung (Ba Huy dong y ngay
         * 29/9/2026), khong co phan nao trong [TAT_CA]. Bai trong sach bai tap danh so
         * theo Unit, xem [soBai].
         */
        if (mon == TIENG_ANH) {
            val unit = HocToi.unitCua(context, BO_TIENG_ANH) ?: return null
            return if (unit <= HocToi.CHUA_HOC_UNIT_NAO) emptySet() else (1..unit).toSet()
        }
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
            ra.addAll(p.cacSoToi(den))
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
