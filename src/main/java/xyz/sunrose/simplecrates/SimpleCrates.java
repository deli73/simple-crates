package xyz.sunrose.simplecrates;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleCrates implements ModInitializer {
    public static final String MODID = "simplecrates";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final Identifier crate_id = new Identifier(MODID, "crate");

    public static final Block CRATE = Registry.register(
            Registry.BLOCK, crate_id,
            new CrateBlock(FabricBlockSettings.copyOf(Blocks.BARREL))
    );

    public static final Item CRATE_ITEM = Registry.register(
            Registry.ITEM, crate_id,
            new BlockItem(CRATE, new FabricItemSettings().group(ItemGroup.DECORATIONS))
    );

    public static final BlockEntityType<CrateBlockEntity> CRATE_BE = Registry.register(
            Registry.BLOCK_ENTITY_TYPE, crate_id,
            FabricBlockEntityTypeBuilder.create(CrateBlockEntity::new, CRATE).build()
    );


    public static final Tag<Item> BANNED_ITEMS = TagFactory.ITEM.create(new Identifier(MODID, "banned_items"));

    @Override
    public void onInitialize() {

    }
}
