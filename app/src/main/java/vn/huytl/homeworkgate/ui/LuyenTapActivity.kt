package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.databinding.StActivityLuyenTapBinding
import vn.huytl.homeworkgate.databinding.StDongViecBinding
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
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
 * BA KHU THEO MON (Ba Huy chot 2/10/2026). Truoc ngay do trang co hai khoi: "Lớp đã học tới"
 * (moi phan hoc mot dong) va "Bài luyện tập" (On lai chung, Luyen tap tung mon, cac de, Kiem
 * tra bai, Do tu vung). Nay moi mon mot khu Toan, KHTN, Tieng Anh, va trong khu theo dung thu
 * tu Ba Huy ke: Bai da hoc, On tap, Kiem tra cong thuc (Tieng Anh: Kiem tra tu vung), Luyen tap
 * tung phan (Toan Dai so, Hinh hoc; KHTN Hoa hoc, Vat li, Sinh hoc; Tieng Anh mot dong), de on
 * dang mo (de tuan thu Bay, de on truoc kiem tra), roi bon dong De thi thu theo ky. Nhin mot
 * khu la biet mon do con bao nhieu viec.
 *
 * LAM BAI TAP TRONG SGK (cung ngay, Ba Huy chot sau): dong ngay duoi Kiem tra, tren Luyen tap
 * tung phan, mo [BaiSgkActivity]. Bai SGK lam tren may thay cho lam vo roi chup; mon chua co SGK
 * trong may (Tieng Anh luc viet) thi khong co dong nay.
 *
 * CAU DA LAM DUNG (cung ngay): moi khu mot dong ngay duoi cac dong Luyen tap, mo [CauDaLamActivity]
 * de xem lai de, cau con da tra loi va loi giai cua moi cau da lam dung tren may.
 *
 * BAI DA HOC LA DONG DAU MOI KHU: bam la hop danh dau nhieu bai ([ChonHocToi.hoiMon]). Moi viec
 * trong khu dung chung tap bai do ([PhanHoc.baiDaHoc]). Doi xong thi [nap] chay lai
 * [GiaiDe.taoNeuCan], nen de du pham vi tu mo ngay luc do.
 *
 * SO DA LAM / TONG o cuoi moi dong (cung ngay), dem tu truoc toi nay. Tong la moi cau cua phan
 * ke ca bai lop chua hoc, nen so dung yen, khong nhay khi con danh dau them bai (Ba Huy chon).
 * Luyen tap: cau da lam dung it nhat mot lan ([LamTrenMay.demTheoPhan]). On tap: so cau da on
 * hom nay tren so cau den hen trong ngay; khong co cau nao den hen thi chi dau ✓. Kiem tra:
 * the da kiem, tu da thuoc, dem nhu trong hai man do. De thi thu: so de da lam tren so de cua ky.
 *
 * DE THI THU THEO KY. Moi mon bon dong giua ky I, cuoi ky I, giua ky II, cuoi ky II
 * ([GiaiDe.CAC_KY]); ky chua co de nao thi dong mo "Chưa có đề". Ky dang co de mo thi tieu de
 * ghi so de ("Đề thi thử số 5 giữa kỳ I môn KHTN"). Bam dong la ra danh sach de cua ky kem tinh
 * trang: mo duoc, dang mo, da lam, hay con thieu bai nao ([GiaiDe.thieuPhamVi]). Truoc ngay do
 * moi mon mot dong "Đề thi thử <môn>" cho ca bo de, va de dang mo co them mot dong rieng.
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

    /** Mot dong Luyen tap: phan (null la ca mon), so cau da lam dung, tong so cau. */
    private data class Luyen(val phan: PhanHoc.Phan?, val daLam: Int, val tong: Int)

    /**
     * Dong "Làm bài tập trong SGK": ten cac quyen SGK, so cau da lam tren tong so cau cua cac quyen
     * (dem nhu man khai bai, ke ca cau phai viet), so bai dang lam do.
     */
    private data class Sgk(val tenSach: String, val daLam: Int, val tong: Int, val dangLam: Int)

    /** Nhung gi mot khu mon can, doc mot lan cho mot lan ve. */
    private data class Khu(
        val mon: String,
        /** Bai da hoc cua mon, null khi chua chon. */
        val daHoc: Set<Int>?,
        val daOn: Int,
        val conOn: Int,
        /** Da kiem (Tieng Anh: da thuoc) tren tong; null khi mon khong co bo the, bo tu. */
        val kiemTra: Pair<Int, Int>?,
        /** Dong Kiem tra da xong hom nay: het the den luot, hay du phut tu vung. */
        val kiemTraXong: Boolean,
        val luyen: List<Luyen>,
        /** Dong "Làm bài tập trong SGK"; null khi mon chua co SGK trong may (Tieng Anh luc viet). */
        val sgk: Sgk?,
        /** So cau da lam dung tren may, cho dong "Câu đã làm đúng" ([CauDaLamActivity]). */
        val soDung: Int,
        /** De tuan, de on kiem tra dang mo ma chua bat dau. De da bat dau nam o man chinh. */
        val deOn: List<DeGiai>,
        /** De on xong hom nay, kem diem. De thi thu xong thi nam trong dong cua ky. */
        val deOnXong: List<Pair<DeGiai, Pair<Int, Int>>>,
        val deThi: List<GiaiDe.TinhTrangDeThi>
    )

    private fun nap() {
        lifecycleScope.launch {
            val ct = this@LuyenTapActivity
            val cac = withContext(Dispatchers.IO) {
                val bayGio = System.currentTimeMillis()
                runCatching { GiaiDe.taoNeuCan(ct) }
                val dangMo = runCatching { GiaiDe.dangMo(ct, bayGio) }.getOrDefault(emptyList())
                val xong = runCatching { GiaiDe.xongHomNay(ct, bayGio) }.getOrDefault(emptyList())
                val deThi = runCatching { GiaiDe.tinhTrangDeThi(ct, bayGio) }.getOrDefault(emptyList())
                KHU.map { mon -> docKhu(ct, mon, bayGio, dangMo, xong, deThi) }
            }
            listOf(b.boxToan, b.boxKhtn, b.boxAnh).zip(cac).forEach { (box, khu) -> veKhu(box, khu) }
        }
    }

    private fun docKhu(
        ct: Context,
        mon: String,
        bayGio: Long,
        dangMo: List<DeGiai>,
        xong: List<DeGiai>,
        deThi: List<GiaiDe.TinhTrangDeThi>
    ): Khu {
        val kho = KhoBai.get(ct)
        val kiemTra: Pair<Int, Int>?
        val kiemTraXong: Boolean
        if (mon == PhanHoc.TIENG_ANH) {
            val tu = runCatching { kho.tinhTrangTu(PhanHoc.BO_TIENG_ANH, LuatTuVung.LAN_DUNG_DE_TINH) }
                .getOrDefault(emptyList())
            kiemTra = if (tu.isEmpty()) null
            else tu.count { it.second == LuatTuVung.TinhTrang.DA_THUOC } to tu.size
            kiemTraXong = runCatching {
                kho.giayTuVungTu(moc0Gio()) >= LuatTuVung.GIAY_TRAN_MOI_NGAY
            }.getOrDefault(false)
        } else {
            val bang = runCatching { BoThe.bang(ct, mon) }.getOrDefault(emptyList())
            kiemTra = if (bang.isEmpty()) null else bang.sumOf { it.soThuoc } to bang.sumOf { it.tongThe }
            // Het the hom nay ma bo nao cung da chon: dong o dang da xong. Con bo chua chon thi
            // chua xong, vao la duoc hoi chon bai.
            kiemTraXong = bang.isNotEmpty() && bang.none { it.hocToi == null } && bang.sumOf { it.soDenLuot } == 0
        }
        val sgk = runCatching {
            val quyen = NganHang.sachGiaoKhoaCua(mon)
            if (quyen.isEmpty()) return@runCatching null
            val tienBo = quyen.map { NganHang.tienBo(ct, it.nguon) }
            Sgk(
                tenSach = gonTenSach(quyen.map { it.ten }),
                daLam = tienBo.sumOf { it.first },
                tong = tienBo.sumOf { it.second },
                dangLam = GiaiDe.baiSgkDangLam(ct, mon, bayGio = bayGio).size
            )
        }.getOrNull()
        val dem = runCatching { LamTrenMay.demTheoPhan(ct, mon) }.getOrDefault(emptyMap())
        val luyen = if (mon == PhanHoc.TIENG_ANH) listOf(Luyen(null, dem[""]?.first ?: 0, dem[""]?.second ?: 0))
        else PhanHoc.cuaMon(mon).map { p -> Luyen(p, dem[p.ma]?.first ?: 0, dem[p.ma]?.second ?: 0) }
        return Khu(
            mon = mon,
            daHoc = PhanHoc.baiDaHoc(ct, mon),
            daOn = LamTrenMay.soDaOnHomNay(ct, mon, bayGio),
            conOn = LamTrenMay.soCauOn(ct, bayGio, mon),
            kiemTra = kiemTra,
            kiemTraXong = kiemTraXong,
            // Phan chua soan cau nao thi khong co gi de bam: an dong.
            luyen = luyen.filter { it.tong > 0 },
            sgk = sgk,
            soDung = LamTrenMay.soCauDaLamDung(ct, mon),
            deOn = dangMo.filter { it.mon == mon && it.loai != GiaiDe.LOAI_DE_THI && !it.daBatDau },
            deOnXong = xong.filter { it.mon == mon && it.loai != GiaiDe.LOAI_DE_THI }
                .map { it to runCatching { GiaiDe.diem(ct, it) }.getOrDefault(0 to 0) },
            deThi = deThi.filter { it.de.mon == mon }
        )
    }

    private fun veKhu(box: LinearLayout, k: Khu) {
        box.removeAllViews()
        val mon = k.mon
        val ten = GiaiDe.tenMon(mon)
        val mauMon = MatMon.mau(mon)

        // Bai da hoc: dong dau khu (Ba Huy chot 2/10/2026), moi viec ben duoi dung chung.
        themDong(
            box,
            hinh = R.drawable.st_ic_cap_sach,
            mau = if (k.daHoc == null) R.color.alert else mauMon,
            ten = "Bài đã học",
            phu = k.daHoc?.let { HocToi.moTa(mon, it).replaceFirstChar { c -> c.uppercase() } } ?: "Chưa chọn"
        ) { ChonHocToi.hoiMon(this, mon) { nap() } }

        // On tap: van hien khi khong co cau den hen, dang da xong: an di thi con khong biet
        // duong on nam o day.
        val denHen = k.daOn + k.conOn
        themDong(
            box,
            hinh = R.drawable.st_ic_on_lai,
            mau = if (k.conOn > 0) R.color.wait else R.color.ok,
            ten = "Ôn tập $ten",
            phu = when {
                denHen == 0 -> "Hôm nay chưa có câu đến hẹn"
                k.conOn == 0 -> "Hôm nay ôn xong rồi"
                else -> "${k.conOn} câu đến hẹn"
            },
            so = if (denHen > 0) "${k.daOn}/$denHen" else "",
            xong = k.conOn == 0
        ) { if (k.conOn > 0) LamBaiActivity.moOn(this, mon) }

        // Kiem tra cong thuc (Tieng Anh: Kiem tra tu vung). Xong hom nay thi van o, dang da
        // xong, cung ly do voi On tap.
        k.kiemTra?.let { (da, tong) ->
            val laAnh = mon == PhanHoc.TIENG_ANH
            themDong(
                box,
                hinh = R.drawable.st_ic_the_hoc,
                mau = if (k.kiemTraXong) R.color.ok else mauMon,
                ten = if (laAnh) "Kiểm tra từ vựng $ten" else "Kiểm tra công thức $ten",
                phu = when {
                    k.daHoc == null -> "Chọn bài lớp đã học trước"
                    !k.kiemTraXong -> ""
                    laAnh -> "Hôm nay đủ rồi"
                    else -> "Hôm nay hết câu"
                },
                so = "$da/$tong",
                xong = k.kiemTraXong
            ) {
                if (laAnh) startActivity(Intent(this, DoTuVungActivity::class.java))
                else HocThuocActivity.mo(this, mon)
            }
        }

        // Bai tap SGK (Ba Huy chot 2/10/2026): ngay duoi Kiem tra, tren Luyen tap tung phan; cung
        // bieu tuong voi cac dong khac, khong nhan "mới". Bai SGK lam tren may, khong lam vo roi
        // chup nua. Bam la chon quyen, chon bai, chon cau ([BaiSgkActivity]).
        k.sgk?.let { s ->
            themDong(
                box,
                hinh = R.drawable.st_ic_the_hoc,
                mau = mauMon,
                ten = "Làm bài tập trong SGK",
                phu = if (s.dangLam > 0) "Đang làm ${s.dangLam} bài" else s.tenSach,
                so = "${s.daLam}/${s.tong}"
            ) { BaiSgkActivity.mo(this, mon) }
        }

        // Luyen tap tung phan. Phan chua co bai nao danh dau thi bam la hoi danh dau truoc:
        // vao man lam bai luc do chi gap cau "het cau".
        k.luyen.forEach { l ->
            val coBai = l.phan?.let { p -> k.daHoc?.any { p.chua(it) } } ?: !k.daHoc.isNullOrEmpty()
            themDong(
                box,
                hinh = R.drawable.st_ic_the_hoc,
                mau = mauMon,
                ten = "Luyện tập $ten" + (l.phan?.let { " ${it.tenDai}" } ?: ""),
                phu = when {
                    k.daHoc == null -> "Chọn bài lớp đã học trước"
                    !coBai -> "Chưa đánh dấu bài nào của phần này"
                    else -> ""
                },
                so = "${l.daLam}/${l.tong}"
            ) {
                if (coBai) LamBaiActivity.moLamThem(this, mon, l.phan)
                else ChonHocToi.hoiMon(this, mon) { nap() }
            }
        }

        // Xem lai cau da lam dung tren may: de, cau con tra loi, loi giai (Ba Huy chot 2/10/2026).
        themDong(
            box,
            hinh = R.drawable.st_ic_da_cham,
            mau = mauMon,
            ten = "Câu đã làm đúng",
            phu = if (k.soDung == 0) "Chưa có câu nào" else "Xem lại đề, câu đã trả lời và lời giải",
            so = "${k.soDung} câu"
        ) { CauDaLamActivity.mo(this, mon) }

        // De on dang mo (de tuan, de on truoc kiem tra), roi de on xong hom nay kem diem.
        k.deOn.forEach { de ->
            themDong(
                box,
                hinh = R.drawable.st_ic_giai_de,
                mau = mauMon,
                ten = GiaiDe.tenDe(de),
                phu = "${de.ten} · ${de.cauIds.size} câu · khoảng ${de.phutGoiY} phút" + GiaiDe.ngayKiemTra(de)
            ) { GiaiDeActivity.mo(this, de.id) }
        }
        k.deOnXong.forEach { (de, diem) ->
            // De lam tren may cham bang sao, khong phai so cau dung (xem GiaiDe.diem).
            themDong(
                box,
                hinh = R.drawable.st_ic_giai_de,
                mau = R.color.ok,
                ten = if (de.trenMay) "${GiaiDe.tenDe(de)}: ${diem.first}/${diem.second} ★"
                else "${GiaiDe.tenDe(de)}: đúng ${diem.first}/${diem.second} câu",
                xong = true
            ) { GiaiDeActivity.mo(this, de.id) }
        }

        // De thi thu: bon dong theo ky.
        GiaiDe.CAC_KY.forEach { (ky, tenKy) ->
            val cua = k.deThi.filter { GiaiDe.kyCua(it.de) == ky }
            val tenDong = "Đề thi thử $tenKy môn $ten"
            if (cua.isEmpty()) {
                themDong(box, hinh = R.drawable.st_ic_giai_de, mau = R.color.ink_soft, ten = tenDong, phu = "Chưa có đề", nhat = true)
                return@forEach
            }
            // De dang lam truoc de moi mo: con thay ngay de minh dang lam do.
            val dang = cua.firstOrNull { it.trangThai == GiaiDe.TT_DANG } ?: cua.firstOrNull { it.dangMo != null }
            val san = cua.count { it.trangThai == GiaiDe.TT_SAN }
            val daLam = cua.count { it.lanCuoi != null }
            val phu = when {
                dang != null -> (if (dang.trangThai == GiaiDe.TT_DANG) "Đang làm" else "Đang mở") +
                    " · ${dang.dangMo?.cauIds?.size ?: dang.de.cauIds.size} câu" +
                    " · khoảng ${dang.dangMo?.phutGoiY ?: dang.de.phut} phút" +
                    if (san > 0) " · thêm $san đề mở được" else ""
                san > 0 -> "$san đề mở được"
                daLam == cua.size -> "Đã làm hết, bấm để làm lại"
                else -> moTaDeHepNhat(cua)
            }
            themDong(
                box,
                hinh = R.drawable.st_ic_giai_de,
                mau = mauMon,
                ten = dang?.let { "Đề thi thử số ${GiaiDe.soDe(it.de)} $tenKy môn $ten" } ?: tenDong,
                phu = phu,
                so = "$daLam/${cua.size} đề"
            ) { hoiDeThi(tenDong, cua) }
        }
    }

    /**
     * Ten cac quyen SGK cho dong phu, bo phan dau trung nhau: "SGK Toán 8 tập một, tập hai". Giu
     * lai chu cuoi cua phan trung ("tập"), cat o dau cach dung truoc no, de khong cat giua mot chu
     * va khong ra "tập một, hai".
     */
    private fun gonTenSach(cac: List<String>): String {
        if (cac.size < 2) return cac.joinToString(", ")
        val dau = cac.first()
        val sau = cac.drop(1).map { ten ->
            val chung = dau.commonPrefixWith(ten).trimEnd()
            val cat = chung.lastIndexOf(' ') + 1
            if (cat > 0) ten.substring(cat) else ten
        }
        return (listOf(dau) + sau).joinToString(", ")
    }

    /**
     * Cac de cua ky deu con khoa: noi de can it bai them nhat se mo khi nao, "Mở khi đã học
     * thêm Bài 3, 10–12". Mon chua chon bai da hoc thi bao chon truoc.
     */
    private fun moTaDeHepNhat(cua: List<GiaiDe.TinhTrangDeThi>): String {
        val t = cua.filter { it.trangThai == GiaiDe.TT_KHOA }
            .minByOrNull { tt -> tt.thieu.sumOf { it.cacSo.size } }
            ?.thieu?.firstOrNull() ?: return ""
        if (t.chuaChon) return "Chọn bài lớp đã học trước"
        return "Mở khi đã học thêm ${if (t.laUnit) "Unit" else "Bài"} ${PhanHoc.gon(t.cacSo)}"
    }

    /**
     * Danh sach de thi cua mot ky: moi de mot dong kem tinh trang. Bam de dang mo thi vao lam,
     * de mo duoc thi mo, de da lam thi xem lai hay lam lai, de chua du pham vi thi noi con
     * thieu bai nao.
     */
    private fun hoiDeThi(tieuDe: String, cac: List<GiaiDe.TinhTrangDeThi>) {
        val dong = cac.map { t ->
            val sao = t.lanCuoi?.let { "${it.saoDat.coerceAtLeast(0)}/${it.saoToiDa} ★" }.orEmpty()
            t.de.ten + " · " + when (t.trangThai) {
                GiaiDe.TT_MO -> "đang mở"
                GiaiDe.TT_DANG -> "đang làm"
                GiaiDe.TT_XONG -> "đã làm $sao"
                GiaiDe.TT_SAN -> "chưa làm"
                else -> GiaiDe.moTaThieu(t.thieu)
            }
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(tieuDe)
            .setItems(dong) { _, i -> chonDeThi(cac[i]) }
            .setNegativeButton("Đóng", null)
            .show()
    }

    /**
     * Cau noi khi con bam mot de chua du pham vi: de mo khi da hoc nhung bai nao, con thieu bai
     * nao ([GiaiDe.cauDeKhoa]), va cho doi la dong Bai da hoc o dau khu cua mon.
     */
    private fun loiDeKhoa(t: GiaiDe.TinhTrangDeThi): String =
        GiaiDe.cauDeKhoa(t.de, t.thieu) +
            " Lớp học tới đó rồi thì đánh dấu thêm ở dòng Bài đã học, khu ${GiaiDe.tenMon(t.de.mon)}."

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

    /**
     * Mot dong, cung khuon voi dong o khoi Viec hom nay cua man chinh, them so "đã làm / tổng"
     * o cuoi ([so]). [nhat] la dong chi de nhin (ky chua co de): chu xam, khong bam duoc.
     */
    private fun themDong(
        box: LinearLayout,
        hinh: Int,
        mau: Int,
        ten: String,
        phu: String = "",
        so: String = "",
        xong: Boolean = false,
        nhat: Boolean = false,
        bam: (() -> Unit)? = null
    ) {
        val v = StDongViecBinding.inflate(layoutInflater, box, false)
        v.hinh.setImageResource(hinh)
        v.hinh.imageTintList = ContextCompat.getColorStateList(this, mau)
        v.hinh.backgroundTintList = ContextCompat.getColorStateList(
            this, if (xong) R.color.ok_soft else R.color.canvas
        )
        v.ten.text = ten
        if (nhat) v.ten.setTextColor(ContextCompat.getColor(this, R.color.ink_soft))
        v.phu.text = phu
        v.phu.visibility = if (phu.isBlank()) View.GONE else View.VISIBLE
        v.so.text = so
        v.so.visibility = if (so.isBlank()) View.GONE else View.VISIBLE
        v.duoi.text = when {
            xong -> "✓"
            nhat -> ""
            else -> "›"
        }
        v.duoi.setTextColor(ContextCompat.getColor(this, if (xong) R.color.ok else R.color.ink_soft))
        if (bam != null && !nhat) v.root.setOnClickListener { bam() }
        else v.root.isClickable = false
        box.addView(v.root)
    }

    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        /** Ba khu theo thu tu tren trang, khop voi ba hop trong layout. */
        private val KHU = listOf(PhanHoc.TOAN, PhanHoc.KHTN, PhanHoc.TIENG_ANH)

        fun mo(activity: Activity) {
            activity.startActivity(Intent(activity, LuyenTapActivity::class.java))
        }
    }
}
