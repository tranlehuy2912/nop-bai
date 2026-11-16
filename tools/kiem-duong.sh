#!/bin/sh
# So cac file hai app chep cua nhau xem con giong nhau khong.
#
# Hai app noi chuyen voi nhau bang ten truong trong Firestore. Lech mot chuoi thi
# lenh gui di khong ai nhan va khong co gi bao loi - chay cai nay sau moi lan sua.
#
# Tu 29/9/2026 Bang dieu khien chep them ThoiKhoaBieu.kt va NgayNghi.kt de ve tab
# Lich hoc. Lech thi dien thoai hien mot lich khac lich tablet dang dung de khoa may.
# Tu 4/10/2026 chep them BanLich.kt (doc lich tu Firestore) va LichMacDinh.kt.
# Ban Duong.kt thu ba cua app may ba noi (cho-gio-choi) bo ngay 16/11/2026 cung app do.
#
# Chay: sh tools/kiem-duong.sh

# Tablet la repo chua script nay, ten thu muc la gi cung duoc. App dien thoai nam
# canh no trong cung thu muc cha.
tablet=$(cd "$(dirname "$0")/.." && pwd)
goc=$(dirname "$tablet")
a="$tablet/app/src/main/java/vn/huytl/homeworkgate/dongbo/Duong.kt"
b="$goc/bang-dieu-khien/app/src/main/java/vn/huytl/bangdieukhien/data/Duong.kt"
lichTablet="$tablet/app/src/main/java/vn/huytl/homeworkgate/data"
lichBang="$goc/bang-dieu-khien/app/src/main/java/vn/huytl/bangdieukhien/data"

for f in "$a" "$b" \
    "$lichTablet/ThoiKhoaBieu.kt" "$lichBang/ThoiKhoaBieu.kt" \
    "$lichTablet/NgayNghi.kt" "$lichBang/NgayNghi.kt" \
    "$lichTablet/BanLich.kt" "$lichBang/BanLich.kt" \
    "$lichTablet/LichMacDinh.kt" "$lichBang/LichMacDinh.kt"; do
    [ -f "$f" ] || { echo "THIEU: $f"; exit 1; }
done

tam=$(mktemp -d)
trap 'rm -rf "$tam"' EXIT

# Bo dong package: hai app hai ten goi, chi phan con lai moi phai giong nhau.
sed '/^package /d' "$a" > "$tam/tablet"
sed '/^package /d' "$b" > "$tam/bang"

lech=0
diff -u "$tam/tablet" "$tam/bang" > "$tam/d1" || {
    echo "LECH: tablet (-) va Bang dieu khien (+)"
    tail -n +3 "$tam/d1"
    lech=1
}
[ "$lech" = 0 ] && echo "Hai ban Duong.kt giong nhau."

lechLich=0
for ten in ThoiKhoaBieu NgayNghi BanLich LichMacDinh; do
    sed '/^package /d' "$lichTablet/$ten.kt" > "$tam/lt"
    sed '/^package /d' "$lichBang/$ten.kt" > "$tam/lb"
    diff -u "$tam/lt" "$tam/lb" > "$tam/d3" || {
        echo "LECH: $ten.kt tablet (-) va Bang dieu khien (+)"
        tail -n +3 "$tam/d3"
        lechLich=1
    }
done
[ "$lechLich" = 0 ] && echo "Hai ban ThoiKhoaBieu.kt, NgayNghi.kt, BanLich.kt, LichMacDinh.kt giong nhau."

[ "$lech" = 0 ] && [ "$lechLich" = 0 ]
