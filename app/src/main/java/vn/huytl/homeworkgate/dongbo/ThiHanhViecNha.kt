package vn.huytl.homeworkgate.dongbo

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.data.DayLog
import vn.huytl.homeworkgate.data.GateState
import vn.huytl.homeworkgate.data.GateStore
import vn.huytl.homeworkgate.data.ViecNha
import vn.huytl.homeworkgate.telegram.ApprovalService
import vn.huytl.homeworkgate.telegram.Notifier

/**
 * Ap ban viec nha vua dat xuong hop/viecnha, tu may ba noi hay tu Bang dieu khien.
 *
 * Doi song doi voi [ThiHanhLenh], va co chu tach ra: lenh la su kien, lam xong thi
 * xoa document di; viec nha la trang thai, document nam nguyen do va bi ghi de moi
 * lan ba bam. Nhet ca hai vao mot ham thi cai vong "lam xong roi xoa" se an mat
 * ban trang thai dang con hieu luc.
 *
 * DOC LAI CUNG MOT BAN BAO NHIEU LAN CUNG KHONG SAO. [ViecNha.apDung] so voi ban
 * dang giu trong may roi moi quyet co gi de lam khong, nen listener bay lai vi doi
 * mang hay vi khoi dong lai app cung khong sinh ra hai lan cong gio.
 */
object ThiHanhViecNha {

    private const val TAG = "ViecNha"

    fun lam(context: Context, d: DocumentSnapshot?) {
        val moi = doc(d) ?: return

        /*
         * Dot nay khep xong tu truoc roi, chi la lan xoa truoc khong di duoc.
         *
         * Xoa lai roi thoi. Khong co dong nay thi mot lan mat mang dung nhip xoa la
         * lan doc sau cong gio them mot lan nua - tablet da bo ban trong may di nen
         * no khong con gi de so, va mot dot DA XONG HET trong y nhu dot moi giao.
         */
        if (moi.id == ViecNha.daKhep(context)) {
            donBan(d, moi.id)
            return
        }

        /*
         * Ban qua cu thi bo, y het [DongBo.quaCu] ben lenh.
         *
         * Chi chan luc BAT DAU mot phien moi. Ban cap nhat cua phien dang chay thi
         * luon nhan, du gio nao: no co the la cai bam "da xong" cuoi cung, ma luc do
         * con dang ngoi truoc man hinh khoa cho duoc mo ra.
         */
        val dangCo = ViecNha.dangTreo(context)
        if (dangCo?.id != moi.id && moi.nhanLuc > 0L && DongBo.quaCu(moi.nhanLuc)) {
            Log.i(TAG, "bo phien ${moi.id}: ba giao tu lau qua")
            return
        }

        val gate = GateStore(context)
        val con = context.getString(R.string.child_name)

        when (ViecNha.apDung(context, moi)) {
            ViecNha.Doi.MOI -> {
                val ke = moi.chuaXong.joinToString(", ") { it.ten }
                val nguoi = ViecNha.nguoiGiao(moi)
                DayLog.add(context, "$nguoi giao việc nhà: $ke")
                // Dang choi thi giu gio lai chu khong cat: so phut do la do lam bai
                // ma co, khong lien quan gi den viec nha.
                gate.pause()
                Notifier.send(
                    context,
                    "$nguoi giao $con làm việc nhà: $ke. Tablet khoá cho đến khi bấm xong hết."
                )
                // Man chan song trong ApprovalService, ma cong dang khoa thi service
                // do co the da tat. Khong goi dong nay la ba giao viec xong man hinh
                // van mo binh thuong. Xet lai ngay: service dang chay ma khong co gi
                // chan thi vong xet cua no dang ngu mot phut mot nhip.
                ApprovalService.ensureRunning(context, xetLaiNgay = true)
                Log.i(TAG, "${moi.ai} giao ${moi.cac.size} viec")
            }

            ViecNha.Doi.BOT_MOT_VIEC ->
                DayLog.add(context, "Việc nhà còn: " + moi.chuaXong.joinToString(", ") { it.ten })

            ViecNha.Doi.XONG_HET -> {
                val phut = moi.tongPhut
                val ke = moi.cac.joinToString(", ") { it.ten }
                ViecNha.xoa(context)
                ViecNha.ghiDaKhep(context, moi.id)
                donBan(d, moi.id)
                Notifier.send(
                    context,
                    "$con làm xong việc nhà ($ke), được $phut phút."
                )
                congGio(context, gate, phut, ke)
                Log.i(TAG, "xong het, cong $phut phut")
            }

            ViecNha.Doi.BO_HET -> {
                // Khong biet ai bam bo, chi biet ai giao: document chi ghi nguoi giao.
                DayLog.add(context, "Bỏ hết việc nhà đã giao")
                ViecNha.ghiDaKhep(context, moi.id)
                donBan(d, moi.id)
                Log.i(TAG, "bo het")
            }

            ViecNha.Doi.KHONG_DOI -> return
        }
        DongBo.dayNgay()
    }

