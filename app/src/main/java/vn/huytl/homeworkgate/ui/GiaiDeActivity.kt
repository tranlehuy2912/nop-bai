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
import vn.huytl.homeworkgate.data.DongHoDe
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.databinding.StActivityGiaiDeBinding
import vn.huytl.homeworkgate.databinding.StTheCauDeBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.DeThi
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier

/**
 * Man Giai de: mot de may ra tu sach bai tap, hay mot de thi in san ([DeThi], tu 30/9/2026),
 * lam mot mach co dong ho. Luat ra de va cham de o [GiaiDe]; man nay chi ve va nhan bam. De
 * thi di theo khung cua to de: ten phan, loi dan, doan van in mot lan o dau phan, cau mang
 * nhan in tren de ("Câu 36", "Bài 3a" - [DeThi.Muc.nhan], tu 1/10/2026).
 *
 * HINH AN (1/10/2026, luat 4 Ba Huy chot). Bai hinh cua de Toan, KHTN ma to de khong in hinh
 * thi the dau bai co dong nhac con tu ve ra giay nhap va nut "Nhờ trợ giúp": lan mot hien hinh
 * goi y, lan hai (sau hop canh bao) hinh day du. Mot khoi cho ca bai, cac y a, b, c khong lap
 * lai; cau trac nghiem khong co loi dan chung thi khoi nam ngay trong the cau. Ve o
 * [KhungGhep.veHinhAn]; muc da mo nam o [nhoHinh] cho toi khi con roi man.
 *
 * BON TRANG THAI, cung mot man:
 *  - chua bat dau: the huong dan va nut Bat dau. Chua hien de: xem truoc de roi moi
 *    bam Bat dau thi dong ho khong con do duoc gi;
 *  - dang lam: dong ho goc tren, tung cau, nut Nop bai cuoi danh sach. Thoat ra van giu
 *    nguyen, vao lai lam tiep. Dong ho thi dung luc thoat ra (tat man hinh, sang app khac,
 *    bam quay lai), vao lai chay tiep: Ba Huy chon ngay 1/10/2026, xem [DongHoDe]. Truoc do
 *    dong ho chay theo gio that, ke ca luc con da di cho khac;
 *  - da nop trac nghiem: ket qua tung cau trac nghiem, va nut chup phan tu luan;
 *  - da co diem: diem ca de, tung cau dung hay chua.
 *
 * KHONG HIEN DAP AN, ke ca sau khi nop. Cau sai di vao dong "Sửa N câu" ngoai man chinh
 * nhu moi cau khac, va chu cai dung ma hien ra o day thi viec sua chi con la chep lai.
 *
 * BAI TAP SGK ([GiaiDe.LOAI_SGK], 2/10/2026): cung man nay, bo the huong dan, bo dong ho, khong
 * nhan tin Telegram luc nop (Ba Huy chi doc nhat ky). Cau phai viet hien mot the khong co ban
 * phim; nop xong thi the cuoi co nut chup cac cau do ([MoChup]), anh di Bang dieu khien nho
 * Claude cham nhu moi bai chup. Gui roi thi nut thanh dong "đã gửi".
 */
class GiaiDeActivity : AppCompatActivity() {

    private lateinit var b: StActivityGiaiDeBinding
    private var de: DeGiai? = null
    private var cacCau: List<CauHoi> = emptyList()

    /** Khung cua de thi in san (so cau in, ten phan, loi dan). Null voi de may ra tu SBT. */
    private var khungDe: DeThi.De? = null

    /** Bon nut chu cua tung cau trac nghiem, de to lai khi con bam ma khong ve lai ca man. */
    private val nutChu = mutableMapOf<String, List<MaterialButton>>()

    /** Ket qua tung cau tu luan sau khi cham, theo id cau. */
    private var ketTuLuan: Map<String, Boolean> = emptyMap()

    /** Ket qua phan trac nghiem vua nop, chi de noi so phut o the ket qua. */
    private var vuaNop: GiaiDe.KetQuaTracNghiem? = null

    /** Ket qua de lam tren may vua nop, de hien "+N" o the cuoi. */
    private var vuaNopMay: GiaiDe.KetQuaNop? = null

