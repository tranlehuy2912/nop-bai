package vn.huytl.homeworkgate.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.telegram.DanDoSender
import vn.huytl.homeworkgate.databinding.StActivityDanDoBinding
import vn.huytl.homeworkgate.databinding.StDongDanDoBinding
import java.io.File
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.data.LuatCongGio
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Chup trang vo dan do mot lan, soat lai cai may doc ra, roi luu cho ca ngay.
 *
 * VI SAO PHAI SOAT. Ban doc ra quyet dinh tron goi 45 phut va quyet dinh cau nao la
 * bai lam them - xem [vn.huytl.homeworkgate.data.LuatCongGio]. Truoc day may doc
 * tam anh ngay trong luc cham bai, doc nham thi con mat phut ma khong biet vi sao.
 *
 * Do tren bon trang vo that cua Le Hoa cho thay soat khong phai buoc cho co: may
 * doc "trang 19" thanh "trang 14", "luyen tap 3 trang 59" thanh "quyen tap 3 trang
 * 54", va xep hai bai tap that vao muc viec khac. Khong co buoc nay thi ba cho do
 * deu lang le di vao so phut.
 *
 * MOT TRANG CHUA NHIEU NGAY. Vo cua Le Hoa chep lien tay, mot trang co hai ba buoi.
 * May tra ve tung khoi, man nay bay ra thanh hang nut de con chon dung buoi cua
 * hom nay - chu khong tu doan, vi doan sai la lay nham bai cua hom khac.
 *
 * KHONG CHAN CON SUA GI CA. O ngay, o chu va o tich deu sua tay duoc. Chan lai thi
 * phai chan bang mot cai luat nao do, ma moi luat o day deu se sai vao dung hom co
 * giao ghi mot kieu la. Doi lai, tam anh goc van nam trong Telegram cua Ba Huy.
 */
class DanDoActivity : AppCompatActivity() {

    private lateinit var b: StActivityDanDoBinding

    /** Cac khoi ngay may doc ra tu trang vo. */
    private var cacNgay: List<VoDanDo.DanDo> = emptyList()
    private var dangChon = 0

    /** Ngay dang chon o o ngay, tach khoi [cacNgay] vi con sua tay duoc. */
    private var ngay: LocalDate = LocalDate.now()

    /** Cac dong dang bay tren man, de luc Luu doc nguoc ra. */
    private val dong = mutableListOf<StDongDanDoBinding>()

    /** Tam anh vua chup, chua luu. Xoa luc dong man neu con khong bam Luu. */
    private var anhTam: File? = null

    /** Duong dan ban da giu lai trong may, cua lan luu truoc. */
    private var anhDaGiu: String? = null

    private val chupTraVe = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { ket ->
        val duong = ket.data?.getStringArrayListExtra(CaptureActivity.KET_QUA_ANH).orEmpty()
        if (ket.resultCode != RESULT_OK || duong.isEmpty()) {
            // Huy tu man soat thi de nguyen man do. Chu may vua doc, cho con sua tay va
            // tam anh cua no van nam nguyen sau camera; ve lai tu ban luu hay dong man
            // la con mat ban vua doc, phai chup lai va may doc them mot luot. Ba Huy
            // chon giu nguyen ngay 26/9/2026.
            if (b.boxSoat.visibility == View.VISIBLE) return@registerForActivityResult
            // Con lai la luc vua mo man, hay dang o man may doc hong. Hai luc do van nhu
            // cu: co ban luu thi ve lai ban luu, chua co thi dong man.
            val cu = VoDanDo.doc(this)
            if (cu == null) {
                finish()
            } else {
                // Man quay ve ban da luu thi bo luon tam anh vua chup. De lai thi bam
                // Luu se ghep chu cua ban cu voi tam anh con da bo, roi gui tam do cho
                // Ba Huy va dua ma anh cua no vao tung bai nop cho Claude doi chieu.
                anhTam?.delete()
                anhTam = null
                anhDaGiu = cu.anh
                if (cu.chuaDoc) {
                    hienChuaDoc()
                } else {
                    veSoat(listOf(cu))
                    hienAnh(cu.anh?.let { File(it) })
                }
            }
            return@registerForActivityResult
        }
        val cac = duong.map { File(it) }
        doiAnh(cac.first())
        cac.drop(1).forEach { it.delete() }
        docBangMay(cac.first())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = StActivityDanDoBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }

