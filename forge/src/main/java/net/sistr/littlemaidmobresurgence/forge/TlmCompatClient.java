package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.api.event.ConvertMaidEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.sistr.littlemaidmobresurgence.client.screen.LittleMaidScreen;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidScreenHandler;

/**
 * [zh] 车万女仆客户端联动：把本模组女仆注册进 TLM 的 IMaid 转换事件。
 *     仅在检测到车万女仆模组时由 LMMRForge 调用，防止 TLM 缺失时加载此类。
 * [en] TLM client integration: registers our maids through TLM's IMaid conversion event.
 *     Only invoked when TLM is loaded, so these classes never load without TLM.
 * [ja] TLMクライアント連携：当ModのメイドをTLMのIMaid変換イベントに登録します。
 *     TLM導入時のみLMMRForgeから呼ばれるため、TLM未導入時にこのクラスはロードされません。
 */
@OnlyIn(Dist.CLIENT)
public final class TlmCompatClient {
    private TlmCompatClient() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(TlmCompatClient::onConvertMaid);
        MinecraftForge.EVENT_BUS.addListener(TlmCompatClient::onScreenInit);
    }

    private static void onConvertMaid(ConvertMaidEvent event) {
        if (event.getEntity() instanceof LittleMaidEntity maid
                && maid.getWorld().isClient) {
            event.setMaid(new LittleMaidImaid(maid));
        }
    }

    /** 在本模组女仆主界面右下角添加“车万模型”按钮（仅 TLM 存在时界面才出现）。 */
    private static void onScreenInit(ScreenEvent.Init event) {
        if (!(event.getScreen() instanceof LittleMaidScreen screen)) {
            return;
        }
        LittleMaidScreenHandler handler = screen.getScreenHandler();
        LittleMaidEntity maid = handler.getGuiEntity();
        if (maid == null) {
            return;
        }
        ButtonWidget tlmButton =
                ButtonWidget.builder(
                                Text.translatable("gui.littlemaidmobresurgence.tlm.open"),
                                b -> {
                                    if (MinecraftClient.getInstance() != null) {
                                        MinecraftClient.getInstance().setScreen(
                                                new TlmModelSelectScreen(maid));
                                    }
                                })
                        .size(64, 20)
                        .build();
        tlmButton.setX(screen.width - 74);
        tlmButton.setY(screen.height - 24);
        event.addListener(tlmButton);
    }
}
