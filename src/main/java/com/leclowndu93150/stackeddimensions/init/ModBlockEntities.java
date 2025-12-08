package com.leclowndu93150.stackeddimensions.init;

import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import com.leclowndu93150.stackeddimensions.block.DimensionalPipeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Stackeddimensions.MODID);

    public static final RegistryObject<BlockEntityType<DimensionalPipeBlockEntity>> DIMENSIONAL_PIPE =
            BLOCK_ENTITIES.register("dimensional_pipe",
                    () -> BlockEntityType.Builder.of(DimensionalPipeBlockEntity::new,
                            ModBlocks.DIMENSIONAL_PIPE.get()).build(null));
}
