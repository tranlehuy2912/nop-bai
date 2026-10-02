package vn.huytl.homeworkgate.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.KhaiChoCham
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.databinding.StActivityChonBaiBinding
import vn.huytl.homeworkgate.databinding.StDongTrangBinding
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.kho.TenBai

/**
 * "Làm bài tập trong SGK": con chon bai, chon cau trong SGK roi lam ngay tren may.
 *
 * VI SAO CO (Ba Huy chot 2/10/2026). Truoc ngay do bai co giao trong SGK phai lam vo roi chup
 * ([ChonBaiActivity]), cho Ba Huy nho Claude cham. Le Hoa khong chiu viet, con bai lam tren may
 * thi may cham ngay. Nay bai SGK Toan, KHTN, Tieng Anh lam tren may. Man nay mo tu dong "Làm bài
 * tập trong SGK" o trang Luyen tap ([LuyenTapActivity]) va tu dong "Bài trong SGK" o man khai bai:
 * hai cua dan ve mot cho.
 *
 * BA BUOC: quyen (chi khi mon co hon mot quyen SGK, nhu Toan hai tap), chon bai (tich nhieu bai
 * mot luc), chon cau de lam. Ba Huy chot chon theo bai chu khong theo trang nhu man khai bai, va
 * bo chu "cô giao" o tieu de: "Chọn bài", "Chọn câu để làm". O vuong dau dong bai van ghi trang
 * dau cua bai, vi co giao hay noi theo trang.
 *
 * LAM VA NOP NHU GIAI DE (Ba Huy chon cach B): cac cau da chon thanh mot de [GiaiDe.LOAI_SGK] tren
 * [GiaiDeActivity]. Moi cau mot the co sao va nut Kiem tra, cuoi trang nut Nop bai, nop moi ghi so
 * va cong phut theo sao. Khong co dong ho. Mo duoc nhieu bai cung luc (Ba Huy chon): bai dang lam
 * do xep o khoi "Đang làm" dau man chon quyen va man chon bai.
 *
 * CAU PHAI VIET (ve hinh, chung minh dai: khong co ban phim ghep) van tich duoc. Chung nam trong
 * de, nop xong thi co nut chup ([MoChup]), va anh di Bang dieu khien nho Claude cham nhu moi bai
 * chup (Ba Huy chot). Con chi tich cau phai viet thi khong co gi lam tren may: nut thanh "Chụp
 * bài", mo thang camera. Cau SGK chua soan ban phim ghep cung la cau phai viet, nen man nay dung
 * duoc ca luc dang soan do.
 *
 * KHOA CAU nhu man khai bai: cau da lam dung, cau dang nam trong bai dang lam, cau dang cho cham,
 * cau dang can sua. Them mot luat cua bai lam tren may: cau vua lam sai het tren may thi 24 gio
 * sau moi chon lai duoc, nhu lam lai o Luyen tap. Khong co luat nay thi con xem loi giai xong mo
 * bai moi lam lai ngay, duoc tron sao.
 */
class BaiSgkActivity : AppCompatActivity() {

    private enum class Buoc { SACH, BAI, CAU }

    private lateinit var b: StActivityChonBaiBinding

    private var mon = ""
    private var cacSach: List<NganHang.Sach> = emptyList()
    private var buoc = Buoc.SACH
    private var sach: NganHang.Sach? = null

    /** Cac bai con dang tich, theo dung thu tu bam. Khi lay cau thi xep lai theo thu tu sach. */
    private val baiChon = linkedSetOf<String>()
    private var cacBai: List<TenBai> = emptyList()

    private var cacCau: List<CauHoi> = emptyList()

    /** Cau lam duoc tren may (co ban phim ghep). Cau con lai la cau phai viet. */
    private var trenMay: Set<String> = emptySet()

    /** Cau khong tich duoc, kem dong ly do in duoi de. */
    private var lyDoKhoa: Map<String, String> = emptyMap()
    private val daTich = linkedSetOf<String>()

