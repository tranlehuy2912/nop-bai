package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang

/**
 * Moi cau lam tren may trong ngan hang: app doc duoc khoi ghep, moi dap an trong file go ra
 * duoc bang dung bo phim man lam bai hien, va app cham la dung.
 *
 * VI SAO CO. tools/ghep/kiem.py kiem du lieu bang ban chep luat so chuoi cua app sang
 * Python. Hai ben lech nhau thi file qua kiem ma con lam dung van bi cham sai, va may
 * khong bao gio biet. Test nay chay chinh ham cham cua app ([Ghep]) tren chinh du lieu
 * da nap, nen moi lan gop du lieu moi (tools/ghep/gop.py) chay lai la biet.
 *
 * Go phim BIEU_THUC y nhu KhungGhep: noi chu cua cac phim da bam. Dap an chuan hoa roi
 * tach thanh phim bang quy hoach dong; chuan hoa kem mot chu "a" cuoi roi bo di, vi
 * [HocThuoc.chuanHoa] cat dau cham cuoi ma phim "×" chuan hoa ra dau cham.
 *
 * Ghi ngay 29/9/2026, luc gop SBT Toan tap mot chuong I va SBT Tieng Anh Unit 1, 2: 199
 * cau, 0 loi.
 */
@RunWith(AndroidJUnit4::class)
class GhepTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun chuoi(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }

    private fun chuan(s: String) = HocThuoc.chuanHoa(s + "a", giuHoa = true).dropLast(1)

    /** Tach [s] da chuan hoa thanh day chi so phim; null la bo phim khong go ra duoc. */
    private fun tach(s: String, phim: List<String>): List<Int>? {
        val p = phim.map(::chuan)
        val tot = arrayOfNulls<List<Int>>(s.length + 1)
        tot[0] = emptyList()
        for (i in 0 until s.length) {
            val truoc = tot[i] ?: continue
            p.forEachIndexed { k, x ->
                if (x.isNotEmpty() && s.startsWith(x, i) && tot[i + x.length] == null) tot[i + x.length] = truoc + k
            }
        }
        return tot[s.length]
    }

    /** Ghep cau [s] tu cac the, moi the dung mot lan, cac the cach nhau mot dau cach. */
    private fun ghepThe(s: String, the: List<String>): List<String>? {
        if (s.isEmpty()) return emptyList()
        for ((i, t) in the.withIndex()) {
            if (!s.startsWith(t)) continue
            val sau = s.substring(t.length)
            if (sau.isNotEmpty() && !sau.startsWith(" ")) continue
            val tiep = ghepThe(sau.removePrefix(" "), the.toMutableList().also { it.removeAt(i) }) ?: continue
            return listOf(t) + tiep
        }
        return null
    }

    @Test
    fun moi_dap_an_go_ra_duoc_va_duoc_cham_dung() {
        NganHang.napNeuCan(context)
        val kho = KhoBai.get(context)
        val cac = LamTrenMay.MON.flatMap { mon ->
            NganHang.sachBaiTapCua(mon).flatMap { kho.cacCauCuaNguon(it.nguon) }.filter { it.lamTrenMay }
        }
        val loi = mutableListOf<String>()
        for (c in cac) {
            val ma = c.id
            val o = JSONObject(c.ghep)
            val g = Ghep.doc(c.ghep)
            if (g == null) {
                loi += "$ma: app khong doc duoc khoi ghep"
                continue
            }
            when (g) {
                is Ghep.Chon -> {
                    val chu = o.optJSONArray("dap")?.let(::chuoi) ?: listOf(o.getString("dap"))
                    val mong = chu.map { it.trim()[0] - 'A' }.toSet()
                    if (g.dap != mong || !g.dung(mong)) loi += "$ma: CHON dap $mong, app doc ${g.dap}"
                }
                is Ghep.DungSai -> if (!g.dung(o.getBoolean("dap"))) loi += "$ma: DUNG_SAI"
                is Ghep.Chu -> {
                    val phim = (g.phimThat + g.nhieu).toSet()
                    g.dap.forEach { d ->
                        val go = d.drop(g.truoc.length).lowercase()
                        val thieu = go.filterNot { it.isWhitespace() }.map { it.toString() }.filter { it !in phim }
                        if (thieu.isNotEmpty()) loi += "$ma: CHU '$d' thieu phim $thieu"
                        if (go.any { it.isWhitespace() } && !g.coCach) loi += "$ma: CHU '$d' thieu phim cach"
                        if (g.soO != null && go.length != g.soO) loi += "$ma: CHU '$d' dai ${go.length}, so o ${g.soO}"
                        if (!g.dung(go)) loi += "$ma: CHU '$d' go dung ma app cham sai"
                    }
                }
                is Ghep.Cau -> g.dap.forEach { d ->
                    val the = ghepThe(d, g.the)
                    if (the == null) loi += "$ma: CAU '$d' khong ghep duoc tu the"
                    else if (!g.dung(the)) loi += "$ma: CAU '$d' ghep dung ma app cham sai"
                }
                is Ghep.BieuThuc -> {
                    val phim = Ghep.phimCoBan(c.mon) + g.phim + g.nhieu
                    g.dap.forEach { d ->
                        val cach = tach(chuan(d), phim)
                        if (cach == null) {
                            loi += "$ma: BIEU_THUC '$d' khong go ra duoc bang phim $phim"
                        } else {
                            val go = cach.joinToString("") { phim[it] }
                            if (!g.dung(go, c.mon)) loi += "$ma: BIEU_THUC '$d' go '$go' ma app cham sai"
                        }
                    }
                }
                is Ghep.Buoc -> {
                    if (!g.dung(g.buoc)) loi += "$ma: BUOC thu tu goc bi cham sai"
                    g.thuTuKhac.forEach { t -> if (!g.dung(t.map { g.buoc[it] })) loi += "$ma: BUOC thu tu $t bi cham sai" }
                }
                is Ghep.O -> listOf(false, true).forEach { nu ->
                    val chon = g.dong.map { d -> d.o.map { x -> if (x.go) "Lê Hòa" else Ghep.thayGioi(x.dung.first(), nu) } }
                    val sai = g.soSai(chon, nu)
                    if (sai != 0) loi += "$ma: O chon het dap an dung ma app bao $sai o sai (nu=$nu)"
                    g.dong.forEach { d ->
                        d.o.forEach { x ->
                            x.sai.forEach { s ->
                                if (x.dung.any { Ghep.thayGioi(it, nu) == Ghep.thayGioi(s, nu) }) loi += "$ma: O '$s' vua dung vua sai"
                            }
                        }
                    }
                }
            }
        }
        println("GHEPTEST: ${cac.size} cau, ${loi.size} loi")
        loi.forEach { println("GHEPTEST: $it") }
        assertTrue("khong co cau lam tren may nao", cac.isNotEmpty())
        assertTrue(loi.joinToString("\n"), loi.isEmpty())
    }
}
