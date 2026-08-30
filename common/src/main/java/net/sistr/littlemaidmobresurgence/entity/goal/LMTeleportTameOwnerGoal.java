package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import net.sistr.littlemaidmobresurgence.LMMRMod;

public class LMTeleportTameOwnerGoal extends TeleportTameOwnerGoal<LittleMaidEntity> {
    protected final LittleMaidEntity maid;

    public LMTeleportTameOwnerGoal(LittleMaidEntity maid, Supplier<Float> teleportStart) {
        super(maid, teleportStart);
        this.maid = maid;
    }

    @Override
    public boolean canStart() {
        // 逃跑中不传送（否则会瞬间被拉回主人身边，逃跑失效）
        if (this.maid.isFleeing()) {
            return false;
        }
        // 休息/避战期间不传送到主人身边（恢复状态不受跟随模式限制）
        if (this.maid.isInRecoveryState()) {
            return false;
        }
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) {
            return false;
        }
        // 同维度时提高传送门槛（默认 16 → 32 格）：让女仆先主动跑回主人身边，
        // 而不是一超范围就被瞬间传送（跨维度仍按原距离传送，因为跑不回去）。
        LivingEntity owner = TameableUtil.getTameOwner(this.maid).orElse(null);
        if (owner != null && owner.getWorld() == this.maid.getWorld()) {
            float far = LMMRMod.getConfig().movement.teleportStartDistance * 2;
            if (this.maid.squaredDistanceTo(owner) < far * far) {
                return false;
            }
        }
        return super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) {
            return false;
        }
        return super.shouldContinue();
    }
}
