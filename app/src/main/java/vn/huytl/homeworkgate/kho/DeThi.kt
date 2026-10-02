package vn.huytl.homeworkgate.kho

import android.content.Context
import android.util.Log
import org.json.JSONObject
import vn.huytl.homeworkgate.data.LichKiemTra

/**
 * Bo de thi in san: moi "bai" trong file la mot de nguyen ven, giu dung thu tu va ten cac
 * phan nhu de in. Cau hoi nam trong kho nhu moi quyen sach ([NganHang] nap); file nay chi
 * doc them khung cua tung de, thu kho khong giu: de nao, cau nao so may, thuoc phan nao,
 * de toi Unit (hay bai) nao, lam trong bao lau, hinh nao an sau nut "Nhờ trợ giúp".
 *
 * VI SAO CO (30/9/2026). Ba Huy muon Le Hoa giai de giua ki, cuoi ki tren may cho quen dang
 * de truoc gio kiem tra that. Giai de cu tu nhat cau sach bai tap roi don trac nghiem len
 * truoc; de thi thi phai di dung nhu to de: phan A roi phan B, cau 5 la cau 5.
 *
 * DE TOAN, KHTN (Ba Huy chot 1/10/2026). Hai mon nay day song song nhieu phan (Toan: Dai so,
 * Hinh hoc; KHTN: Hoa, Li, Sinh - xem [PhanHoc]), con de in cua loigiaihay soan theo thu tu
 * sach: de giua ki 1 KHTN chi co cau Hoa, de giua ki 1 Toan la "ba chuong dau" (Dai so Bai
 * 1-9, Hinh hoc Bai 10-14). Nen pham vi khong ghi tay mot con so nhu den_unit cua de Anh ma
 * tinh tu tung cau: moi cau lam tren may ghi "bai_sgk" (so bai SGK no kiem). Tu 2/10/2026
 * pham vi la tap cac bai do ([De.cacBai]), vi con danh dau tung bai da hoc; [De.denBai] (bai
 * cao nhat cua moi phan) chi con dung de xep de rong truoc ([De.doRong]). Dong ho: de in so
 * phut thi dung so do ("phut"), khong in thi Toan 90, KHTN 60 ([phutMacDinh]).
 *
 * CAU TRUNG GIUA HAI DE. Cung mot cau in o hai de (cau 1 den 4 cua de giua ki 1 so 1 va so
 * 2 giong het nhau) thi de sau ghi "trung" tro ve cau cua de truoc, va hai de dung chung
 * mot id. So cai tra gio theo id, nen cau do chi sinh gio mot lan, lam lai o de sau chi
 * cong phan hon lan tot nhat nhu moi cau lam lai. Cau trung lay bai_sgk va hinh an cua cau
 * goc. Dinh dang o tools/ghep/DINH_DANG.md.
 *
 * Doc tu assets mot lan moi tien trinh roi giu trong bo nho: file khong doi trong luc app
 * chay, va man Giai de hoi khung de moi lan ve.
 */
object DeThi {

    /** Mot cau in trong de, theo dung thu tu in. */
    data class Muc(
        /** So cau in tren de: cau 36 la 36 ke ca khi phan nghe truoc do bi bo. Ma khong ket bang so thi 0. */
        val so: Int,
        /** Id cau trong kho. Cau "trung" thi la id cua cau goc o de khac. */
        val cauId: String,
        /** Phan lon in tren de: "A. LANGUAGE FOCUS", "Phần tự luận (7 điểm)". */
        val phan: String,
        /** Loi dan cua bai trong phan: "Exercise 1. Circle the word whose ...", hay de cua bai tu luan. */
        val nhom: String,
        /** Ly do khong lam tren may ("nghe", "vẽ hình"), rong la lam duoc. */
        val boMay: String,
        /**
         * Chu o dau the cau, dung nhu de in: "Câu 36", "Bài 3a" (1/10/2026). De Anh khong ghi thi
         * la "Câu <so>". De Toan, KHTN danh so hai lan (trac nghiem Cau 1-12, tu luan Bai 1-4) nen
         * khong suy ra duoc tu ma cau.
         */
        val nhan: String,
        /** So bai SGK ma cau kiem (de Toan, KHTN). 0 khi khong ghi: de Anh, cau khong lam tren may. */
        val baiSgk: Int = 0,
        /**
         * Hinh an cua bai (1/10/2026): to de khong in hinh, con tu ve. Bam "Nhờ trợ giúp" lan
         * mot thi hien [hinhGoiY] (hinh trong loi giai, da xoa net, diem lo dap an), lan hai
         * (sau hop canh bao) thi hien [hinhDayDu]. Moi y cua cung mot bai mang cung hai danh
         * sach. Duong dan tinh tu assets/hinh/, nhu [CauHoi.hinh].
         */
        val hinhGoiY: List<String> = emptyList(),
        val hinhDayDu: List<String> = emptyList()
    ) {
        val coHinhAn: Boolean get() = hinhGoiY.isNotEmpty() || hinhDayDu.isNotEmpty()
    }

