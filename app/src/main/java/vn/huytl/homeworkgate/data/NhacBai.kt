package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar

/**
 * Nhac bai theo vo dan do: moi dong dan do nhac truoc mot ngay, cho tiet sau cua dung mon do.
 *
 * TU 30/9/2026 VO DAN DO THOI DUNG DE TINH GIO. Ba Huy bo tron goi 45 phut, bai chup anh chi
 * con tinh le, va vo dan do doi viec: Le Hoa hoc xong thi ghi vo cua ngay hom do, co dan gi
 * la cho buoi hoc ke tiep cua mon do, may nhac truoc mot ngay. Truoc do vo chi song toi trua
 * hom sau, y nhu bai nao trong vo cung la bai cho ngay mai.
 *
 * TUNG MON MOT HAN (Ba Huy chon 30/9/2026). "Tiếng Anh: làm bài tập Unit 2" ghi trong vo thu
 * Hai thi han la tiet Anh ke tiep, chieu thu Bay, va may nhac tu thu Sau. Dong khong doc ra
 * mon ("Mang sách vở đầy đủ") thi han la buoi hoc ke tiep. Han tinh theo [ThoiKhoaBieu] va
 * [NgayNghi], nen vo truoc Tet nhac cho buoi dau tien sau Tet.
 *
 * GIU CHU CUA NHIEU TRANG VO. Bai giao thu Hai co khi toi thu Bay moi den han, trong khi thu
 * Ba, thu Tu con da chup vo moi. Nen o day giu ban chu cua moi trang da soat, toi khi dong
 * cuoi cung cua trang do qua han. Tam anh van chi mot, o [VoDanDo].
 */
object NhacBai {

    /** Mot trang vo da doc ra chu. Chi giu phan nhac bai can, khong giu anh. */
    data class Trang(
        /** Ngay ghi tren vo: ngay Le Hoa hoc buoi co dan. */
        val ngay: LocalDate,
        val cacDong: List<VoDanDo.Dong>,
        /**
         * Moc chup tam anh, lay tu [VoDanDo.DanDo.chupLuc]. Con sua ngay cua chinh trang da
         * luu thi moc nay giu nguyen, nho vay ban cu duoc thay chu khong nam lai voi ngay sai.
         */
        val chupLuc: Long = 0L
    )

    /** Mot dong dan do kem han cua no. */
    data class Muc(
        val chu: String,
        val laBaiTap: Boolean,
        /** Ten mon trong [ThoiKhoaBieu]. null la khong doc ra mon, han la buoi hoc ke tiep. */
        val mon: String?,
        /** Ngay ghi tren trang vo co dong nay. */
        val ngayVo: LocalDate,
        /** Ngay cua buoi hoc phai xong dong nay. */
        val ngayHan: LocalDate,
        val buoi: BuoiHoc
    ) {
        /** Luc vao hoc buoi han. Tu luc nay dong nay thoi nhac. */
        val hetLuc: LocalDateTime get() = lucVaoHoc(ngayHan, buoi)

        /** Nhac tu dau ngay truoc buoi han. */
        val nhacTu: LocalDateTime get() = ngayHan.minusDays(1).atStartOfDay()
    }

    /** Cac dong cung han mot buoi hoc, xep bai tap truoc. */
    data class NhomBuoi(val ngay: LocalDate, val buoi: BuoiHoc, val cac: List<Muc>) {
        val hetLuc: LocalDateTime get() = lucVaoHoc(ngay, buoi)
        val cacBai: List<Muc> get() = cac.filter { it.laBaiTap }
        val dongKhac: List<Muc> get() = cac.filterNot { it.laBaiTap }

        /** Khoa cua buoi, cung dang voi [TinhLoiNhac.maBuoi]: "20261003-CHIEU". */
        val ma: String
            get() = "%04d%02d%02d-%s".format(ngay.year, ngay.monthValue, ngay.dayOfMonth, buoi.buoi.name)

