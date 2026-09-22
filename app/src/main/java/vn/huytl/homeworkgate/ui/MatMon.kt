package vn.huytl.homeworkgate.ui

import vn.huytl.homeworkgate.R

/**
 * Mot mau va mot chu viet tat cho moi mon hoc.
 *
 * VI SAO CO FILE NAY: man chon mon truoc day la chin o chu nhat giong het nhau, chi
 * khac may chu ben trong. Toan voi Ngu van nhin y het nhau, phai doc moi biet dang
 * bam cai gi - trong khi day la thao tac con lam moi ngay. Co mau va chu viet tat
 * thi thanh quen tay, va cung mot mau do se theo ten mon di khap app.
 *
 * MAU KHONG DOI GIUA HAI LAN MO APP. No tinh tu chinh ten mon, khong phai tu vi tri
 * trong danh sach: thoi khoa bieu doi mot mon la ca bang mau xe dich, va con vua
 * quen mau thi phai hoc lai.
 */
object MatMon {

    /** Bay cap mau, xem colors.xml. */
    private val MAU = intArrayOf(
        R.color.mon_1, R.color.mon_2, R.color.mon_3, R.color.mon_4,
        R.color.mon_5, R.color.mon_6, R.color.mon_7
    )

    private val NEN = intArrayOf(
        R.color.mon_1_nen, R.color.mon_2_nen, R.color.mon_3_nen, R.color.mon_4_nen,
        R.color.mon_5_nen, R.color.mon_6_nen, R.color.mon_7_nen
    )

    /**
     * May mon dat mau tay.
     *
     * Nam mon nay chiem phan lon bai ve nha, va con nhin chung moi ngay. Dat tay de
     * chung roi vao nam mau khac han nhau, thay vi pho mac cho ham bam - bam co the
     * nem Toan va Ngu van vao hai mau xanh gan giong.
     */
    private val DAT_TAY = mapOf(
        "Toán" to 0,
        "Khoa học tự nhiên" to 1,
        "Ngữ văn" to 2,
        "Lịch sử - Địa lý" to 3,
        // Nhay qua mon_5: no la mau xanh mong ket, dung canh mau xanh la cua Khoa
        // hoc tu nhien thi luot mat qua khong phan biet duoc. Hong dam thi khac han.
        "Tiếng Anh" to 5
    )

    /**
     * Viet tat hien trong o tron.
     *
     * Mot chu thi nhieu mon trung nhau ("Toán", "Tin học", "Tiếng Anh" deu ra T), ba
     * chu thi khong con vua o tron. Hai chu la vua.
     */
    private val TAT_TAY = mapOf(
        "Toán" to "T",
        "Ngữ văn" to "Văn",
        "Khoa học tự nhiên" to "KH",
        "Lịch sử - Địa lý" to "Sử",
        "Tiếng Anh" to "TA",
        "Tin học" to "Tin",
        "Công nghệ" to "CN",
        "Mỹ thuật" to "MT",
        "Âm nhạc" to "ÂN",
        "Giáo dục công dân" to "GD",
        "Giáo dục địa phương" to "ĐP",
        "Giáo dục thể chất" to "TD",
        "Trải nghiệm hướng nghiệp" to "HN",
        "Trí tuệ nhân tạo" to "AI",
        "Kỹ năng" to "KN"
    )

    /**
     * Nam mon chiem phan lon bai ve nha.
     *
     * Luoi thoi khoa bieu chi to mau cho may mon nay: bay mau tren nam muoi tu o
     * thi doc mot ten mon phai luot qua bay mau khac nhau, ma phan lon cac o do la
     * mon moi tuan mot tiet, khong phai thu can tim.
     */
    fun laMonChinh(ten: String): Boolean = ten in DAT_TAY

    fun mau(ten: String): Int = MAU[vach(ten)]

    fun nen(ten: String): Int = NEN[vach(ten)]

    /**
     * Chu hien trong o tron: lay tu bang tren, khong co thi tu dat.
     *
     * Ten viet hoa ca cum ("AVNN", "STEM") thi giu nguyen hai ky tu dau - do la ten
     * rieng chu khong phai mot cum tu de rut gon.
     */
    fun tat(ten: String): String {
        TAT_TAY[ten]?.let { return it }
        val goi = ten.trim()
        if (goi.isEmpty()) return "?"
        if (goi == goi.uppercase() && goi.length >= 2) return goi.take(2)
        val tu = goi.split(' ', '-').filter { it.isNotBlank() }
        return when {
            tu.size >= 2 -> "${tu[0].first().uppercase()}${tu[1].first().uppercase()}"
            else -> tu[0].take(2).replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Ten mon ra mot so trong 0..6, on dinh qua moi lan chay.
     *
     * Khong dung String.hashCode: no on dinh trong mot ban Java nhung khong co gi
     * bao dam giua cac ban, va mot lan doi la ca app doi mau. Cong ma ky tu thi doi
     * nao cung ra cung mot so.
     */
    private fun vach(ten: String): Int {
        DAT_TAY[ten]?.let { return it }
        var tong = 0
        for (c in ten) tong = (tong + c.code) % 1_000_003
        return tong % MAU.size
    }
}
