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

/** クライアントからサーバーへBloodSuckを設定するパケット */
public class C2SSetBloodSuckPacket {
    public static final Identifier ID = new Identifier(LMMRMod.MODID, "set_blood_suck");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, boolean isBloodSuck) {
        PacketByteBuf buf = createC2SPacket(entity, isBloodSuck);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity, boolean isBloodSuck) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeBoolean(isBloodSuck);
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        boolean isBloodSuck = buf.readBoolean();
        context.queue(() -> applyBloodSuckServer(context.getPlayer(), id, isBloodSuck));
    }

    private static void applyBloodSuckServer(PlayerEntity player, int id, boolean isBloodSuck) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // ご主人がいて、送信元のプレイヤーがご主人なら
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            maid.setBloodSuck(isBloodSuck);
        }
    }
}
