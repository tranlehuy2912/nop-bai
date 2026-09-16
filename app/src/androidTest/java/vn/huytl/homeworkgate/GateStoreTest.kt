package vn.huytl.homeworkgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.EndReason
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.Prefs
import java.util.Calendar

/**
 * Kiem tra phan dem gio, la cho de bi lach nhat va cung la cho neu sai thi khong
 * ai phat hien ra cho den luc dua tre choi ca buoi toi.
 *
 * Chay tren may that hoac may ao vi Prefs dua tren Android Keystore.
 *
 * CAN THAN: [setUp] xoa sach prefs truoc moi ca test, va test chay chung tien trinh
 * voi app that. Chay bo test nay tren mot may dang dung nghia la mat token, chat id
 * va ma PIN tren may do. Chay tren may ao thi khong sao, nhung DUNG chay tren tablet
 * cua Le Hoa; sau khi chay tren may ao thi nap lai cau hinh bang tools/emu.sh seed.
 */
@RunWith(AndroidJUnit4::class)
class GateStoreTest {

    private lateinit var prefs: Prefs
    private lateinit var gate: GateStore

    /** 19:00 ngay 11/9/2026, theo mui gio may. */
    private fun at(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 11, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private val minute = 60_000L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = Prefs.get(context)
        // Xoa het de moi ca test bat dau tu con so khong. Day chinh la dong lam mat
        // cau hinh that neu ai do chay bo test nay tren may dang dung.
        prefs.raw().edit().clear().commit()
        prefs.grantMinutes = 60
        prefs.hardStopMinuteOfDay = 21 * 60
        prefs.tranPhutMoiNgay = 120
        gate = GateStore(context)
    }

    @Test
    fun duyet_xong_thi_cho_con_bam_bat_dau_chu_chua_tinh_gio() {
        val now = at(19, 0)
        val granted = gate.approve(now)

        assertEquals(60, granted)
        assertEquals(GateState.GRANTED, gate.state)
        // Diem quan trong nhat cua ca thay doi nay: duyet roi ma dong ho chua chay.
        assertEquals(0L, gate.remainingMs(now, 1_000L))
        assertTrue(!gate.isOpen())
    }

    @Test
    fun bam_bat_dau_thi_dong_ho_moi_chay() {
        val now = at(19, 0)
        gate.approve(now)
        val phut = gate.start(now, nowElapsed = 1_000L)

        assertEquals(60, phut)
        assertEquals(GateState.ACTIVE, gate.state)
        assertEquals(60 * minute, gate.remainingMs(now, 1_000L))
    }

    @Test
    fun ba_duyet_luc_chieu_con_bam_luc_toi_thi_van_du_gio() {
        // Chinh la canh da lam nay sinh viec tach duyet va bat dau: ba duyet luc
        // 15h khi dang o co quan, den 19h con moi ve toi nha.
        gate.approve(at(15, 0))
        val phut = gate.start(at(19, 0), nowElapsed = 1_000L)

        assertEquals(60, phut)
        assertEquals(60 * minute, gate.remainingMs(at(19, 0), 1_000L))
    }

