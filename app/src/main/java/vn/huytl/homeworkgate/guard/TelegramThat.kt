package vn.huytl.homeworkgate.guard

import android.content.Context
import android.content.Intent
import android.net.Uri
import vn.huytl.homeworkgate.data.Prefs

/**
 * App Telegram that tren tablet, dang nhap tai khoan rieng cua Le Hoa. Khac con bot
 * trong [vn.huytl.homeworkgate.telegram.TelegramClient]: bot la duong cua app nay, con
 * day la app ma con cam len nhan tin.
 *
 * Tu 27/9/2026 nut "Nhan cho ba Huy" mo thang khung chat voi Ba Huy o day, thay cho man
 * chat trong app. Telegram co mo duoc luc het gio choi hay khong la viec cua cac danh
 * sach app Ba Huy dat: bo Telegram vao "Dung moi luc" thi mo duoc ca gio ngu va gio di
 * hoc, xem [vn.huytl.homeworkgate.data.Prefs.moiLucPackages].
 */
object TelegramThat {

    /** Cac ban Telegram chinh thuc: ban Google Play, ban tai tu telegram.org, ban beta. */
    val GOI = setOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.messenger.beta"
    )

    /**
     * Mo Telegram ngay khung chat voi Ba Huy. Tra ve false neu khong mo duoc: may chua
     * cai Telegram, hay chat id khong phai cua mot nguoi.
     *
     * Duong link `tg://openmessage?user_id=` la cua chinh Telegram Android: LaunchActivity
     * doc no roi mo thang khung chat voi nguoi do. Chat id cai luc dat bot la id Telegram
     * cua Ba Huy (lay tu `message.from.id`), nen khong phai nhap them gi. Chat id am la
     * cua mot nhom, link theo nguoi dung khong mo duoc.
     */
    fun moChatVoiBa(context: Context): Boolean {
        val chat = Prefs.get(context).parentChatId
        if (chat <= 0L) return false
        val yDinh = Intent(Intent.ACTION_VIEW, Uri.parse("tg://openmessage?user_id=$chat"))
        val goi = runCatching {
            context.packageManager.queryIntentActivities(yDinh, 0)
                .map { it.activityInfo.packageName }
                .firstOrNull { it in GOI }
        }.getOrNull() ?: return false
        return runCatching {
            context.startActivity(yDinh.setPackage(goi).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }
}

/**
 * So tin Ba Huy gui qua Telegram ma Le Hoa chua doc, dem theo thong bao cua Telegram.
 *
 * Tin toi luc sang, chieu con mo may thi nut o man chinh van phai noi "Ba Huy nhan 3
 * tin moi", nhu thoi man chat cu. App nay khong doc duoc ben trong Telegram, nen dem
 * theo thong bao: [TaiThongBao] ghi so khi thong bao cua khung chat voi ba hien hay
 * doi, va ghi 0 khi no mat.
 *
 * Ghi vao prefs chu khong giu trong bo nho: man chinh ve lai moi khi prefs doi, nen tin
 * toi luc con dang nhin man hinh do thi nut doi ngay. Khoa nay nam trong danh sach bo
 * qua cua [vn.huytl.homeworkgate.dongbo.DongBo], khong keo theo luot day nao.
 */
object TinCuaBa {

    const val K_SO = "tg_tin_ba_chua_doc"

    fun so(context: Context): Int = Prefs.get(context).raw().getInt(K_SO, 0)

    /** Ghi so moi. Giong so cu thi thoi, khong danh thuc ai. */
    fun dat(context: Context, so: Int) {
        val sp = Prefs.get(context).raw()
        if (sp.getInt(K_SO, 0) == so) return
        sp.edit().putInt(K_SO, so.coerceAtLeast(0)).apply()
    }
}
