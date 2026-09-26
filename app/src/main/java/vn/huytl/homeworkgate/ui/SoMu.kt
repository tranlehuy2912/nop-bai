package vn.huytl.homeworkgate.ui

/**
 * Doi so mu viet bang dau "^" trong de bai ra chu so mu that, chi de hien cho con doc:
 * "x^2y^3" thanh "x²y³", "10^23" thanh "10²³", ion "Ca^2+" thanh "Ca²⁺".
 *
 * VI SAO CO. Con khong co sach bai tap giay: cau SBT con doc ngay tren tablet, o man
 * chon cau. Mot dong "(x^2 − 1)^2/((x + 1)(x^3 + 1))" doc kho hon nhieu so voi ban in.
 *
 * CHI DOI LUC HIEN, khong doi chu luu trong ngan hang. De luu voi dau "^" di thang vao
 * cau lenh cham cua AI va vao phep so de cua so cai
 * ([vn.huytl.homeworkgate.data.SoCaiBai.chuanHoa]); doi o goc la doi ca hai cho do.
 * Chieu nguoc lai, con go "²" luc hoc thuoc, da co [vn.huytl.homeworkgate.data.HocThuoc].
 *
 * Chi doi cai chac chan: chu so ngay sau "^", va dau cua ion dung sat sau chu so do hay
 * sat sau "^" ("Ca^2+", "H^+"). Dau "−" dung sau so mu ma lien voi so hay chu, nhu
 * "x^2−1", la phep tru nen giu nguyen. Kieu khac ("^(n+1)", "^x") thi de nguyen dau
 * "^": doi sai thi con doc sai de, con de nguyen thi chi kem dep.
 */
object SoMu {

    private const val SO = "0123456789"
    private const val SO_MU = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    /** Chu so ngay sau "^", co the kem mot dau ion; hoac chi mot dau ion ("H^+"). */
    private val MU = Regex("""\^(\d*)([+−-](?![\p{L}\d(]))?""")

    fun hien(chu: String): String {
        if ('^' !in chu) return chu
        return MU.replace(chu) { m ->
            val so = m.groupValues[1]
            val dau = m.groupValues[2]
            if (so.isEmpty() && dau.isEmpty()) {
                m.value
            } else {
                so.map { SO_MU[SO.indexOf(it)] }.joinToString("") +
                    when (dau) {
                        "" -> ""
                        "+" -> "⁺"
                        else -> "⁻"
                    }
            }
        }
    }
}
