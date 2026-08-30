package net.sistr.littlemaidmobresurgence.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import org.jetbrains.annotations.Nullable;

/**
 * [zh] 管理女仆的心情值与好感度。
 *     心情值（mood）降低会引发愤怒、反叛或逃跑；好感度（favorability）为等级化亲密度，喂食上升、被主人攻击下降。
 * [en] Manages the maid's mood and favorability.
 *     Lower mood causes anger, rebellion or fleeing; favorability is a leveled affection that rises when fed and drops when attacked by the owner.
 * [ja] メイドさんの心情値・好感度を管理します。
 *     心情値(mood)が下がると怒り、抵抗・逃走します。好感度(favorability)はレベル化した親愛度で、餌を与えると上がり、ご主人様に攻撃されると下がります。
 */
public class MaidMood {
    public static final int MAX_MOOD = 100;
    /**
     * [zh] 满级好感度（5 级累计阈值 20/60/140/300，每级所需好感度逐级翻倍）。
     * [en] Max favorability (level thresholds 20/60/140/300, doubling per level).
     * [ja] 最大好感度（レベル閾値 20/60/140/300、レベルごとに倍増）。
     */
    public static final int MAX_FAVORABILITY = 300;
    /**
     * [zh] 客户端显示用的「愤怒」阈值。
     * [en] "Angry" threshold used for client display.
     * [ja] クライアント表示用の「怒り」閾値。
     */
    public static final int ANGRY_THRESHOLD = 30;

    private int mood = 70;
    private int favorability = 0;
    private int angerTicks = 0;
    private int recoveryCoolDown = 0;
    @Nullable private UUID angerTargetUuid;

    public int getMood() {
        return mood;
    }

    public int getFavorability() {
        return favorability;
    }

    /**
     * [zh] 好感度等级（1-5）。逐级翻倍：Lv1→2 需 20、Lv2→3 需 40、Lv3→4 需 80、Lv4→5 需 160（累计 20/60/140/300）。
     * [en] Favorability level (1-5), thresholds double each level: 20/40/80/160 (cumulative 20/60/140/300).
     * [ja] 好感度レベル（1〜5）。レベルごとに倍増：20/40/80/160（累計 20/60/140/300）。
     */
    public int getFavorabilityLevel() {
        return levelOf(this.favorability);
    }

    /**
     * [zh] 由好感度数值换算等级（客户端可直接使用 DataTracker 同步的数值）。
     * [en] Converts a favorability value to a level (client-safe: works on DataTracker-synced values).
     * [ja] 好感度の数値からレベルを算出します（DataTracker 同期値をそのまま使えるクライアント安全実装）。
     */
    public static int levelOf(int favorability) {
        if (favorability >= 300) {
            return 5;
        }
        if (favorability >= 140) {
            return 4;
        }
        if (favorability >= 60) {
            return 3;
        }
        if (favorability >= 20) {
            return 2;
        }
        return 1;
    }

    /**
     * [zh] 达到指定等级所需的累计好感度（level 1-5；Lv1=0，越界时钳制到边界）。
     * [en] Cumulative favorability required to reach a level (1-5; Lv1=0, out-of-range is clamped).
     * [ja] 指定レベルに到達するのに必要な累計好感度（1〜5；Lv1=0、範囲外はクランプ）。
     */
    public static int getThresholdForLevel(int level) {
        return switch (MathHelper.clamp(level, 1, 5)) {
            case 1 -> 0;
            case 2 -> 20;
            case 3 -> 60;
            case 4 -> 140;
            default -> 300;
        };
    }

    /**
     * [zh] 每个好感度等级固定的最大生命值：Lv1=20、Lv2=40、Lv3=80、Lv4=140、Lv5=200。
     * [en] Fixed max HP per favorability level: Lv1=20, Lv2=40, Lv3=80, Lv4=140, Lv5=200.
     * [ja] 好感度レベルごとの固定最大体力：Lv1=20、Lv2=40、Lv3=80、Lv4=140、Lv5=200。
     */
    public static int getMaxHealthForLevel(int level) {
        return switch (level) {
            case 1 -> 20;
            case 2 -> 40;
            case 3 -> 80;
            case 4 -> 140;
            default -> 200;
        };
    }

