package net.sistr.littlemaidmobresurgence.entity;

import com.google.common.collect.Lists;
import dev.architectury.extensions.network.EntitySpawnExtension;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.MultiModelCompound;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayableCompound;
import net.sistr.littlemaidmodelloader.maidmodel.IModelCaps;
import net.sistr.littlemaidmodelloader.multimodel.IMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMPose;
import net.sistr.littlemaidmodelloader.resource.holder.ConfigHolder;
import net.sistr.littlemaidmodelloader.resource.holder.TextureHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMModelManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidmodelloader.resource.util.TextureColors;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.BattleModeType;
import net.sistr.littlemaidmobresurgence.api.mode.Mode;
import net.sistr.littlemaidmobresurgence.api.mode.ModeManager;
import net.sistr.littlemaidmobresurgence.config.LMMRConfig;
import net.sistr.littlemaidmobresurgence.entity.goal.*;
import net.sistr.littlemaidmobresurgence.entity.mode.HasMode;
import net.sistr.littlemaidmobresurgence.entity.mode.HasModeImpl;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetTagManager;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetingSystem;
import net.sistr.littlemaidmobresurgence.entity.util.*;
import net.sistr.littlemaidmobresurgence.item.BackpackUpgradeItem;
import net.sistr.littlemaidmobresurgence.item.MaidSouvenirItem;
import net.sistr.littlemaidmobresurgence.mixin.CrossbowItemInvoker;
import net.sistr.littlemaidmobresurgence.network.SpawnLittleMaidPacket;
import net.sistr.littlemaidmobresurgence.setup.Registration;
import net.sistr.littlemaidmobresurgence.tags.LMTags;
import net.sistr.littlemaidmobresurgence.util.LMCollidable;
import net.sistr.littlemaidmobresurgence.util.ReachAttributeUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * [zh] 小女仆实体本体：契约/雇佣、心情/好感、工作与战斗模式、饥饿/进食、盾牌格挡、
 *     反叛/避战/休息、TACZ 枪械与魔法联动、纪念品复活、强加载等核心逻辑均在此实现。
 * [en] The maid entity itself: contracting/employment, mood/favorability, work & battle modes, hunger/eating,
 *     shield blocking, rebellion/evade/rest states, TACZ gun & magic integration, souvenir revival, force chunk load, etc.
 * [ja] メイドさん本体。契約/雇用、機嫌・好感度、作業・戦闘モード、空腹・食事、盾ガード、
 *     反乱・回避・休息、TACZ 銃・魔法連携、記念品復活、強制チャンクロードなどを実装します。
 */
