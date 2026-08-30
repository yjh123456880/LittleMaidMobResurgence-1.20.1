package net.sistr.littlemaidmobresurgence.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.sistr.littlemaidmobresurgence.entity.util.GuiEntitySupplier;
import net.sistr.littlemaidmobresurgence.setup.Registration;

/**
 * 女仆扩容背包容器。
 *
 * <p>单页自适应：按扩容道具实际提供的格数（{@link LittleMaidEntity#getBackpackExtraSlots()}，
 * 上限 90）以 9 列布局全部扩容格，玩家背包 + 快捷栏紧跟其后。
 * 仅 5 级（下界合金 90 格）分两页：第 0 页 72 格（4 行）、第 1 页 18 格（1 行）；
 * 其余等级（18/36/54/72）单页显示。
 */
public class BackpackScreenHandler extends ScreenHandler
        implements GuiEntitySupplier<LittleMaidEntity> {
    /** 扩容格列数。 */
    public static final int COLS = 9;
    /** 每页最多行数（4 行 = 72 格）。 */
    private static final int MAX_ROWS = 4;
    /** 每页最大格数。 */
    private static final int PAGE_CAPACITY = COLS * MAX_ROWS;

    private final PlayerInventory playerInventory;
    private final LittleMaidEntity maid;
    private final Inventory extraInventory;
    /** 实际生效的扩容格数（0~90）。 */
    private final int extraSlotCount;
    /** 总页数。 */
    private final int totalPages;
    /** 当前页（0 起）。 */
    private final int page;
    /** 当前页的扩容格数。 */
    private final int currentPageSlotCount;
    /** 当前页玩家背包区起始 y（由当前页扩容格行数决定）。 */
    private final int playerInventoryY;

    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packet) {
        this(syncId, playerInventory, packet.readVarInt(), packet.readVarInt(), packet.readVarInt());
    }

    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, int entityId) {
        this(syncId, playerInventory, entityId, 0);
    }

    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, int entityId, int page) {
        // 服务端路径：从女仆实体实时计算扩容格数
        this(syncId, playerInventory, entityId, page, Integer.MIN_VALUE);
    }

    /**
     * @param declaredExtraSlots 服务端经开屏数据包声明的扩容格数（客户端路径）；
     *                           传 {@link Integer#MIN_VALUE} 表示服务端自行计算
     */
    private BackpackScreenHandler(
            int syncId, PlayerInventory playerInventory, int entityId, int page, int declaredExtraSlots) {
        super(Registration.BACKPACK_SCREEN_HANDLER.get(), syncId);
        this.playerInventory = playerInventory;
        LittleMaidEntity maid =
                (LittleMaidEntity) playerInventory.player.getWorld().getEntityById(entityId);
        this.maid = maid;
        if (maid == null) {
            throw new RuntimeException("メイドさんが存在しません。");
        }
        this.extraInventory = maid.littleMaidInventory.getExtraInventory();
        // 客户端用开屏包声明的格数：客户端的扩容道具槽不经 DataTracker 同步，
        // 重进世界后为空，自行计算恒为 0（首次打开不显示格子的根因）
        this.extraSlotCount = declaredExtraSlots != Integer.MIN_VALUE
                ? Math.max(0, Math.min(declaredExtraSlots, 90))
                : Math.min(maid.getBackpackExtraSlots(), 90);

        // 页数：仅当格数超过单页容量（72）时分页
        this.totalPages = extraSlotCount > PAGE_CAPACITY
                ? (extraSlotCount + PAGE_CAPACITY - 1) / PAGE_CAPACITY
                : 1;
        this.page = Math.max(0, Math.min(page, totalPages - 1));

        // 当前页格数
        int pageStart = this.page * PAGE_CAPACITY;
        this.currentPageSlotCount = Math.min(extraSlotCount - pageStart, PAGE_CAPACITY);

        // 当前页扩容格：9 列，起点 x=8, y=18
        int rows = Math.max(1, (currentPageSlotCount + COLS - 1) / COLS);
        int startY = 18;
        for (int i = 0; i < currentPageSlotCount; i++) {
            addSlot(new Slot(extraInventory, pageStart + i,
                    8 + (i % COLS) * 18, startY + (i / COLS) * 18));
        }
        // 玩家背包 + 快捷栏，紧跟当前页扩容格之后
        this.playerInventoryY = startY + rows * 18 + 14;
        layoutPlayerInventorySlots(8, this.playerInventoryY);
    }

    @Override
    public LittleMaidEntity getGuiEntity() {
        return maid;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.maid != null
                && this.maid.isAlive()
                && this.maid.squaredDistanceTo(player) < 8.0F * 8.0F;
    }

    public int getExtraSlotCount() {
        return extraSlotCount;
    }

    /** 当前页。 */
    public int getPage() {
        return page;
    }

    /** 总页数。 */
    public int getTotalPages() {
        return totalPages;
    }

    /** 当前页扩容格数量。 */
    public int getCurrentPageSlotCount() {
        return currentPageSlotCount;
    }

    /** 当前页玩家背包区起始 y（供客户端计算容器高度）。 */
    public int getPlayerInventoryY() {
        return playerInventoryY;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot == null || !slot.hasStack()) {
            return newStack;
        }
        ItemStack originalStack = slot.getStack();
        newStack = originalStack.copy();
        int pageSlots = this.currentPageSlotCount;
        if (invSlot < pageSlots) { // 当前页扩容格 -> 玩家背包
            if (!this.insertItem(originalStack, pageSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else { // 玩家背包 -> 当前页扩容格
            if (!this.insertItem(originalStack, 0, pageSlots, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (originalStack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return newStack;
    }

    private int addSlotRange(Inventory inventory, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            addSlot(new Slot(inventory, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        int index = addSlotRange(playerInventory, 9, leftCol, topRow, 9, 18);
        index = addSlotRange(playerInventory, index, leftCol, topRow + 18, 9, 18);
        index = addSlotRange(playerInventory, index, leftCol, topRow + 36, 9, 18);
        addSlotRange(playerInventory, 0, leftCol, topRow + 58, 9, 18);
    }
}
