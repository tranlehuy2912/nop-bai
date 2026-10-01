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
 * So cai ghi ngay luc cham, khong doi toi luc cong: sang som con chup lai dung bai do thi
 * may thay cau da tra gio va khong tinh lan hai.
 *
 * Cong luc nao: bao thuc cua [MocGio] luc het gio ngu, va nhip xet cua ApprovalService
 * luc man hinh sang, phong khi bao thuc tre hay may tat qua dem. Phan giu an vao tran
 * phut cua ngay duoc cong, khong phai ngay cham.
 *
 * File prefs rieng: ghi o day khong keo theo mot luot day trang thai len Firestore.
 */
object CongSang {

    private const val FILE = "cong_sang"
    private const val K_CAC = "cac"

    /** Mot phan gio dang giu. [baiId] rong la phan khong gan voi bai nao, vi du cau cham lai. */
    data class Muc(val phut: Int, val baiId: String, val luc: Long)

    fun cacMuc(context: Context): List<Muc> {
        val chu = sp(context).getString(K_CAC, null) ?: return emptyList()
        return runCatching {
            val mang = JSONArray(chu)
            (0 until mang.length()).map { i ->
                val o = mang.getJSONObject(i)
                Muc(o.getInt("p"), o.optString("b"), o.optLong("l"))
            }
        }.getOrDefault(emptyList())
    }

    /** Giu [phut] phut toi luc het gio ngu. Dat lai bao thuc de no thuc day dung luc do. */
    fun them(context: Context, phut: Int, baiId: String = "", now: Long = System.currentTimeMillis()) {
        if (phut <= 0) return
        synchronized(this) { luu(context, cacMuc(context) + Muc(phut, baiId, now)) }
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
     * @return cau bao Ba Huy, ben goi gui len Telegram. null la chua den luc hay khong giu gi.
     */
    @Synchronized
    fun congNeuDenLuc(context: Context, now: Long = System.currentTimeMillis()): String? {
        val cac = cacMuc(context)
        if (cac.isEmpty()) return null
        val gate = GateStore(context)
        if (gate.trongGioNgu(now)) return null
        val xin = cac.sumOf { it.phut }
        // Khong con tran ngay (29/9/2026): phut giu da qua tran rieng cua bai dan do luc
        // cham, sang ra cong du.
        val them = xin
        if (them <= 0) {
            luu(context, emptyList())
            return null
        }
        val duoc = if (gate.state == GateState.ACTIVE) {
            gate.extend(them, now = now, useQuota = true)
        } else {
            gate.approve(now = now, wantedMinutes = them, useQuota = true)
        }
        // Khong cap duoc vi mot ly do khac (vua doi trang thai giua chung): giu lai, lan xet
        // sau thu tiep.
        if (duoc == null) return null
        luu(context, emptyList())

        var con = them
        cac.forEach { m ->
            val phan = minOf(m.phut, con)
            con -= phan
            if (m.baiId.isNotEmpty()) DongBo.datTrangThaiBai(context, m.baiId, "DUYET", phan)
        }
        val ba = context.getString(R.string.parent_name_cap)
        DayLog.add(context, "Hết giờ ngủ, cộng $them phút giữ từ lúc $ba chấm trong giờ ngủ")
        SoCaiBai.datLoiNhan(
            context, "Được thêm $them phút của bài $ba chấm trong giờ ngủ.", tinVui = true
        )
        ApprovalService.ensureRunning(context)
        return "⏰ Hết giờ ngủ: đã cộng $them phút giữ từ lúc chấm trong giờ ngủ" +
            (if (them < xin) " (hôm nay chỉ còn $them phút trong hạn mức)" else "") + "."
    }

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
        cac.forEach { mang.put(JSONObject().put("p", it.phut).put("b", it.baiId).put("l", it.luc)) }
        sp(context).edit().putString(K_CAC, mang.toString()).commit()
    }

    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
