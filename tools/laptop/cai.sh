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
systemctl daemon-reload
echo "uid laptop: $(python3 /usr/local/lib/netflix-gio/netflix_gio.py dangky)"
