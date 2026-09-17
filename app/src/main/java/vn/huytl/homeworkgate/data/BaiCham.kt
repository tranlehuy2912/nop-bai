package vn.huytl.homeworkgate.data

/**
 * Dang cua mot muc bai lam, de biet tra bao nhieu phut cho no.
 *
 * Chia theo DANG BAI chu khong theo MON. Mon khong quyet dinh cong suc: mot cau
 * tieng Anh dien tu va mot cau toan nho deu la vai dong, con mot doan van ta con
 * meo va mot bai thuyet trinh deu la nua trang giay. Chia theo mon thi phai nghi
 * ra bang gia cho tung mon moi nam va sua lai moi lan doi lop.
 */
enum class DangBai {
    /**
     * Cau chi can khoanh mot chu, ghi Dung/Sai, noi cot, dien mot tu.
     *
     * Khong tinh tung cau ma tinh theo CUM - xem [LuatCongGio]. Mot trang trac
     * nghiem chi la mot xap dap an mot tu, AI tach ra bao nhieu "cau" la tuy no:
     * do ba lan tren cung nam tam anh KHTN ngay 14/9/2026 ra 36, 4, roi 22 cau. Tra
     * gio theo con so do thi cung mot buoi hoc ra 8 phut hay 72 phut tuy may.
     */
    TRAC_NGHIEM,

    /** Cau nho trong mot bai nhieu cau: 2.26a, 2.26b... Vai dong mot cau. */
    CAU_NHO,

    /** Mot bai dung rieng, khong chia cau nho. */
    BAI_RIENG,

    /** Bai viet dai: doan van, bai van, bai thuyet trinh. Tinh theo trang. */
    VIET_DAI,

    /** Hoc thuoc, chep phat, luyen chu... Anh khong chung minh duoc, khong tinh may. */
    KHONG_TINH
}

/**
 * Mot muc bai lam sau khi AI cham.
 *
 * @param ma ma ngan de goi ten muc do: "2.26a", "cau 3", "doan van".
 * @param de de bai cua muc do, chep tu anh. Day cung la khoa doi chieu voi so cai
 *   de biet muc nay da nop hom nao chua.
 * @param ketQua ket qua cuoi cung hoc sinh viet.
 * @param dung AI cham la dung hay sai.
 * @param docRo AI co doc ro muc nay khong. Khong ro thi dung tu duyet - de Ba Huy
 *   nhin. Thu nghiem ngay 13/9 cho thay gap chu mo, model co xu huong dien vao
 *   bang dap an dung ma no biet, tuc la bai sai co the thanh bai dung.
 * @param trongDanDo muc nay nam trong bai co giao (vo dan do) hay la bai lam them.
 * @param soDong so dong LAM BAI cua muc nay: khong tinh dong chep de, dong bo trong,
 *   dong da gach xoa. Day la thuoc do cong suc - xem [LuatCongGio].
 */
