package net.sistr.littlemaidmobresurgence.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmobresurgence.item.MaidCarryItem;
import net.sistr.littlemaidmobresurgence.item.MaidSouvenirItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [zh] 掉落物保护：让「女仆捕捉蛋」和「女仆纪念品」的掉落实体高亮、不可破坏、永不消失。
 *     对齐 1.12.2 的 LMEntityItemAntiDamage：除虚空外免疫伤害（爆炸/岩浆/火烧）、免疫火焰、
 *     持续发光并重置存活时间（永不消失）。覆盖普通 ItemEntity（纪念品另有 MaidSouvenirEntity，此处为无害叠加）。
 * [en] Item-entity protection: dropped maid carry eggs and souvenirs glow, are indestructible and never despawn.
 *     Mirrors 1.12.2's LMEntityItemAntiDamage: immune to all damage except the void, fire-immune, always glowing,
 *     and age is reset so they never despawn. Applies to normal ItemEntities (souvenirs also have MaidSouvenirEntity; harmless overlap).
 * [ja] ドロップ品保護：メイド捕捉卵と記念品を発光・破壊不能・消滅なしにします。
 *     1.12.2 の LMEntityItemAntiDamage に相当：奈落以外のダメージ無効・炎上無効・常時発光・
 *     寿命リセットで消滅しません。通常の ItemEntity に適用（記念品は MaidSouvenirEntity も併用、無害な重複）。
 */
@Mixin(ItemEntity.class)
public abstract class MixinItemEntityAntiDamage {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void lmr$antiDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (isProtective(((ItemEntity) (Object) this).getStack())
                && !source.isOf(DamageTypes.OUT_OF_WORLD)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isFireImmune", at = @At("RETURN"), cancellable = true)
    private void lmr$fireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (isProtective(((ItemEntity) (Object) this).getStack())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void lmr$protect(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (isProtective(self.getStack())) {
            self.setGlowing(true);
            self.setInvulnerable(true);
            int age = self.getItemAge();
            if (age >= 5900) {
                ((ItemEntityAccessor) (Object) this).setItemAge(0);
            }
        }
    }

    private static boolean isProtective(ItemStack stack) {
        return stack.getItem() instanceof MaidSouvenirItem
                || stack.getItem() instanceof MaidCarryItem;
    }
}
