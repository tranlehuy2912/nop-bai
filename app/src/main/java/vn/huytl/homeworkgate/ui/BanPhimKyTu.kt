package vn.huytl.homeworkgate.ui

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import vn.huytl.homeworkgate.R

/**
 * Dai nut ky hieu ngay duoi o go, dung chung cho man hoc thuoc va man soat bai.
 *
 * VI SAO CO. Ban phim tablet khong co dau mu, chi so duoi, dau cua ion hay mui ten
 * phan ung, con "+" va "*" thi nam sau mot trang ky tu. Bat con di tim phim thi thoi
 * gian hoc thanh thoi gian tim phim. Nut o day chi chen chu vao cho con tro, khong
 * phai ban phim he thong: ban phim he thong phai bat trong Cai dat, ma Cai dat thi
 * dang bi chan.
 *
 * MOI NHOM CHUC NANG MOT HANG, hang nao dai qua be ngang thi tu xuong dong. Truoc
 * 24/9/2026 day la mot dai ngang phai cuon: nut o cuoi dai khong ai thay, va dai Toan
 * con khong co dau cong, trong khi dau cong la ky hieu go nhieu nhat trong bo the Toan
 * (21 lan trong 11 the). Ba Huy chon xep theo nhom.
 *
 * NUT NAO CUNG PHAI CHAM DUNG. [vn.huytl.homeworkgate.data.HocThuoc.chuanHoa] da quy
 * "*" va "·" ve mot dau nhan, "−" va "-" ve mot dau tru, so nho tren va duoi ve so
 * thuong, nen chen bang nut hay go bang ban phim deu cham nhu nhau. Mui ten, "°" va
 * "Δ" thi khong quy ve dau ca: dap an nao co chung thi phai go dung chung, va chi co
 * nut moi go ra. HocThuocTest kiem moi ky hieu trong dap an that deu co nut o day.
 */
object BanPhimKyTu {

    /** Mot hang nut: ten nhom hien o dau hang, va cac ky hieu theo thu tu hien. */
    data class Nhom(val ten: String, val cac: List<String>)

    /**
     * Toan 8. Hang dau la phep tinh vi cong thuc nao cung co, va hai dau cong, nhan la
     * hai dau ban phim tablet giau ky nhat.
     *
     * Dau nhan la "*" chu khong phai "·": Ba Huy chon "*" cho de bam, va hai dau do
     * cham nhu nhau.
     */
    val TOAN = listOf(
        Nhom("Phép tính", listOf("+", "−", "*", "/", "=", "(", ")")),
        Nhom("Mũ, căn", listOf("^", "²", "³", "√", "∛")),
        Nhom("So sánh", listOf("<", ">", "≈", "≠", "≤", "≥")),
        Nhom("Hình học", listOf("°", "∠", "Δ", "∥", "⊥", "π"))
    )

    /**
     * KHTN 8, ca phan Hoa lan phan Li: hai bo the chung mot mon nen chung mot dai.
     *
     * Chi so duoi go bang so thuong van cham dung ("H2O" la "H₂O"), nhung co nut thi
     * con go ra dung chu in trong sach. Dau cua ion thi khac han: dai cu chi co "^" va
     * "+", con bam hai nut do cho "H⁺" thi ra "H^+" va bi cham sai, vi chuanHoa khong
     * coi "^+" la "+". Nut "⁺" va "⁻" go ra dung dap an.
     *
     * Dau nhan o day giu "·" nhu sach KHTN in ("m = n·M"), khac bo Toan.
     */
    val KHTN = listOf(
        Nhom("Chỉ số dưới", "₀₁₂₃₄₅₆₇₈₉".map { it.toString() }),
        Nhom("Mũ, điện tích", listOf("⁺", "⁻") + "⁰¹²³⁴⁵⁶⁷⁸⁹".map { it.toString() } + "^"),
        Nhom("Phép tính", listOf("+", "−", "·", "/", "=", "(", ")", "%", "'", "<", ">")),
        Nhom("Phản ứng", listOf("→", "⇌", "↑", "↓", "°", "Δ"))
    )

