package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.ui.GiongDoc

/**
 * Chu dua cho bo doc o man Kiem tra tu vung (8/10/2026). Bo doc gap ngoac hay gach cheo
 * thi doc ca dau ra, nen moi tu cua bo phai qua [GiongDoc.chuDoc] ma khong con dau nao.
 *
 * Khong xoa prefs, khong dung toi bo doc that: chi so chu.
 */
@RunWith(AndroidJUnit4::class)
class GiongDocTest {

    @Test
    fun bo_ngoac_va_tach_hai_nghia() {
        assertEquals("balance", GiongDoc.chuDoc("balance"))
        assertEquals("zoom in, zoom out", GiongDoc.chuDoc("zoom (in / out)"))
        assertEquals("log on to", GiongDoc.chuDoc("log (on to)"))
        assertEquals("DIY, do-it-yourself", GiongDoc.chuDoc("DIY (do-it-yourself)"))
        assertEquals("farmers' market", GiongDoc.chuDoc(" farmers' market "))
    }

    @Test
    fun moi_tu_trong_bo_deu_doc_duoc() {
        val ct = InstrumentationRegistry.getInstrumentation().targetContext
        val goc = ct.assets.open("tuvung/anh8.json").bufferedReader().use { it.readText() }
        val cac = JSONObject(goc).getJSONArray("cac_tu")
        assertTrue("bo anh8 phai co tu", cac.length() > 0)
        for (i in 0 until cac.length()) {
            val tu = cac.getJSONObject(i).getString("tu")
            val chu = GiongDoc.chuDoc(tu)
            assertFalse("tu \"$tu\" doc ra rong", chu.isBlank())
            assertFalse("tu \"$tu\" con dau la: \"$chu\"", chu.any { it in "()/[]" })
        }
    }
}
