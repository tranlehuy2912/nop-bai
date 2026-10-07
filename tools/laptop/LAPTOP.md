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
- Firefox của cả máy, kể cả tài khoản của anh Huy, chỉ vào được Netflix. Bảng điều khiển có mục
  Bật/Tắt "Firefox chỉ được mở Netflix" trong Cài đặt, nhóm "Laptop" (lúc đầu là nút "Mở web" /
  "Khoá web" ở thẻ Giờ chơi, anh Huy chuyển chiều 07/10/2026), không có hạn. Firefox chỉ đọc luật
  lúc khởi động, nên đổi xong phải mở lại Firefox. Anh Huy chấp nhận điều này.

Mọi lựa chọn trên do anh Huy chốt ngày 07/10/2026.

## Máy và cách vào

| | Giá trị |
|---|---|
| Laptop | HP Pavilion 14, Linux Mint 22.3 bản MATE (không phải XFCE), Ubuntu noble, Firefox 146 bản deb |
| Tailscale | tài khoản `tranlehuy2912@gmail.com`; laptop `huy-hp-pavilion-14-notebook-pc` 100.93.227.126, Mac mini `mac-mini-ca-huy` 100.95.83.54, MacBook Pro `trns-macbook-pro` 100.107.197.55 |
| Mạng nhà | laptop ở 192.168.0.107; Mac mini và MacBook Pro ở mạng khác nên đi qua Tailscale |
| Vân tay khoá SSH ED25519 của laptop | `SHA256:ACKS/1gSt3nqz2oJ76SkiDeQzYRkNjTJKAATPnJP9Uo` |
| Màn đăng nhập | LightDM với slick-greeter |

Tài khoản trên laptop:

- `huy`: của anh Huy, quản trị, có mật khẩu (anh Huy đổi ngày 07/10/2026 vì Lê Hòa biết mật khẩu cũ). `sudo` phải gõ mật khẩu. SSH vào được bằng khoá.
- `harley`: dành cho Claude Code. Không có mật khẩu, ẩn khỏi màn đăng nhập (`/var/lib/AccountsService/users/harley`), chỉ vào bằng khoá SSH, `sudo` không cần mật khẩu (`/etc/sudoers.d/90-harley`). Mọi việc cài đặt chạy bằng tài khoản này.
- `lehoa`: của Lê Hòa, không có quyền quản trị, không có mật khẩu, thuộc nhóm `nopasswdlogin` nên chọn tên là vào.

SSH chỉ nhận khoá, chỉ cho `huy` và `harley` (`/etc/ssh/sshd_config.d/10-harley.conf`). Khoá đang được nhận: `harley@mac-mini-ca-huy` của Mac mini, và `huytl@cnv.vn` của MacBook Pro (thêm tối 07/10/2026). Đăng nhập SSH bằng mật khẩu đã tắt (`PasswordAuthentication no`).

### Cho máy Mac khác vào

Claude Code không được gõ mật khẩu, nên anh Huy làm phần cần mật khẩu:

1. Mac mới cài Tailscale, đăng nhập cùng tài khoản trên. Phần mở rộng mạng của Tailscale phải bật trong Cài đặt hệ thống, mục Login Items & Extensions. Trên Mac mini, lần đầu báo lỗi "permission denied" khi lưu cấu hình VPN; thoát hẳn Tailscale rồi mở lại, bấm Cho phép là được. MacBook Pro cài bằng `brew install --cask tailscale-app`; bật phần mở rộng xong mà cửa sổ Tailscale vẫn báo đỏ thì cũng thoát hẳn rồi mở lại.
2. Trên Mac mới tạo khoá: `ssh-keygen -t ed25519 -N "" -C "harley@<tên máy>" -f ~/.ssh/id_ed25519`. Mac đã có sẵn `~/.ssh/id_ed25519` thì dùng luôn, đừng ghi đè (MacBook Pro dùng khoá có sẵn).
3. Chép khoá vào cả hai tài khoản. Laptop không nhận đăng nhập SSH bằng mật khẩu, nên `ssh-copy-id` không chạy. Có hai cách. Một là phiên Claude Code trên Mac mini (đã vào được) thêm dòng khoá công khai của Mac mới vào `/home/harley/.ssh/authorized_keys` và `/home/huy/.ssh/authorized_keys`. Hai là anh Huy ngồi ở laptop, đăng nhập `huy`: trên Mac mới chạy tạm `python3 -m http.server 8765 --bind <IP Tailscale của Mac mới>` trong một thư mục chỉ có file khoá công khai tên `k`, anh Huy gõ trên laptop `(echo; curl -s <IP>:8765/k) | tee -a ~/.ssh/authorized_keys | sudo tee -a /home/harley/.ssh/authorized_keys` và gõ mật khẩu `huy` cho `sudo`, xong thì tắt chỗ tạm. MacBook Pro vào bằng cách hai. Anh Huy gõ tay theo ảnh chụp và dễ sót dấu cách, nên lệnh đưa anh gõ càng ngắn càng tốt.
4. Thử: `ssh harley@100.93.227.126 'sudo -n true && echo ok'`. Lần đầu so vân tay với bảng trên.

