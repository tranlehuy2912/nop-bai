package vn.huytl.homeworkgate.data

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar

/**
 * Doc ra mot lan kiem tra sap toi tu mot dong chu: dong dan do trong vo ("Toán: tiết
 * sau kiểm tra 15 phút bài 2, 3") hay tin cua co ("Mai lớp kiểm tra 15 phút môn Toán.").
 *
 * VI SAO CO. Ba Huy muon de Giai de mo theo lich kiem tra that cua lop, khong chi moi
 * tuan mot de (27/9/2026). Lich do da nam san trong may, chi la chua ai doc: vo dan do
 * giu nhung dong "tiết sau kiểm tra..." con danh dau la khong phai bai tap, va tin cua
 * co giu nguyen van tin nhom lop Ba Huy chep sang. Xem [GiaiDe.taoNeuCan].
 *
 * DOC BANG LUAT, KHONG GOI AI. Moi dong mot hai chuc chu, cau truc lap lai (mon, "kiểm
 * tra", bai may, luc nao), va doc sai thi hai cua di nhe: mot de on thua, hay thieu de
 * on. Goi AI cho viec nay la them mot duong phu thuoc vao mang va vao han muc khoa.
 *
 * Nhung cho de doc nham, va cach tranh:
 *  - "thì" ma bo dau thanh "thi" (thi cu): so "thi" CO DAU, chi an "thi" khong dau;
 *  - "toàn bộ" bo dau thanh "toan": so "toán" co dau;
 *  - "học sinh", "sinh hoạt" chua chu "sinh": chi nhan Sinh khi la ten mon dau dong,
 *    "môn Sinh" hay "Sinh học";
 *  - "lý do": chi nhan Li khi la ten mon dau dong, "môn Lí" hay "Vật lí";
 *  - "thứ tự" gan giong "thứ tư": so co dau.
 * Tu viet tat va cach viet khong dau thi so tren ban da bo dau: "kiem tra", "ktra",
 * "bai 2 den bai 5".
 */
object LichKiemTra {

    /**
     * Mot lan kiem tra doc ra duoc.
     *
     * @param mon "Toán" hay "Khoa học tự nhiên": hai mon co sach bai tap trong may.
     * @param cacBai so bai dong chu nhac toi, rong la khong noi bai nao.
     * @param chuong so chuong dong chu nhac toi, null la khong noi.
     * @param ngay ngay kiem tra, null la khong doc ra.
     * @param chu dong chu goc, de ghi vao de va vao tin cho Ba Huy.
     */
    data class KiemTra(
        val mon: String,
        val cacBai: List<Int>,
        val chuong: Int?,
        val ngay: LocalDate?,
        val chu: String
    )

    const val TOAN = "Toán"
    const val KHTN = "Khoa học tự nhiên"

    /**
     * Doc mot dong. Rong khi dong khong noi ve kiem tra, hay khong noi mon Toan, KHTN.
     * Dong nhac ca hai mon ("kiểm tra Toán và KHTN") thi ra hai lan kiem tra.
     *
     * @param ngayNguon ngay cua dong chu: ngay ghi tren vo dan do, hay ngay co nhan tin.
     *   Moi ngay tuong doi ("mai", "tiết sau", "thứ năm") tinh tu ngay nay.
     */
    fun doc(chu: String, ngayNguon: LocalDate): List<KiemTra> {
        val goc = chu.trim()
        if (goc.isEmpty()) return emptyList()
        val co = Normalizer.normalize(goc, Normalizer.Form.NFC).lowercase()
        val khong = boDau(co)
        if (!coKiemTra(co, khong)) return emptyList()

        val cacMon = mon(co, khong)
        if (cacMon.isEmpty()) return emptyList()
        val bai = cacBai(khong)
        val chuong = chuong(khong)
        return cacMon.map { m ->
            KiemTra(m, bai, chuong, ngayCuaMon(goc, co, khong, m, cacMon.size > 1, ngayNguon), goc)
        }
    }

