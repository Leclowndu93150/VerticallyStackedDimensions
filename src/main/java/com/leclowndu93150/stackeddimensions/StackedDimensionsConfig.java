package com.leclowndu93150.stackeddimensions;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = Stackeddimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class StackedDimensionsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_STACKED_DIMENSIONS = BUILDER
        .comment("Enable stacked dimensions worldgen")
        .define("enableStackedDimensions", true);

    private static final ForgeConfigSpec.IntValue OVERWORLD_MIN_Y = BUILDER
        .comment("Overworld layer minimum Y coordinate")
        .defineInRange("overworldMinY", -64, -2032, 2016);

    private static final ForgeConfigSpec.IntValue OVERWORLD_MAX_Y = BUILDER
        .comment("Overworld layer maximum Y coordinate")
        .defineInRange("overworldMaxY", 320, -2032, 2016);

    private static final ForgeConfigSpec.IntValue NETHER_MIN_Y = BUILDER
        .comment("Nether layer minimum Y coordinate")
        .defineInRange("netherMinY", 320, -2032, 2016);

    private static final ForgeConfigSpec.IntValue NETHER_MAX_Y = BUILDER
        .comment("Nether layer maximum Y coordinate")
        .defineInRange("netherMaxY", 448, -2032, 2016);

    private static final ForgeConfigSpec.IntValue END_MIN_Y = BUILDER
        .comment("End layer minimum Y coordinate")
        .defineInRange("endMinY", 448, -2032, 2016);

    private static final ForgeConfigSpec.IntValue END_MAX_Y = BUILDER
        .comment("End layer maximum Y coordinate")
        .defineInRange("endMaxY", 576, -2032, 2016);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_ORDER = BUILDER
        .comment("Order of dimensions from bottom to top (overworld, nether, end)")
        .defineListAllowEmpty("dimensionOrder", Arrays.asList("overworld", "nether", "end"), obj -> obj instanceof String);

    private static final ForgeConfigSpec.BooleanValue ENABLE_TELEPORTATION = BUILDER
        .comment("Enable automatic player teleportation between dimension layers")
        .define("enableTeleportation", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableStackedDimensions;
    public static int overworldMinY;
    public static int overworldMaxY;
    public static int netherMinY;
    public static int netherMaxY;
    public static int endMinY;
    public static int endMaxY;
    public static List<? extends String> dimensionOrder;
    public static boolean enableTeleportation;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableStackedDimensions = ENABLE_STACKED_DIMENSIONS.get();
        overworldMinY = OVERWORLD_MIN_Y.get();
        overworldMaxY = OVERWORLD_MAX_Y.get();
        netherMinY = NETHER_MIN_Y.get();
        netherMaxY = NETHER_MAX_Y.get();
        endMinY = END_MIN_Y.get();
        endMaxY = END_MAX_Y.get();
        dimensionOrder = DIMENSION_ORDER.get();
        enableTeleportation = ENABLE_TELEPORTATION.get();
    }
}
