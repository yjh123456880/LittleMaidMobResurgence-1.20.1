package net.sistr.littlemaidmobresurgence.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.util.HasInventory;

/**
 * [zh] 女仆背包实现：基础 18 格 + 动态扩容格（最大 90 格，由扩容道具等级决定），并支持工作物品槽组合视图。
 * [en] Maid inventory: base 18 slots + dynamic extra slots (up to 90, driven by the backpack-upgrade item level),
 *     with a combined view including work-item slots.
 * [ja] メイドのインベントリ実装：基本18スロット＋動的拡張スロット（最大90、拡張アイテムのレベルで決定）。
 *     作業アイテムスロットを含む合成ビューも提供します。
 */
public class LMHasInventory implements HasInventory {
    private final Inventory inventory;
    /**
     * [zh] 额外扩容背包（最大 90 = 5 级 × 18）。有效范围由 {@link #extraSlotCount} 决定。
     * [en] Extra backpack slots (max 90 = 5 levels × 18); the effective range is governed by {@link #extraSlotCount}.
     * [ja] 追加バックパック（最大90＝5レベル×18）。有効範囲は {@link #extraSlotCount} が決定します。
     */
    private final SimpleInventory extraInventory = new SimpleInventory(90);
    /**
     * [zh] 当前生效的额外格数（0-90），由女仆扩容道具等级决定。
     * [en] Currently effective extra slot count (0-90), determined by the maid's backpack-upgrade level.
     * [ja] 現在有効な追加スロット数（0〜90）。拡張アイテムのレベルで決まります。
     */
    private int extraSlotCount = 0;
    private int workItemSlotSize = LMMRMod.getConfig().work.defaultWorkItemSlotSize;

    public LMHasInventory() {
        this.inventory = new SimpleInventory(18);
    }

    public LMHasInventory(int workItemSlotSize) {
        this.inventory = new SimpleInventory(18);
        this.workItemSlotSize = workItemSlotSize;
    }

    @Override
    public Inventory getInventory() {
        // [zh] 组合视图：基础 18 格 + 扩容格（extraInventory 前 extraSlotCount 格）
        // [en] Combined view: base 18 slots + extra slots (first extraSlotCount of extraInventory).
        // [ja] 合成ビュー：基本18スロット＋拡張スロット（extraInventory の先頭 extraSlotCount 個）。
        Inventory base = this.inventory;
        SimpleInventory extra = this.extraInventory;
        return new Inventory() {
            @Override
            public int size() {
                // 动态读取当前扩容格数：Forge 能力（InvWrapper）在女仆出生时抓取一次本视图，
                // 若在此快照扩容格数，读档/升级后的扩容背包会"看不见"新格子（弹药/聚晶/魔法书检索失效）
                return 18 + LMHasInventory.this.extraSlotCount;
            }

            @Override
            public boolean isEmpty() {
                return base.isEmpty();
            }

            @Override
            public ItemStack getStack(int slot) {
                return slot < 18 ? base.getStack(slot) : extra.getStack(slot - 18);
            }

            @Override
            public ItemStack removeStack(int slot, int amount) {
                return slot < 18 ? base.removeStack(slot, amount) : extra.removeStack(slot - 18, amount);
            }

            @Override
            public ItemStack removeStack(int slot) {
                return slot < 18 ? base.removeStack(slot) : extra.removeStack(slot - 18);
            }

            @Override
            public void setStack(int slot, ItemStack stack) {
                if (slot < 18) {
                    base.setStack(slot, stack);
                } else {
                    extra.setStack(slot - 18, stack);
                }
            }

            @Override
            public void markDirty() {
                base.markDirty();
                extra.markDirty();
            }

            @Override
            public boolean canPlayerUse(PlayerEntity player) {
                return base.canPlayerUse(player);
            }

            @Override
            public void clear() {
                base.clear();
            }
        };
    }

    /** 额外扩容背包（扩容格，0~89）。供扩容界面直接使用。 */
    public Inventory getExtraInventory() {
        return extraInventory;
    }

    public int getExtraSlotCount() {
        return extraSlotCount;
    }

    public void setExtraSlotCount(int extraSlotCount) {
        this.extraSlotCount = Math.max(0, Math.min(90, extraSlotCount));
    }

    public int getWorkItemSlotSize() {
        return workItemSlotSize;
    }

    public void setWorkItemSlotSize(int workItemSlotSize) {
        this.workItemSlotSize = workItemSlotSize;
    }

    @Override
    public void writeInventory(NbtCompound nbt) {
        nbt.put("Inventory", this.writeNbt(new NbtList()));
        nbt.putByte("workItemSlotSize", (byte) this.workItemSlotSize);
        nbt.put("ExtraInventory", this.writeExtraNbt(new NbtList()));
    }

