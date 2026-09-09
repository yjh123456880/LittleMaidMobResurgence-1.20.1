package net.sistr.littlemaidmobresurgence.entity.util;

import java.util.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;

public class MaidManagerImpl implements MaidManager {
    private final Map<UUID, LMInfo> maidMap = new HashMap<>();

    @Override
    public void registerMaid(LittleMaidEntity maid) {
        maidMap.put(maid.getUuid(), MaidLMInfo.create(maid, true));
    }

    @Override
    public void removeMaid(UUID uuid) {
        maidMap.remove(uuid);
    }

    @Override
    public void markMaidDead(LittleMaidEntity maid) {
        maidMap.put(maid.getUuid(), MaidLMInfo.createDead(maid));
    }

    @Override
    public List<LMInfo> getMaidList() {
        return List.copyOf(maidMap.values());
    }

    @Override
    public void writeMaidManager(NbtCompound nbt) {
        write(nbt, this.maidMap.values().stream().toList());
    }

    @Override
    public void readMaidManager(NbtCompound nbt) {
        this.maidMap.clear();
        var list = new ArrayList<LMInfo>();
        read(nbt, list);
        list.forEach(lminfo -> maidMap.put(lminfo.id(), lminfo));
    }

    public static void write(NbtCompound nbt, List<LMInfo> list) {
        var listNbt = new NbtList();
        for (LMInfo info : list) {
            NbtCompound infoNbt = new NbtCompound();
            info.write(infoNbt);
            listNbt.add(infoNbt);
        }
        nbt.put("maidList", listNbt);
    }

    public static void read(NbtCompound nbt, List<LMInfo> list) {
        var listNbt = nbt.getList("maidList", NbtElement.COMPOUND_TYPE);
        for (var element : listNbt) {
            NbtCompound infoNbt = (NbtCompound) element;
            LMInfo info = LMInfo.read(infoNbt);
            list.add(info);
        }
    }

    @Override
    public void checkMaidUnload() {
        Map<UUID, LMInfo> updates = new HashMap<>();

        this.maidMap.values().stream()
                .filter(lmInfo -> lmInfo.status() == Status.ALIVE)
                .map(info -> info.getEntity())
                .filter(Optional::isPresent)
                .forEach(
                        o -> {
                            var entity = o.get();
                            // エンティティが死亡 or ワールドが読み込まれていない
                            if (entity instanceof LittleMaidEntity maid
                                    && (!maid.isAlive()
                                            || maid.getServer()
                                                            .getWorld(
                                                                    maid.getWorld()
                                                                            .getRegistryKey())
                                                    == null)) {
                                // 死亡 → DEAD（可删除记录）；仅维度未加载 → UNLOADED（不可删除）
                                updates.put(
                                        maid.getUuid(),
                                        maid.isAlive()
                                                ? MaidLMInfo.create(maid, false)
                                                : MaidLMInfo.createDead(maid));
                            }
                        });

        // ストリーム処理が完了してから一括で更新
        this.maidMap.putAll(updates);
    }
}
