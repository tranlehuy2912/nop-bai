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
     * Moc cat mot bo the: cac bai trong bo ma con da danh dau la da hoc ([chiBai]). Dua thang
     * vao [KhoBai.cacTheDenLuot], [KhoBai.conTheDenLuot]. [denThuTu] con lai cho ham kho cu, tu
     * 2/10/2026 khong ai dat no nua.
     */
    data class Moc(val denThuTu: Int = Int.MAX_VALUE, val chiBai: Set<String>? = null) {
        /** Lop chua hoc bai nao trong bo: khong the nao lot qua. */
        val rong: Boolean get() = denThuTu < 0 || chiBai?.isEmpty() == true
    }

    /**
     * Moc cua bo [bo]: dung cac bai trong bo co so nam trong tap bai da hoc cua mon
     * ([PhanHoc.baiDaHoc]). null khi mon chua chon lan nao (hoi lai, khong doan).
     *
     * Truoc 2/10/2026 bo mot phan (KHTN) cat bang thu tu the toi bai con chon ("hoc toi Bai
     * N"), bo Toan cat theo bai da hoc cua hai phan. Nay con danh dau tung bai cua ca mon, nen
     * moi bo cung cach: lay dung cac bai con da danh dau, bai the nam o cho nao trong file
     * cung vay. Ten bai trong file khong doc ra so thi khong bao gio ra: file bo the luon ghi
     * "Bài N. ...", xem [HocThuocTest].
     */
    fun mocCua(context: Context, bo: String): Moc? {
        val mon = theoMa(bo)?.mon ?: return null
        val daHoc = PhanHoc.baiDaHoc(context, mon) ?: return null
        return Moc(
            chiBai = KhoBai.get(context).cacBaiTrongBoThe(bo)
                .filter { PhanHoc.soBai(it)?.let { so -> so in daHoc } == true }.toSet()
        )
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
     * Dong Kiem tra bai nen hien the nao. Tu 30/9/2026 dong do nam o trang Luyen tap
     * ([vn.huytl.homeworkgate.ui.LuyenTapActivity]), man chinh chi hoi co the den luot khong
     * de ghi vao dong "Luyện tập".
     *
     * Truoc 25/9/2026 dong nay chi hien khi con the den luot. Tu khi cat bo the o bai con
     * chon, lam vay la co ngo cut: con chon Bai 4, lam het the cua Bai 3 va Bai 4, dong
     * bien mat - va hom sau lop hoc Bai 6 thi con khong con cua nao de vao chon lai. Nen
     * het the ma con bai phia sau thi dong van hien, o dang da xong. Tu 30/9/2026 trang Luyen
     * tap con co khoi "Lớp đã học tới" rieng, nhung dong van giu nhu vay.
     *
     * Khong goi [bang] roi dem: man chinh ve lai moi giay khi dong ho dang dem, ma
     * [bang] quet tung the cua moi bo ba lan - den luot, tong so, da thuoc - trong khi
     * o day chi can biet co hay khong. Nen hoi theo thu tu re truoc dat sau, va dung
     * ngay o bo dau tien con the.
     */
    fun tinhTrangManChinh(context: Context): TinhTrang {
        val kho = KhoBai.get(context)
        val cacBo = BO.filter { kho.soTheCua(it.bo) > 0 }
        val moc = cacBo.map { it to mocCua(context, it.bo) }
        if (moc.any { (bo, m) -> m != null && kho.conTheDenLuot(bo.bo, denThuTu = m.denThuTu, chiBai = m.chiBai) }) {
            return TinhTrang.CO_THE
        }
        if (moc.any { it.second == null }) return TinhTrang.CHUA_CHON
        if (cacBo.any { conBaiSau(context, it.bo) }) return TinhTrang.HET_HOM_NAY
        return TinhTrang.KHONG
    }

    /**
     * Con bai nao trong bo chua danh dau la da hoc khong. Chua chon thi coi nhu con: con vao
     * danh dau them bai la co the moi.
     */
    private fun conBaiSau(context: Context, bo: String): Boolean {
        val cac = KhoBai.get(context).cacBaiTrongBoThe(bo)
        if (cac.isEmpty()) return false
        val chi = mocCua(context, bo)?.chiBai ?: return true
        return cac.any { it !in chi }
    }

    /**
     * Cac bo dang co the den luot, de con chon. Bo nao khong con gi thi van hien. [mon] khac
     * null thi chi bo cua mon do: dong "Kiểm tra công thức <môn>" o trang Luyen tap (2/10/2026).
     */
    fun bang(context: Context, mon: String? = null): List<BoDaNap> {
        val kho = KhoBai.get(context)
        return BO.filter { mon == null || it.mon == mon }.map { bo ->
            val m = mocCua(context, bo.bo)
            // Cac bai cua phan bo the ma con da danh dau, de dong cuoi the noi lop dang o dau.
            val cacPhan = PhanHoc.cuaBoThe(bo.bo)
            val cuaBo = PhanHoc.baiDaHoc(context, bo.mon)
                ?.filter { so -> cacPhan.any { it.chua(so) } }?.toSet()
            val hocToi = when {
                m == null -> null
                m.rong -> HocToi.CHUA_HOC_BAI_NAO
                else -> m.chiBai.orEmpty().joinToString(",")
            }
            BoDaNap(
                bo = bo.bo,
                mon = bo.mon,
                ten = bo.ten,
                soDenLuot = if (m == null) 0
                else kho.soTheDenLuot(bo.bo, denThuTu = m.denThuTu, chiBai = m.chiBai),
                tongThe = kho.soTheCua(bo.bo),
                soThuoc = kho.soTheThuoc(bo.bo),
                hocToi = hocToi,
                moTaHocToi = cuaBo?.let { "Bài đã học: " + HocToi.moTa(bo.mon, it) }
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
