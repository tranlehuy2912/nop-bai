package vn.huytl.homeworkgate.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * May co dang noi mang khong.
 *
 * VI SAO CAN. Ca duong nop bai deu phai co mang: cham bai goi API cua Google, gui
 * anh goi API cua Telegram. Mat mang thi con chup xong het ca xap roi moi biet la
 * hong - va luc do may tam anh da bi don di, con phai chup lai tu dau.
 *
 * Nen chan ngay o nut "Nop bai tap", kem mot cau noi ro vi sao. Chan truoc mot cai
 * de dang hon nhieu so voi bao hong sau khi da lam xong viec.
 */
object Mang {

    fun co(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val mang = cm.activeNetwork ?: return false
        val kha = cm.getNetworkCapabilities(mang) ?: return false
        return kha.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            kha.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
