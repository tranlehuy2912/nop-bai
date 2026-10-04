package vn.huytl.homeworkgate.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.time.LocalDate

/**
 * Mot ban thoi khoa bieu kem ngay nghi, doc tu JSON.
 *
 * TU 4/10/2026 LICH KHONG VIET THANG VAO CODE NUA. Mot thang lich doi ba lan (7/9, bo AVNN
 * 27/9, xep lai ca sau buoi chieu 5/10), lan nao cung phai build va cai lai hai app. Ba Huy
 * chon: lich nam o Firestore, document `lichhoc/{maNha}`, truong [Duong.F_JSON] la mot chuoi
 * JSON theo khuon cua [LichMacDinh]. Claude Code ghi vao do qua Firebase console moi lan
 * truong doi lich, hai app chi doc. Luat Firestore khong cho may nao ghi, ke ca tablet: Le
 * Hoa khong sua duoc lich de khoi bi chan.
 *
 * [doc] kiem ky truoc khi nhan, vi mot ban sai lot qua la man chan sai gio. Ban hong thi nem
 * [IllegalArgumentException] ghi ro cho hong, va app giu ban dang dung.
 *
 * Bang dieu khien giu mot ban chep y het file nay, chi khac dong package.
 */
data class BanLich(
    /** Tang len moi lan doi lich. Co hai ban thi app dung ban so cao hon. */
    val phienBan: Int,
    val ghiChu: String,
    val lop: String,
    val phutMoiTiet: Int,
    /** Buoi -> tiet -> phut tinh tu 00:00 luc vao tiet. */
    val gioTiet: Map<Buoi, Map<Int, Int>>,
    val buongMayChieu: Int,
    /** Thu (hang so Calendar) -> moc buong may buoi sang. Thu khong co thi 30 phut truoc gio vao hoc. */
    val buongMaySang: Map<Int, Int>,
    /** Buoi -> thu (hang so Calendar) -> tiet -> ten mon. Chi chua nhung tiet co hoc. */
    val monTheoBuoi: Map<Buoi, Map<Int, Map<Int, String>>>,
    val cacKyNghi: List<KyNghi>,
    /** Ngay hoc cuoi cung, so yyyyMMdd. */
    val hetNamHoc: Int
) {
    /** Hai dau ngay la so yyyyMMdd, tinh ca hai dau. */
    data class KyNghi(val tu: Int, val den: Int, val ten: String)

    companion object {

        private val KHOA_GOC = setOf(
            "phienBan", "ghiChu", "lop", "phutMoiTiet", "gioTiet", "buongMay",
            "sang", "chieu", "ngayNghi", "hetNamHoc"
        )

        /** Ten buoi trong JSON. */
        private val TEN_BUOI = mapOf("sang" to Buoi.SANG, "chieu" to Buoi.CHIEU)

        /**
         * Doc va kiem mot ban. Hong thi nem [IllegalArgumentException], cau loi mo dau bang
         * cho hong ("chieu.3.6: ...") de biet sua dau.
         */
        fun doc(json: String): BanLich {
            val goc = try {
                JSONObject(json)
            } catch (e: Exception) {
                hong("JSON", "không đọc được (${e.message})")
            }
            khongThua(goc, KHOA_GOC, "")

            val phienBan = soNguyen(goc, "phienBan", "")
            if (phienBan < 1) hong("phienBan", "phải từ 1 trở lên")
            val phutMoiTiet = soNguyen(goc, "phutMoiTiet", "")
            if (phutMoiTiet !in 10..120) hong("phutMoiTiet", "phải trong khoảng 10 tới 120")

            val khoiGio = khoi(goc, "gioTiet", "")
            khongThua(khoiGio, TEN_BUOI.keys, "gioTiet.")
            val gioTiet = TEN_BUOI.entries.associate { (ten, buoi) ->
                buoi to docGioTiet(khoi(khoiGio, ten, "gioTiet."), "gioTiet.$ten", phutMoiTiet)
            }

            val monTheoBuoi = TEN_BUOI.entries.associate { (ten, buoi) ->
                buoi to docBuoi(khoi(goc, ten, ""), ten, gioTiet.getValue(buoi))
            }
            if (monTheoBuoi.values.all { it.isEmpty() }) hong("sang, chieu", "cả tuần không có buổi học nào")

            val khoiBuong = khoi(goc, "buongMay", "")
            khongThua(khoiBuong, setOf("sang", "chieu"), "buongMay.")
            val buongMayChieu = gio(chu(khoiBuong, "chieu", "buongMay."), "buongMay.chieu")
            monTheoBuoi.getValue(Buoi.CHIEU).forEach { (thu, mon) ->
                val vaoHoc = gioTiet.getValue(Buoi.CHIEU).getValue(mon.keys.min())
                if (buongMayChieu >= vaoHoc) hong("buongMay.chieu", "phải trước giờ vào học chiều thứ $thu")
            }
            val khoiSang = khoi(khoiBuong, "sang", "buongMay.")
            val buongMaySang = khoiSang.keys().asSequence().associate { k ->
                val cho = "buongMay.sang.$k"
                val thu = thu(k, cho)
                val moc = gio(chu(khoiSang, k, "buongMay.sang."), cho)
                val buoi = monTheoBuoi.getValue(Buoi.SANG)[thu]
                    ?: hong(cho, "thứ $thu không học buổi sáng")
                if (moc >= gioTiet.getValue(Buoi.SANG).getValue(buoi.keys.min())) {
                    hong(cho, "phải trước giờ vào học")
                }
                thu to moc
            }

            val dsNghi = goc.optJSONArray("ngayNghi") ?: hong("ngayNghi", "thiếu hoặc không phải danh sách [...]")
            val cacKyNghi = (0 until dsNghi.length()).map { i ->
                val cho = "ngayNghi.$i"
                val k = dsNghi.optJSONObject(i) ?: hong(cho, "không phải khối {...}")
                khongThua(k, setOf("tu", "den", "ten"), "$cho.")
                val tu = ngay(chu(k, "tu", "$cho."), "$cho.tu")
                val den = ngay(chu(k, "den", "$cho."), "$cho.den")
                if (den < tu) hong(cho, "ngày kết thúc trước ngày bắt đầu")
                KyNghi(tu, den, chu(k, "ten", "$cho."))
            }

            return BanLich(
                phienBan = phienBan,
                ghiChu = goc.optString("ghiChu", ""),
                lop = chu(goc, "lop", ""),
                phutMoiTiet = phutMoiTiet,
                gioTiet = gioTiet,
                buongMayChieu = buongMayChieu,
                buongMaySang = buongMaySang,
                monTheoBuoi = monTheoBuoi,
                cacKyNghi = cacKyNghi,
                hetNamHoc = ngay(chu(goc, "hetNamHoc", ""), "hetNamHoc")
            )
        }

        /** Gio vao tung tiet. Tiet danh so tu 1 lien nhau, tiet sau khong vao truoc khi tiet truoc het. */
        private fun docGioTiet(o: JSONObject, cho: String, phutMoiTiet: Int): Map<Int, Int> {
            val gio = o.keys().asSequence().associate { k ->
                val tiet = k.toIntOrNull()?.takeIf { it in 1..10 } ?: hong("$cho.$k", "tiết phải là số từ 1 tới 10")
                tiet to gio(chu(o, k, "$cho."), "$cho.$k")
            }.toSortedMap()
            if (gio.isEmpty()) hong(cho, "không có tiết nào")
            if (gio.keys.toList() != (1..gio.size).toList()) hong(cho, "tiết phải đánh số liền nhau từ 1")
            gio.entries.zipWithNext().forEach { (truoc, sau) ->
                if (sau.value < truoc.value + phutMoiTiet) {
                    hong("$cho.${sau.key}", "vào trước khi tiết ${truoc.key} hết")
                }
            }
            return gio
        }

        /** Mon tung tiet cua mot buoi trong ca tuan. Thu khong co la khong hoc buoi do. */
        private fun docBuoi(o: JSONObject, tenBuoi: String, gioTiet: Map<Int, Int>): Map<Int, Map<Int, String>> =
            o.keys().asSequence().associate { k ->
                val cho = "$tenBuoi.$k"
                val thu = thu(k, cho)
                val ngay = o.optJSONObject(k) ?: hong(cho, "không phải khối {...}")
                val mon = ngay.keys().asSequence().associate { t ->
                    val tiet = t.toIntOrNull() ?: hong("$cho.$t", "tiết phải là số")
                    if (tiet !in gioTiet) hong("$cho.$t", "buổi $tenBuoi không có tiết $tiet trong gioTiet")
                    tiet to chu(ngay, t, "$cho.")
                }
                if (mon.isEmpty()) hong(cho, "không có tiết nào, bỏ hẳn thứ này đi nếu không học")
                thu to mon
            }

        /** Thu viet bang so nhu tren thoi khoa bieu: "2" la thu hai. Trung luon hang so Calendar. */
        private fun thu(k: String, cho: String): Int =
            k.toIntOrNull()?.takeIf { it in 2..7 } ?: hong(cho, "thứ phải là số từ 2 tới 7")

        private fun gio(s: String, cho: String): Int {
            val m = Regex("""(\d{2}):(\d{2})""").matchEntire(s) ?: hong(cho, "\"$s\" không phải giờ dạng 07:15")
            val (h, p) = m.destructured
            if (h.toInt() > 23 || p.toInt() > 59) hong(cho, "\"$s\" không phải giờ có thật")
            return h.toInt() * 60 + p.toInt()
        }

        private fun ngay(s: String, cho: String): Int {
            val d = try {
                LocalDate.parse(s)
            } catch (e: Exception) {
                hong(cho, "\"$s\" không phải ngày dạng 2027-01-31")
            }
            return d.year * 10_000 + d.monthValue * 100 + d.dayOfMonth
        }

        private fun khoi(o: JSONObject, ten: String, cho: String): JSONObject =
            o.optJSONObject(ten) ?: hong("$cho$ten", "thiếu hoặc không phải khối {...}")

        private fun chu(o: JSONObject, ten: String, cho: String): String =
            (o.opt(ten) as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: hong("$cho$ten", "thiếu hoặc không phải chữ")

        private fun soNguyen(o: JSONObject, ten: String, cho: String): Int =
            o.opt(ten) as? Int ?: hong("$cho$ten", "thiếu hoặc không phải số nguyên")

        /** Khoa la thi bao: go sai mot ten khoa thi thanh thieu mot phan lich ma khong ai hay. */
        private fun khongThua(o: JSONObject, duoc: Set<String>, cho: String) {
            o.keys().asSequence().firstOrNull { it !in duoc }?.let { hong("$cho$it", "khoá lạ, không biết dùng vào đâu") }
        }

        private fun hong(cho: String, viSao: String): Nothing =
            throw IllegalArgumentException("$cho: $viSao")
    }
}

/**
 * Ban lich ca app dang dung, va cho giu ban nhan tu Firestore.
 *
 * Moi cho can lich deu di qua [ThoiKhoaBieu] va [NgayNghi], hai object do doc [ban].
 *
 * Ban nhan tu Firestore luu vao mot file prefs thuong (lich khong co gi bi mat), de may mat
 * mang hay vua bat lai van dung ban moi nhat da nhan. Giua ban luu va [LichMacDinh] thi lay
 * ban [BanLich.phienBan] cao hon: APK moi mang ban mac dinh moi hon thi khong bi mot ban luu
 * cu de len.
 */
object LichDangDung {

    private const val TAG = "LichDangDung"
    private const val FILE = "lich_hoc"
    private const val K_JSON = "json"

    /**
     * Ban mau cua ban thu tu dong tren may ao (ManualBang lichmau). Co khoa nay thi dung no,
     * khong so phien ban, khong nhan ban tu Firestore: man hinh can soat phai dung yen khi
     * lich that doi. Tablet that khong bao gio co khoa nay.
     */
    private const val K_EP = "json_ep"

    @Volatile
    var ban: BanLich = BanLich.doc(LichMacDinh.JSON)
        private set

    sealed interface KetQua {
        data class Doi(val ban: BanLich) : KetQua
        data object GiuNguyen : KetQua
        /** Ban nhan duoc co phien ban thap hon ban dang dung. */
        data class CuHon(val ban: BanLich) : KetQua
        data class Hong(val loi: String) : KetQua
    }

    /** Goi mot lan luc mo app, truoc khi dung lich. */
    fun napTuMay(context: Context) {
        val f = file(context)
        val ep = f.getString(K_EP, null)?.let(::docAnToan)
        if (ep != null) {
            ban = ep
            return
        }
        val luu = f.getString(K_JSON, null)?.let(::docAnToan) ?: return
        if (luu.phienBan >= ban.phienBan) ban = luu
    }

    /** Nhan noi dung truong [Duong.F_JSON] vua doc tu Firestore. */
    fun nhan(context: Context, json: String): KetQua {
        val moi = try {
            BanLich.doc(json)
        } catch (e: IllegalArgumentException) {
            return KetQua.Hong(e.message.orEmpty())
        }
        val f = file(context)
        if (f.contains(K_EP)) return KetQua.GiuNguyen
        if (moi.phienBan < ban.phienBan) return KetQua.CuHon(moi)
        if (moi == ban) return KetQua.GiuNguyen
        f.edit().putString(K_JSON, json).apply()
        ban = moi
        return KetQua.Doi(moi)
    }

    /** Chi ban thu tren may ao: dung [json] bat ke phien ban, giu qua cac lan mo app. Null la bo. */
    fun epTam(context: Context, json: String?) {
        val f = file(context)
        if (json == null) {
            f.edit().remove(K_EP).commit()
            ban = BanLich.doc(LichMacDinh.JSON)
            napTuMay(context)
            return
        }
        val b = BanLich.doc(json)
        f.edit().putString(K_EP, json).commit()
        ban = b
    }

    /** Chi cho test: dung [b] trong tien trinh nay, khong ghi gi xuong may. */
    fun dungTam(b: BanLich) {
        ban = b
    }

    private fun docAnToan(json: String): BanLich? = try {
        BanLich.doc(json)
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "ban lich luu trong may hong, bo qua: ${e.message}")
        null
    }

    private fun file(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
