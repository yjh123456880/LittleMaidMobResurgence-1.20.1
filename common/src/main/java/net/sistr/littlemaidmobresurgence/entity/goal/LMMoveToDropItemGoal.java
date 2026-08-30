package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.util.LMCollidable;

public class LMMoveToDropItemGoal extends MoveToDropItemGoal {
    protected final LittleMaidEntity maid;

    public LMMoveToDropItemGoal(
            LittleMaidEntity maid,
            Supplier<Float> range,
            Supplier<Integer> frequency,
            Supplier<Float> speed) {
        super(maid, range, frequency, speed);
        this.maid = maid;
    }

    @Override
    public boolean isInventoryFull() {
        var inv = this.maid.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 背包能否放入该掉落物的物品堆（模拟判断，不实际写入）。
     *
     * <p>替换仅「18 格全满」的粗判断：背包未满但堆叠已满（不同物品无空位可合并）时，
     * 该物品不应被选为目标，避免女仆反复前往却放不下。
     */
    @Override
    protected boolean canPickItem(ItemEntity item) {
        ItemStack stack = item.getStack();
        var inv = this.maid.getInventory();
        // 复用 Inventory 的模拟插入逻辑：先尝试合并到已有堆叠，再尝试放入空槽
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inv.size() && !remaining.isEmpty(); i++) {
            ItemStack slot = inv.getStack(i);
            if (slot.isEmpty()) {
                continue;
            }
            if (ItemStack.canCombine(slot, remaining)) {
                int move = Math.min(slot.getMaxCount() - slot.getCount(), remaining.getCount());
                if (move > 0) {
                    remaining.decrement(move);
                }
            }
        }
        if (remaining.isEmpty()) {
            return true;
        }
        for (int i = 0; i < inv.size() && !remaining.isEmpty(); i++) {
            if (inv.getStack(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按物品 tag 白名单过滤可捡物品。
     *
     * <p>白名单为空时捡所有可捡物品（保持现状）；非空时仅当物品命中任一配置的 tag 才可捡。
     * 该过滤对驯服版与野良版均生效。
     */
    @Override
    public List<ItemEntity> findAroundDropItem() {
        List<ItemEntity> drops = super.findAroundDropItem();
        List<String> whitelist = LMMRMod.getConfig().movement.pickupItemWhitelistTags;
        if (whitelist.isEmpty()) {
            return drops;
        }
        return drops.stream()
                .filter(item -> isWhitelisted(item.getStack(), whitelist))
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean isWhitelisted(ItemStack stack, List<String> whitelist) {
        for (String tagStr : whitelist) {
            if (!Identifier.isValid(tagStr)) {
                continue;
            }
            TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, new Identifier(tagStr));
            if (stack.isIn(tag)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOwnerRange(Entity entity, Entity owner) {
        Vec3d ownerPos = owner.getPos();
        Vec3d entityPos = entity.getPos().subtract(ownerPos);
        Vec3d ownerRot = owner.getRotationVec(1F);
        double dot = entityPos.dotProduct(ownerRot);
        double range = LMMRMod.getConfig().movement.ownerForwardRange;
        // プレイヤー位置を原点としたアイテムの位置と、プレイヤーの向きの内積がプラス
        // かつ内積の大きさが4m以下
        return 0 < dot && dot < range * range;
    }

    /** 到达掉落物后实际拾取（复用原有进背包/拾取粒子逻辑，不再播放挥臂动画）。 */
    @Override
    public void onPickup(ItemEntity target) {
        // 复用 LMCollidable.onCollision_LMMR：进女仆背包 / 交给玩家，含 cannotPickup 守卫与拾取粒子
        if (target instanceof LMCollidable collidable) {
            collidable.onCollision_LMMR(this.maid);
        }
    }
}
