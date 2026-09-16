package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Mot cau da nop, doc ra tu kho.
 *
 * @param khoa danh tinh cua cau - xem [khoaCua].
 * @param phut so phut da tra cho cau nay. 0 nghia la chua tra (dang sai, cho sua).
 * @param xong cau nay da lam dung va da tra gio chua.
 */
data class CauSo(
    val khoa: String,
    val ma: String,
    val de: String,
    val phut: Int,
    val xong: Boolean,
    val nhanXet: String,
    val luc: Long
)

/**
 * So cai cac cau da nop. Tu ban nay tro di no chi la mot lop mong tren [KhoBai].
 *
 * Hai viec, va ca hai deu khong the thieu khi de AI tu duyet:
 *
 *  1. MOI CAU CHI TRA GIO MOT LAN. Khong co so nay thi con chup lai dung trang bai
 *     hom qua la duoc cong gio lan nua, va AI - von khong nho gi giua hai lan goi -
 *     se duyet that.
 *  2. NHO CAC CAU DANG SAI de man hinh cua con hien ra "con hai cau can sua", va de
 *     lan nop sau biet cau nay la sua bai cu chu khong phai bai moi.
 *
 * DANH TINH CUA MOT CAU - cho da phai sua sau hai ngay chay thu. Truoc day khoa la
 * DE BAI DO AI CHEP LAI, da chuan hoa. No hong theo ca hai chieu: AI chep de moi
 * lan mot khac nen cung mot bai ra hai khoa (tinh gio lan hai), ma hai cau ngan
 * khac nhau lai chuan hoa ra giong nhau (bao "da lam roi" oan). Bay gio khoa lay
 * theo thu tu:
 *
 *  1. MA CAU TRONG SACH ("toan8t1:2.26a") - khi con da khai minh dang lam bai nao.
 *     Co dinh, khong phu thuoc vao chu nghia AI doc duoc.
 *  2. DE BAI DA CHUAN HOA - duong lui cho bai ngoai sach: vo bai tap, phieu photo,
 *     cac mon chua nap sach vao may. Van hong nhu cu, nhung co con hon khong.
 *
 * Giu [GIU_NGAY] ngay roi xoa. Mot dua tre khong quay lai bai cua thang truoc, ma
 * giu mai thi danh sach chi dai ra.
 */
object SoCaiBai {

    private const val K_SO_CU = "so_cai_bai"
    private const val K_LOI_NHAN = "so_cai_loi_nhan"
    private const val K_LOI_NHAN_LUC = "so_cai_loi_nhan_luc"

    /** Khoa gia cho ban ghi "hom nay da tinh tron goi". */
    private const val KHOA_GOI = KhoBai.CAU_GOI

    /** Dau cua khoa lam tu de bai, de nhin mot cai la biet cau do ngoai sach. */
    const val DAU_NGOAI_SACH = "tu:"

    /**
     * Nho bai da cham trong bao lau.
     *
     * Mot nam hoc. Ngan hon thi den thang sau con chup lai chinh trang nay la duoc
     * tinh gio lan nua - ma do la dung cai so nay sinh ra de chan.
     */
    private const val GIU_NGAY = 365

    private fun han(now: Long) = now - GIU_NGAY * 24 * 60 * 60_000L

    /**
     * Danh tinh cua mot cau.
     *
     * Con da khai dang lam bai nao thi [CauCham.cauId] co san ma sach - dung luon.
     * Khong thi quay ve de bai da chuan hoa, va neu ca de cung rong thi lay ma cau.
     */
    fun khoaCua(cau: CauCham): String {
        cau.cauId?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val chu = chuanHoa(cau.de).ifEmpty { chuanHoa(cau.ma) }
        return if (chu.isEmpty()) "" else DAU_NGOAI_SACH + chu
    }

    /** Doc toan bo so cai - tinh trang hien tai cua tung cau, cu nhat truoc. */
    fun tatCa(context: Context, now: Long = System.currentTimeMillis()): List<CauSo> =
        KhoBai.get(context).moiNhatMoiCau(han(now)).map { it.sangCauSo() }

