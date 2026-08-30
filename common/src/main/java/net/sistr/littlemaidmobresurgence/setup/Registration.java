package net.sistr.littlemaidmobresurgence.setup;

import static net.sistr.littlemaidmobresurgence.LMMRMod.MODID;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.Instrument;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.sistr.littlemaidmobresurgence.block.SalaryBoxBlock;
import net.sistr.littlemaidmobresurgence.block.SalaryBoxBlockEntity;
import net.sistr.littlemaidmobresurgence.entity.BackpackScreenHandler;
import net.sistr.littlemaidmobresurgence.entity.CuriosScreenHandler;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidmobresurgence.entity.MaidSouvenirEntity;
import net.sistr.littlemaidmobresurgence.entity.RebellionProxyEntity;
import net.sistr.littlemaidmobresurgence.entity.RestSeatEntity;
import net.sistr.littlemaidmobresurgence.item.BackpackUpgradeItem;
import net.sistr.littlemaidmobresurgence.item.LittleMaidSpawnEggItem;
import net.sistr.littlemaidmobresurgence.item.MaidCarryItem;
import net.sistr.littlemaidmobresurgence.item.MaidStickItem;
import net.sistr.littlemaidmobresurgence.item.MaidSouvenirItem;

public class Registration {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(MODID, RegistryKeys.ENTITY_TYPE);
    private static final DeferredRegister<ItemGroup> ITEM_GROUPS =
            DeferredRegister.create(MODID, RegistryKeys.ITEM_GROUP);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MODID, RegistryKeys.ITEM);
    private static final DeferredRegister<ScreenHandlerType<?>> SCREEN_HANDLERS =
            DeferredRegister.create(MODID, RegistryKeys.SCREEN_HANDLER);
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MODID, RegistryKeys.BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(MODID, RegistryKeys.BLOCK_ENTITY_TYPE);

    public static void init() {
        ENTITIES.register();
        ITEM_GROUPS.register();
        BLOCKS.register();
        ITEMS.register();
        SCREEN_HANDLERS.register();
        BLOCK_ENTITIES.register();
    }

    // エンティティ
    public static final RegistrySupplier<EntityType<LittleMaidEntity>> LITTLE_MAID_MOB =
            ENTITIES.register(
                    "little_maid_mob",
                    () ->
                            EntityType.Builder.<LittleMaidEntity>create(
                                            LittleMaidEntity::new, SpawnGroup.CREATURE)
                                    .setDimensions(0.5F, 1.35F)
                                    .build("little_maid_mob"));
    public static final RegistrySupplier<EntityType<MaidSouvenirEntity>> MAID_SOUVENIR_ENTITY =
            ENTITIES.register(
                    "maid_souvenir",
                    () ->
                            EntityType.Builder.<MaidSouvenirEntity>create(
                                            MaidSouvenirEntity::new, SpawnGroup.MISC)
                                    .setDimensions(0.25F, 0.25F)
                                    .build("maid_souvenir"));
    public static final RegistrySupplier<EntityType<RebellionProxyEntity>> REBELLION_PROXY_ENTITY =
            ENTITIES.register(
                    "rebellion_proxy",
                    () ->
                            EntityType.Builder.<RebellionProxyEntity>create(
                                            RebellionProxyEntity::new, SpawnGroup.MISC)
                                    // 碰撞箱覆盖玩家身体，保证宠物弹道先命中代理而非玩家
                                    .setDimensions(0.9F, 1.4F)
                                    .build("rebellion_proxy"));

    // 女仆休息座位（不可见，女仆骑乘以播放坐姿动画）
    public static final RegistrySupplier<EntityType<RestSeatEntity>> REST_SEAT_ENTITY =
            ENTITIES.register(
                    "rest_seat",
                    () ->
                            EntityType.Builder.<RestSeatEntity>create(
                                            RestSeatEntity::new, SpawnGroup.MISC)
                                    .setDimensions(0.25F, 0.25F)
                                    .build("rest_seat"));

    // アイテムグループ
    public static final RegistrySupplier<ItemGroup> ITEM_GROUP =
            ITEM_GROUPS.register(
                    "common",
                    () ->
                            CreativeTabRegistry.create(
                                    Text.translatable("itemGroup.littlemaidmobresurgence.common"),
                                    Items.CAKE::getDefaultStack));

    // ブロック
    public static final RegistrySupplier<SalaryBoxBlock> SALARY_BOX_BLOCK =
            BLOCKS.register(
                    "salary_box",
                    () ->
                            new SalaryBoxBlock(
                                    AbstractBlock.Settings.create()
                                            .mapColor(MapColor.OAK_TAN)
                                            .instrument(Instrument.BASS)
                                            .strength(2.5f)
                                            .sounds(BlockSoundGroup.WOOD)
                                            .burnable()));

    // アイテム
    public static final RegistrySupplier<Item> LITTLE_MAID_SPAWN_EGG_ITEM =
            ITEMS.register("little_maid_spawn_egg", LittleMaidSpawnEggItem::new);
    public static final RegistrySupplier<Item> MAID_STICK_ITEM =
            ITEMS.register("maid_stick", MaidStickItem::new);

    // 女仆纪念品（死亡掉落，手持右键方块复活女仆）
    public static final RegistrySupplier<Item> MAID_SOUVENIR_ITEM =
            ITEMS.register("maid_souvenir", MaidSouvenirItem::new);

    // 女仆捕捉蛋（右键收纳/放出女仆）
    public static final RegistrySupplier<Item> MAID_CARRY_ITEM =
            ITEMS.register("maid_carry", MaidCarryItem::new);

    // 背包扩容道具（5 级）
    public static final RegistrySupplier<Item> BACKPACK_UPGRADE_COPPER =
            ITEMS.register(
                    "backpack_upgrade_copper",
                    () -> new BackpackUpgradeItem(BackpackUpgradeItem.UpgradeLevel.COPPER));
    public static final RegistrySupplier<Item> BACKPACK_UPGRADE_IRON =
            ITEMS.register(
                    "backpack_upgrade_iron",
                    () -> new BackpackUpgradeItem(BackpackUpgradeItem.UpgradeLevel.IRON));
    public static final RegistrySupplier<Item> BACKPACK_UPGRADE_GOLD =
            ITEMS.register(
                    "backpack_upgrade_gold",
                    () -> new BackpackUpgradeItem(BackpackUpgradeItem.UpgradeLevel.GOLD));
    public static final RegistrySupplier<Item> BACKPACK_UPGRADE_DIAMOND =
            ITEMS.register(
                    "backpack_upgrade_diamond",
                    () -> new BackpackUpgradeItem(BackpackUpgradeItem.UpgradeLevel.DIAMOND));
    public static final RegistrySupplier<Item> BACKPACK_UPGRADE_NETHERITE =
            ITEMS.register(
                    "backpack_upgrade_netherite",
                    () -> new BackpackUpgradeItem(BackpackUpgradeItem.UpgradeLevel.NETHERITE));

    // ブロックアイテム
    public static final RegistrySupplier<Item> SALARY_BOX_BLOCK_ITEM =
            ITEMS.register(
                    "salary_box",
                    () ->
                            new BlockItem(
                                    SALARY_BOX_BLOCK.get(),
                                    new Item.Settings().arch$tab(ITEM_GROUP)));

    // スクリーンハンドラ
    public static final RegistrySupplier<ScreenHandlerType<LittleMaidScreenHandler>>
            LITTLE_MAID_SCREEN_HANDLER =
                    SCREEN_HANDLERS.register(
                            "little_maid",
                            () -> MenuRegistry.ofExtended(LittleMaidScreenHandler::new));

    // 饰品（Curios）スクリーンハンドラ
    public static final RegistrySupplier<ScreenHandlerType<CuriosScreenHandler>>
            CURIOS_SCREEN_HANDLER =
                    SCREEN_HANDLERS.register(
                            "curios",
                            () -> MenuRegistry.ofExtended(CuriosScreenHandler::new));

    // 扩容背包スクリーンハンドラ
    public static final RegistrySupplier<ScreenHandlerType<BackpackScreenHandler>>
            BACKPACK_SCREEN_HANDLER =
                    SCREEN_HANDLERS.register(
                            "backpack",
                            () -> MenuRegistry.ofExtended(BackpackScreenHandler::new));

    // ブロックエンティティ
    public static final RegistrySupplier<BlockEntityType<SalaryBoxBlockEntity>>
            SALARY_BOX_BLOCK_ENTITY =
                    BLOCK_ENTITIES.register(
                            "salary_box",
                            () ->
                                    BlockEntityType.Builder.create(
                                                    SalaryBoxBlockEntity::new,
                                                    SALARY_BOX_BLOCK.get())
                                            .build(
                                                    Util.getChoiceType(
                                                            TypeReferences.BLOCK_ENTITY,
                                                            "salary_box")));


}
