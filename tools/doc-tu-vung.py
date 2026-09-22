#!/usr/bin/env python3
"""Chep bang tu vung o cuoi sach giao khoa ra JSON.

VI SAO CAN SCRIPT CHU KHONG GO TAY. Bang GLOSSARY cuoi cuon Tieng Anh 8 co khoang
ba tram dong, moi dong ba cot: tu kem loai tu, phien am IPA, nghia tieng Viet. Go
tay ba tram dong thi sai vai chuc dong la chuyen binh thuong, va cai dang so la sai
ma khong ai biet cho toi luc con bi cham sai mot tu no viet dung.

CACH CHONG SAI: chay N luot DOC LAP tren cung mot trang roi so ket qua. Cho nao moi
luot mot khac thi chac chan co van de, va danh dau lai de nguoi soat chi phai nhin
may dong do thay vi doc ca ba tram dong. Cho nao may luot deu giong nhau thi chua
chac dung, nhung xac suat ba lan cung sai y het mot kieu thi thap.

KHONG TU SUA GI CA. Script chi chep va bao cho lech, khong doan, khong dien vao cho
trong. Mot o rong trong ket qua la mot o rong trong sach, hoac la may khong doc
duoc - ca hai deu phai de nguoi nhin.

RA DANG DAY DU chu khong ra thang dang the hoc. Bang goc co bon thu (tu, phien am,
loai tu, nghia) cong so Unit, ma the hoc trong app hien chi giu hoi/dap. Chep ra
dang day du thi sau nay doi cach hoi - Anh sang Viet hay Viet sang Anh, tron moi
nhu theo loai tu - khong phai doc lai sach lan nua.

Chay:

  uv run --with pymupdf --python 3.12 tools/doc-tu-vung.py \
      --pdf "~/Downloads/sgk/SGK - Tieng Anh 8.pdf" --trang 137-140 \
      --bo anh8 --mon "Tiếng Anh" --ten "Từ vựng Tiếng Anh 8" \
      --ra tools/ra/anh8.json
"""

import argparse
import base64
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

GOC = "https://generativelanguage.googleapis.com/v1beta"

# Model manh hon ban app dung de cham bai. Cham bai can nhanh vi con dang ngoi cho;
# chep bang thi chay mot lan roi thoi, nen doi cham lay chinh xac.
MODEL_MAC_DINH = "gemini-3.5-flash"

CAU_LENH = """
Ảnh là một trang trong bảng GLOSSARY ở cuối sách giáo khoa Tiếng Anh 8. Trang này
chia thành nhiều cột, mỗi cột là một bảng ba ô: từ tiếng Anh kèm loại từ trong
ngoặc, phiên âm IPA giữa hai dấu gạch chéo, và nghĩa tiếng Việt. Xen giữa các bảng
là các tiêu đề màu cam dạng "Unit 8".

Chép lại TOÀN BỘ các dòng trong ảnh, đọc theo thứ tự: hết cột bên trái rồi mới sang
cột bên phải, từ trên xuống dưới.

Chỉ trả về JSON, không thêm chữ nào khác:
{"cac_dong":[{"unit":8,"tu":"access","loai":"n","am":"/'æksesestimated/","nghia":"nguồn để tiếp cận, truy cập vào"}]}

Quy tắc bắt buộc:
1. "unit" là số của tiêu đề Unit gần nhất Ở PHÍA TRÊN dòng đó. Trang có thể bắt đầu
   giữa chừng một Unit, khi đó các dòng trước tiêu đề Unit đầu tiên thuộc Unit liền
   trước. Nếu không đoán được thì để 0.
2. "tu" chép đúng từ tiếng Anh, giữ nguyên dấu cách và dấu nháy nếu có
   ("farmers' market", "on sale"). KHÔNG chép phần trong ngoặc vào đây.
3. "loai" là phần trong ngoặc ngay sau từ: n, v, adj, adv, prep, conj. Có nhiều loại
   thì ngăn bằng dấu phẩy ("n, v"). Không có ngoặc thì để chuỗi rỗng.
4. "am" chép đúng phiên âm giữa hai dấu gạch chéo, GIỮ CẢ hai dấu gạch chéo. Ký tự
   IPA phải chép đúng ký tự nhìn thấy, không thay bằng chữ cái thường gần giống.
   Không đọc rõ thì để chuỗi rỗng, TUYỆT ĐỐI không đoán.
5. "nghia" chép đúng nghĩa tiếng Việt, giữ nguyên dấu phẩy và dấu ngoặc trong ô.
   Ô xuống dòng thì nối lại thành một dòng, ngăn bằng dấu cách.
6. KHÔNG bỏ sót dòng nào, KHÔNG thêm dòng nào không có trong ảnh, KHÔNG sắp xếp lại.
7. Ô nào trong ảnh bị mờ hoặc che mất thì để chuỗi rỗng cho riêng ô đó, vẫn giữ dòng.

TRƯỚC KHI TRẢ LỜI, KIỂM TRA LẠI:
- Đếm số dòng trong ảnh và số phần tử trong "cac_dong", hai số phải bằng nhau.
- Mỗi tiêu đề Unit trong ảnh phải xuất hiện trong trường "unit" của ít nhất một dòng.
"""


