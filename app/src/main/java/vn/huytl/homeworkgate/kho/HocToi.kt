package vn.huytl.homeworkgate.kho

import android.content.Context
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.Prefs

/**
 * Cac bai lop da hoc cua tung mon: Toan, KHTN theo so bai SGK, Tieng Anh theo so Unit. Le Hoa
 * tu danh dau, may chi giao va chi hoi trong cac bai do.
 *
 * VI SAO CO. Truoc 25/9/2026 hai duong tu lo lay. Bo the khong loc gi ca: lay the den
 * luot tu dau bo xuong theo thu tu file, nen vai luot la toi cong thuc cua bai o
 * truong chua day. Bo tu vung thi doan Unit theo lich: dem tiet Tieng Anh tu ngay khai
 * giang, chia deu muoi hai Unit cho ca nam, lui mot tuan cho chac. Lop that khong chay
 * deu, va lop day cham hon nhip do mot chut la may hoi tu chua hoc. Ba Huy chon bo han
 * cach doan, de con tu noi minh dang o dau.
 *
 * CHON NHIEU BAI, MOI MON MOT TAP (Ba Huy chot 2/10/2026). Tu 25/9 toi 2/10/2026 moi phan hoc
 * mot moc "lop da hoc toi Bai N" (Toan hai phan, KHTN ba phan, xem [PhanHoc]), Tieng Anh mot
 * moc "toi Unit N", va moi bai tu bai dau toi Bai N la da hoc. Lop khong luon hoc dung thu tu
 * sach, va hai mon co phan thi con phai doi nam moc o nam dong. Nay moi mon mot tap bai con
 * danh dau ([daHoc]), o hop chon nhieu o ([vn.huytl.homeworkgate.ui.ChonHocToi.hoiMon]), tu
 * dong dau moi khu mon o trang Luyen tap.
 *
 * MOC CU TU CHUYEN. Lan dau doc mot mon ma chua co khoa moi thi lay moc cu cua cac phan (Tieng
 * Anh: moc Unit cua bo tu) va danh dau san moi bai tu bai dau toi bai cu, roi ghi xuong khoa
 * moi (Ba Huy chon tu chuyen, khong bat con chon lai). Khoa cu de nguyen, khong ai doc nua.
 *
 * CHUA CHON THI BAT CHON, khong doan thay: [daHoc] tra null, va cac man hoi con truoc khi
 * vao. Tap rong la con noi chua hoc bai nao, khac voi chua chon.
 *
 * CON DOI LUC NAO CUNG DUOC, KHONG CAN PIN. Chon nhieu hon lop that thi chi gap cau kho
 * hon, chon it hon thi it cau de kiem phut, va cac duong deu co tran phut rieng moi ngay,
 * nen khong co cho nao de lach. Moi lan doi ghi mot dong nhat ky, dong do di sang Bang
 * dieu khien cung nhat ky trong ngay, de Ba Huy thay con dang o dau.
 *
 * NAM TRONG FILE PREFS CHINH. Mat file (Keystore hong, cai lai app) thi con chi phai chon
 * lai mot lan. Khoa nay vai tuan moi ghi mot lan nen khong can vao KHOA_BO_QUA cua
 * [vn.huytl.homeworkgate.dongbo.DongBo]: DongBo nghe thay, dung lai ban trang thai, thay
 * giong ban vua day thi bo luot ghi.
 */
object HocToi {

    /**
     * Gia tri "chua hoc bai nao cua bo nay" o [BoDaNap.hocToi], khac voi chua chon (null).
     * Con chon cac bai cua phan khac ma chua co bai nao trong bo the.
     */
    const val CHUA_HOC_BAI_NAO = ""

    /** Cac bai (Tieng Anh: Unit) lop da hoc cua [mon], null khi con chua chon lan nao. */
    fun daHoc(context: Context, mon: String): Set<Int>? {
        val ma = maMon(mon) ?: return null
        val sp = Prefs.get(context).raw()
        val khoa = khoaDaHoc(ma)
        if (sp.contains(khoa)) return docTap(sp.getString(khoa, "").orEmpty())
        val cu = mocCu(context, mon) ?: return null
        // Ghi luon de lan sau doc thang khoa moi, va de con doi bai thi so voi tap nay.
        sp.edit().putString(khoa, viet(cu)).commit()
        return cu
    }

    /**
     * Ghi tap bai con vua danh dau, kem mot dong nhat ky. Chon lai dung tap dang co thi khong
     * ghi gi, ke ca nhat ky.
     *
     * commit() chu khong apply(): con chon xong la bat dau luot ngay, va tien trinh bi
     * giet truoc khi apply() kip xuong dia thi lan sau may lai hoi.
     */
    fun datDaHoc(context: Context, mon: String, cac: Set<Int>) {
        val cu = daHoc(context, mon)
        val moi = cac.filter { it > 0 }.toSortedSet()
        if (cu == moi) return
        ghiDaHoc(context, mon, moi)
        ghiNhatKy(context, mon, moi, cu)
    }

