package vn.huytl.homeworkgate.data

import java.text.Normalizer
import kotlin.math.abs
import kotlin.random.Random
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.TheHoc

/**
 * Ban phim ghep cua hai man Kiem tra bai va Kiem tra tu vung, va bac goi y sau moi lan sai.
 *
 * VI SAO CO. Truoc 2/10/2026 hai man nay go bang ban phim Android, man Kiem tra bai them mot
 * dai nut ky hieu, trong khi bai lam tren may thi bam phim ghep va bai co giao thi chup vo.
 * Ba Huy thay ba cach lam la khong dong nhat, va chot ngay hom do:
 *  - Kiem tra tu vung, chieu nhin nghia viet tu: moi chu cai mot o, du 26 phim QWERTY, khong
 *    hien chu dau ([ghepTu]);
 *  - chieu nhin tu chon nghia: bon phuong an cung kieu nut A, B, C, D voi bai lam tren may
 *    ([ghepNghia]);
 *  - Kiem tra bai, dap an la chu: xep the chu, phim nhieu la chu lay tu dap an cac the khac
 *    cung bai ([ghepThe], [Kieu.CHU]). Dap an Toan phan lon la thuat ngu hai ba tieng ("số mũ",
 *    "hạng tử"), con can nhan dung thuat ngu hon la danh van, va ban phim chu Viet du dau thi
 *    qua nhieu phim;
 *  - Kiem tra bai, dap an la so, cong thuc: phim rieng cua the cong phim nhieu, hang so va dau
 *    co dinh ([Ghep.phimCoBan]), khong hien so o vi hien o la lo khuon cong thuc
 *    ([Kieu.BIEU_THUC]).
 *
 * Moi the, moi tu mot khoi [Ghep] dung tai cho, ve bang [vn.huytl.homeworkgate.ui.KhungGhep].
 * Cham van theo luat cu cua tung man ([HocThuoc.dung], [LuatTuVung.dung]) tren chuoi
 * [vn.huytl.homeworkgate.ui.KhungGhep.traLoi]: khoi ghep chi doi cach nhap.
 *
 * BAC GOI Y (Ba Huy chot 2/10/2026), thay cho bac goi y bang chu truoc do (so chu cai, chu dau,
 * nua tu, hai phan ba, ca tu):
 *  - sai 1: mo mot nua phim nhieu, lam tron len;
 *  - sai 2: mo het phim nhieu;
 *  - sai 3: dien san nua dau dap an ([soDienSan]);
 *  - sai 4: hien ca dap an ([BAC_HIEN_HET]); con lam lai cho dung moi qua, va khong duoc phut.
 * Ca hai man hoi lai cau sai o cuoi hang, nen moi bac la mot lan cau quay lai. Phim mo nam
 * nguyen cho, xem [LuatGhep.mucMo].
 */
object PhimKiemTra {

    /** Bac goi y hien ca dap an. Ghi vao so ([vn.huytl.homeworkgate.kho.TraTu.goiY]) la bac cao nhat. */
    const val BAC_HIEN_HET = 4

    /** Bac goi y dien san nua dau dap an. */
    const val BAC_DIEN_SAN = 3

    /** So phim nhieu mo sau [soSai] lan sai, cung thang voi bai lam tren may: xem [LuatGhep.mucMo]. */
    fun mucMo(soSai: Int): Int = soSai.coerceIn(0, LuatGhep.MO_HET)

    /** Bao nhieu phan dau cua dap an [soPhan] phan duoc dien san sau [soSai] lan sai: nua dau, lam tron len. */
    fun soDienSan(soSai: Int, soPhan: Int): Int = if (soSai >= BAC_DIEN_SAN) (soPhan + 1) / 2 else 0

    fun hienHet(soSai: Int): Boolean = soSai >= BAC_HIEN_HET

    /** Hai loai the Kiem tra bai, theo dap an. Xem [kieuCua]. */
    enum class Kieu { CHU, BIEU_THUC }

