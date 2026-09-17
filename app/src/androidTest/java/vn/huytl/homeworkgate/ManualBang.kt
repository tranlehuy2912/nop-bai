package vn.huytl.homeworkgate

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
import vn.huytl.homeworkgate.data.ChatBox
import vn.huytl.homeworkgate.data.ChatFrom
import vn.huytl.homeworkgate.data.DangBai
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.GioiHanApp
import vn.huytl.homeworkgate.data.LuotBaNoi
import vn.huytl.homeworkgate.data.KetQuaCham
import vn.huytl.homeworkgate.data.KhoTinCuaCo
import vn.huytl.homeworkgate.data.KhoaAi
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.NhatKyAi
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.data.Mang
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang
import java.time.LocalDateTime

/**
 * Khong phai test that. Day la cai tay quay cho trang web o tools/web.py: no doc
 * va van moi thu tren may ao roi tra ve JSON.
 *
 * Mot lop duy nhat cho tat ca tinh nang, chu khong moi tinh nang mot lop: trang web
 * chi phai biet mot ten, va them mot viec moi la them mot nhanh trong [viec].
 *
 * CAN THAN, giong [ManualSeed]: lop nay chay trong tien trinh cua bai test chu
 * khong phai tien trinh app, ma EncryptedSharedPreferences khong an toan khi hai
 * tien trinh cung ghi. Nen moi lan GHI xong, trang web tu force-stop roi mo lai app
 * de app doc lai tu dia. Doc thi khong sao.
 *
 * Tham so dai (JSON) truyen bang base64 de khoi phai lo dau nhay cua shell tren may.
 */
@RunWith(AndroidJUnit4::class)
class ManualBang {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val args by lazy { InstrumentationRegistry.getArguments() }
    private val gate by lazy { GateStore(context) }
    private val prefs by lazy { Prefs.get(context) }

