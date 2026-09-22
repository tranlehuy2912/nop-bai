package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Trang vo dan do cua co giao, da doc ra chu va da duoc Le Hoa soat lai.
 *
 * VI SAO TACH RA KHOI LAN NOP BAI. Truoc day trang vo dan do la mot trong ba buoc
 * chup moi lan nop, va con phai chup lai no o TUNG lan nop trong ngay. Chup mot lan
 * thi hong: thieu trang vo, may khong biet cau nao thuoc bai co giao nen coi tat ca
 * la trong tron goi - xem quy tac 17 trong [vn.huytl.homeworkgate.ai.PromptCham] va
 * cach [LuatCongGio] dung trongDanDo - va lan nop thu hai trong ngay khong duoc phut
 * nao.
 *
 * Nen doi cho: chup mot lan, doc ra CHU, con soat lai bang mat roi luu. Cac lan nop
 * sau khong hoi lai trang vo nua, chi kem doan chu do vao cau lenh cham.
 *
 * GIU TUNG DONG KEM MOT O TICH chu khong giu hai cuc chu "bai tap" va "viec khac".
 * Do ra tu bon trang vo that cua Le Hoa: may xep "TOÁN: làm luyện tập 3 trang 59"
 * va "CN: làm bài trang 19" vao viec khac, ma hai cai do moi la bai tap. Bat con
 * cat dan chu giua hai o de sua lai la viec vat; tich mot o thi khong.
 *
 * HAN DUNG DI THEO [LuatCongGio.ngayDanDoHopLe] chu khong tu dat mot cai khac: doan
 * chu nay thay cho tam anh, nen no phai het hieu luc dung luc tam anh do het.
 */
object VoDanDo {

    /**
     * Mot dong dan do.
     *
     * [laBaiTap] la o tich cua con, khong phai may quyet. [mayTich] giu lai ban may
     * doan luc dau, chi de bao cho Ba Huy biet con da sua khac cho nao - o tich doi
     * thang ra gio choi nen cho nao con sua thi ba nen liec qua. null la dong con tu
     * them vao, may khong doc ra.
     */
    data class Dong(
        val chu: String,
        val laBaiTap: Boolean,
        val mayTich: Boolean? = null
    ) {
        /** Con quyet khac may. Dong tu them khong tinh la sua. */
        val conSua: Boolean get() = mayTich != null && mayTich != laBaiTap
    }

    /**
     * @param ngay ngay ghi TREN trang vo, dang yyyy-MM-dd.
     * @param cacDong tung dong dan do cua rieng ngay do.
     * @param luc luc con bam Luu.
     */
    data class DanDo(
        val ngay: String,
        val cacDong: List<Dong>,
        val luc: Long = System.currentTimeMillis(),
        /**
         * Tam anh trang vo, giu lai ca ngay.
         *
         * Giu chu khong xoa sau khi gui: con vao sua lai ban da luu thi tin gui cho
         * Ba Huy lan hai van phai co anh, neu khong ba chi nhan duoc mot danh sach
         * chu khong doi chieu duoc voi cai gi.
         */
        val anh: String? = null
    ) {
        /** Cac bai phai lam roi nop. Day la thu di vao cau lenh cham. */
        val cacBai: List<String> get() = cacDong.filter { it.laBaiTap }.map { it.chu }

        /** Cac dong con danh dau la khong phai bai tap. */
        val dongKhac: List<String> get() = cacDong.filterNot { it.laBaiTap }.map { it.chu }

        fun ngayDoc(): LocalDate? = runCatching { LocalDate.parse(ngay) }.getOrNull()

        /** Mot dong cho man hinh: "14/9 · 2 bài". */
        fun moTa(): String {
            val d = ngayDoc()
            val phanNgay = if (d != null) "${d.dayOfMonth}/${d.monthValue}" else ngay
            val soBai = cacBai.size
            return "$phanNgay · " + if (soBai == 0) "không có bài tập" else "$soBai bài"
        }
    }

    private const val KHOA = "vo_dan_do"

    /** Ban dang con hieu luc, hay null neu chua chup hoac ngay da qua han. */
    fun conHieuLuc(
        context: Context,
        bayGio: LocalDateTime = LocalDateTime.now()
    ): DanDo? {
        val d = doc(context) ?: return null
        return if (LuatCongGio.ngayDanDoHopLe(d.ngayDoc(), bayGio)) d else null
    }

    /** Ban dang nam trong may, khong hoi han. Cho man soat de hien lai cai vua luu. */
    fun doc(context: Context): DanDo? {
        val chu = Prefs.get(context).raw().getString(KHOA, null) ?: return null
        return runCatching { tuJson(JSONObject(chu)) }.getOrNull()
    }

    fun luu(context: Context, d: DanDo) {
        Prefs.get(context).raw().edit().putString(KHOA, sangJson(d).toString()).commit()
    }

    /**
     * Xoa ban dang co, ke ca tam anh.
     *
     * Anh phai di theo ban ghi chu khong o lai: no la anh mot trang vo cua tre con,
     * va app nay da chon phia can than o moi cho khac - tin nhan thi cat trong cho
     * da ma hoa, khung chat chi giu mot tram cau. Giu mot tam anh khong ai con doc
     * nua thi khong co ly do gi bien ho duoc.
     */
    fun xoa(context: Context) {
        doc(context)?.anh?.let { runCatching { java.io.File(it).delete() } }
        Prefs.get(context).raw().edit().remove(KHOA).commit()
    }

    /**
     * Don ban da qua han. Goi luc dich vu khoi dong, y het [ChatBox.donDep].
     *
     * Het han la het duong dung: [conHieuLuc] khong tra no ra nua, tin cho Ba Huy
     * cung khong kem no nua. De nam lai thi mot trang vo cua thu Hai van con trong
     * may vao thu Sau ma khong ai mo, va man soat mo ra lai bay ra dan do cua tuan
     * truoc trong khi man chon mon dang ghi "Chụp vở dặn dò hôm nay".
     */
    fun donDep(context: Context, bayGio: LocalDateTime = LocalDateTime.now()) {
        val d = doc(context) ?: return
        if (!LuatCongGio.ngayDanDoHopLe(d.ngayDoc(), bayGio)) xoa(context)
    }

    fun sangJson(d: DanDo): JSONObject = JSONObject()
        .put("ngay", d.ngay)
        .put("luc", d.luc)
        .put("anh", d.anh)
        .put(
            "dong",
            JSONArray().apply {
                d.cacDong.forEach {
                    put(
                        JSONObject()
                            .put("chu", it.chu)
                            .put("bai", it.laBaiTap)
                            .put("may", it.mayTich ?: JSONObject.NULL)
                    )
                }
            }
        )

    fun tuJson(o: JSONObject): DanDo {
        val mang = o.optJSONArray("dong") ?: JSONArray()
        return DanDo(
            ngay = o.getString("ngay"),
            cacDong = (0 until mang.length()).map { i ->
                val x = mang.getJSONObject(i)
                Dong(
                    chu = x.getString("chu"),
                    laBaiTap = x.optBoolean("bai"),
                    mayTich = if (x.isNull("may")) null else x.optBoolean("may")
                )
            },
            luc = o.optLong("luc"),
            anh = o.optString("anh").takeIf { it.isNotBlank() }
        )
    }
}