    /**
     * Khoi ghep cua mot the hay mot tu, kem cac phan cua dap an theo dung thu tu de dien san.
     *
     * @param motThe dap an chi mot the chu: mo het nhieu la chi con dung the do, nen bac
     *   [LuatGhep.MO_HET] da lo dap an.
     * @param noi ghep mot so phan dau thanh chuoi de cham, xem [loHet].
     */
    class Khoi(
        val ghep: Ghep,
        val phan: List<String>,
        private val motThe: Boolean,
        private val noi: (List<String>) -> String
    ) {
        /** Cac phan dat san o bac goi y cua [soSai] lan sai. */
        fun dienSan(soSai: Int): List<String> = phan.take(soDienSan(soSai, phan.size))

        /**
         * Goi y bac [soSai] da lo dap an chua. Lo thi con lam dung van qua, nhung khong duoc
         * cong phut: con doc tren man chu khong tu nho ra.
         *
         * Lo khi da hien het; khi dap an chi mot the ma nhieu da mo het; khi phan dien san da
         * phu ca dap an (dap an mot phan: "6", "k"); hay khi rieng phan dien san da la mot dap
         * an duoc [chamDung] nhan - the co dap an phu ngan hon dap an chinh. Hai canh sau giong
         * luat cua bac goi y bang chu truoc 2/10/2026: dap an ngan thi lo som hon.
         */
        fun loHet(soSai: Int, chamDung: (String) -> Boolean): Boolean {
            if (hienHet(soSai)) return true
            if (motThe && soSai >= LuatGhep.MO_HET) return true
            val san = dienSan(soSai)
            if (san.isEmpty()) return false
            return san.size >= phan.size || chamDung(noi(san))
        }
    }

    // ------------------------------------------------------------ tu vung

    /**
     * Khoi ghep chieu nhin nghia viet tu: moi chu cai cua [tu] mot o, du 26 phim QWERTY, phim
     * nhieu la moi chu cai khong co trong tu. Khong hien chu dau (Ba Huy chot 2/10/2026).
     *
     * Chu khong phai chu cai thi in san trong khuon chu khong bat go: dau cach thanh khoang
     * cach giua cac o, gach noi ("well-trained") va dau phay tren ("farmers' market") hien
     * thang. Phan trong ngoac in san ca cum: "DIY (do-it-yourself)" chi go DIY, "log (on to)"
     * chi go log. Bo tu ngay do co 60 tu co dau cach, 11 tu co gach noi, 4 tu co ngoac hay dau
     * phay tren.
     */
    fun ghepTu(tu: String): Khoi {
        val khuon = khuonCua(tu)
        val chuGo = khuonChuGo(tu, khuon)
        val coTrongTu = chuGo.map { it.lowercaseChar() }.toSet()
        val nhieu = ('a'..'z').filter { it !in coTrongTu }.map { it.toString() }
        val g = Ghep.Chu(
            sao = 1, hoi = "", truoc = "", dap = listOf(tu), nhieu = nhieu,
            khuon = khuon, duPhim = true
        )
        return Khoi(g, chuGo.map { it.toString() }, motThe = false) { g.dien(it.joinToString("")) }
    }

    /** Khuon o cua mot tu: null la mot o chu cai, khac null la chu in san. Xem [ghepTu]. */
    fun khuonCua(tu: String): List<String?> {
        val ra = mutableListOf<String?>()
        var i = 0
        while (i < tu.length) {
            val c = tu[i]
            when {
                c == '(' -> {
                    val dong = tu.indexOf(')', i).let { if (it < 0) tu.lastIndex else it }
                    ra += tu.substring(i, dong + 1)
                    i = dong + 1
                }
                c in 'a'..'z' || c in 'A'..'Z' -> {
                    ra += null
                    i++
                }
                else -> {
                    ra += c.toString()
                    i++
                }
            }
        }
        return ra
    }

