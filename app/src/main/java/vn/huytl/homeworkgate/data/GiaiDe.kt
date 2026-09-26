package vn.huytl.homeworkgate.data

import android.content.Context
import android.util.Log
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.DeGiai
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Giai de: may lay cau sach bai tap ra thanh mot de, Le Hoa lam mot mach co dong ho.
 *
 * VI SAO CO. Le Hoa khong co sach bai tap giay, nen co giao khong giao cau SBT nao ma
 * hon mot nghin ba tram cau SBT nap ngay 26/9/2026 nam im. Ngay 27/9/2026 Ba Huy chon
 * dung chung de lam hai viec: bai on (lam them, luyen cho hay vap - xem
 * [NganHang.cauNenLamThemCuaMon]) va de kiem tra o day. Ten "Giải đề" la Ba Huy dat, de
 * khong lan voi dong "Kiểm tra bài" cua the hoc thuoc tren man chinh.
 *
 * HAI LOAI DE, deu tu mo, khong ai phai bam ra:
 *  - [LOAI_TUAN]: sang thu Bay, moi mon co SBT mot de, on cac bai lop vua hoc. Lop hoc
 *    xong mot chuong Toan thi de tuan do la muc "Ôn tập chương" cua SBT, von da la mot
 *    de kiem tra chuong. De de do toi thu Bay sau.
 *  - [LOAI_KIEM_TRA]: vo dan do hay tin cua co bao sap kiem tra Toan, KHTN thi mo mot de
 *    on dung may bai do, toi het ngay kiem tra. Doc lich bang [LichKiemTra].
 *
 * CACH LAM. Con bam Bat dau thi dong ho chay. Cau trac nghiem bam chu ngay tren may, cau
 * tu luan lam ra vo. Bam Nop bai: trac nghiem cham ngay bang dap an in cuoi sach, cong
 * gio luon; roi con chup phan tu luan, di dung duong cham nhu bai lam them - xem
 * [vn.huytl.homeworkgate.kho.PhamVi.giaiDe]. Het gio van nop duoc, tin cho Ba Huy ghi
 * lam bao lau (Ba Huy chon ngay 27/9/2026).
 *
 * GIO CHOI tinh y het bai lam them, khong co gia rieng: tu luan theo so dong, trac nghiem
 * [LuatCongGio.PHUT_MOI_CAU_TRAC_NGHIEM] mot cau, chung tran lam them. Diem cua de khong
 * doi ra phut nao: thuong theo diem la cho con mot ly do de tra loi giai, ma loi giai moi
 * cau SBT deu co tren mang.
 *
 * DIEM la so cau dung o lan lam dau trong de, moi y a), b) la mot cau. Cau sai trong de
 * van vao duong sua va lich on lai nhu moi cau khac; sua xong khong doi diem.
 *
 * CAU NAO DUOC VAO DE. Chi cau SBT co dap an, chua tung nop, khong nam trong de khac con
 * han, cung mot quyen (mot lan nop chi mang ma cua mot quyen). Cau trac nghiem khac kieu
 * (dung/sai tung y, noi cot) thi khong, vi khong bam mot chu duoc. Moi de co it nhat mot
 * cau tu luan: de chi toan trac nghiem thi khong co gi de viet ra vo, va cung khong con
 * la mot bai kiem tra.
 */
object GiaiDe {

    const val LOAI_TUAN = "TUAN"
    const val LOAI_KIEM_TRA = "KIEM_TRA"

    /** De tuan mo luc bay nhieu gio sang thu Bay. */
    const val GIO_MO_DE_TUAN = 6

    /** De on truoc kiem tra khong doc ra ngay thi de bay nhieu ngay tinh tu dong chu. */
    private const val NGAY_GIU_KHONG_RO = 3

    /** De da bat dau thi het han van cho nop them bay nhieu gio. */
    private const val GIO_NOP_MUON = 24

    /** Tin cua co trong bay nhieu ngay qua thi con doc lich kiem tra. */
    private const val NGAY_DOC_TIN = 4

    /** Ranh gioi giua hai cau trong tin cua co: xuong dong, hay dau ket cau roi dau cach. */
    private val CAU_TIN = Regex("""\n|(?<=[.!?;])\s+""")

    /** Co de: bay nhieu cau trac nghiem, bay nhieu bai tu luan, toi da bay nhieu y tu luan. */
    private const val SO_TN_KHTN = 8
    private const val SO_BAI_TL_KHTN = 2
    private const val SO_TN_CHUONG = 4
    private const val SO_BAI_TL_CHUONG = 3
    private const val SO_BAI_TL_TOAN = 4
    private const val TOI_DA_Y_TU_LUAN = 6

