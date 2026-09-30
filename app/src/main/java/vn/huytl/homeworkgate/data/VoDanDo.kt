package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Trang vo dan do cua co giao: kieu du lieu chung, dau tich "da chup hom nay", va don ban cu.
 *
 * TU 30/9/2026 VO DAN DO CHI CON MOT VIEC LA NHAC BAI. Ba Huy bo tron goi 45 phut, va bo
 * luon ban "vo dang dung" cua ngay: truoc do may giu dung mot ban, song toi buoi hoc ke
 * tiep, ban luu sau de ban luu truoc du khac ngay. Gio moi trang da soat nam o [NhacBai],
 * moi ngay ghi tren vo mot trang, luu trung ngay thi trang sau de trang truoc. Trang nao
 * het dong de nhac thi [NhacBai] tu bo.
 *
 * MAY DOC KHONG DUOC THI CON TU GO ([NGUON_TU_GO], Ba Huy chon 30/9/2026). Truoc do con gui
 * tam anh cho Ba Huy, Ba Huy nho Claude doc tren Bang dieu khien roi dan ket qua ve bang lenh
 * DOCVO, va trong luc cho thi trang do chua nhac duoc gi. Duong do bo han, cung voi cho giu
 * tam anh cho doc o day va document hop/dando.
 *
 * VI SAO VAN CHUP, DOC RA CHU, CON SOAT LAI. May doc nham mot ngay hay bo sot mot dong thi
 * nhac sai buoi hay quen nhac; o man soat thi sua duoc bang mat.
 *
 * GIU TUNG DONG KEM MOT O TICH chu khong giu hai cuc chu "bai tap" va "viec khac".
 * Do ra tu bon trang vo that cua Le Hoa: may xep "TOÁN: làm luyện tập 3 trang 59"
 * va "CN: làm bài trang 19" vao viec khac, ma hai cai do moi la bai tap. Bat con
 * cat dan chu giua hai o de sua lai la viec vat; tich mot o thi khong.
 */
object VoDanDo {

    /**
     * Mot dong dan do.
     *
     * [laBaiTap] la o tich cua con, khong phai may quyet. [mayTich] giu lai ban may
     * doan luc dau, chi de bao cho Ba Huy biet con da sua khac cho nao. null la dong con
     * tu them vao, may khong doc ra.
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
     * Mot trang vo: khoi may doc ra, hay trang con vua bam Luu de gui Ba Huy.
     *
     * @param ngay ngay ghi TREN trang vo, dang yyyy-MM-dd. Rong la may khong doc ra ngay.
     * @param cacDong tung dong dan do cua rieng ngay do.
     * @param luc luc con bam Luu.
     */
    data class DanDo(
        val ngay: String,
        val cacDong: List<Dong>,
        val luc: Long = System.currentTimeMillis(),
        /**
         * Tam anh trang vo, moi lan chup mot file rieng trong filesDir/dando. Chi song toi
         * luc [vn.huytl.homeworkgate.telegram.DanDoSender] gui xong: [NhacBai] khong giu
         * anh, va Ba Huy da co anh trong Telegram.
         */
        val anh: String? = null,
        /** Ai ra danh sach nay: [NGUON_CON] hay [NGUON_TU_GO]. */
        val nguon: String = NGUON_CON,
        /**
         * Chi co o ban luu bang app truoc 30/9/2026: tam anh con gui cho Ba Huy doc, chua co
         * chu. [donDep] xoa no.
         */
        val chuaDoc: Boolean = false
    ) {
        /** Cac bai phai lam roi nop. */
        val cacBai: List<String> get() = cacDong.filter { it.laBaiTap }.map { it.chu }

        /** Cac dong con danh dau la khong phai bai tap. */
        val dongKhac: List<String> get() = cacDong.filterNot { it.laBaiTap }.map { it.chu }

        fun ngayDoc(): LocalDate? = runCatching { LocalDate.parse(ngay) }.getOrNull()
    }

    /** Con soat ban may doc ra. */
    const val NGUON_CON = "CON"

    /** May doc khong duoc, con nhin anh go lai tung dong. */
    const val NGUON_TU_GO = "TUGO"

