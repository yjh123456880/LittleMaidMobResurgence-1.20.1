package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

/**
 * [zh] 跟随模式硬范围限定：女仆被锁定在以主人为圆心、{@code followRange} 为半径的范围内。
 *     超出范围时近则走回、远或不可达则传送到主人附近；自由/巡逻模式不生效。
 * [en] Follow-mode hard confinement: the maid is locked within {@code followRange} of the owner.
 *     When outside: walk back if close, teleport near the owner if far or unreachable; free/tracer modes are unaffected.
 * [ja] 追従モードのハード制限：主人を円心・{@code followRange} を半径とする範囲にメイドを拘束します。
 *     範囲外では近ければ歩いて戻り、遠い・到達不能なら主人付近へテレポート。自由・レッドストーン追跡モードには影響しません。
 */
public class FollowRangeConfinementGoal extends Goal {
    private final LittleMaidEntity maid;
    @Nullable private LivingEntity owner;
    private int pathRecalcCooldown;

    public FollowRangeConfinementGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private float range() {
        return LMMRMod.getConfig().movement.followRange;
    }

    @Override
    public boolean canStart() {
        if (maid.getMovingMode() != MovingMode.ESCORT
                || TameableUtil.isWait(maid)
                || maid.isInRecoveryState()
                || maid.isFleeing()) {
            return false;
        }
        LivingEntity foundOwner = TameableUtil.getTameOwner(maid).orElse(null);
        if (foundOwner == null || foundOwner.isSpectator()) {
            return false;
        }
        // [zh] 主人跨维度时由 LMTeleportTameOwnerGoal 处理传送，本目标不跨维度寻路/传送
        // [en] Cross-dimension owners are handled by LMTeleportTameOwnerGoal; this goal never paths/teleports across dimensions.
        // [ja] 主人が異次元にいる場合は LMTeleportTameOwnerGoal が処理。本Goalは異次元の経路・テレポートを行いません。
        if (foundOwner.getWorld() != maid.getWorld()) {
            return false;
        }
        this.owner = foundOwner;
        float range = range();
        return maid.squaredDistanceTo(foundOwner) > range * range;
    }

    @Override
    public boolean shouldContinue() {
        if (maid.getMovingMode() != MovingMode.ESCORT || maid.isFleeing()) {
            return false;
        }
        LivingEntity foundOwner = this.owner;
        if (foundOwner == null || !foundOwner.isAlive()) {
            return false;
        }
        if (foundOwner.getWorld() != maid.getWorld()) {
            return false;
        }
        float range = range();
        return maid.squaredDistanceTo(foundOwner) > range * range;
    }

    @Override
    public void start() {
        pathRecalcCooldown = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        maid.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity foundOwner = this.owner;
        if (foundOwner == null) {
            return;
        }
        float range = range();
        double distSq = maid.squaredDistanceTo(foundOwner);
        // [zh] 极远（范围外 24 格以上）才直接传送；否则主动跑回主人身边
        // [en] Teleport only when very far (24+ blocks beyond the range); otherwise actively walk back to the owner.
        // [ja] 極端に遠い（範囲外24ブロック以上）時だけテレポート。それ以外は能動的に主人の元へ戻ります。
        double teleportThreshold = (range + 24) * (range + 24);
        if (distSq > teleportThreshold) {
            teleportNear(foundOwner);
            return;
        }
        if (--pathRecalcCooldown > 0) {
            return;
        }
        pathRecalcCooldown = LMMRMod.getConfig().movement.pathRecalcInterval;
        // [zh] 主动跑回主人身边的落点（距离越远跑得越快，用冲刺速度）
        // [en] Actively run back toward the owner (faster when farther, using sprint speed).
        // [ja] 主人の近くへ能動的に走って戻ります（遠いほど速く、スプリント速度を使用）。
        Path path =
                maid.getNavigation()
                        .findPathTo(foundOwner.getX(), foundOwner.getY(), foundOwner.getZ(), 1);
        if (path != null && path.getEnd() != null) {
            float speed =
                    distSq > (range * 2) * (range * 2)
                            ? LMMRMod.getConfig().movement.sprintSpeed
                            : LMMRMod.getConfig().movement.followSpeed;
            maid.getNavigation().startMovingAlong(path, speed);
            maid.getLookControl().lookAt(foundOwner, 30.0F, 30.0F);
        } else {
        // [zh] 路径不可达（被地形挡住/悬崖）：回退传送，避免卡死
        // [en] Unreachable path (terrain/cliff): fall back to teleport to avoid getting stuck.
        // [ja] 経路が通れない場合（地形・崖）：詰まりを防ぐためテレポートにフォールバック。
        teleportNear(foundOwner);
        }
    }

    private void teleportNear(LivingEntity owner) {
        int maxTry = LMMRMod.getConfig().movement.maxTryTeleportCount;
        int width = LMMRMod.getConfig().movement.teleportWidth;
        int height = LMMRMod.getConfig().movement.teleportHeight;
        BlockPos ownerPos = owner.getBlockPos();
        for (int i = 0; i < maxTry; ++i) {
            int x = ownerPos.getX() + maid.getRandom().nextInt(width * 2 + 1) - width;
            int y = ownerPos.getY() + maid.getRandom().nextInt(height * 2 + 1) - height;
            int z = ownerPos.getZ() + maid.getRandom().nextInt(width * 2 + 1) - width;
            BlockPos target = new BlockPos(x, y, z);
            if (canTeleportTo(maid.getWorld(), target)) {
                maid.refreshPositionAndAngles(
                        x + 0.5, y, z + 0.5, maid.getYaw(), maid.getPitch());
                maid.getNavigation().stop();
                return;
            }
        }
    }

    private boolean canTeleportTo(World world, BlockPos pos) {
        PathNodeType nodeType = LandPathNodeMaker.getLandNodeType(world, pos.mutableCopy());
        if (nodeType != PathNodeType.WALKABLE) {
            return false;
        }
        return world.isSpaceEmpty(
                maid, maid.getBoundingBox().offset(pos.subtract(maid.getBlockPos())));
    }
}
