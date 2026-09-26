package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONObject

/**
 * Ban vo dan do ma tung lan nop dung, giu lai cho den luc Ba Huy cham bang Claude.
 *
 * VI SAO CAN. May cham ngay luc con nop, nen doc thang [VoDanDo.conHieuLuc] la dung
 * ban cua luc do. Claude thi cham luc Ba Huy dan ket qua, co khi tre vai tieng: trong
 * luc do con co the soat lai vo, chup trang khac, hay vo da het han. Doc ban trong may
 * luc cham la cham bai nay theo danh sach bai cua mot luc khac.
 *
 * Tablet dung ban nay truoc ban tren Firestore. Ngay va danh sach bai quyet tron goi 45
 * phut, nen lay tu cho tablet tu ghi, khong lay tu cho qua tay dien thoai roi quay ve.
 *
 * Chi giu chu, khong giu duong dan anh: tam anh thuoc ve [VoDanDo] va di theo no luc
 * het han. Giu hai ngay la du, vi bai cho duyet khong song qua nua dem - xem
 * GateStore.donDepBaiCho. Cung mot kieu voi [KhaiChoCham]: moi bai mot khoa, don ban cu
 * moi lan ghi.
 */
object VoChoCham {

    private const val FILE = "vo_cho_cham"
    private const val GIU_MS = 2 * 24 * 60 * 60_000L

    fun luu(
        context: Context,
        baiId: String,
        vo: VoDanDo.DanDo,
        now: Long = System.currentTimeMillis()
    ) {
        val sp = sp(context)
        val sua = sp.edit()
        sp.all.forEach { (khoa, giaTri) ->
            val luc = runCatching { JSONObject(giaTri as String).optLong("luc") }.getOrDefault(0L)
            if (now - luc > GIU_MS) sua.remove(khoa)
        }
        val ban = VoDanDo.sangJson(vo.copy(anh = null))
        sua.putString(baiId, JSONObject().put("luc", now).put("vo", ban).toString())
        sua.apply()
    }

    fun lay(context: Context, baiId: String): VoDanDo.DanDo? {
        val chu = sp(context).getString(baiId, null) ?: return null
        return runCatching { VoDanDo.tuJson(JSONObject(chu).getJSONObject("vo")) }.getOrNull()
    }

    /**
     * Ban vo ma lan cham nay dung cho bai [baiId].
     *
     * Thuong la ban chep luc nop. Rieng khi ban do chi co anh ([VoDanDo.DanDo.chuaDoc])
     * ma tu luc do chinh tam anh ay da duoc doc ra chu - Ba Huy nho Claude doc, hay mot
     * lan cham truoc da doc - thi lay ban da doc. Khong thi hai bai cung mot trang vo
     * lai cham theo hai lan doc khac nhau.
     */
    fun voChoBai(context: Context, baiId: String): VoDanDo.DanDo? {
        val ban = lay(context, baiId) ?: return null
        if (!ban.chuaDoc) return ban
        val hienTai = VoDanDo.doc(context) ?: return ban
        return if (!hienTai.chuaDoc && hienTai.chupLuc == ban.chupLuc) hienTai.copy(anh = null) else ban
    }

    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
