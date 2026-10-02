package vn.huytl.homeworkgate.data

import android.content.Context
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.kho.TraLoi

/**
 * Bai may giao lam ngay tren tablet: chon cau cho moi luot, ghi ket qua, cong phut.
 *
 * VI SAO CO. Tu 29/9/2026 moi bai may giao - lam them, luyen cho hay vap, on lai, Giai de -
 * lam tren may bang ban phim ghep, khong chup vo nua (Le Hoa bo qua moi bai bat viet).
 * Chup anh chi con bai trong vo dan do. Luat sao o [LuatGhep], dinh dang cau o [Ghep].
 *
 * CHI CAU TRONG PHAN LE HOA DA HOC, theo moc cua [PhanHoc] (Ba Huy dan ngay 29/9/2026).
 * Tieng Anh dung moc Unit o man Do tu vung.
 *
 * PHUT VA TRAN. Lam them, luyen, Giai de chung tran [LuatCongGio.TRAN_TREN_MAY]; on lai
 * tran rieng [LuatCongGio.TRAN_ON_MOI_NGAY]. Phan vuot tran vao quy gio choi ([QuyGio]).
 * Xong luc gio ngu thi giu toi sang nhu Claude cham trong gio ngu, xem [CongSang].
 */
object LamTrenMay {

    /** Ba mon co cau lam tren may. */
    val MON = listOf("Toán", "Khoa học tự nhiên", PhanHoc.TIENG_ANH)

    /**
     * Moi bai vua sai dua bay nhieu cau chua lam len dau luot Luyen tap (Ba Huy chot 2/10/2026).
     * Cac cau con lai cua bai do nam dung cho theo thu tu bai cu toi bai moi. Xem [cauLamThem].
     */
    const val SO_CAU_BAI_VUA_SAI = 3

    private const val MOT_NGAY = 24L * 60 * 60_000L
    private const val MOT_NAM = 365L * MOT_NGAY

    enum class Loai { LAM_THEM, ON, LUYEN, GIAI_DE }

    /**
     * Mot cau da doc san cach ghep, kem tinh trang vong hien tai.
     *
     * [soLuot] la so luot tren may da xong cua cau (moi luot mot dong trong so cai). Khung
     * ghep lay no lam hat tron thu tu nut: trong mot luot so nay dung yen, luot sau thi khac,
     * xem [vn.huytl.homeworkgate.ui.KhungGhep.hatLuot].
     */
    data class Muc(
        val cau: CauHoi,
        val ghep: Ghep,
        val tinhTrang: LuatGhep.TinhTrang,
        val soLuot: Int = 0
    )

    fun muc(context: Context, cau: CauHoi): Muc? {
        if (!cau.lamTrenMay) return null
        val g = Ghep.doc(cau.ghep) ?: return null
        val cacLuot = KhoBai.get(context).cacLuotTrenMay(cau.id)
        return Muc(cau, g, LuatGhep.tinhTrang(cacLuot), cacLuot.size)
    }

    /**
     * Trong cac cau [ids], cau nao lam duoc tren may: co trong ngan hang, thuoc [MON], da
     * soan ghep. Cung dieu kien voi buoc 2 cua [cauLamThem], de cau roi danh sach can sua
     * ([SoCaiBai.canSua]) la cau con thay o Lam bai tren may.
     */
    fun lamDuocTrenMay(context: Context, ids: List<String>): Set<String> {
        if (ids.isEmpty()) return emptySet()
        return KhoBai.get(context).cacCauTheoId(ids)
            .filter { it.mon in MON && it.lamTrenMay && Ghep.doc(it.ghep) != null }
            .map { it.id }.toSet()
    }

    // ------------------------------------------------------------------ chon cau

