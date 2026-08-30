package net.sistr.littlemaidmobresurgence.entity.mode;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmodelloader.util.Tuple;
import net.sistr.littlemaidmobresurgence.api.mode.ItemMatcher;
import net.sistr.littlemaidmobresurgence.api.mode.Mode;
import net.sistr.littlemaidmobresurgence.api.mode.ModeManager;
import net.sistr.littlemaidmobresurgence.entity.util.HasInventory;

/**
 * [zh] HasMode 的委托实现：按主手物品匹配并切换女仆的工作/战斗模式，模式断档时自动换回背包中的模式物品。
 * [en] Delegating implementation of HasMode: matches and switches the maid's work/battle mode by main-hand item,
 *     and swaps back a mode item from the inventory when the current mode can no longer continue.
 * [ja] HasMode の委譲実装：メインハンドのアイテムでモードを判定・切替し、継続不可ならインベントリ内の
 *     モードアイテムへ自動で持ち替えます。
 */
public class HasModeImpl implements HasMode {
    private final LivingEntity owner;
    private final HasInventory hasInventory;
    private final Set<Mode> modes = Sets.newHashSet();
    private final List<Tuple<ItemMatcher, Mode>> itemMatchers = new ObjectArrayList<>();
    private final Consumer<Mode> onModeChange;
    private Mode nowMode;

    public HasModeImpl(
            LivingEntity owner,
            HasInventory hasInventory,
            Set<Mode> modes,
            Consumer<Mode> onModeChange) {
        this.owner = owner;
        this.hasInventory = hasInventory;
        this.onModeChange = onModeChange;
        this.modes.addAll(modes);
        updateMatchList();
    }

    protected void updateMatchList() {
        this.itemMatchers.clear();
        this.modes.stream()
                .flatMap(
                        mode ->
                                mode.getModeType().getItemMatcherList().stream()
                                        .map(tuple -> new Tuple<>(mode, tuple)))
                .sorted(
                        Comparator
                                .<Tuple<Mode, Tuple<ItemMatcher.Priority, ItemMatcher>>>
                                        comparingInt(tuple -> tuple.getB().getA().get())
                                .reversed())
                .forEach(
                        tuple ->
                                this.itemMatchers.add(
                                        new Tuple<>(tuple.getB().getB(), tuple.getA())));
    }

    public void addMode(Mode mode) {
        modes.add(mode);
        updateMatchList();
    }

    public void addAllMode(Collection<Mode> mode) {
        modes.addAll(mode);
        updateMatchList();
    }

    @Override
    public Optional<Mode> getMode() {
        return Optional.ofNullable(this.nowMode);
    }

    @Override
    public void writeModeData(NbtCompound nbt) {
        if (this.nowMode != null) {
            ModeManager.INSTANCE
                    .getId(nowMode)
                    .ifPresent(
                            identifier -> {
                                nbt.putString("ModeID", identifier.toString());
                                NbtCompound modeData = new NbtCompound();
                                nowMode.writeModeData(modeData);
                                nbt.put("ModeData", modeData);
                            });
        }
    }

    @Override
    public void readModeData(NbtCompound nbt) {
        if (nbt.contains("ModeType") && nbt.contains("ModeData")) {
            var modeData = nbt.getCompound("ModeData");
            var modeID = Identifier.tryParse(nbt.getString("ModeID"));
            if (modeID != null) {
                // modesに一致するものがあればピック
                ModeManager.INSTANCE
                        .getType(modeID)
                        .flatMap(
                                modeType ->
                                        modes.stream()
                                                .filter(mode -> mode.getModeType() == modeType)
                                                .findFirst())
                        .ifPresent(
                                mode -> {
                                    mode.readModeData(modeData);
                                    nowMode = mode;
                                    onModeChange.accept(mode);
                                });
            }
        }
    }

