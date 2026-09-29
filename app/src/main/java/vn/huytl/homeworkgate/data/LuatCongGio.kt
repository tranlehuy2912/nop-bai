package vn.huytl.homeworkgate.data

/**
 * Luat quy bai lam ra so phut choi, va tran rieng cua tung phan.
 *
 * TU 29/9/2026 KHONG CON TRAN CHUNG. Ba Huy bo tran ngay 135 phut; moi phan co tran
 * rieng, va tran ngay bang tong cac tran do ([TRAN_NGAY]):
 *
 *  - duong chup anh (bai co giao, ke ca bai co giao le va "Bài khác"): toi da
 *    [TRAN_ANH] phut;
 *  - bai lam tren may (lam them, luyen cho hay vap, Giai de): [TRAN_TREN_MAY];
 *  - on lai theo hen, cung lam tren may: [TRAN_ON_MOI_NGAY];
 *  - Kiem tra bai: [HocThuoc.TRAN_PHUT_MOI_NGAY]; Do tu vung: tran cua [LuatTuVung].
 *
 * Phut lam tren may ma bi tran cat thi vao "Quỹ giờ chơi" ([QuyGio]); duong chup anh thi
 * khong. Gio sao cua bai lam tren may o [LuatGhep]; file nay chi con luat cua duong chup
 * anh.
 *
 * DUONG CHUP ANH CHI CON BAI CO GIAO. Bai co giao le, phieu photo, bai mon chua co sach
 * deu la bai co giao (Ba Huy chot 29/9/2026). Moi cau tinh le: mot dong mot phut, trac
 * nghiem mot phut mot cau, khong san khong tran tung cau.
 *
 * KHONG CON TRON GOI (Ba Huy bo ngay 30/9/2026). Truoc do lam het bai trong vo dan do thi
 * duoc tron goi 45 phut, va vo dan do song toi trua hom sau de may biet bai nao la bai co
 * giao. Tu do vo dan do chi con de nhac bai, xem [NhacBai].
 *
 * Moi muc chi sinh gio MOT lan trong doi. Cau sai thi khong duoc gi; sua dung roi nop
 * lai thi luc do moi tinh, va tinh dung mot lan. Viec nho "muc nay tra gio chua" la cua
 * so cai bai da nop, khong phai cua ham nay.
 */
object LuatCongGio {

    /**
     * Tran ca ngay cua duong chup anh.
     *
     * Truoc 30/9/2026 day cung la gia tron goi vo dan do, va tinh le voi tron goi chung tran
     * nay. Bo tron goi thi giu nguyen tran. Truoc 29/9/2026 bai lam them chup anh co tran
     * rieng 90 phut; tu do bai lam them chi con tren may.
     */
    const val TRAN_ANH = 45

    /**
     * Bai chup anh tinh le: mot dong lam bai duoc mot phut, ca bai tap lan bai viet dai.
     *
     * Do CONG SUC NHIN THAY DUOC chu khong do "do kho": so dong dem duoc, kiem lai duoc
     * bang chinh tam anh. Truoc 29/9/2026 co san 4 phut mot cau, tran 20 phut mot cau, va
     * bai viet dai tinh ba dong bon phut; Ba Huy bo het san va tran tung cau, de chung
     * tran [TRAN_ANH] cua ca phan.
     */
    const val DONG_MOI_PHUT = 1

    /** Trac nghiem chup anh: moi cau dung mot phut, khong con tran moi lan nop (29/9/2026). */
    const val PHUT_MOI_CAU_TRAC_NGHIEM = 1

    /**
     * Tran moi ngay cua bai lam tren may: lam them, luyen cho hay vap, Giai de.
     *
     * Khong co tran thi mot quyen bai tap nang cao la ca buoi toi choi game. Phan vuot
     * tran khong mat: vao "Quỹ giờ chơi", Ba Huy cap khi nao thi con choi khi do.
     */
    const val TRAN_TREN_MAY = 90

    /**
     * Tran rieng cho duong ON LAI tren may, moi ngay.
     *
     * On lai la mot phan rieng voi tran rieng (Ba Huy chot 29/9/2026), khong nam trong
     * [TRAN_TREN_MAY]. Truoc do on lai nam trong tran lam them. Ba muoi phut: du de on het
     * so cau den hen cua mot ngay binh thuong (lich hen 3/10/20/30 ngay - xem
     * [vn.huytl.homeworkgate.kho.KhoBai.KHOANG_HEN_NGAY]), khong du de thay bai moi.
     */
    const val TRAN_ON_MOI_NGAY = 30

