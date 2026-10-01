package vn.huytl.homeworkgate.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.CauBoQua
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.data.LuotDangLam
import vn.huytl.homeworkgate.databinding.StActivityLamBaiBinding
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.DeThi
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Man lam bai may giao ngay tren tablet: lam them, on lai, luyen cho hay vap.
 *
 * MOI LAN MOT CAU. Sao cua cau o goc tren, ba dang ([HangSao], Ba Huy chot 30/9/2026): vao cau
 * la ca hang rong, bam Kiem tra sai thi mot o chuyen xam, lam xong dung thi cac o con lai chuyen
 * vang va sang len mot nhip. Dung thi hien "+N" canh nut - do la phut vua duoc (hay vua vao quy,
 * con khong can biet). Lam lai thi duoi hang sao co dong "Lần trước" la sao tot nhat cua vong
 * ([LamTrenMay.saoLanTruoc]), nen "+N" bang dung so sao vuot dong do. Khong co chu nao giai
 * thich luat: Ba Huy chot ngay 29/9/2026 de con tu kham pha.
 * Luat o [LuatGhep], chon cau va cong phut o [LamTrenMay].
 *
 * THOAT GIUA CHUNG THI GIU NGUYEN LUOT. Moi lan bam Kiem tra sai, luot do (sao con, so lan
 * sai) duoc ghi lai ngay ([LuotDangLam]); vao lai cau do la lam tiep dung cho cu. Thoat ra vao
 * lai khong xoa duoc lan sai nao, ma man hinh tu tat luc con dang nghi cung khong bi phat.
 *
 * CAU TIEP KHI CHUA LAM XONG (Ba Huy chot 30/9/2026). Gap cau khong biet lam thi truoc day con
 * dung o do hoai. Nay nut vien "Câu tiếp" nam canh Kiem tra suot luc lam: bam la sang cau sau,
 * cau nay coi nhu chua lam - khong ghi so cai, khong cap phut, luot sau lui ra cuoi ([CauBoQua]).
 * Cau dang do ma bo qua thi sao da mat van mat, nhu thoat man giua chung.
 *
 * CAU DE THI CO HINH AN (1/10/2026). Cau cua de Toan, KHTN quay lai day mot minh (lam lai khi
 * de het han, on lai khi toi hen), khong co the dau bai nhu trong de. Bai hinh ma to de khong in
 * hinh thi duoi chu cua cau co cung khoi hinh an voi man Giai de ([KhungGhep.veHinhAn]): dong
 * nhac tu ve, nut "Nhờ trợ giúp" mo hinh goi y roi hinh day du, ghi nhat ky (moi bai moi muc mot
 * lan trong ngay), khong tru sao. Muc da mo giu theo tung cau trong [nhoHinh] cho toi khi con
 * roi man.
 */
class LamBaiActivity : AppCompatActivity() {

    private lateinit var b: StActivityLamBaiBinding
    private lateinit var loai: LamTrenMay.Loai
    private var mon = ""
    private var nhan = ""

    private var cac: List<LamTrenMay.Muc> = emptyList()
    private var vt = 0
    private var luot = LuatGhep.Luot(1)
    private var khung: KhungGhep? = null
    private var dangGhi = false
    private val daGhi = mutableListOf<LamTrenMay.Ghi>()
    private val cauBoQua = mutableListOf<CauHoi>()

    /** Hinh an cua cac cau de thi trong luot nay, theo id cau. Doc mot lan trong [tai]. */
    private var hinhAn: Map<String, DeThi.HinhAn> = emptyMap()

    /** Nhan cua cac cau de thi ("Bài 3a, Đề giữa kì 1 số 2"), theo id cau. Doc cung luc [hinhAn]. */
    private var nhanDe: Map<String, String> = emptyMap()

