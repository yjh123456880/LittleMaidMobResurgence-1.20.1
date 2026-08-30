package net.sistr.littlemaidmodelloader.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.sistr.littlemaidmodelloader.client.screen.component.GUIElement;
import net.sistr.littlemaidmodelloader.client.screen.component.ListScrollNav;
import net.sistr.littlemaidmodelloader.client.screen.component.ListGUIElement;
import net.sistr.littlemaidmodelloader.client.screen.component.ScrollableListGUI;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.network.SyncSoundPackPacket;
import net.sistr.littlemaidmodelloader.resource.holder.ConfigHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmobresurgence.client.screen.GuiRenderUtil;
import net.sistr.littlemaidmobresurgence.client.screen.LittleMaidScreen;
import net.sistr.littlemaidmobresurgence.client.screen.PinyinMatcher;
import org.lwjgl.glfw.GLFW;

/** 语音包选择界面(原版灰容器 + 顶部搜索 + 保留滚轮滚动)。 */
public class SoundPackSelectScreen<T extends Entity & SoundPlayable> extends Screen {
  private static final int PANEL_W = 280;
  private static final int PADDING = 8;
  private static final int SCROLL_NAV_GAP = 8;
  private static final int SCROLL_NAV_SIZE = 16;
  private static final int TITLE_H = 14;
  private static final int SEARCH_H = 16;
  private static final int HEADER_H = TITLE_H + 6 + SEARCH_H;
  private static final int LIST_ROWS = 4;
  /** 单个语音包条目高度，与模型选择界面条目一致（scale*3 = 45），保证画面尺寸相同。 */
  private static final int ELEMENT_H = 45;
  private static final int FOOTER_H = 10;
  private static final int ENTRY_GAP = 3;

  private final T entity;
  private final List<SoundPackGUI> allItems = new ArrayList<>();
  private TextFieldWidget searchField;
  private ScrollableListGUI<SoundPackGUI> list;
  private ListScrollNav<SoundPackGUI> scrollNav;
  private int panelX, panelY;
  private int listX, listY, listH, elementW, elementH, panelH;

  public SoundPackSelectScreen(Text titleIn, T owner) {
    super(Text.translatable("gui.littlemaidmobresurgence.soundpack.title"));
    this.entity = owner;
  }

  @Override
  protected void init() {
    assert this.client != null;
    this.elementW = 240;
    this.elementH = ELEMENT_H;
    this.listH = this.elementH * LIST_ROWS;
    this.panelH = PADDING * 2 + HEADER_H + this.listH + FOOTER_H;
    this.panelX = (this.width - PANEL_W) / 2;
    this.panelY = (this.height - this.panelH) / 2;
    this.listX = this.panelX + PADDING;
    this.listY = this.panelY + PADDING + HEADER_H + 2;
    this.clearChildren();

    this.allItems.clear();
    LMConfigManager.INSTANCE.getAllConfig().stream()
        .map(c -> new SoundPackGUI(this.elementW, this.elementH, this.textRenderer, c))
        .forEach(this.allItems::add);
    this.list =
        new ScrollableListGUI<>(
            this.listX,
            this.listY,
            1,
            LIST_ROWS,
            this.elementW,
            this.elementH,
            this.allItems,
            false);
    this.scrollNav =
        new ListScrollNav<>(
            this.list,
            this.listX + this.elementW + SCROLL_NAV_GAP,
            this.listY,
            this.listH,
            SCROLL_NAV_SIZE);

    int searchX = this.panelX + PADDING;
    int searchY = this.panelY + PADDING + HEADER_H - SEARCH_H;
    int searchW = PANEL_W - PADDING * 2;
    String prevText = this.searchField != null ? this.searchField.getText() : "";
    this.searchField =
        new TextFieldWidget(
            this.textRenderer,
            searchX,
            searchY,
            searchW,
            SEARCH_H,
            Text.translatable("gui.littlemaidmobresurgence.search"));
    this.searchField.setChangedListener(this::onSearchChanged);
    this.searchField.setDrawsBackground(true);
    this.searchField.setEditableColor(0x303030);
    this.searchField.setMaxLength(64);
    this.addSelectableChild(this.searchField);

    // 图标按键组:位于灰色边框外、左下角(关闭X 在下,资源(纸) 在上)
    int iconSize = LittleMaidScreen.IconButtonWidget.DEFAULT_SIZE;
    int iconX = this.panelX - iconSize - 6;
    int iconBottom = this.panelY + this.panelH - 26;
    int iconGap = 24;
    this.addDrawableChild(
        new ModelSelectScreen.CloseIconButton(
            iconX, iconBottom, iconSize, iconSize, b -> close()));
    this.addDrawableChild(
        new LittleMaidScreen.IconButtonWidget(
            iconX,
            iconBottom - iconGap,
            Items.PAPER.getDefaultStack(),
            Text.translatable("gui.littlemaidmobresurgence.open_resource_folder"),
            b -> ModelSelectScreen.openLMMLResourcesFolder()));
    this.addDrawableChild(
        new ModelSelectScreen.RefreshIconButton(
            iconX,
            iconBottom - iconGap * 2,
            iconSize,
            iconSize,
            b -> {
              ModelSelectScreen.reloadResources();
              refreshItems();
              ModelSelectScreen.playDownSound();
            }));

    this.searchField.setText(prevText);
  }

