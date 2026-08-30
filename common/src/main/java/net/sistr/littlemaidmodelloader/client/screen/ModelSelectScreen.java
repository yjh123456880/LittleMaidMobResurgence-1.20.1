package net.sistr.littlemaidmodelloader.client.screen;

import dev.architectury.platform.Platform;
import java.awt.Desktop;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.LMMLMod;
import net.sistr.littlemaidmodelloader.client.screen.component.ArmorModelGUI;
import net.sistr.littlemaidmodelloader.client.screen.component.ListScrollNav;
import net.sistr.littlemaidmodelloader.client.screen.component.MultiModelGUI;
import net.sistr.littlemaidmodelloader.client.screen.component.MultiModelGUIUtil;
import net.sistr.littlemaidmodelloader.client.screen.component.ScrollableListGUI;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.network.SyncMultiModelPacket;
import net.sistr.littlemaidmodelloader.resource.loader.LMFileLoader;
import net.sistr.littlemaidmodelloader.resource.holder.TextureHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMModelManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.ArmorPart;
import net.sistr.littlemaidmodelloader.resource.util.ArmorSets;
import net.sistr.littlemaidmodelloader.resource.util.ResourceHelper;
import net.sistr.littlemaidmodelloader.resource.util.TexturePair;
import net.sistr.littlemaidmobresurgence.client.screen.GuiRenderUtil;
import net.sistr.littlemaidmobresurgence.client.screen.LittleMaidScreen;
import net.sistr.littlemaidmobresurgence.client.screen.PinyinMatcher;
import org.lwjgl.glfw.GLFW;

/** 模型/防具选择界面(原版灰容器 + 顶部搜索 + 保留滚轮滚动)。 */
@Environment(EnvType.CLIENT)
public class ModelSelectScreen<T extends Entity & IHasMultiModel> extends Screen {
  public static final Identifier EMPTY_TEXTURE =
      new Identifier(LMMLMod.MODID, "textures/empty.png");
  public static final TexturePair EMPTY_TEXTURE_PAIR = new TexturePair(EMPTY_TEXTURE, null);
  public static final ArmorPart EMPTY_ARMOR_DATA =
      new ArmorPart(null, null, null, null, null, null);
  public static final Identifier MODEL_SELECT_GUI_TEXTURE =
      new Identifier(LMMLMod.MODID, "textures/gui/model_select.png");
  private static final ItemStack ARMOR = Items.DIAMOND_CHESTPLATE.getDefaultStack();
  private static final ItemStack MODEL = Items.ARMOR_STAND.getDefaultStack();
  private static final ItemStack WILD = Items.BONE.getDefaultStack();
  private static final ItemStack CONTRACT = Items.CAKE.getDefaultStack();

  private static final int PANEL_W = 280;
  private static final int PADDING = 8;
  private static final int SCROLL_NAV_GAP = 8;
  private static final int SCROLL_NAV_SIZE = 16;
  private static final int TITLE_H = 14;
  private static final int SEARCH_H = 16;
  private static final int HEADER_H = TITLE_H + 6 + SEARCH_H;
  private static final int LIST_ROWS = 4;
  private static final int LIST_H = 45 * LIST_ROWS;
  private static final int FOOTER_H = 10;
  private static final int PANEL_H = PADDING * 2 + HEADER_H + LIST_H + FOOTER_H;

  private final T entity;
  private final MultiModelGUIUtil.DummyModelEntity dummy;
  private final ArmorSets<ArmorModelGUI> armors = new ArmorSets<>();
  private final int scale = 15;
  private final List<MultiModelGUI> allModelItems = new ArrayList<>();
  private final List<ArmorModelGUI> allArmorItems = new ArrayList<>();
  private Map<String, TextureHolder> textureHolderMap = new HashMap<>();

  private TextFieldWidget searchField;
  private LittleMaidScreen.IconButtonWidget contractButton;
  private ScrollableListGUI<MultiModelGUI> modelList;
  private ScrollableListGUI<ArmorModelGUI> armorList;
  private ListScrollNav<MultiModelGUI> modelScrollNav;
  private ListScrollNav<ArmorModelGUI> armorScrollNav;
  private boolean guiSwitch = true;
  private boolean isContract = true;
  private int panelX, panelY;

  public ModelSelectScreen(Text titleIn, World world, T entity) {
    super(Text.translatable("gui.littlemaidmobresurgence.modelselect.title"));
    this.entity = entity;
    this.dummy = new MultiModelGUIUtil.DummyModelEntity(world);
  }

