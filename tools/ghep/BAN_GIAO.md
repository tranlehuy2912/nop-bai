# Bàn giao: bài máy giao, bàn phím ghép, sao là phút

Viết ngày 29/09/2026 tối, để làm tiếp trên máy khác. Code ở nhánh `bai-may-giao` của cả ba repo
(`nop-bai`, `bang-dieu-khien`, `cho-gio-choi`). Nhánh `main` chưa có gì của đợt này.

## Luật đã chốt (anh Huy, 29/09/2026, không bàn lại)

- Bài máy giao (làm thêm, luyện chỗ hay vấp, ôn lại, Giải đề) của Toán, KHTN, Tiếng Anh làm trên
  tablet bằng bàn phím ghép riêng từng câu. Không viết tay. Câu vẽ bỏ khỏi bài máy giao.
- Sao là phút: ghép chữ 2, ghép câu 2, trắc nghiệm 1, đúng/sai 1, biểu thức và xếp bước bằng số
  dòng lời giải (tối đa 20), ô chọn mỗi ô 1. Đoạn văn Anh: phần 1 xếp câu về bạn A (1 sao mỗi câu),
  phần 2 ghép theo câu gợi ý về bạn thân thật B (mỗi dòng hai ô, 2 sao mỗi câu, tên tự gõ).
- Sai một lần mất một sao (ô sai mỗi ô một sao), bớt phím nhiễu. Hết sao thì thử thêm một lần
  (0 phút), vẫn sai thì hiện lời giải. Làm lại mở sau 24 giờ, chỉ cộng phần hơn lần tốt nhất; chưa
  đủ sao thì cứ 24 giờ mở lại. Ôn lại hẹn 3, 10, 20, 30 ngày là vòng sao mới. Không chú thích luật
  trên app, không rung, không chuỗi đúng.
- Trần riêng: vở dặn dò 45 (gói và tính lẻ chung trần), làm trên máy và Giải đề 90, ôn lại 30,
  Kiểm tra bài 20, Dò từ vựng 30. Tối đa 215 phút/ngày. Bỏ trần chung 135.
- Quỹ giờ chơi: chỉ phút làm trên máy bị trần cắt mới vào quỹ; không hết hạn; hiện trên tablet.
  Ba Huy cấp bằng nút trên Bảng điều khiển (lệnh `CAPQUY`) hoặc `/quy N` trên Telegram.
- Chụp ảnh chỉ còn bài vở dặn dò (bài cô giao, bài khác, môn chưa có sách đều tính là dặn dò).
  Hôm chưa có gói: tự luận 1 phút mỗi dòng, trắc nghiệm 1 phút mỗi câu đúng, không sàn. Hôm đã có
  gói thì ẩn Nộp bài và Nộp lại câu sai; câu sai thuộc ngân hàng thành câu làm trên máy sau 24 giờ.
- Câu Claude chưa chắc hoặc câu đúng mà thiếu số dòng: không tự cấp giờ; điện thoại hiện ba nút
  Đúng (kèm số dòng), Sai, Chụp lại (lệnh `XUCAU`).
- Chỉ giao câu trong phần Lê Hòa đã học. Ngữ văn bỏ làm thêm và ôn lại. Môn chưa có sách gộp
  thành dòng "Bài môn khác". Dòng chụp vở dặn dò đưa ra màn chính.
- Hình thật cho Toán, KHTN làm sau cùng.

## Đã xong (build qua; chưa chạy thử trên máy ảo)

- Tablet: `kho/Ghep.kt`, `data/LuatGhep.kt`, `data/LamTrenMay.kt`, `data/QuyGio.kt`,
  `data/LuotDangLam.kt`, `ui/KhungGhep.kt`, `ui/LamBaiActivity.kt`, cùng khoảng 45 file cũ sửa theo
  luật mới. `KhoBai.BAN = 11`. Đã khai sách `sbtanh8` trong `NganHang` nhưng chưa có file dữ liệu.
- Điện thoại: quỹ và nút cấp quỹ ở thẻ Bảng, thẻ ba nút xử câu ở màn bài (`data/XuCau.kt`), bỏ mục
  trần ngày trong Cài đặt, lời nhờ Claude luôn ghi số dòng. `testDebugUnitTest` qua 113 test.