    /** Bai tap SGK: id cac cau phai viet, va cac cau phai viet chua gui anh (doc moi lan vao). */
    private var phaiViet: Set<String> = emptySet()
    private var vietChuaGui: List<CauHoi> = emptyList()

    private val laSgk: Boolean get() = de?.loai == GiaiDe.LOAI_SGK

    /**
     * Muc hinh an cua tung bai (1/10/2026), khoa theo ma de va bai ([khoaHinh]). Giu o day thi
     * hinh da mo con mo qua moi lan ve lai trong onResume, va cat vao Bundle khi Activity bi
     * dung lai; con roi man (Activity moi) thi hinh an lai. Xem [KhungGhep.NhoHinhAn].
     */
    private val nhoHinh = KhungGhep.NhoHinhAn()

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
        nhoHinh.doc(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nhoHinh.luu(outState)
    }

    /** Doc lai de moi lan vao: vua chup phan tu luan xong, hay may vua cham xong. */
    override fun onResume() {
        super.onResume()
        val id = intent.getStringExtra(EXTRA_DE) ?: return finish()
        lifecycleScope.launch {
            val ct = this@GiaiDeActivity
            val nap = withContext(Dispatchers.IO) {
                val d = GiaiDe.theoId(ct, id) ?: return@withContext null
                khungDe = GiaiDe.maDeThi(d)?.let { DeThi.theoMa(ct, it) }
                if (d.loai == GiaiDe.LOAI_SGK) {
                    // Doc lai moi lan vao: vua chup xong cau phai viet thi nut chup thanh "đã gửi".
                    phaiViet = GiaiDe.cauPhaiViet(ct, d).map { it.id }.toSet()
                    vietChuaGui = GiaiDe.cauPhaiVietChuaGui(ct, d)
                }
                Triple(d, GiaiDe.cacCau(ct, d), d.ketTuLuan)
            }
            if (nap == null) return@launch finish()
            de = nap.first
            cacCau = nap.second
            ketTuLuan = nap.third
            // Doc xong co the ve sau onPause (con vua bam ra ngoai): luc do dung chay dong
            // ho, khong thi nhip mot giay tu dat lai mai va giu man da dong trong bo nho. Dong
            // ho cua de (DongHoDe) cung vay: vao() luc man da khuat thi khong con ai goi roi().
            // Bai tap SGK khong co dong ho.
            val dangHien = lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
            if (dangHien && !laSgk) DongHoDe.vao(ct, nap.first)
            ve()
            tay.removeCallbacks(nhip)
            if (dangHien) tay.post(nhip)
        }
    }

    override fun onPause() {
        tay.removeCallbacks(nhip)
        // Man de khuat: tat man hinh, sang app khac, quay ra man chinh. Dong ho dung o day.
        de?.takeIf { it.loai != GiaiDe.LOAI_SGK }?.let { DongHoDe.roi(this, it) }
        super.onPause()
    }

    override fun onDestroy() {
        tay.removeCallbacks(nhip)
        super.onDestroy()
    }

    // ------------------------------------------------------------------- ve

    private fun ve() {
        val d = de ?: return
        b.tieuDe.text = GiaiDe.tenDe(d)
        // Bai tap SGK khong co gio goi y: "SGK Toán 8 tập một · Bài 2 · 3 câu".
        b.phuDe.text = if (laSgk) "${d.ten} · ${cacCau.size} câu"
        else "${d.ten} · ${cacCau.size} câu · khoảng ${d.phutGoiY} phút" + GiaiDe.ngayKiemTra(d)
        b.danhSach.removeAllViews()
        nutChu.clear()
        if (d.trenMay) return veTrenMay(d)
        if (!d.daBatDau) return veHuongDan(d)

        cacCau.forEachIndexed { i, c -> theCau(i + 1, c, d) }
        if (!d.daNop) veNutNop() else veKetQua(d)
    }

    // ------------------------------------------------------- de lam tren may

