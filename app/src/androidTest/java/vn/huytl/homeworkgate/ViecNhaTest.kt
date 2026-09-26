package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.ViecNha

/**
 * Viec nha ba noi giao: doc ban tu Firestore, va quyet luc nao mo khoa luc nao
 * cong gio.
 *
 * Cho de sai nhat khong phai viec doc du lieu, ma la [ViecNha.apDung]: document
 * hop/viecnha la TRANG THAI chu khong phai su kien, nen tablet doc lai cung mot ban
 * muoi lan van phai ra dung mot lan cong gio.
 *
 * TEN TRUONG VIET THANG CHU KHONG QUA Duong: bo test nay dong vai may ba noi ghi
 * xuong Firestore, nen no phai ghim dung cai ten dang chay tren mang. Dung hang so
 * thi doi ten trong Duong.kt van xanh, ma hai app that thi da het hieu nhau.
 *
 * CAN THAN: bo test nay xoa sach prefs. Dung chay tren tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class ViecNhaTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(context).raw().edit().clear().commit()
    }

    /** Mot dau viec, y nhu may ba ghi xuong. */
    private fun viec(ten: String, phut: Int, xong: Boolean) =
        mapOf("ten" to ten, "phut" to phut, "xong" to xong)

    /** Mot ban trang thai day du. */
    private fun ban(ma: String, vararg cac: Map<String, Any>) =
        ViecNha.tuBan(ma, cac.toList(), 0L)!!

    @Test
    fun doc_dung_ten_va_so_phut() {
        val p = ban(
            "ab12",
            viec("Quét nhà lau nhà", 10, false),
            viec("Rửa chén", 15, true)
        )
        assertEquals("ab12", p.id)
        assertEquals(2, p.cac.size)
        assertEquals("Quét nhà lau nhà", p.cac[0].ten)
        assertEquals(10, p.cac[0].phut)
        assertFalse(p.cac[0].xong)
        assertTrue(p.cac[1].xong)
        assertEquals(25, p.tongPhut)
    }

    @Test
    fun ban_khong_ra_hinh_thu_gi_thi_bo_qua() {
        // Khong co ma phien thi khong biet day la dot nao, ma "dot nao" la thu duy
        // nhat phan biet giao moi voi sua dot dang chay.
        assertNull(ViecNha.tuBan("", listOf(viec("Quét nhà", 10, false)), 0L))
        // Dau viec thieu ten thi bo rieng dau do, khong bo ca ban.
        assertEquals(0, ViecNha.tuBan("ab12", listOf(mapOf("phut" to 10)), 0L)!!.cac.size)
        assertEquals(0, ViecNha.tuBan("ab12", listOf(mapOf("ten" to "  ")), 0L)!!.cac.size)
        // Thieu so phut thi tinh 0 phut, van la mot viec phai lam.
        val p = ViecNha.tuBan("ab12", listOf(mapOf("ten" to "Quét nhà")), 0L)!!
        assertEquals(1, p.cac.size)
        assertEquals(0, p.cac[0].phut)
    }

    @Test
    fun so_phut_qua_lon_bi_keo_ve_tran() {
        // May ba khong go tay so nay, nhung du lieu tren mang thi khong hua gi ca.
        val p = ban("ab12", viec("Quét nhà", 9999, false))
        assertEquals(240, p.cac[0].phut)
    }

    @Test
    fun ba_giao_viec_thi_tablet_khoa() {
        val p = ban("ab12", viec("Quét nhà", 10, false), viec("Rửa chén", 10, false))
        assertEquals(ViecNha.Doi.MOI, ViecNha.apDung(context, p))
        assertTrue(ViecNha.dangKhoa(context))
        assertEquals("Quét nhà, Rửa chén", ViecNha.keChuaXong(context))
    }

    @Test
    fun doc_lai_cung_mot_ban_thi_khong_co_gi_doi() {
        val p = ban("ab12", viec("Quét nhà", 10, false))
        ViecNha.apDung(context, p)
        // Listener bay lai moi lan doi mang hay moi lan app khoi dong. Khong co cho
        // nay thi moi lan do lai mot lan "ba vua giao viec".
        assertEquals(ViecNha.Doi.KHONG_DOI, ViecNha.apDung(context, p))
        assertEquals(ViecNha.Doi.KHONG_DOI, ViecNha.apDung(context, p))
    }

    @Test
    fun xong_mot_viec_thi_van_con_khoa() {
        ViecNha.apDung(context, ban("ab12", viec("Quét nhà", 10, false), viec("Rửa chén", 10, false)))
        val moi = ban("ab12", viec("Quét nhà", 10, true), viec("Rửa chén", 10, false))
        assertEquals(ViecNha.Doi.BOT_MOT_VIEC, ViecNha.apDung(context, moi))
        assertTrue(ViecNha.dangKhoa(context))
        assertEquals("Rửa chén", ViecNha.keChuaXong(context))
    }

    @Test
    fun xong_het_thi_mo_khoa_va_bao_tong_so_phut() {
        ViecNha.apDung(context, ban("ab12", viec("Quét nhà", 10, false), viec("Rửa chén", 15, false)))
        val moi = ban("ab12", viec("Quét nhà", 10, true), viec("Rửa chén", 15, true))
        assertEquals(ViecNha.Doi.XONG_HET, ViecNha.apDung(context, moi))
        assertEquals(25, moi.tongPhut)
        // Ben goi cong gio xong thi tu xoa; truoc do phien van con de doc so phut.
        ViecNha.xoa(context)
        assertFalse(ViecNha.dangKhoa(context))
    }

    @Test
    fun ba_bo_het_thi_mo_khoa_ma_khong_cong_gi() {
        ViecNha.apDung(context, ban("ab12", viec("Quét nhà", 10, false)))
        assertEquals(ViecNha.Doi.BO_HET, ViecNha.apDung(context, ban("ab12")))
        assertFalse(ViecNha.dangKhoa(context))
        assertNull(ViecNha.dangTreo(context))
    }

    @Test
    fun ba_giao_dot_moi_thi_bo_dot_cu_dang_do() {
        ViecNha.apDung(context, ban("ab12", viec("Quét nhà", 10, false), viec("Rửa chén", 10, false)))
        // Ma phien khac: chinh ba vua quyet dinh nhu vay, dot cu khong con nghia.
        val moi = ban("cd34", viec("Tưới cây", 10, false))
        assertEquals(ViecNha.Doi.MOI, ViecNha.apDung(context, moi))
        assertEquals("Tưới cây", ViecNha.keChuaXong(context))
    }

    @Test
    fun dot_da_khep_thi_nho_lai_de_khong_cong_lan_hai() {
        // Khep xong thi tablet xoa document tren Firestore. Lenh xoa do co the khong
        // di duoc, va luc ay ban da xong het nam lai - lan doc sau phai nhan ra.
        val p = ban("ab12", viec("Quét nhà", 10, true))
        assertEquals(ViecNha.Doi.XONG_HET, ViecNha.apDung(context, p))
        ViecNha.xoa(context)
        ViecNha.ghiDaKhep(context, p.id)

        assertEquals("ab12", ViecNha.daKhep(context))
        // Dot khac thi khong dinh gi toi dau nay.
        assertTrue(ViecNha.daKhep(context) != "cd34")
    }

    @Test
    fun nho_ai_giao_de_goi_dung_nguoi() {
        // Bang dieu khien gui "bahuy". May ba gui "banoi", ban cu khong gui gi.
        val p = ViecNha.tuBan("ab12", listOf(viec("Quét nhà", 10, false)), 0L, "bahuy")!!
        assertEquals(ViecNha.Doi.MOI, ViecNha.apDung(context, p))
        assertEquals("bahuy", ViecNha.dangTreo(context)!!.ai)
        assertEquals("Ba Huy", ViecNha.nguoiGiao(ViecNha.dangTreo(context)))

        assertEquals("banoi", ban("cd34", viec("Rửa chén", 10, false)).ai)
        assertEquals("Bà nội", ViecNha.nguoiGiao(null))
    }

    @Test
    fun phien_moi_ma_da_xong_het_thi_khong_khoa() {
        // Gap khi mang rot dung luc: tablet bo lo ca doan giua, den luc doc duoc thi
        // ba da bam xong het. Khong duoc khoa may mot nhip nao, nhung van cong gio.
        val p = ban("ab12", viec("Quét nhà", 10, true))
        assertEquals(ViecNha.Doi.XONG_HET, ViecNha.apDung(context, p))
        assertFalse(ViecNha.dangKhoa(context))
    }
}
