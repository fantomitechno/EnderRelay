/*
 * MIT License
 *
 * Copyright (c) 2024 Simon RENOUX aka fantomitechno
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

import java.util.Objects;
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
        if (this.name != null && !this.name.equals(Component.literal("null"))) {
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
        if (blockEntity.name == null || blockEntity.name.equals(Component.literal("null"))) {
            player.displayClientMessage(Component.translatable("block.enderrelay.nowhere"), true);
            return;
        }
        BlockState blockState = world.getBlockState(blockEntity.teleportPlace);
        Block block = blockState.getBlock();
        Optional<Vec3> optional = EnderRelayBlockEntity.findStandUpPosition(EntityType.PLAYER, world, blockEntity.teleportPlace);
        if (!block.equals(Blocks.LODESTONE) || optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("block.enderrelay.obstructed"), true);
            return;
        }
        world.setBlock(pos, state.setValue(EnderRelayBlock.CHARGE, Integer.valueOf(state.getValue(EnderRelayBlock.CHARGE) - 1)), 3);
        if (world.getBlockState(pos).getValue(EnderRelayBlock.CHARGE) == 0) {
            reset(blockEntity);
        }
        player.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 1.0F, 1.0F, world.getRandom().nextLong()));
        Vec3 coords = optional.get();
        player.moveTo(coords.x, coords.y, coords.z, 0.0f, 0.0f);

        while(!world.noCollision(player) && player.getY() < (double)world.getMaxBuildHeight()) {
            player.setPos(player.getX(), player.getY() + 1.0, player.getZ());
        }
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
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

    public static void reset(EnderRelayBlockEntity blockEntity) {
        blockEntity.teleportPlace = new BlockPos(0, 0, 0);
        blockEntity.name = Component.literal("null");
    }

    public Component getName() {
        return name;
    }
}
