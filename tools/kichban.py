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
THU_SAU_SANG = "2026-09-18T09:00"   # dang trong buoi sang thu sau (vao hoc 09:15)
THU_NAM_CHIEU = "2026-09-17T13:00"  # dang trong buoi chieu thu nam
THU_NAM_SAP = "2026-09-17T11:20"    # con 10 phut nua phai buong may
TOI_THU_TU = "2026-09-16T20:00"     # toi, chua soan cap cho hom sau
CHU_NHAT = "2026-09-20T10:00"       # nghi, khong duoc chan gi

# Moc "ranh": toi thu tu, da tan hoc, chua toi gio ngu. Moi muc khong dinh den thoi
# khoa bieu deu neo vao day chu khong dung gio that cua may Mac.
#
# Dung gio that thi bo thu chay luc nao ra ket qua luc do: chay vao mot chieu trong
# tuan la man chan phu kin man hinh, va het moi muc ve cong deu bao hong - trong khi
# app khong sai gi ca. Da dinh mot lan nhu vay that, luc 13:34 chieu thu tu.
RANH = "2026-09-16T19:30"

# Bai 9 cua bo Cong thuc Toan 8 (assets/hocthuoc/toan8ct.json). Luc dat hang nay no la bai
# cuoi, chon no la hoi ca bo; nay bo co toi Bai 39, va tu 30/9/2026 Toan tach hai phan nen
# dat moc Bai 9 la Dai so toi Bai 9, Hinh hoc chua hoc. Doi ten bai trong file thi sua o day.
BAI_CUOI_TOAN = "Bài 9. Phân tích đa thức thành nhân tử"

# Bai dau cua bo Toan, cho muc go dung mot cau. Trong Bai 1 phan lon dap an la so va
# bieu thuc ("−6", "x³y"), thu go duoc bang adb; tu Bai 2 tro di la chu co dau, adb
# khong go duoc. Xem May.go_dap_an_dung trong web.py.
BAI_DAU_TOAN = "Bài 1. Đơn thức"


