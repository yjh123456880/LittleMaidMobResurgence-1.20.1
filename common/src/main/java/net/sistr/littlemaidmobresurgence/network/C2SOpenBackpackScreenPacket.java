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

/** 客户端向服务器请求打开女仆扩容背包界面（可指定页）。 */
public class C2SOpenBackpackScreenPacket {
    public static final Identifier ID = new Identifier(LMMRMod.MODID, "open_backpack_screen");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity) {
        sendC2SPacket(entity, 0);
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, int page) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeVarInt(page);
        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        int page = buf.readVarInt();
        context.queue(() -> applyOpenBackpackScreenServer(context.getPlayer(), id, page));
    }

    private static void applyOpenBackpackScreenServer(PlayerEntity player, int id, int page) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // 仅主人可打开，且需已装备扩容道具
        if (TameableUtil.getTameOwnerUuid(maid)
                        .filter(uuid -> player.getUuid().equals(uuid))
                        .isPresent()
                && maid.getBackpackUpgradeLevel() > 0) {
            maid.openBackpackScreen(player, page);
        }
    }
}
