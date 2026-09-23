package vn.huytl.homeworkgate.ai

/**
 * Cau lenh gui kem anh bai tap cho AI.
 *
 * Khong phai viet mot lan la xong. Ban nay la ban thu nam, chon sau khi chay thu
 * tren dung ba tam anh bai tap that cua Le Hoa (ngay 13/9, gemini-3.5-flash-lite):
 *
 *  - Ban khong co doan KIEM TRA LAI o cuoi: cau 2.26d sai that ma hai trong ba lan
 *    chay bao la dung. Them doan do vao: sai ba tren ba lan, kem cau nhan xet chi
 *    dung cho sai ("buoc dat nhan tu chung o dong cuoi bi nham dau").
 *  - Quy tac 1 (chep dung cai hoc sinh viet) va 2 (doc khong ro thi noi khong ro)
 *    co vi mot model khac - gemini-3.1-flash-lite - ba tren ba lan doc dong chu mo
 *    THANH DAP AN DUNG cua sach, tuc la bai sai thanh bai dung. Gap chu kho doc,
 *    may co xu huong dien vao cho trong bang cai no biet.
 *  - "so_dong" dem rat on dinh: ba lan chay ra cung mot con so cho ca bon cau.
 *
 * Goi hoc sinh la "con" chu khong phai "em": loi nhan xet hien tren man hinh cua
 * Le Hoa duoi ten Ba Huy. Mot dua tre biet minh dang bi may cham se di tim cach
 * lach may, vi may thi de qua mat hon nguoi that. Trong app, phia con KHONG bao gio
 * nhac den AI; phia Telegram cua Ba Huy thi noi ro la AI cham.
 *
 * Ban thu sau them "bai_duoc_giao" (ngay 14/9/2026). Vo dan do hom do khong giao
 * bai tap nao - chi "tiet sau kiem tra tu vung", "tiet sau kiem tra bai 2 bai 3",
 * "mang sach vo day du" - ma may van tra "lam_het_dan_do": true, tuc la 45 phut
 * tron goi tu tren troi roi xuong. Bat no ke ten bai phai lam ra thanh mot muc
 * rieng thi co mot cai de doi chieu: khong ke duoc thi khong co goi, ma ke ra thi
 * dong chu do di thang vao tin nhan Telegram cho Ba Huy doc.
 *
 * Do lai ba lan tren cung bo anh KHTN ngay 14/9 sau khi them: so cau AI tach ra
 * moi lan mot khac han - 36, 4, roi 22 cau cho dung nam tam anh. Do la ban chat
 * cua trang trac nghiem chi khoanh mot chu, khong phai loi cua ban cau lenh nay;
 * nhung no co nghia la KHONG duoc de so phut phu thuoc vao so cau AI dem duoc.
 *
 * Vi vay ban nay them dang TRAC_NGHIEM, tinh theo cum (xem [LuatCongGio]). Do lai
 * ba lan tren anh nop lai cua ngay 14/9: may nhan ra ca 25/26/21 cau dung deu la
 * TRAC_NGHIEM, va so phut ra 6, 6, 5 - thay vi 50, 52, 42 cua luat cu. So cau van
 * nhay nhu truoc, nhung so phut thi thoi.
 *
 * Ngay 23/9/2026 Ba Huy doi thanh mot cau mot phut. Voi trang dai, cai chan so phut
 * bay gio la tran moi lan nop [LuatCongGio.TRAN_TRAC_NGHIEM]: ba lan do tren deu
 * ra 15. Duoi tran thi so phut di theo so cau may dem duoc.
 *
 * Ban thu bay (ngay 16/9/2026), ba sua nho:
 *
 *  - [CAU_LENH_GIAI_THICH] va [CAU_LENH_DOC_LAI] goi hoc sinh la "em", trong khi hai
 *    ban kia goi la "con". Loi nhan xet hien duoi ten Ba Huy tren man hinh cua Le
 *    Hoa, nen mot cau lot chu "em" la lo ngay ra day khong phai loi cua nguoi;
 *  - bo chu "Hay" dau hai cau lenh do, cho cung giong ra lenh nhu ban chinh;
 * Ban do co thu hai thu de chan viec keo dai bai lam cho duoc nhieu phut, va BO ca
 * hai truoc khi chay:
 *
 *  - mot truong "so_dong_chuan" (may tu uoc bai nay giai gon nhat het may dong) de
 *    lam tran cho so dong con viet. Bo vi do la con so may tu nghi ra ma lai tru
 *    thang vao so phut cua con, trong khi do dai loi giai may sinh ra moi lan mot
 *    khac;
 *  - mot muc trong danh sach loai tru cua "bai_lam": dong chi chep lai ket qua vua
 *    viet o dong ngay tren. Bo vi dong do phan nhieu la dong ket luan ("Vay ... =
 *    ..."), tuc la cach co giao day trinh bay, chu khong phai con viet them de kiem
 *    phut. Phan gian lan that - chep di chep lai mot dong cho dai ra - thi muc "dong
 *    lap lai vo nghia" da chan roi.
 *
 * Con lai hai sua ve xung ho, va chung chi la chu nghia.
 *
 * Ban thu tam (ngay 16/9/2026), hai them:
 *
 *  - RANG BUOC VAN PHONG cho "nhan_xet". Truoc ban nay cho do chi bi rang ve noi
 *    dung ("sai o buoc nao, sua the nao"), khong bi rang ve giong. De tu do thi may
 *    viet kieu "Con can xem lai buoc dat nhan tu chung nhe! Hay kiem tra ky dau cua
 *    tung hang tu." Cau do hien tren man hinh cua Le Hoa duoi ten Ba Huy, ma Ba Huy
 *    khong noi nhu vay - doc vai chuc cau nhu the la dua tre doan ra ngay day la may
 *    cham, tuc la mat dung cai ma ca doan tren dang giu. Nen: toi da 15 chu, mo dau
 *    bang so dong sai, cam "Hay", cam "nhe", cam dau cham than, cam khen, va co mot
 *    cau mau ngay trong cau lenh;
 *  - "loai_loi": xep moi cau sai vao mot trong bay nhan co dinh. Truoc day may chi
 *    biet dem con sai bao nhieu cau, ma con so do khong noi duoc gi de ngoi noi
 *    chuyen voi con. Xep nhan thi sau mot thang co cau tra loi cho "con hay sai kieu
 *    gi" - sai dau hay nham cong thuc la hai viec phai day khac han nhau. Bay nhan
 *    chu khong de may tu dat chu: tu do thi moi lan chay ra mot ten khac, cong don
 *    lai khong ra con so nao. Xem [vn.huytl.homeworkgate.kho.KhoBai.thongKeLoi].
 *
 * Ca hai deu dat o CUOI danh sach quy tac, khong chen vao giua - dung ly do o tren.
 *
 * Sua cau lenh nay thi phai chay lai thu tren anh that, dung sua bang cam tinh:
 * them mot quy tac o giua co the lam loang quy tac cham diem (dung y nhu vay khi
 * them quy tac dem dong o ban thu tu).
 */
