package net.sistr.littlemaidmobresurgence.forge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmobresurgence.api.mode.ItemMatcher;
import net.sistr.littlemaidmobresurgence.api.mode.ModeManager;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.compat.GoetyCompat;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.mode.GoetyMode;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 诡厄巫法（Goety）手杖适配实现 + 诡厄巫法模式注册（仅当 Goety 已安装时加载本类）。
 *
 * <p>Goety 的发布 jar 存在混合映射（实体类覆写 MC 方法时混用 named/SRG 名），导致 Loom 的
 * remapJar 无法对其做反向重映射。因此本适配器<b>不编译期依赖任何 Goety 类</b>，全部通过
 * 反射调用（类名/方法名已对照 goety-2.5.57.2 的字节码核实）：
 * <ul>
 *   <li>识别：{@code IWand.isInstance(item)}</li>
 *   <li>施法：{@code WandUtil.findWand/getSpell/getStats} + {@code ISpell.SpellResult}</li>
 *   <li>法术来源：优先手杖内嵌聚晶；手杖无聚晶时，全位置（主手/副手/物品栏/扩容背包/饰品栏）
 *       检索聚晶包（focus_bag 11格 / focus_pack 21格），收集包内含聚晶的法术随机施放</li>
 *   <li>女仆无灵魂能量体系，直接触发 SpellResult 绕过玩家专属的 SEHelper 记账</li>
 * </ul>
 */
public class GoetyCompatImpl implements GoetyCompat.WandAdapter {

    private static final String IWAND = "com.Polarice3.Goety.api.items.magic.IWand";
    private static final String ISPELL = "com.Polarice3.Goety.api.magic.ISpell";
    private static final String ITHROW = "com.Polarice3.Goety.api.magic.ITouchSpell";
    private static final String IBLOCK = "com.Polarice3.Goety.api.magic.IBlockSpell";
    private static final String WAND_UTIL = "com.Polarice3.Goety.utils.WandUtil";
    private static final String SPELL_STAT = "com.Polarice3.Goety.common.magic.SpellStat";
    private static final String FOCUS_BAG = "com.Polarice3.Goety.common.items.magic.FocusBag";
    private static final String FOCUS_PACK = "com.Polarice3.Goety.common.items.magic.FocusPack";
    private static final String FOCUS_BAG_HANDLER =
            "com.Polarice3.Goety.common.items.handler.FocusBagItemHandler";
    private static final String IFOCUS = "com.Polarice3.Goety.api.items.magic.IFocus";

    private static Class<?> iwandClass;
    private static Class<?> ispellClass;
    private static Class<?> touchClass;
    private static Class<?> blockClass;
    private static Class<?> wandUtilClass;
    private static Class<?> spellStatClass;
    private static Method findWandMethod;
    private static Method getSpellMethod;
    private static Method getStatsMethod;
    private static Method spellResultMethod;
    private static Method conditionsMetMethod;

    // 聚晶包相关（可选：版本无聚晶包时自动降级为仅手杖施法）
    private static Class<?> focusBagClass;
    private static Class<?> focusPackClass;
    private static Class<?> focusBagHandlerClass;
    private static Class<?> ifocusClass;
    private static Method bagGetMethod;
    private static Method getSpellOfFocusMethod;

    /** Goety 已安装时调用：注入适配器并注册诡厄巫法模式。 */
    public static void init() {
        if (!loadApi()) {
            return;
        }
        GoetyCompat.setAdapter(new GoetyCompatImpl());
        ModeManager.INSTANCE.register(
                new Identifier("littlemaidmobresurgence", "goety"),
                ModeType.<GoetyMode>builder((type, maid) -> new GoetyMode(type, "Goety", maid))
                        .addItemMatcher(GoetyCompat::isWand, ItemMatcher.Priority.LOWER)
                        .build());
    }

