package vn.huytl.homeworkgate.data

import android.content.Context
import java.text.Normalizer
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhamVi

/**
 * Doi ket qua Claude cham, do Ba Huy dan tu dien thoai, thanh mot ban cham cua tablet.
 *
 * Dung khi tablet tat cham AI, xem [Prefs.chamBangAi]. Ban cham nay di vao dung cho ban
 * cham cua AI van di: ApprovalService.xuLyBanCham. Nen moi luat - gia moi cau, tran
 * ngay, moi cau chi tra gio mot lan, danh sach can sua, tron goi vo dan do, tin
 * Telegram - van chi nam o mot noi.
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
 * lenh cua may cham luc co ban soat, xem PromptCham.doanDanDo.
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
        // tam anh gan theo bai, y nhu lan nop chup trang vo kem. Co vo hay khong luc do
        // theo dien thoai bao, vi chi ben do biet bai co mang anh trang vo khong.
        val soat = vo?.takeUnless { it.chuaDoc }
        val coVo = coAnhDanDo(giaTri) || soat != null
        val sach = if (pham?.theoSach == true) {
            KhoBai.get(context).cacCauTheoId(pham.cauIds)
        } else {
            emptyList()
        }
        val daDung = mutableSetOf<String>()

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

            val de = q?.de ?: deClaude
            CauCham(
                // Khop duoc sach thi ghi ma cua sach, de so cai va tin Telegram noi cung mot ma.
                ma = q?.ma?.trim() ?: ma,
                de = de,
                ketQua = (o["conViet"] as? String)?.trim().orEmpty(),
                dung = dung,
                docRo = o["chac"] as? Boolean ?: true,
                // Cau trong sach lay dang tu ngan hang. Cau ngoai sach lay dang Claude xep,
                // vi dang quyet gia: trac nghiem tinh theo cum, hoc thuoc khong tinh.
                dang = dangBai(q?.dang) ?: dangBai(o["dang"] as? String) ?: DangBai.CAU_NHO,
                soDong = (o["soDong"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                nhanXet = if (dung) "" else (o["goiY"] as? String)?.trim().orEmpty(),
                cauId = q?.id,
                mon = q?.mon ?: pham?.mon.orEmpty(),
                coDe = de.isNotBlank(),
                mucDo = (o["mucDo"] as? Number)?.toInt() ?: -1,
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
