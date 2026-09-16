package vn.huytl.homeworkgate.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.ai.AiChamBai
import vn.huytl.homeworkgate.ai.ChamBaiIO
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.CaptureStage
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.databinding.StActivitySoatBaiBinding
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.telegram.ApprovalService
import java.io.File

/**
 * Con soat lai ban may doc, roi moi gui bai di.
 *
 * VI SAO CO MAN NAY. May doc net but cua mot dua tre viet voi: "1" ra "l", "0" ra
 * "6", dau tru ra net gach xoa. Truoc day con khong bao gio biet may doc ra cai gi -
 * no chi thay ket qua cuoi, va neu may doc nham thi cham nham, ma cham nham thi hoac
 * con mat gio oan hoac Ba Huy phai mo anh ra doi chieu tung dong.
 *
 * MAN NAY KHONG HIEN DUNG HAY SAI, no hoi dung mot cau: may doc co ra dung chu con
 * viet khong. Hai duong ra, va hai duong nay khac han nhau:
 *
 *  - MAY DOC SAI CHU CON: bai tren giay van dung. Con sua dong do cho khop voi giay.
 *    Luc gui, may se nhin lai anh de xac nhan - xem [AiChamBai.docLai].
 *  - CON TU NHIN LAI VA THAY MINH LAM SAI: bai tren giay sai. Luc do con BO cau do ra
 *    khoi lan nop, sua trong vo roi nop lai. Khong cho go dap an dung vao day: viet
 *    lai bang tay moi vao dau, ma vo con la cai co giao cham chu khong phai cai may.
 *
 * Cau bi bo ra khong ghi vao so mot dong nao, y nhu cau may khong tim thay trong anh:
 * con chua dinh vao no lan nao ca thi khong co gi de ghi.
 */
class SoatBaiActivity : AppCompatActivity() {

    private lateinit var binding: StActivitySoatBaiBinding

    private val pham: PhamVi? by lazy { PhamVi.tuJson(intent.getStringExtra(EXTRA_PHAM)) }

    private val nhom: Map<CaptureStage, List<File>> by lazy {
        CaptureStage.entries.associateWith { st ->
            intent.getStringArrayListExtra(EXTRA_ANH + st.name).orEmpty().map { File(it) }
        }.filterValues { it.isNotEmpty() }
    }

    private var ket: KetQuaCham? = null

    /** O go cua tung cau, theo ma cau. Doc nguoc ra de biet con sua gi. */
    private val oDong = linkedMapOf<String, List<EditText>>()

    /** Cau con tu nhan la lam sai, se lam lai - khong gui lan nay. */
    private val boQua = linkedSetOf<String>()

    /** O con dang go, de dai nut toan biet chen ky tu vao dau. */
    private var dangGo: EditText? = null

