package vn.huytl.homeworkgate.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import vn.huytl.homeworkgate.kho.PhamVi
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
     *
     * Co hai loai nghi, phan biet bang [GateStore.dungViTatManHinh]. Vong dem tu dung vi
     * man hinh tat lau thi bat man hinh len la chay tiep. Con lai la nghi giu (con tu bam
     * Tam dung, Ba Huy, viec nha, gio di hoc): chi chay tiep khi co nguoi bam Choi tiep.
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
 * Man hinh vua sang lai thi [GateStore.manHinhSangLai] da lam gi, de ben goi ghi log cho
 * dung. Khong luu xuong dau ca.
 */
enum class SangLai {
    /** Vong dem da tu dung phien vi man hinh tat lau, nay cho chay tiep. */
    CHAY_TIEP,

    /**
     * Phien van dang chay ma man hinh da tat du lau: vong dem khong kip chay vi tien
     * trinh bi dong bang. Tra lai khoang tat roi chay tiep.
     */
    TRA_LAI,

    /** Dang nghi giu (con tu bam, Ba Huy, viec nha, gio di hoc): de nguyen. */
    VAN_GIU,

    /** Khong co gi phai doi: khong co phien nao, hoac man hinh chi tat mot chut. */
    KHONG_DOI
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
 *
 * Viec bat chinh dong ho khong ton mot lan ghi nao: [start] da luu hai moc cua cung
 * mot thoi diem, nen do lech giua chung la du de ket luan. Xem [tick].
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

    /**
     * Nhu [conChoNopThem], cho mot lan nop cu the. Lan nop lai ma se huy bai cu (xem
     * [PhamVi.huyBaiCu]) thi bai cu nhuong cho: service huy no ngay truoc khi xep bai moi.
     */
    fun conChoNop(pham: PhamVi?): Boolean {
        val hang = baiDangCho()
        val nhuongCho = pham?.takeIf { it.laSua && it.huyBaiCu }
            ?.let { p -> hang.any { it.id == p.suaBai } } == true
        return hang.size - (if (nhuongCho) 1 else 0) < MAX_BAI_CHO
    }

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
     *
     * KHONG duoc lay moc nay de doan phien dang nghi co phai do tat man hinh khong: man
     * hinh tat luc dang choi (moc da ghi) roi Ba Huy tam dung tu dien thoai thi moc van
     * con. Viec do la cua [dungViTatManHinh].
     */
    var screenOffAtWall: Long
        get() = sp.getLong(K_SCREEN_OFF, 0L)
        set(v) = sp.edit().putLong(K_SCREEN_OFF, v).commit().let {}

    /** So milli giay con lai luc bam tam dung. */
    private var pausedRemainingMs: Long
        get() = sp.getLong(K_PAUSED_LEFT, 0L)
        set(v) = sp.edit().putLong(K_PAUSED_LEFT, v).commit().let {}

    /**
     * Lan nghi hien tai co phai do vong dem cua GuardAccessibilityService tu dung vi man
     * hinh tat lau khong. Chi lan nghi do moi chay tiep khi man hinh sang lai (xem
     * [manHinhSangLai]); moi lan nghi khac la nghi giu.
     *
     * VI SAO CO CO NAY (1/10/2026). Truoc do dich vu gap PAUSED la chay tiep luc man hinh
     * sang, voi y la chi vong dem moi de lai PAUSED. Nhung nut Tam dung cua con, lenh DUNG
     * va /dung cua Ba Huy, viec nha va gio di hoc cung de lai dung trang thai do. Thu tren
     * may ao, man hinh chi tat 5 giay: con tam dung de giu gio thi bat man hinh len la mat
     * gio; viec nha vua giao thi dong ho dem lui sau man chan viec nha; trong gio hoc phien
     * chay lai roi bi dung lai sau 0,27 giay, kem them mot tin "Toi gio di hoc" gui Ba Huy.
     *
     * [pause] ghi co nay chung mot lan commit voi trang thai, nen tien trinh bi giet giua
     * chung cung khong lech. Co chi duoc doc khi dang PAUSED, ma vao PAUSED thi phai qua
     * [pause], nen khong phai xoa no o [resume], [endSession] hay [approve]. Khoa chua co
     * (ban app truoc 1/10/2026 de lai mot lan tu dung) thi doc ra false: lan bat man hinh
     * do khong tu chay, con bam Choi tiep mot lan.
     */
    fun dungViTatManHinh(): Boolean =
        state == GateState.PAUSED && sp.getBoolean(K_DUNG_VI_TAT_MAN, false)