    /** Cau it hon chung nay thi khong ra de: ba cau khong phai mot bai kiem tra. */
    private const val TOI_THIEU_CAU = 3

    /** De tuan lay cac bai trong bay nhieu bai gan moc nhat cua moi phan. */
    private const val SO_BAI_GAN_MOC = 3

    /** Moi cau trac nghiem mot phut ruoi, moi y tu luan nam phut; lam tron len boi cua nam. */
    private const val GIAY_MOI_TN = 90
    private const val PHUT_MOI_Y_TU_LUAN = 5

    private const val MOT_NGAY = 24 * 60 * 60_000L
    private const val MOT_NAM = 365 * MOT_NGAY

    // -------------------------------------------------------------- ra de

    /**
     * Mo cac de den luc: de tuan va de on truoc kiem tra. Chay bao nhieu lan cung vay,
     * vi moi nguon ra de mang mot khoa rieng - xem [DeGiai.khoa].
     *
     * Goi tu man chinh moi lan mo, sau khi con luu vo dan do, va khi co tin cua co moi.
     * Chay ngoai luong giao dien: doc ca kho SBT ra mot lan, chung mot phan muoi giay.
     *
     * @return cac de vua mo lan nay.
     */
    fun taoNeuCan(context: Context, bayGio: Long = System.currentTimeMillis()): List<DeGiai> {
        val moi = mutableListOf<DeGiai>()
        runCatching {
            MON.forEach { mon -> taoDeTuan(context, mon, bayGio)?.let { moi += it } }
            moi += taoDeKiemTra(context, bayGio)
        }.onFailure { Log.w(TAG, "ra de hong: ${it.message}") }
        moi.forEach { de ->
            DayLog.add(context, "Máy ra đề ${tenDe(de)}: ${de.cauIds.size} câu, khoảng ${de.phutGoiY} phút")
        }
        return moi
    }

    /** Hai mon co sach bai tap trong may. */
    val MON = listOf(LichKiemTra.TOAN, LichKiemTra.KHTN)

    /** Ten mon cho dong chu ngan: "Toán", "KHTN". */
    fun tenMon(mon: String): String = if (mon == LichKiemTra.KHTN) "KHTN" else mon

    /** Moc thu Bay cua tuan chua [bayGio]: thu Bay gan nhat da qua [GIO_MO_DE_TUAN] gio. */
    fun mocTuan(bayGio: Long): LocalDateTime {
        val luc = LocalDateTime.ofInstant(Instant.ofEpochMilli(bayGio), ZoneId.systemDefault())
        var thuBay = luc.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
            .atTime(GIO_MO_DE_TUAN, 0)
        if (thuBay.isAfter(luc)) thuBay = thuBay.minusWeeks(1)
        return thuBay
    }

    private fun taoDeTuan(context: Context, mon: String, bayGio: Long): DeGiai? {
        val moc = mocTuan(bayGio)
        val khoa = "tuan:${moc.toLocalDate()}:$mon"
        val kho = KhoBai.get(context)
        if (kho.cacDeTu(moc.epochMs() - MOT_NGAY).any { it.khoa == khoa }) return null
        if (PhanHoc.baiDaHoc(context, mon, chiPhanDaChon = true).isNullOrEmpty()) return null

        val chon = chonCauTuan(context, mon, bayGio) ?: return null
        val de = dung(
            mon = mon, loai = LOAI_TUAN, khoa = khoa, chon = chon, bayGio = bayGio,
            hetHan = moc.plusWeeks(1).epochMs()
        )
        return de.takeIf { kho.themDe(it) }
    }