    public void tick() {
        // [zh] 无当前模式：尝试匹配新模式
        // [en] No current mode: try to match a new one.
        // [ja] モード無しなら新たなモードを選択。
        if (nowMode == null) {
            getNewMode().ifPresent(this::changeNewMode);
            return;
        }
        if (!isModeContinue()) {
            // [zh] 检查背包内是否有当前模式的物品
            // [en] Check whether the inventory still has an item for the current mode.
            // [ja] 手持ちアイテムで現在のモードを継続できるかチェック。
            var index = getNowModeItemIndex();
            if (index == -1) {
                // [zh] 模式无法继续：结束并切换新模式
                // [en] Mode cannot continue: reset and switch to a new one.
                // [ja] モード続行不可：終了して新モードへ切替。
                nowMode.resetTask();
                nowMode.endModeTask();
                nowMode = null;
                onModeChange.accept(null);
                // 新たなモードに切り替え
                getNewMode().ifPresent(this::changeNewMode);
            } else {
                // [zh] 背包中仍有模式物品：换到主手
                // [en] A mode item still exists: swap it into the main hand.
                // [ja] モードアイテムがあればメインハンドと入れ替え。
                switchMainHandItem(index);
            }
        }
    }

    /**
     * [zh] 背包内是否存在当前模式的物品（存在则返回槽位索引，否则 -1）。
     * [en] Returns the inventory slot index of a current-mode item, or -1 if absent.
     * [ja] 現在のモードのアイテムがインベントリ内にある場合そのスロット番号を返し、無ければ -1。
     */
    public int getNowModeItemIndex() {
        if (nowMode == null) return -1;

        var inv = hasInventory.getInventory();
        for (int index = 0; index < inv.size(); index++) {
            var stack = inv.getStack(index);
            if (nowMode.getModeType().isModeItem(stack)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * [zh] 将主手物品与背包指定槽位互换。
     * [en] Swaps the main-hand item with the given inventory slot.
     * [ja] メインハンドとインベントリのアイテムを入れ替えます。
     */
    public void switchMainHandItem(int index) {
        var inv = hasInventory.getInventory();
        ItemStack invStack = inv.getStack(index);
        var tmp = owner.getMainHandStack();

        owner.setStackInHand(Hand.MAIN_HAND, invStack);
        inv.setStack(index, tmp);
    }

    /**
     * [zh] 当前主手物品是否仍能维持当前模式。
     * [en] Whether the current main-hand item still enables the current mode.
     * [ja] 現在の手持ちアイテムが現在のモードを有効にするなら True。
     */
    public boolean isModeContinue() {
        if (nowMode == null) return false;
        var stack = owner.getMainHandStack();
        return nowMode.getModeType().isModeItem(stack);
    }

    /**
     * [zh] 切换到新模式：结束旧模式并启动新模式。
     * [en] Switches to a new mode: resets the old one and starts the new one.
     * [ja] モードを切り替えます：旧モードを終了し新モードを開始。
     */
    public void changeNewMode(Mode mode) {
        if (nowMode != null) {
            nowMode.resetTask();
            nowMode.endModeTask();
        }
        mode.startModeTask();
        nowMode = mode;
        onModeChange.accept(mode);
    }

    /**
     * [zh] 返回主手物品对应的新模式（按匹配器优先级）。
     * [en] Returns the mode enabled by the current main-hand item (in matcher priority order).
     * [ja] 現在メインハンドにあるアイテムが有効にするモードを返します（マッチャ優先度順）。
     */
    public Optional<Mode> getNewMode() {
        var mainHand = owner.getMainHandStack();
        if (mainHand.isEmpty()) {
            return Optional.empty();
        }
        for (Tuple<ItemMatcher, Mode> tuple : this.itemMatchers) {
            if (tuple.getA().isMatch(mainHand)) {
                return Optional.of(tuple.getB());
            }
        }
        return Optional.empty();
    }

    public Set<Mode> getModes() {
        return modes;
    }
}
