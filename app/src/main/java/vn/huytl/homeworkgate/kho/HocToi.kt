package vn.huytl.homeworkgate.kho

import android.content.Context
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.Prefs

/**
 * Lop da hoc toi dau trong tung bo: toi bai nao voi bo the hoc thuoc, toi Unit nao voi
 * bo tu vung. Le Hoa tu bam chon, may chi hoi trong phan do.
 *
 * VI SAO CO. Truoc 25/9/2026 hai duong tu lo lay. Bo the khong loc gi ca: lay the den
 * luot tu dau bo xuong theo thu tu file, nen vai luot la toi cong thuc cua bai o
 * truong chua day. Bo tu vung thi doan Unit theo lich: dem tiet Tieng Anh tu ngay khai
 * giang, chia deu muoi hai Unit cho ca nam, lui mot tuan cho chac. Lop that khong chay
 * deu, va lop day cham hon nhip do mot chut la may hoi tu chua hoc. Ba Huy chon bo han
 * cach doan, de con tu noi minh dang o dau.
 *
 * CHUA CHON THI BAT CHON, khong doan thay. Hai man deu khong cho bat dau mot bo chua
 * chon: bam vao la hien hop chon truoc, xem [vn.huytl.homeworkgate.ui.ChonHocToi].
 *
 * CON DOI LUC NAO CUNG DUOC, KHONG CAN PIN. Chon cao hon lop that thi chi gap cau kho
 * hon, chon thap hon thi it cau de kiem phut, va hai duong deu co tran phut rieng moi
 * ngay, nen khong co cho nao de lach. Moi lan doi ghi mot dong nhat ky, dong do di sang
 * Bang dieu khien cung nhat ky trong ngay, de Ba Huy thay con dang o dau.
 *
 * NAM TRONG FILE PREFS CHINH. Mat file (Keystore hong, cai lai app) thi con chi phai chon
 * lai mot lan. Khoa nay vai tuan moi ghi mot lan nen khong can vao KHOA_BO_QUA cua
 * [vn.huytl.homeworkgate.dongbo.DongBo]: DongBo nghe thay, dung lai ban trang thai, thay
 * giong ban vua day thi bo luot ghi.
 */
object HocToi {

    /**
     * Gia tri "chua hoc toi bai nao" cua bo the. Khac voi chua chon, la khi khong co khoa.
     *
     * Can lua chon nay vi bo Toan hien chi co phan hang dang thuc: lop chua toi do thi con
     * phai noi duoc la chua hoc, chu bat chon mot bai la bat hoi dung thu chua hoc.
     */
    const val CHUA_HOC_BAI_NAO = ""

    /** Unit 0 cua bo tu: chua hoc Unit nao. Cung ly do voi [CHUA_HOC_BAI_NAO]. */
    const val CHUA_HOC_UNIT_NAO = 0

    /** Bai cuoi lop da hoc trong bo the [bo], [CHUA_HOC_BAI_NAO], hoac null khi chua chon. */
    fun baiCua(context: Context, bo: String): String? {
        val sp = Prefs.get(context).raw()
        val khoa = khoaThe(bo)
        return if (sp.contains(khoa)) sp.getString(khoa, CHUA_HOC_BAI_NAO).orEmpty() else null
    }

    /**
     * Ghi bai con vua chon. Chon lai dung cai dang co thi khong ghi gi, ke ca nhat ky.
     *
     * commit() chu khong apply(): con chon xong la bat dau luot ngay, va tien trinh bi
     * giet truoc khi apply() kip xuong dia thi lan sau may lai hoi.
     */
    fun datBai(context: Context, bo: BoThe.Bo, bai: String) {
        val cu = baiCua(context, bo.bo)
        if (cu == bai) return
        ghiBai(context, bo.bo, bai)
        ghiNhatKy(context, bo.ten, moTaBai(bai), cu?.let(::moTaBai))
    }

    /** Unit cuoi lop da hoc trong bo tu [bo], [CHUA_HOC_UNIT_NAO], hoac null khi chua chon. */
    fun unitCua(context: Context, bo: String): Int? {
        val sp = Prefs.get(context).raw()
        val khoa = khoaTu(bo)
        return if (sp.contains(khoa)) sp.getInt(khoa, CHUA_HOC_UNIT_NAO) else null
    }

    /** Ghi Unit con vua chon, y het [datBai]. */
    fun datUnit(context: Context, bo: BoTuVung.Bo, unit: Int) {
        val cu = unitCua(context, bo.bo)
        if (cu == unit) return
        ghiUnit(context, bo.bo, unit)
        ghiNhatKy(context, bo.ten, moTaUnit(unit), cu?.let(::moTaUnit))
    }

    /*
     * Ba ham duoi ghi thang, khong qua nhat ky, va chi test goi. Test ma di qua
     * [datBai] thi moi lan chay de lai nam dong "Le Hoa chon Bo thu..." trong nhat ky
     * hom do, va dong do sang Bang dieu khien y nhu con chon that. Lan chay 25/9/2026
     * tren may ao da de lai dung nam dong nhu vay.
     */

    internal fun ghiBai(context: Context, bo: String, bai: String) {
        Prefs.get(context).raw().edit().putString(khoaThe(bo), bai).commit()
    }

    internal fun ghiUnit(context: Context, bo: String, unit: Int) {
        Prefs.get(context).raw().edit().putInt(khoaTu(bo), unit).commit()
    }

    /** Xoa lua chon cua [bo] ca hai phia, the va tu, de test bat dau tu "chua chon". */
    internal fun xoa(context: Context, bo: String) {
        Prefs.get(context).raw().edit().remove(khoaThe(bo)).remove(khoaTu(bo)).commit()
    }

    /** "đã học tới Bài 9. Base. Thang pH", hay "chưa học tới bài nào". */
    fun moTaBai(bai: String): String =
        if (bai == CHUA_HOC_BAI_NAO) "chưa học tới bài nào" else "đã học tới $bai"

    /** "đã học tới Unit 3", hay "chưa học Unit nào". */
    fun moTaUnit(unit: Int): String =
        if (unit <= CHUA_HOC_UNIT_NAO) "chưa học Unit nào" else "đã học tới Unit $unit"

    /**
     * Mot dong nhat ky moi lan doi: con chon gi, va truoc do la gi.
     *
     * Ghi ca cai cu vi thu Ba Huy can nhin la buoc nhay. Tu Bai 4 len Bai 9 trong mot
     * toi la chuyen dang hoi con, con tu Bai 4 len Bai 6 sau hai tuan thi khong.
     */
    private fun ghiNhatKy(context: Context, tenBo: String, moi: String, cu: String?) {
        val ten = context.getString(R.string.child_name)
        val truoc = if (cu == null) "lần đầu chọn" else "trước đó $cu"
        DayLog.add(context, "$ten chọn $tenBo: $moi ($truoc)")
    }

    private fun khoaThe(bo: String) = "hoc_toi_the_$bo"
    private fun khoaTu(bo: String) = "hoc_toi_tu_$bo"
}
