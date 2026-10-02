package vn.huytl.homeworkgate.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.Calendar
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.QuyGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.databinding.ActivityCachKiemGioBinding
import vn.huytl.homeworkgate.databinding.ItemKiemGioBinding
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.KhoBai

/**
 * Bang gia gio choi: lam cai gi thi duoc may phut.
 *
 * VI SAO CO MAN NAY. Luat cong gio nam day du trong [LuatCongGio] va trong dau Ba
 * Huy. Phia Le Hoa thi chi thay dau ra: nop mot xap bai, mot lat sau hien ra mot
 * con so. Khong biet cai gi lam ra con so do thi con khong chon duoc viec nao lam
 * truoc, chi doan - va vai lan doan hut la du de tin rang so phut kia tuy hung.
 *
 * NO LA BANG GIA, KHONG PHAI DANH SACH VIEC. Khong bam vao dong nao duoc, khong co
 * nut nao dan di dau. Viec phai lam hom nay da co cho cua no o the "Viec hom nay"
 * ngoai man chinh; nhet them mot ban sao vao day thi hai cho cung doi hoi mot thu
 * va con phai doan xem cho nao moi la cho that.
 *
 * MOI CON SO DOC TU CHINH CAC HANG SO DANG CHAY - [LuatCongGio], [HocThuoc],
 * [LuatCongGio.TRAN_NGAY]. Go tay lai vao day thi den luc Ba Huy sua mot con so
 * trong luat, man nay im lang noi doi, ma noi doi dung cai bang con dang dua vao
 * de chon lam gi.
 */
class CachKiemGioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCachKiemGioBinding
    private lateinit var prefs: Prefs
    private lateinit var gate: GateStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCachKiemGioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs.get(this)
        gate = GateStore(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top + 28.dp(), bottom = bars.bottom + 28.dp())
            insets
        }
        binding.btnDone.setOnClickListener { finish() }
    }

    /**
     * Ve lai o [onResume] chu khong o [onCreate].
     *
     * Con mo man nay, doc xong, bam ra lam bai, roi bam vao lai de xem con bao
     * nhieu - duong di do la duong thuong nhat. Ve mot lan luc tao thi lan quay lai
     * van la con so cu, va con tuong bai vua nop khong duoc tinh.
     */
    override fun onResume() {
        super.onResume()
        veHomNay()
        veBaiTap()
        veKiemTra()
        veViecNha()
        veKhongTinh()
        veChan()
    }

    // ------------------------------------------------------------- hom nay

    /**
     * Hom nay dang o dau tren han muc ngay.
     *
     * Ve bang chinh [ThanhNgay] cua man chinh chu khong ve mot kieu khac: cung mot con so
     * ma hai man hinh ve hai kieu thi con phai hoc hai lan, va lan nao cung phai doi
     * chieu xem co khop nhau khong.
     */
    private fun veHomNay() {
        val so = ThanhNgay.so(gate)

        binding.txtHomNay.text = "Hôm nay được chơi ${so.duoc}/${so.tong} phút"
        ThanhNgay.ve(binding.khungThanh, binding.phanDaChoi, binding.phanDangGiu, so)

        binding.txtHomNayPhu.text = SpannableStringBuilder().apply {
            // Chu thich mau thay cho cau "Đang giữ N phút chưa chơi" truoc 1/10/2026: cung
            // con so do, nhung co cham mau noi no voi khuc nao tren thanh.
            val chuThich = ThanhNgay.chuThich(this@CachKiemGioActivity, so)
            if (chuThich.isNotEmpty()) append(chuThich).append('\n')
            if (so.conKiem > 0) {
                append("Làm bài nữa thì hôm nay còn kiếm thêm được tối đa ${so.conKiem} phút.")
            } else {
                append("Hôm nay đủ giờ rồi, mai nộp bài tiếp nhé.")
            }
            val quy = QuyGio.so(this@CachKiemGioActivity)
            if (quy > 0) append(" Quỹ giờ chơi: $quy phút.")
        }
    }

    // ----------------------------------------------------------- nop bai tap

    /**
     * Duong bai tap, duong ra nhieu gio nhat nen dat dau tien.
     *
     * Moi phan mot tran rieng, va man nay phai noi ro cho do: con doc nham thanh cong
     * don ca hai tran thi no trong cho mot so phut khong bao gio ra.
     */
    private fun veBaiTap() {
        val box = binding.boxBaiTap
        box.removeAllViews()

        /*
         * Hai duong kiem gio bang bai tap tu 29/9/2026: bai co giao (chup anh, Claude cham)
         * va bai lam tren may. Bai lam them chup anh khong con nua. Tu 30/9/2026 bai co giao
         * chi tinh le, khong con tron goi.
         */
        val hetTranAnh = SoCaiBai.hetTranAnhHomNay(this)
        val daAnh = SoCaiBai.phutAnhHomNay(this)
        themDong(
            box,
            ten = "Chụp bài cô giao",
            gia = "tối đa ${LuatCongGio.TRAN_ANH} phút",
            giaPhu = "mỗi ngày",
            nay = when {
                hetTranAnh -> "Hôm nay đã đủ ${LuatCongGio.TRAN_ANH} phút"
                daAnh > 0 -> "Hôm nay đã được $daAnh phút"
                else -> "Hôm nay chưa cộng"
            },
            mauNay = if (hetTranAnh) R.color.ok else R.color.ink_soft
        )

        /*
         * Cau dang cho sua la gio dang nam san tren ban, nhung chi hom chua du tran anh: du
         * roi thi cau sai chuyen sang lam tren may (Ba Huy chot 29/9/2026).
         */
        val canSua = SoCaiBai.canSua(this)
        if (canSua.isNotEmpty() && !hetTranAnh) {
            themDong(
                box,
                ten = "Sửa câu sai trong vở",
                gia = "tính chung ${LuatCongGio.TRAN_ANH} phút",
                giaPhu = "của bài chụp ảnh",
                nay = "Đang có ${canSua.size} câu chờ sửa: " +
                    canSua.take(3).joinToString(", ") { it.ma } +
                    if (canSua.size > 3) "…" else "",
                mauNay = R.color.alert
            )
        }

        val daMay = SoCaiBai.phutTrenMayHomNay(this)
        val deMo = runCatching { GiaiDe.dangMo(this) }.getOrDefault(emptyList())
        themDong(
            box,
            ten = "Luyện tập, Giải đề",
            gia = "tối đa ${LuatCongGio.TRAN_TREN_MAY} phút",
            giaPhu = "mỗi ngày",
            nay = buildString {
                append(if (daMay > 0) "Hôm nay đã được $daMay phút" else "Hôm nay chưa cộng")
                if (deMo.isNotEmpty()) append(". Đang có: " + deMo.joinToString(", ") { GiaiDe.tenDe(it) })
            },
            mauNay = if (daMay >= LuatCongGio.TRAN_TREN_MAY) R.color.wait else R.color.ink_soft
        )

        val soOn = LamTrenMay.soCauOn(this)
        val daOn = SoCaiBai.phutOnHomNay(this)
        val conOn = (LuatCongGio.TRAN_ON_MOI_NGAY - daOn).coerceAtLeast(0)
        themDong(
            box,
            ten = "Ôn lại câu đến hẹn",
            gia = "tối đa ${LuatCongGio.TRAN_ON_MOI_NGAY} phút",
            giaPhu = "mỗi ngày",
            nay = when {
                conOn <= 0 -> "Hôm nay ôn đủ ${LuatCongGio.TRAN_ON_MOI_NGAY} phút rồi"
                soOn == 0 -> "Hôm nay chưa có câu nào đến hẹn"
                daOn > 0 -> "Đang có $soOn câu đến hẹn, còn $conOn phút"
                else -> "Đang có $soOn câu đến hẹn"
            },
            mauNay = if (soOn == 0 || conOn <= 0) R.color.ink_soft else R.color.brand
        )

        veNhan(binding.nhanBaiTap, SoCaiBai.phutDaCongHomNay(this), het = false)
        ghiChu(
            binding.txtBaiTapChan,
            het = false,
            chu = "Phần làm trên máy vượt trần của ngày thì vào Quỹ giờ chơi. " +
                "${getString(R.string.parent_name_cap)} cấp từ quỹ khi nào thì chơi khi đó."
        )
    }

    // ---------------------------------------------------------- kiem tra bai

    private fun veKiemTra() {
        val box = binding.boxKiemTra
        box.removeAllViews()

        /*
         * Hai dong cua the nay treo TRAN NGAY chu khong treo gia mot cau, cung ly do
         * voi dong "On lai cau den hen" o the tren: moi duong co tran ngay rieng, va
         * con so con can biet la hom nay ngoi lam thi duoc them toi da bao nhieu.
         */
        themDong(
            box,
            ten = "Luyện tập trí nhớ",
            gia = "tối đa ${HocThuoc.TRAN_PHUT_MOI_NGAY} phút",
            giaPhu = "mỗi ngày"
        )

        /*
         * Duong tu vung nam cung the nay nhung co TRAN RIENG.
         *
         * De chung the vi voi Le Hoa hai viec la mot ho: may hoi, con go tra loi
         * ngay tren may. Nhung con so thi phai tach, va dong "nay" cua tung dong noi
         * ro phan cua no - gop mot con so thi lam xong mot ben la tuong het ca hai.
         */
        val daTu = LuatTuVung.phutTrongNgay(KhoBai.get(this).giayTuVungTu(moc0Gio()))
        val tranTu = LuatTuVung.phutTrongNgay(LuatTuVung.GIAY_TRAN_MOI_NGAY)
        val coBoTu = runCatching {
            BoTuVung.BO.any { KhoBai.get(this).soTuCua(it.bo) > 0 }
        }.getOrDefault(false)
        themDong(
            box,
            ten = "Kiểm tra từ vựng",
            gia = "tối đa $tranTu phút",
            giaPhu = "mỗi ngày",
            nay = when {
                !coBoTu -> "Máy chưa có bộ từ nào"
                daTu >= tranTu -> "Hôm nay kiểm tra đủ $tranTu phút rồi"
                daTu > 0 -> "Hôm nay đã được $daTu phút, còn ${tranTu - daTu} phút"
                else -> "Một buổi ${LuatTuVung.SO_TU_HANG_NGAY} từ, chưa dùng phút nào"
            },
            mauNay = if (!coBoTu || daTu >= tranTu) R.color.ink_soft else R.color.brand
        )

        val daCoThe = KhoBai.get(this).phutTheTu(moc0Gio())
        val conThe = (HocThuoc.TRAN_PHUT_MOI_NGAY - daCoThe).coerceAtLeast(0)
        // Moi the chi den luot vai ngay mot lan. Het luot thi phan nay hom nay khong
        // ra duoc phut nao, du tran con nguyen - va do la dieu phai noi ra.
        //
        // Chi dem the trong phan lop da hoc, xem [vn.huytl.homeworkgate.kho.HocToi]. Bo
        // chua chon bai thi dem la 0, va "het cau" luc do la noi sai: con chua chon, chu
        // khong phai het. Con "mai co lai" thi sai khi het the vi lop chua hoc toi bai
        // sau: mai cung khong co gi, cho toi luc con vao chon them bai.
        val denLuot = runCatching { BoThe.bang(this).sumOf { it.soDenLuot } }.getOrDefault(0)
        val tinh = runCatching { BoThe.tinhTrangManChinh(this) }
            .getOrDefault(BoThe.TinhTrang.KHONG)
        val chuaChon = tinh == BoThe.TinhTrang.CHUA_CHON

        veNhan(
            binding.nhanKiemTra,
            daCoThe,
            het = conThe <= 0,
            khiTrong = when {
                denLuot > 0 -> "Đang có $denLuot câu đến lượt"
                chuaChon -> "Chưa chọn bài đã học"
                else -> "Hôm nay hết câu"
            }
        )
        ghiChu(
            binding.txtKiemTraChan,
            het = conThe <= 0 || (denLuot == 0 && !chuaChon),
            chu = when {
                conThe <= 0 -> "Hôm nay hết phần này rồi."
                denLuot == 0 && chuaChon ->
                    "Vào Kiểm tra bài chọn bài lớp đã học tới, máy mới biết hỏi câu nào."
                denLuot == 0 && tinh == BoThe.TinhTrang.HET_HOM_NAY ->
                    "Hôm nay không còn câu nào đến lượt. Lớp học tới bài mới thì vào " +
                        "Kiểm tra bài chọn lại."
                denLuot == 0 -> "Hôm nay không còn câu nào đến lượt, mai có lại."
                daCoThe > 0 -> "Hôm nay đã được $daCoThe phút, còn $conThe phút."
                else -> "Đang có $denLuot câu đến lượt, chưa dùng phút nào."
            }
        )
    }

    // -------------------------------------------------------------- viec nha

    /**
     * Viec nha khac hai duong kia o mot cho: con khong tu bat dau duoc.
     *
     * Nguoi lon giao thi moi co, khong ai giao thi khong co gi de lam - nen o day
     * "chua cong" khong co nghia la "con chua lam". Huy hieu phai noi dung the: dang
     * co viec cho thi ke ra con bao nhieu phut, khong co viec thi noi thang la hom nay
     * chua ai giao, de con thoi doi o cho khong the co gi.
     */
    private fun veViecNha() {
        val box = binding.boxViecNha
        box.removeAllViews()

        val phien = ViecNha.dangTreo(this)
        val chuaXong = phien?.chuaXong.orEmpty()
        val choSan = chuaXong.sumOf { it.phut }
        val daCong = ViecNha.phutHomNay(this)

        themDong(
            box,
            ten = "Làm xong việc nhà được giao",
            gia = "mỗi việc một số phút",
            giaPhu = "không giới hạn",
            nay = if (chuaXong.isEmpty()) "" else
                "Đang chờ: " + chuaXong.joinToString(", ") { "${it.ten} (${it.phut} phút)" },
            mauNay = R.color.wait
        )

        veNhan(
            binding.nhanViecNha,
            daCong,
            het = false,
            khiTrong = when {
                choSan > 0 -> "Làm xong được $choSan phút"
                // Viec 0 phut van la viec dang giao, khong phai "chua ai giao".
                chuaXong.isNotEmpty() -> "Việc đang giao không có phút"
                else -> "Hôm nay chưa ai giao việc"
            }
        )
    }

    // ----------------------------------------------------------- khong tinh

    /**
     * Phan nay quan trong ngang phan tren.
     *
     * Khong co no thi con lam mot xap bai, duoc it phut hon minh cho, va khong biet
     * vi sao. Cai "khong biet vi sao" do moi la thu lam con thoi co gang - chu
     * khong phai so phut it.
     */
    private fun veKhongTinh() {
        val box = binding.boxKhongTinh
        box.removeAllViews()
        listOf(
            "Câu làm sai thì chưa được tính. Sửa lại rồi chụp nộp lại thì mới tính.",
            "Mỗi câu chỉ tính giờ một lần. Nộp lại câu đã được tính rồi thì không cộng nữa.",
            "Ảnh không thấy đề bài thì máy không chấm được, nên câu đó không tính."
        ).forEach { themDongChu(box, it) }
    }

    private fun veChan() {
        val ngu = TinhLoiNhac.gioPhut(prefs.hardStopMinuteOfDay)
        binding.txtChan.text =
            "${getString(R.string.parent_name_cap)} cho thêm giờ, việc nhà và quỹ giờ chơi thì " +
                "không tính vào ${LuatCongGio.TRAN_NGAY} phút này.\n" +
                "Giờ chơi phải xài trước $ngu, tới giờ đó là máy khoá."
    }

    // ----------------------------------------------------------------- dung chung

    /**
     * Huy hieu canh ten mot phan: hom nay phan do cong duoc bao nhieu roi.
     *
     * Bon the, va tung the tra loi mot cau khac nhau:
     *
     *  - da cong va van con cho: xanh, day la cai con muon thay;
     *  - da cong va het phan: cam, con so van duoc khoe nhung kem cau "het roi" de
     *    con thoi ngoi lam them o cho khong ra gi nua;
     *  - chua cong ma het phan: cam, hiem - chi xay ra khi tran rieng bi mot nguon
     *    khac an mat;
     *  - chua cong gi: xam, va chu la [khiTrong] de moi phan tu noi cai cua no.
     *    "Hom nay chua cong" hop cho bai tap, nhung dat vao the viec nha thi thanh
     *    trach con mot viec chi ba noi moi bat dau duoc.
     */
    private fun veNhan(
        o: TextView,
        phut: Int,
        het: Boolean,
        khiTrong: String = "Hôm nay chưa cộng"
    ) {
        val chu: String
        val mau: Int
        val nen: Int
        when {
            phut > 0 && het -> {
                chu = "Đã cộng $phut phút, hết phần này"
                mau = R.color.wait
                nen = R.color.wait_soft
            }
            phut > 0 -> {
                chu = "Hôm nay đã cộng $phut phút"
                mau = R.color.ok
                nen = R.color.ok_soft
            }
            het -> {
                chu = "Hôm nay hết phần này rồi"
                mau = R.color.wait
                nen = R.color.wait_soft
            }
            else -> {
                chu = khiTrong
                mau = R.color.ink_soft
                nen = R.color.canvas
            }
        }
        o.text = chu
        o.setTextColor(ContextCompat.getColor(this, mau))
        o.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, nen))
    }

    /**
     * Mot dong trong bang gia.
     *
     * @param gia so phut TOI DA cua muc nay, khong phai cach tinh. Cach tinh de o
     *   [phu].
     * @param giaPhu don vi cua [gia]: "moi cau", "moi bai", "moi ngay". Bo trong thi
     *   khong hien dong nao - chi dung khi con so da tu no ro nghia.
     * @param nay tinh trang HOM NAY cua rieng muc nay, vi du goi da tinh roi hay
     *   dang co may cau cho sua. Chi nhan cai doi theo ngay; luat thi khong viet o
     *   day, xem chu thich dau item_kiem_gio.xml.
     */
    private fun themDong(
        box: LinearLayout,
        ten: String,
        gia: String,
        giaPhu: String = "",
        nay: String = "",
        mauNay: Int = R.color.ink_soft
    ) {
        if (box.childCount > 0) box.addView(vach())
        val v = ItemKiemGioBinding.inflate(layoutInflater, box, false)
        v.ten.text = ten
        v.gia.text = gia
        v.giaPhu.text = giaPhu
        v.giaPhu.visibility = if (giaPhu.isBlank()) View.GONE else View.VISIBLE
        v.nay.text = nay
        v.nay.visibility = if (nay.isBlank()) View.GONE else View.VISIBLE
        v.nay.setTextColor(ContextCompat.getColor(this, mauNay))
        box.addView(v.root)
    }

    /** Mot dong trong the "Cai khong tinh gio": khong co gia, chi co cau noi. */
    private fun themDongChu(box: LinearLayout, chu: String) {
        if (box.childCount > 0) box.addView(vach())
        box.addView(TextView(this).apply {
            text = chu
            textSize = 15f
            setLineSpacing(3f.px(), 1f)
            setTextColor(ContextCompat.getColor(this@CachKiemGioActivity, R.color.ink))
            setPadding(0, 12.dp(), 0, 12.dp())
        })
    }

    /**
     * Dong ghi chu duoi mot the: con bao nhieu cua cai tran rieng.
     *
     * Het tran thi doi nen sang mau "luu y". Van la mot cau chu nhu cu, nhung luc
     * do no tra loi mot cau hoi khac han - vi sao lam tiep ma khong duoc them phut
     * nao - nen no phai bat mat hon mot dong ghi chu.
     */
    private fun ghiChu(o: TextView, het: Boolean, chu: String) {
        o.text = chu
        o.setBackgroundResource(if (het) R.drawable.st_nen_luu_y else R.drawable.bg_ghi_chu)
        // Dat lai le trong: doi nen la View lay le trong cua drawable moi, ma hai
        // hinh nen o day deu khong khai le nao.
        o.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
    }

    private fun vach(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        setBackgroundColor(ContextCompat.getColor(this@CachKiemGioActivity, R.color.line))
    }

    /** Nua dem hom nay, y het moc ma so cai bai va kho the dung de dem theo ngay. */
    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun Float.px(): Float = this * resources.displayMetrics.density
}
