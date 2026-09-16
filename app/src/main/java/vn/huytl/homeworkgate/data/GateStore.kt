package vn.huytl.homeworkgate.data

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class GateState {
    /** Khong co gio, app giai tri bi chan. */
    LOCKED,

    /** Da gui anh, dang cho bo bam duyet. */
    PENDING,

    /**
     * Ba duyet roi nhung con chua bam Bat dau, nen dong ho chua chay.
     *
     * Co trang thai nay vi ba khong phai luc nao cung ranh bam duyet ngay. Truoc
     * day gio chay tu luc ba bam, nen ba duyet lai o co quan luc ba gio chieu la
     * den bay gio toi con ve nha gio da tieu sach. Gio thi phan duyet va phan tinh
     * gio tach lam hai, con cam may len bam Bat dau moi bat dau dem.
     *
     * Trong luc nay app giai tri van bi chan: da duyet khong co nghia la dang choi.
     */
    GRANTED,

    /** Dang co gio, app giai tri mo. */
    ACTIVE,

    /**
     * Dang nghi giua chung: dong ho dung, so phut con lai giu nguyen.
     *
     * Trong luc nay app giai tri van khoa. Do la cai lam cho viec tam dung khong
     * bi lam dung: muon giu gio thi phai chiu mat quyen choi trong luc do.
     */
    PAUSED
}

/** Ly do mot phien bi cat, de bao len Telegram cho bo biet. */
enum class EndReason {
    RAN_OUT,        // het gio binh thuong
    HARD_STOP,      // qua han chot trong ngay
    REBOOT,         // may khoi dong lai giua phien
    CLOCK_TAMPER,   // dong ho he thong bi day lui
    PARENT_REVOKED, // bo tu bam thu hoi
    NEVER_STARTED   // da duyet ma con khong bam Bat dau, het ngay thi bo
}

/**
 * Mot bai da nop, dang nam cho Ba Huy duyet.
 *
 * @param id ma ngan di kem trong callback_data cua Telegram, de biet ba vua bam
 *   nut cua bai nao.
 * @param messageId tin nhan mang hai nut duyet, de go ban phim khi bai do xong.
 * @param at luc gui, de bo nhung bai qua ngay.
 */
data class BaiCho(val id: String, val messageId: Long, val at: Long)

/**
 * Giu trang thai cong va tinh so gio con lai.
 *
 * Cach dem gio la cho de bi lach nhat, nen o day dung ba moc cung luc:
 *
 *  - dong ho tuong doi [SystemClock.elapsedRealtime], khong doi duoc bang tay
 *    nhung mat het khi reboot;
 *  - dong ho tuyet doi [System.currentTimeMillis], song qua reboot nhung sua
 *    duoc trong Settings;
 *  - han chot trong ngay, vi du 21:00.
 *
 * Phien het khi moc nao het truoc. Day lui dong ho thi moc tuong doi van chay;
 * day tien dong ho thi chinh dua tre mat gio. Reboot giua phien thi cat luon,
 * vi sau reboot khong con cach nao biet phien da tieu bao nhieu.
 */
class GateStore(context: Context) {

    private val prefs = Prefs.get(context)
    private val sp = prefs.raw()

    var state: GateState
        get() = runCatching { GateState.valueOf(sp.getString(K_STATE, null) ?: "") }
            .getOrDefault(GateState.LOCKED)
        private set(v) = sp.edit().putString(K_STATE, v.name).apply()

    /**
     * Cau ghi tren the o man hinh cua Le Hoa, vi du "Bà nội cho chơi".
     *
     * Rong nghia la Ba Huy duyet - do la duong di cua gan het moi phieu gio, khoi
     * phai ghi cung mot cau vao prefs moi lan. Co o nay tu khi co app ben may ba
     * noi: truoc do man hinh ghi cung mot dong "Ba Huy da duyet", nen ba cho gio
     * ma Le Hoa doc ra van la ba duyet.
     */
    val nhanCho: String
        get() = sp.getString(K_NHAN_CHO, "").orEmpty()

