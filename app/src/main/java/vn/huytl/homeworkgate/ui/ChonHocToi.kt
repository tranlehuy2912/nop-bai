package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.graphics.Typeface
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Hop danh dau "lop da hoc nhung bai nao" cua mot mon, dung chung cho trang Luyen tap, man
 * Kiem tra cong thuc, Kiem tra tu vung, Luyen tap tung phan va man nop bai. Lua chon ghi o
 * [HocToi].
 *
 * CHON NHIEU O, CHIA THEO CHUONG (Ba Huy chot 2/10/2026). Truoc do moi phan hoc mot hop chon
 * mot dong "lop da hoc toi bai nao", Toan hai hop, KHTN ba hop. Nay moi mon mot hop: moi bai
 * cua SGK mot o, xep duoi ten chuong; bam ten chuong la chon hay bo ca chuong (danh dau tung
 * bai cua ca nam la ba chuc lan bam). Tieng Anh moi Unit mot o. Danh sach lay tu SGK chu
 * khong tu bo the, vi kho sach bai tap co du moi bai, con bo the Hoa bo Bai 5 va Bai 7 va
 * phan Sinh khong co bo nao.
 *
 * Bam "Xong" moi ghi, ke ca khi khong o nao duoc danh dau: luc do la con noi chua hoc bai nao,
 * khac voi chua chon. "Để sau" hay cham ra ngoai thi khong ghi gi; mon chua chon lan nao thi
 * van chua vao duoc.
 */
object ChonHocToi {

    /**
     * Hoi cac bai lop da hoc cua [mon]. Bam "Xong" thi ghi lai roi goi [xong]; bam "Để sau"
     * hay dong hop thi goi [huy] (man nao can lui ra thi dua [huy] vao).
     */
    fun hoiMon(activity: Activity, mon: String, huy: () -> Unit = {}, xong: () -> Unit) {
        val cacNhom = PhanHoc.cacNhom(activity, mon)
        if (cacNhom.isEmpty()) return huy()
        val laAnh = mon == PhanHoc.TIENG_ANH
        val dangChon = (PhanHoc.baiDaHoc(activity, mon) ?: emptySet()).toMutableSet()
        val mat = activity.resources.displayMetrics.density
        fun Int.dp(): Int = (this * mat).toInt()
        val mauChu = ContextCompat.getColor(activity, R.color.ink)
        val mauNhat = ContextCompat.getColor(activity, R.color.ink_soft)

        val dau = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 24.dp(), 24.dp(), 8.dp())
            addView(TextView(activity).apply {
                text = "${PhanHoc.tenNgan(mon)}: lớp đã học những ${if (laAnh) "Unit" else "bài"} nào?"
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(mauChu)
            })
            addView(TextView(activity).apply {
                text = "Đánh dấu cả ${if (laAnh) "Unit" else "bài"} đang học. Máy chỉ ra câu trong các " +
                    "${if (laAnh) "Unit" else "bài"} đã đánh dấu." +
                    if (laAnh) "" else " Bấm tên chương để chọn hay bỏ cả chương."
                textSize = 15f
                setTextColor(mauNhat)
                setPadding(0, 8.dp(), 0, 0)
            })
        }

        val danhSach = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 4.dp(), 20.dp(), 8.dp())
        }
        // O cua tung bai, va o cua tung chuong voi danh sach so bai cua chuong do.
        val oBai = HashMap<Int, CheckBox>()
        val oChuong = mutableListOf<Pair<CheckBox, List<Int>>>()
        var dangVe = false

        fun veLaiChuong() {
            dangVe = true
            oChuong.forEach { (o, cac) -> o.isChecked = cac.all { it in dangChon } }
            dangVe = false
        }

        cacNhom.forEach { nhom ->
            val soCua = nhom.cacBai.map { it.first }
            if (nhom.ten.isNotBlank()) {
                val o = CheckBox(activity).apply {
                    text = nhom.ten
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(mauChu)
                    setPadding(4.dp(), 10.dp(), 0, 10.dp())
                }
                o.setOnCheckedChangeListener { _, chon ->
                    if (dangVe) return@setOnCheckedChangeListener
                    if (chon) dangChon += soCua else dangChon -= soCua.toSet()
                    dangVe = true
                    soCua.forEach { oBai[it]?.isChecked = chon }
                    dangVe = false
                }
                oChuong += o to soCua
                danhSach.addView(o)
            }
            nhom.cacBai.forEach { (so, ten) ->
                val o = CheckBox(activity).apply {
                    text = ten
                    textSize = 16f
                    setTextColor(mauChu)
                    setPadding(4.dp(), 6.dp(), 0, 6.dp())
                    isChecked = so in dangChon
                }
                o.setOnCheckedChangeListener { _, chon ->
                    if (dangVe) return@setOnCheckedChangeListener
                    if (chon) dangChon += so else dangChon -= so
                    veLaiChuong()
                }
                if (nhom.ten.isNotBlank()) {
                    o.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = 24.dp() }
                }
                oBai[so] = o
                danhSach.addView(o)
            }
        }
        veLaiChuong()

        var daXong = false
        MaterialAlertDialogBuilder(activity)
            .setCustomTitle(dau)
            .setView(ScrollView(activity).apply { addView(danhSach) })
            .setPositiveButton("Xong") { _, _ ->
                daXong = true
                // Chi giu so bai co trong danh sach: tap cu co the con bai ma SGK khong co.
                HocToi.datDaHoc(activity, mon, dangChon.filter { it in oBai.keys }.toSet())
                xong()
            }
            .setNegativeButton("Để sau", null)
            .setOnDismissListener { if (!daXong) huy() }
            .show()
    }
}
