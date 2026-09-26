package vn.huytl.homeworkgate.dongbo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Handler
import android.os.SystemClock
import android.os.Looper
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import vn.huytl.homeworkgate.BuildConfig
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.BaiDaCham
import vn.huytl.homeworkgate.data.ChatFrom
import vn.huytl.homeworkgate.data.ChatLine
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.NhatKyAi
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.kho.TraLoi
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.guard.GuardAccessibilityService
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.Permissions
import vn.huytl.homeworkgate.guard.TinCuaBa
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Duong day sang app Bang dieu khien tren dien thoai Ba Huy.
 *
 * VI SAO KHONG PHAI TELEGRAM: mot con bot khong bao gio thay tin nhan cua chinh
 * no trong getUpdates, va moi token chi mot may duoc long-poll. Nghia la app ben
 * dien thoai khong the gia lam Ba Huy go lenh - xem HopThu ben app cua ba noi,
 * no da dam vao dung buc tuong nay.
 *
 * CHO NGHE NAM O DAU: khong mo service moi, khong dung FCM. Vong doi cua lop nay
 * gan vao [vn.huytl.homeworkgate.guard.GuardAccessibilityService] - dich vu do von
 * da song 24/7 vi Android giu no, ke ca khi cong dang khoa va moi service khac da
 * tat. Di ke no thi lenh toi trong duoi mot giay ma khong them mot tien trinh nen
 * nao.
 *
 * DAY TRANG THAI LUC NAO: bat vao SharedPreferences. Moi thu trong app - trang
 * thai cong, so phut, nhat ky, cau hinh - deu nam trong mot file prefs, nen nghe
 * file do doi la biet co gi moi ma khong phai ram mot dong goi ham vao hai chuc
 * cho trong ma nguon.
 *
 * Telegram van chay song song, khong dung cham gi. Dien thoai het pin thi mo
 * Telegram len van dieu khien duoc nhu cu.
 */
object DongBo {

    private const val TAG = "DongBo"


    /** Ma ghep doi song bao lau. Du de cam dien thoai len go, khong du de quen. */
    private const val MA_GHEP_SONG_MS = 10 * 60_000L

    /** Gom nhieu thay doi lien nhau thanh mot lan ghi. */
    private const val DOI_GOM_MS = 1200L

    /**
     * Gom lau nhat bay nhieu roi phai day, du thay doi van don den.
     *
     * Khong co tran nay thi viec gom bi doi vo han: man hinh chinh cua Le Hoa goi
     * tick moi giay trong luc dang choi, tuc la lan hen nao cung bi huy truoc khi
     * toi. Dung luc con dang cam may thi bang dieu khien ben dien thoai dung im.
     */
    private const val TRAN_GOM_MS = 5_000L

    /**
     * Nhip day lai du khong co gi doi, de dien thoai biet tablet con song.
     *
     * Ban ngay muoi lam phut mot lan, ban dem mot tieng, chay bang [tay] - mot
     * Handler.
     *
     * NEN NO KHONG DUNG GIO, va cho nay tung ghi sai. Handler hen theo
     * SystemClock.uptimeMillis, dong ho DUNG LAI khi CPU ngu: tablet nam im tren
     * ban ca buoi toi thi nhip muoi lam phut co the thanh vai tieng. No cung khong
     * danh thuc may bao gio - ghi chu cu noi nhip dem "cat duoc hai muoi tu luot
     * danh thuc mot dem", ma Handler chua tung danh thuc lan nao de ma cat. Cai
     * nhip dem that su tiet kiem la luot GHI Firestore, khong phai luot thuc day.
     *
     * KHONG DOI SANG AlarmManager, va day la lua chon co chu dich. Mot tablet dang
     * ngu thi khong co tin gi de bao ca; danh thuc no day chi de noi "toi van day"
     * la dot pin that de mua mot thong tin khong ai dung. Ben Bang dieu khien khong
     * cho nhip nay nua: mo app ra la no go [Lenh.PING], tablet dap trong duoi mot
     * giay, va do moi la luc con so can dung. Nhip tim gio chi con la luoi do cho
     * nhung luc khong ai hoi.
     */
    private const val NHIP_TIM_MS = 15 * 60_000L

    /** Nhip tim trong khung gio khuya. */
    private const val NHIP_TIM_DEM_MS = 60 * 60_000L

    /** Khung gio khuya, tinh bang phut trong ngay. Trung khung thoi nghe Telegram. */
    private const val DEM_TU = 23 * 60
    private const val DEM_DEN = 5 * 60

    private val tay = Handler(Looper.getMainLooper())
    private var ct: Context? = null
    private var dangChay = false

    private var ngheLenh: ListenerRegistration? = null
    private var ngheGhep: ListenerRegistration? = null
    private var ngheViecNha: ListenerRegistration? = null

    /** Cac lenh da lam trong lan chay nay, de khong lam hai lan neu xoa hut. */
    private val daLam = mutableSetOf<String>()

    /**
     * Giu tham chieu manh toi lang nghe prefs.
     *
     * SharedPreferences chi giu tham chieu yeu toi lang nghe. De no lam bien cuc bo
     * thi bo don rac nuot mat sau vai phut, va tu do tablet im lang - khong loi,
     * khong dau hieu gi, chi la dien thoai khong bao gio cap nhat nua.
     */
    /**
     * Nhung khoa trong prefs khong lien quan gi den cai day len Firestore.
     *
     * Ca app dung chung mot file prefs, nen khong loc thi moi lan ghi offset cua
     * Telegram hay nhip tim cung keo theo mot luot ghi Firestore, tuc la mot lan
     * bat song. Danh sach nay la cac khoa ghi deu ma khong co mat trong ban day.
     */
    private val KHOA_BO_QUA = setOf(
        "tg_offset",
        "da_noi_dien_thoai_ba",
        "heartbeat_msg",
        "heartbeat_wall",
        "menu_lenh_ban",
        "su_dung_doan",
        TinCuaBa.K_SO,
        K_DAU_DS_APP,
        K_NHA
    )

    private val ngheDoi = SharedPreferences.OnSharedPreferenceChangeListener { _, khoa ->
        if (khoa !in KHOA_BO_QUA) day()
    }

