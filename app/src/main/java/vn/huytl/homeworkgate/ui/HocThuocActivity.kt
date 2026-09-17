package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.databinding.ActivityHocThuocBinding
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TheHoc
import vn.huytl.homeworkgate.kho.TraThe
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier
import java.util.Calendar

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
 * CHOT GOI TU HAI CHO: nut Xong, va [onPause]. Con bam nut Home giua luot van duoc
 * tra cho phan da lam. [daChot] giu cho hai duong do khong chot hai lan.
 */
class HocThuocActivity : AppCompatActivity() {

    private lateinit var b: ActivityHocThuocBinding
    private lateinit var gate: GateStore

    private var boDangLam: BoThe.Bo? = null
    private var cac: List<TheHoc> = emptyList()
    private var viTri = 0

    /** Da go xong the dang hien chua: chua thi nut la "Trả lời", roi thi la "Thẻ tiếp". */
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

        b.btnThoat.setOnClickListener { finish() }
        b.btnChinh.setOnClickListener { if (daTraLoi) sangTheSau() else traLoi() }
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

        bang.forEach { bo ->
            val nut = MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = if (bo.soDenLuot > 0) {
                    "${bo.ten}\n${bo.soDenLuot} thẻ đến lượt"
                } else {
                    "${bo.ten}\nHôm nay không còn thẻ nào đến lượt"
                }
                isEnabled = bo.soDenLuot > 0
                textSize = 16f
                minHeight = 72.dp()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12.dp() }
                setOnClickListener { batDau(bo.bo) }
            }
            b.boxBo.addView(nut)
        }
    }

    // ------------------------------------------------------------------- luot

    private fun batDau(ma: String) {
        val bo = BoThe.theoMa(ma) ?: return
        cac = KhoBai.get(this).cacTheDenLuot(ma, HocThuoc.SO_THE_MOI_LUOT)
        if (cac.isEmpty()) return veChonBo()

        boDangLam = bo
        viTri = 0
        ketQua.clear()
        daChot = false
        b.txtTieuDe.text = bo.ten
        b.boxBo.visibility = View.GONE
        b.txtTrong.visibility = View.GONE
        b.theXong.visibility = View.GONE
        b.boxHoi.visibility = View.VISIBLE
        b.txtChan.text = ""
        // Thanh ky tu toan chi co nghia voi bo the mon Toan. Bo tu vung ma hien no
        // ra thi con phai luot qua mot hang nut vo dung de toi o go.
        if (bo.mon == "Toán") {
            b.daiToan.visibility = View.VISIBLE
            if (b.nutToan.childCount == 0) veDaiToan()
        } else {
            b.daiToan.visibility = View.GONE
        }
        veThe()
    }

    private fun veThe() {
        val the = cac.getOrNull(viTri) ?: return xongLuot()
        daTraLoi = false
        b.txtTien.text = "Thẻ ${viTri + 1} / ${cac.size}"
        b.txtBai.text = the.bai
        b.txtHoi.text = the.hoi
        b.oGo.setText("")
        b.oGo.isEnabled = true
        b.oGo.requestFocus()
        b.theKet.visibility = View.GONE
        b.btnChinh.setText(R.string.hoc_thuoc_tra_loi)
    }

    /**
     * Cham the dang hien roi hien ket qua.
     *
     * HIEN DAP AN DU DUNG HAY SAI. Sai ma khong thay dap an thi con khong hoc duoc
     * gi tu lan sai do, chi biet la minh sai. Dung ma van hien thi con doi chieu
     * duoc cach viet - phan lon cac lan "sai" cua duong nay la sai mot ky tu.
     */
    private fun traLoi() {
        val the = cac.getOrNull(viTri) ?: return
        val go = b.oGo.text.toString()
        if (go.isBlank()) return

        val dung = HocThuoc.dung(go, the)
        ketQua += TraThe(theId = the.id, go = go.trim(), dung = dung, phut = 0)
        daTraLoi = true

        b.oGo.isEnabled = false
        b.theKet.visibility = View.VISIBLE
        b.txtKet.text = if (dung) "Đúng rồi" else "Chưa đúng"
        b.txtKet.setTextColor(
            ContextCompat.getColor(this, if (dung) R.color.ok else R.color.alert)
        )
        b.txtDap.text = if (dung) the.dap else "Đáp án: ${the.dap}"
        b.btnChinh.setText(
            if (viTri + 1 < cac.size) R.string.hoc_thuoc_tiep else R.string.hoc_thuoc_xem_ket
        )
    }

    private fun sangTheSau() {
        viTri++
        veThe()
    }

    private fun xongLuot() {
        chot()
        val dung = ketQua.count { it.dung }
        val phut = phutVuaTra
        b.boxHoi.visibility = View.GONE
        b.theXong.visibility = View.VISIBLE
        b.txtXong.text = "$dung / ${ketQua.size} thẻ đúng"
        b.txtXongPhu.text = when {
            phut > 0 -> "Được thêm $phut phút chơi."
            dung == 0 -> "Chưa được phút nào. Xem lại rồi làm tiếp nhé."
            else -> "Cần ${HocThuoc.THE_MOI_PHUT} thẻ đúng mới được một phút, " +
                "hoặc hôm nay đã đủ ${HocThuoc.TRAN_PHUT_MOI_NGAY} phút của phần học thuộc."
        }
        b.txtChan.text = "Mấy thẻ này sẽ quay lại sau vài ngày để con nhớ lâu."
        b.btnChinh.visibility = View.GONE
    }

    // ------------------------------------------------------------------- chot

    private var phutVuaTra = 0

    /**
     * Ghi ca luot xuong so va cap gio. Goi bao nhieu lan cung chi an mot lan.
     *
     * So phut tinh MOT LAN cho ca luot, khong cong don tung the: [HocThuoc.phutCho]
     * chia so the dung cho ba, va chia tung the mot thi ba the dung ra khong phut
     * nao (moi the duoc 0). Tran ngay cung phai hoi mot lan tai day, sau khi da biet
     * ca luot duoc bao nhieu.
     */
    private fun chot() {
        if (daChot || ketQua.isEmpty()) return
        daChot = true

        val kho = KhoBai.get(this)
        val moc = moc0Gio()
        val dung = ketQua.count { it.dung }
        val phut = HocThuoc.phutCho(dung, kho.phutTheTu(moc))
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
        ketQua.forEach { t ->
            val cua = if (t.dung && conGan > 0) conGan.also { conGan = 0 } else 0
            kho.ghiTraThe(t.copy(phut = cua))
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
            gate.approve(wantedMinutes = phut, useQuota = true, nhanCho = "Học thuộc")
        }
        DayLog.add(this, "Học thuộc $ten: $soThe thẻ đúng, +$phut phút")
        runCatching {
            Notifier.send(
                this,
                "${getString(R.string.child_name)} học thuộc $ten: $soThe thẻ đúng, " +
                    "được $phut phút."
            )
        }
        runCatching { DongBo.dayNgay() }
        ApprovalService.ensureRunning(this)
    }

    // ------------------------------------------------------------------- linh tinh

    /** Thanh ky tu toan, chep cach lam cua [SoatBaiActivity.veDaiToan]. */
    private fun veDaiToan() {
        KY_TU_TOAN.forEach { ky ->
            val nut = MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = ky
                textSize = 17f
                minWidth = 48.dp()
                minimumWidth = 48.dp()
                setPadding(10.dp(), 0, 10.dp(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 44.dp()
                ).apply { marginEnd = 6.dp() }
                // Bam nut khong duoc cuop con tro khoi o dang go.
                isFocusable = false
                setOnClickListener { chen(ky) }
            }
            b.nutToan.addView(nut)
        }
    }

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

    private companion object {
        /** Y het ban ben [SoatBaiActivity]: du cho Toan 8. */
        val KY_TU_TOAN = listOf(
            "^", "²", "³", "√", "∛", "/", "·", "−", "≈", "≠", "≤", "≥",
            "°", "∠", "Δ", "∥", "⊥", "π", "(", ")"
        )
    }
}