    /** Toan bo trang thai may ao, moi muc mot dong. */
    @Test
    fun trangthai() {
        val now = System.currentTimeMillis()

        ra(JSONObject().apply {
            put("k", "cong")
            put("trangThai", gate.state.name)
            put("phutDuocCap", gate.grantedMinutes)
            put("conLaiMs", gate.remainingMs())
            put("dangMo", gate.isOpen())
            put("phutDaDuyetHomNay", gate.phutDaDuyetHomNay(now))
            put("phutConLaiHomNay", gate.phutConLaiHomNay(now))
            put("tranMoiNgay", prefs.tranPhutMoiNgay)
            put("nhanCho", gate.nhanCho)
            put("lyDoDungGanNhat", gate.lastEndReason?.name ?: JSONObject.NULL)
            put("trongGioNgu", gate.trongGioNgu(now))
            put("gioChot", prefs.hardStopMinuteOfDay)
            put("gioDay", prefs.gioDayMinuteOfDay)
            put("conChoNopThem", gate.conChoNopThem())
            put("baiDangCho", JSONArray(gate.baiDangCho().map {
                JSONObject().put("id", it.id).put("luc", it.at)
            }))
        })

        ra(JSONObject().apply {
            put("k", "socai")
            put("goiDaCoHomNay", SoCaiBai.goiDaCoHomNay(context, now))
            put("phutLamThemHomNay", SoCaiBai.phutLamThemHomNay(context, now))
            put("phutDaCongHomNay", SoCaiBai.phutDaCongHomNay(context, now))
            put("loiNhan", SoCaiBai.loiNhan(context, now) ?: JSONObject.NULL)
            put("dangChoSua", JSONArray(SoCaiBai.dangChoSua(context, now).map { c ->
                JSONObject().put("ma", c.ma).put("de", c.de).put("phut", c.phut)
                    .put("nhanXet", c.nhanXet)
            }))
            put("daNop", JSONArray(SoCaiBai.tatCa(context, now).takeLast(40).map { c ->
                JSONObject().put("ma", c.ma).put("de", c.de).put("phut", c.phut)
                    .put("xong", c.xong).put("luc", c.luc)
            }))
        })

        ra(JSONObject().apply {
            put("k", "han")
            put("cac", JSONObject().apply {
                GioiHanApp.tatCa(context).forEach { (goi, phut) ->
                    put(goi, JSONObject()
                        .put("phut", phut)
                        .put("daDungMs", GioiHanApp.daDungMs(context, goi))
                        .put("hetGio", GioiHanApp.hetGio(context, goi)))
                }
            })
            put("danhSachTrang", JSONArray(prefs.allowedPackages))
            put("danhSachDen", JSONArray(prefs.blockedPackages))
            put("appAi", JSONArray(prefs.aiPackages))
        })

        ra(JSONObject().apply {
            put("k", "ai")
            put("soKhoa", KhoaAi.tatCa(context).size)
            put("soKhoaConDung", KhoaAi.soKhoaConDung(context, now))
            put("thuTuDangDung", KhoaAi.thuTuDangDung(context, now))
            put("moTa", KhoaAi.moTa(context, now))
            put("nhatKyHomNay", NhatKyAi.homNay(context).takeLast(1500))
        })

        ra(JSONObject().apply {
            put("k", "tinnhan")
            put("chuaDoc", ChatBox.unread(context))
            put("dangCho", ChatBox.isWaiting(context, now))
            put("khoaVi", ChatBox.blockReason(context, now) ?: JSONObject.NULL)
            put("dong", JSONArray(ChatBox.read(context).takeLast(30).map {
                JSONObject().put("ai", it.from.name).put("chu", it.text).put("luc", it.at)
            }))
            val kho = KhoTinCuaCo(context)
            put("tinCuaCo", JSONArray(kho.danhSach().map {
                JSONObject().put("luc", it.luc).put("noiDung", it.noiDung)
                    .put("daDoc", it.daDoc)
            }))
            put("tinCuaCoChuaDoc", kho.soTinChuaDoc())
            put("baNoiDaChoHomNay", LuotBaNoi.daChoHomNay(context))
        })

        ra(JSONObject().apply {
            put("k", "caidat")
            put("baDangDung", ParentMode.isActive(context))
            put("daCaiDat", prefs.isConfigured)
            put("coToken", prefs.botToken.isNotEmpty())
            put("chatId", prefs.parentChatId)
            put("coPin", prefs.hasPin())
            put("saiPinLienTiep", prefs.saiPinLienTiep)
            put("phutMoiLanDuyet", prefs.grantMinutes)
            put("batManChan", prefs.batManChan)
            put("buoiDuocMoSom", prefs.buoiDuocMoSom)
            put("buoiDaSoan", JSONArray(prefs.buoiDaSoan))
            put("khoaCaiDatHeThong", prefs.lockSystemSettings)
            put("nhipBaoSong", prefs.heartbeatHours)
            put("loiTelegram", prefs.loiTelegram)
            put("nhatKyNgay", DayLog.today(context).takeLast(1500))
        })

        val treo = ViecNha.dangTreo(context)
        ra(JSONObject().apply {
            put("k", "viecnha")
            put("dangKhoa", ViecNha.dangKhoa(context))
            put("chuaXong", ViecNha.keChuaXong(context))
            put("phien", if (treo == null) JSONObject.NULL else JSONObject().apply {
                put("id", treo.id)
                put("nhanLuc", treo.nhanLuc)
                put("xongHet", treo.xongHet)
                put("cac", JSONArray(treo.cac.map { v ->
                    JSONObject().put("ten", v.ten).put("phut", v.phut).put("xong", v.xong)
                }))
            })
        })

        // Kho bai: ngan hang cau hoi nap san, va con da lam toi dau trong do.
        NganHang.napNeuCan(context)
        val tuLuc = now - 365L * 24 * 60 * 60 * 1000
        val kho = KhoBai.get(context)
        ra(JSONObject().apply {
            put("k", "kho")
            put("sach", JSONArray(NganHang.SACH.map { sa ->
                val (xong, tong) = NganHang.tienBo(context, sa.nguon)
                JSONObject().put("nguon", sa.nguon).put("mon", sa.mon)
                    .put("ten", sa.ten).put("daXong", xong).put("tongCau", tong)
            }))
            put("denHenOn", JSONArray(kho.cacCauDenHenOn(tuLuc, now)))
            put("dangChoSua", kho.dangChoSua(tuLuc).size)
            val tb = kho.tienBo(now - 7L * 24 * 60 * 60 * 1000)
            put("tienBo7Ngay", JSONObject()
                .put("soCauDung", tb.soCauDung)
                .put("cauKhoDaGo", tb.cauKhoDaGo)
                .put("soNgayCoBai", tb.soNgayCoBai)
                .put("phutDaKiem", tb.phutDaKiem)
                .put("cauDangChoSua", tb.cauDangChoSua))
        })

        ra(JSONObject().apply {
            put("k", "sudung")
            put("coMang", Mang.co(context))
            val hom = NhatKySuDung.theoApp(context, 0)
            put("soApp", hom.size)
            put("tongMsHomNay", NhatKySuDung.tongMs(context, 0))
            put("cac", JSONArray(hom.take(12).map { a ->
                JSONObject().put("goi", a.goi).put("tongMs", a.tongMs)
                    .put("soDoan", a.cacDoan.size)
            }))
        })

        ra(JSONObject().put("k", "het"))
    }

