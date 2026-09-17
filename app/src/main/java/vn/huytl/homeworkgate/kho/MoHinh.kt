package vn.huytl.homeworkgate.kho

import org.json.JSONArray
import org.json.JSONObject

/**
 * Mot cau hoi trong ngan hang, nap tu sach chu khong phai tu anh chup.
 *
 * [id] la thu quan trong nhat trong ca file nay: no la danh tinh co dinh cua mot
 * cau, dang "nguon:ma" - vi du "toan8t1:2.26a". Sach in ra sao thi ma the, nam nay
 * hay sang nam cung the. So cai bam vao chuoi nay de biet cau da tra gio chua, nen
 * no PHAI khong doi giua hai lan nop. Doi cach dat [id] la ca so cai cu tro thanh
 * vo nghia - luc do phai xoa so va lam lai tu dau.
 */
data class CauHoi(
    val id: String,
    val mon: String,
    val nguon: String,
    val chuong: String,
    val bai: String,
    /** "Bài tập", "Luyện tập 1", "Vận dụng"... de con nhan ra cho can tim. */
    val nhom: String,
    val ma: String,
    val de: String,
    val trang: Int,
    val dang: String,
    val thuTu: Int
) {
    /**
     * Nhan hien cho con, khong phai luc nao cung la [ma].
     *
     * Sach Toan in san so cau ("2.26a") nen lay thang lam nhan: con nhin man hinh va
     * nhin trang sach thay cung mot thu. Sach KHTN khong in so nao ca - so trong
     * ngan hang la so tu dat ("B2.C1"), gio sach ra khong co cho nao ghi the. Nhung
     * cau do hien ten o va so trang, dung thu con dang nhin tren giay.
     */
    fun nhan(): String = when {
        !MA_TU_DAT.matches(ma) -> ma
        nhom.isBlank() -> "Trang $trang"
        else -> "$nhom (tr.$trang)"
    }

    /** Dong hien cho con chon: "2.26a — Phân tích đa thức..." */
    fun dongChon(): String = "${nhan()}  ·  ${de.take(70)}${if (de.length > 70) "…" else ""}"

    private companion object {
        /** Ma do minh dat ra, khong phai ma in trong sach. Xem [nhan]. */
        val MA_TU_DAT = Regex("""^B\d+\.C\d+$""")
    }
}

/**
 * Mot the hoc thuoc: cau hoi CO SAN DAP AN, may cham mot minh khong can AI.
 *
 * KHAC [CauHoi] O DUNG MOT CHO, va cho do doi ca cach cham: bang cau hoi khong giu
 * dap an, vi mot bai toan co nhieu loi giai dung va chi AT nhin anh moi noi duoc
 * dung sai. The hoc thuoc thi nguoc lai - dap an la mot chuoi co dinh, con go ra,
 * may so chuoi. Nhet chung mot bang thi cot "dap" rong o hai tram dong cau hoi, ma
 * moi lan doc ra lai phai hoi "dong nay co dap an khong".
 *
 * [id] dang "bo:ma", vi du "toan8ct:hdt1". Y het [CauHoi.id]: no la danh tinh co
 * dinh, so the hen on bam vao no, nen DOI LA MAT HET lich on cua cac the cu.
 *
 * @param hoi cai hien len man hinh: "quyển sách", "(a + b)²".
 * @param dap cai con phai go ra.
 * @param dapKhac cac ban viet khac cung tinh dung. Xem [vn.huytl.homeworkgate.data.HocThuoc.dung].
 */
data class TheHoc(
    val id: String,
    val mon: String,
    val bo: String,
    val bai: String,
    val hoi: String,
    val dap: String,
    val dapKhac: List<String> = emptyList(),
    val thuTu: Int = 0
)

/** Mot lan con go tra loi mot the hoc thuoc. */
data class TraThe(
    val theId: String,
    val go: String,
    val dung: Boolean,
    val phut: Int,
    val luc: Long = System.currentTimeMillis()
)

/** Mot bo the da nap, kem so the dang den luot. Dung cho man chon bo. */
data class BoDaNap(
    val bo: String,
    val mon: String,
    val ten: String,
    val soDenLuot: Int,
    val tongThe: Int
)

/** Mot bai trong sach, gom nhieu cau. Dung de con chon truoc khi chup. */
data class TenBai(
    val bai: String,
    val chuong: String,
    val trang: Int,
    val soCau: Int
)

