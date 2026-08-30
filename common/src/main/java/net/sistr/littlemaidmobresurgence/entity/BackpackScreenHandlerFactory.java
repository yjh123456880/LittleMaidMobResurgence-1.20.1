package net.sistr.littlemaidmobresurgence.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

/**
 * 女仆扩容背包界面工厂：写实体 ID、页号与**服务端计算的扩容格数**，构造 {@link
 * BackpackScreenHandler}。
 *
 * <p>扩容格数必须经数据包传递：客户端的扩容道具槽（backpackUpgradeSlot）不经过
 * DataTracker 同步，重进世界后为空——若客户端自行计算会得到 0 格（表现为首次打开
 * 扩容背包不显示格子，须先开一次饰品界面让原版容器同步写入道具后才正常）。
 */
public class BackpackScreenHandlerFactory implements ExtendedMenuProvider {
    private final LittleMaidEntity maid;
    private final int page;

    public BackpackScreenHandlerFactory(LittleMaidEntity maid) {
        this(maid, 0);
    }

    public BackpackScreenHandlerFactory(LittleMaidEntity maid, int page) {
        this.maid = maid;
        this.page = page;
    }

    @Override
    public void saveExtraData(PacketByteBuf buf) {
        buf.writeVarInt(maid.getId());
        buf.writeVarInt(page);
        // 服务端实际生效的扩容格数（与 BackpackScreenHandler 的 clamp 一致，上限 90）
        buf.writeVarInt(Math.min(maid.getBackpackExtraSlots(), 90));
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new BackpackScreenHandler(syncId, inv, maid.getId(), page);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("gui.littlemaidmobresurgence.backpack.title");
    }
}
