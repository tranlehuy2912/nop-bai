package vn.huytl.homeworkgate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.huytl.homeworkgate.data.LuotBaNoi
import vn.huytl.homeworkgate.data.Prefs

/**
 * Mot luot moi ngay cua ba noi.
 *
 * Cho nay la cai chan cuoi cung. Ben may ba cung dem, nhung so dem do ve khong khi
 * ba cai lai app - luc do chi con cho nay.
 *
 * CAN THAN: bo test nay xoa sach prefs. Dung chay tren tablet cua Le Hoa.
 */
@RunWith(AndroidJUnit4::class)
class LuotBaNoiTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.get(ctx).raw().edit().clear().commit()
    }

    @Test
    fun may_moi_thi_ba_con_nguyen_luot() {
        assertFalse(LuotBaNoi.daChoHomNay(ctx))
    }

    @Test
    fun cho_roi_thi_het_luot_hom_nay() {
        LuotBaNoi.ghiNhanDaCho(ctx)
        assertTrue(LuotBaNoi.daChoHomNay(ctx))
        // Goi lan nua khong lam no mo ra lai.
        LuotBaNoi.ghiNhanDaCho(ctx)
        assertTrue(LuotBaNoi.daChoHomNay(ctx))
    }

    /**
     * Sang hom sau la co luot moi.
     *
     * Khong doi duoc dong ho may trong bo test, nen gia mot ngay cu bang cach ghi
     * thang xuong khoa ma [LuotBaNoi] doc. Doi ten khoa do ma quen sua o day thi
     * bai nay do - va do la dung cai muon biet.
     */
    @Test
    fun qua_ngay_thi_co_luot_moi() {
        Prefs.get(ctx).raw().edit().putString("hopthu_ngay_da_cho", "2020-01-01").commit()
        assertFalse(LuotBaNoi.daChoHomNay(ctx))
    }
}
