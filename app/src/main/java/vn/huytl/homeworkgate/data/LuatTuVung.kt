package vn.huytl.homeworkgate.data

import vn.huytl.homeworkgate.kho.BuoiDo
import vn.huytl.homeworkgate.kho.Chieu
import vn.huytl.homeworkgate.kho.HocToi
import kotlin.random.Random

/**
 * Luat cua duong TU VUNG: may hoi, con go thang tren tablet, may cham ngay.
 *
 * Ten dat theo [LuatCongGio] cho cung mot lo, va de khong dung ten voi kieu du lieu
 * [vn.huytl.homeworkgate.kho.TuVung] von la MOT TU, con day la LUAT.
 *
 * VI SAO CO DUONG NAY. Ca app do cong suc bang so dong con viet tren giay, ma hoc
 * tu vung thi khong de ra dong nao. Truoc do [DangBai.KHONG_TINH] tra 0 phut cho no,
 * va cai im lang do noi voi dua tre rang hoc tu vung khong dang gi.
 *
 * MUC DICH KHONG PHAI DO XEM CON THUOC BAO NHIEU. Muc dich la moi ngay con deu phai
 * loi tung tu ra khoi dau mot lan. Viec loi ra do co tac dung hon nhieu so voi doc
 * lai bang tu vung, ke ca khi loi khong ra roi moi nhin dap an - nen mot buoi do
 * nam phut hon han muoi phut ngoi doc.
 *
 * MOT BUOI MOI NGAY, xem [BuoiDo]: boc ngau nhien trong cac Unit con chon la lop da hoc,
 * xem [daHoc]. Truoc 29/9/2026 chu thich nay ta them hai buoi neo vao Unit co giao trong vo
 * dan do (buoi toi va sang hom sau), nhung man Do tu vung chua bao gio lam hai buoi do; Ba
 * Huy chon bo phan thua thay vi lam tiep.
 *
 * KHONG BUOI NAO BAT BUOC. Con muon lam thi lam, bo tu nao thi mat phut cua tu do,
 * khong mat gi khac. Bat buoc thi phai co hinh phat, ma hinh phat o day la cat gio
 * choi, tuc la lay mot thu con da lam ra de doi lay mot thu con khong lam - do la
 * hai viec khac nhau va tron chung thi hong ca hai.
 */
object LuatTuVung {

    // ---------------------------------------------------------------- so phut

    /**
     * Mot tu dung du so lan duoc bay nhieu giay.
     *
     * Mot phut, Ba Huy doi ngay 23/9/2026. Truoc do la nua phut. Phai bang gia mot
     * the o [HocThuoc.GIAY_MOI_THE], ly do ghi o do.
     */
    const val GIAY_MOI_TU = 60

    /**
     * Mot cau ngu phap duoc bay nhieu giay.
     *
     * Gap doi mot tu, va phai gap doi. Mot tu go mat muoi tam giay, mot cau ngu phap
     * phai doc het cau roi nghi roi chon, mat ba muoi den bon lam giay. Tra bang
     * nhau thi con bo ngu phap di lam tu vung, vi cung mot gia ma tu vung nhanh gap
     * ba - va luc do phan ngu phap nam do khong ai dung.
     */
    const val GIAY_MOI_CAU_NGU_PHAP = 120

    /**
     * Mot tu phai dung bay nhieu lan TRONG MOT BUOI moi duoc tra giay. Man kiem tra bai
     * dung chung so nay cho moi the.
     *
     * Mot: dung ngay lan dau la tinh gio luon, chi tu sai moi phai lam lai cho toi khi
     * dung. Truoc 27/9/2026 la hai, lan hai hoi lai cach muoi tu de biet con nho that chu
     * khong phai chep lai cai vua nhin; Ba Huy bo lan hoi lai do. [KhoBai.tinhTrangTu]
     * cung doc so nay: buoi nao tu dat du so lan dung la buoi do tu da xong.
     */
    const val LAN_DUNG_DE_TINH = 1

    /**
     * Mot buoi hang ngay toi da bay nhieu tu.
     *
     * Hai muoi tu, moi tu mot phut, la hai muoi phut; go hai muoi tu mat khoang nam
     * phut.
     * Dai hon thi buoi do thanh bai tap, ma bai tap thi dua tre ne.
     */
    const val SO_TU_HANG_NGAY = 20

