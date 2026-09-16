package vn.huytl.homeworkgate.dongbo

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.ChatFrom
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.guard.ChuongTin
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.Permissions
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.TelegramClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Lam mot lenh gui tu app Bang dieu khien.
 *
 * Doi song doi voi phan xu ly lenh Telegram trong ApprovalService.handleMessage.
 * Khong gop hai duong lam mot vi ben do moi buoc deu ket thuc bang mot cau tra loi
 * gui vao chat, con ben nay tra loi bang cach ghi xuong Firestore - go chung ra thi
 * moi ham deu phai mang theo mot cai "gui di dau" rong tuech.
 *
 * Cai PHAI dung chung, va dang dung chung, la [GateStore]: moi luat ve gio ngu,
 * tran phut ngay, cong don phieu cu deu nam trong do. Hai duong chi la hai cai mieng
 * noi vao cung mot cai kho.
 *
 * Tra ve cau ngan de bao lai cho dien thoai. Cau nay hien duoi dang mot dong nho
 * tren man Bang, nen viet nhu noi voi nguoi chu khong phai ma loi.
 */
object ThiHanhLenh {

    fun lam(context: Context, d: DocumentSnapshot): String {
        val kieu = d.getString(Duong.F_KIEU).orEmpty()
        val phut = (d.getLong(Duong.F_PHUT))?.toInt()
        val baiId = d.getString(Duong.F_BAI_ID)
        val chu = d.getString(Duong.F_CHU).orEmpty()
        val taoLuc = d.getLong("tao") ?: 0L

        // Lenh go tu lau qua thi bo.
        //
        // Giong het ly do ben Telegram: tablet mat mang ca buoi toi, sang hom sau
        // vua len mang la ca xau lenh do xuong mot luc - "cho 60 phut" bam toi qua
        // tu dung mo gio choi vao sang som ma khong ai bam gi.
        if (DongBo.quaCu(taoLuc)) {
            val luc = SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(taoLuc))
            return "Lệnh bấm lúc $luc, lâu quá rồi nên máy bỏ qua."
        }

        val gate = GateStore(context)
        val prefs = Prefs.get(context)
        val con = context.getString(R.string.child_name)

