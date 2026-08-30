package net.sistr.littlemaidmobresurgence.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.sistr.littlemaidmobresurgence.entity.CuriosScreenHandler;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.network.C2SOpenCuriosScreenPacket;

/**
 * 女仆饰品界面（分页）。
 *
 * <p>仅用于容纳和展示饰品：Curios 饰品槽 + 背包扩容专属槽 + 玩家背包。
 * 分页布局：第 0 页 = 1 扩容槽 + 36 饰品槽；后续页 = 整页 36 饰品槽（无扩容槽）。
 * 饰品槽总数 ≤ 36 时单页不分页。切页按键样式/位置/交互与扩容背包界面一致
 * （60×16 按钮位于底部，点击发 C2S 包由服务端按目标页重开界面）。
 * 采用原版灰容器风格，与主 GUI 统一。
 *
 * <p>容器高度由实际占用动态计算：玩家背包起始 y + 4 行背包/快捷栏 + 底部边距
 * （多页时额外保留底部翻页区），保证扩容槽 (8,18)、饰品网格 (9 列对齐背包)、玩家背包三区永不重叠。
 */
@Environment(EnvType.CLIENT)
public class CuriosScreen extends HandledScreen<CuriosScreenHandler> {
    private static final int TEXT_COLOR = GuiRenderUtil.TEXT_COLOR;

    /** 玩家背包 4 行（3 行主背包 + 1 行快捷栏，行间距 18）占用高度：3*18 背包 + 4 空隙 + 1*18 快捷栏 = 76px；
     * 与 ScreenHandler.layoutPlayerInventorySlots (topRow, topRow+18, topRow+36, topRow+58) 精确对应。 */
    private static final int PLAYER_BLOCK_HEIGHT = 58 + 18;

    private final LittleMaidEntity owner;

    public CuriosScreen(CuriosScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        // 玩家背包起始 y + 4 行区域 + 底部边距；仅多页（需翻页）时额外保留底部翻页区(24)，单页紧凑无空白
        int bottomSpace = handler.getTotalPages() > 1 ? 24 : 0;
        this.backgroundHeight = handler.getPlayerInventoryY() + PLAYER_BLOCK_HEIGHT + 7 + bottomSpace;
        this.owner = handler.getGuiEntity();
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
        int left = (int) ((this.width - backgroundWidth) / 2F);
        int top = (int) ((this.height - backgroundHeight) / 2F);
        // 多页时增加左右翻页按钮（重新打开到对应页，与扩容背包界面一致）
        int totalPages = this.handler.getTotalPages();
        if (totalPages > 1) {
            int page = this.handler.getPage();
            if (page > 0) {
                this.addDrawableChild(
                        ButtonWidget.builder(
                                        Text.translatable("gui.littlemaidmobresurgence.curios.prev"),
                                        b -> C2SOpenCuriosScreenPacket.sendC2SPacket(owner, page - 1))
                                .position(left + 12, top + backgroundHeight - 20)
                                .size(60, 16)
                                .build());
            }
            if (page < totalPages - 1) {
                this.addDrawableChild(
                        ButtonWidget.builder(
                                        Text.translatable("gui.littlemaidmobresurgence.curios.next"),
                                        b -> C2SOpenCuriosScreenPacket.sendC2SPacket(owner, page + 1))
                                .position(left + backgroundWidth - 72, top + backgroundHeight - 20)
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
        // 扩容背包道具槽：特殊饰品槽位，绘制物品图标提示（灰槽 + 半透明图标）
        int upgradeStart = this.handler.getBackpackUpgradeSlotStart();
        if (upgradeStart >= 0 && upgradeStart < this.handler.slots.size()) {
            var upgradeSlot = this.handler.slots.get(upgradeStart);
            GuiRenderUtil.drawSlotIconHint(context, x, y, upgradeSlot.x, upgradeSlot.y);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.render(context, mouseX, mouseY, partialTicks);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // 标题
        context.drawText(textRenderer, this.title.getString(), 8, 6, TEXT_COLOR, false);
        // 页码（多页时，居中于底部翻页区，与扩容背包界面一致）
        int totalPages = this.handler.getTotalPages();
        if (totalPages > 1) {
            String pageText = Text.translatable(
                    "gui.littlemaidmobresurgence.curios.page",
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