    /**
     * So cau ngu phap chen vao buoi hang ngay.
     *
     * Chen vao chu khong de thanh mot muc rieng con bam vao: de rieng thi con khong
     * bao gio bam, vi cung mot phut ma ngu phap nghi lau hon.
     */
    const val SO_CAU_NGU_PHAP_HANG_NGAY = 5

    /** Tong giay toi da mot buoi hang ngay co the tra. */
    const val GIAY_TOI_DA_HANG_NGAY =
        SO_TU_HANG_NGAY * GIAY_MOI_TU + SO_CAU_NGU_PHAP_HANG_NGAY * GIAY_MOI_CAU_NGU_PHAP

    /**
     * Tran ngay cua duong tu vung: dung bang MOT buoi.
     *
     * Mot ngay mot buoi, het buoi la het phan tu vung cua ngay. Noi long ra thi
     * buoi thu hai boc phai chinh nhung tu vua do xong nam phut truoc - trong so
     * co ha chung xuong that, nhung ha xuong khong phai bang khong - va luc do no
     * tra gio cho viec go lai cai vua nhin. Muon them gio thi con qua duong khac,
     * moi duong mot cai tran rieng la co y.
     */
    const val GIAY_TRAN_MOI_NGAY = GIAY_TOI_DA_HANG_NGAY

    /**
     * Doi giay sang phut de cap gio.
     *
     * Lam tron XUONG, phan du bo di. Tu 23/9/2026 moi tu tron mot phut va moi cau
     * ngu phap tron hai phut nen so giay trong ngay luon chia het cho 60; phan du chi
     * con tu nhung luot ghi truoc do (nua phut mot tu), va bo nua phut le khong ai
     * thac mac. Giu lai phan du sang buoi sau thi phai co mot cai so no, ma mot cai
     * so no nua phut thi khong bo cong.
     */
    fun phutTu(giay: Int): Int = (giay / 60).coerceAtLeast(0)

    /** So phut ca ngay dang duoc, tu tong giay da hoc trong ngay. Chan tran truoc khi chia. */
    fun phutTrongNgay(giay: Int): Int = phutTu(giay.coerceAtMost(GIAY_TRAN_MOI_NGAY))

    /**
     * So phut cong cho mot buoi: phan CHENH cua ca ngay truoc va sau buoi do.
     *
     * Giong het [vn.huytl.homeworkgate.data.HocThuoc.phutThem] va co y giong: hai
     * duong deu dem bang giay, nen phan le duoi mot phut deu nam lai trong ngay chu
     * khong mat. Hai cach tinh khac nhau cho cung mot viec thi som muon co ngay mot
     * ben duoc it hon ma khong ai giai thich noi tai sao.
     */
    fun phutThem(giayTruoc: Int, giayBuoi: Int): Int =
        phutTrongNgay(giayTruoc + giayBuoi) - phutTrongNgay(giayTruoc)

    // ------------------------------------------------------ dang hoc unit nao

    /**
     * Mot tu co nam trong phan lop da hoc khong: Unit cua tu nam trong cac Unit con da danh
     * dau ([unitDaHoc]). Truoc 2/10/2026 con chon "da hoc toi Unit N" va moi Unit tu 1 toi N
     * la da hoc; nay con danh dau tung Unit (Ba Huy chot), moc cu tu chuyen thanh Unit 1-N.
     *
     * Con tu chon, xem [HocToi]. Truoc 25/9/2026 may doan Unit theo lich: dem tiet Tieng
     * Anh tu ngay khai giang, chia deu muoi hai Unit cho ca nam, lui mot tuan cho chac.
     * Lop that khong chay deu (tuan on tap, tuan kiem tra, Unit kho day cham hon), nen
     * lop cham hon nhip chia deu mot chut la may hoi tu chua hoc bao gio. Ba Huy chon bo
     * han cach doan.
     *
     * Van boc tu CA cac Unit da danh dau chu khong chi Unit dang hoc: tu Unit 1 phai
     * quay lai ca nam. Tap nay chi chan mot viec - hoi tu cua Unit lop chua hoc.
     *
     * Tu ghi unit 0 la ban chep hong, xem [vn.huytl.homeworkgate.kho.TuVung.unit], va
     * chung KHONG bi loc khi con da hoc it nhat mot Unit: mot cho hong trong file khong
     * duoc lam mat han mot tu khoi duong hoc. Con chon "chua hoc Unit nao" (tap rong) thi
     * khong tu nao lot qua, ke ca tu do.
     */
    fun daHoc(unitTu: Int, unitDaHoc: Set<Int>): Boolean =
        unitDaHoc.isNotEmpty() && (unitTu <= 0 || unitTu in unitDaHoc)

