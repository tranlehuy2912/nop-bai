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
 *  - gia mot the thap ([THE_MOI_PHUT] the moi duoc mot phut), nen gio sach ra chep
 *    lau hon la ngoi hoc that;
 *  - moi the chi tra gio mot lan cho moi vong hen, y het cau hoi - xem
 *    [vn.huytl.homeworkgate.kho.KhoBai.KHOANG_HEN_NGAY]. Chep mot lan thi lan sau
 *    ba ngay nua the do moi quay lai, va luc do van phai nho.
 *
 * KHONG BAT VIET HOA, KHONG BAT DAU CAU. Xem [chuanHoa] de biet cho nao du di va
 * cho nao khong.
 */
object HocThuoc {

    /**
     * Bay nhieu the dung thi duoc mot phut.
     *
     * Ba, chon theo cong suc that giong cach [LuatCongGio] chon cac con so kia: mot
     * the tu vung nho lai va go ra mat chung muoi lam giay, nen ba the la khoang mot
     * phut ngoi hoc. Dat ngang voi thuc te thi khong ai phai cai ve no.
     *
     * Ha xuong (moi the mot phut) thi mot buoi hoc thuoc ba muoi tu duoc ba muoi
     * phut, nhieu hon ca lam het bai co giao - luc do con bo bai tap di hoc thuoc.
     */
    const val THE_MOI_PHUT = 3

    /**
     * Tran rieng cua duong hoc thuoc, moi ngay.
     *
     * Co tran rieng chu khong chi dua vao tran chung: tran chung 90 phut la cho CA
     * phan lam them, ma hoc thuoc chi nen la mot phan trong do. Khong co dong nay
     * thi ngoi go mot tram the la an tron tran ngay, va bai tap that khong con cho.
     */
    const val TRAN_PHUT_MOI_NGAY = 20

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

    /**
     * So phut cho mot luot, da chan theo tran ngay.
     *
     * @param daCoHomNay so phut duong hoc thuoc da tra trong ngay.
     */
    fun phutCho(soDung: Int, daCoHomNay: Int = 0): Int {
        if (soDung <= 0) return 0
        val con = (TRAN_PHUT_MOI_NGAY - daCoHomNay).coerceAtLeast(0)
        return (soDung / THE_MOI_PHUT).coerceAtMost(con)
    }
}
