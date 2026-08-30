package net.sistr.littlemaidmobresurgence.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sistr.littlemaidmobresurgence.LMMRMod;

/**
 * [zh] 反叛伤害来源统一识别工具。
 *     新生魔艺/诡厄巫法/枪械等模组的法术常以各自 FakePlayer（如 AN_Fake_Player）作为伤害来源，
 *     无法沿 owner 链追溯到女仆实体；本类通过「来源是 PlayerEntity 但并非本世界真实玩家」来识别这类伤害。
 * [en] Utility for uniformly identifying rebellious-maid damage sources.
 *     Spell mods (Ars Nouveau/Goety/guns) often attribute damage to FakePlayers (e.g. AN_Fake_Player),
 *     which cannot be traced to the maid via the owner chain; this class detects them by
 *     "source is a PlayerEntity that is not a real player in this world".
 * [ja] 反乱ダメージソースを統一的に識別するユーティリティ。
 *     新生魔藝/Goety/銃などの呪文は独自の FakePlayer（例：AN_Fake_Player）をソースにするため、
 *     オーナー連鎖でメイドへ遡れません。本クラスは「PlayerEntity だが実在プレイヤーではない」ことで識別します。
 */
public final class RebellionDamageUtil {
    private RebellionDamageUtil() {}

    /**
     * [zh] 伤害来源是否为某模组的 FakePlayer（伪玩家替身），而非真实玩家。
     * [en] Whether the damage source is a mod's FakePlayer (dummy player) rather than a real player.
     * [ja] ダメージソースがモッドの FakePlayer（偽プレイヤー）であり、実在プレイヤーではないかを判定します。
     */
    public static boolean isFakePlayerAttack(DamageSource source, ServerWorld world, PlayerEntity boundPlayer) {
        if (source == null || world == null) {
            return false;
        }
        Entity entity = source.getSource() != null ? source.getSource() : source.getAttacker();
        if (!(entity instanceof PlayerEntity fakePlayer)) {
            return false;
        }
        // [zh] 排除绑定玩家自身（真实玩家相互攻击不应被当作反叛）
        // [en] Exclude the bound player himself (real player-vs-player damage is not rebellion).
        // [ja] 紐付きプレイヤー自身を除外（実プレイヤー同士の攻撃は反乱扱いしない）。
        if (boundPlayer != null && fakePlayer.getUuid().equals(boundPlayer.getUuid())) {
            return false;
        }
        // [zh] 伪玩家不在本世界的玩家列表中（真实在线玩家 getPlayerByUuid 能查到）
        // [en] A FakePlayer is absent from this world's player list (real online players are found by getPlayerByUuid).
        // [ja] 偽プレイヤーはワールドのプレイヤー一覧に存在しません（実在プレイヤーは getPlayerByUuid で引けます）。
        return world.getPlayerByUuid(fakePlayer.getUuid()) == null;
    }

    /**
     * [zh] 构造带通用死亡消息（xxx 死亡了）的反叛代理伤害来源。
     * [en] Builds the rebellion-proxy damage source with the generic death message.
     * [ja] 汎用死亡メッセージ（〇〇は死んだ）を持つ反乱プロキシのダメージソースを構築します。
     */
    public static DamageSource rebellionProxySource(World world) {
        RegistryEntry<DamageType> type =
                world.getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .getEntry(
                                RegistryKey.of(
                                        RegistryKeys.DAMAGE_TYPE,
                                        new Identifier(LMMRMod.MODID, "rebellion_proxy")))
                        .orElse(null);
        return type != null ? new DamageSource(type) : null;
    }

    /**
     * [zh] 本世界是否存在以 boundPlayer 为怒气目标的反叛女仆。
     * [en] Whether any rebellious maid in this world targets boundPlayer.
     * [ja] このワールドに boundPlayer を怒りの対象とする反乱メイドがいるかを判定します。
     */
    public static boolean hasRebelliousMaidTargeting(ServerWorld world, PlayerEntity boundPlayer) {
        return !world.getEntitiesByClass(
                        LittleMaidEntity.class,
                        boundPlayer.getBoundingBox().expand(1.0E7),
                        maid ->
                                maid.isAlive()
                                        && maid.isRebellious()
                                        && maid.isRebellionTarget(boundPlayer))
                .isEmpty();
    }
}
