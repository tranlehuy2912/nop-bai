package vn.huytl.homeworkgate.data

import android.content.Context
import org.json.JSONObject
import vn.huytl.homeworkgate.kho.DeGiai

/**
 * Dong ho Giai de: con da ngoi o man de bao lau. Thoat ra man de thi dong ho dung, vao lai thi
 * chay tiep. Ba Huy chon ngay 1/10/2026, cho ca ba loai de (de tuan, on kiem tra, de thi thu).
 *
 * VI SAO CO. Truoc 1/10/2026 dong ho chi co moc [DeGiai.batDau], va moi cho hien gio deu lay gio
 * that tru moc do: dong ho tren man de, dong "Đang làm, còn N phút" o man chinh, dong ket qua sau
 * khi nop, dong tom tat sang nhat ky cua Ba Huy. Bam Bat dau roi bo do toi hom sau thi man chinh
 * ghi "Đang làm, quá 1078 phút" (thay tren may ao ngay 1/10/2026). So phut nay khong doi ra sao,
 * diem hay gio choi; no chi noi con lam bao lau, nen phai la luc con ngoi o man de.
 *
 * MOI DE MOT MUC: so ms da cong ([Muc.daLamMs]) va luc vao man de gan nhat ([Muc.vaoLuc], 0 la
 * dang dung). Man de goi [vao] luc hien len va [roi] luc bi che hay dong; bam Bat dau va Nop bai
 * di qua [batDau] va [roi] trong [GiaiDe].
 *
 * DE BAT DAU TRUOC 1/10/2026 khong co muc nao. Dang lam do thi dem lai tu 0, tinh tu lan vao man
 * de dau tien (Ba Huy chon): so cu cong ca luc con o ngoai, giu lai la giu mot so sai. Da nop thi
 * giu cach tinh cu, gio that tu luc bat dau toi luc nop, de dong tom tat cua de cu khong doi.
 *
 * MAY SAP hay het pin luc dang o man de thi [Muc.vaoLuc] nam lai, khong ai goi [roi]. Lan vao sau
 * bo doan do: khong biet dong ho dung luc nao, ma cong toi luc vao lai la dem ca luc may tat. Cho
 * khac man de (man chinh, tom tat) chi doc so da cong, vi luc do man de khong o truoc mat con.
 *
 * File prefs rieng, khong ma hoa, y nhu [CongSang]: day khong phai bi mat, va ghi vao file chinh
 * thi moi lan con ra vao man de lai danh thuc DongBo (xem DongBo.KHOA_BO_QUA).
 */
object DongHoDe {

    private const val FILE = "dong_ho_de"

    /** Muc khong ghi gi them sau bay nhieu ngay thi bo, xem [luu]. De het han sau vai tuan. */
    private const val NGAY_GIU = 120

    data class Muc(val daLamMs: Long, val vaoLuc: Long)

    /** Muc cua de [deId]. null la de chua bat dau, hay bat dau truoc 1/10/2026. */
    fun muc(context: Context, deId: String): Muc? {
        val chu = sp(context).getString(deId, null) ?: return null
        return runCatching {
            val o = JSONObject(chu)
            Muc(o.optLong("d"), o.optLong("v"))
        }.getOrNull()
    }

    /** Con bam Bat dau, man de dang o truoc mat: dong ho chay tu [bayGio]. */
    @Synchronized
    fun batDau(context: Context, deId: String, bayGio: Long) {
        luu(context, deId, Muc(0L, bayGio), bayGio)
    }

    /**
     * Man de vua hien len voi [de] dang lam: dong ho chay tiep tu [bayGio].
     *
     * Muc con [Muc.vaoLuc] tu lan truoc (may sap giua chung) thi bo doan do. De cu chua co muc
     * thi dem tu 0.
     */
    @Synchronized
    fun vao(context: Context, de: DeGiai, bayGio: Long = System.currentTimeMillis()) {
        if (!de.daBatDau || de.daNop) return
        luu(context, de.id, Muc(muc(context, de.id)?.daLamMs ?: 0L, bayGio), bayGio)
    }

    /**
     * Man de bi che hay dong, hay con vua bam Nop bai: cong doan tu lan vao gan nhat, dong ho
     * dung. Luc nop thi goi voi ban [de] truoc khi ghi luc nop.
     */
    @Synchronized
    fun roi(context: Context, de: DeGiai, bayGio: Long = System.currentTimeMillis()) {
        if (!de.daBatDau || de.daNop) return
        val m = muc(context, de.id) ?: return
        if (m.vaoLuc <= 0L) return
        luu(context, de.id, Muc(m.daLamMs + (bayGio - m.vaoLuc).coerceAtLeast(0L), 0L), bayGio)
    }

    /**
     * So ms con da lam [de].
     *
     * [dangOMan] true chi o dong ho cua man de, luc man do dang hien: cong them doan tu lan vao
     * gan nhat. Cho khac de false, xem doan MAY SAP o tren.
     */
    fun daLamMs(
        context: Context,
        de: DeGiai,
        bayGio: Long = System.currentTimeMillis(),
        dangOMan: Boolean = false
    ): Long {
        if (!de.daBatDau) return 0L
        val m = muc(context, de.id)
            ?: return if (de.daNop) (de.nopLuc - de.batDau).coerceAtLeast(0L) else 0L
        val dangChay = if (dangOMan && !de.daNop && m.vaoLuc > 0L) {
            (bayGio - m.vaoLuc).coerceAtLeast(0L)
        } else 0L
        return m.daLamMs + dangChay
    }

    /** Xoa muc cua mot de. Chi test goi, de don sau khi chay. */
    internal fun xoa(context: Context, deId: String) {
        sp(context).edit().remove(deId).commit()
    }

    /** Ghi muc cua mot de, va bo muc cua de khong ai dung toi trong [NGAY_GIU] ngay. */
    private fun luu(context: Context, deId: String, m: Muc, bayGio: Long) {
        val sp = sp(context)
        val ed = sp.edit().putString(
            deId, JSONObject().put("d", m.daLamMs).put("v", m.vaoLuc).put("l", bayGio).toString()
        )
        val han = bayGio - NGAY_GIU * 24L * 60 * 60_000L
        sp.all.forEach { (k, v) ->
            val l = runCatching { JSONObject(v as String).optLong("l") }.getOrDefault(0L)
            if (k != deId && l in 1 until han) ed.remove(k)
        }
        ed.commit()
    }

    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