    /**
     * Cau cho mot luot Luyen tap cua mot mon: HET cau con lam duoc luc nay (Ba Huy chot 2/10/2026).
     * Truoc ngay do moi luot toi da 10 cau ("dai hon thi con bo do giua chung"), moi bai toi da ba
     * cau moi; con lam hai cau roi thoat thi luot sau la tam cau cu noi them hai cau ke tiep, va
     * Ba Huy thay khong can noi nhu vay. Bo do giua chung khong mat gi: cau lam xong da ghi so,
     * cau chua lam van nam do, lan sau vao lai van gap. [gioiHan] chi con cho test. Thu tu:
     *  1. cau dang cho lam lai (vong dau chua du sao, da qua 24 gio);
     *  2. cau con lam sai trong bai dan do chup anh, co trong ngan hang, da qua 24 gio:
     *     sua loi cua chinh minh tren may (Ba Huy chot 29/9/2026);
     *  3. cau moi trong cac bai lop da hoc: toi da [SO_CAU_BAI_VUA_SAI] cau cua moi bai vua sai
     *     truoc, roi tu bai cu toi bai moi (so bai tang dan). Ca hai Ba Huy chot ngay 2/10/2026.
     *
     * Bo cau cua de Giai de con han, va bo muc "Ôn tập chương", "Test Yourself": de danh
     * cho Giai de. Cau con vua bam Bài này làm sau ([CauBoQua]) xep sau moi cau khac.
     *
     * [phan] khac null thi chi lay cau cua phan do (Ba Huy chot 2/10/2026: trang Luyen tap moi
     * phan mot dong, "Luyện tập Toán Đại số", "Luyện tập KHTN Hoá học"...). Ca ba buoc deu loc,
     * theo [PhanHoc.phanCuaCau]: cau muc on tap chuong lam o de tuan roi lam lai thi theo chuong.
     */
    fun cauLamThem(
        context: Context,
        mon: String,
        gioiHan: Int = Int.MAX_VALUE,
        bayGio: Long = System.currentTimeMillis(),
        phan: PhanHoc.Phan? = null
    ): List<Muc> {
        fun cuaPhan(c: CauHoi): Boolean = phan == null || PhanHoc.phanCuaCau(c) == phan
        val kho = KhoBai.get(context)
        val han = bayGio - MOT_NAM
        val trongDe = kho.cauTrongDeConHan(bayGio)
        val luot = kho.moiLuotTrenMay(han).groupBy { it.cauId }
        val boQua = CauBoQua.vuaBoQua(context, bayGio)
        val ra = mutableListOf<Muc>()
        val sau = mutableListOf<Muc>()
        val daChon = mutableSetOf<String>()
        fun them(m: Muc) {
            if (m.cau.id in boQua) {
                if (daChon.add(m.cau.id)) sau += m
            } else if (ra.size < gioiHan && daChon.add(m.cau.id)) {
                ra += m
            }
        }

        // 1. Lam lai trong vong dau.
        val cauLamLai = kho.cacCauTheoId(luot.keys.toList()).filter { it.mon == mon && it.id !in trongDe && cuaPhan(it) }
        cauLamLai.mapNotNull { c -> muc(context, c) }
            .filter { m -> m.tinhTrang.vong == 0 && LuatGhep.moLamLai(m.tinhTrang, m.ghep.sao, bayGio) }
            .forEach(::them)

        // 2. Cau sai cua bai chup anh, sua tren may.
        val saiAnh = kho.moiNhatMoiCau(han)
            .filter { !it.trenMay && !it.dung && it.cauId !in luot && bayGio >= it.luc + MOT_NGAY }
            .map { it.cauId }
        kho.cacCauTheoId(saiAnh).filter { it.mon == mon && it.id !in trongDe && cuaPhan(it) }
            .mapNotNull { muc(context, it) }.forEach(::them)

        // 3. Cau moi trong phan da hoc.
        if (ra.size < gioiHan) {
            val xong = kho.cacCauDaXong(han)
            val theoBai = NganHang.cauSbtDaHoc(context, mon)
                .filter {
                    it.lamTrenMay && it.id !in luot && it.id !in xong && it.id !in trongDe &&
                        !it.bai.startsWith("Test Yourself") && cuaPhan(it)
                }
                .groupBy { it.bai }
            // Bai vua sai (ba bai co cau sai gan nhat, ca bai chup lan cau lam tren may sai het)
            // chi dua vai cau dau len truoc, de con luyen ngay cho vua hong (Ba Huy chot 2/10/2026).
            // Ban dau tien cua ngay do (fd6f740) dua ca bai len dau: bai Toan dai toi 16 cau thi con
            // phai lam lien mot mach mot bai roi moi toi Bai 1. Cau con lai cua bai van nam dung
            // cho o vong duoi; them() bo qua cau da chon nen khong cau nao ra hai lan.
            kho.baiVuaSaiCuaMon(mon, han).filter { it in theoBai }.forEach { bai ->
                theoBai.getValue(bai).take(SO_CAU_BAI_VUA_SAI).mapNotNull { muc(context, it) }.forEach(::them)
            }
            // Bai cu truoc, bai moi sau (Ba Huy chot 2/10/2026): cau da lam xong khong ra lai o
            // buoc nay, nen con lam het bai cu thi chi con bai moi. Tu 27/9 toi 2/10/2026 thu tu
            // nguoc lai, bai gan moc truoc (bai so lon nhat da danh dau, lui dan ve bai dau), luot
            // 10 cau, moi bai 3 cau: moi lan lop hoc them bai thi bai moi chen len dau, bai cu bi
            // day lui mai. Tinh voi so cau ngay 2/10/2026, Tieng Anh hoc toi Unit 6 thi toi luot
            // thu 25 con moi gap Unit 1. De thu Bay va de on kiem tra van lay bai gan moc
            // ([GiaiDe]): hai de do on bai lop vua hoc.
            theoBai.keys.sortedBy { PhanHoc.soBai(it) ?: Int.MAX_VALUE }.forEach { bai ->
                theoBai.getValue(bai).mapNotNull { muc(context, it) }.forEach(::them)
            }
        }
        return ra + sau.take((gioiHan - ra.size).coerceAtLeast(0))
    }

