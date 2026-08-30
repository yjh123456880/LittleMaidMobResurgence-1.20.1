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
                    "ALIVE".equals(infoNbt.getString("status"))
                            ? Status.ALIVE
                            : Status.UNLOADED;
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
            return new MaidLMInfo(id, name, lastPos, worldId, null, entityId);
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
                int entityId) {
            super(id, name, Status.ALIVE, lastPos, worldId);
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
                    loaded ? maid.getId() : -1);
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