    /**
     * De tao tu 29/9/2026: moi cau mot the co sao, khung ghep va nut Kiem tra rieng. Sai thi
     * mat sao ngay trong de, cung luat voi bai lam them ([vn.huytl.homeworkgate.data.LuatGhep]).
     * Khong co chu nao giai thich luat sao.
     */
    private fun veTrenMay(d: DeGiai) {
        val dt = khungDe
        if (!d.daBatDau) {
            val khung = theTrang()
            khung.addView(chu("Làm một mạch, như giờ kiểm tra ở lớp.", 18f, dam = true))
            val cacDong = if (dt != null) {
                listOf("${cacCau.size} câu, làm ngay trên máy, các phần giữ nguyên như đề in.") +
                    dongBo(dt.cacMuc) +
                    "Làm trong khoảng ${d.phutGoiY} phút như giờ thi. Quá giờ vẫn nộp được."
            } else listOf(
                "${cacCau.size} câu, làm ngay trên máy.",
                "Gợi ý khoảng ${d.phutGoiY} phút. Quá giờ vẫn nộp được."
            )
            cacDong.forEach { khung.addView(chu("• $it", 16f).apply { dem(top = 8) }) }
            khung.addView(nut("Bắt đầu") { batDau() }.apply { dem(top = 18) })
            b.danhSach.addView(khung)
            return
        }
        if (dt != null) veDeThi(d, dt) else cacCau.forEachIndexed { i, c ->
            val dau = "Câu ${i + 1} · ${c.nhan()}"
            if (c.id in phaiViet) theCauPhaiViet(dau, c) else theCauTrenMay(dau, c, d)
        }
        val khung = theTrang()
        if (!d.daNop) {
            khung.addView(nut("Nộp bài") { hoiNopTrenMay() })
        } else {
            val (dat, toiDa) = GiaiDe.diem(this, d)
            khung.addView(chu("$dat/$toiDa ★", 26f, dam = true))
            vuaNopMay?.let { kq ->
                // Phut cap da gom phan giu toi sang (gio ngu), xem [GiaiDe.KetQuaNop]. Truoc 2/10/2026
                // dong nay cong them phutGiu, nop de luc gio ngu thi so hien ra gap doi.
                val phut = kq.phutCap + kq.phutQuy
                // Vang nhu sao da duoc, cung mau "+N ★" cua tung cau ([HangSao.mauCong], 2/10/2026).
                if (phut > 0) khung.addView(chu("+$phut ★", 20f, dam = true).apply {
                    setTextColor(HangSao.mauCong(this@GiaiDeActivity, phut))
                    dem(top = 6)
                })
            }
            if (laSgk) veChupPhaiViet(khung, d)
        }
        b.danhSach.addView(khung)
    }

    /**
     * De thi in san: di theo khung cua de, dau moi phan mot the ghi ten phan, loi dan va doan
     * van (neu phan co doan), roi tung cau voi nhan in tren de. Phan bo (nghe) chi con mot the
     * noi la khong lam, de so cau nhay tu 30 len 36 con khong ngo ngac.
     *
     * Hinh an (1/10/2026): bai co loi dan thi khoi hinh an nam o the dau bai, ngay duoi loi dan,
     * mot khoi cho ca bai vi moi y cung mot hinh. Cau khong co loi dan chung (trac nghiem Toan,
     * KHTN, [DeThi.Muc.nhom] rong) thi khong co the dau bai nao cua rieng no: khoi nam trong the
     * cau, duoi chu cua cau. Lay muc cua y dau tien con lam tren may: y bo (vẽ hình) khong ve
     * the nao, bai bo het thi khong co gi de lam voi hinh do.
     */
    private fun veDeThi(d: DeGiai, dt: DeThi.De) {
        val theoId = cacCau.associateBy { it.id }
        val daVe = mutableSetOf<String>()
        var phanTruoc: String? = null
        var nhomTruoc: String? = null
        var doanTruoc = ""
        dt.cacMuc.forEachIndexed { i, m ->
            if (m.phan != phanTruoc || m.nhom != nhomTruoc) {
                val cungNhom = dt.cacMuc.drop(i).takeWhile { it.phan == m.phan && it.nhom == m.nhom }
                val doan = cungNhom.firstNotNullOfOrNull { theoId[it.cauId]?.doan?.takeIf { x -> x.isNotBlank() } }
                val hinhBai = if (m.nhom.isBlank()) null
                else cungNhom.firstOrNull { it.coHinhAn && it.boMay.isBlank() }
                theDauPhan(dt, if (m.phan != phanTruoc) m.phan else "", m.nhom, doan, dongBo(cungNhom), hinhBai)
                    ?.let { b.danhSach.addView(it) }
                phanTruoc = m.phan
                nhomTruoc = m.nhom
                doanTruoc = doan.orEmpty()
            }
            if (m.boMay.isNotBlank()) return@forEachIndexed
            val c = theoId[m.cauId] ?: return@forEachIndexed
            if (!daVe.add(c.id)) return@forEachIndexed
            val hienDoan = c.doan.isNotBlank() && c.doan != doanTruoc
            if (c.doan.isNotBlank()) doanTruoc = c.doan
            theCauTrenMay(
                m.nhan, c, d, hienDoan = hienDoan, boDau = c.nhom,
                hinhAn = if (m.nhom.isBlank() && m.coHinhAn) dt to m else null
            )
        }
    }

