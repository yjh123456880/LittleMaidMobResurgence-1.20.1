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

/** クライアントからサーバーへメイドさんのインベントリを開くパケット */
public class C2SOpenInventoryPacket {
    public static final Identifier ID = new Identifier(LMMRMod.MODID, "open_inventory");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity) {
        PacketByteBuf buf = createC2SPacket(entity);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        context.queue(() -> applyOpenInventoryServer(context.getPlayer(), id));
    }

    private static void applyOpenInventoryServer(PlayerEntity player, int id) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // ご主人がいて、送信元のプレイヤーがご主人なら
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            maid.openInventory(player);
        }
    }
}