  @Override
  protected void init() {
    assert this.client != null;
    this.panelX = (this.width - PANEL_W) / 2;
    this.panelY = (this.height - PANEL_H) / 2;
    this.clearChildren();

    Collection<TextureHolder> textureHolders = LMTextureManager.INSTANCE.getAllTextures();
    this.textureHolderMap = new HashMap<>();
    textureHolders.forEach(
        textureHolder -> textureHolderMap.put(textureHolder.getTextureName().toLowerCase(), textureHolder));
    buildModelItems();
    buildArmorItems();

    int searchX = panelX + PADDING;
    int searchY = panelY + PADDING + HEADER_H - SEARCH_H;
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

    // 图标按键组：位于灰色边框外、左下角（关闭X 在最低，向上依次为 契约/防具/模型）
    int iconSize = LittleMaidScreen.IconButtonWidget.DEFAULT_SIZE;
    int iconX = panelX - iconSize - 6;
    int iconBottom = panelY + PANEL_H - 26;
    int iconGap = 24;
    this.addDrawableChild(
        new CloseIconButton(
            iconX,
            iconBottom,
            iconSize,
            iconSize,
            b -> close()));
    this.contractButton =
        this.addDrawableChild(
            new LittleMaidScreen.IconButtonWidget(
                iconX,
                iconBottom - iconGap,
                isContract ? CONTRACT : WILD,
                Text.translatable(
                    isContract
                        ? "gui.littlemaidmobresurgence.modelselect.contract"
                        : "gui.littlemaidmobresurgence.modelselect.wild"),
                b -> toggleContract()));
    this.addDrawableChild(
        new LittleMaidScreen.IconButtonWidget(
            iconX,
            iconBottom - iconGap * 2,
            ARMOR,
            Text.translatable("gui.littlemaidmobresurgence.modelselect.armor"),
            b -> {
              guiSwitch = false;
              playDownSound();
            }));
    this.addDrawableChild(
        new LittleMaidScreen.IconButtonWidget(
            iconX,
            iconBottom - iconGap * 3,
            MODEL,
            Text.translatable("gui.littlemaidmobresurgence.modelselect.model"),
            b -> {
              guiSwitch = true;
              playDownSound();
            }));
    this.addDrawableChild(
        new LittleMaidScreen.IconButtonWidget(
            iconX,
            iconBottom - iconGap * 4,
            Items.PAPER.getDefaultStack(),
            Text.translatable("gui.littlemaidmobresurgence.open_resource_folder"),
            b -> openLMMLResourcesFolder()));
    this.addDrawableChild(
        new RefreshIconButton(
            iconX,
            iconBottom - iconGap * 5,
            iconSize,
            iconSize,
            b -> {
              reloadResources();
              refreshItems();
              playDownSound();
            }));

    this.searchField.setText(prevText);
    restoreModelSelection();
    restoreArmorSelection();
  }

  private void buildModelItems() {
    final LMModelManager modelManager = LMModelManager.INSTANCE;
    allModelItems.clear();
    textureHolderMap.values().stream()
        .sorted(Comparator.comparing(TextureHolder::getTextureName))
        .filter(
            textureHolder ->
                textureHolder.hasSkinTexture(this.isContract)
                    && modelManager
                        .getModel(textureHolder.getModelName(), IHasMultiModel.Layer.SKIN)
                        .isPresent())
        .forEach(
            textureHolder ->
                allModelItems.add(new MultiModelGUI(textureHolder, this.isContract, scale, this.dummy)));
    int listX = panelX + PADDING;
    int listY = panelY + PADDING + HEADER_H + 2;
    this.modelList =
        new ScrollableListGUI<>(
            listX,
            listY,
            1,
            LIST_ROWS,
            scale * 16,
            scale * 3,
            allModelItems,
            false);
    this.modelScrollNav =
        new ListScrollNav<>(
            this.modelList,
            listX + scale * 16 + SCROLL_NAV_GAP,
            listY,
            LIST_H,
            SCROLL_NAV_SIZE);
  }

  private void buildArmorItems() {
    final LMModelManager modelManager = LMModelManager.INSTANCE;
    allArmorItems.clear();
    textureHolderMap.values().stream()
        .sorted(Comparator.comparing(TextureHolder::getTextureName))
        .filter(
            textureHolder ->
                textureHolder.hasArmorTexture()
                    && modelManager
                        .getModel(textureHolder.getModelName(), IHasMultiModel.Layer.INNER)
                        .isPresent())
        .forEach(
            textureHolder ->
                allArmorItems.add(new ArmorModelGUI(textureHolder, scale, this.dummy, this.armors)));
    int listX = panelX + PADDING;
    int listY = panelY + PADDING + HEADER_H + 2;
    this.armorList =
        new ScrollableListGUI<>(
            listX,
            listY,
            1,
            LIST_ROWS,
            scale * 16,
            scale * 3,
            allArmorItems,
            false);
    this.armorScrollNav =
        new ListScrollNav<>(
            this.armorList,
            listX + scale * 16 + SCROLL_NAV_GAP,
            listY,
            LIST_H,
            SCROLL_NAV_SIZE);
  }

