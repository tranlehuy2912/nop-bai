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
 * hen on cua cac the cu tro thanh vo nghia. Ma the ("ma" trong file) cung vay.
 *
 * Moi the co the ghi them "trang": trang sach de nguoi soat mo ra doi chieu. May
 * khong doc truong do.
 */
object BoThe {

    /**
     * @param phanBietHoa cham co giu chu hoa chu thuong khong. Chi bat cho bo hoc KI
     *   HIEU, noi "CO" va "Co" la hai chat khac nhau. Luat cu the o
     *   [vn.huytl.homeworkgate.data.HocThuoc.dung].
     */
    data class Bo(
        val bo: String,
        val mon: String,
        val ten: String,
        val file: String,
        val phanBietHoa: Boolean = false
    )

    /**
     * Cong thuc Toan 8, va hai bo KHTN 8: phan Hoa hoc va phan Vat li.
     *
     * Bo Toan phu ca nam hoc, Bai 1 toi Bai 39 cua SGK Toan 8 Ket noi tri thuc (tap mot
     * Bai 1-20, tap hai Bai 21-39), moi bai mot nhom dung ten in trong sach. Truoc
     * 25/9/2026 no chi co muoi mot the hang dang thuc chia hai nhom tu dat, lam ban mau
     * cho cac bo sau; con chua chon duoc "lop da hoc toi Bai 6" vi Bai 6, 7, 8 nam
     * chung mot nhom. Ten bo van la "Công thức Toán 8" du gio co ca dinh nghia va dinh
     * li, vi ten do nam trong kich ban thu (tools/kichban.py) va trong nhat ky cu.
     *
     * The dinh nghia hoi MOT CHU HAY MOT CUM NGAN chu khong bat go ca cau: may cham bang
     * phep so chuoi (xem [vn.huytl.homeworkgate.data.HocThuoc.dung]), ma mot dinh nghia
     * viet lai bang loi cua con thi dung y van khong khop tung chu. Nen the ghi cau
     * trong sach, bo trong dung cho can nho ("… của mỗi đường" dap "trung điểm"), va ke
     * cac cach viet khac trong dap_khac.
     *
     * KHTN TACH HAI BO chu khong gop mot. Truong chia KHTN cho nhieu giao vien day
     * song song, lop co the dang o Bai 4 phan Hoa trong luc dang o Bai 15 phan Li. Mot
     * luot lay [vn.huytl.homeworkgate.data.HocThuoc.SO_THE_MOI_LUOT] the dau tien
     * con den luot theo thu tu trong file, nen gop mot bo thi phai qua het sau muoi
     * the Hoa moi toi the Li dau tien. Tach ra thi con chon dung phan dang hoc.
     *
     * Phan Sinh hoc khong co bo nao: phan lon la dinh nghia dai, may so tung chu thi
     * khong cham duoc. Tu vung tieng Anh thi di duong rieng, xem [BoTuVung]. Con thieu
     * moc Su.
     */
    val BO = listOf(
        Bo(
            bo = "toan8ct",
            mon = "Toán",
            ten = "Công thức Toán 8",
            file = "hocthuoc/toan8ct.json"
        ),
        Bo(
            bo = "khtn8hoa",
            mon = "Khoa học tự nhiên",
            ten = "KHTN 8 phần Hoá học",
            file = "hocthuoc/khtn8hoa.json",
            phanBietHoa = true
        ),
        Bo(
            bo = "khtn8li",
            mon = "Khoa học tự nhiên",
            ten = "KHTN 8 phần Vật lí",
            file = "hocthuoc/khtn8li.json",
            phanBietHoa = true
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

    /**
     * Moc cat bo [bo] o bai lop da hoc toi, de dua vao [KhoBai.cacTheDenLuot].
     *
     * null khi con chua chon, hoac khi bai con chon khong con trong file (file JSON doi
     * ten bai): ca hai deu phai hoi lai con, khong doan. -1 khi con chon chua hoc bai
     * nao: khong the nao lot qua. Xem [HocToi].
     */
    fun denThuTu(context: Context, bo: String): Int? {
        val bai = HocToi.baiCua(context, bo) ?: return null
        if (bai == HocToi.CHUA_HOC_BAI_NAO) return -1
        val kho = KhoBai.get(context)
        kho.thuTuCuoiCua(bo, bai)?.let { return it }
        /*
         * Bai con chon khong co the nao. Tu 27/9/2026 hop chon liet ke du bai cua sach
         * chu khong chi bai co the (xem [PhanHoc]): chon Bai 7 phan Hoa thi bo the dung
         * o the cuoi cua Bai 6. Lop chua toi the dau tien cua bo thi -1, khong the nao.
         * Ten bai khong doc ra so thi van la file doi ten bai, hoi lai nhu truoc.
         */
        val so = PhanHoc.soBai(bai) ?: return null
        return kho.thuTuCuoiDenBaiSo(bo, so) ?: -1
    }

    /** Dong Kiem tra bai tren man chinh dang o tinh trang nao, xem [tinhTrangManChinh]. */
    enum class TinhTrang {
        /** Co the den luot trong phan lop da hoc. */
        CO_THE,

        /** Con bo chua chon bai da hoc: phai vao chon thi may moi biet hoi gi. */
        CHUA_CHON,

        /** Het the hom nay, nhung con bai phia sau de mo khi lop hoc toi. */
        HET_HOM_NAY,

        /** Khong co bo nao, hoac bo nao cung da chon toi bai cuoi va het the. */
        KHONG
    }

    /**
     * Man chinh nen hien dong Kiem tra bai the nao.
     *
     * Truoc 25/9/2026 dong nay chi hien khi con the den luot. Tu khi cat bo the o bai con
     * chon, lam vay la co ngo cut: con chon Bai 4, lam het the cua Bai 3 va Bai 4, dong
     * bien mat - va hom sau lop hoc Bai 6 thi con khong con cua nao de vao chon lai. Nen
     * het the ma con bai phia sau thi dong van hien, o dang da xong.
     *
     * Khong goi [bang] roi dem: man chinh ve lai moi giay khi dong ho dang dem, ma
     * [bang] quet tung the cua moi bo ba lan - den luot, tong so, da thuoc - trong khi
     * o day chi can biet co hay khong. Nen hoi theo thu tu re truoc dat sau, va dung
     * ngay o bo dau tien con the.
     */
    fun tinhTrangManChinh(context: Context): TinhTrang {
        val kho = KhoBai.get(context)
        val cacBo = BO.filter { kho.soTheCua(it.bo) > 0 }
        val moc = cacBo.map { it to denThuTu(context, it.bo) }
        if (moc.any { (bo, den) -> den != null && kho.conTheDenLuot(bo.bo, denThuTu = den) }) {
            return TinhTrang.CO_THE
        }
        if (moc.any { it.second == null }) return TinhTrang.CHUA_CHON
        if (cacBo.any { conBaiSau(context, it.bo) }) return TinhTrang.HET_HOM_NAY
        return TinhTrang.KHONG
    }

    /**
     * Sau bai con da chon con bai nao nua khong. Chua chon thi coi nhu con.
     *
     * So theo ten bai cuoi trong file chu khong theo so the: chon "chua hoc bai nao" la
     * con ca bo phia sau.
     */
    private fun conBaiSau(context: Context, bo: String): Boolean {
        val bai = HocToi.baiCua(context, bo) ?: return true
        val cac = KhoBai.get(context).cacBaiTrongBoThe(bo)
        if (cac.isEmpty()) return false
        // So theo so bai: con co the chon mot bai khong co the nao, nam giua hai bai co the.
        val so = PhanHoc.soBai(bai)
        val cuoi = PhanHoc.soBai(cac.last())
        if (so != null && cuoi != null) return so < cuoi
        return bai != cac.last()
    }

    /** Cac bo dang co the den luot, de con chon. Bo nao khong con gi thi van hien. */
    fun bang(context: Context): List<BoDaNap> {
        val kho = KhoBai.get(context)
        return BO.map { bo ->
            val den = denThuTu(context, bo.bo)
            BoDaNap(
                bo = bo.bo,
                mon = bo.mon,
                ten = bo.ten,
                soDenLuot = if (den == null) 0 else kho.soTheDenLuot(bo.bo, denThuTu = den),
                tongThe = kho.soTheCua(bo.bo),
                soThuoc = kho.soTheThuoc(bo.bo),
                hocToi = if (den == null) null else HocToi.baiCua(context, bo.bo)
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
