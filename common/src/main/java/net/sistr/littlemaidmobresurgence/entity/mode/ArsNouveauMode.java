package net.sistr.littlemaidmobresurgence.entity.mode;

import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.compat.ArsNouveauCompat;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * 新生魔艺模式：主手持新生魔艺魔法书时激活，向敌人施法。
 *
 * <p>选法术、冷却、施法动画全部委托给 Ars Nouveau 的适配实现；
 * 攻击范围与弓手一致（默认16格 × 射程系数）。Ars Nouveau 未安装时该模式不会激活。
 */
public class ArsNouveauMode extends AbstractArcherMode<Item> {
    protected int cool;

    public ArsNouveauMode(
            ModeType<? extends ArsNouveauMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name, mob);
        this.cool = 20;
    }

    @Override
    public boolean shouldExecute() {
        return ArsNouveauCompat.isSpellBook(this.mob.getMainHandStack()) && super.shouldExecute();
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
        var result =
                this.raycastShootLine(target, maxRange, this.friendlyShotLineFilter());
        if (result.isPresent()) {
            this.cool = 10;
            return;
        }
        // 施法交给 Ars Nouveau 适配实现，返回下次可行动的等待tick
        int next = ArsNouveauCompat.performSpellAttack(this.mob, target, itemStack);
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
        ArsNouveauCompat.stopCast(this.mob);
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack stack) {
        return ArsNouveauCompat.isSpellBook(stack) ? Optional.of(stack.getItem()) : Optional.empty();
    }
}