    /**
     * So phut ba da duyet, dang cho con bam Bat dau.
     *
     * Bam Bat dau roi thi o nay van giu so phut cua phieu luc do, va [extend] khong cong
     * vao day: dang choi hay tam dung thi no khong phai so phut cua phien. Do dai doan dang
     * chay la [durationMs], phan giu luc nghi la [pausedRemainingMs]. [pause] tung lay o
     * nay lam tran, nen phut cong giua phien mat o lan tam dung dau tien (sua ngay 1/10/2026).
     */
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
     * Con kiem them duoc bao nhieu phut hom nay, CHI DE HIEN RA man hinh.
     *
     * Tu 29/9/2026 khong con tran chung chan gio: moi phan tu chan bang tran rieng (xem
     * [LuatCongGio]), va tran ngay bang tong cac tran do. Con so nay khong con chan ai
     * duyet hay cap gi ca - [approve] va [extend] khong doc no nua.
     */
    fun phutConLaiHomNay(now: Long = System.currentTimeMillis()): Int =
        (LuatCongGio.TRAN_NGAY - phutDaDuyetHomNay(now)).coerceAtLeast(0)

    /**
     * So milli giay con DA CHOI that trong ngay, dem bang dong ho chu khong suy ra.
     *
     * VI SAO PHAI DEM (1/10/2026). Truoc do thanh han muc ngay suy "da choi" bang phep tru
     * "phut kiem duoc - phut dang giu", va phep tru do sai moi lan co phut khong di qua
     * so dem kiem duoc. Ba Huy cho them 30 phut giua phien thi khuc "da choi" tut tu 30 ve
     * 0; cuoi ngay thanh ghi da choi 45 trong khi con choi 75. Phut bi bot, bi khoa giua
     * chung hay het han luc di ngu thi nguoc lai: bien mat ma lai bi tinh la da choi.
     *
     * Dem that thi ca hai loai tu dung: phut nguoi lon cho vao phien nao thi choi den dau
     * dem den do, phut bi lay lai thi khong ai choi nen khong dem. Gom lai tung doan phien
     * luc [pause] va [endSession], cong them doan dang chay.
     */
    fun msDaChoiHomNay(
        nowWall: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): Long = msDaChoiDaGom(nowWall) + choiTrongDoan(nowWall, nowElapsed)

    /**
     * Phan da gom cua [msDaChoiHomNay], chua tinh doan dang chay.
     *
     * Day len dien thoai so nay chu khong day [msDaChoiHomNay]: so kia doi moi giay luc
     * dang choi, va ban trang thai co no thi lan nao cung khac ban vua day, DongBo khong bo
     * duoc luot ghi nao. Ben kia tu cong doan dang chay tu [doanChoiTu].
     */
    fun msDaChoiDaGom(nowWall: Long = System.currentTimeMillis()): Long =
        if (sp.getInt(K_CHOI_NGAY, 0) == dayKeyOf(nowWall)) sp.getLong(K_CHOI_MS, 0L) else 0L

    /**
     * Luc doan phien dang chay bat dau, theo gio tuong. 0 la khong co doan nao dang chay,
     * hoac doan do bat dau tu ngay khac - cung luat voi [choiTrongDoan].
     */
    fun doanChoiTu(nowWall: Long = System.currentTimeMillis()): Long =
        if (state == GateState.ACTIVE && dayKeyOf(grantedAtWall) == dayKeyOf(nowWall)) grantedAtWall else 0L

