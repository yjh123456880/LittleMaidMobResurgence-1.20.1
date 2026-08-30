package net.sistr.littlemaidmobresurgence.item;

import dev.architectury.core.item.ArchitecturySpawnEggItem;
import net.minecraft.item.Item;
import net.sistr.littlemaidmobresurgence.setup.Registration;

public class LittleMaidSpawnEggItem extends ArchitecturySpawnEggItem {

    public LittleMaidSpawnEggItem() {
        super(
                Registration.LITTLE_MAID_MOB,
                0xFFFFFF,
                0x804000,
                new Item.Settings().arch$tab(Registration.ITEM_GROUP));
    }
}
