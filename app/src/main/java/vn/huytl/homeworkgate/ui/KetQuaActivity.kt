package vn.huytl.homeworkgate.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.BaiDaCham
import vn.huytl.homeworkgate.databinding.StActivityKetQuaBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Bai da cham: moi lan nop mot the, trong the tung cau dung hay sai va sai o dau.
 *
 * VI SAO CO MAN NAY. Tu ban viet lai man chinh ngay 18/9/2026, dong "Sua N cau roi
 * chup lai" chi con ghi ma ba cau dau. Loi nhan xet cua may cho tung cau van nam
 * trong ban cham, nhung khong con cho nao tren tablet hien no ra: con biet la sai ma
 * khong biet sai o dau. Ba Huy thi thay het tren Bang dieu khien. Man nay cho con
 * xem dung cai Ba Huy xem, bot phan viet cho nguoi lon.
 *
 * Mo tu hai cho tren man chinh: dong "Sua N cau" va dong loi nhan cua Ba Huy. Mo tu
 * dong "Sua N cau" thi co them nut chup lai o duoi, xem xong la chup luon.
 *
 * KHONG HIEN TOM TAT CUA MAY. Tom tat trong ban cham viet cho Ba Huy: so phut, lenh
 * rut lai gio, cau nao con khai chac ma sai. Con chi can biet tung cau dung hay sai,
 * sai thi may noi gi.
 *
 * May cham co the nham, nhu cau 2.33a ngay 23/9/2026: con lam dung ma may bao sai.
 * Nen cuoi danh sach co mot dong dan con nhan Ba Huy khi thay minh dung ma bi bao sai.
 *
 * KHI BA HUY DA NHO CLAUDE CHAM LAI, xem [vn.huytl.homeworkgate.data.SuaCham], moi cau
 * hien theo ket luan cua Claude, kem mot dong noi ro cho nao may da cham nham. Goi y
 * cua Claude cho cau sai thay cho nhan xet cua may: Claude viet cho con tu sua, con
 * nhan xet cua may thi co khi chi sai cho, nhu cau 2.34b hom do.
 */
class KetQuaActivity : AppCompatActivity() {

    private lateinit var binding: StActivityKetQuaBinding
    private var nghe: ListenerRegistration? = null

    /** Da ve duoc danh sach lan nao chua. Roi thi mot lan doc hong khong xoa no di. */
    private var daCoDanhSach = false

    private val gio = SimpleDateFormat("HH:mm", VN)
    private val ngay = SimpleDateFormat("dd/MM", VN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StActivityKetQuaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
        binding.nutQuayLai.setOnClickListener { finish() }

        val sua = intent.getBooleanExtra(EXTRA_SUA, false)
        binding.khungNut.visibility = if (sua) View.VISIBLE else View.GONE
        binding.nutChupLai.setOnClickListener {
            // Khong dong man nay: chup va gui xong, man chup tu dong lai va con quay ve
            // day, thay luon ket qua moi hien vao.
            startActivity(
                Intent(this, CaptureActivity::class.java)
                    .putExtra(CaptureActivity.EXTRA_SUA, true)
                    .putExtra(CaptureActivity.EXTRA_PHAM, intent.getStringExtra(EXTRA_PHAM))
            )
        }
    }

    override fun onStart() {
        super.onStart()
        nghe = DongBo.ngheBaiDaCham(this, SO_BAI) { ds -> ve(ds) }
    }

    override fun onStop() {
        nghe?.remove()
        nghe = null
        super.onStop()
    }

