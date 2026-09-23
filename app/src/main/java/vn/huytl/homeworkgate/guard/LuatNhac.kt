package vn.huytl.homeworkgate.guard

/** Mot goi dang phat tieng thi duoc phat tiep hay phai dung. */
enum class XuLyNhac { CHO_PHAT, DUNG }

/**
 * Luat cho tieng phat ra khi con khong ngoi trong app.
 *
 * Tach khoi dich vu canh app de test duoc bang so, khong phai dung tablet that vao
 * luc 22:05 moi biet luat gio ngu co chay khong.
 *
 * VI SAO CAN RIENG MOT LUAT. Phan chan app chi nhin duoc cai dang hien tren man
 * hinh. Nhac thi khong hien gi ca: con mo Spotify trong gio choi, bam tam dung
 * phien de giu lai so phut, roi dieu khien tiep bang the nhac trong thanh thong
 * bao. Luc do khong co cua so nao de chan, dong ho phien thi dang dung, ma nhac
 * van chay - tuc la nghe mien phi ca buoi toi.
 */
object LuatNhac {

    /**
     * @param laAppNhac     goi nay nam trong danh sach app duoc nghe nen
     * @param duocKhiHetGio goi nay nam trong danh sach trang (tu dien, app hoc)
     * @param hetHanNgay    da het so phut rieng cua goi nay hom nay
     * @param trongGioNgu   dang trong khung tu gio di ngu den gio day
     * @param trongGioHoc   dang trong buoi hoc bi man chan che
     * @param congMo        phien choi dang chay, tuc la con van con phut
     */
    fun xet(
        laAppNhac: Boolean,
        duocKhiHetGio: Boolean,
        hetHanNgay: Boolean,
        trongGioNgu: Boolean,
        trongGioHoc: Boolean,
        congMo: Boolean,
    ): XuLyNhac {
        // Gio di hoc xet truoc moi thu, giong het phan chan app: man chan da che kin
        // man hinh roi ma trong tai van co nhac thi buc tuong do chi con mot nua.
        if (trongGioHoc) return XuLyNhac.DUNG

        // Han rieng cua app xet truoc ca gio choi. Cung mot cau voi phan chan app:
        // mot ngay bao nhieu phut la bay nhieu, du hom do co gio choi hay khong.
        if (hetHanNgay) return XuLyNhac.DUNG

        if (laAppNhac) {
            // Qua gio di ngu thi im, ke ca khi so phut trong ngay con nguyen. Nhac de
            // ngu la cai co ve vo hai nhat trong nha nay, va cung la cai de con nam
            // nghe den mot gio sang nhat.
            return if (trongGioNgu) XuLyNhac.DUNG else XuLyNhac.CHO_PHAT
        }

        // Khong phai app nhac: con dang co gio choi thi phat gi cung duoc, het gio
        // thi chi app trong danh sach trang con duoc keu (app hoc tieng Anh khong
        // doc thanh tieng thi coi nhu hong).
        return if (congMo || duocKhiHetGio) XuLyNhac.CHO_PHAT else XuLyNhac.DUNG
    }
}
