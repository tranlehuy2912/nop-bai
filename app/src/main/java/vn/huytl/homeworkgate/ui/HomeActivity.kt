package vn.huytl.homeworkgate.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
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
import vn.huytl.homeworkgate.data.SoCaiBai
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

    /** Vong ve lai man hinh trong luc no dang mo. */
    private var lamTuoi: Job? = null

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
        // Ve lai deu dan chu khong chi mot lan luc mo man hinh. Truoc day dung mot
        // lan: con dung yen o day thi so phut con lai dung im o 23 phut, va tin
        // nhan moi cua ba khong hien len nut, phai thoat ra vao lai moi thay.
        lamTuoi?.cancel()
        lamTuoi = lifecycleScope.launch {
            while (true) {
                gate.tick()
                render()
                // Dang choi thi ve lai moi giay cho dong ho chay that; luc khac moi
                // 10 giay la du, khong co gi doi nhanh den the.
                delay(if (gate.isOpen()) 1_000L else 10_000L)
            }
        }
    }

    override fun onPause() {
        lamTuoi?.cancel()
        lamTuoi = null
        super.onPause()
    }

    private fun render() {
        val baDangDung = ParentMode.isActive(this)
        // So bai dang xep hang cho Ba Huy duyet. Co the nhieu hon mot: con lam xong
        // dot nay nop tiep dot khac ma khong phai cho duyet xong dot truoc.
        val soBaiCho = gate.soBaiDangCho()
        when {
            baDangDung -> {
                doiMat("🔓", R.color.parent_tint, R.color.parent_soft)
                binding.txtBadge.text = "Máy đang mở"
                binding.txtState.text = getString(R.string.parent_name)
                binding.txtDetail.text = "Đang dùng máy — ${ParentMode.moTa(this)}"
            }
            gate.state == GateState.PENDING && gate.grantedMinutes > 0 -> {
                // Nop them bai de cong don, ma trong tay van con phieu cu.
                doiMat("⏳", R.color.brand, R.color.brand_soft)
                binding.txtBadge.text =
                    if (soBaiCho > 1) "Đã gửi thêm $soBaiCho bài" else "Đã gửi thêm bài"
                binding.txtState.text = "${gate.grantedMinutes} phút"
                binding.txtDetail.text =
                    "Không phải đợi duyệt — chơi được rồi, duyệt xong máy cộng thêm."
            }
            gate.state == GateState.PENDING -> {
                doiMat("⏳", R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = if (soBaiCho > 1) "Đã gửi $soBaiCho bài" else "Đã gửi bài"
                binding.txtState.text = "Chờ Ba Huy duyệt"
                // Dong to o tren da noi "Cho Ba Huy duyet" roi. Dong nay de danh cho
                // cai con khong tu biet: trong luc cho van lam bai tiep duoc.
                binding.txtDetail.text = if (soBaiCho > 1) {
                    "Duyệt từng bài một, bài nào xong là cộng giờ bài đó."
                } else {
                    "Trong lúc chờ, làm thêm bài nộp tiếp cũng được."
                }
            }
            gate.state == GateState.GRANTED -> {
                doiMat("👍", R.color.brand, R.color.brand_soft)
                // Bo trong la Ba Huy duyet, duong di cua gan het moi phieu gio.
                binding.txtBadge.text = gate.nhanCho
                    .ifEmpty { "${getString(R.string.parent_name)} đã duyệt" }
                binding.txtState.text = "${gate.grantedMinutes} phút"
                // Nut to ngay duoi da ghi "Bat dau choi", nhac lai la thua. Giu dung
                // cai con khong tu doan duoc: dong ho chua chay, khong bam som cung
                // khong mat gi.
                binding.txtDetail.text = "Chưa tính giờ đâu — sẵn sàng chơi mới bấm."
            }
            gate.state == GateState.PAUSED -> {
                doiMat("⏸️", R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = "Đang tạm dừng"
                // Den tung giay, giong het luc dang choi: so nay la so se chay tiep
                // khi bam "Choi tiep", nen hien khac di la sau do thay hut mat may
                // chuc giay.
                binding.txtState.text = CountdownOverlay.dinhDang(gate.pausedMs())
                binding.txtDetail.text = "Giờ giữ nguyên, không chạy tiếp." +
                    if (soBaiCho > 0) " Còn $soBaiCho bài chờ duyệt." else ""
            }
            gate.isOpen() -> {
                doiMat("🎮", R.color.ok, R.color.ok_soft)
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
                doiMat("🔒", R.color.locked, R.color.locked_soft)
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
        binding.btnSubmit.isEnabled = !baDangDung && !hetLuot
        // Cung mot nut to, doi chu theo viec dang can lam, de man hinh khong bao
        // gio co hai nut to cung luc.
        binding.btnSubmit.text = when {
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

        veBangSua()
        veSoanTap()
        veTinCuaCo()

        val chuaDoc = ChatBox.unread(this)
        binding.btnChat.text = if (chuaDoc > 0) {
            "${getString(R.string.parent_name)} nhắn $chuaDoc tin mới"
        } else {
            getString(R.string.home_chat)
        }

        veCanhBao()
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
     */
    private fun doiMat(hinh: String, mau: Int, nen: Int) {
        binding.txtIcon.text = hinh
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
        if ((canSua.isEmpty() && loiNhan == null) || ParentMode.isActive(this)) {
            binding.cardSua.visibility = View.GONE
            return
        }
        binding.cardSua.visibility = View.VISIBLE

        // Khong co cau nao can sua ma van co loi nhan: hien mot dong thoi, an nut
        // chup lai. Day la canh nop lai bai da cham hom truoc - phai noi cho con
        // biet vi sao khong duoc gi, khong thi no bam nop lai lan nua.
        if (canSua.isEmpty()) {
            binding.txtSuaTitle.text = "${getString(R.string.parent_name)}: $loiNhan"
            binding.boxSua.removeAllViews()
            binding.btnChupSua.visibility = View.GONE
            return
        }
        binding.btnChupSua.visibility = View.VISIBLE
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
            toast("${getString(R.string.parent_name)} chưa cài đặt xong")
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
            "Huỷ bài vừa nộp. ${con - 1} bài nộp trước vẫn nằm chờ Ba Huy duyệt."
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
