#!/usr/bin/env python3
# Chuong trinh chay ngam tren laptop Linux Mint cua Le Hoa (7/10/2026).
#
# Viec cua no:
#  1. Moi phut hoi Firestore xem co phieu cap gio Netflix moi khong (laptop/{maNha}/cap,
#     xem Duong.LAPTOP, Duong.CAP ben ba app). Phieu do tablet ghi khi Le Hoa doi phut choi,
#     hay Bang dieu khien ghi khi Ba Huy cho them.
#  2. Cong gio cho tai khoan Le Hoa bang Timekpr-nExt (timekpra --settimeleft ... +), roi xoa
#     phieu. Phieu phut am (tu 9/10/2026, nut "Bớt Netflix") thi bot, toi da ve 0. Timekpr-nExt dem gio, bao truoc, va dang xuat khi het gio; o day khong dem gi.
#  3. Ghi lai vao document laptop/{maNha} luc nao het gio (khi con dang dung) hay con bao
#     nhieu (khi khong ai dung), de Bang dieu khien hien "Laptop còn N phút". Chi ghi khi co
#     gi doi, khong ghi moi phut: giu so luot ghi Firestore o muc vai chuc mot ngay.
#  4. Con 5 phut thi hien thong bao trong phien cua Le Hoa (anh Huy chon bao truoc 5 phut).
#  5. Giu luat chan web cua Firefox (/etc/firefox/policies/policies.json): Firefox chi vao
#     Netflix, cho ca may vi Firefox khong co luat rieng tung tai khoan (anh Huy chon khoa ca
#     may). Ba Huy bam "Mở web" tren Bang dieu khien (truong moWeb) thi go luat ra, bam "Khoá
#     web" thi dat lai; khong co han (anh Huy chon). Le Hoa dang nhap thi luat luon co mat,
#     du moWeb dang bat. Firefox chi doc luat luc khoi dong: doi xong phai mo lai Firefox.
#  6. Tu 8/10/2026 lam lenh Ba Huy gui tu Bang dieu khien (laptop/{maNha}/lenh, xem
#     Duong.LenhLaptop ben ba app): nhan len tivi (dai chu netflix-nhan kem giong doc Google
#     Dich), chup man hinh (laptop/{maNha}/anh/moinhat), dang xuat, tat may, va HOI (dien thoai
#     mo tab Gio choi, laptop ghi lai trang thai de dien thoai biet laptop con song). Hoi chung
#     vong mot phut voi phieu: anh Huy chot giu mot phut, lenh tre toi da khoang mot phut.
#     Lam xong ghi ket qua vao truong ketQua.
#  7. Ghi ai dang ngoi man hinh (phien, phienTu) va cac lan bat may, tat may, dang nhap, dang
#     xuat trong ngay (suKien), cho dong "Hôm nay: ..." tren dien thoai.
#
# Chi dung thu vien chuan cua Python 3 (Mint 22 co san 3.12), khong cai them goi pip nao. Rieng
# chup man hinh dung Pillow (python3-pil) va netflix-nhan dung GTK 3 (python3-gi), deu co san
# trong Mint; thieu thi chi lenh do hong, phan con lai van chay.
# Dang nhap Firebase an danh qua REST, nhu ba app Android, nhung bang khoa API trong
# google-services.json. uid cua laptop phai duoc ghi vao truong uidLaptop cua document
# laptop/{maNha} (Claude Code ghi qua console), luat Firestore moi cho laptop doc ghi.
#
# Lenh:
#   netflix_gio.py dangky   dang nhap an danh lan dau, in uid de ghi vao console
#   netflix_gio.py mot      chay mot vong roi thoi (de thu)
#   netflix_gio.py chay     chay mai (systemd goi lenh nay)
#
# Cau hinh: /etc/netflix-gio/cauhinh.json, chi root doc duoc:
#   { "apiKey": "...", "projectId": "...", "maNha": "...", "nguoiDung": "lehoa" }
# Trang thai rieng (token, cac phieu da cong): /var/lib/netflix-gio/trangthai.json.

import base64
import io
import json
import os
import pwd
import re
import signal
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

CAU_HINH = "/etc/netflix-gio/cauhinh.json"
TRANG_THAI = "/var/lib/netflix-gio/trangthai.json"

# Hoi Firestore mot phut mot lan: 1440 luot doc mot ngay, trong goi Spark mien phi
# (50.000 luot). Con bam doi tren tablet thi cho toi da mot phut la laptop co gio.
HOI_PHIEU_GIAY = 60

# Xem Timekpr-nExt bao lau mot lan. Luc con dang xem thi day hon, de thong bao 5 phut
# khong tre qua 15 giay.
NHIP_DANG_DUNG = 15
# Luc khong ai dung thi so phut dung yen, chi doi khi co phieu. Moi lan goi timekpra la bon
# dong trong nhat ky he thong, nen khong hoi day.
NHIP_RANH = 60

BAO_TRUOC_GIAY = 5 * 60

# Moc het gio tren dien thoai lech qua chung nay moi ghi lai. Timekpr-nExt dem theo phien,
# moc tinh lai moi vong lech vai giay vi lam tron; ghi moi lan lech la ghi moi 15 giay.
LECH_GHI_MS = 90_000

LUAT_FIREFOX = "/etc/firefox/policies/policies.json"

# So giay Le Hoa con lai, cho netflix-phien doc luc dang nhap: con 0 thi khong mo Netflix. Truoc
# day dang nhap luc 0 phut van vao duoc khoang 20 giay roi Timekpr-nExt moi day ra, va bam dang
# nhap lien tuc la xem duoc tung doan (anh Huy hoi 7/10/2026). Thu muc trong /run cua root, lehoa
# doc duoc ma khong sua duoc. Dong: "<giay con> <1 neu dang dung> <luc ghi, giay>": dang dung thi
# so giay dang tut, ben doc tu tru di khoang tu luc ghi, nen file cu vai chuc giay van dung.
FILE_CON_LAI = "/run/netflix-gio/conlai"

