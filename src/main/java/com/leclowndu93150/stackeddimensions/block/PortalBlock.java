package com.leclowndu93150.stackeddimensions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public class PortalBlock extends Block {
    
    public static final EnumProperty<PortalLayer> LAYER = EnumProperty.create("layer", PortalLayer.class);
    public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");
    
    public enum PortalLayer implements StringRepresentable {
        BOTTOM("bottom"),
        MIDDLE("middle"),
        TOP("top");
        
        private final String name;
        
        PortalLayer(String name) {
            this.name = name;
        }
        
        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
    
    public PortalBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(0.3F)
                .noLootTable()
                .noOcclusion()
                .lightLevel(state -> 8));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LAYER, PortalLayer.BOTTOM)
                .setValue(CEILING, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYER, CEILING);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) ? true : super.skipRendering(state, adjacentState, direction);
    }
    
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.isCreative() && serverPlayer.getFoodData().getFoodLevel() <= 1) {
                return false;
            }
            
            ServerLevel targetLevel;
            BlockPos targetPos;
            
            if (level.dimension() == Level.OVERWORLD) {
                targetLevel = level.getServer().getLevel(Level.NETHER);
                if (targetLevel == null) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
                
                int offset = switch (state.getValue(LAYER)) {
                    case BOTTOM -> 0;
                    case MIDDLE -> 1;
                    case TOP -> 2;
                };
                targetPos = new BlockPos(pos.getX(), 127 - offset, pos.getZ());
                
            } else if (level.dimension() == Level.NETHER) {
                targetLevel = level.getServer().getLevel(Level.OVERWORLD);
                if (targetLevel == null) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
                
                int minY = targetLevel.getMinBuildHeight();
                int offset = switch (state.getValue(LAYER)) {
                    case BOTTOM -> 0;
                    case MIDDLE -> 1;
                    case TOP -> 2;
                };
                targetPos = new BlockPos(pos.getX(), minY + offset, pos.getZ());
                
            } else {
                return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
            }
            
            BlockState targetState = targetLevel.getBlockState(targetPos);
            if (targetState.getBlock() instanceof PortalBlock || !targetState.isAir()) {
                breakBlockInOtherDimension(targetLevel, targetPos, serverPlayer);
            }
            
            player.causeFoodExhaustion(4.0F);
        }
        
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
    
    private void breakBlockInOtherDimension(ServerLevel targetLevel, BlockPos targetPos, ServerPlayer player) {
        BlockState state = targetLevel.getBlockState(targetPos);
        
        ItemStack mainHandItem = player.getMainHandItem();
        
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(targetLevel, targetPos, state, player);
        if (player.isSpectator()) event.setCanceled(true);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;
        
        BlockEntity blockEntity = targetLevel.getBlockEntity(targetPos);
        
        if (player.isCreative()) {
            targetLevel.destroyBlock(targetPos, false, player);
        } else {
            ItemStack itemCopy = mainHandItem.copy();
            boolean canHarvest = player.hasCorrectToolForDrops(state);
            
            mainHandItem.mineBlock(targetLevel, state, targetPos, player);
            
            if (state.onDestroyedByPlayer(targetLevel, targetPos, player, canHarvest, targetLevel.getFluidState(targetPos))) {
                state.getBlock().destroy(targetLevel, targetPos, state);
                if (canHarvest) {
                    state.getBlock().playerDestroy(targetLevel, player, targetPos, state, blockEntity, itemCopy);
                }
                int exp = event.getExpToDrop();
                if (exp > 0) {
                    state.getBlock().popExperience(targetLevel, targetPos, exp);
                }
            }
        }
    }
}
