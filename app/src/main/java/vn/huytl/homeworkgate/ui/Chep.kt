package vn.huytl.homeworkgate.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import vn.huytl.homeworkgate.R

/**
 * Nut chep o tung cau: de bai o man chon cau va man giai de, them dong Le Hoa viet va
 * goi y sua o man bai da cham. Nut la mot icon, xem st_nut_chep.
 *
 * Ngay 27/9/2026 Ba Huy chon cho Le Hoa chep duoc cac chu nay, ca luc bai chua cham, du
 * da biet chep ra thi dan duoc vao app AI, trinh duyet hay Telegram.
 *
 * CHI CO NUT RIENG, KHONG BAT CHON CHU. Thanh chon chu cua Android co ca nut Chia se,
 * Dich va muc cua app khac cai vao, bam la day chu thang sang app do. Nut nay chi dua
 * chu vao bo nho tam.
 */
object Chep {

    fun vao(context: Context, chu: String) {
        val bang = context.getSystemService(ClipboardManager::class.java) ?: return
        bang.setPrimaryClip(ClipData.newPlainText("Câu", chu))
        Toast.makeText(context, "Đã chép.", Toast.LENGTH_SHORT).show()
    }

    /** Nut cho dong ve bang code. Cung mot layout voi nut dat san trong layout khac. */
    fun nut(cha: ViewGroup, chu: () -> String): MaterialButton =
        (LayoutInflater.from(cha.context).inflate(R.layout.st_nut_chep, cha, false) as MaterialButton)
            .apply { setOnClickListener { vao(context, chu()) } }
}
