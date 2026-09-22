package vn.huytl.homeworkgate.kho

import android.content.Context
import android.util.Log
import org.json.JSONObject
import vn.huytl.homeworkgate.data.Prefs

/**
 * Cac bo tu vung da nap san trong app.
 *
 * Di song song voi [NganHang] va [BoThe], va co y giong het chung o cach lam viec:
 * file JSON trong assets, moi file mot so [ban], so ban doi thi nap lai ca bo. Ba
 * duong cung mot kieu thi nguoi sua khong phai hoc ba cach.
 *
 * FILE NAY DO SCRIPT SINH RA chu khong go tay - xem tools/doc-tu-vung.py. Bang
 * GLOSSARY cuoi sach Tieng Anh 8 co khoang ba tram dong, va script doc hai luot doc
 * lap roi bao ra dung nhung dong hai luot khac nhau de nguoi chi phai soat may dong
 * do. Nhung SOAT VAN LA VIEC CUA NGUOI: hai luot giong nhau chua co nghia la dung.
 *
 * THEM MOT BO MOI: chay script ra file, soat, bo vao assets/tuvung/, them mot dong
 * vao [BO]. Ma bo di thang vao [TuVung.id] nen KHONG duoc doi ve sau - doi la toan
 * bo lich su cua cac tu cu tro thanh vo nghia.
 */
object BoTuVung {

    data class Bo(
        val bo: String,
        val mon: String,
        val ten: String,
        val file: String
    )

    val BO = listOf(
        Bo(
            bo = "anh8",
            mon = "Tiếng Anh",
            ten = "Từ vựng Tiếng Anh 8",
            file = "tuvung/anh8.json"
        )
    )

    fun theoMa(bo: String): Bo? = BO.firstOrNull { it.bo == bo }

    /** Nap cac bo chua co hoac da cu. Goi luc app khoi dong. */
    fun napNeuCan(context: Context) {
        val kho = KhoBai.get(context)
        val sp = Prefs.get(context).raw()
        BO.forEach { bo ->
            val doc = runCatching { doc(context, bo) }.getOrElse { e ->
                Log.w(TAG, "khong doc duoc ${bo.file}: ${e.message}")
                return@forEach
            } ?: return@forEach

            val khoaBan = "botuvung_ban_${bo.bo}"
            if (sp.getInt(khoaBan, 0) == doc.ban && kho.soTuCua(bo.bo) > 0) return@forEach
            kho.napBoTu(bo.bo, doc.cac)
            sp.edit().putInt(khoaBan, doc.ban).apply()
            Log.i(TAG, "nap ${doc.cac.size} tu tu ${bo.ten} (ban ${doc.ban})")
        }
    }

    /**
     * Ma cua mot tu, di vao [TuVung.id].
     *
     * Lay chinh tu do, ha chu thuong, doi dau cach thanh gach ngang: "on sale" ra
     * "anh8:on-sale". Doc duoc bang mat nen luc do file JSON va bang trong may doi
     * chieu duoc voi nhau, khac han mot ma dem so.
     *
     * Hai tu khac nhau ma ra cung ma thi script da bao truoc khi ghi file, va o day
     * ban sau de len ban truoc - mot dong trong bang, khong phai hai dong nua voi.
     */
    fun maCua(tu: String): String =
        tu.trim().lowercase().replace(Regex("\\s+"), "-")

    private class Quyen(val ban: Int, val cac: List<TuVung>)

    /**
     * Doc mot bo tu assets. Tu choi han chu khong nap mot nua, ba cho y het [NganHang].
     */
    private fun doc(context: Context, bo: Bo): Quyen? {
        val chu = context.assets.open(bo.file).bufferedReader().use { it.readText() }
        val o = JSONObject(chu)

        val ban = o.optInt("ban", 0)
        if (ban < 1) {
            Log.w(TAG, "${bo.file} thieu \"ban\" - khong nap")
            return null
        }
        val trongFile = o.optString("bo")
        if (trongFile != bo.bo) {
            Log.w(TAG, "${bo.file} ghi bo \"$trongFile\" ma dang nap cho \"${bo.bo}\" - khong nap")
            return null
        }

        val mang = o.optJSONArray("cac_tu") ?: return null
        val cac = buildList {
            for (i in 0 until mang.length()) {
                val t = mang.optJSONObject(i) ?: continue
                val tu = t.optString("tu").trim()
                val nghia = t.optString("nghia").trim()
                // Thieu mot trong hai thi khong hoi duoc chieu nao ca: chieu Anh sang
                // Viet can nghia, chieu Viet sang Anh can tu.
                if (tu.isEmpty() || nghia.isEmpty()) continue
                add(
                    TuVung(
                        id = "${bo.bo}:${maCua(tu)}",
                        bo = bo.bo,
                        mon = bo.mon,
                        unit = t.optInt("unit", 0),
                        tu = tu,
                        loai = t.optString("loai").trim(),
                        am = t.optString("am").trim(),
                        nghia = nghia,
                        thuTu = size
                    )
                )
            }
        }
        if (cac.isEmpty()) {
            Log.w(TAG, "${bo.file} khong co tu nao - khong nap")
            return null
        }
        return Quyen(ban, cac)
    }

    private const val TAG = "BoTuVung"
}
