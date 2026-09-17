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
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private enum class Buoc { MON, SACH, TRANG, CAU, ON_TAP, LAM_THEM }

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
            Buoc.ON_TAP -> Buoc.MON
            Buoc.LAM_THEM -> Buoc.SACH
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
            Buoc.ON_TAP -> veOnTap()
            Buoc.LAM_THEM -> veLamThem()
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

        // Cau tung sai ma da sua dung: moi con quay lai lam mot lan nua. Dat ngay dau
        // man hinh chu khong giau trong menu - no la viec dang lam nhat o day, va neu
        // phai di tim thi khong dua tre nao di tim.
        lifecycleScope.launch {
            val on = withContext(Dispatchers.IO) { SoCaiBai.cacCauDangOn(this@ChonBaiActivity) }
            if (on.isEmpty() || buoc != Buoc.MON) return@launch
            val dong = LayoutInflater.from(this@ChonBaiActivity)
                .inflate(R.layout.st_dong_chon, binding.danhSach, false) as LinearLayout
            dong.findViewById<TextView>(R.id.ten).text =
                "Ôn lại ${on.size} câu đến hẹn"
            dong.findViewById<TextView>(R.id.phu).apply {
                text = "Đến hẹn nhớ lại. Làm bằng bút đỏ, đúng thì được cộng thêm giờ chơi"
                visibility = View.VISIBLE
            }
            dong.setOnClickListener {
                buoc = Buoc.ON_TAP
                veLai()
            }
            binding.danhSach.addView(dong, 0)
        }

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

    /**
     * Mot mon co the co nhieu quyen: Toan 8 co hai tap.
     *
     * So cau da lam ghi ngay tren dong cua tung quyen chu khong gop lai mot con so:
     * "da lam 12/683" khong noi len gi, con "tap hai: 12/315" thi con biet minh dang
     * do dang o dau. So nay lay tu kho nen ve sau dong moi dien vao.
     */
    private fun veSach() {
        binding.tieuDe.text = "Bài $mon lấy ở đâu?"
        binding.phuDe.text = "Chọn đúng quyển và trang, máy chấm chắc hơn."

        val quyen = NganHang.sachCua(mon)
        quyen.forEach { s ->
            val dong = themDong(ten = s.ten, phu = "Chọn đúng trang, máy chấm chắc hơn") {
                sach = s
                trangChon.clear()
                buoc = Buoc.TRANG
                veLai()
            }
            lifecycleScope.launch {
                val (xong, tong) = withContext(Dispatchers.IO) {
                    NganHang.tienBo(this@ChonBaiActivity, s.nguon)
                }
                if (buoc != Buoc.SACH) return@launch
                dong.findViewById<TextView>(R.id.phu).text = "Đã làm $xong/$tong câu"
            }
        }
        themDong(
            ten = "Bài khác",
            phu = "Vở bài tập, phiếu photo, đề cô cho riêng"
        ) { chupTuDo() }

        // Duong lam them gop ca mon: con khong phai nho cau minh chua lam nam o tap
        // nao. Mot lan nop van chi mang ma cua mot quyen - xem chupLamThem.
        if (quyen.isEmpty()) return
        themDong(
            ten = "Làm thêm cho quen tay",
            phu = "Câu chưa làm, ưu tiên bài con vừa sai"
        ) {
            sach = null
            buoc = Buoc.LAM_THEM
            veLai()
        }
    }

    // ------------------------------------------------------------- buoc lam them

    /**
     * Cau chua lam, uu tien bai con vua sai.
     *
     * Sai mot cau roi chi sua dung moi cau do thi con vua du de qua, chua chac da
     * hieu. Lam them mot cau khac cung bai moi la cho biet. Day la thu ma ngan hang
     * cau hoi mo ra: truoc khi co no, app khong co cach nao biet con CHUA lam cau
     * nao - no chi biet nhung cau con da nop.
     */
    private fun veLamThem() {
        val quyen = NganHang.sachCua(mon)
        if (quyen.isEmpty()) return
        binding.tieuDe.text = "Làm thêm cho quen tay"
        binding.phuDe.text = if (quyen.size == 1) quyen.first().ten else "Sách $mon trong máy"
        daTich.clear()

        lifecycleScope.launch {
            val ct = this@ChonBaiActivity
            val cau = withContext(Dispatchers.IO) { NganHang.cauNenLamThemCuaMon(ct, mon) }
            if (cau.isEmpty()) {
                binding.danhSach.removeAllViews()
                themChuong("Con làm hết sách rồi")
                return@launch
            }
            cacCau = cau
            daXong = emptySet()
            canSua = emptySet()

            binding.danhSach.removeAllViews()
            binding.dayNut.visibility = View.VISIBLE
            var baiCu = ""
            var nguonCu = ""
            cau.forEach { c ->
                // Danh sach nay tron nhieu quyen: khong ghi ten quyen thi "Bài 1" cua
                // tap mot va "Bài 21" cua tap hai nam canh nhau ma khong biet la hai
                // quyen khac nhau - va con chi nop duoc mot quyen mot lan.
                if (quyen.size > 1 && c.nguon != nguonCu) {
                    themChuong(NganHang.sachTheoNguon(c.nguon)?.ten ?: c.nguon)
                    nguonCu = c.nguon
                    baiCu = ""
                }
                if (c.bai.isNotBlank() && c.bai != baiCu) {
                    themChuong("${c.bai} · trang ${c.trang}")
                    baiCu = c.bai
                }
                themCau(c)
            }
            capNhatNut()
        }
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
            veDanTrang(cacTrang)
            capNhatNut()
        }
    }

    /**
     * Ve danh sach trang thanh tung cum, khong do het mot lan.
     *
     * Toan 8 tap mot co 83 trang co bai tap. Dung het 83 dong mot luc thi luong giao
     * dien ban mot nhip thay ro - ma day dung la luc con vua bam vao, nen no se bam
     * them lan nua tuong may khong an. Ve hai muoi dong roi nhuong luong lai, cuon
     * den dau thi phan sau da ve xong den do.
     *
     * Chen ten chuong vao giua: tam muoi ba dong so trang lien tuc thi khong biet
     * dang o dau trong quyen, ma co giao thuong noi "chuong II trang 36".
     */
    private fun veDanTrang(cacTrang: List<TrangSach>, tu: Int = 0, chuongCu: String = "") {
        var chuong = chuongCu
        val den = minOf(tu + MOI_CUM, cacTrang.size)
        for (i in tu until den) {
            val t = cacTrang[i]
            if (t.chuong.isNotBlank() && t.chuong != chuong) {
                themChuong(t.chuong)
                chuong = t.chuong
            }
            themOTrang(t)
        }
        if (den < cacTrang.size) {
            binding.danhSach.post {
                // Con bam Quay lai giua chung thi bo do, dung ve tiep vao man khac.
                if (buoc == Buoc.TRANG) veDanTrang(cacTrang, den, chuong)
            }
        }
    }

    /** Moi nhip ve bay nhieu dong. */
    private val MOI_CUM = 20

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

    // -------------------------------------------------------------- buoc on tap

    /**
     * Cac cau con tung sai, da sua dung, va da DEN HEN nho lai.
     *
     * Khong khoa cau nao ca - o day moi cau deu la cau da tra gio roi, va do chinh
     * la dieu kien de duoc on. Danh sach chi hien cau den hen, nen con khong phai
     * tu chon xem nen on cai nao: den hen thi no nam day, chua den thi khong.
     */
    private fun veOnTap() {
        binding.tieuDe.text = "Ôn lại câu từng sai"
        binding.phuDe.text = "Làm lại trong vở bằng BÚT ĐỎ rồi chụp. Đúng thì được " +
            "cộng thêm giờ chơi. Viết bút thường thì máy không tính giờ."
        daTich.clear()

        lifecycleScope.launch {
            val ct = this@ChonBaiActivity
            val cau = withContext(Dispatchers.IO) {
                KhoBai.get(ct).cacCauTheoId(SoCaiBai.cacCauDangOn(ct))
            }
            if (cau.isEmpty()) {
                buoc = Buoc.MON
                veLai()
                return@launch
            }
            cacCau = cau
            daXong = emptySet()
            canSua = emptySet()

            binding.danhSach.removeAllViews()
            binding.dayNut.visibility = View.VISIBLE
            themDong(ten = "Chọn hết ${cau.size} câu", phu = null) {
                cau.forEach { daTich.add(it.id) }
                veLai()
                danhDauLaiOTich()
            }
            cau.forEach { themCau(it) }
            capNhatNut()
        }
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
        if (buoc == Buoc.LAM_THEM) {
            binding.nutChup.isEnabled = daTich.isNotEmpty()
            binding.nutChup.text = if (daTich.isEmpty()) {
                "Chọn ít nhất một câu"
            } else {
                "Chụp bài (${daTich.size} câu)"
            }
            binding.demTich.text = "Chọn câu con vừa làm xong."
            return
        }
        if (buoc == Buoc.ON_TAP) {
            binding.nutChup.isEnabled = daTich.isNotEmpty()
            binding.nutChup.text = if (daTich.isEmpty()) {
                "Chọn ít nhất một câu"
            } else {
                "Chụp bài ôn (${daTich.size} câu)"
            }
            binding.demTich.text = "Chọn câu con vừa làm lại."
            return
        }
        if (buoc == Buoc.TRANG) {
            binding.nutChup.isEnabled = trangChon.isNotEmpty()
            binding.nutChup.text = if (trangChon.isEmpty()) {
                "Chọn ít nhất một trang"
            } else {
                "Xem câu ở ${keTenTrang()}"
            }
            binding.demTich.text = "Cô giao trang nào thì chọn trang đó."
            return
        }
        binding.nutChup.isEnabled = daTich.isNotEmpty()
        binding.nutChup.text = if (daTich.isEmpty()) {
            "Chọn ít nhất một câu"
        } else {
            "Chụp bài (${daTich.size} câu)"
        }
        binding.demTich.text = if (daTich.isEmpty()) {
            "Chọn câu con vừa làm xong."
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
        if (buoc == Buoc.ON_TAP) return chupOnTap()
        if (buoc == Buoc.LAM_THEM) return chupLamThem()
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

    /**
     * Chup bai lam them. Cau nam rai rac nhieu trang nen khai theo danh sach cau,
     * khong theo trang. Van la bai moi nen tinh gio day du, khong phai nua nhu on.
     *
     * Danh sach lam them tron ca hai tap Toan, ma mot lan nop chi mang duoc ma cua
     * mot quyen. Nen lay quyen cua cau dau tien va chi nop cac cau cung quyen do -
     * giong [chupOnTap]. Con tich lan ca hai tap thi lan nay nop tap cua cau dau,
     * lan sau quay lai nop tiep tap kia.
     */
    private fun chupLamThem() {
        val chon = cacCau.filter { it.id in daTich }
        if (chon.isEmpty()) return
        val nguon = chon.first().nguon
        val cungQuyen = chon.filter { it.nguon == nguon }
        val sachDo = NganHang.sachTheoNguon(nguon)
        moManChup(
            PhamVi(
                mon = sachDo?.mon ?: mon,
                nguon = nguon,
                tenNguon = sachDo?.ten.orEmpty(),
                bai = "làm thêm ${cungQuyen.size} câu",
                cauIds = cungQuyen.map { it.id }
            )
        )
    }

    /**
     * Chup bai on. Tat ca deu la cau trong ngan hang nen deu co ma sach.
     *
     * Lay ten quyen theo cau dau: cau lenh gui AI ke ten mot quyen, tron hai quyen
     * vao mot danh sach thi dong chu do noi sai. Con on cau cua hai quyen cung luc
     * la chuyen hiem, va luc do chia lam hai lan nop cung khong sao.
     */
    private fun chupOnTap() {
        val chon = cacCau.filter { it.id in daTich }
        if (chon.isEmpty()) return
        val nguon = chon.first().nguon
        val cungQuyen = chon.filter { it.nguon == nguon }
        val sachDo = NganHang.sachTheoNguon(nguon)
        moManChup(
            PhamVi(
                mon = sachDo?.mon ?: cungQuyen.first().mon,
                nguon = nguon,
                tenNguon = sachDo?.ten.orEmpty(),
                bai = "ôn lại câu từng sai",
                cauIds = cungQuyen.map { it.id },
                onTap = true
            )
        )
    }

    /**
     * Truoc khi chup, hoi mot cau: cau nao con thay chua chac.
     *
     * VI SAO HOI. Cham bai cho biet con lam dung hay sai. Cau hoi nay cho biet mot
     * thu khac va kho hon: con co BIET minh dang biet gi khong. Cau bao chac ma sai
     * la cho nguy hiem nhat - con se khong quay lai xem no nua. Cau bao chua chac ma
     * dung thi nguoc lai, do la cho con dang tu danh gia thap minh.
     *
     * KHONG DINH GI DEN SO PHUT, va phai giu dung nhu vay. Gan thuong vao "doan
     * dung" thi lan sau con khai theo cai co loi chu khong theo cai no nghi, va cau
     * hoi mat sach gia tri.
     *
     * Chi hoi khi danh sach con ngan. Mot trang trac nghiem ba muoi cau ma bat tich
     * tung cau thi cau hoi tot den may cung thanh mot cai cua ai.
     */
    private fun moManChup(pham: PhamVi) {
        // Lan nop de SUA thi hoi cau khac: con da biet minh sai o day roi, hoi
        // "chac hay chua chac" nua la thua. Hoi mot hop moi lan, khong bao gio hai.
        val dangSua = pham.cauIds.any { it in canSua }
        if (dangSua) return hoiSaiChoNao(pham)
        if (pham.theoSach && !pham.onTap && pham.cauIds.size in 1..MAX_HOI_CHAC) {
            return hoiChuaChac(pham)
        }
        chupThat(pham)
    }

    /**
     * Truoc khi nop lai bai da sua, bat con goi ten cai sai cua chinh no.
     *
     * Mot dong thoi, khong cham dung sai, khong anh huong so phut. Goi ten duoc thi
     * lan sau moi tranh duoc; ma khong goi ten duoc thi thuong la vi con chua sua
     * that, chi chep lai dap an. Dong chu nay di thang sang Telegram cho Ba Huy.
     *
     * Bo qua duoc: bat buoc dien thi con se go mot chu cho xong, va luc do o nay
     * vua vo dung vua thanh mot cai cua phai vuot qua moi lan nop.
     */
    private fun hoiSaiChoNao(pham: PhamVi) {
        val o = EditText(this).apply {
            hint = "Ví dụ: con đặt nhân tử chung sai ở dòng cuối"
            setSingleLine(false)
            maxLines = 3
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, p / 2)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Lần trước con sai ở đâu?")
            .setView(o)
            .setPositiveButton("Chụp bài") { _, _ ->
                chupThat(pham.copy(conNoi = o.text.toString().trim().take(200)))
            }
            .setNegativeButton("Bỏ qua") { _, _ -> chupThat(pham) }
            .show()
    }

    private fun hoiChuaChac(pham: PhamVi) {
        val cac = KhoBai.get(this).cacCauTheoId(pham.cauIds)
        if (cac.isEmpty()) return chupThat(pham)
        val tick = BooleanArray(cac.size)
        MaterialAlertDialogBuilder(this)
            .setTitle("Câu nào con thấy chưa chắc?")
            .setMultiChoiceItems(
                cac.map { it.dongChon() }.toTypedArray(), tick
            ) { _, i, c -> tick[i] = c }
            .setPositiveButton("Chụp bài") { _, _ ->
                chupThat(
                    pham.copy(
                        chuaChac = cac.filterIndexed { i, _ -> tick[i] }.map { it.id },
                        daKhaiChac = true
                    )
                )
            }
            .setNegativeButton("Con chắc hết") { _, _ ->
                chupThat(pham.copy(daKhaiChac = true))
            }
            .show()
    }

    private fun chupThat(pham: PhamVi) {
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

    companion object {
        /**
         * Nhieu hon bay nhieu cau thi khong hoi "chua chac" nua.
         *
         * Mot trang trac nghiem ba muoi cau ma bat tich tung cau la mot cai cua ai
         * dung truoc man chup, va con se bam bua cho xong.
         */
        private const val MAX_HOI_CHAC = 12
    }

    /** Tra ve chinh dong vua them, de cho nao co so lieu ve sau con dien vao. */
    private fun themDong(ten: String, phu: String?, bam: () -> Unit): LinearLayout {
        val v = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_chon, binding.danhSach, false) as LinearLayout
        v.findViewById<TextView>(R.id.ten).text = ten
        v.findViewById<TextView>(R.id.phu).apply {
            text = phu.orEmpty()
            visibility = if (phu.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        v.setOnClickListener { bam() }
        binding.danhSach.addView(v)
        return v
    }
}
