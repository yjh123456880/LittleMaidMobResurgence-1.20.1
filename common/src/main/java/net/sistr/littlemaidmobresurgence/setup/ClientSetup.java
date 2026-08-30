package net.sistr.littlemaidmobresurgence.setup;

import dev.architectury.registry.menu.MenuRegistry;
import net.sistr.littlemaidmobresurgence.client.key.LMKeys;
import net.sistr.littlemaidmobresurgence.client.screen.BackpackScreen;
import net.sistr.littlemaidmobresurgence.client.screen.CuriosScreen;
import net.sistr.littlemaidmobresurgence.client.screen.LittleMaidScreen;

public class ClientSetup {

    public static void init() {
        MenuRegistry.registerScreenFactory(
                Registration.LITTLE_MAID_SCREEN_HANDLER.get(), LittleMaidScreen::new);
        MenuRegistry.registerScreenFactory(
                Registration.CURIOS_SCREEN_HANDLER.get(), CuriosScreen::new);
        MenuRegistry.registerScreenFactory(
                Registration.BACKPACK_SCREEN_HANDLER.get(), BackpackScreen::new);
        LMKeys.init();
    }
}
