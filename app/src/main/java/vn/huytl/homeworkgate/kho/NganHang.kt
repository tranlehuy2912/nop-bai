package vn.huytl.homeworkgate.kho

import android.content.Context
import android.util.Log
import org.json.JSONObject
import vn.huytl.homeworkgate.data.Prefs

/**
 * Ngan hang cau hoi: nhung quyen sach da nap san vao may.
 *
 * Sach nam trong app duoi dang file JSON trong assets, khong tai ve tu dau ca. Ly
 * do: mot quyen sach giao khoa khong doi giua chung nam hoc, nen mot duong tai ve
 * chi them mot thu co the hong dung luc con can nop bai. Doi lai, them sach moi la
 * phai cai lai app - chap nhan duoc, vi mot nam chi them vai quyen.
 *
 * NAP LAI KHI NAO. Moi quyen mang mot so [Sach.ban]. App mo len, so voi so da ghi
 * trong prefs, khac thi nap lai ca quyen. Sua mot cau trong file ma quen tang so
 * ban thi may van dung ban cu - nho tang.
 *
 * MOT LUU Y VE BAN QUYEN: de bai chep trong day la cua sach giao khoa, nap vao may
 * cua mot nha de cham bai cho con. No khong di dau ca - khong len mang, khong sang
 * app khac. Dung dem file nay di phat.
 */
object NganHang {

    /**
     * Mot quyen sach da nap san trong app.
     *
     * Chi ba thu: ma quyen, mon nao, ten de hien ra man hinh, va file nam o dau.
     * SO BAN THI NAM TRONG CHINH FILE JSON, khong khai o day: nguoi sua de bai sua
     * file, va neu con phai nho sang day tang them mot so nua thi kieu gi cung co
     * lan quen - ma quen thi may van dung ban cu, khong bao loi gi ca.
     *
     * @param nguon ma ngan, di vao [CauHoi.id] nen KHONG duoc doi ve sau. Doi la
     *   toan bo so cai cu tro thanh vo nghia: cau da tra gio khong ai nhan ra nua.
     */
    data class Sach(
        val nguon: String,
        val mon: String,
        val ten: String,
        val file: String
    )