def danh_sach():
    return [
        # ---------------- cong va gio choi ----------------
        dict(
            ma="cong-khoa", nhom="Cổng & giờ chơi",
            ten="Chưa nộp bài — máy khoá",
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho,xoaluot"), m.man("HomeActivity")),
            cho=["Chưa nộp bài", "Đang khoá"],
        ),
        dict(
            ma="cong-cho", nhom="Cổng & giờ chơi",
            ten="Nộp rồi, đang chờ ba Huy duyệt",
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho"), m.dat("themcho", ma="thu-tudong"), m.man("HomeActivity")),
            cho=["Chờ ba Huy duyệt"],
        ),
        dict(
            ma="cong-duyet", nhom="Cổng & giờ chơi",
            ten="Ba duyệt rồi, chưa bấm Bắt đầu",
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho,xoaluot"), m.dat("duyet", phut=30),
                           m.man("HomeActivity")),
            # Khong soat chu tren nut to: no nam cuoi trang va bi day khoi vung
            # nhin thay khi man hinh dang co nhieu the. Soat phan the trang thai va dong
            # "Hôm nay kiếm được 30/... phút (chưa chơi 30)" - do moi la cho noi "phieu con
            # nguyen, chua bam". Cau "Chưa tính giờ đâu" da bo khoi the tu 4110de0 (27/9/2026).
            cho=["Ba Huy đã duyệt", "30 phút", "chưa chơi 30"],
        ),
        dict(
            ma="cong-choi", nhom="Cổng & giờ chơi",
            ten="Đang được chơi, đồng hồ chạy",
            # Bam nut THAT chu khong goi start() qua ManualBang: dat qua do thi phai
            # force-stop cho app doc lai, ma force-stop giua phien lai lam app tam
            # dung de giu gio - khong bao gio thay duoc canh dang choi.
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho,xoaluot"), m.dat("duyet", phut=30),
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
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho,xoaluot"), m.dat("duyet", phut=30),
                           m.man("HomeActivity"), m.bam("Bắt đầu chơi"),
                           m.tat_mo_lai()),
            khong=["Chưa nộp bài", "Đang khoá"],
        ),

        # ---------------- man chan theo thoi khoa bieu ----------------
        dict(
            ma="chan-sang", nhom="Màn chắn",
            ten="Sáng thứ sáu — màn chắn, nhắc mang Tin học",
            lam=lambda m: (m.van(THU_SAU_SANG), m.nen()),
            luc=THU_SAU_SANG, loai="CHAN", noi="chan",
            cho=["Tới giờ đi học rồi", "Sáng thứ sáu", "NHỚ MANG THEO",
                 "Tin học", "Mở lại lúc 10:45"],
            khong=["AVNN"],
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
            ten="Tối, chưa soạn tập cho hôm sau",
            lam=lambda m: (m.dat("xoasoan"), m.van(TOI_THU_TU), m.nen()),
            luc=TOI_THU_TU, loai="SOAN_VO", noi="the",
            cho_logic=["Soạn tập cho"],
        ),
        dict(
            ma="chu-nhat", nhom="Màn chắn",
            ten="Chủ nhật — không được chắn, nhưng vẫn nhắc soạn tập",
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
            ten="Màn soạn tập",
            lam=lambda m: (m.dat("xoasoan"), m.man("SoanActivity")),
            cho=["Soạn tập cho"],
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
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho"), m.dat("viecnha", chu="Quét nhà:10:0,Rửa chén:10:0"),
                           m.nen()),
            noi="chan",
            # Khong soat cau nho bam Xong nua: tu 4110de0 (27/9) man chan bo dong do,
            # chi con ai giao va viec nao con phai lam.
            cho=["giao việc nhà", "Quét nhà", "Rửa chén"],
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
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho"), m.dat("viecnha", chu="Quét nhà:10:0,Rửa chén:10:0"),
                           m.man("HomeActivity")),
            cho=["Bà nội giao việc", "Còn 2 việc", "Quét nhà", "Rửa chén"],
        ),
        dict(
            ma="viec-xong", nhom="Việc nhà",
            ten="Bà bấm xong hết — máy mở ra, không chắn nữa",
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho"), m.dat("viecnha", chu="Quét nhà:10:0,Rửa chén:10:0"),
                           m.dat("viecnhaxong"), m.nen()),
            noi_khong="chan",
            khong=["Bà nội giao việc nhà"],
            don=lambda m: m.dat("xoaviecnha"),
        ),

        # ---------------- kho bai, on lai, cac man moi ----------------
        dict(
            ma="on-lai", nhom="Kho bài",
            ten="Câu từng sai đến hẹn ôn lại — hiện trên màn chính",
            lam=lambda m: (m.van(RANH), m.dat("xoaviecnha,dong,xoacho"), m.dat("napdenhen", ma="1.3a"),
                           m.man("HomeActivity")),
            cho=["Ôn lại 1 câu đến hẹn"],
        ),
        dict(
            ma="man-chonbai", nhom="Kho bài",
            ten="Màn khai bài trước khi chụp",
            lam=lambda m: (m.van(RANH), m.man("ChonBaiActivity")),
            # Soat phan khong doi: ten con la cau hinh, doi ten trong cai dat thi
            # khong co nghia la man hinh hong. Dong on lai khong con o man nay, tu
            # 30/9/2026 no nam o trang Luyen tap.
            cho=["đang làm bài môn gì?", "Chọn môn rồi chọn bài", "Toán"],
        ),
        dict(
            ma="on-vao-thang", nhom="Kho bài",
            ten="Vào thẳng màn ôn — liệt kê đúng câu đến hẹn",
            # Nut "Ôn lại" o man chinh mo ChonBaiActivity kem EXTRA_ON_TAP, vao thang
            # buoc on chu khong bat chon lai mon va bai.
            lam=lambda m: (m.van(RANH), m.dat("napdenhen", ma="1.3a"),
                           m.man("ChonBaiActivity", thu={"on_tap": True})),
            cho=["1.3a"],
            man_tren_cung="ChonBaiActivity",
        ),
        dict(
            ma="on-bam-nut", nhom="Kho bài",
            ten="Bấm nút “Ôn lại” trên màn chính thì sang màn ôn",
            lam=lambda m: (m.van(RANH), m.dat("xoaviecnha,dong,xoacho"), m.dat("napdenhen", ma="1.3a"),
                           m.man("HomeActivity"), m.bam("Ôn lại")),
            man_tren_cung="ChonBaiActivity",
            cho=["1.3a"],
        ),
        dict(
            ma="man-tienbo", nhom="Kho bài",
            ten="Màn “Con đã làm được gì”",
            lam=lambda m: (m.chay("ManualTienBo#napThu"), m.man("TienBoActivity")),
            # Tieu de co ten con trong do, ma ten la cai dat chu khong phai hanh vi.
            # Khoi "câu khó đã gỡ" nam duoi bieu do nen khong phai luc nao cung lot
            # vao vung nhin thay - soat phan tren cung.
            cho=["đã làm được gì", "câu đúng", "ngày có nộp bài",
                 "SỐ CÂU ĐÚNG MỖI NGÀY"],
        ),
        dict(
            ma="man-thongke", nhom="Kho bài",
            ten="Màn “Thời gian dùng app” (trang của ba Huy)",
            # Man nay nam sau PIN: khong bat che do ba thi no tu dong dong lai ngay.
            lam=lambda m: (m.chay("ManualThongKe#napThu"), m.dat("bamo", phut=30),
                           m.man("ThongKeActivity")),
            cho=["Thời gian dùng app"],
            man_tren_cung="ThongKeActivity",
            don=lambda m: m.dat("badong"),
        ),
        dict(
            ma="trang-ba-hoi-pin", nhom="Kho bài",
            ten="Mở trang của ba thì phải hỏi lại mã PIN",
            # Truoc day ThongKeActivity tu chan bang PIN. Tu ban 4742134 cho chan do
            # don len ParentActivity - moi duong vao cac trang cua ba deu qua day,
            # va roi app mot luc la no hoi lai. Nen soat o dung cho moi.
            lam=lambda m: (m.van(RANH), m.dat("badong"), m.man("ParentActivity")),
            cho=["Nhập mã PIN", "Gõ lại mã PIN để làm tiếp"],
        ),
        dict(
            ma="man-cachkiemgio", nhom="Kho bài",
            ten="Màn “Cách kiếm giờ chơi” — kể ra từng đường đổi giờ",
            lam=lambda m: (m.van(RANH), m.man("CachKiemGioActivity")),
            # Chi soat phan dau danh sach: may duong con lai nam duoi vung nhin
            # thay, ma uiautomator chi doc duoc phan dang hien.
            cho=["Cách kiếm giờ chơi", "Hôm nay kiếm được", "LÀM GÌ THÌ ĐƯỢC THÊM GIỜ",
                 "Chụp bài cô giao", "45 phút"],
        ),

        # ---------------- vo dan do ----------------
        # Tu 30/9/2026 man vo dan do khong mo thang camera nua: con chon ngay ghi tren vo
        # truoc (mac dinh hom nay, o day la ngay RANH 16/9), roi moi bam chup.
        dict(
            ma="man-dando", nhom="Vở dặn dò",
            ten="Màn vở dặn dò hỏi vở của ngày nào, mặc định hôm nay",
            lam=lambda m: (m.van(RANH), m.dat("xoadando"), m.man("DanDoActivity")),
            cho=["VỞ CỦA NGÀY NÀO", "Hôm nay, 16/9/2026", "Chụp vở ngày 16/9"],
            khong=["VỞ ĐANG NHẮC BÀI"],
        ),
        dict(
            ma="dando-camera", nhom="Vở dặn dò",
            ten="Bấm chụp vở thì mở camera chụp trang vở dặn dò",
            lam=lambda m: (m.van(RANH), m.dat("xoadando"), m.man("DanDoActivity"),
                           m.bam("Chụp vở ngày 16/9")),
            man_tren_cung="CaptureActivity",
            cho=["Chụp trang vở dặn dò", "Đưa trang vở lọt vào khung"],
        ),
        # Tu 30/9/2026 dong vo dan do chi con o man chinh: vo khong con dinh gi toi so phut,
        # no de nhac bai (NhacBai), nen man khai bai khong ru chup vo nua. Ten dong luon la
        # "Chụp vở dặn dò"; hom nay da luu mot trang thi co them dong phu "Đã chụp vở hôm
        # nay", xem HomeActivity.veViecHomNay.
        dict(
            ma="dando-chua-chup", nhom="Vở dặn dò",
            ten="Hôm nay chưa lưu vở thì màn chính chưa ghi đã chụp",
            lam=lambda m: (m.van(RANH), m.dat("xoadando"), m.man("HomeActivity")),
            cho=["Chụp vở dặn dò"],
            khong=["Đã chụp vở hôm nay"],
        ),
        # Vo napdando mang ngay RANH (thu Tu 16/9): Toan, KHTN va dong khong co mon han chieu
        # thu Nam 17/9 nen toi nay da nhac; Tieng Anh han chieu thu Bay 19/9, chua toi luc
        # nhac. Soat ca chu tung dong vo: ten dong nhac doi mot lan roi ngay 30/9/2026 ("Bài
        # cho ..." thanh "Bài dặn dò cho ..."), con chu dong vo la chu cua napdando.
        dict(
            ma="dando-da-luu", nhom="Vở dặn dò",
            ten="Lưu vở rồi thì màn chính ghi đã chụp và nhắc bài cho buổi mai",
            lam=lambda m: (m.van(RANH), m.dat("napdando"), m.man("HomeActivity")),
            cho=["Đã chụp vở hôm nay", "Bài dặn dò cho chiều thứ năm",
                 "Toán: làm bài 2.26 và 2.27 trang 45", "KHTN: tiết sau kiểm tra bài 2, bài 3"],
            khong=["Tiếng Anh: làm bài tập Unit 2"],
            don=lambda m: m.dat("xoadando"),
        ),
        # Bam dong "Bài dặn dò cho ..." o man chinh thi mo man nay (30/9/2026). Vo ngay 16/9
        # (thu Tu): Toan, KHTN va dong khong co mon han chieu thu Nam 17/9, Tieng Anh han
        # chieu thu Bay 19/9.
        dict(
            ma="man-nhac-bai", nhom="Vở dặn dò",
            ten="Màn bài dặn dò sắp tới kể mọi buổi còn bài",
            lam=lambda m: (m.van(RANH), m.dat("napdando", ngay="2026-09-16"),
                           m.man("NhacBaiActivity")),
            cho=["Bài dặn dò sắp tới", "Chiều thứ năm 17/9", "Chiều thứ bảy 19/9",
                 "Toán: làm bài 2.26 và 2.27 trang 45 (vở 16/9)"],
            don=lambda m: m.dat("xoadando"),
        ),
        # ---------------- hoc thuoc ----------------
        dict(
            ma="man-hocthuoc", nhom="Học thuộc",
            ten="Màn kiểm tra bài — kể bộ câu và số câu đến lượt",
            # Dat san "da hoc toi bai cuoi" cho bo Toan: tu 25/9/2026 bo chua chon thi
            # khong co so cau den luot nao, chi co dau hoi.
            lam=lambda m: (m.van(RANH), m.dat("hoctoi", ma="toan8ct", chu=BAI_CUOI_TOAN),
                           m.man("HocThuocActivity")),
            # Toan hai phan tu 30/9/2026: moc Bai 9 thi dong moc ghi "Lớp: Đại số tới Bài 9,
            # Hình học chưa học".
            cho=["Kiểm tra bài", "Công thức Toán 8", "câu đến lượt hôm nay",
                 "Đại số tới Bài 9", "Máy chỉ hỏi tới bài lớp đã học"],
        ),
        dict(
            ma="hocthuoc-chua-chon", nhom="Học thuộc",
            ten="Bộ chưa chọn bài đã học — bấm vào là máy hỏi lớp đã học tới đâu",
            lam=lambda m: (m.van(RANH), m.dat("xoahoctoi", ma="toan8ct"),
                           m.man("HocThuocActivity"), m.bam("Công thức Toán 8")),
            cho=["lớp đã học tới bài nào?", "Chưa học tới bài nào", BAI_CUOI_TOAN],
            khong=["Gõ câu trả lời"],
        ),
        dict(
            ma="hocthuoc-dung", nhom="Học thuộc",
            ten="Gõ đúng một câu — máy chấm đúng ngay trên tablet",
            # Go THAT vao o nhap, khong dat san ket qua: ca cai hay cua duong nay la
            # may giu san dap an va cham tai cho, nen phai thu dung cho do. Doc cau
            # hoi dang hien roi tra dap an trong file bo the, chu khong neo cung mot
            # cau - cau den luot doi theo lich on.
            lam=lambda m: (m.van(RANH), m.dat("xoathe", ma="toan8ct"),
                           m.dat("hoctoi", ma="toan8ct", chu=BAI_DAU_TOAN),
                           m.man("HocThuocActivity"),
                           m.bam("Công thức Toán 8"),
                           m.go_dap_an_dung("Gõ câu trả lời",
                                            "app/src/main/assets/hocthuoc/toan8ct.json"),
                           m.bam("Trả lời")),
            cho=["Đúng rồi"],
            khong=["Sai"],
        ),
        dict(
            ma="hocthuoc-sai", nhom="Học thuộc",
            ten="Gõ sai — máy báo Sai và cho một gợi ý",
            lam=lambda m: (m.van(RANH), m.dat("hoctoi", ma="toan8ct", chu=BAI_CUOI_TOAN),
                           m.man("HocThuocActivity"),
                           m.bam("Công thức Toán 8"),
                           m.go_chu("Gõ câu trả lời", "a^2+b^2"),
                           m.bam("Trả lời")),
            # Sai thi khong hien thang dap an nua, chi goi y - va cau do bi day
            # xuong cuoi hang de lat lai sau, chu khong bo qua.
            cho=["Sai", "Gợi ý:"],
            khong=["Đúng rồi"],
        ),

        # ---------------- man moi ----------------
        dict(
            ma="cua-vao", nhom="Màn hình",
            ten="Bấm icon app thì vào thẳng màn chính",
            # CuaVaoActivity khong ve gi, chi day sang HomeActivity roi dong. No sinh
            # ra vi HomeActivity la singleTask: truoc day bam icon la Android dong
            # het moi man dang nam tren no.
            lam=lambda m: (m.van(RANH), m.man("TienBoActivity"), m.bam_icon()),
            man_tren_cung="HomeActivity",
            khong=["Cửa vào"],
        ),
        dict(
            ma="man-dotuvung", nhom="Học thuộc",
            ten="Màn dò từ vựng — kể bộ từ và số từ đã thuộc",
            lam=lambda m: (m.van(RANH), m.man("DoTuVungActivity")),
            cho=["Dò từ vựng", "Từ vựng Tiếng Anh 8", "Đã thuộc",
                 "Máy chỉ hỏi từ trong các Unit lớp đã học"],
        ),
        dict(
            ma="ketqua-trong", nhom="Bài đã chấm",
            ten="Chưa có bài nào chấm thì màn nói thẳng ra",
            lam=lambda m: (m.van(RANH), m.chay("ManualKetQua#xoaThu"),
                           m.man("KetQuaActivity")),
            cho=["Bài đã chấm", "Chưa có bài nào được chấm"],
        ),
        dict(
            ma="ketqua-co-bai", nhom="Bài đã chấm",
            ten="Có bài rồi thì kể từng câu đúng sai",
            # Man nay doc tu Firestore chu khong doc trong may, nen cho lau hon cac
            # man khac. Ban go loi noi vao du an THU nen bai mau khong vao nha that.
            lam=lambda m: (m.van(RANH), m.chay("ManualKetQua#napThu"),
                           m.man("KetQuaActivity")),
            cho=["Bài đã chấm"],
            khong=["Chưa có bài nào được chấm"],
            don=lambda m: m.chay("ManualKetQua#xoaThu"),
        ),
        dict(
            ma="man-giaide", nhom="Bài đã chấm",
            ten="Màn giải đề — một đề ra từ sách bài tập, có đồng hồ",
            lam=lambda m: (m.van(RANH), m.mo_de(),),
            man_tren_cung="GiaiDeActivity",
            don=lambda m: m.chay("ManualGiaiDe#xoa"),
        ),

        # ---------------- chan app ----------------
        dict(
            ma="chan-app", nhom="Chặn app",
            ten="Cổng đóng thì mở app khác bị đẩy về",
            # Guard day ve man hinh nen chu khong phai lan nao cung mo man hinh cua
            # con, nen chi doi mot dieu: khong con o trong Chrome nua.
            lam=lambda m: (m.van(RANH), m.dat("dong,xoacho"), m.mo_goi("com.android.chrome")),
            man_khong="chrome",
        ),
        dict(
            ma="han-app", nhom="Chặn app",
            ten="Hết hạn giờ riêng của app thì cũng bị đẩy về",
            lam=lambda m: (m.van(RANH), m.dat("xoacho,xoaluot"), m.dat("duyet", phut=30), m.dat("batdau"),
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
    # -- luat, khong dung toi may ao ngoai viec chay tren do --
    ("TinhLoiNhacTest", "Lời nhắc theo thời khoá biểu"),
    ("LuatCongGioTest", "Luật cộng giờ"),
    ("LuatNhacTest", "Tiếng phát ra khi con rời app"),
    ("LuatManHinhTest", "Đọc danh sách cửa sổ ra cách xét"),
    ("SoMuTest", "Đổi số mũ khi hiện đề"),
    ("LuyenTheoLoiTest", "Chỗ hay vấp và câu luyện lại"),
    ("LuatCatMangTest", "Luật cắt mạng và việc bật tắt VPN"),
    ("LuatGhepTest", "Luật sao của bài làm trên máy"),
    # -- so sach trong may --
    ("SoCaiBaiTest", "Sổ cái bài đã nộp"),
    ("PhanToanTest", "Toán hai phần Đại số, Hình học"),
    ("CanSuaTest", "Câu cần sửa: hết hạn, sang làm trên máy, Ba Huy bỏ"),
    ("BaiGuiHongTest", "Lần nộp gửi hỏng, giữ ảnh để gửi lại"),
    ("VoDanDoTest", "Trang vở dặn dò đã soát"),
    ("NhacBaiTest", "Nhắc bài theo tiết sau của từng môn"),
    ("NhatKySuDungTest", "Sổ ghi dùng app lúc nào"),
    ("GioiHanAppTest", "Hạn giờ từng app"),
    ("KhoaAiTest", "Chùm khoá AI"),
    ("BoGoAiTest", "Bộ gõ chữ vào app AI"),
    ("NhatKyAiTest", "Sổ câu hỏi AI giữ chỗ xuống dòng, tin /hoi"),
    ("ViecNhaTest", "Việc nhà bà nội giao"),
    ("LuotBaNoiTest", "Một lượt mỗi ngày của bà nội"),
    ("ChatCuTest", "Dọn khung chat cũ khỏi máy"),
    ("CatMangVpnTest", "App nào đang phải cắt mạng"),
    ("QuyGioTest", "Quỹ giờ chơi: vào quỹ, Ba Huy cấp"),
    # -- ngan hang cau hoi --
    ("NganHangTest", "Ngân hàng câu hỏi nạp từ sách"),
    ("NganHangSbtTest", "Ba quyển sách bài tập"),
    ("NganHangVanTest", "Hai quyển Ngữ văn 8"),
    ("GiaiDeTest", "Giải đề và làm thêm theo bài đã học"),
    ("DeThiTest", "Đề thi thử: khung đề, tự mở theo Unit đã học"),
    ("GhepTest", "Câu làm trên máy: gõ ra được, chấm đúng"),
    ("HangSaoTest", "Ba dạng sao: rỗng, xám, vàng"),
    ("KhungGhepTest", "Thứ tự nút: giữ trong một lượt, trộn lại lượt sau"),
    ("LamTrenMayTest", "Bài làm trên máy: chọn câu, làm lại, ôn, cộng phút"),
    ("HocThuocTest", "Đường kiểm tra bài"),
    ("TuVungTest", "Đường từ vựng"),
    # -- cham bai --
    ("ChamTheoClaudeTest", "Bản Claude chấm thành bản chấm của tablet"),
    ("SuaChamTest", "Sửa bản chấm theo Claude"),
    ("BaiDaChamTest", "Trang Bài đã chấm đọc từ Firestore"),
    # -- lenh tu Bang dieu khien, va duong day ra ngoai --
    ("ChamBaiLenhTest", "Lệnh CHAMBAI"),
    ("DuyetTheoMaTest", "Lệnh DUYỆT và TỪ CHỐI kèm mã bài"),
    ("SuaChamLenhTest", "Lệnh SỬA CHẤM"),
    ("CongSangTest", "Giữ phút chấm trong giờ ngủ, cộng lúc hết giờ ngủ"),
    ("TinCoLenhTest", "Lệnh TIN CÔ và kho tin của cô"),
    ("CaiDatLenhTest", "Lệnh CÀI ĐẶT: app nghe nền, giờ riêng từng app"),
    ("BanDaySuDungTest", "Bản số dùng app đẩy sang Bảng điều khiển"),
    ("TelegramThatTest", "Đếm tin ba Huy gửi qua Telegram"),
    ("TrinhPhatTest", "Từ dịch vụ đọc thông báo tới trình phát"),
    # -- giao dien --
    ("XoayManTest", "Các màn khi tablet xoay"),
    # -- cuoi cung: bo nay XOA SACH cau hinh tren may --
    ("GateStoreTest", "Cổng — XOÁ SẠCH cấu hình trên máy"),
]