/**
 * Mot trang sach co bai tap, de con chon theo trang.
 *
 * @param bai ten bai dau tien co cau o trang nay - chi de con nhin cho do lac, mot
 *   trang co the vat sang bai ke tiep.
 */
data class TrangSach(
    val trang: Int,
    val soCau: Int,
    val bai: String,
    val chuong: String = ""
)

/** Mot lan cham mot cau, da ghi xuong kho. */
data class TraLoi(
    val cauId: String,
    val mon: String,
    val ma: String,
    val de: String,
    val ketQua: String,
    /** Tung dong con viet, chep tu anh ra. Xem [vn.huytl.homeworkgate.data.CauCham.baiLam]. */
    val baiLam: List<String> = emptyList(),
    /** Dong dau tien sai trong [baiLam], dem tu 1; 0 la dung het hoac khong ro. */
    val dongSai: Int = 0,
    /**
     * Lan nay la lam lai de on chu khong phai bai moi.
     *
     * Tach ra vi hai loai tra cong khac nhau (on duoc nua so phut, va chi mot lan),
     * va vi bang tien bo phai dem duoc "cau tung sai nay da on lai chua".
     */
    val onTap: Boolean = false,
    val dung: Boolean,
    val phut: Int,
    val nhanXet: String,
    val luc: Long,
    /*
     * Hai truong duoi day dat o CUOI, sau ca [luc], la co y: cac bai test dung
     * TraLoi(...) theo thu tu chu khong theo ten, nen chen vao giua la vo het.
     */
    /**
     * Con tu khai truoc khi cham: 1 la thay chac, 0 la chua chac, -1 la khong khai.
     *
     * Khong dung vao viec tinh gio, va co y nhu vay: gan thuong hay phat vao day
     * thi con se khai theo cai co loi chu khong theo cai no nghi. De tran thi no la
     * mot cau hoi that, va cau tra loi ghep voi ket qua cham cho ra thu dang gia hon
     * ca diem: con co biet minh dang biet gi khong.
     */
    val khaiChac: Int = -1,
    /** Cau con tu viet ra minh sai cho nao, truoc khi nop lai bai da sua. */
    val conNoi: String = "",
    /**
     * Cau nay sai KIEU gi: mot trong bay nhan o [vn.huytl.homeworkgate.ai.ChamBaiJson.NHAN_LOI].
     * Rong la cau dung, hoac ban cham cu chua co truong nay.
     *
     * Xem [vn.huytl.homeworkgate.kho.KhoBai.thongKeLoi] de biet cot nay de lam gi.
     */
    val loaiLoi: String = ""
)

/**
 * Pham vi con khai truoc khi chup: dang lam bai nao, nhung cau nao.
 *
 * DAY LA CHO SUA THAT SU CUA CA BAN NAY. Truoc day con bam "Nop bai" roi chup, va
 * may phai tu doan tam anh nay la bai gi - doan bang chinh cau lenh gui cho AI. Bay
 * gio con noi truoc, may chi viec cham. Mot dua tre khai "bai 2.26" roi chup bai
 * 2.27 thi AI van thay ngay khi doi chieu de bai trong danh sach voi de bai trong
 * anh; con khai dung ma chup lai bai hom qua thi ma cau da nam trong so cai.
 *
 * [nguon] rong nghia la bai khong co trong ngan hang (vo bai tap, phieu photo, mon
 * chua nap sach). Luc do chay duong cu: AI tu tach cau, so cai lay de bai da chuan
 * hoa lam khoa. Kem chac hon, nhung van la duong duy nhat cho nhung mon chua co
 * sach trong may.
 */
