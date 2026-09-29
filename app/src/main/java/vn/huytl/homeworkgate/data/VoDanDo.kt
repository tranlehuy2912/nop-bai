package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Trang vo dan do cua co giao, da doc ra chu va da duoc Le Hoa soat lai.
 *
 * TU 30/9/2026 VO KHONG CON DUNG DE TINH GIO. Ba Huy bo tron goi 45 phut, bai chup anh chi
 * con tinh le, nen Claude cham bai khong can vo nua. Vo con hai viec: nhac bai theo tung mon
 * ([NhacBai]), va bao truoc lan kiem tra de Giai de mo de on ([GiaiDe]). Le Hoa hoc xong thi
 * ghi vo cua ngay hom do; co dan gi la cho buoi hoc ke tiep cua mon do.
 *
 * VI SAO VAN CHUP MOT LAN, DOC RA CHU, CON SOAT LAI. May doc nham mot ngay hay bo sot mot
 * dong thi nhac sai buoi hay quen nhac; o man soat thi sua duoc bang mat.
 *
 * GIU TUNG DONG KEM MOT O TICH chu khong giu hai cuc chu "bai tap" va "viec khac".
 * Do ra tu bon trang vo that cua Le Hoa: may xep "TOÁN: làm luyện tập 3 trang 59"
 * va "CN: làm bài trang 19" vao viec khac, ma hai cai do moi la bai tap. Bat con
 * cat dan chu giua hai o de sua lai la viec vat; tich mot o thi khong.
 *
 * BAN NAY SONG TOI LUC VAO BUOI HOC KE TIEP ([conDung]). Qua luc do thi vo cua buoi moi
 * moi la vo can chup, va man chinh lai hien "Chụp vở dặn dò hôm nay". Chu cua tung dong thi
 * [NhacBai] giu rieng, toi tiet sau cua dung mon do.
 *
 * MAY DOC HONG THI VAN GIU TAM ANH ([DanDo.chuaDoc]). Con gui tam anh cho Ba Huy, va Ba
 * Huy nho Claude doc tren Bang dieu khien.
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
        val anh: String? = null,
        /**
         * Ma cua tam anh trang vo tren Telegram, co sau khi [vn.huytl.homeworkgate
         * .telegram.DanDoSender] gui xong. null la chua gui duoc.
         *
         * Bang dieu khien cam ma nay tai lai dung tam anh do khi Ba Huy nho Claude doc
         * mot trang may doc khong duoc - xem [vn.huytl.homeworkgate.dongbo.DongBo.banDanDo].
         */
        val fileId: String? = null,
        /**
         * Chi co anh, chua ai doc ra chu: may doc khong duoc va con bam "Gửi ảnh để ba
         * Huy đọc". Luc do [cacDong] rong nhung KHONG co nghia la co khong giao bai tap,
         * ma la chua biet. Ngay tam la ngay chup, doc xong se thay bang ngay ghi tren vo.
         *
         * Hai duong doc ra chu: con bam "Thử đọc lại" khi may doc duoc tro lai, hay Ba
         * Huy nho Claude doc ([tuClaude]).
         */
        val chuaDoc: Boolean = false,
        /** Ai doc ra danh sach nay: [NGUON_CON], [NGUON_CLAUDE] hay [NGUON_LUC_CHAM]. */
        val nguon: String = NGUON_CON,
        /**
         * Luc chup tam anh trang vo. Doc lai hay sua chu thi giu nguyen, chi doi khi
         * con chup tam khac.
         *
         * Dung de biet hai ban co cung mot tam anh khong: ket qua Claude doc den muon
         * phai dung tam anh dang giu ([tuClaude]), va con sua ngay cua trang da luu thi
         * [NhacBai] thay dung trang cu.
         */
        val chupLuc: Long = luc
    ) {
        /** Cac bai phai lam roi nop. */
        val cacBai: List<String> get() = cacDong.filter { it.laBaiTap }.map { it.chu }

        /** Cac dong con danh dau la khong phai bai tap. */
        val dongKhac: List<String> get() = cacDong.filterNot { it.laBaiTap }.map { it.chu }

        fun ngayDoc(): LocalDate? = runCatching { LocalDate.parse(ngay) }.getOrNull()

        /** Mot dong cho man hinh: "14/9 · 2 bài". */
        fun moTa(): String {
            val d = ngayDoc()
            val phanNgay = if (d != null) "${d.dayOfMonth}/${d.monthValue}" else ngay
            if (chuaDoc) return "$phanNgay · chờ ba Huy đọc"
            val soBai = cacBai.size
            return "$phanNgay · " + if (soBai == 0) "không có bài tập" else "$soBai bài"
        }

        /** Ai dung sau danh sach nay, cho dong chu gui Ba Huy: "Lê Hòa đã soát". */
        fun aiDoc(con: String): String = when (nguon) {
            NGUON_CLAUDE -> "Claude đọc"
            NGUON_LUC_CHAM -> "đọc lúc chấm bài"
            else -> "$con đã soát"
        }
    }

    /** Con soat ban may doc ra, hay tu go lai. */
    const val NGUON_CON = "CON"

    /** Claude doc, Ba Huy dan ket qua tu Bang dieu khien ve (lenh DOCVO). */
    const val NGUON_CLAUDE = "CLAUDE"

    /**
     * Doc ra o lan cham bai dau tien, tu tam anh trang vo gan theo bai. Chi con o ban luu
     * truoc 30/9/2026: tu do cham bai khong doc vo nua.
     */
    const val NGUON_LUC_CHAM = "LUCCHAM"

    private const val KHOA = "vo_dan_do"

    /** Ban dang con hieu luc, hay null neu chua chup hoac da vao buoi hoc sau. */
    fun conHieuLuc(
        context: Context,
        bayGio: LocalDateTime = LocalDateTime.now()
    ): DanDo? {
        val d = doc(context) ?: return null
        return if (conDung(d.ngayDoc(), bayGio)) d else null
    }

    /**
     * Vo ghi ngay [ngay] con la vo cua buoi vua hoc khong: tu ngay do toi luc vao buoi hoc
     * ke tiep, theo thoi khoa bieu va ngay nghi.
     *
     * Thay cho luat cu "hom nay, hay hom qua truoc 12 gio trua" (bo 30/9/2026). Luat cu coi
     * vo la bai cho ngay mai, nen vo thu Bay het han toi chu nhat trong khi buoi hoc sau
     * la sang thu Hai, va vo truoc ky nghi het han giua ky nghi.
     *
     * Vo ghi ngay mai thi khong: vo ghi ngay cua buoi vua hoc, xem [NhacBai]. Tim khong ra
     * buoi hoc nao sau (nghi he) thi vo dung toi het ngay hom sau.
     */
    fun conDung(ngay: LocalDate?, bayGio: LocalDateTime = LocalDateTime.now()): Boolean {
        if (ngay == null || ngay.isAfter(bayGio.toLocalDate())) return false
        val het = NhacBai.buoiSau(ngay)?.let { (n, b) -> NhacBai.lucVaoHoc(n, b) }
            ?: ngay.plusDays(2).atStartOfDay()
        return bayGio.isBefore(het)
    }

    /** Ban dang nam trong may, khong hoi han. Cho man soat de hien lai cai vua luu. */
    fun doc(context: Context): DanDo? {
        val chu = Prefs.get(context).raw().getString(KHOA, null) ?: return null
        return runCatching { tuJson(JSONObject(chu)) }.getOrNull()
    }

    // Ba ham ghi deu khoa chung mot cho: [ghiMaAnh] chay o luong nen cua DanDoSender,
    // dung luc con co the dang bam Luu lan nua tren man soat.
    //
    // Luu thi chep luon chu sang [NhacBai]: moi duong luu - con soat, Claude doc - deu
    // phai ra loi nhac, khong de duong nao quen.
    @Synchronized
    fun luu(context: Context, d: DanDo) {
        Prefs.get(context).raw().edit().putString(KHOA, sangJson(d).toString()).commit()
        NhacBai.ghi(context, d)
    }

    /**
     * Ghi ma anh Telegram vao ban dang luu, sau khi tin vo dan do gui xong.
     *
     * Chi ghi khi ban trong may van la ban vua gui, so bang [DanDo.luc]. Con bam Luu
     * lan nua trong luc tin dang gui thi ban moi da thay cho, va ma anh cu khong con la
     * anh cua ban do.
     */
    @Synchronized
    fun ghiMaAnh(context: Context, luc: Long, fileId: String) {
        val d = doc(context) ?: return
        if (d.luc != luc) return
        luu(context, d.copy(fileId = fileId))
    }

    /**
     * Xoa ban dang co, ke ca tam anh.
     *
     * Anh phai di theo ban ghi chu khong o lai: no la anh mot trang vo cua tre con,
     * va app nay da chon phia can than o moi cho khac - cau hinh cat trong cho da ma
     * hoa. Giu mot tam anh khong ai con doc nua thi khong co ly do gi bien ho duoc.
     */
    @Synchronized
    fun xoa(context: Context) {
        doc(context)?.anh?.let { runCatching { java.io.File(it).delete() } }
        Prefs.get(context).raw().edit().remove(KHOA).commit()
    }

    /**
     * Don ban da qua han, ca trang da het nhac ben [NhacBai]. Goi luc dich vu khoi dong.
     *
     * Het han la het duong dung: [conHieuLuc] khong tra no ra nua. De nam lai thi mot
     * trang vo cua thu Hai van con trong may vao thu Sau ma khong ai mo, va man soat mo
     * ra lai bay ra dan do cua tuan truoc trong khi man chinh dang ghi "Chụp vở dặn dò
     * hôm nay". Chu cua trang do thi [NhacBai] van giu toi tiet sau cua tung mon.
     */
    fun donDep(context: Context, bayGio: LocalDateTime = LocalDateTime.now()) {
        val d = doc(context)
        // Ban luu bang app truoc 30/9/2026 chua co chu ben NhacBai: chep sang mot lan, khong
        // thi trang vo chup truoc luc cai ban moi khong duoc nhac dong nao.
        if (d != null && !d.chuaDoc && NhacBai.docTrang(context).none { it.chupLuc == d.chupLuc }) {
            NhacBai.ghi(context, d, bayGio)
        }
        NhacBai.donDep(context, bayGio)
        if (d != null && !conDung(d.ngayDoc(), bayGio)) xoa(context)
    }

    fun sangJson(d: DanDo): JSONObject = JSONObject()
        .put("ngay", d.ngay)
        .put("luc", d.luc)
        .put("anh", d.anh)
        .put("fileId", d.fileId)
        .put("chuaDoc", d.chuaDoc)
        .put("nguon", d.nguon)
        .put("chupLuc", d.chupLuc)
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
            fileId = if (o.isNull("fileId")) null else o.optString("fileId").takeIf { it.isNotBlank() },
            chuaDoc = o.optBoolean("chuaDoc"),
            nguon = o.optString("nguon").ifBlank { NGUON_CON },
            // Ban luu truoc khi co truong nay thi moc chup la luc luu: hoi do moi lan
            // luu deu di kem mot lan chup.
            chupLuc = o.optLong("chupLuc", o.optLong("luc"))
        )
    }

    /** Ket qua cua [tuClaude]: ban moi de luu, hoac cau noi vi sao khong luu. */
    data class TuClaude(val ban: DanDo?, val loi: String = "")

    /**
     * Ban vo moi tu ket qua Claude doc, Ba Huy dan ve qua lenh DOCVO.
     *
     * CHI GHI VAO BAN CHUA DOC, va dung tam anh do. Ket qua den muon - con da chup
     * trang khac, hay da bam "Thử đọc lại" va soat xong - thi bo: ghi de len la mat
     * phan con soat, hay dung chu cua trang nay cho tam anh cua trang kia.
     *
     * O tich cua Claude vao ca [Dong.mayTich], de con mo ra sua thi tin gui Ba Huy
     * van ke dung cho con sua khac Claude.
     *
     * @param giaTri { chupLuc, ngay, cacDong: [{ chu, bai }] }.
     */
    fun tuClaude(cu: DanDo?, giaTri: Any?, bayGio: Long = System.currentTimeMillis()): TuClaude {
        val goi = giaTri as? Map<*, *> ?: return TuClaude(null, "Lệnh thiếu kết quả đọc vở, máy không lưu.")
        if (cu == null) return TuClaude(null, "Tablet không còn giữ vở dặn dò nào nên không lưu được.")
        if ((goi["chupLuc"] as? Number)?.toLong() != cu.chupLuc) {
            return TuClaude(null, "Kết quả này của tấm vở cũ, tablet đang giữ tấm khác nên không lưu.")
        }
        if (!cu.chuaDoc) {
            return TuClaude(null, "Vở dặn dò này đã có chữ rồi, máy không ghi đè.")
        }
        val ngay = (goi["ngay"] as? String)?.trim()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return TuClaude(null, "Ngày trong kết quả không đọc được, máy không lưu.")
        val cac = (goi["cacDong"] as? List<*>).orEmpty().mapNotNull { m ->
            val o = m as? Map<*, *> ?: return@mapNotNull null
            val chu = (o["chu"] as? String)?.trim().orEmpty()
            if (chu.isEmpty()) return@mapNotNull null
            val bai = o["bai"] as? Boolean ?: false
            Dong(chu = chu, laBaiTap = bai, mayTich = bai)
        }
        if (cac.isEmpty()) return TuClaude(null, "Kết quả không có dòng dặn dò nào, máy không lưu.")
        return TuClaude(
            cu.copy(
                ngay = ngay.toString(), cacDong = cac, luc = bayGio,
                chuaDoc = false, nguon = NGUON_CLAUDE
            )
        )
    }
}
