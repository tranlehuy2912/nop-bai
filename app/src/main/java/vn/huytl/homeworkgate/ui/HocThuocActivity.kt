package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.databinding.ActivityHocThuocBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TheHoc
import vn.huytl.homeworkgate.kho.TraThe
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier
import java.util.Calendar
import vn.huytl.homeworkgate.databinding.StTheBoBinding
import vn.huytl.homeworkgate.kho.BoDaNap

/**
 * Hoc thuoc: may hoi, con go tra loi ngay tren tablet, may cham bang phep so chuoi.
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
 * CHOT GOI TU BA CHO: nut Xong, [veLui] khi Back ve man chon bo, va [onPause]. Con bam
 * nut Home giua luot van duoc tra cho phan da lam. [daChot] giu cho cac duong do khong
 * chot hai lan.
 */
class HocThuocActivity : AppCompatActivity() {

    private lateinit var b: ActivityHocThuocBinding
    private lateinit var gate: GateStore

    private var boDangLam: BoThe.Bo? = null

    /**
     * Mot cau trong luot nay, kem nhung gi da xay ra voi no.
     *
     * Song trong bo nho suot luot chu khong ghi xuong ngay: con so phut chi tinh duoc
     * khi biet cau nao da qua duoc [LuatTuVung.LAN_DUNG_DE_TINH] lan dung.
     */
    private class MucHoi(val the: TheHoc) {
        /** So lan go dung TRONG LUOT NAY. */
        var soDung = 0

        /** So lan go sai, dung lam bac goi y - xem [LuatTuVung.goiY]. */
        var soSai = 0

        /** Con bam "Chịu rồi": bo cau nay khoi luot, khong tinh phut. */
        var chiu = false

        val xong get() = soDung >= LuatTuVung.LAN_DUNG_DE_TINH
    }

    /** Cac cau cua luot, theo ma the. Giu thu tu de tong ket doc duoc. */
    private val muc = LinkedHashMap<String, MucHoi>()

    /**
     * HANG HOI: danh sach ma the theo dung thu tu se hoi.
     *
     * Mot ma co the nam trong day NHIEU LAN, va do la ca co che: go sai thi cau do bi
     * day xuong cuoi hang, go dung lan dau thi chen lai cach [LuatTuVung.CHEN_LAI] cau
     * de lan dung thu hai la nho that chu khong phai chep lai cai vua nhin.
     */
    private val hang = mutableListOf<String>()
    private var viTri = 0

    /** Da cham cau dang hien chua: chua thi nut la "Trả lời", roi thi la "Câu tiếp". */
    private var daTraLoi = false

    private val ketQua = mutableListOf<TraThe>()
    private var daChot = false

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
        b.oGo.clearFocus()
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(b.oGo.windowToken, 0)
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
     */
    private fun theBo(bo: BoDaNap): View {
        val v = StTheBoBinding.inflate(layoutInflater, b.boxBo, false)
        val conLuot = bo.soDenLuot > 0
        val mauMon = ContextCompat.getColor(this, MatMon.mau(bo.mon))
        val nenMon = ContextCompat.getColorStateList(this, MatMon.nen(bo.mon))

        v.huyHieu.text = MatMon.tat(bo.mon)
        v.huyHieu.setTextColor(if (conLuot) mauMon else mau(R.color.ink_soft))
        v.huyHieu.backgroundTintList =
            if (conLuot) nenMon else ContextCompat.getColorStateList(this, R.color.canvas)

        v.tenBo.text = bo.ten
        v.tenBo.setTextColor(mau(if (conLuot) R.color.ink else R.color.ink_soft))

        v.phuBo.text = if (conLuot) "câu đến lượt hôm nay" else "Hôm nay xong rồi"
        v.phuBo.setTextColor(if (conLuot) mauMon else mau(R.color.ok))

        v.soDenLuot.text = if (conLuot) bo.soDenLuot.toString() else "✓"
        v.soDenLuot.setTextColor(if (conLuot) mauMon else mau(R.color.ok))

        // Thanh nay do phan da thuoc, khong phai phan con lai: con nhin thay cai
        // minh lam duoc, va no chi dai them chu khong bao gio ngan di.
        v.thanhThuoc.max = bo.tongThe.coerceAtLeast(1)
        v.thanhThuoc.setProgressCompat(bo.soThuoc, false)
        v.thanhThuoc.setIndicatorColor(if (conLuot) mauMon else mau(R.color.ok))
        v.chuThuoc.text = "Đã kiểm ${bo.soThuoc}/${bo.tongThe} câu"

        v.root.isEnabled = conLuot
        v.root.alpha = if (conLuot) 1f else 0.7f
        if (conLuot) v.root.setOnClickListener { batDau(bo.bo) }
        return v.root
    }

