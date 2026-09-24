package vn.huytl.homeworkgate.data

import java.text.Normalizer
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
 * cho nao khong. Rieng bo the ki hieu KHTN thi chu hoa co nghia - xem [dung].
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
     *  - chu hoa chu thuong: o bo Toan va tu vung chu hoa khong mang nghia gi, con
     *    tay con thi quen viet hoa chu dau. Bo nao chu hoa co nghia thi goi voi
     *    [giuHoa];
     *  - khoang trang: bo sach, vi "a^2+2ab+b^2" va "a² + 2ab + b²" la mot cong
     *    thuc. Doi lai "a book" go lien thanh "abook" cung duoc tinh dung - mot cho
     *    du, nhung du ve phia khong phat oan;
     *  - luy thua: "²" va "^2" la mot, "10²³" va "10^23" cung vay. Ban phim tablet
     *    khong co dau mu, ma bat con di tim no thi phan lon thoi gian lam bai thanh
     *    thoi gian tim phim;
     *  - chi so duoi: "H₂O" va "H2O" la mot. Sach in so nho duoi chan, ban phim thi
     *    khong co phim nao go ra no;
     *  - cac loai gach ngang (−, –, —) quy het ve "-": ba ky tu do nhin giong het
     *    nhau tren man hinh;
     *  - dau nhan: "·", "×", "*" va "." la mot. Chinh sach KHTN viet "d.V" o trang
     *    nay va "24,79·n" o trang kia;
     *  - dau chia: ":" va "/" la mot. Tu tieu hoc con da viet phep chia bang hai
     *    cham, "m : V" va "m/V" la mot cong thuc;
     *  - dau phay tren: "’" va "'" la mot. Ban phim co khi tu uon dau nay, ma "m'"
     *    trong cong thuc hieu suat van la "m'";
     *  - hai kieu ma cua cung mot chu co dau: "à" go lien mot ky tu, hay "a" kem mot
     *    dau huyen dung rieng. Nhin y het nhau, ban phim nao ra kieu nao la tuy;
     *  - ky tu an rong bang khong (U+200B, U+200C, U+FEFF...): vai bo go chen vao
     *    luc ghep dau, tren man hinh khong thay gi;
     *  - dau cham, cham than, cham hoi o CUOI: khong ai hoc thuoc mot dau cham.
     *
     * Nhung cho KHONG cho di, va day moi la phan quan trong:
     *
     *  - DAU TIENG VIET. "mà" khac "ma", va do chinh la thu dang hoc;
     *  - chinh ta tieng Anh. Sai mot chu la sai - do la ca noi dung cua the tu vung;
     *  - dau cach GIUA cac tu thi bo, nhung khong cho phep thieu hay thua chu.
     *
     * @param giuHoa giu nguyen chu hoa chu thuong. Chi [dung] goi voi true, cho
     *   nhung dap an ma chu hoa la mot phan cua dap an.
     */
    fun chuanHoa(chu: String, giuHoa: Boolean = false): String {
        val lien = Normalizer.normalize(chu, Normalizer.Form.NFC)
        val mu = (if (giuHoa) lien else lien.lowercase())
            .replace(CUM_MU) { cum -> "^" + cum.value.map(::doiKyTu).joinToString("") }
        return buildString {
            for (c in mu) {
                if (c.isWhitespace() || c.category == CharCategory.FORMAT) continue
                append(doiKyTu(c))
            }
        }.trimEnd('.', '!', '?')
    }

    /** Chu so nho tren dau, theo thu tu tu 0 den 9. */
    private const val SO_MU = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    /** Chu so nho duoi chan, theo thu tu tu 0 den 9. */
    private const val SO_DUOI = "₀₁₂₃₄₅₆₇₈₉"

    /**
     * Mot cum so mu viet lien, co the kem dau o truoc: "²", "²³", "⁻³".
     *
     * Doi CA CUM thanh mot dau mu chu khong doi tung chu: "10²³" doi tung chu ra
     * "10^2^3", trong khi con go "10^23".
     */
    private val CUM_MU = Regex("[⁺⁻]?[⁰¹²³⁴⁵⁶⁷⁸⁹]+")

    /** Dau cham giua, dau nhan, ngoi sao: cung la phep nhan. */
    private const val DAU_NHAN = "·×∙⋅*"

    /** Cac dau phay tren ma ban phim hay tu uon ra. */
    private const val DAU_PHAY_TREN = "’‘ʼ′"

    /** Mot ky tu doi ra ky tu go duoc tren ban phim. Xem [chuanHoa]. */
    private fun doiKyTu(c: Char): Char = when {
        c in SO_MU -> '0' + SO_MU.indexOf(c)
        c in SO_DUOI -> '0' + SO_DUOI.indexOf(c)
        c == '⁺' -> '+'
        // Dau tru mu, dau tru toan hoc, gach ngang ngan, gach ngang dai.
        c == '⁻' || c == '−' || c == '–' || c == '—' -> '-'
        c in DAU_NHAN -> '.'
        c == ':' || c == '÷' -> '/'
        c in DAU_PHAY_TREN -> '\''
        else -> c
    }

    /**
     * Con go the nay dung chua.
     *
     * So voi [TheHoc.dap] va moi ban trong [TheHoc.dapKhac]. Co dapKhac vi mot cau
     * hoi that su co nhieu cau tra loi dung: "big" va "large", "(a+b)(a-b)" va
     * "(a-b)(a+b)". Liet ke ra trong file chu khong de may doan.
     *
     * CHU HOA, voi bo [phanBietHoa]. "CO" la carbon monoxide con "Co" la cobalt, "D"
     * la khoi luong rieng con "d" la trong luong rieng: o nhung bo ki hieu do, chu hoa
     * chinh la thu dang hoc. Nen dap an nao CO chu hoa thi phai go dung tung chu hoa
     * chu thuong, ca chu dau. O go khai inputType="text", khong xin viet hoa dau cau,
     * nen ban phim khong tu viet hoa chu dau; con go "P = F/S" cho "p = F/S" thi do la
     * nham P (trong luong) voi p (ap suat), dung loi ma bo nay can bat.
     *
     * Dap an toan chu thuong ("đỏ", "kg/m³", "ampe kế") thi hoa thuong gi cung duoc,
     * ke ca trong bo phan biet hoa: o do chu hoa khong mang nghia gi ca.
     *
     * Chu hoa lam nhan cho dai luong ("FA", "MA", "CM") thi hoa hay thuong deu la mot
     * cach viet, nhung luat nay khong tu biet chu nao la nhan. Ban viet thuong cua nhan
     * phai ke trong dap_khac, vi du "Fa = d.V".
     */
    fun dung(go: String, the: TheHoc, phanBietHoa: Boolean = false): Boolean {
        if (chuanHoa(go).isEmpty()) return false
        return (listOf(the.dap) + the.dapKhac).any { dap ->
            val giuHoa = phanBietHoa && dap.any { it.isUpperCase() }
            chuanHoa(go, giuHoa) == chuanHoa(dap, giuHoa)
        }
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
