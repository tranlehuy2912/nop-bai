package vn.huytl.homeworkgate.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Luat quy bai lam ra so phut choi, va tran rieng cua tung phan.
 *
 * TU 29/9/2026 KHONG CON TRAN CHUNG. Ba Huy bo tran ngay 135 phut; moi phan co tran
 * rieng, va tran ngay bang tong cac tran do ([TRAN_NGAY]):
 *
 *  - duong chup anh (bai trong vo dan do, ke ca bai co giao le va "Bài khác"): toi da
 *    [TRAN_ANH] phut, tron goi hay tinh le deu chung tran nay;
 *  - bai lam tren may (lam them, luyen cho hay vap, Giai de): [TRAN_TREN_MAY];
 *  - on lai theo hen, cung lam tren may: [TRAN_ON_MOI_NGAY];
 *  - Kiem tra bai: [HocThuoc.TRAN_PHUT_MOI_NGAY]; Do tu vung: tran cua [LuatTuVung].
 *
 * Phut lam tren may ma bi tran cat thi vao "Quỹ giờ chơi" ([QuyGio]); duong chup anh thi
 * khong. Gio sao cua bai lam tren may o [LuatGhep]; file nay chi con luat cua duong chup
 * anh.
 *
 * DUONG CHUP ANH CHI CON BAI DAN DO. Bai co giao le, phieu photo, bai mon chua co sach
 * deu coi la dan do (Ba Huy chot 29/9/2026). Lam het vo dan do thi tron goi; chua het thi
 * tinh le tung cau, mot dong mot phut, trac nghiem mot phut mot cau, khong san khong tran
 * tung cau. Da tinh le bao nhieu thi luc du goi chi cong them cho tron [TRAN_ANH].
 *
 * Moi muc chi sinh gio MOT lan trong doi. Cau sai thi khong duoc gi; sua dung roi nop
 * lai thi luc do moi tinh, va tinh dung mot lan. Viec nho "muc nay tra gio chua" la cua
 * so cai bai da nop, khong phai cua ham nay.
 */
object LuatCongGio {

    /** Lam het bai co giao trong vo dan do. */
    const val PHUT_TRON_GOI_DAN_DO = 45