    /**
     * So nho tren, so nho duoi va dau cua ion: font ve chung chi bang mot nua chu
     * thuong, nen o co 17 nhu cac nut khac thi thanh hat vung. Nut cua chung dung co to
     * hon.
     */
    private const val SO_NHO = "₀₁₂₃₄₅₆₇₈₉⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻"

    /** Dai cua mot mon, hoac null khi mon do khong can nut nao. */
    fun cuaMon(mon: String): List<Nhom>? = when (mon) {
        "Toán" -> TOAN
        "Khoa học tự nhiên" -> KHTN
        else -> null
    }

    /** Ve cac hang nut vao [khung], mot LinearLayout doc. Bam nut thi goi [chen]. */
    fun ve(khung: LinearLayout, cacNhom: List<Nhom>, chen: (String) -> Unit) {
        val ct = khung.context
        val mat = ct.resources.displayMetrics.density
        fun Int.dp(): Int = (this * mat).toInt()

        khung.removeAllViews()
        cacNhom.forEachIndexed { i, nhom ->
            val hang = LinearLayout(ct).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { if (i > 0) topMargin = 4.dp() }
            }
            // Nhan cao bang mot nut va canh giua theo chieu doc, de khi hang xuong dong
            // thi nhan van dung ngang dong nut dau tien.
            hang.addView(TextView(ct).apply {
                text = nhom.ten
                textSize = 13f
                setTextColor(ContextCompat.getColor(ct, R.color.ink_soft))
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(104.dp(), 44.dp())
            })
            val dong = DongNut(ct, khoangNgang = 6.dp(), khoangDoc = 4.dp()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            nhom.cac.forEach { ky ->
                dong.addView(
                    MaterialButton(
                        ct, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
                    ).apply {
                        text = ky
                        textSize = if (ky.length == 1 && ky[0] in SO_NHO) 24f else 17f
                        minWidth = 48.dp()
                        minimumWidth = 48.dp()
                        setPadding(10.dp(), 0, 10.dp(), 0)
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 44.dp())
                        // Bam nut khong duoc cuop con tro khoi o dang go.
                        isFocusable = false
                        setOnClickListener { chen(ky) }
                    }
                )
            }
            hang.addView(dong)
            khung.addView(hang)
        }
    }
}

/**
 * Mot dong nut tu xuong hang khi het be ngang, thay cho cuon ngang.
 *
 * Tu viet chu khong keo them FlexboxLayout: mot phu thuoc moi chi de xep vai chuc cai
 * nut. Flow cua ConstraintLayout cung lam duoc, nhung phai dat id cho tung nut.
 */
private class DongNut(
    context: Context,
    private val khoangNgang: Int,
    private val khoangDoc: Int
) : ViewGroup(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val coTran = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED
        val rong = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var caoDong = 0
        var rongNhat = 0
        for (i in 0 until childCount) {
            val con = getChildAt(i)
            if (con.visibility == GONE) continue
            measureChild(con, widthMeasureSpec, heightMeasureSpec)
            if (coTran && x > 0 && x + con.measuredWidth > rong) {
                x = 0
                y += caoDong + khoangDoc
                caoDong = 0
            }
            rongNhat = maxOf(rongNhat, x + con.measuredWidth)
            x += con.measuredWidth + khoangNgang
            caoDong = maxOf(caoDong, con.measuredHeight)
        }
        setMeasuredDimension(
            resolveSize(rongNhat + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(y + caoDong + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val rong = r - l - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var caoDong = 0
        for (i in 0 until childCount) {
            val con = getChildAt(i)
            if (con.visibility == GONE) continue
            if (x > 0 && x + con.measuredWidth > rong) {
                x = 0
                y += caoDong + khoangDoc
                caoDong = 0
            }
            con.layout(
                paddingLeft + x, paddingTop + y,
                paddingLeft + x + con.measuredWidth, paddingTop + y + con.measuredHeight
            )
            x += con.measuredWidth + khoangNgang
            caoDong = maxOf(caoDong, con.measuredHeight)
        }
    }
}