    private fun taoDeKiemTra(context: Context, bayGio: Long): List<DeGiai> {
        val homNay = ngayCua(bayGio)
        val nguon = buildList {
            VoDanDo.conHieuLuc(context)?.takeUnless { it.chuaDoc }?.let { vo ->
                val ngay = vo.ngayDoc() ?: homNay
                vo.dongKhac.forEach { add(it to ngay) }
            }
            KhoTinCuaCo(context).danhSach()
                .filter { bayGio - it.luc < NGAY_DOC_TIN * MOT_NGAY }
                .forEach { tin ->
                    // Tin nhom lop hay gom nhieu viec trong mot doan ("Mai kiểm tra Toán.
                    // Thứ năm nộp bài KHTN."): doc tung cau, khong thi mon cua cau nay
                    // dinh vao lan kiem tra cua cau kia.
                    tin.noiDung.split(CAU_TIN).forEach { dong -> add(dong to ngayCua(tin.luc)) }
                }
        }
        val kho = KhoBai.get(context)
        val cacDe = kho.cacDeTu(bayGio - 30 * MOT_NGAY)
        val daCo = cacDe.map { it.khoa }.toSet()
        // Mot buoi kiem tra ma vo dan do va tin cua co cung bao thi chi mot de on.
        val buoiDaCo = cacDe.filter { it.loai == LOAI_KIEM_TRA && it.ngayKiemTra.isNotBlank() }
            .map { it.mon to it.ngayKiemTra }.toMutableSet()
        return nguon.flatMap { (dong, ngay) -> LichKiemTra.doc(dong, ngay).map { it to ngay } }
            .mapNotNull { (kt, ngay) ->
                val khoa = "kt:$ngay:${kt.mon}:${kt.chu.hashCode()}"
                if (khoa in daCo) return@mapNotNull null
                val buoi = kt.ngay?.let { kt.mon to it.toString() }
                if (buoi != null && buoi in buoiDaCo) return@mapNotNull null
                val ngayHet = kt.ngay ?: ngay.plusDays(NGAY_GIU_KHONG_RO.toLong())
                val hetHan = ngayHet.plusDays(1).atStartOfDay().epochMs()
                if (hetHan <= bayGio) return@mapNotNull null
                val chon = chonCauKiemTra(context, kt, bayGio) ?: return@mapNotNull null
                dung(
                    mon = kt.mon, loai = LOAI_KIEM_TRA, khoa = khoa, chon = chon, bayGio = bayGio,
                    hetHan = hetHan, ghiChu = kt.chu, ngayKiemTra = kt.ngay?.toString().orEmpty()
                ).takeIf { kho.themDe(it) }?.also { if (buoi != null) buoiDaCo += buoi }
            }
    }

    /** Ket qua chon cau: quyen, ten pham vi, cac cau theo thu tu hien. */
    private class Chon(val nguon: String, val ten: String, val cac: List<CauHoi>)

    private fun dung(
        mon: String,
        loai: String,
        khoa: String,
        chon: Chon,
        bayGio: Long,
        hetHan: Long,
        ghiChu: String = "",
        ngayKiemTra: String = ""
    ) = DeGiai(
        id = "de-" + UUID.randomUUID().toString().take(8),
        mon = mon,
        nguon = chon.nguon,
        loai = loai,
        khoa = khoa,
        ten = chon.ten,
        cauIds = chon.cac.map { it.id },
        phutGoiY = phutGoiY(chon.cac),
        taoLuc = bayGio,
        hetHan = hetHan,
        ghiChu = ghiChu,
        ngayKiemTra = ngayKiemTra
    )

    /**
     * Cau cua de tuan.
     *
     * Toan: lop vua hoc xong mot chuong ma muc "Ôn tập chương" cua chuong do chua co cau
     * nao tung ra thi lay muc do. Con lai lay cac bai gan moc cua moi phan.
     */
    private fun chonCauTuan(context: Context, mon: String, bayGio: Long): Chon? {
        val tuDo = cauTuDo(context, mon, bayGio)
        if (mon == LichKiemTra.TOAN) {
            chuongVuaXong(context, bayGio)?.let { onTap ->
                val cua = tuDo.filter { it.bai == onTap }
                ghep(cua, soTn = SO_TN_CHUONG, soBaiTl = SO_BAI_TL_CHUONG, uuTien = listOf(onTap))
                    ?.let { return Chon(it.first().nguon, onTap, it) }
            }
        }
        val baiGan = tuDo.map { it.bai }.distinct()
            .filter { NganHang.khoangCachMoc(context, mon, it) < SO_BAI_GAN_MOC }
            .sortedBy { NganHang.khoangCachMoc(context, mon, it) }
        return chonTheoBai(tuDo, mon, baiGan)
            // Bai gan moc da het cau: lui ve moi bai da hoc, gan moc truoc.
            ?: chonTheoBai(
                tuDo, mon,
                tuDo.map { it.bai }.distinct().sortedBy { NganHang.khoangCachMoc(context, mon, it) }
            )
    }