object PromptCham {

    /**
     * Cau lenh kem ngay hom nay.
     *
     * Phai noi cho may biet hom nay la ngay nao, vi ba ly do:
     *  - vo hoc sinh rat hay ghi "Thứ hai, ngày 14 tháng 9" khong co nam. Thu ngay
     *    13/9/2026: khong co dong nay thi may tra ve rong, va con mat oan tron goi
     *    45 phut. Co dong nay thi no tu suy ra nam;
     *  - de no biet vo dan do la cua hom nay hay cua tuan truoc;
     *  - may khong tu biet hom nay la ngay nao - no khong co dong ho.
     */
    fun cauLenh(
        homNay: java.time.LocalDate = java.time.LocalDate.now(),
        /** Vo dan do da chup va soat tu truoc, thay cho tam anh. Xem [doanDanDo]. */
        danDo: vn.huytl.homeworkgate.data.VoDanDo.DanDo? = null
    ): String =
        "Hôm nay là ngày ${homNay.dayOfMonth} tháng ${homNay.monthValue} năm ${homNay.year}.\n" +
            CAU_LENH + (danDo?.let { doanDanDo(it.ngay, it.cacBai) } ?: "")

    val CAU_LENH = """
Bạn chấm bài về nhà giúp phụ huynh. Ảnh gồm (có thể thiếu một số): trang vở dặn dò của cô giáo, trang đề bài in trong sách, và bài làm viết tay của học sinh.

Chỉ trả về JSON, không thêm chữ nào khác:
{"mon":"...","ngay_dan_do":"yyyy-MM-dd hoặc null","bai_duoc_giao":["bài 2","bài 3"],"lam_het_dan_do":true,
 "cac_cau":[{"ma":"2.26a","de":"chép lại đề của câu đó","co_de":true,"bai_lam":["dòng 1 học sinh viết","dòng 2 học sinh viết"],"dong_sai":2,"ket_qua":"kết quả cuối cùng học sinh viết","dung":true,"doc_ro":true,"dang":"CAU_NHO","trong_dan_do":true,"so_dong":4,"nhan_xet":"ngắn gọn","loai_loi":"SAI_DAU"}],
 "tom_tat":"một câu"}

Quy tắc bắt buộc:
1. "co_de": true CHỈ KHI bạn thực sự nhìn thấy ĐỀ BÀI của câu đó trong ảnh. Vở chỉ ghi đáp án ("câu 1: B", "2 đúng", "3 chất phản ứng") mà không có đề thì "co_de": false.
2. Câu "co_de": false thì BẮT BUỘC "dung": false. Không biết đề thì không có cách nào biết đáp án đúng hay sai — tuyệt đối không đoán, không suy từ đáp án ra đề.
3. "ket_qua" chép ĐÚNG cái học sinh viết trên giấy, kể cả khi sai. TUYỆT ĐỐI không sửa thành đáp án đúng.
4. "doc_ro": false nếu phải đoán bất kỳ ký tự nào trong bài làm của câu đó (chữ mờ, viết đè, dính nét). Thà nói không đọc được còn hơn đoán.
5. "dung": true CHỈ KHI mọi bước đều đúng và kết quả cuối cùng đúng hoàn toàn. Chỉ cần một dấu sai, một bước sai, hay kết quả lệch một ký tự -> "dung": false. Không có "gần đúng", không có "sai nhẹ".
6. Tự làm lại bài ra nháp trước, rồi mới so với bài của học sinh.
7. "bai_lam": chép lại TỪNG DÒNG học sinh tự viết để làm câu đó, mỗi dòng một phần tử, đúng thứ tự trên giấy. Chép đúng cái trên giấy kể cả khi sai. KHÔNG đưa vào: dòng chép lại đề, dòng trống, dòng đã gạch xoá, dòng lặp lại vô nghĩa.
8. "so_dong": bằng đúng số phần tử của "bai_lam".
9. "dong_sai": số thứ tự dòng đầu tiên sai trong "bai_lam" (1 là dòng đầu tiên). Cả bài đúng, hoặc không chỉ ra được dòng nào, thì để 0.
10. "dang" là một trong: TRAC_NGHIEM (chỉ cần khoanh một chữ, ghi Đúng/Sai, nối cột, điền một từ — không phải trình bày lời giải), CAU_NHO (câu nhỏ trong bài nhiều câu, có trình bày lời giải), BAI_RIENG (bài đứng riêng), VIET_DAI (đoạn văn, bài văn, báo cáo), KHONG_TINH (học thuộc, luyện chữ, chép bài).
11. "nhan_xet" viết cho học sinh đọc, xưng hô gọi học sinh là "con": sai ở bước nào, sửa thế nào. Không giải hộ. Tối đa 15 chữ. Có "dong_sai" thì mở đầu bằng số dòng đó. Cấm chữ "Hãy", cấm chữ "nhé", cấm dấu chấm than, cấm khen. Mẫu đúng: "Dòng 3 đổi dấu sai khi chuyển vế." Câu "dung": true thì để chuỗi rỗng.
12. "ngay_dan_do": ngày ghi trong vở dặn dò, đổi ra yyyy-MM-dd. Vở chỉ ghi ngày và tháng mà không ghi năm thì lấy năm sao cho ngày đó gần hôm nay nhất. Không có vở dặn dò, hoặc không thấy ngày, thì để null.
13. "bai_duoc_giao": chép ra tên từng BÀI TẬP PHẢI LÀM mà vở dặn dò giao, ví dụ ["bài 2","bài 3","SBT 2.26"]. Dặn dò chỉ nhắc việc chứ không phải bài tập nộp được — "mang sách vở đầy đủ", "tiết sau kiểm tra", "học thuộc", "làm đúng nội quy" — thì KHÔNG phải bài tập, để mảng rỗng []. Không có ảnh vở dặn dò thì cũng để rỗng.
14. "lam_het_dan_do": true CHỈ KHI "bai_duoc_giao" có ít nhất một bài VÀ trong ảnh thấy học sinh đã làm hết những bài đó. Mảng rỗng thì bắt buộc false.
15. "trong_dan_do": true nếu câu đó thuộc một bài trong "bai_duoc_giao". Không có ảnh vở dặn dò thì để true.
16. "loai_loi" xếp câu sai vào ĐÚNG MỘT trong bảy nhãn dưới đây, chép đúng chữ in hoa. Câu "dung": true thì để chuỗi rỗng.
   SAI_DAU: sai dấu, mất dấu, nhầm dấu khi chuyển vế hay khi phá ngoặc.
   SAI_BUOC: một bước biến đổi hay một bước lập luận sai, các bước khác đúng.
   NHAM_CONG_THUC: dùng nhầm công thức, quy tắc, định nghĩa, hằng đẳng thức.
   TINH_NHAM: cộng trừ nhân chia ra số sai, cách làm vẫn đúng.
   THIEU: thiếu trường hợp, thiếu điều kiện, thiếu kết luận, hoặc bỏ dở giữa chừng.
   LAC_DE: làm lệch cái đề hỏi, trả lời sang chuyện khác.
   KHAC: sai mà không thuộc sáu nhãn trên.

TRƯỚC KHI TRẢ LỜI, KIỂM TRA LẠI:
- Câu nào bạn không nhìn thấy đề bài thì "co_de": false và "dung": false. Một trang vở chỉ toàn đáp án thì TẤT CẢ các câu đều như vậy.
- Với mỗi câu, tự giải ra đáp án của mình, rồi so từng ký tự với kết quả cuối cùng học sinh viết. Khác một dấu, một chữ -> "dung": false.
- Nếu một bước trung gian sai nhưng đáp án cuối vẫn đúng -> "dung": false, và nói rõ bước nào sai.
- Không được vì bài trình bày đẹp mà cho là đúng.
    """.trimIndent()

