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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DimStackManager {
    
    public static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    public static final BlockState NETHERRACK = Blocks.NETHERRACK.defaultBlockState();
    
    private static final Map<String, Map<Long, Set<BlockPos>>> LOADED_DYNAMIC_PORTALS = new ConcurrentHashMap<>();
    private static final Map<String, Map<Long, Integer>> PORTAL_ANIMATION_STATE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TELEPORT_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long TELEPORT_COOLDOWN_TICKS = 40; // 2 seconds

    private static PortalConfig portalConfig;
    private static int animationTick = 0;
    
    public static void onChunkLoad(ServerLevel world, ChunkAccess chunk) {
        if (portalConfig == null) {
            portalConfig = PortalConfigLoader.load();
        }

        String dimKey = world.dimension().location().toString();
        List<PortalConfig.PortalDefinition> portals = portalConfig.getPortalsForDimension(dimKey);

        for (PortalConfig.PortalDefinition portal : portals) {
            if (portal.dynamicLoading) {
                continue;
            }
            generatePortalInChunk(world, chunk, portal);
        }
    }
    
    private static void generatePortalInChunk(ServerLevel world, ChunkAccess chunk, PortalConfig.PortalDefinition portal) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long seed = world.getSeed() ^ chunk.getPos().toLong();
        Random random = new Random(seed);
        boolean isCeiling = portal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING;
        int startY = portal.getPortalStartY();
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
                    
                    int yPos;
                    if (isCeiling) {
                        yPos = startY - layer;
                    } else {
                        yPos = startY + layer;
                    }
                    
                    pos.set(x, yPos, z);
                    
                    if (layer == portal.portalLayers - 1 && random.nextDouble() < 0.3) {
                        chunk.setBlockState(pos, transitionBlockState, false);
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
                        chunk.setBlockState(pos, transitionBlockState, false);
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

        handleDynamicPortalLoading(player);

        UUID playerId = player.getUUID();
        long currentTick = player.level().getGameTime();
        Long lastTeleportTick = TELEPORT_COOLDOWNS.get(playerId);
        if (lastTeleportTick != null && (currentTick - lastTeleportTick) < TELEPORT_COOLDOWN_TICKS) {
            double feetY = player.getY();
            double headY = feetY + player.getEyeHeight();
            String currentDim = player.level().dimension().location().toString();
            ServerLevel level = (ServerLevel) player.level();

            for (PortalConfig.PortalDefinition portal : portalConfig.portals) {
                if (!portal.enabled || !portal.sourceDimension.equals(currentDim)) continue;

                BlockState transitionBlock = getTransitionBlock(portal.transitionBlock);

                if (portal.portalType == PortalConfig.PortalDefinition.PortalType.FLOOR) {
                    // Floor portal triggers when feetY < bedrockYLevel
                    if (feetY < portal.bedrockYLevel) {
                        int safeY = portal.bedrockYLevel;
                        BlockPos safePos = createSafeSpot(level, (int) player.getX(), (int) player.getZ(), safeY, transitionBlock);
                        player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                        player.setDeltaMovement(0, 0.1, 0);
                        player.fallDistance = 0;
                        break;
                    }
                } else {
                    // Ceiling portal triggers when headY > bedrockYLevel + 1
                    if (headY > portal.bedrockYLevel + 1) {
                        int safeY = portal.bedrockYLevel - 2;
                        BlockPos safePos = createSafeSpot(level, (int) player.getX(), (int) player.getZ(), safeY, transitionBlock);
                        player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                        player.setDeltaMovement(0, -0.1, 0);
                        player.fallDistance = 0;
                        break;
                    }
                }
            }
            return;
        }
        
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
        long seed = level.getSeed() ^ chunk.getPos().toLong();
        Random random = new Random(seed);
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

    private static BlockPos findSafeSpawnPosition(ServerLevel level, int playerX, int playerZ, int portalY, boolean searchDownward) {
        int searchRadius = 16;
        int maxVerticalDistance = 3;

        BlockPos found = searchForSafeSpot(level, playerX, playerZ, portalY, searchRadius, maxVerticalDistance, searchDownward);
        if (found != null) {
            return found;
        }

        int[][] chunkOffsets = {{16, 0}, {-16, 0}, {0, 16}, {0, -16}, {16, 16}, {16, -16}, {-16, 16}, {-16, -16}};
        for (int[] offset : chunkOffsets) {
            int searchX = playerX + offset[0];
            int searchZ = playerZ + offset[1];
            found = searchForSafeSpot(level, searchX, searchZ, portalY, 8, maxVerticalDistance, searchDownward);
            if (found != null) {
                return found;
            }
        }

        int spawnY = searchDownward ? portalY - 3 : portalY + 1;
        return createSafeSpot(level, playerX, playerZ, spawnY);
    }

    private static BlockPos searchForSafeSpot(ServerLevel level, int centerX, int centerZ, int startY, int radius, int verticalRange, boolean searchDownward) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    int x = centerX + dx;
                    int z = centerZ + dz;

                    BlockPos safe = scanVerticallyForSafeSpot(level, x, z, startY, verticalRange, searchDownward);
                    if (safe != null) {
                        return safe;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos scanVerticallyForSafeSpot(ServerLevel level, int x, int z, int portalY, int range, boolean searchDownward) {
        for (int dy = 0; dy < range; dy++) {
            int y = searchDownward ? portalY - 2 - dy : portalY + 1 + dy;
            if (y < level.getMinBuildHeight() + 1 || y > level.getMaxBuildHeight() - 2) continue;

            BlockPos groundPos = new BlockPos(x, y - 1, z);
            BlockPos feetPos = new BlockPos(x, y, z);
            BlockPos headPos = feetPos.above();

            if (searchDownward && headPos.getY() >= portalY) continue;
            if (!searchDownward && feetPos.getY() <= portalY) continue;

            if (isSafeSpot(level, groundPos, feetPos, headPos)) {
                return feetPos;
            }
        }
        return null;
    }

    private static boolean isSafeSpot(ServerLevel level, BlockPos ground, BlockPos feet, BlockPos head) {
        BlockState groundState = level.getBlockState(ground);
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);

        boolean solidGround = groundState.isSolid() && !groundState.is(Blocks.BEDROCK);
        boolean feetClear = !feetState.isSolid();
        boolean headClear = !headState.isSolid();

        return solidGround && feetClear && headClear;
    }

    private static BlockPos createSafeSpot(ServerLevel level, int x, int z, int y) {
        return createSafeSpot(level, x, z, y, Blocks.STONE.defaultBlockState());
    }

    private static BlockPos createSafeSpot(ServerLevel level, int x, int z, int y, BlockState groundBlock) {
        BlockPos feetPos = new BlockPos(x, y, z);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos groundPos = new BlockPos(x + dx, y - 1, z + dz);
                level.setBlock(groundPos, groundBlock, 3);
            }
        }
        level.setBlock(feetPos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(feetPos.above(), Blocks.AIR.defaultBlockState(), 3);

        return feetPos;
    }

    private static void teleportPlayer(ServerPlayer player, PortalConfig.PortalDefinition portal) {
        TELEPORT_COOLDOWNS.put(player.getUUID(), player.level().getGameTime());

        ResourceLocation targetDimRL = new ResourceLocation(portal.targetDimension);
        ResourceKey<Level> targetDimKey = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            targetDimRL
        );

        ServerLevel targetLevel = player.server.getLevel(targetDimKey);
        if (targetLevel == null) return;

        PortalConfig.PortalDefinition arrivalPortal = portal.linkedArrivalPortal;

        Vec3 pos = player.position();
        BlockPos targetPos;

        if ("minecraft:the_end".equals(portal.targetDimension)) {
            targetPos = targetLevel.getSharedSpawnPos();
        } else {
            int baseY;
            boolean searchDownward;

            if (arrivalPortal != null) {
                baseY = arrivalPortal.bedrockYLevel;
                searchDownward = (arrivalPortal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING);
            } else {
                Stackeddimensions.LOGGER.warn("No linked arrival portal for '{}', using fallback Y", portal.name);
                searchDownward = false;
                baseY = targetLevel.getMinBuildHeight() + 64;
            }

            targetPos = findSafeSpawnPosition(targetLevel, (int)pos.x, (int)pos.z, baseY, searchDownward);
            targetPos = validateAgainstOtherPortals(targetLevel, targetPos, portal.targetDimension, arrivalPortal);
        }

        player.teleportTo(targetLevel, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());

        if (portal.portalType == PortalConfig.PortalDefinition.PortalType.FLOOR) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, -1.0, 1.0));
        }
        player.fallDistance = 0;
    }

    private static BlockPos validateAgainstOtherPortals(ServerLevel level, BlockPos pos, String targetDim, PortalConfig.PortalDefinition arrivalPortal) {
        java.util.List<PortalConfig.PortalDefinition> allPortals = portalConfig.getPortalsForDimension(targetDim);

        for (PortalConfig.PortalDefinition otherPortal : allPortals) {
            if (otherPortal == arrivalPortal) continue;

            if (otherPortal.portalType == PortalConfig.PortalDefinition.PortalType.FLOOR && pos.getY() < otherPortal.bedrockYLevel) {
                BlockState transitionBlock = getTransitionBlock(otherPortal.transitionBlock);
                return createSafeSpot(level, pos.getX(), pos.getZ(), otherPortal.bedrockYLevel + 1, transitionBlock);
            }
            if (otherPortal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING && pos.getY() > otherPortal.bedrockYLevel) {
                BlockState transitionBlock = getTransitionBlock(otherPortal.transitionBlock);
                return createSafeSpot(level, pos.getX(), pos.getZ(), otherPortal.bedrockYLevel - 3, transitionBlock);
            }
        }
        return pos;
    }
    
    public static void reloadConfig() {
        PortalConfigLoader.reload();
        portalConfig = null;
        LOADED_DYNAMIC_PORTALS.clear();
    }
}
