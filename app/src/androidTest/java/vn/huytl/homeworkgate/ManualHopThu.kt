package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.HopThuBaNoi

/**
 * Khong phai test that. Day la cach chi cho may ao biet hop thu nam o dau, de thu
 * tay ca duong tu may ba noi ma khong phai lap nhom Telegram.
 *
 * Tren may that thi khong dung cai nay: go /nhom ngay trong nhom la xong.
 *
 * CAN THAN: giong ManualSeed, lop nay chay trong tien trinh bai test chu khong
 * phai tien trinh app. Nap xong phai "adb shell am force-stop" roi mo lai app,
 * khong thi app van doc ban cu trong bo nho.
 *
 *   adb shell am instrument -w \
 *     -e class vn.huytl.homeworkgate.ManualHopThu \
 *     -e nhom '<chat id>' \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ManualHopThu {

    @Test
    fun noiHopThuDeThuTay() {
        val args = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = args.getString("nhom")?.toLongOrNull()
            ?: error("Thieu -e nhom '<chat id>'")

        HopThuBaNoi.datNhom(context, id)
        // Xoa dau "da doc" va dau "hom nay da cho" de thu duoc nhieu lan lien tiep,
        // khong phai doi sang hom sau.
        Prefs.raw(context).edit().remove("hopthu_da_doc").remove("hopthu_ngay_da_cho").commit()

        println("Da noi hop thu voi $id")
    }

    /** Prefs.raw() la internal, muon vao tu day phai di vong mot chut. */
    private object Prefs {
        fun raw(context: android.content.Context) =
            vn.huytl.homeworkgate.data.Prefs.get(context).raw()
    }
}