    /**
     * Cau den hen on lai: vong moi theo lich 3/10/20/30 ngay, hay lam lai trong mot vong on
     * chua du sao. Chi cau lam duoc tren may; cau Ngu van va cau chua soan ghep thi thoi.
     *
     * HET cau dang cho on, khong gioi han (Ba Huy chot 2/10/2026, nhu Luyen tap [cauLamThem]);
     * truoc do moi luot toi da 10 cau. Cau den hen khong het han: hom nay khong on thi mai van
     * con, cong them cau moi den hen. [gioiHan] chi con cho test.
     *
     * SAI HET THI 24 GIO SAU MOI GAP LAI (Ba Huy chot 2/10/2026). Cau chi het den hen khi con
     * lam dung, nen luot on sai het (may da hien loi giai) de cau van den hen, ma moi lan lam mot
     * cau den hen la mot vong sao moi ([ghi]). Truoc ngay do con thoat ra bam On lai lan nua la
     * gap lai cau do ngay voi du sao, chep loi giai vua xem la duoc tron phut. Nay cau nao luot
     * tren may gan nhat sai het thi cho [LuatGhep.CHO_LAM_LAI_MS] nhu Luyen tap; qua 24 gio thi
     * hien lai, van la vong sao moi.
     *
     * [mon] khac null thi chi cau cua mon do: tu 2/10/2026 trang Luyen tap moi mon mot dong
     * "Ôn tập <môn>" (Ba Huy chot), thay cho mot dong On lai chung.
     */
    fun cauOn(
        context: Context,
        gioiHan: Int = Int.MAX_VALUE,
        bayGio: Long = System.currentTimeMillis(),
        mon: String? = null
    ): List<Muc> {
        val kho = KhoBai.get(context)
        val han = bayGio - MOT_NAM
        val denHen = kho.cacCauDenHenOn(han, bayGio)
        val cacLuot = kho.moiLuotTrenMay(han)
        val lamLaiVongOn = cacLuot.filter { it.vong >= 1 }.map { it.cauId }.distinct()
        // moiLuotTrenMay xep theo luc, nen dong cuoi cua moi cau la luot gan nhat.
        val vuaSaiHet = cacLuot.groupBy { it.cauId }
            .filterValues { ds -> ds.last().let { !it.dung && bayGio < it.luc + LuatGhep.CHO_LAM_LAI_MS } }
            .keys
        val cac = kho.cacCauTheoId((denHen + lamLaiVongOn).distinct())
            .filter { it.mon in MON && (mon == null || it.mon == mon) && it.id !in vuaSaiHet }
            .mapNotNull { muc(context, it) }
            .filter { m ->
                m.cau.id in denHen ||
                    (m.tinhTrang.vong >= 1 && LuatGhep.moLamLai(m.tinhTrang, m.ghep.sao, bayGio))
            }
        return CauBoQua.sapSau(cac, CauBoQua.vuaBoQua(context, bayGio)) { it.cau.id }.take(gioiHan)
    }

