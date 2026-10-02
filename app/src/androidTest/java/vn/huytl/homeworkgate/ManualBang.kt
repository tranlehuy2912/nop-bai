package vn.huytl.homeworkgate

import android.os.SystemClock
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.CauCham
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
import vn.huytl.homeworkgate.data.LamTrenMay
import vn.huytl.homeworkgate.data.LuatCongGio
import vn.huytl.homeworkgate.data.LuatGhep
import vn.huytl.homeworkgate.data.NhatKyAi
import vn.huytl.homeworkgate.data.NhatKySuDung
import vn.huytl.homeworkgate.data.Prefs
import vn.huytl.homeworkgate.data.SoCaiBai
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.data.NhacBai
import vn.huytl.homeworkgate.data.VoDanDo
import vn.huytl.homeworkgate.guard.ParentMode
import vn.huytl.homeworkgate.data.Mang
import vn.huytl.homeworkgate.kho.HocToi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.NganHang

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
            // Tu 29/9/2026 khong con tran chung trong prefs: tran ngay la tong cac tran
            // rieng, chi de hien. Giu ten khoa cu vi trang web van doc "tranMoiNgay".
            put("tranMoiNgay", LuatCongGio.TRAN_NGAY)
            put("quyGio", prefs.quyGio)
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
            put("hetTranAnhHomNay", SoCaiBai.hetTranAnhHomNay(context, now))
            // Trang web lay so nay dien vao o "da cong" cua may tinh cong gio ([conggio]).
            // Tu 29/9/2026 o do la so phut duong chup anh da cong (tran 45), nen khoa cu
            // mang so cua duong chup anh. Ba so moi ghi rieng tung phan.
            put("phutLamThemHomNay", SoCaiBai.phutAnhHomNay(context, now))
            put("phutAnhHomNay", SoCaiBai.phutAnhHomNay(context, now))
            put("phutTrenMayHomNay", SoCaiBai.phutTrenMayHomNay(context, now))
            put("phutOnHomNay", SoCaiBai.phutOnHomNay(context, now))
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
            put("phutHomNay", ViecNha.phutHomNay(context, now))
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

        // Tu 30/9/2026: moi ngay mot trang o NhacBai.
        ra(JSONObject().apply {
            put("k", "dando")
            put("daLuuHomNay", VoDanDo.daLuuHomNay(context))
            put("cacTrang", JSONArray(NhacBai.docTrang(context).sortedBy { it.ngay }.map { it.moTa() }))
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
     *  -e viec cho -e phut 30        nguoi lon cho gio: cong vao phien, khong thi cap phieu
     *  -e viec batdau                bam Bat dau
     *  -e viec batdau -e truoc 30    bam Bat dau tu 30 phut truoc, de thanh ngay co san
     *                                khuc da choi ma khong phai ngoi cho
     *  -e viec tamdung / dong
     *  -e viec themcho -e ma bai1    xep mot bai vao hang cho duyet
     *  -e viec xoacho                bo het hang doi cho duyet
     *  -e viec xoaluot               xoa so phut da duyet va da choi trong ngay
     *  -e viec dathan -e goi com.x -e phut 15
     *  -e viec dunghet -e goi com.x  coi nhu da xem het han hom nay
     *  -e viec xoahan -e goi com.x
     *  -e viec tincuaco -e chu "..."
     *  -e viec soan -e ma 20260914-CHIEU   danh dau da soan cap
     *  -e viec xoasoan
     *  -e viec xoasocai              xoa so cai bai da nop
     *  -e viec viecnha -e chu "Quét nhà:10:0,Rửa chén:10:0"
     *  -e viec viecnhaxong           ba bam xong het
     *  -e viec xoaviecnha
     *  -e viec napdando / xoadando  trang vo dan do da soat, ca dau da chup hom nay
     *  -e viec napdenhen -e ma 1.3a  nap mot cau da qua han on lai
     *  -e viec napontrenmay          nap mot cau lam tren may da toi hen on
     *  -e viec bamo -e phut 30       ba cam may (mo cac man cua ba)
     *  -e viec badong
     *  -e viec moisom -e ma <ma buoi>      cho mo som het buoi do
     *  -e viec hoctoi -e ma toan8ct -e chu "<ten bai>"   lop da hoc toi bai do
     *     ("" la chua hoc bai nao); bo tu vung thi -e ma anh8 -e phut <so Unit>
     *  -e viec xoahoctoi -e ma toan8ct  ve lai "chua chon", may se hoi
     *  -e viec xoathe -e ma toan8ct     xoa lich su tra loi cua bo the do
     */
    @Test
    fun viec() {
        val dat = args.getString("viec") ?: error("Thieu -e viec")
        // Nhieu viec cach nhau dau phay thi lam lien mot lan chay.
        //
        // Moi lan "am instrument" la mot lan tien trinh app khoi dong lai, ma khoi
        // dong bay gio ton han: App.onCreate soat lai nam quyen sach, bo the va bo
        // tu vung. Mot muc thu dat bon thu truoc khi mo man hinh thi bon lan khoi
        // dong do cong lai lau hon ca phan dang thu.
        dat.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { motViec(it) }
    }

    private fun motViec(v: String) {
        val phut = args.getString("phut")?.toIntOrNull()
        val ma = args.getString("ma")
        val goi = args.getString("goi")
        val chu = args.getString("chu")?.let { giaiMa(it) }
        var ketQua: Any? = null

        when (v) {
            "duyet" -> ketQua = gate.approve(wantedMinutes = phut)
            "cho" -> ketQua = if (gate.state == GateState.ACTIVE) {
                gate.extend(phut ?: 30)
            } else {
                gate.approve(wantedMinutes = phut ?: 30, useQuota = false)
            }
            "batdau" -> ketQua = args.getString("truoc")?.toLongOrNull()?.let { truoc ->
                val lui = truoc * 60_000L
                gate.start(System.currentTimeMillis() - lui, SystemClock.elapsedRealtime() - lui)
            } ?: gate.start()
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
                // choi_ms, choi_ngay: so phut da choi trong ngay cua thanh ngay (1/10/2026).
                prefs.raw().edit()
                    .remove("day_key").remove("day_count")
                    .remove("bonus_day").remove("bonus_count")
                    .remove("choi_ms").remove("choi_ngay")
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
            "tincuaco" -> {
                KhoTinCuaCo(context).them(chu ?: "")
                ketQua = KhoTinCuaCo(context).soTinChuaDoc()
            }
            "xoatin" -> {
                KhoTinCuaCo(context).xoaHet(); ketQua = 0
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
             * Nap mot trang vo da soat, de nhin canh "may nho roi" ma khong phai chup that.
             * -e ngay yyyy-MM-dd de nap vo cua hom khac (mac dinh hom nay); trung ngay thi thay
             * trang cu. Moi dong mang ten mon o dau, de [NhacBai] tinh han theo tiet sau cua mon.
             */
            "napdando" -> {
                val ngay = java.time.LocalDate.parse(args.getString("ngay") ?: java.time.LocalDate.now().toString())
                NhacBai.ghi(
                    context, ngay,
                    listOf(
                        VoDanDo.Dong("Toán: làm bài 2.26 và 2.27 trang 45", laBaiTap = true),
                        VoDanDo.Dong("Tiếng Anh: làm bài tập Unit 2 trang 14", laBaiTap = true),
                        VoDanDo.Dong("KHTN: tiết sau kiểm tra bài 2, bài 3", laBaiTap = false),
                        VoDanDo.Dong("Mang đủ sách vở, mặc đồng phục", laBaiTap = false)
                    )
                )
                VoDanDo.ghiDaLuu(context)
                ketQua = NhacBai.trangNgay(context, ngay)?.moTa() ?: "đã lưu vở ngày $ngay (không còn gì để nhắc)"
            }
            // Xoa ca dau "da chup vo hom nay" (khoa KHOA_NGAY_LUU cua VoDanDo). Chi xoa trang
            // o NhacBai thi muc "chua chup" chay sau napdando cung ngay van thay dong "Đã chụp vở
            // hôm nay" o man chinh.
            "xoadando" -> {
                NhacBai.xoaHet(context)
                prefs.raw().edit().remove("vo_dan_do_luu_ngay").commit()
                ketQua = NhacBai.docTrang(context).isEmpty() && !VoDanDo.daLuuHomNay(context)
            }
            /*
             * Nap mot cau DA QUA HEN on lai.
             *
             * Ghi cau lam dung tu HOM QUA thi chua toi han, vi hen dau tien la ba ngay
             * (KhoBai.KHOANG_HEN_NGAY): do la canh "da sua xong", khong phai canh "den
             * hen" (ManualChonBai#napOnTap tung vap dung cho nay; ham do bo ngay
             * 2/10/2026 cung man on chup anh). O day lui han nam ngay de chac chan qua
             * moc dau tien.
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
            /*
             * Nap mot cau LAM TREN MAY da toi hen on. Tu 30/9/2026 on lai nam o trang Luyen tap
             * va chi dem cau lam duoc tren may ([LamTrenMay.cauOn]), nen cau chup anh cua
             * napdenhen khong hien o do.
             *
             * Lay cau Toan dau tien ma Luyen tap giao khi lop hoc toi Bai 3, roi tra lai moc Toan
             * cu. Ghi mot luot sai mot lan roi dung, lui bon ngay: hen on dau tien la ba ngay sau
             * lan lam dung, giong LamTrenMayTest. Khong cong phut (congNgay = false). Xoa so truoc
             * de chi con dung cau nay den hen; don bang xoasocai.
             */
            "napontrenmay" -> {
                SoCaiBai.xoaHet(context)
                NganHang.napNeuCan(context)
                val mocCu = MocThu.luu(context)
                MocThu.datToan(context, 3)
                val m = LamTrenMay.cauLamThem(context, "Toán").firstOrNull()
                MocThu.tra(context, mocCu)
                m ?: error("khong co cau Toan nao lam duoc tren may")
                val luot = LuatGhep.kiem(LuatGhep.kiem(LuatGhep.Luot(m.ghep.sao), soSai = 1), soSai = 0)
                val luc = System.currentTimeMillis() - 4L * 24 * 60 * 60 * 1000
                LamTrenMay.ghi(context, m, luot, "thu", LamTrenMay.Loai.LAM_THEM, bayGio = luc, congNgay = false)
                ketQua = "${m.cau.id}: ${LamTrenMay.soCauOn(context)} câu đến hẹn"
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
            // Ghi thang, khong qua nhat ky: dat boi canh cho trang thu chu khong phai
            // Le Hoa chon that. HocToi ghi bang commit() nen khong can epGhi.
            "hoctoi" -> {
                val bo = ma ?: error("Thieu -e ma <bo>")
                val mon = monCuaBo(bo)
                if (phut != null) {
                    // Bo tu: "hoc toi Unit N" doi ra Unit 1 toi N (2/10/2026: con danh dau tung Unit).
                    MocThu.datUnit(context, phut)
                } else if (vn.huytl.homeworkgate.kho.PhanHoc.cuaBoThe(bo).size > 1) {
                    // Bo Toan hai phan: doi ten bai ra so, danh dau moi bai tu 1 toi so do.
                    MocThu.datToan(context, vn.huytl.homeworkgate.kho.PhanHoc.soBai(chu.orEmpty()) ?: 0)
                } else {
                    MocThu.datPhan(context, bo, vn.huytl.homeworkgate.kho.PhanHoc.soBai(chu.orEmpty()) ?: 0)
                }
                ketQua = HocToi.daHoc(context, mon)
            }
            "xoahoctoi" -> {
                val bo = ma ?: error("Thieu -e ma <bo>")
                HocToi.xoa(context, monCuaBo(bo))
                ketQua = 0
            }
            // Xoa lich su tra loi cua MOT bo the, de muc thu go dung luon co the den
            // luot. Trang thu van dong ho ve cung mot buoi toi, nen the nao vua go dung
            // o lan chay truoc thi lan sau van chua toi hen; chay dom lan la het the.
            "xoathe" -> {
                val bo = ma ?: error("Thieu -e ma <bo>")
                ketQua = KhoBai.get(context).writableDatabase
                    .delete("tra_the", "the_id LIKE ?", arrayOf("$bo:%"))
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
     *   -e daCongLamThem 0
     *
     * Tu 29/9/2026 day chi con la luat duong chup anh (bai co giao, tran 45); tu 30/9/2026
     * khong con tron goi nen ban cham khong mang phan vo dan do nua. Tham so
     * "daCongLamThem" giu ten cu vi tools/web.py van gui ten do, nhung nghia moi la so
     * phut chup anh da cong trong ngay (daCongAnhHomNay). "onTap" van nhan nhung bo qua:
     * on lai da sang lam tren may, khong con di qua ham nay.
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
                soDong = c.optInt("soDong", 0)
            )
        }
        val ket = KetQuaCham(mon = o.optString("mon", ""), cac = cac)
        val b = LuatCongGio.tinh(
            ket,
            daCongAnhHomNay = (args.getString("daCongAnh") ?: args.getString("daCongLamThem"))
                ?.toIntOrNull() ?: 0
        )
        ra(JSONObject().apply {
            put("k", "conggio")
            put("phut", b.phut)
            put("canBaHuyXem", b.canBaHuyXem)
            put("dong", JSONArray(b.dong))
            put("phutCua", JSONObject(b.phutCua as Map<*, *>))
            put("thieuDong", JSONArray(b.thieuDong.map { it.ma }))
            put("toiDaMoiNgay", LuatCongGio.TRAN_ANH)
            put("tranAnh", LuatCongGio.TRAN_ANH)
            put("tranTrenMay", LuatCongGio.TRAN_TREN_MAY)
            put("tranOn", LuatCongGio.TRAN_ON_MOI_NGAY)
            put("tranNgay", LuatCongGio.TRAN_NGAY)
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

    /**
     * Mon cua mot ma bo the, ma phan hay bo tu, cho hai lenh "hoctoi", "xoahoctoi": tu 2/10/2026
     * bai da hoc luu theo mon chu khong theo bo.
     */
    private fun monCuaBo(bo: String): String =
        vn.huytl.homeworkgate.kho.BoThe.theoMa(bo)?.mon
            ?: vn.huytl.homeworkgate.kho.PhanHoc.theoMa(bo)?.mon
            ?: if (bo == vn.huytl.homeworkgate.kho.PhanHoc.BO_TIENG_ANH) vn.huytl.homeworkgate.kho.PhanHoc.TIENG_ANH
            else error("Khong biet mon cua $bo")
}
