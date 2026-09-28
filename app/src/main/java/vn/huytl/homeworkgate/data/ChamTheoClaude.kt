package vn.huytl.homeworkgate.data

import android.content.Context
import java.text.Normalizer
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhamVi

/**
 * Doi ket qua Claude cham, do Ba Huy dan tu dien thoai, thanh mot ban cham cua tablet.
 *
 * Tu 28/9/2026 day la duong cham DUY NHAT: Ba Huy bo han phan may cham tren tablet.
 * Ban cham nay di vao ApprovalService.xuLyBanCham, noi moi luat - gia moi cau, tran
 * ngay, moi cau chi tra gio mot lan, danh sach can sua, tron goi vo dan do, tin
 * Telegram - nam o mot cho.
 *
 * CAU TRONG SACH lay de, ma sach va dang bai tu ngan hang, theo pham vi con da khai.
 * Claude chi can noi dung hay sai, con viet gi, va bao nhieu dong. CAU NGOAI SACH thi
 * lay de Claude chep tu anh. Khong co de thi coi nhu khong co de, va luat cong gio tra
 * 0 phut cho cau do, y nhu duong AI.
 *
 * VO DAN DO: Claude doc trang vo chup kem lan nop, tra ve ngay trong vo, cac bai co
 * giao, va con da lam het chua. Ba thu do vao dung ba truong ma may cham van dien, nen
 * [LuatCongGio] tu quyet tron goi nhu moi lan.
 *
 * Lan nop dung ban vo con soat tu dau buoi ([VoChoCham]) thi ngay va danh sach bai lay
 * tu ban do, Claude chi noi con lam het chua va cau nao thuoc bai co giao. Y het cau
 * lenh cua may cham tren tablet luc co ban soat (phan may cham bo ngay 28/9/2026).
 *
 * Muc nao khong co ket luan dung hay sai that thi bo di: mot chu "dung" viet thieu hay
 * viet sai kieu khong duoc phep thanh mot lan cong gio.
 */
object ChamTheoClaude {

