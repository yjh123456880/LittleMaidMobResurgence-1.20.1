package net.sistr.littlemaidmobresurgence.entity;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.sistr.littlemaidmobresurgence.LMMRMod;

/**
 * [zh] 管理女仆的文本气泡（台词）：按情景（被殴/低血量/工作/喂食/饥饿/契约/天气等）与心情值/好感度
 *     选择台词键，并通过数据追踪器同步到客户端显示。
 * [en] Manages the maid's speech bubbles: picks speech keys by situation (hurt/low HP/work/fed/hungry/contract/weather etc.)
 *     plus mood/favorability, and displays them on the client via the data tracker.
 * [ja] メイドさんのテキストバブル（セリフ）を管理します。
 *     状況（被殴・低HP・仕事・餌・空腹・契約・天候など）と機嫌値・好感度に応じてセリフキーを選び、
 *     データトラッカー経由でクライアントに表示させます。
 */
public class MaidSpeech {

    private static final List<String> HURT_ANGRY =
            List.of(
                    "speech.littlemaidmobresurgence.hurt.angry.1",
                    "speech.littlemaidmobresurgence.hurt.angry.2",
                    "speech.littlemaidmobresurgence.hurt.angry.3",
                    "speech.littlemaidmobresurgence.hurt.angry.4",
                    "speech.littlemaidmobresurgence.hurt.angry.5",
                    "speech.littlemaidmobresurgence.hurt.angry.6",
                    "speech.littlemaidmobresurgence.hurt.angry.7",
                    "speech.littlemaidmobresurgence.hurt.angry.8",
                    "speech.littlemaidmobresurgence.hurt.angry.9",
                    "speech.littlemaidmobresurgence.hurt.angry.10",
                    "speech.littlemaidmobresurgence.hurt.angry.11",
                    "speech.littlemaidmobresurgence.hurt.angry.12");
    private static final List<String> HURT_SAD =
            List.of(
                    "speech.littlemaidmobresurgence.hurt.sad.1",
                    "speech.littlemaidmobresurgence.hurt.sad.2",
                    "speech.littlemaidmobresurgence.hurt.sad.3",
                    "speech.littlemaidmobresurgence.hurt.sad.4",
                    "speech.littlemaidmobresurgence.hurt.sad.5",
                    "speech.littlemaidmobresurgence.hurt.sad.6",
                    "speech.littlemaidmobresurgence.hurt.sad.7",
                    "speech.littlemaidmobresurgence.hurt.sad.8",
                    "speech.littlemaidmobresurgence.hurt.sad.9",
                    "speech.littlemaidmobresurgence.hurt.sad.10",
                    "speech.littlemaidmobresurgence.hurt.sad.11",
                    "speech.littlemaidmobresurgence.hurt.sad.12");
    private static final List<String> HURT_CRY =
            List.of(
                    "speech.littlemaidmobresurgence.hurt.cry.1",
                    "speech.littlemaidmobresurgence.hurt.cry.2",
                    "speech.littlemaidmobresurgence.hurt.cry.3",
                    "speech.littlemaidmobresurgence.hurt.cry.4",
                    "speech.littlemaidmobresurgence.hurt.cry.5",
                    "speech.littlemaidmobresurgence.hurt.cry.6",
                    "speech.littlemaidmobresurgence.hurt.cry.7",
                    "speech.littlemaidmobresurgence.hurt.cry.8",
                    "speech.littlemaidmobresurgence.hurt.cry.9",
                    "speech.littlemaidmobresurgence.hurt.cry.10",
                    "speech.littlemaidmobresurgence.hurt.cry.11",
                    "speech.littlemaidmobresurgence.hurt.cry.12");
    private static final List<String> FED_LOW =
            List.of(
                    "speech.littlemaidmobresurgence.fed.low.1",
                    "speech.littlemaidmobresurgence.fed.low.2",
                    "speech.littlemaidmobresurgence.fed.low.3",
                    "speech.littlemaidmobresurgence.fed.low.4",
                    "speech.littlemaidmobresurgence.fed.low.5",
                    "speech.littlemaidmobresurgence.fed.low.6",
                    "speech.littlemaidmobresurgence.fed.low.7",
                    "speech.littlemaidmobresurgence.fed.low.8",
                    "speech.littlemaidmobresurgence.fed.low.9",
                    "speech.littlemaidmobresurgence.fed.low.10",
                    "speech.littlemaidmobresurgence.fed.low.11",
                    "speech.littlemaidmobresurgence.fed.low.12");
    private static final List<String> FED_MID =
            List.of(
                    "speech.littlemaidmobresurgence.fed.mid.1",
                    "speech.littlemaidmobresurgence.fed.mid.2",
                    "speech.littlemaidmobresurgence.fed.mid.3",
                    "speech.littlemaidmobresurgence.fed.mid.4",
                    "speech.littlemaidmobresurgence.fed.mid.5",
                    "speech.littlemaidmobresurgence.fed.mid.6",
                    "speech.littlemaidmobresurgence.fed.mid.7",
                    "speech.littlemaidmobresurgence.fed.mid.8",
                    "speech.littlemaidmobresurgence.fed.mid.9",
                    "speech.littlemaidmobresurgence.fed.mid.10",
                    "speech.littlemaidmobresurgence.fed.mid.11",
                    "speech.littlemaidmobresurgence.fed.mid.12");
    private static final List<String> FED_HIGH =
            List.of(
                    "speech.littlemaidmobresurgence.fed.high.1",
                    "speech.littlemaidmobresurgence.fed.high.2",
                    "speech.littlemaidmobresurgence.fed.high.3",
                    "speech.littlemaidmobresurgence.fed.high.4",
                    "speech.littlemaidmobresurgence.fed.high.5",
                    "speech.littlemaidmobresurgence.fed.high.6",
                    "speech.littlemaidmobresurgence.fed.high.7",
                    "speech.littlemaidmobresurgence.fed.high.8",
                    "speech.littlemaidmobresurgence.fed.high.9",
                    "speech.littlemaidmobresurgence.fed.high.10",
                    "speech.littlemaidmobresurgence.fed.high.11",
                    "speech.littlemaidmobresurgence.fed.high.12");
    private static final List<String> LOW_HP =
            List.of(
                    "speech.littlemaidmobresurgence.lowhp.1",
                    "speech.littlemaidmobresurgence.lowhp.2",
                    "speech.littlemaidmobresurgence.lowhp.3",
                    "speech.littlemaidmobresurgence.lowhp.4",
                    "speech.littlemaidmobresurgence.lowhp.5",
                    "speech.littlemaidmobresurgence.lowhp.6",
                    "speech.littlemaidmobresurgence.lowhp.7",
                    "speech.littlemaidmobresurgence.lowhp.8",
                    "speech.littlemaidmobresurgence.lowhp.9",
                    "speech.littlemaidmobresurgence.lowhp.10");
    private static final List<String> WORK =
            List.of(
                    "speech.littlemaidmobresurgence.work.1",
                    "speech.littlemaidmobresurgence.work.2",
                    "speech.littlemaidmobresurgence.work.3",
                    "speech.littlemaidmobresurgence.work.4",
                    "speech.littlemaidmobresurgence.work.5",
                    "speech.littlemaidmobresurgence.work.6",
                    "speech.littlemaidmobresurgence.work.7",
                    "speech.littlemaidmobresurgence.work.8",
                    "speech.littlemaidmobresurgence.work.9",
                    "speech.littlemaidmobresurgence.work.10",
                    "speech.littlemaidmobresurgence.work.11",
                    "speech.littlemaidmobresurgence.work.12",
                    "speech.littlemaidmobresurgence.work.13",
                    "speech.littlemaidmobresurgence.work.14");
    private static final List<String> IDLE_LOW =
            List.of(
                    "speech.littlemaidmobresurgence.idle.low.1",
                    "speech.littlemaidmobresurgence.idle.low.2",
                    "speech.littlemaidmobresurgence.idle.low.3",
                    "speech.littlemaidmobresurgence.idle.low.4",
                    "speech.littlemaidmobresurgence.idle.low.5",
                    "speech.littlemaidmobresurgence.idle.low.6",
                    "speech.littlemaidmobresurgence.idle.low.7");
    private static final List<String> IDLE_MID =
            List.of(
                    "speech.littlemaidmobresurgence.idle.mid.1",
                    "speech.littlemaidmobresurgence.idle.mid.2",
                    "speech.littlemaidmobresurgence.idle.mid.3",
                    "speech.littlemaidmobresurgence.idle.mid.4",
                    "speech.littlemaidmobresurgence.idle.mid.5",
                    "speech.littlemaidmobresurgence.idle.mid.6",
                    "speech.littlemaidmobresurgence.idle.mid.7",
                    "speech.littlemaidmobresurgence.idle.mid.8");
    private static final List<String> IDLE_HIGH =
            List.of(
                    "speech.littlemaidmobresurgence.idle.high.1",
                    "speech.littlemaidmobresurgence.idle.high.2",
                    "speech.littlemaidmobresurgence.idle.high.3",
                    "speech.littlemaidmobresurgence.idle.high.4",
                    "speech.littlemaidmobresurgence.idle.high.5",
                    "speech.littlemaidmobresurgence.idle.high.6",
                    "speech.littlemaidmobresurgence.idle.high.7",
                    "speech.littlemaidmobresurgence.idle.high.8");
    private static final List<String> HUNGRY =
            List.of(
                    "speech.littlemaidmobresurgence.hungry.1",
                    "speech.littlemaidmobresurgence.hungry.2",
                    "speech.littlemaidmobresurgence.hungry.3",
                    "speech.littlemaidmobresurgence.hungry.4",
                    "speech.littlemaidmobresurgence.hungry.5",
                    "speech.littlemaidmobresurgence.hungry.6",
                    "speech.littlemaidmobresurgence.hungry.7",
                    "speech.littlemaidmobresurgence.hungry.8",
                    "speech.littlemaidmobresurgence.hungry.9",
                    "speech.littlemaidmobresurgence.hungry.10",
                    "speech.littlemaidmobresurgence.hungry.11",
                    "speech.littlemaidmobresurgence.hungry.12");
    private static final List<String> CONTRACT =
            List.of(
                    "speech.littlemaidmobresurgence.contract.1",
                    "speech.littlemaidmobresurgence.contract.2",
                    "speech.littlemaidmobresurgence.contract.3",
                    "speech.littlemaidmobresurgence.contract.4",
                    "speech.littlemaidmobresurgence.contract.5",
                    "speech.littlemaidmobresurgence.contract.6",
                    "speech.littlemaidmobresurgence.contract.7");
    private static final List<String> STRIKE =
            List.of(
                    "speech.littlemaidmobresurgence.strike.1",
                    "speech.littlemaidmobresurgence.strike.2",
                    "speech.littlemaidmobresurgence.strike.3",
                    "speech.littlemaidmobresurgence.strike.4",
                    "speech.littlemaidmobresurgence.strike.5",
                    "speech.littlemaidmobresurgence.strike.6",
                    "speech.littlemaidmobresurgence.strike.7");
    private static final List<String> THANKS =
            List.of(
                    "speech.littlemaidmobresurgence.thanks.1",
                    "speech.littlemaidmobresurgence.thanks.2",
                    "speech.littlemaidmobresurgence.thanks.3",
                    "speech.littlemaidmobresurgence.thanks.4",
                    "speech.littlemaidmobresurgence.thanks.5",
                    "speech.littlemaidmobresurgence.thanks.6",
                    "speech.littlemaidmobresurgence.thanks.7");
    private static final List<String> NIGHT =
            List.of(
                    "speech.littlemaidmobresurgence.night.1",
                    "speech.littlemaidmobresurgence.night.2",
                    "speech.littlemaidmobresurgence.night.3",
                    "speech.littlemaidmobresurgence.night.4");
    private static final List<String> RAIN =
            List.of(
                    "speech.littlemaidmobresurgence.rain.1",
                    "speech.littlemaidmobresurgence.rain.2",
                    "speech.littlemaidmobresurgence.rain.3",
                    "speech.littlemaidmobresurgence.rain.4");
    private static final List<String> SNOW =
            List.of(
                    "speech.littlemaidmobresurgence.snow.1",
                    "speech.littlemaidmobresurgence.snow.2",
                    "speech.littlemaidmobresurgence.snow.3",
                    "speech.littlemaidmobresurgence.snow.4");
    private static final List<String> DEATH =
            List.of(
                    "speech.littlemaidmobresurgence.death.1",
                    "speech.littlemaidmobresurgence.death.2",
                    "speech.littlemaidmobresurgence.death.3",
                    "speech.littlemaidmobresurgence.death.4",
                    "speech.littlemaidmobresurgence.death.5");
    private static final List<String> MORNING =
            List.of(
                    "speech.littlemaidmobresurgence.morning.1",
                    "speech.littlemaidmobresurgence.morning.2",
                    "speech.littlemaidmobresurgence.morning.3",
                    "speech.littlemaidmobresurgence.morning.4",
                    "speech.littlemaidmobresurgence.morning.5");
    private static final List<String> ANGRY =
            List.of(
                    "speech.littlemaidmobresurgence.angry.1",
                    "speech.littlemaidmobresurgence.angry.2",
                    "speech.littlemaidmobresurgence.angry.3",
                    "speech.littlemaidmobresurgence.angry.4",
                    "speech.littlemaidmobresurgence.angry.5");
    private static final List<String> RESURRECT =
            List.of(
                    "speech.littlemaidmobresurgence.resurrect.1",
                    "speech.littlemaidmobresurgence.resurrect.2",
                    "speech.littlemaidmobresurgence.resurrect.3");
    /** 自己自动进食时的台词（泛指食物，不指向具体物品，与玩家喂食的 onFed 区分） */
    private static final List<String> SELF_EAT =
            List.of(
                    "speech.littlemaidmobresurgence.self_eat.1",
                    "speech.littlemaidmobresurgence.self_eat.2",
                    "speech.littlemaidmobresurgence.self_eat.3",
                    "speech.littlemaidmobresurgence.self_eat.4",
                    "speech.littlemaidmobresurgence.self_eat.5",
                    "speech.littlemaidmobresurgence.self_eat.6",
                    "speech.littlemaidmobresurgence.self_eat.7",
                    "speech.littlemaidmobresurgence.self_eat.8",
                    "speech.littlemaidmobresurgence.self_eat.9",
                    "speech.littlemaidmobresurgence.self_eat.10");
    private static final List<String> REBELLION =
            List.of(
                    "speech.littlemaidmobresurgence.rebellion.1",
                    "speech.littlemaidmobresurgence.rebellion.2",
                    "speech.littlemaidmobresurgence.rebellion.3",
                    "speech.littlemaidmobresurgence.rebellion.4",
                    "speech.littlemaidmobresurgence.rebellion.5",
                    "speech.littlemaidmobresurgence.rebellion.6");
    private static final List<String> REBELLION_ATTACK =
            List.of(
                    "speech.littlemaidmobresurgence.rebellion_attack.1",
                    "speech.littlemaidmobresurgence.rebellion_attack.2",
                    "speech.littlemaidmobresurgence.rebellion_attack.3");
    /** 避战（战斗模式低血 + 附近有敌，主动远离）进入时的台词。 */
    private static final List<String> EVADE =
            List.of(
                    "speech.littlemaidmobresurgence.evade.1",
                    "speech.littlemaidmobresurgence.evade.2",
                    "speech.littlemaidmobresurgence.evade.3",
                    "speech.littlemaidmobresurgence.evade.4");
    /** 休息（低血 + 附近无敌，坐下恢复）进入时的台词。 */
    private static final List<String> REST =
            List.of(
                    "speech.littlemaidmobresurgence.rest.1",
                    "speech.littlemaidmobresurgence.rest.2",
                    "speech.littlemaidmobresurgence.rest.3",
                    "speech.littlemaidmobresurgence.rest.4");
    /** 拴绳状态下各情绪的专用文本（每情绪 3 句；反叛除外）。 */
    private static final List<String> LEASHED_ANGRY =
            List.of(
                    "speech.littlemaidmobresurgence.leashed.angry.1",
                    "speech.littlemaidmobresurgence.leashed.angry.2",
                    "speech.littlemaidmobresurgence.leashed.angry.3");
    private static final List<String> LEASHED_SAD =
            List.of(
                    "speech.littlemaidmobresurgence.leashed.sad.1",
                    "speech.littlemaidmobresurgence.leashed.sad.2",
                    "speech.littlemaidmobresurgence.leashed.sad.3");
    private static final List<String> LEASHED_CALM =
            List.of(
                    "speech.littlemaidmobresurgence.leashed.calm.1",
                    "speech.littlemaidmobresurgence.leashed.calm.2",
                    "speech.littlemaidmobresurgence.leashed.calm.3");
    private static final List<String> LEASHED_HAPPY =
            List.of(
                    "speech.littlemaidmobresurgence.leashed.happy.1",
                    "speech.littlemaidmobresurgence.leashed.happy.2",
                    "speech.littlemaidmobresurgence.leashed.happy.3");

