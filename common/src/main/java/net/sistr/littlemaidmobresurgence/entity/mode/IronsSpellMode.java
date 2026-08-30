package net.sistr.littlemaidmobresurgence.entity.mode;

import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.compat.IronsSpellCompat;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * 铁魔法模式：主手持铁魔法施法物品（法术书/法杖/魔剑等）时激活，向敌人施法。
 *
 * <p>模式触发<b>仅看主手</b>（副手法术书/魔剑等不触发本模式）；进入模式后，施法由适配器
 * 检索主手/副手/物品栏/扩容背包/饰品栏中所有魔法书的法术随机施放。带铁魔法法术的近战武器
 * （魔剑）主手持时以 LOW 优先级优先判定为铁魔法模式（高于剑客对近战武器的 LOWER 判定）。
 */
public class IronsSpellMode extends AbstractArcherMode<Item> {
    protected int cool;

    public IronsSpellMode(
            ModeType<? extends IronsSpellMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name, mob);
        this.cool = 20;
    }

    @Override
    public boolean shouldExecute() {
        if (this.mob.getTarget() == null || !this.mob.getTarget().isAlive()) {
            return false;
        }
        this.target = this.mob.getTarget();
        ItemStack container = resolveContainer();
        if (container.isEmpty()) {
            return false;
        }
        this.weaponStack = container;
        this.weapon = getWeaponInstance(container).orElseThrow();
        return true;
    }

    /** 解析施法容器：仅主手铁魔法施法物品（法术书/法杖/魔剑等）触发本模式。 */
    private ItemStack resolveContainer() {
        var main = this.mob.getMainHandStack();
        return IronsSpellCompat.isIronMagicItem(main) ? main : ItemStack.EMPTY;
    }

    @Override
    protected void tickRangedAttack(
            LivingEntity target,
            ItemStack itemStack,
            boolean canSee,
            double distanceSq,
            float maxRange) {
        // 看不见或超出射程时不施法（走位逻辑由父类处理）
        if (!canSee || maxRange * maxRange < distanceSq) {
            return;
        }
        if (0 < this.cool) {
            this.cool--;
            return;
        }
        // 射线检查，射线上有友方时不施法（弓手同款）
        var result = this.raycastShootLine(target, maxRange, this.friendlyShotLineFilter());
        if (result.isPresent()) {
            this.cool = 10;
            return;
        }
        // 从主手解析实际施法容器
        ItemStack container = resolveContainer();
        if (container.isEmpty()) {
            this.cool = 20;
            return;
        }
        // 施法交给 Iron's Spells 适配实现（内部会检索主/副手/物品栏/扩容背包/饰品栏的魔法书），返回下次可行动的等待tick
        int next = IronsSpellCompat.performSpellAttack(this.mob, target, container);
        this.cool = Math.max(1, next);
        this.mob.swingHand(Hand.MAIN_HAND);
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        // 攻击范围与弓箭一致
        return 16F * LMMRMod.getConfig().work.archerShootDistanceFactor;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.cool = 20;
        IronsSpellCompat.stopCast(this.mob);
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack stack) {
        // 铁魔法施法物品（法术书/法杖/魔剑等）即视为本模式武器
        return IronsSpellCompat.isIronMagicItem(stack)
                ? Optional.of(stack.getItem())
                : Optional.empty();
    }
}