    /**
     * Van mot thu gi do. Moi viec mot nhanh; tra lai trang thai moi nhat de trang
     * web khoi phai goi hai lan.
     *
     *  -e viec duyet -e phut 30      cap phieu
     *  -e viec batdau                bam Bat dau
     *  -e viec tamdung / dong
     *  -e viec themcho -e ma bai1    xep mot bai vao hang cho duyet
     *  -e viec xoacho                bo het hang doi cho duyet
     *  -e viec xoaluot               xoa so phut da duyet trong ngay
     *  -e viec dathan -e goi com.x -e phut 15
     *  -e viec dunghet -e goi com.x  coi nhu da xem het han hom nay
     *  -e viec xoahan -e goi com.x
     *  -e viec nhantin -e ai CON|BA -e chu "..."
     *  -e viec tincuaco -e chu "..."
     *  -e viec soan -e ma 20260914-CHIEU   danh dau da soan cap
     *  -e viec xoasoan
     *  -e viec xoasocai              xoa so cai bai da nop
     *  -e viec viecnha -e chu "Quét nhà:10:0,Rửa chén:10:0"
     *  -e viec viecnhaxong           ba bam xong het
     *  -e viec xoaviecnha
     *  -e viec napdenhen -e ma 1.3a  nap mot cau da qua han on lai
     *  -e viec bamo -e phut 30       ba cam may (mo cac man cua ba)
     *  -e viec badong
     *  -e viec moisom -e ma <ma buoi>      cho mo som het buoi do
     */
    @Test
    fun viec() {
        val v = args.getString("viec") ?: error("Thieu -e viec")
        val phut = args.getString("phut")?.toIntOrNull()
        val ma = args.getString("ma")
        val goi = args.getString("goi")
        val chu = args.getString("chu")?.let { giaiMa(it) }
        var ketQua: Any? = null

        when (v) {
            "duyet" -> ketQua = gate.approve(wantedMinutes = phut)
            "batdau" -> ketQua = gate.start()
            "tamdung" -> ketQua = gate.pause()
            "dong" -> ketQua = gate.endSession(EndReason.PARENT_REVOKED).name
            "themcho" -> {
                gate.markPending(ma ?: "thu${System.currentTimeMillis() % 10_000}", 0L)
                ketQua = gate.soBaiDangCho()
            }
            "huycho" -> ketQua = gate.huyBaiMoiNhat()?.id
            "xoacho" -> {
                // Bo het hang doi. Can cho phan tu dong thu: mot bai cho sot lai tu
                // muc truoc lam man hinh chinh doi chu ("Con 1 bai cho duyet"), va
                // muc sau bao hong ma khong phai loi cua app.
                while (gate.huyBaiMoiNhat() != null) { /* bo tung cai */ }
                ketQua = gate.soBaiDangCho()
            }
            "xoaluot" -> {
                prefs.raw().edit()
                    .remove("day_key").remove("day_count")
                    .remove("bonus_day").remove("bonus_count")
                    .commit()
                ketQua = gate.phutConLaiHomNay()
            }
            "dathan" -> {
                GioiHanApp.datHan(context, goi!!, phut ?: 15)
                ketQua = GioiHanApp.han(context, goi)
            }
            "dunghet" -> {
                val h = GioiHanApp.han(context, goi!!)
                GioiHanApp.congThem(context, goi, h * 60_000L)
                // congThem ghi bang apply(); goi them mot ham ghi dong bo de day het
                // hang doi xuong dia truoc khi tien trinh test chet.
                GioiHanApp.datHan(context, goi, h)
                ketQua = GioiHanApp.hetGio(context, goi)
            }
            "xoahan" -> {
                GioiHanApp.xoaDaDung(context, goi!!)
                GioiHanApp.datHan(context, goi, GioiHanApp.han(context, goi))
                ketQua = 0
            }
            "nhantin" -> {
                ChatBox.add(
                    context,
                    if (args.getString("ai") == "BA") ChatFrom.BA else ChatFrom.CON,
                    chu ?: ""
                )
                ketQua = ChatBox.read(context).size
            }
            "tincuaco" -> {
                KhoTinCuaCo(context).them(chu ?: "")
                ketQua = KhoTinCuaCo(context).soTinChuaDoc()
            }
            "xoatin" -> {
                ChatBox.xoaHet(context); KhoTinCuaCo(context).xoaHet(); ketQua = 0
            }
            "soan" -> { prefs.danhDauDaSoan(ma!!); ketQua = prefs.buoiDaSoan.size }
            "xoasoan" -> {
                prefs.buoiDaSoan.toList().forEach { prefs.boDanhDau(it) }
                ketQua = prefs.buoiDaSoan.size
            }
            "moisom" -> { prefs.buoiDuocMoSom = ma ?: ""; ketQua = prefs.buoiDuocMoSom }
            "manchan" -> { prefs.batManChan = ma != "0"; ketQua = prefs.batManChan }
            "xoasocai" -> { SoCaiBai.xoaHet(context); ketQua = 0 }
            "xoakhoaai" -> { KhoaAi.xoaDanhDau(context); ketQua = KhoaAi.soKhoaConDung(context) }
            // Tra lai luot cho gio cua ba noi, de thu nhieu lan trong cung mot ngay
            // ma khong phai doi sang hom sau.
            "xoaluotba" -> {
                Prefs.get(context).raw().edit().remove("hopthu_ngay_da_cho").commit()
                ketQua = LuotBaNoi.daChoHomNay(context)
            }
            // Gia lam nhu may ba noi vua giao viec. Ban dung khuon giong het cai
            // ba ghi xuong hop/viecnha, de duong doc cung duoc thu chu khong chi
            // phan luu tru.
            //
            // Khuon chu: "Quét nhà:10:0,Rửa chén:10:1" - ten:phut:daXong.
            "viecnha" -> {
                val danh = chu ?: "Quét nhà:10:0,Rửa chén:10:0"
                val cac = danh.split(',').mapNotNull { muc ->
                    val c = muc.split(':')
                    if (c.size != 3) return@mapNotNull null
                    mapOf(
                        "ten" to c[0].trim(),
                        "phut" to (c[1].trim().toIntOrNull() ?: 10),
                        "xong" to (c[2].trim() == "1")
                    )
                }
                val phien = ViecNha.tuBan(
                    "thu-${System.currentTimeMillis() % 100000}",
                    cac,
                    System.currentTimeMillis()
                ) ?: error("khong doc duoc danh sach viec: $danh")
                ketQua = ViecNha.apDung(context, phien).name
            }
            // Ba bam "da xong" cho het moi viec trong phien dang treo.
            "viecnhaxong" -> {
                val treo = ViecNha.dangTreo(context) ?: error("khong co phien nao dang treo")
                val xong = treo.copy(cac = treo.cac.map { it.copy(xong = true) })
                ketQua = ViecNha.apDung(context, xong).name
            }
            "xoaviecnha" -> { ViecNha.xoa(context); ketQua = ViecNha.dangKhoa(context) }
            /*
             * Nap mot cau DA QUA HEN on lai.
             *
             * Khong dung ManualChonBai#napOnTap: ban do ghi cau lam dung tu HOM QUA,
             * ma hen dau tien la ba ngay (KhoBai.KHOANG_HEN_NGAY), nen chua toi han
             * - no nap canh "da sua xong", khong phai canh "den hen". O day lui han
             * nam ngay de chac chan qua moc dau tien.
             */
            "napdenhen" -> {
                // Xoa so truoc: neu cau nay da tung lam dung o mot muc thu khac thi
                // [SoCaiBai.ghi] bo qua lan ghi moi (cau da xong roi), va moc "lam
                // dung lan cuoi" van la cua lan truoc - chua den hen.
                SoCaiBai.xoaHet(context)
                NganHang.napNeuCan(context)
                val luc = System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000
                val ma = ma ?: "1.3a"
                val sai = CauCham(
                    ma = ma, de = "", cauId = "toan8t1:$ma", mon = "Toán",
                    dung = false, soDong = 4, nhanXet = "Dòng 2 nhân thiếu một thừa số"
                )
                SoCaiBai.ghi(context, listOf(sai), emptyMap(), luc)
                SoCaiBai.ghi(context, listOf(sai.copy(dung = true)), mapOf(ma to 2), luc)
                ketQua = KhoBai.get(context)
                    .cacCauDenHenOn(luc - 86_400_000L).joinToString(", ")
            }
            // Ba dang cam may. Cac man cua ba (thong ke...) nam sau cua nay.
            // ParentMode ghi bang apply(), tuc la ghi khong dong bo. Tien trinh test
            // chet ngay sau khi ham nay tra ve, nen phai ep mot lan ghi dong bo de
            // day het hang doi xuong dia - khong thi dat xong ma app doc lai van
            // thay che do cu. Giong cho ManualGioiHan da vuong.
            "bamo" -> {
                ParentMode.enable(context, phut); epGhi(); ketQua = ParentMode.moTa(context)
            }
            "badong" -> {
                ParentMode.disable(context); epGhi(); ketQua = ParentMode.isActive(context)
            }
            else -> error("Khong biet viec: $v")
        }
        ra(JSONObject().put("k", "xong").put("viec", v)
            .put("ketQua", ketQua ?: JSONObject.NULL))
    }

