package net.sistr.littlemaidmobresurgence.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.TameAnimalCriterion;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmobresurgence.LMMRMod;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

public class ContractMaidCriterion extends TameAnimalCriterion {
    private static final Identifier ID = new Identifier(LMMRMod.MODID, "contract_maid");

    @Override
    public Identifier getId() {
        return ID;
    }

    public void trigger(ServerPlayerEntity player, LittleMaidEntity entity) {
        super.trigger(player, entity);
    }

    @Override
    public Conditions conditionsFromJson(
            JsonObject jsonObject,
            LootContextPredicate lootContextPredicate,
            AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        var lootContextPredicate2 =
                EntityPredicate.contextPredicateFromJson(
                        jsonObject, "entity", advancementEntityPredicateDeserializer);
        return new CMConditions(lootContextPredicate, lootContextPredicate2);
    }

    public static class CMConditions extends Conditions {

        public CMConditions(LootContextPredicate player, LootContextPredicate entity) {
            super(player, entity);
        }

        @Override
        public Identifier getId() {
            return ID;
        }
    }
}
