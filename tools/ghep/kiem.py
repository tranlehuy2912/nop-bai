#!/usr/bin/env python3
"""
Kiem du lieu ghep (bai lam tren may) truoc khi nap vao app. Dinh dang o DINH_DANG.md.

Chay:
    python3 tools/ghep/kiem.py app/src/main/assets/nganhang/sbtanh8.json
    python3 tools/ghep/kiem.py phan.json --sach app/src/main/assets/nganhang/sbttoan8t1.json
    python3 tools/ghep/kiem.py --tat-ca

Hai loai file:
  - file sach day du (co "cac_bai"): kiem moi cau co "ghep".
  - file phan (co "ghep" la bang ma -> ghep, cua mot tac tu soan cho sach da co): kiem
    tung ma co that trong sach goc, roi kiem ghep nhu tren.

VI SAO CO FILE NAY. Soan sai mot cau la con bi cham oan: bo phim khong go ra duoc dap
an dung, hay so sao khong khop luat. May khong bao gio biet, con thi chi thay "sai".
Nen moi loi o day la loi chan (ma thoat 1), con canh bao (phim nhieu it qua) chi in ra.

Luat so sanh chuoi o day phai giong het HocThuoc.chuanHoa ben app: sua mot ben thi sua
ca ben kia, khong thi file qua kiem o day ma app van cham sai.
"""

import argparse
import json
import os
import re
import sys
import unicodedata

GOC = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
THU_MUC_SACH = os.path.join(GOC, "app", "src", "main", "assets", "nganhang")
THU_MUC_HINH = os.path.join(GOC, "app", "src", "main", "assets", "hinh")

# Bo phim co ban luon co, theo mon. Giu dung thu tu ben app (Ghep.phimCoBan trong kho/Ghep.kt).
PHIM_CO_BAN = {
    "Toán": list("0123456789") + ["+", "−", "×", ":", "/", "=", "(", ")", ","],
    "Khoa học tự nhiên": list("0123456789") + ["+", "−", "·", ":", "/", "=", "(", ")", ",", "→"],
    "Tiếng Anh": [],
}

SAO_CO_DINH = {"CHON": 1, "DUNG_SAI": 1, "CHU": 2, "CAU": 2}
KIEU = {"CHON", "DUNG_SAI", "CHU", "CAU", "BIEU_THUC", "BUOC", "O"}
TRAN_SAO = 20

# ------------------------------------------------------------------ chuan hoa
# Chep tu HocThuoc.chuanHoa (app/src/main/java/.../data/HocThuoc.kt).
SO_MU = "⁰¹²³⁴⁵⁶⁷⁸⁹"
SO_DUOI = "₀₁₂₃₄₅₆₇₈₉"
CUM_MU = re.compile("[⁺⁻]?[⁰¹²³⁴⁵⁶⁷⁸⁹]+")
DAU_NHAN = "·×∙⋅*"
DAU_PHAY_TREN = "’‘ʼ′"


def doi_ky_tu(c):
    if c in SO_MU:
        return str(SO_MU.index(c))
    if c in SO_DUOI:
        return str(SO_DUOI.index(c))
    if c == "⁺":
        return "+"
    if c in "⁻−–—":
        return "-"
    if c in DAU_NHAN:
        return "."
    if c in ":÷":
        return "/"
    if c in DAU_PHAY_TREN:
        return "'"
    return c


def chuan_hoa(chu, giu_hoa=False, cat_cuoi=True):
    """cat_cuoi=False khi chuan hoa tung phim: app noi phim lai roi moi chuan hoa ca chuoi,
    nen dau cham cuoi chi cat o cuoi dap an. Cat o tung phim thi phim "·" (KHTN) va "×"
    (Toan), vi chuan hoa ra dau cham, thanh rong, va dap an "6,02·10²³" bi bao la khong ghep
    duoc tu bo phim du app van nhan (thay luc soan KHTN ngay 29/9/2026)."""
    s = unicodedata.normalize("NFC", chu)
    if not giu_hoa:
        s = s.lower()
    s = CUM_MU.sub(lambda m: "^" + "".join(doi_ky_tu(x) for x in m.group(0)), s)
    ra = []
    for c in s:
        if c.isspace() or unicodedata.category(c) == "Cf":
            continue
        ra.append(doi_ky_tu(c))
    ra = "".join(ra)
    return ra.rstrip(".!?") if cat_cuoi else ra


# ------------------------------------------------------------------ khong ke thu tu
# Chep tu Ghep.khongKeThuTu (app/src/main/java/.../kho/Ghep.kt). Hai cach viet chi khac thu
# tu hang tu (va, mon Toan, thu tu thua so trong don thuc) thi app cham la nhu nhau.
NOI_VE = "=→<>≤≥≠"
MU = re.compile(r"\^-?[0-9]+")
HE_SO = re.compile(r"[0-9]+(,[0-9]+)?(/[0-9]+(,[0-9]+)?)?(\^-?[0-9]+)?")
SO_GIUA = re.compile(r"[0-9]+(,[0-9]+)?(\^-?[0-9]+)?")


