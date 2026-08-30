package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

/**
 * 避战逃跑目标：避战状态下主动远离最近的敌对目标，被追击时继续逃跑；
 * 由 LittleMaidEntity.updateEvadeState() 负责进入/解除状态。
 */
public class EvadeCombatGoal extends Goal {
    private final LittleMaidEntity maid;
    @Nullable private LivingEntity fleeTarget;
    private int pathRecalcCooldown;

    public EvadeCombatGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!maid.isEvading() || TameableUtil.isWait(maid) || maid.isFleeing()) {
            return false;
        }
        LivingEntity target = maid.getNearestHostile();
        if (target != null && target.isAlive()) {
            this.fleeTarget = target;
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        if (!maid.isEvading()) {
            return false;
        }
        LivingEntity target = this.fleeTarget;
        if (target == null || !target.isAlive()) {
            this.fleeTarget = maid.getNearestHostile();
            return this.fleeTarget != null;
        }
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.fleeTarget;
        if (target == null || !target.isAlive()) {
            this.fleeTarget = maid.getNearestHostile();
            return;
        }
        // 计算远离敌人的方向
        Vec3d away = maid.getPos().subtract(target.getPos());
        if (away.lengthSquared() < 1.0E-4) {
            away = maid.getRotationVec(1.0F).multiply(-1.0);
        } else {
            away = away.normalize();
        }
        double distance = maid.distanceTo(target);
        if (--pathRecalcCooldown <= 0) {
            pathRecalcCooldown = LMMRMod.getConfig().movement.pathRecalcInterval;
            Path path =
                    maid.getNavigation()
                            .findPathTo(
                                    maid.getX() + away.x * 8.0,
                                    maid.getY(),
                                    maid.getZ() + away.z * 8.0,
                                    2);
            if (path != null && path.getEnd() != null) {
                // 敌人近时冲刺逃跑，远时按逃跑速度
                float speed =
                        distance < 4.0
                                ? LMMRMod.getConfig().movement.sprintSpeed
                                : LMMRMod.getConfig().movement.escapeSpeed;
                maid.getNavigation().startMovingAlong(path, speed);
            }
        }
        // 面向逃跑方向，但看向【眼睛高度】而非脚底高度，避免头部下压倾斜
        maid.getLookControl()
                .lookAt(maid.getX() + away.x, maid.getEyeY(), maid.getZ() + away.z);
    }

    @Override
    public void stop() {
        this.fleeTarget = null;
        maid.getNavigation().stop();
    }
}
