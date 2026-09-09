package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import net.minecraft.entity.mob.MobEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

/**
 * [zh] 车万女仆 IMaid 包装：让 TLM 的渲染/动画引擎把本模组女仆当作“女仆”来读取模型与状态。
 * [en] TLM IMaid wrapper so the TLM render/animation engine can treat our maids as maids.
 * [ja] TLMのIMaidラッパー。TLMの描画・アニメーションエンジンに当Modのメイドをメイドとして扱わせます。
 */
@OnlyIn(Dist.CLIENT)
public class LittleMaidImaid implements IMaid {
    private final LittleMaidEntity maid;

    public LittleMaidImaid(LittleMaidEntity maid) {
        this.maid = maid;
    }

    @Override
    public String getModelId() {
        return maid.getTlmModelId();
    }

    @Override
    public MobEntity asEntity() {
        return maid;
    }

    @Override
    public boolean isMaidInSittingPose() {
        return maid.isRestSitting() || maid.hasVehicle();
    }
}
