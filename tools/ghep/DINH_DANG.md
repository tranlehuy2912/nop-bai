# Định dạng dữ liệu ghép cho bài làm trên máy

Tài liệu này tả phần dữ liệu thêm vào các file `app/src/main/assets/nganhang/*.json` để Lê Hòa làm bài ngay trên tablet bằng bàn phím ghép, không viết vở. Anh Huy chốt các luật ngày 29/09/2026. Công cụ kiểm tra là `tools/ghep/kiem.py`; file nào nó báo lỗi thì app không được nạp.

## Luật chơi (để hiểu vì sao dữ liệu có từng trường)

- Mỗi câu có V sao, V sao là V phút.
- Mỗi lần bấm Kiểm tra mà sai thì mất một sao. Sau lần sai đầu, máy bớt phím nhiễu.
- Hết sao thì câu đó 0 phút, nhưng con được thử thêm một lần. Vẫn sai thì máy hiện lời giải.
- Làm lại mở sau 24 giờ, chỉ cộng phần hơn so với lần tốt nhất. Chưa đủ sao thì 24 giờ sau lại mở.
- Ôn lại theo hẹn 3, 10, 20, 30 ngày; mỗi lần hẹn là một vòng sao mới.
- Câu nhiều ô: mỗi ô sai trừ một sao.
- Thứ tự nút (phương án, phím, thẻ, dòng) trộn lại ở mỗi lượt mới và giữ nguyên trong một lượt (anh Huy chốt 30/09/2026). Trắc nghiệm cũng trộn, chữ A, B, C, D đánh lại theo thứ tự mới. Riêng câu Đúng/Sai giữ nút Đúng đứng trước, và hàng phím số, dấu của câu biểu thức giữ nguyên.
- Trên app không có chữ nào giải thích luật. Con tự khám phá.

## Các trường mới

Ở mức file (cạnh `nguon`, `ban`, `cac_bai`):

- `doan_van`: bảng mã đoạn văn sang nội dung, cho các câu đọc hiểu dùng chung một đoạn. Xuống dòng bằng `\n`.

Ở mức từng câu (cạnh `ma`, `de`, `trang`...):

- `ghep`: cách làm trên máy, xem các kiểu bên dưới. Thiếu trường này thì câu không có trong bài máy giao.
- `hinh`: danh sách đường dẫn ảnh, tính từ `app/src/main/assets/hinh/`. Ví dụ `["sbtanh8/1.A1.5.webp"]`. Tên file chỉ dùng chữ không dấu, số, dấu chấm, gạch ngang, gạch dưới.
- `doan`: mã đoạn văn trong `doan_van` của file, nếu câu đọc hiểu theo một đoạn.
- `bo_may`: chuỗi lý do, khi câu cố ý không làm trên máy. Ví dụ `"vẽ hình"`, `"vẽ đồ thị"`, `"làm miệng"`. Có `bo_may` thì không có `ghep`.

Chữ trong mọi trường hiển thị được dùng hai thẻ: `<u>...</u>` cho chữ gạch chân (bài phát âm, bài tìm lỗi sai), `<b>...</b>` cho chữ in đậm trong sách. Chỗ trống trong câu viết `___` (ba dấu gạch dưới). Ký hiệu toán viết trên một dòng bằng Unicode như bộ thẻ Học thuộc: `x²`, `x³`, `√`, `a/b`, `H₂O`, `→`, `≈`, `∠`, `△`.

## Các kiểu ghép