  private void onSearchChanged(String text) {
    applyFilter(text);
  }

  private void applyFilter(String keyword) {
    String kw = keyword == null ? "" : keyword.trim();
    List<MultiModelGUI> filteredModels =
        allModelItems.stream()
            .filter(
                g ->
                    kw.isEmpty()
                        || PinyinMatcher.contains(g.getTexture().getTextureName(), kw))
            .toList();
    modelList.clearSelection();
    modelList.setElements(filteredModels);
    modelList.setScroll(0);
    List<ArmorModelGUI> filteredArmors =
        allArmorItems.stream()
            .filter(
                g ->
                    kw.isEmpty()
                        || PinyinMatcher.contains(g.getTexture().getTextureName(), kw))
            .toList();
    armorList.clearSelection();
    armorList.setElements(filteredArmors);
    armorList.setScroll(0);
  }

  private void toggleContract() {
    this.isContract = !this.isContract;
    buildModelItems();
    applyFilter(this.searchField.getText());
    restoreModelSelection();
    if (this.contractButton != null) {
      this.contractButton.setIconItem(isContract ? CONTRACT : WILD);
      this.contractButton.setTooltip(
          Tooltip.of(
              Text.translatable(
                  isContract
                      ? "gui.littlemaidmobresurgence.modelselect.contract"
                      : "gui.littlemaidmobresurgence.modelselect.wild")));
    }
    playDownSound();
  }

  private void restoreModelSelection() {
    TextureHolder ownerSkinTex =
        entity.getTextureHolder(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD);
    var color = entity.getColorMM();
    if (ownerSkinTex != null) {
      modelList.setSelectedBy(
          multiModelGUI -> multiModelGUI.getTexture() == ownerSkinTex,
          multiModelGUI -> multiModelGUI.setSelectColor(color));
    }
  }