    /** Cac cau dang sai, cho con sua lai. */
    fun dangChoSua(context: Context, now: Long = System.currentTimeMillis()): List<CauSo> =
        KhoBai.get(context).dangChoSua(han(now)).map { it.sangCauSo() }

    /** Cau nay da duoc tra gio lan nao chua, hoi bang de bai (bai ngoai sach). */
    fun daTraGio(context: Context, de: String, now: Long = System.currentTimeMillis()): Boolean {
        val k = chuanHoa(de)
        if (k.isEmpty()) return false
        return daTraGioTheoKhoa(context, DAU_NGOAI_SACH + k, now)
    }

    /** Cau nay da duoc tra gio lan nao chua, hoi bang chinh cau vua cham. */
    fun daTraGioCua(
        context: Context,
        cau: CauCham,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val k = khoaCua(cau)
        return k.isNotEmpty() && daTraGioTheoKhoa(context, k, now)
    }

    fun daTraGioTheoKhoa(
        context: Context,
        khoa: String,
        now: Long = System.currentTimeMillis()
    ): Boolean = KhoBai.get(context).daXong(khoa, han(now))

    /**
     * Ghi ket qua mot lan cham vao so.
     *
     * Cau da lam dung va da tra gio thi BO QUA han: gio cua no da vao tay con roi,
     * ghi them mot dong nua chi lam no duoc tinh hai lan.
     */
    fun ghi(
        context: Context,
        cac: List<CauCham>,
        phutCua: Map<String, Int>,
        now: Long = System.currentTimeMillis()
    ) {
        if (cac.isEmpty()) return
        val kho = KhoBai.get(context)
        val tuLuc = han(now)

        cac.forEach { c ->
            val k = khoaCua(c)
            if (k.isEmpty()) return@forEach
            if (kho.daXong(k, tuLuc)) return@forEach
            kho.ghiTraLoi(
                TraLoi(
                    cauId = k,
                    mon = c.mon,
                    ma = c.ma,
                    de = c.de,
                    ketQua = c.ketQua,
                    baiLam = c.baiLam,
                    dongSai = c.dongSai,
                    dung = c.dung,
                    phut = if (c.dung) phutCua[c.ma] ?: 0 else 0,
                    // Cau da xong thi khong giu loi nhan xet: no chi de con biet duong
                    // ma sua, xong roi la het viec. Giu lai chi phinh so va de lai loi
                    // che bai cua mot dua tre trong may ca nam.
                    nhanXet = if (c.dung) "" else c.nhanXet,
                    luc = now
                )
            )
        }
    }