    public int getAngerTicks() {
        return angerTicks;
    }

    /**
     * [zh] 女仆情绪状态（用于 GUI 情绪面板展示）。
     * [en] Maid emotion states (shown in the GUI mood panel).
     * [ja] メイドの感情状態（GUI の機嫌パネル表示用）。
     */
    public enum Emotion {
        REBELLION,
        ANGRY,
        SAD,
        CALM,
        HAPPY
    }

    // [zh] 情绪区间边界（纯心情值映射，互不重叠覆盖 0-100）：ANGRY=[0,30] SAD=[31,50] CALM=[51,70] HAPPY=[71,100]
    // [en] Emotion range boundaries (pure mood mapping, non-overlapping, covering 0-100): ANGRY=[0,30] SAD=[31,50] CALM=[51,70] HAPPY=[71,100]
    // [ja] 感情区間の境界（機嫌値のみで判定、0-100 を重複なく網羅）：ANGRY=[0,30] SAD=[31,50] CALM=[51,70] HAPPY=[71,100]
    public static final int EMOTION_ANGRY_MAX = 30;
    public static final int EMOTION_SAD_MAX = 50;
    public static final int EMOTION_CALM_MAX = 70;

    /**
     * [zh] 纯心情值区间映射：区间互斥（`<=` 边界），覆盖 0-100 无重叠无空隙；服务端与客户端共用同一逻辑。
     * [en] Pure mood-range mapping: mutually exclusive (`<=` boundaries), covering 0-100 with no gaps; shared by server and client.
     * [ja] 機嫌値のみの区間マッピング。区間は排他（`<=` 境界）で 0-100 を隙間なく網羅。サーバー・クライアント共通。
     */
    public static Emotion getEmotionByMood(int mood) {
        if (mood <= EMOTION_ANGRY_MAX) {
            return Emotion.ANGRY;
        }
        if (mood <= EMOTION_SAD_MAX) {
            return Emotion.SAD;
        }
        if (mood <= EMOTION_CALM_MAX) {
            return Emotion.CALM;
        }
        return Emotion.HAPPY;
    }

    /**
     * [zh] 行为判定：心情是否处于愤怒阈值以下（纯心情驱动，与情绪展示强相关）。
     * [en] Behavior check: whether mood is at/below the angry threshold (purely mood-driven, closely tied to displayed emotion).
     * [ja] 行動判定：機嫌が怒り閾値以下かどうか（機嫌値のみで判定、感情表示と強く連動）。
     */
    public boolean isAngry() {
        return mood <= LMMRMod.getConfig().mood.moodAngryThreshold;
    }

    public Optional<UUID> getAngerTargetUuid() {
        return Optional.ofNullable(angerTargetUuid);
    }

    /**
     * [zh] 被攻击时的反应：心情下降并进入怒气状态；若攻击者是主人，好感度也下降。
     * [en] Reaction when attacked: mood drops and anger starts; if the attacker is the owner, favorability also drops.
     * [ja] 攻撃された時の反応。機嫌が下がり怒り状態になります。ご主人様から攻撃された場合は好感度も下がります。
     */
    public void onAttackedBy(@Nullable UUID attackerUuid, boolean isOwner) {
        var config = LMMRMod.getConfig().mood;
        this.mood = Math.max(0, mood - config.moodDropOnAttack);
        this.angerTicks = config.angerDuration;
        this.angerTargetUuid = attackerUuid;
        if (isOwner) {
            // [zh] 满级好感度（MAX_FAVORABILITY）后好感度不再变化（被攻击不下降、喂食封顶）
            // [en] At max favorability (MAX_FAVORABILITY) it no longer changes (no drop on attack, feeding is capped).
            // [ja] 最大好感度（MAX_FAVORABILITY）到達後は変化しません（被弾でも下がらず、餌やりも上限で停止）。
            if (this.favorability < MAX_FAVORABILITY) {
                this.favorability = Math.max(0, favorability - config.favorabilityDropOnOwnerAttack);
            }
            this.mood = Math.max(0, mood - config.moodDropOnOwnerAttack);
        }
    }

