package vn.huytl.homeworkgate.data

/**
 * Cau hinh nap san khi cai len mot may chua co gi.
 *
 * Co cai nay de cai lai may khong phai ngoi dien lai tu dau. Chi ap dung cho o
 * nao dang trong: cai de len ban cu thi moi thu Ba Huy da sua van giu nguyen.
 *
 * DANH DOI: dien gi o day thi cai do nam thang trong file APK. Ai lay duoc file
 * APK ra khoi may la doc duoc, va doc duoc ma PIN nghia la vao duoc che do Ba Huy,
 * tuc la mo duoc toan bo may. Duong lay APK khong de: vao Cai dat bi chan, bat go
 * loi USB cung phai qua Cai dat. Nhung neu mot ngay nao do Le Hoa tro nen thao vat
 * hon, day la cho yeu nhat. Luc do de TRONG dong PIN trong local.properties: may
 * moi cai se khong co PIN nao san, lan dau mo o khoa la vao thang man Cai dat de
 * Ba Huy dat tay mot lan. Khong con gi de doc trong APK nua.
 *
 * Token bot, API key va ma PIN khong con nam trong file nay: chung doc tu
 * local.properties qua BuildConfig, ma local.properties thi nam trong .gitignore
 * nen khong len git. Chep local.properties.mau thanh local.properties roi dien
 * vao. De trong cung build duoc.
 */
object Defaults {

    /**
     * Bot nao thi tuy ban app.
     *
     * Ban go loi (may ao) dung bot rieng, ban that dung bot cua Ba Huy. Telegram chi
     * cho mot may nghe mot bot: hai may cung token la chung giat tin cua nhau, lenh
     * roi vao may nao khong doan truoc duoc. Truoc day phai nho doi token bang tay
     * sau moi lan cai len may ao, quen mot lan la ngoi do ma tim.
     *
     * Hai token dien o local.properties: BOT_THAT va BOT_MAY_AO.
     */
    val BOT_TOKEN: String = if (vn.huytl.homeworkgate.BuildConfig.DEBUG) {
        vn.huytl.homeworkgate.BuildConfig.BOT_MAY_AO
    } else {
        vn.huytl.homeworkgate.BuildConfig.BOT_THAT
    }

    const val PARENT_CHAT_ID = 634_689_551L

    /** So phut cua nut mot cham: nut to duoi anh bai tap, /duyet va /cho khong kem so. */
    const val GRANT_MINUTES = 60

    /** Gio di ngu: 22:00. */
    const val HARD_STOP_MINUTE = 22 * 60

    /** Gio mo lai buoi sang: 06:00. */
    const val WAKE_MINUTE = 6 * 60

    /**
     * Tran gio choi moi ngay theo duong lam bai: tron goi vo dan do 45 cong het tran
     * bai lam them 90. Xem [vn.huytl.homeworkgate.data.LuatCongGio].
     */
    const val TRAN_PHUT_MOI_NGAY = 135

    /**
     * Ma PIN gieo san cho may vua cai xong, dien o local.properties.
     *
     * Chi la gia tri ban dau: [Prefs] bam no ra SHA-256 co salt roi cat, doi PIN
     * trong app la no khong con lien quan gi nua. De trong thi may khong co PIN
     * nao ca - lan dau bam o khoa se vao thang man Cai dat de dat tay.
     */
    val PIN: String = vn.huytl.homeworkgate.BuildConfig.PIN

    /**
     * Khoa API cua AI, theo thu tu uu tien.
     *
     * Nhieu khoa vi ban mien phi co han muc theo ngay: khoa dau het luot thi app
     * nhay sang khoa ke tiep, sang hom sau quay lai khoa dau. Sua duoc trong Cai
     * dat, moi dong mot khoa.
     *
     * DANH DOI: giong token bot va ma PIN o tren - nam thang trong file APK. Ai lay
     * duoc APK ra khoi may la dung duoc khoa nay va tieu vao han muc cua Ba Huy.
     * Khoa AI khong mo duoc tablet, nen no nhe hon hai cai kia, nhung van nen thay
     * khoa moi neu mot ngay nao do file APK ra khoi nha.
     */
    /**
     * Nhung app AI can ghi lai cau con go vao.
     *
     * Muc dich khong phai cam - Ba Huy van de con dung AI. Chi la ghi lai con hoi
     * gi, de doc lai xem con nho AI giai ho hay chi nho chi cho sai.
     *
     * Ten goi phai dung tuyet doi, sai mot chu la khong bat duoc gi ma cung khong
     * bao loi. Neu tren may that co app AI khac (hay ten goi khac ban ghi o day),
     * mo app do go thu mot cau roi xem /hoi: khong thay thi ten goi sai, bao lai de
     * sua. May ten duoi la cac app hay gap, kiem luc thang 1/2026:
     */
    val AI_PACKAGES = setOf(
        "com.openai.chatgpt",                              // ChatGPT
        "com.google.android.apps.bard",                    // Gemini
        "com.google.android.apps.labs.language.tailwind",  // NotebookLM
        "com.microsoft.copilot",                           // Copilot
        "com.anthropic.claude",                            // Claude
        "ai.perplexity.app.android",                       // Perplexity
        "com.deepseek.chat",                               // DeepSeek
        "com.larus.wolf",                                  // Dola (Smart AI Assistant, truoc la Cici)
    )

    /**
     * Key goi Gemini de cham bai. Dien o local.properties, nhieu key thi ngan cach
     * bang dau phay: AI_KEYS=key1,key2. De trong thi Ba Huy dan tay o man hinh
     * Cai dat, o "Key AI" - moi dong mot key.
     */
    val AI_KEYS: List<String> = vn.huytl.homeworkgate.BuildConfig.AI_KEYS
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
