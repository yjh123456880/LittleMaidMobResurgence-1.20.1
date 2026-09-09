package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.api.event.ConvertMaidEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

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
        MinecraftForge.EVENT_BUS.addListener(TlmCompatClient::onClientLogout);
    }

    private static void onConvertMaid(ConvertMaidEvent event) {
        if (event.getEntity() instanceof LittleMaidEntity maid
                && maid.getWorld().isClient) {
            // 返回按女仆缓存的同一包装实例，保证车万 Gecko 能力/动画控制器状态连续
            event.setMaid(LittleMaidImaidCache.get(maid));
        }
    }

    /** 退出世界时清理按实体缓存的 IMaid 包装，避免跨世界残留。 */
    private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LittleMaidImaidCache.clear();
    }
}
