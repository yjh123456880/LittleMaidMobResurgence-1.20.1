package net.sistr.littlemaidmobresurgence.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 铁魔法（Iron's Spells n Spellbooks）法杖/法术书兼容门面。
 *
 * <p>common 模块不直接引用 Iron's Spells 的类（避免未安装时 NoClassDefFoundError），
 * 实际实现由 forge 模块的适配器在检测到该模组后注入，与 {@link TaczCompat} 同构。
 */
public final class IronsSpellCompat {
    @Nullable private static SpellbookAdapter adapter;

    private IronsSpellCompat() {}

    /** 由 forge 模块在 Iron's Spells 已安装时调用，注入实际实现。 */
    public static void setAdapter(SpellbookAdapter adapter) {
        IronsSpellCompat.adapter = adapter;
    }

    public static boolean isInstalled() {
        return adapter != null;
    }

    /** 该物品是否是铁魔法的法术容器（法杖/法术书/魔剑等）。 */
    public static boolean isSpellContainer(ItemStack stack) {
        return adapter != null && adapter.isSpellContainer(stack);
    }

    /** 该物品是否是铁魔法的施法物品（法术容器 或 法杖/魔剑等按类识别）。 */
    public static boolean isIronMagicItem(ItemStack stack) {
        return adapter != null && adapter.isIronMagicItem(stack);
    }

    /**
     * 用法术容器向目标施法（内部处理选法术/冷却/持续施法）。
     *
     * @return 下一次可行动需等待的 tick 数
     */
    public static int performSpellAttack(
            LittleMaidEntity caster, LivingEntity target, ItemStack bookOrStaff) {
        return adapter != null ? adapter.performSpellAttack(caster, target, bookOrStaff) : 100;
    }

    /** 停止施法（模式结束时调用）。 */
    public static void stopCast(LittleMaidEntity maid) {
        if (adapter != null) {
            adapter.stopCast(maid);
        }
    }

    /** 铁魔法法术容器操作适配器，由 forge 模块实现。 */
    public interface SpellbookAdapter {
        boolean isSpellContainer(ItemStack stack);

        boolean isIronMagicItem(ItemStack stack);

        int performSpellAttack(LittleMaidEntity caster, LivingEntity target, ItemStack bookOrStaff);

        void stopCast(LittleMaidEntity maid);
    }
}
