package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import vn.huytl.homeworkgate.kho.PhamVi
import java.io.File
import java.security.SecureRandom
import java.util.Calendar
import java.util.Collections

/**
 * Cac lan nop gui khong xong. Giu lai anh de Le Hoa bam Gui lai, khong phai chup lai.
 *
 * Truoc 29/9/2026 gui hong la service xoa anh va bao chup lai. Ma gui hong co khi la hong
 * giua chung: album da len Telegram, toi tin co nut duyet thi hong. Lan 21:13 ngay
 * 28/9/2026 la vay, voi mot lan nop lai cau sai: Ba Huy thay album "Bài giải · 4 trang" ben
 * Telegram ma Bang dieu khien khong co bai nao. Ba Huy chon giu anh, co nut Gui lai, va ghi
 * loi vao nhat ky.
 *
 * Giu toi da [GIU_TOI_DA] lan, moi lan mot dong tren man chinh. Mot lan nop sau do gui duoc
 * ma la CUNG BAI (xem [cungBai]) thi bo lan hong di: con da chup lai bai do roi. Qua ngay
 * thi bo, y nhu hang cho bo bai chua duyet cua hom truoc. Bo luc nao cung ghi nhat ky.
 *
 * Nam trong file cua [Prefs] chu khong o file rieng: man chinh nghe file do de ve lai, nen
 * service vua ghi la dong "chưa gửi được" hien ngay.
 */
object BaiGuiHong {

    private const val KHOA = "bai_gui_hong"

    /** Doi mot khoa nay moi khi service gui xong, de man chinh ve lai dong "Đang gửi lại". */
    private const val KHOA_XONG = "bai_gui_hong_xong"

    private const val GIU_TOI_DA = 5

    /**
     * Tu luc bam Gui lai toi luc service nhan viec la vai phan giay. Trong khoang do dong
     * van ghi "Đang gửi lại" theo dau luc bam; qua khoang nay ma service khong nhan (chua
     * cai dat xong chang han) thi dong tro lai "chưa gửi được".
     */
    private const val CHO_SERVICE_MS = 30_000L

    /**
     * Anh dang nam trong tay service: da nhan viec, dang gui hay dang cho toi luot. Chi
     * song trong tien trinh: tien trinh chet thi lan gui cung chet theo, danh sach rong la
     * dung.
     */
    private val tepTrongTay: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    data class Lan(
        /** Ma rieng cua lan nop, giu nguyen qua cac lan gui lai. */
        val ma: String,
        /** Anh tung buoc, nam trong cacheDir cua app, van la anh goc chua thu nho. */
        val anh: Map<CaptureStage, List<File>>,
        /** [PhamVi] dang JSON. null la nop khong khai. */
        val pham: String?,
        /** Luc Le Hoa bam Gui lan dau. Gui lai van xet han vo dan do theo luc nay. */
        val lucNop: Long,
        /** Luc hong gan nhat. */
        val hongLuc: Long,
        /** So lan da gui ma hong. */
        val soLan: Int,
        /** Luc bam Gui lai gan nhat, 0 la chua bam. */
        val bamGuiLuc: Long = 0L
    ) {
        fun cacTep(): List<File> = anh.values.flatten()
    }

    /**
     * Ghi lai mot lan gui hong. Cung bo anh voi mot lan dang giu (Le Hoa bam Gui lai ma van
     * hong) thi cong them mot lan hong; bo anh moi thi them mot dong.
     */
    fun giu(
        context: Context,
        anh: Map<CaptureStage, List<File>>,
        pham: String?,
        lucNop: Long,
        now: Long = System.currentTimeMillis()
    ): Lan {
        val ds = doc(context).toMutableList()
        val tep = duongDan(anh.values.flatten())
        val i = ds.indexOfFirst { duongDan(it.cacTep()) == tep }
        val lan = if (i >= 0) {
            ds[i].copy(anh = anh, hongLuc = now, soLan = ds[i].soLan + 1, bamGuiLuc = 0L)
        } else {
            Lan(maMoi(), anh, pham, lucNop, now, soLan = 1)
        }
        if (i >= 0) ds[i] = lan else ds += lan
        // Qua nhieu thi bo lan cu nhat. Hiem ma co: phai hong sau lan lien trong mot ngay.
        while (ds.size > GIU_TOI_DA) {
            val cu = ds.removeAt(0)
            xoaAnh(cu)
            DayLog.add(context, "Bỏ ảnh bài gửi hỏng lúc ${gioPhut(cu.lucNop)} (${keAnh(context, cu.anh)}): giữ tối đa $GIU_TOI_DA lần")
        }
        ghi(context, ds)
        return lan
    }

