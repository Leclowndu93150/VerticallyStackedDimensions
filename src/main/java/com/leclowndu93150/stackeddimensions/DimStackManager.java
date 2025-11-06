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
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int bedrockTop = minY;
                for (int y = minY; y < minY + 10; y++) {
                    pos.set(x, y, z);
                    if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
                        bedrockTop = y;
                        chunk.setBlockState(pos, NETHERRACK, false);
                    }
                }
                
                for (int y = bedrockTop + 1; y <= bedrockTop + TRANSITION_LAYER_THICKNESS; y++) {
                    pos.set(x, y, z);
                    chunk.setBlockState(pos, NETHERRACK, false);
                }
                
                for (int y = bedrockTop + TRANSITION_LAYER_THICKNESS + 1; y <= bedrockTop + TRANSITION_LAYER_THICKNESS + 2; y++) {
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
        
        double y = player.getY();
        ResourceKey<Level> currentDim = player.level().dimension();
        
        if (currentDim == Level.OVERWORLD) {
            int minY = player.level().getMinBuildHeight();
            if (y < minY + 9) {
                teleportToNether(player);
            }
        } else if (currentDim == Level.NETHER) {
            if (y > 128 - 4) {
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
        
        nether.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
        nether.setBlock(targetPos.above(), Blocks.AIR.defaultBlockState(), 3);
        
        player.teleportTo(nether, pos.x, newY, pos.z, player.getYRot(), player.getXRot());
    }
    
    private static void teleportToOverworld(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        
        Vec3 pos = player.position();
        int minY = overworld.getMinBuildHeight();
        double newY = minY + 11;
        
        BlockPos targetPos = new BlockPos((int)pos.x, (int)newY, (int)pos.z);
        overworld.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
        overworld.setBlock(targetPos.above(), Blocks.AIR.defaultBlockState(), 3);
        
        player.teleportTo(overworld, pos.x, newY, pos.z, player.getYRot(), player.getXRot());
    }
}