        b.nutQuayLai.setOnClickListener { finish() }
        b.nutThuLai.setOnClickListener { moManChup() }
        b.nutDocLai.setOnClickListener { docLai() }
        b.nutGuiBa.setOnClickListener { guiChoBa() }
        b.nutChupLai.setOnClickListener { moManChup() }
        b.nutLuu.setOnClickListener { luu() }
        b.nutNgay.setOnClickListener { chonNgay() }
        b.nutThemDong.setOnClickListener { themDong(VoDanDo.Dong("", false)) }

        // Ban het han thi don di roi mo thang camera: luc do dong tren man chon mon
        // dang ghi "Chụp vở dặn dò hôm nay", bay ra dan do cua tuan truoc la lech.
        VoDanDo.donDep(this)
        val daCo = VoDanDo.doc(this)
        when {
            daCo == null -> moManChup()
            daCo.chuaDoc -> {
                anhDaGiu = daCo.anh
                hienChuaDoc()
            }
            else -> {
                anhDaGiu = daCo.anh
                veSoat(listOf(daCo))
                hienAnh(daCo.anh?.let { File(it) })
            }
        }
    }

    override fun onDestroy() {
        // Chi don ban tam. Ban da giu phai o lai: con vao sua lan sau van can no de
        // gui kem cho Ba Huy.
        //
        // Xoay may thi khong toi day: man nay tu ve lai khi xoay, xem configChanges
        // cua no trong AndroidManifest.
        anhTam?.delete()
        super.onDestroy()
    }

    private fun moManChup() {
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
                b.chuHong.text = ket.loi
                b.nutDocLai.visibility = View.VISIBLE
                // Chi tam vua chup moi gui duoc: tam da giu thi ba Huy da co roi. Chua cai
                // Telegram thi khong co ai de gui.
                b.nutGuiBa.visibility =
                    if (anhTam != null && Prefs.get(this@DanDoActivity).isConfigured) View.VISIBLE
                    else View.GONE
                hien(hong = true)
            } else {
                veSoat(ket.cacNgay)
            }
        }
    }

    // ------------------------------------------------------------------ man soat

    private fun veSoat(cac: List<VoDanDo.DanDo>) {
        cacNgay = cac
        // Vao thang khoi gan hom nay nhat ma khong phai ngay mai: vo chep lien tay
        // nen khoi cuoi trang thuong la buoi vua hoc xong, nhung khong phai luon.
        val homNay = LocalDate.now()
        dangChon = cac.indices.maxByOrNull { i ->
            val d = cac[i].ngayDoc()
            when {
                d == null -> Long.MIN_VALUE + 1
                d.isAfter(homNay) -> Long.MIN_VALUE + 2
                else -> d.toEpochDay()
            }
        } ?: 0
        veHangNgay()
        veKhoiDangChon()
        hien(soat = true)
    }

    /** Hang nut chon buoi. Mot buoi thi khong ve gi ca. */
    private fun veHangNgay() {
        b.hangNgay.removeAllViews()
        b.cuonNgay.visibility = if (cacNgay.size > 1) View.VISIBLE else View.GONE
        if (cacNgay.size <= 1) return

        cacNgay.forEachIndexed { i, d ->
            val chon = i == dangChon
            val nut = MaterialButton(
                this, null,
                if (chon) com.google.android.material.R.attr.materialButtonStyle
                else com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = d.ngayDoc()?.let { "${it.dayOfMonth}/${it.monthValue}" } ?: "?"
                textSize = 16f
                cornerRadius = 18.dp()
                minWidth = 0
                setPadding(20.dp(), 0, 20.dp(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 48.dp()
                ).apply { marginEnd = 8.dp() }
                setOnClickListener {
                    if (i == dangChon) return@setOnClickListener
                    nhoKhoiDangSua()
                    dangChon = i
                    veHangNgay()
                    veKhoiDangChon()
                }
            }
            b.hangNgay.addView(nut)
        }
    }

    private fun veKhoiDangChon() {
        val d = cacNgay.getOrNull(dangChon) ?: return
        ngay = d.ngayDoc() ?: LocalDate.now()
        veNutNgay()
        b.danhSachDong.removeAllViews()
        dong.clear()
        d.cacDong.forEach { themDong(it) }
    }

    private fun themDong(x: VoDanDo.Dong) {
        val v = StDongDanDoBinding.inflate(layoutInflater, b.danhSachDong, false)
        // Moi dong deu mang chung id o_chu va o_tich. De Android tu luu thi luc dung
        // lai man no chep chu va o tich cua dong cuoi de len moi dong, va bam Luu la
        // luu ca danh sach thanh mot dong lap lai. Tat di thi chi mat cho con vua sua.
        v.root.isSaveFromParentEnabled = false
        v.oChu.setText(x.chu)
        v.oTich.isChecked = x.laBaiTap
        b.danhSachDong.addView(v.root)
        dong += v
        if (x.chu.isEmpty()) v.oChu.requestFocus()
    }

    /** Doc nguoc man hinh ra, de doi buoi khac khong lam mat cai vua sua. */
    private fun nhoKhoiDangSua() {
        val cu = cacNgay.getOrNull(dangChon) ?: return
        cacNgay = cacNgay.toMutableList().also { it[dangChon] = cu.copy(
            ngay = ngay.toString(),
            cacDong = docManHinh()
        ) }
    }

    private fun docManHinh(): List<VoDanDo.Dong> = dong
        .map { VoDanDo.Dong(it.oChu.text.toString().trim(), it.oTich.isChecked) }
        .filter { it.chu.isNotEmpty() }

    private fun veNutNgay() {
        b.nutNgay.text = "${ngay.dayOfMonth}/${ngay.monthValue}/${ngay.year}"
    }

    private fun chonNgay() {
        DatePickerDialog(
            this,
            { _, nam, thangTu0, ngayTrongThang ->
                ngay = LocalDate.of(nam, thangTu0 + 1, ngayTrongThang)
                veNutNgay()
                veHangNgay()
            },
            ngay.year, ngay.monthValue - 1, ngay.dayOfMonth
        ).show()
    }

    /**
     * Chi luu DUNG buoi dang chon. Cac buoi khac tren trang la cua hom khac.
     *
     * Ngay tren vo khong tinh duoc cho bai hom nay thi hoi lai truoc. Truoc day man nay
     * luu im lang, roi [VoDanDo.donDep] xoa ban do o lan mo man chon mon ke tiep: dong
     * "Chụp vở dặn dò hôm nay" hien lai nhu chua chup, va khong co tron goi. Hay gap nhat
     * la may doc sai ngay, hay co ghi ngay han nop. Bam van gui thi ba Huy van nhan duoc
     * vo, chi la may khong giu no de tinh gio.
     */
    private fun luu(daHoiNgay: Boolean = false) {
        val cac = docManHinh()
        if (cac.isEmpty()) {
            Toast.makeText(this, "Chưa có dòng nào để lưu", Toast.LENGTH_SHORT).show()
            return
        }
        if (!daHoiNgay && !LuatCongGio.ngayDanDoHopLe(ngay, LocalDateTime.now())) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Ngày trên vở không tính cho hôm nay")
                .setMessage(
                    "Vở ghi ngày ${ngay.dayOfMonth}/${ngay.monthValue}/${ngay.year}. Máy chỉ dùng " +
                        "vở ghi ngày hôm nay, hoặc hôm qua khi chưa quá 12 giờ trưa, nên sẽ không " +
                        "giữ vở này để tính giờ. Nếu đây là vở của buổi học hôm nay (máy đọc sai " +
                        "ngày, hay cô ghi ngày hạn nộp) thì bấm Sửa ngày."
                )
                .setPositiveButton("Sửa ngày") { _, _ -> chonNgay() }
                .setNegativeButton("Vẫn gửi ba Huy") { _, _ -> luu(daHoiNgay = true) }
                .show()
            return
        }
        val cu = VoDanDo.doc(this)
        val bayGio = System.currentTimeMillis()
        val ban = VoDanDo.DanDo(
            ngay = ngay.toString(),
            cacDong = cac,
            luc = bayGio,
            anh = anhTam?.let { giuAnh(it) } ?: anhDaGiu,
            // Khong chup lai thi van la tam anh da gui, nen ma anh cu con dung trong luc
            // tin moi dang gui - bai nop luc do van co anh trang vo cho Claude.
            fileId = if (anhTam == null) cu?.fileId else null,
            // Cung tam anh thi giu moc chup cu: bai nop luc vo con cho ba Huy doc van
            // nhan ra day la cung mot trang. Xem VoChoCham.voChoBai.
            chupLuc = if (anhTam == null) cu?.chupLuc ?: bayGio else bayGio
        )
        VoDanDo.luu(this, ban)
        // Gui ca anh lan noi dung con vua xac nhan. Ba doi chieu duoc chu con tich
        // voi chu tren giay, va ngay khong co bai tap thi bam nut duyet ngay duoi.
        DanDoSender.guiNen(this, ban)
        // Chua cai dat xong thi khong co cho nao de gui; dung hua voi con la da gui.
        val daGui = Prefs.get(this).isConfigured
        Toast.makeText(
            this,
            if (daGui) "Đã lưu và gửi cho ${getString(R.string.parent_name)}" else "Đã lưu",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    // ---------------------------------------------------------- may doc khong duoc

    /**
     * Man cua ban chi co anh: may chua doc duoc, anh da gui ba Huy.
     *
     * Van cho doc lai, vi may doc hong thuong la mat mang hay het han muc mot luc. Doc
     * duoc thi con soat nhu moi khi, va ban soat do thay cho ban chi co anh.
     */
    private fun hienChuaDoc() {
        b.chuHong.text = "Máy chưa đọc được trang vở này. Ảnh đã gửi " +
            "${getString(R.string.parent_name)}, ba sẽ nhờ Claude đọc giúp.\n\n" +
            "Mấy lần nộp bài sau không phải chụp lại vở."
        b.nutDocLai.visibility = if (anhDaGiu != null) View.VISIBLE else View.GONE
        b.nutGuiBa.visibility = View.GONE
        hien(hong = true)
    }

    /** Doc lai dung tam dang co, khong mo camera. */
    private fun docLai() {
        val f = anhTam ?: anhDaGiu?.let { File(it) }?.takeIf { it.exists() }
        if (f == null) {
            moManChup()
            return
        }
        docBangMay(f)
    }

    /**
     * May doc khong duoc: giu tam anh lam vo dan do cua ngay, chua co chu, roi gui ba
     * Huy. Xem [VoDanDo.DanDo.chuaDoc].
     *
     * Tu day con khong phai chup lai vo o moi lan nop: tablet gan tam nay theo tung bai,
     * ba Huy nho Claude doc duoc, va lan Nho Claude cham dau tien cung doc duoc no.
     *
     * Ngay tam la hom nay, vi chua ai doc thi chua biet vo ghi ngay nao. Doc xong thi
     * ngay ghi tren vo thay vao.
     */
    private fun guiChoBa() {
        val tam = anhTam ?: return
        val anh = giuAnh(tam)
        if (anh == null) {
            Toast.makeText(this, "Không lưu được ảnh, chụp lại nhé", Toast.LENGTH_SHORT).show()
            return
        }
        val ban = VoDanDo.DanDo(
            ngay = LocalDate.now().toString(),
            cacDong = emptyList(),
            anh = anh,
            chuaDoc = true
        )
        VoDanDo.luu(this, ban)
        DanDoSender.guiNen(this, ban)
        Toast.makeText(
            this,
            "Đã gửi ảnh cho ${getString(R.string.parent_name)}. Mấy lần nộp sau không phải chụp lại vở.",
            Toast.LENGTH_LONG
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
                    android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
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
     * tam anh nay con phai song het ngay de moi lan con sua lai la gui kem cho ba.
     *
     * MOT BAN DUY NHAT, TEN CO DINH, ghi de ban cu. Ban do bi xoa hai duong: chup
     * trang khac roi bam Luu thi ghi de, va het han thi [VoDanDo.donDep] xoa han.
     */
    private fun giuAnh(tam: File): String? = runCatching {
        val kho = File(filesDir, "dando").apply { mkdirs() }
        val dich = File(kho, "trang.jpg")
        tam.copyTo(dich, overwrite = true)
        dich.absolutePath
    }.getOrNull()

    private fun hien(dangDoc: Boolean = false, soat: Boolean = false, hong: Boolean = false) {
        b.boxDoc.visibility = if (dangDoc) View.VISIBLE else View.GONE
        b.boxSoat.visibility = if (soat) View.VISIBLE else View.GONE
        b.boxHong.visibility = if (hong) View.VISIBLE else View.GONE
        b.dayNut.visibility = if (soat) View.VISIBLE else View.GONE
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
