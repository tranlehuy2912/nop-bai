package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
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
import vn.huytl.homeworkgate.databinding.StNutMonBinding
import vn.huytl.homeworkgate.databinding.StOLuyenBinding
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
 * KHU NGU VAN (Ba Huy chot 12/10/2026): khu thu tu, cung khuon: Bai da hoc (Bai 1 toi Bai 10),
 * On tap, Lam bai tap trong SGK, mot dong Luyen tap, Cau da lam dung. Van chua co bo the, bo de
 * nen khong co dong Kiem tra va De thi thu.
 *
 * LAM BAI TAP TRONG SGK (cung ngay, Ba Huy chot sau): dong ngay duoi Kiem tra, tren Luyen tap
 * tung phan, mo [BaiSgkActivity]. Bai SGK lam tren may thay cho lam vo roi chup; mon chua co SGK
 * trong may thi khong co dong nay (Tieng Anh truoc khi nap SGK ngay 2/10/2026).
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
 *
 * PHUONG AN B CUA MAU H (anh Huy chon 16/11/2026). Bon khu doc mot trang (khoang 38 dong) doi
 * thanh hang nut mon o tren: chi ve viec cua mon dang chon, chia ba nhom "Hôm nay" (On tap,
 * Kiem tra, de on), "Luyện tập" (SGK, tung phan, Cau da lam dung), "Đề thi thử" (bon ky), moi
 * viec mot o mau cua mon, ba cot (de thi bon cot). Dong "Bài đã học" dau moi khu thanh nut nho
 * duoi hang mon. Thu tu va luat cua tung viec giu nguyen nhu cac doan tren; chi cach ve doi.
 */
class LuyenTapActivity : AppCompatActivity() {

    private lateinit var b: StActivityLuyenTapBinding

