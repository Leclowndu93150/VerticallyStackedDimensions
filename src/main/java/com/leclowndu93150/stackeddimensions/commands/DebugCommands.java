package com.leclowndu93150.stackeddimensions.commands;

import com.leclowndu93150.stackeddimensions.DimStackManager;
import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stackeddimensions.MODID)
public class DebugCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(
            Commands.literal("stackeddimensions")
                .then(Commands.literal("clearchunk")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        BlockPos playerPos = BlockPos.containing(context.getSource().getPosition());
                        
                        int chunkX = playerPos.getX() >> 4;
                        int chunkZ = playerPos.getZ() >> 4;
                        int startX = chunkX << 4;
                        int startZ = chunkZ << 4;
                        
                        int minY = level.getMinBuildHeight();
                        int maxY = level.getMaxBuildHeight();
                        
                        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                        for (int x = startX; x < startX + 16; x++) {
                            for (int z = startZ; z < startZ + 16; z++) {
                                for (int y = minY; y < maxY; y++) {
                                    pos.set(x, y, z);
                                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                }
                            }
                        }
                        
                        context.getSource().sendSuccess(
                            () -> Component.literal("Cleared chunk at " + chunkX + ", " + chunkZ), 
                            true
                        );
                        
                        return 1;
                    })
                )
                .then(Commands.literal("reloadconfig")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        DimStackManager.reloadConfig();
                        context.getSource().sendSuccess(
                            () -> Component.literal("Portal config reloaded successfully!"), 
                            true
                        );
                        return 1;
                    })
                )
        );
    }
}
