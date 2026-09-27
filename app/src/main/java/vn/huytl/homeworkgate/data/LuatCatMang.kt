package vn.huytl.homeworkgate.data

/**
 * Luat cat mang cho cac app Ba Huy chon vao danh sach cat mang.
 *
 * Tach khoi VpnService de test duoc bang so, khong phai doi den 22:05 moi biet gio
 * ngu co cat khong. Cung mot loi voi [vn.huytl.homeworkgate.guard.LuatNhac] va
 * [vn.huytl.homeworkgate.guard.LuatManHinh].
 *
 * VI SAO CO RIENG DANH SACH CAT MANG, KHONG DUNG THANG DANH SACH CHAN. Chan man
 * hinh chi tac dong khi app o truoc mat; cat mang tac dong ca khi app chay nen -
 * cua so noi (PiP) cua YouTube, tai ngam. Ba Huy chon rieng nhung app dang cat
 * mang: YouTube, game... chu khong cat het moi app bi khoa, de khong dung nham
 * dich vu he thong.
 *
 * App trong danh sach mat mang dung luc no dang bi khoa: het gio choi, gio di hoc,
 * gio ngu. Con trong gio choi that thi van co mang.
 */
object LuatCatMang {

    /**
     * App trong danh sach cat mang co bi cat mang ngay luc nay khong.
     *
     * @param gateMo      dang co gio choi that (phien dang chay)
     * @param trongGioNgu dang trong khung tu gio di ngu den gio day
     * @param trongGioHoc dang trong buoi hoc bi man chan che
     * @param moToanBo    Ba Huy dang mo toan bo may de dung
     */
    fun catMang(
        gateMo: Boolean,
        trongGioNgu: Boolean,
        trongGioHoc: Boolean,
        moToanBo: Boolean,
    ): Boolean {
        // Ba Huy dang cam may thi khong cat gi ca, y nhu phan chan app va chan nhac.
        if (moToanBo) return false

        // Gio di hoc xet truoc ca gio choi: toi gio buong may di hoc thi cat, du con
        // dang giu phieu gio choi.
        if (trongGioHoc) return true

        // Gio ngu cat ca khi con so phut chua dung: mang ban dem la thu de con nam
        // xem toi mot gio sang nhat.
        if (trongGioNgu) return true

        // Con lai: co gio choi thi co mang, het gio thi cat.
        return !gateMo
    }

    /** Viec phai lam voi VPN sau mot lan xet lai. Xem [viecVpn]. */
    enum class ViecVpn {
        /** Khong con app nao phai cat: go VPN, quen dau bi da va dau hong. */
        TAT,

        /** De nguyen nhu dang co. */
        GIU,

        /** Bat VPN, hay dung lai voi danh sach moi. */
        BAT,
    }

    /**
     * Sau mot lan xet lai thi tat, giu hay bat VPN.
     *
     * Thu tu la thu tu uu tien. Het phai cat thi tat truoc moi thu, ke ca luc dang bi da:
     * do la luc quen dau bi da. Con phai cat ma VPN vua bi app khac chiem thi giu nguyen,
     * khong gianh lai (Ba Huy chon ngay 27/9/2026). Chua cho phep thi bat cung vo ich, con
     * vua hong thi cho het khoang nghi.
     *
     * @param can      nhung app phai cat ngay luc nay
     * @param dangCat  nhung app VPN dang cat, rong la VPN dang tat
     * @param biDa     VPN vua bi app khac chiem hay bi ngat giua buoi khoa nay
     * @param choPhep  Ba Huy da bam OK o hop thoai VPN
     * @param dangNghi vua bat hong, chua het khoang nghi
     */
    fun viecVpn(
        can: Set<String>,
        dangCat: Set<String>,
        biDa: Boolean,
        choPhep: Boolean,
        dangNghi: Boolean,
    ): ViecVpn = when {
        can.isEmpty() -> ViecVpn.TAT
        can == dangCat -> ViecVpn.GIU
        biDa || !choPhep || dangNghi -> ViecVpn.GIU
        else -> ViecVpn.BAT
    }
}
