package vn.huytl.homeworkgate.data

import android.content.Context

/**
 * "Quỹ giờ chơi": phut con lam ra tren may ma tran rieng cua phan do da day.
 *
 * VI SAO CO. Tu 29/9/2026 moi phan co tran rieng (lam bai tren may 90, on lai 30, Kiem
 * tra bai 20, Do tu vung 30 phut moi ngay) va khong con tran chung. Truoc day phan vuot
 * tran mat han, nen lam them sau khi day tran la lam khong. Ba Huy chon ghi lai phan do
 * vao mot quy: con thay con so tren tablet, con cap bao nhieu tu quy thi Ba Huy quyet,
 * bang nut tren Bang dieu khien hay lenh /quy tren Telegram.
 *
 * CHI PHAN LAM TREN MAY. Duong chup anh (vo dan do, tron goi 45 phut) khong bao gio vao
 * quy: phan do tinh theo bai co giao, khong phai theo cong con lam them.
 *
 * KHONG HET HAN, KHONG TINH VAO TRAN NGAY khi cap: day la Ba Huy chu dong cho, giong
 * lenh CHO. Phut da cap chua choi het toi gio ngu thi van mat nhu moi phieu khac, khong
 * quay lai quy - Ba Huy chon chi gom phut bi tran cat.
 */
object QuyGio {

    fun so(context: Context): Int = Prefs.get(context).quyGio

    /**
     * Cong [phut] vao quy, kem mot dong nhat ky noi phut do tu dau ra ("Kiểm tra bài",
     * "Làm bài trên máy"). Khoa lai de hai luong cung cong mot luc khong nuot mat nhau.
     */
    fun them(context: Context, phut: Int, tuDau: String) {
        if (phut <= 0) return
        synchronized(this) {
            val p = Prefs.get(context)
            p.quyGio = p.quyGio + phut
        }
        DayLog.add(context, "Vào quỹ giờ chơi $phut phút ($tuDau vượt trần hôm nay)")
    }

    /** Ket qua mot lan cap tu quy. [cap] la so phut da cap, 0 khi khong cap duoc. */
    data class KetQua(val cap: Int, val conLai: Int, val loi: String? = null)

    /**
     * Ba Huy cap [xin] phut tu quy; null la cap het. Tru quy SAU khi cong da nhan phut, y
     * het cac duong cap gio khac: cap hong (gio ngu) thi quy giu nguyen.
     */
    fun cap(context: Context, xin: Int?): KetQua = synchronized(this) {
        val p = Prefs.get(context)
        val co = p.quyGio
        if (co <= 0) return KetQua(0, 0, "Quỹ giờ chơi đang trống.")
        val phut = (xin ?: co).coerceIn(1, co)
        val gate = GateStore(context)
        val duoc = if (gate.state == GateState.ACTIVE) {
            gate.extend(phut)
        } else {
            gate.approve(wantedMinutes = phut, useQuota = false, nhanCho = "Quỹ giờ chơi")
        }
        if (duoc == null) return KetQua(0, co, "Chưa cấp được: đang giờ ngủ hoặc đã quá giờ chốt.")
        p.quyGio = co - phut
        DayLog.add(context, "Ba Huy cấp $phut phút từ quỹ giờ chơi, quỹ còn ${co - phut} phút")
        KetQua(phut, co - phut)
    }
}
