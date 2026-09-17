package vn.huytl.homeworkgate.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * So ghi con mo app nao, tu may gio den may gio.
 *
 * VI SAO CAN: luc het gio choi thi may chan, va Ba Huy biet ngay vi chinh minh
 * phai duyet. Nhung trong gio choi thi may khong chan gi ca - mot tieng do con
 * lam gi tren tablet la mot khoang trong hoan toan. [GioiHanApp] co dem, nhung
 * chi dem app da dat han rieng va chi ra mot con so tong, khong tra loi duoc
 * "may gio den may gio".
 *
 * TACH KHOI [GioiHanApp]: hai thu khac viec. Ben kia la mot cai van - het so phut
 * thi khoa app lai. Cai nay khong khoa gi, chi ghi lai de doc. Tron vao nhau thi
 * moi lan muon xem thong ke lai phai dat han cho app do truoc, va dat han la mot
 * quyet dinh nuoi day con chu khong phai mot cai cong tac ghi so.
 *
 * DON VI LA KHOANG chu khong phai tong so phut: mot dua tre mo YouTube ba lan,
 * moi lan hai muoi phut, khac han voi mot lan mot tieng. Cong lai thanh mot so
 * thi mat dung cho khac nhau do.
 *
 * GIU [GIU_NGAY] NGAY roi xoa. Du de nhin ra mot thoi quen trong tuan, khong du
 * de thanh ho so theo doi mot dua tre - cung mot le voi [DayLog].
 */
object NhatKySuDung {

    /** Mot khoang lien tuc mot app nam truoc mat. */
    data class Doan(val goi: String, val tu: Long, val den: Long) {
        val daiMs: Long get() = (den - tu).coerceAtLeast(0L)
    }

    /** Tong cua mot app trong mot ngay, kem tung khoang de biet luc nao. */
    data class MotApp(val goi: String, val tongMs: Long, val cacDoan: List<Doan>)

    /** Giu nhat ky bao nhieu ngay, tinh ca hom nay. */
    const val GIU_NGAY = 7

    private const val K_DOAN = "su_dung_doan"

    /**
     * Kho rieng cho so nay, khong nam chung file prefs voi phan trang thai.
     *
     * So nay ghi lai moi vai phut trong suot thoi gian con dung may. Nam chung file
     * thi moi lan ghi keo theo mot luot day len Firestore, vi ben [vn.huytl
     * .homeworkgate.dongbo.DongBo] nghe ca file de biet trang thai co doi khong.
     * Ma trang thai thi khong doi ti nao khi con xem YouTube them ba phut.
     */
    private const val KHO = "nhat_ky_su_dung"

    private fun kho(context: Context) = Prefs.khoRieng(context, KHO).also { sp ->
        // Chuyen mot lan tu kho cu. Bo qua thi bang thong ke mat sach bay ngay gan
        // nhat dung hom cap nhat app.
        if (sp.contains(K_DOAN)) return@also
        val cu = Prefs.get(context).raw()
        val du = cu.getString(K_DOAN, null) ?: return@also
        sp.edit().putString(K_DOAN, du).commit()
        cu.edit().remove(K_DOAN).commit()
    }

    /**
     * Toi da bao nhieu khoang. Mot ngay dung may nhieu cung chi vai chuc khoang,
     * nen tran nay chi de mot loi nao do khong lam phong file prefs vo han.
     */
    private const val MAX_DOAN = 800

    /**
     * Roi app roi quay lai trong khoang nay thi tinh la mot khoang lien tuc.
     *
     * Khong noi lai thi mot buoi xem YouTube bi cat thanh hai chuc dong, chi vi
     * moi lan hien thong bao hay doi ngang sang man hinh chinh vai giay. Mot phut
     * ruoi la du de nuot cac lan do ma van tach duoc hai lan choi that su khac
     * nhau.
     */
    private const val NOI_LIEN_MS = 90_000L

    /**
     * Mot khoang dai hon the thi khong tin, bo luon.
     *
     * Gap khi dong ho may bi day toi, hoac khi tien trinh bi dong bang nua ngay
     * roi song lai va tuong rang app van nam truoc mat suot tu do. Ghi vao thi
     * bang thong ke ra "YouTube 9 tieng", ma so do sai va sai theo huong to nhat.
     */
    private const val DOAN_DAI_NHAT_MS = 6 * 60 * 60_000L

