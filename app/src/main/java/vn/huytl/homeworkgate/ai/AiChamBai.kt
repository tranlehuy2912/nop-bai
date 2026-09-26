package vn.huytl.homeworkgate.ai

import android.content.Context
import android.util.Log
import org.json.JSONObject
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.KhoaAi
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhamVi
import vn.huytl.homeworkgate.ui.ImageUtil
import java.io.File

/**
 * Cham mot lan nop bai bang AI.
 *
 * Ba viec, theo dung thu tu: chon khoa con dung duoc, goi cham, roi neu co cau sai
 * thi goi them mot lan nua xin loi giai thich cho con.
 *
 * KHONG BAO GIO nem loi ra ngoai. Cham bai la phan co the hong - het han muc, mat
 * mang, Google doi ten model - va khi no hong thi bai cua con van phai den duoc tay
 * Ba Huy nhu cu. Nen moi duong that bai deu tra ve [Ket] co [Ket.loi] la mot cau
 * tieng Viet doc duoc, de nhan thang sang Telegram.
 */
object AiChamBai {

    /**
     * @param ket ket qua cham, null la khong cham duoc.
     * @param loi vi sao khong cham duoc, viet san bang tieng Viet cho Ba Huy doc.
     */
    data class Ket(val ket: KetQuaCham?, val loi: String? = null)

    /**
     * [anh] la toan bo anh cua lan nop (vo dan do, de bai, bai giai).
     * [anhBaiGiai] la rieng phan bai giai, dung cho lan goi giai thich cho re.
     * [pham] la bai con da khai truoc khi chup; null la lan nop tu do kieu cu.
     */
    fun cham(
        context: Context,
        anh: List<File>,
        anhBaiGiai: List<File> = anh,
        pham: PhamVi? = null
    ): Ket {
        if (anh.isEmpty()) return Ket(null, "Không có ảnh nào để chấm.")

        // Thu anh lai truoc khi gui. Xem [thuNho] - truoc ban nay cho nay gui thang
        // file goc cua camera len Gemini.
        val thu = thuNho(anh, anhBaiGiai)
        try {
            return chamVoiAnh(context, thu.cua(anh), thu.cua(anhBaiGiai), pham)
        } finally {
            thu.don()
        }
    }

    /**
     * Ban da thu nho cua mot xap anh, kem duong don dep.
     *
     * KHONG dung chung file goc: [vn.huytl.homeworkgate.telegram.ApprovalService] xoa
     * anh goc sau khi cham xong, ma xoa nham ban goc luc phan gui Telegram chua gui
     * xong la mat bai cua con.
     */
    private class AnhDaThu(val doi: Map<File, File>) {
        fun cua(goc: List<File>): List<File> = goc.map { doi[it] ?: it }
        fun don() = doi.forEach { (goc, moi) -> if (moi != goc) runCatching { moi.delete() } }
    }

    /**
     * Thu anh ve co vua truoc khi gui cho AI.
     *
     * VI SAO PHAI THU. Truoc ban nay [GeminiClient] doc thang file goc cua camera:
     * tablet chup ra anh 4000x3000, moi tam 3-5MB, ma base64 con phinh them mot phan
     * ba. Ba tam la gan hai chuc MB cho mot lan goi - sat tran cua Gemini, va moi
     * nhom con duoc chup toi muoi tam. Qua tran thi Google tra ve loi, ma phia app
     * chi doc duoc thanh "Khong goi duoc AI (mang?)" - tuc la hong ma khong biet
     * hong vi cai gi.
     *
     * BAI GIAI GIU TO HON phan con lai: do la tam duy nhat may phai doc net but viet
     * tay. Vo dan do va trang sach thi chu in, nho hon van doc duoc.
     */
    private fun thuNho(anh: List<File>, anhBaiGiai: List<File>): AnhDaThu {
        val laBaiGiai = anhBaiGiai.toSet()
        val doi = anh.associateWith { f ->
            runCatching {
                if (f in laBaiGiai) ImageUtil.shrinkBaiGiai(f) else ImageUtil.shrinkInPlace(f)
            }.getOrDefault(f)
        }
        return AnhDaThu(doi)
    }

