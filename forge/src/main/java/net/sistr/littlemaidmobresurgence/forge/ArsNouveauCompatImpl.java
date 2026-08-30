package net.sistr.littlemaidmobresurgence.forge;

import com.hollingsworth.arsnouveau.api.spell.ISpellCaster;
import com.hollingsworth.arsnouveau.api.spell.ISpellCasterProvider;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidmobresurgence.api.mode.ItemMatcher;
import net.sistr.littlemaidmobresurgence.api.mode.ModeManager;
import net.sistr.littlemaidmobresurgence.api.mode.ModeType;
import net.sistr.littlemaidmobresurgence.compat.ArsNouveauCompat;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.entity.mode.ArsNouveauMode;

/**
 * 新生魔艺（Ars Nouveau 1.20.1-4.1.0）魔法书适配实现 + 新生魔艺模式注册（仅当该模组已安装时加载）。
 *
 * <p>从魔法书经 {@link ISpellCasterProvider#getSpellCaster} 收集槽位法术 → 随机选择 → 施放。
 * 弹射物法术手动创建 {@link EntityProjectileSpell} 精确射向目标；非弹射物按 目标/射线 选择
 * onCastOnEntity / onCastOnBlock / onCast。施法前为女仆补满法力并提高上限实现无限魔力
 * （该版本 enoughMana 为包私有不可覆写）。
 *
 * <p>冷却由模式攻击间隔（返回值）控制，不设法术级冷却：每次调用都实际施法并返回固定
 * 4 tick，保证女仆每次攻击都能正常释放魔法。
 */
public class ArsNouveauCompatImpl implements ArsNouveauCompat.ArsCasterAdapter {

    /** Ars Nouveau 已安装时调用：注入适配器并注册新生魔艺模式。 */
    public static void init() {
        ArsNouveauCompat.setAdapter(new ArsNouveauCompatImpl());
        ModeManager.INSTANCE.register(
                new Identifier("littlemaidmobresurgence", "ars_nouveau"),
                ModeType.<ArsNouveauMode>builder(
                                (type, maid) -> new ArsNouveauMode(type, "ArsNouveau", maid))
                        .addItemMatcher(ArsNouveauCompat::isSpellBook, ItemMatcher.Priority.LOWER)
                        .build());
    }

    @Override
    public boolean isSpellBook(ItemStack stack) {
        return stack.getItem() instanceof SpellBook;
    }

    /** 让施法者面向目标（设置 yaw/pitch）。 */
    private static void lookAtTarget(LivingEntity caster, LivingEntity target) {
        Vec3d targetPos = target.getEyePos();
        Vec3d maidPos = caster.getEyePos();
        Vec3d direction = targetPos.subtract(maidPos).normalize();
        float yaw = (float) (Math.atan2(direction.x, direction.z) * 180.0 / Math.PI);
        float pitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);
        caster.setYaw(yaw);
        caster.setPitch(pitch);
    }

    @Override
    public int performSpellAttack(LittleMaidEntity caster, LivingEntity target, ItemStack spellBook) {
        if (!(caster.getWorld() instanceof ServerWorld server)) {
            return 20;
        }
        if (!(spellBook.getItem() instanceof ISpellCasterProvider provider)) {
            return 30;
        }
        ISpellCaster casterIf = provider.getSpellCaster(spellBook);
        if (casterIf == null) {
            return 30;
        }

        // 收集魔法书所有槽位中的有效法术
        List<Spell> spells = new ArrayList<>();
        for (int i = 0; i < casterIf.getMaxSlots(); i++) {
            Spell spell = casterIf.getSpell(i);
            if (spell != null && spell.isValid() && !spell.isEmpty()) {
                spells.add(spell);
            }
        }
        if (spells.isEmpty()) {
            return 40;
        }
        Spell chosen = spells.get(caster.getRandom().nextInt(spells.size()));

        // 女仆拥有无限魔力：大幅提高法力上限并每次补满，保证任意法术均可连续施放
        // （该版本 SpellResolver.enoughMana 为包私有不可覆写，故通过补满法力绕过）
        CapabilityRegistry.getMana(caster)
                .ifPresent(cap -> {
                    if (cap.getMaxMana() < 100000) {
                        cap.setMaxMana(100000);
                    }
                    if (cap.getCurrentMana() < 100000) {
                        cap.setMana(100000);
                    }
                });

        LivingCaster wrappedCaster = new LivingCaster(caster);
        SpellContext context = new SpellContext(server, chosen, caster, wrappedCaster, spellBook);
        SpellResolver resolver = new SpellResolver(context);

        // 先面向目标
        if (target != null) {
            lookAtTarget(caster, target);
        }

        // 弹射物法术：手动创建弹射物精确射向目标（玩家施法同款逻辑，稳定可靠）
        if (chosen.getCastMethod() instanceof MethodProjectile && target != null) {
            try {
                Vec3d targetPos = target.getEyePos();
                Vec3d maidPos = caster.getEyePos();
                Vec3d direction = targetPos.subtract(maidPos).normalize();

                EntityProjectileSpell projectile = new EntityProjectileSpell(server, resolver);
                projectile.setOwner(caster);
                projectile.setPos(maidPos.x, maidPos.y - 0.1, maidPos.z);
                projectile.setVelocity(direction.x, direction.y, direction.z, 0.85F, 0.0F);
                server.spawnEntity(projectile);
            } catch (Exception e) {
                // 回退到标准施法
                resolver.onCastOnEntity(spellBook, target, Hand.MAIN_HAND);
            }
        } else if (target != null) {
            resolver.onCastOnEntity(spellBook, target, Hand.MAIN_HAND);
        } else {
            // 无指定目标：射线追踪后按命中结果施放
            boolean isSensitive = chosen.getBuffsAtIndex(0, caster, AugmentSensitive.INSTANCE) > 0;
            HitResult result = SpellUtil.rayTrace(caster, 5.5, 0, isSensitive);
            if (result instanceof EntityHitResult entityHitResult
                    && entityHitResult.getEntity() instanceof LivingEntity) {
                resolver.onCastOnEntity(spellBook, entityHitResult.getEntity(), Hand.MAIN_HAND);
            } else if (result instanceof BlockHitResult blockHitResult) {
                resolver.onCastOnBlock(blockHitResult);
            } else {
                resolver.onCast(spellBook, server);
            }
        }

        caster.swingHand(Hand.MAIN_HAND);

        // 连续施法：每次调用都实际施放，冷却仅由模式攻击间隔控制（约0.2秒）
        return 4;
    }

    @Override
    public void stopCast(LittleMaidEntity maid) {
        // 无持久化状态需要清理
    }
}
