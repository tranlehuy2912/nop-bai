package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.ai.ChamBaiJson
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.LuatCongGio
import java.time.LocalDateTime

/**
 * Doc cau tra loi cua AI.
 *
 * [THAT] la cau tra loi THAT cua gemini-3.5-flash-lite ngay 13/9/2026 khi cham ba
 * tam anh bai 2.26 cua Le Hoa. Giu nguyen xi lam mau test: model doi cach viet luc
 * nao khong bao truoc, co mot ban ghi that de doi chieu thi biet ngay.
 *
 * Bo nay KHONG xoa prefs va khong goi mang.
 */
@RunWith(AndroidJUnit4::class)
class ChamBaiJsonTest {

    private val toiThuHai = LocalDateTime.of(2026, 9, 14, 19, 0)

    /**
     * Cau tra loi THAT cua Gemini, luu lai tu mot lan cham that.
     *
     * Co them "co_de":true so voi ban ghi goc: truong do sinh ra sau (16/9/2026),
     * khi phat hien con nop nhung trang vo chi co dap an ma khong co de bai. Ban
     * ghi cu khong co truong nay - xem [ban_cu_khong_co_co_de_thi_khong_tra_gio].
     */
    private val THAT = """
{"mon":"Toán","ngay_dan_do":null,"lam_het_dan_do":true,"cac_cau":[
{"ma":"2.26a","co_de":true,"de":"x^2 - 6x + 9 - y^2","ket_qua":"(x - 3 - y)(x - 3 + y)","dung":true,"doc_ro":true,"dang":"CAU_NHO","trong_dan_do":true,"so_dong":4,"nhan_xet":"Bài làm tốt, trình bày rõ ràng và chính xác."},
{"ma":"2.26b","co_de":true,"de":"4x^2 - y^2 + 4y - 4","ket_qua":"(2x - y + 2)(2x + y - 2)","dung":true,"doc_ro":true,"dang":"CAU_NHO","trong_dan_do":true,"so_dong":4,"nhan_xet":"Bài làm tốt, biến đổi đúng."},
{"ma":"2.26c","co_de":true,"de":"xy + z^2 + xz + yz","ket_qua":"(x + z)(y + z)","dung":true,"doc_ro":true,"dang":"CAU_NHO","trong_dan_do":true,"so_dong":4,"nhan_xet":"Bài làm đúng và sạch sẽ."},
{"ma":"2.26d","co_de":true,"de":"x^2 - 4xy + 4y^2 + xz - 2yz","ket_qua":"(x - 2y)(x - 2y - z)","dung":false,"doc_ro":true,"dang":"CAU_NHO","trong_dan_do":true,"so_dong":5,"nhan_xet":"Kết quả cuối cùng chưa đúng, cần nhóm hạng tử chính xác hơn."}],
"tom_tat":"Học sinh hoàn thành tốt bài 2.26 phần a, b, c; riêng phần d kết quả cuối cùng chưa chính xác."}
    """.trimIndent()

    @Test
    fun doc_duoc_cau_tra_loi_that() {
        val ket = ChamBaiJson.doc(THAT)!!
        assertEquals("Toán", ket.mon)
        assertEquals(4, ket.cac.size)
        assertNull(ket.ngayDanDo)

        val d = ket.cac.last()
        assertEquals("2.26d", d.ma)
        assertFalse(d.dung)
        assertEquals(5, d.soDong)
        // Chep dung cai con viet, khong phai dap an dung cua sach.
        assertEquals("(x - 2y)(x - 2y - z)", d.ketQua)
    }

    @Test
    fun cau_tra_loi_that_quy_ra_dung_so_phut() {
        val ket = ChamBaiJson.doc(THAT)!!
        val b = LuatCongGio.tinh(ket, bayGio = toiThuHai)

        // Ba cau dung, moi cau 4 dong -> 4 phut mot cau (mot dong mot phut tu 27/9/2026).
        // Cau d sai: khong tinh. Khong chup vo dan do (ngay_dan_do null) nen khong co
        // tron goi 45.
        assertEquals(12, b.phut)
        assertTrue(b.dong.any { it.contains("2.26d") && it.contains("sửa lại") })
    }

