package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.ai.AiChamBai
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.telegram.ApprovalService
import java.io.File

/**
 * Khong phai test that. Cai cong tay de cham thu bang anh bai tap that.
 *
 * Anh nam trong androidTest/assets: hai trang bai 2.26 (a,b,c dung - d sai) va mot
 * trang bai 2.27 (ca bon cau sai), kem trang sach in.
 *
 *   ...#chamThu               cham 2.26, in ket qua ra logcat
 *   ...#chamThu -e anh sach.jpg,bai227.jpg
 *   ...#quaService            di dung duong that: nho service cham roi nhan Telegram
 *
 * CAN THAN: cac ham nay goi API that va ton han muc khoa AI.
 */
@RunWith(AndroidJUnit4::class)
class ManualCham {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val args by lazy { InstrumentationRegistry.getArguments() }

    /** Chep anh tu assets cua bo test sang cache cua app that. */
    private fun chepAnh(ten: List<String>): List<File> {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        return ten.map { t ->
            val f = File(context.cacheDir, "thu_$t")
            assets.open(t).use { vao -> f.outputStream().use { ra -> vao.copyTo(ra) } }
            f
        }
    }

    @Test
    fun chamThu() {
        val ten = (args.getString("anh") ?: "sach.jpg,giai_ab.jpg,giai_cd.jpg").split(",")
        val anh = chepAnh(ten)
        val giai = anh.filterNot { it.name.contains("sach") }

        val t0 = System.currentTimeMillis()
        val ket = AiChamBai.cham(context, anh, giai)
        val giay = (System.currentTimeMillis() - t0) / 1000.0

        if (ket.ket == null) {
            println("MANUAL_CHAM: khong cham duoc sau ${giay}s -> ${ket.loi}")
            return
        }
        val bang = LuatCongGio.tinh(
            ket.ket!!,
            daCongLamThemHomNay = SoCaiBai.phutLamThemHomNay(context),
            goiDaCoHomNay = SoCaiBai.goiDaCoHomNay(context)
        )
        println("MANUAL_CHAM: ${giay}s | mon=${ket.ket!!.mon} | ${bang.phut} phut")
        ket.ket!!.cac.forEach {
            println("MANUAL_CHAM:   ${it.ma} ${if (it.dung) "DUNG" else "SAI "} " +
                "${it.soDong}d ${if (it.docRo) "" else "(doc khong ro)"} -> ${it.ketQua}")
            if (!it.dung) println("MANUAL_CHAM:      ${it.nhanXet}")
        }
        bang.dong.forEach { println("MANUAL_CHAM:   • $it") }
        anh.forEach { it.delete() }
    }

    /**
     * Di dung duong that: cham -> nhu con da soat xong -> service gui va cap gio.
     *
     * Tu ban co man soat bai, phan cham chay trong man hinh cua con chu khong trong
     * service nua, nen o day cung phai cham truoc roi moi dua ban cham sang service -
     * dung nhu [vn.huytl.homeworkgate.ui.SoatBaiActivity] lam.
     */
    @Test
    fun quaService() {
        val ten = (args.getString("anh") ?: "sach.jpg,giai_ab.jpg,giai_cd.jpg").split(",")
        val anh = chepAnh(ten)
        val nhom = chiaNhom(anh)
        val giai = nhom[vn.huytl.homeworkgate.data.CaptureStage.BAI_GIAI].orEmpty()
        val ket = AiChamBai.cham(context, anh, giai.ifEmpty { anh })
        println("MANUAL_CHAM: cham xong ${ket.ket?.cac?.size ?: -1} cau, loi=${ket.loi}")
        ApprovalService.guiDaSoat(context, nhom, null, ket.ket)
        println("MANUAL_CHAM: da nho service gui ${anh.size} anh")
        // Nam cho o day cho service lam xong. Bo test chay CHUNG tien trinh voi app,
        // ma Android force-stop ca goi ngay khi bo test ket thuc - khong cho thi
        // service bi giet giua chung ("Bringing down service while still waiting").
        Thread.sleep(40_000)
    }

