package vn.huytl.homeworkgate.ui

import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.LoaiLoi
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
 * "Da lam duoc N cau kho" thi nguoc lai: no bien mot lan sai thanh mot viec lam
 * duoc. Man hinh khong giai thich them cau do la gi - con la nguoi lam nhung cau
 * ay, no biet ro hon bat ky dong chu nao viet ra duoc.
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
        binding.khungTrong.visibility = if (trong) View.VISIBLE else View.GONE
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
        binding.txtGo.text = "Đã làm được ${tb.cauKhoDaGo} câu khó"

        veCot(tuLuc)
        veTuBiet(KhoBai.get(this).tuBiet(tuLuc))
        veVap(tuLuc)
        veMon(tb)

        binding.txtChan.text = when {
            tb.cauDangChoSua > 0 ->
                "Còn ${tb.cauDangChoSua} câu đang chờ sửa. Sửa xong là vào bảng này."
            trong -> ""
            else -> "Máy nhớ bài đã chấm trong một năm học."
        }
    }

    /**
     * Day cot: moi ngay mot cot, cao theo so cau dung hom do.
     *
     * Ngay khong lam gi van co mot cot, cao bang mot vach mo - bo han thi day cot
     * co khoang trong khong giai thich duoc, ma "hom do khong lam gi" cung la mot
     * dieu dang thay.
     *
     * Bay ngay thi ghi nhan thu duoi chan cot; ba muoi ngay thi bo nhan di, ba muoi
     * chu chen nhau khong doc duoc ma cung khong ai doc.
     */
    private fun veCot(tuLuc: Long) {
        val theoNgay = KhoBai.get(this).cauDungTheoNgay(tuLuc)
        val dinhDang = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val lich = Calendar.getInstance()

        val cac = (soNgay - 1 downTo 0).map { lui ->
            val c = (lich.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -lui) }
            c to (theoNgay[dinhDang.format(c.time)] ?: 0)
        }
        val cao = cac.maxOf { it.second }

        binding.theCot.visibility = if (cao == 0) View.GONE else View.VISIBLE
        // Tieu de phai noi CAI GI dang duoc dem. "Tung ngay trong tuan" chi noi
        // truc ngang la ngay, con cot cao thap la cai gi thi khong ai doan ra.
        binding.txtCotTieuDe.text = "SỐ CÂU ĐÚNG MỖI NGÀY"
        binding.boxCot.removeAllViews()
        if (cao == 0) return

        cac.forEach { (ngay, so) ->
            val cot = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
            if (so > 0) {
                cot.addView(chuGiuaCot(so.toString()))
            }
            // Cao toi thieu 3dp de ngay trong van con mot vach nhin thay.
            val chieuCao = if (so == 0) 3.dp() else (so * 88.dp() / cao).coerceAtLeast(6.dp())
            cot.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(18.dp(), chieuCao).apply {
                    topMargin = 4.dp()
                    marginStart = 3.dp()
                    marginEnd = 3.dp()
                }
                setBackgroundResource(R.drawable.st_nen_cot)
                backgroundTintList = ContextCompat.getColorStateList(
                    this@TienBoActivity, if (so == 0) R.color.line else R.color.ok
                )
            })
            if (soNgay == 7) {
                cot.addView(chuGiuaCot(tenThuNgan(ngay), tren = 6.dp()))
            }
            binding.boxCot.addView(cot)
        }
    }

    /**
     * Mot dong chu nam GIUA o cot.
     *
     * Phai dat gravity tay: LinearLayout doc phat cho con no be rong match_parent,
     * nen mot TextView tha vao se dinh le trai - va ca hang nhan ngay lech han khoi
     * cac cot ma no dang goi ten.
     */
    private fun chuGiuaCot(chu: String, tren: Int = 0): TextView = TextView(this).apply {
        text = chu
        textSize = 12f
        gravity = android.view.Gravity.CENTER_HORIZONTAL
        setPadding(0, tren, 0, 0)
        setTextColor(ContextCompat.getColor(this@TienBoActivity, R.color.ink_soft))
    }

    private fun tenThuNgan(c: Calendar): String = when (c.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "T2"
        Calendar.TUESDAY -> "T3"
        Calendar.WEDNESDAY -> "T4"
        Calendar.THURSDAY -> "T5"
        Calendar.FRIDAY -> "T6"
        Calendar.SATURDAY -> "T7"
        else -> "CN"
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

        binding.txtTuBiet.text = "${tb.bietTruoc} lần Lê Hòa biết trước chỗ mình chưa vững"
        binding.txtTuBietPhu.text =
            "Trước khi nộp Lê Hòa nói mấy câu đó chưa chắc, và đúng là chưa chắc thật."

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

    /**
     * Cho con hay vap, va ba cau de lam thu.
     *
     * VI SAO CO THE NAY. Bay nhan loi la thu dat gia nhat app dang ghi lai, ma tu
     * truoc den gio chi mot minh Ba Huy doc duoc qua lenh /loi ben Telegram. Nguoi
     * lam nhung cau do thi khong thay gi ca, nen no khong sua duoc cai gi - no chi
     * biet minh sai, khong biet minh sai KIEU gi.
     *
     * BA RANG BUOC, de the nay khong tro thanh mot bang diem tru:
     *
     *  - khong hien so lan. Xem ghi chu trong layout;
     *  - chi mot nhan, cai dung dau. Ke ca bay nhan la ke ra mot danh sach toi loi,
     *    ma doc xong khong biet bat dau tu dau;
     *  - luon di kem viec lam duoc. Chi ra cho sai roi de do la noi voi dua tre mot
     *    dieu no khong lam gi duoc - va lan sau no se khong mo man hinh nay nua.
     *
     * Het cau chua lam trong may bai do thi van hien cho vap, chi bo nut di: cau
     * "con hay vap cho nay" tu no da dang gia, con ba cau chi la duong di tiep.
     */
    private fun veVap(tuLuc: Long) {
        val kho = KhoBai.get(this)
        val nhan = kho.nhanHayVap(tuLuc)?.first
        val moTa = nhan?.let { LoaiLoi.moTaChoCon(it) }
        if (nhan == null || moTa == null) {
            binding.theVap.visibility = View.GONE
            return
        }
        binding.theVap.visibility = View.VISIBLE
        binding.txtVapNhan.text = "CHỖ HAY VẤP"
        binding.txtVap.text = moTa.replaceFirstChar { it.uppercase() }
        binding.txtVapPhu.text =
            "Mấy lần gần đây Lê Hòa vướng ở chỗ này nhiều hơn cả."

        val cau = kho.cacCauLuyenTheoLoi(nhan)
        binding.btnLuyen.visibility = if (cau.isEmpty()) View.GONE else View.VISIBLE
        if (cau.isEmpty()) return
        binding.btnLuyen.text =
            if (cau.size == 1) "Làm thử một câu" else "Làm thử ${cau.size} câu"
        binding.btnLuyen.setOnClickListener {
            startActivity(
                Intent(this, ChonBaiActivity::class.java)
                    .putExtra(ChonBaiActivity.EXTRA_LUYEN, nhan)
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