    /** Hom nay da tinh tron goi vo dan do chua. */
    fun goiDaCoHomNay(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        KhoBai.get(context).coTrongKhoang(KHOA_GOI, moc0GioCua(now))

    /** Danh dau da tinh tron goi hom nay. */
    fun ghiGoi(context: Context, phut: Int, now: Long = System.currentTimeMillis()) {
        // Mot ngay mot goi. Ghi hai dong la mot buoi chieu duoc chin muoi phut.
        if (goiDaCoHomNay(context, now)) return
        KhoBai.get(context).ghiTraLoi(
            TraLoi(
                cauId = KHOA_GOI,
                mon = "",
                ma = "trọn gói",
                de = "làm hết bài cô giao",
                ketQua = "",
                dung = true,
                phut = phut,
                nhanXet = "",
                luc = now
            )
        )
    }

    /**
     * So phut phan LAM THEM da cong trong ngay, de giu tran cua [LuatCongGio].
     *
     * Khong tinh tron goi vo dan do. Tran 90 phut la tran rieng cua phan lam them;
     * goi 45 phut nam ngoai no, nen mot ngay toi da 135. Cong ca goi vao day thi
     * ngay nao co goi, tran lam them tu tut xuong con 45 ma khong ai noi gi.
     */
    fun phutLamThemHomNay(context: Context, now: Long = System.currentTimeMillis()): Int =
        KhoBai.get(context).tongPhut(moc0GioCua(now), truCauId = KHOA_GOI)

    /** Tong so phut da cong trong ngay, ke ca tron goi. Dung de bao cao. */
    fun phutDaCongHomNay(context: Context, now: Long = System.currentTimeMillis()): Int =
        KhoBai.get(context).tongPhut(moc0GioCua(now))

    /**
     * Loi nhan gan nhat cho con, hien tren man hinh cua no.
     *
     * Can cho truong hop khong co cau nao cho sua ma cung khong duoc cong gio - vi
     * du nop lai dung bai da cham hom truoc. Khong co dong nay thi con bam nop, cho
     * mot luc, roi khong thay gi thay doi ca.
     *
     * Van nam trong prefs chu khong xuong kho: day khong phai so sach, no chi la
     * mot cau noi song nua ngay roi bo.
     */
    fun loiNhan(context: Context, now: Long = System.currentTimeMillis()): String? {
        val sp = Prefs.get(context).raw()
        val luc = sp.getLong(K_LOI_NHAN_LUC, 0L)
        if (now - luc > 12 * 60 * 60_000L) return null
        return sp.getString(K_LOI_NHAN, null)?.takeIf { it.isNotBlank() }
    }

    fun datLoiNhan(context: Context, loi: String, now: Long = System.currentTimeMillis()) {
        Prefs.get(context).raw().edit()
            .putString(K_LOI_NHAN, loi)
            .putLong(K_LOI_NHAN_LUC, now)
            .commit()
    }

    fun xoaLoiNhan(context: Context) {
        Prefs.get(context).raw().edit().remove(K_LOI_NHAN).remove(K_LOI_NHAN_LUC).commit()
    }

    fun xoaHet(context: Context) {
        KhoBai.get(context).xoaHetTraLoi()
        Prefs.get(context).raw().edit().remove(K_LOI_NHAN).remove(K_LOI_NHAN_LUC).commit()
    }

    /** Bo cac dong qua cu. Goi luc app khoi dong, khong phai moi lan ghi. */
    fun donCu(context: Context, now: Long = System.currentTimeMillis()) {
        KhoBai.get(context).donCu(han(now))
    }

    /**
     * Chuyen so cai cu trong prefs xuong kho, mot lan duy nhat.
     *
     * Khong co buoc nay thi ngay cai ban moi len, nhung cau con dang cho sua bien
     * mat khoi man hinh, va - nang hon - nhung cau da tra gio tuan truoc tro lai
     * thanh bai moi, chup lai la duoc cong gio lan nua. Dung cai lo minh vua di vit.
     */
    fun chuyenSoCu(context: Context) {
        val sp = Prefs.get(context).raw()
        val raw = sp.getString(K_SO_CU, null) ?: return
        runCatching {
            val a = JSONArray(raw)
            val kho = KhoBai.get(context)
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val khoa = o.optString("khoa")
                if (khoa.isEmpty()) continue
                kho.ghiTraLoi(
                    TraLoi(
                        // So cu chi co bai ngoai sach: hoi do chua co sach nao trong may.
                        cauId = if (khoa == KHOA_GOI) khoa else DAU_NGOAI_SACH + khoa,
                        mon = "",
                        ma = o.optString("ma"),
                        de = o.optString("de"),
                        ketQua = "",
                        dung = o.optBoolean("xong"),
                        phut = o.optInt("phut"),
                        nhanXet = o.optString("nhan_xet"),
                        luc = o.optLong("luc")
                    )
                )
            }
        }
        sp.edit().remove(K_SO_CU).commit()
    }

    /**
     * Chuan hoa de bai de doi chieu. Chi con dung cho bai ngoai sach.
     *
     * Bo het dau cach, dau cau va chu hoa: AI chep de moi lan mot khac mot ti
     * ("x^2 - 6x" / "x^2-6x" / "X^2 - 6X"), ma ba cai do la mot bai.
     */
    fun chuanHoa(de: String?): String =
        de.orEmpty().lowercase().filter { it.isLetterOrDigit() || it == '^' }

    /** Lich su cham cua mot cau, moi nhat truoc. De xem lai con da viet gi. */
    fun lichSuCua(context: Context, khoa: String): List<TraLoi> =
        KhoBai.get(context).lichSuCua(khoa)

    private fun TraLoi.sangCauSo() = CauSo(
        khoa = cauId,
        ma = ma,
        de = de,
        phut = phut,
        xong = dung,
        nhanXet = nhanXet,
        luc = luc
    )

    private fun moc0GioCua(now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
