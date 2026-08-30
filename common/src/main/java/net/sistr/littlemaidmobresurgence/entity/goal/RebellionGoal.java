package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.Mode;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.MaidSpeech;
import net.sistr.littlemaidmobresurgence.entity.RebellionProxyEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;

/**
 * 愤怒反叛目标/攻击目标（仅非战斗模式兜底）。
 *
 * <p>女仆心情归零后反叛，追逐绑定玩家的无敌隐身代理实体（位于玩家眼高，反叛女仆会正视玩家面部），
 * 代理把受到的伤害镜像给玩家（可被盾牌格挡）。战斗模式由 {@link RebellionTargetGoal} + 女仆自身
 * 攻击模式处理（弓箭/枪械/魔法等按模式攻击代理实体）；本 Goal 仅在非攻击模式下兜底：
 * 复刻旧版挥击动画 + 每 20 tick 造成 5 点可格挡伤害。反叛结束（代理消失）后停止。
 */
public class RebellionGoal extends Goal {
    private final LittleMaidEntity maid;
    private LivingEntity target;
    private int attackCooldown = 0;

    public RebellionGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        return maid.isAlive()
                && maid.isRebellious()
                && !maid.getMode().map(Mode::isBattleMode).orElse(false)
                && resolveTarget() != null;
    }

    @Override
    public boolean shouldContinue() {
        // 注意：代理实体为隐藏自身会覆写 isSpectator()=true（用于不被准星拾取/Jade 显示），
        // 因此这里不能按 spectator 过滤目标——否则反叛非战斗攻击会因 shouldContinue 立即失败，
        // 女仆永远无法对代理挥动画/造成伤害。只需确认目标存活即可。
        return maid.isRebellious()
                && this.target != null
                && this.target.isAlive();
    }

    @Override
    public void start() {
        super.start();
        // 反叛时解除待机，避免站桩
        TameableUtil.setWait(maid, false);
        MaidSpeech.onRebellion(maid);
        attackCooldown = 20;
    }

    @Override
    public void stop() {
        this.target = null;
        this.maid.setTarget(null);
        this.maid.getNavigation().stop();
        super.stop();
    }

    @Override
    public void tick() {
        resolveTarget();
        if (this.target != null) {
            // 每tick重新指向代理实体，覆盖其它目标选择
            this.maid.setTarget(this.target);
        } else {
            this.maid.setTarget(null);
            return;
        }
        LivingEntity t = this.target;
        this.maid.getLookControl().lookAt(t, 30.0f, 30.0f);
        double sqDist = this.maid.squaredDistanceTo(t);
        double rangeSq = 4.0;
        if (sqDist > rangeSq) {
            this.maid.getNavigation().startMovingTo(t, 1.3);
        } else {
            this.maid.getNavigation().stop();
        }
        this.attackCooldown = Math.max(0, this.attackCooldown - 1);
        if (sqDist <= rangeSq && this.attackCooldown == 0) {
            // 非战斗模式：调用攻击动画时造成一次 4 点可格挡伤害（代理原样镜像给玩家）；
            // 攻击频率与女仆自身近战攻击频率一致（攻速 + 剑客攻击率系数）
            this.attackCooldown = meleeAttackInterval();
            this.maid.swingHand(Hand.MAIN_HAND);
            t.damage(this.maid.getWorld().getDamageSources().mobAttack(this.maid),
                    (float) LMMRMod.getConfig().health.rebellionMeleeDamage);
            if (this.maid.getRandom().nextInt(5) == 0) {
                MaidSpeech.onRebellionAttack(maid);
            }
        }
    }

    /** 女仆自身近战攻击间隔（tick）：与 FencerMode 一致，基于攻击速度属性。 */
    private int meleeAttackInterval() {
        double attackSpeed =
                this.maid.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
        return MathHelper.ceil(
                1 / attackSpeed * 20 / LMMRMod.getConfig().work.fencerAttackRateFactor);
    }

    /**
     * 解析反叛目标：怒气目标玩家 → 其绑定代理实体（不存在则创建）。
     *
     * <p>服务端按 UUID 精确查询玩家（{@link ServerWorld#getEntity(UUID)}）；
     * 玩家不在本世界（换维度）时返回 null，反叛停转直至怒气消退。
     */
    private LivingEntity resolveTarget() {
        if (!maid.isRebellious()) {
            this.target = null;
            return null;
        }
        UUID uuid = maid.maidMood.getAngerTargetUuid().orElse(null);
        if (uuid == null) {
            this.target = null;
            return null;
        }
        if (maid.getWorld() instanceof ServerWorld serverWorld) {
            Entity resolved = serverWorld.getEntity(uuid);
            if (resolved instanceof PlayerEntity player && player.isAlive()) {
                this.target = RebellionProxyEntity.getOrCreate(serverWorld, player);
            } else {
                this.target = null;
            }
        } else {
            this.target = null;
        }
        return this.target;
    }
}
