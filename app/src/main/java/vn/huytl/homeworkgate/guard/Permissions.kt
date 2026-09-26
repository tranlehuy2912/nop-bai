package vn.huytl.homeworkgate.guard

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import vn.huytl.homeworkgate.App
import vn.huytl.homeworkgate.data.Prefs

/** Mot viec chua lam xong, de man hinh nao cung bao giong nhau. */
enum class Viec {
    TELEGRAM,
    TRO_NANG,
    TRO_NANG_CHET,
    LOP_PHU,
    TIET_KIEM_PIN,
    TU_KHOI_DONG,
    QUAN_TRI,
    THONG_BAO,
    DOC_THONG_BAO,
}

/**
 * @param ten    mot dong ngan, du de biet thieu cai gi
 * @param cach   hong cai gi neu khong lam, hoac phai lam gi
 * @param nang   true la may dang khong chan duoc gi; false la van chan nhung hong
 *               mot phan, vi du khong co dong ho dem nguoc
 */
data class Thieu(
    val viec: Viec,
    val ten: String,
    val cach: String,
    val nang: Boolean,
)

/**
 * Nhung thu app can ma phai tu tay bat trong Settings, khong xin bang hop thoai
 * duoc. Lop nay chi tra loi "da bat chua" va mo dung man hinh can den.
 *
 * Truoc day chi kiem ba quyen he thong. Nhung tren HyperOS, may hong pho bien
 * nhat khong phai vi thieu quyen ma vi tiet kiem pin giet mat tien trinh, hoac vi
 * tu khoi dong bi tat nen khoi dong lai la app khong len. Hai cai do khong hien o
 * dau ca, den luc phat hien ra thi tablet da mo toang may ngay. Gio bao het mot
 * the, ke ca cai chi hong mot phan.
 */
object Permissions {

