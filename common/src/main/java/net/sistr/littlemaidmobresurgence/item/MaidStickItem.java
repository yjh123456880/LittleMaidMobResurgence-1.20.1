package net.sistr.littlemaidmobresurgence.item;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import net.sistr.littlemaidmobresurgence.setup.Registration;
import org.jetbrains.annotations.Nullable;

/**
 * [zh] 女仆杖（工作范围/女仆绑定器）：右键方块顶部绑定工作范围（半径 = 统一 workRange），
 *     空挥切换「绑定范围/绑定女仆」两种模式，shift+右键清除绑定信息。
 *     一个范围可绑定多个女仆，范围写入各女仆 NBT，仅自由行动时生效。
 * [en] Maid stick (work-range / maid binder): right-click a block top to bind a work range (radius = unified workRange),
 *     use on air to toggle "bind range / bind maid" modes, shift+right-click clears bindings.
 *     One range can be bound to many maids; it is stored in each maid's NBT and only applies in free-move mode.
 * [ja] メイドステッキ（作業範囲・メイド登録バインダー）：ブロック上面を右クリックで作業範囲を登録（半径＝統一 workRange）、
 *     空振りで「範囲登録／メイド登録」を切替、Shift+右クリックで登録を解除。
 *     1つの範囲は複数メイドに登録でき、各メイドのNBTに保存され、自由行動時のみ有効です。
 */
public class MaidStickItem extends Item {
    private static final String KEY_WORK_CENTER = "WorkCenter";
    private static final String KEY_WORK_DIMENSION = "WorkDimension";
    private static final String KEY_MODE = "Mode";
    private static final String MODE_RANGE = "range";
    private static final String MODE_MAID = "maid";

    public MaidStickItem() {
        super(new Item.Settings().maxCount(1).arch$tab(Registration.ITEM_GROUP));
    }

    // [zh] ---- 范围信息存取 ----
    // [en] ---- Work-range data access ----
    // [ja] ---- 範囲情報の読み書き ----

