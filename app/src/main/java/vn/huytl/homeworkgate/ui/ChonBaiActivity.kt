package vn.huytl.homeworkgate.ui

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
import vn.huytl.homeworkgate.data.KhaiChoCham
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import vn.huytl.homeworkgate.databinding.StActivityChonBaiBinding
import vn.huytl.homeworkgate.databinding.StDongTrangBinding
import vn.huytl.homeworkgate.kho.CauHoi
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

    /**
     * Bon buoc, dung thu tu con di qua.
     *
     * Ba buoc chup vo khac - on lai bai, lam them cho quen tay, luyen cho hay vap - bo
     * ngay 2/10/2026 (Ba Huy chon). Tu 29/9/2026 ca ba la bai lam tren may
     * ([LamBaiActivity]) va khong con nut nao mo toi chung o day nua. Buoc lam them con
     * giu cach xep cau cu, bai gan moc truoc, trai voi Luyen tap tu 2/10/2026.
     *
     * Ba buoc do la cho duy nhat ra danh sach tron nhieu quyen. Nay danh sach cau chi
     * con cua mot quyen (quyen con vua chon), nen khong con phai chan tich lan hai quyen.
     */
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

    /**
     * Cau so cai con ghi la sai. KHOA, khong tich duoc (Ba Huy chon ngay 28/9/2026): cau
     * sai chi nop lai bang nut "Nộp lại N câu sai" tren the cua bai do o man ket qua. Con
     * muon nop them thi chon cau moi. Truoc day cau sai o day con duoc tich san, va day la
     * duong nop lai thu hai, khac buoc voi duong kia.
     */
    private var canSua: Set<String> = emptySet()

    /** Cau dang nam trong mot bai cho chua cham. Khoa, xem [KhaiChoCham.cauChoCham]. */
    private var choCham: Set<String> = emptySet()
    private val daTich = linkedSetOf<String>()

    /** Cau nay khong tich duoc: da tinh gio, dang can sua, hay dang cho cham. */
    private fun biKhoa(id: String): Boolean = id in daXong || id in canSua || id in choCham

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
        // post chu khong goi thang: luc nay danh sach moi chua do xong, ScrollView
        // van dang giu vi tri cuon cu va cuon ngay bay gio thi khong an thua. Thay
        // ro nhat luc ve tu man vo dan do - dong dau tien bi cuon khuat len tren.
        binding.khungCuon.post { binding.khungCuon.scrollTo(0, 0) }
        when (buoc) {
            Buoc.MON -> veMon()
            Buoc.SACH -> veSach()
            Buoc.TRANG -> veTrang()
            Buoc.CAU -> veCau()
        }
    }

    // ------------------------------------------------------------------ buoc mon

    private fun veMon() {
        binding.tieuDe.text = "Lê Hòa đang làm bài môn gì?"
        binding.phuDe.text = "Chọn môn rồi chọn bài, xong mới chụp."

        // Mon hoc hom nay len dau: phan lon bai ve nha la cua buoi hoc vua xong, nen
        // dat chung o tren la con khoi phai doc het danh sach.
        val homNay = ThoiKhoaBieu.monTrongNgay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        val conLai = ThoiKhoaBieu.tatCaMon().filterNot { it in homNay }

        /*
         * On lai cau den hen khong con o day (29/9/2026): on lai lam tren may, dong cua no
         * nam ngoai man chinh. Man nay chi con bai trong vo dan do.
         *
         * Mon chua co sach trong may gop vao mot dong "Bài môn khác" (Ba Huy chon ngay
         * 29/9/2026): mot lan chup chung, bam vao thi chon mon. Truoc do moi mon mot dong,
         * muoi hai dong chup y het nhau. Mon nao sau nay co sach thi tu hien thanh dong rieng.
         */
        val coSachHomNay = homNay.filter { NganHang.coSach(it) }
        val coSachKhac = conLai.filter { NganHang.coSach(it) }
        val chuaCoSach = (homNay + conLai).filterNot { NganHang.coSach(it) }
        if (coSachHomNay.isNotEmpty()) {
            themChuong("Học hôm nay")
            coSachHomNay.forEach { themMon(it) }
            themChuong("Môn khác")
        }
        coSachKhac.forEach { themMon(it) }
        if (chuaCoSach.isNotEmpty()) {
            themDong(
                ten = "Bài môn khác",
                phu = "Môn chưa có sách trong máy: " + chuaCoSach.take(3).joinToString(", ") +
                    if (chuaCoSach.size > 3) "…" else ""
            ) { hoiMonKhac(chuaCoSach) }
        }
    }

    /** Chon mot mon chua co sach roi chup nhu truoc gio. Mon hoc hom nay xep truoc. */
    private fun hoiMonKhac(cac: List<String>) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Bài môn gì?")
            .setItems(cac.toTypedArray()) { _, i ->
                mon = cac[i]
                chupTuDo()
            }
            .show()
    }

    /**
     * Mot dong mon. Toan, KHTN, Tieng Anh tu 2/10/2026 khong con chup bai SGK (Ba Huy chot): bai
     * SGK lam tren may, man nay chi con dong dan sang "Làm bài tập trong SGK" va "Bài khác". Mon
     * lam tren may ma chua co SGK trong may (Tieng Anh luc viet) thi khong con quyen nao de chon
     * (sach bai tap thi co khong giao), bam la chup thang nhu mon chua co sach.
     */
    private fun themMon(ten: String) {
        val coSach = NganHang.coSach(ten)
        val sgkTrenMay = ten in LamTrenMay.MON
        val coSgk = NganHang.sachGiaoKhoaCua(ten).isNotEmpty()
        themDong(
            ten = ten,
            phu = when {
                !coSach -> null
                sgkTrenMay && coSgk -> "Bài SGK làm trên máy, bài khác chụp ảnh"
                sgkTrenMay -> "Chụp bài"
                else -> "Có sách trong máy — chọn được từng câu"
            },
            huyHieuMon = ten
        ) {
            mon = ten
            if (coSach && (!sgkTrenMay || coSgk)) {
                buoc = Buoc.SACH
                veLai()
            } else {
                // Mon chua nap sach, hay mon lam tren may chua co SGK: khong co gi de chon nua,
                // di thang sang chup.
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

        if (mon in LamTrenMay.MON) return veSachTrenMay()

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

        // Lam them khong con chup anh (29/9/2026): no la bai lam tren may, dong cua no nam
        // ngoai man chinh. Man nay chi con bai trong vo dan do.
    }

    /**
     * Toan, KHTN, Tieng Anh (2/10/2026, Ba Huy chot): dong SGK mo thang "Làm bài tập trong SGK"
     * ([BaiSgkActivity]), cung mot cho voi dong o trang Luyen tap - hai cua dan ve mot cho. Bo
     * hai dong sach bai tap vi co khong giao sach bai tap. "Bài khác" van chup, va anh van di
     * Bang dieu khien nho Claude cham.
     */
    private fun veSachTrenMay() {
        themDong(
            ten = "Bài trong SGK",
            phu = "Làm trên máy, mở Luyện tập › Làm bài tập trong SGK"
        ) {
            BaiSgkActivity.mo(this, mon)
            // Dong man khai bai: lam xong bai SGK, bam quay lai thi ve man chinh, khong ve lai
            // giua chung man nay.
            finish()
        }
        themDong(
            ten = "Bài khác",
            phu = "Vở bài tập, phiếu photo, đề cô cho riêng · chụp ảnh"
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

    // ------------------------------------------------------------------ buoc cau

    private fun veCau() {
        val s = sach ?: return
        if (trangChon.isEmpty()) return
        binding.tieuDe.text = "Lê Hòa làm câu nào?"
        // Ngoac chu khong dau cham giua: ngoac long vao ten sach gon hon ("SGK Toán 8 tập
        // một (tr.36)"), va trung dung quy uoc dang dung cho nhan cau: "2.26d (tr.36)",
        // "Câu hỏi (tr.11)".
        binding.phuDe.text = "${s.ten} (${keTenTrang()})"
        daTich.clear()

        lifecycleScope.launch {
            val ct = this@ChonBaiActivity
            data class Nap(
                val cau: List<CauHoi>,
                val xong: Set<String>,
                val sua: Set<String>,
                val cho: Set<String>
            )
            val nap = withContext(Dispatchers.IO) {
                val cau = NganHang.cacCauTheoTrang(ct, s.nguon, trangChon.toList())
                val kho = KhoBai.get(ct)
                val han = System.currentTimeMillis() - 365L * 24 * 60 * 60_000L
                Nap(
                    cau = cau,
                    xong = kho.daXongTrong(cau.map { it.id }, han),
                    sua = SoCaiBai.canSua(ct).map { it.khoa }.toSet(),
                    cho = KhaiChoCham.cauChoCham(ct)
                )
            }
            cacCau = nap.cau
            daXong = nap.xong
            canSua = nap.sua
            choCham = nap.cho

            binding.danhSach.removeAllViews()
            binding.dayNut.visibility = View.VISIBLE

            val conLam = cacCau.filterNot { biKhoa(it.id) }
            if (conLam.isNotEmpty()) {
                themDong(ten = "Chọn hết ${conLam.size} câu chưa làm", phu = null) {
                    // Tich thang len cac o dang co, KHONG ve lai ca danh sach: ve lai
                    // la chay lai veCau, ma viec dau tien cua no la xoa sach daTich -
                    // bam "Chon het" thanh ra bo het tich.
                    conLam.forEach { daTich.add(it.id) }
                    danhDauLaiOTich()
                }
            }
            cacCau.forEach { themCau(it) }
            capNhatNut()
        }
    }

    /** Dong cau: nhan to dam o tren, de bai o duoi. Tu 2/10/2026 nam o [MoChup.dongCau]. */
    private fun themCau(cau: CauHoi) {
        val dong = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_cau_chep, binding.danhSach, false)
        val o = dong.findViewById<MaterialCheckBox>(R.id.o_cau)
        val xong = cau.id in daXong
        val sua = cau.id in canSua
        val cho = cau.id in choCham

        o.text = MoChup.dongCau(cau) {
            when {
                xong -> append("\n✓ đã tính giờ rồi")
                cho -> append("\n… đang chờ chấm")
                sua -> append("\n● đang cần sửa, nộp lại ở màn Kết quả")
            }
        }
        o.tag = cau.id
        // Chep nhan va de, bo dong tinh trang ("đã tính giờ", "đang cần sửa"...): do la
        // chuyen cua may.
        dong.findViewById<View>(R.id.nut_chep).setOnClickListener {
            Chep.vao(this, SoMu.hienDe(cau.dongChon(), cau.mon))
        }

        if (xong || sua || cho) {
            // Cau da tra gio thi khong tich duoc nua. Chan ngay o day chu khong de
            // no di den luc cham roi moi noi "cau nay tinh roi": con nhin mot cai la
            // biet con nhung cau nao phai lam, khong phai chup xong moi biet.
            //
            // Cau dang can sua va cau dang cho cham cung khoa tu 28/9/2026 (Ba Huy chon):
            // cau sai chi nop lai bang nut tren the cua bai do o man ket qua, con cau
            // dang cho cham thi nop lai la trung, Ba Huy phai cham hai lan.
            o.isEnabled = false
            o.setTextColor(ContextCompat.getColor(this, R.color.ink_soft))
        } else {
            o.setOnCheckedChangeListener { _, tick ->
                if (tick) daTich.add(cau.id) else daTich.remove(cau.id)
                capNhatNut()
            }
        }
        binding.danhSach.addView(dong)
    }

    /** Tich lai cac o theo [daTich] sau khi ve lai danh sach. */
    private fun danhDauLaiOTich() {
        for (i in 0 until binding.danhSach.childCount) {
            // O tich nam trong dong co nut Chep, xem [themCau].
            val v = binding.danhSach.getChildAt(i)
                .findViewById<MaterialCheckBox>(R.id.o_cau) ?: continue
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
     * Hoi cau chua chac roi mo camera, xem [MoChup.mo]. Tach ra [MoChup] ngay 2/10/2026 de man bai
     * tap SGK ([BaiSgkActivity], [GiaiDeActivity]) chup cau phai viet cung mot duong voi man nay.
     */
    private fun moManChup(pham: PhamVi) = MoChup.mo(this, pham) { daGui = true }

    // --------------------------------------------------------------------- ve vat

    private fun themChuong(ten: String) {
        val t = LayoutInflater.from(this)
            .inflate(R.layout.st_dong_chuong, binding.danhSach, false) as TextView
        t.text = ten
        binding.danhSach.addView(t)
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
