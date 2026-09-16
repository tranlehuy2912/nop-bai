package vn.huytl.homeworkgate.dongbo

/**
 * Ten duong dan va ten truong trong Firestore.
 *
 * QUAN TRONG: file nay phai giong het ban sao ben dien thoai Ba Huy
 * (vn.huytl.bangdieukhien.data.Duong trong homework-gate-3). Doi mot chuoi o mot
 * ben thoi la hai app noi hai thu tieng: lenh gui di khong ai nhan, trang thai doc
 * ve luon rong, va khong co gi bao loi ca - do la kieu hong kho tim nhat.
 *
 * Nen moi lan sua o day, sua ca hai file cung mot luc.
 */
object Duong {

    const val NHA = "nha"

    // --- cac document trong mot nha ---
    const val HOP = "hop"
    const val D_TRANG_THAI = "trangthai"
    const val D_CAI_DAT = "caidat"
    const val D_DANH_SACH_APP = "danhsachapp"

    const val LENH = "lenh"
    const val BAI = "bai"
    const val NHAT_KY = "nhatky"
    const val HOI_AI = "hoiai"
    const val CHAT = "chat"
    const val GHEP = "ghep"

    // --- truong trong nha/{nhaId} ---
    const val F_UIDS = "uids"
    const val F_TEN_CON = "tenCon"
    const val F_MA_GHEP = "maGhep"
    const val F_MA_GHEP_HET_HAN = "maGhepHetHan"

    // --- truong trong hop/trangthai ---
    const val F_CONG = "cong"
    const val F_KET_THUC_LUC = "ketThucLuc"
    const val F_CON_LAI_MS = "conLaiMs"
    const val F_PHUT_DA_DUYET = "phutDaDuyet"
    const val F_PHUT_CON_LAI = "phutConLai"
    const val F_SO_BAI_CHO = "soBaiCho"
    const val F_CHE_DO_BA = "cheDoBa"
    const val F_QUYEN = "quyen"
    const val F_PIN_MAY = "pinMay"
    const val F_DANG_SAC = "dangSac"
    const val F_BAN_APP = "banApp"
    const val F_CAP_NHAT_LUC = "capNhatLuc"
    const val F_APP_TRUOC_MAT = "appTruocMat"

    /** Cau tablet noi lai sau khi lam mot lenh: { chu, luc }. */
    const val F_TRA_LOI = "traLoi"

    // --- truong trong lenh/{id} ---
    const val F_KIEU = "kieu"
    const val F_PHUT = "phut"
    const val F_BAI_ID = "baiId"
    const val F_CHU = "chu"
    const val F_TAO_LUC = "taoLuc"

    // --- truong trong bai/{id} ---
    const val F_LUC = "luc"
    const val F_TRANG_THAI = "trangThai"
    const val F_SO_PHUT = "soPhut"
    const val F_ANH = "anh"
    const val F_CHAM = "cham"
    const val F_MESSAGE_ID = "messageId"
    const val F_FILE_ID = "fileId"
    const val F_KHAU = "khau"

    // --- truong trong chat/{id} ---
    const val F_TU = "tu"
    const val F_DA_DOC = "daDoc"

    // --- truong trong nhatky/{ngay} ---
    const val F_DONG = "dong"
}

/**
 * Cac kieu lenh dien thoai gui sang tablet.
 *
 * Chuoi chu khong phai enum: enum ghi xuong Firestore thanh ten hang, ma doi ten
 * hang la lenh cu dang nam trong hang doi thanh vo nghia. Chuoi thi nhin thay
 * ngay trong console Firebase luc do loi.
 */
object Lenh {
    /** Duyet mot bai dang cho. Kem [Duong.F_BAI_ID] va so phut. */
    const val DUYET = "DUYET"

    /** Khong duyet. Kem ly do o [Duong.F_CHU] neu co. */
    const val TU_CHOI = "TUCHOI"

    /** Cho choi ngay, khong tru han muc ngay. Dang choi thi cong them. */
    const val CHO = "CHO"

    /** Bot phut cua phien dang chay. */
    const val BOT = "BOT"

    const val DUNG = "DUNG"
    const val TIEP = "TIEP"

    /** Khoa tablet ngay, mat so phut con lai. */
    const val KHOA = "KHOA"

    /** Mo toan bo may cho Ba Huy dung. Kem so phut, khong co thi khong dat han. */
    const val MO_MAY = "MOMAY"

    /** Dong che do mo toan bo may. */
    const val DONG_MAY = "DONGMAY"

    /** Tat quan tri thiet bi de go app. */
    const val CHO_GO_APP = "CHOGOAPP"

    const val XOA_PIN = "XOAPIN"

    /** Doi mot muc cau hinh. [Duong.F_CHU] la ten muc, gia tri o truong "giaTri". */
    const val CAI_DAT = "CAIDAT"

    /** Nhan mot cau cho con, hien thanh thong bao co tieng tren tablet. */
    const val NHAN = "NHAN"
}

/** Trang thai cong, y het GateState ben tablet. */
object Cong {
    const val KHOA = "LOCKED"
    const val CHO_DUYET = "PENDING"
    const val DA_DUYET = "GRANTED"
    const val DANG_CHOI = "ACTIVE"
    const val TAM_DUNG = "PAUSED"
}
