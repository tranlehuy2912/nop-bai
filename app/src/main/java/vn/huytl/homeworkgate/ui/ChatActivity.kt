package vn.huytl.homeworkgate.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    /** Vong doc tin moi trong luc man hinh dang mo. */
    private var theoDoi: Job? = null

    /**
     * Quay ve tu man chup: may tam anh con vua chup de hoi bai.
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
        binding.txtEmpty.visibility = if (lines.isEmpty()) View.VISIBLE else View.GONE
        lines.forEach { binding.lines.addView(bongBong(it)) }
        binding.scroll.post { binding.scroll.fullScroll(View.FOCUS_DOWN) }
    }

    /** Mot bong bong tin nhan. Cua con nam ben phai, cua ba ben trai. */
    private fun bongBong(line: ChatLine): View {
        val cuaCon = line.from == ChatFrom.CON
        val dp = resources.displayMetrics.density

        val text = TextView(this).apply {
            setText(
                if (line.text.isBlank()) clock.format(Date(line.at))
                else "${line.text}\n${clock.format(Date(line.at))}"
            )
            textSize = 17f
            setPadding((18 * dp).toInt(), (12 * dp).toInt(), (18 * dp).toInt(), (12 * dp).toInt())
            setBackgroundResource(
                if (cuaCon) R.drawable.bg_bubble_con else R.drawable.bg_bubble_ba
            )
            setTextColor(
                if (cuaCon) android.graphics.Color.WHITE
                else androidx.core.content.ContextCompat.getColor(this@ChatActivity, R.color.ink)
            )
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (cuaCon) Gravity.END else Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * dp).toInt() }
            val le = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = if (cuaCon) 0 else (80 * dp).toInt()
                marginStart = if (cuaCon) (80 * dp).toInt() else 0
            }
            anhCua(line)?.let { addView(it, le) }
            addView(text, le)
        }
    }

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

        return android.widget.ImageView(this).apply {
            setImageBitmap(bm)
            adjustViewBounds = true
            maxWidth = rong
            contentDescription = "Ảnh trong tin nhắn"
            setPadding(0, 0, 0, (4 * dp).toInt())
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
                                "❓ ${getString(R.string.child_name)} hỏi bài" +
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
                ChatLine(ChatFrom.CON, chu.ifBlank { "(ảnh hỏi bài)" }, luc)
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
