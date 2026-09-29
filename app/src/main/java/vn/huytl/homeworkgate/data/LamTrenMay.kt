package vn.huytl.homeworkgate.data

import android.content.Context
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Bai may giao lam ngay tren tablet: chon cau cho moi luot, ghi ket qua, cong phut.
 *
 * VI SAO CO. Tu 29/9/2026 moi bai may giao - lam them, luyen cho hay vap, on lai, Giai de -
 * lam tren may bang ban phim ghep, khong chup vo nua (Le Hoa bo qua moi bai bat viet).
 * Chup anh chi con bai trong vo dan do. Luat sao o [LuatGhep], dinh dang cau o [Ghep].
 *
 * CHI CAU TRONG PHAN LE HOA DA HOC, theo moc cua [PhanHoc] (Ba Huy dan ngay 29/9/2026).
 * Tieng Anh dung moc Unit o man Do tu vung.
 *
 * PHUT VA TRAN. Lam them, luyen, Giai de chung tran [LuatCongGio.TRAN_TREN_MAY]; on lai
 * tran rieng [LuatCongGio.TRAN_ON_MOI_NGAY]. Phan vuot tran vao quy gio choi ([QuyGio]).
 * Xong luc gio ngu thi giu toi sang nhu Claude cham trong gio ngu, xem [CongSang].
 */
object LamTrenMay {

    /** Ba mon co cau lam tren may. */
    val MON = listOf("Toán", "Khoa học tự nhiên", PhanHoc.TIENG_ANH)

    /** Mot luot lay bay nhieu cau. Dai hon thi con bo do giua chung. */
    const val SO_CAU_MOI_LUOT = 10

    /** Moi bai toi da bay nhieu cau moi trong mot luot, de danh sach trai ra vai bai. */
    private const val MOI_BAI = 3

    private const val MOT_NGAY = 24L * 60 * 60_000L
    private const val MOT_NAM = 365L * MOT_NGAY

    enum class Loai { LAM_THEM, ON, LUYEN, GIAI_DE }

    /** Mot cau da doc san cach ghep, kem tinh trang vong hien tai. */
    data class Muc(val cau: CauHoi, val ghep: Ghep, val tinhTrang: LuatGhep.TinhTrang)

    fun muc(context: Context, cau: CauHoi): Muc? {
        if (!cau.lamTrenMay) return null
        val g = Ghep.doc(cau.ghep) ?: return null
        return Muc(cau, g, LuatGhep.tinhTrang(KhoBai.get(context).cacLuotTrenMay(cau.id)))
    }

    // ------------------------------------------------------------------ chon cau

