package vn.huytl.homeworkgate.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
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
import vn.huytl.homeworkgate.data.LoaiLoi
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.databinding.StActivityChonBaiBinding
import vn.huytl.homeworkgate.databinding.StDongTrangBinding
import vn.huytl.homeworkgate.kho.CauHoi
import android.widget.EditText
import android.widget.ScrollView
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
    private enum class Buoc { MON, SACH, TRANG, CAU, ON_TAP, LAM_THEM, LUYEN }

    private lateinit var binding: StActivityChonBaiBinding

    private var buoc = Buoc.MON
    private var mon: String = ""
    private var sach: NganHang.Sach? = null

    /** Vao thang buoc on, khong qua buoc chon mon. Xem [EXTRA_ON_TAP]. */
    private var vaoThangOnTap = false

    /**
     * Nhan loi dang luyen, rong la khong phai duong nay. Xem [EXTRA_LUYEN].
     *
     * Duong nay chi vao tu man "Lê Hòa đã làm được gì", nen no luon la duong vao
     * thang: khong co buoc chon mon nao o sau lung de quay ve.
     */
    private var nhanLuyen = ""

    /**
     * Quyen cua cac cau dang tich, null la chua tich gi.
     *
     * Mot lan nop chi mang ma cua MOT quyen - cau lenh gui AI ke ten mot quyen. Cac
     * danh sach tron nhieu quyen (on tap, lam them) truoc day van cho tich lan lon,
     * roi luc chup lang le bo cac cau khong cung quyen voi cau dau tien: con tich
     * tam cau, nut ghi "chup bai on (8 cau)", ma chi nam cau di cham. Ba cau kia con
     * van lam trong vo bang but do, khong ai tinh.
     *
     * Nen chan ngay o cho tich: tich sang quyen khac thi cac tich cu bo di, va man
     * hinh luon chi co mot quyen dang chon.
     */
    private var quyenDangTich: String? = null

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

        // Nut "On lai" ngoai man chinh vao thang buoc on. Truoc day no mo buoc chon
        // mon nhu duong nop bai thuong: con bam "On lai" roi thay man hinh hoi dang
        // lam bai mon gi, kem ca thoi khoa bieu hom nay, con viec no vua bam thi nam
        // trong mot dong nho o tren cung.
        if (intent.getBooleanExtra(EXTRA_ON_TAP, false)) {
            vaoThangOnTap = true
            buoc = Buoc.ON_TAP
        }

        // Duong luyen cho hay vap, vao tu the trong man tien bo.
        intent.getStringExtra(EXTRA_LUYEN)?.takeIf { it.isNotBlank() }?.let {
            nhanLuyen = it
            buoc = Buoc.LUYEN
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
        // Ve tu man vo dan do thi dong dau tien phai doi chu ngay, khong thi con
        // vua luu xong quay ra van thay "Chụp vở dặn dò hôm nay".
        else if (buoc == Buoc.MON) veLai()
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
            // Vao thang buoc on thi lui la thoat han: khong co buoc chon mon nao o
            // sau lung de quay ve, va tha con ra man chon mon la tra no ve dung cho
            // no khong dinh den.
            Buoc.ON_TAP -> if (vaoThangOnTap) {
                finish()
                return
            } else {
                Buoc.MON
            }
            Buoc.LAM_THEM -> Buoc.SACH
            // Luyen luon la duong vao thang tu man tien bo: lui la ve dung man do.
            Buoc.LUYEN -> {
                finish()
                return
            }
        }
        veLai()
    }

    private fun veLai() {
        binding.danhSach.removeAllViews()
        binding.dayNut.visibility = View.GONE
        // post chu khong goi thang: luc nay danh sach moi chua do xong, ScrollView
        // van dang giu vi tri cuon cu va cuon ngay bay gio thi khong an thua. Thay
        // ro nhat luc ve tu man vo dan do - dong dau tien bi cuon khuat len tren.
        binding.khungCuon.post { binding.khungCuon.scrollTo(0, 0) }
        when (buoc) {
            Buoc.MON -> veMon()
            Buoc.SACH -> veSach()
            Buoc.TRANG -> veTrang()
            Buoc.CAU -> veCau()
            Buoc.ON_TAP -> veOnTap()
            Buoc.LAM_THEM -> veLamThem()
            Buoc.LUYEN -> veLuyen()
        }
    }

    // ------------------------------------------------------------------ buoc mon

    private fun veMon() {
        binding.tieuDe.text = "Lê Hòa đang làm bài môn gì?"
        binding.phuDe.text = "Chọn môn rồi chọn bài, xong mới chụp."

        themDanDo()

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
                text = ToChu.toButDo(
                    this@ChonBaiActivity,
                    "Đến hẹn nhớ lại. Làm trong vở bằng bút đỏ rồi chụp"
                )
                visibility = View.VISIBLE
            }
            dong.setOnClickListener {
                buoc = Buoc.ON_TAP
                veLai()
            }
            binding.danhSach.addView(dong, 1)
        }

        if (homNay.isNotEmpty()) {
            themChuong("Học hôm nay")
            homNay.forEach { themMon(it) }
            themChuong("Môn khác")
        }
        conLai.forEach { themMon(it) }
    }

    /**
     * Dong vo dan do, luon nam tren cung man chon mon.
     *
     * DE O DAY CHU KHONG NHET VAO LUC NOP BAI. Trang vo dan do la viec dau buoi, lam
     * MOT lan cho ca ngay; nop bai la viec cuoi buoi, lam bao nhieu lan cung duoc.
     * Truoc day hai viec dinh vao nhau nen con phai chup lai trang vo o tung lan nop.
     *
     * Chup roi thi dong nay VAN o day, chi doi chu: bam vao la xem lai va sua duoc
     * cai may da doc. An di thi con khong con duong nao sua mot chu doc nham, ma cai
     * chu do lai dang quyet dinh tron goi 45 phut.
     */
    private fun themDanDo() {
        // Tat cham AI van hien dong nay: may van doc vo dan do cho con soat, chi viec
        // cham bai la sang tay Claude. Xem Prefs.chamBangAi.
        //
        // Don o day nua chu khong chi luc dich vu khoi dong: [ApprovalService] la
        // foreground START_STICKY, chay lien mach ca tuan nen onCreate cua no gan
        // nhu khong goi lai lan nao. Man nay thi con vao moi lan nop bai.
        VoDanDo.donDep(this)
        val d = VoDanDo.conHieuLuc(this)
        themDong(
            ten = if (d == null) "Chụp vở dặn dò hôm nay" else "Vở dặn dò ${d.moTa()}",
            phu = when {
                d == null -> "Chụp một lần thôi. Máy đọc ra chữ cho Lê Hòa soát lại trước khi dùng"
                d.chuaDoc -> "Máy chưa đọc được, ảnh đã gửi ba Huy. Mấy lần nộp sau không phải chụp lại"
                d.nguon == VoDanDo.NGUON_CLAUDE -> "Claude đã đọc giúp. Bấm để xem hoặc sửa"
                d.nguon == VoDanDo.NGUON_LUC_CHAM -> "Đọc ra lúc chấm bài. Bấm để xem hoặc sửa"
                else -> "Máy nhớ rồi, mấy lần nộp sau không phải chụp lại. Bấm để xem hoặc sửa"
            }
        ) {
            startActivity(Intent(this, DanDoActivity::class.java))
        }
    }

    private fun themMon(ten: String) {
        val coSach = NganHang.coSach(ten)
        themDong(
            ten = ten,
            phu = if (coSach) "Có sách trong máy — chọn được từng câu" else null,
            huyHieuMon = ten
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
                dong.findViewById<LinearProgressIndicator>(R.id.thanh_lam).apply {
                    visibility = View.VISIBLE
                    max = tong.coerceAtLeast(1)
                    setProgressCompat(xong, false)
                }
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
            phu = "Câu chưa làm, ưu tiên bài Lê Hòa vừa sai"
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
        xoaTich()

        lifecycleScope.launch {
            val ct = this@ChonBaiActivity
            val cau = withContext(Dispatchers.IO) { NganHang.cauNenLamThemCuaMon(ct, mon) }
            if (cau.isEmpty()) {
                binding.danhSach.removeAllViews()
                themChuong("Lê Hòa làm hết sách rồi")
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

    // ----------------------------------------------------------------- buoc luyen

    /**
     * Ba cau trong nhung bai con hay vap mot kieu loi.
     *
     * TICH SAN CA BA. Khac han cac buoc kia, o day con khong phai chon gi: no vua
     * bam mot nut ghi "Làm thử 3 câu" o man tien bo, va man hinh nay chi la cho xem
     * de bai truoc khi mo camera. Bat tich lai tung cau la hoi lai mot cau con vua
     * tra loi. Bo tich duoc neu no doi y ve mot cau.
     *
     * Van co the ra danh sach hai quyen khac nhau (Toan tap mot va tap hai), ma mot
     * lan nop chi mang ma cua mot quyen - xem [quyenDangTich]. Nen chi tich san cac
     * cau cung quyen voi cau dau tien, y nhu [chupLamThem] lam luc chup.
     */
    private fun veLuyen() {
        binding.tieuDe.text = "Luyện chỗ hay vấp"
        binding.phuDe.text = LoaiLoi.moTaChoCon(nhanLuyen)
            ?.replaceFirstChar { it.uppercase() }
            .orEmpty()
        xoaTich()

        lifecycleScope.launch {
            val ct = this@ChonBaiActivity
            val cau = withContext(Dispatchers.IO) {
                KhoBai.get(ct).cacCauLuyenTheoLoi(nhanLuyen)
            }
            if (cau.isEmpty()) {
                // Con lam het cac bai do trong luc dang mo man tien bo: dong lai chu
                // khong bo con o mot man hinh trong khong co duong nao di tiep.
                finish()
                return@launch
            }
            cacCau = cau
            daXong = emptySet()
            canSua = emptySet()
            quyenDangTich = cau.first().nguon
            daTich.addAll(cau.filter { it.nguon == quyenDangTich }.map { it.id })

            binding.danhSach.removeAllViews()
            binding.dayNut.visibility = View.VISIBLE
            var baiCu = ""
            cau.forEach { c ->
                if (c.bai.isNotBlank() && c.bai != baiCu) {
                    themChuong("${c.bai} · trang ${c.trang}")
                    baiCu = c.bai
                }
                themCau(c, keoTrang = true)
            }
            danhDauLaiOTich()
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
        val v = StDongTrangBinding.inflate(layoutInflater, binding.danhSach, false)
        v.soTrang.text = t.trang.toString()
        v.soTrang.setTextColor(ContextCompat.getColor(this, R.color.brand_dark))
        v.soTrang.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.brand_soft)
        v.tenBai.text = t.bai.ifBlank { "Trang ${t.trang}" }
        v.soCau.text = if (t.soCau == 1) "1 câu" else "${t.soCau} câu"
        v.oTich.isChecked = t.trang in trangChon
        v.root.tag = t.trang
        v.root.setOnClickListener {
            val tick = !v.oTich.isChecked
            v.oTich.isChecked = tick
            if (tick) trangChon.add(t.trang) else trangChon.remove(t.trang)
            capNhatNut()
        }
        binding.danhSach.addView(v.root)
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
        binding.tieuDe.text = "Ôn lại bài"
        binding.phuDe.text = ToChu.toButDo(
            this,
            "Làm lại trong vở bằng bút đỏ rồi chụp. Viết bút thường thì máy không tính."
        )
        xoaTich()

        lifecycleScope.launch {
            val ct = this@ChonBaiActivity
            val cau = withContext(Dispatchers.IO) {
                KhoBai.get(ct).cacCauTheoId(SoCaiBai.cacCauDangOn(ct))
            }
            if (cau.isEmpty()) {
                // Het cau de on (lich vua doi, hay bai vua duoc cham xong): tra con
                // ve buoc chon mon chu khong de man hinh trong. Bo luon duong vao
                // thang, khong thi nut Quay lai o buoc on sau do lai dong ca man.
                vaoThangOnTap = false
                buoc = Buoc.MON
                veLai()
                return@launch
            }
            cacCau = cau
            daXong = emptySet()
            canSua = emptySet()

            binding.danhSach.removeAllViews()
            binding.dayNut.visibility = View.VISIBLE

            /*
             * Chia theo quyen, moi quyen mot tieu de.
             *
             * Danh sach nay do may dua ra chu khong phai con tu chon, va no tron ca
             * Toan lan KHTN. Nhan cua sach Toan la ma in trong sach ("2.26a") - doi
             * chieu duoc voi trang dang mo, nhung mot minh no khong noi phai mo quyen
             * nao. Cac buoc khac khong can ten quyen vi con vua tu chon sach xong.
             *
             * Va cung la cho giai thich vi sao nop lam hai lan khi co hai quyen.
             */
            cau.groupBy { it.nguon }.forEach { (nguon, cacCauQuyen) ->
                themChuong(NganHang.sachTheoNguon(nguon)?.ten ?: nguon)
                if (cacCauQuyen.size > 1) {
                    themDong(ten = "Chọn hết ${cacCauQuyen.size} câu", phu = null) {
                        daTich.clear()
                        daTich.addAll(cacCauQuyen.map { it.id })
                        quyenDangTich = nguon
                        danhDauLaiOTich()
                    }
                }
                cacCauQuyen.forEach { themCau(it, keoTrang = true) }
            }
            capNhatNut()
        }
    }

    // ------------------------------------------------------------------ buoc cau

    private fun veCau() {
        val s = sach ?: return
        if (trangChon.isEmpty()) return
        binding.tieuDe.text = "Lê Hòa làm câu nào?"
        // Ngoac chu khong dau cham giua: ten sach tu no da co mot gach ngang dai ben
        // trong ("SGK Toán 8 — tập một"), them mot dau ngan cung suc nang nua thi hai
        // dau danh nhau. Ngoac la loai dau khac han nen no long vao gon, va trung dung
        // quy uoc dang dung cho nhan cau: "2.26d (tr.36)", "Câu hỏi (tr.11)".
        binding.phuDe.text = "${s.ten} (${keTenTrang()})"
        xoaTich()

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
                    // Tich thang len cac o dang co, KHONG ve lai ca danh sach: ve lai
                    // la chay lai veCau, ma viec dau tien cua no la xoa sach daTich -
                    // bam "Chon het" thanh ra bo het tich.
                    conLam.forEach { daTich.add(it.id) }
                    quyenDangTich = conLam.first().nguon
                    danhDauLaiOTich()
                }
            }
            cacCau.forEach { themCau(it) }
            capNhatNut()
        }
    }

    /**
     * Mot dong trong danh sach cau: nhan to dam o tren, de bai o duoi.
     *
     * TO DAM CHU KHONG THEM DAU NGAN. Hai phan nay khac loai han nhau - nhan la cho
     * TIM trong sach, de bai la cai PHAI LAM - nen cho chung khac nhau ve net chu thi
     * mat nhin ra ngay, khong phai doc mot dau cham giua roi tu hieu. Doi lai, mot ky
     * tu ngan nam giua hai doan chu dai thi lot thom, va do la ban truoc.
     *
     * Dam cua nhan lay do dai tu chinh [CauHoi.nhan] chu khong di tim ky tu xuong
     * dong: de bai cua vai cau co san dau xuong dong trong do, tim ky tu thi to dam
     * nham ca doan dau cua de.
     */
    private fun dongCau(
        cau: CauHoi,
        nhan: String = cau.nhan(),
        them: SpannableStringBuilder.() -> Unit = {}
    ): CharSequence =
        SpannableStringBuilder(cau.dongChon(nhan)).apply {
            setSpan(
                StyleSpan(Typeface.BOLD), 0, nhan.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            them()
        }

    /**
     * Nhan kem so trang, cho danh sach khong di qua buoc chon trang.
     *
     * Sach Toan in san so cau nen nhan la "2.26a": doi chieu duoc voi trang dang mo,
     * nhung mot minh no khong noi phai mo trang nao. O buoc chon cau thi khong can -
     * con vua tu chon trang xong. O buoc on thi can, vi danh sach do may dua ra.
     *
     * Sach KHTN da co san "(tr.11)" trong nhan nen khong them lan hai.
     */
    private fun nhanKemTrang(cau: CauHoi): String {
        val nhan = cau.nhan()
        return if (cau.trang <= 0 || "tr." in nhan || nhan.startsWith("Trang")) nhan
        else "$nhan (tr.${cau.trang})"
    }

    private fun themCau(cau: CauHoi, keoTrang: Boolean = false) {
        val o = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_cau, binding.danhSach, false) as MaterialCheckBox
        val xong = cau.id in daXong
        val sua = cau.id in canSua

        o.text = dongCau(cau, if (keoTrang) nhanKemTrang(cau) else cau.nhan()) {
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
                quyenDangTich = cau.nguon
            }
            o.setOnCheckedChangeListener { _, tick ->
                if (!tick) {
                    daTich.remove(cau.id)
                    if (daTich.isEmpty()) quyenDangTich = null
                    capNhatNut()
                    return@setOnCheckedChangeListener
                }
                // Tich sang quyen khac: bo cac tich cu di. Xem [quyenDangTich].
                val doiQuyen = quyenDangTich != null && quyenDangTich != cau.nguon
                if (doiQuyen) daTich.clear()
                quyenDangTich = cau.nguon
                daTich.add(cau.id)
                // danhDauLaiOTich bo tich cac o quyen cu tren man hinh, va goi
                // capNhatNut o cuoi - khong goi hai lan.
                if (doiQuyen) danhDauLaiOTich() else capNhatNut()
            }
        }
        binding.danhSach.addView(o)
    }

    /** Bo het tich dang co, ke ca quyen dang chon. */
    private fun xoaTich() {
        daTich.clear()
        quyenDangTich = null
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
            binding.demTich.text = "Chọn câu Lê Hòa vừa làm xong."
            return
        }
        if (buoc == Buoc.ON_TAP) {
            binding.nutChup.isEnabled = daTich.isNotEmpty()
            binding.nutChup.text = if (daTich.isEmpty()) {
                "Chọn ít nhất một câu"
            } else {
                "Chụp bài ôn (${daTich.size} câu)"
            }
            binding.demTich.text = "Chọn câu Lê Hòa vừa làm lại."
            return
        }
        if (buoc == Buoc.LUYEN) {
            binding.nutChup.isEnabled = daTich.isNotEmpty()
            binding.nutChup.text = if (daTich.isEmpty()) {
                "Chọn ít nhất một câu"
            } else {
                "Chụp bài (${daTich.size} câu)"
            }
            // Noi ro day khong phai bai co giao, de con khong tuong minh dang thieu
            // mot viec phai lam. Va noi luon la co tinh gio: cau hoi dau tien cua
            // mot dua tre truoc ba cau tu dung ra la "lam cai nay duoc gi khong".
            binding.demTich.text = "Làm trong vở rồi chụp. Tính giờ như bài làm thêm."
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
            "Chọn câu Lê Hòa vừa làm xong."
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
        if (buoc == Buoc.LAM_THEM || buoc == Buoc.LUYEN) return chupLamThem()
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
                // Duong luyen khong di qua buoc chon mon nen [mon] rong: lay ten mon
                // tu chinh cau, giong [chupOnTap].
                mon = sachDo?.mon ?: mon.ifBlank { cungQuyen.first().mon },
                nguon = nguon,
                tenNguon = sachDo?.ten.orEmpty(),
                bai = if (buoc == Buoc.LUYEN) {
                    "luyện lại ${cungQuyen.size} câu"
                } else {
                    "làm thêm ${cungQuyen.size} câu"
                },
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
                bai = "ôn lại bài",
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
            hint = "Ví dụ: Lê Hòa đặt nhân tử chung sai ở dòng cuối"
            setSingleLine(false)
            maxLines = 3
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, p / 2)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Lần trước Lê Hòa sai ở đâu?")
            .setView(o)
            .setPositiveButton("Chụp bài") { _, _ ->
                chupThat(pham.copy(conNoi = o.text.toString().trim().take(200)))
            }
            .setNegativeButton("Bỏ qua") { _, _ -> chupThat(pham) }
            .show()
    }

    /**
     * Tich nhung cau con thay chua chac.
     *
     * Danh sach dung chinh o tich cua man ben ngoai chu khong dung
     * setMultiChoiceItems: dong mac dinh cua hop thoai cat de bai o dong thu hai, ma
     * de toan thi phan quan trong hay nam o cuoi. Con phai doc duoc ca cau moi biet
     * minh chac hay khong.
     *
     * MOT NUT DI TIEP. Truoc day o day co hai nut - "Chup bai" va "Con chac het" -
     * ma khong tich gi roi bam cai nao cung ra ket qua y het nhau. Khong tich gi da
     * co nghia la con tu tin dung het, khong can mot nut rieng de noi dieu do.
     */
    private fun hoiChuaChac(pham: PhamVi) {
        val cac = KhoBai.get(this).cacCauTheoId(pham.cauIds)
        if (cac.isEmpty()) return chupThat(pham)
        val tick = BooleanArray(cac.size)

        val cot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (12 * resources.displayMetrics.density).toInt()
            setPadding(p, 0, p, 0)
        }

        cac.forEachIndexed { i, cau ->
            val o = LayoutInflater.from(this)
                .inflate(R.layout.st_dong_cau, cot, false) as MaterialCheckBox
            o.text = dongCau(cau)
            o.setOnCheckedChangeListener { _, c -> tick[i] = c }
            cot.addView(o)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Trong những câu dưới đây, có câu nào Lê Hòa không tự tin làm đúng không?")
            .setView(ScrollView(this).apply { addView(cot) })
            .setPositiveButton("Chụp bài để gửi") { _, _ ->
                chupThat(
                    pham.copy(
                        chuaChac = cac.filterIndexed { i, _ -> tick[i] }.map { it.id },
                        daKhaiChac = true
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
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

        /** Mo thang buoc on tap, bo qua buoc chon mon. Nut "On lai" ngoai man chinh. */
        const val EXTRA_ON_TAP = "on_tap"

        /**
         * Mo thang buoc luyen cho hay vap, kem nhan loi.
         *
         * Vao tu the "Chỗ hay vấp" trong man [TienBoActivity]. Gia tri la mot nhan
         * trong [vn.huytl.homeworkgate.data.LoaiLoi.TAP].
         */
        const val EXTRA_LUYEN = "luyen_loi"
    }

    /** Tra ve chinh dong vua them, de cho nao co so lieu ve sau con dien vao. */
    /**
     * Mot dong bam duoc trong ba buoc dau.
     *
     * [huyHieuMon] la ten mon lay mau va chu viet tat, xem [MatMon]. De null thi dong
     * khong co huy hieu - quyen sach hay trang thi khong co mau rieng nao ca.
     */
    private fun themDong(
        ten: String,
        phu: String?,
        huyHieuMon: String? = null,
        bam: () -> Unit
    ): LinearLayout {
        val v = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_chon, binding.danhSach, false) as LinearLayout
        v.findViewById<TextView>(R.id.ten).text = ten
        v.findViewById<TextView>(R.id.phu).apply {
            text = phu.orEmpty()
            visibility = if (phu.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        v.findViewById<TextView>(R.id.huy_hieu).apply {
            if (huyHieuMon == null) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = MatMon.tat(huyHieuMon)
                setTextColor(ContextCompat.getColor(context, MatMon.mau(huyHieuMon)))
                backgroundTintList =
                    ContextCompat.getColorStateList(context, MatMon.nen(huyHieuMon))
            }
        }
        v.setOnClickListener { bam() }
        binding.danhSach.addView(v)
        return v
    }
}
