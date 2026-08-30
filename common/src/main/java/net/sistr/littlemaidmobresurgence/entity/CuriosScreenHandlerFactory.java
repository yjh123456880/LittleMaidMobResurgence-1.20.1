package net.sistr.littlemaidmobresurgence.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * 女仆饰品界面工厂：向数据包写入女仆实体 ID、请求页号与服务端完整 Curios 槽位结构，构造 {@link
 * CuriosScreenHandler}。
 *
 * <p>开屏数据包格式：varInt 实体ID + varInt 页号 + varInt 结构条目数 + (String 类型, varInt 数量)*N。
 * 客户端用完整结构与页号以同一公式分页镜像构建（第 0 页 1 扩容槽+36 饰品槽，后续页整页 36），
 * 两侧每页槽位数量与顺序严格一致。
 *
 * <p><b>关键时序</b>：Forge 的 {@code NetworkHooks.openScreen} 是<b>先调用 {@link #saveExtraData}
 * 再调用 {@link #createMenu}</b>。因此不能依赖 createMenu 的副作用（构造容器时才得到结构）来写包——
 * 必须在调用 openExtendedMenu 之前完成槽位同步，把完整结构预先传入本工厂
 * （见 {@code LittleMaidEntity#openCuriosScreen}），saveExtraData 与 createMenu 共用同一份结构，
 * 保证客户端与服务端每页槽位数量严格一致（否则客户端收空结构建 37 槽、服务端 65 槽，
 * 容器内容同步会 IndexOutOfBounds）。
 */
public class CuriosScreenHandlerFactory implements ExtendedMenuProvider {
    private final LittleMaidEntity maid;
    private final int page;

    /** 打开前预同步的完整 Curios 槽位结构（类型->数量，按槽位顺序）。 */
    @Nullable private final Map<String, Integer> structure;

    public CuriosScreenHandlerFactory(
            LittleMaidEntity maid, int page, @Nullable Map<String, Integer> structure) {
        this.maid = maid;
        this.page = page;
        this.structure = structure;
    }

    @Override
    public void saveExtraData(PacketByteBuf buf) {
        buf.writeVarInt(maid.getId());
        buf.writeVarInt(page);
        // saveExtraData 先于 createMenu 执行（Forge openScreen 时序），必须使用预同步结构；
        // structure 为 null（异常路径）时写空结构，客户端与服务端仍按同一结构构建不会错位
        Map<String, Integer> s = structure != null ? structure : Map.of();
        buf.writeVarInt(s.size());
        for (Map.Entry<String, Integer> entry : s.entrySet()) {
            buf.writeString(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        // 服务端构造：有预同步结构时直接使用（跳过二次同步），否则构造内部自行同步
        return new CuriosScreenHandler(syncId, inv, maid.getId(), page, structure);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("gui.littlemaidmobresurgence.curios.title");
    }
}
