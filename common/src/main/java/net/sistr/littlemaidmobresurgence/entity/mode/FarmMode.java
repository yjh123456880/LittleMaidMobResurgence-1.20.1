package net.sistr.littlemaidmobresurgence.entity.mode;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.Mode;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.util.BlockSearch;
import net.sistr.littlemaidmobresurgence.util.SearchCondition;
import org.jetbrains.annotations.Nullable;

/**
 * 农夫模式（耕种模式）：主手持锄头时激活。
 *
 * <p>行为参考车万女仆（Touhou Little Maid）的农业任务：
 * <ul>
 *   <li>优先收获工作范围内已成熟作物（小麦/胡萝卜/马铃薯/甜菜等下界疣等
 *       {@link CropBlock}/{@link NetherWartBlock}）；</li>
 *   <li>收获产物<b>直接进入女仆背包</b>（走方块掉落表但不生成掉落物实体，
 *       相当于车万女仆的 dropResourcesToMaidInv）；</li>
 *   <li>收获后<b>原位补种</b>：背包有对应种子则消耗 1 粒原地重种（age 0），
 *       无种子则把作物原地重置为幼株；</li>
 *   <li>范围内有空耕地且有种子时，主动种下作物（耕地种作物种子、灵魂沙种下界疣）。</li>
 * </ul>
 */
public class FarmMode extends Mode {
    /** 下界疣最大生长阶段（Yarn 中该常量未公开，与原版一致为 3）。 */
    private static final int MAX_NETHER_WART_AGE = 3;
    /** 可交互距离（格）：自身一格 + 面前相邻一格（2 格），需走到目标跟前才能收获/种植。 */
    private static final double ACTION_REACH_SQ = 1.5 * 1.5;

    protected final LittleMaidEntity mob;
    protected final float distance;

    @Nullable protected BlockPos targetPos;
    @Nullable protected BlockSearch blockSearch;
    protected int recalcPathTimer;
    /** true=正在找成熟作物收获；false=正在找空耕地种植（收获优先）。 */
    protected boolean searchingHarvest = true;

    public FarmMode(
            ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, float distance) {
        super(modeType, name);
        this.mob = mob;
        this.distance = distance;
    }

    @Override
    public boolean shouldExecute() {
        // 主手必须是锄头（HasModeImpl 已按 matcher 切换主手，这里防御性再查一次）
        if (!(mob.getMainHandStack().getItem() instanceof HoeItem)) {
            return false;
        }
        if (!ensureSearch()) {
            return false;
        }
        blockSearch.tick(LMMRMod.getConfig().work.blockSearchBudgetPerTick);
        targetPos = blockSearch.getResult().orElse(null);
        return targetPos != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetPos != null && mob.getMainHandStack().getItem() instanceof HoeItem;
    }

