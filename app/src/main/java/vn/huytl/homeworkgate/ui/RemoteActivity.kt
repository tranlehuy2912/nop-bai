package vn.huytl.homeworkgate.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.databinding.ActivityRemoteBinding
import vn.huytl.homeworkgate.dongbo.KetNoiRemote
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.dongbo.Laptop
import vn.huytl.homeworkgate.dongbo.Nguoi

/**
 * Man Remote tivi tren tablet (11/10/2026): Le Hoa cam tablet dieu khien laptop dang chieu len
 * tivi. Anh Huy chot lam giong man Remote cua Bang dieu khien (mau B), mo tu nut Remote o man
 * Xem phim. Lenh di thang qua Wi-Fi nha toi dich vu netflix-remote tren laptop ([KetNoiRemote]),
 * khong qua Firestore; tablet khong bat Wi-Fi nha thi khong noi duoc. Remote lam duoc dung nhung
 * gi ban phim, chuot cam vao laptop lam duoc, gio xem van do laptop dem.
 *
 * Code chep tu RemoteActivity cua Bang dieu khien, chi doi ten view, chu va mau cho hop app nay;
 * sua mot ben thi xem ben kia. Luc dung remote mat nhin tivi, nen: bam nut nao cung rung nhe, nut
 * am luong o canh tablet chinh tieng tivi khi da noi, va man khong tu tat trong luc mo.
 */
class RemoteActivity : AppCompatActivity() {

    private lateinit var b: ActivityRemoteBinding
    private lateinit var remote: Laptop.RemoteLaptop
    private var ketNoi: KetNoiRemote? = null
    private var lanAmCuoi = 0L
    private var vuaTat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRemoteBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val le = (28 * resources.displayMetrics.density).toInt()
            view.updatePadding(top = thanh.top + le, bottom = thanh.bottom)
            insets
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        remote = Laptop.RemoteLaptop(
            intent.getStringExtra(EXTRA_IP).orEmpty(),
            intent.getIntExtra(EXTRA_CONG, 0),
            intent.getStringExtra(EXTRA_KHOA).orEmpty()
        )

        b.nutVe.setOnClickListener { finish() }
        b.nhanNoi.setOnClickListener {
            if (ketNoi?.trangThai != KetNoiRemote.TrangThai.DA_NOI) noiLai()
        }
        b.banDiChuot.khiDi = { x, y -> ketNoi?.diChuot(x, y) }
        b.banDiChuot.khiBam = { neuDaNoi { it.bamChuot() } }
        b.banDiChuot.khiCuon = { n -> ketNoi?.cuon(n) }

