package net.sistr.littlemaidmobresurgence.forge;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 按女仆缓存 IMaid 包装实例：保证车万 Gecko 能力与 Bedrock 每帧转换都拿到同一实例，
 *     从而让动画控制器、手持物缓存、换手识别等状态跨帧连续。
 * [en] Caches one IMaid wrapper per maid so TLM's gecko capability and per-frame bedrock conversion
 *     share the same instance, keeping animation controllers and held-item state continuous.
 * [ja] メイドごとにIMaidラッパーをキャッシュし、TLMのGecko能力とBedrock毎フレーム変換で同一インスタンスを共有します。
 */
@OnlyIn(Dist.CLIENT)
public final class LittleMaidImaidCache {
    private static final Map<LittleMaidEntity, LittleMaidImaid> CACHE = new WeakHashMap<>();

    private LittleMaidImaidCache() {}

    public static LittleMaidImaid get(LittleMaidEntity maid) {
        return CACHE.computeIfAbsent(maid, LittleMaidImaid::new);
    }

    public static void clear() {
        CACHE.clear();
    }
}
