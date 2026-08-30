package net.sistr.littlemaidmobresurgence.forge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.sistr.littlemaidmobresurgence.compat.CuriosCompat;
import net.sistr.littlemaidmobresurgence.entity.CuriosScreenHandler;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

/**
 * Curios 门面的 Forge 实现。
 *
 * <p>持有 Curios API，通过 {@link CuriosCompat#setAdapter} 注入到 common 门面。职责分两部分：
 *
 * <ul>
 *   <li>{@link #syncCuriosStructure}（服务端，打开饰品 GUI 时）：将女仆的 Curios 槽位动态同步为
 *       参照玩家的槽位结构（类型 + 数量完全一致），返回完整结构供分页计算与开屏数据包使用。</li>
 *   <li>{@link #addCuriosSlots}：按页区间（第 0 页 35 槽 / 后续页 36 槽）创建 {@link MaidCurioSlot}；
 *       服务端包装女仆实时 handler，客户端用本地镜像 handler。</li>
 * </ul>
 *
 * <p>同步规则：
 *
 * <ul>
 *   <li>有主人：双向收敛同步——主人有的类型补齐/对齐数量，主人没有的类型缩为 0（不可见不可用）。
 *   <li>无主人：以玩家默认槽位（{@link CuriosApi#getPlayerSlots(World)}）为基准单向同步。
 *   <li>触发时机：仅在玩家打开自己拥有的女仆的饰品 GUI 时执行；采用活体结构比较去重
 *       （当前结构已与目标一致则跳过），不依赖任何静态标记（跨世界重载后自动重新同步）。
 *   <li>女仆固定背包扩容槽为独立 vanilla 槽位，不在 Curios handler 内，任何同步都不触碰它。
 * </ul>
 */
