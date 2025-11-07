package com.leclowndu93150.stackeddimensions;

import com.leclowndu93150.stackeddimensions.block.PortalBlock;
import com.leclowndu93150.stackeddimensions.config.PortalConfig;
import com.leclowndu93150.stackeddimensions.config.PortalConfigLoader;
import com.leclowndu93150.stackeddimensions.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DimStackManager {
    
    public static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    public static final BlockState NETHERRACK = Blocks.NETHERRACK.defaultBlockState();
    
    private static final Map<String, Map<Long, Set<BlockPos>>> LOADED_DYNAMIC_PORTALS = new ConcurrentHashMap<>();
    private static final Map<String, Map<Long, Integer>> PORTAL_ANIMATION_STATE = new ConcurrentHashMap<>();
    
    private static PortalConfig portalConfig;
    private static int animationTick = 0;
    
    public static void onChunkLoad(ServerLevel world, ChunkAccess chunk) {
        if (portalConfig == null) {
            portalConfig = PortalConfigLoader.load();
        }
        
        String dimKey = world.dimension().location().toString();
        PortalConfig.PortalDefinition portal = portalConfig.getPortalForDimension(dimKey);
        
        if (portal != null && portal.enabled) {
            if (portal.dynamicLoading) {
                return;
            }
            generatePortalInChunk(world, chunk, portal);
        }
    }
    
    private static void generatePortalInChunk(ServerLevel world, ChunkAccess chunk, PortalConfig.PortalDefinition portal) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Random random = new Random(chunk.getPos().toLong());
        boolean isCeiling = portal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING;
        int startY = portal.getPortalStartY();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int layer = 0; layer < portal.portalLayers; layer++) {
                    PortalBlock.PortalLayer portalLayer;
                    if (layer == 0) {
                        portalLayer = PortalBlock.PortalLayer.BOTTOM;
                    } else if (layer == portal.portalLayers - 1) {
                        portalLayer = PortalBlock.PortalLayer.TOP;
                    } else {
                        portalLayer = PortalBlock.PortalLayer.MIDDLE;
                    }
                    
                    BlockState portalState = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                            .setValue(PortalBlock.LAYER, portalLayer)
                            .setValue(PortalBlock.CEILING, isCeiling);
                    
                    int yPos;
                    if (isCeiling) {
                        yPos = startY - layer;
                    } else {
                        yPos = startY + layer;
                    }
                    
                    pos.set(x, yPos, z);
                    
                    if (layer == portal.portalLayers - 1 && random.nextDouble() < 0.3) {
                        chunk.setBlockState(pos, NETHERRACK, false);
                    } else {
                        chunk.setBlockState(pos, portalState, false);
                    }
                }
                
                if (portal.transitionLayerThickness > 0 && !portal.dynamicLoading) {
                    int transitionStart = isCeiling ? 
                        startY - portal.portalLayers : 
                        startY + portal.portalLayers;
                    
                    for (int i = 0; i < portal.transitionLayerThickness; i++) {
                        int yPos = isCeiling ? transitionStart - i : transitionStart + i;
                        pos.set(x, yPos, z);
                        
                        if (random.nextDouble() < 0.5) {
                            chunk.setBlockState(pos, NETHERRACK, false);
                        } else {
                            chunk.setBlockState(pos, DEEPSLATE, false);
                        }
                    }
                }
            }
        }
        
        removeBedrockInChunk(chunk, portal, startY);
    }
    
    private static void removeBedrockInChunk(ChunkAccess chunk, PortalConfig.PortalDefinition portal, int startY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean isCeiling = portal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int layer = 0; layer < portal.bedrockRemovalRange; layer++) {
                    int yPos = isCeiling ? startY - layer : startY + layer;
                    pos.set(x, yPos, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.is(Blocks.BEDROCK)) {
                        chunk.setBlockState(pos, DEEPSLATE, false);
                    }
                }
            }
        }
    }
    
    private static int tickCounter = 0;
    
    public static void onPlayerTick(ServerPlayer player) {
        if (portalConfig == null) {
            portalConfig = PortalConfigLoader.load();
            Stackeddimensions.LOGGER.info("Loaded portal config with {} portals", portalConfig.portals.size());
        }
        
        tickCounter++;
        animationTick++;
        if (tickCounter % 100 == 0) {
            Stackeddimensions.LOGGER.info("Player tick #{} at Y={}", tickCounter, player.getY());
        }
        
        handleDynamicPortalLoading(player);
        
        double feetY = player.getY();
        double headY = feetY + player.getEyeHeight();
        String currentDim = player.level().dimension().location().toString();
        
        for (PortalConfig.PortalDefinition portal : portalConfig.portals) {
            if (!portal.enabled) continue;
            if (!portal.sourceDimension.equals(currentDim)) continue;
            
            boolean shouldTeleport = false;
            if (portal.portalType == PortalConfig.PortalDefinition.PortalType.FLOOR) {
                shouldTeleport = feetY < portal.bedrockYLevel;
            } else {
                shouldTeleport = headY > portal.bedrockYLevel + 1;
            }
            
            if (shouldTeleport) {
                teleportPlayer(player, portal);
                return;
            }
        }
    }
    
    private static void handleDynamicPortalLoading(ServerPlayer player) {
        if (portalConfig == null) return;
        
        String currentDim = player.level().dimension().location().toString();
        
        for (String dimKey : LOADED_DYNAMIC_PORTALS.keySet()) {
            if (!dimKey.equals(currentDim)) {
                cleanupDynamicPortalsForDimension(player.server.getLevel(ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation(dimKey)
                )), dimKey);
            }
        }
        
        for (PortalConfig.PortalDefinition portal : portalConfig.portals) {
            if (!portal.dynamicLoading || !portal.enabled) {
                continue;
            }
            
            String dimKey = portal.sourceDimension;
            if (!currentDim.equals(dimKey)) {
                continue;
            }
            
            double verticalDistance = Math.abs(player.getY() - portal.bedrockYLevel);
            
            if (tickCounter % 100 == 0) {
                Stackeddimensions.LOGGER.info("Processing dynamic portal '{}' | Player Y={} | Portal Y={} | Vertical distance={} | Max distance={}", 
                    portal.name, player.getY(), portal.bedrockYLevel, verticalDistance, portal.dynamicLoadingDistance);
            }
            
            ServerLevel level = (ServerLevel) player.level();
            int chunkX = ((int) player.getX()) >> 4;
            int chunkZ = ((int) player.getZ()) >> 4;
            int loadRadius = (portal.dynamicLoadingDistance >> 4) + 1;
            
            Map<Long, Set<BlockPos>> loadedPortals = LOADED_DYNAMIC_PORTALS.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
            Map<Long, Integer> animationStates = PORTAL_ANIMATION_STATE.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
            Set<Long> chunksToKeep = ConcurrentHashMap.newKeySet();
            
            int generated = 0;
            int skipped = 0;
            
            for (int dx = -loadRadius; dx <= loadRadius; dx++) {
                for (int dz = -loadRadius; dz <= loadRadius; dz++) {
                    int cx = chunkX + dx;
                    int cz = chunkZ + dz;
                    long chunkKey = ((long)cx << 32) | (cz & 0xFFFFFFFFL);
                    
                    if (portal.shouldGeneratePortalAt(player.getX(), player.getY(), player.getZ(), cx, cz)) {
                        chunksToKeep.add(chunkKey);
                        if (!loadedPortals.containsKey(chunkKey)) {
                            if (level.hasChunk(cx, cz)) {
                                if (animationTick % 5 == 0) {
                                    Set<BlockPos> portalBlocks = generateDynamicPortalInChunk(level, level.getChunk(cx, cz), portal);
                                    loadedPortals.put(chunkKey, portalBlocks);
                                    animationStates.put(chunkKey, animationTick);
                                    generated++;
                                    Stackeddimensions.LOGGER.info("Generated dynamic portal at chunk {},{} ({} blocks)", cx, cz, portalBlocks.size());
                                }
                            } else {
                                skipped++;
                            }
                        }
                    }
                }
            }
            
            if (generated > 0 || skipped > 0) {
                Stackeddimensions.LOGGER.info("Portal generation: {} new chunks, {} skipped (not loaded)", generated, skipped);
            }
            
            Set<Long> chunksToRemove = new HashSet<>(loadedPortals.keySet());
            chunksToRemove.removeAll(chunksToKeep);
            
            if (!chunksToRemove.isEmpty() && animationTick % 5 == 0) {
                long chunkToRemove = chunksToRemove.iterator().next();
                Set<BlockPos> portalBlocks = loadedPortals.remove(chunkToRemove);
                animationStates.remove(chunkToRemove);
                
                if (portalBlocks != null) {
                    int removed = 0;
                    for (BlockPos pos : portalBlocks) {
                        if (level.getBlockState(pos).getBlock() instanceof PortalBlock) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            removed++;
                        }
                    }
                    Stackeddimensions.LOGGER.info("Removed {} portal blocks from chunk (animated)", removed);
                }
            }
        }
    }
    
    private static void cleanupDynamicPortalsForDimension(ServerLevel level, String dimKey) {
        if (level == null) return;
        
        Map<Long, Set<BlockPos>> loadedPortals = LOADED_DYNAMIC_PORTALS.get(dimKey);
        Map<Long, Integer> animationStates = PORTAL_ANIMATION_STATE.get(dimKey);
        
        if (loadedPortals == null || loadedPortals.isEmpty()) return;
        
        if (animationTick % 5 == 0 && !loadedPortals.isEmpty()) {
            long chunkKey = loadedPortals.keySet().iterator().next();
            Set<BlockPos> portalBlocks = loadedPortals.remove(chunkKey);
            if (animationStates != null) {
                animationStates.remove(chunkKey);
            }
            
            if (portalBlocks != null) {
                int removed = 0;
                for (BlockPos pos : portalBlocks) {
                    if (level.getBlockState(pos).getBlock() instanceof PortalBlock) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        removed++;
                    }
                }
                Stackeddimensions.LOGGER.info("Cleaned up {} portal blocks from {} (dimension change)", removed, dimKey);
            }
        }
        
        if (loadedPortals.isEmpty()) {
            LOADED_DYNAMIC_PORTALS.remove(dimKey);
            PORTAL_ANIMATION_STATE.remove(dimKey);
        }
    }
    
    private static Set<BlockPos> generateDynamicPortalInChunk(ServerLevel level, ChunkAccess chunk, PortalConfig.PortalDefinition portal) {
        Set<BlockPos> portalBlocks = ConcurrentHashMap.newKeySet();
        Random random = new Random(chunk.getPos().toLong());
        boolean isCeiling = portal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING;
        int startY = portal.getPortalStartY();
        int chunkWorldX = chunk.getPos().getMinBlockX();
        int chunkWorldZ = chunk.getPos().getMinBlockZ();
        
        BlockState transitionBlockState = getTransitionBlock(portal.transitionBlock);
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int layer = 0; layer < portal.portalLayers; layer++) {
                    PortalBlock.PortalLayer portalLayer;
                    if (layer == 0) {
                        portalLayer = PortalBlock.PortalLayer.BOTTOM;
                    } else if (layer == portal.portalLayers - 1) {
                        portalLayer = PortalBlock.PortalLayer.TOP;
                    } else {
                        portalLayer = PortalBlock.PortalLayer.MIDDLE;
                    }
                    
                    BlockState portalState = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                            .setValue(PortalBlock.LAYER, portalLayer)
                            .setValue(PortalBlock.CEILING, isCeiling);
                    
                    int yPos = isCeiling ? startY - layer : startY + layer;
                    BlockPos worldPos = new BlockPos(chunkWorldX + x, yPos, chunkWorldZ + z);
                    
                    if (layer == portal.portalLayers - 1 && random.nextDouble() < 0.3) {
                        level.setBlock(worldPos, transitionBlockState, 3);
                    } else {
                        level.setBlock(worldPos, portalState, 3);
                        portalBlocks.add(worldPos);
                    }
                }
            }
        }
        
        return portalBlocks;
    }
    
    private static BlockState getTransitionBlock(String blockId) {
        try {
            ResourceLocation blockRL = new ResourceLocation(blockId);
            return net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(blockRL).defaultBlockState();
        } catch (Exception e) {
            return NETHERRACK;
        }
    }
    
    private static void teleportPlayer(ServerPlayer player, PortalConfig.PortalDefinition portal) {
        ResourceLocation targetDimRL = new ResourceLocation(portal.targetDimension);
        ResourceKey<Level> targetDimKey = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            targetDimRL
        );
        
        ServerLevel targetLevel = player.server.getLevel(targetDimKey);
        if (targetLevel == null) return;
        
        PortalConfig.PortalDefinition targetPortal = portalConfig.getPortalBySourceAndTarget(
            portal.targetDimension, portal.sourceDimension
        );
        
        Vec3 pos = player.position();
        BlockPos targetPos;
        
        if ("minecraft:the_end".equals(portal.targetDimension)) {
            targetPos = targetLevel.getSharedSpawnPos();
            Stackeddimensions.LOGGER.info("Teleporting to End spawn at {}", targetPos);
        } else {
            double newY;
            if (targetPortal != null) {
                if (targetPortal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING) {
                    newY = targetPortal.bedrockYLevel - 1;
                } else {
                    newY = targetPortal.bedrockYLevel + 1;
                }
            } else {
                newY = targetLevel.getMinBuildHeight() + 64;
            }
            targetPos = new BlockPos((int)pos.x, (int)newY, (int)pos.z);
        }
        
        createSafePlatform(targetLevel, targetPos, targetPortal);
        
        player.teleportTo(targetLevel, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        
        if (portal.portalType == PortalConfig.PortalDefinition.PortalType.FLOOR) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, -1.0, 1.0));
        }
        player.fallDistance = 0;
    }
    
    private static void createSafePlatform(ServerLevel level, BlockPos pos, PortalConfig.PortalDefinition portal) {
        BlockState stateBelow = level.getBlockState(pos.below());
        
        if (stateBelow.getBlock() instanceof PortalBlock) {
            if (!stateBelow.getValue(PortalBlock.LAYER).equals(PortalBlock.PortalLayer.BOTTOM)) {
                return;
            }
            boolean isCeiling = portal != null && portal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING;
            level.setBlock(pos.below(), 
                ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                    .setValue(PortalBlock.LAYER, PortalBlock.PortalLayer.BOTTOM)
                    .setValue(PortalBlock.CEILING, isCeiling), 
                3);
        }
        
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
    }
    
    public static void reloadConfig() {
        PortalConfigLoader.reload();
        portalConfig = null;
        LOADED_DYNAMIC_PORTALS.clear();
    }
}
