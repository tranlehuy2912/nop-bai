# Định dạng dữ liệu ghép cho bài làm trên máy

Tài liệu này tả phần dữ liệu thêm vào các file `app/src/main/assets/nganhang/*.json` để Lê Hòa làm bài ngay trên tablet bằng bàn phím ghép, không viết vở. Anh Huy chốt các luật ngày 29/09/2026. Công cụ kiểm tra là `tools/ghep/kiem.py`; file nào nó báo lỗi thì app không được nạp.

## Luật chơi (để hiểu vì sao dữ liệu có từng trường)

- Mỗi câu có V sao, V sao là V phút.
- Mỗi lần bấm Kiểm tra mà sai thì mất một sao. Sau lần sai đầu, máy bớt phím nhiễu.
- Hết sao thì câu đó 0 phút, nhưng con được thử thêm một lần. Vẫn sai thì máy hiện lời giải.
- Làm lại mở sau 24 giờ, chỉ cộng phần hơn so với lần tốt nhất. Chưa đủ sao thì 24 giờ sau lại mở.
- Ôn lại theo hẹn 3, 10, 20, 30 ngày; mỗi lần hẹn là một vòng sao mới. Chỉ câu từng sai (kể cả câu làm trên máy mà mất sao) rồi đã làm đúng mới vào lịch ôn; câu đúng ngay từ đầu thì không (`KhoBai.denHen`).
- Câu nhiều ô: mỗi ô sai trừ một sao.
- Chỉ giao câu trong phần Lê Hòa đã học (mốc "Lớp đã học tới"). Câu sai ở bài chụp ảnh mà có `ghep` thì sau 24 giờ thành câu làm trên máy.
- Mỗi ngày phần làm trên máy (luyện tập, luyện chỗ hay vấp, Giải đề, bài tập SGK) được tối đa 120 phút (90 trước ngày 02/10/2026), ôn lại 30 phút (`LuatCongGio`). Phút vượt trần không mất mà vào quỹ giờ chơi, Ba Huy cấp sau.
- Từ 02/10/2026 câu SGK Toán, KHTN, Tiếng Anh cũng làm trên máy: vào Luyện tập theo phần như câu sách bài tập (câu SGK của một bài đứng trước câu sách bài tập cùng bài), và là bài cô giao ở dòng "Làm bài tập trong SGK" (`BaiSgkActivity`). Câu SGK chưa có `ghep` hay có `bo_may` là câu phải viết: con làm ra vở, nộp bài xong thì chụp, Claude chấm như bài chụp. Mục không mang số bài của SGK ("Luyện tập chung", "Bài tập cuối chương") không vào Luyện tập theo phần, chỉ làm ở dòng bài tập SGK.
- Thứ tự nút (phương án, phím, thẻ, dòng) trộn lại ở mỗi lượt mới và giữ nguyên trong một lượt (anh Huy chốt 30/09/2026). Trắc nghiệm cũng trộn, chữ A, B, C, D đánh lại theo thứ tự mới. Riêng câu Đúng/Sai giữ nút Đúng đứng trước, và hàng phím số, dấu của câu biểu thức giữ nguyên.
- Trên app không có chữ nào giải thích luật. Con tự khám phá.

## Các trường mới

Ở mức file đã có sẵn `nguon` (không đổi về sau, vì đi vào id câu), `mon`, `ten`, `ban`, `cac_bai`. `ban` tăng mỗi lần sửa file, vì app chỉ nạp lại khi số này đổi, và phải đứng trước `cac_bai` vì app đọc số bản ở đầu file rồi dừng. `mon` của file là thứ `kiem.py` dùng để chọn bộ phím cơ bản; app thì lấy môn từ khai báo sách ở `NganHang.SACH`. Mỗi bài có `chuong`, `bai`, `cac_cau`. Thêm:

- `doan_van`: bảng mã đoạn văn sang nội dung, cho các câu đọc hiểu dùng chung một đoạn. Xuống dòng bằng `\n`.

Ở mức từng câu (cạnh `ma`, `de`, `trang`...):

- `ghep`: cách làm trên máy, xem các kiểu bên dưới. Thiếu trường này thì câu không có trong bài máy giao.
- `hinh`: danh sách đường dẫn ảnh, tính từ `app/src/main/assets/hinh/`. Ví dụ `["sbtanh8/1.A1.5.webp"]`. Tên file chỉ dùng chữ không dấu, số, dấu chấm, gạch ngang, gạch dưới.
- `doan`: mã đoạn văn trong `doan_van` của file, nếu câu đọc hiểu theo một đoạn.
- `bo_may`: chuỗi lý do, khi câu cố ý không làm trên máy. Ví dụ `"vẽ hình"`, `"vẽ đồ thị"`, `"làm miệng"`. Có `bo_may` thì không có `ghep`.

Chữ trong mọi trường hiển thị được dùng hai thẻ: `<u>...</u>` cho chữ gạch chân (bài phát âm, bài tìm lỗi sai), `<b>...</b>` cho chữ in đậm trong sách. Chỗ trống trong câu viết `___` (ba dấu gạch dưới). Ký hiệu toán viết trên một dòng bằng Unicode như bộ thẻ Học thuộc: `x²`, `x³`, `√`, `a/b`, `H₂O`, `→`, `≈`, `∠`, `△`.

## Các kiểu ghép

Mọi kiểu đều có `kieu` và `sao`. Trường `hoi` (không bắt buộc) là chữ hiện trên máy thay cho `de`: dùng khi `de` còn chứa các phương án in liền, khi cần bỏ phần lời dẫn không thuộc câu, hay khi cần tả hình bằng chữ. Không có `hoi` thì máy hiện `de`. Đề thi thì câu nào cũng có `hoi` (xem phần đề thi).

### CHON: trắc nghiệm chọn một, 1 sao

```json
{"kieu": "CHON", "sao": 1,
 "hoi": "Choose the word in which the underlined part is pronounced differently.",
 "cac": ["ch<u>oo</u>se", "c<u>oo</u>l", "g<u>oo</u>d", "t<u>oo</u>l"],
 "dap": "C"}
```

