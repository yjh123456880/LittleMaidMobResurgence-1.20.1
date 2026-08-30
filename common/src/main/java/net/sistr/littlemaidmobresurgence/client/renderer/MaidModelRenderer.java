package net.sistr.littlemaidmobresurgence.client.renderer;

import static net.sistr.littlemaidmodelloader.maidmodel.IModelCaps.*;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelArmorLayer;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelHeldItemLayer;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelLightLayer;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.maidmodel.ModelMultiBase;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMMatrixStack;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import org.joml.Matrix4f;

/** メイド用レンダラ */
@Environment(EnvType.CLIENT)
public class MaidModelRenderer
        extends MobEntityRenderer<LittleMaidEntity, LMMultiModel<LittleMaidEntity>> {
    private static final Identifier NULL_TEXTURE = new Identifier(LMMRMod.MODID, "null");
    /** 气泡显示前的字形预热帧数（隐藏期，用真实绘制烘焙字形/上传图集）。 */
    private static final int SPEECH_WARMUP_FRAMES = 6;

    /**
     * 卡通气泡填充层。深度语义：{@code ALWAYS_DEPTH_TEST}（气泡永远通过深度测试，与原版名标签/
     * SEE_THROUGH 文字一致，隔墙可见、不受方块遮挡）+ {@code ALL_MASK}（写入深度）。
     *
     * <p>关键：气泡框和 SEE_THROUGH 文字必须采用同一条深度策略，两者才总是"一起出现、一起被遮挡"，
     * 否则方块挡住气泡时会出现"只剩黑字、没有框"的漂浮文本，以及随遮挡边界变化的框/文字闪烁。
     *
     * <p>写深度（ALL_MASK）用于遮挡其后渲染的水面（半透明地形）、云层等背景——这些元素深度测试
     * 失败，无法透过气泡显示，避免"背景穿透"。这与旧配置 ALWAYS+COLOR_MASK（只写颜色不写深度）
     * 不同，后者因不写深度导致水/云穿过气泡。
     */
    private static final RenderLayer BUBBLE_FILL_LAYER =
            RenderLayer.of(
                    "lmmr_bubble_fill",
                    VertexFormats.POSITION_COLOR,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(RenderPhase.GUI_PROGRAM)
                            .texture(RenderPhase.NO_TEXTURE)
                            .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                            .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                            .writeMaskState(RenderPhase.ALL_MASK)
                            .cull(RenderPhase.DISABLE_CULLING)
                            .build(false));

    public MaidModelRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new LMMultiModel<>(), 0.5F);
        // エラー吐くので<>消した(ゴリ押し)
        this.addFeature(new MultiModelArmorLayer(this));
        this.addFeature(new MultiModelHeldItemLayer(this));
        this.addFeature(new MultiModelLightLayer(this));
        this.addFeature(new LMHeadFeatureRenderer<>(this, ctx.getModelLoader()));
        // セリフバブルは render() 内でアニメーション非依存の行列により描画する
    }

    @Override
    protected void setupTransforms(
            LittleMaidEntity entity,
            MatrixStack matrices,
            float animationProgress,
            float bodyYaw,
            float tickDelta) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(
                        model ->
                                model.setupTransform(
                                        entity.getCaps(),
                                        new MMMatrixStack(matrices),
                                        animationProgress,
                                        bodyYaw,
                                        tickDelta));
    }

    @Override
    protected void scale(LittleMaidEntity entity, MatrixStack matrices, float amount) {
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelMultiBase)
                .map(
                        model ->
                                ((Number) ((ModelMultiBase) model).getCapsValue(caps_ScaleFactor))
                                        .floatValue())
                .ifPresent(scale -> matrices.scale(scale, scale, scale));
    }

    @Override
    public void render(
            LittleMaidEntity livingEntity,
            float entityYaw,
            float partialTicks,
            MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider,
            int light) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        profiler.push("littlemaidmodelloader:mm");
        livingEntity
                .getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelMultiBase)
                .ifPresent(model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            livingEntity
                    .getModel(IHasMultiModel.Layer.INNER, part)
                    .filter(model -> model instanceof ModelMultiBase)
                    .ifPresent(
                            model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
            livingEntity
                    .getModel(IHasMultiModel.Layer.OUTER, part)
                    .filter(model -> model instanceof ModelMultiBase)
                    .ifPresent(
                            model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
        }
        // 休息坐姿：把模型整体下压到贴地（进度越大坐得越低），让"席地而坐"不悬空
        float restSitProgress = livingEntity.getRestSitProgress();
        if (restSitProgress > 0.01F) {
            matrixStack.translate(
                    0.0F, -LMMRMod.getConfig().client.restSitDrop * restSitProgress, 0.0F);
        }
        super.render(
                livingEntity, entityYaw, partialTicks, matrixStack, vertexConsumerProvider, light);
        profiler.pop();
        // セリフバブルはモデル描画「後」に描く（バニラのネームタグと同じパイプライン位置。
        // 先に描くと深度・描画順が不安定になり、視点によってテキストがちらつく原因になる）
        renderSpeechBubble(livingEntity, matrixStack, vertexConsumerProvider);
    }

    @Override
    protected void renderLabelIfPresent(
            LittleMaidEntity entity,
            Text text,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light) {
        // 说话（气泡展示中）时隐藏原版名札，避免气泡文本与名札重叠显示
        if (entity.getSpeechTimer() > 0) {
            return;
        }
        super.renderLabelIfPresent(entity, text, matrices, vertexConsumers, light);
    }

    /**
     * 頭上に固定されたセリフバブルを描画する。长方形文本框：奶油底+描边，文字写在框中。
     *
     * <p>设计与技术参考自「车万女仆（Touhou Little Maid）」的 ChatBubbleRenderer/EntityGraphics：
     *
     * <ul>
     *   <li>在女仆名标签所用管线位置绘制（身体上方 → cameraOrientation 面向玩家 → 缩放到 -0.025），
     *       与原版标签/气泡一致。
     *   <li>文字直接渲染在原版世界文字管线（textRenderer.draw）中，采用不透明气泡底色 + 近乎纯黑的
     *       高对比文字（颜色沿用车万女仆 0x000000 的对比思路），保证在奶白底上清晰可读、字符不缺失。
     *   <li>沿用 SEE_THROUGH 文字层（原版名标签同款），避免模型/方块深度翻转导致的视角闪烁。
     * </ul>
     */
    private void renderSpeechBubble(
            LittleMaidEntity entity,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers) {
        if (entity.getSpeechTimer() <= 0) {
            return;
        }
        var speech = entity.getSpeech();
        if (speech.isEmpty()) {
            return;
        }
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        // 强制全黑：剥离台词字符串内可能存在的 legacy §颜色/样式码，避免个别字符被改成
        // 与奶白气泡底相近的颜色（例如 §e/§6 的金黄）导致“看起来像缺字”。
        String str = stripLegacyFormatting(Text.translatable(speech.get()).getString());
        // 字形预热：getWidth 只取字形度量，并不会真正把字形烘焙进图集（烘焙发生在 draw 阶段）。
        // 因此在气泡显示前的隐藏期，用真实 draw 到临时 buffer（不提交）强制烘焙字形，
        // 并跨多帧让新增字形图集上传完成，从显示首帧起文本就完整，避免偶发缺字。
        if (entity.getSpeechTimer()
                > LMMRMod.getConfig().speech.speechDuration - SPEECH_WARMUP_FRAMES) {
            prewarmGlyphs(textRenderer, str);
            return;
        }
        // 过长的文字自动换行，避免一行撑爆/重叠
        List<String> lines = wrapText(textRenderer, str, 116F);
        if (lines.isEmpty()) {
            return;
        }
        float lineHeight = textRenderer.fontHeight;
        float textWidth = 0F;
        for (String line : lines) {
            textWidth = Math.max(textWidth, textRenderer.getWidth(line));
        }

        matrices.push();
        // 从实体脚底出发，在实体顶部（getHeight）再上移半格（0.5）作为气泡锚点，
        // 与原版名标签一致，使气泡框严格居于女仆头顶半格处。
        matrices.translate(0.0F, entity.getHeight() + 0.5F, 0.0F);
        // 常にプレイヤー方向を向く（原版ネームタグと同じ）
        matrices.multiply(
                MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // —— 长方形文本框 —— 奶油白填充 + 深棕描边
        int fill = 0xFFFFFDE7;
        int border = 0xFF7A6A4F;
        float pad = 7.0F;
        float totalText = lineHeight * lines.size();
        float boxW = textWidth + pad * 2;
        float boxH = totalText + pad * 2;
        float x1 = -boxW / 2;
        float x2 = boxW / 2;
        float yBottom = 0.0F; // 方框底边（放锚点上方，框向上延伸）
        float yTop = -boxH;

        VertexConsumer fillBuf = vertexConsumers.getBuffer(BUBBLE_FILL_LAYER);
        // 1) 描边（外扩 1.5px）
        quad(fillBuf, matrix4f, x1 - 1.5f, yTop - 1.5f, x2 + 1.5f, yBottom + 1.5f, border);
        // 2) 填充
        quad(fillBuf, matrix4f, x1, yTop, x2, yBottom, fill);

        // —— 文字 —— 原版名牌同款**双遍绘制**：SEE_THROUGH（隔墙可见、不受深度遮挡）
        // + NORMAL（常规深度管线）。单遍 SEE_THROUGH 在部分场景下会出现字符缺失
        // （CJK 字形惰性烘焙首帧空白/批处理交错），双遍同帧先后绘制互为兜底，
        // 且第一遍即完成字形烘焙，第二遍立即可用
        float lineY = yTop + pad;
        for (String line : lines) {
            float lineW = textRenderer.getWidth(line);
            textRenderer.draw(
                    line,
                    -lineW / 2.0F,
                    lineY,
                    0xFF000000,
                    false,
                    matrix4f,
                    vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0,
                    0xF000F0);
            textRenderer.draw(
                    line,
                    -lineW / 2.0F,
                    lineY,
                    0xFF000000,
                    false,
                    matrix4f,
                    vertexConsumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    0xF000F0);
            lineY += lineHeight;
        }
        matrices.pop();
    }

    /** 剥离 Minecraft legacy §格式化码（含 §x 六位十六进制颜色码），保证文本以纯色绘制。 */
    private static String stripLegacyFormatting(String text) {
        if (text == null || text.indexOf('\u00a7') < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                if (code == 'x' && i + 7 < text.length()) {
                    // §x + 6 位十六进制
                    i += 7;
                } else {
                    // § + 单字符颜色/样式/重置码
                    i += 1;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 将文字按最大像素宽度折行（不按单词，逐字符换行以适应中日英文）。 */
    private static List<String> wrapText(TextRenderer textRenderer, String text, float maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                result.add(line.toString());
                line.setLength(0);
                continue;
            }
            if (line.length() > 0 && textRenderer.getWidth(line.toString() + c) > maxWidth) {
                result.add(line.toString());
                line.setLength(0);
            }
            line.append(c);
        }
        if (line.length() > 0) {
            result.add(line.toString());
        }
        return result;
    }

    /**
     * 字形预热：把整条台词真实 draw 到一个【不提交】的临时 buffer。
     * draw 阶段会触发共享字形图集的烘焙与上传（getWidth 不会做这一步），
     * 从而保证气泡真正显示时字形已就绪，避免首帧/偶发缺字。
     */
    private static void prewarmGlyphs(TextRenderer textRenderer, String text) {
        VertexConsumerProvider.Immediate temp =
                VertexConsumerProvider.immediate(new BufferBuilder(256));
        textRenderer.draw(
                text,
                0.0F,
                0.0F,
                0xFFFFFFFF,
                false,
                new Matrix4f(),
                temp,
                TextRenderer.TextLayerType.NORMAL,
                0,
                0xF000F0);
        // 不调用 temp.draw()：临时 buffer 直接丢弃，只利用 draw 阶段的字形烘焙
    }

    private static void quad(
            VertexConsumer buffer, Matrix4f m, float x1, float y1, float x2, float y2, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        buffer.vertex(m, x1, y1, 0).color(r, g, b, a).next();
        buffer.vertex(m, x1, y2, 0).color(r, g, b, a).next();
        buffer.vertex(m, x2, y2, 0).color(r, g, b, a).next();
        buffer.vertex(m, x2, y1, 0).color(r, g, b, a).next();
    }

    public void syncCaps(LittleMaidEntity entity, ModelMultiBase model, float partialTicks) {
        float swingProgress = entity.getHandSwingProgress(partialTicks);
        float right = 0;
        float left = 0;
        if (entity.preferredHand == Hand.MAIN_HAND) {
            if (entity.getMainArm() == Arm.RIGHT) {
                right = swingProgress;
            } else {
                left = swingProgress;
            }
        } else {
            if (entity.getMainArm() != Arm.RIGHT) {
                right = swingProgress;
            } else {
                left = swingProgress;
            }
        }
        model.setCapsValue(caps_onGround, right, left);
        // 坐姿势：骑乘真实载具，或处于休息坐姿（由 REST_SIT_PROGRESS>0 触发，含过渡过程）
        model.setCapsValue(
                caps_isRiding,
                entity.hasVehicle() || entity.getRestSitProgress() > 0.01F);
        model.setCapsValue(caps_isSneak, entity.isSneaking());
        model.setCapsValue(caps_isChild, entity.isBaby());
        model.setCapsValue(caps_heldItemLeft, 0F);
        model.setCapsValue(caps_heldItemRight, 0F);
        // 进食中：副手食物抬到嘴边（caps_heldItem 值放大→手臂上抬约60°，带轻微咬合起伏），
        // 食物本体由手持物品渲染层跟随手臂，自然出现在嘴边（车万女仆同款思路）
        if (entity.isEating()) {
            float v = 3.5F + MathHelper.sin(entity.age * 0.5F) * 0.3F;
            if (entity.getMainArm() == Arm.RIGHT) {
                model.setCapsValue(caps_heldItemLeft, v);
            } else {
                model.setCapsValue(caps_heldItemRight, v);
            }
        } else if (entity.isSugarConsuming()) {
            // 持糖消耗：副手微抬展示手中的糖（不做咀嚼动画）
            float v = 1.5F;
            if (entity.getMainArm() == Arm.RIGHT) {
                model.setCapsValue(caps_heldItemLeft, v);
            } else {
                model.setCapsValue(caps_heldItemRight, v);
            }
        }
        model.setCapsValue(caps_aimedBow, false);
        model.setCapsValue(caps_entityIdFactor, 0F);
        model.setCapsValue(caps_ticksExisted, entity.age);

        model.setCapsValue(caps_aimedBow, entity.isAimingBow());
        model.setCapsValue(
                caps_isWait,
                TameableUtil.isWait(entity)
                        && (LMMRMod.getConfig().client.enableWaitPoseOnMoving
                                || entity.getVelocity().lengthSquared() < 0.01));
        model.setCapsValue(caps_isContract, entity.isContract());
        model.setCapsValue(caps_isBloodsuck, entity.isBloodSuck());
        model.setCapsValue(
                caps_isClock,
                entity.getMainHandStack().getItem() == Items.CLOCK
                        || entity.getOffHandStack().getItem() == Items.CLOCK);
    }

    @Override
    public Identifier getTexture(LittleMaidEntity entity) {
        return entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
    }
}
