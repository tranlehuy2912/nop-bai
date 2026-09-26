package vn.huytl.homeworkgate

import android.app.Activity
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.ui.CaptureActivity
import vn.huytl.homeworkgate.ui.DanDoActivity
import vn.huytl.homeworkgate.ui.SoatBaiActivity
import java.time.LocalDate

/**
 * Man vo dan do va man soat bai khi tablet xoay, hay khi Android dung lai man. Cac man
 * khac thi [moiManTuVeLaiKhiXoay] kiem la deu khai configChanges.
 *
 * Tablet cua Le Hoa bat tu xoay (Ba Huy xac nhan ngay 26/9/2026), con man chup thi khoa
 * doc. Truoc day xoay may la hai man nay bi dung lai tu dau: man dan do mat tam anh
 * vua chup va ban may vua doc, chua co ban luu thi camera bat lai ngay; man soat bai
 * xoa anh bai lam dang cho gui.
 *
 * XOAY THAT tren may ao qua UiAutomation, xong thi tra ve dung che do xoay cu. Khong
 * test nao goi Gemini: man dan do co san ban luu nen khong mo camera, man soat bai
 * khong kem anh nen may cham tra ve ngay - xem [xoayMayThiManSoatBaiGiuNguyen]. Ban vo
 * dan do dang co trong may duoc tra lai sau moi test.
 */
@RunWith(AndroidJUnit4::class)
class XoayManTest {

    private val ins = InstrumentationRegistry.getInstrumentation()
    private val context = ins.targetContext

    private val ban = VoDanDo.DanDo(
        ngay = LocalDate.now().toString(),
        cacDong = listOf(
            VoDanDo.Dong("Toán: làm bài 2.28 trang 47", laBaiTap = true),
            VoDanDo.Dong("KHTN: mang sách vở đầy đủ", laBaiTap = false),
            VoDanDo.Dong("Ngữ văn: học thuộc bài thơ", laBaiTap = false)
        )
    )

    private var banCu: VoDanDo.DanDo? = null

    @Before
    fun napBan() {
        banCu = VoDanDo.doc(context)
        VoDanDo.luu(context, ban)
    }

    // Tra lai ban co truoc test, ca tam anh cua no. VoDanDo.xoa chi chay khi truoc do
    // khong co ban nao, luc do ban thu khong co anh nen khong xoa file nao.
    @After
    fun traBan() {
        val cu = banCu
        if (cu != null) VoDanDo.luu(context, cu) else VoDanDo.xoa(context)
    }

    @Test
    fun xoayMayThiManDanDoGiuNguyenChoConDangSua() {
        ActivityScenario.launch(DanDoActivity::class.java).use { sc ->
            lateinit var truoc: Activity
            sc.onActivity {
                truoc = it
                oChu(it, 1).setText("KHTN: làm bài 3 trang 20")
            }
            xoayQuaLai(sc)
            sc.onActivity {
                assertSame("xoay may thi khong duoc dung lai man", truoc, it)
                assertEquals(
                    listOf(
                        "Toán: làm bài 2.28 trang 47" to true,
                        "KHTN: làm bài 3 trang 20" to false,
                        "Ngữ văn: học thuộc bài thơ" to false
                    ),
                    cacDong(it)
                )
            }
        }
    }

    /**
     * Man soat bai mo khong kem anh, nen may cham tra ve ngay ma khong goi Gemini. Ban
     * vo dan do luc nay phai la ban co chu cua [napBan]: ban chi co anh thi man soat gan
     * tam anh do vao lan cham, va lan cham do goi Gemini that.
     */
    @Test
    fun xoayMayThiManSoatBaiGiuNguyen() {
        check(VoDanDo.conHieuLuc(context)?.chuaDoc != true) { "vo chi co anh se goi Gemini" }
        val moMan = Intent(context, SoatBaiActivity::class.java)
        ActivityScenario.launch<SoatBaiActivity>(moMan).use { sc ->
            lateinit var truoc: Activity
            sc.onActivity { truoc = it }
            xoayQuaLai(sc)
            sc.onActivity { assertSame("xoay may thi khong duoc dung lai man", truoc, it) }
        }
    }

    /**
     * Android van dung lai man o nhung luc khac xoay may: cam ban phim roi, doi co chu,
     * hay thu hoi bo nho luc app nam nen. Moi dong dan do mang chung id o_chu va o_tich,
     * nen de Android tu luu thi no chep dong cuoi de len moi dong.
     */
    @Test
    fun dungLaiManDanDoKhongChepDongCuoiLenMoiDong() {
        ActivityScenario.launch(DanDoActivity::class.java).use { sc ->
            sc.recreate()
            sc.onActivity {
                assertEquals(ban.cacDong.map { d -> d.chu to d.laBaiTap }, cacDong(it))
            }
        }
    }

