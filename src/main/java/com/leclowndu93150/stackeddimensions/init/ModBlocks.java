package com.leclowndu93150.stackeddimensions.init;

import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import com.leclowndu93150.stackeddimensions.block.PortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Stackeddimensions.MODID);
    
    public static final RegistryObject<Block> PORTAL_BLOCK = BLOCKS.register("portal_block", PortalBlock::new);
}
