package net.sistr.littlemaidmobresurgence.forge;

import io.redspace.ironsspellbooks.api.item.weapons.MagicSwordItem;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.item.CastingItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmobresurgence.api.mode.ItemMatcher;
import net.sistr.littlemaidmobresurgence.api.mode.ModeManager;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.compat.IronsSpellCompat;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.mode.IronsSpellMode;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 铁魔法（Iron's Spells n Spellbooks）施法适配实现 + 铁魔法模式注册（仅当该模组已安装时加载）。
 *
 * <p>识别规则：{@link #isIronMagicItem} —— 法术容器（法术书/魔剑等，按 NBT 判定）或法杖/魔剑
 * 等施法物品（按类判定 {@link CastingItem}/{@link MagicSwordItem}）。主手命中时以 LOW 优先级
 * 判定为铁魔法模式（优先于剑客对近战武器的 LOWER 判定）。
 *
 * <p>施法来源：主手 + 副手 + 物品栏 + 扩容背包 + 饰品栏中的所有法术容器；随机选一个可用法术，
 * 经 {@link MagicData#initiateCast} 开始施法 → 逐 tick 推进 handleCastDuration/onServerCastTick
 * → 完成时 onCast/onServerCastComplete 并记录冷却。
 */
public class IronsSpellCompatImpl implements IronsSpellCompat.SpellbookAdapter {

    /** 每个女仆一份施法状态（冷却）。 */
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    /** Iron's Spells 已安装时调用：注入适配器并注册铁魔法模式。 */
    public static void init() {
        IronsSpellCompat.setAdapter(new IronsSpellCompatImpl());
        ModeManager.INSTANCE.register(
                new Identifier("littlemaidmobresurgence", "irons_spell"),
                ModeType.<IronsSpellMode>builder(
                                (type, maid) -> new IronsSpellMode(type, "IronsSpell", maid))
                        // 主手命中铁魔法施法物品时优先进入铁魔法模式（LOW 高于剑客对近战武器的 LOWER）
                        .addItemMatcher(
                                stack -> IronsSpellCompat.isIronMagicItem(stack),
                                ItemMatcher.Priority.LOW)
                        .build());
    }

    private static final class State {
        SpellSlot current;
        boolean active;
        CastSource cachedSource = CastSource.SPELLBOOK;
        final Map<String, Integer> cooldowns = new HashMap<>();
    }

    @Override
    public boolean isSpellContainer(ItemStack stack) {
        return ISpellContainer.isSpellContainer(stack);
    }

    @Override
    public boolean isIronMagicItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (ISpellContainer.isSpellContainer(stack)) {
            return true;
        }
        // 法杖/魔剑等施法物品（即使容器 NBT 为空也按类识别）
        return stack.getItem() instanceof CastingItem || stack.getItem() instanceof MagicSwordItem;
    }

    /** 从单个物品收集法术到候选列表（无容器/空容器/冷却中忽略）。 */
    private void collectSpells(ItemStack stack, State st, List<SpellSlot> out) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ISpellContainer container = ISpellContainer.get(stack);
        if (container == null || container.isEmpty()) {
            return;
        }
        for (SpellSlot slot : container.getActiveSpells()) {
            AbstractSpell spell = slot.getSpell();
            if (spell == null) {
                continue;
            }
            if (st.cooldowns.getOrDefault(spell.getSpellId(), 0) > 0) {
                continue;
            }
            out.add(slot);
        }
    }

    @Override
    public int performSpellAttack(LittleMaidEntity caster, LivingEntity target, ItemStack bookOrStaff) {
        if (!(caster.getWorld() instanceof ServerWorld server)) {
            return 20;
        }
        State st = STATES.computeIfAbsent(caster.getUuid(), k -> new State());
        // 取实体附着的真实魔法数据（Iron's mixin 注入），并在 initiateCast 前先触发
        // getSyncedData() 懒初始化，避免 MagicData.initiateCast 直接访问 syncedSpellData 字段的 NPE
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        magicData.getSyncedData();

        // 持续施法中：推进施法进程
        if (st.active && st.current != null) {
            AbstractSpell spell = st.current.getSpell();
            int level = st.current.getLevel();
            magicData.handleCastDuration();
            if (magicData.isCasting()) {
                spell.onServerCastTick(server, level, caster, magicData);
            }
            int remaining = magicData.getCastDurationRemaining();
            if (remaining <= 0) {
                complete(server, caster, st, spell, level);
                return 5;
            }
            // 持续性法术：每10tick触发一次效果
            if (spell.getCastType() == CastType.CONTINUOUS && (remaining + 1) % 10 == 0) {
                spell.onCast(server, level, caster, st.cachedSource, magicData);
            }
            return 1;
        }

        // 未施法：从 主手+副手+物品栏+扩容背包+饰品栏 的法术容器收集可用法术并随机施放
        List<SpellSlot> available = new ArrayList<>();
        collectSpells(caster.getMainHandStack(), st, available);
        collectSpells(caster.getOffHandStack(), st, available);
        var inv = caster.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            collectSpells(inv.getStack(i), st, available);
        }
        CuriosApi.getCuriosInventory(caster)
                .ifPresent(
                        handler ->
                                handler.findCurios(ISpellContainer::isSpellContainer)
                                        .forEach(
                                                slotResult ->
                                                        collectSpells(
                                                                slotResult.stack(), st, available)));
        // 冷却递减
        st.cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        if (available.isEmpty()) {
            return 30;
        }
        SpellSlot chosen = available.get(caster.getRandom().nextInt(available.size()));
        AbstractSpell spell = chosen.getSpell();
        int level = chosen.getLevel();
        if (!spell.checkPreCastConditions(server, level, caster, magicData)) {
            return 20;
        }
        int castTime = spell.getEffectiveCastTime(level, caster);
        CastSource source =
                caster.getMainHandStack().getItem() instanceof MagicSwordItem
                        ? CastSource.SWORD
                        : CastSource.SPELLBOOK;
        st.cachedSource = source;
        magicData.initiateCast(spell, level, castTime, source, "offhand");
        spell.onServerPreCast(server, level, caster, magicData);
        st.current = chosen;
        st.active = true;
        return 1;
    }

    /** 施法完成：触发结果并记录冷却。 */
    private void complete(ServerWorld server, LittleMaidEntity caster, State st, AbstractSpell spell, int level) {
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        magicData.getSyncedData();
        if (spell.getCastType() == CastType.LONG || spell.getCastType() == CastType.INSTANT) {
            spell.onCast(server, level, caster, st.cachedSource, magicData);
        }
        spell.onServerCastComplete(server, level, caster, magicData, false);
        st.cooldowns.put(spell.getSpellId(), spell.getSpellCooldown());
        magicData.resetCastingState();
        st.current = null;
        st.active = false;
    }

    @Override
    public void stopCast(LittleMaidEntity maid) {
        State st = STATES.remove(maid.getUuid());
        if (st != null) {
            st.active = false;
            st.current = null;
            if (!maid.getWorld().isClient) {
                MagicData magicData = MagicData.getPlayerMagicData(maid);
                magicData.getSyncedData();
                magicData.resetCastingState();
            }
        }
    }
}
