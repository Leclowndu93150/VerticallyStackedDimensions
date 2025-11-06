package com.leclowndu93150.stackeddimensions.mixin.client;

import com.leclowndu93150.stackeddimensions.StackedDimensionsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @Shadow
    private ClientLevel level;
    
    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", ordinal = 0))
    private void changeVoidColor(PoseStack p_202424_, Matrix4f p_254034_, float p_202426_, Camera p_202427_, boolean p_202428_, Runnable p_202429_, CallbackInfo ci) {
        if (!StackedDimensionsConfig.enableStackedDimensions) return;
        if (level == null) return;
        
        double cameraY = p_202427_.getPosition().y;
        
        if (level.dimension() == Level.OVERWORLD) {
            int minY = level.getMinBuildHeight();
            if (cameraY < minY + 20) {
                float mixFactor = Math.min(1.0f, (minY + 20 - (float)cameraY) / 15f);
                Vec3 skyColor = level.getSkyColor(p_202427_.getPosition(), p_202426_);
                float r = (float)skyColor.x * (1 - mixFactor) + 0.3f * mixFactor;
                float g = (float)skyColor.y * (1 - mixFactor) + 0.1f * mixFactor;
                float b = (float)skyColor.z * (1 - mixFactor) + 0.1f * mixFactor;
                RenderSystem.setShaderColor(r, g, b, 1.0f);
            }
        } else if (level.dimension() == Level.NETHER) {
            if (cameraY > 128 - 20) {
                float mixFactor = Math.min(1.0f, ((float)cameraY - (128 - 20)) / 15f);
                RenderSystem.setShaderColor(
                    0.1f * (1 - mixFactor) + 0.6f * mixFactor,
                    0.05f * (1 - mixFactor) + 0.7f * mixFactor,
                    0.05f * (1 - mixFactor) + 0.9f * mixFactor,
                    1.0f
                );
            }
        }
    }
}
