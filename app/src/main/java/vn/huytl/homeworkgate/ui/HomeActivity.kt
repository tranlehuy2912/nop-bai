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
import kotlinx.coroutines.withContext
import android.widget.Toast
import android.widget.LinearLayout
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.BaiGuiHong
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.NhacBai
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.Mang
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.QuyGio
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
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
import vn.huytl.homeworkgate.guard.TrinhPhat
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
    private val ngheDoi = SharedPreferences.OnSharedPreferenceChangeListener { _, khoa ->
        tay.removeCallbacks(veLai)
        tay.postDelayed(veLai, 300L)
        // Vo dan do vua luu, tin cua co vua toi, hay moc "lop da hoc toi" vua doi: xem co
        // can mo de on truoc kiem tra khong. Hoan lau hon lan ve, va bo qua luc dang choi.
        // Chi nghe dung may khoa do: nhat ky, gio cua app han rieng... ghi vao file nay
        // vai giay mot lan, moi lan lai quet ca kho SBT de ra de.
        if (khoa == null || khoa == "vo_dan_do" || khoa == "tin_cua_co" || khoa.startsWith("hoc_toi_")) {
            tay.removeCallbacks(moDe)
            tay.postDelayed(moDe, 2_000L)
        }
    }

    private val moDe = Runnable { if (!gate.isOpen()) moDeNeuCan() }

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
        moDeNeuCan()
    }

    /**
     * Mo de Giai de den luc (sang thu Bay, hay vo dan do bao sap kiem tra). Chay ngoai
     * luong giao dien vi doc ca kho sach bai tap; co de moi thi ve lai danh sach viec.
     */
    private fun moDeNeuCan() {
        lifecycleScope.launch(Dispatchers.IO) {
            val moi = runCatching { GiaiDe.taoNeuCan(this@HomeActivity) }.getOrDefault(emptyList())
            if (moi.isNotEmpty()) withContext(Dispatchers.Main) { render() }
        }
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
        tay.removeCallbacks(moDe)
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
        // Dong chu nho duoi chu to chi con khi co dieu dong to chua noi: ten tung viec
        // luc giao nhieu viec, va so phut con lai luc Ba Huy cam may. Nhung cau giai
        // thich tung nam o day (bam gi, cho gi, duoc toi da bao nhieu phut) Ba Huy thay
        // thua, bo ngay 27/9/2026.
        var chiTiet = ""
        when {
            baDangDung -> {
                doiMat(R.drawable.ic_mat_mo_khoa, R.color.parent_tint, R.color.parent_soft)
                binding.txtBadge.text = "Máy đang mở"
                binding.txtState.text = getString(R.string.parent_name_cap)
                chiTiet = ParentMode.moTa(this).replaceFirstChar { it.uppercase() }
            }
            conViec.isNotEmpty() -> {
                doiMat(R.drawable.ic_mat_viec_nha, R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = "${ViecNha.nguoiGiao(phienViec)} giao việc"
                if (conViec.size == 1) {
                    binding.txtState.text = conViec.first().ten
                } else {
                    binding.txtState.text = "Còn ${conViec.size} việc"
                    chiTiet = conViec.joinToString(", ") { it.ten }
                }
            }
            gate.state == GateState.PENDING && gate.grantedMinutes > 0 -> {
                // Nop them bai de cong don, ma trong tay van con phieu cu.
                doiMat(R.drawable.ic_mat_cho, R.color.brand, R.color.brand_soft)
                binding.txtBadge.text =
                    if (soBaiCho > 1) "Đã gửi thêm $soBaiCho bài" else "Đã gửi thêm bài"
                binding.txtState.text = "${gate.grantedMinutes} phút"
            }
            gate.state == GateState.PENDING -> {
                doiMat(R.drawable.ic_mat_cho, R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = if (soBaiCho > 1) "Đã gửi $soBaiCho bài" else "Đã gửi bài"
                binding.txtState.text = "Chờ ba Huy duyệt"
            }
            gate.state == GateState.GRANTED -> {
                doiMat(R.drawable.ic_mat_da_duyet, R.color.brand, R.color.brand_soft)
                // Bo trong la Ba Huy duyet, duong di cua gan het moi phieu gio.
                binding.txtBadge.text = gate.nhanCho
                    .ifEmpty { "${getString(R.string.parent_name_cap)} đã duyệt" }
                binding.txtState.text = "${gate.grantedMinutes} phút"
            }
            gate.state == GateState.PAUSED -> {
                doiMat(R.drawable.ic_mat_tam_dung, R.color.wait, R.color.wait_soft)
                binding.txtBadge.text = "Đang tạm dừng"
                // Den tung giay, giong het luc dang choi: so nay la so se chay tiep
                // khi bam "Choi tiep", nen hien khac di la sau do thay hut mat may
                // chuc giay.
                binding.txtState.text = CountdownOverlay.dinhDang(gate.pausedMs())
            }
            gate.isOpen() -> {
                doiMat(R.drawable.ic_mat_dang_choi, R.color.ok, R.color.ok_soft)
                binding.txtBadge.text = "Đang được chơi"
                // Dem den tung giay: nhin mot cai la biet con bao lau, khong phai
                // doan giua "con 1 phut" va "con 59 giay".
                binding.txtState.text = CountdownOverlay.dinhDang(gate.remainingMs())
            }
            else -> {
                doiMat(R.drawable.ic_mat_khoa, R.color.locked, R.color.locked_soft)
                binding.txtBadge.text = "Đang khoá"
                binding.txtState.text = "Chưa nộp bài"
            }
        }
        binding.txtDetail.text = chiTiet
        // TextView rong ma de hien van chiem mot dong, cong khoang cach phia tren.
        binding.txtDetail.visibility = if (chiTiet.isEmpty()) View.GONE else View.VISIBLE

        // Het tran trong ngay thi nut nop bai khong lam duoc gi ngoai hien mot cau
        // toast. De no sang xanh nhu binh thuong la moi con bam di bam lai roi tuong
        // may hong.
        // Khong con tran ngay chan nut nay (29/9/2026). Bai chup anh hom nay da du tran thi
        // nop them bai chup khong duoc gi, nen nut chinh doi thanh lam bai tren may - xem
        // [LamBaiActivity]. Truoc 30/9/2026 moc doi la luc da tinh tron goi vo dan do.
        val hetLuot = false
        val hetTranAnh = SoCaiBai.hetTranAnhHomNay(this)
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
        val hetTran = false
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
            hetTranAnh -> "Luyện tập trên máy"
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
            dangCoGi && !hetTranAnh && gate.conChoNopThem() -> View.VISIBLE
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

        // So tin Telegram cua ba ma con chua doc, dem theo thong bao cua Telegram. Mat
        // quyen doc thong bao thi so trong prefs dung yen o lan dem cuoi, nen khong hien.
        val chuaDoc = if (TrinhPhat.coQuyen(this)) TinCuaBa.so(this) else 0
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
        // Tran ngay bang tong tran rieng cua moi phan (29/9/2026).
        val tran = LuatCongGio.TRAN_NGAY
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
            if (chia.isNotEmpty()) append(" (").append(chia.joinToString(", ")).append(")")
            val quy = QuyGio.so(this@HomeActivity)
            if (quy > 0) append(" · Quỹ giờ chơi ").append(quy).append(" phút")
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
     * Thu tu Ba Huy chot 30/9/2026: chup vo dan do, soan tap, bai dan do cho buoi sap
     * toi, cau can sua, Luyen tap. Truoc do cau can sua dung tren soan tap. Lan nop gui
     * hong thi van dung truoc het, xem ben duoi.
     */
    private fun veViecHomNay(baDangDung: Boolean) {
        binding.boxViec.removeAllViews()
        if (baDangDung) {
            binding.nhanViec.visibility = View.GONE
            binding.cardViec.visibility = View.GONE
            return
        }

        /*
         * Cau con phai sua, tru cau con da nop lai va dang cho cham: dem ca nhung cau do thi
         * con vua nop xong van thay "Có 2 câu cần sửa" va tuong minh chua gui. Xem
         * [vn.huytl.homeworkgate.data.KhaiChoCham.cauChoCham]. Hang cho rong thi ham do tra
         * ve ngay, khong hoi kho.
         */
        // Bai chup anh da du tran hom nay thi khong nop lai cau sai bang anh nua: sua xong cung
        // khong ra phut. Cau sai trong ngan hang chuyen sang lam tren may sau 24 gio, xem
        // [LamTrenMay]. Truoc 30/9/2026 moc nay la luc da tinh tron goi.
        val hetTranAnh = SoCaiBai.hetTranAnhHomNay(this)
        val canSua = if (hetTranAnh) emptyList() else SoCaiBai.canSua(this).let { ds ->
            val cho = vn.huytl.homeworkgate.data.KhaiChoCham.cauChoCham(this)
            if (cho.isEmpty()) ds else ds.filterNot { it.khoa in cho }
        }
        val loiNhan = SoCaiBai.loiNhan(this)

        /*
         * Bai da lam xong ma chua toi tay Ba Huy: dung truoc het, truoc ca dong cau can sua.
         *
         * Lan 21:13 ngay 28/9/2026 la mot lan nop lai cau sai gui hong. Loi nhan "Gửi không
         * được" cu nam o nhanh else ben duoi dong cau can sua, ma cau chua gui duoc thi van la
         * cau can sua, nen Le Hoa khong bao gio thay no.
         */
        BaiGuiHong.cacLan(this).forEach { themViecGuiHong(it) }

        /*
         * Vo dan do nam ngay man chinh (29/9/2026), khong trong man Nop bai: tu 30/9/2026 vo
         * khong con dinh gi toi cham bai, no de nhac bai va de mo de on truoc kiem tra.
         *
         * Ten luon la "Chụp vở dặn dò" (30/9/2026): may giu nhieu trang, moi ngay mot trang,
         * nen khong con "vo cua hom nay" de ghi len ten. Cac bai sap toi da hien o cac dong
         * nhac bai ben duoi. Dau tich la hom nay con da luu mot trang, tinh theo ngay bam
         * Luu, de con biet minh chup chua.
         */
        val daLuu = VoDanDo.daLuuHomNay(this)
        themViec(
            hinh = R.drawable.st_ic_dau_hoi,
            mau = R.color.brand,
            ten = "Chụp vở dặn dò",
            phu = if (daLuu) "Đã chụp vở hôm nay" else "",
            xong = daLuu
        ) { startActivity(Intent(this, DanDoActivity::class.java)) }
        themViecSoan()
        themViecNhacBai()

        if (canSua.isNotEmpty()) {
            val ke = canSua.take(3).joinToString(", ") { it.ma }
            /*
             * Bam vao chi mo man ket qua. Nut nop lai nam tren the cua tung bai o do.
             *
             * Truoc 18/9/2026 dong nay mo thang camera: con biet cau nao sai ma khong biet
             * sai o dau. Tu do toi 28/9/2026 no mo man ket qua kem mot nut chup lai chung o
             * cuoi, gom cau sai cua moi bai. Ba Huy bo nut chung ay: moi bai nop lai bang
             * nut tren the cua chinh no, xem [KetQuaActivity].
             */
            themViec(
                hinh = R.drawable.st_ic_dau_hoi,
                mau = R.color.alert,
                ten = "Có ${canSua.size} câu cần sửa",
                phu = "Xem sai ở đâu: " + ke + if (canSua.size > 3) "…" else ""
            ) { KetQuaActivity.mo(this) }
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

        /*
         * De Giai de da bat dau (dang chay gio, hay con chup phan tu luan) o lai day: giau vao
         * trang Luyen tap thi con de quen mot de dang tinh gio. De chua bat dau va de da
         * xong thi nam trong trang do.
         */
        if (!gate.isOpen()) themViecGiaiDeDangLam()

        themViecLuyenTap(demSo = canSua.isEmpty() && !gate.isOpen())

        val co = binding.boxViec.childCount > 0
        binding.nhanViec.visibility = if (co) View.VISIBLE else View.GONE
        binding.cardViec.visibility = if (co) View.VISIBLE else View.GONE
    }

    /** Cac mon co sach bai tap lam tren may trong kho. */
    private fun monLamTrenMay(): List<String> =
        LamTrenMay.MON.filter { NganHang.sachBaiTapCua(it).isNotEmpty() }

    /** Hoi mon roi mo man lam bai tren may. Chi mot mon thi mo thang. */
    private fun hoiMonLamTrenMay() {
        val mon = monLamTrenMay()
        if (mon.isEmpty()) return
        if (mon.size == 1) return LamBaiActivity.moLamThem(this, mon.first())
        MaterialAlertDialogBuilder(this)
            .setTitle("Luyện tập môn gì?")
            .setItems(mon.toTypedArray()) { _, i -> LamBaiActivity.moLamThem(this, mon[i]) }
            .show()
    }

    /**
     * Moi buoi co dong dan do den han mai (hay som hon) mot dong: "Bài dặn dò cho chiều thứ
     * bảy".
     *
     * Nhac truoc mot ngay, cho tiet sau cua dung mon do (Ba Huy chon 30/9/2026), xem
     * [NhacBai]. Dong chu nho ke tung dong dan do; bam vao thi mo man Bài dặn dò sắp tới
     * ([NhacBaiActivity]), cung danh sach voi the tren Bang dieu khien. Truoc 30/9/2026 bam
     * vao chi hien mot hop thoai cua rieng buoi do.
     */
    private fun themViecNhacBai() {
        val cac = runCatching { NhacBai.canNhac(this) }.getOrDefault(emptyList())
        cac.forEach { n ->
            // Moi dong mot dau "•", bai tap hay dan do khac cung vay (Ba Huy chon 30/9/2026).
            val phu = (n.cacBai + n.dongKhac).joinToString("\n") { "• " + it.chu }
            themViec(
                hinh = R.drawable.st_ic_lich,
                mau = R.color.wait,
                ten = "Bài dặn dò cho " + TinhLoiNhac.moTaBuoi(n.buoi),
                phu = phu
            ) { startActivity(Intent(this, NhacBaiActivity::class.java)) }
        }
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

        // Bai trong vo dan do han dung buoi nay, xem [NhacBai]. Dong "Bài dặn dò cho ..." ngay
        // duoi da ke tung bai; o day chi nhac so bai, de soan tap thi nho ca vo bai tap.
        val soBai = runCatching {
            NhacBai.choBuoi(this, cal, buoi).count { it.laBaiTap }
        }.getOrDefault(0)
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
            if (soBai > 0) append(" Có $soBai bài phải làm cho buổi này.")
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

    /**
     * De Giai de con da bat dau ma chua xong: dang lam (con bao nhieu phut), hay da nop phan
     * trac nghiem ma con chup phan tu luan. Xem [GiaiDe].
     *
     * Chi nhung de nay o lai man chinh (30/9/2026). De chua bat dau, de da xong kem diem, va
     * dong "Giai de <mon>" hoi moc lop da hoc nam trong [LuyenTapActivity].
     *
     * Khong hoi trong luc dang choi: ham nay chay moi giay khi dong ho dang dem.
     */
    private fun themViecGiaiDeDangLam() {
        val bayGio = System.currentTimeMillis()
        val mo = runCatching { GiaiDe.dangMo(this, bayGio) }.getOrDefault(emptyList())
        mo.filter { it.daBatDau }.forEach { de ->
            val phu = if (!de.daNop) {
                val con = de.phutGoiY - ((bayGio - de.batDau) / 60_000L).toInt()
                if (con >= 0) "Đang làm, còn $con phút" else "Đang làm, quá ${-con} phút"
            } else {
                "Còn chụp phần tự luận"
            }
            themViec(
                hinh = R.drawable.st_ic_giai_de,
                mau = MatMon.mau(de.mon),
                ten = GiaiDe.tenDe(de),
                phu = phu
            ) { GiaiDeActivity.mo(this, de.id) }
        }
    }

    /**
     * Mot dong "Luyện tập" thay cho nam dong cu: Lam bai tren may, On lai, Giai de, Kiem tra
     * bai, Do tu vung (Ba Huy chot 30/9/2026). Moi mon, moi de them vao la them dong, nen
     * khoi Viec hom nay cu dai ra. Ben trong la [LuyenTapActivity].
     *
     * [demSo] false thi khong ghi dong phu. Hai luc: dang choi, vi ham nay chay moi giay
     * ma dem cau on la mot cau GROUP BY tren ca bang tra loi; va con cau can sua, vi sua bai
     * dang lam do truoc, on lai la viec cua hom nao ranh (giu dung luat cua dong On lai cu).
     */
    private fun themViecLuyenTap(demSo: Boolean) {
        val phu = if (!demSo) "" else runCatching {
            buildList {
                LamTrenMay.soCauOn(this@HomeActivity).takeIf { it > 0 }?.let { add("ôn $it câu") }
                GiaiDe.dangMo(this@HomeActivity).count { !it.daBatDau }.takeIf { it > 0 }
                    ?.let { add("$it đề đang mở") }
                if (BoThe.tinhTrangManChinh(this@HomeActivity) == BoThe.TinhTrang.CO_THE) add("kiểm tra bài")
            }.joinToString(" · ").replaceFirstChar { it.uppercase() }
        }.getOrDefault("")
        themViec(
            hinh = R.drawable.st_ic_the_hoc,
            mau = R.color.brand,
            ten = "Luyện tập",
            phu = phu
        ) { LuyenTapActivity.mo(this) }
    }

    /**
     * Mot lan nop gui khong xong, anh con giu trong may. Xem [BaiGuiHong].
     *
     * Bam vao thi hoi Gui lai hay Bo anh chu khong gui ngay: co khi Le Hoa muon chup lai
     * cho ro hon, va luc do anh cu phai bo di, khong thi dong nay nam do ca ngay.
     */
    private fun themViecGuiHong(lan: BaiGuiHong.Lan) {
        val gio = BaiGuiHong.gioPhut(lan.lucNop)
        if (BaiGuiHong.dangGui(lan)) {
            // Service chua kip nhan viec thi het khoang cho phai ve lai, khong thi dong nam
            // mai o "Đang gửi lại" du khong con ai gui.
            val con = BaiGuiHong.conChoService(lan)
            if (con > 0L) tay.postDelayed(veLai, con + 200L)
            themViec(
                hinh = R.drawable.st_ic_dong_ho,
                mau = R.color.wait,
                ten = "Đang gửi lại bài lúc $gio",
                phu = keTrang(lan),
                mui = false
            ) { toast("Đang gửi, đợi một chút") }
            return
        }
        themViec(
            hinh = R.drawable.st_ic_dau_hoi,
            mau = R.color.alert,
            ten = "Bài lúc $gio chưa gửi được",
            phu = keTrang(lan)
        ) { hoiGuiLai(lan.ma) }
    }

    private fun hoiGuiLai(ma: String) {
        val lan = BaiGuiHong.lay(this, ma) ?: return render()
        if (BaiGuiHong.dangGui(lan)) return toast("Đang gửi, đợi một chút")
        MaterialAlertDialogBuilder(this)
            .setTitle("Bài lúc ${BaiGuiHong.gioPhut(lan.lucNop)} chưa gửi được")
            .setMessage(
                keTrang(lan) + "." +
                    if (lan.soLan > 1) " Đã gửi ${lan.soLan} lần chưa được." else ""
            )
            .setPositiveButton("Gửi lại") { _, _ -> guiLai(ma) }
            .setNegativeButton("Bỏ ảnh") { _, _ -> boAnhGuiHong(ma) }
            .setNeutralButton("Để sau", null)
            .show()
    }

    private fun guiLai(ma: String) {
        val lan = BaiGuiHong.lay(this, ma) ?: return render()
        // Hop hoi mo tu truoc, trong luc do mot lan bam khac da gui roi.
        if (BaiGuiHong.dangGui(lan)) return toast("Đang gửi, đợi một chút")
        val gon = BaiGuiHong.lamGon(this, ma)
        if (gon == null) {
            // He dieu hanh don cacheDir luc may day bo nho: anh bai giai lan do khong con.
            BaiGuiHong.bo(this, ma)
            DayLog.add(this, "Bỏ bài gửi hỏng lúc ${BaiGuiHong.gioPhut(lan.lucNop)}: ảnh không còn trong máy")
            runCatching { DongBo.dayNgay() }
            toast("Ảnh lần đó không còn, ${getString(R.string.child_name)} chụp lại nhé")
            return
        }
        val pham = PhamVi.tuJson(gon.pham)
        if (!gate.conChoNop(pham)) {
            toast("Đã gửi đủ bài, chờ ba Huy duyệt bớt rồi bấm Gửi lại")
            return
        }
        BaiGuiHong.danhDauBamGui(this, ma)
        ApprovalService.ensureRunning(this)
        ApprovalService.guiBai(this, gon.anh, pham, gon.lucNop)
        toast("Đang gửi lại cho ${getString(R.string.parent_name)}")
    }

    private fun boAnhGuiHong(ma: String) {
        val lan = BaiGuiHong.lay(this, ma) ?: return render()
        // Bo anh giua luc dang gui thi lan dang gui hong vi mat anh.
        if (BaiGuiHong.dangGui(lan)) return toast("Đang gửi, đợi một chút")
        BaiGuiHong.bo(this, ma)
        // Ghi vao nhat ky: Ba Huy biet anh cua lan do ben Telegram (neu co) khong con tinh.
        DayLog.add(
            this,
            "Bỏ ảnh bài gửi hỏng lúc ${BaiGuiHong.gioPhut(lan.lucNop)} (${BaiGuiHong.keAnh(this, lan.anh)})"
        )
        runCatching { DongBo.dayNgay() }
        toast("Đã bỏ ảnh")
    }

    /** "Đề bài 5 trang, bài giải 4 trang". */
    private fun keTrang(lan: BaiGuiHong.Lan): String =
        lan.anh.entries.mapIndexed { i, (st, files) ->
            val ten = getString(st.labelRes)
            (if (i == 0) ten else ten.lowercase()) + " ${files.size} trang"
        }.joinToString(", ")

    /** Mot dong trong danh sach viec. */
    private fun themViec(
        hinh: Int,
        mau: Int,
        ten: String,
        /** Dong chu nho duoi ten viec. De trong thi khong hien dong nao. */
        phu: String = "",
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
        // Bai chup anh hom nay da du tran: nop them bai chup khong duoc gi, nut chinh la lam
        // bai tren may. Lam tren may khong can mang.
        if (SoCaiBai.hetTranAnhHomNay(this)) {
            hoiMonLamTrenMay()
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
     * Tu 27/9/2026 Le Hoa nhan tin bang Telegram that, tai khoan rieng cua con, va man
     * chat trong app da bo. Telegram mo duoc luc nao la theo danh sach app Ba Huy dat,
     * nhu moi app khac; Ba Huy bo no vao "Dung moi luc" thi mo duoc ca gio ngu va gio di
     * hoc. Xem [TelegramThat].
     *
     * Khong mo duoc - tablet chua cai Telegram, hay chat id cai luc dat bot khong phai
     * cua mot nguoi - thi noi thang ra, chu khong de con bam ma khong thay gi.
     */
    private fun nhanChoBa() {
        if (TelegramThat.moChatVoiBa(this)) return
        val ba = getString(R.string.parent_name_cap)
        Toast.makeText(
            this,
            "Chưa mở được Telegram. Nhờ $ba xem giúp.",
            Toast.LENGTH_LONG
        ).show()
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
                if (PhienQuanLy.thuPin(this, input.text.toString())) {
                    // Chi mo trang cau hinh, KHONG mo khoa may. Muon mo khoa thi
                    // gat cong tac trong do. Truoc day hai viec nay tron lam mot,
                    // nen chi vao xem lai gio nghi cung lam tablet mo toang.
                    ApprovalService.ensureRunning(this)
                    startActivity(Intent(this, ParentActivity::class.java))
                } else {
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
        // Nho dung bai luc mo hop. Trong luc hop dang mo, ba co the vua duyet bai do ben
        // Telegram: luc bam xac nhan ma lay "bai moi nhat" thi se huy nham bai nop truoc.
        val muonHuy = gate.baiDangCho().lastOrNull()?.id ?: return
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
                val bai = gate.baiDangCho().firstOrNull { it.id == muonHuy }
                if (bai == null) {
                    toast("Bài đó ba Huy đã xử lý rồi, không huỷ nữa")
                    render()
                    return@setPositiveButton
                }
                gate.boBaiCho(bai.id)
                val messageId = bai.messageId
                // Bao ca Bang dieu khien. Khong thi ben do bai nay van ghi dang cho, con
                // hai nut Duyet, Khong duyet thi bam nut nao tablet cung khong con bai de lam.
                DongBo.datTrangThaiBai(this, bai.id, "HUY")
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
