#!/usr/bin/env python3
"""Lay JSON lich tu LichMacDinh.kt, in ra mot dong de dan vao truong "json" tren Firebase.

    python3 tools/lich/xuat.py          # in mot dong JSON
    python3 tools/lich/xuat.py --tom    # in tom tat de soat voi anh thoi khoa bieu

Script chi doc file va kiem JSON dung cu phap. Kiem lich co hop le khong (gio, tiet, buong
may) la viec cua BanLich.doc, chay bang BanLichTest ben bang-dieu-khien truoc khi ghi.
Xem tools/lich/CAP_NHAT_LICH.md.
"""

import json
import sys
from pathlib import Path

FILE = Path(__file__).resolve().parents[2] / \
    "app/src/main/java/vn/huytl/homeworkgate/data/LichMacDinh.kt"
MO = 'const val JSON = """'
THU = {"2": "Thứ 2", "3": "Thứ 3", "4": "Thứ 4", "5": "Thứ 5", "6": "Thứ 6", "7": "Thứ 7"}


def doc():
    s = FILE.read_text(encoding="utf-8")
    i = s.index(MO) + len(MO)
    return json.loads(s[i:s.index('"""', i)])


def tom(o):
    print(f"phienBan {o['phienBan']} · lớp {o['lop']} · {o.get('ghiChu', '')}")
    for buoi in ("sang", "chieu"):
        gio = o["gioTiet"][buoi]
        print(f"\n{buoi.upper()}  " + "  ".join(f"t{t} {g}" for t, g in sorted(gio.items())))
        for thu, mon in sorted(o[buoi].items()):
            print(f"  {THU[thu]}: " + " | ".join(f"{t} {m}" for t, m in sorted(mon.items())))
    print(f"\nbuông máy chiều {o['buongMay']['chieu']}, sáng " +
          ", ".join(f"{THU[t]} {g}" for t, g in sorted(o["buongMay"]["sang"].items())))
    print("nghỉ: " + "; ".join(f"{k['tu']}→{k['den']} {k['ten']}" for k in o["ngayNghi"]))
    print(f"hết năm học {o['hetNamHoc']}")


if __name__ == "__main__":
    o = doc()
    if "--tom" in sys.argv:
        tom(o)
    else:
        print(json.dumps(o, ensure_ascii=False, separators=(",", ":")))
