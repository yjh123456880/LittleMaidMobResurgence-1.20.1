package net.sistr.littlemaidmobresurgence.entity.mode;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.BattleModeType;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

/**
 * [zh] 远程战斗模式基类：锁定目标、绕圈走位（保持距离）、射线友方判定与射击节拍。
 *     超出跟随/工作范围时不停止战斗，而是边走边打地返回范围中心（见 {@link #getConfinement()}）。
 * [en] Base class for ranged battle modes: target lock-on, strafing (range keeping), friendly-line raycast and firing rhythm.
 *     When outside the follow/work range, the mode keeps fighting while returning to the range center (see {@link #getConfinement()}).
 * [ja] 遠距離戦闘モードの基底クラス：ターゲットロック、周回移動（距離維持）、射線上の味方判定、射撃リズムを扱います。
 *     追従・作業範囲外でも戦闘を止めず、範囲中心へ「戦いながら戻る」制御を行います（{@link #getConfinement()} 参照）。
 */
public abstract class AbstractArcherMode<T> extends AbstractBattleMode<T> {
    protected final LittleMaidEntity mob;
    protected int seeTime;
    protected boolean strafingClockwise;
    protected boolean strafingBackwards;
    protected int strafingTime = -1;
    private int returnPathRecalcCooldown;

    public AbstractArcherMode(
            ModeType<? extends AbstractArcherMode> modeType, String name, LittleMaidEntity mob) {
        super(mob, modeType, name);
        this.mob = mob;
    }

    public void startExecuting() {
        this.mob.setAttacking(true);
        this.mob.setAimingBow(true);
        this.mob.play(LMSounds.FIND_TARGET_N);
        this.mob.getNavigation().stop();
        this.returnPathRecalcCooldown = 0;
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        double distanceSq = this.mob.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.mob.getVisibilityCache().canSee(target);
        ItemStack itemStack = this.mob.getMainHandStack();
        float maxRange = getMaxRange(itemStack);
        boolean prevCanSee = 0 < this.seeTime;
        // [zh] 视线状态切换时重置可见计时
        // [en] Reset the visibility timer when the line-of-sight state changes
        // [ja] 見えなくなるか、見えるようになったら
        if (canSee != prevCanSee) {
            this.seeTime = 0;
        }
        // [zh] 失去视线时重置走位计时并反转环绕方向
        // [en] On losing sight, reset the strafe timer and flip the circling direction
        // [ja] 見えなくなったら
        if (prevCanSee && !canSee) {
            this.strafingTime = 0;
            this.strafingClockwise = !this.strafingClockwise;
        }

        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        // [zh] 目标在射程内时累计走位计时
        // [en] Accumulate strafe timing while the target is within range
        // [ja] レンジ内
        if (distanceSq < maxRange * maxRange) {
            ++this.strafingTime;
        } else {
            this.strafingTime = 0;
        }

        // [zh] 每 1 秒以 10% 概率反转环绕方向，避免走位过于死板
        // [en] Every second, flip the circling direction with 10% probability to avoid predictable movement
        // [ja] 1秒ごとに10%の確率で反転
        if (20 <= this.strafingTime) {
            if ((double) this.mob.getRandom().nextFloat() < 0.1D) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            this.strafingTime = 0;
        }

        if (maxRange * maxRange < distanceSq) {
            this.strafingBackwards = false;
        } else if (distanceSq < maxRange * maxRange * 0.75F) {
            this.strafingBackwards = true;
        }

        // [zh] 范围限定：跟随模式以玩家为圆心，自由行动以工作范围中心为圆心。
        //     超出范围或贴近边界时不绕圈走位，而是由本模式主动跑回中心（边走边打），
        //     不再让出 MOVE 控制给范围限定目标——避免两个目标来回交接导致左右摇摆/寻路错误。
        // [en] Range confinement: ESCORT centers on the player, FREEDOM on the bound work center.
        //     When outside or near the boundary, stop strafing and actively walk back to the center while fighting,
        //     without handing MOVE control to the confinement goals (prevents oscillation/swaying).
        // [ja] 範囲制限：追従モードはプレイヤー中心、自由行動は作業範囲中心。
        //     範囲外・境界付近では周回せず、戦いながら中心へ戻ります。MOVE制御を範囲制限Goalへ譲らないことで
        //     目標の受け渡しによる左右振動・経路エラーを防ぎます。
        Confinement confinement = getConfinement();
        if (confinement == null) {
            this.mob
                    .getMoveControl()
                    .strafeTo(
                            this.strafingBackwards ? -0.5F : 0.5F,
                            this.strafingClockwise ? 0.5F : -0.5F);
        } else {
            double centerDistSq = confinement.center().getSquaredDistance(this.mob.getPos());
            float range = confinement.range();
            // [zh] 距边界 1.5 格内即视为“贴近边界”，提前转向回中心，避免反复越界
            // [en] Within 1.5 blocks of the boundary, start returning early to avoid repeatedly crossing it.
            // [ja] 境界から1.5ブロック以内を「境界付近」とみなし、早めに中心へ向かって越境を防ぎます。
            float inner = Math.max(1.0F, range - 1.5F);
            if (centerDistSq > range * range || centerDistSq > inner * inner) {
                returnToConfinementCenter(confinement.center(), centerDistSq, range);
            } else {
                this.mob
                        .getMoveControl()
                        .strafeTo(
                                this.strafingBackwards ? -0.5F : 0.5F,
                                this.strafingClockwise ? 0.5F : -0.5F);
            }
        }
        this.mob.lookAtEntity(target, 30.0F, 30.0F);
        this.mob.getLookControl().lookAt(target, 30f, 30f);

        tickRangedAttack(target, itemStack, canSee, distanceSq, maxRange);
    }