public final class CuriosCompatImpl implements CuriosCompat.CuriosAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuriosCompatImpl.class);
    // Curios 槽位布局：以 CuriosScreenHandler 中的常量为准，保持 ScreenHandler 构建与 Screen 渲染
    // 以及玩家背包列的对齐一致（9 列、x 左起 8、扩容槽下方 y=36、18px 间距、左→右/上→下）
    private static final int SLOT_COLS = CuriosScreenHandler.CURIOS_COLS;
    private static final int SLOT_SPACING = 18;
    private static final int SLOT_ORIGIN_X = CuriosScreenHandler.CURIOS_ORIGIN_X;
    private static final int SLOT_ORIGIN_Y = CuriosScreenHandler.CURIOS_ORIGIN_Y;

    /** 槽位数量合法性上限（防异常数据同步）。 */
    private static final int MAX_SLOTS_PER_TYPE = 64;

    /**
     * 星月遗物 (Celestial Artifacts) 专属 Curios 槽位 ID 黑名单。
     *
     * <p>这些槽位由 celestial_artifacts 通过 data/celestial_artifacts/curios/slots/ 与
     * data/celestial_artifacts/curios/entities/player_celestial.json 注册，仅对玩家生效， 女仆/NPC 不应拥有。
     *
     * <p>注意："scroll" 槽位由神秘遗物 (Enigmatic Legacy) 最先注册，星月遗物通过 data modifier 覆盖了其图标与大小并添加了自身物品到
     * tag，但该槽仍承载神秘遗物的 8 种卷轴物品 （xp/escape/heaven/fabulous/cursed/avarice/darkest/cosmic_scroll），故不屏蔽
     * scroll， 星月遗物的 4 种卷轴物品（traveler/sea_god/skywalker/twisted_scroll）由 {@link
     * MaidCurioSlot#canInsert} 按物品命名空间拦截。
     *
     * <p>列表来源：解包 celestial_artifacts-1.6.2.jar，见 data/celestial_artifacts/curios/slots/ 与
     * data/celestial_artifacts/curios/entities/player_celestial.json（去掉与 EL 共享的 scroll）。
     */
    private static final Set<String> CELESTIAL_ARTIFACTS_SLOT_BLACKLIST =
            Set.of("catastrophe", "etching", "heart", "pendant");

    private CuriosCompatImpl() {}

    /** 在 Curios 已安装时由 forge 入口调用，注入本实现。 */
    public static void init() {
        CuriosCompat.setAdapter(new CuriosCompatImpl());
    }

    @Override
    public Map<String, Integer> syncCuriosStructure(LittleMaidEntity maid, PlayerEntity openingPlayer) {
        // GUI 打开事件：同步女仆槽位为主人当前槽位结构（含校验、活体比较、日志），返回完整结构
        if (maid == null || maid.getWorld() == null || maid.getWorld().isClient) {
            return Map.of();
        }
        World world = maid.getWorld();
        // 1. 解析参照玩家：开 GUI 玩家（必为主人）> 女仆 ownerUuid 解析 > null
        PlayerEntity refPlayer = resolveReferencePlayer(maid, openingPlayer);
        // 2. 构建目标槽位结构（类型 -> 数量），含数据校验
        Map<String, Integer> target = buildTargetStructure(refPlayer, world);
        // Curios 为所有 LivingEntity 附加库存能力（CuriosEventHandler#attachEntitiesCapabilities）
        ICuriosItemHandler maidCurios = CuriosApi.getCuriosInventory(maid).orElse(null);
        if (maidCurios == null) {
            return Map.of();
        }
        if (target.isEmpty()) {
            // 参照玩家存在但结构为空 -> 视为异常数据，跳过同步防止清空女仆槽位，
            // 返回女仆当前实际结构（保持现状可正常分页显示）
            if (refPlayer != null) {
                LOGGER.warn(
                        "[LMMR] 参照玩家 {} 的 Curios 槽位结构为空，跳过同步", refPlayer.getGameProfile().getName());
            }
            return captureStructure(maidCurios);
        }
        // 3. 活体结构比较：女仆当前实际结构已与目标一致才跳过（跨世界重载后女仆结构会
        //    回退为 maid.json 静态清单，此处必然不等，自动重新同步）
        Map<String, Integer> currentStructure = captureStructure(maidCurios);
        if (target.equals(currentStructure)) {
            LOGGER.debug("[LMMR] 女仆 {} 槽位结构与参照一致，跳过同步", maid.getUuid());
            return target;
        }
        // 4. 应用同步（双向收敛）
        Map<String, ICurioStacksHandler> current = maidCurios.getCurios();
        // 4a. 目标存在的类型：已有 -> 对齐数量；没有 -> 运行时创建
        for (Map.Entry<String, Integer> e : target.entrySet()) {
            String id = e.getKey();
            int count = e.getValue();
            if (current.containsKey(id)) {
                // grow/shrink，走 Curios 公开 API，安全且触发事件
                CuriosApi.getSlotHelper().setSlotsForType(id, maid, count);
            } else {
                // 运行时创建（Curios 内部公开类，仅用于清单外的稀有类型）
                createMissingSlotType(maidCurios, maid, id, count);
            }
        }
        // 4b. 女仆有但目标没有的类型：缩到 0（GUI 不可见、不可用）
        for (Map.Entry<String, ICurioStacksHandler> e : current.entrySet()) {
            String id = e.getKey();
            if (!target.containsKey(id)) {
                int cur = e.getValue().getStacks().getSlots();
                if (cur > 0) {
                    CuriosApi.getSlotHelper().shrinkSlotType(id, cur, maid);
                }
            }
        }
        LOGGER.info(
                "[LMMR] 女仆 {} 槽位已同步为 {}（参照={}）",
                maid.getUuid(),
                computeSignature(target),
                refPlayer != null ? refPlayer.getGameProfile().getName() : "玩家默认槽位");
        // 同步完成后返回女仆实际的结构（而非预期的 target），确保 buildSlots 时 live handler 状态一致
        return captureStructure(maidCurios);
    }

    @Override
    public void addCuriosSlots(
            CuriosScreenHandler handler, LittleMaidEntity maid, Map<String, int[]> pageSlice) {
        // 服务端包装女仆实时 Curios handler（真实库存，交互生效）；客户端使用本地镜像
        // handler——后者会被 Curios 同步包原地缩容到 0，导致 "Slot N not in valid range" 崩溃。
        // 物品内容显示依赖原版容器同步（updateSlotStacks），本地 handler 仅作展示载体。
        buildSlots(handler, maid, pageSlice, !maid.getWorld().isClient());
    }

    /** 解析参照玩家：开 GUI 玩家（必为主人）优先，其次女仆 ownerUuid 对应玩家，否则 null。 */
    private PlayerEntity resolveReferencePlayer(LittleMaidEntity maid, PlayerEntity openingPlayer) {
        if (openingPlayer != null) {
            return openingPlayer;
        }
        Optional<UUID> ownerUuid = TameableUtil.getTameOwnerUuid(maid);
        if (ownerUuid.isPresent() && maid.getWorld() instanceof ServerWorld serverWorld) {
            return serverWorld.getPlayerByUuid(ownerUuid.get());
        }
        return null;
    }

    /**
     * 构建目标槽位结构（类型 -> 数量）并进行数据校验。
     *
     * <p>有参照玩家：镜像其当前实际槽位（类型 + 数量完全一致）；无参照玩家：以玩家默认槽位为基准。 校验规则：类型必须存在于 Curios 注册表、数量必须在 [1,
     * MAX_SLOTS_PER_TYPE]，异常条目忽略并记录。
     */
    private Map<String, Integer> buildTargetStructure(PlayerEntity refPlayer, World world) {
        Map<String, Integer> target = new LinkedHashMap<>();
        if (refPlayer != null) {
            CuriosApi.getCuriosInventory(refPlayer)
                    .ifPresent(
                            playerCurios -> {
                                for (Map.Entry<String, ICurioStacksHandler> entry :
                                        playerCurios.getCurios().entrySet()) {
                                    ICurioStacksHandler h = entry.getValue();
                                    if (!h.isVisible()) {
                                        continue;
                                    }
                                    int count = h.getStacks().getSlots();
                                    // 直接镜像玩家实际槽位数量（含 mine_fargo 的 soul_stone 被扩容后的数量），
                                    // 不再钳制到槽位类型默认 size；数量范围由下方 [1, MAX_SLOTS_PER_TYPE] 校验兜底
                                    if (count > 0) {
                                        target.put(entry.getKey(), count);
                                    }
                                }
                            });
        } else {
            // 无主人：单向同步，以玩家默认槽位为基准
            CuriosApi.getPlayerSlots(world)
                    .forEach((id, slotType) -> target.put(id, slotType.getSize()));
        }
        // 数据校验 + 星月遗物专属槽位过滤
        Map<String, Integer> validated = new LinkedHashMap<>();
        int excludedCaSlots = 0;
        for (Map.Entry<String, Integer> e : target.entrySet()) {
            String id = e.getKey();
            int count = e.getValue();
            // 星月遗物 (Celestial Artifacts) 专属槽位：不同步给女仆
            // （catastrophe 灾厄 / etching 铭刻 / heart 心 / pendant 坠饰；
            //  scroll 槽位与神秘遗物共享，保留，物品由 MaidCurioSlot.canInsert 命名空间拦截）
            if (CELESTIAL_ARTIFACTS_SLOT_BLACKLIST.contains(id)) {
                excludedCaSlots++;
                LOGGER.debug("[LMMR] 跳过星月遗物专属槽位 {}（不向女仆/NPC同步）", id);
                continue;
            }
            if (!CuriosApi.getSlot(id, world).isPresent()) {
                LOGGER.warn("[LMMR] 槽位类型 {} 不在 Curios 注册表中，已忽略", id);
                continue;
            }
            if (count <= 0 || count > MAX_SLOTS_PER_TYPE) {
                LOGGER.warn(
                        "[LMMR] 槽位类型 {} 数量 {} 超出合法范围 [1,{}]，已忽略", id, count, MAX_SLOTS_PER_TYPE);
                continue;
            }
            validated.put(id, count);
        }
        if (excludedCaSlots > 0) {
            LOGGER.info(
                    "[LMMR] 已排除 {} 个星月遗物(Celestial Artifacts)专属槽位：{}",
                    excludedCaSlots,
                    CELESTIAL_ARTIFACTS_SLOT_BLACKLIST);
        }
        return validated;
    }

    /** 运行时创建女仆尚未拥有的槽位类型处理器（Curios 公开类；仅用于主人有而静态清单没有的稀有类型）。 */
    private void createMissingSlotType(
            ICuriosItemHandler maidCurios, LittleMaidEntity maid, String id, int count) {
        CuriosApi.getSlot(id, maid.getWorld())
                .ifPresent(
                        slotType -> {
                            Map<String, ICurioStacksHandler> newMap =
                                    new LinkedHashMap<>(maidCurios.getCurios());
                            newMap.put(
                                    id,
                                    new CurioStacksHandler(
                                            maidCurios,
                                            id,
                                            count,
                                            slotType.useNativeGui(),
                                            slotType.hasCosmetic(),
                                            slotType.canToggleRendering(),
                                            slotType.getDropRule()));
                            maidCurios.setCurios(newMap);
                            LOGGER.info(
                                    "[LMMR] 女仆 {} 运行时创建 Curios 槽位类型 {} ×{}",
                                    maid.getUuid(),
                                    id,
                                    count);
                        });
    }

    /** 计算目标槽位结构的签名（排序后的 "类型=数量;" 串），作为同步状态标记。 */
    private String computeSignature(Map<String, Integer> target) {
        return target.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
    }

    /** 从女仆 Curios 实时结构捕获最终槽位结构（可见类型-&gt;数量，保持遍历顺序，与构建槽位一一对应）。 */
    private Map<String, Integer> captureStructure(ICuriosItemHandler curios) {
        Map<String, Integer> structure = new LinkedHashMap<>();
        for (Map.Entry<String, ICurioStacksHandler> entry : curios.getCurios().entrySet()) {
            ICurioStacksHandler stacksHandler = entry.getValue();
            if (!stacksHandler.isVisible()) {
                continue;
            }
            int count = stacksHandler.getStacks().getSlots();
            if (count > 0) {
                structure.put(entry.getKey(), count);
            }
        }
        LOGGER.debug("[LMMR] Curios 可见槽位类型数量: {}", structure.size());
        return structure;
    }

    /**
     * 按页区间构建槽位（类型 → [类型内起始下标, 数量]，见 {@link
     * CuriosScreenHandler#sliceCuriosPage}）。
     *
     * <p>服务端（useLiveHandlers=true）包装女仆实时 Curios handler（真实库存，服务端交互生效）；
     * 客户端（useLiveHandlers=false）使用本地 {@link ItemStackHandler} 镜像（仅作展示载体， 与会被 Curios 同步包原地缩容的客户端
     * handler 解耦）。两侧使用完全相同的布局算法， 槽位数量与顺序一致。
     *
     * <p>类型跨页时起始下标非 0：服务端槽位绑定实时 handler 的绝对下标（穿戴回调上下文正确）；
     * 客户端镜像 handler 分配 [0, 起始下标+数量) 大小以容纳偏移区间。
     */
    private void buildSlots(
            CuriosScreenHandler handler,
            LittleMaidEntity maid,
            Map<String, int[]> pageSlice,
            boolean useLiveHandlers) {
        int slotIndex = 0;
        for (Map.Entry<String, int[]> entry : pageSlice.entrySet()) {
            String identifier = entry.getKey();
            int offset = entry.getValue()[0];
            int count = entry.getValue()[1];
            List<Boolean> renders = List.of();
            IItemHandler stacks;
            if (useLiveHandlers) {
                ICurioStacksHandler stacksHandler =
                        CuriosApi.getCuriosInventory(maid)
                                .map(c -> c.getCurios().get(identifier))
                                .orElse(null);
                // 防御：完整结构刚从同一 map 同步而来，正常必满足区间；仅极端并发下跳过该类型区间
                if (stacksHandler == null
                        || !stacksHandler.isVisible()
                        || stacksHandler.getStacks().getSlots() < offset + count) {
                    LOGGER.warn(
                            "[LMMR] 女仆 {} 的 Curios 槽位类型 {} 在构建期间发生变化，已跳过本页该类型区间",
                            maid.getUuid(),
                            identifier);
                    continue;
                }
                stacks = stacksHandler.getStacks();
                renders = stacksHandler.getRenders();
            } else {
                // 镜像 handler 覆盖 [offset, offset+count) 区间
                stacks = new ItemStackHandler(offset + count);
            }
            for (int i = 0; i < count; i++) {
                int x = SLOT_ORIGIN_X + (slotIndex % SLOT_COLS) * SLOT_SPACING;
                int y = SLOT_ORIGIN_Y + (slotIndex / SLOT_COLS) * SLOT_SPACING;
                handler.addSlotPublic(
                        new MaidCurioSlot(maid, stacks, offset + i, identifier, x, y, renders));
                slotIndex++;
            }
        }
    }
}
