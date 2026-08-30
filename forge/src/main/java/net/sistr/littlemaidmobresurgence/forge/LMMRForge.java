package net.sistr.littlemaidmobresurgence.forge;

import dev.architectury.platform.forge.EventBuses;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.sistr.littlemaidmodelloader.LMMLMod;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelRenderer;
import net.sistr.littlemaidmodelloader.client.resource.LMPackProvider;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.client.renderer.MaidModelRenderer;
import net.sistr.littlemaidmobresurgence.client.renderer.RebellionProxyRenderer;
import net.sistr.littlemaidmobresurgence.client.render.MaidStickRenderHandler;
import net.sistr.littlemaidmobresurgence.config.LMMRConfig;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.setup.ClientSetup;
import net.sistr.littlemaidmobresurgence.setup.ModSetup;
import net.sistr.littlemaidmobresurgence.setup.Registration;
import org.jetbrains.annotations.Nullable;

@Mod(LMMRMod.MODID)
public class LMMRForge {

    public LMMRForge() {
        EventBuses.registerModEventBus(
                LMMRMod.MODID, FMLJavaModLoadingContext.get().getModEventBus());

        LMMRMod.init();
        // 合并自前置模组 LMML：无前置直接加载语音包/模型
        LMMLMod.init();

        // TACZ 枪械兼容：检测到 TACZ 时注册枪手模式
        if (dev.architectury.platform.Platform.isModLoaded("tacz")) {
            TaczGunAdapterImpl.init();
        }

        // Curios 饰品兼容：检测到 Curios 时注入饰品槽位实现
        if (dev.architectury.platform.Platform.isModLoaded("curios")) {
            CuriosCompatImpl.init();
        }

        // 三大魔法模组联动：检测到对应模组时注册魔法模式（诡厄巫法/铁魔法/新生魔艺）
        if (dev.architectury.platform.Platform.isModLoaded("goety")) {
            GoetyCompatImpl.init();
        }
        if (dev.architectury.platform.Platform.isModLoaded("irons_spellbooks")) {
            IronsSpellCompatImpl.init();
        }
        if (dev.architectury.platform.Platform.isModLoaded("ars_nouveau")) {
            ArsNouveauCompatImpl.init();
        }

        ModLoadingContext.get()
                .registerExtensionPoint(
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        () ->
                                new ConfigScreenHandler.ConfigScreenFactory(
                                        (client, parent) ->
                                                AutoConfig.getConfigScreen(LMMRConfig.class, parent)
                                                        .get()));

        // 兜底：女仆背包暴露为 Forge ITEM_HANDLER 能力（主路径见 MixinLivingEntityMaidInventory，
        // 因为 Forge 的 LivingEntity.getCapability 对 ITEM_HANDLER 会直接短路返回双手+盔甲处理器，
        // 不会查询本事件挂载的 provider）
        // （AttachCapabilitiesEvent 是泛型事件，必须用 addGenericListener 注册）
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, LMMRForge::attachCapabilities);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::modInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::spawnRestrictionInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::renderInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::packInit);
        // 按键注册必须发生在 RegisterKeyMappingsEvent 期间（早于 FMLClientSetupEvent），
        // 且 RegisterKeyMappingsEvent 为客户端专属类，须以环境判断隔离，专用服务器不加载
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get()
                    .getModEventBus()
                    .addListener(ClientKeyMappings::register);
        }
    }

    public void modInit(FMLCommonSetupEvent event) {
        ModSetup.init();
        // LMML 网络注册
        net.sistr.littlemaidmodelloader.setup.ModSetup.init();
    }

    public void spawnRestrictionInit(SpawnPlacementRegisterEvent event) {
        event.register(
                Registration.LITTLE_MAID_MOB.get(),
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) ->
                        LittleMaidEntity.isValidNaturalSpawn(world, pos),
                SpawnPlacementRegisterEvent.Operation.OR);
    }

    private static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof LittleMaidEntity maid)) {
            return;
        }
        event.addCapability(
                new Identifier(LMMRMod.MODID, "inventory"),
                new ICapabilityProvider() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> LazyOptional<T> getCapability(
                            Capability<T> capability, @Nullable Direction side) {
                        if (capability == ForgeCapabilities.ITEM_HANDLER) {
                            return (LazyOptional<T>)
                                    LazyOptional.of(
                                            () -> new MaidInventoryHandler(maid));
                        }
                        return LazyOptional.empty();
                    }
                });
    }

    public void clientInit(FMLClientSetupEvent event) {
        ClientSetup.init();
        // LMML 发光 shader 注册
        net.sistr.littlemaidmodelloader.setup.ClientSetup.init();
        // 女仆杖工作范围可视化
        MinecraftForge.EVENT_BUS.addListener(this::onRenderLevelStage);
    }

    /** 女仆杖工作范围环与箭头的世界渲染。 */
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        MaidStickRenderHandler.render(
                net.minecraft.client.MinecraftClient.getInstance(),
                event.getPoseStack(),
                event.getCamera());
    }

    // ClientSetupよりこちらの方が実行が早いため、ClientSetupからArchitecturyのメソッド登録しようとすると無視される
    public void renderInit(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
        event.registerEntityRenderer(
                Registration.REBELLION_PROXY_ENTITY.get(), RebellionProxyRenderer::new);
        // 女仆休息座位：不可见，使用空渲染器
        event.registerEntityRenderer(
                Registration.REST_SEAT_ENTITY.get(),
                net.minecraft.client.render.entity.EmptyEntityRenderer::new);
        // 女仆纪念品掉落物：使用原版物品实体渲染器
        event.registerEntityRenderer(
                Registration.MAID_SOUVENIR_ENTITY.get(),
                net.minecraft.client.render.entity.ItemEntityRenderer::new);
        // LMML 多模型实体渲染器
        event.registerEntityRenderer(
                net.sistr.littlemaidmodelloader.setup.Registration.MULTI_MODEL_ENTITY.get(),
                MultiModelRenderer::new);
        event.registerEntityRenderer(
                net.sistr.littlemaidmodelloader.setup.Registration.DUMMY_MODEL_ENTITY.get(),
                MultiModelRenderer::new);
    }

    public void packInit(AddPackFindersEvent event) {
        if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
            event.addRepositorySource(new LMPackProvider());
        }
    }
}