    data class De(
        /** Ma ngan, di vao khoa cua [DeGiai] va lenh cua Bang dieu khien: "GK1-1", "TGK1-2". */
        val ma: String,
        val nguon: String,
        val mon: String,
        /** "Đề giữa kì 1 số 1". */
        val ten: String,
        /** De Tieng Anh: Unit cuoi cung ma cau trong de cham toi, tinh tren cac cau lam tren may. Mon khac 0. */
        val denUnit: Int,
        /**
         * De Toan, KHTN: ma phan ([PhanHoc.Phan.ma]) toi bai cao nhat ma cac cau lam tren may
         * cua phan do cham toi, theo thu tu phan trong [PhanHoc.TAT_CA]. Phan de khong co cau
         * nao thi khong co mat, tru khi de co cau thieu bai_sgk: luc do moi phan cua mon deu co,
         * toi bai cuoi cua phan (xem [denBai]). De Anh rong.
         */
        val denBai: Map<String, Int>,
        /** Gio lam bai, phut. */
        val phut: Int,
        val cacMuc: List<Muc>,
        /**
         * De Toan, KHTN: moi bai SGK ma cac cau lam tren may cham toi. De chi tu mo khi moi bai
         * nay deu da danh dau la da hoc (Ba Huy chot 2/10/2026, xem
         * [vn.huytl.homeworkgate.data.GiaiDe.thieuPhamVi]). Cau thieu bai_sgk thi coi nhu de thi
         * ca nam: moi bai cua mon, nhu [denBai]. De Anh rong.
         */
        val cacBai: Set<Int> = emptySet()
    ) {
        /** Cac cau lam tren may, theo thu tu in, moi id mot lan. */
        val cauIds: List<String> get() = cacMuc.filter { it.boMay.isBlank() }.map { it.cauId }.distinct()

        val laTiengAnh: Boolean get() = mon == PhanHoc.TIENG_ANH

        /**
         * Do rong cua pham vi, de chon de nao mo truoc khi nhieu de cung du pham vi: de cuoi ki
         * truoc de giua ki, nhu luat "pham vi cao nhat" cua de Anh. De Anh la so Unit; de Toan,
         * KHTN la tong so bai cua cac phan tinh tu bai dau toi bai de cham toi.
         */
        val doRong: Int
            get() = if (laTiengAnh) denUnit
            else denBai.entries.sumOf { (ma, bai) -> PhanHoc.theoMa(ma)?.cacSoToi(bai)?.size ?: 0 }
    }

    /** Mot cau co hinh an, kem de chua no: cho man lam lai, on lai, noi cau dung mot minh. */
    data class HinhAn(val de: De, val muc: Muc)

    @Volatile
    private var nho: List<De>? = null

    /**
     * Danh sach de thay cho bo de that, chi cho test (1/10/2026). Luc viet luat mo de theo phan,
     * repo chua co de Toan, KHTN that (buoc 4.4 moi soan), nen test dung de gia lap tu cau sach
     * bai tap trong kho. Thay han chu khong noi vao sau: noi vao thi tu buoc 4.4 de that cung mon,
     * cung do rong dung truoc de gia va thang luot mo. null la dung bo de that. Ung dung khong
     * bao gio ghi vao day.
     */
    @Volatile
    internal var thayChoTest: List<De>? = null

    /** Moi de cua moi bo de thi, theo thu tu bo de trong [NganHang.SACH] roi thu tu trong file. */
    fun tatCa(context: Context): List<De> =
        thayChoTest ?: nho ?: synchronized(this) { nho ?: doc(context).also { nho = it } }

