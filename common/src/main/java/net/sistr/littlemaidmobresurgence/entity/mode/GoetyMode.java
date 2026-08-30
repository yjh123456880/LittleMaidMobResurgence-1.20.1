package net.sistr.littlemaidmobresurgence.entity.mode;

import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.compat.GoetyCompat;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * 诡厄巫法模式：主手持诡厄巫法手杖时激活，向敌人施法。
 *
 * <p>施法、冷却全部委托给 Goety 的适配实现（参考枪手模式对 TACZ 的委托方式）；
 * 攻击范围与弓手一致（默认16格 × 射程系数）。Goety 未安装时该模式不会激活。
 */
public class GoetyMode extends AbstractArcherMode<Item> {
    protected int cool;

    public GoetyMode(ModeType<? extends GoetyMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name, mob);
        this.cool = 20;
    }

    @Override
    public boolean shouldExecute() {
        return GoetyCompat.isWand(this.mob.getMainHandStack()) && super.shouldExecute();
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
        // 施法交给 Goety 适配实现，返回下次可行动的等待tick
        int next = GoetyCompat.performWandAttack(this.mob, target, itemStack);
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
        GoetyCompat.stopCast(this.mob);
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack stack) {
        return GoetyCompat.isWand(stack) ? Optional.of(stack.getItem()) : Optional.empty();
    }
}
