package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.Laptop

/**
 * Hop doi phut choi sang Netflix tren laptop, mo tu nut "Netflix" o man chinh (7/10/2026).
 *
 * Hai buoc: chon so phut, roi hoi lai mot lan. Hoi lai vi doi roi khong doi nguoc duoc, va
 * phut Netflix het luc nua dem: con bam nham "Đổi hết" luc 23 gio la mat ca phieu.
 */
object DoiNetflix {

    fun hoi(a: Activity, xong: () -> Unit) {
        val co = GateStore(a).phutDoiDuoc()
        val tiLe = Prefs.get(a).tiLeNetflix
        val conLai = (Laptop.conLaiMs(a) / 60_000L).toInt()
        val dauDe = buildString {
            append("1 phút chơi đổi được $tiLe phút Netflix.")
            if (conLai > 0) append("\nLaptop đang còn $conLai phút.")
        }
        if (co <= 0) {
            MaterialAlertDialogBuilder(a)
                .setTitle("Netflix trên laptop")
                .setMessage("$dauDe\n\nCon chưa có phút chơi nào để đổi.")
                .setPositiveButton("Đóng", null)
                .show()
            return
        }

        // Moi nut mot dong ghi ro ca hai so, de con khong phai tu nhan.
        val cacPhut = Laptop.CAC_NUT.filter { it < co }
        val dong = cacPhut.map { "$it phút chơi → ${it * tiLe} phút Netflix" } +
            "Đổi hết $co phút chơi → ${co * tiLe} phút Netflix"
        MaterialAlertDialogBuilder(a)
            .setTitle("Con có $co phút chơi")
            .setItems(dong.toTypedArray()) { _, i ->
                val het = i == cacPhut.size
                hoiLai(a, if (het) co else cacPhut[i], het, tiLe, conLai, xong)
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    private fun hoiLai(
        a: Activity, phut: Int, het: Boolean, tiLe: Int, conLai: Int, xong: () -> Unit
    ) {
        MaterialAlertDialogBuilder(a)
            .setTitle("Đổi $phut phút chơi?")
            .setMessage(
                "Con được ${phut * tiLe} phút Netflix trên laptop, chỉ dùng trong hôm nay: " +
                    "nửa đêm là hết. Đổi rồi thì không lấy lại phút chơi được." +
                    (if (conLai > 0) "\n\nLaptop đang còn $conLai phút." else "")
            )
            .setPositiveButton("Đổi") { _, _ ->
                val kq = Laptop.doi(a, phut, het)
                val chu = kq.loi
                    ?: "Đã đổi ${kq.phutChoi} phút chơi lấy ${kq.phutNetflix} phút Netflix. " +
                        "Khoảng một phút nữa laptop nhận được."
                Toast.makeText(a, chu, Toast.LENGTH_LONG).show()
                xong()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }
}
