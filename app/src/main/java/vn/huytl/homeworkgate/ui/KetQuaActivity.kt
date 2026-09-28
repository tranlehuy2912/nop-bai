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
import com.google.android.material.button.MaterialButton
import vn.huytl.homeworkgate.data.BaiDaCham
import vn.huytl.homeworkgate.data.KhaiChoCham
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.databinding.StActivityKetQuaBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.CauNgoai
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Bai da cham: moi lan nop mot the, trong the tung cau dung hay sai va sai o dau.
 *
 * VI SAO CO MAN NAY. Tu ban viet lai man chinh ngay 18/9/2026, dong "Sua N cau roi
 * chup lai" chi con ghi ma ba cau dau. Loi nhan xet cho tung cau van nam trong ban
 * cham, nhung khong con cho nao tren tablet hien no ra: con biet la sai ma khong biet
 * sai o dau. Ba Huy thi thay het tren Bang dieu khien. Man nay cho con xem dung cai
 * Ba Huy xem, bot phan viet cho nguoi lon.
 *
 * NOP LAI THEO TUNG BAI (Ba Huy chon ngay 28/9/2026). The nao con cau phai sua thi co
 * nut "Nộp lại N câu sai" o cuoi the, va chi nut do moi mo camera chup lai. Truoc day
 * cuoi man co mot nut "Chụp lại" chung, gom cau sai cua moi bai moi ngay nhung chi lay
 * mot quyen: con khong biet anh phai co nhung cau nao, cau quyen khac bi bo lai lang le.
 * Man chinh cung khong con nut chup lai nao: dong "Có N câu cần sửa" chi mo man nay.
 *
 * The giu nguyen ket qua cua lan cham do, nhung moi cau sai co them tinh trang HIEN TAI
 * lay tu so cai: da sua dung o lan nop sau, hay dang cho cham lan nop lai. Nhung cau do
 * khong tinh vao nut.
 *
 * KHONG HIEN TOM TAT CUA BAN CHAM. Tom tat viet cho Ba Huy: so phut, lenh rut lai gio,
 * cau nao con khai chac ma sai. Con chi can biet tung cau dung hay sai, sai thi can sua
 * gi.
 *
 * Cham co the nham, nhu cau 2.33a ngay 23/9/2026: con lam dung ma bi bao sai. Nen cuoi
 * danh sach co mot dong dan con nhan Ba Huy khi thay minh dung ma bi bao sai.
 *
 * KHI BA HUY DA NHO CLAUDE CHAM LAI, xem [vn.huytl.homeworkgate.data.SuaCham], moi cau
 * hien theo ket luan cua Claude, kem mot dong noi ro cho nao lan cham truoc da nham.
 */
class KetQuaActivity : AppCompatActivity() {

    private lateinit var binding: StActivityKetQuaBinding
    private var nghe: ListenerRegistration? = null

    /** Da ve duoc danh sach lan nao chua. Roi thi mot lan doc hong khong xoa no di. */
    private var daCoDanhSach = false

    private val gio = SimpleDateFormat("HH:mm", VN)
    private val ngay = SimpleDateFormat("dd/MM", VN)