    /** Ve ung dung khac. Thieu cai nay thi lop phu khong hien duoc. */
    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun overlayIntent(context: Context) = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )

    /**
     * Tro nang. Thieu cai nay thi khong chan duoc app nao ca.
     *
     * Hoi ca cong tac tong lan danh sach dich vu. Cai ban app moi de len ban cu la
     * Android tat cong tac tong, nhung ten dich vu van con nguyen trong danh sach:
     * doc moi danh sach thi tuong van dang bat, trong khi thuc te khong con ai chan
     * app nao. Do la cach tablet ho ma khong ai biet, ngay sau moi lan cap nhat.
     */
    fun hasAccessibility(context: Context): Boolean {
        val batTong = Settings.Secure.getInt(
            context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        if (batTong == 0) return false

        val expected = ComponentName(context, GuardAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        for (item in splitter) {
            if (item.equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    fun accessibilityIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    /** Quan tri thiet bi. Bat len thi Android tu choi go app. */
    fun hasDeviceAdmin(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm.isAdminActive(adminComponent(context))
    }

    fun adminComponent(context: Context) = ComponentName(context, AdminReceiver::class.java)

    fun deviceAdminIntent(context: Context) = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(context))
        .putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Bật để tablet không bị gỡ app quản lý giờ chơi, và để ba Huy nhận tin " +
                "khi có người định tắt nó."
        )

    /**
     * App co nam ngoai danh sach tiet kiem pin khong.
     *
     * Nam trong danh sach thi he thong duoc phep dung tien trinh luc man hinh tat,
     * ma dung tien trinh la dich vu Telegram chet: bai nop len khong ai duyet, va
     * dong ho dem nguoc dung giua chung.
     */
    fun hasBatteryFree(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)

    /** Thong bao bi tat thi khong con dong ho dem nguoc, tin cua co giao cung khong bao. */
    fun hasNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Quyen doc thong bao. Can cho hai viec: canh tieng phat nen, va dem tin Telegram cua
     * Ba Huy.
     *
     * Android chi cho hoi "app nao dang giu trinh phat" khi co mot dich vu kieu do da
     * duoc bat, va do la cach duy nhat dem duoc so phut nghe nhac cua dung mot app.
     * [TaiThongBao] khong doc noi dung thong bao nao. Tu 27/9/2026 no nhin them ma khung
     * chat va so tin cua thong bao Telegram, de dem so tin Ba Huy gui ma con chua doc.
     */
    fun hasNotificationAccess(context: Context): Boolean = TrinhPhat.coQuyen(context)

    /**
     * May Xiaomi co them cong tac "Tu khoi dong" rieng, tat thi khoi dong lai la
     * app khong chay lai. Khong co cach nao doc duoc cong tac do tu trong app, nen
     * chi nhac va de Ba Huy tu danh dau da bat.
     */
    fun laMayXiaomi(): Boolean = Build.MANUFACTURER.lowercase().let {
        it.contains("xiaomi") || it.contains("redmi") || it.contains("poco")
    }

    /**
     * Tra ve danh sach viec con thieu, viec nang truoc.
     *
     * Khong chi hoi cai dat he thong. Truoc day ham nay chi doc cai dat, nen khi
     * HyperOS giet tien trinh ma khong cho dich vu bind lai thi cong tac van bat,
     * danh sach nay van rong, va man hinh bao "may da san sang" trong khi khong con
     * ai chan app nao. Gio no hoi ca dich vu xem co dang chay that khong.
     */
    fun missing(context: Context): List<Thieu> = buildList {
        val prefs = Prefs.get(context)

        if (!prefs.isConfigured) {
            add(
                Thieu(
                    Viec.TELEGRAM,
                    "Chưa cài đặt xong Telegram và mã PIN",
                    "Thiếu thì bài nộp không gửi đi đâu được.",
                    nang = true
                )
            )
        }

        when (prefs.loiTelegram) {
            401 -> add(
                Thieu(
                    Viec.TELEGRAM,
                    "Bot token sai — Telegram không nhận lệnh nào",
                    "Gõ lệnh trong Telegram sẽ không có gì xảy ra. Vào Cài đặt " +
                        "dán lại token của bot.",
                    nang = true
                )
            )
            409 -> add(
                Thieu(
                    Viec.TELEGRAM,
                    "Có máy khác cũng đang dùng bot này",
                    "Hai máy giành nhau, lệnh rơi vào máy nào không biết trước. " +
                        "Gỡ app ở máy kia, hoặc cho máy kia một bot riêng.",
                    nang = true
                )
            )
        }

        if (!hasAccessibility(context)) {
            add(
                Thieu(
                    Viec.TRO_NANG,
                    "Chưa bật dịch vụ trợ năng cho app Nộp bài",
                    "Đây là thứ chặn app, thiếu nó thì máy mở toang. " +
                        "Cài bản mới xong là Android tắt nó, phải vào bật lại.",
                    nang = true
                )
            )
        } else if (!vuaKhoiDong()) {
            if (!GuardAccessibilityService.dangChay) {
                add(
                    Thieu(
                        Viec.TRO_NANG_CHET,
                        "Trợ năng bật nhưng dịch vụ canh app không chạy",
                        "Máy đang không chặn gì. Vào Trợ năng → Nộp bài, " +
                            "tắt rồi bật lại.",
                        nang = true
                    )
                )
            } else if (GuardAccessibilityService.soMienTru == 0) {
                add(
                    Thieu(
                        Viec.TRO_NANG_CHET,
                        "Dịch vụ canh app không đọc được màn hình chính",
                        "Đang không chặn gì. Tắt rồi bật lại quyền trợ năng.",
                        nang = true
                    )
                )
            }
        }

        if (!hasOverlay(context)) {
            add(
                Thieu(
                    Viec.LOP_PHU,
                    "Chưa cho hiển thị trên ứng dụng khác",
                    "Thiếu thì màn chặn và đồng hồ đếm ngược không hiện lên được.",
                    nang = true
                )
            )
        }

        if (!hasBatteryFree(context)) {
            add(
                Thieu(
                    Viec.TIET_KIEM_PIN,
                    "Chưa tắt tiết kiệm pin cho app Nộp bài",
                    "Máy để yên một lúc là hệ thống tắt app, lúc đó nộp bài " +
                        "không ai nhận và giờ chơi đếm sai.",
                    nang = true
                )
            )
        }

        if (laMayXiaomi() && !prefs.daXacNhanTuKhoiDong) {
            add(
                Thieu(
                    Viec.TU_KHOI_DONG,
                    "Chưa xác nhận đã bật Tự khởi động",
                    "Máy Xiaomi tắt hẳn app sau khi khởi động lại nếu thiếu. " +
                        "Bật xong bấm lại dòng này để đánh dấu.",
                    nang = false
                )
            )
        }

        if (!hasDeviceAdmin(context)) {
            add(
                Thieu(
                    Viec.QUAN_TRI,
                    "Chưa bật quyền quản trị thiết bị",
                    "Vẫn chặn được, nhưng lúc này gỡ app ra là xong.",
                    nang = false
                )
            )
        }

        // Bao ca khi Ba Huy chua dat app nghe nhac nen, va bao nang.
        //
        // Truoc 25/9/2026 dong nay chi hien khi da dat app nghe nen, voi y la thieu
        // quyen thi may van bat im duoc ca may. Thu tren may ao Android 13 thi khong:
        // bat im chi van nho tieng vai giay, va con bam Phat o thanh thong bao la nghe
        // tiep. Khong co quyen nay thi may khong biet app nao dang phat de dung dung app
        // do. Xem [GuardAccessibilityService.xetNhacKhiChuaCoQuyen].
        if (!hasNotificationAccess(context)) {
            add(
                Thieu(
                    Viec.DOC_THONG_BAO,
                    "Chưa cho app đọc thông báo",
                    "Thiếu thì hết giờ Lê Hòa vẫn có thể bấm Phát nhạc ở thanh thông báo: " +
                        "máy không biết app nào đang phát để dừng đúng app đó, " +
                        "và không đếm được số phút nghe nhạc nền. Nút nhắn tin cũng " +
                        "không đếm được tin Telegram mới của ba Huy.",
                    nang = true
                )
            )
        }

        if (!hasNotifications(context)) {
            add(
                Thieu(
                    Viec.THONG_BAO,
                    "Chưa cho app hiện thông báo",
                    "Không có đồng hồ đếm ngược, tin của cô giáo cũng không báo.",
                    nang = false
                )
            )
        }
    }

    /** Chi phan lam may khong chan duoc gi. Dung cho tin bao Telegram. */
    fun missingNang(context: Context): List<Thieu> = missing(context).filter { it.nang }

    /**
     * Cac man hinh he thong co the mo de sua [viec], thu lan luot tu dau danh sach.
     *
     * Tra ve nhieu cai vi man hinh Tu khoi dong cua MIUI khong phai may nao cung
     * co, va khong hoi truoc duoc: he thong an app khac khoi tam nhin cua app nay.
     * Mo khong duoc thi roi xuong man hinh thong tin app, tu do van di tiep duoc.
     */
    fun duongVao(context: Context, viec: Viec): List<Intent> = when (viec) {
        Viec.TELEGRAM -> emptyList()
        Viec.TRO_NANG, Viec.TRO_NANG_CHET -> listOf(accessibilityIntent())
        Viec.LOP_PHU -> listOf(overlayIntent(context))
        Viec.QUAN_TRI -> listOf(deviceAdminIntent(context))
        Viec.TIET_KIEM_PIN -> listOf(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            ),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            thongTinApp(context),
        )
        Viec.DOC_THONG_BAO -> buildList {
            // Android 11 tro len mo thang duoc vao dong cua app nay. Man danh sach
            // chung co ca chuc dich vu, tim dung dong minh can la mot viec vat.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(
                            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            ComponentName(context, TaiThongBao::class.java).flattenToString()
                        )
                )
            }
            add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            add(thongTinApp(context))
        }
        Viec.THONG_BAO -> listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            thongTinApp(context),
        )
        Viec.TU_KHOI_DONG -> listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            thongTinApp(context),
        )
    }

    private fun thongTinApp(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))

    /**
     * Tien trinh vua khoi dong thi chua ket luan duoc.
     *
     * Mo app la Android khoi dong lai tien trinh, va dich vu tro nang bind sau do
     * vai giay. Ket luan ngay luc do thi lan nao mo app cung thay bao dong gia.
     */
    private fun vuaKhoiDong(): Boolean =
        SystemClock.elapsedRealtime() - App.khoiDongLuc < 8_000L
}
