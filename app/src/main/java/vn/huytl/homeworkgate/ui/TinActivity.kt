package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.databinding.StActivityTinBinding

/** Danh sach day du tin co giao, moi nhat len tren. */
class TinActivity : AppCompatActivity() {

    private lateinit var binding: StActivityTinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StActivityTinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }

        val kho = KhoTinCuaCo(this)
        val tin = kho.danhSach()

        binding.tinTrong.visibility = if (tin.isEmpty()) View.VISIBLE else View.GONE
        binding.danhSachTin.removeAllViews()
        tin.forEach { t ->
            val dong = LayoutInflater.from(this)
                .inflate(R.layout.st_dong_tin, binding.danhSachTin, false)
            dong.findViewById<TextView>(R.id.tin_gio).text = t.gioGui()
            dong.findViewById<TextView>(R.id.tin_noi_dung).text = t.noiDung
            binding.danhSachTin.addView(dong)
        }

        // Mo man nay la coi nhu da doc het.
        kho.danhDauDaDocHet()
    }
}
