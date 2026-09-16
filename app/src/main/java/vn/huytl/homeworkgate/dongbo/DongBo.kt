package vn.huytl.homeworkgate.dongbo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.homeworkgate.BuildConfig
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.ChatFrom
import vn.huytl.homeworkgate.data.ChatLine
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.NhatKyAi
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.Permissions
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

    /** Lenh cu hon khoang nay thi bo, y het LENH_QUA_CU_MS ben ApprovalService. */
    private const val LENH_QUA_CU_MS = 30 * 60_000L

    /** Ma ghep doi song bao lau. Du de cam dien thoai len go, khong du de quen. */
    private const val MA_GHEP_SONG_MS = 10 * 60_000L

    /** Gom nhieu thay doi lien nhau thanh mot lan ghi. */
    private const val DOI_GOM_MS = 1200L

    /** Nhip day lai du khong co gi doi, de dien thoai biet tablet con song. */
    private const val NHIP_TIM_MS = 15 * 60_000L

    private val tay = Handler(Looper.getMainLooper())
    private var ct: Context? = null
    private var dangChay = false

    private var ngheLenh: ListenerRegistration? = null
    private var ngheGhep: ListenerRegistration? = null

    /** Cac lenh da lam trong lan chay nay, de khong lam hai lan neu xoa hut. */
    private val daLam = mutableSetOf<String>()

    /**
     * Giu tham chieu manh toi lang nghe prefs.
     *
     * SharedPreferences chi giu tham chieu yeu toi lang nghe. De no lam bien cuc bo
     * thi bo don rac nuot mat sau vai phut, va tu do tablet im lang - khong loi,
     * khong dau hieu gi, chi la dien thoai khong bao gio cap nhat nua.
     */
    private val ngheDoi = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> day() }

    private val nhipTim = object : Runnable {
        override fun run() {
            dayNgay()
            tay.postDelayed(this, NHIP_TIM_MS)
        }
    }

    // --------------------------------------------------------------- vong doi

    /** Da khai bao Firebase chua. Thieu google-services.json thi ca lop nay nam im. */
    fun san(context: Context): Boolean = app(context) != null

    fun batDau(context: Context) {
        if (dangChay) return
        val ung = context.applicationContext
        if (!san(ung)) {
            Log.i(TAG, "chua co google-services.json, khong noi Bang dieu khien")
            return
        }
        ct = ung
        dangChay = true

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
                batNgheGhep(ung)
                Prefs.get(ung).raw().registerOnSharedPreferenceChangeListener(ngheDoi)
                tay.post(nhipTim)
                dayCaiDat(ung)
                dayDanhSachApp(ung)
            }
        }
    }

    fun dungLai() {
        val ung = ct
        dangChay = false
        tay.removeCallbacksAndMessages(null)
        ngheLenh?.remove(); ngheLenh = null
        ngheGhep?.remove(); ngheGhep = null
        if (ung != null) {
            runCatching { Prefs.get(ung).raw().unregisterOnSharedPreferenceChangeListener(ngheDoi) }
        }
    }

    // ------------------------------------------------------------ day di len

    /** Day trang thai, gom cac thay doi lien nhau thanh mot lan ghi. */
    fun day() {
        tay.removeCallbacks(dayThat)
        tay.postDelayed(dayThat, DOI_GOM_MS)
    }

    fun dayNgay() {
        tay.removeCallbacks(dayThat)
        tay.post(dayThat)
    }

    private val dayThat = Runnable {
        val context = ct ?: return@Runnable
        val hop = hop(context, Duong.D_TRANG_THAI) ?: return@Runnable
        val gate = GateStore(context)
        val prefs = Prefs.get(context)
        val bayGio = System.currentTimeMillis()
        val conLai = gate.remainingMs()

        val noi = mapOf(
            Duong.F_CONG to gate.state.name,
            // Moc ket thuc theo gio that, khong phai so phut con lai: dien thoai tu
            // tru dan tren may no, nen cho nay khong phai ghi moi giay mot lan.
            Duong.F_KET_THUC_LUC to if (gate.state == GateState.ACTIVE) bayGio + conLai else 0L,
            Duong.F_CON_LAI_MS to conLai,
            Duong.F_PHUT_DA_DUYET to gate.phutDaDuyetHomNay(),
            Duong.F_PHUT_CON_LAI to gate.phutConLaiHomNay(),
            Duong.F_SO_BAI_CHO to gate.soBaiDangCho(),
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
        )
        hop.set(noi).addOnFailureListener { Log.w(TAG, "day trang thai hong: ${it.message}") }

        dayNhatKy(context)
    }

    /** Nhat ky hom nay, gop ca ngay vao mot document de khoi ton luot ghi. */
    private fun dayNhatKy(context: Context) {
        val dong = DayLog.today(context).lines().filter { it.isNotBlank() }
        if (dong.isEmpty()) return
        nha(context)?.collection(Duong.NHAT_KY)?.document(homNay())
            ?.set(mapOf(Duong.F_DONG to dong))
            ?.addOnFailureListener { Log.w(TAG, "day nhat ky hong: ${it.message}") }

        val hoi = NhatKyAi.homNay(context).lines().filter { it.isNotBlank() }
        if (hoi.isNotEmpty()) {
            nha(context)?.collection(Duong.HOI_AI)?.document(homNay())
                ?.set(mapOf(Duong.F_DONG to hoi))
        }
    }

    /** Ban sao cau hinh dang chay, de man Cai dat ben dien thoai hien so that. */
    fun dayCaiDat(context: Context) {
        val prefs = Prefs.get(context)
        hop(context, Duong.D_CAI_DAT)?.set(
            mapOf(
                "phutMacDinh" to prefs.grantMinutes,
                "gioNgu" to prefs.hardStopMinuteOfDay,
                "gioDay" to prefs.gioDayMinuteOfDay,
                "tranPhutMoiNgay" to prefs.tranPhutMoiNgay,
                "khoaCaiDat" to prefs.lockSystemSettings,
                "appChoPhep" to prefs.allowedPackages.toList(),
                "appChan" to prefs.blockedPackages.toList(),
                "appAi" to prefs.aiPackages.toList(),
                "gioiHanApp" to GioiHanApp.tatCa(context)
            )
        )
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
     */
    fun dayDanhSachApp(context: Context) {
        val pm = context.packageManager
        val y = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val cac = runCatching {
            pm.queryIntentActivities(y, 0)
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                .distinctBy { it.first }
                .sortedBy { it.second.lowercase() }
        }.getOrDefault(emptyList())
        if (cac.isEmpty()) return

        val dau = cac.joinToString(",") { it.first }.hashCode()
        val sp = Prefs.get(context).raw()
        if (sp.getInt(K_DAU_DS_APP, 0) == dau) return

        hop(context, Duong.D_DANH_SACH_APP)?.set(
            mapOf("app" to cac.map { mapOf("goi" to it.first, "ten" to it.second) })
        )?.addOnSuccessListener { sp.edit().putInt(K_DAU_DS_APP, dau).apply() }
    }

    /** Mot lan con nop bai. Goi ngay sau khi anh da len Telegram. */
    fun dayBaiMoi(context: Context, baiId: String, messageId: Long, anh: List<Anh>) {
        nha(context)?.collection(Duong.BAI)?.document(baiId)?.set(
            mapOf(
                Duong.F_LUC to System.currentTimeMillis(),
                Duong.F_TRANG_THAI to "CHO",
                Duong.F_SO_PHUT to 0,
                Duong.F_MESSAGE_ID to messageId,
                Duong.F_ANH to anh.map {
                    mapOf(Duong.F_FILE_ID to it.fileId, Duong.F_KHAU to it.khau)
                }
            )
        )?.addOnFailureListener { Log.w(TAG, "day bai hong: ${it.message}") }
        dayNgay()
    }

    /** Doi trang thai mot bai sau khi Ba Huy duyet hoac tu choi. */
    fun datTrangThaiBai(context: Context, baiId: String, trangThai: String, soPhut: Int = 0) {
        nha(context)?.collection(Duong.BAI)?.document(baiId)
            ?.update(mapOf(Duong.F_TRANG_THAI to trangThai, Duong.F_SO_PHUT to soPhut))
            ?.addOnFailureListener { Log.w(TAG, "doi trang thai bai hong: ${it.message}") }
    }

    /** Ket qua AI cham, gan vao dung bai do de Ba Huy nhin mot cai la quyet duoc. */
    fun dayChamBai(context: Context, baiId: String, cham: Map<String, Any>) {
        nha(context)?.collection(Duong.BAI)?.document(baiId)
            ?.update(Duong.F_CHAM, cham)
            ?.addOnFailureListener { Log.w(TAG, "day ban cham hong: ${it.message}") }
    }

    /** Mot cau trong khung chat. Goi ca khi con nhan va khi ba nhan. */
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
     * Cau tra loi cho lenh vua nhan, de ben dien thoai biet no da an vao dau.
     *
     * Can cho nay vi rat nhieu lenh khong lam duoc ma khong phai loi mang: dang gio
     * ngu, het tran phut ngay, khong co bai nao dang cho. Khong noi lai thi man hinh
     * ben kia im lang, va Ba Huy bam lai lan nua.
     */
    private fun traLoi(context: Context, chu: String) {
        hop(context, Duong.D_TRANG_THAI)?.update(
            Duong.F_TRA_LOI, mapOf("chu" to chu, "luc" to System.currentTimeMillis())
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
                        kq?.let { traLoi(context, it) }
                        dayNgay()
                    }
                // Giu danh sach da lam gon lai. Lenh da xoa khoi Firestore thi khong
                // quay lai nua, nho nhieu cung vo ich.
                if (daLam.size > 200) daLam.clear()
            }
    }

    /** Lenh go tu lau qua thi bo. Dung chung cho ca [ThiHanhLenh]. */
    fun quaCu(taoLuc: Long): Boolean =
        taoLuc > 0L && System.currentTimeMillis() - taoLuc > LENH_QUA_CU_MS

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
                            d.update(Duong.F_UIDS, FieldValue.arrayUnion(xin.id))
                            xin.reference.update("trangThai", "OK")
                            // Ma dung roi thi thu hoi ngay, mot ma mot lan.
                            d.update(Duong.F_MA_GHEP, "", Duong.F_MA_GHEP_HET_HAN, 0L)
                            DayLog.add(context, "Đã nối điện thoại Ba Huy vào bảng điều khiển")
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
