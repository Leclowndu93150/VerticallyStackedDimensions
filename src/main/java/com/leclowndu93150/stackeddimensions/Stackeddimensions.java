package com.leclowndu93150.stackeddimensions;

import com.leclowndu93150.stackeddimensions.block.DimensionalPipeBlockEntity;
import com.leclowndu93150.stackeddimensions.init.ModBlockEntities;
import com.leclowndu93150.stackeddimensions.init.ModBlocks;
import com.leclowndu93150.stackeddimensions.init.ModCreativeTabs;
import com.leclowndu93150.stackeddimensions.init.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Stackeddimensions.MODID)
public class Stackeddimensions {

    public static final String MODID = "stackeddimensions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Stackeddimensions() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ForgeChunkManager.setForcedChunkLoadingCallback(MODID, (level, ticketHelper) -> {
                ticketHelper.getBlockTickets().forEach((pos, chunks) -> {
                    if (!(level.getBlockEntity(pos) instanceof DimensionalPipeBlockEntity)) {
                        chunks.getFirst().forEach(chunk -> ticketHelper.removeTicket(pos, chunk, false));
                        chunks.getSecond().forEach(chunk -> ticketHelper.removeTicket(pos, chunk, true));
                    }
                });
            });
        });
    }
}