    /**
     * Khoa muc hinh an cua mot bai trong [nhoHinh]: ma de, phan, ten bai ("Bài 3" lay tu loi
     * dan, cau khong co loi dan thi nhan cua cau). Co phan vi de co the danh so lai tu dau o
     * phan tu luan, "Câu 1" cua hai phan la hai bai khac nhau.
     */
    private fun khoaHinh(dt: DeThi.De, m: DeThi.Muc): String = "${dt.ma}|${m.phan}|${DeThi.tenBai(m)}"

    /**
     * Cac dong noi cau khong lam tren may trong [cac], rong khi khong co cau nao bo. Phan nghe
     * giu chu cu theo so in: "Phần nghe không làm trên máy (câu 31–35)." Cau bo vi ly do khac
     * (de Toan, KHTN tu 1/10/2026: "vẽ hình", "vẽ biểu đồ") ghi theo nhan in tren de, gom cac
     * cau cung ly do: "Không làm trên máy: Bài 3d, Bài 4c (vẽ hình)." Truoc ngay do moi cau bo
     * deu bi goi la phan nghe.
     */
    private fun dongBo(cac: List<DeThi.Muc>): List<String> {
        val bo = cac.filter { it.boMay.isNotBlank() }
        val (nghe, khac) = bo.partition { it.boMay.equals(BO_NGHE, ignoreCase = true) }
        return buildList {
            val so = nghe.map { it.so }.filter { it > 0 }
            if (so.isNotEmpty()) {
                val cacCau = if (so.size == 1) "${so.first()}" else "${so.min()}–${so.max()}"
                add("Phần nghe không làm trên máy (câu $cacCau).")
            }
            if (khac.isNotEmpty()) {
                val theoLyDo = khac.groupBy { it.boMay }.entries.joinToString("; ") { (lyDo, cau) ->
                    cau.joinToString(", ") { it.nhan } + " ($lyDo)"
                }
                add("Không làm trên máy: $theoLyDo.")
            }
        }
    }

