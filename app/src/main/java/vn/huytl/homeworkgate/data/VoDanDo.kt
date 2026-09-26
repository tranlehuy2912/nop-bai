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
 *
 * TAT CHAM AI VAN DUNG BAN NAY. Moi lan nop, tablet chep ban dang con hieu luc vao bai
 * tren Firestore, va Bang dieu khien dua dung ngay va danh sach bai do vao loi nho
 * Claude - xem [vn.huytl.homeworkgate.dongbo.DongBo.banDanDo] va [VoChoCham]. Nen con
 * chup vo mot lan dau buoi du bai do may cham hay Claude cham.
 *
 * MAY DOC HONG THI VAN GIU TAM ANH ([DanDo.chuaDoc]). Truoc day may doc khong duoc la
 * man vo dan do khong luu gi, va lan nop nao cung hoi chup lai trang vo. Gio con gui
 * tam anh cho Ba Huy, cac lan nop sau mang theo tam do, va chu duoc doc ra sau: Ba Huy
 * nho Claude doc, hay lan cham bai dau tien doc luon.
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
         * Bang dieu khien cam ma nay tai lai dung tam anh do, gui kem cho Claude doi
         * chieu voi danh sach con da tich - xem [vn.huytl.homeworkgate.dongbo.DongBo
         * .banDanDo].
         */
        val fileId: String? = null,
        /**
         * Chi co anh, chua ai doc ra chu: may doc khong duoc va con bam "Gửi ảnh để ba
         * Huy đọc". Luc do [cacDong] rong nhung KHONG co nghia la co khong giao bai tap,
         * ma la chua biet. Ngay tam la ngay chup, doc xong se thay bang ngay ghi tren vo.
         *
         * Ba duong doc ra chu: con bam "Thử đọc lại" khi may doc duoc tro lai, Ba Huy
         * nho Claude doc ([tuClaude]), hoac lan cham bai dau tien doc tam anh gan theo
         * bai ([tuLanCham]).
         */
        val chuaDoc: Boolean = false,
        /** Ai doc ra danh sach nay: [NGUON_CON], [NGUON_CLAUDE] hay [NGUON_LUC_CHAM]. */
        val nguon: String = NGUON_CON,
        /**
         * Luc chup tam anh trang vo. Doc lai hay sua chu thi giu nguyen, chi doi khi
         * con chup tam khac.
         *
         * Dung de biet hai ban co cung mot tam anh khong: bai nop luc vo con chua doc
         * mang theo ban chi co anh, va khi cham phai nhan ra ban da doc sau do la cua
         * dung trang ay - xem [VoChoCham.voChoBai].
         */
        val chupLuc: Long = luc
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

    /** Doc ra o lan cham bai dau tien, tu tam anh trang vo gan theo bai. */
    const val NGUON_LUC_CHAM = "LUCCHAM"

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

    // Ba ham ghi deu khoa chung mot cho: [ghiMaAnh] chay o luong nen cua DanDoSender,
    // dung luc con co the dang bam Luu lan nua tren man soat.
    @Synchronized
    fun luu(context: Context, d: DanDo) {
        Prefs.get(context).raw().edit().putString(KHOA, sangJson(d).toString()).commit()
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
     * Don ban da qua han. Goi luc dich vu khoi dong.
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

    /**
     * Ban vo moi tu lan cham dau tien doc tam anh trang vo gan theo bai.
     *
     * Giu lai ket qua do thi cac bai nop sau mang theo danh sach nay, Claude khong phai
     * doc lai trang vo o moi lan cham, va moi bai trong ngay cham theo cung mot danh
     * sach. Chi co bai tap vi lan cham chi doc ra bai tap, khong ke dan viec khac.
     *
     * null khi khong dung duoc: ban trong may khong con la tam anh bai do mang theo, da
     * co chu roi, hay lan cham khong doc ra ngay trong vo. Thieu ngay thi khong giu: dien
     * ngay chup vao la cho tron goi 45 phut theo mot ngay khong ai doc thay tren vo.
     */
    fun tuLanCham(
        cu: DanDo?,
        chupLucCuaBai: Long,
        ngayDanDo: String?,
        baiDuocGiao: List<String>,
        bayGio: Long = System.currentTimeMillis()
    ): DanDo? {
        if (cu == null || !cu.chuaDoc || cu.chupLuc != chupLucCuaBai) return null
        val ngay = LuatCongGio.docNgay(ngayDanDo) ?: return null
        return cu.copy(
            ngay = ngay.toString(),
            cacDong = baiDuocGiao.map { Dong(it, laBaiTap = true, mayTich = true) },
            luc = bayGio,
            chuaDoc = false,
            nguon = NGUON_LUC_CHAM
        )
    }
}
