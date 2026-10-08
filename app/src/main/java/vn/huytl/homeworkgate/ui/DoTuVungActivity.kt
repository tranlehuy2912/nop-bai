package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.PhimKiemTra
import vn.huytl.homeworkgate.databinding.StActivityDoTuBinding
import vn.huytl.homeworkgate.databinding.StTheBoBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.BuoiDo
import vn.huytl.homeworkgate.kho.Chieu
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.kho.TraTu
import vn.huytl.homeworkgate.kho.TuVung
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier
import java.util.Calendar

/**
 * Kiem tra tu vung (ten tren man truoc 2/10/2026 la "Dò từ vựng"): may hoi mot tu, con tra loi
 * thang tren tablet, may cham ngay.
 *
 * TRA LOI BANG PHIM GHEP tu 2/10/2026, nhu bai lam tren may va man kiem tra bai: chieu nhin tu
 * la bon nut nghia A, B, C, D, chieu nhin nghia la moi chu cai mot o va du 26 phim. Khung ve o
 * [KhungGhep], khoi ghep va bac goi y o [PhimKiemTra]. Truoc ngay do chieu nhin nghia la mot o
 * go ban phim Android, goi y la mot dong chu ("Gợi ý: b…").
 *
 * TACH KHOI [HocThuocActivity] du hai man chay cung mot vong - hoi, cham, sai thi
 * hoi lai, xong thi cap gio. Ly do o hai cho khong gop duoc:
 *
 *  - the hoc thuoc giu mot cap hoi/dap co dinh va den luot theo lich hen 3/10/20/30
 *    ngay; mot tu thi hoi duoc CA HAI CHIEU va duoc boc theo trong so moi buoi;
 *  - chieu nhin tu doan nghia la mot cau trac nghiem bon o, khong phai o go chu.
 *
 * Gop lai thi man kia phai nhan hai loai cau hoi, hai cach chon, hai cach cham -
 * ba cai if long trong mot vong dang chay tot moi ngay. Cai PHAI dung chung, va
 * dang dung chung, la [LuatTuVung] voi [vn.huytl.homeworkgate.data.HocThuoc.chuanHoa], va
 * tu 2/10/2026 ca [PhimKiemTra]: moi luat deu nam trong do, hai man chi la hai cai mieng noi
 * vao cung mot kho.
 *
 * CA BUOI GIU TRONG BO NHO, GHI MOT LAN LUC CHOT - y het man kia, va cung mot ly
 * do: may giet app giua buoi thi con lam lai tu dau, khong mat gi va cung khong
 * duoc tra gio hai lan.
 *
 * MOI TU MOT SAO (Ba Huy chot 2/10/2026, cho cung luat voi bai lam tren may: 1 sao la 1 phut).
 * Sao o goc phai dong tien do, ba dang cua [HangSao]: rong khi con duoc phut, vang khi vua lam
 * dung, xam khi goi y da lo dap an. Sai roi lam dung van duoc sao (luat phut khong doi: chi lo
 * dap an moi mat). Lam xong mot tu thi canh chu ket qua hien "+1 ★", lo dap an hay chiu thi
 * "+0 ★". Phut van cong gop luc chot buoi ([chot]), qua tran ngay thi vao quy gio choi.
 *
 * MAY DOC TU, CON NGHE XONG MOI TRA LOI DUOC (Ba Huy chot 8/10/2026). Cau nao hien len may
 * cung doc ca tu bang giong Anh ([GiongDoc]), o ca hai chieu: chieu nhin nghia thanh bai
 * nghe roi ghep, nhu viet chinh ta, va van 1 phut moi tu. Trong luc doc, khung phim va cac
 * nut tra loi bi khoa ([khoaChoDoc]), bam nhanh khong bo qua duoc. Nut loa nghe lai bao
 * nhieu lan cung duoc va khong khoa. May tat tieng thi van "doc" cho het luot roi mo (Ba Huy
 * chon cach de lam); may khong doc duoc thi khong khoa, man chay nhu truoc ngay do.
 */
class DoTuVungActivity : AppCompatActivity() {

    private lateinit var b: StActivityDoTuBinding
    private lateinit var gate: GateStore

    private var boDangLam: BoTuVung.Bo? = null

    /** Ma phien, de [KhoBai.tinhTrangTu] dem duoc tu nao da xong trong MOT buoi. */
    private var phien = ""

