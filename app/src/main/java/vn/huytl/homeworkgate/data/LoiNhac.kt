package vn.huytl.homeworkgate.data

import java.util.Calendar

enum class LoaiNhac {
    /** Man chan che kin man hinh trong suot buoi hoc. */
    CHAN,
    SAP_DI_HOC,
    SOAN_VO,
    DANG_GIO_HOC
}

data class LoiNhac(
    val loai: LoaiNhac,
    val tieuDe: String,
    val chiTiet: String,
    /** Gap thi dai nhac to len va nam li, khong tu thu lai. */
    val gap: Boolean,
    /** Buoi hoc lien quan, de man soan vo biet liet ke mon nao. */
    val buoi: BuoiHoc?,
    /** Ma buoi hoc, dung lam khoa luu da soan hay chua. */
    val maBuoi: String?,
    /** So giay con lai toi moc buong may. Chi co khi dang dem tung giay, con lai la -1. */
    val giayConLai: Int = -1,
    /** Voi man chan: moc het chan, phut tinh tu 00:00. */
    val phutHetChan: Int = -1
)

/**
 * Tinh xem ngay luc nay co gi dang treo khong.
 *
 * App khong tu keu, no cho Le Hoa mo may roi moi hien. Nen ham nay duoc goi moi lan
 * Le Hoa mo khoa may, va dinh ky trong luc Le Hoa dang dung may.
 */
object TinhLoiNhac {

    /** Bao truoc bao lau so voi moc buong may. */
    private const val BAO_TRUOC_PHUT = 60

    /** Duoi nguong nay thi coi la gap. */
    private const val GAP_KHI_CON_PHUT = 15

    /** Tim toi da bao nhieu ngay toi de ra buoi hoc ke tiep. */
    private const val TIM_TOI_DA_NGAY = 14

    fun tinh(
        now: Calendar,
        maBuoiDaSoan: Set<String>,
        batManChan: Boolean = false,
        buoiDuocMoSom: String = ""
    ): LoiNhac? {
        val phutHienTai = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // Man chan xet truoc het: no phu tu moc buong may toi luc tan hoc, rong
        // hon moi truong hop khac.
        if (batManChan) {
            buoiDangTrongGioChan(now, phutHienTai)?.let { buoi ->
                val ma = maBuoi(now, buoi)
                if (ma != buoiDuocMoSom) {
                    return LoiNhac(
                        loai = LoaiNhac.CHAN,
                        tieuDe = "Tới giờ đi học rồi",
                        chiTiet = "${moTaBuoi(buoi).replaceFirstChar { it.uppercase() }}, " +
                            "vào học ${gioPhut(buoi.phutVaoHoc)}",
                        gap = true,
                        buoi = buoi,
                        maBuoi = ma,
                        phutHetChan = buoi.phutTanHoc
                    )
                }
            }
        }

        // Dang ngoi trong lop ma mo may
        dangTrongGioHoc(now, phutHienTai)?.let { buoi ->
            return LoiNhac(
                loai = LoaiNhac.DANG_GIO_HOC,
                tieuDe = "Giờ này đang có tiết ${buoi.monTheoTiet[tietDangHoc(buoi, phutHienTai)] ?: ""}".trim(),
                chiTiet = "Hôm nay nghỉ học à?",
                gap = false,
                buoi = null,
                maBuoi = null
            )
        }

        val ke = buoiKeTiep(now) ?: return null
        val (calBuoi, buoi) = ke
        val ma = maBuoi(calBuoi, buoi)
        val phutToiBuong = phutConLai(now, calBuoi, ThoiKhoaBieu.phutBuongMay(buoi))
        val phutToiVao = phutConLai(now, calBuoi, buoi.phutVaoHoc)

        // Sap phai buong may di chuan bi
        if (phutToiVao > 0 && phutToiBuong <= BAO_TRUOC_PHUT) {
            val gap = phutToiBuong <= GAP_KHI_CON_PHUT
            // Vao nguong gap thi dem tung giay, cho Le Hoa thay thoi gian dang chay
            // chu khong phai mot con so dung yen.
            val giay = if (gap) giayConLaiToi(now, calBuoi, ThoiKhoaBieu.phutBuongMay(buoi)) else -1
            val tieuDe = when {
                // Vao nguong gap thi hien phut:giay, cho Le Hoa thay dong ho dang chay.
                // Mot con so phut dung yen thi Le Hoa van tuong minh con nhieu thoi gian.
                giay > 0 -> "Còn ${dinhDangPhutGiay(giay)} nữa phải chuẩn bị đi học"
                phutToiBuong > 0 -> "Còn $phutToiBuong phút nữa phải chuẩn bị đi học"
                else -> "Tới giờ chuẩn bị đi học rồi"
            }
            return LoiNhac(
                loai = LoaiNhac.SAP_DI_HOC,
                tieuDe = tieuDe,
                chiTiet = "Vào học lúc ${gioPhut(buoi.phutVaoHoc)}, ${moTaBuoi(buoi)}",
                gap = gap,
                buoi = buoi,
                maBuoi = ma,
                giayConLai = giay
            )
        }

        // Chua chuan bi cho buoi ke tiep
        if (ma !in maBuoiDaSoan) {
            val soMon = buoi.monCanSoan.size

            // Buoi khong co mon nao mang vo va cung khong co the duc thi khong co
            // viec gi de nhac ca. Im lang, dung bay ra mot loi nhac rong.
            if (soMon == 0 && !buoi.coTheDuc) return null

            // Noi dung mon nao ra mon nay. Truoc day buoi chi co the duc van bao
            // "soan tap vo" roi them "khong phai mang vo", doc nhu app hong.
            val tieuDe = if (soMon == 0) {
                "${moTaBuoi(buoi).replaceFirstChar { it.uppercase() }} có tiết học thể dục"
            } else {
                "Soạn tập vở cho ${moTaBuoi(buoi)}"
            }
            val chiTiet = when {
                soMon == 0 -> "Chuẩn bị đồ thể dục"
                buoi.coTheDuc -> "$soMon môn cần mang, và nhớ đồ thể dục"
                else -> "$soMon môn cần mang"
            }
            return LoiNhac(
                loai = LoaiNhac.SOAN_VO,
                tieuDe = tieuDe,
                chiTiet = chiTiet,
                gap = false,
                buoi = buoi,
                maBuoi = ma
            )
        }

        return null
    }

