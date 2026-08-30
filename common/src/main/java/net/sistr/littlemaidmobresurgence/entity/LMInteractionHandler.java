package net.sistr.littlemaidmobresurgence.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.item.SaddleItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.ItemEntity;
import net.sistr.littlemaidmodelloader.network.SyncMultiModelPacket;
import net.sistr.littlemaidmobresurgence.advancement.criterion.LMMRCriteria;
import net.sistr.littlemaidmobresurgence.config.LMMRConfig;
import net.sistr.littlemaidmobresurgence.entity.util.MovingMode;
import net.sistr.littlemaidmobresurgence.entity.util.TameableUtil;
import net.sistr.littlemaidmobresurgence.item.MaidCarryItem;
import net.sistr.littlemaidmobresurgence.item.MaidStickItem;
import net.sistr.littlemaidmobresurgence.tags.LMTags;

/**
 * [zh] 玩家与女仆的右键交互总入口：按手持物品/女仆状态分派契约、喂食、模式切换、取物、打开背包等。
 * [en] Central right-click interaction handler between players and maids: dispatches contracting, feeding, mode switching, item-taking, opening the inventory, etc.
 * [ja] プレイヤーとメイドの右クリック操作を一元的に処理します：契約、餌やり、モード切替、アイテム取り出し、インベントリ表示などを振り分けます。
 */
final class LMInteractionHandler {

    private LMInteractionHandler() {}

