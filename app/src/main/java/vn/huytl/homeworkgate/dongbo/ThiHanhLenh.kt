package vn.huytl.homeworkgate.dongbo

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.BaiCho
import vn.huytl.homeworkgate.data.CauChuaRo
import vn.huytl.homeworkgate.data.ChamTheoClaude
import vn.huytl.homeworkgate.data.CongSang
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.KhaiChoCham
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.SuaCham
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.guard.CatMangVpn
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
         * May ba noi khong con go duoc lenh nao (tu 26/9/2026, app ba chi con viec
         * nha). Luat ben firestore.rules da dong cua lenh/ cua uidsPhu. Cho nay chan
         * lan hai, cho truong hop luat bi dan de len bang ban cu trong console
         * Firebase: luat thi sua bang tay o mot cho khong ai nhin thay, con dong nay
         * thi di theo ban app.
         */
        if (ai == Nguoi.BA_NOI) {
            return "Máy bà nội không gửi lệnh được nữa."
        }

        val gate = GateStore(context)
        val prefs = Prefs.get(context)
        val con = context.getString(R.string.child_name)

        return when (kieu) {
            Lenh.DUYET -> duyet(context, gate, baiId, phut)

            Lenh.TU_CHOI -> tuChoi(context, gate, baiId, chu)

            // Gio thuong: khong tru vao han muc ngay, vi day la nguoi lon chu dong
            // cho chu khong phai con doi bang bai tap.
            Lenh.CHO -> cho(context, gate, phut)

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

            Lenh.TIN_CO -> tinCo(context, chu, taoLuc)

            Lenh.CAI_DAT -> doiCaiDat(context, chu, d.get("giaTri"))

            Lenh.SUA_CHAM -> suaCham(context, gate, d.get("giaTri"))

            Lenh.CHAM_BAI -> chamTheoClaude(context, gate, baiId, d.get("giaTri"))

            Lenh.XU_CAU -> xuCau(context, gate, baiId, d.get("giaTri"))

            Lenh.BO_SUA -> boSua(context, d.get("giaTri"))

            Lenh.CAP_QUY -> {
                val kq = vn.huytl.homeworkgate.data.QuyGio.cap(context, phut?.takeIf { it > 0 })
                kq.loi ?: "Đã cấp ${kq.cap} phút từ quỹ giờ chơi, quỹ còn ${kq.conLai} phút."
            }

            Lenh.DOC_VO -> docVo(context)

            Lenh.MO_DE_THI -> moDeThi(context, chu)

            // Ben kia vua mo app va hoi tablet con song khong. Day mot ban trang
            // thai day du roi thoi: khong ghi nhat ky, khong tra loi gi. Ban trang
            // thai do chinh la cau tra loi, va no den qua duong khac.
            //
            // Day kem so dung app. So do chi di vao luc nay, xem [DongBo.daySuDung].
            Lenh.PING -> {
                DongBo.dayDayDu()
                DongBo.daySuDung(context)
                ""
            }

            else -> "Không hiểu lệnh $kieu."
        }
    }

    /**
     * Bai ma lenh nhac toi, neu no con trong hang cho.
     *
     * Lenh co ma bai thi chi tim dung bai do. Truoc day khong thay thi lay bai cu nhat
     * dang cho, nen bam Duyet o mot bai may da bo (bai nop hom qua, bai con da huy) la
     * duyet nham bai khac voi so phut cua bai kia. Lay bai cu nhat chi con danh cho ban
     * Bang dieu khien cu khong gui ma bai.
     */
    private fun baiTrongHang(gate: GateStore, baiId: String?): BaiCho? {
        val hang = gate.baiDangCho()
        return if (baiId == null) hang.firstOrNull() else hang.firstOrNull { it.id == baiId }
    }

    /**
     * Khong duyet mot bai.
     *
     * Bai khong con trong hang cho thi van ghi TUCHOI len Firestore, neu ben do con ghi
     * CHO. Sang ngay moi, GateStore.tick bo bai nop hom truoc khoi hang cho ma khong bao
     * dien thoai, nen Bang dieu khien van ghi bai do la dang cho. Truoc day bam Khong
     * duyet o bai do chi duoc cau "Khong co bai nao dang cho", con bai thi van nam nguyen
     * o do.
     *
     * Ly do [chu] ghi vao bai de the bai o man Bai da cham hien cho Le Hoa (30/9/2026).
     */
    internal fun tuChoi(context: Context, gate: GateStore, baiId: String?, chu: String): String {
        val bai = baiTrongHang(gate, baiId)
        if (bai == null) {
            if (baiId == null) return "Không có bài nào đang chờ."
            DongBo.datTrangThaiBaiNeuDangCho(context, baiId, "TUCHOI", lyDo = chu)
            return "Bài đó không còn trong hàng chờ của tablet. Đã gỡ khỏi danh sách chờ duyệt."
        }
        gate.boBaiCho(bai.id)
        goNutBenTelegram(context, bai.messageId)
        DongBo.datTrangThaiBai(context, bai.id, "TUCHOI", lyDo = chu)
        DayLog.add(context, "Ba Huy không duyệt" + if (chu.isBlank()) "" else ": $chu")
        return "Đã từ chối bài đó."
    }

    /**
     * Duyet mot bai.
     *
     * Dang choi ma duyet them thi cong thang vao phien dang chay chu khong cat phien
     * roi cap lai tu dau - y het duong Telegram.
     */
    internal fun duyet(context: Context, gate: GateStore, baiId: String?, phut: Int?): String {
        val bai = baiTrongHang(gate, baiId)
            ?: return "Bài đó không còn trong hàng chờ nữa. Muốn cho giờ thì bấm Cho chơi ngay."

        val xin = phut ?: return "Lệnh thiếu số phút, máy không duyệt gì cả."

        // Khong con tran ngay chan nut Duyet (29/9/2026): Ba Huy duyet bao nhieu la bay
        // nhieu. So phut may de nghi tren nut da qua tran rieng cua bai dan do.
        val them = xin
        val catBot = ""

        if (gate.state == GateState.ACTIVE) {
            val conLai = gate.extend(them, useQuota = true) ?: return khongCapDuoc(context)
            gate.boBaiCho(bai.id)
            goNutBenTelegram(context, bai.messageId)
            DongBo.datTrangThaiBai(context, bai.id, "DUYET", them)
            vn.huytl.homeworkgate.data.GiaiDe.baDuyetBai(context, bai.id)
            DayLog.add(context, "Duyệt $them phút giữa phiên")
            return "Đang chơi nên cộng thẳng $them phút, còn $conLai phút.$catBot"
        }

        val duoc = gate.approve(wantedMinutes = them, useQuota = true, requestId = bai.id)
            ?: return khongCapDuoc(context)

        goNutBenTelegram(context, bai.messageId)
        DongBo.datTrangThaiBai(context, bai.id, "DUYET", them)
        vn.huytl.homeworkgate.data.GiaiDe.baDuyetBai(context, bai.id)
        DayLog.add(context, "Duyệt $duoc phút (Bảng điều khiển)")
        ApprovalService.ensureRunning(context)
        // Con dang giu phieu cu ma nop them bai thi duyet la cong don, khong de len.
        return (if (duoc > them) "Đã duyệt thêm $them phút, cộng dồn thành $duoc phút."
        else "Đã duyệt $duoc phút.") + catBot
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
     * Ba Huy cho gio: cong vao phien dang chay, hoac cap mot phieu moi khong tru han
     * muc ngay. Lenh phai kem so phut: so phut mac dinh da bo (27/9/2026).
     */
    private fun cho(context: Context, gate: GateStore, phut: Int?): String {
        val xin = phut ?: return "Lệnh thiếu số phút, máy không cho gì cả."
        val nguoi = "Ba Huy"

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
     * dang cho sua, va Ba Huy gui lai luc khac duoc. Rieng gio ngu thi ghi so ngay va giu
     * so phut toi luc het gio ngu, xem [CongSang].
     *
     * Ghi ca nhat ky lan loi nhan: Le Hoa doc ca hai tren man hinh chinh, va con can
     * biet la may da nham chu khong phai con tu dung nhien duoc them gio.
     */
    internal fun suaCham(context: Context, gate: GateStore, giaTri: Any?): String {
        val danhSach = (giaTri as? List<*>).orEmpty().mapNotNull { m ->
            val o = m as? Map<*, *> ?: return@mapNotNull null
            val ma = (o["ma"] as? String)?.trim().orEmpty()
            // soDong: so dong Claude ghi o lan cham lai, Bang dieu khien gui kem tu 29/9/2026.
            // Firestore tra so la Long.
            val soDong = (o["soDong"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
            if (ma.isEmpty()) null else SuaCham.Cau(ma, (o["de"] as? String).orEmpty(), soDong)
        }
        if (danhSach.isEmpty()) return "Lệnh thiếu danh sách câu, máy không sửa gì."

        val chuanBi = SuaCham.chuanBi(context, danhSach)
        if (chuanBi.cac.isEmpty() && chuanBi.choSoDong.isEmpty()) {
            return "Không còn câu nào trong số đó đang chờ sửa, máy không cộng gì."
        }
        val con = context.getString(R.string.child_name)
        val boQua = if (chuanBi.boQua.isEmpty()) "" else {
            " Bỏ qua ${chuanBi.boQua.joinToString(", ")} vì không còn chờ sửa."
        }

        // Cau dung ma chua co so dong: khong ghi so, van cho sua (29/9/2026). Ghi xong voi 0
        // phut thi cau do khong bao gio duoc tra gio nua, xem SuaCham.
        val keThieu = chuanBi.choSoDong.joinToString(", ") { it.ma }
        val choDong = if (keThieu.isEmpty()) "" else {
            DayLog.add(context, "Ba Huy chấm lại câu $keThieu: đúng nhưng chưa có số dòng, câu vẫn chờ sửa")
            " Câu $keThieu đúng nhưng chưa có số dòng nên máy chưa cộng giờ, câu vẫn chờ sửa. " +
                "Dán lại kết quả Claude có số dòng bằng bản Bảng điều khiển mới."
        }
        if (chuanBi.cac.isEmpty()) return choDong.trim() + boQua

        // Ba Huy cham lai trong gio ngu (Ba Huy chon ngay 27/9/2026): ghi so ngay, phut thi
        // giu toi luc het gio ngu. Tran ngay xet luc cong, xem CongSang.
        val xin = chuanBi.phut
        if (xin > 0 && gate.trongGioNgu()) {
            val daGhi = SuaCham.ghi(context, chuanBi)
            runCatching { DongBo.daySoCai(context, daGhi) }
            CongSang.them(context, xin)
            val ke = daGhi.joinToString(", ") { it.ma }
            val gio = CongSang.gioCong(context)
            DayLog.add(context, "Ba Huy chấm lại câu $ke: $con làm đúng, giữ $xin phút tới $gio")
            SoCaiBai.datLoiNhan(
                context,
                "Ba Huy chấm lại: câu $ke $con làm đúng rồi, máy chấm nhầm. " +
                    "Hết giờ ngủ lúc $gio thì được thêm $xin phút."
            )
            return "Đã sửa câu $ke thành đúng. Đang giờ ngủ nên giữ $xin phút, $gio tablet cộng.$choDong$boQua"
        }

        // Tinh truoc so phut con vao duoc tran ngay, y nhu [duyet]: GateStore cat bot ma
        // khong bao ai, con cau tra loi, nhat ky va so thi phai noi so phut con nhan that.
        // So phut cua cac cau sua da qua tran rieng cua bai dan do trong [SuaCham]; khong
        // con tran ngay de cat them (29/9/2026).
        val phut = xin
        if (xin > 0) {
            if (phut <= 0) return khongCapDuoc(context)
            val dangChoi = gate.state == GateState.ACTIVE
            val duoc = if (dangChoi) {
                gate.extend(phut, useQuota = true)
            } else {
                gate.approve(wantedMinutes = phut, useQuota = true)
            }
            if (duoc == null) return khongCapDuoc(context)
            if (!dangChoi) ApprovalService.ensureRunning(context)
        }

        val daGhi = SuaCham.ghi(context, chuanBi, daCap = phut)
        runCatching { DongBo.daySoCai(context, daGhi) }

        val ke = daGhi.joinToString(", ") { it.ma }
        DayLog.add(
            context,
            "Ba Huy chấm lại câu $ke: $con làm đúng" + if (phut > 0) ", +$phut phút" else ""
        )
        SoCaiBai.datLoiNhan(
            context,
            "Ba Huy chấm lại: câu $ke $con làm đúng rồi, máy chấm nhầm." +
                if (phut > 0) " Được thêm $phut phút." else ""
        )
        val catBot = if (phut in 1 until xin) " Hôm nay chỉ còn $phut phút trong hạn mức." else ""
        return "Đã sửa câu $ke thành đúng" +
            (if (phut > 0) ", cộng $phut phút." else ", không có phút nào để cộng.") + catBot + choDong + boQua
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
        ApprovalService.chamTheoClaude(context, id, ket, pham)
        return "Đã nhận kết quả Claude, tablet đang chấm. Số phút báo trên Telegram."
    }

    /**
     * Ba Huy mo mot de thi in san cho con, xem [Lenh.MO_DE_THI]. De mo ra nam o trang Luyen
     * tap nhu moi de Giai de; con bam vao, bam Bat dau thi dong ho moi chay.
     */
    internal fun moDeThi(context: Context, ma: String?): String {
        val maDe = ma?.trim().orEmpty()
        if (maDe.isEmpty()) return "Lệnh thiếu mã đề, máy không mở đề nào."
        val kq = vn.huytl.homeworkgate.data.GiaiDe.moDeThi(context, maDe, choBa = true)
        val de = kq.de ?: return kq.loi ?: "Máy chưa mở được đề $maDe."
        val con = context.getString(R.string.child_name)
        return if (de.daBatDau) "$con đang làm ${de.ten}."
        else "Đã mở ${de.ten} (${de.cauIds.size} câu, ${de.phutGoiY} phút). $con vào Luyện tập để làm."
    }

    /**
     * Ba Huy bo cau sai khoi danh sach can sua cua con, khong cong phut. Xem [Lenh.BO_SUA]
     * va [vn.huytl.homeworkgate.data.BoSua].
     *
     * Ghi nhat ky de Le Hoa (va Ba Huy khi xem lai) biet dong "câu cần sửa" ngan di vi dau.
     */
    internal fun boSua(context: Context, giaTri: Any?): String {
        val danhSach = (giaTri as? List<*>).orEmpty().mapNotNull { m ->
            val o = m as? Map<*, *> ?: return@mapNotNull null
            val ma = (o["ma"] as? String)?.trim().orEmpty()
            if (ma.isEmpty()) null else ma to (o["de"] as? String).orEmpty()
        }
        if (danhSach.isEmpty()) return "Lệnh thiếu danh sách câu, máy không bỏ câu nào."
        val kq = vn.huytl.homeworkgate.data.BoSua.boTheoMa(context, danhSach)
        val khongThay = if (kq.khongThay.isEmpty()) "" else {
            " Câu ${kq.khongThay.joinToString(", ")} không còn chờ sửa."
        }
        if (kq.daBo.isEmpty()) return "Không có câu nào để bỏ.$khongThay"
        val ke = kq.daBo.joinToString(", ")
        DayLog.add(context, "Ba Huy bỏ câu $ke khỏi danh sách cần sửa")
        return "Đã bỏ câu $ke khỏi danh sách cần sửa, không cộng phút.$khongThay"
    }

    /**
     * Ba Huy tu xu cac cau Claude doc chua chac hay khong ghi so dong. Xem [Lenh.XU_CAU].
     *
     * Bang dieu khien gui lai nguyen ban cham cua Claude, cau nao Ba Huy xu thi da doi: chac
     * la true, dung va soDong theo Ba Huy, cau can chup lai thi chupLai la true. Nen o day di
     * dung duong [chamTheoClaude]: mot luat tinh phut, mot cho ghi so. Cau chup lai thi nhan
     * con chup lai, khong tinh la sai.
     */
    internal fun xuCau(
        context: Context,
        gate: GateStore,
        baiId: String?,
        giaTri: Any?
    ): String {
        val cac = ((giaTri as? Map<*, *>)?.get("cac") ?: giaTri) as? List<*>
        val chupLai = cac.orEmpty().mapNotNull { it as? Map<*, *> }
            .filter { it["chupLai"] as? Boolean == true }
            .mapNotNull { (it["ma"] as? String)?.trim()?.takeIf { m -> m.isNotEmpty() } }
        hienCauChupLai(context, gate, baiId, giaTri, chupLai)
        val tra = chamTheoClaude(context, gate, baiId, giaTri)
        if (chupLai.isNotEmpty()) {
            SoCaiBai.datLoiNhan(
                context,
                "${context.getString(R.string.parent_name_cap)} nhờ chụp lại câu " +
                    chupLai.joinToString(", ") + " cho rõ rồi nộp lại."
            )
        }
        DayLog.add(context, "Ba Huy tự xử câu chưa chắc của bài" +
            if (chupLai.isEmpty()) "" else ", nhờ chụp lại ${chupLai.joinToString(", ")}")
        return tra
    }

    /**
     * Cau Ba Huy nho chup lai ([chupLai], ma Claude) thoi an khoi danh sach can sua cua con, xem
     * [CauChuaRo]: con thay cau do va bam nop lai tren the bai. Chi khi bai con cho, tuc la lenh
     * XU_CAU con duoc cham. Tach rieng de kiem thu goi thang, khong phai bat ApprovalService.
     */
    internal fun hienCauChupLai(
        context: Context,
        gate: GateStore,
        baiId: String?,
        giaTri: Any?,
        chupLai: List<String>
    ) {
        val id = baiId?.trim().orEmpty()
        if (chupLai.isEmpty() || gate.baiDangCho().none { it.id == id }) return
        ChamTheoClaude.banCham(context, giaTri, KhaiChoCham.lay(context, id))?.cac.orEmpty()
            .filter { it.maGoc.ifBlank { it.ma }.trim() in chupLai }
            .forEach { CauChuaRo.bo(context, SoCaiBai.khoaCua(it)) }
    }

    /**
     * Ket qua Claude doc vo dan do, tu Bang dieu khien ban cu. Xem [Lenh.DOC_VO].
     *
     * Tu 30/9/2026 may doc khong duoc thi con tu go ngay tren tablet, khong con tam vo nao cho
     * Ba Huy nho Claude doc, nen lenh nay khong con cho nao de ghi. Tra loi cho ro thay vi im
     * lang, de Ba Huy biet vi sao.
     */
    internal fun docVo(context: Context): String =
        "Tablet không còn nhận kết quả đọc vở: từ 30/9/2026 máy đọc không được thì " +
            "${context.getString(R.string.child_name)} tự gõ vở dặn dò trên tablet."

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

    /**
     * Vi sao khong cap duoc. Trong gio ngu thi noi rieng: luc do Cho choi ngay cung khong
     * cap duoc, ma cau cu van bao bam no.
     */
    private fun khongCapDuoc(context: Context): String {
        val prefs = Prefs.get(context)
        val tu = prefs.hardStopMinuteOfDay
        val den = prefs.gioDayMinuteOfDay
        val gate = GateStore(context)
        if (gate.trongGioNgu()) {
            return "Không cấp được: đang trong giờ ngủ " +
                "%02d:%02d-%02d:%02d.".format(tu / 60, tu % 60, den / 60, den % 60)
        }
        return "Không cấp được lúc này, tablet vừa đổi trạng thái. Bấm lại lần nữa."
    }

    /**
     * Doi mot muc cau hinh.
     *
     * Dien thoai khong ghi thang vao ban sao cau hinh tren Firestore; no gui lenh
     * qua day. Nho the, moi gia tri deu di qua dung mot cho kiem tra, va con so hien
     * ben kia luon la con so that dang chay chu khong phai con so vua mong muon.
     */
    internal fun doiCaiDat(context: Context, ten: String, giaTri: Any?): String {
        val prefs = Prefs.get(context)
        val so = (giaTri as? Number)?.toInt()

        val cau = when (ten) {
            // Bang dieu khien ban cu con dong "Moi lan duyet". So phut mac dinh da bo
            // (27/9/2026): nut duyet nao cung ghi so phut cu the.
            "phutMacDinh" -> return "Số phút mặc định đã bỏ, nút duyệt nào cũng ghi số phút."
            // Bang dieu khien ban cu con dong "Tối đa mỗi ngày". Tran ngay da bo (29/9/2026):
            // moi phan co tran rieng, khong con mot so chung de chinh.
            "tranPhutMoiNgay" -> return "Trần mỗi ngày đã bỏ: mỗi phần có trần riêng, tối đa ${LuatCongGio.TRAN_NGAY} phút một ngày."
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
            // Bang dieu khien ban cu con cong tac "Chấm bài bằng AI trên tablet". Tu
            // 28/9/2026 tablet khong con may cham nao de bat: bai nao cung do Claude cham.
            "chamBangAi" -> return "Tablet không còn tự chấm, bài nào cũng chấm bằng Claude."
            "appChoPhep" -> {
                prefs.allowedPackages = danhSach(giaTri)
                "Danh sách app dùng khi hết giờ chơi: ${prefs.allowedPackages.size} app."
            }
            "appMoiLuc" -> {
                prefs.moiLucPackages = danhSach(giaTri)
                "Danh sách app dùng mọi lúc: ${prefs.moiLucPackages.size} app."
            }
            "appChan" -> {
                prefs.blockedPackages = danhSach(giaTri)
                "Danh sách app chặn hẳn: ${prefs.blockedPackages.size} app."
            }
            // Bang dieu khien co muc nay tu 27/9/2026. Truoc do chi sua tren tablet.
            "appNhac" -> {
                prefs.nhacPackages = danhSach(giaTri)
                "Danh sách app được nghe nền: ${prefs.nhacPackages.size} app."
            }
            // Gui lai danh sach cung la cach Ba Huy bat lai VPN vua bi app khac chiem,
            // nen quen dau bi da va xet lai ngay ca khi danh sach khong doi.
            "appCatMang" -> {
                prefs.catMangPackages = danhSach(giaTri)
                CatMangVpn.boCoBiDa(context)
                CatMangVpn.dongBo(context)
                val so = prefs.catMangPackages.size
                if (so > 0 && !CatMangVpn.daChoPhep(context)) {
                    "Danh sách app cắt mạng khi bị khoá: $so app. Tablet chưa cho phép " +
                        "VPN nên chưa cắt được: vào Cài đặt của app Nộp bài trên tablet, " +
                        "bấm dòng cảnh báo VPN, bấm Xong rồi chọn OK."
                } else {
                    "Danh sách app cắt mạng khi bị khoá: $so app."
                }
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
