package vn.huytl.homeworkgate.guard

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.huytl.homeworkgate.App
import vn.huytl.homeworkgate.data.BoGoAi
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.NhatKyAi
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.ui.HomeActivity
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.telegram.Notifier
import vn.huytl.homeworkgate.telegram.TelegramClient
import vn.huytl.homeworkgate.R

/**
 * Canh xem app nao dang o truoc mat va chan neu khong con gio.
 *
 * Dung danh sach trang: het gio thi chan tat ca, tru nhung app bo duyet cho dung
 * moi luc. Danh sach den khong dung duoc vi con tai app moi ve la thoat, con bo
 * thi phai chay theo bo sung tung cai mot.
 *
 * Chon Accessibility thay vi UsageStatsManager vi Accessibility bao ngay luc cua
 * so doi, con UsageStats phai hoi vong nen luon cham vai giay. Doi lai, day la
 * quyen manh, nen app se khong len Play Store duoc, va cung vi the no chi cai
 * bang tay qua adb.
 */
class GuardAccessibilityService : AccessibilityService() {

    private lateinit var gate: GateStore
    private lateinit var prefs: Prefs
    private val mainHandler = Handler(Looper.getMainLooper())

    private val xetLaiRunnable = Runnable {
        currentPackage?.let { runCatching { evaluate(it) } }
    }

    /**
     * Xet lai sau khi mot chum su kien "danh sach cua so doi" da lang xuong.
     *
     * Xem [GOP_CUA_SO_MS] de biet vi sao phai hoan. Dung mot Runnable RIENG, khong
     * dung ke [xetLaiRunnable]: cai kia bi [henXetLai] huy va dat lai theo nhip
     * chong day lien tuc, hai duong dung chung mot Runnable thi moi cai huy nhau.
     */
    private val gopCuaSoRunnable = Runnable {
        runCatching { evaluate(currentPackage ?: packageName) }
        syncTicker()
    }

    /** Nho san goi nao mo duoc tu man hinh chinh, khoi hoi PackageManager moi lan. */
    private val moDuocTuNha = HashMap<String, Boolean>()

    /** Goi vua bi day di gan nhat, va luc day, de khong day lien tuc mot cho. */
    private var goiVuaChan = ""
    private var lucVuaChan = 0L

    /** So lan app do van con tren man hinh ngay sau khi vua bi day di. */
    private var khongChiuDi = 0

    /** Da bao Ba Huy ve dot nay chua, de khong nhan mot chuc tin giong nhau. */
    private var daBaoKhongDay = false

    /** Lan nhac "dong cua so noi" gan nhat, de giu dung nhip. */
    private var lucNhacCuoi = 0L

    /** Luc bat dau dot "khong day duoc" nay, de biet no keo dai bao lau. */
    private var batDauDot = 0L

    /** Lan gan nhat nhan tin bao cho Ba Huy ve chuyen nay. */
    private var lucBaoCuoi = 0L

    /** Lan day nay da tinh mot diem "khong chiu di" chua. */
    private var daDemLanNay = false

    /** Ba nuoc di co the chon moi lan thay mot app khong duoc phep. */
    private enum class Day { DAY, KHONG_DAY_DUOC, CHO_DA }

    /** App co dat han rieng dang duoc xem, va moc bat dau dem. */
    private val dangDemGio = HashMap<String, Long>()

    /** Mot app dang nam truoc mat: bat dau luc nao, lan cuoi nhin thay luc nao. */
    private class PhienXem(val tu: Long) {
        var thay = tu
        var ghiCuoi = tu
    }

    /** App dang nam truoc mat, cho so ghi su dung. Xem [capNhatSuDung]. */
    private val dangXem = HashMap<String, PhienXem>()

    /** Ten hien thi cua tung goi, hoi mot lan roi nho. Xem [tenNho]. */
    private val tenDaBiet = HashMap<String, String>()

    /** Luc man hinh cua app Nop bai hien len, 0 la dang khong hien. Xem [baoTruocMat]. */
    private var appNhaTu = 0L

    /**
     * Cac goi hien lan cuoi con doc duoc danh sach cua so. Thanh thong bao dang keo
     * xuong thi day la cai nam duoi no. Xem [docManHinh].
     */
    @Volatile
    private var hienTruocKhiKeo: Set<String> = emptySet()

    /** Goi dang bi che duoi thanh thong bao, de moi lan keo chi ghi mot dong. */
    private var goiCheDuoi = ""

    /** Lan cuoi ghi nhat ky chuyen keo thanh thong bao. Xem [cheDuoiThongBao]. */
    private var lucGhiCheDuoi = 0L

    /**
     * Nhip nua phut trong luc co app truoc mat: ghi so su dung, dem gio rieng, roi
     * xet lai luat chan.
     *
     * TRUOC DAY gio rieng chi duoc dem khi co su kien cua so. Ngoi doc mot trang hay
     * xem mot video khong tieng thi nhieu phut lien khong co su kien nao, ma
     * [chotGio] bo han khoang nao dai hon mot phut. App dat 30 phut vi the xai duoc
     * lau hon nhieu moi khoa.
     *
     * Viec chan cung vay: chi chay khi co su kien cua so. Nhung co luat doi theo
     * dong ho ma man hinh khong doi gi: het gio rieng giua chung, hay toi gio ngu
     * trong luc con dang doc trong app danh sach trang. Xet lai o day thi cham nhat
     * nua phut la app bi day ra, khong phai doi con cham vao dau do.
     */
    private val nhipSuDungRunnable = Runnable {
        runCatching {
            val hien = goiDangHienNeuSang()
            demGioTungApp(hien)
            capNhatSuDung(hien)
            if (hien.isNotEmpty()) evaluate(currentPackage ?: packageName)
        }
    }

    /**
     * Cac goi dang phat tieng va dang duoc phep phat.
     *
     * Giu lai giua hai nhip vi phan dem gio can biet app nao van con keu trong khi
     * khong co cua so nao cua no tren man hinh - chinh la luc nghe nhac nen.
     */
    @Volatile
    private var nhacDangPhat: Set<String> = emptySet()

    private val nhipNhacRunnable = Runnable { runCatching { xetNhac() } }

    /** So lan lien tiep da bao mot goi dung ma no van con keu. */
    private val soLanBaoDung = HashMap<String, Int>()

    /** Nhung goi lam man hinh chinh. */
    private var goiManHinhChinh: Set<String> = emptySet()

    private lateinit var overlay: BlockOverlay
    private lateinit var dongHo: CountdownOverlay

    /** Nhip mot giay cho dong ho dem nguoc. Chi chay trong luc dang choi. */
    private var nhipGiay: Job? = null

    private var scope: CoroutineScope? = null
    private var ticker: Job? = null

    /** Package launcher va ban phim, doc mot lan roi nho, khong hoi lai moi su kien. */
    private var systemEssentials: Set<String> = emptySet()

    /**
     * App cua nguoi khac dang o truoc mat. Cua so cua chinh app nay khong duoc ghi
     * vao day.
     *
     * Truoc day moi cua so deu ghi de len bien nay, ke ca lop phu "con 5 phut" do
     * chinh minh dung len. Hau qua: den luc het gio, [onTick] hoi "con dang mo cai
     * gi" thi nhan duoc ten cua chinh minh, [evaluate] thay do la app nha nen bo
     * qua, va dua tre ngoi choi tiep den khi nao no tu chuyen sang man hinh khac.
     * Dung canh mot gio choi 60 phut thi loi nay luon xay ra, vi lop phu canh bao
     * hien o phut thu 55.
     */
    @Volatile
    private var currentPackage: String? = null

    /** Dang nghe su kien go chu hay khong. Xem [ngheChuAi]. */
    private var dangNgheChu = false

    /** Gom chu con go vao app AI de dung lai cau hoan chinh. Xem [BoGoAi]. */
    private val boGoAi = BoGoAi()

    /**
     * Chot cau con go xong roi de do: khong bam gui, khong roi app.
     *
     * [BoGoAi] co san luat "o nhap im qua lau thi coi nhu xong", nhung truoc day khong
     * cho nao goi toi. Cau go xong ma khong gui nam trong bo gom cho den lan con mo
     * app khac, co khi hang tieng sau, va Ba Huy khong biet gi trong luc do.
     *
     * Handler dung khi CPU ngu, nen tat man hinh ngay sau khi go thi lan hen nay tre.
     * Canh do [manHinhReceiver] lo: tat man hinh la chot luon.
     */
    private val imLauRunnable = Runnable {
        runCatching { boGoAi.imLau(SystemClock.elapsedRealtime())?.let { chotCauHoi(it) } }
    }

    /** App AI con dang go do. Nho lai vi luc chot cau, con co the da sang app khac. */
    private var goiAiDangGo = ""


    private var warned = false

    private var audioManager: AudioManager? = null