    /**
     * Tran ngay: tong tran cua moi phan. Chi de hien ra man hinh ("hôm nay kiếm được
     * 60/215 phút"), khong chan gi, vi tung phan da tu chan. Ba Huy chot 215 phut ngay
     * 29/9/2026.
     */
    val TRAN_NGAY: Int
        get() = TRAN_ANH + TRAN_TREN_MAY + TRAN_ON_MOI_NGAY + HocThuoc.TRAN_PHUT_MOI_NGAY +
            LuatTuVung.phutTrongNgay(LuatTuVung.GIAY_TRAN_MOI_NGAY)

    /**
     * So phut cua mot muc chup anh lam dung, theo so dong lam bai.
     *
     * [CauCham.soDong] bang 0 nghia la Claude khong ghi so dong (quen truong, bai ve, bai
     * dien bang). Truoc 29/9/2026 luc do tra muc du phong 4 hay 10 phut; bo san roi thi
     * muc do thanh nguoc doi (cau quen dem duoc nhieu hon cau mot dong that), nen cau do
     * ra 0 va ca bai cho Ba Huy xem - xem [BangTinh.canBaHuyXem].
     */
    fun phutChoCau(cau: CauCham): Int = when {
        // Khong co de thi khong ai cham duoc, nen khong tra gio. Xem [CauCham.coDe].
        !cau.coDe -> 0
        cau.dang == DangBai.KHONG_TINH -> 0
        // Trac nghiem khong co gia rieng tung cau, tinh theo cum trong [tinh].
        cau.dang == DangBai.TRAC_NGHIEM -> 0
        cau.soDong <= 0 -> 0
        else -> cau.soDong / DONG_MOI_PHUT
    }

    /** Cau lam dung ma Claude khong ghi so dong: khong tu cap, cho Ba Huy chon so dong. */
    fun thieuSoDong(cau: CauCham): Boolean =
        cau.dung && cau.coDe && cau.soDong <= 0 &&
            cau.dang != DangBai.TRAC_NGHIEM && cau.dang != DangBai.KHONG_TINH

    /** Ket qua tinh, kem dong giai thich de ghi ra Telegram va man hinh con. */
    data class BangTinh(
        val phut: Int,
        val dong: List<String>,
        /** Co cho nao AI khong doc ro khong. Co thi dung tu duyet. */
        val canBaHuyXem: Boolean,
        /**
         * Cau nao duoc tinh le va duoc dung may phut, theo [CauCham.ma].
         *
         * So cai phai ghi dung con so DA TRA, khong phai gia niem yet cua cau. Hai
         * con so nay khac nhau that: cau bi tran cat cut chi duoc phan con lai.
         */
        val phutCua: Map<String, Int> = emptyMap(),
        /** Cau lam dung ma Claude khong ghi so dong, xem [thieuSoDong]. */
        val thieuDong: List<CauCham> = emptyList()
    )

