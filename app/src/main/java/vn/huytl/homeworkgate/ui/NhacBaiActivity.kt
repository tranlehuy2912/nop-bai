package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.NhacBai
import vn.huytl.homeworkgate.databinding.StActivityNhacBaiBinding

/**
 * Man "Bài dặn dò sắp tới": moi dong vo dan do chua toi han, gom theo buoi, buoi som truoc.
 *
 * CUNG DANH SACH VOI THE "Bài dặn dò sắp tới" TREN BANG DIEU KHIEN (Ba Huy chon 30/9/2026).
 * Ca hai doc tu [NhacBai.sapToi]: tablet day dung danh sach nay len hop/nhacbai, xem
 * [vn.huytl.homeworkgate.dongbo.DongBo.banNhacBai]. Chu tung dong cung viet mot kieu voi the
 * ben dien thoai, de Ba Huy va Le Hoa noi ve cung mot dong thi thay cung mot chu.
 *
 * Truoc do bam dong "Bài cho ..." o man chinh chi mo mot hop thoai cua rieng buoi do.
 */
class NhacBaiActivity : AppCompatActivity() {

    private lateinit var binding: StActivityNhacBaiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StActivityNhacBaiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
        binding.nutQuayLai.setOnClickListener { finish() }
    }

    /** Ve lai moi lan quay vao: toi gio vao hoc thi cac dong cua buoi do roi khoi danh sach. */
    override fun onResume() {
        super.onResume()
        ve()
    }

    private fun ve() {
        val cac = runCatching { NhacBai.sapToi(this) }.getOrDefault(emptyList())
        binding.trong.visibility = if (cac.isEmpty()) View.VISIBLE else View.GONE
        binding.danhSach.removeAllViews()
        cac.forEach { n ->
            val the = layoutInflater.inflate(R.layout.st_the_nhac_bai, binding.danhSach, false)
            the.findViewById<TextView>(R.id.ten_buoi).text = tenBuoi(n)
            the.findViewById<TextView>(R.id.cac_dong).text = cacDong(n)
            binding.danhSach.addView(the)
        }
    }

    companion object {
        /** "Chiều thứ năm 1/10", nhu dau moi khoi tren the ben dien thoai. */
        fun tenBuoi(n: NhacBai.NhomBuoi): String = n.ten().replaceFirstChar { it.uppercase() }

        /**
         * Moi dong mot dau "•", bai tap hay dan do khac cung vay (Ba Huy chon 30/9/2026), bai
         * tap dung truoc. Cuoi dong la ngay tren vo: "(vở 29/9)".
         */
        fun cacDong(n: NhacBai.NhomBuoi): String =
            (n.cacBai + n.dongKhac).joinToString("\n") { "• " + NhacBai.moTa(it) }
    }
}