# --- lenh tu Bang dieu khien (8/10/2026), ten phai giong Duong.LenhLaptop ben ba app ---
LENH_NHAN = "NHAN"
LENH_CHUP = "CHUP"
LENH_DANG_XUAT = "DANGXUAT"
LENH_TAT_MAY = "TATMAY"
LENH_HOI = "HOI"

# Lenh cho qua 5 phut ma laptop chua lam thi bo (anh Huy chot 8/10/2026): khong thi bam "Tat may"
# luc laptop dang tat se lam laptop tu tat ngay lan bat sau, cau "toi gio di hoc" thi doc luc toi.
# Dien thoai cung tu bo lenh cua no sau chung ay, day la lop thu hai khi dien thoai khong mo.
LENH_HET_HAN_MS = 5 * 60_000

# Cau nhan toi da 200 ky tu (anh Huy chot 8/10/2026), luat Firestore chan chu dai hon.
NHAN_TOI_DA = 200

# Khong lay duoc giong doc (mat mang, Google chan) thi dai chu van hien, khong doc, va tat sau
# chung nay giay cho moi lan doc Ba Huy chon (anh Huy chot 10 giay ngay 8/10/2026).
GIAY_KHONG_GIONG = 10

NHAN = "/usr/local/bin/netflix-nhan"
FILE_NHAN = "/run/netflix-gio/nhan.mp3"

# Khoa X cua LightDM. Root dung no ve va chup man hinh :0 ca o man dang nhap lan trong phien cua
# lehoa, huy (thu 8/10/2026), khoi phai doan .Xauthority cua tung nguoi.
XAUTH = "/run/lightdm/root/:0"

# Ten hien o man dang nhap (anh Huy doi 8/10/2026), dung trong cau ket qua gui ve dien thoai.
TEN_HIEN = {"lehoa": "Netflix", "huy": "Admin"}


def ghi_con_lai(giay, dung):
    os.makedirs(os.path.dirname(FILE_CON_LAI), mode=0o755, exist_ok=True)
    tam = FILE_CON_LAI + ".tam"
    with open(tam, "w") as f:
        f.write("%d %d %d\n" % (giay, 1 if dung else 0, int(time.time())))
    os.chmod(tam, 0o644)
    os.replace(tam, FILE_CON_LAI)

# Chan moi trang tru Netflix va cac ten mien chua phim, anh cua no. Kiosk thi khong co thanh
# dia chi, nhung con bam link trong Netflix (Trung tam tro giup, trang ngoai) thi van ra web,
# nen phai chan o day. file:// cung chan: khong thi mo duoc thu muc may bang Ctrl+O.
LUAT = {
    "policies": {
        "WebsiteFilter": {
            "Block": ["<all_urls>"],
            "Exceptions": [
                "https://netflix.com/*", "https://*.netflix.com/*", "https://*.netflix.net/*",
                "https://*.nflxvideo.net/*", "https://*.nflximg.net/*", "https://*.nflximg.com/*",
                "https://*.nflxext.com/*", "https://*.nflxso.net/*",
            ],
        },
        "Homepage": {"URL": "https://www.netflix.com/", "Locked": True, "StartPage": "homepage"},
        "EncryptedMediaExtensions": {"Enabled": True, "Locked": True},
        "DisableDeveloperTools": True,
        "DisablePrivateBrowsing": True,
        "BlockAboutConfig": True,
        "BlockAboutProfiles": True,
        "BlockAboutAddons": True,
        "DisableFirefoxAccounts": True,
        # Tat thanh "Firefox automatically sends some data..." che dau man Netflix.
        "DisableTelemetry": True,
        "OverrideFirstRunPage": "",
        "OverridePostUpdatePage": "",
        "ExtensionSettings": {"*": {"installation_mode": "blocked"}},
        # Phien bi cat ngang (het gio, dang xuat) luc Firefox dang khoi dong thi Firefox dem la mot
        # lan khoi dong loi; du ba lan la no hoi chay che do an toan bang mot hop nho, trong kiosk
        # thi thanh man den hay Firefox thoat ngay (thay ngay 7/10/2026, recent_crashes len 15).
        "DisableSafeMode": True,
        "Preferences": {
            "toolkit.startup.max_resumed_crashes": {"Value": -1, "Status": "locked"},
            "browser.sessionstore.resume_from_crash": {"Value": False, "Status": "locked"},
        },
        "DownloadDirectory": "/tmp",
        "PromptForDownloadLocation": False,
    }
}


def dat_luat_firefox(khoa):
    """Dat (khoa=True) hay go luat chan web. Tra ve True neu vua doi."""
    co = os.path.exists(LUAT_FIREFOX)
    if khoa:
        chu = json.dumps(LUAT, indent=1)
        try:
            with open(LUAT_FIREFOX) as f:
                if f.read() == chu:
                    return False
        except OSError:
            pass
        os.makedirs(os.path.dirname(LUAT_FIREFOX), exist_ok=True)
        tam = LUAT_FIREFOX + ".tam"
        with open(tam, "w") as f:
            f.write(chu)
        os.chmod(tam, 0o644)
        os.replace(tam, LUAT_FIREFOX)
        return True
    if co:
        os.remove(LUAT_FIREFOX)
        return True
    return False


def ghi_log(*chu):
    print(time.strftime("%Y-%m-%d %H:%M:%S"), *chu, flush=True)


def doc_json(duong, mac_dinh):
    try:
        with open(duong) as f:
            return json.load(f)
    except (OSError, ValueError):
        return mac_dinh


def ghi_json(duong, du_lieu):
    os.makedirs(os.path.dirname(duong), exist_ok=True)
    tam = duong + ".tam"
    with open(tam, "w") as f:
        json.dump(du_lieu, f)
    os.chmod(tam, 0o600)
    os.replace(tam, duong)


def goi(phuong_thuc, url, than=None, token=None):
    """Goi HTTP, tra ve (ma, json). Loi mang thi ma la 0."""
    du_lieu = json.dumps(than).encode() if than is not None else None
    yc = urllib.request.Request(url, data=du_lieu, method=phuong_thuc)
    yc.add_header("Content-Type", "application/json")
    if token:
        yc.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(yc, timeout=20) as tl:
            chu = tl.read().decode()
            return tl.status, (json.loads(chu) if chu else {})
    except urllib.error.HTTPError as e:
        chu = e.read().decode(errors="replace")
        try:
            return e.code, json.loads(chu)
        except ValueError:
            return e.code, {"loi": chu[:300]}
    except (urllib.error.URLError, OSError) as e:
        return 0, {"loi": str(e)}


