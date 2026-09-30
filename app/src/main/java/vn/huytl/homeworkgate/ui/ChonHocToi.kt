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

    /**
     * Hoi moc cua mot phan hoc, liet ke du cac bai cua phan lay tu SGK - xem
     * [vn.huytl.homeworkgate.kho.PhanHoc]. Chon xong thi ghi lai roi goi [xong].
     *
     * Dung chung cho man Kiem tra bai (bo the cung phan) va duong sach bai tap: lam
     * them, Giai de. Mot moc cho ca hai, nen con chon mot lan la ca hai cung biet.
     *
     * Bam "Để sau" thi [xong] khong duoc goi, phan do van chua chon.
     */
    fun hoiPhan(
        activity: Activity,
        phan: vn.huytl.homeworkgate.kho.PhanHoc.Phan,
        xong: () -> Unit
    ) {
        val cacBai = vn.huytl.homeworkgate.kho.PhanHoc.cacBai(activity, phan)
        if (cacBai.isEmpty()) return
        val dangChon = when (val bai = vn.huytl.homeworkgate.kho.HocToi.baiCua(activity, phan.ma)) {
            null -> -1
            vn.huytl.homeworkgate.kho.HocToi.CHUA_HOC_BAI_NAO -> 0
            else -> cacBai.indexOf(bai).let { if (it < 0) -1 else it + 1 }
        }
        hoi(
            activity,
            tieuDe = "${phan.ten}: lớp đã học tới bài nào?",
            goiY = "Tính cả bài đang học. Máy chỉ ra bài từ bài đầu tới hết bài " +
                "${activity.getString(R.string.child_name)} chọn.",
            cacMuc = listOf("Chưa học tới bài nào") + cacBai,
            dangChon = dangChon
        ) { i ->
            vn.huytl.homeworkgate.kho.HocToi.datBai(
                activity, phan,
                if (i == 0) vn.huytl.homeworkgate.kho.HocToi.CHUA_HOC_BAI_NAO else cacBai[i - 1]
            )
            xong()
        }
    }

    /**
     * Hoi lop da hoc toi Unit nao trong bo tu [bo], ghi lai roi goi [xong]. Cac Unit lay tu
     * chinh bo tu trong kho, nhu man Do tu vung. Dung o trang Luyen tap (30/9/2026).
     */
    fun hoiUnit(
        activity: Activity,
        bo: vn.huytl.homeworkgate.kho.BoTuVung.Bo,
        xong: () -> Unit
    ) {
        val cacUnit = vn.huytl.homeworkgate.kho.KhoBai.get(activity).cacTuCua(bo.bo)
            .map { it.unit }.filter { it > 0 }.distinct().sorted()
        if (cacUnit.isEmpty()) return
        val dangChon = when (val u = vn.huytl.homeworkgate.kho.HocToi.unitCua(activity, bo.bo)) {
            null -> -1
            vn.huytl.homeworkgate.kho.HocToi.CHUA_HOC_UNIT_NAO -> 0
            else -> cacUnit.indexOf(u).let { if (it < 0) -1 else it + 1 }
        }
        hoi(
            activity,
            tieuDe = "${bo.ten}: lớp đã học tới Unit nào?",
            goiY = "Tính cả Unit đang học. Máy chỉ hỏi từ của Unit 1 tới hết Unit " +
                "${activity.getString(R.string.child_name)} chọn.",
            cacMuc = listOf("Chưa học Unit nào") + cacUnit.map { "Unit $it" },
            dangChon = dangChon
        ) { i ->
            vn.huytl.homeworkgate.kho.HocToi.datUnit(
                activity, bo,
                if (i == 0) vn.huytl.homeworkgate.kho.HocToi.CHUA_HOC_UNIT_NAO else cacUnit[i - 1]
            )
            xong()
        }
    }

    /**
     * Hoi lan luot moi phan con chua chon cua [mon], roi goi [xong] khi da chon het.
     * Con bam "Để sau" o phan nao thi dung o do.
     */
    fun hoiPhanConThieu(activity: Activity, mon: String, xong: () -> Unit) {
        val thieu = vn.huytl.homeworkgate.kho.PhanHoc.chuaChon(activity, mon)
        if (thieu.isEmpty()) return xong()
        hoiPhan(activity, thieu.first()) { hoiPhanConThieu(activity, mon, xong) }
    }

    /**
     * Doi moc cua mot trong [cacPhan]: mot phan thi hoi luon, nhieu phan (bo the Toan: Dai so
     * va Hinh hoc, 30/9/2026) thi hoi doi phan nao truoc, nhu man chon bai.
     */
    fun hoiDoiPhan(
        activity: Activity,
        cacPhan: List<vn.huytl.homeworkgate.kho.PhanHoc.Phan>,
        xong: () -> Unit
    ) {
        if (cacPhan.isEmpty()) return
        if (cacPhan.size == 1) return hoiPhan(activity, cacPhan.first(), xong)
        MaterialAlertDialogBuilder(activity)
            .setTitle("Đổi phần nào?")
            .setItems(
                cacPhan.map { vn.huytl.homeworkgate.kho.PhanHoc.moTaPhan(activity, it) }.toTypedArray()
            ) { _, i -> hoiPhan(activity, cacPhan[i], xong) }
            .setNegativeButton("Để sau", null)
            .show()
    }

    /** Hoi lan luot cac phan trong [cacPhan] con chua chon, roi goi [xong] khi da chon het. */
    fun hoiCacPhanThieu(
        activity: Activity,
        cacPhan: List<vn.huytl.homeworkgate.kho.PhanHoc.Phan>,
        xong: () -> Unit
    ) {
        val thieu = cacPhan.firstOrNull { vn.huytl.homeworkgate.kho.PhanHoc.hocToi(activity, it) == null }
            ?: return xong()
        hoiPhan(activity, thieu) { hoiCacPhanThieu(activity, cacPhan, xong) }
    }
}