## Các phần đã có

### Trên laptop

- Timekpr-nExt bản 0.5.10, lấy từ PPA `ppa:mjasnik/ppa` (kho của Mint chỉ có 0.5.4). Tài khoản `lehoa`: giới hạn mỗi ngày 0 phút, đăng xuất khi hết giờ (`terminate`). Phút chỉ có khi được cộng bằng `timekpra --settimeleft lehoa + <giây>`; đã thử, giới hạn 0 vẫn cộng vượt được.
- Dịch vụ `netflix-gio` (systemd), chạy `/usr/local/lib/netflix-gio/netflix_gio.py chay` bằng quyền root. Cấu hình `/etc/netflix-gio/cauhinh.json` (khoá API, dự án, mã nhà), trạng thái `/var/lib/netflix-gio/trangthai.json` (refresh token của tài khoản ẩn danh, các phiếu đã cộng trong ngày). Việc của nó ghi ở đầu file `netflix_gio.py`. Xem nhật ký: `journalctl -u netflix-gio | grep -v timekpra-su`. Mỗi lần gọi `timekpra` để lại bốn dòng `timekpra-su`, đó là chuyện bình thường. Unit không được ghi `After=timekpr.service`: `timekpr.service` tự ghi `After=multi-user.target`, thành vòng, và lúc khởi động systemd bỏ việc bật `netflix-gio` để gỡ vòng (bản trước tối 07/10/2026 bị vậy, dịch vụ chỉ chạy khi có người bật tay). Dịch vụ chỉ nhận phiếu và đẩy trạng thái lên Firestore khi đồng hồ đã lấy giờ mạng, xem mục đồng hồ ở "Những chỗ dễ vấp".
- Luật chặn web `/etc/firefox/policies/policies.json` do dịch vụ trên đặt hay gỡ, áp cho mọi tài khoản.
- Phiên của Lê Hòa: `/etc/X11/Xsession.d/98netflix-lehoa` thay mọi phiên của `lehoa` bằng `/usr/local/bin/netflix-phien`. Script này chờ luật chặn web có mặt, chạy `marco` (không chạy `timekprc`) và `/usr/local/bin/netflix-thoat` (nút "✕" màu đỏ, chỉ hiện khi rê chuột vào góc phải trên, bấm thì gửi SIGTERM cho Firefox, phiên kết thúc; anh Huy chọn vì con hay dùng chuột), rồi mở `firefox --kiosk https://www.netflix.com/`. Đóng Firefox là đăng xuất. Trước khi mở Firefox, script xoá ba dòng `extensions.installedDistroAddon.langpack-*` trong `prefs.js` của hồ sơ (lý do ở mục 2 của "Việc còn lại") và đặt `browser.startup.couldRestoreSession.count` bằng 2 để Firefox không hiện thanh "Open previous tabs?".
- Gói `xdotool`, cài lúc thử.

Cài lại hay chuyển sang dự án khác bằng `tools/laptop/cai.sh` (đọc đầu file): chép thư mục `tools/laptop` lên laptop, chạy `sudo sh cai.sh <google-services.json> <mã nhà>`. Script in uid ẩn danh của laptop. uid đó phải được ghi vào trường `uidLaptop` của document `laptop/<mã nhà>` qua console Firebase, rồi `systemctl enable --now netflix-gio`.

### Trên Firestore

Chi tiết ở `Duong.kt` (mục `LAPTOP`, `CAP`) và `bang-dieu-khien/firestore.rules` (mục `laptop/{nhaId}`).

