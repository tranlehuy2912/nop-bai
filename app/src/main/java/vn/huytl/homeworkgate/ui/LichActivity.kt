package vn.huytl.homeworkgate.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.graphics.ColorUtils
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.Buoi
import vn.huytl.homeworkgate.data.BuoiHoc
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import vn.huytl.homeworkgate.data.TinhLoiNhac
import vn.huytl.homeworkgate.databinding.StActivityLichBinding
import java.util.Calendar

/**
 * Thoi khoa bieu ca tuan, cho Le Hoa tu tra.
 *
 * Bay theo luoi chu khong theo danh sach doc: cot la thu, hang la tiet. Danh sach
 * doc thi mot tuan dai bon man hinh, phai cuon moi thay thu sau hoc gi, ma cau hoi
 * thuong gap lai la "so sanh hai ngay" - kieu do doc khong duoc. Luoi thi ca tuan
 * nam gon mot trang va liec ngang mot hang la thay ca sau ngay.
 *
 * Chi de xem, khong sua duoc. Doi lich thi sua [ThoiKhoaBieu] roi cai lai app: nhu
 * vay khong co o nhap nao de nhap sai, va go app cung khong mat gi.
 */
class LichActivity : AppCompatActivity() {

    private lateinit var binding: StActivityLichBinding

    /** Mot hang cua luoi: hoac mot tiet trong buoi, hoac mot dai phan cach buoi. */
    private sealed interface Hang {
        data class Bang(val ten: String) : Hang
        data class Tiet(val buoi: Buoi, val tiet: Int) : Hang
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StActivityLichBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }

