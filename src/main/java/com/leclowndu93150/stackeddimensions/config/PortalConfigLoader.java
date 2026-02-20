package com.leclowndu93150.stackeddimensions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

        cachedConfig.buildPortalLinks();
        for (PortalConfig.PortalDefinition portal : cachedConfig.portals) {
            if (portal.linkedArrivalPortal != null) {
                Stackeddimensions.LOGGER.info("Linked portal '{}' -> '{}'", portal.name, portal.linkedArrivalPortal.name);
            } else {
                Stackeddimensions.LOGGER.warn("No linked portal found for '{}'", portal.name);
            }
        }

        saveGuide();
        return cachedConfig;
    }
    
    public static void save(PortalConfig config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            Stackeddimensions.LOGGER.error("Failed to save portal config", e);
        }
        saveGuide();
    }
    
    public static void reload() {
        cachedConfig = null;
        load();
    }
    
    private static void saveGuide() {
        File guideFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "stackeddimensions-guide.txt");
        try (PrintWriter w = new PrintWriter(new FileWriter(guideFile))) {
            w.println("========================================================");
            w.println("  Stacked Dimensions - Configuration Guide");
            w.println("========================================================");
            w.println();
            w.println("HOW THE MOD WORKS");
            w.println("------------------");
            w.println("Stacked Dimensions removes the normal portal system and instead");
            w.println("physically stacks dimensions on top of each other. The Nether is");
            w.println("placed directly below the Overworld (or above, depending on config).");
            w.println("Portal blocks replace the bedrock layer between dimensions, so you");
            w.println("can walk/fall through the boundary to seamlessly travel between them.");
            w.println();
            w.println("When a player touches the portal layer, they are automatically");
            w.println("teleported to a safe position in the target dimension. A 2-second");
            w.println("cooldown prevents rapid back-and-forth teleporting.");
            w.println();
            w.println("Portal blocks can be broken in survival mode, but doing so costs");
            w.println("4.0 food exhaustion per block and also breaks the linked block in");
            w.println("the other dimension. Players with 1 or fewer food points cannot");
            w.println("break portal blocks.");
            w.println();
            w.println("========================================================");
            w.println("  FORGE CONFIG  (stackeddimensions-common.toml)");
            w.println("========================================================");
            w.println();
            w.println("enableStackedDimensions (boolean, default: true)");
            w.println("  Master switch for the mod's world generation. When false,");
            w.println("  no portal layers or bedrock replacement will be generated.");
            w.println();
            w.println("enableTeleportation (boolean, default: true)");
            w.println("  Master switch for automatic player teleportation. When false,");
            w.println("  the portal blocks still generate but walking into them will");
            w.println("  not teleport you.");
            w.println();
            w.println("========================================================");
            w.println("  PORTAL CONFIG  (stackeddimensions-portals.json)");
            w.println("========================================================");
            w.println();
            w.println("The portal config is a JSON file containing a \"portals\" array.");
            w.println("Each entry defines one portal layer between two dimensions.");
            w.println("Portals are paired: you typically need two entries (A->B and B->A).");
            w.println();
            w.println("--- Portal Definition Keys ---");
            w.println();
            w.println("name (string)");
            w.println("  A unique identifier for this portal. Used in logs and linking.");
            w.println("  Example: \"overworld_to_nether\"");
            w.println();
            w.println("source_dimension (string)");
            w.println("  The dimension where this portal layer exists.");
            w.println("  Example: \"minecraft:overworld\", \"minecraft:the_nether\"");
            w.println();
            w.println("target_dimension (string)");
            w.println("  The dimension the player will be teleported to.");
            w.println("  Example: \"minecraft:the_nether\", \"minecraft:the_end\"");
            w.println();
            w.println("bedrock_y_level (integer)");
            w.println("  The Y coordinate where the portal layer begins. This is where");
            w.println("  portal blocks are placed, replacing the bedrock layer.");
            w.println("  For Overworld floor: -64, for Nether ceiling: 127");
            w.println();
            w.println("portal_layers (integer)");
            w.println("  How many blocks thick the portal layer is. A value of 3 means");
            w.println("  3 layers of portal blocks stacked vertically.");
            w.println();
            w.println("portal_type (string: \"floor\" or \"ceiling\")");
            w.println("  Whether this portal is at the floor or ceiling of the dimension.");
            w.println("  - \"floor\": portal blocks extend upward from bedrock_y_level.");
            w.println("    Teleport triggers when the player falls below bedrock_y_level.");
            w.println("  - \"ceiling\": portal blocks extend downward from bedrock_y_level.");
            w.println("    Teleport triggers when the player's head goes above bedrock_y_level + 1.");
            w.println();
            w.println("target_type (string: \"floor\" or \"ceiling\", optional)");
            w.println("  Overrides the expected portal type in the target dimension when");
            w.println("  linking portal pairs. By default, a FLOOR portal expects a CEILING");
            w.println("  arrival and vice versa. Set this to override that logic for");
            w.println("  non-standard stacking arrangements.");
            w.println();
            w.println("bedrock_removal_range (integer, default: 10)");
            w.println("  How many Y levels of bedrock around the portal layer are replaced");
            w.println("  with deepslate. This clears vanilla bedrock so the portal layer");
            w.println("  is accessible and not blocked.");
            w.println();
            w.println("transition_layer_thickness (integer, default: 5)");
            w.println("  Number of extra layers of the transition block placed above or");
            w.println("  below the portal layer. Creates a visual/structural buffer between");
            w.println("  the portal and the normal terrain. Set to 0 to disable.");
            w.println();
            w.println("transition_block (string, default: \"minecraft:netherrack\")");
            w.println("  The block used for transition layers and scattered across the");
            w.println("  outermost portal layer (~30% random replacement). Use any valid");
            w.println("  block ID. Example: \"minecraft:netherrack\", \"minecraft:end_stone\"");
            w.println();
            w.println("dynamic_loading (boolean, default: false)");
            w.println("  When false (default), portal blocks are generated permanently");
            w.println("  during chunk generation (at the SURFACE stage).");
            w.println("  When true, portal blocks are placed/removed at runtime based on");
            w.println("  player proximity. This can reduce world file size but may cause");
            w.println("  brief visual delays as portals load in.");
            w.println();
            w.println("dynamic_loading_distance (integer, default: 128)");
            w.println("  Only used when dynamic_loading is true. The vertical distance");
            w.println("  (in blocks) within which portal blocks are maintained around the");
            w.println("  player. Chunks outside this range have their portal blocks removed.");
            w.println();
            w.println("enabled (boolean, default: true)");
            w.println("  Whether this portal definition is active. Set to false to disable");
            w.println("  a portal without deleting its config entry.");
            w.println();
            w.println("========================================================");
            w.println("  EXAMPLE: DEFAULT CONFIG");
            w.println("========================================================");
            w.println();
            w.println("The default config creates two portals that stack the Nether below");
            w.println("the Overworld:");
            w.println();
            w.println("  1. overworld_to_nether (FLOOR at Y=-64)");
            w.println("     Falling below Y=-64 in the Overworld teleports you to the Nether.");
            w.println("     20 Y levels of bedrock are replaced with deepslate.");
            w.println("     5 layers of netherrack transition above the portal.");
            w.println();
            w.println("  2. nether_to_overworld (CEILING at Y=127)");
            w.println("     Going above Y=128 in the Nether teleports you to the Overworld.");
            w.println("     12 Y levels of bedrock are replaced with deepslate.");
            w.println("     No transition layers (thickness 0).");
            w.println();
            w.println("========================================================");
            w.println("  TIPS");
            w.println("========================================================");
            w.println();
            w.println("- Portals must be defined in pairs (A->B and B->A) for bidirectional");
            w.println("  travel. A one-way portal is possible but the player will have no");
            w.println("  portal return path.");
            w.println("- You can add custom dimension portals by adding new entries with");
            w.println("  modded dimension IDs (e.g., \"mymod:custom_dimension\").");
            w.println("- Use /stackeddimensions reloadconfig to reload the config without");
            w.println("  restarting. Note: this does NOT regenerate already-generated chunks.");
            w.println("- The Dimensional Pipe block can chunk-load across dimensions,");
            w.println("  keeping both sides of a portal area loaded.");
            w.println();
            Stackeddimensions.LOGGER.info("Generated config guide at: " + guideFile.getAbsolutePath());
        } catch (IOException e) {
            Stackeddimensions.LOGGER.error("Failed to write config guide", e);
        }
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
