package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.databinding.StActivityDoTuBinding
import vn.huytl.homeworkgate.databinding.StTheBoBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.BuoiDo
import vn.huytl.homeworkgate.kho.Chieu
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TraTu
import vn.huytl.homeworkgate.kho.TuVung
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier
import java.util.Calendar

/**
 * Do tu vung: may hoi mot tu, con tra loi thang tren tablet, may cham ngay.
 *
 * TACH KHOI [HocThuocActivity] du hai man chay cung mot vong - hoi, cham, sai thi
 * hoi lai, xong thi cap gio. Ly do o hai cho khong gop duoc:
 *
 *  - the hoc thuoc giu mot cap hoi/dap co dinh va den luot theo lich hen 3/10/30
 *    ngay; mot tu thi hoi duoc CA HAI CHIEU va duoc boc theo trong so moi buoi;
 *  - chieu nhin tu doan nghia la mot cau trac nghiem bon o, khong phai o go chu.
 *
 * Gop lai thi man kia phai nhan hai loai cau hoi, hai cach chon, hai cach cham -
 * ba cai if long trong mot vong dang chay tot moi ngay. Cai PHAI dung chung, va
 * dang dung chung, la [LuatTuVung] voi [vn.huytl.homeworkgate.data.HocThuoc.chuanHoa]:
 * moi luat deu nam trong do, hai man chi la hai cai mieng noi vao cung mot kho.
 *
 * CA BUOI GIU TRONG BO NHO, GHI MOT LAN LUC CHOT - y het man kia, va cung mot ly
 * do: may giet app giua buoi thi con lam lai tu dau, khong mat gi va cung khong
 * duoc tra gio hai lan.
 */
class DoTuVungActivity : AppCompatActivity() {

    private lateinit var b: StActivityDoTuBinding
    private lateinit var gate: GateStore

    private var boDangLam: BoTuVung.Bo? = null

    /** Ma phien, de [KhoBai.tinhTrangTu] dem duoc "dung du hai lan trong MOT buoi". */
    private var phien = ""

    /**
     * Mot tu trong buoi nay, kem nhung gi da xay ra voi no.
     *
     * [moiNhu] chon mot lan luc bat dau buoi chu khong chon lai moi lan hien: hoi
     * lai cung mot tu ma bon o doi khac di thi con phai doc lai ca bon, trong khi
     * cai dang kiem tra la con nho nghia hay khong.
     */
    private class MucHoi(val tu: TuVung, val chieu: Chieu, val moiNhu: List<String>) {
        var soDung = 0
        var soSai = 0
        var chiu = false

        /** So lan da thu, de ghi [TraTu.lan]. */
        var lan = 0

        val xong get() = soDung >= LuatTuVung.LAN_DUNG_DE_TINH
    }

    private val muc = LinkedHashMap<String, MucHoi>()

    /** Hang hoi: ma tu theo dung thu tu se hoi. Mot ma nam nhieu lan la co che. */
    private val hang = mutableListOf<String>()
    private var viTri = 0