    /**
     * Mot tu trong buoi nay, kem nhung gi da xay ra voi no.
     *
     * [moiNhu] chon mot lan luc bat dau buoi chu khong chon lai moi lan hien: hoi
     * lai cung mot tu ma bon o doi khac di thi con phai doc lai ca bon, trong khi
     * cai dang kiem tra la con nho nghia hay khong.
     */
    private class MucHoi(
        val tu: TuVung,
        val chieu: Chieu,
        val moiNhu: List<String>,
        /** Hat tron cua tu trong buoi: hoi lai tu nay thi phim mo dung cho lan truoc. */
        val hat: Int
    ) {
        var soDung = 0
        var soSai = 0
        var chiu = false

        /** So lan da thu, de ghi [TraTu.lan]. */
        var lan = 0

        /** O chu va 26 phim cua chieu nhin nghia viet tu, xem [PhimKiemTra.ghepTu]. */
        val khoiTu: PhimKiemTra.Khoi? = if (chieu == Chieu.VIET_ANH) PhimKiemTra.ghepTu(tu.tu) else null

        /** Cac nghia da chon sai o chieu nhin tu, ve mo lan hoi lai - xem [KhungGhep.saiDaChon]. */
        val saiNghia = mutableListOf<String>()

        /** Go dung mot lan la xong - xem [LuatTuVung.LAN_DUNG_DE_TINH]. */
        val xong get() = soDung >= LuatTuVung.LAN_DUNG_DE_TINH

        /**
         * Goi y da lo dap an: van phai lam dung moi xong, nhung khong duoc cong gio.
         *
         * Chieu viet tu theo bac goi y cua [PhimKiemTra] (mo phim sai, dien san nua dau, hien
         * het). Chieu chon nghia (Ba Huy chot 2/10/2026): nghia nao da chon sai thi mo, nen chon
         * sai toi khi chi con mot nghia sang (lan thu ba voi bon nghia) la lo. Truoc ngay do
         * chieu nay khong bao gio lo, va chi noi tu o Unit nao.
         */
        val loHet get() = when (chieu) {
            Chieu.VIET_ANH -> khoiTu?.loHet(soSai) { LuatTuVung.dung(it, tu.tu) } == true
            else -> moiNhu.size - saiNghia.size <= 1
        }

        /** Xong ma khong phai doc dap an tren man: tu duoc tinh gio. */
        val duocGio get() = xong && !loHet
    }

    private val muc = LinkedHashMap<String, MucHoi>()

    /** Hang hoi: ma tu theo dung thu tu se hoi. Mot ma nam nhieu lan la co che. */
    private val hang = mutableListOf<String>()
    private var viTri = 0

    private var daTraLoi = false

    /** Khung phim ghep cua tu dang hoi, ve lai moi lan hien mot tu. */
    private var khung: KhungGhep? = null

    private val ketQua = mutableListOf<TraTu>()

    /** So dong trong [ketQua] da ghi xuong so. Moi lan chot chi ghi va tra phan moi. */
    private var daGhi = 0

    /** Tu da xong va da tra gio trong buoi nay. */
    private val tuDaTra = HashSet<String>()

    /** Giong doc tu, tao o [onCreate], tra o [onDestroy]. */
    private var giong: GiongDoc? = null

    /**
     * Cau dang hoi da nghe may doc het tu chua. Chua thi khung phim va nut tra loi dang khoa:
     * Ba Huy chot 8/10/2026 cau nao cung phai nghe het tu roi moi chon dap an hay sang tu sau.
     */
    private var daNgheCauNay = true

