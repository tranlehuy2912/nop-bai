package vn.huytl.homeworkgate.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.databinding.StActivityGiaiDeBinding
import vn.huytl.homeworkgate.databinding.StTheCauDeBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier

/**
 * Man Giai de: mot de may ra tu sach bai tap, lam mot mach co dong ho. Luat ra de va
 * cham de o [GiaiDe]; man nay chi ve va nhan bam.
 *
 * BON TRANG THAI, cung mot man:
 *  - chua bat dau: the huong dan va nut Bat dau. Chua hien de: xem truoc de roi moi
 *    bam Bat dau thi dong ho khong con do duoc gi;
 *  - dang lam: dong ho goc tren, tung cau, nut Nop bai cuoi danh sach. Thoat ra van giu
 *    nguyen, dong ho van chay: vao lai lam tiep;
 *  - da nop trac nghiem: ket qua tung cau trac nghiem, va nut chup phan tu luan;
 *  - da co diem: diem ca de, tung cau dung hay chua.
 *
 * KHONG HIEN DAP AN, ke ca sau khi nop. Cau sai di vao dong "Sửa N câu" ngoai man chinh
 * nhu moi cau khac, va chu cai dung ma hien ra o day thi viec sua chi con la chep lai.
 */
class GiaiDeActivity : AppCompatActivity() {

    private lateinit var b: StActivityGiaiDeBinding
    private var de: DeGiai? = null
    private var cacCau: List<CauHoi> = emptyList()

    /** Bon nut chu cua tung cau trac nghiem, de to lai khi con bam ma khong ve lai ca man. */
    private val nutChu = mutableMapOf<String, List<MaterialButton>>()

    /** Ket qua tung cau tu luan sau khi cham, theo id cau. */
    private var ketTuLuan: Map<String, Boolean> = emptyMap()

    /** Ket qua phan trac nghiem vua nop, chi de noi so phut o the ket qua. */
    private var vuaNop: GiaiDe.KetQuaTracNghiem? = null

    private val tay = Handler(Looper.getMainLooper())
    private val nhip = object : Runnable {
        override fun run() {
            veDongHo()
            tay.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = StActivityGiaiDeBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
        b.nutQuayLai.setOnClickListener { finish() }
    }

    /** Doc lai de moi lan vao: vua chup phan tu luan xong, hay may vua cham xong. */
    override fun onResume() {
        super.onResume()
        val id = intent.getStringExtra(EXTRA_DE) ?: return finish()
        lifecycleScope.launch {
            val nap = withContext(Dispatchers.IO) {
                val d = GiaiDe.theoId(this@GiaiDeActivity, id) ?: return@withContext null
                Triple(d, GiaiDe.cacCau(this@GiaiDeActivity, d), GiaiDe.ketQuaTuLuan(this@GiaiDeActivity, d))
            }
            if (nap == null) return@launch finish()
            de = nap.first
            cacCau = nap.second
            ketTuLuan = nap.third
            ve()
            tay.removeCallbacks(nhip)
            tay.post(nhip)
        }
    }

    override fun onPause() {
        tay.removeCallbacks(nhip)
        super.onPause()
    }

    // ------------------------------------------------------------------- ve

    private fun ve() {
        val d = de ?: return
        b.tieuDe.text = GiaiDe.tenDe(d)
        b.phuDe.text = "${d.ten} · ${cacCau.size} câu · khoảng ${d.phutGoiY} phút" +
            GiaiDe.ngayKiemTra(d)
        b.danhSach.removeAllViews()
        nutChu.clear()
        if (!d.daBatDau) return veHuongDan(d)

        cacCau.forEachIndexed { i, c -> theCau(i + 1, c, d) }
        if (!d.daNop) veNutNop() else veKetQua(d)
    }

    private fun veHuongDan(d: DeGiai) {
        val ten = getString(R.string.child_name)
        val tn = cacCau.count { it.bamTrenMay }
        val tl = cacCau.size - tn
        val khung = theTrang()
        khung.addView(chu("Làm một mạch, như giờ kiểm tra ở lớp.", 18f, dam = true))
        val cac = buildList {
            if (tn > 0) add("$tn câu trắc nghiệm: bấm chữ ngay trên máy.")
            if (tl > 0) add("$tl câu tự luận: làm ra vở, ghi rõ số câu.")
            add(
                if (tl > 0) "Xong bấm Nộp bài. Máy chấm trắc nghiệm ngay, rồi $ten chụp phần tự luận."
                else "Xong bấm Nộp bài, máy chấm ngay."
            )
            add("Gợi ý khoảng ${d.phutGoiY} phút. Quá giờ vẫn nộp được.")
            add("Giờ chơi tính như bài làm thêm.")
        }
        cac.forEach { khung.addView(chu("• $it", 16f).apply { dem(top = 8) }) }
        khung.addView(nut("Bắt đầu") { batDau() }.apply { dem(top = 18) })
        b.danhSach.addView(khung)
    }

    private fun theCau(so: Int, c: CauHoi, d: DeGiai) {
        val v = StTheCauDeBinding.inflate(LayoutInflater.from(this), b.danhSach, false)
        v.nhan.text = "Câu $so · " + (if (c.bamTrenMay) "Trắc nghiệm" else "Làm ra vở") +
            " · ${c.ma}"
        v.de.text = SoMu.hien(if (c.bamTrenMay) tachPhuongAn(c.de) else c.de)
        if (c.bamTrenMay) {
            v.hangChu.visibility = View.VISIBLE
            val cacNut = CHU.map { chu ->
                MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = chu
                    textSize = 18f
                    minimumWidth = 0
                    minWidth = 0
                    insetTop = 0
                    insetBottom = 0
                    cornerRadius = 14.dp()
                    layoutParams = LinearLayout.LayoutParams(60.dp(), 52.dp()).apply { marginEnd = 12.dp() }
                    isEnabled = !d.daNop
                    setOnClickListener { chon(c.id, chu) }
                    v.hangChu.addView(this)
                }
            }
            nutChu[c.id] = cacNut
            toNut(c.id)
        }
        ketCau(c, d)?.let { (noi, mauKet) ->
            v.ket.visibility = View.VISIBLE
            v.ket.text = noi
            v.ket.setTextColor(mau(mauKet))
        }
        b.danhSach.addView(v.root)
    }

