package net.sistr.littlemaidmobresurgence.client.screen;

import java.util.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetTagManager;
import net.sistr.littlemaidmobresurgence.entity.targeting.TargetingSystem;
import net.sistr.littlemaidmobresurgence.network.C2SSetTargetTagsPacket;
import org.lwjgl.glfw.GLFW;

/**
 * 敌我识别标签设置界面（原版风格分页版）。
 *
 * <p>原版灰容器背景（{@link GuiRenderUtil#drawGrayContainer}），顶部搜索框，
 * 每页固定 8 条（2 列 × 4 行），上一页/下一页按钮与女仆饰品/扩容背包界面一致（60×16），
 * 底部居中标示页码。搜索框采用原版 TextFieldWidget，实时过滤；
 * 若安装了"通用拼音搜索"(jecharacters)，搜索自动支持中文拼音模糊匹配。
 */
@Environment(EnvType.CLIENT)
public class TargetTagScreen extends Screen {
    private static final int TEXT_COLOR = GuiRenderUtil.TEXT_COLOR;
    private static final int PANEL_W = 280;
    private static final int ITEMS_PER_PAGE = 8;
    private static final int COLS = 2;
    private static final int ROWS = ITEMS_PER_PAGE / COLS;
    private static final int PADDING = 8;
    private static final int TITLE_H = 14;
    private static final int SEARCH_H = 16;
    private static final int HEADER_H = TITLE_H + 6 + SEARCH_H; // 标题+间隙+搜索框
    private static final int ENTRY_W = (PANEL_W - PADDING * 2 - 8) / COLS; // 两列中间留8px
    private static final int ENTRY_H = 36;
    private static final int FOOTER_H = 36;
    private static final int PANEL_H =
            PADDING * 2 + HEADER_H + ROWS * ENTRY_H + FOOTER_H;

    private final Entity entity;
    private final List<TargetTagEntry> allEntries = new ArrayList<>();
    private final List<TargetTagEntry> filteredEntries = new ArrayList<>();
    private int currentPage;
    private int totalPages = 1;

    private TextFieldWidget searchField;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private int panelX, panelY;

    public TargetTagScreen(Entity entity, Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
        super(Text.translatable("gui.littlemaidmobresurgence.target_tag.title"));
        this.entity = entity;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        for (Map.Entry<TargetIdentifier, Set<TargetingSystem.TargetTag>> e : targetTags.entrySet()) {
            allEntries.add(new TargetTagEntry(tr, e.getKey(), e.getValue()));
        }
        allEntries.sort(Comparator.comparing(TargetTagEntry::getSortString));
    }

    @Override
    protected void init() {
        assert this.client != null;
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.clearChildren();

        // 搜索框（原版 TextFieldWidget，位于顶部标题下方）
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

        // 翻页按钮（与饰品界面一致：60×16，左下角/右下角）
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
        currentPage = 0; // 搜索重置到首页
        applyFilter(text);
    }

