package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.animation.script.GlWrapper;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.resource.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmobresurgence.client.renderer.MaidModelRenderer;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 车万女仆联动渲染器：女仆设置了 TLM 模型 ID 时用 TLM 的 Bedrock/Gecko 引擎渲染（含动画），
 *     否则委托本模组 LMML 渲染器。结构参考「女仆扩展」的 VillagerMaidRenderer。
 * [en] TLM-linked renderer: renders via TLM's Bedrock/Gecko engines when a TLM model is chosen,
 *     otherwise delegates to the mod's LMML renderer. Architecture modeled on VillagerMaidRenderer.
 * [ja] TLM連携レンダラー。TLMモデル選択時はTLMのBedrock/Geckoエンジンで描画し、未選択時はLMML描画へ委譲します。
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings({"rawtypes", "unchecked"})
public class LittleMaidTlmRenderer
        extends MobEntityRenderer<LittleMaidEntity, BedrockModel<LittleMaidEntity>> {
    private static final String DEFAULT_MODEL_ID = "touhou_little_maid:hakurei_reimu";
    private static final Identifier DEFAULT_TEXTURE =
            new Identifier("touhou_little_maid", "textures/entity/empty.png");

    private final MaidModelRenderer lmmrDelegate;
    private final GeckoEntityMaidRenderer<LittleMaidEntity> geckoRenderer;
    private MaidModelInfo mainInfo;

    public LittleMaidTlmRenderer(EntityRendererFactory.Context context) {
        super(context, new BedrockModel<>(), 0.5F);
        this.lmmrDelegate = new MaidModelRenderer(context);
        this.geckoRenderer = new GeckoEntityMaidRenderer<>(context);
    }

    @Override
    public void render(
            LittleMaidEntity entity,
            float entityYaw,
            float partialTicks,
            MatrixStack poseStack,
            VertexConsumerProvider buffer,
            int packedLight) {
        // 未选择 TLM 模型：完全走本模组 LMML 渲染（模型/贴图/气泡/坐姿全部委托）
        String modelId = entity.getTlmModelId();
        if (modelId.isEmpty()
                || !CustomPackLoader.MAID_MODELS.containsInfo(modelId)) {
            lmmrDelegate.render(
                    entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        IMaid maid = IMaid.convert(entity);
        if (maid == null) {
            lmmrDelegate.render(
                    entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        if (!loadModel(modelId)) {
            lmmrDelegate.render(
                    entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        // gecko 模型走 TLM gecko 渲染管线
        if (mainInfo != null && mainInfo.isGeckoModel()) {
            GeckoMaidEntity<LittleMaidEntity> animatable =
                    geckoRenderer.getAnimatableEntity(entity);
            animatable.setMaidInfo(mainInfo);
            geckoRenderer.render(
                    entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        // Bedrock 模型：JS 动画 + MobRenderer 管线
        this.model.setAnimations(getAnimations(modelId));
        GlWrapper.setPoseStack(poseStack);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        GlWrapper.clearPoseStack();
    }

    /** 加载模型/信息/动画并广播 RenderMaidEvent 供其它模组覆盖。 */
    private boolean loadModel(String modelId) {
        Optional<BedrockModel<LittleMaidEntity>> modelOpt = getModel(modelId);
        Optional<MaidModelInfo> infoOpt = CustomPackLoader.MAID_MODELS.getInfo(modelId);
        String effectiveId = modelId;
        if (infoOpt.isEmpty()) {
            return false;
        }
        if (infoOpt.get().isGeckoModel()) {
            this.mainInfo = infoOpt.get();
            return true;
        }
        if (modelOpt.isEmpty()) {
            // 模型文件缺失：回退默认 TLM 模型，再失败则整体回退 LMML
            effectiveId = DEFAULT_MODEL_ID;
            modelOpt = getModel(DEFAULT_MODEL_ID);
            infoOpt = CustomPackLoader.MAID_MODELS.getInfo(DEFAULT_MODEL_ID);
            if (modelOpt.isEmpty() || infoOpt.isEmpty()) {
                return false;
            }
        }
        this.model = modelOpt.get();
        this.mainInfo = infoOpt.get();
        this.model.setAnimations(getAnimations(effectiveId));
        return true;
    }

    private Optional<BedrockModel<LittleMaidEntity>> getModel(String modelId) {
        return CustomPackLoader.MAID_MODELS.getModel(modelId)
                .map(m -> (BedrockModel<LittleMaidEntity>) (BedrockModel) m);
    }

    private List<Object> getAnimations(String modelId) {
        return CustomPackLoader.MAID_MODELS
                .getAnimation(modelId)
                .orElse(List.of());
    }

    @Override
    protected void scale(LittleMaidEntity maid, MatrixStack poseStack, float amount) {
        if (mainInfo != null) {
            float scale = mainInfo.getRenderEntityScale();
            poseStack.scale(scale, scale, scale);
        } else {
            super.scale(maid, poseStack, amount);
        }
    }

    @Override
    public Identifier getTexture(LittleMaidEntity maid) {
        if (mainInfo == null) {
            return DEFAULT_TEXTURE;
        }
        return mainInfo.getTexture();
    }
}
