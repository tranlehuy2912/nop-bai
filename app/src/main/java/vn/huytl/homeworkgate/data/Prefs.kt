package vn.huytl.homeworkgate.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Moi cau hinh va trang thai nam trong EncryptedSharedPreferences, khoa giu trong
 * Android Keystore. Muc dich khong phai chong hacker, ma chong dua tre cam may
 * cam vao may tinh roi doc file prefs de biet token bot, hoac sua so phut con lai.
 */
/**
 * Ghi chu ve commit() va apply():
 *
 * Cac gia tri cau hinh dung commit() vi chung duoc ghi rat hiem, va neu he thong
 * giet tien trinh truoc khi apply() kip day xuong dia thi mat cau hinh, tablet
 * thanh khong quan ly duoc ma khong ai biet. Rieng telegramOffset va nhip tim
 * ghi lien tuc nen van dung apply().
 */
class Prefs private constructor(private val sp: SharedPreferences) {

    // Cau hinh do bo dat mot lan khi cai may
    var botToken: String
        get() = sp.getString(KEY_BOT_TOKEN, "").orEmpty()
        set(v) = sp.edit().putString(KEY_BOT_TOKEN, v.trim()).commit().let {}

    /** Chat id cua bo. Chi callback_query den tu dung id nay moi duoc tin. */
    var parentChatId: Long
        get() = sp.getLong(KEY_PARENT_CHAT_ID, 0L)
        set(v) = sp.edit().putLong(KEY_PARENT_CHAT_ID, v).commit().let {}

    /** So phut moi lan duoc duyet. */
    var grantMinutes: Int
        get() = sp.getInt(KEY_GRANT_MINUTES, 60)
        set(v) = sp.edit().putInt(KEY_GRANT_MINUTES, v.coerceIn(5, 240)).commit().let {}

    /** Gio di ngu: tu luc nay la khoa. Dang phut tinh tu 00:00. Mac dinh 22:00. */
    var hardStopMinuteOfDay: Int
        get() = sp.getInt(KEY_HARD_STOP, Defaults.HARD_STOP_MINUTE)
        set(v) = sp.edit().putInt(KEY_HARD_STOP, v.coerceIn(0, 24 * 60 - 1)).commit().let {}

    /**
     * Gio mo lai buoi sang. Mac dinh 06:00.
     *
     * Truoc day khong co cai nay, chi co mot moc chot 22:00 tinh theo tung ngay
     * duong lich. Nghia la 22:00 den nua dem thi khoa, nhung qua 00:00 la sang ngay
     * moi nen moc chot nhay sang 22:00 hom sau - con thuc den 00:05 nop bai la choi
     * duoc tiep, va tran phut cung vua dat lai. Gio hai moc nay tao thanh mot khung
     * ngu that, vat qua nua dem.
     */
    var gioDayMinuteOfDay: Int
        get() = sp.getInt(KEY_GIO_DAY, Defaults.WAKE_MINUTE)
        set(v) = sp.edit().putInt(KEY_GIO_DAY, v.coerceIn(0, 24 * 60 - 1)).commit().let {}

    /**
     * So phut choi toi da moi ngay, tinh theo duong lam bai.
     *
     * Truoc day cho nay la SO LUOT duyet moi ngay. Dem luot hop khi moi lan duyet la
     * mot cuc gio bang nhau, nhung tu luc AI cham tung cau thi mot lan nop co the chi
     * la hai phut - dem luot thanh ra chan dung cai khong can chan (con nop nhieu
     * lan) va tha cai can chan (tong gio trong ngay).
     *
     * Gio Ba Huy hay ba noi chu dong cho khong tinh vao day.
     */
    var tranPhutMoiNgay: Int
        get() = sp.getInt(KEY_TRAN_PHUT, Defaults.TRAN_PHUT_MOI_NGAY)
        set(v) = sp.edit().putInt(KEY_TRAN_PHUT, v.coerceIn(15, 600)).commit().let {}

