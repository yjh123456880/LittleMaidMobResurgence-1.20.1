package net.sistr.littlemaidmobresurgence.config;

import com.google.common.collect.Lists;
import java.util.List;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.tags.LMTags;

/**
 * [zh] 模组总配置：按分类聚合全部可配置项（生成/生命/心情/对话/物品/移动/工作/契约等）。
 * [en] Global mod config aggregating all options by category (spawn/health/mood/speech/item/movement/work/contract...).
 * [ja] モッド全体のコンフィグ。カテゴリ別に全設定項目を集約します（スポーン/体力/機嫌/セリフ/アイテム/移動/作業/契約など）。
 */
@Config(name = LMMRMod.MODID)
public class LMMRConfig implements ConfigData {

    @ConfigEntry.Category("spawn")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Spawn spawn = new Spawn();

    public static class Spawn {
        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip
        public boolean canNaturalSpawn = true;

        @ConfigEntry.Gui.Tooltip public boolean canDespawn = false;
        @ConfigEntry.Gui.Tooltip public int spawnMinLightLevel = 8;

        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip
        public List<String> maidSpawnBiomeTags =
                Lists.newArrayList(
                        LMTags.Biomes.MAID_SPAWN_BIOME.id().toString(),
                        BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE.id().toString(),
                        BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE.id().toString(),
                        BiomeTags.VILLAGE_SAVANNA_HAS_STRUCTURE.id().toString(),
                        BiomeTags.VILLAGE_SNOWY_HAS_STRUCTURE.id().toString(),
                        BiomeTags.VILLAGE_TAIGA_HAS_STRUCTURE.id().toString());

        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip
        public List<String> maidSpawnExcludeBiomeTags =
                Lists.newArrayList(LMTags.Biomes.MAID_SPAWN_EXCLUDE_BIOME.id().toString());

        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip public int spawnWeight = 5;
        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip public int minSpawnGroupSize = 1;
        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip public int maxSpawnGroupSize = 3;
        @ConfigEntry.Gui.Tooltip public boolean silentDefaultVoice = false;
        @ConfigEntry.Gui.Tooltip public String defaultSoundPackName = "";
    }

    @ConfigEntry.Category("health")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Health health = new Health();

    public static class Health {
        @ConfigEntry.Gui.Tooltip public int healInterval = 2;
        @ConfigEntry.Gui.Tooltip public int healAmount = 1;
        @ConfigEntry.Gui.Tooltip public float healDelayThreshold = 0.75f;
        @ConfigEntry.Gui.Tooltip public boolean disableMaidDeath = false;
        @ConfigEntry.Gui.Tooltip public int rebellionMeleeDamage = 4;
        @ConfigEntry.Gui.Tooltip public float generalMaidDamageFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip public float battleModeMaidDamageFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip public float nonBattleModeMaidDamageFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip public boolean enableFriendlyFire = true;
        @ConfigEntry.Gui.Tooltip public boolean blockDamageFromAttackProhibited = false;
        @ConfigEntry.Gui.Tooltip public boolean enableSafeMove = true;
        @ConfigEntry.Gui.Tooltip public boolean immortal = false;
        @ConfigEntry.Gui.Tooltip public boolean fallImmunity = false;
        @ConfigEntry.Gui.Tooltip public boolean nonMobDamageImmunity = false;
    }

    @ConfigEntry.Category("mood")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Mood mood = new Mood();