    /** Cau cua de on truoc kiem tra: dung may bai, may chuong dong chu nhac toi. */
    private fun chonCauKiemTra(context: Context, kt: LichKiemTra.KiemTra, bayGio: Long): Chon? {
        val kho = KhoBai.get(context)
        val han = bayGio - MOT_NAM
        val daNop = kho.cacCauDaNop(han)
        val trongDe = kho.cauTrongDeConHan(bayGio)
        // Lop co the kiem tra ca bai chua chon lam moc: co noi thi co biet, nen khong cat
        // theo moc o day. Chi bo cau da nop va cau dang nam trong de khac.
        val tatCa = NganHang.sachBaiTapCua(kt.mon).flatMap { kho.cacCauCuaNguon(it.nguon) }
            .filter { hopLe(it) && it.id !in daNop && it.id !in trongDe }
        val cacBai = when {
            kt.cacBai.isNotEmpty() -> tatCa.map { it.bai }.distinct()
                .filter { PhanHoc.soBai(it) in kt.cacBai }
            kt.chuong != null -> tatCa.filter { soChuong(it.chuong) == kt.chuong }
                .map { it.bai }.distinct()
            else -> emptyList()
        }
        if (cacBai.isNotEmpty()) {
            return chonTheoBai(tatCa, kt.mon, cacBai.sortedByDescending { PhanHoc.soBai(it) ?: 0 })
        }
        // Dong chu khong noi bai nao ("tiết sau kiểm tra 15 phút"): on cac bai gan moc.
        return chonCauTuan(context, kt.mon, bayGio)
    }

    /**
     * Cau SBT co the vao de: trong cac bai lop da hoc (ca muc on tap chuong cua chuong
     * da hoc xong), co dap an, chua tung nop, khong nam trong de khac con han.
     */
    private fun cauTuDo(context: Context, mon: String, bayGio: Long): List<CauHoi> {
        val daHoc = PhanHoc.baiDaHoc(context, mon, chiPhanDaChon = true) ?: return emptyList()
        val kho = KhoBai.get(context)
        val daNop = kho.cacCauDaNop(bayGio - MOT_NAM)
        val trongDe = kho.cauTrongDeConHan(bayGio)
        val cacCau = NganHang.sachBaiTapCua(mon).flatMap { kho.cacCauCuaNguon(it.nguon) }
        val chuongXong = chuongDaXong(cacCau, daHoc)
        return cacCau.filter { c ->
            val so = PhanHoc.soBai(c.bai)
            (if (so != null) so in daHoc else c.chuong in chuongXong) &&
                hopLe(c) && c.id !in daNop && c.id !in trongDe
        }
    }

    /**
     * Cau hop le cho mot de: co dap an, va la trac nghiem bam duoc hay mot cau tu luan.
     * Cau trac nghiem khac kieu (dung/sai, noi cot) thi bo, xem [CauHoi.bamTrenMay].
     */
    private fun hopLe(c: CauHoi): Boolean =
        c.dapAn.isNotBlank() && c.dang != "KHONG_TINH" && c.id !in KHONG_RA_DE &&
            (c.dang != "TRAC_NGHIEM" || c.bamTrenMay)

    /**
     * Cau sach in loi so lieu, luc chep dap an ngay 27/9/2026 da doi chieu ma khong gan
     * duoc voi mot dap an chac chan. Lam them van ra (may cham theo cach lam), nhung
     * khong vao de: diem cua de phai tin duoc.
     *  - sbtkhtn8:6.7: 0,06 mol NaOH va 3,21 g ket tua giai ra x khoang 1,5, sach ghi 3.
     *  - sbtkhtn8:29.11c: tinh theo he so no cua sat ra khoang 45 do, sach ghi khoang 30.
     *  - Trac nghiem 8 trang 54 Toan tap mot: khong phuong an nao dung.
     */
    private val KHONG_RA_DE = setOf(
        "sbtkhtn8:6.7",
        "sbtkhtn8:29.11c",
        "sbttoan8t1:Trắc nghiệm 8 (tr.54)"
    )

    /** Cac chuong ma moi bai trong chuong lop deu da hoc. */
    private fun chuongDaXong(cacCau: List<CauHoi>, daHoc: Set<Int>): Set<String> =
        cacCau.groupBy { it.chuong }
            .filter { (_, cau) ->
                val so = cau.mapNotNull { PhanHoc.soBai(it.bai) }
                so.isNotEmpty() && so.all { it in daHoc }
            }.keys

