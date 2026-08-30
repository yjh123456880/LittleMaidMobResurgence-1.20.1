package net.sistr.littlemaidmobresurgence.advancement.criterion;

import net.minecraft.advancement.criterion.Criteria;

public class LMMRCriteria {
    public static final ContractMaidCriterion CONTRACT_MAID =
            Criteria.register(new ContractMaidCriterion());

    public static void init() {}
}
