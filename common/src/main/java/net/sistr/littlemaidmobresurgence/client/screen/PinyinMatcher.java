package net.sistr.littlemaidmobresurgence.client.screen;

import java.lang.reflect.Method;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 拼音搜索兼容层。
 *
 * <p>运行时若安装了"通用拼音搜索"（Just Enough Characters / jecharacters, modid: jecharacters），
 * 则通过反射调用 {@code me.towdium.jecharacters.utils.Match.contains(text, keyword)}
 * 获得拼音模糊匹配能力（支持中文全拼/首字母、拼音+原文字符混合）；
 * 未安装时回退为普通 {@link String#contains} 包含匹配，无强依赖，Forge/Fabric 双端通用。
 */
@Environment(EnvType.CLIENT)
public final class PinyinMatcher {
    private static final boolean JECHARS_LOADED;
    private static final Method MATCH_CONTAINS;

    static {
        boolean loaded = false;
        Method m = null;
        try {
            // 纯反射检测类是否存在：能加载即说明 jecharacters 已安装
            Class<?> clazz = Class.forName("me.towdium.jecharacters.utils.Match");
            // Match.contains(CharSequence text, CharSequence keyword) -> boolean
            m = clazz.getMethod("contains", CharSequence.class, CharSequence.class);
            loaded = true;
        } catch (Throwable t) {
            loaded = false;
            m = null;
        }
        JECHARS_LOADED = loaded;
        MATCH_CONTAINS = m;
    }

    private PinyinMatcher() {}

    /** @return true 表示 keyword 命中 text（安装 jecharacters 时启用拼音模糊匹配） */
    public static boolean contains(String text, String keyword) {
        if (text == null || keyword == null) return false;
        String k = keyword.trim();
        if (k.isEmpty()) return true;
        // 普通子串匹配（小写）始终作为兜底
        String t = text.toLowerCase(Locale.ROOT);
        String kl = k.toLowerCase(Locale.ROOT);
        if (t.contains(kl)) return true;
        // jecharacters 拼音匹配（传入原文，由其内部处理拼音匹配逻辑）
        if (JECHARS_LOADED && MATCH_CONTAINS != null) {
            try {
                Object ret = MATCH_CONTAINS.invoke(null, text, k);
                if (ret instanceof Boolean b) return b;
            } catch (Throwable ignored) {
                // 反射失败静默降级到普通匹配
            }
        }
        return false;
    }

    /** @return 当前是否已加载 jecharacters 拼音搜索模组 */
    public static boolean isPinyinAvailable() {
        return JECHARS_LOADED;
    }
}
