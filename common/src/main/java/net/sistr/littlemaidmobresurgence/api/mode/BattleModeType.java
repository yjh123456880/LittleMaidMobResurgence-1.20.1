package net.sistr.littlemaidmobresurgence.api.mode;

/** 战斗模式类型（顶层枚举，避免嵌套枚举在 Forge 环境下的类加载问题）。 */
public enum BattleModeType {
    NONE,
    SWORD,
    BOW
}
