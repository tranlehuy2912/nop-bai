# Laptop xem Netflix của Lê Hòa

Viết ngày 07/10/2026 để một phiên Claude Code trên máy khác của anh Huy nắm được ngữ cảnh mà không
cần đọc lại cuộc trò chuyện. Tình trạng ghi ở đây là tình trạng lúc viết; trước khi làm tiếp thì
kiểm lại trên laptop và trên Firestore.

## Mục đích

Laptop cài Linux Mint, Lê Hòa dùng để xem Netflix. Anh Huy muốn:

- Tài khoản của Lê Hòa chỉ mở được Netflix, không làm được gì khác.
- Phút xem Netflix đổi từ phút chơi của tablet: Lê Hòa bấm nút "Netflix" trên tablet, chọn số phút
  chơi, tablet trừ phút trong phiếu hôm nay và cấp gấp đôi số đó thành phút Netflix (tỉ lệ đổi được
  trong Cài đặt của Bảng điều khiển, mặc định 2). Ba Huy cũng cấp thêm được trên Bảng điều khiển, và
  phần này không trừ phút chơi.
- Báo trước 5 phút, hết giờ thì đăng xuất. Không áp giờ ngủ. Phút Netflix chỉ dùng trong ngày,
  nửa đêm là hết, giống phiếu giờ chơi của tablet.
- Firefox của cả máy, kể cả tài khoản của anh Huy, chỉ vào được Netflix. Bảng điều khiển có nút
  "Mở web" / "Khoá web", không có hạn. Firefox chỉ đọc luật lúc khởi động, nên đổi xong phải mở
  lại Firefox. Anh Huy chấp nhận điều này.

Mọi lựa chọn trên do anh Huy chốt ngày 07/10/2026.

## Máy và cách vào

| | Giá trị |
|---|---|
| Laptop | HP Pavilion 14, Linux Mint 22.3 bản MATE (không phải XFCE), Ubuntu noble, Firefox 146 bản deb |
| Tailscale | tài khoản `tranlehuy2912@gmail.com`; laptop `huy-hp-pavilion-14-notebook-pc` 100.93.227.126, Mac mini `mac-mini-ca-huy` 100.95.83.54 |
| Mạng nhà | laptop ở 192.168.0.107; Mac mini ở chỗ khác nên đi qua Tailscale |
| Vân tay khoá SSH ED25519 của laptop | `SHA256:ACKS/1gSt3nqz2oJ76SkiDeQzYRkNjTJKAATPnJP9Uo` |
| Màn đăng nhập | LightDM với slick-greeter |

Tài khoản trên laptop:

- `huy`: của anh Huy, quản trị, có mật khẩu (anh Huy đổi ngày 07/10/2026 vì Lê Hòa biết mật khẩu cũ). `sudo` phải gõ mật khẩu. SSH vào được bằng khoá.
- `harley`: dành cho Claude Code. Không có mật khẩu, ẩn khỏi màn đăng nhập (`/var/lib/AccountsService/users/harley`), chỉ vào bằng khoá SSH, `sudo` không cần mật khẩu (`/etc/sudoers.d/90-harley`). Mọi việc cài đặt chạy bằng tài khoản này.
- `lehoa`: của Lê Hòa, không có quyền quản trị, không có mật khẩu, thuộc nhóm `nopasswdlogin` nên chọn tên là vào.

SSH chỉ nhận khoá, chỉ cho `huy` và `harley` (`/etc/ssh/sshd_config.d/10-harley.conf`). Khoá đang được nhận là khoá `harley@mac-mini-ca-huy` của Mac mini.

### Cho máy Mac khác vào

Claude Code không được gõ mật khẩu, nên anh Huy làm phần cần mật khẩu:

1. Mac mới cài Tailscale, đăng nhập cùng tài khoản trên. Phần mở rộng mạng của Tailscale phải bật trong Cài đặt hệ thống, mục Login Items & Extensions. Trên Mac mini, lần đầu báo lỗi "permission denied" khi lưu cấu hình VPN; thoát hẳn Tailscale rồi mở lại, bấm Cho phép là được.
2. Trên Mac mới tạo khoá: `ssh-keygen -t ed25519 -N "" -C "harley@<tên máy>" -f ~/.ssh/id_ed25519`.
3. Chép khoá vào cả hai tài khoản. Tài khoản `harley` không có mật khẩu nên không dùng được `ssh-copy-id` trực tiếp. Cách đơn giản là để phiên Claude Code trên Mac mini (đã vào được) thêm dòng khoá công khai của Mac mới vào `/home/harley/.ssh/authorized_keys` và `/home/huy/.ssh/authorized_keys`. Cách khác: anh Huy chạy `ssh-copy-id huy@100.93.227.126` (gõ mật khẩu `huy`), rồi `ssh -t huy@100.93.227.126 'sudo tee -a /home/harley/.ssh/authorized_keys < ~/.ssh/authorized_keys'` và gõ mật khẩu cho `sudo`.
4. Thử: `ssh harley@100.93.227.126 'sudo -n true && echo ok'`. Lần đầu so vân tay với bảng trên.

