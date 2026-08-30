package net.sistr.littlemaidmobresurgence.forge;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.inventory.Inventory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.ItemMatcher;
import net.sistr.littlemaidmobresurgence.api.mode.ModeManager;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.compat.TaczCompat;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.mode.GunnerMode;

/**
 * [zh] TACZ 枪械适配实现 + 枪手模式注册（仅当 TACZ 已安装时加载本类）。
 *     射击/瞄准/拔枪/拉栓/换弹全部委托给 TACZ 状态机；无弹药时通过女仆 ITEM_HANDLER 能力检索背包弹药。
 * [en] TACZ gun adapter + gunner-mode registration (loaded only when TACZ is installed).
 *     Firing/aiming/drawing/bolting/reloading are delegated to TACZ's state machine; on NO_AMMO the maid's
 *     ITEM_HANDLER capability is scanned for backpack ammo.
 * [ja] TACZ 銃アダプター実装＋ガンナーモード登録（TACZ 導入時のみロード）。
 *     射撃・照準・抜銃・ボルト・リロードは TACZ のステートマシンへ委譲。弾切れ時はメイドの
 *     ITEM_HANDLER 能力からバックパック内の弾薬を検索します。
 */
public class TaczGunAdapterImpl implements TaczCompat.GunAdapter {
    /**
     * [zh] 超出该距离先开镜瞄准再射击；近距则收镜（避免狙击镜挡视野/近战贴脸打不中）。
     * [en] Beyond this distance, aim before firing; up close, un-aim (avoids scope blocking the view / point-blank misses).
     * [ja] この距離を超えると照準してから撃ち、近距離では照準を外します（スコープが視界を遮る・密着ミスの防止）。
     */
    private static final float AIM_DISTANCE = 10F;
    /**
     * 记录每个女仆最近一次刷新配件缓存的枪械 ID。
     *
     * <p>TACZ 只会在玩家拔枪/切枪等客户端事件时刷新实体上的配件缓存（AttachmentCacheProperty）。
     * 女仆由本适配器驱动，若她中途换武器（例如从普通枪换成 RPG/榴弹发射器），缓存仍是旧枪数据，
     * 子弹构造时读到的 ExplosionData 就是旧枪的 explode=false，导致只有第一段命中伤害、没有第二段爆炸。
     * 每次发现主手枪械变化时主动调 postChangeEvent 重建缓存。
     */
    private static final Map<UUID, Identifier> MAID_CACHED_GUN_ID = new WeakHashMap<>();
    /** 一次性诊断：记录已输出过"无弹药换弹诊断"的女仆，避免刷屏。 */
    private static final Set<UUID> LOGGED_NO_AMMO = new HashSet<>();

    /** TACZ 已安装时调用：注入适配器并注册枪手模式。 */
    public static void init() {
        TaczCompat.setAdapter(new TaczGunAdapterImpl());
        ModeManager.INSTANCE.register(
                new Identifier("littlemaidmobresurgence", "gunner"),
                ModeType.<GunnerMode>builder((type, maid) -> new GunnerMode(type, "Gunner", maid))
                        .addItemMatcher(TaczCompat::isGun, ItemMatcher.Priority.LOWER)
                        .build());
    }

