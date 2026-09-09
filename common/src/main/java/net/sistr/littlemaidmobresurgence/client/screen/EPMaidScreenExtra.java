package net.sistr.littlemaidmobresurgence.client.screen;

import dev.architectury.injectables.annotations.ExpectPlatform;
import java.util.List;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 女仆主界面的可选额外按钮（平台实现）：车万女仆联动按钮等。
 * [en] Optional extra buttons for the maid main GUI (platform implementation): e.g. the TLM model button.
 * [ja] メイド主人画面の追加ボタン（プラットフォーム実装）。例：TLMモデルボタン。
 */
public class EPMaidScreenExtra {
    /**
     * [zh] 返回要加入女仆主界面左侧图标列的按钮（已设置好坐标）；无联动时返回空列表。
     * [en] Returns buttons (already positioned) for the maid GUI's left icon column; empty when unavailable.
     * [ja] メイド主人画面の左アイコン列に追加するボタン（座標設定済み）を返します。
     */
    @ExpectPlatform
    public static List<ButtonWidget> createExtraButtons(
            LittleMaidScreen screen,
            LittleMaidEntity maid,
            int left,
            int top,
            int size,
            int layer) {
        throw new AssertionError();
    }
}
