package vn.huytl.homeworkgate.guard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.LoiNhac
import vn.huytl.homeworkgate.data.TinhLoiNhac
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Man chan che kin man hinh trong suot buoi hoc.
 *
 * Khac han the nhac nho: the nhac chi noi mot cau roi rut di, con man nay nam ly
 * tu luc phai buong may cho den khi tan hoc. Le Hoa khong tat duoc; Ba Huy mo bang
 * lenh Telegram, hoac mo app Nop bai roi vao o khoa goc tren.
 *
 * Man nay nhuong cho chinh app Nop bai: mo app ra la no an di. Khong phai lo hong -
 * trong app do khong co gi de choi, ma lai co dung viec dang can lam la soan cap.
 * Buoc ra khoi app mot cai la no che lai ngay.
 *
 * Lo hong cua rieng lop phu - vao Cai dat tat quyen hien tren ung dung khac la
 * man chan bien mat - thi ben app nay da co [GuardAccessibilityService] bit lai:
 * no chan luon app Cai dat khi [vn.huytl.homeworkgate.data.Prefs.lockSystemSettings]
 * dang bat. Do la cai app Soan tap chay rieng khong lam duoc, va cung la ly do
 * dang nhat de ghep hai app lam mot.
 */
class ManChan(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val contextCoTheme = ContextThemeWrapper(context, R.style.Theme_HomeworkGate)
    private val dinhDangGio = SimpleDateFormat("HH:mm:ss", Locale("vi", "VN"))

    private var view: View? = null

    /** Cham vao man chan. Mo app Nop bai, khong mo khoa may. */
    var khiBam: (() -> Unit)? = null

    val dangHien: Boolean get() = view != null

    fun coQuyen(): Boolean = Settings.canDrawOverlays(context)

    fun hien(nhac: LoiNhac) {
        if (!coQuyen()) return
        handler.post { hienTrenMain(nhac) }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun hienTrenMain(nhac: LoiNhac) {
        if (view == null) {
            val moi = LayoutInflater.from(contextCoTheme).inflate(R.layout.st_man_chan, null)

            // Nuot phim Back. Khong nuot thi Le Hoa bam Back mot cai la thoat man chan.
            moi.isFocusableInTouchMode = true
            moi.setOnKeyListener { _, ma, _ -> ma == KeyEvent.KEYCODE_BACK }
            // Khong nuot cham o lop goc: cua so nay khong dat NOT_TOUCH_MODAL nen
            // da giu tron moi cham roi, khong co gi xuyen xuong duoi. Nuot them o
            // day thi nuot luon cai cham mo app Nop bai.

            val them = runCatching { windowManager.addView(moi, thongSo()) }.isSuccess
            if (!them) return
            view = moi
            moi.requestFocus()
        }

        val v = view ?: return

        v.findViewById<TextView>(R.id.tieu_de_chan).text = nhac.tieuDe
        v.findViewById<TextView>(R.id.phu_de_chan).text = nhac.chiTiet
        v.findViewById<TextView>(R.id.dong_ho).text = dinhDangGio.format(Date())

        if (nhac.phutHetChan >= 0) {
            v.findViewById<TextView>(R.id.het_chan_luc).text =
                "Mở lại lúc ${TinhLoiNhac.gioPhut(nhac.phutHetChan)}"
        }

        veDanhSachMon(v, nhac)

        // Cham vao dau cung mo app Nop bai, y het lop phu "Het gio roi" cua app chu.
        // Khong mo khoa gi ca: trong app do co viec that de lam (soan cap), va co o
        // khoa goc tren cho Ba Huy go PIN.
        v.setOnClickListener { khiBam?.invoke() }
    }

    private fun veDanhSachMon(v: View, nhac: LoiNhac) {
        val khung = v.findViewById<LinearLayout>(R.id.khung_mon_chan)
        val danhSach = v.findViewById<LinearLayout>(R.id.danh_sach_mon_chan)
        val nhan = v.findViewById<TextView>(R.id.nhan_mon_chan)
        val buoi = nhac.buoi

        if (buoi == null) {
            khung.visibility = View.GONE
            return
        }
        khung.visibility = View.VISIBLE
        danhSach.removeAllViews()

        val mon = buoi.monCanSoan
        if (mon.isEmpty()) {
            nhan.text = "HÔM NAY"
            themDong(danhSach, "Có tiết học thể dục, nhớ mặc đồ")
        } else {
            nhan.text = "NHỚ MANG THEO"
            mon.forEach { themDong(danhSach, "•  $it") }
            if (buoi.coTheDuc) themDong(danhSach, "•  Đồ thể dục")
        }
    }

    private fun themDong(cha: LinearLayout, chu: String) {
        val dong = LayoutInflater.from(contextCoTheme)
            .inflate(R.layout.st_dong_mon_chan, cha, false) as TextView
        dong.text = chu
        cha.addView(dong)
    }

    /** Cap nhat dong ho moi giay, de man chan khong trong nhu mot buc anh chet. */
    fun capNhatDongHo() {
        val v = view ?: return
        handler.post {
            v.findViewById<TextView>(R.id.dong_ho)?.text = dinhDangGio.format(Date())
        }
    }

    fun an() {
        handler.post {
            val v = view ?: return@post
            runCatching { windowManager.removeView(v) }
            view = null
        }
    }

    private fun thongSo(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Khong dat NOT_FOCUSABLE: man nay phai nhan duoc phim de nuot nut Back.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        )
}