    /** Dem luot doc co khoa, de bao xong cua luot cu khong mo khoa nham cau moi. */
    private var luotDoc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = StActivityDoTuBinding.inflate(layoutInflater)
        setContentView(b.root)
        gate = GateStore(this)

        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top + 28.dp(), bottom = bars.bottom + 28.dp())
            insets
        }

        b.btnThoat.setOnClickListener { veLui() }
        b.btnChinh.setOnClickListener { if (daTraLoi) sangTuSau() else traLoiGo() }
        b.btnChiu.setOnClickListener { chiuThoi() }
        b.btnLoa.setOnClickListener { ngheLai() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = veLui()
        })
        // Tao truoc khi hien cau dau: bo doc mat vai tram mili giay de khoi dong.
        giong = GiongDoc(this) { veNutLoa() }
        veChonBo()
    }

    /**
     * Lui mot bac, y het man hoc thuoc: dang do tu hay dang xem ket qua buoi thi ve man
     * chon bo, o man chon bo moi ra man chinh. Chot buoi truoc khi ve, vi o lai trong
     * man nay thi khong ai goi [onPause] de chot ho.
     */
    private fun veLui() {
        if (b.boxBo.visibility == View.VISIBLE) return finish()
        chot()
        veChonBo()
    }

    override fun onPause() {
        chot()
        // Ngung doc nhung GIU khoa neu lan doc dau chua het: [onResume] doc lai.
        giong?.dung()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Roi man giua luc may dang doc (tat man hinh, ra man chinh) thi quay lai doc lai tu
        // dang hoi roi moi mo khoa. Khong thi thoat ra vao lai la bo qua duoc tieng doc.
        if (!daNgheCauNay && b.boxHoi.visibility == View.VISIBLE) docCauDangHoi()
    }

    override fun onDestroy() {
        giong?.tat()
        giong = null
        super.onDestroy()
    }

    // ------------------------------------------------------------- chon bo tu

    private fun veChonBo() {
        ngungDoc()
        val kho = KhoBai.get(this)
        b.boxBo.visibility = View.VISIBLE
        b.boxHoi.visibility = View.GONE
        b.theXong.visibility = View.GONE
        b.boxBo.removeAllViews()
        b.txtTieuDe.setText(R.string.do_tu_title)

        val cac = BoTuVung.BO.filter { kho.soTuCua(it.bo) > 0 }
        if (cac.isEmpty()) {
            b.txtTrong.visibility = View.VISIBLE
            b.txtTrong.setText(R.string.do_tu_chua_co_bo)
            return
        }
        b.txtTrong.visibility = View.GONE
        b.txtChan.setText(R.string.do_tu_chan)

        val conGiay = LuatTuVung.GIAY_TRAN_MOI_NGAY - kho.giayTuVungTu(moc0Gio())
        cac.forEach { bo -> b.boxBo.addView(theBo(bo, conGiay > 0)) }
    }

    /**
     * Mot the bo tren man chon.
     *
     * Het tran ngay thi the xam lai nhung VAN o day, y het man kiem tra bai: con
     * nhin thay minh da thuoc toi dau, va biet mai no quay lai.
     *
     * Bo chua chon Unit thi bam vao la hoi "lop da hoc nhung Unit nao" truoc, y het man
     * kiem tra bai, xem [HocToi]. Dong cuoi cua the noi cac Unit da danh dau, kem nut doi
     * bam duoc ca khi the dang xam.
     */
    private fun theBo(bo: BoTuVung.Bo, conGio: Boolean): View {
        val kho = KhoBai.get(this)
        val daHoc = PhanHoc.baiDaHoc(this, bo.mon)
        val caBo = kho.tinhTrangTu(bo.bo, LuatTuVung.LAN_DUNG_DE_TINH)
        // Da chon Unit thi thanh tien do tinh tren phan lop da hoc, nhu truoc khi co cho
        // chon. Chua chon hay chua hoc Unit nao thi phan do rong, va "Đã thuộc 0/0 từ"
        // doc nhu may hong, nen luc do tinh tren ca bo.
        val tinh = if (!daHoc.isNullOrEmpty()) daMo(daHoc, caBo) else caBo
        val thuoc = tinh.count { it.second == LuatTuVung.TinhTrang.DA_THUOC }
        val chuaChon = daHoc == null
        val chuaHoc = daHoc?.isEmpty() == true
        // Het tran ngay thi the VAN bam duoc (29/9/2026): phut do tu dung them tu do vao
        // "Quỹ giờ chơi", xem [chot]. Truoc do the xam lai va con khong do them duoc, nen
        // phan cong con muon lam them khong duoc ghi o dau ca.
        val sang = !chuaHoc

        val v = StTheBoBinding.inflate(layoutInflater, b.boxBo, false)
        val mauMon = ContextCompat.getColor(this, MatMon.mau(bo.mon))

        v.huyHieu.text = MatMon.tat(bo.mon)
        v.huyHieu.setTextColor(if (sang) mauMon else mau(R.color.ink_soft))
        v.huyHieu.backgroundTintList = ContextCompat.getColorStateList(
            this, if (sang) MatMon.nen(bo.mon) else R.color.canvas
        )

        v.tenBo.text = bo.ten
        v.tenBo.setTextColor(mau(if (sang) R.color.ink else R.color.ink_soft))

        v.phuBo.text = when {
            chuaChon -> "Chọn Unit lớp đã học"
            !conGio && !chuaHoc -> "Đủ phút hôm nay, kiểm tra thêm vẫn được"
            // Dong cuoi da noi lop chua hoc Unit nao, o day noi he qua cua no.
            chuaHoc -> "Chưa có từ để hỏi"
            else -> "từ mỗi buổi, trong ${HocToi.moTa(bo.mon, daHoc.orEmpty())}"
        }
        v.phuBo.setTextColor(
            when {
                !conGio -> mau(R.color.ok)
                sang -> mauMon
                else -> mau(R.color.ink_soft)
            }
        )

        v.soDenLuot.text = when {
            chuaChon -> "?"
            chuaHoc -> "–"
            // Phan da hoc it hon mot buoi thi hoi het phan do, khong phai hai muoi.
            else -> minOf(LuatTuVung.SO_TU_HANG_NGAY, tinh.size).toString()
        }
        v.soDenLuot.setTextColor(
            when {
                !conGio -> mau(R.color.ok)
                sang -> mauMon
                else -> mau(R.color.ink_soft)
            }
        )

        v.thanhThuoc.max = tinh.size.coerceAtLeast(1)
        v.thanhThuoc.setProgressCompat(thuoc, false)
        v.thanhThuoc.setIndicatorColor(if (sang) mauMon else mau(R.color.ok))
        v.chuThuoc.text = "Đã thuộc $thuoc/${tinh.size} từ"

        v.chuHocToi.text = daHoc?.let { "Bài đã học: ${HocToi.moTa(bo.mon, it)}" }
            ?: "Bài đã học: chưa chọn"
        v.btnHocToi.text = if (chuaChon) "Chọn" else "Đổi"
        v.btnHocToi.setOnClickListener { hoiHocToi(bo, roiBatDau = chuaChon) }

        v.root.isEnabled = sang
        v.root.alpha = if (sang) 1f else 0.7f
        if (sang) {
            v.root.setOnClickListener {
                if (chuaChon) hoiHocToi(bo, roiBatDau = true) else batDau(bo)
            }
        }
        return v.root
    }

    /**
     * Hoi cac Unit lop da hoc (hop chon nhieu o, [ChonHocToi.hoiMon], dung chung voi khu Tieng
     * Anh o trang Luyen tap), ghi lai, roi ve lai man chon bo.
     *
     * [roiBatDau] y het ben man kiem tra bai: con bam vao bo chua chon de lam, nen chon
     * xong la vao buoi luon.
     */
    private fun hoiHocToi(bo: BoTuVung.Bo, roiBatDau: Boolean) {
        ChonHocToi.hoiMon(this, bo.mon) {
            val daHoc = PhanHoc.baiDaHoc(this, bo.mon)
            if (roiBatDau && !daHoc.isNullOrEmpty()) batDau(bo) else veChonBo()
        }
    }

    private fun mau(id: Int) = ContextCompat.getColor(this, id)

    // ------------------------------------------------------------------- buoi

    /**
     * Chon tu cho mot buoi: ghim tu vua sai truoc, phan con lai boc theo trong so.
     *
     * Ghim cung chu khong de ngau nhien lo: trong so co cao den may cung khong bao
     * dam mot tu vua sai hom qua quay lai hom nay, ma dung luc con vua sai moi la
     * luc hoi lai co ich nhat. Xem [LuatTuVung.SO_GHIM_TU_SAI].
     */
    private fun batDau(bo: BoTuVung.Bo) {
        val kho = KhoBai.get(this)
        // Bo chua chon Unit thi hoi truoc, du vao tu duong nao.
        val daHoc = PhanHoc.baiDaHoc(this, bo.mon) ?: return hoiHocToi(bo, roiBatDau = true)
        val tinh = daMo(daHoc, kho.tinhTrangTu(bo.bo, LuatTuVung.LAN_DUNG_DE_TINH))
        if (tinh.isEmpty()) return veChonBo()

        val ghim = tinh.filter { it.second == LuatTuVung.TinhTrang.VUA_SAI }
            .take(LuatTuVung.SO_GHIM_TU_SAI)
        val daGhim = ghim.map { it.first.id }.toSet()
        val boc = LuatTuVung.bocTheoTrongSo(
            tinh.filterNot { it.first.id in daGhim }
                .map { it to LuatTuVung.trongSo(it.second) },
            LuatTuVung.SO_TU_HANG_NGAY - ghim.size
        )
        val chon = (ghim + boc).shuffled()
        if (chon.isEmpty()) return veChonBo()

        // Moi nhu lay tu CA quyen chu khong chi cac Unit da mo: mot nghia sai chi de
        // loai tru, nhin thay no khong day gi ca va cung khong lam con roi. Dau nam
        // chi co mot Unit, bo lai trong do thi bon o toan tu cung mot trang sach.
        val tatCa = kho.cacTuCua(bo.bo)
        boDangLam = bo
        phien = "tv-${System.currentTimeMillis()}"
        viTri = 0
        ketQua.clear()
        daGhi = 0
        tuDaTra.clear()
        phutVuaTra = 0
        muc.clear()
        hang.clear()
        chon.forEach { (tu, _, soPhienXong) ->
            val chieu = LuatTuVung.chieuCho(soPhienXong)
            val moiNhu = if (chieu == Chieu.ANH_VIET) moiNhuCho(tu, tatCa) else emptyList()
            muc[tu.id] = MucHoi(tu, chieu, moiNhu, hat = "$phien/${tu.id}".hashCode())
            hang += tu.id
        }

        b.txtTieuDe.text = bo.ten
        b.boxBo.visibility = View.GONE
        b.txtTrong.visibility = View.GONE
        b.theXong.visibility = View.GONE
        b.boxHoi.visibility = View.VISIBLE
        b.txtChan.text = ""
        veTu()
    }

    /**
     * Bo cac tu cua Unit ma lop chua hoc toi, theo Unit con chon. Luat o [LuatTuVung.daHoc].
     *
     * Khong con tra lai ca bo khi loc ra rong nhu luc con doan theo lich. Hoi do doan
     * nham thi tha hoi rong con hon mo ra mot man trong; gio rong chi con nghia la con
     * chon "chua hoc Unit nao", va hoi rong la hoi dung thu con vua noi chua hoc.
     */
    private fun daMo(
        daHoc: Set<Int>,
        cac: List<Triple<TuVung, LuatTuVung.TinhTrang, Int>>
    ): List<Triple<TuVung, LuatTuVung.TinhTrang, Int>> =
        cac.filter { LuatTuVung.daHoc(it.first.unit, daHoc) }

    /** Ba ro moi nhu, uu tien giam dan - xem [LuatTuVung.chonMoiNhu]. */
    private fun moiNhuCho(tu: TuVung, tatCa: List<TuVung>): List<String> {
        val khac = tatCa.filter {
            it.id != tu.id && LuatTuVung.moiNhuDuoc(tu.nghia, it.nghia)
        }
        return LuatTuVung.chonMoiNhu(
            dung = tu.nghia,
            cungUnitCungLoai = khac.filter { it.unit == tu.unit && it.loai == tu.loai }
                .map { it.nghia },
            cungLoai = khac.filter { it.loai == tu.loai }.map { it.nghia },
            cungUnit = khac.filter { it.unit == tu.unit }.map { it.nghia }
        )
    }

    private fun dangHoi(): MucHoi? = hang.getOrNull(viTri)?.let { muc[it] }

    private fun veTu() {
        val m = dangHoi() ?: return xongBuoi()
        daTraLoi = false

        // Dem theo SO TU da xong tren tong so tu, khong phai vi tri trong hang: hang
        // dai ra moi lan con go sai.
        val xong = muc.values.count { it.xong || it.chiu }
        b.txtTien.text = "Đã xong $xong / ${muc.size} từ"

        b.theKet.visibility = View.GONE
        b.btnChiu.visibility = View.GONE
        b.txtBao.visibility = View.GONE
        b.txtCong.visibility = View.GONE
        veSao(m)

        if (m.chieu == Chieu.ANH_VIET) veChieuNhinTu(m) else veChieuGoTu(m)

        // Da sai lan nao thi giu dong "Lần trước sai"; goi y nam tren ban phim, mo them moi lan sai.
        if (m.soSai > 0) {
            b.theKet.visibility = View.VISIBLE
            b.txtKet.text = if (m.loHet && m.chieu == Chieu.VIET_ANH) "Gõ lại cho đúng đáp án" else "Lần trước sai"
            b.txtKet.setTextColor(mau(R.color.alert))
            datDap(goiYSauSai(m))
            // Dap an da hien het thi bat go cho dung, khong cho bam chiu de bo qua.
            b.btnChiu.visibility = if (m.loHet) View.GONE else View.VISIBLE
        }

        // Ve xong moi doc: khoa phai de len khung phim vua ve. Tu bi hoi lai cung doc lai.
        veNutLoa()
        docCauDangHoi()
    }

    // ------------------------------------------------------------- giong doc

    /**
     * Doc tu cua cau dang hoi va khoa phan tra loi toi khi doc het. May khong doc duoc thi
     * khong khoa (Ba Huy chot 8/10/2026), man chay nhu truoc.
     */
    private fun docCauDangHoi() {
        val m = dangHoi() ?: return
        val g = giong
        if (g == null || !g.coTheDoc || daTraLoi) return moKhoaDoc()
        val luot = ++luotDoc
        daNgheCauNay = false
        khoaChoDoc()
        // Doc het, hay bo doc loi, treo: deu mo. Luot da bi luot sau de len thi thoi.
        g.doc(GiongDoc.chuDoc(m.tu.tu)) { if (luot == luotDoc) moKhoaDoc() }
    }

    /**
     * Nut loa: nghe lai bao nhieu lan cung duoc, khong khoa (Ba Huy chot 8/10/2026). Bam luc
     * lan doc dau chua het thi doc lai tu dau, va van khoa toi khi doc het lan nay.
     */
    private fun ngheLai() {
        val m = dangHoi() ?: return
        if (!daNgheCauNay) return docCauDangHoi()
        giong?.doc(GiongDoc.chuDoc(m.tu.tu))
    }

    /**
     * Khoa trong luc doc. Khung phim ghep tu bat tat tung phim (phim mo, phim da dung), nen
     * khong dung toi phim cua no ma phu mot lop chan bam len tren ([R.id.lop_khoa]) va lam mo
     * ca khung; hai nut duoi thi tat thang.
     */
    private fun khoaChoDoc() {
        b.lopKhoa.visibility = View.VISIBLE
        b.khung.alpha = DO_MO_KHI_DOC
        b.btnChinh.isEnabled = false
        b.btnChiu.isEnabled = false
        b.txtDoc.visibility = View.VISIBLE
    }

    private fun moKhoaDoc() {
        daNgheCauNay = true
        b.lopKhoa.visibility = View.GONE
        b.khung.alpha = 1f
        b.btnChinh.isEnabled = true
        b.btnChiu.isEnabled = true
        b.txtDoc.visibility = View.GONE
    }

    /** Ngung doc va bo khoa, khi roi phan hoi (ve man chon bo, xong buoi). */
    private fun ngungDoc() {
        giong?.dung()
        luotDoc++
        moKhoaDoc()
    }

    /** Nut loa chi hien khi may doc duoc; bo doc hong giua buoi thi an luon. */
    private fun veNutLoa() {
        b.btnLoa.visibility = if (giong?.coTheDoc == true) View.VISIBLE else View.GONE
    }

    /**
     * Dong duoi chu "Sai": Unit cua tu o chieu chon nghia, ca tu khi da hien het. Rong o chieu
     * viet tu: tu 2/10/2026 goi y nam tren ban phim (phim mo, chu dien san), khong con dong
     * "Gợi ý: 8 chữ cái".
     */
    private fun goiYSauSai(m: MucHoi): String = when {
        m.chieu == Chieu.ANH_VIET -> "Từ này ở Unit ${m.tu.unit}"
        m.loHet -> "Đáp án: ${m.tu.tu}"
        else -> ""
    }

    /** Sao cua tu dang hoi: rong khi con duoc phut, xam khi goi y da lo dap an. Xem [HangSao]. */
    private fun veSao(m: MucHoi) {
        b.txtSao.text = HangSao.chu(this, listOf(if (m.loHet) HangSao.O.MAT else HangSao.O.CON))
    }

    /** "+N ★" canh chu ket qua: vang nhu sao da duoc khi co sao, xam khi "+0 ★" ([HangSao.mauCong]). */
    private fun hienCong(n: Int) {
        b.txtCong.text = "+$n ★"
        b.txtCong.setTextColor(HangSao.mauCong(this, n))
        b.txtCong.visibility = View.VISIBLE
    }

    /** Dong duoi chu ket qua; rong thi an ca dong. */
    private fun datDap(chu: String) {
        b.txtDap.text = chu
        b.txtDap.visibility = if (chu.isBlank()) View.GONE else View.VISIBLE
    }

    private val mon get() = boDangLam?.mon.orEmpty()

    /**
     * Nhin tu tieng Anh, bam mot trong bon nghia: nut A, B, C, D cua phim ghep, nghia da chon
     * sai o lan truoc thi mo.
     */
    private fun veChieuNhinTu(m: MucHoi) {
        b.txtNhan.setText(R.string.do_tu_nhan_anh)
        b.txtHoi.text = m.tu.tu
        b.txtAm.text = m.tu.am
        b.txtAm.visibility = if (m.tu.am.isBlank()) View.GONE else View.VISIBLE

        val g = PhimKiemTra.ghepNghia(m.moiNhu, m.moiNhu.indexOf(m.tu.nghia))
        khung = KhungGhep(b.khung, g, mon, m.hat, saiDaChon = m.saiNghia) {
            // Bam thang vao nghia la tra loi luon, nhu truoc khi sang phim ghep.
            if (!daTraLoi) traLoiNghia(m)
        }.also { it.ve() }
        b.btnChinh.visibility = View.GONE
    }

    private fun traLoiNghia(m: MucHoi) {
        val i = khung?.daChon()?.firstOrNull() ?: return
        val nghia = m.moiNhu[i]
        val dung = nghia == m.tu.nghia
        if (!dung) m.saiNghia += KhungGhep.khoaChon(i)
        cham(m, nghia, dung)
    }

    /**
     * Nhin nghia tieng Viet, viet tu tieng Anh: moi chu cai mot o, du 26 phim. Phim sai mo dan
     * va nua dau tu dien san theo so lan sai, xem [PhimKiemTra].
     */
    private fun veChieuGoTu(m: MucHoi) {
        b.txtNhan.setText(R.string.do_tu_nhan_viet)
        b.txtHoi.text = m.tu.nghia
        b.txtAm.visibility = View.GONE

        val k = m.khoiTu ?: return
        khung = KhungGhep(b.khung, k.ghep, mon, m.hat, dienSan = k.dienSan(m.soSai)) {
            b.txtBao.visibility = View.GONE
        }.also { it.ve(PhimKiemTra.mucMo(m.soSai)) }
        b.btnChinh.visibility = View.VISIBLE
        b.btnChinh.setText(R.string.do_tu_tra_loi)
    }

    private fun traLoiGo() {
        val m = dangHoi() ?: return
        val k = khung ?: return
        if (!k.ghepXong()) {
            b.txtBao.text = "Gõ đủ ô trước đã"
            b.txtBao.visibility = View.VISIBLE
            return
        }
        val go = k.traLoi()
        cham(m, go, LuatTuVung.dung(go, m.tu.tu))
    }

    /**
     * Ghi mot lan thu va ve ket qua.
     *
     * DUNG THI HIEN DAP AN, ke ca lan dung dau: luc do khong con gi de gian nua, ma
     * con doi chieu duoc cach viet cua minh voi cach sach in.
     */
    private fun cham(m: MucHoi, go: String, dung: Boolean) {
        m.lan++
        // Go dung sau khi da doc dap an tren man thi ghi nhu bam chiu: buoi nay khong tinh
        // la buoi tu da xong, mai tu do van duoc ghim. Xem [TraTu.chiu].
        val docDapAn = dung && m.loHet
        ketQua += TraTu(
            tuId = m.tu.id,
            phien = phien,
            buoi = BuoiDo.HANG_NGAY,
            chieu = m.chieu,
            lan = m.lan,
            go = go,
            dung = dung && !docDapAn,
            goiY = m.soSai.coerceAtMost(PhimKiemTra.BAC_HIEN_HET),
            chiu = docDapAn,
            giay = 0
        )
        daTraLoi = true
        khung?.khoa(hienLoiGiai = false)
        b.txtBao.visibility = View.GONE
        b.theKet.visibility = View.VISIBLE
        b.btnChiu.visibility = View.GONE

        if (dung) {
            // Dung mot lan la xong, khong chen lai nua - xem [MucHoi.xong].
            m.soDung++
            b.txtKet.text = "Đúng rồi"
            b.txtKet.setTextColor(mau(R.color.ok))
            datDap(dapAn(m))
            val duoc = !docDapAn
            b.txtSao.text = HangSao.chu(this, listOf(if (duoc) HangSao.O.DUOC else HangSao.O.MAT))
            if (duoc) HangSao.nhip(b.txtSao)
            hienCong(if (duoc) SAO_MOI_TU else 0)
        } else {
            m.soSai++
            // Sai thi sao con nguyen (lam dung lan sau van duoc), tru khi lan sai nay lam lo dap an.
            veSao(m)
            // Chi mot chu "Sai", ke ca khi da hien het tu, nhu man kiem tra bai. Dong tong ket
            // cuoi buoi van ke so tu phai xem dap an.
            b.txtKet.text = "Sai"
            b.txtKet.setTextColor(mau(R.color.alert))
            datDap(goiYSauSai(m))
            // Day xuong cuoi hang: tu nao cung phai lam cho duoc, nhung khong ngoi
            // mai o mot tu.
            hang += m.tu.id
            b.btnChiu.visibility = if (m.loHet) View.GONE else View.VISIBLE
        }
        b.btnChinh.visibility = View.VISIBLE
        b.btnChinh.setText(nutTiep())
    }

    private fun dapAn(m: MucHoi): String =
        if (m.tu.am.isBlank()) "${m.tu.tu} — ${m.tu.nghia}"
        else "${m.tu.tu} ${m.tu.am} — ${m.tu.nghia}"

    /** Con chiu, khong ra duoc: hien dap an, bo tu nay khoi buoi. */
    private fun chiuThoi() {
        val m = dangHoi() ?: return
        m.chiu = true
        m.lan++
        // Ghi mot dong "chiu" xuong so. Khong ghi thi bang tinh trang coi nhu con
        // chua he gap tu nay, ma con da gap va da bi no.
        ketQua += TraTu(
            tuId = m.tu.id,
            phien = phien,
            buoi = BuoiDo.HANG_NGAY,
            chieu = m.chieu,
            lan = m.lan,
            go = "",
            dung = false,
            goiY = PhimKiemTra.BAC_HIEN_HET,
            chiu = true,
            giay = 0
        )
        for (i in hang.size - 1 downTo viTri + 1) if (hang[i] == m.tu.id) hang.removeAt(i)

        daTraLoi = true
        khung?.khoa(hienLoiGiai = false)
        b.txtBao.visibility = View.GONE
        b.btnChiu.visibility = View.GONE
        b.theKet.visibility = View.VISIBLE
        b.txtKet.text = "Từ này để mai gặp lại"
        b.txtKet.setTextColor(mau(R.color.ink_soft))
        datDap("Đáp án: ${dapAn(m)}")
        b.txtSao.text = HangSao.chu(this, listOf(HangSao.O.MAT))
        hienCong(0)
        b.btnChinh.visibility = View.VISIBLE
        b.btnChinh.setText(nutTiep())
    }

    private fun nutTiep(): Int =
        if (viTri + 1 < hang.size) R.string.do_tu_tiep else R.string.do_tu_xem_ket

    private fun sangTuSau() {
        viTri++
        veTu()
    }

    private fun xongBuoi() {
        ngungDoc()
        chot()
        val soXong = muc.values.count { it.xong }
        val soDuocGio = muc.values.count { it.duocGio }
        val soXemDapAn = muc.values.count { it.xong && it.loHet }
        val soChiu = muc.values.count { it.chiu }
        b.boxHoi.visibility = View.GONE
        b.theXong.visibility = View.VISIBLE
        b.txtXong.text = "$soXong / ${muc.size} từ xong"
        b.txtXongPhu.text = buildString {
            when {
                phutVuaTra > 0 -> append("Được thêm $phutVuaTra phút chơi.")
                soDuocGio == 0 -> append("Chưa được phút nào. Xem lại rồi làm tiếp nhé.")
                // Mot tu tron mot phut, nen co tu xong ma khong ra phut chi con mot
                // nghia: phan tu vung cua hom nay da day.
                else -> append(
                    "Mỗi từ xong được ${LuatTuVung.GIAY_MOI_TU / 60} phút, nhưng hôm nay " +
                        "đã kiểm tra đủ phần của ngày rồi."
                )
            }
            if (soXemDapAn > 0) {
                append(" $soXemDapAn từ phải xem đáp án nên không được giờ, mai gặp lại.")
            }
            if (soChiu > 0) append(" Còn $soChiu từ để mai gặp lại.")
        }
        b.txtChan.setText(R.string.do_tu_het_hom_nay)
        b.btnChinh.visibility = View.GONE
        b.btnChiu.visibility = View.GONE
    }

    // ------------------------------------------------------------------- chot

    private var phutVuaTra = 0

    /**
     * Ghi ca buoi xuong so va cap gio. Goi bao nhieu lan cung chi an mot lan.
     *
     * DEM THEO SO TU DA XONG, khong phai so lan go dung: mot tu phai dung
     * [LuatTuVung.LAN_DUNG_DE_TINH] lan moi tinh la xong. Tu phai doc dap an tren man thi
     * xong ma khong tra giay - xem [MucHoi.loHet].
     *
     * Giay gan vao TUNG TU da xong chu khong don het vao mot dong, vi [TraTu.giay]
     * la "so giay da tra cho tu nay" - dem le ra thi sau nay con hoi duoc tu nao da
     * tra bao nhieu. Ben the hoc thuoc don mot cuc vi cot ben do khong hua gi ca.
     */
    private fun chot() {
        // Chot nhieu lan trong mot buoi (tat man hinh giua chung roi lam tiep): moi lan
        // chi ghi dong moi va tra cho tu moi xong. Truoc day lan chot dau khoa ca buoi.
        if (ketQua.size <= daGhi) return
        val moi = ketQua.subList(daGhi, ketQua.size).toList()
        daGhi = ketQua.size

        val kho = KhoBai.get(this)
        val xong = muc.values.filter { it.duocGio }.map { it.tu.id }.toSet() - tuDaTra
        tuDaTra += xong
        val giayBuoi = xong.size * LuatTuVung.GIAY_MOI_TU
        val giayTruoc = kho.giayTuVungTu(moc0Gio())
        val phut = LuatTuVung.phutThem(giayTruoc, giayBuoi)
        phutVuaTra += phut
        // Phan tran rieng cat di thi vao quy gio choi (Ba Huy chot ngay 29/9/2026), y nhu
        // Kiem tra bai. Xem [vn.huytl.homeworkgate.data.QuyGio].
        val vuot = (LuatTuVung.phutTu(giayTruoc + giayBuoi) - LuatTuVung.phutTu(giayTruoc) - phut)
            .coerceAtLeast(0)
        if (vuot > 0) vn.huytl.homeworkgate.data.QuyGio.them(this, vuot, "Kiểm tra từ vựng")

        // Gan giay vao dong DUNG CUOI CUNG cua moi tu vua xong: mot tu mot lan, du no
        // co bao nhieu dong trong buoi.
        val daGan = HashSet<String>()
        moi.asReversed().map { t ->
            val cho = t.dung && t.tuId in xong && daGan.add(t.tuId)
            if (cho) t.copy(giay = LuatTuVung.GIAY_MOI_TU) else t
        }.asReversed().forEach { kho.ghiTraTu(it) }

        if (phut > 0) capGio(phut, xong.size)
    }

    private fun capGio(phut: Int, soTu: Int) {
        val ten = boDangLam?.ten.orEmpty()
        // Tinh vao tran ngay, khac gio viec nha: day la gio doi bang viec hoc, cung
        // mot ho voi bai tap, nen no phai nam trong cung mot cai tran.
        gate.congGioHoc(phut, nhanCho = "Kiểm tra từ vựng")
        DayLog.add(this, "Kiểm tra từ vựng $ten: $soTu từ xong, +$phut phút")
        runCatching {
            Notifier.send(
                this,
                "${getString(R.string.child_name)} kiểm tra từ vựng $ten: $soTu từ xong, " +
                    "được $phut phút."
            )
        }
        runCatching { DongBo.dayNgay() }
        ApprovalService.ensureRunning(this)
    }

    // -------------------------------------------------------------- linh tinh

    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private companion object {
        /** Sao cua mot tu, cung la so phut cua no: xem [LuatTuVung.GIAY_MOI_TU]. */
        const val SAO_MOI_TU = LuatTuVung.GIAY_MOI_TU / 60

        /** Do mo cua khung phim trong luc may doc tu, xem [khoaChoDoc]. */
        const val DO_MO_KHI_DOC = 0.45f
    }
}
