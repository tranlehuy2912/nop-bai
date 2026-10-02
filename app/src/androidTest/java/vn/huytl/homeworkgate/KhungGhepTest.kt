package vn.huytl.homeworkgate

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.button.MaterialButton
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.PhimKiemTra
import vn.huytl.homeworkgate.kho.Ghep
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.TraLoi
import vn.huytl.homeworkgate.ui.KhungGhep

/**
 * Thu tu nut cua khung ghep (Ba Huy chot 30/9/2026): giu nguyen trong mot luot, tron lai o luot
 * sau; trac nghiem cung tron va chu A, B, C, D danh lai theo thu tu moi, ma cham van dung.
 *
 * Dung cau trac nghiem SBT Tieng Anh that trong ngan hang: chu cua phuong an khong qua doi so mu
 * hay chi so hoa hoc, so thang chu tren nut duoc. Chi ghi vao so cai o test cuoi, va xoa lai.
 */
@RunWith(AndroidJUnit4::class)
class KhungGhepTest {

    private lateinit var context: Context
    private lateinit var kho: KhoBai
    /** Dong so cai test da ghi: (ma cau, luc), xoa dung dong do, khong dung dong khac cua cau. */
    private val dongDaGhi = mutableListOf<Pair<String, Long>>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        NganHang.napNeuCan(context)
        kho = KhoBai.get(context)
    }

    @After
    fun tearDown() {
        dongDaGhi.forEach { (id, luc) ->
            kho.writableDatabase.delete("tra_loi", "cau_id = ? AND luc = ?", arrayOf(id, luc.toString()))
        }
    }

    /** Cau trac nghiem mot dap an, [soPa] phuong an. */
    private fun cauChon(soPa: Int): List<LamTrenMay.Muc> =
        kho.cacCauCuaNguon("sbtanh8")
            .mapNotNull { LamTrenMay.muc(context, it) }
            .filter { (it.ghep as? Ghep.Chon)?.let { g -> g.cac.size == soPa && g.dap.size == 1 } == true }

    private fun cungKhung(lam: (LinearLayout) -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            lam(LinearLayout(ContextThemeWrapper(context, R.style.Theme_HomeworkGate)))
        }
    }

    /** Chu tren cac nut phuong an, theo thu tu hien. */
    private fun cacNut(muc: LamTrenMay.Muc, hat: Int): List<String> {
        var ra = emptyList<String>()
        cungKhung { khung ->
            KhungGhep(khung, muc, hat).ve()
            ra = (0 until khung.childCount).map { (khung.getChildAt(it) as TextView).text.toString() }
        }
        return ra
    }

    private fun boChuCai(nut: String) = nut.substring(3)

    @Test
    fun trong_mot_luot_ve_lai_bao_nhieu_lan_cung_mot_thu_tu() {
        val cac = cauChon(4).take(10)
        assertTrue(cac.isNotEmpty())
        cac.forEach { m ->
            val hat = KhungGhep.hatLuot(m.copy(soLuot = 2))
            assertEquals(m.cau.ma, cacNut(m, hat), cacNut(m, hat))
        }
        assertEquals(KhungGhep.hatDe("de1", "sbtanh8:1.A2.1"), KhungGhep.hatDe("de1", "sbtanh8:1.A2.1"))
        assertNotEquals(KhungGhep.hatDe("de1", "sbtanh8:1.A2.1"), KhungGhep.hatDe("de2", "sbtanh8:1.A2.1"))
    }

    @Test
    fun luot_sau_tron_khac_va_chu_cai_danh_lai_tu_a() {
        val cac = cauChon(4)
        assertTrue(cac.size >= 20)
        var khac = 0
        cac.forEach { m ->
            val truoc = cacNut(m, KhungGhep.hatLuot(m.copy(soLuot = 0)))
            val sau = cacNut(m, KhungGhep.hatLuot(m.copy(soLuot = 1)))
            // Chu cai theo vi tri nut, khong theo sach.
            listOf(truoc, sau).forEach { ds ->
                ds.forEachIndexed { k, nut -> assertTrue(nut, nut.startsWith("${'A' + k}. ")) }
            }
            // Van du bon phuong an cua sach, chi doi cho.
            assertEquals(truoc.map(::boChuCai).toSet(), sau.map(::boChuCai).toSet())
            if (truoc.map(::boChuCai) != sau.map(::boChuCai)) khac++
        }
        println("KHUNGGHEP: $khac/${cac.size} cau doi thu tu giua hai luot")
        // Bon phuong an thi hai lan tron giong nhau chi 1/24: gan nhu moi cau phai khac.
        assertTrue("chi $khac/${cac.size} cau doi thu tu", khac * 10 >= cac.size * 8)
    }

    @Test
    fun bam_dung_phuong_an_thi_dung_va_ghi_chu_cai_con_da_thay() {
        (cauChon(4).take(8) + cauChon(3).take(3)).forEach { m ->
            val g = m.ghep as Ghep.Chon
            val dung = g.dap.first()
            val chuDung = KhungGhep.boThe(g.cac[dung])
            (0..3).forEach { n ->
                cungKhung { khung ->
                    val k = KhungGhep(khung, m, KhungGhep.hatLuot(m.copy(soLuot = n)))
                    k.ve()
                    val nut = (0 until khung.childCount).map { (khung.getChildAt(it) as TextView).text.toString() }
                    val vt = nut.indexOfFirst { boChuCai(it) == chuDung }
                    assertTrue("${m.cau.ma}: khong thay nut dung trong $nut", vt >= 0)

                    // Bam mot phuong an sai truoc: sai mot cho, va may xoa lua chon.
                    val sai = (0 until khung.childCount).first { it != vt }
                    khung.getChildAt(sai).performClick()
                    assertEquals(1, k.kiem())

                    khung.getChildAt(vt).performClick()
                    assertEquals(0, k.kiem())
                    assertEquals("${'A' + vt}. $chuDung", k.traLoi())
                }
            }
        }
    }

    // ------------------------------------------------------------ mo phim (2/10/2026)

    /** Moi nut trong khung, ke ca nut nam trong cac hang con, theo thu tu ve. */
    private fun cacNut(v: View): List<MaterialButton> = when (v) {
        is MaterialButton -> listOf(v)
        is ViewGroup -> (0 until v.childCount).flatMap { cacNut(v.getChildAt(it)) }
        else -> emptyList()
    }

    private fun nut(khung: LinearLayout, chu: String): MaterialButton =
        cacNut(khung).first { it.text.toString() == chu && it.isEnabled }

    @Test
    fun sai_thi_phim_nhieu_mo_dan_ma_khong_bien_mat() {
        val g = Ghep.Cau(
            sao = 2, hoi = "", the = listOf("I", "like", "it"),
            nhieu = listOf("likes", "liking", "to", "an"), dap = listOf("I like it")
        )
        cungKhung { khung ->
            val k = KhungGhep(khung, g, "Tiếng Anh", hatTron = 5)
            val moTheoMuc = (0..2).map { muc ->
                k.ve(muc)
                val nut = cacNut(khung)
                // Du bay the o moi muc: the mo van nam nguyen cho.
                assertEquals(7, nut.size)
                nut.filter { !it.isEnabled }.map { it.text.toString() }
            }
            assertEquals(listOf(0, 2, 4), moTheoMuc.map { it.size })
            // Mo them chu khong mo lai tu dau, va chi mo the nhieu.
            assertTrue(moTheoMuc[2].containsAll(moTheoMuc[1]))
            assertTrue(moTheoMuc[2].all { it in g.nhieu })
        }
    }

    @Test
    fun trac_nghiem_sai_thi_phuong_an_vua_chon_mo_ca_khi_ve_lai_tu_dau() {
        val m = cauChon(4).first()
        val g = m.ghep as Ghep.Chon
        val hat = KhungGhep.hatLuot(m)
        var saiDaChon = emptyList<String>()
        var chuSai = ""
        cungKhung { khung ->
            val k = KhungGhep(khung, m, hat)
            k.ve()
            val dung = KhungGhep.boThe(g.cac[g.dap.first()])
            val nutSai = cacNut(khung).first { boChuCai(it.text.toString()) != dung }
            chuSai = nutSai.text.toString()
            nutSai.performClick()
            assertEquals(1, k.kiem())
            saiDaChon = k.saiDaChon()
            assertEquals(1, saiDaChon.size)
            k.ve(1)
            val mo = cacNut(khung).filter { !it.isEnabled }.map { it.text.toString() }
            assertEquals(listOf(chuSai), mo)
        }
        // Tat man roi mo lai: khung moi, cung hat, doc lai phuong an da chon sai tu luot.
        cungKhung { khung ->
            KhungGhep(khung, m, hat, saiDaChon).ve(1)
            assertEquals(listOf(chuSai), cacNut(khung).filter { !it.isEnabled }.map { it.text.toString() })
        }
    }

    @Test
    fun dien_san_khoa_lai_va_tinh_vao_cau_tra_loi() {
        val g = Ghep.BieuThuc(
            sao = 1, hoi = "", dap = listOf("M = m/n"), phim = listOf("M", "m", "n"),
            nhieu = listOf("V", "D"), loiGiai = emptyList()
        )
        cungKhung { khung ->
            val k = KhungGhep(khung, g, "Khoa học tự nhiên", hatTron = 3, dienSan = listOf("M", "=", "m"))
            k.ve()
            assertTrue(k.ghepXong())
            assertEquals("M=m", k.traLoi())
            // Xoa khong cham toi phan dien san.
            nut(khung, "Xoá").performClick()
            assertEquals("M=m", k.traLoi())
            nut(khung, "/").performClick()
            nut(khung, "n").performClick()
            assertEquals("M=m/n", k.traLoi())
            nut(khung, "Xoá").performClick()
            assertEquals("M=m/", k.traLoi())
        }
    }

    @Test
    fun tu_co_khuon_chi_go_chu_cai_phan_trong_ngoac_in_san() {
        val g = PhimKiemTra.ghepTu("log (on to)").ghep
        cungKhung { khung ->
            val k = KhungGhep(khung, g, "Tiếng Anh", hatTron = 1)
            k.ve()
            // Du 26 phim chu cai cong nut Xoa.
            assertEquals(27, cacNut(khung).size)
            assertFalse(k.ghepXong())
            listOf("l", "o", "g").forEach { nut(khung, it).performClick() }
            assertTrue(k.ghepXong())
            assertEquals("log (on to)", k.traLoi())
            // O da day thi bam them khong vao.
            nut(khung, "x").performClick()
            assertEquals("log (on to)", k.traLoi())
        }
    }

    @Test
    fun xong_mot_luot_thi_so_luot_tang_va_luot_sau_tron_khac() {
        val m = cauChon(4).first()
        val truoc = LamTrenMay.muc(context, m.cau)!!
        val luc = System.currentTimeMillis()
        dongDaGhi += m.cau.id to luc
        // Ghi thang mot dong luot tren may nhu LamTrenMay.ghi, khong cong phut, khong day Firestore.
        kho.ghiTraLoi(
            TraLoi(
                cauId = m.cau.id, mon = m.cau.mon, ma = m.cau.ma, de = m.cau.de, ketQua = "x",
                dung = true, phut = 0, nhanXet = "", luc = luc,
                trenMay = true, sao = 1, saoToiDa = 1, vong = 0
            )
        )
        val sau = LamTrenMay.muc(context, m.cau)!!
        assertEquals(truoc.soLuot + 1, sau.soLuot)
        assertNotEquals(KhungGhep.hatLuot(truoc), KhungGhep.hatLuot(sau))
    }
}