    /**
     * Muc "Ôn tập chương" cua chuong Toan lop vua hoc xong, neu muc do chua co cau nao
     * tung ra (trong de hay da nop). null la khong co chuong nao nhu vay.
     */
    private fun chuongVuaXong(context: Context, bayGio: Long): String? {
        val daHoc = PhanHoc.baiDaHoc(context, LichKiemTra.TOAN, chiPhanDaChon = true) ?: return null
        val kho = KhoBai.get(context)
        val cacCau = NganHang.sachBaiTapCua(LichKiemTra.TOAN).flatMap { kho.cacCauCuaNguon(it.nguon) }
        val xong = chuongDaXong(cacCau, daHoc)
        val daDung = kho.cacCauDaNop(bayGio - MOT_NAM) +
            kho.cacDeTu(bayGio - MOT_NAM).flatMap { it.cauIds }
        return cacCau.filter { it.chuong in xong && PhanHoc.soBai(it.bai) == null }
            .groupBy { it.bai }
            .filter { (_, cau) -> cau.none { it.id in daDung } }
            .keys.lastOrNull()
    }

    /**
     * Chon cau trong cac bai [cacBai], theo thu tu uu tien, mot quyen.
     *
     * Quyen la quyen cua bai uu tien nhat. Trac nghiem va bai tu luan rai deu qua cac bai:
     * moi vong lay mot cau cua moi bai, bai dau truoc.
     */
    private fun chonTheoBai(tuDo: List<CauHoi>, mon: String, cacBai: List<String>): Chon? {
        if (cacBai.isEmpty()) return null
        val nguon = tuDo.firstOrNull { it.bai == cacBai.first() }?.nguon ?: return null
        val cua = tuDo.filter { it.nguon == nguon && it.bai in cacBai }
        val laKhtn = mon == LichKiemTra.KHTN
        val cac = ghep(
            cua,
            soTn = if (laKhtn) SO_TN_KHTN else SO_TN_CHUONG,
            soBaiTl = if (laKhtn) SO_BAI_TL_KHTN else SO_BAI_TL_TOAN,
            uuTien = cacBai
        ) ?: return null
        val tenBai = cacBai.filter { b -> cac.any { it.bai == b } }
        return Chon(nguon, keTenBai(tenBai), cac)
    }

    /**
     * Ghep mot de tu cac cau cua mot quyen: toi da [soTn] trac nghiem, [soBaiTl] bai tu
     * luan (mot bai gom ca cac y a, b, c cua no), khong qua [TOI_DA_Y_TU_LUAN] y. null
     * khi khong du [TOI_THIEU_CAU] cau hay khong co cau tu luan nao.
     *
     * Trac nghiem xep truoc, tu luan sau, deu theo thu tu in trong sach.
     */
    private fun ghep(cua: List<CauHoi>, soTn: Int, soBaiTl: Int, uuTien: List<String>): List<CauHoi>? {
        val theoBai = uuTien.associateWith { b -> cua.filter { it.bai == b } }
        val tn = xoay(theoBai.mapValues { (_, c) -> c.filter { it.bamTrenMay } }, soTn)
        val baiToan = theoBai.mapValues { (_, c) ->
            c.filter { it.dang != "TRAC_NGHIEM" }.groupBy { goc(it.ma) }.values.toList()
        }
        val tl = mutableListOf<List<CauHoi>>()
        var soY = 0
        var vong = 0
        while (tl.size < soBaiTl) {
            var them = false
            for (b in uuTien) {
                val nhom = baiToan[b]?.getOrNull(vong) ?: continue
                if (tl.size >= soBaiTl) break
                if (soY + nhom.size > TOI_DA_Y_TU_LUAN) continue
                tl += nhom
                soY += nhom.size
                them = true
            }
            if (!them) break
            vong++
        }
        if (tl.isEmpty()) return null
        val cac = tn.sortedBy { it.thuTu } + tl.flatten().sortedBy { it.thuTu }
        return cac.takeIf { it.size >= TOI_THIEU_CAU }
    }

    /** Moi vong lay mot phan tu cua moi nhom, nhom dau truoc, toi du [soLuong]. */
    private fun <T> xoay(nhom: Map<String, List<T>>, soLuong: Int): List<T> {
        val ra = mutableListOf<T>()
        var vong = 0
        while (ra.size < soLuong) {
            var them = false
            for (cac in nhom.values) {
                val x = cac.getOrNull(vong) ?: continue
                ra += x
                them = true
                if (ra.size >= soLuong) break
            }
            if (!them) break
            vong++
        }
        return ra
    }

    /** "1.28a" ra "1.28", "Ôn cuối năm 4a" ra "Ôn cuối năm 4": cac y cua cung mot bai. */
    private fun goc(ma: String): String = Regex("""^(.*\d)[a-z]$""").find(ma)?.groupValues?.get(1) ?: ma

