package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetTagManager;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetingConfig;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetingSystem;
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;

/**
 * メイドさんのターゲット選択ゴール 3段階優先度システムで敵を選択し、危険な敵からの避難も処理する
 *
 * <p>優先度階層: - CRITICAL: 自分を攻撃した敵 - HIGH: ご主人を攻撃した敵、ご主人が攻撃した敵 - NORMAL: 他のメイドさんを攻撃した敵、周囲の敵対モブ
 */
public class LMTargetGoal extends Goal {
    private final LittleMaidEntity maid;
    private MobEntity target;
    private int recalc = 0;

    public LMTargetGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        // 反叛期间由 RebellionGoal 接管目标，避免常规目标选择覆盖反叛目标
        if (this.maid.isRebellious()) {
            return false;
        }
        int chance = TargetingConfig.getTargetingInterval();
        if (this.maid.getRandom().nextInt(getTickCount(chance)) != 0) {
            return false;
        }

        return targeting();
    }

    private boolean targeting() {
        // 範囲内に敵がいるかチェック
        var aroundMobs = getAroundMobs();
        if (aroundMobs.isEmpty()) {
            this.maid.setTarget(null);
            return false;
        }
        var aroundMaids = getAroundMaids();
        TargetTagManager targetTagManager = this.maid;

        // 3段階優先度システムでターゲット選択、分散ターゲティングも考慮
        var target =
                TargetingSystem.selectTarget(
                        new TargetingSystem.Maid(this.maid),
                        aroundMobs.stream().map(mob -> new TargetingSystem.Mob(mob)).toList(),
                        TameableUtil.getTameOwner(this.maid)
                                .map(TargetingSystem.Master::new)
                                .orElse(null),
                        aroundMaids.stream().map(TargetingSystem.Maid::new).toList(),
                        this.maid.isBloodSuck(),
                        targetTagManager);

        // 危険敵からの避難処理（クリーパー等から距離を取る）
        var enemies = aroundMobs.stream().map(mob -> new TargetingSystem.Mob(mob)).toList();
        var maidWrapper = new TargetingSystem.Maid(this.maid);
        if (TargetingSystem.needsEvacuation(maidWrapper, enemies, targetTagManager)) {
            TargetingSystem.getDangerousEnemies(maidWrapper, enemies, targetTagManager)
                    .forEach(
                            mob ->
                                    this.maid.addFleeEntity(
                                            mob.getMob(),
                                            e ->
                                                    !e.isAlive()
                                                            || this.maid.squaredDistanceTo(e)
                                                                    > (TargetingConfig
                                                                                            .getDangerousAvoidDistance()
                                                                                    + 4)
                                                                            * (TargetingConfig
                                                                                            .getDangerousAvoidDistance()
                                                                                    + 4)));
        }

        // ターゲット設定
        if (target.isPresent()) {
            this.target = target.get();
            this.maid.setTarget(target.get());
            return true;
        }

        this.maid.setTarget(null);
        return false;
    }

    @Override
    public boolean shouldContinue() {
        // 反叛期间不参与常规目标选择
        if (this.maid.isRebellious()) {
            return false;
        }
        // 攻撃を受けたら再計算(tick順の関係で実行されないことを防ぐため、ageに-1する)
        if (getTickCount(this.maid.getLastAttackedTime()) == getTickCount(this.maid.age - 1)) {
            return targeting();
        }
        // 現在のターゲットがまだ有効かチェック
        if (!isTargetable(this.target, getSearchRange())) {
            // ターゲットが居なくなったら再計算
            return targeting();
        }
        // 再計算カウンター
        recalc = Math.max(0, recalc - 1);
        if (recalc > 0) {
            recalc = getTickCount(TargetingConfig.getTargetingInterval());
            return true;
        }
        // 状況の変化により優先度を再計算する
        return targeting();
    }

    @Override
    public void start() {
        super.start();
        // ターゲット確定時の初期設定
        recalc = getTickCount(TargetingConfig.getTargetingInterval());
    }

    @Override
    public void stop() {
        super.stop();
        // ターゲットのクリア
        recalc = 0;
        this.target = null;
        this.maid.setTarget(null);
    }

    private List<MobEntity> getAroundMobs() {
        float distance = getSearchRange();
        return this.maid
                .getWorld()
                .getEntitiesByClass(
                        MobEntity.class,
                        this.maid
                                .getBoundingBox()
                                .expand(distance, distance / 2f, distance)
                                .expand(1),
                        mob ->
                                mob != this.maid
                                        && isTargetable(mob, distance)
                                        && this.maid.getVisibilityCache().canSee(mob));
    }

    private boolean isTargetable(MobEntity mob, float distance) {
        return this.maid.squaredDistanceTo(mob) <= distance * distance
                && maid.canTarget(mob) // isFriend()とcanTakeDamage()判定込み
                && mob.isAlive();
    }

    private List<LittleMaidEntity> getAroundMaids() {
        float distance = getSearchRange();
        return this.maid
                .getWorld()
                .getEntitiesByClass(
                        LittleMaidEntity.class,
                        this.maid
                                .getBoundingBox()
                                .expand(distance, distance / 2f, distance)
                                .expand(1),
                        maid -> maid != this.maid);
    }

    /**
     * [zh] 索敌半径：在默认警戒范围内再收敛到女仆当前移动模式的限定半径
     *     （自由=工作范围、跟随=跟随范围），红石巡逻保持默认警戒范围。
     * [en] Target-search radius: the default alert range is additionally capped by the maid's current
     *     movement-mode confinement radius (freedom = work range, follow = follow range).
     * [ja] 索敵半径：既定警戒範囲を、現在の移動モードの制限半径（自由=作業範囲、追従=追従範囲）で上限します。
     */
    private float getSearchRange() {
        float alert = TargetingConfig.getAlertRange();
        float radius;
        if (this.maid.getMovingMode() == MovingMode.FREEDOM) {
            radius = LMMRMod.getConfig().work.workRange;
        } else if (this.maid.getMovingMode() == MovingMode.ESCORT) {
            radius = LMMRMod.getConfig().movement.followRange;
        } else {
            return alert;
        }
        return Math.min(alert, radius);
    }
}
