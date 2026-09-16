package vn.huytl.homeworkgate.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import vn.huytl.homeworkgate.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.databinding.ActivityAppPickerBinding
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.PhienQuanLy

/**
 * Chon app cho mot trong hai danh sach.
 *
 * Danh sach trang tra loi "cai gi van dung duoc khi het gio", danh sach den tra loi
 * "cai gi khong bao gio duoc dung". Hai cau hoi khac nhau nhung cung mot thao tac
 * chon, nen dung chung mot man hinh, phan biet bang [EXTRA_DANH_SACH].
 *
 * Chi liet ke app co the mo tu man hinh chinh, vi chan mot service nen thi vo
 * nghia, con lam danh sach dai them vai tram dong.
 */
class AppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppPickerBinding
    private lateinit var prefs: Prefs
    private val selected = mutableSetOf<String>()
    private var entries: List<Entry> = emptyList()
    private var danhSachDen = false
    private var datHanGio = false

    private data class Entry(val packageName: String, val label: String, val info: ApplicationInfo)

    companion object {
        const val EXTRA_DANH_SACH = "danh_sach"
        const val DEN = "den"

        /** Man dat han gio rieng cho tung app, khong phai chon vao danh sach nao. */
        const val HAN = "han"

        /** May muc chon san khi dat han, don vi phut. */
        private val MUC_PHUT = listOf(15, 30, 45, 60, 90, 120, 180)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs.get(this)

        if (prefs.hasPin() && !PhienQuanLy.daQuaPin && !ParentMode.isActive(this)) {
            finish()
            return
        }

        datHanGio = intent.getStringExtra(EXTRA_DANH_SACH) == HAN
        danhSachDen = intent.getStringExtra(EXTRA_DANH_SACH) == DEN
        binding.txtHuongDan.text = if (datHanGio) {
            "Đặt số phút mỗi ngày cho từng app. Hết số phút đó là app tự khoá, " +
                "dù Lê Hòa đang có giờ chơi hay app nằm trong danh sách được dùng. " +
                "Sáng hôm sau tính lại từ đầu."
        } else if (danhSachDen) {
            "Chọn app cấm hẳn. Những app này Lê Hòa không mở được kể cả khi đang " +
                "trong giờ chơi. Không dùng được để cấm màn hình chính hay bàn phím."
        } else {
            "Chọn app Lê Hòa vẫn được dùng khi hết giờ chơi, ví dụ từ điển, máy tính, " +
                "app học. Những app còn lại đều bị khoá khi hết giờ."
        }

        if (!datHanGio) {
            selected.addAll(if (danhSachDen) prefs.blockedPackages else prefs.allowedPackages)
        }
        entries = loadLaunchableApps()

        val adapter = AppAdapter()
        binding.listApps.adapter = adapter
        binding.listApps.setOnItemClickListener { _, _, position, _ ->
            val entry = entries[position]
            if (datHanGio) {
                hoiSoPhut(entry) { adapter.notifyDataSetChanged() }
            } else {
                if (!selected.add(entry.packageName)) selected.remove(entry.packageName)
                adapter.notifyDataSetChanged()
            }
        }

        // Man dat han luu ngay luc chon, nen nut duoi cung chi la dong lai.
        if (datHanGio) binding.btnDone.text = "Xong"

        binding.btnDone.setOnClickListener {
            if (datHanGio) {
                finish()
                return@setOnClickListener
            }
            if (danhSachDen) {
                prefs.blockedPackages = selected.toSet()
            } else {
                prefs.allowedPackages = selected.toSet()
            }
            finish()
        }
    }

    /**
     * Hoi so phut moi ngay cho mot app.
     *
     * Luu ngay khi chon chu khong doi bam Xong: man nay khong phai chon mot lan
     * roi luu ca cum nhu hai danh sach kia.
     */
    private fun hoiSoPhut(entry: Entry, xong: () -> Unit) {
        val dangCo = GioiHanApp.han(this, entry.packageName)
        val nhan = (listOf("Không giới hạn") + MUC_PHUT.map { "$it phút mỗi ngày" })
            .toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(entry.label)
            .setItems(nhan) { _, which ->
                val phut = if (which == 0) 0 else MUC_PHUT[which - 1]
                GioiHanApp.datHan(this, entry.packageName, phut)
                xong()
            }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                // Da xem het han hom nay ma Ba Huy muon cho xem them thi xoa so dem,
                // khong phai doi den sang mai.
                if (dangCo > 0 && GioiHanApp.daDungMs(this@AppPickerActivity, entry.packageName) > 0) {
                    setNeutralButton("Xoá giờ đã xem hôm nay") { _, _ ->
                        GioiHanApp.xoaDaDung(this@AppPickerActivity, entry.packageName)
                        xong()
                    }
                }
            }
            .show()
    }

    private fun loadLaunchableApps(): List<Entry> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolve ->
                val pkg = resolve.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == packageName) return@mapNotNull null
                val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                    ?: return@mapNotNull null
                Entry(pkg, pm.getApplicationLabel(info).toString(), info)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount() = entries.size
        override fun getItem(position: Int) = entries[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@AppPickerActivity)
                .inflate(R.layout.item_app, parent, false)
            val entry = entries[position]
            view.findViewById<TextView>(R.id.app_label).text = entry.label
            view.findViewById<ImageView>(R.id.app_icon)
                .setImageDrawable(packageManager.getApplicationIcon(entry.info))
            val phu = view.findViewById<TextView>(R.id.app_phu)
            val o = view.findViewById<MaterialCheckBox>(R.id.app_check)
            if (datHanGio) {
                o.visibility = View.GONE
                val han = GioiHanApp.han(this@AppPickerActivity, entry.packageName)
                phu.visibility = if (han > 0) View.VISIBLE else View.GONE
                if (han > 0) {
                    val daXem = GioiHanApp.daDungMs(this@AppPickerActivity, entry.packageName) / 60_000
                    phu.text = "$han phút mỗi ngày · hôm nay đã xem $daXem phút"
                }
            } else {
                o.visibility = View.VISIBLE
                phu.visibility = View.GONE
                o.isChecked = entry.packageName in selected
            }
            return view
        }
    }
}
