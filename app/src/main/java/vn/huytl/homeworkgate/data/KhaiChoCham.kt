package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONObject
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhamVi

/**
 * Pham vi bai con khai cua tung lan nop, giu lai cho den luc Ba Huy cham bang Claude.
 *
 * VI SAO CAN. Man chup dua pham vi sang service, service gui anh xong la het viec cua
 * no. Lan cham xay ra sau do, co khi vai tieng, luc Ba Huy dan ket qua Claude ve (tu
 * 28/9/2026 tablet khong tu cham nua). Luc do van can dung pham vi cu:
 * cau nao thuoc sach nao, co phai lan on tap khong, con khai cau nao chua chac. Thieu
 * no thi ket qua cua Claude khong khop duoc voi ma cau trong so, va cau nao cung bi
 * tinh la bai ngoai sach.
 *
 * Mot file prefs rieng, moi bai mot khoa. Ban nao qua bay ngay thi don di.
 */
object KhaiChoCham {

    private const val FILE = "khai_cho_cham"
    private const val GIU_MS = 7 * 24 * 60 * 60_000L

    fun luu(
        context: Context,
        baiId: String,
        pham: PhamVi,
        now: Long = System.currentTimeMillis()
    ) {
        val sp = sp(context)
        val sua = sp.edit()
        sp.all.forEach { (khoa, giaTri) ->
            val luc = runCatching { JSONObject(giaTri as String).optLong("luc") }.getOrDefault(0L)
            if (now - luc > GIU_MS) sua.remove(khoa)
        }
        sua.putString(baiId, JSONObject().put("luc", now).put("pham", pham.sangJson()).toString())
        sua.apply()
    }

    fun lay(context: Context, baiId: String): PhamVi? {
        val chu = sp(context).getString(baiId, null) ?: return null
        return runCatching { PhamVi.tuJson(JSONObject(chu).optString("pham")) }.getOrNull()
    }

    /**
     * Cac cau (theo khoa cua so cai) dang nam trong mot bai con cho, ma chua co ket qua
     * cham nao ghi sau luc nop bai do.
     *
     * VI SAO CAN (Ba Huy chon ngay 28/9/2026). Bai nop xong nam cho toi luc Ba Huy nho
     * Claude cham, co khi vai tieng. Trong luc do cac cau cua no chua co trong so, nen man
     * chon cau van cho tich lai, va man ket qua van cho nop lai cau sai cua bai truoc: con
     * nop trung, Ba Huy phai cham hai lan. Nhung cau nay bi khoa, ghi "đang chờ chấm".
     *
     * Chua co ket qua ghi SAU luc nop: bai cho ma da cham roi thi cau sai co dong sai moi,
     * cau dung da tra gio co dong dung moi - hai loai do da co cho cua no (can sua, da tinh
     * gio). Con lai la cau chua cham, hay cau dung ma bai con cho Ba Huy duyet.
     */
    fun cauChoCham(context: Context): Set<String> {
        val hang = GateStore(context).baiDangCho()
        if (hang.isEmpty()) return emptySet()
        val theoBai = hang.mapNotNull { b -> lay(context, b.id)?.let { b to khoaCua(it) } }
        val tatCa = theoBai.flatMap { it.second }.toSet()
        if (tatCa.isEmpty()) return emptySet()
        val luc = runCatching { KhoBai.get(context).lucGhiMoiNhat(tatCa) }.getOrDefault(emptyMap())
        return theoBai.flatMap { (b, khoa) -> khoa.filter { (luc[it] ?: 0L) < b.at } }.toSet()
    }

    /** Khoa so cai cua moi cau con khai trong [pham]: ma sach, hay de bai voi cau ngoai sach. */
    fun khoaCua(pham: PhamVi): List<String> =
        pham.cauIds + pham.cauNgoai.mapNotNull { c ->
            SoCaiBai.chuanHoa(c.de).takeIf { it.isNotEmpty() }?.let { SoCaiBai.DAU_NGOAI_SACH + it }
        }

    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
