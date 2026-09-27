package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.dongbo.DongBo
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh

/**
 * Lenh CAIDAT tu Bang dieu khien cho hai muc dien thoai moi sua duoc tu 27/9/2026:
 * app duoc nghe nen va gio rieng tung app. Kem ban sao cau hinh day sang dien thoai.
 *
 * Giu lai hai muc nay tren may roi tra ve nguyen ven sau khi chay, nhu [TinCoLenhTest]:
 * may ao dung chung co the dang giu cai dat thu.
 */
@RunWith(AndroidJUnit4::class)
class CaiDatLenhTest {

    private lateinit var context: Context
    private var nhacCu: Set<String> = emptySet()
    private var hanCu: Map<String, Int> = emptyMap()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        nhacCu = Prefs.get(context).nhacPackages
        hanCu = GioiHanApp.tatCa(context)
    }

    @After
    fun tearDown() {
        Prefs.get(context).nhacPackages = nhacCu
        GioiHanApp.tatCa(context).keys.forEach { GioiHanApp.datHan(context, it, 0) }
        hanCu.forEach { (goi, phut) -> GioiHanApp.datHan(context, goi, phut) }
    }

    @Test
    fun lenh_app_nhac_thay_ca_danh_sach() {
        Prefs.get(context).nhacPackages = setOf("com.app.cu")

        val tra = ThiHanhLenh.doiCaiDat(context, "appNhac", listOf(SPOTIFY, ZING))

        assertEquals("Danh sách app được nghe nền: 2 app.", tra)
        assertEquals(setOf(SPOTIFY, ZING), Prefs.get(context).nhacPackages)
    }

    /** Bang dieu khien gui moi lan mot app. Firestore tra so phut ve dang Long. */
    @Test
    fun lenh_gio_rieng_chi_doi_app_duoc_gui() {
        GioiHanApp.datHan(context, YOUTUBE, 30)

        val tra = ThiHanhLenh.doiCaiDat(context, "gioiHanApp", mapOf(NETFLIX to 90L))

        assertEquals("Đã đổi giới hạn cho 1 app.", tra)
        assertEquals(90, GioiHanApp.han(context, NETFLIX))
        assertEquals(30, GioiHanApp.han(context, YOUTUBE))
    }

    @Test
    fun gui_khong_phut_la_bo_gio_rieng() {
        GioiHanApp.datHan(context, YOUTUBE, 30)
        GioiHanApp.datHan(context, NETFLIX, 90)

        ThiHanhLenh.doiCaiDat(context, "gioiHanApp", mapOf(YOUTUBE to 0L))

        assertEquals(mapOf(NETFLIX to 90), GioiHanApp.tatCa(context))
    }

    /** Dien thoai doc hai truong nay de hien so app va so phut cua tung app. */
    @Test
    fun ban_sao_cau_hinh_co_app_nhac_va_gio_rieng() {
        Prefs.get(context).nhacPackages = setOf(ZING, SPOTIFY)
        GioiHanApp.tatCa(context).keys.forEach { GioiHanApp.datHan(context, it, 0) }
        GioiHanApp.datHan(context, NETFLIX, 60)

        val ban = DongBo.banCaiDat(context)

        assertEquals(listOf(SPOTIFY, ZING), ban["appNhac"])
        assertEquals(mapOf(NETFLIX to 60), ban["gioiHanApp"])
    }

    private companion object {
        const val SPOTIFY = "com.spotify.music"
        const val ZING = "com.zing.mp3"
        const val NETFLIX = "com.netflix.mediaclient"
        const val YOUTUBE = "com.google.android.youtube"
    }
}