- `cac`: 2 tới 6 phương án, không kèm chữ "A." ở đầu.
- `dap`: chữ cái của phương án đúng, A là phương án đầu.
- Máy trộn thứ tự phương án mỗi lượt, nên phương án không được nhắc chữ cái hay vị trí của phương án khác ("Cả A và B", "Tất cả các ý trên"), và `hoi` phải bỏ hết các phương án in liền trong `de`. `kiem.py` cảnh báo khi gặp hai trường hợp này.
- `nhieu_dap: true`: con bấm chọn được nhiều phương án, và `dap` là danh sách chữ cái, ví dụ `["A", "C"]`. Dùng khi lời dẫn cho chọn hai đáp án ("Sometimes you can choose two answers"). Bật cho mọi câu của bài đó, kể cả câu chỉ một đáp án (khi ấy `dap` là `["B"]`): bật riêng câu hai đáp án thì nhìn cách bấm là con biết câu nào có hai. Vẫn 1 sao. Máy chỉ nhận khi con chọn đúng cả bộ.

### DUNG_SAI: đúng hay sai, 1 sao

```json
{"kieu": "DUNG_SAI", "sao": 1,
 "hoi": "The girl started knitting when she was in grade 10.",
 "dap": false, "nhan": ["True", "False"]}
```

- `nhan` không bắt buộc, mặc định `["Đúng", "Sai"]`.

### CHU: ghép chữ cái thành một từ, 2 sao

```json
{"kieu": "CHU", "sao": 2, "truoc": "c", "dap": ["cook"], "nhieu": ["u", "l", "a", "e"]}
```

- `dap`: mọi cách viết đúng, viết đủ cả phần `truoc`. Máy so không phân biệt hoa thường, bỏ khoảng trắng.
- `truoc`: phần sách cho sẵn (chữ cái đầu), không bắt buộc.
- Phím = các chữ cái khác nhau trong `dap` (bỏ phần `truoc`) cộng `nhieu`. Đáp án có dấu cách thì máy thêm phím cách.
- `nhieu`: chữ cái nhiễu, mỗi phím một ký tự, không trùng chữ nào trong `dap`. Ít nhất 2, dưới 2 là lỗi. Nên nhiều cỡ bằng số chữ thật: `kiem.py` cảnh báo khi ít hơn số chữ thật, đòi tối đa 4.
- Chỉ hiện ô từng chữ khi mọi đáp án dài bằng nhau.

### CAU: ghép thẻ từ thành câu, 2 sao

```json
{"kieu": "CAU", "sao": 2,
 "the": ["She", "enjoys", "playing", "the", "piano."],
 "nhieu": ["enjoy", "to play"],
 "dap": ["She enjoys playing the piano.", "She enjoys playing piano."]}
```

- Mỗi thẻ dùng một lần. Dấu câu dính vào thẻ ("piano.").
- Mỗi câu trong `dap` phải ghép được từ các thẻ trong `the` (không dùng thẻ nhiễu), các thẻ cách nhau đúng một dấu cách.
- `nhieu`: 1 tới 3 thẻ sai, thường là dạng sai của một thẻ đúng.

### BIEU_THUC: ghép biểu thức, đáp số, phương trình hoá học; sao bằng số dòng lời giải

```json
{"kieu": "BIEU_THUC", "sao": 3,
 "dap": ["(x+2)(x+1)", "(x+1)(x+2)"],
 "phim": ["x"],
 "nhieu": ["y", "²"],
 "loi_giai": ["x² + 3x + 2 = x² + x + 2x + 2", "= x(x + 1) + 2(x + 1)", "= (x + 1)(x + 2)"]}
```

- Bộ phím cơ bản luôn có, không ghi vào dữ liệu:
  - Toán: `0` tới `9`, `+`, `−`, `×`, `:`, `/`, `=`, `(`, `)`, `,`
  - Khoa học tự nhiên: `0` tới `9`, `+`, `−`, `·`, `:`, `/`, `=`, `(`, `)`, `,`, `→`
- `phim`: các phím khác cần để ghép ra đáp án, mỗi phím có thể nhiều ký tự (`Na`, `mol`, `cm²`, `x²`). Không ghi lại phím cơ bản.
- `nhieu`: phím nhiễu ngoài bộ cơ bản, nhiều cỡ bằng `phim`. Được để trống khi `phim` trống (đáp án chỉ gồm số).
- `dap`: mọi dạng đúng. Máy so sau khi chuẩn hoá: bỏ khoảng trắng, `x²` bằng `x^2`, `H₂O` bằng `H2O`, `−` bằng `-`, `×` `·` `*` là một, `:` bằng `/`. Khác Kiểm tra bài, máy giữ chữ hoa chữ thường, vì phím đã cố định cách viết (`CO` khác `Co`). Mũi tên phản ứng viết `→`.
- Máy nhận mọi thứ tự hạng tử, nên **không liệt kê các cách xếp khác** của cùng một đa thức hay cùng một vế phản ứng: `x³ + 5x²y − 10xy` và `−10xy + x³ + 5x²y` là một. Môn Toán còn nhận mọi thứ tự thừa số trong đơn thức (`yx²` như `x²y`), môn khác thì không (`H₂O` khác `HO₂`). Máy không gộp gì: `xyx` khác `x²y`, `2x + 3x` khác `5x`, nên câu rút gọn vẫn bắt được bài chưa rút gọn. Thứ tự hai vế giữ nguyên (`x = 2` khác `2 = x`), thứ tự các phần cách bằng `;` cũng giữ nguyên. Chỉ liệt kê dạng khác thật sự (`1/2x` và `x/2`, hai cách viết nghiệm). `kiem.py` báo "lưu ý" cho dạng chỉ khác thứ tự.
- `thu_tu: true`: chỉ nhận đúng thứ tự trong `dap`. Dùng khi thứ tự là chính điều sách hỏi ("sắp xếp theo luỹ thừa giảm dần của x"). Không bật chỉ để giữ một cách viết đẹp.
- `loi_giai`: các dòng lời giải mẫu, đúng như một bài làm đủ bước. Hết sao thì con còn một lượt thử không tính phút; lượt đó vẫn sai thì máy hiện các dòng này. `sao` bằng số dòng, từ 1 tới 20.

### BUOC: xếp các bước lời giải; sao bằng số bước

