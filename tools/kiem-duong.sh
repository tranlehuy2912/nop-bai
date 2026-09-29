#!/bin/sh
# So cac file ba app chep cua nhau xem con giong nhau khong.
#
# Ba app noi chuyen voi nhau bang ten truong trong Firestore. Lech mot chuoi thi
# lenh gui di khong ai nhan va khong co gi bao loi - chay cai nay sau moi lan sua.
#
# Tu 29/9/2026 Bang dieu khien chep them ThoiKhoaBieu.kt va NgayNghi.kt de ve tab
# Lich hoc. Lech thi dien thoai hien mot lich khac lich tablet dang dung de khoa may.
#
# Chay: sh tools/kiem-duong.sh

# Tablet la repo chua script nay, ten thu muc la gi cung duoc. Hai app dien thoai
# nam canh no trong cung thu muc cha.
tablet=$(cd "$(dirname "$0")/.." && pwd)
goc=$(dirname "$tablet")
a="$tablet/app/src/main/java/vn/huytl/homeworkgate/dongbo/Duong.kt"
b="$goc/bang-dieu-khien/app/src/main/java/vn/huytl/bangdieukhien/data/Duong.kt"
c="$goc/cho-gio-choi/app/src/main/java/vn/huytl/chogiochoi/data/Duong.kt"
lichTablet="$tablet/app/src/main/java/vn/huytl/homeworkgate/data"
lichBang="$goc/bang-dieu-khien/app/src/main/java/vn/huytl/bangdieukhien/data"

for f in "$a" "$b" "$c" \
    "$lichTablet/ThoiKhoaBieu.kt" "$lichBang/ThoiKhoaBieu.kt" \
    "$lichTablet/NgayNghi.kt" "$lichBang/NgayNghi.kt"; do
    [ -f "$f" ] || { echo "THIEU: $f"; exit 1; }
done

tam=$(mktemp -d)
trap 'rm -rf "$tam"' EXIT

# Bo dong package: ba app ba ten goi, chi phan con lai moi phai giong nhau.
sed '/^package /d' "$a" > "$tam/tablet"
sed '/^package /d' "$b" > "$tam/bang"
sed '/^package /d' "$c" > "$tam/ba"

lech=0
diff -u "$tam/tablet" "$tam/bang" > "$tam/d1" || {
    echo "LECH: tablet (-) va Bang dieu khien (+)"
    tail -n +3 "$tam/d1"
    lech=1
}
diff -u "$tam/tablet" "$tam/ba" > "$tam/d2" || {
    echo "LECH: tablet (-) va Cho gio choi (+)"
    tail -n +3 "$tam/d2"
    lech=1
}
[ "$lech" = 0 ] && echo "Ba ban Duong.kt giong nhau."

lechLich=0
for ten in ThoiKhoaBieu NgayNghi; do
    sed '/^package /d' "$lichTablet/$ten.kt" > "$tam/lt"
    sed '/^package /d' "$lichBang/$ten.kt" > "$tam/lb"
    diff -u "$tam/lt" "$tam/lb" > "$tam/d3" || {
        echo "LECH: $ten.kt tablet (-) va Bang dieu khien (+)"
        tail -n +3 "$tam/d3"
        lechLich=1
    }
done
[ "$lechLich" = 0 ] && echo "Hai ban ThoiKhoaBieu.kt, NgayNghi.kt giong nhau."

[ "$lech" = 0 ] && [ "$lechLich" = 0 ]