    private val dongHo = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
    private val ngayThang = SimpleDateFormat("dd/MM", Locale("vi", "VN"))

    // ------------------------------------------------------------------- ghi

    /**
     * Ghi nhan [goi] nam truoc mat tu [tu] den [den].
     *
     * Goi nhieu lan cho cung mot khoang dang mo cung duoc: lan sau chi noi dai
     * khoang cu ra. Nho vay dich vu canh app khong phai nho da ghi den dau - cu
     * moi nhip lai ghi lai ca khoang tu luc bat dau den bay gio.
     */
    fun ghi(context: Context, goi: String, tu: Long, den: Long) {
        if (goi.isBlank() || den <= tu) return
        if (den - tu > DOAN_DAI_NHAT_MS) return

        val cac = doc(context).toMutableList()
        // Tim tu cuoi len: khoang dang mo gan nhu luon la mot trong vai dong cuoi.
        val cho = cac.indexOfLast { it.goi == goi && tu <= it.den + NOI_LIEN_MS && den >= it.tu }
        if (cho >= 0) {
            val cu = cac[cho]
            cac[cho] = Doan(goi, minOf(cu.tu, tu), maxOf(cu.den, den))
        } else {
            cac.add(Doan(goi, tu, den))
        }
        luu(context, cac)
    }

    /** Xoa sach, dung khi Ba Huy khong muon giu nua. */
    fun xoaHet(context: Context) {
        kho(context).edit().remove(K_DOAN).commit()
    }

    // ------------------------------------------------------------------- doc

    /**
     * Cac khoang cua mot ngay, [lui] ngay truoc hom nay (0 la hom nay).
     *
     * Khoang vat qua nua dem bi cat dung o moc giao ngay, de tong cua moi ngay
     * cong lai khong vuot qua chinh no.
     */
    fun cuaNgay(context: Context, lui: Int = 0): List<Doan> {
        val dau = mocDauNgay(lui)
        val cuoi = mocDauNgay(lui - 1)
        return doc(context)
            .filter { it.tu < cuoi && it.den > dau }
            .map { Doan(it.goi, maxOf(it.tu, dau), minOf(it.den, cuoi)) }
            .filter { it.den > it.tu }
            .sortedBy { it.tu }
    }

    /** Tung app trong ngay, app dung lau nhat dung truoc. */
    fun theoApp(context: Context, lui: Int = 0): List<MotApp> =
        cuaNgay(context, lui)
            .groupBy { it.goi }
            .map { (goi, cac) -> MotApp(goi, cac.sumOf { it.daiMs }, cac) }
            .sortedByDescending { it.tongMs }

    /**
     * Tong thoi gian may duoc dung trong ngay.
     *
     * Gop cac khoang chong nhau lai truoc khi cong: chia doi man hinh thi hai app
     * cung nam truoc mat, cong thang hai dong lai thanh ra mot tieng dung may
     * hien thanh hai tieng.
     */
    fun tongMs(context: Context, lui: Int = 0): Long =
        gopChongNhau(cuaNgay(context, lui)).sumOf { it.daiMs }

    /** Luc mo may lan dau va luc buong ra lan cuoi trong ngay, hoac null. */
    fun tuDenTrongNgay(context: Context, lui: Int = 0): Pair<Long, Long>? {
        val cac = cuaNgay(context, lui)
        if (cac.isEmpty()) return null
        return cac.first().tu to cac.maxOf { it.den }
    }

    // ---------------------------------------------------------------- chu nghia

    /** "1 tiếng 5 phút", "45 phút", "dưới 1 phút". */
    fun moTaDoDai(ms: Long): String {
        if (ms < 60_000L) return "dưới 1 phút"
        val phut = ms / 60_000L
        val gio = phut / 60
        return when {
            gio == 0L -> "$phut phút"
            phut % 60 == 0L -> "$gio tiếng"
            else -> "$gio tiếng ${phut % 60} phút"
        }
    }

    /** "14:03" */
    fun gio(luc: Long): String = dongHo.format(Date(luc))

    /** "14:03–14:25" */
    fun moTaDoan(doan: Doan): String = "${gio(doan.tu)}–${gio(doan.den)}"

