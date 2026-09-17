package vn.huytl.homeworkgate.kho

import android.content.Context
import android.util.Log
import org.json.JSONObject
import vn.huytl.homeworkgate.data.Prefs

/**
 * Cac bo the hoc thuoc da nap san trong app.
 *
 * Di song song voi [NganHang] va co y giong het no o cach lam viec: file JSON trong
 * assets, moi file mot so [ban], so ban doi thi nap lai. Giong de sua mot cho thi
 * nguoi sua khong phai hoc hai kieu.
 *
 * KHAC NGAN HANG O NOI DUNG. Ben kia la de bai khong co dap an - AI nhin anh roi
 * cham. Ben nay la cau hoi CO dap an co dinh, may cham mot minh bang phep so chuoi,
 * khong goi mang, khong ton khoa AI. Xem [vn.huytl.homeworkgate.data.HocThuoc].
 *
 * THEM MOT BO MOI: them mot file vao assets/hocthuoc/, roi them mot dong vao [BO].
 * Ma bo di thang vao [TheHoc.id] nen KHONG duoc doi ve sau - doi la toan bo lich
 * hen on cua cac the cu tro thanh vo nghia.
 */
object BoThe {

    data class Bo(
        val bo: String,
        val mon: String,
        val ten: String,
        val file: String
    )

    /**
     * Hien moi co cong thuc Toan 8.
     *
     * Bo nay vua la thu dung duoc that, vua la BAN MAU cho cac bo sau: mo file ra la
     * thay du ca bon thu mot the can - ma, cau hoi, dap an, va cac ban viet khac
     * cung tinh dung.
     *
     * Con thieu tu vung tieng Anh va moc Su - do moi la phan hoc thuoc nang nhat cua
     * lop 8. Hai cai do phai chep tu sach that ra nen chua co o day.
     */
    val BO = listOf(
        Bo(
            bo = "toan8ct",
            mon = "Toán",
            ten = "Công thức Toán 8",
            file = "hocthuoc/toan8ct.json"
        )
    )

    fun theoMa(bo: String): Bo? = BO.firstOrNull { it.bo == bo }

    /** Nap cac bo chua co hoac da cu. Goi luc app khoi dong, y het [NganHang.napNeuCan]. */
    fun napNeuCan(context: Context) {
        val kho = KhoBai.get(context)
        val sp = Prefs.get(context).raw()
        BO.forEach { bo ->
            val doc = runCatching { doc(context, bo) }.getOrElse { e ->
                Log.w(TAG, "khong doc duoc ${bo.file}: ${e.message}")
                return@forEach
            } ?: return@forEach

            val khoaBan = "bothe_ban_${bo.bo}"
            if (sp.getInt(khoaBan, 0) == doc.ban && kho.soTheCua(bo.bo) > 0) return@forEach
            kho.napBoThe(bo.bo, doc.cac)
            sp.edit().putInt(khoaBan, doc.ban).apply()
            Log.i(TAG, "nap ${doc.cac.size} the tu ${bo.ten} (ban ${doc.ban})")
        }
    }

    /** Cac bo dang co the den luot, de con chon. Bo nao khong con gi thi van hien. */
    fun bang(context: Context): List<BoDaNap> {
        val kho = KhoBai.get(context)
        return BO.map { bo ->
            BoDaNap(
                bo = bo.bo,
                mon = bo.mon,
                ten = bo.ten,
                soDenLuot = kho.soTheDenLuot(bo.bo),
                tongThe = kho.soTheCua(bo.bo)
            )
        }.filter { it.tongThe > 0 }
    }

    private class Quyen(val ban: Int, val cac: List<TheHoc>)

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

        val cacBai = o.optJSONArray("cac_bai") ?: return null
        var thuTu = 0
        val cac = buildList {
            for (i in 0 until cacBai.length()) {
                val b = cacBai.optJSONObject(i) ?: continue
                val bai = b.optString("bai")
                val cacThe = b.optJSONArray("cac_the") ?: continue
                for (j in 0 until cacThe.length()) {
                    val t = cacThe.optJSONObject(j) ?: continue
                    val ma = t.optString("ma").trim()
                    val hoi = t.optString("hoi").trim()
                    val dap = t.optString("dap").trim()
                    // Thieu mot trong ba thi the do vo nghia: khong co ma thi khong
                    // theo doi duoc lich on, khong co dap an thi khong cham duoc.
                    if (ma.isEmpty() || hoi.isEmpty() || dap.isEmpty()) continue
                    val khac = t.optJSONArray("dap_khac")
                    add(
                        TheHoc(
                            id = "${bo.bo}:$ma",
                            mon = bo.mon,
                            bo = bo.bo,
                            bai = bai,
                            hoi = hoi,
                            dap = dap,
                            dapKhac = (0 until (khac?.length() ?: 0))
                                .mapNotNull { khac?.optString(it)?.trim() }
                                .filter { it.isNotEmpty() },
                            thuTu = thuTu++
                        )
                    )
                }
            }
        }
        if (cac.isEmpty()) {
            Log.w(TAG, "${bo.file} khong co the nao - khong nap")
            return null
        }
        return Quyen(ban, cac)
    }

    private const val TAG = "BoThe"
}
