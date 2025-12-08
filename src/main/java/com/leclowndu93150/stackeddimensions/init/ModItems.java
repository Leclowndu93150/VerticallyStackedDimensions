package com.leclowndu93150.stackeddimensions.init;

import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import com.leclowndu93150.stackeddimensions.item.DimensionalPipeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Stackeddimensions.MODID);

    public static final RegistryObject<Item> DIMENSIONAL_PIPE =
            ITEMS.register("dimensional_pipe", () -> new DimensionalPipeItem(new Item.Properties()));
}