        nut(b.nutPhat) { it.phim("space") }
        nut(b.nutLui) { it.phim("Left") }
        nut(b.nutTien) { it.phim("Right") }
        nut(b.nutBoQua) { it.phim("s") }
        nut(b.nutNho) { it.amLuong(false) }
        nut(b.nutTo) { it.amLuong(true) }
        b.nutBanPhim.setOnClickListener { neuDaNoi { hopGoChu() } }
        b.nutManChon.setOnClickListener { neuDaNoi { hoiVeManChon() } }
        // Di thang qua ket noi remote nhu cac nut khac (anh Huy chot: tablet luon o nha, khong can
        // Firestore). Chua noi thi chi bao ly do.
        b.nutDangXuat.setOnClickListener { neuDaNoi { hoiDangXuat() } }
        b.nutTatMay.setOnClickListener { neuDaNoi { hoiTatMay() } }
        veTrangThai(KetNoiRemote.TrangThai.DANG_NOI)
    }

    override fun onStart() {
        super.onStart()
        noiLai()
    }

    override fun onStop() {
        super.onStop()
        ketNoi?.dong()
        ketNoi = null
    }

    private fun noiLai() {
        ketNoi?.dong()
        lateinit var k: KetNoiRemote
        k = KetNoiRemote(
            remote,
            khiDoi = { t -> if (ketNoi === k) veTrangThai(t) },
            khiAm = { pt, tat -> if (ketNoi === k) veAm(pt, tat) },
            khiBao = { chu -> if (ketNoi === k) noi(chu) }
        )
        ketNoi = k
        k.noi()
    }

    /** Nut am luong o canh dien thoai chinh tieng tivi khi da noi; chua noi thi chinh dien thoai. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val tang = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> true
            KeyEvent.KEYCODE_VOLUME_DOWN -> false
            else -> return super.onKeyDown(keyCode, event)
        }
        val k = ketNoi?.takeIf { it.daNoi } ?: return super.onKeyDown(keyCode, event)
        // Giu nut thi Android lap lai rat nhanh, moi lan laptop phai goi pactl: gioi han lai.
        val bayGio = SystemClock.uptimeMillis()
        if (bayGio - lanAmCuoi >= AM_CACH_MS) {
            lanAmCuoi = bayGio
            k.amLuong(tang)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            ketNoi?.daNoi == true
        ) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun nut(v: View, lam: (KetNoiRemote) -> Unit) {
        v.setOnClickListener {
            neuDaNoi {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                lam(it)
            }
        }
    }

    private fun neuDaNoi(lam: (KetNoiRemote) -> Unit) {
        val k = ketNoi
        if (k != null && k.daNoi) lam(k) else noi(getString(R.string.remote_bao_chua_noi))
    }

    private fun veTrangThai(t: KetNoiRemote.TrangThai) {
        val (chu, mauChu, mauNen) = when (t) {
            KetNoiRemote.TrangThai.DANG_NOI -> Triple(R.string.remote_dang_noi, R.color.ink_soft, R.color.wait_soft)
            KetNoiRemote.TrangThai.DA_NOI -> Triple(R.string.remote_da_noi, R.color.ok, R.color.ok_soft)
            KetNoiRemote.TrangThai.KHONG_NOI_DUOC -> Triple(R.string.remote_chua_noi, R.color.alert, R.color.alert_soft)
            KetNoiRemote.TrangThai.SAI_MA -> Triple(R.string.remote_sai_ma, R.color.alert, R.color.alert_soft)
        }
        b.nhanNoi.setText(chu)
        b.nhanNoi.setTextColor(ContextCompat.getColor(this, mauChu))
        b.nhanNoi.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, mauNen))
        val mo = if (t == KetNoiRemote.TrangThai.DA_NOI) 1f else 0.4f
        for (v in listOf(
            b.khungDiChuot, b.nutBoQua, b.nutLui, b.nutPhat, b.nutTien, b.nutNho, b.nutTo,
            b.nutBanPhim, b.nutManChon, b.nutDangXuat, b.nutTatMay
        )) v.alpha = mo
        when (t) {
            // Vua bam Tat may thi mat ket noi la dung, khong bao "chua noi duoc" nua.
            KetNoiRemote.TrangThai.KHONG_NOI_DUOC -> if (!vuaTat) noi(getString(R.string.remote_bao_chua_noi))
            KetNoiRemote.TrangThai.SAI_MA -> noi(getString(R.string.remote_bao_sai_ma))
            KetNoiRemote.TrangThai.DA_NOI -> {}
            KetNoiRemote.TrangThai.DANG_NOI -> veAm(null, false)
        }
    }

    private fun veAm(pt: Int?, tat: Boolean) {
        b.chuAm.text = if (pt == null) "--" else "$pt%"
        b.iconLoa.setImageResource(if (tat) R.drawable.st_ic_loa_tat else R.drawable.st_ic_loa)
    }

    /**
     * Hop go chu: go den dau gui den do, nhu ban phim cam vao laptop. Chu xoa thi gui bay nhieu lan
     * xoa lui, nen bo go tieng Viet doi "a" thanh "á" cung ra dung tren tivi. Enter gui phim Enter.
     */
    private fun hopGoChu() {
        val o = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEND
            isSingleLine = true
            hint = getString(R.string.remote_go_goi_y)
        }
        val khung = FrameLayout(this).apply {
            val le = (20 * resources.displayMetrics.density).toInt()
            setPadding(le, le / 2, le, 0)
            addView(o)
        }
        var daGui = ""
        o.doAfterTextChanged { e ->
            val k = ketNoi?.takeIf { it.daNoi } ?: return@doAfterTextChanged
            val moi = e?.toString().orEmpty()
            val chung = daGui.commonPrefixWith(moi).length
            repeat((daGui.length - chung).coerceAtMost(XOA_TOI_DA)) { k.phim("BackSpace") }
            k.go(moi.substring(chung))
            daGui = moi
        }
        val hop = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remote_go_title)
            .setView(khung)
            .setPositiveButton(R.string.remote_go_enter) { _, _ -> ketNoi?.phim("Return") }
            .setNegativeButton(R.string.remote_go_xong, null)
            .create()
        o.setOnEditorActionListener { _, _, _ ->
            ketNoi?.phim("Return")
            hop.dismiss()
            true
        }
        hop.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        hop.show()
        o.requestFocus()
    }

    private fun noi(chu: String) = Toast.makeText(this, chu, Toast.LENGTH_SHORT).show()

    private fun hoiDangXuat() {
        val ten = Laptop.phien(this)
        if (ten.isEmpty()) {
            noi(getString(R.string.remote_chua_ai_dang_nhap))
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remote_dang_xuat_title)
            .setMessage(
                if (ten == Nguoi.LE_HOA) R.string.remote_dang_xuat_xem_phim else R.string.remote_dang_xuat_admin
            )
            .setPositiveButton(R.string.remote_dang_xuat) { _, _ -> gui(tat = false) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun hoiTatMay() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remote_tat_may_title)
            .setMessage(R.string.remote_tat_may_noi_dung)
            .setPositiveButton(R.string.remote_tat_may) { _, _ -> gui(tat = true) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Gui Dang xuat hay Tat may qua ket noi remote; laptop lam ngay va tra cau bao. Ghi them mot
     * dong Nhat ky (sang the Nhat ky cua Bang dieu khien) de Ba Huy biet laptop tat tu tablet.
     */
    private fun gui(tat: Boolean) {
        val k = ketNoi?.takeIf { it.daNoi } ?: return noi(getString(R.string.remote_bao_chua_noi))
        if (tat) {
            vuaTat = true
            k.tatMay()
        } else {
            k.dangXuat()
        }
        DayLog.add(this, if (tat) "Tắt laptop từ tablet" else "Đăng xuất laptop từ tablet")
        DongBo.dayNgay()
    }

    private fun hoiVeManChon() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remote_ve_title)
            .setMessage(R.string.remote_ve_noi_dung)
            .setPositiveButton(R.string.remote_ve_nut) { _, _ -> ketNoi?.veManChon() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val EXTRA_IP = "ip"
        private const val EXTRA_CONG = "cong"
        private const val EXTRA_KHOA = "khoa"
        private const val AM_CACH_MS = 150L
        private const val XOA_TOI_DA = 50

        fun mo(context: Context, r: Laptop.RemoteLaptop) {
            context.startActivity(
                Intent(context, RemoteActivity::class.java)
                    .putExtra(EXTRA_IP, r.ip)
                    .putExtra(EXTRA_CONG, r.cong)
                    .putExtra(EXTRA_KHOA, r.khoa)
            )
        }
    }
}