    /** "Chương IV. Định lí Thalès" ra 4. */
    private fun soChuong(ten: String): Int? {
        val t = Regex("""Chương\s+([IVX]+)""").find(ten)?.groupValues?.get(1) ?: return null
        val gia = mapOf('I' to 1, 'V' to 5, 'X' to 10)
        var tong = 0
        for (i in t.indices) {
            val g = gia.getValue(t[i])
            val sau = t.getOrNull(i + 1)?.let { gia[it] } ?: 0
            tong += if (g < sau) -g else g
        }
        return tong
    }

    /** "Bài 3, 4, 5", hay ten muc on tap. */
    private fun keTenBai(cacBai: List<String>): String {
        val so = cacBai.mapNotNull { PhanHoc.soBai(it) }
        if (so.size != cacBai.size) return cacBai.joinToString(", ")
        return "Bài " + so.sorted().joinToString(", ")
    }

    /** Gio goi y, lam tron len boi cua nam phut, trong khoang 10 toi 45. */
    fun phutGoiY(cac: List<CauHoi>): Int {
        val giay = cac.sumOf { if (it.bamTrenMay) GIAY_MOI_TN else PHUT_MOI_Y_TU_LUAN * 60 }
        val phut = (giay + 59) / 60
        return (((phut + 4) / 5) * 5).coerceIn(10, 45)
    }

    // ------------------------------------------------------------ doc de

    /**
     * Cac de con viec cho con: chua nop, hay da nop trac nghiem ma chua gui tu luan. De da
     * bat dau thi het han van cho nop them [GIO_NOP_MUON] gio.
     */
    fun dangMo(context: Context, bayGio: Long = System.currentTimeMillis()): List<DeGiai> =
        KhoBai.get(context).cacDeTu(bayGio - 30 * MOT_NGAY).filter { de ->
            val conHan = de.hetHan > bayGio ||
                (de.daBatDau && de.hetHan + GIO_NOP_MUON * 60 * 60_000L > bayGio)
            conHan && !xongViecCuaCon(context, de)
        }.sortedBy { it.hetHan }

    /** De da cham xong trong ngay hom nay, de man chinh hien dong da xong kem diem. */
    fun xongHomNay(context: Context, bayGio: Long = System.currentTimeMillis()): List<DeGiai> {
        val homNay = ngayCua(bayGio)
        return KhoBai.get(context).cacDeTu(bayGio - 30 * MOT_NGAY).filter { de ->
            daCoDiem(context, de) && ngayCua(maxOf(de.nopLuc, de.chamLuc)) == homNay
        }
    }

    fun theoId(context: Context, id: String): DeGiai? = KhoBai.get(context).deTheoId(id)

    fun cacCau(context: Context, de: DeGiai): List<CauHoi> {
        val theoId = KhoBai.get(context).cacCauTheoId(de.cauIds).associateBy { it.id }
        return de.cauIds.mapNotNull { theoId[it] }
    }

    /** Con da lam het phan cua minh chua: nop trac nghiem, va gui tu luan neu de co. */
    fun xongViecCuaCon(context: Context, de: DeGiai): Boolean {
        if (!de.daNop) return false
        return de.daGuiTuLuan || cacCau(context, de).none { !it.bamTrenMay }
    }

    /** De da co diem ca hai phan chua. */
    fun daCoDiem(context: Context, de: DeGiai): Boolean {
        if (!de.daNop) return false
        val coTuLuan = cacCau(context, de).any { !it.bamTrenMay }
        return !coTuLuan || de.tlDung >= 0
    }

    /** So cau dung ca de va tong so cau. Phan chua cham tinh la chua dung. */
    fun diem(context: Context, de: DeGiai): Pair<Int, Int> =
        (de.tnDung.coerceAtLeast(0) + de.tlDung.coerceAtLeast(0)) to de.cauIds.size

    /**
     * Tung cau tu luan dung hay chua, ghi luc cham xong - xem [DeGiai.ketTuLuan]. Cau con
     * bo ra o man soat, hay may khong tra ve, thi khong co trong ket qua.
     */
    fun ketQuaTuLuan(context: Context, de: DeGiai): Map<String, Boolean> = de.ketTuLuan

    /** "Giải đề Toán", "Ôn kiểm tra KHTN". */
    fun tenDe(de: DeGiai): String {
        val mon = tenMon(de.mon)
        return if (de.loai == LOAI_KIEM_TRA) "Ôn kiểm tra $mon" else "Giải đề $mon"
    }