    /**
     * Man hinh tat lau hon [PAUSE_AFTER_SCREEN_OFF_MS] thi khong tinh la dang choi.
     *
     * Day la ly do co cai nay: truoc day bam Bat dau la dong ho chay mot mach den
     * het, nen di an com nua tieng la mat nua tieng. Cai do day dua tre ngoi li
     * khong dam roi may, dung nguoc voi viec minh muon.
     *
     * Tat man hinh vai giay roi bat lai thi van tinh la choi, khong gian doan gi.
     */
    private val manHinhReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (gate.state == GateState.ACTIVE) {
                        gate.screenOffAtWall = System.currentTimeMillis()
                    }
                    dongSuDung()
                    // Tat man hinh la con khong go tiep nua. Chot ngay o day, khong
                    // doi [imLauRunnable]: lan hen do dung theo CPU khi may ngu.
                    chotCauDangGo()
                }
                Intent.ACTION_SCREEN_ON -> batManHinhLen()
            }
        }
    }


    /**
     * Dang trong mot dot chan nhac khi chua co quyen doc thong bao, de nhat ky chi ghi
     * mot dong moi dot chu khong ghi moi nhip xet lai. Xem [xetNhacKhiChuaCoQuyen].
     */
    private var dangChanNhacMu = false

    /** Lan gan nhat doc lai danh sach mien tru he thong. */
    private var lanDocMienTru = 0L

    /**
     * Con bam play lai tu thanh thong bao thi khong co cua so nao mo ra, nen
     * [onAccessibilityEvent] khong chay va tieng cu the ma phat. Cho nay nghe
     * thang su kien phat tieng, roi de [xetNhac] xet - ca khi co lan khi chua co
     * quyen doc thong bao.
     *
     * Truoc 25/9/2026 duong chua co quyen bat im ngay tai day, roi nghi ba giay moi
     * cho bat lan nua. Ba giay do la cho lach: bat im chi lam he thong van nho tieng
     * va hai giay sau moi bao app kia mat quyen, con bam Phat lai trong luc nghi la
     * nhac chay mai vi khong con nhip nao xet lai. Gio moi viec nam o
     * [xetNhacKhiChuaCoQuyen], va no tu hen xet lai den khi may het tieng.
     */
    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            if (ParentMode.isActive(this@GuardAccessibilityService)) return

            // Ca luc danh sach rong cung phai xet: do la luc nhac vua tat, va phan
            // vua nghe con dang cho chot vao so.
            //
            // Hoan nua giay roi moi xet: doi mot bai hat ban ra ba bon su kien lien
            // tiep, ma moi lan xet la mot luot hoi he thong danh sach trinh phat va
            // danh sach cua so. Gop chum lai thanh mot lan, giong [gopCuaSoRunnable].
            mainHandler.removeCallbacks(nhipNhacRunnable)
            mainHandler.postDelayed(nhipNhacRunnable, GOP_TIENG_MS)
        }
    }

    /**
     * Khoi tao o day chu khong phai trong [onServiceConnected].
     *
     * Android co the bind roi unbind ma chua kip goi onServiceConnected - hay gap
     * khi he thong giet tien trinh, hoac khi tat bat cong tac tro nang. Luc do
     * onUnbind cham vao bien lateinit chua khoi tao va app CHET. Android thay dich
     * vu tro nang crash thi danh dau hong va tu choi bind lai: cong tac van bat,
     * khong con ai chan app nao, phai vao Cai dat tat bat tay moi song lai.
     *
     * onCreate thi luon chay truoc, nen dat o day la het duong crash.
     */
    override fun onCreate() {
        super.onCreate()
        gate = GateStore(this)
        prefs = Prefs.get(this)
        overlay = BlockOverlay(this)
        dongHo = CountdownOverlay(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Tu dang ky loai su kien go chu ngay o day, khong chi trong cau hinh XML.
        //
        // Vao day la TAT su kien go chu, roi chi bat len khi mot app AI ra truoc
        // mat. Xem [ngheChuAi].
        //
        // (Android nho cau hinh dich vu tro nang tu luc BAT no trong Settings. Cai
        // mot ban moi len - qua adb hay qua ban cap nhat - KHONG lam no doc lai
        // XML, nen sua tay serviceInfo luc ket noi la cach duy nhat de cau hinh moi
        // co hieu luc ngay, khoi bat Ba Huy vao Settings tat bat lai dich vu.)
        ngheChuAi(false, batBuoc = true)

        docLaiDanhSachHeThong()
        dangChay = true

        // Duong sang app Bang dieu khien di ke dich vu nay.
        //
        // Day la tien trinh duy nhat trong app song 24/7: Android giu dich vu tro
        // nang, ke ca khi cong dang khoa va ApprovalService da tu tat. Gan listener
        // Firestore vao day thi lenh cua Ba Huy toi trong duoi mot giay ma khong
        // phai mo them mot service nen nao, khong phai nhung khoa may chu cho FCM.
        runCatching { DongBo.batDau(this) }
            .onFailure { Log.w(TAG, "khong bat duoc dong bo: ${it.message}") }
        registerReceiver(
            manHinhReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
        )
        audioManager = getSystemService(AudioManager::class.java)?.also {
            it.registerAudioPlaybackCallback(playbackCallback, Handler(Looper.getMainLooper()))
        }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        // Ke ca so goi man hinh chinh: bang 0 nghia la phep mien tru cho o tim kiem
        // va tro ly dang chet, va do la thu khong nhin ra duoc tu ben ngoai - no chi
        // hien hinh bang mot dong "Khong day duoc Google" trong nhat ky.
        Log.i(TAG, "service len, mien tru=${systemEssentials.size} app, " +
            "man hinh chinh=${goiManHinhChinh.size} goi, " +
            "danh sach trang=${prefs.allowedPackages.size} app, state=${gate.state}")
        baoNeuVuaVangMat()
        syncTicker()
        // Dich vu vua bi giet giua luc dang co nhac thi nhip dem da mat theo. Hoi
        // lai ngay o day, khong doi den lan doi bai tiep theo.
        runCatching { xetNhac() }
        // Chup ngay "dang mo gi", khong doi su kien cua so dau tien. Khong thi ban
        // day len dau tien mang gia tri khoi dau - "man hinh tat" - va Bang dieu
        // khien noi sai cho den khi co ai cham vao may.
        runCatching { capNhatSuDung() }
    }

    /**
     * Vua quay lai sau khi vang mat giua mot phien dang chay thi bao cho ba biet.
     *
     * So phut da tu tru roi, nen tin nay khong phai de xu ly gi, ma de ba phan biet
     * duoc hai chuyen deu dan den "con choi ma khong bi chan": may tu giet tien
     * trinh, va con co tinh tat dich vu. Vang vai giay thi im lang, khong lam phien.
     */
    private fun baoNeuVuaVangMat() {
        val moc = gate.guardGoneAtWall
        if (moc == 0L) return
        gate.guardGoneAtWall = 0L
        if (gate.state != GateState.ACTIVE) return

        val vangMs = System.currentTimeMillis() - moc
        if (vangMs < REPORT_ABSENCE_MS) return

        val phut = vangMs / 60_000 + 1
        Log.i(TAG, "vua vang mat $phut phut giua phien")
        Notifier.send(
            this,
            "Dịch vụ canh app vừa vắng mặt khoảng $phut phút trong lúc " +
                "Lê Hòa đang chơi, nên lúc đó máy không chặn gì. " +
                "Số phút vẫn bị trừ bình thường, phiên đang chạy tiếp.\n" +
                "Nếu chuyện này lặp lại nhiều lần thì nên xem lại phần tiết kiệm pin " +
                "của HyperOS cho app Nộp bài."
        )
    }

    /**
     * Bat hay tat viec nghe su kien "chu trong o nhap vua doi".
     *
     * Su kien do la thu duy nhat mang theo noi dung, va la duong duy nhat ghi lai
     * duoc cau Le Hoa go vao app AI. Nhung he thong ban no cho MOI app: con nhan
     * tin, tim kiem, dien mot o bat ky - moi ky tu la mot lan danh thuc tien trinh
     * nay, de roi no xem goi khong nam trong danh sach AI va bo di. Hang nghin lan
     * mot ngay khong de lam gi.
     *
     * Nen mac dinh tat, chi bat dung luc mot app AI ra truoc mat. Phan ghi cau hoi
     * khong doi gi: luc con go thi app do dang duoc tieu diem, tuc la co da bat.
     */
    private fun ngheChuAi(bat: Boolean, batBuoc: Boolean = false) {
        if (!batBuoc && bat == dangNgheChu) return
        runCatching {
            serviceInfo = serviceInfo.apply {
                eventTypes = if (bat) {
                    eventTypes or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                } else {
                    eventTypes and AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED.inv()
                }
            }
            dangNgheChu = bat
        }.onFailure { Log.w(TAG, "khong doi duoc eventTypes: ${it.message}") }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val loai = event?.eventType ?: return

        // Vao hoac ra khoi che do chia doi man hinh co khi chi ban su kien
        // "danh sach cua so doi" chu khong ban "cua so truoc mat doi". Bo qua no la
        // bo sot dung luc can xet lai nhat.
        //
        // Nhung khong xet NGAY: gop ca chum lai roi xet mot lan. Xem [GOP_CUA_SO_MS].
        if (loai == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            mainHandler.removeCallbacks(gopCuaSoRunnable)
            mainHandler.postDelayed(gopCuaSoRunnable, GOP_CUA_SO_MS)
            return
        }

        // Con go chu vao app AI. Chi loai su kien nay mang theo noi dung, va chi
        // xet no cho dung may app trong danh sach - khong doc chu o bat cu app nao
        // khac. Xem [batChuAi].
        if (loai == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            batChuAi(event)
            return
        }

        if (loai != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // Doi han cua so truoc mat thi xet NGAY, khong hoan: day moi la su kien quyet
        // dinh chan hay khong, va no thua hon han loai kia nen khong can gop. Lan gop
        // dang treo, neu co, tro thanh thua - [evaluate] ngay duoi doc danh sach cua
        // so ngay luc nay, tuc la da bao gom moi thay doi ma lan gop do dinh xet.
        mainHandler.removeCallbacks(gopCuaSoRunnable)

        val pkg = event.packageName?.toString() ?: return
        // Ban phim hien len cung ban mot su kien cua so, mang ten goi cua ban phim.
        // No khong phai app con vua mo, no de len tren app dang dung. Ghi no vao day
        // thi lan chan sau hoi "con dang mo gi" se nhan ve ten ban phim.
        val laPhim = laBanPhim(pkg)
        if (pkg != packageName && !laPhim) {
            currentPackage = pkg
            goiVuaMo = pkg
        }
        xetNgheChuAi(pkg, laPhim)
        // Thanh thong bao cung ban su kien nay, mang ten giao dien he thong. No khong
        // phai app vua mo ben duoi thanh, nen khong truyen vao.
        val goiSuKienMoi = pkg.takeIf { it != packageName && !laPhim && it !in ALWAYS_ALLOWED }
        evaluate(pkg, goiSuKienMoi)
        syncTicker()
    }

    /**
     * Bat hay tat nghe chu theo viec con con o trong app AI khong, va chot cau dang
     * go khi con da roi di.
     *
     * TRUOC DAY nhin moi goi cua su kien: goi nao khong phai app AI la coi nhu con da
     * roi app AI. Hong ngay tu cai cham dau tien - cham vao o nhap thi ban phim hien
     * len, ban mot su kien cua so mang ten ban phim, va viec nghe chu tat truoc khi
     * con go chu dau tien. Thu tren may ao: Chrome khong ghi duoc chu nao; Danh ba
     * ghi duoc nhung mat may chu dau, vi no tu ban them su kien cua so nen nghe bat
     * lai giua chung.
     *
     * Nen gio ban phim thi bo qua han. Goi khac khong phai app AI thi hoi them mot
     * cau: app AI con cua so nao tren man hinh khong. Hop thoai he thong, lop phu cua
     * chinh app nay, nua man hinh ben kia khi chia doi - deu la cua so nam canh hay de
     * len tren; app AI con do nghia la con chua roi. Chi hoi khi dang nghe, tuc la luc
     * app AI vua truoc mat, nen gan nhu ca ngay khong ton gi.
     */
    private fun xetNgheChuAi(pkg: String, laPhim: Boolean) {
        if (laPhim) return
        val appAi = prefs.aiPackages
        val conOAi = pkg in appAi || (dangNgheChu && goiDangHien().any { it in appAi })
        ngheChuAi(conOAi)
        // Con roi app AI sang app khac: cau dang go do, neu co, la da xong.
        if (!conOAi) chotCauDangGo()
    }

    /** Chot ngay cau dang go do, neu co. Dung khi chac chan con khong go tiep nua. */
    private fun chotCauDangGo() {
        mainHandler.removeCallbacks(imLauRunnable)
        boGoAi.roiApp()?.let { chotCauHoi(it) }
    }

    /**
     * Bat mot anh chup o nhap cua app AI, dua vao [boGoAi] gom lai.
     *
     * Bo qua o nhap mat khau: mac du app AI hiem khi co, day la nguyen tac - khong
     * bao gio ghi lai mot o danh dau la mat khau.
     */
    private fun batChuAi(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in prefs.aiPackages) return
        if (event.isPassword) return
        // Dang mo toan bo may thi nguoi go la Ba Huy. Ghi vao day la cau cua ba thanh
        // "Le Hoa hoi ..." tren Telegram va trong so hoi AI cua con.
        //
        // Van de nghe chu bat, chi bo o day: tat nghe thi luc ba khoa may lai, app AI
        // van dang mo, khong co su kien cua so nao de bat nghe lai - con go gi sau do
        // cung mat. Cau con go do TRUOC khi ba mo may van nam trong [boGoAi] va van
        // duoc chot binh thuong, vi do dung la cau cua con.
        if (ParentMode.isActive(this)) return
        val text = event.text?.joinToString(" ")?.trim().orEmpty()
        goiAiDangGo = pkg
        mainHandler.removeCallbacks(imLauRunnable)
        val cau = boGoAi.goChu(text, SystemClock.elapsedRealtime())
        if (cau != null) {
            chotCauHoi(cau)
        } else if (text.isNotEmpty()) {
            // Con dang go do. Hen xem lai khi o nhap da im du lau; phim moi thi hen
            // lai tu dau, nen chi lan hen cua phim cuoi cung la chay. Xem [imLauRunnable].
            mainHandler.postDelayed(imLauRunnable, boGoAi.imMs + 500L)
        }
    }

    /**
     * Ghi mot cau con vua hoi AI, va gui thang cho Ba Huy.
     *
     * Gui ngay chu khong doi den toi: Ba Huy doc duoc luc con dang lam bai, con kip
     * hoi lai "cho ba xem con vua nho AI cai gi". Doi mot ngay thi cau chuyen nguoi.
     *
     * Gui trong runCatching tren luong IO: mat mang khong duoc phep lam chet dich vu
     * canh, va cau van con trong nhat ky de xem lai bang /hoi.
     */
    private fun chotCauHoi(cau: String) {
        val ten = tenApp(goiAiDangGo.ifEmpty { currentPackage.orEmpty() })
        val dong = NhatKyAi.ghi(this, ten, cau)
        Log.i(TAG, "ghi cau hoi AI: $dong")

        val token = prefs.botToken
        val chat = prefs.parentChatId
        if (token.isEmpty() || chat == 0L) return
        scope?.launch(Dispatchers.IO) {
            runCatching {
                TelegramClient(token).sendMessage(
                    chat,
                    "${getString(R.string.child_name)} hỏi $ten:\n$cau"
                )
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        dangChay = false
        runCatching { DongBo.dungLai() }
        // Boc trong runCatching: mot ngoai le o day lam CA APP chet, va hau qua nang
        // hon nhieu so voi viec don dep khong sach.
        return runCatching { donDepKhiRoi() }
            .getOrElse { Log.w(TAG, "loi khi don dep: ${it.message}"); false }
            .let { super.onUnbind(intent) }
    }

    private fun donDepKhiRoi(): Boolean {
        runCatching { unregisterReceiver(manHinhReceiver) }
        mainHandler.removeCallbacks(gopCuaSoRunnable)
        mainHandler.removeCallbacks(xetLaiRunnable)
        mainHandler.removeCallbacks(nhipNhacRunnable)
        // Cau dang go do van ghi vao so truoc khi di. Phai lam truoc khi huy scope:
        // tin gui Telegram chay trong scope do.
        runCatching { chotCauDangGo() }
        // Chot not khoang dang mo truoc khi di, khong thi phan da xem tu lan ghi
        // cuoi den bay gio mat khoi so.
        runCatching { dongSuDung() }
        audioManager?.unregisterAudioPlaybackCallback(playbackCallback)
        audioManager = null
        ticker?.cancel()
        ticker = null
        nhipGiay?.cancel()
        nhipGiay = null
        scope?.cancel()
        overlay.hide()
        dongHo.hide()
        // Chi ghi lai moc roi di, khong cat phien.
        //
        // Truoc day cho nay cat phien, voi ly do "khong thi con tat service, choi
        // thoai mai, roi bat lai va van con nguyen so phut cu". Ly do do sai: hai
        // dong ho dem gio deu la dong ho tuyet doi, chung chay tiep ca khi service
        // da chet. Vang mat ba muoi phut la mat dung ba muoi phut, khong ai giu ho
        // cho. Nen viec cat phien khong chan duoc gi ma chi lam hai dung mot nguoi:
        // dua tre bi khoa ngang khi HyperOS tien tay giet tien trinh nen.
        if (gate.state == GateState.ACTIVE) {
            gate.guardGoneAtWall = System.currentTimeMillis()
        }
        return true
    }

    /**
     * Vong dem nguoc chi song trong luc phien mo. Cong khoa thi khong co gi de dem,
     * su kien cua so du de quyet dinh chan hay khong, nen khong can danh thuc CPU.
     */
    /**
     * Dong ho dem nguoc chi chay trong luc dang choi that.
     *
     * Tam dung, cho duyet hay dang khoa deu khong co gi de dem, va mot cua so noi
     * tren moi thu suot ngay thi phien. Man hinh tat thi phien cung da dung.
     */
    private fun syncDongHo() {
        val canDem = gate.state == GateState.ACTIVE && !ParentMode.isActive(this)
        if (canDem && nhipGiay == null) {
            nhipGiay = scope?.launch {
                while (true) {
                    val conLai = gate.remainingMs()
                    if (conLai <= 0L || gate.state != GateState.ACTIVE) break
                    dongHo.show(CountdownOverlay.dinhDang(conLai))
                    delay(1_000)
                }
                dongHo.hide()
                nhipGiay = null
            }
        } else if (!canDem) {
            nhipGiay?.cancel()
            nhipGiay = null
            dongHo.hide()
        }
    }

    private fun syncTicker() {
        val needTicker = gate.state == GateState.ACTIVE || ParentMode.isActive(this)
        if (needTicker && ticker == null) {
            warned = false
            ticker = scope?.launch {
                while (true) {
                    delay(TICK_INTERVAL_MS)
                    runCatching { onTick() }
                    val stillNeeded = gate.state == GateState.ACTIVE ||
                        ParentMode.isActive(this@GuardAccessibilityService)
                    if (!stillNeeded) break
                }
                ticker = null
            }
        } else if (!needTicker) {
            ticker?.cancel()
            ticker = null
        }
        syncDongHo()
    }

    /**
     * Man hinh vua sang lai. Tra lai khoang thoi gian tat man hinh neu no du dai,
     * roi cho phien chay tiep.
     */
    private fun batManHinhLen() {
        val moc = gate.screenOffAtWall
        gate.screenOffAtWall = 0L
        val tatMs = if (moc == 0L) 0L else System.currentTimeMillis() - moc

        when {
            // Vong dem da kip dung phien lai trong luc man hinh tat.
            gate.state == GateState.PAUSED -> {
                val phut = gate.resume()
                Log.i(TAG, "man hinh sang lai, chay tiep ${phut ?: 0} phut")
                overlay.hide()
            }
            // Vong dem khong kip chay (tien trinh bi dong bang): tra lai o day.
            gate.state == GateState.ACTIVE && tatMs >= PAUSE_AFTER_SCREEN_OFF_MS -> {
                gate.pause(creditMs = tatMs)
                val phut = gate.resume()
                Log.i(TAG, "tat man hinh ${tatMs / 60_000} phut, tra lai, con ${phut ?: 0} phut")
            }
        }
        syncTicker()
        capNhatSuDung()
        runCatching { xetNhac() }
    }

    /** Man hinh dang tat va da tat du lau chua. */
    private fun nghiVoiManHinhTat(): Long {
        val moc = gate.screenOffAtWall
        if (moc == 0L) return 0L
        val pm = getSystemService(PowerManager::class.java)
        if (pm?.isInteractive == true) return 0L
        return System.currentTimeMillis() - moc
    }

    private fun onTick() {
        // Man hinh tat du lau thi dung dong ho lai ngay, de phien khong chet trong
        // luc khong ai dung may.
        val tatMs = nghiVoiManHinhTat()
        if (tatMs > 0L) chotHetGio()
        if (gate.state == GateState.ACTIVE && tatMs >= PAUSE_AFTER_SCREEN_OFF_MS) {
            val phut = gate.pause(creditMs = tatMs)
            gate.screenOffAtWall = System.currentTimeMillis()
            Log.i(TAG, "man hinh tat ${tatMs / 60_000} phut, tam dung o ${phut ?: 0} phut")
            syncTicker()
            return
        }

        val reason = gate.tick()
        if (reason != null) {
            Notifier.sessionEnded(this, reason)
            // Im tieng ngay khi cat phien, khong doi den luc con mo lai app: neu
            // dang nghe nhac thi day ve man hinh chinh van con nghe duoc.
            batImVaXetLai()
            warned = false
            // Chot lai so dung app ngay tai day. Duong binh thuong la evaluate()
            // ben duoi day con ra khoi game, va viec doi cua so tu chot ho. Nhung
            // khi con dang o trong chinh app nay thi khong co cu day nao ca, va
            // khoang dang mo nam do cho den nhip sau.
            runCatching { capNhatSuDung() }
            Log.i(TAG, "het phien ($reason), app truoc mat=$currentPackage, " +
                "man hinh app nha dang mo=${App.manHinhCuaAppDangMo}")
            // Dang o trong app nay thi khong day di dau ca: co the con dang go tin
            // nhan cho ba. Man hinh chinh tu no hien "dang khoa" khi ve tay.
            if (!App.manHinhCuaAppDangMo) currentPackage?.let { evaluate(it) }
            return
        }
        // Tu sua neu phien bat dau ma chua co su kien cua so nao de danh thuc.
        syncDongHo()

        val left = gate.remainingMs()
        if (left in 1..WARN_BEFORE_MS && !warned) {
            warned = true
            // Nhac ngay trong dong ho dem nguoc, khong bung lop phu to bang man hinh
            // nua: dang choi ma bi che mat va bam khong an trong ba giay thi chinh
            // cai loi nhac lai la thu lam con buc minh nhat.
            dongHo.nhac("Còn ${left / 60_000 + 1} phút, chuẩn bị dừng nhé", 6_000L)
        }
    }

    /**
     * Cong gio cho nhung app co dat han rieng.
     *
     * Dem theo cua so dang hien chu khong theo su kien: chia doi man hinh thi ca hai
     * ben deu dang duoc xem, va app nam duoi thanh thong bao thi da bi loai tu truoc
     * khi vao day.
     *
     * Moc thoi gian lay tu dong ho tuong doi, khong phai dong ho he thong - doi gio
     * may khong lam so nay nhay.
     */
    private fun demGioTungApp(hien: Set<String>) {
        val bayGio = SystemClock.elapsedRealtime()
        // Cong ca app dang phat tieng ma khong co cua so nao: nghe nhac nen la dung
        // truong hop do, va no phai an vao cung mot so phut moi ngay.
        val coHan = (hien + nhacDangPhat).filter { GioiHanApp.han(this, it) > 0 }.toSet()

        // App vua roi khoi man hinh: chot not phan vua xem.
        dangDemGio.keys.toList().forEach { pkg ->
            if (pkg !in coHan) {
                chotGio(pkg, bayGio)
            }
        }
        // App dang hien: chot phan da troi qua roi dat moc moi, de mat dien giua
        // chung cung chi mat mot nhip hai muoi giay.
        coHan.forEach { pkg ->
            val moc = dangDemGio[pkg]
            if (moc != null) chotGio(pkg, bayGio)
            dangDemGio[pkg] = bayGio
        }
    }

    /** Ghi phan da xem cua [pkg] tinh den [bayGio] vao so cua ngay. */
    private fun chotGio(pkg: String, bayGio: Long) {
        val moc = dangDemGio.remove(pkg) ?: return
        val troiQua = bayGio - moc
        // Mot phut la tran tren cho moi lan chot: nhip dem la nua phut (xem
        // [nhipSuDungRunnable]), so lon hon the nghia la may vua ngu day hoac vua bi
        // dong bang, khong phai xem that.
        if (troiQua in 1..60_000L) GioiHanApp.congThem(this, pkg, troiQua)
    }

    /**
     * Dung dem het, dung khi tat man hinh hoac dich vu dung lai.
     *
     * Tru app dang phat tieng: tat man hinh khong lam nhac tat, va nua tieng nghe
     * trong luc man hinh den van la nua tieng.
     */
    private fun chotHetGio() {
        val bayGio = SystemClock.elapsedRealtime()
        dangDemGio.keys.toList().filter { it !in nhacDangPhat }.forEach { chotGio(it, bayGio) }
    }

    // ----------------------------------------------------- tieng phat ra o nen

    /**
     * Xet tieng dang phat ra: cong phut cho app duoc nghe nen, bam dung cai khong
     * duoc phep.
     *
     * VI SAO KHONG DE PHAN CHAN APP LO NOT. Phan do chi nhin cua so dang hien, ma
     * nhac thi khong co cua so nao: con mo Spotify trong gio choi, bam tam dung
     * phien de giu lai so phut, roi bam play tu the nhac trong thanh thong bao. Luc
     * do man hinh dang o man hinh chinh, dong ho phien dang dung, khong co gi de
     * chan - va nhac chay den khi nao con ngu.
     *
     * Chay moi [NHIP_NHAC_MS] trong suot luc con tieng, roi tu tat. Tat tieng la
     * khong con nhip nao, khong danh thuc may vo ich.
     */
    private fun xetNhac() {
        mainHandler.removeCallbacks(nhipNhacRunnable)

        // Ba Huy dang mo toan bo may thi khong dung gi ca.
        if (ParentMode.isActive(this)) {
            dangChanNhacMu = false
            if (nhacDangPhat.isNotEmpty()) {
                nhacDangPhat = emptySet()
                demGioTungApp(goiDangHienNeuSang())
            }
            return
        }

        // Chua bat quyen doc thong bao thi khong biet ai dang keu: di duong rieng.
        if (!TrinhPhat.coQuyen(this)) {
            if (nhacDangPhat.isNotEmpty()) {
                nhacDangPhat = emptySet()
                demGioTungApp(goiDangHienNeuSang())
            }
            henXetNhac(xetNhacKhiChuaCoQuyen())
            return
        }
        dangChanNhacMu = false

        val trongGioHoc = buoiDangChan() != null
        val trongGioNgu = gate.trongGioNgu()
        val congMo = gate.isOpen()

        val duocPhat = mutableSetOf<String>()
        val dangKeu = TrinhPhat.dangPhat(this)
        dangKeu.forEach { goi ->
            // Chuong bao thuc, tieng cuoc goi, tieng bam phim: khong bao gio dung.
            if (goi == packageName || goi in ALWAYS_ALLOWED || goi in systemEssentials) {
                return@forEach
            }

            val xu = LuatNhac.xet(
                laAppNhac = goi in prefs.nhacPackages,
                duocKhiHetGio = goi in prefs.allowedPackages,
                hetHanNgay = GioiHanApp.hetGio(this, goi),
                trongGioNgu = trongGioNgu,
                trongGioHoc = trongGioHoc,
                congMo = congMo,
                moiLuc = goi in prefs.moiLucPackages,
            )
            if (xu == XuLyNhac.CHO_PHAT) {
                duocPhat.add(goi)
                soLanBaoDung.remove(goi)
                return@forEach
            }

            // Chi ghi mot dong khi vua chuyen tu duoc phat sang phai dung. Khong the
            // thi con bam play lai mot lan la so cua ngay them muoi dong giong nhau.
            if (goi in nhacDangPhat) {
                val ly = when {
                    // App dung moi luc chi bi dung vi het han rieng, xem [LuatNhac].
                    goi in prefs.moiLucPackages -> "hết số phút hôm nay"
                    trongGioHoc -> "tới giờ đi học"
                    GioiHanApp.hetGio(this, goi) -> "hết số phút hôm nay"
                    trongGioNgu -> "quá giờ đi ngủ"
                    else -> "đang khoá"
                }
                Log.i(TAG, "dung tieng cua $goi: $ly")
                DayLog.add(this, "Dừng nhạc ${tenApp(goi)} — $ly")
            }
            // Bam nut dung cua chinh app do la cach sach nhat: chi mot app im, va con
            // bam play lai la nhac chay tiep tu cho cu. Nhung khong phai app nao cung
            // nghe: co cai khai bao mot trinh phat roi khong nhan lenh nao. Bao hai
            // lan ma no van keu thi quay ve cach cu, gianh quyen phat tieng cua ca may.
            val lan = (soLanBaoDung[goi] ?: 0) + 1
            soLanBaoDung[goi] = lan
            if (!TrinhPhat.dungLai(this, goi) || lan >= 2) {
                Log.i(TAG, "bam dung $goi khong an (lan $lan), bat im ca may")
                AudioHush.hush(this)
            }
        }
        soLanBaoDung.keys.retainAll(dangKeu)

        nhacDangPhat = duocPhat
        demGioTungApp(goiDangHienNeuSang())
        syncNhipNhac(dangKeu.isNotEmpty())
    }

    /**
     * Hen lan xet sau, hoac thoi neu tren may khong con tieng nao.
     *
     * Xet theo "co tieng" chu khong theo "co tieng duoc phep": vua bao mot app dung
     * ma no van keu thi phai quay lai xem no da im chua. Lay nhanh kia thi lan bao
     * thu hai - lan doi sang cach bat im ca may - khong bao gio toi.
     */
    private fun syncNhipNhac(conTieng: Boolean) = henXetNhac(if (conTieng) NHIP_NHAC_MS else null)

    /** Hen lan [xetNhac] sau [sauMs], hoac bo moi lan hen khi [sauMs] la null. */
    private fun henXetNhac(sauMs: Long?) {
        mainHandler.removeCallbacks(nhipNhacRunnable)
        if (sauMs != null) mainHandler.postDelayed(nhipNhacRunnable, sauMs)
    }

    /**
     * Xet tieng khi CHUA co quyen doc thong bao. Tra ve bao lau nua xet lai, hoac null
     * khi may da het tieng.
     *
     * Khong biet ai dang keu, chi biet trong may co nhac ([AudioManager.isMusicActive]).
     * Luat van la [LuatNhac], con cau "app nay duoc keu khi het gio khong" thi tra loi
     * theo app dang truoc mat, y nhu truoc day: dang mo mot app trong danh sach trang
     * thi tieng cua no duoc keu.
     *
     * VI SAO KHONG CHI GIANH QUYEN PHAT TIENG NHU TRUOC. Thu tren may ao Android 13
     * ngay 25/9/2026, voi mot may nhac thu co the nhac o thanh thong bao: gianh quyen
     * xong thi he thong chi van nho tieng trinh phat, hai giay sau moi bao no mat
     * quyen. Con bam Phat lai trong hai giay do la tieng tro lai. App nhac khong chiu
     * nhuong tieng thi con chang can bam gi: bon giay sau he thong tu tra lai am
     * luong, va nhac chay tiep den het buoi.
     *
     * Nen gio lam hai lop, va xet lai moi [NHIP_CHAN_NHAC_MS] den khi het tieng:
     *  - bam phim Dung cho trinh phat dang giu nut media, tuc la dung cai con vua bam
     *    Phat o thanh thong bao. App nao co the nhac o do cung nghe phim nay, ke ca
     *    app khong chiu nhuong tieng;
     *  - gianh quyen phat tieng nhu cu, cho app khong nghe phim media.
     *
     * Van chua bang duong co quyen: phim Dung di toi trinh phat moi nhat, nen hai app
     * cung keu thi co the bam nham app duoc phep. Bat quyen doc thong bao thi het
     * chuyen do - xem canh bao DOC_THONG_BAO trong [Permissions].
     */
    private fun xetNhacKhiChuaCoQuyen(): Long? {
        val am = audioManager ?: return null
        if (!am.isMusicActive) {
            dangChanNhacMu = false
            return null
        }
        val pkg = currentPackage
        val trongGioHoc = buoiDangChan() != null
        val trongGioNgu = gate.trongGioNgu()
        val xu = LuatNhac.xet(
            laAppNhac = false,
            duocKhiHetGio = pkg != null && pkg in prefs.allowedPackages,
            hetHanNgay = false,
            trongGioNgu = trongGioNgu,
            trongGioHoc = trongGioHoc,
            congMo = gate.isOpen(),
            // Khong mien cho app dung moi luc o nhanh nay. Nhanh nay chi biet app dang
            // truoc mat, khong biet app nao dang keu: de Telegram mo truoc mat thi mot
            // Spotify phat nen se keu ca dem. Doi lai, thieu quyen doc thong bao thi tin
            // thoai trong Telegram cung bi dung luc gio ngu, gio hoc - canh bao
            // DOC_THONG_BAO trong [Permissions] la de bat quyen do.
            moiLuc = false,
        )
        // Duoc keu thi van theo doi, nhip thua: het gio giua chung, hay con roi app
        // trong danh sach trang, thi khong co su kien phat tieng nao bao cho biet.
        if (xu == XuLyNhac.CHO_PHAT) {
            dangChanNhacMu = false
            return NHIP_NHAC_MS
        }

        if (!dangChanNhacMu) {
            dangChanNhacMu = true
            val ly = when {
                trongGioHoc -> "tới giờ đi học"
                trongGioNgu -> "quá giờ đi ngủ"
                else -> "đang khoá"
            }
            Log.i(TAG, "co nhac luc dang khoa, chua co quyen doc thong bao: bam Dung va bat im (app truoc mat=$pkg)")
            DayLog.add(this, "Dừng nhạc chạy nền: $ly")
        }
        bamPhimDung(am)
        AudioHush.hush(this)
        return NHIP_CHAN_NHAC_MS
    }

    /** Bam phim Dung cua tai nghe, cho trinh phat dang giu nut media. */
    private fun bamPhimDung(am: AudioManager) {
        runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        }.onFailure { Log.w(TAG, "khong bam duoc phim Dung: ${it.message}") }
    }

    /**
     * Bat im ngay, roi xet lai nhac ngay sau do.
     *
     * Bat im mot lan thi app khong chiu nhuong tieng chi bi van nho vai giay, va khong
     * ban ra su kien phat tieng nao de [playbackCallback] biet ma xet lai. Hen [xetNhac]
     * o day thi duong nao cung theo den khi het tieng.
     */
    private fun batImVaXetLai() {
        AudioHush.hush(this)
        mainHandler.post(nhipNhacRunnable)
    }

    /**
     * Cua so dang hien, hoac tap rong khi man hinh dang tat.
     *
     * Man hinh tat thi danh sach cua so van con nguyen app cuoi cung. Khong hoi cho
     * nay thi mot dem ngu nghe nhac se cong them ca gio xem YouTube cua buoi toi.
     */
    private fun goiDangHienNeuSang(): Set<String> {
        val sang = runCatching {
            getSystemService(PowerManager::class.java)?.isInteractive != false
        }.getOrDefault(true)
        return if (sang) goiDangHien() else emptySet()
    }

    // ------------------------------------------- so ghi con dung app gi, luc nao

    /**
     * Ghi lai app nao dang nam truoc mat, tu may gio den may gio. Xem [NhatKySuDung].
     *
     * KHAC [demGioTungApp]: ben kia dem de KHOA app khi het han rieng, nen chi dem
     * app da dat han va chi can mot con so tong. Cai nay khong khoa gi, chi de Ba
     * Huy doc lai - va cau hoi la "tu may gio den may gio", nen phai ghi tung khoang
     * cho moi app con mo duoc.
     *
     * DUNG DONG HO TUONG DOI HAY TUYET DOI: tuyet doi, khac [demGioTungApp]. So phut
     * da xem thi phai mien nhiem voi viec doi gio may - doi gio lui lai la duoc xem
     * them. Nhung so nay la mot moc tren mat dong ho de doc, "14:03", nen buoc phai
     * lay gio that.
     *
     * TU CO NHIP RIENG: vong [onTick] chi song trong phien choi, ma so nay phai ghi
     * ca luc con dung app trong danh sach trang ngoai gio choi. Nhip o day chi song
     * khi that su co app dang truoc mat va man hinh dang sang, roi tu tat - ve man
     * hinh chinh hay tat man hinh la khong con nhip nao.
     *
     * [hienTai] la danh sach cua so dang hien neu cho goi da doc san, khoi doc hai lan.
     */
    @Synchronized
    private fun capNhatSuDung(hienTai: Set<String>? = null) {
        mainHandler.removeCallbacks(nhipSuDungRunnable)
        val bayGio = System.currentTimeMillis()
        // Man hinh tat thi danh sach cua so van con nguyen app cuoi cung, nen khong
        // hoi cho nay thi mot dem ngu thanh "YouTube tam tieng".
        val sang = runCatching {
            getSystemService(PowerManager::class.java)?.isInteractive != false
        }.getOrDefault(true)
        val tatCa = if (!sang) emptySet() else (hienTai ?: goiDangHien())
        val hien = tatCa.filter { dangTheoDoi(it) }.toSet()

        dangXem.keys.toList().forEach { if (it !in hien) chotSuDung(it, bayGio) }

        hien.forEach { goi ->
            val phien = dangXem[goi]
            when {
                phien == null -> dangXem[goi] = PhienXem(bayGio)

                // Dong ho bi day lui, hoac tien trinh vua dong bang mot luc dai roi
                // song lai. Ca hai truong hop deu khong biet trong khoang do con co
                // dung may hay khong, nen chot khoang cu lai va mo khoang moi.
                bayGio < phien.thay || bayGio - phien.thay > MAT_DAU_SU_DUNG_MS -> {
                    chotSuDung(goi, bayGio)
                    dangXem[goi] = PhienXem(bayGio)
                }

                else -> {
                    phien.thay = bayGio
                    // Ghi xuong prefs thua hon nhip nhin: moi lan ghi la mot lan day
                    // trang thai sang dien thoai Ba Huy, ma so phut trong so chi can
                    // dung den phut.
                    if (bayGio - phien.ghiCuoi >= GHI_SU_DUNG_LAI_MS) {
                        phien.ghiCuoi = bayGio
                        NhatKySuDung.ghi(this, goi, phien.tu, bayGio)
                    }
                }
            }
        }

        baoTruocMat(sang, coAppNha = packageName in tatCa)
        if (dangXem.isNotEmpty()) mainHandler.postDelayed(nhipSuDungRunnable, NHIP_SU_DUNG_MS)
    }

    /** Khoang cua [goi] ket thuc: ghi not vao so roi quen di. */
    private fun chotSuDung(goi: String, bayGio: Long) {
        val phien = dangXem.remove(goi) ?: return
        // Chi tinh den luc CUOI CUNG nhin thay app do. Nhip vua treo mot luc dai thi
        // khoang giua do khong ai biet co gi tren man hinh, doan them vao chi lam so
        // gio phong len.
        val den = if (bayGio - phien.thay <= MAT_DAU_SU_DUNG_MS) bayGio else phien.thay
        if (den - phien.tu >= NGAN_NHAT_SU_DUNG_MS) NhatKySuDung.ghi(this, goi, phien.tu, den)
    }

    /** Khong con gi truoc mat nua: chot het cac khoang dang mo va thoi nhip. */
    @Synchronized
    private fun dongSuDung() {
        mainHandler.removeCallbacks(nhipSuDungRunnable)
        val bayGio = System.currentTimeMillis()
        dangXem.keys.toList().forEach { chotSuDung(it, bayGio) }
        baoTruocMat(sang = false, coAppNha = false)
    }

    /**
     * Chup lai "tablet dang mo gi" va bao duong dong bo, neu no vua doi.
     *
     * Goi o cuoi [capNhatSuDung] va [dongSuDung], ngay sau khi [dangXem] vua doi.
     * Nho vay Bang dieu khien va so ghi dung chung mot dinh nghia "app con dang
     * dung": bo man hinh chinh, ban phim, giao dien he thong, hop thoai khong co
     * bieu tuong. Bang noi con dang mo gi thi so ghi cung dang ghi dung khoang do.
     *
     * Them mot thu so ghi khong co: chinh app Nop bai. So ghi bo no ra vi do la cho
     * lam bai chu khong phai choi. Nhung Bang ma hien "khong mo app nao" trong luc
     * con dang soan tap thi la noi sai.
     *
     * CHI BAO KHI DOI THAT. Ham nay chay moi lan co su kien cua so, va nua phut mot
     * lan trong luc co app mo; moi lan bao la mot luot ghi Firestore. Nen so ca ban
     * chup voi ban truoc, giong thi thoi. Doi app lien tuc thi [DongBo.day] con gom
     * them mot lop nua.
     */
    private fun baoTruocMat(sang: Boolean, coAppNha: Boolean) {
        val bayGio = System.currentTimeMillis()
        appNhaTu = when {
            !sang || !coAppNha -> 0L
            appNhaTu == 0L -> bayGio
            else -> appNhaTu
        }
        val cac = buildList {
            dangXem.forEach { (goi, phien) -> add(TruocMat.MotApp(goi, tenNho(goi), phien.tu)) }
            if (appNhaTu > 0L) add(TruocMat.MotApp(packageName, tenNho(packageName), appNhaTu))
        }.sortedBy { it.tu }

        val moi = TruocMat(sang, cac)
        if (moi == truocMat) return
        truocMat = moi
        Log.i(TAG, "truoc mat: " + if (!sang) "man hinh tat" else moi.ten.ifEmpty { "khong mo app nao" })
        // Dang roi di thi thoi: duong dong bo da dung truoc do, xem [onUnbind].
        // Post sang luong chinh vi ham nay co khi chay trong vong [onTick] o luong
        // nen, ma [DongBo.day] giu moc gom khong khoa.
        if (dangChay) mainHandler.post { runCatching { DongBo.day() } }
    }

    /** Ten app, hoi PackageManager mot lan roi nho. Ham tren chay rat day. */
    private fun tenNho(goi: String): String = tenDaBiet.getOrPut(goi) { tenApp(goi) }

    /**
     * Goi nay co dang la mot app con mo ra dung khong.
     *
     * Bo man hinh chinh, ban phim, giao dien he thong va cac hop thoai khong co bieu
     * tuong: chung hien len ca tram lan mot ngay ma khong phai la "con dang dung cai
     * gi". Con lai thi ghi het - ke ca app trong danh sach trang va ke ca Cai dat,
     * vi cau hoi cua Ba Huy la con dung may vao viec gi, khong phai con co pham luat
     * khong.
     */
    private fun dangTheoDoi(pkg: String): Boolean {
        if (pkg == packageName) return false
        if (pkg in systemEssentials || pkg in ALWAYS_ALLOWED) return false
        if (pkg in goiManHinhChinh || pkg in BAN_CUA_MAN_HINH_CHINH) return false
        return moDuocTuManHinhChinh(pkg)
    }

    /**
     * Goi nay co bieu tuong tren man hinh chinh khong, tuc la co tu mo thang duoc
     * khong. Khong co thi no chi la hop thoai cua app khac goi len.
     *
     * Doc khong duoc thi coi nhu co, de nghieng ve phia chan chu khong tha.
     */
    private fun moDuocTuManHinhChinh(pkg: String): Boolean = moDuocTuNha.getOrPut(pkg) {
        runCatching { packageManager.getLaunchIntentForPackage(pkg) != null }
            .getOrDefault(true)
    }

    /** Mot lan doc danh sach cua so: cac goi coi nhu dang hien, va nen xet the nao. */
    private class ManHinh(val goi: Set<String>, val cach: CachXet)

    /**
     * Tat ca goi dang co cua so ung dung tren man hinh.
     *
     * Chia doi man hinh thi co hai, nen khong the chi nhin goi cua su kien vua roi:
     * dua tre de mot app duoc phep o nua tren, YouTube o nua duoi, va su kien cuoi
     * cung mang ten cai duoc phep.
     *
     * Thanh thong bao dang keo xuong thi la cac goi nam duoi no. Nhip ghi so su dung
     * va dem gio rieng cung hoi qua day, nen giu thanh ma xem tiep thi so phut van
     * chay.
     *
     * Tra ve tap rong neu he thong khong cho doc - luc do [evaluate] quay ve cach cu
     * la xet mot goi cua su kien.
     */
    private fun goiDangHien(): Set<String> = docManHinh().goi

    /**
     * Doc danh sach cua so mot lan, roi de [LuatManHinh] noi nen xet the nao.
     *
     * Lan nao doc duoc app thi nho lai vao [hienTruocKhiKeo]: thanh thong bao keo
     * xuong la he thong thoi bao cac cua so nam duoi no, va luc do chi con cach nho.
     *
     * Chi hoi them khi co mot cua so he thong dang giu tieu diem, tuc la gan nhu chi
     * luc thanh thong bao dang mo. Luc thuong khong ton them luot goi nao.
     *
     * @param goiSuKienMoi xem [LuatManHinh.appDangHien]
     */
    private fun docManHinh(goiSuKienMoi: String? = null): ManHinh {
        val ds = runCatching { windows }.getOrNull().orEmpty()
        val docDuoc = ds.asSequence()
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { goiCua(it) }
            .toSet()

        val tieuDiem = ds
            .firstOrNull { it.type == AccessibilityWindowInfo.TYPE_SYSTEM && it.isFocused }
            ?.let { LuatManHinh.TieuDiem(cuaMinh = goiCua(it) == packageName, phuKin = phuKin(it)) }
        val manKhoa = tieuDiem != null && !tieuDiem.cuaMinh && dangKhoaManHinh()
        val cach = LuatManHinh.cachXet(tieuDiem, manKhoa, docDuoc.isNotEmpty())
        val goi = LuatManHinh.appDangHien(cach, docDuoc, hienTruocKhiKeo, goiSuKienMoi)
        hienTruocKhiKeo = when {
            // Man chan gio hoc hay man hinh khoa dang che: khong biet ben duoi con gi.
            // Quen di, khong thi keo thanh thong bao tren man chan gio hoc se bi xet theo
            // app cua tu truoc buoi hoc.
            cach == CachXet.BO_QUA -> emptySet()
            // Doc duoc thi nho cai doc duoc. Dang phu thi nho ca app vua mo len ben
            // duoi, de lan xet sau - khong do su kien goi - van con thay no.
            docDuoc.isNotEmpty() || cach == CachXet.DUOI_THONG_BAO -> goi
            else -> hienTruocKhiKeo
        }
        return ManHinh(goi, cach)
    }

    /** Ten goi cua mot cua so, doc tu nut goc cua no. */
    private fun goiCua(cuaSo: AccessibilityWindowInfo): String? = runCatching {
        val root = cuaSo.root
        val ten = root?.packageName?.toString()
        root?.recycle()
        ten
    }.getOrNull()

    /** Cua so rong gan bang ca man hinh. Cung phep so voi [cuaSoNhoCua]. */
    private fun phuKin(cuaSo: AccessibilityWindowInfo): Boolean = runCatching {
        val man = resources.displayMetrics
        val khung = Rect().also { cuaSo.getBoundsInScreen(it) }
        khung.width() >= man.widthPixels - LECH_CHO_PHEP &&
            khung.height() >= man.heightPixels - LECH_CHO_PHEP
    }.getOrDefault(false)

    private fun dangKhoaManHinh(): Boolean = runCatching {
        getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
    }.getOrDefault(false)

    /**
     * @param goiSuKienMoi goi cua su kien "cua so truoc mat doi" vua toi, chi duong su kien
     *   do truyen. Xem [LuatManHinh.appDangHien].
     */
    private fun evaluate(pkgSuKien: String, goiSuKienMoi: String? = null) {
        if (ParentMode.isActive(this)) {
            overlay.hide()
            return
        }

        // Man chan gio hoc cua chinh app nay dang giu tieu diem, hoac dang o man hinh
        // khoa: ca hai che het moi app, khong co gi de xet. Thanh thong bao dang keo
        // xuong thi van xet, chi doi cach chan - xem [cheDuoiThongBao].
        val man = docManHinh(goiSuKienMoi)
        if (man.cach == CachXet.BO_QUA) return
        val duoiThongBao = man.cach == CachXet.DUOI_THONG_BAO
        if (!duoiThongBao) goiCheDuoi = ""

        val docDuocCuaSo = man.goi
        // Duoi thanh thong bao thi khong doan theo goi cua su kien: tru duong su kien
        // cua so (da nam trong [goiSuKienMoi]), cac duong khac truyen [currentPackage], co
        // khi la app da bi day di tu lau. Khong biet ben duoi co gi thi thoi.
        val hien = if (duoiThongBao) docDuocCuaSo else docDuocCuaSo.ifEmpty { setOf(pkgSuKien) }
        demGioTungApp(hien)
        capNhatSuDung(hien)
        Log.d(TAG, "xet ${hien.joinToString(",")}: state=${gate.state} " +
            "conGio=${gate.isOpen()} moToanBo=${ParentMode.isActive(this)} " +
            "docDuocCuaSo=${docDuocCuaSo.isNotEmpty()} duoiThongBao=$duoiThongBao")
        if (hien.isEmpty()) return

        // App nha chi duoc mien khi no dung MOT MINH tren man hinh. Mien ca khi no
        // chia man voi app khac thi chi can de no o mot nua la tat duoc toan bo viec
        // chan - dung cai lo ma chia doi man hinh khoet ra.
        if (hien.size == 1 && hien.first() == packageName) return

        // Khong doc duoc danh sach cua so thi khong biet co dang chia doi man hinh
        // hay khong. Luc do quay ve luat cu: dang mo man hinh cua app nay thi khong
        // chan gi.
        //
        // Luat cu chinh la cai lam chia doi man hinh lot luoi, nen chi dung khi mu.
        // Doi lai no giu cho hop thoai nhap PIN khong bao gio thanh cai bay: mat mot
        // phien choi thi con sua duoc, chu khoa Ba Huy ra ngoai thi phai vao che do
        // an toan moi go ra.
        if (docDuocCuaSo.isEmpty() && App.manHinhCuaAppDangMo) return

        // Khong doc duoc man hinh chinh thi khong chan gi ca: chan nham man hinh chinh
        // lam ca may khong dung duoc, tac hai lon hon bo sot mot phien choi. Nhung im
        // lang o day la sai - may trong nhu dang canh ma thuc te khong canh gi.
        if (systemEssentials.isEmpty()) {
            docLaiDanhSachHeThong()
            if (systemEssentials.isEmpty()) {
                Log.w(TAG, "khong doc duoc man hinh chinh, KHONG CHAN GI")
                return
            }
        }

        // Chan neu BAT KY cua so nao dang hien la thu khong duoc phep.
        val coAppDuocPhep = hien.any { lyDoChan(it) == null }
        val coManHinhChinh = hien.any { it in goiManHinhChinh }
        for (pkg in hien) {
            val ly = lyDoChan(pkg) ?: continue

            // Hop thoai cua he thong de len app khac - xin quyen mic, xin quyen may
            // anh, o chon giong noi - mang ten goi rieng, va luat "bat ky cua so nao"
            // o tren coi do la mot app la roi day ca cum di. Hau qua: app hoc tieng
            // Anh nam trong danh sach trang, mo len xin quyen mic mot cai la bi bao
            // "het gio roi", va con khong bao gio cap duoc quyen do.
            //
            // Nhung goi nay khong co bieu tuong tren man hinh chinh, tuc la khong tu
            // mo thang duoc - chi hien khi co app khac goi. Nen khi ben duoi dang la
            // app duoc phep thi de cho chung hien.
            if (coAppDuocPhep && !moDuocTuManHinhChinh(pkg)) {
                Log.i(TAG, "bo qua $pkg: hop thoai he thong de tren app duoc phep")
                continue
            }

            // O tim kiem, o tro ly, man hinh am -1 deu la do man hinh chinh dung len
            // nhung mang ten goi rieng. Day chung di la day chinh man hinh chinh, ma
            // day man hinh chinh thi no lai hien len - dung cai vong lam may nhay
            // lien tuc. Chi mien khi man hinh chinh dang o day that: mo han app Google
            // ra toan man thi khong con man hinh chinh trong danh sach nua, van chan.
            if (coManHinhChinh && pkg in BAN_CUA_MAN_HINH_CHINH) {
                Log.i(TAG, "bo qua $pkg: la mot phan cua man hinh chinh")
                continue
            }
            if (duoiThongBao) {
                cheDuoiThongBao(pkg, ly)
                return
            }
            // Cua so nho hon man hinh = cua so noi, hoac mot nua khi chia doi man
            // hinh. Loai do co nut dong cua rieng no, bam mot cai la xong - lam ngay
            // tu lan dau chu khong doi day hut ba lan roi moi thu. Cua so toan man
            // thi khong dung duong nay: trong do "dong" co the la nut dong cua mot
            // quang cao hay mot hop thoai trong game, bam vao la lam chuyen khac.
            if (cuaSoNhoCua(pkg) && thuDongCuaSo(pkg)) {
                Log.i(TAG, "dong duoc cua so nho cua $pkg ngay tu dau")
                return
            }

            when (xetDay(pkg)) {
                Day.CHO_DA -> return
                Day.KHONG_DAY_DUOC -> {
                    Log.i(TAG, "khong day duoc $pkg (lan thu $khongChiuDi)")
                    khongDayDuoc(pkg)
                }
                Day.DAY -> {
                    Log.i(TAG, "chan $pkg: ${ly.first} (khongChiuDi=$khongChiuDi)")
                    batImVaXetLai()
                    // Trong gio di hoc thi KHONG tu mo man cua Le Hoa. Man chan
                    // nhuong cho chinh app nay, nen mo app ra la buc tuong "toi gio
                    // di hoc" tan luon - dung cai vua day di lai tu mo cua sau.
                    // Muon vao app thi cham vao man chan, do la viec co y bam.
                    block(
                        ly.first,
                        ly.second,
                        moManCon = khongChiuDi == 0 && buoiDangChan() == null
                    )
                }
            }
            return
        }
    }

    /**
     * Thanh thong bao dang keo xuong, ma ben duoi la app khong duoc dung: che app do
     * lai va dong thanh thong bao, nhung khong bam Home.
     *
     * Truoc 27/9/2026 luc nay guard khong xet gi, va Le Hoa giu thanh o sat dau man
     * hinh de xem tiep app ben duoi. Xem [LuatManHinh].
     *
     * Thu tren may ao Android 13: lop phu cua dich vu nay nam TREN thanh thong bao, va
     * trong luc ngon tay con giu thanh thi ca lenh dong thanh lan nut Back deu khong an.
     * Nen moi nhip lam ca hai viec:
     *  - hien lop phu, de giu tay bao lau cung khong xem duoc gi ben duoi;
     *  - dong thanh. Tay con giu thi lenh khong an, tay vua buong la thanh dong. Thieu
     *    buoc nay thi buong tay luc thanh dang keo het co la ket: thanh nam yen, lop phu
     *    cu hai giay hien lai mot lan va an het cham, vuot thanh len cung khong duoc.
     * Thanh dong roi thi danh sach cua so doc duoc nhu thuong, va duong chan thuong lo
     * tiep: bam Home, mo man hinh cua Le Hoa.
     *
     * Khong bam Home ngay tu day vi trong luc thanh con phu, danh sach cua so khong thay
     * app ben duoi: day xong cung khong biet no da di chua, va [xetDay] se tuong app
     * khong chiu di roi bao nham la cua so noi.
     */
    private fun cheDuoiThongBao(pkg: String, ly: Pair<String, String>) {
        overlay.show(ly.first, ly.second, autoHideMs = CHE_DUOI_THONG_BAO_MS)
        dongThanhThongBao()
        henXetLai(XET_LAI_DUOI_THONG_BAO_MS)
        if (pkg == goiCheDuoi) return
        goiCheDuoi = pkg
        Log.i(TAG, "thanh thong bao dang keo tren $pkg: che lai, khong bam Home")
        // Nhat ky chi ghi muoi phut mot lan: keo di keo lai ca chuc lan thi bon muoi
        // dong cua ngay khong bi chuyen nay chiem het.
        val bayGio = SystemClock.elapsedRealtime()
        if (lucGhiCheDuoi == 0L || bayGio - lucGhiCheDuoi >= GHI_CHE_DUOI_MS) {
            lucGhiCheDuoi = bayGio
            DayLog.add(this, "Kéo thanh thông báo xuống khi ${tenApp(pkg)} đang bị chặn, máy che lại")
        }
    }

    /** Dong thanh thong bao lai. Xem [cheDuoiThongBao]. */
    private fun dongThanhThongBao() {
        val lenh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
        } else {
            // Truoc Android 12 chua co lenh rieng. Thanh dang giu tieu diem, nen nut
            // Back di thang vao no va dong no lai.
            GLOBAL_ACTION_BACK
        }
        runCatching { performGlobalAction(lenh) }
    }

    /** Buoi hoc dang bi chan ngay luc nay, hoac null. Cung mot cau tra loi voi [ManChan]. */
    private fun buoiDangChan() = TinhLoiNhac.buoiDangChan(
        java.util.Calendar.getInstance(),
        prefs.batManChan,
        prefs.buoiDuocMoSom
    )

    /** Ly do phai chan goi nay, hoac null neu no duoc phep. */
    private fun lyDoChan(pkg: String): Pair<String, String>? {
        if (pkg == packageName) return null
        if (pkg in systemEssentials || pkg in ALWAYS_ALLOWED) return null
        if (laBanPhim(pkg) || docLaiMienTruNeuCu(pkg)) return null

        // Ba Huy vua bam mot dong trong bang canh bao de di bat quyen. Khong mo
        // duong thi bam xong la bi chinh guard day ve, khong sua duoc gi.
        if (pkg in GUARDED_SYSTEM_PACKAGES && PhienQuanLy.dangChoMoCaiDat()) return null

        if (pkg in STORE_PACKAGES) {
            return "Không cài app mới được" to "Ba Huy mở máy hộ nếu Lê Hòa cần app học"
        }
        if (prefs.lockSystemSettings && pkg in GUARDED_SYSTEM_PACKAGES) {
            return "Cài đặt đang khoá" to "Ba Huy mở bằng mã PIN trong app Nộp bài"
        }
        if (pkg in prefs.blockedPackages) {
            return "App này không được dùng" to
                "Ba Huy đã cấm app này, kể cả trong giờ chơi"
        }

        // Han rieng cua app xet truoc ca gio choi: Netflix mot ngay mot tieng ruoi la
        // mot tieng ruoi, du hom do co gio choi hay khong, du app co nam trong danh
        // sach duoc dung khi het gio hay khong.
        val han = GioiHanApp.han(this, pkg)
        if (han > 0 && GioiHanApp.daDungMs(this, pkg) >= han * 60_000L) {
            return "Hết giờ ${tenApp(pkg)} hôm nay" to
                "Mỗi ngày chỉ $han phút. Mai xem tiếp nhé."
        }

        // App dung moi luc xet sau danh sach cam va han rieng, truoc moi luat ve gio: gio
        // di hoc, gio choi, gio ngu. Ba Huy dat cho Telegram, de Le Hoa nhan cho ba duoc
        // ca luc bi khoa. Xem [Prefs.moiLucPackages].
        if (pkg in prefs.moiLucPackages) return null

        // Gio di hoc xet truoc ca gio choi.
        //
        // Bao "Het gio roi" luc nay la noi sai viec: gio choi van con nguyen, dang
        // duoc giu lai cho tan hoc. Cai dang xay ra la den luc phai buong may di
        // hoc - va do cung la cau ma man chan dang hien ngay sau lop phu nay.
        //
        // Chan ca app trong danh sach duoc dung khi het gio: man chan von da che
        // kin man hinh, mo rieng tu dien ra luc dang phai ra khoi nha thi vo nghia.
        buoiDangChan()?.let { buoi ->
            return "Tới giờ đi học rồi" to
                (TinhLoiNhac.moTaBuoi(buoi).replaceFirstChar { it.uppercase() } +
                    ", vào học ${TinhLoiNhac.gioPhut(buoi.phutVaoHoc)}. " +
                    "Máy mở lại lúc ${TinhLoiNhac.gioPhut(buoi.phutTanHoc)}.")
        }

        if (gate.isOpen()) return null

        // Gio ngu khoa ca danh sach trang.
        //
        // Truoc day danh sach trang mo ca dem, vi no duoc nghi cho tu dien va may
        // tinh. Nhung trong do con co app AI, ma so phut rieng cua app lai tinh lai
        // luc 0 gio: con thuc den 00:05 la co them nua tieng moi. Ba Huy chon khoa.
        //
        // Dat SAU gate.isOpen(): gio choi con thi moi app deu mo. Hien gio thi ba
        // cung khong cap duoc gio trong gio ngu, nhung neu mai sau cap duoc thi do
        // la y cua ba, khong phai cho de chan.
        if (gate.trongGioNgu()) {
            return "Tới giờ ngủ rồi" to
                "Máy mở lại lúc ${TinhLoiNhac.gioPhut(prefs.gioDayMinuteOfDay)}. Mai chơi tiếp nhé."
        }

        if (pkg in prefs.allowedPackages) return null

        // Bao dung viec dang can lam. Truoc day luc nao cung "Nop bai tap de duoc
        // choi tiep" - het luot roi ma van bao nop bai thi con nop, ba tu choi, va
        // khong ai hieu tai sao. Dang tam dung hay da duyet thi viec can lam cung
        // khong phai nop bai.
        val detail = when {
            // Dang cho duyet bai moi ma trong tay con phieu chua dung thi choi duoc
            // ngay, khong phai cho. Bao "dang cho duyet" o day la con ngoi cho oan.
            gate.state == GateState.PENDING && gate.grantedMinutes > 0 ->
                "Đang giữ ${gate.grantedMinutes} phút. Mở app Nộp bài, bấm Bắt đầu chơi"
            gate.state == GateState.PENDING -> "Đã gửi bài rồi, đang chờ ba Huy duyệt"
            gate.state == GateState.PAUSED -> "Đang tạm dừng. Mở app Nộp bài, bấm Chơi tiếp"
            gate.state == GateState.GRANTED -> "Ba Huy duyệt rồi. Mở app Nộp bài, bấm Bắt đầu"
            gate.phutConLaiHomNay() <= 0 -> "Hôm nay đủ giờ chơi rồi, mai nộp bài tiếp nhé"
            else -> "Nộp bài tập để được chơi tiếp nhé"
        }
        return "Hết giờ rồi" to detail
    }

    /**
     * Hen xet lai sau [sauMs], vi lan nay chua toi luot day.
     *
     * Khong hen thi app do nam luon: trong bon giay phanh khong ai day no, ma sau do
     * cung khong co su kien cua so nao moi de goi ham xet nay - app dung yen truoc
     * mat thi Android khong bao gi ca. Con mo hai lan lien la lan thu hai o lai mai.
     */
    private fun henXetLai(sauMs: Long) {
        mainHandler.removeCallbacks(xetLaiRunnable)
        mainHandler.postDelayed(xetLaiRunnable, sauMs.coerceAtLeast(500L) + 200L)
    }

    /**
     * Co duoc day goi nay di ngay bay gio khong, hay vua day cach day mot giay.
     *
     * Day la cai phanh giu cho may khong bao gio dung hinh. Moi lan day di la sinh
     * ra su kien cua so moi, su kien do lai goi ham xet nay - neu app bi chan khong
     * chiu di (cua so noi cua MIUI la kieu do: bam nut home no van nam tren cung)
     * thi hai ben dap nhau vai chuc lan mot giay. Lop phu den bat len tat xuong lien
     * tuc, ma no lai an cham, nen may thanh cuc gach cho toi khi khoi dong lai.
     *
     * Day may lan ma no van con thi thoi khong day nua: bao mot cau roi lui lai nua
     * phut. Con dung duoc cai cua so noi do them mot lat - doi lai may khong chet.
     */
    private fun xetDay(pkg: String): Day {
        val bayGio = SystemClock.elapsedRealtime()
        val cungGoi = pkg == goiVuaChan
        val cachNhau = bayGio - lucVuaChan

        // Vua day xong ma no van con day. Khong phai con vua mo lai: trong bon giay
        // do lop phu che kin va an cham, con khong bam duoc gi. Nghia la chinh no
        // khong chiu di - dem mot diem roi im. Tuyet doi khong day them ngay bay gio:
        // day them chi de ra su kien moi cho dung cai vong lam may nhay lien tuc.
        //
        // Phanh tinh chung cho moi goi, khong tinh rieng tung cai. Neu tinh rieng thi
        // hai goi thay phien nhau la lot: day Chrome xong ve man hinh chinh, o tim
        // kiem cua launcher hien len lai bi coi la app khac, day tiep, Chrome lai len.
        if (cachNhau < CHAN_LAI_SAU_MS) {
            henXetLai(CHAN_LAI_SAU_MS - cachNhau)
            if (cungGoi && !daDemLanNay) {
                daDemLanNay = true
                khongChiuDi += 1
                // Day du so lan ma no van tro lai ngay: bao luon o day chu khong doi
                // den lan day sau, vi lan day sau la ba muoi giay nua.
                if (khongChiuDi >= SO_LAN_COI_NHU_KHONG_DAY) return Day.KHONG_DAY_DUOC
            }
            return Day.CHO_DA
        }

        // Cach lan truoc lau roi thi coi la dot moi, quen het chuyen cu.
        if (!cungGoi || cachNhau > QUEN_SAU_MS) {
            khongChiuDi = 0
            daBaoKhongDay = false
            batDauDot = 0L
        }

        // Da biet la khong day duoc thi thoi khong day nua, nhung cung khong buong:
        // giu nguyen lop phu che no lai, va xet lai deu de biet luc no bien mat.
        if (cungGoi && khongChiuDi >= SO_LAN_COI_NHU_KHONG_DAY) return Day.KHONG_DAY_DUOC

        goiVuaChan = pkg
        lucVuaChan = bayGio
        daDemLanNay = false
        return Day.DAY
    }

    /**
     * Day may lan roi ma app do van nam do. Thoi, chi bao mot cau.
     *
     * Khong goi [block] nua: moi lan day la them mot vong dap nhau, ma ro rang la
     * day khong duoc. Bao Ba Huy dung mot lan cho moi dot, khong thi tin nhan dồn
     * ca chuc cai.
     */
    /**
     * Day may lan roi ma app do van nam do - cua so noi cua MIUI la kieu nhu vay.
     *
     * Thoi khong day nua: da biet day khong an thua, day them chi lam man hinh nhay.
     * Thay vao do la mot nhip deu: hien loi nhac hai giay, roi lui hai giay.
     *
     * Hai giay do la co y. Trong luc lop phu dang hien thi no an cham, con khong bam
     * duoc gi ca - ke ca nut dong cua so noi. Lui ra mot giay la de con tu dong cai
     * cua so do lai. Dong xong thi vong nay tu het, khong ai phai lam gi them.
     *
     * Bam vao loi nhac thi mo thang man hinh cua Le Hoa - de bao gio cung con mot
     * duong ra, khong bi khoa cung.
     */
    private fun khongDayDuoc(pkg: String) {
        // Mot nhip mot lan thoi. Moi lan lop phu tat di la he thong ban ra vai su
        // kien cua so, moi su kien lai goi vao day - khong chan thi nhip hai giay
        // hien hai giay lui bi rut ngan lai con hon mot giay.
        val bayGio = SystemClock.elapsedRealtime()
        if (bayGio - lucNhacCuoi < NHAC_HIEN_MS + NHAC_LUI_MS) return
        lucNhacCuoi = bayGio

        val ten = tenApp(pkg)

        // Thu dong ho cua so do moi nhip mot lan. Lan dau co the truot vi cay giao
        // dien chua dung len xong, hoac vi thanh tieu de cua so noi dang an di - nen
        // thu lai deu chu khong bo sau mot lan.
        if (thuDongCuaSo(pkg)) {
            Log.i(TAG, "da bam nut dong cua so cua $pkg")
            return
        }

        overlay.show(
            "Hết giờ rồi",
            "Đóng cửa sổ nổi của $ten lại. Bấm vào đây để mở app Nộp bài.",
            autoHideMs = NHAC_HIEN_MS,
            khiBam = {
                overlay.hide()
                runCatching {
                    startActivity(
                        Intent(this, HomeActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )
        henXetLai(NHAC_HIEN_MS + NHAC_LUI_MS)

        if (!daBaoKhongDay) {
            daBaoKhongDay = true
            batDauDot = bayGio
            // Nut quay lai chi thu dung mot lan: bam nhieu lan thi co khi lai thoat
            // ra khoi man hinh cua Le Hoa dang o duoi.
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.w(TAG, "khong day duoc $pkg, co the la cua so noi")
            DayLog.add(this, "Không đẩy được $ten ($pkg)")
        }

        // Chi nhan tin khi no keo dai that. Rat nhieu thu chi thoang qua: keo thanh
        // thong bao xuong, hop thoai cua he thong, trinh phat nhac hien mot cai roi
        // bien. Bao ngay tu giay dau thi Ba Huy nhan ca chuc tin mot ngay ve nhung
        // thu tu no het sau ba giay.
        //
        // Va nua tieng moi bao mot lan, du la app nao: mot cai cua so noi nam ca
        // buoi chieu chi dang mot tin, khong phai mot tin moi bon giay.
        val daLau = batDauDot > 0 && bayGio - batDauDot > LAU_MOI_BAO_MS
        val dabaoGanDay = lucBaoCuoi > 0 && bayGio - lucBaoCuoi < CACH_BAO_MS
        if (daLau && !dabaoGanDay) {
            lucBaoCuoi = bayGio
            Notifier.send(
                this,
                "⚠️ $ten ($pkg) đang mở mà tablet đẩy đi không được — " +
                    "có thể là cửa sổ nổi. Máy đang nhắc Lê Hòa đóng lại."
            )
        }
    }

    /**
     * [pkg] co dang hien o mot cua so nho hon man hinh khong.
     *
     * Cua so noi va nua man hinh khi chia doi deu nho hon; app binh thuong thi chiem
     * tron. Do la cach phan biet ma khong phai doan theo ten hang may.
     */
    private fun cuaSoNhoCua(pkg: String): Boolean = runCatching {
        val man = resources.displayMetrics
        windows.any { cuaSo ->
            if (cuaSo.root?.packageName?.toString() != pkg) return@any false
            val khung = Rect().also { cuaSo.getBoundsInScreen(it) }
            khung.width() < man.widthPixels - LECH_CHO_PHEP ||
                khung.height() < man.heightPixels - LECH_CHO_PHEP
        }
    }.getOrDefault(false)

    /**
     * Thu dong cua so cua [pkg] bang chinh nut dong cua no.
     *
     * Android khong cho app thuong day app khac xuong nen - khong co ham nao lam
     * viec do. Nhung dich vu tro nang thi nhin duoc cay giao dien cua cua so dang
     * hien, va bam ho duoc mot nut trong do. Cua so noi cua MIUI co thanh nho o tren
     * voi nut dong va nut thu nho; neu no khai bao ten cho nut do thi day la cach
     * duy nhat dong duoc no tu ben ngoai.
     *
     * Chi bam nut nao ghi ro la dong hay thu nho, va chi trong dung cua so cua app
     * bi chan - khong mo mang sang cho khac, vi bam nham trong app nguoi ta la
     * chuyen khac han.
     */
    private fun thuDongCuaSo(pkg: String): Boolean = runCatching {
        windows.any { cuaSo ->
            val goc = cuaSo.root ?: return@any false
            if (goc.packageName?.toString() != pkg) return@any false
            if (goc.performAction(AccessibilityNodeInfo.ACTION_DISMISS)) return@any true
            timNutDong(goc, 0)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        }
    }.getOrDefault(false)

    /** Tim nut dong trong cay giao dien, khong di sau qua de khoi quet ca man hinh. */
    private fun timNutDong(node: AccessibilityNodeInfo, sau: Int): AccessibilityNodeInfo? {
        if (sau > 4) return null
        val ten = ((node.contentDescription ?: "").toString() + " " +
            (node.viewIdResourceName ?: "")).lowercase()
        if (node.isClickable && TU_KHOA_NUT_DONG.any { ten.contains(it) }) return node
        for (i in 0 until node.childCount) {
            val con = node.getChild(i) ?: continue
            timNutDong(con, sau + 1)?.let { return it }
        }
        return null
    }

    private fun tenApp(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    /**
     * Day app bi chan di, roi mo thang man hinh cua Le Hoa.
     *
     * Truoc day chi day ve man hinh chinh: con doc duoc cau "nop bai tap de duoc
     * choi tiep" nhung lai dang dung giua man hinh chinh, van phai tu di tim icon
     * Nop bai. Gio bao gi thi mo luon cho de lam viec do.
     *
     * Mo duoc tu day la nho quyen hien tren app khac - quyen ma app da phai co san
     * de ve lop phu nay.
     */
    private fun block(title: String, detail: String, moManCon: Boolean) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        overlay.show(title, detail)
        // Chi mo man hinh cua con o lan day dau tien. Nhung lan sau la dang trong
        // mot dot day lien tiep, mo them chi lam day them su kien vao cai vong do.
        if (!moManCon) return
        // Cho man hinh chinh hien ra xong da. Mo ngay lap tuc thi cu chuyen ve man
        // hinh chinh chay sau, day luon man cua Le Hoa xuong duoi.
        mainHandler.postDelayed({
            runCatching {
                startActivity(
                    Intent(this, HomeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }, 400L)
    }

    /**
     * Goi nay co phai ban phim dang dung khong, hoi ngay luc can biet.
     *
     * Hoi hai duong vi [InputMethodManager.getEnabledInputMethodList] doi khi tra
     * ve rong ngay sau khi dich vu khoi dong, con ban phim dat lam mac dinh thi luc
     * nao cung doc duoc tu Settings.
     */
    private fun laBanPhim(pkg: String): Boolean {
        val trongDanhSach = runCatching {
            getSystemService(InputMethodManager::class.java)
                .enabledInputMethodList.any { it.packageName == pkg }
        }.getOrDefault(false)
        if (trongDanhSach) return true

        val macDinh = runCatching {
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        }.getOrNull().orEmpty()
        return macDinh.isNotEmpty() && macDinh.startsWith("$pkg/")
    }

    /** Doc lai danh sach mien tru, nhieu nhat mot lan moi phut, roi xet lai. */
    private fun docLaiMienTruNeuCu(pkg: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lanDocMienTru < 60_000L) return false
        lanDocMienTru = now
        docLaiDanhSachHeThong()
        return pkg in systemEssentials
    }

    /**
     * Doc lai CA HAI danh sach he thong, luon luon cung mot luc.
     *
     * VI SAO PHAI CHUNG MOT HAM. Truoc day moi cho tu goi [readSystemEssentials],
     * con [goiManHinhChinh] thi chi duoc nap trong mot nhanh du phong cua
     * [evaluate] - nhanh chi chay khi danh sach mien tru rong. Ma [onServiceConnected]
     * da nap danh sach mien tru ngay luc dich vu len, nen nhanh do khong bao gio
     * toi, va goiManHinhChinh rong suot ca doi dich vu.
     *
     * Hau qua: phep mien tru "o tim kiem, tro ly, man hinh am mot deu la mot phan
     * cua man hinh chinh" (xem [BAN_CUA_MAN_HINH_CHINH]) khong bao gio dung, vi no
     * doi hoi man hinh chinh phai co trong goiManHinhChinh. Nen o tim kiem Google
     * tren man hinh chinh bi coi la mot app la: guard bam Home de day no di, man
     * hinh chinh hien len keo theo chinh o do, day ba lan khong duoc thi bo cuoc va
     * ghi "Khong day duoc Google" vao nhat ky. Le Hoa thi bi mo app Nop bai len mat.
     */
    private fun docLaiDanhSachHeThong() {
        systemEssentials = readSystemEssentials()
        goiManHinhChinh = readHomePackages()
        soMienTru = systemEssentials.size
    }

    /**
     * Launcher dang dung va cac ban phim da bat.
     *
     * Hoi ca hai duong: resolveActivity cho launcher mac dinh, va
     * queryIntentActivities cho moi launcher da cai. Neu ca hai deu khong ra gi
     * thi tra ve tap rong, va [evaluate] se khong chan bat cu thu gi nua.
     */
    /** Rieng cac goi lam man hinh chinh, de biet luc nao dang dung o man hinh chinh. */
    private fun readHomePackages(): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return runCatching {
            packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet()).minus(GUARDED_SYSTEM_PACKAGES)
    }

    private fun readSystemEssentials(): Set<String> = buildSet {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        runCatching {
            packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }.getOrNull()?.let { add(it) }

        runCatching {
            packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.activityInfo?.packageName }
        }.getOrDefault(emptyList()).forEach { add(it) }

        runCatching {
            getSystemService(InputMethodManager::class.java)
                .enabledInputMethodList
                .mapNotNull { it.packageName }
        }.getOrDefault(emptyList()).forEach { add(it) }
    }
        // App Cai dat co mot man hinh du phong khai bao category HOME, nen no lot
        // vao day va duoc mien tru, khien tinh nang khoa Cai dat im lang khong
        // chay. Ma khoa Cai dat lai la mat xich giu ca vong khoa. Loai ra.
        .minus(GUARDED_SYSTEM_PACKAGES)

    companion object {
        private const val TAG = "HomeworkGate"

        /**
         * Dich vu co dang chay that khong.
         *
         * Cai dat he thong noi "da bat" khong co nghia la Android da khoi dong duoc
         * dich vu. Tren HyperOS, vuot app khoi man hinh da nhiem la giet tien trinh,
         * va neu chua bat Tu khoi dong thi dich vu khong bind lai - cong tac van bat,
         * man hinh van bao moi thu on, ma khong con ai chan app nao.
         *
         * Man hinh doc thang co nay duoc vi no va dich vu nam chung mot tien trinh.
         */
        @Volatile
        var dangChay = false
            private set

        /** So app duoc mien tru. Bang 0 nghia la dich vu dang khong chan gi ca. */
        @Volatile
        var soMienTru = 0
            private set

        /**
         * Tablet dang mo gi, de day sang Bang dieu khien. Xem [baoTruocMat].
         *
         * Chi co nghia khi [dangChay]: dich vu khong chay thi khong ai nhin man hinh,
         * va gia tri nay la cua lan cuoi no con song.
         */
        @Volatile
        var truocMat: TruocMat = TruocMat.TAT
            private set

        /**
         * App vua ra truoc mat theo su kien cua so cuoi cung, tru chinh app nay va ban
         * phim. Khac [truocMat]: khong bi xoa khi tat man hinh, va giu nguyen khi man chan
         * che kin (luc do Android khong bao gi ve cua so ben duoi). Man chan doc no de
         * biet con con dang o app dung moi luc khong, xem ApprovalService.nhuongAppMoiLuc.
         *
         * Chi co nghia khi [dangChay], giong [truocMat].
         */
        @Volatile
        var goiVuaMo: String? = null
            private set
        /**
         * Gop cac su kien "danh sach cua so doi" trong khoang nay thanh mot lan xet.
         *
         * VI SAO PHAI GOP. Loai su kien do ban rat day: ban phim len roi xuong, mot
         * cai toast hien ra roi tat, mot popup trong game, keo thanh thong bao - moi
         * thu deu la mot lan danh sach cua so doi, va mot thao tac cua nguoi dung
         * thuong de ra ba den nam su kien trong vai chuc mili giay.
         *
         * Ma moi su kien do chay [evaluate], von goi [goiDangHien]: mot luot doc
         * `windows` sang tien trinh he thong, roi lay `.root` cua TUNG cua so, moi
         * cai them mot luot nua. Do la goi lien tien trinh, dat hon nhieu lan so voi
         * doc mot bien. Nam lan chay de ra dung mot ket qua giong nhau.
         *
         * Hoan 300 mili giay, su kien moi toi thi huy cai hen cu va hen lai, nen ca
         * chum chi chay mot lan - dung luc chum do lang xuong. Cach
         * [vn.huytl.homeworkgate.ui.HomeActivity] da lam voi lang nghe prefs.
         *
         * CAI GIA: chan cham di toi da 300 mili giay, va CHI o duong nay. Doi han
         * sang app khac thi van chan ngay khong hoan - xem [onAccessibilityEvent].
         */
        private const val GOP_CUA_SO_MS = 300L

        private const val TICK_INTERVAL_MS = 20_000L

        /** Bao lau nhin lai mot lan xem app do con nam truoc mat khong. */
        private const val NHIP_SU_DUNG_MS = 30_000L

        /**
         * Bao lau xet lai tieng dang phat mot lan.
         *
         * Hai muoi giay: bang nhip dem gio cua phien, va la sai so toi da cho moc
         * "het so phut hom nay" - nghe qua han nua phut thi khong ai de y, nghe qua
         * han nam phut thi con biet ngay la co ke ho.
         */
        private const val NHIP_NHAC_MS = 20_000L

        /** Gop cac su kien phat tieng trong khoang nay thanh mot lan xet. */
        private const val GOP_TIENG_MS = 500L

        /**
         * Nhip tre hon the thi coi nhu vua mat dau, khong noi khoang do lai.
         *
         * Ba nhip: mot nhip truot la chuyen thuong khi may dang ban, ba nhip lien
         * thi da la tien trinh bi dong bang.
         */
        private const val MAT_DAU_SU_DUNG_MS = 3 * NHIP_SU_DUNG_MS

        /** Khoang ngan hon the khong ghi vao so: luot qua chu khong phai dung. */
        private const val NGAN_NHAT_SU_DUNG_MS = 5_000L

        /**
         * Khoang dang mo bao lau moi ghi xuong kho mot lan.
         *
         * Day chi la bao hiem cho truong hop tien trinh chet khong kip troi: bi
         * HyperOS giet, het pin, rut nguon. Moi duong ket thuc binh thuong - doi
         * app, tat man hinh, het gio bi day ve man hinh chinh - deu la mot su kien,
         * va su kien nao cung chot khoang ngay luc do. Nen nhip nay khong can day.
         */
        private const val GHI_SU_DUNG_LAI_MS = 5 * 60_000L

        /**
         * Bao lau xet lai mot lan trong luc dang chan nhac ma chua co quyen doc thong
         * bao.
         *
         * Ba giay: ngan hon bon giay he thong tu tra lai am luong cho app khong chiu
         * nhuong tieng (thu tren may ao Android 13 ngay 25/9/2026), nen app do khong
         * kip keu lai.
         */
        private const val NHIP_CHAN_NHAC_MS = 3_000L

        /** Vang mat ngan hon the thi khong bao, vi bind lai vai giay la chuyen thuong. */
        private const val REPORT_ABSENCE_MS = 60_000L

        /** Man hinh tat lau hon the thi ngung dem gio. */
        private const val PAUSE_AFTER_SCREEN_OFF_MS = 60_000L

        /** Cung mot goi thi it nhat chung nay moi day lai lan nua. */
        private const val CHAN_LAI_SAU_MS = 4_000L

        /** Cach nhau lau hon the thi coi la mot dot moi, dem lai tu dau. */
        private const val QUEN_SAU_MS = 20_000L

        /** Day chung nay lan lien tiep ma no van con thi coi nhu day khong duoc. */
        private const val SO_LAN_COI_NHU_KHONG_DAY = 3

        /**
         * Cua so hut vao chung nay diem anh so voi man hinh thi van coi la toan man.
         *
         * Thanh trang thai, thanh dieu huong, tai tho deu an bot mot phan, nen cua so
         * toan man that su it khi bang dung so do man hinh.
         */
        private const val LECH_CHO_PHEP = 120

        /** Chu tren nut dong hoac thu nho cua so, du tieng Anh lan tieng Viet. */
        private val TU_KHOA_NUT_DONG = listOf(
            "close", "dismiss", "exit", "minimize", "collapse",
            "đóng", "thu nhỏ", "thoát", "tắt"
        )

        /** Keo dai hon chung nay moi dang mot tin cho Ba Huy. */
        private const val LAU_MOI_BAO_MS = 30_000L

        /** Va it nhat chung nay moi bao lan nua, du la app nao. */
        private const val CACH_BAO_MS = 30 * 60_000L

        /** Loi nhac "dong cua so noi" hien bao lau moi lan. */
        private const val NHAC_HIEN_MS = 2_000L

        /** Roi lui ra bao lau cho con kip bam dong cua so do. */
        private const val NHAC_LUI_MS = 2_000L

        /** Thanh thong bao con mo tren app bi chan thi bao lau xet lai mot lan. */
        private const val XET_LAI_DUOI_THONG_BAO_MS = 2_000L

        /**
         * Lop phu che app duoi thanh thong bao tu tat sau chung nay. Dai hon nhip xet
         * lai o tren mot chut, de lop phu khong nhay giua hai lan hien.
         */
        private const val CHE_DUOI_THONG_BAO_MS = 3_000L

        /** Nhat ky ghi chuyen keo thanh thong bao it nhat chung nay moi mot lan. */
        private const val GHI_CHE_DUOI_MS = 10 * 60_000L

        /** Bao truoc khi con 5 phut. */
        private const val WARN_BEFORE_MS = 5 * 60_000L

        /** Cho ung dung, chan ca trong gio choi de con khong tu cai app moi. */
        val STORE_PACKAGES = setOf(
            "com.android.vending",
            "com.xiaomi.market",
            "com.xiaomi.mipicks",
            "com.huawei.appmarket"
        )

        /**
         * Nhung app mo duong toi cho tat service nay hoac go app. Chan chung lai
         * la mat xich giu cho ca vong khoa dung vung: muon tat guard thi phai vao
         * Settings, ma duong vao Settings do chinh guard giu.
         *
         * Khong bit duoc Safe Mode: khoi dong vao che do an toan thi service khong
         * chay va con vao Settings thoai mai. Muon bit ca duong do thi phai la
         * Device Owner.
         */
        val GUARDED_SYSTEM_PACKAGES = setOf(
            "com.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.securitycenter",
            "com.miui.securityadd",
            "com.miui.cleanmaster",
            "com.lbe.security.miui"
        )

        /** Khong bao gio chan, du danh sach trang co ghi hay khong. */
        /**
         * Do dung len man hinh chinh nhung mang ten goi khac: o tim kiem, tro ly,
         * man hinh am mot. Chi duoc mien khi man hinh chinh dang hien cung luc.
         */
        val BAN_CUA_MAN_HINH_CHINH = setOf(
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.searchlite",
            "com.android.quicksearchbox",
            "com.miui.personalassistant",
            "com.mi.android.globalpersonalassistant"
        )

        val ALWAYS_ALLOWED = setOf(
            "com.android.systemui",
            "android",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.emergency",
            "com.android.intentresolver",
            // Hop thoai xin quyen (mic, may anh, vi tri). Chan cho nay thi app trong
            // danh sach trang khong bao gio xin duoc quyen no can.
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.miui.permcenter"
        )
    }
}
