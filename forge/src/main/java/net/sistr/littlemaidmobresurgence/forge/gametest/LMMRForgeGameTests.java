package net.sistr.littlemaidmobresurgence.forge.gametest;

import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.gametest.LMMRCommonTests;

@GameTestHolder(LMMRMod.MODID)
@PrefixGameTestTemplate(false)
public class LMMRForgeGameTests {

    private static final String SMALL_FLOOR = "small_floor";
    private static final String FLOOR = "floor";

    // ===== 基本 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void maidSpawn(TestContext context) {
        LMMRCommonTests.maidSpawn(context);
    }

    // ===== C: 雇用・再雇用 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void contractWithCake(TestContext context) {
        LMMRCommonTests.contractWithCake(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void recontractFromStrike(TestContext context) {
        LMMRCommonTests.recontractFromStrike(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void cannotContractWithNonEmployItem(TestContext context) {
        LMMRCommonTests.cannotContractWithNonEmployItem(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nonOwnerCannotInteract(TestContext context) {
        LMMRCommonTests.nonOwnerCannotInteract(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void sneakingSkipsInteraction(TestContext context) {
        LMMRCommonTests.sneakingSkipsInteraction(context);
    }

    // ===== W: 待機切替 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void sugarTogglesWaitOn(TestContext context) {
        LMMRCommonTests.sugarTogglesWaitOn(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void sugarTogglesWaitOff(TestContext context) {
        LMMRCommonTests.sugarTogglesWaitOff(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void sugarHeals(TestContext context) {
        LMMRCommonTests.sugarHeals(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void strikeBlocksSugar(TestContext context) {
        LMMRCommonTests.strikeBlocksSugar(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nonOwnerCannotUseSugar(TestContext context) {
        LMMRCommonTests.nonOwnerCannotUseSugar(context);
    }

    // ===== F: isFriend =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void ownerIsFriend(TestContext context) {
        LMMRCommonTests.ownerIsFriend(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void sameOwnerTamedMobIsFriend(TestContext context) {
        LMMRCommonTests.sameOwnerTamedMobIsFriend(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void anyTamedMobIsFriend(TestContext context) {
        LMMRCommonTests.anyTamedMobIsFriend(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void anyPlayerIsFriendWhenTamed(TestContext context) {
        LMMRCommonTests.anyPlayerIsFriendWhenTamed(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void wildMobIsNotFriend(TestContext context) {
        LMMRCommonTests.wildMobIsNotFriend(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void playerNotFriendWhenWild(TestContext context) {
        LMMRCommonTests.playerNotFriendWhenWild(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void wildMobNotFriendWhenWild(TestContext context) {
        LMMRCommonTests.wildMobNotFriendWhenWild(context);
    }

    // ===== T: canTarget =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void cannotTargetOwner(TestContext context) {
        LMMRCommonTests.cannotTargetOwner(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void cannotTargetSameOwnerTamedMob(TestContext context) {
        LMMRCommonTests.cannotTargetSameOwnerTamedMob(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void cannotTargetAnyTamedMob(TestContext context) {
        LMMRCommonTests.cannotTargetAnyTamedMob(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void canTargetWildHostileMob(TestContext context) {
        LMMRCommonTests.canTargetWildHostileMob(context);
    }

    // ===== P2: FakePlayer ワールド登録検証 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void fakePlayerInWorldPlayers(TestContext context) {
        LMMRCommonTests.fakePlayerInWorldPlayers(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void getTameOwnerReturnsPlayer(TestContext context) {
        LMMRCommonTests.getTameOwnerReturnsPlayer(context);
    }

    // ===== D: damage チェック =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void friendDamageBlockedByDefault(TestContext context) {
        LMMRCommonTests.friendDamageBlockedByDefault(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void friendDamageAllowedWithConfig(TestContext context) {
        LMMRCommonTests.friendDamageAllowedWithConfig(context);
    }

    // ===== FEN: Fencer モード =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void swordActivatesFencerMode(TestContext context) {
        LMMRCommonTests.swordActivatesFencerMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void axeActivatesFencerMode(TestContext context) {
        LMMRCommonTests.axeActivatesFencerMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void bowDoesNotActivateFencer(TestContext context) {
        LMMRCommonTests.bowDoesNotActivateFencer(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void fencerTryAttackDamagesTarget(TestContext context) {
        LMMRCommonTests.fencerTryAttackDamagesTarget(context);
    }

    // ===== DMG: ダメージ処理 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void normalDamageFromMob(TestContext context) {
        LMMRCommonTests.normalDamageFromMob(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void immortalBlocksDamage(TestContext context) {
        LMMRCommonTests.immortalBlocksDamage(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void fallImmunityBlocksFallDamage(TestContext context) {
        LMMRCommonTests.fallImmunityBlocksFallDamage(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nonMobDamageImmunityBlocksNonMobDamage(TestContext context) {
        LMMRCommonTests.nonMobDamageImmunityBlocksNonMobDamage(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void damageWhileWaitingCancelsWait(TestContext context) {
        LMMRCommonTests.damageWhileWaitingCancelsWait(context);
    }

    // ===== SOUL: 死亡・魂生成 =====

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void tamedMaidDeathCreatesSoul(TestContext context) {
        LMMRCommonTests.tamedMaidDeathCreatesSoul(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void wildMaidDeathDoesNotCreateSoul(TestContext context) {
        LMMRCommonTests.wildMaidDeathDoesNotCreateSoul(context);
    }

    // ===== NBT: 読み書き =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesTameState(TestContext context) {
        LMMRCommonTests.nbtPreservesTameState(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesWaitState(TestContext context) {
        LMMRCommonTests.nbtPreservesWaitState(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesMovingMode(TestContext context) {
        LMMRCommonTests.nbtPreservesMovingMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesStrike(TestContext context) {
        LMMRCommonTests.nbtPreservesStrike(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesBloodSuck(TestContext context) {
        LMMRCommonTests.nbtPreservesBloodSuck(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesInventory(TestContext context) {
        LMMRCommonTests.nbtPreservesInventory(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesExperience(TestContext context) {
        LMMRCommonTests.nbtPreservesExperience(context);
    }

    // ===== ESC: 追従 Goal =====

    @GameTest(templateName = FLOOR)
    public static void followGoalStartsWhenFar(TestContext context) {
        LMMRCommonTests.followGoalStartsWhenFar(context);
    }

    @GameTest(templateName = FLOOR)
    public static void followGoalDoesNotStartWhenClose(TestContext context) {
        LMMRCommonTests.followGoalDoesNotStartWhenClose(context);
    }

    @GameTest(templateName = FLOOR)
    public static void followGoalDoesNotStartWhenWaiting(TestContext context) {
        LMMRCommonTests.followGoalDoesNotStartWhenWaiting(context);
    }

    @GameTest(templateName = FLOOR)
    public static void followGoalDoesNotStartInFreedom(TestContext context) {
        LMMRCommonTests.followGoalDoesNotStartInFreedom(context);
    }

    // ===== TP: テレポート Goal =====

    @GameTest(templateName = FLOOR)
    public static void teleportGoalStartsWhenFar(TestContext context) {
        LMMRCommonTests.teleportGoalStartsWhenFar(context);
    }

    @GameTest(templateName = FLOOR)
    public static void teleportGoalDoesNotStartWhenClose(TestContext context) {
        LMMRCommonTests.teleportGoalDoesNotStartWhenClose(context);
    }

    @GameTest(templateName = FLOOR)
    public static void teleportGoalDoesNotStartInFreedom(TestContext context) {
        LMMRCommonTests.teleportGoalDoesNotStartInFreedom(context);
    }

    @GameTest(templateName = FLOOR, tickLimit = 200)
    public static void teleportMovesToOwner(TestContext context) {
        LMMRCommonTests.teleportMovesToOwner(context);
    }

    // ===== FEN 追加: shouldExecute =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void fencerShouldExecuteWithTarget(TestContext context) {
        LMMRCommonTests.fencerShouldExecuteWithTarget(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void fencerShouldNotExecuteWithoutTarget(TestContext context) {
        LMMRCommonTests.fencerShouldNotExecuteWithoutTarget(context);
    }

    // ===== D 追加: ATTACK_PROHIBITED =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void attackProhibitedDamageAllowedByDefault(TestContext context) {
        LMMRCommonTests.attackProhibitedDamageAllowedByDefault(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void attackProhibitedDamageBlockedWithConfig(TestContext context) {
        LMMRCommonTests.attackProhibitedDamageBlockedWithConfig(context);
    }

    // ===== SOUL 追加 =====

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void soulPreservesOwnerUuid(TestContext context) {
        LMMRCommonTests.soulPreservesOwnerUuid(context);
    }

    // ===== MOV: Freedom / Tracer 切替 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void featherEscortToFreedom(TestContext context) {
        LMMRCommonTests.featherEscortToFreedom(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void featherFreedomToEscort(TestContext context) {
        LMMRCommonTests.featherFreedomToEscort(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void redstoneFreedomToTracer(TestContext context) {
        LMMRCommonTests.redstoneFreedomToTracer(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void redstoneTracerToFreedom(TestContext context) {
        LMMRCommonTests.redstoneTracerToFreedom(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void redstoneIgnoredInEscort(TestContext context) {
        LMMRCommonTests.redstoneIgnoredInEscort(context);
    }

    // ===== RIP: Ripper モード =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void shearsActivatesRipperMode(TestContext context) {
        LMMRCommonTests.shearsActivatesRipperMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void stoneDoesNotActivateRipper(TestContext context) {
        LMMRCommonTests.stoneDoesNotActivateRipper(context);
    }

    // ===== HEAL: Healer モード =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void foodActivatesHealerMode(TestContext context) {
        LMMRCommonTests.foodActivatesHealerMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void potionActivatesHealerMode(TestContext context) {
        LMMRCommonTests.potionActivatesHealerMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void swordDoesNotActivateHealer(TestContext context) {
        LMMRCommonTests.swordDoesNotActivateHealer(context);
    }

    // ===== PICK: ドロップアイテム拾い =====

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void tamedMaidPicksUpItem(TestContext context) {
        LMMRCommonTests.tamedMaidPicksUpItem(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void wildMaidDoesNotPickUpItem(TestContext context) {
        LMMRCommonTests.wildMaidDoesNotPickUpItem(context);
    }

    // ===== SAL: お給料消費・ストライキ =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void salaryConsumedFromInventory(TestContext context) {
        LMMRCommonTests.salaryConsumedFromInventory(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void unpaidTimesIncreasesWithoutSalary(TestContext context) {
        LMMRCommonTests.unpaidTimesIncreasesWithoutSalary(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void strikeOnExceedingUnpaidLimit(TestContext context) {
        LMMRCommonTests.strikeOnExceedingUnpaidLimit(context);
    }

    // ===== COOK: Cooking モード判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void bowlActivatesCookingMode(TestContext context) {
        LMMRCommonTests.bowlActivatesCookingMode(context);
    }

    // ===== PHARM: Pharmacist モード判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void waterBottleActivatesPharmacistMode(TestContext context) {
        LMMRCommonTests.waterBottleActivatesPharmacistMode(context);
    }

    // ===== TORCH: Torcher モード判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void torchActivatesTorcherMode(TestContext context) {
        LMMRCommonTests.torchActivatesTorcherMode(context);
    }

    // ===== ARCH: Archer モード判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void bowActivatesArcherMode(TestContext context) {
        LMMRCommonTests.bowActivatesArcherMode(context);
    }

    // ===== INT: インタラクション各種 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void saddleStartsRiding(TestContext context) {
        LMMRCommonTests.saddleStartsRiding(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void glassBottleConvertsToExpBottle(TestContext context) {
        LMMRCommonTests.glassBottleConvertsToExpBottle(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void gunpowderSetsAcceleration(TestContext context) {
        LMMRCommonTests.gunpowderSetsAcceleration(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void bucketConvertedToMilk(TestContext context) {
        LMMRCommonTests.bucketConvertedToMilk(context);
    }

    // ===== MISC: その他 =====

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void deathDropsInventory(TestContext context) {
        LMMRCommonTests.deathDropsInventory(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void validNaturalSpawnCondition(TestContext context) {
        LMMRCommonTests.validNaturalSpawnCondition(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void invalidNaturalSpawnNoSolidBlock(TestContext context) {
        LMMRCommonTests.invalidNaturalSpawnNoSolidBlock(context);
    }

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void maidPicksUpExperienceOrb(TestContext context) {
        LMMRCommonTests.maidPicksUpExperienceOrb(context);
    }

    // ===== COOK: かまど作業 =====

    @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
    public static void cookingInsertItems(TestContext context) {
        LMMRCommonTests.cookingInsertItems(context);
    }

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 300, maxAttempts = 3)
    public static void cookingSmeltAndExtract(TestContext context) {
        LMMRCommonTests.cookingSmeltAndExtract(context);
    }

    // ===== PHARM: 醸造台作業 =====

    @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
    public static void pharmacistInsertItems(TestContext context) {
        LMMRCommonTests.pharmacistInsertItems(context);
    }

    // ===== STORE: チェストに格納 =====

    @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
    public static void storeItemToChest(TestContext context) {
        LMMRCommonTests.storeItemToChest(context);
    }

    // ===== ARCH: 射撃 =====

    private static final String ARCHER_ARENA = "archer_arena";

    @GameTest(templateName = ARCHER_ARENA, maxAttempts = 3)
    public static void archerShootsDamagesTarget(TestContext context) {
        LMMRCommonTests.archerShootsDamagesTarget(context);
    }
}
