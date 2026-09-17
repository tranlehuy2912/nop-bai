package vn.huytl.homeworkgate.ai

import org.json.JSONObject
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.LoaiLoi
import vn.huytl.homeworkgate.kho.CauHoi

/**
 * Doc cau tra loi cua AI thanh [KetQuaCham].
 *
 * Tach rieng khoi phan goi mang de bo test chay duoc voi cau tra loi THAT da luu
 * lai, khong can khoa API va khong can doi Google. Phan de vo nhat cua duong nay
 * khong phai la HTTP ma la cai JSON: model thinh thoang tra ve thieu truong, ghi
 * "dang" bang chu thuong, boc JSON trong ```json ... ```, hay them mot cau chao o
 * dau du da dan la chi tra JSON.
 *
 * Nguyen tac khi doc: thieu gi thi lay mac dinh AN TOAN, dung doan. Thieu "dung"
 * thi coi nhu sai (con nop lai duoc, khong mat gi); thieu "doc_ro" thi coi nhu
 * khong doc ro (Ba Huy nhin mot cai, khong tu duyet).
 */
object ChamBaiJson {

    /**
     * @param danhSach cac cau con da khai truoc khi chup, theo ma. Rong nghia la
     *   lan nop tu do - may tu tach cau nhu truoc.
     */
    fun doc(tra: String?, danhSach: List<CauHoi> = emptyList()): KetQuaCham? {
        val than = locJson(tra) ?: return null
        val o = runCatching { JSONObject(than) }.getOrNull() ?: return null

        // Doi chieu bang ma da chuan hoa: may hay chep "2.26 a" hay "Bài 2.26a" cho
        // cai ma sach in la "2.26a". Bo het dau cach va dau cham thi ba cai do la mot.
        val theoMa = danhSach.associateBy { chuanHoaMa(it.ma) }

        val mang = o.optJSONArray("cac_cau")
        val cac = (0 until (mang?.length() ?: 0)).mapNotNull { i ->
            val c = mang?.optJSONObject(i) ?: return@mapNotNull null
            val ma = c.optString("ma").ifBlank { "câu ${i + 1}" }
            val trongSach = if (c.optBoolean("ngoai_danh_sach", false)) null
            else theoMa[chuanHoaMa(ma)]

            // Con khai lam ma anh khong co bai: bo han, dung ghi vao so. Ghi vao thi
            // cau do thanh "dang cho sua" du con chua dinh vao no bao gio, va man hinh
            // cua con day nhung cau no khong lam.
            if (trongSach != null && !c.optBoolean("co_lam", true)) return@mapNotNull null

            val baiLam = docDanhSach(c.optJSONArray("bai_lam"))

            CauCham(
                // Ma va de lay tu SACH khi cau nam trong danh sach: sach moi la ban
                // that, con chu may doc duoc chi la ban chep lai.
                ma = trongSach?.ma ?: ma,
                de = trongSach?.de ?: c.optString("de"),
                cauId = trongSach?.id,
                ketQua = c.optString("ket_qua"),
                // Thieu "dung" thi coi nhu chua dung: con sua lai va nop tiep duoc,
                // con cong nham gio cho bai sai thi khong lay lai duoc cai uy tin.
                dung = c.optBoolean("dung", false),
                docRo = c.optBoolean("doc_ro", false),
                dang = docDang(c.optString("dang"), trongSach),
                trongDanDo = c.optBoolean("trong_dan_do", false),
                // So dong lay theo DANH SACH DONG may chep ra, khong lay con so may
                // tu khai. Hai cai le ra bang nhau, nhung so dong la thu quy ra phut,
                // ma mot con so khong ai soat duoc thi de phinh; mot danh sach dong
                // thi Ba Huy doc la thay ngay no co dung bay nhieu dong that khong.
                soDong = (baiLam.size.takeIf { it > 0 } ?: c.optInt("so_dong", 0))
                    .coerceIn(0, 500),
                baiLam = baiLam,
                dongSai = c.optInt("dong_sai", 0).coerceIn(0, baiLam.size),
                nhanXet = c.optString("nhan_xet"),
                // Nhan la nhung chu in hoa may chep lai tu cau lenh. Loc qua mot
                // lan: may tra ve nhan la thi coi nhu KHAC, con hon de mot ten
                // lac vao bang thong ke roi nam do mot minh mot dong.
                loaiLoi = LoaiLoi.doc(c.optString("loai_loi"), c.optBoolean("dung", false)),
                // Cau trong danh sach con khai thi de lay tu sach, luon co de. Chi
                // cau tu do moi phai hoi may xem no co nhin thay de khong.
                coDe = trongSach != null || c.optBoolean("co_de", false),
                coLam = c.optBoolean("co_lam", true),
                // -1 khi may khong tra loi. Chi lan on tap moi hoi den mau muc,
                // nen lan thuong luon la -1 va khong ai xet den.
                mucDo = when {
                    !c.has("muc_do") -> -1
                    c.optBoolean("muc_do") -> 1
                    else -> 0
                }
            )
        }
        if (cac.isEmpty()) return null

        val mon = o.optString("mon")
        return KetQuaCham(
            mon = mon,
            cac = cac.map { if (it.mon.isBlank()) it.copy(mon = mon) else it },
            ngayDanDo = o.optString("ngay_dan_do").takeIf { it.isNotBlank() && it != "null" },
            lamHetDanDo = o.optBoolean("lam_het_dan_do", false),
            baiDuocGiao = docDanhSach(o.optJSONArray("bai_duoc_giao")),
            tomTat = o.optString("tom_tat")
        )
    }

    /**
     * Dang bai: lay cua sach neu sach co ghi, khong thi tin may.
     *
     * Sach biet chac hon: mot cau trac nghiem trong sach thi nam nao cung la trac
     * nghiem, con may thi cung mot cau ba lan chay co the ra ba dang khac nhau - ma
     * dang quyet dinh so phut.
     */
    private fun docDang(chu: String?, trongSach: CauHoi?): DangBai {
        trongSach?.dang?.takeIf { it.isNotBlank() }?.let { cuaSach ->
            runCatching { return DangBai.valueOf(cuaSach.trim().uppercase()) }
        }
        return runCatching { DangBai.valueOf(chu.orEmpty().trim().uppercase()) }
            .getOrDefault(DangBai.CAU_NHO)
    }

    /** Bo dau cach, dau cham, chu hoa: "Bài 2.26 a" va "2.26a" la mot. */
    fun chuanHoaMa(ma: String?): String =
        ma.orEmpty().lowercase().filter { it.isLetterOrDigit() }

    /**
     * Lay phan JSON ra khoi cau tra loi.
     *
     * Da dan "chi tra ve JSON" va da dat responseMimeType, nhung van co luc model
     * boc no trong ```json hoac them mot cau dan. Cat tu dau ngoac nhon dau tien
     * den dau ngoac nhon cuoi cung.
     */
    private fun locJson(tra: String?): String? {
        if (tra.isNullOrBlank()) return null
        val dau = tra.indexOf('{')
        val cuoi = tra.lastIndexOf('}')
        if (dau < 0 || cuoi <= dau) return null
        return tra.substring(dau, cuoi + 1)
    }

    /** Doc mot mang chuoi, bo cac o rong. Thieu mang thi la danh sach rong. */
    private fun docDanhSach(a: org.json.JSONArray?): List<String> {
        if (a == null) return emptyList()
        return (0 until a.length())
            .mapNotNull { a.optString(it).trim().takeIf { t -> t.isNotEmpty() && t != "null" } }
    }
}
