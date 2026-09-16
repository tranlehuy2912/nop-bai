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
            .setMessage("Xoá sạch các tin trong máy. Bên Telegram của Ba Huy vẫn còn.")
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
            setText("${line.text}\n${clock.format(Date(line.at))}")
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
            addView(text, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = if (cuaCon) 0 else (80 * dp).toInt()
                      marginStart = if (cuaCon) (80 * dp).toInt() else 0 })
        }
    }

    private fun gui() {
        val text = binding.input.text.toString().trim()
        if (text.isEmpty()) return
        if (!prefs.isConfigured) {
            toast("${getString(R.string.parent_name)} chưa cài đặt xong")
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
