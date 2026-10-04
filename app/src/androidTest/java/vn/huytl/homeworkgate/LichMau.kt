package vn.huytl.homeworkgate

import org.junit.rules.ExternalResource
import vn.huytl.homeworkgate.data.BanLich
import vn.huytl.homeworkgate.data.LichDangDung

/**
 * Ban lich dung yen cho test: lich 8A15 tu 05/10/2026, chep ngay 4/10/2026.
 *
 * Test tinh theo ngay that ("vo thu Tu 16/9, Toan han thu Hai 21/9"). Tu khi lich that nam
 * tren Firestore va doi khong can sua code (xem BanLich), test phai chay tren mot ban khong
 * doi theo, khong thi moi lan truong doi lich lai hong mot loat test ma app khong sai gi.
 *
 * Lop test nao tinh theo lich thi khai `@get:Rule val lich = LichMau.Rule()`. Ban thu tu dong
 * (tools/web.py) dat ban nay bang ManualBang lichmau.
 */
object LichMau {

    const val JSON = """
{
  "phienBan": 1,
  "ghiChu": "Thời khoá biểu 8A15 năm học 2026-2027, áp dụng từ 05/10/2026",
  "lop": "8A15",
  "phutMoiTiet": 45,
  "gioTiet": {
    "sang": {"1": "07:15", "2": "08:00", "3": "09:15", "4": "10:00", "5": "10:45"},
    "chieu": {"1": "12:45", "2": "13:30", "3": "14:15", "4": "15:30", "5": "16:15"}
  },
  "buongMay": {
    "chieu": "11:30",
    "sang": {"2": "08:45", "6": "08:45"}
  },
  "sang": {
    "2": {"3": "Giáo dục thể chất", "4": "Giáo dục thể chất"},
    "6": {"3": "Tin học", "4": "Tin học"}
  },
  "chieu": {
    "2": {"1": "Tiếng Anh", "2": "Toán", "3": "Toán", "4": "Trải nghiệm hướng nghiệp", "5": "Chào cờ"},
    "3": {"1": "Trải nghiệm hướng nghiệp", "2": "STEM", "3": "Âm nhạc", "4": "Công nghệ", "5": "Kỹ năng"},
    "4": {"1": "Trí tuệ nhân tạo", "2": "Mỹ thuật", "3": "Toán", "4": "Toán", "5": "Trải nghiệm hướng nghiệp"},
    "5": {"1": "Khoa học tự nhiên", "2": "Khoa học tự nhiên", "3": "Ngữ văn", "4": "Lịch sử - Địa lý", "5": "Lịch sử - Địa lý"},
    "6": {"1": "Ngữ văn", "2": "Ngữ văn", "3": "Giáo dục công dân", "4": "Khoa học tự nhiên", "5": "Khoa học tự nhiên"},
    "7": {"1": "Tiếng Anh", "2": "Tiếng Anh", "3": "Lịch sử - Địa lý", "4": "Giáo dục địa phương", "5": "Ngữ văn"}
  },
  "ngayNghi": [
    {"tu": "2027-01-01", "den": "2027-01-01", "ten": "Tết Dương lịch"},
    {"tu": "2027-02-01", "den": "2027-02-10", "ten": "nghỉ Tết Nguyên đán"},
    {"tu": "2027-04-16", "den": "2027-04-16", "ten": "Giỗ tổ Hùng Vương"},
    {"tu": "2027-04-30", "den": "2027-05-01", "ten": "lễ 30 tháng 4 và 1 tháng 5"}
  ],
  "hetNamHoc": "2027-05-31"
}
"""

    val ban: BanLich by lazy { BanLich.doc(JSON) }

    /** Dung [ban] trong luc chay tung test, xong tra lai ban cu. */
    class Rule : ExternalResource() {
        private var cu: BanLich? = null

        override fun before() {
            cu = LichDangDung.ban
            LichDangDung.dungTam(ban)
        }

        override fun after() {
            cu?.let(LichDangDung::dungTam)
        }
    }
}
