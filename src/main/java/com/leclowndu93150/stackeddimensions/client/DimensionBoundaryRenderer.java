package com.leclowndu93150.stackeddimensions.client;

import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import com.leclowndu93150.stackeddimensions.StackedDimensionsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Stackeddimensions.MODID, value = Dist.CLIENT)
public class DimensionBoundaryRenderer {
    
    private static final Random RANDOM = new Random();
    private static final int CHECK_RADIUS = 16;
    private static boolean nearNetherBoundary = false;
    private static boolean nearOverworldBoundary = false;
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!StackedDimensionsConfig.enableStackedDimensions) return;
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        
        if (player == null || level == null) return;
        
        nearNetherBoundary = false;
        nearOverworldBoundary = false;
        
        BlockPos playerPos = player.blockPosition();
        
        if (level.dimension() == Level.OVERWORLD) {
            int minY = level.getMinBuildHeight();
            if (player.getY() < minY + 20) {
                nearNetherBoundary = checkForVoidAccess(level, playerPos, minY + 10);
            }
        } else if (level.dimension() == Level.NETHER) {
            if (player.getY() > 128 - 20) {
                nearOverworldBoundary = checkForVoidAccess(level, playerPos, 128 - 10);
            }
        }
        
        if (nearNetherBoundary || nearOverworldBoundary) {
            spawnBoundaryParticles(level, playerPos);
        }
    }
    
    private static boolean checkForVoidAccess(ClientLevel level, BlockPos playerPos, int boundaryY) {
        for (int x = -CHECK_RADIUS; x <= CHECK_RADIUS; x += 4) {
            for (int z = -CHECK_RADIUS; z <= CHECK_RADIUS; z += 4) {
                BlockPos checkPos = playerPos.offset(x, 0, z);
                
                if (hasOpenPath(level, checkPos, boundaryY)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean hasOpenPath(ClientLevel level, BlockPos from, int targetY) {
        int startY = from.getY();
        int direction = targetY > startY ? 1 : -1;
        
        for (int y = startY; direction > 0 ? y <= targetY : y >= targetY; y += direction) {
            BlockPos pos = new BlockPos(from.getX(), y, from.getZ());
            BlockState state = level.getBlockState(pos);
            
            if (!state.isAir() && !state.is(Blocks.WATER) && !state.is(Blocks.LAVA)) {
                return false;
            }
        }
        
        return true;
    }
    
    private static void spawnBoundaryParticles(ClientLevel level, BlockPos playerPos) {
        if (RANDOM.nextInt(2) != 0) return;
        
        for (int i = 0; i < 5; i++) {
            double x = playerPos.getX() + (RANDOM.nextDouble() - 0.5) * 20;
            double z = playerPos.getZ() + (RANDOM.nextDouble() - 0.5) * 20;
            double y;
            
            if (nearNetherBoundary) {
                y = level.getMinBuildHeight() + 10 + RANDOM.nextDouble() * 10;
                level.addParticle(ParticleTypes.CRIMSON_SPORE, x, y, z, 0, 0.03, 0);
                level.addParticle(ParticleTypes.WARPED_SPORE, x, y, z, 0, 0.02, 0);
                if (RANDOM.nextInt(5) == 0) {
                    level.addParticle(ParticleTypes.LAVA, x, y, z, 0, -0.01, 0);
                }
                if (RANDOM.nextInt(8) == 0) {
                    level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.05, 0);
                }
            } else if (nearOverworldBoundary) {
                y = 128 - 10 - RANDOM.nextDouble() * 10;
                level.addParticle(ParticleTypes.PORTAL, x, y, z, 
                    (RANDOM.nextDouble() - 0.5) * 0.3, 
                    -0.03, 
                    (RANDOM.nextDouble() - 0.5) * 0.3);
                if (RANDOM.nextInt(5) == 0) {
                    level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, -0.05, 0);
                }
                if (RANDOM.nextInt(8) == 0) {
                    level.addParticle(ParticleTypes.GLOW, x, y, z, 0, -0.02, 0);
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!StackedDimensionsConfig.enableStackedDimensions) return;
        
        if (nearNetherBoundary) {
            event.setNearPlaneDistance(event.getNearPlaneDistance() * 0.7f);
            event.setFarPlaneDistance(event.getFarPlaneDistance() * 0.5f);
            event.setCanceled(true);
        } else if (nearOverworldBoundary) {
            event.setNearPlaneDistance(event.getNearPlaneDistance() * 0.8f);
            event.setFarPlaneDistance(event.getFarPlaneDistance() * 0.6f);
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!StackedDimensionsConfig.enableStackedDimensions) return;
        
        if (nearNetherBoundary) {
            float mixFactor = 0.3f;
            event.setRed(event.getRed() * (1 - mixFactor) + 0.3f * mixFactor);
            event.setGreen(event.getGreen() * (1 - mixFactor) + 0.1f * mixFactor);
            event.setBlue(event.getBlue() * (1 - mixFactor) + 0.1f * mixFactor);
        } else if (nearOverworldBoundary) {
            float mixFactor = 0.2f;
            event.setRed(event.getRed() * (1 - mixFactor) + 0.7f * mixFactor);
            event.setGreen(event.getGreen() * (1 - mixFactor) + 0.8f * mixFactor);
            event.setBlue(event.getBlue() * (1 - mixFactor) + 1.0f * mixFactor);
        }
    }
}