    /**
     * Xoa document sau khi mot dot viec da khep lai.
     *
     * PHAI XOA, khong phai don cho sach. Hai viec cung mot luc:
     *
     * MOT, chan cong gio hai lan. Xong het thi [ViecNha.xoa] bo ban trong may di,
     * nhung document van nam nguyen tren Firestore - va listener thi giao lai ban
     * hien tai moi lan no gan vao, tuc la moi lan app khoi dong hay dich vu tro nang
     * song lai. Lan do tablet khong con gi de so, nen mot dot DA XONG HET trong nhu
     * mot dot moi vua giao xong ngay. Chan "ban qua cu" o tren chi che duoc sau nua
     * tieng, ma khoi dong lai trong vong nua tieng la chuyen thuong.
     *
     * HAI, day la cau tra loi gui cho may ba noi. Ben do giu nguyen dot viec cho den
     * khi document bien mat, vi "ghi len Firestore xong" khong co nghia la "tablet da
     * nhan va cong gio" - tablet co the dang tat. Con document nghia la chua ai nhan,
     * va ba con nut de gui lai.
     *
     * XOA CO DIEU KIEN, khong xoa thang. hop/viecnha la mot duong dan co dinh: ba giao
     * dot moi dung luc tablet dang xu ly dot cu thi lenh xoa roi trung dot moi. Chi
     * mot nhip do thoi, nhung hau qua nang - ba thay document bien mat, tuong tablet
     * da nhan, trong khi tablet dang khoa may vi dot moi ma khong ai con gui lai duoc.
     */
    private fun donBan(d: DocumentSnapshot?, maPhien: String) {
        val o = d?.reference ?: return
        o.firestore.runTransaction { tr ->
            val nay = tr.get(o)
            if (nay.exists() && nay.getString(Duong.F_MA_PHIEN) == maPhien) tr.delete(o)
            null
        }.addOnFailureListener { Log.w(TAG, "khong xoa duoc ban viec nha", it) }
    }

    /**
     * Cong so phut lam duoc va ghi nhat ky.
     *
     * KHONG PHAI RIENG TU: [ThiHanhLenh] goi vao day khi Bang dieu khien go lenh
     * [Lenh.CONG_VIEC_NHA] - tuc la khi tablet da bo lo mot dot va Ba Huy cong bu.
     * Chung mot ham de dong nhat ky chi co dung mot cho sinh ra; hai cho ghi hai cau
     * gan giong nhau thi doc lai khong biet do la mot viec hay hai viec khac nhau.
     *
     * Khong tru vao tran phut moi ngay: day la gio lam ra bang viec that trong nha,
     * khong phai suat choi cua ngay hom do. Cung ly do voi gio Ba Huy chu dong cho.
     *
     * Tra ve so phut cong duoc, hay null khi khong cap duoc - dang gio ngu chang han.
     * Ca hai truong hop deu de lai mot dong nhat ky: bo qua im lang thi sau nay khong
     * ai doi chieu duoc "chau lam xong ma sao khong thay gio".
     */
    fun congGio(context: Context, gate: GateStore, phut: Int, ke: String): Int? {
        if (phut <= 0) {
            DayLog.add(context, "Xong việc nhà ($ke)")
            return 0
        }
        val duoc = if (gate.state == GateState.ACTIVE) {
            gate.extend(phut)
        } else {
            gate.approve(
                wantedMinutes = phut,
                useQuota = false,
                // Khong ghi "cho ba": Ba Huy cung giao viec nha, va lenh cong bu
                // CONGVIECNHA thi khong biet dot do ai giao.
                nhanCho = "Làm việc nhà"
            )
        }
        if (duoc == null) {
            DayLog.add(context, "Xong việc nhà ($ke) nhưng chưa cộng được $phut phút")
            return null
        }
        DayLog.add(context, "Xong việc nhà ($ke): +$phut phút")
        // Ghi vao so ngay de man bang gia tra loi duoc "hom nay phan viec nha cong
        // chua". Ghi so XIN chu khong phai so [duoc] tra ve: duoc la tong con lai
        // cua ca phien, gom ca gio kiem bang bai tap tu truoc do.
        ViecNha.congPhutHomNay(context, phut)
        ApprovalService.ensureRunning(context)
        return duoc
    }

    /**
     * Doc document thanh mot phien.
     *
     * Document khong ton tai, hay danh sach viec rong ma chua tung co phien nao, thi
     * tra ve null - khong co gi de lam. Danh sach rong TRONG khi dang co phien thi
     * khac: do la ba bo het viec, va [ViecNha.apDung] phai duoc nhin thay no.
     */
    private fun doc(d: DocumentSnapshot?): ViecNha.Phien? {
        if (d == null || !d.exists()) return null
        val ma = d.getString(Duong.F_MA_PHIEN).orEmpty()
        val cac = (d.get(Duong.F_VIEC) as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
        // Doc bang get chu khong bang getString: getString nem loi khi truong khong
        // phai chuoi, va mot truong phu thi khong dang de ca dot viec bi bo.
        val ai = d.get(Duong.F_AI) as? String ?: Nguoi.BA_NOI
        return ViecNha.tuBan(ma, cac, d.getLong(Duong.F_LUC) ?: 0L, ai)
    }
}
