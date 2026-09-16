package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Prefs

/**
 * Khong phai test that. Doi ma PIN tren may ao de thu tay:
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualPin \
 *     -e pin 000000 vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ManualPin {

    @Test
    fun datPin() {
        val args = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = Prefs.get(context)
        val pin = args.getString("pin") ?: return
        prefs.setPin(pin)
        check(prefs.checkPin(pin)) { "dat PIN xong nhung kiem lai khong khop" }
    }
}
