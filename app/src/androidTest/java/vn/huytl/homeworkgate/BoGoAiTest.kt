package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.BoGoAi

/**
 * Kiem tra viec dung lai cau hoi tu dong chu roi rac.
 *
 * Day la phan de sai nhat cua tinh nang ghi cau hoi AI: dich vu tro nang ban ve
 * tung phim mot, ghep sai thi nhat ky day manh vun hoac ghi trung. Test o day
 * chay bang JVM thuong, khong can may.
 */
@RunWith(AndroidJUnit4::class)
class BoGoAiTest {

    /** Go dan tung phim roi bam gui (o nhap trong): phai ra dung cau day du. */
    @Test
    fun goDanRoiGuiThiRaCauDayDu() {
        val bo = BoGoAi()
        assertNull(bo.goChu("giai", 0))
        assertNull(bo.goChu("giai giup", 100))
        assertNull(bo.goChu("giai giup bai 3", 200))
        // O nhap trong = da bam gui.
        assertEquals("giai giup bai 3", bo.goChu("", 300))
    }

    /** Chua gui ma da sang app khac: cau dang go do van phai duoc chot. */
    @Test
    fun roiAppThiChotCauDangGo() {
        val bo = BoGoAi()
        bo.goChu("bai nay sai o dau", 0)
        assertEquals("bai nay sai o dau", bo.roiApp())
    }

    /** Go xong ngoi im: sau nguong im thi chot. */
    @Test
    fun imLauThiChot() {
        val bo = BoGoAi(imMs = 8_000L)
        bo.goChu("chi cho minh cho sai", 1_000)
        assertNull(bo.imLau(5_000))          // moi 4 giay, chua chot
        assertEquals("chi cho minh cho sai", bo.imLau(10_000))  // 9 giay, chot
    }

    /** Cau qua ngan thi bo, khong ghi "ok", "hi". */
    @Test
    fun cauQuaNganThiBo() {
        val bo = BoGoAi(toiThieu = 6)
        bo.goChu("ok", 0)
        assertNull(bo.goChu("", 100))
        bo.goChu("hi ban", 200)
        assertEquals("hi ban", bo.goChu("", 300))  // 6 ky tu, vua du
    }

    /** Chot roi khong lap lai cung cau: con go i het lan hai khong thanh hai dong. */
    @Test
    fun khongGhiTrungCauVuaChot() {
        val bo = BoGoAi()
        bo.goChu("giai bai tap ho minh", 0)
        assertEquals("giai bai tap ho minh", bo.roiApp())
        // Quay lai app, chu cu van con hien trong o nhap -> khong ghi lai.
        bo.goChu("giai bai tap ho minh", 100)
        assertNull(bo.roiApp())
    }

    /** Hai cau khac nhau lien tiep: ca hai deu duoc ghi. */
    @Test
    fun haiCauKhacNhauDeuGhi() {
        val bo = BoGoAi()
        bo.goChu("cau hoi thu nhat", 0)
        assertEquals("cau hoi thu nhat", bo.goChu("", 100))
        bo.goChu("cau hoi thu hai", 200)
        assertEquals("cau hoi thu hai", bo.goChu("", 300))
    }

    /** O nhap trong luc chua go gi: khong chot cai gi ca. */
    @Test
    fun oNhapTrongLucDauKhongChotGi() {
        val bo = BoGoAi()
        assertNull(bo.goChu("", 0))
        assertNull(bo.roiApp())
        assertNull(bo.imLau(100_000))
    }
}