def khoa_ai(duong):
    """Doc AI_KEYS trong local.properties. Nhieu khoa thi ngan bang dau phay.

    Bien moi truong AI_KEYS de len file. Co duong do de chay duoc ma KHONG phai dan
    khoa vao local.properties: moi khoa nam trong file do deu di thang vao file APK
    qua BuildConfig, va mot lan APK ra khoi nha la mat khoa - da xay ra roi.
    """
    tu_moi_truong = os.environ.get("AI_KEYS", "").strip()
    if tu_moi_truong:
        return [k.strip() for k in tu_moi_truong.split(",") if k.strip()]
    chu = open(duong, encoding="utf-8").read()
    m = re.search(r"^AI_KEYS\s*=\s*(.*)$", chu, re.M)
    if not m:
        sys.exit("khong thay AI_KEYS trong %s" % duong)
    cac = [k.strip() for k in m.group(1).split(",") if k.strip()]
    if not cac:
        sys.exit("AI_KEYS trong %s dang de trong" % duong)
    return cac


def hoi_gemini(khoa, model, anh_jpeg, cau_lenh):
    """Mot luot goi. Nem loi ra ngoai de ben goi quyet doi khoa hay bo cuoc."""
    than = json.dumps({
        "contents": [{"parts": [
            {"text": cau_lenh},
            {"inline_data": {"mime_type": "image/jpeg",
                             "data": base64.b64encode(anh_jpeg).decode()}},
        ]}],
        # Nhiet do 0: dang chep lai mot cai bang co san, khong phai dang viet van.
        # De mac dinh thi hai luot chay ra hai ban khac nhau o nhung cho no phan van,
        # ma nhu the thi phep so hai luot chi do do sang tao chu khong do do doc ro.
        "generationConfig": {"responseMimeType": "application/json", "temperature": 0},
    }).encode()

    req = urllib.request.Request(
        "%s/models/%s:generateContent?key=%s" % (GOC, model, khoa),
        data=than, headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=300) as res:
        goi = json.load(res)
    chu = "".join(p.get("text", "")
                  for p in goi["candidates"][0]["content"]["parts"])
    return json.loads(chu)["cac_dong"]


# Loi tam thoi cua Google: cho mot lat roi thu lai la duoc. 503 la "dang dong
# khach", 429 la cham han muc phut, 500 la loi trong nha ho.
MA_THU_LAI = {429, 500, 502, 503, 504}

# Cho bao lau truoc moi lan thu lai. Dan ra chu khong deu: mot dot 503 keo dai vai
# phut, ma thu lai sau ba giay may lan lien thi chi ton han muc chu khong qua duoc.
CHO_THU_LAI = [5, 20, 60, 120]


