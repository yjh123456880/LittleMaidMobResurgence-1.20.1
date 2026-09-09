package net.sistr.littlemaidmobresurgence.forge;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import net.sistr.littlemaidmobresurgence.tags.LMTags;

/**
 * [zh] 车万女仆 IMaid 包装：让 TLM 的渲染/动画引擎把本模组女仆当作“女仆”来读取模型与状态。
 * [en] TLM IMaid wrapper so the TLM render/animation engine can treat our maids as maids.
 * [ja] TLMのIMaidラッパー。TLMの描画・アニメーションエンジンに当Modのメイドをメイドとして扱わせます。
 */
@OnlyIn(Dist.CLIENT)
public class LittleMaidImaid implements IMaid {
    private static final String TLM_ID = "touhou_little_maid";

    private final LittleMaidEntity maid;
    /** [zh] TLM 动画引擎会在此数组中缓存手持物用于识别换手；必须跨帧保持同一实例。 */
    private final ItemStack[] handItemsForAnimation = {ItemStack.EMPTY, ItemStack.EMPTY};

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

    /**
     * [zh] 深度动画桥接：把本模组的工作/战斗/雪仗状态映射成车万女仆的任务 UID，
     *     让车万模型的攻击、弓弩、农夫、火把、喂食、雪仗等任务动画真正播放。
     */
    @Override
    public IMaidTask getTask() {
        if (!deepAnimation()) {
            return TaskManager.getIdleTask();
        }
        return TaskManager.findTask(new Identifier(TLM_ID, resolveTaskPath()))
                .orElse(TaskManager.getIdleTask());
    }

    private String resolveTaskPath() {
        if (maid.isSnowFighting() || maid.isPlayingSnow()) {
            return "snow";
        }
        // 客户端使用 DataTracker 同步的模式名，避免依赖未同步的 Mode 实现
        String mode = maid.getModeName().orElse("");
        boolean ranged =
                switch (mode) {
                    case "Archer", "Gunner", "ArsNouveau", "IronsSpell", "Goety" -> true;
                    default -> false;
                };
        if (ranged) {
            if (maid.getMainHandStack().getItem() instanceof CrossbowItem
                    || maid.getOffHandStack().getItem() instanceof CrossbowItem) {
                return "crossbow_attack";
            }
            return "ranged_attack";
        }
        if (maid.isRebellious()) {
            return "attack";
        }
        return switch (mode) {
            case "Fencer" -> "attack";
            case "Farm" -> "farm";
            case "Torcher" -> "torch";
            case "Healer" -> "feed";
            default -> "idle";
        };
    }

    /** [zh] 持久手持物缓存：TLM 的 hold/use/swing 条件动画依赖该数组识别换手与持械。 */
    @Override
    public ItemStack[] getHandItemsForAnimation() {
        return handItemsForAnimation;
    }

    /** [zh] 战斗/拉弓时举手，触发车万模型的攻击/施法手臂姿态。 */
    @Override
    public boolean isSwingingArms() {
        if (!deepAnimation()) {
            return false;
        }
        return maid.isAimingBow() || isBattleModeName(maid.getModeName().orElse(""));
    }

    /** [zh] 待命且主人手持工资/食物时播放车万的“讨食”动画。 */
    @Override
    public boolean isBegging() {
        if (!deepAnimation() || !TameableUtil.isWait(maid)) {
            return false;
        }
        return TameableUtil.getTameOwner(maid)
                .map(
                        owner -> {
                            ItemStack held = owner.getMainHandStack();
                            return held.isIn(LMTags.Items.MAIDS_SALARY) || held.isFood();
                        })
                .orElse(false);
    }

    /** [zh] 好感度 0–300 按比例映射到车万的 0–384 区间，让表情/小动作阈值生效。 */
    @Override
    public int getFavorability() {
        if (!deepAnimation()) {
            return 0;
        }
        return Math.round(maid.getFavorabilityValue() * 384F / 300F);
    }

    @Override
    public int getExperience() {
        return maid.getSyncedExperiencePoints();
    }

    /** [zh] 梯子动画：沿用原版攀爬状态。 */
    @Override
    public boolean onClimbable() {
        return deepAnimation() && maid.isClimbing();
    }

    /**
     * [zh] 把本模组扩容背包等级映射到车万的小/中/大背包模型层（1–2→小、3–4→中、5→大）。
     */
    @Override
    public IMaidBackpack getMaidBackpackType() {
        if (!deepAnimation()) {
            return BackpackManager.getEmptyBackpack();
        }
        int level = maid.getSyncedBackpackLevel();
        String id =
                level <= 0
                        ? ""
                        : level <= 2 ? "small_backpack" : level <= 4 ? "middle_backpack" : "big_backpack";
        if (id.isEmpty()) {
            return BackpackManager.getEmptyBackpack();
        }
        return BackpackManager.findBackpack(new Identifier(TLM_ID, id))
                .orElse(BackpackManager.getEmptyBackpack());
    }

    private static boolean deepAnimation() {
        return LMMRMod.getConfig().client.tlmDeepAnimation;
    }

    /** [zh] 客户端可用的战斗模式名判定（模式实现本身未必在客户端同步）。 */
    private static boolean isBattleModeName(String mode) {
        return switch (mode) {
            case "Fencer", "Archer", "Gunner", "ArsNouveau", "IronsSpell", "Goety" -> true;
            default -> false;
        };
    }
}