    private var daGui = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StActivitySoatBaiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }

        binding.nutQuayLai.setOnClickListener { hoiRoiThoat() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = hoiRoiThoat()
        })
        binding.nutGui.setOnClickListener { gui() }
        binding.nutChupLai.setOnClickListener { chupLai() }

        veDaiToan()

        // Xoay man, hay Android thu hoi bo nho trong luc con dang soat: dung lai ban
        // cham da co chu KHONG goi AI lan nua. Goi lai vua ton them mot lan han muc,
        // vua co the ra ket qua khac ban con dang nhin - va con thi khong hieu vi sao
        // may vua doi y.
        val cu = ChamBaiIO.doc(savedInstanceState?.getString(LUU_BAN_CHAM))
        if (cu != null) {
            binding.khungCho.visibility = View.GONE
            binding.khungCuon.visibility = View.VISIBLE
            binding.dayNut.visibility = View.VISIBLE
            ket = cu
            ve(cu)
        } else {
            cham()
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        ket?.let { out.putString(LUU_BAN_CHAM, ChamBaiIO.viet(giuBanDangSua(it))) }
    }

    /** Ban cham kem nhung dong con vua sua, de xoay man khong mat cong go lai. */
    private fun giuBanDangSua(k: KetQuaCham): KetQuaCham = k.copy(
        cac = k.cac.map { cau ->
            val dong = oDong[cau.ma]?.map { o -> o.text.toString() } ?: return@map cau
            if (dong == cau.baiLam) cau else cau.copy(baiLam = dong)
        }
    )

    /**
     * Anh chua gui thi don di.
     *
     * Khong loc theo isFinishing: thoat ra la luc can xoa nhat, ma do lai chinh la
     * luc isFinishing bang true. Gui roi thi service so huu anh va tu xoa.
     */
    override fun onDestroy() {
        if (!daGui) nhom.values.flatten().forEach { runCatching { it.delete() } }
        super.onDestroy()
    }

    // ------------------------------------------------------------------ cham bai

    private fun cham() {
        lifecycleScope.launch {
            val anh = nhom.values.flatten()
            val giai = nhom[CaptureStage.BAI_GIAI].orEmpty()
            val kq = withContext(Dispatchers.IO) {
                AiChamBai.cham(this@SoatBaiActivity, anh, giai.ifEmpty { anh }, pham)
            }
            binding.khungCho.visibility = View.GONE
            binding.khungCuon.visibility = View.VISIBLE
            binding.dayNut.visibility = View.VISIBLE

            val k = kq.ket
            if (k == null) {
                khongDocDuoc(kq.loi)
                return@launch
            }
            ket = k
            ve(k)
        }
    }

    /**
     * May khong cham duoc (het han muc, mat mang, Google doi model).
     *
     * Van cho gui: anh va hai nut Duyet/Khong duyet ben Telegram nam nguyen nhu tu
     * truoc den gio, nen hong AI khong duoc phep lam ket bai cua con.
     */
    private fun khongDocDuoc(loi: String?) {
        binding.tieuDe.text = "Máy chưa đọc được bài"
        binding.phuDe.text = loi.orEmpty()
        binding.danhSach.removeAllViews()
        binding.danhSach.addView(
            TextView(this).apply {
                text = "Con cứ gửi, ${getString(R.string.parent_name)} xem giúp nhé."
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@SoatBaiActivity, R.color.ink))
            }
        )
        binding.demCau.text = ""
    }

    private fun ve(k: KetQuaCham) {
        binding.danhSach.removeAllViews()
        oDong.clear()
        k.cac.forEach { theCau(it) }
        capNhatDem()
    }

    private fun theCau(cau: CauCham) {
        val the = LayoutInflater.from(this)
            .inflate(R.layout.st_the_soat_cau, binding.danhSach, false) as LinearLayout
        the.findViewById<TextView>(R.id.ma_cau).text = cau.ma
        the.findViewById<TextView>(R.id.de_cau).text =
            cau.de.take(110) + if (cau.de.length > 110) "…" else ""

        // Khong co danh sach dong (ban cu, hay may tra ve thieu) thi bay ra dung ket
        // qua cuoi - van con mot thu de con soat, con hon mot the trong.
        val dong = cau.baiLam.ifEmpty { listOfNotNull(cau.ketQua.takeIf { it.isNotBlank() }) }
        val khung = the.findViewById<LinearLayout>(R.id.cac_dong)
        val cacO = dong.mapIndexed { i, chu ->
            val o = LayoutInflater.from(this)
                .inflate(R.layout.st_dong_soat, khung, false) as EditText
            o.setText(chu)
            // Dong may thay co van de: to len cho con nhin vao do truoc.
            if (cau.dongSai == i + 1) {
                o.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.wait_soft)
                )
            }
            o.setOnFocusChangeListener { v, co -> if (co) moDaiToan(v as EditText) }
            khung.addView(o)
            o
        }
        oDong[cau.ma] = cacO

        val chuNhac = when {
            !cau.docRo -> "Máy phải đoán chữ ở câu này. Con xem kỹ giúp."
            cau.dongSai > 0 -> "Máy thấy dòng ${cau.dongSai} có vấn đề. Con đọc lại dòng đó nhé."
            else -> ""
        }
        the.findViewById<TextView>(R.id.loi_nhac).apply {
            text = chuNhac
            visibility = if (chuNhac.isBlank()) View.GONE else View.VISIBLE
        }

        val nut = the.findViewById<MaterialButton>(R.id.nut_bo_cau)
        nut.setOnClickListener { doiBoCau(cau.ma, the, nut) }
        binding.danhSach.addView(the)
    }

    private fun doiBoCau(ma: String, the: View, nut: MaterialButton) {
        if (ma in boQua) {
            boQua -= ma
            the.alpha = 1f
            nut.text = "Mình làm sai rồi, để làm lại"
        } else {
            boQua += ma
            the.alpha = 0.45f
            nut.text = "Lấy lại câu này"
        }
        oDong[ma]?.forEach { it.isEnabled = ma !in boQua }
        capNhatDem()
    }

    private fun capNhatDem() {
        val tong = oDong.size
        val gui = tong - boQua.size
        binding.nutGui.isEnabled = gui > 0
        binding.demCau.text = when {
            tong == 0 -> ""
            boQua.isEmpty() -> "Gửi $tong câu."
            gui == 0 -> "Con bỏ hết rồi. Sửa trong vở rồi chụp lại nhé."
            else -> "Gửi $gui câu, để lại ${boQua.size} câu làm lại."
        }
    }

    // ------------------------------------------------------------- dai nut toan

    /**
     * Dai ky tu toan ngay tren o go.
     *
     * Khong viet ban phim he thong: ban phim he thong phai bat trong Cai dat, ma Cai
     * dat thi dang bi chan. Day chi la may cai nut chen chu vao cho con tro - du cho
     * Toan 8, va con dung duoc ngay khong phai cai gi.
     */
    private fun veDaiToan() {
        val dp = resources.displayMetrics.density
        KY_TU_TOAN.forEach { ky ->
            val nut = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            nut.text = ky
            nut.textSize = 17f
            nut.minWidth = (48 * dp).toInt()
            nut.minimumWidth = (48 * dp).toInt()
            nut.setPadding((10 * dp).toInt(), 0, (10 * dp).toInt(), 0)
            nut.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (44 * dp).toInt()
            ).apply { marginEnd = (6 * dp).toInt() }
            // Bam nut khong duoc cuop con tro khoi o dang go.
            nut.isFocusable = false
            nut.setOnClickListener { chen(ky) }
            binding.nutToan.addView(nut)
        }
    }

    private fun moDaiToan(o: EditText) {
        dangGo = o
        binding.daiToan.visibility = View.VISIBLE
    }

    private fun chen(chu: String) {
        val o = dangGo ?: return
        val dau = o.selectionStart.coerceAtLeast(0)
        val cuoi = o.selectionEnd.coerceAtLeast(0)
        o.text.replace(minOf(dau, cuoi), maxOf(dau, cuoi), chu)
    }

    // ---------------------------------------------------------------------- gui

    private fun gui() {
        val goc = ket ?: return guiDi(null)
        val giu = goc.cac.filterNot { it.ma in boQua }
        if (giu.isEmpty()) return

        // Cau nao con sua dong: gom lai de nho may nhin lai anh.
        val daSua = mutableListOf<Pair<CauCham, List<String>>>()
        giu.forEach { cau ->
            val dong = oDong[cau.ma]?.map { it.text.toString() } ?: return@forEach
            if (dong != cau.baiLam) daSua += cau to dong
        }

        if (daSua.isEmpty()) return guiDi(goc.copy(cac = giu))

        binding.nutGui.isEnabled = false
        binding.demCau.text = "Đang nhờ máy đọc lại ${daSua.size} câu…"
        lifecycleScope.launch {
            val lai = withContext(Dispatchers.IO) {
                AiChamBai.docLai(
                    this@SoatBaiActivity,
                    nhom[CaptureStage.BAI_GIAI].orEmpty(),
                    daSua
                )
            }
            val theoMa = lai.associateBy { it.ma }
            guiDi(goc.copy(cac = giu.map { theoMa[it.ma] ?: it }))
        }
    }

    private fun guiDi(k: KetQuaCham?) {
        daGui = true
        ApprovalService.ensureRunning(this)
        ApprovalService.guiDaSoat(this, nhom, pham, k)
        Toast.makeText(
            this, "Đã gửi cho ${getString(R.string.parent_name)}", Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun chupLai() {
        daGui = true
        nhom.values.flatten().forEach { runCatching { it.delete() } }
        startActivity(
            Intent(this, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_PHAM, pham?.sangJson())
        )
        finish()
    }

    private fun hoiRoiThoat() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Thoát và bỏ bài đã chụp?")
            .setMessage("Bài chưa gửi cho ${getString(R.string.parent_name)} sẽ mất.")
            .setPositiveButton("Thoát") { _, _ -> finish() }
            .setNegativeButton("Xem tiếp", null)
            .show()
    }

    companion object {
        private const val LUU_BAN_CHAM = "ban_cham"

        const val EXTRA_PHAM = "pham_vi"
        const val EXTRA_ANH = "anh_"

        /** Bo ky tu du cho Toan 8: luy thua, can, phan so, va may dau hinh hoc. */
        private val KY_TU_TOAN = listOf(
            "^", "²", "³", "√", "∛", "/", "·", "−", "≈", "≠", "≤", "≥",
            "°", "∠", "Δ", "∥", "⊥", "π", "(", ")"
        )

        fun moTu(
            context: android.content.Context,
            nhom: Map<CaptureStage, List<File>>,
            pham: PhamVi?
        ): Intent = Intent(context, SoatBaiActivity::class.java)
            .putExtra(EXTRA_PHAM, pham?.sangJson())
            .apply {
                nhom.forEach { (st, files) ->
                    putStringArrayListExtra(
                        EXTRA_ANH + st.name, ArrayList(files.map { it.absolutePath })
                    )
                }
            }
    }
}