    // ------------------------------------------------------------------ cham

    /**
     * Con go dung tu chua. Chi dung cho chieu [Chieu.VIET_ANH].
     *
     * Dung chung [HocThuoc.chuanHoa] voi the hoc thuoc chu khong viet mot ham thu
     * hai: hai ham chuan hoa thi den luc sua se chi sua mot, va cai con lai im lang
     * cham theo luat cu.
     */
    fun dung(go: String, tu: String): Boolean {
        val g = HocThuoc.chuanHoa(go)
        return g.isNotEmpty() && g == HocThuoc.chuanHoa(tu)
    }

    // ------------------------------------------------------------------ goi y

    /*
     * Bac goi y sau moi lan sai (so chu cai, chu dau, nua tu, hai phan ba, ca tu) tung nam o
     * day, dung chung cho man do tu vung va man kiem tra bai. Tu 2/10/2026 hai man do sang ban
     * phim ghep, va goi y la mo phim nhieu roi dien san nua dau dap an: xem [PhimKiemTra].
     */

    // ----------------------------------------------------------- chon tu nao

    /**
     * Trong so khi boc ngau nhien, theo TINH TRANG cua tu chu khong theo moi cu.
     *
     * VI SAO KHONG CHIA "moi" va "cu" roi cho moi gap doi. Lam phep tinh voi con so
     * that: kho ba tram tu, moi ngay boc hai muoi, thi mot tu trung binh muoi lam
     * ngay gap lai mot lan. Them tu lop sau lop bay vao thanh chin tram tu thi con
     * so do la bon lam ngay. Cang nhieu tu cang loang, va TU CON VUA SAI HOM NAY
     * cung phai cho bon lam ngay moi gap lai - dung luc da quen sach.
     *
     * Cho moi gap doi khong go duoc, vi no chi nang tu moi len chu khong ha tu da
     * thuoc xuong. Bang nay ha: mot tu dung ba lan tro len chi con mot phan tu suat
     * so voi tu dung hai lan, nen cho cua no nhuong lai cho tu dang trat.
     *
     * VAN CHUA DU, va do la ly do co [SO_GHIM_TU_SAI]. Ngau nhien khong bao gio dua
     * duoc mot tu quay lai vao dung ngay mai, du trong so cao den may.
     */
    const val TRONG_SO_VUA_SAI = 8.0
    const val TRONG_SO_CHUA_GAP = 4.0
    const val TRONG_SO_DUNG_1 = 2.0
    const val TRONG_SO_DUNG_2 = 1.0
    const val TRONG_SO_DA_THUOC = 0.25

    /**
     * Bao nhieu o trong buoi hang ngay danh rieng cho tu SAI O BUOI TRUOC.
     *
     * Ghim cung, khong qua boc. Sau o tren hai muoi: du de tu vua sai chac chan quay
     * lai ngay mai, chua du de mot ngay sai nhieu bien buoi hom sau thanh buoi tra
     * bai cu.
     */
    const val SO_GHIM_TU_SAI = 6

    /** Tinh trang mot tu, chi de tra trong so. */
    enum class TinhTrang { VUA_SAI, CHUA_GAP, DUNG_1, DUNG_2, DA_THUOC }

    fun trongSo(t: TinhTrang): Double = when (t) {
        TinhTrang.VUA_SAI -> TRONG_SO_VUA_SAI
        TinhTrang.CHUA_GAP -> TRONG_SO_CHUA_GAP
        TinhTrang.DUNG_1 -> TRONG_SO_DUNG_1
        TinhTrang.DUNG_2 -> TRONG_SO_DUNG_2
        TinhTrang.DA_THUOC -> TRONG_SO_DA_THUOC
    }