    /** "hôm nay", "hôm qua", "thứ tư 16/09". */
    fun tenNgay(lui: Int): String = when (lui) {
        0 -> "hôm nay"
        1 -> "hôm qua"
        else -> {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -lui) }
            "${ThoiKhoaBieu.tenThu(cal.get(Calendar.DAY_OF_WEEK))} ${ngayThang.format(cal.time)}"
        }
    }

    /** Ten app doc duoc, hoac chinh ten goi neu app da go khoi may. */
    fun tenApp(context: Context, goi: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(goi, 0)).toString()
    }.getOrDefault(goi)

    /**
     * Ca ngay gom trong mot doan chu, cho tin nhan Telegram.
     *
     * Cat bot khi qua dai: Telegram chi nhan 4096 ky tu mot tin, ma mot ngay dung
     * may lung tung co the ra ca tram dong. [MAX_APP] app dau va [MAX_KHOANG]
     * khoang moi app la du de nhin ra buoi chieu do troi qua the nao.
     */
    fun tomTat(context: Context, lui: Int = 0, tenCon: String): String {
        val cac = theoApp(context, lui)
        val ngay = tenNgay(lui)
        if (cac.isEmpty()) {
            return "Không ghi được app nào ${ngay}. " +
                "Hoặc máy chưa được dùng, hoặc dịch vụ canh app đang tắt."
        }

        val tong = tongMs(context, lui)
        val khung = tuDenTrongNgay(context, lui)
        return buildString {
            append("📊 $tenCon dùng máy ${moTaDoDai(tong)} $ngay")
            khung?.let { append(" (từ ${gio(it.first)} đến ${gio(it.second)})") }
            append(":\n")
            cac.take(MAX_APP).forEach { app ->
                append("\n• ${tenApp(context, app.goi)} — ${moTaDoDai(app.tongMs)}\n")
                append("   ")
                append(app.cacDoan.take(MAX_KHOANG).joinToString(" · ") { moTaDoan(it) })
                if (app.cacDoan.size > MAX_KHOANG) {
                    append(" · +${app.cacDoan.size - MAX_KHOANG} lần nữa")
                }
                append("\n")
            }
            if (cac.size > MAX_APP) append("\nCòn ${cac.size - MAX_APP} app nữa, ít giờ hơn.")
        }.trim()
    }

    private const val MAX_APP = 12
    private const val MAX_KHOANG = 8

    // ------------------------------------------------------------------ ben trong

    /** Nua dem dau ngay, [lui] ngay truoc hom nay. Lui = -1 la nua dem dem nay. */
    private fun mocDauNgay(lui: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -lui)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Gop cac khoang chong hoac ke nhau thanh mot, khong phan biet app. */
    private fun gopChongNhau(cac: List<Doan>): List<Doan> {
        val ra = mutableListOf<Doan>()
        cac.sortedBy { it.tu }.forEach { doan ->
            val cuoi = ra.lastOrNull()
            if (cuoi != null && doan.tu <= cuoi.den) {
                ra[ra.lastIndex] = cuoi.copy(den = maxOf(cuoi.den, doan.den))
            } else {
                ra.add(doan)
            }
        }
        return ra
    }

    private fun doc(context: Context): List<Doan> =
        kho(context).getString(K_DOAN, "").orEmpty()
            .lineSequence()
            .mapNotNull { dong ->
                val phan = dong.split('|')
                if (phan.size != 3) return@mapNotNull null
                val tu = phan[1].toLongOrNull() ?: return@mapNotNull null
                val den = phan[2].toLongOrNull() ?: return@mapNotNull null
                Doan(phan[0], tu, den)
            }
            .toList()

    /**
     * Ghi bang apply() chu khong commit(): ham nay chay deu trong suot luc con
     * dung may, ma mat nhip cuoi cung khi tien trinh bi giet thi cung chi sai vai
     * phut cua mot khoang - y het [GioiHanApp.congThem].
     */
    private fun luu(context: Context, cac: List<Doan>) {
        val han = mocDauNgay(GIU_NGAY - 1)
        val giu = cac.asSequence()
            .filter { it.den >= han }
            .sortedBy { it.tu }
            .toList()
            .takeLast(MAX_DOAN)
        kho(context).edit()
            .putString(K_DOAN, giu.joinToString("\n") { "${it.goi}|${it.tu}|${it.den}" })
            .apply()
    }
}