    private void applyFilter(String keyword) {
        filteredEntries.clear();
        String kw = keyword == null ? "" : keyword.trim();
        for (TargetTagEntry e : allEntries) {
            if (kw.isEmpty() || PinyinMatcher.contains(e.getSearchHaystack(), kw)) {
                filteredEntries.add(e);
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

        // 搜索框
        this.searchField.render(context, mouseX, mouseY, delta);
        if (this.searchField.getText().isEmpty() && !this.searchField.isFocused()) {
            context.drawText(
                    this.textRenderer,
                    Text.translatable("gui.littlemaidmobresurgence.target_tag.search_hint"),
                    this.searchField.getX() + 4,
                    this.searchField.getY() + 4,
                    0xFF707070, false);
        }

        // 列表条目
        int contentX = panelX + PADDING;
        int contentY = panelY + PADDING + HEADER_H + 2;
        int start = currentPage * ITEMS_PER_PAGE;
        int colGap = ENTRY_W + 8;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = start + i;
            int col = i % COLS;
            int row = i / COLS;
            int x = contentX + col * colGap;
            int y = contentY + row * ENTRY_H;
            if (idx < filteredEntries.size()) {
                filteredEntries.get(idx).render(context, x, y, ENTRY_W, ENTRY_H, mouseX, mouseY, delta);
            } else {
                // 空白占位
                context.fill(x, y, x + ENTRY_W, y + ENTRY_H, 0xFF8B8B8B);
                context.fill(x + 1, y + 1, x + ENTRY_W - 1, y + ENTRY_H - 1, 0xFFC6C6C6);
            }
        }

        // 页码（底部居中，翻页按钮上方）
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

        // 条目按钮 tooltip（在 super 后绘制，确保覆盖在按钮上）
        for (TargetTagEntry e : getVisibleEntries()) {
            e.renderTooltip(context, mouseX, mouseY, this.textRenderer);
        }
    }

    private List<TargetTagEntry> getVisibleEntries() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredEntries.size());
        return filteredEntries.subList(start, end);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        for (TargetTagEntry e : getVisibleEntries()) {
            if (e.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        // 搜索框不持有焦点时，方向键/翻页键可用
        boolean searchFocused = this.searchField.isFocused();
        if (!searchFocused) {
            if ((keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_LEFT)
                    && prevButton != null && prevButton.active) {
                currentPage--; refreshPagination(); return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_RIGHT)
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
    public void removed() {
        super.removed();
        Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> result = new HashMap<>();
        for (TargetTagEntry e : allEntries) {
            result.put(e.targetIdentifier, e.getTags());
        }
        sendTargetTags(result);
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity & TargetTagManager> void sendTargetTags(
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> result) {
        C2SSetTargetTagsPacket.sendC2SPacket((T) entity, result);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ========== 条目 ==========
    private static class TargetTagEntry {
        private enum AttackState {
            ATTACK_PROHIBITED("attack_prohibited", Items.BARRIER.getDefaultStack()),
            PREEMPTIVE_ATTACK_PROHIBITED("preemptive_attack_prohibited", Items.SHIELD.getDefaultStack()),
            PREEMPTIVE_ATTACK_ALLOWED("preemptive_attack_allowed", Items.IRON_SWORD.getDefaultStack());
            final String key; final ItemStack icon;
            AttackState(String k, ItemStack i) { key = k; icon = i; }
            AttackState next() { return values()[(ordinal() + 1) % values().length]; }
        }
        private enum WeaponState {
            NO_WEAPON_RESTRICTION("no_weapon_restriction", Items.AIR.getDefaultStack()),
            MELEE_WEAPON_PROHIBITED("melee_weapon_prohibited", Items.IRON_SWORD.getDefaultStack()),
            RANGED_WEAPON_PROHIBITED("ranged_weapon_prohibited", Items.BOW.getDefaultStack());
            final String key; final ItemStack icon;
            WeaponState(String k, ItemStack i) { key = k; icon = i; }
            WeaponState next() { return values()[(ordinal() + 1) % values().length]; }
        }
        private enum ApproachState {
            APPROACH_ALLOWED("approach_allowed", Items.AIR.getDefaultStack()),
            APPROACH_PROHIBITED("approach_prohibited", Items.CREEPER_HEAD.getDefaultStack());
            final String key; final ItemStack icon;
            ApproachState(String k, ItemStack i) { key = k; icon = i; }
            ApproachState next() { return values()[(ordinal() + 1) % values().length]; }
        }

        private final TextRenderer textRenderer;
        final TargetIdentifier targetIdentifier;
        private final IconToggle[] toggles = new IconToggle[3];
        private AttackState attack;
        private WeaponState weapon;
        private ApproachState approach;
        private int x, y;

        TargetTagEntry(TextRenderer tr, TargetIdentifier id, Set<TargetingSystem.TargetTag> tags) {
            this.textRenderer = tr;
            this.targetIdentifier = id;
            this.attack = determineAttack(tags);
            this.weapon = determineWeapon(tags);
            this.approach = determineApproach(tags);
            toggles[0] = new IconToggle(() -> attack.icon, () -> attack.key, () -> attack = attack.next());
            toggles[1] = new IconToggle(() -> weapon.icon, () -> weapon.key, () -> weapon = weapon.next());
            toggles[2] = new IconToggle(() -> approach.icon, () -> approach.key, () -> approach = approach.next());
        }

        String getSortString() {
            return Text.translatable(targetIdentifier.getEntityType().getTranslationKey()).getString();
        }

        String getSearchHaystack() {
            return targetIdentifier.toString() + "," + getSortString();
        }

        void render(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY, float delta) {
            this.x = x; this.y = y;
            // 条目底色（原版槽位质感）
            ctx.fill(x, y, x + w, y + h, 0xFF8B8B8B);
            ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
            // 实体名（上方，超宽裁剪为...，严格限制在条目框内）
            Text name = Text.translatable(targetIdentifier.getEntityType().getTranslationKey());
            String nameStr = name.getString();
            int nameAvail = w - 6;
            if (textRenderer.getWidth(nameStr) > nameAvail) {
                nameStr = textRenderer.trimToWidth(nameStr, nameAvail - 3) + "...";
            }
            ctx.drawText(textRenderer, nameStr, x + 3, y + 3, GuiRenderUtil.TEXT_COLOR, false);
            // 三个图标按钮（底部一行）
            int btnY = y + h - 22;
            int btnX = x + 2;
            for (IconToggle t : toggles) {
                t.render(ctx, btnX, btnY, 20, 20, mouseX, mouseY);
                btnX += 22;
            }
        }

        void renderTooltip(DrawContext ctx, int mouseX, int mouseY, TextRenderer tr) {
            for (IconToggle t : toggles) {
                if (t.contains(mouseX, mouseY)) {
                    ctx.drawTooltip(tr,
                            Text.translatable("gui.littlemaidmobresurgence.target_tag.tags." + t.tooltipKey.get()),
                            mouseX, mouseY);
                    return;
                }
            }
        }

        boolean mouseClicked(double mx, double my, int btn) {
            if (btn != 0) return false;
            for (IconToggle t : toggles) {
                if (t.contains(mx, my)) { t.onPress.run(); return true; }
            }
            return false;
        }

        Set<TargetingSystem.TargetTag> getTags() {
            Set<TargetingSystem.TargetTag> tags = new HashSet<>();
            if (attack == AttackState.ATTACK_PROHIBITED) tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
            else if (attack == AttackState.PREEMPTIVE_ATTACK_PROHIBITED) tags.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
            if (weapon == WeaponState.MELEE_WEAPON_PROHIBITED) tags.add(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
            else if (weapon == WeaponState.RANGED_WEAPON_PROHIBITED) tags.add(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
            if (approach == ApproachState.APPROACH_PROHIBITED) tags.add(TargetingSystem.TargetTag.APPROACH_PROHIBITED);
            return tags;
        }

        private static AttackState determineAttack(Set<TargetingSystem.TargetTag> t) {
            if (t.contains(TargetingSystem.TargetTag.ATTACK_PROHIBITED)) return AttackState.ATTACK_PROHIBITED;
            if (t.contains(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED)) return AttackState.PREEMPTIVE_ATTACK_PROHIBITED;
            return AttackState.PREEMPTIVE_ATTACK_ALLOWED;
        }
        private static WeaponState determineWeapon(Set<TargetingSystem.TargetTag> t) {
            boolean m = t.contains(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
            boolean r = t.contains(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
            if (m && !r) return WeaponState.MELEE_WEAPON_PROHIBITED;
            if (!m && r) return WeaponState.RANGED_WEAPON_PROHIBITED;
            return WeaponState.NO_WEAPON_RESTRICTION;
        }
        private static ApproachState determineApproach(Set<TargetingSystem.TargetTag> t) {
            return t.contains(TargetingSystem.TargetTag.APPROACH_PROHIBITED)
                    ? ApproachState.APPROACH_PROHIBITED : ApproachState.APPROACH_ALLOWED;
        }
    }

    /** 原版风格图标按钮（灰底按钮+物品图标居中，按下/悬停反馈） */
    private static class IconToggle {
        final java.util.function.Supplier<ItemStack> icon;
        final java.util.function.Supplier<String> tooltipKey;
        final Runnable onPress;
        int x, y, w, h;
        IconToggle(java.util.function.Supplier<ItemStack> icon,
                   java.util.function.Supplier<String> tip, Runnable onPress) {
            this.icon = icon; this.tooltipKey = tip; this.onPress = onPress;
        }
        void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
            boolean pressed = hover && MinecraftClient.getInstance().mouse.wasLeftButtonClicked();
            int bg = pressed ? 0xFF707070 : (hover ? 0xFFB0B0B0 : 0xFF909090);
            int hi = pressed ? 0xFF707070 : 0xFFFFFFFF;
            int sh = pressed ? 0xFFFFFFFF : 0xFF505050;
            ctx.fill(x, y, x + w, y + h, 0xFF373737);
            ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
            ctx.fill(x + 1, y + 1, x + w - 1, y + 2, hi);
            ctx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, sh);
            ItemStack is = icon.get();
            if (!is.isEmpty()) {
                ctx.drawItem(is, x + 2, y + 2);
            }
        }
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}




