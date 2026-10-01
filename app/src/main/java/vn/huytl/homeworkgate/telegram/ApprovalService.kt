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
import android.os.PowerManager
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
import vn.huytl.homeworkgate.data.BaiGuiHong
import vn.huytl.homeworkgate.data.CauChuaRo
import vn.huytl.homeworkgate.data.CongSang
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.KhaiChoCham
import vn.huytl.homeworkgate.data.NhatKyAi
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.data.NhacBai
import vn.huytl.homeworkgate.data.LoaiLoi
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.LoaiNhac
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.ai.ChamBaiIO
import vn.huytl.homeworkgate.data.CaptureStage
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.guard.DaiNhac
import vn.huytl.homeworkgate.guard.ManChan
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.guard.ChuongTin
import vn.huytl.homeworkgate.guard.GuardAccessibilityService
import vn.huytl.homeworkgate.guard.TelegramThat
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.Permissions
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

    /** Nhip nghe cua vong truoc, chi de ghi log mot dong khi no doi. */
    private var nhipTruoc = 0L

    /** Lan gan nhat da sua tin ghim, de sua dung mot lan moi phut. */
    private var lanSuaTinGhim = 0L

    /** Cau da ghim lan truoc, de khong sua lai khi khong co gi doi. */
    private var tinGhimTruoc = ""

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
                // Man hinh sang lai cung la luc tra vong poll ve nhip nhanh: tu
                // gio tro di con dang nhin man hinh, nen ket qua duyet phai toi
                // trong vai giay chu khong cho het nhip thua dang do.
                Intent.ACTION_USER_PRESENT -> {
                    dai.batLaiChuKy()
                    xetLaiNgay()
                    danhThucPoll()
                }
                Intent.ACTION_SCREEN_ON -> {
                    xetLaiNgay()
                    danhThucPoll()
                }
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
        runCatching { vn.huytl.homeworkgate.guard.MocGio.datLai(this) }
        runCatching { DongBo.batDau(this) }
        createChannel()
        VoDanDo.donDep(this)

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
            s.launch { khaiBaoMenuLenh() }
            batNhipNhac()
        } else {
            // Dang ghe hoi thua ma co viec that: con vua nop bai, hay Ba Huy mo may,
            // hay vua het gio dem. Danh thuc vong long-poll day chu khong de no nam
            // not nhip dang do - luc do bam Duyet ben Telegram cung phai cho.
            danhThucPoll()
        }

        // Vua co viec nha moi: che man hinh ngay, khong doi het nhip mot phut cua vong
        // xet. Man hinh dang tat thi thoi, bat man hinh len la tu xet lai; chay vong luc
        // man tat thi nhip mot giay cua man chan an pin ca buoi.
        if (intent?.action == ACTION_XET_LAI && manHinhSang()) {
            xetLaiNgay()
        }

        if (intent?.action == ACTION_GUI) {
            val nhom = CaptureStage.entries.associateWith { st ->
                intent.getStringArrayListExtra(EXTRA_ANH + st.name).orEmpty()
                    .map { java.io.File(it) }
            }.filterValues { it.isNotEmpty() }
            val pham = PhamVi.tuJson(intent.getStringExtra(EXTRA_PHAM))
            val lucNop = intent.getLongExtra(EXTRA_LUC_NOP, 0L).takeIf { it > 0L }
                ?: System.currentTimeMillis()
            val tep = nhom.values.flatten()
            scope?.let { s ->
                BaiGuiHong.nhanGui(tep)
                s.launch {
                    try {
                        synchronized(khoaGui) { guiRoiCho(nhom, pham, lucNop) }
                    } finally {
                        BaiGuiHong.traGui(this@ApprovalService, tep)
                    }
                }
            }
        }

        // Ba Huy dan ket qua Claude cham tu Bang dieu khien. Chay dung doan xu ly cua
        // ban cham AI, chi khac la bai da duoc chi dinh san. Xem [chamTheoClaude].
        if (intent?.action == ACTION_CHAM_CLAUDE) {
            val baiId = intent.getStringExtra(EXTRA_BAI_ID).orEmpty()
            val pham = PhamVi.tuJson(intent.getStringExtra(EXTRA_PHAM))
            val ket = ChamBaiIO.doc(intent.getStringExtra(EXTRA_BAN_CHAM))
            val chupLai = intent.getStringArrayListExtra(EXTRA_CHUP_LAI).orEmpty()
            if (baiId.isNotEmpty() && ket != null) {
                scope?.launch {
                    synchronized(khoaCham) {
                        xuLyBanCham(ket, pham, baiId = baiId, nguoiCham = "Claude", chupLai = chupLai)
                    }
                }
            }
        }

        return START_STICKY
    }

    /**
     * Cat nhip dang cho de vong long-poll hoi lai ngay.
     *
     * Chi cat khi nhip MOI ngan hon nhip dang chay. Cat vo co thi moi lan bat man
     * hinh la mot lan huy va tao lai coroutine, ma tu no khong doi duoc gi.
     */
    private fun danhThucPoll() {
        if (nhipTruoc > 0L && nhipNgheMs() < nhipTruoc) {
            pollJob?.cancel()
            pollJob = scope?.launch { pollLoop() }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(nhanSuKienManHinh) }
        tatNhipNhac()
        dai.an()
        chan.an()
        pollJob?.cancel()
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

    /** Man chan dang nhuong cho mot app dung moi luc. Xem [nhuongAppMoiLuc]. */
    private var dangNhuongMoiLuc = false

    /**
     * Man chan co nen nhuong cho app dung moi luc luc nay khong. Xem [Prefs.moiLucPackages].
     *
     * KHO O CHO: man chan che kin thi Android khong dua cua so ben duoi vao danh sach cua
     * so cua dich vu tro nang, cung khong gui su kien cua no. Nen dich vu chi thay app
     * dung moi luc neu app do mo ra luc man chan dang tam an, tuc la mo tu app Nop bai. Da
     * nhuong roi thi phai giu cho dung luc: che lai mot lan la khong con cach nao biet con
     * van dang o trong Telegram.
     *
     * Bat dau nhuong khi app vua ra truoc mat ([GuardAccessibilityService.goiVuaMo]) la
     * app dung moi luc, hay khi vua bam "Nhan cho ba Huy" chua qua [CHO_APP_KE_MS]: Nop
     * bai tam dung truoc roi Telegram moi hien, mo lanh mat mot hai giay, ma man chan hien
     * vao khoang do thi Telegram nam ben duoi va khong ai thay no.
     *
     * Dang nhuong thi giu qua luc tat man hinh, man khoa, keo thanh thong bao, hop xin quyen
     * - nhung goi trong [GuardAccessibilityService.ALWAYS_ALLOWED] - vi khong luc nao trong
     * do la con da roi Telegram. Sang app khac, ve man hinh chinh, hay chia doi man hinh voi
     * mot app khong phai dung moi luc thi che lai.
     *
     * Dich vu tro nang khong chay thi khong nhuong: hai gia tri doc tu no luc do la cua lan
     * cuoi no con song.
     */
    private fun nhuongAppMoiLuc(): Boolean {
        val moiLuc = prefs.moiLucPackages
        if (moiLuc.isEmpty() || !GuardAccessibilityService.dangChay) {
            dangNhuongMoiLuc = false
            return false
        }
        val tm = GuardAccessibilityService.truocMat
        if (tm.sang && tm.cac.any { it.goi != packageName && it.goi !in moiLuc }) {
            dangNhuongMoiLuc = false
            return false
        }
        val goi = GuardAccessibilityService.goiVuaMo
        val vuaBamNut = SystemClock.elapsedRealtime() - TelegramThat.lucMoTuNut < CHO_APP_KE_MS
        dangNhuongMoiLuc = goi in moiLuc || vuaBamNut ||
            (dangNhuongMoiLuc && (!manHinhSang() || goi in GuardAccessibilityService.ALWAYS_ALLOWED ||
                goi in HOP_THOAI_CUA_APP))
        return dangNhuongMoiLuc
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
        // Luoi do cho VPN cat mang luc man hinh sang, phong khi co gi doi ma khong qua
        // prefs, vi du con vua cai lai YouTube.
        vn.huytl.homeworkgate.guard.CatMangVpn.dongBoThua(this)
        // Het gio ngu ma con giu phut cham trong gio ngu thi cong luon: bao thuc cua MocGio
        // co the tre, hay may tat ca dem. Xem CongSang.
        CongSang.congNeuDenLuc(this)?.let { CongSang.baoBaHuy(this, it) }
        if (ParentMode.isActive(this)) {
            withContext(Dispatchers.Main) { dai.an(); chan.an() }
            dangDemGiay = false
            return false
        }

        // Viec nha ba noi giao xet TRUOC moi thu khac. No khong theo gio nao ca va
        // chi het khi ba bam xong, nen khong the de mot loi nhac theo thoi khoa bieu
        // day no di.
        val nhac = nhacViecNha() ?: TinhLoiNhac.tinh(
            Calendar.getInstance(),
            prefs.buoiDaSoan,
            prefs.batManChan,
            prefs.buoiDuocMoSom,
            soBaiCho = { cal, buoi ->
                runCatching { NhacBai.choBuoi(this, cal, buoi).count { it.laBaiTap } }.getOrDefault(0)
            }
        )
        dangDemGiay = (nhac?.giayConLai ?: -1) >= 0
        val trongGioChan = nhac?.loai == LoaiNhac.CHAN

        withContext(Dispatchers.Main) {
            // Het man chan thi quen chuyen dang nhuong: lan man chan sau phai tu thay lai
            // con dang o dau, khong dua vao mot lan nhuong cu.
            if (!trongGioChan) dangNhuongMoiLuc = false
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

                // Nhuong ca cho app dung moi luc, vi du Telegram de Le Hoa nhan cho ba.
                // Buoc sang app khac la man chan che lai o nhip sau, nhu voi app Nop bai.
                nhac.loai == LoaiNhac.CHAN && nhuongAppMoiLuc() -> { dai.an(); chan.an() }

                nhac.loai == LoaiNhac.CHAN -> {
                    dai.an()
                    // Loi nhac khong doi thi chi chay kim dong ho, khong ve lai ca
                    // danh sach mon moi giay. Doi thi ve lai ngay: bam xong mot viec
                    // nha, hay het viec nha dung luc toi gio di hoc. Xem ManChan.capNhat.
                    chan.capNhat(nhac)
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
        // Chi cat gio cho man chan cua BUOI HOC. Man chan viec nha cung dung dong
        // ho lai, nhung viec do da lam ngay luc nhan lenh, va tin bao thi khac han.
        if (trongGioChan && nhac?.buoi != null) catGioChoiDangCo(nhac)

        // Van giu nhip mot giay ca khi man chan dang nhuong cho app: buoc ra khoi
        // app la no phai che lai ngay, khong de ho mot phut.
        return trongGioChan || dangDemGiay
    }

    /**
     * Man chan cua viec nha, neu dang co viec chua lam xong.
     *
     * Dung chung lop phu voi man chan gio di hoc: cung la che kin man hinh, cung
     * cham vao thi mo app Nop bai, cung nhuong cho khi app do dang mo. Khac o cho
     * no khong co moc het gio - no het khi nguoi lon bam xong het viec.
     */
    private fun nhacViecNha(): vn.huytl.homeworkgate.data.LoiNhac? {
        val phien = ViecNha.dangTreo(this)
        val con = phien?.chuaXong.orEmpty()
        if (con.isEmpty()) return null
        return vn.huytl.homeworkgate.data.LoiNhac(
            loai = LoaiNhac.CHAN,
            tieuDe = "${ViecNha.nguoiGiao(phien)} giao việc nhà",
            chiTiet = "Còn phải làm: " + con.joinToString(", ") { it.ten } + ".",
            gap = true,
            buoi = null,
            maBuoi = null
        )
    }

    /**
     * Toi gio di hoc thi giu lai phien choi dang chay.
     *
     * Khong co doan nay thi dong ho gio choi van chay sau lung man chan: Le Hoa
     * khong bam duoc gi ma van mat phut, va den luc tan hoc thi phieu gio da di
     * mat. Dung [GateStore.pause] chu khong cat han vi so phut do la do Ba Huy
     * duyet that - giu lai de choi not sau buoi hoc moi dung.
     *
     * Goi duoc moi nhip: pause() tra null khi khong co phien nao dang chay, hay phien da
     * nghi giu roi. Phien dang nghi vi tat man hinh (con de may do roi moi toi gio buong
     * may) thi lan goi dau doi no thanh nghi giu va tra so phut, nen tin bao di dung mot
     * lan, ngay luc vao gio hoc. Truoc 1/10/2026 lan nghi do van la tu dung nen tin cho toi
     * luc con bat man hinh (suy tu code). Va trong gio hoc, moi lan con bat man hinh la
     * phien chay lai roi bi cat, them mot tin nua (thu tren may ao: nhat ky co hai dong
     * "Toi gio di hoc" cung mot phut, xem [GateStore.dungViTatManHinh]).
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
     * Co ai dang nhin man hinh khong.
     *
     * Hoi he thong chu khong tu dem: [nhanSuKienManHinh] chi bat duoc luc bat va luc
     * tat, ma dich vu nay khoi dong lai nhieu lan trong ngay - lan nao cung phai
     * biet ngay trang thai hien tai chu khong cho su kien ke tiep.
     *
     * Hong thi coi nhu dang sang: doan sai ve phia nay chi ton pin, doan sai ve phia
     * kia thi lenh Ba Huy go den cham ma khong ai hieu vi sao.
     */
    private fun manHinhSang(): Boolean = runCatching {
        getSystemService(PowerManager::class.java)?.isInteractive != false
    }.getOrDefault(true)

    /**
     * Cach bao lau ghe hoi Telegram mot lan. 0 la nam cho lien tuc.
     *
     * Day la app thoi nam cho, khong phai may ngu: tablet van chay binh thuong, van
     * chan app nhu moi khi. Khac nhau o cho nam cho thi giu mot request mo suot 25
     * giay roi mo lai ngay, tuc la song vo tuyen khong bao gio xuong duoc trang thai
     * nghi sau; ghe hoi thi mo mot cai roi ve.
     *
     * BON MUC:
     *
     *  - 0, nam cho lien tuc: co nguoi that dang doi. Dang co phien choi hay bai cho
     *    duyet (cong khac LOCKED) VA man hinh dang sang, hay may dang mo toan bo.
     *    Lenh phai toi trong vai giay.
     *  - [NHIP_NGAY_MS], cong dang khoa giua ban ngay va DUONG FIRESTORE DANG SONG.
     *    Ban truoc muc nay khong ton tai: ghi chu cu noi phai nam cho ca ngay vi lenh
     *    Ba Huy go luc khoa khong duoc nghe thi im lang, ma dong do viet tu truoc khi
     *    co Firestore. Gio lenh di qua listener nam san trong dich vu tro nang, toi
     *    trong duoi mot giay, con Telegram la duong lui - tra 2.600 luot mot ngay cho
     *    mot duong lui thi khong dang. Het chin tram luot ban ngay con sau muoi.
     *  - [NHIP_DEM_MS] trong khung [NGUNG_TU]-[NGUNG_DEN]: khuya thi ai cung ngu,
     *    ke ca duong lui. Mot dem ba muoi sau luot.
     *
     * Chua ghep dien thoai Ba Huy thi ban ngay van nam cho lien tuc nhu cu: luc do
     * Telegram la duong DUY NHAT, khong phai duong lui.
     *
     * MAN HINH TAT thi ve nhip thua du cong khong khoa, tru khi dang co phien chay.
     * Ba muc tren chi nhin trang thai cong, ma trang thai khong noi duoc co ai dang
     * ngoi truoc may hay khong. Bai nop luc 20:30 ma Ba Huy ban chua duyet thi cong
     * o PENDING den tan nua dem - [vn.huytl.homeworkgate.data.GateStore] chi don
     * hang cho khi sang ngay moi, khong don luc gio ngu nhu phieu duyet va phien tam
     * dung. Ba tieng ruoi nam cho tung 45 giay, trong khi tablet up mat tren ban va
     * ket qua duyet khong ai nhin. Bat man hinh len la [nhanSuKienManHinh] danh thuc
     * vong poll ngay, nen cai gia chi la mot nhip cho lenh go tu Telegram - con lenh
     * tu Bang dieu khien thi di duong Firestore, khong dinh gi den day.
     *
     * Dat TRUOC phep kiem khoa, khong phai sau: dat sau thi ca hai truong hop ngoai
     * LOCKED (PENDING, va phieu da duyet chua bam) deu khong bao gio toi duoc dong
     * nay. Nhung ParentMode van dung tren cung: Ba Huy dang cam may thi do tre dang gia
     * hon pin.
     *
     * Moi muc cham hon deu phai nho hon [LENH_QUA_CU_MS] mot khoang rong, khong thi
     * lenh nam cho den luc duoc doc lai bi chinh app bao la "cu qua" va bo di.
     */
    private fun nhipNgheMs(): Long {
        if (ParentMode.isActive(this)) return 0L
        if (!manHinhSang() && gate.state != GateState.ACTIVE) return NHIP_NGAY_MS
        if (gate.state != GateState.LOCKED) return 0L

        val gio = java.util.Calendar.getInstance()
        val phut = gio.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            gio.get(java.util.Calendar.MINUTE)
        if (phut >= NGUNG_TU || phut < NGUNG_DEN) return NHIP_DEM_MS
        return if (DongBo.duongNhanhSong(this)) NHIP_NGAY_MS else 0L
    }

    /**
     * Loi mang trong luc dang ghe hoi thua thi cho den nhip sau, dung thu lai moi phut.
     *
     * Tablet tat wifi qua dem la ca dem thu lai sau moi phut - nhieu lan danh thuc
     * hon han so voi luc mang binh thuong, tuc la dung cai dinh tiet kiem lai thanh
     * ton nhat.
     */
    private fun cachThuLai(nhip: Long, binhThuong: Long): Long =
        if (nhip > 0L) maxOf(binhThuong, nhip) else binhThuong

    /** Vong long polling. Moi loi mang deu cho roi thu lai, khong bo cuoc. */
    private suspend fun pollLoop() {
        val client = tg
        var backoffMs = 2_000L

        while (scope?.isActive == true) {
            if (!canGiuKetNoi(this)) {
                stopSelf()
                return
            }
            capNhatUi()
            runCatching { guiNhacBaiChoBa() }.onFailure { Log.w(TAG, "nhac bai cho ba hong: ${it.message}") }
            val nhip = nhipNgheMs()
            if (nhip != nhipTruoc) {
                nhipTruoc = nhip
                Log.i(
                    TAG,
                    if (nhip > 0L) "ghe hoi Telegram ${nhip / 60_000} phut mot lan"
                    else "nam cho Telegram lien tuc"
                )
            }
            try {
                val updates = client.getUpdates(
                    offset = prefs.telegramOffset,
                    allowed = listOf("callback_query", "message"),
                    // Nhip thua thi hoi mot cai roi ve, khong nam cho 25 giay.
                    timeoutSec = if (nhip > 0L) 0 else TelegramClient.POLL_TIMEOUT_SEC
                )
                backoffMs = 2_000L
                if (prefs.loiTelegram != 0) prefs.loiTelegram = 0
                for (update in updates) {
                    prefs.telegramOffset = update.optLong("update_id", 0L) + 1
                    handleUpdate(client, update)
                }
                // Co lenh thi xu ly het da roi moi ve nhip thua. Ba Huy go hai lenh
                // lien nhau thi lenh thu hai khong phai cho het mot nhip nua.
                if (nhip > 0L && updates.isEmpty()) delay(nhip)
            } catch (e: TelegramClient.ApiException) {
                Log.w(TAG, "Telegram tra loi ${e.errorCode}: ${e.description}")
                // Hai ma nay khong tu khoi phuc duoc, phai co nguoi vao sua. Ghi lai
                // de bang canh bao trong app noi ra, thay vi cu thu lai am tham.
                if (e.errorCode == 401 || e.errorCode == 409) prefs.loiTelegram = e.errorCode
                // 409 la co hai noi cung goi getUpdates tren mot token. Doi lau hon
                // roi thu lai, vi day thuong la do bo mo bot tren may khac.
                delay(cachThuLai(nhip, if (e.errorCode == 409) 30_000L else backoffMs))
                backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
            } catch (e: Exception) {
                Log.w(TAG, "loi mang khi poll: ${e.javaClass.simpleName} ${e.message}")
                delay(cachThuLai(nhip, backoffMs))
                backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
            }
        }
    }

    /**
     * Tin nhac bai cho Ba Huy, toi hom truoc buoi hoc (Ba Huy chon 30/9/2026). Xem [NhacBai].
     *
     * Moi buoi mot tin, gui mot lan, tu [GIO_NHAC_BA] gio toi hom truoc buoi do. Vo chup sau
     * gio do thi tin di ngay nhip sau. Vo them dong sau khi tin da di thi khong gui lai: tin
     * vo dan do luc con luu da ke tung dong kem buoi se nhac.
     *
     * Chay trong vong poll chu khong trong nhip nhac: nhip nhac chi song luc man hinh sang,
     * ma toi hom truoc Le Hoa co khi khong mo may.
     */
    private fun guiNhacBaiChoBa(bayGio: java.time.LocalDateTime = java.time.LocalDateTime.now()) {
        if (!prefs.isConfigured || bayGio.hour < GIO_NHAC_BA) return
        val mai = bayGio.toLocalDate().plusDays(1)
        val cac = NhacBai.sapToi(this, bayGio).filter { it.ngay == mai }
        if (cac.isEmpty()) return
        val sp = prefs.raw()
        // Khoa buoi "20261003-CHIEU" xep duoc theo chu: bo khoa cua nhung ngay da qua.
        val homNay = "%04d%02d%02d".format(bayGio.year, bayGio.monthValue, bayGio.dayOfMonth)
        val daGui = sp.getStringSet(K_NHAC_BA_DA_GUI, emptySet()).orEmpty().filter { it >= homNay }.toMutableSet()
        val chua = cac.filter { it.ma !in daGui }
        if (chua.isEmpty()) return
        for (n in chua) {
            tg.sendMessage(prefs.parentChatId, chuNhacBa(n))
            daGui += n.ma
            sp.edit().putStringSet(K_NHAC_BA_DA_GUI, daGui.toSet()).apply()
            DayLog.add(this, "Nhắc ba Huy bài cho ${n.ten()}: ${n.cacBai.size} bài")
        }
    }

    private fun chuNhacBa(n: NhacBai.NhomBuoi): String = buildString {
        append("📚 ").append(if (n.cacBai.isEmpty()) "Dặn dò cho " else "Bài cho ")
        append(n.ten()).append(", vào học ").append(TinhLoiNhac.gioPhut(n.buoi.phutVaoHoc)).append(':')
        n.cacBai.forEach { append("\n• ").append(NhacBai.moTa(it)) }
        if (n.dongKhac.isNotEmpty()) {
            if (n.cacBai.isNotEmpty()) append("\n\nDặn dò khác:")
            n.dongKhac.forEach { append("\n• ").append(NhacBai.moTa(it)) }
        }
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
                // "a:<ma>:<phut>". Tin cu con nut "a:<ma>" theo so mac dinh; so mac
                // dinh da bo, nen nut do chi nhac ba chon mot nut co so phut.
                val phan = data.removePrefix("a:").split(":")
                val requestId = phan[0]
                val soPhut = phan.getOrNull(1)?.toIntOrNull()
                if (soPhut == null) {
                    client.answerCallbackQuery(callbackId, "Bấm một nút có ghi số phút.")
                    return
                }
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
                    gate.extend(soPhut)?.also { gate.boBaiCho(requestId) }
                } else {
                    gate.approve(wantedMinutes = soPhut, requestId = requestId)
                }
                if (minutes == null) {
                    client.answerCallbackQuery(callbackId, "Không cấp được: giờ ngủ hoặc hết hạn mức.")
                    client.sendMessage(
                        chatId,
                        "Không cấp giờ được. Hoặc đang trong giờ ngủ, " +
                            "hoặc hôm nay Lê Hòa đã dùng hết hạn mức."
                    )
                } else if (dangChoi) {
                    val them = soPhut
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
                        "Đã duyệt $minutes phút (chưa tính giờ). ${homNayDaDuyet()}${conChoBaoNhieu()}"
                    )
                }
                if (minutes != null) {
                    // Danh dau ben Bang dieu khien nua, khong thi bai da duyet o
                    // Telegram van nam trong danh sach "dang cho" ben dien thoai.
                    DongBo.datTrangThaiBai(this, requestId, "DUYET", soPhut)
                    GiaiDe.baDuyetBai(this, requestId)
                }
                if (messageId != 0L) client.clearReplyMarkup(chatId, messageId)
                withContext(Dispatchers.Main) { refreshNotification() }
            }

            data.startsWith("r:") -> {
                // Bo dung bai cua nut vua bam. Cac bai khac dang cho van nam nguyen:
                // bai nay sai khong co nghia la bai kia cung sai.
                val boBai = data.removePrefix("r:").substringBefore(':')
                if (gate.baiDangCho().none { it.id == boBai }) {
                    // Nut con sot lai duoi mot bai da duyet (go nut hong): ghi TUCHOI
                    // luc nay la de len DUYET ma con van giu gio. Xem DongBo.kt.
                    client.answerCallbackQuery(callbackId, "Yêu cầu này cũ rồi.")
                    if (messageId != 0L) client.clearReplyMarkup(chatId, messageId)
                    return
                }
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
            // "Tat ngay" da bo nam trong so nay, ca hai nut Duyet 45 phut / Khong duyet
            // duoi tin vo dan do (bo tron goi ngay 30/9/2026).
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

        // Ba Huy gui anh cho bot. Truoc 27/9/2026 anh va chu thuong gui cho bot duoc
        // chuyen vao khung chat trong app; khung do da bo, Le Hoa nhan tin bang Telegram
        // that. Tra loi de ba biet phai gui o dau, chu khong im.
        if ((msg.optJSONArray("photo")?.length() ?: 0) > 0) {
            baoNhanThang(client, msg.optJSONObject("chat")?.optLong("id") ?: prefs.parentChatId)
            return
        }
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
        // Chi loc lenh. /tinco thi khong: tin cua co gui toi qua thi sang nay van phai
        // hien.
        val guiLuc = msg.optLong("date", 0L) * 1000L
        if (text.startsWith("/") && word != "tinco" && guiLuc > 0L &&
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
                DongBo.datTrangThaiBai(this, bai.id, "TUCHOI", lyDo = arg)
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
                capGio(client, chatId, arg.toIntOrNull(), useQuota = false, why = "ba Huy cho thêm")
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

            // Cap gio tu quy gio choi (29/9/2026): /quy 30, hay /quy de cap het. Duong du
            // phong cua nut tren Bang dieu khien.
            "quy" -> {
                val kq = vn.huytl.homeworkgate.data.QuyGio.cap(this, arg.toIntOrNull()?.takeIf { it > 0 })
                client.sendMessage(
                    chatId,
                    kq.loi ?: "Đã cấp ${kq.cap} phút từ quỹ giờ chơi. Quỹ còn ${kq.conLai} phút."
                )
                refreshUi()
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
                        "$con đã nộp ${gate.soBaiDangCho()} bài, đang chờ ba Huy duyệt"
                    else -> "Tablet đang khoá"
                }
                val admin = if (Permissions.hasDeviceAdmin(this)) "" else
                    "\n⚠ Quản trị thiết bị đang tắt, app gỡ được."
                client.sendMessage(
                    chatId,
                    "$state. ${homNayDaDuyet()} Giờ ngủ ${gioChot()}.$admin"
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
             *
             * Cau con go nhieu dong hien dung cho con xuong dong. Dai qua mot tin Telegram
             * thi bo cau cu nhat, giu cau moi nhat. Xem NhatKyAi.tinHoi.
             */
            "hoi" -> {
                val log = if (arg == "tatca") NhatKyAi.tatCa(this) else NhatKyAi.homNay(this)
                client.sendMessage(
                    chatId,
                    if (log.isBlank()) {
                        "Chưa ghi được câu hỏi AI nào" +
                            if (arg == "tatca") "." else " hôm nay. Gõ /hoi tatca để xem cả tuần."
                    } else {
                        NhatKyAi.tinHoi(
                            if (arg == "tatca") "Câu hỏi AI đã ghi:" else "Hôm nay hỏi AI:",
                            log,
                            TelegramClient.MAX_TIN
                        )
                    }
                )
            }

            /**
             * Con hay sai kieu gi.
             *
             * /loi     - 30 ngay gan nhat
             * /loi 7   - 7 ngay gan nhat
             *
             * Khac moi bang khac trong app o cho no khong dem viec: khong phai "sai
             * bao nhieu cau" ma "trat o dau". Con so nay de ngoi noi chuyen voi con
             * chu khong de quyet dinh gi ca - khong cho nao trong app doc no.
             */
            "loi" -> {
                val ngay = (arg.trim().toIntOrNull() ?: 30).coerceIn(1, 365)
                val tu = System.currentTimeMillis() - ngay * 24L * 60 * 60_000L
                val bang = KhoBai.get(this).thongKeLoi(tu)
                val tong = bang.sumOf { it.second }
                client.sendMessage(
                    chatId,
                    if (bang.isEmpty()) {
                        "Chưa ghi được kiểu sai nào trong $ngay ngày qua. Bảng này " +
                            "chỉ tính từ bản app có chấm kiểu sai trở đi."
                    } else {
                        "$con hay sai kiểu gì, $ngay ngày qua ($tong lần):\n" +
                            bang.joinToString("\n") { (nhan, lan) ->
                                "• ${LoaiLoi.moTa(nhan)}: $lan lần"
                            }
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
                            "phải chọn lại từng môn và chụp gửi lại."
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

            // Tin co giao chia se tu nhom lop Zalo. Phai co lenh chu khong nhan chu
            // thuong nhu ben app Soan tap: chu thuong go cho bot la ba tuong minh dang
            // nhan cho Le Hoa, xem [baoNhanThang].
            "tinco" -> {
                if (arg.isEmpty()) {
                    client.sendMessage(
                        chatId,
                        "Gõ /tinco kèm nội dung cô nhắn, ví dụ:\n" +
                            "/tinco Mai lớp kiểm tra 15 phút môn Toán."
                    )
                } else {
                    khoTin.them(arg, guiLuc)
                    ChuongTin.baoTinCuaCo(this)
                    client.sendMessage(chatId, "Đã đưa lên tablet rồi.")
                }
            }

            "trogiup", "lenh", "giupdo", "help", "start" -> client.sendMessage(chatId, bangLenh())

            else -> if (text.startsWith("/")) {
                client.sendMessage(chatId, "Không có lệnh đó. Gõ /trogiup để xem danh sách.")
            } else if (text.isNotEmpty()) {
                baoNhanThang(client, chatId)
            }
        }
    }

    /**
     * Ba go chu thuong hay gui anh cho bot: nhac ba nhan thang cho Le Hoa tren Telegram.
     *
     * Truoc 27/9/2026 bot chuyen nhung tin do vao khung chat trong app Nop bai. Khung
     * do da bo; Le Hoa co tai khoan Telegram rieng, va nut "Nhan cho ba Huy" mo thang
     * khung chat voi ba. Tin gui cho bot tu gio khong toi tay con.
     */
    private fun baoNhanThang(client: TelegramClient, chatId: Long) {
        val con = getString(R.string.child_name)
        client.sendMessage(
            chatId,
            "Tin này không tới $con. Giờ $con đọc tin trên Telegram riêng, " +
                "nhắn thẳng vào khung chat với $con nhé. Gõ /trogiup để xem lệnh."
        )
    }

    /** Ke ma cau cho con doc: "câu 2.26a, 2.26b", dai qua thi cat bot. */
    private fun keTenCau(cac: List<vn.huytl.homeworkgate.kho.CauHoi>): String = when {
        cac.isEmpty() -> ""
        cac.size <= 3 -> "câu " + cac.joinToString(", ") { it.ma }
        else -> "câu " + cac.take(3).joinToString(", ") { it.ma } + " và ${cac.size - 3} câu nữa"
    }

    /**
     * Gui anh bai con vua chup sang Telegram, xep bai vao hang cho, roi cho Ba Huy cham.
     *
     * Tu 28/9/2026 tablet khong tu cham nua (Ba Huy bo han phan may cham): bai nao cung
     * nam cho toi luc Ba Huy nho Claude cham tren Bang dieu khien va dan ket qua ve. Gio
     * cap o [xuLyBanCham], luc lenh CHAMBAI toi.
     *
     * Thu tu quan trong. GUI ANH TRUOC, xep hang sau: gui anh la viec co the hong vi mang,
     * ma hong thi phai bao con chup lai - xep hang truoc roi gui hong thi co mot bai nam
     * cho ma Ba Huy khong co tam anh nao de cham.
     *
     * Lan nop lai cac cau sai cua mot bai sai het (xem [PhamVi.huyBaiCu]) thi huy bai cu
     * ngay sau khi gui anh duoc, va TRUOC khi xep bai moi vao hang: hang dang du ba bai ma
     * xep truoc thi [GateStore.markPending] day bai cu nhat ra, co khi la mot bai khac con
     * dang cho Ba Huy duyet.
     */
    private fun guiRoiCho(
        nhom: Map<CaptureStage, List<java.io.File>>,
        pham: PhamVi?,
        /** Luc con bam Gui. Xet han vo dan do theo luc nay, khong theo luc gui xong. */
        lucNop: Long = System.currentTimeMillis()
    ) {
        val anh = nhom.values.flatten()
        /*
         * Anh da mat thi thoi: khong gui, khong giu lai gi. Xay ra khi cung mot bo anh duoc
         * nho gui hai lan (bam Gui lai hai lan): lan truoc gui xong da xoa anh, lan nay toi
         * luot sau. Gui tiep thi hong vi mat anh, roi dong "chưa gửi được" hien lai cho mot
         * bai da nop.
         */
        if (anh.any { !it.exists() }) {
            Log.i(TAG, "bo lan gui: anh khong con")
            return
        }
        val sent = runCatching { HomeworkSender.send(this, nhom) }.getOrElse { e ->
            Log.w(TAG, "gui bai hong: ${e.message}", e)
            /*
             * Giu anh de Le Hoa bam Gui lai, va ghi vao nhat ky anh nao da len Telegram, hong
             * o dau, vi sao (Ba Huy chon ngay 28/9/2026). Truoc do cho nay xoa anh va bao
             * chup lai. Ma hong giua chung thi Telegram da co anh: lan 21:13 ngay 28/9/2026
             * Ba Huy thay album bai giai ma Bang dieu khien khong co bai, va khong biet vi sao.
             */
            val lan = BaiGuiHong.giu(this, nhom, pham?.sangJson(), lucNop)
            DayLog.add(
                this,
                "Gửi bài hỏng (" + BaiGuiHong.keAnh(this, nhom) +
                    (if (lan.soLan > 1) ", lần ${lan.soLan}" else "") + "): " +
                    HomeworkSender.moTaHong(this, e)
            )
            runCatching { DongBo.dayNgay() }
            return
        }

        val baiCu = pham?.takeIf { it.laSua && it.huyBaiCu }
            ?.let { p -> gate.baiDangCho().firstOrNull { it.id == p.suaBai } }
        if (baiCu != null) huyBaiCuKhiNopLai(baiCu)

        gate.markPending(sent.requestId, sent.messageId)
        // Phan tu luan cua de Giai de da gui: man chinh thoi nhac chup, cho diem ve.
        pham?.giaiDe?.takeIf { it.isNotBlank() }?.let {
            GiaiDe.daGuiTuLuan(this, it)
            GiaiDe.ghiBaiTuLuan(this, it, sent.requestId)
        }
        // Giu pham vi lai: lan cham den sau, luc Ba Huy dan ket qua Claude ve, ma luc do
        // van phai biet con da khai nhung cau nao.
        pham?.let { KhaiChoCham.luu(this, sent.requestId, it) }
        /*
         * Bai nop khong con mang theo vo dan do (30/9/2026). Truoc do tablet chep ban vo con
         * hieu luc vao bai, kem anh trang vo, de Claude tinh tron goi. Bo tron goi thi Claude
         * cham tung cau, khong can vo.
         */
        DongBo.dayBaiMoi(this, sent.requestId, sent.messageId, sent.anh, DongBo.banKhai(this, pham))
        // Dung bo anh cua mot lan gui hong dang giu: Le Hoa vua bam Gui lai va lan nay xong.
        // Lan hong nao cung bai voi lan nay thi bo, con da chup lai bai do roi.
        val xong = BaiGuiHong.xong(this, anh, pham)
        DayLog.add(
            this,
            (if (xong.guiLai != null) "Nộp bài (gửi lại): " else "Nộp bài: ") + BaiGuiHong.keAnh(this, nhom)
        )
        anh.forEach { runCatching { it.delete() } }

        val baoCu = if (baiCu != null) {
            " Bài nộp lúc ${gioPhut(baiCu.at)} sai hết nên tablet đã bỏ, thay bằng lần nộp lại này."
        } else {
            ""
        }
        runCatching {
            tg.sendMessage(
                prefs.parentChatId,
                "📝 Ba Huy mở Bảng điều khiển, bấm Nhờ Claude chấm, rồi dán kết quả về " +
                    "để tablet cộng giờ.$baoCu"
            )
        }
    }

    /**
     * Huy bai cu sai het khi con nop lai cac cau sai cua no. Xem [PhamVi.huyBaiCu].
     *
     * Lam y nhu con tu bam "Huỷ bài vừa nộp" o man chinh (HomeActivity.huyYeuCau): go bai
     * khoi hang cho, bao Bang dieu khien la con da huy, go hai nut duyet duoi tin cu ben
     * Telegram. Chi khac la khong hoi con: con da bam nut nop lai cua dung bai nay.
     */
    private fun huyBaiCuKhiNopLai(bai: vn.huytl.homeworkgate.data.BaiCho) {
        gate.boBaiCho(bai.id)
        DongBo.datTrangThaiBai(this, bai.id, "HUY")
        if (bai.messageId != 0L) {
            runCatching { tg.clearReplyMarkup(prefs.parentChatId, bai.messageId) }
        }
        DayLog.add(
            this,
            "${getString(R.string.child_name)} nộp lại, bỏ bài lúc ${gioPhut(bai.at)} (sai hết)"
        )
    }

    /** "11:18" cua mot moc gio, cho cac cau bao ve bai cu. */
    private fun gioPhut(luc: Long): String =
        java.text.SimpleDateFormat("HH:mm", java.util.Locale("vi", "VN")).format(java.util.Date(luc))

    /**
     * Moi lan chi mot ban cham duoc xu ly. Hai ban cham cho cung mot bai toi cung luc (ba
     * dan ket qua Claude hai lan luc tablet mat mang) thi ca hai deu thay bai con cho, va
     * con duoc cap gio hai lan. Xu ly lan luot thi lan sau thay bai da xong.
     */
    private val khoaCham = Any()

    /**
     * Moi lan chi gui mot lan nop, cac lan khac cho toi luot. Hai lan cung bo anh chay song
     * song (Le Hoa bam Gui lai luc lan truoc chua xong) thi mot bai vao hang hai lan, hoac
     * lan xong truoc xoa anh giua luc lan kia dang doc. Xem [BaiGuiHong.dangGui].
     */
    private val khoaGui = Any()

    /**
     * Xu ly mot ban cham cua Claude: cap gio, ghi so, bao Telegram, day sang dien thoai.
     *
     * Truoc 28/9/2026 doan nay con chay cho ca ban may tren tablet cham. Ba Huy bo han
     * phan may cham, nen gio chi con duong Claude: lenh CHAMBAI, xem [chamTheoClaude].
     */
    private fun xuLyBanCham(
        ket: KetQuaCham,
        pham: PhamVi?,
        /** Bai duoc chi dinh san, o duong Claude cham. null la bai vua nop, moi nhat. */
        baiId: String? = null,
        /** Ai cham, de ghi dung vao tin Telegram va nhat ky. */
        nguoiCham: String = "Claude",
        /** Ma cac cau Ba Huy bam "Chụp lại" o lenh XUCAU. Rong o lenh CHAMBAI. */
        chupLai: List<String> = emptyList()
    ) {
        val chatId = prefs.parentChatId
        val con = getString(R.string.child_name)
        val deId = pham?.giaiDe.orEmpty()
        /*
         * Duong Claude cham chi dinh san bai nao. Bai do da roi hang cho - Ba Huy vua
         * duyet tay hay tu choi - thi thoi han. KHONG lay bai moi nhat thay vao: nhu
         * vay la cong gio cua bai nay cho mot bai khac.
         */
        val bai = if (baiId != null) {
            gate.baiDangCho().firstOrNull { it.id == baiId } ?: run {
                Log.i(TAG, "cham theo $nguoiCham: bai $baiId khong con cho, bo qua")
                return
            }
        } else {
            gate.baiDangCho().lastOrNull()
        }

        /*
         * Moc gio cho cac luat tinh theo ngay: phan chup anh, phan lam them va phan on da
         * duoc bao nhieu.
         *
         * Claude cham luc Ba Huy dan ket qua, co khi tre vai tieng sau luc con nop. Tran
         * la tran cua ngay con lam bai, nen lay luc con nop.
         *
         * Khong co chuyen dan sang ngay hom sau: bai cho duyet khong song qua nua dem,
         * xem GateStore.donDepBaiCho. Nen moc nay chi lech bay gio trong cung mot ngay.
         */
        val luc = bai?.at?.takeIf { baiId != null && it > 0L } ?: System.currentTimeMillis()

        // Bo cac cau da tra gio tu lan nop truoc: chup lai bai cu khong duoc tinh
        // lan hai. Cau dang cho sua thi KHONG bo - lan nay con sua no.
        //
        // Hoi bang chinh cau vua cham chu khong bang de bai: cau nao con da khai thi
        // mang san ma sach, va ma sach thi hai lan nop deu nhu nhau. De bai chi con
        // dung cho bai ngoai sach.
        /*
         * Lan ON TAP di duong khac: cau von DA tra gio roi - do moi la dieu kien de
         * duoc on. Loc bang [SoCaiBai.daTraGioCua] o day se vut sach ca xap.
         *
         * Cai loc o day la LICH HEN: on cau chua den hen thi khong ghi so, khong tra
         * phut, con chi nhan mot cau bao chua den hen. Khong co cho nay thi chep lai
         * hai chuc cau cu moi toi la mot duong kiem gio deu dan. Cau ngoai danh sach
         * on (bai ngoai sach may tach them ra) cung roi o day, vi no khong bao gio
         * den hen - xem [vn.huytl.homeworkgate.kho.KhoBai.cacCauDenHenOn].
         */
        val onTap = pham?.onTap == true
        val moi = if (onTap) {
            ket.cac.filter { SoCaiBai.denHenOn(this, it) }
        } else {
            ket.cac.filter { !SoCaiBai.daTraGioCua(this, it) }
        }
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

        val bang = LuatCongGio.tinh(
            ket.copy(cac = moi),
            daCongAnhHomNay = SoCaiBai.phutAnhHomNay(this, luc)
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

        /*
         * Cau vua go duoc: lan nay dung, ma truoc day da tung sai.
         *
         * Do TRUOC khi ghi so, vi ghi xong thi chinh lan nay nam trong lich su.
         *
         * Vi sao dem rieng ra: bang cua con luon noi ve cai con no - "con 2 cau can
         * sua" - va khong co cho nao noi ve cai con da tra xong. Mot dua tre sua ba
         * lan moi dung dang duoc nghe mot cau khac voi dua dung ngay lan dau, va do
         * la cau duy nhat trong ca ban cham nay noi ve cong suc chu khong phai ket
         * qua.
         */
        // Duong Claude cham mot bai may da cham ma chua duyet (xem NhoClaude.chamMoi ben Bang
        // dieu khien): cau may cham sai o chinh lan nop nay da nam trong so. Chi dem lan sai
        // TRUOC lan nop nay, khong thi cau may cham nham thanh cau con "sua lai dung".
        val truocLuc = bai?.at?.takeIf { baiId != null && it > 0L } ?: Long.MAX_VALUE
        val vuaGo = coDe.filter { it.dung }
            .map { it to SoCaiBai.soLanSai(this, it, truocLuc = truocLuc) }
            .filter { it.second > 0 }

        /*
         * Doi chieu loi con khai truoc khi cham voi ket qua.
         *
         * Hai cho dang noi, va chi hai cho do:
         *  - bao chua chac ma dung: con dang tu danh gia thap minh;
         *  - bao chac ma sai: cho nguy hiem nhat trong ca xap bai, vi con se khong
         *    quay lai xem no nua.
         * Cau bao chac va dung, hay bao chua chac va sai, thi khong co gi de noi.
         */
        /*
         * Dau hoi rieng cua lan on tap.
         *
         * On tap cham lai mot cau DA lam dung, tuc la no thao mat cai khoa "moi cau
         * chi tra gio mot lan". Chup lai trang vo cu thi may khong phan biet duoc, vi
         * tren giay khong co dau thoi gian nao. Cai duoi day khong chan con, no chi
         * thoi tu duyet va day sang Ba Huy mo anh ra nhin.
         *
         * Truoc 27/9/2026 con mot dau hoi nua: bai on phai viet but do. Ba Huy bo luat do.
         */
        val nghiChupLai = if (!onTap) emptyList()
        else coDe.filter { SoCaiBai.giongHetLanTruoc(this, it) }

        val coKhai = pham?.daKhaiChac == true
        val chuaChac = pham?.chuaChac.orEmpty().toSet()
        val batNgo = if (!coKhai) emptyList()
        else coDe.filter { it.dung && it.cauId != null && it.cauId in chuaChac }
        val hutTay = if (!coKhai) emptyList()
        else coDe.filter { !it.dung && it.cauId != null && it.cauId !in chuaChac }
        val sai = coDe.filter { !it.dung }
        val dung = coDe.count { it.dung }
        val dauDe = if (ket.mon.isBlank()) "" else "${ket.mon} · "
        val than = StringBuilder(
            if (moi.isEmpty() && trung > 0) {
                "🤖 $nguoiCham chấm: ${dauDe}cả $trung câu đều đã tính giờ hôm trước"
            } else {
                "🤖 $nguoiCham chấm: $dauDe$dung/${moi.size} câu đúng" +
                    if (trung > 0) " ($trung câu đã tính giờ hôm trước, bỏ qua)" else ""
            }
        )
        than.append("\n")
        if (onTap) {
            than.append("• Ôn lại ${moi.size} câu từng sai\n")
        }
        if (pham != null && pham.bai.isNotBlank()) {
            // Bang dieu khien doc dong nay de chep vao loi nho Claude, xem NhoClaude.conKhai
            // ben do. Truoc ngay 26/9/2026 dong nay ghi "Con khai:", ben do doc ca hai kieu.
            than.append("• $con khai: ").append(pham.tenNguon.ifBlank { pham.mon })
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
        /*
         * So phut that su vao tay con, va cau noi cho con khi tu duyet ma khong cap duoc.
         *
         * Tran phut moi ngay cat bot trong GateStore ma khong bao ai, nen tinh truoc o
         * day, y nhu ThiHanhLenh.duyet. Truoc 27/9/2026 tin Telegram, nhat ky, trang thai
         * bai tren dien thoai va cau tren man con deu ghi so may tinh ra: dan ket qua
         * Claude trong gio ngu thi tablet khong cap phut nao va bai van cho duyet, ma man
         * con van ghi "Duoc them ... phut". Cham tran thi cap 3 phut ma cung ghi du so.
         */
        var phutCap = 0
        var khongCapCho: String? = null
        /** Lon hon 0 la phut chua vao tay con: giu toi luc nay, luc het gio ngu. Xem CongSang. */
        var congLuc = 0L
        // Ba Huy bam Duyet ben Telegram trong luc tablet dang xu ly ban cham: bai da bien khoi hang
        // cho. Tu duyet them lan nua la cong gio hai lan cho cung mot bai.
        val baDaXuLy = bai != null && gate.baiDangCho().none { it.id == bai.id }
        val tuDuyet = bang.phut > 0 && !bang.canBaHuyXem && !baDaXuLy && nghiChupLai.isEmpty()
        if (tuDuyet && baiId != null && bai != null && gate.trongGioNgu()) {
            /*
             * Ba Huy dan ket qua Claude trong gio ngu. Ghi so va dong bai ngay bay gio, con
             * gio thi giu toi luc het gio ngu (Ba Huy chon ngay 27/9/2026). Tran ngay xet luc
             * cong, vi phan nay an vao ngay duoc cong chu khong phai ngay cham. Xem CongSang.
             */
            congLuc = CongSang.lucCong(this)
            CongSang.them(this, bang.phut, bai.id)
            gate.boBaiCho(bai.id)
            daCap = true
            phutCap = bang.phut
            val gio = CongSang.gioCong(this)
            DayLog.add(this, "$nguoiCham chấm trong giờ ngủ, giữ ${bang.phut} phút tới $gio")
            than.append("Đang giờ ngủ ${gioChot()}: giữ ${bang.phut} phút, $gio tablet cộng.")
        } else if (tuDuyet) {
            // Tran rieng cua bai dan do da nam trong [LuatCongGio.tinh]; khong con tran
            // ngay cat them (29/9/2026).
            val them = bang.phut
            val duoc = if (them > 0) capGioTuAi(them, bai) else null
            if (duoc == null) {
                when {
                    gate.trongGioNgu() -> {
                        than.append("Không cấp được: đang giờ ngủ ${gioChot()}.")
                        khongCapCho = "Chấm xong lúc đang giờ ngủ nên máy không cộng giờ."
                    }
                    else -> {
                        than.append("Không cấp được. Ba Huy xem giúp nhé.")
                        khongCapCho = "Máy chưa cộng giờ được lần này."
                    }
                }
            } else {
                daCap = true
                phutCap = them
                DayLog.add(this, "$nguoiCham duyệt $them phút")
                than.append("Đã cấp $them phút")
                if (them < bang.phut) than.append(" (hôm nay chỉ còn $them phút trong hạn mức)")
                than.append(". Rút lại: /bot $them")
            }
        } else if (baDaXuLy) {
            than.append("Ba Huy đã xử bài này trước khi $nguoiCham chấm xong nên không cộng thêm.")
        } else if (nghiChupLai.isNotEmpty()) {
            than.append("Chưa cấp giờ: bài ôn ")
                .append(nghiChupLai.joinToString(", ") { it.ma })
                .append(" giống hệt lần trước từng dòng, có thể là chụp lại trang cũ. ")
                .append("Ba Huy xem ảnh giúp.")
        } else if (bang.canBaHuyXem) {
            than.append("Chưa cấp giờ: có chỗ $nguoiCham đọc chưa chắc. Ba Huy xem ảnh rồi duyệt giúp.")
        } else {
            than.append("Chưa cấp giờ. $con sửa lại rồi nộp tiếp.")
        }
        // Khong tu duyet ma van co so phut: noi ro con so do ra.
        //
        // Nut Duyet tren Telegram cap so phut MAC DINH (thuong la 60), khong phai so
        // AI vua tinh. Khong co dong nay thi Ba Huy bam mot cai la cho qua tay gap
        // muoi lan cai bai vua cham.
        if (!daCap && bang.phut > 0 && !baDaXuLy) {
            than.append("\n$nguoiCham tính ${bang.phut} phút")
            // Va sua luon cai nut duoi tin nop bai cho mang dung con so do. Khong
            // sua thi nut to van ghi so mac dinh, va bam mot cai la cho qua tay.
            bai?.let {
                runCatching {
                    tg.datBanPhim(
                        chatId, it.messageId,
                        TelegramClient.banPhimSauCham(it.id, bang.phut)
                    )
                }
            }
        }
        if (sai.isNotEmpty()) {
            than.append("\nCần sửa: ").append(sai.joinToString(", ") { it.ma })
        }
        // Dat cuoi cung, tach dong: day la cau Ba Huy doc roi nhan lai cho con mot
        // cau that. May noi thi khong bang, nhung may thi biet luc nao co chuyen do.
        if (vuaGo.isNotEmpty()) {
            than.append("\nGỡ xong: ")
                .append(vuaGo.joinToString(", ") { (c, lan) -> "${c.ma} (sai $lan lần)" })
        }
        // Cau con tu viet ra truoc khi nop lai. Dua nguyen van sang day: mot dong
        // con tu goi ten cai sai cua minh noi duoc nhieu hon ca bang cham.
        if (!pham?.conNoi.isNullOrBlank()) {
            than.append("\nCon tự nói: “").append(pham.conNoi).append("”")
        }
        if (batNgo.isNotEmpty()) {
            than.append("\nKhai chưa chắc mà đúng: ")
                .append(batNgo.joinToString(", ") { it.ma })
        }
        if (hutTay.isNotEmpty()) {
            than.append("\nKhai chắc mà sai: ").append(hutTay.joinToString(", ") { it.ma })
        }

        // Ghi so: cau sai luon ghi (de con con duong sua); cau dung chi ghi la xong
        // khi gio da vao tay con that. Khong cap duoc thi mai nop lai van duoc tinh.
        val ghiVaoSo = (if (daCap) moi else sai).filter { it.coDe }
        // Tran ngay cat bot thi so ghi so phut da tra that, xem LuatCongGio.chiaPhutDaCap.
        val phutTungCau = LuatCongGio.chiaPhutDaCap(bang, phutCap)
        val daGhi = SoCaiBai.ghi(
            this, ghiVaoSo, if (daCap) phutTungCau else emptyMap(), onTap = onTap,
            chuaChac = if (coKhai) chuaChac else null,
            conNoiChung = pham?.conNoi.orEmpty(),
            deId = deId
        )
        // Cau Claude doc chua chac ma cham chua dung: so van ghi sai, nhung an khoi danh sach
        // can sua cua con toi khi Ba Huy xu (30/9/2026), xem CauChuaRo.
        CauChuaRo.danhDau(this, ghiVaoSo, daGhi)
        // De Giai de: ghi diem phan tu luan vao de, va noi diem ca de ngay trong tin nay.
        // Diem tinh tu ban cham, ca khi chua tu duyet: no noi con lam duoc gi, khong phai
        // con duoc bao nhieu phut.
        if (deId.isNotBlank()) {
            GiaiDe.nhanTuLuan(this, deId, moi)?.let { than.append("\n").append(it) }
        }
        // Day len Firestore ngay: cai lai app la mat sach so cai trong may, ma so cai
        // la thu duy nhat chan viec chup lai bai cu de lay gio lan nua.
        runCatching { DongBo.daySoCai(this, daGhi) }

        // Mot dong cho man hinh cua con, duoi ten Ba Huy. Khong co dong nay thi con
        // nop bai, cho mot luc, roi khong thay gi doi ca - nhat la khi nop lai bai
        // da cham hom truoc: khong cong gio, ma cung khong co cau nao de sua.
        //
        // Phan noi ve gio: "duoc them" chi khi gio da vao tay con, va dung so phut da vao.
        // Tu duyet ma khong cap duoc thi noi vi sao. Ba Huy xu bai truoc khi cham xong thi
        // khong noi gi ve gio: gio la cua lan Ba Huy bam.
        val veGio = when {
            congLuc > 0L -> "Hết giờ ngủ lúc ${CongSang.gioCong(this)} thì được thêm $phutCap phút."
            daCap && phutCap < bang.phut -> "Được thêm $phutCap phút, hôm nay đủ giờ chơi rồi."
            daCap -> "Được thêm $phutCap phút."
            else -> khongCapCho.orEmpty()
        }
        // Chi hai nhanh khen ben duoi la tin vui, va chi khi khong kem cau "chua cong duoc
        // gio": man chinh hien tin vui duoi "Bài đã chấm" kem dau tich, khong duoi dau hoi do.
        // Xem [SoCaiBai.datLoiNhan].
        var tinVui = false
        val cauNhan = when {
                nghiChupLai.isNotEmpty() ->
                    "${getString(R.string.parent_name_cap)} đang xem lại bài ôn, chờ chút nhé."
                bang.canBaHuyXem ->
                    "${getString(R.string.parent_name_cap)} đang xem lại bài, chờ chút nhé."
                // Cau nay dung truoc cau "Bai tot": no noi ve dung cai kho nhat
                // con vua lam duoc, nen no phai la cau con doc thay dau tien.
                bang.phut > 0 && sai.isEmpty() && thieu.isEmpty() && vuaGo.isNotEmpty() -> {
                    tinVui = khongCapCho == null
                    val ten = vuaGo.take(3).joinToString(", ") { it.first.ma } +
                        if (vuaGo.size > 3) " và ${vuaGo.size - 3} câu nữa" else ""
                    "Câu $ten $con làm sai rồi sửa lại đúng. $veGio".trim()
                }
                bang.phut > 0 && sai.isEmpty() && thieu.isEmpty() -> {
                    tinVui = khongCapCho == null
                    "Bài tốt! $veGio".trim()
                }
                // Con khai mot loat cau roi chi chup duoc vai cau: phai noi ra so cau
                // con thieu, khong thi no chi thay so phut it hon minh tuong.
                bang.phut > 0 && sai.isEmpty() ->
                    "$veGio Còn ${keTenCau(thieu)} thì chưa thấy bài làm trong ảnh.".trim()
                bang.phut > 0 -> "$veGio Còn ${sai.size} câu sửa lại nhé.".trim()
                // Cau bi loc ra o lan on tap la cau CHUA DEN HEN, khong phai cau
                // "da on roi". Luat cu chi cho on mot lan, cau nay con sot lai tu do.
                //
                // Nhung con cau trong danh sach on ma anh khong co thi de nhanh "chua
                // thay bai lam" ben duoi noi: cau bi loc ra luc do co the chi la cau
                // ngoai danh sach, khong bao gio den hen, va cau "may nhac khi den luc"
                // thanh mot loi hua khong ai giu.
                onTap && moi.isEmpty() && trung > 0 && thieu.isEmpty() ->
                    "Mấy câu này chưa đến hẹn ôn lại. Máy nhắc $con khi đến lúc nhé."
                !onTap && moi.isEmpty() && trung > 0 ->
                    "Mấy bài này chấm hôm trước rồi, làm bài mới thì mới được cộng giờ nhé."
                // Ca xap chi co dap an: noi thang cho con biet phai chup them cai gi,
                // dung de no ngoi doan vi sao nop ma khong duoc gi.
                coDe.isEmpty() && khongCoDe.isNotEmpty() ->
                    "Ảnh chỉ có đáp án, không có đề bài nên máy không chấm được. " +
                        "$con chụp thêm trang đề giúp nhé."
                khongCoDe.isNotEmpty() ->
                    "Có ${khongCoDe.size} câu không thấy đề bài. $con chụp thêm trang đề nhé."
                thieu.isNotEmpty() && sai.isEmpty() ->
                    "Chưa thấy bài làm của ${keTenCau(thieu)} trong ảnh. Chụp lại cho rõ nhé."
                sai.isEmpty() -> "Chưa cộng giờ được cho bài này."
            else -> "Sửa lại ${sai.size} câu rồi chụp gửi nhé."
        }
        // Mot cau ve loi khai, dat sau cau chinh. Chi noi khi co gi dang noi.
        val doiChieu = when {
            hutTay.isNotEmpty() ->
                " Câu ${hutTay.joinToString(", ") { it.ma }} $con thấy chắc mà lại sai, " +
                    "xem kỹ chỗ đó nhé."
            batNgo.isNotEmpty() ->
                " Mấy câu $con bảo chưa chắc (${batNgo.joinToString(", ") { it.ma }}) " +
                    "hoá ra đúng hết."
            else -> ""
        }
        /*
         * Ba Huy nho chup lai vai cau (lenh XUCAU): cau nho chup lai dung truoc, kem cau ve gio,
         * va khong phai tin vui. Viet o day, sau cung, chu khong o ThiHanhLenh.xuCau: truoc
         * 1/10/2026 ben do ghi cau nay ngay luc nhan lenh, roi doan nay chay sau ghi de len, va
         * cac cau khac dung het thi con chi thay "Bài tốt!". Cau chup lai khong vao so cai nen
         * cung khong nam trong dong "Có N câu cần sửa", tru khi lan cham truoc da ghi no la sai.
         */
        val loiNhan = if (chupLai.isEmpty()) cauNhan + doiChieu else {
            tinVui = false
            ("${getString(R.string.parent_name_cap)} nhờ chụp lại câu ${chupLai.joinToString(", ")} " +
                "cho rõ rồi nộp lại. $veGio").trim()
        }
        SoCaiBai.datLoiNhan(this, loiNhan, tinVui = tinVui)
        Log.i(TAG, "cham bai: cap $phutCap phut, ${sai.size} cau can sua")

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
                        /*
                         * Cau cho Ba Huy tu xu (29/9/2026): Claude doc chua chac, hay cham dung
                         * ma khong ghi so dong. Bang dieu khien hien ba nut Dung (kem so dong
                         * khi canSoDong), Sai, Chup lai cho tung cau, roi gui lenh XUCAU.
                         * maClaude la ma trong ban Claude, de dien thoai tim lai dung cau.
                         * Tablet tu tinh danh sach nay de ben kia khong phai chep lai luat.
                         */
                        "canXem" to (
                            moi.filter { !it.docRo }.map { c -> cauCanXem(c, "CHUA_CHAC") } +
                                bang.thieuDong.filter { it.docRo }.map { c -> cauCanXem(c, "THIEU_DONG") }
                            ),
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
                if (daCap) DongBo.datTrangThaiBai(this, bai.id, "DUYET", phutCap, congLuc)
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

    /** Mot dong trong danh sach "canXem" cua ban cham, xem cho day ban cham len. */
    private fun cauCanXem(c: CauCham, lyDo: String): Map<String, Any> = mapOf(
        "ma" to c.ma,
        "maClaude" to c.maGoc.ifBlank { c.ma },
        "lyDo" to lyDo,
        // Trac nghiem va hoc thuoc khong tinh theo dong, nen nut Dung khong hoi so dong.
        "canSoDong" to (c.dang != DangBai.TRAC_NGHIEM && c.dang != DangBai.KHONG_TINH),
        "soDong" to c.soDong
    )

    /**
     * Cap so phut AI da tinh. Dang choi thi cong thang vao phien.
     *
     * useQuota = true: so phut nay tinh vao so phut doi bang hoc trong ngay (phutDaDuyet),
     * de man hinh noi "hom nay kiem duoc bao nhieu". Tu 29/9/2026 khong con tran chung nen
     * [GateStore] khong cat bot gi; tran rieng cua tung phan da ap luc tinh phut
     * ([vn.huytl.homeworkgate.data.LuatCongGio]).
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
        // Khong con so phut mac dinh: go thieu so thi nhac, khong tu doan.
        if (minutes == null) {
            client.sendMessage(chatId, "Gõ kèm số phút, ví dụ /duyet 30 hay /cho 30.")
            return
        }

        // Dang choi ma ba cho them thi cong vao phien dang chay, chu khong cat
        // phien roi cap lai tu dau. "Cho them 30 phut" luc con con 10 phut nghia
        // la 40, khong phai 30.
        if (gate.state == GateState.ACTIVE) {
            val them = minutes
            val left = gate.extend(them)
            // Duyet mot bai trong luc con dang choi: cong vao phien va go bai do ra
            // khoi hang cho, khong thi no nam do cho den khi qua ngay.
            bai?.let {
                gate.boBaiCho(it.id)
                DongBo.datTrangThaiBai(this, it.id, "DUYET", them)
                GiaiDe.baDuyetBai(this, it.id)
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
        val xin = minutes
        val granted = gate.approve(
            wantedMinutes = minutes,
            useQuota = useQuota,
            requestId = bai?.id,
            nhanCho = nhanCho
        )
        if (granted == null) {
            client.sendMessage(
                chatId,
                // Tu 29/9/2026 khong con tran chung moi ngay, nen chi con gio ngu (hay gio chot)
                // lam lan cap nay hong.
                "Không cấp được: đang trong giờ ngủ, hay đã quá giờ chốt ${gioChot()}."
            )
            return
        }
        if (messageId != 0L) client.clearReplyMarkup(chatId, messageId)
        bai?.let {
            DongBo.datTrangThaiBai(this, it.id, "DUYET", granted)
            GiaiDe.baDuyetBai(this, it.id)
        }
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
            "$dong ${homNayDaDuyet()}${conChoBaoNhieu()}"
        )
        refreshUi()
    }

    /**
     * "Hôm nay đã duyệt 60 phút. Quỹ giờ chơi 25 phút." cho cac tin sau mot lan cap.
     *
     * Truoc 29/9/2026 cho nay ghi "Hôm nay còn N phút", tinh theo tran chung 135 phut. Tran
     * chung da bo, moi phan co tran rieng, nen con so "còn" khong con nghia: chi noi da duyet
     * bao nhieu va quy dang giu bao nhieu.
     */
    private fun homNayDaDuyet(): String {
        val quy = vn.huytl.homeworkgate.data.QuyGio.so(this)
        return "Hôm nay đã duyệt ${GateStore(this).phutDaDuyetHomNay()} phút." +
            if (quy > 0) " Quỹ giờ chơi $quy phút." else ""
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
        /quy 30  cấp từ quỹ giờ chơi (/quy = cấp hết)
        /dung  tạm dừng, giữ giờ lại
        /tiep  chơi tiếp
        /khoa  khoá ngay, mất giờ còn lại

        XEM
        /trangthai  còn mấy phút, đã duyệt bao nhiêu
        /nhatky  hôm nay có gì
        /thongke  dùng app gì, mấy giờ (/thongke 1 = hôm qua)
        /hoi  ${getString(R.string.child_name)} hỏi AI những gì
        /loi  ${getString(R.string.child_name)} hay sai kiểu gì (/loi 7 = bảy ngày)
        /ai  danh sách app AI đang ghi (/ai <gói> thêm, /aibo bỏ)

        SOẠN TẬP
        /lichmai  buổi học kế tiếp có môn gì
        /soanlai  bắt soạn lại vì soạn thiếu
        /mo  mở màn chặn cho hết buổi học này
        /chan  chặn lại ngay
        /tatchan  tắt hẳn màn chặn, chỉ còn nhắc
        /batchan  bật lại màn chặn
        /tinco ...  đưa tin của cô lên tablet

        PHÒNG HỜ
        /bo  mở toàn bộ máy
        /choxoa  tắt quản trị để gỡ app
        /xoapin  xoá PIN, đặt lại trên tablet
        /trogiup  bảng này

        Nhắn cho ${getString(R.string.child_name)} thì nhắn thẳng trên Telegram của ${getString(R.string.child_name)}.
        Số phút, giờ ngủ, danh sách app: sửa trong app.
    """.trimIndent()

    /** Thong bao dem nguoc ve lai dung trang thai sau moi lenh. */
    private fun refreshUi() = runCatching { refreshNotification() }.let {}

    /**
     * Ve lai thong bao va tin ghim.
     *
     * Truoc day day la mot vong rieng, 30 giay mot lan. Vong do lech pha voi vong
     * long-poll nen CPU phai thuc day hai lan thay vi mot, ca ngay, trong khi viec
     * cua no thi khong gap: chu tren thong bao va tin ghim deu la con so tinh bang
     * phut. Bay gio no di ke ngay sau moi lan [pollLoop] quay vong - luc CPU vua
     * thuc day san va socket TLS con dang mo.
     */
    private suspend fun capNhatUi() {
        withContext(Dispatchers.Main) { refreshNotification() }
        capNhatDongHo()
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
            nhipNgheMs() > 0L -> "Tablet đang khoá, ${nhipNgheMs() / 60_000} phút mới " +
                "nghe Telegram một lần. Lệnh gõ từ app Bảng điều khiển thì tới ngay."
            else -> "Tablet đang khoá"
        }
        // Nhip thua ma cong khong khoa: man hinh tablet dang tat. Noi ra, khong thi
        // Ba Huy go /duyet, doi mot phut khong thay gi va tuong tablet chet. Truong
        // hop LOCKED thi chinh dong tren da noi roi, khong lap lai.
        val nhip = nhipNgheMs()
        val cham = if (nhip > 0L && gate.state != GateState.LOCKED) {
            "\nMàn hình tablet đang tắt nên lệnh gõ ở đây tới chậm, tối đa " +
                "${nhip / 60_000} phút. Lệnh từ app Bảng điều khiển vẫn tới ngay."
        } else {
            ""
        }
        val thieu = Permissions.missing(this)
        val canhBao = if (thieu.isEmpty()) "" else
            "\n⚠️ " + thieu.joinToString("; ") { it.ten }
        return "$dong$cham$canhBao"
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
                "Đã gửi ${gate.soBaiDangCho()} bài, đang chờ ba Huy duyệt"
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

        /**
         * Lenh cu hon chung nay thi khong chay nua.
         *
         * Nua tieng: du dai de om ca nhip muoi phut ben duoi va nhung luc
         * tablet mat mang, du ngan de khong co chuyen lenh cua toi hom truoc chay
         * vao sang hom sau.
         */
        private const val LENH_QUA_CU_MS = 30 * 60_000L

        /**
         * Bam "Nhan cho ba Huy" xong thi man chan cho bay nhieu truoc khi che lai. Xem
         * [nhuongAppMoiLuc]. Ba giay: du cho Telegram mo lanh tren tablet.
         */
        private const val CHO_APP_KE_MS = 3_000L

        /** Khung gio thoi nam cho Telegram, tinh bang phut trong ngay. */
        private const val NGUNG_TU = 23 * 60
        private const val NGUNG_DEN = 5 * 60

        /**
         * Khuya thi cach nay lau moi ghe hoi Telegram mot lan.
         *
         * Phai nho hon [LENH_QUA_CU_MS] mot khoang rong: lenh go luc nua dem nam
         * cho toi muoi phut moi duoc doc, ma doc xong lai bao "lenh cu qua" thi
         * thanh ra khong bao gio chay.
         */
        private const val NHIP_DEM_MS = 10 * 60_000L

        /**
         * Ban ngay, cong dang khoa, va da co dien thoai Ba Huy o dau kia.
         *
         * Nam phut chu khong phai muoi nhu ban dem: ban ngay Ba Huy con go Telegram
         * that - luc dang di duong, luc dien thoai het pin - nen do tre phai o muc
         * cho duoc chu khong phai muc "sang mai doc cung duoc".
         *
         * Khong ha xuong mot phut: nhu vay la 540 luot mot ngay, chi kem nam cho
         * lien tuc mot bac, ma cai do thi Firestore da lam nhanh hon nhieu roi.
         */
        private const val NHIP_NGAY_MS = 5 * 60_000L

        /**
         * Nhip xet loi nhac soan tap luc binh thuong.
         *
         * Mot phut: moc som nhat trong thoi khoa bieu la 8:45, va cham mot phut so
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

        /** Cham mot bai dang cho theo ket qua Claude. Xem [chamTheoClaude]. */
        const val ACTION_CHAM_CLAUDE = "vn.huytl.homeworkgate.CHAM_CLAUDE"

        /** Xet lai loi nhac ngay, vi du vua co dot viec nha moi. Xem [ensureRunning]. */
        private const val ACTION_XET_LAI = "vn.huytl.homeworkgate.XET_LAI"

        /** Ma bai dang cho, o duong Claude cham. */
        private const val EXTRA_BAI_ID = "bai_id"

        /** Tu gio nay toi hom truoc buoi hoc thi gui Ba Huy tin nhac bai. Xem [guiNhacBaiChoBa]. */
        private const val GIO_NHAC_BA = 19

        /** Cac buoi da gui tin nhac bai, theo khoa [NhacBai.NhomBuoi.ma]. */
        private const val K_NHAC_BA_DA_GUI = "nhac_ba_da_gui"

        /** Ban cham theo ket qua Claude, dang chu cua [ChamBaiIO]. */
        private const val EXTRA_BAN_CHAM = "ban_cham"

        /** Ma cac cau Ba Huy nho chup lai (lenh XUCAU), xem [xuLyBanCham]. */
        private const val EXTRA_CHUP_LAI = "chup_lai"

        /** Duong dan anh, moi buoc chup mot mang: EXTRA_ANH + ten buoc. */
        private const val EXTRA_ANH = "anh"

        /** Bai con da khai truoc khi chup, dang JSON cua [PhamVi]. */
        private const val EXTRA_PHAM = "pham_vi"
        private const val EXTRA_LUC_NOP = "luc_nop"

        /**
         * Hop thoai ma app dung moi luc goi len: xin quyen cua HyperOS (Telegram xin mic
         * luc ghi am lan dau), chon anh, chon file. Chung hien tren app, mang ten goi
         * rieng. Dang nhuong ma thay mot trong so nay thi van nhuong, khong thi man chan
         * che mat ca hop lan Telegram. Khong dua vao ALWAYS_ALLOWED: trinh quan ly quyen
         * cua Xiaomi van phai bi guard chan khi con tu mo no.
         */
        private val HOP_THOAI_CUA_APP = setOf(
            "com.lbe.security.miui",
            "com.google.android.providers.media.module",
            "com.android.providers.media.module",
            "com.google.android.documentsui",
            "com.android.documentsui"
        )

        /**
         * Gui mot lan nop con vua chup xong.
         *
         * Lam o service chu khong o man hinh vi viec nay phai chay cho xong du con dong
         * man hinh lai ngay sau khi bam gui: tai anh len Telegram mat vai giay.
         *
         * Anh chia theo tung buoc chup, vi tin nhan Telegram van phai co nhan
         * "de bai / bai giai" nhu tu truoc den gio.
         *
         * Khong con ban cham nao di kem: tu 28/9/2026 tablet khong tu cham nua, bai nao
         * cung cho Ba Huy nho Claude cham roi dan ket qua ve - xem [chamTheoClaude].
         */
        fun guiBai(
            context: Context,
            nhom: Map<CaptureStage, List<java.io.File>>,
            pham: PhamVi?,
            /** Luc bam Gui. Gui lai mot lan hong thi van la luc bam Gui lan dau, xem [BaiGuiHong]. */
            lucNop: Long = System.currentTimeMillis()
        ) {
            val intent = Intent(context, ApprovalService::class.java)
                .setAction(ACTION_GUI)
                .putExtra(EXTRA_LUC_NOP, lucNop)
                .putExtra(EXTRA_PHAM, pham?.sangJson())
            nhom.forEach { (st, files) ->
                intent.putStringArrayListExtra(
                    EXTRA_ANH + st.name, ArrayList(files.map { it.absolutePath })
                )
            }
            context.startForegroundService(intent)
        }

        /**
         * Cham mot bai dang cho theo ket qua Claude, dung khi may chua cham bai do.
         *
         * Lam o service vi cung ly do voi [guiBai]: cap gio va gui tin Telegram phai
         * chay cho xong, va doan xu ly ban cham von nam o day. Ban cham tao tu
         * [vn.huytl.homeworkgate.data.ChamTheoClaude].
         */
        fun chamTheoClaude(
            context: Context,
            baiId: String,
            ket: KetQuaCham,
            pham: PhamVi?,
            /** Ma cac cau Ba Huy nho chup lai, o lenh XUCAU. */
            chupLai: List<String> = emptyList()
        ) {
            val intent = Intent(context, ApprovalService::class.java)
                .setAction(ACTION_CHAM_CLAUDE)
                .putExtra(EXTRA_BAI_ID, baiId)
                .putExtra(EXTRA_PHAM, pham?.sangJson())
                .putExtra(EXTRA_BAN_CHAM, ChamBaiIO.viet(ket))
                .putStringArrayListExtra(EXTRA_CHUP_LAI, ArrayList(chupLai))
            context.startForegroundService(intent)
        }

        /**
         * Bat service neu dang can, khong thi thoi.
         *
         * [xetLaiNgay] la xet lai loi nhac ngay luc do. Can khi vua co viec nha moi:
         * service dang chay thi lenh bat chi danh thuc duong Telegram, con vong xet man
         * chan van ngu het nhip mot phut, va trong phut do man hinh van mo.
         */
        fun ensureRunning(context: Context, xetLaiNgay: Boolean = false) {
            val st = GateStore(context).state
            Log.i(TAG, "ensureRunning: state=$st canGiu=${canGiuKetNoi(context)}")
            if (!canGiuKetNoi(context)) return
            val intent = Intent(context, ApprovalService::class.java)
            if (xetLaiNgay) intent.action = ACTION_XET_LAI
            context.startForegroundService(intent)
        }
    }
}
