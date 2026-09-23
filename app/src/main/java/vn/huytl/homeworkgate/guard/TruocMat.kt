package vn.huytl.homeworkgate.guard

/**
 * Tablet dang mo gi, chup lai de day sang Bang dieu khien.
 *
 * Bat bien: [GuardAccessibilityService] thay ca ban moi chu khong sua ban cu, con
 * [vn.huytl.homeworkgate.dongbo.DongBo] doc tu luong khac. Ba thu doc rieng tung
 * bien thi co luc doc trung giua hai lan ghi, ra ten cua app nay voi gio cua app
 * kia.
 */
data class TruocMat(
    /** Man hinh dang sang. Tat thi [cac] luon rong. */
    val sang: Boolean,
    /** Cac app dang tren man hinh, app mo truoc dung truoc. Chia doi man hinh thi co hai. */
    val cac: List<MotApp>
) {
    data class MotApp(val goi: String, val ten: String, val tu: Long)

    /** Mot dong ten de hien: "YouTube", hay "YouTube + Zalo" khi chia doi man hinh. */
    val ten: String get() = cac.joinToString(" + ") { it.ten }

    /** Luc mo app mo lau nhat trong so dang hien. 0 la khong co app nao. */
    val tu: Long get() = cac.minOfOrNull { it.tu } ?: 0L

    companion object {
        val TAT = TruocMat(sang = false, cac = emptyList())
    }
}
