package net.sistr.littlemaidmobresurgence.mixin.forge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.forge.MaidInventoryHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [zh] 让 TACZ 等模组能检索女仆【物品栏/扩容背包】中的弹药盒/子弹以完成换弹。
 *     背景：Forge 给 {@link LivingEntity#getCapability} 打补丁后，对 ITEM_HANDLER（side 为 null）
 *     会直接短路返回「双手 + 盔甲」的 6 格处理器，不查询 AttachCapabilitiesEvent 的 provider。
 *     本 mixin 参考车万女仆（Touhou Little Maid）的枪手兼容：仅当目标是小女仆时接管该能力，
 *     返回「背包 + 双手」的组合处理器；其它生物行为完全不变。
 * [en] Lets TACZ and similar mods find ammo boxes/bullets in the maid's inventory/expanded backpack for reloading.
 *     Forge patches {@link LivingEntity#getCapability} to short-circuit ITEM_HANDLER (side == null) to a fixed
 *     6-slot hands+armor handler, ignoring AttachCapabilitiesEvent providers. Modeled after Touhou Little Maid's
 *     gunner compat: this mixin intercepts only for LittleMaidEntity and returns a combined hands+inventory handler;
 *     other entities are untouched.
 * [ja] TACZ 等がメイドのインベントリ・拡張バックパック内の弾薬を検索してリロードできるようにします。
 *     Forge は {@link LivingEntity#getCapability} をパッチし、ITEM_HANDLER（side=null）では
 *     「両手＋防具」の6スロット処理を直接返すため、AttachCapabilitiesEvent の provider は参照されません。
 *     車万女僕（Touhou Little Maid）のガンナー互換を参考に、LittleMaidEntity のみ能力を差し替え、
 *     「バックパック＋両手」の合成ハンドラを返します。他エンティティの挙動は変わりません。
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityMaidInventory {

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
    private <T> void lmmr$maidInventoryCapability(
            Capability<T> capability,
            @Nullable Direction side,
            CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (capability != ForgeCapabilities.ITEM_HANDLER || side != null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.isAlive() || !(self instanceof LittleMaidEntity maid)) {
            return;
        }
        // 实时读取 主手 / 副手 / 物品栏（含扩容背包），TACZ 的 hasInventoryAmmo /
        // findAndExtractInventoryAmmos 会直接扫描该处理器并从中消耗弹药。
        cir.setReturnValue(LazyOptional.of(() -> new MaidInventoryHandler(maid)).cast());
    }
}