    private fun ve(ds: List<BaiDaCham>?) {
        if (ds == null) {
            if (!daCoDanhSach) hienTrong("Chưa xem được kết quả. Kiểm tra mạng rồi mở lại nhé.")
            return
        }
        daCoDanhSach = true
        if (ds.isEmpty()) {
            hienTrong("Chưa có bài nào được chấm.")
            return
        }
        binding.trong.visibility = View.GONE
        binding.danhSach.removeAllViews()
        ds.forEach { binding.danhSach.addView(theBai(it)) }
        binding.danhSach.addView(
            chu(
                "Máy chấm có thể nhầm. Câu nào ${getString(R.string.child_name)} chắc mình làm " +
                    "đúng mà máy bảo sai thì nhắn ${getString(R.string.parent_name_cap)} nhé.",
                13f, mau = R.color.ink_soft
            ).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(4) }
        )
    }

    private fun hienTrong(noi: String) {
        binding.danhSach.removeAllViews()
        binding.trong.text = noi
        binding.trong.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------ mot lan nop

    private fun theBai(b: BaiDaCham): View {
        val the = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.st_nen_the)
            setPadding(dp(16), dp(14), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }
        the.addView(chu("${b.mon.ifBlank { "Bài nộp" }} · ${luc(b.luc)}", 18f, bold = true))
        if (b.claudeLuc > 0L) {
            val khi = Calendar.getInstance().apply { timeInMillis = b.claudeLuc }
            val luc = gio.format(Date(b.claudeLuc)) +
                if (cungNgay(khi, Calendar.getInstance())) "" else " ngày ${ngay.format(Date(b.claudeLuc))}"
            the.addView(
                chu(
                    "${getString(R.string.parent_name_cap)} đã nhờ Claude chấm lại lúc $luc.",
                    13f, mau = R.color.ink_soft
                )
            )
        }

        val cac = b.cac
        if (cac == null) {
            the.addView(
                chu("Bài này chưa có kết quả chấm từng câu.", 15f, mau = R.color.wait)
                    .apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(4) }
            )
            return the
        }

        // "Khong ro" tach rieng khoi "sai": may doc khong ra chu con viet thi chua noi
        // duoc la con sai, va dem no vao so cau can sua la bat con sua mot cau co khi
        // dung. Dem theo ket luan cuoi cung, tuc la cua Claude khi da cham lai.
        val khongRo = cac.count { it.chuaRo }
        val dung = cac.count { !it.chuaRo && it.dungCuoi }
        val sai = cac.count { !it.chuaRo && !it.dungCuoi }
        val tomTat = if (sai == 0 && khongRo == 0) {
            "Đúng hết $dung câu"
        } else {
            listOfNotNull(
                "Đúng $dung câu".takeIf { dung > 0 },
                "Cần sửa $sai câu".takeIf { sai > 0 },
                "$khongRo câu máy đọc chưa rõ".takeIf { khongRo > 0 }
            ).joinToString(" · ")
        }
        the.addView(
            chu(tomTat, 15f, mau = if (sai == 0 && khongRo == 0) R.color.ok else R.color.ink_soft)
                .apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(2) }
        )
        cac.forEach { the.addView(dongCau(it)) }
        return the
    }

    private fun dongCau(c: BaiDaCham.Cau): View {
        val hang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
        }
        val (dau, mau) = when {
            c.chuaRo -> "?" to R.color.wait
            c.dungCuoi -> "✓" to R.color.ok
            else -> "✕" to R.color.alert
        }
        hang.addView(
            chu(dau, 20f, bold = true, mau = mau).apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        )

        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        cot.addView(chu("Câu ${c.ma.ifBlank { "chưa rõ số" }}", 16f, bold = true))
        if (c.de.isNotBlank()) cot.addView(chu(c.de.trim(), 14f, mau = R.color.ink_soft))
        // Claude da doc chac thi lay chu Claude doc: may doc nham la mot trong hai ly
        // do de nho Claude cham lai, nhu cau 2.32b bi doc chu "b" thanh so 1.
        val cl = c.claude?.takeIf { it.chac }
        val viet = cl?.conViet?.takeIf { it.isNotBlank() } ?: c.ketQua
        if (viet.isNotBlank()) {
            cot.addView(
                chu("${getString(R.string.child_name)} viết: $viet", 15f)
                    .apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(4) }
            )
        }

        val mayDung = c.docRo && c.dung
        val goiY = boConDau(cl?.goiY?.trim()?.takeIf { it.isNotEmpty() } ?: c.nhanXet.trim())
        when {
            c.chuaRo -> cot.addView(chu("Máy đọc không rõ câu này.", 14f, mau = R.color.wait))
            cl != null && cl.dung && !mayDung -> cot.addView(
                ghiChu(
                    "Máy chấm nhầm. Claude chấm lại thấy câu này " +
                        "${getString(R.string.child_name)} làm đúng.",
                    R.color.ok
                )
            )
            cl != null && !cl.dung && mayDung -> cot.addView(
                ghiChu(
                    "Claude chấm lại thấy câu này chưa đúng." +
                        if (goiY.isNotEmpty()) " $goiY" else "",
                    R.color.alert
                )
            )
            !c.dungCuoi && goiY.isNotEmpty() -> cot.addView(ghiChu(goiY, R.color.alert))
        }
        hang.addView(cot)
        return hang
    }

    private fun ghiChu(noi: String, mau: Int): TextView =
        chu(noi, 14f, mau = mau).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(2)
        }

    /**
     * Bo chu "Con" o dau goi y: "Con sửa dấu ở dòng 2." thanh "Sửa dấu ở dòng 2."
     *
     * Truoc ngay 26/9/2026 loi nho gui Claude dan goi hoc sinh la "con", nen goi y trong
     * cac ban cham cu van mo dau bang chu do. Nhan xet cua may cham cung co the nhu vay,
     * vi cau lenh cham cua may van goi "con", xem [vn.huytl.homeworkgate.ai.PromptCham].
     * Ba Huy muon man nay chi ghi "Sửa ...", cho nao can goi thi goi ten.
     */
    private fun boConDau(goiY: String): String =
        if (goiY.startsWith("Con ")) {
            goiY.removePrefix("Con ").trimStart().replaceFirstChar { it.titlecase(VN) }
        } else {
            goiY
        }

    // -------------------------------------------------------------- ve vat

    /** "Hôm nay, 10:23", "Hôm qua, 21:05", hay "20/09, 16:40". */
    private fun luc(luc: Long): String {
        if (luc <= 0L) return "không rõ giờ"
        val c = Calendar.getInstance().apply { timeInMillis = luc }
        val homNay = Calendar.getInstance()
        val homQua = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val ten = when {
            cungNgay(c, homNay) -> "Hôm nay"
            cungNgay(c, homQua) -> "Hôm qua"
            else -> ngay.format(Date(luc))
        }
        return "$ten, ${gio.format(Date(luc))}"
    }

    private fun cungNgay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun chu(
        noi: String,
        co: Float,
        bold: Boolean = false,
        mau: Int = R.color.ink
    ): TextView = TextView(this).apply {
        text = noi
        textSize = co
        setTextColor(ContextCompat.getColor(context, mau))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        /** Muoi lan nop gan nhat: du cho vai ngay, ma cuon het van chua met. */
        private const val SO_BAI = 10L

        private val VN = Locale.forLanguageTag("vi-VN")

        /** Mo tu dong "Sua N cau": hien nut chup lai o duoi. */
        const val EXTRA_SUA = "ket_qua_sua"

        /** Pham vi cho lan chup sua, chuyen nguyen sang [CaptureActivity.EXTRA_PHAM]. */
        const val EXTRA_PHAM = "ket_qua_pham"

        fun mo(context: Context) =
            context.startActivity(Intent(context, KetQuaActivity::class.java))
    }
}
