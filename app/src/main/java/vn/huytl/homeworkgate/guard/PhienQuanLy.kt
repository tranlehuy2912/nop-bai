package vn.huytl.homeworkgate.guard

/**
 * Da qua ma PIN trong lan mo app nay chua.
 *
 * Truoc day cau hoi "Ba Huy co dang o day khong" duoc tra loi bang [ParentMode]:
 * vao trang Ba Huy la tu dong mo khoa toan may. Hai viec do bi tron lam mot, nen
 * chi muon xem lai gio nghi cung lam tablet mo toang, va Ba Huy khong biet.
 *
 * Gio tach ra: co nay chi noi "da go dung PIN", con mo khoa may hay khong la mot
 * cong tac rieng nguoi dung tu bat.
 *
 * Giu trong bo nho chu khong ghi xuong dia: tien trinh chet la phai go PIN lai.
 */
object PhienQuanLy {

    @Volatile
    var daQuaPin = false

    /** Den moc nay (elapsedRealtime) thi thoi mo duong vao Cai dat. */
    @Volatile
    private var moCaiDatDen = 0L

    /**
     * Mo duong vao Cai dat trong [phut] phut.
     *
     * Ly do phai co: bang canh bao bao "chua bat tro nang", bam vao thi app mo
     * thang man hinh Tro nang - ma man hinh do nam trong so app chinh guard dang
     * chan. Bam xong bi day ve ngay, khong lam gi duoc, trong khi cach duy nhat de
     * sua la vao dung cho do. App tu day nguoi dung toi mot canh cua roi tu khoa
     * lai.
     *
     * Chi mo dung may man hinh he thong do, khong mo game hay app khac - khac han
     * cong tac "Mo toan bo may". Va chi goi sau khi da qua PIN.
     */
    fun choMoCaiDat(phut: Int) {
        moCaiDatDen = android.os.SystemClock.elapsedRealtime() + phut * 60_000L
    }

    fun dangChoMoCaiDat(): Boolean =
        moCaiDatDen > 0L && android.os.SystemClock.elapsedRealtime() < moCaiDatDen

    /** Dong lai ngay, dung khi Ba Huy bam Xong hoac khoa may. */
    fun thoiMoCaiDat() {
        moCaiDatDen = 0L
    }

    /**
     * Trang cau hinh co phai hoi lai PIN khong.
     *
     * Ba Huy mo trang cau hinh roi bam nut home, hay chi de may xuong ban cho man
     * hinh tat - lan sau mo app len la con nhin thay ngay trang duyet gio choi, bam
     * mot cai la tu cho minh choi. Nen vang mat lau thi trang bi che sau hop PIN, xem
     * [vn.huytl.homeworkgate.ui.HoiLaiPin].
     *
     * Hai phut moi tinh la lau, theo Ba Huy chon. Muoi giay truoc day chi du cho doi
     * man trong app hay xoay may; sang Telegram chep token hay sang trinh duyet chep
     * khoa AI thi lau hon the.
     *
     * Di bat quyen trong Settings thi khong hoi, vi chinh app vua day nguoi dung
     * sang do.
     */
    fun phaiHoiLaiPin(): Boolean =
        vn.huytl.homeworkgate.App.vangMatMs > VANG_MAT_TOI_DA && !dangChoMoCaiDat()

    /** Vang mat lau hon the thi coi nhu Ba Huy da roi may. */
    private const val VANG_MAT_TOI_DA = 120_000L
}
