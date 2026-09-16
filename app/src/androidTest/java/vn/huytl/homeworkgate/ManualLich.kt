package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.BuoiHoc
import vn.huytl.homeworkgate.data.NgayNghi
import vn.huytl.homeworkgate.data.ThoiKhoaBieu
import vn.huytl.homeworkgate.data.TinhLoiNhac
import java.util.Calendar

/**
 * Khong phai test that. Chay ca tuan trong vai giay thay vi ngoi doi bay ngay.
 *
 * [TinhLoiNhac.tinh] nhan dong ho lam tham so chu khong tu doc gio may, nen o day
 * chi viec quay tay no qua tung phut cua tung ngay roi in ra cho nao doi. Nhung gi
 * in ra la dung nhung gi tablet se hien vao dung phut do - cung mot ham, cung mot
 * thoi khoa bieu.
 *
 * Cai nay tra loi cau "app co goi dung luc khong". Con "man chan co che that khong,
 * chuong co keu that khong" thi phai xem bang mat, luc do dung tools/emu.sh gio de
 * van dong ho may ao toi moc muon xem.
 *
 *   tools/emu.sh tuan                       # bay ngay tu hom nay
 *   tools/emu.sh tuan 2026-09-14 7          # bay ngay tu thu hai 14/9
 *
 * Hoac goi thang:
 *
 *   adb shell am instrument -w -e class vn.huytl.homeworkgate.ManualLich#cathang \
 *     -e tu 2027-01-28 -e ngay 14 \
 *     vn.huytl.homeworkgate.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ManualLich {

    private val args by lazy { InstrumentationRegistry.getArguments() }

    /**
     * Cung mot thu nhu [cathang], nhung nha ra JSON cho trang web o tools/web.py.
     *
     * Moi ban ghi mot dong rieng chu khong gom ca ngay vao mot cuc: println di ra
     * logcat, ma logcat cat cut tin nhan dai qua chung bon nghin byte. Mot dong
     * mot moc thi khong bao gio cham nguong do.
     */
    @Test
    fun json() {
        val batDau = docNgay(args.getString("tu"))
        val soNgay = args.getString("ngay")?.toIntOrNull() ?: 7
        val batManChan = args.getString("chan") != "0"
        val daSoan = if (args.getString("dasoan") == "tatca") {
            moiMaBuoiTrong(batDau, soNgay)
        } else {
            emptySet()
        }

        for (lui in 0 until soNgay) {
            val ngay = (batDau.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, lui) }
            val khoaNgay = "%04d-%02d-%02d".format(
                ngay.get(Calendar.YEAR), ngay.get(Calendar.MONTH) + 1, ngay.get(Calendar.DAY_OF_MONTH)
            )
            val nghi = NgayNghi.tenKyNghi(ngay)

            ra(JSONObject().apply {
                put("k", "ngay")
                put("ngay", khoaNgay)
                put("thu", ThoiKhoaBieu.tenThu(ngay.get(Calendar.DAY_OF_WEEK)))
                put("nghi", nghi ?: JSONObject.NULL)
            })

            val cacBuoi = if (nghi != null) emptyList() else
                ThoiKhoaBieu.buoiHocCua(ngay.get(Calendar.DAY_OF_WEEK))
            for (b in cacBuoi) {
                ra(JSONObject().apply {
                    put("k", "buoi")
                    put("ngay", khoaNgay)
                    put("buoi", b.buoi.name)
                    put("ma", TinhLoiNhac.maBuoi(ngay, b))
                    put("vao", b.phutVaoHoc)
                    put("tan", b.phutTanHoc)
                    put("buong", ThoiKhoaBieu.phutBuongMay(b))
                    put("theDuc", b.coTheDuc)
                    put("mon", JSONArray(b.monCanSoan))
                    put("tiet", JSONObject().apply {
                        b.monTheoTiet.forEach { (tiet, mon) ->
                            put(tiet.toString(), JSONObject().apply {
                                put("mon", mon)
                                put("gio", ThoiKhoaBieu.gioTiet(b.buoi, tiet))
                            })
                        }
                    })
                })
            }

            var truoc: String? = null
            for (phut in 0 until 24 * 60) {
                val luc = (ngay.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, phut / 60)
                    set(Calendar.MINUTE, phut % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val nhac = TinhLoiNhac.tinh(luc, daSoan, batManChan)
                val dau = if (nhac == null) "-" else "${nhac.loai}|${nhac.maBuoi}|${nhac.gap}"
                if (dau == truoc) continue
                truoc = dau
                ra(JSONObject().apply {
                    put("k", "moc")
                    put("ngay", khoaNgay)
                    put("phut", phut)
                    put("loai", nhac?.loai?.name ?: "RANH")
                    put("gap", nhac?.gap ?: false)
                    put("tieuDe", nhac?.tieuDe ?: "")
                    put("chiTiet", nhac?.chiTiet ?: "")
                    put("maBuoi", nhac?.maBuoi ?: JSONObject.NULL)
                })
            }
        }
        ra(JSONObject().put("k", "het"))
    }

    /** Mot moc duy nhat, dang JSON, cho trang web hoi khi bam vao mot phut. */
    @Test
    fun jsonluc() {
        val now = docGio(args.getString("luc") ?: error("Thieu -e luc"))
        val nhac = TinhLoiNhac.tinh(
            now,
            if (args.getString("dasoan") == "tatca") moiMaBuoiTrong(now, 1) else emptySet(),
            args.getString("chan") != "0",
            args.getString("mosom") ?: ""
        )
        val ke = TinhLoiNhac.buoiKeTiep(now)
        ra(JSONObject().apply {
            put("k", "luc")
            put("thu", ThoiKhoaBieu.tenThu(now.get(Calendar.DAY_OF_WEEK)))
            put("loai", nhac?.loai?.name ?: "RANH")
            put("gap", nhac?.gap ?: false)
            put("tieuDe", nhac?.tieuDe ?: "")
            put("chiTiet", nhac?.chiTiet ?: "")
            put("giayConLai", nhac?.giayConLai ?: -1)
            put("phutHetChan", nhac?.phutHetChan ?: -1)
            put("mon", JSONArray(nhac?.buoi?.monCanSoan ?: emptyList<String>()))
            put("theDuc", nhac?.buoi?.coTheDuc ?: false)
            put("keTiep", if (ke == null) JSONObject.NULL else JSONObject().apply {
                put("moTa", TinhLoiNhac.moTaBuoi(ke.second))
                put("ma", TinhLoiNhac.maBuoi(ke.first, ke.second))
                put("vao", ke.second.phutVaoHoc)
                put("buong", ThoiKhoaBieu.phutBuongMay(ke.second))
            })
        })
    }

    private fun ra(o: JSONObject) = println("LICHJSON: $o")

    /**
     * Ca tuan (hay ca thang) mot luot: moi ngay mot bang thoi khoa bieu, roi tung
     * moc app len tieng trong ngay do.
     *
     *  -e tu 2026-09-14   ngay bat dau, mac dinh hom nay
     *  -e ngay 7          chay may ngay, mac dinh 7
     *  -e dasoan tatca    coi nhu da soan cap het, de nhin ro phan chan va dem gio
     *  -e chan 0          tat man chan, xem app con lai gi
     */
    @Test
    fun cathang() {
        val batDau = docNgay(args.getString("tu"))
        val soNgay = args.getString("ngay")?.toIntOrNull() ?: 7
        val batManChan = args.getString("chan") != "0"
        val daSoan = if (args.getString("dasoan") == "tatca") {
            moiMaBuoiTrong(batDau, soNgay)
        } else {
            emptySet()
        }

        ke("THOI KHOA BIEU ${soNgay} NGAY TU ${ngayGon(batDau)}" +
            (if (daSoan.isEmpty()) "" else " (coi nhu da soan cap het)") +
            (if (batManChan) "" else " (man chan TAT)"))

        for (lui in 0 until soNgay) {
            val ngay = (batDau.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, lui) }
            inMotNgay(ngay, daSoan, batManChan)
        }
        println()
        println("MANUAL_LICH: xong ${soNgay} ngay. Muon nhin tan mat mot moc nao do thi:")
        println("MANUAL_LICH:   tools/emu.sh gio 2026-09-14 12:05")
    }

    /**
     * Mot moc duy nhat, khi dang soi mot cho nghi la sai.
     *
     *   ...#motluc -e luc 2026-09-14T12:10
     */
    @Test
    fun motluc() {
        val chu = args.getString("luc") ?: error("Thieu -e luc 2026-09-14T12:10")
        val now = docGio(chu)
        val nhac = TinhLoiNhac.tinh(
            now,
            if (args.getString("dasoan") == "tatca") moiMaBuoiTrong(now, 1) else emptySet(),
            args.getString("chan") != "0",
            args.getString("mosom") ?: ""
        )
        ke("LUC ${ngayGon(now)} ${gio(now)} (${ThoiKhoaBieu.tenThu(now.get(Calendar.DAY_OF_WEEK))})")
        if (nhac == null) {
            println("MANUAL_LICH: khong nhac gi - may mo binh thuong")
        } else {
            println("MANUAL_LICH: ${nhac.loai}${if (nhac.gap) " (GAP)" else ""}")
            println("MANUAL_LICH:   ${nhac.tieuDe}")
            println("MANUAL_LICH:   ${nhac.chiTiet}")
            nhac.buoi?.let { println("MANUAL_LICH:   mon can soan: ${monCua(it)}") }
            if (nhac.phutHetChan >= 0) {
                println("MANUAL_LICH:   chan toi ${TinhLoiNhac.gioPhut(nhac.phutHetChan)}")
            }
        }
        val ke = TinhLoiNhac.buoiKeTiep(now)
        if (ke == null) {
            println("MANUAL_LICH: khong con buoi hoc nao trong hai tuan toi")
        } else {
            println("MANUAL_LICH: buoi ke tiep = ${TinhLoiNhac.maBuoi(ke.first, ke.second)} " +
                "(${TinhLoiNhac.moTaBuoi(ke.second)}, vao hoc ${TinhLoiNhac.gioPhut(ke.second.phutVaoHoc)})")
        }
    }

    // --- in mot ngay ---------------------------------------------------------

    private fun inMotNgay(ngay: Calendar, daSoan: Set<String>, batManChan: Boolean) {
        val nghi = NgayNghi.tenKyNghi(ngay)
        ke("${ThoiKhoaBieu.tenThu(ngay.get(Calendar.DAY_OF_WEEK)).uppercase()} " +
            ngayGon(ngay) + if (nghi != null) "  [$nghi]" else "")

        val cacBuoi = if (nghi != null) emptyList() else
            ThoiKhoaBieu.buoiHocCua(ngay.get(Calendar.DAY_OF_WEEK))
        if (cacBuoi.isEmpty()) {
            println("MANUAL_LICH:   khong co buoi hoc")
        }
        for (b in cacBuoi) {
            println(
                "MANUAL_LICH:   ${b.buoi.name.lowercase()}: tiet ${b.tietDau}-${b.tietCuoi}, " +
                    "hoc ${TinhLoiNhac.gioPhut(b.phutVaoHoc)}-${TinhLoiNhac.gioPhut(b.phutTanHoc)}, " +
                    "buong may ${TinhLoiNhac.gioPhut(ThoiKhoaBieu.phutBuongMay(b))}"
            )
            println("MANUAL_LICH:     mon: ${monCua(b)}")
        }

        // Quay tay dong ho qua tung phut cua ngay, chi in nhung phut app doi giong.
        var truoc: String? = null
        for (phut in 0 until 24 * 60) {
            val luc = (ngay.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, phut / 60)
                set(Calendar.MINUTE, phut % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nhac = TinhLoiNhac.tinh(luc, daSoan, batManChan)
            // Tieu de cua the "sap di hoc" dem nguoc tung phut, lay no lam moc doi
            // thi in ra sau muoi dong dem nguoc. Chi coi la doi khi doi LOAI hay
            // doi BUOI hay bat dau gap.
            val dau = if (nhac == null) "-" else
                "${nhac.loai}|${nhac.maBuoi}|${nhac.gap}"
            if (dau != truoc) {
                truoc = dau
                if (nhac == null) {
                    println("MANUAL_LICH:   ${TinhLoiNhac.gioPhut(phut)}  (thoi nhac)")
                } else {
                    println(
                        "MANUAL_LICH:   ${TinhLoiNhac.gioPhut(phut)}  " +
                            "${nhac.loai}${if (nhac.gap) " GAP" else ""}: ${nhac.tieuDe}" +
                            " | ${nhac.chiTiet}"
                    )
                }
            }
        }
    }

    private fun monCua(b: BuoiHoc): String {
        val mon = b.monCanSoan
        val ten = if (mon.isEmpty()) "khong phai mang vo" else mon.joinToString(", ")
        return if (b.coTheDuc) "$ten (+ do the duc)" else ten
    }

    /** Ma cua moi buoi hoc trong khoang, de gia lam nhu da soan cap het. */
    private fun moiMaBuoiTrong(batDau: Calendar, soNgay: Int): Set<String> = buildSet {
        for (lui in 0 until soNgay + 1) {
            val ngay = (batDau.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, lui) }
            ThoiKhoaBieu.buoiHocCua(ngay.get(Calendar.DAY_OF_WEEK))
                .forEach { add(TinhLoiNhac.maBuoi(ngay, it)) }
        }
    }

    // --- doc tham so ---------------------------------------------------------

    /** "2026-09-14", hay bo trong thi lay hom nay. */
    private fun docNgay(chu: String?): Calendar {
        if (chu.isNullOrBlank()) {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
        }
        val p = chu.split("-")
        return NgayNghi.calendarCua(p[0].toInt(), p[1].toInt(), p[2].toInt())
    }

    /** "2026-09-14T12:10" hay "2026-09-14 12:10". */
    private fun docGio(chu: String): Calendar {
        val (ngay, gio) = chu.trim().split("T", " ").let { it[0] to (it.getOrNull(1) ?: "00:00") }
        val d = ngay.split("-")
        val g = gio.split(":")
        return NgayNghi.calendarCua(
            d[0].toInt(), d[1].toInt(), d[2].toInt(),
            g[0].toInt(), g.getOrNull(1)?.toInt() ?: 0
        )
    }

    private fun ngayGon(c: Calendar) = "%02d/%02d/%d".format(
        c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)
    )

    private fun gio(c: Calendar) =
        "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))

    private fun ke(tieuDe: String) {
        println()
        println("MANUAL_LICH: ===== $tieuDe =====")
    }
}