```json
{"kieu": "BUOC", "sao": 5,
 "buoc": ["a chia 3 dư 2 nên a = 3n + 2 (n ∈ ℕ)", "a² = (3n + 2)²", "= 9n² + 12n + 4",
          "= 3(3n² + 4n + 1) + 1", "Vì 3(3n² + 4n + 1) chia hết cho 3 nên a² chia 3 dư 1"],
 "nhieu": ["= 9n² + 6n + 4", "a chia 3 dư 2 nên a = 3n − 2 (n ∈ ℕ)"],
 "thu_tu_khac": []}
```

- `buoc`: các dòng theo đúng thứ tự, 2 tới 20 dòng. `sao` bằng số dòng.
- `nhieu`: 1 tới 3 dòng sai, là lỗi hay gặp thật (quên nhân đôi, sai dấu, dùng nhầm trường hợp bằng nhau), không phải dòng vô nghĩa.
- `thu_tu_khac`: các thứ tự khác cũng đúng, ghi bằng chỉ số của `buoc` tính từ 0. Ví dụ ba cặp cạnh bằng nhau liệt kê theo thứ tự nào cũng được.
- Dùng cho cả đoạn văn tiếng Anh dạng xếp câu thành đoạn (mỗi câu một bước).

### O: dòng có ô chọn; sao bằng số ô chọn

```json
{"kieu": "O", "sao": 4, "gioi": true,
 "dong": [
  {"chu": "My best friend's name is {Ten}.\n{He} {0} {1} the most.",
   "o": [{"dung": ["likes"], "sai": ["like"]},
         {"dung": ["playing badminton", "reading comics", "listening to music"], "sai": ["to playing chess"]}]},
  {"chu": "{He} does it {0} {1}.",
   "o": [{"dung": ["every day", "twice a week", "three times a week"], "sai": ["two times in week"]},
         {"dung": ["with {his} sister", "with me", "with {his} classmates"], "sai": ["with {he} sister"]}]}
 ]}
```

- Mỗi dòng có chữ với chỗ `{0}`, `{1}`... cho từng ô, theo thứ tự trong `o`.
- Ô chọn: `dung` (ít nhất 1 phương án đúng), `sai` (phương án sai ngữ pháp hay sai nội dung). Máy trộn hai danh sách thành các nút.
- Mỗi ô chọn có ít nhất 3 nút (`dung` cộng `sai`), vì hai nút là đoán mò trúng một nửa (anh Huy chốt 30/09/2026). Phương án sai thêm vào phải là lỗi hay gặp thật, không phải chữ vô nghĩa.
- Ngoại lệ: ô vốn chỉ có hai giá trị thì giữ hai nút và ghi `"hai": true` vào ô đó. Ví dụ: `Đúng`/`Sai`, `Có`/`Không`, `Là đơn thức`/`Không phải`, `biến đổi vật lí`/`biến đổi hoá học`, xếp vào một trong hai cột, chọn một trong hai vật đề bài so sánh (ống nghiệm 1 hay 2). `kiem.py` báo lỗi ô dưới 3 nút mà không có `hai`, và ô có `hai` mà không đúng hai nút.
- Không có ô gõ. Ô `{"go": true}` (con gõ tên riêng bằng bàn phím Android, không chấm) bỏ ngày 02/10/2026, vì anh Huy muốn bài làm trên máy chỉ bấm phím ghép. Tên riêng in sẵn vào chữ của dòng: tên bạn thân là `{Ten}`, tên khác viết thẳng (đang dùng: người thân `mum`, tên làng `Tan Lap`, bạn qua thư `Tom`, người viết thư `Hoa`). Dòng chỉ có tên mà không có ô chọn thì gộp vào dòng kề bên bằng `\n`, vì dòng nào cũng phải có ô. `kiem.py` báo lỗi ô có `go`; app gặp ô `go` thì bỏ cả câu.
- `gioi: true`: máy hỏi trước "bạn nam hay bạn nữ", rồi thay `{He}` `{he}` `{His}` `{his}` `{Him}` `{him}` thành He/She, he/she, His/Her, his/her, Him/Her, him/her, và `{Ten}` thành Minh (bạn nam) hay Lan (bạn nữ).
- `sao` bằng tổng số ô chọn, không quá 20.
- Dùng cho: nối cột (mỗi vế trái một dòng, ô chọn là các vế phải), xếp từ vào cột (mỗi từ một dòng, ô chọn là tên cột), điền theo câu gợi ý, câu đúng về bản thân.

## Chọn kiểu cho từng loại câu

- Trắc nghiệm A, B, C, D: `CHON`.
- Đúng/sai: `DUNG_SAI`. Đúng/sai từng ý a, b, c: `O`, mỗi ý một dòng với ô `Đúng`/`Sai` và `"hai": true`.
- Nối cột, xếp vào cột: `O`.
- Điền một từ, dạng đúng của từ, từ dưới hình, ô chữ, xếp lại chữ cái: `CHU`.
- Viết câu theo gợi ý, viết lại câu, trả lời câu hỏi bằng một câu: `CAU`.
- Đáp số, rút gọn, phân tích nhân tử, giải phương trình, tính toán có kết quả, phương trình hoá học: `BIEU_THUC`.
- Chứng minh, giải thích, bài tính nhiều bước mà con phải trình bày: `BUOC`.
- Câu có nhiều kết quả rời nhau (ví dụ "biểu thức nào là đơn thức"): `O`, mỗi biểu thức một dòng với ô `Có`/`Không` và `"hai": true`.
- Vẽ đồ thị, vẽ biểu đồ, vẽ hình, vẽ sơ đồ (mạch điện, lưới thức ăn), cắt gấp hình: từ 06/10/2026 không còn `bo_may` mà thành câu chọn hình (mục "Câu vẽ: chọn hình" dưới đây).
- Lập bảng thống kê: `O`, mỗi ô cần điền của bảng một dòng (hay một ô chọn trong dòng của hàng đó).
- Điều tra thực tế, dự án, poster, tự đo, việc ở nhà: làm trên máy phần kiến thức (nguyên nhân, biện pháp, nên hay không nên), bỏ phần điều tra; cần số liệu thực tế (lãi suất ngân hàng, nhiệt độ, thân nhiệt, khẩu phần) thì cho số liệu giả định và ghi rõ "Giả sử ..." trong `hoi`.
- `bo_may` chỉ còn cho câu không có phần nào làm được bằng phím ghép, kể cả sau khi đổi như trên (làm miệng thuần, thao tác phần mềm thuần). Ghi lý do ngắn.