    /**
     * Cau cho mot luot lam them cua mot mon. Thu tu:
     *  1. cau dang cho lam lai (vong dau chua du sao, da qua 24 gio);
     *  2. cau con lam sai trong bai dan do chup anh, co trong ngan hang, da qua 24 gio:
     *     sua loi cua chinh minh tren may (Ba Huy chot 29/9/2026);
     *  3. cau moi trong cac bai lop da hoc, bai vua sai truoc, roi bai gan moc.
     *
     * Bo cau cua de Giai de con han, va bo muc "Ôn tập chương", "Test Yourself": de danh
     * cho Giai de.
     */
    fun cauLamThem(
        context: Context,
        mon: String,
        gioiHan: Int = SO_CAU_MOI_LUOT,
        bayGio: Long = System.currentTimeMillis()
    ): List<Muc> {
        val kho = KhoBai.get(context)
        val han = bayGio - MOT_NAM
        val trongDe = kho.cauTrongDeConHan(bayGio)
        val luot = kho.moiLuotTrenMay(han).groupBy { it.cauId }
        val ra = mutableListOf<Muc>()
        val daChon = mutableSetOf<String>()
        fun them(m: Muc) {
            if (ra.size < gioiHan && daChon.add(m.cau.id)) ra += m
        }

        // 1. Lam lai trong vong dau.
        val cauLamLai = kho.cacCauTheoId(luot.keys.toList()).filter { it.mon == mon && it.id !in trongDe }
        cauLamLai.mapNotNull { c -> muc(context, c) }
            .filter { m -> m.tinhTrang.vong == 0 && LuatGhep.moLamLai(m.tinhTrang, m.ghep.sao, bayGio) }
            .forEach(::them)

        // 2. Cau sai cua bai chup anh, sua tren may.
        val saiAnh = kho.moiNhatMoiCau(han)
            .filter { !it.trenMay && !it.dung && it.cauId !in luot && bayGio >= it.luc + MOT_NGAY }
            .map { it.cauId }
        kho.cacCauTheoId(saiAnh).filter { it.mon == mon && it.id !in trongDe }
            .mapNotNull { muc(context, it) }.forEach(::them)

        // 3. Cau moi trong phan da hoc.
        if (ra.size < gioiHan) {
            val xong = kho.cacCauDaXong(han)
            val theoBai = NganHang.cauSbtDaHoc(context, mon)
                .filter {
                    it.lamTrenMay && it.id !in luot && it.id !in xong && it.id !in trongDe &&
                        !it.bai.startsWith("Test Yourself")
                }
                .groupBy { it.bai }
            val vuaSai = kho.baiVuaSaiCuaMon(mon, han).filter { it in theoBai }
            val khoang = NganHang.boDoKhoangCach(context, mon)
            (vuaSai + theoBai.keys.sortedBy(khoang)).distinct().forEach { bai ->
                theoBai.getValue(bai).take(MOI_BAI).mapNotNull { muc(context, it) }.forEach(::them)
            }
        }
        return ra
    }

    /**
     * Cau den hen on lai: vong moi theo lich 3/10/20/30 ngay, hay lam lai trong mot vong on
     * chua du sao. Chi cau lam duoc tren may; cau Ngu van va cau chua soan ghep thi thoi.
     */
    fun cauOn(
        context: Context,
        gioiHan: Int = SO_CAU_MOI_LUOT,
        bayGio: Long = System.currentTimeMillis()
    ): List<Muc> {
        val kho = KhoBai.get(context)
        val han = bayGio - MOT_NAM
        val denHen = kho.cacCauDenHenOn(han, bayGio)
        val lamLaiVongOn = kho.moiLuotTrenMay(han).filter { it.vong >= 1 }.map { it.cauId }.distinct()
        val cac = kho.cacCauTheoId((denHen + lamLaiVongOn).distinct())
            .filter { it.mon in MON }
            .mapNotNull { muc(context, it) }
            .filter { m ->
                m.cau.id in denHen ||
                    (m.tinhTrang.vong >= 1 && LuatGhep.moLamLai(m.tinhTrang, m.ghep.sao, bayGio))
            }
        return cac.take(gioiHan)
    }

    /** So cau dang cho on, cho dong "Ôn lại N câu đến hẹn" ngoai man chinh. */
    fun soCauOn(context: Context, bayGio: Long = System.currentTimeMillis()): Int =
        runCatching { cauOn(context, Int.MAX_VALUE, bayGio).size }.getOrDefault(0)

    /** Cau luyen cho hay vap theo nhan loi, chi cau lam duoc tren may va chua lam tren may. */
    fun cauLuyen(context: Context, nhan: String, gioiHan: Int = KhoBai.SO_CAU_LUYEN): List<Muc> {
        val kho = KhoBai.get(context)
        return kho.cacCauLuyenTheoLoi(nhan, gioiHan = gioiHan * 3)
            .filter { kho.cacLuotTrenMay(it.id).isEmpty() }
            .mapNotNull { muc(context, it) }
            .take(gioiHan)
    }

    // ------------------------------------------------------------------ ghi va cong

    /** Ket qua ghi mot cau: so sao, phut cap ngay, phut vao quy, phut giu toi sang. */
    data class Ghi(val sao: Int, val phutCap: Int, val phutQuy: Int, val phutGiu: Int, val dong: TraLoi)