Mọi kiểu đều có `kieu` và `sao`. Trường `hoi` (không bắt buộc) là chữ hiện trên máy thay cho `de`, dùng khi `de` còn chứa các phương án in liền.

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
- `nhieu`: chữ cái nhiễu, không trùng chữ nào trong `dap`, ít nhất 2 và nhiều cỡ bằng số chữ thật.
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
- `dap`: mọi dạng đúng. Máy so sau khi chuẩn hoá như Kiểm tra bài: bỏ khoảng trắng, `x²` bằng `x^2`, `H₂O` bằng `H2O`, `−` bằng `-`, `×` `·` `*` là một, `:` bằng `/`. Mũi tên phản ứng viết `→`.
- Máy nhận mọi thứ tự hạng tử, nên **không liệt kê các cách xếp khác** của cùng một đa thức hay cùng một vế phản ứng: `x³ + 5x²y − 10xy` và `−10xy + x³ + 5x²y` là một. Môn Toán còn nhận mọi thứ tự thừa số trong đơn thức (`yx²` như `x²y`), môn khác thì không (`H₂O` khác `HO₂`). Máy không gộp gì: `xyx` khác `x²y`, `2x + 3x` khác `5x`, nên câu rút gọn vẫn bắt được bài chưa rút gọn. Thứ tự hai vế giữ nguyên (`x = 2` khác `2 = x`), thứ tự các phần cách bằng `;` cũng giữ nguyên. Chỉ liệt kê dạng khác thật sự (`1/2x` và `x/2`, hai cách viết nghiệm). `kiem.py` báo "lưu ý" cho dạng chỉ khác thứ tự.
- `thu_tu: true`: chỉ nhận đúng thứ tự trong `dap`. Dùng khi thứ tự là chính điều sách hỏi ("sắp xếp theo luỹ thừa giảm dần của x"). Không bật chỉ để giữ một cách viết đẹp.
- `loi_giai`: các dòng lời giải mẫu, đúng như một bài làm đủ bước. Hết sao thì máy hiện các dòng này. `sao` bằng số dòng, từ 1 tới 20.

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
  {"chu": "My best friend's name is {0}.", "o": [{"go": true}]},
  {"chu": "{He} {0} {1} the most.",
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
- Ô gõ: `{"go": true}`, con gõ bằng bàn phím chữ cái (dùng cho tên riêng), không tính sao, không chấm. `goi_y` là chữ mờ trong ô, mặc định "Gõ tên"; ô gõ người thân thì ghi ví dụ `"goi_y": "mum, brother..."`.
- `gioi: true`: máy hỏi trước "bạn nam hay bạn nữ", rồi thay `{He}` `{he}` `{His}` `{his}` `{Him}` `{him}` thành He/She, he/she, His/Her, his/her, Him/Her, him/her.
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
- Vẽ hình, vẽ đồ thị, lập bảng tự vẽ, làm miệng: không có `ghep`, ghi `bo_may`.

## Câu cần hình mà chưa có ảnh

Toán và Khoa học tự nhiên cắt hình sau cùng (anh Huy chốt 29/09/2026), vì chưa đủ bản quét. Câu nào không nhìn hình thì không làm được (đề ghi "Hình 3.5", biểu đồ, bảng số liệu in dạng ảnh) vẫn soạn `ghep` đầy đủ như thường, rồi ghi thêm trong file phần một bảng `can_hinh` từ mã câu sang lời tả hình cần cắt, ví dụ `"can_hinh": {"3.12": "Hình 3.5: hình thang ABCD, hai đường chéo cắt nhau tại O"}`. Lúc gộp vào sách (`tools/ghep/gop.py`), câu có trong `can_hinh` mà chưa có `hinh` thì phần ghép được cất vào trường `ghep_cho_hinh` và câu ghi `bo_may: "chờ hình: <lời tả>"`, để máy không giao câu thiếu hình. Khi đã cắt hình, gộp lại một file phần có bảng `hinh` cho câu đó: `ghep_cho_hinh` tự về lại `ghep` và `bo_may` bị xoá. Câu đề đã tả đủ hình bằng chữ ("Cho tam giác ABC vuông tại A, đường cao AH") thì không cần hình, không ghi vào `can_hinh`.

## Tiếng Anh: mã câu và cách chia

- Mã câu: `<Unit>.<phần><số bài>.<số câu>`, ví dụ `1.B3.2` là Unit 1, phần B, bài 3, câu 2. Test Yourself: `T1.3.2`.
- Bài có hai vế (viết từ dưới hình rồi xếp vào cột): từng hình một câu `1.A1.1`, `1.A1.2`..., phần xếp cột một câu `1.A1.cot`.
- Bài tìm lỗi sai: câu chọn chỗ sai `T1.4.1a` (`CHON`), câu sửa lại `T1.4.1b` (`CHU` hay `CAU`).
- Đoạn văn cuối Unit: phần 1 xếp câu thành đoạn `1.E3a` (`BUOC`, đoạn mẫu về một bạn A do mình viết, 6 tới 8 câu, cộng 2 câu lạc đề), phần 2 ghép theo câu gợi ý `1.E3b` (`O`, `gioi: true`, mỗi câu gợi ý một dòng với hai ô: một ô ngữ pháp, một ô nội dung, cộng dòng gõ tên). Nội dung phần 2 không trùng phần 1.
- Bỏ hẳn (không đưa vào file): bài làm theo cặp, theo nhóm, nói to, luyện đọc, phỏng vấn bạn. Ngoại lệ: trò chơi Unit 4 C2 là câu hỏi kiến thức, giữ lại.
- `bai`: `Unit 1. Leisure time`; Test Yourself thì `Test Yourself 1`.
- `nhom`: tên phần, ví dụ `A. Pronunciation`.
- `trang`: số trang in (trang PDF trừ 1).
- `de`: lời dẫn của bài, xuống dòng, rồi nội dung câu. Câu trắc nghiệm thì `de` giữ cả các phương án như sách in, còn `hoi` bỏ các phương án.
- `dang`, `dap_an`, `loai_dap_an`: câu `CHON` thì `TRAC_NGHIEM`, chữ cái đúng, `TN`; câu khác thì `CAU_NHO`, đáp án bằng chữ, `DAP_SO`.

## Bộ đề thi in sẵn (`dethianh8.json`, từ 30/09/2026)

Một file như file sách, nhưng mỗi `bai` là một đề nguyên vẹn, giữ đúng thứ tự in. Khai trong `NganHang.SACH` với `deThi = true`: không hiện ở màn chọn sách khi nộp bài, không vào làm thêm hay đề tuần, chỉ Giải đề dùng (`kho/DeThi.kt`, `GiaiDe.taoDeThi`, `GiaiDe.moDeThi`). Script sinh file nằm ngoài git, ở `ghep-nhap/dethianh/tao_de.py` cạnh ba repo. Đợt hai (giữa kì 1 số 4 tới 6, cuối kì 1 số 1 tới 8 trừ số 6, ngày 30/09/2026) soạn từ PDF bằng tác tử; bản chữ đề, file từng đề, ghi chú soát và script gộp nằm trong gói `de-thi-anh-dot2-2026-09-30.zip`, cũng ngoài git.

Ở mức bài, thêm `de_thi`:

```json
{"chuong": "Giữa học kì 1", "bai": "Đề giữa kì 1 số 1",
 "de_thi": {"ma": "GK1-1", "den_unit": 3, "phut": 45, "nguon_goc": "loigiaihay.com, đăng 11/11/2023"},
 "cac_cau": [...]}
```

- `ma`: mã ngắn của đề, đi vào khoá của đề đã mở và lệnh `MODETHI` của Bảng điều khiển. Không đổi về sau. Đề giữa kì 1 là `GK1-N` (`chuong` "Giữa học kì 1", `bai` "Đề giữa kì 1 số N"), đề học kì 1 là `CK1-N` (`chuong` "Cuối học kì 1", `bai` "Đề cuối kì 1 số N"), N là số đề của bản in.
- `den_unit`: Unit cuối cùng mà các câu làm trên máy chạm tới. Tablet tự mở đề khi Lê Hòa chọn "Lớp đã học tới" Tiếng Anh bằng hoặc vượt số này. Câu phát âm dùng từ của Unit sau (earthquake, nomadic) không tính, vì câu đó hỏi cách đọc chứ không hỏi nghĩa. Tên đề không quyết định số này: các đề giữa kì 1, cuối kì 1 loigiaihay đăng năm 2024 có câu của Unit 7 tới 9 (tornado, earthquake, mệnh đề Whenever), nên ghi 9 và chỉ mở ở học kì 2 (anh Huy chốt 30/09/2026).
- `phut`: giờ làm bài, cũng là đồng hồ của đề trên máy.

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

`kiem.py` kiểm thêm: mã đề không trùng, `den_unit` 1 tới 12, `phut` 10 tới 120, mã câu bắt đầu bằng mã đề và số câu in tăng dần, `trung` trỏ tới câu có ghép trong file và không mang nội dung riêng.
