package net.sistr.littlemaidmobresurgence.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.sistr.littlemaidmobresurgence.entity.BackpackScreenHandler;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.network.C2SOpenBackpackScreenPacket;

/**
 * 女仆扩容背包界面。
 *
 * <p>单页自适应：按当前页扩容格数动态计算容器高度（9 列布局，玩家背包紧跟其后）。
 * 仅 5 级（下界合金）分两页，通过左右翻页按钮切换（重新打开界面到对应页），无翻页包。
 */
@Environment(EnvType.CLIENT)
public class BackpackScreen extends HandledScreen<BackpackScreenHandler> {
    private static final int TEXT_COLOR = GuiRenderUtil.TEXT_COLOR;

    private final LittleMaidEntity owner;

    public BackpackScreen(BackpackScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.owner = handler.getGuiEntity();
        // 高度 = 玩家背包区起始 y + 玩家背包 4 行(76) + 底边距(7)
        // 仅多页（需翻页）时额外保留底部翻页区(24)，单页紧凑无空白
        int bottomSpace = handler.getTotalPages() > 1 ? 24 : 0;
        this.backgroundHeight = handler.getPlayerInventoryY() + 76 + 7 + bottomSpace;
        this.backgroundWidth = 176;
    }

    @Override
    protected void init() {
        super.init();
        if (owner == null) {
            if (client != null) {
                client.setScreen(null);
            }
            return;
        }
        // 多页时增加左右翻页按钮（重新打开到对应页）
        int totalPages = this.handler.getTotalPages();
        if (totalPages > 1) {
            int x = (this.width - backgroundWidth) / 2;
            int y = (this.height - backgroundHeight) / 2;
            int page = this.handler.getPage();
            if (page > 0) {
                this.addDrawableChild(
                        ButtonWidget.builder(
                                        Text.translatable("gui.littlemaidmobresurgence.backpack.prev"),
                                        b -> C2SOpenBackpackScreenPacket.sendC2SPacket(owner, page - 1))
                                .position(x + 12, y + backgroundHeight - 20)
                                .size(60, 16)
                                .build());
            }
            if (page < totalPages - 1) {
                this.addDrawableChild(
                        ButtonWidget.builder(
                                        Text.translatable("gui.littlemaidmobresurgence.backpack.next"),
                                        b -> C2SOpenBackpackScreenPacket.sendC2SPacket(owner, page + 1))
                                .position(x + backgroundWidth - 72, y + backgroundHeight - 20)
                                .size(60, 16)
                                .build());
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - backgroundWidth) / 2;
        int y = (this.height - backgroundHeight) / 2;
        // 原版灰容器背景
        GuiRenderUtil.drawGrayContainer(context, x, y, this.backgroundWidth, this.backgroundHeight);
        // 手动绘制所有槽位格孔（兜底，确保格子背景与边框可见）
        for (var slot : this.handler.slots) {
            GuiRenderUtil.drawSlotHole(context, x, y, slot.x, slot.y);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, this.title.getString(), 8, 6, TEXT_COLOR, false);
        // 页码（多页时，居中于底部翻页区）
        int totalPages = this.handler.getTotalPages();
        if (totalPages > 1) {
            String pageText = Text.translatable(
                    "gui.littlemaidmobresurgence.backpack.page",
                    this.handler.getPage() + 1, totalPages).getString();
            int w = textRenderer.getWidth(pageText);
            context.drawText(textRenderer, pageText, (backgroundWidth - w) / 2,
                    backgroundHeight - 19, TEXT_COLOR, false);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