    @Test
    fun boc_trong_dau_ngoac_ba_nhay_van_doc_duoc() {
        val ket = ChamBaiJson.doc("Đây là kết quả:\n```json\n$THAT\n```\n")
        assertEquals(4, ket!!.cac.size)
    }

    @Test
    fun thieu_truong_thi_lay_mac_dinh_an_toan() {
        // Khong co "dung", khong co "doc_ro": coi nhu chua dung va doc khong ro.
        // Sai huong nay thi con nop lai duoc; sai huong kia la cong gio cho bai sai.
        val ket = ChamBaiJson.doc("""{"cac_cau":[{"ma":"1","de":"2+2"}]}""")!!
        val c = ket.cac.first()
        assertFalse(c.dung)
        assertFalse(c.docRo)
        assertEquals(DangBai.CAU_NHO, c.dang)
        assertTrue(LuatCongGio.tinh(ket, bayGio = toiThuHai).canBaHuyXem)
    }

    @Test
    fun dang_viet_chu_thuong_van_hieu() {
        val ket = ChamBaiJson.doc(
            """{"cac_cau":[{"ma":"đoạn văn","de":"tả mẹ","co_de":true,"dung":true,
               "doc_ro":true,"dang":"viet_dai","so_dong":22}]}"""
        )!!
        assertEquals(DangBai.VIET_DAI, ket.cac.first().dang)
        assertEquals(29, LuatCongGio.tinh(ket, bayGio = toiThuHai).phut)
    }

    @Test
    fun ban_cu_khong_co_co_de_thi_khong_tra_gio() {
        /*
         * Thieu "co_de" thi coi nhu KHONG co de, tuc la khong tra gio.
         *
         * Nga ve huong nay la co y, va day la mot danh doi that: mot model quen tra
         * truong do se lam con khong duoc phut nao. Nhung hong kieu do thi con doc
         * duoc ngay tren man hinh ("khong thay de bai, chup them trang de giup"), va
         * Ba Huy van con hai nut duyet ben Telegram. Nga huong kia thi mot trang vo
         * toan dap an lai duoc cong gio nhu cu, im lang, khong ai biet.
         */
        val ket = ChamBaiJson.doc(
            """{"cac_cau":[{"ma":"câu 1","de":"","ket_qua":"B","dung":true,
               "doc_ro":true,"dang":"TRAC_NGHIEM"}]}"""
        )!!
        assertFalse(ket.cac.first().coDe)
        assertEquals(0, LuatCongGio.tinh(ket, bayGio = toiThuHai).phut)
    }

    @Test
    fun tra_loi_rac_thi_tra_ve_null_chu_khong_no() {
        assertNull(ChamBaiJson.doc(null))
        assertNull(ChamBaiJson.doc(""))
        assertNull(ChamBaiJson.doc("Xin lỗi, tôi không đọc được ảnh."))
        assertNull(ChamBaiJson.doc("{ hỏng"))
        // JSON dung nhung khong co cau nao: cung la khong cham duoc.
        assertNull(ChamBaiJson.doc("""{"mon":"Toán","cac_cau":[]}"""))
    }
    @Test
    fun doc_duoc_danh_sach_bai_co_giao() {
        val ket = ChamBaiJson.doc(
            """{"ngay_dan_do":"2026-09-14","bai_duoc_giao":["bài 2"," ","bài 3"],
               "lam_het_dan_do":true,"cac_cau":[{"ma":"1","de":"x","dung":true,"doc_ro":true}]}"""
        )!!
        assertEquals(listOf("bài 2", "bài 3"), ket.baiDuocGiao)

        // Thieu han truong do - ban cu cua model, hay vo dan do khong giao bai nao.
        // Rong thi khong co tron goi, du "lam_het_dan_do" co true di nua.
        val trong = ChamBaiJson.doc(
            """{"ngay_dan_do":"2026-09-14","lam_het_dan_do":true,
               "cac_cau":[{"ma":"1","de":"x","co_de":true,"dung":true,"doc_ro":true,
               "so_dong":4}]}"""
        )!!
        assertTrue(trong.baiDuocGiao.isEmpty())
        assertEquals(4, LuatCongGio.tinh(trong, bayGio = toiThuHai).phut)
    }
}
