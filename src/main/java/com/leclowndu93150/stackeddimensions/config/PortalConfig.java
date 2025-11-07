package com.leclowndu93150.stackeddimensions.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PortalConfig {
    
    @SerializedName("portals")
    public List<PortalDefinition> portals;
    
    public static class PortalDefinition {
        @SerializedName("name")
        public String name;
        
        @SerializedName("source_dimension")
        public String sourceDimension;
        
        @SerializedName("target_dimension")
        public String targetDimension;
        
        @SerializedName("bedrock_y_level")
        public int bedrockYLevel;
        
        @SerializedName("portal_layers")
        public int portalLayers;
        
        @SerializedName("portal_type")
        public PortalType portalType;
        
        @SerializedName("bedrock_removal_range")
        public int bedrockRemovalRange = 10;
        
        @SerializedName("transition_layer_thickness")
        public int transitionLayerThickness = 5;
        
        @SerializedName("dynamic_loading")
        public boolean dynamicLoading = false;
        
        @SerializedName("dynamic_loading_distance")
        public int dynamicLoadingDistance = 128;
        
        @SerializedName("enabled")
        public boolean enabled = true;
        
        @SerializedName("transition_block")
        public String transitionBlock = "minecraft:netherrack";
        
        public enum PortalType {
            @SerializedName("floor")
            FLOOR,
            
            @SerializedName("ceiling")
            CEILING
        }
        
        public int getPortalStartY() {
            if (portalType == PortalType.FLOOR) {
                return bedrockYLevel;
            } else {
                return bedrockYLevel;
            }
        }
        
        public int getTeleportTargetY(int sourceY, int portalLayer) {
            if (portalType == PortalType.FLOOR) {
                return bedrockYLevel + portalLayer;
            } else {
                return bedrockYLevel - portalLayer;
            }
        }
        
        public boolean shouldGeneratePortalAt(double playerX, double playerY, double playerZ, int chunkX, int chunkZ) {
            if (!dynamicLoading) {
                return true;
            }
            
            double verticalDistance = Math.abs(playerY - bedrockYLevel);
            return verticalDistance <= dynamicLoadingDistance;
        }
    }
    
    public PortalDefinition getPortalForDimension(String dimension) {
        if (portals == null) return null;
        return portals.stream()
                .filter(p -> p.enabled && p.sourceDimension.equals(dimension))
                .findFirst()
                .orElse(null);
    }
    
    public PortalDefinition getPortalBySourceAndTarget(String sourceDim, String targetDim) {
        if (portals == null) return null;
        return portals.stream()
                .filter(p -> p.enabled && 
                           p.sourceDimension.equals(sourceDim) && 
                           p.targetDimension.equals(targetDim))
                .findFirst()
                .orElse(null);
    }
}
