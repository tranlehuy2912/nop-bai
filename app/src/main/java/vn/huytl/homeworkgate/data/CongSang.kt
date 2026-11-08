package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.guard.MocGio
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.TelegramClient
import java.util.Calendar
import kotlin.concurrent.thread

/**
 * Gio choi cham xong trong gio ngu, giu lai de cong luc het gio ngu.
 *
 * VI SAO CO. Ba Huy hay nho Claude cham sau khi Le Hoa da di ngu, ma trong gio ngu
 * GateStore khong cap phut nao. Truoc 27/9/2026 ket qua dan luc do chi duoc cau "khong
 * cap duoc", bai nam cho toi nua dem roi bi bo, va con mat gio cua bai da lam. Ngay
 * 27/9/2026 Ba Huy chon giu so phut lai, het gio ngu thi cong.
 *
 * Chi hai duong cua Ba Huy di qua day: dan ket qua Claude cham (lenh CHAMBAI) va Claude
 * cham lai (lenh SUACHAM). Con tu nop bai trong gio ngu thi may cham van khong cong gio,
 * nut Duyet cung vay.
 *
 * Tu 30/10/2026 them nut Cho cua Bang dieu khien (lenh CHO, anh Huy chot): toi 29/10/2026
 * Ba Huy cho 20 phut viec nha luc da qua gio ngu, tablet tra loi "Không cấp được", sang
 * hom sau Ba Huy phai day som bam lai. Phan nay ghi [Muc.baCho] va cong khong tinh vao so
 * phut doi bang hoc cua ngay, y nhu lenh CHO luc thuc.
 *
 * So cai ghi ngay luc cham, khong doi toi luc cong: sang som con chup lai dung bai do thi
 * may thay cau da tra gio va khong tinh lan hai.
 *
 * Cong luc nao: bao thuc cua [MocGio] luc het gio ngu, va nhip xet cua ApprovalService
 * luc man hinh sang, phong khi bao thuc tre hay may tat qua dem. Phan giu an vao tran
 * phut cua ngay duoc cong, khong phai ngay cham.
 *
 * File prefs rieng: ghi o day khong keo theo mot luot day trang thai len Firestore. Tu
 * 8/11/2026 phan giu len hop/trangthai qua [banGiu], nen [luu] tu nhac DongBo day.
 */
object CongSang {

    private const val FILE = "cong_sang"
    private const val K_CAC = "cac"

    /**
     * Mot phan gio dang giu. [baiId] rong la phan khong gan voi bai nao, vi du cau cham lai.
     * [baCho] la phut Ba Huy bam Cho trong gio ngu, khong phai phut doi bang bai.
     */
    data class Muc(val phut: Int, val baiId: String, val luc: Long, val baCho: Boolean = false)

    fun cacMuc(context: Context): List<Muc> {
        val chu = sp(context).getString(K_CAC, null) ?: return emptyList()
        return runCatching {
            val mang = JSONArray(chu)
            (0 until mang.length()).map { i ->
                val o = mang.getJSONObject(i)
                Muc(o.getInt("p"), o.optString("b"), o.optLong("l"), o.optBoolean("c"))
            }
        }.getOrDefault(emptyList())
    }

    /** Giu [phut] phut toi luc het gio ngu. Dat lai bao thuc de no thuc day dung luc do. */
    fun them(
        context: Context,
        phut: Int,
        baiId: String = "",
        now: Long = System.currentTimeMillis(),
        baCho: Boolean = false
    ) {
        if (phut <= 0) return
        synchronized(this) { luu(context, cacMuc(context) + Muc(phut, baiId, now, baCho)) }
        MocGio.datLai(context)
    }

    /** Luc se cong: lan het gio ngu ke tiep. Dang khong trong gio ngu thi la [now]. */
    fun lucCong(context: Context, now: Long = System.currentTimeMillis()): Long {
        if (!GateStore(context).trongGioNgu(now)) return now
        val day = Prefs.get(context).gioDayMinuteOfDay
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, day / 60)
            set(Calendar.MINUTE, day % 60)
            // Muon vai giay, y nhu MocGio: bao thuc den dung phut dau thi con nam trong gio ngu.
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** Gio het gio ngu, dang "06:00", de ghi vao tin. */
    fun gioCong(context: Context): String {
        val day = Prefs.get(context).gioDayMinuteOfDay
        return "%02d:%02d".format(day / 60, day % 60)
    }

    /**
     * Ban tom tat phan dang giu, cho truong [vn.huytl.homeworkgate.dongbo.Duong.F_GIU_SANG]
     * cua hop/trangthai (8/11/2026, anh Huy chot): the Gio choi ben Bang dieu khien hien
     * "Giữ 30 phút Ba Huy cho, 07:00 tablet cộng". Truoc ngay do phan giu khong len Firestore,
     * dien thoai chi thay cau tra loi lenh mot lan roi thoi.
     *
     * "luc" chi lay [lucCong] khi dang trong gio ngu: suot dem ham do tra cung mot moc. Ngoai
     * gio ngu no tra ve chinh [now], so doi moi lan goi, ma DongBo bo luot ghi bang cach so ban
     * moi voi ban vua day: so doi lien tuc thi lan nao cung ghi. Luc do gui 0, nghia la tablet
     * se cong o lan xet sau.
     */
    fun banGiu(context: Context, now: Long = System.currentTimeMillis()): Map<String, Any> {
        val cac = cacMuc(context)
        val cho = cac.filter { it.baCho }.sumOf { it.phut }
        val bai = cac.filterNot { it.baCho }.sumOf { it.phut }
        val luc = if (cho + bai > 0 && GateStore(context).trongGioNgu(now)) lucCong(context, now) else 0L
        return mapOf("cho" to cho, "bai" to bai, "luc" to luc)
    }