    /**
     * Muc hinh an cua tung cau, khoa theo id cau (1/10/2026). Cat vao Bundle khi Activity bi dung
     * lai; con roi man thi hinh an lai. Xem [KhungGhep.NhoHinhAn].
     */
    private val nhoHinh = KhungGhep.NhoHinhAn()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = StActivityLamBaiBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
        loai = runCatching { LamTrenMay.Loai.valueOf(intent.getStringExtra(EXTRA_LOAI).orEmpty()) }
            .getOrDefault(LamTrenMay.Loai.LAM_THEM)
        mon = intent.getStringExtra(EXTRA_MON).orEmpty()
        nhan = intent.getStringExtra(EXTRA_NHAN).orEmpty()
        b.nutQuayLai.setOnClickListener { finish() }
        b.nutKiem.setOnClickListener { kiem() }
        b.nutTiep.setOnClickListener { tiep() }
        b.nutBoQua.setOnClickListener { boQuaCau() }
        b.tieuDe.text = tenMan()
        b.phuDe.text = "Đang lấy câu…"
        nhoHinh.doc(savedInstanceState)
        hoiMocRoiTai()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nhoHinh.luu(outState)
    }

    private fun tenMan(): String = when (loai) {
        LamTrenMay.Loai.ON -> "Ôn lại câu đến hẹn"
        LamTrenMay.Loai.LUYEN -> "Luyện chỗ hay vấp"
        else -> "Luyện tập ${GiaiDe.tenMon(mon)}"
    }

    /**
     * Lam them phai biet lop da hoc toi dau (Ba Huy dan chi lay cau trong phan da hoc). Chua
     * chon moc thi hoi truoc, roi moi lay cau.
     */
    private fun hoiMocRoiTai() {
        if (loai != LamTrenMay.Loai.LAM_THEM) return tai()
        if (mon == PhanHoc.TIENG_ANH) {
            if (HocToi.unitCua(this, PhanHoc.BO_TIENG_ANH) == null) return hoiUnit()
            return tai()
        }
        if (PhanHoc.chuaChon(this, mon).isNotEmpty()) {
            ChonHocToi.hoiPhanConThieu(this, mon) { tai() }
            return
        }
        tai()
    }

    private fun hoiUnit() {
        val bo = BoTuVung.BO.firstOrNull { it.bo == PhanHoc.BO_TIENG_ANH } ?: return tai()
        val nhanUnit = arrayOf("Chưa học Unit nào") + (1..12).map { "Unit $it" }
        MaterialAlertDialogBuilder(this)
            .setTitle("Lớp đã học tới Unit nào?")
            .setItems(nhanUnit) { _, i ->
                HocToi.datUnit(this, bo, i)
                tai()
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun tai() {
        lifecycleScope.launch {
            val ct = this@LamBaiActivity
            val (ds, hinh, nhan) = withContext(Dispatchers.IO) {
                val chon = runCatching {
                    NganHang.napNeuCan(ct)
                    when (loai) {
                        LamTrenMay.Loai.ON -> LamTrenMay.cauOn(ct)
                        LamTrenMay.Loai.LUYEN -> LamTrenMay.cauLuyen(ct, nhan)
                        else -> LamTrenMay.cauLamThem(ct, mon)
                    }
                }.getOrDefault(emptyList())
                // Hinh an tim o day, ngoai luong giao dien: lan dau goi, DeThi doc ca cac file bo
                // de tu assets. File de hong thi cau van lam duoc, chi khong co nut tro giup.
                val cuaCau = runCatching {
                    chon.mapNotNull { m -> DeThi.hinhAnCua(ct, m.cau.id)?.let { m.cau.id to it } }.toMap()
                }.getOrDefault(emptyMap())
                val nhanCau = runCatching {
                    chon.mapNotNull { m -> DeThi.nhanCau(ct, m.cau.id)?.let { m.cau.id to it } }.toMap()
                }.getOrDefault(emptyMap())
                Triple(chon, cuaCau, nhanCau)
            }
            cac = ds
            hinhAn = hinh
            nhanDe = nhan
            vt = 0
            if (cac.isEmpty()) {
                b.phuDe.text = when (loai) {
                    LamTrenMay.Loai.ON -> "Hôm nay chưa có câu nào đến hẹn."
                    LamTrenMay.Loai.LUYEN -> "Chưa có câu nào để luyện."
                    else -> "Hết câu trong phần lớp đã học. Học bài mới thì chọn lại mốc."
                }
                b.nutKiem.visibility = View.GONE
                b.nutBoQua.visibility = View.GONE
                return@launch
            }
            veCau()
        }
    }

    private fun veCau() {
        val m = cac[vt]
        luot = LuotDangLam.lay(this, m.cau.id, m.ghep.sao) ?: LuatGhep.Luot(m.ghep.sao)
        b.tieuDe.text = "Câu ${vt + 1}/${cac.size}"
        b.phuDe.text = listOf(NganHang.sachTheoNguon(m.cau.nguon)?.ten.orEmpty(), nhanDe[m.cau.id] ?: m.cau.nhan())
            .filter { it.isNotBlank() }.joinToString(" · ")
        b.de.removeAllViews()
        KhungGhep.veDe(b.de, m)
        hinhAn[m.cau.id]?.let { ha -> KhungGhep.veHinhAn(b.de, ha.de, ha.muc, m.cau.id, nhoHinh) }
        khung = KhungGhep(b.khung, m) { b.ket.text = "" }.also { it.ve(luot.botNhieu) }
        b.nutKiem.visibility = View.VISIBLE
        b.nutTiep.visibility = View.GONE
        b.nutBoQua.visibility = View.VISIBLE
        b.nutBoQua.text = if (vt + 1 < cac.size) "Câu tiếp" else "Xong"
        b.ket.text = ""
        veSao()
        val truoc = LamTrenMay.saoLanTruoc(this, m, loai)
        b.saoTruoc.visibility = if (truoc == null) View.GONE else View.VISIBLE
        b.saoTruoc.text = truoc?.let { HangSao.chuLanTruoc(this, it, m.ghep.sao) } ?: ""
        b.khungCuon.post { b.khungCuon.scrollTo(0, 0) }
    }

    /** Ba dang sao, xem [HangSao]. Het sao ma con thu them, hay xong ma sai, thi xam het. */
    private fun veSao() {
        b.sao.text = HangSao.chu(this, luot)
    }

    private fun kiem() {
        val k = khung ?: return
        if (luot.xong || dangGhi) return
        val soSai = k.kiem()
        if (soSai == null) {
            b.ket.setTextColor(ContextCompat.getColor(this, R.color.ink_soft))
            b.ket.text = "Chưa làm xong câu này"
            return
        }
        luot = LuatGhep.kiem(luot, soSai)
        veSao()
        if (luot.xong && luot.dung && luot.sao > 0) HangSao.nhip(b.sao)
        if (!luot.xong) {
            LuotDangLam.ghi(this, k.muc.cau.id, luot)
            k.ve(luot.botNhieu)
            b.ket.text = ""
            return
        }
        k.khoa(luot.hienLoiGiai)
        b.nutBoQua.visibility = View.GONE
        ghiLuot(k)
    }

    private fun ghiLuot(k: KhungGhep) {
        dangGhi = true
        val m = k.muc
        val lu = luot
        val traLoi = k.traLoi()
        lifecycleScope.launch {
            val ghi = withContext(Dispatchers.IO) {
                runCatching { LamTrenMay.ghi(this@LamBaiActivity, m, lu, traLoi, loai) }.getOrNull()
            }
            dangGhi = false
            ghi?.let {
                daGhi += it
                LuotDangLam.xoa(this@LamBaiActivity, m.cau.id)
            }
            val phut = (ghi?.phutCap ?: 0) + (ghi?.phutQuy ?: 0)
            b.ket.setTextColor(ContextCompat.getColor(this@LamBaiActivity, if (lu.dung) R.color.ok else R.color.ink_soft))
            b.ket.text = if (lu.dung) "+$phut" else "+0"
            b.nutKiem.visibility = View.GONE
            b.nutTiep.visibility = View.VISIBLE
            b.nutTiep.text = if (vt + 1 < cac.size) "Câu tiếp" else "Xong"
        }
    }

    private fun tiep() {
        if (vt + 1 >= cac.size) return finish()
        vt++
        veCau()
    }

    /**
     * Con chua lam duoc cau nay: sang cau sau, cau nay coi nhu chua lam. Khong ghi so cai; luot
     * dang do (neu con da sai) van nam o [LuotDangLam]. Xem [CauBoQua].
     */
    private fun boQuaCau() {
        if (luot.xong || dangGhi || cac.isEmpty()) return
        val m = cac[vt]
        CauBoQua.ghi(this, m.cau.id)
        cauBoQua += m.cau
        tiep()
    }

    /** Roi han man thi ghi mot dong nhat ky cho ca luot. Luot dang do da nam o [LuotDangLam]. */
    override fun onStop() {
        super.onStop()
        if (isFinishing) LamTrenMay.ghiNhatKy(applicationContext, mon, loai, daGhi.toList(), cauBoQua.toList())
    }

    companion object {
        const val EXTRA_LOAI = "loai"
        const val EXTRA_MON = "mon"
        const val EXTRA_NHAN = "nhan"

        fun moLamThem(context: Context, mon: String) = context.startActivity(
            Intent(context, LamBaiActivity::class.java)
                .putExtra(EXTRA_LOAI, LamTrenMay.Loai.LAM_THEM.name)
                .putExtra(EXTRA_MON, mon)
        )

        fun moOn(context: Context) = context.startActivity(
            Intent(context, LamBaiActivity::class.java).putExtra(EXTRA_LOAI, LamTrenMay.Loai.ON.name)
        )

        fun moLuyen(context: Context, nhan: String) = context.startActivity(
            Intent(context, LamBaiActivity::class.java)
                .putExtra(EXTRA_LOAI, LamTrenMay.Loai.LUYEN.name)
                .putExtra(EXTRA_NHAN, nhan)
        )
    }
}