    private fun chamVoiAnh(
        context: Context,
        anh: List<File>,
        anhBaiGiai: List<File>,
        pham: PhamVi?
    ): Ket {
        var khoa = KhoaAi.hienTai(context)
            ?: return Ket(null, "Chưa có khoá AI nào dùng được. ${KhoaAi.moTa(context)}")

        // Danh sach cau con khai. Rong thi cau lenh quay ve ban tu do - may tu tach
        // cau nhu truoc - nen mat mot ban ngan hang cung khong lam ket duong nop bai.
        val danhSach = danhSachCua(context, pham)
        // Vo dan do da chup va soat tu dau buoi. Co no thi lan nop nay khong con
        // trang vo trong xap anh, va doan chu thay vao cho do - xem [VoDanDo]. Ban chi
        // co anh thi khong co chu nao de thay: tam anh da nam trong xap anh roi.
        val danDo = VoDanDo.conHieuLuc(context)?.takeUnless { it.chuaDoc }
        val cauLenh = if (danhSach.isEmpty()) {
            PromptCham.cauLenh(danDo = danDo)
        } else {
            PromptCham.cauLenhTheoDanhSach(
                danhSach,
                tenNguon = pham?.tenNguon.orEmpty(),
                tenBai = pham?.bai.orEmpty(),
                onTap = pham?.onTap == true,
                danDo = danDo
            )
        }

        // Thu lan luot cac khoa. Moi khoa mot lan: khoa nay het han muc thi khoa sau
        // thu ngay, chu khong ngoi doi.
        repeat(KhoaAi.tatCa(context).size) {
            val tra = runCatching { GeminiClient(khoa).hoi(cauLenh, anh) }
                .getOrElse { e ->
                    Log.w(TAG, "goi AI hong: ${e.javaClass.simpleName} ${e.message}")
                    return Ket(null, "Không gọi được AI (mạng?). Ba Huy duyệt tay giúp nhé.")
                }

            if (tra.maLoi == 0) {
                val ket = ChamBaiJson.doc(tra.chu, danhSach)
                    ?: return Ket(null, "AI trả lời không đọc được. Ba Huy duyệt tay giúp nhé.")
                return Ket(themGiaiThich(context, ket, anhBaiGiai, khoa))
            }

            Log.w(TAG, "AI tra loi ${tra.maLoi} voi khoa ${KhoaAi.rutGon(khoa)}")
            val tiep = KhoaAi.nghiTheoLoi(context, khoa, tra.maLoi, tra.than)
                ?: return Ket(null, "AI không chấm được: ${KhoaAi.moTa(context)} " +
                    "Ba Huy duyệt tay giúp nhé.")
            // Cung mot khoa tra ve chinh no nghia la loi khong phai cua khoa (may chu
            // Google ban, ten model sai). Thu lai vong quanh cung the, dung o day.
            if (tiep == khoa) {
                return Ket(null, "AI không chấm được lúc này (Google báo lỗi ${tra.maLoi}). " +
                    "Ba Huy duyệt tay giúp nhé.")
            }
            khoa = tiep
        }
        return Ket(null, "Cả chùm khoá AI đều không dùng được. ${KhoaAi.moTa(context)}")
    }

