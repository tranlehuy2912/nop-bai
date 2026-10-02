package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.HocThuoc
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.PhimKiemTra
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhanHoc
import vn.huytl.homeworkgate.kho.TheHoc
import vn.huytl.homeworkgate.kho.TraThe

/**
 * Luat cham cua duong hoc thuoc.
 *
 * Khong dung den may hay mang, nhung de o day cho cung cho voi cac bo test kia.
 * Bo nay KHONG xoa prefs. No co nap cac bo the that vao kho, y het luc app khoi dong.
 *
 * Phan dang test ky nhat la [HocThuoc.chuanHoa], vi no la cho quyet dinh con bi
 * bao sai oan hay khong. Moi cho no CHO DI deu co mot test, va moi cho no KHONG
 * cho di cung vay - cai thu hai moi quan trong: bo nham dau tieng Viet thi ca bo
 * the tu vung tro thanh vo nghia ma khong ai bao loi.
 */
@RunWith(AndroidJUnit4::class)
class HocThuocTest {

    private fun the(dap: String, vararg khac: String) = TheHoc(
        id = "thu:1", mon = "Toán", bo = "thu", bai = "bài thử",
        hoi = "hỏi", dap = dap, dapKhac = khac.toList()
    )

    // --- nhung cho chuan hoa CHO DI ---

    @Test
    fun chu_hoa_chu_thuong_la_mot() {
        assertTrue(HocThuoc.dung("A Book", the("a book")))
    }

    @Test
    fun thua_thieu_dau_cach_van_dung() {
        assertTrue(HocThuoc.dung("a²+2ab+b²", the("a² + 2ab + b²")))
        assertTrue(HocThuoc.dung("  a² + 2ab + b²  ", the("a² + 2ab + b²")))
    }

    @Test
    fun go_mu_kieu_ban_phim_thuong_van_dung() {
        assertTrue(HocThuoc.dung("a^2 + 2ab + b^2", the("a² + 2ab + b²")))
        assertTrue(HocThuoc.dung("a^3 - b^3", the("a³ − b³")))
    }

    @Test
    fun ba_loai_gach_ngang_la_mot() {
        // Dau tru toan hoc, gach ngang ngan, gach ngang dai, va dau tru ban phim.
        assertTrue(HocThuoc.dung("a - b", the("a − b")))
        assertTrue(HocThuoc.dung("a – b", the("a − b")))
        assertTrue(HocThuoc.dung("a — b", the("a − b")))
    }

    @Test
    fun dau_cham_cuoi_cau_khong_tinh() {
        assertTrue(HocThuoc.dung("a book.", the("a book")))
    }

    // --- nhung cho chuan hoa KHONG cho di ---

    @Test
    fun thieu_dau_tieng_viet_la_sai() {
        assertFalse(HocThuoc.dung("ma", the("mà")))
    }

    @Test
    fun sai_chinh_ta_tieng_anh_la_sai() {
        assertFalse(HocThuoc.dung("libary", the("library")))
    }

    @Test
    fun thieu_mot_hang_tu_la_sai() {
        assertFalse(HocThuoc.dung("a² + b²", the("a² + 2ab + b²")))
    }

    @Test
    fun de_trong_la_sai() {
        assertFalse(HocThuoc.dung("", the("a book")))
        assertFalse(HocThuoc.dung("   ", the("a book")))
    }

    // --- dap an khac ---

    @Test
    fun ban_viet_khac_ke_trong_file_cung_tinh_dung() {
        val t = the("(a − b)(a + b)", "(a + b)(a − b)")
        assertTrue(HocThuoc.dung("(a + b)(a − b)", t))
        assertTrue(HocThuoc.dung("(a-b)(a+b)", t))
    }

