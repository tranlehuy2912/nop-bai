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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.checkbox.MaterialCheckBox
import vn.huytl.homeworkgate.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.databinding.ActivityAppPickerBinding

/**
 * Chon app cho mot trong cac danh sach.
 *
 * Danh sach trang tra loi "cai gi van dung duoc khi het gio", danh sach dung moi luc
 * tra loi "cai gi khong bao gio khoa theo gio", danh sach den tra loi "cai gi khong
 * bao gio duoc dung". Cau hoi khac nhau nhung cung mot thao tac chon, nen dung chung
 * mot man hinh, phan biet bang [EXTRA_DANH_SACH].
 *
 * Chi liet ke app co the mo tu man hinh chinh, vi chan mot service nen thi vo
 * nghia, con lam danh sach dai them vai tram dong.
 */
class AppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppPickerBinding
    private lateinit var prefs: Prefs
    private val hoiLaiPin = HoiLaiPin(this)
    private val selected = mutableSetOf<String>()

    /** Toan bo app mo duoc tu man hinh chinh, doc mot lan luc vao man. */
    private var tatCa: List<Entry> = emptyList()

    /** Phan dang hien: [tatCa] sau khi loc theo [tim] va keo cai da chon len dau. */
    private var entries: List<Entry> = emptyList()

    private var tim = ""
    private var danhSachDen = false
    private var datHanGio = false
    private var chonNhac = false
    private var moiLuc = false

    private data class Entry(val packageName: String, val label: String, val info: ApplicationInfo)

    companion object {
        const val EXTRA_DANH_SACH = "danh_sach"
        const val DEN = "den"

        /** Man dat han gio rieng cho tung app, khong phai chon vao danh sach nao. */
        const val HAN = "han"

        /** Man chon app duoc phat tieng khi het gio choi. */
        const val NHAC = "nhac"

        /** Man chon app dung moi luc, ke ca gio ngu va gio di hoc. */
        const val MOI_LUC = "moiluc"

        /** May muc chon san khi dat han, don vi phut. */
        private val MUC_PHUT = listOf(15, 30, 45, 60, 90, 120, 180)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs.get(this)

        chuaThanhHeThong()

        datHanGio = intent.getStringExtra(EXTRA_DANH_SACH) == HAN
        danhSachDen = intent.getStringExtra(EXTRA_DANH_SACH) == DEN
        chonNhac = intent.getStringExtra(EXTRA_DANH_SACH) == NHAC
        moiLuc = intent.getStringExtra(EXTRA_DANH_SACH) == MOI_LUC
        binding.txtTieuDe.text = when {
            datHanGio -> "Giờ riêng từng app"
            danhSachDen -> "App cấm hẳn"
            chonNhac -> "App được nghe nền"
            moiLuc -> "Dùng mọi lúc"
            else -> "Dùng khi hết giờ chơi"
        }
        binding.txtHuongDan.text = when {
            datHanGio ->
                "Đặt số phút mỗi ngày cho từng app. Hết số phút đó là app tự khoá, " +
                    "dù Lê Hòa đang có giờ chơi hay app nằm trong danh sách được dùng. " +
                    "Sáng hôm sau tính lại từ đầu.\n" +
                    "Đặt giờ ở đây không làm app mở được khi hết giờ chơi. Muốn vậy thì " +
                    "tích thêm app đó ở mục Chọn app dùng khi hết giờ chơi."
            danhSachDen ->
                "Chọn app cấm hẳn. Những app này Lê Hòa không mở được kể cả khi đang " +
                    "trong giờ chơi. Không dùng được để cấm màn hình chính hay bàn phím."
            chonNhac ->
                "Chọn app được phát tiếng khi hết giờ chơi, ví dụ app nghe nhạc. " +
                    "Chỉ là phát tiếng: muốn mở app ra xem thì vẫn phải còn giờ chơi. " +
                    "Nhớ đặt số phút mỗi ngày ở mục Giờ riêng từng app, không thì nghe " +
                    "bao nhiêu cũng được. Quá giờ đi ngủ hoặc tới giờ đi học là tiếng tắt."
            moiLuc ->
                "Chọn app Lê Hòa dùng được mọi lúc, kể cả giờ ngủ, giờ đi học và lúc " +
                    "màn chặn việc nhà đang che, ví dụ Telegram để nhắn cho ba Huy. " +
                    "App cấm hẳn và giờ riêng từng app vẫn áp dụng cho app ở đây."
            else ->
                "Chọn app Lê Hòa vẫn được dùng khi hết giờ chơi, ví dụ từ điển, máy tính, " +
                    "app học. Những app còn lại đều bị khoá khi hết giờ. Từ giờ ngủ tới " +
                    "giờ dậy và trong giờ đi học thì app ở đây cũng khoá."
        }

        if (!datHanGio) {
            selected.addAll(
                when {
                    danhSachDen -> prefs.blockedPackages
                    chonNhac -> prefs.nhacPackages
                    moiLuc -> prefs.moiLucPackages
                    else -> prefs.allowedPackages
                }
            )
        }
        tatCa = loadLaunchableApps()
        locLai()

        val adapter = AppAdapter()
        binding.listApps.adapter = adapter
        binding.listApps.setOnItemClickListener { _, _, position, _ ->
            val entry = entries[position]
            if (datHanGio) {
                hoiSoPhut(entry) { veLai(adapter) }
            } else {
                if (!selected.add(entry.packageName)) selected.remove(entry.packageName)
                veLai(adapter)
            }
        }

        // Go toi dau loc toi do. Danh sach nay khong bao gio dai qua vai chuc dong
        // nen loc thang tren luong chinh, khong can hoan mot nhip nao.
        binding.oTim.doAfterTextChanged {
            tim = it?.toString().orEmpty().trim()
            veLai(adapter)
        }

        demLaiNutXong()

        binding.btnDone.setOnClickListener {
            if (datHanGio) {
                finish()
                return@setOnClickListener
            }
            when {
                danhSachDen -> prefs.blockedPackages = selected.toSet()
                chonNhac -> prefs.nhacPackages = selected.toSet()
                moiLuc -> prefs.moiLucPackages = selected.toSet()
                else -> prefs.allowedPackages = selected.toSet()
            }
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Man nay nam sau PIN nhu Cai dat. Roi app lau, hay chua qua PIN, thi hoi PIN
        // ngay day; cac app vua tich ma chua bam Xong van con.
        hoiLaiPin.roiMoi()
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

    /**
     * Tu Android 15 app ve tran ca man hinh, thuoc tinh statusBarColor trong theme
     * khong con tac dung. Khong chua cho thi cau huong dan tren dau nam duoi thanh
     * trang thai, con nut Xong o day thi dinh thanh dieu huong.
     */
    private fun chuaThanhHeThong() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
    }

    /**
     * Loc theo chu dang go, roi keo nhung app dang chon len dau.
     *
     * Keo len dau vi cau hoi thuong gap nhat khi mo man nay la "minh dang cho nhung
     * cai gi" - ma neu cac app do nam rai rac theo van chu cai thi phai cuon het
     * danh sach moi tra loi duoc.
     */
    private fun locLai() {
        val theoChu = if (tim.isBlank()) tatCa else {
            val k = tim.lowercase()
            tatCa.filter { it.label.lowercase().contains(k) }
        }
        entries = if (datHanGio) {
            // Man dat han khong co o tich; cai da dat gio moi la cai dang chon.
            theoChu.sortedWith(
                compareByDescending<Entry> { GioiHanApp.han(this, it.packageName) > 0 }
                    .thenBy { it.label.lowercase() }
            )
        } else {
            theoChu.sortedWith(
                compareByDescending<Entry> { it.packageName in selected }
                    .thenBy { it.label.lowercase() }
            )
        }
    }

    private fun veLai(adapter: AppAdapter) {
        locLai()
        adapter.notifyDataSetChanged()
        demLaiNutXong()
        binding.txtTrong.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        binding.txtTrong.text = "Không có app nào tên giống “$tim”."
    }

    /**
     * Ghi so dang chon thang len nut Xong.
     *
     * Man dat han thi khong dem: o do moi app mot con so rieng, khong co khai niem
     * "da chon bao nhieu cai".
     */
    private fun demLaiNutXong() {
        binding.btnDone.text = when {
            datHanGio -> "Xong"
            selected.isEmpty() -> "Xong — chưa chọn app nào"
            else -> "Xong — ${selected.size} app"
        }
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
                    // Noi ro app co mo duoc khi het gio choi khong. Dat gio o day chi la
                    // cai tran, khong mo app ra; thieu dong nay thi nhin vao tuong app
                    // da co rieng bay nhieu phut moi ngay, khong can gio choi.
                    val khiHetGio = when (entry.packageName) {
                        in prefs.moiLucPackages -> "mở được mọi lúc"
                        in prefs.allowedPackages ->
                            "mở được khi hết giờ chơi, trừ giờ ngủ, giờ học"
                        else -> "chỉ mở trong giờ chơi"
                    }
                    phu.text = "$han phút mỗi ngày · hôm nay đã xem $daXem phút · $khiHetGio"
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
