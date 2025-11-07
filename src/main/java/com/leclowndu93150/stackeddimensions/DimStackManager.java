package com.leclowndu93150.stackeddimensions;

import com.leclowndu93150.stackeddimensions.block.PortalBlock;
import com.leclowndu93150.stackeddimensions.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class DimStackManager {
    
    public static final int TRANSITION_LAYER_THICKNESS = 5;
    public static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    public static final BlockState NETHERRACK = Blocks.NETHERRACK.defaultBlockState();
    public static final int NETHER_ROOF_Y = 127;
    
    public static void onChunkLoad(ServerLevel world, ChunkAccess chunk) {
        if (!StackedDimensionsConfig.enableStackedDimensions) return;
        
        if (world.dimension() == Level.OVERWORLD) {
            addOverworldTransitionLayer(chunk);
            removeOverworldBedrock(chunk);
        } else if (world.dimension() == Level.NETHER) {
            addNetherTransitionLayer(chunk);
            removeNetherBedrock(chunk);
        }
    }
    
    private static void addOverworldTransitionLayer(ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Random random = new Random(chunk.getPos().toLong());
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockState portalBottom = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                        .setValue(PortalBlock.LAYER, PortalBlock.PortalLayer.BOTTOM)
                        .setValue(PortalBlock.CEILING, false);
                BlockState portalMiddle = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                        .setValue(PortalBlock.LAYER, PortalBlock.PortalLayer.MIDDLE)
                        .setValue(PortalBlock.CEILING, false);
                BlockState portalTop = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                        .setValue(PortalBlock.LAYER, PortalBlock.PortalLayer.TOP)
                        .setValue(PortalBlock.CEILING, false);
                
                pos.set(x, minY, z);
                chunk.setBlockState(pos, portalBottom, false);
                
                pos.set(x, minY + 1, z);
                chunk.setBlockState(pos, portalMiddle, false);
                
                pos.set(x, minY + 2, z);
                if (random.nextDouble() < 0.7) {
                    chunk.setBlockState(pos, portalTop, false);
                } else {
                    chunk.setBlockState(pos, NETHERRACK, false);
                }
                
                for (int y = minY + 3; y <= minY + TRANSITION_LAYER_THICKNESS + 2; y++) {
                    pos.set(x, y, z);
                    if (Math.random() < 0.5) {
                        chunk.setBlockState(pos, NETHERRACK, false);
                    } else {
                        chunk.setBlockState(pos, DEEPSLATE, false);
                    }
                }
            }
        }
    }
    
    private static void addNetherTransitionLayer(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Random random = new Random(chunk.getPos().toLong());
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockState portalBottom = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                        .setValue(PortalBlock.LAYER, PortalBlock.PortalLayer.BOTTOM)
                        .setValue(PortalBlock.CEILING, true);
                BlockState portalMiddle = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                        .setValue(PortalBlock.LAYER, PortalBlock.PortalLayer.MIDDLE)
                        .setValue(PortalBlock.CEILING, true);
                BlockState portalTop = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                        .setValue(PortalBlock.LAYER, PortalBlock.PortalLayer.TOP)
                        .setValue(PortalBlock.CEILING, true);
                
                pos.set(x, NETHER_ROOF_Y, z);
                chunk.setBlockState(pos, portalBottom, false);
                
                pos.set(x, NETHER_ROOF_Y - 1, z);
                chunk.setBlockState(pos, portalMiddle, false);
                
                pos.set(x, NETHER_ROOF_Y - 2, z);
                if (random.nextDouble() < 0.7) {
                    chunk.setBlockState(pos, portalTop, false);
                } else {
                    chunk.setBlockState(pos, NETHERRACK, false);
                }
            }
        }
    }
    
    
    private static void removeOverworldBedrock(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY + TRANSITION_LAYER_THICKNESS * 2; y < minY + 20; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.is(Blocks.BEDROCK)) {
                        chunk.setBlockState(pos, DEEPSLATE, false);
                    }
                }
            }
        }
    }
    
    private static void removeNetherBedrock(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = NETHER_ROOF_Y - 7; y <= NETHER_ROOF_Y + 5; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.is(Blocks.BEDROCK)) {
                        chunk.setBlockState(pos, DEEPSLATE, false);
                    }
                }
            }
        }
    }
    
    public static void onPlayerTick(ServerPlayer player) {
        if (!StackedDimensionsConfig.enableStackedDimensions) return;
        if (!StackedDimensionsConfig.enableTeleportation) return;
        
        double feetY = player.getY();
        double headY = feetY + player.getEyeHeight();
        ResourceKey<Level> currentDim = player.level().dimension();
        
        if (currentDim == Level.OVERWORLD) {
            int minY = player.level().getMinBuildHeight();
            if (feetY < minY) {
                teleportToNether(player);
            }
        } else if (currentDim == Level.NETHER) {
            if (headY > 128) {
                teleportToOverworld(player);
            }
        }
    }
    
    private static void teleportToNether(ServerPlayer player) {
        ServerLevel nether = player.server.getLevel(Level.NETHER);
        if (nether == null) return;
        
        Vec3 pos = player.position();
        double newY = 128 - 6;
        BlockPos targetPos = new BlockPos((int)pos.x, (int)newY, (int)pos.z);
        
        if (!isSafeSpot(nether, targetPos)) {
            targetPos = findSafeSpot(nether, targetPos, 32);
            if (targetPos == null) {
                return;
            }
        }
        
        nether.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
        nether.setBlock(targetPos.above(), Blocks.AIR.defaultBlockState(), 3);
        
        player.teleportTo(nether, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }
    
    private static void teleportToOverworld(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        
        Vec3 pos = player.position();
        int minY = overworld.getMinBuildHeight();
        double newY = minY + 11;
        
        BlockPos targetPos = new BlockPos((int)pos.x, (int)newY, (int)pos.z);
        
        if (!isSafeSpot(overworld, targetPos)) {
            targetPos = findSafeSpot(overworld, targetPos, 32);
            if (targetPos == null) {
                return;
            }
        }
        
        overworld.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
        overworld.setBlock(targetPos.above(), Blocks.AIR.defaultBlockState(), 3);
        
        player.teleportTo(overworld, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }
    
    private static boolean isSafeSpot(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isSolid();
    }
    
    private static BlockPos findSafeSpot(ServerLevel level, BlockPos origin, int radius) {
        BlockPos bestDigSpot = null;
        
        for (int distance = 1; distance <= radius; distance++) {
            for (int xOff = -distance; xOff <= distance; xOff++) {
                for (int zOff = -distance; zOff <= distance; zOff++) {
                    if (Math.abs(xOff) != distance && Math.abs(zOff) != distance) continue;
                    
                    BlockPos checkPos = origin.offset(xOff, 0, zOff);
                    BlockState blockAt = level.getBlockState(checkPos);
                    BlockState blockAbove = level.getBlockState(checkPos.above());
                    BlockState blockBelow = level.getBlockState(checkPos.below());
                    
                    if (blockBelow.isSolid() && !blockAt.isSolid() && !blockAbove.isSolid()) {
                        return checkPos;
                    }
                    
                    if (bestDigSpot == null && blockAt.isSolid() && blockAbove.isSolid() && blockBelow.isSolid()) {
                        bestDigSpot = checkPos;
                    }
                }
            }
        }
        
        if (bestDigSpot != null) {
            level.setBlock(bestDigSpot, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(bestDigSpot.above(), Blocks.AIR.defaultBlockState(), 3);
            return bestDigSpot;
        }
        
        return null;
    }
}
