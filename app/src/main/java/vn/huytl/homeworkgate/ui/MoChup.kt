package vn.huytl.homeworkgate.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.homeworkgate.R
import vn.huytl.homeworkgate.kho.CauHoi
import vn.huytl.homeworkgate.kho.KhoBai
import vn.huytl.homeworkgate.kho.PhamVi

/**
 * Mo man chup cho mot lan nop, theo dung duong chup anh: hoi cau nao con thay chua chac, roi
 * mo [CaptureActivity]. Tu do anh len Telegram, Ba Huy nho Claude cham o Bang dieu khien.
 *
 * TACH RA NGAY 2/10/2026 tu [ChonBaiActivity], de ba cho mo camera di chung mot duong: man khai
 * bai "Bài khác", man chon cau cua bai tap SGK khi con chi chon cau phai viet
 * ([BaiSgkActivity]), va nut chup cau phai viet sau khi nop bai SGK ([GiaiDeActivity]).
 */
object MoChup {

    /**
     * Nhieu hon bay nhieu cau thi khong hoi "chua chac" nua.
     *
     * Mot trang trac nghiem ba muoi cau ma bat tich tung cau la mot cai cua ai
     * dung truoc man chup, va con se bam bua cho xong.
     */
    private const val MAX_HOI_CHAC = 12

    /**
     * Truoc khi chup, hoi mot cau: cau nao con thay chua chac.
     *
     * VI SAO HOI. Cham bai cho biet con lam dung hay sai. Cau hoi nay cho biet mot
     * thu khac va kho hon: con co BIET minh dang biet gi khong. Cau bao chac ma sai
     * la cho nguy hiem nhat - con se khong quay lai xem no nua. Cau bao chua chac ma
     * dung thi nguoc lai, do la cho con dang tu danh gia thap minh.
     *
     * KHONG DINH GI DEN SO PHUT, va phai giu dung nhu vay. Gan thuong vao "doan
     * dung" thi lan sau con khai theo cai co loi chu khong theo cai no nghi, va cau
     * hoi mat sach gia tri.
     *
     * Chi hoi khi danh sach con ngan. Mot trang trac nghiem ba muoi cau ma bat tich
     * tung cau thi cau hoi tot den may cung thanh mot cai cua ai.
     *
     * @param truocKhiMo goi ngay truoc khi mo camera, de man goi biet la con da di chup.
     */
    fun mo(activity: AppCompatActivity, pham: PhamVi, truocKhiMo: () -> Unit = {}) {
        if (pham.theoSach && pham.cauIds.size in 1..MAX_HOI_CHAC) {
            return hoiChuaChac(activity, pham, truocKhiMo)
        }
        moCamera(activity, pham, truocKhiMo)
    }

    /**
     * Tich nhung cau con thay chua chac.
     *
     * Danh sach dung chinh o tich cua man ben ngoai chu khong dung
     * setMultiChoiceItems: dong mac dinh cua hop thoai cat de bai o dong thu hai, ma
     * de toan thi phan quan trong hay nam o cuoi. Con phai doc duoc ca cau moi biet
     * minh chac hay khong.
     *
     * MOT NUT DI TIEP. Truoc day o day co hai nut - "Chup bai" va "Con chac het" -
     * ma khong tich gi roi bam cai nao cung ra ket qua y het nhau. Khong tich gi da
     * co nghia la con tu tin dung het, khong can mot nut rieng de noi dieu do.
     */
    private fun hoiChuaChac(activity: AppCompatActivity, pham: PhamVi, truocKhiMo: () -> Unit) {
        val cac = KhoBai.get(activity).cacCauTheoId(pham.cauIds)
        if (cac.isEmpty()) return moCamera(activity, pham, truocKhiMo)
        val tick = BooleanArray(cac.size)

        val cot = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val p = (12 * activity.resources.displayMetrics.density).toInt()
            setPadding(p, 0, p, 0)
        }

        cac.forEachIndexed { i, cau ->
            val o = LayoutInflater.from(activity)
                .inflate(R.layout.st_dong_cau, cot, false) as MaterialCheckBox
            o.text = dongCau(cau)
            o.setOnCheckedChangeListener { _, c -> tick[i] = c }
            cot.addView(o)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Trong những câu dưới đây, có câu nào Lê Hòa không tự tin làm đúng không?")
            .setView(ScrollView(activity).apply { addView(cot) })
            .setPositiveButton("Chụp bài để gửi") { _, _ ->
                moCamera(
                    activity,
                    pham.copy(
                        chuaChac = cac.filterIndexed { i, _ -> tick[i] }.map { it.id },
                        daKhaiChac = true
                    ),
                    truocKhiMo
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun moCamera(context: Context, pham: PhamVi, truocKhiMo: () -> Unit) {
        truocKhiMo()
        context.startActivity(
            Intent(context, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_PHAM, pham.sangJson())
        )
    }

    /**
     * Mot dong trong danh sach cau: nhan to dam o tren, de bai o duoi.
     *
     * TO DAM CHU KHONG THEM DAU NGAN. Hai phan nay khac loai han nhau - nhan la cho
     * TIM trong sach, de bai la cai PHAI LAM - nen cho chung khac nhau ve net chu thi
     * mat nhin ra ngay, khong phai doc mot dau cham giua roi tu hieu. Doi lai, mot ky
     * tu ngan nam giua hai doan chu dai thi lot thom, va do la ban truoc.
     *
     * Dam cua nhan lay do dai tu chinh [CauHoi.nhan] chu khong di tim ky tu xuong
     * dong: de bai cua vai cau co san dau xuong dong trong do, tim ky tu thi to dam
     * nham ca doan dau cua de.
     */
    fun dongCau(
        cau: CauHoi,
        them: SpannableStringBuilder.() -> Unit = {}
    ): CharSequence =
        // So mu doi ngay luc hien, chu trong kho van giu dau "^". Xem [SoMu].
        SpannableStringBuilder(SoMu.hienDe(cau.dongChon(), cau.mon)).apply {
            setSpan(
                StyleSpan(Typeface.BOLD), 0, SoMu.hien(cau.nhan()).length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            them()
        }
}