    private MaidSpeech() {}

    private static String pick(List<String> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private static boolean enabled(LittleMaidEntity maid) {
        return LMMRMod.getConfig().speech.enableSpeech && !maid.getWorld().isClient;
    }

    /**
     * 情绪隔离：反叛/愤怒期间，所有主动台词都锁定为对应情绪的台词，
     * 防止出现"气鼓鼓想揍你→转头又说和你在一起很开心"的情绪错乱。
     */
    private static String pickWithEmotion(LittleMaidEntity maid, List<String> normalLines) {
        // 纯情绪值驱动：反叛最高优先，愤怒其次，其余按普通台词（心情80+ 绝不出愤怒台词）
        if (maid.isRebellious()) {
            return pick(REBELLION);
        }
        if (maid.getEmotion() == MaidMood.Emotion.ANGRY) {
            return pick(ANGRY);
        }
        return pick(normalLines);
    }

    /** 被殴られた時のセリフ。心情で変化。 */
    public static void onHurt(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        // 严格按当前情绪播放对应文本：反叛→反叛台词；愤怒→愤怒；悲伤→哭泣；平静/开心→难过
        if (maid.isRebellious()) {
            maid.setSpeech(pick(REBELLION_ATTACK));
            return;
        }
        switch (maid.getEmotion()) {
            case ANGRY -> maid.setSpeech(pick(HURT_ANGRY));
            case SAD -> maid.setSpeech(pick(HURT_CRY));
            default -> maid.setSpeech(pick(HURT_SAD));
        }
    }

    /** 餌を与えられた時のセリフ。好感度で変化。 */
    public static void onFed(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        int level = maid.maidMood.getFavorabilityLevel();
        String key = level <= 3 ? pick(FED_LOW) : level <= 6 ? pick(FED_MID) : pick(FED_HIGH);
        maid.setSpeech(key);
    }

    /** 自己自动进食时的台词。泛指吃饱/恢复，不指向具体食物。 */
    public static void onSelfEat(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pick(SELF_EAT));
    }