    static ActionResult handle(LittleMaidEntity maid, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        // [zh] 女仆杖：绑定/清除信息（必须在 sneaking 判定前处理）
        // [en] Maid stick: bind/clear logic must run before the sneaking branch.
        // [ja] メイドステッキ：登録・解除はスニーク判定より先に処理。
        if (stack.getItem() instanceof MaidStickItem maidStick) {
            return maidStick.interactMaid(maid, player, hand);
        }
        // [zh] 女仆捕捉蛋：收纳女仆（空蛋）
        // [en] Maid capture egg: capture the maid (empty egg).
        // [ja] メイド捕捉卵：メイドを収納（空の卵）。
        if (stack.getItem() instanceof MaidCarryItem maidCarry) {
            return maidCarry.interactMaid(maid, player, hand);
        }
        if (player.isSneaking()) {
            // [zh] shift+右键：快捷取出女仆主手物品（对齐 1.12.2 小女仆模组的取物交互）
            // [en] Shift+right-click: quickly take the maid's main-hand item (mirrors the 1.12.2 little-maid interaction).
            // [ja] Shift+右クリック：メイドのメインハンドのアイテムを素早く取り出します（1.12.2 準拠）。
            return takeMainHandItem(maid, player);
        }
        // [zh] 无主人的野生女仆：仅可雇佣
        // [en] Wild maid without an owner: only employable.
        // [ja] 主人がいない場合：契約のみ可能。
        if (TameableUtil.getTameOwnerUuid(maid).isEmpty()) {
            if (stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return contract(maid, player, stack, false);
            }
            return ActionResult.PASS;
        }
        // [zh] 非主人无法交互
        // [en] Non-owners cannot interact.
        // [ja] 主人ではない場合：操作不可。
        if (!player.getUuid().equals(maid.getOwnerUuid())) {
            return ActionResult.PASS;
        }
        // [zh] 罢工状态：仅可用雇佣物品复雇解除（不再用糖回血）
        // [en] Strike state: only re-contracting with an employ item ends it (sugar no longer heals strikes).
        // [ja] ストライキ時：雇用アイテムでの再契約のみで解除（砂糖での回復は廃止）。
        if (maid.isStrike()) {
            // [zh] 用雇佣物品复雇解除罢工
            // [en] Re-contract to end the strike.
            // [ja] 雇用アイテムで再契約し解除。
            if (stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return contract(maid, player, stack, true);
            }
            maid.getWorld().sendEntityStatus(maid, (byte) 6);
            return ActionResult.PASS;
        }
        // [zh] 手持鞍：骑乘/放下
        // [en] Saddle in hand: mount/dismount.
        // [ja] サドルを持っているとき：騎乗・下ろす。
        if (stack.getItem() instanceof SaddleItem) {
            return handleSaddle(maid, player);
        }
        // [zh] 女仆正被该玩家背着（肩车）时不响应
        // [en] Ignored while the maid is being carried by this player.
        // [ja] 肩車されているとき：応答しない。
        if (maid.getVehicle() == player) {
            return ActionResult.PASS;
        }
        // [zh] 砂糖（工资）：回血+恢复饱食度+提升好感/心情，并切换待命状态
        // [en] Sugar (salary): heals, restores satiety, boosts favorability/mood, then toggles waiting.
        // [ja] 砂糖（お給料）：回復＋満腹度＋好感度・機嫌UP、待機状態を切替。
        if (stack.isIn(LMTags.Items.MAIDS_SALARY)) {
            LMMRConfig config = LittleMaidEntity.getConfig();
            // [zh] 喂糖 → 治疗+好感+心情（仅服务端治疗，避免客户端血量预测闪烁）
            // [en] Feed sugar → heal + favorability + mood (healing only on the server to avoid client HP flicker).
            // [ja] 餌を与える → 治療・好感度・機嫌UP（治療はサーバー側のみで実行し、クライアントのHP予測ちらつきを回避）。
            if (!maid.getWorld().isClient) {
                maid.heal(config.health.healAmount);
                // [zh] 糖回血的同时按配置恢复饱食度（无食用动画，直接生效）
                // [en] Sugar also restores satiety per config (no eating animation, applied instantly).
                // [ja] 砂糖は同時に設定値ぶん満腹度を回復（食事アニメなしで即時反映）。
                maid.setHunger(maid.getHungerValue() + config.hunger.sugarSatietyRestore);
                // [zh] 副手短暂持糖展示消耗动画（饱食度已即时恢复，视觉上不重复恢复）
                // [en] Show a brief off-hand sugar-holding animation (satiety is already restored, so no visual double-restore).
                // [ja] オフハンドで砂糖を持つ短いアニメを表示（満腹度は即時回復済みのため視覚的な二重回復なし）。
                ItemStack sugarVisual = stack.copy();
                sugarVisual.setCount(1);
                maid.startSugarConsume(sugarVisual, false);
                maid.maidMood.onFed();
                maid.syncMood();
                MaidSpeech.onFed(maid);
            }
            return changeState(maid, player, stack);
        }
        // [zh] 玩家主动喂食：手持食物右键直接喂食（恢复饥饿、加好感、提情绪），工资类物品除外
        // [en] Player-fed food: right-click with food restores hunger, favorability and mood (salary items excluded).
        // [ja] プレイヤーからの餌やり：食べ物を右クリックで満腹度・好感度・機嫌を回復（お給料アイテムは除外）。
        if (stack.isFood() && !stack.isIn(LMTags.Items.MAIDS_SALARY)) {
            return feedFood(maid, player, hand, stack);
        }
        // [zh] 羽毛：切换自由/跟随
        // [en] Feather: toggle free-move/escort.
        // [ja] 羽根：自由行動/追従を切替。
        if (stack.getItem() == Items.FEATHER) {
            return handleFeather(maid);
        }
        // [zh] 红石：切换自由/红石巡逻
        // [en] Redstone: toggle free-move/tracer.
        // [ja] レッドストーン：自由行動/レッドストーン追跡を切替。
        if ((maid.getMovingMode() == MovingMode.FREEDOM
                        || maid.getMovingMode() == MovingMode.TRACER)
                && stack.getItem() == Items.REDSTONE) {
            return handleRedstone(maid);
        }
        // [zh] 玻璃瓶 → 附魔之瓶（消耗女仆经验）
        // [en] Glass bottle → experience bottle (consumes maid XP).
        // [ja] ガラス瓶→エンチャントの瓶（メイドの経験値を消費）。
        if (maid.getExperiencePoints() >= LittleMaidEntity.getConfig().misc.experienceBottleCost
                && stack.isOf(Items.GLASS_BOTTLE)) {
            return handleGlassBottle(maid, player, hand, stack);
        }
        // [zh] 桶 → 牛奶桶（若开启挤奶）
        // [en] Bucket → milk bucket (if milking is enabled).
        // [ja] バケツ→ミルクバケツ（搾乳が有効な場合）。
        if (LittleMaidEntity.getConfig().misc.canMilking && stack.isOf(Items.BUCKET)) {
            return handleBucket(maid, player, hand, stack);
        }
        // [zh] 火药 → 加速
        // [en] Gunpowder → acceleration.
        // [ja] 火薬→加速。
        if (stack.getItem() == Items.GUNPOWDER) {
            return handleGunpowder(maid, player, stack);
        }
        maid.openInventory(player);
        return ActionResult.success(maid.getWorld().isClient);
    }