// [zh] 上游遗留的 TODO 清单（未实现/待完善项，保持原文便于追溯）
// [en] Legacy TODO list from upstream (unimplemented/pending items, kept verbatim for traceability)
// [ja] 上流由来のTODOリスト（未実装・未調整項目。原文のまま残し、追跡しやすくしています）
// todo 声タイミング調整 / voice timing / 声タイミング調整
// todo ドロップアイテム / drop items / ドロップアイテム
// todo 契約期間の残りは砂糖をあげた時の音符の色で判断してください。 / judge remaining contract by note color when fed sugar / 契約期間の残りは砂糖をあげた時の音符の色で判断してください。
// todo 雪合戦 日が暮れると遊びは終わり / snowball fight ends at nightfall / 雪合戦 日が暮れると遊びは終わり
// todo モードトリガーアイテム指定 / mode trigger item selection / モードトリガーアイテム指定
// todo 署名済みではない書き込み可能な本にパラメータを記述して、右クリックで値が反映される / writable-book parameter injection on right-click / 署名済みではない書き込み可能な本にパラメータを記述して右クリックで反映
// todo メイドさんも金リンゴや牛乳を飲めるように / let maids drink golden apples and milk / メイドさんも金リンゴや牛乳を飲めるように
// todo つまみ食い / snacking / つまみ食い
// todo ダメージ/水没待機解除 実装済みだっけ？ / damage/drowning wait-release implemented? / ダメージ/水没待機解除 実装済みだっけ？
// todo トランザム / trans-am / トランザム
// todo 経験値 / XP / 経験値
// todo 座ったメイドでも追従時に立つように / standing up when following even while sitting / 座ったメイドでも追従時に立つように
// todo スト時砂糖ドカ食い / stress-eating sugar during strike / スト時砂糖ドカ食い
// todo GUIを開いている時に動きを止める / stop moving while the GUI is open / GUIを開いている時に動きを止める
// todo リスポ / respawn / リスポ
// todo 死亡メッセ追加 / add death messages / 死亡メッセ追加
// todo はしご / ladders / はしご
// todo おさわり厳禁：他人のメイドに触ると殴られる / no touching: hitting on touching others' maids / おさわり厳禁：他人のメイドに触ると殴られる
// todo 他人のメイドに視線を合わせた時、ご主人の名札を浮かべる / show owner nametag when staring at others' maids / 他人のメイドに視線を合わせた時、ご主人の名札を浮かべる
public class LittleMaidEntity extends TameableEntity
        implements EntitySpawnExtension,
                HasInventory,
                Contractable,
                HasMode,
                AimingPoseable,
                IHasMultiModel,
                SoundPlayable,
                HasMovingMode,
                CrossbowUser,
                SalaryBoxPosListener,
                TargetTagManager {
    // LMM_FLAGSのindex
    // todo enumにまとめる
    private static final int WAIT_INDEX = 0;
    private static final int AIMING_INDEX = 1;
    private static final int BEGGING_INDEX = 2;
    private static final int BLOOD_SUCK_INDEX = 3;
    private static final int STRIKE_INDEX = 4;
    private static final int PLAYING_SNOW_INDEX = 5;
    private static final int PICKUP_ITEM_INDEX = 6;
    private static final TrackedData<Byte> LMM_FLAGS =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> MOVING_MODE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<String> MODE_NAME =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> CHARGING =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ACCELERATE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> MASTER_STANCE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    public static final TrackedData<Integer> MOOD =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> FAVORABILITY =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<String> SPEECH =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.STRING);
    public static final TrackedData<Integer> SPEECH_TIMER =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> HUNGER =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<ItemStack> EATING_STACK =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> REBELLING =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    /**
     * [zh] 自动进食阈值（0=不自动进食，40/60/80=饥饿低于该百分比时自动进食）。
     * [en] Auto-eat threshold (0 = disabled; 40/60/80 = eat when hunger falls below this percentage).
     * [ja] 自動食事しきい値（0=自動で食べない、40/60/80=満腹度がこの割合を下回ると食べる）。
     */
    private static final TrackedData<Integer> AUTO_EAT_THRESHOLD =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ANGER_TICKS =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /**
     * [zh] 休息坐姿渲染进度（0=站直，1=完全坐下），用于客户端平滑下压模型贴地。
     * [en] Rest-sit render progress (0 = standing, 1 = fully sitting) for smooth model lowering on the client.
     * [ja] 休息時の座り姿勢の描画進捗（0=立つ、1=完全に座る）。クライアントでモデルを沈めて地面に付けます。
     */
    private static final TrackedData<Float> REST_SIT_PROGRESS =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.FLOAT);
    /**
     * [zh] 是否正在「持糖消耗」（自动吃糖回饱食度时副手短暂持糖的动画状态）。
     * [en] Whether the "sugar consume" animation (briefly holding sugar in the off-hand) is active.
     * [ja] 「砂糖消費」アニメーション中かどうか（自動で砂糖を食べ満腹度回復する際のオフハンド表示）。
     */
    private static final TrackedData<Boolean> SUGAR_CONSUMING =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    /**
     * [zh] 是否开启「强加载」：女仆所在区块被强制加载，不会被清理/卸载。
     * [en] Whether "force chunk load" is enabled: the maid's chunk is force-loaded and never unloaded.
     * [ja] 「強制チャンクロード」が有効かどうか。メイドのいるチャンクを強制ロードし、アンロードされないようにします。
     */
    private static final TrackedData<Boolean> FORCE_CHUNK_LOAD =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int hungerTickCounter = 0;
    private int hungerEatCounter = 0;
    // [zh] 委托对象们
    // [en] Delegated components
    // [ja] 委譲コンポーネント群
    public final LMHasInventory littleMaidInventory = new LMHasInventory();
    /**
     * [zh] 背包扩容道具专属槽位（独立于 Curios，仅可放入扩容道具）。
     * [en] Dedicated slot for the backpack-upgrade item (separate from Curios; only upgrade items fit).
     * [ja] バックパック拡張アイテム専用スロット（Curios とは独立。拡張アイテムのみ入れられます）。
     */
    private final net.minecraft.inventory.SimpleInventory backpackUpgradeSlot =
            new net.minecraft.inventory.SimpleInventory(1);
    public final MaidMood maidMood = new MaidMood();
    private int hungerStarveCounter = 0;
    /**
     * [zh] 持糖消耗剩余 tick（糖在副手短暂展示后被消耗）。
     * [en] Remaining ticks of the sugar-consume animation before the sugar is consumed.
     * [ja] 砂糖消費の残りtick（オフハンドで一時表示した後に消費されます）。
     */
    private int sugarConsumeTicks = 0;
    /**
     * [zh] 持糖前副手的原物品（通常是盾牌），消耗结束后还原。
     * [en] The original off-hand item (usually a shield) saved before holding sugar and restored afterwards.
     * [ja] 砂糖を持つ前のオフハンドの元アイテム（通常は盾）。消費後に戻します。
     */
    private ItemStack sugarStoredOffHand = ItemStack.EMPTY;
    /**
     * [zh] 持糖消耗结束时是否恢复饱食度（自动吃糖=true；玩家喂糖=false，交互已即时恢复）。
     * [en] Whether to restore satiety when the sugar consume ends (auto-eat = true; player-fed = false, already restored at interaction).
     * [ja] 砂糖消費終了時に満腹度を回復するか（自動食事=true、プレイヤー給餌=false。給餌は即時回復済み）。
     */
    private boolean sugarRestoreHungerOnEnd = false;
    /**
     * [zh] 持糖消耗动画时长（tick，约 0.6 秒）。
     * [en] Sugar-consume animation duration (ticks; ~0.6 seconds).
     * [ja] 砂糖消費アニメーションの時間（tick、約0.6秒）。
     */
    private static final int SUGAR_CONSUME_DURATION = 12;
    /**
     * [zh] 服务端：当前被强制加载的区块坐标（{@link Integer#MIN_VALUE} 表示未强制）。
     * [en] Server-side: currently force-loaded chunk coordinates ({@link Integer#MIN_VALUE} = none).
     * [ja] サーバー側：現在強制ロード中のチャンク座標（{@link Integer#MIN_VALUE}=未強制）。
     */
    private int forceChunkX = Integer.MIN_VALUE;
    private int forceChunkZ = Integer.MIN_VALUE;
    /**
     * [zh] 进食动画剩余 tick。
     * [en] Remaining ticks of the eating animation.
     * [ja] 食べるアニメーションの残りtick。
     */
    private int eatingTicks = 0;
    /**
     * [zh] 上次应用血量的好感等级（-1=未初始化）。
     * [en] Last favorability level applied to max HP (-1 = uninitialized).
     * [ja] 最後にHPへ適用した好感度レベル（-1=未初期化）。
     */
    private int lastFavorHealthLevel = -1;
    /**
     * [zh] 被玩家攻击后的逃跑剩余 tick。
     * [en] Remaining ticks of fleeing after being attacked by a player.
     * [ja] プレイヤーに攻撃された後の逃走残りtick。
     */
    private int fleeingTicks = 0;
    /**
     * [zh] 普通格挡冷却（tick），与玩家盾牌格挡的短促节奏一致（默认 0.5 秒）。
     * [en] Normal block cooldown (ticks), matching the player's short shield-block rhythm (default 0.5s).
     * [ja] 通常ガードのクールダウン（tick）。プレイヤーの盾ガードと同じ短いリズム（既定0.5秒）。
     */
    private static final int SHIELD_BLOCK_COOLDOWN = 10;
    /**
     * [zh] 破盾攻击（斧头）格挡成功后盾牌的禁用时长（tick），对齐原版：100 tick = 5 秒。
     * [en] Shield-disable duration (ticks) after blocking a shield-breaker (axe) hit, matching vanilla: 100 ticks = 5s.
     * [ja] 破盾攻撃（斧）をガードした後の盾無効時間（tick）。バニラ準拠：100tick=5秒。
     */
    private static final int SHIELD_BREAK_DISABLE_TICKS = 100;
    /**
     * [zh] 破盾攻击对盾牌额外造成的耐久损耗（对应原版 damageShield(5) 的 1 + floor(5)）。
     * [en] Extra shield durability damage from shield-breaker hits (vanilla damageShield(5) = 1 + floor(5)).
     * [ja] 破盾攻撃が盾に与える追加耐久ダメージ（バニラ damageShield(5) の 1＋floor(5) に相当）。
     */
    private static final int SHIELD_BREAK_DURABILITY_DAMAGE = 1 + 5;
    /**
     * [zh] 服务端格挡冷却剩余时间（tick），冷却期间不再格挡。
     * [en] Server-side remaining block cooldown (ticks); no blocking while cooling down.
     * [ja] サーバー側のガードクールダウン残り（tick）。冷却中はガードしません。
     */
    private int shieldBlockCooldown = 0;
    /**
     * [zh] 破盾后盾牌禁用剩余时间（tick），期间不举起盾牌（对应原版盾牌物品冷却）。
     * [en] Remaining shield-disable ticks after a shield break; the shield is not raised meanwhile (vanilla shield-item cooldown).
     * [ja] 破盾後の盾無効残りtick。その間は盾を構えません（バニラの盾アイテム冷却に相当）。
     */
    private int shieldDisableTicks = 0;
    /**
     * [zh] 纪念品复活后的休息状态：血量恢复到自身 50% 前保持休息（替代原罢工复活）。
     * [en] Rest state after souvenir revival: keeps resting until HP recovers to 50% (replaces the old strike-on-revival).
     * [ja] 記念品復活後の休息状態：HPが最大値の50%まで回復するまで休息（従来のストライキ復活を置換）。
     */
    private boolean souvenirReviveRest = false;
    /**
     * [zh] 是否处于休息闩锁（血量 <5% 且无敌人进入，血量 ≥50% 解除）。
     * [en] Whether the rest latch is active (entered below 5% HP with no enemies; exits at ≥50% HP).
     * [ja] 休息ラッチ中かどうか（HP<5％かつ敵なしで開始、HP≥50％で解除）。
     */
    private boolean resting = false;
    /**
     * [zh] 当前是否处于坐姿阶段（休息时原地坐下）。
     * [en] Whether currently in the sitting phase (sitting still while resting).
     * [ja] 現在座っている段階かどうか（休息時にその場で座る）。
     */
    private boolean restSitting = false;
    /**
     * [zh] 休息阶段剩余 tick（坐/站各约 3.5 秒循环）。
     * [en] Remaining ticks of the rest phase (sitting/standing alternate every ~3.5 seconds).
     * [ja] 休息フェーズの残りtick（座る/立つを約3.5秒ごとに交互）。
     */
    private int restPhaseTimer = 0;
    /**
     * [zh] 附近敌人扫描冷却。
     * [en] Cooldown for the nearby-enemy scan.
     * [ja] 近隣敵スキャンのクールダウン。
     */
    private int restEnemyScanCooldown = 0;
    /**
     * [zh] 缓存：附近是否有敌对目标。
     * [en] Cache: whether any hostile target is nearby.
     * [ja] キャッシュ：近くに敵対目標がいるかどうか。
     */
    private boolean restNoEnemy = true;
    /**
     * [zh] 是否处于避战状态（战斗模式 + 血量 <5% + 附近有敌人）。
     * [en] Whether in the evade state (battle mode + below 5% HP + enemies nearby).
     * [ja] 回避状態かどうか（戦闘モード＋HP<5％＋敵が近くにいる）。
     */
    private boolean evading = false;
    /**
     * [zh] 避战期间连续无敌人 tick（达到 10 秒转休息）。
     * [en] Consecutive no-enemy ticks while evading (10 seconds switches to rest).
     * [ja] 回避中の連続無敵tick（10秒で休息へ移行）。
     */
    private int evadeNoEnemyTicks = 0;
    /**
     * [zh] 缓存的最近敌对目标（用于避战逃跑）。
     * [en] Cached nearest hostile (used for evade fleeing).
     * [ja] キャッシュ済みの直近の敵（回避時の逃走用）。
     */
    @Nullable private LivingEntity nearestHostile;
    /**
     * [zh] 休息起始位置（休息踱步时围绕该点走动，不受行动模式限制）。
     * [en] Rest anchor: the maid paces around this point while resting, ignoring moving-mode constraints.
     * [ja] 休息の起点（休息中の歩行はこの地点を中心に行い、移動モードの制約を受けません）。
     */
    @Nullable private BlockPos restAnchor;
    /** 女仆杖绑定的工作范围中心（仅当女仆处于自由行动时作为锁定圆心生效）。 */
    @Nullable private BlockPos boundWorkCenter;
    /** 绑定范围所在维度（与中心成对设置/清除）。 */
    @Nullable private Identifier boundWorkDimension;
    /** 收纳（捕捉蛋）时抑制纪念品掉落（瞬态，不写 NBT）。 */
    private boolean captureSuppressSouvenir = false;
    /** 反叛怒气粒子：特效时长（tick）。 */
    private static final int REBELLION_PARTICLE_DURATION = 20;
    /** 反叛怒气粒子：前一个特效消失后再等待 0.5 秒（10 tick）播放下一个。 */
    private static final int REBELLION_PARTICLE_GAP = 10;
    private int rebellionParticleCooldown = 0;
    /** 进食时暂存的副手物品（结束后恢复） */
    private ItemStack eatingStoredOffHand = ItemStack.EMPTY;
    /** 待恢复的饥饿值（进食动画结束后才加到饥饿值上）。 */
    private int pendingHungerRestore = 0;
    /** 本次进食是否由玩家喂食触发（区分台词来源：喂食用 onFed，自动进食用 onSelfEat，避免互相覆盖）。 */
    private boolean eatingPlayerFed = false;
    /** 変身糖で変身した元の動物のNBT。メイド→動物の復元に使う。 */
    @Nullable public NbtCompound animalMaidNbt;
    public final LMItemContractable<LittleMaidEntity> itemContractable =
            new LMItemContractable<>(
                    this,
                    () -> getConfig().contract.consumeSalaryInterval,
                    () -> getConfig().contract.unpaidDaysLimit,
                    (ItemStack stack) -> stack.isIn(LMTags.Items.MAIDS_SALARY));
    public final HasModeImpl hasModeImpl =
            new HasModeImpl(
                    this,
                    this,
                    new HashSet<>(),
                    mode -> {
                        setModeName(mode != null ? mode.getName() : "");
                    });
    public final MultiModelCompound multiModel;
    public final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private final IModelCaps caps = new LittleMaidModelCaps(this);
    private final LMSafeMovement safeMovement =
            new LMSafeMovement(this, LittleMaidEntity::getConfig, this::getDangerHeightThreshold);
    private final TargetTagManager targetTagManager;

    private final Map<MobEntity, Predicate<MobEntity>> fleeEntities =
            new HashMap<>(); // todo クラス化検討
    @Nullable private BlockPos freedomPos;

    // 首傾げのやつ
    @Environment(EnvType.CLIENT)
    private float interestedAngle;

    @Environment(EnvType.CLIENT)
    private float prevInterestedAngle;

    private int playSoundCool;
    private int idFactor;
    public int experiencePickUpDelay;
    // クライアント側のこの値は信用ならない
    private int accelerationTicks;
    private boolean maidManagerRegistered;

    // コンストラクタ
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new FixedMoveControl(this);
        ((MobNavigation) getNavigation()).setCanPathThroughDoors(true);
        multiModel =
                new MultiModelCompound(
                        this,
                        LMTextureManager.INSTANCE
                                .getTexture("Default")
                                .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")),
                        LMTextureManager.INSTANCE
                                .getTexture("Default")
                                .orElseThrow(
                                        () -> new IllegalStateException("デフォルトテクスチャが存在しません。")));
        soundPlayer =
                new SoundPlayableCompound(
                        this,
                        () -> multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        addDefaultModes(this);
        initIdFactor();
        setRandomTexture();
        setRandomVoice();
        this.targetTagManager = new TargetTagManagerImpl(worldIn);
    }

    // 基本使わない
    public LittleMaidEntity(World world) {
        this(Registration.LITTLE_MAID_MOB.get(), world);
    }

    // スタティックなメソッド

    // todo メイドさんに付与する属性の再考
    public static DefaultAttributeContainer.Builder createLittleMaidAttributes() {
        DefaultAttributeContainer.Builder builder =
                createMobAttributes()
                        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                        .add(EntityAttributes.GENERIC_ATTACK_SPEED)
                        .add(EntityAttributes.GENERIC_LUCK)
                        .add(
                                EntityAttributes.GENERIC_FOLLOW_RANGE,
                                LMMRMod.getConfig().target.followRange);
        ReachAttributeUtil.addAttribute(builder);
        return builder;
    }

    public static boolean isValidNaturalSpawn(WorldAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).isFullCube(world, pos)
                && world.getBaseLightLevel(pos, 0) > LMMRMod.getConfig().spawn.spawnMinLightLevel;
    }

    // 登録メソッドたち

    @Override
    protected void initGoals() {
        LMGoalInitializer.initGoals(this);
    }

    GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    GoalSelector getTargetSelector() {
        return this.targetSelector;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        // 捡取开关（PICKUP_ITEM_INDEX）默认开启
        this.dataTracker.startTracking(LMM_FLAGS, (byte) (1 << PICKUP_ITEM_INDEX));
        this.dataTracker.startTracking(MOVING_MODE, (byte) 0);
        this.dataTracker.startTracking(MODE_NAME, "");
        this.dataTracker.startTracking(CHARGING, false);
        this.dataTracker.startTracking(ACCELERATE, false);
        this.dataTracker.startTracking(MASTER_STANCE, (byte) 0);
        this.dataTracker.startTracking(MOOD, 70);
        this.dataTracker.startTracking(FAVORABILITY, 0);
        this.dataTracker.startTracking(SPEECH, "");
        this.dataTracker.startTracking(SPEECH_TIMER, 0);
        this.dataTracker.startTracking(HUNGER, 100);
        this.dataTracker.startTracking(EATING_STACK, ItemStack.EMPTY);
        this.dataTracker.startTracking(REBELLING, false);
        this.dataTracker.startTracking(ANGER_TICKS, 0);
        this.dataTracker.startTracking(AUTO_EAT_THRESHOLD, 60);
        this.dataTracker.startTracking(REST_SIT_PROGRESS, 0.0F);
        this.dataTracker.startTracking(SUGAR_CONSUMING, false);
        this.dataTracker.startTracking(FORCE_CHUNK_LOAD, false);
    }

    public void addDefaultModes(LittleMaidEntity maid) {
        this.hasModeImpl.addAllMode(ModeManager.INSTANCE.createModes(maid));
    }

    // 読み書き系

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putByte("maidVersion", (byte) 2);

        writeInventory(nbt);
        // 背包扩容道具槽位
        if (!backpackUpgradeSlot.getStack(0).isEmpty()) {
            nbt.put("BackpackUpgrade", backpackUpgradeSlot.getStack(0).writeNbt(new NbtCompound()));
        }
        nbt.putInt("XpTotal", this.experiencePoints);
        if (TameableUtil.getTameOwnerUuid(this).isPresent()) {
            nbt.putBoolean("Wait", TameableUtil.isWait(this));
            nbt.putByte("MovingMode", (byte) this.getMovingMode().getId());
            writeContractable(nbt);
            writeModeData(nbt);
            nbt.putBoolean("isBloodSuck", isBloodSuck());
            nbt.putBoolean("isPickupItem", isPickupItem());
            nbt.putBoolean("ForceChunkLoad", isForceChunkLoad());
            if (this.getMovingMode() == MovingMode.FREEDOM && freedomPos != null) {
                nbt.put("FreedomPos", NbtHelper.fromBlockPos(freedomPos));
            }
            if (boundWorkCenter != null && boundWorkDimension != null) {
                nbt.put("BoundWorkCenter", NbtHelper.fromBlockPos(boundWorkCenter));
                nbt.putString("BoundWorkDimension", boundWorkDimension.toString());
            }
            writeTargetTags(nbt);
        }
        this.multiModel.writeToNbt(nbt);
        nbt.putString("SoundConfigName", getConfigHolder().getName());

        nbt.putInt("accelerationTicks", accelerationTicks);
        nbt.putBoolean("SouvenirReviveRest", souvenirReviveRest);
        maidMood.writeNbt(nbt);
        nbt.putInt("Hunger", getHungerValue());
        nbt.putInt("AutoEatThreshold", getAutoEatThreshold());
        if (animalMaidNbt != null) {
            nbt.put("AnimalMaidNbt", animalMaidNbt);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        int maidVersion = nbt.getByte("maidVersion") & 255;

        if (maidVersion <= 1) {
            var defaultAttributes = createLittleMaidAttributes().build();
            var entityAttributes =
                    new EntityAttribute[] {
                        EntityAttributes.GENERIC_MOVEMENT_SPEED,
                        EntityAttributes.GENERIC_FOLLOW_RANGE
                    };
            for (var attribute : entityAttributes) {
                var customInstance = this.getAttributes().getCustomInstance(attribute);
                if (customInstance != null) {
                    customInstance.setBaseValue(defaultAttributes.getBaseValue(attribute));
                }
            }
        }

        readInventory(nbt);
        // 背包扩容道具槽位
        backpackUpgradeSlot.clear();
        if (nbt.contains("BackpackUpgrade")) {
            ItemStack upgradeStack = ItemStack.fromNbt(nbt.getCompound("BackpackUpgrade"));
            if (!upgradeStack.isEmpty() && BackpackUpgradeItem.isUpgrade(upgradeStack)) {
                backpackUpgradeSlot.setStack(0, upgradeStack);
            }
        }
        this.experiencePoints = nbt.getInt("XpTotal");
        if (maidVersion == 0) {
            var list = nbt.getList("Inventory", 10);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound nbtCompound = list.getCompound(i);
                int j = nbtCompound.getByte("Slot") & 255;
                ItemStack stack = ItemStack.fromNbt(nbtCompound);
                if (!stack.isEmpty()) {
                    if (j == 0) {
                        this.equipStack(EquipmentSlot.MAINHAND, stack);
                    } else if (100 <= j && j < 104) {
                        this.equipStack(
                                EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, j - 100),
                                stack);
                    } else if (j == 150) {
                        this.equipStack(EquipmentSlot.OFFHAND, stack);
                    }
                }
            }
        }

        if (TameableUtil.hasTameOwner(this)) {
            TameableUtil.setWait(this, nbt.getBoolean("Wait"));
            setMovingMode(MovingMode.fromId(nbt.getByte("MovingMode")));
            readContractable(nbt);
            readModeData(nbt);
            setBloodSuck(nbt.getBoolean("isBloodSuck"));
            // 兼容旧存档：无该字段时默认开启捡取
            setPickupItem(!nbt.contains("isPickupItem") || nbt.getBoolean("isPickupItem"));
            // 强加载开关（旧档默认关闭）
            this.dataTracker.set(FORCE_CHUNK_LOAD, nbt.getBoolean("ForceChunkLoad"));
            if (this.getMovingMode() == MovingMode.FREEDOM && nbt.contains("FreedomPos")) {
                freedomPos = NbtHelper.toBlockPos(nbt.getCompound("FreedomPos"));
            }
            if (nbt.contains("BoundWorkCenter")) {
                boundWorkCenter = NbtHelper.toBlockPos(nbt.getCompound("BoundWorkCenter"));
                boundWorkDimension = Identifier.tryParse(nbt.getString("BoundWorkDimension"));
            }
            readTargetTags(nbt);
        }
        this.multiModel.readFromNbt(nbt);
        this.calculateDimensions();
        if (nbt.contains("SoundConfigName")) {
            LMConfigManager.INSTANCE
                    .getConfig(nbt.getString("SoundConfigName"))
                    .ifPresent(this::setConfigHolder);
        }

        accelerationTicks = nbt.getInt("accelerationTicks");
        // 纪念品复活后的休息状态（旧档无该字段默认 false）
        souvenirReviveRest = nbt.getBoolean("SouvenirReviveRest");
        maidMood.readNbt(nbt);
        if (nbt.contains("Hunger")) {
            setHunger(nbt.getInt("Hunger"));
        }
        if (nbt.contains("AutoEatThreshold")) {
            setAutoEatThreshold(nbt.getInt("AutoEatThreshold"));
        }
        if (nbt.contains("AnimalMaidNbt")) {
            animalMaidNbt = nbt.getCompound("AnimalMaidNbt");
        }
        syncMood();
    }

    /** 心情値・好感度をクライアントへ同期する。 */
    public void syncMood() {
        this.dataTracker.set(MOOD, maidMood.getMood());
        this.dataTracker.set(FAVORABILITY, maidMood.getFavorability());
        updateFavorabilityHealth();
    }

    /** 好感度等级决定固定血量（好感数值仅用于升级）。等级变化时才更新最大生命值。 */
    private void updateFavorabilityHealth() {
        if (this.getWorld().isClient) {
            return;
        }
        var instance = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instance == null) {
            return;
        }
        int level = maidMood.getFavorabilityLevel();
        if (level == lastFavorHealthLevel) {
            return;
        }
        double targetMax = MaidMood.getMaxHealthForLevel(level);
        double delta = targetMax - instance.getBaseValue();
        instance.setBaseValue(targetMax);
        // 仅游戏内升级时补血（lastFavorHealthLevel 已初始化 >0）；重进世界加载 NBT 的首次
        // 调用（lastFavorHealthLevel 为初始 -1）只对齐最大生命值、不 heal，保留存档血量——
        // 否则 Lv2+ 女仆每次重进世界都会被 heal(delta) 直接回满血
        boolean wasInitialized = lastFavorHealthLevel > 0;
        lastFavorHealthLevel = level;
        if (delta > 0 && wasInitialized) {
            this.heal((float) delta);
        }
    }

    /** テキストバブル（セリフ）を表示する。 */
    public void setSpeech(String key) {
        if (this.getWorld().isClient) {
            return;
        }
        // 野生女仆（未驯服/未归属于玩家）不播放任何气泡文本
        if (!TameableUtil.hasTameOwner(this)) {
            return;
        }
        // 统一气泡间隔：上一个气泡仍在显示、或处于公共冷却中时，不触发新气泡。
        // 因此无论玩家主动触发（受击/喂食/死亡…）还是女仆自动触发（低血/饥饿/天气…），
        // 气泡的出现频率都受同一间隔约束，避免插队/刷屏。
        if (this.getSpeechTimer() > 0 || this.speechAutoCooldown > 0) {
            return;
        }
        // 拴绳状态下：忽略原本的台词，只播拴绳专用文本（按当前情绪；反叛除外），
        // 且每播放一次扣 2 点心情、2 点好感度。
        if (this.isLeashed() && this.getEmotion() != MaidMood.Emotion.REBELLION) {
            MaidSpeech.onLeashed(this);
            return;
        }
        this.dataTracker.set(SPEECH, key);
        this.dataTracker.set(SPEECH_TIMER, LMMRMod.getConfig().speech.speechDuration);
        // 任意气泡触发后进入公共冷却，保证下一次气泡间隔一致
        this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
    }

    /** 拴绳专用气泡：按当前情绪选文本由 MaidSpeech 调用；每播放一次扣 2 心情 + 2 好感。 */
    public void setLeashedSpeech(String key) {
        if (this.getWorld().isClient) {
            return;
        }
        if (!TameableUtil.hasTameOwner(this)) {
            return;
        }
        // 拴绳状态气泡惩罚：每播一次按配置扣心情、好感（下限钳制 0）；
        // 好感度等级满级（Lv5）时拴绳不再扣好感度，心情照常扣。
        maidMood.addMood(-LMMRMod.getConfig().mood.leashSpeechMoodDrop);
        if (maidMood.getFavorabilityLevel() < 5) {
            maidMood.addFavorability(-LMMRMod.getConfig().mood.leashSpeechFavorabilityDrop);
        }
        syncMood();
        this.dataTracker.set(SPEECH, key);
        this.dataTracker.set(SPEECH_TIMER, LMMRMod.getConfig().speech.speechDuration);
        this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
    }

    public Optional<String> getSpeech() {
        String s = this.dataTracker.get(SPEECH);
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    public int getSpeechTimer() {
        return this.dataTracker.get(SPEECH_TIMER);
    }

    private void tickSpeech() {
        int timer = this.dataTracker.get(SPEECH_TIMER);
        if (timer > 0) {
            timer--;
            this.dataTracker.set(SPEECH_TIMER, timer);
            if (timer == 0) {
                this.dataTracker.set(SPEECH, "");
            }
        }
    }

    private void tickSpeechEvents() {
        var config = LMMRMod.getConfig().speech;
        var world = this.getWorld();
        // 冷却中或上一个气泡尚未播放完毕时，不触发新的主动台词
        if (this.speechAutoCooldown > 0 || this.getSpeechTimer() > 0) {
            return;
        }
        // 同一 tick 只触发一条（按优先级），避免多条台词互相覆盖、气泡刷新过快
        // 低HP
        if (this.getHealth() / this.getMaxHealth() * 100 <= config.lowHealthThresholdPercent
                && this.age % 200 == 0) {
            MaidSpeech.onLowHealth(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
            return;
        }
        // 罢工
        if (this.isStrike() && this.age % 400 == 0) {
            MaidSpeech.onStrike(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
            return;
        }
        // 空腹
        if (this.getHungerValue() < LMMRMod.getConfig().hunger.hungerEatThreshold
                && this.age % 400 == 0) {
            MaidSpeech.onHungry(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
            return;
        }
        // 仕事中
        if (this.getMode().isPresent() && this.age % 600 == 0) {
            MaidSpeech.onWork(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
            return;
        }
        // 夜
        if (world.isNight() && this.age % 2400 == 0) {
            MaidSpeech.onNight(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
            return;
        }
        // 朝（昼の始まり頃にまれに）
        if (world.isDay() && this.age % 8000 == 0 && this.random.nextInt(3) == 0) {
            MaidSpeech.onMorning(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
            return;
        }
        // 雨
        if (world.isRaining() && !world.isThundering() && this.age % 2400 == 0) {
            MaidSpeech.onRain(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
            return;
        }
        // 雪
        if (world.isRaining()
                && this.age % 2400 == 0
                && world
                                .getBiome(this.getBlockPos())
                                .value()
                                .getPrecipitation(this.getBlockPos())
                        == net.minecraft.world.biome.Biome.Precipitation.SNOW) {
            MaidSpeech.onSnow(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
            return;
        }
        // 待機・ランダム
        if (this.age % config.speechInterval == 0 && this.random.nextInt(4) == 0) {
            MaidSpeech.onIdle(this);
            this.speechAutoCooldown = AUTO_SPEECH_COOLDOWN;
        }
    }

    // ---- 空腹値（ハンガー）システム ----

    /** クライアント表示用：現在の空腹値 (0〜100、大きいほど満腹) */
    public int getHungerValue() {
        return this.dataTracker.get(HUNGER);
    }

    public void setHunger(int hunger) {
        this.dataTracker.set(HUNGER, Math.max(0, Math.min(100, hunger)));
    }

    /** 是否正在"持糖消耗"（副手短暂展示糖后恢复饱食度，客户端据此抬起手臂）。 */
    public boolean isSugarConsuming() {
        return this.dataTracker.get(SUGAR_CONSUMING);
    }

    private void tickHunger() {
        var config = LMMRMod.getConfig().hunger;
        // 空腹値の減少（設定間隔毎に1）
        if (hungerTickCounter <= 0) {
            hungerTickCounter = config.hungerDecayInterval;
            setHunger(getHungerValue() - 1);
        } else {
            hungerTickCounter--;
        }
        // 饥饿状态效果：>80% 且生命未满时持续恢复生命（每40tick回1血耗2%饥饿，类似生命恢复1），<20% 缓慢
        int hungerNow = getHungerValue();
        if (this.age % 40 == 0 && !this.getWorld().isClient) {
            int recoveryThreshold = config.hungerRecoveryThreshold; // 默认80
            if (hungerNow > recoveryThreshold && this.getHealth() < this.getMaxHealth()) {
                setHunger(hungerNow - 2); // 每 40tick 消耗 2% 饥饿
                this.heal(1); // 每 40tick 恢复 1 点生命
            } else if (hungerNow < 20) {
                this.addStatusEffect(
                        new StatusEffectInstance(
                                StatusEffects.SLOWNESS, 60, 0, false, false, true));
            }
        }
        // 自動食事チェック：-1=无时无刻进食（按 hungerAlwaysEatInterval 无条件吃），否则按阈值空腹/低HP吃
        if (hungerEatCounter <= 0) {
            int threshold = getAutoEatThreshold();
            boolean alwaysEat = threshold == -1;
            hungerEatCounter = alwaysEat ? config.hungerAlwaysEatInterval : config.hungerEatInterval;
            boolean isHungry = alwaysEat || (threshold > 0 && hungerNow < threshold);
            boolean lowHealth =
                    !alwaysEat
                            && threshold > 0
                            && config.hungerEatWhenLowHealth
                            && this.getHealth() / this.getMaxHealth()
                                    < config.hungerLowHealthThreshold;
            if (isHungry || lowHealth) {
                eatFoodFromInventory();
            }
        } else {
            hungerEatCounter--;
        }
        // 空腹値が0なら飢餓ダメージ（設定間隔毎）
        if (getHungerValue() <= 0) {
            if (hungerStarveCounter <= 0) {
                hungerStarveCounter = config.hungerStarveInterval;
                if (!this.getWorld().isClient) {
                    this.damage(this.getWorld().getDamageSources().starve(), config.hungerStarveDamage);
                }
            } else {
                hungerStarveCounter--;
            }
        } else {
            hungerStarveCounter = 0;
        }
    }

    /** インベントリの食べ物を自動で食べ、空腹回復・回血加速・好感度上昇・食べるアニメーションを行う。 */
    private void eatFoodFromInventory() {
        // 正在吃食物或正在持糖消耗时，不再开吃下一个
        if (this.eatingTicks > 0 || this.sugarConsumeTicks > 0) {
            return;
        }
        Inventory inv = this.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isFood()) {
                continue;
            }
            var food = stack.getItem().getFoodComponent();
            if (food == null) {
                continue;
            }
            // 空腹値回復をアニメーション終了まで遅延させる（バニラ満腹度×4 を 0-100 スケールに）
            this.pendingHungerRestore = Math.max(1, food.getHunger() * 4);
            // 食べるアニメーション（食べ物を手に持って食べるポーズ+パーティクル）
            ItemStack foodStack = stack.copy();
            foodStack.setCount(1);
            // 消費
            stack.decrement(1);
            if (stack.isEmpty()) {
                inv.setStack(i, ItemStack.EMPTY);
            }
            startEatingAnimation(foodStack);
            return;
        }
        // 没有食物：若有糖则消耗 1 颗糖恢复固定 4% 饱食度（无咀嚼动画，但会短暂手持糖并播放消耗粒子）
        if (getHungerValue() < 100) {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (stack.isIn(LMTags.Items.MAIDS_SALARY)) {
                    ItemStack sugarStack = stack.copy();
                    sugarStack.setCount(1);
                    stack.decrement(1);
                    if (stack.isEmpty()) {
                        inv.setStack(i, ItemStack.EMPTY);
                    }
                    // 副手短暂手持糖展示（消耗动画），结束时恢复 4% 饱食度
                    startSugarConsume(sugarStack, true);
                    // 复用现有"消耗糖"状态动画（音符粒子）
                    this.getWorld().sendEntityStatus(this, (byte) 72);
                    break;
                }
            }
        }
    }

    /** 开始"持糖消耗"：把糖放进副手短暂展示（无咀嚼动画），配合挥动+音符粒子。 */
    public void startSugarConsume(ItemStack sugar, boolean restoreHungerOnEnd) {
        if (this.getWorld().isClient) {
            return;
        }
        if (this.sugarConsumeTicks > 0 || this.eatingTicks > 0) {
            return;
        }
        // 若正在使用物品（如持盾格挡），先停止使用，避免占用副手/使用状态
        if (this.isUsingItem()) {
            this.stopUsingItem();
        }
        this.sugarRestoreHungerOnEnd = restoreHungerOnEnd;
        this.sugarStoredOffHand = this.getOffHandStack();
        this.setStackInHand(Hand.OFF_HAND, sugar);
        int consumeDuration = LMMRMod.getConfig().client.sugarConsumeDuration;
        this.sugarConsumeTicks = consumeDuration > 0 ? consumeDuration : SUGAR_CONSUME_DURATION;
        this.dataTracker.set(SUGAR_CONSUMING, true);
        this.swingHand(Hand.OFF_HAND);
    }

    /** 持糖消耗逐 tick：糖在副手展示结束后还原副手，并按需恢复 4% 饱食度。 */
    private void tickSugarConsume() {
        if (this.getWorld().isClient) {
            return;
        }
        if (this.sugarConsumeTicks <= 0) {
            return;
        }
        this.sugarConsumeTicks--;
        if (this.sugarConsumeTicks > 0) {
            return;
        }
        // 消耗完成：自动吃糖路径在此时按配置恢复饱食度
        if (this.sugarRestoreHungerOnEnd) {
            setHunger(getHungerValue() + LMMRMod.getConfig().hunger.sugarSatietyRestore);
        }
        this.sugarRestoreHungerOnEnd = false;
        // 若进食动画未占用副手，则还原原副手物品（通常是盾牌）
        if (this.eatingTicks <= 0) {
            this.setStackInHand(Hand.OFF_HAND, this.sugarStoredOffHand);
        }
        this.sugarStoredOffHand = ItemStack.EMPTY;
        this.dataTracker.set(SUGAR_CONSUMING, false);
    }

    /** 進食開始：食物放进副手并抬起到嘴边，使用原版進食状态（32tick、减速、咀嚼声/粒子），主手物品保持不变。 */
    private void startEatingAnimation(ItemStack foodStack) {
        if (this.getWorld().isClient) {
            return;
        }
        // 若正在"持糖消耗"中，先还原副手并结束该状态，避免与进食动画抢副手
        if (this.sugarConsumeTicks > 0) {
            if (this.eatingTicks <= 0) {
                this.setStackInHand(Hand.OFF_HAND, this.sugarStoredOffHand);
            }
            this.sugarStoredOffHand = ItemStack.EMPTY;
            this.sugarConsumeTicks = 0;
            this.sugarRestoreHungerOnEnd = false;
            this.dataTracker.set(SUGAR_CONSUMING, false);
        }
        this.eatingPlayerFed = false;
        // 若正在使用物品（如持盾格挡），先停止使用，确保 setCurrentHand 能把 activeItem 切到食物、
        // 进食动画完整播放（否则"已在用盾"会让 setCurrentHand 空操作，随后被持盾逻辑打断）
        if (this.isUsingItem()) {
            this.stopUsingItem();
        }
        // 已在进食中则不重复暂存副手（防止把上一次的临时食物误存为原物品）
        if (this.eatingTicks <= 0) {
            this.eatingStoredOffHand = this.getOffHandStack();
        }
        // 副手暂存+临时放食物，主手不动
        this.setStackInHand(Hand.OFF_HAND, foodStack);
        // 原版进食状态：咀嚼声/粒子/减速 + 手臂动画（由 caps 驱动）速度与玩家一致
        this.setCurrentHand(Hand.OFF_HAND);
        this.eatingTicks = 32;
        this.dataTracker.set(EATING_STACK, foodStack);
    }

    /** 玩家喂食/外部触发进食的外部门面。 */
    public void startEatingItem(ItemStack food) {
        if (this.getWorld().isClient) {
            return;
        }
        startEatingAnimation(food);
        // 玩家喂食路径：台词已在喂食瞬间播放（onFed），动画结束不再播自动进食台词
        this.eatingPlayerFed = true;
    }

    /** 進食経過処理：咀嚼粒子 + 原版吃完（isUsingItem 结束）或计时结束后恢复副手。 */
    private void tickEatingAnimation() {
        if (this.eatingTicks <= 0) {
            return;
        }
        // 咀嚼粒子：每4tick在嘴边冒食物碎屑（原版玩家进食同节奏）
        if (this.eatingTicks % 4 == 0
                && this.getWorld() instanceof ServerWorld serverWorld
                && !this.getEatingStack().isEmpty()) {
            serverWorld.spawnParticles(
                    new ItemStackParticleEffect(ParticleTypes.ITEM, this.getEatingStack()),
                    this.getX(),
                    this.getEyeY() - 0.2,
                    this.getZ(),
                    5,
                    0.1,
                    0.1,
                    0.1,
                    0.05);
        }
        this.eatingTicks--;
        // 原版已吃完该物品（不同食物使用时长不同）时提前结束
        if (this.eatingTicks == 0 || !this.isUsingItem()) {
            // 吃完时应用食物的药水效果（金苹果/腐肉/河豚等，概率同原版）
            applyFoodEffects(this.getEatingStack());
            // 进食动画完成才恢复饥饿值、回血、提升好感/心情
            if (!this.getWorld().isClient) {
                var config = LMMRMod.getConfig().hunger;
                if (this.pendingHungerRestore > 0) {
                    setHunger(getHungerValue() + this.pendingHungerRestore);
                    this.pendingHungerRestore = 0;
                }
                // 回血加速
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(config.hungerHealBoost);
                }
                // 好感度・心情値上昇（設定値を使用）
                maidMood.addFavorability(config.hungerFavorabilityGainOnEat);
                maidMood.addMood(config.hungerMoodGainOnEat);
                syncMood();
                // 台词按进食来源区分：自动进食播独立台词；玩家喂食的 onFed 台词已在
                // 喂食瞬间播放，动画结束不覆盖（此前会被 onSelfEat 顶掉）
                if (!this.eatingPlayerFed) {
                    MaidSpeech.onSelfEat(this);
                }
                this.eatingPlayerFed = false;
            }
            this.eatingTicks = 0;
            this.clearActiveItem();
            this.setStackInHand(Hand.OFF_HAND, this.eatingStoredOffHand);
            this.eatingStoredOffHand = ItemStack.EMPTY;
            this.dataTracker.set(EATING_STACK, ItemStack.EMPTY);
        }
    }

    /** 应用食物附带的药水效果（中毒/凋零/饥饿/夜视/金苹果吸收等，概率与原版一致）。 */
    private void applyFoodEffects(ItemStack food) {
        if (food.isEmpty()) {
            return;
        }
        var foodComponent = food.getItem().getFoodComponent();
        if (foodComponent == null) {
            return;
        }
        for (var pair : foodComponent.getStatusEffects()) {
            if (this.random.nextFloat() < pair.getSecond()) {
                this.addStatusEffect(new StatusEffectInstance(pair.getFirst()));
            }
        }
    }

    /** クライアント表示用：現在の心情値 (0〜100) */
    public int getMoodValue() {
        return this.dataTracker.get(MOOD);
    }

    /** クライアント表示用：現在の好感度 (0〜100) */
    public int getFavorabilityValue() {
        return this.dataTracker.get(FAVORABILITY);
    }

    /** 客户端安全的好感等级：由 DataTracker 同步的好感数值换算（maidMood 对象仅服务端更新）。 */
    public int getFavorabilityLevelValue() {
        return MaidMood.levelOf(this.dataTracker.get(FAVORABILITY));
    }

    /** 正在嘴里吃的食物（客户端嘴部渲染用，空表示未在进食）。 */
    public ItemStack getEatingStack() {
        return this.dataTracker.get(EATING_STACK);
    }

    /** 是否正在进食（用于隐藏副手敌方重复渲染，由嘴边食物替代）。 */
    public boolean isEating() {
        return !this.dataTracker.get(EATING_STACK).isEmpty();
    }

    /** 当前情绪（纯心情值驱动，与行为强相关；反叛为独立最高优先状态）。 */
    public MaidMood.Emotion getEmotion() {
        if (this.dataTracker.get(REBELLING)) {
            return MaidMood.Emotion.REBELLION;
        }
        return MaidMood.getEmotionByMood(this.getMoodValue());
    }

    // todo IdFactorが確実にセットされたタイミングで実行されるようにする
    public void setRandomTexture() {
        var textureHolderList =
                LMTextureManager.INSTANCE.getAllTextures().stream()
                        .filter(h -> h.hasSkinTexture(false)) // 野生テクスチャがある
                        .filter(h -> LMModelManager.INSTANCE.hasModel(h.getModelName()))
                        .toList();
        if (textureHolderList.isEmpty()) {
            return;
        }
        var textureHolder = textureHolderList.get(idFactor % textureHolderList.size());
        var colorList =
                Arrays.stream(TextureColors.values())
                        .filter(c -> textureHolder.getTexture(c, false, false).isPresent())
                        .toList();
        if (colorList.isEmpty()) {
            return;
        }
        var color = colorList.get(idFactor % colorList.size());
        this.setColorMM(color);
        this.setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD);
        if (textureHolder.hasArmorTexture()) {
            setTextureHolder(textureHolder, Layer.INNER, Part.HEAD);
            setTextureHolder(textureHolder, Layer.INNER, Part.BODY);
            setTextureHolder(textureHolder, Layer.INNER, Part.LEGS);
            setTextureHolder(textureHolder, Layer.INNER, Part.FEET);
            setTextureHolder(textureHolder, Layer.OUTER, Part.HEAD);
            setTextureHolder(textureHolder, Layer.OUTER, Part.BODY);
            setTextureHolder(textureHolder, Layer.OUTER, Part.LEGS);
            setTextureHolder(textureHolder, Layer.OUTER, Part.FEET);
        }
    }

    public void setRandomVoice() {
        if (getConfig().spawn.silentDefaultVoice) {
            soundPlayer.setConfigHolder(LMConfigManager.EMPTY_CONFIG);
        } else {
            List<ConfigHolder> configs = LMConfigManager.INSTANCE.getAllConfig();
            soundPlayer.setConfigHolder(configs.get(idFactor % configs.size()));
        }
        String defaultSoundPackName = getConfig().spawn.defaultSoundPackName;
        if (!defaultSoundPackName.isEmpty()) {
            LMConfigManager.INSTANCE.getAllConfig().stream()
                    .filter(c -> c.getPackName().equalsIgnoreCase(defaultSoundPackName))
                    .findAny()
                    .ifPresent(soundPlayer::setConfigHolder);
        }
    }

    // 鯖
    @Override
    public void saveAdditionalSpawnData(PacketByteBuf buf) {
        // モデル
        buf.writeEnumConstant(getColorMM());
        buf.writeBoolean(isContractMM());
        buf.writeString(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            buf.writeString(getTextureHolder(Layer.INNER, part).getTextureName());
            buf.writeString(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
        // サウンド
        buf.writeString(getConfigHolder().getName());
        // 頭の装飾品が表示されない対策
        // 原因はインベントリを開くまで同期されないため
        buf.writeItemStack(getInventory().getStack(17));
        // architectury側のミスでPitchYawが逆に与えられているのを修正
        buf.writeFloat(this.getPitch());
        buf.writeFloat(this.getYaw());
        buf.writeVarInt(this.accelerationTicks);
    }

    // 蔵
    @Override
    public void loadAdditionalSpawnData(PacketByteBuf buf) {
        // モデル
        // readString()はクラ処理。このメソッドでは、クラ側なので問題なし
        setColorMM(buf.readEnumConstant(TextureColors.class));
        setContractMM(buf.readBoolean());
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager
                .getTexture(buf.readString())
                .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            textureManager
                    .getTexture(buf.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            textureManager
                    .getTexture(buf.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
        }
        // サウンド
        LMConfigManager.INSTANCE.getConfig(buf.readString()).ifPresent(this::setConfigHolder);

        getInventory().setStack(17, buf.readItemStack());
        this.setPitch(buf.readFloat());
        this.setYaw(buf.readFloat());
        this.accelerationTicks = buf.readVarInt();
    }

    @Override
    public void handleStatus(byte status) {
        switch (status) {
            case 70 -> { // 雇用時
                showEmoteParticle(true);
                play(LMSounds.GET_CAKE);
            }
            case 71 -> { // 再雇用時
                showEmoteParticle(true);
                play(LMSounds.RECONTRACT);
            }
            case 72 -> { // 砂糖あげた時
                this.getWorld()
                        .addParticle(
                                ParticleTypes.NOTE,
                                this.getX(),
                                this.getY() + this.getHeight(),
                                this.getZ(),
                                6 / 24f,
                                0,
                                0);
            }
            case 73 -> showFreedomParticle(); // toFreedom
            case 74 -> showEmoteParticle(false); // toEscort
            case 75 -> showTracerParticle(); // toTracer
            default -> super.handleStatus(status);
        }
    }

    protected void showFreedomParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.getWorld()
                    .addParticle(
                            new DustParticleEffect(
                                    new Vector3f(
                                            this.random.nextFloat(),
                                            this.random.nextFloat(),
                                            this.random.nextFloat()),
                                    1.0f),
                            this.getParticleX(1.0),
                            this.getRandomBodyY() + 0.5,
                            this.getParticleZ(1.0),
                            d,
                            e,
                            f);
        }
    }

    protected void showTracerParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.getWorld()
                    .addParticle(
                            ParticleTypes.CLOUD,
                            this.getParticleX(1.0),
                            this.getRandomBodyY() + 0.5,
                            this.getParticleZ(1.0),
                            d,
                            e,
                            f);
        }
    }

    // バニラメソッズ

    @Override
    public void tick() {
        if (!this.getWorld().isClient() && !this.maidManagerRegistered) {
            TameableUtil.getTameOwner(this)
                    .filter(owner -> owner instanceof MaidManager)
                    .ifPresent(
                            owner -> {
                                ((MaidManager) owner).registerMaid(this);
                                this.maidManagerRegistered = true;
                            });
        }
        int tickMultiple = getTickMultiple();
        for (int i = 0; i < tickMultiple; i++) {
            inTickMultiplePre();
            super.tick();
            inTickMultiplePost();
        }
    }

    protected void inTickMultiplePre() {
        if (!this.getWorld().isClient) {
            // 背包扩容格数随扩容道具等级变化
            littleMaidInventory.setExtraSlotCount(getBackpackExtraSlots());
            if (this.fleeingTicks > 0) {
                this.fleeingTicks--;
            }
            maidMood.tick();
            // 同步反叛/怒气状态给客户端（情绪面板展示）
            this.dataTracker.set(REBELLING, this.isRebellious());
              this.dataTracker.set(ANGER_TICKS, this.maidMood.getAngerTicks());
              if (this.speechAutoCooldown > 0) {
                  this.speechAutoCooldown--;
              }
              tickSpeech();
              tickSpeechEvents();
            tickHunger();
            tickEatingAnimation();
            tickSugarConsume();
            if (this.shieldBlockCooldown > 0) {
                this.shieldBlockCooldown--;
            }
            // 破盾禁用计时：结束后盾牌恢复可举
              if (this.shieldDisableTicks > 0) {
                  this.shieldDisableTicks--;
              }
              // 休息状态：低血量时周期性坐下恢复，血量恢复到自身 50% 以上解除
              updateEvadeState();
              updateRestState();
              tickRestSitAnimation();
              // 强加载：女仆所在区块被强制加载，移动时自动迁移；关闭时解除
              updateForceChunk();
            // 反叛时循环播放怒气粒子：特效持续 20 tick，消失后再停 0.5s（10 tick）播下一轮
            if (this.isRebellious()) {
                if (this.rebellionParticleCooldown > 0) {
                    this.rebellionParticleCooldown--;
                } else {
                    spawnRebellionAngerParticles();
                    this.rebellionParticleCooldown =
                            REBELLION_PARTICLE_DURATION + REBELLION_PARTICLE_GAP;
                }
            } else if (this.rebellionParticleCooldown != 0) {
                // 反叛结束（恢复非反叛）：立即清零，不再播放下一次怒气粒子
                this.rebellionParticleCooldown = 0;
            }
            // 车万女仆同款持盾：持盾姿态时举起副手盾牌（原版 usingItem 状态驱动持盾动画），
            // 姿态解除时放下；进食中 activeItem 是食物，不会被误停
            if (this.shouldRaiseShield()) {
                if (!this.isUsingItem()) {
                    this.setCurrentHand(Hand.OFF_HAND);
                }
            } else if (this.isUsingItem()
                    && this.getActiveItem().getItem() instanceof ShieldItem
                    && !this.isEating()) {
                this.stopUsingItem();
            }
        }
        if (this.experiencePickUpDelay > 0) {
            --this.experiencePickUpDelay;
        }
        if (this.getWorld().isClient) {
            tickInterestedAngle();
        }
        playSoundCool = Math.max(0, playSoundCool - 1);
        decAccelerationTicks();
    }

    protected void inTickMultiplePost() {}

    @Override
    public void tickMovement() {
        tickHandSwing();
        super.tickMovement();
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (TameableUtil.hasTameOwner(this) || getConfig().misc.canPickupItemByNoOwner) {
            pickupItem();
        }
        itemContractable.tick();
        hasModeImpl.tick();
    }

    protected void pickupItem() {
        if (this.getHealth() <= 0 || this.isSpectator()) {
            return;
        }
        // 捡取开关关闭时，不吸附掉落物也不吸经验球（主动走去捡取由 Goal 自行判断开关）
        if (!this.isPickupItem()) {
            return;
        }
        // 吸附范围：掉落物被动吸附半径（可配置，为属性加成预留）、经验球 1.5 格
        float passiveRange = getConfig().misc.passivePickupRange;
        var itemAabb = this.getBoundingBox().expand(passiveRange, 1.0, passiveRange);
        var aroundItems = this.getWorld().getOtherEntities(this, itemAabb);
        List<ExperienceOrbEntity> exps = Lists.newArrayList();
        List<ItemEntity> items = Lists.newArrayList();
        for (Entity entity : aroundItems) {
            if (entity.isRemoved()) {
                continue;
            }
            if (entity instanceof ExperienceOrbEntity exp) {
                if (getConfig().misc.canPickupExperienceOrb) {
                    exps.add(exp);
                }
            } else if (entity instanceof ItemEntity item) {
                // 被动吸附掉落物（类似玩家）：仅当靠近时吸进背包
                if (getConfig().misc.canPickupItem) {
                    items.add(item);
                }
            }
        }
        // 经验球
        if (!exps.isEmpty()) {
            var collidable = ((LMCollidable) Util.getRandom(exps, this.random));
            if (collidable != null) {
                collidable.onCollision_LMMR(this);
            }
        }
        // 掉落物：吸附范围内最近的若干（逐个尝试进背包，避免一次全吸导致卡顿）
        if (!items.isEmpty()) {
            double pickupSq = passiveRange * passiveRange;
            for (Entity item : items) {
                if (item.isRemoved()) {
                    continue;
                }
                if (item.squaredDistanceTo(this) <= pickupSq) {
                    ((LMCollidable) item).onCollision_LMMR(this);
                }
            }
        }
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return getConfig().spawn.canDespawn && TameableUtil.getTameOwnerUuid(this).isEmpty();
    }

    // canSpawnとかでも使われる
    // todo スポーン条件をコンフィグで設定可能にする
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getBlockState(pos.down()).isFullCube(world, pos)
                ? 10.0F
                : world.getPhototaxisFavor(pos);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        // 避战期间不锁定任何目标（不攻击、不接近敌人）
        if (this.isEvading()) {
            return false;
        }
        return super.canTarget(target)
                && !TameableUtil.isFriend(this, target)
                && !getTargetTag(new TargetIdentifier(target))
                        .contains(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    // todo マウント系の位置を調整

    /** 上に乗ってるエンティティへのオフセット */
    @Override
    public double getMountedHeightOffset() {
        IMultiModel model =
                getModel(Layer.SKIN, Part.HEAD).orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getMountedYOffset(getCaps());
    }

    /** 騎乗時のオフセット */
    @Override
    public double getHeightOffset() {
        IMultiModel model =
                getModel(Layer.SKIN, Part.HEAD).orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getyOffset(getCaps()) - getHeight();
    }

    // このままだとEntityDimensionsが作っては捨てられてを繰り返すのでパフォーマンスはよろしくない
    // …が、そもそもそんなにたくさん呼ばれるメソッドでもない
    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        EntityDimensions dimensions;
        IMultiModel model =
                getModel(Layer.SKIN, Part.HEAD).orElse(LMModelManager.INSTANCE.getDefaultModel());
        float height = model.getHeight(getCaps(), MMPose.convertPose(pose));
        float width = model.getWidth(getCaps(), MMPose.convertPose(pose));
        dimensions = EntityDimensions.changing(width, height);
        return dimensions.scaled(getScaleFactor());
    }

    @Nullable
    @Override
    public Entity moveToWorld(ServerWorld destination) {
        // ディメンション移動の時に、自由行動地点を削除する
        Entity entity = super.moveToWorld(destination);
        if (entity == null) return null;
        if (entity instanceof LittleMaidEntity && this.getMovingMode() == MovingMode.FREEDOM) {
            ((LittleMaidEntity) entity).setFreedomPos(null);
        }
        return entity;
    }

    // todo これ何のメソッド？
    @Override
    public boolean isInWalkTargetRange(BlockPos pos) {
        // 自身または主人から16ブロック以内
        if (pos.isWithinDistance(pos, 16)
                || TameableUtil.getTameOwner(this)
                        .filter(owner -> owner.getBlockPos().isWithinDistance(pos, 16))
                        .isPresent()) {
            return super.isInWalkTargetRange(pos);
        }
        return false;
    }

    // todo ボイス周りの調整、コンフィグ化
    @Override
    public void playAmbientSound() {
        if (this.getWorld().isClient
                || this.dead
                || getConfigHolder()
                                .getParameter("LivingVoiceRate")
                                .map(
                                        s -> {
                                            try {
                                                return Float.parseFloat(s);
                                            } catch (Exception e) {
                                                return null;
                                            }
                                        })
                                .orElse(0.2f)
                        < random.nextFloat()) {
            return;
        }
        if (getHealth() / getMaxHealth() < 0.3F) {
            play(LMSounds.LIVING_WHINE);
        } else {
            if (age % 4 == 0 && this.getWorld().isSkyVisible(this.getBlockPos())) {
                Biome biome = this.getWorld().getBiome(getBlockPos()).value();
                if (biome.isCold(getBlockPos())) {
                    play(LMSounds.LIVING_COLD);
                } else if (2 <= biome.getTemperature()) {
                    play(LMSounds.LIVING_HOT);
                }
            } else if (age % 4 == 1 && this.getWorld().isRaining()) {
                var pos = getBlockPos();
                Biome biome = this.getWorld().getBiome(pos).value();
                if (biome.getPrecipitation(pos) == Biome.Precipitation.RAIN) {
                    play(LMSounds.LIVING_RAIN);
                } else if (biome.getPrecipitation(pos) == Biome.Precipitation.SNOW) {
                    play(LMSounds.LIVING_SNOW);
                }
            } else {
                if (this.getMainHandStack().getItem() == Items.CLOCK
                        || this.getOffHandStack().getItem() == Items.CLOCK) {
                    int time = (int) (this.getWorld().getTimeOfDay() % 24000);
                    // 時間約23500-1500はse_living_morning
                    // 時間約12500-23500はse_living_night
                    if (time < 1500 || 23500 <= time) {
                        play(LMSounds.LIVING_MORNING);
                    } else if (12500 <= time) {
                        play(LMSounds.LIVING_NIGHT);
                    } else {
                        play(LMSounds.LIVING_DAYTIME);
                    }
                } else {
                    play(LMSounds.LIVING_DAYTIME);
                }
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        // todo 強制再生メソッドを生やす
        // 死亡ボイスは必ず聞かせる
        this.playSoundCool = 0;
        play(LMSounds.DEATH);
        MaidSpeech.onDeath(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        // 强加载女仆被移除/死亡时解除其区块强制加载，避免残留
        releaseForceChunk();
        super.remove(reason);
        // 死亡/销毁时：契约女仆原地掉落绑定了完整女仆数据的纪念品（不掉落任何物品）
        if (this.getWorld() instanceof ServerWorld serverWorld && reason.shouldDestroy()) {
            // 收纳（捕捉蛋）时跳过纪念品掉落
            if (TameableUtil.getTameOwnerUuid(this).isPresent() && !this.captureSuppressSouvenir) {
                ItemStack souvenir = MaidSouvenirItem.createSouvenir(this);
                var souvenirEntity =
                        new MaidSouvenirEntity(
                                serverWorld, this.getX(), this.getY(), this.getZ(), souvenir);
                souvenirEntity.setToDefaultPickupDelay();
                serverWorld.spawnEntity(souvenirEntity);
            }
        }
    }

    /** 收纳（捕捉蛋）时抑制纪念品掉落。 */
    public boolean isCaptureSuppressSouvenir() {
        return captureSuppressSouvenir;
    }

    public void setCaptureSuppressSouvenir(boolean captureSuppressSouvenir) {
        this.captureSuppressSouvenir = captureSuppressSouvenir;
    }

    /** 纪念品复活后的休息状态：血量恢复到自身 50% 前保持休息。 */
    public boolean isSouvenirReviveRest() {
        return souvenirReviveRest;
    }

    public void setSouvenirReviveRest(boolean souvenirReviveRest) {
        this.souvenirReviveRest = souvenirReviveRest;
    }

    /** 是否正处于休息闩锁中（低血量坐姿恢复）。 */
    public boolean isResting() {
        return resting;
    }

    /** 当前是否处于坐姿阶段（骑乘座位，原地不动）。 */
    public boolean isRestSitting() {
        return restSitting;
    }

    /** 休息坐姿的渲染过渡进度（0=站直，1=完全坐下），客户端据此平滑下压模型贴地。 */
    public float getRestSitProgress() {
        return this.dataTracker.get(REST_SIT_PROGRESS);
    }

    /** 是否处于避战状态（战斗模式 + 低血量 + 附近有敌人）。 */
    public boolean isEvading() {
        return evading;
    }

    /** 恢复状态（休息/避战）期间是否忽略三种行动模式（自由/跟随/红石巡逻）的限制。 */
    public boolean isInRecoveryState() {
        return resting || evading;
    }

    /** 休息起始位置（休息踱步时围绕该点走动）。 */
    @Nullable
    public BlockPos getRestAnchor() {
        return restAnchor;
    }

    /** 最近敌对目标（避战逃跑用，20 tick 刷新一次）。 */
    @Nullable
    public LivingEntity getNearestHostile() {
        return nearestHostile;
    }

    /**
     * 避战状态更新（服务端）：战斗模式 + 血量<5% + 附近有敌人 → 进入避战（清目标、主动远离）；
     * 血量恢复到 30% 以上解除并转头迎击；附近持续 10 秒无敌人则转休息状态。
     */
    private void updateEvadeState() {
        if (this.getWorld().isClient) {
            return;
        }
        float ratio = this.getHealth() / this.getMaxHealth();
        boolean battleMode = this.getMode().map(Mode::isBattleMode).orElse(false);
        boolean hasEnemies = !this.restNoEnemy;

        if (!this.evading) {
            // 进入避战：战斗模式 + 血量<5% + 附近有敌人（且非罢工）
            if (battleMode && ratio < EVADE_ENTER_RATIO && hasEnemies && !this.isStrike()) {
                // 若正在休息则先退出休息
                if (this.resting) {
                    this.resting = false;
                    this.restSitting = false;
                    this.dismountRestSeat();
                }
                this.evading = true;
                this.evadeNoEnemyTicks = 0;
                // 清目标避免避战期间攻击敌人
                this.setTarget(null);
                MaidSpeech.onEvade(this);
            }
            return;
        }
        // 避战中
        if (ratio >= EVADE_EXIT_RATIO) {
            // 血量≥30%：解除避战，转头迎击
            this.evading = false;
            this.evadeNoEnemyTicks = 0;
            return;
        }
        if (hasEnemies) {
            this.evadeNoEnemyTicks = 0;
        } else {
            this.evadeNoEnemyTicks++;
            // 附近持续 10 秒无敌人 → 解除避战，转休息状态
            if (this.evadeNoEnemyTicks >= EVADE_TO_REST_TICKS) {
                this.evading = false;
                this.evadeNoEnemyTicks = 0;
                this.resting = true;
                this.restPhaseTimer = 0;
                this.restAnchor = this.getBlockPos();
                MaidSpeech.onRest(this);
            }
        }
    }

    /**
     * 休息状态更新（服务端）：血量 <5% 且附近无敌人时进入休息闩锁，
     * 周期性播放骑乘（坐下）动画并原地不动（约 3.5 秒），随后起身走动约 3.5 秒再坐下，
     * 直到血量恢复到自身 50% 以上才解除。复活后的女仆强制进入该状态直至 50%。
     */
    private void updateRestState() {
        if (this.getWorld().isClient) {
            return;
        }
        float ratio = this.getHealth() / this.getMaxHealth();
        // 附近敌人扫描（每 20 tick 一次，避免每 tick 全量扫描）
        if (--this.restEnemyScanCooldown <= 0) {
            this.restEnemyScanCooldown = 20;
            scanNearbyHostiles();
        }
        boolean noEnemy =
                this.restNoEnemy
                        && (this.getTarget() == null || !this.getTarget().isAlive())
                        && (this.getAttacker() == null || !this.getAttacker().isAlive());

        if (!this.resting) {
            // 进入条件：非罢工 + 无敌人 +（复活休息：血量<50%；普通休息：血量<5%）
            boolean enter =
                    !this.isStrike()
                            && !this.evading
                            && noEnemy
                            && (this.souvenirReviveRest ? ratio < REST_EXIT_RATIO : ratio < REST_ENTER_RATIO);
            if (enter) {
                this.resting = true;
                this.restPhaseTimer = 0;
                this.restAnchor = this.getBlockPos();
                MaidSpeech.onRest(this);
            }
        }
        // 退出条件：血量≥50% 或 进入罢工（两状态不兼容）
        if (this.resting && (ratio >= REST_EXIT_RATIO || this.isStrike())) {
            this.resting = false;
            this.souvenirReviveRest = false;
            this.restSitting = false;
            dismountRestSeat();
            return;
        }
        if (!this.resting) {
            return;
        }
        // 休息中：无敌人时周期性坐/站；有敌人则站立待命（闩锁保留，血量≥50% 才解除）
        if (noEnemy) {
            if (this.restPhaseTimer <= 0) {
                this.restSitting = !this.restSitting;
                this.restPhaseTimer = REST_PHASE_TICKS;
                if (this.restSitting) {
                    mountRestSeat();
                } else {
                    dismountRestSeat();
                }
            } else {
                this.restPhaseTimer--;
            }
        } else {
            dismountRestSeat();
            this.restSitting = false;
            this.restPhaseTimer = 0;
        }
    }

    /** 坐下：不再生成座位实体，仅停止寻路并交由坐姿进度动画驱动模型下压贴地。 */
    private void mountRestSeat() {
        if (this.getWorld().isClient) {
            return;
        }
        this.getNavigation().stop();
    }

    /** 起身：座位实体已移除；坐姿过渡由 REST_SIT_PROGRESS 平滑控制回落，无需额外处理。 */
    private void dismountRestSeat() {}

    /** 坐姿进度直接取 0/1（瞬间坐下/起身，不做缓动），供客户端渲染下压模型。 */
    private void tickRestSitAnimation() {
        if (this.getWorld().isClient) {
            return;
        }
        this.dataTracker.set(REST_SIT_PROGRESS, this.restSitting ? 1.0F : 0.0F);
    }

    /**
     * 扫描附近敌对目标并缓存：更新 restNoEnemy（附近是否有敌人）与 nearestHostile（最近的敌人）。
     * 敌人判定独立于 canTarget（避战期间 canTarget 会返回 false，不能用于此扫描）。
     */
    private void scanNearbyHostiles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            this.restNoEnemy = true;
            this.nearestHostile = null;
            return;
        }
        java.util.List<MobEntity> enemies =
                serverWorld.getEntitiesByClass(
                        MobEntity.class,
                        this.getBoundingBox().expand(REST_ENEMY_RANGE),
                        mob -> mob.isAlive() && !mob.isRemoved() && isHostileToMaid(this, mob));
        this.restNoEnemy = enemies.isEmpty();
        this.nearestHostile =
                enemies.stream()
                        .min(java.util.Comparator.comparingDouble(m -> m.squaredDistanceTo(this)))
                        .orElse(null);
    }

    /** 判定某个怪物是否是对女仆的敌对目标（正追击女仆，或为敌对生物且非友方）。 */
    private static boolean isHostileToMaid(LittleMaidEntity maid, MobEntity mob) {
        if (mob.getTarget() == maid) {
            return true;
        }
        return mob instanceof HostileEntity && !TameableUtil.isFriend(maid, mob);
    }

    // todo 処理の改善
    @Override
    public boolean tryAttack(Entity target) {
        boolean result = super.tryAttack(target);
        if (this.isBloodSuck()) {
            this.play(LMSounds.ATTACK_BLOOD_SUCK);
        } else {
            this.play(LMSounds.ATTACK);
        }
        // PlayerEntityのattack処理を参考に、武器の耐久地を減らす処理を実装する
        if (result) {
            ItemStack mainHandStack = this.getMainHandStack();
            Entity entity = target;
            if (target instanceof EnderDragonPart) {
                entity = ((EnderDragonPart) target).owner;
            }
            if (!this.getWorld().isClient
                    && !mainHandStack.isEmpty()
                    && entity instanceof LivingEntity) {
                // バニラではこのメソッドの第三引数にはプレイヤーエンティティしか渡されない
                // そのため、他Modにおいて必ずプレイヤーであると仮定して実装した場合にクラッシュする可能性がある
                // その対策にtry/catchを置いておく
                try {
                    mainHandStack.getItem().postHit(mainHandStack, (LivingEntity) entity, this);
                } catch (Exception e) {
                    LMMRMod.LOGGER.error("メイドさんの攻撃時に例外が発生しました。", e);
                }
                if (mainHandStack.isEmpty()) {
                    this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
        }
        return result;
    }

    // todo 処理の見直し
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.dead) {
            return super.damage(source, amount);
        }
        if (!this.getWorld().isClient) {
            // 味方のが当たってもちゃんと動くようにフレンド判定より前
            if (amount <= 0 && source.getSource() instanceof SnowballEntity) {
                play(LMSounds.HURT_SNOW);
                return false;
            }
        }
        LMMRConfig config = getConfig();
        if (config.health.nonMobDamageImmunity && source.getAttacker() == null) {
            return false;
        }
        if (config.health.immortal
                && !source.isOf(DamageTypes.OUT_OF_WORLD)
                && !source.isSourceCreativePlayer()) {
            return false;
        }
        if (config.health.fallImmunity && source.isOf(DamageTypes.FALL)) {
            return false;
        }
        Entity attacker = source.getAttacker();
        // Friendからの攻撃を除外
        // 反叛中允许被反叛目标攻击（反叛=对主人/玩家开战）
        if (!config.health.enableFriendlyFire
                && attacker instanceof LivingEntity livingAttacker
                && TameableUtil.isFriend(this, livingAttacker)
                && !(isRebellious() && isRebellionTarget(livingAttacker))) {
            return false;
        }
        // 攻撃禁止対象からのダメージを除外
        if (config.health.blockDamageFromAttackProhibited
                && attacker instanceof LivingEntity
                && getTargetTag(new TargetIdentifier((LivingEntity) attacker))
                        .contains(TargetingSystem.TargetTag.ATTACK_PROHIBITED)) {
            return false;
        }

        // 盾牌格挡：战斗模式 + 副手盾牌 → 格挡攻击。破盾攻击（斧头）也能格挡第一次，
        // 但格挡后盾牌会被禁用较长时间（对齐原版 100 tick = 5 秒），期间无法再次格挡
        if (canBlockWithShield(source, attacker)) {
            ItemStack shield = this.getOffHandStack();
            boolean shieldBreak = isShieldBreaker(attacker);
            // 破盾攻击：盾牌多扣耐久（对应原版 damageShield(5) 的额外损耗）
            shield.damage(
                    shieldBreak ? SHIELD_BREAK_DURABILITY_DAMAGE : 1,
                    this,
                    e -> e.sendEquipmentBreakStatus(EquipmentSlot.OFFHAND));
            this.playSound(
                    SoundEvents.ITEM_SHIELD_BLOCK,
                    1.0F,
                    0.8F + this.getWorld().getRandom().nextFloat() * 0.4F);
            if (shieldBreak) {
                // 破盾格挡：本次格挡成功，但盾牌禁用 5 秒（放下盾 + 长时间无法格挡）
                this.shieldDisableTicks = SHIELD_BREAK_DISABLE_TICKS;
                this.shieldBlockCooldown = SHIELD_BREAK_DISABLE_TICKS;
            } else {
                // 普通格挡：短促冷却 + 盾臂快速挥动作为反馈动画（保留格挡音效）
                this.shieldBlockCooldown = SHIELD_BLOCK_COOLDOWN;
            }
            this.swingHand(Hand.OFF_HAND);
            return false;
        }

        float factor = config.health.generalMaidDamageFactor;
        if (!TameableUtil.isWait(this)
                && this.getMode().map(Mode::isBattleMode).orElse(false)) {
            factor *= config.health.battleModeMaidDamageFactor;
        } else {
            factor *= config.health.nonBattleModeMaidDamageFactor;
        }
        amount *= factor;

        boolean isHurtTime = 0 < this.hurtTime;
        boolean result = super.damage(source, amount);
        // 被玩家攻击 → 生气/好感度下降/逃离
          if (!this.getWorld().isClient
                  && result
                  && attacker instanceof PlayerEntity player
                  && TameableUtil.getTameOwnerUuid(this).isPresent()) {
              // 记录受击前的情绪（用于决定是否逃跑）
              MaidMood.Emotion preEmotion = MaidMood.getEmotionByMood(this.maidMood.getMood());
              boolean isOwner = TameableUtil.isTameOwner(this, player);
              maidMood.onAttackedBy(player.getUuid(), isOwner);
              syncMood();
              MaidSpeech.onHurt(this);
              // 仅悲伤/平淡心情被玩家攻击会逃跑；开心/愤怒/反叛不逃跑
              if (preEmotion == MaidMood.Emotion.SAD || preEmotion == MaidMood.Emotion.CALM) {
                  fleeFrom(player);
              }
        }
        if (!this.getWorld().isClient && !isHurtTime) {
            if (result
                    && 0 < amount
                    && TameableUtil.isWait(this)
                    && TameableUtil.getTameOwnerUuid(this).isPresent()) {
                TameableUtil.setWait(this, false);
            }
            if (!result || amount <= 0F) {
                play(LMSounds.HURT_NO_DAMAGE);
            } else if (amount > 0F && this.blockedByShield(source)) {
                play(LMSounds.HURT_GUARD);
            } else if (source.isOf(DamageTypes.FALL)) {
                play(LMSounds.HURT_FALL);
            } else if (source.getType().effects() == DamageEffects.BURNING) {
                play(LMSounds.HURT_FIRE);
            } else {
                play(LMSounds.HURT);
            }
        }
        return result;
    }

    /**
     * 盾牌格挡条件：处于持盾姿态（副手盾牌正在举起/使用）+ 攻击伤害（有攻击者）。
     *
     * <p>参照车万女仆：必须先举盾（持盾姿态）才能格挡，并非副手持盾就无条件格挡。
     * 非反叛：仅战斗模式且周围有敌人（举盾条件满足）时可格挡；反叛时持盾姿态恒定开启。
     * 破盾攻击（斧头）与原版一致：可以格挡第一次，但格挡后盾牌禁用 5 秒（见 damage() 中
     * 格挡成功分支的 shieldDisableTicks 处理）。进食中副手是食物、自然不格挡。
     */
    private boolean canBlockWithShield(DamageSource source, Entity attacker) {
        if (this.getWorld().isClient) {
            return false;
        }
        // 只有"持盾姿态"（副手盾牌正在举起/使用）时才可格挡
        if (!this.isBlockingPose()) {
            return false;
        }
        // 格挡冷却中不可再次格挡
        if (this.shieldBlockCooldown > 0) {
            return false;
        }
        // 仅战斗模式可格挡：反叛与正常共用同一规则（举盾姿态由 shouldRaiseShield 统一控制）
        if (!this.getMode().map(Mode::isBattleMode).orElse(false)) {
            return false;
        }
        // 环境伤害（摔落/火烧/饥饿等无攻击者）不属于攻击，不格挡
        if (attacker == null) {
            return false;
        }
        // 副手必须是盾牌（进食时副手是食物，自动不格挡）
        return this.getOffHandStack().getItem() instanceof ShieldItem;
    }

    /** 是否为破盾攻击（攻击者主手武器可禁用盾牌，如斧头/模组破盾武器）。 */
    private boolean isShieldBreaker(Entity attacker) {
        return attacker instanceof LivingEntity livingAttacker
                && (livingAttacker.getMainHandStack().getItem() instanceof AxeItem
                        || livingAttacker.getMainHandStack().isIn(ItemTags.AXES));
    }

    /** 是否应举起盾牌（持盾姿态判定，供服务端维护 usingItem 状态；车万女仆同款思路）。 */
    public boolean shouldRaiseShield() {
        if (!(this.getOffHandStack().getItem() instanceof ShieldItem)) {
            return false;
        }
        // 破盾禁用期间不举盾（盾牌放下，5 秒后恢复；对齐原版盾牌物品冷却）
        if (this.shieldDisableTicks > 0) {
            return false;
        }
        // 远程战斗模式（弓/枪/魔法）需要占用主手"使用"状态，举盾会与之冲突，交给武器
        if (this.getMode()
                .map(mode -> mode.getBattleModeType() == BattleModeType.BOW)
                .orElse(false)) {
            return false;
        }
        if (!this.getMode().map(Mode::isBattleMode).orElse(false)) {
            return false;
        }
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive();
    }

    /** 是否处于持盾动画状态：以原版 usingItem 状态为准（副手正在使用盾牌），
     * 模型据此播放持盾姿势、盾牌前移。 */
    public boolean isBlockingPose() {
        return this.isUsingItem()
                && !this.getActiveItem().isEmpty()
                && this.getActiveItem().getItem() instanceof ShieldItem;
    }

    @Override
    public boolean isBlocking() {
        // 格挡伤害由自定义 canBlockWithShield 处理；不启用原版格挡管线，避免双重格挡/绕过冷却
        return false;
    }

    /** 反叛时在头顶播放村民同款怒气粒子（服务端广播给附近玩家）。 */
    private void spawnRebellionAngerParticles() {
        if (this.getWorld().isClient || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        double px = this.getX();
        double py = this.getY() + this.getHeight() * 0.8F;
        double pz = this.getZ();
        serverWorld.spawnParticles(
                ParticleTypes.ANGRY_VILLAGER, px, py, pz, 5, 0.25, 0.15, 0.25, 0.0);
    }

    /** 怒り時に、対象から逃げる。逃跑期间抑制跟随/工作/传送/休息目标，确保真的跑得掉。 */
    private void fleeFrom(LivingEntity target) {
        this.fleeingTicks = 100;
        Vec3d away = this.getPos().subtract(target.getPos());
        if (away.lengthSquared() < 1.0E-4) {
            away =
                    new Vec3d(
                            -Math.sin(Math.toRadians(this.getYaw())),
                            0,
                            Math.cos(Math.toRadians(this.getYaw())));
        } else {
            away = away.normalize();
        }
        this.getNavigation()
                .startMovingTo(this.getX() + away.x * 8.0, this.getY(), this.getZ() + away.z * 8.0, 1.3);
    }

    /** 是否处于愤怒反叛状态：心情归零（mood=0）后仍被攻击才彻底反叛；心情 0-30 仅愤怒（逃跑）。 */
    public boolean isRebellious() {
        return this.maidMood.getAngerTargetUuid().isPresent()
                && this.maidMood.getMood() <= 0;
    }

    /** 自动台词触发冷却：气泡结束后再等一段时间才允许下一条自动台词，避免刷屏。 */
    private static final int AUTO_SPEECH_COOLDOWN = 100;
    /** 休息状态进入阈值（血量/最大血量 < 5%）。 */
    private static final float REST_ENTER_RATIO = 0.05F;
    /** 休息状态解除阈值（血量/最大血量 ≥ 50%）。 */
    private static final float REST_EXIT_RATIO = 0.5F;
    /** 休息坐/站阶段时长（tick，约 3.5 秒）。 */
    private static final int REST_PHASE_TICKS = 70;
    /** 休息状态检测"附近敌人"的范围（格）。 */
    private static final double REST_ENEMY_RANGE = 16.0D;
    /** 避战进入阈值（血量/最大血量 < 5%）。 */
    private static final float EVADE_ENTER_RATIO = 0.05F;
    /** 避战解除阈值（血量/最大血量 ≥ 30%，转头迎击）。 */
    private static final float EVADE_EXIT_RATIO = 0.30F;
    /** 避战期间附近持续无敌人达到该 tick 数（200=10 秒）后转休息状态。 */
    private static final int EVADE_TO_REST_TICKS = 200;
    /** 自动台词（tickSpeechEvents）触发后的冷却剩余时间。 */
    private int speechAutoCooldown = 0;

    /** 逃跑中（被玩家攻击后短暂逃离，期间跟随/工作/传送目标让路）。 */
    public boolean isFleeing() {
        return this.fleeingTicks > 0;
    }

    /** 自动进食阈值：0=不自动进食，40/60/80=饥饿低于该百分比自动进食，-1=无时无刻进食。 */
    public int getAutoEatThreshold() {
        return this.dataTracker.get(AUTO_EAT_THRESHOLD);
    }

    public void setAutoEatThreshold(int threshold) {
        // 只接受合法档位（-1=无时无刻，0/40/60/80=按饥饿阈值）
        if (threshold == -1 || threshold == 0 || threshold == 40 || threshold == 60 || threshold == 80) {
            this.dataTracker.set(AUTO_EAT_THRESHOLD, threshold);
        }
    }

    /** 切换到下一个自动进食档位（80→60→40→无时无刻→关闭→80）。 */
    public void cycleAutoEatThreshold() {
        int cur = getAutoEatThreshold();
        int next =
                switch (cur) {
                    case 80 -> 60;
                    case 60 -> 40;
                    case 40 -> -1;
                    case -1 -> 0;
                    default -> 80;
                };
        this.dataTracker.set(AUTO_EAT_THRESHOLD, next);
    }

    /** 目标实体是否为愤怒反叛目标。 */
    public boolean isRebellionTarget(LivingEntity entity) {
        return this.maidMood.getAngerTargetUuid().map(u -> entity.getUuid().equals(u)).orElse(false);
    }

    @Override
    public void setHealth(float health) {
        LMMRConfig config = getConfig();
        if (config.health.disableMaidDeath && health <= 0) {
            super.setHealth(1);
            return;
        }
        super.setHealth(health);
    }

    @Override
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        if (isBloodSuck()) play(LMSounds.LAUGHTER);

        return super.onKilledOther(world, other);
    }

    // 射撃

    // todo try/catchを挟む。処理の見直し
    @Override
    public void attack(LivingEntity target, float pullProgress) {
        var stack = this.getMainHandStack();
        // 弾が無い場合は実行されないはずだが、念のためチェック
        var arrowStack = this.getProjectileType(stack);
        boolean isInfinite = EnchantmentHelper.getLevel(Enchantments.INFINITY, stack) >= 1;
        if (arrowStack.isEmpty() && !isInfinite) {
            return;
        }
        if (stack.getItem() instanceof BowItem bowItem) {
            var arrow = ProjectileUtil.createArrowProjectile(this, arrowStack, pullProgress);
            if (arrowStack.getItem() instanceof ArrowItem && !isInfinite) {
                arrow.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
            }
            arrow = EPEntityUtil.arrowCustomHook(bowItem, arrow);
            double xDiff = target.getX() - this.getX();
            double yDiff = target.getEyeY() - arrow.getY();
            double zDiff = target.getZ() - this.getZ();
            double horizonLen = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            arrow.setVelocity(
                    xDiff,
                    yDiff + horizonLen * 0.025,
                    zDiff,
                    pullProgress * 3.0f * getConfig().work.archerShootVelocityFactor,
                    14 - 2 * 4);
            this.playSound(
                    SoundEvents.ENTITY_ARROW_SHOOT,
                    1.0f,
                    1.0f / (this.getRandom().nextFloat() * 0.4f + 1.2f) + pullProgress * 0.5f);
            this.getWorld().spawnEntity(arrow);
            arrowStack.decrement(1);
        } else if (stack.getItem() instanceof CrossbowItem) {
            this.shoot(this, CrossbowItemInvoker.getSpeed(stack));
        }
    }

    // クロスボウ

    public boolean isCharging() {
        return this.dataTracker.get(CHARGING);
    }

    @Override
    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }

    @Override
    public void shoot(
            LivingEntity target,
            ItemStack crossbow,
            ProjectileEntity projectile,
            float multiShotSpray) {
        this.shoot(
                this, target, projectile, multiShotSpray, CrossbowItemInvoker.getSpeed(crossbow));
    }

    // todo 弾道調整
    @Override
    public void shoot(
            LivingEntity entity,
            LivingEntity target,
            ProjectileEntity projectile,
            float multishotSpray,
            float speed) {
        double xDiff = target.getX() - entity.getX();
        double yDiff = target.getEyeY() - projectile.getY();
        double zDiff = target.getZ() - entity.getZ();
        double horizonLen = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
        Vector3f targetAt =
                this.getProjectileLaunchVelocity(
                        entity,
                        new Vec3d(xDiff, yDiff + horizonLen * 0.025, zDiff),
                        multishotSpray);
        projectile.setVelocity(
                targetAt.x(),
                targetAt.y(),
                targetAt.z(),
                speed * getConfig().work.archerShootVelocityFactor,
                14 - entity.getWorld().getDifficulty().getId() * 4);
        entity.playSound(
                SoundEvents.ITEM_CROSSBOW_SHOOT,
                1.0f,
                1.0f / (entity.getRandom().nextFloat() * 0.4f + 0.8f));
    }

    @Override
    public void postShoot() {}

    @Override
    protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
        if (type != MovementType.SELF && type != MovementType.PLAYER) {
            return movement;
        }
        return safeMovement.adjust(movement);
    }

    // マイナスの値も返すことを利用しているため、バージョンアップ/mixinでの仕様変更に注意が必要
    private float getDangerHeightThreshold() {
        int fallDamage = computeFallDamage(0, 1);
        return -fallDamage;
    }

    // todo 複数モデルで問題ないかチェック
    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0.0, this.getStandingEyeHeight() - 0.15f, 1f / 16f);
    }

    // todo 処理の見直し、処理を追加可能に
    // todo 使用アイテムをコンフィグから追加可能に
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return LMInteractionHandler.handle(this, player, hand);
    }

    int getExperiencePoints() {
        return this.experiencePoints;
    }

    public void addExperience(int experience) {
        this.experiencePoints =
                MathHelper.clamp(this.experiencePoints + experience, 0, Integer.MAX_VALUE);
    }

    // GUI開くやつ
    public void openInventory(PlayerEntity player) {
        if (player.getWorld().isClient) {
            return;
        }
        setAttacker(null);
        getNavigation().stop();
        MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, screenFactory);
    }

    /** 打开女仆饰品界面（Curios，第 0 页）。 */
    public void openCuriosScreen(PlayerEntity player) {
        openCuriosScreen(player, 0);
    }

    /** 打开女仆饰品界面（Curios，指定页，用于饰品栏分页切换）。 */
    public void openCuriosScreen(PlayerEntity player, int page) {
        if (player.getWorld().isClient) {
            return;
        }
        setAttacker(null);
        getNavigation().stop();
        // Forge openScreen 先调用 saveExtraData 再调用 createMenu，故必须在打开前同步槽位结构，
        // 否则 saveExtraData 时 lastMenu 为 null，客户端收到空结构不建饰品槽（IndexOutOfBounds）
        Map<String, Integer> structure =
                net.sistr.littlemaidmobresurgence.compat.CuriosCompat.syncCuriosStructure(
                        this, player);
        MenuRegistry.openExtendedMenu(
                (ServerPlayerEntity) player,
                new CuriosScreenHandlerFactory(this, page, structure));
    }

    /** 打开女仆扩容背包界面。 */
    public void openBackpackScreen(PlayerEntity player) {
        openBackpackScreen(player, 0);
    }

    /** 打开女仆扩容背包界面（指定页，用于翻页）。 */
    public void openBackpackScreen(PlayerEntity player, int page) {
        if (player.getWorld().isClient) {
            return;
        }
        setAttacker(null);
        getNavigation().stop();
        MenuRegistry.openExtendedMenu(
                (ServerPlayerEntity) player, new BackpackScreenHandlerFactory(this, page));
    }

    /** 0:wait 1:freedom 2:tracer 3:aiming 4:begging 5:blood suck */
    public void setLMMFlag(int index, boolean value) {
        int i = this.dataTracker.get(LMM_FLAGS);
        int mask = (1 << index);
        if (value) {
            i |= mask;
        } else {
            i &= ~mask;
        }
        this.dataTracker.set(LMM_FLAGS, (byte) i);
    }

    public boolean getLMMFlag(int index) {
        return (this.dataTracker.get(LMM_FLAGS) & (1 << index)) != 0;
    }

    @Override
    public MovingMode getMovingMode() {
        return MovingMode.fromId(this.dataTracker.get(MOVING_MODE));
    }

    @Override
    public void setMovingMode(MovingMode movingMode) {
        this.dataTracker.set(MOVING_MODE, (byte) movingMode.getId());
    }

    // Flee

    Map<MobEntity, Predicate<MobEntity>> getFleeEntities() {
        return this.fleeEntities;
    }

    public void addFleeEntity(MobEntity entity, Predicate<MobEntity> removePredicate) {
        this.fleeEntities.put(entity, removePredicate);
    }

    // インベントリ関連

    @Override
    public Inventory getInventory() {
        return this.littleMaidInventory.getInventory();
    }

    @Override
    public void writeInventory(NbtCompound tag) {
        this.littleMaidInventory.writeInventory(tag);
    }

    @Override
    public void readInventory(NbtCompound tag) {
        this.littleMaidInventory.readInventory(tag);
    }

    public int getWorkItemSlotSize() {
        return this.littleMaidInventory.getWorkItemSlotSize();
    }

    public void setWorkItemSlotNum(int num) {
        this.littleMaidInventory.setWorkItemSlotSize(num);
    }

    // 背包扩容相关

    /** 背包扩容道具专属槽位（独立于 Curios）。 */
    public net.minecraft.inventory.Inventory getBackpackUpgradeSlot() {
        return backpackUpgradeSlot;
    }

    /** 当前扩容等级（0~5，0=未装备扩容道具）。 */
    public int getBackpackUpgradeLevel() {
        return BackpackUpgradeItem.getLevel(backpackUpgradeSlot.getStack(0));
    }

    /** 当前扩容提供的额外格子数（未装备为 0）。 */
    public int getBackpackExtraSlots() {
        return BackpackUpgradeItem.getBonusSlots(backpackUpgradeSlot.getStack(0));
    }

    // todo 計算式の見直し
    @Override
    protected void damageArmor(DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            int i = -1;
            for (ItemStack stack : this.getArmorItems()) {
                i++;
                if (source.isIn(DamageTypeTags.IS_FIRE) && stack.getItem().isFireproof()
                        || !(stack.getItem() instanceof ArmorItem)) {
                    continue;
                }
                var slot = EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, i);
                stack.damage((int) amount, this, arg -> arg.sendEquipmentBreakStatus(slot));
            }
        }
    }

    @Override
    protected void damageHelmet(DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            var stack = getEquippedStack(EquipmentSlot.HEAD);
            if (source.isIn(DamageTypeTags.IS_FIRE) && stack.getItem().isFireproof()
                    || !(stack.getItem() instanceof ArmorItem)) {
                return;
            }
            stack.damage(
                    (int) amount, this, arg -> arg.sendEquipmentBreakStatus(EquipmentSlot.HEAD));
        }
    }

    @Override
    protected void damageShield(float amount) {
        // todo ガード実装
    }

    // todo どこで使われるメソッド？
    @Override
    public StackReference getStackReference(int mappedIndex) {
        var inv = getInventory();
        int i = mappedIndex - 200;
        if (0 <= i && i < inv.size()) {
            return StackReference.of(inv, i);
        }
        return super.getStackReference(mappedIndex);
    }

    // todo 処理の見直し
    @Override
    public ItemStack getProjectileType(ItemStack stack) {
        if (!(stack.getItem() instanceof RangedWeaponItem ranged)) {
            return ItemStack.EMPTY;
        }
        Predicate<ItemStack> predicate = ranged.getHeldProjectiles();
        ItemStack itemStack = RangedWeaponItem.getHeldProjectile(this, predicate);
        if (!itemStack.isEmpty()) {
            return EPEntityUtil.arrowCustomHook(this, stack, itemStack);
        }
        predicate = ranged.getProjectiles();
        var inv = getInventory();
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack itemStack2 = inv.getStack(i);
            if (predicate.test(itemStack2)) {
                return EPEntityUtil.arrowCustomHook(this, stack, itemStack2);
            }
        }
        return EPEntityUtil.arrowCustomHook(this, stack, ItemStack.EMPTY);
    }

    // 防具の更新
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        super.equipStack(slot, stack);

        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            multiModel.updateArmor();
        }
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        // dropInventoryで捨てるので不要
        // 実装的に、こちらはランダムドロップに使うもの
    }

    @Override
    protected void dropInventory() {
        // 死亡不掉落任何物品：全部物品保存在纪念品 NBT 中，复活时随女仆一起恢复
    }

    @Override
    public int getXpToDrop() {
        // 经验值随死亡正常掉落（对齐原版）；物品不掉落，封存在纪念品 NBT
        return this.experiencePoints;
    }

    // todo IdFactorの仕様の見直し
    @Override
    public void setUuid(UUID uuid) {
        super.setUuid(uuid);
        initIdFactor();
    }

    public void initIdFactor() {
        this.idFactor = this.getUuid().hashCode() & 0x7fffffff;
    }

    public int getIdFactor() {
        return idFactor;
    }

    // テイム関連

    @Override
    public void setOwnerUuid(@Nullable UUID uuid) {
        super.setOwnerUuid(uuid);
        this.setContract(true);
    }

    public void setFreedomPos(@Nullable BlockPos freedomPos) {
        this.freedomPos = freedomPos;
    }

    /** 是否绑定了工作范围，且中心位于给定维度。 */
    public boolean hasBoundWorkCenter(World world) {
        return boundWorkCenter != null
                && boundWorkDimension != null
                && boundWorkDimension.equals(world.getRegistryKey().getValue());
    }

    /** 女仆杖绑定的工作范围中心（无敌对女仆时返回 null）。 */
    @Nullable
    public BlockPos getBoundWorkCenter() {
        return boundWorkCenter;
    }

    /** 绑定范围所在维度。 */
    @Nullable
    public Identifier getBoundWorkDimension() {
        return boundWorkDimension;
    }

    /** 设置绑定中心与维度（成对设置）。 */
    public void setBoundWorkCenter(@Nullable BlockPos center, @Nullable Identifier dimension) {
        this.boundWorkCenter = center;
        this.boundWorkDimension = dimension;
    }

    /** 清除女仆的绑定中心与维度。 */
    public void clearBoundWorkCenter() {
        this.boundWorkCenter = null;
        this.boundWorkDimension = null;
    }

    public Optional<BlockPos> getFreedomPos() {
        if (this.getMovingMode() != MovingMode.FREEDOM) {
            return Optional.empty();
        }
        if (freedomPos == null) {
            freedomPos = this.getBlockPos();
        }
        return Optional.of(freedomPos);
    }

    @Override
    public void setInSittingPose(boolean inSittingPose) {}

    @Override
    public boolean isInSittingPose() {
        return TameableUtil.isWait(this);
    }

    @Override
    public void setSitting(boolean sitting) {
        this.setLMMFlag(WAIT_INDEX, sitting);
    }

    @Override
    public boolean isSitting() {
        return this.getLMMFlag(WAIT_INDEX);
    }

    @Override
    public boolean isTamed() {
        return TameableUtil.getTameOwnerUuid(this).isPresent();
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    public boolean isBegging() {
        return this.getLMMFlag(BEGGING_INDEX);
    }

    public void setBegging(boolean begging) {
        this.setLMMFlag(BEGGING_INDEX, begging);
    }

    public boolean isBloodSuck() {
        return this.getLMMFlag(BLOOD_SUCK_INDEX);
    }

    public void setBloodSuck(boolean isBloodSuck) {
        this.setLMMFlag(BLOOD_SUCK_INDEX, isBloodSuck);
    }

    /** 是否开启捡取掉落物（被动吸附 + 主动走去捡取）。默认开启。 */
    public boolean isPickupItem() {
        return this.getLMMFlag(PICKUP_ITEM_INDEX);
    }

    public void setPickupItem(boolean pickup) {
        this.setLMMFlag(PICKUP_ITEM_INDEX, pickup);
    }

    /** 是否开启"强加载"（女仆所在区块被强制加载，不会被清理/卸载）。 */
    public boolean isForceChunkLoad() {
        return this.dataTracker.get(FORCE_CHUNK_LOAD);
    }

    /** 开关"强加载"：服务端立即强制/解除对应区块，客户端仅同步状态。 */
    public void setForceChunkLoad(boolean enabled) {
        this.dataTracker.set(FORCE_CHUNK_LOAD, enabled);
        if (this.getWorld().isClient) {
            return;
        }
        if (enabled) {
            updateForceChunk();
        } else {
            releaseForceChunk();
        }
    }

    /** 服务端逐 tick：强加载开启时跟踪女仆所在区块，移动后自动迁移强制加载。 */
    private void updateForceChunk() {
        if (this.getWorld().isClient || !this.isForceChunkLoad()) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        var chunkPos = this.getChunkPos();
        if (chunkPos.x == this.forceChunkX && chunkPos.z == this.forceChunkZ) {
            return;
        }
        releaseForceChunk();
        serverWorld.setChunkForced(chunkPos.x, chunkPos.z, true);
        this.forceChunkX = chunkPos.x;
        this.forceChunkZ = chunkPos.z;
    }

    /** 服务端：解除当前强加载区块（若存在）。 */
    private void releaseForceChunk() {
        if (this.forceChunkX == Integer.MIN_VALUE || this.forceChunkZ == Integer.MIN_VALUE) {
            return;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.setChunkForced(this.forceChunkX, this.forceChunkZ, false);
        }
        this.forceChunkX = Integer.MIN_VALUE;
        this.forceChunkZ = Integer.MIN_VALUE;
    }

    @Environment(EnvType.CLIENT)
    public float getInterestedAngle(float tickDelta) {
        return (prevInterestedAngle + (interestedAngle - prevInterestedAngle) * tickDelta)
                * ((getId() % 2 == 0 ? 0.08F : -0.08F) * (float) Math.PI);
    }

    @Environment(EnvType.CLIENT)
    private void tickInterestedAngle() {
        prevInterestedAngle = interestedAngle;
        if (isBegging()) {
            interestedAngle = interestedAngle + (1.0F - interestedAngle) * 0.4F;
        } else {
            interestedAngle = interestedAngle + (0.0F - interestedAngle) * 0.4F;
        }
    }

    // 　加速機能

    public int getTickMultiple() {
        // 死亡时强制 1x tick：保证完整播放原版 20tick 的被击退/倒地死亡动画，
        // 加速(多倍 tick)状态不得把它压缩或跳过，否则女仆会"瞬间消失"。
        if (this.isDead()) {
            return 1;
        }
        return this.isAcceleration() ? getConfig().misc.accelerationMultiple : 1;
    }

    public void setAccelerationTicks(int ticks) {
        this.accelerationTicks = ticks;
        if (ticks > 0) {
            this.dataTracker.set(ACCELERATE, true);
        }
    }

    public void decAccelerationTicks() {
        if (this.accelerationTicks > 0) {
            this.accelerationTicks--;
        }
        if (this.accelerationTicks <= 0) {
            this.accelerationTicks = 0;
            this.dataTracker.set(ACCELERATE, false);
        }
    }

    public int getAccelerationTicks() {
        return this.accelerationTicks;
    }

    public boolean isAcceleration() {
        return this.dataTracker.get(ACCELERATE);
    }

    // お給料

    @Override
    public boolean isContract() {
        return TameableUtil.getTameOwnerUuid(this).isPresent();
    }

    @Override
    public void setContract(boolean isContract) {
        itemContractable.setContract(isContract);
    }

    @Override
    public boolean isStrike() {
        return this.getLMMFlag(STRIKE_INDEX);
    }

    @Override
    public void setStrike(boolean strike) {
        itemContractable.setStrike(strike);
        this.setLMMFlag(STRIKE_INDEX, strike);
    }

    @Override
    public void writeContractable(NbtCompound nbt) {
        itemContractable.writeContractable(nbt);
    }

    @Override
    public void readContractable(NbtCompound nbt) {
        itemContractable.readContractable(nbt);
        if (itemContractable.isStrike()) {
            this.setStrike(true);
        }
    }

    public int getUnpaidDays() {
        return itemContractable.getUnpaidTimes();
    }

    // お給料受け取り

    @Override
    public void listenSalaryBoxPos(BlockPos pos) {
        itemContractable.listenSalaryBoxPos(pos);
    }

    // モード機能

    @Override
    public Optional<Mode> getMode() {
        if (this.isStrike()) {
            return Optional.empty();
        }
        return hasModeImpl.getMode();
    }

    @Override
    public void writeModeData(NbtCompound tag) {
        hasModeImpl.writeModeData(tag);
    }

    @Override
    public void readModeData(NbtCompound tag) {
        hasModeImpl.readModeData(tag);
    }

    public void addMode(Mode mode) {
        hasModeImpl.addMode(mode);
    }

    public void addAllMode(Collection<Mode> mode) {
        hasModeImpl.addAllMode(mode);
    }

    public void setModeName(String modeName) {
        this.dataTracker.set(MODE_NAME, modeName);
    }

    @Environment(EnvType.CLIENT)
    public Optional<String> getModeName() {
        String modeName = this.dataTracker.get(MODE_NAME);
        if (modeName.isEmpty()) return Optional.empty();
        return Optional.of(modeName);
    }

    // TargetTag

    @Override
    public Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id) {
        return TameableUtil.getTameOwner(this)
                .map(l -> l instanceof TargetTagManager ? (TargetTagManager) l : null)
                .map(
                        t -> {
                            var otherSync = t.getTargetTagsSync();
                            var thisSync = this.getTargetTagsSync();
                            if (otherSync.hash() != thisSync.hash()) {
                                thisSync.syncFrom(otherSync);
                            }
                            return t;
                        })
                .orElse(this.targetTagManager)
                .getTargetTag(id);
    }

    @Override
    public void writeTargetTags(NbtCompound nbt) {
        this.targetTagManager.writeTargetTags(nbt);
    }

    @Override
    public void readTargetTags(NbtCompound nbt) {
        this.targetTagManager.readTargetTags(nbt);
    }

    @Override
    public Sync getTargetTagsSync() {
        return this.targetTagManager.getTargetTagsSync();
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return canTarget(target);
    }

    // 構え

    @Override
    public boolean isAimingBow() {
        return this.getLMMFlag(AIMING_INDEX);
    }

    @Override
    public void setAimingBow(boolean aiming) {
        this.setLMMFlag(AIMING_INDEX, aiming);
    }

    // マルチモデル関連

    @Override
    public boolean isAllowChangeTexture(
            Entity entity, TextureHolder textureHolder, Layer layer, Part part) {
        return multiModel.isAllowChangeTexture(entity, textureHolder, layer, part);
    }

    @Override
    public void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part) {
        multiModel.setTextureHolder(textureHolder, layer, part);
        if (layer == Layer.SKIN) {
            calculateDimensions();
        }
    }

    @Override
    public TextureHolder getTextureHolder(Layer layer, Part part) {
        return multiModel.getTextureHolder(layer, part);
    }

    @Override
    public void setColorMM(TextureColors textureColor) {
        multiModel.setColorMM(textureColor);
    }

    @Override
    public TextureColors getColorMM() {
        return multiModel.getColorMM();
    }

    @Override
    public void setContractMM(boolean isContract) {
        multiModel.setContractMM(isContract);
    }

    /**
     * マルチモデルの使用テクスチャが契約時のものかどうか ※実際に契約状態かどうかをチェックする場合、 {@link
     * TameableUtil#getTameOwnerUuid(Tameable)}がisPresent()かでチェックすること
     */
    @Override
    public boolean isContractMM() {
        return multiModel.isContractMM();
    }

    @Override
    public Optional<IMultiModel> getModel(Layer layer, Part part) {
        return multiModel.getModel(layer, part);
    }

    @Override
    public Optional<Identifier> getTexture(Layer layer, Part part, boolean isLight) {
        return multiModel.getTexture(layer, part, isLight);
    }

    @Override
    public IModelCaps getCaps() {
        return caps;
    }

    @Override
    public boolean isArmorVisible(Part part) {
        return multiModel.isArmorVisible(part);
    }

    @Override
    public boolean isArmorGlint(Part part) {
        return multiModel.isArmorGlint(part);
    }

    public boolean isPlayingSnow() {
        return this.getLMMFlag(PLAYING_SNOW_INDEX);
    }

    public void setPlayingSnow(boolean isPlayingSnow) {
        this.setLMMFlag(PLAYING_SNOW_INDEX, isPlayingSnow);
    }

    // 音声関係

    // todo 強制再生メソッドを生やす
    // todo 再生クールダウンをコンフィグ化
    @Override
    public void play(String soundName) {
        if (0 < this.playSoundCool) {
            return;
        }
        this.playSoundCool = getConfig().misc.playSoundInterval;
        if (isBloodSuck()) {
            if (soundName.equals(LMSounds.FIND_TARGET_N)) {
                soundName = LMSounds.FIND_TARGET_B;
            } else if (soundName.equals(LMSounds.ATTACK)) {
                soundName = LMSounds.ATTACK_BLOOD_SUCK;
            }
        }
        soundPlayer.play(soundName);
    }

    @Override
    public void setConfigHolder(ConfigHolder configHolder) {
        soundPlayer.setConfigHolder(configHolder);
    }

    @Override
    public ConfigHolder getConfigHolder() {
        return soundPlayer.getConfigHolder();
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return SpawnLittleMaidPacket.create(this);
    }

    public static LMMRConfig getConfig() {
        return LMMRMod.getConfig();
    }

    // MOVEとLOOKでGoalを分離
    public static class LMStareAtHeldItemGoal<T extends LittleMaidEntity>
            extends TameableStareAtHeldItemGoal<T> {
        private final LittleMaidEntity maid;

        public LMStareAtHeldItemGoal(
                T mob,
                Supplier<Float> stareAtRange,
                Predicate<ItemStack> targetItem,
                boolean isTamed) {
            super(mob, stareAtRange, targetItem, isTamed);
            this.maid = mob;
        }

        @Override
        public void tick() {
            super.tick();
            // 動いてたら傾げない
            this.maid.setBegging(this.maid.getNavigation().isIdle());
        }

        @Override
        public void stop() {
            super.stop();
            this.maid.setBegging(false);
        }
    }
}
