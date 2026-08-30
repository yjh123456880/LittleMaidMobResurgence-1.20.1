package net.sistr.littlemaidmobresurgence.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.sistr.littlemaidmobresurgence.compat.CuriosCompat;
import net.sistr.littlemaidmobresurgence.entity.util.GuiEntitySupplier;
import net.sistr.littlemaidmobresurgence.setup.Registration;
import org.jetbrains.annotations.Nullable;

/**
 * 女仆饰品容器 ScreenHandler（分页）。
 *
 * <p>展示玩家背包 + 女仆的 Curios 饰品槽位（通过 {@link CuriosCompat} 门面由 forge 注入）。
 * 未安装 Curios 时仅含玩家背包，无饰品槽，仍可正常打开。
 *
 * <p>分页布局：第 0 页 = 1 扩容专属槽 + 36 兼容饰品槽；后续页 = 完整 36 兼容饰品槽（无扩容槽）。
 * 饰品槽总数 ≤ 36 时单页不分页。切页通过 C2S 包请求服务端按目标页重开界面（与扩容背包一致）。
 *
 * <p>槽位结构同步：Forge openScreen 先调用 saveExtraData 再 createMenu，故同步在
 * {@code LittleMaidEntity#openCuriosScreen} 中于打开前完成，完整结构（类型-&gt;数量，按槽位顺序）
 * 预先传入工厂，写开屏数据包与构造容器共用同一份；服务端按页截取构建（绑定实时 handler），
 * 客户端从数据包读取完整结构与页号，用同一公式截取镜像构建，
 * 保证两侧每页槽位数量与顺序完全一致（客户端的女仆 handler 会被 Curios 同步包原地缩容，不可直接引用）。
 *
 * <p>布局：饰品网格 9 列对齐玩家背包列，起点统一 y=36（扩容槽下方）；玩家背包区在列表末尾，
 * 其下标 = 本页容器槽数（第0页37/后续页36），由 {@link #backpackSlotStart} 动态记录。
 */
