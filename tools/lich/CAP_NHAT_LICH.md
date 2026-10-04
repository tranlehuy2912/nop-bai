# Cập nhật lịch học

Từ 04/10/2026 thời khoá biểu và ngày nghỉ của Lê Hòa nằm trên Firestore, không nằm trong code.
Đổi lịch thì Claude Code ghi bản mới lên Firebase, không build hay cài lại app. Anh Huy chọn
cách này sau khi lịch đổi ba lần trong một tháng (07/09, bỏ AVNN 27/09, xếp lại buổi chiều 05/10).

## Lịch nằm ở đâu

- Firestore, dự án thật `nop-bai-4934d`: collection `lichhoc`, document mang đúng mã nhà, một
  trường `json` kiểu chuỗi. Dự án thử `homework-gate-thu` có cùng chỗ cho máy ảo.
- Luật Firestore cho tablet và Bảng điều khiển của nhà đó đọc, không máy nào ghi được (xem
  `firestore.rules` của bang-dieu-khien). Console Firebase không đi qua luật nên ghi được.
- Bản mang sẵn trong app: `app/src/main/java/vn/huytl/homeworkgate/data/LichMacDinh.kt`, Bảng
  điều khiển giữ bản chép y hệt. App dùng bản này khi chưa nhận được bản nào từ Firestore.
- App luôn dùng bản có `phienBan` cao nhất trong ba bản: bản trên Firestore, bản đã lưu trong
  máy, bản mang sẵn. Bản mới phải tăng `phienBan`, không bao giờ hạ xuống.

## Khuôn JSON

Xem `LichMacDinh.kt`. Thứ viết bằng số như trên thời khoá biểu: `"2"` là thứ hai, `"7"` là thứ
bảy. Tiết là số từ 1. Giờ dạng `"07:15"`, ngày dạng `"2027-01-31"`.

- `phienBan`: số nguyên, tăng 1 mỗi lần đổi.
- `ghiChu`: một câu tả bản này, hiện trong nhật ký tablet khi nhận ("Nhận lịch học mới bản 2: ...").
- `lop`, `phutMoiTiet`.
- `gioTiet.sang`, `gioTiet.chieu`: giờ vào từng tiết.
- `buongMay.chieu`: mốc buông máy mọi buổi chiều. `buongMay.sang`: mốc buông máy từng buổi sáng,
  thứ nào không ghi thì 30 phút trước giờ vào học. Mốc buông máy do anh Huy đặt, đổi lịch mà giờ
  vào học đổi thì hỏi anh.
- `sang`, `chieu`: thứ → tiết → tên môn, chỉ ghi tiết có học. Không học buổi đó thì bỏ hẳn thứ đó.
  Tên môn giữ đúng chữ đang dùng ("Khoa học tự nhiên", "Lịch sử - Địa lý"): nhắc bài theo vở dặn
  dò, kiểm tra, màu ô, tên gọn đều so theo tên môn.
- `ngayNghi`: danh sách `{tu, den, ten}`, tính cả hai đầu. Chủ nhật tự nghỉ, không cần ghi.
- `hetNamHoc`: ngày học cuối cùng, sau ngày này app thôi nhắc.

Khoá lạ, thiếu khoá, giờ sai, tiết chồng nhau, mốc buông máy sau giờ vào học: app từ chối cả bản,
giữ bản đang dùng, ghi nhật ký "Lịch trên Firebase hỏng, vẫn dùng bản N: <chỗ hỏng>".

## Các bước khi anh Huy gửi lịch mới

1. Sửa JSON trong `LichMacDinh.kt` của nop-bai, tăng `phienBan`. Chạy
   `python3 tools/lich/xuat.py --tom` và soát từng ô với ảnh anh gửi.
2. Chép file sang `bang-dieu-khien/app/src/main/java/vn/huytl/bangdieukhien/data/LichMacDinh.kt`
   (chỉ khác dòng `package`), chạy `sh tools/kiem-duong.sh`.
3. Ở bang-dieu-khien chạy `./gradlew :app:testDebugUnitTest --tests '*BanLichTest'`. Lớp này đọc
   bản trong `LichMacDinh.kt` bằng đúng hàm của app; hỏng là bản mới có chỗ sai.
4. Không phải sửa test: các test tính theo ngày chạy trên `LichMau`, một bản đứng yên.
5. Commit hai repo, đẩy GitHub. Không cần build APK: bản mang sẵn chỉ dùng cho lần cài sau.
6. Hỏi anh Huy trước, rồi ghi lên Firebase như mục dưới.
7. Kiểm tablet đã nhận: trên console mở `nha/<mã nhà>/hop/trangthai`, trường `lich` phải bằng
   `phienBan` mới; nhật ký `nha/<mã nhà>/nhatky/<yyyy-MM-dd>` có dòng "Nhận lịch học mới bản N".
   Tablet đang tắt hay mất mạng thì chỉ thấy khi nó có mạng lại.

## Ghi lên Firebase bằng Chrome

Dùng browser-harness nối Chrome của anh Huy (đã đăng nhập console, giao diện tiếng Pháp). Đã
chạy ngày 04/10/2026 trên dự án thử.

- Lấy JSON một dòng: `python3 tools/lich/xuat.py`.
- Sửa bản đang có: mở
  `https://console.firebase.google.com/u/0/project/nop-bai-4934d/firestore/databases/-default-/data/~2Flichhoc~2F<mã nhà>`.
  Rê chuột lên dòng `json` thì hiện nút bút chì (`aria-label` "Modifier le champ json"). Bấm,
  ô sửa là `textarea[name="ng.form0.0.fieldValue.stringValue"]`: gọi `select()` trên ô đó rồi
  `cdp("Input.insertText", text=json)` để thay cả chuỗi, đọc lại `value` so đúng từng ký tự, bấm
  "Mettre à jour".
- Tạo mới (nhà mới, chưa có collection `lichhoc`): ở cột gốc bấm "Commencer une collection", ID
  `lichhoc`, "Suivant", ID document là mã nhà, champ `json`, type `string`, dán JSON vào ô
  "Chaîne", "Enregistrer". Có collection rồi thì dùng "Ajouter un document" ở cột `lichhoc`.
- Bấm bằng `click_at_xy` theo toạ độ lấy từ `getBoundingClientRect()`. Lần bấm đầu trên trang
  vừa mở hay không ăn: bấm xong thì kiểm lại (hộp thoại đã mở chưa, ô sửa có chưa), chưa thì bấm
  lần nữa. Nút bút chì có lúc báo toạ độ sai: chụp màn rồi bấm theo vị trí trên ảnh (ảnh
  `max_dim=1400` thì nhân 1,47 ra toạ độ trang). Rê chuột bằng `Input.dispatchMouseEvent` có lúc
  báo hết giờ chờ nhưng vẫn rê được. `js()` trả về một phần tử DOM thì lỗi, hỏi `!!phần tử`.
- Ghi xong thì mở lại ô sửa, so `value` với chuỗi vừa dán, rồi bấm "Annuler".
- Mã nhà thật không ghi ở đây. Tìm trên console: collection `nha`, document có
  `hop/trangthai.capNhatLuc` gần nhất và `tenCon` "Lê Hòa".
