package net.sistr.littlemaidmobresurgence.client.screen.forge;

import dev.architectury.platform.Platform;
import java.util.List;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.sistr.littlemaidmobresurgence.client.screen.LittleMaidScreen;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.forge.TlmMaidScreenButtons;

/**
 * [zh] EPMaidScreenExtra 的 Forge 实现：仅在车万女仆存在时向主 GUI 左侧图标列追加车万模型按钮。
 * [en] Forge implementation of EPMaidScreenExtra: adds the TLM model button to the left icon column when TLM is present.
 * [ja] EPMaidScreenExtra のForge実装。TLM導入時のみ左アイコン列にTLMモデルボタンを追加します。
 */
public class EPMaidScreenExtraImpl {
    public static List<ButtonWidget> createExtraButtons(
            LittleMaidScreen screen,
            LittleMaidEntity maid,
            int left,
            int top,
            int size,
            int layer) {
        if (!Platform.isModLoaded("touhou_little_maid")
                || maid == null
                || maid.isRebellious()
                || maid.isStrike()) {
            return List.of();
        }
        // TlmMaidScreenButtons 内部引用车万类；仅在确认车万存在后才加载/调用
        return TlmMaidScreenButtons.create(screen, maid, left, top, size, layer);
    }
}
