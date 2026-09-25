package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.R

/**
 * Hop hoi "lop da hoc toi bai nao", dung chung cho man Kiem tra bai va man Do tu vung.
 * Lua chon ghi o [vn.huytl.homeworkgate.kho.HocToi].
 *
 * DANH SACH CAC BAI CUA CHINH BO DO, khong phai o go so. Go so thi con go "Bai 7" trong
 * khi bo KHTN phan Hoa khong co Bai 7, va may phai tu doan Bai 7 nam dau. Danh sach chi
 * co nhung bai co the, nen dong goi y o dau hop noi ro: khong thay bai dang hoc thi chon
 * bai gan nhat phia tren.
 *
 * DONG DAU LUON LA "CHUA HOC". Bo Toan hien chi co phan hang dang thuc, lop chua toi do
 * thi con phai noi duoc la chua hoc, chu bat chon mot bai la bat hoi dung thu chua hoc.
 *
 * CHAM MOT LAN LA CHON XONG, khong co nut dong y: chon nham thi mo lai doi, khong mat gi.
 * Nut "Để sau" dong hop ma khong ghi gi; bo chua chon lan nao thi van chua bat dau duoc.
 */
object ChonHocToi {

    /**
     * @param cacMuc cac dong chon, dong dau la "chua hoc", roi toi cac bai theo thu tu sach.
     * @param dangChon dong dang chon, -1 khi chua chon lan nao.
     * @param chon goi voi dong con cham, sau khi hop da dong.
     */
    fun hoi(
        activity: Activity,
        tieuDe: String,
        goiY: String,
        cacMuc: List<String>,
        dangChon: Int,
        chon: (Int) -> Unit
    ) {
        val mat = activity.resources.displayMetrics.density
        fun Int.dp(): Int = (this * mat).toInt()

        // Tieu de tu ve: hop co danh sach chon thi khong con cho cho setMessage, ma dong
        // goi y la thu con phai doc truoc khi chon.
        val dau = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 24.dp(), 24.dp(), 8.dp())
            addView(TextView(activity).apply {
                text = tieuDe
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(activity, R.color.ink))
            })
            addView(TextView(activity).apply {
                text = goiY
                textSize = 15f
                setTextColor(ContextCompat.getColor(activity, R.color.ink_soft))
                setPadding(0, 8.dp(), 0, 0)
            })
        }

        MaterialAlertDialogBuilder(activity)
            .setCustomTitle(dau)
            .setSingleChoiceItems(cacMuc.toTypedArray(), dangChon) { hop, i ->
                hop.dismiss()
                chon(i)
            }
            .setNegativeButton("Để sau", null)
            .show()
    }
}