## Câu vẽ: chọn hình (từ 06/10/2026)

Anh Huy chốt ngày 06/10/2026: tránh viết tay nhiều nhất có thể. Câu SGK không có `ghep` hay có `bo_may` là câu con phải viết vở rồi chụp, nên câu vẽ đổi thành câu chọn hình:

- Vẽ sẵn 3 hay 4 hình bằng `ghep-nhap/ghep/cong_cu/ve.py` (ngoài git; đồ thị, biểu đồ cột, cột kép, đoạn thẳng, hình quạt tròn, hình hình học, và hàm `ghep`): một hình đúng, các hình sai là lỗi học sinh hay mắc (sai dấu hệ số góc, nhầm tung độ gốc, đổi chỗ hai trục, sai chiều cao một cột, chọn sai loại biểu đồ, thiếu chú thích, sai một độ dài). Hình sai phải sai rõ khi đối chiếu với đề, không sai vì nét vẽ.
- Ghép các hình thành từng hàng hai hình, nhãn "Hình 1" tới "Hình N" chạy tiếp qua các hàng (`ve.ghep_hang`), mỗi hàng một ảnh, lưu bằng `ve.luu` vào thư mục hình của sách, gắn cả danh sách ảnh vào trường `hinh` theo thứ tự. Không dùng một tấm 2 x 2: app vẽ mỗi ảnh cao tối đa 300dp (`KhungGhep.veHinh`), tấm 2 x 2 bị thu còn một nửa, chữ trên trục không đọc được.
- Câu chỉ có việc vẽ: `CHON`, `cac` là `["Hình 1", "Hình 2", ...]`. Máy trộn thứ tự nút nhưng chữ trên nút vẫn trỏ đúng ô trong ảnh. 1 sao.
- Câu có cả phần vẽ lẫn phần tính: `O`, mỗi ý một dòng (`"a) Công thức: {0}"`, `"b) Đồ thị: {0}"`, `"c) Chi phí 15 xe: {0}"`), dòng phần vẽ có ô chọn `Hình 1` tới `Hình N`, dòng phần tính có ô chọn kết quả đúng và kết quả sai do lỗi hay gặp.
- Không vẽ lại hình của sách để làm đáp án nếu hình sách lộ đáp án; hình đề bài (hình cho sẵn để con đọc) vẫn cắt từ ảnh trang như cũ.

## Câu cần hình mà chưa có ảnh

Toán và Khoa học tự nhiên cắt hình sau cùng (anh Huy chốt 29/09/2026), vì lúc đó chưa đủ bản quét (bản quét SBT Toán hai tập có từ 30/09/2026). Câu nào không nhìn hình thì không làm được (đề ghi "Hình 3.5", biểu đồ, bảng số liệu in dạng ảnh) vẫn soạn `ghep` đầy đủ như thường, rồi ghi thêm trong file phần một bảng `can_hinh` từ mã câu sang lời tả hình cần cắt, ví dụ `"can_hinh": {"3.12": "Hình 3.5: hình thang ABCD, hai đường chéo cắt nhau tại O"}`. Lúc gộp vào sách (`tools/ghep/gop.py`), câu có trong `can_hinh` mà chưa có `hinh` thì phần ghép được cất vào trường `ghep_cho_hinh` và câu ghi `bo_may: "chờ hình: <lời tả>"`, để máy không giao câu thiếu hình. Khi đã cắt hình, gộp lại một file phần có bảng `hinh` cho câu đó: `ghep_cho_hinh` tự về lại `ghep` và `bo_may` bị xoá. Câu đề đã tả đủ hình bằng chữ ("Cho tam giác ABC vuông tại A, đường cao AH") thì không cần hình, không ghi vào `can_hinh`.

## Gộp, kiểm và cắt hình

- `python3 tools/ghep/kiem.py <file>` kiểm một file sách hay file phần; `--tat-ca` kiểm mọi file trong `assets/nganhang`. Có lỗi thì không gộp được; cảnh báo in thành dòng "lưu ý". Bộ phím cơ bản trong `kiem.py` (`PHIM_CO_BAN`) phải giống `Ghep.phimCoBan` bên app.
- `python3 tools/ghep/gop.py phan <file phần>...`: gộp các file phần (mỗi file có `nguon` và các bảng `ghep`, `bo_may`, `hinh`, `can_hinh`, `sua_de` theo mã câu) vào file sách đã có, như SBT Toán, SGK Toán, SBT KHTN, SGK KHTN.
- `sua_de` (từ 02/10/2026): mã câu sang cả đề mới, cùng quy ước của file sách (số mũ `^`, KHTN chỉ số số thường). Dùng khi `de` chép lúc nạp sách sai so với bản in, hay không tự đọc được khi đứng một mình trên máy (câu SGK nhắc "tình huống mở đầu", "HĐ2", "hình bên" mà không chép nội dung). Đổi `de` không đổi id câu (id là nguồn và mã).
- `python3 tools/ghep/gop.py sach <nguon> <file Unit>...` (thêm `--ten`, `--mon` khi cần): dựng cả một sách mới từ các file Unit, như SBT Tiếng Anh, xếp bài theo thứ tự sách.
- Cả hai cách đều tự tăng `ban`, giữ `ban` đứng trước `cac_bai`, bỏ các dạng `BIEU_THUC` chỉ khác thứ tự hạng tử, chép ảnh bằng `--hinh-tu <thư mục>`, rồi chạy `kiem.py`. `--thu` chỉ kiểm, không ghi. `kiem.py` báo lỗi thì file sách cũ giữ nguyên.
- `python3 tools/ghep/cat_hinh.py <ảnh trang> <x0> <y0> <x1> <y1> <ra.webp> [--le 6] [--rong 480]`: cắt một hình từ ảnh một trang sách, bỏ nền trắng quanh hình, phóng to rồi lưu WebP cho trường `hinh`.
- Câu sách in sai số liệu mà không gán được đáp án chắc chắn thì vẫn để làm thêm (máy chấm theo cách làm), nhưng ghi mã câu vào `GiaiDe.KHONG_RA_DE` để câu đó không vào đề.

