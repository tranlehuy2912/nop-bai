package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.content.Intent
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.PhienQuanLy
import vn.huytl.homeworkgate.telegram.Notifier

/**
 * Hoi lai PIN ngay tren trang cua Ba Huy, khi trang do mo lai ma chua chac Ba Huy
 * con cam may.
 *
 * Truoc day gap luc do la dong trang, day ve man cua Le Hoa, bat go PIN tu dau. Con
 * khong vao duoc that, nhung Ba Huy sang Telegram chep token hay sang trinh duyet
 * chep khoa AI roi quay lai la mat trang, mat luon chu dang go do chua bam Luu. Gio
 * trang van nam nguyen do, chi bi che sau hop PIN: go dung la lam tiep, bam Huy moi
 * ve man cua con.
 *
 * Hai luc phai hoi:
 *  - app vua vang mat lau, xem [PhienQuanLy.phaiHoiLaiPin];
 *  - trong lan mo app nay chua ai go PIN. Android tat ngam app roi dung lai trang nay
 *    tu danh sach app gan day la roi vao luc do, vi co da-qua-PIN chi nam trong bo
 *    nho va chet theo tien trinh.
 * May chua dat PIN lan nao thi khong hoi, vi luc do chua co gi de bao ve.
 *
 * Moi man nam sau PIN goi [roiMoi] o dau onResume.
 */
class HoiLaiPin(private val man: AppCompatActivity) {

    private var hop: AlertDialog? = null

    init {
        // Xoay may luc hop dang mo thi man cu bi huy, hop phai di theo. Man moi tu hoi
        // lai o onResume, vi co da-qua-PIN da ve false tu luc khoa.
        man.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                hop?.dismiss()
                hop = null
            }
        })
    }

    /**
     * Chay [viec] ngay neu trang dung duoc, hoac sau khi Ba Huy go dung PIN.
     *
     * Dang hoi thi an ca noi dung trang: con cam may len chi thay hop PIN tren nen
     * trong, khong doc duoc token, khoa AI hay gio nghi nam phia sau.
     */
    fun roiMoi(viec: () -> Unit = {}) {
        // Ra ngoai roi quay lai luc hop con mo: van la hop cu, khong mo them hop nua.
        if (hop?.isShowing == true) return
        if (!phaiHoi()) {
            viec()
            return
        }
        PhienQuanLy.daQuaPin = false
        PhienQuanLy.thoiMoCaiDat()
        noiDung(hien = false)
        hoi(viec)
    }

    private fun phaiHoi(): Boolean {
        if (!Prefs.get(man).hasPin()) return false
        if (PhienQuanLy.phaiHoiLaiPin()) return true
        return !PhienQuanLy.daQuaPin && !ParentMode.isActive(man)
    }

    private fun hoi(viec: () -> Unit) {
        val o = EditText(man).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Mã PIN"
            setPadding(48, 40, 48, 40)
        }
        hop = MaterialAlertDialogBuilder(man)
            .setTitle(R.string.pin_title)
            .setMessage("Máy vừa để đó một lúc. Gõ lại mã PIN để làm tiếp.")
            .setView(o)
            .setPositiveButton(R.string.ok) { _, _ -> kiem(o.text.toString(), viec) }
            .setNegativeButton(R.string.cancel) { _, _ -> veManCon(man) }
            // Nut Back cua he thong cung tinh la Huy.
            .setOnCancelListener { veManCon(man) }
            .create()
            .apply {
                // Cham nham ra ngoai hop ma bi day ve thi lai mat trang, dung cai loi
                // vua sua.
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    private fun kiem(pin: String, viec: () -> Unit) {
        hop = null
        if (man.isFinishing || man.isDestroyed) return
        val prefs = Prefs.get(man)
        if (!prefs.checkPin(pin)) {
            // Dem chung voi o PIN o man chinh: go sai o day hay o do cung la mot nguoi
            // dang do ma.
            val soLan = prefs.saiPinLienTiep + 1
            prefs.saiPinLienTiep = soLan
            if (soLan % 3 == 0) Notifier.wrongPinAttempts(man, soLan)
            Toast.makeText(man, R.string.pin_wrong, Toast.LENGTH_SHORT).show()
            hoi(viec)
            return
        }
        prefs.saiPinLienTiep = 0
        PhienQuanLy.daQuaPin = true
        Notifier.parentModeEntered(man)
        noiDung(hien = true)
        viec()
    }

    private fun noiDung(hien: Boolean) {
        man.findViewById<View>(android.R.id.content).visibility =
            if (hien) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        /** Dong trang cau hinh, tra man hinh ve cho Le Hoa, va bat hoi lai PIN. */
        fun veManCon(man: Activity) {
            PhienQuanLy.daQuaPin = false
            PhienQuanLy.thoiMoCaiDat()
            // HomeActivity la singleTask nen mo no la don sach moi man nam tren no.
            man.startActivity(Intent(man, HomeActivity::class.java))
            man.finish()
        }
    }
}