    /** Cac chu cai phai go cua [tu] theo thu tu o, dung chu hoa chu thuong nhu sach in. */
    private fun khuonChuGo(tu: String, khuon: List<String?>): List<Char> {
        val ra = mutableListOf<Char>()
        var i = 0
        khuon.forEach { o ->
            if (o == null) {
                ra += tu[i]
                i++
            } else {
                i += o.length
            }
        }
        return ra
    }

    /**
     * Khoi ghep chieu nhin tu chon nghia: [cac] la cac nghia da tron cua buoi, [dung] la chi so
     * nghia dung. Man tu vung cham bang chi so con bam ([vn.huytl.homeworkgate.ui.KhungGhep.daChon]).
     */
    fun ghepNghia(cac: List<String>, dung: Int): Ghep =
        Ghep.Chon(sao = 1, hoi = "", cac = cac, dap = setOf(dung))

    // ------------------------------------------------------------ the hoc thuoc

    /**
     * Dap an la chu hay la bieu thuc.
     *
     * CHU khi dap an chi gom cac tu, moi tu toan chu cai, co the boc trong ngoac ("ampe (A)"),
     * va hoac co dau cach, hoac co chu tieng Viet co dau, hoac la mot tu thuong tu ba chu tro
     * len ("acid", "chloride"). Con lai la BIEU_THUC: so, cong thuc, ki hieu hoa hoc ("HCl",
     * "CO", "NaOH"), don vi ("Pa"), mot chu cai lam bien ("k", "I") va ten viet tat ("MTC") -
     * nhung thu do go tung ki hieu chu khong xep the.
     */
    fun kieuCua(dap: String): Kieu {
        val t = nfc(dap.trim())
        if (!LA_CHU.matches(t)) return Kieu.BIEU_THUC
        val coCach = ' ' in t
        val coDau = t.any { it.isLetter() && it.code >= 128 }
        val thuongDai = t.length >= 3 && t.all { it in 'a'..'z' }
        return if (coCach || coDau || thuongDai) Kieu.CHU else Kieu.BIEU_THUC
    }

    private val LA_CHU = Regex("""\(?\p{L}+\)?( \(?\p{L}+\)?)*""")

    /**
     * Cac phan cua mot dap an: tu (the chu) voi [Kieu.CHU], phim voi [Kieu.BIEU_THUC].
     *
     * Bieu thuc tach tung ki tu, bo dau cach (cham bo dau cach, xem [HocThuoc.chuanHoa]), tru
     * cum chu cai:
     *  - mon Toan: moi chu cai la mot bien ("xy²" ra x, y, ²; "AC'" ra A, C, '), tru tu tieng
     *    Viet trong cong thuc ("a = a' và b ≠ b'") giu ca tu;
     *  - mon KHTN: chu hoa kem cac chu thuong theo sau la mot ki hieu ("Cl", "Na", "Fe", "Pa"),
     *    cum chu thuong la mot dai luong hay don vi ("mct", "mnước", "mol", "kg"). Nen "HCl"
     *    ra H, Cl va "Fe(OH)₃" ra Fe, (, O, H, ), ₃, nhu con viet tren giay.
     */
    fun tach(dap: String, mon: String): List<String> {
        val t = nfc(dap.trim())
        if (kieuCua(t) == Kieu.CHU) return t.split(' ').filter { it.isNotEmpty() }
        val ra = mutableListOf<String>()
        var i = 0
        while (i < t.length) {
            val c = t[i]
            when {
                c.isWhitespace() -> i++
                c.isLetter() -> {
                    var j = i
                    while (j < t.length && t[j].isLetter()) j++
                    ra += tachCumChu(t.substring(i, j), mon)
                    i = j
                }
                else -> {
                    ra += c.toString()
                    i++
                }
            }
        }
        return ra
    }

    private fun tachCumChu(cum: String, mon: String): List<String> = when {
        mon == MON_TOAN && laTuTiengViet(cum) -> listOf(cum)
        mon == MON_TOAN -> cum.map { it.toString() }
        else -> KI_HIEU.findAll(cum).map { it.value }.toList()
    }