    // ------------------------------------------------------------ lam de

    fun batDau(context: Context, de: DeGiai, bayGio: Long = System.currentTimeMillis()): DeGiai {
        if (de.daBatDau) return de
        val moi = de.copy(batDau = bayGio)
        KhoBai.get(context).luuDe(moi)
        return moi
    }

    /** Con bam mot chu o cau trac nghiem. Bam lai dung chu do la bo chon. */
    fun chon(context: Context, de: DeGiai, cauId: String, chu: String): DeGiai {
        if (de.daNop) return de
        val cu = de.chon[cauId]
        val moi = de.copy(chon = if (cu == chu) de.chon - cauId else de.chon + (cauId to chu))
        KhoBai.get(context).luuDe(moi)
        return moi
    }

    /** Ket qua phan trac nghiem luc con bam Nop bai. */
    data class KetQuaTracNghiem(
        val de: DeGiai,
        val dung: Int,
        val tong: Int,
        /** So phut da cong. 0 khi trac nghiem khong ra phut, hay khong cap duoc. */
        val phut: Int,
        /**
         * Co cau dung ma khong cong duoc phut nao: hom nay het phan lam them, het han muc
         * ngay, hay dang gio ngu. Cau dung luc do chua ghi so, ngay khac lam lai van tinh.
         */
        val khongCapDuoc: Boolean
    )

    /**
     * Con bam Nop bai: cham trac nghiem bang dap an sach, ghi so, cap gio.
     *
     * Ghi so y het [vn.huytl.homeworkgate.telegram.ApprovalService] lam voi bai chup: cau
     * sai luon ghi, de con con duong sua; cau dung chi ghi khi gio da vao tay con that,
     * khong thi ngay khac lam lai cau do van duoc tinh. Diem cua de thi luon ghi, vi no
     * noi con lam duoc gi chu khong phai con duoc bao nhieu phut.
     *
     * Cau khong chon chu nao tinh la sai: het gio chua lam cung la chua lam duoc.
     *
     * @param capGio cap [phut] vao cong, tra ve false khi khong cap duoc. Man hinh truyen
     *   vao cach cap cua no, giong man Kiem tra bai; test truyen mot ham gia.
     */
    fun nopTracNghiem(
        context: Context,
        de: DeGiai,
        bayGio: Long = System.currentTimeMillis(),
        capGio: (Int) -> Boolean
    ): KetQuaTracNghiem {
        if (de.daNop) {
            val tn = cacCau(context, de).filter { it.bamTrenMay }
            return KetQuaTracNghiem(de, de.tnDung.coerceAtLeast(0), tn.size, 0, false)
        }
        val tn = cacCau(context, de).filter { it.bamTrenMay }
        val dung = tn.filter { de.chon[it.id] == it.dapAn }.map { it.id }.toSet()
        val phut = LuatCongGio.phutTracNghiemTrenMay(
            dung.size,
            daCongLamThemHomNay = SoCaiBai.phutLamThemHomNay(context, bayGio),
            goiDaCoHomNay = SoCaiBai.goiDaCoHomNay(context, bayGio)
        )
        val daCap = phut > 0 && capGio(phut)
        val phutCua = if (daCap) chiaDeu(dung.toList(), phut) else emptyMap()

        val kho = KhoBai.get(context)
        val daGhi = mutableListOf<TraLoi>()
        tn.forEach { c ->
            val laDung = c.id in dung
            if (laDung && !daCap) return@forEach
            if (kho.daXong(c.id, bayGio - MOT_NAM)) return@forEach
            val chon = de.chon[c.id].orEmpty()
            val dong = TraLoi(
                cauId = c.id,
                mon = c.mon,
                ma = c.ma,
                de = c.de,
                ketQua = chon,
                baiLam = listOf(if (chon.isEmpty()) "Chưa chọn" else "Chọn $chon"),
                dung = laDung,
                phut = phutCua[c.id] ?: 0,
                nhanXet = "",
                luc = bayGio,
                deId = de.id
            )
            kho.ghiTraLoi(dong)
            daGhi += dong
        }
        runCatching { vn.huytl.homeworkgate.dongbo.DongBo.daySoCai(context, daGhi) }

        val moi = de.copy(nopLuc = bayGio, tnDung = dung.size)
        kho.luuDe(moi)
        return KetQuaTracNghiem(moi, dung.size, tn.size, if (daCap) phut else 0, dung.isNotEmpty() && !daCap)
    }