def tach_tang(s, cat):
    manh, noi, sau, tu = [], "", 0, 0
    for i, c in enumerate(s):
        if c == "(":
            sau += 1
        elif c == ")":
            sau -= 1
        if sau < 0:
            return None
        if sau == 0 and c in cat:
            manh.append(s[tu:i])
            noi += c
            tu = i + 1
    if sau != 0:
        return None
    manh.append(s[tu:])
    return manh, noi


def noi_lai(manh, noi):
    return "".join((noi[i - 1] if i > 0 else "") + m for i, m in enumerate(manh))


def khop_tai(r, s, tu):
    if tu >= len(s):
        return ""
    m = r.match(s, tu)
    return m.group(0) if m else ""


def chuan_hang(h, sap):
    thua, xep, i = [], sap, 0
    while i < len(h):
        c = h[i]
        if c == "(":
            sau, j = 0, i
            while j < len(h):
                if h[j] == "(":
                    sau += 1
                if h[j] == ")":
                    sau -= 1
                    if sau == 0:
                        break
                j += 1
            if j >= len(h):
                return None
            trong = khong_ke_thu_tu(h[i + 1:j], sap)
            if trong is None:
                return None
            mu = khop_tai(MU, h, j + 1)
            thua.append(f"({trong}){mu}")
            i = j + 1 + len(mu)
        elif "0" <= c <= "9":
            so = khop_tai(HE_SO if not thua else SO_GIUA, h, i)
            thua.append(so)
            i += len(so)
        elif "a" <= c <= "z":
            mu = khop_tai(MU, h, i + 1)
            thua.append(c + mu)
            i += 1 + len(mu)
        elif c == ".":
            i += 1
        else:
            xep = False
            thua.append(c)
            i += 1
    if not xep:
        return ".".join(thua)
    loai = lambda t: 0 if "0" <= t[0] <= "9" else (2 if t[0] == "(" else 1)
    return ".".join(sorted(thua, key=lambda t: (loai(t), t)))


def chuan_ve(v, sap):
    if not v:
        return v
    hang, sau, dau, tu = [], 0, "+", 0
    for i, c in enumerate(v):
        if c == "(":
            sau += 1
        elif c == ")":
            sau -= 1
        if sau != 0 or c not in "+-":
            continue
        if i == 0:
            dau, tu = c, 1
            continue
        if v[i - 1] in "^./":
            continue
        than = v[tu:i]
        if not than:
            return None
        h = chuan_hang(than, sap)
        if h is None:
            return None
        hang.append(dau + h)
        dau, tu = c, i + 1
    cuoi = v[tu:]
    if not cuoi:
        return None
    h = chuan_hang(cuoi, sap)
    if h is None:
        return None
    hang.append(dau + h)
    # Kotlin xep chuoi theo ma UTF-16, Python theo ma Unicode: giong nhau khi khong co ky tu
    # ngoai mat phang co ban, ma bieu thuc thi khong co.
    return "".join(sorted(hang))


def khong_ke_thu_tu(s, sap):
    t = tach_tang(s, ";")
    if t is None:
        return None
    doan, cham = t
    gon = []
    for d in doan:
        t2 = tach_tang(d, NOI_VE)
        if t2 is None:
            return None
        ve, noi = t2
        cac = []
        for v in ve:
            x = chuan_ve(v, sap)
            if x is None:
                return None
            cac.append(x)
        gon.append(noi_lai(cac, noi))
    return noi_lai(gon, cham)


def bieu_thuc_bang(a, b, mon, thu_tu=False):
    """Hai cach viet co duoc app cham la nhu nhau khong. Giong Ghep.BieuThuc.dung."""
    x, y = chuan_hoa(a, giu_hoa=True), chuan_hoa(b, giu_hoa=True)
    if not x or not y:
        return False
    if x == y:
        return True
    if thu_tu:
        return False
    sap = mon == "Toán"
    gx = khong_ke_thu_tu(x, sap)
    return gx is not None and gx == khong_ke_thu_tu(y, sap)


def gon_cach(chu):
    """Thu gon dau cach va bo dau cach truoc dau cau, nhu man ghep cau ben app."""
    s = re.sub(r"\s+", " ", unicodedata.normalize("NFC", chu)).strip()
    return re.sub(r" ([,.;:!?])", r"\1", s)


# ------------------------------------------------------------------ bao loi
class So:
    def __init__(self):
        self.loi = []
        self.canh_bao = []

    def l(self, ma, chu):
        self.loi.append(f"[{ma}] {chu}")

    def c(self, ma, chu):
        self.canh_bao.append(f"[{ma}] {chu}")