    /**
     * [zh] 心情值与好感度相关设置（心情/情绪、好感度等级、怒气计时、拴绳气泡惩罚）。
     * [en] Mood and favorability settings (emotion, favorability levels, anger timer, leash-speech penalty).
     * [ja] 機嫌と好感度の設定（感情、好感度レベル、怒りタイマー、ロープ時のセリフ罰則）。
     */
    public static class Mood {
        // [zh] 好感/情绪变动幅度已大幅减弱：增减极小、恢复极慢，不易剧变
        // [en] Mood/favorability changes are intentionally tiny and recovery is slow to avoid drastic swings.
        // [ja] 機嫌・好感度の変動は意図的に小さく、回復も遅くして急激な変化を防いでいます。
        @ConfigEntry.Gui.Tooltip public int moodDropOnAttack = 2;
        @ConfigEntry.Gui.Tooltip public int moodDropOnOwnerAttack = 1;
        @ConfigEntry.Gui.Tooltip public int favorabilityDropOnOwnerAttack = 1;
        @ConfigEntry.Gui.Tooltip public int favorabilityGainOnFeed = 1;
        @ConfigEntry.Gui.Tooltip public int moodGainOnFeed = 4;
        @ConfigEntry.Gui.Tooltip public int angerDuration = 600;
        @ConfigEntry.Gui.Tooltip public int moodAngryThreshold = 30;
        @ConfigEntry.Gui.Tooltip public int moodRecoveryTarget = 60;
        @ConfigEntry.Gui.Tooltip public int moodRecoveryInterval = 400;
        @ConfigEntry.Gui.Tooltip public int leashSpeechMoodDrop = 2;
        @ConfigEntry.Gui.Tooltip public int leashSpeechFavorabilityDrop = 2;
    // [zh] 好感度等级→固定最大血量见 MaidMood.getMaxHealthForLevel（Lv1=20 … Lv5=200），不纳入配置
    // [en] Favorability level → fixed max HP is defined in MaidMood.getMaxHealthForLevel (Lv1=20 ... Lv5=200), not configurable.
    // [ja] 好感度レベル→固定最大HPは MaidMood.getMaxHealthForLevel を参照（Lv1=20 … Lv5=200）。設定項目にはしません。
    }

    @ConfigEntry.Category("speech")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Speech speech = new Speech();

    /**
     * [zh] 对话气泡（台词）相关设置。
     * [en] Speech bubble (dialogue) settings.
     * [ja] セリフ吹き出し（台詞）関連の設定。
     */
    public static class Speech {
        @ConfigEntry.Gui.Tooltip public boolean enableSpeech = true;
        @ConfigEntry.Gui.Tooltip public int speechDuration = 80;
        @ConfigEntry.Gui.Tooltip public int speechInterval = 400;
        @ConfigEntry.Gui.Tooltip public int lowHealthThresholdPercent = 30;
    }

    @ConfigEntry.Category("item")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Item item = new Item();

    /**
     * [zh] 移植物品与方块相关设置（工资箱、女仆杖等）。
     * [en] Ported item and block settings (salary box, maid stick, etc.).
     * [ja] 移植アイテム・ブロック関連の設定（給料箱、メイドステッキなど）。
     */
    public static class Item {
        @ConfigEntry.Gui.Tooltip public int sugarBoxRange = 112;
        @ConfigEntry.Gui.Tooltip public int sugarBoxInterval = 200;
        @ConfigEntry.Gui.Tooltip public int maidStickRange = 10;
        @ConfigEntry.Gui.Tooltip public int maidStickTeleportRange = 128;
    }

    @ConfigEntry.Category("movement")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Movement movement = new Movement();

    public static class Movement {
        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip public float freedomSpeed = 0.65f;
        @ConfigEntry.Gui.Tooltip public float tracerSpeed = 0.65f;
        @ConfigEntry.Gui.Tooltip public int tracerHorizonRange = 4;
        @ConfigEntry.Gui.Tooltip public int tracerVerticalRange = 2;
        @ConfigEntry.Gui.Tooltip public float followSpeed = 1.0f;
        /**
         * [zh] 跟随模式的限定半径（格）：女仆被锁定在以玩家为圆心的该范围内。
         * [en] Follow-mode confinement radius (blocks): the maid is locked within this radius centered on the player.
         * [ja] 追従モードの制限半径（ブロック）。プレイヤーを中心としたこの半径内にメイドを拘束します。
         */
        @ConfigEntry.Gui.Tooltip public float followRange = 8.0f;
        @ConfigEntry.Gui.Tooltip public float followStartDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip public float followEndDistance = 5.0f;
        @ConfigEntry.Gui.Tooltip public float sprintSpeed = 1.2f;
        @ConfigEntry.Gui.Tooltip public float sprintStartDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip public float sprintEndDistance = 6.0f;
        @ConfigEntry.Gui.Tooltip public float teleportStartDistance = 16.0f;
        @ConfigEntry.Gui.Tooltip public int teleportWidth = 3;
        @ConfigEntry.Gui.Tooltip public int teleportHeight = 1;
        @ConfigEntry.Gui.Tooltip public boolean canTeleportOwnerForwards = false;
        @ConfigEntry.Gui.Tooltip public float ownerForwardRange = 4.0f;
        @ConfigEntry.Gui.Tooltip public int maxTryTeleportCount = 10;
        @ConfigEntry.Gui.Tooltip public float pickupItemSpeed = 1.0f;
        @ConfigEntry.Gui.Tooltip public float pickupItemRange = 12.0f;
        @ConfigEntry.Gui.Tooltip public int pickupItemFrequency = 20;
        @ConfigEntry.Gui.Tooltip public boolean pickupItemIgnoreOwnerFront = false;
        @ConfigEntry.Gui.Tooltip public List<String> pickupItemWhitelistTags = Lists.newArrayList();
        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip public float escapeSpeed = 1.2f;
        @ConfigEntry.Gui.Tooltip public int pathRecalcInterval = 10;
    }