## Tiếng Anh: mã câu và cách chia

- Mã câu: `<Unit>.<phần><số bài>.<số câu>`, ví dụ `1.B3.2` là Unit 1, phần B, bài 3, câu 2. Test Yourself: `T1.3.2`. Bài in thành hai phần a, b thì thêm chữ vào số bài (`1.D3b.1`, `T1.7b.6`); phần chỉ là một câu thì không có số câu (`1.D3a`, `T1.7a`).
- Bài có hai vế (viết từ dưới hình rồi xếp vào cột): từng hình một câu `1.A1.1`, `1.A1.2`..., phần xếp cột một câu `1.A1.cot`.
- Bài tìm lỗi sai: câu chọn chỗ sai `T1.4.1a` (`CHON`), câu sửa lại `T1.4.1b` (`CHU` hay `CAU`).
- Bài viết đoạn văn của Unit (số bài theo sách: `E3` ở Unit 1, 2, 3, 6; `E2` ở Unit 4; Unit 5 có cả `E2` lẫn `E3`): phần a xếp câu thành đoạn, ví dụ `1.E3a` (`BUOC`, đoạn mẫu do mình viết, 6 tới 8 câu, cộng 2 câu lạc đề); phần b ghép theo câu gợi ý, ví dụ `1.E3b` (`O`, mỗi câu gợi ý một dòng với hai ô: một ô ngữ pháp, một ô nội dung). Nội dung phần b không trùng phần a. Tới Unit 6 chỉ `1.E3b` bật `gioi: true` (đề viết về bạn thân, tên bạn in sẵn bằng `{Ten}`); các phần b khác không bật. Tên riêng khác in sẵn vào chữ: `2.E2`, `2.E3b` dùng làng Tan Lap, `5.E2b` viết "Dear Tom," và ký tên Hoa.
- Bỏ hẳn (không đưa vào file): bài làm theo cặp, theo nhóm, nói to, luyện đọc, phỏng vấn bạn. Ngoại lệ: trò chơi Unit 4 C2 là câu hỏi kiến thức, giữ lại.
- `bai`: `Unit 1. Leisure time`; Test Yourself thì `Test Yourself 1`.
- `nhom`: tên phần, ví dụ `A. Pronunciation`. Test Yourself thì `nhom` là tên bài, `Test Yourself 1`.
- `trang`: số trang in (trang PDF trừ 1).
- `de`: lời dẫn của bài, xuống dòng, rồi nội dung câu. Câu trắc nghiệm thì `de` giữ cả các phương án như sách in, còn `hoi` bỏ các phương án.
- `dang`, `dap_an`, `loai_dap_an`: câu `CHON` thì `TRAC_NGHIEM`, chữ cái đúng, `TN`; câu khác thì `CAU_NHO`, đáp án bằng chữ, `DAP_SO`.

## SGK Tiếng Anh (`anh8.json`, từ 02/10/2026)

Chép từ bản quét SGK Tiếng Anh 8 Global Success, đợt đầu Unit 1 tới 3, đợt hai Review 1 và Unit 4, dựng bằng `gop.py sach anh8 u01.json u02.json u03.json r1.json u04.json`. Các luật khác SBT Tiếng Anh:

- Bài Review: `chuong` và `bai` là `Review 1` (tới 4), `nhom` là tên phần in trên trang (`Language`, `Skills`). Review không mang số Unit nên chỉ hiện ở dòng "Làm bài tập trong SGK", không vào Luyện tập, như "Luyện tập chung" của SGK Toán. `gop.py` xếp Review k ngay sau Unit 3k.

- `bai` và `chuong` đúng như SBT (`Unit 1. Leisure time`), để Luyện tập xếp câu SGK trước câu SBT cùng Unit.
- Màn chọn câu in nguyên mã làm nhãn, mà cô giao theo số bài và trang, nên mã là nhãn đọc được theo khuôn SGK Toán: `Bài 3.2 (tr.10)` là bài 3 trang 10 câu 2, `Bài 2.a (tr.8)` khi sách đánh chữ, `Bài 2 (tr.8)` khi cả bài là một câu, `Bài 4a (tr.13)` và `Bài 4b.1 (tr.13)` khi bài in hai phần a, b. Trang trong mã là trang in số bài.
- `nhom` là tên phần như sách: `Getting Started`, `A Closer Look 1`, `A Closer Look 2`, `Communication`, `Skills 1`, `Skills 2`, `Looking Back`.
- Mã đoạn văn trong `doan_van`: `<Unit>.<phần viết tắt><số bài>`, ví dụ `1.GS1`. Chỉ là khoá nội bộ.
- Bỏ hẳn: bài nghe (app không có âm thanh), "Listen and repeat", làm theo cặp hay nhóm, đóng vai, Project. Giữ hội thoại "Listen and read" của Getting Started làm đoạn văn cho các bài đọc hiểu sau nó, và giữ bài phát âm mà đáp án chỉ dựa vào kiến thức (xếp từ vào cột theo âm).
- Bài nói (anh Huy chốt 06/10/2026): gợi ý in sẵn hay bài đọc trong sách cho ra đúng một đáp án thì giữ (ghép thẻ hay ô chọn, ví dụ Unit 6 Skills 1 bài 4); bài đoán, hỏi ý kiến riêng thì bỏ.
- Một trang có hai bài cùng số ở hai phần khác nhau (Unit 6 trang 64: Bài 3 của A Closer Look 2 và Bài 3 của Communication): mã của bài thuộc phần sau thêm tên phần, `Bài 3.1 (tr.64, Communication)` (anh Huy chốt 06/10/2026).
- Bài viết đoạn văn ở Skills 2 làm như `E3a`, `E3b` của SBT: phần a xếp đoạn mẫu (`BUOC`), phần b ghép theo câu gợi ý (`O`).

## Bộ đề thi in sẵn (`dethianh8.json`, từ 30/09/2026)

