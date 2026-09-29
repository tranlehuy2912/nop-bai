package vn.huytl.homeworkgate.data

import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Luat sao cua bai lam tren may. Ba Huy chot ngay 29/9/2026:
 *
 *  - Moi cau co V sao, V sao la V phut.
 *  - Moi lan bam Kiem tra ma sai thi mat mot sao; cau nhieu o thi moi o sai mat mot sao.
 *    Sau lan sai dau, may bot phim nhieu.
 *  - Het sao thi cau do 0 phut, nhung con duoc thu them mot lan. Van sai thi may hien loi
 *    giai.
 *  - Lam lai mo sau 24 gio, chi cong phan hon so voi lan tot nhat. Chua du sao thi 24 gio
 *    sau lai mo, toi khi du sao.
 *  - On lai theo hen (3, 10, 20, 30 ngay) la mot vong sao moi, toi da V.
 *  - Tren app khong co chu nao giai thich luat: con tu kham pha, sao tat dan la dau hieu.
 *
 * VI SAO 24 GIO MOI LAM LAI. Cho lam lai ngay thi con vua xep xong, con nho nguyen dap an,
 * lan lam lai chac chan du sao: tru sao mat het tac dung, sai bao nhieu cung go lai lien.
 * Qua mot dem ma con con nho thi do la hoc that, va phan sao go lai la phan thuong xung dang.
 *
 * Tat ca ham o day la ham thuan, khong doc kho hay prefs, de test thang. Ben goi doc cac
 * luot cu tu [vn.huytl.homeworkgate.kho.KhoBai.cacLuotTrenMay].
 */
object LuatGhep {

    /** Lam lai mo sau chung nay, tinh tu luc xong luot truoc. */
    const val CHO_LAM_LAI_MS = 24L * 60 * 60_000L

    /**
     * Mot luot dang lam: tu luc hien cau toi luc xong (dung, hay het ca lan thu them).
     *
     * [thuThem] la luc sao da ve 0 ma con van duoc thu mot lan nua.
     */
    data class Luot(
        val saoToiDa: Int,
        val sao: Int = saoToiDa,
        val lanSai: Int = 0,
        val botNhieu: Boolean = false,
        val thuThem: Boolean = false,
        val xong: Boolean = false,
        val dung: Boolean = false,
        val hienLoiGiai: Boolean = false
    )

    /**
     * Ket qua mot lan bam Kiem tra. [soSai] la so cho sai cua lan do: 0 la dung, 1 cho cau
     * mot cho, so o sai cho cau nhieu o. Goi khi luot da xong thi tra nguyen luot.
     */
    fun kiem(luot: Luot, soSai: Int): Luot {
        if (luot.xong) return luot
        if (soSai <= 0) return luot.copy(xong = true, dung = true)
        if (luot.thuThem) {
            return luot.copy(lanSai = luot.lanSai + 1, xong = true, dung = false, hienLoiGiai = true)
        }
        val con = (luot.sao - soSai).coerceAtLeast(0)
        return luot.copy(
            sao = con,
            lanSai = luot.lanSai + 1,
            botNhieu = true,
            thuThem = con == 0
        )
    }

    /** So sao cua luot khi da xong: dung thi la sao con lai, sai het thi 0. */
    fun saoKhiXong(luot: Luot): Int = if (luot.xong && luot.dung) luot.sao else 0

    // ------------------------------------------------------------------ vong va lam lai

    /** Tinh trang cua mot cau trong vong hien tai, doc tu cac luot cu. */
    data class TinhTrang(
        /** Vong cua luot gan nhat; -1 la chua lam tren may lan nao. */
        val vong: Int,
        /** Sao tot nhat da dat trong vong do. */
        val totNhat: Int,
        /** Luc xong luot gan nhat trong vong do, 0 la chua co. */
        val lanCuoi: Long,
        /** Da tung sai (mat sao hay hong han) o vong dau: dieu kien vao lich on. */
        val tungSai: Boolean
    )

    fun tinhTrang(cacLuot: List<TraLoi>): TinhTrang {
        val tren = cacLuot.filter { it.trenMay }
        if (tren.isEmpty()) return TinhTrang(-1, 0, 0L, false)
        val vong = tren.maxOf { it.vong }
        val trongVong = tren.filter { it.vong == vong }
        return TinhTrang(
            vong = vong,
            totNhat = trongVong.maxOf { if (it.dung) it.sao.coerceAtLeast(0) else 0 },
            lanCuoi = trongVong.maxOf { it.luc },
            tungSai = tren.any { it.vong == 0 && (!it.dung || it.lanSai > 0) }
        )
    }

    /**
     * Cau nay dang cho lam lai trong vong hien tai khong: chua du sao va da qua 24 gio tu
     * luot cuoi. Vong chua co luot nao thi khong phai lam lai.
     */
    fun moLamLai(t: TinhTrang, saoToiDa: Int, bayGio: Long): Boolean =
        t.vong >= 0 && t.totNhat < saoToiDa && bayGio >= t.lanCuoi + CHO_LAM_LAI_MS

    /** Con bao lau nua thi mo lam lai, 0 la da mo (hay khong co gi de mo). */
    fun conChoMs(t: TinhTrang, saoToiDa: Int, bayGio: Long): Long =
        if (t.vong < 0 || t.totNhat >= saoToiDa) 0L
        else (t.lanCuoi + CHO_LAM_LAI_MS - bayGio).coerceAtLeast(0L)

    /**
     * So phut luot vua xong duoc cong: phan hon so voi lan tot nhat cua cung vong. Vong moi
     * (lan dau, hay mot lan on) thi [totNhatCu] la 0 nen duoc tron so sao.
     */
    fun phutCong(saoVuaDat: Int, totNhatCu: Int): Int = (saoVuaDat - totNhatCu).coerceAtLeast(0)

    // ------------------------------------------------------------------ tran va quy

    /**
     * Chia so phut vua lam ra thanh phan cap ngay va phan vao quy gio choi.
     *
     * @param lamRa so phut cua luot (hay ca de) vua xong.
     * @param daDung so phut phan nay da cap trong ngay.
     * @param tran tran ngay cua phan nay.
     * @return (cap ngay, vao quy).
     */
    fun chiaTran(lamRa: Int, daDung: Int, tran: Int): Pair<Int, Int> {
        val con = (tran - daDung).coerceAtLeast(0)
        val cap = lamRa.coerceIn(0, con)
        return cap to (lamRa.coerceAtLeast(0) - cap)
    }
}