    fun theoMa(context: Context, ma: String): De? = tatCa(context).firstOrNull { it.ma == ma }

    /** "GK1-1.36" ra 36. Ma khong ket bang so thi 0. */
    fun soIn(ma: String): Int = ma.substringAfterLast('.').takeWhile { it.isDigit() }.toIntOrNull() ?: 0

    /**
     * Hinh an cua cau [cauId], o de dau tien co cau do. null khi cau khong thuoc de thi nao,
     * hay khong co hinh an. Man lam lai, on lai dung: cau de thi quay lai o do mot minh, khong
     * co the dau bai nhu trong de.
     */
    fun hinhAnCua(context: Context, cauId: String): HinhAn? {
        for (de in tatCa(context)) {
            val m = de.cacMuc.firstOrNull { it.cauId == cauId && it.coHinhAn } ?: continue
            return HinhAn(de, m)
        }
        return null
    }

    /**
     * Nhan cua mot cau de thi khi no dung mot minh (man lam lai, on lai): "Bài 3a, Đề giữa kì 1
     * số 2", theo de dau tien co cau do (cau trung thi la de goc). null khi cau khong thuoc de thi
     * nao. Truoc 1/10/2026 dong phu o do ghi ma tho trong kho ("GK1-1.36", "TGK1-2.B3a").
     */
    fun nhanCau(context: Context, cauId: String): String? {
        for (de in tatCa(context)) {
            val m = de.cacMuc.firstOrNull { it.cauId == cauId } ?: continue
            return "${m.nhan}, ${de.ten}"
        }
        return null
    }

    /**
     * Ten ngan cua bai chua cau, cho dong nhat ky: "Bài 3" lay tu dau loi dan ("Bài 3. (2,5
     * điểm) Cho hình bình hành..."), khong co thi la nhan cua cau ("Câu 9").
     */
    fun tenBai(muc: Muc): String = TEN_BAI.find(muc.nhom)?.groupValues?.get(1) ?: muc.nhan

    private val TEN_BAI = Regex("""^\s*((?:Bài|Câu)\s+\d+)""")

    /** Gio lam bai khi de khong in so phut: Toan 90, KHTN 60 (Ba Huy chot 1/10/2026), con lai 45. */
    fun phutMacDinh(mon: String): Int = when (mon) {
        LichKiemTra.TOAN -> 90
        LichKiemTra.KHTN -> 60
        else -> PHUT_MAC_DINH
    }

    private fun doc(context: Context): List<De> = NganHang.boDeThi().flatMap { sach ->
        runCatching { docQuyen(context, sach) }.getOrElse { e ->
            Log.w(TAG, "khong doc duoc ${sach.file}: ${e.message}")
            emptyList()
        }
    }

    private fun docQuyen(context: Context, sach: NganHang.Sach): List<De> =
        docTuChuoi(context.assets.open(sach.file).bufferedReader().use { it.readText() }, sach)

