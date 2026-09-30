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

    /**
     * Khoa gia cua ban ghi tron goi vo dan do. Bo tron goi tu 30/9/2026 nen khong ghi dong
     * moi nao nua; dong cu van nam trong so, van tinh vao phut anh cua ngay do.
     */
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

    /**
     * Bai lam lan nay co giong HET tung dong voi lan lam dung truoc day khong.
     *
     * Dung o lan on tap, va chi o do. On tap la duong duy nhat duoc cham lai mot cau
     * da lam dung, nen no thao mat cai khoa "moi cau chi tra gio mot lan" - con mo
     * vo ra dung trang cu, chup lai bai tuan truoc, thi tren giay khong co dau thoi
     * gian nao de phan biet.
     *
     * Vi sao so tung dong lai an: hai lan AI doc hai tam anh khac nhau gan nhu khong
     * bao gio ra chuoi giong het, ke ca khi con lam lai dung cach giai cu - chu viet
     * khac di mot ti, may ngat dong khac di, doc nham mot ky tu. Trung khop tuyet
     * doi moi dong thi kha nang cao la cung mot tam anh cua cung mot trang giay.
     *
     * KHONG DUNG DE TU CHOI. Ket qua cua no chi la thoi tu duyet, day sang Ba Huy mo
     * anh ra nhin. Con lam lai that ma khong may trung khop thi mat mot lan cho, chu
     * khong mat gio.
     */
    fun giongHetLanTruoc(context: Context, cau: CauCham): Boolean {
        if (cau.baiLam.isEmpty()) return false
        val k = khoaCua(cau)
        if (k.isEmpty()) return false
        val truoc = KhoBai.get(context).lichSuCua(k)
            .firstOrNull { it.dung && it.baiLam.isNotEmpty() } ?: return false
        return truoc.baiLam == cau.baiLam
    }

    /**
     * Cau nay truoc day da sai may lan. 0 la chua sai lan nao.
     *
     * [truocLuc] chi dem lan sai ghi truoc moc do, vi du truoc lan nop dang cham lai.
     */
    fun soLanSai(
        context: Context,
        cau: CauCham,
        now: Long = System.currentTimeMillis(),
        truocLuc: Long = Long.MAX_VALUE
    ): Int {
        val k = khoaCua(cau)
        if (k.isEmpty()) return 0
        return KhoBai.get(context).soLanSai(k, han(now), truocLuc)
    }

    /**
     * Cac cau dang sai, cho con sua lai bang anh.
     *
     * Bo cau sai vao mot ngay da tinh tron goi (Ba Huy chot 29/9/2026): sua xong cung khong
     * ra phut, vi moi bai chup la bai dan do va da nam trong goi. Cau do neu co trong ngan
     * hang thi 24 gio sau hien o duong lam tren may - xem [LamTrenMay.cauLamThem]; cau ngoai
     * ngan hang thi thoi. Tu 30/9/2026 khong con tron goi, nen chi con nhung ngay cu co goi
     * bi bo; cau sai cua ngay moi luon cho sua.
     */
    fun dangChoSua(context: Context, now: Long = System.currentTimeMillis()): List<CauSo> {
        val kho = KhoBai.get(context)
        val tu = han(now)
        val ngayGoi = kho.lucCacGoi(tu).map { moc0GioCua(it) }.toSet()
        return kho.dangChoSua(tu).filter { moc0GioCua(it.luc) !in ngayGoi }.map { it.sangCauSo() }
    }

    /**
     * Lan sai con giu tren danh sach cua con bao nhieu ngay (Ba Huy chot 30/9/2026).
     *
     * So cai van nho mot nam ([GIU_NGAY]) de chan nop lai lay gio lan hai. Nhung dong
     * "Có N câu cần sửa" dung chung han do thi cu dai ra: ngay 30/9/2026 no ghi 19 cau, co cau
     * cua trang vo con khong con giu. Mot tuan la du cho tiet sau cua moi mon.
     */
    const val HIEN_SUA_NGAY = 7

    /**
     * Cac cau con phai tu chup lai de sua: danh sach tren man chinh, man Ket qua, va cho
     * khoa cau o man chon bai. Bot tu [dangChoSua] ba loai (30/9/2026):
     *  1. lan sai cu hon [HIEN_SUA_NGAY] ngay;
     *  2. cau lam duoc tren may, da qua 24 gio: tu luc do no nam o duong Lam bai tren may
     *     ([LamTrenMay.cauLamThem]), hien them o day la dem mot cau hai lan;
     *  3. cau Ba Huy da bo bang lenh BOSUA ([BoSua]).
     *
     * [SuaCham] van dung [dangChoSua]: Ba Huy sua cham mot bai tuan truoc thi van phai duoc,
     * cau do khong con tren danh sach cua con khong co nghia la may cham dung.
     */
    fun canSua(context: Context, now: Long = System.currentTimeMillis()): List<CauSo> {
        val tuan = now - HIEN_SUA_NGAY * 24 * 60 * 60_000L
        val con = dangChoSua(context, now).filter { it.luc >= tuan && !BoSua.daBo(context, it.khoa, it.luc) }
        if (con.isEmpty()) return con
        val trenMay = LamTrenMay.lamDuocTrenMay(context, con.filter { now >= it.luc + MOT_NGAY }.map { it.khoa })
        return con.filterNot { it.khoa in trenMay }
    }

    private const val MOT_NGAY = 24L * 60 * 60_000L

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
        now: Long = System.currentTimeMillis(),
        onTap: Boolean = false,
        /** Cau con tu bao la chua chac. null nghia la lan nay con khong duoc hoi. */
        chuaChac: Set<String>? = null,
        /** Cau con tu viet ra minh sai cho nao. Mot dong chung cho ca lan nop. */
        conNoiChung: String = "",
        /** Lan nop nay la phan tu luan cua de Giai de nao. Xem [TraLoi.deId]. */
        deId: String = ""
    ): List<TraLoi> {
        if (cac.isEmpty()) return emptyList()
        val kho = KhoBai.get(context)
        val tuLuc = han(now)
        val daGhi = mutableListOf<TraLoi>()

        cac.forEach { c ->
            val k = khoaCua(c)
            if (k.isEmpty()) return@forEach
            // Lan on tap: cau von DA xong - do moi la dieu kien de duoc on. Cho nay
            // chan lan on CHUA DEN HEN, chu khong chan mai mai nhu ban dau.
            val boQua = if (onTap) !kho.denHenOn(k, tuLuc, now) else kho.daXong(k, tuLuc)
            if (boQua) return@forEach
            val dong = TraLoi(
                cauId = k,
                mon = c.mon,
                ma = c.ma,
                de = c.de,
                ketQua = c.ketQua,
                baiLam = c.baiLam,
                dongSai = c.dongSai,
                dung = c.dung,
                phut = if (c.dung) phutCua[c.ma] ?: 0 else 0,
                // Cau da xong thi khong giu loi nhan xet: no chi de con biet duong ma
                // sua, xong roi la het viec. Giu lai chi phinh so va de lai loi che
                // bai cua mot dua tre trong may ca nam.
                nhanXet = if (c.dung) "" else c.nhanXet,
                loaiLoi = c.loaiLoi,
                onTap = onTap,
                khaiChac = when {
                    chuaChac == null -> -1
                    k in chuaChac -> 0
                    else -> 1
                },
                // Chi gan cho cau dang duoc SUA. Cau lam moi trong cung lan nop thi
                // dong chu do khong noi ve no.
                conNoi = if (conNoiChung.isNotBlank() && kho.soLanSai(k, tuLuc) > 0) {
                    conNoiChung
                } else {
                    ""
                },
                luc = now,
                deId = deId
            )
            kho.ghiTraLoi(dong)
            daGhi += dong
        }
        // Tra ve dung nhung dong DA ghi, khong phai danh sach dua vao: ben goi con
        // day chung len Firestore, ma day nham mot dong bi bo qua la so tren may chu
        // noi mot dang, so trong may noi mot dang.
        return daGhi
    }

    /**
     * So phut duong CHUP ANH da cong trong ngay, de giu [LuatCongGio.TRAN_ANH]. Tu
     * 29/9/2026 duong nay chi con bai co giao.
     */
    fun phutAnhHomNay(context: Context, now: Long = System.currentTimeMillis()): Int =
        KhoBai.get(context).tongPhutAnh(moc0GioCua(now))

    /**
     * Bai chup anh hom nay da du [LuatCongGio.TRAN_ANH] chua. Du roi thi nop them anh khong
     * duoc phut nao, nen man chinh doi nut Nop bai sang lam bai tren may.
     *
     * Thay cho "hom nay da tinh tron goi" (bo 30/9/2026): tron goi luc do cung chinh la du
     * tran anh, nen man hinh doi dung cho cu, chi khac moc.
     */
    fun hetTranAnhHomNay(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        phutAnhHomNay(context, now) >= LuatCongGio.TRAN_ANH

    /**
     * So phut bai lam TREN MAY (lam them, luyen, Giai de) da cap trong ngay, de giu
     * [LuatCongGio.TRAN_TREN_MAY]. Phan vao quy khong tinh o day.
     */
    fun phutTrenMayHomNay(context: Context, now: Long = System.currentTimeMillis()): Int =
        KhoBai.get(context).tongPhutTrenMay(moc0GioCua(now), onTap = false)

    /**
     * So phut duong ON LAI tren may da cap trong ngay, de giu [LuatCongGio.TRAN_ON_MOI_NGAY].
     *
     * Tu 29/9/2026 on lai la mot phan rieng voi tran rieng, khong nam trong
     * [phutTrenMayHomNay]. Truoc do on lai la bai chup anh va nam chung ro lam them.
     */
    fun phutOnHomNay(context: Context, now: Long = System.currentTimeMillis()): Int =
        KhoBai.get(context).tongPhutTrenMay(moc0GioCua(now), onTap = true)

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

    /**
     * Cac cau den hen on lai: tung sai, da sua dung, va da den luc nho lai.
     *
     * Chi cau trong sach. Man on khong ve duoc bai ngoai sach, dem ca no vao thi dong
     * "Ôn lại N câu" dem nhieu hon so cau con thay. Xem [KhoBai.cacCauDenHenOn].
     */
    fun cacCauDangOn(context: Context, now: Long = System.currentTimeMillis()): List<String> =
        KhoBai.get(context).cacCauDenHenOn(han(now), now)

    /** Cau nay da duoc tra gio cho mot lan on tap chua. */
    fun daOnTap(context: Context, khoa: String, now: Long = System.currentTimeMillis()): Boolean =
        KhoBai.get(context).daOnTap(khoa, han(now))

    /**
     * Cau nay da den hen on lai chua. Chua den hen thi lan on do khong ghi so, khong
     * co phut - xem [ghi].
     *
     * Cung luat voi [cacCauDangOn]: cau nao khong hien tren man on thi o day cung
     * khong bao gio den hen, ke ca bai ngoai sach da sua dung.
     */
    fun denHenOn(context: Context, cau: CauCham, now: Long = System.currentTimeMillis()): Boolean {
        val k = khoaCua(cau)
        return k.isNotEmpty() && KhoBai.get(context).denHenOn(k, han(now), now)
    }

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
