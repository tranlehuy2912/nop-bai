package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Cua nhan lenh cua app "Cho gio choi" tren may ba noi.
 *
 * TAI SAO PHAI CO CUA RIENG: app ben may ba cam cung token bot van khong go lenh
 * duoc. Telegram co tinh khong tra ve tin nhan do chinh con bot gui trong
 * getUpdates cua no - de hai con bot khong noi vong tron voi nhau. Nghia la moi
 * duong "gia lam Ba Huy go /cho 30" deu tac.
 *
 * Nen doi huong: may ba khong gui lenh ma *dat* lenh xuong mot cho co dinh, tablet
 * ghe qua doc. Cho do la mot nhom Telegram rieng, bot lam quan tri:
 *
 *  - mo ta nhom (setChatDescription / getChat.description): hop thu chinh;
 *  - tin ghim (getChat.pinned_message): duong du phong, phong khi quyen doi thong
 *    tin nhom bi tat.
 *
 * VE PIN: cho nay khong mo them ket noi nao. No di ke dung vong long-poll tablet
 * von da chay san, cung lan CPU thuc day, cung socket TLS OkHttp dang giu - giong
 * cach dong ho dem nguoc dang lam. Va no tu tat: ba chi co mot luot moi ngay, dung
 * xong la thoi hoi cho den sang mai.
 */
object HopThuBaNoi {

    /** Mot lenh doc duoc trong hop thu. */
    data class Lenh(
        val phut: Int,
        /** Luc may ba bam nut, giay tu 1970. */
        val guiLuc: Long,
        /** Ca chuoi goc, dung de nho la da xu ly cai nay roi. */
        val dau: String
    )

    /** Nhom dung lam hop thu. 0 la chua noi, luc do khong hoi han. */
    fun nhom(context: Context): Long = sp(context).getLong(K_NHOM, 0L)

    fun datNhom(context: Context, id: Long) {
        sp(context).edit().putLong(K_NHOM, id).commit()
    }

    /**
     * Hom nay co nen ghe hop thu khong.
     *
     * Ba cho roi thi thoi hoi den sang mai - do la cho tiet kiem nhat, vi phan lon
     * thoi gian trong ngay la sau khi ba da cho.
     *
     * Chan duoi 06:00 la khop voi khung gio app ben may ba cho bam. Som hon thi
     * khong the co lenh nao, hoi cung khong de lam gi.
     *
     * Chan tren doc thang [Prefs.hardStopMinuteOfDay] chu khong dong cung 22:00.
     * So do Ba Huy doi duoc trong man Cai dat: dong cung o day thi ha gio chot
     * xuong 20:00 la tablet van ngo hop thu tho tho den 22:00, ma co doc duoc lenh
     * cung khong cap noi.
     */
    fun nenNgo(context: Context): Boolean {
        if (nhom(context) == 0L) return false
        if (daChoHomNay(context)) return false
        val c = Calendar.getInstance()
        val phut = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        return phut in SOM_NHAT until (Prefs.get(context).hardStopMinuteOfDay + AN_HAN)
    }

    /**
     * Hom nay ba da cho mot lan roi chua.
     *
     * Ben may ba cung dem. Dem ca hai dau la co chu y: may ba go cai dat hay cai
     * lai app la so dem ben do ve khong, luc do chi con cho nay chan.
     */
    fun daChoHomNay(context: Context): Boolean =
        sp(context).getString(K_NGAY, "") == homNay()

    fun ghiNhanDaCho(context: Context) {
        sp(context).edit().putString(K_NGAY, homNay()).commit()
    }

    /**
     * Doc lenh moi tu cau tra loi cua getChat.
     *
     * Tra ve null khi khong co gi moi. "Moi" nghia la chuoi dau khac chuoi lan
     * truoc da xu ly: ma ngau nhien trong chuoi lam hai lan bam cung so phut van
     * ra hai chuoi khac nhau, con mo ta nhom nam nguyen do sau khi doc thi khong
     * bi doc lai lan hai.
     */
    fun lenhMoi(context: Context, chat: JSONObject): Lenh? {
        val da = sp(context).getString(K_DA_DOC, "").orEmpty()
        val ung = listOfNotNull(
            chat.optString("description").takeIf { it.isNotBlank() },
            chat.optJSONObject("pinned_message")?.optString("text")?.takeIf { it.isNotBlank() }
        )
        for (text in ung) {
            val lenh = phanTich(text) ?: continue
            if (lenh.dau == da) continue
            return lenh
        }
        return null
    }

    /**
     * Danh dau da xu ly, ke ca khi tu choi.
     *
     * Phai danh dau ca truong hop tu choi, khong thi lenh cu nam trong mo ta nhom
     * se bi doc di doc lai moi phut cho den khi ba bam lan sau.
     */
    fun danhDauDaDoc(context: Context, lenh: Lenh) {
        sp(context).edit().putString(K_DA_DOC, lenh.dau).commit()
    }

    /**
     * Tach "CHOGIO|30|1757778300|a3f9" thanh so phut va luc gui.
     *
     * Chi nhan dung khuon do. Ai ghim mot tin thuong hay sua mo ta nhom thanh cau
     * gi khac thi ham nay tra null, tablet lam ngo - nhom do van dung lam cho tro
     * chuyen binh thuong duoc.
     */
    private fun phanTich(text: String): Lenh? {
        val dau = text.trim().substringBefore(' ').substringBefore('\n')
        val phan = dau.split('|')
        if (phan.size != 4 || phan[0] != TU_KHOA) return null
        val phut = phan[1].toIntOrNull() ?: return null
        val giay = phan[2].toLongOrNull() ?: return null
        if (phut !in 1..240) return null
        return Lenh(phut = phut, guiLuc = giay, dau = dau)
    }

    private fun homNay(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun sp(context: Context) = Prefs.get(context).raw()

    const val TU_KHOA = "CHOGIO"

    /**
     * Lenh cu hon khoang nay thi bo.
     *
     * Cung y nhu LENH_QUA_CU_MS ben ApprovalService: tablet mat mang ca buoi toi
     * roi sang hom sau vua len mang la doc duoc lenh ba bam tu hom qua, tu dung mo
     * gio choi vao sang som ma khong ai bam gi.
     */
    const val QUA_CU_MS = 30 * 60_000L

    private const val SOM_NHAT = 6 * 60

    /**
     * Ngo them vai phut sau gio chot.
     *
     * Ba bam luc 21:59 thi tablet doc duoc lenh do som nhat la mot phut sau, tuc la
     * da qua 22:00. Khong co khoang an han nay thi lenh do khong ai doc, nam do cho
     * den luc qua cu roi bi bo - ba khong biet, Ba Huy cung khong biet. Doc duoc thi
     * capGio tu choi va nhan tu choi do di thang vao Telegram cho Ba Huy thay.
     */
    private const val AN_HAN = 5

    private const val K_NHOM = "hopthu_nhom"
    private const val K_DA_DOC = "hopthu_da_doc"
    private const val K_NGAY = "hopthu_ngay_da_cho"
}