    /*
     * Hai ham duoi ghi thang, khong qua nhat ky, va chi test goi. Test ma di qua
     * [datDaHoc] thi moi lan chay de lai vai dong "Le Hoa chon..." trong nhat ky hom do,
     * va dong do sang Bang dieu khien y nhu con chon that. Lan chay 25/9/2026 tren may ao
     * da de lai dung nam dong nhu vay.
     */

    internal fun ghiDaHoc(context: Context, mon: String, cac: Set<Int>) {
        val ma = maMon(mon) ?: return
        Prefs.get(context).raw().edit().putString(khoaDaHoc(ma), viet(cac)).commit()
    }

    /** Xoa lua chon cua [mon], ca khoa cu, de test bat dau tu "chua chon". */
    internal fun xoa(context: Context, mon: String) {
        val ma = maMon(mon) ?: return
        val ed = Prefs.get(context).raw().edit().remove(khoaDaHoc(ma))
        khoaCu(mon).forEach { ed.remove(it) }
        ed.commit()
    }

    /** "Bài 1–9, 12", "Unit 1–3", hay "chưa học bài nào". */
    fun moTa(mon: String, cac: Set<Int>): String = when {
        cac.isEmpty() -> if (mon == PhanHoc.TIENG_ANH) "chưa học Unit nào" else "chưa học bài nào"
        mon == PhanHoc.TIENG_ANH -> "Unit " + PhanHoc.gon(cac)
        else -> "Bài " + PhanHoc.gon(cac)
    }

    /**
     * Mot dong nhat ky moi lan doi: con chon gi, va truoc do la gi.
     *
     * Ghi ca cai cu vi thu Ba Huy can nhin la buoc nhay. Tu Bai 4 len Bai 9 trong mot
     * toi la chuyen dang hoi con, con tu Bai 4 len Bai 6 sau hai tuan thi khong.
     */
    private fun ghiNhatKy(context: Context, mon: String, moi: Set<Int>, cu: Set<Int>?) {
        val ten = context.getString(R.string.child_name)
        val truoc = if (cu == null) "lần đầu chọn" else "trước đó ${moTa(mon, cu)}"
        DayLog.add(context, "$ten chọn bài đã học ${PhanHoc.tenNgan(mon)}: ${moTa(mon, moi)} ($truoc)")
    }

    /**
     * Moc cu (truoc 2/10/2026) cua mon, doi ra tap bai: moi phan tung duoc chon gop moi bai
     * tu bai dau toi bai cu. null khi chua phan nao cua mon tung duoc chon. Bai cu khong doc ra
     * so (file doi ten bai) thi bo phan do, nhu luc truoc may cung coi la chua chon.
     */
    private fun mocCu(context: Context, mon: String): Set<Int>? {
        val sp = Prefs.get(context).raw()
        if (mon == PhanHoc.TIENG_ANH) {
            val khoa = khoaTuCu(PhanHoc.BO_TIENG_ANH)
            if (!sp.contains(khoa)) return null
            val unit = sp.getInt(khoa, 0)
            return if (unit <= 0) emptySet() else (1..unit).toSortedSet()
        }
        var coChon = false
        val ra = sortedSetOf<Int>()
        PhanHoc.cuaMon(mon).forEach { p ->
            val khoa = khoaTheCu(p.ma)
            if (!sp.contains(khoa)) return@forEach
            val bai = sp.getString(khoa, "").orEmpty()
            val den = if (bai.isEmpty()) 0 else PhanHoc.soBai(bai) ?: return@forEach
            coChon = true
            ra += p.cacSoToi(den)
        }
        return if (coChon) ra else null
    }

    private fun khoaCu(mon: String): List<String> =
        if (mon == PhanHoc.TIENG_ANH) listOf(khoaTuCu(PhanHoc.BO_TIENG_ANH))
        else PhanHoc.cuaMon(mon).map { khoaTheCu(it.ma) }

    /** Ma mon trong khoa prefs. Mon khong co bai lam tren may thi null. */
    private fun maMon(mon: String): String? = when (mon) {
        PhanHoc.TOAN -> "toan"
        PhanHoc.KHTN -> "khtn"
        PhanHoc.TIENG_ANH -> "anh"
        else -> null
    }

    private fun docTap(chu: String): Set<Int> =
        chu.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.toSortedSet()

    private fun viet(cac: Set<Int>): String = cac.filter { it > 0 }.sorted().joinToString(",")

    private fun khoaDaHoc(ma: String) = "da_hoc_$ma"

    /** Khoa cua moc cu, truoc 2/10/2026: moi phan (ma phan trung ma bo the) va moi bo tu. */
    private fun khoaTheCu(ma: String) = "hoc_toi_the_$ma"
    private fun khoaTuCu(bo: String) = "hoc_toi_tu_$bo"
}
