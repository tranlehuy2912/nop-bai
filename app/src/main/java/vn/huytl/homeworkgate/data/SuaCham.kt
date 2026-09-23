package vn.huytl.homeworkgate.data

import android.content.Context
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Sua ban cham theo ket qua Claude cham lai: cau may bao sai ma that ra con lam dung.
 *
 * VI SAO CO. May cham tren tablet dung mot model nhe, va no doc nham lan giai nham
 * duoc. Bai nop 10:23 ngay 23/9/2026 co cau 2.32b bi doc chu "b" thanh so 1, va cau
 * 2.33a thi may tu nhan sai. Ca hai cau con lam dung, nhung so ghi la sai, nen con bi
 * bat sua hai cau khong co gi de sua. Ba Huy nho Claude cham lai tren dien thoai, dan
 * ket qua vao Bang dieu khien, va Bang dieu khien go lenh
 * [vn.huytl.homeworkgate.dongbo.Lenh.SUA_CHAM] sang day.
 *
 * TINH GIO NHU LUC MAY TU CHAM. Cac cau duoc sua di qua [LuatCongGio.tinh] y nhu mot
 * lan cham binh thuong: cung gia moi cau, cung tran ngay, cung luat cum trac nghiem.
 * Khong co bang gia rieng cho duong nay.
 *
 * CHI SUA CAU DANG CHO SUA. Cau da co mot dong dung trong so tuc la da duoc tra gio,
 * ghi them la tra hai lan. Nen gui lai cung mot lenh, hay go lenh sau khi con da tu
 * sua xong, deu khong cong them phut nao. Con cau may bao dung ma Claude bao sai thi
 * khong dung toi o day: gio da vao tay con, va rut lai vi mot lan cham lai la phat
 * con vi loi cua may.
 *
 * Chia hai buoc [chuanBi] va [ghi] de ben goi cap gio o giua. Cap khong duoc, vi du
 * dang gio ngu, thi khong ghi so: cau van cho sua va Ba Huy gui lai luc khac duoc.
 * Dung le cua duong AI tu cham.
 */
object SuaCham {

    /**
     * Mot cau Claude cham lai la dung.
     *
     * Kem de bai de phan biet hai cau trung ma o hai quyen sach, vi du 2.28 cua Toan
     * tap mot va 2.28 cua mot sach khac cung dang cho sua.
     */
    data class Cau(val ma: String, val de: String = "")

    data class ChuanBi(
        /** Cac cau se ghi la dung, da dien du dang bai va so dong de tinh phut. */
        val cac: List<CauCham>,
        val bang: LuatCongGio.BangTinh,
        /** Ma cac cau duoc gui sang ma khong con dang cho sua. */
        val boQua: List<String>
    ) {
        val phut: Int get() = bang.phut
    }

    fun chuanBi(
        context: Context,
        danhSach: List<Cau>,
        now: Long = System.currentTimeMillis()
    ): ChuanBi {
        val cho = SoCaiBai.dangChoSua(context, now)
        val kho = KhoBai.get(context)
        val boQua = mutableListOf<String>()
        val daChon = mutableSetOf<String>()

        val cac = danhSach.mapNotNull { yeu ->
            val cungMa = cho.filter { it.ma.trim() == yeu.ma.trim() }
            val so = if (cungMa.size <= 1) {
                cungMa.firstOrNull()
            } else {
                cungMa.firstOrNull { SoCaiBai.chuanHoa(it.de) == SoCaiBai.chuanHoa(yeu.de) }
            }
            if (so == null || !daChon.add(so.khoa)) {
                boQua += yeu.ma
                return@mapNotNull null
            }
            // Lan cham gan nhat cua cau, de lay lai dung chu con viet va so dong: so
            // phut cua cau nho tinh theo so dong lam bai.
            val cuoi = kho.lichSuCua(so.khoa).firstOrNull()
            val dang = kho.cauTheoId(so.khoa)?.dang
                ?.let { runCatching { DangBai.valueOf(it) }.getOrNull() }
                ?: DangBai.CAU_NHO
            CauCham(
                ma = so.ma,
                de = so.de,
                ketQua = cuoi?.ketQua.orEmpty(),
                dung = true,
                docRo = true,
                dang = dang,
                soDong = cuoi?.baiLam?.size ?: 0,
                baiLam = cuoi?.baiLam.orEmpty(),
                cauId = so.khoa,
                mon = cuoi?.mon.orEmpty()
            )
        }

        val bang = LuatCongGio.tinh(
            KetQuaCham(mon = cac.firstOrNull()?.mon.orEmpty(), cac = cac),
            daCongLamThemHomNay = SoCaiBai.phutLamThemHomNay(context, now),
            bayGio = LuatCongGio.bayGio(now),
            goiDaCoHomNay = SoCaiBai.goiDaCoHomNay(context, now)
        )
        return ChuanBi(cac, bang, boQua)
    }

    /** Ghi cac cau da chuan bi vao so, voi dung so phut [LuatCongGio] da tinh. */
    fun ghi(
        context: Context,
        chuanBi: ChuanBi,
        now: Long = System.currentTimeMillis()
    ): List<TraLoi> = SoCaiBai.ghi(context, chuanBi.cac, chuanBi.bang.phutCua, now)
}
