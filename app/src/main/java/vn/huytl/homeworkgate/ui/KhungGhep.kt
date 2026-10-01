package vn.huytl.homeworkgate.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.random.Random
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.kho.DeThi
import vn.huytl.homeworkgate.kho.Ghep

/**
 * Khung tra loi cua mot cau lam tren may: ve ban phim ghep dung kieu cua cau, giu cau tra
 * loi con dang ghep, va noi cau do dung hay sai khi con bam Kiem tra.
 *
 * DUNG CHUNG cho man lam bai tren may ([LamBaiActivity]) va man Giai de. Sao va luot o ben
 * goi, theo [vn.huytl.homeworkgate.data.LuatGhep]; o day chi ve va cham.
 *
 * KHONG CO CHU NAO GIAI THICH LUAT (Ba Huy chot 29/9/2026): con tu kham pha qua sao tat dan.
 * Chu tren man chi la de bai, nhan nut, va loi giai khi da het luot.
 *
 * THU TU NUT GIU NGUYEN TRONG MOT LUOT, DOI SANG LUOT SAU (Ba Huy chot 30/9/2026). Tron bang
 * mot hat giong co dinh cho ca luot, nen ve lai (sau lan sai dau, khi bot phim nhieu; hay tat
 * man roi mo lai giua luot) phim khong nhay cho, con khong phai tim lai tu dau. Truoc ngay do
 * hat giong chi la ma cau, nen lam lai sau 24 gio va on lai sau 3, 10, 20, 30 ngay deu ra dung
 * thu tu cu: con co the nho vi tri nut thay vi nho dap an. Nay hat giong la ma cau cong so
 * luot da xong ([hatLuot]); o man Giai de la ma de cong ma cau ([hatDe]), de ca luc xem lai de
 * da nop van dung thu tu con da thay.
 *
 * Trac nghiem cung tron, va chu A, B, C, D danh lai theo thu tu moi: [chon] va [Ghep.Chon.dap]
 * van la chi so trong sach, chi cho hien moi doi. Du lieu khong co phuong an nao nhac chu cai
 * cua phuong an khac ("Cả A và B"), tools/ghep/kiem.py canh bao neu co. Cau Dung/Sai thi khong
 * tron, nut Dung luon dung truoc: doi cho hai nut khong lam con kho nho hon, chi de bam nham.
 * Hang phim co ban cua cau bieu thuc (so, dau) cung giu nguyen nhu mot ban phim.
 *
 * HINH AN CUA DE THI (1/10/2026). Bai hinh ma to de khong in hinh thi con tu ve, va co nut
 * "Nhờ trợ giúp" mo hinh tung buoc ([veHinhAn], muc giu o [NhoHinhAn]). Khoi nay cung nam o day
 * vi hai man dung no la hai man ve de bai bang [veDe]: the dau bai o man Giai de, va cau de thi
 * dung mot minh o man lam lai, on lai.
 */