    @Nullable
    public static BlockPos getWorkCenter(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(KEY_WORK_CENTER)) {
            return NbtHelper.toBlockPos(nbt.getCompound(KEY_WORK_CENTER));
        }
        return null;
    }

    @Nullable
    public static Identifier getWorkDimension(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(KEY_WORK_DIMENSION)) {
            return Identifier.tryParse(nbt.getString(KEY_WORK_DIMENSION));
        }
        return null;
    }

    public static boolean hasWorkCenter(ItemStack stack) {
        return getWorkCenter(stack) != null && getWorkDimension(stack) != null;
    }

    private static void setWorkCenter(ItemStack stack, BlockPos pos, Identifier dimension) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.put(KEY_WORK_CENTER, NbtHelper.fromBlockPos(pos));
        nbt.putString(KEY_WORK_DIMENSION, dimension.toString());
    }

    private static void clearWorkCenter(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            nbt.remove(KEY_WORK_CENTER);
            nbt.remove(KEY_WORK_DIMENSION);
        }
    }

    private static boolean isMaidMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && MODE_MAID.equals(nbt.getString(KEY_MODE));
    }

    private static void toggleMode(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(KEY_MODE, isMaidMode(stack) ? MODE_RANGE : MODE_MAID);
    }

    // ---- 交互 ----

    /**
     * [zh] 空挥：shift 清除范围；否则切换绑定模式（仅在服务端提示，避免重复消息）。
     * [en] Use on air: shift clears the range; otherwise toggles the bind mode (server-side messages only).
     * [ja] 空振り：Shift で範囲クリア、それ以外は登録モード切替（サーバー側のみで通知）。
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        // [zh] 只在服务端执行并提示，避免客户端/服务端各发一次导致重复
        // [en] Only run and message on the server to avoid duplicated feedback.
        // [ja] サーバー側だけで実行・通知し、重複メッセージを防ぎます。
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (user.isSneaking()) {
            clearWorkCenter(stack);
            user.getInventory().markDirty();
            user.sendMessage(Text.translatable("message.littlemaidmobresurgence.maid_stick.cleared"), false);
        } else {
            boolean wasMaid = isMaidMode(stack);
            toggleMode(stack);
            user.getInventory().markDirty();
            user.sendMessage(
                    Text.translatable(
                            wasMaid
                                    ? "message.littlemaidmobresurgence.maid_stick.mode.range"
                                    : "message.littlemaidmobresurgence.maid_stick.mode.maid"),
                    false);
        }
        user.getItemCooldownManager().set(this, 10);
        return TypedActionResult.success(stack);
    }

    /**
     * [zh] 右键方块顶面：range 模式绑定工作范围；maid 模式忽略；shift 清除。
     * [en] Right-click a block top: bind the work range in range mode; ignored in maid mode; shift clears.
     * [ja] ブロック上面を右クリック：範囲モードなら登録、メイドモードなら無視、Shift ならクリア。
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        if (player == null) {
            return ActionResult.PASS;
        }
        if (player.isSneaking()) {
            if (!world.isClient) {
                clearWorkCenter(stack);
                player.getInventory().markDirty();
                player.sendMessage(
                        Text.translatable("message.littlemaidmobresurgence.maid_stick.cleared"), false);
            }
            return ActionResult.SUCCESS;
        }
        if (isMaidMode(stack)) {
            if (!world.isClient) {
                player.sendMessage(
                        Text.translatable("message.littlemaidmobresurgence.maid_stick.mode.block_ignored"),
                        false);
            }
            return ActionResult.CONSUME;
        }
        if (context.getSide() != Direction.UP) {
            if (!world.isClient) {
                player.sendMessage(
                        Text.translatable("message.littlemaidmobresurgence.maid_stick.not_top"), false);
            }
            // [zh] CONSUME 避免落到 use()（空挥）误触发模式切换
            // [en] CONSUME prevents falling through to use() (air-use) and accidentally toggling the mode.
            // [ja] CONSUME で use()（空振り）に流れて誤ってモード切替されるのを防ぎます。
            return ActionResult.CONSUME;
        }
        if (!world.isClient) {
            setWorkCenter(stack, context.getBlockPos(), world.getRegistryKey().getValue());
            player.getInventory().markDirty();
            player.sendMessage(
                    Text.translatable(
                            "message.littlemaidmobresurgence.maid_stick.bound",
                            context.getBlockPos().getX(),
                            context.getBlockPos().getY(),
                            context.getBlockPos().getZ(),
                            (int) LMMRMod.getConfig().work.workRange),
                    false);
            world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    player.getSoundCategory(),
                    1.0F,
                    1.0F);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * [zh] 右键女仆（由 LMInteractionHandler 拦截）：maid 模式绑定范围；shift 清除女仆绑定。
     * [en] Right-click a maid (intercepted by LMInteractionHandler): binds the range in maid mode; shift clears the maid binding.
     * [ja] メイドを右クリック（LMInteractionHandler 経由）：メイドモードで範囲登録、Shift でメイドの登録解除。
     */
    public ActionResult interactMaid(LittleMaidEntity maid, PlayerEntity player, Hand hand) {
        if (maid.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (player.isSneaking()) {
            maid.clearBoundWorkCenter();
            player.sendMessage(
                    Text.translatable("message.littlemaidmobresurgence.maid_stick.maid_cleared"), false);
            return ActionResult.SUCCESS;
        }
        // 只处理持有者自己的契约女仆
        if (!TameableUtil.isTameOwner(maid, player)) {
            return ActionResult.SUCCESS;
        }
        if (!isMaidMode(stack)) {
            player.sendMessage(
                    Text.translatable("message.littlemaidmobresurgence.maid_stick.mode.maid_needed"), false);
            return ActionResult.SUCCESS;
        }
        if (!hasWorkCenter(stack)) {
            player.sendMessage(Text.translatable("message.littlemaidmobresurgence.maid_stick.need_range"), false);
            return ActionResult.SUCCESS;
        }
        Identifier dim = getWorkDimension(stack);
        if (dim == null || !dim.equals(maid.getWorld().getRegistryKey().getValue())) {
            player.sendMessage(Text.translatable("message.littlemaidmobresurgence.maid_stick.dimension"), false);
            return ActionResult.SUCCESS;
        }
        maid.setBoundWorkCenter(getWorkCenter(stack), dim);
        player.sendMessage(
                Text.translatable(
                        "message.littlemaidmobresurgence.maid_stick.maid_bound",
                        maid.getName().getString()),
                false);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(
            ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        boolean maidMode = isMaidMode(stack);
        tooltip.add(
                Text.translatable(
                                maidMode
                                        ? "message.littlemaidmobresurgence.maid_stick.mode.cur_maid"
                                        : "message.littlemaidmobresurgence.maid_stick.mode.cur_range")
                        .formatted(Formatting.GOLD));
        if (hasWorkCenter(stack)) {
            BlockPos center = getWorkCenter(stack);
            tooltip.add(
                    Text.translatable(
                            "message.littlemaidmobresurgence.maid_stick.tooltip_bound",
                            center.getX(),
                            center.getY(),
                            center.getZ())
                            .formatted(Formatting.GOLD));
        }
        tooltip.add(Text.translatable("message.littlemaidmobresurgence.maid_stick.usage.switch").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("message.littlemaidmobresurgence.maid_stick.usage.bind_block").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("message.littlemaidmobresurgence.maid_stick.usage.bind_maid").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("message.littlemaidmobresurgence.maid_stick.usage.clear").formatted(Formatting.GRAY));
    }
}
