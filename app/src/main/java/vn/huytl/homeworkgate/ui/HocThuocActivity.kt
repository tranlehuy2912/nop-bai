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
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.PhimKiemTra
import vn.huytl.homeworkgate.databinding.ActivityHocThuocBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.kho.TheHoc
import vn.huytl.homeworkgate.kho.TraThe
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier
import java.util.Calendar
import vn.huytl.homeworkgate.databinding.StTheBoBinding
import vn.huytl.homeworkgate.kho.BoDaNap

/**
 * Hoc thuoc (ten tren man la "Kiểm tra bài"): may hoi, con tra loi ngay tren tablet, may cham
 * bang phep so chuoi.
 *
 * TRA LOI BANG PHIM GHEP tu 2/10/2026, nhu bai lam tren may va man kiem tra tu vung: dap an
 * chu thi xep the chu, dap an so hay cong thuc thi phim rieng cua the cong hang so va dau. Khoi
 * ghep cua tung the va bac goi y o [PhimKiemTra], khung ve o [KhungGhep]. Truoc ngay do con go
 * bang ban phim Android kem mot dai nut ky hieu, goi y la mot dong chu kem nut chep.
 *
 * VI SAO KHONG DI DUONG NOP BAI. Duong kia do cong suc bang so dong viet tren giay,
 * ma hoc thuoc thi khong de ra dong nao - xem [HocThuoc]. Duong nay doi cau hoi
 * thay vi doi cach do: thay vi hoi "con da viet bao nhieu", no hoi thang cai con
 * phai nho, va cau tra loi thi dung hay sai ro rang.
 *
 * CA LUOT GIU TRONG BO NHO, GHI MOT LAN LUC CHOT. Neu he thong giet app giua luot
 * thi khong co gi duoc ghi ca: con lam lai tu dau, khong mat gi va cung khong duoc
 * tra gio hai lan. Do la ban hong an toan nhat trong ba kieu da can nhac - ghi dan
 * tung the thi bi giet la con mat cong ma khong duoc phut nao, con cap gio dan tung
 * phut thi moi lan cap la mot luot day len Firestore.
 *
 * MOI CAU MOT SAO (Ba Huy chot 2/10/2026, cho cung luat voi bai lam tren may: 1 sao la 1 phut).
 * Sao o goc phai dong tien do, ba dang cua [HangSao]: rong khi con duoc phut, vang khi vua lam
 * dung, xam khi goi y da lo dap an. Sai roi lam dung van duoc sao (luat phut khong doi: chi lo
 * dap an moi mat). Lam xong mot cau thi canh chu ket qua hien "+1 ★", lo dap an hay chiu thi
 * "+0 ★". Phut van cong gop luc chot ([chot]), qua tran ngay thi vao quy gio choi.
 *
 * CHOT GOI TU BA CHO: nut Xong, [veLui] khi Back ve man chon bo, va [onPause]. Con bam
 * nut Home giua luot van duoc tra cho phan da lam. Moi lan chot chi ghi va tra phan
 * lam them tu lan chot truoc ([daGhi], [daTraXong]): tat man hinh giua luot roi bat len
 * lam tiep thi phan sau van duoc tinh. Truoc day lan chot dau khoa ca luot, va moi cau
 * lam sau lan tat man hinh do deu khong duoc ghi, khong duoc gio.
 */
class HocThuocActivity : AppCompatActivity() {

    private lateinit var b: ActivityHocThuocBinding
    private lateinit var gate: GateStore

    private var boDangLam: BoThe.Bo? = null