    /**
     * Tinh trang hien tai cua cac cau, doc tu so cai mot lan moi lan ve danh sach.
     *
     * [conSua]: so cai con ghi cau do la sai. [choCham]: cau dang nam trong mot bai cho
     * chua cham, xem [KhaiChoCham.cauChoCham]. [daXong]: cau da lam dung va da tra gio.
     */
    private class TinhTrang(val conSua: Set<String>, val choCham: Set<String>, val daXong: Set<String>)

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
        val tt = docTinhTrang(ds)
        binding.trong.visibility = View.GONE
        binding.danhSach.removeAllViews()
        ds.forEach { binding.danhSach.addView(theBai(it, tt)) }
        binding.danhSach.addView(
            chu(
                "Chấm có thể nhầm. Câu nào ${getString(R.string.child_name)} chắc mình làm " +
                    "đúng mà bị chấm sai thì nhắn ${getString(R.string.parent_name_cap)} nhé.",
                13f, mau = R.color.ink_soft
            ).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(4) }
        )
    }

    private fun docTinhTrang(ds: List<BaiDaCham>): TinhTrang {
        val khoa = ds.flatMap { b -> b.cac.orEmpty().mapNotNull { khoaCua(it) } }.distinct()
        val han = System.currentTimeMillis() - 365L * 24 * 60 * 60_000L
        return runCatching {
            TinhTrang(
                conSua = SoCaiBai.dangChoSua(this).map { it.khoa }.toSet(),
                choCham = KhaiChoCham.cauChoCham(this),
                daXong = KhoBai.get(this).daXongTrong(khoa, han)
            )
        }.getOrElse { TinhTrang(emptySet(), emptySet(), emptySet()) }
    }

    /**
     * Khoa so cai cua mot cau trong the: ma sach neu con khai theo sach, khong thi de bai
     * da chuan hoa, y nhu [SoCaiBai.khoaCua]. null la cau khong co de, so cai khong ghi.
     */
    private fun khoaCua(c: BaiDaCham.Cau): String? =
        c.cauId ?: SoCaiBai.chuanHoa(c.de).takeIf { it.isNotEmpty() }?.let { SoCaiBai.DAU_NGOAI_SACH + it }

    private fun hienTrong(noi: String) {
        binding.danhSach.removeAllViews()
        binding.trong.text = noi
        binding.trong.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------ mot lan nop

    private fun theBai(b: BaiDaCham, tt: TinhTrang): View {
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
                    "${getString(R.string.parent_name_cap)} đã nhờ Claude chấm" +
                        (if (b.claudeChinh) "" else " lại") + " lúc $luc.",
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

        // "Khong ro" tach rieng khoi "sai": chua doc ra chu con viet thi chua noi duoc la
        // con sai, va dem no vao so cau sai la bat con sua mot cau co khi dung. Dem theo
        // ket luan cuoi cung, tuc la cua Claude khi da cham lai.
        val khongRo = cac.count { it.chuaRo }
        val dung = cac.count { !it.chuaRo && it.dungCuoi }
        val sai = cac.filter { !it.chuaRo && !it.dungCuoi }
        val daSua = sai.count { c -> khoaCua(c)?.let { it in tt.daXong } == true }
        val tomTat = if (sai.isEmpty() && khongRo == 0) {
            "Đúng hết $dung câu"
        } else {
            listOfNotNull(
                "Đúng $dung câu".takeIf { dung > 0 },
                "Sai ${sai.size} câu".takeIf { sai.isNotEmpty() },
                "đã sửa đúng $daSua".takeIf { daSua > 0 },
                "$khongRo câu chưa đọc rõ".takeIf { khongRo > 0 }
            ).joinToString(" · ")
        }
        the.addView(
            chu(tomTat, 15f, mau = if (sai.isEmpty() && khongRo == 0) R.color.ok else R.color.ink_soft)
                .apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(2) }
        )
        cac.forEach { the.addView(dongCau(it, tt)) }

        // Chi cau so cai con ghi la sai, va chua nam trong mot lan nop lai dang cho cham.
        val canNop = sai.filter { c ->
            val k = khoaCua(c) ?: return@filter false
            k in tt.conSua && k !in tt.choCham && k !in tt.daXong
        }
        if (canNop.isNotEmpty()) the.addView(nutNopLai(b, canNop))
        return the
    }

    /**
     * Nut nop lai cac cau sai cua dung bai nay, o cuoi the.
     *
     * Lan nop mang theo dung cac cau do - cau trong sach theo ma sach, cau ngoai sach theo
     * ma va de chep tu ban cham nay - va ma bai nay, xem [PhamVi.suaBai]. Man chup hien de
     * cua cac cau do cho con biet phai chup nhung cau nao.
     *
     * Bai nay con nam cho ma khong co phut nao de duyet (sai het) thi lan nop lai thay han
     * no: gui xong tablet huy bai cu, xem [PhamVi.huyBaiCu].
     */
    private fun nutNopLai(b: BaiDaCham, cac: List<BaiDaCham.Cau>): View =
        MaterialButton(this).apply {
            text = "Nộp lại ${cac.size} câu sai"
            textSize = 17f
            cornerRadius = dp(16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
            ).apply { topMargin = dp(16) }
            setOnClickListener { nopLai(b, cac) }
        }

    private fun nopLai(b: BaiDaCham, cac: List<BaiDaCham.Cau>) {
        val trongSach = cac.mapNotNull { it.cauId }
        val nguon = trongSach.firstOrNull()?.substringBefore(':').orEmpty()
        val sach = NganHang.sachTheoNguon(nguon)
        val khi = Calendar.getInstance().apply { timeInMillis = b.luc }
        val lucNop = if (cungNgay(khi, Calendar.getInstance())) "lúc ${gio.format(Date(b.luc))}"
        else "ngày ${ngay.format(Date(b.luc))} lúc ${gio.format(Date(b.luc))}"
        val pham = PhamVi(
            mon = sach?.mon ?: b.mon,
            nguon = if (trongSach.isEmpty()) "" else nguon,
            tenNguon = sach?.ten ?: b.tenNguon,
            bai = "sửa bài $lucNop",
            cauIds = trongSach,
            cauNgoai = cac.filter { it.cauId == null }.map { CauNgoai(it.ma.trim(), it.de) },
            suaBai = b.id,
            huyBaiCu = b.trangThai == TRANG_THAI_CHO && b.phutDeNghi <= 0
        )
        startActivity(
            Intent(this, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_SUA, true)
                .putExtra(CaptureActivity.EXTRA_PHAM, pham.sangJson())
        )
    }

    private fun dongCau(c: BaiDaCham.Cau, tt: TinhTrang): View {
        // Hang lan sang le phai cua the cho icon chep thang mep chu, xem cho them nut o duoi.
        val hang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = -dp(14) }
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
        val ten = "Câu ${c.ma.ifBlank { "chưa rõ số" }}"
        cot.addView(chu(ten, 16f, bold = true))
        // Chu cua nut chep: dung cac dong dang hien, kem ket luan va chu "Đề:" cho de doc
        // khi dan sang cho khac.
        val chep = mutableListOf(
            "$ten (" + when {
                c.chuaRo -> "chưa đọc rõ"
                c.dungCuoi -> "đúng"
                else -> "chưa đúng"
            } + ")"
        )
        if (c.de.isNotBlank()) {
            val de = SoMu.hien(c.de.trim())
            cot.addView(chu(de, 14f, mau = R.color.ink_soft))
            chep += "Đề: $de"
        }
        // Claude da doc chac thi lay chu Claude doc: doc nham la mot trong hai ly do de
        // nho Claude cham lai, nhu cau 2.32b bi doc chu "b" thanh so 1.
        val cl = c.claude?.takeIf { it.chac }
        val viet = cl?.conViet?.takeIf { it.isNotBlank() } ?: c.ketQua
        if (viet.isNotBlank()) {
            val dong = "${getString(R.string.child_name)} viết: $viet"
            cot.addView(
                chu(dong, 15f)
                    .apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(4) }
            )
            chep += dong
        }

        val mayDung = c.docRo && c.dung
        val goiY = boConDau(cl?.goiY?.trim()?.takeIf { it.isNotEmpty() } ?: c.nhanXet.trim())
        val ghi: Pair<String, Int>? = when {
            c.chuaRo -> "Câu này chưa đọc rõ chữ, ${getString(R.string.parent_name)} sẽ xem lại." to R.color.wait
            cl != null && cl.dung && !mayDung ->
                "Lần chấm trước nhầm. Claude chấm lại thấy câu này " +
                    "${getString(R.string.child_name)} làm đúng." to R.color.ok
            cl != null && !cl.dung && mayDung ->
                "Claude chấm lại thấy câu này chưa đúng." +
                    (if (goiY.isNotEmpty()) " $goiY" else "") to R.color.alert
            !c.dungCuoi && goiY.isNotEmpty() -> goiY to R.color.alert
            else -> null
        }
        if (ghi != null) {
            val (noi, mauGhi) = ghi
            cot.addView(if (c.chuaRo) chu(noi, 14f, mau = mauGhi) else ghiChu(noi, mauGhi))
            chep += noi
        }
        // Tinh trang hien tai cua cau sai: the giu ket qua cua lan cham do, con cau nay co
        // khi da duoc sua o lan nop sau.
        if (!c.chuaRo && !c.dungCuoi) {
            val k = khoaCua(c)
            val bayGio: Pair<String, Int>? = when {
                k == null -> null
                k in tt.daXong -> "Đã sửa đúng rồi." to R.color.ok
                k in tt.choCham -> "Đã nộp lại, đang chờ chấm." to R.color.wait
                else -> null
            }
            if (bayGio != null) {
                cot.addView(ghiChu(bayGio.first, bayGio.second))
                chep += bayGio.first
            }
        }
        hang.addView(cot)
        // Keo nut len cho icon ngang dong "Câu ...": nut 48dp, icon chi 20dp o giua. Nut van
        // nam trong hang nho le tren 14dp cua hang, nen cham vao dau nut cung toi nut. Le
        // phai dat vao hang (o tren) chu khong vao nut: nut tran ra ngoai hang thi phan tran
        // khong nhan cham.
        hang.addView(
            Chep.nut(hang) { chep.joinToString("\n") }.apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    topMargin = -dp(13)
                }
            }
        )
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
     * cac ban cham cu van mo dau bang chu do. Nhan xet cua may cham tren tablet (bo ngay
     * 28/9/2026) cung vay, vi cau lenh cua may goi "con". Ba Huy muon man nay chi ghi
     * "Sửa ...", cho nao can goi thi goi ten.
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

        /** Trang thai bai con nam trong hang cho, ghi tren Firestore. Xem DongBo.dayBaiMoi. */
        private const val TRANG_THAI_CHO = "CHO"

        fun mo(context: Context) =
            context.startActivity(Intent(context, KetQuaActivity::class.java))
    }
}
