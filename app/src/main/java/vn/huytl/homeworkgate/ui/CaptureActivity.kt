package vn.huytl.homeworkgate.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.CaptureStage
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.databinding.ActivityCaptureBinding
import vn.huytl.homeworkgate.telegram.CapSachSender
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.telegram.HomeworkSender
import java.io.File

/**
 * Chup bai tap theo ba buoc roi gui len Telegram.
 *
 * Ba buoc vi mot xap anh tron lan khong cho ba biet dau la de bai dau la bai lam.
 * Chi bai giai la bat buoc: nhieu hom de in san co san o ghi bai ngay duoi, chup
 * mot tam la co ca hai, bat chup du ba nhom chi lam con chup thua.
 *
 * Dung CameraX chu khong goi Intent sang app Camera cua may, vi qua Intent thi
 * nhieu app camera cho chon anh co san trong thu vien, va con se gui lai anh vo
 * chup tu tuan truoc.
 */
class CaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaptureBinding
    private lateinit var prefs: Prefs

    private var imageCapture: ImageCapture? = null

    /**
     * Con dang chup lai phan da sua, khong phai nop bai moi.
     *
     * Luc do bo qua hai buoc dau (vo dan do, de bai): bai cu da co trong so cai roi,
     * cai can bay gio chi la may dong con vua lam lai.
     */
    private val suaBai by lazy { intent.getBooleanExtra(EXTRA_SUA, false) }

    /**
     * Che do chup cap sach da soan, khong phai nop bai.
     *
     * Dung chung man hinh nay vi phan camera - xin quyen, mo CameraX, dai anh nho,
     * bam mot tam de bo tam do - la y het nhau, va vi mot man chup thu hai co
     * nghia la sua mot cho thi phai nho sua ca cho kia. Khac nhau o hai dau: tren
     * man khong co ba buoc, va duoi day gui bang [CapSachSender] chu khong di vao
     * duong duyet gio choi.
     */
    private val maBuoiSoan: String? by lazy { intent.getStringExtra(EXTRA_SOAN_MA_BUOI) }
    private val soanTap: Boolean get() = maBuoiSoan != null

    /**
     * Che do chup trang vo dan do, goi tu [DanDoActivity].
     *
     * Khong gui di dau ca: chup xong tra duong dan ve cho man goi, ben do moi dua cho
     * may doc. Dung chung man chup vi phan camera - xin quyen, mo CameraX, dai anh nho,
     * bam mot tam de bo tam do - y het. Tam anh nay se di vao viec tinh gio, nen van
     * giu khung ngam de con chup thang tu tren xuong.
     */
    private val chupDanDo: Boolean by lazy { intent.getBooleanExtra(EXTRA_DAN_DO, false) }

    /** Ba che do rut gon deu chi co mot xap anh, khong co ba buoc. */
    private val motXap: Boolean get() = suaBai || soanTap || chupDanDo

    /**
     * Cac buoc THAT SU phai chup lan nay, theo thu tu. Dieu huong tien lui chay
     * tren danh sach nay chu khong tren [CaptureStage.entries].
     *
     * BO BUOC DE BAI KHI CON DA KHAI BAI THEO SACH. Luc do de bai da nam san trong
     * cau lenh gui cho may cham - xem [vn.huytl.homeworkgate.ai.PromptCham]
     * CAU_LENH_KHAI_BAI, quy tac 4: cau trong danh sach thi "co_de" luon true, du
     * anh co trang sach hay khong. Bat chup them mot tam nua chi de may nhin lai
     * cai no da doc roi.
     *
     * CON BUOC VO DAN DO THI KHONG BO DUOC, du trong ngay da tinh tron goi. Thieu
     * trang vo do, may khong biet cau nao thuoc bai co giao nen coi TAT CA la trong
     * goi (quy tac 17), va lan nop thu hai se khong duoc phut nao.
     */
    private val cacBuoc: List<CaptureStage> by lazy {
        // Trong may da co ban vo dan do da soat thi khong hoi lai trang vo nua: doan
        // chu do di thang vao cau lenh cham - xem [vn.huytl.homeworkgate.data.VoDanDo].
        //
        // Tat cham AI cung vay: ban soat di kem bai len Firestore, va Bang dieu khien
        // chep no vao loi nho Claude - xem [vn.huytl.homeworkgate.dongbo.DongBo.banDanDo].
        // Chua co ban soat (con chua chup dau buoi, hay may doc khong duoc) thi van chup
        // trang vo o day, va Claude doc anh nhu truoc.
        val canVo = VoDanDo.conHieuLuc(this) == null
        when {
            motXap -> listOf(CaptureStage.BAI_GIAI)
            else -> buildList {
                if (canVo) add(CaptureStage.DAN_DO)
                if (pham?.theoSach != true) add(CaptureStage.DE_BAI)
                add(CaptureStage.BAI_GIAI)
            }
        }
    }

    /**
     * Bai con da khai o man truoc: mon nao, sach nao, nhung cau nao.
     *
     * Null la vao thang man chup ma khong qua man khai bai - van chay duoc, chi la
     * luc cham may phai tu tach cau nhu truoc. Giu duong do de mot cho hong ben kia
     * khong chan mat duong nop bai.
     */
    private val pham: PhamVi? by lazy { PhamVi.tuJson(intent.getStringExtra(EXTRA_PHAM)) }

    private var stage = CaptureStage.DAN_DO

    /** Hom nay da tinh tron goi chua, doc san luc mo man cho [boQua]. */
    private var goiDaCoHomNay = false
    private val shots = CaptureStage.entries.associateWith { mutableListOf<File>() }

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            toast("Không có quyền camera thì không chụp được")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs.get(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        binding.btnTake.setOnClickListener { takePhoto() }
        binding.btnSkip.setOnClickListener { boQua() }
        // Hoi so cai mot lan luc mo man, ngoai luong giao dien. Xem [boQua].
        lifecycleScope.launch {
            goiDaCoHomNay = withContext(Dispatchers.IO) { SoCaiBai.goiDaCoHomNay(this@CaptureActivity) }
        }
        binding.btnNext.setOnClickListener { goNext() }
        // Ba che do rut gon chi co mot xap anh nen buoc dau cung la buoc cuoi, va
        // goNext() thanh gui thang.
        stage = cacBuoc.first()

        binding.stepNotes.setOnClickListener { jumpTo(CaptureStage.DAN_DO) }
        binding.stepProblem.setOnClickListener { jumpTo(CaptureStage.DE_BAI) }
        binding.stepSolution.setOnClickListener { jumpTo(CaptureStage.BAI_GIAI) }

        binding.btnBack.setOnClickListener { lui() }

        // Cu chi vuot Back di chung mot duong voi nut tren man hinh, de hai cach
        // ra khoi man nay khong hanh xu khac nhau.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = lui()
        })

        render()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        // Don anh chua gui, khong de rac lai trong bo nho may. Gui xong thi danh
        // sach da rong nen cho nay khong dung toi. Khong loc theo isFinishing:
        // thoat ra la luc can xoa nhat, ma do lai chinh la luc isFinishing bang
        // true, nen loc kieu do thanh ra giu lai dung nhung tam can bo.
        shots.values.flatten().forEach { it.delete() }
        super.onDestroy()
    }

    /**
     * Duong ra: dang o buoc sau thi lui mot buoc, dang o buoc dau thi thoat han.
     * Thoat ma da chup roi thi hoi lai mot cau, vi bam nham nut thoat luc da chup
     * xong tam thu chin la mat ca chin tam.
     */
    private fun lui() {
        // Hai che do rut gon vao thang buoc cuoi, nen "quay lai" phai la thoat han.
        // Lui mot buoc se roi vao buoc chup de bai - thu ma lan nay khong can chup.
        val previous = cacBuoc.getOrNull(cacBuoc.indexOf(stage) - 1)
        if (previous != null) {
            jumpTo(previous)
            return
        }
        if (shots.values.sumOf { it.size } == 0) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Thoát và bỏ ảnh đã chụp?")
            .setMessage("Ảnh chưa gửi cho ba Huy sẽ mất, Lê Hòa phải chụp lại từ đầu.")
            .setPositiveButton("Thoát") { _, _ -> finish() }
            .setNegativeButton("Chụp tiếp", null)
            .show()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.preview.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
            }.onFailure { toast("Không mở được camera") }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun current(): MutableList<File> = shots.getValue(stage)

    private fun jumpTo(target: CaptureStage) {
        stage = target
        render()
    }

    private fun goNext() {
        if (stage == CaptureStage.BAI_GIAI) {
            if (current().isEmpty()) {
                toast(
                    when {
                        soanTap -> getString(R.string.capture_need_cap)
                        chupDanDo -> "Phải chụp trang vở dặn dò đã."
                        else -> getString(R.string.capture_need_solution)
                    }
                )
                return
            }
            sendAll()
            return
        }
        jumpTo(cacBuoc[cacBuoc.indexOf(stage) + 1])
    }

    /**
     * Bo qua buoc dang chup, khong chup tam nao.
     *
     * Rieng buoc vo dan do, hom nay da tinh tron goi thi hoi lai truoc. Thieu trang vo,
     * quy tac 17 coi moi cau lan nay la bai co giao, ma bai co giao da tra trong goi: ca
     * lan nop ra 0 phut, ke ca bai lam them that. Man chinh luc do chi ghi "Chưa cộng giờ
     * được cho bài này", khong noi vi sao.
     */
    private fun boQua() {
        if (stage == CaptureStage.DAN_DO && goiDaCoHomNay) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Chụp vở dặn dò nhé")
                .setMessage(
                    "Hôm nay đã tính trọn gói bài cô giao. Thiếu trang vở thì máy không biết " +
                        "câu nào là bài làm thêm, nên cả lần nộp này có thể không được phút nào."
                )
                .setPositiveButton("Chụp vở", null)
                .setNegativeButton("Vẫn bỏ qua") { _, _ -> goNext() }
                .show()
            return
        }
        goNext()
    }

    private fun takePhoto() {
        if (current().size >= MAX_PER_STAGE) {
            toast(getString(R.string.capture_full))
            return
        }
        val capture = imageCapture ?: return
        val file = File(cacheDir, "shot_${stage.name}_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        binding.btnTake.isEnabled = false

        // Neu camera treo thi takePicture khong goi lai ham nao ca, khong ca
        // onError. Luc do nut chup tat han va con ngoi bam mai khong hieu tai sao.
        // Thay vi tin la callback luon ve, tu mo khoa lai nut sau mot khoang.
        val moKhoaLai = Runnable {
            if (!binding.btnTake.isEnabled) {
                binding.btnTake.isEnabled = true
                toast("Camera không phản hồi, bấm chụp lại nhé")
            }
        }
        binding.btnTake.postDelayed(moKhoaLai, CAPTURE_TIMEOUT_MS)

        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    binding.btnTake.removeCallbacks(moKhoaLai)
                    binding.btnTake.isEnabled = true
                    current().add(file)
                    render()
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.btnTake.removeCallbacks(moKhoaLai)
                    binding.btnTake.isEnabled = true
                    toast("Chụp lỗi, thử lại nhé")
                }
            }
        )
    }

    private fun render() {
        renderSteps()
        renderStrip()

        // Nhac lai bai da khai ngay tren man chup. Khai o man truoc roi chup nham
        // trang khac la canh de xay ra nhat, va mot dong chu o day chan duoc no.
        val daKhai = pham?.takeIf { it.theoSach }?.let { "${it.bai} · ${it.cauIds.size} câu" }
        binding.txtStageHint.text = when {
            chupDanDo -> "Chụp trang vở dặn dò. Trang có mấy buổi cũng được, " +
                "lát nữa chọn đúng buổi hôm nay."
            soanTap -> getString(R.string.capture_cap_hint)
            suaBai -> "Chụp lại phần Lê Hòa vừa sửa."
            daKhai != null -> "$daKhai\n${getString(stage.hintRes)}"
            else -> getString(stage.hintRes)
        }

        binding.btnBack.text =
            if (stage == cacBuoc.first()) "✕  Thoát" else "‹  Quay lại"
        binding.stepNotes.visibility = hienBuoc(CaptureStage.DAN_DO)
        binding.stepProblem.visibility = hienBuoc(CaptureStage.DE_BAI)
        // O buoc cuoi van hien khi sua bai, vi luc do no la nhan cho biet dang chup
        // cai gi. Chup cap thi khong co nhan nao dung ca, an luon.
        // Chip "Bai giai" chi co nghia khi con dang nop bai. Hai che do kia chup thu
        // khac han, de cai nhan do lai thi no doc ra mot dieu khong dung.
        binding.stepSolution.visibility =
            if (soanTap || chupDanDo) View.GONE else View.VISIBLE
        val tong = shots.values.sumOf { it.size }
        binding.txtTotal.text = when {
            tong == 0 -> ""
            soanTap -> "Đã chụp $tong tấm"
            else -> "Đã chụp $tong trang"
        }

        val count = current().size
        // Buoc khong bat buoc va chua chup gi: cho han mot nut "khong co" de con
        // khong phai doan xem bo qua bang cach nao.
        binding.btnSkip.visibility =
            if (!stage.required && count == 0) View.VISIBLE else View.INVISIBLE

        binding.btnNext.text = when {
            // Che do nay khong gui gi di dau ca, chup xong la tra anh ve man goi.
            chupDanDo -> "Xong"
            stage == CaptureStage.BAI_GIAI -> getString(R.string.capture_send)
            else -> getString(R.string.capture_next)
        }
        binding.btnNext.isEnabled = stage != CaptureStage.BAI_GIAI || count > 0

        // Khung ngam chi can khi chup chu de doc. Chup cap sach thi de con lui ra xa
        // lay ca cai cap, khung chi lam vuong mat.
        val canKhung = !soanTap
        binding.khungNgam.visibility = if (canKhung) View.VISIBLE else View.GONE
        binding.chuKhung.visibility = if (canKhung) View.VISIBLE else View.GONE

        binding.txtCount.text = when {
            count == 0 -> ""
            count >= MAX_PER_STAGE -> getString(R.string.capture_full)
            soanTap -> "$count tấm. ${getString(R.string.capture_remove_hint)}"
            else -> "$count trang. ${getString(R.string.capture_remove_hint)}"
        }
    }

    /** To mau ba o buoc: dang lam thi xanh duong, chup roi thi xanh la. */
    private fun hienBuoc(buoc: CaptureStage): Int =
        if (buoc in cacBuoc) View.VISIBLE else View.GONE

    private fun renderSteps() {
        val views = mapOf(
            CaptureStage.DAN_DO to binding.stepNotes,
            CaptureStage.DE_BAI to binding.stepProblem,
            CaptureStage.BAI_GIAI to binding.stepSolution
        )
        views.forEach { (item, view) ->
            val count = shots.getValue(item).size
            view.background = ContextCompat.getDrawable(
                this,
                when {
                    item == stage -> R.drawable.bg_step_on
                    count > 0 -> R.drawable.bg_step_done
                    else -> R.drawable.bg_step_off
                }
            )
            view.text = if (count > 0) {
                "${getString(item.labelRes)}  $count"
            } else {
                getString(item.labelRes)
            }
        }
    }

    /** Ve lai dai anh nho cua rieng buoc dang lam. Bam mot tam la bo tam do. */
    private fun renderStrip() {
        binding.strip.removeAllViews()
        val size = resources.displayMetrics.density.times(76).toInt()
        val files = current()

        files.forEachIndexed { index, file ->
            val thumb = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (size * 0.14f).toInt()
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "Trang ${index + 1}"
                setImageBitmap(decodeThumb(file, size))
                setOnClickListener {
                    files.removeAt(index).delete()
                    render()
                    toast("Đã bỏ trang ${index + 1}")
                }
            }
            binding.strip.addView(thumb)
        }
        binding.stripScroll.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
    }

    /** Giai ma anh nho thoi, khong nap ca tam vai megabyte vao bo nho. */
    private fun decodeThumb(file: File, target: Int) = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target) sample *= 2
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }.getOrNull()

    private fun sendAll() {
        val groups = shots.filterValues { it.isNotEmpty() }.mapValues { it.value.toList() }
        if (groups[CaptureStage.BAI_GIAI].isNullOrEmpty()) return

        if (soanTap) {
            guiCapSach(groups.getValue(CaptureStage.BAI_GIAI))
            return
        }

        // Chup vo dan do: khong gui gi ca, tra duong dan ve cho man goi. Phai xoa danh
        // sach truoc khi dong, neu khong onDestroy don mat may tam vua chup.
        if (chupDanDo) {
            val anh = groups.getValue(CaptureStage.BAI_GIAI)
            shots.values.forEach { it.clear() }
            setResult(
                RESULT_OK,
                android.content.Intent().putStringArrayListExtra(
                    KET_QUA_ANH, ArrayList(anh.map { it.absolutePath })
                )
            )
            finish()
            return
        }

        // Khong gui thang nua: dua sang man soat de con xem may doc ra chu gi da.
        // Xem [SoatBaiActivity] de biet vi sao chen mot buoc vao giua.
        //
        // Xoa danh sach o day de onDestroy khong xoa file: tu luc nay man soat so
        // huu may tam anh, va no se don khi con bo ngang.
        shots.values.forEach { it.clear() }
        startActivity(SoatBaiActivity.moTu(this, groups, pham))
        finish()
    }

    /**
     * Gui anh cap sach. Khong dung [HomeworkSender], khong ghi bai cho duyet,
     * khong goi AI cham: day khong phai mot lan nop bai.
     *
     * Danh dau da soan chi sau khi gui duoc that. Danh dau truoc thi mat mang mot
     * cai la loi nhac tat di ma Ba Huy chua thay tam anh nao.
     */
    private fun guiCapSach(anh: List<File>) {
        val ma = maBuoiSoan ?: return
        val moTa = intent.getStringExtra(EXTRA_SOAN_MO_TA).orEmpty()
        val mon = intent.getStringArrayExtra(EXTRA_SOAN_MON)?.toList().orEmpty()

        setBusy(true)
        lifecycleScope.launch {
            val ketQua = withContext(Dispatchers.IO) {
                runCatching { CapSachSender.send(this@CaptureActivity, anh, moTa, mon) }
            }
            setBusy(false)
            ketQua.onSuccess {
                prefs.danhDauDaSoan(ma)
                DayLog.add(this@CaptureActivity, "Soạn tập cho $moTa")
                anh.forEach { it.delete() }
                shots.values.forEach { it.clear() }
                toast("Đã gửi cho ${getString(R.string.parent_name)}")
                finish()
            }.onFailure {
                toast("Gửi không được. Kiểm tra mạng rồi thử lại.")
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.progress.visibility = if (busy) View.VISIBLE else View.GONE
        binding.btnNext.isEnabled = !busy
        binding.btnTake.isEnabled = !busy
        binding.btnSkip.isEnabled = !busy
    }

    private fun toast(text: String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    companion object {
        /** Bat che do chup lai phan da sua. */
        const val EXTRA_SUA = "sua_bai"

        /** Bat che do chup trang vo dan do. */
        const val EXTRA_DAN_DO = "chup_dan_do"

        /** Duong dan may tam vua chup, tra ve cho man goi. */
        const val KET_QUA_ANH = "ket_qua_anh"

        /** Bai con vua khai o [ChonBaiActivity], dang JSON cua [PhamVi]. */
        const val EXTRA_PHAM = "pham_vi"

        /**
         * Ma buoi hoc dang soan. Co ma nay la bat che do chup cap sach; khong co
         * thi man hinh chay duong nop bai nhu cu.
         */
        const val EXTRA_SOAN_MA_BUOI = "soan_ma_buoi"
        const val EXTRA_SOAN_MO_TA = "soan_mo_ta"
        const val EXTRA_SOAN_MON = "soan_mon_da_tich"

        /** Album cua Telegram chua toi da 10 anh, nen moi nhom toi da 10 trang. */
        const val MAX_PER_STAGE = 10

        /** Cho camera bao nhieu lau truoc khi coi nhu no treo. */
        const val CAPTURE_TIMEOUT_MS = 8_000L
    }
}