    /** 低HP時のセリフ。 */
    public static void onLowHealth(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, LOW_HP));
    }

    /** 仕事中のセリフ。 */
    public static void onWork(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, WORK));
    }

    /** 待機中のセリフ。心情・好感度で変化。 */
    public static void onIdle(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        // 反叛/愤怒期间锁定情绪台词（纯情绪值驱动）
        if (maid.isRebellious()) {
            maid.setSpeech(pick(REBELLION));
            return;
        }
        if (maid.getEmotion() == MaidMood.Emotion.ANGRY) {
            maid.setSpeech(pick(ANGRY));
            return;
        }
        int level = maid.maidMood.getFavorabilityLevel();
        int mood = maid.maidMood.getMood();
        String key = mood <= 30 ? pick(IDLE_LOW) : level <= 4 ? pick(IDLE_MID) : pick(IDLE_HIGH);
        maid.setSpeech(key);
    }

    /** 空腹時のセリフ。 */
    public static void onHungry(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, HUNGRY));
    }

    /** 契約時のセリフ。 */
    public static void onContract(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pick(CONTRACT));
    }

    /** ストライキ時のセリフ。 */
    public static void onStrike(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pick(STRIKE));
    }

    /** 給料受取時のセリフ。 */
    public static void onThanks(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, THANKS));
    }

    /** 夜のセリフ。 */
    public static void onNight(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, NIGHT));
    }

    /** 雨のセリフ。 */
    public static void onRain(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, RAIN));
    }

    /** 雪のセリフ。 */
    public static void onSnow(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, SNOW));
    }

    /** 死の間際のセリフ。 */
    public static void onDeath(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, DEATH));
    }

    /** 朝のセリフ。 */
    public static void onMorning(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pickWithEmotion(maid, MORNING));
    }

    /** 復活時のセリフ。 */
    public static void onResurrect(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pick(RESURRECT));
    }

    /** 反叛开始时的台词。 */
    public static void onRebellion(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pick(REBELLION));
    }

    /** 反叛攻击时的台词。 */
    public static void onRebellionAttack(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pick(REBELLION_ATTACK));
    }

    /** 进入避战状态时的台词（主动远离敌人）。 */
    public static void onEvade(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pick(EVADE));
    }

    /** 进入休息状态时的台词（坐下恢复）。 */
    public static void onRest(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        maid.setSpeech(pick(REST));
    }

    /** 拴绳状态下的台词：按当前情绪选文本（反叛不在此列，由调用方排除）。 */
    public static void onLeashed(LittleMaidEntity maid) {
        if (!enabled(maid)) return;
        String key =
                switch (maid.getEmotion()) {
                    case ANGRY -> pick(LEASHED_ANGRY);
                    case SAD -> pick(LEASHED_SAD);
                    case CALM -> pick(LEASHED_CALM);
                    default -> pick(LEASHED_HAPPY);
                };
        maid.setLeashedSpeech(key);
    }
}
