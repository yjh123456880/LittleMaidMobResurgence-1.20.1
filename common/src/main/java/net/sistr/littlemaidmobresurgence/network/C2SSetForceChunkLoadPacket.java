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

/** 客户端向服务器切换女仆的"强加载"开关（强制加载所在区块，防止被清理/卸载）。 */
public class C2SSetForceChunkLoadPacket {
    public static final Identifier ID =
            new Identifier(LMMRMod.MODID, "set_force_chunk_load");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, boolean enabled) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeBoolean(enabled);
        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        boolean enabled = buf.readBoolean();
        context.queue(() -> applyForceChunkLoadServer(context.getPlayer(), id, enabled));
    }

    private static void applyForceChunkLoadServer(PlayerEntity player, int id, boolean enabled) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // 仅主人可切换
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            maid.setForceChunkLoad(enabled);
        }
    }
}
