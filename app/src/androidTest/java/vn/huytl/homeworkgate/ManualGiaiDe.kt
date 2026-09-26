package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.GiaiDe
import vn.huytl.homeworkgate.kho.BoThe
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import vn.huytl.homeworkgate.kho.PhanHoc

/**
 * Khong phai test that. Cong tay de thu man Giai de tren may ao, xem [GiaiDe].
 *
 *   ...#moc     dat moc hoc toi: Toan Bai 6, Hoa Bai 6, Li Bai 15, Sinh chua hoc.
 *               Doi so bai: -e toan 9 -e hoa 4 -e li 0 -e sinh 31 (0 la chua hoc).
 *   ...#rade    mo cac de den luc ngay bay gio, in ra logcat dong MANUAL_GIAIDE
 *   ...#xoa     xoa moi de trong 60 ngay qua, ca cac lan lam mang ma de
 *
 * Khong goi mang. Moc hoc toi ghi thang vao prefs, KHONG ghi nhat ky: chay tren may
 * ao that cua nha thi dong nhat ky do se sang Bang dieu khien.
 */
@RunWith(AndroidJUnit4::class)
class ManualGiaiDe {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val args by lazy { InstrumentationRegistry.getArguments() }

    @Test
    fun moc() {
        NganHang.napNeuCan(context)
        BoThe.napNeuCan(context)
        val mac = mapOf("toan8ct" to 6, "khtn8hoa" to 6, "khtn8li" to 15, "khtn8sinh" to 0)
        val khoa = mapOf("toan8ct" to "toan", "khtn8hoa" to "hoa", "khtn8li" to "li", "khtn8sinh" to "sinh")
        mac.forEach { (ma, macDinh) ->
            val so = args.getString(khoa.getValue(ma))?.toIntOrNull() ?: macDinh
            val phan = PhanHoc.theoMa(ma) ?: return@forEach
            val ten = if (so == 0) HocToi.CHUA_HOC_BAI_NAO
            else PhanHoc.cacBai(context, phan).firstOrNull { PhanHoc.soBai(it) == so } ?: return@forEach
            HocToi.ghiBai(context, ma, ten)
            println("MANUAL_GIAIDE: moc $ma = ${ten.ifEmpty { "chua hoc" }}")
        }
    }

    @Test
    fun rade() {
        NganHang.napNeuCan(context)
        val moi = GiaiDe.taoNeuCan(context)
        moi.forEach { println("MANUAL_GIAIDE: moi ${it.id} ${GiaiDe.tenDe(it)} (${it.ten}) ${it.cauIds}") }
        GiaiDe.dangMo(context).forEach { println("MANUAL_GIAIDE: dang mo ${it.id} ${GiaiDe.tenDe(it)}") }
    }

    /** In tinh trang cac de va cac lan lam mang ma de, de xem may cham, ghi so ra sao. */
    @Test
    fun xem() {
        val kho = KhoBai.get(context)
        val gate = vn.huytl.homeworkgate.data.GateStore(context)
        println("MANUAL_GIAIDE: cong ${gate.state} phieu ${gate.grantedMinutes} da duyet ${gate.phutDaDuyetHomNay()}")
        kho.cacDeTu(System.currentTimeMillis() - 60L * 24 * 60 * 60_000L).forEach { de ->
            println("MANUAL_GIAIDE: de ${de.id} ${GiaiDe.tenDe(de)} bat ${de.batDau} nop ${de.nopLuc} gui ${de.guiLuc} tn ${de.tnDung} tl ${de.tlDung}")
            de.cauIds.forEach { id ->
                kho.lichSuCua(id).filter { it.deId == de.id }.forEach {
                    println("MANUAL_GIAIDE:   $id dung=${it.dung} phut=${it.phut} ${it.baiLam}")
                }
            }
        }
    }

    @Test
    fun xoa() {
        val kho = KhoBai.get(context)
        kho.cacDeTu(System.currentTimeMillis() - 60L * 24 * 60 * 60_000L).forEach {
            kho.xoaDe(it.id)
            println("MANUAL_GIAIDE: xoa ${it.id}")
        }
    }
}