    @Test
    fun ban_viet_khong_ke_trong_file_thi_khong_tinh() {
        // Doi cho hai hang tu la dung ve toan, nhung the nay khong khai ra nen may
        // khong tu doan. Muon tinh thi them vao dap_khac trong file.
        assertFalse(HocThuoc.dung("b² + 2ab + a²", the("a² + 2ab + b²")))
    }

    // --- ki hieu cua bo KHTN ---

    @Test
    fun chi_so_duoi_go_bang_so_thuong_van_dung() {
        assertTrue(HocThuoc.dung("H2SO4", the("H₂SO₄"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("(NH2)2CO", the("(NH₂)₂CO"), phanBietHoa = true))
    }

    @Test
    fun mu_nhieu_chu_so_doi_ca_cum() {
        // Doi tung chu thi "10²³" ra "10^2^3", ma con go "10^23".
        assertEquals("10^23", HocThuoc.chuanHoa("10²³"))
        assertTrue(HocThuoc.dung("6,022.10^23", the("6,022·10²³")))
        assertFalse(HocThuoc.dung("6,022.10^2^3", the("6,022·10²³")))
    }

    @Test
    fun cac_kieu_dau_nhan_dau_chia_la_mot() {
        val t = the("FA = d.V")
        assertTrue(HocThuoc.dung("FA = d·V", t, phanBietHoa = true))
        assertTrue(HocThuoc.dung("FA = d×V", t, phanBietHoa = true))
        assertTrue(HocThuoc.dung("FA=d*V", t, phanBietHoa = true))
        assertTrue(HocThuoc.dung("D = m : V", the("D = m/V"), phanBietHoa = true))
    }

    @Test
    fun mic_ghi_dau_nhan_thanh_chu_x_van_dung() {
        // Doc "a nhân b" vao mic thi ban phim ghi "a x b", chu x thuong.
        assertTrue(HocThuoc.dung("a x b + a x c", the("A.B + A.C", "AB + AC")))
        assertTrue(HocThuoc.dung("FA = d x V", the("FA = d.V"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("V = 24,79 x n", the("V = 24,79·n"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("1/3 x S x h", the("1/3.S.h")))
        assertTrue(HocThuoc.dung("b X c", the("b.c")))
    }

    @Test
    fun chu_x_la_bien_thi_van_doc_la_chu_x() {
        // Mic tach bien x ra rieng: cach doc chu x van con.
        assertTrue(HocThuoc.dung("y = a x + b", the("y = ax + b")))
        // Chu x dinh lien chu khac khong phai dau nhan.
        assertFalse(HocThuoc.dung("axb", the("a.b")))
        assertTrue(HocThuoc.dung("−2x³y²z", the("−2x³y²z")))
    }

    @Test
    fun dau_phay_tren_ban_phim_uon_van_la_mot() {
        assertTrue(HocThuoc.dung("H = m’/m·100%", the("H = m'/m·100%"), phanBietHoa = true))
    }

    @Test
    fun ion_go_bang_dau_cong_tru_thuong() {
        assertTrue(HocThuoc.dung("H+", the("H⁺"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("OH-", the("OH⁻"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("OH−", the("OH⁻"), phanBietHoa = true))
    }

    @Test
    fun hai_kieu_ma_cua_chu_co_dau_la_mot() {
        // "a" kem dau huyen dung rieng, nhin y het "à" go lien.
        assertTrue(HocThuoc.dung("màu đỏ", the("màu đỏ")))
    }

    @Test
    fun ky_tu_an_rong_bang_khong_khong_lam_sai() {
        assertTrue(HocThuoc.dung("đỏ‌", the("đỏ")))
        assertTrue(HocThuoc.dung("H​2O", the("H₂O"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("﻿CO2", the("CO₂"), phanBietHoa = true))
    }

    @Test
    fun bo_phan_biet_hoa_thi_CO_khac_Co() {
        val t = the("CO")
        assertTrue(HocThuoc.dung("CO", t, phanBietHoa = true))
        assertFalse(HocThuoc.dung("Co", t, phanBietHoa = true))
        assertFalse(HocThuoc.dung("co", t, phanBietHoa = true))
        // Bo Toan va tu vung khong bat co nay, van bo qua hoa thuong nhu cu.
        assertTrue(HocThuoc.dung("co", t))
    }

    @Test
    fun bo_phan_biet_hoa_thi_chu_dau_cung_phai_dung() {
        // P la trong luong, p la ap suat: go "P = F/S" la nham hai dai luong.
        assertFalse(HocThuoc.dung("P = F/S", the("p = F/S"), phanBietHoa = true))
        // D la khoi luong rieng, d la trong luong rieng. Sai chieu nao cung la sai.
        assertFalse(HocThuoc.dung("D = P/V", the("d = P/V"), phanBietHoa = true))
        assertFalse(HocThuoc.dung("d = m/V", the("D = m/V"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("p = F/S", the("p = F/S"), phanBietHoa = true))
    }

    @Test
    fun nhan_viet_thuong_chi_dung_khi_co_ke_trong_dap_khac() {
        // Chu A trong FA chi la nhan, nen file ke them ban viet thuong.
        val t = the("FA = d.V", "Fa = d.V")
        assertTrue(HocThuoc.dung("Fa = d.V", t, phanBietHoa = true))
        assertFalse(HocThuoc.dung("Fa = d.V", the("FA = d.V"), phanBietHoa = true))
    }

    @Test
    fun dap_an_toan_chu_thuong_thi_hoa_thuong_gi_cung_duoc() {
        assertTrue(HocThuoc.dung("Đỏ", the("đỏ"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("KG/M³", the("kg/m³"), phanBietHoa = true))
        // Dap an phu viet thuong thi cung vay, du dap an chinh co chu hoa.
        assertTrue(HocThuoc.dung("Ampe", the("ampe (A)", "ampe", "A"), phanBietHoa = true))
        assertFalse(HocThuoc.dung("a", the("ampe (A)", "ampe", "A"), phanBietHoa = true))
    }

    // --- cac bo the that trong assets ---

    /**
     * Moi bo that nap du so the trong file, va moi dap an ghep duoc bang phim ghep cua the do.
     *
     * SO THE TRONG FILE PHAI BANG SO THE TRONG BANG, cung ly do voi bo tu vung:
     * the thieu ma, thieu hoi hay thieu dap thi [BoThe] bo qua im lang, con hai the
     * trung ma thi the sau de len the truoc. Ca hai deu chi hien ra o day.
     *
     * GHEP BANG PHIM CUA THE (2/10/2026): man kiem tra bai khong con o go ban phim Android, moi
     * the mot khoi ghep dung tu chinh dap an ([PhimKiemTra.ghepThe]). Phep thu nay ghep dap an
     * chinh tu cac phan cua no - the chu thi noi the bang dau cach, bieu thuc thi noi phim - va
     * doi moi phan phai co tren ban phim (the that, phim rieng hay hang phim co ban), roi cham
     * bang dung [HocThuoc.dung] cua man. Truoc ngay do phep thu doi dap an sang dang go tren ban
     * phim thuong va kiem tung ky hieu co nut tren dai ky tu cua mon.
     */
    @Test
    fun bo_the_that_nap_du_va_ghep_duoc_bang_phim_cua_the() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BoThe.napNeuCan(context)
        val kho = KhoBai.get(context)
        BoThe.BO.forEach { bo ->
            val o = JSONObject(context.assets.open(bo.file).bufferedReader().use { it.readText() })
            val cacBai = o.getJSONArray("cac_bai")
            val soThe = (0 until cacBai.length()).sumOf { cacBai.getJSONObject(it).getJSONArray("cac_the").length() }
            assertEquals("${bo.bo}: file $soThe the ma bang co", soThe, kho.soTheCua(bo.bo))

            val caBo = kho.cacTheCua(bo.bo)
            assertEquals(soThe, caBo.size)
            caBo.forEach { t ->
                val k = PhimKiemTra.ghepThe(t, caBo, bo.phanBietHoa, hat = t.id.hashCode())
                val coTrenPhim: Set<String> = when (val g = k.ghep) {
                    is Ghep.Cau -> g.the.toSet()
                    is Ghep.BieuThuc -> (g.phim + Ghep.phimCoBan(t.mon)).toSet()
                    else -> emptySet()
                }
                val thieu = k.phan.filter { it !in coTrenPhim }
                assertTrue("${t.id}: \"${t.dap}\" thieu phim $thieu", thieu.isEmpty())
                val ghep = if (k.ghep is Ghep.Cau) k.phan.joinToString(" ") else k.phan.joinToString("")
                assertTrue(
                    "${t.id} khong nhan \"$ghep\" ghep tu phim cho \"${t.dap}\"",
                    HocThuoc.dung(ghep, t, bo.phanBietHoa)
                )
            }
        }
    }

    /**
     * Phim nhieu: du so (6 the chu, 4 phim) khi the co phim rieng, khong trung phan nao cua dap
     * an, va khong la ban chi khac hoa thuong cua mot phan dap an khi bo khong phan biet hoa. Dap
     * an toan chu so thi khong co nhieu.
     */
    @Test
    fun phim_nhieu_cua_the_that_du_so_va_khong_trung_dap_an() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BoThe.napNeuCan(context)
        val kho = KhoBai.get(context)
        var thieuNhieu = 0
        BoThe.BO.forEach { bo ->
            val caBo = kho.cacTheCua(bo.bo)
            caBo.forEach { t ->
                val k = PhimKiemTra.ghepThe(t, caBo, bo.phanBietHoa, hat = 7)
                fun khoa(x: String) = if (bo.phanBietHoa) x else x.lowercase()
                val phan = k.phan.map(::khoa).toSet()
                val (nhieu, can) = when (val g = k.ghep) {
                    is Ghep.Cau -> g.nhieu to PhimKiemTra.SO_NHIEU_CHU
                    is Ghep.BieuThuc -> g.nhieu to (if (g.phim.isEmpty()) 0 else PhimKiemTra.SO_NHIEU_PHIM)
                    else -> emptyList<String>() to 0
                }
                assertTrue("${t.id}: nhieu ${nhieu} trung dap an ${k.phan}", nhieu.none { khoa(it) in phan })
                assertEquals("${t.id}: nhieu trung nhau", nhieu.size, nhieu.map(::khoa).toSet().size)
                if (nhieu.size < can) {
                    thieuNhieu++
                    println("HOCTHUOC: ${t.id} \"${t.dap}\" chi co ${nhieu.size}/$can nhieu: $nhieu")
                }
            }
        }
        assertEquals("co the thieu phim nhieu (xem logcat HOCTHUOC)", 0, thieuNhieu)
    }

    @Test
    fun phan_loai_dap_an_chu_hay_bieu_thuc() {
        listOf("số mũ", "cộng", "đỏ", "acid", "chloride", "oxide lưỡng tính", "ampe (A)", "vôn (V)")
            .forEach { assertEquals(it, PhimKiemTra.Kieu.CHU, PhimKiemTra.kieuCua(it)) }
        listOf("−6", "x³y", "k", "MTC", "HCl", "CO", "NaOH", "Pa", "I", "M = m/n", "360°", "a ≠ 0")
            .forEach { assertEquals(it, PhimKiemTra.Kieu.BIEU_THUC, PhimKiemTra.kieuCua(it)) }
    }

    @Test
    fun tach_bieu_thuc_theo_mon() {
        val toan = "Toán"
        val khtn = "Khoa học tự nhiên"
        assertEquals(listOf("−", "2", "x", "y", "²"), PhimKiemTra.tach("−2xy²", toan))
        assertEquals(listOf("A", "C", "'", "/", "A", "C"), PhimKiemTra.tach("AC'/AC", toan))
        // Tu tieng Viet trong cong thuc Toan giu ca tu.
        assertEquals(
            listOf("a", "=", "a", "'", "và", "b", "≠", "b", "'"),
            PhimKiemTra.tach("a = a' và b ≠ b'", toan)
        )
        // KHTN: ky hieu nguyen to, cum chu thuong la mot dai luong.
        assertEquals(listOf("H", "Cl"), PhimKiemTra.tach("HCl", khtn))
        assertEquals(listOf("Fe", "(", "O", "H", ")", "₃"), PhimKiemTra.tach("Fe(OH)₃", khtn))
        assertEquals(listOf("S", "=", "mct", "/", "mnước", "·", "1", "0", "0"), PhimKiemTra.tach("S = mct/mnước·100", khtn))
        assertEquals(listOf("số", "mũ"), PhimKiemTra.tach("số mũ", toan))
    }

    @Test
    fun nut_dien_tich_go_ra_dung_ion() {
        // "H^+" khong phai "H⁺": "^+" khong quy ve "+". Phim ghep cua the "H⁺" co phim "⁺"
        // (2/10/2026; truoc do la nut "⁺" cua dai ky tu KHTN).
        assertFalse(HocThuoc.dung("H^+", the("H⁺"), phanBietHoa = true))
        assertTrue(HocThuoc.dung("H⁺", the("H⁺"), phanBietHoa = true))
        // Bo the nao ghi dap an kieu ban phim thi bam phim ky hieu van khop.
        assertTrue(HocThuoc.dung("SO₄²⁻", the("SO4^2-"), phanBietHoa = true))
    }

    @Test
    fun go_lan_ky_hieu_va_chu_thuong_van_dung() {
        // Chi so dau bang ky hieu, chi so sau bang so thuong: van la mot cong thuc.
        assertTrue(HocThuoc.dung("H₂SO4", the("H₂SO₄"), phanBietHoa = true))
        // Phim "×" o hang co ban Toan cham nhu dau nhan in trong sach.
        assertTrue(HocThuoc.dung("2×a×b", the("2·a·b")))
        assertTrue(HocThuoc.dung("2*a*b", the("2·a·b")))
    }

    /**
     * Bac goi y cua man kiem tra bai (2/10/2026): dien san nua dau o lan sai thu ba, hien het o
     * lan thu tu. Phan dien san da la mot dap an phu duoc cham dung thi tinh nhu hien het:
     * con bam Tra loi ngay la dung, khong duoc gio.
     */
    @Test
    fun dien_san_trung_dap_an_phu_thi_tinh_nhu_hien_het() {
        val the = the("ab + 1", "ab")
        val k = PhimKiemTra.ghepThe(the, listOf(the), phanBietHoa = false, hat = 1)
        val cham = { go: String -> HocThuoc.dung(go, the) }
        assertEquals(listOf("a", "b"), k.dienSan(3))
        assertFalse(k.loHet(2, cham))
        assertTrue(k.loHet(3, cham))

        // Khong trung dap an phu: dien san van chua lo, sai lan thu tu moi lo.
        val goc = the("360°", "360")
        val k2 = PhimKiemTra.ghepThe(goc, listOf(goc), phanBietHoa = false, hat = 1)
        val cham2 = { go: String -> HocThuoc.dung(go, goc) }
        assertEquals("36", k2.dienSan(3).joinToString(""))
        assertFalse(k2.loHet(3, cham2))
        assertTrue(k2.loHet(PhimKiemTra.BAC_HIEN_HET, cham2))
    }

    /** Dap an ngan lo som: mot phan thi dien san la ca dap an, mot the chu thi mo het nhieu la lo. */
    @Test
    fun dap_an_ngan_lo_som() {
        val so = the("6")
        val kSo = PhimKiemTra.ghepThe(so, listOf(so), phanBietHoa = false, hat = 1)
        assertFalse(kSo.loHet(2) { HocThuoc.dung(it, so) })
        assertTrue(kSo.loHet(3) { HocThuoc.dung(it, so) })

        val mot = the("cộng")
        val kMot = PhimKiemTra.ghepThe(mot, listOf(mot, the("chia"), the("số mũ")), phanBietHoa = false, hat = 1)
        assertFalse(kMot.loHet(1) { HocThuoc.dung(it, mot) })
        assertTrue(kMot.loHet(2) { HocThuoc.dung(it, mot) })

        val hai = the("số mũ")
        val kHai = PhimKiemTra.ghepThe(hai, listOf(hai, the("cộng")), phanBietHoa = false, hat = 1)
        assertFalse(kHai.loHet(2) { HocThuoc.dung(it, hai) })
        assertFalse(kHai.loHet(3) { HocThuoc.dung(it, hai) })
    }

    /**
     * Hoi nhanh "con the den luot khong" phai ra dung nhu dem [KhoBai.cacTheDenLuot].
     *
     * Hai duong cung mot dinh nghia: the chua dung lan nao la den luot, dung roi thi
     * cho du hen. Lech nhau thi man chinh an dong Kiem tra bai trong khi man chon bo
     * van con the, hoac nguoc lai.
     */
    @Test
    fun hoi_nhanh_the_den_luot_khop_voi_dem_tung_the() {
        val kho = KhoBai.get(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            kho.napBoThe("thu", listOf(the("a")))
            assertTrue(kho.conTheDenLuot("thu"))

            // Vua go dung: hen ba ngay nua moi hoi lai.
            val bayGio = System.currentTimeMillis()
            kho.ghiTraThe(TraThe(theId = "thu:1", go = "a", dung = true, phut = 0, luc = bayGio))
            assertFalse(kho.conTheDenLuot("thu", bayGio))
            assertEquals(0, kho.soTheDenLuot("thu", bayGio))

            // Qua hen thi ca hai duong deu thay the do.
            val quaHen = bayGio + 4 * 24 * 60 * 60_000L
            assertTrue(kho.conTheDenLuot("thu", quaHen))
            assertEquals(1, kho.soTheDenLuot("thu", quaHen))
        } finally {
            kho.napBoThe("thu", emptyList())
            kho.writableDatabase.delete("tra_the", "the_id LIKE ?", arrayOf("thu:%"))
        }
    }

    // --- lop da hoc toi bai nao ---

    /** Bon the trong ba bai, xep y het mot file that: bai theo sach, the lien nhau. */
    private fun boBaBai() = listOf(
        TheHoc(id = "thu:a1", mon = "Toán", bo = "thu", bai = "Bài 1", hoi = "h", dap = "d", thuTu = 0),
        TheHoc(id = "thu:a2", mon = "Toán", bo = "thu", bai = "Bài 1", hoi = "h", dap = "d", thuTu = 1),
        TheHoc(id = "thu:b1", mon = "Toán", bo = "thu", bai = "Bài 2", hoi = "h", dap = "d", thuTu = 2),
        TheHoc(id = "thu:d1", mon = "Toán", bo = "thu", bai = "Bài 4", hoi = "h", dap = "d", thuTu = 3)
    )

    /**
     * Chon da hoc toi Bai 2 thi ca ba duong dem the chi thay Bai 1 va Bai 2.
     *
     * Ba duong la luot hoi, so tren man chon bo, va cau hoi nhanh cua man chinh. Mot
     * duong quen moc la con vao mot bo "con 4 cau" ma chi duoc hoi 3, hoac man chinh
     * hien dong Kiem tra bai ma vao thi khong co cau nao.
     */
    @Test
    fun chon_da_hoc_toi_bai_nao_thi_chi_hoi_toi_het_bai_do() {
        val kho = KhoBai.get(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            kho.napBoThe("thu", boBaBai())
            assertEquals(listOf("Bài 1", "Bài 2", "Bài 4"), kho.cacBaiTrongBoThe("thu"))

            val den = kho.thuTuCuoiCua("thu", "Bài 2")!!
            assertEquals(
                listOf("thu:a1", "thu:a2", "thu:b1"),
                kho.cacTheDenLuot("thu", 99, denThuTu = den).map { it.id }
            )
            assertEquals(3, kho.soTheDenLuot("thu", denThuTu = den))
            assertTrue(kho.conTheDenLuot("thu", denThuTu = den))

            // Lam het phan da hoc thi hoi nhanh cung thay het, du Bai 4 chua lam cau nao.
            val bayGio = System.currentTimeMillis()
            listOf("thu:a1", "thu:a2", "thu:b1").forEach {
                kho.ghiTraThe(TraThe(theId = it, go = "d", dung = true, phut = 0, luc = bayGio))
            }
            assertFalse(kho.conTheDenLuot("thu", bayGio, den))
            assertEquals(0, kho.soTheDenLuot("thu", bayGio, den))
            // Khong co moc thi van la ca bo, nhu truoc khi co cho chon.
            assertTrue(kho.conTheDenLuot("thu", bayGio))
            assertEquals(listOf("thu:d1"), kho.cacTheDenLuot("thu", 99, bayGio).map { it.id })
        } finally {
            kho.napBoThe("thu", emptyList())
            kho.writableDatabase.delete("tra_the", "the_id LIKE ?", arrayOf("thu:%"))
        }
    }

    /**
     * Mon chua chon la khong co moc, de man hinh hoi lai chu khong doan; chon ma khong bai nao
     * cua bo duoc danh dau thi moc rong, khong the nao lot. Tu 2/10/2026 moc la cac bai da danh
     * dau cua mon ([BoThe.mocCua]), danh dau cach quang thi bo the cung cach quang.
     */
    @Test
    fun chua_chon_chua_hoc_va_danh_dau_cach_quang_deu_ra_dung_moc() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val kho = KhoBai.get(context)
        BoThe.napNeuCan(context)
        // Ghi bang MocThu chu khong bang HocToi.datDaHoc: datDaHoc ghi them nhat ky, va dong
        // "Le Hoa chon bai da hoc" se sang Bang dieu khien nhu con chon that.
        val cu = MocThu.luu(context)
        try {
            HocToi.xoa(context, PhanHoc.KHTN)
            assertNull(BoThe.mocCua(context, "khtn8hoa"))

            MocThu.dat(context, PhanHoc.KHTN, emptySet())
            val rong = BoThe.mocCua(context, "khtn8hoa")!!
            assertTrue(rong.rong)
            assertFalse(kho.conTheDenLuot("khtn8hoa", denThuTu = rong.denThuTu, chiBai = rong.chiBai))

            MocThu.dat(context, PhanHoc.KHTN, setOf(3, 15))
            fun so(bo: String) = BoThe.mocCua(context, bo)!!.chiBai!!.mapNotNull { PhanHoc.soBai(it) }.toSet()
            assertEquals(setOf(3), so("khtn8hoa"))
            assertEquals(setOf(15), so("khtn8li"))

            // Tham so denThuTu cua kho van dung cho ai goi: -1 la khong the nao.
            kho.napBoThe("thu", boBaBai())
            assertEquals(0, kho.soTheDenLuot("thu", denThuTu = -1))
            assertFalse(kho.conTheDenLuot("thu", denThuTu = -1))
        } finally {
            MocThu.tra(context, cu)
            kho.napBoThe("thu", emptyList())
        }
    }

    /**
     * Bai trong moi bo that xep dung thu tu sach.
     *
     * "Da hoc toi Bai 9" cat bo the theo thu tu trong file, xem [KhoBai.thuTuCuoiCua]. Ai
     * them Bai 5 vao cuoi file khtn8hoa thay vi chen giua Bai 4 va Bai 6 thi con chon Bai
     * 4 van bi hoi Bai 5, con chon Bai 9 lai khong duoc hoi. Bai khong danh so (bo Toan)
     * thi khong so duoc, bo qua.
     */
    @Test
    fun bai_trong_bo_that_xep_theo_so_bai_tang_dan() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BoThe.napNeuCan(context)
        val kho = KhoBai.get(context)
        BoThe.BO.forEach { bo ->
            val cacBai = kho.cacBaiTrongBoThe(bo.bo)
            assertTrue("${bo.bo} khong co bai nao", cacBai.isNotEmpty())
            val so = cacBai.mapNotNull { Regex("""^Bài (\d+)\.""").find(it)?.groupValues?.get(1)?.toInt() }
            assertEquals("${bo.bo}: $cacBai", so.sorted().distinct(), so)
        }
    }

    // --- so phut ---

    @Test
    fun mot_the_dung_duoc_mot_phut() {
        assertEquals(120, HocThuoc.giayCho(2))
        assertEquals(720, HocThuoc.giayCho(12))
        // Moi the tron mot phut, khong con nua phut de danh nhu truoc 23/9/2026.
        assertEquals(1, HocThuoc.phutThem(0, HocThuoc.giayCho(1)))
        assertEquals(3, HocThuoc.phutThem(0, HocThuoc.giayCho(3)))
        assertEquals(12, HocThuoc.phutThem(0, HocThuoc.giayCho(12)))
    }

    @Test
    fun khong_dung_the_nao_thi_khong_co_phut() {
        assertEquals(0, HocThuoc.giayCho(0))
        assertEquals(0, HocThuoc.giayCho(-1))
        assertEquals(0, HocThuoc.phutThem(0, 0))
    }

    @Test
    fun le_nua_phut_nam_lai_trong_ngay_chu_khong_mat() {
        // Tu 23/9/2026 moi the tron mot phut, phan le chi con tu nhung luot ghi theo
        // gia cu. Ham van phai giu le trong ngay: ba luot 90 giay, chia rieng thi moi
        // luot mot phut, tong ba; tinh don ca ngay la bon phut ruoi, duoc bon.
        var giay = 0
        var tong = 0
        repeat(3) {
            tong += HocThuoc.phutThem(giay, 90)
            giay += 90
        }
        assertEquals(4, tong)
    }

    @Test
    fun tran_ngay_cat_bot_chu_khong_tu_choi_ca_luot() {
        // Hom nay da lam ra 18 phut, tran la 20: luot nay dang duoc 12 phut, chi con 2.
        assertEquals(2, HocThuoc.phutThem(18 * 60, HocThuoc.giayCho(12)))
        // Het tran thi khong con gi.
        assertEquals(0, HocThuoc.phutThem(HocThuoc.GIAY_TRAN_MOI_NGAY, HocThuoc.giayCho(12)))
        // Ghi nham lon hon tran cung khong duoc ra so am.
        assertEquals(0, HocThuoc.phutThem(999 * 60, HocThuoc.giayCho(12)))
    }

    @Test
    fun mot_the_va_mot_tu_vung_cung_gia() {
        assertEquals(LuatTuVung.GIAY_MOI_TU, HocThuoc.GIAY_MOI_THE)
    }

    @Test
    fun gia_tron_phut_vi_man_hinh_ghi_bang_phut() {
        // Cau bao cuoi luot o man hoc thuoc va man do tu ghi gia bang "X phút", lay so
        // giay chia 60. Gia le giay thi hai cau do im lang noi sai.
        assertEquals(0, HocThuoc.GIAY_MOI_THE % 60)
        assertEquals(0, LuatTuVung.GIAY_MOI_TU % 60)
    }
}