    /**
     * Model dung de cham.
     *
     * Chon gemini-3.5-flash-lite sau khi do sau model tren cung bo anh:
     *
     *  | model                 | giay | token nghi | cau 2.26d (sai that) |
     *  | gemini-3.5-flash-lite |  3-4 |          0 | Sai 3/3 lan          |
     *  | gemini-3.5-flash      |   21 |      4.222 | Sai                  |
     *  | gemini-3.1-flash-lite |  3-5 |          0 | Dung 3/3 - doc nham  |
     *  | gemini-3-flash-preview|   18 |      3.391 | Dung - doc nham      |
     *
     * Ban lite vua nhanh nhat, re nhat (khong dot token "suy nghi"), vua la ban bat
     * duoc loi. Hai ban kia doc dong chu mo thanh dap an dung cua sach.
     *
     * gemini-2.5-* tra 404 voi khoa moi tao ("no longer available to new users"),
     * gemini-3.1-pro tra 429 "limit: 0" - khong co suat mien phi.
     */
    const val MODEL = "gemini-3.5-flash-lite"

    /** Mot lan cham ba tam anh het khoang bay nhieu token vao. De uoc han muc. */
    const val TOKEN_MOI_LAN_UOC = 3_800

    /**
     * Cau lenh thu hai, chi goi khi co cau sai: nho AI chi ro cho sai cho con sua.
     *
     * Tach lam hai lan goi chu khong gop vao mot, vi da thu gop: them mot quy tac
     * "nhan xet phai chi ro dong nao sai" vao [CAU_LENH] thi chinh phan cham diem
     * bi loang - cau 2.27c sai that ma hai tren hai lan chay bao la dung. Cham va
     * giai thich la hai viec, gop lai thi may lam hong ca hai.
     *
     * Lan goi nay re hon nhieu (chi gui anh bai giai, khong gui trang sach) va bo
     * qua duoc khi het han muc: con van biet cau nao sai, chi la khong co loi giai
     * thich kem theo.
     */
    val CAU_LENH_GIAI_THICH = """
Học sinh làm sai mấy câu dưới đây. Chỉ cho học sinh chỗ sai để tự sửa.

Các câu sai (mã | đề | kết quả em viết):
{DANH_SACH}

Chỉ trả về JSON, không thêm chữ nào khác:
{"giai_thich":[{"ma":"2.27a","loi":"Dòng 2: con viết \"= 2(x+y)(...)\" là sai, ... (chỉ rõ dòng nào, chép lại đoạn sai)"}]}

Quy tắc:
- Mỗi câu 1-2 câu văn, viết cho học sinh đọc, gọi học sinh là "con". Tối đa 30 chữ.
- BẮT BUỘC chép lại đoạn viết sai ra và nói sai ở đâu. Cấm viết chung chung kiểu "cần kiểm tra lại các bước".
- Cấm chữ "Hãy", cấm chữ "nhé", cấm dấu chấm than, cấm khen. Mở đầu bằng số dòng nếu chỉ ra được dòng.
- KHÔNG đưa đáp án đúng. Chỉ nói sai chỗ nào để học sinh tự làm lại.
    """.trimIndent()

