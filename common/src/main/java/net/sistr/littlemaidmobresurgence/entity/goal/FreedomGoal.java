package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;

// [zh] 自由行动目标：无主人时也执行；主人存在时以工作范围中心（或自由起点）为圆心限定活动范围
// [en] Free-move goal; runs even without an owner; confines the maid to a radius around the bound work center (or the free-move origin)
// [ja] 自由行動のGoal。主人がいなくても実行されます。作業範囲中心（または自由起点）を円心に行動範囲を制限します。
public class FreedomGoal<T extends LittleMaidEntity> extends WanderAroundFarGoal {
    private final T maid;
    private final Supplier<Float> distance;
    private final Supplier<Float> distanceSq;
    private BlockPos freedomPos;
    private int reCalcCool;

    public FreedomGoal(T mob, float speedIn, Supplier<Float> distance) {
        super(mob, speedIn);
        this.maid = mob;
        this.distance = distance;
        this.distanceSq = () -> distance.get() * distance.get();
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (!TameableUtil.hasTameOwner(maid)
                || TameableUtil.isWait(maid)
                || maid.isInRecoveryState()
                || maid.getMovingMode() != MovingMode.FREEDOM) {
            return false;
        }
        BlockPos center =
                maid.hasBoundWorkCenter(maid.getWorld())
                        ? maid.getBoundWorkCenter()
                        : maid.getFreedomPos().orElse(null);
        // [zh] 超出工作范围：无条件优先返回（即使正在做别的事/正在寻路）
        // [en] Outside the work range: return unconditionally with priority (even while doing something else).
        // [ja] 作業範囲外：無条件で最優先に戻ります（他の行動・探索中でも）。
        if (center != null && center.getSquaredDistance(mob.getPos()) >= distanceSq.get()) {
            return true;
        }
        return maid.getNavigation().isIdle() && super.canStart();
    }

    @Override
    public void start() {
        super.start();
        // [zh] 已绑工作范围（同维度）时以绑定中心为自由行动圆心，否则以当前方块为圆心
        // [en] Use the bound work center (same dimension) as the free-move center, otherwise the current block.
        // [ja] 作業範囲登録済み（同次元）ならその中心を自由行動の円心に、未登録なら現在地を円心に。
        freedomPos =
                maid.hasBoundWorkCenter(maid.getWorld())
                        ? maid.getBoundWorkCenter()
                        : maid.getFreedomPos().orElse(null);
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos center = freedomPos;
        if (center == null) {
            return;
        }
        double distSq = center.getSquaredDistance(mob.getPos());
        if (distSq < distanceSq.get()) {
            return;
        }
        // [zh] 极远（范围外 24 格以上）才直接传送；否则主动跑回工作范围中心
        // [en] Teleport only when very far (24+ blocks beyond the range); otherwise actively walk back to the center.
        // [ja] 極端に遠い（範囲外24ブロック以上）時だけテレポート。それ以外は能動的に中心へ戻ります。
        double teleportThreshold = (distance.get() + 24) * (distance.get() + 24);
        if (distSq > teleportThreshold) {
            teleportBack();
            return;
        }
        if (--reCalcCool > 0) {
            return;
        }
        reCalcCool = getTickCount(LMMRMod.getConfig().movement.pathRecalcInterval);
        // [zh] 主动跑回工作范围中心（距离越远跑得越快，用冲刺速度）
        // [en] Actively run back to the center (faster when farther, using sprint speed).
        // [ja] 中心へ能動的に走って戻ります（遠いほど速く、スプリント速度を使用）。
        Path path =
                mob.getNavigation()
                        .findPathTo(
                                center.getX(),
                                center.getY(),
                                center.getZ(),
                                MathHelper.floor(distance.get() * 0.5));
        if (path != null
                && path.getEnd() != null
                && path.getEnd().getManhattanDistance(center) < distance.get()) {
            float speed =
                    distSq > (distance.get() * 2) * (distance.get() * 2)
                            ? LMMRMod.getConfig().movement.sprintSpeed
                            : LMMRMod.getConfig().movement.followSpeed;
            mob.getNavigation().startMovingAlong(path, speed);
            return;
        }
        // [zh] 路径不可达（被地形挡住/悬崖）：回退传送，避免卡死
        // [en] Unreachable path (terrain/cliff): fall back to teleport to avoid getting stuck.
        // [ja] 経路が通れない場合（地形・崖）：詰まりを防ぐためテレポートにフォールバック。
        teleportBack();
    }

    /**
     * [zh] 传送到工作范围中心附近的可行走落点（比原实现更快、更可靠）。
     * [en] Teleports to a walkable spot near the work center (faster and more reliable than the original).
     * [ja] 作業範囲中心付近の歩行可能な地点へテレポートします（従来より高速・高信頼）。
     */
    private void teleportBack() {
        mob.getNavigation().stop();
        BlockPos center = freedomPos;
        if (center == null) {
            return;
        }
        int maxTry = LMMRMod.getConfig().movement.maxTryTeleportCount;
        int width = LMMRMod.getConfig().movement.teleportWidth;
        int height = LMMRMod.getConfig().movement.teleportHeight;
        for (int i = 0; i < maxTry; ++i) {
            int x = center.getX() + mob.getRandom().nextInt(width * 2 + 1) - width;
            int y = center.getY() + mob.getRandom().nextInt(height * 2 + 1) - height;
            int z = center.getZ() + mob.getRandom().nextInt(width * 2 + 1) - width;
            BlockPos target = new BlockPos(x, y, z);
            if (canTeleportTo(target)) {
                mob.refreshPositionAndAngles(
                        x + 0.5, y, z + 0.5, mob.getYaw(), mob.getPitch());
                mob.getNavigation().stop();
                return;
            }
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        PathNodeType nodeType = LandPathNodeMaker.getLandNodeType(mob.getWorld(), pos.mutableCopy());
        if (nodeType != PathNodeType.WALKABLE) {
            return false;
        }
        return mob.getWorld()
                .isSpaceEmpty(mob, mob.getBoundingBox().offset(pos.subtract(mob.getBlockPos())));
    }

    @Override
    public void stop() {
        super.stop();
        freedomPos = null;
        reCalcCool = 0;
    }
}
