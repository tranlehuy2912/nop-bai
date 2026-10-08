package vn.huytl.homeworkgate.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Nhat ky cau con hoi app AI trong ngay.
 *
 * Tach khoi [DayLog] vi hai thu khac muc dich va khac vong doi. DayLog la su kien
 * cong, giu mot ngay, tra loi "hom nay choi may lan". Cai nay la noi dung con go
 * vao AI, de Ba Huy doc lai xem con nho AI giai ho hay chi nho chi cho sai.
 *
 * Van chi giu vai ngay, khong giu mai: dich la doc trong tuan roi thoi, khong phai
 * lap ho so. Tu 8/10/2026 giu [NGAY_GIU] ngay ke ca hom nay, khong gioi han so cau (truoc
 * do 200 dong, ngay con hoi nhieu thi cau som nhat bi day ra), bang so ngay ban sao tren
 * Firestore (hoiai/{ngay}) va bang trang "Thời gian dùng app": anh Huy muon xem lai bay
 * ngay tren Bang dieu khien.
 */
object NhatKyAi {

    private const val K_TEXT = "nhatky_ai_text"

    /** So ngay giu trong may, tinh ca hom nay. */
    const val NGAY_GIU = DayLog.NGAY_GIU_TREN_MANG

    /**
     * Dau ghi thay cho moi lan con xuong dong trong cau, xem [motDong].
     *
     * Bang dieu khien doi dau nay lai thanh xuong dong that khi ve the "Hoi AI"
     * (DongHoiAi ben bang-dieu-khien), tin /hoi cung vay (xem [traXuongDong]). Cho nao
     * chua doi lai - logcat, ban Bang dieu khien cu - van doc ra la sang dong moi, nho
     * mui ten va hai dau cach.
     */
    const val XUONG_DONG = " ↵ "

    /** Moi kieu xuong dong con co the go hay dan vao, ke ca ba kieu cua Unicode. */
    private val CHO_XUONG_DONG = Regex("""\r\n|[\n\r\u0085\u2028\u2029]""")

