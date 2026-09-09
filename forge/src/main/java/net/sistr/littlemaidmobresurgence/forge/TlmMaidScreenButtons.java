package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.sistr.littlemaidmobresurgence.client.screen.LittleMaidScreen;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 车万模型按钮：使用“博丽的御币”物品贴图作为图标，点击打开车万自带的模型选择界面。
 * [en] TLM model button: uses the "Hakurei Gohei" item texture as its icon and opens TLM's own model GUI.
 * [ja] TLMモデルボタン。「博麗の御幣」のアイテムテクスチャをアイコンにし、TLM標準のモデル画面を開きます。
 */
@OnlyIn(Dist.CLIENT)
public final class TlmMaidScreenButtons {
    private static final String TLM_ID = "touhou_little_maid";
    private static final Identifier HAKUREI_GOHEI_ID = new Identifier(TLM_ID, "hakurei_gohei");

    private TlmMaidScreenButtons() {}

    public static List<ButtonWidget> create(
            LittleMaidScreen screen,
            LittleMaidEntity maid,
            int left,
            int top,
            int size,
            int layer) {
        LittleMaidScreen.IconButtonWidget button =
                new LittleMaidScreen.IconButtonWidget(
                        left - size,
                        top + size * (layer + 1),
                        goheiIcon(),
                        Text.translatable("gui.littlemaidmobresurgence.tlm.open"),
                        b -> openNativeGui(maid));
        return List.of(button);
    }

    private static ItemStack goheiIcon() {
        Item item = ForgeRegistries.ITEMS.getValue(HAKUREI_GOHEI_ID);
        if (item == null || item == Items.AIR) {
            // 极端情况下车万未注册该物品时使用木棍兜底，避免按钮空白
            return new ItemStack(Items.STICK);
        }
        return new ItemStack(item);
    }

    private static void openNativeGui(LittleMaidEntity maid) {
        EntityMaid preview = TlmPreviewMaidFactory.create(maid);
        if (preview == null) {
            return;
        }
        MinecraftClient.getInstance().setScreen(new LittleMaidTlmModelGui(maid, preview));
    }
}
