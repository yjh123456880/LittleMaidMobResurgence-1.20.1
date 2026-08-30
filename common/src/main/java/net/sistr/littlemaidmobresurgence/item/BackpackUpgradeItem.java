package net.sistr.littlemaidmobresurgence.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmobresurgence.setup.Registration;

/**
 * 女仆背包扩容道具。
 *
 * <p>5 个等级：铜/铁/金/钻石/下界合金，每级额外提供 18 个背包格子。
 * 不可堆叠，仅可放入女仆专属扩容槽位。
 */
public class BackpackUpgradeItem extends Item {

    /** 扩容等级：等级值即扩容倍数（1~5），每级 +18 格。 */
    public enum UpgradeLevel {
        COPPER(1),
        IRON(2),
        GOLD(3),
        DIAMOND(4),
        NETHERITE(5);

        private final int level;

        UpgradeLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        /** 该等级提供的额外格子数 = 等级 × 18。 */
        public int getBonusSlots() {
            return level * 18;
        }
    }

    private final UpgradeLevel upgradeLevel;

    public BackpackUpgradeItem(UpgradeLevel upgradeLevel) {
        super(new Item.Settings().maxCount(1).arch$tab(Registration.ITEM_GROUP));
        this.upgradeLevel = upgradeLevel;
    }

    public UpgradeLevel getUpgradeLevel() {
        return upgradeLevel;
    }

    /** 是否为扩容道具。 */
    public static boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof BackpackUpgradeItem;
    }

    /** 获取物品的扩容等级（非扩容道具返回 0）。 */
    public static int getLevel(ItemStack stack) {
        if (stack.getItem() instanceof BackpackUpgradeItem upgrade) {
            return upgrade.upgradeLevel.getLevel();
        }
        return 0;
    }

    /** 获取物品对应的额外格子数（非扩容道具返回 0）。 */
    public static int getBonusSlots(ItemStack stack) {
        if (stack.getItem() instanceof BackpackUpgradeItem upgrade) {
            return upgrade.upgradeLevel.getBonusSlots();
        }
        return 0;
    }
}
