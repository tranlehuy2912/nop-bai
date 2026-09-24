package vn.huytl.homeworkgate.dongbo

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.ChamTheoClaude
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.ChatFrom
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.KhaiChoCham
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.LuotBaNoi
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.SuaCham
import vn.huytl.homeworkgate.guard.ChuongTin
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.guard.Permissions
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier
import vn.huytl.homeworkgate.telegram.TelegramClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Lam mot lenh gui tu app Bang dieu khien hay app Cho gio choi.
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
        // Thieu truong nay la ban Bang dieu khien cu, ma ban do thi chi Ba Huy cam.
        val ai = d.getString(Duong.F_AI) ?: Nguoi.BA_HUY

        // Lenh go tu lau qua thi bo.
        //
        // Giong het ly do ben Telegram: tablet mat mang ca buoi toi, sang hom sau
        // vua len mang la ca xau lenh do xuong mot luc - "cho 60 phut" bam toi qua
        // tu dung mo gio choi vao sang som ma khong ai bam gi.
        //
        // Tru tin cua co. Tin gui toi qua thi sang nay van dung nguyen, con bo di la
        // con khong bao gio biet co dan gi - ma Ba Huy thi tuong da chuyen roi.
        if (kieu != Lenh.TIN_CO && DongBo.quaCu(taoLuc)) {
            val luc = SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(taoLuc))
            return "Lệnh bấm lúc $luc, lâu quá rồi nên máy bỏ qua."
        }

        /*
         * May ba noi chi go duoc lenh cho gio.
         *
         * Luat ben firestore.rules da chan tan goc roi - uid cua may ba nam trong
         * uidsPhu chu khong phai uids, ma luat chi cho uidsPhu tao document co
         * kieu CHO. Cho nay chan lan hai, cho truong hop luat bi dan de len bang
         * ban cu trong console Firebase: luat thi sua bang tay o mot cho khong ai
         * nhin thay, con dong nay thi di theo ban app.
         */
        if (ai == Nguoi.BA_NOI && kieu != Lenh.CHO) {
            return "Máy bà nội chỉ cho giờ được thôi."
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

            // Gio thuong: khong tru vao han muc ngay, vi day la nguoi lon chu dong
            // cho chu khong phai con doi bang bai tap.
            Lenh.CHO -> cho(context, gate, prefs, phut, ai)

            Lenh.CONG_VIEC_NHA -> congViecNha(context, gate, phut, chu)

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
                DayLog.add(context, "Đóng chế độ ba Huy")
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

            Lenh.TIN_CO -> tinCo(context, chu, taoLuc)

            Lenh.CAI_DAT -> doiCaiDat(context, chu, d.get("giaTri"))

            Lenh.SUA_CHAM -> suaCham(context, gate, d.get("giaTri"))

            Lenh.CHAM_BAI -> chamTheoClaude(context, gate, baiId, d.get("giaTri"))

            // Ben kia vua mo app va hoi tablet con song khong. Day mot ban trang
            // thai day du roi thoi: khong ghi nhat ky, khong tra loi gi. Ban trang
            // thai do chinh la cau tra loi, va no den qua duong khac.
            Lenh.PING -> {
                DongBo.dayDayDu()
                ""
            }

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

    /**
     * Cong bu gio cho mot dot viec nha tablet da bo lo.
     *
     * KHI NAO CO LENH NAY: ba bam xong het trong luc tablet dang tat. Den luc tablet
     * song lai thi ban da qua [Duong.QUA_CU_MS] nen no bo qua, con app ben ba thi giu
     * nguyen dot do cho den khi tablet bao da nhan - ma tablet khong bao gio bao. Bang
     * dieu khien nhin thay canh do va go lenh nay thay ba.
     *
     * Vi sao khong dung [Lenh.CHO] cho gon: cau nhat ky. Le Hoa doc nhat ky tren man
     * hinh chinh, va "Xong viec nha (quet nha, rua chen): +20 phut" khac han "Ba Huy
     * cho 20 phut" - mot cai la cong minh lam ra, mot cai la qua nguoi lon cho.
     *
     * Ghi nhat ky va cong gio deu nho [ThiHanhViecNha.congGio], dung cai ham ma duong
     * binh thuong van dung, de hai duong khong de ra hai cau khac nhau.
     */
    private fun congViecNha(
        context: Context,
        gate: GateStore,
        phut: Int?,
        chu: String
    ): String {
        val bu = phut ?: return "Lệnh thiếu số phút, máy không cộng gì cả."
        if (bu !in 1..240) return "Số phút phải trong khoảng 1 đến 240."
        val ke = chu.ifBlank { "việc nhà" }
        // Doc trang thai TRUOC khi cong: cap mot phieu moi doi cong sang GRANTED, nen
        // hoi sau thi cau tra loi lai noi ve trang thai vua tao ra chu khong phai
        // trang thai luc nhan lenh.
        val dangChoi = gate.state == GateState.ACTIVE
        val duoc = ThiHanhViecNha.congGio(context, gate, bu, ke)
            ?: return khongCapDuoc(context)
        return if (dangChoi) {
            "Đang chơi nên cộng thẳng $bu phút, còn $duoc phút."
        } else {
            "Đã cộng $bu phút cho việc nhà."
        }
    }

    /**
     * Cho gio, tu Ba Huy hay tu ba noi.
     *
     * Chung mot ham vi phan viec that su lam thi y het nhau: cong vao phien dang
     * chay, hoac cap mot phieu moi khong tru han muc ngay. Chi khac hai cho, va ca
     * hai deu la ve ba noi: ba mot luot moi ngay, va cau ghi vao nhat ky phai noi
     * dung ten nguoi cho - thu do Le Hoa doc tren man hinh chinh.
     */
    private fun cho(
        context: Context,
        gate: GateStore,
        prefs: Prefs,
        phut: Int?,
        ai: String
    ): String {
        val xin = phut ?: prefs.grantMinutes
        val con = context.getString(R.string.child_name)
        val baNoi = ai == Nguoi.BA_NOI
        val nguoi = if (baNoi) "Bà nội" else "Ba Huy"

        if (baNoi && LuotBaNoi.daChoHomNay(context)) {
            return "Hôm nay bà cho một lần rồi, mai bà cho tiếp nhé."
        }

        /*
         * Tinh luot cua ba NGAY LUC NAY, truoc ca khi biet cap duoc hay khong.
         *
         * Qua gio chot ma khong tinh luot thi ba bam lai duoc - ma bam lai cung the,
         * van khong cap noi. Luc do ba ngoi bam mai mot cai nut khong bao gio chay.
         */
        if (baNoi) {
            LuotBaNoi.ghiNhanDaCho(context)
            Notifier.send(context, "Bà nội vừa bấm cho $con chơi $xin phút.")
        }

        if (gate.state == GateState.ACTIVE) {
            val conLai = gate.extend(xin)
            DayLog.add(context, "$nguoi cho thêm $xin phút giữa phiên")
            return "Đang chơi nên cộng thẳng $xin phút, còn ${conLai ?: 0} phút."
        }

        val duoc = gate.approve(wantedMinutes = xin, useQuota = false)
            ?: return khongCapDuoc(context)
        DayLog.add(context, "$nguoi cho $duoc phút")
        ApprovalService.ensureRunning(context)
        return "Đã cho $duoc phút (chưa tính giờ)."
    }

    /**
     * Sua ban cham theo ket qua Claude cham lai. Luat nam o [SuaCham].
     *
     * Cap gio TRUOC, ghi so SAU, y het duong AI tu cham. Cap khong duoc thi cau van
     * dang cho sua, va Ba Huy gui lai luc khac duoc.
     *
     * Ghi ca nhat ky lan loi nhan: Le Hoa doc ca hai tren man hinh chinh, va con can
     * biet la may da nham chu khong phai con tu dung nhien duoc them gio.
     */
    internal fun suaCham(context: Context, gate: GateStore, giaTri: Any?): String {
        val danhSach = (giaTri as? List<*>).orEmpty().mapNotNull { m ->
            val o = m as? Map<*, *> ?: return@mapNotNull null
            val ma = (o["ma"] as? String)?.trim().orEmpty()
            if (ma.isEmpty()) null else SuaCham.Cau(ma, (o["de"] as? String).orEmpty())
        }
        if (danhSach.isEmpty()) return "Lệnh thiếu danh sách câu, máy không sửa gì."

        val chuanBi = SuaCham.chuanBi(context, danhSach)
        if (chuanBi.cac.isEmpty()) {
            return "Không còn câu nào trong số đó đang chờ sửa, máy không cộng gì."
        }

        val phut = chuanBi.phut
        if (phut > 0) {
            val dangChoi = gate.state == GateState.ACTIVE
            val duoc = if (dangChoi) {
                gate.extend(phut, useQuota = true)
            } else {
                gate.approve(wantedMinutes = phut, useQuota = true)
            }
            if (duoc == null) return khongCapDuoc(context)
            if (!dangChoi) ApprovalService.ensureRunning(context)
        }

        val daGhi = SuaCham.ghi(context, chuanBi)
        runCatching { DongBo.daySoCai(context, daGhi) }

        val ke = daGhi.joinToString(", ") { it.ma }
        DayLog.add(
            context,
            "Ba Huy chấm lại câu $ke: con làm đúng" + if (phut > 0) ", +$phut phút" else ""
        )
        SoCaiBai.datLoiNhan(
            context,
            "Ba Huy chấm lại: câu $ke con làm đúng rồi, máy chấm nhầm." +
                if (phut > 0) " Được thêm $phut phút." else ""
        )
        val boQua = if (chuanBi.boQua.isEmpty()) "" else {
            " Bỏ qua ${chuanBi.boQua.joinToString(", ")} vì không còn chờ sửa."
        }
        return "Đã sửa câu $ke thành đúng" +
            (if (phut > 0) ", cộng $phut phút." else ", không có phút nào để cộng.") + boQua
    }

    /**
     * Cham mot bai dang cho theo ket qua Claude. Dung khi may chua cham bai do: tablet
     * tat cham AI, hay AI hong luc con nop.
     *
     * O day chi dung ban cham. Cap gio, ghi so va bao Telegram lam trong ApprovalService,
     * bang dung doan xu ly ban cham cua AI - xem [ApprovalService.chamTheoClaude]. Nen
     * cau tra loi o day chi noi da nhan, con so phut thi di tin Telegram.
     *
     * Chi cham bai con dang cho duyet. Bai Ba Huy da duyet tay hay tu choi thi thoi:
     * gio da cap roi, cham them la cap lan hai.
     */
    internal fun chamTheoClaude(
        context: Context,
        gate: GateStore,
        baiId: String?,
        giaTri: Any?
    ): String {
        val id = baiId?.trim().orEmpty()
        if (id.isEmpty()) return "Lệnh thiếu mã bài, máy không chấm."
        if (gate.baiDangCho().none { it.id == id }) {
            return "Bài này không còn chờ duyệt nên máy không chấm nữa."
        }
        val pham = KhaiChoCham.lay(context, id)
        val ket = ChamTheoClaude.banCham(context, giaTri, pham)
            ?: return "Lệnh thiếu danh sách câu, máy không chấm."
        ApprovalService.chamTheoClaude(context, id, ket, pham, ChamTheoClaude.coAnhDanDo(giaTri))
        return "Đã nhận kết quả Claude, tablet đang chấm. Số phút báo trên Telegram."
    }

    /**
     * Dua tin cua co giao vao kho tin tren tablet, roi bao len. Y het lenh /tinco ben
     * Telegram, chung ca cau tra loi.
     *
     * [luc] la luc Ba Huy bam gui tren dien thoai. Lenh nay duoc phep den muon - xem
     * cho bo lenh qua cu o [lam] - nen gio tablet nhan co the la sang hom sau, ma man
     * Tin cua co thi ghi gio gui.
     */
    internal fun tinCo(context: Context, chu: String, luc: Long): String {
        if (chu.isBlank()) return "Tin rỗng, không đưa lên."
        KhoTinCuaCo(context).them(chu, luc)
        ChuongTin.baoTinCuaCo(context)
        return "Đã đưa lên tablet rồi."
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
                "Tối đa mỗi ngày giờ là $v phút."
            }
            "gioNgu" -> {
                val v = so?.coerceIn(0, 24 * 60 - 1) ?: return "Thiếu giờ."
                prefs.hardStopMinuteOfDay = v
                // Bao thuc canh moc di ngu phai doi theo, khong thi no con danh
                // thuc theo gio cu cho den lan khoi dong may sau.
                vn.huytl.homeworkgate.guard.MocGio.datLai(context)
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
            "chamBangAi" -> {
                val v = giaTri as? Boolean ?: return "Giá trị không phải bật/tắt."
                prefs.chamBangAi = v
                if (v) {
                    "Đã bật lại chấm bằng AI trên tablet."
                } else {
                    "Đã tắt chấm bằng AI. Bài nộp sẽ chờ Ba Huy chấm bằng Claude."
                }
            }
            "appChoPhep" -> {
                prefs.allowedPackages = danhSach(giaTri)
                "Danh sách app luôn được dùng: ${prefs.allowedPackages.size} app."
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
