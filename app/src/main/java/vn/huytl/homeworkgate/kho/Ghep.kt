package vn.huytl.homeworkgate.kho

import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import vn.huytl.homeworkgate.data.HocThuoc

/**
 * Cach lam mot cau ngay tren may bang ban phim ghep: doc tu truong "ghep" cua cau hoi.
 *
 * VI SAO CO. Le Hoa bo qua moi bai bat viet, chi viet bai trong vo dan do (Ba Huy noi
 * ngay 29/9/2026). Nen bai may giao - lam them, luyen, on lai, Giai de - khong con chup
 * vo nua: con ghep cau tra loi tu mot bo phim rieng cua tung cau, may cham ngay. Dinh
 * dang va luat soan o tools/ghep/DINH_DANG.md; tools/ghep/kiem.py kiem moi file sach
 * truoc khi nap, va luat so sanh o day phai giong het ben do.
 *
 * Bay kieu, moi kieu mot cach nhap:
 *  - [Chon], [DungSai]: bam mot nut.
 *  - [Chu]: go tung chu cai, phim dung lai duoc.
 *  - [Cau]: xep the tu thanh cau, moi the mot lan.
 *  - [BieuThuc]: go tung phim (so, bien, luy thua...), phim dung lai duoc.
 *  - [Buoc]: chon cac dong loi giai theo thu tu, moi dong mot lan.
 *  - [O]: moi dong mot hay vai o, moi o chon mot phuong an.
 *
 * So sao va cach tru sao o [vn.huytl.homeworkgate.data.LuatGhep]; o day chi biet cau
 * tra loi dung hay sai.
 */
sealed class Ghep {
    /** So sao toi da, cung la so phut toi da cua cau. */
    abstract val sao: Int

    /** Chu hien tren may thay cho de bai, rong la hien de bai. */
    abstract val hoi: String

    data class Chon(
        override val sao: Int,
        override val hoi: String,
        val cac: List<String>,
        /** Chi so cac phuong an dung, 0 la phuong an dau (chu A). Thuong chi mot. */
        val dap: Set<Int>,
        /**
         * Bam chon duoc nhieu phuong an ("Sometimes you can choose two answers", bai B4
         * Unit 1 SBT Anh 8). Bat cho CA bai, ke ca cau chi mot dap an: bat rieng cau hai
         * dap an thi nhin cach bam la con biet cau nao co hai.
         *
         * Van mot sao nhu moi cau trac nghiem. Ban soan dau (29/9/2026) tach moi phuong an
         * thanh mot o Dung/Sai, 4 sao mot cau, thanh 24 phut cho sau cau trac nghiem.
         */
        val nhieuDap: Boolean = false
    ) : Ghep() {
        fun dung(chon: Set<Int>): Boolean = chon == dap
    }

    data class DungSai(
        override val sao: Int,
        override val hoi: String,
        val dap: Boolean,
        /** Nhan hai nut, nut "dung" truoc. */
        val nhan: List<String>
    ) : Ghep() {
        fun dung(chon: Boolean): Boolean = chon == dap
    }

    data class Chu(
        override val sao: Int,
        override val hoi: String,
        /** Phan sach cho san (chu cai dau), hien san, con khong phai go. */
        val truoc: String,
        val dap: List<String>,
        val nhieu: List<String>
    ) : Ghep() {
        /** Phan con phai go cua tung dap an, viet thuong. */
        private val phanGo: List<String> =
            dap.map { d -> d.drop(truoc.length).lowercase() }

        /** Cac chu cai that, theo thu tu gap trong dap an. Dau cach tach rieng, xem [coCach]. */
        val phimThat: List<String> =
            phanGo.flatMap { p -> p.filterNot { it.isWhitespace() }.map { it.toString() } }.distinct()

        /** Dap an co dau cach thi phai co phim cach. */
        val coCach: Boolean = phanGo.any { p -> p.any { it.isWhitespace() } }

        /** So o chu khi moi dap an dai bang nhau, null thi hien mot dong go tu do. */
        val soO: Int? = phanGo.map { it.length }.distinct().singleOrNull()

        /** [go] la phan con go, khong kem [truoc]. */
        fun dung(go: String): Boolean {
            val cua = HocThuoc.chuanHoa(truoc + go)
            return cua.isNotEmpty() && dap.any { HocThuoc.chuanHoa(it) == cua }
        }
    }