    @Override
    public void startExecuting() {
        mob.getNavigation().stop();
        ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_D);
    }

    @Override
    public void tick() {
        BlockPos pos = targetPos;
        if (pos == null) {
            return;
        }
        World world = mob.getWorld();
        double distanceSq =
                mob.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        // 距离过远 → 走近目标
        if (ACTION_REACH_SQ < distanceSq) {
            if (--recalcPathTimer < 0) {
                recalcPathTimer = LMMRMod.getConfig().movement.pathRecalcInterval * 2;
                // 目标距离 1：走到作物相邻格，确保进入互动范围再工作
                Path path = mob.getNavigation().findPathTo(pos.getX(), pos.getY(), pos.getZ(), 1);
                if (path == null
                        || path.getEnd() == null
                        || !path.getEnd().getBlockPos().isWithinDistance(pos, 3)) {
                    // 无法到达 → 放弃该目标，重新搜索
                    abandonTarget();
                    return;
                }
                mob.getNavigation().startMovingAlong(path, 1.0);
            }
            return;
        }
        mob.getNavigation().stop();
        if (world.isClient) {
            abandonTarget();
            return;
        }
        // 面向作物后再收获/种植（自身一格 + 面前一格）
        mob.getLookControl().lookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        // 到达后重新验证目标（可能已被他人收走/种下）
        if (isHarvestableCrop(pos)) {
            harvest(pos);
        } else if (isPlantableFarmland(pos)) {
            plant(pos);
        }
        abandonTarget();
    }

    @Override
    public void resetTask() {
        targetPos = null;
        blockSearch = null;
        recalcPathTimer = 0;
        mob.getNavigation().stop();
    }

    /** 确保存在可用搜索：当前搜索未结束则继续；结束则推进阶段（收获→种植）；均无结果则本轮结束。 */
    private boolean ensureSearch() {
        if (blockSearch != null && !blockSearch.isFinished()) {
            return true;
        }
        if (blockSearch != null) {
            // 当前阶段搜索完成且无结果 → 收获阶段结束后切到种植阶段
            if (searchingHarvest) {
                searchingHarvest = false;
            } else {
                // 种植阶段也结束 → 本轮无工作，重置回收获阶段等下次重新扫描
                searchingHarvest = true;
                blockSearch = null;
                return false;
            }
        } else {
            // 全新一轮搜索：从收获阶段开始（每次行动后都会重新扫描）
            searchingHarvest = true;
        }
        if (searchingHarvest) {
            blockSearch = createSearch(this::isHarvestableCrop);
        } else {
            // 没有种子就不用找空地种了
            if (!hasAnySeed()) {
                searchingHarvest = true;
                blockSearch = null;
                return false;
            }
            blockSearch = createSearch(this::isPlantableFarmland);
        }
        return blockSearch != null;
    }

    @Nullable
    private BlockSearch createSearch(java.util.function.Predicate<BlockPos> target) {
        SearchCondition condition = SearchCondition.forMob(mob).maxDistance(distance).build();
        return new BlockSearch(
                mob.getBlockPos(),
                target,
                condition,
                MathHelper.floor(distance * distance * 2));
    }

    /** 该位置是否为可收获的成熟作物。 */
    private boolean isHarvestableCrop(BlockPos pos) {
        BlockState state = mob.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMature(state);
        }
        return block instanceof NetherWartBlock
                && state.get(NetherWartBlock.AGE) >= MAX_NETHER_WART_AGE;
    }

    /** 该位置（空气方块）下方是否为可种植的耕地/灵魂沙，且背包有对应种子。 */
    private boolean isPlantableFarmland(BlockPos pos) {
        World world = mob.getWorld();
        if (!world.isAir(pos)) {
            return false;
        }
        BlockState below = world.getBlockState(pos.down());
        if (below.isOf(Blocks.FARMLAND)) {
            return hasCropSeed();
        }
        return below.isOf(Blocks.SOUL_SAND) && hasWartSeed();
    }

    /** 收获成熟作物：掉落直接进背包（不生成掉落物实体），随后原位补种。 */
    private void harvest(BlockPos pos) {
        World world = mob.getWorld();
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (!isHarvestableCrop(pos)) {
            return;
        }
        Block block = state.getBlock();

        // 1) 掉落表产物直接进女仆背包（车万女仆 dropResourcesToMaidInv 同款：不产生掉落物实体）
        List<ItemStack> drops =
                Block.getDroppedStacks(
                        state, serverWorld, pos, null, mob, mob.getMainHandStack());
        for (ItemStack drop : drops) {
            ItemStack remainder = HopperBlockEntity.transfer(null, mob.getInventory(), drop, null);
            if (!remainder.isEmpty()) {
                // 仅背包已满时的兜底：直接掉落剩余物品，避免物品凭空消失
                Block.dropStack(world, pos, remainder);
            }
        }
        // 破坏粒子（不产生掉落）
        serverWorld.syncWorldEvent(
                null, WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(state));

        // 2) 原位补种：有对应种子 → 消耗 1 粒原地重种（age 0）；无种子 → 原地重置为幼株
        boolean replanted = false;
        ItemStack seed = findSeedFor(block);
        if (!seed.isEmpty()) {
            replanted = placeSeed(seed, pos, true);
        }
        if (!replanted) {
            BlockState young = getYoungState(state);
            if (young != null) {
                world.setBlockState(pos, young, Block.NOTIFY_ALL);
            } else {
                world.removeBlock(pos, false);
            }
        }
        mob.swingHand(Hand.MAIN_HAND);
        ((SoundPlayable) mob).play(LMSounds.INSTALLATION);
    }

    /** 在空耕地上种作物（消耗 1 个种子）。 */
    private void plant(BlockPos pos) {
        World world = mob.getWorld();
        if (world.isClient) {
            return;
        }
        BlockState below = world.getBlockState(pos.down());
        ItemStack seed =
                below.isOf(Blocks.SOUL_SAND) ? findSeed(SeedKind.WART) : findSeed(SeedKind.CROP);
        if (seed.isEmpty()) {
            return;
        }
        placeSeed(seed, pos, false);
    }

    /**
     * 在指定位置种下种子。
     *
     * @param clearOld 是否先移除该位置的旧方块（收获后重种时需要）
     * @return 是否成功（成功会消耗 1 个种子）
     */
    private boolean placeSeed(ItemStack seed, BlockPos pos, boolean clearOld) {
        World world = mob.getWorld();
        Item item = seed.getItem();
        if (!(item instanceof BlockItem blockItem)) {
            return false;
        }
        if (clearOld) {
            world.removeBlock(pos, false);
        }
        ActionResult result =
                blockItem.place(
                        new AutomaticItemPlacementContext(
                                world, pos, Direction.UP, seed, Direction.UP));
        if (!result.isAccepted()) {
            return false;
        }
        seed.decrement(1);
        mob.swingHand(Hand.MAIN_HAND);
        ((SoundPlayable) mob).play(LMSounds.INSTALLATION);
        return true;
    }

    /** 作物的幼株状态（age 0）；非作物返回 null。 */
    @Nullable
    private BlockState getYoungState(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.withAge(0);
        }
        if (block instanceof NetherWartBlock) {
            return state.with(NetherWartBlock.AGE, 0);
        }
        return null;
    }

    /** 背包中是否存在任意种子（作物种子或下界疣）。 */
    private boolean hasAnySeed() {
        return !findSeed(null).isEmpty();
    }

    /** 背包中是否存在可种耕地的作物种子。 */
    private boolean hasCropSeed() {
        return !findSeed(SeedKind.CROP).isEmpty();
    }

    /** 背包中是否存在下界疣种子。 */
    private boolean hasWartSeed() {
        return !findSeed(SeedKind.WART).isEmpty();
    }

    /** 找与指定作物方块对应的种子（如收获小麦后找小麦种子）。 */
    private ItemStack findSeedFor(Block cropBlock) {
        var inventory = mob.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            if (blockItem.getBlock() == cropBlock) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 找背包中第一个可作种子的物品。
     *
     * @param kind 种子类型；null 表示任意
     */
    private ItemStack findSeed(@Nullable SeedKind kind) {
        var inventory = mob.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            Block block = blockItem.getBlock();
            if (block instanceof CropBlock) {
                if (kind == null || kind == SeedKind.CROP) {
                    return stack;
                }
            } else if (block instanceof NetherWartBlock) {
                if (kind == null || kind == SeedKind.WART) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private enum SeedKind {
        CROP,
        WART
    }

    /** 放弃当前目标并清空搜索，下次 shouldExecute 重新扫描。 */
    private void abandonTarget() {
        targetPos = null;
        blockSearch = null;
    }
}
