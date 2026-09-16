package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateStore

/**
 * Khong phai test that. Hai cai cong tay de doi chung khi thu tren may ao:
 * mo cong roi thu mo game, dong cong roi thu lai, xem ket qua co khac nhau khong.
 */
@RunWith(AndroidJUnit4::class)
class ManualGate {

    private val gate by lazy {
        GateStore(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun moCong() {
        // Duyet roi bam Bat dau luon, vi cai tay nay dung de thu canh "dang choi".
        val minutes = gate.approve()
        val thuc = gate.start()
        println("MANUAL_GATE: duyet $minutes phut, bat dau duoc $thuc phut, state=${gate.state}")
    }

    /**
     * Gia lam nhu con vua nop bai, de thu man hinh cho duyet va nut huy.
     *
     * Goi nhieu lan la xep nhieu bai vao hang cho, dung de thu canh duyet tung bai:
     *
     *   ...#choDuyet -e ma bai1
     */
    @Test
    fun choDuyet() {
        val ma = InstrumentationRegistry.getArguments().getString("ma")
            ?: "thu${System.currentTimeMillis() % 10_000}"
        gate.markPending(ma, 0L)
        println("MANUAL_GATE: dang cho duyet, ma=$ma, so bai cho=${gate.soBaiDangCho()}, " +
            "state=${gate.state}")
    }

    /** Chi duyet bai cu nhat, khong bam Bat dau: de thu man hinh cho con bam. */
    @Test
    fun chiDuyet() {
        val bai = gate.baiChoCuNhat()
        val minutes = gate.approve(requestId = bai?.id)
        println("MANUAL_GATE: da duyet ${bai?.id} duoc $minutes phut, " +
            "con ${gate.soBaiDangCho()} bai cho, state=${gate.state}")
    }

    @Test
    fun dongCong() {
        gate.endSession(EndReason.PARENT_REVOKED)
        println("MANUAL_GATE: da dong, state=${gate.state}")
    }
}
