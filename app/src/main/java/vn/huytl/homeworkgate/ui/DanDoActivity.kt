package vn.huytl.homeworkgate.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.ai.DocDanDo
import vn.huytl.homeworkgate.data.NhacBai
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.telegram.DanDoSender
import vn.huytl.homeworkgate.databinding.StActivityDanDoBinding
import vn.huytl.homeworkgate.databinding.StDongDanDoBinding
import java.io.File
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Chon ngay, chup trang vo dan do, soat lai cai may doc ra, roi luu thanh mot trang nhac bai.
 *
 * VI SAO PHAI SOAT. Ban doc ra la thu may dung de nhac bai theo tung mon, xem
 * [vn.huytl.homeworkgate.data.NhacBai]. Tu 30/9/2026 no khong con dinh toi so phut: Ba
 * Huy bo tron goi 45 phut.
 *
 * Do tren bon trang vo that cua Le Hoa cho thay soat khong phai buoc cho co: may
 * doc "trang 19" thanh "trang 14", "luyen tap 3 trang 59" thanh "quyen tap 3 trang
 * 54", va xep hai bai tap that vao muc viec khac. Khong co buoc nay thi ba cho do
 * deu lang le di vao loi nhac.
 *
 * CHON NGAY TRUOC KHI CHUP (Ba Huy chon 30/9/2026). Mac dinh la hom nay; chup bu vo hom kia
 * thi con doi ngay truoc. Moi ngay mot trang, luu trung ngay thi trang sau thay trang truoc.
 * Vo cua Le Hoa chep lien tay, mot trang giay co hai ba buoi: may tra ve tung khoi, va man
 * nay lay khoi dung ngay con chon. Khong khoi nao dung ngay ma may doc ra ngay khac thi hoi
 * lai con ([chonKhoi]), khong tu doan. Truoc do man nay bay ra mot hang nut ngay de con chon
 * khoi, va ngay trong o lay theo may doc.
 *
 * MAY DOC KHONG DUOC THI CON TU GO (Ba Huy chon 30/9/2026). Man soat mo ra voi tam anh to
 * o tren va mot dong trong, ngay la ngay con chon. Khong co nut doc lai hay gui anh cho ba
 * doc: truoc do con gui anh, Ba Huy nho Claude doc tren Bang dieu khien, va trong luc cho thi
 * trang do chua nhac duoc gi.
 *
 * MAN DAU LA DANH SACH TRANG DANG NHAC. Bam vao mot trang thi mo ra sua; doi ngay cua trang
 * do thi trang chuyen sang ngay moi chu khong de lai trang o ngay cu.
 *
 * KHONG CHAN CON SUA GI CA. O ngay, o chu va o tich deu sua tay duoc. Chan lai thi
 * phai chan bang mot cai luat nao do, ma moi luat o day deu se sai vao dung hom co
 * giao ghi mot kieu la. Doi lai, tam anh goc van nam trong Telegram cua Ba Huy.
 */
class DanDoActivity : AppCompatActivity() {

    private lateinit var b: StActivityDanDoBinding

    /** Ngay con chon truoc khi chup. */
    private var ngayChon: LocalDate = LocalDate.now()

    /** Ngay dang hien o man soat, con sua tay duoc. */
    private var ngay: LocalDate = LocalDate.now()

    /**
     * Ngay cua trang da luu dang mo ra sua, null la dang chup trang moi. Luu voi ngay khac
     * thi trang o ngay nay bi bo, xem [NhacBai.ghi].
     */
    private var ngayGoc: LocalDate? = null

    /**
     * May doc khong duoc tam dang soat, con nhin anh tu go ([VoDanDo.NGUON_TU_GO]). Mo trang
     * da luu ra sua thi het, vi luc do la sua so voi lan luu truoc.
     */
    private var tuGo = false

    /** Cac dong dang bay tren man, de luc Luu doc nguoc ra. */
    private val dong = mutableListOf<StDongDanDoBinding>()

    /** Tam anh vua chup, chua luu. Xoa luc dong man neu con khong bam Luu. */
    private var anhTam: File? = null

    /** Camera dang mo, cho ket qua. Giu qua luc Android dung lai man. */
    private var dangChup = false