def the_can_bang(chu):
    """<u> va <b> dong mo du cap, khong long nhau sai."""
    ngan = []
    for m in re.finditer(r"</?([ub])>", chu):
        the = m.group(1)
        if m.group(0).startswith("</"):
            if not ngan or ngan[-1] != the:
                return False
            ngan.pop()
        else:
            ngan.append(the)
    return not ngan


def bo_the(chu):
    return re.sub(r"</?[ub]>", "", chu)


def kiem_chu_hien(so, ma, ten, chu):
    if not isinstance(chu, str) or not chu.strip():
        so.l(ma, f"{ten} rỗng")
        return
    if not the_can_bang(chu):
        so.l(ma, f"{ten} có thẻ <u>/<b> không đủ cặp: {chu!r}")
    if re.search(r"<(?!/?[ub]>)", chu):
        so.c(ma, f"{ten} có dấu < lạ (chỉ dùng <u>, <b>): {chu!r}")


# ------------------------------------------------------------------ tung kieu
def ghep_duoc_phim(dap_chuan, phim_chuan):
    """dap_chuan chia duoc thanh day cac phim (da chuan hoa) khong. Phim dung lai duoc."""
    n = len(dap_chuan)
    duoc = [False] * (n + 1)
    duoc[0] = True
    for i in range(n):
        if not duoc[i]:
            continue
        for p in phim_chuan:
            if p and dap_chuan.startswith(p, i):
                duoc[i + len(p)] = True
    return duoc[n]


def ghep_duoc_the(cau, the):
    """cau ghep duoc tu cac the (moi the mot lan, cach nhau mot dau cach) khong."""
    dung = [False] * len(the)

    def thu(i):
        if i == len(cau):
            return True
        for k, t in enumerate(the):
            if dung[k] or not t:
                continue
            if cau.startswith(t, i):
                j = i + len(t)
                if j == len(cau):
                    dung[k] = True
                    if thu(j):
                        return True
                    dung[k] = False
                elif cau[j] == " ":
                    dung[k] = True
                    if thu(j + 1):
                        return True
                    dung[k] = False
        return False

    return thu(0)


# Tu 30/9/2026 app tron phuong an trac nghiem moi luot va danh lai chu A, B, C (xem KhungGhep).
# Phuong an nhac chu cai hay vi tri cua phuong an khac thi sai nghia sau khi tron.
NHAC_CHU_CAI = re.compile(r"\b[A-F]\s*(?:và|hoặc|hay|,|and|or|&)\s*[A-F]\b")
NHAC_VI_TRI = re.compile(
    r"tất cả|cả hai|các (?:phương án|đáp án|ý|câu) trên|all of the above|none of the above"
    r"|both of the above", re.I)
# Chu hien tren may ma con in san "A. ... B. ...": chu cai tren de se lech voi nut sau khi tron.
IN_PHUONG_AN = re.compile(r"(?:^|\s)A[.)]\s.*(?:^|\s)B[.)]\s", re.S)


def kiem_chon(so, ma, g):
    cac = g.get("cac")
    if not isinstance(cac, list) or not 2 <= len(cac) <= 6:
        so.l(ma, "CHON: 'cac' phải có 2 tới 6 phương án")
        return
    for i, x in enumerate(cac):
        kiem_chu_hien(so, ma, f"cac[{i}]", x)
        if isinstance(x, str) and re.match(r"^\s*[A-F][.)]\s", x):
            so.l(ma, f"CHON: phương án không kèm chữ 'A.' ở đầu: {x!r}")
        if isinstance(x, str) and (NHAC_CHU_CAI.search(bo_the(x)) or NHAC_VI_TRI.search(bo_the(x))):
            so.c(ma, f"CHON: phương án nhắc tới phương án khác, máy trộn thứ tự nên sẽ sai nghĩa: {x!r}")
    if len(set(bo_the(x).strip() for x in cac if isinstance(x, str))) != len(cac):
        so.l(ma, "CHON: có hai phương án giống nhau")
    dap = g.get("dap")
    cuoi = chr(ord("A") + len(cac) - 1)
    nhieu_dap = g.get("nhieu_dap", False)
    if not isinstance(nhieu_dap, bool):
        so.l(ma, "CHON: 'nhieu_dap' phải là true hoặc false")
    if isinstance(dap, list):
        if not nhieu_dap:
            so.l(ma, "CHON: 'dap' là danh sách thì phải bật 'nhieu_dap'")
        if not dap or len(set(dap)) != len(dap) or not all(
                isinstance(x, str) and len(x) == 1 and "A" <= x <= cuoi for x in dap):
            so.l(ma, f"CHON: 'dap' phải là các chữ cái khác nhau trong khoảng phương án, đang là {dap!r}")
    elif not isinstance(dap, str) or len(dap) != 1 or not "A" <= dap <= cuoi:
        so.l(ma, f"CHON: 'dap' phải là một chữ cái trong khoảng phương án, đang là {dap!r}")