    /**
     * Cum chu la mot tu tieng Viet: toan chu Latin va co it nhat mot chu co dau ("và", "khác").
     * Chu Hy Lap ("ΔABC", "π") khong phai: Δ dung truoc ten tam giac la mot ky hieu rieng.
     */
    private fun laTuTiengViet(cum: String): Boolean =
        cum.any { it.code >= 128 } &&
            cum.all { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.LATIN }

    /** Mot ki hieu KHTN: chu hoa kem chu thuong theo sau, hay mot cum chu thuong. */
    private val KI_HIEU = Regex("""\p{Lu}\p{Ll}*|\p{Ll}+|\p{L}""")

    /** So the chu nhieu cua mot the [Kieu.CHU]. */
    const val SO_NHIEU_CHU = 6

    /** So phim nhieu cua mot the [Kieu.BIEU_THUC]. */
    const val SO_NHIEU_PHIM = 4

    /**
     * Khoi ghep cua mot the.
     *
     * Phim nhieu lay tu dap an chinh cua cac the khac trong [cungBo] (khong lay dap an phu: nhieu
     * phai la dap an that cua mot the khac): the cung bai truoc, tron theo [hat], thieu thi lay
     * them o cac bai gan nhat theo thu tu trong file. Bo phim trung voi phan cua the nay, va bo
     * phim chi khac hoa thuong khi bo khong phan biet hoa ("A" lam nhieu cho dap an co "a" thi
     * nhin nhu phim dung). Dap an so khong co phim rieng nao (moi chu so deu o hang co ban) thi
     * khong co nhieu: mot vai chu cai lac giua dap an so thi con biet ngay la phim thua.
     *
     * @param hat hat tron cua buoi, de hai lan hoi cung mot the trong buoi ra cung mot ban phim
     *   (phim mo o lan sau la phim da thay), con buoi sau tron khac.
     */
    fun ghepThe(the: TheHoc, cungBo: List<TheHoc>, phanBietHoa: Boolean, hat: Int): Khoi {
        val rnd = Random(hat)
        val kieu = kieuCua(the.dap)
        val phan = tach(the.dap, the.mon)
        val khac = cungBo.filter { it.id != the.id && kieuCua(it.dap) == kieu }
        val cungBai = khac.filter { it.bai == the.bai }.shuffled(rnd)
        val gan = khac.filter { it.bai != the.bai }.sortedBy { abs(it.thuTu - the.thuTu) }
        val coBan = Ghep.phimCoBan(the.mon).toSet()
        fun khoa(s: String) = if (phanBietHoa) s else s.lowercase()
        val cuaThe = phan.map(::khoa).toSet()
        return when (kieu) {
            Kieu.CHU -> {
                val nhieu = (cungBai + gan).asSequence()
                    .flatMap { tach(it.dap, it.mon).asSequence() }
                    .filter { khoa(it) !in cuaThe }
                    .distinctBy(::khoa)
                    .take(SO_NHIEU_CHU)
                    .toList()
                val g = Ghep.Cau(sao = 1, hoi = "", the = phan, nhieu = nhieu, dap = listOf(the.dap))
                Khoi(g, phan, motThe = phan.size == 1) { it.joinToString(" ") }
            }
            Kieu.BIEU_THUC -> {
                val rieng = phan.filter { it !in coBan }.distinct()
                val nhieu = if (rieng.isEmpty()) emptyList() else (cungBai + gan).asSequence()
                    .flatMap { tach(it.dap, it.mon).asSequence() }
                    .filter { it !in coBan && khoa(it) !in cuaThe }
                    .distinctBy(::khoa)
                    .take(SO_NHIEU_PHIM)
                    .toList()
                val g = Ghep.BieuThuc(
                    sao = 1, hoi = "", dap = listOf(the.dap), phim = rieng, nhieu = nhieu,
                    loiGiai = emptyList()
                )
                Khoi(g, phan, motThe = false) { it.joinToString("") }
            }
        }
    }

    private const val MON_TOAN = "Toán"

    private fun nfc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)
}
