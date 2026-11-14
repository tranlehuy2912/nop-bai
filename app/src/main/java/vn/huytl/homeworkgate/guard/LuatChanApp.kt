package vn.huytl.homeworkgate.guard

import android.content.Context
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.KhoangHoc
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.TinhLoiNhac

/**
 * Bo luat Ba Huy dat ra cho tung app: luc nay app nay co duoc dung khong, va neu khong thi bao
 * Le Hoa cau gi. Tach khoi GuardAccessibilityService ngay 14/11/2026, khi anh Huy chot cho Jokers
 * lam lop chan chinh (tam dung app, xem [KhoaJokers]) con tro nang lam lop du phong: hai lop phai
 * xet cung mot bo luat, lech mot dong la app bi Jokers tam dung ma tro nang lai cho mo, hay nguoc
 * lai, va khong ai biet lop nao dung.
 *
 * Chi gom cac luat theo danh sach va theo gio. Cac mien tru theo man hinh (chinh app nay, man hinh
 * chinh, ban phim, app he thong, luc Ba Huy dang bam di bat quyen) ben goi tu xet truoc: tro nang
 * xet theo cua so dang hien, Jokers loai han cac app do khoi danh sach co the tam dung.
 *
 * Thu tu cac luat la thu tu cu cua GuardAccessibilityService.lyDoChan, giu nguyen tung chu.
 */
object LuatChanApp {

    /**
     * Ly do chan [pkg] (tieu de, chi tiet), hoac null neu duoc dung.
     *
     * [mo] la con gio choi khong. Tro nang truyen `gate.isOpen()`. Jokers truyen them luc dang tu
     * dung vi tat man hinh, xem [KhoaJokers.moChoJokers]. [hoc] la khoang gio di hoc dang bi chan
     * ([TinhLoiNhac.khoangDangChan]), truyen vao de hai ben goi tinh mot lan cho ca loat app.
     */
    fun lyDo(
        context: Context,
        pkg: String,
        prefs: Prefs,
        gate: GateStore,
        mo: Boolean,
        hoc: KhoangHoc?,
        tenApp: (String) -> String,
    ): Pair<String, String>? {
        if (pkg in GuardAccessibilityService.STORE_PACKAGES) {
            return "Không cài app mới được" to "Ba Huy mở máy hộ nếu Lê Hòa cần app học"
        }
        if (prefs.lockSystemSettings && pkg in GuardAccessibilityService.GUARDED_SYSTEM_PACKAGES) {
            return "Cài đặt đang khoá" to "Ba Huy mở bằng mã PIN trong app Nộp bài"
        }
        if (pkg in prefs.blockedPackages) {
            return "App này không được dùng" to
                "Ba Huy đã cấm app này, kể cả trong giờ chơi"
        }

        // Han rieng cua app xet truoc ca gio choi: Netflix mot ngay mot tieng ruoi la
        // mot tieng ruoi, du hom do co gio choi hay khong, du app co nam trong danh
        // sach duoc dung khi het gio hay khong.
        val han = GioiHanApp.han(context, pkg)
        if (han > 0 && GioiHanApp.daDungMs(context, pkg) >= han * 60_000L) {
            return "Hết giờ ${tenApp(pkg)} hôm nay" to
                "Mỗi ngày chỉ $han phút. Mai xem tiếp nhé."
        }

        // App dung moi luc xet sau danh sach cam va han rieng, truoc moi luat ve gio: gio
        // di hoc, gio choi, gio ngu. Ba Huy dat cho Telegram, de Le Hoa nhan cho ba duoc
        // ca luc bi khoa. Xem [Prefs.moiLucPackages].
        if (pkg in prefs.moiLucPackages) return null

        // Gio di hoc xet truoc ca gio choi.
        //
        // Bao "Het gio roi" luc nay la noi sai viec: gio choi van con nguyen, dang
        // duoc giu lai cho tan hoc. Cai dang xay ra la den luc phai buong may di
        // hoc - va do cung la cau ma man chan dang hien ngay sau lop phu nay.
        //
        // Chan ca app trong danh sach duoc dung khi het gio: man chan von da che
        // kin man hinh, mo rieng tu dien ra luc dang phai ra khoi nha thi vo nghia.
        //
        // Tu 14/11/2026 gio chan la gio Ba Huy dat cho tung thu ([vn.huytl.homeworkgate.data.KhoaHoc]),
        // nen "Máy mở lại lúc" la gio mo Ba Huy dat chu khong phai gio tan buoi trong lich.
        hoc?.let { k ->
            val buoi = k.buoi?.let {
                TinhLoiNhac.moTaBuoi(it).replaceFirstChar { c -> c.uppercase() } +
                    ", vào học ${TinhLoiNhac.gioPhut(it.phutVaoHoc)}. "
            }.orEmpty()
            return "Tới giờ đi học rồi" to "${buoi}Máy mở lại lúc ${TinhLoiNhac.gioPhut(k.toi)}."
        }

        if (mo) return null

        // Gio ngu khoa ca danh sach trang.
        //
        // Truoc day danh sach trang mo ca dem, vi no duoc nghi cho tu dien va may
        // tinh. Nhung trong do con co app AI, ma so phut rieng cua app lai tinh lai
        // luc 0 gio: con thuc den 00:05 la co them nua tieng moi. Ba Huy chon khoa.
        //
        // Dat SAU gate.isOpen(): gio choi con thi moi app deu mo. Hien gio thi ba
        // cung khong cap duoc gio trong gio ngu, nhung neu mai sau cap duoc thi do
        // la y cua ba, khong phai cho de chan.
        if (gate.trongGioNgu()) {
            return "Tới giờ ngủ rồi" to
                "Máy mở lại lúc ${TinhLoiNhac.gioPhut(prefs.gioDayMinuteOfDay)}. Mai chơi tiếp nhé."
        }

        if (pkg in prefs.allowedPackages) return null

        // Bao dung viec dang can lam. Truoc day luc nao cung "Nop bai tap de duoc
        // choi tiep" - het luot roi ma van bao nop bai thi con nop, ba tu choi, va
        // khong ai hieu tai sao. Dang tam dung hay da duyet thi viec can lam cung
        // khong phai nop bai.
        val detail = when {
            // Dang cho duyet bai moi ma trong tay con phieu chua dung thi choi duoc
            // ngay, khong phai cho. Bao "dang cho duyet" o day la con ngoi cho oan.
            gate.state == GateState.PENDING && gate.grantedMinutes > 0 ->
                "Đang giữ ${gate.grantedMinutes} phút. Mở app Nộp bài, bấm Bắt đầu chơi"
            gate.state == GateState.PENDING -> "Đã gửi bài rồi, đang chờ ba Huy duyệt"
            gate.state == GateState.PAUSED -> "Đang tạm dừng. Mở app Nộp bài, bấm Chơi tiếp"
            gate.state == GateState.GRANTED -> "Ba Huy duyệt rồi. Mở app Nộp bài, bấm Bắt đầu"
            gate.phutConLaiHomNay() <= 0 -> "Hôm nay đủ giờ chơi rồi, mai nộp bài tiếp nhé"
            else -> "Nộp bài tập để được chơi tiếp nhé"
        }
        return "Hết giờ rồi" to detail
    }
}
