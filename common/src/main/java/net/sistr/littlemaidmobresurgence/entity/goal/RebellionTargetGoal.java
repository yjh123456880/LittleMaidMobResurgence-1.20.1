package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sistr.littlemaidmobresurgence.api.mode.Mode;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.RebellionProxyEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;

/**
 * 反叛目标设定 Goal（仅限战斗模式）。
 *
 * <p>反叛时把女仆的目标设为绑定玩家的代理实体（位于玩家眼高），随后由女仆自身的攻击模式
 * （剑客/弓箭/枪手/魔法等）去追逐并攻击代理实体；仅持有 TARGET 控制位，不占用移动/视线，
 * 因此 ModeWrapperGoal 等模式 Goal 可正常执行。
 */
public class RebellionTargetGoal extends Goal {
    private final LittleMaidEntity maid;

    public RebellionTargetGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        return maid.isAlive()
                && maid.isRebellious()
                && maid.getMode().map(Mode::isBattleMode).orElse(false)
                && resolveProxy() != null;
    }

    @Override
    public boolean shouldContinue() {
        return maid.isRebellious() && resolveProxy() != null;
    }

    @Override
    public void start() {
        super.start();
        // 反叛时解除待机，避免待机 Goal 占用移动/视线导致攻击模式无法运行
        TameableUtil.setWait(maid, false);
    }

    @Override
    public void tick() {
        LivingEntity proxy = resolveProxy();
        maid.setTarget(proxy);
    }

    @Override
    public void stop() {
        this.maid.setTarget(null);
        super.stop();
    }

    /** 解析怒气目标玩家 → 其绑定代理实体（不存在则创建）。 */
    private LivingEntity resolveProxy() {
        if (maid.getWorld().isClient) {
            return null;
        }
        UUID uuid = maid.maidMood.getAngerTargetUuid().orElse(null);
        if (uuid == null) {
            return null;
        }
        if (maid.getWorld() instanceof ServerWorld serverWorld) {
            Entity resolved = serverWorld.getEntity(uuid);
            if (resolved instanceof PlayerEntity player && player.isAlive()) {
                return RebellionProxyEntity.getOrCreate(serverWorld, player);
            }
        }
        return null;
    }
}
