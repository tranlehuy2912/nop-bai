package vn.huytl.homeworkgate.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.databinding.ActivityTietKiemPinBinding
import vn.huytl.homeworkgate.dongbo.CaiDatPin
import vn.huytl.homeworkgate.guard.PhienQuanLy
import vn.huytl.homeworkgate.guard.TruyCapTuXa
import vn.huytl.homeworkgate.pin.Jokers
import vn.huytl.homeworkgate.pin.TietKiemPin

/**
 * Chinh tiet kiem pin ngay tren tablet (5/11/2026). Anh Huy chot: tablet tu lam, anh chinh tu Bang
 * dieu khien, va chinh duoc ca o day phong khi mat dien thoai. Cung bon muc voi nhom "Tiết kiệm
 * pin" ben Bang dieu khien; doi muc nao la luu ngay vao Prefs (DongBo tu ghi lai hop/caidat).
 *
 * Nam sau PIN nhu trang Cai dat, mo tu the "Tiết kiệm pin" o do.
 */
class TietKiemPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTietKiemPinBinding
    private lateinit var prefs: Prefs
    private val hoiLaiPin = HoiLaiPin(this)

    private val khiJokersDoi: () -> Unit = { runOnUiThread { if (!isDestroyed) veTinhTrang() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTietKiemPinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs.get(this)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }

        binding.swBat.setOnCheckedChangeListener { nut, bat ->
            // ve() cung dat lai cong tac; chi lam khi chinh nguoi dung gat.
            if (!nut.isPressed) return@setOnCheckedChangeListener
            prefs.pinBat = bat
            TietKiemPin.apDung(this)
        }
        // ve() chon lai nut theo Prefs cung goi vao day; so khong doi thi khong ghi.
        dungNhom(binding.nhomPhut, CaiDatPin.CAC_MUC_PHUT) {
            if (it == prefs.pinPhut) return@dungNhom
            prefs.pinPhut = it
            TietKiemPin.apDung(this)
        }
        dungNhom(binding.nhomPhutDung, CaiDatPin.CAC_MUC_PHUT_DUNG) {
            if (it == prefs.pinPhutDung) return@dungNhom
            prefs.pinPhutDung = it
            TietKiemPin.apDung(this)
        }
        binding.btnChonApp.setOnClickListener {
            startActivity(
                Intent(this, AppPickerActivity::class.java)
                    .putExtra(AppPickerActivity.EXTRA_DANH_SACH, AppPickerActivity.TAM_DUNG)
            )
        }
        binding.btnJokers.setOnClickListener {
            // Guard chan Jokers nhu moi app la; mo duong vai phut nhu khi di bat quyen trong Cai
            // dat, de hop hoi hay man "Bat dau" cua Jokers khong bi day ve.
            PhienQuanLy.choMoCaiDat(5)
            when (Jokers.tinhTrang(this)) {
                CaiDatPin.JOKERS_CHUA_CHO_PHEP -> Jokers.xinPhep()
                CaiDatPin.JOKERS_CHUA_CHAY -> Jokers.moApp(this)
            }
        }
        Jokers.nghe(khiJokersDoi)
    }

    override fun onResume() {
        super.onResume()
        hoiLaiPin.roiMoi { ve() }
    }

    override fun onDestroy() {
        Jokers.boNghe(khiJokersDoi)
        super.onDestroy()
    }

    /** Mot nut chon cho moi muc so phut, chon mot. Bam la luu ngay qua [khiChon]. */
    private fun dungNhom(nhom: ChipGroup, cacMuc: List<Int>, khiChon: (Int) -> Unit) {
        for (muc in cacMuc) {
            nhom.addView(Chip(this).apply {
                id = View.generateViewId()
                tag = muc
                text = if (muc == 0) "Ngay" else "$muc phút"
                isCheckable = true
            })
        }
        nhom.setOnCheckedStateChangeListener { g, ids ->
            val muc = ids.firstOrNull()?.let { g.findViewById<Chip>(it)?.tag as? Int } ?: return@setOnCheckedStateChangeListener
            khiChon(muc)
        }
    }

    private fun chonNhom(nhom: ChipGroup, muc: Int) {
        for (i in 0 until nhom.childCount) {
            val chip = nhom.getChildAt(i) as Chip
            if (chip.tag == muc && !chip.isChecked) chip.isChecked = true
        }
    }

    private fun ve() {
        binding.swBat.isChecked = prefs.pinBat
        chonNhom(binding.nhomPhut, prefs.pinPhut)
        chonNhom(binding.nhomPhutDung, prefs.pinPhutDung)
        binding.btnChonApp.text = "Chọn app tạm dừng (${prefs.tamDungPackages.size} app)"
        veTinhTrang()
    }

    /** Hai thu tiet kiem pin can, va nut sua ngay tren may neu sua duoc. */
    private fun veTinhTrang() {
        val dong = mutableListOf<String>()
        dong += if (TruyCapTuXa.coQuyen(this)) {
            "Bật Tiết kiệm pin của máy: được."
        } else {
            "Chưa bật được Tiết kiệm pin của máy: app thiếu quyền ghi cài đặt hệ thống. " +
                "Cấp một lần bằng máy tính:\nadb shell pm grant $packageName " +
                "android.permission.WRITE_SECURE_SETTINGS"
        }
        val jokers = Jokers.tinhTrang(this)
        dong += when (jokers) {
            CaiDatPin.JOKERS_SAN_SANG -> "Tạm dừng app: được, Jokers Permissions đang chạy."
            CaiDatPin.JOKERS_CHUA_CAI -> "Chưa tạm dừng được app: tablet chưa cài Jokers Permissions."
            CaiDatPin.JOKERS_CHUA_CHAY ->
                "Chưa tạm dừng được app: Jokers Permissions chưa chạy. Mở Jokers rồi bấm Bắt đầu " +
                    "(cần Wi-Fi và đang bật gỡ lỗi USB)."
            else -> "Chưa tạm dừng được app: app Nộp bài chưa được phép dùng Jokers Permissions."
        }
        val conDung = TietKiemPin.dangTamDung(this)
        if (conDung.isNotEmpty()) {
            dong += "Còn ${conDung.size} app đang tạm dừng, sẽ mở lại khi Jokers chạy lại."
        }
        binding.txtTinhTrang.text = dong.joinToString("\n\n")

        binding.btnJokers.visibility = View.VISIBLE
        when (jokers) {
            CaiDatPin.JOKERS_CHUA_CHO_PHEP -> binding.btnJokers.text = "Cho phép dùng Jokers Permissions"
            CaiDatPin.JOKERS_CHUA_CHAY -> binding.btnJokers.text = "Mở Jokers Permissions"
            else -> binding.btnJokers.visibility = View.GONE
        }
    }
}
