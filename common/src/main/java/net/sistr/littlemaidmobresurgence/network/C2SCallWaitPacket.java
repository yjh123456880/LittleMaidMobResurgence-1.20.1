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
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;

public class C2SCallWaitPacket {
    public static final Identifier ID = new Identifier(LMMRMod.MODID, "call_wait");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, State state) {
        PacketByteBuf buf = createC2SPacket(entity, state);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity, State state) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeEnumConstant(state);
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        State state = buf.readEnumConstant(State.class);
        context.queue(() -> applyMovingStateServer(context.getPlayer(), id, state));
    }

    private static void applyMovingStateServer(PlayerEntity player, int id, State state) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid) || !TameableUtil.isTameOwner(maid, player)) {
            return;
        }
        // [zh] 反叛期间禁用管理界面的召唤/待命操作（反叛=全交互禁用）
        // [en] Calling/waiting is disabled while the maid is rebellious (rebellion disables all interactions).
        // [ja] 反乱中は管理画面からの呼び寄せ・待機操作を拒否します。
        if (maid.isRebellious()) {
            return;
        }
        if (maid.isStrike()) {
            return;
        }
        if (state == State.WAIT) {
            TameableUtil.setWait(maid, true);
        } else {
            TameableUtil.setWait(maid, false);
            maid.setMovingMode(MovingMode.ESCORT);
        }
    }

    public enum State {
        WAIT,
        CALL
    }
}
