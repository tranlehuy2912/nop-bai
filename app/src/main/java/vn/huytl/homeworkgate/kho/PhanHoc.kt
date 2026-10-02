package vn.huytl.homeworkgate.kho

import android.content.Context

/**
 * Cac phan hoc cua tung mon, va cac bai lop da hoc cua moi mon.
 *
 * VI SAO CO PHAN. Truong day KHTN ba phan Hoa, Li, Sinh song song voi ba giao vien, va Toan
 * hai phan Dai so, Hinh hoc cung day song song (Ba Huy chot 30/9/2026). Bai cua hai phan
 * Toan khong lien nhau trong sach: Dai so la Chuong I, II (Bai 1-9) va VI, VII (Bai 21-29),
 * Hinh hoc la Chuong III, IV (Bai 10-17) va IX, X (Bai 33-39). Hai chuong Thong ke (Bai 18-20)
 * va Xac suat (Bai 30-32) tinh theo Dai so. Nen moi phan la mot danh sach khoang
 * [Phan.cacKhoang], khong phai mot khoang. Ma phan KHTN trung ma bo the cung phan
 * ([Phan.boThe]).
 *
 * BAI DA HOC THEO MON, CHON NHIEU BAI (Ba Huy chot 2/10/2026). Tu 25/9 toi 2/10/2026 moi
 * phan mot moc "lop da hoc toi Bai N" (Tieng Anh mot moc "toi Unit N"), va moi bai tu bai
 * dau toi Bai N la da hoc. Lop khong luon hoc dung thu tu sach, va Toan, KHTN thi con phai
 * doi nam moc o nam dong. Nay moi mon mot tap bai con tu danh dau ([baiDaHoc], luu o
 * [HocToi]), mot dong dau moi khu mon o trang Luyen tap. Phan van can: trang Luyen tap tach
 * dong Luyen tap theo phan ([vn.huytl.homeworkgate.data.LamTrenMay.cauLamThem]), va "bai gan
 * moc" de ra cau tinh trong tung phan ([mocCuaPhan]): Bai 9 cua Hoa va Bai 15 cua Li deu la
 * bai lop vua hoc.
 */
object PhanHoc {

    const val TOAN = "Toán"
    const val KHTN = "Khoa học tự nhiên"
    const val TIENG_ANH = "Tiếng Anh"

    /** Bo tu vung cua mon Tieng Anh, xem [BoTuVung]. */
    const val BO_TIENG_ANH = "anh8"

    data class Phan(
        /** Trung ma bo the cung phan. Phan Sinh khong co bo. */
        val ma: String,
        val mon: String,
        val ten: String,
        /** Ten ngan cho dong mo ta gop ca mon: "Hoá", "Lí", "Sinh". */
        val tenNgan: String,
        /** Ten phan sau ten mon o trang Luyen tap: "Luyện tập Toán Đại số", "Luyện tập KHTN Hoá học". */
        val tenDai: String,
        /** Cac khoang so bai cua phan, theo so in trong SGK. */
        val cacKhoang: List<IntRange>,
        /** Quyen SGK de lay ten cac bai cho hop chon. */
        val sgk: List<String>,
        /** Bo the hoc thuoc cua phan, null khi phan khong co bo nao (Sinh). */
        val boThe: String? = null,
        /**
         * So chuong cua phan, cho muc khong mang so bai ("Ôn tập chương III", "Luyện tập chung"):
         * cau o do van phai roi vao mot dong Luyen tap, xem [phanCuaCau].
         */
        val cacChuong: Set<Int> = emptySet()
    ) {
        fun chua(so: Int): Boolean = cacKhoang.any { so in it }

        /** Cac so bai cua phan tu bai dau toi [den], theo thu tu sach. */
        fun cacSoToi(den: Int): List<Int> = cacKhoang.flatMap { it }.filter { it <= den }

        /** Moi so bai cua phan, theo thu tu sach. */
        val cacSo: List<Int> get() = cacKhoang.flatMap { it }
    }

