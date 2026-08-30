package net.sistr.littlemaidmobresurgence.client.render;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.item.MaidStickItem;
import net.sistr.littlemaidmobresurgence.setup.Registration;

/**
 * 女仆杖工作范围可视化：仅当玩家手持已绑范围且同维度的女仆杖时，绘制一个橙色范围环，
 * 以及一个指向绑定方块位置的橙色箭头。无任何浮空文字。
 *
 * <p>环与箭头采用世界空间【四边形填充】绘制（而非 GL_LINES）：因为 {@code glLineWidth} 在
 * 绝大多数显卡上被锁定为 1px、无法加粗，所以用实心四边形条带实现明显更粗的范围环与箭头。
 */
public final class MaidStickRenderHandler {
    private static final int RING_SEGMENTS = 48;
    // 橙色
    private static final int COLOR_R = 255;
    private static final int COLOR_G = 128;
    private static final int COLOR_B = 0;
    private static final int COLOR_A = 255;
    // 世界空间颜色四边形填充层
    private static final RenderLayer RANGE_LAYER =
            RenderLayer.of(
                    "lmmr_range_ring",
                    VertexFormats.POSITION_COLOR,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(RenderPhase.GUI_PROGRAM)
                            .texture(RenderPhase.NO_TEXTURE)
                            .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                            .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                            .writeMaskState(RenderPhase.COLOR_MASK)
                            .cull(RenderPhase.DISABLE_CULLING)
                            .build(false));
    /** 范围环的径向半厚度（格），让环线明显变粗。 */
    private static final float RING_HALF_THICKNESS = 0.05F;
    /** 箭头杆长（格）：根部缩短到原先的三分之一后整体放大约 1.5 倍。 */
    private static final float ARROW_SHAFT_LEN = 0.5F;
    /** 箭头杆的半宽（格）。 */
    private static final float ARROW_HALF_WIDTH = 0.09F;
    /** 箭头头部的半宽/半高（格）。 */
    private static final float ARROW_HEAD_WIDTH = 0.33F;
    private static final float ARROW_HEAD_HEIGHT = 0.18F;
    /**
     * 诊断日志：每个不同状态只打印一次（避免刷屏），但不再受"第一帧状态"限制。
     * 这样玩家持已绑定女仆杖时必然能看到"开始绘制/绘制完成"日志，便于区分
     * "事件未触发 / 客户端无范围数据 / 已开始绘制"三种情况。
     */
    private static final Set<String> LOGGED_MESSAGES = new HashSet<>();

    private MaidStickRenderHandler() {}

    public static void render(MinecraftClient mc, MatrixStack poseStack, Camera camera) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        ItemStack stack = findStick(mc.player);
        if (stack == null) {
            logOnce("MaidStickRender: 未手持女仆杖");
            return;
        }
        BlockPos center = MaidStickItem.getWorkCenter(stack);
        Identifier dimension = MaidStickItem.getWorkDimension(stack);
        if (center == null
                || dimension == null
                || !dimension.equals(mc.world.getRegistryKey().getValue())) {
            logOnce(
                    "MaidStickRender: 女仆杖无有效范围数据 center="
                            + center
                            + " dim="
                            + dimension
                            + " 当前维度="
                            + mc.world.getRegistryKey().getValue());
            return;
        }
        logOnce(
                "MaidStickRender: 开始绘制范围环 center="
                        + center
                        + " dim="
                        + dimension
                        + " 半径="
                        + LMMRMod.getConfig().work.workRange);

        float radius = LMMRMod.getConfig().work.workRange + 0.1F;
        Vec3d cam = camera.getPos();

        // Forge 1.20.1 的 RenderLevelStageEvent 中 poseStack 只包含相机旋转、不包含相机平移，
        // 因此显式平移 -camera 后即可按【世界坐标】绘制（DebugRenderer 同款思路）。
        // 注意：不能 loadIdentity()——那会丢掉事件矩阵里已有的相机旋转，反而画错位置。
        poseStack.push();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        MatrixStack.Entry entry = poseStack.peek();

