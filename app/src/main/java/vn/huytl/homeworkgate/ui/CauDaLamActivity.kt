package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.databinding.StActivityCauDaLamBinding
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cau da lam dung cua mot mon: xem lai de, cau minh da tra loi va loi giai (Ba Huy chot
 * 2/10/2026).
 *
 * VI SAO CO. Bai co giao chup anh da co man Bai da cham, de thi thu co nut "Xem bài đã làm", con
 * cau lam tren may (luyen tap, on tap, cau trong de) thi lam xong mot luot la het: khong co cho
 * nao xem lai cau minh da lam dung. Mo tu dong "Câu đã làm đúng" o tung khu mon cua trang Luyen
 * tap (Ba Huy chon cach nay, khong chon mo tu man Viec da lam xep theo ngay). Chi tren tablet:
 * dien thoai Ba Huy chua doc duoc tung cau.
 *
 * GOM NHOM CHO DE DOC, DE TIM (Ba Huy dan cung ngay). Moi phan mot muc (Toan: Dai so, Hinh hoc;
 * KHTN: Hoa hoc, Vat li, Sinh hoc; Tieng Anh: sach bai tap), cau trong de thi gom rieng muc "Đề
 * thi thử" theo tung de. Trong muc moi bai mot dong kem so cau, xep theo thu tu sach; bam ten bai
 * moi mo cac cau cua bai, de danh sach khong dai het man hinh khi con da lam vai tram cau. Phan
 * chua co cau nao van co muc, ghi "Chưa có câu nào làm đúng", de con biet phan do con trong.
 *
 * MOI CAU GHI DAU DE BAI (Ba Huy dan): chi ghi "Câu 1.1a" thi con khong nhan ra cau nao. Bam vao
 * cau thi hien de day du (cung cach man lam bai ve, [KhungGhep.veDe]), cau con da tra loi, so sao,
 * dung ngay hay sai may lan, gio lam, roi loi giai ([KhungGhep.cacDongLoiGiai]: phuong an dung,
 * cac buoc giai) va dap an chep tu sach ([vn.huytl.homeworkgate.kho.CauHoi.dapAn]): Ba Huy muon
 * co loi giai day du cua sach du cau da dung. Dap an sach cua cau trac nghiem la chu cai theo thu
 * tu in, ma may tron phuong an, nen cau trac nghiem khong ghi dong do.
 *
 * BO TRUNG (Ba Huy chot 2/10/2026, xem anh man nay sau khi cai ban SGK). Cau da lam dung thi
 * phan lon cau tra loi cua con chinh la loi giai: xep buoc dung la dung cac buoc do, chon, ghep chu,
 * ghep cau, o chon dung la dung phuong an do. Hien ca hai khoi thi the nao cung lap mot doan y
 * het. Nen cau tra loi trung loi giai ([trungLoiGiai]) thi chi con mot khoi, nhan ghi "đúng như
 * lời giải"; con xep theo thu tu khac cung dung hay go mot dang dung khac thi van hien ca hai de
 * so. Cau tra loi xep buoc moi buoc mot dong nhu loi giai, khong noi bang dau "|". Dap an sach
 * van giu: chu cua sach viet thanh cau van, khac loi giai mau.
 *
 * Moi cau lay luot dung gan nhat ([LamTrenMay.cauDaLamDung]). Chi xem, khong co nut lam lai (Ba
 * Huy chot): cau da dung du sao thi lam lai khong them phut, cau tung sai thi da quay lai o On tap.
 */
class CauDaLamActivity : AppCompatActivity() {