## Các phần đã có

### Trên laptop

- Timekpr-nExt bản 0.5.10, lấy từ PPA `ppa:mjasnik/ppa` (kho của Mint chỉ có 0.5.4). Tài khoản `lehoa`: giới hạn mỗi ngày 0 phút, đăng xuất khi hết giờ (`terminate`). Phút chỉ có khi được cộng bằng `timekpra --settimeleft lehoa + <giây>`; đã thử, giới hạn 0 vẫn cộng vượt được.
- Dịch vụ `netflix-gio` (systemd), chạy `/usr/local/lib/netflix-gio/netflix_gio.py chay` bằng quyền root. Cấu hình `/etc/netflix-gio/cauhinh.json` (khoá API, dự án, mã nhà), trạng thái `/var/lib/netflix-gio/trangthai.json` (refresh token của tài khoản ẩn danh, các phiếu đã cộng trong ngày). Việc của nó ghi ở đầu file `netflix_gio.py`. Xem nhật ký: `journalctl -u netflix-gio | grep -v timekpra-su`. Mỗi lần gọi `timekpra` để lại bốn dòng `timekpra-su`, đó là chuyện bình thường.
- Luật chặn web `/etc/firefox/policies/policies.json` do dịch vụ trên đặt hay gỡ, áp cho mọi tài khoản.
- Phiên của Lê Hòa: `/etc/X11/Xsession.d/98netflix-lehoa` thay mọi phiên của `lehoa` bằng `/usr/local/bin/netflix-phien`. Script này chờ luật chặn web có mặt, chạy `marco` (không chạy `timekprc`) và `/usr/local/bin/netflix-thoat` (nút "✕" màu đỏ, chỉ hiện khi rê chuột vào góc phải trên, bấm thì gửi SIGTERM cho Firefox, phiên kết thúc; anh Huy chọn vì con hay dùng chuột), rồi mở `firefox --kiosk https://www.netflix.com/`. Đóng Firefox là đăng xuất.
- Gói `xdotool`, cài lúc thử.

Cài lại hay chuyển sang dự án khác bằng `tools/laptop/cai.sh` (đọc đầu file): chép thư mục `tools/laptop` lên laptop, chạy `sudo sh cai.sh <google-services.json> <mã nhà>`. Script in uid ẩn danh của laptop. uid đó phải được ghi vào trường `uidLaptop` của document `laptop/<mã nhà>` qua console Firebase, rồi `systemctl enable --now netflix-gio`.

### Trên Firestore

Chi tiết ở `Duong.kt` (mục `LAPTOP`, `CAP`) và `bang-dieu-khien/firestore.rules` (mục `laptop/{nhaId}`).

- `laptop/{maNha}` nằm ngoài `nha/`, vì laptop không phải người nhà. Claude Code tạo document này qua console, với `uidLaptop`. Laptop ghi `ketThucLuc`, `conLaiMs`, `dangDung`, `webDangMo`, `capCuoi`, `capNhatLuc`, và chỉ ghi khi có gì đổi. Người nhà chỉ ghi được `moWeb` (kiểu bool).
- `laptop/{maNha}/cap/{id}`: phiếu cấp giờ `{ phut, phutChoi, ai, taoLuc }`. Người nhà tạo, laptop đọc mỗi phút, cộng giờ rồi xoá. Phiếu tạo từ hôm trước thì laptop bỏ.
- Laptop đăng nhập ẩn danh qua REST (`identitytoolkit`, `securetoken`) bằng khoá API trong `google-services.json`. Khoá đó không bị giới hạn chỉ cho Android; đã thử được.

### Trong ba app

Nhánh `laptop-netflix` của ba repo, chưa gộp `main`, chưa đẩy GitHub lúc viết file này:

