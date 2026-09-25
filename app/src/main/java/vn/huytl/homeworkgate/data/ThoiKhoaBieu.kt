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
 * Thoi khoa bieu lop 8A15, nam hoc 2026-2027, ap dung tu 07/09/2026.
 *
 * Nam thang trong code chu khong co man nhap: doi lich thi sua o day roi cai lai
 * app. Doi lai khong co o nhap nao de nhap sai, va go app cung khong mat gi.
 */
object ThoiKhoaBieu {

    const val PHUT_MOI_TIET = 45

    const val MON_THE_DUC = "Giáo dục thể chất"

    /** Nhung mon khong phai mang vo di. */
    val MON_KHONG_CAN_VO = setOf("Chào cờ", MON_THE_DUC)

    private val GIO_SANG = mapOf(
        1 to 7 * 60 + 15,
        2 to 8 * 60,
        3 to 9 * 60 + 15,
        4 to 10 * 60,
        5 to 10 * 60 + 45
    )

    private val GIO_CHIEU = mapOf(
        1 to 12 * 60 + 45,
        2 to 13 * 60 + 30,
        3 to 14 * 60 + 15,
        4 to 15 * 60 + 30,
        5 to 16 * 60 + 15
    )

    fun gioTiet(buoi: Buoi, tiet: Int): Int =
        (if (buoi == Buoi.SANG) GIO_SANG else GIO_CHIEU).getValue(tiet)

    /**
     * Cac tiet CO THAT cua mot buoi, ke ca tiet tuan nay khong ai hoc.
     *
     * Luoi thoi khoa bieu ve theo day nay chu khong theo cac tiet dang co mon: bo
     * tiet trong di thi mon o tiet 3 va 4 tut xuong thanh hai hang cuoi bang, nhin
     * ra "hoc hai tiet cuoi buoi sang" trong khi buoi sang con mot tiet nua o duoi.
     */
    fun cacTiet(buoi: Buoi): List<Int> =
        (if (buoi == Buoi.SANG) GIO_SANG else GIO_CHIEU).keys.sorted()

    private val BUONG_MAY_SANG = mapOf(
        Calendar.MONDAY to 8 * 60 + 45,
        Calendar.FRIDAY to 6 * 60 + 45
    )

    /**
     * Moc Le Hoa phai buong may di chuan bi, phut tinh tu 00:00.
     *
     * Khong tinh ra tu gio vao hoc bang mot cong thuc chung: Ba Huy dat tay tung moc.
     * Buoi chieu buong may luc 11:30, truoc gio vao hoc 75 phut, vi trua Le Hoa hay
     * om may lau; hai buoi sang thi 30. Truoc ngay 24/9/2026 buoi chieu la 12:00.
     */
    fun phutBuongMay(buoiHoc: BuoiHoc): Int = when (buoiHoc.buoi) {
        Buoi.CHIEU -> 11 * 60 + 30
        Buoi.SANG -> BUONG_MAY_SANG[buoiHoc.thu] ?: (buoiHoc.phutVaoHoc - 30)
    }

    private val SANG: Map<Int, Map<Int, String>> = mapOf(
        Calendar.MONDAY to mapOf(
            3 to "Giáo dục thể chất",
            4 to "Giáo dục thể chất"
        ),
        Calendar.FRIDAY to mapOf(
            1 to "AVNN",
            2 to "AVNN",
            3 to "Tin học",
            4 to "Tin học"
        )
    )

    private val CHIEU: Map<Int, Map<Int, String>> = mapOf(
        Calendar.MONDAY to mapOf(
            1 to "Tiếng Anh",
            2 to "Khoa học tự nhiên",
            3 to "Kỹ năng",
            4 to "Trải nghiệm hướng nghiệp",
            5 to "Chào cờ"
        ),
        Calendar.TUESDAY to mapOf(
            1 to "Trải nghiệm hướng nghiệp",
            2 to "Ngữ văn",
            3 to "Giáo dục công dân",
            4 to "Công nghệ",
            5 to "Giáo dục địa phương"
        ),
        Calendar.WEDNESDAY to mapOf(
            1 to "Mỹ thuật",
            2 to "STEM",
            3 to "Toán",
            4 to "Toán",
            5 to "Trải nghiệm hướng nghiệp"
        ),
        Calendar.THURSDAY to mapOf(
            1 to "Toán",
            2 to "Khoa học tự nhiên",
            3 to "Khoa học tự nhiên",
            4 to "Ngữ văn",
            5 to "Lịch sử - Địa lý"
        ),
        Calendar.FRIDAY to mapOf(
            1 to "Ngữ văn",
            2 to "Ngữ văn",
            3 to "Âm nhạc",
            4 to "Khoa học tự nhiên",
            5 to "Trí tuệ nhân tạo"
        ),
        Calendar.SATURDAY to mapOf(
            1 to "Tiếng Anh",
            2 to "Tiếng Anh",
            3 to "Lịch sử - Địa lý",
            4 to "Lịch sử - Địa lý",
            5 to "Toán"
        )
    )

    /**
     * Cac mon co hoc trong tuan, bo nhung mon khong co bai ve nha.
     *
     * Dung cho man con khai dang lam bai mon gi. Lay tu thoi khoa bieu chu khong go
     * tay mot danh sach thu hai: doi lop la sua mot cho, va con khong bao gio thay
     * mot mon no khong hoc.
     */
    fun tatCaMon(): List<String> =
        (SANG.values + CHIEU.values)
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
        SANG[thu]?.let { add(BuoiHoc(thu, Buoi.SANG, it)) }
        CHIEU[thu]?.let { add(BuoiHoc(thu, Buoi.CHIEU, it)) }
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
