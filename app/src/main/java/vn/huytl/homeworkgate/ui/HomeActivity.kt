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
import android.widget.LinearLayout
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.CauSo
import vn.huytl.homeworkgate.data.Mang
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.databinding.ActivityHomeBinding
import vn.huytl.homeworkgate.databinding.StDongViecBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.guard.CountdownOverlay
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.PhienQuanLy
import vn.huytl.homeworkgate.guard.TelegramThat
import vn.huytl.homeworkgate.guard.TinCuaBa
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
        binding.btnChat.setOnClickListener { nhanChoBa() }
        // Man chat cu con giu lam duong du phong trong luc chuyen sang Telegram. Bam giu
        // la vao, de Telegram bi dang xuat hay hong thi con van nhan duoc cho ba.
        binding.btnChat.setOnLongClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
            true
        }
        toMauHangPhu()
        binding.btnParent.setOnClickListener { moChoBaHuy() }
        binding.btnNopThem.setOnClickListener { onSubmit(nopThem = true) }
        binding.btnLich.setOnClickListener {
            startActivity(Intent(this, LichActivity::class.java))
        }
        binding.btnTin.setOnClickListener {
            startActivity(Intent(this, TinActivity::class.java))
        }
        binding.btnTienBo.setOnClickListener {
            startActivity(Intent(this, TienBoActivity::class.java))
        }
        // Hai duong vao cung mot man. Thanh han muc la duong dung cho nhat - con
        // dang nhin so phut cua hom nay thi cau hoi "lam gi de duoc them" moc len
        // ngay cho do - nhung mot thanh mau bam duoc thi khong ai doan ra, nen van
        // phai co mot o co ten o hang duoi cung.
        val moBangGia = View.OnClickListener {
            startActivity(Intent(this, CachKiemGioActivity::class.java))
        }
        binding.btnKiemGio.setOnClickListener(moBangGia)
        binding.khungHanNgay.setOnClickListener(moBangGia)

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
        // Viec nha nguoi lon giao, con chua lam xong. Man chan dang che ca may, nhung no
        // nhuong cho chinh app nay - nen day la cho duy nhat con doc duoc con phai
        // lam gi.
        val phienViec = ViecNha.dangTreo(this)
        val conViec = phienViec?.chuaXong.orEmpty()
        when {
            baDangDung -> {
                doiMat(R.drawable.ic_mat_mo_khoa, R.color.parent_tint, R.color.parent_soft)
                binding.txtBadge.text = "Máy đang mở"
                binding.txtState.text = getString(R.string.parent_name_cap)
                binding.txtDetail.text = "Đang dùng máy — ${ParentMode.moTa(this)}"
            }
            conViec.isNotEmpty() -> {
                doiMat(R.drawable.ic_mat_viec_nha, R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = "${ViecNha.nguoiGiao(phienViec)} giao việc"
                binding.txtState.text = if (conViec.size == 1) {
                    conViec.first().ten
                } else {
                    "Còn ${conViec.size} việc"
                }
                binding.txtDetail.text = conViec.joinToString(", ") { it.ten } +
                    ".\n" + ViecNha.NHO_BAM
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
                // Dong to o tren da noi "Cho Ba Huy duyet", nut chinh ben duoi da noi
                // nop them duoc. Dong nay de danh cho cai con khong tu biet: luc nao thi
                // dung toi nut huy nho o duoi cung.
                binding.txtDetail.text = if (soBaiCho > 1) {
                    "Duyệt từng bài một, bài nào xong là cộng giờ bài đó."
                } else {
                    "Ảnh thiếu hay mờ thì huỷ bài vừa nộp rồi chụp lại."
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
        // Dang cho duyet ma chua giu phieu nao: nut chinh la nop them bai. Hang cho da
        // du bai, hay het tran hom nay, thi nop them cung khong duoc duyet: nut tat va
        // noi ly do, thay cho viec bam roi hien mot cau toast.
        val choDuyet = gate.state == GateState.PENDING && gate.grantedMinutes <= 0
        val hangDay = !gate.conChoNopThem()
        val hetTran = gate.phutConLaiHomNay() <= 0
        binding.btnSubmit.isEnabled = !baDangDung && !hetLuot && !vuongViec &&
            !(choDuyet && (hangDay || hetTran))
        // Cung mot nut to, doi chu theo viec dang can lam, de man hinh khong bao
        // gio co hai nut to cung luc.
        binding.btnSubmit.text = when {
            vuongViec -> "Làm xong việc nhà đã"
            hetLuot -> "Hôm nay đủ giờ chơi rồi"
            gate.state == GateState.GRANTED -> "Bắt đầu chơi"
            gate.state == GateState.PENDING && gate.grantedMinutes > 0 -> "Bắt đầu chơi"
            // Truoc day nut chinh luc nay la "Huy, nop lai bai khac": nut to nhat man
            // hinh lam viec huy, dung cho con quen tay bam nut nop bai.
            choDuyet && hangDay -> "Chờ ba Huy duyệt bớt đã"
            choDuyet && hetTran -> "Chờ ba Huy duyệt"
            choDuyet -> "Nộp thêm bài nữa"
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
        //
        // Rieng luc dang cho duyet ma chua giu phieu nao, nut chinh da la nop them. Nut
        // phu luc do la huy bai vua nop, luc nao cung hien: chup mo hay thieu trang thi
        // day la duong duy nhat de nop lai, ke ca khi hang da day.
        val dangCoGi = gate.state != GateState.LOCKED
        binding.btnNopThem.visibility = when {
            baDangDung -> View.GONE
            choDuyet -> View.VISIBLE
            dangCoGi && gate.phutConLaiHomNay() > 0 && gate.conChoNopThem() -> View.VISIBLE
            else -> View.GONE
        }
        binding.btnNopThem.text = when {
            choDuyet -> "Huỷ bài vừa nộp"
            gate.grantedMinutes > 0 || gate.isOpen() -> "Nộp thêm bài để cộng dồn giờ"
            else -> "Nộp thêm bài nữa"
        }
        binding.btnNopThem.setOnClickListener {
            if (choDuyet) huyYeuCau() else onSubmit(nopThem = true)
        }

        veVongPhien()
        veHanNgay()
        veViecHomNay(baDangDung)
        veTinCuaCo()

        // Dem cung mot cho voi viec bam nut, xem [nhanChoBa]: con tin chua doc o man chat
        // cu thi so do la cua man cu, vi bam vao se ra man cu. Khong thi dem tin Telegram.
        val chuaDoc = ChatBox.unread(this).takeIf { it > 0 } ?: TinCuaBa.so(this)
        binding.btnChat.text = if (chuaDoc > 0) {
            "${getString(R.string.parent_name_cap)} nhắn $chuaDoc tin mới"
        } else {
            getString(R.string.home_chat)
        }

        nhipDongHo()
    }

    /**
     * Cac duong phu duoi cung, moi cai mot mau.
     *
     * Truoc day chung xam nhu nhau het, nhin ra mot dai chu chu khong ra may cho
     * di khac nhau. Mau chi dat cho BIEU TUONG, khong dat cho chu: mot hang chu du
     * mau thi no lai to tieng hon ca nut Nop bai o ngay tren.
     */
    private fun toMauHangPhu() {
        listOf(
            binding.btnChat to R.color.parent_tint,
            binding.btnTin to R.color.wait,
            binding.btnLich to R.color.brand,
            binding.btnTienBo to R.color.ok,
            // Nau la mau cua trang thai dang khoa, va man bang gia noi dung mot
            // viec: lam gi de mo cai khoa do. Bon mau kia da co chu, con lai nau
            // voi xanh cua Le Hoa - ma xanh cua Le Hoa dang nam o o ten ngay dau
            // man nay, dung lai o day thi hai cho do trong nhu co lien quan.
            binding.btnKiemGio to R.color.locked
        ).forEach { (nut, mau) ->
            nut.iconTint = ContextCompat.getColorStateList(this, mau)
        }
    }

    /**
     * Vong tron dem nguoc cua phien dang chay.
     *
     * Chi thay cho dong chu to o giua khi that su co gi de dem: dang choi, hay dang
     * tam dung. Cac trang thai khac ("Chua nop bai", "Cho ba Huy duyet") khong co
     * moc nao de ve mot vong quanh.
     */
    private fun veVongPhien() {
        val dangChoi = gate.isOpen()
        val dangDung = gate.state == GateState.PAUSED
        if (!dangChoi && !dangDung) {
            binding.khungVong.visibility = View.GONE
            binding.txtState.visibility = View.VISIBLE
            binding.anhMat.visibility = View.VISIBLE
            return
        }

        val conLai = if (dangChoi) gate.remainingMs() else gate.pausedMs()
        // Tong cua phien: luc tam dung tongPhienMs() tra 0, nen lay chinh phan con
        // lai lam tong - vong day, dung nhu y nghia "gio dang duoc giu nguyen".
        val tong = maxOf(gate.tongPhienMs(), conLai, 1L)
        val mau = if (dangChoi) R.color.ok else R.color.wait

        binding.khungVong.visibility = View.VISIBLE
        binding.txtState.visibility = View.GONE
        binding.anhMat.visibility = View.GONE
        binding.txtVong.text = CountdownOverlay.dinhDang(conLai)
        binding.txtVong.setTextColor(ContextCompat.getColor(this, mau))
        binding.vongPhien.setIndicatorColor(ContextCompat.getColor(this, mau))
        binding.vongPhien.trackColor =
            ContextCompat.getColor(this, if (dangChoi) R.color.ok_soft else R.color.wait_soft)
        binding.vongPhien.max = (tong / 1000L).toInt().coerceAtLeast(1)
        binding.vongPhien.progress = (conLai / 1000L).toInt()
    }

    /**
     * Thanh han muc gio choi trong ngay.
     *
     * Ba doan tren cung mot truc, truc la tran cua ngay:
     *   - da choi: phan da tieu roi, khong lay lai duoc.
     *   - dang giu: da kiem duoc ma chua bam choi, hoac dang tam dung.
     *   - con lai: phan hom nay con co the kiem them bang bai tap.
     *
     * "Da choi" tinh bang phut da duyet tru phan con dang giu, chu khong dem rieng:
     * so phut duyet la con so duy nhat duoc ghi xuong, va mot cai so dem thu hai la
     * mot cho nua de hai ben lech nhau.
     */
    private fun veHanNgay() {
        val tran = prefs.tranPhutMoiNgay
        if (tran <= 0) {
            binding.khungHanNgay.visibility = View.GONE
            return
        }
        val kiem = gate.phutDaDuyetHomNay()
        val giuMs = when {
            gate.isOpen() -> gate.remainingMs()
            gate.state == GateState.PAUSED -> gate.pausedMs()
            gate.grantedMinutes > 0 -> gate.grantedMinutes * 60_000L
            else -> 0L
        }
        val giu = ((giuMs + 59_999L) / 60_000L).toInt().coerceAtMost(kiem)
        val daChoi = (kiem - giu).coerceAtLeast(0)

        binding.khungHanNgay.visibility = View.VISIBLE
        binding.phanDaChoi.setBackgroundColor(ContextCompat.getColor(this, R.color.ok))
        binding.phanDangGiu.setBackgroundColor(ContextCompat.getColor(this, R.color.brand))
        // Trong LinearLayout ngang, weight la ty le. Phan con lai khong can View nao:
        // no chinh la nen cua khung.
        (binding.phanDaChoi.layoutParams as LinearLayout.LayoutParams).weight = daChoi.toFloat()
        (binding.phanDangGiu.layoutParams as LinearLayout.LayoutParams).weight = giu.toFloat()
        val conLai = (tran - kiem).coerceAtLeast(0)

        binding.chuHanNgay.text = buildString {
            append("Hôm nay kiếm được ").append(kiem).append('/').append(tran).append(" phút")
            /*
             * Hai con so sau de TRONG NGOAC chu khong ngan bang dau cham giua.
             *
             * Chung la PHAN CUA con so dau, cong lai dung bang no. Dau cham giua thi
             * ngan nhung thu ngang hang, nen dung o day lam ba con so trong nhu ba su
             * viec bang vai - doc xong khong biet cai nao gom cai nao.
             *
             * Thu tu "da choi" truoc "chua choi" de khop voi thanh mau ngay ben duoi:
             * phan xanh la nam ben trai, phan xanh duong nam ben phai. Ban truoc chu
             * va thanh nguoc nhau.
             */
            val chia = listOfNotNull(
                "đã chơi $daChoi".takeIf { daChoi > 0 },
                "chưa chơi $giu".takeIf { giu > 0 },
            )
            when {
                chia.isNotEmpty() -> append(" (").append(chia.joinToString(", ")).append(")")
                // Cau nhac thi tach han thanh cau rieng: no khong phai mot mau tin
                // ngang hang voi hai con so kia.
                conLai > 0 -> append(". Làm bài để kiếm thêm.")
            }
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
     * Danh sach viec hom nay: on lai, soan cap, hoc thuoc, chup lai cau da sua.
     *
     * TRUOC DAY LA BON THE RIENG, moi the mot nut. Cong voi nut Nop bai, nut nop
     * them va hang duoi cung thi man hinh co gan chuc cho bam - nhieu hon han cai
     * ma mot dua tre lop tam can thay khi cam may len. Gio moi viec la mot dong,
     * bam ca dong, va khong con nut nao trong do.
     *
     * Thu tu la thu tu nen lam: sua bai dang no truoc, roi den viec co gio chot
     * (soan cap), roi moi den hai viec lam luc ranh.
     */
    private fun veViecHomNay(baDangDung: Boolean) {
        binding.boxViec.removeAllViews()
        if (baDangDung) {
            binding.nhanViec.visibility = View.GONE
            binding.cardViec.visibility = View.GONE
            return
        }

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

        if (canSua.isNotEmpty()) {
            val ke = canSua.take(3).joinToString(", ") { it.ma }
            /*
             * Bam vao la ra man ket qua truoc, chup lai sau.
             *
             * Truoc day dong nay mo thang camera: con biet cau nao sai ma khong biet sai
             * o dau, vi loi nhan xet cua may khong con hien o cho nao tren tablet ke tu
             * khi man chinh gon lai ngay 18/9/2026. Nut chup lai nam o day man ket qua,
             * xem xong la chup luon.
             */
            themViec(
                hinh = R.drawable.st_ic_dau_hoi,
                mau = R.color.alert,
                ten = if (canSua.size == 1) "Sửa 1 câu rồi chụp lại"
                else "Sửa ${canSua.size} câu rồi chụp lại",
                phu = "Xem sai ở đâu: " + ke + if (canSua.size > 3) "…" else ""
            ) {
                startActivity(
                    Intent(this, KetQuaActivity::class.java)
                        .putExtra(KetQuaActivity.EXTRA_SUA, true)
                        .putExtra(KetQuaActivity.EXTRA_PHAM, phamViSua(canSua)?.sangJson())
                )
            }
        } else if (loiNhan != null) {
            // Nop lai bai da cham hom truoc ma khong duoc gi: phai noi vi sao, khong
            // thi con bam nop lai lan nua. Bam vao thi xem ket qua cham tung cau.
            themViec(
                hinh = R.drawable.st_ic_dau_hoi,
                mau = R.color.alert,
                ten = "${getString(R.string.parent_name_cap)} nhắn",
                phu = loiNhan
            ) { KetQuaActivity.mo(this) }
        }

        if (denHen.isNotEmpty()) {
            themViec(
                hinh = R.drawable.st_ic_on_lai,
                mau = R.color.wait,
                ten = if (denHen.size == 1) "Ôn lại 1 câu đến hẹn"
                else "Ôn lại ${denHen.size} câu đến hẹn",
                phu = ToChu.toButDo(
                    this, "Làm trong vở bằng bút đỏ rồi chụp"
                )
            ) {
                startActivity(
                    Intent(this, ChonBaiActivity::class.java)
                        .putExtra(ChonBaiActivity.EXTRA_ON_TAP, true)
                )
            }
        }

        themViecSoan()

        /*
         * Kiem tra bai. Het the hom nay ma con bai phia sau thi dong van o day, dang da
         * xong: day la cua duy nhat vao cho chon "lop da hoc toi bai nao", va an no di
         * thi hom sau lop hoc bai moi con khong co cho nao de mo them. Xem
         * [BoThe.tinhTrangManChinh].
         */
        val kiemTra = runCatching { BoThe.tinhTrangManChinh(this) }
            .getOrDefault(BoThe.TinhTrang.KHONG)
        if (kiemTra != BoThe.TinhTrang.KHONG) {
            val het = kiemTra == BoThe.TinhTrang.HET_HOM_NAY
            themViec(
                hinh = R.drawable.st_ic_the_hoc,
                mau = if (het) R.color.ok else R.color.brand,
                ten = if (het) "Kiểm tra bài: hôm nay hết câu" else getString(R.string.hoc_thuoc_nut),
                phu = when (kiemTra) {
                    BoThe.TinhTrang.CHUA_CHON -> "Chọn bài lớp đã học tới để máy hỏi đúng phần"
                    BoThe.TinhTrang.HET_HOM_NAY -> "Lớp học tới bài mới thì vào chọn lại để có thêm câu"
                    else -> ""
                },
                xong = het
            ) {
                startActivity(Intent(this, HocThuocActivity::class.java))
            }
        }

        /*
         * Do tu vung. Hien khi may co bo tu va hom nay chua do het phan cua ngay.
         *
         * Dong rieng chu khong gop vao dong Kiem tra bai o tren: hai duong hai cai
         * tran, lam het duong nay van con nguyen duong kia. Gop mot dong thi con
         * lam xong mot ben la dong do bien mat, va khong biet ben con lai van con.
         */
        val coTuVung = runCatching {
            val kho = KhoBai.get(this)
            BoTuVung.BO.any { kho.soTuCua(it.bo) > 0 } &&
                kho.giayTuVungTu(moc0Gio()) < LuatTuVung.GIAY_TRAN_MOI_NGAY
        }.getOrDefault(false)
        if (coTuVung) {
            themViec(
                hinh = R.drawable.st_ic_the_hoc,
                mau = MatMon.mau("Tiếng Anh"),
                ten = getString(R.string.do_tu_nut)
            ) {
                startActivity(Intent(this, DoTuVungActivity::class.java))
            }
        }

        val co = binding.boxViec.childCount > 0
        binding.nhanViec.visibility = if (co) View.VISIBLE else View.GONE
        binding.cardViec.visibility = if (co) View.VISIBLE else View.GONE
    }

    /**
     * Dong soan cap cho buoi hoc ke tiep.
     *
     * Soan xong roi thi dong VAN o day, chi doi sang dau tich xanh. An di thi man
     * hinh nhay mot cai roi mat mot dong, ma cai can noi - "khong con no gi" - lai
     * la cai dang duoc noi nhat.
     *
     * Bam vao dong da xong thi chi mo ra xem lai da soan nhung mon gi. Truoc day
     * cho nay bo danh dau ngay khi cham, khong hoi gi ca: dong chu ghi "Da soan cap
     * cho chieu thu tu" va mot dau tich, cham vao de doc ky hon la mat sach, thoat
     * ra la loi nhac soan lai hien len nhu chua tung soan. Muon bat soan lai thi
     * vao trong man soan, o do co nut rieng va co hoi lai.
     */
    private fun themViecSoan() {
        val ke = TinhLoiNhac.buoiKeTiep(Calendar.getInstance()) ?: return
        val (cal, buoi) = ke
        val ma = TinhLoiNhac.maBuoi(cal, buoi)
        val daSoan = ma in prefs.buoiDaSoan
        val mon = buoi.monCanSoan
        if (mon.isEmpty() && !buoi.coTheDuc) return

        val phu = buildString {
            append("Vào học ").append(TinhLoiNhac.gioPhut(buoi.phutVaoHoc))
            append(", không xài máy lúc ")
            append(TinhLoiNhac.gioPhut(ThoiKhoaBieu.phutBuongMay(buoi))).append('.')
            if (mon.isEmpty()) {
                append(" Chỉ cần mang đồ thể dục.")
            } else {
                append(" Cần mang: ").append(mon.joinToString(", "))
                if (buoi.coTheDuc) append(", và đồ thể dục")
                append('.')
            }
            NgayNghi.tenKyNghi(cal)?.let { append(" Hôm đó là ").append(it).append('.') }
        }

        themViec(
            hinh = R.drawable.st_ic_cap_sach,
            mau = if (daSoan) R.color.ok else R.color.brand_dark,
            ten = if (daSoan) "Đã soạn tập cho ${TinhLoiNhac.moTaBuoi(buoi)}"
            else "Soạn tập cho ${TinhLoiNhac.moTaBuoi(buoi)}",
            phu = phu,
            xong = daSoan
        ) {
            startActivity(Intent(this, SoanActivity::class.java))
        }
    }

    /** Mot dong trong danh sach viec. */
    private fun themViec(
        hinh: Int,
        mau: Int,
        ten: String,
        /**
         * Dong chu nho duoi ten viec. De trong thi khong hien dong nao.
         *
         * CharSequence chu khong String: co cau can to mau mot doan, xem [ToChu].
         */
        phu: CharSequence = "",
        xong: Boolean = false,
        mui: Boolean = true,
        bam: () -> Unit
    ) {
        val v = StDongViecBinding.inflate(layoutInflater, binding.boxViec, false)
        v.hinh.setImageResource(hinh)
        v.hinh.imageTintList = ContextCompat.getColorStateList(this, mau)
        v.hinh.backgroundTintList = ContextCompat.getColorStateList(
            this, if (xong) R.color.ok_soft else R.color.canvas
        )
        v.ten.text = ten
        v.phu.text = phu
        // Viec nao nhin ten la biet thi khong can dong giai thich. Van de View do
        // trong layout se chua mot khoang trong ngay giua the.
        v.phu.visibility = if (phu.isBlank()) View.GONE else View.VISIBLE
        // Dau tich thay cho mui ten o dong da xong: khong con cho nao de di toi nua.
        v.duoi.text = if (xong) "✓" else if (mui) "›" else ""
        v.duoi.setTextColor(
            ContextCompat.getColor(this, if (xong) R.color.ok else R.color.ink_soft)
        )
        v.root.setOnClickListener { bam() }
        binding.boxViec.addView(v.root)
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
        // Loc theo quyen co trong may, khong theo dau hai cham: khoa ngoai sach cung co
        // dau do ("tu:..."). Truoc day cau ngoai sach cu nhat dung dau danh sach thi
        // nguon ra "tu", ca lan sua mat pham vi, va cau trong sach bi cham theo duong
        // tu do duoi mot khoa moi - co the duoc tra gio lai nhu bai moi.
        val ids = canSua.map { it.khoa }
            .filter { NganHang.sachTheoNguon(it.substringBefore(':')) != null }
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
        // Dang cho duyet ma chua giu phieu: nut chinh la nop them bai, di tiep xuong
        // duong nop binh thuong. Huy bai vua nop nam o nut phu, xem [render].
        // Hang cho du bai thi nut da tat, nhung man co the chua kip ve lai: chan o day
        // luon, dung de con chup xong ca xap moi biet khong nop duoc.
        if (gate.state == GateState.PENDING && !gate.conChoNopThem()) {
            toast("Đã gửi ${gate.soBaiDangCho()} bài, chờ ba Huy duyệt bớt đã nhé")
            render()
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
     * Nhan cho Ba Huy: mo Telegram ngay khung chat voi ba.
     *
     * Tu 27/9/2026 Le Hoa nhan tin bang Telegram that, tai khoan rieng cua con. Telegram
     * mo duoc luc nao la theo danh sach app Ba Huy dat, nhu moi app khac; Ba Huy bo no
     * vao "Dung moi luc" thi mo duoc ca gio ngu va gio di hoc. Xem [TelegramThat].
     *
     * Man chat cu van mo trong hai truong hop: ba vua nhan qua duong cu (go cho bot hay
     * tab Chat cua Bang dieu khien) ma con chua doc, va tablet chua co Telegram. Bam
     * giu nut cung vao duoc man cu.
     */
    private fun nhanChoBa() {
        if (ChatBox.unread(this) == 0 && TelegramThat.moChatVoiBa(this)) return
        startActivity(Intent(this, ChatActivity::class.java))
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
                val bai = gate.huyBaiMoiNhat()
                val messageId = bai?.messageId ?: 0L
                // Bao ca Bang dieu khien. Khong thi ben do bai nay van ghi dang cho, con
                // hai nut Duyet, Khong duyet thi bam nut nao tablet cung khong con bai de lam.
                bai?.let { DongBo.datTrangThaiBai(this, it.id, "HUY") }
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

    /** Moc 0h hom nay, de hoi cac tran tinh theo ngay duong lich. */
    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