  private void restoreArmorSelection() {
    for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
      TextureHolder ownerArmorTex = entity.getTextureHolder(IHasMultiModel.Layer.INNER, part);
      if (ownerArmorTex != null) {
        armorList.setSelectedBy(
            armorModelGUI -> armorModelGUI.getTexture() == ownerArmorTex,
            armorModelGUI -> armorModelGUI.setArmorPart(part, true));
      }
    }
  }

  /** 刷新资源后重建当前界面的模型/防具列表并恢复选择。 */
  private void refreshItems() {
    Collection<TextureHolder> textureHolders = LMTextureManager.INSTANCE.getAllTextures();
    Map<String, TextureHolder> map = new HashMap<>();
    textureHolders.forEach(
        textureHolder -> map.put(textureHolder.getTextureName().toLowerCase(), textureHolder));
    this.textureHolderMap = map;
    buildModelItems();
    buildArmorItems();
    applyFilter(this.searchField.getText());
    restoreModelSelection();
    restoreArmorSelection();
  }

  public static void renderColor(
      DrawContext context, int minX, int minY, int maxX, int maxY, int rgba) {
    context.fill(minX, minY, maxX, maxY, rgba);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(context);
    GuiRenderUtil.drawGrayContainer(context, panelX, panelY, PANEL_W, PANEL_H);

    context.drawText(
        this.textRenderer,
        this.title,
        panelX + PADDING,
        panelY + PADDING + (HEADER_H - SEARCH_H - this.textRenderer.fontHeight) / 2,
        GuiRenderUtil.TEXT_COLOR,
        false);

    this.searchField.render(context, mouseX, mouseY, partialTicks);
    if (this.searchField.getText().isEmpty() && !this.searchField.isFocused()) {
      context.drawText(
          this.textRenderer,
          Text.translatable("gui.littlemaidmobresurgence.modelselect.search_hint"),
          this.searchField.getX() + 4,
          this.searchField.getY() + 4,
          0xFF707070,
          false);
    }

    if (guiSwitch) {
      modelList.render(context, mouseX, mouseY, partialTicks);
      modelScrollNav.render(context, mouseX, mouseY, partialTicks);
      renderEmptyMessage(context, modelList.size());
      renderModelPreview(context, mouseX, mouseY);
    } else {
      armorList.render(context, mouseX, mouseY, partialTicks);
      armorScrollNav.render(context, mouseX, mouseY, partialTicks);
      renderEmptyMessage(context, armorList.size());
      renderArmorPreview(context, mouseX, mouseY);
    }

    super.render(context, mouseX, mouseY, partialTicks);
  }

  private void renderEmptyMessage(DrawContext context, int itemCount) {
    if (itemCount != 0) {
      return;
    }
    String msg = Text.translatable("gui.littlemaidmobresurgence.modelselect.no_results").getString();
    int w = this.textRenderer.getWidth(msg);
    context.drawText(
        this.textRenderer,
        msg,
        panelX + (PANEL_W - w) / 2,
        panelY + PADDING + HEADER_H + LIST_H / 2 - 4,
        0xFF808080,
        false);
  }

  private void renderModelPreview(DrawContext context, int mouseX, int mouseY) {
    modelList
        .getSelectElement()
        .filter(MultiModelGUI::isSelected)
        .ifPresent(
            g ->
                g.getSelectColor()
                    .ifPresent(
                        color -> {
                          TextureHolder texture = g.getTexture();
                          MultiModelGUIUtil.getModel(LMModelManager.INSTANCE, texture)
                              .ifPresent(
                                  model -> {
                                    int size = scale * 3;
                                    MultiModelGUIUtil.getTexturePair(
                                            texture, color, this.isContract)
                                        .ifPresent(
                                            texturePair ->
                                                MultiModelGUIUtil.renderModel(
                                                    context,
                                                    previewX(size),
                                                    previewY(size),
                                                    mouseX,
                                                    mouseY,
                                                    size,
                                                    model,
                                                    texturePair,
                                                    this.dummy));
                                  });
                        }));
  }

  private void renderArmorPreview(DrawContext context, int mouseX, int mouseY) {
    this.armors.foreach(
        (p, g) -> {
          if (g == null) {
            return;
          }
          TextureHolder texture = g.getTexture();
          MultiModelGUIUtil.getModel(LMModelManager.INSTANCE, texture)
              .ifPresent(
                  model -> {
                    int size = scale * 3;
                    ArmorPart armorData =
                        MultiModelGUIUtil.getArmorDate(LMModelManager.INSTANCE, texture, "default");
                    MultiModelGUIUtil.renderArmorPart(
                        context,
                        previewX(size),
                        previewY(size),
                        mouseX,
                        mouseY,
                        size,
                        model,
                        armorData,
                        p,
                        this.dummy);
                  });
        });
  }

  private int previewX(int size) {
    return (this.width + scale * 16 + size * 2) / 2;
  }

  private int previewY(int size) {
    return this.height - size;
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    if (super.mouseClicked(x, y, button)) {
      return true;
    }
    if ((guiSwitch ? modelScrollNav : armorScrollNav).mouseClicked(x, y, button)) {
      return true;
    }
    return (guiSwitch ? modelList : armorList).mouseClicked(x, y, button);
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    if ((guiSwitch ? modelScrollNav : armorScrollNav).mouseReleased(x, y, button)) {
      return true;
    }
    if ((guiSwitch ? modelList : armorList).mouseReleased(x, y, button)) {
      return true;
    }
    return super.mouseReleased(x, y, button);
  }

  @Override
  public boolean mouseDragged(
      double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if ((guiSwitch ? modelScrollNav : armorScrollNav)
        .mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
      return true;
    }
    if ((guiSwitch ? modelList : armorList)
        .mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
  }

  @Override
  public boolean mouseScrolled(double x, double y, double amount) {
    if ((guiSwitch ? modelList : armorList).mouseScrolled(x, y, amount)) {
      return true;
    }
    return super.mouseScrolled(x, y, amount);
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
    modelList
        .getSelectElement()
        .ifPresent(
            g ->
                g.getSelectColor()
                    .ifPresent(
                        color -> {
                          TextureHolder texture = g.getTexture();
                          entity.setColorMM(color);
                          entity.setContractMM(this.isContract);
                          entity.setTextureHolder(
                              texture, IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD);
                          for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
                            entity.setTextureHolder(texture, IHasMultiModel.Layer.INNER, part);
                          }
                        }));

    this.armors.foreach(
        (p, g) -> {
          if (g != null) {
            entity.setTextureHolder(g.getTexture(), IHasMultiModel.Layer.INNER, p);
          }
        });

    ArmorSets<String> armorNames = new ArmorSets<>();
    for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
      armorNames.setArmor(
          entity.getTextureHolder(IHasMultiModel.Layer.INNER, part).getTextureName(), part);
    }
    SyncMultiModelPacket.sendC2SPacket(entity, entity);
  }

  public static void playDownSound() {
    MinecraftClient.getInstance()
        .getSoundManager()
        .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
  }

  @Override
  public boolean shouldPause() {
    return false;
  }

  /** 打开版本目录下的 LMMLResources 文件夹,供玩家放置模型/语音包资源。 */
  public static void openLMMLResourcesFolder() {
    try {
      Path folder = Platform.getGameFolder().resolve("LMMLResources");
      Files.createDirectories(folder);
      boolean opened = false;
      if (Desktop.isDesktopSupported()) {
        try {
          Desktop.getDesktop().open(folder.toFile());
          opened = true;
        } catch (Exception ignored) {
          opened = false;
        }
      }
      if (!opened) {
        new ProcessBuilder("explorer.exe", folder.toString()).start();
      }
    } catch (Exception ignored) {
      // 静默失败,避免影响游戏。
    }
  }

  /** 热加载 LMMLResources 文件夹中的模型/语音包资源。 */
  public static void reloadResources() {
    try {
      // 清空后重载,使删除的资源也能生效
      LMTextureManager.INSTANCE.clear();
      LMConfigManager.INSTANCE.clear();
      // 重新登记资源包/模组内置贴图(避免清空后丢失)
      Collection<Identifier> resourceLocations =
          MinecraftClient.getInstance()
              .getResourceManager()
              .findResources("textures/entity/littlemaid", s -> true)
              .keySet();
      resourceLocations.forEach(
          resourcePath -> {
            String path = resourcePath.getPath();
            ResourceHelper.getTexturePackName(path, false)
                .ifPresent(
                    textureName -> {
                      String modelName = ResourceHelper.getModelName(textureName);
                      int index = ResourceHelper.getIndex(path);
                      if (index != -1) {
                        LMTextureManager.INSTANCE.addTexture(
                            ResourceHelper.getFileName(path, false),
                            textureName,
                            modelName,
                            index,
                            resourcePath);
                      }
                    });
          });
      LMFileLoader.INSTANCE.load();
    } catch (Exception ex) {
      LMMLMod.LOGGER.error("刷新资源失败", ex);
    }
  }

  /** 关闭图标按钮(原版按钮背景 + 手绘 X),表示关闭模型选择界面。 */
  static class CloseIconButton extends ButtonWidget {
    CloseIconButton(int x, int y, int width, int height, PressAction onPress) {
      super(x, y, width, height, Text.empty(), onPress, s -> Text.empty());
      this.setTooltip(Tooltip.of(Text.translatable("gui.littlemaidmobresurgence.modelselect.close")));
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
      super.renderButton(context, mouseX, mouseY, delta);
      int cx = this.getX() + this.width / 2;
      int cy = this.getY() + this.height / 2;
      int r = 3;
      for (int i = -r; i <= r; i++) {
        context.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, 0xFF202020);
        context.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, 0xFF202020);
      }
    }
  }

  /** 刷新资源图标按钮(两个弯折 90° 的像素箭头围成一圈,热加载模型/语音包资源)。 */
  static class RefreshIconButton extends ButtonWidget {
    RefreshIconButton(int x, int y, int width, int height, PressAction onPress) {
      super(x, y, width, height, Text.empty(), onPress, s -> Text.empty());
      this.setTooltip(Tooltip.of(Text.translatable("gui.littlemaidmobresurgence.refresh_resources")));
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
      super.renderButton(context, mouseX, mouseY, delta);
      int c = 0xFF202020;
      int ox = this.getX() + (this.width - 12) / 2;
      int oy = this.getY() + (this.height - 12) / 2;
      // 上箭头: 顶边向右 + 右边向下,头朝下停在垂直中线
      context.fill(ox, oy, ox + 10, oy + 2, c);
      context.fill(ox + 10, oy, ox + 12, oy + 4, c);
      for (int d = 0; d < 3; d++) {
        int rowY = oy + 4 + d;
        int half = 2 - d;
        context.fill(ox + 11 - half, rowY, ox + 11 + half + 1, rowY + 1, c);
      }
      // 下箭头: 底边向左 + 左边向上,头朝上停在垂直中线
      context.fill(ox + 2, oy + 10, ox + 12, oy + 12, c);
      context.fill(ox, oy + 8, ox + 2, oy + 12, c);
      for (int d = 0; d < 3; d++) {
        int rowY = oy + 6 + d;
        int half = d;
        context.fill(ox + 1 - half, rowY, ox + 1 + half + 1, rowY + 1, c);
      }
    }
  }
}
