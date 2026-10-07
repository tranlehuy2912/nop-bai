#!/usr/bin/env python3
# Chuong trinh chay ngam tren laptop Linux Mint cua Le Hoa (7/10/2026).
#
# Viec cua no:
#  1. Moi phut hoi Firestore xem co phieu cap gio Netflix moi khong (laptop/{maNha}/cap,
#     xem Duong.LAPTOP, Duong.CAP ben ba app). Phieu do tablet ghi khi Le Hoa doi phut choi,
#     hay Bang dieu khien ghi khi Ba Huy cho them.
#  2. Cong gio cho tai khoan Le Hoa bang Timekpr-nExt (timekpra --settimeleft ... +), roi xoa
#     phieu. Timekpr-nExt dem gio, bao truoc, va dang xuat khi het gio; o day khong dem gi.
#  3. Ghi lai vao document laptop/{maNha} luc nao het gio (khi con dang dung) hay con bao
#     nhieu (khi khong ai dung), de Bang dieu khien hien "Laptop còn N phút". Chi ghi khi co
#     gi doi, khong ghi moi phut: giu so luot ghi Firestore o muc vai chuc mot ngay.
#  4. Con 5 phut thi hien thong bao trong phien cua Le Hoa (anh Huy chon bao truoc 5 phut).
#  5. Giu luat chan web cua Firefox (/etc/firefox/policies/policies.json): Firefox chi vao
#     Netflix, cho ca may vi Firefox khong co luat rieng tung tai khoan (anh Huy chon khoa ca
#     may). Ba Huy bam "Mở web" tren Bang dieu khien (truong moWeb) thi go luat ra, bam "Khoá
#     web" thi dat lai; khong co han (anh Huy chon). Le Hoa dang nhap thi luat luon co mat,
#     du moWeb dang bat. Firefox chi doc luat luc khoi dong: doi xong phai mo lai Firefox.
#
# Chi dung thu vien chuan cua Python 3 (Mint 22 co san 3.12), khong cai them goi pip nao.
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

import json
import os
import subprocess
import sys
import time
import urllib.error
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

    def xoa(self, ten):
        ma, d = goi("DELETE", "https://firestore.googleapis.com/v1/" + ten, token=self.token())
        if ma != 200:
            raise RuntimeError("xoa phieu hong: %s %s" % (ma, d))

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


def dang_dung(nguoi):
    """Le Hoa co dang dang nhap man hinh khong."""
    r = subprocess.run(["loginctl", "list-sessions", "--no-legend"], capture_output=True, text=True)
    for dong in r.stdout.splitlines():
        cot = dong.split()
        # SESSION UID USER SEAT TTY STATE ...: chi tinh phien co seat (man hinh that).
        if len(cot) >= 4 and cot[2] == nguoi and cot[3].startswith("seat"):
            return True
    return False


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
               or cu["webDangMo"] != ban["webDangMo"]
               or abs(cu["ketThucLuc"] - ket_thuc) > LECH_GHI_MS
               or (not dung and abs(cu["conLaiMs"] - ban["conLaiMs"]) >= 60_000))
        if not can:
            return
        truong = dict(ban, capNhatLuc=bay_gio)
        if self.cap_cuoi is not None:
            truong["capCuoi"] = self.cap_cuoi
        self.fb.ghi_trang_thai(truong)
        self.da_day = ban
        self.cap_cuoi = None

    def vong(self):
        dung = dang_dung(self.nguoi)
        # Le Hoa dang nhap thi khoa web truoc tien, truoc ca khi hoi Firestore: mat mang thi
        # cac buoc sau nem loi, ma luat thi phai co mat.
        if dung and dat_luat_firefox(True):
            ghi_log("Le Hoa dang nhap, khoa web")
        loi_mang = None
        if time.time() - self.lan_hoi >= HOI_PHIEU_GIAY:
            self.lan_hoi = time.time()
            try:
                self.mo_web = self.fb.doc_laptop().get("moWeb", {}).get("booleanValue", False)
                self.xu_ly_phieu()
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
            return dung
        ghi_con_lai(con_lai, dung)
        if dung and 60 < con_lai <= BAO_TRUOC_GIAY and not self.da_bao:
            self.da_bao = True
            bao(self.nguoi, "Còn %d phút xem Netflix" % ((con_lai + 59) // 60),
                "Hết giờ laptop tự đăng xuất. Muốn xem thêm thì đổi phút chơi trên tablet.")
        if con_lai > BAO_TRUOC_GIAY + 30:
            self.da_bao = False
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
    while True:
        dung = False
        try:
            dung = may.vong()
        except Exception as e:  # mat mang, token hong, luat chan: ghi lai roi thu vong sau
            ghi_log("loi:", e)
        time.sleep(NHIP_DANG_DUNG if dung else NHIP_RANH)


if __name__ == "__main__":
    main()
