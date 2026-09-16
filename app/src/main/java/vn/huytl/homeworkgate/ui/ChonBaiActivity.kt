package vn.huytl.homeworkgate.ui

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import vn.huytl.homeworkgate.databinding.StActivityChonBaiBinding
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.kho.TrangSach
import java.util.Calendar

/**
 * Con khai dang lam bai nao, truoc khi mo camera.
 *
 * VI SAO THEM MAN NAY. Truoc day con bam "Nop bai" roi chup thang, va may phai tu
 * doan xem tam anh do la bai gi - doan bang chinh AI dang cham. Hai ngay chay thu
 * cho thay cai gia cua viec do: cung mot bai lan nay may nhan ra la da lam roi,
 * lan sau khong nhan ra, vi giua hai lan no chep de bai moi lan mot khac. Ma "da
 * lam roi hay chua" la cot song cua ca app - no la thu duy nhat chan viec chup lai
 * bai hom qua de lay gio lan nua.
 *
 * Con khai truoc thi cau hoi do khong con phai doan: ma cau lay tu sach, co dinh,
 * nam san trong may tu luc cai app. May chi con lam dung viec no lam tot - nhin
 * bai va noi dung hay sai.
 *
 * BON BUOC: mon -> sach -> trang -> tich cac cau. Mon nao chua nap sach thi bo qua
 * ba buoc sau, di thang sang man chup nhu truoc gio - de mot ban ngan hang chua co
 * khong lam ket duong nop bai cua cac mon con lai.
 *
 * KHONG BAT KHAI THAT CHI TIET. Danh sach cau bay san ra de tich, khong co o nao
 * phai go. Mot dua tre dang muon di choi ma phai go tay "bai 2.26a" thi no se tim
 * duong ngan nhat, va duong ngan nhat cua no khong phai duong minh muon.
 */
class ChonBaiActivity : AppCompatActivity() {

    /** Bon buoc, dung thu tu con di qua. */
    private enum class Buoc { MON, SACH, TRANG, CAU }

    private lateinit var binding: StActivityChonBaiBinding

    private var buoc = Buoc.MON
    private var mon: String = ""
    private var sach: NganHang.Sach? = null

    /**
     * Cac trang con dang chon, theo dung thu tu con bam.
     *
     * Chon theo TRANG chu khong theo bai, vi co giao giao bai theo trang ("lam trang
     * 36, 37"). Cho chon nhieu trang mot luc: mot buoi co giao hay giao hai trang
     * lien nhau, ma bat con nop hai lan thi vua phien vua ton hai lan goi AI.
     */
    private val trangChon = linkedSetOf<Int>()