        return when (kieu) {
            Lenh.DUYET -> duyet(context, gate, baiId, phut)

            Lenh.TU_CHOI -> {
                val bai = baiId?.let { id -> gate.baiDangCho().firstOrNull { it.id == id } }
                    ?: gate.baiChoCuNhat()
                    ?: return "Không có bài nào đang chờ."
                gate.boBaiCho(bai.id)
                goNutBenTelegram(context, bai.messageId)
                baiId?.let { DongBo.datTrangThaiBai(context, it, "TUCHOI") }
                DayLog.add(context, "Ba Huy không duyệt" + if (chu.isBlank()) "" else ": $chu")
                "Đã từ chối bài đó."
            }

            // Gio thuong: khong tru vao han muc ngay, vi day la Ba Huy chu dong cho
            // chu khong phai con doi bang bai tap.
            Lenh.CHO -> cho(context, gate, prefs, phut)

            Lenh.BOT -> {
                val bot = phut ?: 15
                val conLai = gate.extend(-bot)
                when {
                    conLai == null -> "$con đang không trong giờ chơi."
                    conLai <= 0 -> {
                        DayLog.add(context, "Ba Huy bớt giờ, hết luôn phiên")
                        "Bớt $bot phút là hết giờ luôn. Đã khoá."
                    }
                    else -> {
                        DayLog.add(context, "Ba Huy bớt $bot phút")
                        "Đã bớt $bot phút, còn $conLai phút."
                    }
                }
            }

            Lenh.DUNG -> {
                val giu = gate.pause()
                if (giu == null) "$con đang không trong giờ chơi."
                else {
                    DayLog.add(context, "Ba Huy cho tạm dừng, giữ $giu phút")
                    "Đã tạm dừng, giữ $giu phút."
                }
            }

            Lenh.TIEP -> {
                val tiep = gate.resume()
                when {
                    tiep != null -> {
                        DayLog.add(context, "Ba Huy cho chơi tiếp $tiep phút")
                        "Chơi tiếp, còn $tiep phút."
                    }
                    gate.state == GateState.LOCKED -> "Không còn phiên nào đang tạm dừng."
                    else -> "$con đang không tạm dừng."
                }
            }

            Lenh.KHOA -> {
                ParentMode.disable(context)
                gate.endSession(EndReason.PARENT_REVOKED)
                DayLog.add(context, "Ba Huy khoá máy")
                "Đã khoá tablet."
            }

            Lenh.MO_MAY -> {
                ParentMode.enable(context, phut)
                ApprovalService.ensureRunning(context)
                DayLog.add(context, "Mở toàn bộ máy (${ParentMode.moTa(context)})")
                "Đã mở toàn bộ máy, ${ParentMode.moTa(context)}."
            }

            Lenh.DONG_MAY -> {
                ParentMode.disable(context)
                DayLog.add(context, "Đóng chế độ Ba Huy")
                "Đã khoá máy lại."
            }

            Lenh.CHO_GO_APP -> {
                if (!Permissions.hasDeviceAdmin(context)) {
                    "Quản trị thiết bị đang tắt sẵn rồi, gỡ app được."
                } else {
                    context.getSystemService(DevicePolicyManager::class.java)
                        .removeActiveAdmin(Permissions.adminComponent(context))
                    // Mo luon che do Ba Huy: khong thi guard van chan duong vao Cai
                    // dat, tat quan tri roi ma van khong vao go duoc.
                    ParentMode.enable(context, 15)
                    DayLog.add(context, "Tắt quản trị thiết bị để gỡ app")
                    "Đã tắt quản trị thiết bị, mở máy 15 phút — gỡ app được."
                }
            }

            Lenh.XOA_PIN -> {
                // Xoa PIN chu khong dat PIN moi tu xa: PIN go o dau thi no nam lai
                // o do, va cho nay la mot cai kho tren mang.
                prefs.clearPin()
                ParentMode.enable(context)
                DayLog.add(context, "Ba Huy xoá mã PIN")
                "Đã xoá PIN và mở khoá. Đặt PIN mới ngay trên tablet."
            }

            Lenh.NHAN -> {
                // Tin da duoc ghi xuong Firestore o ben dien thoai roi. Cho nay chi
                // lam not phan tren may: cho vao khung chat cua con va keu len.
                if (chu.isBlank()) return "Tin rỗng, không chuyển."
                ChatBox.add(context, ChatFrom.BA, chu)
                ChatBox.stopWaiting(context)
                ChuongTin.keu(context, chu)
                "Đã chuyển cho $con."
            }

            Lenh.CAI_DAT -> doiCaiDat(context, chu, d.get("giaTri"))

            else -> "Không hiểu lệnh $kieu."
        }
    }

    /**
     * Duyet mot bai.
     *
     * Dang choi ma duyet them thi cong thang vao phien dang chay chu khong cat phien
     * roi cap lai tu dau - y het duong Telegram.
     */
    private fun duyet(context: Context, gate: GateStore, baiId: String?, phut: Int?): String {
        val bai = baiId?.let { id -> gate.baiDangCho().firstOrNull { it.id == id } }
            ?: gate.baiChoCuNhat()
            ?: return "Bài đó không còn trong hàng chờ nữa."

        val xin = phut ?: Prefs.get(context).grantMinutes

        if (gate.state == GateState.ACTIVE) {
            val conLai = gate.extend(xin, useQuota = true)
            gate.boBaiCho(bai.id)
            goNutBenTelegram(context, bai.messageId)
            DongBo.datTrangThaiBai(context, bai.id, "DUYET", xin)
            DayLog.add(context, "Duyệt $xin phút giữa phiên")
            return "Đang chơi nên cộng thẳng $xin phút, còn ${conLai ?: 0} phút."
        }

        val duoc = gate.approve(wantedMinutes = xin, useQuota = true, requestId = bai.id)
            ?: return khongCapDuoc(context)

        goNutBenTelegram(context, bai.messageId)
        DongBo.datTrangThaiBai(context, bai.id, "DUYET", xin)
        DayLog.add(context, "Duyệt $duoc phút (Bảng điều khiển)")
        ApprovalService.ensureRunning(context)
        // Con dang giu phieu cu ma nop them bai thi duyet la cong don, khong de len.
        return if (duoc > xin) "Đã duyệt thêm $xin phút, cộng dồn thành $duoc phút."
        else "Đã duyệt $duoc phút."
    }

    private fun cho(context: Context, gate: GateStore, prefs: Prefs, phut: Int?): String {
        val xin = phut ?: prefs.grantMinutes

        if (gate.state == GateState.ACTIVE) {
            val conLai = gate.extend(xin)
            DayLog.add(context, "Ba Huy cho thêm $xin phút giữa phiên")
            return "Đang chơi nên cộng thẳng $xin phút, còn ${conLai ?: 0} phút."
        }

        val duoc = gate.approve(wantedMinutes = xin, useQuota = false)
            ?: return khongCapDuoc(context)
        DayLog.add(context, "Ba Huy cho $duoc phút")
        ApprovalService.ensureRunning(context)
        return "Đã cho $duoc phút (chưa tính giờ)."
    }

    private fun khongCapDuoc(context: Context): String {
        val prefs = Prefs.get(context)
        val tu = prefs.hardStopMinuteOfDay
        val den = prefs.gioDayMinuteOfDay
        return "Không cấp được: đang trong giờ ngủ " +
            "%02d:%02d-%02d:%02d".format(tu / 60, tu % 60, den / 60, den % 60) +
            ", hoặc hôm nay đã đủ ${prefs.tranPhutMoiNgay} phút. Bấm Cho chơi ngay để cho thêm."
    }

    /**
     * Doi mot muc cau hinh.
     *
     * Dien thoai khong ghi thang vao ban sao cau hinh tren Firestore; no gui lenh
     * qua day. Nho the, moi gia tri deu di qua dung mot cho kiem tra, va con so hien
     * ben kia luon la con so that dang chay chu khong phai con so vua mong muon.
     */
    private fun doiCaiDat(context: Context, ten: String, giaTri: Any?): String {
        val prefs = Prefs.get(context)
        val so = (giaTri as? Number)?.toInt()

        val cau = when (ten) {
            "phutMacDinh" -> {
                val v = so?.coerceIn(5, 180) ?: return "Thiếu số phút."
                prefs.grantMinutes = v
                "Mỗi lần duyệt giờ là $v phút."
            }
            "tranPhutMoiNgay" -> {
                val v = so?.coerceIn(15, 480) ?: return "Thiếu số phút."
                prefs.tranPhutMoiNgay = v
                "Trần mỗi ngày giờ là $v phút."
            }
            "gioNgu" -> {
                val v = so?.coerceIn(0, 24 * 60 - 1) ?: return "Thiếu giờ."
                prefs.hardStopMinuteOfDay = v
                "Giờ ngủ giờ là %02d:%02d.".format(v / 60, v % 60)
            }
            "gioDay" -> {
                val v = so?.coerceIn(0, 24 * 60 - 1) ?: return "Thiếu giờ."
                prefs.gioDayMinuteOfDay = v
                "Giờ dậy giờ là %02d:%02d.".format(v / 60, v % 60)
            }
            "khoaCaiDat" -> {
                val v = giaTri as? Boolean ?: return "Giá trị không phải bật/tắt."
                prefs.lockSystemSettings = v
                if (v) "Đã khoá màn Cài đặt của máy." else "Đã mở màn Cài đặt của máy."
            }
            "appChoPhep" -> {
                prefs.allowedPackages = danhSach(giaTri)
                "Danh sách app được chơi: ${prefs.allowedPackages.size} app."
            }
            "appChan" -> {
                prefs.blockedPackages = danhSach(giaTri)
                "Danh sách app chặn hẳn: ${prefs.blockedPackages.size} app."
            }
            "appAi" -> {
                prefs.aiPackages = danhSach(giaTri)
                "Danh sách app AI ghi câu hỏi: ${prefs.aiPackages.size} app."
            }
            "gioiHanApp" -> {
                @Suppress("UNCHECKED_CAST")
                val m = giaTri as? Map<String, Number> ?: return "Giá trị không đúng dạng."
                m.forEach { (goi, p) -> GioiHanApp.datHan(context, goi, p.toInt()) }
                "Đã đổi giới hạn cho ${m.size} app."
            }
            else -> return "Không có mục cài đặt tên $ten."
        }

        DayLog.add(context, "Ba Huy đổi cài đặt: $cau")
        // Ghi lai ban sao ngay, khong doi nhip tim: man Cai dat ben dien thoai dang
        // mo, va con so cu nam do them mot phut la Ba Huy bam lai lan nua.
        DongBo.dayCaiDat(context)
        return cau
    }

    /**
     * Go hai nut Duyet / Khong duyet nam duoi tin nop bai ben Telegram.
     *
     * Xu ly bai bang app Bang dieu khien thi ben Telegram khong biet gi, va hai cai
     * nut cu van nam nguyen duoi anh. Bam vao do sau nay khong cap gio hai lan -
     * [vn.huytl.homeworkgate.telegram.ApprovalService] kiem tra bai con trong hang
     * cho khong roi moi lam - nhung no tra ve "Yeu cau nay cu roi", ma doc cau do
     * trong luc dang tim xem bai da duyet chua thi roi tri. Go han cho sach.
     *
     * Ham goi no dang chay o luong chinh, trong callback cua Firestore, nen phai nem
     * sang luong nen: goi mang o luong chinh la NetworkOnMainThreadException. Nem
     * xong thi quen di - go khong duoc cung chi thua hai cai nut chet.
     */
    private fun goNutBenTelegram(context: Context, messageId: Long) {
        if (messageId == 0L) return
        val prefs = Prefs.get(context)
        if (prefs.botToken.isBlank() || prefs.parentChatId == 0L) return
        thread(isDaemon = true) {
            runCatching {
                TelegramClient(prefs.botToken).clearReplyMarkup(prefs.parentChatId, messageId)
            }
        }
    }

    private fun danhSach(giaTri: Any?): Set<String> =
        (giaTri as? List<*>).orEmpty().filterIsInstance<String>().toSet()
}