data class CauCham(
    val ma: String,
    val de: String,
    val ketQua: String = "",
    val dung: Boolean = false,
    val docRo: Boolean = true,
    val dang: DangBai = DangBai.CAU_NHO,
    val trongDanDo: Boolean = false,
    val soDong: Int = 0,
    val nhanXet: String = "",
    /**
     * Cau nay sai KIEU gi, mot trong bay nhan co dinh o quy tac cuoi cua
     * [vn.huytl.homeworkgate.ai.PromptCham.CAU_LENH]. Rong la cau dung, hoac ban
     * cham cu khong co truong nay.
     *
     * VI SAO XEP NHAN CHU KHONG DE MAY TU DAT CHU: con so "hom nay sai 3 cau" khong
     * noi duoc gi de ngoi noi chuyen voi con. "Ca thang sai dau 14 lan" thi noi
     * duoc. Ma cong don chi ra con so khi ten loi lan nao cung viet giong lan truoc,
     * nen tap nhan phai dong va phai nam trong cau lenh.
     *
     * KHONG DUNG VAO VIEC TINH GIO, y nhu [mucDo] va [CauSo.khaiChac]: cai gi tru
     * vao so phut thi con se khai theo cai co loi chu khong theo cai that.
     */
    val loaiLoi: String = "",
    /**
     * Ma co dinh cua cau trong ngan hang, dang "toan8t1:2.26a".
     *
     * Co ma nay thi so cai khong phai doan gi nua - xem [vn.huytl.homeworkgate.data.SoCaiBai].
     * Null la bai ngoai sach (vo bai tap, phieu photo, mon chua nap sach), luc do
     * quay ve nhan dang bang de bai da chuan hoa nhu truoc.
     */
    val cauId: String? = null,
    /** Mon hoc, chep tu [KetQuaCham.mon] xuong de dong trong kho tu no du nghia. */
    val mon: String = "",
    /**
     * Tung dong con tu viet de lam cau nay, chep tu anh ra, dung thu tu tren giay.
     *
     * Day la thu man doi chieu bay ra cho con soat: may doc "l" ma con viet "1" thi
     * con thay ngay va sua dung dong do. Truoc ban nay may chi tra ve moi [ketQua] -
     * ket qua cuoi - nen neu no doc nham mot dong giua bai thi tren man hinh khong
     * ton tai dong do de ma sua.
     *
     * Chep dung cai tren giay KE CA KHI SAI: day la ban chep, khong phai ban chua.
     *
     * Rong nghia la may khong tra ve (ban cu, hay mot lan tra loi thieu truong).
     */
    val baiLam: List<String> = emptyList(),
    /**
     * Dong dau tien sai trong [baiLam], dem tu 1. So 0 la dung het hoac may khong
     * chi ra duoc.
     *
     * De to mau dong do len cho con nhin. Truoc day cho sai chi nam trong cau van
     * [nhanXet] ("Dong 2: con viet ... la sai"), doc duoc nhung phai doc het cau.
     */
    val dongSai: Int = 0,
    /**
     * May co nhin thay DE BAI cua cau nay khong.
     *
     * Ngay 16/9/2026 Le Hoa nop mot xap anh vo KHTN: "cau 1 B", "cau 2 C", "1 sai",
     * "2 dung"... ba trang toan dap an, khong mot dong de bai nao. "Cau 1 B" thi
     * khong dung khong sai neu khong biet cau 1 hoi gi - ma may van phai tra ve mot
     * chu dung hay sai, nen no se doan. Doan trung thi con duoc gio cho mot trang
     * khong ai cham duoc; doan truot thi con bi bao sai oan.
     *
     * Nen o day bat may khai thang: co nhin thay de khong. Khong co de thi khong tra
     * gio, va noi ro cho con biet de con chup them trang de - chu khong im lang.
     */
    val coDe: Boolean = true,
    /**
     * Con khai la lam cau nay, nhung trong anh co that su thay bai lam khong.
     *
     * Chi co nghia o duong con khai bai truoc khi chup. Khai mot loat cau roi chi
     * lam hai cau ma van duoc tinh ca loat thi cai man khai bai tro thanh cho de
     * gian lan nhat trong app, chu khong phai cho chac nhat.
     */
    val coLam: Boolean = true,
    /**
     * Bai lam cua cau nay co viet bang muc DO khong. Chi hoi o lan on tap.
     *
     * VI SAO PHAI HOI: on tap la duong duy nhat trong ca app duoc phep cham lai mot
     * cau da lam dung - va chinh vi vay no thao mat cai khoa "moi cau chi tra gio
     * mot lan". Con mo vo ra dung trang cu, chup lai bai da lam tuan truoc, thi anh
     * do khong khac gi anh cua mot bai vua lam xong: tren giay khong co dau thoi
     * gian nao ca.
     *
     * Nen luat nha: on thi viet bang but do. Mot cai nhin vao anh la biet, ke ca khi
     * may doc nham chu.
     *
     * Khong dung de TU CHOI, chi dung de thoi tu duyet: may nhin nham mau trong anh
     * thieu sang la chuyen co that, va mot cho hong ben may khong duoc bien thanh
     * mot lan con mat gio. Bao 0 thi bai do sang tay Ba Huy, kem dong chu noi ro
     * vi sao.
     *
     * BA GIA TRI, va phai la ba chu khong phai hai: 1 la muc do, 0 la muc khac, -1
     * la MAY KHONG TRA LOI ve mau. Gop -1 vao 1 thi luat but do tu tat di ma khong
     * ai biet - model bo qua mot truong la chuyen thuong, va luc do moi bai on deu
     * lot qua nhu khong co luat nao. Giu rieng ra thi tin Telegram noi duoc mot cau
     * cho Ba Huy hay.
     */
    val mucDo: Int = -1
)

