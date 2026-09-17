#!/bin/sh
# So ba ban Duong.kt xem con giong nhau khong.
#
# Ba app noi chuyen voi nhau bang ten truong trong Firestore. Lech mot chuoi thi
# lenh gui di khong ai nhan va khong co gi bao loi - chay cai nay sau moi lan sua.
#
# Chay: sh tools/kiem-duong.sh

goc=$(cd "$(dirname "$0")/../.." && pwd)
a="$goc/homework-gate/app/src/main/java/vn/huytl/homeworkgate/dongbo/Duong.kt"
b="$goc/homework-gate-3/app/src/main/java/vn/huytl/bangdieukhien/data/Duong.kt"
c="$goc/homework-gate-2/app/src/main/java/vn/huytl/chogiochoi/data/Duong.kt"

for f in "$a" "$b" "$c"; do
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
exit $lech
