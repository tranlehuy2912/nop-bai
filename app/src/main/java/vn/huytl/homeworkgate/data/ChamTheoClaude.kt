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
 * ngay, moi cau chi tra gio mot lan, danh sach can sua, tin Telegram - nam o mot cho.
 *
 * CAU TRONG SACH lay de, ma sach va dang bai tu ngan hang, theo pham vi con da khai.
 * Claude chi can noi dung hay sai, con viet gi, va bao nhieu dong. CAU NGOAI SACH thi
 * lay de Claude chep tu anh. Khong co de thi coi nhu khong co de, va luat cong gio tra
 * 0 phut cho cau do, y nhu duong AI.
 *
 * KHONG CON PHAN VO DAN DO (30/9/2026). Truoc do ban cham mang ngay trong vo, cac bai co
 * giao va con da lam het chua, de tinh tron goi 45 phut. Ba Huy bo tron goi, nen ban cham
 * chi con tung cau. Bang dieu khien ban cu con gui cac truong do thi o day bo qua.
 *
 * Muc nao khong co ket luan dung hay sai that thi bo di: mot chu "dung" viet thieu hay
 * viet sai kieu khong duoc phep thanh mot lan cong gio.
 */
object ChamTheoClaude {

    /**
     * @param giaTri "giaTri" cua lenh CHAMBAI: mot map { cac, ... }. Van nhan kieu cu chi
     *   co danh sach cau.
     */
    fun banCham(
        context: Context,
        giaTri: Any?,
        pham: PhamVi?
    ): KetQuaCham? {
        val goi = giaTri as? Map<*, *>
        val cacMuc = ((goi?.get("cac") ?: giaTri) as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
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
            /*
             * Ba Huy xem anh roi bam "Chụp lại" cho cau nay (lenh XUCAU, 29/9/2026): anh mo toi
             * ca Ba Huy cung khong doc ra. Coi nhu may khong nhin thay de - khong cham, khong
             * ghi so, khong ghi kieu sai - de con chup lai cau do cho ro.
             */
            val chupLai = o["chupLai"] as? Boolean == true
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
                coDe = de.isNotBlank() && !chupLai,
                maGoc = ma
            )
        }
        if (cac.isEmpty()) return null
        return KetQuaCham(
            mon = pham?.mon?.takeIf { it.isNotBlank() } ?: cac.first().mon,
            cac = cac
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

    private fun dangBai(ten: String?): DangBai? =
        ten?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { DangBai.valueOf(it) }.getOrNull() }
}
