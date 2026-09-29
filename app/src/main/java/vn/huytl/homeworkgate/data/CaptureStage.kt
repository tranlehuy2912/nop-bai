package vn.huytl.homeworkgate.data

import vn.huytl.homeworkgate.R

/**
 * Hai nhom anh trong mot lan nop bai, dung thu tu con chup tren man hinh.
 *
 * Chi [BAI_GIAI] la bat buoc: nhieu hom de in san co san o ghi bai ngay duoi,
 * chup mot tam la co ca hai, bat chup du hai nhom chi lam con chup thua.
 *
 * Vo dan do khong con la mot nhom o day: tu 6d4f7d3 no chi chup o man vo dan do. Anh
 * trang vo gan vao bai mang khau [KHAU_DAN_DO], ten Bang dieu khien van doc.
 *
 * De o day chu khong nam trong man chup, vi ca phan gui len Telegram lan bai test
 * deu can biet cac nhom nay.
 */
enum class CaptureStage(val labelRes: Int, val hintRes: Int, val required: Boolean) {
    DE_BAI(R.string.stage_problem, R.string.stage_problem_hint, false),
    BAI_GIAI(R.string.stage_solution, R.string.stage_solution_hint, true)
}