    @Test
    fun thoi_gian_troi_thi_gio_con_lai_giam_theo() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        val sau20phut = gate.remainingMs(now + 20 * minute, 1_000L + 20 * minute)
        assertEquals(40 * minute, sau20phut)
    }

    @Test
    fun van_dong_ho_tien_len_thi_phien_het_som() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        // Dua tre day dong ho he thong tien 2 gio nhung dong ho tuong doi chi nhich 1 phut.
        val conLai = gate.remainingMs(now + 120 * minute, 1_000L + minute)
        assertEquals(0L, conLai)
    }

    @Test
    fun van_dong_ho_lui_lai_thi_van_tinh_theo_dong_ho_tuong_doi() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        // Day dong ho he thong lui 3 gio, nhung dong ho tuong doi da chay 50 phut.
        val conLai = gate.remainingMs(now - 180 * minute, 1_000L + 50 * minute)
        assertEquals(10 * minute, conLai)
    }

    @Test
    fun day_lui_dong_ho_giua_phien_thi_tick_cat_phien() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)
        gate.tick(now, 1_000L)

        val reason = gate.tick(now - 10 * minute, 1_000L + minute)
        assertEquals(EndReason.CLOCK_TAMPER, reason)
        assertEquals(GateState.LOCKED, gate.state)
    }

    @Test
    fun khoi_dong_lai_may_giua_phien_thi_cat_phien() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 600_000L)
        gate.tick(now, 600_000L)

        // Sau reboot dong ho tuong doi bat dau lai tu gan 0.
        val reason = gate.tick(now + minute, 5_000L)
        assertEquals(EndReason.REBOOT, reason)
        assertEquals(GateState.LOCKED, gate.state)
    }

    @Test
    fun bam_bat_dau_sat_gio_nghi_thi_chi_con_phan_con_lai() {
        gate.approve(at(20, 50))
        // Duyet 60 phut nhung bam luc 20h50, gio chot 21h: chi choi duoc 10 phut.
        assertEquals(10, gate.start(at(20, 50), nowElapsed = 1_000L))
    }

    @Test
    fun phieu_duyet_qua_gio_chot_thi_khong_bam_duoc_nua() {
        gate.approve(at(19, 0))
        assertNull(gate.start(at(21, 30), nowElapsed = 1_000L))
        assertEquals(GateState.LOCKED, gate.state)
    }

    @Test
    fun duyet_ma_khong_bam_bat_dau_thi_het_ngay_la_bo_va_tra_lai_phut() {
        gate.approve(at(19, 0))
        assertEquals(60, gate.phutConLaiHomNay(at(19, 0)))

        val reason = gate.tick(at(21, 30), 1_000L)

        assertEquals(EndReason.NEVER_STARTED, reason)
        assertEquals(GateState.LOCKED, gate.state)
        // Khong dung den thi khong mat phut nao trong tran ngay.
        assertEquals(120, gate.phutConLaiHomNay(at(21, 30)))
    }

    /**
     * Khung gio ngu phai vat qua nua dem.
     *
     * Truoc day chi co mot moc chot tinh theo tung ngay duong lich, nen 00:05 la
     * "ngay moi": con thuc khuya nop bai la choi duoc tiep, va tran phut cung vua
     * dat lai. Bo test nay giu cho do khong quay lai.
     *
     * Gio ngu trong bo test: 21:00 (setUp dat) -> 06:00 (mac dinh).
     */
    @Test
    fun khung_gio_ngu_vat_qua_nua_dem() {
        val maiSau = 24 * 60 * minute

        assertNull(gate.approve(at(21, 30)))
        assertNull(gate.approve(at(23, 59)))
        // Nua dem va rang sang: van la gio ngu.
        assertNull(gate.approve(at(0, 30) + maiSau))
        assertNull(gate.approve(at(5, 59) + maiSau))

        // Dung 06:00 la mo lai.
        assertEquals(60, gate.approve(at(6, 0) + maiSau))
        assertEquals(60, gate.start(at(6, 1) + maiSau, nowElapsed = 1_000L))
    }

    @Test
    fun dang_choi_ma_toi_gio_ngu_thi_cat_phien() {
        gate.approve(at(20, 30))
        gate.start(at(20, 30), nowElapsed = 1_000L)
        // 21:00 la gio ngu: phien phai dung, khong keo sang ngay hom sau.
        assertEquals(30, (gate.remainingMs(at(20, 30), 1_000L) / 60_000L).toInt())

        val ly = gate.tick(at(21, 1), 1_000L + 31 * minute)
        assertEquals(EndReason.HARD_STOP, ly)
        assertEquals(GateState.LOCKED, gate.state)
    }

    @Test
    fun biet_luc_nao_la_gio_ngu() {
        assertTrue(gate.trongGioNgu(at(21, 0)))
        assertTrue(gate.trongGioNgu(at(23, 30)))
        assertTrue(gate.trongGioNgu(at(0, 1)))
        assertTrue(gate.trongGioNgu(at(5, 59)))
        assertFalse(gate.trongGioNgu(at(6, 0)))
        assertFalse(gate.trongGioNgu(at(15, 0)))
        assertFalse(gate.trongGioNgu(at(20, 59)))
    }

    @Test
    fun duyet_sau_gio_nghi_thi_khong_cap() {
        assertNull(gate.approve(at(21, 30)))
        assertEquals(GateState.LOCKED, gate.state)
    }

    @Test
    fun het_tran_phut_trong_ngay_thi_khong_duyet_them() {
        assertEquals(60, gate.approve(at(15, 0)))
        gate.endSession(EndReason.RAN_OUT)
        assertEquals(60, gate.approve(at(17, 0)))
        gate.endSession(EndReason.RAN_OUT)

        assertNull(gate.approve(at(19, 0)))
        assertEquals(0, gate.phutConLaiHomNay(at(19, 0)))
    }

    @Test
    fun cham_tran_thi_cat_bot_cho_vua_chu_khong_tu_choi() {
        // Tran 120, da duyet 90. Xin them 60 thi duoc 30, khong phai bi tu choi:
        // con lam bai that, duoc it con hon khong duoc gi.
        assertEquals(90, gate.approve(at(15, 0), wantedMinutes = 90))
        gate.endSession(EndReason.RAN_OUT)

        assertEquals(30, gate.approve(at(17, 0), wantedMinutes = 60))
        assertEquals(0, gate.phutConLaiHomNay(at(17, 0)))
    }

    @Test
    fun gio_ba_cho_khong_an_vao_tran_ngay() {
        // /cho la nguoi lon chu dong cho, khong phai con doi bang bai tap.
        assertEquals(60, gate.approve(at(15, 0), wantedMinutes = 60, useQuota = false))
        assertEquals(120, gate.phutConLaiHomNay(at(15, 0)))
    }

    @Test
    fun tran_phut_dat_lai_vao_ngay_hom_sau() {
        assertEquals(120, gate.approve(at(15, 0), wantedMinutes = 120))
        assertEquals(0, gate.phutConLaiHomNay(at(15, 0)))
        assertEquals(120, gate.phutConLaiHomNay(at(15, 0) + 24 * 60 * minute))
    }

    @Test
    fun cong_gio_giua_phien_bang_bai_tap_cung_an_vao_tran() {
        val now = at(15, 0)
        gate.approve(now, wantedMinutes = 100)
        gate.start(now, nowElapsed = 1_000L)
        assertEquals(20, gate.phutConLaiHomNay(now))

        // Con nop them bai trong luc dang choi, AI cham duoc 30 phut: chi cong 20.
        gate.extend(30, now + minute, nowElapsed = 1_000L + minute, useQuota = true)
        assertEquals(0, gate.phutConLaiHomNay(now))
    }

    @Test
    fun dich_vu_canh_app_vang_mat_giua_phien_thi_van_bi_tru_dung_so_phut() {
        // Day la ly do khong can cat phien khi dich vu bi Android giet: thoi gian
        // van chay tiep trong luc no vang mat, nen con khong duoc loi gi ca.
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        // Vang mat tu 19:10 den 19:40. Quay lai thi da tieu 40 phut.
        val conLai = gate.remainingMs(now + 40 * minute, 1_000L + 40 * minute)

        assertEquals(20 * minute, conLai)
        assertEquals(GateState.ACTIVE, gate.state)
    }

    @Test
    fun tam_dung_thi_giu_nguyen_so_phut_va_app_van_khoa() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        // Choi 20 phut roi bam tam dung.
        val conLai = gate.pause(now = now + 20 * minute, nowElapsed = 1_000L + 20 * minute)

        assertEquals(40, conLai)
        assertEquals(GateState.PAUSED, gate.state)
        // Giu gio khong phai la thu mien phi: dang nghi thi app giai tri van khoa.
        assertTrue(!gate.isOpen())
    }

    @Test
    fun choi_tiep_thi_lay_lai_dung_so_phut_da_giu() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)
        gate.pause(now = now + 20 * minute, nowElapsed = 1_000L + 20 * minute)

        // Nghi nua tieng roi quay lai, van con xa gio chot nen duoc lai du 40 phut.
        val moc = now + 50 * minute
        val phut = gate.resume(moc, nowElapsed = 1_000L + 50 * minute)

        assertEquals(40, phut)
        assertEquals(GateState.ACTIVE, gate.state)
        assertEquals(40 * minute, gate.remainingMs(moc, 1_000L + 50 * minute))
    }

    @Test
    fun nghi_lau_qua_thi_choi_tiep_chi_con_phan_truoc_gio_chot() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)
        gate.pause(now = now + 20 * minute, nowElapsed = 1_000L + 20 * minute)

        // Giu 40 phut nhung 20:50 moi quay lai, gio chot 21:00 chi con 10 phut.
        val phut = gate.resume(at(20, 50), nowElapsed = 1_000L + 110 * minute)

        assertEquals(10, phut)
    }

    @Test
    fun tat_man_hinh_thi_khoang_do_duoc_tra_lai() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        // Man hinh tat luc 19:10, den 19:25 moi phat hien. Ca 15 phut deu khong
        // phai la choi nen phai duoc tra lai het.
        val moc = now + 25 * minute
        val conLai = gate.pause(
            creditMs = 15 * minute,
            now = moc,
            nowElapsed = 1_000L + 25 * minute
        )

        assertEquals(50, conLai)
    }

    @Test
    fun tra_lai_khong_bao_gio_vuot_qua_so_phut_da_duyet() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        // Tra lai mot con so vo ly thi van chi toi da bang so da duyet.
        val conLai = gate.pause(
            creditMs = 10 * 60 * minute,
            now = now + minute,
            nowElapsed = 1_000L + minute
        )

        assertEquals(60, conLai)
    }

    @Test
    fun dang_nghi_ma_qua_gio_chot_thi_bo_phien() {
        val now = at(20, 50)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)
        gate.pause(now = now + minute, nowElapsed = 1_000L + minute)

        // Gio chot 21:00 khong doi duoc bang cach bam tam dung.
        val reason = gate.tick(at(21, 30), 1_000L + 40 * minute)

        assertEquals(EndReason.HARD_STOP, reason)
        assertEquals(GateState.LOCKED, gate.state)
    }

    @Test
    fun het_gio_binh_thuong_thi_tick_bao_ran_out() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)
        gate.tick(now, 1_000L)

        val reason = gate.tick(now + 61 * minute, 1_000L + 61 * minute)
        assertEquals(EndReason.RAN_OUT, reason)
    }

    @Test
    fun qua_gio_nghi_giua_phien_thi_tick_bao_hard_stop() {
        val now = at(20, 40)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)
        gate.tick(now, 1_000L)

        val reason = gate.tick(at(21, 5), 1_000L + 25 * minute)
        assertEquals(EndReason.HARD_STOP, reason)
    }

    @Test
    fun cong_dong_thi_khong_con_gio() {
        assertEquals(0L, gate.remainingMs())
        assertTrue(!gate.isOpen())
    }
    @Test
    fun tam_dung_thi_giu_ca_phan_giay_le() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        // Choi 20 phut 30 giay. Man hinh cua con hien so nay den tung giay, nen
        // phan le phai duoc giu lai chu khong lam tron mat.
        val moc = 20 * minute + 30_000L
        gate.pause(now = now + moc, nowElapsed = 1_000L + moc)

        assertEquals(39 * minute + 30_000L, gate.pausedMs())
        assertEquals(39, gate.pausedMinutes())
    }
    @Test
    fun duyet_lan_hai_khi_chua_bam_bat_dau_thi_cong_don() {
        assertEquals(60, gate.approve(at(19, 0)))
        // Con lam them bai nop tiep. Duyet lan hai la cong vao, khong de len.
        assertEquals(120, gate.approve(at(19, 10)))
        assertEquals(GateState.GRANTED, gate.state)
        assertEquals(120, gate.grantedMinutes)
        // Ca hai lan duyet deu an vao tran ngay: 60 + 60 = het 120.
        assertEquals(0, gate.phutConLaiHomNay(at(19, 10)))
    }

    @Test
    fun nop_them_bai_roi_bi_tu_choi_thi_van_con_phieu_cu() {
        assertEquals(60, gate.approve(at(19, 0)))
        gate.markPending("bai-moi", 0L, at(19, 5))
        assertEquals(GateState.PENDING, gate.state)

        // Ba khong duyet bai moi. Phan da duyet truoc do phai con nguyen, khong thi
        // nop them bai thanh ra mat gio.
        gate.boBaiCho("bai-moi")
        assertEquals(GateState.GRANTED, gate.state)
        assertEquals(60, gate.grantedMinutes)
    }

    @Test
    fun nop_them_bai_roi_duyet_thi_cong_don() {
        // Buoi chieu, con xa gio nghi 21:00 - de hai tieng gio gom duoc khong bi cat
        // bot. Cat bot la dung, nhung day dang do cho cong don chu khong phai gio chot.
        assertEquals(60, gate.approve(at(15, 0)))
        gate.markPending("bai-moi", 0L, at(15, 10))
        assertEquals(120, gate.approve(at(15, 20), requestId = "bai-moi"))
        assertEquals(GateState.GRANTED, gate.state)

        // Bam Bat dau mot lan la choi ca cuc gio da gom.
        assertEquals(120, gate.start(at(15, 21), nowElapsed = 1_000L))
    }

    @Test
    fun gio_gom_duoc_van_khong_vuot_qua_gio_nghi() {
        gate.approve(at(19, 0))
        gate.markPending("bai-moi", 0L, at(19, 10))
        assertEquals(120, gate.approve(at(19, 20), requestId = "bai-moi"))

        // Gom duoc hai tieng nhung tu 19:21 den 21:00 chi con 99 phut.
        assertEquals(99, gate.start(at(19, 21), nowElapsed = 1_000L))
    }

    @Test
    fun dang_tam_dung_ma_duyet_them_thi_gop_ca_phan_dang_giu() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)
        // Choi 20 phut roi nghi, con giu 40 phut.
        assertEquals(40, gate.pause(now = now + 20 * minute, nowElapsed = 1_000L + 20 * minute))

        // Lam bai nop tiep, duoc duyet 30 phut: thanh 70 chu khong phai mat 40 cu.
        assertEquals(70, gate.approve(at(19, 30), wantedMinutes = 30))
        assertEquals(GateState.GRANTED, gate.state)
        assertEquals(70, gate.start(at(19, 31), nowElapsed = 1_000L + 31 * minute))
    }
    @Test
    fun dang_cho_duyet_bai_moi_van_bam_bat_dau_duoc() {
        assertEquals(60, gate.approve(at(15, 0)))
        gate.markPending("bai-moi", 0L, at(15, 10))
        assertEquals(GateState.PENDING, gate.state)

        // Khong phai ngoi cho ba duyet bai moi roi moi duoc dung phan da duyet truoc
        // do. Nop them bai ma thanh ra bi cam choi thi khong ai nop them.
        assertEquals(60, gate.start(at(15, 1), nowElapsed = 1_000L))
        assertEquals(GateState.ACTIVE, gate.state)
    }

    @Test
    fun chua_duyet_gi_ma_dang_cho_thi_khong_bam_bat_dau_duoc() {
        gate.markPending("bai-dau-tien", 0L, at(15, 0))
        assertNull(gate.start(at(15, 1), nowElapsed = 1_000L))
        assertEquals(GateState.PENDING, gate.state)
    }

    // --- nhieu bai cung xep hang cho duyet ---

    @Test
    fun nop_hai_bai_thi_duyet_tung_bai_mot() {
        gate.markPending("bai-1", 111L, at(15, 0))
        gate.markPending("bai-2", 222L, at(15, 30))
        assertEquals(2, gate.soBaiDangCho())

        // Duyet bai dau: van con bai hai nam cho, nen chua ve GRANTED.
        assertEquals(60, gate.approve(at(16, 0), requestId = "bai-1"))
        assertEquals(GateState.PENDING, gate.state)
        assertEquals(listOf("bai-2"), gate.baiDangCho().map { it.id })

        // Duyet not bai hai: cong don thanh 120 va het bai cho.
        assertEquals(120, gate.approve(at(16, 10), requestId = "bai-2"))
        assertEquals(GateState.GRANTED, gate.state)
        assertEquals(0, gate.soBaiDangCho())
    }

    @Test
    fun duyet_bai_nay_khong_dung_den_bai_kia() {
        gate.markPending("bai-1", 111L, at(15, 0))
        gate.markPending("bai-2", 222L, at(15, 30))

        // Ba tu choi bai hai. Bai mot phai con nguyen trong hang cho.
        assertEquals(222L, gate.boBaiCho("bai-2")?.messageId)
        assertEquals(listOf("bai-1"), gate.baiDangCho().map { it.id })
        assertEquals(GateState.PENDING, gate.state)
    }

    @Test
    fun huy_bai_vua_nop_thi_bo_bai_moi_nhat() {
        gate.markPending("bai-1", 111L, at(15, 0))
        gate.markPending("bai-2", 222L, at(15, 30))

        assertEquals("bai-2", gate.huyBaiMoiNhat()?.id)
        assertEquals(listOf("bai-1"), gate.baiDangCho().map { it.id })
    }

    @Test
    fun huy_not_bai_cuoi_thi_ve_lai_trang_thai_khoa() {
        gate.markPending("bai-1", 0L, at(15, 0))
        gate.huyBaiMoiNhat()
        assertEquals(GateState.LOCKED, gate.state)
        assertEquals(0, gate.soBaiDangCho())
    }

    @Test
    fun xep_hang_toi_da_ba_bai() {
        gate.markPending("bai-1", 0L, at(15, 0))
        gate.markPending("bai-2", 0L, at(15, 10))
        assertTrue(gate.conChoNopThem())

        gate.markPending("bai-3", 0L, at(15, 20))
        assertEquals(3, gate.soBaiDangCho())
        assertFalse(gate.conChoNopThem())
    }

    @Test
    fun dang_choi_ma_nop_them_bai_thi_dong_ho_van_chay() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)

        gate.markPending("bai-moi", 0L, at(19, 10))
        assertEquals(GateState.ACTIVE, gate.state)
        // Dong ho phien khong bi dung lai: van con 50 phut o phut thu muoi.
        assertEquals(
            50 * minute,
            gate.remainingMs(at(19, 10), nowElapsed = 1_000L + 10 * minute)
        )
        assertEquals(1, gate.soBaiDangCho())
    }

    @Test
    fun het_gio_giua_chung_van_giu_bai_dang_cho() {
        val now = at(19, 0)
        gate.approve(now)
        gate.start(now, nowElapsed = 1_000L)
        gate.markPending("bai-moi", 0L, at(19, 10))

        // Het gio truoc khi ba kip bam duyet. Bai da nop phai con do, khong thi con
        // chup ca dong anh ma khong ai xem den.
        gate.endSession(EndReason.RAN_OUT)
        assertEquals(GateState.PENDING, gate.state)
        assertEquals(1, gate.soBaiDangCho())

        // Va van duyet duoc binh thuong sau do.
        assertEquals(60, gate.approve(at(20, 0), requestId = "bai-moi"))
        assertEquals(GateState.GRANTED, gate.state)
    }

    @Test
    fun bai_nop_hom_qua_thi_hom_nay_khong_con_duyet_duoc() {
        val homQua = at(19, 0) - 24 * 60 * minute
        gate.markPending("bai-cu", 0L, homQua)
        assertEquals(1, gate.soBaiDangCho())

        gate.tick(nowWall = at(8, 0), nowElapsed = 1_000L)
        assertEquals(0, gate.soBaiDangCho())
        assertEquals(GateState.LOCKED, gate.state)
    }
}