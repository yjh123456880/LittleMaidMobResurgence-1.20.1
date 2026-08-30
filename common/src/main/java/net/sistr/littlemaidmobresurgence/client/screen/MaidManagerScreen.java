package net.sistr.littlemaidmobresurgence.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.MaidManager;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import net.sistr.littlemaidmobresurgence.network.C2SCallWaitPacket;
import net.sistr.littlemaidmobresurgence.network.C2SOpenInventoryPacket;
import org.lwjgl.glfw.GLFW;

/**
 * 女仆管理界面（原版风格分页版）。
 */
@Environment(EnvType.CLIENT)
public class MaidManagerScreen extends Screen {
    private static final int TEXT_COLOR = GuiRenderUtil.TEXT_COLOR;
    private static final int PANEL_W = 256;
    private static final int ITEMS_PER_PAGE = 3;
    private static final int PADDING = 8;
    private static final int TITLE_H = 14;
    private static final int SEARCH_H = 16;
    private static final int HEADER_H = TITLE_H + 6 + SEARCH_H;
    private static final int ENTRY_W = PANEL_W - PADDING * 2;
    private static final int ENTRY_H = 60;
    private static final int FOOTER_H = 36;
    private static final int PANEL_H =
            PADDING * 2 + HEADER_H + ITEMS_PER_PAGE * ENTRY_H + FOOTER_H;

    private final List<MaidInfoCard> allEntries = new ArrayList<>();
    private final List<MaidInfoCard> filteredEntries = new ArrayList<>();
    private int currentPage;
    private int totalPages = 1;

    private TextFieldWidget searchField;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private int panelX, panelY;

