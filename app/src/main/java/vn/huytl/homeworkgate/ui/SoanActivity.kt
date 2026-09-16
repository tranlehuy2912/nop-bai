package vn.huytl.homeworkgate.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.checkbox.MaterialCheckBox
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.BuoiHoc
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.databinding.StActivitySoanBinding
import java.util.Calendar

/**
 * Man soan vo: liet ke tung mon, Le Hoa tich tung cai mot.
 *
 * Co tinh khong lam mot nut "da soan xong". Mot nut thi bam trong nua giay de tat
 * cho khuat mat roi quen luon. Bat tich tung mon thi muon tat nhanh cung phai quet
 * mat qua du ten mon, ma do chinh la viec can xay ra.
 */
class SoanActivity : AppCompatActivity() {

    private lateinit var binding: StActivitySoanBinding
    private val prefs by lazy { Prefs.get(this) }
    private var buoi: BuoiHoc? = null
    private var maBuoi: String? = null
    private val daTich = mutableSetOf<String>()
    private var tongMon = 0

    /** Vua day man chup len va dang cho no tra man hinh lai. */
    private var dangCho = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StActivitySoanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }

        binding.nutQuayLai.setOnClickListener { finish() }
        binding.nutChup.setOnClickListener {
            // Buoi chi co the duc thi khong bat chup gi ca: Ba Huy chi quan tam mon
            // chinh, con do the duc thi nhac mot cau la du.
            if (tongMon == 0) xongKhongCanChup() else moManChup()
        }
        nap()
    }

    /**
     * Quay ve day tu man chup.
     *
     * Gui duoc that thi buoi da duoc danh dau, dong man nay luon - khong de lai
     * mot danh sach mon da tich xong ma khong con viec gi de lam voi no. Bo do
     * giua chung thi nap lai tu dau, tuc la phai tich lai tung mon.
     *
     * Chi xet khi vua di ra tu day. Man nay con mo duoc de "soan lai" mot buoi da
     * danh dau roi; khong co co [dangCho] thi lan do vua mo ra la dong ngay.
     */
    override fun onResume() {
        super.onResume()
        if (!dangCho) return
        dangCho = false
        if (maBuoi?.let { it in prefs.buoiDaSoan } == true) {
            finish()
            return
        }
        nap()
    }

    private fun nap() {
        daTich.clear()
        val ke = TinhLoiNhac.buoiKeTiep(Calendar.getInstance())
        if (ke == null) {
            binding.tieuDe.text = "Không có buổi học nào sắp tới"
            binding.phuDe.text = "Chắc đang nghỉ."
            binding.khungMon.visibility = View.GONE
            binding.khungTheDuc.visibility = View.GONE
            binding.dayNut.visibility = View.GONE
            return
        }
        binding.dayNut.visibility = View.VISIBLE

        val (cal, b) = ke
        buoi = b
        maBuoi = TinhLoiNhac.maBuoi(cal, b)

        val moTa = TinhLoiNhac.moTaBuoi(b)
        binding.tieuDe.text = if (b.monCanSoan.isEmpty()) {
            "${moTa.replaceFirstChar { it.uppercase() }} có tiết học thể dục"
        } else {
            "Soạn tập cho $moTa"
        }
        binding.phuDe.text =
            "Vào học lúc ${TinhLoiNhac.gioPhut(b.phutVaoHoc)}, có ${b.monTheoTiet.size} tiết"

        val mon = b.monCanSoan
        tongMon = mon.size
        binding.danhSachMon.removeAllViews()
        mon.forEach { ten ->
            val o = LayoutInflater.from(this)
                .inflate(R.layout.st_dong_mon, binding.danhSachMon, false) as MaterialCheckBox
            o.text = ten
            o.setOnCheckedChangeListener { _, tick ->
                if (tick) daTich.add(ten) else daTich.remove(ten)
                capNhatNut()
            }
            binding.danhSachMon.addView(o)
        }

        binding.khungTheDuc.visibility = if (b.coTheDuc) View.VISIBLE else View.GONE
        // Khong co mon nao thi giau luon khung danh sach cho khoi tho mot o rong.
        binding.khungMon.visibility = if (mon.isEmpty()) View.GONE else View.VISIBLE
        capNhatNut()
    }

    private fun capNhatNut() {
        val du = daTich.size >= tongMon

        // Thanh tien do: buoi chi co the duc thi khong co gi de dem, an di cho khoi
        // bay ra mot thanh rong dung yen.
        binding.thanhTienDo.visibility = if (tongMon == 0) View.GONE else View.VISIBLE
        if (tongMon > 0) {
            binding.thanhTienDo.max = tongMon
            binding.thanhTienDo.setProgressCompat(daTich.size, true)
            binding.thanhTienDo.setIndicatorColor(
                ContextCompat.getColor(this, if (du) R.color.ok else R.color.brand)
            )
        }
        binding.nutChup.isEnabled = du
        binding.demTich.text = when {
            tongMon == 0 -> "Buổi này chỉ cần mặc đồ thể dục, không phải chụp gì."
            du -> "Đã tích đủ $tongMon môn."
            else -> "Đã tích ${daTich.size} trên $tongMon môn."
        }
        binding.nutChup.text = if (tongMon == 0) {
            "Con biết rồi"
        } else {
            "Chụp cặp sách gửi ${getString(R.string.parent_name)}"
        }
        if (tongMon == 0) binding.nutChup.isEnabled = true
    }

    /** Buoi chi co the duc: danh dau xong tai cho, khong chup, khong gui Telegram. */
    private fun xongKhongCanChup() {
        maBuoi?.let { prefs.danhDauDaSoan(it) }
        finish()
    }

    private fun moManChup() {
        val ma = maBuoi ?: return
        val b = buoi ?: return
        dangCho = true
        startActivity(
            Intent(this, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_SOAN_MA_BUOI, ma)
                .putExtra(CaptureActivity.EXTRA_SOAN_MO_TA, TinhLoiNhac.moTaBuoi(b))
                .putExtra(CaptureActivity.EXTRA_SOAN_MON, daTich.toTypedArray())
        )
    }
}