    /**
     * Cac lan dang giu, cu truoc moi sau. Lan cua hom truoc thi bo luon o day, xoa ca anh,
     * va ghi nhat ky: sang ngay moi hang cho cung bo bai chua duyet cua hom qua.
     */
    fun cacLan(context: Context, now: Long = System.currentTimeMillis()): List<Lan> {
        val ds = doc(context)
        val (conHan, quaNgay) = ds.partition { cungNgay(it.lucNop, now) }
        if (quaNgay.isNotEmpty()) {
            quaNgay.forEach {
                xoaAnh(it)
                DayLog.add(context, "Bỏ ảnh bài gửi hỏng lúc ${gioPhut(it.lucNop)} hôm trước (${keAnh(context, it.anh)})")
            }
            ghi(context, conHan)
        }
        return conHan
    }

    fun lay(context: Context, ma: String, now: Long = System.currentTimeMillis()): Lan? =
        cacLan(context, now).firstOrNull { it.ma == ma }

    /**
     * Lan nay dang o trong tay service, hay vua bam Gui lai ma service chua kip nhan. Dang
     * gui thi khong cho bam Gui lai hay Bo anh: hai lan gui cung mot bo anh la mot bai vao
     * hang hai lan, con bo anh giua chung thi lan dang gui hong vi mat anh.
     */
    fun dangGui(lan: Lan, now: Long = System.currentTimeMillis()): Boolean =
        dangTrongTay(lan.cacTep()) || (lan.bamGuiLuc > 0L && now - lan.bamGuiLuc < CHO_SERVICE_MS)

    /** Con bao lau thi het khoang cho service nhan viec, de man chinh hen ve lai. */
    fun conChoService(lan: Lan, now: Long = System.currentTimeMillis()): Long =
        if (lan.bamGuiLuc > 0L) (lan.bamGuiLuc + CHO_SERVICE_MS - now).coerceAtLeast(0L) else 0L

    fun danhDauBamGui(context: Context, ma: String, now: Long = System.currentTimeMillis()) {
        val ds = doc(context)
        ghi(context, ds.map { if (it.ma == ma) it.copy(bamGuiLuc = now) else it })
    }

    /**
     * Bo anh da mat khoi mot lan (he dieu hanh don cacheDir luc may day bo nho). De bai la
     * buoc khong bat buoc nen mat thi thoi; bai giai mat trang nao la khong gui lai duoc,
     * tra null. Luu lai ban da bo, de lan gui lai xong khop dung bo anh.
     */
    fun lamGon(context: Context, ma: String): Lan? {
        val lan = doc(context).firstOrNull { it.ma == ma } ?: return null
        val giai = lan.anh[CaptureStage.BAI_GIAI].orEmpty()
        if (giai.isEmpty() || giai.any { !it.exists() || it.length() == 0L }) return null
        val con = lan.anh.mapValues { (_, f) -> f.filter { it.exists() && it.length() > 0 } }
            .filterValues { it.isNotEmpty() }
        if (con == lan.anh) return lan
        val moi = lan.copy(anh = con)
        ghi(context, doc(context).map { if (it.ma == ma) moi else it })
        return moi
    }

    /** Ket qua mot lan gui xong: co phai gui lai lan dang giu, va nhung lan nao bo di. */
    data class Xong(val guiLai: Lan?, val boDi: List<Lan>)

    /**
     * Mot lan nop vua gui xong. Dung bo anh cua mot lan dang giu thi go lan do ([Xong.guiLai]).
     * Lan nao dang giu ma cung bai voi lan vua xong ([cungBai]) thi bo, xoa anh, ghi nhat ky
     * ([Xong.boDi]). Anh cua lan vua xong thi ben goi xoa nhu moi lan gui xong.
     */
    fun xong(
        context: Context,
        tep: Collection<File>,
        pham: PhamVi?,
        now: Long = System.currentTimeMillis()
    ): Xong {
        val ds = doc(context)
        if (ds.isEmpty()) return Xong(null, emptyList())
        val bo = duongDan(tep)
        val guiLai = ds.firstOrNull { duongDan(it.cacTep()) == bo }
        val boDi = ds.filter { it !== guiLai && cungBai(PhamVi.tuJson(it.pham), pham) }
        if (guiLai == null && boDi.isEmpty()) return Xong(null, emptyList())
        boDi.forEach {
            xoaAnh(it)
            DayLog.add(context, "Bỏ ảnh bài gửi hỏng lúc ${gioPhut(it.lucNop)} (${keAnh(context, it.anh)}): đã nộp lại lúc ${gioPhut(now)}")
        }
        ghi(context, ds.filter { it !== guiLai && it !in boDi })
        return Xong(guiLai, boDi)
    }