    /**
     * Cac quyen da nap.
     *
     * Toan 8 ca hai tap, Khoa hoc tu nhien 8 va Ngu van 8 ca hai tap, cong them sach
     * bai tap Toan 8 ca hai tap va sach bai tap KHTN 8. Cac mon khac van chay duong cu
     * - con chon "Bai khac" luc nop, va so cai lay de bai da chuan hoa lam khoa nhu tu
     * truoc den gio.
     *
     * Thu tu trong danh sach la thu tu hien ra man chon sach, nen tap mot dung
     * truoc tap hai, va SGK dung truoc SBT.
     *
     * SACH BAI TAP (SBT) LA NGUON RIENG, khong gop vao quyen SGK cung tap. SBT danh so
     * bai y kieu SGK ("1.1" co o ca hai quyen) ma de khac han, nen chung mot nguon thi
     * hai cau chung mot id. Mot lan nop chi mang mot quyen, nen AI khong bao gio thay
     * hai cau "1.1" trong cung mot danh sach.
     *
     * Ma cau SBT Toan. Bai co y a), b) ma moi y phai trinh bay loi giai thi moi y mot
     * cau ("1.3a", "1.3b"), de moi y chep kem phan de chung de doc rieng van du. Y chi
     * can tra loi ngan (chon bieu thuc nao, dung hay sai) thi de chung mot cau. Trac
     * nghiem trong On tap chuong duoc sach danh lai tu 1 o moi chuong, nen ma kem trang
     * cho khoi trung: "Trắc nghiệm 1 (tr.17)". On tap cuoi nam la "Ôn cuối năm 4a" nhu
     * SGK tap hai. Moi y lay trang in so bai, ke ca khi y c tran sang trang sau: co
     * giao "bai 1.3 trang 7" thi con phai thay du cac y khi chon trang 7.
     *
     * HINH VE CHI CON LAI TRONG CHU. Nop theo sach thi man chup bo buoc chup trang de
     * (xem [vn.huytl.homeworkgate.ui.CaptureActivity]), nen AI khong bao gio thay hinh.
     * Con cung khong co sach bai tap giay (Ba Huy noi ngay 26/9/2026): cau SBT con doc
     * ngay tren tablet, o man chon cau. Nen so lieu cua hinh ghi du trong ngoac ngay
     * sau ten hinh, "(H.5.4, hình a: ... AP = 5, PB = 3,5, AQ = 4, QC = x)", ke ca khi de
     * bat doc chinh thu do tu hinh (toa do cac diem, ten cac bo phan). Bo di thi con
     * khong co cach nao lam, va AI khong co gi de cham.
     *
     * Vai cau SBT in nham (9.14, trac nghiem 2 trang 47 va On cuoi nam 5b cua tap hai).
     * De giu nguyen chu in, them mot ngoac ghi cach hieu dung, khong ghi dap an. Thieu
     * ngoac do thi AI tu giai theo chu in, ra ket qua khong khop phuong an nao va cham
     * sai bai dung cua con.
     *
     * SBT KHTN KHAC SGK KHTN O MA CAU. SGK khong in so cau nen phai tu dat ma ("B12.C3",
     * xem duoi), con SBT in so tung cau ("11.17"), nen ma la so in va tach y theo cung
     * luat voi SBT Toan. Quyen nay nha chi co ban Word do may nhan dang chu tu ban in,
     * khong giu so trang. So trang lay tu muc luc tung cau cua VietJack, khop voi trang
     * dau moi bai trong muc luc sach. Con so trong de da soat voi loi giai cuoi sach va
     * voi ban go lai tren VietJack.
     *
     * KHTN khong in ma cau nhu sach Toan: sach chi danh so 1, 2, 3 trong tung o
     * "Câu hỏi", het o lai dem lai tu dau. Nen ma cau o day la ma tu dat, dang
     * "B12.C3" - cau thu ba cua bai 12, dem theo thu tu in trong bai. Tu dat nghia
     * la KHONG duoc xep lai hay chen them cau vao giua khi sua file: lam the la cac
     * cau sau no doi ma, va so cai khong nhan ra cau da tra gio nua. Them cau moi
     * thi them so tiep theo o cuoi bai.
     *
     * NGU VAN CUNG MA TU DAT, cung mot luat: sach dem lai tu 1 o moi muc (cau hoi
     * sau van ban, thuc hanh tieng Viet, cung co), nen "B3.C7" la cau thu bay cua
     * bai 3, dem lien qua moi muc. On tap hoc ki khong thuoc bai nao nen mang so 0:
     * "B0.C4".
     *
     * De bai Ngu van mang them hai thu sach khong in lien voi cau hoi. Cau sach co
     * danh so thi mo dau bang so do ("Câu 3."). Cau hoi ve mot van ban thi ket bang
     * ten van ban ("(văn bản Hịch tướng sĩ của Trần Quốc Tuấn)"), bai thuc hanh tieng
     * Viet thi ket bang ten muc ("(Thực hành tiếng Việt: trợ từ)"). Ca hai la cho AI
     * cham: no chi thay ma va de bai, khong thay ten muc hay ten bai. Thieu ten van
     * ban thi cau "Xác định luận đề của văn bản" khong biet van ban nao ma cham; thieu
     * so in thi khong ghep duoc voi dong "Câu 3:" con ghi trong vo. Chu in dam cung
     * mat khi chep ra chu thuong, nen cau nao hoi "tu in dam" thi ghi kem cac tu do:
     * "(in đậm: làm xe; chim mòng, nhà đi săn, viên đạn)".
     *
     * Bai 10 la bai du an doc sach: nhieu viec lam tren cuon sach con tu chon, khong
     * co van ban nao de ghi ten. Nhung cau do ket bang "(Bài 10: Sách – người bạn
     * đồng hành)" de AI biet dang cham viec gi.
     *
     * Ngu van bo han phan Noi va nghe, Doc mo rong va cac o goi y ben le van ban.
     * Noi va nghe lam mieng tren lop, anh chup khong thay gi; hai thu con lai khong
     * de ra bai viet nao de nop. Phan Thuc hanh doc chi co o huong dan va van ban,
     * khong co cau hoi danh so, nen cung khong co cau nao.
     */
    val SACH = listOf(
        Sach(
            nguon = "toan8t1",
            mon = "Toán",
            ten = "SGK Toán 8 — tập một",
            file = "nganhang/toan8t1.json"
        ),
        Sach(
            nguon = "toan8t2",
            mon = "Toán",
            ten = "SGK Toán 8 — tập hai",
            file = "nganhang/toan8t2.json"
        ),
        Sach(
            nguon = "sbttoan8t1",
            mon = "Toán",
            ten = "SBT Toán 8 — tập một",
            file = "nganhang/sbttoan8t1.json"
        ),
        Sach(
            nguon = "sbttoan8t2",
            mon = "Toán",
            ten = "SBT Toán 8 — tập hai",
            file = "nganhang/sbttoan8t2.json"
        ),
        Sach(
            nguon = "khtn8",
            mon = "Khoa học tự nhiên",
            ten = "SGK Khoa học tự nhiên 8",
            file = "nganhang/khtn8.json"
        ),
        Sach(
            nguon = "sbtkhtn8",
            mon = "Khoa học tự nhiên",
            ten = "SBT Khoa học tự nhiên 8",
            file = "nganhang/sbtkhtn8.json"
        ),
        Sach(
            nguon = "van8t1",
            mon = "Ngữ văn",
            ten = "SGK Ngữ văn 8 — tập một",
            file = "nganhang/van8t1.json"
        ),
        Sach(
            nguon = "van8t2",
            mon = "Ngữ văn",
            ten = "SGK Ngữ văn 8 — tập hai",
            file = "nganhang/van8t2.json"
        )
    )

