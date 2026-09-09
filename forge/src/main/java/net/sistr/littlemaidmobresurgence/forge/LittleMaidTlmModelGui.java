package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.model.MaidModelGui;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.network.C2SSetTlmModelPacket;

/**
 * [zh] 直接继承车万女仆的 MaidModelGui：完全复用其模型包列表、分页、搜索、预览与详情页，
 *     只把“应用模型/打开详情/关闭”三个钩子重定向到本模组女仆。
 * [en] Extends TLM's MaidModelGui to fully reuse its model list, paging, search, preview and details GUI,
 *     redirecting only the apply-model / open-details / close hooks to our maid.
 * [ja] TLMのMaidModelGuiを継承し、モデル一覧・ページ・検索・プレビュー・詳細画面をそのまま利用します。
 */
@OnlyIn(Dist.CLIENT)
public class LittleMaidTlmModelGui extends MaidModelGui {
    private final LittleMaidEntity realMaid;
    private final EntityMaid previewMaid;

    public LittleMaidTlmModelGui(LittleMaidEntity realMaid, EntityMaid previewMaid) {
        super(previewMaid);
        this.realMaid = realMaid;
        this.previewMaid = previewMaid;
    }

    @Override
    protected void notifyModelChange(EntityMaid maid, MaidModelInfo info) {
        String modelId = info.getModelId().toString();
        previewMaid.setModelId(modelId);
        C2SSetTlmModelPacket.sendC2SPacket(realMaid, modelId);
    }

    @Override
    protected void openDetailsGui(EntityMaid maid, MaidModelInfo modelInfo) {
        MinecraftClient.getInstance()
                .setScreen(new LittleMaidTlmModelDetailsGui(realMaid, previewMaid, modelInfo));
    }

    @Override
    protected void onClickCloseButton() {
        this.close();
    }

    @Override
    public void init() {
        super.init();
        int startX = this.width / 2 + 50;
        int startY = this.height / 2;
        ButtonWidget restoreButton =
                ButtonWidget.builder(
                                Text.translatable("gui.littlemaidmobresurgence.tlm.restore"),
                                b -> {
                                    C2SSetTlmModelPacket.sendC2SPacket(realMaid, "");
                                    this.close();
                                })
                        .position(startX - 96, startY + 122)
                        .size(98, 18)
                        .build();
        this.addDrawableChild(restoreButton);
    }

    @Override
    public void tick() {
        super.tick();
        // 让预览实体的动画 tick 持续推进
        previewMaid.age++;
    }
}
