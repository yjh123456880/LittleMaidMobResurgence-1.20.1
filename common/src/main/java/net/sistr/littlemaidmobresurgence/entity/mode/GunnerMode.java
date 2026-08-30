package net.sistr.littlemaidmobresurgence.entity.mode;

import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.compat.TaczCompat;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 枪手模式：主手持 TACZ 枪械时激活，锁定敌人射击。
 *     射击/瞄准/拔枪/拉栓/换弹全部委托给 TACZ 的实体射击状态机（参考车万女仆实现）；
 *     攻击范围与弓手一致（默认 16 格 × 射程系数）。TACZ 未安装时该模式不会激活。
 * [en] Gunner mode: activates when a TACZ gun is held in the main hand and locks onto enemies.
 *     Shooting/aiming/drawing/bolting/reloading are delegated to TACZ's entity shooting state machine (modeled after Touhou Little Maid);
 *     attack range matches the archer (16 blocks × range factor). Never activates without TACZ installed.
 * [ja] ガンナーモード：メインハンドに TACZ の銃を持つと有効になり、敵をロックして射撃します。
 *     射撃・照準・抜銃・ボルト・リロードは TACZ の射撃ステートマシンに委譲（車万女僕の実装を参考）。
 *     射程は弓手と同じ（16ブロック×射程係数）。TACZ 未導入なら有効になりません。
 */
public class GunnerMode extends AbstractArcherMode<Item> {
    protected int cool;

    public GunnerMode(ModeType<? extends GunnerMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name, mob);
        this.cool = 10;
    }

    @Override
    public boolean shouldExecute() {
        return TaczCompat.isGun(this.mob.getMainHandStack()) && super.shouldExecute();
    }

    @Override
    protected void tickRangedAttack(
            LivingEntity target,
            ItemStack itemStack,
            boolean canSee,
            double distanceSq,
            float maxRange) {
        // [zh] 看不见或超出射程时不射击（走位逻辑由父类处理）
        // [en] Do not fire when out of sight or beyond range (movement is handled by the parent).
        // [ja] 見えない・射程外では撃ちません（移動は親クラスが処理）。
        if (!canSee || maxRange * maxRange < distanceSq) {
            return;
        }
        if (0 < this.cool) {
            this.cool--;
            return;
        }
        // [zh] 射线检查：射线有友方阻挡时不射击（与弓手相同）
        // [en] Ray check: don't shoot if a friendly blocks the line (same as the archer).
        // [ja] 射線チェック：射線上に味方がいる場合は撃ちません（弓手と同じ）。
        var result =
                this.raycastShootLine(target, maxRange, this.friendlyShotLineFilter());
        if (result.isPresent()) {
            this.cool = 10;
            return;
        }
        // [zh] 射击交给 TACZ（内部处理瞄准/拔枪/拉栓/换弹），返回下次可行动的等待 tick
        // [en] Delegate the shot to TACZ (aim/draw/bolt/reload handled inside); returns ticks until the next action.
        // [ja] 射撃は TACZ に委譲（照準・抜銃・ボルト・リロード含む）。次アクションまでの待機tickを返します。
        int next = TaczCompat.performGunAttack(this.mob, target, itemStack);
        this.cool = Math.max(1, next);
        this.mob.swingHand(Hand.MAIN_HAND);
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        // [zh] 16 格 × 射程系数（与弓箭一致）
        // [en] 16 blocks × range factor (same as the archer).
        // [ja] 16ブロック×射程係数（弓と同じ）。
        return 16F * LMMRMod.getConfig().work.archerShootDistanceFactor;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.cool = 10;
        TaczCompat.stopAim(this.mob);
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack stack) {
        return TaczCompat.isGun(stack) ? Optional.of(stack.getItem()) : Optional.empty();
    }
}
