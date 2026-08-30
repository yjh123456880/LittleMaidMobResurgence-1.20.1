package net.sistr.littlemaidmobresurgence.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.RebellionProxyEntity;

/** 反叛伤害代理实体的空渲染器（实体恒为隐身，不绘制任何内容）。 */
@Environment(EnvType.CLIENT)
public class RebellionProxyRenderer extends EntityRenderer<RebellionProxyEntity> {
    private static final Identifier TEXTURE =
            new Identifier(LMMRMod.MODID, "textures/entity/rebellion_proxy/rebellion_proxy.png");

    public RebellionProxyRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(
            RebellionProxyEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light) {
        // 隐身体：不渲染任何内容
    }

    @Override
    public Identifier getTexture(RebellionProxyEntity entity) {
        return TEXTURE;
    }
}