    @ConfigEntry.Category("work")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Work work = new Work();

    public static class Work {
        @ConfigEntry.Gui.Tooltip public int defaultWorkItemSlotSize = 9;
        @ConfigEntry.Gui.Tooltip public float maxTargetRange = 16f;
        @ConfigEntry.Gui.Tooltip public float fencerAttackDistanceFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip public float fencerAttackRateFactor = 0.75f;
        @ConfigEntry.Gui.Tooltip public float archerShootDistanceFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip public float archerShootVelocityFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip public int torcherLightLevelThreshold = 7;
        /**
         * [zh] 统一方块工作范围（格）：方块工作/容器搜索/农夫搜索/自由行动共用的半径。
         * [en] Unified block work range (blocks): shared radius for block work, container search, farming search and free-move confinement.
         * [ja] 統一ブロック作業範囲（ブロック）。ブロック作業・コンテナ探索・農作業探索・自由行動が共用する半径です。
         */
        @ConfigEntry.Gui.Tooltip public float workRange = 8.0f;
        @ConfigEntry.Gui.Tooltip public int blockSearchMaxCount = 128;
        @ConfigEntry.Gui.Tooltip public int blockSearchBudgetPerTick = 10;
        @ConfigEntry.Gui.Tooltip public float torcherSearchMultiplier = 7.0f;
        @ConfigEntry.Gui.Tooltip public int ripperSearchInterval = 40;
    }

    @ConfigEntry.Category("contract")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Contract contract = new Contract();

    public static class Contract {
        @ConfigEntry.Gui.Tooltip public int consumeSalaryInterval = 24000;
        @ConfigEntry.Gui.Tooltip public int unpaidDaysLimit = 7;
        @ConfigEntry.Gui.Tooltip public int maxAutoSalaryReceiptSlotSize = 3;
        @ConfigEntry.Gui.Tooltip public int startAutoSalaryReceiptSlotThreshold = 1;
        @ConfigEntry.Gui.Tooltip public int maxMemorySalaryBoxPos = 4;
        @ConfigEntry.Gui.Tooltip public float memorySalaryBoxDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip public int memorySalaryBoxInterval = 20;
        @ConfigEntry.Gui.Tooltip public float searchSalaryBoxDistance = 16.0f;
        @ConfigEntry.Gui.Tooltip public int startIntervalOfAutoSalaryReceipt = 60;
        @ConfigEntry.Gui.Tooltip public int findPathIntervalOfAutoSalaryReceipt = 10;
        @ConfigEntry.Gui.Tooltip public int maxMoveTimeOnAutoSalaryReceipt = 200;
        @ConfigEntry.Gui.Tooltip public int maxMoveTimeAfterAutoSalaryReceipt = 400;
    }

    @ConfigEntry.Category("misc")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Misc misc = new Misc();

