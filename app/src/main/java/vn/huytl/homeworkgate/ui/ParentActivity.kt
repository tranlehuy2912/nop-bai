package vn.huytl.homeworkgate.ui

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.databinding.ActivityParentBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.PhienQuanLy
import vn.huytl.homeworkgate.guard.Permissions
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier
import vn.huytl.homeworkgate.telegram.TelegramClient

/**
 * Bang dieu khien cua Ba Huy ngay tren may, cho luc dang cam tablet trong tay va
 * khong muon moc dien thoai ra go lenh Telegram.
 *
 * Vao duoc day nghia la da qua PIN o man chon nguoi dung, nen trong nay khong hoi
 * PIN them lan nao nua.
 */
class ParentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentBinding
    private lateinit var prefs: Prefs
    private lateinit var gate: GateStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs.get(this)
        gate = GateStore(this)
        chuaThanhHeThong()

        binding.btnDone.setOnClickListener { traMay() }
        binding.swUnlock.setOnCheckedChangeListener { nut, bat ->
            // Chi lam gi khi chinh nguoi dung gat. render() cung dat lai trang thai
            // cong tac, va neu khong loc thi no tu kich hoat lai chinh no.
            if (!nut.isPressed) return@setOnCheckedChangeListener
            if (bat) ParentMode.enable(this) else ParentMode.disable(this)
            // Bao Telegram ngay tai day chu khong o cho nhap PIN: day moi la luc may
            // that su thoi chan. Nhap PIN dung ma khong gat cong tac thi may van khoa.
            Notifier.moToanBo(this, bat)
            ApprovalService.ensureRunning(this)
            render()
        }
        binding.btnGrant.setOnClickListener { moGioChoi(truLuot = true) }
        binding.btnGift.setOnClickListener { moGioChoi(truLuot = false) }
        binding.btnLock.setOnClickListener { dungGioChoi() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
        binding.btnAllowUninstall.setOnClickListener { choGoApp() }
        binding.btnNoiDienThoai.setOnClickListener { noiDienThoai() }
        binding.btnMoChan.setOnClickListener { doiManChan() }
        binding.cardThongKe.setOnClickListener {
            startActivity(Intent(this, ThongKeActivity::class.java))
        }
    }

    /**
     * Hien hai dong ma de go sang app Bang dieu khien tren dien thoai Ba Huy.
     *
     * Ma nha la ten cai cho tren Firestore, khong doi bao gio. Ma sau so thi het han
     * sau muoi phut - nho the, mot tam anh chup man hinh nay tu tuan truoc khong con
     * dung duoc de noi mot may la vao nha.
     *
     * Man hinh nay nam sau PIN nen chi Ba Huy mo duoc; du vay van de o day chu khong
     * tu dong ghep khi thay may moi: cai gi cho them mot nguoi quyen dieu khien
     * tablet thi phai co mot lan bam tay.
     */
    private fun noiDienThoai() {
        if (!DongBo.san(this)) {
            toast("Bản app này chưa nối Firebase")
            return
        }
        toast("Đang lấy mã…")
        DongBo.maGhepMoi(this) { maNha, maGhep, loi ->
            if (maNha.isEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Chưa lấy được mã")
                    .setMessage(loi)
                    .setPositiveButton("Xong", null)
                    .show()
                return@maGhepMoi
            }
            MaterialAlertDialogBuilder(this)
                .setTitle("Nối điện thoại ba Huy")
                .setMessage(
                    "Mở app Bảng điều khiển trên điện thoại, gõ hai dòng này:\n\n" +
                        "Mã nhà:\n$maNha\n\n" +
                        "Mã ghép:\n$maGhep\n\n" +
                        "Mã ghép sống 10 phút. Hết thì bấm lại là ra mã mới."
                )
                .setPositiveButton("Xong", null)
                .show()
        }
    }

    /**
     * Mo hoac chan lai man chan gio di hoc, cho dung buoi dang dien ra.
     *
     * Chi cho dung buoi nay: sang buoi sau man chan lam viec binh thuong tro lai,
     * khong phai nho bat lai. Cong tac bat/tat han nam trong [SetupActivity], vi do
     * la thu dat mot lan; cai o day la viec cua hom nay.
     */
    private fun doiManChan() {
        val now = java.util.Calendar.getInstance()
        val phut = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            now.get(java.util.Calendar.MINUTE)
        val buoi = TinhLoiNhac.buoiDangTrongGioChan(now, phut) ?: return
        val ma = TinhLoiNhac.maBuoi(now, buoi)

        if (prefs.buoiDuocMoSom == ma) {
            prefs.buoiDuocMoSom = ""
            toast("Đã chặn lại")
        } else {
            prefs.buoiDuocMoSom = ma
            toast("Đã mở cho hết ${TinhLoiNhac.moTaBuoi(buoi)}")
        }
        ApprovalService.ensureRunning(this)
        render()
    }

    /** The man chan, chi hien trong khoang dang bi chan. */
    private fun veManChan() {
        val now = java.util.Calendar.getInstance()
        val phut = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            now.get(java.util.Calendar.MINUTE)
        val buoi = if (prefs.batManChan) {
            TinhLoiNhac.buoiDangTrongGioChan(now, phut)
        } else {
            null
        }
        if (buoi == null) {
            binding.cardManChan.visibility = View.GONE
            return
        }
        binding.cardManChan.visibility = View.VISIBLE

        val daMo = prefs.buoiDuocMoSom == TinhLoiNhac.maBuoi(now, buoi)
        binding.txtManChan.text = if (daMo) {
            "Đang mở trong giờ đi học"
        } else {
            "Đang chặn vì tới giờ đi học"
        }
        binding.txtManChanDetail.text = buildString {
            append(TinhLoiNhac.moTaBuoi(buoi).replaceFirstChar { it.uppercase() })
            append(", tan học ").append(TinhLoiNhac.gioPhut(buoi.phutTanHoc)).append(". ")
            append(if (daMo) "Buổi học sau lại chặn bình thường." else "Mở chỉ cho buổi này.")
        }
        binding.btnMoChan.text = if (daMo) "Chặn lại" else "Mở cho hết buổi này"
    }

    override fun onResume() {
        super.onResume()
        if (PhienQuanLy.phaiVeManCon()) {
            veManCon()
            return
        }
        gate.tick()
        render()
    }

    /** Dong trang cau hinh, tra man hinh ve cho Le Hoa, va bat hoi lai PIN. */
    private fun veManCon() {
        PhienQuanLy.daQuaPin = false
        PhienQuanLy.thoiMoCaiDat()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    /**
     * Roi khoi trang cau hinh bang duong nao cung phai dong lai nhu nhau.
     *
     * Truoc day chi nut "Xong" moi xoa co da-qua-PIN va dong duong vao Cai dat. Bam
     * nut Back cua he thong thi hai cai do con nguyen: may van cho vao thang trang
     * Cai dat khong hoi PIN, va cua vao Settings con mo them 10 phut nua.
     */
    override fun onDestroy() {
        if (isFinishing) {
            PhienQuanLy.daQuaPin = false
            PhienQuanLy.thoiMoCaiDat()
        }
        super.onDestroy()
    }

    private fun render() {
        // Noi ro day la dong ho cua che do Ba Huy, khong phai gio choi cua Le Hoa.
        // Cau cu "May dang mo het, con 30 phut" de bi doc nham thanh so phut con lai
        // cua dua tre.
        val dangMo = ParentMode.isActive(this)
        binding.swUnlock.isChecked = dangMo
        binding.txtUnlockDetail.text = if (dangMo) {
            // Gach ngang chu khong cham: moTa() tra ve cau bat dau bang chu thuong
            // ("khong dat han..."), sau dau cham thi nhin nhu loi chinh ta.
            "Mọi chặn đang tắt, kể cả Cài đặt — ${ParentMode.moTa(this)}. " +
                "Gạt lại để khoá."
        } else {
            "Gạt để tắt hết chặn khi ba Huy cần dùng máy. " +
                "Vào trang này không tự mở khoá."
        }

        val child = getString(R.string.child_name)
        when {
            gate.state == GateState.PENDING -> {
                val so = gate.soBaiDangCho()
                binding.txtState.text = "$child đang chờ duyệt"
                binding.txtDetail.text = if (so > 1) {
                    "$so bài đã gửi sang Telegram, duyệt từng bài một"
                } else {
                    "Ảnh bài tập đã gửi sang cho ba Huy"
                }
            }
            gate.state == GateState.GRANTED -> {
                binding.txtState.text = "Đã duyệt ${gate.grantedMinutes} phút"
                binding.txtDetail.text = "$child chưa bấm chơi nên chưa tính giờ"
            }
            gate.isOpen() -> {
                binding.txtState.text = "$child đang chơi"
                binding.txtDetail.text = "Còn ${gate.remainingMs() / 60_000 + 1} phút"
            }
            else -> {
                binding.txtState.text = "Máy đang khoá với $child"
                binding.txtDetail.text =
                    "Hôm nay còn duyệt được ${gate.phutConLaiHomNay()} phút cho bài tập"
            }
        }

        // Nut cho khong an vao tran trong ngay, nen no van dung duoc khi bai tap da
        // dung het tran.
        binding.btnGift.text = if (gate.state == GateState.ACTIVE) {
            "Cộng thêm giờ (không trừ hạn mức)"
        } else {
            "Cho chơi luôn, không cần nộp bài"
        }

        // Khong co phien nao thi khong co gi de dung: an nut di cho trang con mot
        // nut khoa duy nhat la cai cong tac o tren.
        binding.btnLock.visibility =
            if (gate.state == GateState.LOCKED) View.GONE else View.VISIBLE

        veManChan()
        veThongKe()
        veCanhBao()

        binding.txtNote.text = if (Permissions.hasDeviceAdmin(this)) {
            "Đang bật quản trị thiết bị nên không ai gỡ được app, kể cả ba Huy. " +
                "Muốn gỡ thì bấm \"Cho phép gỡ app\" ở trên."
        } else {
            "Quản trị thiết bị đang TẮT — lúc này Lê Hòa cũng gỡ được app. " +
                "Gỡ xong nhớ vào Cài đặt bật lại."
        }
    }

    /**
     * Mot dong ve hom nay con dung may vao viec gi.
     *
     * Chi ba app dau: dong nay de liec, khong phai de doc. Muon day du thi bam vao
     * the, sang [ThongKeActivity] co ca tung khoang gio.
     */
    private fun veThongKe() {
        val cac = NhatKySuDung.theoApp(this, 0)
        if (cac.isEmpty()) {
            binding.txtThongKe.text = "Hôm nay chưa ghi được app nào"
            binding.txtThongKeDetail.text =
                "Máy ghi lại ${getString(R.string.child_name)} mở app nào, " +
                    "từ mấy giờ tới mấy giờ. Bấm để xem cả tuần."
            return
        }
        binding.txtThongKe.text =
            "Hôm nay dùng máy ${NhatKySuDung.moTaDoDai(NhatKySuDung.tongMs(this, 0))}"
        binding.txtThongKeDetail.text = cac.take(3).joinToString(" · ") {
            "${NhatKySuDung.tenApp(this, it.goi)} ${NhatKySuDung.moTaDoDai(it.tongMs)}"
        }
    }

    /**
     * Bang "con thieu gi", giong het ben Cai dat.
     *
     * O day no quan trong hon ca: Ba Huy mo trang nay ra la luc dang cam may trong
     * tay, sua duoc ngay. Truoc day trang nay khong bao gi, chi co mot dong ve
     * quan tri thiet bi o duoi cung.
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
     * Bam "Xong": chi dong trang cau hinh, khong doi gi khac.
     *
     * Truoc day cho nay tat luon che do mo may. Dung vao thoi trang nay con la "che
     * do Ba Huy": vao trang la may mo, ra khoi trang la may khoa lai. Gio cong tac
     * "Mo toan bo may" moi la thu quyet dinh, nen ra khoi trang ma tat theo thi Ba
     * Huy vua gat cong tac xong, bam Xong mot cai la may khoa lai ngay - cong tac
     * bat len de lam gi nua.
     *
     * Muon khoa lai thi gat cong tac, hoac bam "Khoa ngay" neu muon cat luon gio
     * choi cua con. Ra khoi day van phai nhap PIN lai lan sau.
     */
    private fun traMay() = veManCon()

    /**
     * Hoi so phut truoc khi duyet.
     *
     * Ben Telegram co hang nut chon nhanh, ben may thi truoc day chi cap dung so
     * mac dinh, muon khac phai vao Cai dat sua. Hai duong dieu khien nen giong nhau.
     *
     * Dang choi thi hop thoai doi thanh cong them, vi luc do y cua ba gan nhu chac
     * chan la cho them chu khong phai cap lai tu dau.
     */
    private fun moGioChoi(truLuot: Boolean) {
        val dangChoi = gate.state == GateState.ACTIVE
        val macDinh = prefs.grantMinutes
        // Dua so mac dinh len dau va danh dau, de chin lan muoi chi can bam dong dau.
        val lua = (listOf(macDinh) + listOf(15, 30, 45, 60, 90).filter { it != macDinh })
        val nhan = lua.mapIndexed { i, phut ->
            if (i == 0) "$phut phút  (mặc định)" else "$phut phút"
        }.toTypedArray()

        val tieuDe = when {
            dangChoi -> "Cộng thêm bao nhiêu phút?"
            truLuot -> "Duyệt bao nhiêu phút?"
            else -> "Cho chơi bao nhiêu phút?"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(tieuDe)
            .setItems(nhan) { _, which -> capGio(lua[which], dangChoi, truLuot) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun capGio(phut: Int, dangChoi: Boolean, truLuot: Boolean) {
        if (dangChoi) {
            val left = gate.extend(phut)
            toast("Đã cộng thêm $phut phút, còn ${left ?: 0} phút")
            render()
            return
        }
        // Lay truoc khi duyet, vi duyet xong la bai do bi go ra khoi hang cho.
        // Duyet tren may thi duyet bai cu nhat - muon chon dung mot bai giua dam thi
        // bam nut ngay duoi anh bai do ben Telegram.
        val bai = gate.baiChoCuNhat()
        val messageId = bai?.messageId ?: 0L
        val dangChoDuyet = gate.state == GateState.PENDING

        val minutes = gate.approve(wantedMinutes = phut, useQuota = truLuot, requestId = bai?.id)
        val congDon = minutes != null && minutes > phut
        if (minutes == null) {
            toast(
                if (truLuot) "Không cấp được: đang giờ ngủ, hoặc hôm nay đã hết số phút tối đa"
                else "Không cấp được: đang trong giờ ngủ"
            )
            return
        }
        ApprovalService.ensureRunning(this)
        val conCho = gate.soBaiDangCho()
        toast(
            (if (congDon) "Đã cộng dồn thành $minutes phút"
            else "Đã duyệt $minutes phút (chưa tính giờ)") +
                if (conCho > 0) ", còn $conCho bài chờ duyệt" else ""
        )
        render()

        // Duyet tren may thi go luon ban phim duoi anh ben Telegram. Khong go thi
        // nut "Duyet 60 phut" van nam do, bam vao chi nhan duoc cau "yeu cau nay cu
        // roi" - dung nhung kho hieu.
        if (dangChoDuyet && messageId != 0L) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val client = TelegramClient(prefs.botToken)
                    client.clearReplyMarkup(prefs.parentChatId, messageId)
                    client.editCaption(
                        prefs.parentChatId,
                        messageId,
                        "Đã duyệt $minutes phút ngay trên tablet."
                    )
                }
            }
        }
    }

    /**
     * Cat phien choi cua Le Hoa ngay, mat so phut con lai.
     *
     * Chi lam mot viec nay thoi. Truoc day no tat luon ca che do mo may, tuc la mot
     * nut lam hai viec khac han nhau - va tren trang nay da co san cong tac "Mo toan
     * bo may" de lam viec kia. Dung o lai trang nay de thay ket qua ngay.
     */
    private fun dungGioChoi() {
        gate.endSession(EndReason.PARENT_REVOKED)
        toast("Đã dừng giờ chơi của ${getString(R.string.child_name)}")
        ApprovalService.ensureRunning(this)
        render()
    }

    /**
     * Tat quan tri thiet bi de Android cho go app.
     *
     * Khong tu bat lai duoc: bat quan tri thiet bi bat buoc phai co man hinh he
     * thong cho nguoi dung bam dong y, code khong tu lam thay duoc. Nen o day chi
     * tat, roi bao mot tin len Telegram de ba khong quen mat.
     */
    private fun choGoApp() {
        if (!Permissions.hasDeviceAdmin(this)) {
            startActivity(Permissions.deviceAdminIntent(this))
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Cho phép gỡ app?")
            .setMessage(
                "Tắt quản trị thiết bị thì gỡ được app, nhưng lúc đó " +
                    "${getString(R.string.child_name)} cũng gỡ được. " +
                    "Máy sẽ nhắn Telegram nhắc ba Huy bật lại."
            )
            .setPositiveButton("Tắt") { _, _ ->
                // Go app thi phai vao duoc Cai dat > Ung dung, ma cho do guard chan.
                PhienQuanLy.choMoCaiDat(10)
                val dpm = getSystemService(DevicePolicyManager::class.java)
                dpm.removeActiveAdmin(Permissions.adminComponent(this))
                Notifier.send(
                    this,
                    "Đã tắt quản trị thiết bị theo yêu cầu. Giờ gỡ app được. " +
                        "Gỡ xong nhớ bật lại, không thì máy hở."
                )
                toast("Đã tắt. Vào Cài đặt > Ứng dụng để gỡ.")
                render()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Tu Android 15 app ve tran ca man hinh, thuoc tinh statusBarColor trong
     * theme khong con tac dung. Khong chua cho thi dong "Cau hinh" chui len duoi dong ho
     * va pin cua thanh trang thai.
     */
    private fun chuaThanhHeThong() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
    }

    private fun toast(text: String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