    /**
     * Cho ban vo cua app truoc 30/9/2026: "vo dang dung" cua ngay, hay tam anh cho Ba Huy doc.
     * Ban moi khong ghi vao day nua, chi [donDep] doc ra de don.
     */
    private const val KHOA = "vo_dan_do"

    /** Ngay gan nhat con luu mot trang vo, dang yyyy-MM-dd. Cho dau tich o man chinh. */
    private const val KHOA_NGAY_LUU = "vo_dan_do_luu_ngay"

    /** Thu muc anh trang vo trong filesDir. */
    const val THU_MUC_ANH = "dando"

    /** Con vua luu mot trang vo (ngay nao cung duoc). */
    fun ghiDaLuu(context: Context, homNay: LocalDate = LocalDate.now()) {
        Prefs.get(context).raw().edit().putString(KHOA_NGAY_LUU, homNay.toString()).commit()
    }

    /**
     * Hom nay con da luu trang vo nao chua. Tinh theo ngay bam Luu, khong theo ngay ghi
     * tren vo: Ba Huy chon vay ngay 30/9/2026, nen hom nay chup bu vo hom kia cung tinh.
     */
    fun daLuuHomNay(context: Context, homNay: LocalDate = LocalDate.now()): Boolean =
        Prefs.get(context).raw().getString(KHOA_NGAY_LUU, null) == homNay.toString()

    /**
     * Don khi dich vu khoi dong va khi mo man vo dan do.
     *
     * BAN LUU BANG APP TRUOC 30/9/2026: ban co chu thi chep sang [NhacBai] neu ngay do chua
     * co trang; tam anh cho doc thi bo, vi khong con ai doc no nua. Ca hai deu xoa khoi cho
     * cu, ca anh.
     *
     * Roi bo cac trang [NhacBai] da het dong de nhac, va don file anh mo coi trong
     * filesDir/dando: app bi giet luc DanDoSender dang gui thi file nam lai. Chi xoa file cu
     * hon mot ngay, de khong giat file cua mot lan gui dang chay, va chi file do man vo dan
     * do dat ten ("vo-*.jpg", va "trang.jpg" cua ban cu): ManualDanDo#docAnhThat doc anh day
     * tay vao day.
     */
    fun donDep(
        context: Context,
        bayGio: LocalDateTime = LocalDateTime.now(),
        bayGioMs: Long = System.currentTimeMillis()
    ) {
        val d = docBanCu(context)
        if (d != null) {
            val ngay = d.ngayDoc()
            if (!d.chuaDoc && ngay != null && NhacBai.docTrang(context).none { it.ngay == ngay }) {
                NhacBai.ghi(context, ngay, d.cacDong, bayGio = bayGio)
            }
            d.anh?.let { runCatching { java.io.File(it).delete() } }
        }
        Prefs.get(context).raw().edit().remove(KHOA).commit()
        NhacBai.donDep(context, bayGio)
        java.io.File(context.filesDir, THU_MUC_ANH).listFiles()?.forEach { f ->
            val cuaMan = f.name == "trang.jpg" || (f.name.startsWith("vo-") && f.name.endsWith(".jpg"))
            if (cuaMan && bayGioMs - f.lastModified() > 24 * 60 * 60 * 1000L) {
                runCatching { f.delete() }
            }
        }
    }

    /** Ban cua app cu dang nam trong may, hay null. */
    fun docBanCu(context: Context): DanDo? {
        val chu = Prefs.get(context).raw().getString(KHOA, null) ?: return null
        return runCatching { tuJson(JSONObject(chu)) }.getOrNull()
    }

    /** Ghi mot ban vao cho cu. Chi test dung, de dung lai ban luu cua app truoc 30/9/2026. */
    fun ghiBanCu(context: Context, d: DanDo) {
        Prefs.get(context).raw().edit().putString(KHOA, sangJson(d).toString()).commit()
    }

    fun sangJson(d: DanDo): JSONObject = JSONObject()
        .put("ngay", d.ngay)
        .put("luc", d.luc)
        .put("anh", d.anh)
        .put("chuaDoc", d.chuaDoc)
        .put("nguon", d.nguon)
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
            anh = o.optString("anh").takeIf { it.isNotBlank() },
            nguon = o.optString("nguon").ifBlank { NGUON_CON },
            chuaDoc = o.optBoolean("chuaDoc")
        )
    }
}
