package dev.renoux.enderrelay.blocks.entity;

import com.google.common.collect.ImmutableList;
import dev.renoux.enderrelay.blocks.EnderRelayBlock;
import dev.renoux.enderrelay.load.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class EnderRelayBlockEntity extends BlockEntity {

    private static final ImmutableList<Vec3i> TELEPORT_HORIZONTAL_OFFSETS = ImmutableList.of(
            new Vec3i(0, 0, -1),
            new Vec3i(-1, 0, 0),
            new Vec3i(0, 0, 1),
            new Vec3i(1, 0, 0),
            new Vec3i(-1, 0, -1),
            new Vec3i(1, 0, -1),
            new Vec3i(-1, 0, 1),
            new Vec3i(1, 0, 1)
    );
    private static final ImmutableList<Vec3i> TELEPORT_OFFSETS = new ImmutableList.Builder<Vec3i>()
            .addAll(TELEPORT_HORIZONTAL_OFFSETS)
            .addAll(TELEPORT_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator())
            .addAll(TELEPORT_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator())
            .add(new Vec3i(0, 1, 0))
            .build();
    private BlockPos teleportPlace;
    private Component name;

    public EnderRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    public EnderRelayBlockEntity(BlockPos blockPos, BlockState blockState) {
        this(ModRegistries.ENDER_RELAY_BLOCK_ENTITY, blockPos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        if (this.teleportPlace != null) {
            compoundTag.put("teleportPlace", NbtUtils.writeBlockPos(this.teleportPlace));
            compoundTag.putString("name", Component.Serializer.toJson(this.name));
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        load(NbtUtils.readBlockPos(nbt.getCompound("teleportPlace")), Component.Serializer.fromJson(nbt.getString("name")));
    }

    public void load(BlockPos pos, Component name) {
        this.teleportPlace = pos;
        this.name = name;
    }

    public static void teleportPlayer(Level world, BlockPos pos, BlockState state, ServerPlayer player, EnderRelayBlockEntity blockEntity) {
        if (blockEntity.teleportPlace == null) {
            player.sendSystemMessage(Component.translatable("block.enderrelay.nowhere", true));
        }
        BlockState blockState = world.getBlockState(blockEntity.teleportPlace);
        Block block = blockState.getBlock();
        Optional<Vec3> optional = EnderRelayBlockEntity.findStandUpPosition(EntityType.PLAYER, world, blockEntity.teleportPlace);
        if (!block.equals(Blocks.LODESTONE) || optional.isEmpty()) {
            player.sendSystemMessage(Component.translatable("block.enderrelay.obstructed", true));
            return;
        }
        world.setBlock(pos, state.setValue(EnderRelayBlock.CHARGE, Integer.valueOf(state.getValue(EnderRelayBlock.CHARGE) - 1)), 3);
        player.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 1.0F, 1.0F, world.getRandom().nextLong()));
        if (world.getBlockState(pos).getValue(EnderRelayBlock.CHARGE) == 0) {
            blockEntity.teleportPlace = null;
            blockEntity.name = null;
        }
        Vec3 coords = optional.get();
        player.teleportTo(coords.x, coords.y, coords.z);
    }

    public static Optional<Vec3> findStandUpPosition(EntityType<?> entityType, CollisionGetter collisionGetter, BlockPos blockPos) {
        Optional<Vec3> optional = findStandUpPosition(entityType, collisionGetter, blockPos, true);
        return optional.isPresent() ? optional : findStandUpPosition(entityType, collisionGetter, blockPos, false);
    }

    private static Optional<Vec3> findStandUpPosition(EntityType<?> entityType, CollisionGetter collisionGetter, BlockPos blockPos, boolean bl) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(Vec3i vec3i : TELEPORT_OFFSETS) {
            mutableBlockPos.set(blockPos).move(vec3i);
            Vec3 vec3 = DismountHelper.findSafeDismountLocation(entityType, collisionGetter, mutableBlockPos, bl);
            if (vec3 != null) {
                return Optional.of(vec3);
            }
        }

        return Optional.empty();
    }
}
