package net.sistr.littlemaidmobresurgence.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

/**
 * 原版灰容器绘制工具。
 *
 * <p>统一提供女仆 GUI 使用的原版灰容器背景（斜切立体边框质感），
 * 供 {@link CuriosScreen}、{@link BackpackScreen}、{@link MaidEmotionScreen} 复用，
 * 避免三处重复绘制逻辑，保证风格一致。
 */
@Environment(EnvType.CLIENT)
public final class GuiRenderUtil {
    /** 容器底色（原版灰）。 */
    public static final int BG_COLOR = 0xFFC6C6C6;
    /** 外边框深灰。 */
    public static final int BORDER_COLOR = 0xFF3C3C3C;
    /** 左上高光。 */
    public static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;
    /** 右下阴影。 */
    public static final int SHADOW_COLOR = 0xFF373737;
    /** 正文灰字。 */
    public static final int TEXT_COLOR = 0x404040;

    private GuiRenderUtil() {}

    /**
     * 绘制原版灰容器背景（含斜切立体边框与标题分隔线）。
     *
     * @param context 绘制上下文
     * @param x       容器左上角 x
     * @param y       容器左上角 y
     * @param width   容器宽
     * @param height  容器高
     */
    public static void drawGrayContainer(DrawContext context, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        // 底色
        context.fill(x, y, right, bottom, BG_COLOR);
        // 左上高光
        context.fill(x, y, right, y + 1, HIGHLIGHT_COLOR);
        context.fill(x, y, x + 1, bottom, HIGHLIGHT_COLOR);
        // 右下阴影
        context.fill(x, bottom - 1, right, bottom, SHADOW_COLOR);
        context.fill(right - 1, y, right, bottom, SHADOW_COLOR);
        // 外边框深灰
        context.fill(x - 1, y - 1, right + 1, y, BORDER_COLOR);
        context.fill(x - 1, bottom, right + 1, bottom + 1, BORDER_COLOR);
        context.fill(x - 1, y, x, bottom, BORDER_COLOR);
        context.fill(right, y, right + 1, bottom, BORDER_COLOR);
    }

    /**
     * 绘制标题分隔线（原版容器标题栏下方阴影线）。
     */
    public static void drawTitleBar(DrawContext context, int x, int y, int width) {
        context.fill(x + 1, y + 20, x + width - 1, y + 21, SHADOW_COLOR);
    }

    /**
     * 绘制单个槽位格孔（深灰背景 + 浅灰边框，模拟原版容器槽位）。
     *
     * <p>1.20.1 下 {@code HandledScreen.drawSlot} 的格孔填充在 translate 矩阵后可能不显示，
     * 故在 {@code drawBackground} 中用绝对坐标手动绘制格孔，物品图标仍由 drawSlot 覆盖渲染。
     *
     * @param context 绘制上下文
     * @param originX 容器左上角 x（this.x）
     * @param originY 容器左上角 y（this.y）
     * @param slotX   槽位相对 x
     * @param slotY   槽位相对 y
     */
    public static void drawSlotHole(DrawContext context, int originX, int originY, int slotX, int slotY) {
        // 原版中 Slot.x/y 是 16x16 物品图标的左上角，18x18 格孔需向外扩一圈（-1,-1）才能与物品对齐
        int x = originX + slotX - 1;
        int y = originY + slotY - 1;
        // 深灰格孔背景（16x16，与物品图标区域重合）
        context.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        // 内边框（左上深灰、右下高光，模拟原版槽位立体感）
        context.fill(x, y, x + 18, y + 1, SHADOW_COLOR);
        context.fill(x, y + 17, x + 18, y + 18, HIGHLIGHT_COLOR);
        context.fill(x, y + 1, x + 1, y + 17, SHADOW_COLOR);
        context.fill(x + 17, y + 1, x + 18, y + 17, HIGHLIGHT_COLOR);
    }

    /**
     * 绘制"特殊饰品槽位"提示：在已有格孔之上叠加一个<b>灰色描边阴影</b>（仅保留物品结构外轮廓、中空），
     * 作为"该槽位可放入什么物品"的暗淡提示（参照 Curios 等模组的槽位背景图标）。
     *
     * @param context 绘制上下文
     * @param originX 容器左上角 x（this.x）
     * @param originY 容器左上角 y（this.y）
     * @param slotX   槽位相对 x
     * @param slotY   槽位相对 y
     */
    public static void drawSlotIconHint(
            DrawContext context, int originX, int originY, int slotX, int slotY) {
        int x0 = originX + slotX;
        int y0 = originY + slotY;
        final int color = 0xFF555555;
        for (int row = 0; row < 16; row++) {
            int bits = UPGRADE_OUTLINE[row];
            if (bits == 0) {
                continue;
            }
            int y = y0 + row;
            for (int col = 0; col < 16; col++) {
                if (((bits >> (15 - col)) & 1) != 0) {
                    int x = x0 + col;
                    int x1 = Math.min(x0 + 16, x + 2);
                    int y1 = Math.min(y0 + 16, y + 2);
                    context.fill(x, y, x1, y1, color);
                }
            }
        }
    }

    /** 扩容背包道具槽灰色描边的 16×16 bitmask（行内高位→左，bit=1 即描边像素），内部镂空。 */
    private static final int[] UPGRADE_OUTLINE = {
        0x0000, 0x07E0, 0x0810, 0x300C, 0x4002, 0x4002, 0x4002, 0x4002,
        0x4002, 0x4002, 0x4002, 0x4002, 0x4002, 0x2004, 0x1FF8, 0x0000
    };
}
