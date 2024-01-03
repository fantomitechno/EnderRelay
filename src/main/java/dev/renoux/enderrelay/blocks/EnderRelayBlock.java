package dev.renoux.enderrelay.blocks;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class EnderRelayBlock extends Block {
    public static final int MIN_CHARGES = 0;
    public static final int MAX_CHARGES = 2;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", MIN_CHARGES, MAX_CHARGES);

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
    private static final ImmutableList<Vec3i> RESPAWN_OFFSETS = new ImmutableList.Builder<Vec3i>()
            .addAll(TELEPORT_HORIZONTAL_OFFSETS)
            .addAll(TELEPORT_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator())
            .addAll(TELEPORT_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator())
            .add(new Vec3i(0, 1, 0))
            .build();

    public EnderRelayBlock(Properties properties) {
        super(properties);
    }

    public static int getScaledChargeLevel(BlockState blockState, int i) {
    return Mth.floor((float)(blockState.getValue(CHARGE) - 0) / MAX_CHARGES * (float)i);
    }
}