    fun sachCua(mon: String): List<Sach> = SACH.filter { it.mon == mon }

    fun sachTheoNguon(nguon: String): Sach? = SACH.firstOrNull { it.nguon == nguon }

    /** Mon nay da co sach trong may chua. */
    fun coSach(mon: String): Boolean = sachCua(mon).isNotEmpty()

    /**
     * Nap cac quyen chua co hoac da cu vao kho. Goi luc app khoi dong.
     *
     * Chay tren luong nen: doc mot file JSON vai tram KB roi ghi hai tram dong vao
     * SQLite mat chung mot phan mua giay - khong nhieu, nhung khong co ly do gi de
     * lam viec do tren luong ve man hinh.
     */
    fun napNeuCan(context: Context) {
        val kho = KhoBai.get(context)
        val sp = Prefs.get(context).raw()
        SACH.forEach { sach ->
            val doc = runCatching { doc(context, sach) }.getOrElse { e ->
                Log.w(TAG, "khong doc duoc ${sach.file}: ${e.message}")
                return@forEach
            } ?: return@forEach

            val khoaBan = "nganhang_ban_${sach.nguon}"
            // Nap lai khi so ban trong file khac so da ghi, hoac khi trong kho khong
            // con cau nao - kho rong thi so ban co khop cung vo nghia.
            if (sp.getInt(khoaBan, 0) == doc.ban && kho.soCauCua(sach.nguon) > 0) {
                return@forEach
            }
            kho.napNguon(sach.nguon, doc.cac)
            sp.edit().putInt(khoaBan, doc.ban).apply()
            Log.i(TAG, "nap ${doc.cac.size} cau tu ${sach.ten} (ban ${doc.ban})")
        }
        // Cau con da nop qua "Bai khac" truoc khi mon do co sach: noi sang ma sach, khong
        // thi cau da tra gio thanh cau chua lam. Chay moi lan mo app, xem [KhoBai.noiCauDuongCu].
        val noi = kho.noiCauDuongCu()
        if (noi > 0) Log.i(TAG, "noi $noi cau nop qua duong cu sang ma sach")
    }

    /** Mot quyen vua doc xong: so ban ghi trong file va cac cau trong do. */
    private class Quyen(val ban: Int, val cac: List<CauHoi>)