public class CuriosScreenHandler extends ScreenHandler
        implements GuiEntitySupplier<LittleMaidEntity> {
    private final PlayerInventory playerInventory;
    private final LittleMaidEntity maid;

    /** 扩容专属槽顶部（Screen 渲染坐标需读取）。 */
    public static final int BACKPACK_UPGRADE_SLOT_Y = 18;

    /** 饰品网格与扩容槽/玩家背包的最小间距（视觉上紧密贴合 = 2px）。 */
    private static final int SLOT_PADDING = 2;

    /** 单个槽位尺寸（含原版 2px 视觉边框，实际 Slot.x/y 间距使用 18px）。 */
    private static final int SLOT_SIZE = 18;

    /** 饰品网格左起 x，与玩家背包列对齐。 */
    public static final int CURIOS_ORIGIN_X = 8;

    /** 饰品网格列数，对齐玩家背包 9 列，顺序从左到右、从上到下。 */
    public static final int CURIOS_COLS = 9;

    /** 饰品网格顶部 y：扩容槽 y=18 占 18px，下沿 y=36；贴合 = 直接从 36 开始。 */
    public static final int CURIOS_ORIGIN_Y = BACKPACK_UPGRADE_SLOT_Y + SLOT_SIZE;

    /** 第 0 页兼容饰品槽数（另有 1 扩容槽），与后续页满页 36 槽一致，避免一满页被切到下一页。 */
    public static final int FIRST_PAGE_CURIOS = 36;

    /** 后续页兼容饰品槽数（整页 36 格，无扩容槽）。 */
    public static final int LATER_PAGE_CURIOS = 36;

    /** 饰品槽起始下标（扩容专属槽(0，仅第 0 页)之后、玩家背包之前）。 */
    private int curiosSlotStart = -1;

    /** 服务端声明的完整 Curios 槽位结构（类型->数量，按槽位顺序），写入开屏数据包供客户端分页镜像。 */
    private final Map<String, Integer> curiosStructure = new LinkedHashMap<>();

    /** 玩家背包实际起始 y：饰品槽超过 6 行 (54 槽) 时自动下移，确保三区不重叠。 */
    private int playerInventoryY = 144;

    /** 玩家背包区在 slots 列表中的起始下标（列表布局：扩容槽(第0页) -> 本页 Curios 槽 -> 玩家背包(末尾36格)）。 */
    private int backpackSlotStart = 1;

    /** 当前页（0 起）。 */
    private final int page;

    /** 总页数（饰品槽总数 ≤ 36 时恒为 1，不分页）。 */
    private final int totalPages;

    public CuriosScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packet) {
        this(syncId, playerInventory, packet.readVarInt(), packet.readVarInt(), readCuriosStructure(packet));
    }

    public CuriosScreenHandler(
            int syncId, PlayerInventory playerInventory, int entityId, int requestedPage) {
        this(syncId, playerInventory, entityId, requestedPage, null);
    }

    /**
     * 带已声明结构的构造（服务端与客户端共用）。
     *
     * <p>客户端：从开屏数据包读取完整结构镜像构建。服务端：由工厂传入打开前预同步的结构
     * （Forge openScreen 先 saveExtraData 后 createMenu，结构必须在打开前同步完成），
     * 此时跳过构造内的二次同步；declaredStructure 为 null 时服务端自行同步。
     */
    public CuriosScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            int entityId,
            int requestedPage,
            @Nullable Map<String, Integer> declaredStructure) {
        super(Registration.CURIOS_SCREEN_HANDLER.get(), syncId);
        this.playerInventory = playerInventory;
        LittleMaidEntity maid =
                (LittleMaidEntity) playerInventory.player.getWorld().getEntityById(entityId);
        this.maid = maid;
        if (maid == null) {
            throw new RuntimeException("メイドさんが存在しません。");
        }

        // 完整饰品结构：客户端读开屏包声明；服务端先同步（镜像主人结构）再取同步结果。
        // 分页固定每页格数（第0页36/后续36），玩家背包下标两端恒定，从根上杜绝槽位错位
        Map<String, Integer> full;
        if (declaredStructure != null) {
            full = declaredStructure;
        } else {
            full = CuriosCompat.syncCuriosStructure(maid, playerInventory.player);
        }
        // 分页计算：第 0 页 36 槽（+1 扩容槽），后续页整页 36 槽；总数 ≤ 36 时单页
        int totalCurios = 0;
        for (int c : full.values()) {
            totalCurios += c;
        }
        this.totalPages = totalCurios <= FIRST_PAGE_CURIOS
                ? 1
                : 1 + (totalCurios - FIRST_PAGE_CURIOS + LATER_PAGE_CURIOS - 1) / LATER_PAGE_CURIOS;
        this.page = Math.max(0, Math.min(requestedPage, this.totalPages - 1));
        // 记录完整结构供工厂写开屏包（客户端据此计算相同分页）
        this.setCuriosStructure(full);

        // 背包扩容专属槽：仅第 0 页顶部。独立 vanilla Slot（非 Curios handler），仅可放入 5 种扩容道具。
        if (this.page == 0) {
            this.addSlot(
                    new Slot(maid.getBackpackUpgradeSlot(), 0, 8, BACKPACK_UPGRADE_SLOT_Y) {
                        @Override
                        public boolean canInsert(ItemStack stack) {
                            return net.sistr.littlemaidmobresurgence.item.BackpackUpgradeItem.isUpgrade(stack);
                        }

                        @Override
                        public int getMaxItemCount() {
                            return 1;
                        }
                    });
        }
        // 本页饰品槽：按完整结构截取当前页区间（类型→[类型内起始下标,数量]，类型可跨页切割）
        curiosSlotStart = this.slots.size();
        Map<String, int[]> pageSlice = sliceCuriosPage(full, this.page);
        if (!pageSlice.isEmpty()) {
            CuriosCompat.addCuriosSlots(this, maid, pageSlice);
        }
        // 按本页饰品占用高度计算玩家背包起始 y（第0页36格与后续页36格均为 4 行，网格起点统一 y=36）
        int curiosCount = this.slots.size() - curiosSlotStart;
        int curiosRows = curiosCount == 0 ? 0 : (curiosCount - 1) / CURIOS_COLS + 1;
        int curiosBottom = curiosRows == 0 ? CURIOS_ORIGIN_Y : CURIOS_ORIGIN_Y + curiosRows * SLOT_SIZE;
        this.playerInventoryY = Math.max(144, curiosBottom + SLOT_PADDING + 2);
        this.backpackSlotStart = this.slots.size();
        layoutPlayerInventorySlots(8, this.playerInventoryY);
    }

    /** 从开屏数据包读取服务端声明的槽位结构（类型->数量，保持写入顺序）。 */
    private static Map<String, Integer> readCuriosStructure(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Integer> structure = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            structure.put(buf.readString(), buf.readVarInt());
        }
        return structure;
    }

    /**
     * 截取指定页的饰品槽区间（类型 → [类型内起始下标, 数量]）。
     *
     * <p>槽位计数跨类型连续累计：第 0 页取前 {@link #FIRST_PAGE_CURIOS}=36 个（另有 1 扩容槽），
     * 后续页每页整页 {@link #LATER_PAGE_CURIOS}=36 个；单个类型跨页时按区间切割（起始下标偏移）。
     * 服务端与客户端使用同一份完整结构 + 同一公式截取，保证两侧槽位数量与顺序严格一致。
     */
    public static Map<String, int[]> sliceCuriosPage(Map<String, Integer> fullStructure, int page) {
        int pageSize = page == 0 ? FIRST_PAGE_CURIOS : LATER_PAGE_CURIOS;
        int startSlot = page == 0 ? 0 : FIRST_PAGE_CURIOS + (page - 1) * LATER_PAGE_CURIOS;
        int endSlot = startSlot + pageSize;
        Map<String, int[]> slice = new LinkedHashMap<>();
        int cursor = 0;
        for (Map.Entry<String, Integer> e : fullStructure.entrySet()) {
            int typeStart = cursor;
            int typeEnd = cursor + e.getValue();
            cursor = typeEnd;
            int s = Math.max(typeStart, startSlot);
            int t = Math.min(typeEnd, endSlot);
            if (t > s) {
                slice.put(e.getKey(), new int[] {s - typeStart, t - s});
            }
        }
        return slice;
    }

    /** 背包扩容道具专属槽在 slots 中的下标（仅第 0 页存在，列表首位；后续页无此槽）。 */
    public int getBackpackUpgradeSlotStart() {
        return page == 0 ? 0 : -1;
    }

    @Override
    public LittleMaidEntity getGuiEntity() {
        return maid;
    }

    /** 打开此容器的玩家（服务端包校验已保证其必为女仆主人）。 */
    public PlayerEntity getPlayer() {
        return playerInventory.player;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.maid != null
                && this.maid.isAlive()
                && this.maid.squaredDistanceTo(player) < 8.0F * 8.0F;
    }

    /** 暴露给 forge 门面跨模块添加槽位（protected addSlot 的 public 包装）。 */
    public Slot addSlotPublic(Slot slot) {
        return this.addSlot(slot);
    }

    /** 是否有 Curios 饰品槽（供客户端渲染判断）。 */
    public int getCuriosSlotStart() {
        return curiosSlotStart;
    }

    /** 玩家背包 3 行主背包 + 1 行快捷栏起始 y；饰品区较大时会自动下移，Screen 据此计算容器高度。 */
    public int getPlayerInventoryY() {
        return playerInventoryY;
    }

    /** 当前页（0 起）。 */
    public int getPage() {
        return page;
    }

    /** 总页数（饰品槽总数 ≤ 36 时恒为 1，不分页）。 */
    public int getTotalPages() {
        return totalPages;
    }

    /** 服务端由 Curios 适配器在构建槽位后记录最终槽位结构（与实际槽位一一对应）。 */
    public void setCuriosStructure(Map<String, Integer> structure) {
        this.curiosStructure.clear();
        this.curiosStructure.putAll(structure);
    }

    /** 最终 Curios 槽位结构（服务端写入开屏数据包；客户端为镜像后的同一结构）。 */
    public Map<String, Integer> getCuriosStructure() {
        return curiosStructure;
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
        // 实际槽位布局：扩容专属槽(仅第0页,下标0) -> 本页 Curios 饰品槽 -> 玩家背包(backpackSlotStart..末尾)
        // 分支边界以布局为准，不再使用写死的下标（原写死索引与实际构建顺序错位，
        // 会把玩家背包物品错误转移进饰品槽区，表现为物品栏物品"消失"）
        if (invSlot < this.backpackSlotStart) { // 扩容槽/Curios 槽 -> 玩家背包
            if (!this.insertItem(originalStack, this.backpackSlotStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else { // 玩家背包 -> 扩容道具优先进专属槽（仅第 0 页存在），否则进本页饰品槽
            boolean moved = false;
            if (this.page == 0
                    && net.sistr.littlemaidmobresurgence.item.BackpackUpgradeItem.isUpgrade(originalStack)) {
                moved = this.insertItem(originalStack, 0, 1, false);
            }
            if (!moved && curiosSlotStart >= 0 && curiosSlotStart < this.backpackSlotStart) {
                moved = this.insertItem(originalStack, curiosSlotStart, this.backpackSlotStart, false);
            }
            if (!moved) {
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

    private int addSlotRange(PlayerInventory inventory, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            addSlot(new Slot(inventory, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        // 玩家背包 0~26（3 行 9 列）
        int index = addSlotRange(playerInventory, 9, leftCol, topRow, 9, 18);
        index = addSlotRange(playerInventory, index, leftCol, topRow + 18, 9, 18);
        index = addSlotRange(playerInventory, index, leftCol, topRow + 36, 9, 18);
        // 快捷栏 27~35
        addSlotRange(playerInventory, 0, leftCol, topRow + 58, 9, 18);
    }
}