    /**
     * Quy mot lan cham bai chup anh ra so phut.
     *
     * @param daCongAnhHomNay so phut duong chup anh da cong trong ngay, de giu [TRAN_ANH].
     *   Ben goi lay tu so cai, xem [SoCaiBai.phutAnhHomNay].
     */
    fun tinh(
        goc: KetQuaCham,
        daCongAnhHomNay: Int = 0
    ): BangTinh {
        val dong = mutableListOf<String>()

        /*
         * Cau khong co de thi coi nhu chua dung, du may co khai gi.
         *
         * Cau lenh da dan "khong co de thi bat buoc dung: false", nhung mot cau tra
         * loi lech mot truong khong duoc phep bien thanh mot lan cong gio. Chan ngay
         * o day, mot cho, de moi phep tinh ben duoi - tung cau, cum trac nghiem - deu
         * khong phai nho luat nay lan nua.
         */
        val ket = goc.copy(cac = goc.cac.map { if (it.coDe) it else it.copy(dung = false) })

        val conTran = (TRAN_ANH - daCongAnhHomNay).coerceAtLeast(0)

        val tinhLe = ket.cac.filter { it.dung }
        val thieuDong = tinhLe.filter(::thieuSoDong)

        // Trac nghiem gom thanh mot cum, phan con lai tinh tung cau.
        val tracNghiem = tinhLe.filter { it.dang == DangBai.TRAC_NGHIEM }
        val phutCum = tracNghiem.size * PHUT_MOI_CAU_TRAC_NGHIEM
        val phutTungCau = tinhLe.filterNot { it.dang == DangBai.TRAC_NGHIEM }
            .map { it to phutChoCau(it) }
            .filter { it.second > 0 }

        // Cat theo tung cau chu khong cat cuc tong, de con biet cau nao that su duoc
        // tra bao nhieu ma ghi vao so.
        val duoc = mutableListOf<Pair<CauCham, Int>>()
        var le = 0
        var biCat = false
        for ((cau, p) in phutTungCau) {
            val them = p.coerceAtMost(conTran - le)
            if (them < p) biCat = true
            if (them <= 0) break
            duoc += cau to them
            le += them
        }

        // Ghi ro tung cau duoc may phut, nhung dai qua thi gom lai: mot lan nop hai
        // muoi cau ma liet ke het thi tin nhan Telegram thanh mot man chu.
        duoc.take(SO_DONG_KE_TOI_DA).forEach { (cau, p) ->
            dong += "${cau.ma} (${cau.soDong} dòng): +$p phút"
        }
        if (duoc.size > SO_DONG_KE_TOI_DA) {
            val con = duoc.drop(SO_DONG_KE_TOI_DA)
            dong += "…và ${con.size} câu nữa: +${con.sumOf { it.second }} phút"
        }

        // Cum trac nghiem: tra sau cac cau co loi giai, va cung phai qua tran.
        var phutCumThat = 0
        if (phutCum > 0) {
            phutCumThat = phutCum.coerceAtMost((conTran - le).coerceAtLeast(0))
            if (phutCumThat < phutCum) biCat = true
            if (phutCumThat > 0) {
                le += phutCumThat
                dong += "${tracNghiem.size} câu trắc nghiệm đúng: +$phutCumThat phút"
            }
        }

        if (biCat) {
            dong += "Bài chụp ảnh mỗi ngày tối đa $TRAN_ANH phút, hôm nay đã đủ"
        }

        val sai = ket.cac.filter { !it.dung }
        if (sai.isNotEmpty()) {
            dong += "Chưa tính: " + sai.joinToString(", ") { it.ma } + " (sửa lại rồi nộp tiếp)"
        }

        val mo = ket.cac.filter { !it.docRo }
        if (mo.isNotEmpty()) {
            dong += "Máy đọc không rõ: " + mo.joinToString(", ") { it.ma } + " — nhờ ba Huy xem"
        }
        if (thieuDong.isNotEmpty()) {
            dong += "Claude chưa ghi số dòng: " + thieuDong.joinToString(", ") { it.ma } +
                " (nhờ ba Huy chọn số dòng)"
        }

        return BangTinh(
            phut = le,
            dong = dong,
            canBaHuyXem = mo.isNotEmpty() || thieuDong.isNotEmpty(),
            phutCua = duoc.associate { (cau, p) -> cau.ma to p } +
                chiaDeu(tracNghiem, phutCumThat),
            thieuDong = thieuDong
        )
    }

    /**
     * Chia so phut tablet cap THAT cho tung cau, khi cap khong du [BangTinh.phut] (vi du
     * phan con lai cua mot phieu cat o gio chot trong ngay).
     *
     * So cai phai ghi so da tra, khong phai gia niem yet, xem [BangTinh.phutCua]. Chia theo
     * thu tu trong [BangTinh.phutCua]; cau het phan thi duoc 0 phut. Cap du thi tra ve dung
     * [BangTinh.phutCua].
     */
    fun chiaPhutDaCap(bang: BangTinh, daCap: Int): Map<String, Int> {
        var con = daCap.coerceAtLeast(0)
        return bang.phutCua.mapValues { (_, p) -> minOf(p, con).also { con -= it } }
    }

    /** Ke ro toi da bay nhieu cau, con lai gom mot dong. */
    private const val SO_DONG_KE_TOI_DA = 6

    /**
     * Chia so phut cua mot cum cho tung cau trong cum.
     *
     * So cai ghi theo tung cau - can the de lan sau nop lai biet cau nao da tra gio
     * roi. Chia deu, phan du rai cho may cau dau, de cong lai van dung so phut da
     * tra that chu khong phinh len.
     */
    private fun chiaDeu(cac: List<CauCham>, phut: Int): Map<String, Int> =
        chiaDeuTheoMa(cac.map { it.ma }, phut)

    /** Chia deu [phut] cho [cacMa], phan du cho may ma dau. Dung chung voi Giai de. */
    internal fun chiaDeuTheoMa(cacMa: List<String>, phut: Int): Map<String, Int> {
        if (cacMa.isEmpty()) return emptyMap()
        val moi = phut / cacMa.size
        var du = phut % cacMa.size
        return cacMa.associateWith {
            val them = moi + if (du > 0) 1 else 0
            if (du > 0) du--
            them
        }
    }
}