    /**
     * Buoi hoc ma ngay luc nay dang nam trong khoang bi chan, tu moc buong may
     * toi luc tan hoc.
     *
     * Rong hon khoang gio hoc that: thu hai chan tu 8h45 du tiet dau la 9h15,
     * vi khoang do la luc Le Hoa phai di duong.
     */
    fun buoiDangTrongGioChan(now: Calendar, phutHienTai: Int): BuoiHoc? {
        if (NgayNghi.laNgayNghi(now)) return null
        return ThoiKhoaBieu.buoiHocCua(now.get(Calendar.DAY_OF_WEEK))
            .firstOrNull {
                phutHienTai >= ThoiKhoaBieu.phutBuongMay(it) && phutHienTai < it.phutTanHoc
            }
    }

    /**
     * Buoi hoc dang thuc su bi chan ngay luc nay, hoac null neu khong chan.
     *
     * Khac [buoiDangTrongGioChan] o cho da tinh ca hai duong tat: cong tac chan bi
     * tat han, va buoi nay Ba Huy da cho mo som. De o day chu khong de moi noi tu
     * ghep lai ba dieu kien, vi ghep thieu mot cai la hai cho trong app tra loi
     * khac nhau cho cung mot cau hoi "co dang chan khong".
     */
    fun buoiDangChan(
        now: Calendar,
        batManChan: Boolean,
        buoiDuocMoSom: String
    ): BuoiHoc? {
        if (!batManChan) return null
        val phut = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val buoi = buoiDangTrongGioChan(now, phut) ?: return null
        return if (maBuoi(now, buoi) == buoiDuocMoSom) null else buoi
    }

    /** So giay tu bay gio toi mot moc trong ngay. */
    private fun giayConLaiToi(now: Calendar, calBuoi: Calendar, phutTrongNgay: Int): Int {
        val moc = (calBuoi.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, phutTrongNgay / 60)
            set(Calendar.MINUTE, phutTrongNgay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return ((moc.timeInMillis - now.timeInMillis) / 1000L).toInt().coerceAtLeast(0)
    }

    /** Buoi hoc dang dien ra ngay luc nay, neu co. */
    private fun dangTrongGioHoc(now: Calendar, phutHienTai: Int): BuoiHoc? {
        if (NgayNghi.laNgayNghi(now)) return null
        return ThoiKhoaBieu.buoiHocCua(now.get(Calendar.DAY_OF_WEEK))
            .firstOrNull { phutHienTai >= it.phutVaoHoc && phutHienTai < it.phutTanHoc }
    }

    private fun tietDangHoc(buoi: BuoiHoc, phutHienTai: Int): Int =
        buoi.monTheoTiet.keys.sorted().lastOrNull {
            phutHienTai >= ThoiKhoaBieu.gioTiet(buoi.buoi, it)
        } ?: buoi.tietDau

    /**
     * Buoi hoc gan nhat chua dien ra, cung voi ngay cua no.
     *
     * Bo qua ngay nghi le va chu nhat. Tim toi hai tuan de Le Hoa vuot qua duoc ky
     * nghi Tet dai.
     */
    fun buoiKeTiep(now: Calendar): Pair<Calendar, BuoiHoc>? {
        val phutHienTai = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (lui in 0 until TIM_TOI_DA_NGAY) {
            val cal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, lui) }
            if (NgayNghi.laNgayNghi(cal)) continue

            val cacBuoi = ThoiKhoaBieu.buoiHocCua(cal.get(Calendar.DAY_OF_WEEK))
            val buoi = if (lui == 0) {
                cacBuoi.firstOrNull { it.phutVaoHoc > phutHienTai }
            } else {
                cacBuoi.firstOrNull()
            }
            if (buoi != null) return cal to buoi
        }
        return null
    }

    /** So phut tu bay gio toi mot moc trong ngay [calBuoi]. */
    private fun phutConLai(now: Calendar, calBuoi: Calendar, phutTrongNgay: Int): Int {
        val moc = (calBuoi.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, phutTrongNgay / 60)
            set(Calendar.MINUTE, phutTrongNgay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return ((moc.timeInMillis - now.timeInMillis) / 60_000L).toInt()
    }

    fun maBuoi(cal: Calendar, buoi: BuoiHoc): String = "%04d%02d%02d-%s".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
        buoi.buoi.name
    )

    fun moTaBuoi(buoi: BuoiHoc): String {
        val ten = if (buoi.buoi == Buoi.SANG) "sáng" else "chiều"
        return "$ten ${ThoiKhoaBieu.tenThu(buoi.thu)}"
    }

    fun gioPhut(phut: Int): String = "%02d:%02d".format(phut / 60, phut % 60)

    /** Doi giay thanh dang phut:giay, vi du 512 giay ra 8:32. */
    fun dinhDangPhutGiay(giay: Int): String = "%d:%02d".format(giay / 60, giay % 60)
}