- `nop-bai`: nút "Netflix" ở hàng dưới màn chính, chỉ hiện khi laptop đã nối (`ui/DoiNetflix`, `dongbo/Laptop`), `GateStore.doiPhut`, tỉ lệ `Prefs.tiLeNetflix` qua lệnh `CAIDAT tiLeNetflix`, `tools/laptop/`. Test: 6 test `doi_*` trong `GateStoreTest`.
- `bang-dieu-khien`: thẻ Giờ chơi có dòng "Netflix trên laptop" (còn bao nhiêu phút, nút "Cấp thêm" 15, 30, 60 phút) và dòng trạng thái web với nút "Mở web" / "Khoá web". Cài đặt có mục "Đổi sang Netflix" (1 đến 5). Luật Firestore mới.
- `cho-gio-choi`: chỉ chép `Duong.kt`.

Commit được làm trong các worktree `.worktrees/netflix-<repo>` ở gốc workspace trên Mac mini, vì có nhiều phiên khác cùng làm trong cây chính.

## Tình trạng (cập nhật 07/10/2026, 15 giờ)

- Đã đưa lên bản thật: ba repo gộp vào `main` và đẩy GitHub; luật Firestore mới đã đăng lên cả dự án thử lẫn dự án thật `nop-bai-4934d` (14 giờ 52); laptop nối dự án thật, nhà `czfy4pdxn6gz5u8t3hap`, uid ẩn danh của laptop ghi ở trường `uidLaptop` của `laptop/czfy4pdxn6gz5u8t3hap`. Document `laptop/tp9vjs55v5zpp3zjaybx` bên dự án thử vẫn để đó cho máy ảo.
- Web đang khoá cho cả máy. `lehoa` có 0 phút.
- Đã thử từ xa phiên của Lê Hòa: chọn "Lê Hòa" là có nút Đăng nhập, không hỏi mật khẩu; Netflix toàn màn hình trên tivi; nút ✕ ở góc phải trên; thông báo "Còn N phút xem Netflix" (30 giây) lúc còn 5 phút; hết giờ đăng xuất; 0 phút thì chỉ hiện hộp "Hết phút xem Netflix..." rồi đăng xuất.

## Việc còn lại

1. Anh Huy đăng nhập tài khoản Netflix một lần trong phiên của Lê Hòa (cần cấp ít phút trước); Claude Code không đăng nhập thay.
2. Anh Huy cài bản mới của Nộp bài lên tablet thật và Bảng điều khiển lên điện thoại thật. Chưa cài thì tablet không có nút Netflix và điện thoại không có dòng "Netflix trên laptop".

## Những chỗ dễ vấp

- Không gõ mật khẩu, không đăng nhập Netflix hay Google thay anh Huy.
- Laptop có lúc có người đang dùng (07/10/2026 đang mở YouTube trên tài khoản `huy`). Đừng gõ phím hay bấm chuột lên màn hình laptop bằng `xdotool`: hôm đó vài phím đã lọt vào cửa sổ của người đang dùng. Muốn thử Firefox thì dùng hồ sơ riêng (`firefox --new-instance --profile /tmp/...`) và chỉ chụp màn hình.
- Chụp hay bấm trên màn đăng nhập (chưa ai đăng nhập) phải dùng `sudo env DISPLAY=:0 XAUTHORITY=/run/lightdm/root/:0`. Gửi thông báo vào phiên của Lê Hòa phải có `XAUTHORITY=/home/lehoa/.Xauthority`, thiếu thì `notify-send` thoát mã 0 mà không hiện gì.
- `timekpra --getuserinfort` ghi số đang chạy dưới tên `ACTUAL_TIME_LEFT_DAY`, còn `--getuserinfo` ghi `TIME_LEFT_DAY`.
- Sửa `/var/lib/AccountsService/users/<tên>` xong phải `systemctl restart accounts-daemon` rồi khởi động lại `lightdm` thì màn đăng nhập mới ẩn tài khoản.
- `pkill -f "<chuỗi>"` qua SSH khớp luôn chính câu lệnh của phiên SSH và tự giết nó. Viết chuỗi có ngoặc vuông, ví dụ `pkill -f "[f]f-thu"`. Tiến trình chính của Firefox tên là `firefox-bin`, nên `pkill -x firefox` không bắt được gì.
- Đầu ra của `curl` trên Mac mini đi qua bộ lọc `rtk` và bị cắt. Muốn đọc JSON thô thì dùng `rtk proxy curl`.
- Laptop xuất hình ra tivi qua HDMI. Một phiên khác ngày 07/10/2026 có hỏi về việc phát tiếng và hiện hộp thoại ra tivi.