    /**
     * Ghi mot luot vua xong va cong phut theo tran rieng.
     *
     * [loai] ON va cau dang den hen thi mo vong moi (sao tinh lai tu dau); con lai lam tiep
     * vong hien tai, chi cong phan hon lan tot nhat cua vong do.
     *
     * CAP TRUOC, GHI SO SAU nhu moi duong cap gio khac; nhung gio ngu thi khong cap duoc ma
     * van ghi so, phut giu lai toi sang ([CongSang]) - con lam dung that thi khong mat.
     */
    fun ghi(
        context: Context,
        muc: Muc,
        luot: LuatGhep.Luot,
        traLoi: String,
        loai: Loai,
        deId: String = "",
        bayGio: Long = System.currentTimeMillis(),
        congNgay: Boolean = true
    ): Ghi {
        val kho = KhoBai.get(context)
        val tt = LuatGhep.tinhTrang(kho.cacLuotTrenMay(muc.cau.id))
        val vongMoi = loai == Loai.ON && kho.denHenOn(muc.cau.id, bayGio - MOT_NAM, bayGio)
        val vong = when {
            tt.vong < 0 -> 0
            vongMoi -> tt.vong + 1
            else -> tt.vong
        }
        val totNhatCu = if (tt.vong < 0 || vongMoi) 0 else tt.totNhat
        val sao = LuatGhep.saoKhiXong(luot)
        val lamRa = LuatGhep.phutCong(sao, totNhatCu)
        val onTap = vong >= 1
        val (cap, quy) = if (!congNgay) {
            0 to 0
        } else {
            val daDung = if (onTap) SoCaiBai.phutOnHomNay(context, bayGio)
            else SoCaiBai.phutTrenMayHomNay(context, bayGio)
            val tran = if (onTap) LuatCongGio.TRAN_ON_MOI_NGAY else LuatCongGio.TRAN_TREN_MAY
            LuatGhep.chiaTran(lamRa, daDung, tran)
        }

        var giu = 0
        if (cap > 0) {
            val gate = GateStore(context)
            val nhan = if (onTap) "Ôn lại" else "Làm bài trên máy"
            val duoc = if (gate.trongGioNgu(bayGio)) null else gate.congGioHoc(cap, nhanCho = nhan)
            if (duoc == null) {
                CongSang.them(context, cap, now = bayGio)
                giu = cap
            }
        }

        val dong = TraLoi(
            cauId = muc.cau.id,
            mon = muc.cau.mon,
            ma = muc.cau.ma,
            de = muc.cau.de,
            ketQua = traLoi,
            onTap = onTap,
            dung = luot.dung,
            phut = cap,
            nhanXet = "",
            luc = bayGio,
            deId = deId,
            trenMay = true,
            sao = sao,
            saoToiDa = muc.ghep.sao,
            vong = vong,
            lanSai = luot.lanSai
        )
        kho.ghiTraLoi(dong)
        if (quy > 0) QuyGio.them(context, quy, if (onTap) "Ôn lại" else "Làm bài trên máy")
        runCatching { DongBo.daySoCai(context, listOf(dong)) }
        return Ghi(sao, cap, quy, giu, dong)
    }

    /** Dong nhat ky cho ca mot luot lam, ghi mot lan luc thoat man chu khong tung cau. */
    fun ghiNhatKy(context: Context, mon: String, loai: Loai, cac: List<Ghi>) {
        if (cac.isEmpty()) return
        val ten = when (loai) {
            Loai.ON -> "Ôn lại"
            Loai.LUYEN -> "Luyện chỗ hay vấp"
            Loai.GIAI_DE -> "Giải đề"
            Loai.LAM_THEM -> "Làm bài trên máy"
        }
        val sao = cac.sumOf { it.sao }
        val toiDa = cac.sumOf { it.dong.saoToiDa }
        val phut = cac.sumOf { it.phutCap }
        val quy = cac.sumOf { it.phutQuy }
        val tenMon = if (mon.isBlank()) "" else " " + GiaiDe.tenMon(mon)
        DayLog.add(
            context,
            "$ten$tenMon: ${cac.size} câu, $sao/$toiDa sao, +$phut phút" +
                if (quy > 0) " (vào quỹ $quy phút)" else ""
        )
    }
}