Một file như file sách, nhưng mỗi `bai` là một đề nguyên vẹn, giữ đúng thứ tự in. Khai trong `NganHang.SACH` với `deThi = true`: không hiện ở màn chọn sách khi nộp bài, không vào làm thêm hay đề tuần (`kho/DeThi.kt`, `GiaiDe.taoDeThi`, `GiaiDe.moDeThi`). Câu của đề vẫn quay lại như mọi câu làm trên máy: ở phần làm lại của Luyện tập khi đề đã hết hạn, và ở Ôn lại khi tới hẹn. Đợt 1 (giữa kì 1 số 1 tới 3) sinh bằng `ghep-nhap/dethianh/tao_de.py`, nằm ngoài git cạnh ba repo. Đợt hai (giữa kì 1 số 4 tới 6, cuối kì 1 số 1 tới 8 trừ số 6, ngày 30/09/2026) soạn từ PDF bằng tác tử; bản chữ đề, file từng đề, ghi chú soát và script gộp nằm trong gói `de-thi-anh-dot2-2026-09-30.zip`, cũng ngoài git, và không có trên máy đang dùng (kiểm ngày 01/10/2026).

Không chạy lại `tao_de.py`: script chỉ dựng 3 đề của đợt 1 với `ban` 1 rồi ghi đè cả file, nên chạy lại là mất 10 đề đợt hai, và máy cũng mất theo vì `NganHang` nạp lại khi số bản khác. Từ 01/10/2026 script tự dừng khi file đích có nhiều đề hơn hay `ban` cao hơn. Đợt sau nối đề mới vào cuối `cac_bai` của `dethianh8.json`, giữ nguyên các đề đã có, tăng `ban`, rồi chạy `kiem.py`, `DeThiTest` và `GhepTest`.

Ở mức bài, thêm `de_thi`:

```json
{"chuong": "Giữa học kì 1", "bai": "Đề giữa kì 1 số 1",
 "de_thi": {"ma": "GK1-1", "den_unit": 3, "phut": 45, "nguon_goc": "loigiaihay.com, đăng 11/11/2023"},
 "cac_cau": [...]}
```

- `ma`: mã ngắn của đề, đi vào khoá của đề đã mở và lệnh `MODETHI` của Bảng điều khiển. Không đổi về sau. Đề giữa kì 1 là `GK1-N` (`chuong` "Giữa học kì 1", `bai` "Đề giữa kì 1 số N"), đề học kì 1 là `CK1-N` (`chuong` "Cuối học kì 1", `bai` "Đề cuối kì 1 số N"), N là số đề của bản in.
- `den_unit`: Unit cuối cùng mà các câu làm trên máy chạm tới. Tablet tự mở đề khi Lê Hòa chọn "Lớp đã học tới" Tiếng Anh bằng hoặc vượt số này. Câu phát âm dùng từ của Unit sau (earthquake, nomadic) không tính, vì câu đó hỏi cách đọc chứ không hỏi nghĩa. Tên đề không quyết định số này: các đề giữa kì 1, cuối kì 1 loigiaihay đăng năm 2024 có câu của Unit 7 tới 9 (tornado, earthquake, mệnh đề Whenever), nên ghi 9 và chỉ mở ở học kì 2 (anh Huy chốt 30/09/2026).
- `phut`: giờ làm bài, cũng là đồng hồ của đề trên máy.
- `nguon_goc`: nơi lấy đề và ngày đăng, để người soạn tra lại. App không đọc trường này.

Luật mở đề (`GiaiDe.taoDeThi`, `GiaiDe.moDeThi`): mỗi môn tối đa một đề đang mở (anh Huy chốt 01/10/2026). Đề sau của môn nào mở từ hôm sau ngày nộp đề trước của chính môn đó. Với Tiếng Anh, tablet tự mở đề có `den_unit` cao nhất trong các đề chưa nộp mà mốc Unit đã tới (cùng phạm vi thì theo thứ tự trong file). Đề Toán, KHTN mở theo bài SGK, xem mục dưới. Đề mở ra giữ 30 ngày, hết hạn mà chưa nộp thì lượt sau mở lại. Lê Hòa tự mở lại đề đã nộp ở trang Luyện tập từ hôm sau, chỉ với đề đã tới phạm vi; Ba Huy mở được mọi đề bằng lệnh `MODETHI`.

Ở mức câu:

- `ma`: `<mã đề>.<số câu in>`, ví dụ `GK1-1.36`. Số câu in tăng dần; máy hiện "Câu 36" đúng như đề.
- `phan`: phần lớn in trên đề (`A. LANGUAGE FOCUS`), `nhom`: lời dẫn của bài (`Exercise 3. Fill each blank ...`). Máy in phần và lời dẫn một lần ở đầu mỗi bài, cùng đoạn văn nếu bài có đoạn.
- `hoi` của mọi câu bắt đầu bằng đúng chuỗi `nhom`, xuống dòng, rồi mới tới nội dung câu. Làm trong đề thì máy bỏ dòng đầu đó (lời dẫn đã in ở đầu bài); câu đứng một mình ở Ôn lại hay làm lại thì vẫn đủ nghĩa. Câu phát âm, trọng âm chỉ có lời dẫn, nội dung nằm ở các phương án. Câu như vậy thì máy in lại lời dẫn ngay trong thẻ câu, để thẻ không trống (anh Huy chốt 30/09/2026).
- `trung`: câu in y hệt một câu của đề khác trong cùng file, ví dụ `{"ma": "GK1-2.1", "phan": "...", "nhom": "...", "trung": "GK1-1.1"}`. Câu này không có `de`, `ghep`, `doan`; máy dùng chung id của câu gốc, nên sổ cái chỉ trả giờ một lần, làm lại ở đề sau chỉ cộng phần hơn lần tốt nhất. Câu điền đoạn văn trùng mà số câu in khác (CK1-4 câu 26 tới 30 và CK1-5 câu 23 tới 27) thì không ghi `trung`: đề sau sẽ in đoạn văn của đề trước với số chỗ trống lệch số câu. Cả một đề in lại y hệt đề khác thì bỏ đề sau (CK1-6 là GK1-4).
- Phần nghe chưa có âm thanh: mỗi câu vẫn ghi `de` và `bo_may: "nghe"` để máy biết phần đó ở đâu và in "Phần nghe không làm trên máy (câu 31–35)".
- `ghi_chu`: chỗ sửa so với đề in (đề in hai phương án cùng đúng, lỗi gõ, câu trỏ "dòng 4" của đoạn văn). App không đọc trường này.
- Câu điền một từ vào đoạn văn: ghi thêm vào `dap` các từ khác cũng đúng, vì đáp án in thường chỉ có một.
- Câu tìm lỗi sai: `CHON`, `hoi` sau dòng lời dẫn là câu có các phần gạch chân, `cac` là các phần gạch chân theo thứ tự in, cách sửa ghi ở `ghi_chu`. Không tách câu sửa lại (mã câu phải kết bằng số in).
- Chỗ không dùng mạo từ ghi `Ø` như sách bài tập, kể cả khi đề in `x` hay `-`.
- Đề in không có tên phần lớn thì `phan` là chuỗi rỗng, máy chỉ in lời dẫn.