    /**
     * Doc cac de tu noi dung mot file bo de. Tach khoi [docQuyen] de test doc duoc mot file
     * gia (1/10/2026): repo chua co file de Toan, KHTN that.
     */
    internal fun docTuChuoi(chu: String, sach: NganHang.Sach): List<De> {
        val o = JSONObject(chu)
        val cacBai = o.optJSONArray("cac_bai") ?: return emptyList()
        // Cau trung lay bai_sgk, hinh an cua cau goc: gom moi cau co noi dung cua ca file truoc.
        val goc = HashMap<String, JSONObject>()
        for (i in 0 until cacBai.length()) {
            val cs = cacBai.optJSONObject(i)?.optJSONArray("cac_cau") ?: continue
            for (j in 0 until cs.length()) {
                val c = cs.optJSONObject(j) ?: continue
                if (c.optString("trung").isBlank()) goc[c.optString("ma").trim()] = c
            }
        }
        return (0 until cacBai.length()).mapNotNull { i ->
            val b = cacBai.optJSONObject(i) ?: return@mapNotNull null
            val meta = b.optJSONObject("de_thi") ?: return@mapNotNull null
            val ma = meta.optString("ma").trim()
            val cacCau = b.optJSONArray("cac_cau")
            if (ma.isEmpty() || cacCau == null) return@mapNotNull null
            val cacMuc = (0 until cacCau.length()).mapNotNull { j ->
                val c = cacCau.optJSONObject(j) ?: return@mapNotNull null
                val maCau = c.optString("ma").trim()
                val maGoc = c.optString("trung").trim().ifEmpty { maCau }
                if (maGoc.isEmpty()) return@mapNotNull null
                val noiDung = goc[maGoc] ?: c
                val so = soIn(maCau)
                Muc(
                    so = so,
                    cauId = "${sach.nguon}:$maGoc",
                    phan = c.optString("phan").trim(),
                    nhom = c.optString("nhom").trim(),
                    boMay = c.optString("bo_may").trim(),
                    nhan = c.optString("nhan").trim().ifEmpty { if (so > 0) "Câu $so" else maCau.substringAfterLast('.') },
                    baiSgk = noiDung.optInt("bai_sgk", 0),
                    hinhGoiY = chuoi(noiDung.optJSONArray("hinh_goi_y")),
                    hinhDayDu = chuoi(noiDung.optJSONArray("hinh_day_du"))
                )
            }
            val laAnh = sach.mon == PhanHoc.TIENG_ANH
            De(
                ma = ma,
                nguon = sach.nguon,
                mon = sach.mon,
                ten = b.optString("bai").trim(),
                denUnit = if (laAnh) meta.optInt("den_unit", DEN_UNIT_CA_NAM) else 0,
                denBai = if (laAnh) emptyMap() else denBai(sach.mon, ma, cacMuc),
                phut = meta.optInt("phut", phutMacDinh(sach.mon)),
                cacMuc = cacMuc,
                cacBai = if (laAnh) emptySet() else cacBai(sach.mon, cacMuc)
            )
        }
    }

    /**
     * Pham vi tung phan cua mot de Toan, KHTN: bai cao nhat ma cac cau lam tren may cham toi.
     *
     * Cau lam tren may ma thieu bai_sgk (kiem.py bat loi nay, day chi la cho phong) thi khong
     * doan: coi nhu de thi ca nam, moi phan toi bai cuoi cua phan, nhu de Anh khong ghi pham vi
     * ([DEN_UNIT_CA_NAM]). Doan sai theo chieu nguoc lai thi de tu mo truoc khi lop hoc toi.
     */
    private fun denBai(mon: String, ma: String, cacMuc: List<Muc>): Map<String, Int> {
        val lam = cacMuc.filter { it.boMay.isBlank() }
        val cacPhan = PhanHoc.cuaMon(mon)
        if (lam.any { it.baiSgk <= 0 || PhanHoc.cuaBai(mon, it.baiSgk) == null }) {
            Log.w(TAG, "de $ma co cau thieu bai_sgk, coi nhu thi ca nam")
            return cacPhan.associate { p -> p.ma to p.cacKhoang.maxOf { it.last } }
        }
        val caoNhat = lam.groupBy { PhanHoc.cuaBai(mon, it.baiSgk)!!.ma }
            .mapValues { (_, cac) -> cac.maxOf { it.baiSgk } }
        // Giu thu tu phan cua PhanHoc (Dai so truoc Hinh hoc, Hoa truoc Li) cho dong chu pham vi.
        return cacPhan.mapNotNull { p -> caoNhat[p.ma]?.let { p.ma to it } }.toMap()
    }

    /**
     * Moi bai SGK ma cac cau lam tren may cua de cham toi. Co cau thieu bai_sgk thi khong doan:
     * coi nhu de thi ca nam, moi bai cua mon, giong [denBai].
     */
    private fun cacBai(mon: String, cacMuc: List<Muc>): Set<Int> {
        val lam = cacMuc.filter { it.boMay.isBlank() }
        if (lam.any { it.baiSgk <= 0 || PhanHoc.cuaBai(mon, it.baiSgk) == null }) {
            return PhanHoc.cuaMon(mon).flatMap { it.cacSo }.toSortedSet()
        }
        return lam.map { it.baiSgk }.toSortedSet()
    }

    private fun chuoi(a: org.json.JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() }

    /** De khong ghi pham vi thi coi nhu thi ca nam: chi mo khi lop hoc het sach. */
    private const val DEN_UNIT_CA_NAM = 12

    private const val PHUT_MAC_DINH = 45

    private const val TAG = "DeThi"
}