    /**
     * Cau lenh cho lan nop CO KHAI BAI: con da chon san minh dang lam nhung cau nao.
     *
     * Khac ban tu do o dung mot cho, nhung do la cho quan trong nhat: may khong con
     * duoc tu quyet dinh trong anh co bao nhieu cau va moi cau ten gi. Danh sach da
     * nam san trong cau lenh, viec cua may chi con la cham dung hay sai.
     *
     * Vi sao phai the: do ba lan tren cung nam tam anh KHTN ngay 14/9/2026, may tach
     * ra 36, roi 4, roi 22 cau, va moi lan chep de mot khac. So cai lay de bai lam
     * khoa nen cung mot bai hom nay mot khoa, mai mot khoa - tinh gio lan hai. Cho
     * san danh sach thi khoa la ma sach, khong con gi de lech.
     *
     * Van cho phep may bao cau NGOAI danh sach: con lam them bai khong khai truoc la
     * chuyen binh thuong, va bat may im di thi phan lam them khong bao gio duoc tinh.
     * Nhung cau do di duong cu - nhan dang bang de bai - nen kem chac hon.
     */
    fun cauLenhTheoDanhSach(
        cac: List<vn.huytl.homeworkgate.kho.CauHoi>,
        tenNguon: String,
        tenBai: String,
        homNay: java.time.LocalDate = java.time.LocalDate.now(),
        /** Lan nay la on lai bai cu. Them mot cau hoi ve mau muc, xem [DOAN_ON_TAP]. */
        onTap: Boolean = false,
        /** Vo dan do da chup va soat tu truoc, thay cho tam anh. Xem [doanDanDo]. */
        danDo: vn.huytl.homeworkgate.data.VoDanDo.DanDo? = null
    ): String {
        val danhSach = cac.joinToString("\n") { "${it.ma} | ${it.de}" }
        return "Hôm nay là ngày ${homNay.dayOfMonth} tháng ${homNay.monthValue} " +
            "năm ${homNay.year}.\n" +
            CAU_LENH_KHAI_BAI
                .replace("{NGUON}", tenNguon)
                .replace("{BAI}", tenBai)
                .replace("{DANH_SACH}", danhSach) +
            (if (onTap) DOAN_ON_TAP else "") +
            (danDo?.let { doanDanDo(it.ngay, it.cacBai) } ?: "")
    }

