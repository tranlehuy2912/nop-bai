#!/usr/bin/env bash
# Dung may ao de thu Homework Gate ma khong phai dung tablet that.
#
# May ao dat theo Xiaomi Pad 5: man 2560x1600, mat do 280dpi, Android 13.
# Quyen Accessibility va Device Admin binh thuong phai bam tay trong Settings,
# o day cap thang qua adb, vi bam tay moi lan cai lai rat mat cong.
#
#   tools/emu.sh up        # tao may ao neu chua co roi bat len
#   tools/emu.sh install   # build APK roi cai, cap het quyen
#   tools/emu.sh seed      # nap token bot + chat id + PIN 1234
#   tools/emu.sh test      # chay GateStoreTest tren may ao
#   tools/emu.sh web       # trang web: tu dong thu, lich ca tuan, cong, han app...
#   tools/emu.sh tuan      # in ra app se nhac gi suot bay ngay toi, khong phai doi
#   tools/emu.sh gio 2026-09-14 12:05   # van dong ho may ao toi mot moc
#   tools/emu.sh giothuc   # tra dong ho ve gio that
#   tools/emu.sh guithu    # gui thu mot lan nop bai len Telegram
#   tools/emu.sh open      # mo cong bang tay, khong can Telegram
#   tools/emu.sh close     # dong cong
#   tools/emu.sh status    # xem trang thai cong, quyen, service
#   tools/emu.sh log       # xem log cua app
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

AVD=pad5
IMAGE="system-images;android-33;google_apis;arm64-v8a"
PKG=vn.huytl.homeworkgate
RUNNER="$PKG.test/androidx.test.runner.AndroidJUnitRunner"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Chay mot lop test cu the. Tach ra vi ca ba lenh seed/test/open deu goi giong nhau.
instrument() {
  adb shell am instrument -w -e class "$1" "${@:2}" "$RUNNER"
}

cmd_up() {
  if ! avdmanager list avd 2>/dev/null | grep -q "Name: $AVD"; then
    echo "Tao may ao $AVD..."
    echo no | avdmanager create avd -n "$AVD" -k "$IMAGE" --force >/dev/null
    # Pad 5 that: 2560x1600, 11 inch. Ram 4G cho du, khong thi HyperOS-style
    # giet tien trinh nen minh khong phan biet duoc loi that voi loi thieu ram.
    local ini="$HOME/.android/avd/$AVD.avd/config.ini"
    {
      echo "hw.lcd.width=2560"
      echo "hw.lcd.height=1600"
      echo "hw.lcd.density=280"
      echo "hw.ramSize=4096"
      echo "vm.heapSize=512"
      echo "hw.keyboard=yes"
      # Camera sau phai la virtualscene. De "emulated" nhu avdmanager tu dat thi
      # khung hinh tra ve timestamp khong tang, CameraX hien mot man den, va minh
      # tuong app loi camera trong khi loi la o may ao.
      echo "hw.camera.back=virtualscene"
      echo "disk.dataPartition.size=6G"
    } >> "$ini"
  fi

  if adb devices | grep -q emulator; then
    echo "May ao dang chay roi."
  else
    echo "Bat may ao..."
    # virtualscene: camera sau nhin vao mot can phong ao, du de bam nut chup.
    # -gpu host chu khong phai auto: auto co the roi vao Vulkan phan mem
    # (llvmpipe), luc do canh quay ao cua camera sau ra toan mau den va minh
    # tuong app khong mo duoc camera.
    # -dns-server: may ao thua DNS cua may Mac, ma cau hinh DNS cua may Mac thi
    # khong phai luc nao cung dung duoc trong may ao. Ping duoc IP ma khong phan
    # giai duoc ten mien la dau hieu cua chuyen nay, va luc do moi thu dinh den
    # api.telegram.org deu treo.
    nohup emulator -avd "$AVD" -no-snapshot-load -gpu host \
      -dns-server 8.8.8.8,1.1.1.1 \
      -camera-back virtualscene -camera-front emulated \
      >/tmp/emu-$AVD.log 2>&1 &
  fi
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
  echo "May ao san sang."
}

cmd_install() {
  (cd "$ROOT" && ./gradlew assembleDebug assembleDebugAndroidTest)
  adb install -r -g "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
  adb install -r "$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
  cmd_perms
}

cmd_perms() {
  adb shell pm grant $PKG android.permission.CAMERA || true
  adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
  adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
  adb shell dpm set-active-admin "$PKG/.guard.AdminReceiver" || true
  # Bat Accessibility. Tren may that day la man hinh bo phai tu vao bam.
  adb shell settings put secure enabled_accessibility_services \
    "$PKG/$PKG.guard.GuardAccessibilityService"
  adb shell settings put secure accessibility_enabled 1
  echo "Da cap quyen."
}