    /**
     * KHTN 8 Ket noi tri thuc: Chuong I, II la Hoa (Bai 1-12, ke ca bai mo dau ve hoa
     * chat va thiet bi thi nghiem), Chuong III toi VI la Li (Bai 13-29), Chuong VII,
     * VIII la Sinh (Bai 30-47).
     */
    val TAT_CA = listOf(
        Phan(
            "toan8ds", TOAN, "Toán 8 Đại số", "Đại số", "Đại số", listOf(1..9, 18..32),
            listOf("toan8t1", "toan8t2"), boThe = "toan8ct", cacChuong = setOf(1, 2, 5, 6, 7, 8)
        ),
        Phan(
            "toan8hh", TOAN, "Toán 8 Hình học", "Hình học", "Hình học", listOf(10..17, 33..39),
            listOf("toan8t1", "toan8t2"), boThe = "toan8ct", cacChuong = setOf(3, 4, 9, 10)
        ),
        Phan(
            "khtn8hoa", KHTN, "KHTN 8 phần Hoá học", "Hoá", "Hoá học", listOf(1..12),
            listOf("khtn8"), "khtn8hoa", cacChuong = setOf(1, 2)
        ),
        Phan(
            "khtn8li", KHTN, "KHTN 8 phần Vật lí", "Lí", "Vật lí", listOf(13..29),
            listOf("khtn8"), "khtn8li", cacChuong = setOf(3, 4, 5, 6)
        ),
        Phan(
            "khtn8sinh", KHTN, "KHTN 8 phần Sinh học", "Sinh", "Sinh học", listOf(30..47),
            listOf("khtn8"), cacChuong = setOf(7, 8)
        )
    )

    fun cuaMon(mon: String): List<Phan> = TAT_CA.filter { it.mon == mon }

    fun theoMa(ma: String): Phan? = TAT_CA.firstOrNull { it.ma == ma }

    /** Phan chua bai so [so] cua mon [mon]. */
    fun cuaBai(mon: String, so: Int): Phan? = cuaMon(mon).firstOrNull { it.chua(so) }

    /** Cac phan dung bo the [bo]: KHTN mot phan, bo Toan hai phan. */
    fun cuaBoThe(bo: String): List<Phan> = TAT_CA.filter { it.boThe == bo }

    /** Ten mon cho dong chu ngan: "Toán", "KHTN", "Tiếng Anh". */
    fun tenNgan(mon: String): String = if (mon == KHTN) "KHTN" else mon

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

