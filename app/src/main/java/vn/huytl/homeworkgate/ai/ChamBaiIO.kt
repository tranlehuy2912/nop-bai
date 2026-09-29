package vn.huytl.homeworkgate.ai

import org.json.JSONArray
import org.json.JSONObject
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.KetQuaCham

/**
 * Goi mot ban cham lai thanh chu de gui qua Intent, va mo ra o dau kia.
 *
 * VI SAO CAN. Ban cham cua Claude toi tablet qua lenh CHAMBAI (ThiHanhLenh, chay trong
 * listener Firestore), con CAP GIO - gui Telegram, ghi so, day sang dien thoai Ba Huy -
 * thi nam trong service, vi no phai chay xong du tien trinh nghe lenh co bi dung giua
 * chung. Giua hai noi do chi co Intent, ma Intent thi khong mang duoc doi tuong Kotlin.
 *
 * Truoc 28/9/2026 lop nay con chuyen ban may tren tablet cham, tu man soat bai sang
 * service. Phan may cham da bo, lop nay giu cho duong Claude.
 *
 * KHONG dung Serializable/Parcelable: ban cham nay con di tiep xuong so va sang
 * Firestore duoi dang chu, nen co san mot ban chu la tien ca duong sau. Chu o day do
 * CHINH APP viet ra nen duoc phep tin; nhung van khong nem loi, vi mot ban cham hong
 * thi tha mat lan cham con hon ket ca app.
 */
object ChamBaiIO {

    fun viet(ket: KetQuaCham): String = JSONObject()
        .put("mon", ket.mon)
        .put("tom_tat", ket.tomTat)
        .put(
            "cac_cau",
            JSONArray().apply {
                ket.cac.forEach { c ->
                    put(
                        JSONObject()
                            .put("ma", c.ma)
                            .put("de", c.de)
                            .put("cau_id", c.cauId)
                            .put("mon", c.mon)
                            .put("ket_qua", c.ketQua)
                            .put("bai_lam", JSONArray(c.baiLam))
                            .put("dong_sai", c.dongSai)
                            .put("dung", c.dung)
                            .put("doc_ro", c.docRo)
                            .put("dang", c.dang.name)
                            .put("so_dong", c.soDong)
                            .put("nhan_xet", c.nhanXet)
                            .put("loai_loi", c.loaiLoi)
                            .put("co_de", c.coDe)
                            .put("co_lam", c.coLam)
                    )
                }
            }
        )
        .toString()

    fun doc(chu: String?): KetQuaCham? {
        if (chu.isNullOrBlank()) return null
        val o = runCatching { JSONObject(chu) }.getOrNull() ?: return null
        val mang = o.optJSONArray("cac_cau") ?: return null
        val cac = (0 until mang.length()).mapNotNull { i ->
            val c = mang.optJSONObject(i) ?: return@mapNotNull null
            CauCham(
                ma = c.optString("ma"),
                de = c.optString("de"),
                cauId = c.optString("cau_id").takeIf { it.isNotBlank() && it != "null" },
                mon = c.optString("mon"),
                ketQua = c.optString("ket_qua"),
                baiLam = docDong(c.optJSONArray("bai_lam")),
                dongSai = c.optInt("dong_sai"),
                dung = c.optBoolean("dung"),
                docRo = c.optBoolean("doc_ro"),
                dang = runCatching { DangBai.valueOf(c.optString("dang")) }
                    .getOrDefault(DangBai.CAU_NHO),
                soDong = c.optInt("so_dong"),
                nhanXet = c.optString("nhan_xet"),
                loaiLoi = c.optString("loai_loi"),
                coDe = c.optBoolean("co_de", true),
                coLam = c.optBoolean("co_lam", true)
            )
        }
        if (cac.isEmpty()) return null
        return KetQuaCham(
            mon = o.optString("mon"),
            cac = cac,
            tomTat = o.optString("tom_tat")
        )
    }

    /**
     * Doc mang chuoi, GIU ca o rong.
     *
     * Mot dong rong van la mot dong cua bai lam: bo di thi cac dong sau tut len mot bac,
     * va so thu tu dong sai lech theo.
     */
    private fun docDong(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { a.optString(it) }
    }
}
