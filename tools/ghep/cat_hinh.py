#!/usr/bin/env python3
"""
Cat mot hinh tu anh mot trang sach, luu WebP cho truong "hinh" cua cau hoi.

Chay:
    python3 tools/ghep/cat_hinh.py <anh-trang.png> <x0> <y0> <x1> <y1> <ra.webp> [--le 6] [--rong 480]

Toa do tinh bang diem anh tren chinh tam anh trang (goc tren trai la 0 0). Nen cat rong
hon hinh mot chut roi de tham so --le cat bot vien trang: ham tu bo phan nen trang quanh
hinh, chi giu --le diem anh vien.

VI SAO PHONG TO. Anh quet sach bai tap chi rong 1199 diem anh, mot hinh nho chung 225 x
145. Tablet ve no rong gap ba, gap bon: de may phong to luc ve thi hinh vo hat. Phong to
mot lan o day bang LANCZOS roi luu WebP thi tren man hinh min hon, ma moi hinh van chi
vai chuc KB.
"""

import argparse
import os
import sys

from PIL import Image, ImageChops


def cat_nen(img, le, nguong=235):
    """Bo phan nen sang (trang hay kem nhat cua trang sach) quanh hinh."""
    xam = img.convert("L")
    # Diem nao toi hon nguong la noi dung. Nen trang sach co mau kem nhat nen dung nguong
    # thay vi so voi mau goc tren trai.
    mat_na = xam.point(lambda v: 255 if v < nguong else 0)
    hop = mat_na.getbbox()
    if not hop:
        return img
    x0, y0, x1, y1 = hop
    x0 = max(0, x0 - le)
    y0 = max(0, y0 - le)
    x1 = min(img.width, x1 + le)
    y1 = min(img.height, y1 + le)
    return img.crop((x0, y0, x1, y1))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("trang")
    ap.add_argument("x0", type=int)
    ap.add_argument("y0", type=int)
    ap.add_argument("x1", type=int)
    ap.add_argument("y1", type=int)
    ap.add_argument("ra")
    ap.add_argument("--le", type=int, default=6, help="vien giu lai quanh hinh, diem anh")
    ap.add_argument("--rong", type=int, default=480, help="phong to cho toi it nhat chung nay")
    ap.add_argument("--khong-cat-nen", action="store_true")
    a = ap.parse_args()

    img = Image.open(a.trang).convert("RGB")
    if not (0 <= a.x0 < a.x1 <= img.width and 0 <= a.y0 < a.y1 <= img.height):
        sys.exit(f"toa do ngoai anh {img.width}x{img.height}")
    hinh = img.crop((a.x0, a.y0, a.x1, a.y1))
    if not a.khong_cat_nen:
        hinh = cat_nen(hinh, a.le)
    if hinh.width < a.rong:
        ti = a.rong / hinh.width
        hinh = hinh.resize((a.rong, max(1, round(hinh.height * ti))), Image.LANCZOS)
    os.makedirs(os.path.dirname(os.path.abspath(a.ra)), exist_ok=True)
    hinh.save(a.ra, "WEBP", quality=85, method=6)
    print(f"{a.ra}: {hinh.width}x{hinh.height}, {os.path.getsize(a.ra) // 1024} KB")


if __name__ == "__main__":
    main()