class Firebase:
    def __init__(self, cau_hinh, tt):
        self.key = cau_hinh["apiKey"]
        self.du_an = cau_hinh["projectId"]
        self.ma_nha = cau_hinh["maNha"]
        self.tt = tt
        self.id_token = None
        self.het_han = 0

    def dang_ky(self):
        if self.tt.get("refreshToken"):
            return self.tt["uid"]
        ma, d = goi("POST",
                    "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + self.key,
                    {"returnSecureToken": True})
        if ma != 200:
            raise RuntimeError("dang ky an danh hong: %s %s" % (ma, d))
        self.tt["uid"] = d["localId"]
        self.tt["refreshToken"] = d["refreshToken"]
        ghi_json(TRANG_THAI, self.tt)
        self.id_token = d["idToken"]
        self.het_han = time.time() + int(d.get("expiresIn", "3600")) - 120
        return self.tt["uid"]

    def token(self):
        if self.id_token and time.time() < self.het_han:
            return self.id_token
        if not self.tt.get("refreshToken"):
            raise RuntimeError("chua dang ky, chay: netflix_gio.py dangky")
        # securetoken nhan form, khong nhan JSON.
        than = ("grant_type=refresh_token&refresh_token=" + self.tt["refreshToken"]).encode()
        yc = urllib.request.Request(
            "https://securetoken.googleapis.com/v1/token?key=" + self.key, data=than, method="POST")
        yc.add_header("Content-Type", "application/x-www-form-urlencoded")
        try:
            with urllib.request.urlopen(yc, timeout=20) as tl:
                d = json.loads(tl.read().decode())
        except (urllib.error.URLError, OSError, ValueError) as e:
            raise RuntimeError("lam moi token hong: %s" % e)
        self.id_token = d["id_token"]
        self.het_han = time.time() + int(d.get("expires_in", "3600")) - 120
        if d.get("refresh_token") and d["refresh_token"] != self.tt["refreshToken"]:
            self.tt["refreshToken"] = d["refresh_token"]
            ghi_json(TRANG_THAI, self.tt)
        return self.id_token

    def goc(self):
        return ("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/laptop/%s"
                % (self.du_an, self.ma_nha))

    def doc_laptop(self):
        ma, d = goi("GET", self.goc(), token=self.token())
        if ma != 200:
            raise RuntimeError("doc document laptop hong: %s %s" % (ma, d))
        return d.get("fields", {})

    def cac_phieu(self):
        ma, d = goi("GET", self.goc() + "/cap?pageSize=50", token=self.token())
        if ma != 200:
            raise RuntimeError("doc phieu hong: %s %s" % (ma, d))
        return d.get("documents", [])

    def cac_lenh(self):
        ma, d = goi("GET", self.goc() + "/lenh?pageSize=20", token=self.token())
        if ma != 200:
            raise RuntimeError("doc lenh hong: %s %s" % (ma, d))
        return d.get("documents", [])

    def xoa(self, ten):
        ma, d = goi("DELETE", "https://firestore.googleapis.com/v1/" + ten, token=self.token())
        if ma != 200:
            raise RuntimeError("xoa phieu hong: %s %s" % (ma, d))

    def ghi_anh(self, jpg, luc, phien):
        # Ghi de ca document (khong updateMask): chi giu anh moi nhat, anh Huy chot 8/10/2026.
        ma, d = goi("PATCH", self.goc() + "/anh/moinhat",
                    {"fields": {"jpg": gia_tri(jpg), "luc": gia_tri(luc), "phien": gia_tri(phien)}},
                    token=self.token())
        if ma != 200:
            raise RuntimeError("ghi anh hong: %s %s" % (ma, d))

    def ghi_trang_thai(self, truong):
        # updateMask chi ghi dung cac truong nay. Thieu no thi PATCH ghi de ca document,
        # mat uidLaptop, va luat Firestore chan.
        mask = "&".join("updateMask.fieldPaths=" + k for k in truong)
        ma, d = goi("PATCH", self.goc() + "?" + mask, {"fields": {k: gia_tri(v) for k, v in truong.items()}},
                    token=self.token())
        if ma != 200:
            raise RuntimeError("ghi trang thai hong: %s %s" % (ma, d))


def gia_tri(v):
    if isinstance(v, bool):
        return {"booleanValue": v}
    if isinstance(v, int):
        return {"integerValue": str(v)}
    if isinstance(v, dict):
        return {"mapValue": {"fields": {k: gia_tri(x) for k, x in v.items()}}}
    if isinstance(v, list):
        # Mang rong phai la arrayValue rong, khong co khoa values.
        return {"arrayValue": {"values": [gia_tri(x) for x in v]} if v else {}}
    if isinstance(v, bytes):
        return {"bytesValue": base64.b64encode(v).decode()}
    return {"stringValue": str(v)}


def so(truong, ten):
    o = truong.get(ten, {})
    if "integerValue" in o:
        return int(o["integerValue"])
    if "doubleValue" in o:
        return int(o["doubleValue"])
    return 0


def chu(truong, ten):
    return truong.get(ten, {}).get("stringValue", "")


# ----------------------------------------------------------------- Timekpr-nExt

def timekpr_con_lai(nguoi, dung):
    """So giay con lai trong ngay theo Timekpr-nExt, None neu khong doc duoc."""
    # getuserinfort la so dang chay, chi co khi nguoi do dang dang nhap; khong thi rong,
    # va getuserinfo la so Timekpr-nExt luu gan nhat.
    for lenh in (("--getuserinfort", "--getuserinfo") if dung else ("--getuserinfo",)):
        r = subprocess.run(["timekpra", lenh, nguoi], capture_output=True, text=True)
        for dong in r.stdout.splitlines():
            # getuserinfort ghi "ACTUAL_TIME_LEFT_DAY", getuserinfo ghi "TIME_LEFT_DAY".
            if dong.startswith("TIME_LEFT_DAY:") or dong.startswith("ACTUAL_TIME_LEFT_DAY:"):
                try:
                    return int(dong.split(":", 1)[1].strip())
                except ValueError:
                    pass
    return None


