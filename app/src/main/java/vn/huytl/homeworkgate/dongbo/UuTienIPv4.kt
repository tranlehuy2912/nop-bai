package vn.huytl.homeworkgate.dongbo

import android.util.Log
import io.grpc.EquivalentAddressGroup
import io.grpc.NameResolver
import io.grpc.NameResolverProvider
import io.grpc.NameResolverRegistry
import io.grpc.Status
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URI

/**
 * Bat Firestore thu dia chi IPv4 truoc IPv6.
 *
 * LOI GAP NGAY 04/11/2026 TREN TABLET THAT. Wi-Fi nha (LeHuy) phat IPv6 cho tablet, nhung
 * IPv6 khong ra duoc internet: tu tablet, mo TCP 443 toi firestore.googleapis.com qua IPv4
 * thi noi ngay, qua IPv6 thi treo; ping6 toi Google, Telegram mat het goi. Tablet co dia
 * chi IPv6 toan cau nen Android xep dia chi IPv6 len truoc (RFC 6724). gRPC 1.62 cua
 * Firestore thu tung dia chi theo thu tu, ma noi TCP toi IPv6 thi treo chu khong bao loi.
 * Firestore (GrpcCallProvider) thay kenh dung o CONNECTING 15 giay thi huy kenh lam lai tu
 * dau, lai bang IPv6, nen khong bao gio toi luot IPv4. Logcat tu 18:03 toi 19:36 toan
 * "Channel shutdownNow invoked" cu 15 giay mot lan; lenh PING nam cho trong lenh/,
 * hop/trangthai dung o toi hom truoc. Telegram van chay vi OkHttp 5 thu IPv4 va IPv6 song
 * song. Sang 04/11 cung Firestore im ma Telegram chay y nhu vay, nhung nhat ky buoi sang
 * da troi mat nen khong kiem lai duoc. May ao khong co IPv6 nen khong ra loi nay.
 *
 * Android khong cho app hay adb (khong root) tat IPv6 cua Wi-Fi, va anh Huy khong o nha de
 * tat tren router, nen sua trong app.
 *
 * CACH LAM: dang ky mot NameResolverProvider cho scheme "dns", uu tien cao hon ban DNS cua
 * gRPC (5). Firestore dung kenh bang OkHttpChannelBuilder.forTarget(host), host khong co
 * scheme, nen gRPC lay scheme mac dinh cua so dang ky chung, tuc la ban nay. Ban nay goi
 * lai ban DNS goc (giu nguyen phan thu lai, bo nho dem cua no), chi xep lai ket qua: IPv4
 * len truoc, IPv6 giu phia sau. Mang chi co IPv6 thi DNS khong tra ve ban ghi A nao, nen
 * thu tu khong doi va van noi bang IPv6.
 *
 * Phai goi [batDau] truoc khi Firestore dung kenh dau tien, nen goi o dau App.onCreate.
 * Telegram, Firebase Auth khong di qua gRPC nen khong bi anh huong.
 */
object UuTienIPv4 {
    private const val TAG = "HomeworkGate"

    /** Ban DNS cua gRPC (DnsNameResolverProvider) co uu tien 5; so lon hon thi duoc chon. */
    private const val UU_TIEN = 6

    @Volatile
    var daDangKy = false
        private set

    fun batDau() {
        if (daDangKy) return
        val soDangKy = NameResolverRegistry.getDefaultRegistry()
        val goc = soDangKy.getProviderForScheme("dns")
        if (goc == null) {
            Log.w(TAG, "uu tien IPv4: khong thay ban DNS cua gRPC, de nguyen")
            return
        }
        soDangKy.register(BanXep(goc))
        daDangKy = true
    }

    /**
     * Dua dia chi IPv4 len truoc, IPv6 ra sau, giu thu tu cu trong tung loai. Lam ca hai
     * tang: nhom co IPv4 len truoc nhom khong co, va trong moi nhom IPv4 len truoc. gRPC
     * 1.62 tra moi dia chi mot nhom, con pick-first trai phang het roi thu tu dau.
     */
    fun xep(cacNhom: List<EquivalentAddressGroup>): List<EquivalentAddressGroup> {
        val daXep = cacNhom.map { nhom ->
            val (v4, khac) = nhom.addresses.partition { laIPv4(it) }
            if (v4.isEmpty() || khac.isEmpty()) nhom
            else EquivalentAddressGroup(v4 + khac, nhom.attributes)
        }
        val (coV4, khongCo) = daXep.partition { nhom -> nhom.addresses.any { laIPv4(it) } }
        return coV4 + khongCo
    }

    private fun laIPv4(dc: SocketAddress): Boolean =
        dc is InetSocketAddress && dc.address is Inet4Address

    private fun demIPv4(cacNhom: List<EquivalentAddressGroup>): Int =
        cacNhom.sumOf { nhom -> nhom.addresses.count { laIPv4(it) } }

    private class BanXep(private val goc: NameResolverProvider) : NameResolverProvider() {
        override fun isAvailable(): Boolean = true
        override fun priority(): Int = UU_TIEN
        override fun getDefaultScheme(): String = goc.defaultScheme
        override fun getProducedSocketAddressTypes(): Collection<Class<out SocketAddress>> =
            goc.producedSocketAddressTypes

        override fun newNameResolver(uri: URI, args: NameResolver.Args): NameResolver? {
            val trong = goc.newNameResolver(uri, args) ?: return null
            return BocKetQua(trong)
        }
    }

    private class BocKetQua(private val trong: NameResolver) : NameResolver() {
        override fun getServiceAuthority(): String = trong.serviceAuthority

        override fun start(nghe: Listener2) {
            trong.start(object : Listener2() {
                override fun onResult(kq: ResolutionResult) {
                    val cu = kq.addresses
                    val moi = xep(cu)
                    val tong = cu.sumOf { it.addresses.size }
                    val v4 = demIPv4(cu)
                    if (moi != cu) {
                        Log.i(TAG, "uu tien IPv4: ${trong.serviceAuthority} co $v4 IPv4, " +
                            "${tong - v4} IPv6, dua IPv4 len truoc")
                    } else {
                        Log.d(TAG, "uu tien IPv4: ${trong.serviceAuthority} co $v4 IPv4, " +
                            "${tong - v4} IPv6, giu thu tu")
                    }
                    // toBuilder giu nguyen thuoc tinh cua ket qua: ban DNS goc cai vao do
                    // cho kenh bao lai lan phan giai co thanh khong, de no hen lan thu lai.
                    nghe.onResult(kq.toBuilder().setAddresses(moi).build())
                }

                override fun onError(loi: Status) = nghe.onError(loi)
            })
        }

        override fun refresh() = trong.refresh()
        override fun shutdown() = trong.shutdown()
    }
}
