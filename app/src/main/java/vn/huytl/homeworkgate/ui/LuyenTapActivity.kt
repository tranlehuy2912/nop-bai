package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.databinding.StActivityLuyenTapBinding
import vn.huytl.homeworkgate.databinding.StDongViecBinding
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.BoTuVung
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import java.util.Calendar

/**
 * Trang Luyen tap: moi viec luyen them nam o day, man chinh chi con mot dong "Luyện tập".
 *
 * VI SAO CO (Ba Huy chot 30/9/2026). Truoc do Lam bai tren may, On lai, Giai de, Kiem tra
 * bai, Do tu vung moi viec mot dong o khoi Viec hom nay cua man chinh, va moi mon, moi de
 * them vao la them dong: ngay co de Giai de ca ba mon thi khoi do dai hon mot man hinh. Man
 * chinh giu nhung viec gan voi ngay gio (vo dan do, bai cho buoi sau, soan tap, cau can sua,
 * de Giai de dang lam do vi no dang chay gio), con viec luyen them - lam luc nao cung duoc
 * - vao day.
 *
 * LOP DA HOC TOI DAU NAM TREN CUNG. Moc [HocToi] dung chung cho moi viec ben duoi (bo the
 * Kiem tra bai, sach bai tap cua Lam bai va Giai de, Unit cua Do tu vung va Tieng Anh), ma
 * truoc day chi hien luc con bam vao mot bo chua chon, doi lai thi phai tim nut "Đổi" tren
 * the bo trong tung man. Nay moi phan mot dong, bam la mo dung hop chon cu ([ChonHocToi]),
 * nen luat chon va dong nhat ky di sang Bang dieu khien khong doi. Cac hop hoi trong tung
 * man van giu, phong khi con vao thang mot bo chua chon.
 *
 * DE THI IN SAN (30/9/2026, [vn.huytl.homeworkgate.kho.DeThi]). Moi mon co de thi mot dong
 * "Đề thi thử <mon>", theo thu tu [GiaiDe.MON] (Toan, KHTN, Tieng Anh); mon chua co de nao thi
 * khong hien. Truoc 1/10/2026 chi co de Anh nen chi co dong "Đề thi thử Tiếng Anh". Tu ngay do
 * co ca de Toan, KHTN, moi mon mot dong nhu hang de thi o Bang dieu khien, va dong phu (de nao
 * dang mo, pham vi cua de khoa) noi rieng cho mot mon. Bam dong la ra danh sach de cua mon kem
 * tinh trang: mo duoc, dang mo, da lam, hay chua toi pham vi (de Anh theo Unit, de Toan, KHTN
 * theo bai cua tung phan, xem [GiaiDe.thieuPhamVi]). De dang mo co them dong rieng mang ten de,
 * nhu moi de Giai de. Con doi moc o khoi tren thi [nap] chay lai [GiaiDe.taoNeuCan], nen de toi
 * pham vi tu mo ngay luc do.
 *
 * Trang nay khong ve lai moi giay nhu man chinh, nen dem cau on va doc de Giai de ca luc
 * dang choi cung khong sao. Doc kho ngoai luong giao dien, roi moi ve.
 */
class LuyenTapActivity : AppCompatActivity() {