def chep_mot_trang(cac_khoa, model, anh, so_luot, nhan):
    """Chay [so_luot] luot doc lap tren mot trang, tra ve danh sach cac ban.

    THU LAI KHI GOOGLE BAN. Ban dau moi luot chi thu moi khoa dung mot lan, va mot
    dot 503 - Google bao dang dong khach - la ca luot do mat trang. Da xay ra that:
    mot lan chay sau tren tam luot hong, hai trang khong doc duoc dong nao.
    """
    ban = []
    for i in range(so_luot):
        cuoi = None
        xong = False
        for lan, cho in enumerate([0] + CHO_THU_LAI):
            if cho:
                print("  %s luot %d: doi %ds roi thu lai (%s)" % (nhan, i + 1, cho, cuoi))
                time.sleep(cho)
            for khoa in cac_khoa:
                try:
                    ban.append(hoi_gemini(khoa, model, anh, CAU_LENH))
                    print("  %s luot %d: %d dong" % (nhan, i + 1, len(ban[-1])))
                    xong = True
                    break
                except urllib.error.HTTPError as e:
                    than = e.read()[:150].decode("utf8", "replace").replace("\n", " ")
                    cuoi = "HTTP %d %s" % (e.code, than)
                    if e.code not in MA_THU_LAI:
                        # 401, 403: khoa hong. Thu lai bao nhieu lan cung the.
                        xong = None
                        break
                except Exception as e:  # noqa: BLE001
                    cuoi = "%s: %s" % (type(e).__name__, e)
            if xong or xong is None:
                break
        if not xong:
            print("  %s luot %d HONG - %s" % (nhan, i + 1, cuoi))

    # Chuan hoa phien am NGAY TAI DAY, truoc khi dem so hai luot.
    #
    # Ban dau viec nay lam o buoc gop cuoi cung, va hau qua la phep so hai luot bao
    # 59 cho lech chi vi mot luot go dau nhay ASCII con luot kia go dau trong am
    # that - cung mot cach doc, hai cach viet. Nam muoi chin dong nhieu nhu vay nhan
    # chim mat cho lech THAT (muc "UFO", hai luot doc ra hai cach doc khac han), ma
    # ca cai bang canh bao sinh ra chi de nguoi doc duoc trong mot phut.
    for b in ban:
        for d in b:
            d["am"] = sua_ipa(d.get("am", ""))
    return ban


# Ky tu IPA that trong sach, va ky tu ASCII nhin gan giong ma may hay chep ra thay.
# Doi duoc mot cach an toan vi trong mot chuoi phien am thi khong co nghia nao khac:
# dau hai cham LUON la dau keo dai, so 3 LUON la nguyen am ɜ (chu so khong bao gio
# xuat hien trong phien am mot tu), dau nhay va dau phay LUON la dau trong am.
#
# Cot phien am khong tham gia cham diem - no chi hien ra sau khi con tra loi - nen
# mot cho sai o day khong lam ai mat phut. Nhung con doc no moi ngay, va day chinh la
# cho de con nhin ra trong am nam o dau, nen van nen dung.
IPA_THAY = {":": "\u02d0", "3": "\u025c", "'": "\u02c8", ",": "\u02cc"}


def sua_ipa(am):
    """Doi cac ky tu ASCII may chep ra tro lai ky tu IPA that.

    CHI DOI BEN TRONG HAI DAU GACH CHEO. Ban dau ham nay doi ca chuoi, va no lam
    hong dung nhung muc co HAI cach doc: sach in "/'sætən/, /'sætɜːn/" voi dau phay
    NGOAI cap gach cheo de ngan hai cach, ma doi ca chuoi thi dau phay do bien thanh
    dau trong am phu - ra "/ˈsætən/ˌ /ˈsætɜːn/", mot chuoi khong con nghia gi. Trong
    ban chep dau tien co hai muc dinh: "Saturn" va "UFO".

    Ben trong cap gach cheo thi luat cu van dung: dau phay va dau nhay luon la dau
    trong am, dau hai cham luon la dau keo dai, so 3 luon la nguyen am.
    """
    if not am:
        return am
    ra, trong = [], False
    for c in am:
        if c == "/":
            trong = not trong
            ra.append(c)
        else:
            ra.append(IPA_THAY.get(c, c) if trong else c)
    chu = "".join(ra)

    # Chu "a" thuong ngay truoc dau keo dai LUON la nguyen am ɑ.
    #
    # Hai ky tu nay nhin gan nhu y het trong font cua sach - a (U+0061) va ɑ (U+0251)
    # khac nhau moi cai mau o dau chu - nen may chep nham rat deu: mot luot ra
    # "/ˈbaːɡən/", luot kia ra "/ˈbɑːɡən/". Doi duoc an toan vi tieng Anh khong co
    # nguyen am dai /aː/; chu "a" thuong chi dung trong /aɪ/ va /aʊ/, ma hai cai do
    # khong co dau keo dai theo sau.
    return chu.replace("a\u02d0", "\u0251\u02d0")