    /**
     * Doc mot trang vo dan do ra chu. Khong cham gi ca, chi doc.
     *
     * Tach han khoi duong cham bai vi hai viec nay xay ra o hai luc khac nhau: doc
     * vo dan do la viec dau buoi, lam MOT lan; cham bai la viec cuoi buoi, lam bao
     * nhieu lan cung duoc. Va vi ban doc ra chu con phai qua mat Le Hoa soat lai -
     * mot buoc ma duong cham bai khong co.
     *
     * HOI IT THOI. Chi can ngay va danh sach bai tap: do la hai thu duy nhat
     * [vn.huytl.homeworkgate.data.LuatCongGio] dung toi. Phan dan do khac hoi them
     * cho con doc, khong di vao cho nao tinh gio.
     */
    fun cauLenhDocDanDo(homNay: java.time.LocalDate = java.time.LocalDate.now()): String =
        "Hôm nay là ngày ${homNay.dayOfMonth} tháng ${homNay.monthValue} " +
            "năm ${homNay.year}.\n" + CAU_LENH_DAN_DO

    val CAU_LENH_DAN_DO = """
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

    /**
     * Doan noi them vao cau lenh cham khi lan nop nay KHONG co anh vo dan do, nhung
     * trong may da co ban da soat.
     *
     * Phai noi ro de no de len quy tac "khong co anh vo dan do thi trong_dan_do de
     * true". Quy tac do sinh ra cho lan nop khong biet gi ve dan do; con o day thi
     * biet, va biet chac hon ca anh - doan chu nay da qua mat con soat lai.
     */
    fun doanDanDo(ngay: String, cacBai: List<String>): String {
        val ke = if (cacBai.isEmpty()) {
            "hôm đó cô giáo KHÔNG giao bài tập nào"
        } else {
            cacBai.joinToString("; ")
        }
        return """


Nội dung trang vở dặn dò đã được chép sẵn từ đầu buổi và học sinh đã soát lại:
- Ngày ghi trên vở: $ngay
- Bài cô giáo giao: $ke

Dùng đúng nội dung này, KHÔNG suy từ ảnh: "ngay_dan_do" lấy đúng ngày trên, "bai_duoc_giao" lấy đúng danh sách trên. "trong_dan_do" của một câu là true chỉ khi câu đó thuộc một trong các bài được giao ở trên; danh sách rỗng thì mọi câu đều false. Quy tắc "không có ảnh vở dặn dò thì để true" KHÔNG áp dụng lần này.
        """.trimIndent()
    }

    /**
     * Noi them vao cuoi cau lenh khi la lan on tap.
     *
     * Chi hoi mot thu: bai lam viet bang muc mau gi. Do la quan sat tren anh, khong
     * phai con so may tu nghi ra - nen no on dinh hon nhieu so voi kieu "uoc xem bai
     * nay dang may dong".
     *
     * Vi sao can: on tap la duong duy nhat duoc cham lai mot cau da lam dung, tuc la
     * no thao mat cai khoa "moi cau chi tra gio mot lan". Chup lai trang vo cu thi
     * anh khong khac gi anh bai vua lam. Luat nha bit cho do: on thi viet but do.
     *
     * Dat o CUOI chu khong chen vao giua danh sach quy tac: ban thu tu da cho thay
     * them mot quy tac o giua lam loang han phan cham diem.
     */
    val DOAN_ON_TAP = """

Lần này học sinh ÔN LẠI bài cũ. Nhà quy định bài ôn phải viết bằng mực ĐỎ, để phân biệt với bài đã làm từ trước bằng mực thường.

Thêm vào mỗi câu một trường nữa:
"muc_do": true nếu bài làm của câu đó viết bằng mực đỏ; false nếu viết bằng mực xanh, đen, bút chì, hoặc nhìn không rõ màu.

Chỉ nhìn màu của BÀI LÀM học sinh viết, không tính màu của đề in trong sách hay chữ cô giáo chữa.
    """.trimIndent()

    val CAU_LENH_KHAI_BAI = """
Bạn chấm bài về nhà giúp phụ huynh. Ảnh gồm (có thể thiếu một số): trang vở dặn dò của cô giáo, trang đề bài in trong sách, và bài làm viết tay của học sinh.

Học sinh khai là đang làm những câu sau, lấy từ {NGUON} — {BAI}:
{DANH_SACH}

Chỉ trả về JSON, không thêm chữ nào khác:
{"mon":"...","ngay_dan_do":"yyyy-MM-dd hoặc null","bai_duoc_giao":["bài 2","bài 3"],"lam_het_dan_do":true,
 "cac_cau":[{"ma":"2.26a","co_lam":true,"co_de":true,"bai_lam":["dòng 1 học sinh viết","dòng 2 học sinh viết"],"dong_sai":2,"ket_qua":"kết quả cuối cùng học sinh viết","dung":true,"doc_ro":true,"dang":"CAU_NHO","trong_dan_do":true,"so_dong":4,"nhan_xet":"ngắn gọn","loai_loi":"SAI_DAU","ngoai_danh_sach":false,"de":"chỉ cần khi ngoai_danh_sach là true"}],
 "tom_tat":"một câu"}

Quy tắc bắt buộc:
1. "ma" phải CHÉP ĐÚNG mã trong danh sách trên, không tự đặt mã khác, không gộp hai câu làm một, không tách một câu làm hai.
2. "co_lam": false nếu trong ảnh KHÔNG thấy bài làm của câu đó, hoặc bài làm trong ảnh rõ ràng không phải câu đó. Câu "co_lam": false thì để "dung": false và không cần nhận xét.
3. Ảnh có bài làm không thuộc danh sách thì vẫn ghi vào "cac_cau", đặt "ngoai_danh_sach": true, "ma" theo đúng cách sách đánh số câu đó, và "de" chép lại đề của câu đó. Câu nằm trong danh sách thì không cần "de".
4. "co_de": câu nằm trong danh sách trên thì luôn là true (đề đã cho sẵn ở trên). Câu "ngoai_danh_sach" thì true CHỈ KHI bạn nhìn thấy đề của nó trong ảnh; vở chỉ ghi đáp án mà không có đề thì false, và khi đó BẮT BUỘC "dung": false — không biết đề thì không có cách nào biết đúng sai.
5. "ket_qua" chép ĐÚNG cái học sinh viết trên giấy, kể cả khi sai. TUYỆT ĐỐI không sửa thành đáp án đúng.
6. "doc_ro": false nếu phải đoán bất kỳ ký tự nào trong bài làm của câu đó (chữ mờ, viết đè, dính nét). Thà nói không đọc được còn hơn đoán.
7. "dung": true CHỈ KHI mọi bước đều đúng và kết quả cuối cùng đúng hoàn toàn. Chỉ cần một dấu sai, một bước sai, hay kết quả lệch một ký tự -> "dung": false. Không có "gần đúng", không có "sai nhẹ".
8. Tự làm lại bài ra nháp trước, rồi mới so với bài của học sinh.
9. "bai_lam": chép lại TỪNG DÒNG học sinh tự viết để làm câu đó, mỗi dòng một phần tử, đúng thứ tự trên giấy. Chép đúng cái trên giấy kể cả khi sai, TUYỆT ĐỐI không sửa thành lời giải đúng. KHÔNG đưa vào: dòng chép lại đề, dòng trống, dòng đã gạch xoá, dòng lặp lại vô nghĩa.
10. "so_dong": bằng đúng số phần tử của "bai_lam".
11. "dong_sai": số thứ tự dòng đầu tiên sai trong "bai_lam" (1 là dòng đầu tiên). Cả bài đúng, hoặc không chỉ ra được dòng nào, thì để 0.
12. "dang" là một trong: TRAC_NGHIEM (chỉ cần khoanh một chữ, ghi Đúng/Sai, nối cột, điền một từ — không phải trình bày lời giải), CAU_NHO (câu nhỏ trong bài nhiều câu, có trình bày lời giải), BAI_RIENG (bài đứng riêng), VIET_DAI (đoạn văn, bài văn, báo cáo), KHONG_TINH (học thuộc, luyện chữ, chép bài).
13. "nhan_xet" viết cho học sinh đọc, xưng hô gọi học sinh là "con": sai ở bước nào, sửa thế nào. Không giải hộ. Tối đa 15 chữ. Có "dong_sai" thì mở đầu bằng số dòng đó. Cấm chữ "Hãy", cấm chữ "nhé", cấm dấu chấm than, cấm khen. Mẫu đúng: "Dòng 3 đổi dấu sai khi chuyển vế." Câu "dung": true thì để chuỗi rỗng.
14. "ngay_dan_do": ngày ghi trong vở dặn dò, đổi ra yyyy-MM-dd. Vở chỉ ghi ngày và tháng mà không ghi năm thì lấy năm sao cho ngày đó gần hôm nay nhất. Không có vở dặn dò, hoặc không thấy ngày, thì để null.
15. "bai_duoc_giao": chép ra tên từng BÀI TẬP PHẢI LÀM mà vở dặn dò giao, ví dụ ["bài 2","bài 3","SBT 2.26"]. Dặn dò chỉ nhắc việc chứ không phải bài tập nộp được — "mang sách vở đầy đủ", "tiết sau kiểm tra", "học thuộc", "làm đúng nội quy" — thì KHÔNG phải bài tập, để mảng rỗng []. Không có ảnh vở dặn dò thì cũng để rỗng.
16. "lam_het_dan_do": true CHỈ KHI "bai_duoc_giao" có ít nhất một bài VÀ trong ảnh thấy học sinh đã làm hết những bài đó. Mảng rỗng thì bắt buộc false.
17. "trong_dan_do": true nếu câu đó thuộc một bài trong "bai_duoc_giao". Không có ảnh vở dặn dò thì để true.
18. "loai_loi" xếp câu sai vào ĐÚNG MỘT trong bảy nhãn dưới đây, chép đúng chữ in hoa. Câu "dung": true thì để chuỗi rỗng.
   SAI_DAU: sai dấu, mất dấu, nhầm dấu khi chuyển vế hay khi phá ngoặc.
   SAI_BUOC: một bước biến đổi hay một bước lập luận sai, các bước khác đúng.
   NHAM_CONG_THUC: dùng nhầm công thức, quy tắc, định nghĩa, hằng đẳng thức.
   TINH_NHAM: cộng trừ nhân chia ra số sai, cách làm vẫn đúng.
   THIEU: thiếu trường hợp, thiếu điều kiện, thiếu kết luận, hoặc bỏ dở giữa chừng.
   LAC_DE: làm lệch cái đề hỏi, trả lời sang chuyện khác.
   KHAC: sai mà không thuộc sáu nhãn trên.

TRƯỚC KHI TRẢ LỜI, KIỂM TRA LẠI:
- Câu ngoài danh sách mà bạn không nhìn thấy đề thì "co_de": false và "dung": false.
- Mỗi câu trong danh sách trên xuất hiện đúng một lần trong "cac_cau", không thiếu câu nào.
- Với mỗi câu, tự giải ra đáp án của mình, rồi so từng ký tự với kết quả cuối cùng học sinh viết. Khác một dấu, một chữ -> "dung": false.
- Nếu một bước trung gian sai nhưng đáp án cuối vẫn đúng -> "dung": false, và nói rõ bước nào sai.
- Không được vì bài trình bày đẹp mà cho là đúng.
    """.trimIndent()

    /**
     * Cau lenh thu ba: con bao may doc nham, nho may NHIN LAI ANH.
     *
     * Day la chot chan cua man soat bai. Con sua duoc tung dong may doc ra, nen neu
     * cu tin thang chu con go vao ma cham lai thi con go dap an dung vao la duoc
     * cong gio, trong khi tren giay van sai. Nen loi con sua khong phai ban thay
     * the - no chi la LOI KHAI de may soi lai dung cho do tren anh.
     *
     * May soi lai roi cham lai ca cau tu ANH, khong phai tu chu con go. Ba duong ra:
     *
     *  - anh dung la nhu con noi  -> cham theo ban moi, tu duyet nhu thuong;
     *  - anh khong phai nhu con noi -> "dung_nhu_hoc_sinh_noi": false, app ha
     *    "doc_ro" xuong de KHONG tu duyet, Ba Huy cam ca hai ban voi tam anh ma quyet;
     *  - van khong doc noi -> "doc_ro": false, cung sang tay Ba Huy.
     *
     * Chi gui anh BAI GIAI, khong gui trang sach hay vo dan do: cho can nhin lai chi
     * nam trong bai lam, va lan goi nay phai re vi no co the xay ra vai lan mot toi.
     */
    fun cauLenhDocLai(cac: List<KhaiSua>): String =
        CAU_LENH_DOC_LAI.replace(
            "{DANH_SACH}",
            cac.joinToString("\n\n") { k ->
                "${k.ma} | ${k.de}\nhọc sinh nói mình viết:\n" +
                    k.dongKhai.mapIndexed { i, d -> "  dòng ${i + 1}: $d" }.joinToString("\n")
            }
        )

    /**
     * Mot cau con bao may doc nham, kem cac dong con noi minh da viet.
     */
    data class KhaiSua(val ma: String, val de: String, val dongKhai: List<String>)

    val CAU_LENH_DOC_LAI = """
Bạn đã chấm bài này rồi. Học sinh nói bạn đọc nhầm chữ viết tay ở mấy câu dưới đây. NHÌN LẠI ẢNH thật kỹ ở đúng những câu đó, rồi chấm lại.

{DANH_SACH}

Chỉ trả về JSON, không thêm chữ nào khác:
{"cac_cau":[{"ma":"2.26a","dung_nhu_hoc_sinh_noi":true,"bai_lam":["dòng 1","dòng 2"],"dong_sai":2,"ket_qua":"kết quả cuối cùng","dung":false,"doc_ro":true,"so_dong":2,"nhan_xet":"ngắn gọn","loai_loi":"SAI_DAU"}]}

Quy tắc bắt buộc:
1. "bai_lam" chép lại cái BẠN ĐỌC ĐƯỢC TRÊN ẢNH sau khi nhìn kỹ — KHÔNG phải chép lại lời học sinh. Học sinh có thể nói sai, cố ý hoặc vô tình.
2. "dung_nhu_hoc_sinh_noi": true CHỈ KHI nhìn kỹ lại thấy trên giấy đúng là như học sinh nói. Còn phân vân thì để false.
3. "doc_ro": false nếu nhìn kỹ rồi mà vẫn phải đoán ký tự.
4. Chấm lại câu đó theo đúng cái bạn vừa đọc được: tự giải ra nháp trước, rồi so từng ký tự với kết quả cuối cùng. Sai một dấu -> "dung": false.
5. "dong_sai": số thứ tự dòng đầu tiên sai trong "bai_lam" (1 là dòng đầu). Đúng hết thì 0.
6. "nhan_xet" viết cho học sinh đọc, gọi học sinh là "con": sai ở bước nào. Không giải hộ, không đưa đáp án. Tối đa 15 chữ. Có "dong_sai" thì mở đầu bằng số dòng đó. Cấm chữ "Hãy", cấm chữ "nhé", cấm dấu chấm than, cấm khen. Mẫu đúng: "Dòng 3 đổi dấu sai khi chuyển vế." Câu "dung": true thì để chuỗi rỗng.
7. "loai_loi" xếp câu sai vào ĐÚNG MỘT trong bảy nhãn, chép đúng chữ in hoa: SAI_DAU, SAI_BUOC, NHAM_CONG_THUC, TINH_NHAM, THIEU, LAC_DE, KHAC. Câu "dung": true thì để chuỗi rỗng.
    """.trimIndent()

    /** Dat danh sach cau sai vao [CAU_LENH_GIAI_THICH]. */
    fun cauLenhGiaiThich(cac: List<vn.huytl.homeworkgate.data.CauCham>): String =
        CAU_LENH_GIAI_THICH.replace(
            "{DANH_SACH}",
            cac.joinToString("\n") { "${it.ma} | ${it.de} | ${it.ketQua}" }
        )
}