- `laptop/{maNha}` nằm ngoài `nha/`, vì laptop không phải người nhà. Claude Code tạo document này qua console, với `uidLaptop`. Laptop ghi `ketThucLuc`, `conLaiMs`, `dangDung`, `webDangMo`, `capCuoi`, `capNhatLuc`, và chỉ ghi khi có gì đổi. Người nhà chỉ ghi được `moWeb` (kiểu bool).
- `laptop/{maNha}/cap/{id}`: phiếu cấp giờ `{ phut, phutChoi, ai, taoLuc }`. Người nhà tạo, laptop đọc mỗi phút, cộng giờ rồi xoá. Phiếu tạo từ hôm trước thì laptop bỏ.
- Laptop đăng nhập ẩn danh qua REST (`identitytoolkit`, `securetoken`) bằng khoá API trong `google-services.json`. Khoá đó không bị giới hạn chỉ cho Android; đã thử được.

### Trong ba app

Làm ở nhánh `laptop-netflix` của ba repo, đã gộp `main` và đẩy GitHub ngày 07/10/2026:

- `nop-bai`: nút "Netflix" ở hàng dưới màn chính, chỉ hiện khi laptop đã nối (`ui/DoiNetflix`, `dongbo/Laptop`), `GateStore.doiPhut`, tỉ lệ `Prefs.tiLeNetflix` qua lệnh `CAIDAT tiLeNetflix`, `tools/laptop/`. Test: 6 test `doi_*` trong `GateStoreTest`.
- `bang-dieu-khien`: thẻ Giờ chơi có dòng "Xem Netflix" (tên lúc đầu "Netflix trên laptop"; còn bao nhiêu phút, nút "Cấp thêm" 15, 30, 45, 60 phút hay gõ số từ 1 tới 600 ở dòng "Khác", 600 là trần của luật Firestore cho phiếu cấp). Cài đặt có nhóm "Laptop": mục "Tỷ lệ đổi Netflix" (ghi kiểu "1:2", chọn từ 1:1 tới 1:5; trước 07/10/2026 tên là "Đổi sang Netflix") và mục Bật/Tắt "Firefox chỉ được mở Netflix" (dòng nhỏ bên dưới ghi laptop đã mở hay khoá web chưa). Mục Bật/Tắt này lúc đầu là dòng trạng thái web kèm nút "Mở web" / "Khoá web" ở thẻ Giờ chơi, chuyển sang Cài đặt chiều 07/10/2026. Luật Firestore mới.
- `cho-gio-choi`: chỉ chép `Duong.kt`.

Commit được làm trong các worktree `.worktrees/netflix-<repo>` ở gốc workspace trên Mac mini, vì có nhiều phiên khác cùng làm trong cây chính.

## Tình trạng (cập nhật 07/10/2026, 20 giờ 30)

- Đã đưa lên bản thật: ba repo gộp vào `main` và đẩy GitHub; luật Firestore mới đã đăng lên cả dự án thử lẫn dự án thật `nop-bai-4934d` (14 giờ 52); laptop nối dự án thật, nhà `czfy4pdxn6gz5u8t3hap`, uid ẩn danh của laptop ghi ở trường `uidLaptop` của `laptop/czfy4pdxn6gz5u8t3hap`. Document `laptop/tp9vjs55v5zpp3zjaybx` bên dự án thử vẫn để đó cho máy ảo.
- Web đang khoá cho cả máy. `lehoa` có 0 phút.
- Đã thử từ xa phiên của Lê Hòa: chọn "Lê Hòa" là có nút Đăng nhập, không hỏi mật khẩu; Netflix toàn màn hình trên tivi; nút ✕ ở góc phải trên; thông báo "Còn N phút xem Netflix" (30 giây) lúc còn 5 phút; hết giờ đăng xuất; 0 phút thì chỉ hiện hộp "Hết phút xem Netflix..." rồi đăng xuất.
- Hồ sơ Firefox của Lê Hòa đã có Widevine bản 4.10.3050.0, mở lại không tải nữa.
- Từ khoảng 16 giờ, mở Netflix là dừng ở màn chọn hồ sơ "Who's watching?" (mọi hồ sơ đều có khoá PIN), trong khi lúc 15 giờ 20 thì vào thẳng "Home - Netflix". Chưa rõ vì sao. Có vẻ không do `prefs.js`: lần thử đầu tiên lúc 16 giờ 10, với `prefs.js` chỉ có 10 dòng, cũng dừng ở màn đó.
- Tối 07/10/2026 (phiên trên MacBook Pro): `netflix-gio` đã tắt từ một lần khởi động lại chiều tối, vì vòng thứ tự nói ở mục "Trên laptop". Đã sửa unit, thêm bước chờ đồng hồ lấy giờ mạng, và khi chưa đọc được Timekpr-nExt thì thử lại sau 15 giây thay vì một phút. Khởi động lại thử lúc 20 giờ 26: dịch vụ tự chạy ở giây 89, chờ đồng hồ (lấy giờ mạng ở giây 96), Timekpr-nExt lên ở giây 99, vòng sau ghi được số phút. Lúc bật lại dịch vụ, ba phiếu Ba Huy cấp trong ngày còn nằm chờ (20, 20, 30 phút) được cộng một lượt, `lehoa` có 70 phút; anh Huy bảo đặt lại 0 phút (20 giờ 31).