    /**
     * Danh sach trang: nhung app con duoc dung ca khi het gio, vi du tu dien,
     * may tinh, app hoc tieng Anh.
     *
     * Chon danh sach trang thay vi danh sach den vi danh sach den luon chay sau:
     * con tai ve mot game moi la game do khong nam trong danh sach, khong ai chan.
     * Voi danh sach trang, app la mac dinh bi chan, bo khong phai lam gi.
     */
    var allowedPackages: Set<String>
        get() = sp.getStringSet(KEY_ALLOWED, emptySet()).orEmpty()
        set(v) = sp.edit().putStringSet(KEY_ALLOWED, v).commit().let {}

    /**
     * Cac app AI can ghi lai cau con hoi. Nap san tu [Defaults.AI_PACKAGES].
     *
     * De o Prefs chu khong dung thang hang so, de sau nay them mot app AI moi bang
     * lenh Telegram, khoi phai build lai APK va cai lai may.
     */
    var aiPackages: Set<String>
        get() = sp.getStringSet(KEY_AI_PKG, null) ?: Defaults.AI_PACKAGES
        set(v) = sp.edit().putStringSet(KEY_AI_PKG, v).commit().let {}

    /**
     * Cac app duoc phat tieng khi con da het gio choi, tuc la nghe nhac nen.
     *
     * Khac han danh sach trang. Danh sach trang cho MO APP RA nhin; cai nay chi cho
     * PHAT TIENG trong khi man hinh dang o cho khac, hoac dang tat. Hai thu tach
     * nhau vi chung khong cung mot viec: nghe nhac trong luc don phong thi duoc, con
     * ngoi luot Spotify chon bai nua tieng thi van la dung may ngoai gio.
     *
     * Chi la "duoc phep phat", khong phai "phat bao nhieu cung duoc": so phut moi
     * ngay dat rieng o [vn.huytl.homeworkgate.data.GioiHanApp], va phut phat tieng
     * duoc cong vao dung so do.
     */
    var nhacPackages: Set<String>
        get() = sp.getStringSet(KEY_NHAC, emptySet()).orEmpty()
        set(v) = sp.edit().putStringSet(KEY_NHAC, v).commit().let {}

    /**
     * Danh sach den: nhung app cam han, ke ca trong gio choi.
     *
     * Danh sach trang tra loi cau "cai gi duoc dung khi het gio". Cai nay tra loi
     * mot cau khac han: "cai gi khong bao gio duoc dung". Co gio choi thi moi app
     * deu mo, nen truoc day khong co cach nao noi "rieng cai nay thi khong".
     *
     * Khong dung duoc de chan man hinh chinh hay ban phim: [vn.huytl.homeworkgate
     * .guard.GuardAccessibilityService] xet mien tru he thong truoc, nen cham nham
     * vao do cung khong bien tablet thanh cuc gach.
     */
    var blockedPackages: Set<String>
        get() = sp.getStringSet(KEY_BLOCKED, emptySet()).orEmpty()
        set(v) = sp.edit().putStringSet(KEY_BLOCKED, v).commit().let {}