    /**
     * The dau mot phan cua de thi: ten phan (khi doi phan), loi dan, khoi hinh an cua bai, cau
     * bo, doan van. null khi khong co gi de in: de khong ghi ten phan ma cau lai khong co loi dan
     * chung (trac nghiem Toan, KHTN co the gap tu 1/10/2026; de Anh cau nao cung co loi dan nen
     * chua gap). Khong co dong kiem nay thi man hien mot the trong.
     *
     * @param hinhAn muc mang hinh an cua bai, null khi bai khong co hinh an.
     */
    private fun theDauPhan(
        dt: DeThi.De,
        phan: String,
        nhom: String,
        doan: String?,
        bo: List<String>,
        hinhAn: DeThi.Muc?
    ): LinearLayout? {
        if (phan.isBlank() && nhom.isBlank() && bo.isEmpty() && doan.isNullOrBlank()) return null
        val khung = theTrang()
        if (phan.isNotBlank()) khung.addView(chu(phan, 15f, dam = true, mauChu = R.color.brand_dark))
        if (nhom.isNotBlank()) {
            khung.addView(chu(KhungGhep.boThe(nhom), 17f, dam = true).apply { if (phan.isNotBlank()) dem(top = 6) })
            if (hinhAn != null) KhungGhep.veHinhAn(khung, dt, hinhAn, khoaHinh(dt, hinhAn), nhoHinh)
        }
        bo.forEach { dong ->
            khung.addView(chu(dong, 15f, mauChu = R.color.ink_soft).apply { dem(top = 6) })
        }
        if (!doan.isNullOrBlank()) {
            khung.addView(TextView(this).apply {
                text = KhungGhep.hien(doan)
                textSize = 16f
                setTextColor(mau(R.color.ink))
                setBackgroundResource(R.drawable.bg_ghi_chu)
                setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
                setLineSpacing(3f * resources.displayMetrics.density, 1f)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 10.dp() }
            })
        }
        return khung
    }

    /**
     * @param dauThe dong dau the: de thi ghi dung nhan in tren de ("Câu 36", "Bài 3a"), khong
     *   kem ma cau trong kho; de may ra tu SBT ghi "Câu 3 · 2.26a" (so trong de, nhan cua sach).
     * @param hienDoan, boDau xem [KhungGhep.veDe]: de thi da in loi dan va doan van o the dau phan.
     * @param hinhAn khoi hinh an ve ngay trong the, duoi chu cua cau: chi cau de thi khong co
     *   loi dan chung, xem [veDeThi].
     */
    private fun theCauTrenMay(
        dauThe: String,
        c: CauHoi,
        d: DeGiai,
        hienDoan: Boolean = true,
        boDau: String = "",
        hinhAn: Pair<DeThi.De, DeThi.Muc>? = null
    ) {
        val muc = LamTrenMay.muc(this, c) ?: return
        val the = theTrang()
        val dau = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        dau.addView(chu(dauThe, 14f, mauChu = R.color.ink_soft).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        var luot = GiaiDe.luotCua(d, c.id, muc.ghep.sao)
        val sao = TextView(this).apply {
            textSize = 22f
            setTextColor(mau(R.color.wait))
        }
        // Ba dang sao nhu man lam bai ([HangSao]). De da nop ma cau chua xong thi tinh la chua
        // lam duoc: xam het, dung de o rong nhu con dang lam do.
        fun veSao() {
            val hien = if (d.daNop && !luot.xong) luot.copy(xong = true, dung = false) else luot
            sao.text = HangSao.chu(this, hien)
        }
        veSao()
        dau.addView(sao)
        the.addView(dau)
        val deBai = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8.dp() }
        }
        KhungGhep.veDe(deBai, muc, hienDoan = hienDoan, boDau = boDau)
        hinhAn?.let { (dt, m) -> KhungGhep.veHinhAn(deBai, dt, m, khoaHinh(dt, m), nhoHinh) }
        the.addView(deBai)
        val o = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dp() }
        }
        the.addView(o)
        if (luot.xong || d.daNop) {
            // Da xong (hay da nop): hien cau tra loi da ghi, khong ve lai ban phim.
            val traLoi = GiaiDe.traLoiCua(d, c.id)
            if (traLoi.isNotBlank()) o.addView(chu(KhungGhep.boThe(traLoi), 17f))
            if (luot.hienLoiGiai) KhungGhep(o, muc, KhungGhep.hatDe(d.id, c.id)).khoa(true)
            b.danhSach.addView(the)
            return
        }
        val khung = KhungGhep(o, muc, KhungGhep.hatDe(d.id, c.id), luot.saiDaChon)
            .also { it.ve(LuatGhep.mucMo(luot)) }
        val bao = chu("", 15f, mauChu = R.color.ink_soft)
        // So sao vua duoc cua cau, hien ngay khi cau xong (Ba Huy chot 2/10/2026, cung luat voi man
        // lam bai). Phut van cong mot lan luc nop ca de, chi cong phan hon lan tot nhat cua cau.
        val cong = chu("", 20f, dam = true).apply { visibility = View.GONE }
        the.addView(nut("Kiểm tra") {
            val dd = de ?: return@nut
            if (dd.daNop || luot.xong) return@nut
            val soSai = khung.kiem()
            if (soSai == null) {
                bao.text = "Chưa làm xong câu này"
                return@nut
            }
            bao.text = ""
            luot = LuatGhep.kiem(luot, soSai).copy(saiDaChon = khung.saiDaChon())
            de = GiaiDe.luuLuot(this, dd, c.id, luot, khung.traLoi())
            veSao()
            if (luot.xong && luot.dung && luot.sao > 0) HangSao.nhip(sao)
            if (luot.xong) {
                khung.khoa(luot.hienLoiGiai)
                val n = LuatGhep.saoKhiXong(luot)
                cong.text = "+$n ★"
                cong.setTextColor(HangSao.mauCong(this, n))
                cong.visibility = View.VISIBLE
            } else {
                khung.ve(LuatGhep.mucMo(luot))
            }
        }.apply { dem(top = 12) })
        the.addView(bao)
        the.addView(cong)
        b.danhSach.addView(the)
    }

    /**
     * The cau phai viet cua bai tap SGK: de bai, khong co ban phim. Con lam ra vo; nop xong thi
     * chup o the cuoi ([veChupPhaiViet]).
     */
    private fun theCauPhaiViet(dauThe: String, c: CauHoi) {
        val the = theTrang()
        the.addView(chu(dauThe, 14f, mauChu = R.color.ink_soft))
        the.addView(chu(SoMu.hienDe(c.de, c.mon), 17f).apply { dem(top = 8) })
        the.addView(chu("Câu phải viết: làm ra vở, nộp bài xong thì chụp ảnh.", 15f, mauChu = R.color.wait).apply {
            dem(top = 10)
        })
        b.danhSach.addView(the)
    }

    /**
     * The cuoi cua bai tap SGK da nop: con cau phai viet chua gui thi co nut chup, gui roi thi mot
     * dong noi dang cho cham. Anh di dung duong chup cua man khai bai ([MoChup]), Ba Huy nho Claude
     * cham o Bang dieu khien (Ba Huy chot 2/10/2026).
     */
    private fun veChupPhaiViet(khung: LinearLayout, d: DeGiai) {
        if (phaiViet.isEmpty()) return
        if (vietChuaGui.isEmpty()) {
            khung.addView(chu("Đã gửi câu phải viết, chờ Ba Huy nhờ Claude chấm.", 15f, mauChu = R.color.ink_soft).apply {
                dem(top = 10)
            })
            return
        }
        khung.addView(chu("Còn ${vietChuaGui.size} câu phải viết", 16f, dam = true).apply { dem(top = 12) })
        khung.addView(nut("Chụp câu phải viết") {
            val sach = NganHang.sachTheoNguon(d.nguon)
            MoChup.mo(
                this,
                PhamVi(
                    mon = sach?.mon ?: d.mon,
                    nguon = d.nguon,
                    tenNguon = sach?.ten.orEmpty(),
                    bai = d.ten.substringAfter(" · ", d.ten),
                    cauIds = vietChuaGui.map { it.id }
                )
            )
        }.apply { dem(top = 8) })
    }

    private fun hoiNopTrenMay() {
        val d = de ?: return
        val chuaXong = cacCau.count { c ->
            val g = LamTrenMay.muc(this, c)?.ghep ?: return@count false
            !GiaiDe.luotCua(d, c.id, g.sao).xong
        }
        if (chuaXong == 0) return nopTrenMay()
        MaterialAlertDialogBuilder(this)
            .setTitle("Còn $chuaXong câu chưa xong")
            .setMessage("Nộp luôn thì câu chưa xong tính là chưa làm được.")
            .setPositiveButton("Nộp luôn") { _, _ -> nopTrenMay() }
            .setNegativeButton("Làm tiếp", null)
            .show()
    }

    private fun nopTrenMay() {
        val d = de ?: return
        lifecycleScope.launch {
            val ct = this@GiaiDeActivity
            val kq = withContext(Dispatchers.IO) {
                val kq = GiaiDe.nop(ct, d)
                if (kq.de.loai == GiaiDe.LOAI_SGK) vietChuaGui = GiaiDe.cauPhaiVietChuaGui(ct, kq.de)
                kq
            }
            vuaNopMay = kq
            de = kq.de
            val ten = getString(R.string.child_name)
            // Da gom phan giu toi sang, xem [GiaiDe.KetQuaNop]; truoc 2/10/2026 cong them phutGiu.
            val phut = kq.phutCap
            // Bai tap SGK khong nhan tin: Ba Huy chi doc nhat ky (chot 2/10/2026), [GiaiDe.nop] da ghi.
            if (!laSgk) runCatching {
                Notifier.send(
                    ct,
                    "$ten nộp ${GiaiDe.tenDe(d)} (${d.ten}" + GiaiDe.ngayKiemTra(d) + "): " +
                        "${kq.saoDat}/${kq.saoToiDa} sao" +
                        (if (phut > 0) ", được $phut phút" else "") +
                        (if (kq.phutQuy > 0) ", vào quỹ ${kq.phutQuy} phút" else "") +
                        ". Làm ${DongHoDe.daLamMs(ct, kq.de) / 60_000L} phút, gợi ý ${d.phutGoiY}."
                )
            }
            runCatching { DongBo.dayNgay() }
            ApprovalService.ensureRunning(this@GiaiDeActivity)
            ve()
            veDongHo()
        }
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
        v.de.text = SoMu.hienDe(if (c.bamTrenMay) tachPhuongAn(c.de) else c.de, c.mon)
        v.nutChep.root.setOnClickListener { Chep.vao(this, "${v.nhan.text}\n${v.de.text}") }
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
        if (tl.isNotEmpty() && !GiaiDe.daGuiThat(this, d)) {
            khung.addView(
                chu("Còn phần tự luận: $ten chụp các câu đã làm trong vở.", 16f).apply { dem(top = 10) }
            )
            khung.addView(nut("Chụp phần tự luận") { chupTuLuan() }.apply { dem(top = 14) })
        } else if (tl.isNotEmpty() && GiaiDe.baDuyetKhongCham(this, d)) {
            khung.addView(
                chu("Ba Huy đã duyệt phần tự luận, không chấm điểm. Điểm trên chỉ tính trắc nghiệm.",
                    15f, mauChu = R.color.ink_soft).apply { dem(top = 10) }
            )
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
        // Bai tap SGK khong co dong ho (Ba Huy chon cach B: nhu Giai de nhung khong tinh gio).
        if (!d.daBatDau || d.loai == GiaiDe.LOAI_SGK) {
            b.dongHo.visibility = View.GONE
            return
        }
        b.dongHo.visibility = View.VISIBLE
        // Chi tinh luc con ngoi o man nay, xem DongHoDe. Ham nay chi chay khi man dang hien.
        val daLam = DongHoDe.daLamMs(this, d, System.currentTimeMillis(), dangOMan = true) / 1000L
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
                    " Làm ${DongHoDe.daLamMs(this, kq.de) / 60_000L} phút, gợi ý ${d.phutGoiY}."
            )
        }
        runCatching { DongBo.dayNgay() }
        ApprovalService.ensureRunning(this)
        ve()
        veDongHo()
        if (coTuLuan) chupTuLuan()
    }

    private fun capGio(phut: Int): Boolean = GateStore(this).congGioHoc(phut, nhanCho = "Giải đề") != null

    /**
     * Mo camera cho phan tu luan, di duong cham nhu bai lam them. Pham vi mang ma de de
     * luc cham xong ghi diem vao de.
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

        /** Ly do bo cua phan nghe trong file de ("bo_may": "nghe"), xem [dongBo]. */
        private const val BO_NGHE = "nghe"

        private val PHUONG_AN = Regex(
            """^(.*)\s+A\.\s+(.*)\s+B\.\s+(.*)\s+C\.\s+(.*)\s+D\.\s+(.*)$""",
            RegexOption.DOT_MATCHES_ALL
        )

        /**
         * De trac nghiem viet lien mot dong ("... là A. 10 g. B. 3 g. C. 0,9 g. D. 0,1 g."):
         * dua moi phuong an xuong mot dong cho con doc. Moi moc "A. ", "B. ", "C. ", "D. " deu lay
         * lan xuat hien CUOI (ca bon nhom deu tham), de ten diem trong de ("tam giác A. ...") hay
         * trong mot phuong an (18.2 KHTN: "B. Vị trí C.") khong cat nham. Da so tren 282 cau
         * trac nghiem cua ba quyen SBT ngay 27/9/2026: chi 18.2 doi. Khong tach duoc thi giu nguyen.
         */
        internal fun tachPhuongAn(de: String): String {
            val m = PHUONG_AN.find(de) ?: return de
            val (hoi, a, b, c, d) = m.destructured
            return "$hoi\n\nA. $a\nB. $b\nC. $c\nD. $d"
        }

        fun mo(context: Context, deId: String) {
            context.startActivity(Intent(context, GiaiDeActivity::class.java).putExtra(EXTRA_DE, deId))
        }
    }
}