  private void onSearchChanged(String text) {
    applyFilter(text);
  }

  private void applyFilter(String keyword) {
    String kw = keyword == null ? "" : keyword.trim();
    List<SoundPackGUI> filtered =
        this.allItems.stream()
            .filter(g -> kw.isEmpty() || PinyinMatcher.contains(g.getSearchText(), kw))
            .toList();
    this.list.clearSelection();
    this.list.setElements(filtered);
    this.list.setScroll(0);
  }

  /** 刷新资源后重建语音包列表。 */
  private void refreshItems() {
    this.allItems.clear();
    LMConfigManager.INSTANCE.getAllConfig().stream()
        .map(c -> new SoundPackGUI(this.elementW, this.elementH, this.textRenderer, c))
        .forEach(this.allItems::add);
    applyFilter(this.searchField.getText());
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    this.renderBackground(context);
    GuiRenderUtil.drawGrayContainer(context, this.panelX, this.panelY, PANEL_W, this.panelH);

    context.drawText(
        this.textRenderer,
        this.title,
        this.panelX + PADDING,
        this.panelY + PADDING + (HEADER_H - SEARCH_H - this.textRenderer.fontHeight) / 2,
        GuiRenderUtil.TEXT_COLOR,
        false);

    this.searchField.render(context, mouseX, mouseY, delta);
    if (this.searchField.getText().isEmpty() && !this.searchField.isFocused()) {
      context.drawText(
          this.textRenderer,
          Text.translatable("gui.littlemaidmobresurgence.soundpack.search_hint"),
          this.searchField.getX() + 4,
          this.searchField.getY() + 4,
          0xFF707070,
          false);
    }

    this.list.render(context, mouseX, mouseY, delta);
    this.scrollNav.render(context, mouseX, mouseY, delta);
    if (this.list.size() == 0) {
      String msg = Text.translatable("gui.littlemaidmobresurgence.soundpack.no_results").getString();
      int w = this.textRenderer.getWidth(msg);
      context.drawText(
          this.textRenderer,
          msg,
          this.panelX + (PANEL_W - w) / 2,
          this.listY + this.listH / 2 - 4,
          0xFF808080,
          false);
    }

    super.render(context, mouseX, mouseY, delta);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    if (this.scrollNav.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    return this.list.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (this.scrollNav.mouseReleased(mouseX, mouseY, button)) {
      return true;
    }
    if (this.list.mouseReleased(mouseX, mouseY, button)) {
      return true;
    }
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(
      double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if (this.scrollNav.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
      return true;
    }
    if (this.list.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    if (this.list.mouseScrolled(mouseX, mouseY, amount)) {
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, amount);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      close();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char chr, int modifiers) {
    if (this.searchField.charTyped(chr, modifiers)) {
      return true;
    }
    return super.charTyped(chr, modifiers);
  }

  @Override
  public void removed() {
    super.removed();
    this.list
        .getSelectElement()
        .ifPresent(gui -> SyncSoundPackPacket.sendC2SPacket(this.entity, gui.getConfigHolder()));
  }

  @Override
  public boolean shouldPause() {
    return false;
  }

  public static class SoundPackGUI extends GUIElement implements ListGUIElement {
    private final TextRenderer textRenderer;
    private final ConfigHolder configHolder;
    private boolean selected;

    protected SoundPackGUI(
        int width, int height, TextRenderer textRenderer, ConfigHolder configHolder) {
      super(width, height);
      this.textRenderer = textRenderer;
      this.configHolder = configHolder;
    }

    public String getSearchText() {
      return configHolder.getPackName()
          + " "
          + configHolder.getParentName()
          + " "
          + configHolder.getFileName();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      // 黑色条目底,底部留 gap 与下一条目分隔
      int contentH = this.height - ENTRY_GAP;
      context.fill(this.x, this.y, this.x + this.width, this.y + contentH, 0xFF000000);
      int lineH = this.textRenderer.fontHeight + 1;
      context.drawText(
          this.textRenderer,
          configHolder.getPackName(),
          this.x + 6,
          this.y + 3,
          0xFFFFFFFF,
          false);
      context.drawText(
          this.textRenderer,
          configHolder.getParentName(),
          this.x + 6,
          this.y + 3 + lineH,
          0xFFFFFFFF,
          false);
      context.drawText(
          this.textRenderer,
          configHolder.getFileName(),
          this.x + 6,
          this.y + 3 + lineH * 2,
          0xFFFFFFFF,
          false);
      if (this.selected) {
        context.fill(this.x, this.y, this.x + this.width, this.y + contentH, 0x80FFFFFF);
      }
    }

    @Override
    public void setSelected(boolean selected) {
      this.selected = selected;
    }

    @Override
    public boolean isSelected() {
      return this.selected;
    }

    public ConfigHolder getConfigHolder() {
      return configHolder;
    }
  }
}
