package vn.huytl.homeworkgate.ai

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import vn.huytl.homeworkgate.data.KhoaAi
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.ui.ImageUtil
import java.io.File
import java.time.LocalDate

/**
 * Doc mot trang vo dan do ra chu. Khong cham gi ca.
 *
 * Chep cach xoay khoa cua [AiChamBai.chamVoiAnh]: thu lan luot cac khoa, khoa nao
 * het han muc thi sang khoa sau ngay chu khong ngoi doi. Hai cho giong nhau den
 * muc nay thi de gop lai, nhung gop vao la [AiChamBai] phai nhan them mot che do
 * nua trong khi no da la doan kho nhat cua app.
 *
 * BAN DOC RA KHONG DI THANG VAO VIEC TINH GIO. No ra man hinh cho Le Hoa soat lai
 * da - xem [vn.huytl.homeworkgate.ui.DanDoActivity]. May doc nham mot ngay hay bo
 * sot mot bai deu la chuyen thuong, va o day thi sua duoc bang mat, khong phai doi
 * den luc mat phut moi biet.
 */
object DocDanDo {

    /**
     * [cacNgay] rong la doc khong duoc hoac trang khong co dan do nao; luc do [loi]
     * noi vi sao, hoac rong neu trang that su khong co gi.
     */
    data class Ket(val cacNgay: List<VoDanDo.DanDo>, val loi: String = "")

    /** Chan luong goi - phai goi tu Dispatchers.IO. */
    fun doc(context: Context, anh: List<File>): Ket {
        if (anh.isEmpty()) return Ket(emptyList(), "Chưa có ảnh nào.")

        // Thu anh ve co thuong truoc khi gui. Man dan do dua thang file goc cua
        // camera vao day - 4000x3000, 3-5MB, base64 con phinh them mot phan ba.
        // VIEC NAY KHONG BOT TOKEN: Gemini 3 tinh moi anh mot so token co dinh theo
        // muc media_resolution, anh to hay nho deu vay. Cai bot duoc la dung luong
        // gui qua wifi nha. Co thuong 1600px la du vi ban doc ra con qua man soat
        // cua Le Hoa - xem [ImageUtil]. Thu hong thi gui tam goc, con hon khong doc.
        val nho = anh.map { f -> runCatching { ImageUtil.shrinkInPlace(f) }.getOrDefault(f) }
        try {
            return docVoiAnh(context, nho)
        } finally {
            // shrinkInPlace tra ve chinh file goc khi khong giai ma duoc anh. Chi
            // xoa ban da thu: ban goc con phai song de luu va gui kem cho ba Huy.
            nho.zip(anh).forEach { (moi, goc) -> if (moi != goc) runCatching { moi.delete() } }
        }
    }

    private fun docVoiAnh(context: Context, anh: List<File>): Ket {
        var khoa = KhoaAi.hienTai(context)
            ?: return Ket(
                emptyList(),
                "Máy chưa đọc được. Nhờ ba Huy xem giúp: ${KhoaAi.moTa(context)}"
            )

        val cauLenh = PromptCham.cauLenhDocDanDo()

        repeat(KhoaAi.tatCa(context).size) {
            val tra = runCatching { GeminiClient(khoa).hoi(cauLenh, anh) }
                .getOrElse { e ->
                    Log.w(TAG, "goi AI hong: ${e.javaClass.simpleName} ${e.message}")
                    return Ket(emptyList(), "Máy không đọc được, chắc mất mạng. Thử lại nhé.")
                }

            if (tra.maLoi == 0) {
                val cac = docJson(tra.chu)
                    ?: return Ket(emptyList(), "Máy trả lời không đọc được. Chụp lại rõ hơn nhé.")
                if (cac.isEmpty()) {
                    return Ket(emptyList(), "Không thấy dặn dò nào trong ảnh. Chụp lại rõ hơn nhé.")
                }
                return Ket(cac)
            }

            Log.w(TAG, "AI tra loi ${tra.maLoi} voi khoa ${KhoaAi.rutGon(khoa)}")
            val tiep = KhoaAi.nghiTheoLoi(context, khoa, tra.maLoi, tra.than)
                ?: return Ket(
                    emptyList(),
                    "Máy không đọc được. Nhờ ba Huy xem giúp: ${KhoaAi.moTa(context)}"
                )
            if (tiep == khoa) {
                return Ket(emptyList(), "Máy không đọc được lúc này (Google báo lỗi ${tra.maLoi}).")
            }
            khoa = tiep
        }
        return Ket(
            emptyList(),
            "Máy không đọc được lúc này. Nhờ ba Huy xem giúp: ${KhoaAi.moTa(context)}"
        )
    }

    /**
     * Doc JSON may tra ve ra tung khoi ngay.
     *
     * KHOI THIEU NGAY VAN GIU LAI, dien tam ngay hom nay. Con dang dung truoc man
     * soat, sua mot o ngay de hon nhieu so voi chup lai ca trang vo - va vut ca khoi
     * di thi con mat het nhung dong may doc dung.
     */
    private fun docJson(chu: String?): List<VoDanDo.DanDo>? {
        val o = runCatching { JSONObject(chu.orEmpty()) }.getOrNull() ?: return null
        val mang = o.optJSONArray("cac_ngay") ?: return emptyList()
        return (0 until mang.length()).mapNotNull { i ->
            val k = mang.optJSONObject(i) ?: return@mapNotNull null
            val dong = k.optJSONArray("cac_dong") ?: JSONArray()
            val cac = (0 until dong.length()).mapNotNull { j ->
                val x = dong.optJSONObject(j) ?: return@mapNotNull null
                val t = x.optString("chu").trim()
                if (t.isEmpty()) {
                    null
                } else {
                    // Ghi ban doan cua may vao ca hai cho: mot cai de tich san tren
                    // man hinh, mot cai giu nguyen de sau nay biet con sua cho nao.
                    val doan = x.optBoolean("la_bai_tap")
                    VoDanDo.Dong(chu = t, laBaiTap = doan, mayTich = doan)
                }
            }
            if (cac.isEmpty()) return@mapNotNull null
            VoDanDo.DanDo(
                ngay = k.optString("ngay").takeIf { hopLe(it) } ?: LocalDate.now().toString(),
                cacDong = cac
            )
        }
    }

    private fun hopLe(ngay: String): Boolean =
        runCatching { LocalDate.parse(ngay) }.isSuccess

    private const val TAG = "DocDanDo"
}