    /** "Chương III. Tứ giác" ra 3. Khong co chu Chuong thi null. */
    fun soChuong(ten: String): Int? {
        val t = SO_CHUONG.find(ten)?.groupValues?.get(1) ?: return null
        t.toIntOrNull()?.let { return it }
        var tong = 0
        var truoc = 0
        for (c in t.reversed()) {
            val gia = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; else -> return null }
            if (gia < truoc) tong -= gia else { tong += gia; truoc = gia }
        }
        return tong.takeIf { it > 0 }
    }

    private val SO_CHUONG = Regex("""Chương\s+([IVX]+|\d+)""")

    /**
     * Phan cua mot cau sach bai tap: theo so bai, khong co so bai ("Ôn tập chương III") thi
     * theo so chuong. null khi mon khong chia phan (Tieng Anh) hay khong doc ra (muc "Bài tập
     * ôn tập cuối năm" khong thuoc chuong nao).
     */
    fun phanCuaCau(cau: CauHoi): Phan? {
        soBai(cau.bai)?.let { return cuaBai(cau.mon, it) }
        val ch = soChuong(cau.chuong) ?: return null
        return cuaMon(cau.mon).firstOrNull { ch in it.cacChuong }
    }

    /** Ten cac bai cua phan, theo thu tu sach, lay tu SGK trong kho. */
    fun cacBai(context: Context, phan: Phan): List<String> =
        phan.sgk.flatMap { NganHang.cacBai(context, it) }
            .map { it.bai }
            .filter { ten -> soBai(ten)?.let { phan.chua(it) } == true }
            .distinct()

    /**
     * Cac bai (Tieng Anh: so Unit) lop da hoc cua mot mon, theo dung cac o con danh dau. null
     * khi con chua chon lan nao: luc do ben goi hoi con truoc, khong doan. Tap rong la con noi
     * chua hoc bai nao.
     */
    fun baiDaHoc(context: Context, mon: String): Set<Int>? = HocToi.daHoc(context, mon)

    /** Cac bai da hoc cua mot phan. null khi mon chua chon. */
    fun baiDaHocCuaPhan(context: Context, phan: Phan): Set<Int>? =
        baiDaHoc(context, phan.mon)?.filter { phan.chua(it) }?.toSet()

    /**
     * Bai cao nhat da danh dau cua phan: "moc" de xep bai gan lop dang hoc len truoc. null khi
     * mon chua chon hay phan chua co bai nao duoc danh dau.
     */
    fun mocCuaPhan(context: Context, phan: Phan): Int? = baiDaHocCuaPhan(context, phan)?.maxOrNull()

    /** Mon nay con chua chon bai da hoc lan nao. */
    fun chuaChon(context: Context, mon: String): Boolean = baiDaHoc(context, mon) == null

    /**
     * Mot dong noi bai da hoc cua mon, cho dong nho duoi tieu de: "Đã học Bài 1–9, 12",
     * "Đã học Unit 1–3", "Chưa học bài nào", "Chưa chọn bài đã học".
     */
    fun moTa(context: Context, mon: String): String {
        val cac = baiDaHoc(context, mon) ?: return "Chưa chọn bài đã học"
        return if (cac.isEmpty()) HocToi.moTa(mon, cac).replaceFirstChar { it.uppercase() }
        else "Đã học " + HocToi.moTa(mon, cac)
    }

    /** "1–4, 6, 10–12": cac so lien nhau gop thanh khoang. */
    fun gon(cac: Collection<Int>): String {
        val s = cac.distinct().sorted()
        val ra = mutableListOf<String>()
        var i = 0
        while (i < s.size) {
            var j = i
            while (j + 1 < s.size && s[j + 1] == s[j] + 1) j++
            ra += if (i == j) "${s[i]}" else "${s[i]}–${s[j]}"
            i = j + 1
        }
        return ra.joinToString(", ")
    }

    /** Mot nhom trong hop chon bai: mot chuong SGK (Tieng Anh: ca bo Unit) va cac bai cua no. */
    data class Nhom(val ten: String, val cacBai: List<Pair<Int, String>>)

    /**
     * Cac bai de con danh dau, chia theo chuong, theo thu tu sach. Toan, KHTN lay ten bai trong
     * SGK; Tieng Anh moi Unit cua bo tu vung mot o, kem ten Unit neu sach bai tap co.
     */
    fun cacNhom(context: Context, mon: String): List<Nhom> {
        if (mon == TIENG_ANH) {
            val tenUnit = NganHang.sachBaiTapCua(TIENG_ANH).flatMap { NganHang.cacBai(context, it.nguon) }
                .mapNotNull { b -> SO_UNIT.find(b.bai)?.groupValues?.get(1)?.toIntOrNull()?.let { it to b.bai } }
                .toMap()
            val cacUnit = KhoBai.get(context).cacTuCua(BO_TIENG_ANH).map { it.unit }.filter { it > 0 }
            val tatCa = (cacUnit + tenUnit.keys).distinct().sorted()
            if (tatCa.isEmpty()) return emptyList()
            return listOf(Nhom("", tatCa.map { u -> u to (tenUnit[u] ?: "Unit $u") }))
        }
        val sgk = cuaMon(mon).flatMap { it.sgk }.distinct()
        val ra = mutableListOf<Nhom>()
        val daCo = mutableSetOf<Int>()
        sgk.flatMap { NganHang.cacBai(context, it) }.forEach { b ->
            val so = soBai(b.bai) ?: return@forEach
            if (!daCo.add(so)) return@forEach
            if (ra.isEmpty() || ra.last().ten != b.chuong) ra += Nhom(b.chuong, emptyList())
            val cuoi = ra.removeAt(ra.size - 1)
            ra += cuoi.copy(cacBai = cuoi.cacBai + (so to b.bai))
        }
        return ra
    }
}