    private var daTraLoi = false
    private val ketQua = mutableListOf<TraTu>()
    private var daChot = false

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
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = veLui()
        })
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
        b.oGo.clearFocus()
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(b.oGo.windowToken, 0)
        veChonBo()
    }

    override fun onPause() {
        chot()
        super.onPause()
    }

    // ------------------------------------------------------------- chon bo tu

    private fun veChonBo() {
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
     * Bo chua chon Unit thi bam vao la hoi "lop da hoc toi Unit nao" truoc, y het man
     * kiem tra bai, xem [HocToi]. Dong cuoi cua the noi lop dang o Unit may, kem nut doi
     * bam duoc ca khi the dang xam.
     */
    private fun theBo(bo: BoTuVung.Bo, conGio: Boolean): View {
        val kho = KhoBai.get(this)
        val unit = HocToi.unitCua(this, bo.bo)
        val caBo = kho.tinhTrangTu(bo.bo, LuatTuVung.LAN_DUNG_DE_TINH)
        // Da chon Unit thi thanh tien do tinh tren phan lop da hoc, nhu truoc khi co cho
        // chon. Chua chon hay chua hoc Unit nao thi phan do rong, va "Đã thuộc 0/0 từ"
        // doc nhu may hong, nen luc do tinh tren ca bo.
        val tinh = if (unit != null && unit > HocToi.CHUA_HOC_UNIT_NAO) daMo(unit, caBo) else caBo
        val thuoc = tinh.count { it.second == LuatTuVung.TinhTrang.DA_THUOC }
        val chuaChon = unit == null
        val chuaHoc = unit == HocToi.CHUA_HOC_UNIT_NAO
        // Het tran ngay thi ca bo chua chon cung xam: vao chon xong cung khong duoc phut
        // nao, con nut doi o dong cuoi van bam duoc.
        val sang = conGio && !chuaHoc

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
            !conGio -> "Hôm nay dò đủ rồi"
            chuaChon -> "Chọn Unit lớp đã học tới"
            // Dong cuoi da noi lop chua hoc Unit nao, o day noi he qua cua no.
            chuaHoc -> "Chưa có từ để hỏi"
            // Moi hoc Unit 1 thi "Unit 1–1" doc nhu may hong.
            unit == 1 -> "từ mỗi buổi, trong Unit 1"
            else -> "từ mỗi buổi, trong Unit 1–$unit"
        }
        v.phuBo.setTextColor(
            when {
                !conGio -> mau(R.color.ok)
                sang -> mauMon
                else -> mau(R.color.ink_soft)
            }
        )

        v.soDenLuot.text = when {
            !conGio -> "✓"
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

        v.chuHocToi.text = unit?.let { "Lớp ${HocToi.moTaUnit(it)}" }
            ?: "Lớp đã học tới: chưa chọn"
        v.btnHocToi.text = if (chuaChon) "Chọn" else "Đổi"
        v.btnHocToi.setOnClickListener { hoiHocToi(bo, roiBatDau = chuaChon && conGio) }

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
     * Hoi lop da hoc toi Unit nao trong [bo], ghi lai, roi ve lai man chon bo.
     *
     * [roiBatDau] y het ben man kiem tra bai: con bam vao bo chua chon de lam, nen chon
     * xong la vao buoi luon.
     */
    private fun hoiHocToi(bo: BoTuVung.Bo, roiBatDau: Boolean) {
        val cacUnit = KhoBai.get(this).cacTuCua(bo.bo)
            .map { it.unit }.filter { it > 0 }.distinct().sorted()
        if (cacUnit.isEmpty()) return
        val cacMuc = listOf("Chưa học Unit nào") + cacUnit.map { "Unit $it" }
        val dangChon = when (val u = HocToi.unitCua(this, bo.bo)) {
            null -> -1
            HocToi.CHUA_HOC_UNIT_NAO -> 0
            else -> cacUnit.indexOf(u).let { if (it < 0) -1 else it + 1 }
        }
        ChonHocToi.hoi(
            this,
            tieuDe = "${bo.ten}: lớp đã học tới Unit nào?",
            goiY = "Tính cả Unit đang học. Máy chỉ hỏi từ của Unit 1 tới hết Unit " +
                "${getString(R.string.child_name)} chọn.",
            cacMuc = cacMuc,
            dangChon = dangChon
        ) { i ->
            val unit = if (i == 0) HocToi.CHUA_HOC_UNIT_NAO else cacUnit[i - 1]
            HocToi.datUnit(this, bo, unit)
            if (roiBatDau && unit > HocToi.CHUA_HOC_UNIT_NAO) batDau(bo) else veChonBo()
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
        val unit = HocToi.unitCua(this, bo.bo) ?: return hoiHocToi(bo, roiBatDau = true)
        val tinh = daMo(unit, kho.tinhTrangTu(bo.bo, LuatTuVung.LAN_DUNG_DE_TINH))
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
        daChot = false
        muc.clear()
        hang.clear()
        chon.forEach { (tu, _, soPhienXong) ->
            val chieu = LuatTuVung.chieuCho(soPhienXong)
            val moiNhu = if (chieu == Chieu.ANH_VIET) moiNhuCho(tu, tatCa) else emptyList()
            muc[tu.id] = MucHoi(tu, chieu, moiNhu)
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
        unit: Int,
        cac: List<Triple<TuVung, LuatTuVung.TinhTrang, Int>>
    ): List<Triple<TuVung, LuatTuVung.TinhTrang, Int>> =
        cac.filter { LuatTuVung.daHoc(it.first.unit, unit) }

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

        if (m.chieu == Chieu.ANH_VIET) veChieuNhinTu(m) else veChieuGoTu(m)

        // Da sai lan nao thi giu goi y tren man hinh, va mo them mot bac moi lan sai.
        if (m.soSai > 0) {
            b.theKet.visibility = View.VISIBLE
            b.txtKet.text = "Lần trước chưa đúng"
            b.txtKet.setTextColor(mau(R.color.alert))
            b.txtDap.text = if (m.chieu == Chieu.ANH_VIET) {
                "Từ này ở Unit ${m.tu.unit}"
            } else {
                "Gợi ý: ${LuatTuVung.goiY(m.tu.tu, m.soSai)}"
            }
            b.btnChiu.visibility = View.VISIBLE
        }
    }

    /** Nhin tu tieng Anh, bam mot trong bon nghia. */
    private fun veChieuNhinTu(m: MucHoi) {
        b.txtNhan.setText(R.string.do_tu_nhan_anh)
        b.txtHoi.text = m.tu.tu
        b.txtAm.text = m.tu.am
        b.txtAm.visibility = if (m.tu.am.isBlank()) View.GONE else View.VISIBLE

        b.oGo.visibility = View.GONE
        b.boxChon.visibility = View.VISIBLE
        b.boxChon.removeAllViews()
        m.moiNhu.forEach { nghia ->
            val o = LayoutInflater.from(this)
                .inflate(R.layout.st_dong_chon_nghia, b.boxChon, false) as MaterialButton
            o.text = nghia
            o.setOnClickListener { if (!daTraLoi) cham(m, nghia, nghia == m.tu.nghia) }
            b.boxChon.addView(o)
        }
        // Bam thang vao o la tra loi luon, nen khong co nut "Trả lời" o chieu nay.
        b.btnChinh.visibility = View.GONE
    }

    /** Nhin nghia tieng Viet, go tu tieng Anh. */
    private fun veChieuGoTu(m: MucHoi) {
        b.txtNhan.setText(R.string.do_tu_nhan_viet)
        b.txtHoi.text = m.tu.nghia
        b.txtAm.visibility = View.GONE

        b.boxChon.visibility = View.GONE
        b.oGo.visibility = View.VISIBLE
        b.oGo.setText("")
        b.oGo.isEnabled = true
        b.oGo.requestFocus()
        b.btnChinh.visibility = View.VISIBLE
        b.btnChinh.setText(R.string.do_tu_tra_loi)
    }

    private fun traLoiGo() {
        val m = dangHoi() ?: return
        val go = b.oGo.text.toString()
        if (go.isBlank()) return
        cham(m, go.trim(), LuatTuVung.dung(go, m.tu.tu))
    }

    /**
     * Ghi mot lan thu va ve ket qua.
     *
     * DUNG THI HIEN DAP AN, ke ca lan dung dau: luc do khong con gi de gian nua, ma
     * con doi chieu duoc cach viet cua minh voi cach sach in.
     */
    private fun cham(m: MucHoi, go: String, dung: Boolean) {
        m.lan++
        ketQua += TraTu(
            tuId = m.tu.id,
            phien = phien,
            buoi = BuoiDo.HANG_NGAY,
            chieu = m.chieu,
            lan = m.lan,
            go = go,
            dung = dung,
            goiY = m.soSai.coerceAtMost(LuatTuVung.BAC_GOI_Y_TOI_DA),
            chiu = false,
            giay = 0
        )
        daTraLoi = true
        b.oGo.isEnabled = false
        b.boxChon.children().forEach { it.isEnabled = false }
        b.theKet.visibility = View.VISIBLE
        b.btnChiu.visibility = View.GONE

        if (dung) {
            m.soDung++
            b.txtKet.text = if (m.xong) "Đúng rồi" else "Đúng rồi, từ này sẽ hỏi lại một lần"
            b.txtKet.setTextColor(mau(R.color.ok))
            b.txtDap.text = dapAn(m)
            if (!m.xong) chenLai(m.tu.id)
        } else {
            m.soSai++
            b.txtKet.text = "Chưa đúng"
            b.txtKet.setTextColor(mau(R.color.alert))
            b.txtDap.text = if (m.chieu == Chieu.ANH_VIET) {
                "Từ này ở Unit ${m.tu.unit}"
            } else {
                "Gợi ý: ${LuatTuVung.goiY(m.tu.tu, m.soSai)}"
            }
            // Day xuong cuoi hang: tu nao cung phai lam cho duoc, nhung khong ngoi
            // mai o mot tu.
            hang += m.tu.id
            b.btnChiu.visibility = View.VISIBLE
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
            goiY = LuatTuVung.BAC_GOI_Y_TOI_DA,
            chiu = true,
            giay = 0
        )
        for (i in hang.size - 1 downTo viTri + 1) if (hang[i] == m.tu.id) hang.removeAt(i)

        daTraLoi = true
        b.oGo.isEnabled = false
        b.boxChon.children().forEach { it.isEnabled = false }
        b.btnChiu.visibility = View.GONE
        b.theKet.visibility = View.VISIBLE
        b.txtKet.text = "Từ này để mai gặp lại"
        b.txtKet.setTextColor(mau(R.color.ink_soft))
        b.txtDap.text = "Đáp án: ${dapAn(m)}"
        b.btnChinh.visibility = View.VISIBLE
        b.btnChinh.setText(nutTiep())
    }

    private fun nutTiep(): Int =
        if (viTri + 1 < hang.size) R.string.do_tu_tiep else R.string.do_tu_xem_ket

    private fun chenLai(ma: String) {
        val cho = LuatTuVung.chenLai(viTri, hang.size - viTri - 1).coerceAtMost(hang.size)
        hang.add(cho, ma)
    }

    private fun sangTuSau() {
        viTri++
        veTu()
    }

    private fun xongBuoi() {
        chot()
        val soXong = muc.values.count { it.xong }
        val soChiu = muc.values.count { it.chiu }
        b.boxHoi.visibility = View.GONE
        b.theXong.visibility = View.VISIBLE
        b.txtXong.text = "$soXong / ${muc.size} từ xong"
        b.txtXongPhu.text = buildString {
            when {
                phutVuaTra > 0 -> append("Được thêm $phutVuaTra phút chơi.")
                soXong == 0 -> append("Chưa được phút nào. Xem lại rồi làm tiếp nhé.")
                // Mot tu tron mot phut, nen co tu xong ma khong ra phut chi con mot
                // nghia: phan tu vung cua hom nay da day.
                else -> append(
                    "Mỗi từ xong được ${LuatTuVung.GIAY_MOI_TU / 60} phút, nhưng hôm nay " +
                        "đã dò đủ phần của ngày rồi."
                )
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
     * [LuatTuVung.LAN_DUNG_DE_TINH] lan moi tinh la xong.
     *
     * Giay gan vao TUNG TU da xong chu khong don het vao mot dong, vi [TraTu.giay]
     * la "so giay da tra cho tu nay" - dem le ra thi sau nay con hoi duoc tu nao da
     * tra bao nhieu. Ben the hoc thuoc don mot cuc vi cot ben do khong hua gi ca.
     */
    private fun chot() {
        if (daChot || ketQua.isEmpty()) return
        daChot = true

        val kho = KhoBai.get(this)
        val xong = muc.values.filter { it.xong }.map { it.tu.id }.toSet()
        val giayBuoi = xong.size * LuatTuVung.GIAY_MOI_TU
        phutVuaTra = LuatTuVung.phutThem(kho.giayTuVungTu(moc0Gio()), giayBuoi)

        // Gan giay vao dong DUNG CUOI CUNG cua moi tu da xong: mot tu mot lan, du no
        // co bao nhieu dong trong buoi.
        val daGan = HashSet<String>()
        ketQua.asReversed().map { t ->
            val cho = t.dung && t.tuId in xong && daGan.add(t.tuId)
            if (cho) t.copy(giay = LuatTuVung.GIAY_MOI_TU) else t
        }.asReversed().forEach { kho.ghiTraTu(it) }

        if (phutVuaTra > 0) capGio(phutVuaTra, xong.size)
    }

    private fun capGio(phut: Int, soTu: Int) {
        val ten = boDangLam?.ten.orEmpty()
        // Tinh vao tran ngay, khac gio viec nha: day la gio doi bang viec hoc, cung
        // mot ho voi bai tap, nen no phai nam trong cung mot cai tran.
        if (gate.state == GateState.ACTIVE) {
            gate.extend(phut, useQuota = true)
        } else {
            gate.approve(wantedMinutes = phut, useQuota = true, nhanCho = "Dò từ vựng")
        }
        DayLog.add(this, "Dò từ vựng $ten: $soTu từ xong, +$phut phút")
        runCatching {
            Notifier.send(
                this,
                "${getString(R.string.child_name)} dò từ vựng $ten: $soTu từ xong, " +
                    "được $phut phút."
            )
        }
        runCatching { DongBo.dayNgay() }
        ApprovalService.ensureRunning(this)
    }

    // -------------------------------------------------------------- linh tinh

    private fun android.view.ViewGroup.children(): List<View> =
        (0 until childCount).map { getChildAt(it) }

    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