    /**
     * Con bao may doc nham may cau: nho may nhin lai anh roi cham lai rieng may cau do.
     *
     * Tra ve ban da cap nhat cua dung nhung cau trong [khai], theo dung thu tu. Cau
     * nao may khong tra loi, hay ca lan goi hong, thi giu nguyen ban cu NHUNG ha
     * [CauCham.docRo] xuong false - tuc la khong tu duyet, de Ba Huy nhin. Hong o
     * day khong duoc phep bien thanh "con noi sao nghe vay".
     *
     * @param khai cap (cau goc, cac dong con noi minh da viet).
     */
    fun docLai(
        context: Context,
        anhBaiGiai: List<File>,
        khai: List<Pair<CauCham, List<String>>>
    ): List<CauCham> {
        if (khai.isEmpty()) return emptyList()
        val chiuThua = khai.map { (cau, _) -> cau.copy(docRo = false) }
        if (anhBaiGiai.isEmpty()) return chiuThua

        val khoa = KhoaAi.hienTai(context) ?: return chiuThua
        val thu = thuNho(anhBaiGiai, anhBaiGiai)
        val tra = try {
            runCatching {
                GeminiClient(khoa).hoi(
                    PromptCham.cauLenhDocLai(
                        khai.map { (cau, dong) ->
                            PromptCham.KhaiSua(cau.ma, cau.de, dong)
                        }
                    ),
                    thu.cua(anhBaiGiai)
                )
            }.getOrNull()
        } finally {
            thu.don()
        }
        if (tra == null || tra.maLoi != 0) {
            if (tra != null) KhoaAi.nghiTheoLoi(context, khoa, tra.maLoi, tra.than)
            Log.w(TAG, "doc lai hong, de ba Huy xem")
            return chiuThua
        }

        val theoMa = DocLaiJson.doc(tra.chu)
        return khai.map { (cau, _) ->
            val moi = theoMa[ChamBaiJson.chuanHoaMa(cau.ma)] ?: return@map cau.copy(docRo = false)
            cau.copy(
                baiLam = moi.baiLam,
                dongSai = moi.dongSai,
                ketQua = moi.ketQua,
                dung = moi.dung,
                // May khong dam chac anh dung nhu con noi -> khong tu duyet.
                docRo = moi.docRo && moi.dungNhuConNoi,
                soDong = moi.baiLam.size.takeIf { it > 0 } ?: cau.soDong,
                nhanXet = moi.nhanXet.ifBlank { cau.nhanXet },
                // Doc lai xong thanh cau dung thi xoa nhan loi cu di: cai loi do
                // hoa ra la may doc nham chu con khong sai.
                loaiLoi = if (moi.dung) "" else moi.loaiLoi.ifBlank { cau.loaiLoi }
            )
        }
    }

    /** Cac cau trong ngan hang ma con vua khai. Giu dung thu tu in trong sach. */
    private fun danhSachCua(context: Context, pham: PhamVi?): List<CauHoi> {
        if (pham == null || !pham.theoSach) return emptyList()
        return KhoBai.get(context).cacCauTheoId(pham.cauIds)
    }

    /**
     * Xin loi giai thich cho nhung cau sai, roi gan vao nhan xet.
     *
     * Hong o buoc nay thi khong sao: con van biet cau nao sai, chi la loi nhan xet
     * ngan gon hon. Nen moi loi o day deu nuot, khong lam hong ca lan cham.
     */
    private fun themGiaiThich(
        context: Context,
        ket: KetQuaCham,
        anhBaiGiai: List<File>,
        khoa: String
    ): KetQuaCham {
        val sai = ket.cac.filter { !it.dung }
        if (sai.isEmpty() || anhBaiGiai.isEmpty()) return ket

        val tra = runCatching {
            GeminiClient(khoa).hoi(PromptCham.cauLenhGiaiThich(sai), anhBaiGiai)
        }.getOrNull() ?: return ket
        if (tra.maLoi != 0) {
            KhoaAi.nghiTheoLoi(context, khoa, tra.maLoi, tra.than)
            return ket
        }

        val loi = docGiaiThich(tra.chu) 
        if (loi.isEmpty()) return ket
        return ket.copy(cac = ket.cac.map { cau ->
            loi[cau.ma]?.takeIf { it.isNotBlank() }?.let { cau.copy(nhanXet = it) } ?: cau
        })
    }

    private fun docGiaiThich(chu: String?): Map<String, String> {
        if (chu.isNullOrBlank()) return emptyMap()
        val dau = chu.indexOf('{')
        val cuoi = chu.lastIndexOf('}')
        if (dau < 0 || cuoi <= dau) return emptyMap()
        val o = runCatching { JSONObject(chu.substring(dau, cuoi + 1)) }.getOrNull()
            ?: return emptyMap()
        val mang = o.optJSONArray("giai_thich") ?: return emptyMap()
        return (0 until mang.length()).mapNotNull { i ->
            val g = mang.optJSONObject(i) ?: return@mapNotNull null
            val ma = g.optString("ma").ifBlank { return@mapNotNull null }
            ma to g.optString("loi")
        }.toMap()
    }

    private const val TAG = "HomeworkGate"
}
