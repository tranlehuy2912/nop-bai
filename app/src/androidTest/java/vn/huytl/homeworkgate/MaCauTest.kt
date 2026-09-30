package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.ui.MaCau

/**
 * Ma phieu Claude tu dat (30/9/2026): "B5-TL3g" hien "B5-TL3g: Bài 5, tự luận, câu 3g". Ban
 * Bang dieu khien co MaCauTest y het, chay bang unit test.
 */
@RunWith(AndroidJUnit4::class)
class MaCauTest {

    @Test
    fun doc_ma_phieu_kem_bai_phan_va_cau() {
        assertEquals("Bài 5, tự luận, câu 3g", MaCau.moTa("B5-TL3g"))
        assertEquals("Bài 5, trắc nghiệm, câu 1", MaCau.moTa("B5-TN1"))
        assertEquals("Bài 12, tự luận, câu 10b", MaCau.moTa(" b12-tl10B "))
        assertEquals("Trắc nghiệm, câu 4", MaCau.moTa("TN4"))
        assertEquals("B5-TL3g: Bài 5, tự luận, câu 3g", MaCau.hien("B5-TL3g"))
    }

    @Test
    fun ma_sach_va_ma_khac_de_nguyen() {
        listOf("2.19a", "3b", "2", "BT3", "B5", "TL", "B5-TL3g)").forEach { assertNull(it, MaCau.moTa(it)) }
        assertEquals("2.19a", MaCau.hien(" 2.19a "))
    }
}
