package com.leclowndu93150.stackeddimensions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

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
                .strength(-1.0F, 3600000.0F)
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
    public boolean skipRendering(BlockState state, BlockState adjacentState, net.minecraft.core.Direction direction) {
        return adjacentState.is(this) ? true : super.skipRendering(state, adjacentState, direction);
    }
    
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            tryBreakInOtherDimension(serverLevel, pos, serverPlayer);
        }
    }
    
    private void tryBreakInOtherDimension(ServerLevel level, BlockPos pos, ServerPlayer player) {
        ServerLevel targetLevel;
        BlockPos targetPos;
        
        if (level.dimension() == Level.OVERWORLD) {
            targetLevel = level.getServer().getLevel(Level.NETHER);
            if (targetLevel == null) return;
            
            int offset = switch (level.getBlockState(pos).getValue(LAYER)) {
                case BOTTOM -> 1;
                case MIDDLE -> 2;
                case TOP -> 3;
            };
            targetPos = new BlockPos(pos.getX(), 127 - offset, pos.getZ());
            
        } else if (level.dimension() == Level.NETHER) {
            targetLevel = level.getServer().getLevel(Level.OVERWORLD);
            if (targetLevel == null) return;
            
            int minY = targetLevel.getMinBuildHeight();
            int offset = switch (level.getBlockState(pos).getValue(LAYER)) {
                case BOTTOM -> 1;
                case MIDDLE -> 2;
                case TOP -> 3;
            };
            targetPos = new BlockPos(pos.getX(), minY + offset, pos.getZ());
            
        } else {
            return;
        }
        
        BlockState targetState = targetLevel.getBlockState(targetPos);
        if (targetState.getBlock() instanceof PortalBlock) {
            targetLevel.destroyBlock(targetPos, true, player);
            level.destroyBlock(pos, false, player);
        }
    }
}
