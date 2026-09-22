package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.ChatFrom
import vn.huytl.homeworkgate.data.ChatLine
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.databinding.ActivityChatBinding
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.TelegramClient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Cho Le Hoa nhan mot viec gap cho Ba Huy: quen vo o lop, may sap het pin, bai kho
 * qua. Truoc day con khong co duong nao noi, ngoai viec gui anh bai tap.
 *
 * Man nay khong bi khoa theo gio choi. Chan duong bao tin cua mot dua tre de ep no
 * lam bai la doi hai thu khong nen doi.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var prefs: Prefs

    private val clock = SimpleDateFormat("HH:mm", Locale("vi", "VN"))

    /** Cho vach ngay cach day hon hai hom: "Thu ba, 16/9". */
    private val thuNgay = SimpleDateFormat("EEEE, d/M", Locale("vi", "VN"))

    /** Vong doc tin moi trong luc man hinh dang mo. */
    private var theoDoi: Job? = null

    /**
     * Quay ve tu man chup: may tam anh con vua chup.
     *
     * Gui lan luot tung tam, kem dong chu con dang go (neu co) dat vao tam dau. Gui
     * hong tam nao thi bo tam do, khong ghi vao khung chat - de con thay thieu ma
     * chup lai, chu khong tuong la Ba Huy da nhan.
     */
    private val chupTraVe = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { ket ->
        if (ket.resultCode != RESULT_OK) return@registerForActivityResult
        val anh = ket.data?.getStringArrayListExtra(CaptureActivity.KET_QUA_ANH)
            .orEmpty().map { java.io.File(it) }
        if (anh.isNotEmpty()) guiAnh(anh)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs.get(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            // Ban phim che mat o go chu neu khong chua cho cho no.
            view.updatePadding(top = bars.top, bottom = maxOf(bars.bottom, ime.bottom))
            insets
        }

        binding.btnChup.setOnClickListener {
            chupTraVe.launch(
                android.content.Intent(this, CaptureActivity::class.java)
                    .putExtra(CaptureActivity.EXTRA_CHAT, true)
            )
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnXoaChat.setOnClickListener { hoiXoaHet() }
        binding.btnSend.setOnClickListener { gui() }

        // Mo man chat la don luon cau qua cu khoi may.
        ChatBox.donDep(this)
        render()
        ChatBox.markRead(this)
    }

    override fun onResume() {
        super.onResume()
        ApprovalService.ensureRunning(this)
        // Ba tra loi trong luc man nay dang mo thi hien ra luon, khong bat con
        // thoat ra vao lai moi thay.
        //
        // Giu lai Job de huy o onPause. Khong huy thi moi lan quay lai man nay lai
        // them mot vong lap nua chay song song, ca dam cung doc mot cho.
        theoDoi?.cancel()
        theoDoi = lifecycleScope.launch {
            var seen = ChatBox.read(this@ChatActivity).size
            while (true) {
                delay(3_000)
                val now = ChatBox.read(this@ChatActivity)
                if (now.size != seen) {
                    seen = now.size
                    render()
                    ChatBox.markRead(this@ChatActivity)
                }
            }
        }
    }

    override fun onPause() {
        theoDoi?.cancel()
        theoDoi = null
        super.onPause()
    }

    /**
     * Hoi truoc khi xoa, vi xoa xong khong lay lai duoc.
     *
     * Ban sao cac tin nay van nam ben Telegram cua Ba Huy, nen xoa o day la don
     * may chu khong phai mat han cuoc noi chuyen.
     */
    private fun hoiXoaHet() {
        if (ChatBox.read(this).isEmpty()) {
            toast("Chưa có tin nào để xoá")
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Xoá hết tin nhắn?")
            .setMessage("Xoá sạch các tin trong máy. Bên Telegram của ba Huy vẫn còn.")
            .setPositiveButton("Xoá hết") { _, _ ->
                ChatBox.xoaHet(this)
                render()
                toast("Đã xoá")
            }
            .setNegativeButton("Để nguyên", null)
            .show()
    }

    private fun render() {
        val lines = ChatBox.read(this)
        binding.lines.removeAllViews()
        binding.tinTrong.visibility = if (lines.isEmpty()) View.VISIBLE else View.GONE

        // Chen mot moc moi lan sang ngay khac. Khong co no thi cau hom qua va cau
        // sang nay dinh lien mot mach, ma khoang cach giua hai cau lai chinh la thu
        // quyet dinh doc chung nhu the nao.
        var ngayTruoc = ""
        lines.forEach { line ->
            val ngay = tenNgay(line.at)
            if (ngay != ngayTruoc) {
                binding.lines.addView(mocNgay(ngay))
                ngayTruoc = ngay
            }
            binding.lines.addView(bongBong(line))
        }
        binding.scroll.post { binding.scroll.fullScroll(View.FOCUS_DOWN) }
    }

    /** "Hom nay", "Hom qua", con xa hon thi ghi han thu may ngay may. */
    private fun tenNgay(luc: Long): String {
        val homNay = dauNgay(System.currentTimeMillis())
        return when (dauNgay(luc)) {
            homNay -> "Hôm nay"
            homNay - 24 * 60 * 60_000L -> "Hôm qua"
            else -> thuNgay.format(Date(luc)).replaceFirstChar { it.uppercase() }
        }
    }

    private fun dauNgay(luc: Long): Long = Calendar.getInstance().apply {
        timeInMillis = luc
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Vach ngay, doi mau chu nho giong nhan nhom o man chinh. */
    private fun mocNgay(chu: String): View {
        val dp = resources.displayMetrics.density
        return AppCompatTextView(this).apply {
            text = chu
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@ChatActivity, R.color.ink_soft))
            setPadding(0, (18 * dp).toInt(), 0, (6 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    /**
     * Mot bong bong tin nhan. Cua con nam ben phai, cua ba ben trai.
     *
     * ANH, CHU VA GIO NAM TRONG CUNG MOT BONG BONG. Truoc day tam anh la mot view
     * rieng dat phia tren, con gio thi noi vao duoi cau chu trong cung mot o text
     * cung co chu - nen mot tin chi co anh de ra mot cuc mau xanh trong chi de ghi
     * "08:22", va gio thi to bang cau noi, doc nhu mot dong cua tin nhan.
     */
    private fun bongBong(line: ChatLine): View {
        val cuaCon = line.from == ChatFrom.CON
        val dp = resources.displayMetrics.density
        val anh = anhCua(line)
        val mauChu = if (cuaCon) android.graphics.Color.WHITE else mau(R.color.ink)
        val mauGio = mau(if (cuaCon) R.color.chat_gio_con else R.color.ink_soft)

        // Bong bong co anh thi vien mong lai, de tam anh gan nhu lap day no; chu va
        // gio luc do tu thut vao bang le rieng.
        val le = (if (anh != null) 6 else 18) * dp
        val leDoc = (if (anh != null) 6 else 12) * dp
        val leChu = (if (anh != null) 10 * dp else 0f).toInt()

        val bong = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(
                if (cuaCon) R.drawable.bg_bubble_con else R.drawable.bg_bubble_ba
            )
            setPadding(le.toInt(), leDoc.toInt(), le.toInt(), leDoc.toInt())
        }

        anh?.let { bong.addView(it) }

        if (line.text.isNotBlank()) {
            bong.addView(AppCompatTextView(this).apply {
                text = line.text
                textSize = 17f
                setLineSpacing(4 * dp, 1f)
                setTextColor(mauChu)
                setPadding(leChu, (if (anh != null) 8 * dp else 0f).toInt(), leChu, 0)
            })
        }

        // Gio o cuoi bong bong, ca hai ben: no la dau cham het cua mot tin, khong
        // phai nhan cua ai noi.
        bong.addView(
            AppCompatTextView(this).apply {
                text = clock.format(Date(line.at))
                textSize = 12f
                setTextColor(mauGio)
                gravity = Gravity.END
                setPadding(leChu, (3 * dp).toInt(), leChu, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (cuaCon) Gravity.END else Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (6 * dp).toInt() }
            addView(
                bong,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Chua cho ben kia mot khoang, de bong bong khong keo het be
                    // ngang va con nhin ra ngay ai dang noi.
                    marginEnd = if (cuaCon) 0 else (80 * dp).toInt()
                    marginStart = if (cuaCon) (80 * dp).toInt() else 0
                }
            )
        }
    }

    private fun mau(id: Int) = ContextCompat.getColor(this, id)

    /**
     * Tam anh di kem mot cau, neu co va neu file van con.
     *
     * Giai ma anh nho thoi - bong bong chat rong chung mot phan ba man, nap ca tam
     * vai megabyte vao day la phi bo nho ma nhin cung khong ro hon.
     */
    private fun anhCua(line: ChatLine): View? {
        val duong = line.anh ?: return null
        val f = java.io.File(duong)
        if (!f.exists()) return null
        val dp = resources.displayMetrics.density
        val rong = (260 * dp).toInt()

        val bm = runCatching {
            val bien = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(duong, bien)
            var mau = 1
            while (bien.outWidth / (mau * 2) >= rong) mau *= 2
            android.graphics.BitmapFactory.decodeFile(
                duong,
                android.graphics.BitmapFactory.Options().apply { inSampleSize = mau }
            )
        }.getOrNull() ?: return null

        /*
         * Bo goc tam anh cho khop voi bong bong boc no. Goc vuong nam trong mot cai
         * vo bo 20dp la cho de thay nhat tren ca man hinh nay.
         *
         * Ban kinh phai nhan len theo ty le thu nho: RoundedBitmapDrawable bo goc
         * tinh bang pixel CUA TAM ANH, ma ImageView thi co tam anh lai cho vua 260dp
         * - de nguyen 14dp thi anh cang to goc nhin cang vuong.
         */
        val tyLe = bm.width / minOf(bm.width, rong).toFloat()
        val tron = RoundedBitmapDrawableFactory.create(resources, bm).apply {
            cornerRadius = 14 * dp * tyLe
        }

        return AppCompatImageView(this).apply {
            setImageDrawable(tron)
            adjustViewBounds = true
            maxWidth = rong
            contentDescription = "Ảnh trong tin nhắn"
        }
    }


    private fun guiAnh(anh: List<java.io.File>) {
        if (!prefs.isConfigured) {
            toast("${getString(R.string.parent_name_cap)} chưa cài đặt xong")
            anh.forEach { it.delete() }
            return
        }
        val chu = binding.input.text.toString().trim()
        binding.btnChup.isEnabled = false
        binding.btnSend.isEnabled = false

        lifecycleScope.launch {
            val giu = withContext(Dispatchers.IO) {
                val client = TelegramClient(prefs.botToken)
                val kho = ChatBox.thuMucAnh(this@ChatActivity)
                anh.mapIndexedNotNull { i, f ->
                    val nho = runCatching { ImageUtil.shrinkBaiGiai(f) }.getOrDefault(f)
                    val ok = runCatching {
                        client.sendPhoto(
                            prefs.parentChatId,
                            nho,
                            if (i == 0) {
                                "📷 ${getString(R.string.child_name)} gửi hình" +
                                    if (chu.isNotEmpty()) ":\n$chu" else ""
                            } else {
                                ""
                            },
                            null
                        )
                    }.isSuccess
                    // Giu mot ban trong may de con mo lai xem duoc, roi don ban goc.
                    val giuLai = java.io.File(kho, "con_${System.currentTimeMillis()}_$i.jpg")
                    if (ok) runCatching { nho.copyTo(giuLai, overwrite = true) }
                    if (nho != f) nho.delete()
                    f.delete()
                    if (ok) giuLai.absolutePath else null
                }
            }
            binding.btnChup.isEnabled = true
            binding.btnSend.isEnabled = true

            if (giu.isEmpty()) {
                toast("Gửi ảnh không được, kiểm tra mạng rồi thử lại")
                return@launch
            }
            val luc = System.currentTimeMillis()
            giu.forEachIndexed { i, duong ->
                ChatBox.add(
                    this@ChatActivity, ChatFrom.CON,
                    if (i == 0) chu else "", luc + i, duong
                )
            }
            ChatBox.noteSent(this@ChatActivity)
            DongBo.dayTin(
                this@ChatActivity,
                ChatLine(ChatFrom.CON, chu.ifBlank { "(hình)" }, luc)
            )
            binding.input.setText("")
            render()
            ApprovalService.ensureRunning(this@ChatActivity)
        }
    }

    private fun gui() {
        val text = binding.input.text.toString().trim()
        if (text.isEmpty()) return
        if (!prefs.isConfigured) {
            toast("${getString(R.string.parent_name_cap)} chưa cài đặt xong")
            return
        }
        ChatBox.blockReason(this)?.let {
            toast(it)
            return
        }

        binding.btnSend.isEnabled = false
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    TelegramClient(prefs.botToken).sendMessage(
                        chatId = prefs.parentChatId,
                        text = "💬 ${getString(R.string.child_name)} nhắn:\n$text"
                    )
                }.isSuccess
            }
            binding.btnSend.isEnabled = true
            if (ok) {
                ChatBox.add(this@ChatActivity, ChatFrom.CON, text)
                ChatBox.noteSent(this@ChatActivity)
                // Sang ca hai duong: Telegram cho Ba Huy doc ngay tren dien thoai,
                // va Firestore cho khung chat trong app Bang dieu khien.
                DongBo.dayTin(
                    this@ChatActivity,
                    ChatLine(ChatFrom.CON, text, System.currentTimeMillis())
                )
                binding.input.setText("")
                render()
                ApprovalService.ensureRunning(this@ChatActivity)
            } else {
                toast("Gửi không được, kiểm tra mạng rồi thử lại")
            }
        }
    }

    private fun toast(text: String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
