package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.databinding.ActivityTienBoBinding
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TienBo

/**
 * Man hinh Le Hoa nhin lai viec minh da lam.
 *
 * VI SAO CO MAN NAY: moi bang tong ket khac trong app deu la cua Ba Huy - con dung
 * app gi luc nao, con hoi AI nhung gi, nhat ky trong ngay. Con thi chi thay hai
 * thu: cai khoa, va mon no "con 2 cau can sua". Mot dua tre lam bon muoi cau dung
 * trong tuan ma khong co cho nao noi ra dieu do.
 *
 * NHUNG CON SO CHON DUA VAO DAY deu la so viec DA LAM XONG. Khong co "so cau sai",
 * khong co ty le phan tram, khong co chuoi ngay lien tiep. Ty le va chuoi ngay bien
 * viec hoc thanh mot cai bang diem phai giu, va ngay dut chuoi la ngay bo cuoc.
 * "Cau kho da go" thi nguoc lai: no bien mot lan sai thanh mot viec lam duoc.
 */
class TienBoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTienBoBinding

    private var soNgay = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTienBoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top + 28.dp(), bottom = bars.bottom + 28.dp())
            insets
        }

        binding.btnDone.setOnClickListener { finish() }
        binding.nhomKhoang.check(R.id.btn_tuan)
        binding.nhomKhoang.addOnButtonCheckedListener { _, id, chon ->
            if (!chon) return@addOnButtonCheckedListener
            soNgay = if (id == R.id.btn_thang) 30 else 7
            ve()
        }
        ve()
    }

    private fun ve() {
        val tuLuc = System.currentTimeMillis() - soNgay * 24L * 60 * 60_000L
        val tb = KhoBai.get(this).tienBo(tuLuc)

        val trong = tb.soCauDung == 0 && tb.cauKhoDaGo == 0
        binding.txtTrong.visibility = if (trong) View.VISIBLE else View.GONE
        binding.txtTrong.text = getString(R.string.tien_bo_trong)

        binding.txtTong.text = "${tb.soCauDung} câu đúng"
        binding.txtPhu.text = buildString {
            append(if (soNgay == 7) "trong 7 ngày qua" else "trong 30 ngày qua")
            if (tb.soNgayCoBai > 0) append(", ").append(tb.soNgayCoBai).append(" ngày có nộp bài")
            if (tb.phutDaKiem > 0) {
                append("\nĐổi được ").append(moTaPhut(tb.phutDaKiem)).append(" chơi")
            }
        }

        // The "cau kho da go" chi hien khi that su co. Hien mot so khong o day thi
        // no thanh cho trong cho con nhin vao, ma cho trong do khong noi len gi.
        binding.theGo.visibility = if (tb.cauKhoDaGo > 0) View.VISIBLE else View.GONE
        binding.txtGo.text = "${tb.cauKhoDaGo} câu khó đã gỡ"
        binding.txtGoPhu.text = "Mấy câu này con làm sai, rồi tự sửa lại cho đúng."

        veTuBiet(KhoBai.get(this).tuBiet(tuLuc))
        veMon(tb)

        binding.txtChan.text = when {
            tb.cauDangChoSua > 0 ->
                "Còn ${tb.cauDangChoSua} câu đang chờ sửa. Sửa xong là vào bảng này."
            trong -> ""
            else -> "Máy nhớ bài đã chấm trong một năm học."
        }
    }

    /**
     * Con tu doc bung minh truoc khi nop: co trung khong.
     *
     * VI SAO DAT O MAN NAY. Truoc khi chup, con duoc hoi "cau nao con thay chua
     * chac". Cau tra loi do nam trong may tu ban truoc nhung khong ai doc lai: no chi
     * sinh ra mot cau nhan ngay sau lan nop, roi lan nop sau ghi de len. Mot cau noi
     * mot lan la mot phan ung; cong don ca tuan lai thi no thanh mot cai guong, va
     * cai guong do soi dung thu kho day nhat - con co biet minh dang biet gi khong.
     *
     * CON SO TO nhat la [TuBiet.bietTruoc]: con bao chua chac va dung la chua chac.
     * Chon no chu khong chon "doan trung bao nhieu phan tram" vi hai le. Phan tram
     * la mot diem so, ma man nay khong co diem so nao. Va "bao chac roi lam dung"
     * chiem gan het so lan nen dua vao chi lam con so phinh len ma khong noi gi.
     *
     * DONG "tuong chac ma con sot" DUNG LA MOT SO CAU SAI, tuc la dung cai ghi chu
     * dau file bao khong dua vao. Giu lai vi no khac mot cai bang diem o cho no chi
     * thang duoc viec phai lam tiep: may cau do la cho con dang nham la minh vung.
     * Va no chi hien khi khac khong.
     */
    private fun veTuBiet(tb: vn.huytl.homeworkgate.kho.TuBiet) {
        binding.boxTuBiet.removeAllViews()
        binding.theTuBiet.visibility = if (tb.coGi()) View.VISIBLE else View.GONE
        if (!tb.coGi()) return

        binding.txtTuBiet.text = "${tb.bietTruoc} lần con biết trước chỗ mình chưa vững"
        binding.txtTuBietPhu.text =
            "Trước khi nộp con nói mấy câu đó chưa chắc, và đúng là chưa chắc thật."

        if (tb.honTuong > 0) {
            themDong("Chưa chắc mà hoá ra làm đúng: ${tb.honTuong} lần", R.color.ok)
        }
        if (tb.tuongChac > 0) {
            themDong(
                "Thấy chắc mà còn sót: ${tb.tuongChac} lần — mấy chỗ đó xem lại nhé",
                R.color.ink_soft
            )
        }
    }

    private fun themDong(chu: String, mau: Int) {
        binding.boxTuBiet.addView(TextView(this).apply {
            text = chu
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@TienBoActivity, mau))
            setPadding(0, 10.dp(), 0, 0)
        })
    }

    /**
     * Tung mon duoc bao nhieu cau, kem mot thanh ngang de nhin la thay ngay mon nao
     * dang nhieu nhat. Dai cua thanh tinh theo mon cao nhat chu khong theo tong: so
     * cau moi mon chenh nhau it thi thanh nao cung gan bang nhau, nhin nhu nhau het.
     */
    private fun veMon(tb: TienBo) {
        binding.boxMon.removeAllViews()
        binding.theMon.visibility = if (tb.theoMon.isEmpty()) View.GONE else View.VISIBLE
        if (tb.theoMon.isEmpty()) return

        val daiNhat = tb.theoMon.maxOf { it.second }.coerceAtLeast(1)
        tb.theoMon.forEachIndexed { i, (mon, so) ->
            val hang = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (i > 0) topMargin = 14.dp() }
            }

            val dong = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            dong.addView(TextView(this).apply {
                text = mon
                setTextColor(ContextCompat.getColor(this@TienBoActivity, R.color.ink))
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            dong.addView(TextView(this).apply {
                text = "$so câu"
                setTextColor(ContextCompat.getColor(this@TienBoActivity, R.color.ink_soft))
                textSize = 15f
                gravity = Gravity.END
            })
            hang.addView(dong)

            val nen = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 8.dp()
                ).apply { topMargin = 6.dp() }
                setBackgroundColor(ContextCompat.getColor(this@TienBoActivity, R.color.line))
            }
            nen.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, -1, so.toFloat() / daiNhat)
                setBackgroundColor(ContextCompat.getColor(this@TienBoActivity, R.color.ok))
            })
            // Phan con lai de trong, de thanh khong bi keo dai het hang.
            nen.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, -1, (daiNhat - so).toFloat() / daiNhat
                )
            })
            hang.addView(nen)
            binding.boxMon.addView(hang)
        }
    }

    /** "45 phút", "1 tiếng 20 phút". */
    private fun moTaPhut(phut: Int): String = when {
        phut < 60 -> "$phut phút"
        phut % 60 == 0 -> "${phut / 60} tiếng"
        else -> "${phut / 60} tiếng ${phut % 60} phút"
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
