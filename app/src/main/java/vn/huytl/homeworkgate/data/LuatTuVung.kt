package vn.huytl.homeworkgate.data

import java.util.Calendar
import vn.huytl.homeworkgate.kho.BuoiDo
import vn.huytl.homeworkgate.kho.Chieu
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
 * BA BUOI, xem [BuoiDo]. Buoi hang ngay boc ngau nhien tu ca kho. Hai buoi kia neo
 * vao Unit co giao trong vo dan do, mot buoi toi va mot buoi sang hom sau.
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
     * Mot tu phai dung bay nhieu lan TRONG MOT BUOI moi duoc tra giay.
     *
     * Hai, va lan thu hai khong duoc hoi ngay sau lan thu nhat - xem [chenLai]. Hoi
     * ngay thi con con nho nguyen tu cai vua nhin, dung gan chac chan, va lan hai
     * khong do them gi ngoai viec nhan doi so lan go.
     */
    const val LAN_DUNG_DE_TINH = 2

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
     * Lui may ngay khi tinh Unit dang hoc.
     *
     * VI SAO PHAI LUI. Nhip tinh ra o [unitDangHoc] chia deu ca nam, con lop that
     * thi khong chay deu: co tuan on tap, co tuan kiem tra an mat tiet, co Unit kho
     * co day cham hon. Cang ve cuoi nam sai so cang don. App cung khong biet ngay
     * nghi rieng cua truong - [NgayNghi] chi co le quoc gia, chinh no ghi ro nhu vay
     * - ma moi ngay nghi khong biet lam moc that lui lai trong khi moc tinh thi
     * khong lui.
     *
     * Lech hai chieu khong nang nhu nhau. App cham hon lop thi con bi hoi tu cu,
     * tuc la on lai, khong hai gi. App nhanh hon lop thi hoi tu chua hoc bao gio, va
     * con ngoi chiu tran. Mot tuan dem de day han ve phia thu nhat.
     */
    const val NGAY_LUI_MOC_UNIT = 7

    /**
     * So tiet cua mot mon trong khoang [tu] den [den], da tru ngay nghi.
     *
     * Dem tu chinh [ThoiKhoaBieu] chu khong go mot con so: sua thoi khoa bieu thi
     * nhip mo Unit tu di theo, khong phai nho sua them cho nao.
     */
    fun soTietCua(mon: String, tu: Calendar, den: Calendar): Int {
        var n = 0
        val d = tu.clone() as Calendar
        while (!d.after(den)) {
            if (!NgayNghi.laNgayNghi(d)) {
                n += ThoiKhoaBieu.buoiHocCua(d.get(Calendar.DAY_OF_WEEK))
                    .sumOf { buoi -> buoi.monTheoTiet.values.count { it == mon } }
            }
            d.add(Calendar.DAY_OF_MONTH, 1)
        }
        return n
    }

    /**
     * Lop dang hoc toi Unit may, tinh theo lich chu khong hoi ai.
     *
     * Chia deu [soUnit] Unit cho tong so tiet cua mon do ca nam. Voi Tieng Anh 8 la
     * ba tiet mot tuan, 109 tiet ca nam, 12 Unit - ra khoang chin tiet moi Unit, tuc
     * ba tuan, dung bang phan phoi chuong trinh that.
     *
     * Con so nay dung lam TRAN chu khong phai de ep: buoi do van boc theo trong so
     * trong tat ca cac Unit tu 1 den day, nen tu Unit 1 quay lai ca nam. No chi chan
     * mot viec - hoi tu cua Unit ma lop chua toi.
     *
     * Khong dem duoc tiet nao (thoi khoa bieu khong co mon do) thi mo het: tha hoi
     * rong con hon khoa sach ca quyen ma khong ai hieu tai sao.
     */
    fun unitDangHoc(
        mon: String,
        soUnit: Int,
        luc: Calendar = Calendar.getInstance()
    ): Int {
        if (soUnit <= 0) return 0
        val dau = ThoiKhoaBieu.ngayBatDauNamHoc()
        val cuoi = NgayNghi.ngayHocCuoiCung()
        val tong = soTietCua(mon, dau, cuoi)
        if (tong <= 0) return soUnit

        val moc = (luc.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -NGAY_LUI_MOC_UNIT)
        }
        if (moc.before(dau)) return 1
        val daHoc = soTietCua(mon, dau, if (moc.after(cuoi)) cuoi else moc)
        // Lam tron LEN: hoc duoc mot tiet cua Unit nao la Unit do da mo. Lam tron
        // xuong thi ca tuan dau nam khong co Unit nao mo ra ca.
        val u = (daHoc * soUnit + tong - 1) / tong
        return u.coerceIn(1, soUnit)
    }

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

    /**
     * Sau moi lan sai thi ho ra them mot chut, KHONG hien thang dap an.
     *
     * VI SAO KHONG HIEN DAP AN. Hien ra thi con go bua mot cai, doc dap an, go lai
     * cho dung, va an tron so giay ma khong nho gi. Bat con tu tim thi con phai mo
     * sach hay di hoi - va do chinh la viec hoc.
     *
     * VI SAO VAN PHAI HO DAN. Buoi do hang ngay boc ca nhung tu cua Unit tu may
     * thang truoc. Con khong biet tu do nam o dau ma tra, tablet thi dang khoa nen
     * khong co tu dien, va tam gio toi thi co khi khong ai de hoi. Khong ho gi ca la
     * con ket cung o mot tu, va mot tuan nhu the la con thoi mo app.
     *
     * Bac 0 khong ho gi. Bac 1 cho biet tu dai may chu. Bac 2 hien chu cai dau. Bac 3
     * hien nua tu. Het bac thi con van con nut "Chịu rồi" de di tiep.
     */
    fun goiY(tu: String, bac: Int): String {
        val t = tu.trim()
        return when {
            bac <= 0 -> ""
            bac == 1 -> "${t.count { !it.isWhitespace() }} chữ cái"
            bac == 2 -> t.first() + "…"
            else -> t.take((t.length + 1) / 2) + "…"
        }
    }

    /** Het bac goi y thi thoi, khong ho them. */
    const val BAC_GOI_Y_TOI_DA = 3

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

    /**
     * Sau lan tra loi dau, tu do quay lai sau bay nhieu tu khac trong cung buoi.
     *
     * Khong hoi lai ngay. Hoi ngay thi cai vua nhin con nam nguyen trong dau, dung
     * gan chac chan, va lan hai khong do them gi. Cach ra muoi tu thi lan hai la nho
     * that. Buoi ngan hon muoi tu thi day xuong cuoi buoi.
     */
    const val CHEN_LAI = 10

    /** Vi tri chen lai tu vua tra loi, tinh tu vi tri hien tai. */
    fun chenLai(viTriHienTai: Int, soConLai: Int): Int =
        viTriHienTai + minOf(CHEN_LAI, soConLai.coerceAtLeast(1))

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

    /**
     * Buoi do nay co tra giay khong.
     *
     * [BuoiDo.DAN_DO_TOI] thi khong: no nam trong tron goi 45 phut cua bai co giao,
     * tra them nua la tra hai lan cho mot viec. Hai buoi kia tra theo tung tu.
     */
    fun coTraGiay(buoi: BuoiDo): Boolean = buoi != BuoiDo.DAN_DO_TOI
}