    /**
     * Mot cau trong luot nay, kem nhung gi da xay ra voi no.
     *
     * Song trong bo nho suot luot chu khong ghi xuong ngay: con so phut chi tinh duoc
     * khi biet cau nao da xong.
     */
    private class MucHoi(
        val the: TheHoc,
        private val phanBietHoa: Boolean,
        /** Khoi ghep cua the, dung mot lan luc bat dau luot: xem [PhimKiemTra.ghepThe]. */
        val khoi: PhimKiemTra.Khoi,
        /** Hat tron cua the trong luot: hoi lai the nay thi phim mo dung cho lan truoc. */
        val hat: Int
    ) {
        /** So lan lam dung TRONG LUOT NAY. */
        var soDung = 0

        /** So lan sai, dung lam bac goi y - xem [PhimKiemTra]. */
        var soSai = 0

        /** Con bam "Chịu rồi": bo cau nay khoi luot, khong tinh phut. */
        var chiu = false

        /**
         * Go dung mot lan la xong, ke ca cau da sai truoc do trong luot - xem
         * [LuatTuVung.LAN_DUNG_DE_TINH]. Truoc 27/9/2026 phai dung hai lan.
         */
        val xong get() = soDung >= LuatTuVung.LAN_DUNG_DE_TINH

        /**
         * Goi y da lo dap an, hay phan dien san da la mot dap an duoc cham dung - xem
         * [PhimKiemTra.Khoi.loHet]. Van phai lam dung moi xong, nhung khong duoc cong gio.
         */
        val loHet get() = khoi.loHet(soSai) { HocThuoc.dung(it, the, phanBietHoa) }

        /** Xong ma khong phai doc dap an tren man: cau duoc tinh gio. */
        val duocGio get() = xong && !loHet
    }

    /** Cac cau cua luot, theo ma the. Giu thu tu de tong ket doc duoc. */
    private val muc = LinkedHashMap<String, MucHoi>()

    /**
     * HANG HOI: danh sach ma the theo dung thu tu se hoi.
     *
     * Mot ma co the nam trong day NHIEU LAN: go sai thi cau do bi day xuong cuoi hang,
     * sai may lan thi quay lai may lan, cho toi khi go dung.
     */
    private val hang = mutableListOf<String>()
    private var viTri = 0

    /** Da cham cau dang hien chua: chua thi nut la "Trả lời", roi thi la "Câu tiếp". */
    private var daTraLoi = false

    /** Khung phim ghep cua the dang hoi, ve lai moi lan hien mot the. */
    private var khung: KhungGhep? = null

    /** Ma luot, lam hat tron phim cua tung the: luot sau tron khac luot truoc. */
    private var phien = ""

    private val ketQua = mutableListOf<TraThe>()

    /** So dong trong [ketQua] da ghi xuong so. */
    private var daGhi = 0