def gop_nghia(a, b):
    """Gop nghia cua hai dong cung mot tu.

    KHONG DUNG PHEP "chuoi nay nam trong chuoi kia" DE COI LA DA CO. Tieng Viet co
    rat nhieu tu la tien to cua tu khac, va cai bay do da sap: "release" o Unit 5
    nghia "thả", o Unit 7 nghia "thải ra, làm thoát ra". Chu "thả" nam gon trong chu
    "thải", nen phep kiem tra cu ket luan la da co roi va BO HAN nghia "thả" - mot
    nghia con phai hoc bien mat ma khong bao gi.

    Luat dung: hai ben chi coi la mot khi ben dai bat dau bang ben ngan VA ngay sau
    do la mot ranh gioi that - dau cach hay dau mo ngoac. "tài khoản" va "tài khoản
    (ngân hàng...)" thoa, nen giu ban dai. "thả" va "thải ra..." khong thoa vi sau
    "thả" la chu "i", nen giu ca hai.
    """
    a, b = (a or "").strip(), (b or "").strip()
    if not b:
        return a
    if not a:
        return b
    if a == b:
        return a
    dai, ngan = (a, b) if len(a) >= len(b) else (b, a)
    if dai.startswith(ngan) and dai[len(ngan):len(ngan) + 1] in (" ", "("):
        return dai
    return "%s; %s" % (a, b)


def khoa_dong(d):
    """Danh tinh mot dong: chinh cai ma ben app se dat cho tu do.

    PHAI GIONG HET BoTuVung.maCua BEN APP - ha chu thuong, khoang trang doi thanh
    gach ngang. Ban dau ham nay gom khoang trang thanh mot dau cach thay vi doi
    thanh gach ngang, va hai luat khac nhau mot chut do da lam mat mot tu: sach
    Unit 11 in hai dong rieng "face-to-face" (adj) va "face to face" (adv), script
    coi la hai tu khac nhau nen ghi ca hai vao file, con app dat cho ca hai cung mot
    ma nen dong nap sau de len dong truoc. File 278 tu, bang trong may 277, khong
    ai bao gi.

    Hai dau doi chieu nhau thi cho trung bi bat ngay tu day va duoc gop dang hoang,
    kem mot dong canh bao cho nguoi doc.
    """
    return re.sub(r"\s+", "-", (d.get("tu") or "").strip().lower())


