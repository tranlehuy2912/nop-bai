package vn.huytl.homeworkgate.ui

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.CauSo
import vn.huytl.homeworkgate.data.Mang
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.databinding.ActivityHomeBinding
import vn.huytl.homeworkgate.guard.CountdownOverlay
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.PhienQuanLy
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier
import vn.huytl.homeworkgate.telegram.TelegramClient
import java.util.Calendar

/**
 * Man hinh Le Hoa nhin thay. Chi mot nut to.
 *
 * Duong vao phan cua ba khong nam o day nua ma o man chon nguoi dung, de tren
 * man nay khong con nut nao de bam thu.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var prefs: Prefs
    private lateinit var gate: GateStore

    /**
     * Vong ve lai dong ho, chi song trong luc dang choi that.
     *
     * Ngoai luc do man hinh nay khong co gi tu chay, nen khong can vong nao: cai gi
     * doi thi [ngheDoi] bao.
     */
    private var lamTuoi: Job? = null

    private val tay = Handler(Looper.getMainLooper())

    private val veLai = Runnable { render() }

    /**
     * Ve lai khi co thu gi that su doi, thay cho viec cu muoi giay hoi mot lan.
     *
     * Moi trang thai cua app deu nam trong prefs - so phut, hang bai cho, tin nhan
     * cua Ba Huy - nen nghe file do doi la biet dung luc phai ve lai. Ban cu goi
     * gate.tick() moi muoi giay chi de phat hien nhung thu nay, ma moi lan tick la
     * mot lan ghi xuong dia keo theo mot luot day len Firestore, trong khi may thi
     * dang khoa va khong co gi xay ra ca.
     *
     * Hoan mot nhip ngan roi moi ve: mot lan cap gio ghi vai khoa lien nhau, gom
     * lai thanh mot lan ve.
     */
    private val ngheDoi = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        tay.removeCallbacks(veLai)
        tay.postDelayed(veLai, 300L)
    }

    /**
     * Tu Android 13 thong bao phai xin. Khong co quyen nay thi thong bao dem
     * nguoc khong hien, con khong biet con bao nhieu phut va bat ngo bi cat.
     */
    private val requestNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs.get(this)
        gate = GateStore(this)
        applySystemBarPadding()

        binding.btnSubmit.setOnClickListener { onSubmit() }
        binding.btnChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        binding.btnParent.setOnClickListener { moChoBaHuy() }
        binding.btnNopThem.setOnClickListener { onSubmit(nopThem = true) }
        binding.btnSoan.setOnClickListener { moManSoan() }
        binding.btnLich.setOnClickListener {
            startActivity(Intent(this, LichActivity::class.java))
        }
        binding.btnTin.setOnClickListener {
            startActivity(Intent(this, TinActivity::class.java))
        }
        binding.btnTienBo.setOnClickListener {
            startActivity(Intent(this, TienBoActivity::class.java))
        }
        binding.btnHocThuoc.setOnClickListener {
            startActivity(Intent(this, HocThuocActivity::class.java))
        }

        askNotificationPermission()

        if (!prefs.isConfigured) {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    /**
     * Tren tablet, thanh taskbar cua he thong nam de len day man hinh va che mat
     * phan duoi nut Nop bai. Con bam vao do la trung taskbar chu khong trung nut.
     * Chua khoang cho thanh do bang chinh so do he thong bao, thay vi doan mot
     * con so co dinh vi moi doi may mot khac.
     */
    private fun applySystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onResume() {
        super.onResume()
        ApprovalService.ensureRunning(this)
        // Mot lan tick luc vao man hinh: phieu duyet cua hom qua, hay bai cho da qua
        // ngay, phai duoc don ngay chu khong doi den luc co su kien.
        gate.tick()
        Prefs.get(this).raw().registerOnSharedPreferenceChangeListener(ngheDoi)
        render()
    }

    /**
     * Bat vong mot giay khi dang choi, tat di khi thoi.
     *
     * Goi tu cuoi [render] nen khong phai nho bat o tung cho: trang thai doi kieu gi
     * thi cung di qua day.
     */
    private fun nhipDongHo() {
        if (gate.isOpen()) {
            if (lamTuoi != null) return
            lamTuoi = lifecycleScope.launch {
                while (true) {
                    delay(1_000L)
                    gate.tick()
                    render()
                    if (!gate.isOpen()) break
                }
                lamTuoi = null
            }
        } else {
            lamTuoi?.cancel()
            lamTuoi = null
        }
    }

    override fun onPause() {
        runCatching {
            Prefs.get(this).raw().unregisterOnSharedPreferenceChangeListener(ngheDoi)
        }
        tay.removeCallbacks(veLai)
        lamTuoi?.cancel()
        lamTuoi = null
        super.onPause()
    }

    private fun render() {
        val baDangDung = ParentMode.isActive(this)
        // So bai dang xep hang cho Ba Huy duyet. Co the nhieu hon mot: con lam xong
        // dot nay nop tiep dot khac ma khong phai cho duyet xong dot truoc.
        val soBaiCho = gate.soBaiDangCho()
        // Viec ba noi giao, con chua lam xong. Man chan dang che ca may, nhung no
        // nhuong cho chinh app nay - nen day la cho duy nhat con doc duoc con phai
        // lam gi.
        val conViec = ViecNha.dangTreo(this)?.chuaXong.orEmpty()
        when {
            baDangDung -> {
                doiMat(R.drawable.ic_mat_mo_khoa, R.color.parent_tint, R.color.parent_soft)
                binding.txtBadge.text = "Máy đang mở"
                binding.txtState.text = getString(R.string.parent_name_cap)
                binding.txtDetail.text = "Đang dùng máy — ${ParentMode.moTa(this)}"
            }
            conViec.isNotEmpty() -> {
                doiMat(R.drawable.ic_mat_viec_nha, R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = "Bà nội giao việc"
                binding.txtState.text = if (conViec.size == 1) {
                    conViec.first().ten
                } else {
                    "Còn ${conViec.size} việc"
                }
                binding.txtDetail.text = conViec.joinToString(", ") { it.ten } +
                    ".\nLàm xong nhờ bà bấm trên điện thoại của bà."
            }
            gate.state == GateState.PENDING && gate.grantedMinutes > 0 -> {
                // Nop them bai de cong don, ma trong tay van con phieu cu.
                doiMat(R.drawable.ic_mat_cho, R.color.brand, R.color.brand_soft)
                binding.txtBadge.text =
                    if (soBaiCho > 1) "Đã gửi thêm $soBaiCho bài" else "Đã gửi thêm bài"
                binding.txtState.text = "${gate.grantedMinutes} phút"
                binding.txtDetail.text =
                    "Không phải đợi duyệt — chơi được rồi, duyệt xong máy cộng thêm."
            }
            gate.state == GateState.PENDING -> {
                doiMat(R.drawable.ic_mat_cho, R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = if (soBaiCho > 1) "Đã gửi $soBaiCho bài" else "Đã gửi bài"
                binding.txtState.text = "Chờ ba Huy duyệt"
                // Dong to o tren da noi "Cho Ba Huy duyet" roi. Dong nay de danh cho
                // cai con khong tu biet: trong luc cho van lam bai tiep duoc.
                binding.txtDetail.text = if (soBaiCho > 1) {
                    "Duyệt từng bài một, bài nào xong là cộng giờ bài đó."
                } else {
                    "Trong lúc chờ, làm thêm bài nộp tiếp cũng được."
                }
            }
            gate.state == GateState.GRANTED -> {
                doiMat(R.drawable.ic_mat_da_duyet, R.color.brand, R.color.brand_soft)
                // Bo trong la Ba Huy duyet, duong di cua gan het moi phieu gio.
                binding.txtBadge.text = gate.nhanCho
                    .ifEmpty { "${getString(R.string.parent_name_cap)} đã duyệt" }
                binding.txtState.text = "${gate.grantedMinutes} phút"
                // Nut to ngay duoi da ghi "Bat dau choi", nhac lai la thua. Giu dung
                // cai con khong tu doan duoc: dong ho chua chay, khong bam som cung
                // khong mat gi.
                binding.txtDetail.text = "Chưa tính giờ đâu — sẵn sàng chơi mới bấm."
            }
            gate.state == GateState.PAUSED -> {
                doiMat(R.drawable.ic_mat_tam_dung, R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = "Đang tạm dừng"
                // Den tung giay, giong het luc dang choi: so nay la so se chay tiep
                // khi bam "Choi tiep", nen hien khac di la sau do thay hut mat may
                // chuc giay.
                binding.txtState.text = CountdownOverlay.dinhDang(gate.pausedMs())
                binding.txtDetail.text = "Giờ giữ nguyên, không chạy tiếp." +
                    if (soBaiCho > 0) " Còn $soBaiCho bài chờ duyệt." else ""
            }
            gate.isOpen() -> {
                doiMat(R.drawable.ic_mat_dang_choi, R.color.ok, R.color.ok_soft)
                binding.txtBadge.text = "Đang được chơi"
                // Dem den tung giay: nhin mot cai la biet con bao lau, khong phai
                // doan giua "con 1 phut" va "con 59 giay".
                binding.txtState.text = CountdownOverlay.dinhDang(gate.remainingMs())
                // Nop them bai trong luc dang choi thi dong ho van chay. Ghi ro o day
                // de con biet bai da gui di roi, khong phai chup lai.
                binding.txtDetail.text = if (soBaiCho > 0) {
                    "Đã gửi $soBaiCho bài, duyệt xong là cộng thêm."
                } else {
                    "Hết giờ máy tự khoá."
                }
            }
            else -> {
                doiMat(R.drawable.ic_mat_khoa, R.color.locked, R.color.locked_soft)
                binding.txtBadge.text = "Đang khoá"
                binding.txtState.text = "Chưa nộp bài"
                val con = gate.phutConLaiHomNay()
                val daCo = gate.phutDaDuyetHomNay()
                binding.txtDetail.text = when {
                    con <= 0 -> "Hôm nay đủ giờ chơi rồi, mai nộp bài tiếp nhé"
                    daCo > 0 -> "Hôm nay đã được $daCo phút, làm bài thêm được tối đa $con phút nữa"
                    else -> "Làm bài xong được tối đa $con phút chơi hôm nay"
                }
            }
        }

        // Het tran trong ngay thi nut nop bai khong lam duoc gi ngoai hien mot cau
        // toast. De no sang xanh nhu binh thuong la moi con bam di bam lai roi tuong
        // may hong.
        val hetLuot = gate.state == GateState.LOCKED && gate.phutConLaiHomNay() <= 0
        // Con viec nha thi khong bam chơi duoc: man chan van che ca may, bam vao
        // chi ton mot cai bam ma khong thay gi doi. Nop bai thi van cho - bai co the
        // da lam xong tu truoc, va giu lai cung khong duoc gi.
        val nutLaChoi = gate.state == GateState.GRANTED || gate.state == GateState.PAUSED ||
            gate.isOpen() || (gate.state == GateState.PENDING && gate.grantedMinutes > 0)
        val vuongViec = conViec.isNotEmpty() && nutLaChoi
        binding.btnSubmit.isEnabled = !baDangDung && !hetLuot && !vuongViec
        // Cung mot nut to, doi chu theo viec dang can lam, de man hinh khong bao
        // gio co hai nut to cung luc.
        binding.btnSubmit.text = when {
            vuongViec -> "Làm xong việc nhà đã"
            hetLuot -> "Hôm nay đủ giờ chơi rồi"
            gate.state == GateState.GRANTED -> "Bắt đầu chơi"
            gate.state == GateState.PENDING && gate.grantedMinutes > 0 -> "Bắt đầu chơi"
            gate.state == GateState.PENDING -> "Huỷ, nộp lại bài khác"
            gate.state == GateState.PAUSED -> "Chơi tiếp"
            gate.isOpen() -> "Tạm dừng, giữ giờ lại"
            else -> getString(R.string.home_submit)
        }

        // Nut phu: nop them bai de cong don gio. Mo trong moi trang thai tru luc
        // may dang khoa han - dang cho duyet cung nop tiep duoc, dang choi cung vay.
        // Truoc day dang cho duyet la het duong nop, phai ngoi cho ba bam duyet xong
        // moi lam bai tiep duoc.
        //
        // Het tran hom nay thi thoi, nop nua cung khong duyet duoc. Xep hang du
        // [GateStore.MAX_BAI_CHO] bai cung thoi, cho ba duyet bot da.
        val dangCoGi = gate.state != GateState.LOCKED
        binding.btnNopThem.visibility =
            if (!baDangDung && dangCoGi && gate.phutConLaiHomNay() > 0 && gate.conChoNopThem()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        binding.btnNopThem.text = if (gate.grantedMinutes > 0 || gate.isOpen()) {
            "Nộp thêm bài để cộng dồn giờ"
        } else {
            "Nộp thêm bài nữa"
        }
        binding.btnNopThem.setOnClickListener { onSubmit(nopThem = true) }
        veHocThuoc(baDangDung)

        veBangSua()
        veSoanTap()
        veTinCuaCo()

        val chuaDoc = ChatBox.unread(this)
        binding.btnChat.text = if (chuaDoc > 0) {
            "${getString(R.string.parent_name_cap)} nhắn $chuaDoc tin mới"
        } else {
            getString(R.string.home_chat)
        }

        veCanhBao()
        nhipDongHo()
    }

    /**
     * The soan cap cho buoi hoc ke tiep.
     *
     * Hien ca khi da soan xong, chi doi mau va doi chu. An di luc do thi man hinh
     * nhay mot cai roi mat mot the, ma cai can noi - "khong con no gi" - lai la
     * cai dang duoc noi nhat.
     */
    private fun veSoanTap() {
        val ke = TinhLoiNhac.buoiKeTiep(Calendar.getInstance())
        if (ke == null) {
            binding.cardSoan.visibility = View.GONE
            return
        }
        val (cal, buoi) = ke
        val ma = TinhLoiNhac.maBuoi(cal, buoi)
        val daSoan = ma in prefs.buoiDaSoan
        val mon = buoi.monCanSoan

        // Buoi khong co vo ma cung khong co the duc thi khong co viec gi de bay ra.
        if (mon.isEmpty() && !buoi.coTheDuc) {
            binding.cardSoan.visibility = View.GONE
            return
        }
        binding.cardSoan.visibility = View.VISIBLE

        binding.cardSoan.setCardBackgroundColor(
            ContextCompat.getColor(this, if (daSoan) R.color.ok_soft else R.color.canvas)
        )
        binding.txtSoanTitle.text = if (daSoan) {
            "Đã soạn cặp cho ${TinhLoiNhac.moTaBuoi(buoi)}"
        } else {
            "Chưa soạn cặp cho ${TinhLoiNhac.moTaBuoi(buoi)}"
        }
        binding.txtSoanDetail.text = buildString {
            append("Vào học ").append(TinhLoiNhac.gioPhut(buoi.phutVaoHoc))
            append(", không xài máy lúc ")
            append(TinhLoiNhac.gioPhut(ThoiKhoaBieu.phutBuongMay(buoi)))
            append('.')
            if (mon.isEmpty()) {
                append("\nChỉ cần mang đồ thể dục.")
            } else {
                append("\nCần mang: ").append(mon.joinToString(", "))
                if (buoi.coTheDuc) append(", và đồ thể dục")
                append('.')
            }
            NgayNghi.tenKyNghi(cal)?.let { append("\nHôm đó là ").append(it).append('.') }
        }
        binding.btnSoan.text = when {
            daSoan -> "Soạn lại"
            mon.isEmpty() -> "Chuẩn bị đồ thể dục"
            else -> getString(R.string.soan_title)
        }
    }

    /**
     * Nut vao kho tin co giao.
     *
     * Chi hien khi co tin that. Ba noi cam tablet len doc duoc o day, khong phai
     * mo Zalo.
     */
    private fun veTinCuaCo() {
        val kho = KhoTinCuaCo(this)
        val soTin = kho.danhSach().size
        if (soTin == 0) {
            binding.btnTin.visibility = View.GONE
            binding.vachTin.visibility = View.GONE
            return
        }
        binding.btnTin.visibility = View.VISIBLE
        binding.vachTin.visibility = View.VISIBLE
        val chuaDoc = kho.soTinChuaDoc()
        binding.btnTin.text = if (chuaDoc > 0) {
            "${getString(R.string.soan_tin_co)} · $chuaDoc tin mới"
        } else {
            getString(R.string.soan_tin_co)
        }
    }

    /**
     * Mo man soan. Bam "Soan lai" thi bo danh dau truoc, khong thi man soan vua mo
     * ra da thay buoi nay xong roi va tu dong lai.
     */
    private fun moManSoan() {
        val ke = TinhLoiNhac.buoiKeTiep(Calendar.getInstance()) ?: return
        val ma = TinhLoiNhac.maBuoi(ke.first, ke.second)
        if (ma in prefs.buoiDaSoan) prefs.boDanhDau(ma)
        startActivity(Intent(this, SoanActivity::class.java))
    }

    /**
     * Mot mau va mot hinh cho moi trang thai.
     *
     * Truoc day the trang thai luc nao cung mot mau trang giong nhau, chi khac may
     * chu o giua. "Dang khoa" voi "Dang duoc choi" nam cung mot cho, cung co chu,
     * lieec qua thi giong het nhau.
     *
     * [hinh] la mot vector trong res/drawable, do lai theo [mau] ngay tai day. Cho
     * nay tung la mot emoji: no ve theo bo font cua may chu khong theo bang mau cua
     * app, va doi may mot kieu.
     */
    private fun doiMat(hinh: Int, mau: Int, nen: Int) {
        binding.anhMat.setImageResource(hinh)
        binding.anhMat.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, mau))
        binding.cardState.setCardBackgroundColor(ContextCompat.getColor(this, nen))
        binding.txtBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, mau))
        binding.txtBadge.setTextColor(ContextCompat.getColor(this, R.color.surface))
        binding.txtState.setTextColor(ContextCompat.getColor(this, mau))
    }

    /**
     * Bang "con cau nao can sua".
     *
     * Loi nhan xet hien duoi ten Ba Huy, khong noi gi den may moc. Mot dua tre biet
     * minh dang bi may cham se di tim cach lach may, vi may de qua mat hon nguoi
     * that. Ben Telegram thi ghi ro la AI cham - do la phia cua Ba Huy.
     */
    private fun veBangSua() {
        val canSua = SoCaiBai.dangChoSua(this)
        val loiNhan = SoCaiBai.loiNhan(this)
        /*
         * Cau den hen nho lai. Chi hoi khi khong con no gi: sua bai dang lam do
         * truoc, on lai la viec cua hom nao ranh.
         *
         * Va khong hoi trong luc dang choi. Ham nay chay moi giay khi dong ho dang
         * dem, ma cau tra loi thi la mot cau GROUP BY tren ca bang tra loi - hoi moi
         * giay de ru mot dua tre dang choi di on bai thi vua ton vua vo ich.
         */
        val denHen = if (canSua.isEmpty() && !gate.isOpen()) {
            SoCaiBai.cacCauDangOn(this)
        } else {
            emptyList()
        }
        if ((canSua.isEmpty() && loiNhan == null && denHen.isEmpty()) ||
            ParentMode.isActive(this)
        ) {
            binding.cardSua.visibility = View.GONE
            return
        }
        binding.cardSua.visibility = View.VISIBLE

        // Khong co cau nao can sua ma van co loi nhan: hien mot dong thoi, an nut
        // chup lai. Day la canh nop lai bai da cham hom truoc - phai noi cho con
        // biet vi sao khong duoc gi, khong thi no bam nop lai lan nua.
        if (canSua.isEmpty() && loiNhan != null) {
            binding.txtSuaTitle.text = "${getString(R.string.parent_name_cap)}: $loiNhan"
            binding.boxSua.removeAllViews()
            binding.btnChupSua.visibility = View.GONE
            return
        }

        /*
         * Khong no gi, nhung co cau den hen nho lai.
         *
         * De chung cho voi bang "can sua" chu khong them mot the thu hai: hai the
         * chi hien mot cai moi luc, va ca hai deu tra loi cung mot cau hoi - bay gio
         * con nen lam gi.
         */
        if (canSua.isEmpty()) {
            binding.txtSuaTitle.text = if (denHen.size == 1) {
                "1 câu đến hẹn ôn lại"
            } else {
                "${denHen.size} câu đến hẹn ôn lại"
            }
            binding.boxSua.removeAllViews()
            binding.boxSua.addView(
                android.widget.TextView(this).apply {
                    text = "Mấy câu con từng sai. Làm lại trong vở bằng bút đỏ " +
                        "rồi chụp, đúng thì được cộng thêm giờ chơi. Viết bút thường thì " +
                        "máy không tính giờ."
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.ink))
                    setPadding(0, (6 * resources.displayMetrics.density).toInt(), 0, 0)
                }
            )
            binding.btnChupSua.visibility = View.VISIBLE
            binding.btnChupSua.text = "Ôn lại"
            binding.btnChupSua.setOnClickListener {
                startActivity(Intent(this, ChonBaiActivity::class.java))
            }
            return
        }
        binding.btnChupSua.visibility = View.VISIBLE
        binding.btnChupSua.text = "Chụp lại câu đã sửa"
        binding.txtSuaTitle.text = if (canSua.size == 1) {
            "Còn 1 câu cần sửa"
        } else {
            "Còn ${canSua.size} câu cần sửa"
        }

        binding.boxSua.removeAllViews()
        val dp = resources.displayMetrics.density
        canSua.take(5).forEach { cau ->
            val dong = android.widget.TextView(this).apply {
                text = buildString {
                    append("• ").append(cau.ma)
                    if (cau.nhanXet.isNotBlank()) append(": ").append(cau.nhanXet)
                }
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.ink))
                setPadding(0, (6 * dp).toInt(), 0, 0)
            }
            binding.boxSua.addView(dong)
        }

        binding.btnChupSua.setOnClickListener {
            startActivity(
                Intent(this, CaptureActivity::class.java)
                    .putExtra(CaptureActivity.EXTRA_SUA, true)
                    .putExtra(CaptureActivity.EXTRA_PHAM, phamViSua(canSua)?.sangJson())
            )
        }
    }

    /**
     * Nut Hoc thuoc: chi hien khi that su co the den luot.
     *
     * Het the ma nut van nam do thi con bam vao, thay mot man hinh khong co gi, roi
     * quay ra - lan sau no khong bam nua, ke ca hom co the. Cung mot le voi the
     * "cau kho da go" ben [TienBoActivity]: mot con so khong hay mot nut rong deu
     * chiem cho ma khong noi len gi.
     *
     * Dem tren luong ve man hinh, va do la co y: mot bo the vai tram dong thi phep
     * dem nay mat khong toi mot phan muoi giay, con day ra luong nen thi phai giu
     * mot cai gi do de biet ket qua ve co con kip khong.
     */
    private fun veHocThuoc(baDangDung: Boolean) {
        val co = !baDangDung &&
            runCatching { BoThe.bang(this).any { it.soDenLuot > 0 } }.getOrDefault(false)
        binding.btnHocThuoc.visibility = if (co) View.VISIBLE else View.GONE
    }

    /**
     * Pham vi cho lan chup sua bai: dung cac cau dang cho sua.
     *
     * Con khong phai khai lai gi ca - may da biet no dang no nhung cau nao. Va vi
     * biet, lan cham nay cung di duong ma cau co dinh chu khong phai doan lai tu
     * de bai, tuc la sua xong nop lai thi dung cau do duoc danh dau la xong.
     *
     * Tra ve null khi khong cau nao trong so do co ma sach - bai ngoai sach thi
     * khong co gi de khai, cu de may tu tach cau nhu truoc.
     */
    private fun phamViSua(canSua: List<CauSo>): PhamVi? {
        val ids = canSua.map { it.khoa }.filter { it.contains(':') }
        if (ids.isEmpty()) return null
        val nguon = ids.first().substringBefore(':')
        val sach = NganHang.sachTheoNguon(nguon) ?: return null
        // Chi lay cau cung mot quyen: cau lenh gui cho AI ke ten mot quyen, tron hai
        // quyen vao mot danh sach thi dong chu do noi sai.
        val cungSach = ids.filter { it.startsWith("$nguon:") }
        return PhamVi(
            mon = sach.mon,
            nguon = nguon,
            tenNguon = sach.ten,
            bai = "các câu cần sửa",
            cauIds = cungSach
        )
    }

    /**
     * Bang "con thieu gi". Hien ca tren man cua Le Hoa, vi day la man hinh mo ra
     * moi ngay: co gi ho thi Ba Huy nhin thay ngay lan toi cam may.
     */
    private fun veCanhBao() {
        CanhBao.veThe(
            this,
            binding.cardWarning,
            binding.txtWarningTitle,
            binding.boxWarning
        ) { render() }
    }

    /**
     * @param nopThem true la con bam "Nop them bai de cong don gio" chu khong phai
     *   nut to. Luc do bo qua het cac nhanh Bat dau / Tam dung, di thang vao chup.
     */
    private fun onSubmit(nopThem: Boolean = false) {
        if (!prefs.isConfigured) {
            toast("${getString(R.string.parent_name_cap)} chưa cài đặt xong")
            return
        }
        if (!nopThem && gate.state == GateState.GRANTED) {
            batDau()
            return
        }
        // Dang cho duyet bai moi ma van giu phieu cu: nut to la Bat dau choi.
        if (!nopThem && gate.state == GateState.PENDING && gate.grantedMinutes > 0) {
            batDau()
            return
        }
        if (!nopThem && gate.state == GateState.PENDING) {
            huyYeuCau()
            return
        }
        if (!nopThem && gate.state == GateState.PAUSED) {
            val phut = gate.resume()
            if (phut == null) {
                toast("Tới giờ ngủ rồi, mai Lê Hòa nộp bài tiếp nhé")
            } else {
                toast("Chơi tiếp, còn $phut phút")
                Notifier.conChoiTiep(this, phut)
            }
            render()
            return
        }
        if (!nopThem && gate.isOpen()) {
            // Tam dung la khoa app giai tri lai luon. Nho vay giu gio khong phai la
            // thu mien phi: muon giu thi phai chiu khong choi trong luc do.
            val phut = gate.pause()
            toast("Đã tạm dừng, giữ lại ${CountdownOverlay.dinhDang(gate.pausedMs())}")
            Notifier.conTamDung(this, phut ?: 0)
            render()
            return
        }
        if (gate.phutConLaiHomNay() <= 0) {
            toast("Hôm nay đủ giờ chơi rồi, mai nộp bài tiếp nhé")
            return
        }
        // Mat mang thi chan ngay o day. Ca duong nop bai deu can mang - cham bai goi
        // Google, gui anh goi Telegram - nen de con chup xong ca xap roi moi bao hong
        // la bat no lam khong cong.
        if (!Mang.co(this)) {
            toast("Máy chưa có mạng nên chưa nộp bài được. Bật wifi rồi thử lại nhé.")
            return
        }

        // Qua man khai bai truoc, khong vao thang camera nua. Xem [ChonBaiActivity]
        // de biet vi sao them mot buoc vao giua.
        startActivity(Intent(this, ChonBaiActivity::class.java))
    }

    /**
     * Duong vao phan cua Ba Huy, qua ma PIN.
     *
     * Truoc day day la mot o to bang nua man hinh dau tien. Nhung app nay la app
     * quan ly con: Le Hoa mo no moi ngay, con Ba Huy thi dieu khien qua Telegram la
     * chinh va chi thinh thoang moi can bang dieu khien tren may. Cho no mot nut
     * chu nho o goc la dung trong so.
     */
    private fun moChoBaHuy() {
        if (!prefs.hasPin()) {
            startActivity(Intent(this, SetupActivity::class.java))
            return
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Mã PIN"
            setPadding(48, 40, 48, 40)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pin_title)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (prefs.checkPin(input.text.toString())) {
                    prefs.saiPinLienTiep = 0
                    // Chi mo trang cau hinh, KHONG mo khoa may. Muon mo khoa thi
                    // gat cong tac trong do. Truoc day hai viec nay tron lam mot,
                    // nen chi vao xem lai gio nghi cung lam tablet mo toang.
                    PhienQuanLy.daQuaPin = true
                    Notifier.parentModeEntered(this)
                    ApprovalService.ensureRunning(this)
                    startActivity(Intent(this, ParentActivity::class.java))
                } else {
                    val soLan = prefs.saiPinLienTiep + 1
                    prefs.saiPinLienTiep = soLan
                    if (soLan % 3 == 0) Notifier.wrongPinAttempts(this, soLan)
                    toast(getString(R.string.pin_wrong))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Huy yeu cau dang cho duyet, de con nop lai.
     *
     * Truoc day khong co duong nay: da gui roi ma ba chua duyet thi con ket, khong
     * nop lai duoc cung khong rut lai duoc. Chup thieu mot trang, hay chup mo, la
     * phai ngoi cho ba duyet cai anh hong do roi moi lam gi tiep duoc.
     *
     * Go luon ban phim duoi anh ben Telegram, khong thi ba van thay nut Duyet cua
     * mot yeu cau da khong con.
     */
    private fun huyYeuCau() {
        val con = gate.soBaiDangCho()
        val loi = if (con > 1) {
            "Huỷ bài vừa nộp. ${con - 1} bài nộp trước vẫn nằm chờ ba Huy duyệt."
        } else {
            "Ba Huy sẽ thấy là Lê Hòa đã huỷ. Sau đó Lê Hòa chụp và nộp lại từ đầu."
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Huỷ yêu cầu đã gửi?")
            .setMessage(loi)
            .setPositiveButton("Huỷ yêu cầu") { _, _ ->
                val messageId = gate.huyBaiMoiNhat()?.messageId ?: 0L
                render()
                toast("Đã huỷ, Lê Hòa nộp lại nhé")
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val client = TelegramClient(prefs.botToken)
                        if (messageId != 0L) {
                            client.clearReplyMarkup(prefs.parentChatId, messageId)
                        }
                        client.sendMessage(
                            prefs.parentChatId,
                            "${getString(R.string.child_name)} đã huỷ yêu cầu vừa gửi " +
                                "và sẽ nộp lại. Ba Huy bỏ qua ảnh ở trên nhé."
                        )
                    }
                }
            }
            .setNegativeButton("Để nguyên", null)
            .show()
    }

    /** Con bam Bat dau: tu day dong ho moi chay. */
    private fun batDau() {
        val phut = gate.start()
        if (phut == null) {
            toast("Tới giờ ngủ rồi, mai Lê Hòa nộp bài tiếp nhé")
            render()
            return
        }
        ApprovalService.ensureRunning(this)
        Notifier.conBatDau(this, phut)
        toast("Bắt đầu! Lê Hòa có $phut phút")
        render()
    }

    private fun toast(text: String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
