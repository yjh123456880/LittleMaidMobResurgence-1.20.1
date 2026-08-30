package net.sistr.littlemaidmobresurgence.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.client.screen.ModelSelectScreen;
import net.sistr.littlemaidmodelloader.client.screen.SoundPackSelectScreen;
import net.sistr.littlemaidmodelloader.util.Tuple;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidmobresurgence.entity.MaidMood;
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;
import net.sistr.littlemaidmobresurgence.network.*;

// todo モード名表示/移動状態をアイコンで表記
// todo ストライキ時の表示改善
@Environment(EnvType.CLIENT)
public class LittleMaidScreen extends HandledScreen<LittleMaidScreenHandler> {
    private static final Identifier GUI =
            new Identifier("lmreengaged", "textures/gui/container/littlemaidinventory2.png");
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    private static final ItemStack ARMOR = Items.LEATHER_CHESTPLATE.getDefaultStack();
    private static final ItemStack BOOK = Items.BOOK.getDefaultStack();
    private static final ItemStack NOTE = Items.NOTE_BLOCK.getDefaultStack();
    private static final ItemStack FEATHER = Items.FEATHER.getDefaultStack();
    private static final ItemStack IRON_SWORD = Items.IRON_SWORD.getDefaultStack();
    private static final ItemStack IRON_AXE = Items.IRON_AXE.getDefaultStack();
    private static final ItemStack CHEST = Items.CHEST.getDefaultStack();
    private final LittleMaidEntity owner;
    private Text stateText;
    private MovingMode prevMovingMode;
    private MovingMode movingMode;
    private int workItemSlotSize;
    private boolean isSettingWISS;