    public static class Misc {
        @ConfigEntry.Gui.Tooltip public boolean canPickupItem = true;
        @ConfigEntry.Gui.Tooltip public boolean canPickupExperienceOrb = true;
        @ConfigEntry.Gui.Tooltip public boolean canPickupItemByNoOwner = false;
        @ConfigEntry.Gui.Tooltip public float passivePickupRange = 2.0f;
        @ConfigEntry.Gui.Tooltip public boolean canMilking = false;
        @ConfigEntry.Gui.Tooltip public int playSoundInterval = 5;
        @ConfigEntry.Gui.Tooltip public float followAtHeldSalaryRange = 1.5f;
        @ConfigEntry.Gui.Tooltip public float followAtHeldEmployItemRange = 1.5f;
        @ConfigEntry.Gui.Tooltip public float stareAtSalaryRange = 4.0f;
        @ConfigEntry.Gui.Tooltip public float stareAtEmployItemRange = 4.0f;
        @ConfigEntry.Gui.Tooltip public int maxAccelerationStack = 8;
        @ConfigEntry.Gui.Tooltip public int accelerationTicksPerStack = 80;
        @ConfigEntry.Gui.Tooltip public int accelerationMultiple = 2;
        @ConfigEntry.Gui.Tooltip public int experienceBottleCost = 7;
    }

    @ConfigEntry.Category("target")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Target target = new Target();

    public static class Target {
        // [zh] 敌怪检测范围（实体属性。不作用于已存在的女仆，仅新生成时生效）
        // [en] Enemy detection range (entity attribute; only applied to newly spawned maids, not existing ones).
        // [ja] 敵検出範囲（Attribute。既存エンティティには適用されず、新規スポーン時のみ反映）
        @ConfigEntry.Gui.RequiresRestart @ConfigEntry.Gui.Tooltip public double followRange = 48.0;
        @ConfigEntry.Gui.Tooltip public int targetingInterval = 10;

        // [zh] 距离相关设置
        // [en] Distance-related settings
        // [ja] 距離関連設定
        // [zh] 警戒范围（敌怪检测与先制攻击范围）
        // [en] Alert range (enemy detection and preemptive attack range)
        // [ja] 警戒範囲（敵検出・先制攻撃範囲）
        @ConfigEntry.Gui.Tooltip public int alertRange = 16;
        // [zh] 战斗范围（实际战斗行动范围）
        // [en] Combat range (actual combat action range)
        // [ja] 戦闘範囲（実際の戦闘行動範囲）
        @ConfigEntry.Gui.Tooltip public int combatRange = 8;
        // [zh] 危险敌人回避距离（与苦力怕等保持距离）
        // [en] Distance to keep from dangerous enemies (e.g. creepers)
        // [ja] 危険敵回避距離（クリーパー等から距離を取る）
        @ConfigEntry.Gui.Tooltip public int dangerousAvoidDistance = 8;

        // [zh] 分散索敌设置（防止集火同一目标）
        // [en] Distributed targeting settings (prevent focus-firing one target)
        // [ja] 分散ターゲティング設定（集中攻撃を防ぐ）
        // [zh] 分散比率（女仆总数中同时攻击同一敌人的比例，如 0.5 = 50%）
        // [en] Distribution ratio (fraction of maids allowed to attack the same enemy, e.g. 0.5 = 50%)
        // [ja] 分散比率（メイドさんの50%が同じ敵を攻撃）
        @ConfigEntry.Gui.Tooltip public double distributionRatio = 0.5;
        // [zh] 每个目标的最大攻击者数量（防止集火）
        // [en] Max attackers per target (prevents focus fire)
        // [ja] 1体あたり最大攻撃者数（集中攻撃防止）
        @ConfigEntry.Gui.Tooltip public int maxAttackersPerTarget = 2;

        // [zh] 生命相关设置
        // [en] Health-related settings
        // [ja] 体力関連設定
        // [zh] 负伤判定阈值（生命低于 50% 视为负伤）
        // [en] Injury threshold (below 50% HP counts as injured)
        // [ja] 負傷判定閾値（体力50%以下で負傷扱い）
        @ConfigEntry.Gui.Tooltip public float injuredThreshold = 0.5f;
        // [zh] 攻击判定有效时长（200 tick = 10 秒）
        // [en] Valid ticks for attack judgment (200 ticks = 10 seconds)
        // [ja] 攻撃判定有効時間（10秒間、200tick）
        @ConfigEntry.Gui.Tooltip public int attackedByValidTicks = 200;
    }

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Client client = new Client();

