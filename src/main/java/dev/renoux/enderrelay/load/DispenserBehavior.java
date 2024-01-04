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
                    if (blockState.getValue(EnderRelayBlock.CHARGE) != EnderRelayBlock.MAX_CHARGES) {
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
