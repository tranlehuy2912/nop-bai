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
              dung     INTEGER NOT NULL,
              phut     INTEGER NOT NULL,
              nhan_xet TEXT,
              luc      INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX ix_tra_cau ON tra_loi(cau_id)")
        db.execSQL("CREATE INDEX ix_tra_luc ON tra_loi(luc)")
    }

    /**
     * Doi ban thi lam lai tu dau, KHONG giu du lieu cu.
     *
     * Kho nay khong phai noi giu thu khong lay lai duoc: ngan hang cau hoi nap lai
     * tu file trong app, con cau tra loi cu chi dang gia trong vai thang. Viet ham
     * chuyen doi cho tung ban la cong cho mot viec khong ai can den.
     */
    override fun onUpgrade(db: SQLiteDatabase, cu: Int, moi: Int) {
        db.execSQL("DROP TABLE IF EXISTS cau_hoi")
        db.execSQL("DROP TABLE IF EXISTS tra_loi")
        onCreate(db)
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
            SELECT trang, COUNT(*), MIN(thu_tu), MIN(bai)
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
                            bai = c.getString(3).orEmpty()
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
                put("dung", if (t.dung) 1 else 0)
                put("phut", t.phut)
                put("nhan_xet", t.nhanXet)
                put("luc", t.luc)
            }
        )
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

    /** Bo cac ban ghi qua cu. Goi luc mo app, khong phai moi lan ghi. */
    fun donCu(truocLuc: Long) {
        writableDatabase.delete("tra_loi", "luc < ?", arrayOf(truocLuc.toString()))
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
        dung = getInt(getColumnIndexOrThrow("dung")) == 1,
        phut = getInt(getColumnIndexOrThrow("phut")),
        nhanXet = getString(getColumnIndexOrThrow("nhan_xet")).orEmpty(),
        luc = getLong(getColumnIndexOrThrow("luc"))
    )

    companion object {
        private const val TEN = "kho_bai.db"
        private const val BAN = 2

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

        @Volatile
        private var ban: KhoBai? = null

        fun get(context: Context): KhoBai =
            ban ?: synchronized(this) {
                ban ?: KhoBai(context).also { ban = it }
            }
    }
}