    public MaidManagerScreen(List<MaidManager.LMInfo> lmInfoList) {
        super(Text.translatable("gui.littlemaidmobresurgence.maidmanager.title"));
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String currentWorldId = "";
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            currentWorldId = mc.world.getRegistryKey().getValue().toString();
        }
        String finalWorldId = currentWorldId;
        List<MaidInfoCard> cards = lmInfoList.stream()
                .map(info -> new MaidInfoCard(tr, info))
                .sorted(createSortComparator(finalWorldId))
                .collect(Collectors.toList());
        allEntries.addAll(cards);
    }

    @Override
    protected void init() {
        assert this.client != null;
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.clearChildren();

        int searchX = panelX + PADDING;
        int searchY = panelY + PADDING + HEADER_H - SEARCH_H;
        int searchW = PANEL_W - PADDING * 2;
        String prevText = this.searchField != null ? this.searchField.getText() : "";
        this.searchField =
                new TextFieldWidget(
                        this.textRenderer,
                        searchX, searchY, searchW, SEARCH_H,
                        Text.translatable("gui.littlemaidmobresurgence.search"));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.searchField.setDrawsBackground(true);
        this.searchField.setEditableColor(0x303030);
        this.searchField.setMaxLength(64);
        this.searchField.setText(prevText);
        this.addSelectableChild(this.searchField);

        int btnY = panelY + PANEL_H - PADDING - 18;
        this.prevButton =
                this.addDrawableChild(
                        ButtonWidget.builder(
                                        Text.translatable("gui.littlemaidmobresurgence.curios.prev"),
                                        b -> { currentPage--; refreshPagination(); })
                                .position(panelX + PADDING, btnY)
                                .size(60, 16)
                                .build());
        this.nextButton =
                this.addDrawableChild(
                        ButtonWidget.builder(
                                        Text.translatable("gui.littlemaidmobresurgence.curios.next"),
                                        b -> { currentPage++; refreshPagination(); })
                                .position(panelX + PANEL_W - PADDING - 60, btnY)
                                .size(60, 16)
                                .build());

        applyFilter(prevText);
    }

    private void onSearchChanged(String text) {
        currentPage = 0;
        applyFilter(text);
    }

    private void applyFilter(String keyword) {
        filteredEntries.clear();
        String kw = keyword == null ? "" : keyword.trim();
        for (MaidInfoCard c : allEntries) {
            if (kw.isEmpty() || PinyinMatcher.contains(c.getSearchHaystack(), kw)) {
                filteredEntries.add(c);
            }
        }
        this.totalPages = Math.max(1, (filteredEntries.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        this.currentPage = MathHelper.clamp(currentPage, 0, totalPages - 1);
        refreshPagination();
    }

    private void refreshPagination() {
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = currentPage < totalPages - 1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        GuiRenderUtil.drawGrayContainer(context, panelX, panelY, PANEL_W, PANEL_H);

        // 标题（垂直居中于搜索框上方区域，与上下两边距离相等）
        context.drawText(
                this.textRenderer, this.title,
                panelX + PADDING,
                panelY + PADDING + (HEADER_H - SEARCH_H - this.textRenderer.fontHeight) / 2,
                TEXT_COLOR, false);

        this.searchField.render(context, mouseX, mouseY, delta);
        if (this.searchField.getText().isEmpty() && !this.searchField.isFocused()) {
            context.drawText(
                    this.textRenderer,
                    Text.translatable("gui.littlemaidmobresurgence.maidmanager.search_hint"),
                    this.searchField.getX() + 4,
                    this.searchField.getY() + 4,
                    0xFF707070, false);
        }

        int contentX = panelX + PADDING;
        int contentY = panelY + PADDING + HEADER_H + 2;
        int start = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = start + i;
            int x = contentX;
            int y = contentY + i * ENTRY_H;
            if (idx < filteredEntries.size()) {
                filteredEntries.get(idx).render(context, x, y, ENTRY_W, ENTRY_H, mouseX, mouseY, delta);
            } else {
                context.fill(x, y, x + ENTRY_W, y + ENTRY_H, 0xFF8B8B8B);
                context.fill(x + 1, y + 1, x + ENTRY_W - 1, y + ENTRY_H - 1, 0xFFC6C6C6);
            }
        }

        if (filteredEntries.isEmpty()) {
            String msg = Text.translatable("gui.littlemaidmobresurgence.maidmanager.no_results").getString();
            int w = this.textRenderer.getWidth(msg);
            context.drawText(this.textRenderer, msg,
                    panelX + (PANEL_W - w) / 2,
                    contentY + (ITEMS_PER_PAGE * ENTRY_H) / 2 - 4,
                    0xFF808080, false);
        }

        if (totalPages > 1) {
            String pageStr = Text.translatable("gui.littlemaidmobresurgence.curios.page",
                    currentPage + 1, totalPages).getString();
            int w = this.textRenderer.getWidth(pageStr);
            context.drawText(this.textRenderer, pageStr,
                    panelX + (PANEL_W - w) / 2,
                    panelY + PANEL_H - PADDING - 30,
                    TEXT_COLOR, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private List<MaidInfoCard> getVisibleEntries() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredEntries.size());
        return filteredEntries.subList(start, end);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        for (MaidInfoCard c : getVisibleEntries()) {
            if (c.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (MaidInfoCard c : getVisibleEntries()) {
            if (c.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        boolean searchFocused = this.searchField.isFocused();
        if (!searchFocused) {
            if ((keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_UP)
                    && prevButton != null && prevButton.active) {
                currentPage--; refreshPagination(); return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_DOWN)
                    && nextButton != null && nextButton.active) {
                currentPage++; refreshPagination(); return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchField.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static Comparator<MaidInfoCard> createSortComparator(String currentWorldId) {
        return Comparator
                .comparing((MaidInfoCard c) -> c.info.status().name())
                .thenComparing(c -> {
                    String worldId = c.info.getWorldId();
                    return worldId.equals(currentWorldId) ? 0 : 1;
                })
                .thenComparing(c -> c.info.getWorldId())
                .thenComparing(c -> c.info.name())
                .thenComparing(c -> {
                    String worldId = c.info.getWorldId();
                    if (!worldId.equals(currentWorldId)) return Double.MAX_VALUE;
                    var client = MinecraftClient.getInstance();
                    if (client == null || client.player == null) return Double.MAX_VALUE;
                    return c.info.getEntityClient(client.world)
                            .map(e -> client.player.squaredDistanceTo(e.getX(), e.getY(), e.getZ()))
                            .orElse(Double.MAX_VALUE);
                });
    }

    private static class MaidInfoCard {
        final TextRenderer textRenderer;
        final MaidManager.LMInfo info;
        private final LittleMaidScreen.IconButtonWidget inventoryButton;
        private final ButtonWidget callWaitButton;
        private int x, y;

        MaidInfoCard(TextRenderer tr, MaidManager.LMInfo info) {
            this.textRenderer = tr;
            this.info = info;
            this.inventoryButton =
                    new LittleMaidScreen.IconButtonWidget(
                            0, 0,
                            new ItemStack(Items.CHEST),
                            Text.translatable("gui.littlemaidmobresurgence.maidmanager.open_inventory"),
                            (button) -> openInventory());
            this.callWaitButton =
                    ButtonWidget.builder(
                                    Text.translatable("gui.littlemaidmobresurgence.maidmanager.call"),
                                    onPress -> toggleCallWait())
                            .size(40, 18)
                            .build();
        }

        String getSearchHaystack() {
            var client = MinecraftClient.getInstance();
            StringBuilder sb = new StringBuilder();
            sb.append(info.name()).append(',').append(info.status().getText().getString());
            String worldId = info.getWorldId();
            if (!worldId.isEmpty()) sb.append(',').append(worldId);
            info.getEntityClient(client.world)
                    .filter(e -> e instanceof LittleMaidEntity)
                    .map(e -> (LittleMaidEntity) e)
                    .ifPresent(m -> m.getModeName().ifPresent(n -> sb.append(',').append(n)));
            return sb.toString();
        }

        private boolean canInteractWithMaid() {
            var client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) return false;
            String worldId = info.getWorldId();
            if (worldId.isEmpty()
                    || !worldId.equals(client.world.getRegistryKey().getValue().toString())) {
                return false;
            }
            return info.getEntityClient(client.world)
                    .map(entity -> {
                        double squaredDistance =
                                client.player.squaredDistanceTo(
                                        entity.getX(), entity.getY(), entity.getZ());
                        return squaredDistance < 64.0;
                    })
                    .orElse(false);
        }

        private void openInventory() {
            var client = MinecraftClient.getInstance();
            if (client == null || client.world == null) return;
            if (canInteractWithMaid()) {
                info.getEntityClient(client.world)
                        .ifPresent(entity -> C2SOpenInventoryPacket.sendC2SPacket(entity));
            }
        }

        private void toggleCallWait() {
            var client = MinecraftClient.getInstance();
            if (client == null || client.world == null) return;
            info.getEntityClient(client.world)
                    .filter(e -> e instanceof LittleMaidEntity)
                    .map(e -> (LittleMaidEntity) e)
                    .ifPresent(e ->
                            C2SCallWaitPacket.sendC2SPacket(
                                    e,
                                    TameableUtil.isWait(e)
                                            ? C2SCallWaitPacket.State.CALL
                                            : C2SCallWaitPacket.State.WAIT));
        }

        void render(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY, float delta) {
            this.x = x; this.y = y;
            var client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) return;
            TextRenderer tr = client.textRenderer;

            ctx.fill(x, y, x + w, y + h, 0xFF8B8B8B);
            ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);

            int textX = x + 3;
            // 信息显示区向右延展约0.8cm（14px），让内容尽量完整展示；仍与右侧按钮保持间距
            int textW = w / 2 - 22;
            int lineH = tr.fontHeight;
            int scaledLineH = (int) (lineH * 1.2f);
            int curY = y + 2;

            // 名称行：黑色底条 + 放大1.2倍白字（带阴影），超宽裁剪为...
            ctx.fill(x, y, x + w - 1, y + scaledLineH + 2, 0xFF000000);
            String name = info.getEntityClient(client.world)
                    .map(entity -> entity.getName().getString())
                    .orElse(info.name());
            var matrices = ctx.getMatrices();
            matrices.push();
            matrices.scale(1.2f, 1.2f, 1.0f);
            String nm = name;
            int nameAvail = (int) ((textW - 4) / 1.2f);
            if (tr.getWidth(nm) > nameAvail) {
                nm = tr.trimToWidth(nm, nameAvail - 3) + "...";
            }
            ctx.drawText(tr, nm, (int) (textX / 1.2f), (int) (curY / 1.2f), 0xFFFFFFFF, true);
            matrices.pop();
            curY += scaledLineH + 2;

            // 信息行统一采用原版白色标准字 + 阴影（替换原先的灰字），状态行加粗突出
            var statusText = info.status().getText().copy().formatted(Formatting.BOLD);
            var loadedText = info.isLoaded()
                    ? Text.translatable("gui.littlemaidmobresurgence.maidmanager.loaded").formatted(Formatting.GRAY)
                    : Text.translatable("gui.littlemaidmobresurgence.maidmanager.unloaded").formatted(Formatting.GRAY);
            statusText = statusText
                    .append(Text.literal(" / ").formatted(Formatting.GRAY))
                    .append(loadedText);
            ctx.drawText(tr, statusText, textX, curY, 0xFFFFFFFF, true);
            curY += lineH + 1;

            String worldId = info.getWorldId();
            if (!worldId.isEmpty()) {
                drawScrollingText(ctx, tr, worldId, textX, curY, textW, 0xFFFFFFFF, true);
                curY += lineH + 1;
            }

            if (!worldId.isEmpty()) {
                BlockPos pos = info.getEntityClient(client.world)
                        .map(entity -> entity.getBlockPos())
                        .orElse(info.getLastPos());
                var coordText = Text.literal(
                        String.format("XYZ: %d, %d, %d", pos.getX(), pos.getY(), pos.getZ())).copy();
                if (worldId.equals(client.world.getRegistryKey().getValue().toString())) {
                    double squaredDistance =
                            client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    double distance = Math.sqrt(squaredDistance);
                    Formatting distanceColor = distance <= 8.0 ? Formatting.WHITE : Formatting.GRAY;
                    Text distanceText = Text.literal(String.format(" (%.0fm)", distance))
                            .formatted(distanceColor);
                    coordText.append(distanceText);
                }
                drawScrollingText(ctx, tr, coordText, textX, curY, textW, 0xFFFFFFFF, true);
                curY += lineH + 1;
            }

            int modeY = curY;
            info.getEntityClient(client.world)
                    .filter(e -> e instanceof LittleMaidEntity)
                    .map(e -> (LittleMaidEntity) e)
                    .ifPresent(m -> m.getModeName().ifPresent(modeName ->
                            ctx.drawText(tr,
                                    Text.translatable("mode." + LMMRMod.MODID + "." + modeName),
                                    textX, modeY, 0xFFFFFFFF, true)));

            int entityX = x + w - 14;
            int entityY = y + h - 6;
            int entitySize = 28;
            info.getEntityClient(client.world)
                    .filter(e -> e instanceof LivingEntity)
                    .map(e -> (LivingEntity) e)
                    .ifPresent(e -> InventoryScreen.drawEntity(
                            ctx, entityX, entityY, entitySize, 20, 0, e));

            if (canInteractWithMaid()) {
                // 按钮组（箱子20 + 间距4 + 切换40）垂直在卡片内居中，水平整体右移18px（约1cm）避免遮挡关键信息
                int btnGroupX = x + w / 2 - 14;
                int invBtnY = y + (h - 20) / 2;
                int waitBtnY = y + (h - 18) / 2;
                inventoryButton.setPosition(btnGroupX, invBtnY);
                inventoryButton.render(ctx, mouseX, mouseY, delta);

                info.getEntityClient(client.world)
                        .filter(e -> e instanceof LittleMaidEntity)
                        .map(e -> (LittleMaidEntity) e)
                        .ifPresent(e -> {
                            if (TameableUtil.isWait(e)) {
                                callWaitButton.setMessage(
                                        Text.translatable("gui.littlemaidmobresurgence.maidmanager.call"));
                            } else {
                                callWaitButton.setMessage(
                                        Text.translatable("gui.littlemaidmobresurgence.maidmanager.wait"));
                            }
                        });
                callWaitButton.setPosition(btnGroupX + 24, waitBtnY);
                callWaitButton.render(ctx, mouseX, mouseY, delta);
            }
        }

        boolean mouseClicked(double mx, double my, int btn) {
            if (btn != 0) return false;
            if (!canInteractWithMaid()) return false;
            if (inventoryButton.mouseClicked(mx, my, btn)) return true;
            if (callWaitButton.mouseClicked(mx, my, btn)) return true;
            return false;
        }

        boolean mouseReleased(double mx, double my, int btn) {
            if (!canInteractWithMaid()) return false;
            if (inventoryButton.mouseReleased(mx, my, btn)) return true;
            if (callWaitButton.mouseReleased(mx, my, btn)) return true;
            return false;
        }

        private static void drawScrollingText(
                DrawContext ctx, TextRenderer tr, String text,
                int x, int y, int availableWidth, int color, boolean shadow) {
            drawScrollingText(ctx, tr, Text.of(text), x, y, availableWidth, color, shadow);
        }

        private static void drawScrollingText(
                DrawContext ctx, TextRenderer tr, Text text,
                int x, int y, int availableWidth, int color, boolean shadow) {
            int textWidth = tr.getWidth(text);
            if (textWidth <= availableWidth) {
                ctx.drawText(tr, text, x, y, color, shadow);
            } else {
                double seconds = Util.getMeasuringTimeMs() / 1000.0;
                double scrollSpeed = 20.0;
                int displayWidth = availableWidth - 8;
                int scrollDistance = textWidth - displayWidth;
                double cycleTime = (scrollDistance + displayWidth) / scrollSpeed;
                double cyclePosition = (seconds % cycleTime) / cycleTime;

                int scrollOffset;
                if (cyclePosition < 0.8) {
                    scrollOffset = (int) (cyclePosition / 0.8 * scrollDistance);
                } else {
                    scrollOffset = scrollDistance;
                }

                ctx.enableScissor(x, y, x + displayWidth, y + tr.fontHeight);
                ctx.drawText(tr, text, x - scrollOffset, y, color, shadow);
                ctx.disableScissor();
            }
        }
    }
}