        /** "chiều thứ bảy 3/10". */
        fun ten(): String = "${TinhLoiNhac.moTaBuoi(buoi)} ${ngay.dayOfMonth}/${ngay.monthValue}"
    }

    /**
     * Tim toi da bao nhieu ngay de ra tiet sau cua mot mon. Mon mot tuan mot tiet thi
     * bay ngay, cong them muoi ngay nghi Tet la gan ba tuan.
     */
    private const val TIM_TOI_DA_NGAY = 28

    /** Giu toi da bay nhieu trang. Trang nao cung het han trong vai tuan, so nay chi la chot. */
    private const val GIU_TOI_DA_TRANG = 20

    private const val KHOA = "nhac_bai"

    // ------------------------------------------------------------------ doc ten mon

    /**
     * Mon cua mot dong dan do, doc tu ten mon o dau dong. null la khong doc ra.
     *
     * Vo cua Le Hoa ghi ten mon kieu nao cung co: "TOÁN", "KHTN", "NV", "LS-ĐL", "ÂNhạc",
     * "STEAM", "TTNT". May doc vo giu nguyen chu viet tat, xem [vn.huytl.homeworkgate.ai
     * .DocDanDo], nen o day phai hieu duoc het.
     *
     * CHI XET PHAN TRUOC DAU HAI CHAM (hay gach ngang co cach hai ben). Khong co dau ngan
     * cach thi chi nhan ten mon khong the nham: "Sinh hoạt ngoài trời" khong duoc doc
     * thanh mon Sinh, "Anh trai..." khong duoc doc thanh Tieng Anh.
     */
    fun monCua(chu: String): String? {
        val t = chu.trim()
        val haiCham = t.indexOf(':')
        if (haiCham in 1..DAU_DAI_NHAT) return tuTen(t.substring(0, haiCham))
        Regex("""\s[-–]\s""").find(t)?.range?.first?.takeIf { it in 1..DAU_DAI_NHAT }?.let {
            return tuTen(t.substring(0, it))
        }
        val khong = LichKiemTra.boDau(t.lowercase())
        return DAU_DONG_KHONG_NHAM.firstOrNull { (mau, _) -> mau.containsMatchIn(khong) }?.second
    }

    private const val DAU_DAI_NHAT = 30

    /**
     * Mon tu phan ten o dau dong. Thu ca cum, roi bot dan tu cuoi: "Toán (cô Lan)" van la
     * Toan, "Tiếng Anh cô Hà" van la Tieng Anh. So thu tu o dau ("1. Toán") thi bo qua.
     */
    private fun tuTen(dau: String): String? {
        val tu = LichKiemTra.boDau(dau.lowercase())
            .split(Regex("""[^a-z0-9]+"""))
            .filter { it.isNotEmpty() }
            .dropWhile { t -> t.all { it.isDigit() } }
        for (n in tu.size downTo 1) {
            TEN_MON[tu.take(n).joinToString("")]?.let { return it }
        }
        return null
    }