    private val chupTraVe = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { ket ->
        dangChup = false
        val duong = ket.data?.getStringArrayListExtra(CaptureActivity.KET_QUA_ANH).orEmpty()
        if (ket.resultCode != RESULT_OK || duong.isEmpty()) {
            // Huy tu man soat thi de nguyen man do. Chu may vua doc, cho con sua tay va
            // tam anh cua no van nam nguyen sau camera; ve lai hay dong man la con mat
            // ban vua doc, phai chup lai va may doc them mot luot. Ba Huy chon giu nguyen
            // ngay 26/9/2026. Huy tu man chon ngay thi ve lai man chon ngay.
            if (b.boxSoat.visibility == View.VISIBLE) return@registerForActivityResult
            veChon()
            return@registerForActivityResult
        }
        // Chup nhieu tam thi lay tam CUOI: thuong la tam con chup lai vi tam truoc mo.
        val cac = duong.map { File(it) }
        val chon = cac.last()
        cac.dropLast(1).forEach { it.delete() }
        doiAnh(chon)
        docBangMay(chon)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dangChup = savedInstanceState?.getBoolean(K_DANG_CHUP) == true
        savedInstanceState?.getString(K_NGAY_CHON)?.let { ngayChon = LocalDate.parse(it) }
        ngayGoc = savedInstanceState?.getString(K_NGAY_GOC)?.let { LocalDate.parse(it) }
        tuGo = savedInstanceState?.getBoolean(K_TU_GO) == true
        b = StActivityDanDoBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }

        b.nutQuayLai.setOnClickListener { quayLai() }
        onBackPressedDispatcher.addCallback(this) { quayLai() }
        b.nutNgayChup.setOnClickListener { chonNgayChup() }
        b.nutChup.setOnClickListener {
            ngayGoc = null
            moManChup()
        }
        b.nutChupLai.setOnClickListener { moManChup() }
        b.nutLuu.setOnClickListener { luu() }
        b.nutNgay.setOnClickListener { chonNgay() }
        b.nutThemDong.setOnClickListener { themDong(VoDanDo.Dong("", false)) }

