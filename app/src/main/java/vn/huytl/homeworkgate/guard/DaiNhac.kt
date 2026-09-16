package vn.huytl.homeworkgate.guard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ContextThemeWrapper
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.LoaiNhac
import vn.huytl.homeworkgate.data.LoiNhac
import vn.huytl.homeworkgate.ui.SoanActivity

/**
 * Dai nhac troi vao mep tren man hinh.
 *
 * Khong mo Activity de bao, vi lam vay la cuop man hinh giua luc Le Hoa dang choi.
 * Dai nay nam de len tren, Le Hoa van bam duoc xuong app ben duoi, liec mat la doc
 * xong. Rieng luc sap tre hoc thi no o lai khong tu thu.
 */
class DaiNhac(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Context cua Service khong mang theme nao, nen moi thuoc tinh dang ?attr
     * trong layout deu khong giai duoc va inflate no tung ngay. Boc theme cua app
     * vao day roi hay dung de dung view.
     */
    private val contextCoTheme = ContextThemeWrapper(context, R.style.Theme_HomeworkGate)
    private val handler = Handler(Looper.getMainLooper())

    private var view: View? = null
    private var dangHien: LoiNhac? = null

    /** Loi nhac vua tu rut, va luc rut, de biet dang trong ky nghi cua no. */
    private var nhacVuaRut: String? = null
    private var lucRut = 0L

    private val tuAn = Runnable {
        dangHien?.let {
            nhacVuaRut = khoa(it)
            lucRut = SystemClock.elapsedRealtime()
        }
        an()
    }

    fun coQuyen(): Boolean = Settings.canDrawOverlays(context)

    fun hien(nhac: LoiNhac) {
        if (!coQuyen()) return
        if (dangNghi(nhac)) return
        handler.post { hienTrenMain(nhac) }
    }

    /**
     * Vua bay ra xong thi nghi mot lat, dung bay lai ngay.
     *
     * Khong co doan nay thi the bay ra 30 giay, rut di, roi nhip sau lai bay ra -
     * nghia la nua thoi gian man hinh cua Le Hoa co mot the nam de len tren. Nhac
     * dai nhu the thi thanh ra nhin mai quen mat, dung nguoc voi viec minh muon.
     *
     * Hai truong hop khong nghi:
     *  - viec gap: no von khong tu rut, va cung khong duoc phep im;
     *  - doi sang loi nhac khac: do la tin moi, phai bao ngay.
     */
    private fun dangNghi(nhac: LoiNhac): Boolean {
        if (nhac.gap) return false
        if (khoa(nhac) != nhacVuaRut) return false
        return SystemClock.elapsedRealtime() - lucRut < NGHI_GIUA_HAI_LAN_MS
    }

    /** Nhan dien mot loi nhac, de biet lan bay sau co phai cung mot viec khong. */
    private fun khoa(nhac: LoiNhac) = "${nhac.loai}|${nhac.maBuoi}"

    /**
     * Bo ky nghi, cho loi nhac bay ra ngay o lan xet toi.
     *
     * Goi khi Le Hoa vua mo khoa may: do la mot lan cam may moi, va cung la luc dang
     * de nhac nhat. Khong co cho nay thi mo may luc 20:02 sau khi da thay the luc
     * 20:00 se khong thay gi ca.
     */
    fun batLaiChuKy() {
        nhacVuaRut = null
    }

    @SuppressLint("InflateParams")
    private fun hienTrenMain(nhac: LoiNhac) {
        handler.removeCallbacks(tuAn)

        var moiThem = false
        if (view == null) {
            val moi = LayoutInflater.from(contextCoTheme).inflate(R.layout.st_dai_nhac, null)
            val them = runCatching { windowManager.addView(moi, thongSo()) }.isSuccess
            if (!them) return
            view = moi
            moiThem = true
        }
        val v = view ?: return
        dangHien = nhac

        v.findViewById<TextView>(R.id.tieu_de).text = nhac.tieuDe
        v.findViewById<TextView>(R.id.chi_tiet).text = nhac.chiTiet
        v.findViewById<ImageView>(R.id.bieu_tuong).setImageResource(
            when (nhac.loai) {
                LoaiNhac.SOAN_VO -> R.drawable.st_ic_cap_sach
                LoaiNhac.SAP_DI_HOC -> R.drawable.st_ic_dong_ho
                LoaiNhac.DANG_GIO_HOC -> R.drawable.st_ic_dau_hoi
                // CHAN khong bao gio di qua day, no dung ManChan chu khong dung the nho
                LoaiNhac.CHAN -> R.drawable.st_ic_dong_ho
            }
        )
        v.findViewById<View>(R.id.dai).setBackgroundResource(
            if (nhac.gap) R.drawable.st_nen_the_nhac_gap else R.drawable.st_nen_the_nhac
        )
        v.findViewById<TextView>(R.id.chi_tiet).setTextColor(
            ContextCompat.getColor(
                context,
                if (nhac.gap) R.color.st_the_nhac_chu_mo_gap else R.color.st_the_nhac_chu_mo
            )
        )

        val nutMo = v.findViewById<TextView>(R.id.nut_mo)
        nutMo.visibility = if (nhac.loai == LoaiNhac.SOAN_VO) View.VISIBLE else View.GONE
        nutMo.setOnClickListener { moManSoan() }
        v.setOnClickListener { if (nhac.loai == LoaiNhac.SOAN_VO) moManSoan() }

        if (moiThem) {
            // Troi vao chu khong bat cai bup: dot ngot thi giat minh, ma muc dich
            // la keo mat Le Hoa chu khong phai doa Le Hoa.
            v.alpha = 0f
            v.translationY = -40f
            v.animate().alpha(1f).translationY(0f).setDuration(260L).start()
        }
        v.visibility = View.VISIBLE

        // Viec gap thi nam li. Viec thuong thi troi ra roi tu rut, khong an vay
        // man hinh cua Le Hoa.
        if (!nhac.gap) handler.postDelayed(tuAn, TU_AN_SAU_MS)
    }

    private fun moManSoan() {
        val intent = Intent(context, SoanActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        runCatching { ContextCompat.startActivity(context, intent, null) }
        an()
    }

    fun an() {
        handler.post {
            handler.removeCallbacks(tuAn)
            val v = view ?: return@post
            runCatching { windowManager.removeView(v) }
            view = null
            dangHien = null
        }
    }

    private fun thongSo(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_FOCUSABLE de Le Hoa van go phim duoc trong app ben duoi.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            y = cachMepTren()
        }

    /**
     * Day the xuong duoi thanh trang thai. Doc so do that cua may chu khong doan
     * mot con so co dinh, vi tai tho voi thanh trang thai moi doi mot khac.
     */
    private fun cachMepTren(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private companion object {
        /**
         * Bay ra xong thi nghi bao lau moi bay lai cung mot loi nhac.
         *
         * 5 phut: du lau de Le Hoa choi het mot van game ma khong bi che mat goc
         * tren, du ngan de viec chua lam khong roi vao quen lang ca buoi toi.
         *
         * Khong tinh tu luc bay ra ma tu luc rut di, nen mot chu ky day du la
         * 30 giay co the + 5 phut im. Mo khoa may thi bo ky nghi nay di, xem
         * [batLaiChuKy].
         */
        const val NGHI_GIUA_HAI_LAN_MS = 5 * 60_000L

        /**
         * Viec thuong thi nam bao lau roi tu rut.
         *
         * 30 giay: du lau de Le Hoa dang mai choi van kip ngang mat len doc, va de
         * doc het ca ba dong chu tren the. Truoc day 12 giay - vua du liec neu dang
         * nhin thang vao mep tren, con dang cam may lam viec khac thi the troi ra roi
         * rut di ma khong ai thay, tuc la loi nhac coi nhu khong hien.
         *
         * Viec gap thi khong dung so nay: no nam li cho den khi het gap.
         */
        const val TU_AN_SAU_MS = 30_000L
    }
}
