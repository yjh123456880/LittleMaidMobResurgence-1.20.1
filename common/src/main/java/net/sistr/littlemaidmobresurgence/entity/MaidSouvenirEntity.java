package net.sistr.littlemaidmobresurgence.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.sistr.littlemaidmobresurgence.mixin.ItemEntityAccessor;
import net.sistr.littlemaidmobresurgence.setup.Registration;

/**
 * 女仆纪念品掉落物实体：永不消失、免疫伤害（火/岩浆/爆炸等）、持续高亮。
 *
 * <p>对齐 1.12.2 小女仆模组的 {@code LMEntityItemAntiDamage}：只有虚空能销毁纪念品。
 */
public class MaidSouvenirEntity extends ItemEntity {
    public MaidSouvenirEntity(EntityType<? extends MaidSouvenirEntity> type, World world) {
        super(type, world);
        this.setGlowing(true);
        this.setInvulnerable(true);
    }

    public MaidSouvenirEntity(World world, double x, double y, double z, ItemStack stack) {
        this(Registration.MAID_SOUVENIR_ENTITY.get(), world);
        this.setPosition(x, y, z);
        this.setStack(stack);
        this.setVelocity(0.0, 0.1, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        // 永不消失：ItemEntity 默认 6000 tick 后 despawn，这里持续重置存活时间
        if (this.getItemAge() >= 5900) {
            ((ItemEntityAccessor) (Object) this).setItemAge(0);
        }
        // 持续高亮（车万女仆魂符/胶片同款发光轮廓）
        if (!this.isGlowing()) {
            this.setGlowing(true);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // 除虚空外完全免疫伤害：爆炸/岩浆/火烧都不会销毁纪念品
        if (source.isOf(DamageTypes.OUT_OF_WORLD)) {
            return super.damage(source, amount);
        }
        return false;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }
}
