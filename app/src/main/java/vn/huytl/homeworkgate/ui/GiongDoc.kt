package vn.huytl.homeworkgate.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Giong doc tieng Anh cua man Kiem tra tu vung (Ba Huy chot 8/10/2026): cau nao may cung
 * doc ca tu bang giong Anh, va con phai nghe het moi tra loi duoc. Viec khoa nut nam o
 * [DoTuVungActivity]; lop nay chi doc va bao luc doc xong.
 *
 * DUNG BO DOC CO SAN CUA ANDROID ([TextToSpeech]) chu khong thu am tung tu: 277 tu cua bo
 * tu vung doc duoc het ma APK khong nang them. Bo doc la mot app khac (tren may ao `pad5`
 * la com.google.android.tts). Tu Android 11 app phai khai bao no trong <queries> cua
 * manifest; thieu dong do thi [TextToSpeech] bao loi ngay luc khoi dong, man chay khong
 * co tieng.
 *
 * GIONG ANH vi phien am trong bo tu ghi theo kieu Anh (68 tu co ɒ, əʊ; khong tu nao ghi
 * kieu My). May thieu giong Anh thi doc giong My; khong co tieng Anh nao thi [coTheDoc] la
 * false va man chay nhu truoc ngay 8/10, khong khoa gi (Ba Huy chot: "khong doc thi khong
 * khoa"). Ca hai truong hop ghi mot dong nhat ky moi ngay, de Ba Huy biet ma cai them giong
 * doc tren tablet.
 *
 * MOI LUOT DOC MANG MOT MA, chi luot moi nhat duoc bao xong. Doc tu moi la bo do luot cu
 * ([TextToSpeech.QUEUE_FLUSH]); bo doc bao luot cu bi dung thi bo qua, khong thi tieng doc
 * cua tu truoc mo khoa nham tu sau.
 *
 * BO DOC TREO HAY BAO LOI thi luot do coi nhu da xong ([CHO_TOI_DA_MS]), de man khong bao
 * gio ket o mot tu. Hai luot hong lien tiep thi thoi doc toi khi mo lai man: bo doc dang
 * cho mang chang han, moi tu phai cho muoi giay thi con bo cuoc truoc khi xong buoi.
 */
class GiongDoc(context: Context, private val khiDoi: () -> Unit = {}) {

    private val ct = context.applicationContext
    private val chinh = Handler(Looper.getMainLooper())

    /** Bo doc dang khoi dong (vai tram mili giay sau khi mo man): chua biet doc duoc khong. */
    private var dangKhoiDong = true
    private var docDuoc = false
    private var daTat = false
    private var loiLienTiep = 0

    /** Ma cua luot dang cho bao xong, va viec phai lam luc do. Luot moi de len luot cu. */
    private var maCho: String? = null
    private var khiXong: (() -> Unit)? = null

    /** Chu xin doc trong luc bo doc con khoi dong: khoi dong xong thi doc. */
    private var choDoc: String? = null
    private var dem = 0

    private val quaGio = Runnable { maCho?.let { xong(it, ok = false, ly = "bộ đọc không trả lời") } }

    private val nghe = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
            utteranceId?.let { chinh.post { xong(it, ok = true) } }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            utteranceId?.let { chinh.post { xong(it, ok = false, ly = "bộ đọc báo lỗi") } }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            utteranceId?.let { chinh.post { xong(it, ok = false, ly = "bộ đọc báo lỗi $errorCode") } }
        }
    }

    // Bo doc goi lai bang luong cua no, co khi goi ngay trong ham dung (may khong co bo doc
    // nao): day ve luong chinh thi [tts] da gan xong.
    private val tts = TextToSpeech(ct) { kq -> chinh.post { khoiDongXong(kq) } }

    /** Con doc duoc, hoac chua biet vi dang khoi dong. False thi man khong khoa, an nut loa. */
    val coTheDoc get() = !daTat && (dangKhoiDong || docDuoc)

    /**
     * Doc [chu]. [khiXong] chay tren luong chinh dung mot lan: khi doc het, khi bo doc loi hay
     * qua [CHO_TOI_DA_MS], va ngay lap tuc khi may khong doc duoc. Luot bi luot sau de len
     * hay bi [dung] thi khong goi.
     */
    fun doc(chu: String, khiXong: () -> Unit = {}) {
        if (!coTheDoc) return khiXong()
        chinh.removeCallbacks(quaGio)
        val ma = "tu-${++dem}"
        maCho = ma
        this.khiXong = khiXong
        tiengMinhDenLuc = SystemClock.elapsedRealtime() + CHO_TOI_DA_MS + DU_TIENG_MS
        chinh.postDelayed(quaGio, CHO_TOI_DA_MS)
        if (dangKhoiDong) {
            choDoc = chu
            return
        }
        noi(chu, ma)
    }

    /** Ngung doc ngay (con roi man, xong buoi). Luot dang cho bi bo, khong bao xong. */
    fun dung() {
        chinh.removeCallbacks(quaGio)
        maCho = null
        khiXong = null
        choDoc = null
        tiengMinhDenLuc = SystemClock.elapsedRealtime() + DU_TIENG_MS
        if (!dangKhoiDong && !daTat) runCatching { tts.stop() }
    }

    /** Tra bo doc cho he thong. Goi o onDestroy; sau do [coTheDoc] la false. */
    fun tat() {
        dung()
        daTat = true
        runCatching { tts.shutdown() }
    }

    private fun noi(chu: String, ma: String) {
        val kq = runCatching { tts.speak(chu, TextToSpeech.QUEUE_FLUSH, null, ma) }
            .getOrDefault(TextToSpeech.ERROR)
        if (kq != TextToSpeech.SUCCESS) xong(ma, ok = false, ly = "bộ đọc báo lỗi")
    }

    private fun khoiDongXong(kq: Int) {
        if (daTat) return
        dangKhoiDong = false
        val giong = if (kq == TextToSpeech.SUCCESS) chonGiong() else null
        docDuoc = giong != null
        Log.i(
            TAG,
            "giong doc: khoi dong=$kq giong=$giong bo doc=${runCatching { tts.defaultEngine }.getOrNull()}"
        )
        when {
            kq != TextToSpeech.SUCCESS -> baoMoiNgay(
                "Máy chưa đọc được từ vựng: không mở được bộ đọc giọng nói. " +
                    "Màn Kiểm tra từ vựng chạy không có tiếng."
            )
            giong == null -> baoMoiNgay(
                "Máy chưa đọc được từ vựng: bộ đọc giọng nói thiếu tiếng Anh. " +
                    "Màn Kiểm tra từ vựng chạy không có tiếng."
            )
            giong != Locale.UK -> baoMoiNgay("Máy đọc từ vựng bằng giọng Mỹ vì bộ đọc thiếu giọng Anh.")
        }
        if (docDuoc) tts.setOnUtteranceProgressListener(nghe)

        val chu = choDoc
        choDoc = null
        val ma = maCho
        if (chu != null && ma != null) {
            if (docDuoc) noi(chu, ma) else xong(ma, ok = false, ly = "không có bộ đọc")
        }
        khiDoi()
    }

    /** Giong Anh neu bo doc co, khong thi giong My; null khi bo doc khong co tieng Anh. */
    private fun chonGiong(): Locale? {
        // LANG_AVAILABLE (0) la co tieng Anh ma khong co giong cua nuoc Anh: bo doc se doc
        // giong mac dinh cua no, thuong la giong My. Tu LANG_COUNTRY_AVAILABLE moi la giong Anh.
        val anh = runCatching { tts.setLanguage(Locale.UK) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (anh >= TextToSpeech.LANG_COUNTRY_AVAILABLE) return Locale.UK
        val my = runCatching { tts.setLanguage(Locale.US) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        return if (my >= TextToSpeech.LANG_AVAILABLE) Locale.US else null
    }

    private fun xong(ma: String, ok: Boolean, ly: String = "") {
        if (ma != maCho) return
        maCho = null
        chinh.removeCallbacks(quaGio)
        tiengMinhDenLuc = SystemClock.elapsedRealtime() + DU_TIENG_MS
        val f = khiXong
        khiXong = null
        if (ok) {
            loiLienTiep = 0
        } else if (docDuoc) {
            Log.w(TAG, "giong doc: luot $ma khong xong ($ly)")
            if (++loiLienTiep >= SO_LOI_THOI_DOC) {
                docDuoc = false
                baoMoiNgay(
                    "Máy thôi đọc từ vựng giữa buổi vì $ly. Màn Kiểm tra từ vựng chạy tiếp không có tiếng."
                )
                khiDoi()
            }
        }
        f?.invoke()
    }

    /** Mot dong nhat ky cho Ba Huy, moi loi mot lan moi ngay: con mo man bao lan cung vay. */
    private fun baoMoiNgay(chu: String) {
        runCatching {
            val kho = Prefs.khoRieng(ct, KHO)
            val homNay = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val khoa = "bao_${chu.hashCode()}"
            if (kho.getString(khoa, null) == homNay) return
            kho.edit().putString(khoa, homNay).apply()
            DayLog.add(ct, chu)
        }.onFailure { Log.w(TAG, "giong doc: khong ghi duoc nhat ky: ${it.message}") }
    }

    companion object {
        private const val TAG = "HomeworkGate"

        /**
         * Mot tu doc chung mot hai giay. Qua muoi giay ma bo doc chua bao xong thi coi nhu khong
         * doc duoc va mo khoa (Ba Huy dong y ngay 8/10/2026), de man khong ket.
         */
        const val CHO_TOI_DA_MS = 10_000L

        /** Bo doc bao xong ma loa co the con keu them mot chut. */
        private const val DU_TIENG_MS = 2_000L

        /** Hong may luot lien tiep thi thoi doc toi khi mo lai man. */
        private const val SO_LOI_THOI_DOC = 2

        /** Kho prefs rieng ([Prefs.khoRieng]) giu ngay da bao loi, khong keo DongBo ghi Firestore. */
        private const val KHO = "giong_doc"

        @Volatile
        private var tiengMinhDenLuc = 0L

        /**
         * Giong doc cua app co the dang phat tieng. Tieng do do bo doc (mot app khac) phat
         * thay minh, nen [vn.huytl.homeworkgate.guard.GuardAccessibilityService] luc chua co
         * quyen doc thong bao khong phan biet duoc voi nhac nen ma phai hoi o day.
         */
        fun dangPhatTieng(): Boolean = SystemClock.elapsedRealtime() < tiengMinhDenLuc

        /**
         * Chu dua cho bo doc: bo ngoac, tach hai nghia, vi bo doc gap "(" hay "/" la doc ca dau.
         * "zoom (in / out)" thanh "zoom in, zoom out", "log (on to)" thanh "log on to",
         * "DIY (do-it-yourself)" thanh "DIY, do-it-yourself".
         */
        fun chuDoc(tu: String): String {
            val goc = tu.trim()
            val m = Regex("""^(.*?)\s*\(([^)]*)\)\s*(.*)$""").find(goc)
            val chu = if (m == null) {
                goc
            } else {
                val (truoc, trong, sau) = m.destructured
                val cac = trong.split('/').map { it.trim() }.filter { it.isNotEmpty() }
                when {
                    cac.size > 1 -> cac.joinToString(", ") { "$truoc $it $sau".trim() }
                    // Chu viet tat roi chu day du: ngat mot nhip cho de nghe.
                    truoc.matches(Regex("[A-Z]{2,}")) -> "$truoc, $trong $sau"
                    else -> "$truoc $trong $sau"
                }
            }
            return chu.replace("/", ", ").replace(Regex("\\s+"), " ").trim().trimEnd(',')
        }
    }
}