`kiem.py` kiểm thêm ở mọi bộ đề: mã đề không trùng trong file, mã câu bắt đầu bằng mã đề, `trung` trỏ tới câu có ghép trong file và không mang nội dung riêng (`de`, `ghep`, `bo_may`, `doan`, `hinh`, `bai_sgk`, `hinh_goi_y`, `hinh_day_du`), `hinh_goi_y` và `hinh_day_du` theo luật ở mục dưới. Riêng đề Tiếng Anh: `den_unit` 1 tới 12, `phut` 10 tới 120, số câu in tăng dần, câu thiếu `nhom` thì in lưu ý.

## Đề thi Toán, KHTN (`dethitoan8.json`, `dethikhtn8.json`, từ 01/10/2026)

Khung file giống `dethianh8.json` ở mục trên: mỗi `bai` là một đề, giữ đúng thứ tự in, khai trong `NganHang.SACH` với `deThi = true`. Nguồn là `dethitoan8` (`mon` "Toán") và `dethikhtn8` (`mon` "Khoa học tự nhiên"). Đề gốc là PDF của loigiaihay.com ở `~/Downloads/sgk/de thi toan/` và `de thi khtn/`. Ngày 01/10/2026 hai file này chưa có trong `assets/nganhang`. Các luật dưới đây anh Huy chốt ngày 01/10/2026.

Ở mức bài:

```json
{"chuong": "Giữa học kì 1", "bai": "Đề giữa kì 1 số 2",
 "de_thi": {"ma": "TGK1-2", "nguon_goc": "loigiaihay.com, đăng 03/09/2024"},
 "cac_cau": [...]}
```

- `ma`: không trùng với đề nào của mọi bộ đề, kể cả đề Tiếng Anh. Toán: `TGK1-N` giữa kì 1, `TCK1-N` cuối kì 1, `TGK2-N` và `TCK2-N` ở học kì 2. KHTN: `KGK1-N`, `KCK1-N`, `KGK2-N`, `KCK2-N`. N là số đề của bản in. Chữ `T`, `K` ở đầu để mã không trùng mã đề Anh (`GK1-N`). `kiem.py` chỉ so mã đề trong một file, nên hai file trùng mã thì nó không bắt được. `chuong` và `bai` đặt như đề Anh.
- Không có `den_unit`. Phạm vi của đề tính từ `bai_sgk` của các câu, xem luật mở đề bên dưới.
- `phut`: chỉ ghi khi đề in thời gian làm bài, 10 tới 150. Đề không in thì bỏ trường này, đồng hồ trên máy là 90 phút với Toán, 60 phút với KHTN. Hết giờ vẫn nộp được, như đề Anh.
- `nguon_goc`: như đề Anh. Ngày đăng là dấu thời gian ở cuối tên file PDF (`...-1725329814.pdf` là 03/09/2024), khớp với ngày đã ghi cho các đề Anh.

Ở mức câu, thêm so với đề Anh:

- `ma`: `<mã đề>.<khoá>`. Khoá chỉ gồm chữ không dấu, số và gạch ngang, không có dấu chấm, không trùng trong đề, đặt theo thứ tự in: trắc nghiệm `C5`, tự luận Toán `B3a` (Bài 3 ý a) hay `B2-1a` (Bài 2 ý 1a), tự luận KHTN `TL2b`. Máy không đọc số câu từ mã, nên không đòi số tăng dần như đề Anh.
- `nhan`: chữ ở đầu thẻ câu, đúng như đề in: "Câu 5", "Bài 3a", "Bài 2.1a", "Câu 2b". Bắt buộc ở mọi câu, kể cả câu `trung`. Đề Anh không có trường này, máy hiện "Câu <số in>".
- `bai_sgk`: số bài SGK mà câu kiểm, Toán 1 tới 39, KHTN 1 tới 47. Tên bài tra ở `toan8t1.json`, `toan8t2.json`, `khtn8.json`. Bắt buộc ở câu có `ghep`. Câu chạm hai bài thì ghi bài lớn hơn.
- `phan`: phần lớn in trên đề, như "Phần trắc nghiệm (3 điểm)", "I. Trắc nghiệm (6 điểm)".
- `nhom`: câu trắc nghiệm không có lời dẫn chung thì `nhom` rỗng và `hoi` chỉ có câu hỏi. Câu tự luận thì `nhom` là đề của cả bài ("Bài 3. (2,5 điểm) Cho hình bình hành ABCD ..."), mỗi ý là một câu, và `hoi` của ý bắt đầu bằng đúng chuỗi `nhom`, xuống dòng, rồi tới ý ("a) Tứ giác AKCI là hình gì? Vì sao?"). Như đề Anh, làm trong đề thì máy bỏ dòng đầu đó.
- `de`: `nhom` (nếu có), xuống dòng, rồi nội dung câu; câu trắc nghiệm giữ các phương án như đề in. Ký hiệu viết như sách bài tập: trong `de` số mũ là `x^2`, công thức hoá học là số thường (`CaCO3`); trong `hoi`, `cac`, `buoc` là `x²`, `CaCO₃`. Góc viết "góc A", tam giác "ΔABE". Lớp chữ của PDF làm mất số mũ, chỉ số và dấu góc, nên phải đối chiếu với ảnh trang.
- `trung`: như đề Anh. Câu `trung` không mang `bai_sgk`, `hinh_goi_y`, `hinh_day_du` (lấy của câu gốc), nhưng có `nhan` riêng vì số in ở hai đề thường khác nhau.
- `bo_may`: lý do không làm trên máy, như "vẽ hình", "vẽ biểu đồ".

