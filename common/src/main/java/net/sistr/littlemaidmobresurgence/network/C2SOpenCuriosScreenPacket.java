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

/** クライアントからサーバーへ女仆の饰品界面を開くパケット（可指定页，用于饰品栏分页切换）。 */
public class C2SOpenCuriosScreenPacket {
    public static final Identifier ID = new Identifier(LMMRMod.MODID, "open_curios_screen");

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
        context.queue(() -> applyOpenCuriosScreenServer(context.getPlayer(), id, page));
    }

    private static void applyOpenCuriosScreenServer(PlayerEntity player, int id, int page) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // ご主人がいて、送信元のプレイヤーがご主人なら
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            maid.openCuriosScreen(player, page);
        }
    }
}