    private fun coKiemTra(co: String, khong: String): Boolean =
        "kiem tra" in khong || "kiemtra" in khong || "khao sat" in khong ||
            VIET_TAT_KIEM_TRA.containsMatchIn(khong) ||
            // "thi" co dau: "thì" bo dau cung ra "thi".
            Regex("""(?<![\p{L}])thi(?![\p{L}])""").containsMatchIn(co)

    /** "ktra", "k.tra", "kt 15p". */
    private val VIET_TAT_KIEM_TRA = Regex("""(?<![a-z])(k\.?\s?tra|kt)(?![a-z])""")

    private val DAU_DONG_TOAN = Regex("""^\s*toán\s*[:\-–]""")
    private val DAU_DONG_KHTN = Regex(
        """^\s*(khtn|khoa học tự nhiên|hoá|hóa|hoá học|hóa học|lí|lý|vật lí|vật lý|sinh|sinh học)\s*[:\-–]"""
    )

    /** Mon cua dong chu, theo thu tu: ten mon dau dong, "môn ...", roi ten mon o bat cu dau. */
    private fun mon(co: String, khong: String): List<String> {
        if (DAU_DONG_TOAN.containsMatchIn(co)) return listOf(TOAN)
        if (DAU_DONG_KHTN.containsMatchIn(co)) return listOf(KHTN)
        return buildList {
            if (Regex("""(?<![\p{L}])toán(?![\p{L}])""").containsMatchIn(co)) add(TOAN)
            val khtn = Regex("""(?<![a-z])khtn(?![a-z])""").containsMatchIn(khong) ||
                "khoa hoc tu nhien" in khong ||
                Regex("""(?<![\p{L}])(hoá|hóa)(?![\p{L}])""").containsMatchIn(co) ||
                Regex("""vật\s*(lí|lý)|môn\s*(lí|lý|sinh)|sinh\s*học""").containsMatchIn(co)
            if (khtn) add(KHTN)
        }
    }

    /**
     * Cac so bai nhac toi: "bài 2, 3", "bài 2 và bài 3", "bài 2-5", "bài 2 đến bài 5".
     * Doc tren ban bo dau, nen "bai 2 den 5" cung vay.
     */
    fun cacBai(khong: String): List<Int> {
        val ra = linkedSetOf<Int>()
        CUM_BAI.findAll(khong).forEach { m ->
            val cum = m.groupValues[1]
            val so = Regex("""\d+""").findAll(cum).map { it.value.toInt() to it.range }.toList()
            so.forEachIndexed { i, (n, khoang) ->
                ra += n
                val sau = so.getOrNull(i + 1) ?: return@forEachIndexed
                val giua = cum.substring(khoang.last + 1, sau.second.first)
                if (Regex("""-|–|den|toi""").containsMatchIn(giua) && sau.first > n && sau.first - n <= 12) {
                    for (k in n + 1 until sau.first) ra += k
                }
            }
        }
        return ra.filter { it in 1..60 }
    }

    /**
     * Mot cum so bai sau chu "bai". So dung truoc dau cham va mot chu so ("bài 2.26",
     * "bài 10.3") la so cau bai tap chu khong phai so bai: bo qua.
     *
     * Moi so phai lay tron chu so, `(?!\d|\.\d)`: chi co `(?!\.\d)` thi may lui bot mot
     * chu so cho qua duoc dieu kien, va "bài 10.3" doc thanh bai 1, "bài 25.1" thanh bai 2.
     */
    private val CUM_BAI = Regex(
        """(?<![a-z])bai\s*(\d+(?!\d|\.\d)(?:\s*(?:,|;|&|\+|va|-|–|den|toi)\s*(?:bai\s*)?\d+(?!\d|\.\d))*)"""
    )

    /** "chương 1", "chương II". */
    private fun chuong(khong: String): Int? {
        val m = Regex("""(?<![a-z])chuong\s*([ivx]+|\d+)(?![a-z])""").find(khong) ?: return null
        val t = m.groupValues[1]
        return t.toIntOrNull() ?: laMa(t)
    }