    /** Bo mot lan: xoa anh va xoa dong giu. */
    fun bo(context: Context, ma: String) {
        val ds = doc(context)
        ds.filter { it.ma == ma }.forEach { xoaAnh(it) }
        ghi(context, ds.filter { it.ma != ma })
    }

    /** Service vua nhan mot lan gui. Xem [dangGui]. */
    fun nhanGui(tep: Collection<File>) {
        tepTrongTay.addAll(tep.map { it.absolutePath })
    }

    /** Service gui xong (duoc hay hong). Doi mot khoa de man chinh ve lai. */
    fun traGui(context: Context, tep: Collection<File>) {
        tepTrongTay.removeAll(tep.map { it.absolutePath }.toSet())
        Prefs.get(context).raw().edit().putLong(KHOA_XONG, System.currentTimeMillis()).apply()
    }

    /**
     * Hai lan nop co phai cung mot bai khong: cung nop lai cau sai cua mot bai, cung phan
     * tu luan cua mot de, hay cung mon va cung danh sach cau khai. Khong khai thi khong biet,
     * coi nhu khac: giu lai thua con hon bo nham bai cua con.
     */
    fun cungBai(a: PhamVi?, b: PhamVi?): Boolean {
        if (a == null || b == null) return false
        if (a.laSua || b.laSua) return a.suaBai == b.suaBai
        if (a.giaiDe.isNotBlank() || b.giaiDe.isNotBlank()) return a.giaiDe == b.giaiDe
        return a.cauIds.isNotEmpty() && a.mon == b.mon && a.cauIds.toSet() == b.cauIds.toSet()
    }

    /** "đề bài 5, bài giải 4", dung cach ke cua dong "Nộp bài" trong nhat ky. */
    fun keAnh(context: Context, anh: Map<CaptureStage, List<File>>): String =
        anh.entries.joinToString(", ") { (st, files) ->
            "${context.getString(st.labelRes).lowercase()} ${files.size}"
        }

    fun gioPhut(luc: Long): String =
        java.text.SimpleDateFormat("HH:mm", java.util.Locale("vi", "VN")).format(java.util.Date(luc))

    private fun dangTrongTay(tep: List<File>): Boolean =
        tep.isNotEmpty() && tep.all { it.absolutePath in tepTrongTay }

    private fun xoaAnh(lan: Lan) = lan.cacTep().forEach { runCatching { it.delete() } }

    private fun duongDan(tep: Collection<File>): Set<String> = tep.map { it.absolutePath }.toSet()

    private fun maMoi(): String {
        val b = ByteArray(6).also { SecureRandom().nextBytes(it) }
        return b.joinToString("") { "%02x".format(it) }
    }

    private fun cungNgay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun ghi(context: Context, ds: List<Lan>) {
        val sua = Prefs.get(context).raw().edit()
        if (ds.isEmpty()) {
            sua.remove(KHOA).commit()
            return
        }
        val mang = JSONArray()
        ds.forEach { lan ->
            val anh = JSONObject()
            lan.anh.forEach { (st, files) ->
                anh.put(st.name, JSONArray().apply { files.forEach { put(it.absolutePath) } })
            }
            val o = JSONObject()
                .put("ma", lan.ma)
                .put("anh", anh)
                .put("lucNop", lan.lucNop)
                .put("hongLuc", lan.hongLuc)
                .put("soLan", lan.soLan)
                .put("bamGuiLuc", lan.bamGuiLuc)
            lan.pham?.let { o.put("pham", it) }
            mang.put(o)
        }
        sua.putString(KHOA, mang.toString()).commit()
    }

    private fun doc(context: Context): List<Lan> {
        val chu = Prefs.get(context).raw().getString(KHOA, null) ?: return emptyList()
        val mang = runCatching { JSONArray(chu) }.getOrNull() ?: return emptyList()
        return (0 until mang.length()).mapNotNull { i ->
            runCatching {
                val o = mang.getJSONObject(i)
                val anhJson = o.getJSONObject("anh")
                // Theo thu tu enum, y nhu luc chup: de bai truoc, bai giai sau.
                val anh = CaptureStage.entries.associateWith { st ->
                    val f = anhJson.optJSONArray(st.name) ?: JSONArray()
                    (0 until f.length()).map { File(f.getString(it)) }
                }.filterValues { it.isNotEmpty() }
                Lan(
                    ma = o.getString("ma"),
                    anh = anh,
                    pham = o.optString("pham").takeIf { it.isNotBlank() },
                    lucNop = o.optLong("lucNop"),
                    hongLuc = o.optLong("hongLuc"),
                    soLan = o.optInt("soLan", 1),
                    bamGuiLuc = o.optLong("bamGuiLuc")
                )
            }.getOrNull()
        }
    }
}