def kiem_dung_sai(so, ma, g):
    if not isinstance(g.get("dap"), bool):
        so.l(ma, "DUNG_SAI: 'dap' phải là true hoặc false")
    nhan = g.get("nhan")
    if nhan is not None and (not isinstance(nhan, list) or len(nhan) != 2):
        so.l(ma, "DUNG_SAI: 'nhan' phải có đúng hai nhãn")


def kiem_chu(so, ma, g):
    dap = g.get("dap")
    if not isinstance(dap, list) or not dap or not all(isinstance(x, str) and x.strip() for x in dap):
        so.l(ma, "CHU: 'dap' phải là danh sách chữ không rỗng")
        return
    truoc = g.get("truoc", "") or ""
    chu_that = set()
    for d in dap:
        if truoc and not d.lower().startswith(truoc.lower()):
            so.l(ma, f"CHU: đáp án {d!r} không bắt đầu bằng phần cho sẵn {truoc!r}")
            continue
        phan = d[len(truoc):].lower() if truoc else d.lower()
        chu_that |= {c for c in phan if not c.isspace()}
    nhieu = g.get("nhieu", [])
    if not isinstance(nhieu, list):
        so.l(ma, "CHU: 'nhieu' phải là danh sách")
        return
    nhieu_thuong = [str(x).lower() for x in nhieu]
    for x in nhieu_thuong:
        if len(x) != 1:
            so.l(ma, f"CHU: phím nhiễu phải là một ký tự: {x!r}")
        elif x in chu_that:
            so.l(ma, f"CHU: phím nhiễu {x!r} trùng chữ có trong đáp án")
    if len(set(nhieu_thuong)) < 2:
        so.l(ma, "CHU: cần ít nhất 2 phím nhiễu")
    elif len(set(nhieu_thuong)) < min(4, len(chu_that)):
        so.c(ma, f"CHU: phím nhiễu ít ({len(set(nhieu_thuong))}) so với {len(chu_that)} chữ thật")


def kiem_cau(so, ma, g):
    the = g.get("the")
    dap = g.get("dap")
    if not isinstance(the, list) or len(the) < 2 or not all(isinstance(t, str) and t.strip() for t in the):
        so.l(ma, "CAU: 'the' phải có ít nhất 2 thẻ không rỗng")
        return
    if not isinstance(dap, list) or not dap:
        so.l(ma, "CAU: 'dap' phải có ít nhất một câu")
        return
    the_gon = [gon_cach(t) for t in the]
    if len(set(the_gon)) != len(the_gon):
        so.c(ma, "CAU: có hai thẻ giống hệt nhau (được, nhưng xem lại)")
    for d in dap:
        if not ghep_duoc_the(gon_cach(d), the_gon):
            so.l(ma, f"CAU: đáp án {d!r} không ghép được từ các thẻ")
    nhieu = g.get("nhieu", [])
    if not isinstance(nhieu, list) or not 1 <= len(nhieu) <= 3:
        so.l(ma, "CAU: cần 1 tới 3 thẻ nhiễu")
    else:
        for x in nhieu:
            if gon_cach(str(x)) in the_gon:
                so.l(ma, f"CAU: thẻ nhiễu {x!r} trùng một thẻ thật")


