package net.sistr.littlemaidmobresurgence.item;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.MaidSpeech;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import net.sistr.littlemaidmobresurgence.setup.Registration;
import org.jetbrains.annotations.Nullable;

/**
 * [zh] 女仆纪念品：女仆死亡时掉落，完整保存女仆数据（NBT），手持右键方块可复活女仆。
 *     对齐 1.12.2 小女仆模组的纪念品机制：复活后仅 1 滴血并进入休息状态，生命恢复到自身 50% 后解除。
 * [en] Maid souvenir: dropped when a maid dies, stores the full maid data (NBT); right-click a block while holding it to revive her.
 *     Mirrors the 1.12.2 little-maid memorial: revived maids have 1 HP and rest until HP recovers to 50%.
 * [ja] メイド記念品：死亡時にドロップし、メイドの完全データ（NBT）を保存。持ってブロックを右クリックで復活。
 *     1.12.2 の記念品仕様に準拠：復活時は HP1 の休息状態で、HP が最大値の50%まで回復すると解除されます。
 */
public class MaidSouvenirItem extends Item {
    /**
     * [zh] 女仆完整 NBT（写入物品堆 NBT）。
     * [en] The maid's full NBT (stored on the ItemStack).
     * [ja] メイドの完全 NBT（ItemStack の NBT に保存）。
     */
    public static final String MAID_DATA_KEY = "MaidData";
    /**
     * [zh] 女仆名字（显示名用）。
     * [en] Maid name (for the display name).
     * [ja] メイド名（表示名用）。
     */
    public static final String MAID_NAME_KEY = "MaidName";
    /**
     * [zh] 主人名字（tooltip 用）。
     * [en] Owner name (for tooltip).
     * [ja] ご主人の名前（ツールチップ用）。
     */
    public static final String OWNER_NAME_KEY = "OwnerName";

    public MaidSouvenirItem() {
        super(new Item.Settings().maxCount(1).arch$tab(Registration.ITEM_GROUP));
    }

    /**
     * [zh] 从女仆创建纪念品：完整保存女仆 NBT + 名字 + 主人名，并清理死亡瞬间的运动/受击残留。
     * [en] Creates a souvenir from a maid: saves full NBT + name + owner name, and clears leftover motion/hurt state.
     * [ja] メイドから記念品を作成：完全NBT＋名前＋主人名を保存し、死亡時のモーション・被弾状態を除去。
     */
    public static ItemStack createSouvenir(LittleMaidEntity maid) {
        ItemStack stack = new ItemStack(Registration.MAID_SOUVENIR_ITEM.get());
        NbtCompound maidNbt = new NbtCompound();
        maid.writeNbt(maidNbt);
        // [zh] 经验值已随死亡正常掉落为经验球，不再封存在纪念品中（避免复活白嫖经验）
        // [en] XP already drops as orbs on death, so it is not stored in the souvenir (prevents free XP on revival).
        // [ja] 経験値は死亡時に経験値オーブとしてドロップ済みのため記念品には保存しません（復活での経験値稼ぎ防止）。
        maidNbt.putInt("XpTotal", 0);
        // [zh] 清除死亡瞬间残留的运动/受击状态，避免复活带出「被击退/红闪/倒地」效果
        // [en] Clear leftover motion/hurt state so revival does not carry knockback/red-flash/death effects.
        // [ja] 死亡時のモーション・被弾状態を除去し、復活時にノックバック等の残滓が出ないようにします。
        maidNbt.remove("Motion");
        maidNbt.putShort("HurtTime", (short) 0);
        maidNbt.putInt("HurtByTimestamp", 0);
        maidNbt.putShort("DeathTime", (short) 0);
        maidNbt.putFloat("FallDistance", 0.0F);
        // [zh] 清掉加速残值（防止复活后仍带加速倍率）
        // [en] Clear remaining acceleration ticks (prevents leftover speed boost after revival).
        // [ja] 加速の残りもクリア（復活後に加速倍率が残らないように）。
        maidNbt.putInt("accelerationTicks", 0);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.put(MAID_DATA_KEY, maidNbt);
        nbt.putString(MAID_NAME_KEY, maid.getName().getString());
        TameableUtil.getTameOwner(maid)
                .ifPresent(owner -> nbt.putString(OWNER_NAME_KEY, owner.getName().getString()));
        return stack;
    }