    /** Ten mon da bo dau, bo cach, bo gach -> ten mon trong [ThoiKhoaBieu]. */
    private val TEN_MON: Map<String, String> = buildMap {
        fun them(mon: String, vararg ten: String) = ten.forEach { put(it, mon) }
        them("Toán", "toan")
        them("Ngữ văn", "nv", "van", "nguvan")
        them("Tiếng Anh", "ta", "anh", "tienganh", "tanh", "anhvan", "av", "english")
        them(
            LichKiemTra.KHTN, "khtn", "khoahoctunhien", "khoahoc", "ly", "li", "vatly", "vatli",
            "hoa", "hoahoc", "sinh", "sinhhoc"
        )
        them("Lịch sử - Địa lý", "lsdl", "lichsudialy", "lichsuvadialy", "ls", "dl", "su", "dia", "sudia", "lichsu", "dialy")
        them("Giáo dục công dân", "gdcd", "cd", "giaoduccongdan", "congdan")
        them("Công nghệ", "cn", "congnghe")
        them("Tin học", "tin", "tinhoc")
        them("Mỹ thuật", "mt", "mythuat", "mithuat")
        them("Âm nhạc", "an", "anhac", "amnhac", "nhac")
        them("STEM", "stem", "steam")
        them("Trí tuệ nhân tạo", "ttnt", "trituenhantao", "ai")
        them(
            "Trải nghiệm hướng nghiệp", "tn", "tnhn", "hdtn", "hdtnhn", "trainghiem",
            "trainghiemhuongnghiep", "huongnghiep"
        )
        them("Kỹ năng", "kn", "kynang", "kinang", "kynangsong")
        them("Giáo dục địa phương", "gddp", "giaoducdiaphuong", "diaphuong")
        them(ThoiKhoaBieu.MON_THE_DUC, "gdtc", "td", "theduc", "giaoducthechat", "thechat")
    }

    /**
     * Ten mon o dau dong khong co dau ngan cach, tren chu da bo dau. Chi nhung ten khong
     * the la chu thuong: viet tat nhu "khtn", "gdcd", hay ten mon viet du.
     */
    private val DAU_DONG_KHONG_NHAM: List<Pair<Regex, String>> = listOf(
        "toan" to "Toán",
        "ngu van" to "Ngữ văn",
        "tieng anh" to "Tiếng Anh",
        "khtn" to LichKiemTra.KHTN,
        "khoa hoc tu nhien" to LichKiemTra.KHTN,
        "ls ?- ?dl" to "Lịch sử - Địa lý",
        "gdcd" to "Giáo dục công dân",
        "cong nghe" to "Công nghệ",
        "tin hoc" to "Tin học",
        "my thuat" to "Mỹ thuật",
        "am nhac" to "Âm nhạc",
        "stea?m" to "STEM",
        "ttnt" to "Trí tuệ nhân tạo",
        "gddp" to "Giáo dục địa phương",
        "gdtc" to ThoiKhoaBieu.MON_THE_DUC
    ).map { (mau, mon) -> Regex("""^$mau(?![a-z0-9])""") to mon }

    // ------------------------------------------------------------------ tinh han

    /**
     * Buoi hoc ma mot dong phai xong: tiet dau tien cua [mon] sau ngay [ngayVo], bo ngay
     * nghi. Mon null, hay mon khong co trong thoi khoa bieu, thi la buoi hoc ke tiep.
     *
     * Tinh tu ngay SAU ngay tren vo: vo ghi luc tan hoc, cac buoi cua chinh hom do da qua.
     */
    fun hanCua(mon: String?, ngayVo: LocalDate): Pair<LocalDate, BuoiHoc>? {
        if (mon != null) timBuoi(ngayVo) { mon in it.monTheoTiet.values }?.let { return it }
        return timBuoi(ngayVo) { true }
    }

    /** Buoi hoc dau tien sau ngay [ngay]. Dung cho han cua ban vo dang giu, xem [VoDanDo]. */
    fun buoiSau(ngay: LocalDate): Pair<LocalDate, BuoiHoc>? = hanCua(null, ngay)

    private fun timBuoi(sau: LocalDate, dung: (BuoiHoc) -> Boolean): Pair<LocalDate, BuoiHoc>? {
        for (i in 1..TIM_TOI_DA_NGAY) {
            val d = sau.plusDays(i.toLong())
            if (NgayNghi.laNgayNghi(NgayNghi.calendarCua(d.year, d.monthValue, d.dayOfMonth))) continue
            ThoiKhoaBieu.buoiHocCua(thuCua(d)).firstOrNull(dung)?.let { return d to it }
        }
        return null
    }

    fun lucVaoHoc(ngay: LocalDate, buoi: BuoiHoc): LocalDateTime =
        ngay.atTime(buoi.phutVaoHoc / 60, buoi.phutVaoHoc % 60)

