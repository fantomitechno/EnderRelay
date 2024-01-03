package dev.renoux.enderrelay.load;

import dev.renoux.enderrelay.blocks.EnderRelayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DispenserBehavior {
    public static void init() {
        DispenserBlock.registerBehavior(Items.END_CRYSTAL, new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                Direction direction = blockSource.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos blockPos = blockSource.getPos().relative(direction);
                Level level = blockSource.getLevel();
                BlockState blockState = level.getBlockState(blockPos);
                this.setSuccess(true);
                if (blockState.is(ModRegistries.ENDER_RELAY_BLOCK)) {
                    if (blockState.getValue(EnderRelayBlock.CHARGE) != 4) {
                        EnderRelayBlock.charge(null, level, blockPos, blockState);
                        itemStack.shrink(1);
                    } else {
                        this.setSuccess(false);
                    }

                    return itemStack;
                } else {
                    return super.execute(blockSource, itemStack);
                }
            }
        });
    }
}