    private val dongHo = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("vi-VN"))

    /** Dau moi cau trong so, dung nhu [ghi] viet: ngay gio, hai dau cach, [ten app]. */
    private val DAU_CAU = Regex("""^\d{2}/\d{2} \d{2}:\d{2}  \[""")

    /** Ghi mot cau con vua hoi [tenApp]. Tra ve dong da ghi, de con gui di Telegram. */
    fun ghi(context: Context, tenApp: String, cau: String): String {
        val sp = Prefs.get(context).raw()
        val dong = "${dongHo.format(Date())}  [$tenApp]  ${motDong(cau)}"
        val cu = sp.getString(K_TEXT, "").orEmpty()
        val moi = conTrongHan(tachCau(cu) + dong)
        sp.edit().putString(K_TEXT, moi.joinToString("\n")).commit()
        return dong
    }

    /**
     * Bo cac cau cu hon [NGAY_GIU] ngay, xet theo ngay ghi o dau cau ("dd/MM").
     *
     * So chi ghi ngay va thang, khong ghi nam; ma so khong bao gio giu qua bay ngay nen
     * khong co hai cau cung "dd/MM" khac nam. Cau ghi truoc 23/9/2026 co the nam tren nhieu
     * dong, [tachCau] da gop dong tiep vao cau cua no nen bo cau nao thi bo ca dong tiep.
     */
    internal fun conTrongHan(cac: List<String>, now: Long = System.currentTimeMillis()): List<String> {
        val dinhDang = SimpleDateFormat("dd/MM", Locale.forLanguageTag("vi-VN"))
        val lich = Calendar.getInstance().apply { timeInMillis = now }
        val ngay = (0 until NGAY_GIU).map {
            dinhDang.format(lich.time).also { lich.add(Calendar.DAY_OF_MONTH, -1) }
        }.toSet()
        return cac.filter { cau -> cau.take(5) in ngay }
    }

    /**
     * Cau con go, viet lai thanh mot dong de cat vao so.
     *
     * Mot cau mot dong. So nay noi cac cau bang dau xuong dong roi doc lai bang
     * lines(), va [homNay] chi giu dong bat dau bang ngay - nen de nguyen cau hai dong
     * thi dong thu hai roi mat, ca o /hoi lan the "Hoi AI" tren Bang dieu khien. Truoc
     * 23/9/2026 no mat that.
     *
     * Tu 23/9 den 28/9/2026 cho xuong dong bi thay bang dau cach. Ngay 28/9 Ba Huy bao
     * the Hoi AI "chua xuong hang": de bai con dan vao bon dong ("... duong cao AH." /
     * "a) ..." / "b) ..." / "c) ...") hien thanh mot doan lien, chu "b)" treo cuoi dong.
     * Nen gio moi cho xuong dong ghi thanh [XUONG_DONG] de Bang dieu khien va tin /hoi
     * tra lai duoc. Cau ghi trong khoang do van la mot doan lien, khong khoi phuc duoc.
     *
     * Dong trong va khoang trang quanh cho xuong dong gop lai thanh mot lan xuong
     * dong: the Hoi AI dung dong trong de ngan hai cau, de dong trong ben trong mot cau
     * thi nhin nhu sang cau moi. U+2028, U+2029, U+0085 cung tinh la xuong dong.
     * lines() khong cat o chung nen so khong hong, nhung dau cham trong khuon doc ben
     * Bang dieu khien khong khop chung, va ca dong se hien nguyen ban.
     *
     * Tin Telegram van gui cau goc, xem noi goi [ghi].
     */
    fun motDong(cau: String): String =
        cau.split(CHO_XUONG_DONG)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(XUONG_DONG)

    /**
     * Doi [XUONG_DONG] trong so (ban cua [tatCa] hay [homNay]) ve lai xuong dong that,
     * cho tin /hoi.
     *
     * Moi cau van mo dau bang ngay gio va [ten app], nen dong khong co ngay o dau la
     * phan tiep cua cau ben tren. Khong them dong trong giua hai cau nhu the Hoi AI ben
     * Bang dieu khien: mot tin Telegram chi chua 4096 chu (TelegramClient.MAX_TIN), them
     * dong trong la [tinHoi] phai bo them cau cu.
     *
     * Chi doi dung chuoi [motDong] ghi, nen dau ↵ con tu go ma khong co hai dau cach
     * hai ben thi giu nguyen.
     */
    fun traXuongDong(so: String): String = so.replace(XUONG_DONG, "\n")

    /**
     * Than tin /hoi: dong [tieuDe], roi cac cau trong [so] (ban cua [tatCa] hay [homNay])
     * voi cho con xuong dong tra lai nhu [traXuongDong].
     *
     * Dai qua [toiDa] chu thi bo cau cu nhat truoc, bo ca cau chu khong cat giua cau, va
     * dong thu hai cua tin noi bao nhieu cau khong hien. So xep cau cu len truoc, ma
     * TelegramClient.sendMessage cat duoi tin dai qua mot tin, nen truoc 28/9/2026 tin dai
     * mat dung may cau moi nhat, la cau Ba Huy dang muon doc. /hoi tatca la ca [NGAY_GIU]
     * ngay, so day la chac chan dai qua mot tin.
     *
     * Cau moi nhat luon duoc giu, ke ca khi mot minh no da dai qua [toiDa]: [BoGoAi]
     * khong co tran do dai, con dan ca bai doc vao app AI la co. Khi do sendMessage cat
     * duoi cau do, y nhu tin bao luc con vua hoi.
     */
    fun tinHoi(tieuDe: String, so: String, toiDa: Int): String {
        val cac = tachCau(so).map { traXuongDong(it) }
        val ca = tieuDe + "\n" + cac.joinToString("\n")
        if (ca.length <= toiDa || cac.size < 2) return ca

        // Chua cho cho dong bao viet voi cac.size: so cau bo luon it hon cac.size, nen dong
        // bao that khong dai hon. Con 2 la hai dau xuong dong, sau [tieuDe] va sau dong bao.
        var con = toiDa - tieuDe.length - baoBot(cac.size).length - 2 - cac.last().length
        var tu = cac.lastIndex
        while (tu > 0 && cac[tu - 1].length + 1 <= con) {
            tu--
            con -= cac[tu].length + 1
        }
        return tieuDe + "\n" + baoBot(tu) + "\n" + cac.drop(tu).joinToString("\n")
    }

    /** Dong thu hai cua tin /hoi khi [tinHoi] phai bo [n] cau cu nhat. */
    private fun baoBot(n: Int) = "(Tin dài quá nên không hiện $n câu cũ nhất.)"

    /**
     * Cac cau trong [so], cu nhat truoc.
     *
     * Dong khong mo dau bang [DAU_CAU] la phan tiep cua cau ben tren. Tu 16/9 den 23/9/2026
     * [ghi] con de nguyen cho con xuong dong, nen cau nhieu dong ghi trong khoang do nam
     * tren nhieu dong cua so. [homNay] bo mat cac dong tiep do, [tatCa] thi van con. Gop
     * lai de [tinHoi] bo cau nao thi bo ca dong tiep cua no: dau tin khong treo mot dong
     * le khong ro cua cau nao, va so cau khong hien dem dung.
     */
    private fun tachCau(so: String): List<String> {
        val cac = mutableListOf<String>()
        for (dong in so.lines()) {
            if (dong.isBlank()) continue
            if (cac.isEmpty() || DAU_CAU.containsMatchIn(dong)) cac += dong
            else cac[cac.lastIndex] += "\n" + dong
        }
        return cac
    }

    /** Toan bo nhat ky con giu. */
    fun tatCa(context: Context): String =
        Prefs.get(context).raw().getString(K_TEXT, "").orEmpty()

    /** Chi cac dong cua hom nay. */
    fun homNay(context: Context): String {
        val hom = SimpleDateFormat("dd/MM", Locale.forLanguageTag("vi-VN")).format(Date())
        return tatCa(context).lines().filter { it.startsWith(hom) }.joinToString("\n")
    }
}
