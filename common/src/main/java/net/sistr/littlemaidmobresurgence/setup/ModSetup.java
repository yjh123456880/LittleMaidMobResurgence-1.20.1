package net.sistr.littlemaidmobresurgence.setup;

import dev.architectury.registry.level.biome.BiomeModifications;
import java.util.List;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.api.mode.Modes;
import net.sistr.littlemaidmobresurgence.network.Networking;

public class ModSetup {

    public static void init() {
        Networking.INSTANCE.init();

        if (LMMRMod.getConfig().spawn.canNaturalSpawn) {
            registerSpawnSettingLM();
        }

        Modes.init();
    }

    private static void registerSpawnSettingLM() {
        // todo メイドさんのスポーン設定容易化
        var spawnBiomeTags =
                LMMRMod.getConfig().spawn.maidSpawnBiomeTags.stream()
                        .filter(Identifier::isValid)
                        .map(Identifier::new)
                        .map(id -> TagKey.of(RegistryKeys.BIOME, id))
                        .toList();
        var spawnExcludeBiomeTags =
                LMMRMod.getConfig().spawn.maidSpawnExcludeBiomeTags.stream()
                        .filter(Identifier::isValid)
                        .map(Identifier::new)
                        .map(id -> TagKey.of(RegistryKeys.BIOME, id))
                        .toList();
        BiomeModifications.addProperties(
                (context) -> canSpawnBiome(context, spawnBiomeTags, spawnExcludeBiomeTags),
                (context, mutable) ->
                        mutable.getSpawnProperties()
                                .addSpawn(
                                        Registration.LITTLE_MAID_MOB.get().getSpawnGroup(),
                                        new SpawnSettings.SpawnEntry(
                                                Registration.LITTLE_MAID_MOB.get(),
                                                LMMRMod.getConfig().spawn.spawnWeight,
                                                LMMRMod.getConfig().spawn.minSpawnGroupSize,
                                                LMMRMod.getConfig().spawn.maxSpawnGroupSize)));
    }

    private static boolean canSpawnBiome(
            BiomeModifications.BiomeContext context,
            List<TagKey<Biome>> spawnBiomeTags,
            List<TagKey<Biome>> spawnExcludeBiomeTags) {
        for (TagKey<Biome> biomeTag : spawnBiomeTags) {
            if (context.hasTag(biomeTag)) {
                for (TagKey<Biome> excludeBiomeTag : spawnExcludeBiomeTags) {
                    if (context.hasTag(excludeBiomeTag)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