    /**
     * So milli giay con dang giu, chua choi: phan con lai cua phien dang chay, phan giu
     * luc tam dung, hay phieu da duyet chua bam Bat dau.
     *
     * Dang choi thi doc [remainingMs] chu khong doc [grantedMinutes]: o do van giu so
     * phut cua phieu luc bam Bat dau, phien het gio ma [tick] chua kip cat thi man hinh
     * se hien lai ca phieu cu.
     */
    fun msDangGiu(
        nowWall: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): Long = when (state) {
        GateState.ACTIVE -> remainingMs(nowWall, nowElapsed)
        GateState.PAUSED -> pausedRemainingMs
        GateState.GRANTED, GateState.PENDING -> grantedMinutes * 60_000L
        GateState.LOCKED -> 0L
    }

    /**
     * Doan phien dang chay da choi duoc bao lau, 0 neu khong co phien nao chay.
     *
     * Dem bang dong ho tuong doi nhu [remainingMs]. Vua reboot thi dong ho do da ve 0,
     * nen lay dong ho tuong: lech mat may chuc giay khoi dong may, con hon bo ca doan.
     * Khong dai qua do dai phien, va khong qua gio chot - phien ket o gio chot ma vong
     * dem 30 giay moi kip cat thi may giay sau do khong phai la choi.
     *
     * Doan bat dau tu mot ngay khac thi khong tinh cho ngay [nowWall]. Tablet het pin luc
     * 21:55 giua phien, sang hom sau mo len moi cat phien vi reboot: khong co dong nay thi
     * ca doan toi qua vao so da choi cua sang nay. Thay tren may ao ngay 1/10/2026, khi mot
     * phien gia cua bo test (ngay 11/9) bi dong vao hom nay va thanh ghi "đã chơi 50".
     */
    private fun choiTrongDoan(nowWall: Long, nowElapsed: Long): Long {
        if (state != GateState.ACTIVE) return 0L
        if (dayKeyOf(grantedAtWall) != dayKeyOf(nowWall)) return 0L
        val troi = if (nowElapsed >= grantedAtElapsed) {
            nowElapsed - grantedAtElapsed
        } else {
            nowWall - grantedAtWall
        }
        val chot = hardStopWall
        val tran = if (chot > 0L) minOf(durationMs, chot - grantedAtWall) else durationMs
        return troi.coerceIn(0L, tran.coerceAtLeast(0L))
    }

    /**
     * Cong [ms] vao so da choi cua ngay [nowWall], ghi chung lan ghi cua ben goi.
     *
     * Chung mot lan ghi voi lan doi trang thai, khong ghi rieng: moi lan ghi prefs keo theo
     * mot luot DongBo xet day len Firestore, va tien trinh bi giet giua hai lan ghi la so
     * da choi lech voi trang thai.
     */
    private fun gomDaChoi(ed: SharedPreferences.Editor, ms: Long, nowWall: Long) {
        if (ms <= 0L) return
        val ngay = dayKeyOf(nowWall)
        val cu = if (sp.getInt(K_CHOI_NGAY, 0) == ngay) sp.getLong(K_CHOI_MS, 0L) else 0L
        ed.putInt(K_CHOI_NGAY, ngay).putLong(K_CHOI_MS, cu + ms)
    }

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
        donDepBaiCho(nowWall)

        // Phieu duyet ma con khong dung den: chi co gia tri trong ngay do va khong
        // qua gio chot. Khong co cho nay thi phieu toi qua van bam Bat dau duoc
        // vao sang hom sau, va con choi mot tieng bang bai tap hom qua.
        if (state == GateState.GRANTED) {
            val quaNgay = dayKeyOf(nowWall) != dayKeyOf(approvedAtWall)
            if (quaNgay || nowWall >= hardStopWallFor(nowWall)) {
                refundApproval(nowWall)
                return endSession(EndReason.NEVER_STARTED, nowWall, nowElapsed)
            }
            return null
        }

        // Dang nghi thi khong co gi de dem, nhung van phai het hieu luc khi qua gio
        // chot hoac sang ngay moi. Khong co cho nay thi bam tam dung luc 20:55 la
        // giu duoc gio den tan sang hom sau.
        if (state == GateState.PAUSED) {
            val quaNgay = dayKeyOf(nowWall) != dayKeyOf(approvedAtWall)
            if (quaNgay || nowWall >= hardStopWallFor(nowWall)) {
                return endSession(EndReason.HARD_STOP, nowWall, nowElapsed)
            }
            return null
        }

