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
import dev.renoux.enderrelay.blocks.entity.EnderRelayBlockEntity;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.quiltmc.qsl.block.entity.api.QuiltBlockEntityTypeBuilder;
import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings;
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings;

import static dev.renoux.enderrelay.EnderRelay.metadata;

public class ModRegistries {

    static EnderRelayBlock ENDER_RELAY = new EnderRelayBlock(
            QuiltBlockSettings.of()
                    .strength(50.0F, 1200.0F)
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .lightLevel(blockState -> EnderRelayBlock.getScaledChargeLevel(blockState, 15))
                    .pushReaction(PushReaction.IGNORE));

    public static Item ENDER_RELAY_ITEM;
    public static Block ENDER_RELAY_BLOCK;
    public static BlockEntityType ENDER_RELAY_BLOCK_ENTITY;
    public static void init() {
        initItems();
        initBlocks();
    }

    public static void initItems() {
        ENDER_RELAY_ITEM = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(metadata.id(), "ender_relay"), new BlockItem(ENDER_RELAY, new QuiltItemSettings().stacksTo(64).fireResistant()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register((entries -> entries.addAfter(Items.RESPAWN_ANCHOR, ENDER_RELAY_ITEM)));
    }

    public static void initBlocks() {
        ENDER_RELAY_BLOCK = Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(metadata.id(), "ender_relay"), ENDER_RELAY);

        ENDER_RELAY_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(metadata.id(), "ender_relay"), QuiltBlockEntityTypeBuilder.create(EnderRelayBlockEntity::new, ENDER_RELAY_BLOCK).build());
    }
}