    data class Cau(
        override val sao: Int,
        override val hoi: String,
        val the: List<String>,
        val nhieu: List<String>,
        val dap: List<String>
    ) : Ghep() {
        fun dung(cacThe: List<String>): Boolean {
            val cau = gonCach(cacThe.joinToString(" "))
            return cau.isNotEmpty() && dap.any { gonCach(it) == cau }
        }
    }

    data class BieuThuc(
        override val sao: Int,
        override val hoi: String,
        val dap: List<String>,
        /** Phim ngoai bo co ban, xem [phimCoBan]. */
        val phim: List<String>,
        val nhieu: List<String>,
        val loiGiai: List<String>,
        /**
         * Thu tu hang tu la chinh dieu de hoi ("sắp xếp theo luỹ thừa giảm dần"): chi nhan
         * dung thu tu trong [dap]. Mac dinh la khong, xem [khongKeThuTu].
         */
        val thuTu: Boolean = false
    ) : Ghep() {
        /**
         * Giu hoa thuong: phim da co dinh chu hoa chu thuong ("Na", "CO"), con khong go ra
         * duoc cach viet khac. Bo di thi "Co" (cobalt) thanh dung cho dap an "CO".
         *
         * KHONG KE THU TU HANG TU. Ban soan Chuong I SBT Toan (29/9/2026) phai liet ke toi sau
         * cach xep cho mot da thuc, ma da thuc sau hang tu co 720 cach: con go dung ma khac
         * thu tu la mat sao oan. Nen hai ben duoc dua ve dang [khongKeThuTu] roi moi so.
         * [mon] Toan thi xep ca thua so trong tung don thuc ("yx²" nhu "x²y"); mon khac thi
         * khong, vi "H₂O" va "HO₂" la hai chat khac nhau.
         */
        fun dung(go: String, mon: String = ""): Boolean {
            val cua = HocThuoc.chuanHoa(go, giuHoa = true)
            if (cua.isEmpty()) return false
            if (dap.any { HocThuoc.chuanHoa(it, giuHoa = true) == cua }) return true
            if (thuTu) return false
            val sap = mon == "Toán"
            val cuaGon = khongKeThuTu(cua, sap) ?: return false
            return dap.any { khongKeThuTu(HocThuoc.chuanHoa(it, giuHoa = true), sap) == cuaGon }
        }
    }

    data class Buoc(
        override val sao: Int,
        override val hoi: String,
        val buoc: List<String>,
        val nhieu: List<String>,
        /** Cac thu tu khac cung dung, bang chi so cua [buoc]. */
        val thuTuKhac: List<List<Int>>
    ) : Ghep() {
        fun dung(chon: List<String>): Boolean {
            val gon = chon.map(::gonCach)
            return (listOf(buoc.indices.toList()) + thuTuKhac).any { thuTu ->
                thuTu.size == gon.size && thuTu.indices.all { i -> gonCach(buoc[thuTu[i]]) == gon[i] }
            }
        }
    }

    data class O(
        override val sao: Int,
        override val hoi: String,
        /** Hoi truoc ban nam hay ban nu roi thay {He}, {his}... Xem [thayGioi]. */
        val gioi: Boolean,
        val dong: List<Dong>
    ) : Ghep() {
        data class Dong(val chu: String, val o: List<Ong>)

        /**
         * Mot o: [go] la o go tu do (ten rieng), khong cham, [goiY] la chu mo trong o do; con
         * lai chon trong [dung] + [sai].
         */
        data class Ong(val go: Boolean, val dung: List<String>, val sai: List<String>, val goiY: String = "")

        /** Cac o chon (khong ke o go), theo thu tu dong roi thu tu trong dong. */
        val cacOChon: List<Pair<Int, Int>> =
            dong.flatMapIndexed { i, d -> d.o.indices.filter { !d.o[it].go }.map { i to it } }

        /**
         * So o chon sai. [chon] theo dung hinh dang [dong]: chon[i][k] la phuong an con
         * chon o o k cua dong i, null la chua chon (cung tinh la sai).
         */
        fun soSai(chon: List<List<String?>>, nu: Boolean): Int = cacOChon.count { (i, k) ->
            val x = chon.getOrNull(i)?.getOrNull(k) ?: return@count true
            dong[i].o[k].dung.none { thayGioi(it, nu) == x }
        }
    }

