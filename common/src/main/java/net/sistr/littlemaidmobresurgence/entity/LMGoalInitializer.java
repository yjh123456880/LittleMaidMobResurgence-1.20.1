package net.sistr.littlemaidmobresurgence.entity;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.MobEntity;
import net.sistr.littlemaidmobresurgence.config.LMMRConfig;
import net.sistr.littlemaidmobresurgence.entity.goal.*;
import net.sistr.littlemaidmobresurgence.entity.mode.ModeWrapperGoal;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import net.sistr.littlemaidmobresurgence.tags.LMTags;

final class LMGoalInitializer {

    private LMGoalInitializer() {}

    static void initGoals(LittleMaidEntity maid) {
        int priority = -1;
        LMMRConfig config = LittleMaidEntity.getConfig();

        // 愤怒反叛（最高优先级，覆盖其它行为）
        maid.getGoalSelector().add(--priority, new RebellionGoal(maid));
        // 反叛-战斗模式：仅设目标为代理实体，让女仆自身攻击模式去攻击代理
        maid.getGoalSelector().add(--priority, new RebellionTargetGoal(maid));

        maid.getGoalSelector().add(++priority, new SwimGoal(maid));
        maid.getGoalSelector().add(++priority, new LongDoorInteractGoal(maid, true));

        maid.getGoalSelector().add(++priority, new LMCollectSalaryFromContainerGoal<>(maid));

        maid.getGoalSelector().add(++priority, new WaitGoal<>(maid));

        maid.getGoalSelector()
                .add(
                        ++priority,
                        new LMTeleportTameOwnerGoal(
                                maid, () -> config.movement.teleportStartDistance));

        // 危険な敵からの逃避
        maid.getGoalSelector()
                .add(
                        ++priority,
                        new FleeEntityGoal<>(
                                maid,
                                MobEntity.class,
                                config.target.dangerousAvoidDistance,
                                config.movement.followSpeed,
                                config.movement.sprintSpeed,
                                entity -> maid.getFleeEntities().containsKey(entity)) {
                            @Override
                            public void tick() {
                                maid.getFleeEntities()
                                        .entrySet()
                                        .removeIf(entry -> entry.getValue().test(entry.getKey()));
                                super.tick();
                            }

                            @Override
                            public void stop() {
                                super.stop();
                                this.mob.getNavigation().stop();
                            }
                        });

        // 跟随模式硬范围限定（高于工作/跟随，低于危险敌逃避）
        maid.getGoalSelector().add(++priority, new FollowRangeConfinementGoal(maid));

        // 避战逃跑（战斗模式低血量时主动远离敌人）
        maid.getGoalSelector().add(++priority, new EvadeCombatGoal(maid));

        // 休息踱步（起身阶段在休息点附近走动，不受行动模式限制）
        maid.getGoalSelector().add(++priority, new RestPaceGoal(maid));

        maid.getGoalSelector()
                .add(
                        ++priority,
                        new ModeWrapperGoal<>(maid) {
                            @Override
                            public boolean canStart() {
                                return !this.owner.isStrike()
                                        && !maid.isFleeing()
                                        && !maid.isInRecoveryState()
                                        && super.canStart();
                            }

                            @Override
                            public boolean shouldContinue() {
                                return !this.owner.isStrike()
                                        && !maid.isFleeing()
                                        && !maid.isInRecoveryState()
                                        && super.shouldContinue();
                            }
                        });

        maid.getGoalSelector()
                .add(
                        ++priority,
                        new HasMMFollowTameOwnerGoal<>(
                                maid,
                                () -> config.movement.sprintSpeed,
                                () -> config.movement.sprintStartDistance,
                                () -> config.movement.sprintEndDistance) {
                            @Override
                            public void start() {
                                super.start();
                                this.tameable.setSprinting(true);
                            }

                            @Override
                            public void stop() {
                                super.stop();
                                this.tameable.setSprinting(false);
                            }
                        });

        maid.getGoalSelector()
                .add(
                        ++priority,
                        new FollowAtHeldItemGoal<>(
                                maid,
                                () -> config.misc.stareAtSalaryRange,
                                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY),
                                () -> config.misc.followAtHeldSalaryRange,
                                true));
        maid.getGoalSelector()
                .add(
                        ++priority,
                        new LittleMaidEntity.LMStareAtHeldItemGoal<>(
                                maid,
                                () -> config.misc.stareAtSalaryRange,
                                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY),
                                true));

        // todo 頭の装飾品を仕舞わないようにする
        maid.getGoalSelector()
                .add(
                        ++priority,
                        new LMStoreItemToContainerGoal<>(
                                maid,
                                stack ->
                                        stack.isIn(LMTags.Items.MAIDS_SALARY)
                                                || maid.hasModeImpl
                                                        .getMode()
                                                        .filter(
                                mode ->
                                        mode.getModeType()
                                                .isModeItem(stack))
                                        .isPresent(),
                                () -> config.work.workRange));

        maid.getGoalSelector()
                .add(
                        ++priority,
                        new LMMoveToDropItemGoal(
                                maid,
                                () -> config.movement.pickupItemRange,
                                () -> config.movement.pickupItemFrequency,
                                () -> config.movement.pickupItemSpeed) {
                            @Override
                            public boolean canStart() {
                                return TameableUtil.hasTameOwner(maid)
                                        && !maid.isInRecoveryState()
                                        && super.canStart();
                            }

                            @Override
                            public List<ItemEntity> findAroundDropItem() {
                                // 默认跟随状态也捡主人附近掉落物；
                                // 仅在 pickupItemIgnoreOwnerFront 开启时才过滤主人前方物品（不抢战利品）
                                if (!config.movement.pickupItemIgnoreOwnerFront) {
                                    return super.findAroundDropItem();
                                }
                                return TameableUtil.getTameOwner(maid)
                                        .map(
                                                owner ->
                                                        super.findAroundDropItem().stream()
                                                                .filter(
                                                                        item ->
                                                                                !this.isOwnerRange(
                                                                                        item,
                                                                                        owner))
                                                                .collect(Collectors.toList()))
                                        .orElse(super.findAroundDropItem());
                            }
                        });

        maid.getGoalSelector()
                .add(
                        ++priority,
                        new HasMMFollowTameOwnerGoal<>(
                                maid,
                                () -> config.movement.followSpeed,
                                () -> config.movement.followStartDistance,
                                () -> config.movement.followEndDistance));

        maid.getGoalSelector().add(++priority, new PlaySnowGoal(maid));

        maid.getGoalSelector()
                .add(++priority, new RedstoneTraceGoal(maid, () -> config.movement.tracerSpeed));
        maid.getGoalSelector()
                .add(
                        ++priority,
                new FreedomGoal<>(
                                maid,
                                config.movement.freedomSpeed,
                                () -> config.work.workRange));

        // 野良
        maid.getGoalSelector()
                .add(
                        ++priority,
                        new LMMoveToDropItemGoal(
                                maid,
                                () -> config.movement.pickupItemRange,
                                () -> config.movement.pickupItemFrequency,
                                () -> config.movement.pickupItemSpeed) {
                            @Override
                            public boolean canStart() {
                                return !TameableUtil.hasTameOwner(maid)
                                        && config.misc.canPickupItemByNoOwner
                                        && !maid.isInRecoveryState()
                                        && super.canStart();
                            }
                        });
        maid.getGoalSelector()
                .add(
                        ++priority,
                        new EscapeDangerGoal(maid, config.movement.escapeSpeed) {
                            @Override
                            public boolean canStart() {
                                return !TameableUtil.hasTameOwner(maid) && super.canStart();
                            }
                        });
        maid.getGoalSelector()
                .add(
                        ++priority,
                        new FollowAtHeldItemGoal<>(
                                maid,
                                () -> config.misc.stareAtEmployItemRange,
                                stack -> stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE),
                                () -> config.misc.followAtHeldEmployItemRange,
                                false));
        maid.getGoalSelector()
                .add(
                        ++priority,
                        new LittleMaidEntity.LMStareAtHeldItemGoal<>(
                                maid,
                                () -> config.misc.stareAtEmployItemRange,
                                stack -> stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE),
                                false));

        maid.getGoalSelector()
                .add(
                        ++priority,
                        new WanderAroundFarGoal(maid, config.movement.freedomSpeed) {
                            @Override
                            public boolean canStart() {
                                return !TameableUtil.hasTameOwner(maid) && super.canStart();
                            }
                        });

        // 視線
        maid.getGoalSelector()
                .add(++priority, new LookAtEntityGoal(maid, LivingEntity.class, 8.0F));
        maid.getGoalSelector().add(priority, new LookAroundGoal(maid));

        // ターゲット系
        maid.getTargetSelector().add(0, new LMTargetGoal(maid));
    }
}