    /**
     * [zh] 显示名：{女仆名}的纪念品（1.12.2 同款命名）。
     * [en] Display name: "{maid name}'s Souvenir" (same naming as 1.12.2).
     * [ja] 表示名：「{メイド名}の記念品」（1.12.2 と同じ命名）。
     */
    @Override
    public Text getName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        String maidName = nbt != null ? nbt.getString(MAID_NAME_KEY) : "";
        if (maidName.isEmpty()) {
            return super.getName(stack);
        }
        return Text.translatable("item.littlemaidmobresurgence.maid_souvenir.named", maidName);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            @Nullable World world,
            List<Text> tooltip,
            TooltipContext context) {
        tooltip.add(
                Text.translatable("item.littlemaidmobresurgence.maid_souvenir.info")
                        .formatted(Formatting.LIGHT_PURPLE));
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            if (nbt.contains(OWNER_NAME_KEY)) {
                tooltip.add(
                        Text.translatable(
                                        "item.littlemaidmobresurgence.maid_souvenir.owner",
                                        nbt.getString(OWNER_NAME_KEY))
                                .formatted(Formatting.GRAY));
            }
            if (nbt.contains(MAID_NAME_KEY)) {
                tooltip.add(
                        Text.translatable(
                                        "item.littlemaidmobresurgence.maid_souvenir.maid",
                                        nbt.getString(MAID_NAME_KEY))
                                .formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
        // [zh] 客户端仅返回成功播放挥手动画，实际逻辑在服务端执行
        // [en] Client only returns success for the swing animation; the real logic runs on the server.
        // [ja] クライアントは成功を返して腕振りアニメのみ再生。実処理はサーバー側。
            return ActionResult.SUCCESS;
        }
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        if (player == null) {
            return ActionResult.PASS;
        }
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(MAID_DATA_KEY)) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        // [zh] 在点击面的外侧复活女仆（对齐 1.12.2：pos.offset(facing)）
        // [en] Revive the maid on the clicked face's outer side (mirrors 1.12.2: pos.offset(facing)).
        // [ja] クリック面の外側に復活（1.12.2 準拠：pos.offset(facing)）。
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        LittleMaidEntity maid = Registration.LITTLE_MAID_MOB.get().create(world);
        if (maid == null) {
            return ActionResult.PASS;
        }
        maid.readNbt(nbt.getCompound(MAID_DATA_KEY));
        maid.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        // [zh] 复活女仆：只剩 1 滴血并清除着火/状态效果
        // [en] Revived maid starts with 1 HP; fire and status effects are cleared.
        // [ja] 復活時は HP1 から開始。炎上・状態効果を除去。
        maid.setHealth(1.0F);
        maid.extinguish();
        maid.clearStatusEffects();
        // [zh] 复活女仆：进入休息状态（血量恢复到自身 50% 前保持坐姿恢复），不再采用罢工
        // [en] Revived maid enters the rest state (sits and recovers until 50% HP) instead of striking.
        // [ja] 復活メイドは休息状態（HP50%まで座って回復）。ストライキは使いません。
        maid.setSouvenirReviveRest(true);
        // [zh] 复活时情绪下限为 10，并清空反叛/愤怒残留，避免复活后立刻反叛
        // [en] Mood is raised to at least 10 and anger is cleared so the revived maid does not instantly rebel.
        // [ja] 復活時の機嫌下限を10にし、反乱・怒りの残滓をクリアして即反乱を防ぎます。
        maid.maidMood.ensureMoodAtLeast(10);
        maid.syncMood();
        serverWorld.spawnEntity(maid);
        MaidSpeech.onResurrect(maid);
          // [zh] 消耗纪念品（对齐 1.12.2：创造模式也消耗）
          // [en] Consume the souvenir (mirrors 1.12.2: consumed in creative mode too).
          // [ja] 記念品を消費（1.12.2 準拠：クリエイティブでも消費）。
          stack.decrement(1);
          // [zh] 强制同步背包并清空空槽，让创造/生存都能即时看到消耗，避免重复使用同一纪念品
          // [en] Force-sync the inventory and clear the empty slot so consumption is visible immediately and the same souvenir cannot be reused.
          // [ja] インベントリを即時同期して空スロットを除去。クリエイティブでも消費が見え、同一記念品の再利用を防ぎます。
          player.getInventory().markDirty();
          if (stack.isEmpty()) {
              player.setStackInHand(context.getHand(), ItemStack.EMPTY);
          }
          // [zh] 1 秒使用冷却
          // [en] 1-second use cooldown
          // [ja] 1秒の使用クールダウン
        player.getItemCooldownManager().set(this, 20);
        return ActionResult.SUCCESS;
    }
}