        if (state != GateState.ACTIVE) return null

        // Reboot: dong ho tuong doi khong bao gio tu giam.
        if (nowElapsed < grantedAtElapsed) return endSession(EndReason.REBOOT, nowWall, nowElapsed)

        /*
         * Day lui dong ho he thong.
         *
         * Khong con ghi moc sau moi lan tick nua. Luc bam Bat dau, [start] da luu
         * hai moc CUA CUNG MOT THOI DIEM: [grantedAtWall] doc tu dong ho tuong va
         * [grantedAtElapsed] doc tu dong ho tuong doi. Khong ai chinh dong ho thi
         * hai cai do troi bang nhau, nen chi can mot phep tru la biet.
         *
         * Ban cu so hai lan tick lien nhau, va phai ghi hai so xuong dia moi 20 giay
         * de so duoc - moi lan ghi keo theo mot luot day len Firestore. Cach nay vua
         * khong ghi gi, vua bat chac hon: day lui ba muoi giay nam lan thi ban cu lot
         * ca nam (moi lan deu duoi nguong), con cach nay cong don nen lan thu tu la
         * dinh.
         *
         * Cho phep lech [CLOCK_SLACK_MS] de tru cho viec dong bo gio qua mang.
         */
        if (nowWall - grantedAtWall < (nowElapsed - grantedAtElapsed) - CLOCK_SLACK_MS) {
            return endSession(EndReason.CLOCK_TAMPER, nowWall, nowElapsed)
        }

        if (remainingMs(nowWall, nowElapsed) > 0L) return null

        val reason = if (nowWall >= hardStopWall) EndReason.HARD_STOP else EndReason.RAN_OUT
        return endSession(reason, nowWall, nowElapsed)
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