    /**
     * De trac nghiem viet lien mot dong ("... là A. 10 g. B. 3 g. C. 0,9 g. D. 0,1 g."):
     * dua moi phuong an xuong mot dong cho con doc. Lay lan xuat hien CUOI cua "A. ", de
     * ten diem trong de ("tam giác A. ...") khong cat nham. Khong tach duoc thi giu nguyen.
     */
    private fun tachPhuongAn(de: String): String {
        val m = PHUONG_AN.find(de) ?: return de
        val (hoi, a, b, c, d) = m.destructured
        return "$hoi\n\nA. $a\nB. $b\nC. $c\nD. $d"
    }

    /** Dong ket qua duoi mot cau sau khi nop. Khong noi dap an dung. */
    private fun ketCau(c: CauHoi, d: DeGiai): Pair<String, Int>? {
        if (!d.daNop) return null
        if (c.bamTrenMay) {
            val chon = d.chon[c.id] ?: return "Chưa chọn" to R.color.alert
            return if (chon == c.dapAn) "Chọn $chon · đúng" to R.color.ok
            else "Chọn $chon · chưa đúng" to R.color.alert
        }
        return when (ketTuLuan[c.id]) {
            true -> "Đúng" to R.color.ok
            false -> "Chưa đúng" to R.color.alert
            null -> null
        }
    }

    /**
     * To lai bon nut cua mot cau theo chu dang chon. Nop roi thi chu da chon doi sang
     * xanh hay do theo dung sai, ba chu kia mo di: nhin la biet khong bam duoc nua, va
     * khong chu nao lo ra dap an dung.
     */
    private fun toNut(cauId: String) {
        val d = de ?: return
        val dangChon = d.chon[cauId]
        val dapAn = cacCau.firstOrNull { it.id == cauId }?.dapAn
        nutChu[cauId]?.forEachIndexed { i, n ->
            val chon = CHU[i] == dangChon
            val mauChinh = mau(
                when {
                    !d.daNop || !chon -> R.color.brand
                    dangChon == dapAn -> R.color.ok
                    else -> R.color.alert
                }
            )
            n.backgroundTintList = ColorStateList.valueOf(if (chon) mauChinh else mau(R.color.surface))
            n.setTextColor(if (chon) mau(R.color.surface) else mauChinh)
            n.strokeColor = ColorStateList.valueOf(mauChinh)
            n.strokeWidth = 2.dp()
            n.alpha = if (d.daNop && !chon) 0.35f else 1f
        }
    }

    private fun veNutNop() {
        val khung = theTrang()
        val tn = cacCau.filter { it.bamTrenMay }
        val tl = cacCau.size - tn.size
        khung.addView(
            chu(
                if (tl > 0) "Làm xong cả phần tự luận trong vở rồi mới bấm Nộp bài."
                else "Xem lại rồi bấm Nộp bài.",
                15f, mauChu = R.color.ink_soft
            )
        )
        khung.addView(nut("Nộp bài") { hoiNop() }.apply { dem(top = 14) })
        b.danhSach.addView(khung)
    }