    /** Doi thu cua LocalDate sang hang so Calendar ma [ThoiKhoaBieu] dung. */
    private fun thuCua(d: LocalDate): Int = when (d.dayOfWeek) {
        DayOfWeek.MONDAY -> Calendar.MONDAY
        DayOfWeek.TUESDAY -> Calendar.TUESDAY
        DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
        DayOfWeek.THURSDAY -> Calendar.THURSDAY
        DayOfWeek.FRIDAY -> Calendar.FRIDAY
        DayOfWeek.SATURDAY -> Calendar.SATURDAY
        else -> Calendar.SUNDAY
    }

    /**
     * Moi dong cua moi trang, kem han. Dong trung chu va trung han thi chi giu mot, cua
     * trang moi hon: con chep lai bai chua lam xong sang trang hom sau la chuyen thuong.
     */
    fun cacMuc(cacTrang: List<Trang>): List<Muc> {
        val daCo = mutableSetOf<String>()
        return cacTrang.sortedByDescending { it.ngay }.flatMap { tr ->
            tr.cacDong.mapNotNull { d ->
                val mon = monCua(d.chu)
                val (ngayHan, buoi) = hanCua(mon, tr.ngay) ?: return@mapNotNull null
                val khoa = SoCaiBai.chuanHoa(d.chu) + "|" + ngayHan + "|" + buoi.buoi
                if (!daCo.add(khoa)) return@mapNotNull null
                Muc(d.chu, d.laBaiTap, mon, tr.ngay, ngayHan, buoi)
            }
        }
    }

    /** Moi dong chua toi han, gom theo buoi, buoi som truoc. */
    fun sapToi(cacTrang: List<Trang>, bayGio: LocalDateTime): List<NhomBuoi> =
        gom(cacMuc(cacTrang).filter { it.hetLuc.isAfter(bayGio) })

    /** Cac dong dang trong khoang nhac: tu dau ngay truoc buoi han toi luc vao hoc. */
    fun canNhac(cacTrang: List<Trang>, bayGio: LocalDateTime): List<NhomBuoi> =
        gom(cacMuc(cacTrang).filter { !bayGio.isBefore(it.nhacTu) && it.hetLuc.isAfter(bayGio) })

    private fun gom(cac: List<Muc>): List<NhomBuoi> =
        cac.groupBy { it.ngayHan to it.buoi.buoi }
            .map { (_, ds) ->
                NhomBuoi(
                    ds.first().ngayHan, ds.first().buoi,
                    ds.sortedWith(compareByDescending<Muc> { it.laBaiTap }.thenBy { it.ngayVo })
                )
            }
            .sortedBy { it.hetLuc }

    // ------------------------------------------------------------------ luu trong may

    fun docTrang(context: Context): List<Trang> {
        val chu = Prefs.get(context).raw().getString(KHOA, null) ?: return emptyList()
        return runCatching { tuJson(JSONArray(chu)) }.getOrDefault(emptyList())
    }

    /**
     * Giu chu cua mot trang vo vua luu. Ban chi co anh thi chua co gi de nhac.
     *
     * Thay trang cung ngay, hay cung tam anh: con soat lai trang da luu, hay sua ngay cua no,
     * thi ban cu phai di. Trang da het han o moi dong thi bo.
     */
    @Synchronized
    fun ghi(context: Context, vo: VoDanDo.DanDo, bayGio: LocalDateTime = LocalDateTime.now()) {
        if (vo.chuaDoc) return
        val ngay = vo.ngayDoc() ?: return
        val moi = Trang(ngay, vo.cacDong, vo.chupLuc)
        val giu = docTrang(context).filterNot {
            it.ngay == ngay || (vo.chupLuc != 0L && it.chupLuc == vo.chupLuc)
        }
        luu(context, don(giu + moi, bayGio))
    }