# Token khong truyen qua dong lenh de no khong nam lai trong lich su shell.
# De trong file tools/bot.env (da cho vao .gitignore):
#   TOKEN=123456:ABC...
#   CHATID=987654321
#   PIN=000000
cmd_seed() {
  local env_file="$ROOT/tools/bot.env"
  [ -f "$env_file" ] || { echo "Chua co $env_file"; exit 1; }
  # shellcheck disable=SC1090
  source "$env_file"
  # KHONG nap TOKEN that vao may ao: Telegram chi cho mot may nghe mot bot, may ao
  # nghe chung token voi tablet cua Le Hoa la hai ben giat tin cua nhau (loi 409).
  # Ban go loi da co san bot rieng trong Defaults.BOT_MAY_AO, nen o day khong truyen
  # token gi ca - tru khi bot.env co dat TOKEN_MAY_AO.
  instrument "$PKG.ManualSeed" ${TOKEN_MAY_AO:+-e token "$TOKEN_MAY_AO"} \
    -e chatid "$CHATID" -e minutes "${MINUTES:-60}" ${PIN:+-e pin "$PIN"}
  echo "Da nap cau hinh."
}

# Chu y: bo test xoa sach cau hinh tren may (xem chu thich trong GateStoreTest).
# Chay xong thi nap lai bang "tools/emu.sh seed".
cmd_test()   { instrument "$PKG.GateStoreTest"; echo; echo "Da xoa cau hinh tren may. Chay 'tools/emu.sh seed' de nap lai."; }

# Gui thu mot lan nop bai ma khong can camera. So trang tung nhom dat qua bien:
#   DANDO=1 DEBAI=2 BAIGIAI=2 tools/emu.sh guithu
cmd_guithu() {
  instrument "$PKG.ManualSend" \
    -e dando "${DANDO:-1}" -e debai "${DEBAI:-2}" -e baigiai "${BAIGIAI:-2}"
}
cmd_open()   { instrument "$PKG.ManualGate#moCong"; }
cmd_close()  { instrument "$PKG.ManualGate#dongCong"; }

# Ten du an Firebase trong mot file google-services.json. Rong neu chua co file.
ten_du_an() {
  [ -f "$1" ] || return 0
  grep -o '"project_id"[^,]*' "$1" 2>/dev/null | head -1 | cut -d'"' -f4 || true
}

cmd_status() {
  # Du an Firebase truoc tien: day la cho de nham nhat, va nham o day thi may ao
  # ghi thang vao du lieu that ma khong co dau hieu gi.
  echo "--- firebase ---"
  local thu that
  thu="$(ten_du_an "$ROOT/app/src/debug/google-services.json")"
  that="$(ten_du_an "$ROOT/app/google-services.json")"
  if [ -n "$thu" ]; then
    echo "ban go loi: $thu"
  else
    echo "ban go loi: ${that:-chua co} <-- DU AN THAT! chua co app/src/debug/google-services.json"
  fi
  echo "ban that:   ${that:-chua co}"
  # Ban dang cai tren may ao that su noi vao dau, doc tu log chinh no.
  adb logcat -d -s DongBo 2>/dev/null | grep -o "project=.*" | tail -1 || true
  echo "--- app dang o truoc mat ---"
  adb shell dumpsys activity activities | grep -m1 'ResumedActivity' || true
  echo "--- accessibility ---"
  adb shell settings get secure enabled_accessibility_services
  echo "--- overlay ---"
  adb shell dumpsys window | grep -c "HomeworkGate\|BlockOverlay" || true
  echo "--- service dang chay ---"
  adb shell dumpsys activity services $PKG | grep -E "ServiceRecord|app=" | head -5 || true
}

cmd_log() { adb logcat -v time -s HomeworkGate:V Guard:V ApprovalService:V AndroidRuntime:E; }

# Chay mot lop test roi in lai nhung gi no println.
#
# "am instrument" khong tra println ra man hinh, no chi di ra logcat duoi tag
# System.out. Nen xoa logcat truoc, chay, roi vot lai dung nhung dong cua minh.
chay_va_doc() {
  adb logcat -c >/dev/null 2>&1 || true
  instrument "$@" >/dev/null
  adb logcat -d -s System.out 2>/dev/null | sed -n 's/.*System\.out: //p' | tr -d '\r'
}

