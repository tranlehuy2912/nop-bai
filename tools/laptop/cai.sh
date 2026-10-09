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
# Ket noi nghe Firestore (9/10/2026) can thu vien Firestore cua Google: cai vao moi truong Python
# rieng, tao voi --system-site-packages de van thay Pillow cua he thong (lenh chup man hinh). Ban
# thu vien ghim theo lan cai dau tien, keo theo grpcio 1.84.0, protobuf 7.36.2, google-auth 2.61.0.
apt-get install -y -q python3-venv >/dev/null
[ -x /usr/local/lib/netflix-gio/venv/bin/python3 ] || python3 -m venv --system-site-packages /usr/local/lib/netflix-gio/venv
/usr/local/lib/netflix-gio/venv/bin/pip install -q --disable-pip-version-check google-cloud-firestore==2.34.1
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
# Phien cua lehoa: man chon Netflix hay YouTube (9/10/2026; truoc do chi co Netflix) roi Firefox toan
# man hinh, dang nhap khong can mat khau (chon ten la vao).
install -m 755 "$day/netflix-phien" /usr/local/bin/netflix-phien
install -m 755 "$day/netflix-chon" /usr/local/bin/netflix-chon
install -m 755 "$day/netflix-thoat" /usr/local/bin/netflix-thoat
# Xem trang Netflix luc moi mo, trang con trong thi tu bam F5 (8/10/2026), xem dau file netflix-canh.
install -m 755 "$day/netflix-canh" /usr/local/bin/netflix-canh
# Dai chu Ba Huy nhan len tivi (8/10/2026), netflix_gio.py goi khi co lenh NHAN.
install -m 755 "$day/netflix-nhan" /usr/local/bin/netflix-nhan
install -m 644 "$day/98netflix-lehoa" /etc/X11/Xsession.d/98netflix-lehoa
# Bat may la tu vao phien Netflix (8/10/2026), xem dau file 80-netflix-lehoa.conf.
install -m 644 "$day/80-netflix-lehoa.conf" /etc/lightdm/lightdm.conf.d/80-netflix-lehoa.conf
# Co tivi cam thi man dang nhap chi hien tren tivi (8/10/2026), xem dau file chi-tivi.
install -m 755 "$day/chi-tivi" /usr/local/bin/chi-tivi
install -m 644 "$day/81-chi-tivi.conf" /etc/lightdm/lightdm.conf.d/81-chi-tivi.conf
# Ten hien o man dang nhap la "Xem phim" (anh Huy chot 9/10/2026 khi phien co them YouTube; tu
# 8/10/2026 la "Netflix"; tai khoan huy hien "Admin"). Man dang nhap doc ten moi tu lan hien sau.
id lehoa >/dev/null 2>&1 || useradd -m -c "Xem phim" -s /bin/bash lehoa
usermod -c "Xem phim" lehoa
usermod -aG nopasswdlogin lehoa
# Che do han che YouTube muc "Vua" cho ca may, ke ca tai khoan Admin (anh Huy chot 9/10/2026). Firefox
# khong co luat bat che do nay, nen lam theo cach Google huong dan cho mang truong hoc: tro cac ten
# YouTube ve may chu han che restrictmoderate.youtube.com (muc "Gat" la restrict.youtube.com). Ghi ca
# IPv4 lan IPv6: laptop co IPv6 that, thieu dong IPv6 thi co the di vong qua DNS ra may chu thuong.
# Dia chi tra luc cai, khong tra duoc thi dung dia chi da biet ngay 9/10/2026. Firefox phai tat DNS
# qua HTTPS moi doc file nay (luat DNSOverHTTPS trong netflix_gio.py). Kiem tra: mo
# https://www.youtube.com/check_content_restrictions, phai ghi "Moderate restricted".
han_che=restrictmoderate.youtube.com
ip4=$(getent ahostsv4 $han_che | awk '{print $1; exit}')
ip6=$(getent ahostsv6 $han_che | awk '$1 !~ /^::ffff:/ {print $1; exit}')
ten_yt="www.youtube.com m.youtube.com youtubei.googleapis.com youtube.googleapis.com www.youtube-nocookie.com"
sed -i '/^# netflix-gio: YouTube han che/,/^# netflix-gio: het/d' /etc/hosts
{
    echo "# netflix-gio: YouTube han che muc Vua, cai.sh ghi, sua thi sua cai.sh"
    echo "${ip4:-216.239.38.119} $ten_yt"
    echo "${ip6:-2001:4860:4802:32::77} $ten_yt"
    echo "# netflix-gio: het"
} >> /etc/hosts
# Tieng ra tivi qua HDMI (9/10/2026). Tai khoan chua co lua chon dau ra thi WirePlumber chon loa
# laptop (analog-output-speaker uu tien 10000, hdmi-output-0 chi 5900): hinh len tivi ma tieng ra
# loa laptop, ca nha tuong tivi mat tieng. Ghi san lua chon HDMI nhu tai khoan huy tu luu ngay
# 1/10/2026; da co file (Le Hoa tu chon dau ra khac) thi de nguyen. Ten thiet bi la cua laptop HP
# nay (card HDMI o pci-0000_00_03.0); may khac thi xem pactl list short sinks. Rut tivi ra roi
# cam lai thi WirePlumber chon lai the nao: chua thu.
if [ ! -f /home/lehoa/.local/state/wireplumber/default-nodes ]; then
    runuser -u lehoa -- mkdir -p /home/lehoa/.local/state/wireplumber
    runuser -u lehoa -- sh -c 'printf "[default-nodes]\ndefault.configured.audio.sink=%s\ndefault.configured.audio.sink.0=%s\n" "$1" "$1" > /home/lehoa/.local/state/wireplumber/default-nodes' - alsa_output.pci-0000_00_03.0.hdmi-stereo
fi
# Timekpr-nExt: 0 phut moi ngay, het gio thi dang xuat, khong tu bao (chi bao tieng Viet tu
# netflix_gio.py, anh Huy chon 7/10/2026).
timekpra --settimelimits lehoa "0;0;0;0;0;0;0" >/dev/null
timekpra --setlockouttype lehoa terminate >/dev/null
timekpra --sethidetrayicon lehoa true >/dev/null
systemctl daemon-reload
echo "uid laptop: $(python3 /usr/local/lib/netflix-gio/netflix_gio.py dangky)"
# Chay lai dich vu neu dang chay; lan dau thi bat sau khi da ghi uid len console.
systemctl try-restart netflix-gio
