package net.sistr.littlemaidmobresurgence.entity.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidmobresurgence.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

public interface MaidManager {
    void registerMaid(LittleMaidEntity maid);

    /**
     * [zh] 从管理列表删除一条女仆记录（不作用于世界中的实体/纪念品）。
     * [en] Removes one maid record from the manager list (does not affect the entity or souvenir in the world).
     * [ja] 管理リストからメイド記録を1件削除します（ワールド上の実体・記念品には影響しません）。
     */
    void removeMaid(java.util.UUID uuid);

    /**
     * [zh] 标记女仆已死亡：管理界面仅对已死亡记录显示删除按钮。
     * [en] Marks the maid as dead: the manager GUI only shows the delete button for dead records.
     * [ja] メイドを死亡扱いにします。管理画面では死亡記録のみ削除ボタンを表示します。
     */
    void markMaidDead(LittleMaidEntity maid);

    List<LMInfo> getMaidList();

    void writeMaidManager(NbtCompound nbt);

    void readMaidManager(NbtCompound nbt);

    void checkMaidUnload();

    abstract sealed class LMInfo permits MaidLMInfo {
        protected final UUID id;
        protected final String name;
        protected final Status status;
        protected final BlockPos lastPos;
        protected final String worldId;

        protected LMInfo(UUID id, String name, Status status, BlockPos lastPos, String worldId) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.lastPos = lastPos;
            this.worldId = worldId;
        }

        public UUID id() {
            return id;
        }

        public String name() {
            return name;
        }

        public Status status() {
            return status;
        }

        public void write(NbtCompound infoNbt) {
            infoNbt.putString("name", name);
            infoNbt.putString("status", status.name());
            infoNbt.putUuid("id", id);
            infoNbt.putIntArray(
                    "lastPos", new int[] {lastPos.getX(), lastPos.getY(), lastPos.getZ()});
            infoNbt.putString("worldId", worldId);
            var entityId = getEntityId();
            if (entityId != -1) {
                infoNbt.putInt("entityId", entityId);
            }
        }

        public static LMInfo read(NbtCompound infoNbt) {
            String name = infoNbt.getString("name");
            // 兼容旧存档：旧版 SOUL_ENTITY/SOUL_WITHIN 状态一律降级为 UNLOADED
            Status status =
                    switch (infoNbt.getString("status")) {
                        case "ALIVE" -> Status.ALIVE;
                        case "DEAD" -> Status.DEAD;
                        default -> Status.UNLOADED;
                    };
            UUID id = infoNbt.getUuid("id");
            BlockPos lastPos = BlockPos.ORIGIN;
            if (infoNbt.contains("lastPos")) {
                int[] lastPosArray = infoNbt.getIntArray("lastPos");
                lastPos = new BlockPos(lastPosArray[0], lastPosArray[1], lastPosArray[2]);
            }
            String worldId = infoNbt.getString("worldId");
            int entityId = -1;
            if (infoNbt.contains("entityId")) {
                entityId = infoNbt.getInt("entityId");
            }
            return new MaidLMInfo(id, name, lastPos, worldId, null, entityId, status);
        }

        public Optional<Entity> getEntityClient(World world) {
            var entityId = getEntityId();
            if (entityId == -1) {
                return Optional.empty();
            }
            return Optional.ofNullable(world.getEntityById(entityId));
        }

        public abstract Optional<Entity> getEntity();

        public abstract boolean isLoaded();

        public abstract int getEntityId();

        public BlockPos getLastPos() {
            return lastPos;
        }

        public String getWorldId() {
            return worldId;
        }
    }

    final class MaidLMInfo extends LMInfo {
        private final @Nullable LittleMaidEntity maid;
        private final int entityId;

        private MaidLMInfo(
                UUID id,
                String name,
                BlockPos lastPos,
                String worldId,
                @Nullable LittleMaidEntity maid,
                int entityId,
                Status status) {
            super(id, name, status, lastPos, worldId);
            this.maid = maid;
            this.entityId = entityId;
        }

        public @Nullable LittleMaidEntity maid() {
            return maid;
        }

        public static MaidLMInfo create(LittleMaidEntity maid, boolean loaded) {
            return new MaidLMInfo(
                    maid.getUuid(),
                    maid.getName().getString(),
                    maid.getBlockPos(),
                    maid.getWorld().getRegistryKey().getValue().toString(),
                    loaded ? maid : null,
                    loaded ? maid.getId() : -1,
                    loaded ? Status.ALIVE : Status.UNLOADED);
        }

        /** [zh] 死亡记录：保留最后位置/名字，用于管理界面清理。 */
        public static MaidLMInfo createDead(LittleMaidEntity maid) {
            return new MaidLMInfo(
                    maid.getUuid(),
                    maid.getName().getString(),
                    maid.getBlockPos(),
                    maid.getWorld().getRegistryKey().getValue().toString(),
                    null,
                    -1,
                    Status.DEAD);
        }

        @Override
        public Optional<Entity> getEntity() {
            return Optional.ofNullable(maid);
        }

        @Override
        public boolean isLoaded() {
            return this.maid != null || this.entityId != -1;
        }

        @Override
        public int getEntityId() {
            return this.entityId;
        }
    }

    enum Status {
        ALIVE(Text.literal("Alive").formatted(Formatting.WHITE)), // 生きてる
        DEAD(Text.literal("Dead").formatted(Formatting.RED)), // 死亡
        UNLOADED(Text.literal("Unloaded").formatted(Formatting.GRAY)); // 読み込まれていない/死亡済み

        private final Text text;

        Status(Text text) {
            this.text = text;
        }

        public Text getText() {
            return text;
        }
    }
}
