package vn.huytl.homeworkgate.ui

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.databinding.ActivityXemPhimBinding
import vn.huytl.homeworkgate.dongbo.Laptop

/**
 * Doi phut choi sang phut xem phim tren laptop, mo tu nut "Xem phim" o man chinh.
 *
 * Anh Huy chot ngay 9/10/2026, thay cho chuoi hop thoai DoiNetflix (7/10/2026) ma anh thay
 * xau: mot man rieng, tren cung hai con so, giua la sau nut 15' 30' 45' 60' Đổi hết Khác.
 *
 * Hai buoc: bam nut de CHON (nut doi mau, hien dong xem truoc "laptop se con bao nhieu"),
 * roi bam nut xanh la "Đổi N phút chơi". Anh Huy chon cach nay thay cho bam la doi luon, vi
 * doi roi khong lay lai duoc: con bam nham 60' la mat ca tieng choi. Hai buoc nay cung thay
 * cho hop hoi lai cua ban cu.
 *
 * Nut nao cung bam duoc, ke ca khi lon hon so phut dang co ("cho mo nut", anh Huy chot):
 * luc do dong xem truoc bao con chi co bao nhieu phut va khong co nut Doi. Hop cu thi an
 * cac nut do. Nut "Khác" mo hop go so, go qua so dang co thi hop bao ngay tai cho.
 *
 * Doi xong khong hien thong bao nho nua ma hien mot dai ket qua tren man: "Đang chờ laptop
 * nhận", roi "Laptop đã nhận" khi phieu vua ghi bien mat. Laptop cong gio xong thi xoa phieu
 * (xem [Laptop]), nen phieu mat la dau chac chan nhat rang laptop da nhan. Truoc day dong
 * thong bao noi "khoảng một phút nữa laptop nhận được", tu luc laptop con hoi moi phut.
 */
class XemPhimActivity : AppCompatActivity() {

    private lateinit var binding: ActivityXemPhimBinding
    private lateinit var gate: GateStore
    private val tay = Handler(Looper.getMainLooper())

    /** So phut con vua chon bang nut 15' toi 60' hay hop "Khác". null la chua chon gi. */
    private var chon: Int? = null

    /** Con chon nut "Đổi hết": so phut di theo so dang co, va doi xong thi cat luon phien. */
    private var chonHet = false

    /** Dai ket qua lan doi vua roi. null la chua doi lan nao tu luc mo man. */
    private var ketQua: Pair<String, Boolean>? = null

    /** Nghe phieu vua ghi de biet laptop da nhan chua. Xem [ngheNhan]. */
    private var ngheNhan: ListenerRegistration? = null

    private lateinit var cacNut: List<Pair<MaterialButton, Int>>

    /**
     * Ve lai moi giay trong luc man dang mo. Dang choi thi phut choi con giam theo dong ho,
     * va laptop dang co nguoi xem thi phut laptop cung giam: so tren man phai di theo, khong
     * thi con chon theo mot con so da cu.
     */
    private val nhip = object : Runnable {
        override fun run() {
            ve()
            tay.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityXemPhimBinding.inflate(layoutInflater)
        setContentView(binding.root)
        gate = GateStore(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.goc) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top + 28.dp(), bottom = bars.bottom + 28.dp())
            insets
        }
        binding.btnDone.setOnClickListener { finish() }

