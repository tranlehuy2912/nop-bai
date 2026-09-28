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
 * TU 28/9/2026 DAY LA VIEC DUY NHAT CON GOI GEMINI. Ba Huy bo han phan may cham bai
 * tren tablet (AiChamBai, man soat ban may doc): bai nao cung do Claude cham tren Bang
 * dieu khien. Buoc soat vo dau buoi thi giu, Ba Huy chon tu 26/9/2026: khong co no thi
 * lan nop nao cung phai chup lai trang vo, va Claude phai tu doan buoi nao la hom nay
 * tren mot trang chep nhieu buoi.
 *
 * Xoay khoa: thu lan luot cac khoa, khoa nao het han muc thi sang khoa sau ngay chu
 * khong ngoi doi. Xem [KhoaAi].
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

        val lenh = cauLenh()

        repeat(KhoaAi.tatCa(context).size) {
            val tra = runCatching { GeminiClient(khoa).hoi(lenh, anh) }
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

    /**
     * Doc mot trang vo dan do ra chu. Khong cham gi ca, chi doc.
     *
     * Chuyen tu PromptCham sang day ngay 28/9/2026, luc bo phan may cham bai: day la
     * cau lenh duy nhat con gui cho Gemini. Ban doc ra chu con phai qua mat Le Hoa
     * soat lai, xem [vn.huytl.homeworkgate.ui.DanDoActivity].
     *
     * HOI IT THOI. Chi can ngay va danh sach bai tap: do la hai thu duy nhat
     * [vn.huytl.homeworkgate.data.LuatCongGio] dung toi. Phan dan do khac hoi them
     * cho con doc, khong di vao cho nao tinh gio.
     */
    private fun cauLenh(homNay: java.time.LocalDate = java.time.LocalDate.now()): String =
        "Hôm nay là ngày ${homNay.dayOfMonth} tháng ${homNay.monthValue} " +
            "năm ${homNay.year}.\n" + CAU_LENH

    private val CAU_LENH = """
Ảnh là một trang trong vở dặn dò của học sinh lớp 8, do chính học sinh chép lại lời cô giáo dặn cuối mỗi buổi học.

MỘT TRANG THƯỜNG CHỨA NHIỀU NGÀY. Mỗi ngày là một khối: một dòng ghi ngày, thường kèm chữ "Dặn dò", rồi vài dòng dặn dò bên dưới, mỗi dòng bắt đầu bằng tên môn. Trả về TẤT CẢ các khối thấy trên trang, đúng thứ tự từ trên xuống. TUYỆT ĐỐI không trộn dòng của ngày này sang ngày khác, và không bỏ sót ngày nào.

Chỉ trả về JSON, không thêm chữ nào khác:
{"cac_ngay":[{"ngay":"yyyy-MM-dd hoặc null","cac_dong":[{"chu":"Toán: làm bài 2 trang 36","la_bai_tap":true},{"chu":"Tiếng Anh: tiết sau kiểm tra từ vựng","la_bai_tap":false}]}]}

Quy tắc bắt buộc:
1. "ngay": ngày ghi ở đầu khối, đổi ra yyyy-MM-dd. Vở ghi kiểu nào cũng phải đọc được: "15/9/2026", "Thứ hai, ngày 14 tháng 9", hay tiếng Anh "Monday, september 14th, 2026". Thiếu năm thì lấy năm sao cho ngày đó gần hôm nay nhất. Không thấy ngày thì để null.
2. "chu": chép NGUYÊN VĂN cả dòng, giữ tên môn ở đầu đúng như vở viết tắt: "KHTN", "NV", "GDCD", "CN", "LS-ĐL", "TTNT", "ÂNhạc", "STEAM". Không viết lại cho hay hơn, không mở rộng chữ viết tắt, không gộp hai dòng làm một, không tách một dòng làm hai.
3. "la_bai_tap": true CHỈ KHI dòng đó bảo LÀM một bài rồi nộp lại được — "làm bài 2 trang 36", "vẽ ký họa trên giấy A4", "làm luyện tập 3 trang 59". Những thứ sau luôn là false: ôn bài, học thuộc, xem trước bài, tiết sau kiểm tra, mang sách vở, mang đồ, làm đúng nội quy, sinh hoạt ngoài trời.
4. Không đoán. Chữ nào nhìn không ra thì chép phần đọc được và bỏ phần không đọc được, đừng suy ra nội dung. Con số thì đặc biệt cẩn thận: 4 với 9, 5 với 6 rất dễ nhầm — không chắc thì cứ chép cái mình thấy, người sẽ soát lại.
5. Trang không có dặn dò nào thì "cac_ngay" là mảng rỗng.
    """.trimIndent()

    private const val TAG = "DocDanDo"
}
