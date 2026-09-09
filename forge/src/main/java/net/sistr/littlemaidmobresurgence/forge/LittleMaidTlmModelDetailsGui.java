package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.detail.MaidModelDetailsGui;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.MinecraftClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 复用 TLM 的模型详情页，仅把“返回”重定向回本模组的模型界面（而不是假女仆界面）。
 * [en] Reuses TLM's model details GUI, redirecting only "return" back to our model GUI.
 * [ja] TLMのモデル詳細画面を再利用し、「戻る」だけ当Modのモデル画面へ戻します。
 */
@OnlyIn(Dist.CLIENT)
public class LittleMaidTlmModelDetailsGui extends MaidModelDetailsGui {
    private final LittleMaidEntity realMaid;
    private final EntityMaid previewMaid;

    public LittleMaidTlmModelDetailsGui(
            LittleMaidEntity realMaid, EntityMaid previewMaid, MaidModelInfo modelInfo) {
        super(previewMaid, modelInfo);
        this.realMaid = realMaid;
        this.previewMaid = previewMaid;
    }

    @Override
    protected void applyReturnButtonLogic() {
        MinecraftClient.getInstance()
                .setScreen(new LittleMaidTlmModelGui(realMaid, previewMaid));
    }
}