    companion object {
        /**
         * Doc mot khoi ghep. Hong hay kieu la thi tra null: cau do khong lam tren may duoc,
         * chu khong lam do ca man hinh. File da qua tools/ghep/kiem.py nen hiem khi gap.
         */
        fun doc(json: String): Ghep? {
            if (json.isBlank()) return null
            return runCatching { docObject(JSONObject(json)) }.getOrNull()
        }

        private fun docObject(o: JSONObject): Ghep? {
            val sao = o.optInt("sao", 0)
            if (sao !in 1..LuatSao.TRAN_SAO) return null
            val hoi = o.optString("hoi")
            return when (o.optString("kieu")) {
                "CHON" -> {
                    val cac = chuoi(o.optJSONArray("cac"))
                    // "dap" la mot chu cai, hay danh sach chu cai khi co "nhieu_dap".
                    val chu = o.optJSONArray("dap")?.let(::chuoi) ?: listOf(o.optString("dap"))
                    val dap = chu.mapNotNull { c -> c.trim().singleOrNull()?.let { it - 'A' } }.toSet()
                    if (cac.size < 2 || dap.isEmpty() || dap.any { it !in cac.indices }) null
                    else Chon(sao, hoi, cac, dap, o.optBoolean("nhieu_dap"))
                }
                "DUNG_SAI" -> DungSai(
                    sao, hoi, o.optBoolean("dap"),
                    chuoi(o.optJSONArray("nhan")).takeIf { it.size == 2 } ?: listOf("Đúng", "Sai")
                )
                "CHU" -> {
                    val dap = chuoi(o.optJSONArray("dap"))
                    if (dap.isEmpty()) null
                    else Chu(sao, hoi, o.optString("truoc"), dap, chuoi(o.optJSONArray("nhieu")))
                }
                "CAU" -> {
                    val the = chuoi(o.optJSONArray("the"))
                    val dap = chuoi(o.optJSONArray("dap"))
                    if (the.isEmpty() || dap.isEmpty()) null
                    else Cau(sao, hoi, the, chuoi(o.optJSONArray("nhieu")), dap)
                }
                "BIEU_THUC" -> {
                    val dap = chuoi(o.optJSONArray("dap"))
                    if (dap.isEmpty()) null
                    else BieuThuc(
                        sao, hoi, dap, chuoi(o.optJSONArray("phim")),
                        chuoi(o.optJSONArray("nhieu")), chuoi(o.optJSONArray("loi_giai")),
                        o.optBoolean("thu_tu")
                    )
                }
                "BUOC" -> {
                    val buoc = chuoi(o.optJSONArray("buoc"))
                    val khac = o.optJSONArray("thu_tu_khac")?.let { a ->
                        (0 until a.length()).mapNotNull { i ->
                            a.optJSONArray(i)?.let { t -> (0 until t.length()).map { t.optInt(it) } }
                        }
                    }.orEmpty().filter { it.sorted() == buoc.indices.toList() }
                    if (buoc.size < 2) null else Buoc(sao, hoi, buoc, chuoi(o.optJSONArray("nhieu")), khac)
                }
                "O" -> {
                    val a = o.optJSONArray("dong") ?: return null
                    val dong = (0 until a.length()).mapNotNull { i ->
                        val d = a.optJSONObject(i) ?: return@mapNotNull null
                        val cacO = d.optJSONArray("o") ?: return@mapNotNull null
                        O.Dong(
                            d.optString("chu"),
                            (0 until cacO.length()).mapNotNull { k ->
                                val x = cacO.optJSONObject(k) ?: return@mapNotNull null
                                O.Ong(
                                    x.optBoolean("go"),
                                    chuoi(x.optJSONArray("dung")),
                                    chuoi(x.optJSONArray("sai")),
                                    x.optString("goi_y")
                                )
                            }
                        )
                    }
                    if (dong.isEmpty()) null else O(sao, hoi, o.optBoolean("gioi"), dong)
                }
                else -> null
            }
        }

        private fun chuoi(a: JSONArray?): List<String> =
            if (a == null) emptyList()
            else (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }

        /**
         * Bo phim luon co, theo mon. Giu dung thu tu va dung ky tu voi PHIM_CO_BAN ben
         * tools/ghep/kiem.py: file du lieu khong ghi lai cac phim nay.
         */
        fun phimCoBan(mon: String): List<String> = when (mon) {
            "Toán" -> SO + listOf("+", "−", "×", ":", "/", "=", "(", ")", ",")
            "Khoa học tự nhiên" -> SO + listOf("+", "−", "·", ":", "/", "=", "(", ")", ",", "→")
            else -> emptyList()
        }

        private val SO = (0..9).map { it.toString() }

        /**
         * Thu gon dau cach va bo dau cach truoc dau cau: "She  enjoys piano ." thanh
         * "She enjoys piano.". Cung luat voi gon_cach ben tools/ghep/kiem.py.
         */
        fun gonCach(chu: String): String =
            Normalizer.normalize(chu, Normalizer.Form.NFC)
                .replace(Regex("\\s+"), " ").trim()
                .replace(Regex(" ([,.;:!?])"), "$1")

        /**
         * Dang chuan khong ke thu tu cua mot bieu thuc da qua [HocThuoc.chuanHoa], de hai
         * cach viet chi khac thu tu hang tu so ra bang nhau. Xem [BieuThuc.dung].
         *
         * Tach theo ";" roi theo cac dau noi hai ve ("=", "→", "<"...), giu nguyen thu tu cac
         * ve: "x = 2" khac "2 = x". Moi ve tach thanh cac hang tu o tang ngoai ngoac, moi hang
         * tu giu dau cua no, roi xep lai; trong ngoac cung vay. [sapThuaSo] thi xep ca thua so
         * trong hang tu, nhung chi hang tu toan so, chu thuong, luy thua, ngoac va dau nhan:
         * gap chu hoa (ten diem, "ΔABC"), chu la (π), can hay phep chia giua chung thi giu
         * nguyen thu tu trong hang tu do. "1/2x" van xep duoc: phan so dung dau la he so, dung
         * cach bo the Hoc thuoc da nhan.
         *
         * Khong gop gi ca: "xyx" khong thanh "x²y", "2x + 3x" khong thanh "5x". Cau hoi rut
         * gon van phai nhan ra ban chua rut gon la sai.
         *
         * Tra null khi tach khong duoc (ngoac lech, hai dau lien nhau nhu "Na⁺ + Cl⁻" doi ra
         * "Na++Cl-"): ben goi coi nhu khong co dang chuan, chi so nguyen chuoi.
         */
        fun khongKeThuTu(s: String, sapThuaSo: Boolean): String? {
            val (doan, cham) = tachTang(s, ";") ?: return null
            val gon = doan.map { d ->
                val (ve, noi) = tachTang(d, NOI_VE) ?: return null
                noiLai(ve.map { chuanVe(it, sapThuaSo) ?: return null }, noi)
            }
            return noiLai(gon, cham)
        }

        /** Dau noi hai ve. Thu tu cac ve giu nguyen, chi thu tu trong tung ve la khong ke. */
        private const val NOI_VE = "=→<>≤≥≠"

        /**
         * Tach [s] theo cac ky tu trong [cat] nam ngoai ngoac, tra cac manh kem chuoi dau da
         * cat theo thu tu. null la ngoac lech.
         */
        private fun tachTang(s: String, cat: String): Pair<List<String>, String>? {
            val manh = mutableListOf<String>()
            val noi = StringBuilder()
            var sau = 0
            var tu = 0
            for ((i, c) in s.withIndex()) {
                if (c == '(') sau++ else if (c == ')') sau--
                if (sau < 0) return null
                if (sau == 0 && c in cat) {
                    manh += s.substring(tu, i)
                    noi.append(c)
                    tu = i + 1
                }
            }
            if (sau != 0) return null
            manh += s.substring(tu)
            return manh to noi.toString()
        }

        private fun noiLai(manh: List<String>, noi: String): String = buildString {
            manh.forEachIndexed { i, m ->
                if (i > 0) append(noi[i - 1])
                append(m)
            }
        }

        /** Mot ve: cac hang tu co dau, xep lai. Ve rong (dong bat dau bang "=") giu rong. */
        private fun chuanVe(v: String, sap: Boolean): String? {
            if (v.isEmpty()) return v
            val hang = mutableListOf<String>()
            var sau = 0
            var dau = '+'
            var tu = 0
            for ((i, c) in v.withIndex()) {
                if (c == '(') sau++ else if (c == ')') sau--
                if (sau != 0 || (c != '+' && c != '-')) continue
                if (i == 0) {
                    dau = c
                    tu = 1
                    continue
                }
                // Dau cua so mu ("x^-2") hay cua so sau dau nhan chia ("2.-3"), khong phai
                // cho tach hang tu.
                if (v[i - 1] in "^./") continue
                val than = v.substring(tu, i)
                if (than.isEmpty()) return null
                hang += "$dau" + (chuanHang(than, sap) ?: return null)
                dau = c
                tu = i + 1
            }
            val cuoi = v.substring(tu)
            if (cuoi.isEmpty()) return null
            hang += "$dau" + (chuanHang(cuoi, sap) ?: return null)
            return hang.sorted().joinToString("")
        }

        /**
         * Mot hang tu khong dau, tach thanh thua so noi bang "." de "2·3x" khong thanh "23x".
         * Ngoac chuan hoa de quy. [sap] thi xep: so truoc, roi chu, roi ngoac.
         */
        private fun chuanHang(h: String, sap: Boolean): String? {
            val thua = mutableListOf<String>()
            var xep = sap
            var i = 0
            while (i < h.length) {
                val c = h[i]
                when {
                    c == '(' -> {
                        var sau = 0
                        var j = i
                        while (j < h.length) {
                            if (h[j] == '(') sau++
                            if (h[j] == ')') {
                                sau--
                                if (sau == 0) break
                            }
                            j++
                        }
                        if (j >= h.length) return null
                        val trong = khongKeThuTu(h.substring(i + 1, j), sap) ?: return null
                        val mu = khopTai(MU, h, j + 1)
                        thua += "($trong)$mu"
                        i = j + 1 + mu.length
                    }
                    // Chi chu so ASCII: chuanHoa da doi so mu, so duoi ve ASCII, con isDigit()
                    // nhan ca chu so Unicode khac ma regex \d khong khop, vong lap se dung im.
                    c in '0'..'9' -> {
                        val so = khopTai(if (thua.isEmpty()) HE_SO else SO_GIUA, h, i)
                        thua += so
                        i += so.length
                    }
                    c in 'a'..'z' -> {
                        val mu = khopTai(MU, h, i + 1)
                        thua += "$c$mu"
                        i += 1 + mu.length
                    }
                    c == '.' -> i++
                    else -> {
                        xep = false
                        thua += c.toString()
                        i++
                    }
                }
            }
            if (!xep) return thua.joinToString(".")
            return thua.sortedWith(compareBy<String>({ loaiThua(it) }, { it })).joinToString(".")
        }

        private fun loaiThua(t: String): Int = when {
            t[0] in '0'..'9' -> 0
            t[0] == '(' -> 2
            else -> 1
        }

        /** Chuoi [r] khop dung tai vi tri [tu] cua [s], rong la khong khop. */
        private fun khopTai(r: Regex, s: String, tu: Int): String =
            if (tu >= s.length) "" else r.find(s, tu)?.takeIf { it.range.first == tu }?.value.orEmpty()

        /** So mu sau chuanHoa: "^2", "^-3". */
        private val MU = Regex("\\^-?\\d+")

        /** He so dung dau hang tu: so thap phan phay, co the la phan so "1/2", co the co mu. */
        private val HE_SO = Regex("\\d+(,\\d+)?(/\\d+(,\\d+)?)?(\\^-?\\d+)?")

        /** So giua hang tu: khong nhan phan so, "x/2" khong phai he so. */
        private val SO_GIUA = Regex("\\d+(,\\d+)?(\\^-?\\d+)?")

        /** Thay {He}, {his}... theo ban nam hay nu. Xem [O.gioi]. */
        fun thayGioi(chu: String, nu: Boolean): String = GIOI.replace(chu) { m ->
            when (m.groupValues[1]) {
                "He" -> if (nu) "She" else "He"
                "he" -> if (nu) "she" else "he"
                "His" -> if (nu) "Her" else "His"
                "his" -> if (nu) "her" else "his"
                "Him" -> if (nu) "Her" else "Him"
                "him" -> if (nu) "her" else "him"
                else -> m.value
            }
        }

        private val GIOI = Regex("""\{(He|he|His|his|Him|him)\}""")
    }
}

/** Hang so chung cua luat sao, de [Ghep] khong phai phu thuoc vao tang data. */
object LuatSao {
    /** Mot cau toi da bay nhieu sao, cung la bay nhieu phut. */
    const val TRAN_SAO = 20
}