    /** Cac khu doc o lan [nap] cuoi. Doi mon chi ve lai tu day, khong doc kho lai. */
    private var cacKhu: List<Khu> = emptyList()

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
        savedInstanceState?.getString(KHOA_MON)?.takeIf { it in KHU }?.let { monDangChon = it }
        b.nutDaHoc.setOnClickListener { ChonHocToi.hoiMon(this, monDangChon) { nap() } }
        veHangMon()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KHOA_MON, monDangChon)
    }

    /** Hang nut mon: mon dang chon dam mau, mon khac nhat di. Bam la ve lai viec cua mon do. */
    private fun veHangMon() {
        b.hangMon.removeAllViews()
        KHU.forEachIndexed { i, mon ->
            val nut = StNutMonBinding.inflate(layoutInflater, b.hangMon, false).root
            nut.text = GiaiDe.tenMon(mon)
            nut.background = HinhBong.nut(this, ContextCompat.getColor(this, MatMon.mau(mon)))
            nut.alpha = if (mon == monDangChon) 1f else 0.45f
            if (i == KHU.lastIndex) (nut.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
            nut.setOnClickListener {
                if (monDangChon == mon) return@setOnClickListener
                monDangChon = mon
                veHangMon()
                veMon()
            }
            b.hangMon.addView(nut)
        }
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
        /** Tieng Anh: het tu den luot trong cac Unit da hoc, khac voi du phut. */
        val hetTu: Boolean,
        val luyen: List<Luyen>,
        /** Dong "Làm bài tập trong SGK"; null khi mon chua co SGK trong may. */
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
            cacKhu = cac
            veMon()
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
        var hetTu = false
        if (mon == PhanHoc.TIENG_ANH) {
            val tu = runCatching { kho.tinhTrangTu(PhanHoc.BO_TIENG_ANH, LuatTuVung.LAN_DUNG_DE_TINH) }
                .getOrDefault(emptyList())
            kiemTra = if (tu.isEmpty()) null
            else tu.count { it.tinh == LuatTuVung.TinhTrang.DA_THUOC } to tu.size
            // Tu 11/10/2026 tu da thuoc chi ra khi den hen, nen co ngay khong con tu nao de hoi:
            // dong o dang xong nhu het the. Chua chon Unit thi chua xong, vao la duoc hoi chon.
            val daHocAnh = PhanHoc.baiDaHoc(ct, mon)
            hetTu = tu.isNotEmpty() && !daHocAnh.isNullOrEmpty() &&
                tu.none { it.denLuot && LuatTuVung.daHoc(it.tu.unit, daHocAnh) }
            kiemTraXong = hetTu || runCatching {
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
            // Van chi dem cau lam tren may, nhu man bai SGK ([BaiSgkActivity.tienBo]).
            val tienBo = quyen.map { BaiSgkActivity.tienBo(ct, mon, it) }
            if (tienBo.sumOf { it.second } == 0) return@runCatching null
            Sgk(
                tenSach = gonTenSach(quyen.map { it.ten }),
                daLam = tienBo.sumOf { it.first },
                tong = tienBo.sumOf { it.second },
                dangLam = GiaiDe.baiSgkDangLam(ct, mon, bayGio = bayGio).size
            )
        }.getOrNull()
        val dem = runCatching { LamTrenMay.demTheoPhan(ct, mon) }.getOrDefault(emptyMap())
        // Tieng Anh, Ngu van khong chia phan: mot dong Luyen tap cho ca mon.
        val luyen = if (PhanHoc.cuaMon(mon).isEmpty()) listOf(Luyen(null, dem[""]?.first ?: 0, dem[""]?.second ?: 0))
        else PhanHoc.cuaMon(mon).map { p -> Luyen(p, dem[p.ma]?.first ?: 0, dem[p.ma]?.second ?: 0) }
        return Khu(
            mon = mon,
            daHoc = PhanHoc.baiDaHoc(ct, mon),
            daOn = LamTrenMay.soDaOnHomNay(ct, mon, bayGio),
            conOn = LamTrenMay.soCauOn(ct, bayGio, mon),
            kiemTra = kiemTra,
            kiemTraXong = kiemTraXong,
            hetTu = hetTu,
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

    /** Mot o o trang: hinh, ten, dong phu, tien do [da]/[tong] (tong 0 thi khong co thanh). */
    private class O(
        val hinh: Int,
        val ten: String,
        val phu: String = "",
        val da: Int = 0,
        val tong: Int = 0,
        /** Chu thay cho so "da/tong" khi khong co thanh, vi du "254 câu". */
        val so: String = "",
        val xong: Boolean = false,
        /** O chi de nhin (ky chua co de): nhat di, khong bam duoc. */
        val nhat: Boolean = false,
        val bam: (() -> Unit)? = null
    )

    private fun veMon() {
        val k = cacKhu.firstOrNull { it.mon == monDangChon } ?: return
        val mon = k.mon
        val ten = GiaiDe.tenMon(mon)

        // Nut Bai da hoc cua mon (truoc 16/11/2026 la dong dau moi khu). Chua chon thi chu do.
        b.nutDaHoc.text = k.daHoc?.let { HocToi.moTa(mon, it).replaceFirstChar { c -> c.uppercase() } }
            ?: "Chưa chọn"
        b.nutDaHoc.setTextColor(ContextCompat.getColor(this, if (k.daHoc == null) R.color.alert else R.color.ink))
        b.nutDaHoc.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.st_ic_but, 0)

        b.boxMon.removeAllViews()

        // ---- Hom nay: On tap, Kiem tra, de on dang mo hay xong hom nay.
        val homNay = mutableListOf<O>()
        // On tap van hien khi khong co cau den hen, dang da xong: an di thi con khong biet duong
        // on nam o day.
        val denHen = k.daOn + k.conOn
        homNay += O(
            hinh = R.drawable.st_ic_on_lai,
            ten = "Ôn tập $ten",
            phu = when {
                denHen == 0 -> "Hôm nay chưa có câu đến hẹn"
                k.conOn == 0 -> "Hôm nay ôn xong rồi"
                else -> "${k.conOn} câu đến hẹn"
            },
            da = k.daOn,
            tong = denHen,
            xong = k.conOn == 0
        ) { if (k.conOn > 0) LamBaiActivity.moOn(this, mon) }
        k.kiemTra?.let { (da, tong) ->
            val laAnh = mon == PhanHoc.TIENG_ANH
            homNay += O(
                hinh = R.drawable.st_ic_the_hoc,
                ten = if (laAnh) "Kiểm tra từ vựng" else "Kiểm tra công thức",
                phu = when {
                    k.daHoc == null -> "Chọn bài lớp đã học trước"
                    !k.kiemTraXong -> ""
                    laAnh -> if (k.hetTu) "Hôm nay hết từ" else "Hôm nay đủ rồi"
                    else -> "Hôm nay hết câu"
                },
                da = da,
                tong = tong,
                xong = k.kiemTraXong
            ) {
                if (laAnh) startActivity(Intent(this, DoTuVungActivity::class.java))
                else HocThuocActivity.mo(this, mon)
            }
        }
        k.deOn.forEach { de ->
            homNay += O(
                hinh = R.drawable.st_ic_giai_de,
                ten = GiaiDe.tenDe(de),
                phu = "${de.cauIds.size} câu · khoảng ${de.phutGoiY} phút" + GiaiDe.ngayKiemTra(de)
            ) { GiaiDeActivity.mo(this, de.id) }
        }
        k.deOnXong.forEach { (de, diem) ->
            // De lam tren may cham bang sao, khong phai so cau dung (xem GiaiDe.diem).
            homNay += O(
                hinh = R.drawable.st_ic_giai_de,
                ten = GiaiDe.tenDe(de),
                phu = if (de.trenMay) "${diem.first}/${diem.second} ★" else "đúng ${diem.first}/${diem.second} câu",
                xong = true
            ) { GiaiDeActivity.mo(this, de.id) }
        }
        nhom("Hôm nay", homNay, 3)

        // ---- Luyen tap: SGK, tung phan, Cau da lam dung.
        val luyen = mutableListOf<O>()
        k.sgk?.let { sg ->
            luyen += O(
                hinh = R.drawable.st_ic_the_hoc,
                ten = "Làm bài tập trong SGK",
                phu = if (sg.dangLam > 0) "Đang làm ${sg.dangLam} bài" else sg.tenSach,
                da = sg.daLam,
                tong = sg.tong
            ) { BaiSgkActivity.mo(this, mon) }
        }
        // Phan chua co bai nao danh dau thi bam la hoi danh dau truoc: vao man lam bai luc do chi
        // gap cau "het cau".
        k.luyen.forEach { l ->
            val coBai = l.phan?.let { p -> k.daHoc?.any { p.chua(it) } } ?: !k.daHoc.isNullOrEmpty()
            luyen += O(
                hinh = R.drawable.st_ic_but,
                ten = "Luyện tập " + (l.phan?.tenDai ?: ten),
                phu = when {
                    k.daHoc == null -> "Chọn bài lớp đã học trước"
                    !coBai -> "Chưa đánh dấu bài nào của phần này"
                    else -> ""
                },
                da = l.daLam,
                tong = l.tong
            ) {
                if (coBai) LamBaiActivity.moLamThem(this, mon, l.phan)
                else ChonHocToi.hoiMon(this, mon) { nap() }
            }
        }
        luyen += O(
            hinh = R.drawable.st_ic_da_cham,
            ten = "Câu đã làm đúng",
            phu = if (k.soDung == 0) "Chưa có câu nào" else "Xem lại đề và lời giải",
            so = "${k.soDung} câu"
        ) { CauDaLamActivity.mo(this, mon) }
        nhom("Luyện tập", luyen, 3)

        // ---- De thi thu: bon o theo ky. Mon chua co bo de nao (Ngu van) thi khong co nhom nay.
        if (NganHang.boDeThi().none { it.mon == mon }) return
        val deThi = GiaiDe.CAC_KY.map { (ky, tenKy) ->
            val cua = k.deThi.filter { GiaiDe.kyCua(it.de) == ky }
            val tenDong = "Đề thi thử $tenKy môn $ten"
            val tenO = tenKy.replaceFirstChar { it.uppercase() }
            if (cua.isEmpty()) {
                return@map O(hinh = R.drawable.st_ic_giai_de, ten = tenO, phu = "Chưa có đề", nhat = true)
            }
            // De dang lam truoc de moi mo: con thay ngay de minh dang lam do.
            val dang = cua.firstOrNull { it.trangThai == GiaiDe.TT_DANG } ?: cua.firstOrNull { it.dangMo != null }
            val san = cua.count { it.trangThai == GiaiDe.TT_SAN }
            val daLam = cua.count { it.lanCuoi != null }
            val phu = when {
                dang != null -> {
                    // De cuong on (11/10/2026, ma "TCK1-DC1") khong co so de: goi bang ten.
                    val so = GiaiDe.soDe(dang.de)
                    (if (dang.trangThai == GiaiDe.TT_DANG) "Đang làm " else "Đang mở ") +
                        if (so.all(Char::isDigit)) "đề số $so" else dang.de.ten
                }
                san > 0 -> "$san đề mở được"
                daLam == cua.size -> "Đã làm hết, bấm để làm lại"
                else -> moTaDeHepNhat(cua)
            }
            O(
                hinh = R.drawable.st_ic_giai_de,
                ten = tenO,
                phu = phu,
                da = daLam,
                tong = cua.size
            ) { hoiDeThi(tenDong, cua) }
        }
        nhom("Đề thi thử", deThi, 4)
    }

    /** Mot nhom: nhan chu Coiny roi cac o, [cot] o mot hang. Hang thieu thi o khong gian ra. */
    private fun nhom(tenNhom: String, cac: List<O>, cot: Int) {
        if (cac.isEmpty()) return
        val nhan = TextView(this).apply {
            text = tenNhom
            typeface = ResourcesCompat.getFont(this@LuyenTapActivity, R.font.coiny)
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@LuyenTapActivity, R.color.ink))
            setPadding(dp(4), dp(18), 0, 0)
        }
        b.boxMon.addView(nhan)
        val mauMon = ContextCompat.getColor(this, MatMon.mau(monDangChon))
        cac.chunked(cot).forEach { hang ->
            val dong = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = cot.toFloat()
                clipChildren = false
            }
            hang.forEach { o -> dong.addView(taoO(dong, o, mauMon)) }
            b.boxMon.addView(dong, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun taoO(dong: LinearLayout, o: O, mau: Int): View {
        val v = StOLuyenBinding.inflate(layoutInflater, dong, false)
        v.than.background = HinhBong.o(this, mau)
        v.hinh.setImageResource(o.hinh)
        v.ten.text = o.ten
        v.phu.text = o.phu
        v.phu.visibility = if (o.phu.isBlank()) View.GONE else View.VISIBLE
        when {
            o.tong > 0 -> {
                v.thanh.visibility = View.VISIBLE
                v.thanh.weightSum = o.tong.toFloat()
                (v.phanLam.layoutParams as LinearLayout.LayoutParams).weight = o.da.coerceIn(0, o.tong).toFloat()
                v.so.text = "${o.da}/${o.tong}"
            }
            o.so.isNotBlank() -> {
                v.thanh.visibility = View.INVISIBLE
                v.so.text = o.so
            }
            else -> v.khungTienDo.visibility = View.GONE
        }
        v.dauXong.visibility = if (o.xong) View.VISIBLE else View.GONE
        if (o.nhat) v.root.alpha = 0.45f
        val bam = o.bam
        if (bam != null && !o.nhat) v.than.setOnClickListener { bam() } else v.than.isClickable = false
        return v.root
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

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
            " Lớp học tới đó rồi thì đánh dấu thêm ở nút Bài đã học của môn ${GiaiDe.tenMon(t.de.mon)}."

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

    private fun moc0Gio(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        /** Cac mon theo thu tu tren hang nut. Ngu van tu 12/10/2026. */
        private val KHU = listOf(PhanHoc.TOAN, PhanHoc.KHTN, PhanHoc.TIENG_ANH, PhanHoc.NGU_VAN)

        private const val KHOA_MON = "mon"

        /**
         * Mon dang chon, giu trong tien trinh: ra trang khac roi quay lai (hay xoay man) van
         * dung mon vua xem. Mo app lai tu dau thi ve Toan.
         */
        private var monDangChon: String = PhanHoc.TOAN

        fun mo(activity: Activity) {
            activity.startActivity(Intent(activity, LuyenTapActivity::class.java))
        }
    }
}