    /**
     * So cau dang cho on, cho dong "Ôn tập <môn>" o trang Luyen tap ([mon]) va dong "Luyện tập"
     * ngoai man chinh (ca ba mon).
     */
    fun soCauOn(context: Context, bayGio: Long = System.currentTimeMillis(), mon: String? = null): Int =
        runCatching { cauOn(context, Int.MAX_VALUE, bayGio, mon).size }.getOrDefault(0)

    /**
     * So cau cua [mon] da on hom nay: luot tren may thuoc mot vong on (vong 1 tro di) tu nua dem.
     * Cong voi [soCauOn] la so cau den hen trong ngay, cho dong "Ôn tập <môn>" ghi "2/5" (Ba Huy
     * chot 2/10/2026): cau on xong thi het den hen, nen chi dem cau con lai thi so cu tut dan.
     */
    fun soDaOnHomNay(context: Context, mon: String, bayGio: Long = System.currentTimeMillis()): Int =
        runCatching {
            KhoBai.get(context).moiLuotTrenMay(nuaDem(bayGio))
                .filter { it.vong >= 1 && it.mon == mon }
                .map { it.cauId }.distinct().size
        }.getOrDefault(0)

    /**
     * So cau da lam dung it nhat mot lan tren tong so cau lam tren may, cho dong Luyen tap cua
     * trang Luyen tap va dong nho o man lam bai ("Đại số: đã làm 12/155 câu", Ba Huy chot
     * 2/10/2026). Dem tren ca phan, ke ca bai lop chua hoc: tong dung yen, khong nhay khi con
     * danh dau them bai. Khoa la [PhanHoc.Phan.ma]; Tieng Anh khong chia phan nen khoa la "".
     *
     * Chi cau sach bai tap cua cac bai co so: muc "Ôn tập chương", "Luyện tập chung", "Test
     * Yourself" danh cho Giai de, khong bao gio ra o dong Luyen tap nen khong dem.
     */
    fun demTheoPhan(context: Context, mon: String): Map<String, Pair<Int, Int>> {
        val kho = KhoBai.get(context)
        val dung = kho.cacCauDaXong(0L)
        val cac = NganHang.sachBaiTapCua(mon).flatMap { kho.cacCauCuaNguon(it.nguon) }
            .filter { it.lamTrenMay && !it.bai.startsWith("Test Yourself") }
        val ra = HashMap<String, Pair<Int, Int>>()
        cac.forEach { c ->
            val so = PhanHoc.soBai(c.bai) ?: return@forEach
            val khoa = if (mon == PhanHoc.TIENG_ANH) "" else PhanHoc.cuaBai(mon, so)?.ma ?: return@forEach
            val (d, t) = ra[khoa] ?: (0 to 0)
            ra[khoa] = (d + if (c.id in dung) 1 else 0) to (t + 1)
        }
        return ra
    }

    /** Mot cau da lam dung tren may, kem luot dung gan nhat cua no. */
    data class CauDung(val cau: CauHoi, val luot: TraLoi)

    /**
     * Cac cau cua [mon] da lam dung tren may, moi cau mot dong voi luot dung GAN NHAT: cau trong
     * luyen tap, on tap va cau trong de (de tuan, de on kiem tra, de thi thu), deu lam bang phim
     * ghep. Cho man xem lai cau da lam ([vn.huytl.homeworkgate.ui.CauDaLamActivity], Ba Huy chot
     * 2/10/2026). Cau chup anh (bai co giao) khong vao, chung da co man Bai da cham; luot het sao
     * ma van sai (hien loi giai) khong tinh la dung.
     *
     * Luot gan nhat chu khong phai luot nhieu sao nhat: man xem lai ghi gio lam, va con can thay
     * lan lam moi nhat cua minh.
     */
    fun cauDaLamDung(context: Context, mon: String): List<CauDung> {
        val kho = KhoBai.get(context)
        val moiCau = kho.moiLuotTrenMay(0L).filter { it.dung && it.mon == mon }
            .groupBy { it.cauId }
            .mapValues { (_, cac) -> cac.maxBy { it.luc } }
        if (moiCau.isEmpty()) return emptyList()
        val theoId = kho.cacCauTheoId(moiCau.keys.toList()).associateBy { it.id }
        return moiCau.mapNotNull { (id, luot) -> theoId[id]?.let { CauDung(it, luot) } }
    }

