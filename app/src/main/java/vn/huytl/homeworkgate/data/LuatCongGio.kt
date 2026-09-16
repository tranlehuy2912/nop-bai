package vn.huytl.homeworkgate.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Luat quy bai lam ra so phut choi.
 *
 * Hai cach tinh, khong cong chung vao nhau:
 *
 *  - LAM HET BAI CO GIAO: tron goi [PHUT_TRON_GOI_DAN_DO] phut. Day la cach tinh
 *    dung nhat, vi no neo vao luong bai that su cua mot buoi hoc - co giao la nguoi
 *    biet hom nay nen lam bao nhieu, khong phai may.
 *  - BAI LAM THEM: tinh le tung muc, co tran [TRAN_LAM_THEM] phut moi ngay.
 *
 * Neu cong ca hai cho cung mot bai thi thanh tinh hai lan, va mo duong cho viec
 * lam that nhieu cau de de kiem gio. Nen: lam het vo dan do thi cac muc trong do
 * nam trong goi, chi muc NGOAI vo dan do moi tinh le. Lam chua het thi khong co
 * goi, tinh le tung muc - de con muon du gio thi lam cho het chu khong lam nua
 * chung roi di tim bai de.
 *
 * Moi muc chi sinh gio MOT lan trong doi. Cau sai thi khong duoc gi; sua dung roi
 * nop lai thi luc do moi tinh, va tinh dung mot lan. Viec nho "muc nay tra gio
 * chua" la cua so cai bai da nop, khong phai cua ham nay.
 */
object LuatCongGio {

    /** Lam het bai co giao trong vo dan do. */
    const val PHUT_TRON_GOI_DAN_DO = 45

    /**
     * Bai tap: hai dong lam bai duoc mot phut.
     *
     * Do CONG SUC NHIN THAY DUOC chu khong do "do kho". Do kho thi may doan khong
     * noi - cung mot trang, cung mot model, ba lan chay no con doi y ve mot cau; ma
     * kho hay de con tuy dua tre nao lam. So dong thi dem duoc, kiem lai duoc bang
     * chinh tam anh, va khong cai duoc.
     *
     * Con so chon de trung voi luat cu cua Ba Huy chu khong phai de doi no: mot cau
     * phan tich nhan tu viet 3-4 dong van ra 2 phut, mot bai dai 10 dong ra 5 phut.
     * Mot luat thay cho hai, va tu co gian cho nhung bai o giua.
     */
    const val DONG_MOI_PHUT = 2

    /** Cau ngan may cung duoc chung nay. */
    const val TOI_THIEU_BAI_TAP = 2

    /** Mot cau toi da bay nhieu: chan viec viet dai dong de kiem gio. */
    const val TRAN_MOT_BAI_TAP = 10

    /**
     * Bai viet dai (doan van, bai van, bao cao): ba dong duoc hai phut.
     *
     * Nhinh hon bai tap vi moi dong phai tu nghi ra chu. Mot trang vo khoang hai
     * muoi dong, ra khoang 14 phut - dung bang "15 phut mot trang" ma Ba Huy uoc
     * luong. Dem dong thay vi dem trang vi chu to chu nho moi hom mot khac.
     */
    const val DONG_MOI_PHUT_VIET_DAI = 3
    const val PHUT_MOI_NHIP_VIET_DAI = 2
    const val TOI_THIEU_VIET_DAI = 5

    /**
     * Trac nghiem: bay nhieu cau dung moi duoc mot phut.
     *
     * Tinh theo CUM chu khong theo cau, vi "mot cau" o trang trac nghiem la khai
     * niem khong on: cung nam tam anh KHTN ngay 14/9/2026, ba lan chay ra 36, 4, roi
     * 22 cau. Theo cum thi chenh lech do chi con vai phut thay vi gap ba lan.
     *
     * Con so lay tu cong suc that: khoanh het mot trang trac nghiem chung muoi lam
     * phut, mot trang chung sau muoi cau -> bon cau mot phut.
     */
    const val CAU_TRAC_NGHIEM_MOI_PHUT = 4

    /** Mot lan nop toi da bay nhieu phut tu trac nghiem. */
    const val TRAN_TRAC_NGHIEM = 15

    /** Mot bai viet dai toi da bay nhieu, du con viet may trang. */
    const val TRAN_MOT_BAI_VIET_DAI = 30

    /**
     * Tran cho phan bai lam them moi ngay.
     *
     * Khong co tran thi mot quyen bai tap nang cao la ca buoi toi choi game: cu lam
     * cau de, cau nao cung hai phut. Tran nay khong dinh gi den goi vo dan do, nen
     * mot ngay toi da la 45 + 90 = [TOI_DA_MOI_NGAY] phut.
     */
    const val TRAN_LAM_THEM = 90

