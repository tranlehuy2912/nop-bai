package vn.huytl.homeworkgate.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Cua vao khi bam icon app. Khong ve gi, tu dong ngay.
 *
 * Truoc day icon tro thang vao HomeActivity, ma HomeActivity la singleTask: mo mot
 * man singleTask la Android dong het moi man nam tren no. Nen bam icon la mat trang
 * dang mo, du chi vua ra ngoai mot giay. Ba Huy dang go do trong Cai dat, sang app
 * khac chep khoa roi bam icon quay ve la mat trang, trong khi mo tu danh sach app gan
 * day thi trang van con.
 *
 * Gio icon tro vao day. App dang chay thi Android chi dua ca chong man cu len truoc,
 * y nhu danh sach app gan day, va man nay khong duoc tao ra. App chua chay thi man
 * nay moi chay: mo HomeActivity roi dong. Ai duoc xem trang cua Ba Huy sau khi vang
 * mat thi do [HoiLaiPin] lo, vao bang duong nao cung vay.
 *
 * HomeActivity van giu singleTask: guard, man chan va thong bao mo no de keo con ve
 * man chinh, luc do van phai dong het moi man nam tren.
 */
class CuaVaoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Launcher gui intent khac thuong thi Android co the dat man nay len tren chong
        // man dang co, thay vi chi dua chong do len. Luc do chi can dong di la lo ra
        // dung man cu.
        if (!isTaskRoot) {
            finish()
            return
        }
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
