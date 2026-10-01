package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CongSang
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.dongbo.ThiHanhLenh
import java.util.Calendar

/**
 * Lenh XUCAU co cau Ba Huy bam "Chụp lại": loi nhan cuoi cung cua con phai la loi nho chup lai.
 *
 * Truoc 1/10/2026 ThiHanhLenh.xuCau ghi loi nhan do ngay luc nhan lenh, roi ApprovalService
 * cham xong sau va ghi de len. Loi nam o thu tu giua hai ben, nen bo test nay KHAC
 * ChamBaiLenhTest: no chay that nhanh cham trong service va doi service cham xong.
 *
 * Service cham xong thi gui tin Telegram. Truoc khi chay, chat id doi tam thanh 1 (bot khong co
 * cuoc tro chuyen nao o do, Telegram tu choi), chay xong tra lai. Chat id 0 thi khong duoc: may
 * coi nhu chua cai dat va service tu dung, khong cham gi ca. Chi chay tren may ao.
 *
 * Don dep: tra chat id va gio ngu, bo bai thu khoi hang cho, dong phieu gio, xoa so cai va loi
 * nhan. Ban cham ghi xuong Firestore bang update() nen bai thu khong co document thi khong ghi gi.
 */
@RunWith(AndroidJUnit4::class)
class XuCauLenhTest {

    private lateinit var context: Context
    private lateinit var prefs: Prefs
    private val id = "thu-xu-cau"
    private var daDoi = false
    private var chatCu = 0L
    private var nguCu = 0
    private var dayCu = 0

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = Prefs.get(context)
        assumeTrue("Chưa nạp cấu hình, chạy tools/emu.sh seed trước", prefs.isConfigured)
        chatCu = prefs.parentChatId
        nguCu = prefs.hardStopMinuteOfDay
        dayCu = prefs.gioDayMinuteOfDay
        daDoi = true
        prefs.parentChatId = 1L
        // Ngoai gio ngu: cau dung cong ngay va bai roi hang cho, do la dau hieu service da cham xong.
        val c = Calendar.getInstance()
        val bayGio = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        prefs.hardStopMinuteOfDay = (bayGio + 180) % (24 * 60)
        prefs.gioDayMinuteOfDay = (bayGio + 240) % (24 * 60)
        SoCaiBai.xoaHet(context)
        CongSang.xoaHet(context)
        GateStore(context).boBaiCho(id)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
    }

    @After
    fun tearDown() {
        if (!daDoi) return
        // Tin ket qua cham di toi chat 1 du tra chat id som: xuLyBanCham doc chat id mot lan luc bat
        // dau. Van doi mot nhip, phong cac tin khac service gui ngay sau do.
        Thread.sleep(3_000L)
        prefs.parentChatId = chatCu
        prefs.hardStopMinuteOfDay = nguCu
        prefs.gioDayMinuteOfDay = dayCu
        GateStore(context).boBaiCho(id)
        GateStore(context).endSession(EndReason.PARENT_REVOKED)
        SoCaiBai.xoaHet(context)
        CongSang.xoaHet(context)
    }

    @Test
    fun loi_nho_chup_lai_la_loi_nhan_cuoi_cung_sau_khi_service_cham_xong() {
        val gate = GateStore(context)
        gate.markPending(id, 0L)
        // Dung kieu Bang dieu khien gui: map co "cac", moi cau mot map, so la Long nhu Firestore.
        val giaTri: Map<String, Any> = mapOf(
            "cac" to listOf(
                mapOf("ma" to "1", "de" to "Tính 2 + 3.", "dung" to true, "chac" to true, "soDong" to 1L),
                mapOf("ma" to "2", "de" to "Tính 4 + 5.", "dung" to false, "chac" to true, "chupLai" to true)
            )
        )

        val tra = ThiHanhLenh.xuCau(context, gate, id, giaTri)
        println("XUCAU: $tra")

        // Cau 1 dung, mot dong: service tu duyet 1 phut va bai roi hang cho. Loi nhan ghi sau
        // doan do vai mili giay, nen doi them mot nhip.
        val het = System.currentTimeMillis() + 20_000L
        while (GateStore(context).baiDangCho().any { it.id == id } && System.currentTimeMillis() < het) {
            Thread.sleep(200L)
        }
        Thread.sleep(1_500L)
        val loi = SoCaiBai.loiNhan(context).orEmpty()
        println("XUCAU_LOI_NHAN: $loi")

        assertTrue("service chưa chấm xong", GateStore(context).baiDangCho().none { it.id == id })
        assertTrue(loi, loi.startsWith("Ba Huy nhờ chụp lại câu 2 cho rõ rồi nộp lại."))
        assertTrue(loi, loi.contains("Được thêm 1 phút."))
        assertFalse(SoCaiBai.loiNhanLaTinVui(context))
    }
}