    @Override
    public boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof IGun;
    }

    @Override
    public int performGunAttack(LittleMaidEntity shooter, LivingEntity target, ItemStack gunItem) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return 100;
        }
        Identifier gunId = iGun.getGunId(gunItem);
        Optional<CommonGunIndex> optional = TimelessAPI.getCommonGunIndex(gunId);
        if (optional.isEmpty()) {
            return 100;
        }
        CommonGunIndex gunIndex = optional.get();
        GunData gunData = gunIndex.getGunData();

        IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);
        // 女仆换枪后主动重建配件缓存（爆炸/伤害/射速等属性都来自该缓存）
        Identifier cachedGunId = MAID_CACHED_GUN_ID.get(shooter.getUuid());
        if (!gunId.equals(cachedGunId) || gunOperator.getCacheProperty() == null) {
            AttachmentPropertyManager.postChangeEvent(shooter, gunItem);
            MAID_CACHED_GUN_ID.put(shooter.getUuid(), gunId);
        }
        // 让枪口对准目标：水平/垂直夹角换算成 yaw/pitch 再扣扳机
        float pitch = computePitchTo(shooter, target);
        float yaw = computeYawTo(shooter, target);
        ShootResult result = gunOperator.shoot(() -> pitch, () -> yaw);

        // 状态机占用中的结果优先返回等待时长，避免射击/瞄准逻辑打断换弹、拉栓等动作
        int occupiedDelay = occupiedStateDelay(result);
        if (occupiedDelay >= 0) {
            return occupiedDelay;
        }

        if (result == ShootResult.NOT_DRAW) {
            gunOperator.draw(shooter::getMainHandStack);
            return Math.round(gunData.getDrawTime() * 20) + 2;
        }
        if (result == ShootResult.NEED_BOLT) {
            gunOperator.bolt();
            return Math.round(gunData.getBoltActionTime() * 20) + 2;
        }
        if (result == ShootResult.NO_AMMO) {
            logNoAmmoDiagnostic(shooter, gunItem);
            return attemptReload(gunOperator, gunData);
        }
        if (result == ShootResult.OVERHEATED || result == ShootResult.FORGE_EVENT_CANCEL) {
            return 10;
        }

        // 按枪种与距离决定开镜/收镜，返回等待的瞄准耗时（-1 表示无需调整）
        int aimDelay = adjustAiming(gunIndex, gunOperator, shooter, target);
        if (aimDelay >= 0) {
            return aimDelay;
        }

        // 半自动/点射：给射击冷却留时间，避免连续扣扳机吞弹
        FireMode fireMode = iGun.getFireMode(gunItem);
        return (fireMode == FireMode.SEMI || fireMode == FireMode.BURST)
                ? 10 + shooter.getRandom().nextInt(5)
                : 2;
    }

    /** 射击状态机"占用中"的结果 → 等待 tick；非占用结果返回 -1（继续走后续逻辑）。 */
    private static int occupiedStateDelay(ShootResult result) {
        return switch (result) {
            case COOL_DOWN, IS_SPRINTING -> 2;
            case IS_RELOADING -> 10; // 换弹中，等换弹节奏推进
            case IS_DRAWING, IS_BOLTING, IS_MELEE -> 5;
            case ID_NOT_EXIST, NOT_GUN, UNKNOWN_FAIL, NETWORK_FAIL -> 100;
            default -> -1;
        };
    }

    /** 枪口指向目标的垂直俯仰角（眼睛高度差 / 水平距离）。 */
    private static float computePitchTo(LivingEntity shooter, LivingEntity target) {
        double dx = target.getX() - shooter.getX();
        double dy = target.getEyeY() - shooter.getEyeY();
        double dz = target.getZ() - shooter.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horizontal));
    }

    /** 枪口指向目标的水平偏航角（Minecraft yaw 约定：0=+Z，顺时针为正）。 */
    private static float computeYawTo(LivingEntity shooter, LivingEntity target) {
        double dx = target.getX() - shooter.getX();
        double dz = target.getZ() - shooter.getZ();
        return (float) -Math.toDegrees(Math.atan2(dx, dz));
    }

    /** 无弹药：触发换弹并返回等待时长；若换弹未真正启动则短等后重试。 */
    private static int attemptReload(IGunOperator gunOperator, GunData gunData) {
        gunOperator.reload();
        ReloadState.StateType state = gunOperator.getDataHolder().reloadStateType;
        if (state.isReloading()) {
            float emptyTime = gunData.getReloadData().getCooldown().getEmptyTime();
            return Math.round(emptyTime * 20) + 2;
        }
        return 5;
    }

    /**
     * 一次性诊断：同时输出【女仆背包直接扫描】与【TACZ 同款 ITEM_HANDLER 能力扫描】的结果，
     * 用于确认检索链路：背包数据是否就位、能力是否暴露全部槽位。
     */
    private static void logNoAmmoDiagnostic(LittleMaidEntity maid, ItemStack gunItem) {
        if (!LOGGED_NO_AMMO.add(maid.getUuid())) {
            return;
        }
        Inventory inv = maid.getInventory();
        boolean inInv = false;
        for (int i = 0; i < inv.size(); i++) {
            if (isAmmoOfGun(inv.getStack(i), gunItem)) {
                inInv = true;
                break;
            }
        }
        int capSlots = -1;
        boolean inCap = false;
        String capHandlerType = "无";
        var cap = maid.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        if (cap != null && cap.isPresent()) {
            IItemHandler handler = cap.resolve().orElse(null);
            if (handler != null) {
                capHandlerType = handler.getClass().getSimpleName();
                capSlots = handler.getSlots();
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (isAmmoOfGun(handler.getStackInSlot(i), gunItem)) {
                        inCap = true;
                        break;
                    }
                }
            }
        }
        LMMRMod.LOGGER.info(
                "[LMMR] 枪手换弹诊断：背包槽数={}，背包内找到弹药={}，能力处理器={}，能力槽数={}，能力内找到弹药={}",
                inv.size(),
                inInv,
                capHandlerType,
                capSlots,
                inCap);
    }

    /** 开镜决策：狙击枪总是开镜；其余枪种在超出瞄准距离时开镜、近距收镜。 */
    private static int adjustAiming(
            CommonGunIndex gunIndex,
            IGunOperator gunOperator,
            LittleMaidEntity shooter,
            LivingEntity target) {
        boolean isSniper =
                GunTabType.SNIPER.name().toLowerCase(Locale.ENGLISH).equals(gunIndex.getType());
        boolean aiming = gunOperator.getSynIsAiming();
        if (isSniper) {
            if (!aiming) {
                gunOperator.aim(true);
                return aimTimeTicks(gunIndex) + 2;
            }
            return -1;
        }
        float distance = shooter.distanceTo(target);
        if (distance <= AIM_DISTANCE && aiming) {
            gunOperator.aim(false);
            return aimTimeTicks(gunIndex) + 2;
        }
        if (distance > AIM_DISTANCE && !aiming) {
            gunOperator.aim(true);
            return aimTimeTicks(gunIndex) + 2;
        }
        return -1;
    }

    /** 读取该枪的开镜耗时（tick）。 */
    private static int aimTimeTicks(CommonGunIndex gunIndex) {
        return Math.round(gunIndex.getGunData().getAimTime() * 20);
    }

    /** 判断物品是否为该枪的弹药或弹药盒。 */
    private static boolean isAmmoOfGun(ItemStack stack, ItemStack gunItem) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunItem, stack)) {
            return true;
        }
        return stack.getItem() instanceof IAmmoBox ammoBox
                && ammoBox.isAmmoBoxOfGun(gunItem, stack);
    }

    @Override
    public void stopAim(LittleMaidEntity maid) {
        ItemStack mainHandItem = maid.getMainHandStack();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            return;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getCommonGunIndex(gunId)
                .ifPresent(
                        gunIndex -> {
                            IGunOperator gunOperator = IGunOperator.fromLivingEntity(maid);
                            if (gunOperator.getSynIsAiming()) {
                                gunOperator.aim(false);
                            }
                        });
    }
}
