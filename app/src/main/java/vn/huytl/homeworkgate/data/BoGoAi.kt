package vn.huytl.homeworkgate.data

/**
 * Dung lai cau con go vao app AI, tu dong chu roi rac ma dich vu tro nang ban ve.
 *
 * TAI SAO CAN LOP RIENG: su kien go chu khong ban ra "day la cau hoan chinh". No
 * ban ra tung anh chup cua o nhap sau moi phim - "g", "gi", "gia", "giai"... roi
 * khi con bam gui thi o nhap trong, ban ve mot chuoi rong. Neu ghi thang moi su
 * kien thi nhat ky day nhung manh vun. Lop nay gom lai: giu ban dai nhat, va chi
 * chot mot cau khi co dau hieu con da gui no di.
 *
 * Ba dau hieu chot mot cau:
 *  - o nhap trong tro lai (vua bam gui), xem [goChu];
 *  - con roi khoi app AI sang app khac, xem [roiApp];
 *  - o nhap im qua lau, xem [imLau] - phong khi con go xong ma khong gui, hay app
 *    web khong ban ra su kien "da gui".
 *
 * Lop nay khong biet gi ve Telegram hay Android, chi la mot cai may gom chu, nen
 * kiem tra duoc bang test thuong ma khong can may.
 */
class BoGoAi(
    /** Cau ngan hon nay thi bo. "ok", "hi" khong dang ghi, va thuong la go nham. */
    private val toiThieu: Int = 6,
    /**
     * O nhap im lau hon nay (mili giay) thi coi nhu cau da xong.
     *
     * De mo chu khong giau: ben goi can biet de hen dung luc goi [imLau]. Lop nay
     * khong tu hen gio duoc, vi no khong biet gi ve Android.
     */
    val imMs: Long = 8_000L,
) {
    private var chu = ""
    private var luc = 0L

    /** Cau chot gan nhat, de con go i het lan hai khong ghi thanh hai dong. */
    private var daChot = ""

    /**
     * Nap mot anh chup cua o nhap. Tra ve cau da xong neu day la luc chot, khong
     * thi null.
     */
    fun goChu(text: String, now: Long): String? {
        val t = text.trim()
        return if (t.isEmpty()) {
            chot()
        } else {
            chu = t
            luc = now
            null
        }
    }

    /** Con roi khoi app AI. Cau dang go do, neu co, la xong. */
    fun roiApp(): String? = chot()

    /** Goi dinh ky. Chot cau neu o nhap da im qua lau. */
    fun imLau(now: Long): String? =
        if (chu.isNotEmpty() && now - luc > imMs) chot() else null

    private fun chot(): String? {
        val c = chu.trim()
        chu = ""
        if (c.length < toiThieu) return null
        if (c == daChot) return null
        daChot = c
        return c
    }
}