def cong_gio(nguoi, giay):
    r = subprocess.run(["timekpra", "--settimeleft", nguoi, "+", str(giay)],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError("timekpra cong gio hong: %s %s" % (r.stdout, r.stderr))


def bot_gio(nguoi, giay):
    """Bot [giay] giay Netflix (phieu phut am, Ba Huy bam "Bớt Netflix", 9/10/2026). Bot qua so
    dang con thi ve 0, khong am (anh Huy chot). Tra ve so giay da bot that.

    Con dang xem ma ve 0 thi Timekpr-nExt dang xuat ngay nhu luc het gio; anh Huy chot khong bao
    them tren tivi, con duoi 5 phut thi van co thong bao "Còn N phút" cua vong.
    """
    con = timekpr_con_lai(nguoi, dang_dung(nguoi))
    if con is None:
        # Timekpr-nExt chua chay (vua bat may): nem loi de phieu nam cho, vong sau lam lai.
        raise RuntimeError("chua doc duoc Timekpr-nExt, chua bot")
    tru = min(giay, max(con, 0))
    if tru <= 0:
        return 0
    r = subprocess.run(["timekpra", "--settimeleft", nguoi, "-", str(tru)],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError("timekpra bot gio hong: %s %s" % (r.stdout, r.stderr))
    return tru


def dang_dung(nguoi):
    """Le Hoa co dang dang nhap man hinh khong."""
    r = subprocess.run(["loginctl", "list-sessions", "--no-legend"], capture_output=True, text=True)
    for dong in r.stdout.splitlines():
        cot = dong.split()
        # SESSION UID USER SEAT TTY STATE ...: chi tinh phien co seat (man hinh that).
        if len(cot) >= 4 and cot[2] == nguoi and cot[3].startswith("seat"):
            return True
    return False


def dong_ho_da_dong_bo():
    """Dong ho may da lay gio qua mang chua.

    Laptop mat gio moi lan tat han may (thay 7/10/2026: luc khoi dong dong ho la 26/11/2025,
    systemd-timesyncd dat lai thanh gio luu lan truoc roi moi lay gio mang). Tat tu toi qua thi trong
    khoang do may tuong van la hom qua, xu_ly_phieu coi phieu sang nay la phieu cu va xoa mat.
    """
    if os.path.exists("/run/systemd/timesync/synchronized"):
        return True
    try:
        r = subprocess.run(["timedatectl", "show", "-p", "NTPSynchronized", "--value"],
                           capture_output=True, text=True, timeout=10)
    except (OSError, subprocess.TimeoutExpired):
        return False
    return r.stdout.strip() == "yes"


def bao(nguoi, tieu_de, noi_dung):
    """Hien thong bao trong phien cua [nguoi]. Hong thi bo qua, Timekpr-nExt van bao luc cuoi."""
    try:
        uid = int(subprocess.run(["id", "-u", nguoi], capture_output=True, text=True).stdout.strip())
        subprocess.run(
            ["runuser", "-u", nguoi, "--", "env",
             # Thieu XAUTHORITY thi notify-send van thoat ma 0 ma khong hien gi (thu 7/10/2026).
             "DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/%d/bus" % uid, "DISPLAY=:0",
             "XAUTHORITY=/home/%s/.Xauthority" % nguoi,
             # MATE tat thong bao sau khoang 10 giay, ke ca loai critical; giu 30 giay.
             "notify-send", "-u", "critical", "-t", "30000", "-a", "Netflix", tieu_de, noi_dung],
            capture_output=True, timeout=10)
    except (OSError, ValueError, subprocess.TimeoutExpired) as e:
        ghi_log("khong hien duoc thong bao:", e)


# ----------------------------------------------------------------- lenh tu dien thoai

def gio_phut(luc_ms):
    return time.strftime("%H:%M", time.localtime(luc_ms / 1000))


def ten_hien(ten):
    return TEN_HIEN.get(ten, ten or "")


GOOGLE_TTS = "https://translate.google.com/_/TranslateWebserverUi/data/batchexecute"
# Google Dich chi nhan toi da 100 ky tu moi lan goi. Cach goi va cach doc cau tra loi chep tu thu
# vien gTTS 2.5.4 (gtts/tts.py), viet lai bang thu vien chuan vi laptop khong co pip.
TTS_TOI_DA = 100


def cat_cau(chu, toi_da=TTS_TOI_DA):
    """Cat chu thanh doan <= toi_da ky tu: uu tien sau dau cau, roi dau phay, roi khoang trang."""
    chu = " ".join(chu.split())
    doan = []
    while len(chu) > toi_da:
        cat = 0
        for dau in (".!?", ",;:", " "):
            for i in range(toi_da - 1, 0, -1):
                if chu[i] in dau:
                    cat = i + 1
                    break
            if cat:
                break
        cat = cat or toi_da
        doan.append(chu[:cat].strip())
        chu = chu[cat:].strip()
    if chu:
        doan.append(chu)
    return doan


def giong_google(chu):
    """File mp3 giong doc tieng Viet cua Google Dich (anh Huy chon 8/10/2026). Hong thi nem loi.

    Day la cua khong chinh thuc cua Google Dich, khong can khoa, Google co the chan bat cu luc nao.
    Luc do nhan_tivi van hien dai chu, chi khong doc, va bao ve dien thoai (anh Huy chot).
    """
    am = b""
    for doan in cat_cau(chu):
        tham_so = json.dumps([doan, "vi", None, "null"], separators=(",", ":"))
        rpc = json.dumps([[["jQ1olc", tham_so, None, "generic"]]], separators=(",", ":"))
        yc = urllib.request.Request(GOOGLE_TTS, data=("f.req=" + urllib.parse.quote(rpc) + "&").encode(),
                                    method="POST")
        yc.add_header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
        yc.add_header("Referer", "http://translate.google.com/")
        yc.add_header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                                    "(KHTML, like Gecko) Chrome/130.0 Safari/537.36")
        with urllib.request.urlopen(yc, timeout=15) as tl:
            tra = tl.read().decode("utf-8", "replace")
        m = re.search(r'jQ1olc","\[\\"(.*?)\\"]', tra)
        if not m:
            raise RuntimeError("Google khong tra giong doc: " + tra[:120])
        am += base64.b64decode(m.group(1))
    return am


def phien_man_hinh():
    """(ten, ma phien) cua nguoi dang ngoi man hinh; (None, None) khi dang o man dang nhap."""
    r = subprocess.run(["loginctl", "list-sessions", "--no-legend"], capture_output=True, text=True)
    for dong in r.stdout.splitlines():
        # SESSION UID USER SEAT TTY STATE ...: chi tinh phien co seat (man hinh that), bo phien
        # dang dong va phien cua man dang nhap (lightdm).
        cot = dong.split()
        if len(cot) >= 6 and cot[3].startswith("seat") and cot[2] != "lightdm" and cot[5] != "closing":
            return cot[2], cot[0]
    return None, None


def luc_dang_nhap(ma_phien):
    """Luc phien bat dau (ms), 0 neu khong doc duoc."""
    r = subprocess.run(["loginctl", "show-session", ma_phien, "-p", "Timestamp", "--value"],
                       capture_output=True, text=True)
    # "Thu 2026-10-08 16:27:10 +07"
    cot = r.stdout.split()
    try:
        return int(time.mktime(time.strptime(cot[1] + " " + cot[2], "%Y-%m-%d %H:%M:%S")) * 1000)
    except (IndexError, ValueError):
        return 0


def chay_nguoi(ten, lenh, giay=30):
    """Chay lenh bang tai khoan [ten], trong phien am thanh cua nguoi do (PipeWire)."""
    uid = pwd.getpwnam(ten).pw_uid
    return subprocess.run(["runuser", "-u", ten, "--", "env", "XDG_RUNTIME_DIR=/run/user/%d" % uid] + lenh,
                          capture_output=True, text=True, timeout=giay)


def tat_tieng(ten):
    """Tat tieng moi luong dang phat trong phien [ten] (phim), tra ve cac luong da tat.

    Anh Huy chot 8/10/2026: doc thi tat han tieng phim, doc xong tra lai nhu cu. Luong dang tat
    san thi de nguyen, luc tra khong bat no len.
    """
    r = chay_nguoi(ten, ["pactl", "-f", "json", "list", "sink-inputs"])
    try:
        ds = json.loads(r.stdout or "[]")
    except ValueError:
        return []
    da_tat = []
    for luong in ds:
        if not luong.get("mute"):
            chay_nguoi(ten, ["pactl", "set-sink-input-mute", str(luong["index"]), "1"])
            da_tat.append(luong["index"])
    return da_tat


def tra_tieng(ten, da_tat):
    for i in da_tat:
        chay_nguoi(ten, ["pactl", "set-sink-input-mute", str(i), "0"])


def loa_hdmi():
    """Thiet bi ALSA cua cong HDMI dang cam tivi, vd "hdmi:CARD=HDMI,DEV=0"; None neu khong co.

    Doc /proc/asound/card*/eld#<codec>.<chan>: monitor_present 1 la chan do dang noi man hinh co loa
    (9/10/2026 la "SONY TV" o card 1 "HDMI", eld#0.0). Chan thu N cua codec la DEV=N.
    """
    import glob
    for eld in sorted(glob.glob("/proc/asound/card*/eld#*")):
        try:
            with open(eld) as f:
                if "monitor_present\t\t1" not in f.read().replace(" ", "\t"):
                    continue
            with open(os.path.join(os.path.dirname(eld), "id")) as f:
                the = f.read().strip()
        except OSError:
            continue
        return "hdmi:CARD=%s,DEV=%s" % (the, eld.rsplit(".", 1)[1])
    return None


def phat_giong(ten):
    """Phat FILE_NHAN mot lan. Co nguoi dang nhap thi phat trong phien do, khong thi phat bang root
    thang ra ALSA (o man dang nhap khong co PipeWire cua ai). Tra ve True neu phat xong.

    Trong phien, tieng ra loa mac dinh cua phien. Ngay 9/10/2026 phien lehoa chua tung chon loa nen
    WirePlumber chon loa laptop (uu tien cao hon HDMI), Netflix lan giong doc deu khong ra tivi;
    phien chi-tivi da dat lai loa mac dinh cua lehoa la HDMI. O man dang nhap thi alsasink khong ghi
    thiet bi se ra card 0 (loa laptop), nen chi thang cong HDMI dang cam tivi.
    """
    lenh = ["gst-play-1.0", "--no-interactive", "-q", FILE_NHAN]
    if ten:
        r = chay_nguoi(ten, lenh, giay=120)
    else:
        hdmi = loa_hdmi()
        loa = '--audiosink=alsasink device="%s"' % hdmi if hdmi else "--audiosink=alsasink"
        r = subprocess.run(lenh[:1] + [loa] + lenh[1:], capture_output=True, text=True, timeout=120)
    return r.returncode == 0


def nhan_tivi(chu, so_lan, nhan_ai, ten):
    """Hien dai chu va doc [so_lan] lan. Tra ve (ok, cau ket qua cho dien thoai)."""
    try:
        am = giong_google(chu)
        loi_giong = None
    except Exception as e:  # mat mang, Google chan, Google doi cach tra
        am = None
        loi_giong = e
    if am:
        os.makedirs(os.path.dirname(FILE_NHAN), mode=0o755, exist_ok=True)
        with open(FILE_NHAN, "wb") as f:
            f.write(am)
        os.chmod(FILE_NHAN, 0o644)
    env = dict(os.environ, DISPLAY=":0", XAUTHORITY=XAUTH)
    lenh = [NHAN, "--chu", chu, "--ai", nhan_ai]
    if am is None:
        lenh += ["--giay", str(GIAY_KHONG_GIONG * so_lan)]
    dai = subprocess.Popen(lenh, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    luc = gio_phut(time.time() * 1000)
    try:
        if am is None:
            ghi_log("khong lay duoc giong doc:", loi_giong)
            dai.wait(timeout=GIAY_KHONG_GIONG * so_lan + 30)
            return False, ("Đã hiện trên tivi lúc %s nhưng không đọc được: laptop không lấy được "
                           "giọng đọc (mất mạng hoặc Google chặn)." % luc)
        # Cho dai chu hien roi moi doc, de tieng khong toi truoc chu.
        time.sleep(1)
        da_tat = tat_tieng(ten) if ten else []
        doc_duoc = 0
        try:
            for lan in range(so_lan):
                if lan:
                    time.sleep(1)
                if not phat_giong(ten):
                    break
                doc_duoc += 1
        finally:
            tra_tieng(ten, da_tat)
    finally:
        if dai.poll() is None:
            dai.terminate()
            try:
                dai.wait(timeout=5)
            except subprocess.TimeoutExpired:
                dai.kill()
    if doc_duoc == 0:
        return False, "Đã hiện trên tivi lúc %s nhưng laptop không phát được tiếng." % luc
    return True, "Đã đọc %d lần trên tivi lúc %s." % (doc_duoc, luc)


def chup_man_hinh():
    """Anh JPEG man hinh :0, nho hon 700 KB (document Firestore toi da 1 MB, base64 lam to them)."""
    os.environ["XAUTHORITY"] = XAUTH
    from PIL import ImageGrab  # Pillow co san tren Mint, chi lenh nay can
    anh = ImageGrab.grab(xdisplay=":0").convert("RGB")
    anh.thumbnail((1280, 1280))
    for chat in (70, 55, 40):
        bo = io.BytesIO()
        anh.save(bo, "JPEG", quality=chat)
        if bo.tell() < 700_000:
            break
    return bo.getvalue()


def he_thong_dang_tat():
    r = subprocess.run(["systemctl", "is-system-running"], capture_output=True, text=True)
    return r.stdout.strip() == "stopping"


# ----------------------------------------------------------------- mot vong

class May:
    def __init__(self):
        self.cau_hinh = doc_json(CAU_HINH, None)
        if not self.cau_hinh:
            sys.exit("thieu " + CAU_HINH)
        self.nguoi = self.cau_hinh.get("nguoiDung", "lehoa")
        self.tt = doc_json(TRANG_THAI, {})
        self.tt.setdefault("daNhan", {})
        self.fb = Firebase(self.cau_hinh, self.tt)
        self.lan_hoi = 0
        self.da_day = None
        self.cap_cuoi = None
        self.da_bao = False
        # Ba Huy co bam "Mở web" khong. Mac dinh khoa: chua doc duoc Firestore thi khoa.
        self.mo_web = False
        self.web_dang_mo = None
        # Da lay gio mang lan nao trong lan chay nay chua; roi thi thoi hoi.
        self.gio_dung = False
        # Ket qua nam lenh gan nhat (truong ketQua, moi muc { id, kieu, ok, chu, luc }), dien
        # thoai tim theo id lenh no gui. Giu nam muc vi mot vong co the lam nhieu lenh.
        self.ket_qua = []
        # Phai ghi trang thai ngay ca khi so phut khong doi: lenh HOI, su kien moi.
        self.can_day = False
        # Nguoi dang ngoi man hinh lan xet truoc, de ghi su kien vao, ra.
        self.phien = None
        self.phien_tu = 0
        self.da_ghi_khoi_dong = False
        self.lan_ghi_song = 0

    # --------------------------------------------------- bat, tat, dang nhap trong ngay

    def them_su_kien(self, kieu, luc, ai=""):
        """Them mot dong vao suKien hom nay: bat, tat, mat (tat dot ngot), vao, ra (8/10/2026)."""
        hom_nay = time.strftime("%Y-%m-%d")
        sk = self.tt.get("suKien") or {}
        if sk.get("ngay") != hom_nay:
            sk = {"ngay": hom_nay, "ds": []}
        moi = {"kieu": kieu, "luc": int(luc), "ai": ai or ""}
        # Dich vu chay lai giua phien thi gap lai dung lan dang nhap do: khong ghi hai lan.
        if time.strftime("%Y-%m-%d", time.localtime(luc / 1000)) == hom_nay and moi not in sk["ds"]:
            sk["ds"].append(moi)
            sk["ds"].sort(key=lambda x: x["luc"])
        self.tt["suKien"] = sk
        ghi_json(TRANG_THAI, self.tt)
        self.can_day = True

    def su_kien_hom_nay(self):
        sk = self.tt.get("suKien") or {}
        return sk.get("ds", []) if sk.get("ngay") == time.strftime("%Y-%m-%d") else []

    def ghi_khoi_dong(self):
        """Ghi lan bat may nay, mot lan, sau khi dong ho da lay gio mang (truoc do gio sai)."""
        self.da_ghi_khoi_dong = True
        with open("/proc/sys/kernel/random/boot_id") as f:
            ma = f.read().strip()
        if self.tt.get("bootId") == ma:
            return  # dich vu chay lai giua chung, khong phai may vua bat
        with open("/proc/uptime") as f:
            bat = int((time.time() - float(f.read().split()[0])) * 1000)
        # Lan bat truoc khong ghi duoc luc tat (mat dien, giu nut nguon, het pin): ghi luc cuoi
        # cung con thay may chay.
        if self.tt.get("bootId") and not self.tt.get("daGhiTat") and self.tt.get("songLuc"):
            self.them_su_kien("mat", self.tt["songLuc"])
        self.tt.update(bootId=ma, batLuc=bat, daGhiTat=False)
        self.them_su_kien("bat", bat)
        ghi_log("may bat luc", gio_phut(bat))

    def ghi_tat(self):
        """Ghi luc tat may va day len ngay, truoc khi mat mang. Mot lan moi lan bat may."""
        if self.tt.get("daGhiTat"):
            return
        bay_gio = int(time.time() * 1000)
        self.them_su_kien("tat", bay_gio)
        self.tt["daGhiTat"] = True
        ghi_json(TRANG_THAI, self.tt)
        try:
            truong = {"tatLuc": bay_gio, "suKien": self.su_kien_hom_nay(), "capNhatLuc": bay_gio,
                      "ketQua": self.ket_qua}
            self.fb.ghi_trang_thai(truong)
        except Exception as e:
            ghi_log("khong ghi duoc luc tat:", e)

    def xet_phien(self):
        """Ghi su kien khi nguoi ngoi man hinh doi (dang nhap, dang xuat, doi tai khoan)."""
        ten, ma = phien_man_hinh()
        if self.phien is not None and ten == self.phien[0]:
            return ten
        bay_gio = int(time.time() * 1000)
        cu = self.phien[0] if self.phien else None
        self.phien = (ten, ma)
        if cu:
            self.them_su_kien("ra", bay_gio, cu)
        if ten:
            tu = luc_dang_nhap(ma)
            # Luc dang nhap ghi theo dong ho luc do; vua bat may thi co the con sai gio.
            if not tu or tu > bay_gio or tu < self.tt.get("batLuc", 0):
                tu = bay_gio
            self.phien_tu = tu
            self.them_su_kien("vao", tu, ten)
        else:
            self.phien_tu = 0
        return ten

    # --------------------------------------------------- lenh tu dien thoai

    def xu_ly_lenh(self):
        ds = self.fb.cac_lenh()
        ds.sort(key=lambda d: so(d.get("fields", {}), "taoLuc"))
        for d in ds:
            ten = d["name"]
            ma = ten.rsplit("/", 1)[1]
            f = d.get("fields", {})
            kieu = chu(f, "kieu")
            tao = so(f, "taoLuc")
            # Xoa truoc roi moi lam: lam truoc ma xoa hong thi lenh tat may lam laptop tat lai o
            # lan bat sau.
            self.fb.xoa(ten)
            bay_gio = int(time.time() * 1000)
            if kieu != LENH_HOI and bay_gio - tao > LENH_HET_HAN_MS:
                ok, cau = False, "Laptop chưa nhận lệnh trong 5 phút, đã bỏ."
                ghi_log("bo lenh %s %s tao luc %s" % (kieu, ma, gio_phut(tao)))
            else:
                try:
                    ok, cau = self.lam_lenh(kieu, f)
                except Exception as e:  # lenh hong thi bao ve dien thoai, khong lam ket ca vong
                    ok, cau = False, "Laptop làm lệnh bị lỗi: %s" % e
                ghi_log("lenh %s %s: %s" % (kieu, ma, cau))
            self.ket_qua = (self.ket_qua + [{"id": ma, "kieu": kieu, "ok": ok, "chu": cau,
                                             "luc": int(time.time() * 1000)}])[-5:]
            self.can_day = True
            if kieu == LENH_TAT_MAY and ok:
                self.ghi_tat()
                subprocess.run(["systemctl", "poweroff"])
                return

    def lam_lenh(self, kieu, f):
        ten = self.phien[0] if self.phien else None
        bay_gio = int(time.time() * 1000)
        if kieu == LENH_HOI:
            return True, ""
        if kieu == LENH_NHAN:
            cau = " ".join(chu(f, "chu").split())[:NHAN_TOI_DA]
            if not cau:
                return False, "Câu nhắn trống."
            so_lan = min(max(so(f, "soLan"), 1), 3)
            nhan_ai = "Ba Huy nhắn · " + gio_phut(so(f, "taoLuc") or bay_gio)
            return nhan_tivi(cau, so_lan, nhan_ai, ten)
        if kieu == LENH_CHUP:
            jpg = chup_man_hinh()
            self.fb.ghi_anh(jpg, bay_gio, ten or "")
            return True, "Đã chụp màn hình lúc %s." % gio_phut(bay_gio)
        if kieu == LENH_DANG_XUAT:
            if not ten:
                return False, "Chưa ai đăng nhập laptop."
            r = subprocess.run(["loginctl", "terminate-session", self.phien[1]], capture_output=True, text=True)
            if r.returncode != 0:
                return False, "Không đăng xuất được: " + (r.stderr.strip() or "lỗi lạ")
            return True, "Đã đăng xuất tài khoản %s lúc %s." % (ten_hien(ten), gio_phut(bay_gio))
        if kieu == LENH_TAT_MAY:
            return True, "Laptop tắt máy lúc %s." % gio_phut(bay_gio)
        return False, "Laptop chưa biết lệnh %s, cần cài bản mới lên laptop." % kieu

    def xu_ly_phieu(self):
        hom_nay = time.strftime("%Y-%m-%d")
        for d in self.fb.cac_phieu():
            ten = d["name"]
            ma = ten.rsplit("/", 1)[1]
            f = d.get("fields", {})
            phut = so(f, "phut")
            tao = so(f, "taoLuc")
            ai = chu(f, "ai")
            ngay = time.strftime("%Y-%m-%d", time.localtime(tao / 1000)) if tao else ""
            if ma in self.tt["daNhan"]:
                # Da cong roi, lan truoc xoa hong. Xoa lai, khong cong lan hai.
                pass
            elif ngay != hom_nay:
                # Phut Netflix chi dung trong ngay, nhu phieu gio choi tablet (anh Huy chot
                # 7/10/2026): laptop tat tu toi qua thi phieu toi qua bo.
                ghi_log("bo phieu %s cua ngay %s (%d phut, %s)" % (ma, ngay, phut, ai))
            elif 0 < phut <= 600:
                cong_gio(self.nguoi, phut * 60)
                self.tt["daNhan"][ma] = hom_nay
                ghi_json(TRANG_THAI, self.tt)
                self.cap_cuoi = {"phut": phut, "ai": ai, "luc": int(time.time() * 1000)}
                self.da_bao = False
                ghi_log("cong %d phut Netflix (%s, phieu %s)" % (phut, ai, ma))
            elif -600 <= phut < 0:
                # Phieu bot (9/10/2026): cung duong, cung han trong ngay voi phieu cap (anh Huy chot
                # "cap them thi cung phai bot").
                tru = bot_gio(self.nguoi, -phut * 60)
                self.tt["daNhan"][ma] = hom_nay
                ghi_json(TRANG_THAI, self.tt)
                self.cap_cuoi = {"phut": phut, "ai": ai, "luc": int(time.time() * 1000)}
                ghi_log("bot %d phut Netflix, bot that %d giay (%s, phieu %s)" % (-phut, tru, ai, ma))
            self.fb.xoa(ten)
        # Bo cac ma phieu cu, chi can nho trong ngay.
        cu = [k for k, v in self.tt["daNhan"].items() if v != hom_nay]
        if cu:
            for k in cu:
                del self.tt["daNhan"][k]
            ghi_json(TRANG_THAI, self.tt)

    def day_trang_thai(self, con_lai, dung):
        bay_gio = int(time.time() * 1000)
        ket_thuc = bay_gio + con_lai * 1000 if dung and con_lai > 0 else 0
        ban = {"ketThucLuc": ket_thuc, "conLaiMs": con_lai * 1000, "dangDung": dung,
               "webDangMo": bool(self.web_dang_mo)}
        cu = self.da_day
        can = (cu is None or cu["dangDung"] != dung or self.cap_cuoi is not None
               or self.can_day
               or cu["webDangMo"] != ban["webDangMo"]
               or abs(cu["ketThucLuc"] - ket_thuc) > LECH_GHI_MS
               or (not dung and abs(cu["conLaiMs"] - ban["conLaiMs"]) >= 60_000))
        if not can:
            return
        # Tu 8/10/2026 kem ai dang ngoi man hinh, luc bat may, cac su kien trong ngay; tatLuc ve 0
        # vi may dang chay (dien thoai so tatLuc voi batLuc de biet laptop tat chua).
        truong = dict(ban, capNhatLuc=bay_gio, phien=(self.phien[0] if self.phien else "") or "",
                      phienTu=self.phien_tu, batLuc=self.tt.get("batLuc", 0), tatLuc=0,
                      suKien=self.su_kien_hom_nay())
        if self.cap_cuoi is not None:
            truong["capCuoi"] = self.cap_cuoi
        if self.ket_qua:
            truong["ketQua"] = self.ket_qua
        self.fb.ghi_trang_thai(truong)
        self.da_day = ban
        self.can_day = False
        self.cap_cuoi = None

    def vong(self):
        dung = dang_dung(self.nguoi)
        # Le Hoa dang nhap thi khoa web truoc tien, truoc ca khi hoi Firestore: mat mang thi
        # cac buoc sau nem loi, ma luat thi phai co mat.
        if dung and dat_luat_firefox(True):
            ghi_log("Le Hoa dang nhap, khoa web")
        loi_mang = None
        if not self.gio_dung:
            self.gio_dung = dong_ho_da_dong_bo()
        if self.gio_dung:
            if not self.da_ghi_khoi_dong:
                self.ghi_khoi_dong()
            self.xet_phien()
            # Luc cuoi con thay may chay, de lan bat sau biet may tat dot ngot luc nao. Ghi dia moi
            # phut la du.
            if time.time() - self.lan_ghi_song >= 60:
                self.lan_ghi_song = time.time()
                self.tt["songLuc"] = int(time.time() * 1000)
                ghi_json(TRANG_THAI, self.tt)
        if time.time() - self.lan_hoi >= HOI_PHIEU_GIAY:
            self.lan_hoi = time.time()
            try:
                self.mo_web = self.fb.doc_laptop().get("moWeb", {}).get("booleanValue", False)
                if self.gio_dung:
                    self.xu_ly_phieu()
                    self.xu_ly_lenh()
                else:
                    ghi_log("dong ho chua lay gio mang, chua nhan phieu")
            except Exception as e:
                # Van di tiep de dat luat Firefox theo lan doc truoc; bao loi sau cung.
                loi_mang = e
        mo = self.mo_web and not dung
        if dat_luat_firefox(not mo):
            ghi_log("mo web" if mo else "khoa web")
        self.web_dang_mo = mo
        if loi_mang is not None:
            raise loi_mang
        con_lai = timekpr_con_lai(self.nguoi, dung)
        if con_lai is None:
            ghi_log("khong doc duoc Timekpr-nExt")
            # Vua khoi dong may thi Timekpr-nExt thuong chua chay. Chua co FILE_CON_LAI thi
            # netflix-phien coi la het phut, nen thu lai sau NHIP_DANG_DUNG chu khong cho mot phut.
            return True
        ghi_con_lai(con_lai, dung)
        if dung and 60 < con_lai <= BAO_TRUOC_GIAY and not self.da_bao:
            self.da_bao = True
            bao(self.nguoi, "Còn %d phút xem Netflix" % ((con_lai + 59) // 60),
                "Hết giờ laptop tự đăng xuất. Muốn xem thêm thì đổi phút chơi trên tablet.")
        if con_lai > BAO_TRUOC_GIAY + 30:
            self.da_bao = False
        # Gio sai thi moc het gio va capNhatLuc ghi len dien thoai cung sai.
        if self.gio_dung:
            self.day_trang_thai(con_lai, dung)
        # Web dang mo thi xem day, de Le Hoa dang nhap la khoa ngay.
        return dung or mo


def main():
    lenh = sys.argv[1] if len(sys.argv) > 1 else "chay"
    may = May()
    if lenh == "dangky":
        print(may.fb.dang_ky())
        return
    if lenh == "mot":
        may.vong()
        return

    # systemd gui SIGTERM khi dung dich vu. May dang tat thi ghi luc tat len dien thoai truoc.
    def khi_dung(so_tin_hieu, khung):
        if he_thong_dang_tat():
            may.ghi_tat()
        sys.exit(0)

    signal.signal(signal.SIGTERM, khi_dung)
    while True:
        dung = False
        try:
            dung = may.vong()
        except Exception as e:  # mat mang, token hong, luat chan: ghi lai roi thu vong sau
            ghi_log("loi:", e)
        time.sleep(NHIP_DANG_DUNG if dung else NHIP_RANH)


if __name__ == "__main__":
    main()
