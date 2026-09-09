package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

/**
 * [zh] 为车万自带模型界面创建客户端预览用假女仆：复制本模组女仆的位置/朝向/坐姿/装备与当前 TLM 模型。
 * [en] Creates a client-side preview maid for TLM's own model GUI, copying pose/equipment and current TLM model.
 * [ja] TLM標準モデル画面用のクライアント側プレビューメイドを作成します。
 */
@OnlyIn(Dist.CLIENT)
public final class TlmPreviewMaidFactory {
    private static final String DEFAULT_MODEL = "touhou_little_maid:hakurei_reimu";

    private TlmPreviewMaidFactory() {}

    @Nullable
    public static EntityMaid create(LittleMaidEntity realMaid) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return null;
        }
        EntityMaid preview = EntityMaid.TYPE.create(world);
        if (preview == null) {
            preview = new EntityMaid(world);
        }
        EntityCacheUtil.clearMaidDataResidue(preview, true);
        String modelId = realMaid.getTlmModelId();
        preview.setModelId(modelId == null || modelId.isEmpty() ? DEFAULT_MODEL : modelId);
        preview.setIsYsmModel(false);
        preview.refreshPositionAndAngles(
                realMaid.getX(),
                realMaid.getY(),
                realMaid.getZ(),
                realMaid.getYaw(),
                realMaid.getPitch());
        preview.headYaw = realMaid.headYaw;
        preview.bodyYaw = realMaid.bodyYaw;
        preview.setOnGround(true);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            preview.equipStack(slot, realMaid.getEquippedStack(slot).copy());
        }
        if (realMaid.getCustomName() != null) {
            preview.setCustomName(realMaid.getCustomName().copy());
        }
        return preview;
    }
}