    /** So La Ma viet thuong ra so, 1 den 20. Dung chung voi [GiaiDe] luc doc ten chuong. */
    internal fun laMa(t: String): Int? {
        val gia = mapOf('i' to 1, 'v' to 5, 'x' to 10)
        var tong = 0
        for (i in t.indices) {
            val g = gia[t[i]] ?: return null
            val sau = t.getOrNull(i + 1)?.let { gia[it] } ?: 0
            tong += if (g < sau) -g else g
        }
        return tong.takeIf { it in 1..20 }
    }

    /**
     * Ngay kiem tra cua mot mon. Dong nhac nhieu mon ("Toán ngày 5/11, KHTN ngày 7/11")
     * thi doc trong doan cua mon do truoc, khong thi mon nao cung lay ngay dau tien.
     */
    private fun ngayCuaMon(
        goc: String,
        co: String,
        khong: String,
        mon: String,
        nhieuMon: Boolean,
        nguon: LocalDate
    ): LocalDate? {
        if (nhieuMon) {
            val cacDoan = goc.split(Regex("""[,;]"""))
            val cua = cacDoan.filter { d ->
                val c = Normalizer.normalize(d, Normalizer.Form.NFC).lowercase()
                this.mon(c, boDau(c)).contains(mon)
            }
            if (cua.size == 1) {
                val d = cua[0]
                val c = Normalizer.normalize(d, Normalizer.Form.NFC).lowercase()
                ngay(d, c, boDau(c), mon, nguon)?.let { return it }
            }
        }
        return ngay(goc, co, khong, mon, nguon)
    }

    /**
     * Ngay kiem tra, tinh tu [nguon]. null la khong doc ra.
     *
     * Thu tu: ngay viet so ("5/11", "ngày 5 tháng 11"), roi thu trong tuan ("thứ 5", co
     * "tuần sau" thi la thu do cua tuan sau), roi "mai", "tiết sau", "tuần sau".
     *
     * Ngay viet so ma khong sau [nguon] thi thuong la ngay cua tin chu khong phai ngay
     * kiem tra, nen xet cac dau hieu khac truoc.
     */
    private fun ngay(goc: String, co: String, khong: String, mon: String, nguon: LocalDate): LocalDate? {
        val tuanSau = "tuan sau" in khong || "tuan toi" in khong
        // Ngay viet so dung bang ngay cua tin thi chi dung khi khong con dau hieu nao
        // khac: "Thông báo ngày 28/9: thứ 4 kiểm tra" la kiem tra thu Tu.
        val vietSo = ngayVietSo(khong, nguon)
        if (vietSo != null && vietSo.isAfter(nguon)) return vietSo
        val homNay = vietSo?.takeIf { it == nguon }
        THU.forEach { (mau, thu) ->
            if (Regex(mau).containsMatchIn(co)) {
                if (tuanSau) {
                    // Thu Hai cua tuan sau, roi toi dung thu do.
                    val dauTuanSau = nguon.plusDays((8 - nguon.dayOfWeek.value).toLong())
                    return dauTuanSau.plusDays((thu.value - 1).toLong())
                }
                return ngaySau(nguon) { it.dayOfWeek == thu }
            }
        }
        if (laNgayMai(goc)) return nguon.plusDays(1)
        if ("tiet sau" in khong || "tiet toi" in khong || "buoi sau" in khong || "buoi toi" in khong) {
            return ngaySau(nguon) { d -> mon in ThoiKhoaBieu.monTrongNgay(thuCua(d)) }
        }
        if (tuanSau) return nguon.plusDays(7)
        return homNay
    }

