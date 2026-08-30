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

/** C2Sで自動食事の閾値をセットするパケット（80/60/40/0=切）。 */
public class C2SSetAutoEatPacket {
    public static final Identifier ID = new Identifier(LMMRMod.MODID, "set_auto_eat");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, int threshold) {
        NetworkManager.sendToServer(ID, createC2SPacket(entity, threshold));
    }

    public static PacketByteBuf createC2SPacket(Entity entity, int threshold) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeVarInt(threshold);
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        int threshold = buf.readVarInt();
        context.queue(() -> applyServer(context.getPlayer(), id, threshold));
    }

    private static void applyServer(PlayerEntity player, int id, int threshold) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)
                || !TameableUtil.isTameOwner(maid, player)) {
            return;
        }
        if (maid.isStrike()) {
            return;
        }
        maid.setAutoEatThreshold(threshold);
    }
}