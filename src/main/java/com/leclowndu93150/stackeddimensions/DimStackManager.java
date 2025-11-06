package com.leclowndu93150.stackeddimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

public class DimStackManager {
    
    public static final int TRANSITION_LAYER_THICKNESS = 5;
    public static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    public static final BlockState NETHERRACK = Blocks.NETHERRACK.defaultBlockState();
    
    private static Integer cachedNetherRoofY = null;
    
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
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < minY + TRANSITION_LAYER_THICKNESS * 2; y++) {
                    pos.set(x, y, z);
                    
                    if (y < minY + TRANSITION_LAYER_THICKNESS) {
                        chunk.setBlockState(pos, NETHERRACK, false);
                    } else {
                        chunk.setBlockState(pos, DEEPSLATE, false);
                    }
                }
                
                pos.set(x, minY - 5, z);
                chunk.setBlockState(pos, Blocks.BEDROCK.defaultBlockState(), false);
            }
        }
    }
    
    private static void addNetherTransitionLayer(ChunkAccess chunk) {
        if (cachedNetherRoofY == null) {
            cachedNetherRoofY = findNetherRoofY(chunk);
        }
        
        int roofY = cachedNetherRoofY;
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = roofY + 1; y <= roofY + TRANSITION_LAYER_THICKNESS * 2; y++) {
                    pos.set(x, y, z);
                    
                    if (y > roofY + TRANSITION_LAYER_THICKNESS) {
                        chunk.setBlockState(pos, DEEPSLATE, false);
                    } else {
                        chunk.setBlockState(pos, NETHERRACK, false);
                    }
                }
                
                for (int y = roofY - 3; y <= roofY; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                }
                
                pos.set(x, roofY + TRANSITION_LAYER_THICKNESS * 2 + 5, z);
                chunk.setBlockState(pos, Blocks.BEDROCK.defaultBlockState(), false);
            }
        }
    }
    
    private static int findNetherRoofY(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    pos.set(x, y, z);
                    if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
                        return y;
                    }
                }
            }
        }
        
        return 127;
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
                        chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                    }
                }
            }
        }
    }
    
    private static void removeNetherBedrock(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int maxY = chunk.getMaxBuildHeight() - 1;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = maxY - 20; y < maxY - TRANSITION_LAYER_THICKNESS * 2; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.is(Blocks.BEDROCK)) {
                        chunk.setBlockState(pos, Blocks.NETHERRACK.defaultBlockState(), false);
                    }
                }
            }
        }
    }
    
    public static void onPlayerTick(ServerPlayer player) {
        if (!StackedDimensionsConfig.enableStackedDimensions) return;
        if (!StackedDimensionsConfig.enableTeleportation) return;
        
        double y = player.getY();
        ResourceKey<Level> currentDim = player.level().dimension();
        
        if (currentDim == Level.OVERWORLD && y < StackedDimensionsConfig.overworldMinY + 1) {
            teleportToNether(player);
        } else if (currentDim == Level.NETHER) {
            if (cachedNetherRoofY != null && y > cachedNetherRoofY + TRANSITION_LAYER_THICKNESS) {
                teleportToOverworld(player);
            }
        }
    }
    
    private static void teleportToNether(ServerPlayer player) {
        ServerLevel nether = player.server.getLevel(Level.NETHER);
        if (nether == null) return;
        
        Vec3 pos = player.position();
        if (cachedNetherRoofY == null) {
            cachedNetherRoofY = 127;
        }
        double newY = cachedNetherRoofY - 2;
        
        player.teleportTo(nether, pos.x, newY, pos.z, player.getYRot(), player.getXRot());
    }
    
    private static void teleportToOverworld(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        
        Vec3 pos = player.position();
        double newY = StackedDimensionsConfig.overworldMinY + 2;
        
        player.teleportTo(overworld, pos.x, newY, pos.z, player.getYRot(), player.getXRot());
    }
}
