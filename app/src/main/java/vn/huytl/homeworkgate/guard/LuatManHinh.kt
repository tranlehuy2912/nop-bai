package vn.huytl.homeworkgate.guard

/** Doc xong danh sach cua so thi guard xet the nao. */
enum class CachXet {
    /** Xet binh thuong: app bi chan thi day ve man hinh chinh. */
    XET,

    /** Khong xet gi: man chan gio hoc cua chinh app nay, hoac man hinh khoa. */
    BO_QUA,

    /** Thanh thong bao dang keo xuong: che app bi chan ben duoi va dong thanh, khong bam Home. */
    DUOI_THONG_BAO,
}

/**
 * Luat doc danh sach cua so, tach khoi dich vu canh app de test duoc bang so.
 *
 * TRUOC 27/9/2026 luat chi co mot cau: co cua so he thong nao dang giu tieu diem
 * thi coi la thanh thong bao dang mo, va thoi khong xet gi ca, de con bam duoc nut
 * tam dung nhac tren thanh ma khong bi bao het gio. Le Hoa tim ra cho lach: keo
 * thanh thong bao xuong mot chut roi giu yen ngon tay. Thanh chua dong nen guard
 * khong xet, con app ben duoi van hien gan tron, xem bao lau cung duoc.
 *
 * Them mot chuyen thu tren may ao Android 13: trong luc thanh dang keo, danh sach
 * cua so chi con dung mot cai la chinh thanh do. Thanh phu kin man hinh va an cham,
 * nen he thong khong bao cac cua so nam duoi nua. Muon biet ben duoi co gi thi phai
 * nho lai cai vua hien truoc khi keo - xem [appDangHien].
 */
object LuatManHinh {

    /**
     * Cua so he thong (kieu TYPE_SYSTEM) dang giu tieu diem.
     *
     * @param cuaMinh cua so cua chinh app nay. Chi co man chan gio hoc la giu tieu
     *   diem, de nuot nut Back.
     * @param phuKin  cua so rong gan bang ca man hinh, nhu thanh thong bao khi keo
     *   xuong. Khong phai cai thanh nho tren dau cua so noi.
     */
    class TieuDiem(val cuaMinh: Boolean, val phuKin: Boolean)

    /**
     * @param tieuDiem   cua so he thong dang giu tieu diem, null neu tieu diem o mot app
     * @param manKhoa    dang o man hinh khoa. Man khoa cung ve trong cua so cua thanh
     *   thong bao, nen trong danh sach no giong het thanh dang mo.
     * @param docDuocApp doc duoc it nhat mot cua so app
     */
    fun cachXet(tieuDiem: TieuDiem?, manKhoa: Boolean, docDuocApp: Boolean): CachXet = when {
        tieuDiem == null -> CachXet.XET
        // Hai man nay che het moi app, khong co gi dung duoc ben duoi. Giu nguyen
        // cach cu la khong xet gi.
        tieuDiem.cuaMinh || manKhoa -> CachXet.BO_QUA
        // Khong doc duoc app nao nghia la co cai dang phu kin man hinh, du no rong
        // hay hep: cua so an cham ma khong cho cham xuyen thi he thong giau het cac
        // cua so ben duoi.
        tieuDiem.phuKin || !docDuocApp -> CachXet.DUOI_THONG_BAO
        // Cua so he thong nho ma app ben duoi van doc duoc, nhu thanh keo tren dau
        // cua so noi: xet binh thuong. Coi no la thanh thong bao thi con chi can cham
        // vao cai thanh do la guard thoi chan.
        else -> CachXet.XET
    }

    /**
     * Cac goi coi nhu dang hien.
     *
     * Thanh thong bao dang phu ma khong doc duoc app nao thi lay cai vua hien ngay
     * truoc khi keo. Trong luc ngon tay giu thanh, cham nao cung di vao thanh, nen
     * app ben duoi khong tu doi duoc. Duong con lai la mot app mo len ngay duoi thanh,
     * nhu keo mot thong bao ra cua so noi: danh sach cua so khong thay no, chi co su
     * kien cua so bao ten. [goiSuKienMoi] la ten do, va no duoc them vao cai nam duoi.
     *
     * @param goiSuKienMoi goi cua su kien "cua so truoc mat doi" vua toi, null neu lan xet
     *   nay khong do su kien do goi. Dung truyen goi nho tu truoc: co khi do la app da
     *   bi day di tu lau.
     */
    fun appDangHien(
        cach: CachXet,
        docDuoc: Set<String>,
        truocKhiKeo: Set<String>,
        goiSuKienMoi: String? = null,
    ): Set<String> =
        if (cach == CachXet.DUOI_THONG_BAO && docDuoc.isEmpty()) {
            truocKhiKeo + listOfNotNull(goiSuKienMoi)
        } else {
            docDuoc
        }
}
