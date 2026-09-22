#!/usr/bin/env python3
"""Ban thu Homework Gate bang chuot thay vi go lenh.

    tools/emu.sh web        # roi mo http://127.0.0.1:8765

Trang nay khong tu tinh gi ca. Moi con so no ve deu lay tu ManualLich / ManualBang
chay tren may ao - tuc la tu ThoiKhoaBieu, TinhLoiNhac, LuatCongGio, GateStore that.
Neu no tu tinh lay thi du an co hai bo luat, va den luc sua mot ben thi ben kia im
lang noi doi.

Chi dung thu vien co san cua Python 3, khong phai cai gi them.
"""

import base64
import json
import re
import shlex
import shutil
import signal
import subprocess
import sys
import threading
import time
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

GOC = Path(__file__).resolve().parent
TRANG = GOC / "web" / "index.html"
PKG = "vn.huytl.homeworkgate"
RUNNER = f"{PKG}.test/androidx.test.runner.AndroidJUnitRunner"
DICH_VU_TRO_NANG = f"{PKG}/{PKG}.guard.GuardAccessibilityService"
DUONG_CAY = "/sdcard/cay-man.xml"
APK_TEST = GOC.parent / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"

# adb logcat la kenh dung chung: xoa roi doc lai ma hai yeu cau chay chen nhau thi
# ben nay nuot ket qua cua ben kia. Mot khoa cho tat ca cac lan goi may ao.
KHOA = threading.Lock()

# Chay kho lich khong doi neu tham so khong doi, ma moi lan mat gan mot giay.
NHO = {}


def adb(*args, nhi_phan=False, timeout=60):
    r = subprocess.run(["adb", *args], capture_output=True, timeout=timeout)
    if nhi_phan:
        return r.stdout
    return r.stdout.decode("utf-8", "replace").replace("\r", "").strip()


def co_may_ao():
    return any(d.endswith("\tdevice") for d in adb("devices").splitlines()[1:])


def dam_bao_root():
    """Chac chan adbd dang chay quyen root.

    Cac man hinh cua app deu exported=false - dung nhu no phai the, de con khong mo
    thang bang adb hay shortcut. Nghia la chi shell quyen root moi mo duoc chung.
    May ao khoi dong lai la adbd tut ve quyen shell, va luc do moi muc mo man hinh
    deu bao "Permission Denial" - trong khi app khong sai gi.
    """
    if adb("shell", "id", "-u").strip() == "0":
        return
    adb("root")
    adb("wait-for-device", timeout=30)
    time.sleep(1.0)


def app_dang_chay():
    return bool(adb("shell", "pidof", PKG).strip())


def cap_quyen():
    """Cap lai quyen tro nang cho app.

    Phai goi sau MOI lan force-stop: Android xoa han app khoi
    enabled_accessibility_services khi goi bi force-stop. Khong cap lai thi app
    khong chan duoc app nao nua, ma tren man hinh no chi hien mot dong "Chua bat
    dich vu tro nang" lan giua nhung viec khac - rat de tuong la app hong.
    """
    adb("shell", "settings", "put", "secure",
        "enabled_accessibility_services", DICH_VU_TRO_NANG)
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")


def mo_app(cho_dich_vu=True):
    adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
    if not cho_dich_vu:
        return
    # Cho den khi dich vu len that, thay vi ngu mot khoang doan chung.
    for _ in range(20):
        time.sleep(0.4)
        if "ApprovalService" in adb("shell", "dumpsys", "activity", "services", PKG):
            return


def giu_app(fn):
    """Chay fn roi don dep hau qua cua "am instrument".

    Chay xong mot bo instrument, Android force-stop luon goi duoc do - trong log la
    "Force stopping ...: finished inst". Hai he luy, ca hai deu tung lam minh tuong
    app hong: app dang chay bi giet, va quyen tro nang bi xoa theo. Nen moi duong
    nao co dung toi may ao deu di qua day.
    """
    dang_chay = app_dang_chay()
    try:
        return fn()
    finally:
        cap_quyen()
        if dang_chay:
            mo_app()


def b64(o):
    """JSON -> base64 an toan cho shell tren may. Xem ManualBang.giaiMa."""
    return base64.urlsafe_b64encode(
        json.dumps(o, ensure_ascii=False).encode("utf-8")).decode("ascii")


def thieu_apk_test(ket):
    return ("Unable to find instrumentation" in ket
            or "Cannot locate ourselves" in ket
            or "INSTRUMENTATION_CODE: -1" in ket)


def cai_lai_apk_test():
    if not APK_TEST.exists():
        return False
    return "Success" in adb("install", "-r", str(APK_TEST), timeout=180)


