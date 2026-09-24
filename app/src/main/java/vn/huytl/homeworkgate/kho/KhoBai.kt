package vn.huytl.homeworkgate.kho

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import vn.huytl.homeworkgate.data.LoaiLoi
import vn.huytl.homeworkgate.data.LuatTuVung
import vn.huytl.homeworkgate.data.SoCaiBai

/**
 * Kho bai tap nam trong may: ngan hang cau hoi va toan bo cau tra loi cua con.
 *
 * VI SAO PHAI CO CAI NAY. Truoc day so cai lay DE BAI DO AI CHEP LAI lam khoa
 * nhan dang - xem lich su cua [vn.huytl.homeworkgate.data.SoCaiBai]. Nhung chinh
 * cau lenh cham bai da ghi lai: cung nam tam anh KHTN ngay 14/9/2026, ba lan chay
 * AI tach ra 36, roi 4, roi 22 cau, va moi lan chep de mot khac. Nen cung mot bai
 * hom nay ra khoa nay, mai ra khoa khac - tinh gio lan hai; ma hai cau ngan khac
 * nhau ("Rut gon", "Tinh nhanh") lai chuan hoa ra gan giong nhau - bao "da lam roi"
 * oan. Do la dung cai Ba Huy thay khi chay thu hai ngay: "luc phan biet duoc, luc
 * khong".
 *
 * Chua duoc chung nao con danh tinh cua mot cau con do AI dat ra. Nen o day cau
 * hoi co MA CO DINH, nap san tu sach giao khoa, va con khai truoc minh dang lam
 * bai nao. AI chi con viec cham dung hay sai - viec no lam duoc - chu khong phai
 * viec nho xem hom qua da cham cai gi.
 *
 * VI SAO LA SQLITE CHU KHONG PHAI JSON TRONG PREFS nhu phan con lai cua app: mot
 * quyen sach giao khoa la hon hai tram cau, ba nam hoc la vai nghin cau tra loi.
 * Doc ca xap JSON ra roi loc trong bo nho cho moi lan hoi "cau nay tra gio chua"
 * van chay duoc, nhung ghi thi phai ghi lai TOAN BO mang moi lan - va ghi kieu do
 * vao dung luc service bi giet la mat sach so cai. SQLite ghi tung dong, khong bao
 * gio mat ca quyen so vi mot lan ghi hong.
 *
 * KHONG DAY LEN FIREBASE. Ba Huy chon vay: kho nay chi song trong may cua Le Hoa.
 * Doi lai mat mang van cham va van chan trung duoc - ma mat mang la luc de gian
 * lan nhat neu viec chan trung phai hoi may chu.
 */