/**
 * Bay kieu sai, va ten tieng Viet de doc len.
 *
 * TAP DONG, va do la ca cai gia tri. De may tu dat ten loi thi moi lan chay ra mot
 * chu khac ("sai dau", "nham dau", "loi dau") va cong don lai khong ra con so nao.
 * Bay nhan nay duoc ke thang trong cau lenh gui cho may - xem quy tac cuoi cua
 * [vn.huytl.homeworkgate.ai.PromptCham.CAU_LENH] - va moi ban cham doc ve deu bi
 * ep ve dung tap nay.
 *
 * Bay la con so chon co chu dich. It hon thi "KHAC" nuot gan het; nhieu hon thi hai
 * nhan sat nghia nhau (sai dau va sai buoc bien doi) bi may xep loan xa, va cong lai
 * khong tin duoc nua. Them nhan thi phai sua o CA HAI cho: o day va trong cau lenh.
 */
object LoaiLoi {

    const val SAI_DAU = "SAI_DAU"
    const val SAI_BUOC = "SAI_BUOC"
    const val NHAM_CONG_THUC = "NHAM_CONG_THUC"
    const val TINH_NHAM = "TINH_NHAM"
    const val THIEU = "THIEU"
    const val LAC_DE = "LAC_DE"
    const val KHAC = "KHAC"

    /** Ten tieng Viet, theo dung thu tu muon doc len khi hai nhan bang diem nhau. */
    private val TEN = linkedMapOf(
        SAI_DAU to "sai dấu",
        SAI_BUOC to "sai một bước biến đổi",
        NHAM_CONG_THUC to "nhầm công thức",
        TINH_NHAM to "tính nhầm số",
        THIEU to "thiếu trường hợp hoặc bỏ dở",
        LAC_DE to "lạc đề",
        KHAC to "kiểu khác"
    )

    val TAP: Set<String> = TEN.keys

    fun moTa(nhan: String): String = TEN[nhan] ?: TEN.getValue(KHAC)

    /**
     * Ep nhan may tra ve ve dung [TAP].
     *
     * Cau dung thi khong co loi nao, du may co khai gi: khong chan o day thi bang
     * thong ke co ca nhung cau lam dung nam trong cot "sai dau".
     */
    fun doc(chu: String?, dung: Boolean): String {
        if (dung) return ""
        val n = chu.orEmpty().trim().uppercase()
        if (n.isEmpty()) return ""
        return if (n in TAP) n else KHAC
    }
}

/**
 * Toan bo ket qua AI cham mot lan nop.
 *
 * @param ngayDanDo ngay ghi trong vo dan do, dang yyyy-MM-dd. Null la khong chup
 *   vo dan do, hoac chup ma khong doc duoc ngay.
 * @param lamHetDanDo AI doi chieu bai lam voi vo dan do: da lam het chua.
 * @param baiDuocGiao ten cac bai tap ma vo dan do giao phai lam: "bai 2", "SBT 2.26".
 *   Rong nghia la vo dan do khong giao bai tap nao - chi dan viec khong nop duoc
 *   bai ("mang sach vo day du", "tiet sau kiem tra", "hoc thuoc"). Luc do khong co
 *   "lam het bai co giao" de ma tra tron goi; xem [LuatCongGio].
 */
data class KetQuaCham(
    val mon: String = "",
    val cac: List<CauCham> = emptyList(),
    val ngayDanDo: String? = null,
    val lamHetDanDo: Boolean = false,
    val baiDuocGiao: List<String> = emptyList(),
    val tomTat: String = ""
)
