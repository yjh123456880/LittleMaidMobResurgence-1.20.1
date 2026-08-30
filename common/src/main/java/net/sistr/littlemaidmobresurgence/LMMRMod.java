package net.sistr.littlemaidmobresurgence;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraft.entity.LivingEntity;
import net.sistr.littlemaidmobresurgence.advancement.criterion.LMMRCriteria;
import net.sistr.littlemaidmobresurgence.config.LMMRConfig;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import net.sistr.littlemaidmobresurgence.setup.Registration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LMMRMod {
    public static final String MODID = "littlemaidmobresurgence";
    public static final Logger LOGGER = LogManager.getLogger();
    private static ConfigHolder<LMMRConfig> CONFIG_HOLDER;

    public static void init() {
        AutoConfig.register(LMMRConfig.class, Toml4jConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(LMMRConfig.class);

        Registration.init();
        registerAttribute();

        LMMRCriteria.init();
    }

    public static void registerAttribute() {
        EntityAttributeRegistry.register(
                Registration.LITTLE_MAID_MOB, LittleMaidEntity::createLittleMaidAttributes);
        EntityAttributeRegistry.register(
                Registration.REBELLION_PROXY_ENTITY, LivingEntity::createLivingAttributes);
    }

    public static LMMRConfig getConfig() {
        return CONFIG_HOLDER.getConfig();
    }
}