class KhoBai private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, TEN, null, BAN) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE cau_hoi (
              id     TEXT PRIMARY KEY,
              mon    TEXT NOT NULL,
              nguon  TEXT NOT NULL,
              chuong TEXT,
              bai    TEXT,
              nhom   TEXT,
              ma     TEXT NOT NULL,
              de     TEXT NOT NULL,
              trang  INTEGER,
              dang   TEXT,
              thu_tu INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX ix_cau_bai ON cau_hoi(nguon, bai)")

        /*
         * Moi lan cham mot cau la mot dong, khong ghi de len dong cu.
         *
         * Giu ca lich su chu khong chi trang thai cuoi, vi ba viec: Ba Huy xem lai
         * duoc con da viet gi hom truoc; nhin duoc mot cau con phai sua may lan moi
         * dung; va khi mot cau bi cham nham thi con doi chieu duoc, thay vi chi thay
         * mot chu "sai" khong biet tu dau ra.
         */
        db.execSQL(
            """
            CREATE TABLE tra_loi (
              id       INTEGER PRIMARY KEY AUTOINCREMENT,
              cau_id   TEXT NOT NULL,
              mon      TEXT,
              ma       TEXT,
              de       TEXT,
              ket_qua  TEXT,
              bai_lam  TEXT,
              dong_sai INTEGER NOT NULL DEFAULT 0,
              on_tap   INTEGER NOT NULL DEFAULT 0,
              khai_chac INTEGER NOT NULL DEFAULT -1,
              con_noi  TEXT,
              loai_loi TEXT,
              dung     INTEGER NOT NULL,
              phut     INTEGER NOT NULL,
              nhan_xet TEXT,
              luc      INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX ix_tra_cau ON tra_loi(cau_id)")
        db.execSQL("CREATE INDEX ix_tra_luc ON tra_loi(luc)")
        taoBangThe(db)
    }

    /**
     * Hai bang cua duong hoc thuoc - xem [vn.huytl.homeworkgate.data.HocThuoc].
     *
     * Tach ham rieng vi phai goi tu ca [onCreate] lan [onUpgrade]: may da cai ban cu
     * thi khong di qua onCreate nua, ma thieu hai bang nay la man hoc thuoc mo len
     * la do ngay.
     *
     * dap_khac cat bang [NGAN_DONG] y het bai_lam: SQLite khong co kieu mang, va dat
     * ra mot bang rieng chi de giu hai ba chuoi thi khong bo cong.
     */
    private fun taoBangThe(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS the_hoc (
              id       TEXT PRIMARY KEY,
              mon      TEXT NOT NULL,
              bo       TEXT NOT NULL,
              bai      TEXT,
              hoi      TEXT NOT NULL,
              dap      TEXT NOT NULL,
              dap_khac TEXT,
              thu_tu   INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_the_bo ON the_hoc(bo)")
        /*
         * Moi lan go mot dong, khong ghi de - cung le voi bang tra_loi.
         *
         * Giu ca lich su vi lich hen on doc tu day: the do dung lan cuoi luc nao, va
         * da dung duoc may lan roi. Chi giu trang thai cuoi thi khong dem duoc so
         * lan, ma so lan chinh la cai quyet dinh khoang hen ke tiep.
         */
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tra_the (
              id     INTEGER PRIMARY KEY AUTOINCREMENT,
              the_id TEXT NOT NULL,
              go     TEXT,
              dung   INTEGER NOT NULL,
              chiu   INTEGER NOT NULL DEFAULT 0,
              phut   INTEGER NOT NULL,
              giay   INTEGER NOT NULL DEFAULT 0,
              luc    INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_tra_the ON tra_the(the_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_tra_the_luc ON tra_the(luc)")
        taoBangTuVung(db)
    }

    /**
     * Hai bang cua duong tu vung - xem [vn.huytl.homeworkgate.data.LuatTuVung].
     *
     * Tach khoi [taoBangThe] du hai duong nhin giong nhau: the hoc thuoc giu mot cap
     * hoi/dap co dinh, con mot tu duoc hoi ca hai chieu va mang them phien am voi
     * loai tu. Nhet chung mot bang thi co bon cot rong o moi dong cua ben kia.
     */
    private fun taoBangTuVung(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tu_vung (
              id     TEXT PRIMARY KEY,
              bo     TEXT NOT NULL,
              mon    TEXT NOT NULL,
              unit   INTEGER NOT NULL DEFAULT 0,
              tu     TEXT NOT NULL,
              loai   TEXT,
              am     TEXT,
              nghia  TEXT NOT NULL,
              thu_tu INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_tu_bo ON tu_vung(bo)")
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_tu_unit ON tu_vung(bo, unit)")
        /*
         * MOI LAN THU MOT DONG, ke ca lan sai, ke ca lan bam "Chịu rồi".
         *
         * Chi ghi lan cuoi cung thi mat dung thu dang gia nhat: TY LE DUNG NGAY LAN
         * DAU. So phut tra cho viec ngoi go, con ty le lan dau moi tra loi duoc cau
         * hoi that su cua Ba Huy - con co hoc tu vung khong. Hai con so do tach nhau,
         * va cai thu hai chi con o day.
         *
         * "phien" de dem "dung du hai lan trong MOT buoi": khong co cot nay thi hai
         * lan dung cua hai ngay khac nhau cong lai thanh mot buoi da xong.
         */
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tra_tu (
              id     INTEGER PRIMARY KEY AUTOINCREMENT,
              tu_id  TEXT NOT NULL,
              phien  TEXT NOT NULL,
              buoi   TEXT NOT NULL,
              chieu  TEXT NOT NULL,
              lan    INTEGER NOT NULL DEFAULT 1,
              go     TEXT,
              dung   INTEGER NOT NULL,
              goi_y  INTEGER NOT NULL DEFAULT 0,
              chiu   INTEGER NOT NULL DEFAULT 0,
              giay   INTEGER NOT NULL DEFAULT 0,
              luc    INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_tra_tu ON tra_tu(tu_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_tra_tu_luc ON tra_tu(luc)")
    }

    /**
     * Len ban moi thi GIU nguyen cau tra loi cu.
     *
     * Ban dau cho nay xoa het va tao lai, voi ly do "cau tra loi cu chi dang gia
     * trong vai thang". Ly do do sai: bang tra_loi chinh la so cai chan viec tinh
     * gio hai lan cho cung mot cau. Xoa no di la mo duong cho viec chup lai nguyen
     * xap bai cu de lay gio lan nua, ma do la thu ca he thong nay dung de chan.
     *
     * Rieng bang cau hoi thi xoa duoc that: no nap lai tu file trong chinh app.
     */
    override fun onUpgrade(db: SQLiteDatabase, cu: Int, moi: Int) {
        db.execSQL("DROP TABLE IF EXISTS cau_hoi")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cau_hoi (
              id     TEXT PRIMARY KEY,
              mon    TEXT NOT NULL,
              nguon  TEXT NOT NULL,
              chuong TEXT,
              bai    TEXT,
              nhom   TEXT,
              ma     TEXT NOT NULL,
              de     TEXT NOT NULL,
              trang  INTEGER,
              dang   TEXT,
              thu_tu INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_cau_bai ON cau_hoi(nguon, bai)")

        // Ban 4: con tu khai truoc la chac hay chua chac, va tu noi minh sai cho nao.
        if (cu < 4) {
            runCatching {
                db.execSQL("ALTER TABLE tra_loi ADD COLUMN khai_chac INTEGER NOT NULL DEFAULT -1")
            }
            runCatching { db.execSQL("ALTER TABLE tra_loi ADD COLUMN con_noi TEXT") }
        }

        // Ban 5: cau nay sai KIEU gi. Dong cu de rong, khong doan nguoc - bang thong
        // ke chi tinh tu ngay len ban nay tro di, va no noi ro dieu do.
        if (cu < 5) {
            runCatching { db.execSQL("ALTER TABLE tra_loi ADD COLUMN loai_loi TEXT") }
        }

        // Ban 6: duong hoc thuoc. Hai bang moi, khong dung den bang nao dang co.
        if (cu < 6) runCatching { taoBangThe(db) }

        // Ban 7: duong tu vung. Cung vay, hai bang moi dung rieng.
        if (cu < 7) runCatching { taoBangTuVung(db) }

        // Ban 8: phan biet "go sai" voi "bam Chịu rồi" o duong the hoc thuoc.
        if (cu < 8) {
            runCatching { db.execSQL("ALTER TABLE tra_the ADD COLUMN chiu INTEGER NOT NULL DEFAULT 0") }
        }

        /*
         * Ban 9: the hoc thuoc tinh bang giay, khong con "ba the mot phut".
         *
         * Dong cu de giay = 0 chu khong doan nguoc tu cot phut. Doan nguoc thi so
         * giay cua hom truoc tu nhien moc len, va tran ngay cua HOM NAY bi an mat
         * mot phan - ma cot nay chi dung cho tran cua chinh ngay hom nay.
         */
        if (cu < 9) {
            runCatching { db.execSQL("ALTER TABLE tra_the ADD COLUMN giay INTEGER NOT NULL DEFAULT 0") }
        }
    }

    // ------------------------------------------------------------ ngan hang cau

    /** Thay toan bo cau hoi cua mot nguon (mot quyen sach) bang danh sach moi. */
    fun napNguon(nguon: String, cac: List<CauHoi>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("cau_hoi", "nguon = ?", arrayOf(nguon))
            cac.forEach { c ->
                writableDatabase.insertWithOnConflict(
                    "cau_hoi", null,
                    ContentValues().apply {
                        put("id", c.id)
                        put("mon", c.mon)
                        put("nguon", c.nguon)
                        put("chuong", c.chuong)
                        put("bai", c.bai)
                        put("nhom", c.nhom)
                        put("ma", c.ma)
                        put("de", c.de)
                        put("trang", c.trang)
                        put("dang", c.dang)
                        put("thu_tu", c.thuTu)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun soCauCua(nguon: String): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM cau_hoi WHERE nguon = ?", arrayOf(nguon)
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** Cac bai cua mot nguon, theo dung thu tu in trong sach. */
    fun cacBaiCua(nguon: String): List<TenBai> =
        readableDatabase.rawQuery(
            """
            SELECT bai, chuong, MIN(trang), COUNT(*), MIN(thu_tu)
            FROM cau_hoi WHERE nguon = ?
            GROUP BY bai, chuong
            ORDER BY MIN(thu_tu)
            """.trimIndent(),
            arrayOf(nguon)
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        TenBai(
                            bai = c.getString(0).orEmpty(),
                            chuong = c.getString(1).orEmpty(),
                            trang = c.getInt(2),
                            soCau = c.getInt(3)
                        )
                    )
                }
            }
        }

    /**
     * Cac trang co bai tap trong mot quyen, theo thu tu.
     *
     * Co giao giao bai theo TRANG ("lam trang 36, 37") chu khong theo ten bai, nen
     * con phai chon duoc theo trang. Mot trang co the vat sang hai bai, va mot bai
     * trai ra may trang - nen day khong phai la cach nhom lai cua [cacBaiCua] ma la
     * mot cach nhin khac han vao cung mot quyen.
     */
    fun cacTrangCua(nguon: String): List<TrangSach> =
        readableDatabase.rawQuery(
            """
            SELECT trang, COUNT(*), MIN(thu_tu), MIN(bai), MIN(chuong)
            FROM cau_hoi WHERE nguon = ? AND trang > 0
            GROUP BY trang
            ORDER BY trang
            """.trimIndent(),
            arrayOf(nguon)
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        TrangSach(
                            trang = c.getInt(0),
                            soCau = c.getInt(1),
                            bai = c.getString(3).orEmpty(),
                            chuong = c.getString(4).orEmpty()
                        )
                    )
                }
            }
        }

    /** Cac cau nam o nhung trang da chon, theo thu tu in trong sach. */
    fun cacCauTheoTrang(nguon: String, trang: List<Int>): List<CauHoi> {
        if (trang.isEmpty()) return emptyList()
        val cho = trang.joinToString(",") { "?" }
        return readableDatabase.query(
            "cau_hoi", null, "nguon = ? AND trang IN ($cho)",
            (listOf(nguon) + trang.map { it.toString() }).toTypedArray(),
            null, null, "thu_tu"
        ).use { c -> buildList { while (c.moveToNext()) add(c.docCauHoi()) } }
    }

    /** Cac cau trong mot bai, theo thu tu in trong sach. */
    fun cacCauCua(nguon: String, bai: String): List<CauHoi> =
        readableDatabase.query(
            "cau_hoi", null, "nguon = ? AND bai = ?", arrayOf(nguon, bai),
            null, null, "thu_tu"
        ).use { c -> buildList { while (c.moveToNext()) add(c.docCauHoi()) } }

    fun cauTheoId(id: String): CauHoi? =
        readableDatabase.query("cau_hoi", null, "id = ?", arrayOf(id), null, null, null)
            .use { if (it.moveToFirst()) it.docCauHoi() else null }

    fun cacCauTheoId(ids: List<String>): List<CauHoi> {
        if (ids.isEmpty()) return emptyList()
        val cho = ids.joinToString(",") { "?" }
        return readableDatabase.query(
            "cau_hoi", null, "id IN ($cho)", ids.toTypedArray(), null, null, "thu_tu"
        ).use { c -> buildList { while (c.moveToNext()) add(c.docCauHoi()) } }
    }

    // -------------------------------------------------------------- cau tra loi

    fun ghiTraLoi(t: TraLoi) {
        writableDatabase.insert(
            "tra_loi", null,
            ContentValues().apply {
                put("cau_id", t.cauId)
                put("mon", t.mon)
                put("ma", t.ma)
                put("de", t.de)
                put("ket_qua", t.ketQua)
                // Danh sach dong cat thanh mot chuoi: SQLite khong co kieu mang, ma
                // dat ra mot bang rieng chi de giu vai dong chu thi khong bo cong.
                put("bai_lam", t.baiLam.joinToString(NGAN_DONG))
                put("dong_sai", t.dongSai)
                put("on_tap", if (t.onTap) 1 else 0)
                put("khai_chac", t.khaiChac)
                put("con_noi", t.conNoi)
                put("loai_loi", t.loaiLoi)
                put("dung", if (t.dung) 1 else 0)
                put("phut", t.phut)
                put("nhan_xet", t.nhanXet)
                put("luc", t.luc)
            }
        )
    }

    /**
     * Cac cau DEN HEN on lai.
     *
     * "Tung sai" moi la cai dang on. Cau con lam dung ngay tu dau thi no da biet lam
     * roi; bat lam lai chi ton thi gio cua ca hai cha con. Cho nao con trat chan mot
     * lan thi cho do dang quay lai.
     *
     * CO LICH chu khong on luc nao cung duoc. Ban dau moi cau chi duoc tra gio cho
     * DUNG MOT lan on, con muon on luc nao thi on. Ba Huy muon bo cai chan "mot lan"
     * do, nhung bo thang thi cau cu thanh may in gio: chep lai hai chuc cau da lam
     * moi toi, moi cau nua so phut, ma khong hoc them gi. Nen thay chan bang HEN.
     *
     * Khoang hen dan ra - xem [KHOANG_HEN_NGAY]. Do khong phai con so tu nhien: nho
     * lai mot thu ngay truoc khi kip quen la cach lam no o lai. Va no tu chan luon
     * duong kiem gio, vi mot cau ca thang moi tra tien mot lan.
     *
     * Moi nhat truoc: cau sai tuan nay gan hon cau sai thang truoc.
     */
    fun cacCauDenHenOn(tuLuc: Long, bayGio: Long = System.currentTimeMillis()): List<String> =
        readableDatabase.rawQuery(
            """
            SELECT t.cau_id,
                   MAX(CASE WHEN t.dung = 1 THEN t.luc ELSE 0 END) AS lan_dung_cuoi,
                   SUM(CASE WHEN t.on_tap = 1 AND t.dung = 1 THEN 1 ELSE 0 END) AS so_on
            FROM tra_loi t
            WHERE t.luc >= ? AND t.cau_id <> ?
            GROUP BY t.cau_id
            HAVING MAX(CASE WHEN t.dung = 0 AND t.on_tap = 0 THEN 1 ELSE 0 END) = 1
               AND MAX(CASE WHEN t.dung = 1 THEN 1 ELSE 0 END) = 1
            ORDER BY MAX(t.luc) DESC
            """.trimIndent(),
            arrayOf(tuLuc.toString(), CAU_GOI)
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val hen = mocHen(c.getLong(1), c.getInt(2)) ?: continue
                    if (bayGio >= hen) add(c.getString(0))
                }
            }
        }

    /**
     * Luc nao cau nay den hen on lai. null la da on du so lan, thoi.
     *
     * @param lanDungCuoi luc gan nhat con lam dung cau do.
     * @param soLanDaOn da on dung duoc may lan roi.
     */
    private fun mocHen(lanDungCuoi: Long, soLanDaOn: Int): Long? {
        if (lanDungCuoi <= 0L) return null
        val khoang = KHOANG_HEN_NGAY.getOrNull(soLanDaOn) ?: return null
        return lanDungCuoi + khoang * 24L * 60 * 60_000L
    }

    /** Cau nay co dang den hen on khong. Hoi luc cham, de quyet co tra gio hay khong. */
    fun denHenOn(cauId: String, tuLuc: Long, bayGio: Long = System.currentTimeMillis()): Boolean =
        readableDatabase.rawQuery(
            """
            SELECT MAX(CASE WHEN dung = 1 THEN luc ELSE 0 END),
                   SUM(CASE WHEN on_tap = 1 AND dung = 1 THEN 1 ELSE 0 END)
            FROM tra_loi WHERE cau_id = ? AND luc >= ?
            """.trimIndent(),
            arrayOf(cauId, tuLuc.toString())
        ).use { c ->
            if (!c.moveToFirst()) return false
            val hen = mocHen(c.getLong(0), c.getInt(1)) ?: return false
            bayGio >= hen
        }

    // ------------------------------------------------------------- the hoc thuoc

    /** Thay toan bo the cua mot bo bang danh sach moi. Giu nguyen bang tra_the. */
    fun napBoThe(bo: String, cac: List<TheHoc>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("the_hoc", "bo = ?", arrayOf(bo))
            cac.forEach { t ->
                writableDatabase.insertWithOnConflict(
                    "the_hoc", null,
                    ContentValues().apply {
                        put("id", t.id)
                        put("mon", t.mon)
                        put("bo", t.bo)
                        put("bai", t.bai)
                        put("hoi", t.hoi)
                        put("dap", t.dap)
                        put("dap_khac", t.dapKhac.joinToString(NGAN_DONG))
                        put("thu_tu", t.thuTu)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun soTheCua(bo: String): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM the_hoc WHERE bo = ?", arrayOf(bo)
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /**
     * Cac the DEN LUOT trong mot bo, theo thu tu in trong file.
     *
     * Den luot la mot trong hai: chua bao gio go dung, hoac da dung ma den han nho
     * lai. Han lay thang [mocHen] - dung cai lich 3/10/30 ngay cua cau hoi, khong
     * de ra mot lich thu hai. Qua het ba moc thi coi nhu thuoc, the do thoi hien ra.
     *
     * Loc trong Kotlin chu khong trong SQL: mot bo the co vai tram dong, doc het ra
     * roi loc mat khong toi mot phan muoi giay, ma [mocHen] la mot ham Kotlin - viet
     * lai no bang SQL la co hai ban luat song song, va den luc sua se chi sua mot.
     */
    fun cacTheDenLuot(bo: String, gioiHan: Int, bayGio: Long = System.currentTimeMillis()):
        List<TheHoc> = readableDatabase.rawQuery(
            """
            SELECT t.*,
              (SELECT MAX(luc) FROM tra_the WHERE the_id = t.id AND dung = 1) AS lan_dung,
              (SELECT COUNT(*) FROM tra_the WHERE the_id = t.id AND dung = 1) AS so_dung
            FROM the_hoc t WHERE t.bo = ? ORDER BY t.thu_tu
            """.trimIndent(),
            arrayOf(bo)
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val soDung = c.getInt(c.getColumnIndexOrThrow("so_dung"))
                    val denLuot = if (soDung == 0) true else {
                        val hen = mocHen(c.getLong(c.getColumnIndexOrThrow("lan_dung")), soDung - 1)
                        hen != null && bayGio >= hen
                    }
                    if (denLuot) add(c.docThe())
                    if (size >= gioiHan) break
                }
            }
        }

    /**
     * So the trong bo da qua het moi moc nho lai, tuc la coi nhu thuoc.
     *
     * Dung chung [mocHen] voi [cacTheDenLuot]: mot the het moc la mot the khong con
     * hien ra nua, va do cung la dinh nghia cua "thuoc" tren man chon bo.
     */
    fun soTheThuoc(bo: String): Int =
        readableDatabase.rawQuery(
            """
            SELECT
              (SELECT MAX(luc) FROM tra_the WHERE the_id = t.id AND dung = 1) AS lan_dung,
              (SELECT COUNT(*) FROM tra_the WHERE the_id = t.id AND dung = 1) AS so_dung
            FROM the_hoc t WHERE t.bo = ?
            """.trimIndent(),
            arrayOf(bo)
        ).use { c ->
            var n = 0
            while (c.moveToNext()) {
                val soDung = c.getInt(c.getColumnIndexOrThrow("so_dung"))
                if (soDung == 0) continue
                val hen = mocHen(c.getLong(c.getColumnIndexOrThrow("lan_dung")), soDung - 1)
                if (hen == null) n++
            }
            n
        }

    /** Dem the den luot ma khong doc ca bo ra - cho man chon bo. */
    fun soTheDenLuot(bo: String, bayGio: Long = System.currentTimeMillis()): Int =
        cacTheDenLuot(bo, Int.MAX_VALUE, bayGio).size

    /**
     * Bo nay con the nao den luot khong. Cung dinh nghia voi [cacTheDenLuot].
     *
     * Hoi nhanh truoc: the chua go dung lan nao thi chac chan den luot, va SQLite dung
     * ngay o the dau tien nhu vay. Chi khi moi the deu da dung it nhat mot lan moi
     * phai tinh han tung the. Man chinh hoi cau nay moi giay khi dong ho dang dem -
     * xem [BoThe.conTheDenLuot].
     */
    fun conTheDenLuot(bo: String, bayGio: Long = System.currentTimeMillis()): Boolean {
        val coTheChuaDung = readableDatabase.rawQuery(
            """
            SELECT 1 FROM the_hoc t WHERE t.bo = ?
              AND NOT EXISTS (SELECT 1 FROM tra_the WHERE the_id = t.id AND dung = 1)
            LIMIT 1
            """.trimIndent(),
            arrayOf(bo)
        ).use { it.moveToFirst() }
        return coTheChuaDung || cacTheDenLuot(bo, 1, bayGio).isNotEmpty()
    }

    fun ghiTraThe(t: TraThe) {
        writableDatabase.insert(
            "tra_the", null,
            ContentValues().apply {
                put("the_id", t.theId)
                put("go", t.go)
                put("dung", if (t.dung) 1 else 0)
                put("chiu", if (t.chiu) 1 else 0)
                put("phut", t.phut)
                put("giay", t.giay)
                put("luc", t.luc)
            }
        )
    }

    /** So phut duong hoc thuoc da cap duoc tu [tuLuc]. Chi de hien ra man hinh. */
    fun phutTheTu(tuLuc: Long): Int =
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(phut), 0) FROM tra_the WHERE luc >= ?",
            arrayOf(tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** So GIAY duong hoc thuoc da lam ra tu [tuLuc], de giu tran ngay. Giong [giayTuVungTu]. */
    fun giayTheTu(tuLuc: Long): Int =
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(giay), 0) FROM tra_the WHERE luc >= ?",
            arrayOf(tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** So the da go dung tu [tuLuc], dem theo the chu khong theo lan. */
    fun soTheDungTu(tuLuc: Long): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(DISTINCT the_id) FROM tra_the WHERE luc >= ? AND dung = 1",
            arrayOf(tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun Cursor.docThe() = TheHoc(
        id = getString(getColumnIndexOrThrow("id")),
        mon = getString(getColumnIndexOrThrow("mon")).orEmpty(),
        bo = getString(getColumnIndexOrThrow("bo")).orEmpty(),
        bai = getString(getColumnIndexOrThrow("bai")).orEmpty(),
        hoi = getString(getColumnIndexOrThrow("hoi")).orEmpty(),
        dap = getString(getColumnIndexOrThrow("dap")).orEmpty(),
        dapKhac = getString(getColumnIndexOrThrow("dap_khac")).orEmpty()
            .split(NGAN_DONG).filter { it.isNotEmpty() },
        thuTu = getInt(getColumnIndexOrThrow("thu_tu"))
    )

    // ---------------------------------------------------------------- tu vung

    /** Thay toan bo tu cua mot bo bang danh sach moi. Giu nguyen bang tra_tu. */
    fun napBoTu(bo: String, cac: List<TuVung>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("tu_vung", "bo = ?", arrayOf(bo))
            cac.forEach { t ->
                writableDatabase.insertWithOnConflict(
                    "tu_vung", null,
                    ContentValues().apply {
                        put("id", t.id)
                        put("bo", t.bo)
                        put("mon", t.mon)
                        put("unit", t.unit)
                        put("tu", t.tu)
                        put("loai", t.loai)
                        put("am", t.am)
                        put("nghia", t.nghia)
                        put("thu_tu", t.thuTu)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun soTuCua(bo: String): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM tu_vung WHERE bo = ?", arrayOf(bo)
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun cacTuCua(bo: String, unit: Int? = null): List<TuVung> {
        val (sql, tham) = if (unit == null) {
            "SELECT * FROM tu_vung WHERE bo = ? ORDER BY unit, thu_tu" to arrayOf(bo)
        } else {
            "SELECT * FROM tu_vung WHERE bo = ? AND unit = ? ORDER BY thu_tu" to
                arrayOf(bo, unit.toString())
        }
        return readableDatabase.rawQuery(sql, tham).use { c ->
            buildList { while (c.moveToNext()) add(c.docTu()) }
        }
    }

    /**
     * Tinh trang tung tu trong mot bo, kem so phien da xong, de ben goi boc va chon
     * chieu hoi.
     *
     * TINH TRONG KOTLIN chu khong trong SQL, cung mot le voi [cacTheDenLuot]: luat
     * "vua sai" va "da thuoc" nam trong [vn.huytl.homeworkgate.data.LuatTuVung], viet lai
     * no bang SQL la co hai ban luat song song va den luc sua se chi sua mot.
     *
     * Doc het lich su cua ca bo ra mot lan. Mot nam hoc chin tram tu, moi tu chung
     * sau lan gap, moi lan hai ba lan thu, ra khoang mot van dong - doc het mat vai
     * phan muoi giay, va chi doc mot lan moi buoi.
     */
    fun tinhTrangTu(
        bo: String,
        lanDungDeXong: Int
    ): List<Triple<TuVung, LuatTuVung.TinhTrang, Int>> {
        val cac = cacTuCua(bo)
        if (cac.isEmpty()) return emptyList()

        // Lich su theo tung tu, moi tu mot danh sach (phien, lan, dung), theo thu tu thoi gian.
        class Lan(val phien: String, val dung: Boolean)
        val lichSu = HashMap<String, MutableList<Lan>>()
        readableDatabase.rawQuery(
            """
            SELECT t.tu_id, t.phien, t.dung FROM tra_tu t
            JOIN tu_vung v ON v.id = t.tu_id
            WHERE v.bo = ? ORDER BY t.luc
            """.trimIndent(),
            arrayOf(bo)
        ).use { c ->
            while (c.moveToNext()) {
                lichSu.getOrPut(c.getString(0)) { mutableListOf() }
                    .add(Lan(c.getString(1), c.getInt(2) == 1))
            }
        }

        return cac.map { tu ->
            val ls = lichSu[tu.id].orEmpty()
            if (ls.isEmpty()) return@map Triple(tu, LuatTuVung.TinhTrang.CHUA_GAP, 0)

            // Phien gan nhat: lan thu DAU TIEN cua phien do co dung khong. Lay lan dau
            // chu khong lay ca phien: cuoi phien thi tu nao cung dung, vi buoi do
            // khong cho di tiep khi chua dung.
            val phienCuoi = ls.last().phien
            val dauPhienCuoi = ls.first { it.phien == phienCuoi }.dung

            val soPhienXong = ls.groupBy { it.phien }
                .count { (_, cacLan) -> cacLan.count { it.dung } >= lanDungDeXong }

            val tinh = when {
                !dauPhienCuoi -> LuatTuVung.TinhTrang.VUA_SAI
                soPhienXong <= 0 -> LuatTuVung.TinhTrang.CHUA_GAP
                soPhienXong == 1 -> LuatTuVung.TinhTrang.DUNG_1
                soPhienXong == 2 -> LuatTuVung.TinhTrang.DUNG_2
                else -> LuatTuVung.TinhTrang.DA_THUOC
            }
            // So phien da xong di kem chu khong de ben goi tu doan nguoc tu tinh
            // trang: [LuatTuVung.TinhTrang.VUA_SAI] che mat con so do, ma chinh no
            // quyet dinh hoi chieu nao - xem [LuatTuVung.chieuCho].
            Triple(tu, tinh, soPhienXong)
        }
    }

    fun ghiTraTu(t: TraTu) {
        writableDatabase.insert(
            "tra_tu", null,
            ContentValues().apply {
                put("tu_id", t.tuId)
                put("phien", t.phien)
                put("buoi", t.buoi.name)
                put("chieu", t.chieu.name)
                put("lan", t.lan)
                put("go", t.go)
                put("dung", if (t.dung) 1 else 0)
                put("goi_y", t.goiY)
                put("chiu", if (t.chiu) 1 else 0)
                put("giay", t.giay)
                put("luc", t.luc)
            }
        )
    }

    /** So GIAY duong tu vung da tra tu [tuLuc], de giu tran ngay. */
    fun giayTuVungTu(tuLuc: Long): Int =
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(giay), 0) FROM tra_tu WHERE luc >= ?",
            arrayOf(tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /**
     * Ty le dung ngay lan dau, trong khoang tu [tuLuc].
     *
     * DAY MOI LA CON SO TRA LOI CAU HOI CUA BA HUY: con co hoc tu vung khong. So phut
     * thi tra cho viec ngoi go, ai ngoi du lau cung duoc; con ty le lan dau thi khong
     * co cach nao co.
     *
     * Dem theo (tu, phien): moi lan mot tu duoc dua ra trong mot buoi la mot cau hoi,
     * va chi lan THU DAU TIEN cua no tinh vao day.
     */
    fun tyLeDungLanDau(tuLuc: Long): Pair<Int, Int> =
        readableDatabase.rawQuery(
            """
            SELECT COUNT(*), COALESCE(SUM(dau_dung), 0) FROM (
              SELECT tu_id, phien,
                     (SELECT dung FROM tra_tu x
                       WHERE x.tu_id = t.tu_id AND x.phien = t.phien
                       ORDER BY x.luc LIMIT 1) AS dau_dung
              FROM tra_tu t WHERE t.luc >= ?
              GROUP BY t.tu_id, t.phien
            )
            """.trimIndent(),
            arrayOf(tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(1) to it.getInt(0) else 0 to 0 }

    private fun Cursor.docTu() = TuVung(
        id = getString(getColumnIndexOrThrow("id")),
        bo = getString(getColumnIndexOrThrow("bo")).orEmpty(),
        mon = getString(getColumnIndexOrThrow("mon")).orEmpty(),
        unit = getInt(getColumnIndexOrThrow("unit")),
        tu = getString(getColumnIndexOrThrow("tu")).orEmpty(),
        loai = getString(getColumnIndexOrThrow("loai")).orEmpty(),
        am = getString(getColumnIndexOrThrow("am")).orEmpty(),
        nghia = getString(getColumnIndexOrThrow("nghia")).orEmpty(),
        thuTu = getInt(getColumnIndexOrThrow("thu_tu"))
    )

    // ------------------------------------------------------------------ tu biet

    /**
     * Con tu doc bung minh co trung khong - xem [TuBiet].
     *
     * DU LIEU DA NAM SAN trong cot khai_chac tu ban 4, chi la truoc do khong ai doc
     * lai. Cho duy nhat dung den no la mot cau ghep vao loi nhan ngay sau lan nop,
     * ma cau do bi lan nop sau ghi de - mot phan ung, khong phai mot cai guong.
     *
     * Chi dem nhung lan CON DUOC HOI (khai_chac >= 0). Lan khong duoc hoi thi con
     * khong khai gi ca, gop vao day la dem mot y kien khong ai noi ra.
     */
    fun tuBiet(tuLuc: Long): TuBiet =
        readableDatabase.rawQuery(
            """
            SELECT
              SUM(CASE WHEN khai_chac = 0 AND dung = 0 THEN 1 ELSE 0 END),
              SUM(CASE WHEN khai_chac = 0 AND dung = 1 THEN 1 ELSE 0 END),
              SUM(CASE WHEN khai_chac = 1 AND dung = 0 THEN 1 ELSE 0 END)
            FROM tra_loi
            WHERE luc >= ? AND khai_chac >= 0 AND cau_id <> ?
            """.trimIndent(),
            arrayOf(tuLuc.toString(), CAU_GOI)
        ).use { c ->
            if (!c.moveToFirst()) TuBiet(0, 0, 0)
            else TuBiet(c.getInt(0), c.getInt(1), c.getInt(2))
        }

    /**
     * Con hay sai kieu gi: dem tung nhan loi, nhieu nhat truoc.
     *
     * VI SAO CAN. Cho nay truoc do chi tra loi duoc "hom nay sai 3 cau", ma con so
     * do khong dung duoc vao viec gi ngoai viec bat con sua. "Ca thang sai dau 14
     * lan" thi lai la mot cau noi duoc voi con, va no chi ra dung cho phai ngoi lai
     * day - sai dau va nham cong thuc la hai viec khac han nhau.
     *
     * DEM TUNG LAN CHU KHONG DEM TUNG CAU. Mot cau sai ba lan cung mot kieu thi ba
     * lan do deu la that, va chinh cai lap lai moi la dau hieu. Dem theo cau se lam
     * phang dung cho can nhin.
     *
     * TINH CA LAN ON TAP, khac [soLanSai] von bo lan on ra. Ben kia dem de noi voi
     * con "cau nay con sua may lan", nen lan on khong phai la mot lan sai moi. Ben
     * nay hoi mot cau khac: con hay trat o dau. Trat lai dung cho cu trong luc on
     * la cau tra loi ro nhat cho cau hoi do.
     *
     * Ban cham truoc ban 5 khong co nhan nao, nen chung khong xuat hien o day -
     * bang nay chi tinh tu luc len ban do tro di.
     */
    fun thongKeLoi(tuLuc: Long): List<Pair<String, Int>> =
        readableDatabase.rawQuery(
            """
            SELECT loai_loi, COUNT(*) FROM tra_loi
            WHERE luc >= ? AND dung = 0 AND loai_loi IS NOT NULL AND loai_loi <> ''
            GROUP BY loai_loi ORDER BY COUNT(*) DESC
            """.trimIndent(),
            arrayOf(tuLuc.toString())
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getInt(1)) } }

    /**
     * Nhan loi con vap nhieu nhat trong khoang, kem so lan. Null la chua du de noi.
     *
     * VI SAO CO NGUONG. Bang [thongKeLoi] la cua Ba Huy, doc de biet nen ngoi day
     * con cai gi; con man hinh cua con thi khong duoc phep noi "con hay sai kieu
     * nay" khi cho do moi sai hai lan. Sai hai lan co the chi la hai hom mat tap
     * trung, ma dong chu do thi dua tre mang theo ca thang.
     *
     * BO [LoaiLoi.KHAC] ra khoi danh sach ung vien. Sau nhan kia deu chi duoc mot
     * viec lam tiep - xem lai dau, xem lai cong thuc. Rieng "kieu khac" thi khong:
     * no la thung rac cua sau nhan tren, va noi voi con rang no hay sai kieu khac
     * la mot cau vua dung vua vo dung.
     *
     * Danh sach da xep giam dan theo so lan nen chi can lay cai dau tien qua duoc
     * hai cua nay.
     */
    fun nhanHayVap(tuLuc: Long, toiThieu: Int = TOI_THIEU_VAP): Pair<String, Int>? =
        thongKeLoi(tuLuc).firstOrNull { (nhan, lan) ->
            nhan != LoaiLoi.KHAC && lan >= toiThieu
        }

    /**
     * Cau chua lam, nam trong nhung BAI ma con hay vap nhan [nhan].
     *
     * CHO CAN BIET TRUOC KHI DOC TIEP: nhan loi la thuoc tinh cua mot LAN LAM, nam
     * trong bang tra_loi. Ngan hang cau hoi khong co cot nao noi mot cau thuoc dang
     * kien thuc gi - co "dang" nhung do la TRAC_NGHIEM hay CAU_NHO, chuyen khac han.
     * Nen khong co duong thang tu "hay sai dau" sang "cac cau ve chuyen ve".
     *
     * Duong vong: lay ba BAI ma con vap nhan do nhieu nhat, roi rut cau chua lam
     * trong dung ba bai ay. Cung bai thi phan lon la cung dang toan - con sai dau
     * luc chuyen ve thi sai trong bai phuong trinh, va cau khac cung bai do cung bat
     * chuyen ve. Khong chac bang viec gan nhan dang toan cho tung cau trong ngan
     * hang, nhung cai do la gan tay cho ca nghin cau, va gan sai mot lan thi khong
     * ai phat hien ra.
     *
     * Bo cau TRAC_NGHIEM va KHONG_TINH: mot cau khoanh A B C D khong luyen duoc cai
     * dau hay cai buoc bien doi, ma do la ca ly do con duoc dua den day.
     *
     * Chi lay cau CHUA DUNG DEN BAO GIO, khong phai cau "chua lam dung". Cau dang
     * cho sua da nam san o duong khac ngoai man chinh, va dua lai vao day thi con
     * lam hai lan mot viec ma lan nay khong duoc tinh gio - xem [daXong].
     */
    fun cacCauLuyenTheoLoi(
        nhan: String,
        tuLuc: Long = System.currentTimeMillis() - CUA_SO_LUYEN_MS,
        gioiHan: Int = SO_CAU_LUYEN
    ): List<CauHoi> {
        if (nhan.isBlank() || gioiHan <= 0) return emptyList()

        data class Bai(val nguon: String, val ten: String)
        val hayVap = readableDatabase.rawQuery(
            """
            SELECT c.nguon, c.bai FROM tra_loi t
            JOIN cau_hoi c ON c.id = t.cau_id
            WHERE t.dung = 0 AND t.loai_loi = ? AND t.luc >= ? AND c.bai <> ''
            GROUP BY c.nguon, c.bai
            ORDER BY COUNT(*) DESC, MAX(t.luc) DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(nhan, tuLuc.toString(), SO_BAI_LUYEN.toString())
        ).use { c ->
            buildList { while (c.moveToNext()) add(Bai(c.getString(0), c.getString(1))) }
        }
        if (hayVap.isEmpty()) return emptyList()

        // Bai gan day nhat truoc: con vua vap o do tuan nay thi no gan hon bai vap
        // tu thang truoc. Moi bai lay du gioiHan roi cat o cuoi, de mot bai da lam
        // het khong lam ca danh sach ngan lai.
        return hayVap.flatMap { bai ->
            readableDatabase.rawQuery(
                """
                SELECT * FROM cau_hoi c
                WHERE c.nguon = ? AND c.bai = ?
                  AND c.dang NOT IN ('TRAC_NGHIEM', 'KHONG_TINH')
                  AND NOT EXISTS (SELECT 1 FROM tra_loi t
                                  WHERE t.cau_id = c.id AND t.luc >= ?)
                ORDER BY c.thu_tu
                LIMIT ?
                """.trimIndent(),
                arrayOf(bai.nguon, bai.ten, tuLuc.toString(), gioiHan.toString())
            ).use { c -> buildList { while (c.moveToNext()) add(c.docCauHoi()) } }
        }.take(gioiHan)
    }

    /**
     * Cau nay da sai may lan, khong tinh lan on tap.
     *
     * Dem de noi lai voi con mot cau cho dung: sua ba lan moi xong thi khac voi dung
     * ngay lan dau, va cai khac do dang duoc noi ra.
     */
    fun soLanSai(cauId: String, tuLuc: Long): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM tra_loi WHERE cau_id = ? AND luc >= ? AND dung = 0 AND on_tap = 0",
            arrayOf(cauId, tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** Cau nay da duoc tra gio cho mot lan on tap chua. */
    fun daOnTap(cauId: String, tuLuc: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM tra_loi WHERE cau_id = ? AND on_tap = 1 AND dung = 1 AND luc >= ? LIMIT 1",
            arrayOf(cauId, tuLuc.toString())
        ).use { it.moveToFirst() }

    /** Dem cau da lam dung trong mot quyen - de hien bang tien bo. */
    fun soCauDaXongCua(nguon: String, tuLuc: Long): Int =
        readableDatabase.rawQuery(
            """
            SELECT COUNT(DISTINCT t.cau_id) FROM tra_loi t
            JOIN cau_hoi c ON c.id = t.cau_id
            WHERE c.nguon = ? AND t.dung = 1 AND t.luc >= ?
            """.trimIndent(),
            arrayOf(nguon, tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /**
     * Cau nen lam them: chua lam bao gio, va nam trong chinh nhung BAI con vua sai.
     *
     * Sai mot cau thi lam them mot cau giong no moi va duoc cho hong - do la ly do
     * cua ham nay. Sach da chia san theo bai, ma mot bai la mot dang, nen "cung bai"
     * chinh la "cung dang" ma khong phai gan nhan gi them.
     *
     * Het cau trong nhung bai do thi lay tiep cau chua lam theo thu tu in trong sach:
     * con van co viec de lam, chi la khong con nham dung cho no vua trat.
     */
    fun cacCauNenLamThem(nguon: String, tuLuc: Long, gioiHan: Int): List<CauHoi> {
        if (gioiHan <= 0) return emptyList()
        val baiVuaSai = readableDatabase.rawQuery(
            """
            SELECT c.bai FROM tra_loi t
            JOIN cau_hoi c ON c.id = t.cau_id
            WHERE t.dung = 0 AND t.luc >= ? AND c.nguon = ?
            GROUP BY c.bai
            ORDER BY MAX(t.luc) DESC
            LIMIT 3
            """.trimIndent(),
            arrayOf(tuLuc.toString(), nguon)
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

        val chuaLam = { loc: String, arg: Array<String> ->
            readableDatabase.rawQuery(
                """
                SELECT * FROM cau_hoi c
                WHERE c.nguon = ? AND $loc
                  AND NOT EXISTS (SELECT 1 FROM tra_loi t
                                  WHERE t.cau_id = c.id AND t.dung = 1 AND t.luc >= ?)
                ORDER BY c.thu_tu
                LIMIT ?
                """.trimIndent(),
                arrayOf(nguon) + arg + arrayOf(tuLuc.toString(), gioiHan.toString())
            ).use { c -> buildList { while (c.moveToNext()) add(c.docCauHoi()) } }
        }

        val trongBaiVuaSai = if (baiVuaSai.isEmpty()) emptyList() else {
            val cho = baiVuaSai.joinToString(",") { "?" }
            chuaLam("c.bai IN ($cho)", baiVuaSai.toTypedArray())
        }
        if (trongBaiVuaSai.size >= gioiHan) return trongBaiVuaSai

        return (trongBaiVuaSai + chuaLam("1 = 1", emptyArray()))
            .distinctBy { it.id }
            .take(gioiHan)
    }

    /** Cau nay da co lan nao lam dung VA duoc tra gio chua. */
    fun daXong(cauId: String, tuLuc: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM tra_loi WHERE cau_id = ? AND dung = 1 AND luc >= ? LIMIT 1",
            arrayOf(cauId, tuLuc.toString())
        ).use { it.moveToFirst() }

    /**
     * Trong [ids], cau nao da lam dung va da tra gio.
     *
     * Hoi mot lan cho ca bai thay vi goi [daXong] cho tung cau: man chon bai ve ba
     * chuc cau mot luc, ba chuc lan mo cursor cho mot viec nhu the la phi.
     */
    fun daXongTrong(ids: List<String>, tuLuc: Long): Set<String> {
        if (ids.isEmpty()) return emptySet()
        val cho = ids.joinToString(",") { "?" }
        return readableDatabase.rawQuery(
            "SELECT DISTINCT cau_id FROM tra_loi " +
                "WHERE dung = 1 AND luc >= ? AND cau_id IN ($cho)",
            (listOf(tuLuc.toString()) + ids).toTypedArray()
        ).use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }
    }

    /**
     * Lan cham GAN NHAT cua moi cau - tuc la tinh trang hien tai cua tung cau.
     *
     * Chon dong bang MAX(id) chu khong phai MAX(luc). Hai lan cham cach nhau mot
     * phan nghin giay - hay trong bo test la cung mot moc thoi gian y het - thi
     * MAX(luc) tra ve CA HAI dong, va cau da sua dung van con hien ra la dang sai.
     * So thu tu dong thi khong bao gio trung.
     */
    fun moiNhatMoiCau(tuLuc: Long): List<TraLoi> =
        readableDatabase.rawQuery(
            """
            SELECT t.* FROM tra_loi t
            JOIN (SELECT cau_id, MAX(id) AS cuoi FROM tra_loi
                  WHERE luc >= ? GROUP BY cau_id) g
              ON t.id = g.cuoi
            ORDER BY t.luc, t.id
            """.trimIndent(),
            arrayOf(tuLuc.toString())
        ).use { c -> buildList { while (c.moveToNext()) add(c.docTraLoi()) } }

    /**
     * Cac cau dang sai, cho con sua lai.
     *
     * Lay lan cham gan nhat cua moi cau, va bo cau nao da co lan lam dung. Neu chi
     * loc "dong nao dung = 0" thi mot cau con da sua xong van con nam do mai mai,
     * vi lan sai hom truoc khong bao gio mat di.
     */
    fun dangChoSua(tuLuc: Long): List<TraLoi> {
        val daXong = readableDatabase.rawQuery(
            "SELECT DISTINCT cau_id FROM tra_loi WHERE dung = 1 AND luc >= ?",
            arrayOf(tuLuc.toString())
        ).use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }

        return moiNhatMoiCau(tuLuc)
            .filter { !it.dung && it.cauId != CAU_GOI && it.cauId !in daXong }
    }

    /** Tong so phut da cong trong khoang, bo qua [truCauId] neu co. */
    fun tongPhut(tuLuc: Long, truCauId: String? = null): Int {
        val sql = StringBuilder("SELECT SUM(phut) FROM tra_loi WHERE dung = 1 AND luc >= ?")
        val args = mutableListOf(tuLuc.toString())
        if (truCauId != null) {
            sql.append(" AND cau_id <> ?")
            args += truCauId
        }
        return readableDatabase.rawQuery(sql.toString(), args.toTypedArray())
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * Tong so phut duong ON da tra trong khoang.
     *
     * Tach khoi [tongPhut] vi hai con so giu hai cai tran khac nhau - xem
     * [vn.huytl.homeworkgate.data.LuatCongGio.TRAN_ON_MOI_NGAY].
     */
    fun tongPhutOnTap(tuLuc: Long): Int =
        readableDatabase.rawQuery(
            "SELECT SUM(phut) FROM tra_loi WHERE dung = 1 AND on_tap = 1 AND luc >= ?",
            arrayOf(tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** Co ban ghi nao cua [cauId] trong khoang nay khong. */
    fun coTrongKhoang(cauId: String, tuLuc: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM tra_loi WHERE cau_id = ? AND luc >= ? LIMIT 1",
            arrayOf(cauId, tuLuc.toString())
        ).use { it.moveToFirst() }

    /** Lich su cham cua mot cau, moi nhat truoc. De Ba Huy xem lai. */
    fun lichSuCua(cauId: String): List<TraLoi> =
        readableDatabase.query(
            "tra_loi", null, "cau_id = ?", arrayOf(cauId), null, null, "luc DESC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.docTraLoi()) } }

    // ------------------------------------------------------------ bang tien bo

    /**
     * Nhung con so cho man hinh cua chinh Le Hoa xem.
     *
     * Tat ca cac bang thong ke khac trong app deu la cua Ba Huy: con dung app gi,
     * luc nao, hoi AI nhung gi. Khong co cho nao de dua tre nhin lai viec cua no.
     * May dem duoc thi cho no thay.
     *
     * Dem theo CAU chu khong theo lan cham: mot cau sai roi sua ba lan van la mot
     * cau. Dem theo lan thi con lam sai nhieu lai ra so to hon.
     */
    /**
     * So cau dung theo tung ngay, de man Tien bo ve thanh mot day cot.
     *
     * Khoa la ngay dang "yyyy-MM-dd" theo gio may. Ngay khong co bai thi khong co
     * khoa - ben ve tu dien 0 vao, vi mot ngay trong cung la mot thong tin.
     */
    fun cauDungTheoNgay(tuLuc: Long): Map<String, Int> =
        readableDatabase.rawQuery(
            """
            SELECT strftime('%Y-%m-%d', luc / 1000, 'unixepoch', 'localtime') AS ngay,
                   COUNT(DISTINCT cau_id)
            FROM tra_loi WHERE dung = 1 AND luc >= ? AND cau_id <> ?
            GROUP BY ngay
            """.trimIndent(),
            arrayOf(tuLuc.toString(), CAU_GOI)
        ).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getInt(1)) }
        }

    fun tienBo(tuLuc: Long): TienBo {
        val db = readableDatabase
        val tu = tuLuc.toString()

        val dung = db.rawQuery(
            "SELECT COUNT(DISTINCT cau_id) FROM tra_loi WHERE dung = 1 AND luc >= ? AND cau_id <> ?",
            arrayOf(tu, CAU_GOI)
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        val theoMon = db.rawQuery(
            """
            SELECT COALESCE(NULLIF(mon, ''), 'Khác') AS m, COUNT(DISTINCT cau_id)
            FROM tra_loi WHERE dung = 1 AND luc >= ? AND cau_id <> ?
            GROUP BY m ORDER BY COUNT(DISTINCT cau_id) DESC
            """.trimIndent(),
            arrayOf(tu, CAU_GOI)
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getInt(1)) } }

        /*
         * Cau kho da go: tung sai, sau do lam lai dung.
         *
         * Con so nay dung o phia doi dien voi bang "con may cau can sua". Cung mot
         * su viec - lam sai - nhung mot ben la mon no, mot ben la viec da lam xong.
         * Mot dua tre sua ba lan moi dung dang duoc dem vao dau do.
         */
        val daGo = db.rawQuery(
            """
            SELECT COUNT(*) FROM (
              SELECT t.cau_id FROM tra_loi t
              WHERE t.luc >= ? AND t.cau_id <> ?
              GROUP BY t.cau_id
              HAVING MAX(CASE WHEN t.dung = 0 AND t.on_tap = 0 THEN 1 ELSE 0 END) = 1
                 AND MAX(CASE WHEN t.dung = 1 THEN 1 ELSE 0 END) = 1
            )
            """.trimIndent(),
            arrayOf(tu, CAU_GOI)
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        // Ngay theo gio may, khong phai gio UTC: nop bai luc 21h toi van la hom nay.
        val soNgay = db.rawQuery(
            """
            SELECT COUNT(DISTINCT date(luc / 1000, 'unixepoch', 'localtime'))
            FROM tra_loi WHERE luc >= ? AND cau_id <> ?
            """.trimIndent(),
            arrayOf(tu, CAU_GOI)
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        val phut = db.rawQuery(
            "SELECT SUM(phut) FROM tra_loi WHERE luc >= ?", arrayOf(tu)
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        val dangCho = dangChoSua(tuLuc).size

        return TienBo(
            soCauDung = dung,
            theoMon = theoMon,
            cauKhoDaGo = daGo,
            soNgayCoBai = soNgay,
            phutDaKiem = phut,
            cauDangChoSua = dangCho
        )
    }

    /**
     * Cau nay sai may lan truoc khi lam dung, tinh trong khoang.
     *
     * Dem lan sai THAT SU, tuc la nhung lan cham truoc lan dung dau tien. Sai ba
     * lan roi dung thi tra ve 3; dung ngay lan dau thi tra ve 0.
     */
    fun soLanSaiTruocKhiDung(cauId: String, tuLuc: Long): Int =
        readableDatabase.rawQuery(
            """
            SELECT COUNT(*) FROM tra_loi
            WHERE cau_id = ? AND luc >= ? AND dung = 0 AND on_tap = 0
              AND id < (SELECT MIN(id) FROM tra_loi
                        WHERE cau_id = ? AND luc >= ? AND dung = 1)
            """.trimIndent(),
            arrayOf(cauId, tuLuc.toString(), cauId, tuLuc.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** Bo cac ban ghi qua cu. Goi luc mo app, khong phai moi lan ghi. */
    fun donCu(truocLuc: Long) {
        writableDatabase.delete("tra_loi", "luc < ?", arrayOf(truocLuc.toString()))
    }

    /**
     * Thay toan bo so cai bang ban keo ve tu Firestore.
     *
     * Thay chu khong gop: cho nay chi chay luc khoi phuc sau khi cai lai app, luc do
     * trong may von khong co gi. Gop thi moi lan khoi phuc lai nhan doi so dong, va
     * so phut da cong trong ngay se nhan doi theo.
     */
    fun napSoCai(cac: List<TraLoi>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("tra_loi", null, null)
            cac.forEach { ghiTraLoi(it) }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
        // So tren Firestore van giu khoa cu cua nhung cau da noi, keo ve la khoa cu
        // quay lai. Noi lai ngay, dung doi toi lan mo app sau.
        noiCauDuongCu()
    }

    /**
     * Doi khoa cua nhung cau da nop qua "Bai khac" sang ma cau trong sach, neu sach
     * co dung cau do.
     *
     * VI SAO CAN. Truoc khi mot mon co sach trong may, con nop bai mon do qua duong
     * cu, va so cai ghi khoa bang de bai AI chep lai ("tu:..."). Nap sach xong, cung
     * cau do mang ma sach ("van8t1:B1.C5"), ma khong co gi noi hai khoa: cau da tra
     * gio thanh cau chua lam, chup lai trang vo cu la duoc tra gio lan hai, va man
     * "Lam them" moi con lam lai bai da nop. Sach Ngu van vao may ngay 24/9/2026, khi
     * nam hoc da di duoc ba tuan.
     *
     * So phan cot loi cua de AI chep voi de trong sach cung mon - xem [loiDe]. Khop
     * thi doi cau_id cua MOI dong cua khoa cu, nen ca lich su sai roi sua di theo, va
     * khong sinh dong nao moi: so phut va so cau da lam giu nguyen. Mot khoa cu dinh
     * toi nhieu cau sach thi bo qua: noi nham la con mat gio cua mot cau chua lam, con
     * bo qua thi chi quay ve duong cu nhu truoc. Thu tren ca ngan hang ngay 24/9/2026,
     * gia nhu AI chep dung de sach: khong noi nham cau nao, bo qua 2 trong 1361 cau.
     *
     * Goi luc mo app va sau khi keo so cu ve. Chay lai bao nhieu lan cung vay: khoa
     * da doi thi khong con dang "tu:" de doi nua.
     *
     * @return so khoa cu da doi.
     */
    fun noiCauDuongCu(): Int {
        val db = writableDatabase
        // Moi khoa cu mot dong: mon va de cua lan nop gan nhat.
        val cu = db.rawQuery(
            """
            SELECT t.cau_id, t.mon, t.de FROM tra_loi t
            JOIN (SELECT MAX(id) AS cuoi FROM tra_loi
                  WHERE cau_id LIKE ? GROUP BY cau_id) m ON t.id = m.cuoi
            """.trimIndent(),
            arrayOf(SoCaiBai.DAU_NGOAI_SACH + "%")
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(Triple(c.getString(0), c.getString(1).orEmpty(), loiDe(c.getString(2))))
                }
            }
        }
        if (cu.isEmpty()) return 0

        val sachTheoMon = cu.map { it.second }.filter { it.isNotBlank() }.toSet()
            .associateWith { mon ->
                db.rawQuery("SELECT id, de FROM cau_hoi WHERE mon = ?", arrayOf(mon)).use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(0) to loiDe(c.getString(1))) }
                }
            }

        var doi = 0
        db.beginTransaction()
        try {
            for ((khoa, mon, loi) in cu) {
                val coThe = sachTheoMon[mon].orEmpty().filter { coTheLa(loi, it.second) }
                // Giong het mot cau thi lay cau do. Khong thi chi noi khi chi co mot cau
                // sach dinh toi: cau nho 1.17a, 1.17b chung phan dan, AI chi chep phan
                // dan thi khong biet la cau nao.
                val chon = coThe.singleOrNull { it.second == loi }
                    ?: coThe.singleOrNull()?.takeIf { duChac(loi, it.second) }
                    ?: continue
                val soDong = db.update(
                    "tra_loi",
                    ContentValues().apply { put("cau_id", chon.first) },
                    "cau_id = ?",
                    arrayOf(khoa)
                )
                if (soDong > 0) doi++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return doi
    }

    /**
     * Phan cot loi cua mot de bai, de so de AI chep voi de trong sach.
     *
     * Bo ghi chu nha minh them vao de sach ("(văn bản ...)", "(Thực hành tiếng Việt:
     * ...)", "(in đậm: ...)") va nhan so o dau ("Câu 3.", "2.26a."): AI khong chep ghi
     * chu, con nhan so thi co lan chep co lan khong. Ngoac cua chinh sach thi GIU: o
     * Toan do la bieu thuc, bo di thi "(2b + 1)^2" voi "(x + 3)^2" thanh mot. Phan con
     * lai chuan hoa y het khoa so cai, xem [SoCaiBai.chuanHoa].
     */
    private fun loiDe(de: String?): String =
        SoCaiBai.chuanHoa(de.orEmpty().replace(GHI_CHU, " ").replace(NHAN_DAU, ""))

    /**
     * Hai de (da qua [loiDe]) co the la mot cau khong: giong het, hoac mot ben nam
     * tron trong ben kia - AI chep them nhan so, hay chi chep cau dau cua de. Ben ngan
     * phai du [KHOP_TOI_THIEU] ky tu: "rútgọnbiểuthức" nam trong ca chuc cau Toan.
     */
    private fun coTheLa(cu: String, sach: String): Boolean = when {
        cu.isEmpty() || sach.isEmpty() -> false
        cu == sach -> true
        cu.length >= KHOP_TOI_THIEU && sach.contains(cu) -> true
        sach.length >= KHOP_TOI_THIEU && cu.contains(sach) -> true
        else -> false
    }

    /** Du chac de noi: AI chep them thi duoc, chep thieu thi phai con sau phan muoi de sach. */
    private fun duChac(cu: String, sach: String): Boolean =
        cu == sach || cu.contains(sach) || cu.length * 10 >= sach.length * 6

    fun xoaHetTraLoi() {
        writableDatabase.delete("tra_loi", null, null)
    }

    private fun Cursor.docCauHoi() = CauHoi(
        id = getString(getColumnIndexOrThrow("id")),
        mon = getString(getColumnIndexOrThrow("mon")),
        nguon = getString(getColumnIndexOrThrow("nguon")),
        chuong = getString(getColumnIndexOrThrow("chuong")).orEmpty(),
        bai = getString(getColumnIndexOrThrow("bai")).orEmpty(),
        nhom = getString(getColumnIndexOrThrow("nhom")).orEmpty(),
        ma = getString(getColumnIndexOrThrow("ma")),
        de = getString(getColumnIndexOrThrow("de")),
        trang = getInt(getColumnIndexOrThrow("trang")),
        dang = getString(getColumnIndexOrThrow("dang")).orEmpty(),
        thuTu = getInt(getColumnIndexOrThrow("thu_tu"))
    )

    private fun Cursor.docTraLoi() = TraLoi(
        cauId = getString(getColumnIndexOrThrow("cau_id")),
        mon = getString(getColumnIndexOrThrow("mon")).orEmpty(),
        ma = getString(getColumnIndexOrThrow("ma")).orEmpty(),
        de = getString(getColumnIndexOrThrow("de")).orEmpty(),
        ketQua = getString(getColumnIndexOrThrow("ket_qua")).orEmpty(),
        baiLam = getString(getColumnIndexOrThrow("bai_lam")).orEmpty()
            .split(NGAN_DONG).filter { it.isNotEmpty() },
        dongSai = getInt(getColumnIndexOrThrow("dong_sai")),
        onTap = getInt(getColumnIndexOrThrow("on_tap")) == 1,
        khaiChac = getInt(getColumnIndexOrThrow("khai_chac")),
        conNoi = getString(getColumnIndexOrThrow("con_noi")).orEmpty(),
        loaiLoi = getString(getColumnIndexOrThrow("loai_loi")).orEmpty(),
        dung = getInt(getColumnIndexOrThrow("dung")) == 1,
        phut = getInt(getColumnIndexOrThrow("phut")),
        nhanXet = getString(getColumnIndexOrThrow("nhan_xet")).orEmpty(),
        luc = getLong(getColumnIndexOrThrow("luc"))
    )

    companion object {
        private const val TEN = "kho_bai.db"
        private const val BAN = 9

        /**
         * Dau ngan giua cac dong bai lam khi cat vao mot o.
         *
         * Ky tu don vi (U+001F) chu khong phai xuong dong: bai toan co the co dau
         * xuong dong trong mot dong, con ky tu nay thi khong ban phim nao go ra duoc.
         */
        private const val NGAN_DONG = "\u001F"

        /**
         * Khoa gia cho ban ghi "hom nay da tinh tron goi vo dan do".
         *
         * Nam chung bang voi cau that vi no cung la mot khoan gio da tra, va cung
         * phai chi tra mot lan mot ngay. Dau @ de khong bao gio dung ma sach that.
         */
        const val CAU_GOI = "@goi-dan-do"

        /** Ghi chu nha minh them vao de sach, xem [NganHang.SACH]. Dung o [loiDe]. */
        private val GHI_CHU = Regex(
            """\((?:văn bản |Thực hành tiếng Việt: |Phiếu học tập số |Bài 10: |[^()]*in đậm: )[^()]*\)"""
        )

        /** Nhan so o dau de bai: "Câu 3.", "Bài 2:", "2.26a.", "3)". Xem [loiDe]. */
        private val NHAN_DAU = Regex("""^\s*(?:[Cc]âu|[Bb]ài)?\s*\d+(?:\.\d+)*[a-z]?\s*[.:)]\s*""")

        /** De ngan hon chung nay thi chi noi khi giong het. Xem [coTheLa]. */
        private const val KHOP_TOI_THIEU = 30

        /**
         * Sau bao nhieu ngay thi mot cau den hen on lai.
         *
         * Lan dau tinh tu luc con SUA XONG cau do, cac lan sau tinh tu lan on truoc.
         * Het day nay thi thoi, coi nhu cau do da thuoc.
         *
         * Ba moc thua dan: ba ngay, muoi ngay, mot thang. Du thua de khong thanh
         * viec vat moi toi, du day de con chua kip quen han.
         */
        val KHOANG_HEN_NGAY = listOf(3, 10, 30)

        /**
         * Vap bay nhieu lan thi man hinh cua con moi noi ra. Xem [nhanHayVap].
         *
         * Ba: du de khong phai mot hom mat tap trung, ma van con trong mot thang de
         * dong chu do con kip co ich.
         */
        const val TOI_THIEU_VAP = 3

        /** Rut cau luyen tu bay nhieu bai con hay vap nhat. Xem [cacCauLuyenTheoLoi]. */
        private const val SO_BAI_LUYEN = 3

        /**
         * Nhin lai bay nhieu ngay de biet con hay vap o BAI nao.
         *
         * Co dinh mot thang, khong theo cai nut 7/30 ngay ben man tien bo. Hai noi
         * goi ham nay - cai the de quyet dinh co hien nut khong, va man luyen de ve
         * danh sach - phai ra cung mot ket qua, khong thi con bam nut "Làm thử 3 câu"
         * roi sang man ben thay ba cau khac han.
         */
        private const val CUA_SO_LUYEN_MS = 30L * 24 * 60 * 60_000L

        /**
         * Mot lan luyen bay nhieu cau.
         *
         * Ba, va khong nen hon: day la viec con TU chon lam them sau khi da xong bai
         * co giao. Mot danh sach dai thi no dong man hinh lai.
         */
        const val SO_CAU_LUYEN = 3

        @Volatile
        private var ban: KhoBai? = null

        fun get(context: Context): KhoBai =
            ban ?: synchronized(this) {
                ban ?: KhoBai(context).also { ban = it }
            }
    }
}