    /** So cau da xong va da tra gio trong luot nay. */
    private var daTraXong = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHocThuocBinding.inflate(layoutInflater)
        setContentView(b.root)
        gate = GateStore(this)

        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top + 28.dp(), bottom = bars.bottom + 28.dp())
            insets
        }

        b.btnThoat.setOnClickListener { veLui() }
        b.btnChinh.setOnClickListener { if (daTraLoi) sangTheSau() else traLoi() }
        b.btnChiu.setOnClickListener { chiuThoi() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = veLui()
        })
        veChonBo()
    }

    /**
     * Lui mot bac, cho ca nut Back lan mui ten goc tren.
     *
     * Dang lam luot, hay dang xem ket qua luot, thi ve man chon bo; o man chon bo moi
     * ra man chinh. Truoc 24/9/2026 ca hai nut deu thoat thang ra man chinh, va con
     * muon lam bo khac thi phai bam vao "Kiểm tra bài" lai tu dau.
     *
     * Chot luot TRUOC khi ve man chon bo. Ra khoi man hinh thi [onPause] chot ho, con
     * o lai trong man nay thi khong ai goi onPause, va phan con vua lam se mat. Chot
     * hai lan cung chi an mot lan, xem [chot].
     */
    private fun veLui() {
        if (b.boxBo.visibility == View.VISIBLE) return finish()
        chot()
        veChonBo()
    }

    override fun onPause() {
        chot()
        super.onPause()
    }

    // ------------------------------------------------------------- chon bo the

    private fun veChonBo() {
        val bang = BoThe.bang(this)
        b.boxBo.visibility = View.VISIBLE
        b.boxHoi.visibility = View.GONE
        b.theXong.visibility = View.GONE
        b.boxBo.removeAllViews()
        b.txtTieuDe.setText(R.string.hoc_thuoc_title)

        if (bang.isEmpty()) {
            b.txtTrong.visibility = View.VISIBLE
            b.txtTrong.setText(R.string.hoc_thuoc_chua_co_bo)
            return
        }
        b.txtTrong.visibility = View.GONE
        b.txtChan.text = getString(R.string.hoc_thuoc_chan)

        bang.forEach { bo -> b.boxBo.addView(theBo(bo)) }
    }

    /**
     * Mot the bo tren man chon.
     *
     * Bo con the den luot thi sang mau mon va bam duoc; het luot hom nay thi xam lai
     * nhung VAN o day chu khong bien mat - con nhin thay minh da thuoc toi dau, va
     * biet mai no quay lai.
     *
     * Bo chua chon bai da hoc cung sang va bam duoc, nhung bam vao la hoi "lop da hoc
     * toi bai nao" truoc, chon xong moi vao luot. Ba Huy muon bat con chon chu khong
     * de may doan, xem [HocToi]. Dong cuoi cua the noi lop dang o dau, kem nut doi
     * bam duoc ca khi the dang xam.
     */
    private fun theBo(bo: BoDaNap): View {
        val v = StTheBoBinding.inflate(layoutInflater, b.boxBo, false)
        val chuaChon = bo.hocToi == null
        val chuaHoc = bo.hocToi == HocToi.CHUA_HOC_BAI_NAO
        val conLuot = bo.soDenLuot > 0
        val sang = conLuot || chuaChon
        val mauMon = ContextCompat.getColor(this, MatMon.mau(bo.mon))
        val nenMon = ContextCompat.getColorStateList(this, MatMon.nen(bo.mon))

        v.huyHieu.text = MatMon.tat(bo.mon)
        v.huyHieu.setTextColor(if (sang) mauMon else mau(R.color.ink_soft))
        v.huyHieu.backgroundTintList =
            if (sang) nenMon else ContextCompat.getColorStateList(this, R.color.canvas)

        v.tenBo.text = bo.ten
        v.tenBo.setTextColor(mau(if (sang) R.color.ink else R.color.ink_soft))

        v.phuBo.text = when {
            chuaChon -> "Chọn bài lớp đã học tới"
            conLuot -> "câu đến lượt hôm nay"
            // Dong cuoi da noi lop chua hoc toi bai nao, o day noi he qua cua no.
            chuaHoc -> "Chưa có câu để hỏi"
            else -> "Hôm nay xong rồi"
        }
        v.phuBo.setTextColor(
            when {
                sang -> mauMon
                chuaHoc -> mau(R.color.ink_soft)
                else -> mau(R.color.ok)
            }
        )

        v.soDenLuot.text = when {
            chuaChon -> "?"
            conLuot -> bo.soDenLuot.toString()
            chuaHoc -> "–"
            else -> "✓"
        }
        v.soDenLuot.setTextColor(
            when {
                sang -> mauMon
                chuaHoc -> mau(R.color.ink_soft)
                else -> mau(R.color.ok)
            }
        )

        // Thanh nay do phan da thuoc, khong phai phan con lai: con nhin thay cai
        // minh lam duoc, va no chi dai them chu khong bao gio ngan di. Nen no tinh tren
        // ca bo chu khong tren phan da hoc: chon them bai thi thanh khong tut.
        v.thanhThuoc.max = bo.tongThe.coerceAtLeast(1)
        v.thanhThuoc.setProgressCompat(bo.soThuoc, false)
        v.thanhThuoc.setIndicatorColor(if (sang) mauMon else mau(R.color.ok))
        v.chuThuoc.text = "Đã kiểm ${bo.soThuoc}/${bo.tongThe} câu"

        v.chuHocToi.text = bo.moTaHocToi ?: "Lớp đã học tới: chưa chọn"
        v.btnHocToi.text = if (chuaChon) "Chọn" else "Đổi"
        v.btnHocToi.setOnClickListener { hoiHocToi(bo.bo, roiBatDau = chuaChon) }

        v.root.isEnabled = sang
        v.root.alpha = if (sang) 1f else 0.7f
        if (sang) {
            v.root.setOnClickListener {
                if (chuaChon) hoiHocToi(bo.bo, roiBatDau = true) else batDau(bo.bo)
            }
        }
        return v.root
    }

    /**
     * Hoi lop da hoc toi bai nao trong bo [ma], ghi lai, roi ve lai man chon bo.
     *
     * [roiBatDau] khi con bam vao mot bo chua chon: con bam de lam, nen chon xong la
     * vao luot luon neu co the den luot, khong bat bam them lan nua.
     */
    private fun hoiHocToi(ma: String, roiBatDau: Boolean) {
        val bo = BoThe.theoMa(ma) ?: return
        // Bo cua mot phan hoc (ca ba bo hien nay) thi hoi du cac bai cua phan, dung chung moc
        // voi kho sach bai tap. Xem [PhanHoc]. Bo Toan hai phan Dai so, Hinh hoc (30/9/2026):
        // con phan chua chon thi hoi lan luot, chon du roi thi hoi doi phan nao.
        val cacPhan = PhanHoc.cuaBoThe(ma)
        if (cacPhan.isNotEmpty()) {
            val xong = { sauKhiChon(ma, roiBatDau) }
            if (cacPhan.any { PhanHoc.hocToi(this, it) == null }) ChonHocToi.hoiCacPhanThieu(this, cacPhan, xong)
            else ChonHocToi.hoiDoiPhan(this, cacPhan, xong)
            return
        }
        val cacBai = KhoBai.get(this).cacBaiTrongBoThe(ma)
        if (cacBai.isEmpty()) return
        val cacMuc = listOf("Chưa học tới bài nào") + cacBai
        val dangChon = when (val bai = HocToi.baiCua(this, ma)) {
            null -> -1
            HocToi.CHUA_HOC_BAI_NAO -> 0
            else -> cacBai.indexOf(bai).let { if (it < 0) -1 else it + 1 }
        }
        ChonHocToi.hoi(
            this,
            tieuDe = "${bo.ten}: lớp đã học tới bài nào?",
            goiY = "Tính cả bài đang học. Máy chỉ hỏi từ bài đầu tới hết bài " +
                "${getString(R.string.child_name)} chọn. " +
                "Không thấy bài đang học thì chọn bài gần nhất phía trên nó.",
            cacMuc = cacMuc,
            dangChon = dangChon
        ) { i ->
            HocToi.datBai(this, bo, if (i == 0) HocToi.CHUA_HOC_BAI_NAO else cacBai[i - 1])
            sauKhiChon(ma, roiBatDau)
        }
    }

    private fun sauKhiChon(ma: String, roiBatDau: Boolean) {
        val m = BoThe.mocCua(this, ma)
        if (roiBatDau && m != null && KhoBai.get(this).conTheDenLuot(ma, denThuTu = m.denThuTu, chiBai = m.chiBai)) {
            batDau(ma)
        } else {
            veChonBo()
        }
    }

    private fun mau(id: Int) = ContextCompat.getColor(this, id)

    // ------------------------------------------------------------------- luot

    /**
     * Bat dau mot luot, chi voi the trong phan lop da hoc. Bo chua chon thi hoi truoc -
     * mot duong vao the ma khong qua [theBo] cung khong lot duoc qua cho hoi do.
     */
    private fun batDau(ma: String) {
        val bo = BoThe.theoMa(ma) ?: return
        val m = BoThe.mocCua(this, ma) ?: return hoiHocToi(ma, roiBatDau = true)
        val cac = KhoBai.get(this).cacTheDenLuot(
            ma, HocThuoc.SO_THE_MOI_LUOT, denThuTu = m.denThuTu, chiBai = m.chiBai
        )
        if (cac.isEmpty()) return veChonBo()

        boDangLam = bo
        phien = "kt-${System.currentTimeMillis()}"
        // Phim nhieu cua moi the lay tu dap an cac the khac trong ca bo, xem [PhimKiemTra.ghepThe].
        val caBo = KhoBai.get(this).cacTheCua(ma)
        viTri = 0
        ketQua.clear()
        daGhi = 0
        daTraXong = 0
        phutVuaTra = 0
        muc.clear()
        hang.clear()
        // Tron thu tu, khong hoi theo thu tu in trong sach. Hoi theo thu tu sach thi
        // con nho duoc theo mach - cau nay xong den cau ke - ma do la nho vi tri chu
        // khong phai nho noi dung.
        cac.shuffled().forEach {
            val hat = "$phien/${it.id}".hashCode()
            muc[it.id] = MucHoi(it, bo.phanBietHoa, PhimKiemTra.ghepThe(it, caBo, bo.phanBietHoa, hat), hat)
            hang += it.id
        }
        b.txtTieuDe.text = bo.ten
        b.boxBo.visibility = View.GONE
        b.txtTrong.visibility = View.GONE
        b.theXong.visibility = View.GONE
        b.boxHoi.visibility = View.VISIBLE
        b.txtChan.text = ""
        veThe()
    }

    /** Muc dang hoi, hay null khi het hang. */
    private fun dangHoi(): MucHoi? = hang.getOrNull(viTri)?.let { muc[it] }

    private fun veThe() {
        val m = dangHoi() ?: return xongLuot()
        daTraLoi = false

        // Dem theo SO CAU da xong tren tong so cau, khong phai vi tri trong hang:
        // hang dai ra moi lan con go sai, nen "câu 7 / 5" la con so vo nghia.
        val xong = muc.values.count { it.xong || it.chiu }
        b.txtTien.text = "Đã xong $xong / ${muc.size} câu"

        b.txtBai.text = m.the.bai
        b.txtHoi.text = m.the.hoi
        b.txtBao.visibility = View.GONE
        b.txtCong.visibility = View.GONE
        veSao(m)
        khung = KhungGhep(b.khung, m.khoi.ghep, m.the.mon, m.hat, dienSan = m.khoi.dienSan(m.soSai)) {
            b.txtBao.visibility = View.GONE
        }.also { it.ve(PhimKiemTra.mucMo(m.soSai)) }
        // Hien lai nut: het luot truoc thi [xongLuot] da an no. Tu khi Back ve man chon
        // bo, luot moi chay ngay trong man nay chu khong mo lai man tu dau.
        b.btnChinh.visibility = View.VISIBLE
        b.btnChinh.setText(R.string.hoc_thuoc_tra_loi)

        // Da sai lan nao thi giu dong "Lần trước sai"; goi y nam tren ban phim, mo them moi lan sai.
        if (m.soSai > 0) {
            b.theKet.visibility = View.VISIBLE
            b.txtKet.text = when {
                !m.loHet -> "Lần trước sai"
                m.khoi.ghep is Ghep.Cau -> "Xếp lại cho đúng đáp án"
                else -> "Gõ lại cho đúng đáp án"
            }
            b.txtKet.setTextColor(mau(R.color.alert))
            hienGoiY(m)
            // Dap an da hien het thi bat go cho dung, khong cho bam chiu de bo qua.
            b.btnChiu.visibility = if (m.loHet) View.GONE else View.VISIBLE
        } else {
            b.theKet.visibility = View.GONE
            b.btnChiu.visibility = View.GONE
        }
    }

    /**
     * Dong duoi chu "Sai" hay "Lần trước sai": dap an khi goi y da hien het, khong thi rong.
     *
     * Tu 2/10/2026 goi y nam tren ban phim (phim mo, phan dien san). Truoc do day la dong
     * "Gợi ý: s…" kem nut chep phan goi y vao o go (Ba Huy chon ngay 27/9/2026); phim ghep
     * khong co o go de dan, va phan dien san da lam dung viec do.
     */
    private fun hienGoiY(m: MucHoi) {
        datDap(if (m.loHet) "Đáp án: ${m.the.dap}" else "")
    }

    /** Sao cua cau dang hoi: rong khi con duoc phut, xam khi goi y da lo dap an. Xem [HangSao]. */
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

    /**
     * Cham cau dang hien roi hien ket qua.
     *
     * SAI THI KHONG HIEN DAP AN, chi ho them mot bac goi y. Hien dap an thi con go
     * bua mot cai, doc dap an, go lai cho dung, va an tron so phut ma khong nho gi.
     * Bat con tu tim thi con phai mo sach hay di hoi, va do chinh la viec hoc.
     *
     * DUNG THI HIEN DAP AN, ke ca lan dung dau. Luc do khong con gi de gian nua, ma
     * con thi doi chieu duoc cach viet cua minh voi cach sach in - phan lon cac lan
     * "sai" cua duong nay la sai mot ky tu.
     */
    private fun traLoi() {
        val m = dangHoi() ?: return
        val k = khung ?: return
        if (!k.ghepXong()) {
            b.txtBao.text = if (m.khoi.ghep is Ghep.Cau) "Chọn thẻ trước đã" else "Gõ câu trả lời trước đã"
            b.txtBao.visibility = View.VISIBLE
            return
        }
        val go = k.traLoi()

        val dung = HocThuoc.dung(go, m.the, boDangLam?.phanBietHoa == true)
        // Go dung sau khi da doc dap an tren man thi ghi xuong so nhu bam chiu: kho khong
        // coi la con da nho, the do mai co lai chu khong di nghi ba ngay. Xem [TraThe.chiu].
        val docDapAn = dung && m.loHet
        ketQua += TraThe(
            theId = m.the.id, go = go.trim(), dung = dung && !docDapAn, phut = 0, chiu = docDapAn
        )
        daTraLoi = true
        k.khoa(hienLoiGiai = false)
        b.txtBao.visibility = View.GONE
        b.theKet.visibility = View.VISIBLE
        b.btnChiu.visibility = View.GONE

        if (dung) {
            // Dung mot lan la xong, khong chen lai nua - xem [MucHoi.xong].
            m.soDung++
            b.txtKet.text = "Đúng rồi"
            b.txtKet.setTextColor(mau(R.color.ok))
            datDap(m.the.dap)
            val duoc = !docDapAn
            b.txtSao.text = HangSao.chu(this, listOf(if (duoc) HangSao.O.DUOC else HangSao.O.MAT))
            if (duoc) HangSao.nhip(b.txtSao)
            hienCong(if (duoc) SAO_MOI_THE else 0)
        } else {
            m.soSai++
            // Sai thi sao con nguyen (lam dung lan sau van duoc), tru khi lan sai nay lam lo dap an.
            veSao(m)
            // Chi mot chu "Sai", ke ca khi da hien het dap an: Ba Huy bo cau "không được cộng
            // giờ" o day, va doi "Chưa đúng" thanh "Sai" (27/9/2026). Dong tong ket cuoi luot
            // van ke so cau phai xem dap an.
            b.txtKet.text = "Sai"
            b.txtKet.setTextColor(mau(R.color.alert))
            hienGoiY(m)
            // Day xuong cuoi hang. Cau nao cung phai lam cho duoc, nhung khong phai
            // ngoi mai o mot cau - con di tiep roi quay lai.
            hang += m.the.id
        }
        b.btnChinh.setText(nutTiep())
    }

    /** Con chiu, khong go ra duoc: hien dap an, bo cau nay khoi luot. */
    private fun chiuThoi() {
        val m = dangHoi() ?: return
        m.chiu = true
        // Ghi mot dong "chiu" xuong so. Khong ghi thi bang tinh trang ben [KhoBai]
        // coi nhu con chua he gap cau nay, ma con da gap va da bi no.
        ketQua += TraThe(theId = m.the.id, go = "", dung = false, phut = 0, chiu = true)
        // Bo moi lan xuat hien con lai cua cau nay trong hang, tru cho dang dung.
        for (i in hang.size - 1 downTo viTri + 1) if (hang[i] == m.the.id) hang.removeAt(i)

        daTraLoi = true
        khung?.khoa(hienLoiGiai = false)
        b.txtBao.visibility = View.GONE
        b.btnChiu.visibility = View.GONE
        b.theKet.visibility = View.VISIBLE
        b.txtKet.text = "Câu này để mai làm lại"
        b.txtKet.setTextColor(mau(R.color.ink_soft))
        datDap("Đáp án: ${m.the.dap}")
        b.txtSao.text = HangSao.chu(this, listOf(HangSao.O.MAT))
        hienCong(0)
        b.btnChinh.setText(nutTiep())
    }

    /** Con cau nao phia sau khong: co thi "Câu tiếp", het thi "Xem kết quả". */
    private fun nutTiep(): Int =
        if (viTri + 1 < hang.size) R.string.hoc_thuoc_tiep else R.string.hoc_thuoc_xem_ket

    private fun sangTheSau() {
        viTri++
        veThe()
    }

    private fun xongLuot() {
        chot()
        val soXong = muc.values.count { it.xong }
        val soDuocGio = muc.values.count { it.duocGio }
        val soXemDapAn = muc.values.count { it.xong && it.loHet }
        val soChiu = muc.values.count { it.chiu }
        val phut = phutVuaTra
        b.boxHoi.visibility = View.GONE
        b.theXong.visibility = View.VISIBLE
        b.txtXong.text = "$soXong / ${muc.size} câu xong"
        b.txtXongPhu.text = buildString {
            when {
                phut > 0 -> append("Được thêm $phut phút chơi.")
                soDuocGio == 0 -> append("Chưa được phút nào. Xem lại rồi làm tiếp nhé.")
                // Mot cau tron mot phut, nen co cau xong ma khong ra phut chi con mot
                // nghia: phan kiem tra bai cua hom nay da day.
                else -> append(
                    "Mỗi câu xong được ${HocThuoc.GIAY_MOI_THE / 60} phút, nhưng hôm nay " +
                        "đã đủ ${HocThuoc.TRAN_PHUT_MOI_NGAY} phút của phần kiểm tra bài rồi."
                )
            }
            if (soXemDapAn > 0) {
                append(" $soXemDapAn câu phải xem đáp án nên không được giờ, mai có lại.")
            }
            if (soChiu > 0) append(" Còn $soChiu câu để mai làm lại.")
        }
        // Noi dung su that: cau lam xong moi duoc nghi vai ngay, cau chua xong thi
        // mai co ngay. Dong cu noi "moi cau vai ngay mot lan" nen con lam sai doc
        // vao lai tuong phai cho may ngay moi go lai duoc.
        b.txtChan.text = "Câu làm xong sẽ nghỉ vài ngày. Câu chưa xong thì mai có lại."
        b.btnChinh.visibility = View.GONE
    }

    // ------------------------------------------------------------------- chot

    private var phutVuaTra = 0

    /**
     * Ghi phan moi lam cua luot xuong so va cap gio cho phan do. Goi nhieu lan thi moi
     * lan chi an phan lam them tu lan truoc.
     *
     * DEM THEO SO CAU DA XONG, khong phai so lan go dung. Truoc 27/9/2026 mot cau phai
     * dung hai lan moi xong, dem so lan go dung thi con duoc tra gap doi cho mot cau. Gio
     * dung mot lan la xong, nhung dem theo cau van la cach dem dung.
     *
     * So phut tinh MOT LAN cho ca luot, khong cong don tung cau. Hoi mot cau con la
     * nua phut (truoc 23/9/2026), chia le tung cau thi cau nao cung ra 0; gio mot cau
     * tron mot phut, nhung tran ngay van phai hoi mot lan tai day, sau khi da biet ca
     * luot lam ra bao nhieu giay.
     */
    private fun chot() {
        if (ketQua.size <= daGhi) return
        val moi = ketQua.subList(daGhi, ketQua.size).toList()
        daGhi = ketQua.size

        val kho = KhoBai.get(this)
        val moc = moc0Gio()
        // Chi dem cau duoc gio: cau phai doc dap an tren man thi xong ma khong tra gio.
        val xongHet = muc.values.count { it.duocGio }
        val dung = (xongHet - daTraXong).coerceAtLeast(0)
        daTraXong = xongHet
        val giayLuot = HocThuoc.giayCho(dung)
        val giayTruoc = kho.giayTheTu(moc)
        val phut = HocThuoc.phutThem(giayTruoc, giayLuot)
        phutVuaTra += phut
        // Phan tran rieng cua Kiem tra bai cat di thi vao quy gio choi (Ba Huy chot ngay
        // 29/9/2026), khong mat nhu truoc. Xem [vn.huytl.homeworkgate.data.QuyGio].
        val vuot = ((giayTruoc + giayLuot) / 60 - giayTruoc / 60 - phut).coerceAtLeast(0)
        if (vuot > 0) vn.huytl.homeworkgate.data.QuyGio.them(this, vuot, "Kiểm tra bài")

        /*
         * Cot phut ghi so phut LAM RA, khong phai so phut cong duoc that.
         *
         * Hai con so nay tung lech nhau khi tran ngay chung (135 phut) da het. Tran do bo
         * tu 29/9/2026, nhung van ghi so lam ra, vi cot nay chi de giu tran RIENG cua
         * duong hoc thuoc - cai tran hoi "hom nay da hoc thuoc bao nhieu", khong phai
         * "da choi bao nhieu".
         *
         * Gan het vao THE DAU TIEN dung cua luot: ben tren cong cot nay lai nen tong
         * phai dung, con no nam o dong nao thi khong ai hoi. Chia le ra tung dong
         * chi de ra mot cot so khong ai cong nham duoc.
         */
        var conGan = phut
        var conGanGiay = giayLuot
        moi.forEach { t ->
            val cua = if (t.dung && conGan > 0) conGan.also { conGan = 0 } else 0
            // Cot giay gan het vao dong dung dau tien, y het cot phut. Gan ca khi
            // phut bang 0: cot nay la cong suc lam ra, tran ngay doc no - xem
            // [HocThuoc.phutThem].
            val cuaGiay = if (t.dung && conGanGiay > 0) conGanGiay.also { conGanGiay = 0 } else 0
            kho.ghiTraThe(t.copy(phut = cua, giay = cuaGiay))
        }

        if (phut > 0) capGio(phut, dung)
    }

    private fun capGio(phut: Int, soThe: Int) {
        val ten = boDangLam?.ten.orEmpty()
        // Ghi vao so dem gio doi bang hoc cua ngay, khac gio viec nha. Tran rieng 20 phut
        // da chan o [HocThuoc.phutThem], khong con tran chung (29/9/2026).
        gate.congGioHoc(phut, nhanCho = "Kiểm tra bài")
        DayLog.add(this, "Kiểm tra bài $ten: $soThe câu đúng, +$phut phút")
        runCatching {
            Notifier.send(
                this,
                "${getString(R.string.child_name)} làm kiểm tra bài $ten: $soThe câu đúng, " +
                    "được $phut phút."
            )
        }
        runCatching { DongBo.dayNgay() }
        ApprovalService.ensureRunning(this)
    }

    // ------------------------------------------------------------------- linh tinh

    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private companion object {
        /** Sao cua mot the, cung la so phut cua no: xem [HocThuoc.GIAY_MOI_THE]. */
        const val SAO_MOI_THE = HocThuoc.GIAY_MOI_THE / 60
    }
}