    /**
     * Cac bai dang cho duyet, cu nhat dung truoc.
     *
     * Truoc day cho nay chi giu duoc mot bai. Con lam xong dot hai trong luc dot mot
     * chua ai duyet thi khong nop duoc, phai ngoi cho - trong khi ba hoan toan co
     * the duyet tung bai mot, moi bai mot so phut rieng.
     */
    fun baiDangCho(): List<BaiCho> {
        val raw = sp.getString(K_PENDING_LIST, null)
            // Ban cu chi luu mot bai. Doc not no lan dau sau khi cap nhat app, de
            // bai con vua nop truoc luc cap nhat khong bien mat.
            ?: return sp.getString(K_PENDING_ID, "").orEmpty()
                .takeIf { it.isNotEmpty() }
                ?.let { listOf(BaiCho(it, sp.getLong(K_PENDING_MSG, 0L), System.currentTimeMillis())) }
                .orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                BaiCho(o.getString("i"), o.optLong("m"), o.optLong("a"))
            }
        }.getOrDefault(emptyList())
    }

    fun soBaiDangCho(): Int = baiDangCho().size

    /** Bai cu nhat dang cho, tuc la bai den luot duoc duyet truoc. */
    fun baiChoCuNhat(): BaiCho? = baiDangCho().firstOrNull()

    /** Con cho nop them bai nua khong, hay da xep hang du [MAX_BAI_CHO] bai. */
    fun conChoNopThem(): Boolean = baiDangCho().size < MAX_BAI_CHO

    private var grantedAtWall: Long
        get() = sp.getLong(K_GRANT_WALL, 0L)
        set(v) = sp.edit().putLong(K_GRANT_WALL, v).apply()

    private var grantedAtElapsed: Long
        get() = sp.getLong(K_GRANT_ELAPSED, 0L)
        set(v) = sp.edit().putLong(K_GRANT_ELAPSED, v).apply()

    private var durationMs: Long
        get() = sp.getLong(K_DURATION, 0L)
        set(v) = sp.edit().putLong(K_DURATION, v).apply()

    /**
     * Luc man hinh tat, neu no tat giua mot phien dang chay. Bang 0 la man hinh
     * dang bat. Cat vao dia chu khong giu trong bo nho, vi man hinh tat chinh la
     * luc he thong hay dong bang tien trinh nhat.
     */
    var screenOffAtWall: Long
        get() = sp.getLong(K_SCREEN_OFF, 0L)
        set(v) = sp.edit().putLong(K_SCREEN_OFF, v).commit().let {}

    /** So milli giay con lai luc bam tam dung. */
    private var pausedRemainingMs: Long
        get() = sp.getLong(K_PAUSED_LEFT, 0L)
        set(v) = sp.edit().putLong(K_PAUSED_LEFT, v).commit().let {}

    /** So phut ba da duyet, dang cho con bam Bat dau. */
    var grantedMinutes: Int
        get() = sp.getInt(K_GRANTED_MINUTES, 0)
        private set(v) = sp.edit().putInt(K_GRANTED_MINUTES, v).apply()

    /** Luc ba bam duyet, de biet phieu duyet nay cua ngay nao. */
    private var approvedAtWall: Long
        get() = sp.getLong(K_APPROVED_WALL, 0L)
        set(v) = sp.edit().putLong(K_APPROVED_WALL, v).apply()

    private var hardStopWall: Long
        get() = sp.getLong(K_HARD_STOP_WALL, 0L)
        set(v) = sp.edit().putLong(K_HARD_STOP_WALL, v).apply()

    /** Moc dong ho lan cuoi [tick] chay, de bat viec day lui dong ho. */
    private var lastSeenWall: Long
        get() = sp.getLong(K_SEEN_WALL, 0L)
        set(v) = sp.edit().putLong(K_SEEN_WALL, v).apply()

    private var lastSeenElapsed: Long
        get() = sp.getLong(K_SEEN_ELAPSED, 0L)
        set(v) = sp.edit().putLong(K_SEEN_ELAPSED, v).apply()

    /**
     * Luc dich vu canh app roi khoi he thong, neu no roi giua mot phien dang chay.
     * Bang 0 nghia la no dang o day.
     */
    var guardGoneAtWall: Long
        get() = sp.getLong(K_GUARD_GONE, 0L)
        set(v) = sp.edit().putLong(K_GUARD_GONE, v).commit().let {}

    /** Ly do phien gan nhat ket thuc, de tang bao len Telegram dung mot lan. */
    var lastEndReason: EndReason?
        get() = sp.getString(K_END_REASON, null)?.let {
            runCatching { EndReason.valueOf(it) }.getOrNull()
        }
        set(v) = sp.edit().putString(K_END_REASON, v?.name).apply()

    // So phut da duyet trong ngay
    private var quotaDayKey: Int
        get() = sp.getInt(K_DAY_KEY, 0)
        set(v) = sp.edit().putInt(K_DAY_KEY, v).apply()

    private var phutDaDuyet: Int
        get() = sp.getInt(K_DAY_PHUT, 0)
        set(v) = sp.edit().putInt(K_DAY_PHUT, v).apply()

    /**
     * So phut da duyet hom nay theo duong lam bai.
     *
     * Gio Ba Huy hay ba noi CHO khong nam trong so nay: do la nguoi lon chu dong
     * cho, khong phai con doi bang bai tap.
     */
    fun phutDaDuyetHomNay(now: Long = System.currentTimeMillis()): Int =
        if (quotaDayKey == dayKeyOf(now)) phutDaDuyet else 0

    /**
     * Con duoc duyet them bao nhieu phut hom nay.
     *
     * Truoc day cho nay dem SO LUOT duyet chu khong dem phut. Dem luot hop khi moi
     * lan duyet la mot cuc gio bang nhau, nhung tu luc AI cham tung cau thi mot lan
     * nop chi con hai phut - dem luot thanh ra chan dung cai khong can chan (con nop
     * nhieu lan) va tha cai can chan (tong gio trong ngay).
     */
    fun phutConLaiHomNay(now: Long = System.currentTimeMillis()): Int =
        (prefs.tranPhutMoiNgay - phutDaDuyetHomNay(now)).coerceAtLeast(0)

    /**
     * So milli giay con lai cua phien. Tra 0 neu khong con gio.
     * Ham nay chi doc, khong tu doi trang thai; viec do de [tick] lam.
     */
    fun remainingMs(
        nowWall: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): Long {
        if (state != GateState.ACTIVE) return 0L
        // Dong ho tuong doi tut xuong nghia la may vua reboot.
        if (nowElapsed < grantedAtElapsed) return 0L

        val byElapsed = durationMs - (nowElapsed - grantedAtElapsed)
        val byWall = durationMs - (nowWall - grantedAtWall)
        val byHardStop = hardStopWall - nowWall
        return minOf(byElapsed, byWall, byHardStop).coerceAtLeast(0L)
    }

    fun isOpen(): Boolean = remainingMs() > 0L

    /**
     * Chay dinh ky (khoang 30 giay mot lan) va truoc moi quyet dinh chan app.
     * Tra ve ly do vua cat phien, hoac null neu khong co gi doi.
     */
    fun tick(
        nowWall: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): EndReason? {
        val prevWall = lastSeenWall
        val prevElapsed = lastSeenElapsed
        lastSeenWall = nowWall
        lastSeenElapsed = nowElapsed

        donDepBaiCho(nowWall)

        // Phieu duyet ma con khong dung den: chi co gia tri trong ngay do va khong
        // qua gio chot. Khong co cho nay thi phieu toi qua van bam Bat dau duoc
        // vao sang hom sau, va con choi mot tieng bang bai tap hom qua.
        if (state == GateState.GRANTED) {
            val quaNgay = dayKeyOf(nowWall) != dayKeyOf(approvedAtWall)
            if (quaNgay || nowWall >= hardStopWallFor(nowWall)) {
                refundApproval(nowWall)
                return endSession(EndReason.NEVER_STARTED)
            }
            return null
        }

        // Dang nghi thi khong co gi de dem, nhung van phai het hieu luc khi qua gio
        // chot hoac sang ngay moi. Khong co cho nay thi bam tam dung luc 20:55 la
        // giu duoc gio den tan sang hom sau.
        if (state == GateState.PAUSED) {
            val quaNgay = dayKeyOf(nowWall) != dayKeyOf(approvedAtWall)
            if (quaNgay || nowWall >= hardStopWallFor(nowWall)) {
                return endSession(EndReason.HARD_STOP)
            }
            return null
        }

        if (state != GateState.ACTIVE) return null

        // Reboot: dong ho tuong doi khong bao gio tu giam.
        if (nowElapsed < grantedAtElapsed || (prevElapsed > 0 && nowElapsed < prevElapsed)) {
            return endSession(EndReason.REBOOT)
        }

        // Day lui dong ho he thong. Cho phep lech CLOCK_SLACK_MS de tru cho
        // viec dong bo gio qua mang, vi NTP co the keo lui vai giay.
        if (prevWall > 0 && nowWall < prevWall - CLOCK_SLACK_MS) {
            return endSession(EndReason.CLOCK_TAMPER)
        }

        if (remainingMs(nowWall, nowElapsed) > 0L) return null

        val reason = if (nowWall >= hardStopWall) EndReason.HARD_STOP else EndReason.RAN_OUT
        return endSession(reason)
    }

    /**
     * Con bam nut nop bai, anh da gui len Telegram xong.
     *
     * Bai moi xep vao cuoi hang cho chu khong de len bai cu: ba duyet tung bai mot,
     * moi bai mot so phut rieng.
     */
    fun markPending(requestId: String, messageId: Long, now: Long = System.currentTimeMillis()) {
        // Qua [MAX_BAI_CHO] thi bo bai cu nhat. Gan nhu khong xay ra vi man hinh da
        // chan tu truoc, nhung neu co thi giu bai vua chup con hon giu bai cu.
        val hangDoi = (baiDangCho() + BaiCho(requestId, messageId, now)).takeLast(MAX_BAI_CHO)

        // Dang choi ma nop them bai thi khong dung dong ho lai. Duyet xong se cong
        // thang vao phien dang chay.
        if (state == GateState.ACTIVE) {
            sp.edit().putString(K_PENDING_LIST, ghiHangDoi(hangDoi)).commit()
            return
        }

        // Nop them bai trong luc dang giu gio: gom phan dang giu lai mot cho de lan
        // duyet sau cong don vao do. Dang tam dung thi phan con lai cung tinh, lam
        // tron len phut - con da choi mot doan roi moi di lam bai them.
        val giu = if (state == GateState.PAUSED) {
            ((pausedRemainingMs + 59_999L) / 60_000L).toInt()
        } else {
            grantedMinutes
        }
        sp.edit()
            .putString(K_PENDING_LIST, ghiHangDoi(hangDoi))
            .putInt(K_GRANTED_MINUTES, giu)
            .putLong(K_PAUSED_LEFT, 0L)
            .putString(K_STATE, GateState.PENDING.name)
            .commit()
    }

    /**
     * Bo mot bai ra khoi hang cho: ba bam khong duyet, hoac con tu huy.
     *
     * Neu truoc do con dang giu mot phieu chua dung (nop them bai de cong don) thi
     * tra ve dung phieu do, dung ha xuong khoa. Khong thi nop them bai ma bi tu choi
     * la mat luon phan gio da duoc duyet truoc do - con se khong dam nop them lan nao
     * nua, va cai tinh nang cong don thanh vo dung.
     *
     * Tra ve chinh bai vua bo, de ben goi go ban phim dung tin nhan do ben Telegram.
     */
    fun boBaiCho(requestId: String): BaiCho? {
        val hangDoi = baiDangCho()
        val bai = hangDoi.firstOrNull { it.id == requestId } ?: return null
        luuHangDoi(hangDoi - bai)
        return bai
    }

    /** Con bam "Huy bai vua nop": bo bai moi nhat, giu nguyen cac bai cu con cho. */
    fun huyBaiMoiNhat(): BaiCho? {
        val hangDoi = baiDangCho()
        val bai = hangDoi.lastOrNull() ?: return null
        luuHangDoi(hangDoi - bai)
        return bai
    }

    /**
     * Ghi lai hang cho va sua trang thai cho khop.
     *
     * Het bai cho ma van dang o PENDING thi phai roi ve GRANTED (con giu phieu chua
     * dung) hoac LOCKED. Khong co cho nay thi man hinh ket o "dang cho duyet" trong
     * khi khong con bai nao.
     */
    private fun luuHangDoi(hangDoi: List<BaiCho>) {
        val ed = sp.edit().putString(K_PENDING_LIST, ghiHangDoi(hangDoi))
        if (state == GateState.PENDING && hangDoi.isEmpty()) {
            ed.putString(
                K_STATE,
                if (grantedMinutes > 0) GateState.GRANTED.name else GateState.LOCKED.name
            )
        }
        ed.commit()
    }

    private fun ghiHangDoi(hangDoi: List<BaiCho>): String {
        val array = JSONArray()
        hangDoi.forEach {
            array.put(JSONObject().put("i", it.id).put("m", it.messageId).put("a", it.at))
        }
        return array.toString()
    }

    /**
     * Ba da bam duyet. Chua tinh gio: chi ghi lai la co phieu, cho con bam Bat dau.
     *
     * Tra ve so phut da duyet, hoac null neu khong cap duoc (qua gio chot trong
     * ngay, hoac het luot).
     */
    fun approve(
        now: Long = System.currentTimeMillis(),
        wantedMinutes: Int? = null,
        useQuota: Boolean = true,
        requestId: String? = null,
        /** Ai cap phieu nay. Bo trong la Ba Huy, xem [nhanCho]. */
        nhanCho: String? = null
    ): Int? {
        // Dang co phien chay thi khong duyet de len. Cap phieu moi luc nay se xoa
        // sach so phut con lai va da con ra khoi game giua chung, trong khi y cua
        // ba gan nhu chac chan la cong them. Ben goi phai tu chon: cong them bang
        // [extend], hay dung phien cu truoc da.
        if (state == GateState.ACTIVE) return null

        val hardStop = hardStopWallFor(now)
        if (now >= hardStop) return null
        // Gio thuong (useQuota=false) khong tru vao han muc trong ngay: no la ba
        // chu dong cho chu khong phai con doi bang bai tap.
        val conTran = phutConLaiHomNay(now)
        if (useQuota && conTran <= 0) return null

        // Cong don, khong de len. Con lam xong bai duoc duyet 60 phut nhung chua
        // choi, lam them bai nua nop tiep - duyet lan hai la thanh 120 phut chu
        // khong phai van 60. Truoc day lan duyet sau xoa sach lan truoc, tuc la lam
        // them bai xong lai mat gio, khong ai lam the ca.
        //
        // PENDING cung tinh: do la luc con giu phieu cu ma vua nop them bai moi.
        // PAUSED thi gop not phan gio dang giu lai, lam tron len phut.
        val dangGiu = when (state) {
            GateState.GRANTED, GateState.PENDING -> grantedMinutes
            GateState.PAUSED -> ((pausedRemainingMs + 59_999L) / 60_000L).toInt()
            else -> 0
        }
        // Cham tran trong ngay thi cat bot cho vua, chu khong tu choi ca lan duyet:
        // con lam bai that, cat con 10 phut van hon la khong duoc gi.
        val xin = (wantedMinutes ?: prefs.grantMinutes).coerceIn(1, 600)
        val them = if (useQuota) xin.coerceAtMost(conTran) else xin
        val minutes = (them + dangGiu).coerceIn(1, 600)
        val today = dayKeyOf(now)
        val daDuyet = phutDaDuyetHomNay(now)

        // Duyet dung mot bai trong hang cho. Con bai khac dang cho thi van o PENDING:
        // man hinh con luc do ghi "da duyet ... phut, con mot bai dang cho".
        val conCho = if (requestId == null) baiDangCho() else baiDangCho().filterNot { it.id == requestId }

        // Ghi mot lan roi doi ghi xong. Truoc day day la tam lan edit().apply()
        // rieng le: tien trinh bi giet giua chung la trang thai con nua voi, kieu
        // da danh dau mo cong nhung so phut bang khong. HyperOS giet tien trinh
        // rat han nen day khong phai truong hop hiem.
        sp.edit()
            .putInt(K_GRANTED_MINUTES, minutes)
            .putString(K_NHAN_CHO, nhanCho.orEmpty())
            // Gio dang giu da gop vao phieu moi roi, xoa di khong thi dem hai lan.
            .putLong(K_PAUSED_LEFT, 0L)
            .putLong(K_APPROVED_WALL, now)
            .putInt(K_DAY_KEY, today)
            .putInt(K_DAY_PHUT, if (useQuota) daDuyet + them else daDuyet)
            .putString(K_PENDING_LIST, ghiHangDoi(conCho))
            .remove(K_END_REASON)
            .putString(
                K_STATE,
                if (conCho.isEmpty()) GateState.GRANTED.name else GateState.PENDING.name
            )
            .commit()

        return minutes
    }

    /**
     * Con bam Bat dau. Tu day dong ho moi chay.
     *
     * So phut thuc te co the it hon so ba duyet, neu bam sat gio chot: duyet 60
     * phut luc 19h ma 20h50 moi bam thi chi con 10 phut. Tra ve so phut that su
     * duoc choi, hoac null neu khong con gi de choi.
     */
    fun start(
        now: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): Int? {
        // Dang cho duyet bai moi ma trong tay van con phieu chua dung thi van bam
        // Bat dau duoc. Bat con ngoi cho ba duyet xong moi duoc dung phan da duyet
        // tu truoc la vo ly: nop them bai hoa ra thanh bi phat.
        val giuSan = state == GateState.PENDING && grantedMinutes > 0
        if (state != GateState.GRANTED && !giuSan) return null

        val hardStop = hardStopWallFor(now)
        val actual = minOf(grantedMinutes * 60_000L, hardStop - now)
        if (actual <= 0) {
            endSession(EndReason.HARD_STOP)
            return null
        }

        sp.edit()
            .putLong(K_GRANT_WALL, now)
            .putLong(K_GRANT_ELAPSED, nowElapsed)
            .putLong(K_DURATION, actual)
            .putLong(K_HARD_STOP_WALL, hardStop)
            .putLong(K_SEEN_WALL, now)
            .putLong(K_SEEN_ELAPSED, nowElapsed)
            .remove(K_END_REASON)
            .putString(K_STATE, GateState.ACTIVE.name)
            .commit()

        return (actual / 60_000L).toInt()
    }

    /**
     * Bo cac bai cho qua ngay, va go trang thai ket.
     *
     * Bai nop toi qua ma sang nay moi bam duyet thi con duoc choi bang bai tap hom
     * qua - giong het ly do phieu duyet chua dung chi song trong ngay. Ham nay cung
     * la cho go trang thai PENDING con sot lai tu ban app cu, luc do hang cho rong
     * ma man hinh van ghi "dang cho duyet".
     */
    private fun donDepBaiCho(nowWall: Long) {
        val hangDoi = baiDangCho()
        val giu = hangDoi.filter { it.at > 0L && dayKeyOf(it.at) == dayKeyOf(nowWall) }
        if (giu.size != hangDoi.size) {
            luuHangDoi(giu)
            return
        }
        if (state == GateState.PENDING && hangDoi.isEmpty()) {
            state = if (grantedMinutes > 0) GateState.GRANTED else GateState.LOCKED
        }
    }

    /** Tra lai so phut da tru, khi phieu duyet het han ma chua dung den. */
    private fun refundApproval(now: Long) {
        if (quotaDayKey != dayKeyOf(now)) return
        phutDaDuyet = (phutDaDuyet - grantedMinutes).coerceAtLeast(0)
    }

    /**
     * Dung dong ho lai, giu nguyen so phut con lai.
     *
     * [creditMs] la khoang thoi gian tra lai vi no khong dang bi tinh - dung cho
     * viec tu dung khi tat man hinh: luc phat hien ra thi man hinh da tat mot luc
     * roi, va ca khoang do deu khong phai la choi.
     *
     * Tra ve so phut con lai sau khi dung, hoac null neu khong co phien nao chay.
     */
    fun pause(
        creditMs: Long = 0L,
        now: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): Int? {
        if (state != GateState.ACTIVE) return null

        // Khong bao gio tra lai nhieu hon so da duyet ban dau.
        val tran = grantedMinutes * 60_000L
        val conLai = (remainingMs(now, nowElapsed) + creditMs).coerceIn(0L, tran)
        if (conLai <= 0L) {
            endSession(EndReason.RAN_OUT)
            return 0
        }

        sp.edit()
            .putLong(K_PAUSED_LEFT, conLai)
            .putLong(K_DURATION, 0L)
            .putString(K_STATE, GateState.PAUSED.name)
            .commit()

        return (conLai / 60_000L).toInt()
    }

    /**
     * Chay tiep sau khi nghi. Van khong vuot qua gio chot trong ngay: nghi mot
     * tieng roi quay lai luc 21:05 thi gio nghi van la gio nghi.
     */
    fun resume(
        now: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): Int? {
        if (state != GateState.PAUSED) return null

        val hardStop = hardStopWallFor(now)
        val actual = minOf(pausedRemainingMs, hardStop - now)
        if (actual <= 0L) {
            endSession(EndReason.HARD_STOP)
            return null
        }

        sp.edit()
            .putLong(K_GRANT_WALL, now)
            .putLong(K_GRANT_ELAPSED, nowElapsed)
            .putLong(K_DURATION, actual)
            .putLong(K_HARD_STOP_WALL, hardStop)
            .putLong(K_SEEN_WALL, now)
            .putLong(K_SEEN_ELAPSED, nowElapsed)
            .putLong(K_PAUSED_LEFT, 0L)
            .putString(K_STATE, GateState.ACTIVE.name)
            .commit()

        return (actual / 60_000L).toInt()
    }

    /** So phut con lai khi dang nghi, de man hinh noi duoc "dang dung o 23 phut". */
    fun pausedMinutes(): Int = (pausedRemainingMs / 60_000L).toInt()

    /**
     * So milli giay dang giu, de man hinh hien den tung giay.
     *
     * Lam tron xuong phut thi "12 phut" nhin nhu da mat 43 giay le - dung ra con
     * nguyen do, chi la khong hien. Dem den giay thi con lai bam tiep la khop.
     */
    fun pausedMs(): Long = pausedRemainingMs

    /**
     * Cong them hoac bot phut cua phien dang chay.
     *
     * Cong thi van khong vuot qua han chot trong ngay: 20:50 ma cong them mot gio
     * thi cung chi duoc toi 21:00. Bot ma het sach thi cat phien luon.
     *
     * Tra ve so phut con lai sau khi doi, hoac null neu khong co phien nao dang chay.
     */
    fun extend(
        deltaMinutes: Int,
        now: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime(),
        useQuota: Boolean = false
    ): Int? {
        if (state != GateState.ACTIVE) return null

        // Cong gio giua phien cung an vao tran ngay, neu do la gio doi bang bai tap.
        val them = if (useQuota && deltaMinutes > 0) {
            val conTran = phutConLaiHomNay(now)
            if (conTran <= 0) return null
            deltaMinutes.coerceAtMost(conTran).also {
                sp.edit()
                    .putInt(K_DAY_KEY, dayKeyOf(now))
                    .putInt(K_DAY_PHUT, phutDaDuyetHomNay(now) + it)
                    .apply()
            }
        } else {
            deltaMinutes
        }

        val newDuration = durationMs + them * 60_000L
        if (newDuration <= nowElapsed - grantedAtElapsed) {
            endSession(EndReason.PARENT_REVOKED)
            return 0
        }
        durationMs = newDuration
        return (remainingMs(now, nowElapsed) / 60_000L).toInt()
    }

    /**
     * Cat phien va ghi lai ly do.
     *
     * Bai dang cho duyet thi giu nguyen. Con nop bai trong luc dang choi, het gio
     * truoc khi ba kip bam duyet - cat luon bai do la con mat cong chup ma khong ai
     * xem, va ba bam Duyet chi nhan duoc cau "yeu cau nay cu roi".
     */
    fun endSession(reason: EndReason): EndReason {
        val conCho = baiDangCho()
        // Cung ly do nhu approve: mot lan ghi, doi ghi xong.
        sp.edit()
            .putString(
                K_STATE,
                if (conCho.isEmpty()) GateState.LOCKED.name else GateState.PENDING.name
            )
            .putLong(K_DURATION, 0L)
            .putLong(K_GRANT_WALL, 0L)
            .putLong(K_GRANT_ELAPSED, 0L)
            .putLong(K_HARD_STOP_WALL, 0L)
            .putLong(K_PAUSED_LEFT, 0L)
            .putInt(K_GRANTED_MINUTES, 0)
            .putLong(K_APPROVED_WALL, 0L)
            .putString(K_END_REASON, reason.name)
            .commit()
        return reason
    }

    /**
     * Dang trong khung gio ngu hay khong.
     *
     * Khung nay vat qua nua dem (vi du 22:00 -> 06:00), nen khong the so sanh kieu
     * "truoc hay sau mot moc" nhu truoc day duoc. Truoc day chi co mot moc chot
     * 22:00 tinh theo tung ngay duong lich: 22:00 den nua dem thi khoa, nhung qua
     * 00:00 la ngay moi nen moc chot nhay sang 22:00 hom sau - con thuc den 00:05
     * nop bai la lai choi duoc, va tran phut trong ngay cung vua dat lai.
     */
    fun trongGioNgu(now: Long = System.currentTimeMillis()): Boolean {
        val phut = phutTrongNgay(now)
        val tu = prefs.hardStopMinuteOfDay
        val den = prefs.gioDayMinuteOfDay
        if (tu == den) return false
        return if (tu < den) phut in tu until den else phut >= tu || phut < den
    }

    /**
     * Luc phien phai dung: lan ke tiep dong ho cham gio di ngu.
     *
     * Dang trong gio ngu thi tra ve chinh [now] - khong con phut nao de choi.
     */
    private fun hardStopWallFor(now: Long): Long {
        if (trongGioNgu(now)) return now
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, prefs.hardStopMinuteOfDay / 60)
            set(Calendar.MINUTE, prefs.hardStopMinuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Da qua gio di ngu cua hom nay (tuc la dang o rang sang truoc gio day) thi
        // moc phai la toi nay, khong phai hom qua.
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun phutTrongNgay(now: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun dayKeyOf(now: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return cal.get(Calendar.YEAR) * 10_000 +
            (cal.get(Calendar.MONTH) + 1) * 100 +
            cal.get(Calendar.DAY_OF_MONTH)
    }

    companion object {
        /** Dung sai cho phep khi dong ho he thong bi keo lui. */
        const val CLOCK_SLACK_MS = 90_000L

        private const val K_STATE = "gate_state"
        private const val K_NHAN_CHO = "nhan_cho"
        /**
         * Toi da bay nhieu bai xep hang cho duyet mot luc.
         *
         * Khong de khong han: moi bai la mot chum anh gui len Telegram, va mot dua
         * tre bam nham mot hoi la ba mo may ra thay hai muoi tin nhan.
         */
        const val MAX_BAI_CHO = 3

        private const val K_PENDING_LIST = "pending_list"

        // Hai khoa cua ban cu, chi con doc de chuyen tiep mot lan sau khi cap nhat.
        private const val K_PENDING_ID = "pending_id"
        private const val K_PENDING_MSG = "pending_msg"
        private const val K_GRANT_WALL = "grant_wall"
        private const val K_GRANT_ELAPSED = "grant_elapsed"
        private const val K_DURATION = "grant_duration"
        private const val K_HARD_STOP_WALL = "hard_stop_wall"
        private const val K_SEEN_WALL = "seen_wall"
        private const val K_SEEN_ELAPSED = "seen_elapsed"
        private const val K_END_REASON = "end_reason"
        private const val K_DAY_KEY = "day_key"

        /** So phut da duyet trong ngay. Ban cu dem so luot o "day_count". */
        private const val K_DAY_PHUT = "day_phut"
        private const val K_PAUSED_LEFT = "paused_left"
        private const val K_SCREEN_OFF = "screen_off_wall"
        private const val K_GUARD_GONE = "guard_gone_wall"
        private const val K_GRANTED_MINUTES = "granted_minutes"
        private const val K_APPROVED_WALL = "approved_wall"
    }
}