# Van dong ho may ao toi mot moc bat ky, de khoi ngoi doi den thu hai.
#
#   tools/emu.sh gio                    # xem may ao dang o gio nao
#   tools/emu.sh gio 2026-09-14 12:05   # thu hai, vua qua moc buong may
#   tools/emu.sh gio 11:50              # giu nguyen ngay, chi doi gio
#
# Phai tat auto_time truoc, khong thi may ao keo lai gio that tu mang sau vai phut
# va minh tuong app tu tat man chan. Dich vu xet lai loi nhac moi 60 giay (moi giay
# khi dang dem nguoc), nen van xong cho chung mot phut roi hay ket luan.
cmd_gio() {
  local ngay="${1:-}" gio="${2:-}"
  if [ -z "$ngay" ]; then
    echo "May ao: $(adb shell date | tr -d '\r')"
    echo "May Mac: $(date)"
    return
  fi
  # Chi truyen mot tham so co dau hai cham thi do la gio, giu nguyen ngay tren may.
  if [[ "$ngay" == *:* ]]; then
    gio="$ngay"
    ngay="$(adb shell date +%Y-%m-%d | tr -d '\r')"
  fi
  [ -n "$gio" ] || gio="00:00"

  adb root >/dev/null 2>&1 || true
  adb wait-for-device
  adb shell settings put global auto_time 0
  adb shell settings put global auto_time_zone 0
  # toybox date nhan dang MMDDhhmmYYYY.ss
  adb shell date "${ngay:5:2}${ngay:8:2}${gio%%:*}${gio##*:}${ngay:0:4}.00" >/dev/null
  adb shell am broadcast -a android.intent.action.TIME_SET >/dev/null
  echo "May ao dang o $(adb shell date | tr -d '\r')"
}

# Tra dong ho may ao ve gio that.
cmd_giothuc() {
  adb root >/dev/null 2>&1 || true
  adb wait-for-device
  adb shell settings put global auto_time 1
  adb shell settings put global auto_time_zone 1
  adb shell date "$(date +%m%d%H%M%Y.%S)" >/dev/null
  adb shell am broadcast -a android.intent.action.TIME_SET >/dev/null
  echo "May ao dang o $(adb shell date | tr -d '\r')"
}

# Chay kho ca tuan: in ra app se noi gi vao tung moc cua bay ngay toi, trong vai
# giay, khong can van dong ho. Dung de soat luat; con muon nhin man chan bang mat
# thi dung "gio" o tren.
#
#   tools/emu.sh tuan                   # bay ngay tu hom nay
#   tools/emu.sh tuan 2026-09-14 7      # bay ngay tu thu hai 14/9
#   DASOAN=tatca tools/emu.sh tuan      # coi nhu da soan cap het
cmd_tuan() {
  chay_va_doc "$PKG.ManualLich#cathang" \
    ${1:+-e tu "$1"} -e ngay "${2:-7}" \
    ${DASOAN:+-e dasoan "$DASOAN"} ${CHAN:+-e chan "$CHAN"}
}

# Mot moc duy nhat:  tools/emu.sh luc 2026-09-14T12:10
cmd_luc() {
  chay_va_doc "$PKG.ManualLich#motluc" -e luc "${1:?Thieu moc, vi du 2026-09-14T12:10}" \
    ${DASOAN:+-e dasoan "$DASOAN"} ${CHAN:+-e chan "$CHAN"}
}

# Trang web thay cho ca chuc lenh o tren: TU DONG THU (chon muc, bam mot nut, may
# tu dung boi canh - chup man hinh - soat ket qua), lich ca tuan, cong va gio
# choi, may tinh luat cong gio, han gio tung app, tin nhan, nhat ky, va van dong
# ho may ao. Xem tools/web.py va tools/kichban.py.
cmd_web() {
  exec python3 "$ROOT/tools/web.py" "${1:-8765}"
}

case "${1:-}" in
  up) cmd_up ;;
  install) cmd_install ;;
  perms) cmd_perms ;;
  seed) cmd_seed ;;
  test) cmd_test ;;
  gio) shift; cmd_gio "$@" ;;
  giothuc) cmd_giothuc ;;
  tuan) shift; cmd_tuan "$@" ;;
  luc) shift; cmd_luc "$@" ;;
  web) shift; cmd_web "$@" ;;
  guithu) cmd_guithu ;;
  open) cmd_open ;;
  close) cmd_close ;;
  status) cmd_status ;;
  log) cmd_log ;;
  *) sed -n '2,20p' "$0"; exit 1 ;;
esac
