package com.leclowndu93150.stackeddimensions.mixin;

import com.leclowndu93150.stackeddimensions.DimStackManager;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    
    @Shadow
    @Final
    ServerLevel level;
    
    @Inject(
        method = "scheduleChunkGeneration",
        at = @At("RETURN")
    )
    private void onChunkGenerated(ChunkHolder p_140361_, ChunkStatus status, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        if (status == ChunkStatus.FULL) {
            cir.getReturnValue().thenAccept(either -> {
                either.ifLeft(chunk -> {
                    DimStackManager.onChunkLoad(level, chunk);
                });
            });
        }
    }
}
