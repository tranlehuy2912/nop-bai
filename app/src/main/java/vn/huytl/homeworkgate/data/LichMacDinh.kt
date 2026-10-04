package vn.huytl.homeworkgate.data

/**
 * Ban lich mang san trong app, dung khi chua nhan duoc ban nao tu Firestore.
 *
 * Tu 4/10/2026 lich dang dung nam o Firestore (xem [BanLich]). Ban nay chi la cho dua: may
 * moi cai, hay chua lan nao vao duoc mang. Doi lich thi doi o Firestore truoc, roi chep ban
 * moi vao day cho lan build sau; app tu lay ban co [BanLich.phienBan] cao hon.
 *
 * Viet JSON trong mot chuoi Kotlin chu khong de file trong resources: khong co buoc doc file
 * nao co the hong luc chay. tools/lich/xuat.py lay JSON tu file nay ra mot dong de dan len
 * Firebase console, xem tools/lich/CAP_NHAT_LICH.md.
 *
 * Bang dieu khien giu mot ban chep y het file nay, chi khac dong package.
 */
object LichMacDinh {

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
}