    /**
     * [zh] 被喂食（蛋糕/糖等）时的反应：好感度与心情上升，怒气清零。
     * [en] Reaction when fed (cake/sugar etc.): favorability and mood rise, anger is cleared.
     * [ja] 餌（ケーキ/砂糖など）を与えられた時の反応。好感度と機嫌が上がり、怒りが解除されます。
     */
    public void onFed() {
        var config = LMMRMod.getConfig().mood;
        this.favorability =
                Math.min(MAX_FAVORABILITY, favorability + config.favorabilityGainOnFeed);
        this.mood = Math.min(MAX_MOOD, mood + config.moodGainOnFeed);
        this.angerTicks = 0;
        this.angerTargetUuid = null;
    }

    /**
     * [zh] 直接增减好感度（自动进食等使用），结果钳制在 0 与上限之间。
     * [en] Directly adds favorability (e.g. auto-eating), clamped between 0 and the cap.
     * [ja] 好感度を直接加算します（自動食事などに使用）。0〜上限にクランプ。
     */
    public void addFavorability(int amount) {
        this.favorability = Math.max(0, Math.min(MAX_FAVORABILITY, favorability + amount));
    }

    /**
     * [zh] 直接增减心情值（自动进食等使用），结果钳制在 0 与上限之间。
     * [en] Directly adds mood (e.g. auto-eating), clamped between 0 and the cap.
     * [ja] 機嫌値を直接加算します（自動食事などに使用）。0〜上限にクランプ。
     */
    public void addMood(int amount) {
        this.mood = Math.max(0, Math.min(MAX_MOOD, mood + amount));
    }

    /**
     * [zh] 把心情值至少抬到 min，并清空怒气目标/怒气计时（用于纪念品复活，防止残留反叛/愤怒）。
     * [en] Raises mood to at least min and clears anger target/timer (used on souvenir revival to prevent leftover rebellion/anger).
     * [ja] 機嫌を最低 min まで引き上げ、怒り目標・怒りタイマーをクリアします（記念品による復活時の残留反乱・怒り防止）。
     */
    public void ensureMoodAtLeast(int min) {
        this.mood = Math.max(min, Math.min(MAX_MOOD, mood));
        this.angerTicks = 0;
        this.angerTargetUuid = null;
    }

    /**
     * [zh] 每 tick 调用：怒气随时间消退，心情缓慢恢复至目标值。
     * [en] Called every tick: anger fades over time and mood slowly recovers toward the target.
     * [ja] 毎tick呼びます。怒りを時間で解消し、機嫌を目標値までゆっくり回復させます。
     */
    public void tick() {
        var config = LMMRMod.getConfig().mood;
        if (angerTicks > 0) {
            angerTicks--;
            if (angerTicks == 0) {
                angerTargetUuid = null;
            }
        }
        if (mood < config.moodRecoveryTarget && angerTicks <= 0) {
            if (recoveryCoolDown <= 0) {
                mood = Math.min(config.moodRecoveryTarget, mood + 1);
                recoveryCoolDown = config.moodRecoveryInterval;
            } else {
                recoveryCoolDown--;
            }
        }
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putInt("Mood", mood);
        nbt.putInt("Favorability", favorability);
        nbt.putInt("AngerTicks", angerTicks);
        if (angerTargetUuid != null) {
            nbt.putUuid("AngerTarget", angerTargetUuid);
        }
    }

    public void readNbt(NbtCompound nbt) {
        // [zh] 旧版本存档无 Mood 字段时保持默认值 70（getInt 缺省返回 0 会让老女仆以愤怒状态加载）
        // [en] Keep the default 70 when old saves lack the Mood field (a missing getInt default of 0 would load old maids as angry).
        // [ja] 旧バージョンのセーブに Mood フィールドが無い場合は既定値 70 を維持（欠落時の getInt 既定 0 だと古いメイドが怒り状態でロードされるため）
        mood = nbt.contains("Mood") ? MathHelper.clamp(nbt.getInt("Mood"), 0, MAX_MOOD) : mood;
        favorability = MathHelper.clamp(nbt.getInt("Favorability"), 0, MAX_FAVORABILITY);
        angerTicks = Math.max(0, nbt.getInt("AngerTicks"));
        if (nbt.containsUuid("AngerTarget")) {
            angerTargetUuid = nbt.getUuid("AngerTarget");
        } else {
            angerTargetUuid = null;
        }
    }
}
