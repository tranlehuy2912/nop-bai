package vn.huytl.homeworkgate.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.huytl.homeworkgate.data.KhoaAi
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.databinding.ActivitySetupBinding
import vn.huytl.homeworkgate.guard.Heartbeat
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.PhienQuanLy
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.TelegramClient

/**
 * Man cai dat cua bo. Chi vao duoc khi da nhap dung PIN o man chinh, hoac khi
 * may chua cai dat lan nao.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs.get(this)

        // Vao thang man nay ma khong qua PIN thi dong lai. Lan dau chua co PIN
        // thi cho vao, vi luc do chua co gi de bao ve.
        if (prefs.hasPin() && !PhienQuanLy.daQuaPin && !ParentMode.isActive(this)) {
            toast("Cần nhập mã PIN ở màn hình chính")
            finish()
            return
        }

        load()

        binding.btnDetectChat.setOnClickListener { detectChatId() }
        binding.btnTest.setOnClickListener { sendTest() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnPickApps.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        binding.btnGioiHan.setOnClickListener {
            startActivity(
                Intent(this, AppPickerActivity::class.java)
                    .putExtra(AppPickerActivity.EXTRA_DANH_SACH, AppPickerActivity.HAN)
            )
        }
        binding.btnPickBlocked.setOnClickListener {
            startActivity(
                Intent(this, AppPickerActivity::class.java)
                    .putExtra(AppPickerActivity.EXTRA_DANH_SACH, AppPickerActivity.DEN)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Giong trang cau hinh: roi khoi app roi quay lai thi khong duoc vao thang
        // day nua. HomeActivity la singleTask nen mo no la don sach ca hai man.
        if (PhienQuanLy.phaiVeManCon()) {
            PhienQuanLy.daQuaPin = false
            PhienQuanLy.thoiMoCaiDat()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        veCanhBao()
    }

    /**
     * Bang "con thieu gi" o dau trang, moi dong bam duoc.
     *
     * Ve lai moi lan quay ve man nay: Ba Huy bam mot dong, di sang man hinh he
     * thong bat quyen, quay lai la dong do phai bien mat ngay thi moi biet la xong.
     */
    private fun veCanhBao() {
        CanhBao.veThe(
            this,
            binding.cardWarning,
            binding.txtWarningTitle,
            binding.boxWarning
        ) { veCanhBao() }
    }

    private fun load() {
        binding.edtToken.setText(prefs.botToken)
        if (prefs.parentChatId != 0L) {
            binding.edtChatId.setText(prefs.parentChatId.toString())
        }
        binding.edtAiKeys.setText(prefs.aiKeys.joinToString("\n"))
        veDongKhoaAi()
        binding.edtMinutes.setText(prefs.grantMinutes.toString())
        binding.edtHardStop.setText(formatMinuteOfDay(prefs.hardStopMinuteOfDay))
        binding.edtGioDay.setText(formatMinuteOfDay(prefs.gioDayMinuteOfDay))
        binding.edtDailyLimit.setText(prefs.tranPhutMoiNgay.toString())
        binding.swLockSettings.isChecked = prefs.lockSystemSettings
        binding.swManChan.isChecked = prefs.batManChan
    }

    private fun save() {
        val token = binding.edtToken.text?.toString()?.trim().orEmpty()
        if (token.isEmpty()) {
            toast("Chưa có bot token")
            return
        }
        val chatId = binding.edtChatId.text?.toString()?.trim()?.toLongOrNull()
        if (chatId == null || chatId == 0L) {
            toast("Chat id chưa đúng")
            return
        }
        val hardStop = parseMinuteOfDay(binding.edtHardStop.text?.toString())
        if (hardStop == null) {
            toast("Giờ đi ngủ phải viết dạng 22:00")
            return
        }
        val gioDay = parseMinuteOfDay(binding.edtGioDay.text?.toString())
        if (gioDay == null) {
            toast("Giờ mở lại buổi sáng phải viết dạng 06:00")
            return
        }
        val pin = binding.edtPin.text?.toString().orEmpty()
        if (!prefs.hasPin() && pin.length < 4) {
            toast("Đặt mã PIN ít nhất 4 số")
            return
        }

        prefs.botToken = token
        prefs.parentChatId = chatId
        prefs.aiKeys = binding.edtAiKeys.text?.toString().orEmpty().split("\n")
        prefs.grantMinutes = binding.edtMinutes.text?.toString()?.toIntOrNull() ?: 60
        prefs.hardStopMinuteOfDay = hardStop
        prefs.gioDayMinuteOfDay = gioDay
        prefs.tranPhutMoiNgay = binding.edtDailyLimit.text?.toString()?.toIntOrNull() ?: 135
        prefs.lockSystemSettings = binding.swLockSettings.isChecked
        // Tat man chan thi bo luon viec da mo som truoc do, khong de mot ma buoi cu
        // nam lai lam buoi do thoi chan sau khi bat lai.
        prefs.batManChan = binding.swManChan.isChecked
        if (!prefs.batManChan) prefs.buoiDuocMoSom = ""
        if (pin.isNotEmpty()) prefs.setPin(pin)

        Heartbeat.schedule(this)
        ApprovalService.ensureRunning(this)
        toast("Đã lưu")
        finish()
    }

    /**
     * Mot dong noi ro dang dung khoa nao, vi ban than danh sach khoa nhin nhu nhau
     * ca: hai dong chu AIza dai bang nhau khong cho biet cai nao dang chay.
     */
    private fun veDongKhoaAi() {
        binding.txtAiHint.text = KhoaAi.moTa(this)
    }

    /**
     * Lay chat id bang cach doc tin nhan gan nhat gui cho bot. Bo chi can nhan
     * mot cau bat ky cho bot roi bam nut nay, khoi phai di tim id o dau.
     */
    private fun detectChatId() {
        val token = binding.edtToken.text?.toString()?.trim().orEmpty()
        if (token.isEmpty()) {
            toast("Nhập bot token trước đã")
            return
        }
        binding.txtDetectHint.text = "Đang tìm… Nhắn một câu bất kỳ cho bot rồi chờ vài giây."

        lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) {
                runCatching {
                    val client = TelegramClient(token)
                    client.getUpdates(offset = 0, allowed = listOf("message"))
                        .mapNotNull {
                            it.optJSONObject("message")?.optJSONObject("from")?.optLong("id")
                        }
                        .lastOrNull { it != 0L }
                }.getOrNull()
            }

            if (found == null) {
                binding.txtDetectHint.text =
                    "Chưa thấy tin nào. Mở Telegram, nhắn cho bot một câu, rồi bấm lại."
            } else {
                binding.edtChatId.setText(found.toString())
                binding.txtDetectHint.text = "Đã tìm ra chat id $found"
            }
        }
    }

    private fun sendTest() {
        val token = binding.edtToken.text?.toString()?.trim().orEmpty()
        val chatId = binding.edtChatId.text?.toString()?.trim()?.toLongOrNull()
        if (token.isEmpty() || chatId == null) {
            toast("Cần token và chat id")
            return
        }
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    TelegramClient(token).sendMessage(
                        chatId,
                        "Tablet kết nối được rồi. Đây là tin thử."
                    )
                }.isSuccess
            }
            toast(if (ok) "Gửi được, kiểm tra Telegram nhé" else "Gửi không được")
        }
    }

    private fun formatMinuteOfDay(value: Int): String =
        "%02d:%02d".format(value / 60, value % 60)

    private fun parseMinuteOfDay(text: String?): Int? {
        val parts = text?.trim()?.split(":") ?: return null
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun toast(text: String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