    /** Toi da mot ngay: tron goi vo dan do cong het tran lam them. */
    const val TOI_DA_MOI_NGAY = PHUT_TRON_GOI_DAN_DO + TRAN_LAM_THEM

    /**
     * So phut cua mot muc lam dung, theo so dong lam bai.
     *
     * [CauCham.soDong] bang 0 nghia la ben goi khong dem duoc dong nao (AI khong
     * tra ve, hay bai khong co dong nao de dem). Luc do quay ve luat phang cu: cau
     * nho 2 phut, bai rieng 5 phut - de mot cho hong khong lam con mat gio.
     */
    fun phutChoCau(cau: CauCham): Int = when {
        // Khong co de thi khong ai cham duoc, nen khong tra gio. Xem [CauCham.coDe].
        !cau.coDe -> 0
        cau.dang == DangBai.KHONG_TINH -> 0
        // Trac nghiem khong co gia rieng tung cau, tinh theo cum trong [tinh].
        cau.dang == DangBai.TRAC_NGHIEM -> 0
        cau.soDong <= 0 -> if (cau.dang == DangBai.BAI_RIENG) 5 else TOI_THIEU_BAI_TAP
        cau.dang == DangBai.VIET_DAI ->
            (cau.soDong * PHUT_MOI_NHIP_VIET_DAI / DONG_MOI_PHUT_VIET_DAI)
                .coerceIn(TOI_THIEU_VIET_DAI, TRAN_MOT_BAI_VIET_DAI)
        else -> (cau.soDong / DONG_MOI_PHUT).coerceIn(TOI_THIEU_BAI_TAP, TRAN_MOT_BAI_TAP)
    }

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
        val trongGoi: List<CauCham> = emptyList()
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
     * Quy mot lan cham ra so phut.
     *
     * @param daCongLamThemHomNay so phut phan "lam them" da cong trong ngay, de giu
     *   tran. Ben goi lay tu so cai.
     */
    fun tinh(
        goc: KetQuaCham,
        daCongLamThemHomNay: Int = 0,
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
        // Tron goi moi ngay chi mot lan. Khong co dong nay thi chup lai vo dan do
        // vao buoi toi la them 45 phut nua, ma bai thi van la bai ban chieu.
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
        val coGoi = ket.lamHetDanDo && ngayOk && coBaiGiao && !goiDaCoHomNay

        /*
         * Trong ngay DA CO tron goi - lan nay tra hay lan truoc tra cung vay. Bai co
         * giao da nam gon trong 45 phut do roi, khong duoc tinh le them lan nua.
         *
         * Phai tach khoi [coGoi]. Lan nop de SUA BAI chi gui anh bai giai, khong co
         * trang vo dan do, nen ngay_dan_do ve null va coGoi = false. Lay coGoi lam
         * moc o day thi chinh bai cua goi lai duoc tinh le tung cau: ngay 14/9/2026
         * Le Hoa duoc 45 phut goi, nop lai bai da sua duoc them 40 phut nua cho cung
         * mot xap bai, tong 85 phut cho mot buoi khong co bai tap nao duoc giao.
         */
        val goiConHieuLuc = coGoi || goiDaCoHomNay

        var phut = 0
        if (goiDaCoHomNay) {
            dong += "Hôm nay đã tính trọn gói bài cô giao rồi, lần này chỉ tính bài làm thêm"
        }
        if (coGoi) {
            phut += PHUT_TRON_GOI_DAN_DO
            dong += "Làm hết bài cô giao ngày ${ngay!!.dayOfMonth}/${ngay.monthValue} " +
                "(${keTen(ket.baiDuocGiao)}): +$PHUT_TRON_GOI_DAN_DO phút"
        } else if (ket.lamHetDanDo && ket.ngayDanDo != null && !ngayOk) {
            // Co chup vo dan do, co lam het, nhung ngay khong con hieu luc.
            dong += "Vở dặn dò ghi ngày ${ket.ngayDanDo} — không phải bài hôm nay nên " +
                "không tính trọn gói"
        } else if (ket.lamHetDanDo && ngayOk && !coBaiGiao && !goiDaCoHomNay) {
            dong += "Vở dặn dò hôm đó không giao bài tập nào (chỉ dặn việc) nên không " +
                "tính trọn gói — bài làm tính lẻ từng câu"
        }

        // Muc nao tinh le: khong co goi thi tinh het, co goi thi chi tinh bai lam them.
        val trongGoi = ket.cac.filter { it.dung && goiConHieuLuc && it.trongDanDo }
        val tinhLe = ket.cac.filter { it.dung && (!goiConHieuLuc || !it.trongDanDo) }

        // Trac nghiem gom thanh mot cum, phan con lai tinh tung cau nhu cu.
        val tracNghiem = tinhLe.filter { it.dang == DangBai.TRAC_NGHIEM }
        val phutCum = (tracNghiem.size / CAU_TRAC_NGHIEM_MOI_PHUT).coerceAtMost(TRAN_TRAC_NGHIEM)
        val phutTungCau = tinhLe.filterNot { it.dang == DangBai.TRAC_NGHIEM }
            .map { it to phutChoCau(it) }
            .filter { it.second > 0 }

        // Tran chi ap cho phan lam them, tuc la khi trong ngay da co goi. Khong co
        // goi thi phan tinh le CHINH LA bai co giao, chan lai la phat con vi lam nhieu.
        val conTran =
            if (goiConHieuLuc) (TRAN_LAM_THEM - daCongLamThemHomNay).coerceAtLeast(0)
            else Int.MAX_VALUE

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
            val dem = if (cau.soDong > 0) " (${cau.soDong} dòng)" else ""
            dong += "${cau.ma}$dem: +$p phút"
        }
        if (duoc.size > SO_DONG_KE_TOI_DA) {
            val con = duoc.drop(SO_DONG_KE_TOI_DA)
            dong += "…và ${con.size} câu nữa: +${con.sumOf { it.second }} phút"
        }