def chay_test(lop, **tham_so):
    """Chay mot lop test roi vot lai nhung dong JSON no in ra."""
    with KHOA:
        adb("logcat", "-c")
        lenh = ["shell", "am", "instrument", "-w", "-e", "class", lop]
        for k, v in tham_so.items():
            if v is not None:
                # adb shell noi cac doi so lai bang khoang trang roi shell tren may
                # tach ra lai, nen gia tri co khoang trang phai tu boc nhay lay.
                lenh += ["-e", k, shlex.quote(str(v))]
        lenh.append(RUNNER)
        ket = adb(*lenh, timeout=180)
        # "Unable to find instrumentation" khong chua chu FAILURES nao ca, nen neu
        # chi bat hai chuoi kia thi lan APK test bien mat khoi may se di qua im
        # lang: moi lenh dat deu khong lam gi, va cac muc thu sau do bao "khong
        # thay chu" - trong dang le phai bao "khong chay duoc tren may".
        if thieu_apk_test(ket):
            # APK test co luc bien mat giua chung: chay "gradlew connectedAndroidTest"
            # hay bam Run test trong Android Studio deu go no ra khi xong, va mot bo
            # thu dang chay o day thi nga giua duong. Cai lai roi chay tiep, chu
            # khong bat nguoi ta chay lai ca bo tu dau.
            if not cai_lai_apk_test():
                raise RuntimeError(
                    "Máy ảo chưa có APK test (vn.huytl.homeworkgate.test) và cài "
                    "lại không được. Chạy: tools/emu.sh install")
            ket = adb(*lenh, timeout=180)
            if thieu_apk_test(ket):
                raise RuntimeError(
                    "Cài lại APK test rồi mà vẫn không chạy được. "
                    "Chạy: tools/emu.sh install")
        if "FAILURES" in ket or "Process crashed" in ket:
            raise RuntimeError(ket[-2000:])
        if "OK (" not in ket and "INSTRUMENTATION_CODE" not in ket:
            raise RuntimeError(f"không rõ kết quả chạy {lop}: {ket[-600:]}")
        tho = adb("logcat", "-d", "-s", "System.out")
    ra = []
    for dong in tho.splitlines():
        for tien_to in ("LICHJSON: ", "BANGJSON: "):
            cho = dong.find(tien_to)
            if cho >= 0:
                ra.append(json.loads(dong[cho + len(tien_to):]))
                break
    return ra


# ---------------------------------------------------------------- doc / van


def lich(tu, so_ngay, dasoan, chan):
    khoa = (tu, so_ngay, dasoan, chan)
    if khoa in NHO:
        return NHO[khoa]
    ban_ghi = chay_test(
        f"{PKG}.ManualLich#json",
        tu=tu, ngay=so_ngay,
        dasoan="tatca" if dasoan else None,
        chan="0" if not chan else None,
    )
    cac_ngay, theo_ngay = [], {}
    for b in ban_ghi:
        k = b.get("k")
        if k == "ngay":
            b["buoi"], b["moc"] = [], []
            cac_ngay.append(b)
            theo_ngay[b["ngay"]] = b
        elif k in ("buoi", "moc"):
            theo_ngay[b["ngay"]][k].append(b)
    # Moi moc keo dai toi moc ke tiep; moc cuoi keo het ngay.
    for n in cac_ngay:
        for i, m in enumerate(n["moc"]):
            m["het"] = n["moc"][i + 1]["phut"] if i + 1 < len(n["moc"]) else 1440
    NHO[khoa] = cac_ngay
    return cac_ngay


def bang():
    """Toan bo trang thai may ao, gom theo tung muc."""
    return {b["k"]: b for b in chay_test(f"{PKG}.ManualBang#trangthai")
            if b.get("k") not in (None, "het")}


def chay_bo_test(lop):
    with KHOA:
        ket = adb("shell", "am", "instrument", "-w", "-e", "class", lop, RUNNER,
                  timeout=600)
    m = re.search(r"OK \((\d+) tests?\)", ket)
    if m:
        return {"dat": True, "so": int(m.group(1)), "tho": ket[-4000:]}
    m = re.search(r"Tests run: (\d+),\s+Failures: (\d+)", ket)
    return {"dat": False, "so": int(m.group(1)) if m else 0,
            "hong": int(m.group(2)) if m else 0, "tho": ket[-8000:]}


