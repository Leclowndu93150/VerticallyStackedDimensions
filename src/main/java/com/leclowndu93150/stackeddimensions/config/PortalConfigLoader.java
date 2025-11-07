package com.leclowndu93150.stackeddimensions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class PortalConfigLoader {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "stackeddimensions-portals.json");
    
    private static PortalConfig cachedConfig;
    
    public static PortalConfig load() {
        if (cachedConfig != null) {
            return cachedConfig;
        }
        
        if (!CONFIG_FILE.exists()) {
            cachedConfig = createDefaultConfig();
            save(cachedConfig);
            Stackeddimensions.LOGGER.info("Created default portal config at: " + CONFIG_FILE.getAbsolutePath());
        } else {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                cachedConfig = GSON.fromJson(reader, PortalConfig.class);
                Stackeddimensions.LOGGER.info("Loaded portal config from: " + CONFIG_FILE.getAbsolutePath());
            } catch (IOException e) {
                Stackeddimensions.LOGGER.error("Failed to load portal config, using defaults", e);
                cachedConfig = createDefaultConfig();
            }
        }
        
        return cachedConfig;
    }
    
    public static void save(PortalConfig config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            Stackeddimensions.LOGGER.error("Failed to save portal config", e);
        }
    }
    
    public static void reload() {
        cachedConfig = null;
        load();
    }
    
    private static PortalConfig createDefaultConfig() {
        PortalConfig config = new PortalConfig();
        config.portals = new ArrayList<>();
        
        PortalConfig.PortalDefinition overworldToNether = new PortalConfig.PortalDefinition();
        overworldToNether.name = "overworld_to_nether";
        overworldToNether.sourceDimension = "minecraft:overworld";
        overworldToNether.targetDimension = "minecraft:the_nether";
        overworldToNether.bedrockYLevel = -64;
        overworldToNether.portalLayers = 3;
        overworldToNether.portalType = PortalConfig.PortalDefinition.PortalType.FLOOR;
        overworldToNether.bedrockRemovalRange = 20;
        overworldToNether.transitionLayerThickness = 5;
        overworldToNether.transitionBlock = "minecraft:netherrack";
        overworldToNether.dynamicLoading = false;
        overworldToNether.dynamicLoadingDistance = 128;
        overworldToNether.enabled = true;
        config.portals.add(overworldToNether);
        
        PortalConfig.PortalDefinition netherToOverworld = new PortalConfig.PortalDefinition();
        netherToOverworld.name = "nether_to_overworld";
        netherToOverworld.sourceDimension = "minecraft:the_nether";
        netherToOverworld.targetDimension = "minecraft:overworld";
        netherToOverworld.bedrockYLevel = 127;
        netherToOverworld.portalLayers = 3;
        netherToOverworld.portalType = PortalConfig.PortalDefinition.PortalType.CEILING;
        netherToOverworld.bedrockRemovalRange = 12;
        netherToOverworld.transitionLayerThickness = 0;
        netherToOverworld.transitionBlock = "minecraft:netherrack";
        netherToOverworld.dynamicLoading = false;
        netherToOverworld.dynamicLoadingDistance = 128;
        netherToOverworld.enabled = true;
        config.portals.add(netherToOverworld);
        
        return config;
    }
}