    /** In so cai va trang thai cong ra logcat, de doi chung sau moi lan nop. */
    @Test
    fun inSo() {
        val gate = vn.huytl.homeworkgate.data.GateStore(context)
        println("MANUAL_SO: state=${gate.state} giu=${gate.grantedMinutes} phut, " +
            "hom nay da duyet ${gate.phutDaDuyetHomNay()} phut, " +
            "con ${gate.phutConLaiHomNay()} phut")
        println("MANUAL_SO: da cong hom nay (so cai) = ${SoCaiBai.phutDaCongHomNay(context)} phut")
        val khoa = vn.huytl.homeworkgate.data.KhoaAi.tatCa(context)
        println("MANUAL_SO: khoa AI = ${khoa.size} cai: " +
            khoa.joinToString { vn.huytl.homeworkgate.data.KhoaAi.rutGon(it) })
        println("MANUAL_SO: token bot=${if (Prefs.get(context).botToken.isEmpty()) "TRONG" else "co"}, " +
            "chatid=${Prefs.get(context).parentChatId}, PIN=${if (Prefs.get(context).hasPin()) "co" else "chua"}, " +
            "tran=${Prefs.get(context).tranPhutMoiNgay} phut")
        SoCaiBai.tatCa(context).forEach {
            println("MANUAL_SO:   ${it.ma} ${if (it.xong) "XONG ${it.phut}p" else "CHO SUA"} | ${it.de}")
        }
    }

    /**
     * Tat quan tri thiet bi de go duoc app tren may ao.
     *
     * Tren may that viec nay lam bang nut "Cho phep go app" trong trang cau hinh.
     * adb khong lam duoc: he thong khong cho go mot admin khong phai admin thu.
     */
    @Test
    fun tatQuanTri() {
        val dpm = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
        dpm.removeActiveAdmin(vn.huytl.homeworkgate.guard.Permissions.adminComponent(context))
        println("MANUAL_CHAM: da tat quan tri thiet bi")
    }

    /** Gia lam nhu het sach khoa AI, de thu duong "AI khong cham duoc". */
    @Test
    fun tatKhoaAi() {
        Prefs.get(context).aiKeys = emptyList()
        println("MANUAL_CHAM: da go het khoa AI")
    }

    /** Nap lai chum khoa mac dinh. */
    @Test
    fun batKhoaAi() {
        Prefs.get(context).aiKeys = vn.huytl.homeworkgate.data.Defaults.AI_KEYS
        vn.huytl.homeworkgate.data.KhoaAi.xoaDanhDau(context)
        println("MANUAL_CHAM: da nap lai ${Prefs.get(context).aiKeys.size} khoa")
    }

    /**
     * Di het duong that: gui anh len Telegram nhu luc con bam Nop bai, roi cham.
     *
     * Dung de nhin bang mat trong Telegram: chum anh bai lam, tin nop bai kem hai
     * nut duyet, va ngay duoi la ket qua cham tra loi vao dung tin do.
     */
    @Test
    fun nopThatRoiCham() {
        val ten = (args.getString("anh") ?: "sach.jpg,giai_ab.jpg,giai_cd.jpg").split(",")
        val anh = chepAnh(ten)
        val nhom = chiaNhom(anh)
        val giai = nhom[vn.huytl.homeworkgate.data.CaptureStage.BAI_GIAI].orEmpty()

        // Gio mot minh guiDaSoat lam ca hai: gui anh len Telegram roi cap gio. Truoc
        // day phai goi HomeworkSender roi goi service cham, hai lan.
        val ket = AiChamBai.cham(context, anh, giai.ifEmpty { anh })
        ApprovalService.guiDaSoat(context, nhom, null, ket.ket)
        Thread.sleep(45_000)
    }

    /**
     * Mo man soat bai voi anh that, de nhin bang mat.
     *
     * Chay trong nen roi chup man hinh: bo test phai nam cho, vi Android force-stop
     * ca goi ngay khi bo test ket thuc, ma man soat thi song trong chinh goi do.
     */
    @Test
    fun moManSoat() {
        val ten = (args.getString("anh") ?: "sach.jpg,giai_ab.jpg,giai_cd.jpg").split(",")
        val anh = chepAnh(ten)
        context.startActivity(
            vn.huytl.homeworkgate.ui.SoatBaiActivity.moTu(context, chiaNhom(anh), null)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        println("MANUAL_CHAM: da mo man soat, nam cho 90 giay")
        Thread.sleep(90_000)
    }

    /** Chia xap anh thu thanh nhom de bai / bai giai theo ten file. */
    private fun chiaNhom(anh: List<File>) = mapOf(
        vn.huytl.homeworkgate.data.CaptureStage.DE_BAI to anh.filter { it.name.contains("sach") },
        vn.huytl.homeworkgate.data.CaptureStage.BAI_GIAI to
            anh.filterNot { it.name.contains("sach") }
    ).filterValues { it.isNotEmpty() }

    @Test
    fun xoaSoCai() {
        SoCaiBai.xoaHet(context)
        println("MANUAL_CHAM: da xoa so cai")
    }
}
