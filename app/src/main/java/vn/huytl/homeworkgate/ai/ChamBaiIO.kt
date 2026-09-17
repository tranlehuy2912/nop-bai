package vn.huytl.homeworkgate.ai

import org.json.JSONArray
import org.json.JSONObject
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.KetQuaCham

/**
 * Goi mot ban cham lai thanh chu de gui qua Intent, va mo ra o dau kia.
 *
 * VI SAO CAN. Tu ban co man soat bai, hai viec truoc day lam lien nhau bi tach doi:
 * CHAM thi xay ra trong man hinh cua con (con phai ngoi xem ket qua doc va sua),
 * con CAP GIO - gui Telegram, ghi so, day sang dien thoai Ba Huy - thi van phai
 * nam trong service, vi no phai chay xong du con co dong man hinh lai.
 *
 * Giua hai noi do chi co Intent, ma Intent thi khong mang duoc doi tuong Kotlin.
 * KHONG dung Serializable/Parcelable: ban cham nay con di tiep xuong so va sang
 * Firestore duoi dang chu, nen co san mot ban chu la tien ca duong sau.
 *
 * KHAC [ChamBaiJson]: ben do doc chu cua AI - dinh dang cua Google, thieu truong
 * la chuyen thuong, doc phai thu than. Ben nay doc chu do CHINH APP viet ra, nen
 * duoc phep tin hon; nhung van khong nem loi, vi mot ban cham hong thi tha mat
 * lan nop con hon ket ca app.
 */
object ChamBaiIO {

    fun viet(ket: KetQuaCham): String = JSONObject()
        .put("mon", ket.mon)
        .put("ngay_dan_do", ket.ngayDanDo)
        .put("lam_het_dan_do", ket.lamHetDanDo)
        .put("bai_duoc_giao", JSONArray(ket.baiDuocGiao))
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
                            .put("trong_dan_do", c.trongDanDo)
                            .put("so_dong", c.soDong)
                            .put("muc_do", c.mucDo)
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
                trongDanDo = c.optBoolean("trong_dan_do"),
                soDong = c.optInt("so_dong"),
                mucDo = c.optInt("muc_do", -1),
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
            ngayDanDo = o.optString("ngay_dan_do").takeIf { it.isNotBlank() && it != "null" },
            lamHetDanDo = o.optBoolean("lam_het_dan_do"),
            baiDuocGiao = docDong(o.optJSONArray("bai_duoc_giao")),
            tomTat = o.optString("tom_tat")
        )
    }

    /**
     * Doc mang chuoi, GIU ca o rong.
     *
     * Khac cho doc chu cua AI: o do o rong la rac nen bo di. O day mot dong rong
     * co the la dong con vua xoa het chu trong man soat, ma bo di thi cac dong sau
     * tut len mot bac - va so dong thi quy ra phut.
     */
    private fun docDong(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { a.optString(it) }
    }
}