def kiem_bieu_thuc(so, ma, g, mon):
    co_ban = PHIM_CO_BAN.get(mon)
    if co_ban is None:
        so.l(ma, f"BIEU_THUC: môn {mon!r} chưa có bộ phím cơ bản")
        co_ban = []
    phim = g.get("phim", [])
    nhieu = g.get("nhieu", [])
    dap = g.get("dap")
    loi_giai = g.get("loi_giai")
    if not isinstance(phim, list) or not isinstance(nhieu, list):
        so.l(ma, "BIEU_THUC: 'phim' và 'nhieu' phải là danh sách")
        return
    for p in phim:
        if not isinstance(p, str) or not p.strip():
            so.l(ma, f"BIEU_THUC: phím rỗng trong 'phim'")
        elif p in co_ban:
            so.l(ma, f"BIEU_THUC: {p!r} đã có trong bộ phím cơ bản, bỏ khỏi 'phim'")
    tat_ca = [chuan_hoa(p, giu_hoa=True, cat_cuoi=False) for p in co_ban + phim if isinstance(p, str) and p.strip()]
    for x in nhieu:
        if not isinstance(x, str) or not x.strip():
            so.l(ma, "BIEU_THUC: phím nhiễu rỗng")
        elif x in co_ban:
            so.l(ma, f"BIEU_THUC: phím nhiễu {x!r} nằm trong bộ phím cơ bản")
        elif x in phim:
            so.l(ma, f"BIEU_THUC: phím nhiễu {x!r} trùng phím thật")
    if phim and len(nhieu) < 1:
        so.l(ma, "BIEU_THUC: có 'phim' thì cần ít nhất một phím nhiễu")
    elif phim and len(nhieu) < min(len(phim), 3):
        so.c(ma, f"BIEU_THUC: phím nhiễu ít ({len(nhieu)}) so với {len(phim)} phím thật")
    thu_tu = g.get("thu_tu", False)
    if not isinstance(thu_tu, bool):
        so.l(ma, "BIEU_THUC: 'thu_tu' phải là true hoặc false")
        thu_tu = False
    if not isinstance(dap, list) or not dap:
        so.l(ma, "BIEU_THUC: 'dap' phải có ít nhất một dạng đúng")
    else:
        # Dang chi khac thu tu hang tu thi app da nhan, liet ke them chi lam file dai.
        chuoi = [d for d in dap if isinstance(d, str) and d.strip()]
        for i, a in enumerate(chuoi):
            for b in chuoi[:i]:
                if bieu_thuc_bang(a, b, mon, thu_tu):
                    so.c(ma, f"BIEU_THUC: {a!r} chỉ khác {b!r} ở thứ tự, app đã tự nhận, bỏ bớt")
                    break
        for d in dap:
            if not isinstance(d, str) or not d.strip():
                so.l(ma, "BIEU_THUC: đáp án rỗng")
                continue
            # Giu hoa thuong: "Na" va "na" la hai phim khac nhau.
            if not ghep_duoc_phim(chuan_hoa(d, giu_hoa=True), tat_ca):
                so.l(ma, f"BIEU_THUC: đáp án {d!r} không ghép được từ bộ phím")
            if "⟶" in d:
                so.l(ma, f"BIEU_THUC: dùng mũi tên → thay cho ⟶ trong {d!r}")
    if not isinstance(loi_giai, list) or not loi_giai:
        so.l(ma, "BIEU_THUC: 'loi_giai' phải có ít nhất một dòng")
        return
    for i, x in enumerate(loi_giai):
        kiem_chu_hien(so, ma, f"loi_giai[{i}]", x)
    if g.get("sao") != min(len(loi_giai), TRAN_SAO):
        so.l(ma, f"BIEU_THUC: 'sao' phải bằng số dòng lời giải ({min(len(loi_giai), TRAN_SAO)}), đang là {g.get('sao')}")
    if len(loi_giai) > TRAN_SAO:
        so.l(ma, f"BIEU_THUC: lời giải quá {TRAN_SAO} dòng")


def kiem_buoc(so, ma, g):
    buoc = g.get("buoc")
    if not isinstance(buoc, list) or not 2 <= len(buoc) <= TRAN_SAO:
        so.l(ma, f"BUOC: 'buoc' phải có 2 tới {TRAN_SAO} dòng")
        return
    for i, x in enumerate(buoc):
        kiem_chu_hien(so, ma, f"buoc[{i}]", x)
    if len(set(gon_cach(x) for x in buoc)) != len(buoc):
        so.l(ma, "BUOC: có hai bước giống hệt nhau, máy không phân biệt được thứ tự")
    nhieu = g.get("nhieu", [])
    if not isinstance(nhieu, list) or not 1 <= len(nhieu) <= 3:
        so.l(ma, "BUOC: cần 1 tới 3 dòng nhiễu")
    else:
        for x in nhieu:
            kiem_chu_hien(so, ma, "nhieu", x)
            if gon_cach(str(x)) in {gon_cach(b) for b in buoc}:
                so.l(ma, f"BUOC: dòng nhiễu trùng một bước thật: {x!r}")
    for t in g.get("thu_tu_khac", []) or []:
        if sorted(t) != list(range(len(buoc))):
            so.l(ma, f"BUOC: thứ tự khác {t} không phải hoán vị của 0..{len(buoc) - 1}")
        elif t == list(range(len(buoc))):
            so.l(ma, "BUOC: thứ tự khác trùng thứ tự chính")
    if g.get("sao") != len(buoc):
        so.l(ma, f"BUOC: 'sao' phải bằng số bước ({len(buoc)}), đang là {g.get('sao')}")


GIOI = re.compile(r"\{(He|he|His|his|Him|him)\}")


