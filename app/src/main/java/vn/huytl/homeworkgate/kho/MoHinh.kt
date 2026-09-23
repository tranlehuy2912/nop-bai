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

    /**
     * Dong hien cho con chon: nhan o dong tren, de bai o dong duoi.
     *
     *     Luyện tập 1 (tr.6)
     *     Trong các biểu thức sau đây, biểu thức nào là đơn thức? ...
     *
     * XUONG DONG chu khong ngan bang mot dau giua hai phan. Ban truoc ngan bang dau
     * cham giua ("2.26a  ·  Phân tích..."), va no xau o dung cho hay gap nhat: de bai
     * dai nen dong tu xuong hang, cai dau cham giua nam lot thom giua mot doan chu va
     * khong con phan cach duoc gi. Ma khong the thay bang dau ")" kieu sach Toan
     * duoc: nhan cua sach KHTN da san la "Câu hỏi (tr.11)", them mot dau dong nua
     * thanh hai dau dong lien nhau.
     *
     * Xuong dong thi khong can dau ngan nao ca, va ben goi to dam dong tren de hai
     * phan tach han nhau - xem [vn.huytl.homeworkgate.ui.ChonBaiActivity].
     *
     * Khong cat bot de bai. Truoc day cat o ky tu thu 70 roi cham lung, ma dung cho
     * do thuong la giua chuoi bieu thuc - "3x^3y; -4; (3 ..." khong noi duoc cau nao
     * voi cau nao. Dong nay la thu duy nhat con doc de biet minh dang tich cau gi, va
     * o tich thi cho xuong dong thoai mai.
     */
    fun dongChon(nhan: String = nhan()): String = "$nhan\n$de"

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

/**
 * Mot tu trong bang GLOSSARY cuoi sach giao khoa.
 *
 * VI SAO KHONG DUNG [TheHoc] cho ca tu vung. The hoc thuoc giu mot cap hoi/dap co
 * dinh: hoi la "(a + b)²", dap la "a² + 2ab + b²", doi cho nhau thi vo nghia. Mot tu
 * thi nguoc lai - no duoc hoi theo CA HAI chieu, va chieu nao la tuy tu do da gap
 * may lan: lan dau hoi Anh sang Viet cho con nhan mat chu, quen roi thi hoi Viet
 * sang Anh bat go ra. Nhet vao hoi/dap thi phai luu moi tu hai dong, va hai dong do
 * mang hai lich on rieng trong khi chung la mot tu.
 *
 * Con hai thu nua chi tu vung moi co. [am] de doc len va de hien sau khi tra loi,
 * khong tham gia cham - nen may doc nham mot ky tu IPA thi khong ai mat phut. [loai]
 * de tron moi nhu cho cau trac nghiem: moi nhu phai cung loai tu thi con moi phai
 * biet nghia, chu lay bua thi no loai tru duoc ma khong can biet gi.
 *
 * [id] dang "bo:tu", vi du "anh8:access". Y het [CauHoi.id] va [TheHoc.id]: doi cach
 * dat la mat het lich on cua cac tu cu.
 */
data class TuVung(
    val id: String,
    val bo: String,
    val mon: String,
    /** So Unit trong sach. 0 la khong biet - chi xay ra khi ban chep bi hong. */
    val unit: Int,
    val tu: String,
    /** n, v, adj, adv, prep, conj. Nhieu loai thi ngan bang dau phay. */
    val loai: String,
    /** Phien am IPA, con ca hai dau gach cheo. Rong la sach khong ghi hoac doc khong ro. */
    val am: String,
    val nghia: String,
    val thuTu: Int = 0
)

/** Con duoc hoi mot tu theo chieu nao. */
enum class Chieu {
    /** Hien tu tieng Anh, con chon nghia trong bon lua chon. Cho tu con chua gap. */
    ANH_VIET,

    /** Hien nghia tieng Viet, con go tu tieng Anh ra. Cho tu con da dung it nhat mot lan. */
    VIET_ANH
}

/** Buoi do nay la buoi nao. Quyet dinh tra phut kieu gi, xem [vn.huytl.homeworkgate.data.TuVung]. */
enum class BuoiDo {
    /** Buoi do hang ngay, boc ngau nhien co trong so. Tra giay theo tung tu. */
    HANG_NGAY,

    /** Do het tu cua Unit co giao, buoi toi. Nam trong tron goi 45 phut, khong tra rieng. */
    DAN_DO_TOI,

    /** Do lai chinh nhung tu do sang hom sau. Tra giay theo tung tu. */
    DAN_DO_SANG
}

/** Mot lan con tra loi mot tu. Moi lan thu la mot dong, ke ca lan sai. */
data class TraTu(
    val tuId: String,
    /** Ma phien, de dem "dung du hai lan trong MOT buoi". */
    val phien: String,
    val buoi: BuoiDo,
    val chieu: Chieu,
    /** Lan thu may cua tu nay trong phien nay, dem tu 1. */
    val lan: Int,
    val go: String,
    val dung: Boolean,
    /** Da phai mo may bac goi y. 0 la con tu lam duoc. */
    val goiY: Int,
    /** Con bam "Chịu rồi" chu khong tu ra duoc. */
    val chiu: Boolean,
    /**
     * So GIAY da tra cho tu nay, khong phai so phut.
     *
     * Phai la giay vi truoc 23/9/2026 mot tu chi dang gia nua phut, va nhung dong
     * ghi tu hoi do van con trong kho. Luu bang phut thi 30 giay lam tron thanh 0
     * hay 1 deu sai, ma cong hai muoi con so da lam tron thi lech han vai phut so
     * voi con so dang le phai tra.
     */
    val giay: Int,
    val luc: Long = System.currentTimeMillis()
)

/** Mot lan con go tra loi mot the hoc thuoc. */
data class TraThe(
    val theId: String,
    val go: String,
    val dung: Boolean,
    /**
     * Con bam "Chịu rồi" chu khong tu go ra duoc.
     *
     * Tach khoi [dung] = false du ca hai deu la khong tra loi duoc. Go sai la con co
     * thu; bam chiu la con bo. Hai viec do noi hai chuyen khac nhau voi Ba Huy, va
     * gop lai thanh mot thi khong con cach nao tach ra. Giong [TraTu.chiu].
     */
    val chiu: Boolean = false,
    /**
     * So PHUT da cong duoc cho luot nay. Ca luot gan het vao mot dong.
     *
     * Khac [giay] o cho nay: cot giay la cong suc lam ra, cot phut la cai that su
     * cap duoc sau khi qua tran. Hai so lech nhau khi tran ngay chung da het.
     */
    val phut: Int,
    /**
     * So GIAY luot nay lam ra, chua qua tran. Tran ngay cua duong hoc thuoc doc
     * cot nay - xem [vn.huytl.homeworkgate.data.HocThuoc.phutThem]. Giong [TraTu.giay].
     */
    val giay: Int = 0,
    val luc: Long = System.currentTimeMillis()
)

/** Mot bo the da nap, kem so the dang den luot. Dung cho man chon bo. */
data class BoDaNap(
    val bo: String,
    val mon: String,
    val ten: String,
    val soDenLuot: Int,
    val tongThe: Int,
    /** So the da qua het cac moc nho lai. Man chon bo ve thanh tien do tu day. */
    val soThuoc: Int = 0
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
