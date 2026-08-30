package net.sistr.littlemaidmobresurgence.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.RebellionDamageUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * [zh] 反叛女仆伤害来源统一重写。
 *     新生魔艺/诡厄巫法/枪械等模组的法术弹道可能绕过代理实体直接命中玩家（来源为 FakePlayer）。
 *     本 mixin 在玩家受击时，若伤害来源能沿 owner 链追溯到某只反叛女仆且怒气目标正是该玩家，
 *     就把来源重写为自定义 rebellion_proxy 伤害类型（可盾牌格挡 + 通用死亡广播）。
 * [en] Uniformly rewrites the damage source of rebellious-maid attacks.
 *     Spell projectiles from Ars Nouveau/Goety/guns can bypass the proxy and hit the player with FakePlayer sources;
 *     when the source can be traced along the owner chain to a rebellious maid targeting that player,
 *     the source is rewritten to the custom rebellion_proxy damage type (shield-blockable + generic death message).
 * [ja] 反乱メイドのダメージソースを統一的に書き換えます。
 *     新生魔藝/Goety/銃などの弾道がプロキシを迂回してプレイヤーに命中する場合（FakePlayer 由来）、
 *     オーナー連鎖から反乱メイドと判別できれば、ソースを独自の rebellion_proxy ダメージ種別
 *     （盾で防げる＋汎用死亡メッセージ）へ書き換えます。
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityDamage {

    @ModifyVariable(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private DamageSource lmr$rewriteRebellionSource(DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) {
            return source;
        }
        LittleMaidEntity maid = resolveRebelliousMaid(source);
        boolean fromRebelliousMaid = maid != null && maid.isRebellionTarget(player);
        // [zh] FakePlayer 法术来源（新生魔艺/诡厄巫法等）无法溯源到女仆；若确有反叛女仆以该玩家为目标，同样重写
        // [en] FakePlayer spell sources cannot be traced to a maid; if a rebellious maid does target this player, rewrite anyway.
        // [ja] FakePlayer 由来の呪文ダメージはメイドへ遡れません。反乱メイドがこのプレイヤーを狙っていれば同様に書き換えます。
        if (!fromRebelliousMaid
                && self.getWorld() instanceof ServerWorld server
                && RebellionDamageUtil.isFakePlayerAttack(source, server, player)
                && RebellionDamageUtil.hasRebelliousMaidTargeting(server, player)) {
            fromRebelliousMaid = true;
        }
        if (!fromRebelliousMaid) {
            return source;
        }
        DamageSource proxySource = RebellionDamageUtil.rebellionProxySource(self.getWorld());
        return proxySource != null ? proxySource : source;
    }

    /**
     * [zh] 沿伤害来源的 owner 链解析是否来自某只反叛女仆。
     * [en] Resolves whether the damage source traces back (via the owner chain) to a rebellious maid.
     * [ja] ダメージソースのオーナー連鎖から反乱メイド由来かを判定します。
     */
    private static LittleMaidEntity resolveRebelliousMaid(DamageSource source) {
        if (source == null) {
            return null;
        }
        if (source.getAttacker() instanceof LittleMaidEntity maid && maid.isRebellious()) {
            return maid;
        }
        LittleMaidEntity maid = findMaidOwner(source.getSource());
        return maid != null && maid.isRebellious() ? maid : null;
    }

    private static LittleMaidEntity findMaidOwner(Entity entity) {
        if (entity instanceof LittleMaidEntity maid) {
            return maid;
        }
        if (entity instanceof ProjectileEntity projectile
                && projectile.getOwner() instanceof LittleMaidEntity maid) {
            return maid;
        }
        if (entity != null) {
            try {
                java.lang.reflect.Method method = entity.getClass().getMethod("getOwner");
                if (method.getReturnType().isAssignableFrom(Entity.class)
                        && method.invoke(entity) instanceof LittleMaidEntity maid) {
                    return maid;
                }
            } catch (ReflectiveOperationException ignored) {
        // [zh] 无 getOwner 方法，跳过
        // [en] No getOwner method; skip.
        // [ja] getOwner メソッドが無い場合はスキップ。
            }
        }
        return null;
    }
}