    /** Bo cac trang da het han o moi dong. Goi cung luc don [VoDanDo]. */
    @Synchronized
    fun donDep(context: Context, bayGio: LocalDateTime = LocalDateTime.now()) {
        val cu = docTrang(context)
        val con = don(cu, bayGio)
        if (con.size != cu.size) luu(context, con)
    }

    @Synchronized
    fun xoaHet(context: Context) {
        Prefs.get(context).raw().edit().remove(KHOA).commit()
    }

    private fun don(cac: List<Trang>, bayGio: LocalDateTime): List<Trang> =
        cac.filter { tr ->
            tr.cacDong.any { d ->
                hanCua(monCua(d.chu), tr.ngay)?.let { (n, b) -> lucVaoHoc(n, b).isAfter(bayGio) } == true
            }
        }.sortedBy { it.ngay }.takeLast(GIU_TOI_DA_TRANG)

    private fun luu(context: Context, cac: List<Trang>) {
        Prefs.get(context).raw().edit().putString(KHOA, sangJson(cac).toString()).commit()
    }

    fun sapToi(context: Context, bayGio: LocalDateTime = LocalDateTime.now()): List<NhomBuoi> =
        sapToi(docTrang(context), bayGio)

    fun canNhac(context: Context, bayGio: LocalDateTime = LocalDateTime.now()): List<NhomBuoi> =
        canNhac(docTrang(context), bayGio)

    /** Cac dong han dung buoi [buoi] ngay [ngay], cho man soan tap va loi nhac soan tap. */
    fun choBuoi(context: Context, ngay: LocalDate, buoi: BuoiHoc): List<Muc> =
        choBuoi(docTrang(context), ngay, buoi)

    /** Nhu tren, ngay lay tu Calendar cua [TinhLoiNhac.buoiKeTiep]. */
    fun choBuoi(context: Context, cal: Calendar, buoi: BuoiHoc): List<Muc> =
        choBuoi(context, ngayCua(cal), buoi)

    fun choBuoi(cacTrang: List<Trang>, ngay: LocalDate, buoi: BuoiHoc): List<Muc> =
        cacMuc(cacTrang).filter { it.ngayHan == ngay && it.buoi.buoi == buoi.buoi }
            .sortedWith(compareByDescending<Muc> { it.laBaiTap }.thenBy { it.ngayVo })

    fun ngayCua(cal: Calendar): LocalDate =
        LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

    /** Mot dong cho man hinh va tin nhan: "Toán: làm bài 2 trang 36 (vở 28/9)". */
    fun moTa(m: Muc): String = "${m.chu} (vở ${m.ngayVo.dayOfMonth}/${m.ngayVo.monthValue})"

    fun sangJson(cac: List<Trang>): JSONArray = JSONArray().apply {
        cac.forEach { tr ->
            put(
                JSONObject()
                    .put("ngay", tr.ngay.toString())
                    .put("chupLuc", tr.chupLuc)
                    .put(
                        "dong",
                        JSONArray().apply {
                            tr.cacDong.forEach { put(JSONObject().put("chu", it.chu).put("bai", it.laBaiTap)) }
                        }
                    )
            )
        }
    }

    fun tuJson(mang: JSONArray): List<Trang> = (0 until mang.length()).mapNotNull { i ->
        val o = mang.optJSONObject(i) ?: return@mapNotNull null
        val ngay = runCatching { LocalDate.parse(o.optString("ngay")) }.getOrNull() ?: return@mapNotNull null
        val dong = o.optJSONArray("dong") ?: JSONArray()
        Trang(
            ngay = ngay,
            cacDong = (0 until dong.length()).mapNotNull { j ->
                val x = dong.optJSONObject(j) ?: return@mapNotNull null
                val chu = x.optString("chu").trim()
                if (chu.isEmpty()) null else VoDanDo.Dong(chu, x.optBoolean("bai"))
            },
            chupLuc = o.optLong("chupLuc")
        )
    }
}
