package vn.huytl.homeworkgate

import android.app.Activity
import android.app.Application
import android.os.Bundle
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.kho.NganHang

/**
 * Chi de biet mot viec: man hinh cua chinh app nay co dang truoc mat khong.
 *
 * Truoc day cau hoi do duoc tra loi bang cach xem su kien cua so cuoi cung mang
 * ten goi nao. Sai, vi lop phu "Lê Hòa 5 phut" cung mang ten goi nay: no hien len
 * mot cai la coi nhu app dang o truoc mat, va vi lop phu tu an di khong sinh ra
 * su kien nao nen ket luan do dung mai. Hau qua la den luc het gio, bo canh tuong
 * con dang o trong app nha nen khong day di dau, va dua tre choi tiep.
 *
 * Vong doi Activity thi khong doan: chi man hinh that moi goi onResume.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        donDepKho()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (dangHien == 0 && roiNenLuc > 0L) {
                    vangMatMs = android.os.SystemClock.elapsedRealtime() - roiNenLuc
                }
                dangHien += 1
                manHinhCuaAppDangMo = true
            }

            override fun onActivityPaused(activity: Activity) {
                dangHien = (dangHien - 1).coerceAtLeast(0)
                manHinhCuaAppDangMo = dangHien > 0
                if (dangHien == 0) roiNenLuc = android.os.SystemClock.elapsedRealtime()
            }

            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, out: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /**
     * Ba viec voi kho bai tap, lam mot lan luc app khoi dong.
     *
     * Chay tren luong nen vi co doc file va ghi SQLite. Khong cho ai cai gi ca: man
     * hinh dau tien khong dung den kho, va neu ba viec nay chua xong luc con bam
     * "Nop bai" thi cung chi la danh sach bai hien ra cham mot nhip.
     */
    private fun donDepKho() {
        Thread {
            val ct = applicationContext
            runCatching { SoCaiBai.chuyenSoCu(ct) }
            runCatching { NganHang.napNeuCan(ct) }
            runCatching { SoCaiBai.donCu(ct) }
        }.apply { isDaemon = true }.start()
    }

    companion object {
        /** Luc tien trinh nay bat dau, de biet da du lau de ket luan chua. */
        val khoiDongLuc: Long = android.os.SystemClock.elapsedRealtime()

        private var dangHien = 0
        private var roiNenLuc = 0L

        /**
         * App vua vang mat bao lau truoc lan quay lai nay.
         *
         * Doi man hinh trong cung app thi so nay chi vai chuc mili giay, vi onPause
         * cua man cu va onResume cua man moi di lien nhau. Ra han khoi app - bam
         * nut home, tat man hinh - thi no tinh bang giay tro len. Do la cach phan
         * biet "dang lam viec trong app" voi "de do roi di".
         */
        @Volatile
        var vangMatMs = 0L
            private set

        /** Mot man hinh cua app dang o truoc mat nguoi dung. */
        @Volatile
        var manHinhCuaAppDangMo = false
            private set
    }
}
