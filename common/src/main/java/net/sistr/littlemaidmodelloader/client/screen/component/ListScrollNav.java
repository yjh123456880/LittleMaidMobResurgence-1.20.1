package net.sistr.littlemaidmodelloader.client.screen.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * 女仆 GUI 风格的滚轮导航条:上/下箭头按键 + 中间可拖拽滑块。
 *
 * <p>参照车万女仆界面的右侧导航键绘制(深灰底 + 白高光 + 深灰阴影的立体按钮,箭头用三角绘制)。
 * 用于模型/语音包等可纵向滚动的列表,与 {@link ScrollableListGUI} 配合:
 * 不使用其内置旧式滚动条,改由本组件控制滚动位置。
 */
public class ListScrollNav<T extends GUIElement> {
    private final ScrollableListGUI<T> list;
    private final int x;
    private final int y;
    private final int height;
    private final int buttonSize;
    private boolean dragging;
    private int dragOffset;

    public ListScrollNav(ScrollableListGUI<T> list, int x, int y, int height, int buttonSize) {
        this.list = list;
        this.x = x;
        this.y = y;
        this.height = height;
        this.buttonSize = buttonSize;
    }

    private int totalRows() {
        return (list.size() + list.widthStack - 1) / list.widthStack;
    }

    public int maxScroll() {
        return Math.max(0, totalRows() - list.heightStack);
    }

    private int currentScroll() {
        return MathHelper.clamp(list.getScroll(), 0, maxScroll());
    }

    private int trackY() {
        return y + buttonSize;
    }

    private int trackH() {
        return (y + height - buttonSize) - trackY();
    }

    private int upY() {
        return y;
    }

    private int downY() {
        return y + height - buttonSize;
    }

    /** 计算滑块位置:[thumbY, thumbH]。 */
    private int[] thumbBounds() {
        int trackY = trackY();
        int trackH = trackH();
        int totalRows = totalRows();
        int max = maxScroll();
        int thumbH;
        if (max == 0 || totalRows == 0) {
            thumbH = Math.max(1, trackH - 2);
        } else {
            thumbH = Math.max(8, (int) (trackH * list.heightStack / (float) totalRows));
            thumbH = MathHelper.clamp(thumbH, 8, trackH - 2);
        }
        float frac = max == 0 ? 0f : currentScroll() / (float) max;
        int avail = trackH - 2 - thumbH;
        int thumbY = trackY + 1 + (int) (avail * frac);
        return new int[] {thumbY, thumbH};
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawArrowButton(ctx, x, upY(), buttonSize, buttonSize, true, isHover(mouseX, mouseY, upY()));
        drawArrowButton(ctx, x, downY(), buttonSize, buttonSize, false, isHover(mouseX, mouseY, downY()));
        int trackY = trackY();
        int trackH = trackH();
        // 凹陷轨道(深色)
        ctx.fill(x, trackY, x + buttonSize, trackY + trackH, 0xFF3A3A3A);
        ctx.fill(x + 1, trackY + 1, x + buttonSize - 1, trackY + trackH - 1, 0xFF707070);
        int[] tb = thumbBounds();
        int thumbY = tb[0];
        int thumbH = tb[1];
        // 凸起滑块(亮色)
        ctx.fill(x, thumbY, x + buttonSize, thumbY + thumbH, 0xFFC8C8C8);
        ctx.fill(x, thumbY, x + buttonSize, thumbY + 1, 0xFFFFFFFF);
        ctx.fill(x, thumbY, x + 1, thumbY + thumbH, 0xFFFFFFFF);
        ctx.fill(x, thumbY + thumbH - 1, x + buttonSize, thumbY + thumbH, 0xFF404040);
        ctx.fill(x + buttonSize - 1, thumbY, x + buttonSize, thumbY + thumbH, 0xFF404040);
    }

    private void drawArrowButton(
            DrawContext ctx, int bx, int by, int w, int h, boolean up, boolean hover) {
        int bg = hover ? 0xFFB0B0B0 : 0xFF909090;
        ctx.fill(bx - 1, by - 1, bx + w + 1, by + h + 1, 0xFF373737);
        ctx.fill(bx, by, bx + w, by + h, bg);
        ctx.fill(bx, by, bx + w, by + 1, 0xFFFFFFFF);
        ctx.fill(bx, by, bx + 1, by + h, 0xFFFFFFFF);
        ctx.fill(bx, by + h - 1, bx + w, by + h, 0xFF505050);
        ctx.fill(bx + w - 1, by, bx + w, by + h, 0xFF505050);
        int cx = bx + w / 2;
        for (int dy = 0; dy < 4; dy++) {
            int rowY = by + (up ? h / 2 - 1 + dy : h / 2 - 1 + (3 - dy));
            int halfW = 1 + dy;
            ctx.fill(cx - halfW, rowY, cx + halfW + 1, rowY + 1, 0xFF202020);
        }
    }

    private boolean isHover(int mouseX, int mouseY, int by) {
        return mouseX >= x && mouseX < x + buttonSize && mouseY >= by && mouseY < by + buttonSize;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (isHover((int) mouseX, (int) mouseY, upY())) {
            list.setScroll(currentScroll() - 1);
            return true;
        }
        if (isHover((int) mouseX, (int) mouseY, downY())) {
            list.setScroll(currentScroll() + 1);
            return true;
        }
        int trackY = trackY();
        int trackH = trackH();
        if (mouseX >= x
                && mouseX < x + buttonSize
                && mouseY >= trackY
                && mouseY < trackY + trackH) {
            int[] tb = thumbBounds();
            if (mouseY >= tb[0] && mouseY < tb[0] + tb[1]) {
                dragging = true;
                dragOffset = (int) mouseY - tb[0];
            } else {
                int step = mouseY < tb[0] ? -list.heightStack : list.heightStack;
                list.setScroll(currentScroll() + step);
            }
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!dragging) {
            return false;
        }
        int max = maxScroll();
        int trackY = trackY();
        int trackH = trackH();
        int[] tb = thumbBounds();
        int thumbH = tb[1];
        int avail = trackH - thumbH;
        if (avail <= 0 || max == 0) {
            return false;
        }
        int newY = (int) mouseY - dragOffset;
        int scroll = (int) Math.round((newY - trackY) / (float) avail * max);
        list.setScroll(MathHelper.clamp(scroll, 0, max));
        return true;
    }

    /** 外部滚轮滚动后同步滑块位置(可选)。 */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x
                && mouseX < x + buttonSize
                && mouseY >= y
                && mouseY < y + height;
    }
}
