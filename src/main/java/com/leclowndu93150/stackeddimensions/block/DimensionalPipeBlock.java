package com.leclowndu93150.stackeddimensions.block;

import com.leclowndu93150.stackeddimensions.config.PortalConfig;
import com.leclowndu93150.stackeddimensions.config.PortalConfigLoader;
import com.leclowndu93150.stackeddimensions.init.ModBlockEntities;
import com.leclowndu93150.stackeddimensions.init.ModBlocks;
import com.leclowndu93150.stackeddimensions.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DimensionalPipeBlock extends BaseEntityBlock {

    private static final ThreadLocal<Boolean> IS_BREAKING_LINKED = ThreadLocal.withInitial(() -> false);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public DimensionalPipeBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .noOcclusion()
                .lightLevel(state -> 6));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DimensionalPipeBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.DIMENSIONAL_PIPE.get(), DimensionalPipeBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                convertToPortal(serverLevel, pos, state, (ServerPlayer) player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    private void convertToPortal(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        Direction facing = state.getValue(FACING);
        boolean isCeiling = facing == Direction.DOWN;

        PortalConfig portalConfig = PortalConfigLoader.load();
        String currentDim = level.dimension().location().toString();
        PortalConfig.PortalDefinition portal = portalConfig.getPortalForDimension(currentDim);

        PortalBlock.PortalLayer layer = PortalBlock.PortalLayer.BOTTOM;
        if (portal != null) {
            int baseY = portal.bedrockYLevel;
            int offset = isCeiling ? (baseY - pos.getY()) : (pos.getY() - baseY);
            layer = switch (offset) {
                case 0 -> PortalBlock.PortalLayer.BOTTOM;
                case 1 -> PortalBlock.PortalLayer.MIDDLE;
                default -> PortalBlock.PortalLayer.TOP;
            };
        }

        if (level.getBlockEntity(pos) instanceof DimensionalPipeBlockEntity pipeEntity) {
            DimensionalPipeBlockEntity linked = pipeEntity.getLinkedPipe();
            if (linked != null && linked.getLevel() instanceof ServerLevel linkedLevel) {
                PortalConfig.PortalDefinition linkedPortal = portalConfig.getPortalForDimension(
                        linkedLevel.dimension().location().toString());
                if (linkedPortal == null || !linkedPortal.dynamicLoading) {
                    convertLinkedPipeToPortal(linkedLevel, linked.getBlockPos(), linked.getBlockState());
                }
            }
        }

        BlockState portalState = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                .setValue(PortalBlock.LAYER, layer)
                .setValue(PortalBlock.CEILING, isCeiling);
        level.setBlock(pos, portalState, 3);

        player.getInventory().add(new ItemStack(ModItems.DIMENSIONAL_PIPE.get()));
        level.playSound(null, pos, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void convertLinkedPipeToPortal(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean isCeiling = facing == Direction.DOWN;

        PortalConfig portalConfig = PortalConfigLoader.load();
        String currentDim = level.dimension().location().toString();
        PortalConfig.PortalDefinition portal = portalConfig.getPortalForDimension(currentDim);

        PortalBlock.PortalLayer layer = PortalBlock.PortalLayer.BOTTOM;
        if (portal != null) {
            int baseY = portal.bedrockYLevel;
            int offset = isCeiling ? (baseY - pos.getY()) : (pos.getY() - baseY);
            layer = switch (offset) {
                case 0 -> PortalBlock.PortalLayer.BOTTOM;
                case 1 -> PortalBlock.PortalLayer.MIDDLE;
                default -> PortalBlock.PortalLayer.TOP;
            };
        }

        BlockState portalState = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                .setValue(PortalBlock.LAYER, layer)
                .setValue(PortalBlock.CEILING, isCeiling);
        level.setBlock(pos, portalState, 3);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (IS_BREAKING_LINKED.get()) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }

        if (level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof DimensionalPipeBlockEntity pipeEntity) {
                DimensionalPipeBlockEntity linked = pipeEntity.getLinkedPipe();

                if (linked != null && linked.getLevel() instanceof ServerLevel linkedLevel) {
                    PortalConfig portalConfig = PortalConfigLoader.load();
                    String linkedDim = linkedLevel.dimension().location().toString();
                    PortalConfig.PortalDefinition linkedPortal = portalConfig.getPortalForDimension(linkedDim);

                    if (linkedPortal == null || !linkedPortal.dynamicLoading) {
                        IS_BREAKING_LINKED.set(true);
                        try {
                            convertLinkedPipeToPortal(linkedLevel, linked.getBlockPos(), linked.getBlockState());
                        } finally {
                            IS_BREAKING_LINKED.set(false);
                        }
                    }
                }
            }
        }

        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
}