    /**
     * [zh] 不再因超出范围而停止战斗模式：回程由 {@link #tick()} 内部的“边走边打”处理，
     *     避免与范围限定目标来回交接导致左右摇摆/寻路错误。
     * [en] The mode no longer stops when out of range: returning is handled inside {@link #tick()} ("fight while walking back"),
     *     avoiding the goal hand-off oscillation that caused swaying/path errors.
     * [ja] 範囲外でも戦闘モードを止めません。復帰処理は {@link #tick()} 内の「戦いながら戻る」で行い、
     *     範囲制限Goalとの受け渡しによる左右振動・経路エラーを防ぎます。
     */
    @Override
    public boolean shouldContinueExecuting() {
        return super.shouldContinueExecuting();
    }

    /**
     * [zh] 当前移动模式的范围限定（跟随=以玩家为圆心；自由行动=以工作范围中心为圆心）。
     * [en] Range confinement of the current moving mode (ESCORT = centered on the player; FREEDOM = centered on the bound work center).
     * [ja] 現在の移動モードの範囲制限（追従=プレイヤー中心、自由行動=作業範囲中心）。
     */
    @Nullable
    private Confinement getConfinement() {
        if (mob.getMovingMode() == MovingMode.ESCORT) {
            LivingEntity owner = TameableUtil.getTameOwner(mob).orElse(null);
            if (owner == null || !owner.isAlive() || owner.isSpectator()) {
                return null;
            }
            // [zh] 主人跨维度时不启用范围限定：避免寻路/传送指向异维坐标（跨维度由传送目标负责）
            // [en] Skip confinement when the owner is in another dimension: pathing/teleport must not target foreign coordinates (cross-dim is handled by the teleport goal).
            // [ja] 主人が異次元の場合は範囲制限を無効化（他次元座標への経路・テレポートを回避。異次元はテレポートGoalが担当）。
            if (owner.getWorld() != mob.getWorld()) {
                return null;
            }
            return new Confinement(
                    owner.getBlockPos(), LMMRMod.getConfig().movement.followRange);
        }
        if (mob.getMovingMode() == MovingMode.FREEDOM) {
            BlockPos center =
                    mob.hasBoundWorkCenter(mob.getWorld())
                            ? mob.getBoundWorkCenter()
                            : mob.getFreedomPos().orElse(null);
            if (center == null) {
                return null;
            }
            return new Confinement(center, LMMRMod.getConfig().work.workRange);
        }
        // [zh] 追踪（TRACER）/其它模式无范围限定，正常绕圈走位
        // [en] Tracer (TRACER) and other modes have no confinement; normal circling applies.
        // [ja] 追跡（TRACER）等のモードは範囲制限なし。通常の周回移動を続けます。
        return null;
    }