def gop(ban, nhan):
    """Gop cac luot lai, danh dau cho lech.

    LAY BAN DAU LAM GOC va chi doi chieu cac ban sau vao no, chu khong bo phieu da
    so: ba luot ma hai luot doc sai giong nhau thi bo phieu ra ket qua sai ma lai
    khong bao gi ca. Doi chieu thi moi cho khong khop deu noi ra, ke ca khi ban dau
    la ban dung.
    """
    if not ban:
        return [], ["%s: khong luot nao chay duoc" % nhan]

    goc, canh = ban[0], []
    if len(ban) > 1:
        for j, khac in enumerate(ban[1:], start=2):
            if len(khac) != len(goc):
                canh.append("%s: luot 1 doc %d dong, luot %d doc %d dong"
                            % (nhan, len(goc), j, len(khac)))
        theo_tu = [{khoa_dong(d): d for d in b} for b in ban[1:]]
        for d in goc:
            k = khoa_dong(d)
            for j, bang in enumerate(theo_tu, start=2):
                if k not in bang:
                    canh.append("%s: \"%s\" chi luot 1 doc ra" % (nhan, d.get("tu")))
                    continue
                for o in ("unit", "loai", "am", "nghia"):
                    if str(bang[k].get(o, "")) != str(d.get(o, "")):
                        canh.append("%s: \"%s\" cot %s lech - luot 1 \"%s\", luot %d \"%s\""
                                    % (nhan, d.get("tu"), o, d.get(o), j, bang[k].get(o)))
    return goc, canh


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pdf", required=True)
    ap.add_argument("--trang", required=True,
                    help="so trang PDF, dang 137-140 hoac 137,138,140")
    ap.add_argument("--bo", required=True, help="ma bo, di vao id tung the")
    ap.add_argument("--mon", default="Tiếng Anh")
    ap.add_argument("--ten", required=True)
    ap.add_argument("--ra", required=True)
    ap.add_argument("--luot", type=int, default=2, help="so luot doc doc lap moi trang")
    ap.add_argument("--dpi", type=int, default=200)
    ap.add_argument("--model", default=MODEL_MAC_DINH)
    ap.add_argument("--local", default="local.properties")
    ap.add_argument(
        "--tu-luot", default="",
        help="dung ban tho da luu (file .luot.json) thay vi goi Gemini lai",
    )
    ap.add_argument(
        "--sua", default="",
        help="file JSON sua tay, ap sau cung; xem CACH_SUA trong ma nguon",
    )
    args = ap.parse_args()

    trang = []
    for phan in args.trang.split(","):
        if "-" in phan:
            a, b = phan.split("-")
            trang += list(range(int(a), int(b) + 1))
        else:
            trang.append(int(phan))

    # Chay lai tu ban tho: cac luot da chep xong tu lan truoc, chi dem so va gop lai.
    #
    # Co duong nay vi phan sua loi cua script nam O BUOC SAU khi goi mang - chuan hoa
    # phien am, so hai luot, gop nghia. Sua mot trong ba cho do ma phai goi Gemini
    # lai tam lan nua thi vua cham vua ton han muc, ma ban tho thi khong doi.
    cu = None
    if args.tu_luot:
        cu = json.load(open(args.tu_luot, encoding="utf-8"))
        print("chay lai tu %s, khong goi mang" % args.tu_luot)
    else:
        import pymupdf

        cac_khoa = khoa_ai(args.local)
        d = pymupdf.open(os.path.expanduser(args.pdf))
        print("doc %s, %d trang, %d luot moi trang, model %s"
              % (os.path.basename(args.pdf), len(trang), args.luot, args.model))

    dong, canh, tho, trang_hong = [], [], {}, []
    for so in trang:
        if cu is not None:
            ban = cu.get("tr%d" % so, [])
            # Chuan hoa lai: ban tho giu nguyen van may tra ve, chua qua sua_ipa.
            for b in ban:
                for x in b:
                    x["am"] = sua_ipa(x.get("am", ""))
            print("trang %d: %s" % (so, ", ".join("%d dong" % len(b) for b in ban) or "khong co"))
        else:
            anh = d[so - 1].get_pixmap(dpi=args.dpi).tobytes("jpeg", jpg_quality=88)
            print("trang %d (%d KB)" % (so, len(anh) // 1024))
            ban = chep_mot_trang(cac_khoa, args.model, anh, args.luot, "tr%d" % so)
        tho["tr%d" % so] = ban
        if not ban:
            trang_hong.append(so)
        g, c = gop(ban, "tr%d" % so)
        dong += g
        canh += c

    # Bo dong khong co tu hoac khong co nghia: mot the thieu mot trong hai thi khong
    # hoi duoc chieu nao ca. Bao ra de nguoi biet da bo cai gi.
    tot = [x for x in dong if (x.get("tu") or "").strip() and (x.get("nghia") or "").strip()]
    for x in dong:
        if x not in tot:
            canh.append("bo dong thieu tu hoac nghia: %s" % json.dumps(x, ensure_ascii=False))

    # Trung tu: sach that su lap mot tu o hai Unit, va thuong la hai NGHIA khac nhau -
    # "release" la "thả" o Unit 5 va "thải ra, làm thoát ra" o Unit 7.
    #
    # GOP NGHIA LAI chu khong bo ban sau. Bo di la mat han mot nghia con phai hoc, ma
    # mat im lang: trong file chi con mot dong nhin rat binh thuong. Giu ca hai dong
    # thi cung khong duoc, vi ma cua mot tu lay tu chinh tu do nen hai dong se de len
    # nhau trong may, va con bi hoi cung mot tu hai lan voi hai dap an khac nhau.
    #
    # Gop thi mot the mang ca hai nghia: hoi chieu Viet sang Anh thi hien ca hai nghia
    # roi con go ra mot tu, dung. Hoi chieu Anh sang Viet thi lua chon dung la ca cum.
    theo_tu, cuoi = {}, []
    for x in tot:
        k = khoa_dong(x)
        x["am"] = sua_ipa(x.get("am", ""))
        cu = theo_tu.get(k)
        if cu is None:
            theo_tu[k] = x
            cuoi.append(x)
            continue
        canh.append("trung tu \"%s\" o Unit %s va Unit %s, da gop hai nghia lam mot"
                    % (x.get("tu"), cu.get("unit"), x.get("unit")))
        cu["nghia"] = gop_nghia(cu.get("nghia", ""), x.get("nghia", ""))
        # Giu Unit NHO HON: tu xuat hien lan dau o Unit nao thi con hoc no tu do.
        if x.get("unit") and cu.get("unit") and x["unit"] < cu["unit"]:
            cu["unit"] = x["unit"]
        # Loai tu ben nao co thi lay, khong de trong neu mot ben khai duoc.
        if not cu.get("loai") and x.get("loai"):
            cu["loai"] = x["loai"]

    # Ap cac sua tay SAU CUNG.
    #
    # VI SAO CAN. Nguoi soat ba tram dong the nao cung tim ra vai cho may doc nham -
    # va hai luot doc giong nhau khong cuu duoc, vi may sai deu ca hai lan la chuyen
    # co that. Khong co duong nay thi moi sua tay deu bi lan chay sau xoa sach, va
    # nguoi soat phai nho trong dau minh da sua nhung gi.
    #
    # Dang file: {"release": {"unit": 5, "nghia": "thả"}, ...}. Khoa la chinh tu do.
    # Sua tu nao khong con trong ban chep thi BAO RA chu khong lam thinh: no nghia la
    # sach doi, hoac ban chep lan nay tach cau khac lan truoc, va ca hai deu dang xem.
    if args.sua:
        sua = json.load(open(args.sua, encoding="utf-8"))
        theo_tu = {khoa_dong(x): x for x in cuoi}
        for tu, cac_o in sua.items():
            dich = theo_tu.get(khoa_dong({"tu": tu}))
            if dich is None:
                canh.append("sua tay \"%s\": khong co tu nay trong ban chep, bo qua" % tu)
                continue
            for o, gia_tri in cac_o.items():
                if str(dich.get(o, "")) != str(gia_tri):
                    print("  sua tay: %s.%s \"%s\" -> \"%s\"" % (tu, o, dich.get(o), gia_tri))
                dich[o] = gia_tri

    ra = {
        "bo": args.bo,
        "mon": args.mon,
        "ten": args.ten,
        # Tang tay moi lan sua file, giong cac file trong assets/nganhang.
        "ban": 1,
        "nguon_trang": trang,
        "cac_tu": cuoi,
    }
    os.makedirs(os.path.dirname(args.ra) or ".", exist_ok=True)

    # MOT TRANG KHONG DOC DUOC THI KHONG GHI GI CA.
    #
    # Ban dau cho nay cu ghi nhung trang doc duoc. Hau qua khi Google ban: file ra
    # 136 tu thay vi 278, va no DE LEN ban tot cua lan chay truoc. Nhin vao file thi
    # khong co dau hieu gi - JSON van hop le, van co "ban": 1, chi la thieu mot nua
    # cuon sach. Kieu hong im lang do dung bang kieu hong ma [NganHang.doc] tu choi
    # tu dau: nap mot nua quyen con te hon khong nap.
    if trang_hong:
        print("\nKHONG GHI FILE: trang %s khong doc duoc dong nao."
              % ", ".join(str(t) for t in trang_hong))
        print("File cu o %s giu nguyen. Chay lai sau, hoac doi --model." % args.ra)
        sys.exit(1)

    # Ghi ra file tam roi moi doi ten. Dut giua chung - het pin, bam Ctrl-C - thi file
    # cu van nguyen ven chu khong con mot nua.
    tam = args.ra + ".dang-ghi"
    with open(tam, "w", encoding="utf-8") as f:
        json.dump(ra, f, ensure_ascii=False, indent=1)
    os.replace(tam, args.ra)

    # Giu ban tho cua TUNG luot. Khong giu thi moi lan muon xem lai mot cho hai luot
    # doc khac nhau deu phai chay lai ca bon trang, mat muoi phut va ton han muc.
    with open(os.path.splitext(args.ra)[0] + ".luot.json", "w", encoding="utf-8") as f:
        json.dump(tho, f, ensure_ascii=False, indent=1)

    theo_unit = {}
    for x in cuoi:
        theo_unit[x.get("unit")] = theo_unit.get(x.get("unit"), 0) + 1
    print("\nghi %s: %d tu" % (args.ra, len(cuoi)))
    print("theo Unit: " + ", ".join("U%s=%d" % (u, n) for u, n in sorted(
        theo_unit.items(), key=lambda t: (t[0] is None, t[0]))))

    if canh:
        duong = os.path.splitext(args.ra)[0] + ".canh-bao.txt"
        with open(duong, "w", encoding="utf-8") as f:
            f.write("\n".join(canh) + "\n")
        print("\n%d cho can nguoi soat, ghi o %s" % (len(canh), duong))
        for c in canh[:15]:
            print("  " + c)
        if len(canh) > 15:
            print("  ... con %d dong nua" % (len(canh) - 15))
    else:
        print("\ncac luot doc giong het nhau. Van nen soat mot luot bang mat.")


if __name__ == "__main__":
    main()