        // Cum trac nghiem: tra sau cac cau co loi giai, va cung phai qua tran.
        var phutCumThat = 0
        if (phutCum > 0) {
            phutCumThat = phutCum.coerceAtMost(conTran - le)
            if (phutCumThat < phutCum) biCat = true
            if (phutCumThat > 0) {
                le += phutCumThat
                dong += "${tracNghiem.size} câu trắc nghiệm đúng: +$phutCumThat phút"
            }
        }

        if (biCat) {
            dong += "Bài làm thêm hôm nay tối đa $TRAN_LAM_THEM phút, cắt còn $conTran phút"
        }
        phut += le

        // Noi ro vi sao lam dung ma khong duoc phut nao - neu khong, lan nop de sua
        // bai tra ve mot tin nhan im lang va con tuong la may cham hong.
        if (goiDaCoHomNay && trongGoi.isNotEmpty()) {
            dong += "Đúng rồi, nhưng nằm trong trọn gói bài cô giao hôm nay nên không " +
                "cộng thêm: " + keTen(trongGoi.map { it.ma })
        }

        val sai = ket.cac.filter { !it.dung }
        if (sai.isNotEmpty()) {
            dong += "Chưa tính: " + sai.joinToString(", ") { it.ma } + " (sửa lại rồi nộp tiếp)"
        }

        val mo = ket.cac.filter { !it.docRo }
        if (mo.isNotEmpty()) {
            dong += "Máy đọc không rõ: " + mo.joinToString(", ") { it.ma } + " — nhờ Ba Huy xem"
        }

        return BangTinh(
            phut = phut,
            dong = dong,
            canBaHuyXem = mo.isNotEmpty(),
            daTinhGoi = coGoi,
            phutCua = duoc.associate { (cau, p) -> cau.ma to p } +
                chiaDeu(tracNghiem, phutCumThat),
            trongGoi = trongGoi
        )
    }

    /** Doi LocalDateTime tu moc may, de cho test dua gio vao thang. */
    fun bayGio(mocMs: Long): LocalDateTime =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(mocMs), ZoneId.systemDefault())

    /** Truoc gio nay sang hom sau thi bai hom qua van con tinh. */
    private const val GIO_HET_HAN_SANG = 12

    /** Ke ro toi da bay nhieu cau, con lai gom mot dong. */
    private const val SO_DONG_KE_TOI_DA = 6

    /**
     * Chia so phut cua mot cum cho tung cau trong cum.
     *
     * So cai ghi theo tung cau - can the de lan sau nop lai biet cau nao da tra gio
     * roi. Chia deu, phan du rai cho may cau dau, de cong lai van dung so phut da
     * tra that chu khong phinh len.
     */
    private fun chiaDeu(cac: List<CauCham>, phut: Int): Map<String, Int> {
        if (cac.isEmpty()) return emptyMap()
        val moi = phut / cac.size
        var du = phut % cac.size
        return cac.associate { c ->
            val them = moi + if (du > 0) 1 else 0
            if (du > 0) du--
            c.ma to them
        }
    }

    /** Liet ke ma cau, dai qua thi cat - mot trang trac nghiem co ba muoi cau. */
    private fun keTen(ma: List<String>): String =
        if (ma.size <= SO_DONG_KE_TOI_DA) ma.joinToString(", ")
        else ma.take(SO_DONG_KE_TOI_DA).joinToString(", ") +
            " và ${ma.size - SO_DONG_KE_TOI_DA} câu nữa"
}
