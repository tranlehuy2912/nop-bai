package vn.huytl.homeworkgate.telegram

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.SystemClock
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vn.huytl.homeworkgate.App
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.ChatFrom
import vn.huytl.homeworkgate.data.ChatLine
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.NhatKyAi
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.HopThuBaNoi
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.LoaiNhac
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.ai.AiChamBai
import vn.huytl.homeworkgate.ai.ChamBaiIO
import vn.huytl.homeworkgate.data.CaptureStage
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.guard.DaiNhac
import vn.huytl.homeworkgate.guard.ManChan
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.guard.ChuongTin
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.Permissions
import vn.huytl.homeworkgate.ui.ChatActivity
import vn.huytl.homeworkgate.ui.HomeActivity
import android.content.BroadcastReceiver
import android.content.IntentFilter
import java.util.Calendar

/**
 * Giu duong day voi Telegram trong luc cong khong khoa.
 *
 * Chi song khi dang cho duyet hoac dang trong phien choi. Cong khoa thi service
 * tu dung, khong giu ket noi mang nao ca. Do la ly do khong dung WorkManager:
 * viec nay can phan ung trong vai giay chu khong phai vai phut.
 */
class ApprovalService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var gate: GateStore

    private var scope: CoroutineScope? = null
    private var pollJob: Job? = null

    /**
     * Mot client duy nhat cho ca vong long-poll lan dong ho dem nguoc.
     *
     * Dung chung de dong ho dem nguoc di ke ket noi TLS da mo san cua long-poll,
     * khong phai bat tay lai tu dau moi phut. Do la ly do no gan nhu khong ton pin.
     */
    private val tg: TelegramClient by lazy { TelegramClient(prefs.botToken) }

    /** Dang trong khung gio thoi nghe hay khong, chi de ghi log mot dong khi doi. */
    private var ngungTruoc = false

    /** Lan gan nhat da sua tin ghim, de sua dung mot lan moi phut. */
    private var lanSuaTinGhim = 0L

    /** Lan gan nhat da ghe hop thu cua ba noi. */
    private var lanNgoHopThu = 0L

    /** Cau da ghim lan truoc, de khong sua lai khi khong co gi doi. */
    private var tinGhimTruoc = ""
    private var uiJob: Job? = null

    // ===================== Phan nhac soan tap vo =====================
    // Gop vao day chu khong de mot dich vu nen thu hai. Hai dich vu nen nghia la
    // hai thong bao thuong truc va hai lan bi HyperOS de y, ma viec chung lam thi
    // gan nhau: cung nam cho Le Hoa mo may, cung nghe lenh cua Ba Huy tren Telegram.

    private lateinit var dai: DaiNhac
    private lateinit var chan: ManChan
    private lateinit var khoTin: KhoTinCuaCo

    /**
     * Nhip xet loi nhac. Chi chay trong luc man hinh sang.
     *
     * Khac han [uiJob] von chay ca ngay: loi nhac chi co nghia khi co nguoi dang
     * cam may. Man hinh tat la tat nhip, khong danh thuc CPU de nhac mot cai man
     * hinh dang tat.
     */
    private var nhacJob: Job? = null

    /** Dang o nguong dem tung giay truoc moc buong may. */
    @Volatile
    private var dangDemGiay = false

    /**
     * Nghe man hinh bat tat.
     *
     * ACTION_USER_PRESENT bat buoc dang ky luc chay: tu Android 8, khai trong
     * manifest la khong nhan duoc.
     */
    private val nhanSuKienManHinh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                // Vua mo khoa: day la luc dang de nhac nhat, nen xet lai ngay chu
                // khong cho het nhip dang do. Khong co dong nay thi loi nhac hien
                // muon toi mot phut sau khi Le Hoa mo may - ma mot phut la du de
                // no mo xong game va quen mat.
                // Mo khoa la mot lan cam may moi: bo ky nghi cua the nhac, de loi
                // nhac bay ra ngay chu khong bi tinh vao lan vua hien cach day vai
                // phut. Bat man hinh khong mo khoa thi khong tinh.
                Intent.ACTION_USER_PRESENT -> {
                    dai.batLaiChuKy()
                    xetLaiNgay()
                }
                Intent.ACTION_SCREEN_ON -> xetLaiNgay()
                Intent.ACTION_SCREEN_OFF -> {
                    dai.an()
                    // Khong an man chan: tat man roi bat lai ma het chan thi chi
                    // can bam nut nguon hai cai la thoat.
                    tatNhipNhac()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "ApprovalService onCreate")
        prefs = Prefs.get(this)
        gate = GateStore(this)
        // Duong sang Bang dieu khien thuong di ke dich vu tro nang. Bat lai o day
        // cho truong hop quyen do dang tat: luc ay it ra nhung gi service nay lam
        // - duyet bai, cap gio - van hien sang dien thoai Ba Huy.
        runCatching { DongBo.batDau(this) }
        createChannel()
        // Don cau chat qua cu ngay o day: service nay khoi dong lai nhieu lan trong
        // ngay, nen lich su cu khong nam lai trong may du khong ai mo man chat.
        ChatBox.donDep(this)

        khoTin = KhoTinCuaCo(this)
        dai = DaiNhac(this)
        chan = ManChan(this).apply {
            // Cham vao man chan chi mo app Nop bai, khong mo khoa gi ca - giong het
            // lop phu "Het gio roi" cua GuardAccessibilityService. Trong app do co
            // the soan cap, va co o khoa goc tren de Ba Huy go PIN.
            khiBam = {
                startActivity(
                    Intent(this@ApprovalService, HomeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }
        }
        registerReceiver(
            nhanSuKienManHinh,
            IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            Context.RECEIVER_NOT_EXPORTED
        )

        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: state=${gate.state} canGiu=${canGiuKetNoi(this)} " +
            "token=${if (prefs.botToken.isEmpty()) "TRONG" else "co"} chatid=${prefs.parentChatId}")
        if (!canGiuKetNoi(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (scope == null) {
            val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope = s
            pollJob = s.launch { pollLoop() }
            uiJob = s.launch { uiLoop() }
            s.launch { khaiBaoMenuLenh() }
            batNhipNhac()
        } else if (ngungTruoc && !dangNgungNghe()) {
            // Dang thoi nghe giua dem ma co viec that: con vua nop bai, hay Ba Huy mo
            // may. Danh thuc vong long-poll day chu khong de no nam not muoi phut -
            // luc do bam Duyet ben Telegram cung phai cho.
            pollJob?.cancel()
            pollJob = scope?.launch { pollLoop() }
        }

        if (intent?.action == ACTION_GUI) {
            val nhom = CaptureStage.entries.associateWith { st ->
                intent.getStringArrayListExtra(EXTRA_ANH + st.name).orEmpty()
                    .map { java.io.File(it) }
            }.filterValues { it.isNotEmpty() }
            val pham = PhamVi.tuJson(intent.getStringExtra(EXTRA_PHAM))
            val ket = ChamBaiIO.doc(intent.getStringExtra(EXTRA_BAN_CHAM))
            scope?.launch { guiRoiCap(nhom, pham, ket) }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(nhanSuKienManHinh) }
        tatNhipNhac()
        dai.an()
        chan.an()
        pollJob?.cancel()
        uiJob?.cancel()
        scope?.cancel()
        scope = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Nhip xet loi nhac soan tap, chi song trong luc man hinh sang.
     *
     * Nhanh cham theo viec: dang chan hoac dang dem nguoc tung giay thi moi giay
     * mot lan cho dong ho chay that, con lai mot phut mot lan la du - khong co moc
     * nao trong thoi khoa bieu can do chinh xac hon the.
     */
    private fun batNhipNhac() {
        if (nhacJob != null) return
        nhacJob = scope?.launch {
            while (true) {
                delay(if (xetLoiNhac()) 1_000L else NHIP_NHAC_MS)
            }
        }
    }

    private fun tatNhipNhac() {
        nhacJob?.cancel()
        nhacJob = null
    }

    /**
     * Xet lai ngay lap tuc, khong cho het nhip dang do.
     *
     * Dung lai chinh vong nhip chu khong chay mot lan rieng: hai duong cung goi
     * [xetLoiNhac] thi co luc chung chay chong len nhau, ma ham do vua doc vua ve
     * len man hinh.
     */
    private fun xetLaiNgay() {
        tatNhipNhac()
        batNhipNhac()
    }

    /**
     * Xet xem ngay luc nay co gi dang treo khong, roi hien dung mot thu.
     *
     * Thu tu xet la thu tu uu tien, tu tren xuong:
     *  - Ba Huy dang dung may thi thoi, khong chan ma cung khong nhac;
     *  - man chan xet truoc the nhac, vi no phu mot khoang rong hon;
     *  - man nhap PIN dang mo thi tam nhuong, khong thi man chan che mat o nhap;
     *  - man hinh cua chinh app dang mo thi khong can the nhac nua.
     *
     * @return co phai nhip mot giay khong.
     *
     * Nhip nhanh cham quyet theo loi nhac vua tinh ra, KHONG theo [ManChan.dangHien].
     * Man chan hien bang mot lenh post len main handler, nen ngay sau khi goi hien()
     * thi dangHien van con false - hoi no o day la nga ra nhip mot phut dung luc
     * vua bat dau chan: dong ho tren man chan dung im, va Ba Huy bam mo khoa thi
     * man PIN bi che mat gan mot phut truoc khi vong sau kip nhuong cho no.
     */
    private suspend fun xetLoiNhac(): Boolean {
        if (ParentMode.isActive(this)) {
            withContext(Dispatchers.Main) { dai.an(); chan.an() }
            dangDemGiay = false
            return false
        }

        val nhac = TinhLoiNhac.tinh(
            Calendar.getInstance(),
            prefs.buoiDaSoan,
            prefs.batManChan,
            prefs.buoiDuocMoSom
        )
        dangDemGiay = (nhac?.giayConLai ?: -1) >= 0
        val trongGioChan = nhac?.loai == LoaiNhac.CHAN

        withContext(Dispatchers.Main) {
            when {
                nhac == null -> { dai.an(); chan.an() }

                // Man chan nhuong cho chinh app Nop bai.
                //
                // Khong phai lo hong: trong app do khong co gi de choi, ma lai co
                // dung viec dang can lam la soan cap. Doi lai, o khoa goc tren man
                // chinh chay binh thuong, nen Ba Huy van go PIN o dung mot cho nhu
                // moi khi - khong phai de rieng mot man nhap PIN cho luc bi chan.
                // Buoc ra khoi app mot cai la man chan che lai ngay.
                nhac.loai == LoaiNhac.CHAN &&
                    App.manHinhCuaAppDangMo -> { dai.an(); chan.an() }

                nhac.loai == LoaiNhac.CHAN -> {
                    dai.an()
                    // Dung lai thi chi thay kim dong ho, khong ve lai ca danh sach
                    // mon moi giay. Noi dung chi doi khi sang buoi khac, ma giua hai
                    // buoi thi man chan da an di roi hien lai.
                    if (chan.dangHien) chan.capNhatDongHo() else chan.hien(nhac)
                }

                // Chinh app nay dang mo thi thoi: man hinh da noi du, ma the nhac
                // con che mat goc tren.
                App.manHinhCuaAppDangMo -> { dai.an(); chan.an() }

                else -> { chan.an(); dai.hien(nhac) }
            }
        }

        // Cat gio choi ngoai khoi Main: pause() ghi thang xuong dia bang commit(),
        // khong nen lam viec do trong luc dang giu luong ve giao dien. Cat ca khi
        // dang mo app Nop bai: dang trong gio di hoc thi khong dot phut choi, du
        // man chan tam nhuong cho.
        if (trongGioChan) catGioChoiDangCo(nhac)

        // Van giu nhip mot giay ca khi man chan dang nhuong cho app: buoc ra khoi
        // app la no phai che lai ngay, khong de ho mot phut.
        return trongGioChan || dangDemGiay
    }

    /**
     * Toi gio di hoc thi giu lai phien choi dang chay.
     *
     * Khong co doan nay thi dong ho gio choi van chay sau lung man chan: Le Hoa
     * khong bam duoc gi ma van mat phut, va den luc tan hoc thi phieu gio da di
     * mat. Dung [GateStore.pause] chu khong cat han vi so phut do la do Ba Huy
     * duyet that - giu lai de choi not sau buoi hoc moi dung.
     *
     * Goi duoc moi nhip: pause() tu tra null khi khong co phien nao dang chay.
     */
    private fun catGioChoiDangCo(nhac: vn.huytl.homeworkgate.data.LoiNhac) {
        val phut = gate.pause() ?: return
        val moTa = nhac.buoi?.let { TinhLoiNhac.moTaBuoi(it) }.orEmpty()
        runCatching { Notifier.catVaoGioHoc(this, phut, moTa) }
        refreshUi()
    }

    /**
     * Day danh sach lenh len Telegram de go dau / la hien menu kem mo ta.
     *
     * Chi lam lai khi danh sach thay doi. Truoc day ham setMyCommands co san nhung
     * khong cho nao goi, nen menu do chua bao gio hien ra trong chat.
     */
    private fun khaiBaoMenuLenh() {
        val dau = tg.dauLenh()
        if (prefs.menuLenhDau == dau) return
        runCatching { tg.setMyCommands() }.onSuccess { prefs.menuLenhDau = dau }
    }

    /**
     * Co dang trong khung gio thoi nam cho Telegram khong.
     *
     * Day la app thoi nghe, khong phai may ngu: tablet van chay binh thuong, van
     * chan app nhu moi khi. Chi khac la thay vi nam cho Telegram lien tuc (25 giay
     * mot lan), app cu muoi phut ghe hoi mot cai - mot dem het ba muoi sau lan hoi
     * thay vi gan chin tram. Lenh Ba Huy go luc nua dem van chay, cham nhat muoi
     * phut.
     *
     * Van nghe lien tuc khi con dang co phien choi, khi may dang mo toan bo, hoac
     * khi con vua nhan tin dang cho tra loi - luc do co nguoi that dang doi.
     */
    private fun dangNgungNghe(): Boolean {
        if (gate.state != GateState.LOCKED) return false
        if (ParentMode.isActive(this)) return false
        if (ChatBox.isWaiting(this)) return false

        val gio = java.util.Calendar.getInstance()
        val phut = gio.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            gio.get(java.util.Calendar.MINUTE)
        return phut >= NGUNG_TU || phut < NGUNG_DEN
    }

    /**
     * Loi mang giua dem thi cho den nhip sau, dung thu lai moi phut.
     *
     *
     * Tablet tat wifi qua dem la ca dem thu lai sau moi phut - nhieu lan danh thuc
     * hon han so voi luc mang binh thuong, tuc la dung cai dinh tiet kiem lai thanh
     * ton nhat.
     */
    private fun cachThuLai(ngung: Boolean, binhThuong: Long): Long =
        if (ngung) maxOf(binhThuong, NHIP_NGUNG_NGHE_MS) else binhThuong

    /** Vong long polling. Moi loi mang deu cho roi thu lai, khong bo cuoc. */
    private suspend fun pollLoop() {
        val client = tg
        var backoffMs = 2_000L

        while (scope?.isActive == true) {
            if (!canGiuKetNoi(this)) {
                stopSelf()
                return
            }
            val ngung = dangNgungNghe()
            if (ngung != ngungTruoc) {
                ngungTruoc = ngung
                Log.i(
                    TAG,
                    if (ngung) {
                        "vao gio thoi nghe, ${NHIP_NGUNG_NGHE_MS / 60_000} phut hoi mot lan"
                    } else {
                        "het gio thoi nghe, nghe Telegram lien tuc tro lai"
                    }
                )
            }
            try {
                val updates = client.getUpdates(
                    offset = prefs.telegramOffset,
                    allowed = listOf("callback_query", "message"),
                    // Gio thoi nghe thi hoi mot cai roi ve, khong nam cho 25 giay.
                    timeoutSec = if (ngung) 0 else TelegramClient.POLL_TIMEOUT_SEC
                )
                backoffMs = 2_000L
                if (prefs.loiTelegram != 0) prefs.loiTelegram = 0
                for (update in updates) {
                    prefs.telegramOffset = update.optLong("update_id", 0L) + 1
                    handleUpdate(client, update)
                }
                // Ghe hop thu cua ba noi ngay tai day.
                //
                // Dat trong vong nay chu khong tao vong rieng la co y: CPU vua thuc
                // day de nhan cau tra loi getUpdates, socket TLS cua OkHttp con dang
                // mo - hoi them mot cau nua luc nay khong them lan danh thuc nao.
                // Tach ra vong rieng thi moi lan hoi la mot lan danh thuc.
                ngoHopThu(client)

                // Co lenh thi xu ly het da roi moi thoi nghe tiep.
                if (ngung && updates.isEmpty()) delay(NHIP_NGUNG_NGHE_MS)
            } catch (e: TelegramClient.ApiException) {
                Log.w(TAG, "Telegram tra loi ${e.errorCode}: ${e.description}")
                // Hai ma nay khong tu khoi phuc duoc, phai co nguoi vao sua. Ghi lai
                // de bang canh bao trong app noi ra, thay vi cu thu lai am tham.
                if (e.errorCode == 401 || e.errorCode == 409) prefs.loiTelegram = e.errorCode
                // 409 la co hai noi cung goi getUpdates tren mot token. Doi lau hon
                // roi thu lai, vi day thuong la do bo mo bot tren may khac.
                delay(cachThuLai(ngung, if (e.errorCode == 409) 30_000L else backoffMs))
                backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
            } catch (e: Exception) {
                Log.w(TAG, "loi mang khi poll: ${e.javaClass.simpleName} ${e.message}")
                delay(cachThuLai(ngung, backoffMs))
                backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
            }
        }
    }

    /**
     * Ghe hop thu cua app "Cho gio choi" ben may ba noi.
     *
     * Chi hoi khi that su can: da noi nhom, hom nay ba chua cho lan nao, dang trong
     * khung 05:00-22:00, va cach lan hoi truoc du lau. Ba cho xong mot lan la ham
     * nay im den sang mai - phan lon thoi gian trong ngay no khong ton mot request
     * nao ca.
     *
     * Nuot moi loi. Hop thu hong khong duoc phep lam chet vong long-poll, vi vong
     * do con dang lo viec chinh la nghe lenh cua Ba Huy.
     */
    private fun ngoHopThu(client: TelegramClient) {
        val gio = SystemClock.elapsedRealtime()
        if (gio - lanNgoHopThu < NHIP_NGO_HOP_THU_MS) return
        if (!HopThuBaNoi.nenNgo(this)) return
        lanNgoHopThu = gio

        val nhom = HopThuBaNoi.nhom(this)
        val lenh = runCatching { HopThuBaNoi.lenhMoi(this, client.getChat(nhom)) }
            .onFailure { Log.w(TAG, "khong doc duoc hop thu: ${it.message}") }
            .getOrNull() ?: return

        // Danh dau truoc khi lam gi. Lenh nay chi duoc xet dung mot lan, du phan
        // duoi co cap duoc hay khong: khong danh dau ma cap hong thi no nam trong
        // mo ta nhom, cu mot phut lai duoc doc lai mot lan.
        HopThuBaNoi.danhDauDaDoc(this, lenh)

        val con = getString(R.string.child_name)
        val tre = System.currentTimeMillis() - lenh.guiLuc * 1000L
        if (tre > HopThuBaNoi.QUA_CU_MS) {
            val luc = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
                .format(Date(lenh.guiLuc * 1000L))
            runCatching {
                client.sendMessage(
                    prefs.parentChatId,
                    "Bà nội bấm cho ${lenh.phut} phút lúc $luc, lâu quá rồi nên máy bỏ qua."
                )
            }
            return
        }

        // Tinh luot cua ba ngay khi nhan, truoc ca khi biet cap duoc hay khong.
        // Qua gio chot ma khong tinh thi ba bam lai duoc, ma bam lai cung the.
        HopThuBaNoi.ghiNhanDaCho(this)
        DayLog.add(this, "Bà nội cho ${lenh.phut} phút")

        // Boc runCatching vi tin nay chi de bao. Truoc day no goi thang, va mot lan
        // rot mang ngay dong nay la nem ra khoi ham, bo lai lenh da danh dau da doc
        // ma chua ai cap gio - ba mat luot ca ngay ma khong ai biet. Cap gio truoc,
        // bao sau, va bao hong thi thoi.
        runCatching {
            client.sendMessage(
                prefs.parentChatId,
                "Bà nội vừa bấm cho $con chơi ${lenh.phut} phút."
            )
        }
        capGio(
            client, prefs.parentChatId, lenh.phut,
            useQuota = false, why = "bà nội cho", nhanCho = "Bà nội cho chơi"
        )
        Log.i(TAG, "hop thu: ba noi cho ${lenh.phut} phut cho $con")
    }

    private suspend fun handleUpdate(client: TelegramClient, update: JSONObject) {
        update.optJSONObject("callback_query")?.let { cq ->
            handleCallback(client, cq)
            return
        }
        update.optJSONObject("message")?.let { msg ->
            handleMessage(client, msg)
        }
    }

    private suspend fun handleCallback(client: TelegramClient, cq: JSONObject) {
        val callbackId = cq.optString("id")
        val fromId = cq.optJSONObject("from")?.optLong("id") ?: 0L

        // Chi bo moi duoc bam. Nguoi la co token cung khong tu duyet cho minh duoc.
        if (fromId != prefs.parentChatId) {
            client.answerCallbackQuery(callbackId, "Bạn không có quyền duyệt.")
            return
        }

        val data = cq.optString("data")
        val chatId = cq.optJSONObject("message")?.optJSONObject("chat")?.optLong("id")
            ?: prefs.parentChatId
        val messageId = cq.optJSONObject("message")?.optLong("message_id") ?: 0L

        when {
            data.startsWith("a:") -> {
                // "a:<ma>" la so phut mac dinh, "a:<ma>:<phut>" la ba chon so khac.
                val phan = data.removePrefix("a:").split(":")
                val requestId = phan[0]
                val soPhut = phan.getOrNull(1)?.toIntOrNull()
                if (gate.baiDangCho().none { it.id == requestId }) {
                    client.answerCallbackQuery(callbackId, "Yêu cầu này cũ rồi.")
                    if (messageId != 0L) client.clearReplyMarkup(chatId, messageId)
                    return
                }
                // Con da bam Bat dau trong luc cho duyet: cong thang vao phien dang
                // chay chu khong bao "khong cap duoc". Duyet bai xong ma may tra loi
                // "khong cap duoc" thi ba tuong hong.
                val dangChoi = gate.state == GateState.ACTIVE
                val minutes = if (dangChoi) {
                    gate.extend(soPhut ?: prefs.grantMinutes)?.also { gate.boBaiCho(requestId) }
                } else {
                    gate.approve(wantedMinutes = soPhut, requestId = requestId)
                }
                if (minutes == null) {
                    client.answerCallbackQuery(callbackId, "Không cấp được: giờ ngủ hoặc hết định mức.")
                    client.sendMessage(
                        chatId,
                        "Không cấp giờ được. Hoặc đang trong giờ ngủ, " +
                            "hoặc hôm nay Lê Hòa đã dùng hết định mức phút."
                    )
                } else if (dangChoi) {
                    val them = soPhut ?: prefs.grantMinutes
                    DayLog.add(this, "Duyệt $them phút, cộng vào phiên đang chạy")
                    client.answerCallbackQuery(callbackId, "Đã cộng $them phút.")
                    client.editCaption(chatId, messageId, "Đã duyệt, cộng $them phút.")
                    client.sendMessage(
                        chatId,
                        "${getString(R.string.child_name)} đang chơi nên cộng thẳng " +
                            "$them phút vào phiên. Còn $minutes phút.${conChoBaoNhieu()}"
                    )
                } else {
                    DayLog.add(this, "Duyệt $minutes phút (chưa tính giờ)")
                    client.answerCallbackQuery(callbackId, "Đã duyệt $minutes phút.")
                    client.editCaption(chatId, messageId, "Đã duyệt $minutes phút.")
                    client.sendMessage(
                        chatId,
                        "Đã duyệt $minutes phút (chưa tính giờ). Hôm nay còn " +
                            "${gate.phutConLaiHomNay()} phút.${conChoBaoNhieu()}"
                    )
                }
                if (minutes != null) {
                    // Danh dau ben Bang dieu khien nua, khong thi bai da duyet o
                    // Telegram van nam trong danh sach "dang cho" ben dien thoai.
                    DongBo.datTrangThaiBai(
                        this, requestId, "DUYET", soPhut ?: prefs.grantMinutes
                    )
                }
                if (messageId != 0L) client.clearReplyMarkup(chatId, messageId)
                withContext(Dispatchers.Main) { refreshNotification() }
            }

            data.startsWith("r:") -> {
                // Bo dung bai cua nut vua bam. Cac bai khac dang cho van nam nguyen:
                // bai nay sai khong co nghia la bai kia cung sai.
                val boBai = data.removePrefix("r:").substringBefore(':')
                gate.boBaiCho(boBai)
                DongBo.datTrangThaiBai(this, boBai, "TUCHOI")
                DayLog.add(this, "Ba Huy bấm không duyệt")
                client.answerCallbackQuery(callbackId, "Đã từ chối.")
                if (messageId != 0L) {
                    client.clearReplyMarkup(chatId, messageId)
                    client.editCaption(chatId, messageId, "Không duyệt.")
                }
                if (gate.soBaiDangCho() > 0) {
                    client.sendMessage(chatId, "Còn ${gate.soBaiDangCho()} bài đang chờ duyệt.")
                }
                withContext(Dispatchers.Main) { refreshNotification() }
            }

            // Nut cu con nam trong lich su chat. Khong tra loi thi Telegram quay
            // vong tren may Ba Huy cho den khi het gio - tuong nhu may treo. Nut
            // "Tat ngay" da bo nam trong so nay.
            else -> {
                client.answerCallbackQuery(callbackId, "Nút này không còn dùng nữa.")
                if (messageId != 0L) client.clearReplyMarkup(chatId, messageId)
            }
        }
    }

    /**
     * Lenh go tay tu chat cua ba.
     *
     * Khong lenh nao hoi PIN: chat id da la bang chung day. Do cung la duong cuu
     * ho khi ba quen PIN, vi luc ay man hinh tren tablet khong con mo duoc nua.
     *
     * Lenh nao doi so thi thieu so van chay duoc, lay muc mac dinh, vi ba thuong
     * go vao luc dang ban chu khong ngoi tra cu phap.
     */
    private fun handleMessage(client: TelegramClient, msg: JSONObject) {
        val fromId = msg.optJSONObject("from")?.optLong("id") ?: 0L
        if (fromId != prefs.parentChatId) return
        val text = msg.optString("text").trim()
        val chatId = msg.optJSONObject("chat")?.optLong("id") ?: prefs.parentChatId
        val word = text.substringBefore(' ').removePrefix("/").lowercase()
        val arg = text.substringAfter(' ', "").trim()
        val con = getString(R.string.child_name)

        // Lenh go qua lau roi thi khong lam nua.
        //
        // Telegram giu lai tin chua ai doc trong 24 tieng. Tablet mat mang ca buoi
        // toi, hay bi tat nguon, thi sang hom sau vua len mang la ca xau lenh do do
        // xuong mot luc - "/cho 60" go toi qua tu dung mo gio choi vao sang som ma
        // khong ai bam gi. Tra loi de Ba Huy biet no khong chay, chu khong im.
        //
        // Chi loc lenh. Tin nhan chu thuong gui cho con thi den muon van co nghia.
        val guiLuc = msg.optLong("date", 0L) * 1000L
        if (text.startsWith("/") && guiLuc > 0L &&
            System.currentTimeMillis() - guiLuc > LENH_QUA_CU_MS
        ) {
            val luc = SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(guiLuc))
            client.sendMessage(
                chatId,
                "Lệnh /$word gõ lúc $luc, lâu quá rồi nên máy bỏ qua. Gõ lại nếu vẫn cần."
            )
            return
        }

        when (word) {
            // --- duyet bai dang cho ---
            // Go tay thi duyet bai cu nhat dang cho. Muon duyet dung mot bai giua
            // dam thi bam nut ngay duoi anh bai do - nut mang san ma cua bai.
            "duyet", "ok" -> {
                val bai = gate.baiChoCuNhat()
                if (bai == null) {
                    client.sendMessage(chatId, "Không có bài nào đang chờ duyệt.")
                    return
                }
                capGio(client, chatId, arg.toIntOrNull(), useQuota = true, why = "duyệt bài", bai = bai)
            }

            "tuchoi" -> {
                val bai = gate.baiChoCuNhat()
                if (bai == null) {
                    client.sendMessage(chatId, "Không có bài nào đang chờ.")
                    return
                }
                gate.boBaiCho(bai.id)
                DongBo.datTrangThaiBai(this, bai.id, "TUCHOI")
                if (bai.messageId != 0L) client.clearReplyMarkup(chatId, bai.messageId)
                DayLog.add(this, "Ba Huy không duyệt" + if (arg.isEmpty()) "" else ": $arg")
                client.sendMessage(
                    chatId,
                    (if (arg.isEmpty()) "Đã từ chối." else "Đã từ chối, lý do: $arg") +
                        conChoBaoNhieu()
                )
                refreshUi()
            }

            // --- gio choi ---
            // /them la ten goi khac cua /cho: dang choi thi capGio cong thang so
            // phut vao phien dang chay, dung viec ma /them van lam.
            "cho", "thuong", "them" -> {
                // Gio thuong: khong tru vao han muc ngay, vi day la ba chu dong cho
                // chu khong phai con doi bang bai tap.
                capGio(client, chatId, arg.toIntOrNull(), useQuota = false, why = "Ba Huy cho thêm")
            }

            "bot" -> {
                val phut = arg.toIntOrNull() ?: 15
                val left = gate.extend(-phut)
                when {
                    left == null -> client.sendMessage(chatId, "$con đang không trong giờ chơi.")
                    left <= 0 -> {
                        DayLog.add(this, "Ba Huy bớt giờ, hết luôn phiên")
                        client.sendMessage(chatId, "Bớt $phut phút là hết giờ luôn. Đã khoá.")
                        refreshUi()
                    }
                    else -> {
                        DayLog.add(this, "Ba Huy bớt $phut phút")
                        client.sendMessage(chatId, "Đã bớt $phut phút. Còn lại $left phút.")
                        refreshUi()
                    }
                }
            }

            "dung", "nghi" -> {
                val left = gate.pause()
                if (left == null) {
                    client.sendMessage(chatId, "$con đang không trong giờ chơi.")
                } else {
                    DayLog.add(this, "Ba Huy cho tạm dừng, giữ $left phút")
                    client.sendMessage(
                        chatId,
                        "Đã tạm dừng, giữ $left phút. Gõ /tiep khi cho chơi lại."
                    )
                    refreshUi()
                }
            }

            "tiep" -> {
                val phut = gate.resume()
                if (phut == null) {
                    client.sendMessage(
                        chatId,
                        if (gate.state == GateState.LOCKED) "Không còn phiên nào đang tạm dừng."
                        else "$con đang không tạm dừng."
                    )
                } else {
                    DayLog.add(this, "Ba Huy cho chơi tiếp $phut phút")
                    client.sendMessage(chatId, "Chơi tiếp, còn $phut phút.")
                    refreshUi()
                }
            }

            /**
             * Chon cho lam hop thu cho app "Cho gio choi" ben may ba noi.
             *
             * Go /nhom o dau thi cho do thanh hop thu. Go trong chat rieng nay cung
             * duoc: bot ghim duoc tin trong chat rieng, va app cu khong ghim gi bao
             * gio nen khong ai giam chan ai. Go trong mot nhom rieng thi sach hon -
             * khong lam ban chat nay - nhung phai dat bot lam quan tri vien.
             *
             * Muon chi dinh cho khac thi /nhom <ma>.
             *
             * Tra ve kem ma so de dan thang sang app ben may ba, khoi di dau tim.
             */
            "nhom" -> {
                val moi = arg.toLongOrNull() ?: chatId
                HopThuBaNoi.datNhom(this, moi)
                val laNhom = moi != prefs.parentChatId
                client.sendMessage(
                    chatId,
                    (if (moi == chatId) "Đã lấy chỗ này làm hộp thư cho máy bà nội."
                    else "Đã đặt hộp thư là $moi.") +
                        "\n\nMã: $moi\n\n" +
                        "Mở app Cho giờ chơi trên máy bà, bấm bánh răng góc phải, dán mã " +
                        "này vào." +
                        if (laNhom) {
                            " Nhớ đặt bot làm quản trị viên trong nhóm đó, bật quyền " +
                                "Đổi thông tin nhóm và Ghim tin nhắn."
                        } else {
                            ""
                        }
                )
            }

            "khoa", "tat" -> {
                ParentMode.disable(this)
                gate.endSession(EndReason.PARENT_REVOKED)
                DayLog.add(this, "Ba Huy khoá máy")
                client.sendMessage(chatId, "Đã khoá tablet.")
                refreshUi()
            }

            "trangthai" -> {
                val left = gate.remainingMs() / 60_000
                val state = when {
                    ParentMode.isActive(this) ->
                        "Đang mở toàn bộ máy — ${ParentMode.moTa(this)}"
                    gate.state == GateState.ACTIVE -> "$con đang chơi, còn $left phút"
                    gate.state == GateState.GRANTED ->
                        "Đã duyệt ${gate.grantedMinutes} phút, $con chưa bấm chơi"
                    gate.state == GateState.PAUSED ->
                        "$con đang tạm dừng, giữ lại ${gate.pausedMinutes()} phút"
                    gate.state == GateState.PENDING ->
                        "$con đã nộp ${gate.soBaiDangCho()} bài, đang chờ Ba Huy duyệt"
                    else -> "Tablet đang khoá"
                }
                val admin = if (Permissions.hasDeviceAdmin(this)) "" else
                    "\n⚠ Quản trị thiết bị đang tắt, app gỡ được."
                client.sendMessage(
                    chatId,
                    "$state. Hôm nay đã duyệt ${gate.phutDaDuyetHomNay()} phút, " +
                        "còn ${gate.phutConLaiHomNay()} phút. Giờ ngủ ${gioChot()}.$admin"
                )
            }

            // --- may moc ---
            "bo", "chedobo", "bahuy" -> {
                // Khong ghi so phut thi khong dat han: anh Huy tu nho tat.
                val minutes = arg.toIntOrNull()
                ParentMode.enable(this, minutes)
                ensureRunning(this)
                DayLog.add(this, "Mở toàn bộ máy (${ParentMode.moTa(this)})")
                client.sendMessage(
                    chatId,
                    "Đã mở toàn bộ máy, ${ParentMode.moTa(this)}. " +
                        "Gõ /khoa hoặc gạt công tắc trên tablet để khoá lại."
                )
            }

            "choxoa" -> {
                val dpm = getSystemService(DevicePolicyManager::class.java)
                if (!Permissions.hasDeviceAdmin(this)) {
                    client.sendMessage(chatId, "Quản trị thiết bị đang tắt sẵn rồi, gỡ app được.")
                    return
                }
                dpm.removeActiveAdmin(Permissions.adminComponent(this))
                // Mo luon che do ba, khong thi guard van chan duong vao Cai dat va
                // ba tat quan tri roi ma van khong vao go duoc.
                ParentMode.enable(this, 15)
                DayLog.add(this, "Tắt quản trị thiết bị để gỡ app")
                client.sendMessage(
                    chatId,
                    "Đã tắt quản trị thiết bị, mở máy 15 phút — gỡ app được.\n" +
                        "Còn giữ app thì bật lại quyền trong Cài đặt của app."
                )
                refreshUi()
            }

            "nhatky" -> {
                val log = DayLog.today(this)
                client.sendMessage(
                    chatId,
                    if (log.isBlank()) "Hôm nay chưa có gì xảy ra."
                    else "Hôm nay:\n$log"
                )
            }

            /**
             * Con dung app nao, tu may gio den may gio.
             *
             * /thongke    - hom nay
             * /thongke 1  - hom qua, 2 la hom kia, toi da [NhatKySuDung.GIU_NGAY] ngay
             *
             * Khac /nhatky: ben kia la su kien cua cai cong (duyet, bat dau, het gio),
             * cai nay la mot tieng do di vao dau. Trong gio choi thi cong mo toang nen
             * /nhatky khong con gi de ke, ma do dung la luc can biet nhat.
             */
            "thongke" -> {
                val lui = (arg.toIntOrNull() ?: 0).coerceIn(0, NhatKySuDung.GIU_NGAY - 1)
                client.sendMessage(chatId, NhatKySuDung.tomTat(this, lui, con))
            }

            /**
             * Xem lai cau con da hoi app AI.
             *
             * /hoi        - cua hom nay
             * /hoi tatca  - toan bo nhat ky con giu
             */
            "hoi" -> {
                val log = if (arg == "tatca") NhatKyAi.tatCa(this) else NhatKyAi.homNay(this)
                client.sendMessage(
                    chatId,
                    if (log.isBlank()) {
                        "Chưa ghi được câu hỏi AI nào" +
                            if (arg == "tatca") "." else " hôm nay. Gõ /hoi tatca để xem cả tuần."
                    } else {
                        (if (arg == "tatca") "Câu hỏi AI đã ghi:\n" else "Hôm nay hỏi AI:\n") + log
                    }
                )
            }

            /**
             * Danh sach app AI dang ghi lai cau hoi, va them / bo app.
             *
             * /ai              - liet ke
             * /ai <ten.goi>    - them mot app
             * /aibo <ten.goi>  - bo mot app
             *
             * Ten goi lay o dau: mo app AI tren tablet, go thu mot cau, roi xem /hoi.
             * Ghi duoc thi ten goi dung. Khong thi vao Play Store, phan Chia se, link
             * co dang .../details?id=TEN.GOI.
             */
            "ai" -> {
                val goi = arg.trim()
                if (goi.isEmpty()) {
                    val ds = prefs.aiPackages.sorted().joinToString("\n")
                    client.sendMessage(chatId, "Đang ghi câu hỏi của $con ở các app:\n$ds")
                } else {
                    prefs.aiPackages = prefs.aiPackages + goi
                    client.sendMessage(chatId, "Đã thêm $goi vào danh sách ghi câu hỏi AI.")
                }
            }

            "aibo" -> {
                val goi = arg.trim()
                if (goi.isEmpty() || goi !in prefs.aiPackages) {
                    client.sendMessage(chatId, "Không có $goi trong danh sách. Gõ /ai để xem.")
                } else {
                    prefs.aiPackages = prefs.aiPackages - goi
                    client.sendMessage(chatId, "Đã bỏ $goi.")
                }
            }

            "xoapin" -> {
                // Xoa PIN chu khong dat PIN moi qua chat: PIN go trong chat la
                // no nam lai trong lich su, con cam dien thoai ba la doc duoc.
                prefs.clearPin()
                ParentMode.enable(this)
                client.sendMessage(
                    chatId,
                    "Đã xoá PIN và mở khoá máy.\n" +
                        "Mở app Nộp bài → ổ khoá góc trên phải để đặt PIN mới."
                )
            }

            // --- soan tap vo ---

            "mo" -> {
                val now = Calendar.getInstance()
                val phut = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val buoi = TinhLoiNhac.buoiDangTrongGioChan(now, phut)
                if (buoi == null) {
                    client.sendMessage(chatId, "Tablet đang không bị chặn, không cần mở.")
                } else {
                    prefs.buoiDuocMoSom = TinhLoiNhac.maBuoi(now, buoi)
                    client.sendMessage(
                        chatId,
                        "Đã mở tablet cho hết ${TinhLoiNhac.moTaBuoi(buoi)}. " +
                            "Buổi học sau lại chặn bình thường, anh khỏi phải bật lại."
                    )
                }
            }

            "chan" -> {
                prefs.buoiDuocMoSom = ""
                client.sendMessage(chatId, "Đã chặn lại tablet ngay.")
            }

            "tatchan" -> {
                prefs.batManChan = false
                prefs.buoiDuocMoSom = ""
                client.sendMessage(
                    chatId,
                    "Đã tắt hẳn màn chặn giờ đi học. Từ giờ app chỉ nhắc chứ không " +
                        "chặn nữa. Bật lại bằng /batchan."
                )
            }

            "batchan" -> {
                prefs.batManChan = true
                client.sendMessage(chatId, "Đã bật lại màn chặn giờ đi học.")
            }

            "soanlai" -> {
                val ma = prefs.buoiVuaSoan
                if (ma.isEmpty()) {
                    client.sendMessage(chatId, "Chưa có buổi nào $con báo soạn xong cả.")
                } else {
                    prefs.boDanhDau(ma)
                    client.sendMessage(
                        chatId,
                        "Đã bật lại lời nhắc. Lần tới $con mở máy là nó hiện ra, " +
                            "phải tích lại từng môn và chụp lại cặp."
                    )
                }
            }

            "lichmai" -> {
                val ke = TinhLoiNhac.buoiKeTiep(Calendar.getInstance())
                if (ke == null) {
                    client.sendMessage(chatId, "Không tìm ra buổi học nào sắp tới.")
                } else {
                    val (cal, buoi) = ke
                    val nghi = NgayNghi.tenKyNghi(cal)
                    val mon = buoi.monTheoTiet.entries.sortedBy { it.key }
                        .joinToString("\n") { "  tiết ${it.key}: ${it.value}" }
                    val canSoan = buoi.monCanSoan
                    client.sendMessage(
                        chatId,
                        buildString {
                            append("Buổi kế tiếp: ").append(TinhLoiNhac.moTaBuoi(buoi))
                            append(", vào học ")
                            append(TinhLoiNhac.gioPhut(buoi.phutVaoHoc)).append("\n\n")
                            append(mon).append("\n\n")
                            if (canSoan.isEmpty()) {
                                append("Không phải mang vở môn nào.")
                            } else {
                                append("Cần mang: ").append(canSoan.joinToString(", "))
                            }
                            if (buoi.coTheDuc) append("\nCó thể dục, nhớ mặc đồ.")
                            if (nghi != null) append("\n\nLưu ý: hôm đó là ").append(nghi)
                        }
                    )
                }
            }

            // Tin co giao chia se tu nhom lop Zalo. Phai co lenh chu khong the go
            // chu thuong nhu ben app Soan tap: o day go chu thuong da co nghia roi,
            // do la nhan tin cho Le Hoa.
            "tinco" -> {
                if (arg.isEmpty()) {
                    client.sendMessage(
                        chatId,
                        "Gõ /tinco kèm nội dung cô nhắn, ví dụ:\n" +
                            "/tinco Mai lớp kiểm tra 15 phút môn Toán."
                    )
                } else {
                    khoTin.them(arg)
                    baoCoTinCuaCo()
                    client.sendMessage(chatId, "Đã đưa lên tablet rồi.")
                }
            }

            "trogiup", "lenh", "giupdo", "help", "start" -> client.sendMessage(chatId, bangLenh())

            else -> if (text.startsWith("/")) {
                client.sendMessage(chatId, "Không có lệnh đó. Gõ /trogiup để xem danh sách.")
            } else if (text.isNotEmpty()) {
                // Ba go chu thuong, khong phai lenh: day la cau tra loi cho con.
                nhanTinChoCon(client, chatId, text)
            }
        }
    }

    /**
     * Ba nhan cho con. Hien thanh thong bao co tieng, vi con co the dang o app khac
     * hoac dang de may xuong ban, ma tin cua ba thuong la tra loi cho mot viec gap.
     */
    private fun nhanTinChoCon(client: TelegramClient, chatId: Long, text: String) {
        ChatBox.add(this, ChatFrom.BA, text)
        ChatBox.stopWaiting(this)
        DongBo.dayTin(this, ChatLine(ChatFrom.BA, text, System.currentTimeMillis()))
        ChuongTin.keu(this, text)
        client.sendMessage(
            chatId,
            "Đã chuyển cho ${getString(R.string.child_name)}."
        )
    }

    /**
     * Bao co tin cua co giao bang thong bao Android thuong.
     *
     * Khong dung the noi nhu loi nhac soan vo: tin cua co can doc ky va can xem
     * lai, ma the noi thi troi qua roi mat. Thong bao thi nam trong khay cho toi
     * khi co nguoi bam vao.
     */
    private fun baoCoTinCuaCo() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(TIN_CO_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    TIN_CO_CHANNEL_ID,
                    "Tin của cô giáo",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Báo khi Ba Huy chuyển tin của cô từ nhóm lớp sang"
                }
            )
        }
        val chuaDoc = khoTin.soTinChuaDoc()
        val open = PendingIntent.getActivity(
            this,
            2,
            Intent(this, vn.huytl.homeworkgate.ui.TinActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            TIN_CO_NOTIFICATION_ID,
            NotificationCompat.Builder(this, TIN_CO_CHANNEL_ID)
                .setSmallIcon(R.drawable.st_ic_cap_sach)
                .setContentTitle("Cô giáo nhắn tin")
                .setContentText(if (chuaDoc > 1) "$chuaDoc tin chưa đọc" else "Bấm để đọc")
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()
        )
    }

    /**
     * Cham bai vua nop bang AI, roi tu duyet neu duoc.
     *
     * Luat cua Ba Huy: cau nao dung thi cong gio cau do ngay, cau nao sai thi bao
     * de con sua roi nop lai. Nen o day KHONG doi "dung het moi duyet" - cham xong
     * la cap luon phan da dung.
     *
     * Hai truong hop khong tu duyet:
     *  - AI bao co cho no doc khong ro: cho Ba Huy nhin, vi doan mo la de duyet nham
     *    bai sai thanh bai dung;
     *  - khong cham duoc (het han muc, mat mang, Google doi model): bao ro ly do de
     *    Ba Huy duyet tay. Anh va hai nut Duyet/Khong duyet van nam nguyen ben
     *    Telegram nhu truoc gio, nen hong AI khong lam ket bai cua con.
     */
    /** Ke ma cau cho con doc: "câu 2.26a, 2.26b", dai qua thi cat bot. */
    private fun keTenCau(cac: List<vn.huytl.homeworkgate.kho.CauHoi>): String = when {
        cac.isEmpty() -> ""
        cac.size <= 3 -> "câu " + cac.joinToString(", ") { it.ma }
        else -> "câu " + cac.take(3).joinToString(", ") { it.ma } + " và ${cac.size - 3} câu nữa"
    }

    /**
     * Con da soat xong ban may doc: gui anh sang Telegram roi cap gio.
     *
     * Thu tu quan trong. GUI ANH TRUOC, cap gio sau: gui anh la viec co the hong vi
     * mang, ma hong thi phai bao con biet de chup lai - con neu cap gio truoc roi
     * gui hong thi con duoc gio ma Ba Huy khong thay bai dau ca.
     *
     * [ket] null nghia la man soat khong co ban cham nao de dua sang (AI hong luc do).
     * Van gui anh: Ba Huy con hai nut Duyet/Khong duyet ben Telegram nhu tu truoc
     * den gio, nen hong AI khong lam ket bai cua con.
     */
    private fun guiRoiCap(
        nhom: Map<CaptureStage, List<java.io.File>>,
        pham: PhamVi?,
        ket: KetQuaCham?
    ) {
        val anh = nhom.values.flatten()
        val sent = runCatching { HomeworkSender.send(this, nhom) }.getOrElse { e ->
            Log.w(TAG, "gui bai hong: ${e.message}")
            SoCaiBai.datLoiNhan(this, "Gửi không được, kiểm tra mạng rồi chụp lại nhé.")
            anh.forEach { runCatching { it.delete() } }
            return
        }

        gate.markPending(sent.requestId, sent.messageId)
        DongBo.dayBaiMoi(this, sent.requestId, sent.messageId, sent.anh)
        DayLog.add(
            this,
            "Nộp bài: " + nhom.entries.joinToString(", ") { (st, files) ->
                "${getString(st.labelRes).lowercase()} ${files.size}"
            }
        )
        anh.forEach { runCatching { it.delete() } }

        if (ket == null) {
            runCatching {
                tg.sendMessage(prefs.parentChatId, "⚠️ Máy không chấm được lần này. Ba Huy duyệt tay giúp nhé.")
            }
            return
        }
        xuLyBanCham(ket, pham)
    }


    /**
     * Xu ly mot ban cham da co: cap gio, ghi so, bao Telegram, day sang dien thoai.
     *
     * Tach khoi phan goi AI vi tu ban co man soat bai, HAI VIEC NAY XAY RA O HAI NOI:
     * cham thi trong man hinh cua con (con phai ngoi xem may doc co dung khong), con
     * cap gio thi phai o service, vi no van phai chay xong du con dong man hinh lai
     * hay tablet khoa man.
     */
    private fun xuLyBanCham(ket: KetQuaCham, pham: PhamVi?) {
        val chatId = prefs.parentChatId
        val con = getString(R.string.child_name)
        val bai = gate.baiDangCho().lastOrNull()

        // Bo cac cau da tra gio tu lan nop truoc: chup lai bai cu khong duoc tinh
        // lan hai. Cau dang cho sua thi KHONG bo - lan nay con sua no.
        //
        // Hoi bang chinh cau vua cham chu khong bang de bai: cau nao con da khai thi
        // mang san ma sach, va ma sach thi hai lan nop deu nhu nhau. De bai chi con
        // dung cho bai ngoai sach.
        val moi = ket.cac.filter { !SoCaiBai.daTraGioCua(this, it) }
        val trung = ket.cac.size - moi.size

        /*
         * Con khai lam ma trong anh khong thay: noi ra, dung im lang bo qua.
         *
         * Khai ca bai muoi cau roi chup hai cau la canh de xay ra nhat khi man khai
         * bai moi ve - va neu may lang le bo chin cau kia thi con chi thay "duoc 4
         * phut" ma khong hieu tai sao it the. Noi ro thi lan sau no khai dung.
         */
        val thieu = if (pham != null && pham.theoSach) {
            val daCham = ket.cac.mapNotNull { it.cauId }.toSet()
            vn.huytl.homeworkgate.kho.KhoBai.get(this)
                .cacCauTheoId(pham.cauIds.filter { it !in daCham })
        } else {
            emptyList()
        }

        val goiDaCo = SoCaiBai.goiDaCoHomNay(this)
        val bang = LuatCongGio.tinh(
            ket.copy(cac = moi),
            daCongLamThemHomNay = SoCaiBai.phutLamThemHomNay(this),
            goiDaCoHomNay = goiDaCo
        )
        /*
         * Cau may khong nhin thay de: tach han ra.
         *
         * KHONG cho vao danh sach "can sua": con lam roi, chi la may khong co gi de
         * doi chieu. Bao con "sua lai" la bao sai, va man hinh cua con se day nhung
         * cau khong co gi de sua ca. Cung khong ghi vao so: khong co ban an nao het,
         * mai con chup kem trang de la cham lai binh thuong.
         */
        val khongCoDe = moi.filter { !it.coDe }
        val coDe = moi.filter { it.coDe }
        val sai = coDe.filter { !it.dung }
        val dung = coDe.count { it.dung }
        val dauDe = if (ket.mon.isBlank()) "" else "${ket.mon} · "
        val than = StringBuilder(
            if (moi.isEmpty() && trung > 0) {
                "🤖 AI chấm: ${dauDe}cả $trung câu đều đã tính giờ hôm trước"
            } else {
                "🤖 AI chấm: $dauDe$dung/${moi.size} câu đúng" +
                    if (trung > 0) " ($trung câu đã tính giờ hôm trước, bỏ qua)" else ""
            }
        )
        than.append("\n")
        if (pham != null && pham.bai.isNotBlank()) {
            than.append("• Con khai: ").append(pham.tenNguon.ifBlank { pham.mon })
                .append(" — ").append(pham.bai).append("\n")
        }
        bang.dong.forEach { than.append("• ").append(it).append("\n") }
        if (thieu.isNotEmpty()) {
            than.append("• Khai làm nhưng ảnh không thấy: ")
                .append(thieu.joinToString(", ") { it.ma }).append("\n")
        }
        if (khongCoDe.isNotEmpty()) {
            than.append("• Không có đề nên không chấm được: ")
                .append(khongCoDe.take(6).joinToString(", ") { it.ma })
                .append(if (khongCoDe.size > 6) " và ${khongCoDe.size - 6} câu nữa" else "")
                .append(" — ảnh chỉ có đáp án\n")
        }

        // Cap gio TRUOC, roi moi ghi so. Khong the ghi "cau nay da tra 2 phut" khi
        // chua biet co tra duoc hay khong: qua gio nghi, hay AI doc khong ro, la
        // khong cap gi ca - ma so van tru mat cua con hai phut do va lan sau nop lai
        // khong duoc tinh nua.
        var daCap = false
        // Ba Huy bam Duyet ben Telegram trong luc AI dang cham: bai da bien khoi hang
        // cho. Tu duyet them lan nua la cong gio hai lan cho cung mot bai.
        val baDaXuLy = bai != null && gate.baiDangCho().none { it.id == bai.id }
        val tuDuyet = bang.phut > 0 && !bang.canBaHuyXem && !baDaXuLy
        if (tuDuyet) {
            val phut = capGioTuAi(bang.phut, bai)
            if (phut == null) {
                than.append("Không cấp được (đang giờ ngủ). Ba Huy xem giúp nhé.")
            } else {
                daCap = true
                DayLog.add(this, "AI duyệt ${bang.phut} phút")
                than.append("Đã cấp ${bang.phut} phút. Rút lại: /bot ${bang.phut}")
            }
        } else if (baDaXuLy) {
            than.append("Ba Huy đã xử bài này trước khi máy chấm xong nên không cộng thêm.")
        } else if (bang.canBaHuyXem) {
            than.append("Chưa cấp giờ: có chỗ máy đọc không rõ. Ba Huy xem ảnh rồi duyệt giúp.")
        } else if (bang.trongGoi.isNotEmpty()) {
            than.append("Không cộng thêm phút: phần này nằm trong trọn gói bài cô giao đã tính hôm nay.")
        } else {
            than.append("Chưa cấp giờ. $con sửa lại rồi nộp tiếp.")
        }
        if (sai.isNotEmpty()) {
            than.append("\nCần sửa: ").append(sai.joinToString(", ") { it.ma })
        }

        // Ghi so: cau sai luon ghi (de con con duong sua); cau dung chi ghi la xong
        // khi gio da vao tay con that. Khong cap duoc thi mai nop lai van duoc tinh.
        //
        // Cau nam trong tron goi da tra thi cung la XONG, du lan nay khong cong phut
        // nao: gio cua no da nam trong 45 phut kia. Khong ghi thi man hinh con cu bao
        // "can sua" mai du con da sua dung, va moi lan nop lai la mot lan cham lai.
        val xongTheoGoi = if (!bang.canBaHuyXem && !baDaXuLy) bang.trongGoi else emptyList()
        val ghiVaoSo = (if (daCap) moi else (xongTheoGoi + sai)).filter { it.coDe }
        SoCaiBai.ghi(this, ghiVaoSo, if (daCap) bang.phutCua else emptyMap())
        if (daCap && bang.daTinhGoi) SoCaiBai.ghiGoi(this, LuatCongGio.PHUT_TRON_GOI_DAN_DO)

        // Mot dong cho man hinh cua con, duoi ten Ba Huy. Khong co dong nay thi con
        // nop bai, cho mot luc, roi khong thay gi doi ca - nhat la khi nop lai bai
        // da cham hom truoc: khong cong gio, ma cung khong co cau nao de sua.
        SoCaiBai.datLoiNhan(
            this,
            when {
                bang.canBaHuyXem ->
                    "${getString(R.string.parent_name)} đang xem lại bài, chờ chút nhé."
                bang.phut > 0 && sai.isEmpty() && thieu.isEmpty() ->
                    "Bài tốt! Được thêm ${bang.phut} phút."
                // Con khai mot loat cau roi chi chup duoc vai cau: phai noi ra so cau
                // con thieu, khong thi no chi thay so phut it hon minh tuong.
                bang.phut > 0 && sai.isEmpty() ->
                    "Được thêm ${bang.phut} phút. Còn ${keTenCau(thieu)} thì chưa thấy " +
                        "bài làm trong ảnh."
                bang.phut > 0 -> "Được thêm ${bang.phut} phút. Còn ${sai.size} câu sửa lại nhé."
                moi.isEmpty() && trung > 0 ->
                    "Mấy bài này chấm hôm trước rồi, làm bài mới thì mới được cộng giờ nhé."
                // Ca xap chi co dap an: noi thang cho con biet phai chup them cai gi,
                // dung de no ngoi doan vi sao nop ma khong duoc gi.
                coDe.isEmpty() && khongCoDe.isNotEmpty() ->
                    "Ảnh chỉ có đáp án, không có đề bài nên máy không chấm được. " +
                        "Con chụp thêm trang đề giúp nhé."
                khongCoDe.isNotEmpty() ->
                    "Có ${khongCoDe.size} câu không thấy đề bài. Con chụp thêm trang đề nhé."
                thieu.isNotEmpty() && sai.isEmpty() ->
                    "Chưa thấy bài làm của ${keTenCau(thieu)} trong ảnh. Chụp lại cho rõ nhé."
                sai.isEmpty() -> "Chưa cộng giờ được cho bài này."
                else -> "Sửa lại ${sai.size} câu rồi chụp gửi nhé."
            }
        )
        Log.i(TAG, "cham bai: cap ${bang.phut} phut, ${sai.size} cau can sua")

        // Ban cham sang app Bang dieu khien.
        //
        // Day ca ban cham chu khong chi mot dong ket luan: ben dien thoai Ba Huy
        // liec bang nay la quyet duoc, phan lon cac lan khoi phai mo anh ra.
        if (bai != null) {
            runCatching {
                DongBo.dayChamBai(
                    this, bai.id,
                    mapOf(
                        "mon" to ket.mon,
                        "tomTat" to than.toString(),
                        "phutDeNghi" to bang.phut,
                        "lamHetDanDo" to ket.lamHetDanDo,
                        "cac" to moi.map { c ->
                            mapOf(
                                "ma" to c.ma,
                                "de" to c.de,
                                "ketQua" to c.ketQua,
                                "dung" to c.dung,
                                "docRo" to c.docRo,
                                "soDong" to c.soDong,
                                "nhanXet" to c.nhanXet
                            )
                        }
                    )
                )
                if (daCap) DongBo.datTrangThaiBai(this, bai.id, "DUYET", bang.phut)
            }.onFailure { Log.w(TAG, "day ban cham khong duoc: ${it.message}") }
        }

        // Tra loi thang vao tin nop bai, de ket qua cham nam ngay duoi chum anh cua
        // con. Ba Huy mo Telegram len la thay ca hai cung mot cho: anh bai lam va
        // may cham cai gi - khong phai keo len keo xuong doi chieu.
        runCatching {
            tg.sendMessage(
                chatId,
                than.toString(),
                replyToMessageId = bai?.messageId ?: 0L
            )
        }.onFailure { Log.w(TAG, "gui ket qua cham khong duoc: ${it.message}") }
        // Da tu duyet thi go hai nut Duyet/Khong duyet duoi tin nop bai: viec xong
        // roi, de lai chi to nut chet. Chua cap duoc thi GIU NGUYEN - do la duong
        // Ba Huy duyet tay.
        if (daCap && bai != null && bai.messageId != 0L) {
            runCatching { tg.clearReplyMarkup(chatId, bai.messageId) }
        }
        refreshUi()
    }

    /**
     * Cap so phut AI da tinh. Dang choi thi cong thang vao phien.
     *
     * useQuota = true: gio kiem bang bai tap an vao tran phut moi ngay. Cham tran
     * thi [GateStore] tu cat bot cho vua chu khong tu choi ca lan duyet.
     */
    private fun capGioTuAi(phut: Int, bai: vn.huytl.homeworkgate.data.BaiCho?): Int? {
        if (gate.state == GateState.ACTIVE) {
            val con = gate.extend(phut, useQuota = true) ?: return null
            bai?.let { gate.boBaiCho(it.id) }
            return con
        }
        return gate.approve(wantedMinutes = phut, useQuota = true, requestId = bai?.id)
    }

    /** Cap gio va tra loi ve chat. Dung chung cho /duyet va /cho. */
    private fun capGio(
        client: TelegramClient,
        chatId: Long,
        minutes: Int?,
        useQuota: Boolean,
        why: String,
        bai: vn.huytl.homeworkgate.data.BaiCho? = null,
        /** Cau hien tren man hinh cua Le Hoa. Bo trong la Ba Huy duyet. */
        nhanCho: String? = null
    ) {
        val con = getString(R.string.child_name)

        // Dang choi ma ba cho them thi cong vao phien dang chay, chu khong cat
        // phien roi cap lai tu dau. "Cho them 30 phut" luc con con 10 phut nghia
        // la 40, khong phai 30.
        if (gate.state == GateState.ACTIVE) {
            val them = minutes ?: prefs.grantMinutes
            val left = gate.extend(them)
            // Duyet mot bai trong luc con dang choi: cong vao phien va go bai do ra
            // khoi hang cho, khong thi no nam do cho den khi qua ngay.
            bai?.let {
                gate.boBaiCho(it.id)
                DongBo.datTrangThaiBai(this, it.id, "DUYET", them)
            }
            if (bai != null && bai.messageId != 0L) client.clearReplyMarkup(chatId, bai.messageId)
            DayLog.add(this, "Ba Huy cho thêm $them phút giữa phiên")
            client.sendMessage(
                chatId,
                "$con đang chơi nên cộng thẳng $them phút vào phiên. " +
                    "Còn ${left ?: 0} phút.${conChoBaoNhieu()}"
            )
            refreshUi()
            return
        }

        val messageId = bai?.messageId ?: 0L
        val xin = minutes ?: prefs.grantMinutes
        val granted = gate.approve(
            wantedMinutes = minutes,
            useQuota = useQuota,
            requestId = bai?.id,
            nhanCho = nhanCho
        )
        if (granted == null) {
            client.sendMessage(
                chatId,
                "Không cấp được: đang trong giờ ngủ ${gioChot()}, hoặc hôm nay đã đủ " +
                    "${prefs.tranPhutMoiNgay} phút. Gõ /cho để cho thêm ngoài định mức."
            )
            return
        }
        if (messageId != 0L) client.clearReplyMarkup(chatId, messageId)
        bai?.let { DongBo.datTrangThaiBai(this, it.id, "DUYET", granted) }
        DayLog.add(this, "Duyệt $granted phút ($why)")
        ensureRunning(this)
        // Con dang giu phieu cu ma nop them bai thi duyet la cong don, khong de len.
        // Noi ro ca hai so, khong thi nhin "da duyet 120 phut" ba lai tuong minh vua
        // go nham.
        val dong = if (granted > xin) {
            "Đã duyệt thêm $xin phút, cộng dồn thành $granted phút."
        } else {
            "Đã duyệt $granted phút (chưa tính giờ)."
        }
        client.sendMessage(
            chatId,
            "$dong Hôm nay còn ${gate.phutConLaiHomNay()} phút.${conChoBaoNhieu()}"
        )
        refreshUi()
    }

    /** Doi chu nhac con may bai nua dang xep hang, hoac chuoi rong neu het. */
    private fun conChoBaoNhieu(): String {
        val con = gate.soBaiDangCho()
        return if (con > 0) " Còn $con bài đang chờ duyệt." else ""
    }

    /** Khung gio ngu, dang "22:00-06:00". */
    private fun gioChot(): String {
        val tu = prefs.hardStopMinuteOfDay
        val den = prefs.gioDayMinuteOfDay
        return "%02d:%02d-%02d:%02d".format(tu / 60, tu % 60, den / 60, den % 60)
    }

    /**
     * Bang lenh hien khi go /trogiup.
     *
     * Moi lenh mot dong, moi dong may chu. Truoc day bang nay co them cau giai
     * thich cach tinh gio va doan noi ve nhan tin, dung nhung dai: mo ra la mot
     * man chu, phai doc moi tim ra dong minh can.
     */
    private fun bangLenh(): String = """
        GIỜ CHƠI
        /duyet 60  duyệt bài chờ cũ nhất
        /tuchoi  không duyệt bài cũ nhất
        /cho 30  cho chơi, đang chơi thì cộng thêm
        /bot 15  bớt giờ
        /dung  tạm dừng, giữ giờ lại
        /tiep  chơi tiếp
        /khoa  khoá ngay, mất giờ còn lại

        XEM
        /trangthai  còn mấy phút, đã duyệt bao nhiêu
        /nhatky  hôm nay có gì
        /thongke  dùng app gì, mấy giờ (/thongke 1 = hôm qua)
        /hoi  con hỏi AI những gì
        /ai  danh sách app AI đang ghi (/ai <gói> thêm, /aibo bỏ)

        SOẠN CẶP
        /lichmai  buổi học kế tiếp có môn gì
        /soanlai  bắt soạn lại vì soạn thiếu
        /mo  mở màn chặn cho hết buổi học này
        /chan  chặn lại ngay
        /tatchan  tắt hẳn màn chặn, chỉ còn nhắc
        /batchan  bật lại màn chặn
        /tinco ...  đưa tin của cô lên tablet

        PHÒNG HỜ
        /nhom  lấy chỗ đang gõ làm hộp thư cho máy bà nội
        /bo  mở toàn bộ máy
        /choxoa  tắt quản trị để gỡ app
        /xoapin  xoá PIN, đặt lại trên tablet
        /trogiup  bảng này

        Gõ chữ thường = nhắn cho Lê Hòa.
        Số phút, giờ ngủ, danh sách app: sửa trong app.
    """.trimIndent()

    /** Thong bao dem nguoc ve lai dung trang thai sau moi lenh. */
    private fun refreshUi() = runCatching { refreshNotification() }.let {}

    /** Cap nhat thong bao dem nguoc va tu tat khi het phien. */
    private suspend fun uiLoop() {
        while (scope?.isActive == true) {
            withContext(Dispatchers.Main) { refreshNotification() }
            capNhatDongHo()
            if (!canGiuKetNoi(this)) {
                stopSelf()
                return
            }
            delay(if (dangNgungNghe()) NHIP_NGUNG_NGHE_MS else 30_000L)
        }
    }

    /**
     * Sua tin ghim mot lan moi phut trong luc con dang choi.
     *
     * Di ke vong 30 giay da co san nen khong them lan danh thuc nao; phan them chi
     * la mot request nho tren ket noi da mo. Ngoai phien choi thi ca service nay
     * khong chay, nen no khong ton gi.
     */
    private fun capNhatDongHo() {
        val moTa = moTaChoTinGhim()
        val doiTrangThai = moTa != tinGhimTruoc
        val now = SystemClock.elapsedRealtime()

        // Dang choi thi sua moi phut cho so phut chay that. Ngoai phien choi chi sua
        // khi cau chu doi - truoc day service tat han luc do nen khong can nghi den,
        // gio no chay ca ngay, ma sua tin ghim moi 30 giay trong khi may dang khoa
        // thi vua ton mang vua khong them tin gi.
        if (!doiTrangThai &&
            (gate.state != GateState.ACTIVE || now - lanSuaTinGhim < 60_000L)
        ) return

        lanSuaTinGhim = now
        tinGhimTruoc = moTa
        runCatching { Notifier.ghimTin(this, tg, moTa) }
    }

    /** Mot dong duy nhat mo ta tablet dang the nao, cho tin ghim. */
    private fun moTaChoTinGhim(): String {
        val con = getString(R.string.child_name)
        val dong = when {
            ParentMode.isActive(this) -> "Đang mở toàn bộ máy — ${ParentMode.moTa(this)}"
            gate.state == GateState.ACTIVE -> {
                val conLai = gate.remainingMs() / 60_000 + 1
                val hetLuc = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
                    .format(Date(System.currentTimeMillis() + gate.remainingMs()))
                "$con đang chơi — còn $conLai phút, hết lúc $hetLuc"
            }
            gate.state == GateState.PAUSED ->
                "$con tạm dừng — giữ lại ${gate.pausedMinutes()} phút"
            gate.state == GateState.GRANTED ->
                "Đã duyệt ${gate.grantedMinutes} phút — $con chưa bấm chơi"
            gate.state == GateState.PENDING ->
                "$con đã nộp ${gate.soBaiDangCho()} bài, đang chờ duyệt"
            dangNgungNghe() -> "Tablet đang khoá — đêm khuya nên ${NHIP_NGUNG_NGHE_MS / 60_000} phút " +
                "mới nghe Telegram một lần"
            else -> "Tablet đang khoá"
        }
        val thieu = Permissions.missing(this)
        val canhBao = if (thieu.isEmpty()) "" else
            "\n⚠️ " + thieu.joinToString("; ") { it.ten }
        return "$dong$canhBao"
    }

    private fun refreshNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val text = when {
            ParentMode.isActive(this) ->
                "Đang mở toàn bộ máy — ${ParentMode.moTa(this)}"
            gate.state == GateState.PENDING ->
                "Đã gửi ${gate.soBaiDangCho()} bài, đang chờ Ba Huy duyệt"
            gate.state == GateState.GRANTED ->
                "Ba Huy duyệt ${gate.grantedMinutes} phút, bấm Bắt đầu để chơi"
            gate.state == GateState.PAUSED ->
                "Đang tạm dừng, giữ lại ${gate.pausedMinutes()} phút"
            gate.state == GateState.ACTIVE -> {
                val left = gate.remainingMs()
                if (left > 0) "Còn ${left / 60_000 + 1} phút" else "Hết giờ"
            }
            else -> "Đang khoá"
        }

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_gate)
            .setContentTitle("Nộp bài")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Trạng thái giờ chơi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị thời gian còn lại và trạng thái chờ duyệt"
                setShowBadge(false)
            }
        )
    }

    companion object {
        private const val TAG = "HomeworkGate"
        private const val CHANNEL_ID = "gate_status"
        private const val NOTIFICATION_ID = 1001
        private const val TIN_CO_CHANNEL_ID = "tin_cua_co"
        private const val TIN_CO_NOTIFICATION_ID = 1003

        /**
         * Lenh cu hon chung nay thi khong chay nua.
         *
         * Nua tieng: du dai de om ca nhip muoi phut ben duoi va nhung luc
         * tablet mat mang, du ngan de khong co chuyen lenh cua toi hom truoc chay
         * vao sang hom sau.
         */
        private const val LENH_QUA_CU_MS = 30 * 60_000L

        /** Khung gio thoi nam cho Telegram, tinh bang phut trong ngay. */
        private const val NGUNG_TU = 23 * 60
        private const val NGUNG_DEN = 5 * 60

        /**
         * Trong gio thoi nghe thi cach nay lau moi ghe hoi Telegram mot lan.
         *
         * Phai nho hon [LENH_QUA_CU_MS] mot khoang rong: lenh go luc nua dem nam
         * cho toi muoi phut moi duoc doc, ma doc xong lai bao "lenh cu qua" thi
         * thanh ra khong bao gio chay.
         */
        private const val NHIP_NGUNG_NGHE_MS = 10 * 60_000L

        /**
         * Bao lau ghe hop thu cua ba noi mot lan.
         *
         * Mot phut la do tre ba phai chiu tu luc bam den luc tablet mo gio. Khong
         * ai thay do tre do: con con phai cam may len bam Bat dau thi dong ho moi
         * chay. Ha xuong nua chi ton them request chu khong nhanh hon duoc bao nhieu.
         */
        private const val NHIP_NGO_HOP_THU_MS = 60_000L

        /**
         * Nhip xet loi nhac soan tap luc binh thuong.
         *
         * Mot phut: moc som nhat trong thoi khoa bieu la 6:45, va cham mot phut so
         * voi moc do thi khong ai thay. Rieng luc dang dem nguoc hay dang chan thi
         * vong tu ha xuong mot giay.
         */
        private const val NHIP_NHAC_MS = 60_000L

        /**
         * Co giu duong day voi Telegram khong. Cai dat xong la giu, khong tat nua.
         *
         * Truoc day cho nay tra false khi may dang khoa, voi y la tiet kiem pin.
         * Nhung may khoa la trang thai gan nhu ca ngay, va do dung la luc Ba Huy hay
         * go lenh nhat: go /cho 1 thi khong ai nghe, khong ai tra loi, tuong app
         * hong. Te hon nua, Telegram giu lenh do lai 24 tieng roi doc cho service o
         * lan mo sau - nghia la mot lenh go toi hom truoc co the mo gio choi vao
         * sang hom sau.
         *
         * Cai gia phai tra la mot thong bao thuong truc va mot ket noi long-poll
         * nam khong. Ket noi do 25 giay moi hoi mot lan va khong giu CPU, nen gan
         * nhu khong ton pin - voi dieu kien app da duoc bo ra khoi tiet kiem pin,
         * vi vay bang canh bao moi bat cai do len dau.
         */
        private fun canGiuKetNoi(context: Context): Boolean =
            Prefs.get(context).isConfigured

        /** Con da soat xong ban may doc, nho service gui anh roi cap gio. */
        const val ACTION_GUI = "vn.huytl.homeworkgate.GUI_BAI"

        /** Ban cham con vua soat, dang chu cua [ChamBaiIO]. */
        private const val EXTRA_BAN_CHAM = "ban_cham"

        /** Duong dan anh, moi buoc chup mot mang: EXTRA_ANH + ten buoc. */
        private const val EXTRA_ANH = "anh"

        /** Bai con da khai truoc khi chup, dang JSON cua [PhamVi]. */
        private const val EXTRA_PHAM = "pham_vi"

        /**
         * Gui mot lan nop con da soat xong.
         *
         * Lam o service chu khong o man hinh vi hai viec nay deu phai chay cho xong
         * du con dong man hinh lai ngay sau khi bam gui: tai anh len Telegram mat vai
         * giay, va cap gio thi khong duoc phep lam nua chung.
         *
         * Anh chia theo tung buoc chup, vi tin nhan Telegram van phai co nhan
         * "vo dan do / de bai / bai giai" nhu tu truoc den gio.
         *
         * [ket] null la may khong cham duoc lan nay - van gui anh, Ba Huy duyet tay.
         */
        fun guiDaSoat(
            context: Context,
            nhom: Map<CaptureStage, List<java.io.File>>,
            pham: PhamVi?,
            ket: KetQuaCham?
        ) {
            val intent = Intent(context, ApprovalService::class.java)
                .setAction(ACTION_GUI)
                .putExtra(EXTRA_PHAM, pham?.sangJson())
                .putExtra(EXTRA_BAN_CHAM, ket?.let { ChamBaiIO.viet(it) })
            nhom.forEach { (st, files) ->
                intent.putStringArrayListExtra(
                    EXTRA_ANH + st.name, ArrayList(files.map { it.absolutePath })
                )
            }
            context.startForegroundService(intent)
        }

        /** Bat service neu dang can, khong thi thoi. */
        fun ensureRunning(context: Context) {
            val st = GateStore(context).state
            Log.i(TAG, "ensureRunning: state=$st canGiu=${canGiuKetNoi(context)}")
            if (!canGiuKetNoi(context)) return
            val intent = Intent(context, ApprovalService::class.java)
            context.startForegroundService(intent)
        }
    }
}