    /**
     * Cai hay go app thi gui lai danh sach app sang dien thoai.
     *
     * Truoc day danh sach chi di luc dong bo vua bat va luc ghep may. App cai sau luc
     * do khong hien ben Bang dieu khien cho toi lan tien trinh khoi dong lai, nen Ba
     * Huy khong tich duoc app vua cai vao danh sach nao tu dien thoai.
     *
     * Cap nhat app cung ban ra hai su kien nay. [dayDanhSachApp] so danh sach ten goi
     * truoc, khong doi thi thoi, nen moi lan cap nhat chi ton mot luot hoi
     * PackageManager.
     */
    private val ngheCaiApp = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            ct?.let { runCatching { dayDanhSachApp(it) } }
        }
    }

    private val nhipTim = object : Runnable {
        override fun run() {
            // Xoa ban da day de lan nay chac chan di, du khong co gi doi: ben dien
            // thoai coi so lieu qua lau khong ai dong la so lieu cu.
            banDaDay = null
            dayNgay()
            tay.postDelayed(this, nhipTimMs())
        }
    }

    /** Ban dem thi tha nhip tim ra. Xem [NHIP_TIM_DEM_MS]. */
    private fun nhipTimMs(): Long {
        val gio = java.util.Calendar.getInstance()
        val phut = gio.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            gio.get(java.util.Calendar.MINUTE)
        return if (phut >= DEM_TU || phut < DEM_DEN) NHIP_TIM_DEM_MS else NHIP_TIM_MS
    }

    /**
     * Ban trang thai vua day len, de khong day lai y het mot lan nua.
     *
     * So sanh bo qua [Duong.F_CAP_NHAT_LUC] vi truong do lan nao cung khac.
     */
    private var banDaDay: Map<String, Any?>? = null

    /** Ban sao cau hinh vua ghi, de chi ghi lai khi no doi. Xem [dayCaiDatNeuDoi]. */
    private var caiDatDaDay: Map<String, Any>? = null

    /** Ban vo dan do vua ghi len hop/dando, null la vua xoa. Xem [dayDanDoNeuDoi]. */
    private var danDoDaDay: Map<String, Any>? = null
    private var daDayDanDo = false

    /** Luc bat dau chuoi gom hien tai, de giu [TRAN_GOM_MS]. */
    private var batDauGom = 0L

    /** Hai so nhat ky da day, de khong ghi lai y het. */
    private var nhatKyDaDay: List<String>? = null
    private var hoiAiDaDay: List<String>? = null

    // --------------------------------------------------------------- vong doi

    /** Da khai bao Firebase chua. Thieu google-services.json thi ca lop nay nam im. */
    fun san(context: Context): Boolean = app(context) != null

    /**
     * Duong lenh nhanh co dang song khong: Firestore da noi VA co dien thoai o dau kia.
     *
     * Ben [vn.huytl.homeworkgate.telegram.ApprovalService] hoi cau nay de biet co con
     * phai nam cho Telegram tung 25 giay mot hay khong. Phai du ca hai ve: Firestore
     * chay ma chua ghep dien thoai nao thi khong ai gui lenh qua duong do ca.
     *
     * Khong hoi "dien thoai co dang mo khong" - cai do khong biet duoc, va cung khong
     * can: listener nam ben tablet, lenh Ba Huy go luc nao cung toi ngay luc do.
     */
    fun duongNhanhSong(context: Context): Boolean =
        dangChay && Prefs.get(context).daNoiDienThoaiBa

    /**
     * Ten du an Firebase ban build nay dang noi toi.
     *
     * Ban go loi va ban that nam o hai du an khac nhau, chon bang chinh cai file
     * google-services.json ma Gradle nhet vao (xem app/build.gradle.kts). Hai du an
     * nhin tu trong app thi giong het nhau, nen phai co mot cho doc ra duoc dang o
     * du an nao - khong thi may ao ghi vao du lieu that ma khong ai biet.
     */
    fun duAn(context: Context): String = app(context)?.options?.projectId.orEmpty()

    fun batDau(context: Context) {
        if (dangChay) return
        val ung = context.applicationContext
        if (!san(ung)) {
            Log.i(TAG, "chua co google-services.json, khong noi Bang dieu khien")
            return
        }
        ct = ung
        dangChay = true
        Log.i(TAG, "noi Firebase project=${duAn(ung)} (${if (BuildConfig.DEBUG) "ban go loi" else "ban that"})")

        dangNhap(ung) { duoc, _ ->
            if (!duoc) {
                dangChay = false
                return@dangNhap
            }
            // Lap nha XONG roi moi bat lang nghe.
            //
            // Bat truoc thi moi lang nghe va moi lan ghi deu tra ve
            // PERMISSION_DENIED: luat ben Firestore hoi "co phai nguoi nha khong",
            // ma nha thi chua ton tai. Cho mot nhip nay la het.
            lapNhaNeuChua(ung) { xong ->
                if (!xong) {
                    dangChay = false
                    return@lapNhaNeuChua
                }
                batNgheLenh(ung)
                batNgheViecNha(ung)
                batNgheGhep(ung)
                Prefs.get(ung).raw().registerOnSharedPreferenceChangeListener(ngheDoi)
                ung.registerReceiver(ngheCaiApp, IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addDataScheme("package")
                })
                tay.post(nhipTim)
                dayCaiDat(ung)
                dayDanhSachApp(ung)
            }
        }
    }

    fun dungLai() {
        val ung = ct
        dangChay = false
        banDaDay = null
        caiDatDaDay = null
        danDoDaDay = null
        daDayDanDo = false
        nhatKyDaDay = null
        hoiAiDaDay = null
        batDauGom = 0L
        tay.removeCallbacksAndMessages(null)
        ngheLenh?.remove(); ngheLenh = null
        ngheGhep?.remove(); ngheGhep = null
        ngheViecNha?.remove(); ngheViecNha = null
        if (ung != null) {
            runCatching { Prefs.get(ung).raw().unregisterOnSharedPreferenceChangeListener(ngheDoi) }
            // Chua kip dang ky (dung lai truoc khi lap nha xong) thi ham nay nem loi.
            runCatching { ung.unregisterReceiver(ngheCaiApp) }
        }
    }

    // ------------------------------------------------------------ day di len

    /** Day trang thai, gom cac thay doi lien nhau thanh mot lan ghi. */
    fun day() {
        val gio = SystemClock.elapsedRealtime()
        if (batDauGom == 0L) batDauGom = gio
        if (gio - batDauGom >= TRAN_GOM_MS) return dayNgay()
        tay.removeCallbacks(dayThat)
        tay.postDelayed(dayThat, DOI_GOM_MS)
    }

    fun dayNgay() {
        tay.removeCallbacks(dayThat)
        tay.post(dayThat)
    }

    /**
     * Day ban trang thai day du, ke ca khi khong co gi doi so voi lan truoc.
     *
     * [dayThat] binh thuong bo qua lan ghi neu ban moi giong het ban vua day - do
     * la cai giu cho so luot ghi Firestore o muc vai chuc mot ngay. Nhung khi ben
     * kia HOI (xem [Lenh.PING]) thi cau tra loi "khong co gi doi" phai duoc noi ra,
     * khong thi dien thoai cho mai mot ban tin khong bao gio den.
     */
    fun dayDayDu() {
        banDaDay = null
        dayNgay()
    }

    private val dayThat = Runnable {
        val context = ct ?: return@Runnable
        val hop = hop(context, Duong.D_TRANG_THAI) ?: return@Runnable
        val gate = GateStore(context)
        val prefs = Prefs.get(context)
        val bayGio = System.currentTimeMillis()
        val conLai = gate.remainingMs()

        val noi: Map<String, Any> = mapOf(
            Duong.F_CONG to gate.state.name,
            // Moc ket thuc theo gio that, khong phai so phut con lai: dien thoai tu
            // tru dan tren may no, nen cho nay khong phai ghi moi giay mot lan.
            Duong.F_KET_THUC_LUC to if (gate.state == GateState.ACTIVE) bayGio + conLai else 0L,
            /*
             * So ms DUNG YEN, khong phai so dang chay.
             *
             * Dang choi thi ben dien thoai tu tru tu [Duong.F_KET_THUC_LUC], khong
             * nhin truong nay - nen gui 0. Truoc day gui thang remainingMs: no doi
             * moi giay, lam moi phep so sanh "co gi doi khong" thanh vo nghia, va
             * ca ngay khong bao gio bo qua duoc mot luot ghi nao.
             *
             * Dang tam dung hay dang giu phieu thi so nay dung im, va do moi la luc
             * ben kia can no.
             */
            Duong.F_CON_LAI_MS to when (gate.state) {
                GateState.PAUSED -> gate.pausedMs()
                GateState.GRANTED, GateState.PENDING -> gate.grantedMinutes * 60_000L
                else -> 0L
            },
            /** Ca phien dai bao nhieu, de ben kia ve thanh chay cho dung. */
            Duong.F_TONG_PHIEN_MS to gate.tongPhienMs(),
            Duong.F_PHUT_DA_DUYET to gate.phutDaDuyetHomNay(),
            Duong.F_PHUT_CON_LAI to gate.phutConLaiHomNay(),
            Duong.F_SO_BAI_CHO to gate.soBaiDangCho(),
            // Viec nha ba noi giao, cac viec CHUA xong. Khong co truong nay thi ben
            // dien thoai chi thay "dang tam dung" ma khong hieu vi sao, trong khi
            // tablet dang bi che kin man hinh.
            Duong.F_VIEC_NHA to ViecNha.dangTreo(context)?.chuaXong.orEmpty().map { it.ten },
            Duong.F_CHE_DO_BA to mapOf(
                "bat" to ParentMode.isActive(context),
                "hetLuc" to if (ParentMode.coHan(context)) {
                    bayGio + ParentMode.remainingMs(context)
                } else 0L
            ),
            Duong.F_QUYEN to mapOf(
                "trogiup" to Permissions.hasAccessibility(context),
                "quantri" to Permissions.hasDeviceAdmin(context),
                "noi" to Permissions.hasOverlay(context),
                "pin" to prefs.hasPin()
            ),
            Duong.F_PIN_MAY to pinMay(context),
            Duong.F_DANG_SAC to dangSac(context),
            Duong.F_BAN_APP to BuildConfig.VERSION_NAME,
            Duong.F_CAP_NHAT_LUC to bayGio
        ) + truocMat()
        /*
         * Khong co gi doi so voi lan truoc thi thoi ghi. Bo [Duong.F_CAP_NHAT_LUC] ra
         * khoi phep so vi truong do lan nao cung khac.
         *
         * PHEP SO NAY CHI AP CHO DOCUMENT TRANG THAI. Nhat ky phai di tiep du trang
         * thai khong doi: con hoi AI mot cau, hay ba noi bam xong mot viec nha, deu
         * ghi vao nhat ky ma khong lam doi mot truong nao o tren. Ban dau cho nay
         * return thang, va hau qua la nhat ky ben dien thoai Ba Huy dung im ca ngay.
         */
        val deSo = noi - Duong.F_CAP_NHAT_LUC
        batDauGom = 0L
        if (deSo != banDaDay) {
            banDaDay = deSo
            hop.set(noi)
                .addOnFailureListener { Log.w(TAG, "day trang thai hong: ${it.message}") }
        }

        dayCaiDatNeuDoi(context)
        dayDanDoNeuDoi(context)
        dayNhatKy(context)
    }

    /**
     * Vo dan do cua ngay, cho the vo dan do o tab Bang ben dien thoai. Xem [Duong.D_DAN_DO].
     *
     * Chay trong [dayThat] y nhu ban sao cau hinh: ban vo nam trong cung file prefs, nen
     * con luu, Claude doc, hay ban het han bi don deu keo theo mot lan so. Khong co ban
     * nao con hieu luc thi xoa document, de ben dien thoai khong con nut nho Claude doc
     * mot tam vo cu. Nhip tim goi [dayThat] deu dan, nen ban het han qua dem cung duoc
     * xoa ma khong can ai mo man vo dan do.
     */
    private fun dayDanDoNeuDoi(context: Context) {
        val vo = VoDanDo.conHieuLuc(context)
        val ban = vo?.let { banDanDo(it) + (Duong.F_LUC to it.luc) }
        if (daDayDanDo && ban == danDoDaDay) return
        val ref = hop(context, Duong.D_DAN_DO) ?: return
        daDayDanDo = true
        danDoDaDay = ban
        val viec = if (ban == null) ref.delete() else ref.set(ban)
        viec.addOnFailureListener {
            // Quen ban vua nho, de lan prefs doi sau ghi lai.
            daDayDanDo = false
            Log.w(TAG, "day vo dan do hong: ${it.message}")
        }
    }

    /**
     * Ba truong ve man hinh tablet, hoac rong khi khong biet.
     *
     * Khong biet la luc dich vu canh app khong chay: no la thu duy nhat nhin thay
     * man hinh. Luc do bo ca ba truong ra chu khong ghi "khong mo app nao". Bang
     * dieu khien gap truong vang thi an dong do di, con ghi rong la noi sai.
     *
     * Doi app la mot lan ghi Firestore, va Ba Huy da chon cai gia do: so con dang
     * mo gi phai toi ngay, khong doi nhip tim muoi lam phut. Doi lien tuc thi
     * [day] gom lai, it nhat 1,2 giay moi mot luot.
     */
    private fun truocMat(): Map<String, Any> {
        if (!GuardAccessibilityService.dangChay) return emptyMap()
        val tm = GuardAccessibilityService.truocMat
        return mapOf(
            Duong.F_APP_TRUOC_MAT to tm.ten,
            Duong.F_APP_TRUOC_MAT_TU to tm.tu,
            Duong.F_MAN_HINH_SANG to tm.sang
        )
    }

    /**
     * Nhat ky hom nay, gop ca ngay vao mot document de khoi ton luot ghi.
     *
     * Moi so co dau rieng: mot dong moi trong nhat ky thi khong co ly do gi phai
     * ghi lai ca so hoi AI, va nguoc lai.
     */
    private fun dayNhatKy(context: Context) {
        val dong = DayLog.today(context).lines().filter { it.isNotBlank() }
        if (dong.isNotEmpty() && dong != nhatKyDaDay) {
            nhatKyDaDay = dong
            nha(context)?.collection(Duong.NHAT_KY)?.document(homNay())
                ?.set(mapOf(Duong.F_DONG to dong))
                ?.addOnFailureListener { Log.w(TAG, "day nhat ky hong: ${it.message}") }
        }

        val hoi = NhatKyAi.homNay(context).lines().filter { it.isNotBlank() }
        if (hoi.isNotEmpty() && hoi != hoiAiDaDay) {
            hoiAiDaDay = hoi
            nha(context)?.collection(Duong.HOI_AI)?.document(homNay())
                ?.set(mapOf(Duong.F_DONG to hoi))
                ?.addOnFailureListener { Log.w(TAG, "day so hoi AI hong: ${it.message}") }
        }
    }

    /**
     * Ghi ngay ban sao cau hinh dang chay, du no giong ban vua ghi.
     *
     * Dung sau mot lenh tu dien thoai (man Cai dat ben do dang mo, doi mot nhip la
     * Ba Huy bam lai), luc dong bo vua bat va luc ghep may (nha moi thi document cung
     * moi). Con lai de [dayCaiDatNeuDoi] lo.
     */
    fun dayCaiDat(context: Context) {
        caiDatDaDay = null
        dayCaiDatNeuDoi(context)
    }

    /**
     * Ghi ban sao cau hinh neu no khac ban vua ghi. Chay trong [dayThat], tuc la sau
     * moi lan prefs doi.
     *
     * Truoc day ban sao chi duoc ghi luc dong bo vua bat, luc ghep may, va sau mot
     * lenh tu dien thoai. Ba Huy sua ngay tren tablet, vi du tich them mot app vao
     * danh sach luon duoc dung, thi dien thoai van hien danh sach cu. Mo muc do ben
     * dien thoai roi bam Xong la gui nguyen danh sach cu ve, de len danh sach moi.
     *
     * So nguyen ban sao voi ban vua ghi: them mot muc vao ban sao la muc do tu duoc
     * dong bo, khoi phai nho sua them mot danh sach khoa o cho khac.
     *
     * Cac danh sach app sap xep lai truoc khi so: Set doc ra tu prefs khong hua giu
     * thu tu, ma List thi so ca thu tu.
     */
    private fun dayCaiDatNeuDoi(context: Context) {
        val prefs = Prefs.get(context)
        val ban = mapOf(
            "phutMacDinh" to prefs.grantMinutes,
            "gioNgu" to prefs.hardStopMinuteOfDay,
            "gioDay" to prefs.gioDayMinuteOfDay,
            "tranPhutMoiNgay" to prefs.tranPhutMoiNgay,
            "khoaCaiDat" to prefs.lockSystemSettings,
            "chamBangAi" to prefs.chamBangAi,
            "appChoPhep" to prefs.allowedPackages.sorted(),
            "appMoiLuc" to prefs.moiLucPackages.sorted(),
            "appChan" to prefs.blockedPackages.sorted(),
            "appAi" to prefs.aiPackages.sorted(),
            "gioiHanApp" to GioiHanApp.tatCa(context)
        )
        if (ban == caiDatDaDay) return
        val hop = hop(context, Duong.D_CAI_DAT) ?: return
        caiDatDaDay = ban
        hop.set(ban).addOnFailureListener {
            // Quen ban vua nho, de lan prefs doi sau ghi lai.
            caiDatDaDay = null
            Log.w(TAG, "day cai dat hong: ${it.message}")
        }
    }

    /**
     * Danh sach app dang cai, de ben dien thoai chon bang cach tich chuot.
     *
     * Truoc day muon chan mot app phai go dung ten goi kieu com.mojang.minecraftpe
     * vao Telegram. Go sai mot chu thi khong bao loi gi ca - app do don gian la
     * khong bao gio bi chan, va mai sau moi phat hien ra.
     *
     * Chi day khi danh sach that su doi: may chuc app moi lan mo may la ton mang
     * ma khong duoc gi.
     *
     * Goi luc dong bo vua bat, luc ghep may, va moi lan cai hay go app (xem
     * [ngheCaiApp]). So danh sach ten goi truoc, doc ten tung app sau: doc ten la
     * phan ton thoi gian, ma cap nhat app thi danh sach goi khong doi.
     */
    fun dayDanhSachApp(context: Context) {
        val pm = context.packageManager
        val y = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val tim = runCatching { pm.queryIntentActivities(y, 0) }.getOrDefault(emptyList())
        if (tim.isEmpty()) return

        val dau = tim.map { it.activityInfo.packageName }.distinct().sorted()
            .joinToString(",").hashCode()
        val sp = Prefs.get(context).raw()
        if (sp.getInt(K_DAU_DS_APP, 0) == dau) return

        val cac = runCatching {
            tim.map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                .distinctBy { it.first }
                .sortedBy { it.second.lowercase() }
        }.getOrDefault(emptyList())
        if (cac.isEmpty()) return

        hop(context, Duong.D_DANH_SACH_APP)?.set(
            mapOf("app" to cac.map { mapOf("goi" to it.first, "ten" to it.second) })
        )?.addOnSuccessListener { sp.edit().putInt(K_DAU_DS_APP, dau).apply() }
    }

    /**
     * Day so dung app sang Bang dieu khien. Xem [Duong.D_SU_DUNG].
     *
     * Chi goi khi dien thoai go [Lenh.PING], tuc la luc Ba Huy vua mo app ra. Khong
     * nghe so nay doi nhu phan trang thai: no doi vai phut mot lan suot luc con dung
     * may, ma so do chi can dung vao luc co nguoi nhin.
     *
     * Ghi de ca ban, khong so voi ban vua day nhu [dayThat]: khoang dang mo tinh den
     * luc day, nen con dang cam may thi lan nao cung khac. Con PING thi Bang dieu khien
     * da gian ra it nhat nua phut mot lan.
     */
    fun daySuDung(context: Context) {
        val hop = hop(context, Duong.D_SU_DUNG) ?: return
        val bayGio = System.currentTimeMillis()
        val cac = NhatKySuDung.banDeDay(context, dangMo(context, bayGio))
        hop.set(
            mapOf(
                Duong.F_DOAN to cac.map {
                    mapOf(Duong.F_GOI to it.goi, Duong.F_TU to it.tu, Duong.F_DEN to it.den)
                },
                Duong.F_APP to cac.map { it.goi }.distinct().map {
                    mapOf(Duong.F_GOI to it, Duong.F_TEN to NhatKySuDung.tenApp(context, it))
                },
                Duong.F_GIU_NGAY to NhatKySuDung.GIU_NGAY,
                Duong.F_DANG_GHI to GuardAccessibilityService.dangChay,
                Duong.F_CAP_NHAT_LUC to bayGio
            )
        ).addOnFailureListener { Log.w(TAG, "day so dung app hong: ${it.message}") }
    }

    /**
     * Cac app dang tren man hinh, thanh khoang tinh den [bayGio].
     *
     * Lay cung mot cho voi dong "dang mo gi" o [truocMat], va bo chinh app Nop bai vi
     * so ghi khong tinh no. Man hinh tat hay dich vu canh app khong chay thi khong co
     * khoang nao dang mo.
     */
    private fun dangMo(context: Context, bayGio: Long): List<NhatKySuDung.Doan> {
        if (!GuardAccessibilityService.dangChay) return emptyList()
        val tm = GuardAccessibilityService.truocMat
        if (!tm.sang) return emptyList()
        return tm.cac.filter { it.goi != context.packageName }
            .map { NhatKySuDung.Doan(it.goi, it.tu, bayGio) }
    }

    /**
     * Mot lan con nop bai. Goi ngay sau khi anh da len Telegram.
     *
     * [khai] la cac cau con khai kem de, xem [banKhai]. Gui kem de app Bang dieu khien
     * co de bai cho Claude ngay ca khi tablet khong tu cham. [danDo] la vo dan do con
     * soat ma lan nop nay dung, xem [banDanDo].
     */
    fun dayBaiMoi(
        context: Context,
        baiId: String,
        messageId: Long,
        anh: List<Anh>,
        khai: Map<String, Any>? = null,
        danDo: Map<String, Any>? = null
    ) {
        val noi = mutableMapOf<String, Any>(
            Duong.F_LUC to System.currentTimeMillis(),
            Duong.F_TRANG_THAI to "CHO",
            Duong.F_SO_PHUT to 0,
            Duong.F_MESSAGE_ID to messageId,
            Duong.F_ANH to anh.map {
                mapOf(Duong.F_FILE_ID to it.fileId, Duong.F_KHAU to it.khau)
            }
        )
        khai?.let { noi[Duong.F_KHAI] = it }
        danDo?.let { noi[Duong.F_DAN_DO] = it }
        nha(context)?.collection(Duong.BAI)?.document(baiId)?.set(noi)
            ?.addOnFailureListener { Log.w(TAG, "day bai hong: ${it.message}") }
        dayNgay()
    }

    /**
     * Cac cau con khai, kem de va dang bai lay tu ngan hang. Xem [Duong.F_KHAI].
     *
     * null la con khong khai theo sach: nop tu do, hay man khai bai khong co sach mon
     * do. Luc do Claude phai tu nhan ra cau va chep de tu anh.
     */
    fun banKhai(context: Context, pham: PhamVi?): Map<String, Any>? {
        if (pham == null || !pham.theoSach) return null
        val cac = KhoBai.get(context).cacCauTheoId(pham.cauIds)
        if (cac.isEmpty()) return null
        return mapOf(
            "tenNguon" to pham.tenNguon,
            "bai" to pham.bai,
            "mon" to pham.mon,
            "onTap" to pham.onTap,
            "cac" to cac.map {
                mapOf("ma" to it.ma, "cauId" to it.id, "de" to it.de, "dang" to it.dang)
            }
        )
    }

    /**
     * Vo dan do cua ngay, chep vao bai luc nop va vao hop/dando. Xem [Duong.F_DAN_DO].
     *
     * Chi dua len phan Claude can: ngay, bai phai lam, dong dan viec khac, va ma anh
     * trang vo de doi chieu. Dong nao con tich khac may thi khong dua: Ba Huy da thay
     * no trong tin vo dan do tren Telegram.
     *
     * Ban chi co anh thi cacBai rong ma chuaDoc la true: ben dien thoai khong duoc hieu
     * no la hom co khong giao bai tap.
     */
    fun banDanDo(vo: VoDanDo.DanDo): Map<String, Any> = buildMap {
        put("ngay", vo.ngay)
        put("cacBai", vo.cacBai)
        put("dongKhac", vo.dongKhac)
        vo.fileId?.let { put(Duong.F_FILE_ID, it) }
        put("chuaDoc", vo.chuaDoc)
        put("nguon", vo.nguon)
        put("chupLuc", vo.chupLuc)
    }

    /** Doi trang thai mot bai sau khi Ba Huy duyet hoac tu choi. */
    fun datTrangThaiBai(context: Context, baiId: String, trangThai: String, soPhut: Int = 0) {
        nha(context)?.collection(Duong.BAI)?.document(baiId)
            ?.update(mapOf(Duong.F_TRANG_THAI to trangThai, Duong.F_SO_PHUT to soPhut))
            ?.addOnFailureListener { Log.w(TAG, "doi trang thai bai hong: ${it.message}") }
    }

    /**
     * Doi trang thai mot bai, chi khi ban tren Firestore con ghi CHO.
     *
     * Dung cho bai khong con trong hang cho tren may. Luc do may khong biet bai da duoc
     * duyet ben Telegram hay chua, nen phai doc lai truoc. Ghi de len mot bai da duyet
     * thi con van giu gio, ma danh sach ben dien thoai lai ghi la khong duyet.
     */
    fun datTrangThaiBaiNeuDangCho(context: Context, baiId: String, trangThai: String) {
        val ref = nha(context)?.collection(Duong.BAI)?.document(baiId) ?: return
        ref.firestore.runTransaction { tr ->
            val d = tr.get(ref)
            if (d.exists() && d.getString(Duong.F_TRANG_THAI) == "CHO") {
                tr.update(ref, mapOf(Duong.F_TRANG_THAI to trangThai, Duong.F_SO_PHUT to 0))
            }
            null
        }.addOnFailureListener { Log.w(TAG, "doi trang thai bai hong: ${it.message}") }
    }

    /** Ket qua AI cham, gan vao dung bai do de Ba Huy nhin mot cai la quyet duoc. */
    fun dayChamBai(context: Context, baiId: String, cham: Map<String, Any>) {
        nha(context)?.collection(Duong.BAI)?.document(baiId)
            ?.update(Duong.F_CHAM, cham)
            ?.addOnFailureListener { Log.w(TAG, "day ban cham hong: ${it.message}") }
    }

    /**
     * Nghe cac lan nop gan day kem ban cham, moi nhat truoc.
     *
     * Cho man ket qua cua con, xem [vn.huytl.homeworkgate.ui.KetQuaActivity]. Doc dung
     * thu ma [dayBaiMoi] va [dayChamBai] ghi xuong, nen bai vua nop hien ra ngay va ban
     * cham tu dien vao khi may cham xong.
     *
     * [khi] nhan null khi khong doc duoc: may chua khai bao Firebase, hay Firestore bao
     * loi. Ben goi quyet co giu danh sach dang hien hay khong.
     */
    fun ngheBaiDaCham(
        context: Context,
        soLuong: Long,
        khi: (List<BaiDaCham>?) -> Unit
    ): ListenerRegistration? {
        if (!san(context)) {
            khi(null)
            return null
        }
        val nghe = nha(context)?.collection(Duong.BAI)
            ?.orderBy(Duong.F_LUC, Query.Direction.DESCENDING)
            ?.limit(soLuong)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) {
                    Log.w(TAG, "doc bai da cham hong: ${loi.message}")
                    khi(null)
                    return@addSnapshotListener
                }
                khi(snap?.documents.orEmpty().map { BaiDaCham.doc(it) })
            }
        if (nghe == null) khi(null)
        return nghe
    }

    /** Mot cau trong khung chat. Goi ca khi con nhan va khi ba nhan. */
    // ------------------------------------------------------------------ so cai

    /**
     * Day cac dong so cai vua ghi len Firestore.
     *
     * VI SAO. Go app la mat sach du lieu trong may - ke ca so cai, thu duy nhat chan
     * viec chup lai bai hom qua de lay gio lan nua. Co ban tren Firestore thi cai lai
     * may xong, ghep lai voi dien thoai Ba Huy la keo ve duoc nguyen ven.
     *
     * Moi lan cham mot document, ma document chu khong phai mot mang trong mot o:
     * mot nam hoc la hon nghin dong, ma mot document Firestore chi chua duoc 1MB.
     */
    fun daySoCai(context: Context, cac: List<TraLoi>) {
        if (cac.isEmpty()) return
        val goc = nha(context)?.collection(Duong.SO_CAI) ?: return
        cac.forEach { d ->
            goc.document(maDong(d)).set(
                mapOf(
                    "cauId" to d.cauId,
                    "mon" to d.mon,
                    "ma" to d.ma,
                    "de" to d.de,
                    "ketQua" to d.ketQua,
                    "baiLam" to d.baiLam,
                    "dongSai" to d.dongSai,
                    "onTap" to d.onTap,
                    "dung" to d.dung,
                    "phut" to d.phut,
                    "nhanXet" to d.nhanXet,
                    "luc" to d.luc
                )
            )
        }
    }

    /**
     * Keo toan bo so cai tu Firestore ve may, thay cho ban trong may.
     *
     * Goi sau khi vua ghep lai voi nha cu. [xong] nhan so dong keo ve, hay mot cau
     * tieng Viet noi vi sao khong keo duoc.
     */
    fun keoSoVe(context: Context, xong: (Int, String) -> Unit) {
        val goc = nha(context)?.collection(Duong.SO_CAI)
            ?: return xong(0, "Chưa nối được với Firestore.")
        goc.get()
            .addOnSuccessListener { snap ->
                val cac = snap.documents.mapNotNull { d ->
                    val cauId = d.getString("cauId") ?: return@mapNotNull null
                    TraLoi(
                        cauId = cauId,
                        mon = d.getString("mon").orEmpty(),
                        ma = d.getString("ma").orEmpty(),
                        de = d.getString("de").orEmpty(),
                        ketQua = d.getString("ketQua").orEmpty(),
                        baiLam = (d.get("baiLam") as? List<*>).orEmpty().map { it.toString() },
                        dongSai = (d.getLong("dongSai") ?: 0L).toInt(),
                        onTap = d.getBoolean("onTap") ?: false,
                        dung = d.getBoolean("dung") ?: false,
                        phut = (d.getLong("phut") ?: 0L).toInt(),
                        nhanXet = d.getString("nhanXet").orEmpty(),
                        luc = d.getLong("luc") ?: 0L
                    )
                }
                KhoBai.get(context).napSoCai(cac)
                DayLog.add(context, "Khôi phục sổ cái: ${cac.size} câu")
                xong(cac.size, "")
            }
            .addOnFailureListener { xong(0, noiLoi(it)) }
    }

    /** Ten document cho mot dong so cai. Dau gach cheo la ky tu Firestore khong cho. */
    private fun maDong(d: TraLoi): String =
        "${d.cauId}_${d.luc}".replace('/', '_')

    // ------------------------------------------------------- khoi phuc nha cu

    /** Ma nha dang dung. Hien ra cho Ba Huy ghi lai phong khi phai cai lai may. */
    fun maNhaHienTai(context: Context): String = maNha(context)

    /**
     * Xin vao lai mot nha da co - dung sau khi cai lai app.
     *
     * NGUOC CHIEU voi luong ghep doi cu. Binh thuong tablet lap nha va ket nap dien
     * thoai; nhung cai lai app la tablet mat het, ke ca tu cach "nguoi nha" tren
     * Firestore, nen lan nay TABLET la ben xin va DIEN THOAI la ben ket nap.
     *
     * Luat ben Firestore von da cho phep chieu nay: o /ghep/{uid} ai dang nhap cung
     * ghi duoc phan mang ten minh, con ket nap thi phai la nguoi nha. Chi co phan
     * ung dung la truoc day chua ai di chieu do.
     */
    fun xinVaoNha(
        context: Context,
        maNhaCu: String,
        maGhep: String,
        xong: (duoc: Boolean, loi: String) -> Unit
    ) {
        val ung = context.applicationContext
        if (!san(ung)) return xong(false, "Máy chưa nối được Firebase.")
        dangNhap(ung) { duoc, viSao ->
            if (!duoc) return@dangNhap xong(false, viSao)
            val uid = FirebaseAuth.getInstance(app(ung)!!).currentUser?.uid.orEmpty()
            if (uid.isEmpty()) return@dangNhap xong(false, "Chưa đăng nhập được.")

            val cua = db(ung)?.collection(Duong.NHA)?.document(maNhaCu)
                ?.collection(Duong.GHEP)?.document(uid)
                ?: return@dangNhap xong(false, "Chưa nối được Firestore.")

            cua.set(mapOf("ma" to maGhep, "luc" to System.currentTimeMillis()))
                .addOnSuccessListener { choKetNap(ung, maNhaCu, cua, xong) }
                .addOnFailureListener { xong(false, noiLoi(it)) }
        }
    }

    /**
     * Ngoi cho dien thoai Ba Huy ket nap.
     *
     * Chi ghi ma nha xuong may KHI DA duoc ket nap. Ghi som thi lan mo app sau tablet
     * tuong minh la nguoi nha cua mot nha khong cho minh vao, va moi thu deu tra ve
     * PERMISSION_DENIED ma khong ai hieu vi sao.
     */
    private fun choKetNap(
        context: Context,
        maNhaCu: String,
        cua: DocumentReference,
        xong: (Boolean, String) -> Unit
    ) {
        var nghe: ListenerRegistration? = null
        nghe = cua.addSnapshotListener { d, loi ->
            if (loi != null) {
                nghe?.remove()
                return@addSnapshotListener xong(false, noiLoi(loi))
            }
            when (d?.getString("trangThai")) {
                "OK" -> {
                    nghe?.remove()
                    fileThuong(context).edit().putString(K_NHA, maNhaCu).commit()
                    dungLai()
                    batDau(context)
                    xong(true, "")
                }
                "SAI" -> {
                    nghe?.remove()
                    xong(false, "Mã ghép sai hoặc đã hết hạn.")
                }
            }
        }
    }

    fun dayTin(context: Context, tin: ChatLine) {
        nha(context)?.collection(Duong.CHAT)?.add(
            mapOf(
                Duong.F_TU to if (tin.from == ChatFrom.CON) "CON" else "BA",
                Duong.F_CHU to tin.text,
                Duong.F_LUC to tin.at,
                Duong.F_DA_DOC to false
            )
        )?.addOnFailureListener { Log.w(TAG, "day tin hong: ${it.message}") }
    }

    /**
     * Cau tra loi cho lenh vua nhan, de ben go lenh biet no da an vao dau.
     *
     * Can cho nay vi rat nhieu lenh khong lam duoc ma khong phai loi mang: dang gio
     * ngu, het tran phut ngay, khong co bai nao dang cho. Khong noi lai thi man hinh
     * ben kia im lang, va nguoi go bam lai lan nua.
     *
     * Kem [Duong.F_AI] vi tu khi co ca may ba noi thi o nay co hai nguoi doc. Ba Huy
     * khong can thay cau tra loi cho cai nut ba vua bam, va nguoc lai - do la cau
     * cua viec minh vua lam hay cua nguoi khac, ben doc tu loc lay.
     */
    private fun traLoi(context: Context, chu: String, ai: String) {
        hop(context, Duong.D_TRANG_THAI)?.update(
            Duong.F_TRA_LOI,
            mapOf("chu" to chu, "luc" to System.currentTimeMillis(), Duong.F_AI to ai)
        )
    }

    // ----------------------------------------------------------- nghe lenh

    private fun batNgheLenh(context: Context) {
        ngheLenh?.remove()
        ngheLenh = nha(context)?.collection(Duong.LENH)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) {
                    Log.w(TAG, "nghe lenh hong: ${loi.message}")
                    return@addSnapshotListener
                }
                snap?.documents.orEmpty()
                    .sortedBy { it.getLong("tao") ?: 0L }
                    .forEach { d ->
                        if (!daLam.add(d.id)) return@forEach
                        val kq = runCatching { ThiHanhLenh.lam(context, d) }
                            .onFailure { Log.w(TAG, "lam lenh hong", it) }
                            .getOrNull()
                        // Xoa du lam duoc hay khong: khong xoa thi cai lenh do nam
                        // lai trong hang va duoc doc lai mai mai.
                        d.reference.delete()
                        // Chuoi rong la lenh co y khong tra loi gi - xem [Lenh.PING].
                        // Ghi vao o traLoi thi ben dien thoai moc len mot dong trong.
                        kq?.takeIf { it.isNotBlank() }
                            ?.let { traLoi(context, it, d.getString(Duong.F_AI) ?: Nguoi.BA_HUY) }
                        dayNgay()
                    }
                // Giu danh sach da lam gon lai. Lenh da xoa khoi Firestore thi khong
                // quay lai nua, nho nhieu cung vo ich.
                if (daLam.size > 200) daLam.clear()
            }
    }

    // ------------------------------------------------------- nghe viec nha

    /**
     * Nghe viec nha ba noi giao.
     *
     * Mot document chu khong phai mot hang doi, va khong xoa sau khi doc: day la
     * trang thai day du - ca danh sach viec lan viec nao da xong - nen ban moi
     * nhat luon la ban dung. Xem [ThiHanhViecNha].
     *
     * Nghe o day chu khong ghe qua moi phut nhu duong Telegram cu: ba bam "da xong"
     * trong luc con dang ngoi truoc man hinh bi khoa, va mot phut cho o dung cho do
     * la mot phut rat dai.
     */
    private fun batNgheViecNha(context: Context) {
        ngheViecNha?.remove()
        ngheViecNha = hop(context, Duong.D_VIEC_NHA)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) {
                    Log.w(TAG, "nghe viec nha hong: ${loi.message}")
                    return@addSnapshotListener
                }
                runCatching { ThiHanhViecNha.lam(context, snap) }
                    .onFailure { Log.w(TAG, "lam viec nha hong", it) }
            }
    }

    /** Lenh go tu lau qua thi bo. Dung chung cho ca [ThiHanhLenh]. */
    fun quaCu(taoLuc: Long): Boolean =
        taoLuc > 0L && System.currentTimeMillis() - taoLuc > Duong.QUA_CU_MS

    // ----------------------------------------------------------- ghep doi

    /**
     * Lam mot ma ghep doi moi va tra ve ca hai dong de hien len man hinh.
     *
     * Ma nha khong doi bao gio; ma sau so thi het han sau muoi phut, nen anh chup
     * man hinh tablet tu tuan truoc khong dung lai duoc.
     */
    fun maGhepMoi(
        context: Context,
        xong: (maNha: String, maGhep: String, loi: String) -> Unit
    ) {
        dangNhap(context) { duoc, viSao ->
            if (!duoc) return@dangNhap xong("", "", viSao)
            lapNhaNeuChua(context) { co ->
                if (!co) return@lapNhaNeuChua xong("", "", "Chưa lập được nhà trên Firestore.")
                datMaGhep(context, xong)
            }
        }
    }

    private fun datMaGhep(
        context: Context,
        xong: (maNha: String, maGhep: String, loi: String) -> Unit
    ) {
        val ma = "%06d".format(SecureRandom().nextInt(1_000_000))
        nha(context)?.update(
            mapOf(
                Duong.F_MA_GHEP to ma,
                Duong.F_MA_GHEP_HET_HAN to System.currentTimeMillis() + MA_GHEP_SONG_MS,
                Duong.F_TEN_CON to context.getString(R.string.child_name)
            )
        )?.addOnSuccessListener {
            batNgheGhep(context)
            xong(maNha(context), ma, "")
        }?.addOnFailureListener {
            Log.w(TAG, "dat ma ghep hong", it)
            xong("", "", noiLoi(it))
        }
    }

    /**
     * Nghe loi xin vao nha.
     *
     * Chi tablet ket nap duoc: luat ben Firestore cho may la ghi duoc dung mot cho
     * la o mang ten uid cua chinh no, khong doc duoc gi khac cho den khi uid do nam
     * trong danh sach nguoi nha.
     */
    private fun batNgheGhep(context: Context) {
        ngheGhep?.remove()
        ngheGhep = nha(context)?.collection(Duong.GHEP)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) return@addSnapshotListener
                val d = nha(context) ?: return@addSnapshotListener
                snap?.documents.orEmpty().forEach { xin ->
                    if (xin.getString("trangThai") != null) return@forEach
                    d.get().addOnSuccessListener { nhaDoc ->
                        val ma = nhaDoc.getString(Duong.F_MA_GHEP).orEmpty()
                        val han = nhaDoc.getLong(Duong.F_MA_GHEP_HET_HAN) ?: 0L
                        val dung = ma.isNotEmpty() &&
                            xin.getString("ma") == ma &&
                            System.currentTimeMillis() < han
                        if (dung) {
                            /*
                             * May ba noi vao danh sach phu, khong vao danh sach
                             * nguoi nha day du.
                             *
                             * Khac nhau o cho luat ben Firestore: nguoi nha day du
                             * go duoc moi lenh, ke ca khoa may va tat quan tri thiet
                             * bi; danh sach phu thi chi cho gio va giao viec nha.
                             * Nhet nham may ba vao uids la ba bam nham mot cai la
                             * tablet khoa cung, ma khong co gi chan lai.
                             */
                            val phu = xin.getString(Duong.F_AI) == Nguoi.BA_NOI
                            d.update(
                                if (phu) Duong.F_UIDS_PHU else Duong.F_UIDS,
                                FieldValue.arrayUnion(xin.id)
                            )
                            xin.reference.update("trangThai", "OK")
                            // Ma dung roi thi thu hoi ngay, mot ma mot lan.
                            d.update(Duong.F_MA_GHEP, "", Duong.F_MA_GHEP_HET_HAN, 0L)
                            DayLog.add(
                                context,
                                if (phu) "Đã nối máy bà nội vào"
                                else "Đã nối điện thoại ba Huy vào bảng điều khiển"
                            )
                            // Tu day tro di lenh co duong nhanh de di, nen ben
                            // Telegram thoi nam cho lien tuc. Xem [duongNhanhSong].
                            if (!phu) Prefs.get(context).daNoiDienThoaiBa = true
                            dayNgay()
                            dayCaiDat(context)
                            dayDanhSachApp(context)
                        } else {
                            xin.reference.update("trangThai", "SAI")
                        }
                    }
                }
            }
    }

    // ------------------------------------------------------------- rieng tu

    private fun app(context: Context): FirebaseApp? =
        runCatching { FirebaseApp.initializeApp(context.applicationContext) }.getOrNull()
            ?: runCatching { FirebaseApp.getInstance() }.getOrNull()

    private fun db(context: Context): FirebaseFirestore? =
        app(context)?.let { FirebaseFirestore.getInstance(it) }

    private fun dangNhap(context: Context, xong: (Boolean, String) -> Unit) {
        val a = app(context)?.let { FirebaseAuth.getInstance(it) }
            ?: return xong(false, "Bản app này chưa nối Firebase.")
        if (a.currentUser != null) return xong(true, "")
        a.signInAnonymously()
            .addOnSuccessListener { xong(true, "") }
            .addOnFailureListener {
                Log.w(TAG, "dang nhap an danh hong: ${it.message}")
                xong(false, noiLoi(it))
            }
    }

    /**
     * Doi loi cua Firebase sang cau chi duoc viec phai lam.
     *
     * Cau goc ("An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]") doc
     * xong khong ai biet phai bam vao dau, trong khi no chi co mot nghia duy nhat:
     * chua bat dang nhap an danh trong console Firebase.
     */
    private fun noiLoi(loi: Exception): String {
        val chu = loi.message.orEmpty()
        return when {
            chu.contains("CONFIGURATION_NOT_FOUND", true) ->
                "Chưa bật đăng nhập ẩn danh: vào console Firebase → Authentication → " +
                    "Sign-in method → Anonymous → Enable."
            chu.contains("PERMISSION_DENIED", true) ->
                "Firestore từ chối. Kiểm tra đã dán luật trong firestore.rules chưa."
            chu.contains("NOT_FOUND", true) ->
                "Chưa tạo Firestore Database trong project Firebase."
            chu.contains("UNAVAILABLE", true) || chu.contains("network", true) ->
                "Máy không vào được mạng."
            else -> "Firebase báo: $chu"
        }
    }

    /**
     * Ma nha cua may nay. Chua co thi tu sinh mot ma khong ai doan duoc.
     *
     * VI SAO KHONG NAM TRONG [Prefs]: Prefs la EncryptedSharedPreferences, khoa giu
     * trong Android Keystore. Cho do rat hop cho token bot va ma PIN, nhung no co
     * mot canh hong ma may cho khac khong co: khoa trong Keystore mat hieu luc -
     * khoi phuc may, doi man hinh khoa, hay mot ban ROM vua cap nhat - la ca file
     * doc khong ra, va app mo len voi mot kho rong tron.
     *
     * Voi token bot thi mat la biet ngay: Telegram im, Ba Huy nhan ra trong buoi
     * toi. Voi ma nha thi KHONG: tablet lang le lap mot cai nha moi, ghi trang thai
     * vao do, con dien thoai van ngoi doc cai nha cu - hai ben deu chay, deu khong
     * bao loi, ma khong ben nao thay ben nao nua. Dung mot lan nhu the trong luc
     * thu tren may ao la du de khong bao gio de no o cho de mat.
     *
     * Nen ma nha nam o mot file thuong. No khong phai bi mat: biet ma nha cung
     * khong doc duoc gi, vi luat ben Firestore chi cho nhung uid da duoc ket nap.
     *
     * Van doc [Prefs] truoc mot lan de chuyen nha cu sang - may da chay ban truoc
     * thi ma nam trong do.
     */
    private fun maNha(context: Context): String {
        val ngoai = fileThuong(context)
        ngoai.getString(K_NHA, null)?.takeIf { it.isNotBlank() }?.let { return it }

        // Ban truoc giu ma trong Prefs. Chuyen sang file thuong roi dung tiep, khong
        // sinh ma moi: sinh moi la mat lien lac voi dien thoai da ghep.
        val cu = runCatching { Prefs.get(context).raw().getString(K_NHA, null) }.getOrNull()
        if (!cu.isNullOrBlank()) {
            ngoai.edit().putString(K_NHA, cu).commit()
            return cu
        }

        val chu = "abcdefghijkmnpqrstuvwxyz23456789"
        val r = SecureRandom()
        val ma = (1..20).map { chu[r.nextInt(chu.length)] }.joinToString("")
        ngoai.edit().putString(K_NHA, ma).commit()
        return ma
    }

    /**
     * File thuong, khong ma hoa, chi giu ma nha.
     *
     * Tach khoi file cua Prefs chu khong ghi chung: chung file thi mot ngay nao do
     * ai do them mot thu that su can giau vao day.
     */
    private fun fileThuong(context: Context) =
        context.applicationContext.getSharedPreferences("dongbo_nha", Context.MODE_PRIVATE)

    /**
     * Lap nha tren Firestore neu chua co.
     *
     * Luat ben do chi cho tao khi nguoi tao tu ghi dung mot minh vao danh sach, nen
     * cho nay phai set chinh xac nhu the. Goi lai lan hai khong sao: da co nha thi
     * merge khong doi gi ca.
     */
    private fun lapNhaNeuChua(context: Context, xong: (Boolean) -> Unit) {
        val uid = app(context)?.let { FirebaseAuth.getInstance(it).currentUser?.uid }.orEmpty()
        if (uid.isEmpty()) return xong(false)
        val d = nha(context) ?: return xong(false)
        d.get()
            .addOnSuccessListener { co ->
                if (co.exists()) return@addOnSuccessListener xong(true)
                // Ghi dung khuon luat doi: nguoi lap tu ghi dung mot minh vao danh
                // sach. Thua mot uid la Firestore tu choi ca lan ghi.
                d.set(
                    mapOf(
                        Duong.F_UIDS to listOf(uid),
                        Duong.F_TEN_CON to context.getString(R.string.child_name),
                        "tao" to System.currentTimeMillis()
                    )
                ).addOnSuccessListener { xong(true) }
                    .addOnFailureListener {
                        Log.w(TAG, "lap nha hong: ${it.message}")
                        xong(false)
                    }
            }
            .addOnFailureListener {
                Log.w(TAG, "khong doc duoc nha: ${it.message}")
                xong(false)
            }
    }

    internal fun nha(context: Context): DocumentReference? =
        db(context)?.collection(Duong.NHA)?.document(maNha(context))

    private fun hop(context: Context, ten: String): DocumentReference? =
        nha(context)?.collection(Duong.HOP)?.document(ten)

    private fun pinMay(context: Context): Int {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return -1
        val muc = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val day = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (muc < 0 || day <= 0) -1 else muc * 100 / day
    }

    private fun dangSac(context: Context): Boolean {
        val i: Intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return false
        return i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }

    private fun homNay(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Mot tam anh da gui len Telegram, de dien thoai tai lai bang file_id. */
    data class Anh(val fileId: String, val khau: String)

    private const val K_NHA = "dongbo_ma_nha"
    private const val K_DAU_DS_APP = "dongbo_dau_ds_app"
}
