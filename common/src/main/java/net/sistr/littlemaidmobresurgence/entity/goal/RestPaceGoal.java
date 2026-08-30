package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;

/**
 * 休息踱步目标：休息状态的"起身"阶段在休息起始点附近随机走动，
 * 不受自由/跟随/红石巡逻三种行动模式限制。
 */
public class RestPaceGoal extends Goal {
    private static final int PACE_RADIUS = 6;
    private static final int RECALC_COOLDOWN = 40;

    private final LittleMaidEntity maid;
    private BlockPos paceTarget;
    private int recalcCooldown;
    private boolean moved;

    public RestPaceGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return maid.isResting()
                && !maid.isRestSitting()
                // 起身动画（坐姿下压回落）完成前保持原地，避免"坐着滑行"
                && maid.getRestSitProgress() <= 0.01F
                && !TameableUtil.isWait(maid)
                && !maid.isFleeing()
                && maid.getNavigation().isIdle();
    }

    @Override
    public boolean shouldContinue() {
        return maid.isResting() && !maid.isRestSitting();
    }

    @Override
    public void tick() {
        BlockPos anchor = maid.getRestAnchor();
        if (anchor == null) {
            return;
        }
        // 到达目标后重新选点
        if (paceTarget != null
                && maid.squaredDistanceTo(
                                paceTarget.getX() + 0.5, paceTarget.getY(), paceTarget.getZ() + 0.5)
                        < 1.0) {
            paceTarget = null;
            moved = false;
        }
        if (paceTarget == null) {
            if (--recalcCooldown > 0) {
                return;
            }
            recalcCooldown = RECALC_COOLDOWN;
            int x = anchor.getX() + maid.getRandom().nextInt(PACE_RADIUS * 2 + 1) - PACE_RADIUS;
            int z = anchor.getZ() + maid.getRandom().nextInt(PACE_RADIUS * 2 + 1) - PACE_RADIUS;
            paceTarget = new BlockPos(x, anchor.getY(), z);
            moved = false;
        }
        if (!moved) {
            moved = true;
            maid.getNavigation()
                    .startMovingTo(
                            paceTarget.getX() + 0.5, paceTarget.getY(), paceTarget.getZ() + 0.5, 0.5);
        }
    }

    @Override
    public void stop() {
        paceTarget = null;
        recalcCooldown = 0;
        moved = false;
        maid.getNavigation().stop();
    }
}
