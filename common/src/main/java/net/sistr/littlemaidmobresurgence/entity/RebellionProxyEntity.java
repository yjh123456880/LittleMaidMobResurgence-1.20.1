package net.sistr.littlemaidmobresurgence.entity;

import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.world.World;
import net.sistr.littlemaidmobresurgence.setup.Registration;

/**
 * 反叛伤害代理实体：绑定玩家的无敌隐身实体。
 *
 * <p>反叛女仆优先攻击它，它把受到的伤害按原伤害类型/数值镜像给绑定玩家
 * （mobAttack/武器伤害属于可格挡类型，玩家可正常举盾格挡）。同一玩家全局只存在一个，
 * 仅在「本世界仍有反叛女仆针对该玩家」期间存活，玩家消失/死亡时立即移除。
 */
public class RebellionProxyEntity extends LivingEntity {
    private static final String BOUND_PLAYER_KEY = "BoundPlayer";
    /** 同世界内查找代理/反叛女仆的扫描半径（反叛稀少，偶尔全图扫描可接受）。 */
    private static final double SCAN_RANGE = 1.0E7;

    private UUID boundPlayerUuid;

    public RebellionProxyEntity(EntityType<? extends RebellionProxyEntity> type, World world) {
        super(type, world);
        this.setInvisible(true);
        this.setNoGravity(true);
        this.noClip = true;
    }

    /** 获取/创建绑定玩家的代理实体（同世界已存在则复用，保证每玩家仅一个）。 */
    public static RebellionProxyEntity getOrCreate(ServerWorld world, PlayerEntity player) {
        List<RebellionProxyEntity> existing =
                world.getEntitiesByClass(
                        RebellionProxyEntity.class,
                        player.getBoundingBox().expand(SCAN_RANGE),
                        proxy -> proxy.isAlive() && proxy.isBoundTo(player));
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        RebellionProxyEntity proxy =
                new RebellionProxyEntity(Registration.REBELLION_PROXY_ENTITY.get(), world);
        proxy.boundPlayerUuid = player.getUuid();
        // 让代理实体的"眼睛"与玩家眼睛同高：保证女仆正视玩家面部，且射向代理的视线不会被天花板挡住
        proxy.refreshPositionAndAngles(
                player.getX(),
                player.getEyeY() - proxy.getStandingEyeHeight(),
                player.getZ(),
                0.0F,
                0.0F);
        world.spawnEntity(proxy);
        return proxy;
    }

    public boolean isBoundTo(PlayerEntity player) {
        return player != null && isBoundTo(player.getUuid());
    }

