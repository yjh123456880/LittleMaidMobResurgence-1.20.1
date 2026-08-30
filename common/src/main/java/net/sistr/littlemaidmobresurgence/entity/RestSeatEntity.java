package net.sistr.littlemaidmobresurgence.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * 女仆休息座位：不可见、无碰撞、不可被玩家交互选中的"座位"实体。
 *
 * <p>女仆骑乘它以播放骑乘（坐下）动画并保持原地不动；女仆离开座位后自动移除。
 */
public class RestSeatEntity extends Entity {
    public RestSeatEntity(EntityType<?> type, World world) {
        super(type, world);
        this.setInvisible(true);
        this.setNoGravity(true);
        this.noClip = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void initDataTracker() {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isSpectator() {
        // 不被玩家准星拾取 / HUD 信息提示选中
        return true;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.setVelocity(0.0, 0.0, 0.0);
        // 女仆离开座位后自动移除
        if (!this.getWorld().isClient && this.getPassengerList().isEmpty()) {
            this.discard();
        }
    }
}