    public static class Client {
        @ConfigEntry.Gui.Tooltip public boolean enableWaitPoseOnMoving = false;
        /**
         * [zh] 休息坐下时模型整体下压的高度（格），让臀部/腿贴地而非悬空；可按模型微调。
         * [en] How far the model is lowered (blocks) when sitting during rest so hips/legs touch the ground; tune per model.
         * [ja] 休息時に座る際のモデル沈み込み量（ブロック）。お尻・脚を地面に付けます。モデルに応じて調整。
         */
        @ConfigEntry.Gui.Tooltip public float restSitDrop = 0.25f;
        /**
         * [zh] 女仆手持糖的消耗动画时长（tick，20 tick = 1 秒）。
         * [en] Duration of the maid's sugar-holding consume animation (ticks; 20 ticks = 1 second).
         * [ja] メイドが砂糖を手に持つ消費アニメーションの時間（tick、20tick=1秒）。
         */
        @ConfigEntry.Gui.Tooltip public int sugarConsumeDuration = 12;
    }

    @ConfigEntry.Category("hunger")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Hunger hunger = new Hunger();

    /**
     * [zh] 女仆饥饿值（饱食度）相关设置。
     * [en] Maid hunger (satiety) settings.
     * [ja] メイドさんの空腹値（満腹度）関連の設定。
     */
    public static class Hunger {
        @ConfigEntry.Gui.Tooltip public int hungerDecayInterval = 80;
        @ConfigEntry.Gui.Tooltip public int hungerEatThreshold = 50;
        @ConfigEntry.Gui.Tooltip public boolean hungerEatWhenLowHealth = true;
        @ConfigEntry.Gui.Tooltip public float hungerLowHealthThreshold = 0.5f;
        @ConfigEntry.Gui.Tooltip public float hungerHealBoost = 2.0f;
        @ConfigEntry.Gui.Tooltip public int hungerFavorabilityGainOnEat = 0;
        @ConfigEntry.Gui.Tooltip public int hungerMoodGainOnEat = 2;
        @ConfigEntry.Gui.Tooltip public int hungerEatInterval = 40;
        @ConfigEntry.Gui.Tooltip public int hungerStarveInterval = 80;
        @ConfigEntry.Gui.Tooltip public float hungerStarveDamage = 1.0f;
        /**
         * [zh] 「无时无刻进食」档位的进食间隔（tick，600 = 30 秒）。
         * [en] Eating interval for the "always eat" mode (ticks; 600 = 30 seconds).
         * [ja] 「常に食べる」モードの食事間隔（tick、600=30秒）。
         */
        @ConfigEntry.Gui.Tooltip public int hungerAlwaysEatInterval = 600;
        /**
         * [zh] 饥饿值高于该百分比时，消耗多余饥饿恢复生命（1% 饥饿 = 1 生命）。
         * [en] When hunger is above this percentage, consume surplus hunger to restore HP (1% hunger = 1 HP).
         * [ja] 満腹度がこの割合を超えると余剰分を消費して体力を回復します（満腹度1%=体力1）。
         */
        @ConfigEntry.Gui.Tooltip public int hungerRecoveryThreshold = 80;
        /**
         * [zh] 女仆消耗糖时每次恢复的饱食度百分比（0-100）。
         * [en] Satiety percentage restored each time the maid consumes sugar (0-100).
         * [ja] メイドが砂糖を消費するたびに回復する満腹度の割合（0-100）。
         */
        @ConfigEntry.Gui.Tooltip public int sugarSatietyRestore = 4;
    }

    @ConfigEntry.Category("model")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Model model = new Model();

    /**
     * [zh] 小女仆模型资源加载器（LMML）的配置项，已并入本体配置。
     * [en] Config of the LittleMaidModelLoader (LMML), merged into this mod's config.
     * [ja] 小女仆モデルローダー（LMML）の設定項目を本モッドに統合したもの。
     */
    public static class Model {
        @ConfigEntry.Gui.Tooltip public float voiceVolume = 1.0f;
        @ConfigEntry.Gui.Tooltip public boolean enableAlpha = true;
        @ConfigEntry.Gui.Tooltip public boolean debugMode = false;
    }
}
