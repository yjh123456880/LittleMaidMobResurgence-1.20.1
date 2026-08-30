package net.sistr.littlemaidmobresurgence.client.key;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.option.KeyBinding;
import net.sistr.littlemaidmobresurgence.network.OpenMaidManagerScreenPacket;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键定义。
 *
 * <p>注意：此处仅构造 {@link KeyBinding}，不在类加载时注册——Architectury 的
 * KeyMappingRegistry 在 Forge 侧的 RegisterKeyMappingsEvent 触发后调用会抛出
 * "registered after event" 异常（并中断 ClientSetup 后续初始化）。实际注册由
 * forge 模块在 RegisterKeyMappingsEvent 事件期间完成。
 */
public class LMKeys {
    public static final KeyBinding OPEN_MAID_MANAGER_SCREEN =
            new KeyBinding(
                    "key.littlemaidmobresurgence.open_maid_manager_screen",
                    GLFW.GLFW_KEY_M,
                    "key.categories.littlemaidmobresurgence");

    public static void init() {
        ClientTickEvent.CLIENT_PRE.register(
                client -> {
                    boolean flag = false;
                    while (OPEN_MAID_MANAGER_SCREEN.wasPressed()) {
                        flag = true;
                    }
                    if (flag) {
                        if (client.player == null || client.currentScreen != null) {
                            return;
                        }
                        OpenMaidManagerScreenPacket.sendC2SPacket();
                    }
                });
    }
}