    private lateinit var b: StActivityCauDaLamBinding
    private var mon = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = StActivityCauDaLamBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
        b.nutQuayLai.setOnClickListener { finish() }
        mon = intent.getStringExtra(EXTRA_MON).orEmpty()
        b.phuDe.text = GiaiDe.tenMon(mon)
        nap()
    }

    /** Mot bai (hay mot de thi) va cac cau da lam dung cua no, theo thu tu in. */
    private data class Bai(val ten: String, val cac: List<LamTrenMay.CauDung>)

    /** Mot muc: mot phan hoc, sach bai tap Tieng Anh, hay cac de thi thu. */
    private data class Muc(val ten: String, val cacBai: List<Bai>, val anKhiRong: Boolean = false)

    private fun nap() {
        lifecycleScope.launch {
            val ct = this@CauDaLamActivity
            val cac = withContext(Dispatchers.IO) {
                runCatching { LamTrenMay.cauDaLamDung(ct, mon) }.getOrDefault(emptyList())
            }
            b.phuDe.text = "${GiaiDe.tenMon(mon)} · ${cac.size} câu"
            ve(gom(cac))
        }
    }

    /** Gom theo muc roi theo bai. Bai xep theo thu tu sach: quyen trong [NganHang.SACH], roi thu tu in. */
    private fun gom(cac: List<LamTrenMay.CauDung>): List<Muc> {
        val quyen = NganHang.SACH.map { it.nguon }
        fun cacBai(l: List<LamTrenMay.CauDung>): List<Bai> =
            l.sortedWith(compareBy({ quyen.indexOf(it.cau.nguon).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it.cau.thuTu }))
                .groupBy { it.cau.bai }
                .map { (ten, cs) -> Bai(ten, cs) }
        val (deThi, sach) = cac.partition { NganHang.sachTheoNguon(it.cau.nguon)?.deThi == true }
        val ra = mutableListOf<Muc>()
        val cacPhan = PhanHoc.cuaMon(mon)
        if (cacPhan.isEmpty()) {
            ra += Muc("SÁCH BÀI TẬP", cacBai(sach))
        } else {
            cacPhan.forEach { p -> ra += Muc(p.tenDai.uppercase(), cacBai(sach.filter { PhanHoc.phanCuaCau(it.cau) == p })) }
            // Muc "Bài tập ôn tập cuối năm" khong thuoc chuong nao nen khong vao phan nao.
            ra += Muc("KHÁC", cacBai(sach.filter { PhanHoc.phanCuaCau(it.cau) == null }), anKhiRong = true)
        }
        ra += Muc("ĐỀ THI THỬ", cacBai(deThi), anKhiRong = true)
        return ra
    }

    private fun ve(cacMuc: List<Muc>) {
        b.box.removeAllViews()
        if (cacMuc.all { it.cacBai.isEmpty() }) {
            b.box.addView(chu(
                "Chưa có câu nào làm đúng trên máy. Làm đúng một câu ở Luyện tập, Ôn tập hay đề thi " +
                    "là câu đó vào đây.",
                16f, mau = R.color.ink_soft
            ).apply { setPadding(0, 16.dp(), 0, 0) })
            return
        }
        cacMuc.forEach { m ->
            if (m.cacBai.isEmpty() && m.anKhiRong) return@forEach
            val so = m.cacBai.sumOf { it.cac.size }
            val nhan = layoutInflater.inflate(R.layout.st_nhan_nhom, b.box, false) as TextView
            nhan.text = if (so > 0) "${m.ten} · $so CÂU" else m.ten
            b.box.addView(nhan)
            val the = layoutInflater.inflate(R.layout.st_the_nhom, b.box, false)
            val trong = the.findViewById<LinearLayout>(R.id.ben_trong)
            if (m.cacBai.isEmpty()) {
                trong.addView(chu("Chưa có câu nào làm đúng", 15f, mau = R.color.ink_soft).apply {
                    setPadding(18.dp(), 14.dp(), 18.dp(), 14.dp())
                })
            }
            m.cacBai.forEachIndexed { i, bai -> themBai(trong, bai, keTren = i > 0) }
            b.box.addView(the)
        }
    }

    /** Dong ten bai kem so cau; bam la mo hay dong cac cau cua bai. Cau chi dung luc mo lan dau. */
    private fun themBai(trong: LinearLayout, bai: Bai, keTren: Boolean) {
        if (keTren) trong.addView(ke())
        val ds = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val mui = chu("›", 22f, mau = R.color.ink_soft)
        val dau = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(18.dp(), 14.dp(), 18.dp(), 14.dp())
            nenBam(this)
            addView(chu(bai.ten, 17f, dam = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(chu("${bai.cac.size} câu", 15f, mau = R.color.ink_soft).apply { setPadding(12.dp(), 0, 12.dp(), 0) })
            addView(mui)
        }
        dau.setOnClickListener {
            if (ds.childCount == 0) bai.cac.forEach { themCau(ds, it) }
            val mo = ds.visibility != View.VISIBLE
            ds.visibility = if (mo) View.VISIBLE else View.GONE
            mui.rotation = if (mo) 90f else 0f
        }
        trong.addView(dau)
        trong.addView(ds)
    }

    /**
     * Mot cau: so cau, sao, va dau de bai hai dong (Ba Huy dan: chi ghi so cau thi khong nhan ra).
     * Bam la mo chi tiet ngay duoi; luc mo thi an dau de vi chi tiet da co de day du.
     */
    private fun themCau(ds: LinearLayout, cd: LamTrenMay.CauDung) {
        val ghep = Ghep.doc(cd.cau.ghep)
        ds.addView(ke(thut = true))
        val chiTiet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 8.dp(), 0, 4.dp())
        }
        val dauDe = chu(KhungGhep.hien(ghep?.hoi?.ifBlank { null } ?: cd.cau.de, cd.cau.mon), 15f, mau = R.color.ink_soft).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 2.dp(), 0, 0)
        }
        val dong = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(34.dp(), 12.dp(), 18.dp(), 12.dp())
            nenBam(this)
            addView(LinearLayout(this@CauDaLamActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                addView(chu("Câu ${cd.cau.ma}", 16f, dam = true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(chu(
                    HangSao.chu(this@CauDaLamActivity, HangSao.cacOLanTruoc(cd.luot.sao.coerceAtLeast(0), cd.luot.saoToiDa)),
                    17f
                ))
            })
            addView(dauDe)
            addView(chiTiet)
        }
        dong.setOnClickListener {
            if (chiTiet.childCount == 0) veChiTiet(chiTiet, cd)
            val mo = chiTiet.visibility != View.VISIBLE
            chiTiet.visibility = if (mo) View.VISIBLE else View.GONE
            dauDe.visibility = if (mo) View.GONE else View.VISIBLE
        }
        ds.addView(dong)
    }

    /** De day du, cau con da tra loi, sao va gio lam, roi loi giai va dap an sach. */
    private fun veChiTiet(hop: LinearLayout, cd: LamTrenMay.CauDung) {
        val cau = cd.cau
        val l = cd.luot
        val muc = runCatching { LamTrenMay.muc(this, cau) }.getOrNull()
        val g = muc?.ghep
        if (muc != null) KhungGhep.veDe(hop, muc)
        else hop.addView(chu(KhungGhep.hien(cau.de, cau.mon), 17f))

        // Bieu thuc luu lien phim ("(x−y)(x+y+8)"): them dau cach quanh dau nhu luc con dang go.
        val traLoi = if (g is Ghep.BieuThuc) KhungGhep.hienBieuThuc(l.ketQua.map { it.toString() }, cau.mon)
        else l.ketQua
        val loiGiai = g?.let { KhungGhep.cacDongLoiGiai(it) }.orEmpty()
        val trung = g != null && trungLoiGiai(g, traLoi, loiGiai, cau.mon)
        val ten = getString(R.string.child_name)
        hop.addView(nhanNho(if (trung) "$ten trả lời, đúng như lời giải" else "$ten trả lời"))
        val dongTraLoi = (if (g is Ghep.Buoc) tachBuoc(traLoi) else emptyList()).ifEmpty { listOf(traLoi) }
        dongTraLoi.forEach { hop.addView(chu(KhungGhep.hien(it.ifBlank { "—" }, cau.mon), 17f, dam = true)) }
        val ketQua = if (l.lanSai <= 0) "Đúng ngay lần đầu, được ${l.sao.coerceAtLeast(0)}/${l.saoToiDa} sao"
        else "Sai ${l.lanSai} lần rồi làm đúng, được ${l.sao.coerceAtLeast(0)}/${l.saoToiDa} sao"
        hop.addView(chu("$ketQua · ${GIO.format(Date(l.luc))}", 14f, mau = R.color.ink_soft).apply {
            setPadding(0, 4.dp(), 0, 0)
        })

        // Dap an sach trung mot dong loi giai thi khong ghi lai: cau ghep chu "book", hay buoc
        // cuoi "= (x − y)(x + y + 8)" cua bieu thuc.
        val dapSach = cau.dapAn.trim().takeIf { d ->
            d.isNotEmpty() && g !is Ghep.Chon && loiGiai.none { goc(it, cau.mon) == goc(d, cau.mon) }
        }
        val hienLoiGiai = loiGiai.isNotEmpty() && !trung
        if (!hienLoiGiai && dapSach == null) return
        val giai = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_ghi_chu)
            setPadding(12.dp(), 10.dp(), 12.dp(), 12.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dp() }
        }
        if (hienLoiGiai) {
            giai.addView(nhanNho("Lời giải", tren = 0))
            loiGiai.forEach { giai.addView(chu(KhungGhep.hien(it, cau.mon), 17f)) }
        }
        if (dapSach != null) {
            giai.addView(nhanNho("Đáp án trong sách", tren = if (hienLoiGiai) 10 else 0))
            giai.addView(chu(KhungGhep.hien(dapSach, cau.mon), 16f))
        }
        hop.addView(giai)
    }

    private fun nhanNho(noi: String, tren: Int = 12): TextView = chu(noi, 13f, mau = R.color.ink_soft, dam = true).apply {
        setPadding(0, tren.dp(), 0, 2.dp())
    }

    private fun chu(noi: CharSequence, co: Float, mau: Int = R.color.ink, dam: Boolean = false): TextView =
        TextView(this).apply {
            text = noi
            textSize = co
            setTextColor(ContextCompat.getColor(this@CauDaLamActivity, mau))
            if (dam) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setLineSpacing(2f * resources.displayMetrics.density, 1f)
        }

    /** Vach ke mong giua hai dong; [thut] thi lui vao cho bang dong cau. */
    private fun ke(thut: Boolean = false): View = View(this).apply {
        setBackgroundColor(ContextCompat.getColor(this@CauDaLamActivity, R.color.line))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
            marginStart = (if (thut) 34 else 18).dp()
            marginEnd = 18.dp()
        }
    }

    private fun nenBam(v: View) {
        val tv = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        v.setBackgroundResource(tv.resourceId)
        v.isClickable = true
        v.isFocusable = true
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_MON = "mon"

        private val GIO = SimpleDateFormat("HH:mm 'ngày' dd/MM", Locale("vi", "VN"))

        fun mo(activity: Activity, mon: String) {
            activity.startActivity(Intent(activity, CauDaLamActivity::class.java).putExtra(EXTRA_MON, mon))
        }

        /** Cac buoc con da xep, theo dung cach [KhungGhep] ghi vao so cai (noi bang " | "). */
        fun tachBuoc(traLoi: String): List<String> = traLoi.split(" | ").filter { it.isNotBlank() }

        /**
         * Chu de so: doi so mu va the nhu luc hien, bo dau bang dau dong, bo moi dau cach, chu
         * thuong. "= (x − y)(x + y + 8)" va "(x−y)(x+y+8)" la mot.
         */
        fun goc(chu: String, mon: String): String = KhungGhep.hien(chu, mon).toString().trim()
            .removePrefix("=").filterNot { it.isWhitespace() }.lowercase()

        /**
         * Cau tra loi [traLoi] (chu da ghi vao so cai) noi dung y het [loiGiai]: khi do man chi hien
         * mot khoi. Trac nghiem ghi kem chu cai cua luot ("B. is on high posts"), chu cai doi theo
         * luot nen bo di truoc khi so. Bieu thuc co cac dong tinh thi khong bao gio trung: con chi
         * go ket qua cuoi.
         */
        fun trungLoiGiai(g: Ghep, traLoi: String, loiGiai: List<String>, mon: String): Boolean {
            if (traLoi.isBlank() || loiGiai.isEmpty()) return false
            val cac = when (g) {
                is Ghep.Buoc -> tachBuoc(traLoi)
                is Ghep.Chon -> traLoi.split("; ").map { it.replace(Regex("""^[A-F]\.\s*"""), "") }
                else -> listOf(traLoi)
            }
            return when (g) {
                is Ghep.Buoc -> cac.size == loiGiai.size && cac.indices.all { goc(cac[it], mon) == goc(loiGiai[it], mon) }
                // Chon nhieu dap an: con bam theo thu tu tren man, loi giai theo thu tu sach.
                is Ghep.Chon -> cac.map { goc(it, mon) }.sorted() == loiGiai.map { goc(it, mon) }.sorted()
                else -> goc(cac.joinToString(""), mon) == goc(loiGiai.joinToString(""), mon)
            }
        }
    }
}