    /**
     * Moc bao thuc cho [MocGio]: luc het gio ngu, neu dang giu phut nao. null la khong can.
     *
     * Chi tra moc trong tuong lai. Het gio ngu ma cong chua duoc thi de nhip xet luc man hinh
     * sang lo, chu dat bao thuc ngay bay gio thi bao thuc do goi lai chinh no mai.
     */
    fun mocBaoThuc(context: Context, now: Long = System.currentTimeMillis()): Long? {
        if (cacMuc(context).isEmpty()) return null
        return lucCong(context, now).takeIf { it > now }
    }

    /**
     * Het gio ngu roi thi cong het phan dang giu, mot lan, an vao tran ngay.
     *
     * Tran cat bot thi bai giu sau duoc phan con lai, va trang thai tung bai tren dien thoai
     * ghi dung so phut bai do duoc. Tran het han thi bo phan giu: gio cua bai lam chi co
     * nghia trong ngay duoc cong.
     *
     * Phut bai cham va phut Ba Huy cho ([Muc.baCho]) cap thanh hai lan: phut bai ghi vao so
     * phut doi bang hoc cua ngay (useQuota), phut Ba Huy cho thi khong, y nhu lenh CHO luc
     * thuc. Lan dau cap duoc ma lan sau hong thi chi giu lai phan chua cap, lan xet sau cong
     * tiep phan do.
     *
     * @return cau bao Ba Huy, ben goi gui len Telegram. null la chua den luc hay khong giu gi.
     */
    @Synchronized
    fun congNeuDenLuc(context: Context, now: Long = System.currentTimeMillis()): String? {
        val cac = cacMuc(context)
        if (cac.isEmpty()) return null
        val gate = GateStore(context)
        if (gate.trongGioNgu(now)) return null
        // Khong con tran ngay (29/9/2026): phut giu da qua tran rieng cua bai dan do luc
        // cham, sang ra cong du.
        val bai = cac.filterNot { it.baCho }
        val cho = cac.filter { it.baCho }
        val phutBai = bai.sumOf { it.phut }
        val phutCho = cho.sumOf { it.phut }
        if (phutBai + phutCho <= 0) {
            luu(context, emptyList())
            return null
        }
        // Khong cap duoc vi mot ly do khac (vua doi trang thai giua chung): giu lai, lan xet
        // sau thu tiep.
        if (phutBai > 0) {
            if (cap(gate, phutBai, useQuota = true, now = now) == null) return null
            luu(context, cho)
            bai.forEach { m ->
                if (m.baiId.isNotEmpty()) DongBo.datTrangThaiBai(context, m.baiId, "DUYET", m.phut)
            }
        }
        val daCho = phutCho > 0 && cap(gate, phutCho, useQuota = false, now = now) != null
        if (daCho) luu(context, emptyList())
        if (phutBai <= 0 && !daCho) return null

        val ba = context.getString(R.string.parent_name_cap)
        if (phutBai > 0) {
            DayLog.add(context, "Hết giờ ngủ, cộng $phutBai phút giữ từ lúc $ba chấm trong giờ ngủ")
        }
        if (daCho) DayLog.add(context, "Hết giờ ngủ, cộng $phutCho phút $ba cho trong giờ ngủ")
        val choCon = listOfNotNull(
            "$phutBai phút của bài $ba chấm trong giờ ngủ".takeIf { phutBai > 0 },
            "$phutCho phút $ba cho trong giờ ngủ".takeIf { daCho }
        )
        SoCaiBai.datLoiNhan(context, "Được thêm ${choCon.joinToString(" và ")}.", tinVui = true)
        ApprovalService.ensureRunning(context)
        val choBa = listOfNotNull(
            "$phutBai phút giữ từ lúc chấm trong giờ ngủ".takeIf { phutBai > 0 },
            "$phutCho phút anh cho trong giờ ngủ".takeIf { daCho }
        )
        return "⏰ Hết giờ ngủ: đã cộng ${choBa.joinToString(" và ")}."
    }

    /** Cong vao phien dang chay, hay cap phieu (cong don vao phieu dang giu). */
    private fun cap(gate: GateStore, phut: Int, useQuota: Boolean, now: Long): Int? =
        if (gate.state == GateState.ACTIVE) gate.extend(phut, now = now, useQuota = useQuota)
        else gate.approve(now = now, wantedMinutes = phut, useQuota = useQuota)

    /** Gui cau cua [congNeuDenLuc] len Telegram. Chay o luong nen: ben goi co khi o luong chinh. */
    fun baoBaHuy(context: Context, chu: String) {
        val prefs = Prefs.get(context)
        if (prefs.botToken.isBlank() || prefs.parentChatId == 0L) return
        thread(isDaemon = true) {
            runCatching { TelegramClient(prefs.botToken).sendMessage(prefs.parentChatId, chu) }
        }
    }

    /** Xoa het phan dang giu. Chi de test don dep. */
    internal fun xoaHet(context: Context) = luu(context, emptyList())

    private fun luu(context: Context, cac: List<Muc>) {
        val mang = JSONArray()
        cac.forEach {
            val o = JSONObject().put("p", it.phut).put("b", it.baiId).put("l", it.luc)
            if (it.baCho) o.put("c", true)
            mang.put(o)
        }
        sp(context).edit().putString(K_CAC, mang.toString()).commit()
        // File nay nam ngoai file prefs chinh nen DongBo khong tu nghe thay: phai nhac no day
        // trang thai, khong thi [banGiu] chi len Firestore khi co khoa khac doi.
        DongBo.day()
    }

    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