    /**
     * Tran ca ngay cua duong chup anh: tron goi va tinh le cong lai khong qua chung nay.
     *
     * Bang gia tron goi la co y: bai dan do chup anh dang dung chung nay, lam het hay lam
     * mot nua cung vay. Truoc 29/9/2026 bai lam them chup anh co tran rieng 90 phut; tu do
     * bai lam them chi con tren may.
     */
    const val TRAN_ANH = PHUT_TRON_GOI_DAN_DO

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
        /** Lan tinh nay co an tron goi vo dan do khong. */
        val daTinhGoi: Boolean = false,
        /**
         * Cau nao duoc tinh le va duoc dung may phut, theo [CauCham.ma].
         *
         * So cai phai ghi dung con so DA TRA, khong phai gia niem yet cua cau. Hai
         * con so nay khac nhau that: cau nam trong tron goi duoc 0, va cau bi tran
         * cat cut chi duoc phan con lai.
         */
        val phutCua: Map<String, Int> = emptyMap(),
        /** Cau lam dung nhung khong cong phut, vi da nam trong tron goi da tra. */
        val trongGoi: List<CauCham> = emptyList(),
        /**
         * So phut cua tron goi trong [phut], khi [daTinhGoi]. Khong con luon la 45: da tinh
         * le truoc do bao nhieu thi goi chi cong phan con lai cho tron [TRAN_ANH].
         */
        val phutGoi: Int = 0,
        /** Cau lam dung ma Claude khong ghi so dong, xem [thieuSoDong]. */
        val thieuDong: List<CauCham> = emptyList()
    )

    /**
     * Ngay ghi trong vo dan do co con hieu luc de nhan tron goi khong.
     *
     * Le Hoa hoc buoi chieu 12:30-17:00, nen bai co giao hom nay thuong duoc lam
     * ngay toi do hoac sang hom sau truoc khi di hoc. Vi vay:
     *
     *  - ngay hom nay: duoc;
     *  - ngay hom qua, va bay gio chua qua trua: duoc;
     *  - thu Bay, va hom nay la Chu nhat: duoc ca ngay - cuoi tuan khong co buoi
     *    hoc chen vao giua nen khong so nham voi bai cua hom khac.
     *
     * Ngay trong tuong lai thi khong: vo ghi ngay mai co nghia la chua hoc den do.
     */
    fun ngayDanDoHopLe(
        ngayTrongVo: LocalDate?,
        bayGio: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        if (ngayTrongVo == null) return false
        val homNay = bayGio.toLocalDate()

        if (ngayTrongVo.isAfter(homNay)) return false
        if (ngayTrongVo == homNay) return true

        val homQua = homNay.minusDays(1)
        if (ngayTrongVo == homQua) {
            // Sang hom sau, truoc khi di hoc buoi chieu.
            if (bayGio.hour < GIO_HET_HAN_SANG) return true
            // Thu Bay chup vao Chu nhat: ca ngay Chu nhat deu duoc.
            if (ngayTrongVo.dayOfWeek == DayOfWeek.SATURDAY &&
                homNay.dayOfWeek == DayOfWeek.SUNDAY
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Doc ngay ghi trong vo dan do.
     *
     * Vo cua tre con ghi ngay kieu nao cung co: "2026-09-14", "14/9/2026", va nhat
     * la "Thứ hai, ngày 14 tháng 9 năm 2026". Cai bay nam o chu THU: "Thứ 2, ngày 14
     * tháng 9 năm 2026" co bon con so, ma so dau tien la thu chu khong phai ngay.
     * Nen o day khong quet so mot cach may moc, ma tim tung dang mot theo thu tu.
     */
    fun docNgay(chu: String?, homNay: LocalDate = LocalDate.now()): LocalDate? {
        if (chu.isNullOrBlank()) return null
        val t = chu.trim()

        // 2026-09-14
        Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(t)?.let { m ->
            return lam(m.groupValues[3], m.groupValues[2], m.groupValues[1])
        }
        // 14/9/2026 hoac 14-09-2026
        Regex("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})").find(t)?.let { m ->
            return lam(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        }
        // ngay 14 thang 9 nam 2026 (co dau hay khong dau deu doc duoc)
        Regex(
            "ng[àa]y\\s*(\\d{1,2}).{0,15}?th[áa]ng\\s*(\\d{1,2}).{0,15}?n[ăa]m\\s*(\\d{4})",
            RegexOption.IGNORE_CASE
        ).find(t)?.let { m ->
            return lam(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        }
        // Lay so bon chu so lam nam, hai so ngay truoc no lam ngay va thang. Cach nay
        // nuot duoc ca "Thứ 2, ngày 14 tháng 9 năm 2026".
        val so = Regex("\\d+").findAll(t).map { it.value }.toList()
        val viTriNam = so.indexOfLast { it.length == 4 }
        if (viTriNam >= 2) {
            return lam(so[viTriNam - 2], so[viTriNam - 1], so[viTriNam])
        }

        // Vo khong ghi nam - rat hay gap: "Thứ hai, ngày 14 tháng 9". Lay nam nao
        // cho ra ngay gan hom nay nhat. Cau lenh gui cho AI da noi ro hom nay la
        // ngay nao nen phan nhieu no tu quy ra; day la luoi do thu hai.
        val thieuNam = Regex("ng[àa]y\\s*(\\d{1,2}).{0,15}?th[áa]ng\\s*(\\d{1,2})", RegexOption.IGNORE_CASE)
            .find(t)
            ?: Regex("^(\\d{1,2})[/-](\\d{1,2})$").find(t)
        if (thieuNam != null) {
            val ngay = thieuNam.groupValues[1]
            val thang = thieuNam.groupValues[2]
            return listOf(homNay.year - 1, homNay.year, homNay.year + 1)
                .mapNotNull { lam(ngay, thang, it.toString()) }
                .minByOrNull { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(homNay, it)) }
        }
        return null
    }

    private fun lam(ngay: String, thang: String, nam: String): LocalDate? = runCatching {
        LocalDate.of(nam.toInt(), thang.toInt(), ngay.toInt())
    }.getOrNull()

    /**
     * Quy mot lan cham bai chup anh ra so phut.
     *
     * @param daCongAnhHomNay so phut duong chup anh da cong trong ngay (tinh le va tron
     *   goi), de giu [TRAN_ANH]. Ben goi lay tu so cai, xem [SoCaiBai.phutAnhHomNay].
     */
    fun tinh(
        goc: KetQuaCham,
        daCongAnhHomNay: Int = 0,
        bayGio: LocalDateTime = LocalDateTime.now(),
        goiDaCoHomNay: Boolean = false
    ): BangTinh {
        val dong = mutableListOf<String>()

        /*
         * Cau khong co de thi coi nhu chua dung, du may co khai gi.
         *
         * Cau lenh da dan "khong co de thi bat buoc dung: false", nhung mot cau tra
         * loi lech mot truong khong duoc phep bien thanh mot lan cong gio. Chan ngay
         * o day, mot cho, de moi phep tinh ben duoi - tinh le, tron goi, cum trac
         * nghiem - deu khong phai nho luat nay lan nua.
         */
        val ket = goc.copy(cac = goc.cac.map { if (it.coDe) it else it.copy(dung = false) })

        val ngay = docNgay(ket.ngayDanDo, bayGio.toLocalDate())
        val ngayOk = ngayDanDoHopLe(ngay, bayGio)
        /*
         * Tron goi la tra cho viec LAM HET BAI CO GIAO, nen phai co bai duoc giao da.
         *
         * Ngay 14/9/2026 vo dan do cua Le Hoa ghi: tiet sau kiem tra tu vung, tiet
         * sau kiem tra bai 2 bai 3, mang sach vo day du, lam dung noi quy - khong mot
         * bai tap nao. May van tra "lam_het_dan_do": true va con duoc 45 phut. Bat no
         * ke ten bai duoc giao ra thi khong con cho nao de gat: khong ke duoc thi
         * khong co goi, ma ke ra thi Ba Huy doc duoc ngay tren Telegram.
         */
        val coBaiGiao = ket.baiDuocGiao.isNotEmpty()
        // Tron goi moi ngay chi mot lan. Khong co dong nay thi chup lai vo dan do
        // vao buoi toi la them 45 phut nua, ma bai thi van la bai ban chieu.
        val coGoi = ket.lamHetDanDo && ngayOk && coBaiGiao && !goiDaCoHomNay

        /*
         * Trong ngay DA CO tron goi - lan nay tra hay lan truoc tra cung vay. Moi bai chup
         * anh deu la bai dan do (29/9/2026), nen hom da co goi thi bai nop them khong duoc
         * tinh le nua: gio cua chung da nam trong goi. Man chinh cung an nut Nop bai tu luc
         * do toi het ngay.
         *
         * Phai tach khoi [coGoi]. Lan nop de SUA BAI chi gui anh bai giai, khong co
         * trang vo dan do, nen ngay_dan_do ve null va coGoi = false. Lay coGoi lam
         * moc o day thi chinh bai cua goi lai duoc tinh le tung cau: ngay 14/9/2026
         * Le Hoa duoc 45 phut goi, nop lai bai da sua duoc them 40 phut nua cho cung
         * mot xap bai, tong 85 phut cho mot buoi khong co bai tap nao duoc giao.
         */
        val goiConHieuLuc = coGoi || goiDaCoHomNay
        val conTran = (TRAN_ANH - daCongAnhHomNay).coerceAtLeast(0)

        var phut = 0
        var phutGoi = 0
        if (goiDaCoHomNay) {
            dong += "Hôm nay đã tính trọn gói bài dặn dò rồi, bài nộp thêm không cộng phút"
        }
        if (coGoi) {
            phutGoi = conTran
            phut += phutGoi
            dong += "Làm hết bài cô giao ngày ${ngay!!.dayOfMonth}/${ngay.monthValue} " +
                "(${keTen(ket.baiDuocGiao)}): +$phutGoi phút" +
                if (daCongAnhHomNay > 0) " (cộng cho tròn $TRAN_ANH, đã tính lẻ $daCongAnhHomNay phút)" else ""
        } else if (ket.lamHetDanDo && ket.ngayDanDo != null && !ngayOk) {
            // Co chup vo dan do, co lam het, nhung ngay khong con hieu luc.
            dong += "Vở dặn dò ghi ngày ${ket.ngayDanDo} — không phải bài hôm nay nên " +
                "không tính trọn gói"
        } else if (ket.lamHetDanDo && ngayOk && !coBaiGiao && !goiDaCoHomNay) {
            dong += "Vở dặn dò hôm đó không giao bài tập nào (chỉ dặn việc) nên máy " +
                "không tự tính trọn gói — bài làm tính lẻ từng câu. Muốn cho gói thì " +
                "bấm nút dưới tin vở dặn dò."
        }

        // Co goi thi moi cau dung nam trong goi; chua co goi thi tinh le tung cau.
        val trongGoi = if (goiConHieuLuc) ket.cac.filter { it.dung } else emptyList()
        val tinhLe = if (goiConHieuLuc) emptyList() else ket.cac.filter { it.dung }
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
            dong += "Bài dặn dò mỗi ngày tối đa $TRAN_ANH phút, hôm nay đã đủ"
        }
        phut += le

        // Noi ro vi sao lam dung ma khong duoc phut nao - neu khong, lan nop de sua
        // bai tra ve mot tin nhan im lang va con tuong la may cham hong.
        if (goiDaCoHomNay && trongGoi.isNotEmpty()) {
            dong += "Đúng rồi, nhưng nằm trong trọn gói bài dặn dò hôm nay nên không " +
                "cộng thêm: " + keTen(trongGoi.map { it.ma })
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
            phut = phut,
            dong = dong,
            canBaHuyXem = mo.isNotEmpty() || thieuDong.isNotEmpty(),
            daTinhGoi = coGoi,
            phutCua = duoc.associate { (cau, p) -> cau.ma to p } +
                chiaDeu(tracNghiem, phutCumThat),
            trongGoi = trongGoi,
            phutGoi = phutGoi,
            thieuDong = thieuDong
        )
    }

    /**
     * Chia so phut tablet cap THAT cho goi va tung cau, khi cap khong du [BangTinh.phut]
     * (vi du phan con lai cua mot phieu cat o gio chot trong ngay).
     *
     * So cai phai ghi so da tra, khong phai gia niem yet, xem [BangTinh.phutCua]. Goi lay
     * truoc, roi toi tung cau theo thu tu trong [BangTinh.phutCua]; cau het phan thi
     * duoc 0 phut. Cap du thi tra ve dung goi va [BangTinh.phutCua] nhu cu.
     *
     * @return phut cua goi (0 neu lan nay khong tinh goi) va phut tung cau.
     */
    fun chiaPhutDaCap(bang: BangTinh, daCap: Int): Pair<Int, Map<String, Int>> {
        var con = daCap.coerceAtLeast(0)
        val goi = if (bang.daTinhGoi) minOf(bang.phutGoi, con) else 0
        con -= goi
        return goi to bang.phutCua.mapValues { (_, p) -> minOf(p, con).also { con -= it } }
    }

    /** Doi LocalDateTime tu moc may, de cho test dua gio vao thang. */
    fun bayGio(mocMs: Long): LocalDateTime =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(mocMs), ZoneId.systemDefault())

    /**
     * Truoc gio nay sang hom sau thi bai hom qua van con tinh.
     *
     * Khong con private: man hinh bang gia cua Le Hoa
     * ([vn.huytl.homeworkgate.ui.CachKiemGioActivity]) noi ra con so nay cho con
     * biet sang mai chup vo con kip khong. Noi bang mot so go tay thi den luc doi
     * gio o day, cau do im lang noi sai.
     */
    const val GIO_HET_HAN_SANG = 12

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

    /** Liet ke ma cau, dai qua thi cat - mot trang trac nghiem co ba muoi cau. */
    private fun keTen(ma: List<String>): String =
        if (ma.size <= SO_DONG_KE_TOI_DA) ma.joinToString(", ")
        else ma.take(SO_DONG_KE_TOI_DA).joinToString(", ") +
            " và ${ma.size - SO_DONG_KE_TOI_DA} câu nữa"
}