    /**
     * May tinh luat cong gio: dua vao mot ban cham gia, xem ra bao nhieu phut.
     *
     * Phan nay khong dung toi may ao chut nao ngoai viec chay tren do - no la ham
     * thuan. Nhung chay o day thi con so ra la con so THAT cua app, khong phai ban
     * chep tay trong trang web.
     *
     *   -e cham <base64 cua JSON KetQuaCham>
     *   -e daCongLamThem 0   -e goiDaCo 0   -e bayGio 2026-09-14T20:00
     */
    @Test
    fun conggio() {
        val o = JSONObject(giaiMa(args.getString("cham") ?: error("Thieu -e cham")))
        val cac = mutableListOf<CauCham>()
        val mang = o.optJSONArray("cac") ?: JSONArray()
        for (i in 0 until mang.length()) {
            val c = mang.getJSONObject(i)
            cac += CauCham(
                ma = c.optString("ma", "câu ${i + 1}"),
                de = c.optString("de", c.optString("ma", "câu ${i + 1}")),
                dung = c.optBoolean("dung", true),
                docRo = c.optBoolean("docRo", true),
                dang = runCatching { DangBai.valueOf(c.optString("dang", "CAU_NHO")) }
                    .getOrDefault(DangBai.CAU_NHO),
                trongDanDo = c.optBoolean("trongDanDo", false),
                soDong = c.optInt("soDong", 0)
            )
        }
        val giao = mutableListOf<String>()
        val mangGiao = o.optJSONArray("baiDuocGiao") ?: JSONArray()
        for (i in 0 until mangGiao.length()) giao += mangGiao.getString(i)

        val ket = KetQuaCham(
            mon = o.optString("mon", ""),
            cac = cac,
            ngayDanDo = o.optString("ngayDanDo").ifBlank { null },
            lamHetDanDo = o.optBoolean("lamHetDanDo", false),
            baiDuocGiao = giao
        )
        val bayGio = args.getString("bayGio")?.let {
            LocalDateTime.parse(it.replace(" ", "T").let { s -> if (s.length == 16) "$s:00" else s })
        } ?: LocalDateTime.now()

        val b = LuatCongGio.tinh(
            ket,
            daCongLamThemHomNay = args.getString("daCongLamThem")?.toIntOrNull() ?: 0,
            bayGio = bayGio,
            goiDaCoHomNay = args.getString("goiDaCo") == "1"
        )
        ra(JSONObject().apply {
            put("k", "conggio")
            put("phut", b.phut)
            put("daTinhGoi", b.daTinhGoi)
            put("canBaHuyXem", b.canBaHuyXem)
            put("dong", JSONArray(b.dong))
            put("phutCua", JSONObject(b.phutCua as Map<*, *>))
            put("trongGoi", JSONArray(b.trongGoi.map { it.ma }))
            put("bayGio", bayGio.toString())
            put("tranLamThem", LuatCongGio.TRAN_LAM_THEM)
            put("tronGoi", LuatCongGio.PHUT_TRON_GOI_DAN_DO)
            put("toiDaMoiNgay", LuatCongGio.TOI_DA_MOI_NGAY)
        })
    }

    /**
     * Ep moi lan ghi dang cho xuong dia.
     *
     * commit() tren cung mot SharedPreferences cho het hang doi cua apply() chay
     * xong roi moi tra ve, nen mot lan ghi dong bo bat ky la du.
     */
    private fun epGhi() {
        prefs.raw().edit().putLong("manualbang_ep_ghi", System.currentTimeMillis()).commit()
    }

    private fun giaiMa(s: String): String =
        runCatching { String(Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP)) }
            .getOrDefault(s)

    private fun ra(o: JSONObject) = println("BANGJSON: $o")
}