def kiem_o(so, ma, g):
    dong = g.get("dong")
    if not isinstance(dong, list) or not dong:
        so.l(ma, "O: 'dong' phải có ít nhất một dòng")
        return
    so_o = 0
    co_gioi = False
    for i, d in enumerate(dong):
        chu = d.get("chu") if isinstance(d, dict) else None
        o = d.get("o") if isinstance(d, dict) else None
        if not isinstance(chu, str) or not isinstance(o, list) or not o:
            so.l(ma, f"O: dòng {i} thiếu 'chu' hay 'o'")
            continue
        kiem_chu_hien(so, ma, f"dong[{i}].chu", chu)
        cho = sorted(set(int(x) for x in re.findall(r"\{(\d+)\}", chu)))
        if cho != list(range(len(o))):
            so.l(ma, f"O: dòng {i} có chỗ {cho} không khớp {len(o)} ô")
        if GIOI.search(chu):
            co_gioi = True
        for k, x in enumerate(o):
            if not isinstance(x, dict):
                so.l(ma, f"O: dòng {i} ô {k} không phải object")
                continue
            if x.get("go"):
                if not isinstance(x.get("goi_y", ""), str):
                    so.l(ma, f"O: dòng {i} ô {k} 'goi_y' phải là chữ")
                continue
            dung = x.get("dung")
            sai = x.get("sai", [])
            if not isinstance(dung, list) or not dung:
                so.l(ma, f"O: dòng {i} ô {k} thiếu phương án đúng")
                continue
            if not isinstance(sai, list):
                so.l(ma, f"O: dòng {i} ô {k} 'sai' phải là danh sách")
                continue
            for p in dung + sai:
                if GIOI.search(str(p)):
                    co_gioi = True
            if set(map(gon_cach, dung)) & set(map(gon_cach, sai)):
                so.l(ma, f"O: dòng {i} ô {k} có phương án vừa đúng vừa sai")
            # It nhat 3 nut moi o (Ba Huy chot 30/9/2026): hai nut la doan mo trung mot nua.
            # Ngoai le la o von chi co hai gia tri (Dung/Sai, Co/Khong, hai cot, hai vat dem
            # so), ghi "hai": true de nguoi soan phai tu quyet chu khong lot vi quen them.
            n = len(dung) + len(sai)
            hai = x.get("hai", False)
            if n < 2:
                so.l(ma, f"O: dòng {i} ô {k} chỉ có một phương án, không có gì để chọn")
            elif not isinstance(hai, bool):
                so.l(ma, f"O: dòng {i} ô {k} 'hai' phải là true hay false")
            elif hai and n != 2:
                so.l(ma, f"O: dòng {i} ô {k} ghi 'hai' mà có {n} nút; 'hai' chỉ dùng cho ô đúng hai nút")
            elif not hai and n < 3:
                so.l(ma, f"O: dòng {i} ô {k} chỉ có {n} nút, cần ít nhất 3: thêm phương án sai, "
                         "hoặc ghi \"hai\": true nếu ô vốn chỉ có hai giá trị (Đúng/Sai, Có/Không, hai cột)")
            so_o += 1
    if co_gioi and not g.get("gioi"):
        so.l(ma, "O: có {He}/{his}... mà không bật 'gioi'")
    if so_o == 0:
        so.l(ma, "O: không có ô chọn nào")
    if g.get("sao") != min(so_o, TRAN_SAO):
        so.l(ma, f"O: 'sao' phải bằng số ô chọn ({so_o}), đang là {g.get('sao')}")
    if so_o > TRAN_SAO:
        so.l(ma, f"O: quá {TRAN_SAO} ô chọn, tách thành nhiều câu")


def kiem_ghep(so, ma, g, mon):
    if not isinstance(g, dict):
        so.l(ma, "'ghep' phải là object")
        return
    kieu = g.get("kieu")
    if kieu not in KIEU:
        so.l(ma, f"kiểu lạ {kieu!r}")
        return
    sao = g.get("sao")
    if not isinstance(sao, int) or not 1 <= sao <= TRAN_SAO:
        so.l(ma, f"'sao' phải là số nguyên 1 tới {TRAN_SAO}")
    if kieu in SAO_CO_DINH and sao != SAO_CO_DINH[kieu]:
        so.l(ma, f"{kieu} luôn {SAO_CO_DINH[kieu]} sao, đang là {sao}")
    if "hoi" in g:
        kiem_chu_hien(so, ma, "hoi", g["hoi"])
    if kieu == "CHON":
        kiem_chon(so, ma, g)
    elif kieu == "DUNG_SAI":
        kiem_dung_sai(so, ma, g)
    elif kieu == "CHU":
        kiem_chu(so, ma, g)
    elif kieu == "CAU":
        kiem_cau(so, ma, g)
    elif kieu == "BIEU_THUC":
        kiem_bieu_thuc(so, ma, g, mon)
    elif kieu == "BUOC":
        kiem_buoc(so, ma, g)
    elif kieu == "O":
        kiem_o(so, ma, g)


