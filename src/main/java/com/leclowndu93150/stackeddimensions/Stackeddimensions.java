package com.leclowndu93150.stackeddimensions;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Stackeddimensions.MODID)
public class Stackeddimensions {

    public static final String MODID = "stackeddimensions";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Stackeddimensions() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, StackedDimensionsConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("StackedDimensions mod initializing...");
        if (StackedDimensionsConfig.enableStackedDimensions) {
            LOGGER.info("Stacked Dimensions enabled!");
            LOGGER.info("Overworld: Y {} to {}", StackedDimensionsConfig.overworldMinY, StackedDimensionsConfig.overworldMaxY);
            LOGGER.info("Nether: Y {} to {}", StackedDimensionsConfig.netherMinY, StackedDimensionsConfig.netherMaxY);
            LOGGER.info("End: Y {} to {}", StackedDimensionsConfig.endMinY, StackedDimensionsConfig.endMaxY);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("StackedDimensions server starting");
    }
}