    private var cacCau: List<CauHoi> = emptyList()
    private var daXong: Set<String> = emptySet()
    private var canSua: Set<String> = emptySet()
    private val daTich = linkedSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StActivityChonBaiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }

        binding.nutQuayLai.setOnClickListener { lui() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = lui()
        })
        binding.nutChup.setOnClickListener {
            if (buoc == Buoc.TRANG) {
                buoc = Buoc.CAU
                veLai()
            } else {
                chup()
            }
        }

        veLai()
    }

    /**
     * Quay ve day sau khi chup xong thi dong luon.
     *
     * Man chup gui bai roi tu dong; neu man nay con nam lai thi con thay mot danh
     * sach cau da tich cua lan nop vua roi, tich tiep duoc, va nop lai chinh no.
     */
    override fun onResume() {
        super.onResume()
        if (daGui) finish()
    }

    private var daGui = false

    /** Lui mot buoc; dang o buoc dau thi thoat han. */
    private fun lui() {
        buoc = when (buoc) {
            Buoc.MON -> {
                finish()
                return
            }
            Buoc.SACH -> Buoc.MON
            Buoc.TRANG -> Buoc.SACH
            Buoc.CAU -> Buoc.TRANG
        }
        veLai()
    }

    private fun veLai() {
        binding.danhSach.removeAllViews()
        binding.dayNut.visibility = View.GONE
        binding.khungCuon.scrollTo(0, 0)
        when (buoc) {
            Buoc.MON -> veMon()
            Buoc.SACH -> veSach()
            Buoc.TRANG -> veTrang()
            Buoc.CAU -> veCau()
        }
    }

    // ------------------------------------------------------------------ buoc mon

    private fun veMon() {
        binding.tieuDe.text = "Con đang làm bài môn gì?"
        binding.phuDe.text = "Chọn môn rồi chọn bài, xong mới chụp."

        // Mon hoc hom nay len dau: phan lon bai ve nha la cua buoi hoc vua xong, nen
        // dat chung o tren la con khoi phai doc het danh sach.
        val homNay = ThoiKhoaBieu.monTrongNgay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        val conLai = ThoiKhoaBieu.tatCaMon().filterNot { it in homNay }

        if (homNay.isNotEmpty()) {
            themChuong("Học hôm nay")
            homNay.forEach { themMon(it) }
            themChuong("Môn khác")
        }
        conLai.forEach { themMon(it) }
    }

    private fun themMon(ten: String) {
        val coSach = NganHang.coSach(ten)
        themDong(
            ten = ten,
            phu = if (coSach) "Có sách trong máy — chọn được từng câu" else null
        ) {
            mon = ten
            if (coSach) {
                buoc = Buoc.SACH
                veLai()
            } else {
                // Mon chua nap sach: khong co gi de chon nua, di thang sang chup.
                chupTuDo()
            }
        }
    }

    // ----------------------------------------------------------------- buoc sach

    private fun veSach() {
        binding.tieuDe.text = "Bài $mon lấy ở đâu?"
        binding.phuDe.text = ""

        NganHang.sachCua(mon).forEach { s ->
            themDong(ten = s.ten, phu = "Chọn đúng trang, máy chấm chắc hơn") {
                sach = s
                trangChon.clear()
                buoc = Buoc.TRANG
                veLai()
            }
        }
        themDong(
            ten = "Bài khác",
            phu = "Vở bài tập, phiếu photo, đề cô cho riêng"
        ) { chupTuDo() }
    }

    // ---------------------------------------------------------------- buoc trang

    private fun veTrang() {
        val s = sach ?: return
        binding.tieuDe.text = "Cô giao trang nào?"
        binding.phuDe.text = s.ten

        lifecycleScope.launch {
            val cacTrang = withContext(Dispatchers.IO) {
                NganHang.cacTrang(this@ChonBaiActivity, s.nguon)
            }
            if (cacTrang.isEmpty()) {
                // Ngan hang rong (file chua nap, hay nap hong): dung chan con lai, cho
                // di duong tu do nhu cac mon khac.
                chupTuDo()
                return@launch
            }
            binding.danhSach.removeAllViews()
            binding.dayNut.visibility = View.VISIBLE
            cacTrang.forEach { t -> themOTrang(t) }
            capNhatNut()
        }
    }

    private fun themOTrang(t: TrangSach) {
        val o = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_cau, binding.danhSach, false) as MaterialCheckBox
        o.text = "Trang ${t.trang}  ·  ${t.soCau} câu\n${t.bai}"
        o.tag = t.trang
        o.isChecked = t.trang in trangChon
        o.setOnCheckedChangeListener { _, tick ->
            if (tick) trangChon.add(t.trang) else trangChon.remove(t.trang)
            capNhatNut()
        }
        binding.danhSach.addView(o)
    }

    // ------------------------------------------------------------------ buoc cau

    private fun veCau() {
        val s = sach ?: return
        if (trangChon.isEmpty()) return
        binding.tieuDe.text = "Con làm câu nào?"
        binding.phuDe.text = "${s.ten} · ${keTenTrang()}"
        daTich.clear()

        lifecycleScope.launch {
            val ct = this@ChonBaiActivity
            data class Nap(val cau: List<CauHoi>, val xong: Set<String>, val sua: Set<String>)
            val nap = withContext(Dispatchers.IO) {
                val cau = NganHang.cacCauTheoTrang(ct, s.nguon, trangChon.toList())
                val kho = KhoBai.get(ct)
                val han = System.currentTimeMillis() - 365L * 24 * 60 * 60_000L
                Nap(
                    cau = cau,
                    xong = kho.daXongTrong(cau.map { it.id }, han),
                    sua = SoCaiBai.dangChoSua(ct).map { it.khoa }.toSet()
                )
            }
            cacCau = nap.cau
            daXong = nap.xong
            canSua = nap.sua

            binding.danhSach.removeAllViews()
            binding.dayNut.visibility = View.VISIBLE

            val conLam = cacCau.filterNot { it.id in daXong }
            if (conLam.isNotEmpty()) {
                themDong(ten = "Chọn hết ${conLam.size} câu chưa tính giờ", phu = null) {
                    conLam.forEach { daTich.add(it.id) }
                    veLai()
                    // Ve lai xoa het o tich nen phai tich lai theo danh sach vua chon.
                    danhDauLaiOTich()
                }
            }
            cacCau.forEach { themCau(it) }
            capNhatNut()
        }
    }

    private fun themCau(cau: CauHoi) {
        val o = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_cau, binding.danhSach, false) as MaterialCheckBox
        val xong = cau.id in daXong
        val sua = cau.id in canSua

        o.text = buildString {
            append(cau.dongChon())
            if (xong) append("\n✓ đã tính giờ rồi")
            if (sua) append("\n● đang cần sửa lại")
        }
        o.tag = cau.id

        if (xong) {
            // Cau da tra gio thi khong tich duoc nua. Chan ngay o day chu khong de
            // no di den luc cham roi moi noi "cau nay tinh roi": con nhin mot cai la
            // biet con nhung cau nao phai lam, khong phai chup xong moi biet.
            o.isEnabled = false
            o.setTextColor(ContextCompat.getColor(this, R.color.ink_soft))
        } else {
            // Cau dang cho sua thi tich san: lan nop nay gan nhu chac chan la de sua
            // no, va bat con tich lai tung cau moi lan sua la mot viec thua.
            if (sua) {
                o.isChecked = true
                daTich.add(cau.id)
            }
            o.setOnCheckedChangeListener { _, tick ->
                if (tick) daTich.add(cau.id) else daTich.remove(cau.id)
                capNhatNut()
            }
        }
        binding.danhSach.addView(o)
    }

    /** Tich lai cac o theo [daTich] sau khi ve lai danh sach. */
    private fun danhDauLaiOTich() {
        for (i in 0 until binding.danhSach.childCount) {
            val v = binding.danhSach.getChildAt(i) as? MaterialCheckBox ?: continue
            val id = v.tag as? String ?: continue
            if (v.isEnabled) v.isChecked = id in daTich
        }
        capNhatNut()
    }

    private fun capNhatNut() {
        if (buoc == Buoc.TRANG) {
            binding.nutChup.isEnabled = trangChon.isNotEmpty()
            binding.nutChup.text = if (trangChon.isEmpty()) {
                "Chọn ít nhất một trang"
            } else {
                "Xem câu ở ${keTenTrang()}"
            }
            binding.demTich.text = "Cô giao trang nào thì tích trang đó."
            return
        }
        binding.nutChup.isEnabled = daTich.isNotEmpty()
        binding.nutChup.text = if (daTich.isEmpty()) {
            "Chọn ít nhất một câu"
        } else {
            "Chụp bài (${daTich.size} câu)"
        }
        binding.demTich.text = if (daTich.isEmpty()) {
            "Tích vào câu con vừa làm xong."
        } else {
            "Đã chọn ${daTich.size} câu."
        }
    }

    /** "trang 36" hay "trang 36, 37" - dung cho ca man hinh lan cau lenh gui AI. */
    private fun keTenTrang(): String =
        "trang " + trangChon.sorted().joinToString(", ")

    // ---------------------------------------------------------------------- chup

    /** Bai ngoai ngan hang: van chup va van cham, chi la khong co ma cau co dinh. */
    private fun chupTuDo() = moManChup(PhamVi(mon = mon))

    private fun chup() {
        val s = sach ?: return chupTuDo()
        if (trangChon.isEmpty()) return chupTuDo()
        moManChup(
            PhamVi(
                mon = mon,
                nguon = s.nguon,
                tenNguon = s.ten,
                bai = keTenTrang(),
                // Giu dung thu tu in trong sach, khong phai thu tu con bam.
                cauIds = cacCau.map { it.id }.filter { it in daTich }
            )
        )
    }

    private fun moManChup(pham: PhamVi) {
        daGui = true
        startActivity(
            Intent(this, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_PHAM, pham.sangJson())
        )
    }

    // --------------------------------------------------------------------- ve vat

    private fun themChuong(ten: String) {
        val t = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_chuong, binding.danhSach, false) as TextView
        t.text = ten
        binding.danhSach.addView(t)
    }

    private fun themDong(ten: String, phu: String?, bam: () -> Unit) {
        val v = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_chon, binding.danhSach, false) as LinearLayout
        v.findViewById<TextView>(R.id.ten).text = ten
        v.findViewById<TextView>(R.id.phu).apply {
            text = phu.orEmpty()
            visibility = if (phu.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        v.setOnClickListener { bam() }
        binding.danhSach.addView(v)
    }
}