    /**
     * "5/11", "5/11/2026", "ngày 5 tháng 11", "ngày 5 tháng 11 năm 2026".
     *
     * Khong ghi nam thi lay nam lam ngay gan [nguon] nhat: "2/1" viet trong thang muoi hai
     * la thang mot nam sau, "29/12" viet trong thang mot la thang muoi hai nam truoc.
     */
    private fun ngayVietSo(khong: String, nguon: LocalDate): LocalDate? {
        val m = Regex("""(?<![\d/])(\d{1,2})\s*/\s*(\d{1,2})(?:\s*/\s*(\d{4}))?(?![\d/])""").find(khong)
            ?: Regex("""(?<![a-z])ngay\s*(\d{1,2})\s*thang\s*(\d{1,2})(?:\s*nam\s*(\d{4}))?(?!\d)""").find(khong)
            ?: return null
        val d = m.groupValues[1].toInt()
        val t = m.groupValues[2].toInt()
        m.groupValues[3].toIntOrNull()?.let { nam -> return runCatching { LocalDate.of(nam, t, d) }.getOrNull() }
        return (nguon.year - 1..nguon.year + 1)
            .mapNotNull { nam -> runCatching { LocalDate.of(nam, t, d) }.getOrNull() }
            .minByOrNull { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(nguon, it)) }
    }

    /**
     * "mai", "ngày mai", "Mai" o dau cau. Doc tren chu goc, con dau va hoa thuong: bo dau
     * thi "mãi", "mái" cung thanh "mai", viet thuong thi ten co giao "cô Mai" cung thanh
     * "cô mai". Ten nguoi dung sau cô, thầy, bạn... thi khong tinh.
     */
    private fun laNgayMai(goc: String): Boolean {
        val chu = Normalizer.normalize(goc, Normalizer.Form.NFC)
        val ten = Regex("""(?i)(?<![\p{L}])(cô|thầy|bạn|chị|anh|em|bà|ông|bác|chú|dì|cậu)\s+mai(?![\p{L}])""")
        val sach = ten.replace(chu, " ")
        return Regex("""(?<![\p{L}])(?:[Nn]gày\s+)?mai(?![\p{L}])""").containsMatchIn(sach) ||
            Regex("""(?:^|[.:;!?\-–]\s*)Mai(?![\p{L}])""").containsMatchIn(sach)
    }

    private val THU = listOf(
        """thứ\s*(hai|2)(?![\p{L}\d])""" to DayOfWeek.MONDAY,
        """thứ\s*(ba|3)(?![\p{L}\d])""" to DayOfWeek.TUESDAY,
        """thứ\s*(tư|4)(?![\p{L}\d])""" to DayOfWeek.WEDNESDAY,
        """thứ\s*(năm|5)(?![\p{L}\d])""" to DayOfWeek.THURSDAY,
        """thứ\s*(sáu|6)(?![\p{L}\d])""" to DayOfWeek.FRIDAY,
        """thứ\s*(bảy|7)(?![\p{L}\d])""" to DayOfWeek.SATURDAY
    )

    /** Ngay dau tien SAU [tu] thoa [dung], trong vong hai tuan. */
    private fun ngaySau(tu: LocalDate, dung: (LocalDate) -> Boolean): LocalDate? =
        (1L..14L).map { tu.plusDays(it) }.firstOrNull(dung)

    /** Doi DayOfWeek sang hang so Calendar ma [ThoiKhoaBieu] dung. */
    private fun thuCua(d: LocalDate): Int = when (d.dayOfWeek) {
        DayOfWeek.MONDAY -> Calendar.MONDAY
        DayOfWeek.TUESDAY -> Calendar.TUESDAY
        DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
        DayOfWeek.THURSDAY -> Calendar.THURSDAY
        DayOfWeek.FRIDAY -> Calendar.FRIDAY
        DayOfWeek.SATURDAY -> Calendar.SATURDAY
        else -> Calendar.SUNDAY
    }

    /** Bo dau tieng Viet, "đ" thanh "d". Chu da viet thuong tu truoc. */
    fun boDau(chu: String): String =
        Normalizer.normalize(chu, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .replace('đ', 'd')
}