    /** Phan tu luan da chup va gui di cham. Goi tu man chup luc gui. */
    fun daGuiTuLuan(context: Context, deId: String, bayGio: Long = System.currentTimeMillis()) {
        val kho = KhoBai.get(context)
        val de = kho.deTheoId(deId) ?: return
        if (de.daGuiTuLuan) return
        kho.luuDe(de.copy(guiLuc = bayGio))
    }

    /**
     * Phan tu luan cham xong. Goi tu [vn.huytl.homeworkgate.telegram.ApprovalService]
     * sau khi ghi so, ca duong AI cham lan duong Claude cham.
     *
     * Chi dem cau trong de, lan cham DAU TIEN: lan nop lai de sua cau sai khong di qua day,
     * vi no khong mang ma de. Cham lai lan hai cho cung phan tu luan (Ba Huy nho Claude
     * cham lai) thi lay ket qua moi, van la lan lam dau cua con.
     *
     * @return dong tom tat cho tin Telegram, null khi [deId] khong con de nao.
     */
    fun nhanTuLuan(
        context: Context,
        deId: String,
        daCham: List<CauCham>,
        bayGio: Long = System.currentTimeMillis()
    ): String? {
        val kho = KhoBai.get(context)
        val de = kho.deTheoId(deId) ?: return null
        val tl = cacCau(context, de).filter { !it.bamTrenMay }.map { it.id }.toSet()
        val ket = daCham.filter { tl.contains(it.cauId.orEmpty()) }
            .associate { it.cauId.orEmpty() to it.dung }
        val moi = de.copy(
            tlDung = ket.count { it.value },
            chamLuc = bayGio,
            guiLuc = if (de.guiLuc > 0) de.guiLuc else bayGio,
            ketTuLuan = ket
        )
        kho.luuDe(moi)
        val tomTat = tomTat(context, moi)
        DayLog.add(context, tomTat)
        return tomTat
    }

    /**
     * Mot dong ve de cho Ba Huy: "Giải đề Toán (Bài 3, 4, 5): đúng 7/10 câu (trắc nghiệm
     * 4/4, tự luận 3/6), làm 32 phút, gợi ý 30 phút".
     */
    fun tomTat(context: Context, de: DeGiai): String {
        val cac = cacCau(context, de)
        val tn = cac.count { it.bamTrenMay }
        val tl = cac.size - tn
        val phan = buildList {
            if (tn > 0) add("trắc nghiệm ${de.tnDung.coerceAtLeast(0)}/$tn")
            if (tl > 0) add(if (de.tlDung >= 0) "tự luận ${de.tlDung}/$tl" else "tự luận chưa chấm")
        }
        val dung = de.tnDung.coerceAtLeast(0) + de.tlDung.coerceAtLeast(0)
        val lam = if (de.daBatDau && de.daNop) ((de.nopLuc - de.batDau) / 60_000L).toInt() else -1
        val gio = when {
            lam < 0 -> ""
            lam > de.phutGoiY -> ", làm $lam phút (gợi ý ${de.phutGoiY}, quá ${lam - de.phutGoiY} phút)"
            else -> ", làm $lam phút (gợi ý ${de.phutGoiY})"
        }
        val tong = if (daCoDiem(context, de)) "đúng $dung/${cac.size} câu" else "đã nộp"
        return "${tenDe(de)} (${de.ten}${ngayKiemTra(de)}): $tong (${phan.joinToString(", ")})$gio"
    }

    /** ", kiểm tra ngày 2/10" cho de on truoc kiem tra da doc ra ngay. Rong voi de khac. */
    fun ngayKiemTra(de: DeGiai): String {
        if (de.loai != LOAI_KIEM_TRA || de.ngayKiemTra.isBlank()) return ""
        val d = runCatching { LocalDate.parse(de.ngayKiemTra) }.getOrNull() ?: return ""
        return ", kiểm tra ngày ${d.dayOfMonth}/${d.monthValue}"
    }

    /** Chia deu [phut] cho [ids], phan du cho may cau dau. Cung cach [LuatCongGio]. */
    private fun chiaDeu(ids: List<String>, phut: Int): Map<String, Int> {
        if (ids.isEmpty()) return emptyMap()
        val moi = phut / ids.size
        var du = phut % ids.size
        return ids.associateWith {
            val them = moi + if (du > 0) 1 else 0
            if (du > 0) du--
            them
        }
    }

    private fun ngayCua(ms: Long): LocalDate =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun LocalDateTime.epochMs(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private const val TAG = "GiaiDe"
}
