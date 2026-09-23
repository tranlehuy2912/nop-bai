package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Prefs

/**
 * Khong phai test that. Them mot goi vao danh sach app AI de thu tay tren may ao,
 * vi cac app AI that (ChatGPT, NotebookLM) khong cai duoc len may ao.
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualAi \
 *     -e goi 'com.android.chrome' \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 *
 * Them -e bo 1 de bo goi do ra. Thu xong nho bo: goi con nam trong danh sach thi
 * go gi vao app do cung ghi thanh cau hoi AI va gui Telegram.
 */
@RunWith(AndroidJUnit4::class)
class ManualAi {
    @Test
    fun themGoiAi() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val args = InstrumentationRegistry.getArguments()
        val goi = args.getString("goi") ?: error("Thieu -e goi")
        val prefs = Prefs.get(ctx)
        prefs.aiPackages = if (args.getString("bo") != null) prefs.aiPackages - goi
        else prefs.aiPackages + goi
        println("MANUAL_AI: aiPackages = ${prefs.aiPackages}")
    }
}
