package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.client.screen.LittleMaidScreen;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 车万模型按钮：使用车万女仆模组 logo 作为图标，点击打开车万自带的模型选择界面。
 * [en] TLM model button: uses the TLM mod logo as its icon and opens TLM's own model selection GUI.
 * [ja] TLMモデルボタン。TLM本体のロゴをアイコンにし、TLM標準のモデル選択画面を開きます。
 */
@OnlyIn(Dist.CLIENT)
public final class TlmMaidScreenButtons {
    private static final String TLM_ID = "touhou_little_maid";

    private TlmMaidScreenButtons() {}

    public static List<ButtonWidget> create(
            LittleMaidScreen screen,
            LittleMaidEntity maid,
            int left,
            int top,
            int size,
            int layer) {
        TlmLogoButton button =
                new TlmLogoButton(
                        left - size,
                        top + size * (layer + 1),
                        Text.translatable("gui.littlemaidmobresurgence.tlm.open"),
                        b -> openNativeGui(maid));
        return List.of(button);
    }

    private static void openNativeGui(LittleMaidEntity maid) {
        EntityMaid preview = TlmPreviewMaidFactory.create(maid);
        if (preview == null) {
            return;
        }
        MinecraftClient.getInstance().setScreen(new LittleMaidTlmModelGui(maid, preview));
    }

    /** 主 GUI 同款 20×20 图标按钮，图标为运行时从车万 jar 读取的 logo。 */
    private static final class TlmLogoButton extends LittleMaidScreen.IconButtonWidget {
        private TlmLogoButton(int x, int y, Text tooltip, ButtonWidget.PressAction onPress) {
            super(x, y, ItemStack.EMPTY, tooltip, onPress);
        }

        @Override
        protected void renderButton(
                DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderButton(context, mouseX, mouseY, delta);
            Identifier logo = TlmLogoTexture.get();
            if (logo != null) {
                context.drawTexture(
                        logo,
                        getX() + 2,
                        getY() + 2,
                        0.0F,
                        0.0F,
                        16,
                        16,
                        16,
                        16);
                return;
            }
            ItemStack fallback = TlmLogoTexture.fallback();
            if (!fallback.isEmpty()) {
                context.drawItem(
                        fallback,
                        getX() - 8 + getWidth() / 2,
                        getY() - 8 + getHeight() / 2);
            }
        }
    }

    /**
     * [zh] 运行时从车万女仆 jar 读取 mods.toml 指定的 logo.png；不打包进本模组。
     *     读取失败时回退为车万刷怪蛋物品图标。
     */
    private static final class TlmLogoTexture {
        private static final Identifier LOGO_ID = new Identifier(LMMRMod.MODID, "tlm_logo");
        private static boolean attempted;
        private static Identifier logo;
        private static ItemStack fallback = ItemStack.EMPTY;

        private TlmLogoTexture() {}

        static Identifier get() {
            ensureLoaded();
            return logo;
        }

        static ItemStack fallback() {
            ensureLoaded();
            return fallback;
        }

        private static void ensureLoaded() {
            if (attempted) {
                return;
            }
            attempted = true;
            try {
                String logoFile =
                        ModList.get()
                                .getModContainerById(TLM_ID)
                                .flatMap(container -> container.getModInfo().getLogoFile())
                                .orElse("logo.png");
                var fileInfo = ModList.get().getModFileById(TLM_ID);
                if (fileInfo == null) {
                    throw new IllegalStateException("TLM mod file not found");
                }
                Path path = fileInfo.getFile().findResource(logoFile);
                try (InputStream input = Files.newInputStream(path)) {
                    NativeImage image = NativeImage.read(input);
                    MinecraftClient.getInstance()
                            .getTextureManager()
                            .registerTexture(LOGO_ID, new NativeImageBackedTexture(image));
                    logo = LOGO_ID;
                }
            } catch (Throwable t) {
                LMMRMod.LOGGER.warn(
                        "[LMR] Failed to load Touhou Little Maid logo, using spawn egg icon", t);
                Item item =
                        ForgeRegistries.ITEMS.getValue(
                                new Identifier(TLM_ID, "maid_spawn_egg"));
                if (item != null) {
                    fallback = new ItemStack(item);
                }
            }
        }
    }
}