## Việc còn lại

1. Đã xong 07/10/2026 15 giờ: anh Huy tự đăng nhập Netflix qua VNC (`x11vnc` đã cài, mật khẩu VNC anh Huy đặt ở `/home/harley/.vnc/passwd`; bật bằng `sudo x11vnc -display :0 -auth /home/lehoa/.Xauthority -rfbauth /home/harley/.vnc/passwd -listen 100.93.227.126 -rfbport 5900 -forever -shared -bg`, bản 0.9.16 vẫn mở thêm cổng IPv6 dù có `-no6`/`-listen6`, nên chặn tạm bằng `ip6tables -I INPUT -p tcp --dport 5900 ! -i tailscale0 -j DROP` rồi gỡ khi tắt). Muốn VNC thấy phiên của Lê Hòa thì cấp ít phút và đăng nhập phiên đó trước (X của phiên bị dựng lại mỗi lần đăng nhập, nên `x11vnc` phải bật sau khi đăng nhập). Phía anh Huy: máy Mac đã vào Tailscale thì chạy `open vnc://100.93.227.126`, gõ mật khẩu VNC; không cần khoá SSH, chỉ phiên bật VNC mới cần. Tắt: `sudo pkill -x x11vnc`, gỡ luật `ip6tables -D ...` cùng tham số, đăng xuất phiên, đặt 0 phút. Đăng nhập lại thì vào thẳng "Home - Netflix". VNC đã tắt.
2. Đã xong 07/10/2026 16 giờ 30: Widevine không còn tải lại mỗi lần mở. Trước đó `netflix-phien` xoá cả `prefs.js` để tránh màn đen, mất luôn dấu "đã cài Widevine" (`media.gmp-widevinecdm.*`), nên lần nào Firefox cũng hiện "Firefox is installing components needed to play the audio or video..." và phải tải lại. Chia đôi `prefs.js` hỏng (90 dòng) thì màn đen chỉ hiện khi có đủ hai nhóm dòng: ba dòng `extensions.installedDistroAddon.langpack-en-CA`, `-en-GB`, `-vi` (gói ngôn ngữ kèm Firefox ở `/usr/lib/firefox/distribution/extensions`), và hai dòng `extensions.lastAppBuildId`, `extensions.lastAppVersion`. Bỏ một trong hai nhóm là Netflix hiện bình thường, nên `netflix-phien` giờ chỉ xoá nhóm đầu. Mười dòng `media.gmp*` không liên quan. Chưa rõ vì sao hai nhóm đó gây màn đen. Có thể luật chặn mọi tiện ích (`ExtensionSettings` `"*": blocked`) đã gỡ gói ngôn ngữ (thư mục `extensions` của hồ sơ trống) trong khi Firefox vẫn tin là đã cài. Thử với `netflix-phien` mới, đăng nhập bốn lần liền: không lần nào màn đen. Lần đầu Firefox bắt đầu cài Widevine sau khoảng 60 giây và cài xong 47 giây sau đó. Các lần sau không tải lại (`media.gmp-widevinecdm.version` vẫn là 4.10.3050.0, `lastDownload` không đổi) và không còn thanh báo nào trên Netflix. Chưa thử phát phim, vì mọi hồ sơ Netflix đều có khoá PIN. Các file thử để lại ở `/root` của laptop, xoá được: `prefs-hong.js` (bản hỏng), `ds-tatca.txt` và các danh sách nhóm con, `thu2.sh <danh sách>` (dựng `prefs.js` chỉ gồm các dòng trong danh sách rồi đăng nhập; muốn dùng thì tạm tắt dòng sửa `prefs.js` trong `netflix-phien`), `thu-that.sh <giây>` (đăng nhập như thật, chờ rồi chụp), `thu-cai.sh <giây tối đa>` (chờ Firefox cài xong Widevine). Trước khi chạy cả ba đều phải cấp phút (`timekpra --settimeleft lehoa = 1200`), và phải chờ tới khi `/run/netflix-gio/conlai` có số mới (tối đa một phút), không thì phiên chỉ hiện hộp "Hết phút". Xong thì đặt lại 0 phút.
3. Anh Huy cài bản mới của Nộp bài lên tablet thật và Bảng điều khiển lên điện thoại thật. Chưa cài thì tablet không có nút Netflix và điện thoại không có dòng "Netflix trên laptop".