    private fun mau(id: Int) = ContextCompat.getColor(this, id)

    // ------------------------------------------------------------------- luot

    private fun batDau(ma: String) {
        val bo = BoThe.theoMa(ma) ?: return
        val cac = KhoBai.get(this).cacTheDenLuot(ma, HocThuoc.SO_THE_MOI_LUOT)
        if (cac.isEmpty()) return veChonBo()

        boDangLam = bo
        viTri = 0
        ketQua.clear()
        daChot = false
        muc.clear()
        hang.clear()
        // Tron thu tu, khong hoi theo thu tu in trong sach. Hoi theo thu tu sach thi
        // con nho duoc theo mach - cau nay xong den cau ke - ma do la nho vi tri chu
        // khong phai nho noi dung.
        cac.shuffled().forEach {
            muc[it.id] = MucHoi(it)
            hang += it.id
        }
        b.txtTieuDe.text = bo.ten
        b.boxBo.visibility = View.GONE
        b.txtTrong.visibility = View.GONE
        b.theXong.visibility = View.GONE
        b.boxHoi.visibility = View.VISIBLE
        b.txtChan.text = ""
        // Moi mon mot dai ky tu rieng, mon nao khong can thi an han: hien may hang
        // nut vo dung thi con phai luot qua chung moi toi o go. Xem [BanPhimKyTu].
        val cacNhom = BanPhimKyTu.cuaMon(bo.mon)
        if (cacNhom != null) {
            b.daiKyTu.visibility = View.VISIBLE
            BanPhimKyTu.ve(b.daiKyTu, cacNhom) { chen(it) }
        } else {
            b.daiKyTu.visibility = View.GONE
        }
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
        b.oGo.setText("")
        b.oGo.isEnabled = true
        b.oGo.requestFocus()
        // Hien lai nut: het luot truoc thi [xongLuot] da an no. Tu khi Back ve man chon
        // bo, luot moi chay ngay trong man nay chu khong mo lai man tu dau.
        b.btnChinh.visibility = View.VISIBLE
        b.btnChinh.setText(R.string.hoc_thuoc_tra_loi)

        // Da sai lan nao thi giu goi y tren man hinh, va mo them mot bac moi lan sai.
        if (m.soSai > 0) {
            b.theKet.visibility = View.VISIBLE
            b.txtKet.text = "Lần trước chưa đúng"
            b.txtKet.setTextColor(mau(R.color.alert))
            b.txtDap.text = "Gợi ý: ${LuatTuVung.goiY(m.the.dap, m.soSai)}"
            b.btnChiu.visibility = View.VISIBLE
        } else {
            b.theKet.visibility = View.GONE
            b.btnChiu.visibility = View.GONE
        }
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
        val go = b.oGo.text.toString()
        if (go.isBlank()) return

        val dung = HocThuoc.dung(go, m.the, boDangLam?.phanBietHoa == true)
        ketQua += TraThe(theId = m.the.id, go = go.trim(), dung = dung, phut = 0)
        daTraLoi = true
        b.oGo.isEnabled = false
        b.theKet.visibility = View.VISIBLE
        b.btnChiu.visibility = View.GONE

        if (dung) {
            m.soDung++
            b.txtKet.text = if (m.xong) "Đúng rồi" else "Đúng rồi, câu này sẽ hỏi lại một lần"
            b.txtKet.setTextColor(mau(R.color.ok))
            b.txtDap.text = m.the.dap
            // Chua du so lan dung thi chen lai, CACH RA chu khong hoi ngay: hoi ngay
            // thi cai vua nhin con nam nguyen trong dau, dung gan chac chan, va lan
            // hai khong do them gi.
            if (!m.xong) chenLai(m.the.id)
        } else {
            m.soSai++
            b.txtKet.text = "Chưa đúng"
            b.txtKet.setTextColor(mau(R.color.alert))
            b.txtDap.text = "Gợi ý: ${LuatTuVung.goiY(m.the.dap, m.soSai)}"
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
        b.oGo.isEnabled = false
        b.btnChiu.visibility = View.GONE
        b.theKet.visibility = View.VISIBLE
        b.txtKet.text = "Câu này để mai làm lại"
        b.txtKet.setTextColor(mau(R.color.ink_soft))
        b.txtDap.text = "Đáp án: ${m.the.dap}"
        b.btnChinh.setText(nutTiep())
    }

    /** Con cau nao phia sau khong: co thi "Câu tiếp", het thi "Xem kết quả". */
    private fun nutTiep(): Int =
        if (viTri + 1 < hang.size) R.string.hoc_thuoc_tiep else R.string.hoc_thuoc_xem_ket

    /**
     * Chen ma the vao hang, cach vi tri hien tai [LuatTuVung.CHEN_LAI] cau.
     *
     * Gan cuoi hang thi day han xuong cuoi - khong con du cau de chen vao giua.
     */
    private fun chenLai(ma: String) {
        val cho = LuatTuVung.chenLai(viTri, hang.size - viTri - 1).coerceAtMost(hang.size)
        hang.add(cho, ma)
    }

    private fun sangTheSau() {
        viTri++
        veThe()
    }

    private fun xongLuot() {
        chot()
        val soXong = muc.values.count { it.xong }
        val soChiu = muc.values.count { it.chiu }
        val phut = phutVuaTra
        b.boxHoi.visibility = View.GONE
        b.theXong.visibility = View.VISIBLE
        b.txtXong.text = "$soXong / ${muc.size} câu xong"
        b.txtXongPhu.text = buildString {
            when {
                phut > 0 -> append("Được thêm $phut phút chơi.")
                soXong == 0 -> append("Chưa được phút nào. Xem lại rồi làm tiếp nhé.")
                // Mot cau tron mot phut, nen co cau xong ma khong ra phut chi con mot
                // nghia: phan kiem tra bai cua hom nay da day.
                else -> append(
                    "Mỗi câu xong được ${HocThuoc.GIAY_MOI_THE / 60} phút, nhưng hôm nay " +
                        "đã đủ ${HocThuoc.TRAN_PHUT_MOI_NGAY} phút của phần kiểm tra bài rồi."
                )
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
     * Ghi ca luot xuong so va cap gio. Goi bao nhieu lan cung chi an mot lan.
     *
     * DEM THEO SO CAU DA XONG, khong phai so lan go dung. Mot cau phai dung
     * [LuatTuVung.LAN_DUNG_DE_TINH] lan moi tinh la xong, nen dem so lan go dung thi
     * hai lan cua cung mot cau thanh hai cau - va con duoc tra gap doi cho mot cau.
     *
     * So phut tinh MOT LAN cho ca luot, khong cong don tung cau. Hoi mot cau con la
     * nua phut (truoc 23/9/2026), chia le tung cau thi cau nao cung ra 0; gio mot cau
     * tron mot phut, nhung tran ngay van phai hoi mot lan tai day, sau khi da biet ca
     * luot lam ra bao nhieu giay.
     */
    private fun chot() {
        if (daChot || ketQua.isEmpty()) return
        daChot = true

        val kho = KhoBai.get(this)
        val moc = moc0Gio()
        val dung = muc.values.count { it.xong }
        val giayLuot = HocThuoc.giayCho(dung)
        val phut = HocThuoc.phutThem(kho.giayTheTu(moc), giayLuot)
        phutVuaTra = phut

        /*
         * Cot phut ghi so phut LAM RA, khong phai so phut cong duoc that.
         *
         * Hai con so nay lech nhau dung mot truong hop: tran ngay chung (135 phut)
         * da het, luc do [GateStore.approve] cat bot hoac tu choi han. Van ghi so
         * lam ra, vi cot nay chi de giu tran RIENG cua duong hoc thuoc - cai tran
         * hoi "hom nay da hoc thuoc bao nhieu", khong phai "da choi bao nhieu". Ghi
         * so cong duoc thi ngay nao tran chung het som, tran rieng tu noi ra va con
         * ngoi go them ca tram the ma khong duoc gi.
         *
         * Gan het vao THE DAU TIEN dung cua luot: ben tren cong cot nay lai nen tong
         * phai dung, con no nam o dong nao thi khong ai hoi. Chia le ra tung dong
         * chi de ra mot cot so khong ai cong nham duoc.
         */
        var conGan = phut
        var conGanGiay = giayLuot
        ketQua.forEach { t ->
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
        // Tinh vao tran ngay, khac gio viec nha: day la gio doi bang viec hoc, cung
        // mot ho voi bai tap, nen no phai nam trong cung mot cai tran.
        if (gate.state == GateState.ACTIVE) {
            gate.extend(phut, useQuota = true)
        } else {
            gate.approve(wantedMinutes = phut, useQuota = true, nhanCho = "Kiểm tra bài")
        }
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

    private fun chen(chu: String) {
        val o: EditText = b.oGo
        val dau = o.selectionStart.coerceAtLeast(0)
        val cuoi = o.selectionEnd.coerceAtLeast(0)
        o.text.replace(minOf(dau, cuoi), maxOf(dau, cuoi), chu)
    }

    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
