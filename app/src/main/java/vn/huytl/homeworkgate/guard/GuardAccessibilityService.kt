package vn.huytl.homeworkgate.guard

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
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

    private val nhipSuDungRunnable = Runnable { runCatching { capNhatSuDung() } }

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

    /** Gom chu con go vao app AI de dung lai cau hoan chinh. Xem [BoGoAi]. */
    private val boGoAi = BoGoAi()

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
                }
                Intent.ACTION_SCREEN_ON -> batManHinhLen()
            }
        }
    }


    /** Lan gan nhat da bat im tieng, de khong bat di bat lai lien tuc. */
    private var lastHushMs = 0L

    /** Lan gan nhat doc lai danh sach mien tru he thong. */
    private var lanDocMienTru = 0L

    /**
     * Con bam play lai tu thanh thong bao thi khong co cua so nao mo ra, nen
     * [onAccessibilityEvent] khong chay va tieng cu the ma phat. Cho nay nghe
     * thang su kien phat tieng.
     *
     * Khong chan tieng cua app trong danh sach trang: chung duoc phep dung ca khi
     * het gio, ma mot app hoc tieng Anh khong doc duoc thanh thi coi nhu hong.
     */
    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            if (configs.isEmpty()) return
            if (ParentMode.isActive(this@GuardAccessibilityService)) return
            if (gate.isOpen()) return

            val pkg = currentPackage
            if (pkg != null && (pkg in prefs.allowedPackages || pkg in systemEssentials)) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastHushMs < HUSH_COOLDOWN_MS) return
            lastHushMs = now

            Log.i(TAG, "co tieng phat trong luc dang khoa, bat im (app truoc mat=$pkg)")
            AudioHush.hush(this@GuardAccessibilityService)
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
        // Android nho cau hinh dich vu tro nang tu luc BAT no trong Settings. Cai
        // de mot ban moi len - qua adb hay qua ban cap nhat - KHONG lam no doc lai
        // XML: service van chay voi cau hinh cu. Nen may da bat truoc khi co tinh
        // nang ghi cau hoi AI se khong bao gio nghe su kien go chu, du XML ban moi
        // da khai bao. Trieu chung dung la: cap nhat xong, AI khong bao tin gi.
        //
        // Sua tay serviceInfo luc ket noi thi co hieu luc ngay, khoi bat ba tu vao
        // Settings tat/bat lai dich vu sau moi lan cap nhat.
        runCatching {
            serviceInfo = serviceInfo.apply {
                eventTypes = eventTypes or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            }
        }.onFailure { Log.w(TAG, "khong dat duoc eventTypes: ${it.message}") }

        systemEssentials = readSystemEssentials()
        dangChay = true

        // Duong sang app Bang dieu khien di ke dich vu nay.
        //
        // Day la tien trinh duy nhat trong app song 24/7: Android giu dich vu tro
        // nang, ke ca khi cong dang khoa va ApprovalService da tu tat. Gan listener
        // Firestore vao day thi lenh cua Ba Huy toi trong duoi mot giay ma khong
        // phai mo them mot service nen nao, khong phai nhung khoa may chu cho FCM.
        runCatching { DongBo.batDau(this) }
            .onFailure { Log.w(TAG, "khong bat duoc dong bo: ${it.message}") }
        soMienTru = systemEssentials.size
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
        Log.i(TAG, "service len, mien tru=${systemEssentials.size} app, " +
            "danh sach trang=${prefs.allowedPackages.size} app, state=${gate.state}")
        baoNeuVuaVangMat()
        syncTicker()
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val loai = event?.eventType ?: return

        // Vao hoac ra khoi che do chia doi man hinh co khi chi ban su kien
        // "danh sach cua so doi" chu khong ban "cua so truoc mat doi". Bo qua no la
        // bo sot dung luc can xet lai nhat.
        if (loai == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            evaluate(currentPackage ?: packageName)
            syncTicker()
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
        val pkg = event.packageName?.toString() ?: return
        if (pkg != packageName) currentPackage = pkg
        // Con roi app AI sang app khac: cau dang go do, neu co, la da xong.
        if (pkg !in prefs.aiPackages) {
            boGoAi.roiApp()?.let { chotCauHoi(it) }
        }
        evaluate(pkg)
        syncTicker()
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
        val text = event.text?.joinToString(" ")?.trim().orEmpty()
        goiAiDangGo = pkg
        boGoAi.goChu(text, SystemClock.elapsedRealtime())?.let { chotCauHoi(it) }
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
            AudioHush.hush(this)
            warned = false
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
        val coHan = hien.filter { GioiHanApp.han(this, it) > 0 }.toSet()

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
        // Mot phut la tran tren cho moi lan chot: nhip xet la hai muoi giay, so lon
        // hon the nghia la may vua ngu day hoac vua bi dong bang, khong phai xem that.
        if (troiQua in 1..60_000L) GioiHanApp.congThem(this, pkg, troiQua)
    }

    /** Dung dem het, dung khi tat man hinh hoac dich vu dung lai. */
    private fun chotHetGio() {
        val bayGio = SystemClock.elapsedRealtime()
        dangDemGio.keys.toList().forEach { chotGio(it, bayGio) }
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
        val hien = if (!sang) emptySet()
        else (hienTai ?: goiDangHien()).filter { dangTheoDoi(it) }.toSet()

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
    }

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
     * Thanh thong bao (hoac trung tam dieu khien) dang keo xuong hay khong.
     *
     * Cua so cua no do he thong giu, kieu TYPE_SYSTEM, va chi nhan tieu diem khi
     * dang mo that. Thanh trang thai nam yen o tren thi khong nhan tieu diem, nen
     * khong nham.
     */
    private fun manThongBaoDangMo(): Boolean = runCatching {
        windows.any { it.type == AccessibilityWindowInfo.TYPE_SYSTEM && it.isFocused }
    }.getOrDefault(false)

    /**
     * Tat ca goi dang co cua so ung dung tren man hinh.
     *
     * Chia doi man hinh thi co hai, nen khong the chi nhin goi cua su kien vua roi:
     * dua tre de mot app duoc phep o nua tren, YouTube o nua duoi, va su kien cuoi
     * cung mang ten cai duoc phep.
     *
     * Tra ve tap rong neu he thong khong cho doc - luc do [evaluate] quay ve cach cu
     * la xet mot goi cua su kien.
     */
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

    private fun goiDangHien(): Set<String> = runCatching {
        windows.asSequence()
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { cuaSo ->
                val root = cuaSo.root
                val ten = root?.packageName?.toString()
                root?.recycle()
                ten
            }
            .toSet()
    }.getOrDefault(emptySet())

    private fun evaluate(pkgSuKien: String) {
        if (ParentMode.isActive(this)) {
            overlay.hide()
            return
        }

        // Dang keo thanh thong bao xuong thi khong xet gi ca. Luc do app nam phia sau
        // van con trong danh sach cua so, nhung nguoi dung dang nhin thanh thong bao
        // chu khong dung app do - vao day ma xet thi cu keo thanh thong bao xuong la
        // bi bao "het gio roi", ke ca khi chi dinh bam nut tam dung nhac. Dong thanh
        // do lai la xet lai binh thuong.
        if (manThongBaoDangMo()) return

        val docDuocCuaSo = goiDangHien()
        val hien = docDuocCuaSo.ifEmpty { setOf(pkgSuKien) }
        demGioTungApp(hien)
        capNhatSuDung(hien)
        Log.d(TAG, "xet ${hien.joinToString(",")}: state=${gate.state} " +
            "conGio=${gate.isOpen()} moToanBo=${ParentMode.isActive(this)} " +
            "docDuocCuaSo=${docDuocCuaSo.isNotEmpty()}")

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
            systemEssentials = readSystemEssentials()
            goiManHinhChinh = readHomePackages()
            soMienTru = systemEssentials.size
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
                    AudioHush.hush(this)
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
            gate.state == GateState.PENDING -> "Đã gửi bài rồi, đang chờ Ba Huy duyệt"
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
        systemEssentials = readSystemEssentials()
        return pkg in systemEssentials
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
        private const val TICK_INTERVAL_MS = 20_000L

        /** Bao lau nhin lai mot lan xem app do con nam truoc mat khong. */
        private const val NHIP_SU_DUNG_MS = 30_000L

        /**
         * Nhip tre hon the thi coi nhu vua mat dau, khong noi khoang do lai.
         *
         * Ba nhip: mot nhip truot la chuyen thuong khi may dang ban, ba nhip lien
         * thi da la tien trinh bi dong bang.
         */
        private const val MAT_DAU_SU_DUNG_MS = 3 * NHIP_SU_DUNG_MS

        /** Khoang ngan hon the khong ghi vao so: luot qua chu khong phai dung. */
        private const val NGAN_NHAT_SU_DUNG_MS = 5_000L

        /** Khoang dang mo bao lau moi ghi xuong prefs mot lan. */
        private const val GHI_SU_DUNG_LAI_MS = 2 * 60_000L

        /** Bat im xong thi chinh viec do lai sinh ra su kien, nen phai cho mot nhip. */
        private const val HUSH_COOLDOWN_MS = 3_000L

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
