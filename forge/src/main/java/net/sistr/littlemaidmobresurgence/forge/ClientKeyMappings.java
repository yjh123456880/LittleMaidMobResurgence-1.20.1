package net.sistr.littlemaidmobresurgence.forge;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.sistr.littlemaidmobresurgence.client.key.LMKeys;

/**
 * 客户端按键注册（仅客户端加载）。
 *
 * <p>必须在 Forge 的 RegisterKeyMappingsEvent 事件期间注册按键。 之前在 FMLClientSetupEvent（晚于该事件）中经 Architectury 的
 * KeyMappingRegistry 注册， 会抛出 "Key mapping ... registered after event" 异常，并导致 ClientSetup.init 中
 * 后续初始化（LMKeys.init、LMML 客户端初始化）被中断。
 *
 * <p>本类仅由 {@link LMMRForge} 在物理客户端（FMLEnvironment.dist == CLIENT）时注册监听， 专用服务器不会加载本类及
 * RegisterKeyMappingsEvent。
 */
final class ClientKeyMappings {
    private ClientKeyMappings() {}

    static void register(RegisterKeyMappingsEvent event) {
        event.register(LMKeys.OPEN_MAID_MANAGER_SCREEN);
    }
}
