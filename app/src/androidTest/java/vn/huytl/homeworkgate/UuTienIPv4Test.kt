package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.grpc.Attributes
import io.grpc.EquivalentAddressGroup
import io.grpc.NameResolverRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.dongbo.UuTienIPv4
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Firestore thu IPv4 truoc IPv6. Xem [UuTienIPv4].
 *
 * May ao khong co IPv6 nen khong dung lai duoc loi that (Wi-Fi phat IPv6 ma IPv6 khong ra
 * internet). Bo test chi giu hai dieu: ham xep dua IPv4 len truoc, va ban xep da nam trong
 * so dang ky cua gRPC luc app chay, tuc la kenh Firestore se di qua no. Khong dung prefs,
 * chay tren may nao cung duoc.
 */
@RunWith(AndroidJUnit4::class)
class UuTienIPv4Test {

    private fun dc(ip: String) = InetSocketAddress(InetAddress.getByName(ip), 443)

    private fun nhom(vararg ip: String) = EquivalentAddressGroup(ip.map { dc(it) })

    private fun thuTu(cacNhom: List<EquivalentAddressGroup>): List<String> =
        cacNhom.flatMap { n -> n.addresses.map { (it as InetSocketAddress).address.hostAddress!! } }

    @Test
    fun moi_dia_chi_mot_nhom_thi_nhom_IPv4_len_truoc_giu_thu_tu_cu() {
        // Kieu gRPC 1.62 tra ve: moi dia chi mot nhom, IPv6 dung dau nhu Android xep.
        val vao = listOf(
            nhom("2404:6800:4003:c00::5f"),
            nhom("142.250.198.202"),
            nhom("2404:6800:4003:c01::5f"),
            nhom("142.250.198.170"),
        )
        assertEquals(
            listOf("142.250.198.202", "142.250.198.170", "2404:6800:4003:c00::5f", "2404:6800:4003:c01::5f"),
            thuTu(UuTienIPv4.xep(vao)),
        )
    }

    @Test
    fun trong_mot_nhom_IPv4_len_truoc_va_giu_thuoc_tinh_cua_nhom() {
        val khoa = Attributes.Key.create<String>("thu")
        val thuocTinh = Attributes.newBuilder().set(khoa, "giu").build()
        val vao = listOf(
            EquivalentAddressGroup(listOf(dc("2404:6800:4003:c00::5f"), dc("142.250.198.202")), thuocTinh),
        )
        val ra = UuTienIPv4.xep(vao)
        assertEquals(listOf("142.250.198.202", "2404:6800:4003:c00::5f"), thuTu(ra))
        assertEquals("giu", ra.single().attributes.get(khoa))
    }

    @Test
    fun chi_co_mot_loai_thi_de_nguyen() {
        val chiV6 = listOf(nhom("2404:6800:4003:c00::5f"), nhom("2404:6800:4003:c01::5f"))
        assertEquals(chiV6, UuTienIPv4.xep(chiV6))
        val chiV4 = listOf(nhom("142.250.198.202", "142.250.198.170"))
        assertSame(chiV4[0], UuTienIPv4.xep(chiV4)[0])
    }

    @Test
    fun app_chay_len_la_ban_xep_da_thay_ban_DNS_cua_gRPC() {
        assertTrue(UuTienIPv4.daDangKy)
        val dangDung = NameResolverRegistry.getDefaultRegistry().getProviderForScheme("dns")!!
        assertTrue(dangDung.javaClass.name, dangDung.javaClass.name.contains("UuTienIPv4"))
        assertEquals("dns", NameResolverRegistry.getDefaultRegistry().defaultScheme)
    }
}
