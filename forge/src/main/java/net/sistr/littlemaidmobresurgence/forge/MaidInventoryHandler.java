package net.sistr.littlemaidmobresurgence.forge;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 女仆全物品访问处理器：每次调用【实时】读取主手/副手/物品栏（含扩容背包），不缓存槽位，
 *     避免 CombinedInvWrapper 槽位计数固化/视图过期导致 TACZ 等搜不到背包中的弹药/法术容器。
 *     参考车万女仆枪手兼容：对 ITEM_HANDLER（side=null）返回「背包 + 双手」组合处理器。
 * [en] Live view of the maid's full inventory (main/off hand + inventory incl. expanded backpack), with no slot caching,
 *     so TACZ-style mods never miss ammo/spell containers due to stale wrapper views.
 *     Modeled after Touhou Little Maid's gunner compat: returns a combined "backpack + hands" handler for ITEM_HANDLER (side == null).
 * [ja] メイドの全インベントリ（主手/副手＋インベントリ・拡張バックパック）を毎回リアルタイムに参照するハンドラ。
 *     スロット数をキャッシュしないため、CombinedInvWrapper の固定化・ビュー期限切れで弾薬が見つからない問題を回避。
 *     車万女僕のガンナー互換を参考に、ITEM_HANDLER（side=null）へ「バックパック＋両手」の合成ハンドラを返します。
 */
public final class MaidInventoryHandler implements IItemHandlerModifiable {
    private final LittleMaidEntity maid;

    public MaidInventoryHandler(LittleMaidEntity maid) {
        this.maid = maid;
    }

    @Override
    public int getSlots() {
        return 2 + maid.getInventory().size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0) {
            return maid.getMainHandStack();
        }
        if (slot == 1) {
            return maid.getOffHandStack();
        }
        return maid.getInventory().getStack(slot - 2);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getStackInSlot(slot);
        int canInsert =
                Math.max(0, Math.min(stack.getMaxCount(), getSlotLimit(slot)) - current.getCount());
        int toInsert = Math.min(stack.getCount(), canInsert);
        if (toInsert <= 0) {
            return stack;
        }
        ItemStack remainder = stack.copy();
        remainder.setCount(stack.getCount() - toInsert);
        if (!simulate) {
            ItemStack merged = current.copy();
            merged.setCount(current.getCount() + toInsert);
            setStackInSlot(slot, merged);
        }
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getStackInSlot(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(current.getCount(), amount);
        ItemStack result = current.copy();
        result.setCount(toExtract);
        if (!simulate) {
            ItemStack left = current.copy();
            left.setCount(current.getCount() - toExtract);
            setStackInSlot(slot, left);
        }
        return result;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            maid.setStackInHand(Hand.MAIN_HAND, stack);
        } else if (slot == 1) {
            maid.setStackInHand(Hand.OFF_HAND, stack);
        } else {
            maid.getInventory().setStack(slot - 2, stack);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