        // 四边形填充层：顶点写入实体缓冲区，交由原版在"方块实体之后"端批次统一冲刷
        VertexConsumer consumer =
                mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RANGE_LAYER);

        // 世界坐标：环心位于绑定方块中心，抬升 1.5 格（方块顶面之上 0.5 格）
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 1.5;
        double cz = center.getZ() + 0.5;

        // 橙色范围环：径向加厚的扁平圆环带（一圈四边形），比 1px GL 线明显更粗
        double outerRadius = radius + RING_HALF_THICKNESS;
        double innerRadius = radius - RING_HALF_THICKNESS;
        for (int i = 0; i < RING_SEGMENTS; ++i) {
            double a0 = Math.PI * 2 * i / RING_SEGMENTS;
            double a1 = Math.PI * 2 * (i + 1) / RING_SEGMENTS;
            quad(
                    consumer,
                    entry,
                    cx + Math.cos(a0) * outerRadius,
                    cy,
                    cz + Math.sin(a0) * outerRadius,
                    cx + Math.cos(a1) * outerRadius,
                    cy,
                    cz + Math.sin(a1) * outerRadius,
                    cx + Math.cos(a1) * innerRadius,
                    cy,
                    cz + Math.sin(a1) * innerRadius,
                    cx + Math.cos(a0) * innerRadius,
                    cy,
                    cz + Math.sin(a0) * innerRadius);
        }

        // 箭头：单面公告板（始终朝向玩家，忽略俯仰以保持竖直下指），根部缩短、整体放大
        double tipY = center.getY() + 1.5;
        double ax = cx;
        double az = cz;
        // 朝向玩家的水平方向（用于把单面转向玩家）
        double ddx = cam.x - cx;
        double ddz = cam.z - cz;
        double dlen = Math.sqrt(ddx * ddx + ddz * ddz);
        double fx = dlen < 1.0E-4 ? 0.0 : ddx / dlen;
        double fz = dlen < 1.0E-4 ? -1.0 : ddz / dlen;
        // 右向量（水平、垂直于视线方向，用于在单面内铺开宽度）
        double rx = fz;
        double rz = -fx;

        double hw = ARROW_HALF_WIDTH;
        double shl = ARROW_SHAFT_LEN;
        double headW = ARROW_HEAD_WIDTH;
        double headH = ARROW_HEAD_HEIGHT;

        // 杆：单面四边形，从尖端向上延伸 shl
        quad(
                consumer,
                entry,
                ax - rx * hw, tipY + shl, az - rz * hw,
                ax + rx * hw, tipY + shl, az + rz * hw,
                ax + rx * hw, tipY,       az + rz * hw,
                ax - rx * hw, tipY,       az - rz * hw);

        // 头：正对向下的倒三角（退化为四边形的三角面，尖端在 tipY - headH）
        quad(
                consumer,
                entry,
                ax - rx * headW, tipY,         az - rz * headW,
                ax + rx * headW, tipY,         az + rz * headW,
                ax,              tipY - headH, az,
                ax - rx * headW, tipY,         az - rz * headW);
        poseStack.pop();
        logOnce("MaidStickRender: 绘制完成");
    }

    private static void logOnce(String message) {
        if (LOGGED_MESSAGES.add(message)) {
            LMMRMod.LOGGER.info("[LMMR] " + message);
        }
    }

    private static void quad(
            VertexConsumer consumer,
            MatrixStack.Entry entry,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
            double x4,
            double y4,
            double z4) {
        vertex(consumer, entry, x1, y1, z1);
        vertex(consumer, entry, x2, y2, z2);
        vertex(consumer, entry, x3, y3, z3);
        vertex(consumer, entry, x4, y4, z4);
    }

    private static void vertex(
            VertexConsumer consumer, MatrixStack.Entry entry, double x, double y, double z) {
        consumer.vertex(entry.getPositionMatrix(), (float) x, (float) y, (float) z)
                .color(COLOR_R, COLOR_G, COLOR_B, COLOR_A)
                .next();
    }

    private static ItemStack findStick(net.minecraft.entity.player.PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (main.getItem() == Registration.MAID_STICK_ITEM.get()) {
            return main;
        }
        ItemStack off = player.getOffHandStack();
        return off.getItem() == Registration.MAID_STICK_ITEM.get() ? off : null;
    }
}