    /**
     * Cac khoa API cua AI, theo thu tu uu tien.
     *
     * Luu thanh mot chuoi moi dong mot khoa chu khong phai StringSet: StringSet
     * khong giu thu tu, ma o day thu tu chinh la luat - khoa dau tien con han muc
     * thi dung khoa do.
     */
    var aiKeys: List<String>
        get() = sp.getString(KEY_AI_KEYS, "").orEmpty()
            .split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        set(v) = sp.edit()
            .putString(KEY_AI_KEYS, v.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n"))
            .commit().let {}

    /** Chan luon app Settings va trung tam bao mat cua Xiaomi. */
    var lockSystemSettings: Boolean
        get() = sp.getBoolean(KEY_LOCK_SETTINGS, true)
        set(v) = sp.edit().putBoolean(KEY_LOCK_SETTINGS, v).commit().let {}

    /** Offset cho getUpdates cua Telegram. */
    var telegramOffset: Long
        get() = sp.getLong(KEY_TG_OFFSET, 0L)
        set(v) = sp.edit().putLong(KEY_TG_OFFSET, v).apply()

    /** message_id cua tin nhip tim. Moi lan ping chi sua lai tin nay. */
    var heartbeatMessageId: Long
        get() = sp.getLong(KEY_HEARTBEAT_MSG, 0L)
        set(v) = sp.edit().putLong(KEY_HEARTBEAT_MSG, v).apply()

    /**
     * Dau cua danh sach lenh da khai bao voi Telegram lan gan nhat.
     *
     * Khac dau thi khai lai. Khong goi moi lan mo dich vu: menu nam ben phia
     * Telegram, khai mot lan la xong.
     */
    var menuLenhDau: Int
        get() = sp.getInt(KEY_MENU_LENH, 0)
        set(v) = sp.edit().putInt(KEY_MENU_LENH, v).apply()

    /**
     * Ba Huy da danh dau la bat cong tac "Tu khoi dong" cua Xiaomi chua.
     *
     * Cong tac do nam trong app he thong, code khong doc duoc trang thai that, nen
     * chi hoi mot lan roi nho. Tat den day khong biet duoc; nhung nhac mai thi Ba
     * Huy quen nhin bang canh bao, ma bang do con dung cho nhung viec doc duoc.
     */
    var daXacNhanTuKhoiDong: Boolean
        get() = sp.getBoolean(KEY_TU_KHOI_DONG, false)
        set(v) = sp.edit().putBoolean(KEY_TU_KHOI_DONG, v).commit().let {}

    /**
     * Dien thoai Ba Huy da duoc ket nap vao nha tren Firestore chua.
     *
     * DUNG DE LAM GI: quyet dinh tablet co con phai nam cho Telegram lien tuc khong.
     * Co dien thoai roi thi lenh di duong Firestore, toi trong duoi mot giay qua cai
     * listener von da nam san trong dich vu tro nang; Telegram luc do la duong lui,
     * nam cho no 25 giay mot lan suot muoi tam tieng la tra tien cho mot duong khong
     * ai di. Xem [vn.huytl.homeworkgate.telegram.ApprovalService.nhipNgheMs].
     *
     * Chi bat len, khong bao gio tu tat: may bi go app thi tablet khong biet, ma
     * doan sai theo huong "chac la go roi" thi quay ve nam cho ca ngay. Ba Huy go
     * lenh Telegram trong luc khoa van chay, cham nhat nam phut.
     */
    var daNoiDienThoaiBa: Boolean
        get() = sp.getBoolean(KEY_NOI_DIEN_THOAI_BA, false)
        set(v) = sp.edit().putBoolean(KEY_NOI_DIEN_THOAI_BA, v).commit().let {}

    /**
     * Ma loi gan nhat Telegram tra ve, 0 la dang binh thuong.
     *
     * Chi giu 401 (token sai) va 409 (co may khac cung nghe mot bot). Hai cai do
     * lam moi lenh Telegram im lang hoan toan, ma truoc day khong cho nao noi ra:
     * go lenh khong thay gi, mo app cung khong thay gi, phai cam may cam dien thoai
     * doc log moi biet.
     */
    var loiTelegram: Int
        get() = sp.getInt(KEY_LOI_TELEGRAM, 0)
        set(v) = sp.edit().putInt(KEY_LOI_TELEGRAM, v).apply()

    /** So gio giua hai lan bao con song. */
    var heartbeatHours: Int
        get() = sp.getInt(KEY_HEARTBEAT_HOURS, 3)
        set(v) = sp.edit().putInt(KEY_HEARTBEAT_HOURS, v.coerceIn(1, 24)).commit().let {}

    var lastHeartbeatWall: Long
        get() = sp.getLong(KEY_HEARTBEAT, 0L)
        set(v) = sp.edit().putLong(KEY_HEARTBEAT, v).apply()

    /**
     * So lan go sai PIN lien tiep, xoa ve 0 khi go dung.
     *
     * Go sai vai lan lien tiep tren mot cai may ma dua tre dang cam nghia la no
     * dang thu. Bat duoc luc dang thu thi con kip doi PIN; bat duoc luc thu trung
     * thi da muon.
     */
    var saiPinLienTiep: Int
        get() = sp.getInt(KEY_WRONG_PIN, 0)
        set(v) = sp.edit().putInt(KEY_WRONG_PIN, v).commit().let {}

    // ===================== Phan soan tap vo =====================
    // Trang thai rieng cua viec soan cap theo thoi khoa bieu. Nam chung file voi
    // phan gio choi vi ca hai deu la trang thai cua cung mot cai tablet, va vi
    // nhu vay ca app chi co mot kho cau hinh duy nhat de ma hoa va sao luu.

    /** Bat tat toan bo tinh nang chan man hinh trong gio di hoc. */
    var batManChan: Boolean
        get() = sp.getBoolean(KEY_BAT_CHAN, true)
        set(v) = sp.edit().putBoolean(KEY_BAT_CHAN, v).commit().let {}

    /**
     * Ma buoi hoc ma Ba Huy da cho mo som. Man chan bo qua dung buoi nay thoi,
     * sang buoi sau lai chan binh thuong, khoi phai nho bat lai.
     */
    var buoiDuocMoSom: String
        get() = sp.getString(KEY_MO_SOM, "").orEmpty()
        set(v) = sp.edit().putString(KEY_MO_SOM, v).commit().let {}

    /**
     * Ma cac buoi hoc Le Hoa da bao la soan xong.
     *
     * Giu lai ca cac ma cu cung khong sao, moi ma chi vai chuc byte va mot nam
     * hoc chi co khoang bon tram buoi.
     */
    var buoiDaSoan: Set<String>
        get() = sp.getStringSet(KEY_DA_SOAN, emptySet()).orEmpty()
        private set(v) = sp.edit().putStringSet(KEY_DA_SOAN, v).commit().let {}

    fun danhDauDaSoan(maBuoi: String) {
        buoiDaSoan = buoiDaSoan + maBuoi
        buoiVuaSoan = maBuoi
    }

    /** Ba Huy bam bat lai vi thay soan thieu. */
    fun boDanhDau(maBuoi: String) {
        buoiDaSoan = buoiDaSoan - maBuoi
    }

    /** Ma buoi gan nhat Le Hoa bao da soan, de lenh /soanlai khoi phai go ma. */
    var buoiVuaSoan: String
        get() = sp.getString(KEY_VUA_SOAN, "").orEmpty()
        set(v) = sp.edit().putString(KEY_VUA_SOAN, v).commit().let {}

    // PIN cua bo, luu duoi dang SHA-256 co salt
    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp.edit()
            .putString(KEY_PIN_SALT, salt.toHex())
            .putString(KEY_PIN_HASH, hashPin(pin, salt))
            .commit()
    }

