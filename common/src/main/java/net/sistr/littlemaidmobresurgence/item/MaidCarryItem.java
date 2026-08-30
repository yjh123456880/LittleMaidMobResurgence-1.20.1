package net.sistr.littlemaidmobresurgence.item;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import net.sistr.littlemaidmobresurgence.setup.Registration;
import org.jetbrains.annotations.Nullable;

/**
 * [zh] 女仆捕捉蛋：右键自己的契约女仆可整只收纳（保存完整 NBT、移除且不掉落），
 *     右键方块按原状态放回；空/满蛋分别用 maidporter_0/maidporter_1 贴图（custom_model_data）。
 *     掉落物与纪念品一样高亮、不可破坏、永不消失（见 MixinItemEntityAntiDamage）。
 * [en] Maid capture egg: right-click your contracted maid to store her entirely (full NBT, removed without drops),
 *     right-click a block to release her as-is; empty/filled textures are maidporter_0/maidporter_1 (custom_model_data).
 *     Dropped eggs glow, are indestructible and never despawn (see MixinItemEntityAntiDamage).
 * [ja] メイド捕捉卵：自分の契約メイドを右クリックで丸ごと収納（完全NBT保存・ドロップなしで除去）、
 *     ブロック右クリックで元の状態のまま放出。空/満で maidporter_0/maidporter_1 テクスチャ切替（custom_model_data）。
 *     ドロップ品は記念品同様に発光・破壊不能・消滅なしです（MixinItemEntityAntiDamage 参照）。
 */
public class MaidCarryItem extends Item {
    public static final String MAID_DATA_KEY = "MaidData";
    public static final String MAID_NAME_KEY = "MaidName";
    public static final String OWNER_NAME_KEY = "OwnerName";
    public static final int FILLED_MODEL_DATA = 1;

    public MaidCarryItem() {
        super(new Item.Settings().maxCount(1).arch$tab(Registration.ITEM_GROUP));
    }

    // [zh] ---- 数据存取 ----
    // [en] ---- Data access ----
    // [ja] ---- データ読み書き ----

    public static boolean hasData(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(MAID_DATA_KEY);
    }

    /**
     * [zh] 把整只女仆写入蛋（覆写蛋内旧数据）。
     * [en] Writes the whole maid into the egg (overwrites old data).
     * [ja] メイド全体を卵に書き込みます（古いデータは上書き）。
     */
    public static void fill(ItemStack stack, LittleMaidEntity maid) {
        NbtCompound maidNbt = new NbtCompound();
        maid.writeNbt(maidNbt);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.put(MAID_DATA_KEY, maidNbt);
        nbt.putString(MAID_NAME_KEY, maid.getName().getString());
        nbt.putInt("CustomModelData", FILLED_MODEL_DATA);
        TameableUtil.getTameOwner(maid)
                .ifPresent(owner -> nbt.putString(OWNER_NAME_KEY, owner.getName().getString()));
    }

    public static ItemStack createCarry(LittleMaidEntity maid) {
        ItemStack stack = new ItemStack(Registration.MAID_CARRY_ITEM.get());
        fill(stack, maid);
        return stack;
    }