- `Duong.kt` ba bản giống nhau, thêm `F_QUY_GIO`, `Lenh.CAP_QUY`, `Lenh.XU_CAU`.
- Công cụ dữ liệu ở thư mục này: `DINH_DANG.md` (định dạng), `kiem.py` (bộ kiểm), `cat_hinh.py`
  (cắt hình), `gop.py` (gộp vào file sách, tăng `ban`).

## Còn phải làm, theo thứ tự

1. Test tablet. 12 lớp cũ đã sửa theo luật mới, biên dịch được, CHƯA CHẠY lần nào. Chạy từng lớp
   trên `pad5` (`-e class`, không chạy cả bộ, không chạy `Manual*`). Viết bốn lớp mới: `GhepTest`
   (so với `kiem.py`, gồm so biểu thức không kể thứ tự), `LuatGhepTest`, `QuyGioTest`,
   `LamTrenMayTest`.
2. Lỗi đã biết: lệnh `SUACHAM` với câu đúng mà Claude thiếu số dòng thì ghi câu là xong với 0 phút,
   lẽ ra phải chờ Ba Huy. Sửa ở `ThiHanhLenh.suaCham` và `SuaCham.ghi` (xét `bang.thieuDong`). Test
   đỏ đang giữ lỗi này: `SuaChamLenhTest.cau_claude_cham_khong_co_dong_bai_lam_thi_khong_bi_ghi_xong_voi_0_phut`.
3. Soạn dữ liệu ghép, mỗi đợt khoảng 5 tác tử (lần trước 20 tác tử cùng lúc thì hết giới hạn phiên):
   - Đã xong: SBT Anh Unit 1 (68 câu), SBT Toán tập 1 chương I (65 câu), cả hai 0 lỗi `kiem.py`.
   - SBT Anh Unit 2 có JSON nhưng thiếu ảnh `2.A2.5`, `2.A2.6`.
   - Chưa có: SBT Anh Unit 3 tới 12, Test Yourself 1 tới 4 (Unit 4, 8 có vài ảnh cắt dở); SBT Toán
     các chương còn lại; SGK Toán; SBT và SGK KHTN (KHTN soạn thử trước để viết hướng dẫn riêng).
   - Thứ tự và mẫu lời giao việc ở `HANG_CHO.md` trong gói dữ liệu nháp.
4. Gộp: `python3 tools/ghep/gop.py sach sbtanh8 u01.json ... --hinh-tu <thư mục hinh>`, rồi
   `python3 tools/ghep/kiem.py --tat-ca`.
5. Cắt hình Toán, KHTN. Còn thiếu bản quét SBT Toán 8 hai tập (anh Huy chưa gửi).
6. Chạy thử trên `pad5` và `dt_bahuy`. Cài lên tablet thật cần đúng khoá ký bản release (chưa xác
   định, xem `CLAUDE.md`).

Chưa báo anh Huy: lịch ôn cũ chỉ đưa lại câu từng sai rồi sửa đúng, không phải mọi câu đúng. Code
mới tính thêm câu làm trên máy bị mất sao là "từng sai".

## Dữ liệu nháp không nằm trong git

Repo trên GitHub đang công khai, nên dữ liệu có chữ và ảnh chép từ sách không đưa vào đây. Gói dữ
liệu nháp (hướng dẫn soạn `HUONG_DAN.md` của Anh và `HUONG_DAN_TOAN.md`, `HANG_CHO.md`, công cụ
`cong_cu/`, các file JSON đã soạn, ảnh đã cắt) là file `ghep-nhap-2026-09-29.zip`, mang sang bằng
đường riêng. Ảnh chụp từng trang SBT Anh không mang, sinh lại từ PDF được.

Sách nguồn (máy cũ để ở `~/Downloads/sgk/`): SBT Tiếng Anh 8 Global Success, SGK Toán 8 tập 1 và 2,
SGK KHTN 8 KNTT, SGK Tiếng Anh 8, và docx SBT KHTN 8 KNTT của thuvienhoclieu.com.