def trang_thai():
    if not co_may_ao():
        return {"mayAo": False, "gioMac": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
    # Dau nham nhay: adb shell cat chuoi theo khoang trang, nen dinh dang phai boc
    # lai trong nhay, khong thi chi con phan ngay.
    return {
        "mayAo": True,
        "gioMayAo": adb("shell", 'date "+%Y-%m-%d %H:%M:%S"'),
        "gioMac": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "tuDongGio": adb("shell", "settings", "get", "global", "auto_time") == "1",
        "daCai": PKG in adb("shell", "pm", "list", "packages", PKG),
        "dangChay": app_dang_chay(),
        "dichVu": "ApprovalService" in adb(
            "shell", "dumpsys", "activity", "services", PKG),
        "troNang": PKG in adb(
            "shell", "settings", "get", "secure", "enabled_accessibility_services"),
    }


def van_gio(luc):
    """luc = 'YYYY-MM-DDTHH:MM'. Tat auto_time truoc, khong thi mang keo lai."""
    m = re.fullmatch(r"(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})", luc)
    if not m:
        raise ValueError(f"Moc khong doc duoc: {luc}")
    nam, thang, ngay, gio, phut = m.groups()
    with KHOA:
        adb("root")
        adb("wait-for-device", timeout=30)
        adb("shell", "settings", "put", "global", "auto_time", "0")
        adb("shell", "settings", "put", "global", "auto_time_zone", "0")
        adb("shell", "date", f"{thang}{ngay}{gio}{phut}{nam}.00")
        adb("shell", "am", "broadcast", "-a", "android.intent.action.TIME_SET")


def gio_that():
    with KHOA:
        adb("root")
        adb("wait-for-device", timeout=30)
        adb("shell", "settings", "put", "global", "auto_time", "1")
        adb("shell", "settings", "put", "global", "auto_time_zone", "1")
        adb("shell", "date", datetime.now().strftime("%m%d%H%M%Y.%S"))
        adb("shell", "am", "broadcast", "-a", "android.intent.action.TIME_SET")


# ---------------------------------------------------------------- may chu


# ---------------------------------------------------------------- thu tu dong

import tempfile          # noqa: E402  (de gan cho phan tu dong thu)
import kichban           # noqa: E402

TU_DONG = {"dangChay": False, "xong": 0, "tong": 0, "hienTai": "",
           "ket": [], "thuMuc": "", "dung": False}
ANH_THU = {}             # ma muc -> byte PNG, de trang web hien lai


class May:
    """Cai tay quay cho mot muc thu: dung boi canh roi dua man hinh can xem ra truoc.

    Cac lan GHI gom lai roi moi khoi dong app mot lan: "am instrument" da force-stop
    app khi chay xong, nen giua hai lan ghi khong co tien trinh app nao con song de
    ghi de - khoi dong lai sau tung lan la thua va cham.
    """

    def __init__(self):
        self._da_ghi = False

    # --- dung boi canh ---
    def dat(self, viec, **k):
        if "chu" in k:
            k["chu"] = base64.urlsafe_b64encode(
                str(k["chu"]).encode("utf-8")).decode("ascii")
        chay_test(f"{PKG}.ManualBang#viec", viec=viec, **k)
        self._da_ghi = True

    def van(self, luc):
        van_gio(luc)
        # Cho app chay mot nhip NGAY SAU khi doi gio, truoc khi kich ban cap phieu.
        #
        # Dong ho lui lai la dau hieu gian lan that, va app cat phien vi chuyen do
        # (EndReason.CLOCK_TAMPER) - dung nhu no phai lam. Nhung o day nguoi van dong
        # ho la minh, nen phai cho no ghi nhan moc moi truoc; khong thi phieu vua cap
        # bi cat ngay, va muc thu bao hong trong khi app khong sai gi.
        adb("shell", "am", "force-stop", PKG)
        cap_quyen()
        adb("shell", "am", "start", "-n", f"{PKG}/{PKG}.ui.HomeActivity")
        time.sleep(2.0)
        self._da_ghi = True

    def gio_that(self):
        gio_that()
        self._da_ghi = True

    def chay(self, lop_ham):
        """Chay mot lop Manual bat ky, vi du "ManualTienBo#napThu".

        Cac man hinh moi deu can so lieu co san moi nhin ra duoc cai gi; nguoi viet
        app da lam san cac lop nap so lieu do, nen o day goi lai chu khong chep mot
        ban nap khac.
        """
        chay_test(f"{PKG}.{lop_ham}")
        self._da_ghi = True

    def _lam_moi(self):
        dam_bao_root()
        adb("shell", "am", "force-stop", PKG)
        cap_quyen()
        self._da_ghi = False

    # --- dua man hinh ra truoc ---
    def nen(self):
        """Bat dich vu ma KHONG mo man hinh nao cua app.

        Khong dung "mo HomeActivity roi bam Home": man chan va the nhac deu nhuong
        cho chinh app Nop bai, nen luc app vua o truoc mat thi vong xet dau tien cua
        dich vu quyet dinh an di - va voi the "soan cap" thi vong sau tan sau muoi
        giay nua moi toi. Bat thang dich vu thi vong dau tien da thay man hinh nen
        la cua Android, va no hien ra ngay.
        """
        self._lam_moi()
        adb("shell", "am", "start-foreground-service", "-n",
            f"{PKG}/.telegram.ApprovalService")
        time.sleep(2.5)

    def man(self, ten, thu=None):
        """Mo mot man hinh. [thu] la cac extra kieu boolean di kem intent."""
        self._lam_moi()
        lenh = ["shell", "am", "start", "-n", f"{PKG}/{PKG}.ui.{ten}"]
        for k, v in (thu or {}).items():
            lenh += ["--ez", k, "true" if v else "false"]
        adb(*lenh)
        time.sleep(2.0)

    def bam(self, chu):
        """Bam vao nut mang dong chu nay, bang tay that chu khong goi ham trong app.

        Co nhung trang thai chi dung duoc khi CHINH APP doi no: bam "Bat dau choi"
        thi phien chay tiep trong tien trinh app. Neu dat qua ManualBang thi phai
        force-stop de app doc lai, ma force-stop giua phien lai lam app tam dung giu
        gio - thanh ra khong bao gio nhin thay canh "dang duoc choi".
        """
        # Man chinh co luc day the (on lai, thieu viec, soan cap) day nut to xuong
        # duoi vung nhin thay, ma uiautomator chi doc duoc phan dang hien. Cuon
        # xuong tim tiep thay vi bao hong - nguoi dung cung se cuon nhu vay.
        for lan in range(4):
            o = tim_o(chu)
            if o:
                adb("shell", "input", "tap", str(o[0]), str(o[1]))
                time.sleep(2.0)
                return
            if lan < 3:
                adb("shell", "input", "swipe", "1280", "1200", "1280", "500", "300")
                time.sleep(1.0)
        raise RuntimeError(f"không thấy nút “{chu}” trên màn hình, kể cả khi cuộn xuống")

    def go_chu(self, o, chu):
        """Bam vao o nhap roi go chu vao do, nhu con go tren tablet.

        Duong hoc thuoc khong chup anh va khong goi AI - con go thang tren may. Muon
        thu that duong do thi phai go that, khong co cua sau nao de dat san cau tra
        loi vao.
        """
        diem = tim_o(o)
        if not diem:
            raise RuntimeError(f"không thấy ô “{o}” để gõ vào")
        adb("shell", "input", "tap", str(diem[0]), str(diem[1]))
        time.sleep(0.8)
        adb("shell", "input", "text", shlex.quote(chu))
        time.sleep(0.8)

    def go_dap_an_dung(self, o, duong_bo):
        """Doc cau hoi dang hien roi go dung dap an cua no.

        Khong neo cung dap an cua mot the: so the den luot va thu tu boc doi theo
        lich on, nen the dau tien hom nay khong phai the dau tien hom qua. Neo cung
        thi muc thu hong vi hoi sang cau khac, ma app van dung.
        """
        chu = chu_tren_man()
        cac = []
        o_bo = json.loads((GOC.parent / duong_bo).read_text(encoding="utf-8"))
        for bai in o_bo.get("cac_bai", []):
            cac += bai.get("cac_the", [])
        # Lay the co cau hoi DAI NHAT khop voi man hinh: "(a + b)²" cung nam trong
        # "(a + b)³" neu so kieu ngan nhat truoc.
        khop = sorted((t for t in cac if t.get("hoi") and t["hoi"] in chu),
                      key=lambda t: -len(t["hoi"]))
        if not khop:
            raise RuntimeError("không nhận ra thẻ nào đang hiện trên màn hình")
        self.go_chu(o, go_duoc(khop[0]["dap"]))

    def tat_mo_lai(self):
        """Tat han app roi mo lai, de xem no nho duoc gi qua mot lan bi giet."""
        adb("shell", "am", "force-stop", PKG)
        cap_quyen()
        time.sleep(1.0)
        adb("shell", "am", "start", "-n", f"{PKG}/{PKG}.ui.HomeActivity")
        time.sleep(2.5)

    def mo_goi(self, goi):
        self._lam_moi()
        mo_app()
        adb("shell", "monkey", "-p", goi, "-c",
            "android.intent.category.LAUNCHER", "1")
        time.sleep(4)


def chu_tren_man():
    """Moi chu dang hien tren man hinh, ke ca tren cua so noi cua app khac.

    Doc bang uiautomator chu khong doc anh: no thay duoc man chan (mot overlay),
    va so sanh chuoi thi khong co chuyen "gan dung".
    """
    return " | ".join(re.findall(r'text="([^"]*)"', do_cay_man()))


def do_cay_man(): 
    """Cay giao dien dang hien, dang XML.

    Xoa file cu truoc roi moi dump: uiautomator co luc khong dump duoc (man hinh
    khong chiu dung yen, hoac dang o mot cua so no khong voi toi), va luc do lenh
    cat van doc ra ban cu. Da mot lan ba man hinh khac nhau deu "chua" cung mot
    dong chu vi chuyen nay - test bao dat ma thuc ra dang doc lai anh cu.
    """
    # Thu vai lan: uiautomator doi man hinh dung yen moi dump, ma man hinh dang co
    # dong ho dem nguoc thi giay nao no cung doi. Hai muc ve phien choi tung hong vi
    # dung cai do - khong phai app sai.
    for lan in range(3):
        adb("shell", "rm", "-f", DUONG_CAY)
        ket = adb("shell", "uiautomator", "dump", DUONG_CAY, timeout=20)
        if "dumped to" in ket:
            return adb("shell", "cat", DUONG_CAY, timeout=20)
        if lan < 2:
            time.sleep(1.5)
    return ""


def go_duoc(dap):
    """Doi dap an sang dang go duoc bang "adb shell input text".

    HocThuoc.chuanHoa doi "²" thanh "^2" truoc khi so, nen go "^2" van dung - ma
    "²" thi input text tren may ao khong go ra duoc.
    """
    return (dap.replace("²", "^2").replace("³", "^3")
            .replace("−", "-").replace("–", "-").replace("—", "-"))


def tim_o(chu):
    """Tim o vuong cua phan tu mang dong chu nay, tra ve diem giua."""
    xml = do_cay_man()
    if not xml:
        return None
    for the in re.findall(r"<node[^>]*/?>", xml):
        m_chu = re.search(r'text="([^"]*)"', the)
        if not m_chu or chu not in m_chu.group(1):
            continue
        m_o = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', the)
        if m_o:
            x1, y1, x2, y2 = map(int, m_o.groups())
            return ((x1 + x2) // 2, (y1 + y2) // 2)
    return None


def man_tren_cung():
    d = adb("shell", "dumpsys", "activity", "activities")
    m = re.search(r"topResumedActivity=ActivityRecord\{\S+ \S+ ([^/]+)/(\S+)", d)
    return f"{m.group(1)}/{m.group(2)}" if m else ""


def cua_so_noi():
    """Cua so noi cua app dang che cai gi: "chan", "the", hay "khong".

    Doc bang dumpsys window vi uiautomator khong thay cua so khong focus - the nhac
    la mot cai nhu vay. Phan biet bang kich thuoc: man chan phu kin man hinh, the
    nhac chi la mot o nho o goc.
    """
    tho = adb("shell", "dumpsys", "window", "windows")
    khoi, dang_trong = [], None
    for dong in tho.splitlines():
        if re.search(r"Window #\d+ Window\{\S+ \S+ " + re.escape(PKG) + r"\}:", dong):
            dang_trong = []
            khoi.append(dang_trong)
        elif dang_trong is not None:
            if dong.strip().startswith("Window #"):
                dang_trong = None
            else:
                dang_trong.append(dong)
    rong_man = 0
    m = re.search(r"display=\[0,0\]\[(\d+),(\d+)\]", tho)
    if m:
        rong_man = int(m.group(1)) * int(m.group(2))
    for k in khoi:
        than = "\n".join(k)
        if "mViewVisibility=0x0" not in than:
            continue          # cua so con do nhung dang an
        m = re.search(r"Requested w=(\d+) h=(\d+)", than)
        if not m:
            continue
        dien = int(m.group(1)) * int(m.group(2))
        if rong_man and dien >= rong_man * 0.6:
            return "chan"
        return "the"
    return "khong"


def nhac_theo_luat(luc):
    """Hoi thang TinhLoiNhac xem dung moc do dang le phai nhac gi."""
    ra = chay_test(f"{PKG}.ManualLich#jsonluc", luc=luc)
    return ra[0] if ra else {}


def chup(ma):
    # Chup hong thi bo qua, dung de no keo do ca muc thu: tam anh la de nguoi xem,
    # con cham diem thi da lam bang chu va cua so noi roi.
    try:
        png = adb("exec-out", "screencap", "-p", nhi_phan=True, timeout=30)
    except Exception:
        return False
    ANH_THU[ma] = png
    thu_muc = TU_DONG.get("thuMuc")
    if thu_muc:
        try:
            (Path(thu_muc) / f"{ma}.png").write_bytes(png)
        except OSError:
            pass
    return True


TEN_NOI = {"chan": "màn chắn phủ kín", "the": "thẻ nhắc nhỏ",
           "khong": "không che gì"}

# Cua so noi hien ra sau mot nhip cua dich vu, khong phai ngay khi lenh chay xong.
# Soat mot phat roi ket luan thi cung mot muc luc dat luc hong tuy may nhanh cham.
DOI_TOI_DA_GIAY = 15


def soat_man(muc, chu, noi, tren):
    """Nhung cho khong dat, xet tren mot lan chup man hinh."""
    loi = []
    if not chu and (muc.get("cho") or muc.get("khong")):
        loi.append("không đọc được chữ trên màn hình (uiautomator không dump được)")
    for c in muc.get("cho", []):
        if c not in chu:
            loi.append(f"không thấy chữ “{c}” trên màn hình")
    for c in muc.get("khong", []):
        if c in chu:
            loi.append(f"không được có chữ “{c}” mà vẫn thấy")
    if muc.get("noi") and noi != muc["noi"]:
        loi.append(f"cửa sổ nổi đang là “{TEN_NOI[noi]}”, "
                   f"đáng lẽ “{TEN_NOI[muc['noi']]}”")
    if muc.get("noi_khong") and noi == muc["noi_khong"]:
        loi.append(f"đang có “{TEN_NOI[noi]}” mà đáng lẽ không được có")
    if muc.get("man_tren_cung") and muc["man_tren_cung"] not in tren:
        loi.append(f"màn trên cùng là {tren or '(không rõ)'}, "
                   f"đáng lẽ {muc['man_tren_cung']}")
    if muc.get("man_khong") and muc["man_khong"] in tren:
        loi.append(f"vẫn đang ở {tren} — đáng lẽ phải bị đẩy ra")
    return loi


def chay_mot_muc(muc, may):
    bat_dau = time.time()
    try:
        muc["lam"](may)
    except Exception as e:
        return {"ma": muc["ma"], "ten": muc["ten"], "nhom": muc["nhom"],
                "dat": False, "loai": "man", "ghi": [f"dựng bối cảnh hỏng: {e}"],
                "giay": round(time.time() - bat_dau, 1), "coAnh": False}

    # Doi man hinh yen vi roi moi ket luan, va chup sau cung - de tam anh nguoi xem
    # dung la canh may da cham diem, khong phai mot khoanh khac truoc do.
    # Chi doc chu khi muc nay that su soat chu: mot lan uiautomator dump ton vai
    # giay, va co luc khong dump duoc nen phai thu lai - muc chi hoi "con co con o
    # trong Chrome khong" thi khong viec gi phai tra cai gia do.
    can_chu = bool(muc.get("cho") or muc.get("khong"))
    han = time.time() + DOI_TOI_DA_GIAY
    while True:
        chu = chu_tren_man() if can_chu else ""
        noi, tren = cua_so_noi(), man_tren_cung()
        loi = soat_man(muc, chu, noi, tren)
        if not loi or time.time() >= han:
            break
        time.sleep(1.5)
    co_anh = chup(muc["ma"])

    ghi = []
    if muc.get("noi") and not loi:
        ghi.append(f"cửa sổ nổi đúng kiểu: {TEN_NOI[noi]}")
    if muc.get("noi_khong") and noi != muc["noi_khong"]:
        ghi.append(f"không có “{TEN_NOI[muc['noi_khong']]}” — đúng "
                   f"(đang là: {TEN_NOI[noi]})")
    if muc.get("man_tren_cung") and muc["man_tren_cung"] in tren:
        ghi.append(f"màn trên cùng đúng: {muc['man_tren_cung']}")
    if muc.get("man_khong") and muc["man_khong"] not in tren:
        ghi.append(f"đã bị đẩy ra khỏi {muc['man_khong']} — đúng")

    if muc.get("luc"):
        n = nhac_theo_luat(muc["luc"])
        cau = f"{n.get('tieuDe', '')} · {n.get('chiTiet', '')}"
        if muc.get("loai") and n.get("loai") != muc["loai"]:
            loi.append(f"luật ra {n.get('loai')}, đáng lẽ {muc['loai']}")
        for c in muc.get("cho_logic", []):
            if c not in cau:
                loi.append(f"luật không sinh ra chữ “{c}” (mà ra: {cau.strip(' ·')})")
        if cau.strip(" ·"):
            ghi.append(f"máy phải hiện: {cau}")

    if muc.get("don"):
        try:
            muc["don"](may)
        except Exception as e:
            loi.append(f"dọn dẹp hỏng: {e}")

    return {"ma": muc["ma"], "ten": muc["ten"], "nhom": muc["nhom"],
            "dat": not loi, "loai": "man",
            "ghi": loi + ghi if loi else (ghi or ["mọi thứ cần có đều đúng"]),
            "chu": chu[:1200], "giay": round(time.time() - bat_dau, 1),
            "coAnh": co_anh}


def chay_tu_dong(cac_ma):
    """Chay cac muc da chon. Goi trong mot luong nen; tien do de o [TU_DONG]."""
    muc_theo_ma = {m["ma"]: m for m in kichban.danh_sach()}
    man = [muc_theo_ma[m] for m in cac_ma if m in muc_theo_ma]
    lop = [m[len("test:"):] for m in cac_ma if m.startswith("test:")]

    thu_muc = Path(tempfile.gettempdir()) / "homework-gate-thu" / \
        datetime.now().strftime("%Y%m%d-%H%M%S")
    thu_muc.mkdir(parents=True, exist_ok=True)

    TU_DONG.update(dangChay=True, xong=0, tong=len(man) + len(lop),
                   hienTai="", ket=[], thuMuc=str(thu_muc), dung=False)
    ANH_THU.clear()
    may = May()

    # Don rac cua lan truoc truoc khi bat dau.
    #
    # Viec nha va che do ba deu phu len MOI muc khac: con viec nha thi man chan che
    # kin man hinh du dang xet canh nao, va che do ba thi tat het loi nhac. Mot lan
    # chay do dang bo lai mot trong hai thu la ca bo thu sau do bao hong ma app
    # khong sai gi.
    dam_bao_root()
    if man:
        TU_DONG["hienTai"] = "dọn trạng thái còn lại của lần trước"
        try:
            may.dat("xoaviecnha")
            may.dat("badong")
            # So cai con cau "den hen on lai" tu lan truoc thi man chinh moc them
            # mot the, va the do day nut to xuong khoi vung nhin thay.
            may.dat("xoasocai")
        except Exception as e:
            TU_DONG["ket"].append({
                "ma": "don-dep", "ten": "Dọn trạng thái trước khi thử",
                "nhom": "Chuẩn bị", "dat": False, "loai": "man", "coAnh": False,
                "ghi": [str(e)], "giay": 0})
    try:
        for m in man:
            if TU_DONG["dung"]:
                break
            TU_DONG["hienTai"] = m["ten"]
            # Mot muc no ra ngoai (adb treo, may ao lac) thi chi muc do hong. Truoc
            # day cho nay khong bat gi, nen mot lan screencap qua han la ca lan chay
            # dung o giua va hai muoi muc con lai khong ai biet the nao.
            try:
                TU_DONG["ket"].append(chay_mot_muc(m, may))
            except Exception as e:
                TU_DONG["ket"].append({
                    "ma": m["ma"], "ten": m["ten"], "nhom": m["nhom"],
                    "dat": False, "loai": "man", "coAnh": False,
                    "ghi": [f"{type(e).__name__}: {e}"], "giay": 0})
            TU_DONG["xong"] += 1

        for ten_lop in lop:
            if TU_DONG["dung"]:
                break
            TU_DONG["hienTai"] = f"bộ test {ten_lop}"
            t0 = time.time()
            try:
                r = chay_bo_test(f"{PKG}.{ten_lop}")
            except Exception as e:
                r = {"dat": False, "so": 0, "hong": 0, "tho": f"{type(e).__name__}: {e}"}
            TU_DONG["ket"].append({
                "ma": f"test:{ten_lop}", "ten": ten_lop, "nhom": "Bộ test",
                "dat": r["dat"], "loai": "test", "coAnh": False,
                "ghi": [f"{r['so']} test đạt hết"] if r["dat"]
                       else [f"{r.get('hong', '?')} hỏng trên {r['so']} test"],
                "tho": r["tho"][-3000:], "giay": round(time.time() - t0, 1)})
            TU_DONG["xong"] += 1
    finally:
        # Tra may ao ve trang thai dung duoc: gio that, quyen con, app dang chay.
        try:
            gio_that()
            cap_quyen()
            mo_app()
        except Exception:
            pass
        TU_DONG["dangChay"] = False
        TU_DONG["hienTai"] = ""


class Tay(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *a):
        pass

    def _gui(self, ma, kieu, than):
        if isinstance(than, str):
            than = than.encode("utf-8")
        self.send_response(ma)
        self.send_header("Content-Type", kieu)
        self.send_header("Content-Length", str(len(than)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(than)

    def _json(self, o, ma=200):
        self._gui(ma, "application/json; charset=utf-8",
                  json.dumps(o, ensure_ascii=False))

    def _can_may_ao(self):
        if co_may_ao():
            return True
        self._json({"loi": "Chua co may ao. Chay: tools/emu.sh up"}, 503)
        return False

    def do_GET(self):
        u = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(u.query).items()}
        try:
            if u.path in ("/", "/index.html"):
                return self._gui(200, "text/html; charset=utf-8",
                                 TRANG.read_text(encoding="utf-8"))
            if u.path == "/api/trangthai":
                return self._json(trang_thai())
            if u.path == "/api/lich":
                if not self._can_may_ao():
                    return
                return self._json({"ngay": giu_app(lambda: lich(
                    q.get("tu") or datetime.now().strftime("%Y-%m-%d"),
                    int(q.get("ngay", 7)),
                    q.get("dasoan") == "1",
                    q.get("chan", "1") == "1"))})
            if u.path == "/api/luc":
                if not self._can_may_ao():
                    return
                ra = giu_app(lambda: chay_test(
                    f"{PKG}.ManualLich#jsonluc", luc=q["luc"],
                    dasoan="tatca" if q.get("dasoan") == "1" else None,
                    chan="0" if q.get("chan", "1") != "1" else None))
                return self._json(ra[0] if ra else {"loi": "khong co ket qua"})
            if u.path == "/api/bang":
                if not self._can_may_ao():
                    return
                return self._json(giu_app(bang))
            if u.path == "/api/tudong/danhsach":
                return self._json({
                    "man": [{k: v for k, v in m.items()
                             if k in ("ma", "ten", "nhom")}
                            for m in kichban.danh_sach()],
                    "test": [{"ma": f"test:{a}", "ten": a, "nhom": "Bộ test",
                              "moTa": b} for a, b in kichban.BO_TEST]})
            if u.path == "/api/tudong/tien":
                return self._json(TU_DONG)
            if u.path == "/api/tudong/anh":
                png = ANH_THU.get(q.get("ma", ""))
                if not png:
                    return self._gui(404, "text/plain", "chua co anh")
                return self._gui(200, "image/png", png)
            if u.path == "/api/nhatky":
                tho = adb("logcat", "-d", "-t", "400", "-s", "HomeworkGate",
                          "Guard", "ApprovalService", "AndroidRuntime", "System.out")
                return self._json({"tho": tho[-20000:]})
            if u.path == "/api/anh":
                if not co_may_ao():
                    return self._gui(503, "text/plain", "chua co may ao")
                with KHOA:
                    png = adb("exec-out", "screencap", "-p", nhi_phan=True, timeout=30)
                return self._gui(200, "image/png", png)
            return self._gui(404, "text/plain", "khong co")
        except Exception as e:  # tra loi ro ra man hinh chu khong chet im
            return self._json({"loi": f"{type(e).__name__}: {e}"}, 500)

    def do_POST(self):
        u = urlparse(self.path)
        n = int(self.headers.get("Content-Length") or 0)
        than = json.loads(self.rfile.read(n) or b"{}")
        try:
            if u.path == "/api/viec":
                tham = {k: v for k, v in than.items() if k != "moiApp"}
                # ManualBang tu giai base64 cho tham so chu.
                if "chu" in tham:
                    tham["chu"] = base64.urlsafe_b64encode(
                        str(tham["chu"]).encode("utf-8")).decode("ascii")

                def lam():
                    ket = chay_test(f"{PKG}.ManualBang#viec", **tham)
                    # Doc lai trang thai TRUOC khi mo lai app: doc cung la mot lan
                    # instrument, ma lan nao cung force-stop goi khi xong.
                    return {"xong": ket[0] if ket else None, "bang": bang()}

                # Ghi xong phai cho app doc lai tu dia: cau hinh nam trong
                # EncryptedSharedPreferences, tien trinh app dang chay giu mot ban
                # trong bo nho va se ghi de nguoc lai. Tat dau tich thi nhanh hon
                # nhung phai tu khoi dong lai o cuoi.
                if than.get("moiApp", True):
                    ra = lam()
                    cap_quyen()
                    mo_app()
                    return self._json(ra)
                return self._json(giu_app(lam))

            if u.path == "/api/conggio":
                return self._json(giu_app(lambda: (lambda r: r[0] if r else
                                                   {"loi": "khong co ket qua"})(
                    chay_test(f"{PKG}.ManualBang#conggio",
                              cham=b64(than["cham"]),
                              bayGio=than.get("bayGio"),
                              daCongLamThem=than.get("daCongLamThem", 0),
                              goiDaCo="1" if than.get("goiDaCo") else "0",
                              onTap="1" if than.get("onTap") else "0"))))

            if u.path == "/api/test":
                return self._json(giu_app(lambda: chay_bo_test(than["lop"])))

            if u.path == "/api/telegram":
                # Gui THAT len bot va chat dang cau hinh tren may ao. Chi chay khi
                # nguoi dung tu bam va tu xac nhan tren trang.
                def gui():
                    with KHOA:
                        return adb("shell", "am", "instrument", "-w", "-e", "class",
                                   f"{PKG}.ManualSend",
                                   "-e", "dando", str(than.get("dando", 1)),
                                   "-e", "debai", str(than.get("debai", 1)),
                                   "-e", "baigiai", str(than.get("baigiai", 1)),
                                   RUNNER, timeout=300)
                return self._json({"tho": giu_app(gui)[-4000:]})

            if u.path == "/api/tudong/chay":
                if TU_DONG["dangChay"]:
                    return self._json({"loi": "Đang chạy dở, đợi xong đã."}, 409)
                if not co_may_ao():
                    return self._json({"loi": "Chưa có máy ảo."}, 503)
                threading.Thread(target=chay_tu_dong, args=(than["ma"],),
                                 daemon=True).start()
                time.sleep(0.3)
                return self._json(TU_DONG)
            if u.path == "/api/tudong/dung":
                TU_DONG["dung"] = True
                return self._json(TU_DONG)

            if u.path == "/api/gio":
                van_gio(than["luc"])
            elif u.path == "/api/giothuc":
                gio_that()
            elif u.path == "/api/home":
                adb("shell", "input", "keyevent", "KEYCODE_HOME")
            elif u.path == "/api/moapp":
                mo_app()
            elif u.path == "/api/khoidonglai":
                adb("shell", "am", "force-stop", PKG)
                cap_quyen()
                time.sleep(1.0)
                mo_app()
            elif u.path == "/api/quyen":
                cap_quyen()
            elif u.path == "/api/quen":
                NHO.clear()
            else:
                return self._gui(404, "text/plain", "khong co")
            return self._json(trang_thai())
        except Exception as e:
            return self._json({"loi": f"{type(e).__name__}: {e}"}, 500)


def main():
    if not shutil.which("adb"):
        sys.exit("Khong thay adb trong PATH. Chay qua tools/emu.sh web.")
    cong = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
    may = ThreadingHTTPServer(("127.0.0.1", cong), Tay)
    # Bat ca SIGTERM chu khong chi Ctrl+C: kill may chu kieu nao thi dong ho may ao
    # cung phai duoc tra ve, khong thi no ket lai o mot ngay nao do khong ai nho.
    signal.signal(signal.SIGTERM,
                  lambda *_: (_ for _ in ()).throw(KeyboardInterrupt))
    print(f"Mo trinh duyet: http://127.0.0.1:{cong}", flush=True)
    if not co_may_ao():
        print("Chua thay may ao. Chay 'tools/emu.sh up' o mot cua so khac.")
    try:
        may.serve_forever()
    except KeyboardInterrupt:
        # Luoi do thu hai, canh quen bam nut "Ve gio that": tat may chu ma dong ho
        # may ao con ket lai o tuan truoc thi hom sau mo ra ngoi doan app hong cho
        # nao. Ctrl+C la tra lai luon.
        print()
        if co_may_ao() and adb("shell", "settings", "get", "global",
                               "auto_time") != "1":
            print("Dong ho may ao dang bi van tay - tra ve gio that...")
            try:
                gio_that()
                print("Da tra ve", adb("shell", 'date "+%Y-%m-%d %H:%M:%S"'))
            except Exception as e:
                print("Tra khong duoc:", e, "- chay 'tools/emu.sh giothuc'")
        print("Dong.")


if __name__ == "__main__":
    main()
