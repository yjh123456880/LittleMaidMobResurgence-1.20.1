package net.sistr.littlemaidmobresurgence.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;

/**
 * [zh] 车万女仆联动：客户端把选中的 TLM 模型 ID 写入女仆（空串=恢复 LMML 模型）。
 * [en] TLM integration: the client stores the chosen TLM model ID on the maid (empty restores LMML models).
 * [ja] TLM連携：選択したTLMモデルIDをメイドに保存します（空文字でLMMLモデルに戻ります）。
 */
public class C2SSetTlmModelPacket {
    public static final Identifier ID =
            new Identifier(LMMRMod.MODID, "set_tlm_model");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, String modelId) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeString(modelId == null ? "" : modelId);
        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(
            PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        String modelId = buf.readString();
        context.queue(() -> applyServer(context.getPlayer(), id, modelId));
    }

    private static void applyServer(PlayerEntity player, int id, String modelId) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)
                || !TameableUtil.isTameOwner(maid, player)) {
            return;
        }
        // 反叛/罢工期间不允许修改外观
        if (maid.isRebellious() || maid.isStrike()) {
            return;
        }
        maid.setTlmModelId(modelId);
    }
}
