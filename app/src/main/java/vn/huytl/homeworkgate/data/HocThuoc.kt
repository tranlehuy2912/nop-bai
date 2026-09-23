package vn.huytl.homeworkgate.data

import vn.huytl.homeworkgate.kho.TheHoc

/**
 * Luat cua duong HOC THUOC: may hoi, con go thang tren tablet, may cham ngay.
 *
 * VI SAO CO DUONG NAY. Ca app tinh gio theo CONG SUC NHIN THAY DUOC tren giay - so
 * dong con viet. Nhung hoc thuoc thi khong de ra dong nao: tu vung tieng Anh, cong
 * thuc, moc su. [DangBai.KHONG_TINH] tra 0 phut cho dung nhung thu do, va chuyen do
 * dung ve mat chong gian lan - anh chup mot trang chep tu vung khong chung minh
 * duoc con da thuoc. Nhung im lang thi lai noi voi dua tre mot cau khac han: hoc
 * thuoc khong dang gi.
 *
 * Duong nay go dung cho do, va go bang cach doi cach hoi. Khong chup anh, khong goi
 * AI: may giu san dap an, hien cau hoi len, con go tu tri nho. Go dung la dung, sai
 * la sai, khong co cho nao de cai.
 *
 * CON LAI MOT LO, va no khong bit duoc bang ma: con mo sach ra chep. Ba thu giu cho
 * lo do khong rong ra:
 *
 *  - luc hoc thuoc thi cong dang khoa, nen khong co app nao khac de tra;
 *  - moi ngay toi da [TRAN_PHUT_MOI_NGAY] phut, nen chep sach ca buoi cung chi duoc
 *    chung ay. Truoc 23/9/2026 cho nay con dua vao gia the thap, nua phut mot the,
 *    de gio sach ra chep lau hon ngoi hoc that; hom do Ba Huy nang len mot phut;
 *  - moi the chi tra gio mot lan cho moi vong hen, y het cau hoi - xem
 *    [vn.huytl.homeworkgate.kho.KhoBai.KHOANG_HEN_NGAY]. Chep mot lan thi lan sau
 *    ba ngay nua the do moi quay lai, va luc do van phai nho.
 *
 * KHONG BAT VIET HOA, KHONG BAT DAU CAU. Xem [chuanHoa] de biet cho nao du di va
 * cho nao khong.
 */
object HocThuoc {

    /**
     * Mot the dung duoc bay nhieu giay.
     *
     * Dem bang GIAY chu khong bang "may the mot phut", va dung mot gia voi mot tu o
     * [LuatTuVung.GIAY_MOI_TU]: hai duong deu la ngoi nho lai roi go ra, cong suc
     * mot cau nhu nhau, nen tra khac nhau thi con chi chon duong nao re hon.
     *
     * Mot phut, Ba Huy doi ngay 23/9/2026 cung luc voi tu vung. Truoc do la ba muoi
     * giay, de con khong bo bai tap di hoc thuoc. Cai chan cho chuyen do bay gio la
     * [TRAN_PHUT_MOI_NGAY]: go bao nhieu the thi mot ngay cung chi ra chung ay phut.
     */
    const val GIAY_MOI_THE = 60

    /**
     * Tran rieng cua duong hoc thuoc, moi ngay.
     *
     * Co tran rieng chu khong chi dua vao tran chung: tran chung 90 phut la cho CA
     * phan lam them, ma hoc thuoc chi nen la mot phan trong do. Khong co dong nay
     * thi ngoi go mot tram the la an tron tran ngay, va bai tap that khong con cho.
     */
    const val TRAN_PHUT_MOI_NGAY = 20

    /** Cung mot tran, doi ra giay de tinh chung mot don vi voi [GIAY_MOI_THE]. */
    const val GIAY_TRAN_MOI_NGAY = TRAN_PHUT_MOI_NGAY * 60

    /** Mot luot hoi toi da bay nhieu the. Dai hon thi thanh viec vat, khong ai ngoi het. */
    const val SO_THE_MOI_LUOT = 12

