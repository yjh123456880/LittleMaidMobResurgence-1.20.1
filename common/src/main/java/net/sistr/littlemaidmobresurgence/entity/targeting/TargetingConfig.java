package net.sistr.littlemaidmobresurgence.entity.targeting;

import net.sistr.littlemaidmobresurgence.LMMRMod;

/** メイドさんターゲティングシステムの設定クラス 3段階優先度システム用のシンプル設定 */
public class TargetingConfig {

    // ========== 距離関連設定 ==========

    /** 警戒範囲（先制攻撃やターゲット検出の範囲） */
    public static int getAlertRange() {
        return LMMRMod.getConfig().target.alertRange;
    }

    /** 戦闘範囲（実際に戦闘を行う範囲） */
    public static int getCombatRange() {
        return LMMRMod.getConfig().target.combatRange;
    }

    /** 危険対象回避距離 */
    public static int getDangerousAvoidDistance() {
        return LMMRMod.getConfig().target.dangerousAvoidDistance;
    }

    // ========== 分散ターゲティング設定 ==========

    /** 分散ターゲティングの比率（メイドさん数の50%） */
    public static double getDistributionRatio() {
        return LMMRMod.getConfig().target.distributionRatio;
    }

    /** 一体あたりの最大攻撃者数 */
    public static int getMaxAttackersPerTarget() {
        return LMMRMod.getConfig().target.maxAttackersPerTarget;
    }

    // ========== 体力関連設定 ==========

    /** 負傷判定の体力閾値（50%） */
    public static float getInjuredThreshold() {
        return LMMRMod.getConfig().target.injuredThreshold;
    }

    /** 攻撃判定の有効tick数 */
    public static int getAttackedByValidTicks() {
        return LMMRMod.getConfig().target.attackedByValidTicks;
    }

    /** ターゲッティング再計算間隔(tick) */
    public static int getTargetingInterval() {
        return LMMRMod.getConfig().target.targetingInterval;
    }
}
