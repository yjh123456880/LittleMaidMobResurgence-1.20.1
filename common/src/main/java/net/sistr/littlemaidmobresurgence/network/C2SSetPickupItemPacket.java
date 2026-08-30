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

/** 客户端向服务器切换女仆的捡取掉落物开关。 */
public class C2SSetPickupItemPacket {
    public static final Identifier ID = new Identifier(LMMRMod.MODID, "set_pickup_item");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, boolean pickup) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeBoolean(pickup);
        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        boolean pickup = buf.readBoolean();
        context.queue(() -> applyPickupItemServer(context.getPlayer(), id, pickup));
    }

    private static void applyPickupItemServer(PlayerEntity player, int id, boolean pickup) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // 仅主人可切换
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            maid.setPickupItem(pickup);
        }
    }
}