    private fun veKetQua(d: DeGiai) {
        val khung = theTrang()
        val tn = cacCau.filter { it.bamTrenMay }
        val tl = cacCau.filterNot { it.bamTrenMay }
        val ten = getString(R.string.child_name)
        if (GiaiDe.daCoDiem(this, d)) {
            val (dung, tong) = GiaiDe.diem(this, d)
            khung.addView(chu("Đúng $dung/$tong câu", 22f, dam = true))
            val sai = tong - dung
            if (sai > 0) {
                khung.addView(
                    chu(
                        "Câu chưa đúng nằm ở dòng Sửa bài ngoài màn chính. Sửa xong vẫn được " +
                            "cộng giờ, chỉ điểm của đề giữ nguyên.",
                        15f, mauChu = R.color.ink_soft
                    ).apply { dem(top = 6) }
                )
            }
        } else if (tn.isNotEmpty()) {
            khung.addView(chu("Trắc nghiệm: đúng ${d.tnDung.coerceAtLeast(0)}/${tn.size} câu", 20f, dam = true))
        }
        vuaNop?.let { kq ->
            val dongPhut = when {
                kq.phut > 0 -> "Trắc nghiệm được ${kq.phut} phút."
                kq.khongCapDuoc ->
                    "Lúc này máy chưa cộng được giờ. Các câu trắc nghiệm đúng để dành, ngày khác làm lại vẫn được tính."
                else -> ""
            }
            if (dongPhut.isNotEmpty()) khung.addView(chu(dongPhut, 15f).apply { dem(top = 6) })
        }
        if (tl.isNotEmpty() && !d.daGuiTuLuan) {
            khung.addView(
                chu("Còn phần tự luận: $ten chụp các câu đã làm trong vở.", 16f).apply { dem(top = 10) }
            )
            khung.addView(nut("Chụp phần tự luận") { chupTuLuan() }.apply { dem(top = 14) })
        } else if (tl.isNotEmpty() && d.tlDung < 0) {
            khung.addView(
                chu("Đã gửi phần tự luận. Chấm xong máy báo điểm ở đây.", 15f, mauChu = R.color.ink_soft)
                    .apply { dem(top = 10) }
            )
        }
        b.danhSach.addView(khung)
    }

    /** Dong ho goc tren: con lai bao nhieu, hay da qua bao nhieu phut. */
    private fun veDongHo() {
        val d = de ?: return
        if (!d.daBatDau) {
            b.dongHo.visibility = View.GONE
            return
        }
        b.dongHo.visibility = View.VISIBLE
        val den = if (d.daNop) d.nopLuc else System.currentTimeMillis()
        val daLam = ((den - d.batDau) / 1000L).coerceAtLeast(0L)
        val con = d.phutGoiY * 60L - daLam
        val quaGio = con < 0
        b.dongHo.text = when {
            d.daNop -> "Làm ${daLam / 60} phút"
            quaGio -> "Quá ${(-con + 59) / 60} phút"
            else -> "Còn %d:%02d".format(con / 60, con % 60)
        }
        val mauChu = if (quaGio && !d.daNop) R.color.alert else R.color.brand_dark
        val mauNen = if (quaGio && !d.daNop) R.color.alert_soft else R.color.brand_soft
        b.dongHo.setTextColor(mau(mauChu))
        b.dongHo.backgroundTintList = ColorStateList.valueOf(mau(mauNen))
    }

    // ------------------------------------------------------------ viec lam

    private fun batDau() {
        val d = de ?: return
        de = GiaiDe.batDau(this, d)
        DayLog.add(this, "${getString(R.string.child_name)} bắt đầu ${GiaiDe.tenDe(d)} (${d.ten})")
        ve()
        veDongHo()
    }

    private fun chon(cauId: String, chu: String) {
        val d = de ?: return
        if (d.daNop) return
        de = GiaiDe.chon(this, d, cauId, chu)
        toNut(cauId)
    }

    private fun hoiNop() {
        val d = de ?: return
        val chuaChon = cacCau.count { it.bamTrenMay && d.chon[it.id] == null }
        if (chuaChon == 0) return nop()
        MaterialAlertDialogBuilder(this)
            .setTitle("Còn $chuaChon câu trắc nghiệm chưa chọn")
            .setMessage("Nộp luôn thì câu chưa chọn tính là chưa làm được.")
            .setPositiveButton("Nộp luôn") { _, _ -> nop() }
            .setNegativeButton("Làm tiếp", null)
            .show()
    }

