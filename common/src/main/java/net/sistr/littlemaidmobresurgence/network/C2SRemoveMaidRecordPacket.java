package net.sistr.littlemaidmobresurgence.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.util.MaidManager;

/**
 * [zh] 女仆管理界面：客户端请求删除一条女仆记录（仅本人管理列表，不影响世界实体/纪念品）。
 * [en] Maid manager screen: client requests removal of one maid record (only the owner's list;
 *     world entities and souvenirs are unaffected).
 * [ja] メイド管理画面：メイド記録の削除をサーバーへ要求します（本人のリストのみ。実体・記念品には影響なし）。
 */
public class C2SRemoveMaidRecordPacket {
    public static final Identifier ID =
            new Identifier(LMMRMod.MODID, "remove_maid_record");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(UUID maidUuid) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(maidUuid);
        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(
            PacketByteBuf buf, NetworkManager.PacketContext context) {
        UUID maidUuid = buf.readUuid();
        context.queue(() -> applyRemoveServer(context.getPlayer(), maidUuid));
    }

    private static void applyRemoveServer(PlayerEntity player, UUID maidUuid) {
        // 管理记录绑定在玩家自身上（MaidManager mixin），天然只允许本人删除自己的记录
        if (player instanceof MaidManager manager) {
            manager.removeMaid(maidUuid);
        }
    }
}