    /**
     * [zh] 超出范围/贴近边界时主动跑回范围中心（边走边打）；极远或不可达则传送回中心。
     * [en] When outside or near the boundary, actively run back to the range center while fighting; teleport if very far or unreachable.
     * [ja] 範囲外・境界付近では戦いながら中心へ戻ります。極端に遠い・到達不能ならテレポート。
     */
    private void returnToConfinementCenter(BlockPos center, double distSq, float range) {
        // [zh] 极远（范围外 24 格以上）才直接传送；否则主动跑回范围中心
        // [en] Teleport only when very far (24+ blocks beyond the range); otherwise actively walk back.
        // [ja] 極端に遠い（範囲外24ブロック以上）時だけテレポート。それ以外は能動的に中心へ戻ります。
        double teleportThreshold = (range + 24) * (range + 24);
        if (distSq > teleportThreshold) {
            teleportToConfinementCenter(center);
            return;
        }
        if (--returnPathRecalcCooldown > 0) {
            return;
        }
        returnPathRecalcCooldown = LMMRMod.getConfig().movement.pathRecalcInterval;
        // [zh] 主动跑回范围中心（距离越远跑得越快，用冲刺速度）
        // [en] Actively run back to the center (faster when farther, using sprint speed).
        // [ja] 中心へ能動的に走って戻ります（遠いほど速く、スプリント速度を使用）。
        Path path =
                mob.getNavigation()
                        .findPathTo(
                                center.getX(),
                                center.getY(),
                                center.getZ(),
                                MathHelper.floor(range * 0.5));
        if (path != null
                && path.getEnd() != null
                && path.getEnd().getManhattanDistance(center) < range) {
            float speed =
                    distSq > (range * 2) * (range * 2)
                            ? LMMRMod.getConfig().movement.sprintSpeed
                            : LMMRMod.getConfig().movement.followSpeed;
            mob.getNavigation().startMovingAlong(path, speed);
            return;
        }
        // [zh] 路径不可达（被地形挡住/悬崖）：回退传送，避免卡死
        // [en] Unreachable path (terrain/cliff): fall back to teleport to avoid getting stuck.
        // [ja] 経路が通れない場合（地形・崖）：詰まりを防ぐためテレポートにフォールバック。
        teleportToConfinementCenter(center);
    }

    /**
     * [zh] 传送到范围中心附近的可行走落点（复用范围限定目标同款安全传送逻辑）。
     * [en] Teleports to a walkable spot near the range center (same safe-teleport logic as the confinement goals).
     * [ja] 範囲中心付近の歩行可能な地点へテレポートします（範囲制限Goalと同じ安全テレポート処理）。
     */
    private void teleportToConfinementCenter(BlockPos center) {
        mob.getNavigation().stop();
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

    private record Confinement(BlockPos center, float range) {}

    protected abstract void tickRangedAttack(
            LivingEntity target,
            ItemStack itemStack,
            boolean canSee,
            double distanceSq,
            float maxRange);

    protected abstract float getMaxRange(ItemStack itemStack);

    protected Optional<EntityHitResult> raycastShootLine(
            LivingEntity target, float maxRange, Predicate<Entity> predicate) {
        var targetAt = target.getEyePos();
        var toTargetVec = targetAt.subtract(this.mob.getEyePos()).normalize();
        Vec3d start = this.mob.getCameraPosVec(1F);
        Vec3d end = start.add(toTargetVec.multiply(maxRange));
        Box box = new Box(start, end).expand(1D);
        var result =
                ProjectileUtil.getEntityCollision(
                        mob.getWorld(), this.mob, start, end, box, predicate);
        return Optional.ofNullable(result);
    }

    /**
     * [zh] 射线上友方阻挡判定：非反叛时友方挡住不射击；反叛时排除怒气目标（玩家）——
     *     否则射向代理实体（位于玩家身上）的箭/法术会被玩家挡住而永远打不出去。
     * [en] Friendly-line-of-fire filter: allies block shooting unless rebelling; during rebellion the anger target (player) is excluded,
     *     otherwise arrows/spells aimed at the proxy (inside the player) would always be blocked.
     * [ja] 射線上の味方判定：非反乱時は味方がいれば撃ちません。反乱時は怒りの対象（プレイヤー）を除外し、
     *     プレイヤー内側にいるプロキシ実体を狙った弾がプレイヤーに遮られて撃てない問題を防ぎます。
     */
    protected Predicate<Entity> friendlyShotLineFilter() {
        return e ->
                e instanceof LivingEntity living
                        && !this.mob.canTarget(living)
                        && !(living instanceof net.sistr.littlemaidmobresurgence.entity.RebellionProxyEntity)
                        && !(this.mob.isRebellious() && this.mob.isRebellionTarget(living));
    }

    public void resetTask() {
        this.mob.setAttacking(false);
        this.mob.setAimingBow(false);
        this.seeTime = 0;
        this.returnPathRecalcCooldown = 0;
    }

    @Override
    public BattleModeType getBattleModeType() {
        return BattleModeType.BOW;
    }
}
