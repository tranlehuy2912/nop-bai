package vn.huytl.homeworkgate.kho

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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
              phut   INTEGER NOT NULL,
              luc    INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_tra_the ON tra_the(the_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_tra_the_luc ON tra_the(luc)")
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

    /** Dem the den luot ma khong doc ca bo ra - cho man chon bo. */
    fun soTheDenLuot(bo: String, bayGio: Long = System.currentTimeMillis()): Int =
        cacTheDenLuot(bo, Int.MAX_VALUE, bayGio).size

    fun ghiTraThe(t: TraThe) {
        writableDatabase.insert(
            "tra_the", null,
            ContentValues().apply {
                put("the_id", t.theId)
                put("go", t.go)
                put("dung", if (t.dung) 1 else 0)
                put("phut", t.phut)
                put("luc", t.luc)
            }
        )
    }

    /** So phut duong hoc thuoc da tra tu [tuLuc], de giu tran ngay. */
    fun phutTheTu(tuLuc: Long): Int =
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(phut), 0) FROM tra_the WHERE luc >= ?",
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
    }

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
        private const val BAN = 6

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

        @Volatile
        private var ban: KhoBai? = null

        fun get(context: Context): KhoBai =
            ban ?: synchronized(this) {
                ban ?: KhoBai(context).also { ban = it }
            }
    }
}