    /** So cau cua [mon] da lam dung tren may, cho dong "Câu đã làm đúng" o trang Luyen tap. */
    fun soCauDaLamDung(context: Context, mon: String): Int = runCatching {
        KhoBai.get(context).moiLuotTrenMay(0L).filter { it.dung && it.mon == mon }
            .map { it.cauId }.distinct().size
    }.getOrDefault(0)

    private fun nuaDem(bayGio: Long): Long = java.util.Calendar.getInstance().apply {
        timeInMillis = bayGio
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Cau luyen cho hay vap theo nhan loi, chi cau lam duoc tren may va chua lam tren may. */
    fun cauLuyen(context: Context, nhan: String, gioiHan: Int = KhoBai.SO_CAU_LUYEN): List<Muc> {
        val kho = KhoBai.get(context)
        val cac = kho.cacCauLuyenTheoLoi(nhan, gioiHan = gioiHan * 3)
            .filter { kho.cacLuotTrenMay(it.id).isEmpty() }
            .mapNotNull { muc(context, it) }
        return CauBoQua.sapSau(cac, CauBoQua.vuaBoQua(context)) { it.cau.id }.take(gioiHan)
    }

    /**
     * So sao tot nhat cua cau trong vong dang lam, cho dong "Lần trước" o man lam bai (Ba Huy
     * chot 30/9/2026). null la khong hien: cau chua lam tren may lan nao, hay luot nay mo vong
     * moi (on lai den hen: vong moi duoc tron so sao, khong co moc nao de vuot).
     *
     * La sao TOT NHAT chu khong phai luot gan nhat, vi phut chi cong phan hon lan tot nhat
     * ([LuatGhep.phutCong]). Luot dau 2 sao, luot sau 1 sao: hien "lần trước 1 sao" thi con tuong
     * lan nay duoc 2 sao la co phut, ma that ra +0. Hien moc 2 thi "+N" luon bang so sao vuot moc.
     *
     * Dung chung dieu kien vong moi voi [ghi] ([moVongMoi]), de dong nay khong noi khac luat.
     */
    fun saoLanTruoc(
        context: Context,
        muc: Muc,
        loai: Loai,
        bayGio: Long = System.currentTimeMillis()
    ): Int? {
        if (muc.tinhTrang.vong < 0) return null
        if (moVongMoi(KhoBai.get(context), muc.cau.id, loai, bayGio)) return null
        return muc.tinhTrang.totNhat
    }

    /** Luot nay co mo vong sao moi khong: chi khi on lai mot cau dang den hen. */
    private fun moVongMoi(kho: KhoBai, cauId: String, loai: Loai, bayGio: Long): Boolean =
        loai == Loai.ON && kho.denHenOn(cauId, bayGio - MOT_NAM, bayGio)

    // ------------------------------------------------------------------ ghi va cong

    /** Ket qua ghi mot cau: so sao, phut cap ngay, phut vao quy, phut giu toi sang. */
    data class Ghi(val sao: Int, val phutCap: Int, val phutQuy: Int, val phutGiu: Int, val dong: TraLoi)

    /**
     * Ghi mot luot vua xong va cong phut theo tran rieng.
     *
     * [loai] ON va cau dang den hen thi mo vong moi (sao tinh lai tu dau); con lai lam tiep
     * vong hien tai, chi cong phan hon lan tot nhat cua vong do.
     *
     * CAP TRUOC, GHI SO SAU nhu moi duong cap gio khac; nhung gio ngu thi khong cap duoc ma
     * van ghi so, phut giu lai toi sang ([CongSang]) - con lam dung that thi khong mat.
     */
    fun ghi(
        context: Context,
        muc: Muc,
        luot: LuatGhep.Luot,
        traLoi: String,
        loai: Loai,
        deId: String = "",
        bayGio: Long = System.currentTimeMillis(),
        congNgay: Boolean = true
    ): Ghi {
        val kho = KhoBai.get(context)
        val tt = LuatGhep.tinhTrang(kho.cacLuotTrenMay(muc.cau.id))
        val vongMoi = moVongMoi(kho, muc.cau.id, loai, bayGio)
        val vong = when {
            tt.vong < 0 -> 0
            vongMoi -> tt.vong + 1
            else -> tt.vong
        }
        val totNhatCu = if (tt.vong < 0 || vongMoi) 0 else tt.totNhat
        val sao = LuatGhep.saoKhiXong(luot)
        val lamRa = LuatGhep.phutCong(sao, totNhatCu)
        val onTap = vong >= 1
        val (cap, quy) = if (!congNgay) {
            0 to 0
        } else {
            val daDung = if (onTap) SoCaiBai.phutOnHomNay(context, bayGio)
            else SoCaiBai.phutTrenMayHomNay(context, bayGio)
            val tran = if (onTap) LuatCongGio.TRAN_ON_MOI_NGAY else LuatCongGio.TRAN_TREN_MAY
            LuatGhep.chiaTran(lamRa, daDung, tran)
        }

        var giu = 0
        if (cap > 0) {
            val gate = GateStore(context)
            val nhan = nhanCong(loai, onTap)
            val duoc = if (gate.trongGioNgu(bayGio)) null else gate.congGioHoc(cap, nhanCho = nhan)
            if (duoc == null) {
                CongSang.them(context, cap, now = bayGio)
                giu = cap
            }
        }

        val dong = TraLoi(
            cauId = muc.cau.id,
            mon = muc.cau.mon,
            ma = muc.cau.ma,
            de = muc.cau.de,
            ketQua = traLoi,
            onTap = onTap,
            dung = luot.dung,
            phut = cap,
            nhanXet = "",
            luc = bayGio,
            deId = deId,
            trenMay = true,
            sao = sao,
            saoToiDa = muc.ghep.sao,
            vong = vong,
            lanSai = luot.lanSai
        )
        kho.ghiTraLoi(dong)
        CauBoQua.xoa(context, muc.cau.id)
        if (quy > 0) QuyGio.them(context, quy, nhanCong(loai, onTap))
        runCatching { DongBo.daySoCai(context, listOf(dong)) }
        return Ghi(sao, cap, quy, giu, dong)
    }

    /** Nhan cua dong cong gio va dong vao quy: Ba Huy doc o nhat ky. */
    private fun nhanCong(loai: Loai, onTap: Boolean): String = when {
        onTap -> "Ôn lại"
        loai == Loai.GIAI_DE -> "Giải đề"
        else -> "Luyện tập"
    }

    /**
     * Dong nhat ky cho ca mot luot lam, ghi mot lan luc thoat man chu khong tung cau. Cau con
     * bam Bài này làm sau ([boQua]) ke them o cuoi dong, de Ba Huy biet cau nao con chua lam duoc.
     */
    fun ghiNhatKy(context: Context, mon: String, loai: Loai, cac: List<Ghi>, boQua: List<CauHoi> = emptyList()) {
        if (cac.isEmpty() && boQua.isEmpty()) return
        val ten = when (loai) {
            Loai.ON -> "Ôn lại"
            Loai.LUYEN -> "Luyện chỗ hay vấp"
            Loai.GIAI_DE -> "Giải đề"
            Loai.LAM_THEM -> "Luyện tập"
        }
        val tenMon = if (mon.isBlank()) "" else " " + GiaiDe.tenMon(mon)
        val phan = mutableListOf<String>()
        if (cac.isNotEmpty()) {
            val sao = cac.sumOf { it.sao }
            val toiDa = cac.sumOf { it.dong.saoToiDa }
            val phut = cac.sumOf { it.phutCap }
            val quy = cac.sumOf { it.phutQuy }
            phan += "${cac.size} câu, $sao/$toiDa sao, +$phut phút" + if (quy > 0) " (vào quỹ $quy phút)" else ""
        }
        val cauBo = boQua.distinctBy { it.id }
        if (cauBo.isNotEmpty()) {
            phan += "bỏ qua ${cauBo.size} câu: " + cauBo.take(MA_BO_QUA_TOI_DA).joinToString(", ") { it.ma } +
                if (cauBo.size > MA_BO_QUA_TOI_DA) "…" else ""
        }
        DayLog.add(context, "$ten$tenMon: " + phan.joinToString(", "))
    }

    /** Dong nhat ky ke toi da bay nhieu ma cau bo qua. */
    private const val MA_BO_QUA_TOI_DA = 5
}
