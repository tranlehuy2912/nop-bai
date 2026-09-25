package vn.huytl.homeworkgate.ui

import android.content.res.ColorStateList
import android.os.Bundle
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
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.LuatCongGio
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
 * [Prefs.tranPhutMoiNgay]. Go tay lai vao day thi den luc Ba Huy sua mot con so
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
     * Chep y he thanh ba doan ngoai man chinh - da choi, dang giu, con lai - chu
     * khong ve mot kieu khac: cung mot con so ma hai man hinh ve hai kieu thi con
     * phai hoc hai lan, va lan nao cung phai doi chieu xem co khop nhau khong.
     */
    private fun veHomNay() {
        val tran = prefs.tranPhutMoiNgay
        val kiem = gate.phutDaDuyetHomNay()
        val con = gate.phutConLaiHomNay()

        val giuMs = when {
            gate.isOpen() -> gate.remainingMs()
            gate.state == GateState.PAUSED -> gate.pausedMs()
            gate.grantedMinutes > 0 -> gate.grantedMinutes * 60_000L
            else -> 0L
        }
        val giu = ((giuMs + 59_999L) / 60_000L).toInt().coerceAtMost(kiem)
        val daChoi = (kiem - giu).coerceAtLeast(0)

        binding.txtHomNay.text = "Hôm nay kiếm được $kiem/$tran phút"
        binding.phanDaChoi.setBackgroundColor(ContextCompat.getColor(this, R.color.ok))
        binding.phanDangGiu.setBackgroundColor(ContextCompat.getColor(this, R.color.brand))
        (binding.phanDaChoi.layoutParams as LinearLayout.LayoutParams).weight = daChoi.toFloat()
        (binding.phanDangGiu.layoutParams as LinearLayout.LayoutParams).weight = giu.toFloat()
        binding.khungThanh.requestLayout()

        binding.txtHomNayPhu.text = buildString {
            if (con > 0) {
                append("Làm bài nữa thì hôm nay còn kiếm thêm được tối đa $con phút.")
            } else {
                append("Hôm nay đủ giờ rồi, mai nộp bài tiếp nhé.")
            }
            if (giu > 0) append(" Đang giữ $giu phút chưa chơi.")
        }
    }

    // ----------------------------------------------------------- nop bai tap

    /**
     * Duong bai tap, duong ra nhieu gio nhat nen dat dau tien.
     *
     * Hai nhom rieng nhau chu khong cong vao nhau, va man nay phai noi ro cho do:
     * lam het bai co giao la mot cuc tron goi, con lai la tinh le tung cau. Con
     * doc nham thanh cong ca hai thi no trong cho mot so phut khong bao gio ra.
     */
    private fun veBaiTap() {
        val box = binding.boxBaiTap
        box.removeAllViews()

        val goiDaCo = SoCaiBai.goiDaCoHomNay(this)
        themDong(
            box,
            ten = "Làm hết bài cô giao",
            gia = "${LuatCongGio.PHUT_TRON_GOI_DAN_DO} phút",
            giaPhu = "mỗi ngày một lần",
            nay = if (goiDaCo) "Hôm nay đã cộng ${LuatCongGio.PHUT_TRON_GOI_DAN_DO} phút"
            else "Hôm nay chưa cộng",
            mauNay = if (goiDaCo) R.color.ok else R.color.ink_soft
        )

        /*
         * Cau dang cho sua la gio dang nam san tren ban.
         *
         * No chua duoc tra lan nao - cau sai tra 0 - nen sua xong nop lai la an dung
         * gia goc cua cau do. Chi hien khi that su dang no: khong no gi ma van bay ra
         * mot dong "sua cau sai" thi no la mot loi trach chung chung.
         */
        val canSua = SoCaiBai.dangChoSua(this)
        if (canSua.isNotEmpty()) {
            themDong(
                box,
                ten = "Sửa câu đã làm sai",
                gia = "tối đa ${LuatCongGio.TRAN_MOT_BAI_TAP} phút",
                giaPhu = "mỗi câu",
                nay = "Đang có ${canSua.size} câu chờ sửa: " +
                    canSua.take(3).joinToString(", ") { it.ma } +
                    if (canSua.size > 3) "…" else "",
                mauNay = R.color.alert
            )
        }

        themDong(
            box,
            ten = "Bài tập làm thêm",
            gia = "tối đa ${LuatCongGio.TRAN_MOT_BAI_TAP} phút",
            giaPhu = "mỗi câu"
        )

        themDong(
            box,
            ten = "Bài văn, đoạn văn dài",
            gia = "tối đa ${LuatCongGio.TRAN_MOT_BAI_VIET_DAI} phút",
            giaPhu = "mỗi bài"
        )

        themDong(
            box,
            ten = "Trắc nghiệm",
            gia = "tối đa ${LuatCongGio.TRAN_TRAC_NGHIEM} phút",
            giaPhu = "mỗi lần nộp"
        )

        val denHen = SoCaiBai.cacCauDangOn(this)
        val daOn = SoCaiBai.phutOnHomNay(this)
        val conOn = (LuatCongGio.TRAN_ON_MOI_NGAY - daOn).coerceAtLeast(0)
        themDong(
            box,
            ten = "Ôn lại câu đến hẹn",
            /*
             * TRAN NGAY, khong phai gia mot cau.
             *
             * Duong nay co ca hai con so - moi cau duoc nua gia, va ca ngay nhieu
             * nhat [LuatCongGio.TRAN_ON_MOI_NGAY] - nhung cot phai chi treo duoc mot.
             * Treo con so ngay, vi do la con so con thuc su can: no dang doi xem toi
             * nay ngoi on thi duoc them bao nhieu gio choi, chu khong phai mot cau
             * le dang bao nhieu.
             *
             * Cac dong khac trong the nay van treo gia le ("mỗi câu", "mỗi bài") vi
             * chung khong co tran ngay rieng: ca nhom dung chung tran
             * [LuatCongGio.TRAN_LAM_THEM], va cho noi con so do la dong ghi chu cuoi
             * the. Treo 90 vao tung dong thi thanh noi moi dong duoc 90.
             */
            gia = "tối đa ${LuatCongGio.TRAN_ON_MOI_NGAY} phút",
            giaPhu = "mỗi ngày",
            // Den hen hay khong la viec cua may, con khong ep duoc. Noi ra de con
            // biet hom nay o day co gi de lam hay khong. Het tran thi cau do thanh
            // thua: co den hen cung khong ra phut nao nua, va do moi la thu phai noi.
            nay = when {
                conOn <= 0 -> "Hôm nay ôn đủ ${LuatCongGio.TRAN_ON_MOI_NGAY} phút rồi"
                denHen.isEmpty() -> "Hôm nay chưa có câu nào đến hẹn"
                daOn > 0 -> "Đang có ${denHen.size} câu đến hẹn, còn $conOn phút"
                else -> "Đang có ${denHen.size} câu đến hẹn"
            },
            mauNay = if (denHen.isEmpty() || conOn <= 0) R.color.ink_soft else R.color.brand
        )

        val daLamThem = SoCaiBai.phutLamThemHomNay(this)
        val conLamThem = (LuatCongGio.TRAN_LAM_THEM - daLamThem).coerceAtLeast(0)
        /*
         * Tran lam them CHI SONG VAO NHUNG HOM DA TINH GOI - xem bien conTran trong
         * [LuatCongGio.tinh]. Hom nao khong co goi thi phan tinh le chinh la bai co
         * giao, chan no lai la phat con vi lam nhieu, nen hom do chi con tran chung
         * cua ngay.
         *
         * Viet hai cau khac nhau cho hai hom chu khong mot cau chung. Cau chung thi
         * phai noi "neu... thi...", ma mot dua tre doc cau dieu kien ve mot con so
         * no chua gap bao gio se hieu thanh "hom nay toi da 90" - tuc la tu bo dung
         * luc dang con cho.
         */
        val goiChanTran = goiDaCo && conLamThem <= 0
        /*
         * Huy hieu dem CA THE: goi cong bai le, tuc la [SoCaiBai.phutDaCongHomNay].
         *
         * So nay chi gom duong bai tap that: the hoc ghi sang bang tra_the, viec nha
         * va gio Ba Huy cho thi khong di qua so cai bai bao gio. Nen no khong phai
         * mot lat cat cua con so o the tren cung, no la tong cua dung the nay.
         */
        veNhan(binding.nhanBaiTap, SoCaiBai.phutDaCongHomNay(this), het = goiChanTran)
        ghiChu(
            binding.txtBaiTapChan,
            het = goiChanTran,
            chu = if (goiDaCo) {
                "Hôm nay đã tính gói ${LuatCongGio.PHUT_TRON_GOI_DAN_DO} phút, nên mọi " +
                    "thứ ngoài gói cộng lại nhiều nhất ${LuatCongGio.TRAN_LAM_THEM} phút. " +
                    when {
                        conLamThem <= 0 -> "Hôm nay hết phần này rồi."
                        daLamThem > 0 -> "Đã được $daLamThem phút, còn $conLamThem phút."
                        else -> "Chưa dùng phút nào."
                    }
            } else {
                "Hôm nào tính gói ${LuatCongGio.PHUT_TRON_GOI_DAN_DO} phút thì mọi thứ " +
                    "ngoài gói cộng lại nhiều nhất ${LuatCongGio.TRAN_LAM_THEM} phút. " +
                    "Hôm nay chưa tính gói, nên chỉ còn tối đa ${prefs.tranPhutMoiNgay} " +
                    "phút của cả ngày."
            }
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
            ten = "Dò từ vựng",
            gia = "tối đa $tranTu phút",
            giaPhu = "mỗi ngày",
            nay = when {
                !coBoTu -> "Máy chưa có bộ từ nào"
                daTu >= tranTu -> "Hôm nay dò đủ $tranTu phút rồi"
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
     * Ba giao thi moi co, ba khong giao thi khong co gi de lam - nen o day "chua
     * cong" khong co nghia la "con chua lam". Huy hieu phai noi dung the: dang co
     * viec cho thi ke ra con bao nhieu phut, khong co viec thi noi thang la hom nay
     * ba chua giao, de con thoi doi o cho khong the co gi.
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
            ten = "Làm xong việc bà giao",
            gia = "bà đặt số phút",
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
                else -> "Hôm nay bà chưa giao việc"
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
            "Ảnh không thấy đề bài thì máy không chấm được, nên câu đó không tính.",
            "Vở dặn dò của hôm khác thì không tính gói " +
                "${LuatCongGio.PHUT_TRON_GOI_DAN_DO} phút.",
            "Vở dặn dò hôm đó cô chỉ dặn việc, không giao bài tập nào, thì máy không " +
                "tự cộng gói. Bài làm hôm đó tính lẻ từng câu, còn gói thì ba Huy xem " +
                "vở rồi quyết."
        ).forEach { themDongChu(box, it) }
    }

    private fun veChan() {
        val ngu = TinhLoiNhac.gioPhut(prefs.hardStopMinuteOfDay)
        binding.txtChan.text =
            "${getString(R.string.parent_name_cap)} hoặc bà nội cho thêm giờ thì không " +
                "tính vào ${prefs.tranPhutMoiNgay} phút này.\n" +
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