        val nut = listOf(binding.nut15, binding.nut30, binding.nut45, binding.nut60)
        cacNut = nut.zip(Laptop.CAC_NUT)
        cacNut.forEach { (n, phut) ->
            n.text = "$phut'"
            n.setOnClickListener { chonSo(phut) }
        }
        binding.nutHet.setOnClickListener {
            chon = null
            chonHet = true
            ketQua = null
            ve()
        }
        binding.nutKhac.setOnClickListener { hoiSoKhac() }
        binding.btnDoi.setOnClickListener { doi() }
    }

    override fun onResume() {
        super.onResume()
        tay.removeCallbacks(nhip)
        nhip.run()
    }

    override fun onPause() {
        super.onPause()
        tay.removeCallbacks(nhip)
    }

    override fun onDestroy() {
        ngheNhan?.remove()
        ngheNhan = null
        super.onDestroy()
    }

    private fun chonSo(phut: Int) {
        chon = phut
        chonHet = false
        ketQua = null
        ve()
    }

    private fun ve() {
        val co = gate.phutDoiDuoc()
        val tiLe = Prefs.get(this).tiLeNetflix
        val laptop = (Laptop.conLaiMs(this) / 60_000L).toInt()
        binding.txtCo.text = co.toString()
        binding.txtLaptop.text = laptop.toString()

        val kq = ketQua
        binding.txtKetQua.visibility = if (kq == null) View.GONE else View.VISIBLE
        if (kq != null) {
            val (chu, tot) = kq
            binding.txtKetQua.text = chu
            binding.txtKetQua.setTextColor(mau(if (tot) R.color.ok else R.color.alert))
            binding.txtKetQua.backgroundTintList = ContextCompat.getColorStateList(
                this, if (tot) R.color.ok_soft else R.color.alert_soft
            )
        }

        // Laptop chua noi thi nut "Xem phim" o man chinh da an, nhung man nay van co the dang
        // mo tu truoc (document laptop vua bi go). Phieu ghi luc do khong ai nhan.
        val chuaDuoc = when {
            !Laptop.daNoi(this) -> "Laptop chưa nối với tablet. Nhờ Ba Huy xem giúp."
            co <= 0 -> "Con chưa có phút chơi nào để đổi."
            else -> null
        }
        binding.txtChuaDoiDuoc.text = chuaDuoc
        binding.txtChuaDoiDuoc.visibility = if (chuaDuoc == null) View.GONE else View.VISIBLE
        binding.khungChon.visibility = if (chuaDuoc == null) View.VISIBLE else View.GONE

        cacNut.forEach { (n, phut) -> toNut(n, !chonHet && chon == phut) }
        toNut(binding.nutHet, chonHet)
        // Nut "Khác" sang mau khi so dang chon khong phai so cua mot nut nao.
        val c = chon
        toNut(binding.nutKhac, !chonHet && c != null && c !in Laptop.CAC_NUT)

        val phut = if (chonHet) co else c
        if (chuaDuoc != null || phut == null) {
            binding.txtXemTruoc.visibility = View.GONE
            binding.txtLuuY.visibility = View.GONE
            binding.btnDoi.visibility = View.GONE
            return
        }
        binding.txtXemTruoc.visibility = View.VISIBLE
        if (phut > co) {
            binding.txtXemTruoc.text = "Con chỉ có $co phút chơi."
            binding.txtXemTruoc.setTextColor(mau(R.color.alert))
            binding.txtLuuY.visibility = View.GONE
            binding.btnDoi.visibility = View.GONE
            return
        }
        binding.txtXemTruoc.text = "$phut phút chơi thành ${phut * tiLe} phút xem phim. " +
            "Laptop sẽ còn ${laptop + phut * tiLe} phút."
        binding.txtXemTruoc.setTextColor(mau(R.color.ink))
        // GateStore.doiPhut cat luon phien khi lay het, hay khi phan con lai duoi mot phut.
        val catPhien = (gate.state == GateState.ACTIVE || gate.state == GateState.PAUSED) &&
            (chonHet || phut == co)
        binding.txtLuuY.text = "Đổi rồi thì không lấy lại phút chơi được." +
            if (catPhien) " Đổi hết thì phiên chơi trên tablet dừng luôn." else ""
        binding.txtLuuY.visibility = View.VISIBLE
        binding.btnDoi.text = "Đổi $phut phút chơi"
        binding.btnDoi.visibility = View.VISIBLE
    }

    /** Nut dang chon: to dam hon va co vien, de con thay ngay minh vua bam cai nao. */
    private fun toNut(n: MaterialButton, dangChon: Boolean) {
        n.backgroundTintList = ContextCompat.getColorStateList(
            this, if (dangChon) R.color.brand_dark else R.color.brand
        )
        n.strokeColor = ContextCompat.getColorStateList(this, R.color.ink)
        n.strokeWidth = if (dangChon) 3.dp() else 0
    }

    /**
     * Hop go so phut cho nut "Khác". Go sai thi bao ngay duoi o va giu hop mo, khong dong hop
     * roi moi bao: con phai go lai tu dau. Go dung thi chi CHON so do, con van phai bam nut
     * Doi o man, nhu cac nut khac.
     */
    private fun hoiSoKhac() {
        val co = gate.phutDoiDuoc()
        val o = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = if (co > 0) "Từ 1 tới $co" else "Số phút chơi"
            setPadding(48, 40, 48, 40)
        }
        val hop = MaterialAlertDialogBuilder(this)
            .setTitle("Đổi bao nhiêu phút chơi?")
            .setView(o)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton("Thôi", null)
            .show()
        hop.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val so = o.text.toString().trim().toIntOrNull()
            val coBayGio = gate.phutDoiDuoc()
            when {
                so == null || so <= 0 -> o.error = "Gõ số phút trước đã."
                so > coBayGio -> o.error = "Con chỉ có $coBayGio phút chơi."
                else -> {
                    hop.dismiss()
                    chonSo(so)
                }
            }
        }
    }

    private fun doi() {
        val co = gate.phutDoiDuoc()
        val phut = (if (chonHet) co else chon) ?: return
        val kq = Laptop.doi(this, phut, chonHet)
        chon = null
        chonHet = false
        val loi = kq.loi
        if (loi != null) {
            ketQua = loi to false
            ve()
            return
        }
        val daDoi = "Đã đổi ${kq.phutChoi} phút chơi lấy ${kq.phutNetflix} phút xem phim."
        ketQua = "$daDoi Đang chờ laptop nhận." to true
        ve()
        kq.phieu?.let { ngheNhan(it, daDoi) }
    }

    /**
     * Cho phieu vua ghi bien mat: laptop cong gio xong thi xoa phieu. Chi tin ban tu may chu
     * (isFromCache false): ban trong may co the bao "khong co" truoc khi phieu kip len.
     * Mat mang hay laptop dang tat thi dai van ghi "Đang chờ laptop nhận"; phieu nam cho tren
     * Firestore, laptop bat len trong ngay la nhan.
     *
     * Khong gan listener vao vong doi Activity (ban co tham so Activity tu go o onStop): con
     * ra man chinh roi quay lai thi listener do khong tu gan lai, dai dung mai o "Đang chờ".
     * Go o [onDestroy].
     */
    private fun ngheNhan(phieu: DocumentReference, daDoi: String) {
        ngheNhan?.remove()
        ngheNhan = phieu.addSnapshotListener { snap, loi ->
            if (loi != null) {
                Log.w(TAG, "nghe phieu laptop hong: ${loi.message}")
                return@addSnapshotListener
            }
            if (snap == null || snap.exists() || snap.metadata.isFromCache) return@addSnapshotListener
            ngheNhan?.remove()
            ngheNhan = null
            // Con da doi them lan nua thi dai dang noi ve lan moi, khong ghi de.
            if (ketQua?.first?.startsWith(daDoi) == true) {
                ketQua = "$daDoi Laptop đã nhận." to true
                ve()
            }
        }
    }

    private fun mau(id: Int): Int = ContextCompat.getColor(this, id)

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "XemPhim"
    }
}
