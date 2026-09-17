package vn.huytl.homeworkgate.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.databinding.ActivityThongKeBinding
import vn.huytl.homeworkgate.guard.GuardAccessibilityService
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.PhienQuanLy

/**
 * Con da dung app gi, tu may gio den may gio, tong bao nhieu.
 *
 * VI SAO CO TRANG NAY: het gio choi thi may chan, va Ba Huy biet ngay vi chinh
 * minh la nguoi duyet. Nhung TRONG gio choi thi may khong chan gi - mot tieng do
 * la mot khoang trong hoan toan, khong ai biet no di vao YouTube hay vao app hoc
 * tieng Anh. Trang nay lap dung cai khoang do.
 *
 * KHONG CO NUT NAO SUA DUOC GI: day la trang doc. Muon chan mot app hay dat han
 * cho no thi sang [AppPickerActivity] - o do moi la cho ra quyet dinh. Tron hai
 * viec vao mot trang thi moi lan liec xem con choi gi lai thanh mot lan phai can
 * nhac co phat hay khong.
 */
class ThongKeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThongKeBinding

    /** Dang xem ngay nao: 0 la hom nay, 1 la hom qua. */
    private var lui = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThongKeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Y het [AppPickerActivity]: vao thang bang adb ma chua qua PIN thi dong
        // luon. Day la so con lam gi tren may, khong phai thu de mo ra tu ngoai.
        if (Prefs.get(this).hasPin() && !PhienQuanLy.daQuaPin && !ParentMode.isActive(this)) {
            finish()
            return
        }

        chuaThanhHeThong()
        binding.btnDone.setOnClickListener { finish() }
        veHangNgay()
        ve()
    }

    /** Hang bay o chon ngay, hom nay dung dau. */
    private fun veHangNgay() {
        binding.hangNgay.removeAllViews()
        (0 until NhatKySuDung.GIU_NGAY).forEach { n ->
            val nut = LayoutInflater.from(this)
                .inflate(R.layout.item_ngay, binding.hangNgay, false) as MaterialButton
            nut.text = NhatKySuDung.tenNgay(n).replaceFirstChar { it.uppercase() }
            nut.setOnClickListener {
                if (lui == n) return@setOnClickListener
                lui = n
                veHangNgay()
                ve()
                binding.cuonNgay.smoothScrollTo(0, 0)
            }
            toMau(nut, dangXem = n == lui)
            binding.hangNgay.addView(nut)
        }
    }

    private fun toMau(nut: MaterialButton, dangXem: Boolean) {
        if (dangXem) {
            nut.setBackgroundColor(mau(R.color.brand))
            nut.setTextColor(Color.WHITE)
            nut.strokeColor = android.content.res.ColorStateList.valueOf(mau(R.color.brand))
        } else {
            nut.setBackgroundColor(Color.TRANSPARENT)
            nut.setTextColor(mau(R.color.ink_soft))
            nut.strokeColor = android.content.res.ColorStateList.valueOf(mau(R.color.line))
        }
    }

    private fun ve() {
        val cac = NhatKySuDung.theoApp(this, lui)
        val tong = NhatKySuDung.tongMs(this, lui)
        val ten = NhatKySuDung.tenNgay(lui)
        val con = getString(R.string.child_name)

        binding.txtPhuDe.text = "Máy nhớ ${NhatKySuDung.GIU_NGAY} ngày gần nhất"

        binding.txtTong.text = if (cac.isEmpty()) {
            "Không có app nào ${ten}"
        } else {
            "$con dùng máy ${NhatKySuDung.moTaDoDai(tong)}"
        }

        // Dong phu chi co nghia khi co so lieu. Ngay trong ma van ghi lai ten ngay
        // o day thi thanh hai dong noi cung mot cau.
        val khung = NhatKySuDung.tuDenTrongNgay(this, lui)
        binding.txtKhungGio.visibility = if (khung == null) View.GONE else View.VISIBLE
        if (khung != null) {
            binding.txtKhungGio.text = ten.replaceFirstChar { it.uppercase() } +
                " · từ ${NhatKySuDung.gio(khung.first)} đến ${NhatKySuDung.gio(khung.second)}" +
                " · ${cac.size} app"
        }

        binding.danhSach.removeAllViews()
        binding.theDanhSach.visibility = if (cac.isEmpty()) View.GONE else View.VISIBLE
        binding.txtTrong.visibility = if (cac.isEmpty()) View.VISIBLE else View.GONE

        // Noi ro vi sao trong. Mot trang trang tron nhin giong nhu "con ngoan, khong
        // dung gi", trong khi ly do hay gap hon nhieu la dich vu canh app dang tat -
        // tuc la may cung khong chan gi ca, va do moi la tin can biet.
        //
        // Chi noi cau do khi dang xem hom nay: dich vu tat ngay bay gio khong giai
        // thich duoc vi sao thu hai tuan truoc khong co dong nao.
        binding.txtTrong.text = if (lui == 0 && !GuardAccessibilityService.dangChay) {
            "Dịch vụ canh app đang TẮT nên máy không ghi được gì, mà cũng không chặn gì.\n" +
                "Bật lại trong Cài đặt của app."
        } else {
            "Không ghi được app nào $ten."
        }

        val daiNhat = cac.firstOrNull()?.tongMs ?: 0L
        cac.forEachIndexed { i, app ->
            if (i > 0) binding.danhSach.addView(duongKe())
            binding.danhSach.addView(veDong(app, daiNhat))
        }

        binding.txtGhiChu.text =
            "Chỉ ghi app mở được từ màn hình chính. Màn hình chính, bàn phím và " +
                "app Nộp bài không tính. Quá ${NhatKySuDung.GIU_NGAY} ngày thì máy tự xoá."
    }

    private fun veDong(app: NhatKySuDung.MotApp, daiNhat: Long): View {
        val dong = LayoutInflater.from(this)
            .inflate(R.layout.item_thong_ke, binding.danhSach, false)

        dong.findViewById<TextView>(R.id.app_ten).text = NhatKySuDung.tenApp(this, app.goi)
        dong.findViewById<TextView>(R.id.app_tong).text = NhatKySuDung.moTaDoDai(app.tongMs)
        dong.findViewById<ImageView>(R.id.app_icon).setImageDrawable(
            runCatching { packageManager.getApplicationIcon(app.goi) }
                .getOrElse { ContextCompat.getDrawable(this, R.mipmap.ic_launcher) }
        )

        // Thanh do dai theo ty le voi app dung lau nhat trong ngay, khong theo tong
        // ca ngay: neu theo tong thi mot ngay dung muoi app la ca muoi thanh deu
        // ngan tit bang nhau, khong so sanh duoc gi.
        val phan = if (daiNhat <= 0L) 0f else app.tongMs.toFloat() / daiNhat
        chiaThanh(dong.findViewById(R.id.app_thanh), phan)
        chiaThanh(dong.findViewById(R.id.app_thanh_con), 1f - phan)

        val khoang = dong.findViewById<TextView>(R.id.app_khoang)
        khoang.text = buildString {
            append(app.cacDoan.take(MAX_KHOANG).joinToString(" · ") { NhatKySuDung.moTaDoan(it) })
            if (app.cacDoan.size > MAX_KHOANG) {
                append(" · +${app.cacDoan.size - MAX_KHOANG} lần nữa")
            }
        }
        return dong
    }

    private fun chiaThanh(o: View, phan: Float) {
        o.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)
            .apply { weight = phan.coerceIn(0f, 1f) }
    }

    private fun duongKe(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.displayMetrics.density.toInt().coerceAtLeast(1)
        ).apply { marginStart = (70 * resources.displayMetrics.density).toInt() }
        setBackgroundColor(mau(R.color.line))
    }

    /**
     * Tu Android 15 app ve tran ca man hinh, thuoc tinh statusBarColor trong theme
     * khong con tac dung. Cong them le 28dp von co trong layout de dong tieu de
     * khong dinh thanh trang thai.
     */
    private fun chuaThanhHeThong() {
        val le = (28 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top + le, bottom = thanh.bottom + le)
            insets
        }
    }

    private fun mau(id: Int) = ContextCompat.getColor(this, id)

    companion object {
        /** Bao nhieu khoang thi ghi het, hon nua thi gom lai mot cau. */
        private const val MAX_KHOANG = 20
    }
}
