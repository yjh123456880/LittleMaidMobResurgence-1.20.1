package net.sistr.littlemaidmobresurgence.forge;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/**
 * 女仆的 Curios 饰品槽位。
 *
 * <p>继承 Forge 的 {@link SlotItemHandler}（Forge 原生类，不在 Yarn 映射内）， 包装 Curios 的动态物品栈 handler。仅 forge
 * 模块编译，common 不可引用。
 *
 * <p>服务端包装女仆实时 Curios handler；客户端包装本地镜像 handler（由开屏数据包声明结构构建）。 所有访问均带越界防御：Curios 同步包会原地缩容
 * handler，若槽位下标超出当前容量则视为空槽位，避免渲染崩溃。
 */
public class MaidCurioSlot extends SlotItemHandler {
    private final String identifier;
    private final LittleMaidEntity maid;
    private final SlotContext slotContext;
    private final List<Boolean> renderStatuses;
    private final IItemHandler itemHandler;
    private final int slotIndex;

    public MaidCurioSlot(
            LittleMaidEntity maid,
            IItemHandler handler,
            int index,
            String identifier,
            int xPosition,
            int yPosition,
            List<Boolean> renders) {
        super(handler, index, xPosition, yPosition);
        this.identifier = identifier;
        this.maid = maid;
        this.renderStatuses = renders;
        this.itemHandler = handler;
        this.slotIndex = index;
        this.slotContext = new SlotContext(identifier, maid, index, false, true);
    }

    public String getIdentifier() {
        return this.identifier;
    }

    /** 越界防御：handler 被 Curios 原地缩容时按空槽位处理，防止 "Slot N not in valid range" 崩溃。 */
    private boolean isOutOfRange() {
        return this.slotIndex >= this.itemHandler.getSlots();
    }

    @Override
    public ItemStack getStack() {
        if (isOutOfRange()) {
            return ItemStack.EMPTY;
        }
        return super.getStack();
    }

    /** 从 Curios 槽位类型取图标背景（若存在）。 */
    @Override
    public Pair<Identifier, Identifier> getBackgroundSprite() {
        Identifier icon =
                CuriosApi.getSlot(identifier, maid.getWorld())
                        .map(slotType -> slotType.getIcon())
                        .orElse(null);
        if (icon != null) {
            return Pair.of(new Identifier("textures/atlas/blocks.png"), icon);
        }
        return super.getBackgroundSprite();
    }

    @Override
    public void setStack(@Nonnull ItemStack stack) {
        // 越界防御：handler 已缩容时丢弃写入，防止 validateSlotIndex 异常
        if (isOutOfRange()) {
            return;
        }
        ItemStack current = this.getStack();
        boolean unchanged = current.isEmpty() && stack.isEmpty();
        super.setStack(stack);
        // 仅服务端触发穿戴回调：客户端的原版容器同步（updateSlotStacks）也会调用 setStack，
        // 不加此判断会在客户端重复触发 onEquipFromUse（音效等副作用）
        if (!unchanged && !ItemStack.areEqual(current, stack) && !this.maid.getWorld().isClient) {
            CuriosApi.getCurio(stack).ifPresent(curio -> curio.onEquipFromUse(this.slotContext));
        }
    }

    /** 星月遗物 (Celestial Artifacts) 饰品限定由玩家装备，女仆等 NPC 实体不可装载。 */
    private static final String CELESTIAL_ARTIFACTS_NAMESPACE = "celestial_artifacts";

    @Override
    public boolean canInsert(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        // 星月遗物：非玩家实体一律拒绝（女仆/NPC 无法装备其任何饰品）。
        // 该模组未注册自定义 Curios 槽位类型，其饰品全部放入标准槽位
        // （charm/bracelet/back/body 等），故在物品命名空间层拦截即可覆盖全部入口
        if (!(slotContext.entity() instanceof PlayerEntity)) {
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            if (CELESTIAL_ARTIFACTS_NAMESPACE.equals(itemId.getNamespace())) {
                return false;
            }
        }
        // 槽位类型匹配验证（与原版玩家饰品栏一致）：饰品只能放入其声明支持的槽位类型，
        // 由 CuriosApi.isStackValid 依据槽位类型的 validators（curios:tag 检查物品是否在
        // curios:{槽位id} 物品标签中，或 ICurio.getSlotsIdentifier() 显式声明）判定。
        // 缺失此验证会导致任意饰品可放入任意槽位、同类型饰品重复放置的错误
        if (!CuriosApi.isStackValid(slotContext, stack)) {
            return false;
        }
        return CuriosApi.getCurio(stack).map(curio -> curio.canEquip(slotContext)).orElse(false);
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerEntity) {
        if (!super.canTakeItems(playerEntity)) {
            return false;
        }
        return CuriosApi.getCurio(this.getStack())
                .map(curio -> curio.canUnequip(slotContext))
                .orElse(true);
    }

    @Override
    public int getMaxItemCount() {
        return this.getStack().getMaxCount();
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return Math.min(this.getMaxItemCount(), stack.getMaxCount());
    }

    @OnlyIn(Dist.CLIENT)
    public String getSlotName() {
        String key = "curios.identifier." + this.identifier;
        String translated = net.minecraft.client.resource.language.I18n.translate(key);
        if (!translated.equals(key)) {
            return translated;
        }
        if (!this.identifier.isEmpty()) {
            return Character.toUpperCase(this.identifier.charAt(0))
                    + this.identifier.substring(1).toLowerCase();
        }
        return "";
    }
}