    /** 反射加载 Goety API；任一步失败则禁用本联动（Goety 缺失/版本不符时安全降级）。 */
    private static synchronized boolean loadApi() {
        try {
            iwandClass = Class.forName(IWAND);
            ispellClass = Class.forName(ISPELL);
            touchClass = Class.forName(ITHROW);
            blockClass = Class.forName(IBLOCK);
            wandUtilClass = Class.forName(WAND_UTIL);
            spellStatClass = Class.forName(SPELL_STAT);
            findWandMethod = wandUtilClass.getMethod("findWand", LivingEntity.class);
            getSpellMethod = wandUtilClass.getMethod("getSpell", LivingEntity.class);
            getStatsMethod =
                    wandUtilClass.getMethod("getStats", LivingEntity.class, ispellClass);
            spellResultMethod =
                    ispellClass.getMethod(
                            "SpellResult",
                            ServerWorld.class,
                            LivingEntity.class,
                            ItemStack.class,
                            spellStatClass);
            conditionsMetMethod =
                    ispellClass.getMethod("conditionsMet", ServerWorld.class, LivingEntity.class);
            loadFocusBagApi();
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** 反射加载聚晶包相关 API（失败仅关闭聚晶包来源，不影响手杖施法）。 */
    private static void loadFocusBagApi() {
        try {
            focusBagClass = Class.forName(FOCUS_BAG);
            focusPackClass = Class.forName(FOCUS_PACK);
            focusBagHandlerClass = Class.forName(FOCUS_BAG_HANDLER);
            ifocusClass = Class.forName(IFOCUS);
            bagGetMethod = focusBagHandlerClass.getMethod("get", ItemStack.class);
            getSpellOfFocusMethod = ifocusClass.getMethod("getSpell");
        } catch (ReflectiveOperationException ignored) {
            focusBagClass = null;
        }
    }

    @Override
    public boolean isWand(ItemStack stack) {
        return iwandClass != null && iwandClass.isInstance(stack.getItem());
    }

    /** 该物品是否为聚晶包/大型聚晶包。 */
    private boolean isFocusBag(ItemStack stack) {
        if (stack == null || stack.isEmpty() || focusBagClass == null) {
            return false;
        }
        Item item = stack.getItem();
        return focusBagClass.isInstance(item) || focusPackClass.isInstance(item);
    }

    /** 从单个聚晶包收集所有聚晶搭载的法术（反射读取 Forge capability handler）。 */
    private List<Object> collectSpellsFromBag(ItemStack bag) {
        List<Object> spells = new ArrayList<>();
        if (!isFocusBag(bag)) {
            return spells;
        }
        try {
            Object handler = bagGetMethod.invoke(null, bag);
            Method getSlots = handler.getClass().getMethod("getSlots");
            Method getStackInSlot = handler.getClass().getMethod("getStackInSlot", int.class);
            int slots = (int) getSlots.invoke(handler);
            for (int i = 0; i < slots; i++) {
                ItemStack focusStack = (ItemStack) getStackInSlot.invoke(handler, i);
                if (focusStack == null || focusStack.isEmpty()) {
                    continue;
                }
                if (ifocusClass.isInstance(focusStack.getItem())) {
                    Object spell = getSpellOfFocusMethod.invoke(focusStack.getItem());
                    if (spell != null) {
                        spells.add(spell);
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // 读取失败则忽略该包
        }
        return spells;
    }

    /** 全位置检索聚晶包（主手/副手/物品栏/扩容背包/饰品栏），收集其内含聚晶的法术并随机取一个。 */
    private Object findFocusBagSpell(LittleMaidEntity caster) {
        List<Object> spells = new ArrayList<>();
        // 主手 + 副手
        collectFocusBags(caster.getMainHandStack(), spells);
        collectFocusBags(caster.getOffHandStack(), spells);
        // 物品栏（含扩容背包，若位于同一 Inventory）
        var inv = caster.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            collectFocusBags(inv.getStack(i), spells);
        }
        // 饰品栏（Curios，含可装备的聚晶包）
        CuriosApi.getCuriosInventory(caster)
                .ifPresent(
                        handler ->
                                handler.findCurios(this::isFocusBag)
                                        .forEach(
                                                slotResult ->
                                                        spells.addAll(
                                                                collectSpellsFromBag(
                                                                        slotResult.stack()))));
        if (spells.isEmpty()) {
            return null;
        }
        return spells.get(caster.getRandom().nextInt(spells.size()));
    }

    /** 若物品是聚晶包则收集其内含聚晶的法术（只检索聚晶包，不检索散落聚晶）。 */
    private void collectFocusBags(ItemStack stack, List<Object> spells) {
        if (isFocusBag(stack)) {
            spells.addAll(collectSpellsFromBag(stack));
        }
    }

    @Override
    public int performWandAttack(LittleMaidEntity caster, LivingEntity target, ItemStack wand) {
        if (!(caster.getWorld() instanceof ServerWorld server)) {
            return 100;
        }
        try {
            // 1) 取手中实际手杖（主手/副手）与其内嵌聚晶的法术
            ItemStack heldWand = (ItemStack) findWandMethod.invoke(null, caster);
            Object spell = heldWand.isEmpty() ? null : getSpellMethod.invoke(null, caster);
            ItemStack castSourceStack = heldWand;
            // 2) 手杖无聚晶时，尝试从聚晶包（物品栏/扩容背包/饰品栏）读取法术
            if (spell == null) {
                spell = findFocusBagSpell(caster);
                castSourceStack = ItemStack.EMPTY;
                if (spell == null) {
                    // 无手杖聚晶、无聚晶包法术
                    return 40;
                }
            }
            // 触碰/方块类法术不适合自动远程施放，跳过
            if (touchClass.isInstance(spell) || blockClass.isInstance(spell)) {
                return 40;
            }
            if (!(boolean) conditionsMetMethod.invoke(spell, server, caster)) {
                return 20;
            }
            Object stats = getStatsMethod.invoke(null, caster, spell);
            spellResultMethod.invoke(spell, server, caster, castSourceStack, stats);
            return 20;
        } catch (InvocationTargetException | IllegalAccessException e) {
            return 100;
        }
    }

    @Override
    public void stopCast(LittleMaidEntity maid) {
        // Goety 手杖为瞬时施放，无持续状态需要清理
    }
}