    /** Con da di chup cau phai viet: quay lai man nay thi dong, nhu man khai bai. */
    private var daDiChup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = StActivityChonBaiBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
        mon = intent.getStringExtra(EXTRA_MON).orEmpty()
        cacSach = NganHang.sachGiaoKhoaCua(mon)
        if (cacSach.isEmpty()) return finish()
        if (cacSach.size == 1) {
            sach = cacSach.first()
            buoc = Buoc.BAI
        }
        b.nutQuayLai.setOnClickListener { lui() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = lui()
        })
        b.nutChup.setOnClickListener {
            when (buoc) {
                Buoc.BAI -> if (baiChon.isNotEmpty()) {
                    buoc = Buoc.CAU
                    veLai()
                }
                Buoc.CAU -> lamBai()
                Buoc.SACH -> Unit
            }
        }
        veLai()
    }

    override fun onResume() {
        super.onResume()
        if (daDiChup) finish()
    }

    private fun lui() {
        buoc = when (buoc) {
            Buoc.SACH -> return finish()
            Buoc.BAI -> if (cacSach.size > 1) Buoc.SACH else return finish()
            Buoc.CAU -> Buoc.BAI
        }
        veLai()
    }

    private fun veLai() {
        b.danhSach.removeAllViews()
        b.dayNut.visibility = View.GONE
        // post: danh sach moi chua do xong, cuon ngay bay gio khong an (xem ChonBaiActivity).
        b.khungCuon.post { b.khungCuon.scrollTo(0, 0) }
        when (buoc) {
            Buoc.SACH -> veSach()
            Buoc.BAI -> veBai()
            Buoc.CAU -> veCau()
        }
    }

    // ------------------------------------------------------------------ buoc sach

    private fun veSach() {
        b.tieuDe.text = "Bài SGK quyển nào?"
        b.phuDe.text = "Làm bài tập trong SGK · ${GiaiDe.tenMon(mon)}"
        lifecycleScope.launch {
            val ct = this@BaiSgkActivity
            val nap = withContext(Dispatchers.IO) {
                docDangLam(ct, null) to cacSach.map { NganHang.tienBo(ct, it.nguon) }
            }
            if (buoc != Buoc.SACH) return@launch
            veDangLam(nap.first)
            cacSach.zip(nap.second).forEach { (s, tienBo) ->
                val (xong, tong) = tienBo
                val dong = themDong(ten = s.ten, phu = "Đã làm $xong/$tong câu") {
                    sach = s
                    baiChon.clear()
                    buoc = Buoc.BAI
                    veLai()
                }
                dong.findViewById<LinearProgressIndicator>(R.id.thanh_lam).apply {
                    visibility = View.VISIBLE
                    max = tong.coerceAtLeast(1)
                    setProgressCompat(xong, false)
                }
            }
        }
    }

    // ------------------------------------------------------------------- buoc bai

    private fun veBai() {
        val s = sach ?: return
        b.tieuDe.text = "Chọn bài"
        b.phuDe.text = s.ten
        lifecycleScope.launch {
            val ct = this@BaiSgkActivity
            data class Nap(val dangLam: List<DangLam>, val bai: List<TenBai>, val daLam: Map<String, Int>)
            val nap = withContext(Dispatchers.IO) {
                val kho = KhoBai.get(ct)
                val tatCa = kho.cacCauCuaNguon(s.nguon)
                val xong = kho.daXongTrong(tatCa.map { it.id }, System.currentTimeMillis() - MOT_NAM)
                Nap(
                    dangLam = docDangLam(ct, s.nguon),
                    bai = kho.cacBaiCua(s.nguon),
                    daLam = tatCa.filter { it.id in xong }.groupingBy { it.bai }.eachCount()
                )
            }
            if (buoc != Buoc.BAI) return@launch
            cacBai = nap.bai
            veDangLam(nap.dangLam)
            var chuong = ""
            nap.bai.forEach { t ->
                if (t.chuong.isNotBlank() && t.chuong != chuong) {
                    themChuong(t.chuong)
                    chuong = t.chuong
                }
                themOBai(t, nap.daLam[t.bai] ?: 0)
            }
            b.dayNut.visibility = View.VISIBLE
            capNhatNut()
        }
    }

    private fun themOBai(t: TenBai, daLam: Int) {
        val v = StDongTrangBinding.inflate(layoutInflater, b.danhSach, false)
        v.soTrang.text = t.trang.toString()
        v.soTrang.setTextColor(ContextCompat.getColor(this, R.color.brand_dark))
        v.soTrang.backgroundTintList = ContextCompat.getColorStateList(this, R.color.brand_soft)
        v.tenBai.text = t.bai
        v.soCau.text = "${t.soCau} câu" + if (daLam > 0) " · đã làm $daLam" else ""
        v.oTich.isChecked = t.bai in baiChon
        v.root.setOnClickListener {
            val tick = !v.oTich.isChecked
            v.oTich.isChecked = tick
            if (tick) baiChon.add(t.bai) else baiChon.remove(t.bai)
            capNhatNut()
        }
        b.danhSach.addView(v.root)
    }

    // ------------------------------------------------------------------- buoc cau

    private fun veCau() {
        val s = sach ?: return
        if (baiChon.isEmpty()) return
        b.tieuDe.text = "Chọn câu để làm"
        b.phuDe.text = "${s.ten} · ${keBai()}"
        daTich.clear()
        lifecycleScope.launch {
            val ct = this@BaiSgkActivity
            data class Nap(val cau: List<CauHoi>, val trenMay: Set<String>, val khoa: Map<String, String>)
            val nap = withContext(Dispatchers.IO) {
                val kho = KhoBai.get(ct)
                val bayGio = System.currentTimeMillis()
                val cau = baiTheoSach().flatMap { kho.cacCauCua(s.nguon, it) }
                val muc = cau.associate { it.id to LamTrenMay.muc(ct, it) }
                val ids = cau.map { it.id }
                val xong = kho.daXongTrong(ids, bayGio - MOT_NAM)
                val dangLam = GiaiDe.baiSgkDangLam(ct, mon, bayGio = bayGio).flatMap { it.cauIds }.toSet()
                val cho = KhaiChoCham.cauChoCham(ct)
                val sua = SoCaiBai.canSua(ct, bayGio).map { it.khoa }.toSet()
                val khoa = cau.mapNotNull { c ->
                    val m = muc[c.id]
                    val lyDo = when {
                        c.id in xong -> "✓ đã làm rồi"
                        c.id in dangLam -> "… đang nằm trong một bài đang làm"
                        c.id in cho -> "… đang chờ chấm"
                        c.id in sua -> "● đang cần sửa, nộp lại ở màn Kết quả"
                        // Vua lam sai het tren may, hay nop bai luc cau con chua xong (tinh 0 sao):
                        // 24 gio sau moi lam lai, nhu o Luyen tap.
                        m != null && m.tinhTrang.vong >= 0 &&
                            !LuatGhep.moLamLai(m.tinhTrang, m.ghep.sao, bayGio) -> "… lần trước chưa được, 24 giờ sau làm lại được"
                        else -> null
                    }
                    lyDo?.let { c.id to it }
                }.toMap()
                Nap(cau, muc.filterValues { it != null }.keys, khoa)
            }
            if (buoc != Buoc.CAU) return@launch
            cacCau = nap.cau
            trenMay = nap.trenMay
            lyDoKhoa = nap.khoa
            b.danhSach.removeAllViews()
            b.dayNut.visibility = View.VISIBLE
            val conLam = cacCau.filter { it.id !in lyDoKhoa }
            if (conLam.isNotEmpty()) {
                themDong(ten = "Chọn hết ${conLam.size} câu chưa làm", phu = null) {
                    // Tich thang len cac o, khong ve lai danh sach: ve lai la xoa het tich.
                    conLam.forEach { daTich.add(it.id) }
                    danhDauLaiOTich()
                }
            }
            cacCau.forEach { themCau(it) }
            capNhatNut()
        }
    }

    private fun themCau(cau: CauHoi) {
        val dong = LayoutInflater.from(this).inflate(R.layout.st_dong_cau_chep, b.danhSach, false)
        val o = dong.findViewById<MaterialCheckBox>(R.id.o_cau)
        val lyDo = lyDoKhoa[cau.id]
        o.text = MoChup.dongCau(cau) {
            if (cau.id !in trenMay) append("\n✎ phải viết, chụp ảnh sau khi nộp")
            if (lyDo != null) append("\n$lyDo")
        }
        o.tag = cau.id
        dong.findViewById<View>(R.id.nut_chep).setOnClickListener {
            Chep.vao(this, SoMu.hienDe(cau.dongChon(), cau.mon))
        }
        if (lyDo != null) {
            o.isEnabled = false
            o.setTextColor(ContextCompat.getColor(this, R.color.ink_soft))
        } else {
            o.setOnCheckedChangeListener { _, tick ->
                if (tick) daTich.add(cau.id) else daTich.remove(cau.id)
                capNhatNut()
            }
        }
        b.danhSach.addView(dong)
    }

    /** Tich lai cac o theo [daTich], sau khi bam "Chọn hết". */
    private fun danhDauLaiOTich() {
        for (i in 0 until b.danhSach.childCount) {
            val v = b.danhSach.getChildAt(i).findViewById<MaterialCheckBox>(R.id.o_cau) ?: continue
            val id = v.tag as? String ?: continue
            if (v.isEnabled) v.isChecked = id in daTich
        }
        capNhatNut()
    }

    private fun capNhatNut() {
        when (buoc) {
            Buoc.BAI -> {
                b.nutChup.isEnabled = baiChon.isNotEmpty()
                b.nutChup.text = when (baiChon.size) {
                    0 -> "Chọn ít nhất một bài"
                    1 -> "Chọn câu ở ${tenNgan(baiChon.first())}"
                    else -> "Chọn câu ở ${baiChon.size} bài"
                }
                b.demTich.text = "Chọn được nhiều bài một lúc."
            }
            Buoc.CAU -> {
                val may = daTich.count { it in trenMay }
                val viet = daTich.size - may
                b.nutChup.isEnabled = daTich.isNotEmpty()
                b.nutChup.text = when {
                    daTich.isEmpty() -> "Chọn ít nhất một câu"
                    may > 0 -> "Làm bài (${daTich.size} câu)"
                    else -> "Chụp bài ($viet câu)"
                }
                b.demTich.text = when {
                    daTich.isEmpty() -> "Câu có bàn phím làm ngay trên máy, câu phải viết chụp sau khi nộp."
                    may > 0 && viet > 0 -> "Đã chọn ${daTich.size} câu, $viet câu phải viết chụp sau khi nộp."
                    else -> "Đã chọn ${daTich.size} câu."
                }
            }
            Buoc.SACH -> Unit
        }
    }

    // --------------------------------------------------------------------- lam bai

    /**
     * Co cau lam tren may: dung de roi mo man lam bai, dong man nay. Chi co cau phai viet: mo
     * thang camera, cung duong voi man khai bai.
     */
    private fun lamBai() {
        val s = sach ?: return
        // Giu dung thu tu in trong sach, khong phai thu tu con bam.
        val ids = cacCau.map { it.id }.filter { it in daTich }
        if (ids.isEmpty()) return
        if (ids.none { it in trenMay }) {
            MoChup.mo(
                this,
                PhamVi(mon = s.mon, nguon = s.nguon, tenNguon = s.ten, bai = keBai(), cauIds = ids)
            ) { daDiChup = true }
            return
        }
        b.nutChup.isEnabled = false
        lifecycleScope.launch {
            val ct = this@BaiSgkActivity
            val de = withContext(Dispatchers.IO) { GiaiDe.taoBaiSgk(ct, s, baiTheoSach(), ids) }
            if (de == null) {
                capNhatNut()
                return@launch
            }
            GiaiDeActivity.mo(ct, de.id)
            finish()
        }
    }

    // ----------------------------------------------------------------- bai dang lam

    /** Mot bai SGK dang lam do: de, so cau lam tren may da xong tren tong, so cau phai viet. */
    private class DangLam(val de: DeGiai, val xong: Int, val tong: Int, val viet: Int)

    private fun docDangLam(ct: Context, nguon: String?): List<DangLam> =
        GiaiDe.baiSgkDangLam(ct, mon, nguon).map { de ->
            val viet = GiaiDe.cauPhaiViet(ct, de).map { it.id }.toSet()
            val may = de.cauIds.filter { it !in viet }
            val xong = may.count { id ->
                runCatching { org.json.JSONObject(de.chon[id].orEmpty()).optBoolean("x") }.getOrDefault(false)
            }
            DangLam(de, xong, may.size, viet.size)
        }

    /** Khoi "Đang làm" dau man: bam mot bai la lam tiep bai do. */
    private fun veDangLam(cac: List<DangLam>) {
        if (cac.isEmpty()) return
        themChuong("Đang làm")
        cac.forEach { d ->
            themDong(
                ten = d.de.ten,
                phu = "Đã làm ${d.xong}/${d.tong} câu" + if (d.viet > 0) " · ${d.viet} câu phải viết" else ""
            ) {
                GiaiDeActivity.mo(this, d.de.id)
                finish()
            }
        }
    }

    // ---------------------------------------------------------------------- ve vat

    /** Cac bai con chon, xep theo thu tu trong sach. */
    private fun baiTheoSach(): List<String> {
        val thuTu = cacBai.map { it.bai }
        return baiChon.sortedBy { thuTu.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    /** "Bài 2, Bài 3" cho dong phu; bai khong mang so thi giu ca ten. */
    private fun keBai(): String = baiTheoSach().joinToString(", ") { tenNgan(it) }

    /** "Bài 2. Đa thức" ra "Bài 2"; "Luyện tập chung (trang 17)" giu nguyen. */
    private fun tenNgan(bai: String): String =
        PhanHoc.soBai(bai)?.let { bai.substringBefore(".").trim() } ?: bai

    private fun themChuong(ten: String) {
        val t = LayoutInflater.from(this).inflate(R.layout.st_dong_chuong, b.danhSach, false) as TextView
        t.text = ten
        b.danhSach.addView(t)
    }

    /** Mot dong bam duoc, cung kieu voi man khai bai. Tra ve dong vua them. */
    private fun themDong(ten: String, phu: String?, bam: () -> Unit): LinearLayout {
        val v = LayoutInflater.from(this).inflate(R.layout.st_dong_chon, b.danhSach, false) as LinearLayout
        v.findViewById<TextView>(R.id.ten).text = ten
        v.findViewById<TextView>(R.id.phu).apply {
            text = phu.orEmpty()
            visibility = if (phu.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        v.findViewById<TextView>(R.id.huy_hieu).visibility = View.GONE
        v.setOnClickListener { bam() }
        b.danhSach.addView(v)
        return v
    }

    companion object {
        private const val EXTRA_MON = "mon"
        private const val MOT_NAM = 365L * 24 * 60 * 60_000L

        fun mo(context: Context, mon: String) {
            context.startActivity(Intent(context, BaiSgkActivity::class.java).putExtra(EXTRA_MON, mon))
        }
    }
}
