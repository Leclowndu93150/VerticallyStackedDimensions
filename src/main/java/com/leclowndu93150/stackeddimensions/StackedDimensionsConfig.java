package com.leclowndu93150.stackeddimensions;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Stackeddimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class StackedDimensionsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_STACKED_DIMENSIONS = BUILDER
        .comment("Enable stacked dimensions worldgen")
        .define("enableStackedDimensions", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_TELEPORTATION = BUILDER
        .comment("Enable automatic player teleportation between dimension layers")
        .define("enableTeleportation", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableStackedDimensions;
    public static boolean enableTeleportation;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableStackedDimensions = ENABLE_STACKED_DIMENSIONS.get();
        enableTeleportation = ENABLE_TELEPORTATION.get();
    }
}