data class PhamVi(
    val mon: String,
    val nguon: String = "",
    val tenNguon: String = "",
    val bai: String = "",
    val cauIds: List<String> = emptyList(),
    /**
     * Lan nop nay la ON LAI cau da lam dung roi, khong phai bai moi.
     *
     * Doi hai thu: so cai khong duoc bo qua cau "da tra gio" nua (cai chinh no vua
     * chan), va so phut chi con mot nua - va chi tra mot lan trong doi moi cau.
     */
    val onTap: Boolean = false,
    /**
     * Cau con tu bao la chua chac, khai truoc khi chup.
     *
     * Khong dung vao viec tinh gio. Xem [TraLoi.khaiChac].
     */
    val chuaChac: List<String> = emptyList(),
    /**
     * Con da duoc hoi "cau nao chua chac" chua.
     *
     * Phai tach khoi [chuaChac] rong: khong duoc hoi, va duoc hoi roi bao chac het,
     * la hai chuyen khac han. Cai dau khong noi len gi; cai sau la mot loi khai.
     */
    val daKhaiChac: Boolean = false,
    /**
     * Con tu viet ra minh sai cho nao, mot dong, truoc khi nop lai bai da sua.
     *
     * Khong cham dung sai, khong anh huong so phut. Muc dich la bat dua tre goi ten
     * duoc cai sai cua chinh no - goi ten duoc thi lan sau moi tranh - va de Ba Huy
     * doc mot dong la biet con that su hieu hay chi chep lai dap an.
     */
    val conNoi: String = ""
) {
    val theoSach: Boolean get() = nguon.isNotBlank() && cauIds.isNotEmpty()

    fun sangJson(): String = JSONObject()
        .put("mon", mon)
        .put("nguon", nguon)
        .put("ten_nguon", tenNguon)
        .put("bai", bai)
        .put("cau_ids", JSONArray(cauIds))
        .put("on_tap", onTap)
        .put("chua_chac", JSONArray(chuaChac))
        .put("da_khai_chac", daKhaiChac)
        .put("con_noi", conNoi)
        .toString()

    companion object {
        fun tuJson(chu: String?): PhamVi? {
            if (chu.isNullOrBlank()) return null
            val o = runCatching { JSONObject(chu) }.getOrNull() ?: return null
            val a = o.optJSONArray("cau_ids")
            return PhamVi(
                mon = o.optString("mon"),
                nguon = o.optString("nguon"),
                tenNguon = o.optString("ten_nguon"),
                bai = o.optString("bai"),
                cauIds = (0 until (a?.length() ?: 0)).mapNotNull { a?.optString(it) }
                    .filter { it.isNotBlank() },
                onTap = o.optBoolean("on_tap", false),
                chuaChac = o.optJSONArray("chua_chac").let { m ->
                    (0 until (m?.length() ?: 0)).mapNotNull { m?.optString(it) }
                        .filter { it.isNotBlank() }
                },
                daKhaiChac = o.optBoolean("da_khai_chac", false),
                conNoi = o.optString("con_noi")
            )
        }
    }
}

/**
 * Cac con so cho man hinh "Con da lam duoc gi".
 *
 * @param soCauDung so CAU lam dung, khong phai so lan cham.
 * @param theoMon tung mon duoc bao nhieu cau, nhieu nhat truoc.
 * @param cauKhoDaGo cau tung sai roi lam lai dung duoc.
 * @param soNgayCoBai co bao nhieu ngay khac nhau co nop bai.
 * @param phutDaKiem tong so phut choi da doi duoc bang bai tap trong khoang.
 * @param cauDangChoSua con may cau dang sai, chua sua xong.
 */
/**
 * Con co biet minh dang biet gi khong.
 *
 * Ba con so, va ca ba deu la SO LAN chu khong phai ty le. Ty le thi phai chia, ma
 * chia ra la mot diem so - dung cai [TienBo] co y khong co.
 *
 * Don vi la mot LAN KHAI, khong phai mot cau. Cung mot cau tuan truoc con bao chua
 * chac, tuan nay bao chac, la hai lan con tu doc bung minh va ca hai deu that.
 *
 * @param bietTruoc con bao chua chac, va dung la chua chac. Day la cai dang ke: no
 *   khong phai mot lan sai, no la mot lan con nhin ra cho minh con hong truoc khi
 *   co ai cham.
 * @param honTuong con bao chua chac ma hoa ra lam dung.
 * @param tuongChac con bao chac ma van con sot.
 */
data class TuBiet(
    val bietTruoc: Int,
    val honTuong: Int,
    val tuongChac: Int
) {
    /** Con da duoc hoi lan nao chua. Chua thi khong hien the nay ra. */
    fun coGi(): Boolean = bietTruoc > 0 || honTuong > 0 || tuongChac > 0
}

data class TienBo(
    val soCauDung: Int,
    val theoMon: List<Pair<String, Int>>,
    val cauKhoDaGo: Int,
    val soNgayCoBai: Int,
    val phutDaKiem: Int,
    val cauDangChoSua: Int
)
