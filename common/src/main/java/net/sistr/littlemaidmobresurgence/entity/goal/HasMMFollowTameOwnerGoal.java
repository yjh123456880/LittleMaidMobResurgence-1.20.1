package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.function.Supplier;
import net.minecraft.entity.passive.TameableEntity;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.HasMovingMode;
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;

public class HasMMFollowTameOwnerGoal<T extends TameableEntity & HasMovingMode>
        extends FollowTameOwnerGoal<T> {

    public HasMMFollowTameOwnerGoal(
            T tameable,
            Supplier<Float> speed,
            Supplier<Float> followStart,
            Supplier<Float> followEnd) {
        super(tameable, speed, followStart, followEnd);
    }

    @Override
    public boolean canStart() {
          // 逃跑中不跟随（否则会立即把逃跑路径拉回主人身边）
          if (this.tameable instanceof LittleMaidEntity maid && maid.isFleeing()) {
              return false;
          }
          // 休息/避战期间不跟随（恢复状态不受跟随模式限制）
          if (this.tameable instanceof LittleMaidEntity maid && maid.isInRecoveryState()) {
              return false;
          }
          return this.tameable.getMovingMode() == MovingMode.ESCORT && super.canStart();
    }
}
