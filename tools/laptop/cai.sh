#!/bin/sh
# Cai (hay cai lai) chuong trinh cap gio Netflix len laptop. Chay bang root tren laptop:
#   sudo sh cai.sh <duong-dan-google-services.json> <maNha>
# File google-services.json lay tu app nao cung duoc (ba app chung mot du an Firebase):
# dung file trong app/src/debug/ la noi du an THU, file trong app/ la du an THAT.
set -e
gs="$1"; nha="$2"
[ -f "$gs" ] && [ -n "$nha" ] || { echo "Cach chay: sudo sh cai.sh google-services.json maNha"; exit 1; }
day=$(dirname "$0")
install -d -m 755 /usr/local/lib/netflix-gio
install -m 755 "$day/netflix_gio.py" /usr/local/lib/netflix-gio/netflix_gio.py
install -d -m 700 /etc/netflix-gio /var/lib/netflix-gio
python3 - "$gs" "$nha" <<'PY'
import json, sys
d = json.load(open(sys.argv[1]))
ch = {"apiKey": d["client"][0]["api_key"][0]["current_key"],
      "projectId": d["project_info"]["project_id"],
      "maNha": sys.argv[2], "nguoiDung": "lehoa"}
open("/etc/netflix-gio/cauhinh.json", "w").write(json.dumps(ch, indent=1))
print("du an", ch["projectId"], "nha", ch["maNha"])
PY
chmod 600 /etc/netflix-gio/cauhinh.json
install -m 644 "$day/netflix-gio.service" /etc/systemd/system/netflix-gio.service
# Phien chi co Netflix cho lehoa, va dang nhap khong can mat khau (chon ten la vao).
install -m 755 "$day/netflix-phien" /usr/local/bin/netflix-phien
install -m 755 "$day/netflix-thoat" /usr/local/bin/netflix-thoat
# Dai chu Ba Huy nhan len tivi (8/10/2026), netflix_gio.py goi khi co lenh NHAN.
install -m 755 "$day/netflix-nhan" /usr/local/bin/netflix-nhan
install -m 644 "$day/98netflix-lehoa" /etc/X11/Xsession.d/98netflix-lehoa
# Bat may la tu vao phien Netflix (8/10/2026), xem dau file 80-netflix-lehoa.conf.
install -m 644 "$day/80-netflix-lehoa.conf" /etc/lightdm/lightdm.conf.d/80-netflix-lehoa.conf
# Ten hien o man dang nhap la "Netflix" (anh Huy doi 8/10/2026; tai khoan huy hien "Admin").
id lehoa >/dev/null 2>&1 || useradd -m -c "Netflix" -s /bin/bash lehoa
usermod -aG nopasswdlogin lehoa
# Timekpr-nExt: 0 phut moi ngay, het gio thi dang xuat, khong tu bao (chi bao tieng Viet tu
# netflix_gio.py, anh Huy chon 7/10/2026).
timekpra --settimelimits lehoa "0;0;0;0;0;0;0" >/dev/null
timekpra --setlockouttype lehoa terminate >/dev/null
timekpra --sethidetrayicon lehoa true >/dev/null
systemctl daemon-reload
echo "uid laptop: $(python3 /usr/local/lib/netflix-gio/netflix_gio.py dangky)"
# Chay lai dich vu neu dang chay; lan dau thi bat sau khi da ghi uid len console.
systemctl try-restart netflix-gio
