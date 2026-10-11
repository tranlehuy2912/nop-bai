package vn.huytl.homeworkgate.dongbo

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Ket noi thang tu man Remote toi dich vu netflix-remote tren laptop qua Wi-Fi nha (11/10/2026,
 * anh Huy chot: khong qua Firestore cho khoi tre). Cach noi, cac lenh xem dau file
 * tools/laptop/netflix-remote ben repo nop-bai.
 *
 * Mot luong gui: noi, tra ma, roi gui lenh theo thu tu bam. Di chuot thi cong don [diChuot] lai
 * va gui moi [NHIP_CHUOT_MS] mot lan, khong gui tung su kien cham. Mot luong doc: AL (am luong),
 * LOI, BAO, PONG. Moi ham goi lai chay tren luong chinh.
 *
 * Chep nguyen tu Bang dieu khien (data/KetNoiRemote.kt) ngay 11/10/2026 cho man Remote tren tablet;
 * sua mot ben thi sua ca ben kia.
 *
 * Moi doi tuong chi noi mot lan: noi lai thi tao doi tuong moi, de luong cua lan noi truoc dang
 * thoat khong dong nham lan noi sau. Ham goi lai cua doi tuong cu van co the toi sau khi da thay,
 * ben goi phai bo qua.
 */
class KetNoiRemote(
    private val remote: Laptop.RemoteLaptop,
    private val khiDoi: (TrangThai) -> Unit,
    /** Dong AL: phan tram (null khi khong ai dang nhap) va co dang tat tieng khong. */
    private val khiAm: (Int?, Boolean) -> Unit,
    /** Cau LOI hay BAO laptop gui ve, de hien cho nguoi bam. */
    private val khiBao: (String) -> Unit
) {
    enum class TrangThai { DANG_NOI, DA_NOI, KHONG_NOI_DUOC, SAI_MA }

    private val chinh = Handler(Looper.getMainLooper())
    private val hang = LinkedBlockingQueue<String>()
    private val khoaChuot = Any()
    private var dx = 0
    private var dy = 0

    @Volatile private var dang = false
    @Volatile private var oc: Socket? = null
    @Volatile private var lanNhanCuoi = 0L

    @Volatile var trangThai = TrangThai.DANG_NOI
        private set

    fun noi() {
        dang = true
        doi(TrangThai.DANG_NOI)
        Thread({ chay() }, "remote-gui").start()
    }

    fun dong() {
        dang = false
        hang.offer(DUNG)
        runCatching { oc?.close() }
    }

    val daNoi get() = trangThai == TrangThai.DA_NOI

    fun diChuot(x: Int, y: Int) {
        if (x == 0 && y == 0) return
        synchronized(khoaChuot) { dx += x; dy += y }
    }

    fun bamChuot() = gui("B")
    fun cuon(n: Int) = gui("C $n")
    fun phim(ten: String) = gui("K $ten")
    fun go(chu: String) { if (chu.isNotEmpty()) gui("T $chu") }
    fun amLuong(tang: Boolean) = gui(if (tang) "V +" else "V -")
    fun veManChon() = gui("X")

    /** Dang xuat, tat laptop (11/10/2026, cho man Remote tren tablet), laptop tra BAO hay LOI. */
    fun dangXuat() = gui("DX")
    fun tatMay() = gui("TAT")

    private fun gui(dong: String) {
        if (daNoi) hang.offer(dong)
    }

    private fun doi(t: TrangThai) {
        trangThai = t
        chinh.post { khiDoi(t) }
    }

    private fun chay() {
        val s = Socket()
        oc = s
        try {
            s.tcpNoDelay = true
            s.connect(InetSocketAddress(remote.ip, remote.cong), NOI_MS)
            s.soTimeout = NOI_MS
            val doc = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val viet = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
            val chao = doc.readLine().orEmpty()
            if (!chao.startsWith("CHAO ")) throw java.io.IOException("laptop khong chao: $chao")
            viet.write("MA ${maKy(remote.khoa, chao.removePrefix("CHAO ").trim())}\n")
            viet.flush()
            val tra = doc.readLine().orEmpty()
            if (tra != "OK") {
                doi(if (tra == "SAI") TrangThai.SAI_MA else TrangThai.KHONG_NOI_DUOC)
                return
            }
            s.soTimeout = 0
            lanNhanCuoi = System.currentTimeMillis()
            doi(TrangThai.DA_NOI)
            Thread({ docMai(doc) }, "remote-doc").start()
            var lanHoi = System.currentTimeMillis()
            while (dang) {
                val lenh = hang.poll(NHIP_CHUOT_MS, TimeUnit.MILLISECONDS)
                // Gui phan chuot don lai truoc lenh vua bam, de bam chuot roi dung cho.
                val (x, y) = synchronized(khoaChuot) { (dx to dy).also { dx = 0; dy = 0 } }
                if (x != 0 || y != 0) viet.write("M $x $y\n")
                if (lenh != null && lenh != DUNG) viet.write(lenh + "\n")
                val bayGio = System.currentTimeMillis()
                if (bayGio - lanHoi >= HOI_MS) {
                    viet.write("P\n")
                    lanHoi = bayGio
                }
                viet.flush()
                if (bayGio - lanNhanCuoi > IM_MS) throw java.io.IOException("laptop im qua lau")
            }
        } catch (e: Exception) {
            if (dang) doi(TrangThai.KHONG_NOI_DUOC)
        } finally {
            dang = false
            runCatching { s.close() }
        }
    }

    private fun docMai(doc: BufferedReader) {
        try {
            while (true) {
                val dong = doc.readLine() ?: break
                lanNhanCuoi = System.currentTimeMillis()
                when {
                    dong.startsWith("AL ") -> {
                        val cot = dong.split(" ")
                        val pt = cot.getOrNull(1)?.toIntOrNull()
                        val tat = cot.getOrNull(2) == "1"
                        chinh.post { khiAm(pt, tat) }
                    }
                    dong.startsWith("LOI ") || dong.startsWith("BAO ") -> {
                        val chu = dong.substring(4)
                        chinh.post { khiBao(chu) }
                    }
                }
            }
        } catch (_: Exception) {
        }
        // Laptop dong ket noi (tat may, dich vu chay lai): luong gui thay o lan ghi sau.
        if (dang) {
            dang = false
            hang.offer(DUNG)
            runCatching { oc?.close() }
            doi(TrangThai.KHONG_NOI_DUOC)
        }
    }

    companion object {
        private const val DUNG = "\u0000"
        private const val NOI_MS = 3000
        /** Chuot gui toi da khoang 60 lan mot giay. */
        private const val NHIP_CHUOT_MS = 16L
        private const val HOI_MS = 10_000L
        /** Laptop tra PONG moi lan hoi; im qua chung nay la ket noi da chet. */
        private const val IM_MS = 25_000L

        /** HMAC-SHA256 cua [so], khoa la chuoi [khoa], viet hex chu thuong nhu laptop. */
        fun maKy(khoa: String, so: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(khoa.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            return mac.doFinal(so.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }
    }
}