def kiem_cau_hoi(so, c, mon, doan_van, hinh_goc):
    ma = c.get("ma", "?")
    g = c.get("ghep")
    bo = c.get("bo_may")
    if g is not None and bo is not None:
        so.l(ma, "vừa có 'ghep' vừa có 'bo_may'")
    cho = c.get("ghep_cho_hinh")
    if cho is not None:
        # Cau can hinh ma chua co anh: ghep soan xong, gop.py cat sang day (xem gop.py).
        if g is not None:
            so.l(ma, "vừa có 'ghep' vừa có 'ghep_cho_hinh'")
        if not str(bo or "").startswith("chờ hình"):
            so.l(ma, "có 'ghep_cho_hinh' thì 'bo_may' phải bắt đầu bằng 'chờ hình'")
        kiem_ghep(so, ma, cho, mon)
    if g is not None:
        kiem_ghep(so, ma, g, mon)
        # App hien 'hoi', thieu thi hien 'de' (KhungGhep.veDe).
        hien = g.get("hoi") or c.get("de") or ""
        if g.get("kieu") == "CHON" and isinstance(hien, str) and IN_PHUONG_AN.search(hien):
            so.c(ma, "CHON: chữ hiện trên máy còn in 'A. … B. …', máy trộn phương án nên chữ cái sẽ "
                     "lệch với nút; thêm 'hoi' bỏ các phương án")
        if g.get("kieu") == "CHON" and c.get("dang") == "TRAC_NGHIEM" and c.get("dap_an"):
            d = g.get("dap")
            tn = sorted(set(re.findall(r"\b[A-F]\b", c["dap_an"]))) if isinstance(d, list) else c["dap_an"].strip()
            if c.get("loai_dap_an") == "TN" and tn != (sorted(d) if isinstance(d, list) else d):
                so.l(ma, f"CHON: 'dap' {d!r} khác 'dap_an' {c['dap_an']!r} của câu")
    for h in c.get("hinh", []) or []:
        if not re.match(r"^[A-Za-z0-9._/-]+$", h):
            so.l(ma, f"tên ảnh có ký tự lạ: {h!r}")
        elif hinh_goc and not os.path.isfile(os.path.join(hinh_goc, h)):
            so.l(ma, f"không thấy ảnh {h} trong {hinh_goc}")
    d = c.get("doan")
    if d is not None and d not in doan_van:
        so.l(ma, f"đoạn văn {d!r} không có trong 'doan_van'")


def kiem_de_thi(so, b, dt, ma_de):
    """Bai la mot de thi in san (tu 30/9/2026, xem DINH_DANG.md): kiem khung de."""
    ma = dt.get("ma") if isinstance(dt, dict) else None
    ten = b.get("bai", "?")
    if not isinstance(ma, str) or not ma.strip():
        so.l(ten, "'de_thi' thiếu 'ma'")
        return
    if ma in ma_de:
        so.l(ma, "mã đề thi trùng")
    ma_de.add(ma)
    den = dt.get("den_unit")
    if not isinstance(den, int) or not 1 <= den <= 12:
        so.l(ma, f"'den_unit' phải là số Unit 1 tới 12, đang là {den!r}")
    phut = dt.get("phut")
    if not isinstance(phut, int) or not 10 <= phut <= 120:
        so.l(ma, f"'phut' phải là số phút 10 tới 120, đang là {phut!r}")
    truoc = 0
    for c in b.get("cac_cau", []):
        mc = c.get("ma", "?")
        if not str(mc).startswith(ma + "."):
            so.l(mc, f"câu của đề {ma} phải có mã bắt đầu bằng '{ma}.'")
        so_in = re.match(r"^(\d+)", str(mc).rsplit(".", 1)[-1])
        if not so_in:
            so.l(mc, "mã câu đề thi phải kết bằng số câu in trên đề")
        elif int(so_in.group(1)) <= truoc:
            so.l(mc, "số câu in phải tăng dần theo thứ tự trong đề")
        else:
            truoc = int(so_in.group(1))
        if not str(c.get("nhom", "")).strip():
            so.c(mc, "câu đề thi thiếu 'nhom' (lời dẫn của bài), máy không in được đầu phần")
    if not any("ghep" in c or "trung" in c for c in b.get("cac_cau", [])):
        so.l(ma, "đề thi không có câu nào làm được trên máy")


