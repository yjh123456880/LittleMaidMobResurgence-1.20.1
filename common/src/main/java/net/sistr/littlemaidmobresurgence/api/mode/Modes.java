package net.sistr.littlemaidmobresurgence.api.mode;

import static net.sistr.littlemaidmobresurgence.LMMRMod.MODID;

import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.mode.ArcherMode;
import net.sistr.littlemaidmobresurgence.entity.mode.BlockReservationManager;
import net.sistr.littlemaidmobresurgence.entity.mode.BlockWorkMode;
import net.sistr.littlemaidmobresurgence.entity.mode.BrewingWorkStrategy;
import net.sistr.littlemaidmobresurgence.entity.mode.FarmMode;
import net.sistr.littlemaidmobresurgence.entity.mode.FencerMode;
import net.sistr.littlemaidmobresurgence.entity.mode.FurnaceWorkStrategy;
import net.sistr.littlemaidmobresurgence.entity.mode.HealerMode;
import net.sistr.littlemaidmobresurgence.entity.mode.RipperMode;
import net.sistr.littlemaidmobresurgence.entity.mode.TorcherMode;
import net.sistr.littlemaidmobresurgence.tags.LMTags;

/** デフォルトのモードを追加するクラス メイド専用 */
public class Modes {
    public static final ModeType<FencerMode> FENCER_MODE_TYPE;
    public static final ModeType<ArcherMode> ARCHER_MODE_TYPE;
    public static final ModeType<BlockWorkMode> COOKING_MODE_TYPE;
    public static final ModeType<RipperMode> RIPPER_MODE_TYPE;
    public static final ModeType<TorcherMode> TORCHER_MODE_TYPE;
    public static final ModeType<HealerMode> HEALER_MODE_TYPE;
    public static final ModeType<BlockWorkMode> PHARMACIST_MODE_TYPE;
    public static final ModeType<FarmMode> FARM_MODE_TYPE;

    static {
        FENCER_MODE_TYPE = buildFencerMode().build();
        ARCHER_MODE_TYPE = buildArcherMode().build();
        COOKING_MODE_TYPE = buildCookingMode().build();
        RIPPER_MODE_TYPE = buildRipperMode().build();
        TORCHER_MODE_TYPE = buildTorcherMode().build();
        HEALER_MODE_TYPE = buildHealerMode().build();
        PHARMACIST_MODE_TYPE = buildPharmacistMode().build();
        FARM_MODE_TYPE = buildFarmMode().build();
    }

    public static ModeType.Builder<FencerMode> buildFencerMode() {
        return ModeType.<FencerMode>builder(
                        (type, maid) -> new FencerMode(type, "Fencer", maid, 1.0f))
                .addItemMatcher(ItemMatchers.clazz(SwordItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.clazz(AxeItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(
                        ItemMatchers.tag(LMTags.Items.FENCER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<ArcherMode> buildArcherMode() {
        return ModeType.<ArcherMode>builder((type, maid) -> new ArcherMode(type, "Archer", maid))
                .addItemMatcher(ItemMatchers.clazz(IRangedWeapon.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(
                        ItemMatchers.tag(LMTags.Items.ARCHER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<BlockWorkMode> buildCookingMode() {
        return ModeType.<BlockWorkMode>builder(
                        (type, maid) ->
                                new BlockWorkMode(
                                        type,
                                        "Cooking",
                                        maid,
                                        BlockReservationManager.INSTANCE,
                                        new FurnaceWorkStrategy()))
                .addItemMatcher(
                        ItemMatchers.tag(LMTags.Items.COOKING_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<RipperMode> buildRipperMode() {
        return ModeType.<RipperMode>builder(
                        (type, maid) -> new RipperMode(type, "Ripper", maid, 8F))
                .addItemMatcher(ItemMatchers.clazz(ShearsItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(
                        ItemMatchers.tag(LMTags.Items.RIPPER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<TorcherMode> buildTorcherMode() {
        return ModeType.<TorcherMode>builder(
                        (type, maid) -> new TorcherMode(type, "Torcher", maid, 12F))
                .addItemMatcher(
                        stack ->
                                stack.getItem() instanceof BlockItem
                                        && 9
                                                < ((BlockItem) stack.getItem())
                                                        .getBlock()
                                                        .getDefaultState()
                                                        .getLuminance(),
                        ItemMatcher.Priority.LOWER)
                .addItemMatcher(
                        ItemMatchers.tag(LMTags.Items.TORCHER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<HealerMode> buildHealerMode() {
        return ModeType.<HealerMode>builder((type, maid) -> new HealerMode(type, "Healer", maid))
                .addItemMatcher(stack -> stack.getItem().isFood(), ItemMatcher.Priority.LOWER)
                .addItemMatcher(
                        stack -> !PotionUtil.getPotion(stack).getEffects().isEmpty(),
                        ItemMatcher.Priority.LOWER)
                .addItemMatcher(
                        ItemMatchers.tag(LMTags.Items.HEALER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<BlockWorkMode> buildPharmacistMode() {
        return ModeType.<BlockWorkMode>builder(
                        (type, maid) ->
                                new BlockWorkMode(
                                        type,
                                        "Pharmacist",
                                        maid,
                                        BlockReservationManager.INSTANCE,
                                        new BrewingWorkStrategy()))
                .addItemMatcher(
                        stack -> {
                            var potion = PotionUtil.getPotion(stack);
                            return potion != Potions.EMPTY && potion.getEffects().isEmpty();
                        },
                        ItemMatcher.Priority.LOWER)
                .addItemMatcher(
                        ItemMatchers.tag(LMTags.Items.PHARMACIST_MODE),
                        ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<FarmMode> buildFarmMode() {
        return ModeType.<FarmMode>builder(
                        (type, maid) ->
                                        new FarmMode(
                                                type,
                                                "Farm",
                                                maid,
                                        LMMRMod.getConfig().work.workRange))
                .addItemMatcher(ItemMatchers.clazz(HoeItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.FARM_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static void init() {
        register("fencer", FENCER_MODE_TYPE);
        register("archer", ARCHER_MODE_TYPE);
        register("cooking", COOKING_MODE_TYPE);
        register("ripper", RIPPER_MODE_TYPE);
        register("torcher", TORCHER_MODE_TYPE);
        register("healer", HEALER_MODE_TYPE);
        register("pharmacist", PHARMACIST_MODE_TYPE);
        register("farm", FARM_MODE_TYPE);
    }

    private static void register(String id, ModeType<?> modeType) {
        ModeManager.INSTANCE.register(new Identifier(MODID, id), modeType);
    }
}