    /**
     * Cham trac nghiem, cap gio, bao Ba Huy, roi mo camera cho phan tu luan neu de co.
     *
     * Cap gio y het man Kiem tra bai: dang choi thi cong vao phien, khong thi thanh mot
     * phieu cho con bam Bat dau. Tinh vao tran ngay, vi day la gio doi bang bai tap.
     */
    private fun nop() {
        val d = de ?: return
        val kq = GiaiDe.nopTracNghiem(this, d) { phut -> capGio(phut) }
        vuaNop = kq
        de = kq.de
        val ten = getString(R.string.child_name)
        val coTuLuan = cacCau.any { !it.bamTrenMay }
        if (kq.tong > 0) {
            DayLog.add(
                this,
                "${GiaiDe.tenDe(d)} (${d.ten}): trắc nghiệm đúng ${kq.dung}/${kq.tong}" +
                    if (kq.phut > 0) ", +${kq.phut} phút" else ""
            )
        }
        runCatching {
            Notifier.send(
                this,
                "$ten nộp ${GiaiDe.tenDe(d)} (${d.ten}" + GiaiDe.ngayKiemTra(d) + "): " +
                    (if (kq.tong > 0) "trắc nghiệm đúng ${kq.dung}/${kq.tong}" else "không có trắc nghiệm") +
                    (if (kq.phut > 0) ", được ${kq.phut} phút" else "") +
                    (if (coTuLuan) ". Phần tự luận ${ten} chụp gửi sau." else ".") +
                    " Làm ${((kq.de.nopLuc - kq.de.batDau) / 60_000L)} phút, gợi ý ${d.phutGoiY}."
            )
        }
        runCatching { DongBo.dayNgay() }
        ApprovalService.ensureRunning(this)
        ve()
        veDongHo()
        if (coTuLuan) chupTuLuan()
    }

    private fun capGio(phut: Int): Boolean {
        val gate = GateStore(this)
        return if (gate.state == GateState.ACTIVE) {
            gate.extend(phut, useQuota = true) != null
        } else {
            gate.approve(wantedMinutes = phut, useQuota = true, nhanCho = "Giải đề") != null
        }
    }

    /**
     * Mo camera cho phan tu luan, di duong cham nhu bai lam them. Pham vi mang ma de de
     * luc cham xong ghi diem vao de, va de man chup bo buoc vo dan do.
     */
    private fun chupTuLuan() {
        val d = de ?: return
        val tl = cacCau.filterNot { it.bamTrenMay }
        if (tl.isEmpty()) return
        val sach = NganHang.sachTheoNguon(d.nguon)
        val pham = PhamVi(
            mon = sach?.mon ?: d.mon,
            nguon = d.nguon,
            tenNguon = sach?.ten.orEmpty(),
            bai = "Giải đề (${d.ten})",
            cauIds = tl.map { it.id },
            giaiDe = d.id
        )
        startActivity(
            Intent(this, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_PHAM, pham.sangJson())
        )
    }

    // ------------------------------------------------------------------ ve vat

    private fun theTrang(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.st_nen_the)
        setPadding(18.dp(), 18.dp(), 18.dp(), 18.dp())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() }
    }

    private fun chu(noi: String, co: Float, dam: Boolean = false, mauChu: Int = R.color.ink): TextView =
        TextView(this).apply {
            text = noi
            textSize = co
            setTextColor(mau(mauChu))
            if (dam) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

    /** Nut vua co, nam ngay duoi phan chu no tac dong, khong keo het be ngang. */
    private fun nut(noi: String, bam: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            text = noi
            textSize = 17f
            cornerRadius = 28.dp()
            minHeight = 56.dp()
            setPadding(32.dp(), paddingTop, 32.dp(), paddingBottom)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { bam() }
        }

    private fun View.dem(top: Int) {
        (layoutParams as? LinearLayout.LayoutParams)?.topMargin = top.dp()
    }

    private fun mau(id: Int) = ContextCompat.getColor(this, id)

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_DE = "de_giai"

        private val CHU = listOf("A", "B", "C", "D")

        private val PHUONG_AN = Regex(
            """^(.*)\s+A\.\s+(.*?)\s+B\.\s+(.*?)\s+C\.\s+(.*?)\s+D\.\s+(.*)$""",
            RegexOption.DOT_MATCHES_ALL
        )

        fun mo(context: Context, deId: String) {
            context.startActivity(Intent(context, GiaiDeActivity::class.java).putExtra(EXTRA_DE, deId))
        }
    }
}
