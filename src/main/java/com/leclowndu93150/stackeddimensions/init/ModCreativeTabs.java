package com.leclowndu93150.stackeddimensions.init;

import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Stackeddimensions.MODID);

    public static final RegistryObject<CreativeModeTab> STACKED_DIMENSIONS_TAB = CREATIVE_TABS.register("stackeddimensions_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.stackeddimensions"))
                    .icon(() -> new ItemStack(ModItems.DIMENSIONAL_PIPE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.DIMENSIONAL_PIPE.get());
                    })
                    .build()
    );
}