class KhungGhep(
    private val khung: LinearLayout,
    val muc: LamTrenMay.Muc,
    /** Hat giong tron thu tu nut cua luot nay, xem [hatLuot] va [hatDe]. */
    hatTron: Int = hatLuot(muc),
    /** Goi moi lan cau tra loi doi, de ben goi xoa dong bao loi cu. */
    private val khiDoi: () -> Unit = {}
) {
    private val ct: Context = khung.context
    private val g: Ghep = muc.ghep
    private val tron = Random(hatTron)

    private var botNhieu = false
    private var daXong = false
    private var hienGiai = false

    // Trang thai cau tra loi, moi kieu dung phan cua no.
    /** Phuong an da bam cua cau trac nghiem; cau [Ghep.Chon.nhieuDap] giu duoc nhieu. */
    private val chon = mutableSetOf<Int>()
    private var chonDs: Boolean? = null
    private val phimDaGo = mutableListOf<String>()
    private val theDaChon = mutableListOf<Int>()
    private val dongDaChon = mutableListOf<Int>()
    private var nu: Boolean? = null
    private val chonO = mutableMapOf<Pair<Int, Int>, String>()
    private val goO = mutableMapOf<Pair<Int, Int>, String>()
    private var oSai: Set<Pair<Int, Int>> = emptySet()

    /** Cac phim/the/dong da tron mot lan, kem co nhieu hay khong. */
    private data class Muc2(val chu: String, val nhieu: Boolean)

    private val phimChu: List<Muc2> by lazy {
        val c = g as Ghep.Chu
        (c.phimThat.map { Muc2(it, false) } + c.nhieu.map { Muc2(it.lowercase(), true) }).shuffled(tron)
    }
    private val phimBieuThuc: List<Muc2> by lazy {
        val b = g as Ghep.BieuThuc
        (b.phim.map { Muc2(it, false) } + b.nhieu.map { Muc2(it, true) }).shuffled(tron)
    }
    private val cacThe: List<Muc2> by lazy {
        val c = g as Ghep.Cau
        (c.the.map { Muc2(it, false) } + c.nhieu.map { Muc2(it, true) }).shuffled(tron)
    }
    private val cacDong: List<Muc2> by lazy {
        val b = g as Ghep.Buoc
        (b.buoc.map { Muc2(it, false) } + b.nhieu.map { Muc2(it, true) }).shuffled(tron)
    }
    /** Thu tu hien cac phuong an trac nghiem: phan tu thu k la chi so trong sach cua nut k. */
    private val thuTuChon: List<Int> by lazy { (g as Ghep.Chon).cac.indices.shuffled(tron) }

    /** Chu cai dang hien cua phuong an [i] (chi so trong sach). */
    private fun chuCai(i: Int): Char = 'A' + thuTuChon.indexOf(i)

    private val tronO: Map<Pair<Int, Int>, List<String>> by lazy {
        val o = g as Ghep.O
        o.cacOChon.associateWith { (i, k) ->
            val x = o.dong[i].o[k]
            (x.dung + x.sai).shuffled(tron)
        }
    }

    // ------------------------------------------------------------------ ve

    /**
     * Ve lai toan bo khung. [botNhieu] true thi bo phim/the/dong nhieu ra khoi ban phim, xem
     * [vn.huytl.homeworkgate.data.LuatGhep.Luot.botNhieu].
     */
    fun ve(botNhieu: Boolean = this.botNhieu) {
        this.botNhieu = botNhieu
        khung.removeAllViews()
        when (g) {
            is Ghep.Chon -> veChon(g)
            is Ghep.DungSai -> veDungSai(g)
            is Ghep.Chu -> veChu(g)
            is Ghep.Cau -> veCau(g)
            is Ghep.BieuThuc -> veBieuThuc(g)
            is Ghep.Buoc -> veBuoc(g)
            is Ghep.O -> veO(g)
        }
        if (hienGiai) veLoiGiai()
    }

    /** Het luot: khoa ban phim, [hienLoiGiai] true thi hien loi giai duoi khung. */
    fun khoa(hienLoiGiai: Boolean) {
        daXong = true
        hienGiai = hienLoiGiai
        ve(botNhieu)
    }

    /**
     * Cham cau tra loi hien tai. Tra so cho sai (0 la dung), hay null khi con chua ghep xong
     * (ben goi bao "chưa làm xong", khong tru sao). Sai thi xoa phan sai de con lam lai.
     */
    fun kiem(): Int? {
        val soSai: Int = when (g) {
            is Ghep.Chon -> if (chon.isEmpty()) return null else if (g.dung(chon)) 0 else 1
            is Ghep.DungSai -> {
                val c = chonDs ?: return null
                if (g.dung(c)) 0 else 1
            }
            is Ghep.Chu -> {
                if (phimDaGo.isEmpty()) return null
                if (g.soO != null && phimDaGo.joinToString("").length < g.soO) return null
                if (g.dung(phimDaGo.joinToString(""))) 0 else 1
            }
            is Ghep.Cau -> {
                if (theDaChon.isEmpty()) return null
                if (g.dung(theDaChon.map { cacThe[it].chu })) 0 else 1
            }
            is Ghep.BieuThuc -> {
                if (phimDaGo.isEmpty()) return null
                if (g.dung(phimDaGo.joinToString(""), muc.cau.mon)) 0 else 1
            }
            is Ghep.Buoc -> {
                if (dongDaChon.isEmpty()) return null
                if (g.dung(dongDaChon.map { cacDong[it].chu })) 0 else 1
            }
            is Ghep.O -> {
                val n = nu ?: if (g.gioi) return null else false
                if (g.cacOChon.any { it !in chonO }) return null
                val sai = g.cacOChon.filter { (i, k) ->
                    val x = chonO[i to k]
                    g.dong[i].o[k].dung.none { Ghep.thayGioi(it, n) == x }
                }.toSet()
                oSai = sai
                sai.size
            }
        }
        if (soSai > 0) xoaSau()
        return soSai
    }

    /** Sai thi xoa cau tra loi de ghep lai; cau nhieu o chi xoa o sai. */
    private fun xoaSau() {
        chon.clear()
        chonDs = null
        phimDaGo.clear()
        theDaChon.clear()
        dongDaChon.clear()
        oSai.forEach { chonO.remove(it) }
    }

    /** Cau tra loi dang chu, ghi vao so cai de Ba Huy xem lai. */
    fun traLoi(): String = when (g) {
        // Chu cai con da thay tren nut luot nay, kem chu cua phuong an: chu cai doi theo luot.
        is Ghep.Chon -> chon.sortedBy { thuTuChon.indexOf(it) }
            .joinToString("; ") { "${chuCai(it)}. ${boThe(g.cac[it])}" }
        is Ghep.DungSai -> chonDs?.let { if (it) g.nhan[0] else g.nhan[1] }.orEmpty()
        is Ghep.Chu -> g.truoc + phimDaGo.joinToString("")
        is Ghep.Cau -> theDaChon.joinToString(" ") { cacThe[it].chu }
        is Ghep.BieuThuc -> phimDaGo.joinToString("")
        is Ghep.Buoc -> dongDaChon.joinToString(" | ") { boThe(cacDong[it].chu) }
        is Ghep.O -> g.dong.indices.joinToString(" ") { i -> cauO(g, i) }
    }

    // ------------------------------------------------------------------ tung kieu

    private fun veChon(c: Ghep.Chon) {
        thuTuChon.forEachIndexed { vt, i ->
            khung.addView(nutRong("${'A' + vt}. ", c.cac[i], i in chon, mauDung(i in c.dap)) {
                // Cau nhieu dap an: bam lan nua la bo chon. Cau mot dap an: bam la doi.
                if (c.nhieuDap) {
                    if (!chon.remove(i)) chon += i
                } else {
                    chon.clear()
                    chon += i
                }
                doi()
            })
        }
    }

    private fun veDungSai(d: Ghep.DungSai) {
        val hang = DongNut(ct, 8.dp(), 8.dp())
        listOf(true, false).forEach { gt ->
            hang.addView(nut(if (gt) d.nhan[0] else d.nhan[1], chonDs == gt, to = true) {
                chonDs = gt
                doi()
            })
        }
        khung.addView(hang)
    }

    private fun veChu(c: Ghep.Chu) {
        // Dong hien chu da go: o tung chu khi biet do dai, khong thi mot dong lien.
        val go = phimDaGo.joinToString("")
        val hien = LinearLayout(ct).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (c.truoc.isNotEmpty()) hien.addView(oChu(c.truoc, dam = true, khung = false))
        val soO = c.soO
        if (soO != null) {
            for (i in 0 until soO) hien.addView(oChu(go.getOrNull(i)?.let { if (it == ' ') "␣" else it.toString() } ?: "", khung = true))
        } else {
            hien.addView(oChu(go.ifEmpty { " " }, khung = true, rong = true))
        }
        khung.addView(hien)
        if (daXong) return
        val phim = DongNut(ct, 6.dp(), 6.dp()).apply { layoutParams = hangLp(10) }
        phimChu.filter { !botNhieu || !it.nhieu }.forEach { p ->
            phim.addView(nut(p.chu) { themPhim(p.chu, c.soO) })
        }
        if (c.coCach) phim.addView(nut("␣") { themPhim(" ", c.soO) })
        phim.addView(nutXoa())
        khung.addView(phim)
    }

    private fun themPhim(p: String, soO: Int? = null) {
        if (daXong) return
        if (soO != null && phimDaGo.joinToString("").length >= soO) return
        phimDaGo += p
        doi()
    }

    private fun veBieuThuc(b: Ghep.BieuThuc) {
        khung.addView(TextView(ct).apply {
            text = hienBieuThuc(phimDaGo).ifEmpty { " " }
            textSize = 24f
            setTextColor(mau(R.color.ink))
            setBackgroundResource(R.drawable.st_nen_o_go)
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
            minHeight = 56.dp()
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = hangLp(0)
        })
        if (daXong) return
        val rieng = DongNut(ct, 6.dp(), 6.dp()).apply { layoutParams = hangLp(10) }
        phimBieuThuc.filter { !botNhieu || !it.nhieu }.forEach { p -> rieng.addView(nut(p.chu) { themPhim(p.chu) }) }
        rieng.addView(nutXoa())
        khung.addView(rieng)
        val coBan = DongNut(ct, 6.dp(), 6.dp()).apply { layoutParams = hangLp(6) }
        Ghep.phimCoBan(muc.cau.mon).forEach { p -> coBan.addView(nut(p) { themPhim(p) }) }
        khung.addView(coBan)
    }

    private fun veCau(c: Ghep.Cau) {
        val da = DongNut(ct, 6.dp(), 6.dp()).apply {
            setBackgroundResource(R.drawable.st_nen_o_go)
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            minimumHeight = 60.dp()
            layoutParams = hangLp(0)
        }
        theDaChon.forEachIndexed { vt, i ->
            da.addView(nut(cacThe[i].chu, chon = true) {
                if (daXong) return@nut
                theDaChon.removeAt(vt)
                doi()
            })
        }
        khung.addView(da)
        if (daXong) return
        val kho = DongNut(ct, 6.dp(), 6.dp()).apply { layoutParams = hangLp(10) }
        cacThe.forEachIndexed { i, t ->
            if (i in theDaChon || (botNhieu && t.nhieu)) return@forEachIndexed
            kho.addView(nut(t.chu) {
                theDaChon += i
                doi()
            })
        }
        khung.addView(kho)
    }

    private fun veBuoc(b: Ghep.Buoc) {
        val da = LinearLayout(ct).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.st_nen_o_go)
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            minimumHeight = 60.dp()
            layoutParams = hangLp(0)
        }
        dongDaChon.forEachIndexed { vt, i ->
            da.addView(nutRong("${vt + 1}. ", cacDong[i].chu, chon = true) {
                if (daXong) return@nutRong
                dongDaChon.removeAt(vt)
                doi()
            })
        }
        khung.addView(da)
        if (daXong) return
        cacDong.forEachIndexed { i, d ->
            if (i in dongDaChon || (botNhieu && d.nhieu)) return@forEachIndexed
            khung.addView(nutRong("", d.chu, false) {
                dongDaChon += i
                doi()
            })
        }
    }

    private fun veO(o: Ghep.O) {
        if (o.gioi && nu == null) {
            khung.addView(chu("Bạn thân của con là bạn nam hay bạn nữ?", 17f))
            val hang = DongNut(ct, 8.dp(), 8.dp()).apply { layoutParams = hangLp(8) }
            hang.addView(nut("Bạn nam", to = true) { nu = false; doi() })
            hang.addView(nut("Bạn nữ", to = true) { nu = true; doi() })
            khung.addView(hang)
            return
        }
        val n = nu ?: false
        o.dong.forEachIndexed { i, d ->
            val the = LinearLayout(ct).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(10.dp(), 8.dp(), 10.dp(), 10.dp())
                layoutParams = hangLp(if (i == 0) 0 else 8)
                val coSai = d.o.indices.any { (i to it) in oSai && (i to it) !in chonO }
                setBackgroundResource(if (coSai) R.drawable.st_nen_luu_y else R.drawable.st_nen_the)
            }
            the.addView(chu(cauO(o, i), 17f))
            d.o.forEachIndexed { k, x ->
                if (x.go) {
                    if (daXong) return@forEachIndexed
                    the.addView(EditText(ct).apply {
                        setText(goO[i to k].orEmpty())
                        hint = x.goiY.ifBlank { "Gõ tên" }
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                        textSize = 17f
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                            override fun afterTextChanged(s: Editable?) {
                                goO[i to k] = s?.toString().orEmpty()
                                khiDoi()
                            }
                        })
                    })
                    return@forEachIndexed
                }
                if (daXong) return@forEachIndexed
                val hang = DongNut(ct, 6.dp(), 6.dp()).apply { layoutParams = hangLp(6) }
                tronO.getValue(i to k).forEach { pa ->
                    val hien = Ghep.thayGioi(pa, n)
                    hang.addView(nut(hien, chon = chonO[i to k] == hien) {
                        chonO[i to k] = hien
                        doi()
                    })
                }
                the.addView(hang)
            }
            khung.addView(the)
        }
    }

    /** Mot dong cua cau nhieu o, o da chon thi hien chu, chua chon thi "___". */
    private fun cauO(o: Ghep.O, i: Int): String {
        val n = nu ?: false
        var s = Ghep.thayGioi(o.dong[i].chu, n)
        o.dong[i].o.forEachIndexed { k, x ->
            val dien = if (x.go) goO[i to k]?.takeIf { it.isNotBlank() } else chonO[i to k]
            s = s.replace("{$k}", dien ?: "___")
        }
        return s
    }

    // ------------------------------------------------------------------ loi giai

    private fun veLoiGiai() {
        val hop = LinearLayout(ct).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_ghi_chu)
            setPadding(12.dp(), 10.dp(), 12.dp(), 12.dp())
            layoutParams = hangLp(12)
        }
        hop.addView(chu("Lời giải", 14f, mau = R.color.ink_soft, dam = true))
        val cac: List<String> = when (g) {
            is Ghep.Chon -> g.dap.sortedBy { thuTuChon.indexOf(it) }.map { "${chuCai(it)}. ${g.cac[it]}" }
            is Ghep.DungSai -> listOf(if (g.dap) g.nhan[0] else g.nhan[1])
            is Ghep.Chu -> listOf(g.dap.first())
            is Ghep.Cau -> listOf(g.dap.first())
            is Ghep.BieuThuc -> g.loiGiai.ifEmpty { listOf(g.dap.first()) }
            is Ghep.Buoc -> g.buoc
            is Ghep.O -> g.dong.indices.map { i ->
                var s = Ghep.thayGioi(g.dong[i].chu, nu ?: false)
                g.dong[i].o.forEachIndexed { k, x ->
                    s = s.replace("{$k}", if (x.go) "…" else Ghep.thayGioi(x.dung.first(), nu ?: false))
                }
                s
            }
        }
        cac.forEach { hop.addView(chu(it, 17f)) }
        khung.addView(hop)
    }

    // ------------------------------------------------------------------ dung chung

    private fun doi() {
        oSai = oSai.filter { it !in chonO }.toSet()
        khiDoi()
        ve(botNhieu)
    }

    private fun nutXoa(): MaterialButton = nut("Xoá") {
        if (daXong || phimDaGo.isEmpty()) return@nut
        phimDaGo.removeAt(phimDaGo.lastIndex)
        doi()
    }.apply { setTextColor(mau(R.color.alert)) }

    /** Nut vua chu, cho phim, the tu, phuong an ngan. */
    private fun nut(chu: String, chon: Boolean = false, to: Boolean = false, bam: () -> Unit): MaterialButton =
        MaterialButton(ct, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = hien(chu, muc.cau.mon)
            isAllCaps = false
            textSize = when {
                to -> 18f
                chu.length <= 2 -> 22f
                else -> 17f
            }
            minWidth = 52.dp()
            minimumWidth = 52.dp()
            // Cao theo chu, thap nhat 52dp: phuong an dai (105 ky tu o bai C2 Unit 1) xuong dong
            // khi tablet dung doc, cao co dinh thi dong thu hai bi cat.
            minHeight = 52.dp()
            minimumHeight = 52.dp()
            setPadding(12.dp(), 6.dp(), 12.dp(), 6.dp())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (chon) {
                backgroundTintList = ColorStateList.valueOf(mau(R.color.brand_soft))
                setTextColor(mau(R.color.brand_dark))
            }
            isEnabled = !daXong
            setOnClickListener { bam() }
        }

    /** Nut rong het be ngang, chu canh trai, cho phuong an dai va dong loi giai. */
    private fun nutRong(dau: String, chu: String, chon: Boolean, mauChu: Int? = null, bam: () -> Unit): MaterialButton =
        MaterialButton(ct, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = hien("$dau$chu", muc.cau.mon)
            isAllCaps = false
            textSize = 17f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
            minHeight = 52.dp()
            layoutParams = hangLp(6)
            if (chon) {
                backgroundTintList = ColorStateList.valueOf(mau(R.color.brand_soft))
                setTextColor(mau(R.color.brand_dark))
            }
            if (daXong && mauChu != null) setTextColor(mau(mauChu))
            isEnabled = !daXong
            setOnClickListener { bam() }
        }

    /** Het luot thi to xanh phuong an dung, cho con thay minh da chon gi. */
    private fun mauDung(la: Boolean): Int? = if (daXong && hienGiai && la) R.color.ok else null

    private fun oChu(chu: String, dam: Boolean = false, khung: Boolean, rong: Boolean = false): TextView =
        TextView(ct).apply {
            text = chu
            textSize = 26f
            setTextColor(mau(R.color.ink))
            if (dam) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            if (khung) setBackgroundResource(R.drawable.st_nen_o_go)
            layoutParams = LinearLayout.LayoutParams(
                if (rong) ViewGroup.LayoutParams.MATCH_PARENT else if (khung) 44.dp() else ViewGroup.LayoutParams.WRAP_CONTENT,
                52.dp()
            ).apply { marginEnd = 4.dp() }
        }

    private fun chu(s: String, co: Float, mau: Int = R.color.ink, dam: Boolean = false): TextView =
        TextView(ct).apply {
            text = hien(s, muc.cau.mon)
            textSize = co
            setTextColor(mau(mau))
            if (dam) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(2f * resources.displayMetrics.density, 1f)
        }

    private fun hangLp(tren: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = tren.dp() }

    private fun mau(id: Int): Int = ContextCompat.getColor(ct, id)

    private fun Int.dp(): Int = (this * ct.resources.displayMetrics.density).toInt()

    /**
     * Muc hinh an cua tung bai de thi trong mot lan mo man (1/10/2026): [AN] chua mo, [GOI_Y] da
     * hien hinh goi y, [DAY_DU] da hien hinh day du. Moi man co hinh an giu mot cai.
     *
     * GIU TRONG ACTIVITY, KHONG GHI VAO KHO (luat 4 Ba Huy chot 1/10/2026): thoat khoi man roi
     * vao lai (Activity moi) thi hinh an lai, nut ve lan mot, con muon xem phai bam lai (nhat ky
     * chi ghi lan dau trong ngay, xem [GiaiDe.ghiXemHinh]). Con trong mot lan mo man thi hinh da
     * mo phai con mo: man Giai de ve
     * lai ca danh sach moi lan onResume (tat man hinh roi bat lai, quay ve tu man chup), va
     * Android van co the dung lai Activity (doi co chu, he thong giet tien trinh luc man nam
     * nen; xoay man thi khong, hai man nay khai configChanges). Nen ben goi giu mot cai trong
     * Activity, cat vao Bundle o onSaveInstanceState ([luu]) va doc lai o onCreate ([doc]).
     */
    class NhoHinhAn {
        private val muc = HashMap<String, Int>()

        /**
         * Ham ve lai khoi dang hien cua tung khoa. Hop canh bao co the mo lau toi luc man da ve
         * lai (tat man hinh roi bat lai): bam "Vẫn xem" luc do phai doi khoi moi tren man, khong
         * phai khoi cu da roi khoi danh sach.
         */
        private val dangHien = HashMap<String, () -> Unit>()

        fun cua(khoa: String): Int = muc[khoa] ?: AN

        /** Dat muc cua [khoa] roi ve lai khoi dang hien cua khoa do. */
        fun dat(khoa: String, m: Int) {
            muc[khoa] = m
            dangHien[khoa]?.invoke()
        }

        /** Khoi vua ve thay cho khoi cu cung khoa. */
        fun dangKy(khoa: String, veLai: () -> Unit) {
            dangHien[khoa] = veLai
        }

        fun luu(ra: Bundle) {
            val cacKhoa = muc.keys.toList()
            ra.putStringArray(K_KHOA, cacKhoa.toTypedArray())
            ra.putIntArray(K_MUC, cacKhoa.map { muc.getValue(it) }.toIntArray())
        }

        fun doc(vao: Bundle?) {
            val cacKhoa = vao?.getStringArray(K_KHOA) ?: return
            val cacMuc = vao.getIntArray(K_MUC) ?: return
            cacKhoa.zip(cacMuc.toList()).forEach { (k, m) -> muc[k] = m }
        }

        companion object {
            const val AN = 0
            const val GOI_Y = 1
            const val DAY_DU = 2
            private const val K_KHOA = "hinh_an_khoa"
            private const val K_MUC = "hinh_an_muc"
        }
    }

    companion object {
        /**
         * Hat tron cua mot luot o man lam bai tren may: ma cau cong so luot da xong.
         *
         * Luot dang lam chua ghi vao so cai (chi nam o [vn.huytl.homeworkgate.data.LuotDangLam]
         * toi khi xong), nen trong luot so luot dung yen: tat man roi mo lai giua luot van dung
         * thu tu cu. Xong luot la them mot dong, luot sau (lam lai sau 24 gio, on lai) tron khac.
         */
        fun hatLuot(muc: LamTrenMay.Muc): Int = muc.cau.id.hashCode() * 31 + muc.soLuot

        /**
         * Hat tron cua mot cau trong de Giai de: ma de cong ma cau. Cau trong de chi ghi vao so
         * cai luc nop ca de, nen khong lay theo so luot duoc: nop xong thi so luot tang, va man
         * xem lai de se hien thu tu khac voi luc con lam. Moi de mot thu tu, de sau khac.
         */
        fun hatDe(deId: String, cauId: String): Int = "$deId/$cauId".hashCode()

        /**
         * Chu co the <u>, <b> va xuong dong sang chu hien duoc. Cac dau <, >, & khac trong
         * chu (bat dang thuc, mui ten) phai giu nguyen, nen thoat het roi tra lai hai the.
         *
         * So mu viet bang "^" trong de sach ("x^2y") doi ra chu so mu that nhu moi man khac
         * ([SoMu]): cau khong co "hoi" thi man nay hien thang [vn.huytl.homeworkgate.kho.CauHoi.de],
         * va truoc 29/9/2026 con doc "x^2" o day. Cau KHTN doi them chi so cong thuc hoa hoc
         * ([SoMu.hienDe]), nen truyen [mon] cua cau.
         */
        fun hien(goc: String, mon: String = ""): CharSequence {
            val chu = SoMu.hienDe(goc, mon)
            if (!chu.contains('<') && !chu.contains('\n') && !chu.contains('&')) return chu
            val thoat = chu.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace(Regex("&lt;(/?)([ub])&gt;"), "<$1$2>")
                .replace("\n", "<br>")
            return HtmlCompat.fromHtml(thoat, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        fun boThe(chu: String): String = chu.replace(Regex("</?[ub]>"), "")

        /** Bieu thuc da go, them dau cach quanh dau cong tru bang cho de doc. */
        fun hienBieuThuc(cac: List<String>): String = cac.joinToString("") {
            if (it in setOf("+", "−", "=", "→", "×", "·", ":")) " $it " else it
        }.replace("  ", " ").trim()

        /**
         * Ve de bai cua mot cau vao [khung]: chu (hay [Ghep.hoi]), anh, doan van. Dung chung
         * cho man lam bai va man Giai de.
         *
         * Hai tham so sau chi de thi in san dung (man Giai de, [vn.huytl.homeworkgate.kho.DeThi]):
         * ten phan va loi dan da in mot lan o dau phan, doan van in o cau dau cua doan.
         *
         * Hinh in san ([vn.huytl.homeworkgate.kho.CauHoi.hinh]) hien ngay. Hinh an cua de thi (to
         * de khong in hinh) khong ve o day: no thuoc ca bai chu khong thuoc mot y, nen ben goi tu
         * dat [veHinhAn] vao cho can (the dau bai o man Giai de, duoi chu cau o man lam lai).
         *
         * @param hienDoan false thi khong ve doan van: cau truoc trong de da ve roi.
         * @param boDau dong dau cua chu can bo khi no dung bang chuoi nay. Chu cua cau de thi la
         *   "loi dan\nnoi dung cau", de cau do dung mot minh (On lai, lam lai) van du nghia;
         *   trong de thi loi dan da in o dau phan.
         */
        fun veDe(khung: LinearLayout, muc: LamTrenMay.Muc, hienDoan: Boolean = true, boDau: String = "") {
            val ct = khung.context
            val mat = ct.resources.displayMetrics.density
            if (hienDoan && muc.cau.doan.isNotBlank()) {
                khung.addView(TextView(ct).apply {
                    text = hien(muc.cau.doan)
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(ct, R.color.ink))
                    setBackgroundResource(R.drawable.bg_ghi_chu)
                    setPadding((12 * mat).toInt(), (10 * mat).toInt(), (12 * mat).toInt(), (10 * mat).toInt())
                    setLineSpacing(3f * mat, 1f)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = (10 * mat).toInt() }
                })
            }
            // Cau phat am, trong am cua de thi chi co loi dan, noi dung nam o cac phuong an. Bo
            // dong loi dan thi the cau trong tron, chi con bon nut (Ba Huy thay kho hieu ngay
            // 30/9/2026), nen cau chi co loi dan thi in lai loi dan ngay trong the.
            val goc = muc.ghep.hoi.ifBlank { muc.cau.de }
            val chu = if (boDau.isNotBlank() && goc.startsWith(boDau)) {
                goc.removePrefix(boDau).trimStart('\n').ifBlank { goc }
            } else {
                goc
            }
            if (chu.isNotBlank()) khung.addView(TextView(ct).apply {
                text = hien(chu, muc.cau.mon)
                textSize = 19f
                setTextColor(ContextCompat.getColor(ct, R.color.ink))
                setLineSpacing(3f * mat, 1f)
            })
            veHinh(khung, muc.cau.hinh)
        }

        /**
         * Ve cac anh [cacDuong] (tinh tu assets/hinh/) vao [khung], moi anh mot dong, cao toi da
         * 300dp. Tach khoi [veDe] ngay 1/10/2026 de hinh an ([veHinhAn]) hien cung kieu voi hinh
         * in tren de. Anh khong mo duoc (thieu file) thi bo qua, nhu truoc.
         *
         * @return so anh ve duoc.
         */
        fun veHinh(khung: LinearLayout, cacDuong: List<String>, moTa: String = "Hình của câu"): Int {
            val ct = khung.context
            val mat = ct.resources.displayMetrics.density
            var so = 0
            cacDuong.forEach { duong ->
                val bm = runCatching {
                    ct.assets.open("hinh/$duong").use { BitmapFactory.decodeStream(it) }
                }.getOrNull() ?: return@forEach
                khung.addView(ImageView(ct).apply {
                    setImageBitmap(bm)
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_START
                    maxHeight = (300 * mat).toInt()
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (10 * mat).toInt() }
                    contentDescription = moTa
                })
                so++
            }
            return so
        }

        /**
         * Khoi hinh an cua mot bai de thi ma to de khong in hinh (luat 4, Ba Huy chot 1/10/2026),
         * them vao cuoi [khung]. Dung chung cho the dau bai o man Giai de va cho cau de thi dung
         * mot minh o man lam lai, on lai ([LamBaiActivity]).
         *
         * Muc [NhoHinhAn.AN]: dong nhac con tu ve ra giay nhap, va nut "Nhờ trợ giúp". Bam lan mot:
         * hien hinh goi y ([DeThi.Muc.hinhGoiY]: hinh trong loi giai da xoa net, diem lo dap an);
         * bai co hinh day du thi nut con do, khong thi nut an. Bam lan hai: hop canh bao, dong y
         * thi hinh day du thay cho hinh goi y va nut an. Dong nhac chi hien khi con nut, vi no bao
         * con bam nut do.
         *
         * KHONG TRU SAO. Mo hinh thi ghi mot dong nhat ky ([GiaiDe.ghiXemHinh], moi bai moi muc mot
         * lan trong ngay): do la cach duy nhat Ba Huy biet con da nho hinh. Ghi hong thi hinh van
         * hien, con khong bi ket o day.
         *
         * Khoi tu ve lai phan cua minh, khong nho ben goi ve lai ca man: ve lai ca man thi khung
         * ghep cua moi cau dung lai tu dau, mat cau tra loi con dang ghep do ma chua bam Kiem tra.
         *
         * Bai chi co hinh day du ma khong co hinh goi y (kiem.py bat loi nay, day chi la cho phong)
         * thi lan bam dau hien luon hop canh bao: hinh duy nhat co the lo cach lam.
         *
         * @param khoa khoa muc trong [nho]: ma de va bai o man Giai de, id cau o man lam lai.
         */
        fun veHinhAn(khung: LinearLayout, de: DeThi.De, muc: DeThi.Muc, khoa: String, nho: NhoHinhAn) {
            val ct = khung.context
            val mat = ct.resources.displayMetrics.density
            val hop = LinearLayout(ct).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (12 * mat).toInt() }
            }
            khung.addView(hop)

            fun dong(s: String, co: Float, dam: Boolean = false, tren: Int = 0): TextView = TextView(ct).apply {
                text = s
                textSize = co
                setTextColor(ContextCompat.getColor(ct, R.color.ink_soft))
                if (dam) setTypeface(typeface, Typeface.BOLD)
                setLineSpacing(2f * mat, 1f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (tren * mat).toInt() }
            }

            fun moDayDu() {
                MaterialAlertDialogBuilder(ct)
                    .setTitle("Xem hình đầy đủ?")
                    .setMessage(
                        "Hình đầy đủ lấy từ lời giải, có thể lộ cách làm. " +
                            "Con thử vẽ lại một lần nữa, vẫn không ra thì mới xem."
                    )
                    .setPositiveButton("Vẫn xem") { _, _ ->
                        // Hai hop cung mo (bam nhanh hai lan) thi hop sau khong ghi them dong nao.
                        if (nho.cua(khoa) == NhoHinhAn.DAY_DU) return@setPositiveButton
                        runCatching { GiaiDe.ghiXemHinh(ct, de, muc, dayDu = true) }
                        nho.dat(khoa, NhoHinhAn.DAY_DU)
                    }
                    .setNegativeButton("Thôi", null)
                    .show()
            }

            fun ve() {
                hop.removeAllViews()
                val m = nho.cua(khoa)
                val conNut = m == NhoHinhAn.AN || (m == NhoHinhAn.GOI_Y && muc.hinhDayDu.isNotEmpty())
                if (conNut) {
                    hop.addView(dong("Đề không in hình: con tự vẽ ra giấy nháp. Vẽ không được thì bấm Nhờ trợ giúp.", 15f))
                }
                if (m != NhoHinhAn.AN) {
                    val dayDu = m == NhoHinhAn.DAY_DU
                    val ten = if (dayDu) "Hình đầy đủ" else "Hình gợi ý"
                    hop.addView(dong(ten, 14f, dam = true, tren = if (conNut) 10 else 0))
                    if (veHinh(hop, if (dayDu) muc.hinhDayDu else muc.hinhGoiY, ten) == 0) {
                        hop.addView(dong("Máy chưa có hình này.", 15f, tren = 6))
                    }
                }
                if (!conNut) return
                hop.addView(MaterialButton(ct, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = "Nhờ trợ giúp"
                    isAllCaps = false
                    textSize = 17f
                    minHeight = (52 * mat).toInt()
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (10 * mat).toInt() }
                    // Doc muc luc bam, khong lay muc luc ve: nut nay co the da cu.
                    setOnClickListener {
                        when (nho.cua(khoa)) {
                            NhoHinhAn.AN -> if (muc.hinhGoiY.isEmpty()) moDayDu() else {
                                runCatching { GiaiDe.ghiXemHinh(ct, de, muc, dayDu = false) }
                                nho.dat(khoa, NhoHinhAn.GOI_Y)
                            }
                            NhoHinhAn.GOI_Y -> moDayDu()
                        }
                    }
                })
            }

            nho.dangKy(khoa) { ve() }
            ve()
        }
    }
}
