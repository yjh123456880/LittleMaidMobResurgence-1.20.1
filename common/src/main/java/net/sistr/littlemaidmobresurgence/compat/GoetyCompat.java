package net.sistr.littlemaidmobresurgence.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 诡厄巫法（Goety）手杖兼容门面。
 *
 * <p>common 模块不直接引用 Goety 的类（避免未安装 Goety 时 NoClassDefFoundError），
 * 实际实现由 forge 模块的适配器在检测到 Goety 后注入，与 {@link TaczCompat} 同构。
 */
public final class GoetyCompat {
    @Nullable private static WandAdapter adapter;

    private GoetyCompat() {}

    /** 由 forge 模块在 Goety 已安装时调用，注入实际实现。 */
    public static void setAdapter(WandAdapter adapter) {
        GoetyCompat.adapter = adapter;
    }

    public static boolean isInstalled() {
        return adapter != null;
    }

    /** 该物品是否是诡厄巫法的手杖。 */
    public static boolean isWand(ItemStack stack) {
        return adapter != null && adapter.isWand(stack);
    }

    /**
     * 用手杖向目标施法（内部处理选法术/冷却/施法动画）。
     *
     * @return 下一次可行动需等待的 tick 数
     */
    public static int performWandAttack(LittleMaidEntity caster, LivingEntity target, ItemStack wand) {
        return adapter != null ? adapter.performWandAttack(caster, target, wand) : 100;
    }

    /** 停止施法（模式结束时调用）。 */
    public static void stopCast(LittleMaidEntity maid) {
        if (adapter != null) {
            adapter.stopCast(maid);
        }
    }

    /** 诡厄巫法手杖操作适配器，由 forge 模块实现。 */
    public interface WandAdapter {
        boolean isWand(ItemStack stack);

        int performWandAttack(LittleMaidEntity caster, LivingEntity target, ItemStack wand);

        void stopCast(LittleMaidEntity maid);
    }
}
