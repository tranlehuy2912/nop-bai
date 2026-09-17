package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.content.Intent
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.guard.Permissions
import vn.huytl.homeworkgate.guard.PhienQuanLy
import vn.huytl.homeworkgate.guard.Thieu
import vn.huytl.homeworkgate.guard.Viec

/**
 * Bang "con thieu gi", dung chung cho ca ba man hinh.
 *
 * Truoc day moi man tu viet mot kieu: man cua Le Hoa gop het thanh mot cuc chu,
 * man Cai dat in mot dong "Con thieu: ...", con trang cau hinh khong bao gi. Ba
 * thu deu chi doc duoc, khong bam vao dau duoc, va khong cai nao nhac den tiet
 * kiem pin hay tu khoi dong - dung hai thu hay lam tablet ho nhat. Gio mot cho
 * lam, bam vao tung dong la di thang den man hinh can bat.
 */
object CanhBao {

    /**
     * Ve ca tam the canh bao: an di khi khong thieu gi, doi mau va doi tieu de
     * theo muc do, roi liet ke tung viec.
     *
     * Ba man hinh dung chung ham nay de bang canh bao o dau cung y het nhau. Doc
     * mot lan o man cua Le Hoa, sang trang cau hinh thay dung cau do, la khoi phai
     * doc lai.
     *
     * [veLai] duoc goi sau khi nguoi dung sua xong ngay trong hop thoai, de man
     * hinh goi no ve lai chinh no.
     */
    fun veThe(
        activity: Activity,
        the: MaterialCardView,
        tieuDe: TextView,
        khung: LinearLayout,
        veLai: () -> Unit,
    ): List<Thieu> {
        val thieu = Permissions.missing(activity)
        khung.removeAllViews()
        thieu.forEach { khung.addView(dong(activity, it, veLai)) }

        if (thieu.isEmpty()) {
            the.visibility = View.GONE
            return thieu
        }
        the.visibility = View.VISIBLE

        // Chi doi mau do khi co viec nang. Viec nhe - thieu quan tri thiet bi, thieu
        // thong bao - van dang chan duoc, de mau do cho ca hai thi nhin mai thanh
        // quen, den luc ho that cung troi qua.
        val nang = thieu.count { it.nang }
        tieuDe.text = "Thiếu ${thieu.size} việc"
        the.setCardBackgroundColor(
            ContextCompat.getColor(
                activity,
                if (nang > 0) R.color.alert_soft else R.color.wait_soft
            )
        )
        return thieu
    }

    /** Mot dong: dau hieu nang nhe, ten viec, va mot cau noi hong cai gi. */
    private fun dong(activity: Activity, thieu: Thieu, veLai: () -> Unit): LinearLayout {
        val hang = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(activity, 10), 0, dp(activity, 10))
            isClickable = true
            isFocusable = true
            setBackgroundResource(chonNen(activity))
            setOnClickListener { bam(activity, thieu, veLai) }
        }

        hang.addView(TextView(activity).apply {
            text = if (thieu.nang) "⛔" else "⚠️"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            layoutParams = LinearLayout.LayoutParams(
                dp(activity, 30), ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
        })

        val cot = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        cot.addView(TextView(activity).apply {
            text = thieu.ten
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(ContextCompat.getColor(activity, R.color.ink))
        })
        cot.addView(TextView(activity).apply {
            text = thieu.cach
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ContextCompat.getColor(activity, R.color.ink_soft))
        })
        hang.addView(cot)

        hang.addView(TextView(activity).apply {
            text = "›"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(ContextCompat.getColor(activity, R.color.ink_soft))
            setPadding(dp(activity, 8), 0, 0, 0)
        })
        return hang
    }

    private fun bam(activity: Activity, thieu: Thieu, veLai: () -> Unit) {
        when (thieu.viec) {
            Viec.TELEGRAM ->
                activity.startActivity(Intent(activity, SetupActivity::class.java))

            // Cong tac nay khong doc duoc tu trong app, nen phai hoi. Mo man hinh
            // cua MIUI ra roi van phai quay lai bam "Đã bật rồi", khong thi dong
            // canh bao nam mai o do.
            Viec.TU_KHOI_DONG -> MaterialAlertDialogBuilder(activity)
                .setTitle("Tự khởi động")
                .setMessage(
                    "Máy Xiaomi có công tắc Tự khởi động riêng cho từng app. Tắt thì " +
                        "khởi động lại tablet là app Nộp bài không chạy nữa, và máy " +
                        "hết chặn cho tới khi có người mở app.\n\n" +
                        "Bấm Mở để vào màn hình đó, bật Nộp bài lên, rồi quay lại bấm " +
                        "Đã bật rồi."
                )
                .setPositiveButton("Mở") { _, _ -> moCaiDat(activity, thieu) }
                .setNeutralButton("Đã bật rồi") { _, _ ->
                    Prefs.get(activity).daXacNhanTuKhoiDong = true
                    veLai()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

            else -> moCaiDat(activity, thieu)
        }
    }

    /**
     * Thu lan luot cac man hinh he thong cho viec nay.
     *
     * Khong man nao chac chan co: man tu khoi dong chi may Xiaomi moi co, va tu
     * Android 11 app khong nhin thay app khac nen khong hoi truoc duoc, phai mo
     * that roi xem co van khong.
     */
    private fun moCaiDat(activity: Activity, thieu: Thieu) {
        // Mo duong vao Cai dat truoc khi di, khong thi guard day ve ngay. Chi lam
        // khi da qua PIN: bang nay cung hien o man cua Le Hoa.
        if (PhienQuanLy.daQuaPin) {
            PhienQuanLy.choMoCaiDat(10)
        } else {
            Toast.makeText(
                activity,
                "Máy đang khoá Cài đặt. Nhờ ba Huy bấm ổ khoá góc trên phải, " +
                    "nhập PIN rồi sửa trong đó.",
                Toast.LENGTH_LONG
            ).show()
        }

        val duong = Permissions.duongVao(activity, thieu.viec)
        for (intent in duong) {
            if (runCatching { activity.startActivity(intent) }.isSuccess) return
        }
        Toast.makeText(
            activity,
            "Máy này không mở thẳng màn hình đó được, phải vào Cài đặt tìm.",
            Toast.LENGTH_LONG
        ).show()
    }

    /** Nen co hieu ung bam, lay tu theme de dung mau cua tung man hinh. */
    private fun chonNen(activity: Activity): Int {
        val gia = TypedValue()
        activity.theme.resolveAttribute(
            android.R.attr.selectableItemBackground, gia, true
        )
        return gia.resourceId
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
