package vn.huytl.homeworkgate.data

import vn.huytl.homeworkgate.R

/**
 * Ba nhom anh trong mot lan nop bai, dung thu tu con chup tren man hinh.
 *
 * Chi [BAI_GIAI] la bat buoc: nhieu hom de in san co san o ghi bai ngay duoi,
 * chup mot tam la co ca hai, bat chup du ba nhom chi lam con chup thua.
 *
 * De o day chu khong nam trong man chup, vi ca phan gui len Telegram lan bai test
 * deu can biet ba nhom nay.
 */
enum class CaptureStage(val labelRes: Int, val hintRes: Int, val required: Boolean) {
    DAN_DO(R.string.stage_notes, R.string.stage_notes_hint, false),
    DE_BAI(R.string.stage_problem, R.string.stage_problem_hint, false),
    BAI_GIAI(R.string.stage_solution, R.string.stage_solution_hint, true)
}
