#!/usr/bin/env python3
"""Cac muc thu tu dong cho ban thu web.

Moi muc: dung boi canh tren may ao, chup lai man hinh, roi soat bang ba duong:

  cho / khong   chu THAT dang hien, doc bang "uiautomator dump".
  noi           cua so noi cua app dang che cai gi, doc bang "dumpsys window":
                "chan" man chan phu kin, "the" the nhac nho, "khong" khong che gi.
  luc + loai    hoi thang TinhLoiNhac xem dung moc do dang le phai nhac gi.

Phai co ca ba vi khong duong nao mot minh du. uiautomator chi doc duoc cua so dang
focus, ma the nhac thi khong focus - tung co mot muc bao "khong thay chu" trong khi
tam anh chup ra co the nam ro rang tren goc man hinh. Nguoc lai dumpsys chi biet co
mot cua so to bang ngan ay, khong biet trong do viet gi.

Anh chup de NGUOI xem, khong co doan nao doc anh de cham diem.

Chu de soat lay tu chinh ma nguon (HomeActivity, ManChan, cac layout), nen moi muc
la mot cau hoi that: app co con hien dung cau do nua khong.
"""

# Vai moc co that trong lich 2026-2027, moi moc soi mot canh khac nhau.
THU_SAU_SANG = "2026-09-18T07:00"   # dang trong buoi sang thu sau (vao hoc 07:15)
THU_NAM_CHIEU = "2026-09-17T13:00"  # dang trong buoi chieu thu nam
THU_NAM_SAP = "2026-09-17T11:50"    # con 10 phut nua phai buong may
TOI_THU_TU = "2026-09-16T20:00"     # toi, chua soan cap cho hom sau
CHU_NHAT = "2026-09-20T10:00"       # nghi, khong duoc chan gi

# Moc "ranh": toi thu tu, da tan hoc, chua toi gio ngu. Moi muc khong dinh den thoi
# khoa bieu deu neo vao day chu khong dung gio that cua may Mac.
#
# Dung gio that thi bo thu chay luc nao ra ket qua luc do: chay vao mot chieu trong
# tuan la man chan phu kin man hinh, va het moi muc ve cong deu bao hong - trong khi
# app khong sai gi ca. Da dinh mot lan nhu vay that, luc 13:34 chieu thu tu.
RANH = "2026-09-16T19:30"