    /** shift+右键快捷取出女仆主手物品：放入玩家背包，背包满则在女仆面前掉落。 */
    private static ActionResult takeMainHandItem(LittleMaidEntity maid, PlayerEntity player) {
        if (maid.getWorld().isClient) {
            // 客户端只放行动画，实际逻辑由服务端执行
            return ActionResult.SUCCESS;
        }
        ItemStack held = maid.getMainHandStack();
        if (held.isEmpty()) {
            return ActionResult.PASS;
        }
        // 整体取出并清空女仆主手
        ItemStack taken = held.copy();
        maid.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        if (!player.getInventory().insertStack(taken)) {
            // 背包放不下：原地掉落（带短暂拾取延迟，避免立即被女仆吸回）
            ItemEntity drop =
                    new ItemEntity(
                            maid.getWorld(),
                            maid.getX(),
                            maid.getEyeY() - 0.3,
                            maid.getZ(),
                            taken);
            drop.setPickupDelay(20);
            maid.getWorld().spawnEntity(drop);
        }
        maid.getWorld().playSound(
                null,
                maid.getX(),
                maid.getY(),
                maid.getZ(),
                SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS,
                0.8F,
                maid.getRandom().nextFloat() * 0.2F + 0.9F);
        return ActionResult.SUCCESS;
    }

    private static ActionResult handleSaddle(LittleMaidEntity maid, PlayerEntity player) {
        if (!maid.hasVehicle()) {
            if (player.hasPassengers()) {
                player.removeAllPassengers();
            }
            maid.startRiding(player);
        } else {
            var vehicle = maid.getVehicle();
            if (vehicle == player) {
                maid.stopRiding();
            }
        }
        return ActionResult.success(maid.getWorld().isClient);
    }

    private static ActionResult handleFeather(LittleMaidEntity maid) {
        if (maid.getMovingMode() == MovingMode.ESCORT) {
            maid.getWorld().sendEntityStatus(maid, (byte) 73);
            maid.setMovingMode(MovingMode.FREEDOM);
            maid.setFreedomPos(maid.getBlockPos());
        } else {
            maid.getWorld().sendEntityStatus(maid, (byte) 74);
            maid.setMovingMode(MovingMode.ESCORT);
        }
        return ActionResult.success(maid.getWorld().isClient);
    }

    private static ActionResult handleRedstone(LittleMaidEntity maid) {
        if (maid.getMovingMode() == MovingMode.FREEDOM) {
            maid.getWorld().sendEntityStatus(maid, (byte) 75);
            maid.setMovingMode(MovingMode.TRACER);
        } else {
            maid.getWorld().sendEntityStatus(maid, (byte) 73);
            maid.setMovingMode(MovingMode.FREEDOM);
            maid.setFreedomPos(maid.getBlockPos());
        }
        return ActionResult.success(maid.getWorld().isClient);
    }

    private static ActionResult handleGlassBottle(
            LittleMaidEntity maid, PlayerEntity player, Hand hand, ItemStack stack) {
        maid.getWorld()
                .playSound(
                        null,
                        maid.getX(),
                        maid.getY(),
                        maid.getZ(),
                        SoundEvents.ITEM_BOTTLE_FILL,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f);
        ItemStack itemStack2 =
                ItemUsage.exchangeStack(stack, player, Items.EXPERIENCE_BOTTLE.getDefaultStack());
        player.setStackInHand(hand, itemStack2);
        maid.addExperience(-LittleMaidEntity.getConfig().misc.experienceBottleCost);
        return ActionResult.success(maid.getWorld().isClient);
    }

    private static ActionResult handleBucket(
            LittleMaidEntity maid, PlayerEntity player, Hand hand, ItemStack stack) {
        player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
        ItemStack itemStack2 =
                ItemUsage.exchangeStack(stack, player, Items.MILK_BUCKET.getDefaultStack());
        player.setStackInHand(hand, itemStack2);
        return ActionResult.success(maid.getWorld().isClient);
    }

    private static ActionResult handleGunpowder(
            LittleMaidEntity maid, PlayerEntity player, ItemStack stack) {
        LMMRConfig config = LittleMaidEntity.getConfig();
        int maxAccelerationStack = config.misc.maxAccelerationStack;
        int accelerationTicks = config.misc.accelerationTicksPerStack;
        int resumeCount = Math.min(maxAccelerationStack, stack.getCount());
        int acTicks = resumeCount * accelerationTicks;
        maid.setAccelerationTicks(acTicks);

        if (!player.getAbilities().creativeMode) {
            stack.decrement(resumeCount);
            if (stack.isEmpty()) {
                player.getInventory().removeOne(stack);
            }
        }

        return ActionResult.success(maid.getWorld().isClient);
    }

