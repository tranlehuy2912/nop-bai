package vn.huytl.homeworkgate.data

import java.util.Calendar

enum class Buoi { SANG, CHIEU }

/** Mot buoi hoc co that trong tuan. */
data class BuoiHoc(
    val thu: Int,
    val buoi: Buoi,
    /** So tiet -> ten mon, chi chua nhung tiet co hoc. */
    val monTheoTiet: Map<Int, String>
) {
    val tietDau: Int get() = monTheoTiet.keys.min()
    val tietCuoi: Int get() = monTheoTiet.keys.max()

    /** Phut tinh tu 00:00, luc chuong vao tiet dau. */
    val phutVaoHoc: Int
        get() = ThoiKhoaBieu.gioTiet(buoi, tietDau)

    /** Phut tinh tu 00:00, luc tan buoi. */
    val phutTanHoc: Int
        get() = ThoiKhoaBieu.gioTiet(buoi, tietCuoi) + ThoiKhoaBieu.PHUT_MOI_TIET

    /** Cac mon can mang vo, da bo nhung mon khong dung vo. */
    val monCanSoan: List<String>
        get() = monTheoTiet.entries.sortedBy { it.key }
            .map { it.value }
            .distinct()
            .filterNot { it in ThoiKhoaBieu.MON_KHONG_CAN_VO }

    /** Co tiet the duc thi phai mac do the duc, khong phai mang vo. */
    val coTheDuc: Boolean
        get() = monTheoTiet.values.any { it == ThoiKhoaBieu.MON_THE_DUC }
}

/**
 * Thoi khoa bieu cua Le Hoa: tiet nao hoc mon gi, gio vao tung tiet, moc buong may.
 *
 * TU 4/10/2026 SO LIEU KHONG NAM O DAY NUA. Lich lay tu [LichDangDung.ban], tuc ban nhan tu
 * Firestore hay ban mang san trong app ([LichMacDinh]), xem [BanLich]. Doi lich thi Claude
 * Code ghi ban moi len Firestore, khong sua file nay, khong cai lai app. Object nay chi con
 * cach dung so lieu: buoi nao, may gio, mon nao can mang vo.
 *
 * Code chi giu mot ban lich: vo dan do cu tinh han theo ban dang dung, dung voi nhung tiet
 * tu ngay lich do ap dung tro di.
 *
 * Bang dieu khien giu mot ban chep y het file nay, chi khac dong package, de ve tab
 * Lich hoc (tu 29/9/2026). Sua o day thi chep sang ban do va chay tools/kiem-duong.sh.
 */
object ThoiKhoaBieu {

    private val ban: BanLich get() = LichDangDung.ban

    val PHUT_MOI_TIET: Int get() = ban.phutMoiTiet

    const val MON_THE_DUC = "Giáo dục thể chất"

    /** Nhung mon khong phai mang vo di. */
    val MON_KHONG_CAN_VO = setOf("Chào cờ", MON_THE_DUC)

    /** Ten lop in tren man thoi khoa bieu, "8A15". */
    val lop: String get() = ban.lop

    fun gioTiet(buoi: Buoi, tiet: Int): Int = ban.gioTiet.getValue(buoi).getValue(tiet)

    /**
     * Cac tiet CO THAT cua mot buoi, ke ca tiet tuan nay khong ai hoc.
     *
     * Luoi thoi khoa bieu ve theo day nay chu khong theo cac tiet dang co mon: bo
     * tiet trong di thi mon o tiet 3 va 4 tut xuong thanh hai hang cuoi bang, nhin
     * ra "hoc hai tiet cuoi buoi sang" trong khi buoi sang con mot tiet nua o duoi.
     */
    fun cacTiet(buoi: Buoi): List<Int> = ban.gioTiet.getValue(buoi).keys.sorted()

    /**
     * Moc Le Hoa phai buong may di chuan bi, phut tinh tu 00:00.
     *
     * Khong tinh ra tu gio vao hoc bang mot cong thuc chung: Ba Huy dat tay tung moc, ghi
     * trong ban lich. Buoi chieu buong may luc 11:30, truoc gio vao hoc 75 phut, vi trua Le
     * Hoa hay om may lau; buoi sang co ghi moc (thu hai, thu sau 8:45) thi theo moc, khong
     * ghi thi 30 phut truoc gio vao hoc. Truoc ngay 24/9/2026 buoi chieu la 12:00. Truoc
     * ngay 27/9/2026 sang thu sau vao hoc tu tiet 1 nen buong may luc 6:45.
     */
    fun phutBuongMay(buoiHoc: BuoiHoc): Int = when (buoiHoc.buoi) {
        Buoi.CHIEU -> ban.buongMayChieu
        Buoi.SANG -> ban.buongMaySang[buoiHoc.thu] ?: (buoiHoc.phutVaoHoc - 30)
    }

    /**
     * Cac mon co hoc trong tuan, bo nhung mon khong co bai ve nha.
     *
     * Dung cho man con khai dang lam bai mon gi. Lay tu thoi khoa bieu chu khong go
     * tay mot danh sach thu hai: doi lop la sua mot cho, va con khong bao gio thay
     * mot mon no khong hoc.
     */
    fun tatCaMon(): List<String> =
        ban.monTheoBuoi.values
            .flatMap { it.values }
            .flatMap { it.values }
            .distinct()
            .filterNot { it in MON_KHONG_CAN_VO }
            .sorted()

    /** Cac mon hoc trong mot thu, de xep len dau danh sach cho con khoi phai tim. */
    fun monTrongNgay(thu: Int): List<String> =
        buoiHocCua(thu)
            .flatMap { it.monCanSoan }
            .distinct()

    /** Cac buoi hoc cua mot thu, theo thu tu trong ngay. */
    fun buoiHocCua(thu: Int): List<BuoiHoc> = buildList {
        ban.monTheoBuoi[Buoi.SANG]?.get(thu)?.let { add(BuoiHoc(thu, Buoi.SANG, it)) }
        ban.monTheoBuoi[Buoi.CHIEU]?.get(thu)?.let { add(BuoiHoc(thu, Buoi.CHIEU, it)) }
    }

    fun tenThu(thu: Int): String = when (thu) {
        Calendar.MONDAY -> "thứ hai"
        Calendar.TUESDAY -> "thứ ba"
        Calendar.WEDNESDAY -> "thứ tư"
        Calendar.THURSDAY -> "thứ năm"
        Calendar.FRIDAY -> "thứ sáu"
        Calendar.SATURDAY -> "thứ bảy"
        else -> "chủ nhật"
    }
}