def danh_sach():
    return [
        # ---------------- cong va gio choi ----------------
        dict(
            ma="cong-khoa", nhom="Cổng & giờ chơi",
            ten="Chưa nộp bài — máy khoá",
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.dat("xoaluot"), m.man("HomeActivity")),
            cho=["Chưa nộp bài", "Đang khoá"],
        ),
        dict(
            ma="cong-cho", nhom="Cổng & giờ chơi",
            ten="Nộp rồi, đang chờ ba Huy duyệt",
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.dat("themcho", ma="thu-tudong"), m.man("HomeActivity")),
            cho=["Chờ ba Huy duyệt"],
        ),
        dict(
            ma="cong-duyet", nhom="Cổng & giờ chơi",
            ten="Ba duyệt rồi, chưa bấm Bắt đầu",
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.dat("xoaluot"), m.dat("duyet", phut=30),
                           m.man("HomeActivity")),
            # Khong soat chu tren nut to: no nam cuoi trang va bi day khoi vung
            # nhin thay khi man hinh dang co nhieu the. Soat phan the trang thai -
            # do moi la cho noi "phieu con nguyen, chua bam".
            cho=["30 phút", "Chưa tính giờ đâu"],
        ),
        dict(
            ma="cong-choi", nhom="Cổng & giờ chơi",
            ten="Đang được chơi, đồng hồ chạy",
            # Bam nut THAT chu khong goi start() qua ManualBang: dat qua do thi phai
            # force-stop cho app doc lai, ma force-stop giua phien lai lam app tam
            # dung de giu gio - khong bao gio thay duoc canh dang choi.
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.dat("xoaluot"), m.dat("duyet", phut=30),
                           m.man("HomeActivity"), m.bam("Bắt đầu chơi")),
            cho=["Đang được chơi"],
        ),
        dict(
            ma="cong-giu", nhom="Cổng & giờ chơi",
            ten="Đang chơi mà app bị tắt — không được mất giờ",
            # Canh nay quan trong hon "bam nut tam dung": he thong giet app la chuyen
            # xay ra that tren tablet, va neu gio choi mat theo thi Le Hoa mat phut
            # da doi duoc bang bai lam.
            #
            # Khong doi app phai TAM DUNG hay phai CHAY TIEP - ca hai deu chap nhan
            # duoc. Cai duy nhat khong duoc phep la phien bien mat, nen o day chi
            # soat mot dieu: man hinh khong duoc quay ve "chua nop bai".
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.dat("xoaluot"), m.dat("duyet", phut=30),
                           m.man("HomeActivity"), m.bam("Bắt đầu chơi"),
                           m.tat_mo_lai()),
            khong=["Chưa nộp bài", "Đang khoá"],
        ),

        # ---------------- man chan theo thoi khoa bieu ----------------
        dict(
            ma="chan-sang", nhom="Màn chắn",
            ten="Sáng thứ sáu — màn chắn, nhắc mang AVNN và Tin học",
            lam=lambda m: (m.van(THU_SAU_SANG), m.nen()),
            luc=THU_SAU_SANG, loai="CHAN", noi="chan",
            cho=["Tới giờ đi học rồi", "Sáng thứ sáu", "NHỚ MANG THEO",
                 "AVNN", "Tin học", "Mở lại lúc 10:45"],
        ),
        dict(
            ma="chan-chieu", nhom="Màn chắn",
            ten="Chiều thứ năm — màn chắn tới 17:00",
            lam=lambda m: (m.van(THU_NAM_CHIEU), m.nen()),
            luc=THU_NAM_CHIEU, loai="CHAN", noi="chan",
            cho=["Tới giờ đi học rồi", "Chiều thứ năm", "Mở lại lúc 17:00", "Toán"],
        ),
        dict(
            ma="sap-di-hoc", nhom="Màn chắn",
            ten="Còn 10 phút nữa phải buông máy — thẻ nhắc đếm ngược",
            # The nhac khong focus nen uiautomator khong doc duoc chu cua no; soat
            # bang cua so noi cong voi cau chu ma TinhLoiNhac tra ra.
            lam=lambda m: (m.van(THU_NAM_SAP), m.nen()),
            luc=THU_NAM_SAP, loai="SAP_DI_HOC", noi="the",
            cho_logic=["phải chuẩn bị đi học", "12:45"],
        ),
        dict(
            ma="soan-vo", nhom="Màn chắn",
            ten="Tối, chưa soạn cặp cho hôm sau",
            lam=lambda m: (m.dat("xoasoan"), m.van(TOI_THU_TU), m.nen()),
            luc=TOI_THU_TU, loai="SOAN_VO", noi="the",
            cho_logic=["Soạn tập vở cho"],
        ),
        dict(
            ma="chu-nhat", nhom="Màn chắn",
            ten="Chủ nhật — không được chắn, nhưng vẫn nhắc soạn cặp",
            # Khong doi "khong che gi": chu nhat van con the nhac soan cap cho sang
            # thu hai, dung nhu dai thoi gian o tab Lich. Cai phai khong co la MAN
            # CHAN - do moi la cai giam Le Hoa o nha vao ngay nghi.
            lam=lambda m: (m.van(CHU_NHAT), m.nen()),
            luc=CHU_NHAT, loai="SOAN_VO", noi_khong="chan",
            khong=["Tới giờ đi học rồi"],
        ),
        dict(
            ma="chan-tat", nhom="Màn chắn",
            ten="Tắt công tắc màn chắn thì giờ học cũng không chắn",
            lam=lambda m: (m.dat("manchan", ma="0"), m.van(THU_SAU_SANG), m.nen()),
            noi_khong="chan",
            khong=["Tới giờ đi học rồi"],
            don=lambda m: m.dat("manchan", ma="1"),
        ),

        # ---------------- cac man hinh ----------------
        dict(
            ma="man-lich", nhom="Màn hình",
            ten="Màn thời khoá biểu",
            lam=lambda m: (m.van(RANH), m.man("LichActivity")),
            cho=["Lớp 8A15"],
        ),
        dict(
            ma="man-soan", nhom="Màn hình",
            ten="Màn soạn tập vở",
            lam=lambda m: (m.dat("xoasoan"), m.man("SoanActivity")),
            cho=["Soạn tập cho"],
        ),
        dict(
            ma="man-chat", nhom="Màn hình",
            ten="Màn nhắn tin cho ba Huy",
            lam=lambda m: (m.man("ChatActivity"),),
            cho=["Có việc gì cần báo ba Huy"],
        ),
        dict(
            ma="man-tin", nhom="Màn hình",
            ten="Màn tin của cô giáo",
            lam=lambda m: (m.dat("tincuaco", chu="Ngày mai kiểm tra 15 phút bài 3"),
                           m.man("TinActivity")),
            cho=["Tin của cô giáo", "kiểm tra 15 phút"],
            don=lambda m: m.dat("xoatin"),
        ),

        # ---------------- viec nha ba noi giao ----------------
        dict(
            ma="viec-chan", nhom="Việc nhà",
            ten="Bà nội giao việc — khoá cả máy tới khi bà bấm xong",
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.dat("viecnha", chu="Quét nhà:10:0,Rửa chén:10:0"),
                           m.nen()),
            noi="chan",
            cho=["Bà nội giao việc nhà", "Quét nhà", "Rửa chén",
                 "Làm xong nhờ bà bấm trên điện thoại của bà"],
        ),
        dict(
            ma="viec-man-chinh", nhom="Việc nhà",
            ten="Màn chính lúc còn việc nhà — kể ra việc nào chưa xong",
            # Man chan nhuong cho chinh app Nop bai, nen day la cho duy nhat con doc
            # duoc con phai lam gi.
            #
            # Khong soat nut to ("Làm xong việc nhà đã"): man hinh nay co luc day du
            # the (on lai, thieu viec, soan cap) day nut xuong duoi vung nhin thay,
            # ma uiautomator chi doc duoc phan dang hien. Soat no thi muc nay dat hay
            # hong tuy vao hom do co bao nhieu the - khong noi len dieu gi ve app.
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.dat("viecnha", chu="Quét nhà:10:0,Rửa chén:10:0"),
                           m.man("HomeActivity")),
            cho=["Bà nội giao việc", "Còn 2 việc", "Quét nhà", "Rửa chén"],
        ),
        dict(
            ma="viec-xong", nhom="Việc nhà",
            ten="Bà bấm xong hết — máy mở ra, không chắn nữa",
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.dat("viecnha", chu="Quét nhà:10:0,Rửa chén:10:0"),
                           m.dat("viecnhaxong"), m.nen()),
            noi_khong="chan",
            khong=["Bà nội giao việc nhà"],
            don=lambda m: m.dat("xoaviecnha"),
        ),

        # ---------------- kho bai, on lai, cac man moi ----------------
        dict(
            ma="on-lai", nhom="Kho bài",
            ten="Câu từng sai đến hẹn ôn lại — hiện trên màn chính",
            lam=lambda m: (m.van(RANH), m.dat("xoaviecnha"), m.dat("dong"),
                           m.dat("xoacho"), m.dat("napdenhen", ma="1.3a"),
                           m.man("HomeActivity")),
            cho=["câu đến hẹn ôn lại", "Ôn lại"],
        ),
        dict(
            ma="man-chonbai", nhom="Kho bài",
            ten="Màn khai bài trước khi chụp",
            lam=lambda m: (m.dat("napdenhen", ma="1.3a"),
                           m.man("ChonBaiActivity")),
            cho=["Con đang làm bài môn gì?", "Toán", "Ôn lại 1 câu đến hẹn"],
        ),
        dict(
            ma="man-tienbo", nhom="Kho bài",
            ten="Màn “Con đã làm được gì”",
            lam=lambda m: (m.chay("ManualTienBo#napThu"), m.man("TienBoActivity")),
            cho=["Con đã làm được gì", "câu đúng", "câu khó đã gỡ"],
        ),
        dict(
            ma="man-thongke", nhom="Kho bài",
            ten="Màn “Dùng app gì, lúc nào” (trang của ba Huy)",
            # Man nay nam sau PIN: khong bat che do ba thi no tu dong dong lai ngay.
            lam=lambda m: (m.chay("ManualThongKe#napThu"), m.dat("bamo", phut=30),
                           m.man("ThongKeActivity")),
            cho=["Dùng app gì, lúc nào"],
            man_tren_cung="ThongKeActivity",
            don=lambda m: m.dat("badong"),
        ),
        dict(
            ma="thongke-khoa", nhom="Kho bài",
            ten="Ba chưa gõ PIN thì màn thống kê không mở được",
            lam=lambda m: (m.dat("badong"), m.man("ThongKeActivity")),
            man_khong="ThongKeActivity",
        ),

        # ---------------- chan app ----------------
        dict(
            ma="chan-app", nhom="Chặn app",
            ten="Cổng đóng thì mở app khác bị đẩy về",
            # Guard day ve man hinh nen chu khong phai lan nao cung mo man hinh cua
            # con, nen chi doi mot dieu: khong con o trong Chrome nua.
            lam=lambda m: (m.van(RANH), m.dat("dong"), m.dat("xoacho"),
                           m.mo_goi("com.android.chrome")),
            man_khong="chrome",
        ),
        dict(
            ma="han-app", nhom="Chặn app",
            ten="Hết hạn giờ riêng của app thì cũng bị đẩy về",
            lam=lambda m: (m.van(RANH), m.dat("xoacho"), m.dat("xoaluot"),
                           m.dat("duyet", phut=30), m.dat("batdau"),
                           m.dat("dathan", goi="com.android.chrome", phut=15),
                           m.dat("dunghet", goi="com.android.chrome"),
                           m.mo_goi("com.android.chrome")),
            man_khong="chrome",
            don=lambda m: (m.dat("xoahan", goi="com.android.chrome"), m.dat("dong")),
        ),
    ]


# Cac lop test JUnit, cung nam trong danh sach chon de "chon tat ca" chay het ca hai
# loai. GateStoreTest de rieng vi no xoa sach cau hinh tren may.
BO_TEST = [
    ("TinhLoiNhacTest", "Lời nhắc theo thời khoá biểu"),
    ("LuatCongGioTest", "Luật cộng giờ"),
    ("SoCaiBaiTest", "Sổ cái bài đã nộp"),
    ("KhoaAiTest", "Chùm khoá AI"),
    ("GioiHanAppTest", "Hạn giờ từng app"),
    ("ChatBoxTest", "Hộp tin nhắn"),
    ("HopThuBaNoiTest", "Hộp thư ba nội"),
    ("BoGoAiTest", "Bộ gõ chữ vào app AI"),
    ("ChamBaiJsonTest", "Đọc JSON chấm bài"),
    ("NganHangTest", "Ngân hàng câu hỏi nạp từ sách"),
    ("NhatKySuDungTest", "Sổ ghi dùng app lúc nào"),
    ("ViecNhaTest", "Việc nhà bà nội giao"),
    ("GateStoreTest", "Cổng — XOÁ SẠCH cấu hình trên máy"),
]
