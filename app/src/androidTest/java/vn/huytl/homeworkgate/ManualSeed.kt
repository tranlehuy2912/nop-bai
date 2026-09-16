package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Prefs

/**
 * Khong phai test that. Day la cach nap san cau hinh vao may ao de thu tay,
 * vi dien form qua adb input rat de lac phim.
 *
 * CAN THAN: lop nay chay trong tien trinh cua bai test, khong phai tien trinh app.
 * EncryptedSharedPreferences khong an toan khi hai tien trinh cung ghi, va moi lan
 * ghi la ghi lai ca ban do trong bo nho, nen ben nay ghi de len ben kia. Trieu
 * chung: nap cau hinh xong ma app van bao "chua cai dat", hoac cac so ve mac dinh.
 * Cach tranh: sau khi nap thi "adb shell am force-stop" roi mo lai app, de app doc
 * lai tu dia. App that chi chay mot tien trinh nen khong dinh chuyen nay.
 *
 * Token va chat id truyen tu dong lenh de khong nam lai trong ma nguon:
 *   adb shell am instrument -w \
 *     -e class vn.huytl.homeworkgate.ManualSeed \
 *     -e token '<token>' -e chatid '<id>' \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ManualSeed {

    @Test
    fun napCauHinhDeThuTay() {
        val args = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = Prefs.get(context)

        // Khong co -e token thi GIU NGUYEN token dang co. Truoc day cho nay ghi de
        // bang token truyen vao, ma tools/emu.sh lai truyen token that tu bot.env:
        // may ao nghe chung mot bot voi tablet cua Le Hoa, hai ben giat tin cua nhau
        // (Telegram tra ve 409). Ban go loi da co san token rieng trong Defaults.
        args.getString("token")?.takeIf { it.isNotBlank() }?.let { prefs.botToken = it }
        prefs.parentChatId = args.getString("chatid")?.toLongOrNull() ?: 999_000_111L
        // PIN truyen tu dong lenh, khong thi giu nguyen cai dang co; chi khi
        // chua co PIN nao moi dat tam 1234.
        val pin = args.getString("pin")
        when {
            pin != null -> prefs.setPin(pin)
            !prefs.hasPin() -> prefs.setPin("1234")
        }
        prefs.grantMinutes = args.getString("minutes")?.toIntOrNull() ?: 60
        // Gio chot dat duoc tu dong lenh de thu canh "het gio" ma khong phai
        // ngoi cho ca tieng: -e giochot 11:45
        val chot = args.getString("giochot")?.split(":")
        prefs.hardStopMinuteOfDay = if (chot?.size == 2) {
            (chot[0].toIntOrNull() ?: 23) * 60 + (chot[1].toIntOrNull() ?: 30)
        } else {
            23 * 60 + 30
        }
        prefs.tranPhutMoiNgay = 135
        // Nap lai chum khoa AI mac dinh va xoa danh dau "dang nghi". Bo KhoaAiTest
        // thay khoa that bang khoa gia ("khoa-mot"...), chay xong ma khong nap lai
        // thi phan cham bai an 400 ba lan roi bao "ca chum dang nghi 23 tieng".
        prefs.aiKeys = vn.huytl.homeworkgate.data.Defaults.AI_KEYS
        vn.huytl.homeworkgate.data.KhoaAi.xoaDanhDau(context)
        prefs.lockSystemSettings = true

        // -e cam com.android.chrome de thu danh sach den
        args.getString("cam")?.let { prefs.blockedPackages = it.split(",").toSet() }

        // Xoa so luot da duyet trong ngay. Khong co dong nay thi thu di thu lai
        // vai lan la het luot, va nut "Mo gio choi" im lang khong cap gi ca.
        prefs.raw().edit()
            .remove("day_key").remove("day_count")
            .remove("bonus_day").remove("bonus_count")
            .commit()
        // Danh sach trang de rong: het gio thi chan tat ca tru app he thong.
        prefs.allowedPackages = emptySet()
    }
}