        // Don ban cua app cu va trang da het dong de nhac, truoc khi bay danh sach.
        VoDanDo.donDep(this)
        val sua = intent.getStringExtra(EXTRA_SUA_NGAY)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        when {
            savedInstanceState == null && sua != null -> moTrang(sua)
            // Man bi dung lai trong luc camera dang mo thi ket qua van se ve: de man chon
            // ngay nam sau camera ma giu nguyen ngay goc, ket qua ve thi di tiep nhu thuong.
            dangChup -> veChon(datLai = false)
            savedInstanceState?.getBoolean(K_SOAT) == true -> veLaiSoat(savedInstanceState)
            else -> veChon()
        }
    }

    /**
     * Ve lai man soat sau khi Android dung lai man (thu hoi bo nho luc app nam nen, doi co
     * chu). Cac dong di qua Bundle vi moi dong mang chung id, Android khong tu luu duoc, xem
     * [themDong]. Tam anh vua chup nam trong cacheDir, van con.
     */
    private fun veLaiSoat(st: Bundle) {
        anhTam = st.getString(K_ANH_TAM)?.let { File(it) }?.takeIf { it.exists() }
        hienAnh(anhTam)
        val chu = st.getStringArrayList(K_CHU).orEmpty()
        val tich = st.getBooleanArray(K_TICH) ?: BooleanArray(0)
        val may = st.getIntArray(K_MAY) ?: IntArray(0)
        val cac = chu.indices.map { i ->
            VoDanDo.Dong(
                chu[i], tich.getOrElse(i) { false },
                when (may.getOrElse(i) { -1 }) { 1 -> true; 0 -> false; else -> null }
            )
        }
        veSoat(st.getString(K_NGAY)?.let { LocalDate.parse(it) } ?: ngayChon, cac)
    }

    override fun onDestroy() {
        // Chi don ban tam. Man bi dung lai thi giu tam vua chup, man moi doc lai no tu
        // Bundle, xem [veLaiSoat].
        //
        // Xoay may thi khong toi day: man nay tu ve lai khi xoay, xem configChanges
        // cua no trong AndroidManifest.
        if (!isChangingConfigurations) anhTam?.delete()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(K_DANG_CHUP, dangChup)
        outState.putString(K_NGAY_CHON, ngayChon.toString())
        ngayGoc?.let { outState.putString(K_NGAY_GOC, it.toString()) }
        outState.putBoolean(K_TU_GO, tuGo)
        if (b.boxSoat.visibility == View.VISIBLE) {
            // Giu ca dong chua co chu: con vua bam them dong ma chua go.
            outState.putBoolean(K_SOAT, true)
            outState.putString(K_NGAY, ngay.toString())
            anhTam?.let { outState.putString(K_ANH_TAM, it.absolutePath) }
            outState.putStringArrayList(K_CHU, ArrayList(dong.map { it.oChu.text.toString() }))
            outState.putBooleanArray(K_TICH, dong.map { it.oTich.isChecked }.toBooleanArray())
            outState.putIntArray(
                K_MAY,
                dong.map { when (it.root.tag as? Boolean) { true -> 1; false -> 0; null -> -1 } }.toIntArray()
            )
        }
    }

    /** Dang o man chon ngay thi dong man, o man khac thi ve man chon ngay. */
    private fun quayLai() {
        if (b.boxChon.visibility == View.VISIBLE) {
            finish()
            return
        }
        anhTam?.delete()
        anhTam = null
        veChon()
    }

    // ----------------------------------------------------------------- man chon ngay

    /** [datLai] false la chi ve, giu nguyen trang dang sua: camera dang mo tu man do. */
    private fun veChon(datLai: Boolean = true) {
        if (datLai) {
            anhTam?.delete()
            anhTam = null
            ngayGoc = null
            tuGo = false
        }
        veNutNgayChup()

        val cac = NhacBai.docTrang(this).sortedByDescending { it.ngay }
        b.nhanDangNhac.visibility = if (cac.isEmpty()) View.GONE else View.VISIBLE
        b.chuDangNhac.visibility = b.nhanDangNhac.visibility
        b.danhSachTrang.removeAllViews()
        cac.forEach { tr ->
            val nut = MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "Vở ${tr.moTa()}"
                textSize = 16f
                cornerRadius = 18.dp()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 56.dp()
                ).apply { topMargin = 6.dp() }
                setOnClickListener { moTrang(tr.ngay) }
            }
            b.danhSachTrang.addView(nut)
        }
        hien(chon = true)
    }

    private fun veNutNgayChup() {
        val homNay = LocalDate.now()
        val chu = "${ngayChon.dayOfMonth}/${ngayChon.monthValue}/${ngayChon.year}"
        b.nutNgayChup.text = if (ngayChon == homNay) "Hôm nay, $chu" else chu
        b.nutChup.text = "Chụp vở ngày ${ngayChon.dayOfMonth}/${ngayChon.monthValue}"
        val daCo = NhacBai.trangNgay(this, ngayChon) != null
        b.chuDaCo.text = "Đã có vở ngày ${ngayChon.dayOfMonth}/${ngayChon.monthValue}. " +
            "Chụp nữa thì trang mới thay trang cũ."
        b.chuDaCo.visibility = if (daCo) View.VISIBLE else View.GONE
    }

    private fun chonNgayChup() {
        hopNgay(ngayChon) {
            ngayChon = it
            veNutNgayChup()
        }
    }

    /** Mo trang da luu ra sua. Khong co anh: anh goc nam trong Telegram cua ba Huy. */
    private fun moTrang(ngayTrang: LocalDate) {
        val tr = NhacBai.trangNgay(this, ngayTrang) ?: return veChon()
        anhTam?.delete()
        anhTam = null
        tuGo = false
        ngayGoc = ngayTrang
        ngayChon = ngayTrang
        hienAnh(null)
        // O tich cua lan luu truoc vao cho o tich cua may, de tin gui ba Huy ke dung cho
        // con doi lan nay. Xem [DanDoSender.chuThich].
        veSoat(ngayTrang, tr.cacDong.map { it.copy(mayTich = it.laBaiTap) })
    }

    private fun moManChup() {
        dangChup = true
        chupTraVe.launch(
            Intent(this, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_DAN_DO, true)
        )
    }

    private fun docBangMay(f: File) {
        hien(dangDoc = true)
        lifecycleScope.launch {
            val ket = withContext(Dispatchers.IO) { DocDanDo.doc(this@DanDoActivity, listOf(f)) }
            if (ket.cacNgay.isEmpty()) {
                // May doc khong duoc: con nhin anh go luon, khong cho ai doc giup (Ba Huy
                // chon 30/9/2026). Ly do chi ghi vao log: cau loi viet cho con kieu "Thử lại
                // nhé" hay "Nhờ ba Huy xem giúp", ma o day con khong phai lam hai viec do.
                android.util.Log.w("DanDoActivity", "may doc hong: ${ket.loi}")
                tuGo = true
                veSoat(ngayChon, listOf(VoDanDo.Dong("", false)))
            } else {
                tuGo = false
                chonKhoi(ket.cacNgay)
            }
        }
    }

    /**
     * Lay khoi dung ngay con chon trong cac khoi may doc ra.
     *
     * Khong co khoi nao dung ngay ma may doc ra ngay khac (khong sau hom nay) thi hoi con
     * vo nay cua ngay nao: thuong la con quen doi ngay khi chup bu vo hom truoc, co khi la
     * may doc nham ngay. Khoi khong doc ra ngay thi lay ngay con chon, khong hoi. Ngay sau
     * hom nay thi khong dua ra de chon: vo ghi ngay cua buoi vua hoc, ngay sau hom nay gan
     * nhu chac la may doc sai.
     */
    private fun chonKhoi(cac: List<VoDanDo.DanDo>) {
        val homNay = LocalDate.now()
        cac.firstOrNull { it.ngayDoc() == ngayChon }?.let { return veSoat(ngayChon, it.cacDong) }
        // Khoi cuoi trang thuong la buoi vua hoc xong, nhung khong phai luon: lay khoi gan
        // hom nay nhat ma khong phai ngay sau, khoi thieu ngay xep sau cung.
        val gan = cac.maxByOrNull { k ->
            val d = k.ngayDoc()
            when {
                d == null -> Long.MIN_VALUE + 1
                d.isAfter(homNay) -> Long.MIN_VALUE + 2
                else -> d.toEpochDay()
            }
        } ?: cac.first()
        val khac = cac.mapNotNull { it.ngayDoc() }.filter { !it.isAfter(homNay) }
            .distinct().sortedDescending()
        if (khac.isEmpty()) return veSoat(ngayChon, gan.cacDong)

        val chu = "${ngayChon.dayOfMonth}/${ngayChon.monthValue}"
        val muc = khac.map { "Vở ngày ${it.dayOfMonth}/${it.monthValue}" } + "Đúng là ngày $chu"
        MaterialAlertDialogBuilder(this)
            .setTitle("Con chọn vở ngày $chu, mà trang vở ghi ngày khác. Vở này của ngày nào?")
            .setItems(muc.toTypedArray()) { _, i ->
                val d = khac.getOrNull(i)
                if (d == null) {
                    veSoat(ngayChon, gan.cacDong)
                } else {
                    ngayChon = d
                    veSoat(d, cac.first { it.ngayDoc() == d }.cacDong)
                }
            }
            .setCancelable(false)
            .show()
    }

    // ------------------------------------------------------------------ man soat

    private fun veSoat(ngayVo: LocalDate, cacDong: List<VoDanDo.Dong>) {
        ngay = ngayVo
        veNutNgay()
        val con = getString(R.string.child_name)
        b.chuSoat.text = when {
            tuGo -> "Máy chưa đọc được trang vở này. $con nhìn ảnh, gõ lại từng dòng cô dặn " +
                "(mỗi dòng một ô, bấm + Thêm một dòng để thêm), tích những dòng là bài tập rồi bấm Lưu."
            ngayGoc != null && anhTam == null ->
                "Vở ngày ${ngayVo.dayOfMonth}/${ngayVo.monthValue} đã lưu. Sai chỗ nào thì sửa chỗ đó rồi bấm Lưu."
            else -> "Máy đọc được thế này. $con xem lại, sai chỗ nào thì sửa chỗ đó rồi bấm Lưu."
        }
        // Tu go thi phai doc duoc chu tren anh: cho anh cao hon va net hon luc chi de doi chieu.
        b.anhVo.maxHeight = (if (tuGo) 560 else 240).dp()
        hienAnh(anhTam)
        b.danhSachDong.removeAllViews()
        dong.clear()
        cacDong.forEach { themDong(it) }
        hien(soat = true)
    }

    private fun themDong(x: VoDanDo.Dong) {
        val v = StDongDanDoBinding.inflate(layoutInflater, b.danhSachDong, false)
        // Moi dong deu mang chung id o_chu va o_tich. De Android tu luu thi luc dung
        // lai man no chep chu va o tich cua dong cuoi de len moi dong, va bam Luu la
        // luu ca danh sach thanh mot dong lap lai. Tat di thi chi mat cho con vua sua.
        v.root.isSaveFromParentEnabled = false
        // Giu o tich goc cua may (hay cua Claude) theo tung dong. Mat cai nay thi luc
        // luu dong nao cung thanh "con tu them", va tin gui Ba Huy khong con bao duoc
        // cho con doi o tich.
        v.root.tag = x.mayTich
        v.oChu.setText(x.chu)
        v.oTich.isChecked = x.laBaiTap
        b.danhSachDong.addView(v.root)
        dong += v
        if (x.chu.isEmpty()) v.oChu.requestFocus()
    }

    private fun docManHinh(): List<VoDanDo.Dong> = dong
        .map { VoDanDo.Dong(it.oChu.text.toString().trim(), it.oTich.isChecked, it.root.tag as? Boolean) }
        .filter { it.chu.isNotEmpty() }

    private fun veNutNgay() {
        b.nutNgay.text = "${ngay.dayOfMonth}/${ngay.monthValue}/${ngay.year}"
    }

    private fun chonNgay() {
        hopNgay(ngay) {
            ngay = it
            veNutNgay()
        }
    }

    /** Hop chon ngay, khong cho chon ngay sau hom nay: vo ghi ngay cua buoi vua hoc. */
    private fun hopNgay(tu: LocalDate, xong: (LocalDate) -> Unit) {
        DatePickerDialog(
            this,
            { _, nam, thangTu0, ngayTrongThang -> xong(LocalDate.of(nam, thangTu0 + 1, ngayTrongThang)) },
            tu.year, tu.monthValue - 1, tu.dayOfMonth
        ).apply {
            datePicker.maxDate = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        }.show()
    }

    /**
     * Luu thanh trang cua ngay dang chon. Trang cung ngay bi thay, xem [NhacBai.ghi].
     *
     * Hai luc hoi lai truoc:
     *  - ngay do da co trang ma con chua duoc bao: da bao o man chon ngay khi chup, hay
     *    dang sua chinh trang do, thi khong hoi nua;
     *  - moi dong cua trang da qua tiet sau cua mon do: luu xong may khong nhac gi, nen
     *    noi ra. Hay gap nhat la con chon nham ngay.
     */
    private fun luu(daHoiTrung: Boolean = false, daHoiHet: Boolean = false) {
        val cac = docManHinh()
        if (cac.isEmpty()) {
            Toast.makeText(this, "Chưa có dòng nào để lưu", Toast.LENGTH_SHORT).show()
            return
        }
        val chuNgay = "${ngay.dayOfMonth}/${ngay.monthValue}"
        val daBao = ngayGoc ?: ngayChon
        if (!daHoiTrung && ngay != daBao && NhacBai.trangNgay(this, ngay) != null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Đã có vở ngày $chuNgay")
                .setMessage("Lưu thì trang này thay trang vở ngày $chuNgay đã lưu trước.")
                .setPositiveButton("Thay trang cũ") { _, _ -> luu(daHoiTrung = true, daHoiHet = daHoiHet) }
                .setNegativeButton("Sửa ngày") { _, _ -> chonNgay() }
                .show()
            return
        }
        if (!daHoiHet && NhacBai.sapToi(listOf(NhacBai.Trang(ngay, cac)), LocalDateTime.now()).isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Vở ngày $chuNgay không còn gì để nhắc")
                .setMessage(
                    "Các dòng trong vở này đều đã qua tiết sau của môn đó. Máy vẫn gửi cho " +
                        "${getString(R.string.parent_name)} nhưng không nhắc dòng nào. Nếu chọn " +
                        "nhầm ngày thì bấm Sửa ngày."
                )
                .setPositiveButton("Vẫn lưu") { _, _ -> luu(daHoiTrung = true, daHoiHet = true) }
                .setNegativeButton("Sửa ngày") { _, _ -> chonNgay() }
                .show()
            return
        }

        val bayGio = System.currentTimeMillis()
        val anh = anhTam?.let { giuAnh(it, bayGio) }
        val ban = VoDanDo.DanDo(
            ngay = ngay.toString(), cacDong = cac, luc = bayGio, anh = anh,
            nguon = if (tuGo) VoDanDo.NGUON_TU_GO else VoDanDo.NGUON_CON
        )
        NhacBai.ghi(this, ngay, cac, boNgay = ngayGoc?.takeIf { it != ngay })
        VoDanDo.ghiDaLuu(this)
        // Gui ca anh lan noi dung con vua xac nhan. Ba doi chieu duoc chu con tich
        // voi chu tren giay, va thay tung dong se nhac cho buoi nao.
        DanDoSender.guiNen(this, ban, sua = ngayGoc != null && anh == null)
        // Chua cai dat xong thi khong co cho nao de gui; dung hua voi con la da gui.
        val daGui = Prefs.get(this).isConfigured
        Toast.makeText(
            this,
            if (daGui) "Đã lưu và gửi cho ${getString(R.string.parent_name)}" else "Đã lưu",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    // ------------------------------------------------------------------ linh tinh

    private fun doiAnh(f: File) {
        anhTam?.delete()
        anhTam = f
        hienAnh(f)
    }

    private fun hienAnh(f: File?) {
        val bm = f?.takeIf { it.exists() }?.let {
            runCatching {
                android.graphics.BitmapFactory.decodeFile(
                    it.absolutePath,
                    android.graphics.BitmapFactory.Options().apply { inSampleSize = if (tuGo) 2 else 4 }
                )
            }.getOrNull()
        }
        b.anhVo.setImageBitmap(bm)
        b.anhVo.visibility = if (bm == null) View.GONE else View.VISIBLE
    }

    /**
     * Chep tam anh vua chup sang cho o lai duoc.
     *
     * Man chup tra ve file trong cacheDir - may co the don cho do bat cu luc nao, ma
     * tam anh nay con phai song toi luc gui xong.
     *
     * MOI LAN LUU MOT FILE RIENG, ten theo luc luu. Truoc 30/9/2026 chi co mot file ten co
     * dinh, vi may chi giu mot ban vo; gio DanDoSender xoa anh cua trang da soat sau khi
     * gui, va mot ten chung thi lan gui truoc xoa mat anh cua lan luu sau. File mo coi thi
     * [VoDanDo.donDep] don.
     */
    private fun giuAnh(tam: File, luc: Long): String? = runCatching {
        val kho = File(filesDir, VoDanDo.THU_MUC_ANH).apply { mkdirs() }
        val dich = File(kho, "vo-$luc.jpg")
        tam.copyTo(dich, overwrite = true)
        dich.absolutePath
    }.getOrNull()

    private fun hien(
        chon: Boolean = false,
        dangDoc: Boolean = false,
        soat: Boolean = false
    ) {
        b.boxChon.visibility = if (chon) View.VISIBLE else View.GONE
        b.boxDoc.visibility = if (dangDoc) View.VISIBLE else View.GONE
        b.boxSoat.visibility = if (soat) View.VISIBLE else View.GONE
        b.dayNut.visibility = if (soat) View.VISIBLE else View.GONE
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        /** Mo thang trang da luu cua ngay nay ra sua (yyyy-MM-dd). Cho test. */
        const val EXTRA_SUA_NGAY = "sua_ngay"

        private const val K_DANG_CHUP = "dang_chup"
        private const val K_NGAY_CHON = "ngay_chon"
        private const val K_NGAY_GOC = "ngay_goc"
        private const val K_TU_GO = "tu_go"
        private const val K_SOAT = "soat"
        private const val K_NGAY = "ngay"
        private const val K_ANH_TAM = "anh_tam"
        private const val K_CHU = "chu"
        private const val K_TICH = "tich"
        private const val K_MAY = "may"
    }
}
