package net.sistr.littlemaidmobresurgence.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.MaidMood;
import net.sistr.littlemaidmobresurgence.util.ReachAttributeUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 女仆属性面板：展示情绪、好感/心情/饥饿，以及全部属性数值。
 *
 * <p>第 1 页（page 0）显示完整头部信息（情绪名/好感度/好感/心情/饱食/饥饿条）+ 基础 5 项属性
 * （攻击力/血量/攻速/攻距/移速）+ 部分模组属性；后续页面仅保留标题栏，模组属性直接从标题
 * 分隔线下方开始排列，不再重复头部信息。每页容量根据页类型动态计算，确保属性区在屏幕内完整显示。
 *
 * <p>其他模组通过属性系统新增的属性自动遍历 {@link Registries#ATTRIBUTE} 识别并按名称排序展示，
 * 超出每页容量时通过左右翻页按钮分页。采用原版灰容器风格（复用 {@link GuiRenderUtil}）。
 */
@Environment(EnvType.CLIENT)
public class MaidEmotionScreen extends Screen {
    private static final int TEXT_COLOR = GuiRenderUtil.TEXT_COLOR;
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    private static final int MAX_FAVOR = MaidMood.MAX_FAVORABILITY;

    /** 面板宽。 */
    private static final int PANEL_W = 220;
    /** 第 1 页属性区起始偏移（标题+情绪+好感+心情+饥饿+饥饿条 完整头部）。 */
    private static final int HEADER_OFFSET = 124;
    /** 后续页属性区起始偏移（仅标题栏：标题文字 y+8 + 分隔线 y+20 + 4px 间距）。 */
    private static final int TITLE_ONLY_OFFSET = 24;
    /** 基础属性条目数（固定第 1 页）。 */
    private static final int BASE_ATTR_COUNT = 5;
    /** 每行属性高度（行距）。 */
    private static final int ATTR_LINE_HEIGHT = 10;
    /** 面板底部留白（翻页按钮区）。 */
    private static final int BOTTOM_PADDING = 24;

    private final LittleMaidEntity owner;
    /** 模组新增属性（按名称排序后的快照）。 */
    private List<AttrEntry> modAttributes = List.of();
    /** 当前页（0 起）。 */
    private int currentPage;
    /** 第 1 页可容纳的模组属性条目数（含完整头部 + 5 基础属性）。 */
    private int firstPageCapacity;
    /** 后续每页可容纳的模组属性条目数（仅标题栏，空间更大）。 */
    private int laterPageCapacity;
    /** 总页数。 */
    private int totalPages = 1;

    /** 属性条目（名称 + 值）。 */
    private record AttrEntry(String name, double value) {}

    public MaidEmotionScreen(LittleMaidEntity owner) {
        super(Text.translatable("gui.littlemaidmobresurgence.emotion.title"));
        this.owner = owner;
    }

    @Override
    protected void init() {
        super.init();
        // 收集模组属性（客户端可稳定访问注册表）
        this.modAttributes = collectModAttributes();
        // 根据屏幕高度动态计算每页容量
        int maxPanelH = this.height - 20;
        this.firstPageCapacity = computeFirstPageCapacity(maxPanelH);
        this.laterPageCapacity = computeLaterPageCapacity(maxPanelH);
        this.totalPages = computeTotalPages();
        this.currentPage = Math.min(this.currentPage, this.totalPages - 1);
        rebuildPaginationButtons();
    }

    /** 计算第 1 页可容纳的模组属性行数（完整头部 + 5 基础属性 + 底部翻页区之外）。 */
    private int computeFirstPageCapacity(int maxPanelH) {
        int attrAreaH = maxPanelH - HEADER_OFFSET - BASE_ATTR_COUNT * ATTR_LINE_HEIGHT - BOTTOM_PADDING;
        return Math.max(0, attrAreaH / ATTR_LINE_HEIGHT);
    }

    /** 计算后续每页可容纳的模组属性行数（仅标题栏 + 底部翻页区之外，空间更大）。 */
    private int computeLaterPageCapacity(int maxPanelH) {
        int attrAreaH = maxPanelH - TITLE_ONLY_OFFSET - BOTTOM_PADDING;
        return Math.max(1, attrAreaH / ATTR_LINE_HEIGHT);
    }

    /** 根据两档页容量计算总页数。 */
    private int computeTotalPages() {
        int total = modAttributes.size();
        if (total <= firstPageCapacity) {
            return 1;
        }
        int remaining = total - firstPageCapacity;
        return 1 + Math.max(1, (remaining + laterPageCapacity - 1) / laterPageCapacity);
    }

    /** 获取指定页显示的第一个模组属性下标。 */
    private int getStartIndex(int page) {
        if (page == 0) {
            return 0;
        }
        return firstPageCapacity + (page - 1) * laterPageCapacity;
    }

    /** 获取指定页显示的模组属性条目数。 */
    private int getModCountOnPage(int page) {
        int start = getStartIndex(page);
        if (page == 0) {
            return Math.min(firstPageCapacity, Math.max(0, modAttributes.size() - start));
        }
        return Math.min(laterPageCapacity, Math.max(0, modAttributes.size() - start));
    }

    /** 重建上一页/下一页按钮（仅当存在对应页时显示）。 */
    private void rebuildPaginationButtons() {
        this.clearChildren();
        int panelH = computePanelHeight();
        int px = this.width / 2 - PANEL_W / 2;
        int py = this.height / 2 - panelH / 2;

        // 上一页按钮（左侧）
        if (currentPage > 0) {
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.translatable("gui.littlemaidmobresurgence.emotion.prev"),
                                    b -> {
                                        currentPage--;
                                        rebuildPaginationButtons();
                                    })
                            .position(px + 12, py + panelH - 20)
                            .size(60, 16)
                            .build());
        }
        // 下一页按钮（右侧）
        if (currentPage < totalPages - 1) {
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.translatable("gui.littlemaidmobresurgence.emotion.next"),
                                    b -> {
                                        currentPage++;
                                        rebuildPaginationButtons();
                                    })
                            .position(px + PANEL_W - 72, py + panelH - 20)
                            .size(60, 16)
                            .build());
        }
    }

    /**
     * 当前页面板高度：第 1 页含完整头部 + 5 基础属性；后续页仅标题栏，高度根据实际模组属性行数计算，
     * clamp 到屏幕范围。
     */
    private int computePanelHeight() {
        int modLines = getModCountOnPage(currentPage);
        int h;
        if (currentPage == 0) {
            h = HEADER_OFFSET + BASE_ATTR_COUNT * ATTR_LINE_HEIGHT + modLines * ATTR_LINE_HEIGHT + BOTTOM_PADDING;
        } else {
            h = TITLE_ONLY_OFFSET + modLines * ATTR_LINE_HEIGHT + BOTTOM_PADDING;
        }
        return Math.min(h, this.height - 20);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int cx = this.width / 2;

        int panelH = computePanelHeight();
        int px = cx - PANEL_W / 2;
        int py = this.height / 2 - panelH / 2;

        // 复用公共灰容器绘制
        GuiRenderUtil.drawGrayContainer(context, px, py, PANEL_W, panelH);
        GuiRenderUtil.drawTitleBar(context, px, py, PANEL_W);

        // 标题（每页都显示，作为容器标识）
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, py + 8, 0xFFFFFF);

        // ====== 仅第 1 页渲染完整头部（情绪/好感/心情/饱食/饥饿条）======
        int statY;
        int line = 0;
        if (currentPage == 0) {
            MaidMood.Emotion emo = this.owner.getEmotion();
            String emoName = Text.translatable("emotion.littlemaidmobresurgence." + emo.name()).getString();
            int level = this.owner.getFavorabilityLevelValue();
            int favor = this.owner.getFavorabilityValue();
            int remainToNext =
                    level >= 5
                            ? 0
                            : Math.max(0, MaidMood.getThresholdForLevel(level + 1) - favor);
            int mood = this.owner.getMoodValue();
            int hunger = this.owner.getHungerValue();

            // 情绪名（彩色）
            context.drawCenteredTextWithShadow(this.textRenderer, emoName, cx, py + 24, colorOf(emo));

            // 好感/心情/饥饿
            drawCenteredText(context, Text.translatable("gui.littlemaidmobresurgence.emotion.level", level), cx, py + 44);
            drawCenteredText(context, Text.translatable("gui.littlemaidmobresurgence.emotion.favor", favor), cx, py + 56);
            drawCenteredText(context,
                    favor >= MAX_FAVOR || level >= 5
                            ? Text.translatable("gui.littlemaidmobresurgence.emotion.max")
                            : Text.translatable("gui.littlemaidmobresurgence.emotion.next_level", remainToNext),
                    cx, py + 68);
            drawCenteredText(context, Text.translatable("gui.littlemaidmobresurgence.littlemaid.status.mood", mood), cx, py + 80);
            drawCenteredText(context, Text.translatable("gui.littlemaidmobresurgence.littlemaid.status.hunger", hunger), cx, py + 96);
            drawHungerBar(context, cx, py + 108, hunger);

            // 基础 5 项属性（仅第 1 页）
            statY = py + HEADER_OFFSET;
            context.drawText(textRenderer, getAttrLabel("attack_damage") + " : "
                    + fmt(getAttr(EntityAttributes.GENERIC_ATTACK_DAMAGE)), px + 12, statY + line++ * ATTR_LINE_HEIGHT, TEXT_COLOR, false);
            context.drawText(textRenderer, getAttrLabel("health") + " : "
                    + fmt(owner.getHealth()) + "/" + fmt(owner.getMaxHealth()), px + 12, statY + line++ * ATTR_LINE_HEIGHT, TEXT_COLOR, false);
            context.drawText(textRenderer, getAttrLabel("attack_speed") + " : "
                    + fmt(getAttr(EntityAttributes.GENERIC_ATTACK_SPEED)), px + 12, statY + line++ * ATTR_LINE_HEIGHT, TEXT_COLOR, false);
            context.drawText(textRenderer, getAttrLabel("attack_range") + " : "
                    + fmt(ReachAttributeUtil.getAttackRange(owner)), px + 12, statY + line++ * ATTR_LINE_HEIGHT, TEXT_COLOR, false);
            context.drawText(textRenderer, getAttrLabel("movement_speed") + " : "
                    + fmt(getAttr(EntityAttributes.GENERIC_MOVEMENT_SPEED)), px + 12, statY + line++ * ATTR_LINE_HEIGHT, TEXT_COLOR, false);
        } else {
            // 后续页：属性区从标题分隔线下方直接开始
            statY = py + TITLE_ONLY_OFFSET;
        }

        // 当前页模组属性
        int startIdx = getStartIndex(currentPage);
        int endIdx = Math.min(modAttributes.size(), startIdx + (currentPage == 0 ? firstPageCapacity : laterPageCapacity));
        for (int i = startIdx; i < endIdx; i++) {
            AttrEntry e = modAttributes.get(i);
            context.drawText(textRenderer, e.name() + " : " + fmt(e.value()), px + 12, statY + line++ * ATTR_LINE_HEIGHT, TEXT_COLOR, false);
        }

        // 页码指示（居中，底部翻页区）
        if (totalPages > 1) {
            String pageText = Text.translatable(
                    "gui.littlemaidmobresurgence.emotion.page", currentPage + 1, totalPages).getString();
            int w = this.textRenderer.getWidth(pageText);
            context.drawText(this.textRenderer, pageText, cx - w / 2, py + panelH - 15, TEXT_COLOR, false);
        }

        // 控件（翻页按钮）最后渲染，确保绘制在面板之上
        super.render(context, mouseX, mouseY, delta);
    }

    /** 遍历属性注册表，收集女仆拥有且不在基础白名单内的模组属性。 */
    private List<AttrEntry> collectModAttributes() {
        List<AttrEntry> result = new ArrayList<>();
        for (EntityAttribute attr : Registries.ATTRIBUTE) {
            if (isBaseAttribute(attr)) {
                continue;
            }
            EntityAttributeInstance instance = owner.getAttributeInstance(attr);
            if (instance == null) {
                continue;
            }
            String name = Text.translatable(attr.getTranslationKey()).getString();
            result.add(new AttrEntry(name, instance.getValue()));
        }
        result.sort(Comparator.comparing(AttrEntry::name));
        return result;
    }

    /** 判断属性是否为女仆基础属性（不视为模组新增属性）。 */
    private static boolean isBaseAttribute(EntityAttribute attr) {
        // Forge 攻击范围属性已在基础 5 项中作为"攻距"展示
        Identifier id = Registries.ATTRIBUTE.getId(attr);
        if (id != null && "forge".equals(id.getNamespace()) && "entity_reach".equals(id.getPath())) {
            return true;
        }
        return attr == EntityAttributes.GENERIC_MAX_HEALTH
                || attr == EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE
                || attr == EntityAttributes.GENERIC_MOVEMENT_SPEED
                || attr == EntityAttributes.GENERIC_FLYING_SPEED
                || attr == EntityAttributes.GENERIC_ARMOR
                || attr == EntityAttributes.GENERIC_ARMOR_TOUGHNESS
                || attr == EntityAttributes.GENERIC_FOLLOW_RANGE
                || attr == EntityAttributes.GENERIC_ATTACK_KNOCKBACK
                || attr == EntityAttributes.GENERIC_ATTACK_DAMAGE
                || attr == EntityAttributes.GENERIC_ATTACK_SPEED
                || attr == EntityAttributes.GENERIC_LUCK;
    }

    /** 居中绘制文本（灰字）。 */
    private void drawCenteredText(DrawContext context, Text text, int cx, int y) {
        int w = this.textRenderer.getWidth(text);
        context.drawText(this.textRenderer, text, cx - w / 2, y - 4, TEXT_COLOR, false);
    }

    /** 绘制玩家原版饥饿条（10 个鸡腿，引用 icons.png）。hunger 0-100 映射 10 个鸡腿。 */
    private void drawHungerBar(DrawContext context, int cx, int y, int hunger) {
        int barWidth = 80;
        int x = cx - barWidth / 2;
        int halfHearts = hunger / 5;
        for (int i = 0; i < 10; i++) {
            int sx = x + i * 8;
            int iconState;
            if (halfHearts >= (i + 1) * 2) {
                iconState = 2;
            } else if (halfHearts == i * 2 + 1) {
                iconState = 1;
            } else {
                iconState = 0;
            }
            int u;
            int v = 27;
            if (iconState == 2) {
                u = 52;
            } else if (iconState == 1) {
                u = 61;
            } else {
                u = 16;
            }
            context.drawTexture(ICONS, sx, y, u, v, 9, 9);
        }
    }

    private double getAttr(EntityAttribute attr) {
        EntityAttributeInstance instance = owner.getAttributeInstance(attr);
        return instance != null ? instance.getValue() : 0;
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String getAttrLabel(String key) {
        return Text.translatable("gui.littlemaidmobresurgence.emotion.stat." + key).getString();
    }

    private static int colorOf(MaidMood.Emotion e) {
        return switch (e) {
            case REBELLION -> 0xFFFF5555;
            case ANGRY -> 0xFFFFAA00;
            case SAD -> 0xFF55AAFF;
            case CALM -> 0xFFAAAAAA;
            case HAPPY -> 0xFF55FF55;
        };
    }
}
