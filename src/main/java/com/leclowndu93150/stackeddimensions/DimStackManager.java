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
        double newY = 126;
        BlockPos targetPos = new BlockPos((int)pos.x, (int)newY, (int)pos.z);
        
        createSafePlatform(nether, targetPos);
        
        player.teleportTo(nether, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, -1.0, 1.0));
        player.fallDistance = 0;
    }
    
    private static void teleportToOverworld(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        
        Vec3 pos = player.position();
        int minY = overworld.getMinBuildHeight();
        double newY = minY + 1;
        
        BlockPos targetPos = new BlockPos((int)pos.x, (int)newY, (int)pos.z);
        
        createSafePlatform(overworld, targetPos);
        
        player.teleportTo(overworld, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }
    
    private static void createSafePlatform(ServerLevel level, BlockPos pos) {
        BlockState stateBelow = level.getBlockState(pos.below());
        
        if (stateBelow.getBlock() instanceof PortalBlock) {
            if (!stateBelow.getValue(PortalBlock.LAYER).equals(PortalBlock.PortalLayer.BOTTOM)) {
                return;
            }
            level.setBlock(pos.below(), 
                ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                    .setValue(PortalBlock.LAYER, PortalBlock.PortalLayer.BOTTOM)
                    .setValue(PortalBlock.CEILING, level.dimension() == Level.NETHER), 
                3);
        }
        
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
    }
}