    public LittleMaidScreen(
            LittleMaidScreenHandler screenContainer, PlayerInventory inv, Text titleIn) {
        super(screenContainer, inv, titleIn);
        this.backgroundHeight = 208;
        owner = screenContainer.getGuiEntity();
        workItemSlotSize = screenContainer.getWorkItemSlotSize();
        prevMovingMode = movingMode = owner != null ? owner.getMovingMode() : MovingMode.ESCORT;
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
        int left = (int) ((this.width - backgroundWidth) / 2F) - 5;
        int right = (int) ((this.width - backgroundWidth) / 2F) + backgroundWidth + 5;
        int top = (int) ((this.height - backgroundHeight) / 2F);
        int size = 20;
        int layer = -1;
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        BOOK,
                        Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.tooltip.open_target_tag_setting"),
                        button -> OpenTargetTagScreenPacket.sendC2SPacket(this.client.player)));
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        NOTE,
                        Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.tooltip.open_sound_pack_select"),
                        button -> client.setScreen(new SoundPackSelectScreen<>(title, owner))));
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        ARMOR,
                        Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.tooltip.open_model_select"),
                        button ->
                                client.setScreen(
                                        new ModelSelectScreen<>(title, owner.getWorld(), owner))));
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        FEATHER,
                        Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.tooltip.change_moving_mode"),
                        button -> {
                            if (this.owner.isStrike()) {
                                return;
                            }
                            switch (movingMode) {
                                case ESCORT -> movingMode = MovingMode.FREEDOM;
                                case FREEDOM -> movingMode = MovingMode.TRACER;
                                case TRACER -> movingMode = MovingMode.ESCORT;
                                default -> {}
                            }
                            stateText = getStateText();
                            // 立即发送：close() 在跳转子界面时不会触发，延迟发送会导致切换丢失
                            C2SSetMovingStatePacket.sendC2SPacket(this.owner, movingMode);
                            prevMovingMode = movingMode;
                        }));
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        FEATHER,
                        Text.empty(),
                        button ->
                                C2SSetBloodSuckPacket.sendC2SPacket(
                                        this.owner, !this.owner.isBloodSuck())) {
                    private static final Text changeBloodSuck =
                            Text.translatable(
                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_blood_suck");
                    private static final Text toBloodSuck =
                            changeBloodSuck
                                    .copy()
                                    .append(
                                            Text.translatable(
                                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_blood_suck.to_blood_suck"));
                    private static final Text isBloodSuck =
                            changeBloodSuck
                                    .copy()
                                    .append(
                                            Text.translatable(
                                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_blood_suck.is_blood_suck"));

                    @Override
                    public ItemStack getIconItem() {
                        return LittleMaidScreen.this.owner.isBloodSuck() ? IRON_AXE : IRON_SWORD;
                    }

                    @Override
                    protected void renderButton(
                            DrawContext context, int mouseX, int mouseY, float delta) {
                        super.renderButton(context, mouseX, mouseY, delta);

                        setTooltip(
                                Tooltip.of(
                                        LittleMaidScreen.this.owner.isBloodSuck()
                                                ? isBloodSuck
                                                : toBloodSuck));
                    }
                });
        // 自動食事切替（蛋糕图标）：80% → 60% → 40% → 关闭 循环
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        Items.CAKE.getDefaultStack(),
                        Text.empty(),
                        button -> C2SSetAutoEatPacket.sendC2SPacket(owner, nextAutoEat())) {
                    @Override
                    protected void renderButton(
                            DrawContext context, int mouseX, int mouseY, float delta) {
                        super.renderButton(context, mouseX, mouseY, delta);
                        setTooltip(Tooltip.of(getAutoEatTooltip()));
                    }
                });
        // 情绪面板（翡翠图标）
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        Items.EMERALD.getDefaultStack(),
                        Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.tooltip.open_emotion"),
                        button -> client.setScreen(new MaidEmotionScreen(owner))));
        // 饰品界面（鞘翅图标）
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        Items.ELYTRA.getDefaultStack(),
                        Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.tooltip.open_curios"),
                        button -> C2SOpenCuriosScreenPacket.sendC2SPacket(owner)));
        // 扩容背包界面（箱子图标，仅装备扩容道具时显示）
        if (this.handler.getBackpackUpgradeLevel() > 0) {
            this.addDrawableChild(
                    new IconButtonWidget(
                            left - size,
                            top + size * ++layer,
                            Items.CHEST.getDefaultStack(),
                            Text.translatable(
                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.open_backpack"),
                            button -> C2SOpenBackpackScreenPacket.sendC2SPacket(owner)));
        }
        // 捡取掉落物开关（线图标，独立 layer，不覆盖扩容背包箱子按键）
        this.addDrawableChild(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        Items.STRING.getDefaultStack(),
                        Text.empty(),
                        button -> C2SSetPickupItemPacket.sendC2SPacket(
                                owner, !owner.isPickupItem())) {
                    private static final Text changePickup =
                            Text.translatable(
                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_pickup_item");
                    private static final Text toPickup =
                            changePickup.copy()
                                    .append(
                                            Text.translatable(
                                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_pickup_item.to_pickup"));
                    private static final Text isPickup =
                            changePickup.copy()
                                    .append(
                                            Text.translatable(
                                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_pickup_item.is_pickup"));

                    @Override
                    protected void renderButton(
                            DrawContext context, int mouseX, int mouseY, float delta) {
                        super.renderButton(context, mouseX, mouseY, delta);
                        setTooltip(
                                Tooltip.of(
                                        LittleMaidScreen.this.owner.isPickupItem()
                                                ? isPickup
                                                : toPickup));
                    }
                });
        layer = -1;
        this.addDrawableChild(
                new IconButtonWidget(
                        right,
                        top + size * ++layer,
                        BOOK,
                        Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.tooltip.open_maid_manager"),
                        button -> OpenMaidManagerScreenPacket.sendC2SPacket()));

        // 强加载开关（信标图标）：女仆所在区块被强加载，不会被清理/卸载
        this.addDrawableChild(
                new IconButtonWidget(
                        right,
                        top + size * 1,
                        Items.BEACON.getDefaultStack(),
                        Text.empty(),
                        button ->
                                C2SSetForceChunkLoadPacket.sendC2SPacket(
                                        owner, !owner.isForceChunkLoad())) {
                    private static final Text changeForce =
                            Text.translatable(
                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_force_chunk_load");
                    private static final Text toForce =
                            changeForce
                                    .copy()
                                    .append(
                                            Text.translatable(
                                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_force_chunk_load.to_force"));
                    private static final Text isForce =
                            changeForce
                                    .copy()
                                    .append(
                                            Text.translatable(
                                                    "gui.littlemaidmobresurgence.littlemaid.tooltip.change_force_chunk_load.is_force"));

                    @Override
                    protected void renderButton(
                            DrawContext context, int mouseX, int mouseY, float delta) {
                        super.renderButton(context, mouseX, mouseY, delta);
                        setTooltip(
                                Tooltip.of(
                                        LittleMaidScreen.this.owner.isForceChunkLoad()
                                                ? isForce
                                                : toForce));
                    }
                });

        this.addDrawableChild(
                new IconButtonWidget(
                        right,
                        top + 75,
                        CHEST,
                        Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.tooltip.setting_work_item_slot"),
                        button -> isSettingWISS = true));
        stateText = getStateText();
    }

    /** 当前档位的下一个自动进食档位（80→60→40→无时无刻→关闭→80）。 */
    private int nextAutoEat() {
        return switch (owner.getAutoEatThreshold()) {
            case 80 -> 60;
            case 60 -> 40;
            case 40 -> -1;
            case -1 -> 0;
            default -> 80;
        };
    }

    /** 自动进食按钮的实时提示文本。 */
    private Text getAutoEatTooltip() {
        int t = owner.getAutoEatThreshold();
        String stateKey =
                t == -1
                        ? "gui.littlemaidmobresurgence.auto_eat.always"
                        : t == 0
                                ? "gui.littlemaidmobresurgence.auto_eat.off"
                                : "gui.littlemaidmobresurgence.auto_eat." + t;
        return Text.translatable("gui.littlemaidmobresurgence.littlemaid.tooltip.auto_eat")
                .append(": ")
                .append(Text.translatable(stateKey));
    }

    public Text getStateText() {
        if (owner.isStrike()) {
            return Text.translatable("state." + LMMRMod.MODID + ".Strike");
        }
        MutableText stateText =
                Text.translatable("state." + LMMRMod.MODID + "." + movingMode.getName());
        owner.getModeName()
                .ifPresent(
                        modeName ->
                                stateText
                                        .append(" : ")
                                        .append(
                                                Text.translatable(
                                                        "mode." + LMMRMod.MODID + "." + modeName)));
        return stateText;
    }

    /** 女仆GUI主状态：仅显示 当前情绪 + 好感度等级 + 饥饿值。详细数值见情绪面板。 */
    public String getStatusText() {
        MaidMood.Emotion emo = owner.getEmotion();
        String emotion =
                Text.translatable("emotion.littlemaidmobresurgence." + emo.name()).getString();
        // DataTracker 同步值计算等级，客户端才能实时变化
        String level = "Lv" + owner.getFavorabilityLevelValue();
        String hunger =
                Text.translatable(
                                "gui.littlemaidmobresurgence.littlemaid.status.hunger",
                                owner.getHungerValue())
                        .getString();
        return emotion + " " + level + " " + hunger;
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        // 少し重たいかもしれないが、screenを開く直前にsetModeNameした場合に取得がズレるので毎tickやる
        stateText = getStateText();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.render(context, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        InventoryScreen.drawEntity(
                context,
                (this.width - this.backgroundWidth) / 2 + 52,
                (this.height - this.backgroundHeight) / 2 + 59,
                20,
                (this.width - this.backgroundWidth) / 2F + 52 - mouseX,
                (this.height - this.backgroundHeight) / 2F + 30 - mouseY,
                owner);

        if (isSettingWISS) {
            renderWISSSetting(context, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // お仕事アイテムスロット数設定中に、クリックした場合
        if (isSettingWISS) {
            // スロットをクリックしたなら、そのスロットの一つ手前までで設定する
            getMaidSlotPos(mouseX, mouseY)
                    .ifPresent(
                            pos -> {
                                workItemSlotSize = convSlotIndex(pos.getA(), pos.getB());
                                C2SSetWorkItemSlotSizePacket.sendC2SPacket(owner, workItemSlotSize);
                            });

            isSettingWISS = false;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public Optional<Tuple<Integer, Integer>> getMaidSlotPos(double x, double y) {
        float left = (width - backgroundWidth) / 2F;
        float top = (height - backgroundHeight) / 2F;
        float baseLeft = left + 7;
        float baseTop = top + 75;
        int size = 18;
        int slotCol = 9;
        int slotRow = 2;
        if (baseLeft <= x
                && x < baseLeft + size * slotCol
                && baseTop <= y
                && y < baseTop + size * slotRow) {
            int indexX = MathHelper.floor((x - baseLeft) / size);
            int indexY = MathHelper.floor((y - baseTop) / size);
            return Optional.of(new Tuple<>(indexX, indexY));
        }
        return Optional.empty();
    }

    public int convSlotIndex(int x, int y) {
        return y * 9 + x;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        // 状態テキスト（移動状態 : モード名）
        context.drawText(textRenderer, this.stateText.getString(), 8, 65, 0x404040, false);
        // 心情・好感度・空腹値のステータス（右寄せ、与上方状态文字同一基线，避免偏高）
        String status = getStatusText();
        int statusWidth = textRenderer.getWidth(status);
        context.drawText(textRenderer, status, 168 - statusWidth, 65, 0x404040, false);
        float left = (width - backgroundWidth) / 2F;
        float top = (height - backgroundHeight) / 2F;
        if (left + 7 <= mouseX && mouseX < left + 96 && top + 7 <= mouseY && mouseY < top + 60) {
            drawArmor(context);
        } else {
            drawHealth(context, mouseX, mouseY);
        }
    }

    protected void drawHealth(DrawContext context, int mouseX, int mouseY) {
        float left = (width - backgroundWidth) / 2F;
        float top = (height - backgroundHeight) / 2F;
        if (left + 98 <= mouseX
                && mouseX < left + 98 + 5 * 9
                && top + 7 <= mouseY
                && mouseY < top + 7 + 2 * 9) {
            String healthStr =
                    MathHelper.ceil(owner.getHealth())
                            + " / "
                            + MathHelper.ceil(owner.getMaxHealth());
            context.drawText(
                    textRenderer,
                    healthStr,
                    98 + (int) ((5 * 9 - textRenderer.getWidth(healthStr)) / 2F),
                    16 - (int) (textRenderer.fontHeight / 2F),
                    0x404040,
                    false);
        } else {
            float health = (owner.getHealth() / owner.getMaxHealth()) * 20F;
            drawHealth(context, 98, 7, MathHelper.clamp(health - 10, 0, 10), 5);
            drawHealth(context, 98, 16, MathHelper.clamp(health, 0, 10), 5);
        }
        RenderSystem.setShaderTexture(0, GUI);
    }

    protected void drawHealth(DrawContext context, int x, int y, float health, int rowHeart) {
        drawIcon(context, x, y, health, rowHeart, 16, 0, 52, 0, 61, 0);
    }

    protected void drawArmor(DrawContext context) {
        float armor = owner.getArmor();
        drawArmor(context, 98, 7, MathHelper.clamp(armor - 10, 0, 10), 5);
        drawArmor(context, 98, 16, MathHelper.clamp(armor, 0, 10), 5);
    }

    protected void drawArmor(DrawContext context, int x, int y, float health, int rowHeart) {
        drawIcon(context, x, y, health, rowHeart, 16, 9, 34, 9, 25, 9);
    }

    protected void drawIcon(
            DrawContext context,
            int x,
            int y,
            float num,
            int row,
            int baseU,
            int baseV,
            int overU,
            int overV,
            int halfU,
            int halfV) {
        for (int i = 0; i < row; i++) {
            context.drawTexture(ICONS, x + i * 9, y, baseU, baseV, 9, 9);
            if (1 < num) {
                context.drawTexture(ICONS, x + i * 9, y, overU, overV, 9, 9);
            } else if (0 < num) {
                context.drawTexture(ICONS, x + i * 9, y, halfU, halfV, 9, 9);
            }
            num -= 2;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int relX = (this.width - this.backgroundWidth) / 2;
        int relY = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(GUI, relX, relY, 0, 0, this.backgroundWidth, this.backgroundHeight);

        if (!isSettingWISS) {
            drawWorkItemSlotOverlay(context, workItemSlotSize);
        }
    }

    public void renderWISSSetting(DrawContext context, int mouseX, int mouseY, float delta) {
        int relX = (this.width - this.backgroundWidth) / 2;
        int relY = (this.height - this.backgroundHeight) / 2;
        int slotSize = 18;
        int top = relY + 75;
        int bottom = top + slotSize * 2;
        int left = relX + 7;
        int right = left + slotSize * 9;
        int color = 0x80000000;
        // スロットを抜いて黒くする
        context.fill(0, 0, this.width, top, color);
        context.fill(0, top, left, bottom, color);
        context.fill(right, top, this.width, bottom, color);
        context.fill(0, bottom, this.width, this.height, color);

        var optional = getMaidSlotPos(mouseX, mouseY);
        if (optional.isPresent()) {
            var pos = optional.get();
            int index = convSlotIndex(pos.getA(), pos.getB());
            drawWorkItemSlotOverlay(context, index);
        } else {
            drawWorkItemSlotOverlay(context, workItemSlotSize);
        }
    }

    // お仕事アイテムスロットをオーバーレイ表示する
    public void drawWorkItemSlotOverlay(DrawContext context, int num) {
        int relX = (this.width - this.backgroundWidth) / 2;
        int relY = (this.height - this.backgroundHeight) / 2;

        for (int i = 0; i < num; i++) {
            int slotSize = 18;
            // (7, 75)が原点
            int baseX = relX + 7;
            int baseY = relY + 75;
            int x = baseX + slotSize * (i % 9);
            int y = baseY + slotSize * (i / 9);
            context.fill(x, y, x + slotSize, y + slotSize, 0x40FF4040);
        }
    }

    @Override
    public void close() {
        super.close();
        // 兜底：若仍有未同步的变更（理论上点击时已即时发送）
        if (prevMovingMode != movingMode) {
            C2SSetMovingStatePacket.sendC2SPacket(owner, movingMode);
        }
    }

    public static class IconButtonWidget extends ButtonWidget {
        public static final int DEFAULT_SIZE = 20;
        private ItemStack iconItem;

        public IconButtonWidget(
                int x, int y, ItemStack iconItem, Text tooltip, PressAction onPress) {
            this(x, y, DEFAULT_SIZE, DEFAULT_SIZE, Text.empty(), onPress, Supplier::get, iconItem);
            this.setTooltip(Tooltip.of(tooltip));
        }

        public IconButtonWidget(
                int x,
                int y,
                int width,
                int height,
                Text message,
                PressAction onPress,
                NarrationSupplier narrationSupplier,
                ItemStack iconItem) {
            super(x, y, width, height, message, onPress, narrationSupplier);
            this.iconItem = iconItem;
        }

        public ItemStack getIconItem() {
            return iconItem;
        }

        public void setIconItem(ItemStack stack) {
            this.iconItem = stack;
        }

        @Override
        protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderButton(context, mouseX, mouseY, delta);
            context.drawItem(
                    getIconItem(),
                    this.getX() - 8 + this.width / 2,
                    this.getY() - 8 + this.height / 2);
        }
    }
}
