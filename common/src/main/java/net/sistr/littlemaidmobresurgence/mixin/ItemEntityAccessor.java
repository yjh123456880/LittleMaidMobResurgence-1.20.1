package net.sistr.littlemaidmobresurgence.mixin;

import java.util.UUID;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Accessor
    UUID getOwner();

    /** 存活时间（tick）：纪念品实体用它来防止 despawn。 */
    @Accessor("itemAge")
    void setItemAge(int itemAge);
}