    private static void clear(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            nbt.remove(MAID_DATA_KEY);
            nbt.remove(MAID_NAME_KEY);
            nbt.remove(OWNER_NAME_KEY);
            nbt.remove("CustomModelData");
        }
    }

    /**
     * [zh] 显示名：满蛋显示「{名}的捕捉蛋」。
     * [en] Display name: filled egg shows "{name}'s Capture Egg".
     * [ja] 表示名：満タンの卵は「{名前}の捕捉卵」。
     */
    @Override
    public Text getName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        String maidName = nbt != null ? nbt.getString(MAID_NAME_KEY) : "";
        if (maidName.isEmpty()) {
            return super.getName(stack);
        }
        return Text.translatable("item.littlemaidmobresurgence.maid_carry.named", maidName);
    }

    /**
     * [zh] 满蛋时发光（对齐 1.12.2 的 hasEffect）。
     * [en] Glows when filled (mirrors 1.12.2's hasEffect).
     * [ja] 満タンのとき発光（1.12.2 の hasEffect に相当）。
     */
    @Override
    public boolean hasGlint(ItemStack stack) {
        return hasData(stack);
    }

    // [zh] ---- 交互 ----
    // [en] ---- Interactions ----
    // [ja] ---- インタラクション ----

    /**
     * [zh] 右键空挥：不弹提示（收纳/放出均不刷屏）。
     * [en] Use on air: no feedback message (capture/release are silent by design).
     * [ja] 空振り：通知なし（収納・放出はチャットを出さない設計）。
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        // 收纳/放出不弹聊天提示，避免刷屏
        return TypedActionResult.success(stack);
    }

    /**
     * [zh] 右键方块：满蛋在点击面外侧放出女仆；空蛋直接忽略。
     * [en] Right-click a block: releases the maid on the clicked face's outer side; empty egg is ignored.
     * [ja] ブロック右クリック：クリック面の外側にメイドを放出。空卵は無視。
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        if (player == null) {
            return ActionResult.PASS;
        }
        if (!hasData(stack)) {
            return ActionResult.SUCCESS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }
        // [zh] 在点击面外侧放出（对齐 1.12.2：pos.offset(facing)）
        // [en] Release on the clicked face's outer side (mirrors 1.12.2: pos.offset(facing)).
        // [ja] クリック面の外側に放出（1.12.2 準拠：pos.offset(facing)）。
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        LittleMaidEntity maid = Registration.LITTLE_MAID_MOB.get().create(world);
        if (maid == null) {
            return ActionResult.PASS;
        }
        maid.readNbt(stack.getNbt().getCompound(MAID_DATA_KEY));
        maid.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        if (maid.getHealth() < 1.0F) {
            maid.setHealth(1.0F);
        }
        serverWorld.spawnEntity(maid);
        clear(stack);
        player.getInventory().markDirty();
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                player.getSoundCategory(),
                1.0F,
                1.0F);
        return ActionResult.SUCCESS;
    }

    /**
     * [zh] 右键女仆（由 LMInteractionHandler 拦截）：空蛋收纳持有者自己的契约女仆；满蛋忽略。
     * [en] Right-click a maid (intercepted by LMInteractionHandler): empty egg captures the holder's own contracted maid; filled egg is ignored.
     * [ja] メイド右クリック（LMInteractionHandler 経由）：空卵は自分の契約メイドを収納、満タンは無視。
     */
    public ActionResult interactMaid(LittleMaidEntity maid, PlayerEntity player, Hand hand) {
        if (maid.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (hasData(stack)) {
            return ActionResult.SUCCESS;
        }
        // [zh] 只有持有者自己的契约女仆可收纳
        // [en] Only the holder's own contracted maid can be captured.
        // [ja] 自分の契約メイドのみ収納可能。
        if (!TameableUtil.isTameOwner(maid, player)) {
            return ActionResult.SUCCESS;
        }
        fill(stack, maid);
        player.setStackInHand(hand, stack);
        player.getInventory().markDirty();
        // [zh] 避免收纳时触发纪念品掉落
        // [en] Prevent souvenir drop during capture.
        // [ja] 収納時の記念品ドロップを防止。
        maid.setCaptureSuppressSouvenir(true);
        maid.discard();
        player.getItemCooldownManager().set(this, 10);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(
            ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (hasData(stack) && nbt != null) {
            if (nbt.contains(OWNER_NAME_KEY)) {
                tooltip.add(
                        Text.translatable(
                                        "message.littlemaidmobresurgence.maid_carry.owner",
                                        nbt.getString(OWNER_NAME_KEY))
                                .formatted(Formatting.GRAY));
            }
            tooltip.add(
                    Text.translatable(
                                    "message.littlemaidmobresurgence.maid_carry.maid",
                                    nbt.getString(MAID_NAME_KEY))
                            .formatted(Formatting.GRAY));
        }
        tooltip.add(
                Text.translatable("message.littlemaidmobresurgence.maid_carry.info")
                        .formatted(Formatting.LIGHT_PURPLE));
    }
}