    /**
     * Con bam Chup lai o man soat roi huy camera: man soat phai nguyen nhu luc roi di,
     * ke ca dong con vua sua tay ma chua bam Luu. Ba Huy chon cach nay ngay 26/9/2026.
     *
     * Man chup that bi chan lai, tra ve "huy" nhu luc con bam Back o camera.
     */
    @Test
    fun huyCameraTuManSoatThiGiuNguyenMan() {
        val chan = ins.addMonitor(
            CaptureActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null),
            true
        )
        try {
            ActivityScenario.launch(DanDoActivity::class.java).use { sc ->
                sc.onActivity {
                    oChu(it, 1).setText("KHTN: làm bài 3 trang 20")
                    it.findViewById<View>(R.id.nut_chup_lai).performClick()
                }
                ins.waitForIdleSync()
                assertEquals("phai mo man chup dung mot lan", 1, chan.hits)
                sc.onActivity {
                    assertEquals(View.VISIBLE, it.findViewById<View>(R.id.box_soat).visibility)
                    assertEquals("KHTN: làm bài 3 trang 20", oChu(it, 1).text.toString())
                }
            }
        } finally {
            ins.removeMonitor(chan)
        }
    }

    /**
     * Moi man cua app tu ve lai khi xoay, tru man chup vi man do khoa doc.
     *
     * Hai test xoay that o tren cho thay configChanges giu nguyen man tren may nay. Test
     * nay giu cho man nao cung co no, ke ca man them sau: quen khai la luot hoc thuoc
     * bi chot giua chung, hay tin chat gui hai lan - xem chu thich dau AndroidManifest.
     */
    @Test
    fun moiManTuVeLaiKhiXoay() {
        val canCo = ActivityInfo.CONFIG_ORIENTATION or ActivityInfo.CONFIG_SCREEN_SIZE or
            ActivityInfo.CONFIG_SCREEN_LAYOUT or ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE
        @Suppress("DEPRECATION")
        val cacMan = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            .activities.orEmpty()
            // Bo man cua thu vien (Firebase, Google Play) gop vao manifest.
            .filter { it.name.startsWith("vn.huytl.homeworkgate.") }
        val sai = cacMan.filter {
            if (it.name == CaptureActivity::class.java.name) {
                it.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                it.configChanges and canCo != canCo
            }
        }.map { it.name.substringAfterLast('.') }
        assertTrue("chua tu ve lai khi xoay: $sai", sai.isEmpty())
        assertTrue("khong doc duoc danh sach man", cacMan.size >= 18)
    }

    // ------------------------------------------------------------------ linh tinh

    private fun cacDong(a: Activity): List<Pair<String, Boolean>> {
        val ds = a.findViewById<LinearLayout>(R.id.danh_sach_dong)
        return (0 until ds.childCount).map { i ->
            val dong = ds.getChildAt(i)
            dong.findViewById<EditText>(R.id.o_chu).text.toString() to
                dong.findViewById<CheckBox>(R.id.o_tich).isChecked
        }
    }

    private fun oChu(a: Activity, i: Int): EditText =
        a.findViewById<LinearLayout>(R.id.danh_sach_dong).getChildAt(i)
            .findViewById(R.id.o_chu)

    /**
     * Xoay may ao sang chieu kia roi xoay ve, cho man nhan du hai lan doi chieu.
     *
     * Tra ve dung che do cu: khoa lai o goc cu truoc, roi moi tha cho tu xoay neu luc
     * dau dang tu xoay, de goc luu trong cai dat cung ve nhu cu.
     */
    private fun <A : Activity> xoayQuaLai(sc: ActivityScenario<A>) {
        val ui = ins.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val cr = context.contentResolver
        val tuXoay = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
        val gocCu = Settings.System.getInt(cr, Settings.System.USER_ROTATION, 0)
        var gocNay = 0
        sc.onActivity {
            @Suppress("DEPRECATION")
            gocNay = it.windowManager.defaultDisplay.rotation
        }
        val chieuDau = chieu(sc)
        try {
            // ROTATION_FREEZE_0..270 trung voi Surface.ROTATION_0..270.
            ui.setRotation(
                if (gocNay % 2 == 0) UiAutomation.ROTATION_FREEZE_90
                else UiAutomation.ROTATION_FREEZE_0
            )
            choDen(sc) { it != chieuDau }
        } finally {
            ui.setRotation(gocCu)
            if (tuXoay == 1) ui.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
        choDen(sc) { it == chieuDau }
    }

    private fun <A : Activity> chieu(sc: ActivityScenario<A>): Int {
        var c = 0
        sc.onActivity { c = it.resources.configuration.orientation }
        return c
    }

    private fun <A : Activity> choDen(sc: ActivityScenario<A>, dat: (Int) -> Boolean) {
        val het = System.currentTimeMillis() + 10_000
        while (true) {
            // Man bi dung lai thi giua chung onActivity co the nem loi: cho mot nhip.
            val c = runCatching { chieu(sc) }.getOrNull()
            if (c != null && dat(c)) return
            check(System.currentTimeMillis() < het) { "may ao khong xoay" }
            Thread.sleep(100)
        }
    }
}