    /**
     * @param giaTri "giaTri" cua lenh CHAMBAI: mot map { cac, ngayDanDo, baiDuocGiao,
     *   lamHetDanDo, coAnhDanDo }. Van nhan kieu cu chi co danh sach cau.
     * @param vo ban vo ma lan nop do dung, xem [VoChoCham.voChoBai]. null la lan nop khong
     *   dung ban nao.
     */
    fun banCham(
        context: Context,
        giaTri: Any?,
        pham: PhamVi?,
        vo: VoDanDo.DanDo? = null
    ): KetQuaCham? {
        val goi = giaTri as? Map<*, *>
        val cacMuc = ((goi?.get("cac") ?: giaTri) as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
        // Co ban soat la co vo dan do, du dien thoai co bao hay khong: Bang dieu khien ban
        // cu khong biet ban soat, ma quy tac 17 duoi day chi danh cho lan nop khong co vo.
        //
        // Ban chi co anh thi chua co danh sach nao de dung: ngay va bai lay tu Claude doc
        // tam anh gan theo bai (khau DAN_DO, xem KHAU_DAN_DO). Co vo hay khong luc do
        // theo dien thoai bao, vi chi ben do biet bai co mang anh trang vo khong.
        val soat = vo?.takeUnless { it.chuaDoc }
        val coVo = coAnhDanDo(giaTri) || soat != null
        val sach = if (pham?.theoSach == true) {
            KhoBai.get(context).cacCauTheoId(pham.cauIds)
        } else {
            emptyList()
        }
        val daDung = mutableSetOf<String>()
        val ngoai = pham?.cauNgoai.orEmpty()
        val daDungNgoai = mutableSetOf<String>()

        val cac = cacMuc.mapNotNull { o ->
            val ma = (o["ma"] as? String)?.trim().orEmpty()
            val dung = o["dung"] as? Boolean
            if (ma.isEmpty() || dung == null) return@mapNotNull null

            val deClaude = (o["de"] as? String)?.trim().orEmpty()
            val cungMa = sach.filter { chuanMa(it.ma) == chuanMa(ma) && it.id !in daDung }
            val q = if (cungMa.size <= 1) {
                cungMa.firstOrNull()
            } else {
                cungMa.firstOrNull { SoCaiBai.chuanHoa(it.de) == SoCaiBai.chuanHoa(deClaude) }
                    ?: cungMa.first()
            }
            q?.let { daDung += it.id }
            // Cau ngoai sach cua lan nop lai, xem [PhamVi.cauNgoai]. Khop theo ma.
            val cn = if (q != null) null else ngoai.firstOrNull {
                chuanMa(it.ma) == chuanMa(ma) && it.ma !in daDungNgoai
            }
            cn?.let { daDungNgoai += it.ma }

            /*
             * Lan NOP LAI cac cau sai cua mot bai: chi cham cau trong danh sach khai.
             *
             * Con sua de len chinh trang vo cu, nen anh co ca nhung cau da dung tu lan truoc.
             * Loi nho da dan Claude bo qua chung, nhung dat chot o day: cau ngoai danh sach
             * thi coi la bai ngoai sach, khoa so cai theo de bai, so cai khong nhan ra la cau
             * da tra gio, va con duoc tra gio lan hai. Ba Huy chon ngay 28/9/2026.
             */
            if (pham?.laSua == true && q == null && cn == null) return@mapNotNull null

            // Cau ngoai sach cua lan nop lai lay de chep tu ban cham cu, khong lay de Claude
            // chep lan nay: so cai khoa cau ngoai sach theo de, xem [PhamVi.cauNgoai].
            val de = q?.de ?: cn?.de?.takeIf { it.isNotBlank() } ?: deClaude
            CauCham(
                // Khop duoc sach thi ghi ma cua sach, de so cai va tin Telegram noi cung mot ma.
                ma = q?.ma?.trim() ?: cn?.ma ?: ma,
                de = de,
                ketQua = (o["conViet"] as? String)?.trim().orEmpty(),
                dung = dung,
                docRo = o["chac"] as? Boolean ?: true,
                // Cau trong sach lay dang tu ngan hang. Cau ngoai sach lay dang Claude xep,
                // vi dang quyet gia: trac nghiem tinh theo cum, hoc thuoc khong tinh.
                dang = dangBai(q?.dang) ?: dangBai(o["dang"] as? String) ?: DangBai.CAU_NHO,
                soDong = (o["soDong"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                nhanXet = if (dung) "" else (o["goiY"] as? String)?.trim().orEmpty(),
                // Kieu sai Claude xep (tu 28/9/2026), ep ve bay nhan cua LoaiLoi. Cau dung
                // thi khong co nhan nao, du Claude co ghi gi.
                loaiLoi = LoaiLoi.doc(o["loaiLoi"] as? String, dung),
                cauId = q?.id,
                mon = q?.mon ?: pham?.mon.orEmpty(),
                coDe = de.isNotBlank(),
                /*
                 * Quy tac 17 cua may cham: lan nop KHONG co trang vo dan do thi moi cau
                 * la bai co giao. Day la lan nop de sua bai, chi co anh bai giai. Coi la
                 * bai lam them thi xap bai da nam trong tron goi hom nay lai duoc tinh
                 * le them lan nua - ngay 14/9/2026 la 45 phut goi cong 40 phut nua.
                 */
                trongDanDo = when {
                    !coVo -> true
                    // Ban soat ghi hom do co khong giao bai tap nao: khong cau nao thuoc
                    // bai co giao, du Claude noi gi. Cau lenh cua may cham dan y nhu vay.
                    soat != null && soat.cacBai.isEmpty() -> false
                    else -> o["trongDanDo"] as? Boolean ?: false
                }
            )
        }
        if (cac.isEmpty()) return null
        return KetQuaCham(
            mon = pham?.mon?.takeIf { it.isNotBlank() } ?: cac.first().mon,
            cac = cac,
            ngayDanDo = soat?.ngay
                ?: (goi?.get("ngayDanDo") as? String)?.trim()?.takeIf { it.isNotEmpty() },
            // Viet sai kieu la khong co goi, y nhu "dung": mot chu "true" khong duoc
            // thanh 45 phut.
            lamHetDanDo = goi?.get("lamHetDanDo") as? Boolean ?: false,
            baiDuocGiao = soat?.cacBai ?: (goi?.get("baiDuocGiao") as? List<*>).orEmpty()
                .mapNotNull { (it as? String)?.trim()?.takeIf { t -> t.isNotEmpty() } }
        )
    }

    /**
     * Ma cau da bo het nhung cach viet khac nhau cua cung mot cau, de so voi ngan hang.
     *
     * Claude co khi chep "2.33A", "2.33 a", "Câu 2.33a" hay "2.33a)" thay cho "2.33a".
     * So y het thi cau do khong khop cau nao trong sach, mat de, va tra 0 phut du con lam
     * dung. Chi bo nhung thu khong bao gio phan biet hai cau: hoa thuong, khoang trang,
     * chu "câu" hay "bài" o dau, dau cham, ngoac dong va hai cham o cuoi.
     *
     * App Bang dieu khien co mot ban y het, NhoClaude.chuanMa, de doi ve ma trong khai
     * truoc khi gui. Sua ben nay thi sua ca ben do.
     */
    internal fun chuanMa(ma: String): String {
        val t = Normalizer.normalize(ma, Normalizer.Form.NFC).lowercase().filterNot { it.isWhitespace() }
        val dau = listOf("câu", "cau", "bài", "bai").firstOrNull { t.startsWith(it) }
        return (if (dau == null) t else t.removePrefix(dau)).trimEnd('.', ')', ']', ':')
    }

    /** Lan nop nay co trang vo dan do khong, theo dien thoai bao. Kieu cu thi khong. */
    fun coAnhDanDo(giaTri: Any?): Boolean =
        (giaTri as? Map<*, *>)?.get("coAnhDanDo") as? Boolean ?: false

    private fun dangBai(ten: String?): DangBai? =
        ten?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { DangBai.valueOf(it) }.getOrNull() }
}
