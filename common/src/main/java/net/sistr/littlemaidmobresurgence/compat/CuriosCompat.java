package net.sistr.littlemaidmobresurgence.compat;

import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.sistr.littlemaidmobresurgence.entity.CuriosScreenHandler;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Curios 饰品模组兼容门面。
 *
 * <p>common 模块不直接引用 Curios 的类（避免未安装 Curios 时 NoClassDefFoundError），
 * 实际实现由 forge 模块的适配器在检测到 Curios 后注入。
 *
 * <p>未安装 Curios 时 {@link #isLoaded()} 返回 false，所有方法均为空操作，
 * 保证女仆饰品界面仍可打开（仅显示背包与扩容槽，不渲染饰品槽）。
 */
public final class CuriosCompat {
    @Nullable private static CuriosAdapter adapter;

    private CuriosCompat() {}

    /** 由 forge 模块在 Curios 已安装时调用，注入实际实现。 */
    public static void setAdapter(CuriosAdapter adapter) {
        CuriosCompat.adapter = adapter;
    }

    public static boolean isLoaded() {
        return adapter != null;
    }

    /**
     * 服务端：将女仆的 Curios 槽位同步为参照玩家的结构（活体比较去重、双向收敛），
     * 返回同步后的完整结构（类型-&gt;数量，按槽位顺序），供分页计算与开屏数据包使用。
     *
     * <p>未安装 Curios 时返回空 Map（饰品界面退化为扩容槽 + 玩家背包）。
     */
    public static Map<String, Integer> syncCuriosStructure(LittleMaidEntity maid, PlayerEntity openingPlayer) {
        return adapter != null ? adapter.syncCuriosStructure(maid, openingPlayer) : Map.of();
    }

    /**
     * 按页区间（类型-&gt;[类型内起始下标,数量]，见 {@link
     * CuriosScreenHandler#sliceCuriosPage}）构建饰品槽位。
     *
     * <p>服务端包装女仆实时 Curios handler（真实库存，交互生效）；客户端使用本地镜像
     * handler（Curios 同步包会原地缩容客户端 handler，直接引用会导致
     * "Slot N not in valid range" 崩溃）。两侧使用完全相同的布局算法，槽位数量与顺序一致。
     */
    public static void addCuriosSlots(
            CuriosScreenHandler handler, LittleMaidEntity maid, Map<String, int[]> pageSlice) {
        if (adapter != null) {
            adapter.addCuriosSlots(handler, maid, pageSlice);
        }
    }

    /** Curios 饰品槽位适配器，由 forge 模块实现（持有 Curios API）。 */
    public interface CuriosAdapter {
        /** 服务端同步女仆槽位并返回完整结构（未安装/异常时返回空或当前实际结构）。 */
        default Map<String, Integer> syncCuriosStructure(
                LittleMaidEntity maid, PlayerEntity openingPlayer) {
            return Map.of();
        }

        /** 按页区间构建饰品槽位（服务端实时 handler / 客户端镜像 handler）。 */
        default void addCuriosSlots(
                CuriosScreenHandler handler, LittleMaidEntity maid, Map<String, int[]> pageSlice) {}
    }
}