    /**
     * Boc [soLuong] phan tu theo trong so, khong lay trung.
     *
     * Boc khong hoan lai: mot buoi hoi cung mot tu hai lan thi lan hai vo nghia, ma
     * con thi thay ngay va biet may dang boc bua.
     *
     * Tra ve theo thu tu boc duoc, KHONG sap lai theo thu tu trong sach. Hoi theo
     * thu tu sach thi con nho duoc theo mach - tu nay xong den tu ke - ma cai do la
     * nho vi tri chu khong phai nho nghia.
     */
    fun <T> bocTheoTrongSo(
        cac: List<Pair<T, Double>>,
        soLuong: Int,
        rnd: Random = Random.Default
    ): List<T> {
        val con = cac.filter { it.second > 0.0 }.toMutableList()
        val ra = mutableListOf<T>()
        repeat(soLuong.coerceAtMost(con.size)) {
            val tong = con.sumOf { it.second }
            var moc = rnd.nextDouble() * tong
            var i = 0
            while (i < con.size - 1) {
                moc -= con[i].second
                if (moc <= 0.0) break
                i++
            }
            ra += con.removeAt(i).first
        }
        return ra
    }

    /**
     * Chieu hoi mot tu: chua bao gio dung thi cho nhan mat chu da.
     *
     * Bat go mot tu chua he thay bao gio la bat lam mot viec khong lam duoc, va con
     * chi hoc duoc dieu duy nhat la minh dot. Cho nhan mat mot lan roi moi bat go.
     */
    fun chieuCho(soLanDung: Int): Chieu =
        if (soLanDung <= 0) Chieu.ANH_VIET else Chieu.VIET_ANH

    // ------------------------------------------------------------ trac nghiem

    /** So lua chon cua mot cau trac nghiem. */
    const val SO_LUA_CHON = 4

    /**
     * Chon moi nhu cho mot cau trac nghiem: cung Unit, cung loai tu.
     *
     * Cung loai tu moi la moi nhu that. Lay bua thi mot cau hoi nghia cua mot dong
     * tu se co ba moi nhu la danh tu, va con loai tru duoc ma khong can biet nghia -
     * luc do bai kiem tra do nham thu.
     *
     * Khong du moi cung Unit cung loai thi noi ra: cung loai bat ky Unit nao truoc,
     * roi moi den cung Unit khac loai. Van khong du thi tra ve it hon - mot cau ba
     * lua chon van hon mot cau co moi nhu lo lieu.
     */
    fun <T> chonMoiNhu(
        dung: T,
        cungUnitCungLoai: List<T>,
        cungLoai: List<T>,
        cungUnit: List<T>,
        rnd: Random = Random.Default
    ): List<T> {
        val ra = linkedSetOf(dung)
        listOf(cungUnitCungLoai, cungLoai, cungUnit).forEach { nguon ->
            nguon.shuffled(rnd).forEach { if (ra.size < SO_LUA_CHON) ra += it }
        }
        return ra.toList().shuffled(rnd)
    }

    /**
     * Mot nghia co lam moi nhu duoc cho [nghiaDung] khong.
     *
     * VI SAO CAN. Mot Unit chi co hai muoi tu, va sach xep cac tu cung chu de nam
     * canh nhau: Unit 1 co fond "men, thich", keen "say me, ham thich", crazy "rat
     * thich, qua say me". Lay ca ba lam lua chon cua mot cau thi con khong tra loi
     * duoc bang kien thuc, chi doan - va doan sai thi mat luot ma khong hoc duoc gi.
     * Cho nay lo ra ngay buoi do dau tien tren may that.
     *
     * Phep loc tho: chung mot tieng nao la bo. Tho that, nhung no bat dung cai can
     * bat, va bat hut mot hai cau thi cung chi la cau do de hon mot chut. Lam tinh
     * hon thi phai biet nghia cua tieng Viet, ma do la viec cua mot cai tu dien chu
     * khong phai cua mot ham trong app cham bai.
     *
     * Loc het sach thi [chonMoiNhu] tra ve it lua chon hon chu khong hong.
     */
    fun moiNhuDuoc(nghiaDung: String, nghia: String): Boolean =
        (tiengCua(nghiaDung) intersect tiengCua(nghia)).isEmpty()

    /** Cac tieng co nghia trong mot chuoi, da bo dau cau va tieng mot chu. */
    private fun tiengCua(nghia: String): Set<String> =
        nghia.lowercase().split(Regex("[^\\p{L}]+")).filter { it.length >= 2 }.toSet()
}