    /**
     * Cong gio doi bang viec hoc tren tablet (kiem tra bai, do tu vung, Giai de): dang
     * choi thi cong vao phien, khong thi cap phieu. Tran rieng cua tung phan do ben goi
     * tu chan truoc (xem [LuatCongGio]); o day chi ghi vao so dem cua ngay.
     *
     * @return so phut con lai (dang choi) hay so phut cua phieu, null la khong cap duoc.
     */
    fun congGioHoc(phut: Int, nhanCho: String): Int? =
        if (state == GateState.ACTIVE) extend(phut, useQuota = true)
        else approve(wantedMinutes = phut, useQuota = true, nhanCho = nhanCho)

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
        // Khong con tran chung (29/9/2026): phan nao da tu chan bang tran rieng truoc khi
        // goi vao day. useQuota chi con ghi so phut doi bang hoc vao so dem cua ngay, de
        // man hinh noi "hom nay kiem duoc bao nhieu".
        val xin = (wantedMinutes ?: prefs.grantMinutes).coerceIn(1, 600)
        val them = xin
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
            endSession(EndReason.HARD_STOP, now, nowElapsed)
            return null
        }

        sp.edit()
            .putLong(K_GRANT_WALL, now)
            .putLong(K_GRANT_ELAPSED, nowElapsed)
            .putLong(K_DURATION, actual)
            .putLong(K_HARD_STOP_WALL, hardStop)
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
     * [viTatManHinh] chi vong dem cua GuardAccessibilityService dat true, khi no tu dung
     * phien vi man hinh tat lau: lan nghi do bat man hinh len la chay tiep (xem
     * [dungViTatManHinh]). Mac dinh la nghi giu, chi chay tiep khi co nguoi bam Choi tiep,
     * nen duong tam dung nao viet sau nay ma quen tham so nay thi hong ve phia con phai bam
     * Choi tiep, khong hong ve phia may tu chay gio choi.
     *
     * Dang nghi vi tat man hinh ma co ai goi tam dung giu thi lan nghi do doi thanh nghi
     * giu, va ham tra ve so phut dang giu nhu mot lan tam dung moi. Goi lai lan nua thi tra
     * null nhu thuong.
     *
     * Tra ve so phut con lai sau khi dung, hoac null neu khong co phien nao chay hay khong
     * doi gi.
     */
    fun pause(
        creditMs: Long = 0L,
        now: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime(),
        viTatManHinh: Boolean = false
    ): Int? {
        // Doi nghi vi tat man hinh thanh nghi giu (1/10/2026). Day la canh hay gap nhat
        // ngoai doi: ba giao viec nha luc tablet dang nam tat man hinh, Le Hoa de may do roi
        // moi toi gio buong may di hoc, hay Ba Huy go /dung luc may dang ngu. Phien da do vong
        // dem tu dung, nen thieu nhanh nay thi ham tra null, khong doi gi, va bat man hinh
        // len la phien chay lai du cac duong do da tam dung. Tra so phut chu khong tra null
        // de lenh DUNG dap "Da tam dung, giu ... phut" (truoc do dap sai la con dang khong
        // trong gio choi) va tin "Toi gio di hoc" gui dung luc vao gio hoc. Lan goi sau thay
        // co da tat, roi xuong dong ben duoi tra null: catGioChoiDangCo goi moi giay ma
        // khong gui them tin nao.
        if (state == GateState.PAUSED && !viTatManHinh && dungViTatManHinh()) {
            sp.edit().putBoolean(K_DUNG_VI_TAT_MAN, false).commit()
            return (pausedRemainingMs / 60_000L).toInt()
        }
        if (state != GateState.ACTIVE) return null

        // Khong bao gio tra lai nhieu hon ca doan dang chay. [durationMs] la do dai doan
        // nay: so ms luc bam Bat dau hay Choi tiep, cong ca phut [extend] them hay bot giua
        // chung. Khoang tat man hinh chi duoc tra phan da troi trong doan, nen con lai sau
        // khi tra toi da bang ca doan, nhu the doan chua tieu phut nao.
        //
        // Truoc 1/10/2026 tran la [grantedMinutes], so phut cua phieu luc bam Bat dau, ma
        // [extend] khong sua o do. Phien da duoc cong them giua chung (Ba Huy cho them, cham
        // bai xong, viec nha, quy gio choi) thi lan tam dung dau tien cat phan con lai ve
        // bang phieu cu, mat het phan da cong. Vong dem tu dung phien khi man hinh tat du
        // lau, va dung roi chay lai luc man hinh sang, nen loi nay gap that: phieu 45 phut,
        // cham bai cong 60 o phut thu 5, tam dung o phut thu 6 con 45 thay vi 99 (thu tren
        // may ao ngay 1/10/2026). Sau mot lan nghi thi tran cu lai long: doan choi tiep chi
        // con phan da giu, ma moc tat man hinh co tu truoc luc choi tiep (Ba Huy bam Choi
        // tiep tu dien thoai trong luc may van tat man hinh) thi khoang tra lai gom ca luc
        // nghi, la phut khong ai tieu ma van duoc tra (suy tu code, chua gap tren may that).
        val tran = durationMs
        val conLai = (remainingMs(now, nowElapsed) + creditMs).coerceIn(0L, tran)
        if (conLai <= 0L) {
            endSession(EndReason.RAN_OUT, now, nowElapsed)
            return 0
        }
        // Khoang man hinh tat duoc tra lai thi cung khong phai la choi.
        val choi = (choiTrongDoan(now, nowElapsed) - creditMs).coerceAtLeast(0L)

        val ed = sp.edit()
            .putLong(K_PAUSED_LEFT, conLai)
            .putLong(K_DURATION, 0L)
            .putString(K_STATE, GateState.PAUSED.name)
            // Chung lan ghi voi trang thai, xem [dungViTatManHinh].
            .putBoolean(K_DUNG_VI_TAT_MAN, viTatManHinh)
        gomDaChoi(ed, choi, now)
        ed.commit()

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
            endSession(EndReason.HARD_STOP, now, nowElapsed)
            return null
        }

        sp.edit()
            .putLong(K_GRANT_WALL, now)
            .putLong(K_GRANT_ELAPSED, nowElapsed)
            .putLong(K_DURATION, actual)
            .putLong(K_HARD_STOP_WALL, hardStop)
            .putLong(K_PAUSED_LEFT, 0L)
            .putString(K_STATE, GateState.ACTIVE.name)
            .commit()

        return (actual / 60_000L).toInt()
    }

    /**
     * Man hinh vua sang lai sau khoang tat [tatMs] (0 la khong biet luc tat). Luat cua
     * GuardAccessibilityService, tach ra day de test duoc nhu [vn.huytl.homeworkgate.guard.LuatManHinh]:
     *
     *  - vong dem da tu dung phien vi man hinh tat lau: chay tiep;
     *  - dang nghi giu: de nguyen, cho nguoi bam Choi tiep;
     *  - phien van chay ma man hinh tat tu [nguongMs] tro len: vong dem khong kip chay vi
     *    tien trinh bi dong bang, tra lai khoang tat roi chay tiep.
     *
     * Truoc 1/10/2026 dich vu gap PAUSED la chay tiep, khong xet ai da dung. Chuyen ke o
     * [dungViTatManHinh].
     */
    fun manHinhSangLai(
        tatMs: Long,
        nguongMs: Long,
        now: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): SangLai = when {
        dungViTatManHinh() -> {
            resume(now, nowElapsed)
            SangLai.CHAY_TIEP
        }
        state == GateState.PAUSED -> SangLai.VAN_GIU
        state == GateState.ACTIVE && tatMs >= nguongMs -> {
            // Danh dau tu dung de neu tien trinh chet giua hai lan ghi nay thi lan bat
            // man hinh sau van chay tiep duoc.
            pause(creditMs = tatMs, now = now, nowElapsed = nowElapsed, viTatManHinh = true)
            resume(now, nowElapsed)
            SangLai.TRA_LAI
        }
        else -> SangLai.KHONG_DOI
    }

    /**
     * Ca phien nay dai bao nhieu ms, 0 neu khong co phien nao chay.
     *
     * Dung yen trong suot phien, khac [remainingMs] tru dan tung giay. Ben dien
     * thoai Ba Huy lay so nay lam moc cua thanh chay.
     */
    fun tongPhienMs(): Long = if (state == GateState.ACTIVE) durationMs else 0L

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

        // Gio doi bang hoc thi ghi vao so dem cua ngay. Khong con tran chung de cat bot
        // (29/9/2026): moi phan da tu chan bang tran rieng.
        val them = deltaMinutes
        if (useQuota && deltaMinutes > 0) {
            sp.edit()
                .putInt(K_DAY_KEY, dayKeyOf(now))
                .putInt(K_DAY_PHUT, phutDaDuyetHomNay(now) + deltaMinutes)
                .apply()
        }

        val newDuration = durationMs + them * 60_000L
        if (newDuration <= nowElapsed - grantedAtElapsed) {
            endSession(EndReason.PARENT_REVOKED, now, nowElapsed)
            return 0
        }
        durationMs = newDuration
        return (remainingMs(now, nowElapsed) / 60_000L).toInt()
    }

    /**
     * So phut tron con dang giu trong phieu hom nay, de doi sang Netflix tren laptop
     * (7/10/2026). Phieu chua bam Bat dau, dang tam dung hay dang chay deu doi duoc; phan
     * le giay khong tinh.
     */
    fun phutDoiDuoc(
        now: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): Int = when (state) {
        GateState.GRANTED, GateState.PENDING -> grantedMinutes
        GateState.PAUSED -> (pausedRemainingMs / 60_000L).toInt()
        GateState.ACTIVE -> (remainingMs(now, nowElapsed) / 60_000L).toInt()
        else -> 0
    }.coerceAtLeast(0)

    /**
     * Lay [phut] phut ra khoi phieu hom nay de doi sang Netflix tren laptop (7/10/2026).
     * Tra ve so phut da lay that, 0 la khong lay duoc gi.
     *
     * Lay nhieu hon dang giu thi chi lay phan dang giu. [het] la con bam "Đổi hết": lay
     * xong thi cat phien luon, ke ca vai chuc giay le con lai. Khong cat thi tablet con
     * mot phieu 30 giay, con bam Bat dau la het ngay, va the tren man hinh ghi "0 phút".
     *
     * Phut lay ra KHONG tra vao [phutDaDuyetHomNay]: so do la phut con kiem duoc trong
     * ngay, doi sang laptop van la phut con da kiem. Thanh ngay tu dung vi no dem phut da
     * choi that cong phut dang giu, va phan doi di khong con nam o dau ca.
     */
    fun doiPhut(
        phut: Int,
        het: Boolean = false,
        now: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): Int {
        val co = phutDoiDuoc(now, nowElapsed)
        val lay = minOf(phut, co)
        if (lay <= 0) return 0
        val conMs: Long
        when (state) {
            GateState.GRANTED, GateState.PENDING -> {
                val con = grantedMinutes - lay
                if (con > 0 && !het) {
                    sp.edit().putInt(K_GRANTED_MINUTES, con).commit()
                } else if (state == GateState.GRANTED) {
                    endSession(EndReason.RAN_OUT, now, nowElapsed)
                } else {
                    // PENDING ma het phieu: van cho duyet bai, chi la khong con phut giu san.
                    sp.edit().putInt(K_GRANTED_MINUTES, 0).commit()
                }
                return lay
            }
            GateState.PAUSED -> conMs = pausedRemainingMs - lay * 60_000L
            GateState.ACTIVE -> conMs = remainingMs(now, nowElapsed) - lay * 60_000L
            else -> return 0
        }
        if (het || conMs < 60_000L) {
            // Duoi mot phut cung cat: mot phieu chua toi mot phut chi lam con bam Bat dau
            // roi bi khoa ngay.
            endSession(EndReason.RAN_OUT, now, nowElapsed)
        } else if (state == GateState.PAUSED) {
            pausedRemainingMs = conMs
        } else {
            durationMs -= lay * 60_000L
        }
        return lay
    }

    /**
     * Cat phien va ghi lai ly do.
     *
     * Bai dang cho duyet thi giu nguyen. Con nop bai trong luc dang choi, het gio
     * truoc khi ba kip bam duyet - cat luon bai do la con mat cong chup ma khong ai
     * xem, va ba bam Duyet chi nhan duoc cau "yeu cau nay cu roi".
     *
     * Dang choi thi gom doan vua choi vao [msDaChoiHomNay]. Phan chua choi bi bo (khoa,
     * thu hoi, toi gio ngu) khong gom: khong ai choi phan do.
     */
    fun endSession(
        reason: EndReason,
        nowWall: Long = System.currentTimeMillis(),
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): EndReason {
        val conCho = baiDangCho()
        val choi = choiTrongDoan(nowWall, nowElapsed)
        // Cung ly do nhu approve: mot lan ghi, doi ghi xong.
        val ed = sp.edit()
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
        gomDaChoi(ed, choi, nowWall)
        ed.commit()
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
        private const val K_END_REASON = "end_reason"
        private const val K_DAY_KEY = "day_key"

        /** So phut da duyet trong ngay. Ban cu dem so luot o "day_count". */
        private const val K_DAY_PHUT = "day_phut"
        private const val K_PAUSED_LEFT = "paused_left"
        private const val K_SCREEN_OFF = "screen_off_wall"

        /** Lan nghi hien tai do vong dem tu dung vi tat man hinh, xem [dungViTatManHinh]. */
        private const val K_DUNG_VI_TAT_MAN = "dung_vi_tat_man"
        private const val K_GUARD_GONE = "guard_gone_wall"
        private const val K_GRANTED_MINUTES = "granted_minutes"
        private const val K_APPROVED_WALL = "approved_wall"

        /** So milli giay da choi trong ngay [K_CHOI_NGAY], xem [msDaChoiHomNay]. */
        private const val K_CHOI_MS = "choi_ms"
        private const val K_CHOI_NGAY = "choi_ngay"
    }
}