    @Override
    public void readInventory(NbtCompound nbt) {
        int maidVersion = nbt.getByte("maidVersion") & 255;
        if (maidVersion == 0) {
            this.readNbtOld(nbt.getList("Inventory", 10));
        } else {
            this.readNbt(nbt.getList("Inventory", 10));
        }
        if (nbt.contains("workItemSlotSize")) {
            this.workItemSlotSize = nbt.getByte("workItemSlotSize") & 255;
        }
        if (nbt.contains("ExtraInventory")) {
            this.readExtraNbt(nbt.getList("ExtraInventory", 10));
        }
    }

    public NbtList writeNbt(NbtList nbtList) {
        int i;
        NbtCompound nbt;
        for (i = 0; i < 18; ++i) {
            var stack = this.inventory.getStack(i);
            if (!stack.isEmpty()) {
                nbt = new NbtCompound();
                nbt.putByte("Slot", (byte) i);
                stack.writeNbt(nbt);
                nbtList.add(nbt);
            }
        }

        return nbtList;
    }

    public void readNbt(NbtList nbtList) {
        this.inventory.clear();

        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            ItemStack stack = ItemStack.fromNbt(nbtCompound);
            if (!stack.isEmpty()) {
                if (j < 18) {
                    this.inventory.setStack(j, stack);
                }
            }
        }
    }

    public void readNbtOld(NbtList nbtList) {
        this.inventory.clear();

        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            ItemStack stack = ItemStack.fromNbt(nbtCompound);
            if (!stack.isEmpty()) {
                if (1 <= j && j <= 18) {
                    this.inventory.setStack(j - 1, stack);
                }
            }
        }
    }

    /** 扩容格 NBT 序列化（0~89）。 */
    public NbtList writeExtraNbt(NbtList nbtList) {
        for (int i = 0; i < 90; ++i) {
            var stack = this.extraInventory.getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound nbt = new NbtCompound();
                nbt.putByte("Slot", (byte) i);
                stack.writeNbt(nbt);
                nbtList.add(nbt);
            }
        }
        return nbtList;
    }

    public void readExtraNbt(NbtList nbtList) {
        this.extraInventory.clear();
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            ItemStack stack = ItemStack.fromNbt(nbtCompound);
            if (!stack.isEmpty() && j < 90) {
                this.extraInventory.setStack(j, stack);
            }
        }
    }

    public static Inventory getInvAndHands(LittleMaidEntity maid) {
        var inv = maid.getInventory();
        return new Inventory() {
            @Override
            public int size() {
                return 20;
            }

            @Override
            public boolean isEmpty() {
                return inv.isEmpty()
                        && maid.getMainHandStack().isEmpty()
                        && maid.getOffHandStack().isEmpty();
            }

            @Override
            public ItemStack getStack(int slot) {
                if (slot == 0) {
                    return maid.getMainHandStack();
                } else if (slot == 1) {
                    return maid.getOffHandStack();
                }
                return inv.getStack(slot - 2);
            }

            @Override
            public ItemStack removeStack(int slot, int amount) {
                if (slot == 0) {
                    ItemStack itemStack = maid.getMainHandStack();
                    if (itemStack.isEmpty() || amount <= 0) {
                        return ItemStack.EMPTY;
                    }
                    itemStack = itemStack.split(amount);
                    if (!itemStack.isEmpty()) {
                        this.markDirty();
                    }
                    return itemStack;
                } else if (slot == 1) {
                    ItemStack itemStack = maid.getOffHandStack();
                    if (itemStack.isEmpty() || amount <= 0) {
                        return ItemStack.EMPTY;
                    }
                    itemStack = itemStack.split(amount);
                    if (!itemStack.isEmpty()) {
                        this.markDirty();
                    }
                    return itemStack;
                }
                return inv.removeStack(slot - 2, amount);
            }

            @Override
            public ItemStack removeStack(int slot) {
                if (slot == 0) {
                    var stack = maid.getMainHandStack();
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    maid.setStackInHand(Hand.MAIN_HAND, stack);
                    return stack;
                } else if (slot == 1) {
                    var stack = maid.getOffHandStack();
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    maid.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                    return stack;
                }
                return inv.removeStack(slot - 2);
            }

            @Override
            public void setStack(int slot, ItemStack stack) {
                if (slot == 0) {
                    maid.setStackInHand(Hand.MAIN_HAND, stack);
                } else if (slot == 1) {
                    maid.setStackInHand(Hand.OFF_HAND, stack);
                } else {
                    inv.setStack(slot - 2, stack);
                }
            }

            @Override
            public void markDirty() {
                inv.markDirty();
            }

            @Override
            public boolean canPlayerUse(PlayerEntity player) {
                return inv.canPlayerUse(player);
            }

            @Override
            public void clear() {
                inv.clear();
            }
        };
    }
}