        binding.nutQuayLai.setOnClickListener { finish() }
        ve()
    }

    private fun ve() {
        val tuan = tuanNay()
        val ke = TinhLoiNhac.buoiKeTiep(Calendar.getInstance())
        val maKeTiep = ke?.let { TinhLoiNhac.maBuoi(it.first, it.second) }
        val homNay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        binding.phuDeLich.text =
            "Lớp 8A15 · tuần ${ngayThang(tuan.first())} – ${ngayThang(tuan.last())}"

        val hang = dungHang(tuan)
        veCotLe(hang)

        binding.luoi.removeAllViews()
        tuan.forEach { ngay ->
            binding.luoi.addView(veCot(ngay, hang, homNay, maKeTiep))
        }
    }

    /**
     * Danh sach hang cua luoi, dung tu chinh thoi khoa bieu cua tuan nay.
     *
     * Ke DU cac tiet cua buoi, ke ca tiet tuan nay khong ai hoc. Truoc day chi ke
     * tiet co mon, nen buoi sang chi ra bon hang: thu hai hoc tiet 3 va 4 thi hai o
     * do roi xuong day bang, va liec qua tuong la hai tiet cuoi buoi sang - trong
     * khi buoi sang co nam tiet, va con phai biet minh ve luc may gio.
     *
     * Van bo han mot buoi khi ca tuan khong hoc buoi do: mot buoi trong tron thi
     * khong co gi de doc, ma van chiem nua chieu cao bang.
     */
    private fun dungHang(tuan: List<Calendar>): List<Hang> {
        val cacBuoi = tuan.flatMap { ThoiKhoaBieu.buoiHocCua(it.get(Calendar.DAY_OF_WEEK)) }
        return buildList {
            Buoi.entries.forEach { buoi ->
                if (cacBuoi.none { it.buoi == buoi && it.monTheoTiet.isNotEmpty() }) return@forEach
                add(Hang.Bang(if (buoi == Buoi.SANG) "SÁNG" else "CHIỀU"))
                ThoiKhoaBieu.cacTiet(buoi).forEach { add(Hang.Tiet(buoi, it)) }
            }
        }
    }

    /** Cot le trai: ten dai buoi, va so tiet kem gio. */
    private fun veCotLe(hang: List<Hang>) {
        binding.cotLe.removeAllViews()
        // Chua cho dau cot cua cac ngay, de hang dau tien cua cot le thang voi hang
        // dau tien cua cac cot ben phai.
        binding.cotLe.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.st_cao_dau_cot)
            )
            background = ke(veTrai = false)
        })

        hang.forEachIndexed { i, h ->
            val cuoi = i == hang.lastIndex
            val o = taoO(binding.cotLe)
            when (h) {
                is Hang.Bang -> {
                    o.text = h.ten
                    o.setTextColor(mau(R.color.brand_dark))
                    o.textSize = 12f
                    o.letterSpacing = 0.08f
                    o.setTypeface(null, Typeface.BOLD)
                    o.gravity = Gravity.BOTTOM
                    o.setPadding(o.paddingLeft, 0, o.paddingRight, 8)
                    o.background = ke(veTrai = false, veDuoi = !cuoi)
                }
                is Hang.Tiet -> {
                    o.text = "tiết ${h.tiet}\n${TinhLoiNhac.gioPhut(ThoiKhoaBieu.gioTiet(h.buoi, h.tiet))}"
                    o.setTextColor(mau(R.color.ink_soft))
                    o.textSize = 12f
                    o.background = ke(veTrai = false, veDuoi = !cuoi)
                }
            }
            binding.cotLe.addView(o)
        }
    }

    private fun veCot(
        ngay: Calendar,
        hang: List<Hang>,
        homNay: Int,
        maKeTiep: String?
    ): View {
        val thu = ngay.get(Calendar.DAY_OF_WEEK)
        val cot = LayoutInflater.from(this)
            .inflate(R.layout.st_cot_lich, binding.luoi, false)

        cot.findViewById<View>(R.id.dau_cot).background = ke(veTrai = true)

        val laHomNay = thu == homNay
        cot.findViewById<TextView>(R.id.nhan_thu).apply {
            text = ThoiKhoaBieu.tenThu(thu).uppercase()
            setTextColor(mau(if (laHomNay) R.color.ok else R.color.brand_dark))
        }
        cot.findViewById<TextView>(R.id.nhan_ngay).text =
            if (laHomNay) "${ngayThang(ngay)} · hôm nay" else ngayThang(ngay)

        val khung = cot.findViewById<LinearLayout>(R.id.danh_sach_o)
        val nghi = NgayNghi.tenKyNghi(ngay)
        // Bo chu "nghi" co san trong ten ky nghi truoc khi ghep, khong thi ra "Nghi
        // nghi Tet Nguyen dan". Ten ky nghi moi cai mot kieu: co cai da mang chu
        // nghi ("nghi Tet Nguyen dan", "nghi he"), co cai khong ("Tet Duong lich").
        val cacBuoi = if (nghi == null) ThoiKhoaBieu.buoiHocCua(thu) else emptyList()

        hang.forEachIndexed { i, h ->
            val cuoi = i == hang.lastIndex
            val o = taoO(khung)
            when (h) {
                // Dai "SANG"/"CHIEU": ke net ngang duoi nhan de no thanh mot dong
                // tieu de that su, nhung KHONG ke net doc - net doc dut mot doan o
                // day chinh la cho tach hai buoi ra.
                is Hang.Bang -> {
                    o.text = ""
                    o.background = ke(veTrai = false, veDuoi = !cuoi)
                }
                is Hang.Tiet -> {
                    o.background = ke(veTrai = true, veDuoi = !cuoi)
                    val buoi = cacBuoi.firstOrNull { it.buoi == h.buoi }
                    val mon = buoi?.monTheoTiet?.get(h.tiet)
                    if (mon == null) {
                        // Ngay nghi thi ghi ten ky nghi vao dung hang dau tien, cac
                        // hang con lai de trong: mot cot toan chu "nghi" lap lai sau
                        // lan thi thanh nhieu, ma van khong noi them duoc gi.
                        if (nghi != null && i == hang.indexOfFirst { it is Hang.Tiet }) {
                            o.text = "Nghỉ ${nghi.removePrefix("nghỉ ")}"
                            o.setTextColor(mau(R.color.ink_soft))
                        } else {
                            // De trong han. Truoc day co mot dau cham mo danh dau o
                            // rong, nhung tu luc ke net dut thi o rong da tu thay
                            // duoc, cai cham chi la mot vet ban.
                            o.text = ""
                        }
                    } else {
                        // Mau nam o CHU, khong o nen, va chi cho bon mon chinh.
                        //
                        // Da thu hai muc qua tay: to nen tung o thi nam muoi tu o
                        // thanh mot tam vai vun; to chu cho ca bay mon thi doc mot
                        // ten phai luot qua bay mau, ma phan lon so do la mon moi
                        // tuan mot tiet - khong phai thu ai di tim trong bang.
                        //
                        // Rieng buoi ke tiep moi to nen: ca bang chi co mot cho nhu
                        // vay, va do la cho duy nhat can nhin ra truoc tien.
                        val chinh = MatMon.laMonChinh(mon)
                        val keTiep = laKeTiep(ngay, buoi, maKeTiep)
                        o.text = mon
                        o.setTextColor(
                            mau(
                                when {
                                    chinh -> MatMon.mau(mon)
                                    keTiep -> R.color.brand_dark
                                    else -> R.color.ink_soft
                                }
                            )
                        )
                        // Nen cua o ke tiep lay mau chu dao chu khong lay mau mon:
                        // mon phu khong co mau rieng, nen neu lay theo mon thi buoi
                        // ke tiep hien ra mot mang xam - nhin nhu o bi khoa chu
                        // khong nhu cho dang phai chu y.
                        o.background = ke(
                            veTrai = true,
                            veDuoi = !cuoi,
                            nen = if (keTiep) {
                                ColorUtils.setAlphaComponent(mau(R.color.brand), 34)
                            } else {
                                android.graphics.Color.TRANSPARENT
                            }
                        )
                        if (keTiep) o.setTypeface(null, Typeface.BOLD)
                    }
                }
            }
            khung.addView(o)
        }
        return cot
    }

    private fun laKeTiep(ngay: Calendar, buoi: BuoiHoc, maKeTiep: String?): Boolean =
        maKeTiep != null && TinhLoiNhac.maBuoi(ngay, buoi) == maKeTiep

    private fun taoO(cha: ViewGroup): TextView =
        LayoutInflater.from(this).inflate(R.layout.st_o_lich, cha, false) as TextView

    private fun mau(id: Int) = ContextCompat.getColor(this, id)

    /**
     * Mot o co ke net dut. [nen] khac trong suot la o cua buoi ke tiep.
     *
     * [veDuoi] tat o hang cuoi cung: net do se nam sat mep the, thanh ra hai duong
     * song song cach nhau mot ly - nhin nhu ve hut chu khong phai ve co y.
     */
    private fun ke(
        veTrai: Boolean,
        veDuoi: Boolean = true,
        nen: Int = android.graphics.Color.TRANSPARENT
    ) = KeDut(
        mau = mau(R.color.st_ke_luoi),
        doDay = resources.displayMetrics.density,
        veDuoi = veDuoi,
        veTrai = veTrai,
        nen = nen
    )

    /**
     * Thu hai den thu bay cua tuan dang xet.
     *
     * Chu nhat thi lay tuan toi: tuan nay hoc xong roi, ma cai Le Hoa can biet luc
     * do la mai thu hai hoc gi.
     */
    private fun tuanNay(): List<Calendar> {
        val now = Calendar.getInstance()
        val lui = when (val thu = now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> -1
            else -> thu - Calendar.MONDAY
        }
        val thuHai = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -lui) }
        return (0..5).map {
            (thuHai.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, it) }
        }
    }

    private fun ngayThang(cal: Calendar): String =
        "%02d/%02d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
}
