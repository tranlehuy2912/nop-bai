package vn.huytl.homeworkgate.data

import android.content.Context
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
 * Muc nao khong co ket luan dung hay sai that thi bo di: mot chu "dung" viet thieu hay
 * viet sai kieu khong duoc phep thanh mot lan cong gio.
 */
object ChamTheoClaude {

    /**
     * @param giaTri "giaTri" cua lenh CHAMBAI: mot map { cac, ngayDanDo, baiDuocGiao,
     *   lamHetDanDo, coAnhDanDo }. Van nhan kieu cu chi co danh sach cau.
     */
    fun banCham(context: Context, giaTri: Any?, pham: PhamVi?): KetQuaCham? {
        val goi = giaTri as? Map<*, *>
        val cacMuc = ((goi?.get("cac") ?: giaTri) as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
        val coVo = coAnhDanDo(giaTri)
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
            val cungMa = sach.filter { it.ma.trim() == ma && it.id !in daDung }
            val q = if (cungMa.size <= 1) {
                cungMa.firstOrNull()
            } else {
                cungMa.firstOrNull { SoCaiBai.chuanHoa(it.de) == SoCaiBai.chuanHoa(deClaude) }
                    ?: cungMa.first()
            }
            q?.let { daDung += it.id }

            val de = q?.de ?: deClaude
            CauCham(
                ma = ma,
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
                trongDanDo = if (coVo) o["trongDanDo"] as? Boolean ?: false else true
            )
        }
        if (cac.isEmpty()) return null
        return KetQuaCham(
            mon = pham?.mon?.takeIf { it.isNotBlank() } ?: cac.first().mon,
            cac = cac,
            ngayDanDo = (goi?.get("ngayDanDo") as? String)?.trim()?.takeIf { it.isNotEmpty() },
            // Viet sai kieu la khong co goi, y nhu "dung": mot chu "true" khong duoc
            // thanh 45 phut.
            lamHetDanDo = goi?.get("lamHetDanDo") as? Boolean ?: false,
            baiDuocGiao = (goi?.get("baiDuocGiao") as? List<*>).orEmpty()
                .mapNotNull { (it as? String)?.trim()?.takeIf { t -> t.isNotEmpty() } }
        )
    }

    /** Lan nop nay co trang vo dan do khong, theo dien thoai bao. Kieu cu thi khong. */
    fun coAnhDanDo(giaTri: Any?): Boolean =
        (giaTri as? Map<*, *>)?.get("coAnhDanDo") as? Boolean ?: false

    private fun dangBai(ten: String?): DangBai? =
        ten?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { DangBai.valueOf(it) }.getOrNull() }
}