Hình in sẵn trên tờ đề thì ghi `hinh` như mọi câu, máy hiện ngay. Bài hình mà tờ đề không in hình, hình chỉ có trong lời giải, thì ghi hai trường, đường dẫn tính từ `assets/hinh/` như `hinh`:

- `hinh_goi_y`: ảnh hiện sau lần bấm "Nhờ trợ giúp" thứ nhất.
- `hinh_day_du`: ảnh hiện sau lần bấm thứ hai.

Có `hinh_day_du` thì phải có `hinh_goi_y`. Mọi câu của cùng một bài (cùng `nhom` khác rỗng) trong một đề mang đúng cùng hai danh sách, vì câu đứng một mình ở Ôn lại hay làm lại lấy hình của chính nó. Câu trắc nghiệm có `nhom` rỗng thì mỗi câu tự mang hình của mình. Tên ảnh theo mã đề và bài: `dethitoan8/TGK1-2.B3.goi-y.webp`, `dethitoan8/TGK1-2.B3.day-du.webp`.

Trên máy, hình ẩn. Dưới đề bài có dòng nhắc và nút "Nhờ trợ giúp". Bấm lần 1 hiện hình gợi ý. Bấm lần 2 thì máy hiện hộp cảnh báo rằng hình đầy đủ có thể lộ lời giải, con đồng ý mới hiện. Thoát khỏi màn rồi vào lại thì hình ẩn lại, nút về lần 1. Mở hình không trừ sao. Con mở hình thì nhật ký ngày (`DayLog`) ghi một dòng, mỗi bài mỗi mức một lần trong ngày, ví dụ "Lê Hòa xem hình gợi ý Bài 3, Đề thi thử Toán (Đề giữa kì 1 số 2)", "Lê Hòa xem hình đầy đủ Bài 3, Đề thi thử Toán (Đề giữa kì 1 số 2)".

Cách làm hai ảnh:

- Lấy ảnh hình trong phần lời giải của PDF. Ảnh nhúng lấy ra được bằng `pypdf` (`page.images`). Ví dụ đề Toán giữa kì 1 số 2 có hình Bài 3 là `Image6.jpg` (1296 x 569) ở trang 6.
- `hinh_day_du` là ảnh nguyên vẹn.
- `hinh_goi_y` là cùng ảnh đó, đã xoá các điểm và nét mà chỉ lời giải mới có: giao điểm do lời giải tự đặt tên, đường kẻ thêm, nhất là thứ trả lời luôn câu hỏi. Mọi điểm và đoạn thẳng đề nhắc tới thì giữ. Ở TGK1-2 Bài 3, lời giải gọi O là giao điểm của AC và KI, chính là điểm ý c bắt chứng minh ba đường thẳng AC, EF, KI cùng đi qua, nên hình gợi ý xoá điểm O và chữ "O", còn các đoạn AC, BD, AI, CK, KI giữ nguyên.
- Điểm bị xoá nằm trên nét thì vẽ lại khúc nét bị che, để đường không đứt.
- Hai ảnh cắt cùng một khung cho khỏi lệch khi đổi qua lại, rồi lưu WebP như `cat_hinh.py` (chất lượng 85). Mở cả hai ảnh ra xem trước và sau khi xoá.

Nguyên tắc anh Huy chốt ngày 02/10/2026: mục tiêu là nâng kiến thức cho Lê Hòa, không phải lấy điểm. Câu sai logic hay sai kiến thức (đề tự mâu thuẫn, đáp án in sai, kiến thức sai hay không xác định được theo SGK) thì sửa cho đúng; không sửa được thì bỏ khỏi máy (`bo_may` ghi rõ lý do). Câu có hai, ba đáp án cùng đúng mà máy chỉ nhận một thì sửa phương án cho chỉ còn một đáp án đúng, đổi thành chọn nhiều đáp án (`dap` là danh sách, `nhieu_dap: true`), hoặc ghi rõ "đúng nhất". Phương án kiểu "Cả A, B, C đều đúng" đổi thành câu chọn nhiều đáp án, thay phương án đó bằng một phương án sai.

Luật mở đề Toán, KHTN (`GiaiDe.taoDeThi`):

- Phạm vi của đề ở mỗi phần học (Toán: Đại số `toan8ds`, Hình học `toan8hh`; KHTN: Hoá `khtn8hoa`, Lí `khtn8li`, Sinh `khtn8sinh`; xem `kho/PhanHoc.kt`) là `bai_sgk` cao nhất trong các câu làm trên máy thuộc phần đó.
- Đề tự mở khi mọi phần có câu trong đề đều đã học tới bài đó, theo mốc "Lớp đã học tới" của từng phần. Phần không có câu nào trong đề thì không xét, kể cả khi con chưa chọn mốc của phần đó. Ví dụ một đề có câu Bài 6, Bài 9 (Đại số) và Bài 14 (Hình học) mở khi Đại số tới Bài 9 và Hình học tới Bài 14.
- Mỗi môn tối đa một đề đang mở, như mục trên. Nhiều đề cùng đủ phạm vi thì đề rộng hơn mở trước, cùng độ rộng thì theo thứ tự trong file. Đề mở ra giữ 30 ngày, hết hạn mà chưa nộp thì lượt sau mở lại. Ba Huy mở được mọi đề bằng lệnh `MODETHI`.

Với đề Toán, KHTN, `kiem.py` kiểm thêm: không có `den_unit`; `phut` nếu có thì 10 tới 150; khoá sau mã đề chỉ gồm chữ không dấu, số, gạch ngang; mọi câu có `nhan` không rỗng; câu có `ghep` có `bai_sgk` trong khoảng bài của môn; câu trắc nghiệm để `nhom` rỗng thì không bị lưu ý; `hoi` của câu có `nhom` mà không bắt đầu bằng `nhom` rồi xuống dòng thì in lưu ý. Khoảng bài của từng môn nằm ở `BAI_SGK_CUOI` trong `kiem.py`, phải khớp `PhanHoc.TAT_CA`. Kiểm một file nằm ngoài app, như đề mẫu để thử, thì thêm `--hinh <thư mục hình>`.