    /**
     * Ep hai ben ve cung mot dang truoc khi so.
     *
     * Nhung cho CHO DI, va tung cho deu co ly do:
     *
     *  - chu hoa chu thuong: dau cau tren ban phim tablet tu viet hoa, phat con vi
     *    cai tu dong do thi vo ly;
     *  - khoang trang: bo sach, vi "a^2+2ab+b^2" va "a² + 2ab + b²" la mot cong
     *    thuc. Doi lai "a book" go lien thanh "abook" cung duoc tinh dung - mot cho
     *    du, nhung du ve phia khong phat oan;
     *  - luy thua: "²" va "^2" la mot. Ban phim tablet khong co dau mu, ma bat con
     *    di tim no thi phan lon thoi gian lam bai thanh thoi gian tim phim;
     *  - cac loai gach ngang (−, –, —) quy het ve "-": ba ky tu do nhin giong het
     *    nhau tren man hinh;
     *  - dau cham, cham than, cham hoi o CUOI: khong ai hoc thuoc mot dau cham.
     *
     * Nhung cho KHONG cho di, va day moi la phan quan trong:
     *
     *  - DAU TIENG VIET. "mà" khac "ma", va do chinh la thu dang hoc;
     *  - chinh ta tieng Anh. Sai mot chu la sai - do la ca noi dung cua the tu vung;
     *  - dau cach GIUA cac tu thi bo, nhung khong cho phep thieu hay thua chu.
     */
    fun chuanHoa(chu: String): String = chu
        .lowercase()
        .replace('−', '-')  // dau tru toan hoc
        .replace('–', '-')  // gach ngang ngan
        .replace('—', '-')  // gach ngang dai
        .replace("²", "^2")
        .replace("³", "^3")
        .filterNot { it.isWhitespace() }
        .trimEnd('.', '!', '?')

    /**
     * Con go the nay dung chua.
     *
     * So voi [TheHoc.dap] va moi ban trong [TheHoc.dapKhac]. Co dapKhac vi mot cau
     * hoi that su co nhieu cau tra loi dung: "big" va "large", "(a+b)(a-b)" va
     * "(a-b)(a+b)". Liet ke ra trong file chu khong de may doan.
     */
    fun dung(go: String, the: TheHoc): Boolean {
        val g = chuanHoa(go)
        if (g.isEmpty()) return false
        return g == chuanHoa(the.dap) || the.dapKhac.any { g == chuanHoa(it) }
    }

    /** So giay mot luot lam ra, chua chan tran. */
    fun giayCho(soDung: Int): Int = soDung.coerceAtLeast(0) * GIAY_MOI_THE

    /**
     * So phut ca ngay dang duoc, tu tong so giay da hoc trong ngay.
     *
     * Chan tran TRUOC khi chia, nen qua tran bao nhieu cung chi ra dung tran.
     */
    fun phutTrongNgay(giay: Int): Int = giay.coerceIn(0, GIAY_TRAN_MOI_NGAY) / 60

    /**
     * So phut cong cho mot luot: phan CHENH cua ca ngay truoc va sau luot do.
     *
     * VI SAO KHONG CHIA RIENG TUNG LUOT. Truoc 23/9/2026 mot the la nua phut, nen
     * luot le the thi chia rieng ra bao nhieu cung mat: ba luot moi luot ba the la
     * chin the, dang bon phut ruoi, ma chia rieng thi moi luot duoc mot phut - con
     * mat mot phut ruoi vi da chia lam ba lan ngoi. Tinh bang phan chenh thi phan le
     * nam lai trong ngay va luot sau nhat duoc.
     *
     * Tu hom do mot the tron mot phut nen luot moi khong de ra phan le nua. Van giu
     * cach tinh nay vi no dung voi moi gia, va gia the doi lan nua thi khong phai
     * viet lai.
     *
     * @param giayTruoc tong giay duong hoc thuoc da lam ra trong ngay, truoc luot nay.
     * @param giayLuot so giay luot nay lam ra, tu [giayCho].
     */
    fun phutThem(giayTruoc: Int, giayLuot: Int): Int =
        phutTrongNgay(giayTruoc + giayLuot) - phutTrongNgay(giayTruoc)
}
