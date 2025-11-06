package com.leclowndu93150.stackeddimensions.events;

import com.leclowndu93150.stackeddimensions.DimStackManager;
import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stackeddimensions.MODID)
public class PlayerTickHandler {
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            DimStackManager.onPlayerTick(serverPlayer);
        }
    }
}