def kiem_sach(duong, hinh_goc):
    so = So()
    o = json.load(open(duong, encoding="utf-8"))
    mon = o.get("mon", "")
    doan_van = o.get("doan_van", {}) or {}
    ma_da_gap = set()
    ma_de = set()
    # Cau "trung" (de thi): tro ve cau cua de khac trong cung file, dung chung id cau do.
    ma_co_noi_dung = {c.get("ma") for b in o.get("cac_bai", []) for c in b.get("cac_cau", [])
                      if "trung" not in c and "ghep" in c}
    for b in o.get("cac_bai", []):
        if "de_thi" in b:
            kiem_de_thi(so, b, b["de_thi"], ma_de)
        for c in b.get("cac_cau", []):
            ma = c.get("ma", "?")
            if ma in ma_da_gap:
                so.l(ma, "mã câu trùng")
            ma_da_gap.add(ma)
            if "trung" in c:
                if "de_thi" not in b:
                    so.l(ma, "'trung' chỉ dùng cho câu của đề thi")
                if c["trung"] not in ma_co_noi_dung:
                    so.l(ma, f"'trung' trỏ tới {c['trung']!r}, không phải câu có ghép trong file")
                for k in ("de", "ghep", "bo_may", "doan", "hinh"):
                    if k in c:
                        so.l(ma, f"câu 'trung' lấy nội dung của câu gốc, không được có '{k}'")
                continue
            kiem_chu_hien(so, ma, "de", c.get("de", ""))
            kiem_cau_hoi(so, c, mon, doan_van, hinh_goc)
    for k, v in doan_van.items():
        kiem_chu_hien(so, k, "doan_van", v)
    return so


def kiem_phan(duong, duong_sach, hinh_goc):
    so = So()
    o = json.load(open(duong, encoding="utf-8"))
    nguon = o.get("nguon", "")
    if not duong_sach:
        duong_sach = os.path.join(THU_MUC_SACH, f"{nguon}.json")
    sach = json.load(open(duong_sach, encoding="utf-8"))
    if sach.get("nguon") != nguon:
        so.l("-", f"file phần ghi nguồn {nguon!r} mà sách gốc là {sach.get('nguon')!r}")
    mon = sach.get("mon", "")
    cau_goc = {c["ma"]: c for b in sach.get("cac_bai", []) for c in b.get("cac_cau", [])}
    ghep = o.get("ghep", {}) or {}
    bo = o.get("bo_may", {}) or {}
    hinh = o.get("hinh", {}) or {}
    # Cau can hinh ma chua cat (Toan, KHTN cat hinh sau cung): ma -> loi ta hinh.
    can_hinh = o.get("can_hinh", {}) or {}
    for ma, ta in can_hinh.items():
        if not isinstance(ta, str) or not ta.strip():
            so.l(ma, "'can_hinh' phải tả hình cần cắt bằng chữ")
        if ma not in ghep:
            so.l(ma, "có trong 'can_hinh' mà không có trong 'ghep'")
    for ma in list(ghep) + list(bo) + list(hinh) + list(can_hinh):
        if ma not in cau_goc:
            so.l(ma, "mã này không có trong sách gốc")
    for ma in set(ghep) & set(bo):
        so.l(ma, "vừa có trong 'ghep' vừa có trong 'bo_may'")
    for ma, g in ghep.items():
        if ma not in cau_goc:
            continue
        c = dict(cau_goc[ma])
        c["ghep"] = g
        if ma in hinh:
            c["hinh"] = hinh[ma]
        kiem_cau_hoi(so, c, mon, sach.get("doan_van", {}) or {}, hinh_goc)
    return so


def main():
    ap = argparse.ArgumentParser(description="Kiểm dữ liệu ghép")
    ap.add_argument("file", nargs="*")
    ap.add_argument("--sach", help="sách gốc cho file phần (mặc định tìm theo nguồn)")
    ap.add_argument("--hinh", default=THU_MUC_HINH, help="thư mục gốc của ảnh")
    ap.add_argument("--tat-ca", action="store_true", help="kiểm mọi file trong assets/nganhang")
    a = ap.parse_args()
    cac = list(a.file)
    if a.tat_ca:
        cac += sorted(os.path.join(THU_MUC_SACH, f) for f in os.listdir(THU_MUC_SACH) if f.endswith(".json"))
    if not cac:
        ap.error("cần ít nhất một file")
    tong_loi = 0
    for f in cac:
        o = json.load(open(f, encoding="utf-8"))
        so = kiem_sach(f, a.hinh) if "cac_bai" in o else kiem_phan(f, a.sach, a.hinh)
        so_ghep = 0
        if "cac_bai" in o:
            so_ghep = sum(1 for b in o["cac_bai"] for c in b.get("cac_cau", []) if "ghep" in c)
        else:
            so_ghep = len(o.get("ghep", {}))
        print(f"{os.path.basename(f)}: {so_ghep} câu có ghép, {len(so.loi)} lỗi, {len(so.canh_bao)} cảnh báo")
        for x in so.loi:
            print("  LỖI", x)
        for x in so.canh_bao:
            print("  lưu ý", x)
        tong_loi += len(so.loi)
    sys.exit(1 if tong_loi else 0)


if __name__ == "__main__":
    main()