    public boolean isBoundTo(UUID playerUuid) {
        return playerUuid != null && playerUuid.equals(boundPlayerUuid);
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public boolean isPushable() {
        // 不与玩家/其它实体互相推挤（避免玩家被代理碰撞箱推动）
        return false;
    }

    @Override
    public boolean isSpectator() {
        // 让玩家准星拾取/Jade 类信息提示跳过代理：不阻挡左右键交互、不显示在 HUD 提示中。
        // 近战反叛仍直接攻击代理并镜像伤害；远程弹道穿过代理打到玩家时由 MixinLivingEntityDamage 重写来源。
        return true;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        // 代理实体不装备任何物品
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return List.of();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        PlayerEntity player = resolveBoundPlayer(world);
        if (player == null || !player.isAlive()) {
            this.discard();
            return;
        }
        // 跟随玩家：眼睛与玩家眼睛同高，保证反叛女仆正视玩家面部，且射向代理的视线不受天花板影响
        this.refreshPositionAndAngles(
                player.getX(),
                player.getEyeY() - this.getStandingEyeHeight(),
                player.getZ(),
                0.0F,
                0.0F);
        this.setVelocity(0.0, 0.0, 0.0);
        // 周期性检查：本世界已无针对该玩家的反叛女仆时移除
        if (this.age % 20 == 0 && !hasRebelliousMaidTargeting(world, player)) {
            this.discard();
        }
    }

    private PlayerEntity resolveBoundPlayer(ServerWorld world) {
        if (boundPlayerUuid == null) {
            return null;
        }
        return world.getPlayerByUuid(boundPlayerUuid);
    }

    private boolean hasRebelliousMaidTargeting(ServerWorld world, PlayerEntity player) {
        return !world.getEntitiesByClass(
                        LittleMaidEntity.class,
                        this.getBoundingBox().expand(SCAN_RANGE),
                        maid ->
                                maid.isAlive()
                                        && maid.isRebellious()
                                        && maid.isRebellionTarget(player))
                .isEmpty();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient) {
            return false;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        PlayerEntity player = resolveBoundPlayer(world);
        if (player == null || !player.isAlive()) {
            return false;
        }
        LittleMaidEntity maid = resolveRebelliousMaid(source);
        if (maid != null && maid.isRebellionTarget(player)) {
            // 反叛女仆的伤害：统一用自定义代理伤害类型镜像（可盾牌格挡 + 通用「xxx 死亡了」广播）
            DamageSource proxySource = RebellionDamageUtil.rebellionProxySource(world);
            if (proxySource != null) {
                player.damage(proxySource, amount);
            }
        } else if (RebellionDamageUtil.isFakePlayerAttack(source, world, player)
                && RebellionDamageUtil.hasRebelliousMaidTargeting(world, player)) {
            // 模组法术通过 FakePlayer 造成伤害（如新生魔艺 AN_Fake_Player）：无法沿 owner 链溯源，
            // 但代理碰撞箱仅会拦截反叛女仆的攻击，可安全视为反叛伤害 → 通用「xxx 死亡了」广播
            DamageSource proxySource = RebellionDamageUtil.rebellionProxySource(world);
            if (proxySource != null) {
                player.damage(proxySource, amount);
            }
        } else {
            // 其它来源（敌对生物弹道等）撞上代理碰撞箱：按原伤害来源/数值原样转给玩家，
            // 避免代理碰撞箱把敌对伤害"吞掉"导致玩家不受伤
            player.damage(source, amount);
        }
        // 代理自身无敌：不受伤、不死亡、不产生击退
        return false;
    }

    /** 解析伤害是否来自某只反叛女仆：优先看攻击者，其次沿直接来源实体的 owner 链追溯。 */
    private LittleMaidEntity resolveRebelliousMaid(DamageSource source) {
        if (source.getAttacker() instanceof LittleMaidEntity maid && maid.isRebellious()) {
            return maid;
        }
        LittleMaidEntity maid = findMaidOwner(source.getSource());
        return maid != null && maid.isRebellious() ? maid : null;
    }

    /** 沿实体 owner 链查找女仆（弹射物 getOwner / OwnableEntity getOwner / 实体本身）。 */
    private LittleMaidEntity findMaidOwner(Entity entity) {
        if (entity instanceof LittleMaidEntity maid) {
            return maid;
        }
        if (entity instanceof ProjectileEntity projectile
                && projectile.getOwner() instanceof LittleMaidEntity maid) {
            return maid;
        }
        // 兼容其它自定义弹射物（Ars/Goety 等）的 getOwner() 方法
        if (entity != null) {
            try {
                java.lang.reflect.Method method = entity.getClass().getMethod("getOwner");
                if (method.getReturnType().isAssignableFrom(Entity.class)
                        && method.invoke(entity) instanceof LittleMaidEntity maid) {
                    return maid;
                }
            } catch (ReflectiveOperationException ignored) {
                // 无 getOwner 方法或调用失败，跳过
            }
        }
        return null;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (boundPlayerUuid != null) {
            nbt.putUuid(BOUND_PLAYER_KEY, boundPlayerUuid);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid(BOUND_PLAYER_KEY)) {
            boundPlayerUuid = nbt.getUuid(BOUND_PLAYER_KEY);
        }
    }
}