    /**
     * Doc mot quyen tu assets. Tra ve null khi file khong dung duoc.
     *
     * Tu choi han chu khong co nap duoc phan nao hay phan do, va ba cho tu choi deu
     * la ba kieu sua file sai co that:
     *
     *  - "ban" thieu hay be hon 1: may khong con cach nao biet file da doi, nen sua
     *    de bai xong thi may van dung ban cu - im lang, khong bao gi ca. Tu choi tai
     *    day thi bo test bao do ngay, con hon de con nop bai vao mot ban sach sai.
     *  - "nguon" trong file khac ma khai ben [SACH]: gan nhu chac chan la copy file
     *    cua quyen khac ra sua ma quen doi. Nap vao thi de bai quyen nay mang ma cau
     *    quyen kia, va so cai tu do tro di noi doi.
     *  - khong co cau nao: file loi hay chep hut.
     */
    private fun doc(context: Context, sach: Sach): Quyen? {
        val chu = context.assets.open(sach.file).bufferedReader().use { it.readText() }
        val o = JSONObject(chu)

        val ban = o.optInt("ban", 0)
        if (ban < 1) {
            Log.w(TAG, "${sach.file} thieu \"ban\" (hoac be hon 1) - khong nap")
            return null
        }
        val nguonTrongFile = o.optString("nguon")
        if (nguonTrongFile != sach.nguon) {
            Log.w(TAG, "${sach.file} ghi nguon \"$nguonTrongFile\" ma dang nap cho " +
                "\"${sach.nguon}\" - khong nap")
            return null
        }

        val cacBai = o.optJSONArray("cac_bai") ?: return null
        var thuTu = 0
        val cac = buildList {
            for (i in 0 until cacBai.length()) {
                val b = cacBai.optJSONObject(i) ?: continue
                val chuong = b.optString("chuong")
                val bai = b.optString("bai")
                val cacCau = b.optJSONArray("cac_cau") ?: continue
                for (j in 0 until cacCau.length()) {
                    val c = cacCau.optJSONObject(j) ?: continue
                    val ma = c.optString("ma").trim()
                    val de = c.optString("de").trim()
                    if (ma.isEmpty() || de.isEmpty()) continue
                    add(
                        CauHoi(
                            id = "${sach.nguon}:$ma",
                            mon = sach.mon,
                            nguon = sach.nguon,
                            chuong = chuong,
                            bai = bai,
                            nhom = c.optString("nhom"),
                            ma = ma,
                            de = de,
                            trang = c.optInt("trang"),
                            dang = c.optString("dang").ifBlank { "CAU_NHO" },
                            thuTu = thuTu++
                        )
                    )
                }
            }
        }
        if (cac.isEmpty()) {
            Log.w(TAG, "${sach.file} khong co cau nao - khong nap")
            return null
        }
        return Quyen(ban, cac)
    }

    /** So cau da lam dung tren tong so cau cua mot quyen. */
    fun tienBo(context: Context, nguon: String): Pair<Int, Int> {
        val kho = KhoBai.get(context)
        val han = System.currentTimeMillis() - 365L * 24 * 60 * 60_000L
        return kho.soCauDaXongCua(nguon, han) to kho.soCauCua(nguon)
    }

    /**
     * Cau nen lam them, gop tu moi quyen cua mot mon.
     *
     * Toan co bon quyen, SGK va SBT moi thu hai tap: giua nam hoc con dang lam tap
     * hai, con phan lon cau chua lam lai nam o tap mot. Lay deu moi quyen roi tron
     * theo thu tu quyen thi danh sach lam them van co mat quyen dang hoc.
     *
     * Danh sach tron nhieu quyen, nhung MOT LAN NOP chi mang ma cua mot quyen -
     * [PhamVi] chi co mot [PhamVi.nguon]. Man chon bai se gom cac cau cung quyen voi
     * cau dau tien, giong duong on tap.
     */
    fun cauNenLamThemCuaMon(context: Context, mon: String, gioiHan: Int = 12): List<CauHoi> {
        val quyen = sachCua(mon)
        if (quyen.size <= 1) {
            val nguon = quyen.firstOrNull()?.nguon ?: return emptyList()
            return cauNenLamThem(context, nguon, gioiHan)
        }
        // Chia deu suat cho tung quyen, lam tron len de khong quyen nao bi bo troi.
        val moiQuyen = (gioiHan + quyen.size - 1) / quyen.size
        return quyen.flatMap { cauNenLamThem(context, it.nguon, moiQuyen) }.take(gioiHan)
    }

    /** Cau nen lam them: chua lam, uu tien bai con vua sai. */
    fun cauNenLamThem(context: Context, nguon: String, gioiHan: Int = 12): List<CauHoi> {
        val han = System.currentTimeMillis() - 365L * 24 * 60 * 60_000L
        return KhoBai.get(context).cacCauNenLamThem(nguon, han, gioiHan)
    }

    fun cacTrang(context: Context, nguon: String): List<TrangSach> =
        KhoBai.get(context).cacTrangCua(nguon)

    fun cacCauTheoTrang(context: Context, nguon: String, trang: List<Int>): List<CauHoi> =
        KhoBai.get(context).cacCauTheoTrang(nguon, trang)

    fun cacBai(context: Context, nguon: String): List<TenBai> =
        KhoBai.get(context).cacBaiCua(nguon)

    fun cacCau(context: Context, nguon: String, bai: String): List<CauHoi> =
        KhoBai.get(context).cacCauCua(nguon, bai)

    private const val TAG = "NganHang"
}
