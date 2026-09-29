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
import vn.huytl.homeworkgate.kho.Ghep
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
 * CACH LAM (tu 29/9/2026). Ca de lam tren may bang ban phim ghep, khong chup phan tu luan
 * nua (Le Hoa bo qua moi bai bat viet). Con bam Bat dau thi dong ho chay; moi cau mot khung
 * ghep co sao rieng, bam Kiem tra sai thi mat sao ngay trong de - cung luat sao voi bai lam
 * them, xem [LuatGhep]. Bam Nop bai thi moi cau ghi so, cong phut. Het gio van nop duoc,
 * tin cho Ba Huy ghi lam bao lau (Ba Huy chon ngay 27/9/2026). Thoat ra vao lai thi sao
 * tung cau giu nguyen ([luotCua]): khong co cach nao thoat ra de lay lai sao.
 *
 * GIO CHOI tinh y het bai lam them: moi sao mot phut, chung tran
 * [LuatCongGio.TRAN_TREN_MAY], phan vuot tran vao Quỹ giờ chơi. Diem cua de khong doi ra
 * phut nao them.
 *
 * DIEM la tong sao con dat duoc luc nop tren tong sao toi da cua de (Ba Huy chot 29/9/2026).
 * Cau chua xong luc nop tinh 0 sao, va 24 gio sau mo lam lai nhu moi cau khac.
 *
 * CAU NAO DUOC VAO DE. Chi cau lam duoc tren may (co "ghep"), chua tung lam, khong nam
 * trong de khac con han, cung mot quyen. Moi de co it nhat mot cau khong phai trac nghiem.
 * De cu (truoc 29/9/2026, [DeGiai.trenMay] = false) van chay theo luat cu cho toi het han.
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

    /** De tuan Tieng Anh: bay nhieu cau chon, bay nhieu cau ghep (tu, cau, doan). */
    private const val SO_TN_ANH = 10
    private const val SO_BAI_TL_ANH = 6

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

    /** Ba mon co sach bai tap lam tren may. */
    val MON = listOf(LichKiemTra.TOAN, LichKiemTra.KHTN, PhanHoc.TIENG_ANH)

    /** Ten mon cho dong chu ngan: "Toán", "KHTN", "Tiếng Anh". */
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
        saoToiDa = tongSao(chon.cac),
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
        if (mon == PhanHoc.TIENG_ANH) {
            testVuaToi(context, bayGio)?.let { ten ->
                val cua = cauCuaMuc(context, mon, ten, bayGio)
                if (cua.size >= TOI_THIEU_CAU) return Chon(cua.first().nguon, ten, cua)
            }
        }
        if (mon == LichKiemTra.TOAN) {
            chuongVuaXong(context, bayGio)?.let { onTap ->
                val cua = tuDo.filter { it.bai == onTap }
                ghep(cua, soTn = SO_TN_CHUONG, soBaiTl = SO_BAI_TL_CHUONG, uuTien = listOf(onTap))
                    ?.let { return Chon(it.first().nguon, onTap, it) }
            }
        }
        val khoang = NganHang.boDoKhoangCach(context, mon)
        val baiGan = tuDo.map { it.bai }.distinct()
            .filter { khoang(it) < SO_BAI_GAN_MOC }
            .sortedBy(khoang)
        return chonTheoBai(tuDo, mon, baiGan)
            // Bai gan moc da het cau: lui ve moi bai da hoc, gan moc truoc.
            ?: chonTheoBai(tuDo, mon, tuDo.map { it.bai }.distinct().sortedBy(khoang))
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
     * Bai Test Yourself cua sach bai tap Tieng Anh ma lop vua hoc toi (xong Unit 3, 6, 9 hay
     * 12) va chua cau nao cua no tung ra. Dung nhu muc "Ôn tập chương" cua SBT Toan: tuan do
     * de tuan la nguyen bai Test Yourself (Ba Huy dong y ngay 29/9/2026).
     */
    private fun testVuaToi(context: Context, bayGio: Long): String? {
        val daHoc = PhanHoc.baiDaHoc(context, PhanHoc.TIENG_ANH) ?: return null
        val unit = daHoc.maxOrNull() ?: return null
        if (unit < 3) return null
        val ten = "Test Yourself ${unit / 3}"
        val kho = KhoBai.get(context)
        val daDung = kho.cacCauDaNop(bayGio - MOT_NAM) + kho.cacDeTu(bayGio - MOT_NAM).flatMap { it.cauIds }
        val cua = NganHang.sachBaiTapCua(PhanHoc.TIENG_ANH).flatMap { kho.cacCauCuaNguon(it.nguon) }
            .filter { it.bai == ten }
        return ten.takeIf { cua.isNotEmpty() && cua.none { it.id in daDung } }
    }

    /** Moi cau lam duoc tren may cua mot muc (mot bai, mot Test Yourself), theo thu tu in. */
    private fun cauCuaMuc(context: Context, mon: String, bai: String, bayGio: Long): List<CauHoi> {
        val kho = KhoBai.get(context)
        val trongDe = kho.cauTrongDeConHan(bayGio)
        return NganHang.sachBaiTapCua(mon).flatMap { kho.cacCauCuaNguon(it.nguon) }
            .filter { it.bai == bai && hopLe(it) && it.id !in trongDe }
    }

    /**
     * Cau SBT co the vao de: trong cac bai lop da hoc (ca muc on tap chuong cua chuong
     * da hoc xong), lam duoc tren may, chua tung nop, khong nam trong de khac con han.
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
     * Cau hop le cho mot de: lam duoc tren may (co "ghep", khong bi "bo_may"). Tu 29/9/2026
     * moi kieu cau deu lam tren may, ke ca dung/sai tung y va noi cot.
     */
    private fun hopLe(c: CauHoi): Boolean =
        c.lamTrenMay && c.dang != "KHONG_TINH" && c.id !in KHONG_RA_DE

    /** Cau chon mot phuong an (kieu CHON): xep vao phan trac nghiem cua de. */
    private fun laChon(c: CauHoi): Boolean = c.ghep.contains("\"kieu\":\"CHON\"")

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
        val cac = ghep(
            cua,
            soTn = when (mon) {
                LichKiemTra.KHTN -> SO_TN_KHTN
                PhanHoc.TIENG_ANH -> SO_TN_ANH
                else -> SO_TN_CHUONG
            },
            soBaiTl = when (mon) {
                LichKiemTra.KHTN -> SO_BAI_TL_KHTN
                PhanHoc.TIENG_ANH -> SO_BAI_TL_ANH
                else -> SO_BAI_TL_TOAN
            },
            uuTien = cacBai
        ) ?: return null
        val tenBai = cacBai.filter { b -> cac.any { it.bai == b } }
        return Chon(nguon, keTenBai(tenBai, mon), cac)
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
        val tn = xoay(theoBai.mapValues { (_, c) -> c.filter(::laChon) }, soTn)
        val baiToan = theoBai.mapValues { (_, c) ->
            c.filterNot(::laChon).groupBy { goc(it.ma) }.values.toList()
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
    private fun goc(ma: String): String = MA_CO_Y.find(ma)?.groupValues?.get(1) ?: ma

    /** "2.26d" la y d cua cau 2.26. Dung mot lan cho moi cau luc ra de, nen dung san mot lan. */
    private val MA_CO_Y = Regex("""^(.*\d)[a-z]$""")

    /** "Chương IV. Định lí Thalès" ra 4. */
    private fun soChuong(ten: String): Int? {
        // Cung mot cach doc voi lich kiem tra ([LichKiemTra.laMa]): hai ben phai ra cung
        // mot so thi "kiểm tra chương III" moi khop cau cua chuong do.
        val t = Regex("""Chương\s+([IVX]+|\d+)""").find(ten)?.groupValues?.get(1) ?: return null
        val tong = t.toIntOrNull() ?: LichKiemTra.laMa(t.lowercase()) ?: return null
        return tong
    }

    /**
     * "Bài 3, 4, 5", "Unit 1, Unit 2" (Tieng Anh), hay ten muc on tap.
     *
     * Tieng Anh ke ten ngan cua tung muc chu khong ke so: [PhanHoc.soBai] doc "Unit 2. Life
     * in the countryside" ra 2 va "Test Yourself 1" ra 3, nen truoc ngay 29/9/2026 toi de
     * Tieng Anh mang ten "Bài 1, 2".
     */
    private fun keTenBai(cacBai: List<String>, mon: String): String {
        val so = cacBai.mapNotNull { PhanHoc.soBai(it) }
        if (so.size != cacBai.size) return cacBai.joinToString(", ")
        if (mon == PhanHoc.TIENG_ANH) {
            return cacBai.sortedBy { PhanHoc.soBai(it) }.joinToString(", ") { it.substringBefore(".").trim() }
        }
        return "Bài " + so.sorted().joinToString(", ")
    }

    /**
     * Gio goi y, lam tron len boi cua nam phut, trong khoang 10 toi 60: cau chon mot phut
     * ruoi, cau khac mot phut moi sao (sao di theo so dong loi giai).
     */
    fun phutGoiY(cac: List<CauHoi>): Int {
        val giay = cac.sumOf { c ->
            val sao = Ghep.doc(c.ghep)?.sao ?: 1
            if (laChon(c)) GIAY_MOI_TN else sao * 60
        }
        val phut = (giay + 59) / 60
        return (((phut + 4) / 5) * 5).coerceIn(10, 60)
    }

    /** Tong sao toi da cua cac cau, ghi vao de luc tao. */
    private fun tongSao(cac: List<CauHoi>): Int = cac.sumOf { Ghep.doc(it.ghep)?.sao ?: 0 }

    // ------------------------------------------------------------ doc de

    /**
     * Cac de con viec cho con: chua nop, hay da nop trac nghiem ma chua gui tu luan. De da
     * bat dau thi het han van cho nop them [GIO_NOP_MUON] gio.
     */
    fun dangMo(context: Context, bayGio: Long = System.currentTimeMillis()): List<DeGiai> =
        KhoBai.get(context).cacDeConHan(bayGio - GIO_NOP_MUON * 60 * 60_000L).filter { de ->
            val conHan = de.hetHan > bayGio ||
                (de.daBatDau && de.hetHan + GIO_NOP_MUON * 60 * 60_000L > bayGio)
            conHan && !xongViecCuaCon(context, de)
        }.sortedBy { it.hetHan }

    /** De da cham xong trong ngay hom nay, de man chinh hien dong da xong kem diem. */
    fun xongHomNay(context: Context, bayGio: Long = System.currentTimeMillis()): List<DeGiai> {
        val homNay = ngayCua(bayGio)
        val kho = KhoBai.get(context)
        // Xet ngay truoc: daCoDiem doc cau cua de tu kho, ma phan lon de khong xong hom nay.
        return (kho.cacDeTu(bayGio - 30 * MOT_NGAY) + kho.cacDeConHan(bayGio - 2 * MOT_NGAY))
            .distinctBy { it.id }
            .filter { de -> ngayCua(maxOf(de.nopLuc, de.chamLuc)) == homNay && daCoDiem(context, de) }
    }

    fun theoId(context: Context, id: String): DeGiai? = KhoBai.get(context).deTheoId(id)

    fun cacCau(context: Context, de: DeGiai): List<CauHoi> {
        val theoId = KhoBai.get(context).cacCauTheoId(de.cauIds).associateBy { it.id }
        return de.cauIds.mapNotNull { theoId[it] }
    }

    /** Con da lam het phan cua minh chua: nop trac nghiem, va gui tu luan neu de co. */
    fun xongViecCuaCon(context: Context, de: DeGiai): Boolean {
        if (!de.daNop) return false
        if (de.trenMay) return true
        return daGuiThat(context, de) || cacCau(context, de).none { !it.bamTrenMay }
    }

    /** De da co diem ca hai phan chua. Ba duyet tu luan ma khong cham cung tinh la xong. */
    fun daCoDiem(context: Context, de: DeGiai): Boolean {
        if (!de.daNop) return false
        if (de.trenMay) return true
        val coTuLuan = cacCau(context, de).any { !it.bamTrenMay }
        return !coTuLuan || de.tlDung >= 0 || baDuyetKhongCham(context, de)
    }

    // ------------------------------------------------ bai nop cua phan tu luan

    /*
     * Phan tu luan di nhu mot bai nop thuong, nhung chi duong cham ([nhanTuLuan]) moi ghi
     * diem. Bai ra khoi hang cho bang duong khac thi truoc day de ket o "da gui, chua cham"
     * mai mai: khong diem, khong nut chup lai, bien khoi man chinh. Gio (Ba Huy chon ngay
     * 27/9/2026):
     * - Con huy, Ba Huy khong duyet, bai roi hang luc qua ngay: de mo lai nut chup tu luan.
     * - Ba Huy duyet ma khong cham: de dong, chi co diem trac nghiem.
     *
     * Nho bai nao la tu luan cua de nao trong prefs rieng, khong them cot vao KhoBai: xet
     * "bai con cho khong" luc doc, nen duong nao lam bai roi hang cung ra dung mot ket qua.
     */

    private fun spBai(context: Context) =
        context.applicationContext.getSharedPreferences("giai_de_bai", Context.MODE_PRIVATE)

    /** Ghi ma bai nop cua phan tu luan. Goi ngay sau khi bai vao hang cho. */
    fun ghiBaiTuLuan(context: Context, deId: String, baiId: String) {
        spBai(context).edit().putString("bai_$deId", baiId).remove("duyet_$deId").apply()
    }

    /** Ba Huy duyet bai [baiId]. Bai do la tu luan cua mot de chua cham thi dong de lai. */
    fun baDuyetBai(context: Context, baiId: String) {
        val sp = spBai(context)
        val deId = sp.all.entries.firstOrNull { it.key.startsWith("bai_") && it.value == baiId }
            ?.key?.removePrefix("bai_") ?: return
        val de = KhoBai.get(context).deTheoId(deId) ?: return
        if (de.tlDung >= 0) return
        sp.edit().putBoolean("duyet_$deId", true).apply()
        DayLog.add(context, tomTat(context, de))
    }

    /** Ba Huy da duyet phan tu luan cua [de] ma khong cham. */
    fun baDuyetKhongCham(context: Context, de: DeGiai): Boolean =
        de.tlDung < 0 && spBai(context).getBoolean("duyet_${de.id}", false)

    /**
     * Phan tu luan da gui va con dung: da cham, ba da duyet, hay bai con nam trong hang
     * cho. Bai roi hang ma khong ai cham hay duyet thi la chua gui, de con chup lai.
     * De gui tu ban truoc (chua nho ma bai) thi giu nhu cu.
     */
    fun daGuiThat(context: Context, de: DeGiai): Boolean {
        if (!de.daGuiTuLuan) return false
        if (de.tlDung >= 0 || baDuyetKhongCham(context, de)) return true
        val baiId = spBai(context).getString("bai_${de.id}", null) ?: return true
        return GateStore(context).baiDangCho().any { it.id == baiId }
    }

    /**
     * Diem cua de. De lam tren may: tong sao dat duoc tren tong sao toi da. De cu: so cau
     * dung tren tong so cau, phan chua cham tinh la chua dung.
     */
    fun diem(context: Context, de: DeGiai): Pair<Int, Int> =
        if (de.trenMay) de.saoDat.coerceAtLeast(0) to de.saoToiDa
        else (de.tnDung.coerceAtLeast(0) + de.tlDung.coerceAtLeast(0)) to de.cauIds.size

    // ------------------------------------------------------------ de lam tren may

    /**
     * Trang thai sao cua mot cau trong de dang lam. Luu trong [DeGiai.chon], moi cau mot khoi
     * JSON: s sao con, l so lan sai, x xong, d dung, b da bot nhieu, t dang thu them, h hien
     * loi giai, a cau tra loi. Chua co thi la luot moi du sao.
     */
    fun luotCua(de: DeGiai, cauId: String, saoToiDa: Int): LuatGhep.Luot {
        val o = runCatching { org.json.JSONObject(de.chon[cauId].orEmpty()) }.getOrNull()
            ?: return LuatGhep.Luot(saoToiDa)
        return LuatGhep.Luot(
            saoToiDa = saoToiDa,
            sao = o.optInt("s", saoToiDa).coerceIn(0, saoToiDa),
            lanSai = o.optInt("l"),
            botNhieu = o.optBoolean("b"),
            thuThem = o.optBoolean("t"),
            xong = o.optBoolean("x"),
            dung = o.optBoolean("d"),
            hienLoiGiai = o.optBoolean("h")
        )
    }

    /** Cau tra loi con da ghep o mot cau cua de, ghi vao so luc nop. */
    fun traLoiCua(de: DeGiai, cauId: String): String =
        runCatching { org.json.JSONObject(de.chon[cauId].orEmpty()).optString("a") }.getOrDefault("")

    /** Ghi trang thai sao cua mot cau sau moi lan bam Kiem tra, de thoat ra vao lai van giu. */
    fun luuLuot(context: Context, de: DeGiai, cauId: String, luot: LuatGhep.Luot, traLoi: String): DeGiai {
        if (de.daNop) return de
        val o = org.json.JSONObject()
            .put("s", luot.sao).put("l", luot.lanSai).put("x", luot.xong).put("d", luot.dung)
            .put("b", luot.botNhieu).put("t", luot.thuThem).put("h", luot.hienLoiGiai).put("a", traLoi)
        val moi = de.copy(chon = de.chon + (cauId to o.toString()))
        KhoBai.get(context).luuDe(moi)
        return moi
    }

    /** Ket qua nop mot de lam tren may. */
    data class KetQuaNop(
        val de: DeGiai,
        val saoDat: Int,
        val saoToiDa: Int,
        val phutCap: Int,
        val phutQuy: Int,
        val phutGiu: Int
    )

    /**
     * Con bam Nop bai o de lam tren may: moi cau ghi so va cong phut theo luat sao. Cau chua
     * xong tinh la het luot, 0 sao; 24 gio sau cau do mo lam lai nhu moi cau khac.
     */
    fun nop(context: Context, de: DeGiai, bayGio: Long = System.currentTimeMillis()): KetQuaNop {
        if (de.daNop) return KetQuaNop(de, de.saoDat.coerceAtLeast(0), de.saoToiDa, 0, 0, 0)
        val ghi = cacCau(context, de).mapNotNull { c ->
            val m = LamTrenMay.muc(context, c) ?: return@mapNotNull null
            val l = luotCua(de, c.id, m.ghep.sao)
            val xong = if (l.xong) l else l.copy(xong = true, dung = false)
            LamTrenMay.ghi(context, m, xong, traLoiCua(de, c.id), LamTrenMay.Loai.GIAI_DE, deId = de.id, bayGio = bayGio)
        }
        val sao = ghi.sumOf { it.sao }
        val moi = de.copy(nopLuc = bayGio, saoDat = sao)
        KhoBai.get(context).luuDe(moi)
        DayLog.add(context, tomTat(context, moi))
        return KetQuaNop(moi, sao, de.saoToiDa, ghi.sumOf { it.phutCap }, ghi.sumOf { it.phutQuy }, ghi.sumOf { it.phutGiu })
    }

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
        // De cu (truoc 29/9/2026): trac nghiem mot phut mot cau, chung tran lam bai tren may.
        val phut = LuatGhep.chiaTran(
            dung.size * LuatCongGio.PHUT_MOI_CAU_TRAC_NGHIEM,
            SoCaiBai.phutTrenMayHomNay(context, bayGio),
            LuatCongGio.TRAN_TREN_MAY
        ).first
        val daCap = phut > 0 && capGio(phut)
        val phutCua = if (daCap) LuatCongGio.chiaDeuTheoMa(dung.toList(), phut) else emptyMap()

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
                deId = de.id,
                trenMay = true,
                sao = if (laDung) 1 else 0,
                saoToiDa = 1
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
        // Gui lai sau khi bai truoc roi hang (xem [daGuiThat]) thi ghi lai luc gui.
        if (de.daGuiTuLuan && daGuiThat(context, de)) return
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
        // Gop voi ket qua da co, khong thay het: lan cham sau (con chup lai phan tu luan,
        // hay bai sua) chi mang nhung cau chua tra gio, thay het thi diem cua lan dau mat.
        val ket = de.ketTuLuan + daCham.filter { tl.contains(it.cauId.orEmpty()) }
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
        if (de.trenMay) {
            val lam = if (de.daBatDau && de.daNop) ((de.nopLuc - de.batDau) / 60_000L).toInt() else -1
            val gio = when {
                lam < 0 -> ""
                lam > de.phutGoiY -> ", làm $lam phút (gợi ý ${de.phutGoiY}, quá ${lam - de.phutGoiY} phút)"
                else -> ", làm $lam phút (gợi ý ${de.phutGoiY})"
            }
            val ket = if (de.daNop) "${de.saoDat.coerceAtLeast(0)}/${de.saoToiDa} sao" else "chưa nộp"
            return "${tenDe(de)} (${de.ten}${ngayKiemTra(de)}): $ket$gio"
        }
        val cac = cacCau(context, de)
        val tn = cac.count { it.bamTrenMay }
        val tl = cac.size - tn
        val phan = buildList {
            if (tn > 0) add("trắc nghiệm ${de.tnDung.coerceAtLeast(0)}/$tn")
            if (tl > 0) add(
                when {
                    de.tlDung >= 0 -> "tự luận ${de.tlDung}/$tl"
                    baDuyetKhongCham(context, de) -> "tự luận ba duyệt, không chấm"
                    else -> "tự luận chưa chấm"
                }
            )
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

    private fun ngayCua(ms: Long): LocalDate =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun LocalDateTime.epochMs(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private const val TAG = "GiaiDe"
}