    private lateinit var b: StActivityLuyenTapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = StActivityLuyenTapBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
        b.nutQuayLai.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        nap()
    }

    /** Nhung gi can hoi kho, doc mot lan cho mot lan ve. */
    private data class Nap(
        val soOn: Int,
        val deMo: List<DeGiai>,
        val deXong: List<Pair<DeGiai, Pair<Int, Int>>>,
        val deThi: List<GiaiDe.TinhTrangDeThi>,
        val kiemTra: BoThe.TinhTrang,
        val coTuVung: Boolean,
        val tuVungXong: Boolean
    )

    private fun nap() {
        lifecycleScope.launch {
            val ct = this@LuyenTapActivity
            val n = withContext(Dispatchers.IO) {
                val bayGio = System.currentTimeMillis()
                runCatching { GiaiDe.taoNeuCan(ct) }
                val kho = KhoBai.get(ct)
                val coTu = runCatching { BoTuVung.BO.any { kho.soTuCua(it.bo) > 0 } }.getOrDefault(false)
                Nap(
                    soOn = LamTrenMay.soCauOn(ct, bayGio),
                    // De da bat dau (dang lam, hay con chup tu luan) nam ngoai man chinh.
                    deMo = runCatching { GiaiDe.dangMo(ct, bayGio) }.getOrDefault(emptyList())
                        .filter { !it.daBatDau },
                    deXong = runCatching { GiaiDe.xongHomNay(ct, bayGio) }.getOrDefault(emptyList())
                        .map { it to GiaiDe.diem(ct, it) },
                    deThi = runCatching { GiaiDe.tinhTrangDeThi(ct, bayGio) }.getOrDefault(emptyList()),
                    kiemTra = runCatching { BoThe.tinhTrangManChinh(ct) }.getOrDefault(BoThe.TinhTrang.KHONG),
                    coTuVung = coTu,
                    tuVungXong = coTu && runCatching {
                        kho.giayTuVungTu(moc0Gio()) >= LuatTuVung.GIAY_TRAN_MOI_NGAY
                    }.getOrDefault(false)
                )
            }
            ve(n)
        }
    }

    private fun ve(n: Nap) {
        b.boxHocToi.removeAllViews()
        b.boxLuyen.removeAllViews()
        veHocToi()

        // On lai. Khong co cau den hen thi van hien, dang da xong: an di thi con khong biet
        // duong on nam o day.
        themDong(
            b.boxLuyen,
            hinh = R.drawable.st_ic_on_lai,
            mau = if (n.soOn > 0) R.color.wait else R.color.ok,
            ten = when (n.soOn) {
                0 -> "Ôn lại: hôm nay chưa có câu đến hẹn"
                1 -> "Ôn lại 1 câu đến hẹn"
                else -> "Ôn lại ${n.soOn} câu đến hẹn"
            },
            xong = n.soOn == 0
        ) { if (n.soOn > 0) LamBaiActivity.moOn(this) }

        // Luyen tap tung mon (truoc 30/9/2026 ten la "Làm bài <mon> trên máy"): moi mon mot
        // dong, thay cho hop hoi mon cua man chinh cu.
        LamTrenMay.MON.filter { NganHang.sachBaiTapCua(it).isNotEmpty() }.forEach { mon ->
            themDong(
                b.boxLuyen,
                hinh = R.drawable.st_ic_the_hoc,
                mau = MatMon.mau(mon),
                ten = "Luyện tập ${GiaiDe.tenMon(mon)}",
                phu = if (chuaChonMoc(mon)) "Chọn bài lớp đã học trước" else ""
            ) { LamBaiActivity.moLamThem(this, mon) }
        }

        // Giai de: de dang mo chua bat dau, de xong hom nay kem diem.
        n.deMo.forEach { de ->
            // De thi mang ten rieng ("Đề giữa kì 1 số 1") len dong tren: ngay duoi con dong
            // "Đề thi thử <mon>" cua ca danh sach de mon do, hai dong cung ten thi con khong
            // biet bam dong nao de lam.
            val laDeThi = de.loai == GiaiDe.LOAI_DE_THI
            themDong(
                b.boxLuyen,
                hinh = R.drawable.st_ic_giai_de,
                mau = MatMon.mau(de.mon),
                ten = if (laDeThi) de.ten else GiaiDe.tenDe(de),
                phu = "${if (laDeThi) GiaiDe.tenDe(de) else de.ten} · ${de.cauIds.size} câu · " +
                    "khoảng ${de.phutGoiY} phút" + GiaiDe.ngayKiemTra(de)
            ) { GiaiDeActivity.mo(this, de.id) }
        }
        n.deXong.forEach { (de, diem) ->
            // De lam tren may cham bang sao, khong phai so cau dung (xem GiaiDe.diem).
            val ten = if (de.loai == GiaiDe.LOAI_DE_THI) "${GiaiDe.tenDe(de)} (${de.ten})" else GiaiDe.tenDe(de)
            themDong(
                b.boxLuyen,
                hinh = R.drawable.st_ic_giai_de,
                mau = R.color.ok,
                ten = if (de.trenMay) "$ten: ${diem.first}/${diem.second} ★"
                else "$ten: đúng ${diem.first}/${diem.second} câu",
                xong = true
            ) { GiaiDeActivity.mo(this, de.id) }
        }

        // De thi in san: moi mon mot dong (1/10/2026), bam la hien danh sach de cua mon. De
        // dang mo da co dong rieng o tren (nhu moi de Giai de), dong nay de xem de nao con, de
        // nao da lam, va tu mo.
        GiaiDe.MON.forEach { mon ->
            val cua = n.deThi.filter { it.de.mon == mon }
            if (cua.isEmpty()) return@forEach
            // De dang lam truoc de moi mo: con thay ngay de minh dang lam do (2/10/2026, truoc do
            // dong nay ghi "đang mở" ca khi con da bam Bat dau, lech voi Bang dieu khien).
            val mo = cua.firstOrNull { it.trangThai == GiaiDe.TT_DANG } ?: cua.firstOrNull { it.dangMo != null }
            val san = cua.count { it.trangThai == GiaiDe.TT_SAN }
            val xong = cua.count { it.trangThai == GiaiDe.TT_XONG }
            // Chi con de khoa thi noi pham vi cua de hep nhat: lop hoc toi do la co de dau tien.
            // De Anh do rong la so Unit, nen van ra "Mở khi lớp học tới Unit 3" nhu truoc.
            val khoaHep = cua.filter { it.trangThai == GiaiDe.TT_KHOA }.minByOrNull { it.de.doRong }
            themDong(
                b.boxLuyen,
                hinh = R.drawable.st_ic_giai_de,
                mau = MatMon.mau(mon),
                ten = "Đề thi thử ${GiaiDe.tenMon(mon)}",
                phu = when {
                    mo != null -> "${mo.de.ten} " + (if (mo.trangThai == GiaiDe.TT_DANG) "đang làm" else "đang mở") +
                        if (san > 0) ", thêm $san đề mở được" else ""
                    san > 0 -> "$san đề mở được" + if (xong > 0) ", đã làm $xong đề" else ""
                    xong > 0 -> "Đã làm $xong đề"
                    khoaHep != null -> "Mở khi lớp học ${GiaiDe.moTaPhamVi(khoaHep.de)}"
                    else -> ""
                }
            ) { hoiDeThi(mon, cua) }
        }
        // Mon chua chon moc thi khong ra de: truoc 30/9/2026 o day co them dong "Giải đề <mon>"
        // de hoi moc, nay khoi Lop da hoc toi o tren da lam viec do (dong do to do).

        // Kiem tra bai. Het the hom nay ma con bai phia sau thi dong van o, dang da xong.
        if (n.kiemTra != BoThe.TinhTrang.KHONG) {
            val het = n.kiemTra == BoThe.TinhTrang.HET_HOM_NAY
            themDong(
                b.boxLuyen,
                hinh = R.drawable.st_ic_the_hoc,
                mau = if (het) R.color.ok else R.color.brand,
                ten = if (het) "Kiểm tra bài: hôm nay hết câu" else getString(R.string.hoc_thuoc_nut),
                xong = het
            ) { startActivity(Intent(this, HocThuocActivity::class.java)) }
        }

        // Do tu vung. Du phut cua ngay thi van hien, dang da xong, cung ly do voi On lai.
        if (n.coTuVung) {
            themDong(
                b.boxLuyen,
                hinh = R.drawable.st_ic_the_hoc,
                mau = if (n.tuVungXong) R.color.ok else MatMon.mau(PhanHoc.TIENG_ANH),
                ten = if (n.tuVungXong) "Kiểm tra từ vựng: hôm nay đủ rồi" else getString(R.string.do_tu_nut),
                xong = n.tuVungXong
            ) { startActivity(Intent(this, DoTuVungActivity::class.java)) }
        }
    }

    /** Moi phan hoc mot dong, roi Unit Tieng Anh. */
    private fun veHocToi() {
        PhanHoc.TAT_CA.forEach { phan ->
            if (PhanHoc.cacBai(this, phan).isEmpty()) return@forEach
            val so = PhanHoc.hocToi(this, phan)
            themDong(
                b.boxHocToi,
                hinh = R.drawable.st_ic_cap_sach,
                mau = if (so == null) R.color.alert else MatMon.mau(phan.mon),
                ten = phan.ten,
                phu = when (so) {
                    null -> "Chưa chọn"
                    0 -> "Chưa học bài nào"
                    else -> "Đã học tới Bài $so"
                }
            ) { ChonHocToi.hoiPhan(this, phan) { nap() } }
        }
        BoTuVung.BO.forEach { bo ->
            val unit = HocToi.unitCua(this, bo.bo)
            themDong(
                b.boxHocToi,
                hinh = R.drawable.st_ic_cap_sach,
                mau = if (unit == null) R.color.alert else MatMon.mau(bo.mon),
                ten = bo.mon,
                phu = when {
                    unit == null -> "Chưa chọn"
                    unit <= HocToi.CHUA_HOC_UNIT_NAO -> "Chưa học Unit nào"
                    else -> "Đã học tới Unit $unit"
                }
            ) { ChonHocToi.hoiUnit(this, bo) { nap() } }
        }
    }

    /**
     * Danh sach de thi cua mot mon: moi de mot dong kem tinh trang. Bam de dang mo thi vao lam,
     * de mo duoc thi mo, de da lam thi xem lai hay lam lai, de chua toi pham vi thi noi pham vi
     * ("mở khi học tới Unit 3", "mở khi học Đại số tới Bài 9, Hình học tới Bài 14").
     */
    private fun hoiDeThi(mon: String, cac: List<GiaiDe.TinhTrangDeThi>) {
        val dong = cac.map { t ->
            val sao = t.lanCuoi?.let { "${it.saoDat.coerceAtLeast(0)}/${it.saoToiDa} ★" }.orEmpty()
            t.de.ten + " · " + when (t.trangThai) {
                GiaiDe.TT_MO -> "đang mở"
                GiaiDe.TT_DANG -> "đang làm"
                GiaiDe.TT_XONG -> "đã làm $sao"
                GiaiDe.TT_SAN -> "chưa làm"
                else -> "mở khi học ${GiaiDe.moTaPhamVi(t.de)}"
            }
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Đề thi thử ${GiaiDe.tenMon(mon)}")
            .setItems(dong) { _, i -> chonDeThi(cac[i]) }
            .setNegativeButton("Đóng", null)
            .show()
    }

    /**
     * Cau noi khi con bam mot de chua toi pham vi. De Anh giu cau cu (30/9/2026): de chi mot
     * phan, them dong "Hiện ..." thi chi nhac lai so Unit vua noi. De Toan, KHTN (1/10/2026) mo
     * theo nhieu phan, nen noi phan nao con thieu ([GiaiDe.moTaThieu]) va dong nao can doi o khoi
     * Lop da hoc toi: ten dong o do la [PhanHoc.Phan.ten] ("Toán 8 Hình học", xem [veHocToi]),
     * tim theo [GiaiDe.Thieu.maPhan].
     */
    private fun loiDeKhoa(t: GiaiDe.TinhTrangDeThi): String {
        val phamVi = "Đề này mở khi lớp học ${GiaiDe.moTaPhamVi(t.de)}."
        if (t.de.laTiengAnh) return "$phamVi Lớp học tới đó rồi thì đổi dòng Tiếng Anh ở khối Lớp đã học tới."
        if (t.thieu.isEmpty()) return phamVi
        val ten = t.thieu.map { th -> PhanHoc.theoMa(th.maPhan)?.ten ?: th.tenPhan }
        val cacDong = if (ten.size == 1) "dòng ${ten.first()}"
        else "các dòng ${ten.dropLast(1).joinToString(", ")} và ${ten.last()}"
        return "$phamVi Hiện ${GiaiDe.moTaThieu(t.thieu)}. Lớp học tới đó rồi thì đổi $cacDong ở khối Lớp đã học tới."
    }

    private fun chonDeThi(t: GiaiDe.TinhTrangDeThi) {
        t.dangMo?.let { return GiaiDeActivity.mo(this, it.id) }
        when (t.trangThai) {
            GiaiDe.TT_KHOA -> MaterialAlertDialogBuilder(this)
                .setTitle(t.de.ten)
                .setMessage(loiDeKhoa(t))
                .setPositiveButton("Đã hiểu", null)
                .show()
            GiaiDe.TT_XONG -> {
                val cu = t.lanCuoi ?: return
                MaterialAlertDialogBuilder(this)
                    .setTitle(t.de.ten)
                    .setMessage("Lần làm trước được ${cu.saoDat.coerceAtLeast(0)}/${cu.saoToiDa} ★.")
                    .setPositiveButton("Làm lại") { _, _ -> moDeThi(t.de.ma) }
                    .setNegativeButton("Xem bài đã làm") { _, _ -> GiaiDeActivity.mo(this, cu.id) }
                    .setNeutralButton("Đóng", null)
                    .show()
            }
            else -> moDeThi(t.de.ma)
        }
    }

    private fun moDeThi(ma: String) {
        lifecycleScope.launch {
            val ct = this@LuyenTapActivity
            val kq = withContext(Dispatchers.IO) { GiaiDe.moDeThi(ct, ma, choBa = false) }
            val de = kq.de
            if (de != null) {
                GiaiDeActivity.mo(ct, de.id)
            } else {
                MaterialAlertDialogBuilder(ct)
                    .setMessage(kq.loi ?: "Máy chưa mở được đề này.")
                    .setPositiveButton("Đã hiểu", null)
                    .show()
            }
        }
    }

    private fun chuaChonMoc(mon: String): Boolean =
        if (mon == PhanHoc.TIENG_ANH) HocToi.unitCua(this, PhanHoc.BO_TIENG_ANH) == null
        else PhanHoc.chuaChon(this, mon).isNotEmpty()

    /** Mot dong, cung khuon voi dong o khoi Viec hom nay cua man chinh. */
    private fun themDong(
        box: LinearLayout,
        hinh: Int,
        mau: Int,
        ten: String,
        phu: String = "",
        xong: Boolean = false,
        bam: () -> Unit
    ) {
        val v = StDongViecBinding.inflate(layoutInflater, box, false)
        v.hinh.setImageResource(hinh)
        v.hinh.imageTintList = ContextCompat.getColorStateList(this, mau)
        v.hinh.backgroundTintList = ContextCompat.getColorStateList(
            this, if (xong) R.color.ok_soft else R.color.canvas
        )
        v.ten.text = ten
        v.phu.text = phu
        v.phu.visibility = if (phu.isBlank()) View.GONE else View.VISIBLE
        v.duoi.text = if (xong) "✓" else "›"
        v.duoi.setTextColor(ContextCompat.getColor(this, if (xong) R.color.ok else R.color.ink_soft))
        v.root.setOnClickListener { bam() }
        box.addView(v.root)
    }

    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        fun mo(activity: Activity) {
            activity.startActivity(Intent(activity, LuyenTapActivity::class.java))
        }
    }
}
