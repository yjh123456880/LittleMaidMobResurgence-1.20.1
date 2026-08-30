package net.sistr.littlemaidmobresurgence.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

/**
 * TACZ 枪械模组兼容门面。
 *
 * <p>common 模块不直接引用 TACZ 的类（避免未安装 TACZ 时 NoClassDefFoundError），
 * 实际实现由 forge 模块的适配器在检测到 TACZ 后注入。
 */
public final class TaczCompat {
    @Nullable private static GunAdapter adapter;

    private TaczCompat() {}

    /** 由 forge 模块在 TACZ 已安装时调用，注入实际实现。 */
    public static void setAdapter(GunAdapter adapter) {
        TaczCompat.adapter = adapter;
    }

    public static boolean isInstalled() {
        return adapter != null;
    }

    /** 该物品是否是 TACZ 枪械。 */
    public static boolean isGun(ItemStack stack) {
        return adapter != null && adapter.isGun(stack);
    }

    /**
     * 用 TACZ 枪械射击目标（内部处理瞄准/拔枪/拉栓/换弹状态机）。
     *
     * @return 下一次可行动需等待的 tick 数
     */
    public static int performGunAttack(LittleMaidEntity shooter, LivingEntity target, ItemStack gunItem) {
        return adapter != null ? adapter.performGunAttack(shooter, target, gunItem) : 100;
    }

    /** 停止瞄准（模式结束时调用）。 */
    public static void stopAim(LittleMaidEntity maid) {
        if (adapter != null) {
            adapter.stopAim(maid);
        }
    }

    /** TACZ 枪械操作适配器，由 forge 模块实现。 */
    public interface GunAdapter {
        boolean isGun(ItemStack stack);

        int performGunAttack(LittleMaidEntity shooter, LivingEntity target, ItemStack gunItem);

        void stopAim(LittleMaidEntity maid);
    }
}