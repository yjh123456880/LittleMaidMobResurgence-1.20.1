package net.sistr.littlemaidmobresurgence.entity.util;

import net.minecraft.entity.Entity;

/** 開いているGUIのエンティティを取得するインターフェイス */
public interface GuiEntitySupplier<T extends Entity> {

    T getGuiEntity();
}
