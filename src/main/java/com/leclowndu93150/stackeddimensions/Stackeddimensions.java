package com.leclowndu93150.stackeddimensions;

import com.leclowndu93150.stackeddimensions.init.ModBlocks;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Stackeddimensions.MODID)
public class Stackeddimensions {

    public static final String MODID = "stackeddimensions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Stackeddimensions() {
        ModBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