## Những chỗ dễ vấp

- Không gõ mật khẩu, không đăng nhập Netflix hay Google thay anh Huy.
- Laptop có lúc có người đang dùng (07/10/2026 đang mở YouTube trên tài khoản `huy`). Đừng gõ phím hay bấm chuột lên màn hình laptop bằng `xdotool`: hôm đó vài phím đã lọt vào cửa sổ của người đang dùng. Muốn thử Firefox thì dùng hồ sơ riêng (`firefox --new-instance --profile /tmp/...`) và chỉ chụp màn hình.
- Chụp hay bấm trên màn đăng nhập (chưa ai đăng nhập) phải dùng `sudo env DISPLAY=:0 XAUTHORITY=/run/lightdm/root/:0`. Gửi thông báo vào phiên của Lê Hòa phải có `XAUTHORITY=/home/lehoa/.Xauthority`, thiếu thì `notify-send` thoát mã 0 mà không hiện gì.
- `timekpra --getuserinfort` ghi số đang chạy dưới tên `ACTUAL_TIME_LEFT_DAY`, còn `--getuserinfo` ghi `TIME_LEFT_DAY`. Lúc `lehoa` chưa đăng nhập thì `--getuserinfort` chỉ in một dòng tiêu đề.
- Tiêu đề cửa sổ "Netflix — Mozilla Firefox" chưa chắc là màn đen: màn chọn hồ sơ "Who's watching?" cũng mang tiêu đề đó. Phân biệt bằng kích thước ảnh chụp (màn đen khoảng 6 KB, có hình từ khoảng 680 KB) hoặc tải ảnh về xem.
- Đọc mã JavaScript của Firefox ngay trên laptop bằng `unzip -p /usr/lib/firefox/omni.ja modules/<tên file>`; phần trình duyệt nằm ở `/usr/lib/firefox/browser/omni.ja`. `unzip` báo lỗi định dạng nhưng vẫn đọc được.
- Sửa `/var/lib/AccountsService/users/<tên>` xong phải `systemctl restart accounts-daemon` rồi khởi động lại `lightdm` thì màn đăng nhập mới ẩn tài khoản.
- `pkill -f "<chuỗi>"` qua SSH khớp luôn chính câu lệnh của phiên SSH và tự giết nó. Viết chuỗi có ngoặc vuông, ví dụ `pkill -f "[f]f-thu"`. Tiến trình chính của Firefox tên là `firefox-bin`, nên `pkill -x firefox` không bắt được gì.
- Đầu ra của `curl` trên Mac mini đi qua bộ lọc `rtk` và bị cắt. Muốn đọc JSON thô thì dùng `rtk proxy curl`.
- Laptop xuất hình ra tivi qua HDMI. Một phiên khác ngày 07/10/2026 có hỏi về việc phát tiếng và hiện hộp thoại ra tivi.
- Laptop mất giờ mỗi lần tắt hẳn (thấy tối 07/10/2026): lúc khởi động đồng hồ là 26/11/2025 (mốc của systemd), `systemd-timesyncd` đặt lại thành giờ đã lưu lần trước, rồi mới lấy giờ mạng. Có thể pin đồng hồ trong máy đã hết, chưa kiểm. Tắt từ tối qua thì trong khoảng đó máy tưởng vẫn là hôm qua, nên `netflix_gio.py` chờ có `/run/systemd/timesync/synchronized` (hoặc `timedatectl` báo `NTPSynchronized=yes`) rồi mới nhận phiếu, không thì phiếu sáng nay bị coi là phiếu cũ và bị xoá. Timekpr-nExt xử lý cú nhảy giờ này ra sao thì chưa thử.
