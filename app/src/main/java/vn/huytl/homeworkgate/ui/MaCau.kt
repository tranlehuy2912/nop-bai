package vn.huytl.homeworkgate.ui

/**
 * Doc ma cau Claude tu dat cho phieu bai tap ngoai sach: "B5-TL3g" la Bai 5, tu luan, cau 3g.
 *
 * VI SAO CO. Bai nop khong khai theo sach thi Claude tu dat ma cho tung cau. Bai KHTN 21:15
 * ngay 28/9/2026 la phieu Bai 5, Claude dat B5-TN1 cho trac nghiem cau 1 va B5-TL3g cho tu
 * luan cau 3 y g. Man chinh ghi "B5-TL3g" thi Ba Huy khong doc ra, nen Ba Huy chon hien kem
 * nghia: "B5-TL3g: Bài 5, tự luận, câu 3g" (30/9/2026).
 *
 * Chi doc kieu ma do, co hay khong co phan "B<so>-" dau. Ma sach ("2.19a") va ma khac thi de
 * nguyen. App Bang dieu khien co mot ban y het (data/MaCau.kt), sua ben nay thi sua ca ben do.
 */
object MaCau {

    private val MAU = Regex("""^(?:B(\d+)\s*[-.]?\s*)?(TN|TL)\s*(\d+)\s*([a-z]{0,2})$""", RegexOption.IGNORE_CASE)

    /** "Bài 5, tự luận, câu 3g", hay null khi ma khong theo kieu phieu. */
    fun moTa(ma: String): String? {
        val m = MAU.matchEntire(ma.trim()) ?: return null
        val (bai, phan, cau, y) = m.destructured
        val tenPhan = if (phan.equals("TN", ignoreCase = true)) "trắc nghiệm" else "tự luận"
        val dau = if (bai.isEmpty()) tenPhan.replaceFirstChar { it.uppercase() } else "Bài $bai, $tenPhan"
        return "$dau, câu $cau${y.lowercase()}"
    }

    /** "B5-TL3g: Bài 5, tự luận, câu 3g", hay chinh ma do khi khong doc duoc. */
    fun hien(ma: String): String = moTa(ma)?.let { "${ma.trim()}: $it" } ?: ma.trim()
}
