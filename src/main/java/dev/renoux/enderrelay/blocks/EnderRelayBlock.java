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
package dev.renoux.enderrelay.blocks;

import dev.renoux.enderrelay.blocks.entity.EnderRelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EnderRelayBlock extends BaseEntityBlock {
    public static final int MIN_CHARGES = 0;
    public static final int MAX_CHARGES = 2;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", MIN_CHARGES, MAX_CHARGES);

    public EnderRelayBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, Integer.valueOf(0)));
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && !isTeleportFuel(itemStack) && isTeleportFuel(player.getItemInHand(InteractionHand.OFF_HAND))) {
            return InteractionResult.PASS;
        } else if (isTeleportFuel(itemStack) && canBeCharged(state)) {
            charge(player, world, pos, state);
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        } else if (state.getValue(CHARGE) == 0) {
            return InteractionResult.PASS;
        } else if (!canTeleport(world)) {
            if (!world.isClientSide) {
                this.explode(world, pos);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            if (!world.isClientSide) {
                if (isTeleportCompass(itemStack, world)) {
                    EnderRelayBlockEntity blentity = (EnderRelayBlockEntity) world.getBlockEntity(pos);
                    assert itemStack.getTag() != null;
                    if (!itemStack.getTag().contains("LodestonePos")) {
                        player.sendSystemMessage(Component.translatable("item.minecraft.lodestone_compass.nowhere"));
                        return InteractionResult.PASS;
                    }
                    BlockPos compassPos = NbtUtils.readBlockPos(itemStack.getTag().getCompound("LodestonePos"));
                    Component name = itemStack.getHoverName();
                    if (name.equals(Items.COMPASS.getName(itemStack))) {
                        name = Component.literal(compassPos.getX() + "/" + compassPos.getY() + "/" + compassPos.getZ());
                    }

                    if (name.equals(Component.literal("null"))) {
                        player.sendSystemMessage(Component.translatable("block.enderrelay.not_this_name_please"));
                        return InteractionResult.CONSUME;
                    }
                    player.sendSystemMessage(Component.translatable("block.enderrelay.set_teleport", name));
                    blentity.load(compassPos, name);
                    blentity.setChanged();
                } else {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    EnderRelayBlockEntity blentity = (EnderRelayBlockEntity) world.getBlockEntity(pos);
                    if (serverPlayer.isCrouching()) {
                        if (blentity.getName().equals(Component.literal("null")))
                            serverPlayer.displayClientMessage(Component.translatable("block.enderrelay.teleport_to", blentity.getName()), true);
                        else
                            serverPlayer.displayClientMessage(Component.translatable("block.enderrelay.nowhere"), true);
                    } else
                        EnderRelayBlockEntity.teleportPlayer(world, pos, state, serverPlayer, blentity);
                }
            }

            return InteractionResult.CONSUME;
        }
    }

    public static boolean canTeleport(Level world) {
        return !(world.dimensionType().bedWorks() || world.dimensionType().respawnAnchorWorks());
    }

    private boolean isTeleportFuel(ItemStack itemStack) {
        return itemStack.is(Items.END_CRYSTAL);
    }

    private boolean isTeleportCompass(ItemStack itemStack, Level world) {
        CompoundTag nbt = itemStack.getTag();
        if (nbt == null || !itemStack.is(Items.COMPASS)) return false;
        return CompassItem.getLodestoneDimension(nbt).isPresent() && CompassItem.getLodestoneDimension(nbt).get().equals(world.dimension());
    }

    private static boolean canBeCharged(BlockState blockState) {
        return blockState.getValue(CHARGE) < MAX_CHARGES;
    }

    private static boolean isWaterThatWouldFlow(BlockPos blockPos, Level level) {
        FluidState fluidState = level.getFluidState(blockPos);
        if (!fluidState.is(FluidTags.WATER)) {
            return false;
        } else if (fluidState.isSource()) {
            return true;
        } else {
            float f = (float)fluidState.getAmount();
            if (f < 2.0F) {
                return false;
            } else {
                FluidState fluidState2 = level.getFluidState(blockPos.below());
                return !fluidState2.is(FluidTags.WATER);
            }
        }
    }

    private void explode(Level level, BlockPos blockPos) {
        level.removeBlock(blockPos, false);
        boolean bl = Direction.Plane.HORIZONTAL.stream().map(blockPos::relative).anyMatch(blockPosx -> isWaterThatWouldFlow(blockPosx, level));
        final boolean bl2 = bl || level.getFluidState(blockPos.above()).is(FluidTags.WATER);
        ExplosionDamageCalculator explosionDamageCalculator = new ExplosionDamageCalculator() {
            @Override
            public Optional<Float> getBlockExplosionResistance(
                    Explosion explosion, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, FluidState fluidState
            ) {
                return blockPos.equals(blockPos) && bl2
                        ? Optional.of(Blocks.WATER.getExplosionResistance())
                        : super.getBlockExplosionResistance(explosion, blockGetter, blockPos, blockState, fluidState);
            }
        };
        Vec3 vec3 = blockPos.getCenter();
        level.explode(null, level.damageSources().badRespawnPointExplosion(vec3), explosionDamageCalculator, vec3, 5.0F, false, Level.ExplosionInteraction.BLOCK);
    }

    public static void charge(@Nullable Entity entity, Level level, BlockPos blockPos, BlockState blockState) {
        BlockState blockState2 = blockState.setValue(CHARGE, Integer.valueOf(blockState.getValue(CHARGE) + 1));
        level.setBlock(blockPos, blockState2, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(entity, blockState2));
        level.playSound(
                null,
                (double)blockPos.getX() + 0.5,
                (double)blockPos.getY() + 0.5,
                (double)blockPos.getZ() + 0.5,
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    public static int getScaledChargeLevel(BlockState blockState, int i) {
        return Mth.floor((float)(blockState.getValue(CHARGE) - 0) / MAX_CHARGES * (float)i);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderRelayBlockEntity(pos, state);
    }
}
