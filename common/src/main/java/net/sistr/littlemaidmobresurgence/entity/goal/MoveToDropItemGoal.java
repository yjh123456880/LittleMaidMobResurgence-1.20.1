package net.sistr.littlemaidmobresurgence.entity.goal;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import org.jetbrains.annotations.Nullable;

/**
 * 主动前往掉落物并拾取的 Goal。
 *
 * <p>女仆主动寻找范围内掉落物，寻路移动到其附近，到达后调用 {@link #onPickup} 播放拾取动作并拾取。
 * 支持持续追踪被水流冲走的物品，并通过可达性过滤、目标消失与超时机制避免卡死。
 */
public abstract class MoveToDropItemGoal extends Goal {
    /** 到达目标后触发拾取的距离（格）。缩小到 1.0，使女仆贴近掉落物正上方才拾取，而非停在 2 格外。 */
    private static final double PICKUP_RADIUS = 1.0;
    /** 寻路目标距离容差（格），与拾取半径一致，让女仆走到物品所在格内。 */
    private static final int PATH_DISTANCE = 1;
    private final PathAwareEntity mob;
    private final Supplier<Float> range;
    private final Supplier<Integer> frequency;
    private final Supplier<Float> speed;
    @Nullable protected ItemEntity target;
    protected int startAge;

    public MoveToDropItemGoal(
            PathAwareEntity mob,
            Supplier<Float> range,
            Supplier<Integer> frequency,
            Supplier<Float> speed) {
        this.mob = mob;
        this.range = range;
        this.frequency = frequency;
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // 捡取开关关闭时不主动走去捡取（被动吸附也已关闭）
        if (mob instanceof net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity maid
                && !maid.isPickupItem()) {
            return false;
        }
        // 固定间隔检查：扫描间隔不超过 5 tick（0.25 秒），减少女仆对掉落物的响应停顿与犹豫
        int freq = Math.min(Math.max(1, this.getTickCount(frequency.get())), 5);
        if (isInventoryFull() || this.mob.age % freq != 0) {
            return false;
        }
        ItemEntity selected = selectReachableTarget();
        if (selected == null) {
            return false;
        }
        this.target = selected;
        this.startAge = mob.age;
        // 不在 canStart 里直接控制 navigation（否则与跟随 Goal 争用 Control.MOVE 时
        // goal 未真正 start，tick 不执行、onPickup 不触发）；改到 start() 中驱动。
        return true;
    }

    /**
     * 按距离升序逐个尝试，返回第一个「有视线且可达」的掉落物。
     *
     * <p>避免最近目标不可达（水中/悬崖/隔墙）时整体放弃拾取；视线过滤避免隔墙反复无效寻路。
     */
    @Nullable
    private ItemEntity selectReachableTarget() {
        return findAroundDropItem().stream()
                .filter(item -> !item.isRemoved())
                .filter(this::canPickItem)
                .sorted(Comparator.comparingDouble(item -> item.squaredDistanceTo(mob)))
                .filter(mob::canSee)
                .filter(item -> mob.getNavigation().findPathTo(item.getBlockPos(), PATH_DISTANCE) != null)
                .findFirst()
                .orElse(null);
    }

    /** 子类可覆写的目标过滤（如背包能否放入该物品堆）。默认放行。 */
    protected boolean canPickItem(ItemEntity item) {
        return true;
    }

    @Override
    public void start() {
        super.start();
        if (mob instanceof SoundPlayable) {
            ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_I);
        }
        // goal 真正激活后才控制 navigation，确保 tick 会执行并完成拾取
        if (target != null && !target.isRemoved()) {
            Path path = mob.getNavigation().findPathTo(target.getBlockPos(), PATH_DISTANCE);
            if (path != null) {
                mob.getNavigation().startMovingAlong(path, speed.get());
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        if (target == null || target.isRemoved()) {
            return false;
        }
        // 已进入拾取范围：即使导航已 idle（已到达目标格），也让 tick 完成拾取
        if (target.squaredDistanceTo(mob) < PICKUP_RADIUS * PICKUP_RADIUS) {
            return true;
        }
        if (mob.getNavigation().isIdle()) {
            return false;
        }
        // 超时防卡死：frequency*10 tick 内未拾取则放弃
        return (mob.age - startAge) < Math.max(20, this.getTickCount(frequency.get()) * 10);
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        // 先判断拾取：到达目标附近即拾取（不依赖挥臂动画，动画缺失不影响逻辑）
        if (target.squaredDistanceTo(mob) < PICKUP_RADIUS * PICKUP_RADIUS) {
            onPickup(target);
            // 捡取后立即寻找下一个掉落物继续，实现持续扫描拾取
            ItemEntity next = selectReachableTarget();
            if (next == null || isInventoryFull()) {
                this.target = null;
                return;
            }
            this.target = next;
            this.startAge = mob.age;
            Path nextPath = mob.getNavigation().findPathTo(next.getBlockPos(), PATH_DISTANCE);
            if (nextPath != null) {
                mob.getNavigation().startMovingAlong(nextPath, speed.get());
            }
            return;
        }
        // 持续追踪目标（物品可能被水流冲走），寻路失败则等待超时自动停止
        Path path = mob.getNavigation().findPathTo(target.getBlockPos(), PATH_DISTANCE);
        if (path != null) {
            mob.getNavigation().startMovingAlong(path, speed.get());
        }
    }

    @Override
    public void stop() {
        this.target = null;
        super.stop();
    }

    /** 到达掉落物后由子类实现的拾取动作。 */
    public abstract void onPickup(ItemEntity target);

    public abstract boolean isInventoryFull();

    /** 垂直搜索范围（格），与车万女仆 VERTICAL_SEARCH_RANGE 一致，便于捡到上下层掉落物。 */
    private static final float VERTICAL_SEARCH_RANGE = 4.0F;

    public List<ItemEntity> findAroundDropItem() {
        float range = this.range.get();
        return mob.getWorld()
                .getEntitiesByClass(
                        ItemEntity.class,
                        mob.getBoundingBox().expand(range, VERTICAL_SEARCH_RANGE, range),
                        item -> !item.isRemoved()
                                && item.squaredDistanceTo(mob) < range * range);
    }
}