    /**
     * [zh] 玩家主动喂食：恢复饥饿、增加好感、提升情绪；正在进食时不接受新喂食。
     * [en] Player-fed food: restores hunger, favorability and mood; new feeding is rejected while eating.
     * [ja] プレイヤーからの餌やり：満腹度・好感度・機嫌を回復。食事中は新しい餌を受け付けません。
     */
    private static ActionResult feedFood(
            LittleMaidEntity maid, PlayerEntity player, Hand hand, ItemStack stack) {
        // [zh] 喂食间隔阻拦：上一口没吃完不能喂下一口
        // [en] Feeding interval: no new bite until the current one finishes.
        // [ja] 食事間隔：前の一口が終わるまで次の餌は受け付けません。
        if (maid.isEating()) {
            return ActionResult.PASS;
        }
        if (!maid.getWorld().isClient) {
            var food = stack.getItem().getFoodComponent();
            if (food != null) {
                // [zh] 恢复饥饿（营养值 × 4 → 0-100 刻度）
                // [en] Restore hunger (nutrition × 4 → 0-100 scale).
                // [ja] 満腹度を回復（栄養値×4 → 0-100 スケール）。
                int restore = Math.max(1, food.getHunger() * 4);
                maid.setHunger(maid.getHungerValue() + restore);
                // [zh] 好感度 + 心情值（喂食也顺带安抚怒气）
                // [en] Favorability + mood (feeding also soothes anger).
                // [ja] 好感度＋機嫌（餌やりで怒りも鎮めます）。
                maid.maidMood.onFed();
                maid.syncMood();
                MaidSpeech.onFed(maid);
            }
            // [zh] 进食动画：副手拿食物放到嘴边，速度与玩家一致，并减速
            // [en] Eating animation: food held to the mouth at vanilla speed with the eating slow-down.
            // [ja] 食事アニメ：オフハンドで食物を口元へ。速度はバニラと同等で減速します。
            ItemStack foodStack = stack.copy();
            foodStack.setCount(1);
            maid.startEatingItem(foodStack);
        }
        maid.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0F, 1.0F);
        consumeItem(player, stack, 1);
        return ActionResult.success(maid.getWorld().isClient);
    }

    static ActionResult changeState(LittleMaidEntity maid, PlayerEntity player, ItemStack stack) {
        maid.getWorld().sendEntityStatus(maid, (byte) 72);
        maid.playSound(
                SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, maid.getRandom().nextFloat() * 0.1F + 1.0F);
        maid.setFreedomPos(maid.getBlockPos());
        maid.getNavigation().stop();
        TameableUtil.switchWait(maid);
        consumeItem(player, stack, 1);
        return ActionResult.success(maid.getWorld().isClient);
    }

    static ActionResult contract(
            LittleMaidEntity maid, PlayerEntity player, ItemStack stack, boolean isReContract) {
        if (!isReContract) {
            maid.getWorld().sendEntityStatus(maid, (byte) 70);
            if (player instanceof ServerPlayerEntity) {
                LMMRCriteria.CONTRACT_MAID.trigger((ServerPlayerEntity) player, maid);
            }
        } else {
            maid.getWorld().sendEntityStatus(maid, (byte) 71);
        }
        maid.setOwnerUuid(player.getUuid());
        maid.setContractMM(true);
        // [zh] 契约后心情立即提升
        // [en] Mood boosts right after contracting.
        // [ja] 契約直後は嬉しい状態に。
        if (!maid.getWorld().isClient) {
            maid.maidMood.onFed();
            maid.syncMood();
            MaidSpeech.onContract(maid);
        }
        if (!maid.getWorld().isClient) {
            SyncMultiModelPacket.sendS2CPacket(maid, maid);
        }
        maid.setStrike(false);
        maid.itemContractable.setUnpaidTimes(0);
        maid.getNavigation().stop();
        maid.setMovingMode(MovingMode.ESCORT);
        consumeItem(player, stack, 1);
        return ActionResult.success(maid.getWorld().isClient);
    }

    /**
     * [zh] 消耗玩家手持物品（创造模式不消耗）。
     * [en] Consumes the player's held item (not in creative mode).
     * [ja] プレイヤーの手持ちアイテムを消費します（クリエイティブでは消費しません）。
     */
    private static void consumeItem(PlayerEntity player, ItemStack stack, int amount) {
        if (!player.getAbilities().creativeMode) {
            stack.decrement(amount);
            if (stack.isEmpty()) {
                player.getInventory().removeOne(stack);
            }
        }
    }
}
