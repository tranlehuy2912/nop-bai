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
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.data.LuotDangLam
import vn.huytl.homeworkgate.databinding.StActivityLamBaiBinding
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Man lam bai may giao ngay tren tablet: lam them, on lai, luyen cho hay vap.
 *
 * MOI LAN MOT CAU. Sao cua cau o goc tren, sang la con, xam la da mat; bam Kiem tra sai thi
 * mot sao tat. Dung thi hien "+N" canh nut - do la phut vua duoc (hay vua vao quy, con khong
 * can biet). Khong co chu nao giai thich luat: Ba Huy chot ngay 29/9/2026 de con tu kham pha.
 * Luat o [LuatGhep], chon cau va cong phut o [LamTrenMay].
 *
 * THOAT GIUA CHUNG THI GIU NGUYEN LUOT. Moi lan bam Kiem tra sai, luot do (sao con, so lan
 * sai) duoc ghi lai ngay ([LuotDangLam]); vao lai cau do la lam tiep dung cho cu. Thoat ra vao
 * lai khong xoa duoc lan sai nao, ma man hinh tu tat luc con dang nghi cung khong bi phat.
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
        b.tieuDe.text = tenMan()
        b.phuDe.text = "Đang lấy câu…"
        hoiMocRoiTai()
    }

    private fun tenMan(): String = when (loai) {
        LamTrenMay.Loai.ON -> "Ôn lại câu đến hẹn"
        LamTrenMay.Loai.LUYEN -> "Luyện chỗ hay vấp"
        else -> "Làm bài ${GiaiDe.tenMon(mon)}"
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
            val ds = withContext(Dispatchers.IO) {
                runCatching {
                    NganHang.napNeuCan(ct)
                    when (loai) {
                        LamTrenMay.Loai.ON -> LamTrenMay.cauOn(ct)
                        LamTrenMay.Loai.LUYEN -> LamTrenMay.cauLuyen(ct, nhan)
                        else -> LamTrenMay.cauLamThem(ct, mon)
                    }
                }.getOrDefault(emptyList())
            }
            cac = ds
            vt = 0
            if (cac.isEmpty()) {
                b.phuDe.text = when (loai) {
                    LamTrenMay.Loai.ON -> "Hôm nay chưa có câu nào đến hẹn."
                    LamTrenMay.Loai.LUYEN -> "Chưa có câu nào để luyện."
                    else -> "Hết câu trong phần lớp đã học. Học bài mới thì chọn lại mốc."
                }
                b.nutKiem.visibility = View.GONE
                return@launch
            }
            veCau()
        }
    }

    private fun veCau() {
        val m = cac[vt]
        luot = LuotDangLam.lay(this, m.cau.id, m.ghep.sao) ?: LuatGhep.Luot(m.ghep.sao)
        b.tieuDe.text = "Câu ${vt + 1}/${cac.size}"
        b.phuDe.text = listOf(NganHang.sachTheoNguon(m.cau.nguon)?.ten.orEmpty(), m.cau.nhan())
            .filter { it.isNotBlank() }.joinToString(" · ")
        b.de.removeAllViews()
        KhungGhep.veDe(b.de, m)
        khung = KhungGhep(b.khung, m) { b.ket.text = "" }.also { it.ve(luot.botNhieu) }
        b.nutKiem.visibility = View.VISIBLE
        b.nutTiep.visibility = View.GONE
        b.ket.text = ""
        veSao()
        b.khungCuon.post { b.khungCuon.scrollTo(0, 0) }
    }

    /** Sao sang la con, sao rong la da mat. Het sao ma con thu them thi rong het. */
    private fun veSao() {
        val con = if (luot.xong && !luot.dung) 0 else luot.sao
        b.sao.text = "★".repeat(con) + "☆".repeat((luot.saoToiDa - con).coerceAtLeast(0))
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
        if (!luot.xong) {
            LuotDangLam.ghi(this, k.muc.cau.id, luot)
            k.ve(luot.botNhieu)
            b.ket.text = ""
            return
        }
        k.khoa(luot.hienLoiGiai)
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

    /** Roi han man thi ghi mot dong nhat ky cho ca luot. Luot dang do da nam o [LuotDangLam]. */
    override fun onStop() {
        super.onStop()
        if (isFinishing) LamTrenMay.ghiNhatKy(applicationContext, mon, loai, daGhi.toList())
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
