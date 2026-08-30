package net.sistr.littlemaidmobresurgence.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 新生魔艺（Ars Nouveau）魔法书兼容门面。
 *
 * <p>common 模块不直接引用 Ars Nouveau 的类（避免未安装时 NoClassDefFoundError），
 * 实际实现由 forge 模块的适配器在检测到该模组后注入，与 {@link TaczCompat} 同构。
 */
public final class ArsNouveauCompat {
    @Nullable private static ArsCasterAdapter adapter;

    private ArsNouveauCompat() {}

    /** 由 forge 模块在 Ars Nouveau 已安装时调用，注入实际实现。 */
    public static void setAdapter(ArsCasterAdapter adapter) {
        ArsNouveauCompat.adapter = adapter;
    }

    public static boolean isInstalled() {
        return adapter != null;
    }

    /** 该物品是否是新生魔艺的魔法书。 */
    public static boolean isSpellBook(ItemStack stack) {
        return adapter != null && adapter.isSpellBook(stack);
    }

    /**
     * 用魔法书向目标施法（内部处理选法术/冷却/施法动画）。
     *
     * @return 下一次可行动需等待的 tick 数
     */
    public static int performSpellAttack(
            LittleMaidEntity caster, LivingEntity target, ItemStack spellBook) {
        return adapter != null ? adapter.performSpellAttack(caster, target, spellBook) : 100;
    }

    /** 停止施法（模式结束时调用）。 */
    public static void stopCast(LittleMaidEntity maid) {
        if (adapter != null) {
            adapter.stopCast(maid);
        }
    }

    /** 新生魔艺魔法书操作适配器，由 forge 模块实现。 */
    public interface ArsCasterAdapter {
        boolean isSpellBook(ItemStack stack);

        int performSpellAttack(LittleMaidEntity caster, LivingEntity target, ItemStack spellBook);

        void stopCast(LittleMaidEntity maid);
    }
}