    /** Xoa PIN de dat lai. Goi tu Telegram khi bo quen PIN. */
    fun clearPin() {
        sp.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).commit()
    }

    fun hasPin(): Boolean = !sp.getString(KEY_PIN_HASH, null).isNullOrEmpty()

    fun checkPin(pin: String): Boolean {
        val hash = sp.getString(KEY_PIN_HASH, null) ?: return false
        val salt = sp.getString(KEY_PIN_SALT, null)?.fromHex() ?: return false
        return constantTimeEquals(hash, hashPin(pin, salt))
    }

    val isConfigured: Boolean
        get() = botToken.isNotEmpty() && parentChatId != 0L && hasPin()

    internal fun raw(): SharedPreferences = sp

    /**
     * Dien san nhung o con trong bang [Defaults].
     *
     * Chi dien o trong, khong de len cai da co: cai de len ban cu thi gio nghi hay
     * so luot Ba Huy da chinh van nguyen ven. Chay mot lan luc [Prefs] duoc tao.
     */
    private fun napMacDinhNeuTrong() {
        if (botToken.isEmpty()) botToken = Defaults.BOT_TOKEN
        if (parentChatId == 0L) parentChatId = Defaults.PARENT_CHAT_ID
        if (!sp.contains(KEY_GRANT_MINUTES)) grantMinutes = Defaults.GRANT_MINUTES
        if (!sp.contains(KEY_HARD_STOP)) hardStopMinuteOfDay = Defaults.HARD_STOP_MINUTE
        if (!sp.contains(KEY_GIO_DAY)) gioDayMinuteOfDay = Defaults.WAKE_MINUTE
        if (!sp.contains(KEY_TRAN_PHUT)) tranPhutMoiNgay = Defaults.TRAN_PHUT_MOI_NGAY
        if (!sp.contains(KEY_AI_KEYS)) aiKeys = Defaults.AI_KEYS
        // Khong dien PIN trong local.properties thi de may khong co PIN luon, chu
        // KHONG gieo chuoi rong: gieo rong la hasPin() hoa true va tu do bam OK
        // voi o trong la qua duoc. Khong co PIN thi o khoa dan thang vao Cai dat.
        if (!hasPin() && Defaults.PIN.isNotEmpty()) setPin(Defaults.PIN)
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return md.digest().toHex()
    }

    /** So sanh khong thoat som, tranh ro ri do dai qua thoi gian phan hoi. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    companion object {
        private const val FILE_NAME = "gate_prefs"

        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_PARENT_CHAT_ID = "parent_chat_id"
        private const val KEY_GRANT_MINUTES = "grant_minutes"
        private const val KEY_HARD_STOP = "hard_stop_minute"
        private const val KEY_GIO_DAY = "gio_day_minute"
        private const val KEY_TRAN_PHUT = "tran_phut_moi_ngay"
        private const val KEY_ALLOWED = "allowed_packages"
        private const val KEY_AI_PKG = "ai_packages"
        private const val KEY_BLOCKED = "blocked_packages"
        private const val KEY_NHAC = "nhac_packages"
        private const val KEY_LOCK_SETTINGS = "lock_settings"
        private const val KEY_TG_OFFSET = "tg_offset"
        private const val KEY_HEARTBEAT = "heartbeat_wall"
        private const val KEY_HEARTBEAT_MSG = "heartbeat_msg"
        private const val KEY_MENU_LENH = "menu_lenh_ban"
        private const val KEY_HEARTBEAT_HOURS = "heartbeat_hours"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_WRONG_PIN = "sai_pin_lien_tiep"
        private const val KEY_TU_KHOI_DONG = "da_xac_nhan_tu_khoi_dong"
        private const val KEY_LOI_TELEGRAM = "loi_telegram"
        private const val KEY_NOI_DIEN_THOAI_BA = "da_noi_dien_thoai_ba"
        private const val KEY_AI_KEYS = "ai_keys"
        private const val KEY_BAT_CHAN = "bat_man_chan"
        private const val KEY_MO_SOM = "buoi_duoc_mo_som"
        private const val KEY_DA_SOAN = "buoi_da_soan"
        private const val KEY_VUA_SOAN = "buoi_vua_soan"

        @Volatile
        private var instance: Prefs? = null

        /** Cac kho ma hoa phu, theo ten file. */
        private val khoPhu = mutableMapOf<String, SharedPreferences>()

        /**
         * Mot kho ma hoa RIENG, khong dung chung file voi [FILE_NAME].
         *
         * Co cai nay vi [vn.huytl.homeworkgate.dongbo.DongBo] nghe ca file prefs
         * chinh de biet khi nao can day trang thai sang dien thoai Ba Huy. Thu gi
         * ghi deu dan ma khong lien quan den trang thai - nhat ky con mo app nao,
         * chang han - ma nam chung file thi cu moi lan ghi lai keo theo mot luot
         * ghi Firestore, tuc la mot lan bat song cho khong.
         *
         * Van ma hoa nhu kho chinh: nhat ky con dung app gi luc nao khong phai thu
         * de nam tran trong may.
         */
        fun khoRieng(context: Context, ten: String): SharedPreferences =
            synchronized(khoPhu) {
                khoPhu.getOrPut(ten) {
                    val ung = context.applicationContext
                    val masterKey = MasterKey.Builder(ung)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        ung,
                        ten,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                }
            }

        fun get(context: Context): Prefs = instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }

        private fun create(context: Context): Prefs {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val sp = EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            return Prefs(sp).apply { napMacDinhNeuTrong() }
        }

        private fun ByteArray.toHex(): String =
            joinToString("") { "%02x".format(it) }

        private fun String.fromHex(): ByteArray =
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
