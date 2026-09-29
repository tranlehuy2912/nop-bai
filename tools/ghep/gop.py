#!/usr/bin/env python3
"""
Gop du lieu ghep vao cac file sach trong app/src/main/assets/nganhang. Dinh dang o
DINH_DANG.md, kiem bang kiem.py.

Hai viec:

1. File phan -> sach da co (SBT Toan, SGK Toan, SBT KHTN, SGK KHTN):
       python3 tools/ghep/gop.py phan chuong1.json chuong2.json ...
   Moi file phan co "nguon" va cac bang "ghep", "bo_may", "hinh", "can_hinh" (ma cau ->
   gia tri). Cau co trong can_hinh ma chua co hinh thi chua giao duoc: ghep cat vao
   "ghep_cho_hinh", cau ghi bo_may "chờ hình: <lời tả>". Co hinh roi thi chay lai gop voi
   bang "hinh" cua cau do, ghep_cho_hinh tu ve lai "ghep".

2. Cac file Unit -> mot sach moi (SBT Tieng Anh):
       python3 tools/ghep/gop.py sach sbtanh8 u01.json u02.json ... ty1.json ...
   Moi file Unit la mot sach nho (nguon, mon, ten, ban, doan_van, cac_bai). Cac bai xep
   theo thu tu trong sach: Unit 1, 2, 3, Test Yourself 1, Unit 4...

Ca hai viec:
  - tang "ban" cua sach len mot, vi app chi nap lai khi so ban doi (NganHang.napNeuCan);
  - giu "ban" dung truoc "cac_bai": NganHang.docBan doc so ban tu dau file roi dung;
  - bo cac dang BIEU_THUC chi khac thu tu hang tu (app da tu nhan, xem kiem.py);
  - --hinh-tu <thu muc>: chep anh ma cau dung toi tu thu muc do vao assets/hinh;
  - ghi file dung kieu cu (indent 1, giu chu co dau), roi chay kiem.py tren file do.

Khong ghi gi khi co loi: kiem.py bao loi thi file sach cu duoc giu nguyen.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile

GOC = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
THU_MUC_SACH = os.path.join(GOC, "app", "src", "main", "assets", "nganhang")
THU_MUC_HINH = os.path.join(GOC, "app", "src", "main", "assets", "hinh")
KIEM = os.path.join(os.path.dirname(os.path.abspath(__file__)), "kiem.py")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kiem  # noqa: E402

CHO_HINH = "chờ hình"


def doc(duong):
    with open(duong, encoding="utf-8") as f:
        return json.load(f)


def ghi(duong, sach):
    # Giu "ban" truoc "cac_bai", va cac khoa khac theo thu tu cu.
    thu_tu = ["nguon", "mon", "ten", "ban", "doan_van", "cac_bai"]
    ra = {k: sach[k] for k in thu_tu if k in sach}
    for k, v in sach.items():
        if k not in ra:
            ra[k] = v
    if "cac_bai" in ra:
        ra["cac_bai"] = ra.pop("cac_bai")
    with open(duong, "w", encoding="utf-8") as f:
        json.dump(ra, f, ensure_ascii=False, indent=1)
        f.write("\n")


def bo_dang_thua(g, mon):
    """BIEU_THUC: bo dang chi khac thu tu hang tu voi mot dang dung truoc no."""
    if not isinstance(g, dict) or g.get("kieu") != "BIEU_THUC" or g.get("thu_tu"):
        return g
    giu = []
    for d in g.get("dap", []):
        if not any(kiem.bieu_thuc_bang(d, x, mon) for x in giu):
            giu.append(d)
    if len(giu) != len(g.get("dap", [])):
        g = dict(g)
        g["dap"] = giu
    return g


def chep_hinh(cac_hinh, hinh_tu):
    thieu = []
    for h in cac_hinh:
        dich = os.path.join(THU_MUC_HINH, h)
        nguon = os.path.join(hinh_tu, h) if hinh_tu else None
        if nguon and os.path.isfile(nguon):
            os.makedirs(os.path.dirname(dich), exist_ok=True)
            shutil.copyfile(nguon, dich)
        elif not os.path.isfile(dich):
            thieu.append(h)
    return thieu


def kiem_file(duong):
    r = subprocess.run([sys.executable, KIEM, duong], capture_output=True, text=True)
    return r.returncode, (r.stdout + r.stderr).strip()


def ap_cau(c, ma, ghep, bo, hinh, can_hinh, mon):
    """Ap mot ma cau cua file phan vao cau c trong sach. Tra ve ten viec da lam."""
    if ma in hinh:
        c["hinh"] = hinh[ma]
    if ma in bo:
        c.pop("ghep", None)
        c.pop("ghep_cho_hinh", None)
        c["bo_may"] = bo[ma]
        return "bo_may"
    g = ghep.get(ma)
    if g is None and "ghep_cho_hinh" in c and c.get("hinh"):
        # Lan gop truoc cau nay cho hinh, lan nay moi co hinh: dua ghep ve cho cu.
        g = c["ghep_cho_hinh"]
    if g is None:
        return None
    g = bo_dang_thua(g, mon)
    if ma in can_hinh and not c.get("hinh"):
        c.pop("ghep", None)
        c["ghep_cho_hinh"] = g
        c["bo_may"] = f"{CHO_HINH}: {can_hinh[ma].strip()}"
        return "cho_hinh"
    c.pop("ghep_cho_hinh", None)
    if str(c.get("bo_may", "")).startswith(CHO_HINH):
        c.pop("bo_may", None)
    c["ghep"] = g
    return "ghep"


def gop_phan(cac_file, hinh_tu, thu):
    theo_nguon = {}
    for f in cac_file:
        p = doc(f)
        theo_nguon.setdefault(p["nguon"], []).append((f, p))
    loi = 0
    for nguon, cac in theo_nguon.items():
        duong = os.path.join(THU_MUC_SACH, f"{nguon}.json")
        sach = doc(duong)
        mon = sach.get("mon", "")
        cau = {c["ma"]: c for b in sach["cac_bai"] for c in b["cac_cau"]}
        dem = {}
        cac_hinh = []
        for f, p in cac:
            ghep = p.get("ghep", {}) or {}
            bo = p.get("bo_may", {}) or {}
            hinh = p.get("hinh", {}) or {}
            can_hinh = p.get("can_hinh", {}) or {}
            for ma in set(ghep) | set(bo) | set(hinh) | set(can_hinh):
                if ma not in cau:
                    print(f"  LỖI {os.path.basename(f)}: mã {ma!r} không có trong {nguon}")
                    loi += 1
                    continue
                viec = ap_cau(cau[ma], ma, ghep, bo, hinh, can_hinh, mon)
                if viec:
                    dem[viec] = dem.get(viec, 0) + 1
            for ds in hinh.values():
                cac_hinh += ds
        if loi:
            continue
        sach["ban"] = int(sach.get("ban", 1)) + 1
        if not ghi_va_kiem(duong, sach, thu):
            loi += 1
            continue
        thieu = [] if thu else chep_hinh(cac_hinh, hinh_tu)
        for h in thieu:
            print(f"  LỖI {nguon}: thiếu ảnh {h}")
        loi += len(thieu)
        print(f"{nguon}: bản {sach['ban']}, " + ", ".join(f"{k} {v}" for k, v in sorted(dem.items())))
    return loi


def khoa_bai(ten):
    m = re.match(r"^Unit (\d+)", ten)
    if m:
        return (int(m.group(1)), 0)
    m = re.match(r"^Test Yourself (\d+)", ten)
    if m:
        return (3 * int(m.group(1)), 1)
    return (99, 0)


def gop_sach(nguon, cac_file, hinh_tu, thu, ten, mon):
    duong = os.path.join(THU_MUC_SACH, f"{nguon}.json")
    ban_cu = doc(duong).get("ban", 0) if os.path.isfile(duong) else 0
    cac_bai, doan_van, loi = [], {}, 0
    for f in cac_file:
        p = doc(f)
        if p.get("nguon") != nguon:
            print(f"  LỖI {os.path.basename(f)}: nguồn {p.get('nguon')!r} khác {nguon!r}")
            loi += 1
            continue
        mon = mon or p.get("mon")
        ten = ten or p.get("ten")
        for k, v in (p.get("doan_van") or {}).items():
            if k in doan_van and doan_van[k] != v:
                print(f"  LỖI {os.path.basename(f)}: đoạn văn {k!r} trùng mã mà khác chữ")
                loi += 1
            doan_van[k] = v
        cac_bai += p.get("cac_bai", [])
    ten_bai = [b["bai"] for b in cac_bai]
    for t in set(ten_bai):
        if ten_bai.count(t) > 1:
            print(f"  LỖI bài {t!r} có trong hai file")
            loi += 1
    if loi:
        return loi
    cac_bai.sort(key=lambda b: khoa_bai(b["bai"]))
    for b in cac_bai:
        for c in b["cac_cau"]:
            if "ghep" in c:
                c["ghep"] = bo_dang_thua(c["ghep"], mon)
    sach = {"nguon": nguon, "mon": mon, "ten": ten, "ban": ban_cu + 1,
            "doan_van": doan_van, "cac_bai": cac_bai}
    if not ghi_va_kiem(duong, sach, thu):
        return 1
    cac_hinh = [h for b in cac_bai for c in b["cac_cau"] for h in c.get("hinh", [])]
    thieu = [] if thu else chep_hinh(cac_hinh, hinh_tu)
    for h in thieu:
        print(f"  LỖI {nguon}: thiếu ảnh {h}")
    so_cau = sum(len(b["cac_cau"]) for b in cac_bai)
    print(f"{nguon}: bản {sach['ban']}, {len(cac_bai)} bài, {so_cau} câu, {len(cac_hinh)} ảnh")
    return len(thieu)


def ghi_va_kiem(duong, sach, thu):
    """Ghi ra file tam, kiem, qua thi moi thay file that. [thu] thi chi kiem."""
    fd, tam = tempfile.mkstemp(suffix=".json", prefix=os.path.basename(duong)[:-5] + "-")
    os.close(fd)
    try:
        ghi(tam, sach)
        # Anh chua chep sang assets thi kiem.py khong thay: kiem anh rieng o buoc chep.
        r = subprocess.run([sys.executable, KIEM, tam, "--hinh", ""], capture_output=True, text=True)
        ra = (r.stdout + r.stderr).strip()
        dong = [x for x in ra.splitlines() if "LỖI" in x]
        if r.returncode != 0:
            print(f"  {os.path.basename(duong)}: kiem.py báo lỗi, KHÔNG ghi")
            for x in dong[:40]:
                print("  ", x)
            return False
        canh = [x for x in ra.splitlines() if "lưu ý" in x]
        for x in canh[:20]:
            print("  ", x)
        if not thu:
            shutil.move(tam, duong)
            tam = None
        return True
    finally:
        if tam and os.path.exists(tam):
            os.remove(tam)


def main():
    ap = argparse.ArgumentParser(description="Gộp dữ liệu ghép vào sách")
    sub = ap.add_subparsers(dest="viec", required=True)
    a1 = sub.add_parser("phan", help="gộp file phần vào sách đã có")
    a1.add_argument("file", nargs="+")
    a2 = sub.add_parser("sach", help="dựng một sách mới từ các file Unit")
    a2.add_argument("nguon")
    a2.add_argument("file", nargs="+")
    a2.add_argument("--ten")
    a2.add_argument("--mon")
    for a in (a1, a2):
        a.add_argument("--hinh-tu", help="thư mục chứa ảnh đã cắt, cấu trúc như assets/hinh")
        a.add_argument("--thu", action="store_true", help="chỉ kiểm, không ghi")
    a = ap.parse_args()
    if a.viec == "phan":
        loi = gop_phan(a.file, a.hinh_tu, a.thu)
    else:
        loi = gop_sach(a.nguon, a.file, a.hinh_tu, a.thu, a.ten, a.mon)
    sys.exit(1 if loi else 0)


if __name__ == "__main__":
    main()
