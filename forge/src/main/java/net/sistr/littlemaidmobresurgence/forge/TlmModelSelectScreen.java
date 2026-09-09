package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.client.resource.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.resource.models.MaidModels;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.network.C2SSetTlmModelPacket;

/**
 * [zh] 车万女仆模型选择页：列出 TLM 已加载的女仆模型（Bedrock/Gecko），点击套用到本模组女仆；
 *     顶部提供“还原 LMML 模型”清空按钮。
 * [en] TLM model picker: lists TLM maid models (Bedrock/Gecko) and applies the chosen one to our maid;
 *     a "Restore LMML model" button clears the selection.
 * [ja] TLMモデル選択画面。TLMのメイドモデルを一覧表示して当Modのメイドへ適用します。
 */
@OnlyIn(Dist.CLIENT)
public class TlmModelSelectScreen extends Screen {
    private static final int ENTRY_H = 22;
    private static final int ENTRIES_PER_PAGE = 9;

    private final LittleMaidEntity maid;
    private final List<String> modelIds = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private int page = 0;

    private ButtonWidget prevButton;
    private ButtonWidget nextButton;

    public TlmModelSelectScreen(LittleMaidEntity maid) {
        super(Text.translatable("gui.littlemaidmobresurgence.tlm.title"));
        this.maid = maid;
        collectModels();
    }

    private void collectModels() {
        List<String> ids = new ArrayList<>(
                CustomPackLoader.MAID_MODELS.getModelIdSet());
        ids.sort(Comparator.naturalOrder());
        MaidModels models = CustomPackLoader.MAID_MODELS;
        for (String id : ids) {
            Optional<MaidModelInfo> info = models.getInfo(id);
            String name = info.map(MaidModelInfo::getName).orElse("");
            String extra = info.isPresent() && info.get().isGeckoModel()
                    ? " [Gecko]"
                    : "";
            String label = name == null || name.isEmpty() ? id : name + " (" + id + ")" + extra;
            modelIds.add(id);
            labels.add(label);
        }
    }

    @Override
    public void init() {
        int totalPages = Math.max(1, (modelIds.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        this.clearChildren();

        int centerX = this.width / 2;
        int y = 34;
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, modelIds.size());
        for (int i = start; i < end; i++) {
            int idx = i;
            ButtonWidget entry =
                    ButtonWidget.builder(
                                    Text.literal(labels.get(i)),
                                    b -> selectModel(modelIds.get(idx)))
                            .position(centerX - 150, y)
                            .size(300, 18)
                            .build();
            if (modelIds.get(i).equals(maid.getTlmModelId())) {
                entry.setMessage(
                        Text.literal("> " + labels.get(i))
                                .copy()
                                .formatted(Formatting.YELLOW));
            }
            this.addDrawableChild(entry);
            y += ENTRY_H;
        }

        ButtonWidget clearButton =
                ButtonWidget.builder(
                                Text.translatable("gui.littlemaidmobresurgence.tlm.restore"),
                                b -> selectModel(""))
                        .position(centerX - 150, this.height - 28)
                        .size(140, 20)
                        .build();
        this.addDrawableChild(clearButton);
        this.prevButton =
                ButtonWidget.builder(
                                Text.translatable("gui.littlemaidmobresurgence.curios.prev"),
                                b -> {
                                    page--;
                                    init();
                                })
                        .position(centerX - 6, this.height - 28)
                        .size(60, 20)
                        .build();
        this.nextButton =
                ButtonWidget.builder(
                                Text.translatable("gui.littlemaidmobresurgence.curios.next"),
                                b -> {
                                    page++;
                                    init();
                                })
                        .position(centerX + 62, this.height - 28)
                        .size(60, 20)
                        .build();
        this.addDrawableChild(prevButton);
        this.addDrawableChild(nextButton);
        prevButton.active = page > 0;
        nextButton.active = page < totalPages - 1;
    }

    private void selectModel(String modelId) {
        C2SSetTlmModelPacket.sendC2SPacket(maid, modelId);
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        String current = maid.getTlmModelId();
        String summary = current.isEmpty()
                ? Text.translatable("gui.littlemaidmobresurgence.tlm.current_none").getString()
                : Text.translatable("gui.littlemaidmobresurgence.tlm.current", current).getString();
        context.drawCenteredTextWithShadow(
                this.textRenderer, summary, this.width / 2, 22, 0xFFCCCCCC);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
