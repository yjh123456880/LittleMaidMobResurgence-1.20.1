package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

/**
 * [zh] 玩家邀请的打雪仗小游戏目标：限时朝邀请玩家投掷雪球，结束后由女仆发放心情奖励。
 * [en] Player-invited snowball-fight goal: throws snowballs at the inviting player for a short time;
 *     the mood reward is granted when the fight ends.
 * [ja] プレイヤー招待の雪合戦ゴール：招待したプレイヤーへ期間限定で雪玉を投げます。
 */
public class SnowFightGoal extends Goal {
    private static final int CRAFT_TICKS = 24;
    private static final int THROW_INTERVAL = 14;

    private final LittleMaidEntity maid;
    private int state;
    private int timer;
    private int throwCounter;
    @Nullable private PlayerEntity partner;

    public SnowFightGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!maid.isSnowFighting() || maid.getWorld().isClient) {
            return false;
        }
        return resolvePartner() != null;
    }

    @Override
    public boolean shouldContinue() {
        if (!maid.isSnowFighting()) {
            return false;
        }
        PlayerEntity p = resolvePartner();
        if (p == null || !p.isAlive() || maid.squaredDistanceTo(p) > 16.0 * 16.0) {
            // 对手消失/过远：提前结束（不发放奖励）
            maid.cancelSnowFight();
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        state = 0;
        timer = 0;
        throwCounter = 0;
        partner = resolvePartner();
        maid.getNavigation().stop();
    }

    @Override
    public void stop() {
        partner = null;
        maid.finishSnowFight();
    }

    @Override
    public void tick() {
        if (partner == null || !partner.isAlive()) {
            return;
        }
        maid.getLookControl().lookAt(partner);
        // 制作雪球（小幅蹲下动作）
        if (state == 0) {
            maid.setSneaking(true);
            timer++;
            if (timer >= CRAFT_TICKS) {
                state = 1;
                timer = 0;
            }
            return;
        }
        // 投掷雪球
        maid.setSneaking(false);
        timer++;
        if (timer >= THROW_INTERVAL) {
            timer = 0;
            throwCounter++;
            shootSnowBall(maid.getWorld(), partner);
        }
        // 打完一轮后重新“捏雪球”，直到女仆的雪仗计时结束
        if (throwCounter >= 5) {
            throwCounter = 0;
            state = 0;
            timer = 0;
        }
    }

    private void shootSnowBall(World world, LivingEntity target) {
        world.playSound(
                null,
                maid.getX(),
                maid.getY(),
                maid.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.NEUTRAL,
                0.5F,
                0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        if (world.isClient) {
            return;
        }
        SnowballEntity snowball = new SnowballEntity(world, maid);
        snowball.setItem(Items.SNOWBALL.getDefaultStack());
        double dx = target.getX() - maid.getX();
        double dy = target.getEyeY() - maid.getEyeY();
        double dz = target.getZ() - maid.getZ();
        double dist = Math.max(1.0E-4, Math.sqrt(dx * dx + dy * dy + dz * dz));
        snowball.setVelocity(
                dx / dist * 1.3,
                dy / dist * 1.3,
                dz / dist * 1.3,
                1.3F,
                2.0F);
        world.spawnEntity(snowball);
        maid.play(LMSounds.SHOOT);
        maid.swingHand(Hand.MAIN_HAND);
    }

    @Nullable
    private PlayerEntity resolvePartner() {
        int id = maid.getSnowFightPartnerId();
        if (id == -1 || maid.getWorld().isClient) {
            return null;
        }
        if (maid.getWorld().getEntityById(id) instanceof PlayerEntity player) {
            partner = player;
            return player;
        }
        return null;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}
